package com.rinaorc.zombiez.worldboss.bosses;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.WorldBoss;
import com.rinaorc.zombiez.worldboss.WorldBossType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Le Brise-Glace (Contrôle - Taille x3)
 *
 * Capacité: Applique "Slowness X" et gèle le sol autour de lui.
 * Stratégie: Utiliser des projectiles à distance car le corps-à-corps est mortel.
 *
 * Mécaniques:
 * - Zone de gel permanente autour du boss
 * - Pulse de Slowness extrême périodique
 * - Transformation visuelle du sol en glace
 * - Dégâts de froid aux joueurs trop proches
 */
public class IceBreakerBoss extends WorldBoss {

    // Zone gelée
    private boolean freezeZoneActive = false;

    // Configuration
    private static final double FREEZE_RADIUS = 10.0;
    private static final double DANGER_RADIUS = 5.0;
    private static final int SLOWNESS_AMPLIFIER = 9; // Slowness X (très puissant)
    private static final double COLD_DAMAGE = 3.0;

    public IceBreakerBoss(ZombieZPlugin plugin, int zoneId) {
        super(plugin, WorldBossType.ICE_BREAKER, zoneId);
    }

    @Override
    public void spawn(Location location) {
        super.spawn(location);
        // Activer la zone gelée
        freezeZoneActive = true;
    }

    @Override
    protected void useAbility() {
        if (entity == null || !entity.isValid()) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Pulse de gel intense
        world.playSound(bossLoc, Sound.BLOCK_GLASS_BREAK, 2f, 0.3f);
        world.playSound(bossLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 2f, 0.5f);

        // Onde visuelle
        for (int radius = 1; radius <= (int) FREEZE_RADIUS; radius++) {
            final int r = radius;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!active || entity == null) return;
                    drawFreezeWave(bossLoc, r);
                }
            }.runTaskLater(plugin, radius * 2L);
        }

        // Appliquer Slowness X à tous les joueurs dans la zone
        for (Player player : getNearbyPlayers(FREEZE_RADIUS)) {
            applyFreezeEffect(player);
        }

        // Annonce
        for (Player player : getNearbyPlayers(30)) {
            player.sendMessage("§b" + type.getDisplayName() + " §7libère une vague de §bfroid intense§7!");
        }
    }

    /**
     * Dessine une onde de gel
     */
    private void drawFreezeWave(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            Location loc = center.clone().add(
                Math.cos(rad) * radius,
                0.5,
                Math.sin(rad) * radius
            );
            world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.2, 0.3, 0.2, 0.02);
            world.spawnParticle(Particle.DUST, loc, 2, 0.1, 0.1, 0.1,
                new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.5f));
        }
    }

    /**
     * Applique l'effet de gel à un joueur
     */
    private void applyFreezeEffect(Player player) {
        // Slowness X
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, SLOWNESS_AMPLIFIER));

        // Mining Fatigue
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 2));

        // Effet visuel de gel
        player.setFreezeTicks(player.getMaxFreezeTicks());

        // Message
        player.sendMessage("§b§l⚠ §7Vous êtes §bgelé§7! Éloignez-vous du boss!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1f);
    }

    @Override
    protected void tick() {
        // Appliquer les effets procéduraux des traits
        super.tick();

        if (entity == null || !entity.isValid()) return;
        if (!freezeZoneActive) return;

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Mettre à jour la zone gelée visuellement
        updateFreezeZone(bossLoc);

        // Dégâts de froid aux joueurs trop proches
        for (Player player : getNearbyPlayers(DANGER_RADIUS)) {
            double distance = player.getLocation().distance(bossLoc);

            // Dégâts inversement proportionnels à la distance
            double damageMultiplier = 1.0 - (distance / DANGER_RADIUS);
            double damage = COLD_DAMAGE * damageMultiplier;

            if (damage > 0) {
                player.damage(damage, entity);
                player.setFreezeTicks(Math.min(player.getMaxFreezeTicks(), player.getFreezeTicks() + 20));

                // Avertissement (toutes les secondes)
                if (Math.random() < 0.3) {
                    player.sendMessage("§b§l⚠ §7Le froid vous consume! Éloignez-vous!");
                }
            }

            // Slowness passive dans la zone de danger
            if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
            }
        }

        // Slowness modéré dans la zone externe
        for (Player player : getNearbyPlayers(FREEZE_RADIUS)) {
            if (player.getLocation().distance(bossLoc) > DANGER_RADIUS) {
                if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                }
            }
        }
    }

    /**
     * Met à jour visuellement la zone gelée
     */
    private void updateFreezeZone(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Particules de neige permanentes
        for (int i = 0; i < 15; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = Math.random() * FREEZE_RADIUS;
            Location particleLoc = center.clone().add(
                Math.cos(angle) * dist,
                0.2 + Math.random() * 2,
                Math.sin(angle) * dist
            );
            world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0.02);
        }

        // Glace au sol (simulation visuelle avec particules)
        if (Math.random() < 0.2) {
            for (int i = 0; i < 5; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * FREEZE_RADIUS;
                Location iceLoc = center.clone().add(
                    Math.cos(angle) * dist,
                    0.1,
                    Math.sin(angle) * dist
                );
                world.spawnParticle(Particle.BLOCK, iceLoc, 2, 0.3, 0.1, 0.3,
                    Material.BLUE_ICE.createBlockData());
            }
        }

        // Son de craquement de glace occasionnel
        if (Math.random() < 0.05) {
            world.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f);
        }
    }

    @Override
    protected void ambientParticles() {
        super.ambientParticles();

        if (entity == null || !entity.isValid()) return;

        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Aura de givre
        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * Math.PI * 2;
            double height = Math.random() * 3 * type.getScale();
            Location frostLoc = loc.clone().add(
                Math.cos(angle) * 1.5,
                height,
                Math.sin(angle) * 1.5
            );
            world.spawnParticle(Particle.SNOWFLAKE, frostLoc, 1, 0.1, 0.1, 0.1, 0.01);
        }

        // Souffle glacé
        if (Math.random() < 0.1) {
            world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    protected void updateBossBar() {
        super.updateBossBar();

        // Utiliser le nom procédural
        if (bossBar != null) {
            String bossName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();
            bossBar.setTitle(bossName + " §7[Zone de gel active] - §c" + getFormattedHealth());
        }
    }

    @Override
    protected void deathEffects(Location location) {
        super.deathEffects(location);

        World world = location.getWorld();
        if (world == null) return;

        // Explosion de glace
        world.playSound(location, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
        world.spawnParticle(Particle.SNOWFLAKE, location, 200, 5, 5, 5, 0.1);
        world.spawnParticle(Particle.BLOCK, location, 100, 3, 3, 3, 0.1,
            Material.BLUE_ICE.createBlockData());
    }

    @Override
    public void cleanup() {
        freezeZoneActive = false;
        super.cleanup();
    }
}
