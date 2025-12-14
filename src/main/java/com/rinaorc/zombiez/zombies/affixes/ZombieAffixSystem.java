package com.rinaorc.zombiez.zombies.affixes;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Système d'affixes pour les zombies
 * Chaque affixe modifie le comportement et les stats du zombie
 * Les affixes apparaissent dès la zone 1 (rare) pour montrer le système
 */
public class ZombieAffixSystem {

    // Tous les affixes disponibles
    private static final Map<String, ZombieAffix> AFFIXES = new LinkedHashMap<>();
    
    // Chance d'affixe par zone (augmentée pour zone 1-2)
    private static final double[] ZONE_AFFIX_CHANCE = {
        0.00,  // Zone 0 (spawn)
        0.03,  // Zone 1 - 3% (était 0%)
        0.05,  // Zone 2 - 5% (était 0%)
        0.08,  // Zone 3 - 8% (était 0%)
        0.12,  // Zone 4
        0.15,  // Zone 5
        0.20,  // Zone 6 (PvP)
        0.18,  // Zone 7
        0.22,  // Zone 8
        0.28,  // Zone 9
        0.35,  // Zone 10
        0.45   // Zone 11
    };
    
    // Chance de double affixe (très rare)
    private static final double[] ZONE_DOUBLE_AFFIX_CHANCE = {
        0.00, 0.00, 0.00, 0.00, 0.02, 0.03, 0.05, 0.05, 0.08, 0.10, 0.15, 0.25
    };

    static {
        initializeAffixes();
    }

    /**
     * Initialise tous les affixes disponibles
     */
    private static void initializeAffixes() {
        // ========== TIER 1 - Affixes basiques (Zones 1-3) ==========
        
        AFFIXES.put("swift", ZombieAffix.builder()
            .id("swift")
            .name("Rapide")
            .displayName("§b⚡ Rapide")
            .description("Se déplace 30% plus vite")
            .tier(1)
            .minZone(1)
            .color(Color.AQUA)
            .particle(Particle.ELECTRIC_SPARK)
            .rewardMultiplier(1.2)
            .onApply((zombie, level) -> {
                var attr = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
                if (attr != null) attr.setBaseValue(attr.getBaseValue() * 1.3);
            })
            .build());
        
        AFFIXES.put("tough", ZombieAffix.builder()
            .id("tough")
            .name("Résistant")
            .displayName("§7🛡 Résistant")
            .description("25% de vie en plus")
            .tier(1)
            .minZone(1)
            .color(Color.GRAY)
            .particle(Particle.CRIT)
            .rewardMultiplier(1.15)
            .onApply((zombie, level) -> {
                var attr = zombie.getAttribute(Attribute.MAX_HEALTH);
                if (attr != null) {
                    attr.setBaseValue(attr.getBaseValue() * 1.25);
                    zombie.setHealth(attr.getBaseValue());
                }
            })
            .build());
        
        AFFIXES.put("hungry", ZombieAffix.builder()
            .id("hungry")
            .name("Affamé")
            .displayName("§c🍖 Affamé")
            .description("Inflige la faim au contact")
            .tier(1)
            .minZone(1)
            .color(Color.MAROON)
            .particle(Particle.EGG_CRACK)
            .rewardMultiplier(1.1)
            .onHit((zombie, player) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1));
            })
            .build());
        
        // ========== TIER 2 - Affixes intermédiaires (Zones 3-5) ==========
        
        AFFIXES.put("burning", ZombieAffix.builder()
            .id("burning")
            .name("Brûlant")
            .displayName("§6🔥 Brûlant")
            .description("Enflamme les joueurs au contact")
            .tier(2)
            .minZone(3)
            .color(Color.ORANGE)
            .particle(Particle.FLAME)
            .rewardMultiplier(1.25)
            .onHit((zombie, player) -> {
                player.setFireTicks(60); // 3 secondes de feu
            })
            .onTick((zombie, ticks) -> {
                if (ticks % 10 == 0) {
                    zombie.getWorld().spawnParticle(Particle.FLAME, 
                        zombie.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.02);
                }
            })
            .build());
        
        AFFIXES.put("frozen", ZombieAffix.builder()
            .id("frozen")
            .name("Gelé")
            .displayName("§b❄ Gelé")
            .description("Ralentit les joueurs au contact")
            .tier(2)
            .minZone(3)
            .color(Color.fromRGB(150, 200, 255))
            .particle(Particle.SNOWFLAKE)
            .rewardMultiplier(1.25)
            .onHit((zombie, player) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                player.setFreezeTicks(Math.min(player.getFreezeTicks() + 40, 140));
            })
            .onTick((zombie, ticks) -> {
                if (ticks % 10 == 0) {
                    zombie.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                        zombie.getLocation().add(0, 1, 0), 5, 0.3, 0.4, 0.3, 0.01);
                }
            })
            .build());
        
        AFFIXES.put("poisonous", ZombieAffix.builder()
            .id("poisonous")
            .name("Venimeux")
            .displayName("§a☠ Venimeux")
            .description("Empoisonne au contact")
            .tier(2)
            .minZone(3)
            .color(Color.GREEN)
            .particle(Particle.ENTITY_EFFECT)
            .rewardMultiplier(1.3)
            .onHit((zombie, player) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));
            })
            .build());
        
        AFFIXES.put("thorns", ZombieAffix.builder()
            .id("thorns")
            .name("Épineux")
            .displayName("§4🌵 Épineux")
            .description("Renvoie 15% des dégâts")
            .tier(2)
            .minZone(4)
            .color(Color.fromRGB(139, 69, 19))
            .particle(Particle.DAMAGE_INDICATOR)
            .rewardMultiplier(1.2)
            .onDamaged((zombie, player, damage) -> {
                player.damage(damage * 0.15, zombie);
            })
            .build());
        
        // ========== TIER 3 - Affixes dangereux (Zones 5-7) ==========
        
        AFFIXES.put("wither", ZombieAffix.builder()
            .id("wither")
            .name("Flétrisseur")
            .displayName("§8💀 Flétrisseur")
            .description("Inflige le wither au contact")
            .tier(3)
            .minZone(5)
            .color(Color.BLACK)
            .particle(Particle.SMOKE)
            .rewardMultiplier(1.4)
            .onHit((zombie, player) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            })
            .onTick((zombie, ticks) -> {
                if (ticks % 5 == 0) {
                    zombie.getWorld().spawnParticle(Particle.SMOKE, 
                        zombie.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
                }
            })
            .build());
        
        AFFIXES.put("vampiric", ZombieAffix.builder()
            .id("vampiric")
            .name("Vampirique")
            .displayName("§4🩸 Vampirique")
            .description("Se soigne en frappant")
            .tier(3)
            .minZone(5)
            .color(Color.fromRGB(139, 0, 0))
            .particle(Particle.DAMAGE_INDICATOR)
            .rewardMultiplier(1.35)
            .onHit((zombie, player) -> {
                double heal = Math.min(4, zombie.getAttribute(Attribute.MAX_HEALTH).getBaseValue() - zombie.getHealth());
                zombie.setHealth(zombie.getHealth() + heal);
                zombie.getWorld().spawnParticle(Particle.HEART, zombie.getLocation().add(0, 2, 0), 3);
            })
            .build());
        
        AFFIXES.put("teleporter", ZombieAffix.builder()
            .id("teleporter")
            .name("Téléporteur")
            .displayName("§5✦ Téléporteur")
            .description("Se téléporte parfois derrière vous")
            .tier(3)
            .minZone(6)
            .color(Color.PURPLE)
            .particle(Particle.PORTAL)
            .rewardMultiplier(1.45)
            .onTick((zombie, ticks) -> {
                if (ticks % 100 == 0 && Math.random() < 0.3) { // 30% chance toutes les 5s
                    LivingEntity target = zombie.getTarget();
                    if (target instanceof Player player && player.getLocation().distance(zombie.getLocation()) < 15) {
                        // Téléporter derrière le joueur
                        var loc = player.getLocation().clone();
                        loc.add(loc.getDirection().multiply(-2));
                        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
                        
                        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        zombie.getWorld().spawnParticle(Particle.PORTAL, zombie.getLocation(), 30, 0.5, 1, 0.5);
                        zombie.teleport(loc);
                        zombie.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 0.5, 1, 0.5);
                        zombie.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                    }
                }
            })
            .build());
        
        AFFIXES.put("shielded", ZombieAffix.builder()
            .id("shielded")
            .name("Bouclier")
            .displayName("§e🛡 Bouclier")
            .description("Immunisé aux dégâts toutes les 5 hits")
            .tier(3)
            .minZone(6)
            .color(Color.YELLOW)
            .particle(Particle.END_ROD)
            .rewardMultiplier(1.5)
            .onApply((zombie, level) -> {
                zombie.setMetadata("affix_shield_charges", 
                    new org.bukkit.metadata.FixedMetadataValue(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), 5));
            })
            .onDamaged((zombie, player, damage) -> {
                var meta = zombie.getMetadata("affix_shield_charges");
                if (!meta.isEmpty()) {
                    int charges = meta.get(0).asInt();
                    if (charges <= 0) {
                        // Bouclier actif!
                        zombie.getWorld().spawnParticle(Particle.END_ROD, zombie.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                        zombie.getWorld().playSound(zombie.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
                        zombie.setMetadata("affix_shield_charges", 
                            new org.bukkit.metadata.FixedMetadataValue(
                                org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), 5));
                        zombie.setNoDamageTicks(10);
                    } else {
                        zombie.setMetadata("affix_shield_charges", 
                            new org.bukkit.metadata.FixedMetadataValue(
                                org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), charges - 1));
                    }
                }
            })
            .build());
        
        // ========== TIER 4 - Affixes élites (Zones 8-10) ==========
        
        AFFIXES.put("juggernaut", ZombieAffix.builder()
            .id("juggernaut")
            .name("Juggernaut")
            .displayName("§4§l⚔ Juggernaut")
            .description("Double vie, 50% résistance, lent")
            .tier(4)
            .minZone(8)
            .color(Color.fromRGB(100, 0, 0))
            .particle(Particle.ANGRY_VILLAGER)
            .rewardMultiplier(2.0)
            .onApply((zombie, level) -> {
                var health = zombie.getAttribute(Attribute.MAX_HEALTH);
                var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
                var armor = zombie.getAttribute(Attribute.ARMOR);
                
                if (health != null) {
                    health.setBaseValue(health.getBaseValue() * 2);
                    zombie.setHealth(health.getBaseValue());
                }
                if (speed != null) speed.setBaseValue(speed.getBaseValue() * 0.6);
                if (armor != null) armor.setBaseValue(armor.getBaseValue() + 10);
            })
            .build());
        
        AFFIXES.put("berserker", ZombieAffix.builder()
            .id("berserker")
            .name("Berserker")
            .displayName("§c§l🔱 Berserker")
            .description("Plus rapide et fort quand blessé")
            .tier(4)
            .minZone(8)
            .color(Color.RED)
            .particle(Particle.ANGRY_VILLAGER)
            .rewardMultiplier(1.8)
            .onTick((zombie, ticks) -> {
                if (ticks % 20 == 0) {
                    double healthPercent = zombie.getHealth() / zombie.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                    if (healthPercent < 0.5) {
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false));
                        zombie.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 2, 0), 3);
                    }
                }
            })
            .build());
        
        AFFIXES.put("necromancer", ZombieAffix.builder()
            .id("necromancer")
            .name("Nécromancien")
            .displayName("§5§l☠ Nécromancien")
            .description("Invoque des mini-zombies à sa mort")
            .tier(4)
            .minZone(9)
            .color(Color.fromRGB(75, 0, 130))
            .particle(Particle.SOUL)
            .rewardMultiplier(1.7)
            .onDeath((zombie, killer) -> {
                for (int i = 0; i < 3; i++) {
                    var loc = zombie.getLocation().clone().add(
                        (Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3);
                    var minion = zombie.getWorld().spawn(loc, Zombie.class);
                    minion.setBaby(true);
                    minion.setCustomName("§7Sbire");
                    minion.getAttribute(Attribute.MAX_HEALTH).setBaseValue(10);
                    minion.setHealth(10);
                    
                    zombie.getWorld().spawnParticle(Particle.SOUL, loc, 10);
                }
                zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            })
            .build());
        
        // ========== TIER 5 - Affixes légendaires (Zone 10-11) ==========
        
        AFFIXES.put("arcane", ZombieAffix.builder()
            .id("arcane")
            .name("Arcanique")
            .displayName("§d§l✧ Arcanique")
            .description("Lance des projectiles magiques")
            .tier(5)
            .minZone(10)
            .color(Color.FUCHSIA)
            .particle(Particle.WITCH)
            .rewardMultiplier(2.5)
            .onTick((zombie, ticks) -> {
                if (ticks % 60 == 0) { // Toutes les 3 secondes
                    LivingEntity target = zombie.getTarget();
                    if (target instanceof Player player && zombie.getLocation().distance(player.getLocation()) < 12) {
                        // Lancer un projectile magique
                        var loc = zombie.getEyeLocation();
                        var direction = player.getLocation().add(0, 1, 0).subtract(loc).toVector().normalize();
                        
                        zombie.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1.2f);
                        
                        // Simuler le projectile avec des particules
                        var currentLoc = loc.clone();
                        for (int i = 0; i < 15; i++) {
                            currentLoc.add(direction.clone().multiply(0.8));
                            zombie.getWorld().spawnParticle(Particle.WITCH, currentLoc, 5, 0.1, 0.1, 0.1, 0);
                            
                            if (currentLoc.distance(player.getLocation().add(0, 1, 0)) < 1.5) {
                                player.damage(6, zombie);
                                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                                zombie.getWorld().spawnParticle(Particle.WITCH, currentLoc, 30, 0.5, 0.5, 0.5, 0.1);
                                break;
                            }
                        }
                    }
                }
            })
            .build());
        
        AFFIXES.put("void", ZombieAffix.builder()
            .id("void")
            .name("Void")
            .displayName("§0§l◆ Void")
            .description("Crée des zones de dégâts au sol")
            .tier(5)
            .minZone(11)
            .color(Color.BLACK)
            .particle(Particle.REVERSE_PORTAL)
            .rewardMultiplier(3.0)
            .onTick((zombie, ticks) -> {
                if (ticks % 80 == 0 && Math.random() < 0.5) {
                    LivingEntity target = zombie.getTarget();
                    if (target instanceof Player player) {
                        var loc = player.getLocation().clone();
                        zombie.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.5f);
                        
                        // Avertissement
                        for (int i = 0; i < 20; i++) {
                            double angle = (2 * Math.PI / 20) * i;
                            zombie.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                                loc.clone().add(Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2), 1);
                        }
                        
                        // Dégâts après délai (scheduler needed in real implementation)
                        // Pour simplifier, dégâts immédiats légers
                        if (player.getLocation().distance(loc) < 2.5) {
                            player.damage(4, zombie);
                        }
                    }
                }
            })
            .build());
    }

    /**
     * Détermine si un zombie doit avoir un affixe
     */
    public static boolean shouldHaveAffix(int zoneId) {
        if (zoneId < 0 || zoneId >= ZONE_AFFIX_CHANCE.length) return false;
        return Math.random() < ZONE_AFFIX_CHANCE[zoneId];
    }

    /**
     * Détermine si un zombie doit avoir un second affixe
     */
    public static boolean shouldHaveDoubleAffix(int zoneId) {
        if (zoneId < 0 || zoneId >= ZONE_DOUBLE_AFFIX_CHANCE.length) return false;
        return Math.random() < ZONE_DOUBLE_AFFIX_CHANCE[zoneId];
    }

    /**
     * Sélectionne un affixe aléatoire pour une zone donnée
     */
    public static ZombieAffix selectAffixForZone(int zoneId) {
        List<ZombieAffix> validAffixes = AFFIXES.values().stream()
            .filter(a -> a.getMinZone() <= zoneId)
            .toList();
        
        if (validAffixes.isEmpty()) return null;
        
        // Poids par tier (les tiers plus élevés sont plus rares)
        List<ZombieAffix> weighted = new ArrayList<>();
        for (ZombieAffix affix : validAffixes) {
            int weight = switch (affix.getTier()) {
                case 1 -> 100;
                case 2 -> 60;
                case 3 -> 30;
                case 4 -> 12;
                case 5 -> 4;
                default -> 50;
            };
            for (int i = 0; i < weight; i++) {
                weighted.add(affix);
            }
        }
        
        return weighted.get(new Random().nextInt(weighted.size()));
    }

    /**
     * Obtient un affixe par son ID
     */
    public static ZombieAffix getAffix(String id) {
        return AFFIXES.get(id);
    }

    /**
     * Obtient tous les affixes
     */
    public static Collection<ZombieAffix> getAllAffixes() {
        return AFFIXES.values();
    }

    // ==================== INNER CLASS ====================

    @Getter
    @lombok.Builder
    public static class ZombieAffix {
        private final String id;
        private final String name;
        private final String displayName;
        private final String description;
        private final int tier;
        private final int minZone;
        private final Color color;
        private final Particle particle;
        private final double rewardMultiplier;
        
        // Callbacks
        @lombok.Builder.Default
        private final BiConsumer<Zombie, Integer> onApply = (z, l) -> {};
        @lombok.Builder.Default
        private final BiConsumer<Zombie, Player> onHit = (z, p) -> {};
        @lombok.Builder.Default
        private final TriConsumer<Zombie, Player, Double> onDamaged = (z, p, d) -> {};
        @lombok.Builder.Default
        private final BiConsumer<Zombie, Player> onDeath = (z, p) -> {};
        @lombok.Builder.Default
        private final BiConsumer<Zombie, Integer> onTick = (z, t) -> {};
    }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
