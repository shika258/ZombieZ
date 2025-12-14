package com.rinaorc.zombiez.zombies.spawning;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Système de spawn des boss
 */
public class BossSpawnSystem {

    private final ZombieZPlugin plugin;
    private final ZombieManager zombieManager;
    
    @Getter
    private final Map<Integer, Long> bossSpawnCooldowns = new HashMap<>();
    
    private static final long BOSS_COOLDOWN = 30 * 60 * 1000; // 30 minutes
    
    // Stats
    private int totalBossesSpawned = 0;
    private int patientZeroSpawned = 0;

    public BossSpawnSystem(ZombieZPlugin plugin, ZombieManager zombieManager) {
        this.plugin = plugin;
        this.zombieManager = zombieManager;
    }

    /**
     * Tente de spawner un boss dans une zone
     */
    public boolean trySpawnBoss(Zone zone, Location location) {
        int zoneId = zone.getId();
        
        // Vérifier le cooldown
        Long lastSpawn = bossSpawnCooldowns.get(zoneId);
        if (lastSpawn != null && System.currentTimeMillis() - lastSpawn < BOSS_COOLDOWN) {
            return false;
        }
        
        // Spawner le boss
        spawnBoss(zone, location);
        bossSpawnCooldowns.put(zoneId, System.currentTimeMillis());
        
        return true;
    }

    /**
     * Spawne un boss
     */
    public void spawnBoss(Zone zone, Location location) {
        ZombieType bossType = getBossTypeForZone(zone.getId());
        int level = zone.getMaxZombieLevel();
        
        zombieManager.spawnZombie(bossType, location, level);
        totalBossesSpawned++;
        
        // Annonce
        for (Player player : plugin.getZoneManager().getPlayersInZone(zone)) {
            player.sendTitle("§c§lBOSS!", "§7Un boss apparaît!", 10, 40, 10);
        }
    }
    
    /**
     * Spawne un boss pour une zone spécifique (admin command)
     */
    public void spawnZoneBoss(int zoneId, Location location) {
        Zone zone = plugin.getZoneManager().getZoneById(zoneId);
        if (zone == null) {
            zone = plugin.getZoneManager().getSpawnZone();
        }
        
        ZombieType bossType = getBossTypeForZone(zoneId);
        int level = zone != null ? zone.getMaxZombieLevel() : 10;
        
        zombieManager.spawnZombie(bossType, location, level);
        totalBossesSpawned++;
        
        MessageUtils.broadcast("§c§l⚠ BOSS §7de la zone §e" + zoneId + " §7spawné!");
    }

    /**
     * Obtient le type de boss pour une zone
     */
    private ZombieType getBossTypeForZone(int zoneId) {
        return switch (zoneId) {
            case 1, 2 -> ZombieType.BUTCHER;
            case 3, 4 -> ZombieType.WIDOW;
            case 5, 6 -> ZombieType.THE_GIANT;
            case 7, 8 -> ZombieType.THE_PHANTOM;
            case 9, 10, 11 -> ZombieType.PATIENT_ZERO;
            default -> ZombieType.BUTCHER;
        };
    }

    /**
     * Force le spawn d'un boss (admin)
     */
    public void forceSpawnBoss(Location location, ZombieType bossType, int level) {
        zombieManager.spawnZombie(bossType, location, level);
        totalBossesSpawned++;
    }
    
    /**
     * Spawne Patient Zéro (boss final)
     */
    public void spawnPatientZero(Location location) {
        zombieManager.spawnZombie(ZombieType.PATIENT_ZERO, location, 50);
        patientZeroSpawned++;
        
        MessageUtils.broadcast("§4§l☠ PATIENT ZÉRO §7a été invoqué!");
        MessageUtils.broadcast("§cPréparez-vous au combat final!");
        
        // Effets dramatiques pour tous les joueurs
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§4§lPATIENT ZÉRO", "§cLe fléau originel approche...", 20, 60, 20);
        }
    }
    
    /**
     * Obtient les statistiques
     */
    public String getStats() {
        return String.format("Boss: %d | PatientZero: %d | Cooldowns: %d", 
            totalBossesSpawned, patientZeroSpawned, bossSpawnCooldowns.size());
    }
}
