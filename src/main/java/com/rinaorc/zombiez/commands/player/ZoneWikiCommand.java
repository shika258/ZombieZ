package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Zone;
import com.rinaorc.zombiez.zones.gui.ZoneDetailGUI;
import com.rinaorc.zombiez.zones.gui.ZoneWikiGUI;
import org.bukkit.Sound;
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
 * Commande /wiki ou /zones - Ouvre le menu Wiki des zones
 * Permet aux joueurs de consulter toutes les informations sur les zones
 * Les admins peuvent se téléporter via le menu
 */
public class ZoneWikiCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ZoneWikiCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        // Si un argument est fourni, ouvrir directement la zone spécifiée
        if (args.length > 0) {
            try {
                int zoneId = Integer.parseInt(args[0]);
                Zone zone = plugin.getZoneManager().getZoneById(zoneId);

                if (zone == null || zone.getId() == 0) {
                    player.sendMessage("§c[Wiki] Zone #" + zoneId + " non trouvée!");
                    player.sendMessage("§7Utilise §e/" + label + " §7pour ouvrir le menu complet.");
                    return true;
                }

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new ZoneDetailGUI(plugin, player, zone).open();
                return true;

            } catch (NumberFormatException e) {
                // Essayer de trouver par nom
                Zone zone = plugin.getZoneManager().getZoneByName(args[0]);

                if (zone == null) {
                    // Chercher par nom partiel
                    String search = String.join(" ", args).toLowerCase();
                    for (Zone z : plugin.getZoneManager().getAllZones()) {
                        if (z.getDisplayName().toLowerCase().contains(search) ||
                            z.getName().toLowerCase().contains(search)) {
                            zone = z;
                            break;
                        }
                    }
                }

                if (zone != null && zone.getId() > 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new ZoneDetailGUI(plugin, player, zone).open();
                    return true;
                }

                player.sendMessage("§c[Wiki] Zone \"" + args[0] + "\" non trouvée!");
                player.sendMessage("§7Utilise §e/" + label + " §7pour ouvrir le menu complet.");
                return true;
            }
        }

        // Ouvrir le menu principal
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        new ZoneWikiGUI(plugin, player).open();

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            // Ajouter les IDs de zones
            for (Zone zone : plugin.getZoneManager().getAllZones()) {
                if (zone.getId() == 0) continue; // Skip spawn

                String id = String.valueOf(zone.getId());
                if (id.startsWith(partial)) {
                    completions.add(id);
                }

                // Ajouter aussi les noms
                if (zone.getName().toLowerCase().startsWith(partial)) {
                    completions.add(zone.getName());
                }
            }
        }

        return completions;
    }
}
