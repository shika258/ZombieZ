package com.rinaorc.zombiez.weather;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Effet météo spécial: Invasion Géante
 *
 * Pendant 90 secondes, TOUS les zombies actifs deviennent GÉANTS (scale x5-10)
 *
 * Caractéristiques:
 * - Scale aléatoire entre 5.0 et 10.0 par zombie
 * - +25% loot drop pendant l'événement
 * - Effets visuels: tremblements de sol, particules d'explosion
 * - Sons: rugissements de dragon, explosions
 * - Les nouveaux zombies spawnés pendant l'événement sont aussi géants
 */
public class GiantInvasionEffect extends WeatherEffect {

    // === CONFIGURATION ===
    private static final double MIN_SCALE = 5.0;
    private static final double MAX_SCALE = 10.0;
    private static final Random random = new Random();

    // === TRACKING DES SCALES ORIGINALES ===
    // Map: EntityUUID -> Scale originale (pour restauration)
    private final Map<UUID, Double> originalScales = new ConcurrentHashMap<>();

    // Map: EntityUUID -> Scale appliquée (pour affichage/debug)
    @Getter
    private final Map<UUID, Double> appliedScales = new ConcurrentHashMap<>();

    // === TÂCHES SPÉCIALES ===
    private BukkitTask screenShakeTask;
    private BukkitTask newMobWatcherTask;
    private BukkitTask footstepTask;

    // === STATISTIQUES ===
    @Getter
    private int totalMobsEnlarged = 0;
    @Getter
    private int newMobsEnlarged = 0;

    public GiantInvasionEffect(ZombieZPlugin plugin, int durationTicks) {
        super(plugin, WeatherType.GIANT_INVASION, durationTicks);
    }

    public GiantInvasionEffect(ZombieZPlugin plugin) {
        super(plugin, WeatherType.GIANT_INVASION);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void start() {
        if (active) return;

        // Appeler le parent pour la boss bar, annonces, etc.
        super.start();

        // === AGRANDIR TOUS LES ZOMBIES EXISTANTS ===
        enlargeAllExistingZombies();

        // === DÉMARRER LES TÂCHES SPÉCIALES ===
        startGiantTasks();

        // === ANNONCE SPÉCIALE ===
        announceGiantInvasion();
    }

    @Override
    protected void cleanup() {
        // Arrêter les tâches spéciales
        cancelGiantTasks();

        // === RESTAURER LES SCALES ORIGINALES ===
        restoreAllOriginalScales();

        // Clear les maps
        originalScales.clear();
        appliedScales.clear();

        // Appeler le parent pour le reste du cleanup
        super.cleanup();
    }

    // ==================== AGRANDISSEMENT DES ZOMBIES ====================

    /**
     * Agrandit tous les zombies actifs avec une scale aléatoire x5-10
     */
    private void enlargeAllExistingZombies() {
        var zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        if (targetWorld == null) return;

        // Parcourir toutes les entités du monde
        for (Entity entity : targetWorld.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!zombieManager.isZombieZMob(entity)) continue;

            enlargeZombie(living);
        }

        plugin.log(java.util.logging.Level.INFO,
            "§6[Giant Invasion] " + totalMobsEnlarged + " zombies agrandis!");
    }

    /**
     * Agrandit un zombie spécifique
     */
    private void enlargeZombie(LivingEntity zombie) {
        UUID entityId = zombie.getUniqueId();

        // Si déjà agrandi, skip
        if (originalScales.containsKey(entityId)) return;

        // Sauvegarder la scale originale
        var scaleAttr = zombie.getAttribute(Attribute.SCALE);
        if (scaleAttr == null) return;

        double originalScale = scaleAttr.getBaseValue();
        originalScales.put(entityId, originalScale);

        // Générer une scale aléatoire entre 5 et 10
        double giantScale = MIN_SCALE + (random.nextDouble() * (MAX_SCALE - MIN_SCALE));

        // Appliquer la nouvelle scale
        scaleAttr.setBaseValue(giantScale);
        appliedScales.put(entityId, giantScale);

        // Effets visuels sur le zombie
        Location loc = zombie.getLocation();
        zombie.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0);
        zombie.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 1, 1, 1, 0.1);
        zombie.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.5f);

        totalMobsEnlarged++;
    }

    /**
     * Restaure la scale originale d'un zombie
     */
    private void restoreZombieScale(LivingEntity zombie) {
        UUID entityId = zombie.getUniqueId();

        Double originalScale = originalScales.get(entityId);
        if (originalScale == null) return;

        var scaleAttr = zombie.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(originalScale);
        }

        // Effets visuels de rétrécissement
        Location loc = zombie.getLocation();
        zombie.getWorld().spawnParticle(Particle.POOF, loc, 15, 1, 1, 1, 0.05);
        zombie.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.5f);
    }

    /**
     * Restaure toutes les scales originales
     */
    private void restoreAllOriginalScales() {
        if (targetWorld == null) return;

        int restored = 0;
        for (Entity entity : targetWorld.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!originalScales.containsKey(entity.getUniqueId())) continue;

            restoreZombieScale(living);
            restored++;
        }

        plugin.log(java.util.logging.Level.INFO,
            "§a[Giant Invasion] " + restored + " zombies restaurés à leur taille normale");
    }

    // ==================== TÂCHES SPÉCIALES ====================

    private void startGiantTasks() {
        // === SCREEN SHAKE EFFECT (toutes les 3 secondes) ===
        screenShakeTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active) return;

            // Tremblement de sol pour tous les joueurs
            for (Player player : cachedPlayers) {
                applyScreenShake(player);
            }
        }, 60L, 60L); // Toutes les 3 secondes

        // === NEW MOB WATCHER (toutes les 0.5 secondes) ===
        // Surveille les nouveaux zombies spawnés et les agrandit
        newMobWatcherTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active) return;
            watchForNewMobs();
        }, 10L, 10L); // Toutes les 0.5 secondes

        // === FOOTSTEP SOUNDS (toutes les secondes) ===
        footstepTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active) return;
            playGiantFootsteps();
        }, 20L, 20L); // Toutes les secondes
    }

    private void cancelGiantTasks() {
        if (screenShakeTask != null && !screenShakeTask.isCancelled()) {
            screenShakeTask.cancel();
        }
        if (newMobWatcherTask != null && !newMobWatcherTask.isCancelled()) {
            newMobWatcherTask.cancel();
        }
        if (footstepTask != null && !footstepTask.isCancelled()) {
            footstepTask.cancel();
        }
    }

    /**
     * Applique un effet de tremblement de sol au joueur
     */
    private void applyScreenShake(Player player) {
        // Utiliser des particules et sons pour simuler le tremblement
        Location playerLoc = player.getLocation();

        // Son de tremblement de sol
        player.playSound(playerLoc, Sound.ENTITY_RAVAGER_STEP, 0.8f, 0.5f);

        // Particules au sol autour du joueur
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI / 8) * i;
            double x = playerLoc.getX() + Math.cos(angle) * 3;
            double z = playerLoc.getZ() + Math.sin(angle) * 3;
            Location particleLoc = new Location(playerLoc.getWorld(), x, playerLoc.getY(), z);

            player.spawnParticle(Particle.BLOCK, particleLoc, 5, 0.2, 0.1, 0.2, 0,
                Material.DIRT.createBlockData());
        }

        // Son distant d'explosion occasionnel
        if (random.nextDouble() < 0.3) {
            player.playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 0.8f);
        }
    }

    /**
     * Surveille les nouveaux zombies et les agrandit
     */
    private void watchForNewMobs() {
        var zombieManager = plugin.getZombieManager();
        if (zombieManager == null || targetWorld == null) return;

        for (Entity entity : targetWorld.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!zombieManager.isZombieZMob(entity)) continue;

            // Si pas encore agrandi
            if (!originalScales.containsKey(entity.getUniqueId())) {
                enlargeZombie(living);
                newMobsEnlarged++;
            }
        }
    }

    /**
     * Joue des sons de pas de géants aléatoires
     */
    private void playGiantFootsteps() {
        if (targetWorld == null) return;

        for (Player player : cachedPlayers) {
            // Trouver un zombie géant proche
            Entity nearestGiant = null;
            double nearestDist = 50 * 50; // 50 blocs

            for (Entity entity : player.getNearbyEntities(50, 30, 50)) {
                if (appliedScales.containsKey(entity.getUniqueId())) {
                    double dist = entity.getLocation().distanceSquared(player.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestGiant = entity;
                    }
                }
            }

            // Jouer le son de pas depuis la direction du géant
            if (nearestGiant != null) {
                Location giantLoc = nearestGiant.getLocation();
                Double scale = appliedScales.get(nearestGiant.getUniqueId());

                // Volume et pitch basés sur la taille
                float volume = (float) (0.3 + (scale / 10.0) * 0.5);
                float pitch = (float) (1.0 - (scale / 10.0) * 0.4);

                player.playSound(giantLoc, Sound.ENTITY_IRON_GOLEM_STEP, volume, pitch);

                // Particules de pas
                player.spawnParticle(Particle.BLOCK, giantLoc, 8, 0.3, 0.1, 0.3, 0,
                    Material.STONE.createBlockData());
            }
        }
    }

    // ==================== EFFETS VISUELS ====================

    @Override
    protected void spawnWeatherParticles(Player player, Location loc, int count) {
        // Particules de base (explosions lointaines)
        for (int i = 0; i < count / 2; i++) {
            double x = loc.getX() + (random.nextDouble() - 0.5) * 30;
            double y = loc.getY() + random.nextDouble() * 10;
            double z = loc.getZ() + (random.nextDouble() - 0.5) * 30;

            Location particleLoc = new Location(loc.getWorld(), x, y, z);
            player.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.5, 0.5, 0.5, 0.01);
        }
    }

    @Override
    protected void spawnSpecialParticles(Player player, Location loc) {
        // Particules de poussière au sol (effet séisme)
        for (int i = 0; i < 5; i++) {
            double x = loc.getX() + (random.nextDouble() - 0.5) * 8;
            double z = loc.getZ() + (random.nextDouble() - 0.5) * 8;
            Location groundLoc = new Location(loc.getWorld(), x, loc.getY(), z);

            // Couleur terre/brun
            Color dirtColor = Color.fromRGB(139, 90, 43);
            player.spawnParticle(Particle.DUST, groundLoc, 2, 0.2, 0.1, 0.2, 0,
                new Particle.DustOptions(dirtColor, 1.5f));
        }

        // Explosions occasionnelles dans le ciel
        if (random.nextDouble() < 0.1) {
            double x = loc.getX() + (random.nextDouble() - 0.5) * 40;
            double y = loc.getY() + 15 + random.nextDouble() * 10;
            double z = loc.getZ() + (random.nextDouble() - 0.5) * 40;

            player.spawnParticle(Particle.EXPLOSION, new Location(loc.getWorld(), x, y, z), 1);
        }
    }

    // ==================== ANNONCES SPÉCIALES ====================

    private void announceGiantInvasion() {
        for (Player player : getAffectedPlayers()) {
            // Title épique
            player.sendTitle(
                "§6§l🦖 INVASION GÉANTE!",
                "§eLes zombies sont ÉNORMES! §a+25% LOOT!",
                10, 80, 20
            );

            // Son dramatique
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.5f);

            // Message détaillé dans le chat
            player.sendMessage("");
            player.sendMessage("§6§l═══════════════════════════════════");
            player.sendMessage("§6§l🦖 INVASION GÉANTE! 🦖");
            player.sendMessage("§e§oLes zombies ont muté et sont devenus GÉANTS!");
            player.sendMessage("");
            player.sendMessage("§7► Taille zombies: §c§lx5 à x10");
            player.sendMessage("§7► Durée: §e90 secondes");
            player.sendMessage("§7► Bonus Loot: §a+25%");
            player.sendMessage("§7► Danger: §c+50% dégâts zombies");
            player.sendMessage("");
            player.sendMessage("§e§lSURVIVEZ ET RÉCOLTEZ LES RÉCOMPENSES!");
            player.sendMessage("§6§l═══════════════════════════════════");
            player.sendMessage("");

            // Particules d'explosion autour du joueur
            player.spawnParticle(Particle.EXPLOSION_EMITTER,
                player.getLocation().add(0, 1, 0), 1);
        }
    }

    @Override
    protected void announceEnd() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle(
                "§a§l✓ FIN DE L'INVASION",
                "§7Les zombies retrouvent leur taille normale...",
                10, 60, 20
            );

            player.sendMessage("");
            player.sendMessage("§a§l✓ §7L'§eInvasion Géante §7est terminée!");
            player.sendMessage("§7Zombies agrandis: §e" + totalMobsEnlarged);
            player.sendMessage("§7Nouveaux spawns géants: §e" + newMobsEnlarged);
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
    }

    // ==================== BOSS BAR OVERRIDE ====================

    @Override
    protected String buildBossBarTitle() {
        int remainingSeconds = getRemainingTimeSeconds();
        String timeStr = formatTime(remainingSeconds);

        // Afficher le nombre de géants actifs
        int giantCount = appliedScales.size();

        return "§6§l🦖 INVASION GÉANTE §7- §e" + timeStr +
               " §7| §c" + giantCount + " GÉANTS §7| §a+25% LOOT";
    }

    // ==================== DEBUG ====================

    @Override
    public String getDebugInfo() {
        return String.format(
            "[%s] Giant Invasion | %ds | Enlarged: %d | New: %d | Active Giants: %d",
            id, getRemainingTimeSeconds(), totalMobsEnlarged,
            newMobsEnlarged, appliedScales.size()
        );
    }
}
