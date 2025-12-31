package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.listeners.PetCombatListener;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Implémentations des capacités passives des pets
 */

// ==================== DÉTECTION ====================

@Getter
class DetectionPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int radius;

    DetectionPassive(String id, String name, String desc, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());
        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);

        for (Entity entity : nearby) {
            if (entity instanceof Monster) {
                // Particules de détection
                Location loc = entity.getLocation().add(0, 1, 0);
                player.spawnParticle(Particle.DUST, loc, 3,
                        new Particle.DustOptions(Color.RED, 0.5f));
            }
        }
    }
}

// ==================== LOOT BONUS ====================

@Getter
class LootBonusPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double bonusPercent;

    LootBonusPassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.bonusPercent = bonus;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}

// ==================== LUMIÈRE ====================

@Getter
class LightPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int radius;

    LightPassive(String id, String name, String desc, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.radius = radius;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Particules lumineuses
        Location loc = player.getLocation().add(0, 1, 0);
        player.spawnParticle(Particle.END_ROD, loc, 2, 0.5, 0.5, 0.5, 0);
    }
}

// ==================== RÉDUCTION DE DÉGÂTS ====================

// ==================== VITESSE ====================

@Getter
class SpeedPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double speedBonus;

    SpeedPassive(String id, String name, String desc, double speed) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.speedBonus = speed;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        float currentSpeed = player.getWalkSpeed();
        float newSpeed = (float) Math.min(1.0, currentSpeed + (0.2 * speedBonus * petData.getStatMultiplier()));
        player.setWalkSpeed(newSpeed);
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        player.setWalkSpeed(0.2f); // Reset à la vitesse par défaut
    }
}

// ==================== ATTAQUE AUTO ====================

@Getter
class AttackPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damage;
    private final int intervalTicks;
    private final int attackRange;
    private final Map<UUID, Long> lastAttack = new HashMap<>();

    AttackPassive(String id, String name, String desc, double damage, int interval) {
        this(id, name, desc, damage, interval, 5); // Range par défaut de 5 blocs
    }

    AttackPassive(String id, String name, String desc, double damage, int interval, int range) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damage = damage;
        this.intervalTicks = interval;
        this.attackRange = range;
        PassiveAbilityCleanup.registerForCleanup(lastAttack);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastAttack.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50))
            return;
        lastAttack.put(player.getUniqueId(), now);

        double adjustedDamage = damage * petData.getStatMultiplier();
        Monster target = findTarget(player);

        if (target != null) {
            target.damage(adjustedDamage, player);
            petData.addDamage((long) adjustedDamage);

            // Effet visuel
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5);
        }
    }

    /**
     * Trouve la meilleure cible pour le pet
     * Priorité 1: Le mob que le joueur attaque actuellement
     * Priorité 2: Le mob le plus proche dans la portée
     */
    private Monster findTarget(Player player) {
        // Priorité 1: La cible du joueur
        LivingEntity playerTarget = PetCombatListener.getPlayerTarget(player);
        if (playerTarget instanceof Monster monster && monster.isValid() && !monster.isDead()) {
            double distance = player.getLocation().distance(monster.getLocation());
            if (distance <= attackRange) {
                return monster;
            }
        }

        // Priorité 2: Le mob le plus proche
        Monster closestMonster = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(attackRange, attackRange, attackRange)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                double distance = player.getLocation().distance(monster.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestMonster = monster;
                }
            }
        }

        return closestMonster;
    }
}

// ==================== RÉGÉNÉRATION ====================

@Getter
class RegenPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healAmount;
    private final int intervalTicks;
    private final Map<UUID, Long> lastHeal = new HashMap<>();

    RegenPassive(String id, String name, String desc, double heal, int interval) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healAmount = heal;
        this.intervalTicks = interval;
        PassiveAbilityCleanup.registerForCleanup(lastHeal);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastHeal.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50))
            return;
        lastHeal.put(player.getUniqueId(), now);

        double adjustedHeal = healAmount * petData.getStatMultiplier();
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + adjustedHeal);
        player.setHealth(newHealth);

        // Effet visuel
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1);
    }
}

// ==================== INTERCEPTION ====================

// ==================== ENFLAMMER ====================

@Getter
class IgnitePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double igniteChance;

    IgnitePassive(String id, String name, String desc, double chance) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.igniteChance = chance;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
        double adjustedChance = igniteChance * petData.getStatMultiplier();
        if (Math.random() < adjustedChance) {
            target.setFireTicks(60); // 3 secondes de feu
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 10);
        }
    }
}

// ==================== RALENTISSEMENT ====================

@Getter
class SlowPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int slowDurationTicks;

    SlowPassive(String id, String name, String desc, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.slowDurationTicks = duration;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
        int adjustedDuration = (int) (slowDurationTicks * petData.getStatMultiplier());
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, adjustedDuration, 1, false, false));
    }
}

// ==================== DÉGÂTS ÉLÉMENTAIRES ====================

@Getter
class ElementalDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageBonus;
    private final String element;

    ElementalDamagePassive(String id, String name, String desc, double bonus, String element) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageBonus = bonus;
        this.element = element;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}

// ==================== RÉDUCTION COOLDOWN ====================

@Getter
class CooldownReductionPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double cdrPercent;

    CooldownReductionPassive(String id, String name, String desc, double cdr) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cdrPercent = cdr;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via ClassManager/TalentManager
}

// ==================== AURA DE DÉGÂTS ====================

@Getter
class AuraPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerSecond;
    private final int radius;
    private final Map<UUID, Long> lastDamage = new HashMap<>();

    AuraPassive(String id, String name, String desc, double dps, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerSecond = dps;
        this.radius = radius;
        PassiveAbilityCleanup.registerForCleanup(lastDamage);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastDamage.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 1000)
            return; // 1 seconde
        lastDamage.put(player.getUniqueId(), now);

        double adjustedDamage = damagePerSecond * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        // Récupérer la cible prioritaire du joueur
        LivingEntity playerTarget = PetCombatListener.getPlayerTarget(player);
        UUID priorityTargetId = playerTarget != null ? playerTarget.getUniqueId() : null;

        // Collecter et trier les mobs par distance (les plus proches d'abord)
        List<Monster> monsters = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                monsters.add(monster);
            }
        }

        // Trier par distance (plus proche = premier)
        monsters.sort(Comparator.comparingDouble(m -> player.getLocation().distanceSquared(m.getLocation())));

        // Appliquer les dégâts avec bonus sur la cible prioritaire
        for (Monster monster : monsters) {
            double damageToApply = adjustedDamage;

            // Bonus de 50% sur la cible que le joueur attaque
            if (priorityTargetId != null && monster.getUniqueId().equals(priorityTargetId)) {
                damageToApply *= 1.5;
                // Effet visuel spécial pour la cible prioritaire
                monster.getWorld().spawnParticle(Particle.FLAME, monster.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
            }

            monster.damage(damageToApply, player);
            petData.addDamage((long) damageToApply);
        }
    }
}

// ==================== PARADE ====================

// ==================== MULTIPLICATEUR DE DÉGÂTS ====================

// ==================== NÉCROMANCIE ====================

@Getter
class NecromancyPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double convertChance;
    private final int duration;

    NecromancyPassive(String id, String name, String desc, double chance, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.convertChance = chance;
        this.duration = duration;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        if (!(killed instanceof Monster))
            return;

        double adjustedChance = convertChance * petData.getStatMultiplier();
        if (Math.random() < adjustedChance) {
            // Particules de conversion
            killed.getWorld().spawnParticle(Particle.WITCH, killed.getLocation(), 30, 0.5, 1, 0.5, 0);
            // La logique de minion est gérée ailleurs
        }
    }
}

// ==================== EVOKER FANGS ====================

@Getter
class EvokerFangsPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double fangChance;              // 20% de base
    private final double damagePercent;           // % des dégâts du joueur
    private final Map<UUID, Long> lastFangSpawn = new HashMap<>();

    EvokerFangsPassive(String id, String name, String desc, double chance, double dmgPercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.fangChance = chance;
        this.damagePercent = dmgPercent;
        PassiveAbilityCleanup.registerForCleanup(lastFangSpawn);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Cooldown de 0.5s entre les spawns de crocs
        if (now - lastFangSpawn.getOrDefault(playerUUID, 0L) < 500) {
            return damage;
        }

        // Calculer la chance ajustée par niveau
        double adjustedChance = fangChance + (petData.getStatMultiplier() - 1) * 0.10;

        if (Math.random() > adjustedChance) {
            return damage;
        }

        lastFangSpawn.put(playerUUID, now);

        // Calculer les dégâts des crocs
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedDmgPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.15;
        double fangDamage = playerDamage * adjustedDmgPercent;

        Location targetLoc = target.getLocation();
        World world = targetLoc.getWorld();

        // Invoquer les crocs d'Evoker
        spawnEvokerFangs(world, targetLoc, player, fangDamage, petData);

        // Animation de l'Evoker pet (lever les bras)
        animateEvokerPet(player, true);

        // Remettre l'animation normale après un court délai
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> animateEvokerPet(player, false),
            15L
        );

        return damage;
    }

    private void spawnEvokerFangs(World world, Location center, Player player, double damage, PetData petData) {
        // Spawn d'une ligne de 3 crocs
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0, direction.getX());

        // Son d'invocation
        world.playSound(center, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 1.0f);
        world.playSound(center, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 1.2f);

        // Particules de préparation
        world.spawnParticle(Particle.WITCH, center, 15, 0.5, 0.2, 0.5, 0.02);

        // Spawner 3 crocs en ligne
        for (int i = -1; i <= 1; i++) {
            Location fangLoc = center.clone().add(perpendicular.clone().multiply(i * 0.8));
            fangLoc.setY(center.getY());

            // Spawn du croc avec délai progressif
            final int delay = Math.abs(i) * 2;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    // Spawner le croc d'Evoker
                    org.bukkit.entity.EvokerFangs fangs = world.spawn(fangLoc, org.bukkit.entity.EvokerFangs.class, f -> {
                        f.setOwner(player);
                    });

                    // Appliquer les dégâts manuellement aux mobs proches
                    Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("ZombieZ"),
                        () -> {
                            for (Entity entity : world.getNearbyEntities(fangLoc, 1.0, 1.5, 1.0)) {
                                if (entity instanceof Monster m && m.isValid() && !m.isDead()) {
                                    m.damage(damage, player);
                                    petData.addDamage((long) damage);

                                    // Particules d'impact
                                    m.getWorld().spawnParticle(Particle.CRIT, m.getLocation().add(0, 1, 0),
                                        10, 0.2, 0.3, 0.2, 0.1);
                                }
                            }
                        },
                        5L // Les crocs frappent après ~0.25s
                    );
                },
                delay
            );
        }
    }

    private void animateEvokerPet(Player player, boolean raise) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Evoker evoker
                && entity.getScoreboardTags().contains(ownerTag)) {

                // Note: L'API Bukkit ne permet pas de contrôler directement l'animation
                // On utilise des particules pour simuler l'effet magique
                if (raise) {
                    Location loc = evoker.getLocation().add(0, 1.5, 0);
                    evoker.getWorld().spawnParticle(Particle.ENCHANT, loc, 20, 0.3, 0.3, 0.3, 0.5);
                    evoker.getWorld().spawnParticle(Particle.WITCH, loc, 10, 0.2, 0.2, 0.2, 0.02);
                }
                break;
            }
        }
    }
}

// ==================== BONUS HP ====================

@Getter
class HealthBonusPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double bonusPercent;

    HealthBonusPassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.bonusPercent = bonus;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via onEquip/onUnequip
}

// ==================== JOYFUL TEARS (Happy Ghast) ====================

@Getter
class JoyfulTearsPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healPercent;             // 5% HP par kill

    JoyfulTearsPassive(String id, String name, String desc, double healPct) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healPercent = healPct;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        if (!(killed instanceof Monster)) return;

        // Calculer le heal ajusté par niveau
        double adjustedHeal = healPercent + (petData.getStatMultiplier() - 1) * 0.03;
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        double healAmount = maxHealth * adjustedHeal;

        // Appliquer le soin
        double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
        player.setHealth(newHealth);

        // Effets visuels - larmes de joie du Ghast
        Location playerLoc = player.getLocation().add(0, 1.5, 0);
        World world = player.getWorld();

        // Trouver le Ghast pet pour les effets
        Entity ghastPet = findGhastPet(player);
        Location effectLoc = ghastPet != null ? ghastPet.getLocation() : playerLoc.add(0, 1, 0);

        // Particules de larmes arc-en-ciel
        spawnRainbowTears(world, effectLoc, playerLoc);

        // Particules de soin sur le joueur
        world.spawnParticle(Particle.HEART, playerLoc, 3, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 5, 0.4, 0.4, 0.4, 0);

        // Son de soin
        world.playSound(playerLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 2.0f);
        world.playSound(effectLoc, Sound.ENTITY_GHAST_AMBIENT, 0.4f, 1.8f);
    }

    private Entity findGhastPet(Player player) {
        String ownerTag = "pet_owner_" + player.getUniqueId();
        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof org.bukkit.entity.Ghast
                && entity.getScoreboardTags().contains(ownerTag)) {
                return entity;
            }
        }
        return null;
    }

    private void spawnRainbowTears(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        // Couleurs arc-en-ciel
        Color[] rainbowColors = {
            Color.fromRGB(255, 100, 100),   // Rose
            Color.fromRGB(255, 200, 100),   // Orange clair
            Color.fromRGB(255, 255, 150),   // Jaune
            Color.fromRGB(150, 255, 150),   // Vert clair
            Color.fromRGB(150, 200, 255),   // Bleu clair
            Color.fromRGB(200, 150, 255)    // Violet clair
        };

        for (double d = 0; d < distance; d += 0.4) {
            Location point = from.clone().add(direction.clone().multiply(d));

            // Choisir une couleur arc-en-ciel
            Color color = rainbowColors[(int)(d * 2) % rainbowColors.length];
            Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f);

            world.spawnParticle(Particle.DUST, point, 1, 0.05, 0.05, 0.05, dust);

            // Petites étoiles scintillantes
            if (d % 0.8 < 0.4) {
                world.spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }
}

// ==================== CHAOS ====================

@Getter
class ChaosPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int intervalSeconds;
    private final Map<UUID, Long> lastEffect = new HashMap<>();
    private final Random random = new Random();

    ChaosPassive(String id, String name, String desc, int interval) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.intervalSeconds = interval;
        PassiveAbilityCleanup.registerForCleanup(lastEffect);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastEffect.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalSeconds * 1000L))
            return;
        lastEffect.put(player.getUniqueId(), now);

        // Effet aléatoire
        int effect = random.nextInt(6);
        int duration = (int) (100 * petData.getStatMultiplier()); // 5 secondes de base

        switch (effect) {
            case 0 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
            case 1 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
            case 2 ->
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, false, false));
            case 3 -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 0, false, false));
            case 4 -> {
                // Dégâts de zone
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof Monster m) {
                        m.damage(10 * petData.getStatMultiplier(), player);
                    }
                }
            }
            case 5 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 1, false, false));
        }

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.5);
    }
}

// ==================== IMMUNITÉ DÉBUFFS ====================

@Getter
class DebuffImmunityPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private static final double ALLY_SHARE_RADIUS = 8.0; // Rayon pour partager l'immunité

    DebuffImmunityPassive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    /**
     * Vérifie si le bonus de partage aux alliés est actif (niveau 5+)
     */
    private boolean shouldShareWithAllies(PetData petData) {
        return petData.getStatMultiplier() >= 1.5; // Niveau 5+
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Retirer les effets négatifs du joueur
        removeDebuffs(player);

        // Niveau 5+ : Partager l'immunité aux alliés proches
        if (shouldShareWithAllies(petData)) {
            for (Entity entity : player.getNearbyEntities(ALLY_SHARE_RADIUS, ALLY_SHARE_RADIUS, ALLY_SHARE_RADIUS)) {
                if (entity instanceof Player ally && !ally.equals(player)) {
                    removeDebuffs(ally);
                }
            }
        }
    }

    /**
     * Retire tous les effets négatifs d'un joueur
     */
    private void removeDebuffs(Player player) {
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.LEVITATION);
        player.removePotionEffect(PotionEffectType.BAD_OMEN);
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }
}

// ==================== TÉLÉPORTATION SUR DÉGÂTS ====================

@Getter
class TeleportOnDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int teleportDistance;
    private final int cooldownSeconds;
    private final Map<UUID, Long> lastTeleport = new HashMap<>();

    TeleportOnDamagePassive(String id, String name, String desc, int distance, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.teleportDistance = distance;
        this.cooldownSeconds = cooldown;
        PassiveAbilityCleanup.registerForCleanup(lastTeleport);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        long now = System.currentTimeMillis();
        long last = lastTeleport.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (cooldownSeconds * 1000L))
            return;
        lastTeleport.put(player.getUniqueId(), now);

        // Téléportation aléatoire
        Location loc = player.getLocation();
        double angle = Math.random() * 2 * Math.PI;
        int dist = (int) (teleportDistance * petData.getStatMultiplier());
        Location newLoc = loc.clone().add(
                Math.cos(angle) * dist,
                0,
                Math.sin(angle) * dist);

        // Trouver une position sûre
        newLoc = findSafeLocation(newLoc);
        if (newLoc != null) {
            player.teleport(newLoc);
            player.getWorld().spawnParticle(Particle.PORTAL, loc, 30, 0.5, 1, 0.5, 0);
            player.getWorld().spawnParticle(Particle.PORTAL, newLoc, 30, 0.5, 1, 0.5, 0);
            player.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    private Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return null;

        int y = loc.getBlockY();
        for (int i = 0; i <= 5; i++) {
            Location check = loc.clone();
            check.setY(y + i);
            if (world.getBlockAt(check).isPassable() &&
                    world.getBlockAt(check.clone().add(0, 1, 0)).isPassable()) {
                return check;
            }
        }
        return loc;
    }
}

// ==================== DÉGÂTS MÊLÉE ====================

// ==================== RÉGÉNÉRATION AVANCÉE ====================

@Getter
class AdvancedRegenPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healPerTick;
    private final int intervalTicks;
    private final double healEfficiencyBonus;
    private final Map<UUID, Long> lastHeal = new HashMap<>();

    AdvancedRegenPassive(String id, String name, String desc, double heal, int interval, double efficiency) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healPerTick = heal;
        this.intervalTicks = interval;
        this.healEfficiencyBonus = efficiency;
        PassiveAbilityCleanup.registerForCleanup(lastHeal);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastHeal.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50))
            return;
        lastHeal.put(player.getUniqueId(), now);

        double adjustedHeal = healPerTick * petData.getStatMultiplier();
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + adjustedHeal);
        player.setHealth(newHealth);
    }
}

// ==================== RENAISSANCE AVANCÉE ====================

@Getter
class AdvancedRebornPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healthPercent;
    private final Map<UUID, Boolean> usedThisLife = new HashMap<>();

    AdvancedRebornPassive(String id, String name, String desc, double health) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healthPercent = health;
        PassiveAbilityCleanup.registerForCleanup(usedThisLife);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    public boolean canReborn(UUID uuid) {
        return !usedThisLife.getOrDefault(uuid, false);
    }

    public void useReborn(UUID uuid) {
        usedThisLife.put(uuid, true);
    }

    public void resetReborn(UUID uuid) {
        usedThisLife.put(uuid, false);
    }
}

// ==================== EXÉCUTION ====================

@Getter
class ExecutionPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double executeThreshold;

    ExecutionPassive(String id, String name, String desc, double threshold) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.executeThreshold = threshold;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
        double threshold = executeThreshold * petData.getStatMultiplier();
        double healthPercent = target.getHealth() / target.getMaxHealth();

        if (healthPercent <= threshold) {
            target.setHealth(0);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation(), 30, 0.5, 1, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 2.0f);
            petData.addKill();
        }
    }
}

// ==================== DÉGÂTS VRAIS ====================

@Getter
class TrueDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double trueDamagePercent;

    TrueDamagePassive(String id, String name, String desc, double percent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.trueDamagePercent = percent;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}

// ==================== BOOST VITESSE ====================

@Getter
class SpeedBoostPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double speedBoost;

    SpeedBoostPassive(String id, String name, String desc, double boost) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.speedBoost = boost;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        // Vitesse de déplacement
        float newSpeed = (float) Math.min(1.0, 0.2 + (0.2 * speedBoost * petData.getStatMultiplier()));
        player.setWalkSpeed(newSpeed);

        // Vitesse d'attaque via potion
        int level = (int) (speedBoost * 10 * petData.getStatMultiplier());
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, Math.min(level, 2), false, false));
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.HASTE);
    }
}

// ==================== MULTI-ATTAQUE ====================

// ==================== PUISSANCE ET LENTEUR ====================

// ==================== RAFALES DE VENT (BREEZE) ====================

@Getter
class WindGustPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double procChance;          // 25% de chance
    private final double damageBonus;         // +15% dégâts sur le proc
    private final double knockbackStrength;   // Force du knockback

    WindGustPassive(String id, String name, String desc, double chance, double bonus, double knockback) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = chance;
        this.damageBonus = bonus;
        this.knockbackStrength = knockback;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        // 25% de chance de déclencher une rafale
        if (Math.random() < procChance * petData.getStatMultiplier()) {
            World world = player.getWorld();
            Location targetLoc = target.getLocation();

            // Appliquer le knockback à la cible
            org.bukkit.util.Vector direction = targetLoc.toVector()
                .subtract(player.getLocation().toVector())
                .normalize()
                .multiply(knockbackStrength);
            direction.setY(0.4); // Légère élévation
            target.setVelocity(direction);

            // Repousser les ennemis proches aussi
            for (Entity entity : target.getNearbyEntities(3, 2, 3)) {
                if (entity instanceof Monster m && m != target) {
                    org.bukkit.util.Vector pushDir = m.getLocation().toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(knockbackStrength * 0.7);
                    pushDir.setY(0.3);
                    m.setVelocity(pushDir);
                }
            }

            // Effets visuels - particules de vent
            world.spawnParticle(Particle.CLOUD, targetLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.SWEEP_ATTACK, targetLoc, 5, 0.3, 0.3, 0.3, 0);

            // Sons de vent (Breeze sounds)
            world.playSound(targetLoc, Sound.ENTITY_BREEZE_INHALE, 0.8f, 1.2f);
            world.playSound(targetLoc, Sound.ENTITY_BREEZE_WIND_BURST, 0.6f, 1.5f);

            // Bonus de dégâts
            return damage * (1 + damageBonus);
        }
        return damage;
    }
}

// PassiveAbilityCleanup class moved to its own file
// (PassiveAbilityCleanup.java)
