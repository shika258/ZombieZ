package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies HAZARD
 * Comportement: Crée des zones de danger, effets persistants
 * Types: TOXIC, MUTANT
 */
public class HazardZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int toxicPoolsCreated = 0;
    private static final int MAX_POOLS = 5;

    public HazardZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 6000;
    }

    @Override
    public void tick() {
        tickCounter++;

        switch (zombieType) {
            case TOXIC -> tickToxic();
            case MUTANT -> tickMutant();
            default -> tickToxic();
        }
    }

    /**
     * Toxic: Laisse une traînée de poison, crée des flaques toxiques
     */
    private void tickToxic() {
        // Aura toxique permanente
        if (tickCounter % 10 == 0) {
            playParticles(Particle.ITEM_SLIME, zombie.getLocation().add(0, 0.5, 0), 5, 0.5, 0.3, 0.5);

            // Dégâts aux joueurs très proches
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.damage(1, zombie);
                    if (random.nextFloat() < 0.3f) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                    }
                });
        }

        // Laisser une traînée toxique
        if (tickCounter % 60 == 0 && toxicPoolsCreated < MAX_POOLS) {
            createToxicPool(zombie.getLocation());
            toxicPoolsCreated++;
        }

        // Explosion toxique sur ability
        if (canUseAbility()) {
            Player target = findNearestPlayer(15);
            if (target != null && zombie.getLocation().distance(target.getLocation()) < 8) {
                toxicBurst();
                useAbility();
            }
        }
    }

    /**
     * Mutant: Corps instable, mutations aléatoires, explosions de gore
     */
    private void tickMutant() {
        // Effet visuel de mutation
        if (tickCounter % 20 == 0) {
            playParticles(Particle.DRAGON_BREATH, zombie.getLocation().add(0, 1, 0), 5, 0.4, 0.5, 0.4);
            playParticles(Particle.ITEM_SLIME, zombie.getLocation().add(0, 0.5, 0), 3, 0.3, 0.2, 0.3);
        }

        // Mutation aléatoire toutes les 10 secondes
        if (tickCounter % 200 == 0) {
            randomMutation();
        }

        // Zones de corruption
        if (canUseAbility()) {
            Player target = findNearestPlayer(12);
            if (target != null) {
                corruptionBlast(target);
                useAbility();
            }
        }

        // Instabilité - parfois explose partiellement
        if (tickCounter % 100 == 0 && random.nextFloat() < 0.2f) {
            partialExplosion();
        }
    }

    /**
     * Crée une flaque toxique
     */
    private void createToxicPool(Location loc) {
        // Créer un AreaEffectCloud
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setRadius(2.5f);
        cloud.setRadiusPerTick(-0.005f);
        cloud.setDuration(200); // 10 secondes
        cloud.setWaitTime(0);
        cloud.setColor(org.bukkit.Color.fromRGB(0, 150, 0));
        cloud.setParticle(Particle.ITEM_SLIME);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 0), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0), true);
        cloud.setSource(zombie);

        playSound(Sound.ENTITY_SLIME_SQUISH, 0.8f, 0.6f);
    }

    /**
     * Explosion toxique
     */
    private void toxicBurst() {
        playSound(Sound.ENTITY_SLIME_DEATH, 1.5f, 0.5f);

        // Vague de particules
        for (int i = 0; i < 3; i++) {
            final int ring = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double radius = 2 + ring * 2;
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = zombie.getLocation().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
                    playParticles(Particle.ITEM_SLIME, particleLoc, 3, 0.2, 0.2, 0.2);
                }
            }, ring * 3L);
        }

        // Dégâts et effets
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 6, 3, 6).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double distance = p.getLocation().distance(zombie.getLocation());
                double damage = (8 + level) * (1 - distance / 8);
                p.damage(damage, zombie);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
            });

        // Créer des flaques autour
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 2 + random.nextDouble() * 3;
            Location poolLoc = zombie.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            createToxicPool(poolLoc);
        }
    }

    /**
     * Mutation aléatoire du Mutant
     */
    private void randomMutation() {
        int mutation = random.nextInt(4);

        playSound(Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, 1f, 0.5f);
        playParticles(Particle.DRAGON_BREATH, zombie.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);

        switch (mutation) {
            case 0 -> {
                // Mutation de vitesse
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2, false, true));
                zombie.setCustomName("§5[Rapide] " + zombie.getCustomName());
            }
            case 1 -> {
                // Mutation de force
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 1, false, true));
                zombie.setCustomName("§c[Fort] " + zombie.getCustomName());
            }
            case 2 -> {
                // Mutation de résistance
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 1, false, true));
                zombie.setCustomName("§9[Dur] " + zombie.getCustomName());
            }
            case 3 -> {
                // Mutation régénérative
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 0, false, true));
                zombie.setCustomName("§a[Régén] " + zombie.getCustomName());
            }
        }
    }

    /**
     * Explosion de corruption
     */
    private void corruptionBlast(Player target) {
        playSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        // Rayon de corruption
        Location rayLoc = zombie.getEyeLocation().clone();
        for (int i = 0; i < 15; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location currentLoc = rayLoc.clone().add(direction.clone().multiply(step));
                playParticles(Particle.DRAGON_BREATH, currentLoc, 10, 0.3, 0.3, 0.3);

                // Dégâts sur le chemin
                currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(5 + level, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                    });
            }, i);
        }

        // Zone corrompue à l'impact
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location impactLoc = rayLoc.clone().add(direction.clone().multiply(15));
            createCorruptionZone(impactLoc);
        }, 15L);
    }

    /**
     * Crée une zone de corruption
     */
    private void createCorruptionZone(Location loc) {
        AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
        cloud.setRadius(3f);
        cloud.setRadiusPerTick(-0.01f);
        cloud.setDuration(160);
        cloud.setWaitTime(0);
        cloud.setColor(org.bukkit.Color.fromRGB(100, 0, 100));
        cloud.setParticle(Particle.DRAGON_BREATH);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0), true);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1), true);
        cloud.setSource(zombie);

        playSound(Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
    }

    /**
     * Explosion partielle du Mutant instable
     */
    private void partialExplosion() {
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        playParticles(Particle.ITEM_SLIME, zombie.getLocation().add(0, 1, 0), 30, 1, 1, 1);
        playParticles(Particle.DRAGON_BREATH, zombie.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5);

        // Dégâts et effets aux joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 4, 3, 4).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(4, zombie);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                Vector knockback = p.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(0.8).setY(0.3);
                p.setVelocity(knockback);
            });

        // Auto-dégâts
        zombie.damage(5);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Appliquer poison au contact
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));

        if (zombieType == ZombieType.MUTANT) {
            // Chance de mutation au contact
            if (random.nextFloat() < 0.2f) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            }
        }

        playParticles(Particle.ITEM_SLIME, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Le Toxic suinte quand touché
        if (zombieType == ZombieType.TOXIC) {
            if (random.nextFloat() < 0.3f) {
                createToxicPool(zombie.getLocation());
            }
        }

        // Le Mutant devient instable quand blessé
        if (zombieType == ZombieType.MUTANT && isHealthBelow(0.5)) {
            if (random.nextFloat() < 0.2f) {
                partialExplosion();
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Grande explosion toxique à la mort
        playSound(Sound.ENTITY_SLIME_DEATH, 2f, 0.3f);

        if (zombieType == ZombieType.TOXIC) {
            toxicBurst();
        } else if (zombieType == ZombieType.MUTANT) {
            // Explosion massive de mutation
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            playParticles(Particle.DRAGON_BREATH, zombie.getLocation(), 100, 3, 2, 3);

            applyAreaEffect(6, PotionEffectType.WITHER, 80, 1);
            applyAreaEffect(6, PotionEffectType.NAUSEA, 100, 0);

            // Créer plusieurs zones de corruption
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double dist = 2 + random.nextDouble() * 3;
                Location poolLoc = zombie.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                createCorruptionZone(poolLoc);
            }
        }
    }
}
