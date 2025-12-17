package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Particules lumineuses
        Location loc = player.getLocation().add(0, 1, 0);
        player.spawnParticle(Particle.END_ROD, loc, 2, 0.5, 0.5, 0.5, 0);
    }
}

// ==================== RÉDUCTION DE DÉGÂTS ====================

@Getter
class DamageReductionPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double reductionPercent;

    DamageReductionPassive(String id, String name, String desc, double reduction) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.reductionPercent = reduction;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
}

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
    public boolean isPassive() { return true; }

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
    private final Map<UUID, Long> lastAttack = new HashMap<>();

    AttackPassive(String id, String name, String desc, double damage, int interval) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damage = damage;
        this.intervalTicks = interval;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastAttack.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50)) return;
        lastAttack.put(player.getUniqueId(), now);

        double adjustedDamage = damage * petData.getStatMultiplier();
        Collection<Entity> nearby = player.getNearbyEntities(3, 3, 3);

        for (Entity entity : nearby) {
            if (entity instanceof Monster monster && monster.isValid()) {
                monster.damage(adjustedDamage, player);
                petData.addDamage((long) adjustedDamage);

                // Effet visuel
                monster.getWorld().spawnParticle(Particle.CRIT, monster.getLocation().add(0, 1, 0), 5);
                break; // Une seule cible par tick
            }
        }
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
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastHeal.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50)) return;
        lastHeal.put(player.getUniqueId(), now);

        double adjustedHeal = healAmount * petData.getStatMultiplier();
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + adjustedHeal);
        player.setHealth(newHealth);

        // Effet visuel
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 1);
    }
}

// ==================== INTERCEPTION ====================

@Getter
class InterceptPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double interceptPercent;

    InterceptPassive(String id, String name, String desc, double intercept) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.interceptPercent = intercept;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
}

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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

    @Override
    public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
        int adjustedDuration = (int) (slowDurationTicks * petData.getStatMultiplier());
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, adjustedDuration, 1, false, false));
    }
}

// ==================== RENAISSANCE ====================

@Getter
class RebornPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healthPercent;
    private final int cooldownSeconds;
    private final Map<UUID, Long> lastReborn = new HashMap<>();

    RebornPassive(String id, String name, String desc, double health, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healthPercent = health;
        this.cooldownSeconds = cooldown;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        if (player.getHealth() - damage > 0) return; // Pas mort

        long now = System.currentTimeMillis();
        long last = lastReborn.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (cooldownSeconds * 1000L)) return;
        lastReborn.put(player.getUniqueId(), now);

        // Sauver de la mort
        double adjustedHealth = player.getMaxHealth() * healthPercent * petData.getStatMultiplier();
        player.setHealth(Math.max(1, adjustedHealth));

        // Effet visuel
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 1, 1, 1, 0.1);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

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
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastDamage.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 1000) return; // 1 seconde
        lastDamage.put(player.getUniqueId(), now);

        double adjustedDamage = damagePerSecond * petData.getStatMultiplier();
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        Collection<Entity> nearby = player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(adjustedDamage, player);
                petData.addDamage((long) adjustedDamage);
            }
        }
    }
}

// ==================== PARADE ====================

@Getter
class ParryPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldownSeconds;
    private final Map<UUID, Long> lastParry = new HashMap<>();

    ParryPassive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldownSeconds = cooldown;
    }

    @Override
    public boolean isPassive() { return true; }

    public boolean canParry(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastParry.getOrDefault(uuid, 0L);
        return now - last >= (cooldownSeconds * 1000L);
    }

    public void triggerParry(UUID uuid) {
        lastParry.put(uuid, System.currentTimeMillis());
    }
}

// ==================== MULTIPLICATEUR DE DÉGÂTS ====================

@Getter
class DamageMultiplierPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double multiplier;

    DamageMultiplierPassive(String id, String name, String desc, double mult) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.multiplier = mult;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
}

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
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        if (!(killed instanceof Monster)) return;

        double adjustedChance = convertChance * petData.getStatMultiplier();
        if (Math.random() < adjustedChance) {
            // Particules de conversion
            killed.getWorld().spawnParticle(Particle.WITCH, killed.getLocation(), 30, 0.5, 1, 0.5, 0);
            // La logique de minion est gérée ailleurs
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
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via onEquip/onUnequip
}

// ==================== DÉGÂTS CRITIQUES ====================

@Getter
class CritDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double critBonus;

    CritDamagePassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.critBonus = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
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
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastEffect.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalSeconds * 1000L)) return;
        lastEffect.put(player.getUniqueId(), now);

        // Effet aléatoire
        int effect = random.nextInt(6);
        int duration = (int) (100 * petData.getStatMultiplier()); // 5 secondes de base

        switch (effect) {
            case 0 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, false));
            case 1 -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, false));
            case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1, false, false));
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

    DebuffImmunityPassive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Retirer les effets négatifs
        player.removePotionEffect(PotionEffectType.POISON);
        player.removePotionEffect(PotionEffectType.WITHER);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NAUSEA);
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
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        long now = System.currentTimeMillis();
        long last = lastTeleport.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (cooldownSeconds * 1000L)) return;
        lastTeleport.put(player.getUniqueId(), now);

        // Téléportation aléatoire
        Location loc = player.getLocation();
        double angle = Math.random() * 2 * Math.PI;
        int dist = (int) (teleportDistance * petData.getStatMultiplier());
        Location newLoc = loc.clone().add(
            Math.cos(angle) * dist,
            0,
            Math.sin(angle) * dist
        );

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
        if (world == null) return null;

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

@Getter
class MeleeDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double meleeBonus;

    MeleeDamagePassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.meleeBonus = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
}

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
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        long now = System.currentTimeMillis();
        long last = lastHeal.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < (intervalTicks * 50)) return;
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
    }

    @Override
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

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
    public boolean isPassive() { return true; }

    @Override
    public void onEquip(Player player, PetData petData) {
        // Vitesse de déplacement
        float newSpeed = (float) Math.min(1.0, 0.2 + (0.2 * speedBoost * petData.getStatMultiplier()));
        player.setWalkSpeed(newSpeed);

        // Vitesse d'attaque via potion
        int level = (int) (speedBoost * 10 * petData.getStatMultiplier());
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, Math.min(level, 2), false, false));
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        player.setWalkSpeed(0.2f);
        player.removePotionEffect(PotionEffectType.HASTE);
    }
}

// ==================== MULTI-ATTAQUE ====================

@Getter
class MultiAttackPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attackCount;

    MultiAttackPassive(String id, String name, String desc, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attackCount = count;
    }

    @Override
    public boolean isPassive() { return true; }

    // Le bonus est appliqué via PetCombatListener
}

// ==================== PUISSANCE ET LENTEUR ====================

@Getter
class PowerSlowPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageBonus;
    private final double speedMalus;

    PowerSlowPassive(String id, String name, String desc, double damage, double speed) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageBonus = damage;
        this.speedMalus = speed;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onEquip(Player player, PetData petData) {
        float newSpeed = (float) Math.max(0.05, 0.2 + (0.2 * speedMalus));
        player.setWalkSpeed(newSpeed);
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        player.setWalkSpeed(0.2f);
    }
}
