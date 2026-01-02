package com.rinaorc.zombiez.leaderboards.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande /lbadmin - Administration des classements
 * Permission: zombiez.admin.leaderboard
 *
 * Usage:
 * - /lbadmin refresh - Force le rafraîchissement du cache
 * - /lbadmin reset <type> <period> - Réinitialise un classement
 * - /lbadmin resetall <period> - Réinitialise tous les classements d'une période
 * - /lbadmin ban <joueur> [raison] - Bannit un joueur des classements
 * - /lbadmin unban <joueur> - Débannit un joueur
 * - /lbadmin flag <joueur> - Flag un joueur pour comportement suspect
 * - /lbadmin unflag <joueur> - Retire le flag d'un joueur
 * - /lbadmin flagged - Liste les joueurs flaggés
 * - /lbadmin season new <nom> - Force une nouvelle saison
 * - /lbadmin season end - Termine la saison actuelle
 * - /lbadmin set <joueur> <type> <valeur> - Définit un score
 * - /lbadmin add <joueur> <type> <valeur> - Ajoute à un score
 * - /lbadmin distribute - Force la distribution des récompenses
 */
public class LeaderboardAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private static final String PERMISSION = "zombiez.admin.leaderboard";

    public LeaderboardAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cTu n'as pas la permission d'utiliser cette commande!");
            return true;
        }

        com.rinaorc.zombiez.leaderboards.LeaderboardManager manager = plugin.getNewLeaderboardManager();
        if (manager == null) {
            sender.sendMessage("§cLe système de classements n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "refresh", "reload" -> handleRefresh(sender, manager);
            case "reset" -> handleReset(sender, manager, args);
            case "resetall" -> handleResetAll(sender, manager, args);
            case "ban" -> handleBan(sender, manager, args);
            case "unban" -> handleUnban(sender, manager, args);
            case "flag" -> handleFlag(sender, manager, args);
            case "unflag" -> handleUnflag(sender, manager, args);
            case "flagged" -> handleFlagged(sender, manager);
            case "season" -> handleSeason(sender, manager, args);
            case "set" -> handleSet(sender, manager, args);
            case "add" -> handleAdd(sender, manager, args);
            case "distribute" -> handleDistribute(sender, manager);
            case "stats" -> handleStats(sender, manager);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleRefresh(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        sender.sendMessage("§e[LB] Rafraîchissement du cache en cours...");
        manager.refreshAllCache();
        sender.sendMessage("§a[LB] Cache rafraîchi avec succès!");
    }

    private void handleReset(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /lbadmin reset <type> <period>");
            return;
        }

        LeaderboardType type = findType(args[1]);
        LeaderboardPeriod period = findPeriod(args[2]);

        if (type == null) {
            sender.sendMessage("§cType invalide: " + args[1]);
            return;
        }

        if (period == null) {
            sender.sendMessage("§cPériode invalide: " + args[2]);
            return;
        }

        if (period == LeaderboardPeriod.ALL_TIME) {
            sender.sendMessage("§c⚠ ATTENTION: Cette action est irréversible!");
            sender.sendMessage("§cVoulez-vous vraiment réinitialiser le classement ALL_TIME?");
            sender.sendMessage("§7Utilisez /lbadmin resetall all_time pour confirmer.");
            return;
        }

        manager.resetPeriod(type, period);
        sender.sendMessage("§a[LB] Classement §e" + type.getDisplayName() + " §7(" + period.getDisplayName() + ") §aréinitialisé!");
    }

    private void handleResetAll(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lbadmin resetall <period>");
            return;
        }

        LeaderboardPeriod period = findPeriod(args[1]);
        if (period == null) {
            sender.sendMessage("§cPériode invalide: " + args[1]);
            return;
        }

        for (LeaderboardType type : LeaderboardType.values()) {
            manager.resetPeriod(type, period);
        }

        sender.sendMessage("§a[LB] Tous les classements §7(" + period.getDisplayName() + ") §aréinitialisés!");
    }

    private void handleBan(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lbadmin ban <joueur> [raison]");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Comportement suspect";

        manager.banPlayer(target.getUniqueId(), reason, sender.getName());
        sender.sendMessage("§a[LB] Joueur §e" + args[1] + " §abanni des classements!");
        sender.sendMessage("§7Raison: " + reason);

        // Notifier le joueur si en ligne
        if (target.isOnline()) {
            ((Player) target).sendMessage("§c[LB] Tu as été banni des classements!");
            ((Player) target).sendMessage("§7Raison: " + reason);
        }
    }

    private void handleUnban(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lbadmin unban <joueur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        manager.unbanPlayer(target.getUniqueId());
        sender.sendMessage("§a[LB] Joueur §e" + args[1] + " §adébanni des classements!");

        if (target.isOnline()) {
            ((Player) target).sendMessage("§a[LB] Tu as été débanni des classements!");
        }
    }

    private void handleFlag(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lbadmin flag <joueur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Flag manuel par admin";

        manager.flagPlayer(target.getUniqueId(), reason);
        sender.sendMessage("§e[LB] Joueur §f" + args[1] + " §eflaggé pour surveillance!");
    }

    private void handleUnflag(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /lbadmin unflag <joueur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        manager.unflagPlayer(target.getUniqueId());
        sender.sendMessage("§a[LB] Flag retiré pour §e" + args[1]);
    }

    private void handleFlagged(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        Set<UUID> flagged = manager.getFlaggedPlayers();

        sender.sendMessage("§8§m                                              ");
        sender.sendMessage("  §e§l⚠ JOUEURS FLAGGÉS §7(" + flagged.size() + ")");
        sender.sendMessage("");

        if (flagged.isEmpty()) {
            sender.sendMessage("  §aAucun joueur flaggé!");
        } else {
            for (UUID uuid : flagged) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
                String status = player.isOnline() ? "§a(en ligne)" : "§7(hors ligne)";
                sender.sendMessage("  §e• " + name + " " + status);
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m                                              ");
    }

    private void handleSeason(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            // Afficher les infos de la saison
            com.rinaorc.zombiez.leaderboards.LeaderboardManager.SeasonData season = manager.getCurrentSeason();
            if (season != null) {
                sender.sendMessage("§6[Saison] " + season.getName());
                sender.sendMessage("§7ID: " + season.getId() + " | Jours restants: " + season.getDaysRemaining());
            } else {
                sender.sendMessage("§cAucune saison active!");
            }
            sender.sendMessage("§7Commandes: /lbadmin season new <nom> | /lbadmin season end");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("new") && args.length >= 3) {
            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            manager.startNewSeason(name);
            sender.sendMessage("§a[LB] Nouvelle saison démarrée: §e" + name);
        } else if (action.equals("end")) {
            manager.endCurrentSeason();
            sender.sendMessage("§a[LB] Saison terminée! Les récompenses ont été distribuées.");
        } else {
            sender.sendMessage("§cUsage: /lbadmin season new <nom> | /lbadmin season end");
        }
    }

    private void handleSet(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /lbadmin set <joueur> <type> <valeur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        LeaderboardType type = findType(args[2]);

        if (type == null) {
            sender.sendMessage("§cType invalide: " + args[2]);
            return;
        }

        try {
            long value = Long.parseLong(args[3]);
            manager.updateScore(target.getUniqueId(), target.getName() != null ? target.getName() : "Unknown", type, value);
            sender.sendMessage("§a[LB] Score de §e" + args[1] + " §adéfini à §f" + value + " §apour §e" + type.getDisplayName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cValeur invalide: " + args[3]);
        }
    }

    private void handleAdd(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /lbadmin add <joueur> <type> <valeur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        LeaderboardType type = findType(args[2]);

        if (type == null) {
            sender.sendMessage("§cType invalide: " + args[2]);
            return;
        }

        try {
            long value = Long.parseLong(args[3]);
            manager.incrementScore(target.getUniqueId(), target.getName() != null ? target.getName() : "Unknown", type, value);
            sender.sendMessage("§a[LB] Ajouté §f" + value + " §aau score de §e" + args[1] + " §apour §e" + type.getDisplayName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cValeur invalide: " + args[3]);
        }
    }

    private void handleDistribute(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        sender.sendMessage("§e[LB] Distribution forcée des récompenses en cours...");

        // Distribuer pour toutes les périodes sauf ALL_TIME (qui est permanent)
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            if (period != LeaderboardPeriod.ALL_TIME) {
                for (LeaderboardType type : LeaderboardType.values()) {
                    manager.distributeRewards(type, period);
                }
            }
        }

        sender.sendMessage("§a[LB] Récompenses distribuées!");
    }

    private void handleStats(CommandSender sender, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        sender.sendMessage("§8§m                                              ");
        sender.sendMessage("  §6§l📊 STATISTIQUES LEADERBOARDS");
        sender.sendMessage("");
        sender.sendMessage("  §7Cache size: §f" + manager.getCacheSize());
        sender.sendMessage("  §7Dernière MAJ: §f" + formatTime(manager.getLastUpdate()));
        sender.sendMessage("  §7Joueurs flaggés: §e" + manager.getFlaggedPlayers().size());
        sender.sendMessage("");

        com.rinaorc.zombiez.leaderboards.LeaderboardManager.SeasonData season = manager.getCurrentSeason();
        if (season != null) {
            sender.sendMessage("  §7Saison: §c" + season.getName());
            sender.sendMessage("  §7Jours restants: §f" + season.getDaysRemaining());
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m                                              ");
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "jamais";
        long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000;
        if (secondsAgo < 60) return "il y a " + secondsAgo + "s";
        if (secondsAgo < 3600) return "il y a " + (secondsAgo / 60) + "m";
        return "il y a " + (secondsAgo / 3600) + "h";
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                              ");
        sender.sendMessage("  §6§l🔧 ADMIN CLASSEMENTS");
        sender.sendMessage("");
        sender.sendMessage("  §e/lbadmin refresh §7- Rafraîchir le cache");
        sender.sendMessage("  §e/lbadmin reset <type> <period> §7- Reset un classement");
        sender.sendMessage("  §e/lbadmin resetall <period> §7- Reset tous les classements");
        sender.sendMessage("  §e/lbadmin ban <joueur> [raison] §7- Bannir des classements");
        sender.sendMessage("  §e/lbadmin unban <joueur> §7- Débannir");
        sender.sendMessage("  §e/lbadmin flag/unflag <joueur> §7- Flagger/Déflagger");
        sender.sendMessage("  §e/lbadmin flagged §7- Voir les joueurs flaggés");
        sender.sendMessage("  §e/lbadmin season new/end §7- Gérer les saisons");
        sender.sendMessage("  §e/lbadmin set <joueur> <type> <val> §7- Définir score");
        sender.sendMessage("  §e/lbadmin add <joueur> <type> <val> §7- Ajouter au score");
        sender.sendMessage("  §e/lbadmin distribute §7- Distribuer les récompenses");
        sender.sendMessage("  §e/lbadmin stats §7- Statistiques du système");
        sender.sendMessage("");
        sender.sendMessage("§8§m                                              ");
    }

    private LeaderboardType findType(String name) {
        String normalized = name.toUpperCase().replace("-", "_");
        for (LeaderboardType type : LeaderboardType.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    private LeaderboardPeriod findPeriod(String name) {
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            if (period.name().equalsIgnoreCase(name)) {
                return period;
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "refresh", "reset", "resetall", "ban", "unban",
                "flag", "unflag", "flagged", "season", "set", "add", "distribute", "stats"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "reset", "set", "add" -> {
                    for (LeaderboardType type : LeaderboardType.values()) {
                        completions.add(type.name().toLowerCase());
                    }
                }
                case "resetall" -> {
                    for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                        completions.add(period.name().toLowerCase());
                    }
                }
                case "ban", "unban", "flag", "unflag" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        completions.add(p.getName());
                    }
                }
                case "season" -> completions.addAll(Arrays.asList("new", "end"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reset")) {
                for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                    completions.add(period.name().toLowerCase());
                }
            } else if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                for (LeaderboardType type : LeaderboardType.values()) {
                    completions.add(type.name().toLowerCase());
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add")) {
                completions.addAll(Arrays.asList("100", "1000", "10000"));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
