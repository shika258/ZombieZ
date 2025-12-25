package com.rinaorc.zombiez.worldboss.bosses;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.WorldBoss;
import com.rinaorc.zombiez.worldboss.WorldBossType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le Pyromancien Zombie (Magie - Taille x2)
 *
 * Capacité: Crée un cercle de feu au sol. Si un joueur reste dedans, le boss se soigne.
 * Stratégie: Forcer le boss à sortir de sa zone de feu.
 *
 * Mécaniques:
 * - Cercle de feu qui reste au sol pendant 10 secondes
 * - Le boss se soigne pour chaque joueur dans le cercle
 * - Les joueurs dans le cercle subissent des dégâts de feu
 * - Plusieurs cercles peuvent exister simultanément
 */
public class PyromancerBoss extends WorldBoss {

    // Cercles de feu actifs (thread-safe pour accès depuis BukkitRunnable)
    private final Set<FireCircle> activeCircles = ConcurrentHashMap.newKeySet();

    // Configuration
    private static final double CIRCLE_RADIUS = 6.0;
    private static final int CIRCLE_DURATION_TICKS = 200; // 10 secondes
    private static final double HEAL_PER_PLAYER = 20.0;
    private static final double FIRE_DAMAGE = 5.0;
    private static final int MAX_CIRCLES = 3;

    public PyromancerBoss(ZombieZPlugin plugin, int zoneId) {
        super(plugin, WorldBossType.PYROMANCER, zoneId);
    }

    @Override
    protected void useAbility() {
        if (entity == null || !entity.isValid()) return;
        if (activeCircles.size() >= MAX_CIRCLES) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Créer un nouveau cercle de feu à la position du boss
        createFireCircle(bossLoc.clone());

        // Avertissement
        for (Player player : getNearbyPlayers(30)) {
            player.sendMessage("§6" + type.getDisplayName() + " §7invoque un §ccercle de feu§7!");
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        }
    }

    /**
     * Crée un cercle de feu
     */
    private void createFireCircle(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Effet de création
        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 2f, 0.5f);
        world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 2f, 0.8f);

        // Explosion initiale
        world.spawnParticle(Particle.EXPLOSION, center, 3, 1, 0.5, 1, 0);
        world.spawnParticle(Particle.LAVA, center, 20, CIRCLE_RADIUS / 2, 0.5, CIRCLE_RADIUS / 2, 0);

        FireCircle circle = new FireCircle(center);
        activeCircles.add(circle);

        // Tâche pour le cercle
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (ticks >= CIRCLE_DURATION_TICKS || !active || entity == null) {
                    // Fin du cercle
                    activeCircles.remove(circle);
                    // Effet de fin
                    world.spawnParticle(Particle.SMOKE, center, 30, CIRCLE_RADIUS / 2, 0.5, CIRCLE_RADIUS / 2, 0.05);
                    cancel();
                    return;
                }

                // Particules du cercle (toutes les 2 ticks)
                if (ticks % 2 == 0) {
                    drawFireCircle(center);
                }

                // Dégâts et soin (toutes les 10 ticks = 0.5s)
                if (ticks % 10 == 0) {
                    processCircleEffects(center);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Dessine le cercle de feu avec des particules
     */
    private void drawFireCircle(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Cercle extérieur
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            Location particleLoc = center.clone().add(
                Math.cos(rad) * CIRCLE_RADIUS,
                0.2,
                Math.sin(rad) * CIRCLE_RADIUS
            );
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
        }

        // Flammes aléatoires à l'intérieur
        for (int i = 0; i < 5; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = Math.random() * CIRCLE_RADIUS;
            Location flameLoc = center.clone().add(
                Math.cos(angle) * dist,
                0.1,
                Math.sin(angle) * dist
            );
            world.spawnParticle(Particle.FLAME, flameLoc, 2, 0.2, 0.3, 0.2, 0.02);
            world.spawnParticle(Particle.SMOKE, flameLoc.clone().add(0, 0.5, 0), 1, 0.1, 0.2, 0.1, 0.01);
        }
    }

    /**
     * Traite les effets du cercle (dégâts aux joueurs, soin au boss)
     */
    private void processCircleEffects(Location center) {
        if (entity == null || !entity.isValid()) return;

        World world = center.getWorld();
        if (world == null) return;

        int playersInCircle = 0;

        // Vérifier les joueurs dans le cercle
        for (Player player : getNearbyPlayers(CIRCLE_RADIUS + 5)) {
            double distance = player.getLocation().distance(center);

            if (distance <= CIRCLE_RADIUS) {
                playersInCircle++;

                // Dégâts de feu
                player.damage(FIRE_DAMAGE, entity);
                player.setFireTicks(40);

                // Avertissement
                player.sendMessage("§c§l⚠ §7Vous brûlez dans le cercle de feu!");
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1f);

                // Particules sur le joueur
                world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.02);
            }
        }

        // Soigner le boss si des joueurs sont dans le cercle
        if (playersInCircle > 0 && entity.isValid()) {
            double healAmount = HEAL_PER_PLAYER * playersInCircle;
            var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);

            if (maxHealth != null) {
                double newHealth = Math.min(maxHealth.getValue(), entity.getHealth() + healAmount);
                entity.setHealth(newHealth);

                // Effets de soin
                Location bossLoc = entity.getLocation().add(0, 1.5, 0);
                world.spawnParticle(Particle.HEART, bossLoc, 5, 0.5, 0.5, 0.5, 0);
                world.playSound(bossLoc, Sound.ENTITY_WITCH_DRINK, 0.5f, 1.2f);

                // Notification
                for (Player player : getNearbyPlayers(30)) {
                    player.sendMessage("§6" + type.getDisplayName() + " §7se soigne grâce aux flammes! §c(+" +
                        String.format("%.0f", healAmount) + " HP)");
                }
            }
        }
    }

    @Override
    protected void tick() {
        // Appliquer les effets procéduraux des traits
        super.tick();

        // Aura de chaleur autour du boss
        if (entity != null && entity.isValid()) {
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world != null && Math.random() < 0.3) {
                // Petites flammes qui s'échappent
                world.spawnParticle(Particle.SMALL_FLAME,
                    loc.clone().add(
                        (Math.random() - 0.5) * 2,
                        Math.random() * 2.5,
                        (Math.random() - 0.5) * 2
                    ), 1, 0, 0, 0, 0.01);
            }
        }
    }

    @Override
    protected void updateBossBar() {
        super.updateBossBar();

        // Ajouter le nombre de cercles actifs (utilise le nom procédural)
        if (bossBar != null && !activeCircles.isEmpty()) {
            String bossName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();
            bossBar.setTitle(bossName + " §7[Cercles: §c" + activeCircles.size() + "§7] - §c" + getFormattedHealth());
        }
    }

    @Override
    public void cleanup() {
        activeCircles.clear();
        super.cleanup();
    }

    /**
     * Représente un cercle de feu actif
     */
    private static class FireCircle {
        private final Location center;
        private final long creationTime;

        public FireCircle(Location center) {
            this.center = center;
            this.creationTime = System.currentTimeMillis();
        }
    }
}
