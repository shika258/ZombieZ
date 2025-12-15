package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies SKELETON
 * Comportement: Reste à distance, tire des flèches avec précision
 * Types: SKELETON, STRAY
 *
 * SKELETON: Squelette classique avec arc, tire des flèches normales
 * STRAY: Variante glacée, tire des flèches de ralentissement
 */
public class SkeletonZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isRetreating = false;
    private int arrowsFired = 0;
    private long lastStrafeTime = 0;
    private boolean strafeRight = true;

    // Distance préférée pour tirer
    private static final double PREFERRED_DISTANCE = 15.0;
    private static final double TOO_CLOSE = 8.0;
    private static final double MAX_RANGE = 25.0;

    public SkeletonZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = zombieType == ZombieType.STRAY ? 2500 : 2000; // Stray tire un peu plus lentement
    }

    @Override
    public void tick() {
        tickCounter++;

        Player target = findNearestPlayer(MAX_RANGE);
        if (target == null) {
            // Pas de cible, rester en veille
            if (tickCounter % 40 == 0) {
                playIdleEffects();
            }
            return;
        }

        double distance = zombie.getLocation().distance(target.getLocation());

        // Comportement de positionnement
        if (distance < TOO_CLOSE && !isRetreating) {
            // Trop proche, reculer
            retreat(target);
        } else if (distance > PREFERRED_DISTANCE * 1.3) {
            // Trop loin, se rapprocher
            zombie.setTarget(target);
        } else {
            // Distance idéale, strafer et tirer
            strafe(target);

            if (canUseAbility() && hasLineOfSight(target)) {
                fireArrow(target);
                useAbility();
            }
        }

        // Effets visuels spécifiques au type
        if (tickCounter % 20 == 0) {
            playTypeEffects();
        }
    }

    /**
     * Effets en attente (pas de cible)
     */
    private void playIdleEffects() {
        switch (zombieType) {
            case SKELETON -> {
                playSound(Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 1f);
            }
            case STRAY -> {
                playSound(Sound.ENTITY_STRAY_AMBIENT, 0.5f, 1f);
                playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);
            }
        }
    }

    /**
     * Effets visuels selon le type
     */
    private void playTypeEffects() {
        switch (zombieType) {
            case SKELETON -> {
                // Particules d'os occasionnelles
                if (random.nextFloat() < 0.3f) {
                    playParticles(Particle.BLOCK, zombie.getLocation().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2);
                }
            }
            case STRAY -> {
                // Aura de froid
                playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(0, 1, 0), 5, 0.4, 0.5, 0.4);
                playParticles(Particle.CLOUD, zombie.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);

                // Ralentir légèrement les joueurs très proches
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 3, 2, 3).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false)));
            }
        }
    }

    /**
     * Vérifie si le squelette a une ligne de vue sur la cible
     */
    private boolean hasLineOfSight(Player target) {
        return zombie.hasLineOfSight(target);
    }

    /**
     * Recule pour maintenir la distance
     */
    private void retreat(Player target) {
        isRetreating = true;

        Vector retreatDir = zombie.getLocation().toVector()
            .subtract(target.getLocation().toVector()).normalize();

        // Saut arrière avec variation latérale
        double lateralOffset = (random.nextDouble() - 0.5) * 0.5;
        Vector lateral = new Vector(-retreatDir.getZ(), 0, retreatDir.getX()).multiply(lateralOffset);

        zombie.setVelocity(retreatDir.add(lateral).multiply(0.7).setY(0.25));

        switch (zombieType) {
            case SKELETON -> playSound(Sound.ENTITY_SKELETON_STEP, 1f, 1.2f);
            case STRAY -> playSound(Sound.ENTITY_STRAY_STEP, 1f, 1.2f);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isRetreating = false, 30);
    }

    /**
     * Se déplace latéralement pour éviter les attaques
     */
    private void strafe(Player target) {
        if (System.currentTimeMillis() - lastStrafeTime < 1500) return;

        lastStrafeTime = System.currentTimeMillis();

        // Changer de direction de strafe aléatoirement
        if (random.nextFloat() < 0.3f) {
            strafeRight = !strafeRight;
        }

        Vector toTarget = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        // Vecteur perpendiculaire pour le strafe
        Vector strafeDir = new Vector(-toTarget.getZ(), 0, toTarget.getX());
        if (!strafeRight) strafeDir.multiply(-1);

        zombie.setVelocity(strafeDir.multiply(0.4).setY(0));
    }

    /**
     * Tire une flèche vers la cible
     */
    private void fireArrow(Player target) {
        Location eyeLoc = zombie.getEyeLocation();

        // Prédiction de la position de la cible
        Vector targetVelocity = target.getVelocity();
        double distance = eyeLoc.distance(target.getEyeLocation());
        double travelTime = distance / 2.5; // Vitesse approximative de la flèche

        Location predictedLoc = target.getEyeLocation().add(
            targetVelocity.getX() * travelTime * 0.5,
            targetVelocity.getY() * travelTime * 0.3,
            targetVelocity.getZ() * travelTime * 0.5
        );

        Vector direction = predictedLoc.toVector()
            .subtract(eyeLoc.toVector()).normalize();

        // Ajouter de l'imprécision basée sur la distance et le niveau
        double accuracy = Math.max(0.02, 0.12 - (level * 0.002));
        direction.add(new Vector(
            (random.nextDouble() - 0.5) * accuracy,
            (random.nextDouble() - 0.5) * accuracy * 0.5,
            (random.nextDouble() - 0.5) * accuracy
        ));

        // Son de tir
        switch (zombieType) {
            case SKELETON -> playSound(Sound.ENTITY_SKELETON_SHOOT, 1f, 1f);
            case STRAY -> playSound(Sound.ENTITY_STRAY_SHOOT, 1f, 0.9f);
        }

        // Créer la flèche
        Arrow arrow = zombie.getWorld().spawnArrow(
            eyeLoc.add(direction.clone().multiply(0.5)),
            direction,
            (float) (1.8 + level * 0.05), // Vitesse basée sur le niveau
            0
        );

        arrow.setShooter(zombie);
        arrow.setDamage(zombieType.getBaseDamage() * (1 + level * 0.1));
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        // Effets spéciaux pour Stray
        if (zombieType == ZombieType.STRAY) {
            applyStrayArrowEffects(arrow);
        }

        // Particules de tir
        playParticles(Particle.CRIT, eyeLoc, 5, 0.1, 0.1, 0.1);

        arrowsFired++;

        // Tir en rafale tous les 5 tirs
        if (arrowsFired >= 5 && random.nextFloat() < 0.4f) {
            burstFire(target);
            arrowsFired = 0;
        }
    }

    /**
     * Applique les effets de flèche gelée du Stray
     */
    private void applyStrayArrowEffects(Arrow arrow) {
        // Particules de glace sur la flèche
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                task.cancel();
                return;
            }
            playParticles(Particle.SNOWFLAKE, arrow.getLocation(), 2, 0.05, 0.05, 0.05);
        }, 0L, 2L);

        // Handler d'impact pour appliquer le ralentissement
        arrow.addScoreboardTag("stray_arrow_" + level);
    }

    /**
     * Rafale de 2 flèches rapides
     */
    private void burstFire(Player target) {
        for (int i = 1; i <= 2; i++) {
            final int delay = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid() && target.isOnline() && hasLineOfSight(target)) {
                    fireArrow(target);
                }
            }, delay * 5L);
        }
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Attaque au corps à corps (peu de dégâts, le squelette préfère fuir)
        switch (zombieType) {
            case SKELETON -> {
                playSound(Sound.ENTITY_SKELETON_HURT, 1f, 1.5f);
                // Tente de repousser et fuir
                target.setVelocity(target.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(0.5).setY(0.2));
            }
            case STRAY -> {
                playSound(Sound.ENTITY_STRAY_HURT, 1f, 1.5f);
                // Gel au contact
                target.setFreezeTicks(Math.min(target.getFreezeTicks() + 80, target.getMaxFreezeTicks()));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                playParticles(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3);
            }
        }

        // Toujours essayer de fuir après une attaque au corps à corps
        retreat(target);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Fuir quand blessé
        if (attacker instanceof Player player) {
            // Plus de chance de fuir si blessé gravement
            double fleeChance = isHealthBelow(0.4) ? 0.8f : 0.4f;
            if (random.nextFloat() < fleeChance) {
                retreat(player);
            }
        }

        // Animation de douleur
        switch (zombieType) {
            case SKELETON -> playSound(Sound.ENTITY_SKELETON_HURT, 1f, 1f);
            case STRAY -> {
                playSound(Sound.ENTITY_STRAY_HURT, 1f, 1f);
                // Explosion de particules de glace
                playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
            }
        }

        // Si critique, tenter une dernière salve
        if (isHealthBelow(0.2) && attacker instanceof Player player) {
            if (canUseAbility() && hasLineOfSight(player)) {
                burstFire(player);
                useAbility();
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Effets de mort
        switch (zombieType) {
            case SKELETON -> {
                playSound(Sound.ENTITY_SKELETON_DEATH, 1f, 1f);
                playParticles(Particle.BLOCK, zombie.getLocation().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5);

                // Chance de dropper des os (géré par le loot system)
            }
            case STRAY -> {
                playSound(Sound.ENTITY_STRAY_DEATH, 1f, 1f);
                playParticles(Particle.SNOWFLAKE, zombie.getLocation(), 50, 1, 1, 1);
                playParticles(Particle.CLOUD, zombie.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);

                // Explosion de froid à la mort
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 4, 2, 4).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                        p.setFreezeTicks(Math.min(p.getFreezeTicks() + 60, p.getMaxFreezeTicks()));
                    });
            }
        }
    }
}
