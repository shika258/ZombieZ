package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Elite Chasseur - Un zombie elite fuit le joueur
 *
 * Mecanique:
 * - Un zombie elite spawn a distance et FUIT le joueur
 * - Le joueur doit le traquer et le tuer avant qu'il ne s'echappe
 * - Le zombie laisse une trainee de particules
 * - Recompenses augmentent si tue rapidement
 */
public class EliteHunterEvent extends MicroEvent {

    private Zombie eliteZombie;
    private UUID eliteUUID;
    private int ticksSinceLastParticle = 0;
    private boolean killed = false;
    private long killTime = 0;

    // Configuration
    private static final double ELITE_HEALTH = 80.0;
    private static final double ELITE_SPEED = 0.35; // Tres rapide
    private static final int PARTICLE_INTERVAL = 5; // Particules toutes les 5 ticks

    public EliteHunterEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.ELITE_HUNTER, player, location, zone);
    }

    @Override
    protected void onStart() {
        // Spawn le zombie elite
        eliteZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        eliteUUID = eliteZombie.getUniqueId();
        registerEntity(eliteZombie);

        // Configuration du zombie
        eliteZombie.setCustomName("§c§l💀 Elite Chasseur §7[§e" + (int) ELITE_HEALTH + "§c❤§7]");
        eliteZombie.setCustomNameVisible(true);
        eliteZombie.setBaby(false);
        eliteZombie.setShouldBurnInDay(false);

        // Stats
        eliteZombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(ELITE_HEALTH);
        eliteZombie.setHealth(ELITE_HEALTH);
        eliteZombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(ELITE_SPEED);
        eliteZombie.getAttribute(Attribute.ARMOR).setBaseValue(8.0);

        // Equipement distinctif (armure en or)
        eliteZombie.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        eliteZombie.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        eliteZombie.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
        eliteZombie.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
        eliteZombie.getEquipment().setHelmetDropChance(0f);
        eliteZombie.getEquipment().setChestplateDropChance(0f);
        eliteZombie.getEquipment().setLeggingsDropChance(0f);
        eliteZombie.getEquipment().setBootsDropChance(0f);

        // Effets
        eliteZombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        eliteZombie.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Tag pour identification
        eliteZombie.addScoreboardTag("micro_event_entity");
        eliteZombie.addScoreboardTag("elite_hunter");
        eliteZombie.addScoreboardTag("event_" + id);

        // Effet de spawn
        location.getWorld().spawnParticle(Particle.SMOKE, location, 30, 0.5, 1, 0.5, 0.1);
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    @Override
    protected void tick() {
        if (eliteZombie == null || eliteZombie.isDead()) {
            if (killed) {
                complete();
            } else {
                fail();
            }
            return;
        }

        // Faire fuir le zombie LOIN du joueur
        makeZombieFlee();

        // Spawn des particules de trainee
        ticksSinceLastParticle++;
        if (ticksSinceLastParticle >= PARTICLE_INTERVAL) {
            ticksSinceLastParticle = 0;
            spawnTrailParticles();
        }

        // Verifier si le zombie est trop loin (echec)
        double distance = eliteZombie.getLocation().distance(player.getLocation());
        if (distance > 80) {
            // Le zombie s'est echappe!
            player.sendMessage("§c§l✗ §7L'Elite Chasseur s'est echappe!");
            fail();
            return;
        }

        // ActionBar avec infos
        String direction = getDirectionToTarget();
        sendActionBar("§c💀 Elite Chasseur §7| Distance: §e" + (int) distance + "m " + direction +
            " §7| Temps: §e" + getRemainingTimeSeconds() + "s");

        // Mettre a jour le nom avec la vie
        double healthPercent = eliteZombie.getHealth() / ELITE_HEALTH;
        String healthColor = healthPercent > 0.5 ? "§a" : (healthPercent > 0.25 ? "§e" : "§c");
        eliteZombie.setCustomName("§c§l💀 Elite Chasseur §7[" + healthColor + (int) eliteZombie.getHealth() + "§c❤§7]");
    }

    /**
     * Fait fuir le zombie loin du joueur
     */
    private void makeZombieFlee() {
        if (eliteZombie == null || player == null) return;

        Location zombieLoc = eliteZombie.getLocation();
        Location playerLoc = player.getLocation();

        // Direction opposee au joueur
        Vector fleeDirection = zombieLoc.toVector().subtract(playerLoc.toVector()).normalize();

        // Ajouter un peu de randomisation
        fleeDirection.add(new Vector(
            (Math.random() - 0.5) * 0.3,
            0,
            (Math.random() - 0.5) * 0.3
        )).normalize();

        // Calculer la destination
        Location targetLoc = zombieLoc.clone().add(fleeDirection.multiply(10));

        // Faire regarder dans la direction de fuite
        eliteZombie.lookAt(targetLoc);

        // Appliquer la velocite
        eliteZombie.setVelocity(fleeDirection.multiply(0.4).setY(0));
    }

    /**
     * Spawn les particules de trainee
     */
    private void spawnTrailParticles() {
        if (eliteZombie == null || eliteZombie.isDead()) return;

        Location loc = eliteZombie.getLocation().add(0, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.2, 0.2, 0.2, 0.01);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.1, 0.1, 0.1, 0.01);
    }

    /**
     * Obtient la direction vers la cible
     */
    private String getDirectionToTarget() {
        if (eliteZombie == null) return "";

        Location playerLoc = player.getLocation();
        Location targetLoc = eliteZombie.getLocation();

        double dx = targetLoc.getX() - playerLoc.getX();
        double dz = targetLoc.getZ() - playerLoc.getZ();

        if (Math.abs(dz) > Math.abs(dx)) {
            return dz < 0 ? "§b↑Nord" : "§c↓Sud";
        } else {
            return dx > 0 ? "§e→Est" : "§6←Ouest";
        }
    }

    @Override
    protected void onCleanup() {
        if (eliteZombie != null && !eliteZombie.isDead()) {
            // Effet de disparition
            Location loc = eliteZombie.getLocation();
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.3, 0.5, 0.3, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        }
    }

    @Override
    protected int getBonusPoints() {
        if (!killed) return 0;

        // Bonus basé sur la rapidité du kill
        long timeToKill = killTime - startTime;
        int secondsToKill = (int) (timeToKill / 1000);

        if (secondsToKill < 10) return 300;      // Tres rapide: +300
        if (secondsToKill < 20) return 200;      // Rapide: +200
        if (secondsToKill < 30) return 100;      // Normal: +100
        return 50;                                // Lent: +50
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (entity.getUniqueId().equals(eliteUUID)) {
            // Effet de hit
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (entity.getUniqueId().equals(eliteUUID)) {
            killed = true;
            killTime = System.currentTimeMillis();

            // Effet de mort spectaculaire
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().spawnParticle(Particle.SOUL, loc, 20, 0.5, 1, 0.5, 0.05);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.5f);

            // Annonce
            long timeToKill = killTime - startTime;
            player.sendMessage("§a§l✓ §7Elite Chasseur elimine en §e" + String.format("%.1f", timeToKill / 1000.0) + "s§7!");

            return true;
        }
        return false;
    }
}
