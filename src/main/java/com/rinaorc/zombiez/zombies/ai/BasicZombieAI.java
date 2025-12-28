package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

/**
 * IA basique pour les zombies BASIC
 * Comportement simple: marcher vers la cible et attaquer
 * Variations selon le type spécifique
 */
public class BasicZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isGroaning = false;

    public BasicZombieAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 8000; // 8 secondes
    }

    @Override
    public void tick() {
        tickCounter++;

        // Comportement selon le type spécifique
        switch (zombieType) {
            case WALKER -> tickWalker();
            case CRAWLER -> tickCrawler();
            case RUNNER -> tickRunner();
            case SHAMBLER -> tickShambler();
            case MUMMY -> tickMummy();
            case DROWNER -> tickDrowner();
            default -> tickWalker();
        }
    }

    /**
     * Walker: Zombie classique, parfois grogne pour alerter d'autres zombies
     */
    private void tickWalker() {
        if (tickCounter % 100 == 0 && random.nextFloat() < 0.3f) {
            groan();
        }
    }

    /**
     * Crawler: Rampe au sol, plus dur à toucher, peut bondir
     */
    private void tickCrawler() {
        // Effet visuel de rampement
        if (tickCounter % 20 == 0) {
            playBlockParticles(zombie.getLocation(), 3, 0.3, 0, 0.3);
        }

        // Bondir vers la cible si proche
        if (canUseAbility()) {
            Player target = findNearestPlayer(8);
            if (target != null && zombie.getLocation().distance(target.getLocation()) < 6) {
                leapTowards(target.getLocation(), 0.8);
                useAbility();
                playSound(Sound.ENTITY_SPIDER_AMBIENT, 1f, 1.2f);
            }
        }
    }

    /**
     * Runner: Très rapide, sprint vers la cible
     */
    private void tickRunner() {
        Player target = findNearestPlayer(20);
        if (target != null && tickCounter % 40 == 0) {
            // Sprint boost temporaire
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 60, 1, false, false));
            playParticles(Particle.CLOUD, zombie.getLocation(), 5, 0.2, 0.1, 0.2);
        }
    }

    /**
     * Shambler: Très lent mais tanky, parfois s'arrête
     */
    private void tickShambler() {
        // S'arrête parfois de bouger
        if (tickCounter % 60 == 0 && random.nextFloat() < 0.2f) {
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 10, false, false));
        }

        // Résistance naturelle
        if (tickCounter % 100 == 0) {
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.RESISTANCE, 100, 0, false, false));
        }
    }

    /**
     * Mummy: Immunisé au feu, lance des bandages qui ralentissent
     */
    private void tickMummy() {
        // Immunité au feu
        zombie.setFireTicks(0);

        // Lancer des bandages
        if (canUseAbility()) {
            Player target = findNearestPlayer(12);
            if (target != null) {
                throwBandage(target);
                useAbility();
            }
        }
    }

    /**
     * Drowner: Nage rapidement, attire les joueurs dans l'eau
     */
    private void tickDrowner() {
        // Bonus de vitesse dans l'eau
        if (zombie.isInWater()) {
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false));
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 40, 2, false, false));
        }

        // Effet d'eau dégoulinante
        if (tickCounter % 10 == 0) {
            playParticles(Particle.DRIPPING_WATER, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3);
        }
    }

    /**
     * Grognement qui alerte les zombies proches
     */
    private void groan() {
        if (isGroaning)
            return;
        isGroaning = true;

        playSound(Sound.ENTITY_ZOMBIE_AMBIENT, 2f, 0.8f);
        playParticles(Particle.SONIC_BOOM, zombie.getLocation().add(0, 1, 0), 1, 0, 0, 0);

        // Attirer les zombies proches vers la même cible
        if (currentTarget != null) {
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 15, 10, 15).stream()
                    .filter(e -> e instanceof Zombie && e != zombie)
                    .map(e -> (Zombie) e)
                    .limit(3)
                    .forEach(z -> z.setTarget(currentTarget));
        }

        // Reset après un délai
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isGroaning = false, 100);
    }

    /**
     * Lance une bandage ralentissante (Mummy)
     */
    private void throwBandage(Player target) {
        playSound(Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.8f);
        playParticles(Particle.ITEM_SNOWBALL, zombie.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2);

        // Appliquer slowness
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
    }

    @Override
    public void onAttack(Player target) {
        this.currentTarget = target;

        switch (zombieType) {
            case CRAWLER -> {
                // Chance de faire tomber la cible
                if (random.nextFloat() < 0.2f) {
                    target.setVelocity(target.getVelocity().setY(-0.5));
                }
            }
            case RUNNER -> {
                // Attaque rapide mais faible
                playParticles(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3);
            }
            case DROWNER -> {
                // Chance d'appliquer slowness (comme si le joueur était mouillé)
                if (random.nextFloat() < 0.3f) {
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0));
                }
            }
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Runner fuit quand blessé
        if (zombieType == ZombieType.RUNNER && isHealthBelow(0.3)) {
            if (attacker instanceof Player player) {
                // Fuir dans la direction opposée
                org.bukkit.util.Vector fleeDirection = zombie.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                zombie.setVelocity(fleeDirection.multiply(0.8).setY(0.3));
            }
        }

        // Shambler devient résistant quand blessé
        if (zombieType == ZombieType.SHAMBLER && isHealthBelow(0.5)) {
            zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.RESISTANCE, 60, 1));
        }
    }
}
