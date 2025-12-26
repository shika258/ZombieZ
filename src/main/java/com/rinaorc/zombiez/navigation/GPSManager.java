package com.rinaorc.zombiez.navigation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Système GPS avec flèche 3D visible pour guider les joueurs vers les objectifs
 *
 * Utilise des ItemDisplay entities avec:
 * - Une flèche spectrale visible et brillante
 * - Rotation fluide vers la destination
 * - Effet de pulsation quand proche
 * - Glow coloré via scoreboard team
 */
public class GPSManager implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // Joueurs avec GPS actif et leurs entités flèche
    private final Map<UUID, GPSArrow> activeArrows = new ConcurrentHashMap<>();

    // Patterns pour extraire les coordonnées
    private static final Pattern COORD_PATTERN = Pattern.compile("§b~?(-?\\d+),?\\s*~?(-?\\d+),?\\s*~?(-?\\d+)");

    // Configuration
    private static final double ARROW_DISTANCE = 3.0;    // Distance devant le joueur
    private static final double ARROW_HEIGHT = 1.5;      // Hauteur au niveau des yeux
    private static final float ARROW_SCALE = 1.8f;       // Taille de la flèche
    private static final float LERP_SPEED = 0.15f;       // Vitesse d'interpolation
    private static final double ARRIVAL_DISTANCE = 5.0;  // Distance pour effet pulse

    // Team pour le glow coloré
    private Team gpsTeam;
    private static final String GPS_TEAM_NAME = "zombiez_gps";

    // Tâche de mise à jour
    private BukkitTask updateTask;

    public GPSManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        setupGlowTeam();
        startUpdateTask();
    }

    /**
     * Configure le scoreboard team pour le glow jaune
     */
    private void setupGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Supprimer l'ancienne team si elle existe
        gpsTeam = scoreboard.getTeam(GPS_TEAM_NAME);
        if (gpsTeam != null) {
            gpsTeam.unregister();
        }

        // Créer la nouvelle team avec couleur jaune
        gpsTeam = scoreboard.registerNewTeam(GPS_TEAM_NAME);
        gpsTeam.color(net.kyori.adventure.text.format.NamedTextColor.GOLD);
        gpsTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    /**
     * Toggle le GPS pour un joueur
     */
    public boolean toggleGPS(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeArrows.containsKey(uuid)) {
            disableGPS(player);
            return false;
        } else {
            return enableGPS(player);
        }
    }

    /**
     * Active le GPS pour un joueur
     */
    public boolean enableGPS(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier si l'étape actuelle a des coordonnées
        Location destination = getDestinationForPlayer(player);
        if (destination == null) {
            player.sendMessage("§c§l✗ §7Aucune destination disponible pour cette étape.");
            player.sendMessage("§8Le GPS fonctionne pour les étapes avec coordonnées.");
            return false;
        }

        // Supprimer l'ancienne flèche si existante
        GPSArrow oldArrow = activeArrows.remove(uuid);
        if (oldArrow != null) {
            oldArrow.destroy();
        }

        // Créer la nouvelle flèche
        GPSArrow arrow = new GPSArrow(player, destination);
        activeArrows.put(uuid, arrow);

        // Feedback
        double distance = player.getLocation().distance(destination);
        player.sendMessage("§a§l✓ §eGPS activé! §7Distance: §e" + String.format("%.0f", distance) + " blocs");
        player.sendMessage("§7Suivez la §eflèche dorée §7pour atteindre votre objectif.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);

        return true;
    }

    /**
     * Désactive le GPS pour un joueur
     */
    public void disableGPS(Player player) {
        UUID uuid = player.getUniqueId();

        GPSArrow arrow = activeArrows.remove(uuid);
        if (arrow != null) {
            arrow.destroy();
        }

        player.sendMessage("§c§l✗ §7GPS désactivé.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    /**
     * Vérifie si le GPS est actif pour un joueur
     */
    public boolean isGPSActive(Player player) {
        return activeArrows.containsKey(player.getUniqueId());
    }

    /**
     * Obtient la destination de l'étape actuelle du joueur
     */
    public Location getDestinationForPlayer(Player player) {
        JourneyStep step = journeyManager.getCurrentStep(player);
        if (step == null) return null;

        return parseCoordinates(step.getDescription(), player.getWorld().getName());
    }

    /**
     * Parse les coordonnées depuis une description d'étape
     */
    private Location parseCoordinates(String description, String worldName) {
        if (description == null) return null;

        Matcher matcher = COORD_PATTERN.matcher(description);
        if (!matcher.find()) return null;

        try {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            return new Location(plugin.getServer().getWorld(worldName), x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Démarre la tâche de mise à jour (tous les 2 ticks = 10 FPS)
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, GPSArrow>> iterator = activeArrows.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, GPSArrow> entry = iterator.next();
                    UUID uuid = entry.getKey();
                    GPSArrow arrow = entry.getValue();

                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        arrow.destroy();
                        iterator.remove();
                        continue;
                    }

                    // Vérifier si la destination a changé
                    Location newDest = getDestinationForPlayer(player);
                    if (newDest == null) {
                        arrow.destroy();
                        iterator.remove();
                        player.sendMessage("§e§l! §7Objectif atteint ou changé - GPS désactivé.");
                        continue;
                    }

                    // Mettre à jour la destination si changée
                    if (!isSameLocation(arrow.destination, newDest)) {
                        arrow.destination = newDest;
                    }

                    // Mettre à jour la position et rotation de la flèche
                    arrow.update(player);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private boolean isSameLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getBlockX() == b.getBlockX() &&
               a.getBlockY() == b.getBlockY() &&
               a.getBlockZ() == b.getBlockZ();
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GPSArrow arrow = activeArrows.remove(uuid);
        if (arrow != null) {
            arrow.destroy();
        }
    }

    /**
     * Arrête proprement le manager
     */
    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        for (GPSArrow arrow : activeArrows.values()) {
            arrow.destroy();
        }
        activeArrows.clear();

        // Nettoyer la team
        if (gpsTeam != null) {
            try {
                gpsTeam.unregister();
            } catch (Exception ignored) {}
        }
    }

    // ==================== CLASSE INTERNE: GPS ARROW ====================

    /**
     * Représente une flèche GPS pour un joueur
     * Utilise ItemDisplay pour une visibilité maximale
     */
    private class GPSArrow {
        private final UUID playerUuid;
        private Entity arrowEntity;
        private Location destination;

        // État d'interpolation
        private Location currentPosition;
        private float currentYaw = 0;
        private float currentPitch = 0;
        private long lastUpdate = 0;

        GPSArrow(Player player, Location destination) {
            this.playerUuid = player.getUniqueId();
            this.destination = destination;
            this.currentPosition = calculateTargetPosition(player.getLocation());

            spawn(player);
        }

        /**
         * Spawn l'entité flèche
         */
        private void spawn(Player player) {
            World world = player.getWorld();
            Location spawnLoc = calculateTargetPosition(player.getLocation());

            arrowEntity = world.spawn(spawnLoc, ItemDisplay.class, display -> {
                // Item: Flèche spectrale (très visible, brillante)
                display.setItemStack(new ItemStack(Material.SPECTRAL_ARROW));

                // Transformation: Scale et rotation initiale
                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),           // Translation
                    new AxisAngle4f(0, 0, 1, 0),    // Left rotation
                    new Vector3f(ARROW_SCALE, ARROW_SCALE, ARROW_SCALE), // Scale
                    new AxisAngle4f(0, 0, 1, 0)     // Right rotation
                ));

                // Billboard: CENTER pour toujours voir la flèche
                display.setBillboard(Display.Billboard.FIXED);

                // Glow pour visibilité
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 215, 0)); // Or

                // Visibilité
                display.setViewRange(128f);
                display.setShadowRadius(0);
                display.setShadowStrength(0);

                // Pas de collision
                display.setCustomNameVisible(false);
            });

            // Ajouter à la team pour le glow coloré
            if (gpsTeam != null && arrowEntity != null) {
                gpsTeam.addEntity(arrowEntity);
            }
        }

        /**
         * Met à jour la position et rotation de la flèche
         */
        void update(Player player) {
            if (arrowEntity == null || !arrowEntity.isValid()) {
                // Respawn si l'entité a été supprimée
                spawn(player);
                return;
            }

            Location playerLoc = player.getLocation();
            Location targetPos = calculateTargetPosition(playerLoc);

            // Calculer la direction vers la destination
            Vector toDestination = destination.toVector().subtract(playerLoc.toVector());
            double distance = toDestination.length();

            // Normaliser
            if (distance > 0.1) {
                toDestination.normalize();
            } else {
                toDestination = playerLoc.getDirection();
            }

            // Angles cibles
            float targetYaw = (float) Math.toDegrees(Math.atan2(-toDestination.getX(), toDestination.getZ()));
            float targetPitch = (float) Math.toDegrees(-Math.asin(toDestination.getY()));

            // Interpolation fluide
            currentPosition = lerp(currentPosition, targetPos, LERP_SPEED);
            currentYaw = lerpAngle(currentYaw, targetYaw, LERP_SPEED);
            currentPitch = lerpAngle(currentPitch, targetPitch, LERP_SPEED * 0.5f);

            // Effet de pulsation si proche
            float scale = ARROW_SCALE;
            if (distance < ARRIVAL_DISTANCE) {
                // Pulsation rapide
                scale = ARROW_SCALE * (0.8f + 0.4f * (float) Math.sin(System.currentTimeMillis() * 0.01));
            }

            // Téléporter l'entité
            Location newLoc = currentPosition.clone();
            newLoc.setYaw(currentYaw);
            newLoc.setPitch(currentPitch);
            arrowEntity.teleport(newLoc);

            // Mettre à jour la transformation pour la rotation de la flèche
            if (arrowEntity instanceof ItemDisplay display) {
                // Convertir yaw/pitch en rotation pour que la flèche pointe vers la destination
                float yawRad = (float) Math.toRadians(-currentYaw + 90); // Ajustement pour pointer correctement
                float pitchRad = (float) Math.toRadians(currentPitch);

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(pitchRad, 1, 0, 0), // Pitch rotation
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(yawRad, 0, 1, 0)    // Yaw rotation
                ));
            }

            // ActionBar avec distance (toutes les 10 updates = 1 seconde)
            lastUpdate++;
            if (lastUpdate % 10 == 0) {
                String distStr = distance < 10 ? String.format("%.1f", distance) : String.format("%.0f", distance);
                player.sendActionBar(Component.text("§e➤ §7Distance: §e" + distStr + " blocs"));
            }
        }

        /**
         * Calcule la position cible de la flèche devant le joueur
         */
        private Location calculateTargetPosition(Location playerLoc) {
            // Direction vers la destination
            Vector toDestination = destination.toVector().subtract(playerLoc.toVector());
            if (toDestination.length() > 0.1) {
                toDestination.normalize();
            } else {
                toDestination = playerLoc.getDirection();
            }

            // Position devant le joueur, vers la destination
            return playerLoc.clone()
                .add(0, ARROW_HEIGHT, 0)
                .add(toDestination.multiply(ARROW_DISTANCE));
        }

        /**
         * Détruit l'entité flèche
         */
        void destroy() {
            if (arrowEntity != null && arrowEntity.isValid()) {
                arrowEntity.remove();
            }
            arrowEntity = null;
        }

        // Utilitaires d'interpolation
        private Location lerp(Location from, Location to, double t) {
            if (from == null) return to;
            return new Location(
                from.getWorld(),
                from.getX() + (to.getX() - from.getX()) * t,
                from.getY() + (to.getY() - from.getY()) * t,
                from.getZ() + (to.getZ() - from.getZ()) * t
            );
        }

        private float lerpAngle(float from, float to, float t) {
            float diff = to - from;
            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            return from + diff * t;
        }
    }
}
