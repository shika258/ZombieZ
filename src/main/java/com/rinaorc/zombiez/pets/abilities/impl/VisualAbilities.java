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

// ==================== SERPENT FOUDROYANT - Éclairs en Chaîne ====================

@Getter
class ChainLightningPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double chainChance;
    private final int maxChains;
    private final double chainDamage;

    public ChainLightningPassive(String id, String name, String desc, double chance, int chains, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.chainChance = chance;
        this.maxChains = chains;
        this.chainDamage = damage;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        if (Math.random() > chainChance * petData.getStatMultiplier()) return damage;

        // Déclencher la chaîne d'éclairs
        int chains = (int) (maxChains * petData.getStatMultiplier());
        double chainDmg = chainDamage * petData.getStatMultiplier();

        chainLightning(player, target, chains, chainDmg, petData, new HashSet<>());

        return damage;
    }

    private void chainLightning(Player player, LivingEntity current, int remainingChains, double damage, PetData petData, Set<UUID> hit) {
        if (remainingChains <= 0 || current == null) return;
        hit.add(current.getUniqueId());

        // Effet d'éclair sur la cible
        Location loc = current.getLocation().add(0, 1, 0);
        current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 0.3, 0.5, 0.3, 0.1);
        current.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        current.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);

        current.damage(damage, player);
        petData.addDamage((long) damage);

        // Trouver la prochaine cible
        LivingEntity next = null;
        double minDist = 8;

        for (Entity e : current.getNearbyEntities(8, 8, 8)) {
            if (e instanceof Monster m && !hit.contains(m.getUniqueId())) {
                double dist = m.getLocation().distance(current.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    next = m;
                }
            }
        }

        if (next != null) {
            // Dessiner l'éclair entre les deux cibles
            drawLightningBolt(current.getLocation().add(0, 1, 0), next.getLocation().add(0, 1, 0));

            // Continuer la chaîne avec délai
            LivingEntity finalNext = next;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> chainLightning(player, finalNext, remainingChains - 1, damage * 0.8, petData, hit),
                3L
            );
        }
    }

    private void drawLightningBolt(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        Random random = new Random();
        Location current = from.clone();

        for (double d = 0; d < distance; d += 0.5) {
            // Ajouter un peu de zigzag
            double offsetX = (random.nextDouble() - 0.5) * 0.3;
            double offsetY = (random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (random.nextDouble() - 0.5) * 0.3;

            current.add(direction.clone().multiply(0.5)).add(offsetX, offsetY, offsetZ);
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.05, 0.05, 0.05, 0);
        }
    }
}

@Getter
class ThunderstormActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double boltDamage;
    private final int boltCount;

    public ThunderstormActive(String id, String name, String desc, double damage, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.boltDamage = damage;
        this.boltCount = count;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double damage = boltDamage * petData.getStatMultiplier();
        int count = (int) (boltCount * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §e§l⚡ TEMPÊTE DE FOUDRE!");
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);

        // Collecter les cibles
        List<Monster> targets = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(15, 15, 15)) {
            if (e instanceof Monster m) targets.add(m);
        }

        // Frapper les cibles avec des éclairs
        new BukkitRunnable() {
            int struck = 0;
            Random random = new Random();

            @Override
            public void run() {
                if (struck >= count || targets.isEmpty()) {
                    cancel();
                    return;
                }

                // Choisir une cible
                Monster target = targets.get(random.nextInt(targets.size()));
                Location loc = target.getLocation();

                // Effet d'éclair spectaculaire
                loc.getWorld().strikeLightningEffect(loc);

                // Particules supplémentaires
                for (int i = 0; i < 5; i++) {
                    double y = i * 5;
                    Location boltLoc = loc.clone().add(0, y, 0);
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 20, 0.2, 2, 0.2, 0.1);
                }
                loc.getWorld().spawnParticle(Particle.FLASH, loc, 3, 0.5, 0.5, 0.5, 0);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);

                // Dégâts + chaîne
                target.damage(damage, player);
                petData.addDamage((long) damage);

                // Dégâts aux ennemis proches
                for (Entity e : target.getNearbyEntities(4, 4, 4)) {
                    if (e instanceof Monster m && m != target) {
                        m.damage(damage * 0.5, player);
                        m.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, m.getLocation().add(0, 1, 0), 15, 0.2, 0.3, 0.2, 0.05);
                        petData.addDamage((long) (damage * 0.5));
                    }
                }

                struck++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 5L);

        return true;
    }
}

// ==================== GOLEM DE LAVE - Éruption Volcanique ====================

@Getter
class LavaTrailPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double burnDamage;
    private final Map<UUID, Long> lastTrail = new HashMap<>();

    public LavaTrailPassive(String id, String name, String desc, double damage) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.burnDamage = damage;
        PassiveAbilityCleanup.registerForCleanup(lastTrail);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Laisser une traînée de lave toutes les secondes
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (now - lastTrail.getOrDefault(uuid, 0L) < 1000) return;
        lastTrail.put(uuid, now);

        Location loc = player.getLocation();
        double damage = burnDamage * petData.getStatMultiplier();

        // Créer une zone de lave temporaire
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.5, 0.1, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.3, 0.1, 0.3, 0.02);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.1, 0.2, 0.01);

        // Zone de dégâts pendant 3 secondes
        new BukkitRunnable() {
            int ticks = 0;
            Location trailLoc = loc.clone();

            @Override
            public void run() {
                if (ticks >= 60) {
                    cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    trailLoc.getWorld().spawnParticle(Particle.LAVA, trailLoc, 5, 0.3, 0.1, 0.3, 0);
                    for (Entity e : trailLoc.getWorld().getNearbyEntities(trailLoc, 1.5, 1, 1.5)) {
                        if (e instanceof Monster m) {
                            m.damage(damage, player);
                            m.setFireTicks(40);
                            petData.addDamage((long) damage);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

@Getter
class VolcanicEruptionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double eruptionDamage;
    private final int projectileCount;

    public VolcanicEruptionActive(String id, String name, String desc, double damage, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.eruptionDamage = damage;
        this.projectileCount = count;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 40; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        double damage = eruptionDamage * petData.getStatMultiplier();
        int count = (int) (projectileCount * petData.getStatMultiplier());

        player.sendMessage("§a[Pet] §c§l🌋 ÉRUPTION VOLCANIQUE!");

        // Effet d'éruption initial
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 5, 1, 0.5, 1, 0);
        center.getWorld().spawnParticle(Particle.LAVA, center, 100, 2, 3, 2, 0.5);
        center.getWorld().spawnParticle(Particle.FLAME, center, 150, 3, 4, 3, 0.2);
        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 50, 2, 3, 2, 0.1);

        // Colonne de feu
        new BukkitRunnable() {
            int height = 0;

            @Override
            public void run() {
                if (height > 15) {
                    cancel();
                    return;
                }

                Location columnLoc = center.clone().add(0, height, 0);
                columnLoc.getWorld().spawnParticle(Particle.FLAME, columnLoc, 30, 1, 0.5, 1, 0.1);
                columnLoc.getWorld().spawnParticle(Particle.LAVA, columnLoc, 10, 0.5, 0.3, 0.5, 0);

                height += 2;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 2L);

        // Projectiles de lave
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            final int delay = i * 3;

            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), () -> {
                // Direction aléatoire vers l'extérieur
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 0.5 + random.nextDouble() * 0.5;
                Vector velocity = new Vector(
                    Math.cos(angle) * speed,
                    0.8 + random.nextDouble() * 0.4,
                    Math.sin(angle) * speed
                );

                Location start = center.clone().add(0, 2, 0);
                spawnLavaProjectile(start, velocity, player, damage, petData);
            }, delay);
        }

        return true;
    }

    private void spawnLavaProjectile(Location start, Vector velocity, Player player, double damage, PetData petData) {
        new BukkitRunnable() {
            Location current = start.clone();
            Vector vel = velocity.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 60 || current.getY() < start.getY() - 5) {
                    // Impact au sol
                    current.getWorld().spawnParticle(Particle.LAVA, current, 20, 0.5, 0.2, 0.5, 0);
                    current.getWorld().spawnParticle(Particle.FLAME, current, 15, 0.3, 0.1, 0.3, 0.05);
                    current.getWorld().playSound(current, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.0f);

                    for (Entity e : current.getWorld().getNearbyEntities(current, 2, 2, 2)) {
                        if (e instanceof Monster m) {
                            m.damage(damage, player);
                            m.setFireTicks(100);
                            petData.addDamage((long) damage);
                        }
                    }

                    cancel();
                    return;
                }

                // Gravité
                vel.setY(vel.getY() - 0.08);
                current.add(vel);

                // Particules
                current.getWorld().spawnParticle(Particle.LAVA, current, 3, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.FLAME, current, 5, 0.1, 0.1, 0.1, 0.02);
                current.getWorld().spawnParticle(Particle.SMOKE, current, 2, 0.1, 0.1, 0.1, 0.01);

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
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
