package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Événement Invasion de Horde
 *
 * Déroulement:
 * 1. Les joueurs doivent défendre une position pendant un temps donné
 * 2. Des vagues de zombies de plus en plus fortes arrivent
 * 3. Le nombre de défenseurs affecte la difficulté
 * 4. Récompenses basées sur le nombre de vagues survivées
 */
public class HordeInvasionEvent extends DynamicEvent {

    // Configuration
    private int totalWaves;
    @Getter
    private int currentWave = 0;
    private int waveIntervalSeconds = 20;  // Temps entre chaque vague
    private int secondsUntilNextWave;

    // Zombies
    private int baseZombiesPerWave = 5;
    private int zombiesThisWave = 0;
    private int zombiesKilledThisWave = 0;
    private int totalZombiesKilled = 0;

    // Défense
    private int defendersInArea = 0;
    private int defenseRadius = 25;

    // Marqueur visuel
    private ArmorStand waveMarker;
    private ArmorStand centerMarker;

    // Tâche de particules (pour cleanup)
    private BukkitTask particleTask;

    // Statistiques
    private boolean waveClear = true;

    public HordeInvasionEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.HORDE_INVASION, location, zone);

        // Configuration basée sur la zone
        this.totalWaves = 5 + zone.getId() / 10;  // 5-10 vagues
        this.baseZombiesPerWave = 5 + zone.getId() / 5;  // Plus de zombies par vague
        this.secondsUntilNextWave = waveIntervalSeconds;

        // Réduire la durée max car c'est basé sur les vagues
        this.maxDuration = 20 * 60 * (totalWaves + 2); // Temps max basé sur les vagues
    }

    @Override
    protected void startMainLogic() {
        // Créer les marqueurs visuels
        createMarkers();

        // Démarrer la première vague après un délai
        secondsUntilNextWave = 10; // 10 secondes avant la première vague

        // Démarrer le tick
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Crée les marqueurs visuels du centre de défense
     */
    private void createMarkers() {
        World world = location.getWorld();
        if (world == null) return;

        // Marqueur central
        centerMarker = (ArmorStand) world.spawnEntity(location.clone().add(0, 0.5, 0), EntityType.ARMOR_STAND);
        centerMarker.setVisible(false);
        centerMarker.setGravity(false);
        centerMarker.setMarker(true);
        centerMarker.setCustomName("§4§l💀 POINT DE DÉFENSE 💀");
        centerMarker.setCustomNameVisible(true);

        // Marqueur de vague
        waveMarker = (ArmorStand) world.spawnEntity(location.clone().add(0, 2, 0), EntityType.ARMOR_STAND);
        waveMarker.setVisible(false);
        waveMarker.setGravity(false);
        waveMarker.setMarker(true);
        waveMarker.setCustomName("§e⏳ Préparation...");
        waveMarker.setCustomNameVisible(true);

        // Particules de zone
        showDefenseZone();
    }

    /**
     * Affiche la zone de défense avec des particules
     * OPTIMISÉ: Stocke la tâche pour cleanup propre
     */
    private void showDefenseZone() {
        World world = location.getWorld();
        if (world == null) return;

        particleTask = new BukkitRunnable() {
            int angle = 0;

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                // Cercle de particules
                for (int i = 0; i < 360; i += 15) {
                    double radians = Math.toRadians(i + angle);
                    double x = location.getX() + Math.cos(radians) * defenseRadius;
                    double z = location.getZ() + Math.sin(radians) * defenseRadius;
                    Location particleLoc = new Location(world, x, location.getY() + 0.5, z);

                    Particle.DustOptions dust = new Particle.DustOptions(
                        currentWave > 0 ? Color.RED : Color.YELLOW, 1);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
                }
                angle += 5;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public void tick() {
        elapsedTicks += 20;

        World world = location.getWorld();
        if (world == null) return;

        // Compter les défenseurs
        countDefenders();

        // Gérer le timing des vagues
        if (waveClear) {
            secondsUntilNextWave--;

            if (secondsUntilNextWave <= 0) {
                if (currentWave >= totalWaves) {
                    // Toutes les vagues complétées!
                    complete();
                    return;
                }
                startNextWave();
            } else {
                // Mise à jour du marqueur
                if (waveMarker != null && waveMarker.isValid()) {
                    waveMarker.setCustomName("§e⏳ Vague " + (currentWave + 1) + " dans §c" + secondsUntilNextWave + "s");
                }
            }
        } else {
            // Vérifier si la vague est terminée
            checkWaveComplete();
        }

        // Mettre à jour la boss bar
        double progress = (double) currentWave / totalWaves;
        String status = waveClear ? "§eProchaine vague: " + secondsUntilNextWave + "s" :
            "§cVague " + currentWave + "/" + totalWaves + " - " + zombiesKilledThisWave + "/" + zombiesThisWave + " tués";
        updateBossBar(progress, status);

        // Particules ambient pendant les vagues
        if (!waveClear && elapsedTicks % 40 == 0) {
            world.spawnParticle(Particle.SMOKE, location.clone().add(0, 2, 0),
                10, 3, 2, 3, 0.02);
        }
    }

    /**
     * Compte les défenseurs dans la zone
     */
    private void countDefenders() {
        World world = location.getWorld();
        if (world == null) return;

        defendersInArea = 0;
        for (Player player : world.getNearbyEntities(location, defenseRadius, 20, defenseRadius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            defendersInArea++;
            addParticipant(player);
        }

        // Mettre à jour le marqueur central
        if (centerMarker != null && centerMarker.isValid()) {
            String color = defendersInArea > 0 ? "§a" : "§c";
            centerMarker.setCustomName("§4§l💀 " + color + defendersInArea + " Défenseur(s) §4§l💀");
        }
    }

    /**
     * Démarre la prochaine vague
     */
    private void startNextWave() {
        currentWave++;
        waveClear = false;
        zombiesKilledThisWave = 0;

        // Calculer le nombre de zombies pour cette vague
        // Plus de zombies si plus de défenseurs
        int defenderBonus = Math.max(0, defendersInArea - 1) * 2;
        zombiesThisWave = baseZombiesPerWave + (currentWave * 3) + defenderBonus;

        World world = location.getWorld();
        if (world == null) return;

        // Annoncer la vague
        for (Player player : world.getNearbyEntities(location, 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle(
                "§c§lVAGUE " + currentWave,
                "§7" + zombiesThisWave + " zombies approchent!",
                10, 40, 10
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        }

        // Mettre à jour le marqueur
        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.setCustomName("§c§l⚔ VAGUE " + currentWave + " ⚔");
        }

        // Spawn les zombies en plusieurs fois
        spawnWaveZombies();
    }

    /**
     * Spawn les zombies de la vague
     */
    private void spawnWaveZombies() {
        World world = location.getWorld();
        if (world == null) return;

        // Spawn progressif
        new BukkitRunnable() {
            int spawned = 0;
            int spawnPerTick = Math.max(1, zombiesThisWave / 10);

            @Override
            public void run() {
                if (!active || spawned >= zombiesThisWave) {
                    cancel();
                    return;
                }

                for (int i = 0; i < spawnPerTick && spawned < zombiesThisWave; i++) {
                    // Position autour du périmètre de défense
                    double angle = Math.random() * Math.PI * 2;
                    double distance = defenseRadius + 5 + Math.random() * 15;
                    double x = location.getX() + Math.cos(angle) * distance;
                    double z = location.getZ() + Math.sin(angle) * distance;
                    int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

                    Location spawnLoc = new Location(world, x, y, z);

                    // Spawn avec niveau bonus basé sur la vague
                    int levelBonus = currentWave - 1;
                    plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, zone.getId() + levelBonus);

                    spawned++;
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * Vérifie si la vague est terminée
     */
    private void checkWaveComplete() {
        // La vague est terminée quand tous les zombies sont tués
        // On compte les zombies tués via le listener

        if (zombiesKilledThisWave >= zombiesThisWave) {
            onWaveComplete();
        }
    }

    /**
     * Appelé quand une vague est terminée
     */
    private void onWaveComplete() {
        waveClear = true;
        secondsUntilNextWave = waveIntervalSeconds;

        World world = location.getWorld();
        if (world == null) return;

        // Annoncer
        for (Player player : world.getNearbyEntities(location, 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {

            if (currentWave >= totalWaves) {
                player.sendTitle(
                    "§a§l✓ VICTOIRE!",
                    "§7Toutes les vagues repoussées!",
                    10, 60, 20
                );
            } else {
                player.sendTitle(
                    "§a§l✓ Vague " + currentWave + " repoussée!",
                    "§7Prochaine vague dans " + secondsUntilNextWave + "s",
                    10, 40, 10
                );
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        }

        // Bonus de points pour la vague
        int waveBonus = 25 * currentWave;
        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, waveBonus);
                player.sendMessage("§a+§e" + waveBonus + " Points §7(Bonus vague " + currentWave + ")");
            }
        }

        // Mettre à jour le marqueur
        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.setCustomName("§a§l✓ Vague " + currentWave + " terminée!");
        }
    }

    /**
     * Appelé quand un zombie est tué dans la zone
     * (Appelé par le listener)
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions
     */
    public void onZombieKilled(Location killLocation) {
        if (!active || waveClear) return;

        // Vérifier que le kill est dans la zone (avec validation du monde)
        double distance = safeDistance(killLocation, location);
        if (distance <= defenseRadius + 20) {
            zombiesKilledThisWave++;
            totalZombiesKilled++;
        }
    }

    /**
     * Vérifie si une location est dans la zone de défense
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions
     */
    public boolean isInDefenseZone(Location loc) {
        double distance = safeDistance(loc, location);
        return distance != Double.MAX_VALUE && distance <= defenseRadius + 20;
    }

    @Override
    protected void distributeRewards() {
        // Bonus basé sur les vagues complétées
        int wavesCompleted = waveClear ? currentWave : currentWave - 1;
        int bonusMultiplier = wavesCompleted;

        int totalPoints = basePointsReward + (zone.getId() * 10) + (bonusMultiplier * 50);
        int totalXp = baseXpReward + (zone.getId() * 5) + (bonusMultiplier * 25);

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, totalPoints);

                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
                }

                player.sendMessage("");
                player.sendMessage("§a§l✓ INVASION REPOUSSÉE!");
                player.sendMessage("§7Vagues complétées: §e" + wavesCompleted + "/" + totalWaves);
                player.sendMessage("§7Zombies tués: §e" + totalZombiesKilled);
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Annuler la tâche de particules
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }

        // Supprimer les marqueurs
        if (centerMarker != null && centerMarker.isValid()) {
            centerMarker.remove();
        }
        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.remove();
        }
    }

    @Override
    protected String getStartSubtitle() {
        return "Défendez votre position contre " + totalWaves + " vagues!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Wave: %d/%d | Zombies: %d/%d | Defenders: %d | NextWave: %ds",
            currentWave, totalWaves, zombiesKilledThisWave, zombiesThisWave,
            defendersInArea, secondsUntilNextWave);
    }
}
