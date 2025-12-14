package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande /zone - Affiche les informations sur la zone actuelle ou une zone spécifique
 */
public class ZoneCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ZoneCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        if (args.length == 0) {
            // Afficher la zone actuelle
            showCurrentZone(player);
        } else if (args[0].equalsIgnoreCase("list")) {
            // Lister toutes les zones
            showZoneList(player);
        } else {
            // Afficher une zone spécifique
            try {
                int zoneId = Integer.parseInt(args[0]);
                showZoneInfo(player, zoneId);
            } catch (NumberFormatException e) {
                // Chercher par nom
                Zone zone = plugin.getZoneManager().getZoneByName(args[0]);
                if (zone != null) {
                    showZoneInfo(player, zone);
                } else {
                    MessageUtils.send(player, "§cZone non trouvée! §7Utilise §e/zone list §7pour voir les zones.");
                }
            }
        }

        return true;
    }

    /**
     * Affiche les informations de la zone actuelle
     */
    private void showCurrentZone(Player player) {
        Zone zone = plugin.getZoneManager().getPlayerZone(player);
        
        if (zone == null) {
            MessageUtils.send(player, "§cVous n'êtes dans aucune zone connue!");
            return;
        }

        showZoneInfo(player, zone);
    }

    /**
     * Affiche les informations d'une zone par ID
     */
    private void showZoneInfo(Player player, int zoneId) {
        Zone zone = plugin.getZoneManager().getZoneById(zoneId);
        
        if (zone == null) {
            MessageUtils.send(player, "§cZone #" + zoneId + " non trouvée!");
            return;
        }

        showZoneInfo(player, zone);
    }

    /**
     * Affiche les informations détaillées d'une zone
     */
    private void showZoneInfo(Player player, Zone zone) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int currentZ = player.getLocation().getBlockZ();
        boolean isInZone = zone.containsZ(currentZ);
        
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         " + zone.getColoredName().toUpperCase());
        player.sendMessage("         " + zone.getStarsDisplay());
        player.sendMessage("");
        
        // Description
        if (zone.getDescription() != null && !zone.getDescription().isEmpty()) {
            player.sendMessage("  §7\"" + zone.getDescription() + "\"");
            player.sendMessage("");
        }
        
        // Position
        player.sendMessage("  §7Position: §fZ " + zone.getMinZ() + " → " + zone.getMaxZ());
        
        if (isInZone) {
            double progress = zone.getProgressPercent(currentZ);
            String progressBar = MessageUtils.progressBar(progress, 15, "§a", "§7");
            player.sendMessage("  §7Progression: " + progressBar + " §f" + String.format("%.0f", progress) + "%");
        }
        player.sendMessage("");
        
        // Caractéristiques
        player.sendMessage("  §7Difficulté: §c" + zone.getDifficulty() + "/10");
        player.sendMessage("  §7Niv. Zombies: §c" + zone.getMinZombieLevel() + " - " + zone.getMaxZombieLevel());
        player.sendMessage("  §7Bonus XP: §a+" + (int)((zone.getXpMultiplier() - 1) * 100) + "%");
        player.sendMessage("  §7Bonus Loot: §6+" + (int)((zone.getLootMultiplier() - 1) * 100) + "%");
        
        // Flags spéciaux
        StringBuilder flags = new StringBuilder("  §7Flags: ");
        if (zone.isPvpEnabled()) flags.append("§c⚔PvP ");
        if (zone.isSafeZone()) flags.append("§a🛡Safe ");
        if (zone.isBossZone()) flags.append("§5👑Boss ");
        if (zone.isDangerous()) flags.append("§e⚠" + zone.getEnvironmentalEffect() + " ");
        
        player.sendMessage(flags.toString());
        
        // Refuge
        if (zone.getRefugeId() > 0) {
            player.sendMessage("  §7Refuge: §a✓ §7(ID: " + zone.getRefugeId() + ")");
        }
        
        // Joueurs dans la zone
        int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());
        player.sendMessage("  §7Joueurs actuels: §e" + playersInZone);
        
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    /**
     * Affiche la liste de toutes les zones
     */
    private void showZoneList(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int maxZoneReached = data != null ? data.getMaxZone().get() : 1;
        int currentZone = data != null ? data.getCurrentZone().get() : 1;

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         §6§l🗺 LISTE DES ZONES");
        player.sendMessage("");

        for (Zone zone : plugin.getZoneManager().getZonesSorted()) {
            boolean unlocked = zone.getId() <= maxZoneReached;
            boolean current = zone.getId() == currentZone;
            
            String status;
            if (current) {
                status = "§a► ";
            } else if (unlocked) {
                status = "§a✓ ";
            } else {
                status = "§8🔒 ";
            }

            String difficulty = zone.getStarsDisplay();
            int playersInZone = plugin.getZoneManager().getPlayersInZone(zone.getId());
            String players = playersInZone > 0 ? " §7(" + playersInZone + ")" : "";

            player.sendMessage("  " + status + zone.getColoredName() + " " + difficulty + players);
        }

        player.sendMessage("");
        player.sendMessage("  §7Utilise §e/zone <id> §7pour plus d'infos");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            
            // Ajouter "list"
            if ("list".startsWith(partial)) {
                completions.add("list");
            }
            
            // Ajouter les IDs de zones
            for (Zone zone : plugin.getZoneManager().getAllZones()) {
                String id = String.valueOf(zone.getId());
                if (id.startsWith(partial)) {
                    completions.add(id);
                }
                if (zone.getName().toLowerCase().startsWith(partial)) {
                    completions.add(zone.getName());
                }
            }
        }

        return completions;
    }
}
