package com.rinaorc.zombiez.leaderboards;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Listener centralisé pour tous les GUIs de leaderboard
 * Gère les clics et interactions dans les menus de classement
 */
public class LeaderboardListener implements Listener {

    private final ZombieZPlugin plugin;

    public LeaderboardListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        var holder = event.getInventory().getHolder();

        // LeaderboardMainGUI - Menu principal
        if (holder instanceof LeaderboardMainGUI gui) {
            gui.handleClick(event);
            return;
        }

        // LeaderboardCategoryGUI - Catégorie de classements
        if (holder instanceof LeaderboardCategoryGUI gui) {
            gui.handleClick(event);
            return;
        }

        // LeaderboardDetailGUI - Détails d'un classement
        if (holder instanceof LeaderboardDetailGUI gui) {
            gui.handleClick(event);
            return;
        }

        // LeaderboardPlayerGUI - Profil d'un joueur
        if (holder instanceof LeaderboardPlayerGUI gui) {
            gui.handleClick(event);
            return;
        }

        // LeaderboardRewardsGUI - Récompenses
        if (holder instanceof LeaderboardRewardsGUI gui) {
            gui.handleClick(event);
            return;
        }

        // LeaderboardCompareGUI - Comparaison de joueurs
        if (holder instanceof LeaderboardCompareGUI gui) {
            gui.handleClick(event);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        var holder = event.getInventory().getHolder();

        // Bloquer le drag dans tous les GUIs de leaderboard
        if (holder instanceof LeaderboardMainGUI ||
            holder instanceof LeaderboardCategoryGUI ||
            holder instanceof LeaderboardDetailGUI ||
            holder instanceof LeaderboardPlayerGUI ||
            holder instanceof LeaderboardRewardsGUI ||
            holder instanceof LeaderboardCompareGUI) {
            event.setCancelled(true);
        }
    }
}
