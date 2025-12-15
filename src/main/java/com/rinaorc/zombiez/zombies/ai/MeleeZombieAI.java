package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies MELEE
 * Comportement: Agressif au corps à corps, combos, charges
 * Types: BERSERKER, CLIMBER, RAVAGER
 */
public class MeleeZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int comboCount = 0;
    private long lastComboTime = 0;
    private boolean isCharging = false;
    private boolean isFrenzied = false;

    // Paramètres de combat
    private static final long COMBO_WINDOW = 3000; // 3 secondes pour combo
    private static final int MAX_COMBO = 5;

    public MeleeZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 6000;
    }

    @Override
    public void tick() {
        tickCounter++;

        // Reset combo si trop de temps s'est écoulé
        if (System.currentTimeMillis() - lastComboTime > COMBO_WINDOW) {
            comboCount = 0;
        }

        switch (zombieType) {
            case BERSERKER -> tickBerserker();
            case CLIMBER -> tickClimber();
            case RAVAGER -> tickRavager();
            default -> tickBerserker();
        }
    }

    /**
     * Berserker: Plus il attaque, plus il devient fort. Frénésie quand blessé.
     */
    private void tickBerserker() {
        // Effet de rage permanent
        if (tickCounter % 30 == 0) {
            playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);
        }

        // Activer la frénésie si blessé
        if (!isFrenzied && isHealthBelow(0.4)) {
            activateFrenzy();
        }

        // Charge vers la cible
        Player target = findNearestPlayer(15);
        if (target != null && canUseAbility()) {
            double distance = zombie.getLocation().distance(target.getLocation());
            if (distance > 5 && distance < 12) {
                berserkerCharge(target);
                useAbility();
            }
        }
    }

    /**
     * Climber: Grimpe aux murs, saute sur les joueurs depuis les hauteurs
     */
    private void tickClimber() {
        // Effet d'araignée
        if (tickCounter % 20 == 0) {
            playParticles(Particle.BLOCK, zombie.getLocation(), 2, 0.3, 0.1, 0.3);
        }

        // Capacité de grimper (simulé par des sauts)
        Player target = findNearestPlayer(20);
        if (target != null) {
            double heightDiff = target.getLocation().getY() - zombie.getLocation().getY();

            // Si la cible est en hauteur, tenter de grimper
            if (heightDiff > 2 && canUseAbility()) {
                climbJump(target);
                useAbility();
            }
            // Si le zombie est en hauteur, plonger sur la cible
            else if (heightDiff < -3 && zombie.getLocation().distance(target.getLocation()) < 8) {
                diveBomb(target);
            }
        }
    }

    /**
     * Ravager: Dégâts massifs, détruit tout sur son passage
     */
    private void tickRavager() {
        // Son de grognement
        if (tickCounter % 60 == 0) {
            playSound(Sound.ENTITY_RAVAGER_AMBIENT, 0.5f, 0.8f);
        }

        Player target = findNearestPlayer(18);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Charge dévastatrice
        if (canUseAbility() && distance > 6 && distance < 15 && !isCharging) {
            devastatingCharge(target);
            useAbility();
        }

        // Attaque de zone au corps à corps
        if (distance < 3 && tickCounter % 40 == 0) {
            sweepAttack();
        }
    }

    /**
     * Active la frénésie du Berserker
     */
    private void activateFrenzy() {
        isFrenzied = true;

        playSound(Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.2f);
        playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5);

        // Boost massif
        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.5);
        }

        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.8);
        }

        // Effets visuels continus
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || zombie.isDead()) {
                task.cancel();
                return;
            }
            playParticles(Particle.FLAME, zombie.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);
        }, 0L, 10L);

        // Message aux joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 15, 10, 15).stream()
            .filter(e -> e instanceof Player)
            .forEach(e -> ((Player) e).sendMessage("§c§l⚠ Le Berserker entre en FRÉNÉSIE!"));
    }

    /**
     * Charge du Berserker
     */
    private void berserkerCharge(Player target) {
        isCharging = true;

        playSound(Sound.ENTITY_RAVAGER_ROAR, 1f, 1.5f);

        // Direction et vitesse
        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 3, false, false));

        // Charge avec particules
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || zombie.isOnGround() && tickCounter % 20 > 15) {
                isCharging = false;
                task.cancel();
                return;
            }

            zombie.setVelocity(direction.clone().multiply(1.2).setY(0.1));
            playParticles(Particle.CRIT, zombie.getLocation(), 5, 0.3, 0.1, 0.3);

            // Dégâts aux joueurs sur le chemin
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 1.5, 1.5, 1.5).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    double damage = 8 + level + comboCount * 2;
                    p.damage(damage, zombie);
                    p.setVelocity(direction.clone().multiply(1.5).setY(0.5));
                    isCharging = false;
                    task.cancel();
                });
        }, 0L, 2L);
    }

    /**
     * Saut de grimpe du Climber
     */
    private void climbJump(Player target) {
        playSound(Sound.ENTITY_SPIDER_AMBIENT, 1f, 1f);

        // Saut puissant vers le haut et vers la cible
        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();
        direction.setY(1.2);
        zombie.setVelocity(direction.multiply(0.8));

        playParticles(Particle.BLOCK, zombie.getLocation(), 15, 0.3, 0.1, 0.3);
    }

    /**
     * Plongeon sur la cible (Climber)
     */
    private void diveBomb(Player target) {
        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        zombie.setVelocity(direction.multiply(1.5));
        playSound(Sound.ENTITY_PHANTOM_SWOOP, 1.5f, 0.8f);

        // Vérifier l'impact
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || zombie.isOnGround()) {
                // Impact au sol
                if (zombie.isOnGround()) {
                    impactDamage(zombie.getLocation(), 4, 10 + level);
                }
                task.cancel();
                return;
            }
            playParticles(Particle.CLOUD, zombie.getLocation(), 3, 0.2, 0.2, 0.2);
        }, 5L, 2L);
    }

    /**
     * Charge dévastatrice du Ravager
     */
    private void devastatingCharge(Player target) {
        isCharging = true;

        playSound(Sound.ENTITY_RAVAGER_ROAR, 2f, 0.7f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, false, false));

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || !isCharging) {
                task.cancel();
                return;
            }

            zombie.setVelocity(direction.clone().multiply(1.0).setY(0));
            playParticles(Particle.BLOCK, zombie.getLocation(), 10, 0.5, 0.1, 0.5);
            playSound(Sound.ENTITY_RAVAGER_STEP, 0.5f, 0.8f);

            // Dégâts et knockback massifs
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    double damage = 12 + level * 1.5;
                    p.damage(damage, zombie);
                    Vector knockback = direction.clone().multiply(2).setY(0.8);
                    p.setVelocity(knockback);
                    isCharging = false;

                    // Impact
                    impactDamage(p.getLocation(), 3, 5);
                    task.cancel();
                });

            // Timeout
            if (zombie.getLocation().distance(target.getLocation()) < 2) {
                isCharging = false;
                task.cancel();
            }
        }, 0L, 3L);
    }

    /**
     * Attaque balayante du Ravager
     */
    private void sweepAttack() {
        playSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
        playParticles(Particle.SWEEP_ATTACK, zombie.getLocation().add(0, 1, 0), 5, 1, 0.5, 1);

        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 3, 2, 3).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(6 + level, zombie);
                Vector knockback = p.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(1.2).setY(0.4);
                p.setVelocity(knockback);
            });
    }

    /**
     * Dégâts d'impact
     */
    private void impactDamage(Location loc, double radius, double damage) {
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        playParticles(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5);

        loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> p.damage(damage, zombie));
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Système de combo
        comboCount = Math.min(MAX_COMBO, comboCount + 1);
        lastComboTime = System.currentTimeMillis();

        // Dégâts bonus selon le combo
        if (comboCount >= 3) {
            double bonusDamage = comboCount * 1.5;
            target.damage(bonusDamage, zombie);

            if (comboCount == MAX_COMBO) {
                // Attaque finale de combo
                playSound(Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
                playParticles(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                comboCount = 0;
            }
        }

        // Effet visuel de combo
        if (comboCount > 1) {
            target.sendMessage("§c✦ Combo x" + comboCount + "!");
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Le Berserker contre-attaque quand touché
        if (zombieType == ZombieType.BERSERKER && attacker instanceof Player player) {
            if (random.nextFloat() < 0.3f + (isFrenzied ? 0.2f : 0)) {
                // Riposte
                player.damage(3 + level * 0.5, zombie);
                playSound(Sound.ENTITY_PLAYER_HURT, 1f, 1f);
                playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
            }
        }

        // Le Ravager devient plus agressif quand blessé
        if (zombieType == ZombieType.RAVAGER && isHealthBelow(0.5)) {
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Le Berserker explose de rage
        if (zombieType == ZombieType.BERSERKER && isFrenzied) {
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            impactDamage(zombie.getLocation(), 4, 8);
        }

        // Le Ravager fait trembler le sol
        if (zombieType == ZombieType.RAVAGER) {
            playSound(Sound.ENTITY_RAVAGER_HURT, 2f, 0.5f);
            playParticles(Particle.BLOCK, zombie.getLocation(), 50, 2, 0.5, 2);
        }
    }
}
