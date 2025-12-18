package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.zones.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

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

    // TextDisplay flottant au-dessus du zombie
    private TextDisplay healthDisplay;

    // Configuration
    private static final double ELITE_HEALTH = 80.0;
    private static final double ELITE_SPEED = 0.22; // Vitesse reduite mais plus menaçant
    private static final double SPAWN_DISTANCE = 15.0; // Distance de spawn proche du joueur
    private static final int PARTICLE_INTERVAL = 5; // Particules toutes les 5 ticks
    private static final float TEXT_SCALE = 1.3f;

    public EliteHunterEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.ELITE_HUNTER, player, location, zone);
    }

    @Override
    protected void onStart() {
        // Calculer une position de spawn proche du joueur (15 blocs)
        Location playerLoc = player.getLocation();
        double angle = Math.random() * Math.PI * 2;
        Location spawnLoc = playerLoc.clone().add(
            Math.cos(angle) * SPAWN_DISTANCE,
            0,
            Math.sin(angle) * SPAWN_DISTANCE
        );
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        // Spawn le zombie elite
        eliteZombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        eliteUUID = eliteZombie.getUniqueId();
        registerEntity(eliteZombie);

        // Configuration du zombie - plus intelligent et menaçant
        eliteZombie.setCustomName("§c§l💀 Elite Chasseur §7[§e" + (int) ELITE_HEALTH + "§c❤§7]");
        eliteZombie.setCustomNameVisible(true);
        eliteZombie.setBaby(false);
        eliteZombie.setShouldBurnInDay(false);

        // Stats - vitesse reduite mais meilleure armor
        eliteZombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(ELITE_HEALTH);
        eliteZombie.setHealth(ELITE_HEALTH);
        eliteZombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(ELITE_SPEED);
        eliteZombie.getAttribute(Attribute.ARMOR).setBaseValue(12.0); // Plus resistant
        eliteZombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.6); // Resist aux knockbacks

        // Equipement distinctif (armure en or)
        eliteZombie.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
        eliteZombie.getEquipment().setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
        eliteZombie.getEquipment().setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
        eliteZombie.getEquipment().setBoots(new ItemStack(Material.GOLDEN_BOOTS));
        eliteZombie.getEquipment().setHelmetDropChance(0f);
        eliteZombie.getEquipment().setChestplateDropChance(0f);
        eliteZombie.getEquipment().setLeggingsDropChance(0f);
        eliteZombie.getEquipment().setBootsDropChance(0f);

        // Effets visuels - pas de bonus de vitesse (vitesse reduite)
        eliteZombie.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Tag pour identification
        eliteZombie.addScoreboardTag("micro_event_entity");
        eliteZombie.addScoreboardTag("elite_hunter");
        eliteZombie.addScoreboardTag("event_" + id);

        // Creer le TextDisplay flottant au-dessus du zombie
        Location displayLoc = spawnLoc.clone().add(0, 2.8, 0);
        healthDisplay = displayLoc.getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(180, 0, 0, 0));
            display.setTransformation(new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                new org.joml.Quaternionf(),
                new Vector3f(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE),
                new org.joml.Quaternionf()
            ));
            display.text(createHealthDisplay(ELITE_HEALTH, ELITE_HEALTH));
        });
        registerEntity(healthDisplay);

        // Effet de spawn
        spawnLoc.getWorld().spawnParticle(Particle.SMOKE, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    /**
     * Cree le Component pour l'affichage de vie
     */
    private Component createHealthDisplay(double currentHealth, double maxHealth) {
        double healthPercent = currentHealth / maxHealth;
        NamedTextColor healthColor = healthPercent > 0.5 ? NamedTextColor.GREEN :
            (healthPercent > 0.25 ? NamedTextColor.YELLOW : NamedTextColor.RED);

        // Barre de vie visuelle
        int barLength = 10;
        int filled = (int) (healthPercent * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }

        return Component.text("💀 ÉLITE CHASSEUR 💀", NamedTextColor.RED, TextDecoration.BOLD)
            .appendNewline()
            .append(Component.text(bar.toString(), healthColor))
            .append(Component.text(" " + (int) currentHealth + "❤", healthColor));
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

        // Mettre a jour le TextDisplay (suit le zombie et affiche la vie)
        if (healthDisplay != null && healthDisplay.isValid()) {
            healthDisplay.teleport(eliteZombie.getLocation().add(0, 2.8, 0));
            healthDisplay.text(createHealthDisplay(eliteZombie.getHealth(), ELITE_HEALTH));
        }

        // Faire fuir le zombie LOIN du joueur (toutes les 5 ticks pour optimisation)
        if (elapsedTicks % 5 == 0) {
            makeZombieFlee();
        }

        // Spawn des particules de trainee (toutes les 10 ticks = 0.5s)
        ticksSinceLastParticle++;
        if (ticksSinceLastParticle >= 10) {
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

        // Le nom du zombie n'est plus necessaire car on utilise le TextDisplay
        eliteZombie.setCustomNameVisible(false);
    }

    /**
     * Fait fuir le zombie loin du joueur - Navigation intelligente via Pathfinder
     */
    private void makeZombieFlee() {
        if (eliteZombie == null || !eliteZombie.isValid() || player == null) return;

        Location zombieLoc = eliteZombie.getLocation();
        Location playerLoc = player.getLocation();

        // Direction opposee au joueur
        Vector fleeDirection = zombieLoc.toVector().subtract(playerLoc.toVector()).normalize();

        // Ajouter un peu de randomisation pour un comportement plus naturel
        fleeDirection.add(new Vector(
            (Math.random() - 0.5) * 0.4,
            0,
            (Math.random() - 0.5) * 0.4
        )).normalize();

        // Calculer la destination (plus loin pour meilleure navigation)
        Location targetLoc = zombieLoc.clone().add(fleeDirection.multiply(15));

        // Ajuster Y au sol pour eviter les bugs de navigation
        targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc) + 1);

        // Utiliser le Pathfinder natif de Paper pour une navigation fluide
        try {
            eliteZombie.getPathfinder().moveTo(targetLoc, ELITE_SPEED * 1.5);
        } catch (Exception e) {
            // Fallback: utiliser velocite directe si pathfinder echoue
            eliteZombie.setVelocity(fleeDirection.multiply(0.3).setY(0));
        }

        // Faire regarder dans la direction de fuite
        eliteZombie.lookAt(targetLoc);
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
        // Nettoyer le TextDisplay
        if (healthDisplay != null && healthDisplay.isValid()) {
            healthDisplay.remove();
        }

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
