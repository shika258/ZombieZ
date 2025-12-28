package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies EXPLOSIVE
 * Comportement: Explose à la mort ou quand proche, dégâts de zone
 * Types: BLOATER, EXPLOSIVE, CREEPER
 */
public class ExplosiveZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isFuseActive = false;
    private int fuseTime = 0;
    private boolean hasExploded = false;

    // Paramètres d'explosion
    private static final int BLOATER_FUSE_TICKS = 20; // 1 seconde pour Bloater
    private static final int EXPLOSIVE_FUSE_TICKS = 40; // 2 secondes pour Explosive
    private static final int CREEPER_FUSE_TICKS = 30; // 1.5 secondes pour Creeper
    private static final double TRIGGER_DISTANCE = 3.0;
    private static final double CREEPER_TRIGGER_DISTANCE = 2.5;

    public ExplosiveZombieAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 10000;
    }

    @Override
    public void tick() {
        tickCounter++;

        if (hasExploded) return;

        switch (zombieType) {
            case BLOATER -> tickBloater();
            case EXPLOSIVE -> tickExplosive();
            case CREEPER -> tickCreeper();
            default -> tickBloater();
        }
    }

    /**
     * Creeper: Se faufile silencieusement et explose violemment
     * Comportement iconique de Minecraft
     */
    private void tickCreeper() {
        // Effet de tension permanente
        if (tickCounter % 40 == 0 && !isFuseActive) {
            // Le Creeper est silencieux... effrayant
            playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1, 0), 2, 0.2, 0.3, 0.2);
        }

        Player target = findNearestPlayer(20);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Se rapprocher furtivement
        if (!isFuseActive) {
            zombie.setTarget(target);

            // Vitesse normale, pas de bruit
            if (distance > 10) {
                // Marche normale vers la cible
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 0, false, false));
            }
        }

        // Déclencher la mèche quand très proche
        if (distance < CREEPER_TRIGGER_DISTANCE && !isFuseActive) {
            startCreeperFuse();
        }

        // Compte à rebours de la mèche
        if (isFuseActive) {
            fuseTime++;
            updateCreeperFuseEffects();

            // Le creeper s'arrête pendant l'explosion
            zombie.setTarget(null);

            // Annuler si le joueur s'éloigne
            if (distance > 5 && fuseTime < CREEPER_FUSE_TICKS / 2) {
                cancelFuse();
            } else if (fuseTime >= CREEPER_FUSE_TICKS) {
                creeperExplosion();
            }
        }
    }

    /**
     * Démarre la mèche du Creeper
     */
    private void startCreeperFuse() {
        isFuseActive = true;
        fuseTime = 0;

        playSound(Sound.ENTITY_CREEPER_PRIMED, 1.5f, 1f);

        // Le Creeper gonfle (effet visuel)
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, false));
    }

    /**
     * Annule la mèche si le joueur s'éloigne
     */
    private void cancelFuse() {
        isFuseActive = false;
        fuseTime = 0;
        zombie.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    /**
     * Effets visuels pendant le compte à rebours du Creeper
     */
    private void updateCreeperFuseEffects() {
        float progress = (float) fuseTime / CREEPER_FUSE_TICKS;

        // Le Creeper "clignote" en blanc de plus en plus vite
        if (fuseTime % Math.max(1, (int) (8 - progress * 6)) == 0) {
            playParticles(Particle.ELECTRIC_SPARK, zombie.getLocation().add(0, 1, 0),
                (int) (5 + progress * 15), 0.4, 0.5, 0.4);
            playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f + progress);
        }

        // Fumée croissante
        playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1, 0),
            (int) (3 + progress * 10), 0.3, 0.4, 0.3);
    }

    /**
     * Explosion du Creeper - massive et dévastatrice
     */
    private void creeperExplosion() {
        hasExploded = true;
        Location loc = zombie.getLocation();

        // Son d'explosion iconique
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
        playSound(Sound.ENTITY_CREEPER_DEATH, 1.5f, 1f);

        // Effet visuel massif
        playParticles(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0);
        for (int i = 0; i < 30; i++) {
            double x = (random.nextDouble() - 0.5) * 6;
            double y = random.nextDouble() * 4;
            double z = (random.nextDouble() - 0.5) * 6;
            playParticles(Particle.LARGE_SMOKE, loc.clone().add(x, y, z), 2, 0.1, 0.1, 0.1);
        }

        // Dégâts massifs - le Creeper est TRÈS dangereux
        double explosionRadius = 5 + level * 0.4;
        double maxDamage = 20 + level * 3; // Dégâts énormes

        zombie.getWorld().getNearbyEntities(loc, explosionRadius, explosionRadius, explosionRadius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double distance = p.getLocation().distance(loc);
                double damageMultiplier = 1 - (distance / explosionRadius);
                double damage = maxDamage * damageMultiplier;

                p.damage(Math.max(damage, 5), zombie);

                // Knockback massif
                Vector knockback = p.getLocation().toVector().subtract(loc.toVector()).normalize()
                    .multiply(2.5 * damageMultiplier).setY(1.0);
                p.setVelocity(knockback);

                // Effet de surdité (nausée)
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
            });

        // Tuer le zombie
        zombie.setHealth(0);
    }

    /**
     * Bloater: Gonfle et explose en zone de gaz toxique
     */
    private void tickBloater() {
        // Effet visuel de gonflement
        if (tickCounter % 30 == 0) {
            playParticles(Particle.ITEM_SLIME, zombie.getLocation().add(0, 1, 0), 5, 0.4, 0.4, 0.4);
            playSound(Sound.BLOCK_HONEY_BLOCK_SLIDE, 0.4f, 0.6f);
        }

        Player target = findNearestPlayer(15);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Déclencher la mèche si proche
        if (distance < TRIGGER_DISTANCE && !isFuseActive) {
            startFuse(BLOATER_FUSE_TICKS);
        }

        // Compte à rebours de la mèche
        if (isFuseActive) {
            fuseTime++;
            updateFuseEffects();

            if (fuseTime >= BLOATER_FUSE_TICKS) {
                bloaterExplosion();
            }
        }
    }

    /**
     * Explosive: Court vers la cible et explose violemment
     */
    private void tickExplosive() {
        // Effet de fumée permanente
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3);
        }

        Player target = findNearestPlayer(20);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Courir vers la cible
        if (!isFuseActive) {
            zombie.setTarget(target);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
        }

        // Déclencher la mèche si proche
        if (distance < TRIGGER_DISTANCE && !isFuseActive) {
            startFuse(EXPLOSIVE_FUSE_TICKS);
            // Cri de kamikaze
            playSound(Sound.ENTITY_CREEPER_PRIMED, 1.5f, 0.5f);
        }

        // Compte à rebours
        if (isFuseActive) {
            fuseTime++;
            updateFuseEffects();

            // Se bloquer sur place pendant l'explosion
            zombie.setTarget(null);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));

            if (fuseTime >= EXPLOSIVE_FUSE_TICKS) {
                kamikazeExplosion();
            }
        }
    }

    /**
     * Démarre la mèche
     */
    private void startFuse(int maxFuse) {
        isFuseActive = true;
        fuseTime = 0;

        playSound(Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
        playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3);

        // Effet de gonflement visuel
        zombie.setCustomName("§c§l⚠ " + zombie.getCustomName() + " §c§l⚠");
    }

    /**
     * Effets visuels pendant le compte à rebours
     */
    private void updateFuseEffects() {
        // Plus on approche de l'explosion, plus c'est intense
        int maxFuse = zombieType == ZombieType.BLOATER ? BLOATER_FUSE_TICKS : EXPLOSIVE_FUSE_TICKS;
        float progress = (float) fuseTime / maxFuse;

        // Particules de plus en plus intenses
        int particleCount = (int) (5 + progress * 20);
        playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1, 0), particleCount, 0.3, 0.3, 0.3);

        // Son de tick qui accélère
        if (fuseTime % Math.max(1, (int) (10 - progress * 8)) == 0) {
            playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f + progress);
        }

        // Lueur rouge
        if (zombieType == ZombieType.EXPLOSIVE && fuseTime % 5 == 0) {
            playParticles(Particle.FLAME, zombie.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2);
        }
    }

    /**
     * Explosion du Bloater - zone de gaz toxique
     */
    private void bloaterExplosion() {
        hasExploded = true;
        Location loc = zombie.getLocation();

        // Effets visuels
        playSound(Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.4f);
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.8f);

        // Nuage de particules
        for (int i = 0; i < 50; i++) {
            double x = (random.nextDouble() - 0.5) * 6;
            double y = random.nextDouble() * 3;
            double z = (random.nextDouble() - 0.5) * 6;
            playParticles(Particle.ITEM_SLIME, loc.clone().add(x, y, z), 1, 0, 0, 0);
        }

        // Dégâts initiaux
        double explosionRadius = 4 + level * 0.2;
        zombie.getWorld().getNearbyEntities(loc, explosionRadius, explosionRadius, explosionRadius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double damage = 6 + level;
                p.damage(damage, zombie);
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
            });

        // Zone de gaz persistante (5 secondes)
        createToxicCloud(loc, 5);

        // Tuer le zombie
        zombie.setHealth(0);
    }

    /**
     * Explosion kamikaze - dégâts massifs
     */
    private void kamikazeExplosion() {
        hasExploded = true;
        Location loc = zombie.getLocation();

        // Effets visuels intenses
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        playSound(Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.8f);

        // Explosion visuelle
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * (2 + r * 2);
                    double z = Math.sin(rad) * (2 + r * 2);
                    playParticles(Particle.EXPLOSION, loc.clone().add(x, 0.5, z), 1, 0, 0, 0);
                    playParticles(Particle.FLAME, loc.clone().add(x, 0.5, z), 3, 0.2, 0.2, 0.2);
                }
            }, r * 2L);
        }

        // Dégâts et knockback
        double explosionRadius = 5 + level * 0.3;
        zombie.getWorld().getNearbyEntities(loc, explosionRadius, explosionRadius, explosionRadius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double distance = p.getLocation().distance(loc);
                double damageMultiplier = 1 - (distance / explosionRadius);
                double damage = (15 + level * 2) * damageMultiplier;

                p.damage(damage, zombie);

                // Knockback
                Vector knockback = p.getLocation().toVector().subtract(loc.toVector()).normalize()
                    .multiply(2 * damageMultiplier).setY(0.8);
                p.setVelocity(knockback);

                // Effets
                p.setFireTicks(60);
            });

        // Tuer le zombie
        zombie.setHealth(0);
    }

    /**
     * Crée un nuage toxique persistant
     */
    private void createToxicCloud(Location center, int durationSeconds) {
        for (int second = 0; second < durationSeconds; second++) {
            final int tick = second;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Particules du nuage
                for (int i = 0; i < 20; i++) {
                    double x = (random.nextDouble() - 0.5) * 6;
                    double y = random.nextDouble() * 2;
                    double z = (random.nextDouble() - 0.5) * 6;
                    center.getWorld().spawnParticle(Particle.ITEM_SLIME, center.clone().add(x, y, z), 1, 0, 0, 0);
                }

                // Dégâts aux joueurs dans le nuage
                center.getWorld().getNearbyEntities(center, 4, 2, 4).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(2, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                    });
            }, tick * 20L);
        }
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Si au corps à corps, déclencher la mèche
        if (!isFuseActive) {
            if (zombieType == ZombieType.CREEPER) {
                startCreeperFuse();
            } else {
                int fuseTicks = zombieType == ZombieType.BLOATER ? BLOATER_FUSE_TICKS : EXPLOSIVE_FUSE_TICKS;
                startFuse(fuseTicks / 2); // Mèche plus courte au corps à corps
            }
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Si critique, accélérer la mèche
        if (isFuseActive) {
            fuseTime += 5;
        }

        // Explosion préventive si presque mort
        if (isHealthBelow(0.15) && !hasExploded) {
            if (zombieType == ZombieType.BLOATER) {
                bloaterExplosion();
            } else {
                kamikazeExplosion();
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Explosion à la mort si pas encore explosé
        if (!hasExploded) {
            switch (zombieType) {
                case BLOATER -> bloaterExplosion();
                case CREEPER -> creeperExplosion();
                default -> kamikazeExplosion();
            }
        }
    }
}
