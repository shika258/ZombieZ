package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Classe de base pour l'IA des zombies personnalisés
 * Chaque catégorie de zombie a son propre comportement
 */
@Getter
public abstract class ZombieAI {

    protected final ZombieZPlugin plugin;
    protected final Zombie zombie;
    protected final ZombieType zombieType;
    protected final int level;
    protected final Random random = new Random();

    // Cooldowns pour les abilities
    protected long lastAbilityUse = 0;
    protected long abilityCooldown = 5000; // 5 secondes par défaut

    // État de l'IA
    protected boolean isEnraged = false;
    protected Player currentTarget = null;

    public ZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        this.plugin = plugin;
        this.zombie = zombie;
        this.zombieType = zombieType;
        this.level = level;
    }

    /**
     * Met à jour l'IA (appelé régulièrement)
     */
    public abstract void tick();

    /**
     * Appelé quand le zombie attaque
     */
    public abstract void onAttack(Player target);

    /**
     * Appelé quand le zombie prend des dégâts
     */
    public abstract void onDamaged(Entity attacker, double damage);

    /**
     * Appelé quand le zombie meurt
     */
    public void onDeath(Player killer) {
        // Comportement par défaut - peut être override
    }

    /**
     * Vérifie si l'ability est prête
     */
    protected boolean canUseAbility() {
        return System.currentTimeMillis() - lastAbilityUse >= abilityCooldown;
    }

    /**
     * Utilise l'ability et met le cooldown
     */
    protected void useAbility() {
        lastAbilityUse = System.currentTimeMillis();
    }

    /**
     * Trouve le joueur le plus proche
     */
    protected Player findNearestPlayer(double range) {
        return zombie.getWorld().getNearbyEntities(zombie.getLocation(), range, range, range).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> !p.isDead() && p.getGameMode() == org.bukkit.GameMode.SURVIVAL)
                .min((a, b) -> (int) (a.getLocation().distance(zombie.getLocation())
                        - b.getLocation().distance(zombie.getLocation())))
                .orElse(null);
    }

    /**
     * Fait sauter le zombie vers une cible
     */
    protected void leapTowards(Location target, double power) {
        Vector direction = target.toVector().subtract(zombie.getLocation().toVector()).normalize();
        direction.setY(0.4);
        zombie.setVelocity(direction.multiply(power));
    }

    /**
     * Applique un effet de zone
     */
    protected void applyAreaEffect(double radius, PotionEffectType effect, int duration, int amplifier) {
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> p.addPotionEffect(new PotionEffect(effect, duration, amplifier)));
    }

    /**
     * Joue des particules
     */
    protected void playParticles(Particle particle, Location loc, int count, double offsetX, double offsetY,
            double offsetZ) {
        zombie.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ);
    }

    /**
     * Joue des particules de type BLOCK avec BlockData
     */
    protected void playParticles(Particle particle, Location loc, int count, double offsetX, double offsetY,
            double offsetZ, Material blockMaterial) {
        if (particle == Particle.BLOCK) {
            zombie.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ,
                    blockMaterial.createBlockData());
        } else {
            // Fallback pour autres particules
            playParticles(particle, loc, count, offsetX, offsetY, offsetZ);
        }
    }

    /**
     * Joue des particules de bloc avec un matériau par défaut (DIRT)
     */
    protected void playBlockParticles(Location loc, int count, double offsetX, double offsetY, double offsetZ) {
        playParticles(Particle.BLOCK, loc, count, offsetX, offsetY, offsetZ, Material.DIRT);
    }

    /**
     * Joue un son
     */
    protected void playSound(Sound sound, float volume, float pitch) {
        zombie.getWorld().playSound(zombie.getLocation(), sound, volume, pitch);
    }

    /**
     * Soigne le zombie
     */
    protected void heal(double amount) {
        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            zombie.setHealth(Math.min(zombie.getHealth() + amount, maxHealth.getValue()));
        }
    }

    /**
     * Vérifie si le zombie est en dessous d'un certain % de vie
     */
    protected boolean isHealthBelow(double percent) {
        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null)
            return false;
        return zombie.getHealth() / maxHealth.getValue() < percent;
    }

    /**
     * Active le mode enragé
     */
    protected void enrage() {
        if (isEnraged)
            return;
        isEnraged = true;

        // Boost de stats
        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.3);
        }

        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.5);
        }

        // Effet visuel
        playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3);
        playSound(Sound.ENTITY_ZOMBIE_AMBIENT, 1.5f, 0.5f);
    }
}
