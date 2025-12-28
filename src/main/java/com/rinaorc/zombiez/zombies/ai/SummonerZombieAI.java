package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IA pour les zombies SUMMONER
 * Comportement: Invoque des minions, reste à distance, rituels
 * Types: NECROMANCER
 */
public class SummonerZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private final List<UUID> summonedMinions = new ArrayList<>();
    private static final int MAX_MINIONS = 6;
    private boolean isChanneling = false;
    private int ritualProgress = 0;

    public SummonerZombieAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 12000; // 12 secondes entre invocations
    }

    @Override
    public void tick() {
        tickCounter++;

        // Nettoyer les minions morts
        summonedMinions.removeIf(uuid -> {
            Entity entity = plugin.getServer().getEntity(uuid);
            return entity == null || !entity.isValid() || entity.isDead();
        });

        switch (zombieType) {
            case NECROMANCER -> tickNecromancer();
            default -> tickNecromancer();
        }
    }

    /**
     * Necromancer: Invoque des zombies, drain de vie, rituels sombres
     */
    private void tickNecromancer() {
        // Aura sombre permanente
        if (tickCounter % 15 == 0) {
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4);
            playParticles(Particle.ASH, zombie.getLocation().add(0, 0.5, 0), 5, 1, 0.5, 1);
        }

        Player target = findNearestPlayer(25);
        if (target == null)
            return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Maintenir la distance
        if (distance < 6) {
            retreatFromTarget(target);
        }

        // Invoquer des minions si on en a peu
        if (canUseAbility() && summonedMinions.size() < MAX_MINIONS && !isChanneling) {
            summonMinions(target);
            useAbility();
        }

        // Sort de drain de vie
        if (tickCounter % 80 == 0 && distance < 15) {
            darkBolt(target);
        }

        // Ritual quand blessé
        if (isHealthBelow(0.4) && !isChanneling && canUseAbility()) {
            startDarkRitual();
            useAbility();
        }
    }

    /**
     * Recule de la cible
     */
    private void retreatFromTarget(Player target) {
        Vector away = zombie.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();
        zombie.setVelocity(away.multiply(0.6).setY(0.2));
    }

    /**
     * Invoque des minions
     */
    private void summonMinions(Player target) {
        isChanneling = true;

        // Animation de canalisation
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));
        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.8f);

        // Cercle d'invocation
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            Location particleLoc = zombie.getLocation().add(Math.cos(rad) * 2, 0.1, Math.sin(rad) * 2);
            playParticles(Particle.SOUL_FIRE_FLAME, particleLoc, 5, 0.1, 0.1, 0.1);
        }

        // Invocation après délai
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) {
                isChanneling = false;
                return;
            }

            int minionCount = 2 + random.nextInt(2);
            playSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.5f, 0.5f);

            for (int i = 0; i < minionCount && summonedMinions.size() < MAX_MINIONS; i++) {
                Location spawnLoc = getRandomSpawnLocation();
                if (spawnLoc == null)
                    continue;

                // Effet de spawn
                playParticles(Particle.SOUL, spawnLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3);

                // Créer le minion via ZombieManager
                var zombieManager = plugin.getZombieManager();
                if (zombieManager != null) {
                    var minion = zombieManager.spawnZombie(
                            random.nextFloat() < 0.7f ? ZombieType.WALKER : ZombieType.CRAWLER,
                            spawnLoc,
                            Math.max(1, level - 3));

                    if (minion != null) {
                        summonedMinions.add(minion.getEntityId());

                        // Marquer comme minion
                        Entity entity = plugin.getServer().getEntity(minion.getEntityId());
                        if (entity instanceof Zombie z) {
                            z.setTarget(target);
                            z.addScoreboardTag("necro_minion_" + zombie.getUniqueId());
                        }
                    }
                }
            }

            isChanneling = false;
        }, 40L);
    }

    /**
     * Obtient une position de spawn aléatoire
     */
    private Location getRandomSpawnLocation() {
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 3 + random.nextDouble() * 4;
            Location loc = zombie.getLocation().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance);

            // Trouver le sol
            for (int y = (int) loc.getY(); y > zombie.getWorld().getMinHeight(); y--) {
                Location checkLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
                if (checkLoc.getBlock().getType().isSolid()) {
                    return checkLoc.add(0, 1, 0);
                }
            }
        }
        return null;
    }

    /**
     * Projectile de magie noire
     */
    private void darkBolt(Player target) {
        playSound(Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.5f);

        // Créer un projectile visuel
        Vector direction = target.getEyeLocation().toVector()
                .subtract(zombie.getEyeLocation().toVector()).normalize();

        Location projectileLoc = zombie.getEyeLocation().clone();

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            projectileLoc.add(direction.clone().multiply(1.5));
            playParticles(Particle.SOUL_FIRE_FLAME, projectileLoc, 5, 0.1, 0.1, 0.1);

            // Vérifier l'impact
            if (projectileLoc.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                // Impact
                double damage = 4 + level;
                target.damage(damage, zombie);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                heal(damage * 0.5); // Drain de vie
                playParticles(Particle.SOUL, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                task.cancel();
            }

            // Timeout
            if (projectileLoc.distance(zombie.getLocation()) > 30) {
                task.cancel();
            }
        }, 0L, 1L);
    }

    /**
     * Rituel sombre pour se soigner et buff les minions
     */
    private void startDarkRitual() {
        isChanneling = true;
        ritualProgress = 0;

        playSound(Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 0.5f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, false));

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || !isChanneling) {
                task.cancel();
                return;
            }

            ritualProgress++;

            // Cercle de rituel
            double radius = 3 + (ritualProgress % 10) * 0.3;
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle + ritualProgress * 5);
                Location particleLoc = zombie.getLocation().add(Math.cos(rad) * radius, 0.1, Math.sin(rad) * radius);
                playParticles(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0, 0, 0);
            }

            // Pilier central
            playParticles(Particle.SOUL, zombie.getLocation().add(0, ritualProgress * 0.1, 0), 5, 0.2, 0.2, 0.2);

            if (ritualProgress >= 50) {
                // Rituel complet
                completeRitual();
                task.cancel();
            }
        }, 0L, 2L);
    }

    /**
     * Complète le rituel
     */
    private void completeRitual() {
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 50, 2, 2, 2);

        // Grosse régénération
        heal(zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() * 0.4);

        // Buff tous les minions
        for (UUID minionId : summonedMinions) {
            Entity entity = plugin.getServer().getEntity(minionId);
            if (entity instanceof Zombie minion) {
                minion.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, true));
                minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, false, true));
                minion.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0, false, true));
                playParticles(Particle.SOUL_FIRE_FLAME, minion.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3);
            }
        }

        // Invoquer des minions bonus
        for (int i = 0; i < 2; i++) {
            Location spawnLoc = getRandomSpawnLocation();
            if (spawnLoc != null) {
                playParticles(Particle.SOUL, spawnLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3);
                var zombieManager = plugin.getZombieManager();
                if (zombieManager != null) {
                    var minion = zombieManager.spawnZombie(ZombieType.SHADOW, spawnLoc, level);
                    if (minion != null) {
                        summonedMinions.add(minion.getEntityId());
                    }
                }
            }
        }

        isChanneling = false;
    }

    @Override
    public void onAttack(Player target) {
        if (target.isDead())
            return; // Protection contre boucle infinie

        currentTarget = target;

        // Le Nécromancien drain la vie au corps à corps
        double drainAmount = 2 + level * 0.5;
        target.damage(drainAmount, zombie);
        heal(drainAmount * 0.8);

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
        playParticles(Particle.SOUL, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);

        // Fuir après l'attaque
        retreatFromTarget(target);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Interrompre le rituel si touché
        if (isChanneling && random.nextFloat() < 0.3f) {
            isChanneling = false;
            playSound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 0.5f);
        }

        // Les minions défendent leur maître
        if (attacker instanceof Player player) {
            for (UUID minionId : summonedMinions) {
                Entity entity = plugin.getServer().getEntity(minionId);
                if (entity instanceof Zombie minion) {
                    minion.setTarget(player);
                }
            }
        }

        // Téléportation d'urgence si critique
        if (isHealthBelow(0.2) && attacker instanceof Player player) {
            teleportAway(player);
        }
    }

    /**
     * Téléportation d'urgence
     */
    private void teleportAway(Player threat) {
        Vector away = zombie.getLocation().toVector()
                .subtract(threat.getLocation().toVector()).normalize();
        Location teleportLoc = zombie.getLocation().add(away.multiply(10));

        // Trouver une position valide
        for (int y = (int) teleportLoc.getY(); y > zombie.getWorld().getMinHeight(); y--) {
            Location checkLoc = new Location(teleportLoc.getWorld(), teleportLoc.getX(), y, teleportLoc.getZ());
            if (checkLoc.getBlock().getType().isSolid()) {
                teleportLoc = checkLoc.add(0, 1, 0);
                break;
            }
        }

        playParticles(Particle.SOUL, zombie.getLocation(), 30, 0.5, 1, 0.5);
        playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        zombie.teleport(teleportLoc);
        playParticles(Particle.SOUL, teleportLoc, 30, 0.5, 1, 0.5);
    }

    @Override
    public void onDeath(Player killer) {
        // Les minions deviennent fous à la mort du maître
        playSound(Sound.ENTITY_WITHER_DEATH, 0.5f, 1.5f);
        playParticles(Particle.SOUL, zombie.getLocation(), 50, 2, 2, 2);

        for (UUID minionId : summonedMinions) {
            Entity entity = plugin.getServer().getEntity(minionId);
            if (entity instanceof Zombie minion) {
                // Les minions gagnent de la force mais perdent de la vie
                minion.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2, false, true));
                minion.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 1, false, true));
                playParticles(Particle.SOUL_FIRE_FLAME, minion.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3);
            }
        }
    }
}
