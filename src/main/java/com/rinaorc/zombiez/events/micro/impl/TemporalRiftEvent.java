package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Faille Temporelle - Un portail crache des zombies
 *
 * Mecanique:
 * - Un portail visuel apparait
 * - Des zombies spawn toutes les 1-2 secondes
 * - Tuer tous les zombies (15-25) ferme le portail
 * - Si le timer expire, les zombies restants deviennent enrages
 */
public class TemporalRiftEvent extends MicroEvent {

    private final Set<UUID> riftZombies = new HashSet<>();
    private int zombiesToSpawn;
    private int zombiesSpawned = 0;
    private int zombiesKilled = 0;
    private int ticksSinceLastSpawn = 0;
    private int spawnInterval = 30; // 1.5 secondes entre spawns (accelere)
    private boolean riftClosed = false;

    // Configuration
    private static final int MIN_ZOMBIES = 15;
    private static final int MAX_ZOMBIES = 25;
    private static final double ZOMBIE_HEALTH = 30.0;

    public TemporalRiftEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.TEMPORAL_RIFT, player, location, zone);

        // Nombre de zombies base sur la zone
        int baseZombies = MIN_ZOMBIES + (zone.getId() / 10);
        this.zombiesToSpawn = Math.min(MAX_ZOMBIES, baseZombies);
    }

    @Override
    protected void onStart() {
        // Effet d'ouverture du portail
        spawnRiftOpenEffect();

        // Spawn les premiers zombies immediatement
        spawnZombie();
        spawnZombie();
    }

    @Override
    protected void tick() {
        // Effet visuel du portail
        if (elapsedTicks % 5 == 0) {
            spawnRiftParticles();
        }

        // Son ambiant du portail
        if (elapsedTicks % 40 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.2f);
        }

        // Spawn des zombies
        ticksSinceLastSpawn++;
        if (zombiesSpawned < zombiesToSpawn && ticksSinceLastSpawn >= spawnInterval) {
            ticksSinceLastSpawn = 0;
            spawnZombie();

            // Accelerer les spawns au fur et a mesure
            spawnInterval = Math.max(15, spawnInterval - 1);
        }

        // Verifier si tous les zombies sont tues
        if (zombiesKilled >= zombiesToSpawn) {
            riftClosed = true;
            complete();
            return;
        }

        // Cleanup des zombies morts de la liste
        riftZombies.removeIf(uuid -> {
            var entity = plugin.getServer().getEntity(uuid);
            return entity == null || entity.isDead();
        });

        // ActionBar
        int remaining = zombiesToSpawn - zombiesKilled;
        String progress = createProgressBar(zombiesKilled, zombiesToSpawn);
        sendActionBar("§d⚡ Faille Temporelle §7| " + progress + " §c" + remaining + " restants §7| §e" + getRemainingTimeSeconds() + "s");
    }

    /**
     * Spawn un zombie custom de la faille via le ZombieManager
     */
    private void spawnZombie() {
        if (zombiesSpawned >= zombiesToSpawn) return;

        // Position aleatoire autour du portail
        double angle = Math.random() * Math.PI * 2;
        double radius = 1 + Math.random() * 2;
        Location spawnLoc = location.clone().add(
            Math.cos(angle) * radius,
            0,
            Math.sin(angle) * radius
        );
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        // Selectionner un type de zombie custom approprie pour la zone
        ZombieType zombieType = selectZombieType();
        int level = Math.max(1, zone.getId() / 2); // Niveau base sur la zone

        // Spawn via le ZombieManager du plugin
        ZombieManager.ActiveZombie activeZombie = plugin.getZombieManager().spawnZombie(zombieType, spawnLoc, level);

        if (activeZombie != null) {
            UUID zombieId = activeZombie.getEntityId();
            riftZombies.add(zombieId);

            // Recuperer l'entite pour la configurer
            var entity = plugin.getServer().getEntity(zombieId);
            if (entity instanceof LivingEntity living) {
                registerEntity(living);

                // Ajouter des tags specifiques a l'event
                living.addScoreboardTag("micro_event_entity");
                living.addScoreboardTag("temporal_rift");
                living.addScoreboardTag("event_" + id);

                // Ajouter l'effet temporel (particules violettes)
                living.setGlowing(true);

                // Cibler le joueur si c'est un Zombie
                if (living instanceof Zombie zombie) {
                    zombie.setTarget(player);
                }
            }

            zombiesSpawned++;
        } else {
            // Fallback: spawn un zombie vanilla si le ZombieManager echoue
            spawnFallbackZombie(spawnLoc);
        }

        // Effet de spawn depuis le portail
        spawnLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 15, 0.3, 0.5, 0.3, 0.1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    /**
     * Selectionne un type de zombie custom approprie pour la zone
     */
    private ZombieType selectZombieType() {
        // Obtenir les types de zombies valides pour cette zone
        List<ZombieType> validTypes = java.util.Arrays.stream(ZombieType.values())
            .filter(t -> t.canSpawnInZone(zone.getId()))
            .filter(t -> !t.isBoss()) // Pas de boss dans les events
            .filter(t -> t.getTier() <= 3) // Limiter la difficulte
            .toList();

        if (validTypes.isEmpty()) {
            // Fallback sur les types de base
            return ZombieType.WALKER;
        }

        // Choisir aleatoirement parmi les types valides
        return validTypes.get((int) (Math.random() * validTypes.size()));
    }

    /**
     * Spawn un zombie vanilla en fallback
     */
    private void spawnFallbackZombie(Location spawnLoc) {
        Zombie zombie = (Zombie) location.getWorld().spawn(spawnLoc, Zombie.class, z -> {
            z.setCustomName("§d⚡ Zombie Temporel §7[§c" + (int) ZOMBIE_HEALTH + "❤§7]");
            z.setCustomNameVisible(true);
            z.setBaby(false);
            z.setShouldBurnInDay(false);
            z.setGlowing(true);

            z.getAttribute(Attribute.MAX_HEALTH).setBaseValue(ZOMBIE_HEALTH);
            z.setHealth(ZOMBIE_HEALTH);
            z.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.28);

            z.addScoreboardTag("micro_event_entity");
            z.addScoreboardTag("temporal_rift");
            z.addScoreboardTag("event_" + id);

            z.setTarget(player);
        });

        riftZombies.add(zombie.getUniqueId());
        registerEntity(zombie);
        zombiesSpawned++;
    }

    /**
     * Effet d'ouverture du portail
     */
    private void spawnRiftOpenEffect() {
        Location center = location.clone().add(0, 1, 0);
        location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 40, 0.8, 1.2, 0.8, 0.15);
        location.getWorld().spawnParticle(Particle.END_ROD, center, 15, 0.4, 0.8, 0.4, 0.08);
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_TRIGGER, 1f, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2f);
    }

    /**
     * Particules continues du portail (optimise)
     */
    private void spawnRiftParticles() {
        Location center = location.clone().add(0, 1.5, 0);

        // Cercle de particules (4 points au lieu de 8)
        for (int i = 0; i < 4; i++) {
            double angle = (Math.PI * 2 / 4) * i + (elapsedTicks * 0.1);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            location.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 0, z), 1, 0, 0, 0, 0.5);
        }

        // Centre du portail
        location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 3, 0.2, 0.4, 0.2, 0.03);
    }

    /**
     * Cree une barre de progression
     */
    private String createProgressBar(int current, int total) {
        int bars = 10;
        int filled = (int) ((double) current / total * bars);

        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a█");
            } else {
                sb.append("§8░");
            }
        }
        sb.append("§7]");
        return sb.toString();
    }

    @Override
    protected void onCleanup() {
        // Effet de fermeture du portail
        if (riftClosed) {
            Location center = location.clone().add(0, 1, 0);
            location.getWorld().spawnParticle(Particle.FLASH, center, 1);
            location.getWorld().spawnParticle(Particle.END_ROD, center, 20, 0.8, 1.2, 0.8, 0.15);
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
        } else {
            // Echec - les zombies restants deviennent enrages
            for (UUID zombieId : riftZombies) {
                var entity = plugin.getServer().getEntity(zombieId);
                if (entity instanceof Zombie zombie && !zombie.isDead()) {
                    // Effet d'enragement
                    zombie.setCustomName("§4§l⚡ Zombie Enragé");
                    zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(10);
                    zombie.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                    // Retirer le tag pour qu'ils restent apres l'event
                    zombie.removeScoreboardTag("micro_event_entity");
                }
            }
            riftZombies.clear();
            spawnedEntities.clear(); // Ne pas supprimer les zombies enrages
        }
    }

    @Override
    protected int getBonusPoints() {
        if (!riftClosed) return 0;

        // Bonus si tous tues rapidement
        int timeSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (timeSeconds < 15) return 200;  // Perfect clear
        if (timeSeconds < 20) return 100;  // Fast clear
        return 50;                          // Normal clear
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (riftZombies.contains(entity.getUniqueId())) {
            // Effet de hit
            entity.getWorld().spawnParticle(Particle.ENCHANTED_HIT, entity.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.05);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (riftZombies.contains(entity.getUniqueId())) {
            zombiesKilled++;
            riftZombies.remove(entity.getUniqueId());

            // Effet de mort
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.1);

            // Son de compteur
            float pitch = 0.5f + (zombiesKilled / (float) zombiesToSpawn);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, pitch);

            // Message a certains paliers
            int remaining = zombiesToSpawn - zombiesKilled;
            if (remaining == 10 || remaining == 5 || remaining == 1) {
                player.sendMessage("§d⚡ §7" + remaining + " zombie" + (remaining > 1 ? "s" : "") + " restant" + (remaining > 1 ? "s" : "") + "!");
            }

            return true;
        }
        return false;
    }
}
