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

    // Limites globales
    private int maxMobsPerZone = 8;
    private int spawnCheckIntervalTicks = 600; // 30 secondes
    private double baseSpawnChance = 0.15; // 15% de chance par check

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

        // Zone 1 (Village) - Cochons et Poulets communs
        zoneConfigs.put(1, new ZoneSpawnConfig(1, true,
            new PassiveMobType[]{PassiveMobType.PIG, PassiveMobType.CHICKEN}));

        // Zone 2 (Plaines) - Vaches et Moutons
        zoneConfigs.put(2, new ZoneSpawnConfig(2, true,
            new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.SHEEP, PassiveMobType.PIG}));

        // Zone 3 (Désert) - Poulets rares
        zoneConfigs.put(3, new ZoneSpawnConfig(3, true,
            new PassiveMobType[]{PassiveMobType.CHICKEN}));

        // Zone 4 (Forêt Sombre) - Cochons sauvages
        zoneConfigs.put(4, new ZoneSpawnConfig(4, true,
            new PassiveMobType[]{PassiveMobType.PIG, PassiveMobType.COW}));

        // Zone 5 (Marécages) - Poulets des marais
        zoneConfigs.put(5, new ZoneSpawnConfig(5, true,
            new PassiveMobType[]{PassiveMobType.CHICKEN, PassiveMobType.PIG}));

        // Zones 6+ - Plus rares mais meilleure qualité
        for (int i = 6; i <= 11; i++) {
            zoneConfigs.put(i, new ZoneSpawnConfig(i, true,
                new PassiveMobType[]{PassiveMobType.COW, PassiveMobType.PIG, PassiveMobType.SHEEP}));
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

                // Vérifier le nombre de mobs dans la zone
                long currentCount = activeMobs.values().stream()
                    .filter(m -> m.zoneId == zoneId)
                    .count();

                if (currentCount >= maxMobsPerZone) {
                    continue;
                }

                // Chance de spawn ajustée par zone (zones plus élevées = mobs plus rares mais meilleurs)
                double adjustedChance = baseSpawnChance * (1.0 - (zoneId * 0.05));

                if (random.nextDouble() < adjustedChance) {
                    spawnRandomMobNearPlayer(player, config);
                }
            }
        }
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
        entity.setCustomName(type.getDisplayName());
        entity.setCustomNameVisible(false);

        // Métadonnées ZombieZ
        entity.setMetadata("zombiez_passive", new FixedMetadataValue(plugin, true));
        entity.setMetadata("zombiez_passive_type", new FixedMetadataValue(plugin, type.name()));
        entity.setMetadata("zombiez_zone", new FixedMetadataValue(plugin, zoneId));

        // Enregistrer
        PassiveMobData data = new PassiveMobData(entity.getUniqueId(), type, zoneId);
        activeMobs.put(entity.getUniqueId(), data);

        return entity;
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

        // Générer le loot custom
        List<ItemStack> loot = generateFoodLoot(data.type, data.zoneId);

        // Dropper les items
        Location dropLoc = entity.getLocation();
        for (ItemStack item : loot) {
            dropLoc.getWorld().dropItemNaturally(dropLoc, item);
        }

        // Message au joueur
        if (!loot.isEmpty()) {
            String itemName = loot.get(0).getItemMeta() != null ?
                loot.get(0).getItemMeta().getDisplayName() : data.type.getDisplayName();
            killer.sendMessage("§a🍖 §7Vous avez obtenu: " + itemName);
        }
    }

    /**
     * Génère le loot de nourriture pour un mob
     */
    private List<ItemStack> generateFoodLoot(PassiveMobType mobType, int zoneId) {
        List<ItemStack> loot = new ArrayList<>();

        // Obtenir les drops possibles pour ce type de mob
        List<FoodItem> possibleDrops = foodRegistry.getDropsForMob(mobType);

        if (possibleDrops.isEmpty()) return loot;

        // Calculer le bonus de qualité basé sur la zone
        double qualityBonus = zoneId * 0.05; // +5% par zone

        // Toujours dropper au moins 1 item commun
        FoodItem guaranteedDrop = foodRegistry.getGuaranteedDrop(mobType);
        if (guaranteedDrop != null) {
            int amount = 1 + random.nextInt(2); // 1-2 items
            loot.add(guaranteedDrop.createItemStack(amount));
        }

        // Chance de drop rare
        double rareChance = 0.08 + qualityBonus; // 8% base + bonus zone
        if (random.nextDouble() < rareChance) {
            FoodItem rareDrop = foodRegistry.getRareDrop(mobType);
            if (rareDrop != null) {
                loot.add(rareDrop.createItemStack(1));
            }
        }

        // Chance de drop légendaire (très rare)
        double legendaryChance = 0.02 + (qualityBonus * 0.5); // 2% base
        if (random.nextDouble() < legendaryChance) {
            FoodItem legendaryDrop = foodRegistry.getLegendaryDrop(mobType);
            if (legendaryDrop != null) {
                loot.add(legendaryDrop.createItemStack(1));
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

    /**
     * Types de mobs passifs
     */
    public enum PassiveMobType {
        PIG(EntityType.PIG, "§d🐷 Cochon Sauvage", "pig"),
        CHICKEN(EntityType.CHICKEN, "§f🐔 Poulet Fermier", "chicken"),
        COW(EntityType.COW, "§6🐄 Vache Paisible", "cow"),
        SHEEP(EntityType.SHEEP, "§f🐑 Mouton Laineux", "sheep");

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
    private static class PassiveMobData {
        final UUID entityId;
        final PassiveMobType type;
        final int zoneId;
        final long spawnTime;

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
