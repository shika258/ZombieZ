package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Commande /refuge - Affiche les informations sur les refuges
 */
public class RefugeCommand implements CommandExecutor {

    private final ZombieZPlugin plugin;

    public RefugeCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        Zone currentZone = plugin.getZoneManager().getPlayerZone(player);
        int playerZ = player.getLocation().getBlockZ();

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         §e§l🏠 REFUGES");
        player.sendMessage("");
        player.sendMessage("  §7Les refuges offrent:");
        player.sendMessage("  §a✓ §7Zone sécurisée (pas de zombies)");
        player.sendMessage("  §a✓ §7Activation de checkpoint");
        player.sendMessage("  §a✓ §7Marchands et services");
        player.sendMessage("  §a✓ §7Stockage personnel");
        player.sendMessage("");

        // Refuge le plus proche vers le nord
        Zone nearestNorth = findNearestRefugeNorth(playerZ);
        // Refuge le plus proche vers le sud
        Zone nearestSouth = findNearestRefugeSouth(playerZ);

        if (nearestNorth != null) {
            int distance = nearestNorth.getMinZ() - playerZ;
            player.sendMessage("  §a↑ Nord: " + nearestNorth.getColoredName());
            player.sendMessage("    §7Distance: §e" + distance + " blocs");
        }

        if (nearestSouth != null && nearestSouth != nearestNorth) {
            int distance = playerZ - nearestSouth.getMaxZ();
            player.sendMessage("  §c↓ Sud: " + nearestSouth.getColoredName());
            player.sendMessage("    §7Distance: §e" + distance + " blocs");
        }

        // Afficher si le joueur est dans un refuge
        if (currentZone != null && currentZone.getRefugeId() > 0) {
            player.sendMessage("");
            player.sendMessage("  §a✓ §7Vous êtes près du §aRefuge " + currentZone.getRefugeId());
            player.sendMessage("  §7Cherchez le §ebeacon §7pour activer");
            player.sendMessage("  §7le checkpoint!");
        }

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");

        // Envoyer la direction via la boussole (si possible)
        if (nearestNorth != null) {
            MessageUtils.sendActionBar(player, "§a↑ Refuge le plus proche: §e" + 
                (nearestNorth.getMinZ() - playerZ) + " §ablocs au nord");
        }

        return true;
    }

    /**
     * Trouve le refuge le plus proche vers le nord
     */
    private Zone findNearestRefugeNorth(int currentZ) {
        Zone nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Zone zone : plugin.getZoneManager().getAllZones()) {
            if (zone.getRefugeId() > 0 && zone.getMinZ() > currentZ) {
                int distance = zone.getMinZ() - currentZ;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = zone;
                }
            }
        }

        return nearest;
    }

    /**
     * Trouve le refuge le plus proche vers le sud
     */
    private Zone findNearestRefugeSouth(int currentZ) {
        Zone nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Zone zone : plugin.getZoneManager().getAllZones()) {
            if (zone.getRefugeId() > 0 && zone.getMaxZ() < currentZ) {
                int distance = currentZ - zone.getMaxZ();
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = zone;
                }
            }
        }

        return nearest;
    }
}
