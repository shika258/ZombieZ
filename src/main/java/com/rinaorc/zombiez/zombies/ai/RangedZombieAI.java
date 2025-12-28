package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies RANGED
 * Comportement: Reste à distance, projectiles
 * Types: SPITTER
 */
public class RangedZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isRetreating = false;
    private int projectilesFired = 0;

    // Distance préférée
    private static final double PREFERRED_DISTANCE = 12.0;
    private static final double TOO_CLOSE = 6.0;

    public RangedZombieAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 3000; // 3 secondes entre tirs
    }

    @Override
    public void tick() {
        tickCounter++;

        Player target = findNearestPlayer(25);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Comportement de positionnement
        if (distance < TOO_CLOSE && !isRetreating) {
            retreat(target);
        } else if (distance > PREFERRED_DISTANCE * 1.5) {
            // Se rapprocher
            setZombieTarget(target);
        } else if (canUseAbility()) {
            // Tirer
            fireProjectile(target);
            useAbility();
        }

        // Effets visuels du Spitter
        if (tickCounter % 20 == 0) {
            playParticles(Particle.ITEM_SLIME, zombie.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2);
        }
    }

    /**
     * Recule pour maintenir la distance
     */
    private void retreat(Player target) {
        isRetreating = true;

        Vector retreatDir = zombie.getLocation().toVector()
            .subtract(target.getLocation().toVector()).normalize();

        // Sauter en arrière
        zombie.setVelocity(retreatDir.multiply(0.8).setY(0.3));
        playSound(Sound.ENTITY_SLIME_JUMP, 1f, 1.2f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isRetreating = false, 40);
    }

    /**
     * Tire un projectile toxique
     */
    private void fireProjectile(Player target) {
        Location eyeLoc = zombie.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector()
            .subtract(eyeLoc.toVector()).normalize();

        // Ajouter un peu d'imprécision basée sur la distance
        double distance = zombie.getLocation().distance(target.getLocation());
        double spread = Math.min(0.15, distance * 0.01);
        direction.add(new Vector(
            (random.nextDouble() - 0.5) * spread,
            (random.nextDouble() - 0.5) * spread,
            (random.nextDouble() - 0.5) * spread
        ));

        playSound(Sound.ENTITY_LLAMA_SPIT, 1f, 0.8f);

        // Créer le projectile
        SmallFireball projectile = zombie.getWorld().spawn(eyeLoc.add(direction), SmallFireball.class);
        projectile.setDirection(direction);
        projectile.setVelocity(direction.multiply(1.5));
        projectile.setShooter(zombie);
        projectile.setIsIncendiary(false);
        projectile.setYield(0); // Pas d'explosion

        // Particules de suivi
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!projectile.isValid()) {
                task.cancel();
                return;
            }
            playParticles(Particle.ITEM_SLIME, projectile.getLocation(), 3, 0.1, 0.1, 0.1);
        }, 0L, 2L);

        // Impact handler
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (projectile.isValid()) {
                createToxicPool(projectile.getLocation());
                projectile.remove();
            }
        }, 60L); // Auto-expire après 3 secondes

        projectilesFired++;

        // Tir en rafale après plusieurs projectiles
        if (projectilesFired >= 3 && random.nextFloat() < 0.3f) {
            burstFire(target);
            projectilesFired = 0;
        }
    }

    /**
     * Rafale de projectiles
     */
    private void burstFire(Player target) {
        for (int i = 1; i <= 2; i++) {
            final int delay = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid() && target.isOnline()) {
                    fireProjectile(target);
                }
            }, delay * 5L);
        }
    }

    /**
     * Crée une flaque toxique au sol
     */
    private void createToxicPool(Location loc) {
        playSound(Sound.ENTITY_SLIME_SQUISH, 1f, 0.8f);
        playParticles(Particle.ITEM_SLIME, loc, 20, 1, 0.1, 1);

        // Zone de dégâts pendant quelques secondes
        final int duration = 5; // 5 secondes
        for (int second = 0; second < duration; second++) {
            final int tick = second;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Particules de la flaque
                playParticles(Particle.ITEM_SLIME, loc, 5, 1.5, 0.1, 1.5);

                // Dégâts aux joueurs dans la zone
                loc.getWorld().getNearbyEntities(loc, 2, 1, 2).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(2 + level * 0.5, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                    });
            }, tick * 20L);
        }
    }

    @Override
    public void onAttack(Player target) {
        // Le Spitter préfère fuir au corps à corps
        currentTarget = target;

        // Crachat au visage en dernier recours
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        playSound(Sound.ENTITY_LLAMA_SPIT, 1.5f, 1f);
        playParticles(Particle.ITEM_SLIME, target.getEyeLocation(), 10, 0.2, 0.2, 0.2);

        // Puis fuir
        retreat(target);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Fuir quand blessé
        if (attacker instanceof Player player) {
            if (isHealthBelow(0.5) || random.nextFloat() < 0.3f) {
                retreat(player);
            }
        }

        // Explosion toxique quand critique
        if (isHealthBelow(0.2)) {
            Location loc = zombie.getLocation();
            playSound(Sound.ENTITY_SLIME_DEATH, 1.5f, 0.5f);
            applyAreaEffect(4, PotionEffectType.POISON, 100, 1);
            playParticles(Particle.ITEM_SLIME, loc.add(0, 1, 0), 50, 2, 1, 2);
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Explosion de substance toxique à la mort
        createToxicPool(zombie.getLocation());
        playSound(Sound.ENTITY_SLIME_DEATH, 1.5f, 0.6f);
    }
}
