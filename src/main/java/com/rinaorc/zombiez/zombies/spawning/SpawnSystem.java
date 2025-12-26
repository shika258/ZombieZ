package com.rinaorc.zombiez.zombies.spawning;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de spawn dynamique des zombies
 * Gère le spawn autour des joueurs selon leur zone
 */
public class SpawnSystem {

    private final ZombieZPlugin plugin;
    private final ZombieManager zombieManager;
    private final Random random;
    
    // Configuration par zone
    private final Map<Integer, ZoneSpawnConfig> zoneConfigs;
    
    // Tables pondérées précalculées par zone (OPTIMISATION)
    private final Map<Integer, WeightedZombieTable> weightedTables;
    
    // Cooldowns de spawn par joueur
    private final Map<UUID, Long> playerSpawnCooldowns;

    // Compteur de spawns pour éviter le lag
    private int spawnsThisTick = 0;
    private static final int MAX_SPAWNS_PER_TICK = 15; // Augmenté de 5 à 15

    // Configuration de densité de spawn
    private static final double MIN_DISTANCE_BETWEEN_ZOMBIES = 5.0; // Distance minimum entre zombies
    private static final int MAX_ZOMBIES_IN_AREA = 8; // Max zombies dans un rayon de 10 blocs
    private static final double DENSITY_CHECK_RADIUS = 10.0; // Rayon pour vérifier la densité

    // ═══════════════════════════════════════════════════════════════════════════
    // ANTI-ACCUMULATION: Limites strictes par joueur
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int MAX_MOBS_PER_PLAYER = 40; // Max mobs autour d'un joueur
    private static final double PLAYER_MOB_CHECK_RADIUS = 48.0; // Rayon de vérification

    // ═══════════════════════════════════════════════════════════════════════════
    // ANTI-AFK: Détection de joueurs inactifs
    // ═══════════════════════════════════════════════════════════════════════════
    private final Map<UUID, Location> lastPlayerLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerAfkSince = new ConcurrentHashMap<>();
    private static final long AFK_THRESHOLD_MS = 30_000; // 30 secondes sans bouger = AFK
    private static final double AFK_MOVEMENT_THRESHOLD = 2.0; // Distance minimale pour reset AFK
    private static final int AFK_MOB_LIMIT = 15; // Limite réduite pour joueurs AFK

    // ═══════════════════════════════════════════════════════════════════════════
    // HARD CAP: Limite globale stricte avec vérification réelle
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int HARD_GLOBAL_CAP = 400; // Limite absolue avant pause de spawn
    private static final int EMERGENCY_CLEANUP_THRESHOLD = 350; // Déclenche cleanup agressif

    // État du système
    @Getter
    private boolean enabled = true;

    @Getter
    private boolean nightBoostActive = false;

    @Getter
    private double spawnMultiplier = 1.0;

    // Zone de spawn protégée (aucun zombie ne spawn dans cette zone)
    private boolean protectedAreaEnabled = false;
    private int protectedMinX, protectedMaxX;
    private int protectedMinY, protectedMaxY;
    private int protectedMinZ, protectedMaxZ;

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE SPÉCIALE: Zone du météore (zombies incendiés - Chapitre 2 Étape 6)
    // ═══════════════════════════════════════════════════════════════════════════
    private static final int FIRE_ZONE_MIN_X = 273;
    private static final int FIRE_ZONE_MAX_X = 416;
    private static final int FIRE_ZONE_MIN_Y = 70;
    private static final int FIRE_ZONE_MAX_Y = 103;
    private static final int FIRE_ZONE_MIN_Z = 9449;
    private static final int FIRE_ZONE_MAX_Z = 9550;
    private static final double FIRE_ZOMBIE_SPAWN_CHANCE = 0.85; // 85% de chance de spawn FIRE_ZOMBIE
    private static final double REDUCED_SPAWN_CHANCE_FIRE_ZONE = 0.25; // 25% de spawn normal dans la zone
    private final NamespacedKey FIRE_ZOMBIE_KEY; // Clé PDC pour tracking Journey étape 6

    // Tick spread pour les joueurs - distribue sur 4 ticks (250ms chacun)
    private static final int PLAYER_SPREAD = 4;
    private int playerTickCounter = 0;

    public SpawnSystem(ZombieZPlugin plugin, ZombieManager zombieManager) {
        this.plugin = plugin;
        this.zombieManager = zombieManager;
        this.random = new Random();
        this.zoneConfigs = new HashMap<>();
        this.weightedTables = new HashMap<>();
        this.playerSpawnCooldowns = new ConcurrentHashMap<>();
        this.FIRE_ZOMBIE_KEY = new NamespacedKey(plugin, "fire_zombie");

        initializeZoneConfigs();
        initializeWeightedTables(); // Précalculer les tables
        loadProtectedSpawnArea(); // Charger la zone de spawn protégée
        startSpawnTask();
    }

    /**
     * Charge la configuration de la zone de spawn protégée depuis config.yml
     */
    private void loadProtectedSpawnArea() {
        var config = plugin.getConfig();
        protectedAreaEnabled = config.getBoolean("gameplay.protected-spawn-area.enabled", false);

        if (protectedAreaEnabled) {
            protectedMinX = config.getInt("gameplay.protected-spawn-area.min-x", 0);
            protectedMaxX = config.getInt("gameplay.protected-spawn-area.max-x", 0);
            protectedMinY = config.getInt("gameplay.protected-spawn-area.min-y", 0);
            protectedMaxY = config.getInt("gameplay.protected-spawn-area.max-y", 256);
            protectedMinZ = config.getInt("gameplay.protected-spawn-area.min-z", 0);
            protectedMaxZ = config.getInt("gameplay.protected-spawn-area.max-z", 0);

            plugin.log(java.util.logging.Level.INFO,
                "§a✓ Zone de spawn protégée chargée: X[" + protectedMinX + "-" + protectedMaxX +
                "] Y[" + protectedMinY + "-" + protectedMaxY +
                "] Z[" + protectedMinZ + "-" + protectedMaxZ + "]");
        }
    }

    /**
     * Vérifie si une location est dans la zone de spawn protégée
     */
    private boolean isInProtectedSpawnArea(Location loc) {
        // Vérifier la zone de spawn principale
        if (protectedAreaEnabled) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            if (x >= protectedMinX && x <= protectedMaxX &&
                y >= protectedMinY && y <= protectedMaxY &&
                z >= protectedMinZ && z <= protectedMaxZ) {
                return true;
            }
        }

        // Vérifier les zones de refuges protégées
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager != null && refugeManager.isInAnyRefugeProtectedArea(loc)) {
            return true;
        }

        return false;
    }

    /**
     * Initialise les configurations de spawn par zone
     * Génère dynamiquement les configs pour les 50 zones
     */
    private void initializeZoneConfigs() {
        // Zone 0 - Spawn (pas de zombies)
        zoneConfigs.put(0, new ZoneSpawnConfig(0, 0, 0, 0, 0));

        // Générer les configs pour les 50 zones dynamiquement
        for (int zoneId = 1; zoneId <= 50; zoneId++) {
            // Calculer les paramètres de spawn basés sur la zone
            int maxZombies;
            int spawnRate;
            int minSpawnRadius;
            int maxSpawnRadius;

            // Calcul du niveau de base - basé sur la formule du ZoneManager:
            // minLevel = (zoneId - 1) * 2 + 1, maxLevel = zoneId * 2
            // Ex: Zone 46 -> minLevel = 91, maxLevel = 92
            int baseLevel = (zoneId - 1) * 2 + 1;

            // Essayer d'obtenir le niveau depuis la zone si elle existe
            Zone zone = plugin.getZoneManager() != null ? plugin.getZoneManager().getZoneById(zoneId) : null;
            if (zone != null) {
                baseLevel = zone.getMinZombieLevel();
            }

            // Calcul du nombre max de zombies et taux de spawn selon l'acte
            if (zoneId <= 10) {
                // ACTE I - LES DERNIERS JOURS: densité augmentée pour début de partie dynamique
                maxZombies = 50 + (zoneId * 5);  // 55-100
                spawnRate = 18 + (zoneId * 2);    // 20-38 spawns/min (beaucoup plus rapide)
                minSpawnRadius = 12;              // Plus proche du joueur
                maxSpawnRadius = 25 + zoneId;     // Rayon réduit pour concentration
            } else if (zoneId <= 20) {
                // ACTE II - LA CONTAMINATION: densité croissante, transition depuis Act I
                maxZombies = 70 + ((zoneId - 10) * 4);  // 74-110
                spawnRate = 25 + ((zoneId - 10));        // 26-35 spawns/min
                minSpawnRadius = 14;
                maxSpawnRadius = 35;
            } else if (zoneId <= 30) {
                // ACTE III - LE CHAOS: haute densité
                maxZombies = 90 + ((zoneId - 20) * 4);  // 94-130
                spawnRate = 20 + ((zoneId - 20) / 2);    // 20-25 spawns/min
                minSpawnRadius = 14;
                maxSpawnRadius = 36;
            } else if (zoneId <= 40) {
                // ACTE IV - L'EXTINCTION: densité décroissante, zombies plus forts
                maxZombies = 80 + ((zoneId - 30) * 2);  // 82-100
                spawnRate = 15 + ((zoneId - 30) / 3);    // 15-18 spawns/min
                minSpawnRadius = 14;
                maxSpawnRadius = 34;
            } else {
                // ACTE V - L'ORIGINE DU MAL: densité réduite, zombies très dangereux
                maxZombies = 60 + ((zoneId - 40) * 2);  // 62-80
                spawnRate = 12 + ((zoneId - 40) / 3);    // 12-15 spawns/min
                minSpawnRadius = 12;
                maxSpawnRadius = 30;
            }

            // Ajustements spéciaux pour certaines zones
            if (zoneId == 26) {
                // Zone PVP - moins de zombies pour focus sur le PVP
                maxZombies = 60;
                spawnRate = 12;
            }
            if (zoneId == 50) {
                // Zone Boss finale - densité réduite, zombies très dangereux
                maxZombies = 50;
                spawnRate = 10;
            }

            zoneConfigs.put(zoneId, new ZoneSpawnConfig(
                maxZombies, spawnRate, minSpawnRadius, maxSpawnRadius, baseLevel
            ));
        }
    }

    /**
     * Démarre la tâche de spawn avec spread pour optimisation.
     * Au lieu de traiter 200 joueurs/sec, on traite ~50 joueurs tous les 5 ticks.
     * Gain estimé: ~1 TPS avec 200 joueurs.
     */
    private void startSpawnTask() {
        // Task principal de spawn (toutes les 5 ticks = 250ms)
        // Traite 1/4 des joueurs à chaque tick = même fréquence effective par joueur
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;

                playerTickCounter++;
                int currentBucket = playerTickCounter % PLAYER_SPREAD;

                // Reset le compteur de spawns tous les 4 ticks (1 seconde complète)
                if (currentBucket == 0) {
                    spawnsThisTick = 0;
                    updateNightBoost();
                }

                // Traiter seulement les joueurs du bucket actuel
                List<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers().stream().toList();
                int playerCount = onlinePlayers.size();

                for (int i = 0; i < playerCount; i++) {
                    // Spread: ne traiter que les joueurs dont l'index correspond au bucket
                    if (i % PLAYER_SPREAD == currentBucket) {
                        processPlayerSpawn(onlinePlayers.get(i));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L); // Démarrage après 1s, puis toutes les 250ms
        
        // Task de nettoyage principal (toutes les 30 secondes)
        new BukkitRunnable() {
            @Override
            public void run() {
                zombieManager.cleanupDistantZombies();
                // Synchroniser les compteurs toutes les 30s pour éviter les désynchronisations
                int fixed = zombieManager.synchronizeCounters();
                if (fixed > 5) {
                    plugin.getLogger().info("[Clearlag] Compteurs synchronisés: " + fixed + " entrées invalides corrigées");
                }
            }
        }.runTaskTimer(plugin, 600L, 600L); // 30 secondes

        // Task de nettoyage léger plus fréquent (toutes les 10 secondes)
        // Vérifie uniquement les chunks déchargés et entités invalides
        new BukkitRunnable() {
            @Override
            public void run() {
                zombieManager.cleanupUnloadedChunks();
            }
        }.runTaskTimer(plugin, 200L, 200L); // 10 secondes

        // Task de vérification de surpopulation (toutes les 5 secondes)
        // Nettoie les zones surchargées et synchronise les compteurs
        new BukkitRunnable() {
            @Override
            public void run() {
                performOverpopulationCheck();
            }
        }.runTaskTimer(plugin, 100L, 100L); // 5 secondes

        // Task de cleanup des données AFK (toutes les minutes)
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupAfkData();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // 60 secondes

        plugin.log(java.util.logging.Level.INFO, "§a✓ Système de clearlag et anti-accumulation démarré");
    }

    /**
     * Vérifie et corrige la surpopulation de mobs
     * - Synchronise les compteurs internes avec les entités réelles
     * - Nettoie les zones surchargées
     */
    private void performOverpopulationCheck() {
        // Vérification du hard cap
        int totalMobs = zombieManager.getTotalZombieCount();

        if (totalMobs >= EMERGENCY_CLEANUP_THRESHOLD) {
            // Nettoyer les mobs les plus vieux de chaque zone surchargée
            for (int zoneId = 1; zoneId <= 50; zoneId++) {
                ZoneSpawnConfig config = zoneConfigs.get(zoneId);
                if (config == null) continue;

                int zoneCount = zombieManager.getZombieCount(zoneId);
                int targetLimit = (int) (config.maxZombies * 0.7); // Réduire à 70% de la limite

                if (zoneCount > targetLimit) {
                    int cleaned = zombieManager.cleanupOldestMobsInZone(zoneId, targetLimit);
                    if (cleaned > 0) {
                        plugin.getLogger().info("[AntiAccumulation] Zone " + zoneId + ": " + cleaned + " mobs nettoyés");
                    }
                }
            }
        }

        // Vérifier aussi la surpopulation par joueur
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int mobsAround = countMobsAroundPlayer(player);
            boolean isAfk = playerAfkSince.containsKey(player.getUniqueId())
                && (System.currentTimeMillis() - playerAfkSince.get(player.getUniqueId())) >= AFK_THRESHOLD_MS;

            int limit = isAfk ? AFK_MOB_LIMIT : MAX_MOBS_PER_PLAYER;

            // Si trop de mobs autour d'un joueur AFK, nettoyer les plus éloignés
            if (mobsAround > limit + 10 && isAfk) {
                cleanupExcessMobsAroundPlayer(player, limit);
            }
        }
    }

    /**
     * Nettoie les mobs en excès autour d'un joueur
     * Supprime les mobs les plus éloignés du joueur
     */
    private void cleanupExcessMobsAroundPlayer(Player player, int targetLimit) {
        List<Entity> mobs = new ArrayList<>();

        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), PLAYER_MOB_CHECK_RADIUS, PLAYER_MOB_CHECK_RADIUS, PLAYER_MOB_CHECK_RADIUS)) {
            if (zombieManager.isZombieZMob(entity)) {
                mobs.add(entity);
            }
        }

        if (mobs.size() <= targetLimit) return;

        // Trier par distance décroissante (plus loin en premier)
        Location playerLoc = player.getLocation();
        mobs.sort((a, b) -> Double.compare(
            b.getLocation().distanceSquared(playerLoc),
            a.getLocation().distanceSquared(playerLoc)
        ));

        // Supprimer les excédents (les plus éloignés)
        int toRemove = mobs.size() - targetLimit;
        int removed = 0;

        for (int i = 0; i < toRemove && i < mobs.size(); i++) {
            Entity mob = mobs.get(i);
            // Notifier le ZombieManager avant suppression
            zombieManager.onZombieDeath(mob.getUniqueId(), null);
            mob.remove();
            removed++;
        }

        if (removed > 0) {
            plugin.getLogger().info("[AntiAccumulation] " + removed + " mobs nettoyés autour de " + player.getName() + " (AFK)");
        }
    }

    /**
     * Nettoie les données AFK des joueurs déconnectés
     */
    private void cleanupAfkData() {
        Set<UUID> onlineIds = plugin.getServer().getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .collect(java.util.stream.Collectors.toSet());

        lastPlayerLocations.keySet().removeIf(id -> !onlineIds.contains(id));
        playerAfkSince.keySet().removeIf(id -> !onlineIds.contains(id));
        playerSpawnCooldowns.keySet().removeIf(id -> !onlineIds.contains(id));
    }

    /**
     * Met à jour le boost de nuit
     */
    private void updateNightBoost() {
        World world = plugin.getServer().getWorlds().get(0);
        long time = world.getTime();
        
        // Nuit: 13000 - 23000
        nightBoostActive = time >= 13000 && time <= 23000;
    }

    /**
     * Traite le spawn pour un joueur
     */
    private void processPlayerSpawn(Player player) {
        if (spawnsThisTick >= MAX_SPAWNS_PER_TICK) return;

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 1: Hard cap global - arrêter tout spawn si dépassé
        // ═══════════════════════════════════════════════════════════════════
        int totalMobs = zombieManager.getTotalZombieCount();
        if (totalMobs >= HARD_GLOBAL_CAP) {
            // Déclencher un cleanup d'urgence si on approche la limite
            if (totalMobs >= EMERGENCY_CLEANUP_THRESHOLD) {
                triggerEmergencyCleanup();
            }
            return;
        }

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 2: Détection AFK et mise à jour du statut
        // ═══════════════════════════════════════════════════════════════════
        boolean isAfk = updateAndCheckAfkStatus(player, now);
        int playerMobLimit = isAfk ? AFK_MOB_LIMIT : MAX_MOBS_PER_PLAYER;

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 3: Limite de mobs autour du joueur (ANTI-ACCUMULATION)
        // ═══════════════════════════════════════════════════════════════════
        int mobsAroundPlayer = countMobsAroundPlayer(player);
        if (mobsAroundPlayer >= playerMobLimit) {
            return; // Trop de mobs autour de ce joueur
        }

        // Vérifier le cooldown
        long lastSpawn = playerSpawnCooldowns.getOrDefault(playerId, 0L);

        // Obtenir la zone du joueur
        Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        if (zone == null) return;

        int zoneId = zone.getId();
        ZoneSpawnConfig config = zoneConfigs.get(zoneId);
        if (config == null || config.maxZombies == 0) return;

        // Calculer le cooldown de spawn (plus lent si AFK)
        double spawnInterval = 60000.0 / config.spawnRate; // ms entre spawns
        if (nightBoostActive) {
            spawnInterval *= 0.7; // 30% plus rapide la nuit
        }
        if (isAfk) {
            spawnInterval *= 3.0; // 3x plus lent si AFK
        }

        if (now - lastSpawn < spawnInterval) return;

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 4: Limite de zone (avec vérification réelle)
        // ═══════════════════════════════════════════════════════════════════
        int currentCount = zombieManager.getZombieCount(zoneId);
        if (currentCount >= config.maxZombies) return;

        // Trouver un point de spawn valide
        Location spawnLoc = findValidSpawnLocation(player, config);
        if (spawnLoc == null) return;

        // ═══════════════════════════════════════════════════════════════════
        // ZONE MÉTÉORE: Logique spéciale pour les zombies incendiés
        // ═══════════════════════════════════════════════════════════════════
        boolean inFireZone = isInFireZombieZone(spawnLoc);

        // Dans la zone météore, réduire les spawns normaux (75% ignorés)
        if (inFireZone && random.nextDouble() > REDUCED_SPAWN_CHANCE_FIRE_ZONE) {
            return; // Skip ce spawn pour réduire la densité de mobs normaux
        }

        // Choisir le type de zombie
        ZombieType type;
        if (inFireZone && random.nextDouble() < FIRE_ZOMBIE_SPAWN_CHANCE) {
            // 85% de chance de spawner un FIRE_ZOMBIE dans la zone météore
            type = ZombieType.FIRE_ZOMBIE;
        } else {
            type = selectZombieType(zoneId);
        }

        // Calculer le niveau
        int level = calculateZombieLevel(config.baseLevel, player);

        // Spawner!
        ZombieManager.ActiveZombie zombie = zombieManager.spawnZombie(type, spawnLoc, level);

        if (zombie != null) {
            playerSpawnCooldowns.put(playerId, now);
            spawnsThisTick++;

            // Si c'est un FIRE_ZOMBIE, appliquer les effets visuels
            if (type == ZombieType.FIRE_ZOMBIE) {
                applyFireZombieEffects(zombie);
            }
        }
    }

    /**
     * Vérifie si une location est dans la zone du météore (zombies incendiés)
     */
    private boolean isInFireZombieZone(Location loc) {
        return loc.getX() >= FIRE_ZONE_MIN_X && loc.getX() <= FIRE_ZONE_MAX_X &&
               loc.getY() >= FIRE_ZONE_MIN_Y && loc.getY() <= FIRE_ZONE_MAX_Y &&
               loc.getZ() >= FIRE_ZONE_MIN_Z && loc.getZ() <= FIRE_ZONE_MAX_Z;
    }

    /**
     * Applique les effets visuels de feu aux zombies incendiés
     * Ajoute aussi le marqueur PDC pour le tracking Journey (Chapitre 2 Étape 6)
     */
    private void applyFireZombieEffects(ZombieManager.ActiveZombie activeZombie) {
        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (entity instanceof org.bukkit.entity.Zombie zombie) {
            // Marqueur PDC pour tracking Journey Chapitre 2 Étape 6
            zombie.getPersistentDataContainer().set(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE, (byte) 1);

            // Effet de feu permanent
            zombie.setVisualFire(true);
            zombie.setFireTicks(Integer.MAX_VALUE);

            // Armure de cuir rouge
            org.bukkit.inventory.ItemStack helmet = createFireArmor(org.bukkit.Material.LEATHER_HELMET);
            org.bukkit.inventory.ItemStack chestplate = createFireArmor(org.bukkit.Material.LEATHER_CHESTPLATE);
            org.bukkit.inventory.ItemStack leggings = createFireArmor(org.bukkit.Material.LEATHER_LEGGINGS);
            org.bukkit.inventory.ItemStack boots = createFireArmor(org.bukkit.Material.LEATHER_BOOTS);

            zombie.getEquipment().setHelmet(helmet);
            zombie.getEquipment().setChestplate(chestplate);
            zombie.getEquipment().setLeggings(leggings);
            zombie.getEquipment().setBoots(boots);

            // Pas de drop d'armure
            zombie.getEquipment().setHelmetDropChance(0);
            zombie.getEquipment().setChestplateDropChance(0);
            zombie.getEquipment().setLeggingsDropChance(0);
            zombie.getEquipment().setBootsDropChance(0);

            // Particules de spawn
            zombie.getWorld().spawnParticle(org.bukkit.Particle.FLAME,
                zombie.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.03);
        }
    }

    /**
     * Crée une pièce d'armure de cuir rouge pour les zombies incendiés
     */
    private org.bukkit.inventory.ItemStack createFireArmor(org.bukkit.Material armorType) {
        org.bukkit.inventory.ItemStack armor = new org.bukkit.inventory.ItemStack(armorType);
        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(org.bukkit.Color.fromRGB(180, 30, 30));
            armor.setItemMeta(leatherMeta);
        }
        return armor;
    }

    /**
     * Compte le nombre de mobs ZombieZ autour d'un joueur
     * Utilise getNearbyEntities pour performance
     */
    private int countMobsAroundPlayer(Player player) {
        int count = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getLocation(), PLAYER_MOB_CHECK_RADIUS, PLAYER_MOB_CHECK_RADIUS, PLAYER_MOB_CHECK_RADIUS)) {
            if (zombieManager.isZombieZMob(entity)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Met à jour le statut AFK du joueur et vérifie s'il est AFK
     * @return true si le joueur est considéré AFK
     */
    private boolean updateAndCheckAfkStatus(Player player, long now) {
        UUID playerId = player.getUniqueId();
        Location currentLoc = player.getLocation();
        Location lastLoc = lastPlayerLocations.get(playerId);

        if (lastLoc == null || !lastLoc.getWorld().equals(currentLoc.getWorld())) {
            // Première vérification ou changement de monde
            lastPlayerLocations.put(playerId, currentLoc.clone());
            playerAfkSince.remove(playerId);
            return false;
        }

        // Vérifier si le joueur a bougé
        double distance = lastLoc.distance(currentLoc);
        if (distance > AFK_MOVEMENT_THRESHOLD) {
            // Le joueur a bougé - reset AFK
            lastPlayerLocations.put(playerId, currentLoc.clone());
            playerAfkSince.remove(playerId);
            return false;
        }

        // Le joueur n'a pas bougé - vérifier depuis combien de temps
        Long afkStart = playerAfkSince.get(playerId);
        if (afkStart == null) {
            // Commencer à compter le temps AFK
            playerAfkSince.put(playerId, now);
            return false;
        }

        // Vérifier si le seuil AFK est atteint
        return (now - afkStart) >= AFK_THRESHOLD_MS;
    }

    /**
     * Déclenche un cleanup d'urgence quand il y a trop de mobs
     */
    private void triggerEmergencyCleanup() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int cleaned = zombieManager.forceCleanupAllDistantMobs();
            if (cleaned > 0) {
                plugin.getLogger().warning("[SpawnSystem] Emergency cleanup: " + cleaned + " mobs removed");
            }
        });
    }

    /**
     * Trouve une location de spawn valide
     */
    private Location findValidSpawnLocation(Player player, ZoneSpawnConfig config) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return null;
        
        // Essayer plusieurs fois de trouver un spot valide
        for (int attempt = 0; attempt < 10; attempt++) {
            // Distance aléatoire dans le rayon
            double distance = config.minSpawnRadius + 
                random.nextDouble() * (config.maxSpawnRadius - config.minSpawnRadius);
            
            // Angle aléatoire
            double angle = random.nextDouble() * 2 * Math.PI;
            
            // Calculer les coordonnées
            double x = playerLoc.getX() + distance * Math.cos(angle);
            double z = playerLoc.getZ() + distance * Math.sin(angle);
            
            // Trouver le sol
            Location loc = new Location(world, x, playerLoc.getY(), z);
            Location groundLoc = findGround(loc);
            
            if (groundLoc != null && isValidSpawnLocation(groundLoc, player)) {
                return groundLoc;
            }
        }
        
        return null;
    }

    /**
     * Trouve le sol sous une position
     */
    private Location findGround(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;
        
        int startY = (int) loc.getY();
        
        // Chercher vers le bas
        for (int y = startY; y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt((int) loc.getX(), y, (int) loc.getZ());
            Block above = block.getRelative(BlockFace.UP);
            Block above2 = above.getRelative(BlockFace.UP);
            
            if (block.getType().isSolid() && !above.getType().isSolid() && !above2.getType().isSolid()) {
                return new Location(world, loc.getX(), y + 1, loc.getZ());
            }
        }
        
        // Chercher vers le haut
        for (int y = startY; y < world.getMaxHeight(); y++) {
            Block block = world.getBlockAt((int) loc.getX(), y, (int) loc.getZ());
            Block above = block.getRelative(BlockFace.UP);
            Block above2 = above.getRelative(BlockFace.UP);
            
            if (block.getType().isSolid() && !above.getType().isSolid() && !above2.getType().isSolid()) {
                return new Location(world, loc.getX(), y + 1, loc.getZ());
            }
        }
        
        return null;
    }

    /**
     * Vérifie si une location est valide pour le spawn
     */
    private boolean isValidSpawnLocation(Location loc, Player player) {
        World world = loc.getWorld();
        if (world == null) return false;

        // Vérifier si la location est dans la zone de spawn protégée
        if (isInProtectedSpawnArea(loc)) return false;

        Block block = loc.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        // Vérifier que le sol est solide
        if (!below.getType().isSolid()) return false;

        // Vérifier que l'espace est libre
        if (block.getType().isSolid()) return false;
        if (block.getRelative(BlockFace.UP).getType().isSolid()) return false;

        // Pas dans l'eau (sauf pour les noyés)
        if (block.isLiquid()) return false;

        // Pas trop proche du joueur
        if (loc.distance(player.getLocation()) < 15) return false;

        // Vérifier la luminosité (plus sombre = plus de chance)
        int lightLevel = block.getLightLevel();
        if (lightLevel > 10 && random.nextDouble() > 0.3) return false;

        // ═══════════════════════════════════════════════════════════════════
        // OPTIMISATION: Vérifier la densité de zombies pour éviter le clustering
        // ═══════════════════════════════════════════════════════════════════
        if (!isSpawnDensityValid(loc)) {
            return false;
        }

        return true;
    }

    /**
     * Vérifie si la densité de spawn est acceptable à cette position
     * Évite le clustering de zombies au même endroit
     */
    private boolean isSpawnDensityValid(Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;

        int nearbyZombies = 0;

        // Utiliser getNearbyEntities qui est optimisé par Bukkit
        for (Entity entity : world.getNearbyEntities(loc, DENSITY_CHECK_RADIUS, DENSITY_CHECK_RADIUS, DENSITY_CHECK_RADIUS)) {
            if (zombieManager.isZombieZMob(entity)) {
                // Vérifier la distance minimale
                double distance = entity.getLocation().distance(loc);
                if (distance < MIN_DISTANCE_BETWEEN_ZOMBIES) {
                    // Trop proche d'un autre zombie
                    return false;
                }
                nearbyZombies++;
            }
        }

        // Vérifier le nombre max dans la zone
        return nearbyZombies < MAX_ZOMBIES_IN_AREA;
    }
    
    /**
     * Initialise les tables pondérées précalculées (OPTIMISATION)
     * Appelé UNE SEULE FOIS au démarrage - Support pour 50 zones
     */
    private void initializeWeightedTables() {
        for (int zoneId = 0; zoneId <= 50; zoneId++) {
            final int currentZoneId = zoneId;
            List<ZombieType> validTypes = Arrays.stream(ZombieType.values())
                .filter(t -> t.canSpawnInZone(currentZoneId))
                .filter(t -> !t.isBoss())
                .toList();

            if (validTypes.isEmpty()) {
                validTypes = List.of(ZombieType.WALKER);
            }

            // Construire la table pondérée une seule fois
            WeightedZombieTable table = new WeightedZombieTable(validTypes);
            weightedTables.put(zoneId, table);
        }

        plugin.log(java.util.logging.Level.INFO, "§a✓ Tables de spawn zombie précalculées (50 zones)");
    }

    /**
     * Sélectionne un type de zombie pour une zone (OPTIMISÉ)
     * Utilise les tables précalculées au lieu de reconstruire à chaque spawn
     */
    private ZombieType selectZombieType(int zoneId) {
        WeightedZombieTable table = weightedTables.get(zoneId);
        if (table == null) {
            table = weightedTables.get(1); // Fallback zone 1
        }
        return table.select(random);
    }
    
    /**
     * Table pondérée précalculée pour sélection rapide O(1)
     * Utilise l'algorithme "Alias Method" pour une sélection en temps constant
     */
    private static class WeightedZombieTable {
        private final ZombieType[] types;
        private final int[] alias;
        private final double[] prob;
        private final int size;
        
        WeightedZombieTable(List<ZombieType> validTypes) {
            this.size = validTypes.size();
            this.types = validTypes.toArray(new ZombieType[0]);
            this.alias = new int[size];
            this.prob = new double[size];
            
            // Calculer les poids
            double[] weights = new double[size];
            double totalWeight = 0;
            
            for (int i = 0; i < size; i++) {
                int weight = switch (types[i].getTier()) {
                    case 1 -> 100;
                    case 2 -> 60;
                    case 3 -> 30;
                    case 4 -> 12;
                    case 5 -> 4;
                    default -> 50;
                };
                weights[i] = weight;
                totalWeight += weight;
            }
            
            // Normaliser
            double average = totalWeight / size;
            for (int i = 0; i < size; i++) {
                prob[i] = weights[i] / average;
            }
            
            // Construire la table alias (Vose's algorithm)
            Deque<Integer> small = new ArrayDeque<>();
            Deque<Integer> large = new ArrayDeque<>();
            
            for (int i = 0; i < size; i++) {
                if (prob[i] < 1.0) small.add(i);
                else large.add(i);
            }
            
            while (!small.isEmpty() && !large.isEmpty()) {
                int l = small.poll();
                int g = large.poll();
                alias[l] = g;
                prob[g] = prob[g] + prob[l] - 1.0;
                if (prob[g] < 1.0) small.add(g);
                else large.add(g);
            }
            
            while (!large.isEmpty()) prob[large.poll()] = 1.0;
            while (!small.isEmpty()) prob[small.poll()] = 1.0;
        }
        
        /**
         * Sélection en O(1) - extrêmement rapide
         */
        ZombieType select(Random random) {
            int col = random.nextInt(size);
            return random.nextDouble() < prob[col] ? types[col] : types[alias[col]];
        }
    }

    /**
     * Calcule le niveau d'un zombie
     */
    private int calculateZombieLevel(int baseLevel, Player player) {
        // Niveau basé sur la zone + légère variation
        int level = baseLevel + random.nextInt(3) - 1;
        
        // Bonus de niveau selon le niveau du joueur
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null) {
            level += playerData.getLevel().get() / 10;
        }
        
        // Bonus de nuit
        if (nightBoostActive) {
            level += 1;
        }
        
        return Math.max(1, level);
    }

    /**
     * Spawn un seul zombie à une position donnée
     */
    public void spawnSingleZombie(Location location, int zoneId) {
        ZoneSpawnConfig config = zoneConfigs.getOrDefault(zoneId, zoneConfigs.get(1));

        Location groundLoc = findGround(location);
        if (groundLoc == null) {
            groundLoc = location; // Fallback à la position originale
        }

        ZombieType type = selectZombieType(zoneId);
        int level = config.baseLevel + random.nextInt(3);

        zombieManager.spawnZombie(type, groundLoc, level);
    }

    /**
     * Force un spawn de vague de zombies
     * Utilise un système de distribution uniforme pour éviter le clustering
     */
    public void spawnWave(Location center, int count, int zoneId) {
        ZoneSpawnConfig config = zoneConfigs.getOrDefault(zoneId, zoneConfigs.get(1));

        // Utiliser des angles distribués uniformément pour éviter le clustering
        double angleStep = (2 * Math.PI) / count;
        int spawned = 0;
        int maxAttempts = count * 3; // Maximum d'essais pour éviter boucle infinie
        int attempts = 0;

        while (spawned < count && attempts < maxAttempts) {
            // Distribuer les spawns en cercles concentriques
            double baseAngle = angleStep * spawned + (random.nextDouble() * 0.5 - 0.25);
            double distance = 10 + (spawned % 3) * 8 + random.nextDouble() * 5; // Cercles à 10, 18, 26 blocs

            Location spawnLoc = center.clone().add(
                distance * Math.cos(baseAngle),
                0,
                distance * Math.sin(baseAngle)
            );

            Location groundLoc = findGround(spawnLoc);
            if (groundLoc != null && isSpawnDensityValid(groundLoc)) {
                ZombieType type = selectZombieType(zoneId);
                int level = config.baseLevel + random.nextInt(3);

                if (zombieManager.spawnZombie(type, groundLoc, level) != null) {
                    spawned++;
                }
            }
            attempts++;
        }
    }

    /**
     * Active/désactive le système de spawn
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            plugin.getLogger().info("[SpawnSystem] Système de spawn désactivé");
        } else {
            plugin.getLogger().info("[SpawnSystem] Système de spawn activé");
        }
    }
    
    /**
     * Définit le multiplicateur de spawn (pour les événements)
     */
    public void setSpawnMultiplier(double multiplier) {
        this.spawnMultiplier = Math.max(0.1, Math.min(10.0, multiplier));
        plugin.getLogger().info("[SpawnSystem] Multiplicateur de spawn: " + spawnMultiplier);
    }

    /**
     * Obtient les stats du système
     */
    public String getStats() {
        return String.format("Enabled: %s | Night: %s | Spawns/tick: %d",
            enabled, nightBoostActive, spawnsThisTick);
    }

    /**
     * Configuration de spawn par zone
     */
    private static class ZoneSpawnConfig {
        final int maxZombies;
        final int spawnRate;      // spawns par minute par joueur
        final int minSpawnRadius;
        final int maxSpawnRadius;
        final int baseLevel;

        ZoneSpawnConfig(int maxZombies, int spawnRate, int minSpawnRadius, int maxSpawnRadius, int baseLevel) {
            this.maxZombies = maxZombies;
            this.spawnRate = spawnRate;
            this.minSpawnRadius = minSpawnRadius;
            this.maxSpawnRadius = maxSpawnRadius;
            this.baseLevel = baseLevel;
        }
    }
}
