package com.rinaorc.zombiez.zombies.spawning;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire d'événements spéciaux
 * Hordes, Blood Moon, Boss spawns
 */
public class EventManager {

    private final ZombieZPlugin plugin;
    private final ZombieManager zombieManager;
    private final SpawnSystem spawnSystem;
    private final Random random;
    
    // État des événements
    @Getter
    private ActiveEvent currentEvent;
    
    // Événements par zone
    private final Map<Integer, ZoneEvent> zoneEvents;
    
    // Boss bars pour les événements
    private final Map<String, BossBar> eventBossBars;
    
    // Cooldowns
    private long lastHordeTime = 0;

    public EventManager(ZombieZPlugin plugin, ZombieManager zombieManager, SpawnSystem spawnSystem) {
        this.plugin = plugin;
        this.zombieManager = zombieManager;
        this.spawnSystem = spawnSystem;
        this.random = new Random();
        this.zoneEvents = new ConcurrentHashMap<>();
        this.eventBossBars = new ConcurrentHashMap<>();
        
        startEventChecker();
    }

    /**
     * Démarre la vérification périodique des événements
     */
    private void startEventChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForEvents();
            }
        }.runTaskTimer(plugin, 200L, 200L); // Toutes les 10 secondes
    }

    /**
     * Vérifie si un événement doit se déclencher
     */
    private void checkForEvents() {
        if (currentEvent != null && currentEvent.isActive()) {
            return; // Un événement est déjà en cours
        }

        // Vérifier Horde aléatoire
        if (shouldTriggerRandomHorde()) {
            triggerRandomHorde();
        }
    }

    /**
     * Vérifie si une horde aléatoire doit spawn
     */
    private boolean shouldTriggerRandomHorde() {
        long now = System.currentTimeMillis();
        
        // Minimum 10 minutes entre hordes
        if (now - lastHordeTime < 600000) return false;
        
        // Besoin d'au moins 3 joueurs
        if (plugin.getServer().getOnlinePlayers().size() < 3) return false;
        
        // 5% de chance par vérification
        return random.nextDouble() < 0.05;
    }

    // ==================== HORDES ====================

    /**
     * Déclenche une horde aléatoire
     */
    public void triggerRandomHorde() {
        // Choisir un joueur au hasard
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return;
        
        Player target = players.get(random.nextInt(players.size()));
        Zone zone = plugin.getZoneManager().getZoneAt(target.getLocation());
        if (zone == null || zone.getId() == 0) return;
        
        startHorde(target.getLocation(), zone.getId(), 20 + random.nextInt(20));
    }

    /**
     * Démarre une horde à une position
     */
    public void startHorde(Location center, int zoneId, int totalZombies) {
        lastHordeTime = System.currentTimeMillis();
        
        HordeEvent horde = new HordeEvent(center, zoneId, totalZombies);
        zoneEvents.put(zoneId, horde);
        
        // Annoncer aux joueurs proches
        for (Player p : center.getWorld().getNearbyEntities(center, 100, 50, 100).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            p.sendMessage("§c§l⚠ HORDE! §7Une vague de zombies approche!");
            p.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
        }
        
        horde.start();
    }

    /**
     * Événement Horde
     */
    private class HordeEvent extends ZoneEvent {
        private final Location center;
        private final int zoneId;
        private final int totalZombies;
        private int spawned = 0;
        private BukkitTask spawnTask;
        
        HordeEvent(Location center, int zoneId, int totalZombies) {
            this.center = center;
            this.zoneId = zoneId;
            this.totalZombies = totalZombies;
        }
        
        @Override
        public void start() {
            active = true;
            
            spawnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active || spawned >= totalZombies) {
                        end();
                        cancel();
                        return;
                    }
                    
                    // Spawn 3-5 zombies par tick
                    int toSpawn = Math.min(3 + random.nextInt(3), totalZombies - spawned);
                    spawnSystem.spawnWave(center, toSpawn, zoneId);
                    spawned += toSpawn;
                }
            }.runTaskTimer(plugin, 0L, 40L); // Toutes les 2 secondes
        }
        
        @Override
        public void end() {
            active = false;
            if (spawnTask != null) {
                spawnTask.cancel();
            }
            zoneEvents.remove(zoneId);
        }
        
        @Override
        public String getName() {
            return "Horde Zone " + zoneId;
        }
    }

    // ==================== BOSS ====================

    /**
     * Spawn un mini-boss
     */
    public void spawnMiniBoss(Location location, int zoneId) {
        ZombieType[] miniBosses = {
            ZombieType.BUTCHER,
            ZombieType.WIDOW,
            ZombieType.THE_GIANT,
            ZombieType.THE_PHANTOM
        };
        
        // Filtrer par zone
        List<ZombieType> valid = Arrays.stream(miniBosses)
            .filter(t -> t.canSpawnInZone(zoneId))
            .toList();
        
        if (valid.isEmpty()) return;
        
        ZombieType boss = valid.get(random.nextInt(valid.size()));
        int level = zoneId + random.nextInt(3);
        
        zombieManager.spawnZombie(boss, location, level);
        
        // Annoncer
        for (Player p : location.getWorld().getNearbyEntities(location, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            p.sendMessage("§c§l⚠ " + boss.getDisplayName() + " §7apparaît!");
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
        }
    }

    /**
     * Spawn un boss de zone
     */
    public void spawnZoneBoss(int zoneId, Location location) {
        ZombieType boss = switch (zoneId) {
            case 1, 2, 3 -> ZombieType.BOSS_GUARDIAN;
            case 4, 5, 6 -> ZombieType.BOSS_SHADOW_ELDER;
            case 7, 8 -> ZombieType.BOSS_FROST_LORD;
            case 9, 10 -> ZombieType.BOSS_ABOMINATION;
            default -> ZombieType.BOSS_GUARDIAN;
        };
        
        int level = zoneId * 2;
        
        zombieManager.spawnZombie(boss, location, level);
        
        // Annoncer globalement
        announceGlobal("§4§l⚠ BOSS DE ZONE", 
            "§c" + boss.getDisplayName() + " §7s'éveille dans la Zone " + zoneId + "!");
        
        // Son global
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.8f);
        }
    }

    /**
     * Spawn Patient Zéro (boss final)
     */
    public void spawnPatientZero(Location location) {
        if (currentEvent != null && currentEvent.isActive()) {
            return;
        }
        
        PatientZeroEvent event = new PatientZeroEvent(location);
        currentEvent = event;
        event.start();
    }

    /**
     * Événement Patient Zéro
     */
    private class PatientZeroEvent extends ActiveEvent {
        private final Location location;
        private ZombieManager.ActiveZombie boss;
        
        PatientZeroEvent(Location location) {
            this.location = location;
        }
        
        @Override
        public void start() {
            active = true;
            
            // Cinématique de spawn
            announceGlobal("§4§l", "");
            announceGlobal("§4§l  ☠ PATIENT ZÉRO S'ÉVEILLE ☠", "");
            announceGlobal("§4§l", "");
            
            // Son dramatique
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2f, 0.5f);
            }
            
            // Spawn après un délai
            new BukkitRunnable() {
                @Override
                public void run() {
                    boss = zombieManager.spawnZombie(ZombieType.PATIENT_ZERO, location, 20);
                    
                    // Boss bar personnalisée
                    BossBar bar = Bukkit.createBossBar(
                        "§4§l☠ PATIENT ZÉRO - PHASE 1 ☠",
                        BarColor.RED,
                        BarStyle.SEGMENTED_20
                    );
                    bar.setProgress(1.0);
                    plugin.getServer().getOnlinePlayers().forEach(bar::addPlayer);
                    eventBossBars.put("patient_zero", bar);
                }
            }.runTaskLater(plugin, 60L);
        }
        
        @Override
        public void end() {
            active = false;
            
            BossBar bar = eventBossBars.remove("patient_zero");
            if (bar != null) {
                bar.removeAll();
            }
            
            // Victoire!
            announceGlobal("§a§l★ VICTOIRE! ★", 
                "§aPatient Zéro a été vaincu! Le monde est sauvé!");
            
            // Feu d'artifice pour tous
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                plugin.getEconomyManager().addPoints(p, 5000);
                plugin.getEconomyManager().addGems(p, 100);
            }
            
            currentEvent = null;
        }
        
        @Override
        public String getName() {
            return "Patient Zéro";
        }
    }

    // ==================== UTILITAIRES ====================

    /**
     * Annonce globale
     */
    private void announceGlobal(String title, String subtitle) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (title != null && !title.isEmpty()) {
                p.sendTitle(
                    title.replace("&", "§"),
                    subtitle != null ? subtitle.replace("&", "§") : "",
                    10, 70, 20
                );
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                p.sendMessage(subtitle.replace("&", "§"));
            }
        }
    }

    /**
     * Force la fin de l'événement en cours
     */
    public void forceEndCurrentEvent() {
        if (currentEvent != null && currentEvent.isActive()) {
            currentEvent.end();
        }
    }

    /**
     * Obtient les stats des événements
     */
    public String getEventStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current: ").append(currentEvent != null ? currentEvent.getName() : "None");
        sb.append(" | Zone Events: ").append(zoneEvents.size());
        return sb.toString();
    }

    /**
     * Classe de base pour les événements actifs
     */
    @Getter
    public abstract static class ActiveEvent {
        protected boolean active = false;
        
        public abstract void start();
        public abstract void end();
        public abstract String getName();
    }

    /**
     * Classe de base pour les événements de zone
     */
    public abstract static class ZoneEvent extends ActiveEvent {
    }
}
