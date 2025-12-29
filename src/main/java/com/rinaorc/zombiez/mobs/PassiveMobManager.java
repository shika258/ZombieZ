package com.rinaorc.zombiez.mobs;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.mobs.food.FoodItemRegistry;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;

/**
 * Gestionnaire des mobs passifs custom
 * Spawn des cochons, poulets, vaches, moutons en faible quantité
 * qui droppent de la nourriture custom pour se régénérer
 */
public class PassiveMobManager implements Listener {

    private final ZombieZPlugin plugin;

    @Getter
    private final FoodItemRegistry foodRegistry;

    // Tracking des mobs passifs actifs
    private final Map<UUID, PassiveMobData> activeMobs;

    // Configuration des spawns par zone
    private final Map<Integer, ZoneSpawnConfig> zoneConfigs;

    // Limites globales - Augmentées pour plus de nourriture disponible
    private static final int BASE_MAX_MOBS_PER_ZONE = 14;
    private static final int MAX_MOBS_BONUS_HIGH_ZONES = 6; // Bonus pour zones 16+
    private int spawnCheckIntervalTicks = 200; // 10 secondes (doublé)
    private double baseSpawnChance = 0.45; // 45% de chance par check (augmenté)
    private static final double MIN_SPAWN_CHANCE = 0.20; // 20% minimum garanti (doublé)

    private final Random random = new Random();

    public PassiveMobManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.foodRegistry = new FoodItemRegistry();
        this.activeMobs = new ConcurrentHashMap<>();
        this.zoneConfigs = new HashMap<>();

        initializeZoneConfigs();
        startSpawnTask();
    }

    /**
     * Configure les spawns par zone
     */
    private void initializeZoneConfigs() {
        // Zone 0 (Spawn/Refuge) - Pas de mobs
        zoneConfigs.put(0, new ZoneSpawnConfig(0, false, new PassiveMobType[]{}));

        // Zone 1 (Village) - Cochons, Poulets et Lapins communs
        zoneConfigs.put(1, new ZoneSpawnConfig(1, true,
            new PassiveMobType[]{PassiveMobType.PIG, PassiveMobType.CHICKEN, PassiveMobType.RABBIT}));

        // Zone 2 (Plaines) - Vaches, Moutons, Chevaux
        zoneConfigs.put(2, new ZoneSpawnConfig(2, true,
            new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.SHEEP, PassiveMobType.PIG, PassiveMobType.HORSE}));

        // Zone 3 (Désert) - Poulets et Lapins rares
        zoneConfigs.put(3, new ZoneSpawnConfig(3, true,
            new PassiveMobType[]{PassiveMobType.CHICKEN, PassiveMobType.RABBIT}));

        // Zone 4 (Forêt Sombre) - Cochons sauvages et Lapins
        zoneConfigs.put(4, new ZoneSpawnConfig(4, true,
            new PassiveMobType[]{PassiveMobType.PIG, PassiveMobType.COW, PassiveMobType.RABBIT}));

        // Zone 5 (Marécages) - Poulets des marais et Cochons
        zoneConfigs.put(5, new ZoneSpawnConfig(5, true,
            new PassiveMobType[]{PassiveMobType.CHICKEN, PassiveMobType.PIG, PassiveMobType.RABBIT}));

        // Zones 6-8 - Plaines avancées avec chevaux
        for (int i = 6; i <= 8; i++) {
            zoneConfigs.put(i, new ZoneSpawnConfig(i, true,
                new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.PIG, PassiveMobType.SHEEP, PassiveMobType.HORSE}));
        }

        // Zones 9-11 - Montagnes avec chèvres
        for (int i = 9; i <= 11; i++) {
            zoneConfigs.put(i, new ZoneSpawnConfig(i, true,
                new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.SHEEP, PassiveMobType.GOAT}));
        }

        // Zones 12-15 - Hautes terres avec tous les animaux
        for (int i = 12; i <= 15; i++) {
            zoneConfigs.put(i, new ZoneSpawnConfig(i, true,
                new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.SHEEP, PassiveMobType.GOAT, PassiveMobType.HORSE}));
        }

        // Zones 16+ - Régions avancées, tous types disponibles mais plus rares
        for (int i = 16; i <= 50; i++) {
            zoneConfigs.put(i, new ZoneSpawnConfig(i, true,
                new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.PIG, PassiveMobType.SHEEP,
                    PassiveMobType.HORSE, PassiveMobType.RABBIT, PassiveMobType.GOAT}));
        }
    }

    /**
     * Démarre la tâche de spawn périodique
     */
    private void startSpawnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndSpawnMobs();
            }
        }.runTaskTimer(plugin, 200L, spawnCheckIntervalTicks);

        // Tâche de nettoyage des mobs morts
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupDeadMobs();
            }
        }.runTaskTimer(plugin, 100L, 200L);
    }

    /**
     * Vérifie et spawn des mobs pour chaque zone avec des joueurs
     */
    private void checkAndSpawnMobs() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Player player : world.getPlayers()) {
                int zoneId = plugin.getZoneManager().getZoneAt(player.getLocation()).getId();
                ZoneSpawnConfig config = zoneConfigs.get(zoneId);

                if (config == null || !config.canSpawn || config.allowedTypes.length == 0) {
                    continue;
                }

                // Calculer la limite dynamique de mobs pour cette zone
                int maxMobsForZone = calculateMaxMobsForZone(zoneId);

                // Vérifier le nombre de mobs dans la zone
                long currentCount = activeMobs.values().stream()
                    .filter(m -> m.zoneId == zoneId)
                    .count();

                if (currentCount >= maxMobsForZone) {
                    continue;
                }

                // Chance de spawn ajustée par zone avec minimum garanti
                // Formule: décroissance douce qui ne descend jamais sous le minimum
                double adjustedChance = calculateSpawnChance(zoneId);

                if (random.nextDouble() < adjustedChance) {
                    spawnRandomMobNearPlayer(player, config);
                }
            }
        }
    }

    /**
     * Calcule la limite de mobs passifs pour une zone donnée
     * Les zones avancées (16+) ont un bonus pour compenser la difficulté
     */
    private int calculateMaxMobsForZone(int zoneId) {
        if (zoneId >= 30) {
            // Zones très avancées: plus de mobs pour compenser la difficulté
            return BASE_MAX_MOBS_PER_ZONE + MAX_MOBS_BONUS_HIGH_ZONES + 4;
        } else if (zoneId >= 16) {
            // Zones avancées: bonus standard
            return BASE_MAX_MOBS_PER_ZONE + MAX_MOBS_BONUS_HIGH_ZONES;
        } else if (zoneId >= 10) {
            // Zones intermédiaires: petit bonus
            return BASE_MAX_MOBS_PER_ZONE + 4;
        }
        return BASE_MAX_MOBS_PER_ZONE;
    }

    /**
     * Calcule la chance de spawn pour une zone avec minimum garanti
     * Utilise une décroissance logarithmique douce au lieu d'une réduction linéaire
     * qui empêchait tout spawn après la zone 20
     */
    private double calculateSpawnChance(int zoneId) {
        // Formule: chance = base * (1 / (1 + zone * 0.03))
        // Cela donne une décroissance douce qui ne tombe jamais à 0
        double decayFactor = 1.0 / (1.0 + (zoneId * 0.03));
        double chance = baseSpawnChance * decayFactor;

        // Appliquer le minimum garanti
        return Math.max(chance, MIN_SPAWN_CHANCE);
    }

    /**
     * Spawn un mob aléatoire près d'un joueur
     */
    private void spawnRandomMobNearPlayer(Player player, ZoneSpawnConfig config) {
        // Position aléatoire entre 20 et 40 blocs du joueur
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 20 + random.nextDouble() * 20;
        double x = Math.cos(angle) * distance;
        double z = Math.sin(angle) * distance;

        Location spawnLoc = player.getLocation().add(x, 0, z);

        // Trouver le sol
        spawnLoc = findSafeSpawnLocation(spawnLoc);
        if (spawnLoc == null) return;

        // Choisir un type de mob
        PassiveMobType type = config.allowedTypes[random.nextInt(config.allowedTypes.length)];

        spawnPassiveMob(type, spawnLoc, config.zoneId);
    }

    /**
     * Trouve une position de spawn sûre (sur le sol)
     */
    private Location findSafeSpawnLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // Chercher le bloc solide le plus haut
        int highestY = world.getHighestBlockYAt(x, z);
        Location safeLoc = new Location(world, x + 0.5, highestY + 1, z + 0.5);

        // Vérifier que c'est un endroit sûr (pas dans l'eau, la lave, etc.)
        if (safeLoc.getBlock().isLiquid() ||
            safeLoc.clone().add(0, -1, 0).getBlock().isLiquid()) {
            return null;
        }

        return safeLoc;
    }

    /**
     * Spawn un mob passif custom
     */
    public Animals spawnPassiveMob(PassiveMobType type, Location location, int zoneId) {
        if (location.getWorld() == null) return null;

        Animals entity = (Animals) location.getWorld().spawnEntity(location, type.getEntityType());

        // Configuration du mob
        entity.setRemoveWhenFarAway(true);

        // Métadonnées ZombieZ
        entity.setMetadata("zombiez_passive", new FixedMetadataValue(plugin, true));
        entity.setMetadata("zombiez_passive_type", new FixedMetadataValue(plugin, type.name()));
        entity.setMetadata("zombiez_zone", new FixedMetadataValue(plugin, zoneId));

        // Afficher le nom avec la vie
        updatePassiveMobHealthDisplay(entity);
        entity.setCustomNameVisible(true);

        // Enregistrer
        PassiveMobData data = new PassiveMobData(entity.getUniqueId(), type, zoneId);
        activeMobs.put(entity.getUniqueId(), data);

        return entity;
    }

    /**
     * Met à jour l'affichage de vie d'un mob passif
     */
    public void updatePassiveMobHealthDisplay(LivingEntity entity) {
        if (!isZombieZPassiveMob(entity)) return;

        // Récupérer le type
        PassiveMobType type = getPassiveMobType(entity);
        if (type == null) return;

        // Calculer la vie
        var maxHealthAttr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        int currentHealth = (int) Math.ceil(entity.getHealth());
        int maxHealth = maxHealthAttr != null ? (int) Math.ceil(maxHealthAttr.getValue()) : 20;

        // Formater l'affichage
        String healthDisplay = formatHealthDisplay(currentHealth, maxHealth);
        String fullName = type.getDisplayName() + " " + healthDisplay;

        entity.setCustomName(fullName);
        entity.setCustomNameVisible(true);
    }

    /**
     * Formate l'affichage de la vie: "❤ 10/10"
     */
    private String formatHealthDisplay(int currentHealth, int maxHealth) {
        double healthPercent = maxHealth > 0 ? (double) currentHealth / maxHealth : 0;
        String healthColor;
        if (healthPercent > 0.6) {
            healthColor = "§a"; // Vert
        } else if (healthPercent > 0.3) {
            healthColor = "§e"; // Jaune
        } else {
            healthColor = "§c"; // Rouge
        }
        return healthColor + currentHealth + "§7/§a" + maxHealth + " §c❤";
    }

    /**
     * Obtient le type de mob passif depuis une entité
     */
    public PassiveMobType getPassiveMobType(Entity entity) {
        if (!entity.hasMetadata("zombiez_passive_type")) {
            return null;
        }
        try {
            String typeName = entity.getMetadata("zombiez_passive_type").get(0).asString();
            return PassiveMobType.valueOf(typeName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gère la mort d'un mob passif
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPassiveMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Vérifier si c'est un mob passif ZombieZ
        if (!entity.hasMetadata("zombiez_passive")) {
            return;
        }

        // Supprimer les drops vanilla
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Récupérer les données
        PassiveMobData data = activeMobs.remove(entity.getUniqueId());
        if (data == null) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Générer le loot custom avec les FoodItems pour le glow
        List<FoodDropInfo> loot = generateFoodLootWithInfo(data.type, data.zoneId);

        // Dropper les items avec glow et nom visible
        Location dropLoc = entity.getLocation();
        for (FoodDropInfo dropInfo : loot) {
            ItemStack itemStack = dropInfo.foodItem.createItemStack(dropInfo.amount);
            org.bukkit.entity.Item droppedItem = dropLoc.getWorld().dropItemNaturally(dropLoc, itemStack);

            // Appliquer le glow et le nom visible comme les armes/armures
            org.bukkit.ChatColor glowColor = getFoodRarityChatColor(dropInfo.foodItem.getRarity());
            plugin.getItemManager().applyDroppedItemEffects(droppedItem, dropInfo.foodItem.getDisplayName(), glowColor);
        }

        // Message au joueur
        if (!loot.isEmpty()) {
            String itemName = loot.get(0).foodItem.getDisplayName();
            killer.sendMessage("§a🍖 §7Vous avez obtenu: " + loot.get(0).foodItem.getRarity().getColor() + itemName);
        }
    }

    /**
     * Convertit une FoodRarity en ChatColor pour le glow
     */
    private org.bukkit.ChatColor getFoodRarityChatColor(FoodItem.FoodRarity rarity) {
        return switch (rarity) {
            case COMMON -> org.bukkit.ChatColor.WHITE;
            case UNCOMMON -> org.bukkit.ChatColor.GREEN;
            case RARE -> org.bukkit.ChatColor.BLUE;
            case EPIC -> org.bukkit.ChatColor.DARK_PURPLE;
            case LEGENDARY -> org.bukkit.ChatColor.GOLD;
        };
    }

    /**
     * Info pour un drop de nourriture (FoodItem + quantité)
     */
    private record FoodDropInfo(FoodItem foodItem, int amount) {}

    /**
     * Génère le loot de nourriture pour un mob avec les infos FoodItem pour le glow
     */
    private List<FoodDropInfo> generateFoodLootWithInfo(PassiveMobType mobType, int zoneId) {
        List<FoodDropInfo> loot = new ArrayList<>();

        // Obtenir les drops possibles pour ce type de mob
        List<FoodItem> possibleDrops = foodRegistry.getDropsForMob(mobType);

        if (possibleDrops.isEmpty()) return loot;

        // Calculer le bonus de qualité basé sur la zone
        double qualityBonus = zoneId * 0.05; // +5% par zone

        // Toujours dropper au moins 1 item commun
        FoodItem guaranteedDrop = foodRegistry.getGuaranteedDrop(mobType);
        if (guaranteedDrop != null) {
            int amount = 1 + random.nextInt(2); // 1-2 items (réduit de 1-3)
            loot.add(new FoodDropInfo(guaranteedDrop, amount));
        }

        // Chance de drop rare (5% base + bonus zone) - réduit de 8%
        double rareChance = 0.05 + qualityBonus;
        if (random.nextDouble() < rareChance) {
            FoodItem rareDrop = foodRegistry.getRareDrop(mobType);
            if (rareDrop != null) {
                loot.add(new FoodDropInfo(rareDrop, 1));
            }
        }

        // Chance de drop épique (2% base + bonus zone × 0.5) - réduit de 4%
        double epicChance = 0.02 + (qualityBonus * 0.5);
        if (random.nextDouble() < epicChance) {
            FoodItem epicDrop = foodRegistry.getEpicDrop(mobType);
            if (epicDrop != null) {
                loot.add(new FoodDropInfo(epicDrop, 1));
            }
        }

        // Chance de drop légendaire (1% base + bonus zone × 0.3) - réduit de 2%
        double legendaryChance = 0.01 + (qualityBonus * 0.3);
        if (random.nextDouble() < legendaryChance) {
            FoodItem legendaryDrop = foodRegistry.getLegendaryDrop(mobType);
            if (legendaryDrop != null) {
                loot.add(new FoodDropInfo(legendaryDrop, 1));
            }
        }

        return loot;
    }

    /**
     * Nettoie les mobs morts ou invalides du tracking
     */
    private void cleanupDeadMobs() {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, PassiveMobData> entry : activeMobs.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (entity == null || !entity.isValid() || entity.isDead()) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            activeMobs.remove(id);
        }
    }

    /**
     * Vérifie si une entité est un mob passif ZombieZ
     */
    public boolean isZombieZPassiveMob(Entity entity) {
        return entity.hasMetadata("zombiez_passive");
    }

    /**
     * Obtient le nombre total de mobs passifs actifs
     */
    public int getTotalMobCount() {
        return activeMobs.size();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE CLEANUP (CLEARLAG)
    // ═══════════════════════════════════════════════════════════════════════════

    private static final double PLAYER_NEARBY_RADIUS = 64.0;     // Rayon pour joueur proche
    private static final long MOB_MAX_AGE_MS = 5 * 60 * 1000;    // 5 minutes max d'existence si isolé
    private static final int CLEANUP_BATCH_SIZE = 30;            // Nombre max de mobs à traiter par tick

    /**
     * Système de clearlag pour les mobs passifs - appelé périodiquement par PerformanceManager
     * Nettoie les mobs distants, isolés ou invalides
     */
    public void cleanupDistantMobs() {
        List<UUID> toRemove = new ArrayList<>();
        int processed = 0;

        for (Map.Entry<UUID, PassiveMobData> entry : activeMobs.entrySet()) {
            if (processed >= CLEANUP_BATCH_SIZE) break;
            processed++;

            UUID entityId = entry.getKey();
            PassiveMobData data = entry.getValue();
            Entity entity = plugin.getServer().getEntity(entityId);

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 1: Entité invalide ou morte
            // ═══════════════════════════════════════════════════════════════════
            if (entity == null || !entity.isValid() || entity.isDead()) {
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 2: Chunk non chargé
            // ═══════════════════════════════════════════════════════════════════
            if (!entity.getLocation().getChunk().isLoaded()) {
                entity.remove();
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 3: Pas de joueur à proximité
            // ═══════════════════════════════════════════════════════════════════
            if (!hasPlayerNearby(entity, PLAYER_NEARBY_RADIUS)) {
                entity.remove();
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 4: Mob isolé depuis trop longtemps
            // ═══════════════════════════════════════════════════════════════════
            long age = System.currentTimeMillis() - data.spawnTime;
            if (age > MOB_MAX_AGE_MS) {
                entity.remove();
                toRemove.add(entityId);
            }
        }

        // Nettoyer les entrées
        for (UUID id : toRemove) {
            activeMobs.remove(id);
        }

        // Log si beaucoup de mobs nettoyés (debug mode only)
        if (toRemove.size() > 5 && plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[Clearlag Passifs] " + toRemove.size() + " mobs passifs nettoyés");
        }
    }

    /**
     * Vérifie si un joueur est à proximité d'une entité
     */
    private boolean hasPlayerNearby(Entity entity, double radius) {
        double radiusSq = radius * radius;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(entity.getWorld())) {
                if (player.getLocation().distanceSquared(entity.getLocation()) <= radiusSq) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Nettoie tous les mobs passifs ZombieZ dans un chunk
     * Appelé par ChunkUnloadListener lors du déchargement
     *
     * @param chunk Le chunk à nettoyer
     * @return Le nombre de mobs nettoyés
     */
    public int cleanupChunk(Chunk chunk) {
        int cleaned = 0;

        for (Entity entity : chunk.getEntities()) {
            if (isZombieZPassiveMob(entity)) {
                activeMobs.remove(entity.getUniqueId());
                entity.remove();
                cleaned++;
            }
        }

        return cleaned;
    }

    /**
     * Force le nettoyage de tous les mobs passifs (commande admin)
     *
     * @return Le nombre de mobs supprimés
     */
    public int forceCleanupAll() {
        int cleaned = 0;

        for (Map.Entry<UUID, PassiveMobData> entry : activeMobs.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (entity != null && entity.isValid()) {
                entity.remove();
                cleaned++;
            }
        }

        activeMobs.clear();
        return cleaned;
    }

    /**
     * Obtient les données d'un mob passif actif
     */
    public PassiveMobData getActiveMob(UUID entityId) {
        return activeMobs.get(entityId);
    }

    /**
     * Types de mobs passifs
     */
    public enum PassiveMobType {
        PIG(EntityType.PIG, "§d🐷 Cochon Sauvage", "pig"),
        CHICKEN(EntityType.CHICKEN, "§f🐔 Poulet Fermier", "chicken"),
        COW(EntityType.COW, "§6🐄 Vache Paisible", "cow"),
        SHEEP(EntityType.SHEEP, "§f🐑 Mouton Laineux", "sheep"),
        HORSE(EntityType.HORSE, "§e🐴 Cheval Sauvage", "horse"),
        RABBIT(EntityType.RABBIT, "§f🐰 Lapin des Plaines", "rabbit"),
        GOAT(EntityType.GOAT, "§7🐐 Chèvre de Montagne", "goat");

        @Getter
        private final EntityType entityType;
        @Getter
        private final String displayName;
        @Getter
        private final String id;

        PassiveMobType(EntityType entityType, String displayName, String id) {
            this.entityType = entityType;
            this.displayName = displayName;
            this.id = id;
        }
    }

    /**
     * Données d'un mob passif actif
     */
    public static class PassiveMobData {
        public final UUID entityId;
        public final PassiveMobType type;
        public final int zoneId;
        public final long spawnTime;

        PassiveMobData(UUID entityId, PassiveMobType type, int zoneId) {
            this.entityId = entityId;
            this.type = type;
            this.zoneId = zoneId;
            this.spawnTime = System.currentTimeMillis();
        }
    }

    /**
     * Configuration de spawn par zone
     */
    private static class ZoneSpawnConfig {
        final int zoneId;
        final boolean canSpawn;
        final PassiveMobType[] allowedTypes;

        ZoneSpawnConfig(int zoneId, boolean canSpawn, PassiveMobType[] allowedTypes) {
            this.zoneId = zoneId;
            this.canSpawn = canSpawn;
            this.allowedTypes = allowedTypes;
        }
    }
}
