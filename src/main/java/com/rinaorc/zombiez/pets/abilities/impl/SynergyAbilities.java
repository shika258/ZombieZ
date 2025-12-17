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
 * Implémentations des capacités de synergie des nouveaux pets
 * Ces abilities interagissent avec les systèmes de classe, talents, et mécaniques de jeu
 */

// ==================== COMBO SYSTEM (Scarabée de Combo) ====================

@Getter
public class ComboPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerKill;
    private final double maxBonus;
    private final Map<UUID, Integer> comboStacks = new HashMap<>();
    private final Map<UUID, Long> lastKillTime = new HashMap<>();

    public ComboPassive(String id, String name, String desc, double perKill, double max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerKill = perKill;
        this.maxBonus = max;
        PassiveAbilityCleanup.registerForCleanup(comboStacks);
        PassiveAbilityCleanup.registerForCleanup(lastKillTime);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastKillTime.getOrDefault(uuid, 0L);

        // Reset si plus de 5 secondes sans kill
        if (now - last > 5000) {
            comboStacks.put(uuid, 0);
        }

        int currentStacks = comboStacks.getOrDefault(uuid, 0);
        int maxStacks = (int) (maxBonus / damagePerKill);
        if (currentStacks < maxStacks) {
            comboStacks.put(uuid, currentStacks + 1);
        }

        lastKillTime.put(uuid, now);

        // Effet visuel selon le niveau de combo
        if (currentStacks > 10) {
            player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        int stacks = comboStacks.getOrDefault(player.getUniqueId(), 0);
        double bonus = stacks * damagePerKill * petData.getStatMultiplier();
        return damage * (1 + bonus);
    }

    public int getComboStacks(UUID uuid) {
        return comboStacks.getOrDefault(uuid, 0);
    }

    public void consumeCombo(UUID uuid) {
        comboStacks.put(uuid, 0);
    }
}

@Getter
public class ComboExplosionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final ComboPassive comboPassive;

    public ComboExplosionActive(String id, String name, String desc, ComboPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.comboPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int stacks = comboPassive.getComboStacks(player.getUniqueId());
        if (stacks < 5) {
            player.sendMessage("§c[Pet] §7Pas assez de combo! (§e" + stacks + "§7/5 minimum)");
            return false;
        }

        double damage = stacks * 5 * petData.getStatMultiplier();
        comboPassive.consumeCombo(player.getUniqueId());

        Collection<Entity> nearby = player.getNearbyEntities(8, 8, 8);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                petData.addDamage((long) damage);
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 5, 3, 1, 3, 0);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 4, 1, 4, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        player.sendMessage("§a[Pet] §6Explosion de Combo! §c" + (int)damage + " §7dégâts! (§e" + stacks + " §7stacks)");

        return true;
    }
}

// ==================== LIFESTEAL (Larve Parasitaire) ====================

@Getter
public class LifestealPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double lifestealPercent;

    public LifestealPassive(String id, String name, String desc, double percent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.lifestealPercent = percent;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        double lifesteal = damage * (lifestealPercent / 100.0) * petData.getStatMultiplier();
        double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + lifesteal);
        player.setHealth(newHealth);

        if (lifesteal > 0.5) {
            player.spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 1, 0.2, 0.2, 0.2, 0);
        }

        return damage;
    }
}

@Getter
public class FeastActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healPercent;
    private final Map<UUID, Boolean> feastReady = new HashMap<>();

    public FeastActive(String id, String name, String desc, double heal) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healPercent = heal;
        PassiveAbilityCleanup.registerForCleanup(feastReady);
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        feastReady.put(player.getUniqueId(), true);
        player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);
        player.sendMessage("§a[Pet] §7Festin préparé! Le prochain kill soigne §c" + (int)(healPercent) + "% §7HP!");

        // Auto-expiration après 30s
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> feastReady.remove(player.getUniqueId()),
            600L
        );
        return true;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        if (feastReady.getOrDefault(player.getUniqueId(), false)) {
            feastReady.remove(player.getUniqueId());
            double heal = player.getMaxHealth() * (healPercent / 100.0) * petData.getStatMultiplier();
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            player.sendMessage("§a[Pet] §2Festin! §7Soigné de §c" + (int)heal + "❤");
        }
    }
}

// ==================== RAGE STACKING (Esprit de Rage - Guerrier) ====================

@Getter
public class RageStackPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerStack;
    private final double maxStacks;
    private final Map<UUID, Integer> rageStacks = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();

    public RageStackPassive(String id, String name, String desc, double perStack, double max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerStack = perStack;
        this.maxStacks = max;
        PassiveAbilityCleanup.registerForCleanup(rageStacks);
        PassiveAbilityCleanup.registerForCleanup(lastHitTime);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastHitTime.getOrDefault(uuid, 0L);

        // Reset si plus de 5 secondes sans attaque
        if (now - last > 5000) {
            rageStacks.put(uuid, 0);
        }

        int currentStacks = rageStacks.getOrDefault(uuid, 0);
        int maxS = (int) (maxStacks / damagePerStack);
        if (currentStacks < maxS) {
            rageStacks.put(uuid, currentStacks + 1);
        }
        lastHitTime.put(uuid, now);

        double bonus = currentStacks * damagePerStack * petData.getStatMultiplier();

        // Effet visuel de rage
        if (currentStacks > 5) {
            player.spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 1);
        }

        return damage * (1 + bonus);
    }

    public int getRageStacks(UUID uuid) {
        return rageStacks.getOrDefault(uuid, 0);
    }
}

@Getter
public class UnleashActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final RageStackPassive ragePassive;
    private final Map<UUID, Long> unleashEnd = new HashMap<>();

    public UnleashActive(String id, String name, String desc, RageStackPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.ragePassive = passive;
        PassiveAbilityCleanup.registerForCleanup(unleashEnd);
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int stacks = ragePassive.getRageStacks(player.getUniqueId());
        unleashEnd.put(player.getUniqueId(), System.currentTimeMillis() + 5000);

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, stacks / 5, false, true));
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 30, 1, 1, 1, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.0f);
        player.sendMessage("§a[Pet] §c§lDÉCHAÎNEMENT! §7Stacks doublés pendant 5s! (§e" + stacks + " §7stacks)");

        return true;
    }

    public boolean isUnleashed(UUID uuid) {
        Long end = unleashEnd.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }
}

// ==================== MARK SYSTEM (Faucon Chasseur - Chasseur) ====================

@Getter
public class MarkPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double bonusDamage;
    private final Set<UUID> markedEntities = new HashSet<>();

    public MarkPassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.bonusDamage = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        // Marquer la cible
        if (!markedEntities.contains(target.getUniqueId())) {
            markedEntities.add(target.getUniqueId());
            target.setGlowing(true);

            // Expire après 10 secondes
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    markedEntities.remove(target.getUniqueId());
                    if (target.isValid()) target.setGlowing(false);
                },
                200L
            );
        }

        // Bonus de dégâts sur cibles marquées
        if (markedEntities.contains(target.getUniqueId())) {
            return damage * (1 + bonusDamage * petData.getStatMultiplier());
        }

        return damage;
    }

    public boolean isMarked(UUID entityId) {
        return markedEntities.contains(entityId);
    }
}

@Getter
public class PredatorStrikeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final MarkPassive markPassive;

    public PredatorStrikeActive(String id, String name, String desc, MarkPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.markPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Trouver la cible marquée la plus proche
        LivingEntity target = null;
        double minDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof Monster monster && markPassive.isMarked(monster.getUniqueId())) {
                double dist = entity.getLocation().distance(player.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    target = monster;
                }
            }
        }

        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucune cible marquée!");
            return false;
        }

        double damage = 50 * petData.getStatMultiplier();
        target.damage(damage, player);
        petData.addDamage((long) damage);

        // Effet visuel
        Location from = player.getLocation().add(0, 1, 0);
        Location to = target.getLocation().add(0, 1, 0);
        Vector direction = to.toVector().subtract(from.toVector()).normalize();

        for (int i = 0; i < (int) minDist * 2; i++) {
            Location loc = from.clone().add(direction.clone().multiply(i * 0.5));
            player.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.1, 0.1, 0.1, 0);
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, to, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
        player.sendMessage("§a[Pet] §6Frappe Prédatrice! §c" + (int)damage + " §7dégâts!");

        return true;
    }
}

// ==================== SOUL ORB SYSTEM (Orbe d'Âmes - Occultiste) ====================

@Getter
public class SoulOrbPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double skillBonusPerOrb;
    private final int maxOrbs;
    private final Map<UUID, Integer> soulOrbs = new HashMap<>();

    public SoulOrbPassive(String id, String name, String desc, double bonus, int max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.skillBonusPerOrb = bonus;
        this.maxOrbs = max;
        PassiveAbilityCleanup.registerForCleanup(soulOrbs);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();
        int current = soulOrbs.getOrDefault(uuid, 0);
        int maxO = (int) (maxOrbs * petData.getStatMultiplier());

        if (current < maxO) {
            soulOrbs.put(uuid, current + 1);
            player.spawnParticle(Particle.WITCH, killed.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        }
    }

    public int getSoulOrbs(UUID uuid) {
        return soulOrbs.getOrDefault(uuid, 0);
    }

    public void consumeOrbs(UUID uuid) {
        soulOrbs.put(uuid, 0);
    }

    public double getSkillBonus(UUID uuid, double basePetMultiplier) {
        return soulOrbs.getOrDefault(uuid, 0) * skillBonusPerOrb * basePetMultiplier;
    }
}

@Getter
public class SoulReleaseActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerOrb;
    private final SoulOrbPassive soulPassive;

    public SoulReleaseActive(String id, String name, String desc, double damage, SoulOrbPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerOrb = damage;
        this.soulPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 15; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int orbs = soulPassive.getSoulOrbs(player.getUniqueId());
        if (orbs < 1) {
            player.sendMessage("§c[Pet] §7Aucune orbe d'âme!");
            return false;
        }

        soulPassive.consumeOrbs(player.getUniqueId());
        double totalDamage = orbs * damagePerOrb * petData.getStatMultiplier();

        Collection<Entity> nearby = player.getNearbyEntities(10, 10, 10);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(totalDamage, player);
                petData.addDamage((long) totalDamage);

                // Effet visuel d'âme
                Location from = player.getLocation().add(0, 1, 0);
                Location to = monster.getLocation().add(0, 1, 0);
                Vector dir = to.toVector().subtract(from.toVector()).normalize();
                for (int i = 0; i < 10; i++) {
                    Location loc = from.clone().add(dir.clone().multiply(i * 0.5));
                    player.getWorld().spawnParticle(Particle.WITCH, loc, 3, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 50 * orbs, 3, 2, 3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);
        player.sendMessage("§a[Pet] §5Libération d'Âmes! §c" + (int)totalDamage + " §7dégâts (§e" + orbs + " §7orbes)");

        return true;
    }
}

// ==================== ELEMENTAL ROTATION (Salamandre Élémentaire) ====================

@Getter
public class ElementalRotationPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double elementBonus;
    private final Map<UUID, Integer> currentElement = new HashMap<>(); // 0=Fire, 1=Ice, 2=Lightning
    private final Map<UUID, Long> lastRotation = new HashMap<>();

    public ElementalRotationPassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.elementBonus = bonus;
        PassiveAbilityCleanup.registerForCleanup(currentElement);
        PassiveAbilityCleanup.registerForCleanup(lastRotation);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastRotation.getOrDefault(uuid, 0L);

        // Rotation toutes les 10 secondes
        if (now - last > 10000) {
            int current = currentElement.getOrDefault(uuid, 0);
            currentElement.put(uuid, (current + 1) % 3);
            lastRotation.put(uuid, now);

            String element = switch (currentElement.get(uuid)) {
                case 0 -> "§c🔥 Feu";
                case 1 -> "§b❄ Glace";
                case 2 -> "§e⚡ Foudre";
                default -> "§7?";
            };
            player.sendMessage("§a[Pet] §7Élément: " + element);
        }

        // Particules selon l'élément
        Particle particle = switch (currentElement.getOrDefault(uuid, 0)) {
            case 0 -> Particle.FLAME;
            case 1 -> Particle.SNOWFLAKE;
            case 2 -> Particle.ELECTRIC_SPARK;
            default -> Particle.CRIT;
        };
        player.spawnParticle(particle, player.getLocation().add(0, 0.5, 0), 2, 0.3, 0.2, 0.3, 0.01);
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        int element = currentElement.getOrDefault(player.getUniqueId(), 0);
        double bonus = 1 + (elementBonus * petData.getStatMultiplier());

        // Appliquer l'effet élémentaire
        switch (element) {
            case 0 -> target.setFireTicks(60); // Feu
            case 1 -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false)); // Glace
            case 2 -> { // Foudre - chain damage
                for (Entity e : target.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof Monster m && e != target) {
                        m.damage(damage * 0.3, player);
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, m.getLocation(), 10);
                    }
                }
            }
        }

        return damage * bonus;
    }

    public int getCurrentElement(UUID uuid) {
        return currentElement.getOrDefault(uuid, 0);
    }
}

@Getter
public class ElementalFusionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    public ElementalFusionActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double damage = 30 * petData.getStatMultiplier();

        Collection<Entity> nearby = player.getNearbyEntities(8, 8, 8);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                // Triple effet
                monster.damage(damage, player);
                monster.setFireTicks(100);
                monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1, false, false));
                petData.addDamage((long) damage);

                // Particules fusionnées
                monster.getWorld().spawnParticle(Particle.FLAME, monster.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
                monster.getWorld().spawnParticle(Particle.SNOWFLAKE, monster.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
                monster.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, monster.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
            }
        }

        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3, 2, 1, 2, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §6§lFusion Élémentaire! §7Brûle, gèle et électrocute!");

        return true;
    }
}

// ==================== VENGEANCE SYSTEM (Spectre de Vengeance) ====================

@Getter
public class VengeancePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double accumulationPercent;
    private final double maxRage;
    private final Map<UUID, Double> vengeanceRage = new HashMap<>();

    public VengeancePassive(String id, String name, String desc, double percent, double max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.accumulationPercent = percent;
        this.maxRage = max;
        PassiveAbilityCleanup.registerForCleanup(vengeanceRage);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        UUID uuid = player.getUniqueId();
        double current = vengeanceRage.getOrDefault(uuid, 0.0);
        double added = damage * (accumulationPercent / 100.0) * petData.getStatMultiplier();
        double maxR = maxRage * petData.getStatMultiplier();

        vengeanceRage.put(uuid, Math.min(maxR, current + added));

        if (added > 5) {
            player.spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        double rage = vengeanceRage.getOrDefault(uuid, 0.0);

        if (rage > 0) {
            vengeanceRage.put(uuid, 0.0);
            player.sendMessage("§a[Pet] §c+" + (int)rage + " §7dégâts de Rage!");
            return damage + rage;
        }

        return damage;
    }

    public double getRage(UUID uuid) {
        return vengeanceRage.getOrDefault(uuid, 0.0);
    }
}

@Getter
public class VengeanceExplosionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final VengeancePassive vengeancePassive;

    public VengeanceExplosionActive(String id, String name, String desc, VengeancePassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.vengeancePassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 15; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double rage = vengeancePassive.getRage(player.getUniqueId());
        if (rage < 20) {
            player.sendMessage("§c[Pet] §7Pas assez de Rage! (§e" + (int)rage + "§7/20 minimum)");
            return false;
        }

        vengeancePassive.vengeanceRage.put(player.getUniqueId(), 0.0);

        Collection<Entity> nearby = player.getNearbyEntities(8, 8, 8);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(rage, player);
                petData.addDamage((long) rage);
            }
        }

        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation(), 50, 5, 2, 5, 0);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation(), 30, 4, 1, 4, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, 0.5f, 1.5f);
        player.sendMessage("§a[Pet] §c§lExplosion de Vengeance! §7" + (int)rage + " dégâts!");

        return true;
    }
}

// ==================== JACKPOT SYSTEM (Djinn du Jackpot) ====================

@Getter
public class JackpotPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double jackpotChanceBonus;
    private final double jackpotRewardBonus;

    public JackpotPassive(String id, String name, String desc, double chanceBonus, double rewardBonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.jackpotChanceBonus = chanceBonus;
        this.jackpotRewardBonus = rewardBonus;
    }

    @Override
    public boolean isPassive() { return true; }

    public double getJackpotChanceBonus(PetData petData) {
        return jackpotChanceBonus * petData.getStatMultiplier();
    }

    public double getJackpotRewardBonus(PetData petData) {
        return jackpotRewardBonus * petData.getStatMultiplier();
    }
}

@Getter
public class SuperJackpotActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    public SuperJackpotActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 90; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Déclenche un jackpot garanti - les récompenses sont gérées par le système de jackpot
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 100, 2, 2, 2, 0.5);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Donner des récompenses directement
        int emeralds = (int) (10 + Math.random() * 20 * petData.getStatMultiplier());
        int gold = (int) (20 + Math.random() * 40 * petData.getStatMultiplier());

        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EMERALD, emeralds));
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.GOLD_INGOT, gold));

        player.sendMessage("§a[Pet] §6§l★ SUPER JACKPOT! §7Récompenses x3!");
        player.sendMessage("§7   §a+" + emeralds + " Émeraudes, §6+" + gold + " Or");

        return true;
    }
}

// ==================== CLASS ADAPTIVE (Dragon Chromatique) ====================

@Getter
public class ClassAdaptivePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double classBonus;

    public ClassAdaptivePassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.classBonus = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    // Les bonus sont appliqués via le ClassManager
    public double getClassBonus(PetData petData) {
        return classBonus * petData.getStatMultiplier();
    }
}

@Getter
public class ChromaticBreathActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    public ChromaticBreathActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double damage = 60 * petData.getStatMultiplier();
        Vector direction = player.getLocation().getDirection().normalize();

        // L'attaque s'adapte à la classe (détection via ClassManager serait idéale)
        // Ici on fait un effet générique puissant

        // Onde de choc en cône
        for (int i = 1; i <= 12; i++) {
            final int distance = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    Location loc = player.getLocation().add(direction.clone().multiply(distance)).add(0, 1, 0);
                    loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 1, 0.5, 1, 0.05);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.5, 0.5, 0.1);

                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                        if (entity instanceof Monster monster) {
                            monster.damage(damage / 3, player);
                            petData.addDamage((long) (damage / 3));
                        }
                    }
                },
                i * 2L
            );
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
        player.sendMessage("§a[Pet] §5§lSouffle Chromatique! §7Adapté à votre classe!");

        return true;
    }
}

// ==================== ZONE ADAPTATION (Sentinelle des Zones) ====================

@Getter
public class ZoneAdaptPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double zoneBonus;

    public ZoneAdaptPassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.zoneBonus = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    // Les bonus sont appliqués via le ZoneManager
    public double getZoneBonus(PetData petData) {
        return zoneBonus * petData.getStatMultiplier();
    }
}

@Getter
public class ZoneMasteryActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;

    public ZoneMasteryActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 60; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int duration = (int) (200 * petData.getStatMultiplier());

        // Immunité aux effets environnementaux
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 1, false, true));

        // Boost de stats
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, true));

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 100, 2, 2, 2, 1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §b§lMaîtrise de Zone! §7Immunité environnementale + bonus x2!");

        return true;
    }
}

// ==================== SYMBIOTE (Symbiote Éternel) ====================

@Getter
public class SymbiotePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double amplification;

    public SymbiotePassive(String id, String name, String desc, double amp) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.amplification = amp;
    }

    @Override
    public boolean isPassive() { return true; }

    public double getAmplification(PetData petData) {
        return amplification * petData.getStatMultiplier();
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        return damage * (1 + getAmplification(petData));
    }
}

@Getter
public class SymbioticFusionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final Map<UUID, Long> fusionEnd = new HashMap<>();

    public SymbioticFusionActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        PassiveAbilityCleanup.registerForCleanup(fusionEnd);
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 90; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int duration = (int) (300 * petData.getStatMultiplier());
        fusionEnd.put(player.getUniqueId(), System.currentTimeMillis() + duration * 50L);

        // Buffs massifs
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 3, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 2, false, true));

        // Effet visuel continu
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.spawnParticle(Particle.WITCH, player.getLocation(), 10, 0.5, 1, 0.5, 0.02);
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 100, 1, 2, 1, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 2.0f);
        player.sendMessage("§a[Pet] §5§l★ FUSION SYMBIOTIQUE! §7Tous les bonus x2 pendant 15s!");

        return true;
    }

    public boolean isFused(UUID uuid) {
        Long end = fusionEnd.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }
}

// ==================== NEXUS (Nexus Dimensionnel - Support) ====================

@Getter
public class NexusAuraPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double allyBonus;
    private final double enemyDebuff;
    private final int radius;

    public NexusAuraPassive(String id, String name, String desc, double ally, double enemy, int rad) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.allyBonus = ally;
        this.enemyDebuff = enemy;
        this.radius = rad;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        int adjustedRadius = (int) (radius * petData.getStatMultiplier());

        // Débuff aux ennemis
        for (Entity entity : player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster) {
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, 40, 0, false, false));
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 40, 0, false, false));
            }
        }

        // Particules d'aura
        Location loc = player.getLocation();
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            Location particleLoc = loc.clone().add(
                Math.cos(rad) * adjustedRadius * 0.8,
                0.5,
                Math.sin(rad) * adjustedRadius * 0.8
            );
            player.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0, 0, 0, 0);
        }
    }

    public double getAllyBonus(PetData petData) {
        return allyBonus * petData.getStatMultiplier();
    }
}

@Getter
public class DimensionalConvergenceActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double shieldAmount;

    public DimensionalConvergenceActive(String id, String name, String desc, double shield) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.shieldAmount = shield;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 75; }

    @Override
    public boolean activate(Player player, PetData petData) {
        double adjustedShield = shieldAmount * petData.getStatMultiplier();
        Location destination = player.getLocation();

        // Téléporter tous les alliés proches et leur donner un bouclier
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof Player ally && ally != player) {
                if (ally.getLocation().distance(player.getLocation()) < 50) {
                    // Téléporter
                    Location oldLoc = ally.getLocation();
                    ally.teleport(destination.clone().add(Math.random() * 3 - 1.5, 0, Math.random() * 3 - 1.5));

                    // Effet de téléportation
                    oldLoc.getWorld().spawnParticle(Particle.PORTAL, oldLoc, 30, 0.5, 1, 0.5, 0.5);

                    // Bouclier (via Resistance)
                    ally.addPotionEffect(new PotionEffect(
                        PotionEffectType.ABSORPTION, 200, 4, false, true));
                    ally.sendMessage("§a[Pet] §7Téléporté vers " + player.getName() + " avec un bouclier!");
                }
            }
        }

        // Bouclier pour le joueur aussi
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.ABSORPTION, 200, 4, false, true));

        player.getWorld().spawnParticle(Particle.END_ROD, destination, 100, 3, 2, 3, 0.1);
        player.getWorld().spawnParticle(Particle.PORTAL, destination, 200, 5, 1, 5, 1);
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.sendMessage("§a[Pet] §5§lConvergence Dimensionnelle! §7Alliés téléportés + boucliers!");

        return true;
    }
}
