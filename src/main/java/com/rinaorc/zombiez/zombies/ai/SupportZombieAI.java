package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * IA pour les zombies SUPPORT
 * Comportement: Buff les alliés, appel de renforts, débuff les joueurs
 * Types: SCREAMER
 */
public class SupportZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int screamCharges = 3;
    private long lastScream = 0;
    private boolean isScreaming = false;

    // Paramètres
    private static final long SCREAM_COOLDOWN = 8000;
    private static final double SUPPORT_RANGE = 15.0;

    public SupportZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 5000;
    }

    @Override
    public void tick() {
        tickCounter++;

        switch (zombieType) {
            case SCREAMER -> tickScreamer();
            default -> tickScreamer();
        }
    }

    /**
     * Screamer: Crie pour alerter les zombies, buff les alliés, débuff les joueurs
     */
    private void tickScreamer() {
        // Effet visuel de bouche béante
        if (tickCounter % 40 == 0) {
            playParticles(Particle.SONIC_BOOM, zombie.getEyeLocation().add(zombie.getLocation().getDirection().multiply(0.5)),
                1, 0, 0, 0);
        }

        Player target = findNearestPlayer(20);
        if (target == null) return;

        // Hurler si il y a des joueurs et des zombies à buff
        if (canScream()) {
            List<Zombie> nearbyZombies = getNearbyZombies(SUPPORT_RANGE);

            if (!nearbyZombies.isEmpty() || target.getLocation().distance(zombie.getLocation()) < 10) {
                scream(target, nearbyZombies);
            }
        }

        // Maintenir la distance - le Screamer fuit le combat rapproché
        double distance = zombie.getLocation().distance(target.getLocation());
        if (distance < 5 && !isScreaming) {
            retreatAndScream(target);
        }
    }

    /**
     * Vérifie si peut crier
     */
    private boolean canScream() {
        return screamCharges > 0 && System.currentTimeMillis() - lastScream >= SCREAM_COOLDOWN && !isScreaming;
    }

    /**
     * Obtient les zombies alliés proches
     */
    private List<Zombie> getNearbyZombies(double range) {
        return zombie.getWorld().getNearbyEntities(zombie.getLocation(), range, range, range).stream()
            .filter(e -> e instanceof Zombie && e != zombie)
            .map(e -> (Zombie) e)
            .toList();
    }

    /**
     * Le cri du Screamer
     */
    private void scream(Player primaryTarget, List<Zombie> allies) {
        isScreaming = true;
        lastScream = System.currentTimeMillis();
        screamCharges--;

        // Animation de préparation
        zombie.setTarget(null);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10, false, false));

        playSound(Sound.ENTITY_RAVAGER_ROAR, 0.5f, 2f);

        // Le cri après un court délai
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) return;

            // SON PRINCIPAL
            playSound(Sound.ENTITY_GHAST_SCREAM, 2f, 0.8f);
            playSound(Sound.ENTITY_WARDEN_ROAR, 1f, 1.5f);

            // Onde de choc visuelle
            for (int ring = 0; ring < 5; ring++) {
                final int r = ring;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!zombie.isValid()) return;
                    double radius = 3 + r * 3;
                    for (int angle = 0; angle < 360; angle += 30) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * radius;
                        double z = Math.sin(rad) * radius;
                        playParticles(Particle.SONIC_BOOM, zombie.getLocation().add(x, 1, z), 1, 0, 0, 0);
                    }
                }, r * 2L);
            }

            // EFFET 1: Débuff aux joueurs proches
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 12, 8, 12).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    double distance = p.getLocation().distance(zombie.getLocation());
                    int amplifier = distance < 6 ? 1 : 0;

                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, amplifier));

                    if (distance < 6) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        p.damage(3, zombie);
                    }

                    // Message d'alerte
                    p.sendMessage("§c§l☠ Le cri vous paralyse!");
                });

            // EFFET 2: Buff aux zombies alliés
            for (Zombie ally : allies) {
                if (!ally.isValid()) continue;

                // Vitesse et force
                ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, true));
                ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, true));

                // Petit soin
                var maxHealth = ally.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    ally.setHealth(Math.min(ally.getHealth() + 5, maxHealth.getValue()));
                }

                // Attirer vers la cible
                ally.setTarget(primaryTarget);

                // Effet visuel sur l'allié
                ally.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, ally.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2);
            }

            // EFFET 3: Appeler des renforts (petite chance)
            if (random.nextFloat() < 0.3f + level * 0.02f) {
                callReinforcements(primaryTarget.getLocation());
            }

            isScreaming = false;
        }, 20L);
    }

    /**
     * Recule et crie
     */
    private void retreatAndScream(Player threat) {
        // Sauter en arrière
        Vector retreatDir = zombie.getLocation().toVector()
            .subtract(threat.getLocation().toVector()).normalize();
        zombie.setVelocity(retreatDir.multiply(0.8).setY(0.4));

        playSound(Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);

        // Crier pendant la retraite
        if (canScream()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid()) {
                    scream(threat, getNearbyZombies(SUPPORT_RANGE));
                }
            }, 10L);
        }
    }

    /**
     * Appelle des renforts
     */
    private void callReinforcements(Location targetLoc) {
        int reinforcements = 2 + random.nextInt(3);

        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);

        for (int i = 0; i < reinforcements; i++) {
            final int delay = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;

                // Position autour du Screamer
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 3 + random.nextDouble() * 5;
                Location spawnLoc = zombie.getLocation().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
                );

                // Trouver le sol
                spawnLoc = findGroundLocation(spawnLoc);
                if (spawnLoc == null) return;

                // Effet de spawn
                playParticles(Particle.SMOKE, spawnLoc, 20, 0.3, 0.5, 0.3);
                playSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 0.8f);

                // Spawner un zombie basique via le plugin
                // Note: On utilise le ZombieManager du plugin
                var zombieManager = plugin.getZombieManager();
                if (zombieManager != null) {
                    zombieManager.spawnZombie(ZombieType.WALKER, spawnLoc, Math.max(1, level - 2));
                }
            }, delay * 10L);
        }
    }

    /**
     * Trouve le sol à une position
     */
    private Location findGroundLocation(Location loc) {
        for (int y = (int) loc.getY(); y > loc.getWorld().getMinHeight(); y--) {
            Location checkLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
            if (checkLoc.getBlock().getType().isSolid()) {
                return checkLoc.add(0, 1, 0);
            }
        }
        return null;
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Le Screamer ne fait pas beaucoup de dégâts mais applique des débuffs
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));

        // Tenter de fuir après l'attaque
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid() && target.isOnline()) {
                retreatAndScream(target);
            }
        }, 5L);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Crier de panique quand blessé
        if (attacker instanceof Player player && !isScreaming && random.nextFloat() < 0.4f) {
            if (canScream()) {
                scream(player, getNearbyZombies(SUPPORT_RANGE));
            }
        }

        // Dernière chance - cri de mort
        if (isHealthBelow(0.2) && screamCharges > 0 && !isScreaming) {
            screamCharges = 1; // Forcer un dernier cri
            if (attacker instanceof Player player) {
                scream(player, getNearbyZombies(SUPPORT_RANGE * 1.5));
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Dernier cri à la mort
        playSound(Sound.ENTITY_GHAST_DEATH, 1.5f, 1.2f);
        playParticles(Particle.SONIC_BOOM, zombie.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5);

        // Débuff léger aux joueurs proches
        applyAreaEffect(8, PotionEffectType.SLOWNESS, 40, 0);
    }
}
