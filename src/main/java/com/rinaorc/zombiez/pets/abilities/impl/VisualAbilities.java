package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Abilities avec effets visuels spectaculaires
 * Ces abilities utilisent des particules et effets pour créer des attaques impressionnantes
 */

// ==================== PHÉNIX SOLAIRE - Boules de Feu ====================

@Getter
class FireballPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double fireballChance;
    private final double fireballDamage;
    private final Map<UUID, Long> lastFireball = new HashMap<>();

    public FireballPassive(String id, String name, String desc, double chance, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.fireballChance = chance;
        this.fireballDamage = damage;
        PassiveAbilityCleanup.registerForCleanup(lastFireball);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (now - lastFireball.getOrDefault(uuid, 0L) < 2000) return damage;
        if (Math.random() > fireballChance * petData.getStatMultiplier()) return damage;

        lastFireball.put(uuid, now);

        // Lancer une boule de feu visuelle
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double finalDamage = fireballDamage * petData.getStatMultiplier();

        new BukkitRunnable() {
            Location current = start.clone();
            int distance = 0;

            @Override
            public void run() {
                if (distance > 20) {
                    explodeFireball(current, player, finalDamage, petData);
                    cancel();
                    return;
                }

                // Déplacer la boule de feu
                current.add(direction.clone().multiply(1.5));

                // Particules de la boule de feu
                current.getWorld().spawnParticle(Particle.FLAME, current, 15, 0.2, 0.2, 0.2, 0.02);
                current.getWorld().spawnParticle(Particle.LAVA, current, 3, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 5, 0.1, 0.1, 0.1, 0.02);

                // Vérifier collision
                for (Entity e : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (e instanceof Monster) {
                        explodeFireball(current, player, finalDamage, petData);
                        cancel();
                        return;
                    }
                }

                // Son de vol
                if (distance % 3 == 0) {
                    current.getWorld().playSound(current, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.5f);
                }

                distance++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return damage;
    }

    private void explodeFireball(Location loc, Player player, double damage, PetData petData) {
        // Explosion visuelle
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 2, 2, 2, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 1.5, 1.5, 1.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Dégâts
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof Monster m) {
                m.damage(damage, player);
                m.setFireTicks(100);
                petData.addDamage((long) damage);
            }
        }
    }
}

@Getter
class MeteorShowerActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerMeteor;
    private final int meteorCount;

    public MeteorShowerActive(String id, String name, String desc, double damage, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerMeteor = damage;
        this.meteorCount = count;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double damage = damagePerMeteor * petData.getStatMultiplier();
        int count = (int) (meteorCount * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §6§l☄ PLUIE DE MÉTÉORES!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);

        // Spawner les météores
        new BukkitRunnable() {
            int spawned = 0;
            Random random = new Random();

            @Override
            public void run() {
                if (spawned >= count) {
                    cancel();
                    return;
                }

                // Position aléatoire autour du joueur
                double offsetX = (random.nextDouble() - 0.5) * 16;
                double offsetZ = (random.nextDouble() - 0.5) * 16;
                Location target = center.clone().add(offsetX, 0, offsetZ);
                Location start = target.clone().add(0, 25, 0);

                // Animation du météore
                spawnMeteor(start, target, player, damage, petData);

                spawned++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 4L);

        return true;
    }

    private void spawnMeteor(Location start, Location target, Player player, double damage, PetData petData) {
        Vector direction = target.toVector().subtract(start.toVector()).normalize().multiply(2);

        new BukkitRunnable() {
            Location current = start.clone();

            @Override
            public void run() {
                if (current.getY() <= target.getY() + 1) {
                    // Impact!
                    current.getWorld().spawnParticle(Particle.EXPLOSION, current, 5, 1, 0.5, 1, 0);
                    current.getWorld().spawnParticle(Particle.FLAME, current, 80, 3, 0.5, 3, 0.15);
                    current.getWorld().spawnParticle(Particle.LAVA, current, 30, 2, 0.3, 2, 0);
                    current.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current, 20, 2, 0.5, 2, 0.05);
                    current.getWorld().playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                    // Dégâts de zone
                    for (Entity e : current.getWorld().getNearbyEntities(current, 4, 4, 4)) {
                        if (e instanceof Monster m) {
                            m.damage(damage, player);
                            m.setFireTicks(80);
                            petData.addDamage((long) damage);

                            // Knockback
                            Vector kb = e.getLocation().toVector().subtract(current.toVector()).normalize().multiply(0.8).setY(0.4);
                            e.setVelocity(kb);
                        }
                    }

                    cancel();
                    return;
                }

                // Traînée du météore
                current.add(direction);
                current.getWorld().spawnParticle(Particle.FLAME, current, 20, 0.3, 0.3, 0.3, 0.05);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 10, 0.2, 0.2, 0.2, 0.02);
                current.getWorld().spawnParticle(Particle.LAVA, current, 5, 0.2, 0.2, 0.2, 0);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== HYDRE DE GIVRE - Pluie de Glace ====================

@Getter
class IceShardPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double shardChance;
    private final double slowDuration;

    public IceShardPassive(String id, String name, String desc, double chance, double duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.shardChance = chance;
        this.slowDuration = duration;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        if (Math.random() > shardChance * petData.getStatMultiplier()) return damage;

        // Lancer un éclat de glace
        Location start = player.getEyeLocation();
        Location end = target.getLocation().add(0, 1, 0);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();

        // Traînée de glace
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 15 || current.distance(end) < 1) {
                    // Impact glacé
                    current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 30, 0.5, 0.5, 0.5, 0.1);
                    current.getWorld().spawnParticle(Particle.BLOCK, current, 20, 0.3, 0.3, 0.3, Material.ICE.createBlockData());
                    current.getWorld().playSound(current, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

                    target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, (int)(slowDuration * 20 * petData.getStatMultiplier()), 2, false, true));
                    target.setFreezeTicks(60);

                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.2));
                current.getWorld().spawnParticle(Particle.SNOWFLAKE, current, 8, 0.1, 0.1, 0.1, 0.02);
                current.getWorld().spawnParticle(Particle.END_ROD, current, 3, 0.05, 0.05, 0.05, 0);
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return damage * 1.1; // Bonus de dégâts de glace
    }
}

@Getter
class BlizzardActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerSecond;
    private final int durationSeconds;
    private final int radius;

    public BlizzardActive(String id, String name, String desc, double dps, int duration, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerSecond = dps;
        this.durationSeconds = duration;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double dps = damagePerSecond * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §b§l❄ BLIZZARD DÉCHAÎNÉ!");
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);

        new BukkitRunnable() {
            int ticks = 0;
            Random random = new Random();

            @Override
            public void run() {
                if (ticks >= durationSeconds * 20) {
                    cancel();
                    return;
                }

                // Particules de tempête de neige
                for (int i = 0; i < 30; i++) {
                    double x = (random.nextDouble() - 0.5) * adjustedRadius * 2;
                    double y = random.nextDouble() * 8;
                    double z = (random.nextDouble() - 0.5) * adjustedRadius * 2;
                    Location particleLoc = center.clone().add(x, y, z);

                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.2, 0.2, 0.2, 0.05);
                    if (random.nextDouble() < 0.3) {
                        center.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 1, 0, 0, 0, Material.SNOW_BLOCK.createBlockData());
                    }
                }

                // Effet de vent tourbillonnant
                double angle = ticks * 0.2;
                for (int ring = 0; ring < 3; ring++) {
                    double ringRadius = adjustedRadius * (0.3 + ring * 0.3);
                    for (int j = 0; j < 8; j++) {
                        double a = angle + j * Math.PI / 4 + ring * Math.PI / 6;
                        Location ringLoc = center.clone().add(Math.cos(a) * ringRadius, 0.5 + ring, Math.sin(a) * ringRadius);
                        center.getWorld().spawnParticle(Particle.SNOWFLAKE, ringLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    }
                }

                // Dégâts et gel (toutes les 20 ticks)
                if (ticks % 20 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.5f, 0.5f);
                    for (Entity e : center.getWorld().getNearbyEntities(center, adjustedRadius, 6, adjustedRadius)) {
                        if (e instanceof Monster m) {
                            m.damage(dps, player);
                            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
                            m.setFreezeTicks(Math.min(m.getFreezeTicks() + 40, 300));
                            petData.addDamage((long) dps);

                            // Particules de gel sur l'ennemi
                            m.getWorld().spawnParticle(Particle.SNOWFLAKE, m.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                        }
                    }
                }

                // Son ambiant
                if (ticks % 10 == 0) {
                    center.getWorld().playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }
}

// ==================== MOUTON ARC-EN-CIEL - Spectre Chromatique ====================

@Getter
class ChromaticSpectrumPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int colorCycleMs;                  // 3000ms = 3s entre chaque couleur
    private final double baseDamageBonus;            // 15% bonus dégâts (rouge)
    private final double baseLifestealPercent;       // 5% lifesteal (vert)

    // Couleurs: 0=Rouge, 1=Orange, 2=Jaune, 3=Vert, 4=Bleu, 5=Violet
    private final Map<UUID, Integer> currentColor = new HashMap<>();
    private final Map<UUID, Long> lastColorChange = new HashMap<>();
    private final Map<UUID, Boolean> allBuffsActive = new HashMap<>(); // Pour l'ultimate

    // DyeColor correspondants
    private static final org.bukkit.DyeColor[] RAINBOW_COLORS = {
        org.bukkit.DyeColor.RED,
        org.bukkit.DyeColor.ORANGE,
        org.bukkit.DyeColor.YELLOW,
        org.bukkit.DyeColor.LIME,
        org.bukkit.DyeColor.LIGHT_BLUE,
        org.bukkit.DyeColor.PURPLE
    };

    public ChromaticSpectrumPassive(String id, String name, String desc, int cycleMs,
                                     double dmgBonus, double lifesteal) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.colorCycleMs = cycleMs;
        this.baseDamageBonus = dmgBonus;
        this.baseLifestealPercent = lifesteal;
        PassiveAbilityCleanup.registerForCleanup(currentColor);
        PassiveAbilityCleanup.registerForCleanup(lastColorChange);
        PassiveAbilityCleanup.registerForCleanup(allBuffsActive);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastChange = lastColorChange.getOrDefault(uuid, 0L);

        // Si tous les buffs sont actifs (ultimate), ne pas cycler
        if (allBuffsActive.getOrDefault(uuid, false)) {
            return;
        }

        // Ajuster le cycle par niveau (plus rapide à haut niveau)
        int adjustedCycle = (int) (colorCycleMs - (petData.getStatMultiplier() - 1) * 300);
        adjustedCycle = Math.max(adjustedCycle, 1500); // Minimum 1.5s

        // Cycler les couleurs
        if (now - lastChange > adjustedCycle) {
            int current = currentColor.getOrDefault(uuid, 0);
            int newColor = (current + 1) % 6;
            currentColor.put(uuid, newColor);
            lastColorChange.put(uuid, now);

            // Changer la couleur du mouton
            updateSheepColor(player, newColor);

            // Message avec l'effet actif
            String colorInfo = getColorInfo(newColor, petData);
            player.sendMessage("§a[Pet] " + colorInfo);
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int color = currentColor.getOrDefault(uuid, 0);
        boolean allBuffs = allBuffsActive.getOrDefault(uuid, false);
        World world = target.getWorld();

        double bonusDamage = 0;
        double adjustedBonus = baseDamageBonus + (petData.getStatMultiplier() - 1) * 0.05;
        double adjustedLifesteal = baseLifestealPercent + (petData.getStatMultiplier() - 1) * 0.02;

        // Si l'ultimate est actif, appliquer tous les effets
        if (allBuffs) {
            // Rouge: +15% dégâts
            bonusDamage += damage * adjustedBonus;

            // Jaune: +10% crit (simulé par dégâts bonus aléatoire)
            if (Math.random() < 0.10 + (petData.getStatMultiplier() - 1) * 0.03) {
                bonusDamage += damage * 0.5; // Crit!
                world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            }

            // Vert: 5% lifesteal
            double heal = damage * adjustedLifesteal;
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));

            // Violet: Slow
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));

            // Particules arc-en-ciel
            spawnRainbowParticles(target.getLocation().add(0, 1, 0), world);

        } else {
            // Appliquer seulement l'effet de la couleur actuelle
            switch (color) {
                case 0 -> { // Rouge: +15% dégâts
                    bonusDamage = damage * adjustedBonus;
                    world.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.02);
                }
                case 1 -> { // Orange: Vitesse d'attaque (effet visuel + petit bonus)
                    bonusDamage = damage * 0.08;
                    world.spawnParticle(Particle.LAVA, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                }
                case 2 -> { // Jaune: Crit chance +10%
                    if (Math.random() < 0.10 + (petData.getStatMultiplier() - 1) * 0.03) {
                        bonusDamage = damage * 0.5;
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
                    }
                }
                case 3 -> { // Vert: 5% lifesteal
                    double heal = damage * adjustedLifesteal;
                    player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                    world.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
                }
                case 4 -> { // Bleu: (défense gérée dans onDamageReceived)
                    world.spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.02);
                }
                case 5 -> { // Violet: Slow ennemi
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
                    world.spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        return damage + bonusDamage;
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        UUID uuid = player.getUniqueId();
        int color = currentColor.getOrDefault(uuid, 0);
        boolean allBuffs = allBuffsActive.getOrDefault(uuid, false);

        // Bleu: -10% dégâts reçus (appliqué via effet de particules ici, réduction réelle ailleurs)
        if (color == 4 || allBuffs) {
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0),
                10, 0.4, 0.4, 0.4, 0.02);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 1.8f);
        }
    }

    /**
     * Retourne le pourcentage de réduction de dégâts si couleur bleue active
     */
    public double getDamageReduction(UUID uuid, double petMultiplier) {
        int color = currentColor.getOrDefault(uuid, 0);
        boolean allBuffs = allBuffsActive.getOrDefault(uuid, false);

        if (color == 4 || allBuffs) {
            return 0.10 + (petMultiplier - 1) * 0.03; // 10% + bonus niveau
        }
        return 0;
    }

    private void updateSheepColor(Player player, int colorIndex) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(20, 10, 20)) {
            if (entity instanceof org.bukkit.entity.Sheep sheep
                && entity.getScoreboardTags().contains(ownerTag)) {

                sheep.setColor(RAINBOW_COLORS[colorIndex]);

                // Particules de transition
                Particle particle = getColorParticle(colorIndex);
                sheep.getWorld().spawnParticle(particle, sheep.getLocation().add(0, 0.5, 0),
                    15, 0.3, 0.3, 0.3, 0.05);

                break;
            }
        }
    }

    private String getColorInfo(int colorIndex, PetData petData) {
        double mult = petData.getStatMultiplier();
        return switch (colorIndex) {
            case 0 -> "§c🔴 ROUGE §7- §c+" + (int)((baseDamageBonus + (mult-1)*0.05) * 100) + "% dégâts";
            case 1 -> "§6🟠 ORANGE §7- §6+20% vitesse attaque";
            case 2 -> "§e🟡 JAUNE §7- §e+" + (int)((0.10 + (mult-1)*0.03) * 100) + "% crit";
            case 3 -> "§a🟢 VERT §7- §a" + (int)((baseLifestealPercent + (mult-1)*0.02) * 100) + "% lifesteal";
            case 4 -> "§b🔵 BLEU §7- §b-" + (int)((0.10 + (mult-1)*0.03) * 100) + "% dégâts reçus";
            case 5 -> "§d🟣 VIOLET §7- §dSlow les ennemis";
            default -> "§7?";
        };
    }

    private Particle getColorParticle(int colorIndex) {
        return switch (colorIndex) {
            case 0 -> Particle.FLAME;
            case 1 -> Particle.LAVA;
            case 2 -> Particle.CRIT;
            case 3 -> Particle.HAPPY_VILLAGER;
            case 4 -> Particle.SNOWFLAKE;
            case 5 -> Particle.WITCH;
            default -> Particle.CRIT;
        };
    }

    private void spawnRainbowParticles(Location loc, World world) {
        world.spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.CRIT, loc, 2, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.2, 0.2, 0.2, 0.02);
        world.spawnParticle(Particle.WITCH, loc, 2, 0.2, 0.2, 0.2, 0.02);
    }

    public void setAllBuffsActive(UUID uuid, boolean active) {
        allBuffsActive.put(uuid, active);
    }

    public int getCurrentColor(UUID uuid) {
        return currentColor.getOrDefault(uuid, 0);
    }
}

@Getter
class PrismaticNovaActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double damagePercent;              // % dégâts joueur
    private final int buffDurationTicks;             // 6s = 120 ticks
    private final ChromaticSpectrumPassive spectrumPassive;

    public PrismaticNovaActive(String id, String name, String desc, int cd,
                                double dmgPercent, int buffDuration, ChromaticSpectrumPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.damagePercent = dmgPercent;
        this.buffDurationTicks = buffDuration;
        this.spectrumPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        UUID uuid = player.getUniqueId();

        // Vérifier qu'il y a des ennemis
        List<Monster> targets = player.getNearbyEntities(10, 6, 10).stream()
            .filter(e -> e instanceof Monster)
            .map(e -> (Monster) e)
            .toList();

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à proximité!");
            return false;
        }

        // Calculer les dégâts
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.20;
        double novaDamage = playerDamage * adjustedPercent;

        // Ajuster la durée des buffs
        int adjustedDuration = (int) (buffDurationTicks + (petData.getStatMultiplier() - 1) * 40);

        // Activer tous les buffs
        spectrumPassive.setAllBuffsActive(uuid, true);

        // Faire cycler rapidement les couleurs du mouton pendant l'explosion
        cycleSheepColorsRapidly(player, 30); // 1.5 secondes d'animation

        // Effet de préparation
        world.playSound(playerLoc, Sound.ENTITY_SHEEP_AMBIENT, 2.0f, 0.5f);
        world.playSound(playerLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        player.sendMessage("§a[Pet] §d§l🌈 NOVA PRISMATIQUE! §7Tous les bonus actifs pendant " + (adjustedDuration / 20) + "s!");

        // Explosion arc-en-ciel avec anneaux expansifs
        for (int ring = 1; ring <= 4; ring++) {
            final int currentRing = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> spawnRainbowRing(world, playerLoc, currentRing * 2.5),
                ring * 4L
            );
        }

        // Infliger les dégâts aux ennemis
        for (Monster target : targets) {
            target.damage(novaDamage, player);
            petData.addDamage((long) novaDamage);

            // Appliquer tous les effets
            target.setFireTicks(40); // Rouge - feu
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false)); // Violet - slow

            // Lifesteal sur chaque cible
            double heal = novaDamage * 0.05;
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));

            // Particules sur la cible
            Location targetLoc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 20, 0.5, 0.5, 0.5, 0.2);
        }

        // Appliquer les buffs au joueur
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, adjustedDuration, 1, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, adjustedDuration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, adjustedDuration, 0, false, true));

        // Son d'explosion
        world.playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        world.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

        // Désactiver les buffs après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                spectrumPassive.setAllBuffsActive(uuid, false);
                player.sendMessage("§a[Pet] §7Les bonus arc-en-ciel se dissipent...");
                world.playSound(playerLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
            },
            adjustedDuration
        );

        return true;
    }

    private void cycleSheepColorsRapidly(Player player, int durationTicks) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        new BukkitRunnable() {
            int ticks = 0;
            int colorIndex = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                // Changer la couleur toutes les 2 ticks
                if (ticks % 2 == 0) {
                    for (Entity entity : player.getNearbyEntities(20, 10, 20)) {
                        if (entity instanceof org.bukkit.entity.Sheep sheep
                            && entity.getScoreboardTags().contains(ownerTag)) {

                            org.bukkit.DyeColor[] colors = {
                                org.bukkit.DyeColor.RED, org.bukkit.DyeColor.ORANGE,
                                org.bukkit.DyeColor.YELLOW, org.bukkit.DyeColor.LIME,
                                org.bukkit.DyeColor.LIGHT_BLUE, org.bukkit.DyeColor.PURPLE
                            };
                            sheep.setColor(colors[colorIndex % 6]);
                            colorIndex++;
                            break;
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    private void spawnRainbowRing(World world, Location center, double radius) {
        org.bukkit.Color[] colors = {
            org.bukkit.Color.RED, org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW,
            org.bukkit.Color.LIME, org.bukkit.Color.AQUA, org.bukkit.Color.PURPLE
        };

        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            Location ringLoc = center.clone().add(
                Math.cos(rad) * radius,
                0.5,
                Math.sin(rad) * radius
            );

            // Alterner les particules colorées
            int colorIndex = (angle / 60) % 6;
            Particle particle = switch (colorIndex) {
                case 0 -> Particle.FLAME;
                case 1 -> Particle.LAVA;
                case 2 -> Particle.CRIT;
                case 3 -> Particle.HAPPY_VILLAGER;
                case 4 -> Particle.SNOWFLAKE;
                case 5 -> Particle.WITCH;
                default -> Particle.CRIT;
            };

            world.spawnParticle(particle, ringLoc, 3, 0.1, 0.1, 0.1, 0.01);
        }

        world.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f + (float)(radius * 0.1));
    }
}

// ==================== RENARD DES NEIGES - Morsure Glaciale ====================

@Getter
class FrostBitePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int maxStacks;                           // 5 stacks pour geler
    private final double freezeDuration;                   // 1.5s de gel
    private final double frozenDamageBonus;                // +30% dégâts sur gelé

    // Tracking des stacks par ennemi (entityUUID -> stacks)
    private final Map<UUID, Map<UUID, Integer>> frostStacks = new HashMap<>();
    // Tracking des ennemis gelés (entityUUID -> temps de fin du gel)
    private final Map<UUID, Map<UUID, Long>> frozenEnemies = new HashMap<>();

    public FrostBitePassive(String id, String name, String desc, int maxStacks,
                            double freezeDuration, double frozenBonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.maxStacks = maxStacks;
        this.freezeDuration = freezeDuration;
        this.frozenDamageBonus = frozenBonus;
        PassiveAbilityCleanup.registerForCleanup(frostStacks);
        PassiveAbilityCleanup.registerForCleanup(frozenEnemies);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID playerUUID = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        World world = target.getWorld();
        Location targetLoc = target.getLocation().add(0, 1, 0);
        long now = System.currentTimeMillis();

        // Initialiser les maps si nécessaire
        frostStacks.computeIfAbsent(playerUUID, k -> new HashMap<>());
        frozenEnemies.computeIfAbsent(playerUUID, k -> new HashMap<>());

        Map<UUID, Integer> playerFrostStacks = frostStacks.get(playerUUID);
        Map<UUID, Long> playerFrozenEnemies = frozenEnemies.get(playerUUID);

        // Vérifier si la cible est gelée
        boolean isFrozen = playerFrozenEnemies.containsKey(targetUUID) &&
                           playerFrozenEnemies.get(targetUUID) > now;

        if (isFrozen) {
            // Bonus de dégâts sur cible gelée
            double adjustedBonus = frozenDamageBonus + (petData.getStatMultiplier() - 1) * 0.10;
            double bonusDamage = damage * adjustedBonus;

            // Effet visuel de dégâts de glace
            world.spawnParticle(Particle.BLOCK, targetLoc, 15, 0.3, 0.3, 0.3, Material.BLUE_ICE.createBlockData());
            world.spawnParticle(Particle.SNOWFLAKE, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);
            world.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);

            return damage + bonusDamage;
        }

        // Ajouter un stack de givre
        int currentStacks = playerFrostStacks.getOrDefault(targetUUID, 0) + 1;
        playerFrostStacks.put(targetUUID, currentStacks);

        // Particules de givre (intensité selon stacks)
        world.spawnParticle(Particle.SNOWFLAKE, targetLoc, currentStacks * 3, 0.2, 0.3, 0.2, 0.02);

        // Afficher les stacks
        if (currentStacks < maxStacks) {
            // Cristaux de glace progressifs autour de l'ennemi
            for (int i = 0; i < currentStacks; i++) {
                double angle = i * (2 * Math.PI / maxStacks);
                Location crystalLoc = target.getLocation().add(
                    Math.cos(angle) * 0.5,
                    0.3 + i * 0.2,
                    Math.sin(angle) * 0.5
                );
                world.spawnParticle(Particle.END_ROD, crystalLoc, 1, 0, 0, 0, 0);
            }

            // Slow progressif selon les stacks
            int slowLevel = Math.min(currentStacks - 1, 2); // Slow I à III
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slowLevel, false, false));

            world.playSound(targetLoc, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, 1.2f + currentStacks * 0.1f);
        }

        // À 5 stacks: GELER l'ennemi
        if (currentStacks >= maxStacks) {
            // Reset les stacks
            playerFrostStacks.put(targetUUID, 0);

            // Calculer la durée de gel (ajustée par niveau)
            double adjustedFreezeDuration = freezeDuration + (petData.getStatMultiplier() - 1) * 0.5;
            long freezeEndTime = now + (long)(adjustedFreezeDuration * 1000);
            playerFrozenEnemies.put(targetUUID, freezeEndTime);

            // Message
            player.sendMessage("§a[Pet] §b❄ Cible §lGELÉE §r§bpendant " + String.format("%.1f", adjustedFreezeDuration) + "s!");

            // Effet de gel spectaculaire
            world.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            world.playSound(targetLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);

            // Animation de gel
            freezeEnemy(target, player, petData, (int)(adjustedFreezeDuration * 20));
        }

        return damage;
    }

    private void freezeEnemy(LivingEntity target, Player player, PetData petData, int freezeTicks) {
        Location baseLoc = target.getLocation();
        World world = target.getWorld();

        // Explosion de cristaux de glace initiale
        for (int i = 0; i < 20; i++) {
            double angle = i * (2 * Math.PI / 20);
            double radius = 0.6;
            Location crystalLoc = baseLoc.clone().add(
                Math.cos(angle) * radius,
                Math.random() * 2,
                Math.sin(angle) * radius
            );
            world.spawnParticle(Particle.BLOCK, crystalLoc, 3, 0.1, 0.1, 0.1, Material.ICE.createBlockData());
        }

        // Maintenir l'ennemi gelé
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= freezeTicks || !target.isValid() || target.isDead()) {
                    // Fin du gel - explosion de glace
                    if (target.isValid()) {
                        Location endLoc = target.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.BLOCK, endLoc, 30, 0.5, 0.5, 0.5, Material.BLUE_ICE.createBlockData());
                        world.spawnParticle(Particle.SNOWFLAKE, endLoc, 20, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(endLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
                    }
                    cancel();
                    return;
                }

                // Maintenir immobile
                target.setVelocity(new Vector(0, -0.1, 0)); // Légère gravité pour rester au sol
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 255, false, false));
                target.setFreezeTicks(target.getMaxFreezeTicks());

                // Particules de gel continues
                if (ticks % 5 == 0) {
                    Location loc = target.getLocation().add(0, 1, 0);

                    // Aura glacée
                    for (int i = 0; i < 8; i++) {
                        double angle = ticks * 0.3 + i * (Math.PI / 4);
                        double radius = 0.4;
                        Location iceLoc = loc.clone().add(
                            Math.cos(angle) * radius,
                            Math.sin(ticks * 0.2) * 0.3,
                            Math.sin(angle) * radius
                        );
                        world.spawnParticle(Particle.SNOWFLAKE, iceLoc, 1, 0, 0, 0, 0);
                    }

                    // Cristaux statiques
                    if (ticks % 10 == 0) {
                        world.spawnParticle(Particle.BLOCK, loc, 5, 0.3, 0.5, 0.3, Material.PACKED_ICE.createBlockData());
                    }
                }

                // Son de craquement de glace
                if (ticks % 20 == 0) {
                    world.playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.8f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Nettoyer les données d'un ennemi mort
     */
    public void cleanupEnemy(UUID playerUUID, UUID enemyUUID) {
        Map<UUID, Integer> stacks = frostStacks.get(playerUUID);
        if (stacks != null) stacks.remove(enemyUUID);

        Map<UUID, Long> frozen = frozenEnemies.get(playerUUID);
        if (frozen != null) frozen.remove(enemyUUID);
    }

    /**
     * Vérifier si une cible est actuellement gelée
     */
    public boolean isTargetFrozen(UUID playerUUID, UUID targetUUID) {
        Map<UUID, Long> frozen = frozenEnemies.get(playerUUID);
        if (frozen == null) return false;
        Long endTime = frozen.get(targetUUID);
        return endTime != null && endTime > System.currentTimeMillis();
    }
}

@Getter
class ArcticStormActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int radius;                              // 8 blocs
    private final int durationSeconds;                     // 6 secondes
    private final double damagePercentPerSecond;           // 10% dégâts joueur/s
    private final FrostBitePassive frostPassive;           // Pour appliquer des stacks de givre

    public ArcticStormActive(String id, String name, String desc, int cd, int radius,
                              int duration, double dpsPercent, FrostBitePassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.radius = radius;
        this.durationSeconds = duration;
        this.damagePercentPerSecond = dpsPercent;
        this.frostPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();
        UUID playerUUID = player.getUniqueId();

        // Calculer les dégâts basés sur les stats du joueur
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedPercent = damagePercentPerSecond + (petData.getStatMultiplier() - 1) * 0.03;
        double dps = playerDamage * adjustedPercent;

        int adjustedRadius = (int) (radius + (petData.getStatMultiplier() - 1) * 2);
        int adjustedDuration = durationSeconds + (int)((petData.getStatMultiplier() - 1) * 2);

        // Vérifier qu'il y a des ennemis
        List<Monster> targets = player.getNearbyEntities(adjustedRadius, 6, adjustedRadius).stream()
            .filter(e -> e instanceof Monster)
            .map(e -> (Monster) e)
            .toList();

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi dans la zone!");
            return false;
        }

        // Message et son de début
        player.sendMessage("§a[Pet] §b§l❄ TEMPÊTE ARCTIQUE! §r§b(rayon " + adjustedRadius + " blocs, " + adjustedDuration + "s)");
        world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.5f);
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);

        // Changer le renard en variante SNOW
        setFoxToSnowVariant(player, true);

        // Animation de tempête arctique
        new BukkitRunnable() {
            int ticks = 0;
            Random random = new Random();
            Location stormCenter = center.clone();

            @Override
            public void run() {
                if (ticks >= adjustedDuration * 20) {
                    // Fin de la tempête - Explosion de gel finale
                    world.playSound(stormCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

                    for (int i = 0; i < 100; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist = random.nextDouble() * adjustedRadius;
                        Location explosionLoc = stormCenter.clone().add(
                            Math.cos(angle) * dist,
                            random.nextDouble() * 4,
                            Math.sin(angle) * dist
                        );
                        world.spawnParticle(Particle.SNOWFLAKE, explosionLoc, 3, 0.2, 0.2, 0.2, 0.1);
                        world.spawnParticle(Particle.BLOCK, explosionLoc, 2, 0.1, 0.1, 0.1, Material.SNOW_BLOCK.createBlockData());
                    }

                    // Remettre le renard normal
                    setFoxToSnowVariant(player, false);

                    player.sendMessage("§a[Pet] §7La tempête se dissipe...");
                    cancel();
                    return;
                }

                // Particules de blizzard - flocons de neige tombants
                for (int i = 0; i < 40; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double dist = random.nextDouble() * adjustedRadius;
                    double height = random.nextDouble() * 8;

                    Location particleLoc = stormCenter.clone().add(
                        Math.cos(angle) * dist,
                        height,
                        Math.sin(angle) * dist
                    );

                    world.spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.3, 0.3, 0.3, 0.02);

                    // Neige tombante occasionnelle
                    if (random.nextDouble() < 0.2) {
                        world.spawnParticle(Particle.BLOCK, particleLoc, 1, 0, 0, 0, Material.SNOW.createBlockData());
                    }
                }

                // Anneaux tourbillonnants de glace
                double spiralAngle = ticks * 0.15;
                for (int ring = 0; ring < 3; ring++) {
                    double ringRadius = (adjustedRadius * 0.3) + ring * (adjustedRadius * 0.25);
                    for (int j = 0; j < 12; j++) {
                        double a = spiralAngle + j * (Math.PI / 6) + ring * (Math.PI / 4);
                        Location ringLoc = stormCenter.clone().add(
                            Math.cos(a) * ringRadius,
                            0.5 + ring * 0.8 + Math.sin(ticks * 0.1) * 0.3,
                            Math.sin(a) * ringRadius
                        );
                        world.spawnParticle(Particle.SNOWFLAKE, ringLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        world.spawnParticle(Particle.END_ROD, ringLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Colonnes de glace aléatoires
                if (ticks % 15 == 0) {
                    double pillarAngle = random.nextDouble() * Math.PI * 2;
                    double pillarDist = random.nextDouble() * adjustedRadius * 0.8;
                    Location pillarBase = stormCenter.clone().add(
                        Math.cos(pillarAngle) * pillarDist,
                        0,
                        Math.sin(pillarAngle) * pillarDist
                    );

                    for (double y = 0; y < 4; y += 0.3) {
                        Location pillarLoc = pillarBase.clone().add(0, y, 0);
                        world.spawnParticle(Particle.BLOCK, pillarLoc, 3, 0.15, 0.1, 0.15, Material.BLUE_ICE.createBlockData());
                        world.spawnParticle(Particle.SNOWFLAKE, pillarLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                    world.playSound(pillarBase, Sound.BLOCK_GLASS_PLACE, 0.5f, 1.5f);
                }

                // Dégâts et slow toutes les 20 ticks (1 seconde)
                if (ticks % 20 == 0) {
                    world.playSound(stormCenter, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.8f, 0.5f);

                    for (Entity e : stormCenter.getWorld().getNearbyEntities(stormCenter, adjustedRadius, 6, adjustedRadius)) {
                        if (e instanceof Monster m) {
                            // Dégâts
                            m.damage(dps, player);
                            petData.addDamage((long) dps);

                            // Slow III
                            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 2, false, false));

                            // Augmenter le freeze ticks
                            m.setFreezeTicks(Math.min(m.getFreezeTicks() + 40, m.getMaxFreezeTicks()));

                            // Particules sur la cible
                            Location mLoc = m.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.SNOWFLAKE, mLoc, 10, 0.3, 0.4, 0.3, 0.05);
                            world.spawnParticle(Particle.BLOCK, mLoc, 5, 0.2, 0.3, 0.2, Material.ICE.createBlockData());
                        }
                    }
                }

                // Son ambiant de vent glacial
                if (ticks % 10 == 0) {
                    world.playSound(stormCenter, Sound.WEATHER_RAIN, 0.6f, 0.3f);
                    world.playSound(stormCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    /**
     * Change le renard en variante SNOW pendant l'ultimate
     */
    private void setFoxToSnowVariant(Player player, boolean snow) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Fox fox
                && entity.getScoreboardTags().contains(ownerTag)) {

                org.bukkit.entity.Fox.Type type = snow ?
                    org.bukkit.entity.Fox.Type.SNOW :
                    org.bukkit.entity.Fox.Type.RED;
                fox.setFoxType(type);

                // Particules de transformation
                Location foxLoc = fox.getLocation().add(0, 0.5, 0);
                if (snow) {
                    fox.getWorld().spawnParticle(Particle.SNOWFLAKE, foxLoc, 30, 0.3, 0.4, 0.3, 0.1);
                    fox.getWorld().spawnParticle(Particle.END_ROD, foxLoc, 10, 0.2, 0.3, 0.2, 0.05);
                    fox.getWorld().playSound(foxLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
                } else {
                    fox.getWorld().spawnParticle(Particle.FLAME, foxLoc, 15, 0.3, 0.3, 0.3, 0.05);
                }

                break;
            }
        }
    }
}

// ==================== OMBRE DÉCHIRANTE - Tentacules d'Ombre ====================

@Getter
class ShadowTentaclePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double tentacleChance;
    private final double tentacleDamage;

    public ShadowTentaclePassive(String id, String name, String desc, double chance, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.tentacleChance = chance;
        this.tentacleDamage = damage;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        if (Math.random() > tentacleChance * petData.getStatMultiplier()) return damage;

        // Faire surgir un tentacule d'ombre sous la cible
        Location targetLoc = target.getLocation();
        double tentacleDmg = tentacleDamage * petData.getStatMultiplier();

        // Animation du tentacule qui surgit
        new BukkitRunnable() {
            double height = 0;
            Location baseLoc = targetLoc.clone();

            @Override
            public void run() {
                if (height > 3) {
                    // Attaque du tentacule
                    target.damage(tentacleDmg, player);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false));
                    petData.addDamage((long) tentacleDmg);

                    baseLoc.getWorld().spawnParticle(Particle.SMOKE, baseLoc.clone().add(0, 2, 0), 30, 0.3, 0.5, 0.3, 0.05);
                    baseLoc.getWorld().playSound(baseLoc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.5f);

                    cancel();
                    return;
                }

                // Particules du tentacule montant
                for (int i = 0; i < 3; i++) {
                    double angle = (height * 60 + i * 120) * Math.PI / 180;
                    double radius = 0.3;
                    Location particleLoc = baseLoc.clone().add(
                        Math.cos(angle) * radius,
                        height,
                        Math.sin(angle) * radius
                    );
                    baseLoc.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 5, 0.05, 0.05, 0.05, 0);
                    baseLoc.getWorld().spawnParticle(Particle.WITCH, particleLoc, 2, 0.05, 0.05, 0.05, 0);
                }

                height += 0.5;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 2L);

        return damage;
    }
}

@Getter
class VoidVortexActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double vortexDamage;
    private final int durationSeconds;

    public VoidVortexActive(String id, String name, String desc, double damage, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.vortexDamage = damage;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 50; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getTargetBlock(null, 15).getLocation().add(0, 1, 0);
        double damage = vortexDamage * petData.getStatMultiplier();

        player.sendMessage("§a[Pet] §8§l🌀 VORTEX DU NÉANT!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.3f);

        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (ticks >= durationSeconds * 20) {
                    // Explosion finale
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 10, 2, 1, 2, 0);
                    center.getWorld().spawnParticle(Particle.WITCH, center, 100, 4, 2, 4, 0.3);
                    center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);

                    for (Entity e : center.getWorld().getNearbyEntities(center, 6, 6, 6)) {
                        if (e instanceof Monster m) {
                            m.damage(damage * 3, player);
                            petData.addDamage((long) (damage * 3));
                        }
                    }

                    cancel();
                    return;
                }

                // Spirale de particules d'ombre
                for (int ring = 0; ring < 3; ring++) {
                    double radius = 2 + ring * 1.5;
                    double ringAngle = angle + ring * Math.PI / 3;

                    for (int i = 0; i < 12; i++) {
                        double a = ringAngle + i * Math.PI / 6;
                        double y = Math.sin(ticks * 0.1 + ring) * 0.5 + ring * 0.3;
                        Location particleLoc = center.clone().add(
                            Math.cos(a) * radius,
                            y,
                            Math.sin(a) * radius
                        );

                        center.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                        center.getWorld().spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Tentacules surgissant au hasard
                if (ticks % 20 == 0) {
                    double tentacleAngle = Math.random() * Math.PI * 2;
                    double tentacleRadius = Math.random() * 4;
                    Location tentacleLoc = center.clone().add(
                        Math.cos(tentacleAngle) * tentacleRadius,
                        0,
                        Math.sin(tentacleAngle) * tentacleRadius
                    );
                    spawnTentacle(tentacleLoc, player, damage, petData);
                }

                // Aspirer les ennemis
                for (Entity e : center.getWorld().getNearbyEntities(center, 8, 4, 8)) {
                    if (e instanceof Monster) {
                        Vector pull = center.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.15);
                        e.setVelocity(e.getVelocity().add(pull));
                    }
                }

                // Son ambiant
                if (ticks % 15 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.5f);
                }

                angle += 0.15;
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void spawnTentacle(Location loc, Player player, double damage, PetData petData) {
        new BukkitRunnable() {
            double height = 0;

            @Override
            public void run() {
                if (height > 4) {
                    for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0, 2, 0), 1.5, 2, 1.5)) {
                        if (e instanceof Monster m) {
                            m.damage(damage, player);
                            petData.addDamage((long) damage);
                        }
                    }
                    cancel();
                    return;
                }

                for (int i = 0; i < 5; i++) {
                    double angle = (height * 90 + i * 72) * Math.PI / 180;
                    Location particleLoc = loc.clone().add(
                        Math.cos(angle) * 0.2,
                        height,
                        Math.sin(angle) * 0.2
                    );
                    loc.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 3, 0.05, 0.05, 0.05, 0);
                }

                height += 0.4;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== ESPRIT PRISMATIQUE - Rayons Arc-en-ciel ====================

@Getter
class PrismaticBeamPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double beamDamage;
    private final Map<UUID, Long> lastBeam = new HashMap<>();

    public PrismaticBeamPassive(String id, String name, String desc, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.beamDamage = damage;
        PassiveAbilityCleanup.registerForCleanup(lastBeam);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (now - lastBeam.getOrDefault(uuid, 0L) < 3000) return damage;
        lastBeam.put(uuid, now);

        // Rayon prismatique vers la cible
        Location start = player.getLocation().add(0, 1.5, 0);
        Location end = target.getLocation().add(0, 1, 0);
        double beamDmg = beamDamage * petData.getStatMultiplier();

        drawPrismaticBeam(start, end);

        // Dégâts bonus
        target.damage(beamDmg, player);
        petData.addDamage((long) beamDmg);

        // Effet sur la cible
        target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.2);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);

        return damage;
    }

    private void drawPrismaticBeam(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA, Color.BLUE, Color.PURPLE};

        for (double d = 0; d < distance; d += 0.3) {
            Location point = from.clone().add(direction.clone().multiply(d));
            int colorIndex = (int) ((d / distance) * colors.length) % colors.length;
            Color color = colors[colorIndex];

            Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
            from.getWorld().spawnParticle(Particle.DUST, point, 3, 0.05, 0.05, 0.05, dust);
            from.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }
    }
}

@Getter
class RainbowNovaActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double novaDamage;
    private final int radius;

    public RainbowNovaActive(String id, String name, String desc, double damage, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.novaDamage = damage;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double damage = novaDamage * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §d§l✨ NOVA PRISMATIQUE!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA, Color.BLUE, Color.PURPLE, Color.FUCHSIA};

        // Animation de la nova
        new BukkitRunnable() {
            double currentRadius = 0;
            int colorOffset = 0;

            @Override
            public void run() {
                if (currentRadius > adjustedRadius) {
                    cancel();
                    return;
                }

                // Anneaux de couleur
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    Location particleLoc = center.clone().add(
                        Math.cos(rad) * currentRadius,
                        0.5,
                        Math.sin(rad) * currentRadius
                    );

                    int colorIndex = (i / 10 + colorOffset) % colors.length;
                    Color color = colors[colorIndex];
                    Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);

                    center.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, dust);
                    center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                }

                // Piliers de lumière
                if ((int) currentRadius % 3 == 0) {
                    for (int a = 0; a < 360; a += 45) {
                        double rad = Math.toRadians(a + colorOffset * 5);
                        Location pillarBase = center.clone().add(
                            Math.cos(rad) * currentRadius,
                            0,
                            Math.sin(rad) * currentRadius
                        );

                        int colorIndex = (a / 45 + colorOffset) % colors.length;
                        Color color = colors[colorIndex];

                        for (double y = 0; y < 5; y += 0.3) {
                            Location pillarLoc = pillarBase.clone().add(0, y, 0);
                            Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
                            center.getWorld().spawnParticle(Particle.DUST, pillarLoc, 2, 0.05, 0.05, 0.05, dust);
                        }
                    }
                }

                // Dégâts aux ennemis touchés par l'onde
                for (Entity e : center.getWorld().getNearbyEntities(center, currentRadius + 1, 3, currentRadius + 1)) {
                    if (e instanceof Monster m) {
                        double dist = m.getLocation().distance(center);
                        if (dist >= currentRadius - 0.5 && dist <= currentRadius + 0.5) {
                            m.damage(damage, player);
                            petData.addDamage((long) damage);

                            // Particules sur l'ennemi
                            m.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, m.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
                        }
                    }
                }

                currentRadius += 0.8;
                colorOffset++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }
}

// ==================== KRAKEN MINIATURE - Tentacules d'Eau ====================

@Getter
class WaterTentaclePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double grabChance;
    private final double holdDuration;

    public WaterTentaclePassive(String id, String name, String desc, double chance, double duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.grabChance = chance;
        this.holdDuration = duration;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        if (Math.random() > grabChance * petData.getStatMultiplier()) return damage;

        // Tentacule d'eau qui immobilise
        Location targetLoc = target.getLocation();
        int holdTicks = (int) (holdDuration * 20 * petData.getStatMultiplier());

        // Animation du tentacule
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= holdTicks || !target.isValid()) {
                    cancel();
                    return;
                }

                // Garder l'ennemi immobile
                target.setVelocity(new Vector(0, 0, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));

                // Particules de tentacule d'eau
                for (int i = 0; i < 4; i++) {
                    double angle = (ticks * 15 + i * 90) * Math.PI / 180;
                    double radius = 0.5;
                    for (double y = 0; y < 2; y += 0.3) {
                        Location particleLoc = targetLoc.clone().add(
                            Math.cos(angle) * radius,
                            y,
                            Math.sin(angle) * radius
                        );
                        targetLoc.getWorld().spawnParticle(Particle.DRIPPING_WATER, particleLoc, 2, 0.05, 0.05, 0.05, 0);
                        targetLoc.getWorld().spawnParticle(Particle.BUBBLE, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_SQUID_SQUIRT, 1.0f, 0.8f);

        return damage * 1.2; // Bonus de dégâts sur cible immobilisée
    }
}

@Getter
class TsunamiActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double waveDamage;
    private final int waveLength;

    public TsunamiActive(String id, String name, String desc, double damage, int length) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.waveDamage = damage;
        this.waveLength = length;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 50; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        double damage = waveDamage * petData.getStatMultiplier();
        int length = (int) (waveLength * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §b§l🌊 TSUNAMI!");
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 2.0f, 0.5f);

        new BukkitRunnable() {
            int distance = 0;
            Set<UUID> hit = new HashSet<>();

            @Override
            public void run() {
                if (distance > length) {
                    cancel();
                    return;
                }

                // Calculer la position de la vague
                Location waveCenter = start.clone().add(direction.clone().multiply(distance));

                // Dessiner la vague (arc de cercle)
                Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX());
                int waveWidth = 5 + distance / 3;
                double waveHeight = 2 + distance * 0.15;

                for (int w = -waveWidth; w <= waveWidth; w++) {
                    Location waveLoc = waveCenter.clone().add(perpendicular.clone().multiply(w * 0.5));

                    // Particules d'eau
                    for (double y = 0; y < waveHeight; y += 0.3) {
                        Location particleLoc = waveLoc.clone().add(0, y, 0);
                        start.getWorld().spawnParticle(Particle.DRIPPING_WATER, particleLoc, 3, 0.2, 0.1, 0.2, 0);
                        start.getWorld().spawnParticle(Particle.BUBBLE_POP, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);

                        if (y < 0.5) {
                            start.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 5, 0.3, 0.1, 0.3, 0.1);
                        }
                    }

                    // Crête de la vague (plus de particules en haut)
                    Location crestLoc = waveLoc.clone().add(0, waveHeight, 0);
                    start.getWorld().spawnParticle(Particle.SPLASH, crestLoc, 10, 0.2, 0.1, 0.2, 0.2);
                    start.getWorld().spawnParticle(Particle.CLOUD, crestLoc, 2, 0.1, 0.05, 0.1, 0.02);
                }

                // Dégâts et knockback
                for (Entity e : waveCenter.getWorld().getNearbyEntities(waveCenter, waveWidth * 0.5 + 1, waveHeight, 2)) {
                    if (e instanceof Monster m && !hit.contains(m.getUniqueId())) {
                        hit.add(m.getUniqueId());
                        m.damage(damage, player);
                        petData.addDamage((long) damage);

                        // Knockback dans la direction de la vague
                        Vector kb = direction.clone().multiply(1.2).setY(0.5);
                        m.setVelocity(kb);

                        // Effet de noyade
                        m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
                        m.setRemainingAir(0);
                    }
                }

                // Son de vague
                if (distance % 3 == 0) {
                    waveCenter.getWorld().playSound(waveCenter, Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.8f);
                }

                distance += 2;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 2L);

        return true;
    }
}

// ==================== ÉTOILE FILANTE - Traînée Stellaire ====================

@Getter
class StardustTrailPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double trailDamage;
    private final Map<UUID, Long> lastTrail = new HashMap<>();

    public StardustTrailPassive(String id, String name, String desc, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.trailDamage = damage;
        PassiveAbilityCleanup.registerForCleanup(lastTrail);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (now - lastTrail.getOrDefault(uuid, 0L) < 500) return;
        lastTrail.put(uuid, now);

        Location loc = player.getLocation();
        double damage = trailDamage * petData.getStatMultiplier();

        // Traînée d'étoiles
        Color[] starColors = {Color.WHITE, Color.YELLOW, Color.fromRGB(255, 215, 0)};
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1;
            double offsetY = random.nextDouble() * 2;
            double offsetZ = (random.nextDouble() - 0.5) * 1;
            Location particleLoc = loc.clone().add(offsetX, offsetY, offsetZ);

            Color color = starColors[random.nextInt(starColors.length)];
            Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f);
            loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.05, 0.05, 0.05, dust);
            loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
        }

        // Dégâts aux ennemis dans la traînée
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 2, 1)) {
            if (e instanceof Monster m) {
                m.damage(damage, player);
                petData.addDamage((long) damage);
            }
        }
    }
}

@Getter
class ShootingStarActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double starDamage;
    private final int starCount;

    public ShootingStarActive(String id, String name, String desc, double damage, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.starDamage = damage;
        this.starCount = count;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double damage = starDamage * petData.getStatMultiplier();
        int count = (int) (starCount * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §e§l⭐ PLUIE D'ÉTOILES!");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);

        Random random = new Random();
        Color[] colors = {Color.WHITE, Color.YELLOW, Color.fromRGB(255, 215, 0), Color.fromRGB(255, 182, 193)};

        for (int i = 0; i < count; i++) {
            int delay = i * 5;

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), () -> {
                // Position de départ aléatoire dans le ciel
                double offsetX = (random.nextDouble() - 0.5) * 20;
                double offsetZ = (random.nextDouble() - 0.5) * 20;
                Location start = center.clone().add(offsetX, 20 + random.nextDouble() * 10, offsetZ);

                // Trouver une cible ou un point au sol
                Location target = center.clone().add(
                    (random.nextDouble() - 0.5) * 10,
                    0,
                    (random.nextDouble() - 0.5) * 10
                );

                // Choisir une cible proche si possible
                for (Entity e : center.getWorld().getNearbyEntities(center, 12, 10, 12)) {
                    if (e instanceof Monster && random.nextDouble() < 0.6) {
                        target = e.getLocation();
                        break;
                    }
                }

                spawnShootingStar(start, target, player, damage, petData, colors[random.nextInt(colors.length)]);
            }, delay);
        }

        return true;
    }

    private void spawnShootingStar(Location start, Location target, Player player, double damage, PetData petData, Color color) {
        Vector direction = target.toVector().subtract(start.toVector()).normalize().multiply(2);

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 40 || current.getY() < target.getY() + 1) {
                    // Impact stellaire
                    current.getWorld().spawnParticle(Particle.END_ROD, current, 30, 1, 0.5, 1, 0.1);
                    current.getWorld().spawnParticle(Particle.FLASH, current, 1, 0, 0, 0, 0);

                    Particle.DustOptions dust = new Particle.DustOptions(color, 2.0f);
                    current.getWorld().spawnParticle(Particle.DUST, current, 50, 1.5, 0.5, 1.5, dust);

                    current.getWorld().playSound(current, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.5f);

                    for (Entity e : current.getWorld().getNearbyEntities(current, 3, 3, 3)) {
                        if (e instanceof Monster m) {
                            m.damage(damage, player);
                            petData.addDamage((long) damage);
                        }
                    }

                    cancel();
                    return;
                }

                current.add(direction);

                // Traînée d'étoile
                Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);
                current.getWorld().spawnParticle(Particle.DUST, current, 5, 0.1, 0.1, 0.1, dust);
                current.getWorld().spawnParticle(Particle.END_ROD, current, 3, 0.05, 0.05, 0.05, 0.02);

                // Traînée scintillante
                for (int i = 0; i < 3; i++) {
                    Location trailLoc = current.clone().subtract(direction.clone().multiply(i * 0.3));
                    Particle.DustOptions trailDust = new Particle.DustOptions(color, 1.0f - i * 0.2f);
                    current.getWorld().spawnParticle(Particle.DUST, trailLoc, 2, 0.05, 0.05, 0.05, trailDust);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}
