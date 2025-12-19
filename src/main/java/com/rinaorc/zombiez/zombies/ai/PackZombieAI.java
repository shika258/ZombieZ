package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 * IA pour les zombies PACK
 * Comportement: Meute coordonnée, attaques en groupe, bonus de proximité
 * Types: RABID_WOLF
 *
 * RABID_WOLF: Loup enragé infecté, attaque en meute
 * - Bonus de dégâts quand d'autres loups sont proches
 * - Capacité de hurlement pour alerter la meute
 * - Attaques coordonnées et encerclement
 * - Très rapide et agressif
 */
public class PackZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isHowling = false;
    private long lastHowlTime = 0;
    private int packBonus = 0;
    private boolean isLeaping = false;

    // Cooldown du hurlement (alerter la meute)
    private static final long HOWL_COOLDOWN = 15000; // 15 secondes

    public PackZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 4000; // Attaque spéciale toutes les 4 secondes
    }

    @Override
    public void tick() {
        tickCounter++;

        // Calculer le bonus de meute
        updatePackBonus();

        switch (zombieType) {
            case RABID_WOLF -> tickRabidWolf();
            default -> tickRabidWolf();
        }
    }

    /**
     * Met à jour le bonus de meute basé sur les autres loups proches
     */
    private void updatePackBonus() {
        if (tickCounter % 20 != 0)
            return; // Vérifier toutes les secondes

        List<Entity> nearbyWolves = zombie.getWorld()
                .getNearbyEntities(zombie.getLocation(), 12, 8, 12).stream()
                .filter(e -> e instanceof Zombie || e instanceof Wolf)
                .filter(e -> e.hasMetadata("zombiez_type"))
                .filter(e -> {
                    String typeName = e.getMetadata("zombiez_type").get(0).asString();
                    return typeName.equals("RABID_WOLF");
                })
                .filter(e -> !e.getUniqueId().equals(zombie.getUniqueId()))
                .collect(Collectors.toList());

        packBonus = Math.min(nearbyWolves.size(), 5); // Max 5 bonus de meute

        // Effets visuels de meute active
        if (packBonus >= 2 && tickCounter % 40 == 0) {
            playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.2, 0),
                    packBonus, 0.3, 0.2, 0.3);
        }
    }

    /**
     * Comportement du loup enragé
     */
    private void tickRabidWolf() {
        // Effets visuels de rage
        if (tickCounter % 15 == 0) {
            playParticles(Particle.SMOKE, zombie.getLocation().add(0, 0.8, 0), 2, 0.2, 0.2, 0.2);

            // Grognement occasionnel
            if (random.nextFloat() < 0.2f) {
                playSound(Sound.ENTITY_WOLF_GROWL, 0.8f, 0.8f + random.nextFloat() * 0.4f);
            }
        }

        // Bave de rage (écume)
        if (tickCounter % 30 == 0) {
            playParticles(Particle.DRIPPING_WATER, zombie.getLocation().add(0, 0.5, 0), 1, 0.1, 0, 0.1);
        }

        Player target = findNearestPlayer(25);
        if (target == null) {
            // Pas de cible - comportement de patrouille
            idleBehavior();
            return;
        }

        double distance = zombie.getLocation().distance(target.getLocation());

        // Toujours cibler agressivement
        zombie.setTarget(target);

        // Boost de vitesse permanent (les loups sont RAPIDES)
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, packBonus > 0 ? 1 : 0, false, false));

        // Hurlement pour alerter la meute
        if (distance < 20 && canHowl() && packBonus < 3) {
            howl(target);
        }

        // Capacités selon la distance
        if (distance > 8 && distance < 18 && canUseAbility() && !isLeaping) {
            // Bond vers la cible
            leapAttack(target);
            useAbility();
        } else if (distance < 4) {
            // Au corps à corps - attaque féroce
            if (tickCounter % 15 == 0) {
                savageAttack(target);
            }
        }

        // Encerclement tactique (si en meute)
        if (packBonus >= 2 && tickCounter % 60 == 0) {
            flankingManeuver(target);
        }
    }

    /**
     * Comportement en l'absence de cible
     */
    private void idleBehavior() {
        if (tickCounter % 100 == 0) {
            // Hurlement de solitude occasionnel
            if (random.nextFloat() < 0.2f) {
                playSound(Sound.ENTITY_WOLF_HOWL, 1f, 0.9f);
                playParticles(Particle.SOUL, zombie.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3);
            }
        }
    }

    /**
     * Vérifie si le loup peut hurler
     */
    private boolean canHowl() {
        return System.currentTimeMillis() - lastHowlTime >= HOWL_COOLDOWN;
    }

    /**
     * Hurlement pour alerter la meute et effrayer les joueurs
     */
    private void howl(Player target) {
        isHowling = true;
        lastHowlTime = System.currentTimeMillis();

        // Le loup lève la tête et hurle
        playSound(Sound.ENTITY_WOLF_HOWL, 2f, 0.7f);
        playParticles(Particle.SOUL, zombie.getLocation().add(0, 1.5, 0), 15, 0.5, 0.8, 0.5);

        // S'arrêter pendant le hurlement
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 10, false, false));

        // Effet de peur sur les joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 15, 8, 15).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, false));
                });

        // Alerter les autres loups de la zone (boost temporaire)
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 30, 15, 30).stream()
                .filter(e -> e instanceof Zombie)
                .filter(e -> e.hasMetadata("zombiez_type"))
                .filter(e -> e.getMetadata("zombiez_type").get(0).asString().equals("RABID_WOLF"))
                .forEach(wolf -> {
                    if (wolf instanceof LivingEntity living) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, false));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, false));
                        wolf.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, wolf.getLocation().add(0, 1, 0),
                                5, 0.3, 0.3, 0.3);

                        // Cibler le même joueur
                        if (wolf instanceof Zombie z) {
                            z.setTarget(target);
                        }
                    }
                });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isHowling = false, 30L);
    }

    /**
     * Bond vers la cible
     */
    private void leapAttack(Player target) {
        isLeaping = true;

        playSound(Sound.ENTITY_WOLF_AMBIENT, 1.2f, 1.5f);

        // Direction vers la cible avec arc de saut
        Vector direction = target.getLocation().toVector()
                .subtract(zombie.getLocation().toVector()).normalize();

        // Saut puissant
        zombie.setVelocity(direction.multiply(1.3).setY(0.6));

        playParticles(Particle.CLOUD, zombie.getLocation(), 10, 0.3, 0.1, 0.3);

        // Vérifier l'impact
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid()) {
                isLeaping = false;
                task.cancel();
                return;
            }

            // Particules de saut
            playParticles(Particle.CRIT, zombie.getLocation(), 3, 0.2, 0.2, 0.2);

            // Impact si proche d'un joueur ou au sol
            if (zombie.isOnGround() && tickCounter % 20 > 10) {
                isLeaping = false;
                task.cancel();

                // Dégâts d'impact si proche
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> {
                            double damage = (6 + level + packBonus * 2);
                            p.damage(damage, zombie);
                            playSound(Sound.ENTITY_WOLF_GROWL, 1.5f, 0.6f);
                            playParticles(Particle.DAMAGE_INDICATOR, p.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);

                            // Chance de mettre à terre
                            if (random.nextFloat() < 0.3f) {
                                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                            }
                        });
            }
        }, 5L, 3L);

        // Timeout
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isLeaping = false, 40L);
    }

    /**
     * Attaque sauvage au corps à corps
     */
    private void savageAttack(Player target) {
        // Morsure féroce
        playSound(Sound.ENTITY_WOLF_GROWL, 1f, 1f + random.nextFloat() * 0.3f);

        // Dégâts bonus basés sur la meute
        double baseDamage = 4 + level * 0.5;
        double packDamage = packBonus * 1.5;
        double totalDamage = baseDamage + packDamage;

        target.damage(totalDamage, zombie);

        // Effet de saignement (wither)
        if (random.nextFloat() < 0.25f + packBonus * 0.05f) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
            playParticles(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3);
        }

        // Animation de morsure
        playParticles(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
    }

    /**
     * Manoeuvre de flanc (encerclement)
     */
    private void flankingManeuver(Player target) {
        // Se déplacer sur le côté de la cible
        Vector toTarget = target.getLocation().toVector()
                .subtract(zombie.getLocation().toVector()).normalize();

        // Vecteur perpendiculaire (flanc)
        Vector flankDir = new Vector(-toTarget.getZ(), 0, toTarget.getX());
        if (random.nextBoolean())
            flankDir.multiply(-1);

        // Petit mouvement latéral
        zombie.setVelocity(flankDir.multiply(0.5).setY(0.1));

        playParticles(Particle.CLOUD, zombie.getLocation(), 5, 0.2, 0.1, 0.2);
    }

    @Override
    public void onAttack(Player target) {
        if (target.isDead())
            return; // Protection contre boucle infinie

        currentTarget = target;

        // Morsure enragée
        playSound(Sound.ENTITY_WOLF_GROWL, 1.5f, 0.7f);

        // Dégâts bonus de meute
        if (packBonus > 0) {
            double bonusDamage = packBonus * 2;
            target.damage(bonusDamage, zombie);
        }

        // Chance de renversement
        if (random.nextFloat() < 0.15f + packBonus * 0.03f) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1));
            target.setVelocity(target.getVelocity().add(new Vector(
                    (random.nextDouble() - 0.5) * 0.3,
                    0.2,
                    (random.nextDouble() - 0.5) * 0.3)));
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Cri de douleur
        playSound(Sound.ENTITY_WOLF_HURT, 1f, 1f);

        // Les autres loups deviennent plus agressifs
        if (attacker instanceof Player player) {
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 15, 8, 15).stream()
                    .filter(e -> e instanceof Zombie)
                    .filter(e -> e.hasMetadata("zombiez_type"))
                    .filter(e -> e.getMetadata("zombiez_type").get(0).asString().equals("RABID_WOLF"))
                    .filter(e -> !e.getUniqueId().equals(zombie.getUniqueId()))
                    .forEach(wolf -> {
                        if (wolf instanceof Zombie z) {
                            z.setTarget(player);
                        }
                        wolf.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                                wolf.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2);
                    });
        }

        // Fuite si gravement blessé (instinct de survie)
        if (isHealthBelow(0.2) && attacker instanceof Player player) {
            // 50% de chance de fuir, 50% de devenir enragé
            if (random.nextFloat() < 0.5f) {
                // Fuir
                Vector away = zombie.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                zombie.setVelocity(away.multiply(1.0).setY(0.3));
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, false, false));
            } else {
                // Rage désespérée
                enrage();
                playSound(Sound.ENTITY_WOLF_GROWL, 2f, 0.5f);
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Hurlement de mort
        playSound(Sound.ENTITY_WOLF_DEATH, 1.5f, 0.7f);
        playSound(Sound.ENTITY_WOLF_WHINE, 1f, 0.8f);

        playParticles(Particle.SMOKE, zombie.getLocation(), 20, 0.5, 0.5, 0.5);
        playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3);

        // Les autres loups de la meute deviennent enragés
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 20, 10, 20).stream()
                .filter(e -> e instanceof Zombie)
                .filter(e -> e.hasMetadata("zombiez_type"))
                .filter(e -> e.getMetadata("zombiez_type").get(0).asString().equals("RABID_WOLF"))
                .forEach(wolf -> {
                    if (wolf instanceof LivingEntity living) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, true));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, true));
                        wolf.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                                wolf.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
                        wolf.getWorld().playSound(wolf.getLocation(), Sound.ENTITY_WOLF_HOWL, 1f, 0.8f);
                    }
                });

    }
}
