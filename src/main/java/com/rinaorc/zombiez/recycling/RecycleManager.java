package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableRarity;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.mobs.food.FoodItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
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

    // Cooldown pour le rappel d'inventaire plein (évite le spam)
    private final Map<UUID, Long> inventoryFullReminderCooldown;

    // Durée du cooldown en millisecondes (30 secondes)
    private static final long REMINDER_COOLDOWN_MS = 30_000L;

    // Tâche de résumé toutes les minutes
    private BukkitTask summaryTask;

    // Points de base par rareté (équilibrage progressif)
    public static final int[] BASE_POINTS_BY_RARITY = {
        2,      // COMMON (60% drop) - Points faibles mais fréquents
        5,      // UNCOMMON (25% drop) - 2.5x common
        15,     // RARE (10% drop) - 3x uncommon
        40,     // EPIC (4% drop) - 2.7x rare
        100,    // LEGENDARY (0.9% drop) - 2.5x epic
        300,    // MYTHIC (0.1% drop) - 3x legendary
        1000    // EXALTED (0.01% drop) - 3.3x mythic
    };

    // Points de base pour les consommables par rareté
    public static final int[] CONSUMABLE_POINTS_BY_RARITY = {
        3,      // COMMON - Légèrement plus que les items normaux
        8,      // UNCOMMON
        20,     // RARE
        50,     // EPIC
        150     // LEGENDARY
    };

    public RecycleManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.playerSettings = new ConcurrentHashMap<>();
        this.inventoryFullReminderCooldown = new ConcurrentHashMap<>();

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
     * Calcule les points de recyclage pour un consommable
     *
     * @param rarity Rareté du consommable
     * @param zoneLevel Niveau de zone du consommable
     * @return Points gagnés
     */
    public int calculateConsumableRecyclePoints(ConsumableRarity rarity, int zoneLevel) {
        int basePoints = CONSUMABLE_POINTS_BY_RARITY[rarity.ordinal()];

        // Multiplicateur de zone: progression significative
        double zoneMultiplier = 1.0 + (zoneLevel * 0.15);

        return (int) Math.round(basePoints * zoneMultiplier);
    }

    /**
     * Recycle un consommable et donne les points au joueur
     *
     * @param player Le joueur qui recycle
     * @param item L'ItemStack consommable à recycler
     * @return Les points gagnés, ou 0 si non recyclable
     */
    public int recycleConsumable(Player player, ItemStack item) {
        Consumable consumable = Consumable.fromItemStack(item);
        if (consumable == null) {
            return 0;
        }

        int points = calculateConsumableRecyclePoints(consumable.getRarity(), consumable.getZoneId());

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

            // Tracker pour le parcours (Journey)
            notifyJourneyProgress(player, settings);

            // Tracker pour la mission ITEMS_RECYCLED
            plugin.getMissionManager().updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.ITEMS_RECYCLED, 1);
        }

        return points;
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

            // Tracker pour le parcours (Journey)
            notifyJourneyProgress(player, settings);
        }

        return points;
    }

    /**
     * Vérifie si un ItemStack est une nourriture ZombieZ
     */
    public boolean isFoodItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(FoodItem.FOOD_KEY, PersistentDataType.STRING);
    }

    /**
     * Obtient l'ID de nourriture depuis un ItemStack
     */
    public String getFoodId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(FoodItem.FOOD_KEY, PersistentDataType.STRING);
    }

    /**
     * Recycle une nourriture et donne les points au joueur
     *
     * @param player Le joueur qui recycle
     * @param item L'ItemStack nourriture à recycler
     * @return Les points gagnés, ou 0 si non recyclable
     */
    public int recycleFood(Player player, ItemStack item) {
        String foodId = getFoodId(item);
        if (foodId == null) return 0;

        // Obtenir le FoodItemRegistry
        var passiveMobManager = plugin.getPassiveMobManager();
        if (passiveMobManager == null) return 0;

        FoodItemRegistry registry = passiveMobManager.getFoodRegistry();
        if (registry == null) return 0;

        FoodItem foodItem = registry.getItem(foodId);
        if (foodItem == null) return 0;

        // Convertir FoodRarity en index pour les points (même échelle que ConsumableRarity)
        int rarityIndex = foodItem.getRarity().ordinal();
        if (rarityIndex >= CONSUMABLE_POINTS_BY_RARITY.length) {
            rarityIndex = CONSUMABLE_POINTS_BY_RARITY.length - 1;
        }

        // Utiliser les mêmes points que les consommables
        int basePoints = CONSUMABLE_POINTS_BY_RARITY[rarityIndex];

        // Pas de zone level pour la nourriture, utiliser zone 1 comme base
        int points = basePoints;

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

            // Tracker pour le parcours (Journey)
            notifyJourneyProgress(player, settings);

            // Tracker pour la mission ITEMS_RECYCLED
            plugin.getMissionManager().updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.ITEMS_RECYCLED, 1);
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

        // Vérifier si c'est un consommable ZombieZ
        if (Consumable.isConsumable(item)) {
            RecycleSettings settings = getSettings(player.getUniqueId());
            if (settings.shouldRecycleConsumables()) {
                int points = recycleConsumable(player, item);
                if (points > 0) {
                    event.setCancelled(true);
                    event.getItem().remove();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                    return;
                }
            }
            // Pas recyclé, vérifier l'inventaire
            checkInventoryFullReminder(player);
            return;
        }

        // Vérifier si c'est une nourriture ZombieZ (recyclée avec le même toggle que les consommables)
        if (isFoodItem(item)) {
            RecycleSettings settings = getSettings(player.getUniqueId());
            if (settings.shouldRecycleConsumables()) {
                int points = recycleFood(player, item);
                if (points > 0) {
                    event.setCancelled(true);
                    event.getItem().remove();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                    return;
                }
            }
            // Pas recyclé, vérifier l'inventaire
            checkInventoryFullReminder(player);
            return;
        }

        // Vérifier si c'est un item ZombieZ (non-consommable)
        if (!ZombieZItem.isZombieZItem(item)) {
            return;
        }

        // Tenter l'auto-recyclage
        if (autoRecycleIfEnabled(player, item)) {
            // Annuler le ramassage - l'item a été recyclé
            event.setCancelled(true);
            event.getItem().remove();
        } else {
            // L'item n'a pas été recyclé - vérifier si l'inventaire est presque plein
            checkInventoryFullReminder(player);
        }
    }

    /**
     * Vérifie si l'inventaire est presque plein et envoie un rappel
     */
    private void checkInventoryFullReminder(Player player) {
        int emptySlots = countEmptySlots(player);

        // Si 3 slots ou moins disponibles, envoyer un rappel
        if (emptySlots <= 3) {
            sendInventoryFullReminder(player, emptySlots);
        }
    }

    /**
     * Compte les slots vides dans l'inventaire du joueur (hors armure et offhand)
     */
    private int countEmptySlots(Player player) {
        int empty = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                empty++;
            }
        }
        return empty;
    }

    /**
     * Envoie un rappel pour le recyclage si le cooldown est passé
     */
    private void sendInventoryFullReminder(Player player, int emptySlots) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Vérifier le cooldown
        Long lastReminder = inventoryFullReminderCooldown.get(playerId);
        if (lastReminder != null && (now - lastReminder) < REMINDER_COOLDOWN_MS) {
            return; // Encore en cooldown
        }

        // Mettre à jour le cooldown
        inventoryFullReminderCooldown.put(playerId, now);

        // Vérifier si le recyclage auto est déjà activé
        RecycleSettings settings = getSettings(playerId);

        if (emptySlots == 0) {
            // Inventaire complètement plein
            player.sendMessage("§c§l⚠ §cInventaire plein!");
            if (!settings.isAutoRecycleEnabled()) {
                player.sendMessage("§7Activez le recyclage automatique avec §e/recycle §7pour libérer de l'espace!");
            } else {
                player.sendMessage("§7Configurez plus de raretés à recycler avec §e/recycle§7!");
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
        } else {
            // Presque plein (1-3 slots)
            player.sendMessage("§6§l⚠ §eInventaire presque plein! §7(" + emptySlots + " slots restants)");
            if (!settings.isAutoRecycleEnabled()) {
                player.sendMessage("§7Utilisez §e/recycle §7pour activer le recyclage automatique.");
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
        }
    }

    /**
     * Sauvegarde et nettoie les données du joueur à la déconnexion
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Sauvegarder les settings dans PlayerData avant déconnexion
        syncToPlayerData(playerId);

        // Nettoyer les caches
        playerSettings.remove(playerId);
        inventoryFullReminderCooldown.remove(playerId);
    }

    /**
     * Charge les paramètres depuis PlayerData au login
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // Chargement différé pour s'assurer que PlayerData est chargé
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            syncFromPlayerData(event.getPlayer().getUniqueId());
        }, 5L);
    }

    /**
     * Synchronise les settings depuis PlayerData vers le cache
     */
    public void syncFromPlayerData(UUID playerId) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(playerId);
        if (playerData != null && playerData.getRecycleSettingsData() != null) {
            String data = playerData.getRecycleSettingsData();
            if (!data.isEmpty()) {
                RecycleSettings settings = RecycleSettings.deserialize(data);
                playerSettings.put(playerId, settings);
            }
        }
    }

    /**
     * Synchronise les settings du cache vers PlayerData
     */
    public void syncToPlayerData(UUID playerId) {
        RecycleSettings settings = playerSettings.get(playerId);
        if (settings != null) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayer(playerId);
            if (playerData != null) {
                playerData.setRecycleSettingsData(settings.serialize());
                playerData.markDirty();
            }
        }
    }

    /**
     * Démarre la tâche de recyclage automatique et résumé toutes les minutes
     */
    private void startSummaryTask() {
        summaryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                RecycleSettings settings = playerSettings.get(player.getUniqueId());
                if (settings == null) {
                    continue;
                }

                // Synchroniser vers PlayerData périodiquement
                syncToPlayerData(player.getUniqueId());

                if (!settings.isAutoRecycleEnabled()) {
                    continue;
                }

                // Scanner et recycler les items dans l'inventaire
                recycleInventoryItems(player, settings);

                // Récupérer les stats de la dernière minute (incluant le recyclage d'inventaire)
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

    // Constante pour la taille de la hotbar
    private static final int HOTBAR_SIZE = 9;

    /**
     * Recycle automatiquement les items dans l'inventaire du joueur
     * selon ses paramètres de recyclage
     *
     * OPTIMISÉ: Calcul batch par stack au lieu de par item
     */
    private void recycleInventoryItems(Player player, RecycleSettings settings) {
        ItemStack[] contents = player.getInventory().getStorageContents();

        // Pré-charger PlayerData une seule fois
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Vérifier si la protection de la hotbar est activée
        boolean protectHotbar = settings.isProtectHotbarEnabled();

        double vipMultiplier = playerData.getPointsMultiplier();
        int totalRecycled = 0;
        int totalPoints = 0;
        int bestSingleRecycle = 0;

        for (int i = 0; i < contents.length; i++) {
            // Protéger la hotbar (slots 0-8) si l'option est activée
            if (protectHotbar && i < HOTBAR_SIZE) {
                continue;
            }

            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            int pointsPerItem = 0;
            int amount = item.getAmount();

            // Vérifier si c'est un consommable
            if (Consumable.isConsumable(item)) {
                if (!settings.shouldRecycleConsumables()) {
                    continue;
                }
                Consumable consumable = Consumable.fromItemStack(item);
                if (consumable != null) {
                    pointsPerItem = calculateConsumableRecyclePoints(consumable.getRarity(), consumable.getZoneId());
                    pointsPerItem = (int) Math.round(pointsPerItem * vipMultiplier);
                }
            }
            // Vérifier si c'est une nourriture ZombieZ (recyclée avec le toggle consommables)
            else if (isFoodItem(item)) {
                if (!settings.shouldRecycleConsumables()) {
                    continue;
                }
                String foodId = getFoodId(item);
                if (foodId != null) {
                    var passiveMobManager = plugin.getPassiveMobManager();
                    if (passiveMobManager != null) {
                        FoodItemRegistry registry = passiveMobManager.getFoodRegistry();
                        if (registry != null) {
                            FoodItem foodItem = registry.getItem(foodId);
                            if (foodItem != null) {
                                int rarityIndex = foodItem.getRarity().ordinal();
                                if (rarityIndex >= CONSUMABLE_POINTS_BY_RARITY.length) {
                                    rarityIndex = CONSUMABLE_POINTS_BY_RARITY.length - 1;
                                }
                                pointsPerItem = CONSUMABLE_POINTS_BY_RARITY[rarityIndex];
                                pointsPerItem = (int) Math.round(pointsPerItem * vipMultiplier);
                            }
                        }
                    }
                }
            }
            // Sinon, vérifier si c'est un item ZombieZ recyclable
            else if (ZombieZItem.isZombieZItem(item)) {
                Rarity rarity = ZombieZItem.getItemRarity(item);
                if (rarity == null || !settings.shouldRecycle(rarity)) {
                    continue;
                }

                int zoneLevel = ZombieZItem.getItemZoneLevel(item);
                pointsPerItem = calculateRecyclePoints(rarity, zoneLevel);
                pointsPerItem = (int) Math.round(pointsPerItem * vipMultiplier);
            } else {
                continue;
            }

            if (pointsPerItem <= 0) {
                continue;
            }

            int stackPoints = pointsPerItem * amount;

            totalPoints += stackPoints;
            totalRecycled += amount;

            // Tracker le meilleur recyclage unique (par item, pas par stack)
            if (pointsPerItem > bestSingleRecycle) {
                bestSingleRecycle = pointsPerItem;
            }

            // Supprimer l'item de l'inventaire
            player.getInventory().setItem(i, null);
        }

        // Appliquer tous les points en une seule fois
        if (totalRecycled > 0) {
            playerData.addPoints(totalPoints);
            playerData.addTotalPointsEarned(totalPoints);

            // Mettre à jour les stats de recyclage en batch
            settings.addRecycledItemsBatch(totalRecycled, totalPoints);

            // Tracker pour le parcours (Journey)
            notifyJourneyProgress(player, settings);

            // Feedback sonore
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.3f);
        }
    }

    /**
     * Formate les points pour affichage
     */
    public static String formatPoints(long points) {
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

        // Sauvegarder tous les settings avant arrêt
        for (UUID playerId : playerSettings.keySet()) {
            syncToPlayerData(playerId);
        }
        playerSettings.clear();
    }

    /**
     * Synchronise tous les joueurs en ligne vers PlayerData
     * Appelé périodiquement pour éviter la perte de données
     */
    public void syncAllToPlayerData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncToPlayerData(player.getUniqueId());
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
            "§7Raretés recyclées: §f" + settings.getEnabledRaritiesCount() + "/7",
            "",
            "§7Utilisez §e/recycle §7pour ouvrir le menu."
        };
    }

    /**
     * Notifie le système de parcours (Journey) de la progression du recyclage
     */
    private void notifyJourneyProgress(Player player, RecycleSettings settings) {
        var journeyListener = plugin.getJourneyListener();
        if (journeyListener != null) {
            int totalRecycled = (int) settings.getTotalItemsRecycled().get();
            journeyListener.onRecycleItems(player, totalRecycled);
        }
    }
}
