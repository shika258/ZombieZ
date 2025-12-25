package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tâche récurrente qui tente de spawner des World Boss
 *
 * Fonctionnement:
 * - Check toutes les 5 minutes
 * - Chance configurable de spawn (par défaut 15%)
 * - Spawn près d'un joueur aléatoire
 * - Respecte la limite de boss concurrent
 */
public class WorldBossSpawnerTask {

    private final ZombieZPlugin plugin;
    private final WorldBossManager manager;

    private BukkitTask task;
    private long lastSpawnAttempt = 0;
    private long nextSpawnCheck = 0;

    // Intervalle de vérification en ticks (5 minutes = 6000 ticks)
    private static final long CHECK_INTERVAL_TICKS = 6000L;

    public WorldBossSpawnerTask(ZombieZPlugin plugin, WorldBossManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Démarre la tâche de spawn
     */
    public void start() {
        if (task != null) {
            task.cancel();
        }

        // Calculer le premier check (entre min et max intervalle)
        scheduleNextCheck();

        // Démarrer la tâche de vérification
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);

        plugin.getLogger().info("[WorldBossSpawnerTask] Démarré - Premier spawn possible dans " +
            (nextSpawnCheck - System.currentTimeMillis()) / 60000 + " minutes");
    }

    /**
     * Arrête la tâche
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * Tick de la tâche (appelé toutes les 5 minutes)
     */
    private void tick() {
        if (!manager.isEnabled()) return;

        long now = System.currentTimeMillis();

        // Vérifier si c'est l'heure de tenter un spawn
        if (now >= nextSpawnCheck) {
            lastSpawnAttempt = now;

            // Tenter de spawner
            boolean spawned = manager.trySpawnBoss();

            if (spawned) {
                plugin.getLogger().info("[WorldBossSpawnerTask] World Boss spawné avec succès!");
            }

            // Programmer le prochain check
            scheduleNextCheck();
        }
    }

    /**
     * Programme le prochain check de spawn
     */
    private void scheduleNextCheck() {
        int minMinutes = manager.getMinSpawnIntervalMinutes();
        int maxMinutes = manager.getMaxSpawnIntervalMinutes();

        // Intervalle aléatoire entre min et max
        int intervalMinutes = minMinutes + (int) (Math.random() * (maxMinutes - minMinutes));

        nextSpawnCheck = System.currentTimeMillis() + (intervalMinutes * 60L * 1000L);

        plugin.getLogger().fine("[WorldBossSpawnerTask] Prochain check dans " + intervalMinutes + " minutes");
    }

    /**
     * Force un check immédiat
     */
    public void forceCheck() {
        nextSpawnCheck = System.currentTimeMillis();
    }

    /**
     * Obtient le temps restant avant le prochain check
     */
    public long getTimeUntilNextCheck() {
        return Math.max(0, nextSpawnCheck - System.currentTimeMillis());
    }

    /**
     * Obtient les infos de debug
     */
    public String getDebugInfo() {
        long remaining = getTimeUntilNextCheck();
        long minutes = remaining / 60000;
        long seconds = (remaining % 60000) / 1000;

        return String.format("Prochain check: %dm %ds | Dernier: %s",
            minutes, seconds,
            lastSpawnAttempt > 0 ? ((System.currentTimeMillis() - lastSpawnAttempt) / 60000) + "m ago" : "jamais");
    }
}
