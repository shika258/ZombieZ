package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import com.rinaorc.zombiez.zones.gui.ZoneDetailGUI;
import com.rinaorc.zombiez.zones.gui.ZoneWikiGUI;
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
 * Commande /zone - Wiki des zones avec menu GUI interactif
 *
 * Ouvre un menu GUI wiki complet des zones:
 * - /zone ou /zones : Ouvre le menu wiki des zones
 * - /zone <id> : Ouvre les details d'une zone specifique
 * - /zone list : Liste texte des zones (legacy)
 * - /zone info : Informations sur la zone actuelle (legacy)
 *
 * Les joueurs peuvent consulter toutes les informations.
 * Les admins peuvent se teleporter vers n'importe quelle zone.
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
            // Ouvrir le menu wiki des zones
            new ZoneWikiGUI(plugin, player, 0).open();
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
            return true;
        }

        // Sous-commandes
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> showZoneList(player);
            case "info", "current" -> showCurrentZone(player);
            case "wiki", "menu", "gui" -> {
                new ZoneWikiGUI(plugin, player, 0).open();
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
            }
            case "help", "?" -> showHelp(player);
            default -> openZoneByIdOrName(player, args[0]);
        }

        return true;
    }

    /**
     * Ouvre une zone par ID ou nom
     */
    private void openZoneByIdOrName(Player player, String input) {
        try {
            int zoneId = Integer.parseInt(input);
            Zone zone = plugin.getZoneManager().getZoneById(zoneId);

            if (zone == null) {
                MessageUtils.send(player, "§cZone #" + zoneId + " non trouvee! §7(0-50)");
                return;
            }

            new ZoneDetailGUI(plugin, player, zone, 0, 0).open();
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

        } catch (NumberFormatException e) {
            Zone zone = plugin.getZoneManager().getZoneByName(input);
            if (zone != null) {
                new ZoneDetailGUI(plugin, player, zone, 0, 0).open();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else {
                MessageUtils.send(player, "§cZone non trouvee! §7Utilise §e/zone §7pour le wiki.");
            }
        }
    }

    /**
     * Affiche l'aide de la commande
     */
    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         §6§l🗺 COMMANDE /ZONE");
        player.sendMessage("");
        player.sendMessage("  §e/zone §8- §7Ouvre le wiki des zones");
        player.sendMessage("  §e/zone <id> §8- §7Details d'une zone");
        player.sendMessage("  §e/zone list §8- §7Liste textuelle des zones");
        player.sendMessage("  §e/zone info §8- §7Info zone actuelle");
        player.sendMessage("  §e/zone help §8- §7Affiche cette aide");
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");
    }

    /**
     * Affiche les informations de la zone actuelle
     */
    private void showCurrentZone(Player player) {
        Zone zone = plugin.getZoneManager().getPlayerZone(player);

        if (zone == null) {
            MessageUtils.send(player, "§cVous n'etes dans aucune zone connue!");
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

            // Sous-commandes
            String[] subCommands = {"list", "info", "wiki", "menu", "help"};
            for (String sub : subCommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }

            // IDs de zones (0-50)
            for (int i = 0; i <= 50; i++) {
                String id = String.valueOf(i);
                if (id.startsWith(partial)) {
                    completions.add(id);
                }
            }

            // Noms de zones
            for (Zone zone : plugin.getZoneManager().getAllZones()) {
                if (zone.getName().toLowerCase().startsWith(partial)) {
                    completions.add(zone.getName());
                }
            }
        }

        return completions;
    }
}
