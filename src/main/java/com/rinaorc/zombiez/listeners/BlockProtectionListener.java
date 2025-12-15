package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * Listener pour protéger les blocs de la map
 * Empêche les joueurs de casser ou placer des blocs
 * (map pré-construite)
 */
public class BlockProtectionListener implements Listener {

    private final ZombieZPlugin plugin;

    public BlockProtectionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Empêche les joueurs de casser des blocs
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Les admins peuvent bypass
        if (player.hasPermission("zombiez.admin.bypass")) {
            return;
        }

        // Annuler l'événement
        event.setCancelled(true);
    }

    /**
     * Empêche les joueurs de placer des blocs
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Les admins peuvent bypass
        if (player.hasPermission("zombiez.admin.bypass")) {
            return;
        }

        // Annuler l'événement
        event.setCancelled(true);
    }

    /**
     * Empêche les joueurs de vider des seaux (eau, lave)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        // Les admins peuvent bypass
        if (player.hasPermission("zombiez.admin.bypass")) {
            return;
        }

        // Annuler l'événement
        event.setCancelled(true);
    }

    /**
     * Empêche les joueurs de remplir des seaux
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();

        // Les admins peuvent bypass
        if (player.hasPermission("zombiez.admin.bypass")) {
            return;
        }

        // Annuler l'événement
        event.setCancelled(true);
    }
}
