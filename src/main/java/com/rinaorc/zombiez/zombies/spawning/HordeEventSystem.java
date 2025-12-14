package com.rinaorc.zombiez.zombies.spawning;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Système d'événements de horde
 */
public class HordeEventSystem {

    private final ZombieZPlugin plugin;
    private final ZombieManager zombieManager;
    private final SpawnSystem spawnSystem;
    
    @Getter
    private boolean hordeActive = false;
    
    @Getter
    private boolean bloodMoonActive = false;
    
    @Getter
    private Zone currentHordeZone = null;
    
    private int hordeWave = 0;
    private int zombiesRemaining = 0;
    
    // Stats
    private int totalEventsStarted = 0;
    private int totalZombiesSpawned = 0;
    private int bloodMoonCount = 0;

    public HordeEventSystem(ZombieZPlugin plugin, ZombieManager zombieManager, SpawnSystem spawnSystem) {
        this.plugin = plugin;
        this.zombieManager = zombieManager;
        this.spawnSystem = spawnSystem;
    }
    
    /**
     * Types d'événements de horde
     */
    public enum HordeEventType {
        STANDARD("Horde Standard", 1.0, 20),
        ELITE("Horde d'Élite", 1.5, 15),
        BOSS_RUSH("Rush de Boss", 2.0, 10),
        ENDLESS("Vagues Infinies", 1.2, 30),
        NIGHTMARE("Cauchemar", 3.0, 25);
        
        @Getter private final String displayName;
        @Getter private final double difficultyMultiplier;
        @Getter private final int baseZombieCount;
        
        HordeEventType(String displayName, double difficultyMultiplier, int baseZombieCount) {
            this.displayName = displayName;
            this.difficultyMultiplier = difficultyMultiplier;
            this.baseZombieCount = baseZombieCount;
        }
    }

    /**
     * Démarre un événement de horde dans une zone
     */
    public void startHorde(Zone zone) {
        if (hordeActive) return;
        
        hordeActive = true;
        currentHordeZone = zone;
        hordeWave = 0;
        totalEventsStarted++;
        
        // Annonce
        MessageUtils.broadcast("§c§l⚠ HORDE! §7Une horde de zombies envahit §e" + zone.getDisplayName() + "§7!");
        
        // Démarrer les vagues
        startNextWave();
    }
    
    /**
     * Démarre un événement avec type spécifique
     */
    public void startEvent(int zoneId, Location location, HordeEventType eventType) {
        Zone zone = plugin.getZoneManager().getZoneById(zoneId);
        if (zone == null) {
            zone = plugin.getZoneManager().getZoneAt(location);
        }
        if (zone == null) return;
        
        if (hordeActive) {
            return;
        }
        
        hordeActive = true;
        currentHordeZone = zone;
        hordeWave = 0;
        totalEventsStarted++;
        
        // Annonce
        MessageUtils.broadcast("§c§l⚠ " + eventType.getDisplayName().toUpperCase() + "! §7Une horde envahit §e" + zone.getDisplayName() + "§7!");
        
        // Spawner des zombies selon le type
        int count = (int) (eventType.getBaseZombieCount() * eventType.getDifficultyMultiplier());
        
        for (int i = 0; i < count; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (hordeActive) {
                    spawnHordeZombie();
                    totalZombiesSpawned++;
                }
            }, i * 10L);
        }
    }

    /**
     * Démarre la prochaine vague
     */
    private void startNextWave() {
        if (!hordeActive || currentHordeZone == null) return;
        
        hordeWave++;
        int zombieCount = 10 + (hordeWave * 5);
        zombiesRemaining = zombieCount;
        
        // Annonce vague
        for (Player player : plugin.getZoneManager().getPlayersInZone(currentHordeZone)) {
            player.sendTitle("§c§lVAGUE " + hordeWave, "§7" + zombieCount + " zombies!", 10, 30, 10);
        }
        
        // Spawner les zombies progressivement
        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            if (!hordeActive || zombiesRemaining <= 0) {
                task.cancel();
                return;
            }
            
            spawnHordeZombie();
            zombiesRemaining--;
            totalZombiesSpawned++;
            
            if (zombiesRemaining <= 0) {
                // Vague terminée
                if (hordeWave < 5) {
                    // Prochaine vague après 10 secondes
                    Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, 200L);
                } else {
                    // Horde terminée
                    endHorde(true);
                }
            }
        }, 20L, 10L);
    }

    /**
     * Spawne un zombie de horde
     */
    private void spawnHordeZombie() {
        if (currentHordeZone == null) return;
        
        List<Player> players = plugin.getZoneManager().getPlayersInZone(currentHordeZone);
        if (players.isEmpty()) return;
        
        Player target = players.get(new Random().nextInt(players.size()));
        Location spawnLoc = getSpawnLocationNear(target.getLocation());
        
        int level = currentHordeZone.getMinZombieLevel() + hordeWave;
        zombieManager.spawnZombie(ZombieType.WALKER, spawnLoc, level);
    }

    /**
     * Obtient une location de spawn près d'un joueur
     */
    private Location getSpawnLocationNear(Location center) {
        double angle = Math.random() * Math.PI * 2;
        double distance = 15 + Math.random() * 10;
        
        return center.clone().add(
            Math.cos(angle) * distance,
            0,
            Math.sin(angle) * distance
        );
    }

    /**
     * Termine la horde
     */
    public void endHorde(boolean success) {
        if (!hordeActive) return;
        
        hordeActive = false;
        
        if (success) {
            MessageUtils.broadcast("§a§l✓ HORDE REPOUSSÉE! §7Félicitations aux survivants!");
            
            // Récompenses
            if (currentHordeZone != null) {
                for (Player player : plugin.getZoneManager().getPlayersInZone(currentHordeZone)) {
                    int reward = 500 * hordeWave;
                    plugin.getEconomyManager().addPoints(player, reward, "Horde");
                    plugin.getEconomyManager().addGems(player, hordeWave * 2);
                }
            }
        } else {
            MessageUtils.broadcast("§c§l✗ HORDE PERDUE! §7Les zombies ont gagné...");
        }
        
        currentHordeZone = null;
        hordeWave = 0;
    }

    /**
     * Annule la horde en cours
     */
    public void cancelHorde() {
        endHorde(false);
    }
    
    /**
     * Démarre la Blood Moon
     */
    public void startBloodMoon() {
        if (bloodMoonActive) return;
        
        bloodMoonActive = true;
        bloodMoonCount++;
        
        MessageUtils.broadcast("§4§l☠ BLOOD MOON! §cLes zombies sont enragés!");
        
        // Augmenter les spawns
        spawnSystem.setSpawnMultiplier(3.0);
        
        // Terminer après 10 minutes
        Bukkit.getScheduler().runTaskLater(plugin, this::endBloodMoon, 20L * 60 * 10);
    }
    
    /**
     * Termine la Blood Moon
     */
    public void endBloodMoon() {
        if (!bloodMoonActive) return;
        
        bloodMoonActive = false;
        spawnSystem.setSpawnMultiplier(1.0);
        
        MessageUtils.broadcast("§a§l☀ La Blood Moon est terminée! §7Le soleil se lève...");
    }
    
    /**
     * Obtient les statistiques
     */
    public String getStats() {
        return String.format("Events: %d | Spawns: %d | BloodMoons: %d | Active: %s", 
            totalEventsStarted, totalZombiesSpawned, bloodMoonCount, 
            hordeActive ? "Oui" : (bloodMoonActive ? "BloodMoon" : "Non"));
    }
}
