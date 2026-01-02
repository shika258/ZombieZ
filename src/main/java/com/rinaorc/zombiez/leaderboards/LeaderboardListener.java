package com.rinaorc.zombiez.leaderboards;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.gui.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

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

    /**
     * Notifie le joueur s'il a des récompenses de leaderboard en attente
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Délai pour laisser le temps au joueur de se connecter complètement
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Vérifier les récompenses en attente de manière async
            plugin.getNewLeaderboardManager().getPendingRewards(player.getUniqueId())
                .thenAccept(rewards -> {
                    if (rewards.isEmpty()) return;

                    // Calculer les totaux
                    long totalPoints = rewards.stream()
                        .mapToLong(LeaderboardManager.PendingReward::getPoints).sum();
                    int totalGems = rewards.stream()
                        .mapToInt(LeaderboardManager.PendingReward::getGems).sum();

                    // Notifier sur le thread principal
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;

                        // Son de notification
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

                        // Title
                        Title title = Title.title(
                            Component.text("§a§l✓ Récompenses disponibles!"),
                            Component.text("§7Tu as §e" + rewards.size() + " §7récompense(s) de classement"),
                            Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofSeconds(3),
                                Duration.ofMillis(500)
                            )
                        );
                        player.showTitle(title);

                        // Message détaillé dans le chat
                        player.sendMessage("");
                        player.sendMessage("§8§m                                                  ");
                        player.sendMessage("§6§l  🏆 RÉCOMPENSES DE CLASSEMENT §6§l🏆");
                        player.sendMessage("");
                        player.sendMessage("  §7Tu as §e§l" + rewards.size() + " §7récompense(s) à réclamer!");
                        player.sendMessage("");
                        player.sendMessage("  §6Total disponible:");
                        if (totalPoints > 0) {
                            player.sendMessage("    §f▸ §e" + formatNumber(totalPoints) + " points");
                        }
                        if (totalGems > 0) {
                            player.sendMessage("    §f▸ §d" + totalGems + " gemmes");
                        }
                        player.sendMessage("");
                        player.sendMessage("  §a§l➤ §e/lb rewards §7pour réclamer");
                        player.sendMessage("§8§m                                                  ");
                        player.sendMessage("");
                    });
                });
        }, 60L); // 3 secondes après la connexion
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}
