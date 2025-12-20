package com.rinaorc.zombiez.zombies.spawning;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

    // État du système
    @Getter
    private boolean enabled = true;

    @Getter
    private boolean nightBoostActive = false;

    @Getter
    private double spawnMultiplier = 1.0;

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
        
        initializeZoneConfigs();
        initializeWeightedTables(); // Précalculer les tables
        startSpawnTask();
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
                // ACTE I - LES DERNIERS JOURS: densité modérée
                maxZombies = 40 + (zoneId * 4);  // 44-80
                spawnRate = 8 + (zoneId / 2);     // 8-13
                minSpawnRadius = 20;
                maxSpawnRadius = 35 + zoneId;
            } else if (zoneId <= 20) {
                // ACTE II - LA CONTAMINATION: densité croissante
                maxZombies = 60 + ((zoneId - 10) * 4);  // 64-100
                spawnRate = 10 + ((zoneId - 10) / 2);    // 10-15
                minSpawnRadius = 18;
                maxSpawnRadius = 40;
            } else if (zoneId <= 30) {
                // ACTE III - LE CHAOS: haute densité
                maxZombies = 80 + ((zoneId - 20) * 4);  // 84-120
                spawnRate = 12 + ((zoneId - 20) / 3);    // 12-15
                minSpawnRadius = 15;
                maxSpawnRadius = 38;
            } else if (zoneId <= 40) {
                // ACTE IV - L'EXTINCTION: densité décroissante, zombies plus forts
                maxZombies = 70 + ((zoneId - 30) * 2);  // 72-90
                spawnRate = 10 + ((zoneId - 30) / 4);    // 10-12
                minSpawnRadius = 15;
                maxSpawnRadius = 35;
            } else {
                // ACTE V - L'ORIGINE DU MAL: faible densité, zombies très dangereux
                maxZombies = 50 + ((zoneId - 40) * 2);  // 52-70
                spawnRate = 7 + ((zoneId - 40) / 3);     // 7-10
                minSpawnRadius = 12;
                maxSpawnRadius = 30;
            }

            // Ajustements spéciaux pour certaines zones
            if (zoneId == 26) {
                // Zone PVP - moins de zombies
                maxZombies = 50;
                spawnRate = 8;
            }
            if (zoneId == 50) {
                // Zone Boss finale - très peu de zombies mais très dangereux
                maxZombies = 40;
                spawnRate = 5;
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

        plugin.log(java.util.logging.Level.INFO, "§a✓ Système de clearlag démarré");
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
        
        // Vérifier le cooldown
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastSpawn = playerSpawnCooldowns.getOrDefault(playerId, 0L);
        
        // Obtenir la zone du joueur
        Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        if (zone == null) return;
        
        int zoneId = zone.getId();
        ZoneSpawnConfig config = zoneConfigs.get(zoneId);
        if (config == null || config.maxZombies == 0) return;
        
        // Calculer le cooldown de spawn
        double spawnInterval = 60000.0 / config.spawnRate; // ms entre spawns
        if (nightBoostActive) {
            spawnInterval *= 0.7; // 30% plus rapide la nuit
        }
        
        if (now - lastSpawn < spawnInterval) return;
        
        // Vérifier si la zone a de la place
        int currentCount = zombieManager.getZombieCount(zoneId);
        if (currentCount >= config.maxZombies) return;
        
        // Trouver un point de spawn valide
        Location spawnLoc = findValidSpawnLocation(player, config);
        if (spawnLoc == null) return;
        
        // Choisir le type de zombie
        ZombieType type = selectZombieType(zoneId);
        
        // Calculer le niveau
        int level = calculateZombieLevel(config.baseLevel, player);
        
        // Spawner!
        ZombieManager.ActiveZombie zombie = zombieManager.spawnZombie(type, spawnLoc, level);
        
        if (zombie != null) {
            playerSpawnCooldowns.put(playerId, now);
            spawnsThisTick++;
        }
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
