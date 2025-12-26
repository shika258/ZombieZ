package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de battement de cœur quand le joueur a peu de vie
 *
 * Effet dopamine: Crée de la tension et de l'adrénaline quand le joueur
 * est en danger, renforçant l'immersion et le sentiment d'urgence.
 *
 * Fonctionnalités:
 * - Battement de cœur qui s'accélère avec la perte de vie
 * - Effets visuels rouges aux bords de la vision
 * - Vignette pulsante synchronisée avec le battement
 *
 * @author ZombieZ Dopamine System
 */
public class LowHealthHeartbeatManager implements Listener {

    private final ZombieZPlugin plugin;

    // Seuils de déclenchement (pourcentage de vie)
    private static final double DANGER_THRESHOLD = 0.30;      // 30% - début du heartbeat
    private static final double CRITICAL_THRESHOLD = 0.15;    // 15% - heartbeat rapide
    private static final double EXTREME_THRESHOLD = 0.08;     // 8% - heartbeat très rapide

    // Intervalles de battement en ticks (20 ticks = 1 seconde)
    private static final int DANGER_INTERVAL = 30;            // ~1.5s entre battements
    private static final int CRITICAL_INTERVAL = 18;          // ~0.9s entre battements
    private static final int EXTREME_INTERVAL = 10;           // ~0.5s entre battements

    // Map des joueurs avec leur task de battement actif
    private final Map<UUID, HeartbeatData> activeHeartbeats = new ConcurrentHashMap<>();

    // Task principale de monitoring (stockée pour shutdown propre)
    private BukkitTask monitoringTask;

    public LowHealthHeartbeatManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Démarre le système de monitoring
     */
    public void start() {
        // Task de vérification périodique toutes les 10 ticks (0.5s)
        monitoringTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkPlayerHealth(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    /**
     * Arrête proprement le système
     */
    public void shutdown() {
        // Annuler la tâche de monitoring principale
        if (monitoringTask != null && !monitoringTask.isCancelled()) {
            monitoringTask.cancel();
            monitoringTask = null;
        }

        // Annuler toutes les tâches de heartbeat actives
        for (HeartbeatData data : activeHeartbeats.values()) {
            if (data.task != null && !data.task.isCancelled()) {
                data.task.cancel();
            }
        }
        activeHeartbeats.clear();
    }

    /**
     * Vérifie la vie d'un joueur et ajuste le battement de cœur
     */
    private void checkPlayerHealth(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) return;

        double currentHealth = player.getHealth();
        double maxHealth = getMaxHealth(player);
        if (maxHealth <= 0) return; // Protection division par zéro

        double healthPercent = currentHealth / maxHealth;

        UUID uuid = player.getUniqueId();
        HeartbeatData data = activeHeartbeats.get(uuid);

        // Joueur en dessous du seuil de danger
        if (healthPercent <= DANGER_THRESHOLD && healthPercent > 0) {
            int requiredInterval = calculateInterval(healthPercent);

            if (data == null) {
                // Démarrer un nouveau heartbeat
                startHeartbeat(player, requiredInterval);
            } else if (data.currentInterval != requiredInterval) {
                // Ajuster l'intervalle si le pourcentage de vie a changé significativement
                stopHeartbeat(uuid);
                startHeartbeat(player, requiredInterval);
            }
        } else {
            // Joueur au-dessus du seuil ou mort - arrêter le heartbeat
            if (data != null) {
                stopHeartbeat(uuid);
            }
        }
    }

    /**
     * Calcule l'intervalle de battement basé sur le pourcentage de vie
     */
    private int calculateInterval(double healthPercent) {
        if (healthPercent <= EXTREME_THRESHOLD) {
            return EXTREME_INTERVAL;
        } else if (healthPercent <= CRITICAL_THRESHOLD) {
            return CRITICAL_INTERVAL;
        } else {
            return DANGER_INTERVAL;
        }
    }

    /**
     * Démarre le battement de cœur pour un joueur
     */
    private void startHeartbeat(Player player, int interval) {
        UUID uuid = player.getUniqueId();

        // Créer la tâche de battement
        BukkitTask task = new BukkitRunnable() {
            private boolean isBeatOne = true; // Alterner entre les deux battements

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopHeartbeat(uuid);
                    return;
                }

                playHeartbeat(player, isBeatOne);
                isBeatOne = !isBeatOne;
            }
        }.runTaskTimer(plugin, 0L, interval / 2); // Diviser par 2 pour avoir le double-battement

        activeHeartbeats.put(uuid, new HeartbeatData(task, interval));
    }

    /**
     * Arrête le battement de cœur pour un joueur
     */
    private void stopHeartbeat(UUID uuid) {
        HeartbeatData data = activeHeartbeats.remove(uuid);
        if (data != null && data.task != null && !data.task.isCancelled()) {
            data.task.cancel();
        }
    }

    /**
     * Obtient la vie maximum d'un joueur de manière sécurisée
     */
    private double getMaxHealth(Player player) {
        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0; // 20 = valeur par défaut MC
    }

    /**
     * Joue un battement de cœur avec effets audio et visuels
     *
     * @param player Le joueur
     * @param isFirstBeat true pour le premier battement (fort), false pour le second (faible)
     */
    private void playHeartbeat(Player player, boolean isFirstBeat) {
        double maxHealth = getMaxHealth(player);
        double healthPercent = maxHealth > 0 ? player.getHealth() / maxHealth : 1.0;

        // ═══════════════════════════════════════════════════════════════════
        // SON DU BATTEMENT DE CŒUR
        // ═══════════════════════════════════════════════════════════════════

        // Volume et pitch basés sur l'urgence
        float volume = isFirstBeat ? 0.8f : 0.4f;
        float pitch;

        if (healthPercent <= EXTREME_THRESHOLD) {
            pitch = isFirstBeat ? 0.7f : 0.9f;  // Plus grave = plus tendu
            volume = isFirstBeat ? 1.0f : 0.6f;
        } else if (healthPercent <= CRITICAL_THRESHOLD) {
            pitch = isFirstBeat ? 0.8f : 1.0f;
            volume = isFirstBeat ? 0.9f : 0.5f;
        } else {
            pitch = isFirstBeat ? 0.9f : 1.1f;
        }

        // Son principal - Warden Heartbeat (parfait pour l'effet)
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, volume, pitch);

        // Son secondaire subtil pour plus d'immersion (seulement sur le premier battement)
        if (isFirstBeat && healthPercent <= CRITICAL_THRESHOLD) {
            player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.15f, 1.8f);
        }

        // ═══════════════════════════════════════════════════════════════════
        // EFFETS VISUELS - Désactivés car gênants pour le gameplay
        // Le son du battement de cœur est suffisant pour l'immersion
        // ═══════════════════════════════════════════════════════════════════

        // if (isFirstBeat) {
        //     spawnVignetteEffect(player, healthPercent);
        // }
    }

    /**
     * Crée un effet de vignette rouge aux bords de la vision
     * Simule les bords rouges qu'on voit dans les FPS quand on est blessé
     */
    private void spawnVignetteEffect(Player player, double healthPercent) {
        // Intensité basée sur la vie restante
        int particleCount;
        float size;

        if (healthPercent <= EXTREME_THRESHOLD) {
            particleCount = 25;
            size = 2.5f;
        } else if (healthPercent <= CRITICAL_THRESHOLD) {
            particleCount = 15;
            size = 2.0f;
        } else {
            particleCount = 8;
            size = 1.5f;
        }

        // Particules rouges autour du joueur (simulant une vignette)
        // Spawn en cercle autour de la tête du joueur
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            Color.fromRGB(180, 20, 20), // Rouge sang
            size
        );

        // Cercle de particules à la hauteur des yeux
        double radius = 1.2;
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI / particleCount) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Spawn uniquement visible par le joueur (via le packet)
            player.spawnParticle(
                Particle.DUST,
                player.getEyeLocation().add(x, 0.3, z),
                1,
                0, 0, 0, 0,
                dustOptions
            );
        }

        // Effet additionnel pour vie critique: flash rouge subtil au centre
        if (healthPercent <= CRITICAL_THRESHOLD) {
            player.spawnParticle(
                Particle.DUST,
                player.getEyeLocation().add(0, 0, 0),
                3,
                0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0f)
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS - Réaction immédiate aux changements de vie
    // ═══════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Vérifier au tick suivant pour avoir la vie mise à jour
        plugin.getServer().getScheduler().runTask(plugin, () -> checkPlayerHealth(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Vérifier au tick suivant pour avoir la vie mise à jour
        plugin.getServer().getScheduler().runTask(plugin, () -> checkPlayerHealth(player));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Vérifier la vie après un court délai (données chargées)
        plugin.getServer().getScheduler().runTaskLater(plugin,
            () -> checkPlayerHealth(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopHeartbeat(event.getPlayer().getUniqueId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Données du battement de cœur actif pour un joueur
     */
    private static class HeartbeatData {
        final BukkitTask task;
        final int currentInterval;

        HeartbeatData(BukkitTask task, int currentInterval) {
            this.task = task;
            this.currentInterval = currentInterval;
        }
    }
}
