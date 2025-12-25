package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.bosses.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manager principal pour le système de World Boss
 *
 * Responsabilités:
 * - Gestion du spawn automatique des World Boss
 * - Tracking de tous les boss actifs
 * - Interface pour les commandes admin
 * - Intégration avec le système de zones
 */
public class WorldBossManager {

    private final ZombieZPlugin plugin;

    // Boss actifs (UUID du boss -> WorldBoss)
    @Getter
    private final Map<UUID, WorldBoss> activeBosses = new ConcurrentHashMap<>();

    // Tâche de spawn
    private WorldBossSpawnerTask spawnerTask;

    // Configuration (sera chargée depuis config)
    @Getter private int minSpawnIntervalMinutes = 20;
    @Getter private int maxSpawnIntervalMinutes = 40;
    @Getter private double spawnChance = 0.15; // 15% de chance à chaque check
    @Getter private int minSpawnRadius = 20;
    @Getter private int maxSpawnRadius = 40;
    @Getter private int maxConcurrentBosses = 2;
    @Getter private boolean enabled = true;

    // Statistiques
    private int totalBossesSpawned = 0;
    private int totalBossesKilled = 0;
    private int totalBossesDespawned = 0;

    public WorldBossManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Charge la configuration
     */
    private void loadConfig() {
        var config = plugin.getConfig();
        if (config == null) return;

        // Section worldboss si elle existe
        if (config.contains("worldboss")) {
            enabled = config.getBoolean("worldboss.enabled", true);
            minSpawnIntervalMinutes = config.getInt("worldboss.spawn.min-interval-minutes", 20);
            maxSpawnIntervalMinutes = config.getInt("worldboss.spawn.max-interval-minutes", 40);
            spawnChance = config.getDouble("worldboss.spawn.chance", 0.15);
            minSpawnRadius = config.getInt("worldboss.spawn.min-radius", 20);
            maxSpawnRadius = config.getInt("worldboss.spawn.max-radius", 40);
            maxConcurrentBosses = config.getInt("worldboss.max-concurrent", 2);
        }
    }

    /**
     * Démarre le système de World Boss
     */
    public void start() {
        if (!enabled) {
            plugin.getLogger().info("[WorldBoss] Système désactivé dans la config");
            return;
        }

        // Démarrer la tâche de spawn
        spawnerTask = new WorldBossSpawnerTask(plugin, this);
        spawnerTask.start();

        plugin.getLogger().info("[WorldBoss] Système démarré - Intervalle: " +
            minSpawnIntervalMinutes + "-" + maxSpawnIntervalMinutes + " min, Chance: " +
            (spawnChance * 100) + "%");
    }

    /**
     * Arrête le système de World Boss
     */
    public void stop() {
        // Arrêter la tâche de spawn
        if (spawnerTask != null) {
            spawnerTask.stop();
            spawnerTask = null;
        }

        // Nettoyer tous les boss actifs
        for (WorldBoss boss : new ArrayList<>(activeBosses.values())) {
            boss.cleanup();
        }
        activeBosses.clear();

        plugin.getLogger().info("[WorldBoss] Système arrêté");
    }

    /**
     * Tente de spawner un World Boss
     * Appelé par WorldBossSpawnerTask
     */
    public boolean trySpawnBoss() {
        if (!enabled) return false;
        if (activeBosses.size() >= maxConcurrentBosses) return false;

        // Roll la chance de spawn
        if (Math.random() > spawnChance) return false;

        // Choisir un joueur aléatoire
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return false;

        // Filtrer les joueurs éligibles (pas en zone spawn, pas en refuge, pas déjà un boss proche)
        players.removeIf(player -> {
            var zone = plugin.getZoneManager().getPlayerZone(player);
            // Pas en zone spawn (zone 0)
            if (zone != null && zone.getId() == 0) return true;
            // Pas dans un refuge
            if (plugin.getRefugeManager().isInAnyRefugeProtectedArea(player.getLocation())) return true;
            // Pas déjà un boss proche
            return hasBossNearby(player.getLocation(), 100);
        });

        if (players.isEmpty()) return false;

        // Choisir un joueur aléatoire
        Player targetPlayer = players.get((int) (Math.random() * players.size()));

        // Trouver une location de spawn
        Location spawnLocation = findSpawnLocation(targetPlayer);
        if (spawnLocation == null) return false;

        // Choisir un type de boss aléatoire
        WorldBossType bossType = WorldBossType.random();

        // Déterminer la zone
        var zone = plugin.getZoneManager().getZoneAt(spawnLocation);
        int zoneId = zone != null ? zone.getId() : 1;

        // Spawner le boss
        return spawnBoss(bossType, spawnLocation, zoneId);
    }

    /**
     * Spawn un boss spécifique
     */
    public boolean spawnBoss(WorldBossType type, Location location, int zoneId) {
        if (activeBosses.size() >= maxConcurrentBosses) {
            plugin.getLogger().warning("[WorldBoss] Limite de boss atteinte (" + maxConcurrentBosses + ")");
            return false;
        }

        try {
            // Créer l'instance du boss approprié
            WorldBoss boss = createBoss(type, zoneId);
            if (boss == null) {
                plugin.getLogger().warning("[WorldBoss] Impossible de créer le boss " + type.name());
                return false;
            }

            // Spawner
            boss.spawn(location);

            // Enregistrer
            activeBosses.put(boss.getBossId(), boss);
            totalBossesSpawned++;

            plugin.getLogger().info("[WorldBoss] " + type.getDisplayName() + " spawné en zone " + zoneId);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[WorldBoss] Erreur lors du spawn", e);
            return false;
        }
    }

    /**
     * Crée une instance du boss approprié
     */
    private WorldBoss createBoss(WorldBossType type, int zoneId) {
        return switch (type) {
            case THE_BUTCHER -> new ButcherBoss(plugin, zoneId);
            case SHADOW_UNSTABLE -> new ShadowUnstableBoss(plugin, zoneId);
            case PYROMANCER -> new PyromancerBoss(plugin, zoneId);
            case HORDE_QUEEN -> new HordeQueenBoss(plugin, zoneId);
            case ICE_BREAKER -> new IceBreakerBoss(plugin, zoneId);
        };
    }

    /**
     * Trouve une location de spawn valide autour d'un joueur
     */
    private Location findSpawnLocation(Player player) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return null;

        // Essayer plusieurs fois de trouver un spot valide
        for (int attempt = 0; attempt < 20; attempt++) {
            // Angle et distance aléatoires
            double angle = Math.random() * Math.PI * 2;
            double distance = minSpawnRadius + Math.random() * (maxSpawnRadius - minSpawnRadius);

            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance;

            // Trouver le sol solide
            int y = world.getHighestBlockYAt((int) x, (int) z);
            Location testLoc = new Location(world, x, y + 1, z);

            // Vérifier que c'est un bon spot
            if (isValidSpawnLocation(testLoc)) {
                return testLoc;
            }
        }

        return null;
    }

    /**
     * Vérifie si une location est valide pour le spawn
     */
    private boolean isValidSpawnLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        // Vérifier que c'est dans les limites de la map
        if (!plugin.getZoneManager().isInMapBounds(loc)) return false;

        // Vérifier que le sol est solide
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();
        if (!ground.getType().isSolid()) return false;

        // Vérifier qu'il y a de l'espace pour le boss
        Block atLoc = loc.getBlock();
        Block aboveLoc = loc.clone().add(0, 1, 0).getBlock();
        Block above2Loc = loc.clone().add(0, 2, 0).getBlock();

        if (atLoc.getType().isSolid() || aboveLoc.getType().isSolid() || above2Loc.getType().isSolid()) {
            return false;
        }

        // Pas en zone spawn
        var zone = plugin.getZoneManager().getZoneAt(loc);
        if (zone != null && zone.getId() == 0) return false;

        // Pas dans une zone de refuge
        if (plugin.getRefugeManager().isInAnyRefugeProtectedArea(loc)) return false;

        // Pas dans l'eau
        if (ground.isLiquid() || atLoc.isLiquid()) return false;

        return true;
    }

    /**
     * Vérifie s'il y a un boss proche d'une location
     */
    public boolean hasBossNearby(Location loc, double radius) {
        for (WorldBoss boss : activeBosses.values()) {
            if (!boss.isActive()) continue;
            if (boss.getEntity() == null) continue;
            if (!boss.getEntity().getWorld().equals(loc.getWorld())) continue;

            if (boss.getEntity().getLocation().distance(loc) <= radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtient un boss par son entité
     */
    public WorldBoss getBossByEntity(Entity entity) {
        if (!(entity instanceof Zombie zombie)) return null;

        // Vérifier les tags
        if (!zombie.getScoreboardTags().contains("world_boss")) return null;

        // Chercher le boss correspondant
        for (WorldBoss boss : activeBosses.values()) {
            if (boss.getEntity() != null && boss.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return boss;
            }
        }

        return null;
    }

    /**
     * Obtient un boss par son UUID
     */
    public WorldBoss getBossById(UUID bossId) {
        return activeBosses.get(bossId);
    }

    /**
     * Appelé quand un boss meurt
     */
    public void onBossDeath(WorldBoss boss, Player killer) {
        if (boss == null) return;

        boss.onDeath(killer);
        activeBosses.remove(boss.getBossId());
        totalBossesKilled++;

        plugin.getLogger().info("[WorldBoss] " + boss.getType().getDisplayName() +
            " tué par " + killer.getName());
    }

    /**
     * Supprime un boss (admin ou despawn)
     */
    public void removeBoss(WorldBoss boss, String reason) {
        if (boss == null) return;

        boss.despawn(reason);
        activeBosses.remove(boss.getBossId());
        totalBossesDespawned++;

        plugin.getLogger().info("[WorldBoss] " + boss.getType().getDisplayName() +
            " retiré: " + reason);
    }

    /**
     * Force le spawn d'un boss (commande admin)
     */
    public boolean forceSpawn(WorldBossType type, Location location) {
        var zone = plugin.getZoneManager().getZoneAt(location);
        int zoneId = zone != null ? zone.getId() : 1;

        // Ignorer la limite pour les spawns forcés
        WorldBoss boss = createBoss(type, zoneId);
        if (boss == null) return false;

        boss.spawn(location);
        activeBosses.put(boss.getBossId(), boss);
        totalBossesSpawned++;

        return true;
    }

    /**
     * Supprime tous les boss actifs
     */
    public void clearAllBosses() {
        for (WorldBoss boss : new ArrayList<>(activeBosses.values())) {
            boss.cleanup();
        }
        activeBosses.clear();
    }

    /**
     * Obtient les statistiques
     */
    public String getStats() {
        return String.format(
            "Actifs: %d/%d | Spawned: %d | Killed: %d | Despawned: %d",
            activeBosses.size(), maxConcurrentBosses,
            totalBossesSpawned, totalBossesKilled, totalBossesDespawned
        );
    }

    /**
     * Obtient la liste des boss actifs formatée
     */
    public List<String> getActiveBossInfo() {
        List<String> info = new ArrayList<>();
        for (WorldBoss boss : activeBosses.values()) {
            if (boss.isActive() && boss.getEntity() != null) {
                Location loc = boss.getEntity().getLocation();
                info.add(String.format("§e%s §7(Zone %d) - §c%s §7@ [%.0f, %.0f, %.0f]",
                    boss.getType().getDisplayName(),
                    boss.getZoneId(),
                    boss.getFormattedHealth(),
                    loc.getX(), loc.getY(), loc.getZ()
                ));
            }
        }
        return info;
    }

    /**
     * Vérifie si le système est activé
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Active/désactive le système
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }
}
