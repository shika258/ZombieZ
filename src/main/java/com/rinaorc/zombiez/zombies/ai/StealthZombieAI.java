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
 * IA pour les zombies STEALTH
 * Comportement: Invisibilité, attaques surprises, très mobiles
 * Types: LURKER, SHADOW, SPECTRE
 */
public class StealthZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isInvisible = false;
    private boolean isAmbushing = false;
    private long lastAmbushTime = 0;
    private Player ambushTarget = null;

    // Paramètres de stealth
    private static final long AMBUSH_COOLDOWN = 15000; // 15 secondes
    private static final double AMBUSH_RANGE = 20.0;
    private static final double ATTACK_RANGE = 4.0;

    public StealthZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 8000;

        // Commencer invisible
        goInvisible();
    }

    @Override
    public void tick() {
        tickCounter++;

        switch (zombieType) {
            case LURKER -> tickLurker();
            case SHADOW -> tickShadow();
            case SPECTRE -> tickSpectre();
            default -> tickLurker();
        }
    }

    /**
     * Lurker: Se cache et bondit sur les joueurs
     */
    private void tickLurker() {
        Player target = findNearestPlayer(AMBUSH_RANGE);

        if (target == null) {
            // Pas de cible, rester caché
            if (!isInvisible) goInvisible();
            return;
        }

        double distance = zombie.getLocation().distance(target.getLocation());

        if (isInvisible) {
            // S'approcher silencieusement
            if (distance > ATTACK_RANGE) {
                zombie.setTarget(target);
            } else if (canAmbush()) {
                // Attaque surprise!
                ambush(target);
            }
        } else {
            // Visible - combattre normalement puis se recacher
            if (tickCounter % 100 == 0 && !isAmbushing) {
                goInvisible();
            }
        }
    }

    /**
     * Shadow: Se téléporte dans les ombres
     */
    private void tickShadow() {
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SMOKE, zombie.getLocation(), 3, 0.3, 0.5, 0.3);
        }

        Player target = findNearestPlayer(AMBUSH_RANGE);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Téléportation si trop loin ou pour esquiver
        if (canUseAbility() && (distance > 10 || (distance < 3 && random.nextFloat() < 0.3f))) {
            shadowStep(target);
            useAbility();
        }

        // Attaque dans le dos
        if (distance < ATTACK_RANGE && canAmbush()) {
            backstab(target);
        }
    }

    /**
     * Spectre: Traverse les murs, intangible
     */
    private void tickSpectre() {
        // Effet fantomatique permanent
        if (tickCounter % 5 == 0) {
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3);
        }

        // Le Spectre est partiellement intangible
        if (tickCounter % 60 == 0) {
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, false, false));
        }

        Player target = findNearestPlayer(AMBUSH_RANGE);
        if (target == null) return;

        // Drain de vie à distance
        if (canUseAbility()) {
            double distance = zombie.getLocation().distance(target.getLocation());
            if (distance < 8) {
                lifeDrain(target);
                useAbility();
            }
        }

        // Phase à travers pour attaquer
        if (canAmbush() && random.nextFloat() < 0.2f) {
            phaseAttack(target);
        }
    }

    /**
     * Passe en mode invisible
     */
    private void goInvisible() {
        isInvisible = true;
        zombie.setInvisible(true);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        zombie.setCustomNameVisible(false);

        playSound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.5f, 1.5f);
        playParticles(Particle.SMOKE, zombie.getLocation(), 20, 0.5, 1, 0.5);
    }

    /**
     * Révèle le zombie
     */
    private void reveal() {
        isInvisible = false;
        zombie.setInvisible(false);
        zombie.removePotionEffect(PotionEffectType.INVISIBILITY);
        zombie.setCustomNameVisible(true);

        playSound(Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1f, 1f);
        playParticles(Particle.SMOKE, zombie.getLocation(), 30, 0.5, 1, 0.5);
    }

    /**
     * Vérifie si peut faire une embuscade
     */
    private boolean canAmbush() {
        return System.currentTimeMillis() - lastAmbushTime >= AMBUSH_COOLDOWN;
    }

    /**
     * Attaque embuscade
     */
    private void ambush(Player target) {
        isAmbushing = true;
        lastAmbushTime = System.currentTimeMillis();
        ambushTarget = target;

        // Révéler et bondir
        reveal();
        leapTowards(target.getLocation(), 1.2);

        playSound(Sound.ENTITY_PHANTOM_SWOOP, 1.5f, 0.8f);
        playParticles(Particle.CRIT, zombie.getLocation(), 20, 0.5, 0.5, 0.5);

        // Dégâts bonus d'embuscade
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid() && target.isOnline()) {
                double distance = zombie.getLocation().distance(target.getLocation());
                if (distance < 3) {
                    double ambushDamage = 8 + level * 2;
                    target.damage(ambushDamage, zombie);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    playSound(Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
                }
            }
            isAmbushing = false;
        }, 10L);
    }

    /**
     * Téléportation Shadow
     */
    private void shadowStep(Player target) {
        // Se téléporter derrière la cible
        Vector behindTarget = target.getLocation().getDirection().multiply(-3);
        org.bukkit.Location teleportLoc = target.getLocation().add(behindTarget);
        teleportLoc.setY(target.getLocation().getY());

        // Effet de disparition
        playParticles(Particle.SMOKE, zombie.getLocation(), 30, 0.5, 1, 0.5);
        playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);

        // Téléportation
        zombie.teleport(teleportLoc);

        // Effet d'apparition
        playParticles(Particle.SMOKE, zombie.getLocation(), 30, 0.5, 1, 0.5);
        playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
    }

    /**
     * Attaque dans le dos (Shadow)
     */
    private void backstab(Player target) {
        lastAmbushTime = System.currentTimeMillis();

        // Vérifier si on est derrière la cible
        Vector toZombie = zombie.getLocation().toVector().subtract(target.getLocation().toVector());
        Vector targetFacing = target.getLocation().getDirection();
        double dot = toZombie.normalize().dot(targetFacing.normalize());

        // Si dans le dos (dot > 0 = devant, dot < 0 = derrière)
        double damage = 6 + level;
        if (dot < -0.5) {
            // Backstab! Dégâts triplés
            damage *= 3;
            playSound(Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
            playParticles(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3);
            target.sendMessage("§c§l✦ BACKSTAB! ✦");
        }

        target.damage(damage, zombie);
    }

    /**
     * Drain de vie (Spectre)
     */
    private void lifeDrain(Player target) {
        playSound(Sound.ENTITY_WITHER_SHOOT, 0.5f, 2f);

        // Particules de drain
        Vector direction = target.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize();
        for (int i = 0; i < 10; i++) {
            org.bukkit.Location particleLoc = zombie.getLocation().add(0, 1, 0).add(direction.clone().multiply(i * 0.5));
            playParticles(Particle.SOUL, particleLoc, 1, 0, 0, 0);
        }

        // Dégâts et soin
        double damage = 4 + level;
        target.damage(damage, zombie);
        heal(damage * 0.5);
    }

    /**
     * Attaque en phase (Spectre)
     */
    private void phaseAttack(Player target) {
        lastAmbushTime = System.currentTimeMillis();

        // Devenir intangible
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
        playParticles(Particle.PORTAL, zombie.getLocation(), 50, 0.5, 1, 0.5);
        playSound(Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 2f);

        // Rush vers la cible
        Vector direction = target.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize();
        zombie.setVelocity(direction.multiply(2).setY(0.3));

        // Dégâts à tout sur le passage
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || zombie.isOnGround()) {
                task.cancel();
                return;
            }
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 1.5, 1.5, 1.5).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.damage(5 + level, zombie);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                });
            playParticles(Particle.SOUL, zombie.getLocation(), 5, 0.3, 0.3, 0.3);
        }, 0L, 2L);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Bonus de dégâts si invisible
        if (isInvisible) {
            reveal();
            target.damage(4 + level, zombie); // Dégâts bonus
            playParticles(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Si invisible, révéler temporairement
        if (isInvisible) {
            reveal();
            // Se recacher après un délai
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid() && !zombie.isDead()) {
                    goInvisible();
                }
            }, 80L); // 4 secondes
        }

        // Tenter de fuir si blessé
        if (attacker instanceof Player player && isHealthBelow(0.3)) {
            if (zombieType == ZombieType.SHADOW) {
                shadowStep(player); // Téléporter loin
            } else {
                // Sauter en arrière et devenir invisible
                Vector awayDir = zombie.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                zombie.setVelocity(awayDir.multiply(1).setY(0.4));
                goInvisible();
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Les zombies stealth laissent une trace de fumée
        playParticles(Particle.SMOKE, zombie.getLocation(), 50, 1, 1, 1);
        playSound(Sound.ENTITY_PHANTOM_DEATH, 1f, 1f);

        // Le Spectre crée une dernière onde de drain
        if (zombieType == ZombieType.SPECTRE) {
            applyAreaEffect(5, PotionEffectType.WITHER, 60, 0);
            playParticles(Particle.SOUL, zombie.getLocation(), 30, 2, 1, 2);
        }
    }
}
