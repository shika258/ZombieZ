package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
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
 * IA pour les zombies TANK
 * Comportement: Résistant, lent, attaques puissantes
 * Types: ARMORED, GIANT, COLOSSUS
 */
public class TankZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int consecutiveHits = 0;
    private boolean isCharging = false;
    private long lastChargeTime = 0;

    public TankZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 10000; // 10 secondes

        // Appliquer résistance permanente
        applyTankBuffs();
    }

    private void applyTankBuffs() {
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        // Les tanks ont plus de knockback resistance
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.8);
        }
    }

    @Override
    public void tick() {
        tickCounter++;

        switch (zombieType) {
            case ARMORED, ARMORED_ELITE -> tickArmored();
            case GIANT -> tickGiant();
            case COLOSSUS -> tickColossus();
            default -> tickArmored();
        }
    }

    /**
     * Armored: Très résistant, peut activer un bouclier temporaire
     */
    private void tickArmored() {
        // Effet visuel d'armure
        if (tickCounter % 40 == 0) {
            playParticles(Particle.SCRAPE, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3);
        }

        // Activer le bouclier quand blessé
        if (canUseAbility() && isHealthBelow(0.5)) {
            activateShield();
            useAbility();
        }
    }

    /**
     * Giant: Énorme, secoue le sol, attaque de zone
     */
    private void tickGiant() {
        // Effet de tremblement quand il marche
        if (tickCounter % 30 == 0) {
            groundPound(false);
        }

        // Charge vers la cible
        if (canUseAbility() && !isCharging) {
            Player target = findNearestPlayer(15);
            if (target != null && zombie.getLocation().distance(target.getLocation()) > 5) {
                startCharge(target);
                useAbility();
            }
        }
    }

    /**
     * Colossus: Immense, très lent, dégâts massifs
     */
    private void tickColossus() {
        // Le colosse fait trembler le sol en permanence
        if (tickCounter % 20 == 0) {
            playSound(Sound.ENTITY_IRON_GOLEM_STEP, 0.5f, 0.4f);
            playParticles(Particle.BLOCK, zombie.getLocation(), 15, 1, 0.1, 1);
        }

        // Attaque de zone massive
        if (canUseAbility()) {
            Player target = findNearestPlayer(8);
            if (target != null) {
                massiveStomp();
                useAbility();
            }
        }

        // Rage quand en dessous de 30%
        if (isHealthBelow(0.3) && !isEnraged) {
            enrage();
            // Le colosse gagne de la vitesse quand enragé
            zombie.removePotionEffect(PotionEffectType.SLOWNESS);
            playSound(Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);
        }
    }

    /**
     * Active un bouclier de dégâts réduits
     */
    private void activateShield() {
        playSound(Sound.ITEM_ARMOR_EQUIP_IRON, 1.5f, 0.8f);
        playParticles(Particle.ENCHANTED_HIT, zombie.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5);

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2, false, false));

        // Effet visuel pendant la durée
        for (int i = 0; i < 5; i++) {
            final int tick = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid()) {
                    playParticles(Particle.END_ROD, zombie.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);
                }
            }, i * 20L);
        }
    }

    /**
     * Frappe le sol
     */
    private void groundPound(boolean strong) {
        double radius = strong ? 6 : 3;
        double damage = strong ? 6 : 2;

        playSound(Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.6f);
        playParticles(Particle.BLOCK, zombie.getLocation(), 30, radius / 2, 0.1, radius / 2);

        // Dégâts et knockback aux joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), radius, 2, radius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(damage, zombie);
                Vector knockback = p.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(strong ? 1.5 : 0.8).setY(0.4);
                p.setVelocity(knockback);
            });
    }

    /**
     * Commence une charge vers la cible
     */
    private void startCharge(Player target) {
        isCharging = true;
        lastChargeTime = System.currentTimeMillis();

        playSound(Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.6f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3, false, false));

        // Direction de charge
        Vector chargeDir = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        // Scheduler pour la charge
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || System.currentTimeMillis() - lastChargeTime > 3000) {
                isCharging = false;
                task.cancel();
                return;
            }

            zombie.setVelocity(chargeDir.clone().multiply(0.8).setY(0));
            playParticles(Particle.CLOUD, zombie.getLocation(), 5, 0.3, 0.1, 0.3);

            // Dégâts aux joueurs sur le chemin
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.damage(8 + level, zombie);
                    p.setVelocity(chargeDir.clone().multiply(1.2).setY(0.5));
                    playSound(Sound.ENTITY_PLAYER_HURT, 1f, 1f);
                    isCharging = false;
                    task.cancel();
                });
        }, 0L, 2L);
    }

    /**
     * Stomp massif du Colossus
     */
    private void massiveStomp() {
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        playSound(Sound.ENTITY_RAVAGER_STUNNED, 1f, 0.5f);

        // Onde de choc visuelle
        for (int ring = 0; ring < 5; ring++) {
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;
                double radius = 2 + r * 2;
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    playParticles(Particle.EXPLOSION, zombie.getLocation().add(x, 0.1, z), 1, 0, 0, 0);
                }
            }, r * 2L);
        }

        // Dégâts en zone
        groundPound(true);

        // Slowness aux joueurs touchés
        applyAreaEffect(8, PotionEffectType.SLOWNESS, 60, 2);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;
        consecutiveHits++;

        // Les tanks font des dégâts bonus après plusieurs hits
        if (consecutiveHits >= 3) {
            playSound(Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.5f);
            target.damage(3 + level / 2, zombie);
            consecutiveHits = 0;
        }

        // Le Giant projette les joueurs
        if (zombieType == ZombieType.GIANT) {
            Vector knockback = target.getLocation().toVector()
                .subtract(zombie.getLocation().toVector()).normalize()
                .multiply(1.5).setY(0.6);
            target.setVelocity(knockback);
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Les tanks regagnent de la vie quand ils bloquent des dégâts
        if (zombie.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            heal(damage * 0.1);
        }

        // Le Colossus devient plus dangereux quand blessé
        if (zombieType == ZombieType.COLOSSUS && isHealthBelow(0.5) && !isEnraged) {
            enrage();
        }

        // Réinitialiser les hits consécutifs
        if (attacker instanceof Player) {
            consecutiveHits = 0;
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Le Giant et le Colossus font une dernière onde de choc
        if (zombieType == ZombieType.GIANT || zombieType == ZombieType.COLOSSUS) {
            groundPound(true);
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.3f);
        }
    }
}
