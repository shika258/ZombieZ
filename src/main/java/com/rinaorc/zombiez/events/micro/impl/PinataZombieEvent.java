package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

/**
 * Pinata Zombie - Un zombie dore rempli de loot
 *
 * Mecanique:
 * - Un zombie dore/brillant spawn et ne bouge pas
 * - Le tuer fait EXPLOSER du loot dans toutes les directions
 * - Plus le joueur frappe vite, plus il y a de loot
 * - Effet visuel tres satisfaisant a la mort
 */
public class PinataZombieEvent extends MicroEvent {

    private Zombie pinataZombie;
    private UUID pinataUUID;
    private boolean killed = false;
    private int hitCount = 0;
    private final Random random = new Random();

    // Configuration
    private static final double PINATA_HEALTH = 150.0;
    private static final int BASE_LOOT_COUNT = 8;
    private static final int MAX_LOOT_COUNT = 15;

    public PinataZombieEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone) {
        super(plugin, MicroEventType.PINATA_ZOMBIE, player, location, zone);
    }

    @Override
    protected void onStart() {
        // Spawn la pinata
        pinataZombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        pinataUUID = pinataZombie.getUniqueId();
        registerEntity(pinataZombie);

        // Configuration
        pinataZombie.setCustomName("§e§l🎁 PIÑATA ZOMBIE §6✨");
        pinataZombie.setCustomNameVisible(true);
        pinataZombie.setBaby(false);
        pinataZombie.setShouldBurnInDay(false);
        pinataZombie.setAI(false); // Ne bouge pas!

        // Stats (beaucoup de vie)
        pinataZombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(PINATA_HEALTH);
        pinataZombie.setHealth(PINATA_HEALTH);
        pinataZombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0); // Immobile

        // Armure doree coloree
        Color goldColor = Color.fromRGB(255, 215, 0);
        Color rainbowColors[] = {
            Color.fromRGB(255, 0, 0),
            Color.fromRGB(255, 165, 0),
            Color.fromRGB(255, 255, 0),
            Color.fromRGB(0, 255, 0)
        };

        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, goldColor);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, rainbowColors[0]);
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, rainbowColors[1]);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, rainbowColors[2]);

        pinataZombie.getEquipment().setHelmet(helmet);
        pinataZombie.getEquipment().setChestplate(chestplate);
        pinataZombie.getEquipment().setLeggings(leggings);
        pinataZombie.getEquipment().setBoots(boots);
        pinataZombie.getEquipment().setHelmetDropChance(0f);
        pinataZombie.getEquipment().setChestplateDropChance(0f);
        pinataZombie.getEquipment().setLeggingsDropChance(0f);
        pinataZombie.getEquipment().setBootsDropChance(0f);

        // Effet de glow
        pinataZombie.setGlowing(true);

        // Tags
        pinataZombie.addScoreboardTag("micro_event_entity");
        pinataZombie.addScoreboardTag("pinata_zombie");
        pinataZombie.addScoreboardTag("event_" + id);

        // Effet de spawn (reduit)
        location.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, 1, 0), 20, 0.4, 0.8, 0.4, 0.15);
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
    }

    /**
     * Cree une piece d'armure en cuir coloree
     */
    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void tick() {
        if (pinataZombie == null || pinataZombie.isDead()) {
            if (killed) {
                complete();
            } else {
                fail();
            }
            return;
        }

        // Particules scintillantes (toutes les 8 ticks = reduit)
        if (elapsedTicks % 8 == 0) {
            spawnSparkleParticles();
        }

        // Son de clochette periodique
        if (elapsedTicks % 40 == 0) {
            location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 1.5f + random.nextFloat() * 0.5f);
        }

        // Rotation de la pinata via bodyYaw (plus performant que teleport)
        if (elapsedTicks % 2 == 0) {
            pinataZombie.setBodyYaw(pinataZombie.getBodyYaw() + 10);
        }

        // ActionBar
        double healthPercent = pinataZombie.getHealth() / PINATA_HEALTH;
        String healthBar = createHealthBar(healthPercent);
        sendActionBar("§e🎁 Piñata §7| " + healthBar + " §7| Hits: §e" + hitCount + " §7| §e" + getRemainingTimeSeconds() + "s");
    }

    // Cache pour eviter de creer des objets a chaque tick
    private static final Particle.DustOptions GOLD_DUST = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f);

    /**
     * Particules scintillantes (optimise)
     */
    private void spawnSparkleParticles() {
        if (pinataZombie == null) return;

        Location loc = pinataZombie.getLocation().add(0, 1, 0);

        // Une seule etoile
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 0.5 + random.nextDouble() * 0.5;
        loc.getWorld().spawnParticle(Particle.END_ROD,
            loc.getX() + Math.cos(angle) * radius,
            loc.getY() + random.nextDouble() * 1.5,
            loc.getZ() + Math.sin(angle) * radius,
            1, 0, 0, 0, 0);

        // Particule doree
        loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.3, 0.5, 0.3, 0, GOLD_DUST);
    }

    /**
     * Cree une barre de vie
     */
    private String createHealthBar(double percent) {
        int bars = 15;
        int filled = (int) (percent * bars);

        StringBuilder sb = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§e█");
            } else {
                sb.append("§8░");
            }
        }
        sb.append("§7]");
        return sb.toString();
    }

    @Override
    protected void onCleanup() {
        // Cleanup gere dans handleDeath
    }

    @Override
    protected int getBonusPoints() {
        // Bonus base sur le nombre de hits (plus de hits = joueur actif)
        return Math.min(hitCount * 5, 200);
    }

    // Couleurs pre-calculees pour les confettis
    private static final Color[] CONFETTI_COLORS = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA, Color.FUCHSIA
    };

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (entity.getUniqueId().equals(pinataUUID)) {
            hitCount++;

            // Effet de hit satisfaisant (reduit)
            Location loc = entity.getLocation().add(0, 1, 0);
            loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 5, 0.2, 0.2, 0.2, 0.15);

            // Son de hit qui augmente en pitch
            float pitch = Math.min(2.0f, 0.5f + (hitCount * 0.05f));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, pitch);

            // Un seul confetti colore par hit (utilise couleurs pre-calculees)
            Color confettiColor = CONFETTI_COLORS[hitCount % CONFETTI_COLORS.length];
            loc.getWorld().spawnParticle(Particle.DUST, loc, 2,
                0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(confettiColor, 1.0f));

            return true;
        }
        return false;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        if (entity.getUniqueId().equals(pinataUUID)) {
            killed = true;

            Location loc = entity.getLocation().add(0, 1, 0);

            // EXPLOSION DE LOOT SPECTACULAIRE!
            explodeLoot(loc);

            // Effets visuels (equilibres - encore festifs mais pas excessifs)
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.8, 1.5, 0.8, 0.3);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 25, 0.8, 1.2, 0.8, 0.2);
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1);

            // Sons de celebration
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
            loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

            // Message
            player.sendMessage("§e§l🎁 PIÑATA EXPLOSÉE! §7Ramassez le loot!");

            return true;
        }
        return false;
    }

    /**
     * Fait exploser le loot dans toutes les directions
     */
    private void explodeLoot(Location center) {
        // Calculer le nombre de loot (base + bonus hits)
        int lootCount = BASE_LOOT_COUNT + Math.min(hitCount / 3, MAX_LOOT_COUNT - BASE_LOOT_COUNT);

        for (int i = 0; i < lootCount; i++) {
            // Generer un item
            ItemStack lootItem = generateLootItem();

            // Spawn l'item avec velocite vers l'exterieur
            Item droppedItem = center.getWorld().dropItem(center, lootItem);

            // Velocite aleatoire vers l'exterieur (comme une explosion)
            double angle = random.nextDouble() * Math.PI * 2;
            double upward = 0.3 + random.nextDouble() * 0.4;
            double outward = 0.2 + random.nextDouble() * 0.3;

            Vector velocity = new Vector(
                Math.cos(angle) * outward,
                upward,
                Math.sin(angle) * outward
            );
            droppedItem.setVelocity(velocity);
        }

        // Ajouter quelques pieces (points visuels)
        for (int i = 0; i < 5; i++) {
            ItemStack goldNugget = new ItemStack(Material.GOLD_NUGGET, random.nextInt(5) + 1);
            Item nugget = center.getWorld().dropItem(center, goldNugget);

            double angle = random.nextDouble() * Math.PI * 2;
            nugget.setVelocity(new Vector(
                Math.cos(angle) * 0.3,
                0.5,
                Math.sin(angle) * 0.3
            ));
        }
    }

    /**
     * Genere un item de loot aleatoire
     */
    private ItemStack generateLootItem() {
        // Utiliser le systeme de loot du plugin
        try {
            // Determiner la rarete
            double roll = random.nextDouble() * 100;
            Rarity rarity;

            if (roll < 60) {
                rarity = Rarity.COMMON;
            } else if (roll < 85) {
                rarity = Rarity.UNCOMMON;
            } else if (roll < 95) {
                rarity = Rarity.RARE;
            } else if (roll < 99) {
                rarity = Rarity.EPIC;
            } else {
                rarity = Rarity.LEGENDARY;
            }

            // Generer l'item via l'ItemManager
            return plugin.getItemManager().generateItem(zone.getId(), rarity);
        } catch (Exception e) {
            // Fallback: item basique
            Material[] fallbacks = {
                Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.IRON_SWORD, Material.IRON_AXE
            };
            return new ItemStack(fallbacks[random.nextInt(fallbacks.length)]);
        }
    }
}
