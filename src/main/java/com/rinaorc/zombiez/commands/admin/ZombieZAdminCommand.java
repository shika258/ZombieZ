package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande d'administration principale du plugin
 * /zombiez <subcommand> [args...]
 */
public class ZombieZAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "info", "stats", "setzone", "setspawn", "givexp", "givepoints",
        "givegems", "setlevel", "teleport", "debug", "cache"
    );

    public ZombieZAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        // Permission check
        if (!sender.hasPermission("zombiez.admin")) {
            MessageUtils.sendRaw((Player) sender, "§cVous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            case "stats" -> handleStats(sender, args);
            case "setzone" -> handleSetZone(sender, args);
            case "setspawn" -> handleSetSpawn(sender);
            case "givexp" -> handleGiveXp(sender, args);
            case "givepoints" -> handleGivePoints(sender, args);
            case "givegems" -> handleGiveGems(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "teleport", "tp" -> handleTeleport(sender, args);
            case "debug" -> handleDebug(sender);
            case "cache" -> handleCache(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Affiche l'aide
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lZOMBIEZ §7- Commandes Admin");
        sender.sendMessage("");
        sender.sendMessage("§e/zombiez reload §7- Recharger la config");
        sender.sendMessage("§e/zombiez info §7- Informations du plugin");
        sender.sendMessage("§e/zombiez stats <joueur> §7- Stats d'un joueur");
        sender.sendMessage("§e/zombiez setzone <zone> §7- Définir une zone");
        sender.sendMessage("§e/zombiez setspawn §7- Définir le spawn à votre position");
        sender.sendMessage("§e/zombiez givexp <joueur> <montant> §7- Donner XP");
        sender.sendMessage("§e/zombiez givepoints <joueur> <montant> §7- Donner Points");
        sender.sendMessage("§e/zombiez givegems <joueur> <montant> §7- Donner Gems");
        sender.sendMessage("§e/zombiez setlevel <joueur> <niveau> §7- Définir niveau");
        sender.sendMessage("§e/zombiez tp <zone> §7- TP vers une zone");
        sender.sendMessage("§e/zombiez debug §7- Activer/désactiver debug");
        sender.sendMessage("§e/zombiez cache §7- Statistiques du cache");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Recharge la configuration
     */
    private void handleReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        plugin.reload();
        long time = System.currentTimeMillis() - start;
        
        sender.sendMessage("§a✓ Configuration rechargée en §e" + time + "ms");
    }

    /**
     * Affiche les informations du plugin
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lZOMBIEZ §7- Informations");
        sender.sendMessage("");
        sender.sendMessage("§7Version: §e" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Auteur: §eRinaorc Studio");
        sender.sendMessage("§7Joueurs en ligne: §e" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("§7Joueurs en cache: §e" + plugin.getPlayerDataManager().getCachedCount());
        sender.sendMessage("§7Zones chargées: §e" + plugin.getZoneManager().getAllZones().size());
        sender.sendMessage("§7BDD: §e" + plugin.getDatabaseManager().getDatabaseType());
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Affiche les stats d'un joueur (admin)
     */
    private void handleStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zombiez stats <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées non chargées pour ce joueur!");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lSTATS §7- " + target.getName());
        sender.sendMessage("");
        sender.sendMessage("§7UUID: §e" + data.getUuid());
        sender.sendMessage("§7Niveau: §e" + data.getLevel().get() + " §7(XP: " + data.getXp().get() + ")");
        sender.sendMessage("§7Prestige: §e" + data.getPrestige().get());
        sender.sendMessage("§7Points: §6" + EconomyManager.formatPoints(data.getPoints().get()));
        sender.sendMessage("§7Gems: §d" + data.getGems().get());
        sender.sendMessage("§7Kills: §c" + data.getKills().get() + " §7| Deaths: §4" + data.getDeaths().get());
        sender.sendMessage("§7K/D: §e" + String.format("%.2f", data.getKDRatio()));
        sender.sendMessage("§7Zone actuelle: §b" + data.getCurrentZone().get() + 
            " §7| Max: §b" + data.getMaxZone().get());
        sender.sendMessage("§7Checkpoint: §a" + data.getCurrentCheckpoint().get());
        sender.sendMessage("§7Temps de jeu: §e" + data.getFormattedPlaytime());
        sender.sendMessage("§7VIP: §e" + data.getVipRank() + 
            (data.isVip() ? " §a(Actif)" : " §c(Inactif)"));
        sender.sendMessage("§7Kill Streak: §6" + data.getKillStreak().get() + 
            " §7| Best: §6" + data.getBestKillStreak().get());
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Configure une zone
     */
    private void handleSetZone(CommandSender sender, String[] args) {
        // TODO: Implémenter la configuration de zone
        sender.sendMessage("§eFonctionnalité en cours de développement...");
    }

    /**
     * Définit le point de spawn à la position actuelle du joueur
     */
    private void handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande doit être exécutée par un joueur!");
            return;
        }

        Location loc = player.getLocation();

        // Sauvegarder dans la config
        FileConfiguration config = plugin.getConfig();
        config.set("gameplay.spawn.world", loc.getWorld().getName());
        config.set("gameplay.spawn.x", loc.getBlockX());
        config.set("gameplay.spawn.y", loc.getBlockY());
        config.set("gameplay.spawn.z", loc.getBlockZ());
        config.set("gameplay.spawn.yaw", loc.getYaw());
        config.set("gameplay.spawn.pitch", loc.getPitch());
        plugin.saveConfig();

        // Mettre à jour le ZoneManager si nécessaire
        plugin.getZoneManager().setSpawnLocation(loc);

        sender.sendMessage("§a✓ Point de spawn défini!");
        sender.sendMessage(String.format("§7Position: §e%d, %d, %d §7dans §e%s",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
    }

    /**
     * Donne de l'XP à un joueur
     */
    private void handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zombiez givexp <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        try {
            long amount = Long.parseLong(args[2]);
            plugin.getEconomyManager().addXp(target, amount, "Admin");
            sender.sendMessage("§a✓ " + amount + " XP donnés à " + target.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
    }

    /**
     * Donne des points à un joueur
     */
    private void handleGivePoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zombiez givepoints <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        try {
            long amount = Long.parseLong(args[2]);
            plugin.getEconomyManager().addPoints(target, amount, "Admin");
            sender.sendMessage("§a✓ " + EconomyManager.formatPoints(amount) + " Points donnés à " + target.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
    }

    /**
     * Donne des gems à un joueur
     */
    private void handleGiveGems(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zombiez givegems <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            plugin.getEconomyManager().addGems(target, amount, "Admin");
            sender.sendMessage("§a✓ " + amount + " Gems données à " + target.getName());
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
    }

    /**
     * Définit le niveau d'un joueur
     */
    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zombiez setlevel <joueur> <niveau>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées non chargées!");
            return;
        }

        try {
            int level = Integer.parseInt(args[2]);
            data.getLevel().set(level);
            data.getXp().set(0);
            data.markDirty();
            sender.sendMessage("§a✓ Niveau de " + target.getName() + " défini à " + level);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cNiveau invalide!");
        }
    }

    /**
     * Téléporte vers une zone
     */
    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zombiez tp <zone_id>");
            return;
        }

        try {
            int zoneId = Integer.parseInt(args[1]);
            var zone = plugin.getZoneManager().getZoneById(zoneId);
            
            if (zone == null) {
                sender.sendMessage("§cZone non trouvée! (ID: " + zoneId + ")");
                return;
            }

            // Téléporter au début de la zone (maxZ car progression vers Z décroissant)
            int targetZ = zone.getMaxZ() - 10;
            var location = player.getLocation().clone();
            location.setZ(targetZ);
            location.setY(player.getWorld().getHighestBlockYAt(location) + 1);

            player.teleport(location);
            sender.sendMessage("§a✓ Téléporté vers " + zone.getColoredName());
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID de zone invalide!");
        }
    }

    /**
     * Active/désactive le mode debug
     */
    private void handleDebug(CommandSender sender) {
        // Toggle debug mode
        boolean current = plugin.getConfigManager().isDebugMode();
        // Note: Ceci nécessiterait une méthode setter dans ConfigManager
        sender.sendMessage("§eMode debug: " + (!current ? "§aActivé" : "§cDésactivé"));
        sender.sendMessage("§7(Modifier config.yml et faire /zombiez reload)");
    }

    /**
     * Affiche les statistiques du cache
     */
    private void handleCache(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lCACHE §7- Statistiques");
        sender.sendMessage("");
        sender.sendMessage("§7Player Cache: §e" + plugin.getPlayerDataManager().getCacheStats());
        sender.sendMessage("§7DB Pool: §e" + plugin.getDatabaseManager().getPoolStats());
        sender.sendMessage("§8§m                                        ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin")) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("stats") || subCmd.equals("givexp") || 
                subCmd.equals("givepoints") || subCmd.equals("givegems") || 
                subCmd.equals("setlevel")) {
                // Complétion des noms de joueurs
                String partial = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
            } else if (subCmd.equals("tp") || subCmd.equals("teleport")) {
                // Complétion des zones
                String partial = args[1].toLowerCase();
                for (var zone : plugin.getZoneManager().getAllZones()) {
                    String id = String.valueOf(zone.getId());
                    if (id.startsWith(partial)) {
                        completions.add(id);
                    }
                }
            }
        }

        return completions;
    }
}
