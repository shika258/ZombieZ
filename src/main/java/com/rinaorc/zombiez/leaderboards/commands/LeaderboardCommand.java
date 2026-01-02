package com.rinaorc.zombiez.leaderboards.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.*;
import com.rinaorc.zombiez.leaderboards.gui.*;
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
 * Commande /lb - Accès aux classements
 * Usage:
 * - /lb - Ouvre le GUI principal
 * - /lb top [type] - Affiche le top 10 d'un type
 * - /lb me - Affiche ses propres classements
 * - /lb compare <joueur> - Compare avec un autre joueur
 * - /lb rewards - Ouvre le GUI des récompenses
 * - /lb season - Affiche les infos de la saison
 */
public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public LeaderboardCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        com.rinaorc.zombiez.leaderboards.LeaderboardManager manager = plugin.getNewLeaderboardManager();
        if (manager == null) {
            player.sendMessage("§cLe système de classements n'est pas disponible.");
            return true;
        }

        // Pas d'arguments = ouvrir le GUI principal
        if (args.length == 0) {
            new LeaderboardMainGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "top" -> handleTop(player, manager, args);
            case "me", "moi" -> handleMe(player, manager);
            case "compare", "vs" -> handleCompare(player, manager, args);
            case "rewards", "recompenses" -> new LeaderboardRewardsGUI(plugin, player).open();
            case "season", "saison" -> handleSeason(player, manager);
            case "help", "aide" -> sendHelp(player);
            default -> {
                // Essayer de trouver un type de leaderboard
                LeaderboardType type = findType(args[0]);
                if (type != null) {
                    new LeaderboardDetailGUI(plugin, player, type, LeaderboardPeriod.ALL_TIME, 1).open();
                } else {
                    sendHelp(player);
                }
            }
        }

        return true;
    }

    private void handleTop(Player player, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        LeaderboardType type = LeaderboardType.KILLS_TOTAL;
        LeaderboardPeriod period = LeaderboardPeriod.ALL_TIME;

        if (args.length >= 2) {
            LeaderboardType found = findType(args[1]);
            if (found != null) {
                type = found;
            }
        }

        if (args.length >= 3) {
            LeaderboardPeriod foundPeriod = findPeriod(args[2]);
            if (foundPeriod != null) {
                period = foundPeriod;
            }
        }

        // Afficher le top 10 en chat
        displayTop10(player, manager, type, period);
    }

    private void displayTop10(Player player, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager,
                              LeaderboardType type, LeaderboardPeriod period) {
        List<LeaderboardEntry> entries = manager.getTopEntries(type, period, 10);

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("  §6§l🏆 " + type.getDisplayName().toUpperCase() + " §7- " + period.getDisplayName());
        player.sendMessage("");

        if (entries.isEmpty()) {
            player.sendMessage("  §7Aucune donnée disponible.");
        } else {
            for (LeaderboardEntry entry : entries) {
                String rankDisplay = entry.getRankIcon();
                String color = entry.getRank() <= 3 ? entry.getRankColor() : "§7";
                String value = type.getColumn().contains("playtime") ?
                    entry.getFormattedTime() : entry.getFormattedValue();

                player.sendMessage("  " + rankDisplay + " " + color + entry.getPlayerName() + " §f- §e" + value);
            }
        }

        // Position du joueur
        int playerRank = manager.getPlayerRank(player.getUniqueId(), type, period);
        if (playerRank > 0 && playerRank > 10) {
            player.sendMessage("");
            player.sendMessage("  §7Ta position: §e#" + playerRank);
        }

        player.sendMessage("");
        player.sendMessage("  §7Utilise §e/lb " + type.name().toLowerCase() + " §7pour plus de détails.");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    private void handleMe(Player player, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        new LeaderboardPlayerGUI(plugin, player, player.getUniqueId()).open();
    }

    private void handleCompare(Player player, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /lb compare <joueur>");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cTu ne peux pas te comparer à toi-même!");
            return;
        }

        // Vérifier que le joueur cible existe dans nos données
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage("§cJoueur introuvable: §e" + targetName);
            return;
        }

        new LeaderboardCompareGUI(plugin, player, player.getUniqueId(), target.getUniqueId()).open();
    }

    private void handleSeason(Player player, com.rinaorc.zombiez.leaderboards.LeaderboardManager manager) {
        com.rinaorc.zombiez.leaderboards.LeaderboardManager.SeasonData season = manager.getCurrentSeason();

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");

        if (season != null) {
            player.sendMessage("  §c§l🏆 " + season.getName().toUpperCase() + " §c§l🏆");
            player.sendMessage("");
            player.sendMessage("  §7ID: §f" + season.getId());
            player.sendMessage("  §7Temps restant: §e" + season.getDaysRemaining() + " jours");
            player.sendMessage("");
            player.sendMessage("  §7Les classements saisonniers sont réinitialisés");
            player.sendMessage("  §7à la fin de chaque saison (30 jours).");
            player.sendMessage("");
            player.sendMessage("  §6Récompenses exclusives:");
            player.sendMessage("    §e• Titres uniques pour le top 3");
            player.sendMessage("    §e• Cosmétiques saisonniers");
            player.sendMessage("    §e• Bonus de points x2");
        } else {
            player.sendMessage("  §7Aucune saison active actuellement.");
        }

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("  §6§l🏆 COMMANDES CLASSEMENTS 🏆");
        player.sendMessage("");
        player.sendMessage("  §e/lb §7- Ouvrir le menu des classements");
        player.sendMessage("  §e/lb top [type] [période] §7- Top 10 en chat");
        player.sendMessage("  §e/lb me §7- Voir tes classements");
        player.sendMessage("  §e/lb compare <joueur> §7- Comparer avec un joueur");
        player.sendMessage("  §e/lb rewards §7- Réclamer tes récompenses");
        player.sendMessage("  §e/lb season §7- Infos sur la saison");
        player.sendMessage("");
        player.sendMessage("  §7Périodes: §fall, daily, weekly, monthly, seasonal");
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    private LeaderboardType findType(String name) {
        String normalized = name.toUpperCase().replace("-", "_");
        for (LeaderboardType type : LeaderboardType.values()) {
            if (type.name().equalsIgnoreCase(normalized) ||
                type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    private LeaderboardPeriod findPeriod(String name) {
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            if (period.name().equalsIgnoreCase(name) ||
                period.getDisplayName().equalsIgnoreCase(name)) {
                return period;
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("top", "me", "compare", "rewards", "season", "help");
            completions.addAll(subCommands);

            // Ajouter les types de leaderboard
            for (LeaderboardType type : LeaderboardType.values()) {
                completions.add(type.name().toLowerCase());
            }

            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("top")) {
                for (LeaderboardType type : LeaderboardType.values()) {
                    completions.add(type.name().toLowerCase());
                }
            } else if (args[0].equalsIgnoreCase("compare") || args[0].equalsIgnoreCase("vs")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }

            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                completions.add(period.name().toLowerCase());
            }

            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
