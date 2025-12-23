package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire central du système de recyclage automatique
 * Gère le recyclage des items, le calcul des points et les notifications
 */
public class RecycleManager implements Listener {

    private final ZombieZPlugin plugin;

    // Cache des paramètres de recyclage par joueur
    private final Map<UUID, RecycleSettings> playerSettings;

    // Tâche de résumé toutes les minutes
    private BukkitTask summaryTask;

    // Points de base par rareté (équilibrage progressif)
    @Getter
    private static final int[] BASE_POINTS_BY_RARITY = {
        2,      // COMMON (60% drop) - Points faibles mais fréquents
        5,      // UNCOMMON (25% drop) - 2.5x common
        15,     // RARE (10% drop) - 3x uncommon
        40,     // EPIC (4% drop) - 2.7x rare
        100,    // LEGENDARY (0.9% drop) - 2.5x epic
        300,    // MYTHIC (0.1% drop) - 3x legendary
        1000    // EXALTED (0.01% drop) - 3.3x mythic
    };

    public RecycleManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.playerSettings = new ConcurrentHashMap<>();

        // Enregistrer les événements
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Démarrer la tâche de résumé toutes les minutes
        startSummaryTask();
    }

    /**
     * Obtient les paramètres de recyclage d'un joueur
     */
    public RecycleSettings getSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, id -> new RecycleSettings());
    }

    /**
     * Charge les paramètres depuis les données du joueur
     */
    public void loadSettings(UUID playerId, String serializedData) {
        RecycleSettings settings = RecycleSettings.deserialize(serializedData);
        playerSettings.put(playerId, settings);
    }

    /**
     * Sauvegarde les paramètres en chaîne sérialisée
     */
    public String saveSettings(UUID playerId) {
        RecycleSettings settings = playerSettings.get(playerId);
        return settings != null ? settings.serialize() : "";
    }

    /**
     * Calcule les points de recyclage pour un item
     *
     * Formule: basePoints * (1 + zoneLevel * 0.15)
     * - Zone 1: x1.15
     * - Zone 10: x2.5
     * - Zone 25: x4.75
     * - Zone 50: x8.5
     *
     * @param rarity Rareté de l'item
     * @param zoneLevel Niveau de zone de l'item
     * @return Points gagnés
     */
    public int calculateRecyclePoints(Rarity rarity, int zoneLevel) {
        int basePoints = BASE_POINTS_BY_RARITY[rarity.ordinal()];

        // Multiplicateur de zone: progression significative
        double zoneMultiplier = 1.0 + (zoneLevel * 0.15);

        return (int) Math.round(basePoints * zoneMultiplier);
    }

    /**
     * Recycle un item et donne les points au joueur
     *
     * @param player Le joueur qui recycle
     * @param item L'ItemStack à recycler
     * @return Les points gagnés, ou 0 si non recyclable
     */
    public int recycleItem(Player player, ItemStack item) {
        if (!ZombieZItem.isZombieZItem(item)) {
            return 0;
        }

        Rarity rarity = ZombieZItem.getItemRarity(item);
        int zoneLevel = ZombieZItem.getItemZoneLevel(item);

        if (rarity == null) {
            return 0;
        }

        int points = calculateRecyclePoints(rarity, zoneLevel);

        // Appliquer le multiplicateur VIP si applicable
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null) {
            double vipMultiplier = playerData.getPointsMultiplier();
            points = (int) Math.round(points * vipMultiplier);

            // Ajouter les points au joueur
            playerData.addPoints(points);
            playerData.addTotalPointsEarned(points);

            // Mettre à jour les stats de recyclage
            RecycleSettings settings = getSettings(player.getUniqueId());
            settings.addRecycledItem(points);
        }

        return points;
    }

    /**
     * Recycle automatiquement un item si les conditions sont remplies
     *
     * @return true si l'item a été recyclé
     */
    public boolean autoRecycleIfEnabled(Player player, ItemStack item) {
        if (!ZombieZItem.isZombieZItem(item)) {
            return false;
        }

        RecycleSettings settings = getSettings(player.getUniqueId());
        if (!settings.isAutoRecycleEnabled()) {
            return false;
        }

        Rarity rarity = ZombieZItem.getItemRarity(item);
        if (rarity == null || !settings.shouldRecycle(rarity)) {
            return false;
        }

        // Recycler l'item
        int points = recycleItem(player, item);

        if (points > 0) {
            // Feedback sonore discret
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
            return true;
        }

        return false;
    }

    /**
     * Écoute les ramassages d'items pour auto-recyclage
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();

        // Vérifier si c'est un item ZombieZ
        if (!ZombieZItem.isZombieZItem(item)) {
            return;
        }

        // Tenter l'auto-recyclage
        if (autoRecycleIfEnabled(player, item)) {
            // Annuler le ramassage - l'item a été recyclé
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    /**
     * Nettoie les données du joueur à la déconnexion
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Les données sont sauvegardées via PlayerData, on peut garder le cache
        // pour une éventuelle reconnexion rapide
    }

    /**
     * Démarre la tâche de résumé toutes les minutes
     */
    private void startSummaryTask() {
        summaryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                RecycleSettings settings = playerSettings.get(player.getUniqueId());
                if (settings == null || !settings.isAutoRecycleEnabled()) {
                    continue;
                }

                long[] stats = settings.resetLastMinuteStats();
                long points = stats[0];
                long items = stats[1];

                if (items > 0) {
                    // Envoyer le résumé en ActionBar
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§a♻ Recyclage: §f" + items + " items §7→ §6+" + formatPoints(points) + " pts"
                    ));

                    // Son de notification discret
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 1.2f);
                }
            }
        }, 20L * 60, 20L * 60); // Toutes les minutes
    }

    /**
     * Formate les points pour affichage
     */
    private String formatPoints(long points) {
        if (points >= 1_000_000) {
            return String.format("%.1fM", points / 1_000_000.0);
        } else if (points >= 1_000) {
            return String.format("%.1fK", points / 1_000.0);
        }
        return String.valueOf(points);
    }

    /**
     * Arrête le système de recyclage
     */
    public void shutdown() {
        if (summaryTask != null) {
            summaryTask.cancel();
        }
    }

    /**
     * Obtient une prévisualisation des points pour une rareté et zone donnée
     */
    public String getPointsPreview(Rarity rarity, int zoneLevel) {
        int minPoints = calculateRecyclePoints(rarity, 1);
        int currentPoints = calculateRecyclePoints(rarity, zoneLevel);
        int maxPoints = calculateRecyclePoints(rarity, 50);

        return String.format("§7Zone 1: §f%d §7| §eCurrent (Z%d): §f%d §7| §6Zone 50: §f%d",
            minPoints, zoneLevel, currentPoints, maxPoints);
    }

    /**
     * Obtient les statistiques de recyclage globales d'un joueur
     */
    public String[] getPlayerStats(UUID playerId) {
        RecycleSettings settings = getSettings(playerId);

        return new String[] {
            "§6♻ §lStatistiques de Recyclage",
            "",
            "§7Session:",
            "  §fItems recyclés: §e" + settings.getSessionItemsRecycled().get(),
            "  §fPoints gagnés: §6" + formatPoints(settings.getSessionPointsEarned().get()),
            "",
            "§7Total (tous temps):",
            "  §fItems recyclés: §e" + settings.getTotalItemsRecycled().get(),
            "  §fPoints gagnés: §6" + formatPoints(settings.getTotalPointsEarned().get()),
            "",
            "§7Raretés recyclées: §f" + settings.getEnabledRaritiesCount() + "/7"
        };
    }
}
