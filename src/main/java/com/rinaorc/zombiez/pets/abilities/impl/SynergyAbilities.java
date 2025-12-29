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

// ==================== COMBO SYSTEM (Armadillo Combo) ====================

@Getter
class ComboPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerKill;      // Bonus par kill (0.5% = 0.005)
    private final double baseMaxBonus;       // Max bonus de base (5% = 0.05)
    private final int baseTimeoutSeconds;    // Timeout de base (15s)
    private final Map<UUID, Integer> comboStacks = new HashMap<>();
    private final Map<UUID, Long> lastKillTime = new HashMap<>();

    public ComboPassive(String id, String name, String desc, double perKill, double baseMax, int baseTimeout) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerKill = perKill;
        this.baseMaxBonus = baseMax;
        this.baseTimeoutSeconds = baseTimeout;
        PassiveAbilityCleanup.registerForCleanup(comboStacks);
        PassiveAbilityCleanup.registerForCleanup(lastKillTime);
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Calcule le timeout effectif basé sur le niveau du pet
     * Base: 15s, Niveau max: 30s
     */
    private long getEffectiveTimeout(PetData petData) {
        // Le stat multiplier va de 1.0 (niveau 1) à ~2.0 (niveau max)
        double multiplier = petData.getStatMultiplier();
        return (long) (baseTimeoutSeconds * 1000 * multiplier);
    }

    /**
     * Calcule le max bonus effectif basé sur le niveau du pet
     * Base: 5%, Niveau max: 15%
     */
    private double getEffectiveMaxBonus(PetData petData) {
        // Base: 5%, avec stat multiplier max (~2.0) → 10%,
        // mais on veut atteindre 15% donc on triple le base
        return baseMaxBonus * petData.getStatMultiplier() * 1.5;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastKillTime.getOrDefault(uuid, 0L);
        long timeout = getEffectiveTimeout(petData);

        // Reset si timeout dépassé
        if (now - last > timeout) {
            comboStacks.put(uuid, 0);
        }

        double effectiveMax = getEffectiveMaxBonus(petData);
        int currentStacks = comboStacks.getOrDefault(uuid, 0);
        int maxStacks = (int) (effectiveMax / damagePerKill);

        if (currentStacks < maxStacks) {
            comboStacks.put(uuid, currentStacks + 1);
            int newStacks = currentStacks + 1;
            double currentBonus = newStacks * damagePerKill * 100;

            // Notification tous les 5 stacks
            if (newStacks % 5 == 0 || newStacks == maxStacks) {
                player.sendMessage("§a[Pet] §6Combo x" + newStacks + " §7(§e+" + String.format("%.1f", currentBonus) + "% §7dégâts)");
            }
        }

        lastKillTime.put(uuid, now);

        // Effet visuel selon le niveau de combo
        int stacks = comboStacks.get(uuid);
        if (stacks >= 5) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0),
                3 + stacks / 5, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastKillTime.getOrDefault(uuid, 0L);
        long timeout = getEffectiveTimeout(petData);

        // Vérifier si le combo a expiré
        if (now - last > timeout) {
            int oldStacks = comboStacks.getOrDefault(uuid, 0);
            if (oldStacks > 0) {
                comboStacks.put(uuid, 0);
                player.sendMessage("§c[Pet] §7Combo perdu...");
            }
            return damage;
        }

        int stacks = comboStacks.getOrDefault(uuid, 0);
        double bonus = stacks * damagePerKill;
        return damage * (1 + bonus);
    }

    public int getComboStacks(UUID uuid) {
        return comboStacks.getOrDefault(uuid, 0);
    }

    public void consumeCombo(UUID uuid) {
        comboStacks.put(uuid, 0);
    }

    public double getCurrentBonus(UUID uuid) {
        return comboStacks.getOrDefault(uuid, 0) * damagePerKill;
    }
}

@Getter
class ComboExplosionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final ComboPassive comboPassive;
    private final double damagePerStack;  // Dégâts par stack (5 par défaut)
    private final int explosionRadius;    // Rayon de l'explosion (8 blocs)

    public ComboExplosionActive(String id, String name, String desc, ComboPassive passive, double damagePerStack, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.comboPassive = passive;
        this.damagePerStack = damagePerStack;
        this.explosionRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        int stacks = comboPassive.getComboStacks(player.getUniqueId());
        if (stacks < 3) {
            player.sendMessage("§c[Pet] §7Pas assez de combo! (§e" + stacks + "§7/3 minimum)");
            return false;
        }

        // Consommer le combo et calculer les dégâts
        double bonusPercent = comboPassive.getCurrentBonus(player.getUniqueId()) * 100;
        double damage = stacks * damagePerStack * petData.getStatMultiplier();
        comboPassive.consumeCombo(player.getUniqueId());

        // Compter les ennemis touchés
        int enemiesHit = 0;
        Collection<Entity> nearby = player.getNearbyEntities(explosionRadius, explosionRadius, explosionRadius);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                petData.addDamage((long) damage);
                enemiesHit++;

                // Effet visuel sur chaque cible
                monster.getWorld().spawnParticle(Particle.CRIT, monster.getLocation().add(0, 1, 0),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Animation d'explosion concentrique
        for (int ring = 1; ring <= explosionRadius; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    Location center = player.getLocation();
                    for (int angle = 0; angle < 360; angle += 15) {
                        double rad = Math.toRadians(angle);
                        Location loc = center.clone().add(Math.cos(rad) * r, 0.5, Math.sin(rad) * r);
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                    }
                },
                ring * 2L
            );
        }

        // Effets sonores et visuels centraux
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3, 1, 0.5, 1, 0);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 3, 1, 3, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);

        player.sendMessage("§a[Pet] §6§lEXPLOSION DE COMBO! §c" + (int)damage + " §7dégâts × §e" + enemiesHit +
            " §7ennemis (§6" + stacks + " §7stacks, §e+" + String.format("%.1f", bonusPercent) + "%§7)");

        return true;
    }
}

// ==================== LIFESTEAL (Larve Parasitaire) ====================

@Getter
class LifestealPassive implements PetAbility {
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
class FeastActive implements PetAbility {
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
class RageStackPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerStack;      // +2% par stack (0.02)
    private final double maxBonus;            // Max +30% (0.30)
    private final Map<UUID, Integer> rageStacks = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, Long> unleashEnd = new HashMap<>();  // Pour le mode Déchaînement

    public RageStackPassive(String id, String name, String desc, double perStack, double max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerStack = perStack;
        this.maxBonus = max;
        PassiveAbilityCleanup.registerForCleanup(rageStacks);
        PassiveAbilityCleanup.registerForCleanup(lastHitTime);
        PassiveAbilityCleanup.registerForCleanup(unleashEnd);
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
            int oldStacks = rageStacks.getOrDefault(uuid, 0);
            if (oldStacks > 0) {
                rageStacks.put(uuid, 0);
                player.sendMessage("§c[Pet] §7Rage perdue...");
            }
        }

        // Calculer le max de stacks (30% / 2% = 15 stacks)
        int currentStacks = rageStacks.getOrDefault(uuid, 0);
        int maxStacks = (int) (maxBonus / damagePerStack);

        // Ajouter un stack si pas au max
        if (currentStacks < maxStacks) {
            rageStacks.put(uuid, currentStacks + 1);
            currentStacks = currentStacks + 1;

            // Notification tous les 5 stacks
            if (currentStacks % 5 == 0 || currentStacks == maxStacks) {
                double currentBonus = currentStacks * damagePerStack * 100;
                player.sendMessage("§a[Pet] §c🔥 Rage x" + currentStacks + " §7(§e+" +
                    String.format("%.0f", currentBonus) + "% §7dégâts)");
            }
        }
        lastHitTime.put(uuid, now);

        // Calculer le bonus de dégâts
        double bonus = currentStacks * damagePerStack;

        // DÉCHAÎNEMENT: doubler le bonus si actif!
        if (isUnleashed(uuid)) {
            bonus *= 2;

            // Effet visuel intense
            player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.05);
        }

        // Effet visuel de rage (à partir de 5 stacks)
        if (currentStacks >= 5) {
            player.spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 1);
        }

        return damage * (1 + bonus);
    }

    public int getRageStacks(UUID uuid) {
        return rageStacks.getOrDefault(uuid, 0);
    }

    /**
     * Active le mode Déchaînement (stacks doublés)
     */
    public void activateUnleash(UUID uuid, int durationMs) {
        unleashEnd.put(uuid, System.currentTimeMillis() + durationMs);
    }

    /**
     * Vérifie si le joueur est en mode Déchaînement
     */
    public boolean isUnleashed(UUID uuid) {
        Long end = unleashEnd.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }
}

@Getter
class UnleashActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final RageStackPassive ragePassive;
    private final int unleashDurationMs = 5000;  // 5 secondes

    public UnleashActive(String id, String name, String desc, RageStackPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.ragePassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        int stacks = ragePassive.getRageStacks(uuid);

        if (stacks < 1) {
            player.sendMessage("§c[Pet] §7Aucun stack de rage!");
            return false;
        }

        // Activer le mode Déchaînement (double les stacks dans le passif)
        ragePassive.activateUnleash(uuid, unleashDurationMs);

        // Calculer le bonus actuel et doublé
        double currentBonus = stacks * 2; // 2% par stack
        double doubledBonus = currentBonus * 2;

        // Effet visuel explosif
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 50, 1.5, 1, 1.5, 0.1);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 40, 1, 0.5, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);

        // Son de rugissement
        player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);

        player.sendMessage("§a[Pet] §c§l🔥 DÉCHAÎNEMENT! §7Bonus §e+" + (int)currentBonus +
            "% §7→ §c+" + (int)doubledBonus + "% §7pendant 5s!");

        return true;
    }
}

// ==================== MARK SYSTEM (Faucon Chasseur - Chasseur) ====================

@Getter
class MarkPassive implements PetAbility {
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
class PredatorStrikeActive implements PetAbility {
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
class SoulOrbPassive implements PetAbility {
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
class SoulReleaseActive implements PetAbility {
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
class ElementalRotationPassive implements PetAbility {
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
class ElementalFusionActive implements PetAbility {
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
class VengeancePassive implements PetAbility {
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

    public void clearRage(UUID uuid) {
        vengeanceRage.put(uuid, 0.0);
    }
}

@Getter
class VengeanceExplosionActive implements PetAbility {
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

        vengeancePassive.clearRage(player.getUniqueId());

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
class JackpotPassive implements PetAbility {
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
class SuperJackpotActive implements PetAbility {
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
class ClassAdaptivePassive implements PetAbility {
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
class ChromaticBreathActive implements PetAbility {
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
class ZoneAdaptPassive implements PetAbility {
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
class ZoneMasteryActive implements PetAbility {
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
class SymbiotePassive implements PetAbility {
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
class SymbioticFusionActive implements PetAbility {
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

// ==================== CHAMPIGNON EXPLOSIF (Spores Volatiles) ====================

@Getter
class VolatileSporesPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double explosionDamagePercent;  // % des dégâts du kill (0.20 = 20%)
    private final int explosionRadius;             // Rayon de l'explosion (3 blocs)

    public VolatileSporesPassive(String id, String name, String desc, double damagePercent, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.explosionDamagePercent = damagePercent;
        this.explosionRadius = radius;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        Location deathLocation = killed.getLocation();
        World world = deathLocation.getWorld();

        // Calculer les dégâts de l'explosion basés sur la santé max du mob tué
        double killDamage = killed.getMaxHealth();
        double explosionDamage = killDamage * explosionDamagePercent * petData.getStatMultiplier();

        // Rayon ajusté par niveau
        int adjustedRadius = (int) (explosionRadius + (petData.getStatMultiplier() - 1) * 2);

        // Infliger des dégâts aux mobs proches
        int enemiesHit = 0;
        for (Entity entity : world.getNearbyEntities(deathLocation, adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster && entity != killed) {
                monster.damage(explosionDamage, player);
                petData.addDamage((long) explosionDamage);
                enemiesHit++;

                // Effet visuel sur chaque cible
                monster.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                    monster.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
            }
        }

        // Effets visuels de l'explosion de spores
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, deathLocation.add(0, 0.5, 0),
            40, 1.5, 0.5, 1.5, 0.1);
        world.spawnParticle(Particle.CRIMSON_SPORE, deathLocation,
            30, 1, 0.5, 1, 0.02);
        world.spawnParticle(Particle.POOF, deathLocation,
            15, 0.5, 0.3, 0.5, 0.05);

        // Son d'explosion de spores
        world.playSound(deathLocation, Sound.BLOCK_FUNGUS_BREAK, 1.0f, 0.5f);
        world.playSound(deathLocation, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 0.8f, 1.2f);

        // Message si des ennemis sont touchés
        if (enemiesHit > 0) {
            player.sendMessage("§a[Pet] §6Spores Volatiles! §c" + (int)explosionDamage +
                " §7dégâts → §e" + enemiesHit + " §7ennemi" + (enemiesHit > 1 ? "s" : ""));
        }
    }
}

@Getter
class FungalDetonationActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageMultiplier;     // 150% = 1.5
    private final int explosionRadius;          // 6 blocs
    private final int chargeTimeTicks;          // 1.5s = 30 ticks

    public FungalDetonationActive(String id, String name, String desc, double damageMultiplier, int radius, int chargeTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageMultiplier = damageMultiplier;
        this.explosionRadius = radius;
        this.chargeTimeTicks = chargeTicks;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Annonce de la charge
        player.sendMessage("§a[Pet] §c§l⚠ DÉTONATION EN COURS...");
        world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

        // Phase de charge avec effets visuels progressifs
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation();

                // Particules de charge croissantes
                double progress = (double) tick / chargeTimeTicks;
                int particleCount = (int) (10 + progress * 30);
                double radius = 0.5 + progress * 2;

                // Cercle de spores qui se concentre
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle + tick * 10);
                    double x = Math.cos(rad) * radius * (1 - progress * 0.5);
                    double z = Math.sin(rad) * radius * (1 - progress * 0.5);
                    Location particleLoc = playerLoc.clone().add(x, 0.5 + progress, z);
                    world.spawnParticle(Particle.CRIMSON_SPORE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }

                // Son de charge
                if (tick % 5 == 0) {
                    world.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f + (float)progress);
                }

                tick++;

                // Détonation finale
                if (tick >= chargeTimeTicks) {
                    cancel();
                    executeDetonation(player, petData);
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void executeDetonation(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Calculer les dégâts (basé sur l'attribut d'attaque du joueur)
        double baseDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double damage = baseDamage * damageMultiplier * petData.getStatMultiplier();

        // Rayon ajusté par niveau
        int adjustedRadius = (int) (explosionRadius + (petData.getStatMultiplier() - 1) * 3);

        // Infliger dégâts et knockback
        int enemiesHit = 0;
        for (Entity entity : world.getNearbyEntities(center, adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster) {
                // Dégâts
                monster.damage(damage, player);
                petData.addDamage((long) damage);
                enemiesHit++;

                // Knockback depuis le joueur
                Vector knockback = monster.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.5);
                monster.setVelocity(knockback);

                // Effet sur la cible
                monster.getWorld().spawnParticle(Particle.CRIT,
                    monster.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Explosion visuelle massive
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 5, 2, 1, 2, 0);
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center, 200, adjustedRadius, 2, adjustedRadius, 0.2);
        world.spawnParticle(Particle.CRIMSON_SPORE, center, 150, adjustedRadius, 3, adjustedRadius, 0.1);
        world.spawnParticle(Particle.POOF, center, 50, adjustedRadius * 0.7, 1, adjustedRadius * 0.7, 0.1);

        // Onde de choc visuelle (anneaux concentriques)
        for (int ring = 1; ring <= adjustedRadius; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    for (int angle = 0; angle < 360; angle += 10) {
                        double rad = Math.toRadians(angle);
                        Location loc = center.clone().add(Math.cos(rad) * r, 0.3, Math.sin(rad) * r);
                        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                    }
                },
                ring * 2L
            );
        }

        // Sons d'explosion
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 0.8f);
        world.playSound(center, Sound.BLOCK_FUNGUS_BREAK, 2.0f, 0.3f);

        // Message final
        player.sendMessage("§a[Pet] §c§l💥 DÉTONATION FONGIQUE! §7" + (int)damage +
            " dégâts → §e" + enemiesHit + " §7ennemi" + (enemiesHit > 1 ? "s" : "") +
            " §7(rayon §6" + adjustedRadius + "§7 blocs)");
    }
}

// ==================== NEXUS (Nexus Dimensionnel - Support) ====================

@Getter
class NexusAuraPassive implements PetAbility {
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
class DimensionalConvergenceActive implements PetAbility {
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

// ==================== GOLEM SISMIQUE (Pas Lourds / Séisme) ====================

@Getter
class HeavyStepsPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attacksForTrigger;    // 5 attaques
    private final int stunDurationTicks;    // 1s = 20 ticks
    private final int stunRadius;           // 3 blocs
    private final Map<UUID, Integer> attackCounters = new HashMap<>();

    public HeavyStepsPassive(String id, String name, String desc, int attacksNeeded, int stunTicks, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attacksForTrigger = attacksNeeded;
        this.stunDurationTicks = stunTicks;
        this.stunRadius = radius;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int count = attackCounters.getOrDefault(uuid, 0) + 1;

        // Calculer le nombre d'attaques nécessaires selon le niveau
        int adjustedTrigger = (int) Math.max(3, attacksForTrigger - (petData.getStatMultiplier() - 1));

        if (count >= adjustedTrigger) {
            attackCounters.put(uuid, 0);
            triggerQuake(player, petData);
        } else {
            attackCounters.put(uuid, count);

            // Indicateur de progression (particules sous les pieds)
            if (count >= adjustedTrigger - 2) {
                player.getWorld().spawnParticle(Particle.BLOCK,
                    player.getLocation().add(0, 0.1, 0),
                    5, 0.3, 0, 0.3, 0,
                    org.bukkit.Material.STONE.createBlockData());
            }
        }

        return damage;
    }

    private void triggerQuake(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Durée de stun ajustée par niveau
        int adjustedStunDuration = (int) (stunDurationTicks + (petData.getStatMultiplier() - 1) * 10);

        // Stun tous les mobs dans le rayon
        int enemiesStunned = 0;
        for (Entity entity : world.getNearbyEntities(center, stunRadius, stunRadius, stunRadius)) {
            if (entity instanceof Monster monster) {
                // Appliquer le stun via Slowness + Weakness
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedStunDuration, 127, false, false));
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, adjustedStunDuration, 127, false, false));

                // Petit knockback vertical (effet de secousse)
                monster.setVelocity(monster.getVelocity().setY(0.3));

                enemiesStunned++;
            }
        }

        // Effets visuels de secousse
        for (int ring = 1; ring <= stunRadius; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    for (int angle = 0; angle < 360; angle += 30) {
                        double rad = Math.toRadians(angle);
                        Location loc = center.clone().add(Math.cos(rad) * r, 0.1, Math.sin(rad) * r);
                        world.spawnParticle(Particle.BLOCK, loc, 8, 0.2, 0.1, 0.2, 0,
                            org.bukkit.Material.STONE.createBlockData());
                    }
                },
                ring
            );
        }

        // Particules centrales
        world.spawnParticle(Particle.EXPLOSION, center.add(0, 0.5, 0), 1, 0, 0, 0, 0);

        // Son de secousse
        world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);

        // Message
        if (enemiesStunned > 0) {
            player.sendMessage("§a[Pet] §6Pas Lourds! §e" + enemiesStunned +
                " §7ennemi" + (enemiesStunned > 1 ? "s" : "") + " §cstun §7" +
                String.format("%.1f", adjustedStunDuration / 20.0) + "s");
        }
    }
}

@Getter
class SeismicSlamActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseDamage;        // 30 dégâts de base
    private final int stunDurationTicks;    // 2s = 40 ticks
    private final int slamRadius;           // 8 blocs

    public SeismicSlamActive(String id, String name, String desc, double damage, int stunTicks, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseDamage = damage;
        this.stunDurationTicks = stunTicks;
        this.slamRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Calcul des valeurs
        double damage = baseDamage * petData.getStatMultiplier();
        int adjustedStun = (int) (stunDurationTicks + (petData.getStatMultiplier() - 1) * 20);
        int adjustedRadius = (int) (slamRadius + (petData.getStatMultiplier() - 1) * 2);

        // Animation de préparation (le golem lève les bras)
        player.sendMessage("§a[Pet] §6§l⚡ SÉISME!");
        world.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);

        // Appliquer dégâts et stun
        int enemiesHit = 0;
        for (Entity entity : world.getNearbyEntities(center, adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster) {
                // Dégâts
                monster.damage(damage, player);
                petData.addDamage((long) damage);

                // Stun puissant
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedStun, 127, false, false));
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, adjustedStun, 127, false, false));

                // Knockback vers le haut + écartement
                Vector knockback = monster.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(0.8)
                    .setY(0.6);
                monster.setVelocity(knockback);

                enemiesHit++;
            }
        }

        // Animation d'onde de choc progressive
        for (int ring = 1; ring <= adjustedRadius; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    // Cercle de particules
                    for (int angle = 0; angle < 360; angle += 15) {
                        double rad = Math.toRadians(angle);
                        Location loc = center.clone().add(Math.cos(rad) * r, 0.2, Math.sin(rad) * r);

                        // Blocs qui sautent
                        world.spawnParticle(Particle.BLOCK, loc, 15, 0.3, 0.3, 0.3, 0.1,
                            org.bukkit.Material.COBBLESTONE.createBlockData());

                        // Fissures
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 2, 0.1, 0.1, 0.1, 0.02);
                    }

                    // Son progressif
                    world.playSound(center.clone().add(0, 0, r), Sound.BLOCK_STONE_BREAK, 0.8f, 0.5f + r * 0.05f);
                },
                ring * 2L
            );
        }

        // Explosion centrale
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 3, 1, 0.5, 1, 0);
        world.spawnParticle(Particle.BLOCK, center, 100, 2, 0.5, 2, 0.5,
            org.bukkit.Material.STONE.createBlockData());

        // Sons principaux
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.3f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.3f);

        // Message final
        player.sendMessage("§a[Pet] §c§l💥 " + (int)damage + " §7dégâts + §cstun " +
            String.format("%.1f", adjustedStun / 20.0) + "s §7→ §e" + enemiesHit +
            " §7ennemi" + (enemiesHit > 1 ? "s" : "") + " §7(rayon §6" + adjustedRadius + "§7)");

        return true;
    }
}

// ==================== FEU FOLLET (Boules de Feu) ====================

@Getter
class WispFireballPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attacksForTrigger;     // 3 attaques de base
    private final double damagePercent;       // 30% des dégâts du joueur
    private final Map<UUID, Integer> attackCounters = new HashMap<>();

    public WispFireballPassive(String id, String name, String desc, int attacksNeeded, double damagePercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attacksForTrigger = attacksNeeded;
        this.damagePercent = damagePercent;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int count = attackCounters.getOrDefault(uuid, 0) + 1;

        // Nombre d'attaques ajusté selon le niveau (min 3)
        int adjustedTrigger = (int) Math.max(3, attacksForTrigger - (petData.getStatMultiplier() - 1));

        if (count >= adjustedTrigger) {
            attackCounters.put(uuid, 0);
            shootFireball(player, petData, target);
        } else {
            attackCounters.put(uuid, count);

            // Indicateur visuel de charge
            if (count >= adjustedTrigger - 1) {
                player.getWorld().spawnParticle(Particle.FLAME,
                    player.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0.02);
            }
        }

        return damage;
    }

    private void shootFireball(Player player, PetData petData, LivingEntity target) {
        Location origin = player.getLocation().add(0, 1.5, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);
        World world = origin.getWorld();

        // Calculer la direction
        Vector direction = targetLoc.toVector().subtract(origin.toVector()).normalize();

        // Dégâts de la boule de feu
        double adjustedDamagePercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.10;
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double fireballDamage = playerDamage * adjustedDamagePercent;

        // Son de lancement
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);

        // Animation de la boule de feu (projectile visuel)
        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int ticks = 0;
            final int maxTicks = 40; // 2 secondes max

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Avancer la boule de feu
                currentLoc.add(direction.clone().multiply(1.0));

                // Particules de la boule de feu
                world.spawnParticle(Particle.FLAME, currentLoc, 10, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticle(Particle.SMOKE, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);

                // Vérifier collision avec les mobs
                for (Entity entity : world.getNearbyEntities(currentLoc, 1, 1, 1)) {
                    if (entity instanceof Monster monster) {
                        // Impact!
                        monster.damage(fireballDamage, player);
                        monster.setFireTicks(60); // Brûle pendant 3s
                        petData.addDamage((long) fireballDamage);

                        // Effets d'impact
                        world.spawnParticle(Particle.LAVA, currentLoc, 15, 0.3, 0.3, 0.3, 0);
                        world.spawnParticle(Particle.FLAME, currentLoc, 20, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(currentLoc, Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);

                        player.sendMessage("§a[Pet] §6🔥 Boule de feu! §c" + (int)fireballDamage + " §7dégâts + brûlure");

                        cancel();
                        return;
                    }
                }

                // Vérifier si on a dépassé la cible ou touché un bloc
                if (currentLoc.getBlock().getType().isSolid() ||
                    currentLoc.distance(origin) > 20) {
                    cancel();
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

@Getter
class InfernalBarrageActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int fireballCount;          // 5 boules de feu
    private final double damagePercent;       // 50% des dégâts du joueur

    public InfernalBarrageActive(String id, String name, String desc, int count, double damagePercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.fireballCount = count;
        this.damagePercent = damagePercent;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location origin = player.getLocation().add(0, 1.5, 0);
        World world = origin.getWorld();
        Vector baseDirection = player.getLocation().getDirection().normalize();

        // Dégâts par boule de feu
        double adjustedDamagePercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.15;
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double fireballDamage = playerDamage * adjustedDamagePercent;

        // Nombre de boules ajusté
        int adjustedCount = (int) (fireballCount + (petData.getStatMultiplier() - 1) * 2);

        player.sendMessage("§a[Pet] §c§l🔥 BARRAGE INFERNAL!");
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);

        // Tirer les boules de feu en éventail
        double spreadAngle = 60.0; // 60 degrés d'éventail total
        double angleStep = spreadAngle / (adjustedCount - 1);
        double startAngle = -spreadAngle / 2;

        for (int i = 0; i < adjustedCount; i++) {
            final int index = i;
            final double angle = startAngle + (angleStep * i);

            // Délai entre chaque boule de feu
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> launchFireball(player, petData, origin.clone(), baseDirection.clone(), angle, fireballDamage),
                i * 3L
            );
        }

        return true;
    }

    private void launchFireball(Player player, PetData petData, Location origin, Vector baseDir, double angle, double damage) {
        World world = origin.getWorld();

        // Rotation du vecteur pour l'éventail
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        Vector direction = new Vector(
            baseDir.getX() * cos - baseDir.getZ() * sin,
            baseDir.getY(),
            baseDir.getX() * sin + baseDir.getZ() * cos
        ).normalize();

        // Son de lancement
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.0f + (float)(Math.random() * 0.4));

        // Animation de la boule de feu
        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int ticks = 0;
            final int maxTicks = 60;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Avancer la boule de feu
                currentLoc.add(direction.clone().multiply(0.8));

                // Particules plus grosses pour le barrage
                world.spawnParticle(Particle.FLAME, currentLoc, 15, 0.25, 0.25, 0.25, 0.03);
                world.spawnParticle(Particle.LAVA, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                // Vérifier collision avec les mobs
                for (Entity entity : world.getNearbyEntities(currentLoc, 1.2, 1.2, 1.2)) {
                    if (entity instanceof Monster monster && !hitEntities.contains(entity.getUniqueId())) {
                        hitEntities.add(entity.getUniqueId());

                        // Impact!
                        monster.damage(damage, player);
                        monster.setFireTicks(80); // Brûle pendant 4s
                        petData.addDamage((long) damage);

                        // Effets d'impact
                        world.spawnParticle(Particle.EXPLOSION, currentLoc, 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.FLAME, currentLoc, 30, 0.5, 0.5, 0.5, 0.15);
                        world.playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
                    }
                }

                // Vérifier si on a touché un bloc solide
                if (currentLoc.getBlock().getType().isSolid()) {
                    // Explosion à l'impact
                    world.spawnParticle(Particle.FLAME, currentLoc, 20, 0.3, 0.3, 0.3, 0.1);
                    world.playSound(currentLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== ARAIGNÉE CHASSEUSE (Prédateur / Embuscade) ====================

@Getter
class PredatorPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double bonusDamageOnSlowed;    // +25% de dégâts sur ralentis
    private final int slowDurationTicks;          // 1.5s = 30 ticks
    private final Set<UUID> markedTargets = new HashSet<>();  // Cibles marquées par l'embuscade

    public PredatorPassive(String id, String name, String desc, double bonusDamage, int slowTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.bonusDamageOnSlowed = bonusDamage;
        this.slowDurationTicks = slowTicks;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        World world = player.getWorld();

        // Vérifier si la cible est ralentie, immobilisée ou marquée
        boolean isVulnerable = false;

        // Vérifier effets de ralentissement/immobilisation
        if (target.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            isVulnerable = true;
        }
        // Vérifier si stun (Slowness 127 = notre mécanisme de stun)
        PotionEffect slowEffect = target.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slowEffect != null && slowEffect.getAmplifier() >= 100) {
            isVulnerable = true;
        }

        // Vérifier si marqué par l'embuscade
        if (markedTargets.contains(target.getUniqueId())) {
            isVulnerable = true;
        }

        // Appliquer le ralentissement à chaque attaque
        int adjustedSlowDuration = (int) (slowDurationTicks + (petData.getStatMultiplier() - 1) * 10);
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, adjustedSlowDuration, 1, false, false));

        // Effet visuel de toile
        world.spawnParticle(Particle.BLOCK, target.getLocation().add(0, 0.5, 0),
            5, 0.2, 0.2, 0.2, 0, org.bukkit.Material.COBWEB.createBlockData());

        // Calculer les dégâts bonus si la cible est vulnérable
        if (isVulnerable) {
            double adjustedBonus = bonusDamageOnSlowed + (petData.getStatMultiplier() - 1) * 0.10;
            double bonusDamage = damage * adjustedBonus;

            // Effet visuel de prédateur
            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                8, 0.3, 0.3, 0.3, 0.1);
            world.playSound(target.getLocation(), Sound.ENTITY_SPIDER_HURT, 0.5f, 1.5f);

            return damage + bonusDamage;
        }

        return damage;
    }

    /**
     * Marque une cible (appelé par SpiderAmbushActive)
     */
    public void markTarget(LivingEntity target, int durationTicks) {
        UUID uuid = target.getUniqueId();
        markedTargets.add(uuid);

        // Retirer la marque après la durée
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> markedTargets.remove(uuid),
            durationTicks
        );
    }

    /**
     * Vérifie si une cible est marquée
     */
    public boolean isMarked(UUID uuid) {
        return markedTargets.contains(uuid);
    }
}

@Getter
class SpiderAmbushActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int immobilizeDurationTicks;    // 3s = 60 ticks
    private final double markDamageBonus;          // +50% dégâts reçus
    private final int markDurationTicks;           // 5s = 100 ticks
    private final PredatorPassive predatorPassive;

    public SpiderAmbushActive(String id, String name, String desc, int immobilizeTicks,
                              double markBonus, int markTicks, PredatorPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.immobilizeDurationTicks = immobilizeTicks;
        this.markDamageBonus = markBonus;
        this.markDurationTicks = markTicks;
        this.predatorPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        // Trouver l'ennemi le plus proche
        Monster target = null;
        double minDist = 15; // Portée max de 15 blocs

        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof Monster monster) {
                double dist = entity.getLocation().distance(playerLoc);
                if (dist < minDist) {
                    minDist = dist;
                    target = monster;
                }
            }
        }

        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucune cible à proximité!");
            return false;
        }

        final Monster finalTarget = target;
        final Location targetLoc = target.getLocation();

        // Calcul des durées ajustées par niveau
        int adjustedImmobilize = (int) (immobilizeDurationTicks + (petData.getStatMultiplier() - 1) * 20);
        int adjustedMarkDuration = (int) (markDurationTicks + (petData.getStatMultiplier() - 1) * 40);

        // Son de préparation
        world.playSound(playerLoc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.5f);
        player.sendMessage("§a[Pet] §c§l\uD83D\uDD77 EMBUSCADE!");

        // Animation du bond de l'araignée (de la position du joueur vers la cible)
        Location spiderStart = playerLoc.clone().add(0, 1, 0);
        Vector direction = targetLoc.toVector().subtract(spiderStart.toVector()).normalize();

        new BukkitRunnable() {
            Location currentLoc = spiderStart.clone();
            int ticks = 0;
            final int maxTicks = (int) (minDist * 2); // Durée basée sur la distance

            @Override
            public void run() {
                if (ticks >= maxTicks || !finalTarget.isValid()) {
                    // Arrivée sur la cible - Impact!
                    executeImpact(player, petData, finalTarget, adjustedImmobilize, adjustedMarkDuration);
                    cancel();
                    return;
                }

                // Avancer l'araignée
                currentLoc.add(direction.clone().multiply(0.5));

                // Particules de l'araignée en mouvement
                world.spawnParticle(Particle.BLOCK, currentLoc, 8, 0.2, 0.2, 0.2, 0,
                    org.bukkit.Material.COBWEB.createBlockData());
                world.spawnParticle(Particle.SMOKE, currentLoc, 3, 0.1, 0.1, 0.1, 0.02);

                // Son de mouvement rapide
                if (ticks % 4 == 0) {
                    world.playSound(currentLoc, Sound.ENTITY_SPIDER_STEP, 0.5f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void executeImpact(Player player, PetData petData, Monster target,
                               int immobilizeDuration, int markDuration) {
        Location impactLoc = target.getLocation();
        World world = impactLoc.getWorld();

        // Immobiliser la cible (stun via Slowness 127 + Weakness 127)
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, immobilizeDuration, 127, false, false));
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS, immobilizeDuration, 127, false, false));

        // Marquer la cible pour les dégâts bonus
        predatorPassive.markTarget(target, markDuration);
        target.setGlowing(true);

        // Retirer le glow après la durée de la marque
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                if (target.isValid()) {
                    target.setGlowing(false);
                }
            },
            markDuration
        );

        // Effet visuel de toile d'immobilisation
        world.spawnParticle(Particle.BLOCK, impactLoc.add(0, 0.5, 0), 50, 1, 0.5, 1, 0,
            org.bukkit.Material.COBWEB.createBlockData());
        world.spawnParticle(Particle.CRIT, impactLoc, 20, 0.5, 0.5, 0.5, 0.1);

        // Toiles visuelles au sol (cercle)
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            Location webLoc = impactLoc.clone().add(Math.cos(rad) * 1.5, 0.1, Math.sin(rad) * 1.5);
            world.spawnParticle(Particle.BLOCK, webLoc, 5, 0.2, 0, 0.2, 0,
                org.bukkit.Material.COBWEB.createBlockData());
        }

        // Sons d'impact
        world.playSound(impactLoc, Sound.ENTITY_SPIDER_DEATH, 1.0f, 0.8f);
        world.playSound(impactLoc, Sound.BLOCK_WOOL_PLACE, 1.0f, 0.5f);
        world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

        // Message
        player.sendMessage("§a[Pet] §7Cible immobilisée §c" + String.format("%.1f", immobilizeDuration / 20.0) +
            "s §7+ marquée §e+" + (int)(markDamageBonus * 100) + "% §7dégâts pendant §e" +
            String.format("%.1f", markDuration / 20.0) + "s");
    }
}
