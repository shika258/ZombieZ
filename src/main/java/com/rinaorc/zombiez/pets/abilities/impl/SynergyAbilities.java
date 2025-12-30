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

// ==================== SPECTRE TRAQUEUR (Chasse / Exécution) ====================

@Getter
class PredatorInstinctPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healthThreshold;       // 50% = 0.50
    private final double bonusDamageOnMarked;   // +20% = 0.20
    private final Set<UUID> markedEntities = new HashSet<>();

    public PredatorInstinctPassive(String id, String name, String desc, double threshold, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healthThreshold = threshold;
        this.bonusDamageOnMarked = bonus;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        World world = player.getWorld();

        // Seuil de HP ajusté par niveau (50% base, jusqu'à 60% au niveau max)
        double adjustedThreshold = healthThreshold + (petData.getStatMultiplier() - 1) * 0.10;

        // Vérifier si la cible doit être marquée automatiquement
        double healthPercent = target.getHealth() / target.getMaxHealth();

        if (healthPercent <= adjustedThreshold && !markedEntities.contains(target.getUniqueId())) {
            // Marquer automatiquement la proie affaiblie
            markedEntities.add(target.getUniqueId());
            target.setGlowing(true);

            // Effet visuel de marquage
            world.spawnParticle(Particle.WITCH, target.getLocation().add(0, 1.5, 0),
                15, 0.3, 0.3, 0.3, 0.05);
            world.playSound(target.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 1.5f);

            player.sendMessage("§a[Pet] §c\uD83D\uDC41 Proie repérée! §7Cible marquée (§e<" +
                (int)(adjustedThreshold * 100) + "% HP§7)");

            // Expire si la cible meurt ou après 15 secondes
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    markedEntities.remove(target.getUniqueId());
                    if (target.isValid()) target.setGlowing(false);
                },
                300L  // 15 secondes
            );
        }

        // Bonus de dégâts sur cibles marquées
        if (markedEntities.contains(target.getUniqueId())) {
            double adjustedBonus = bonusDamageOnMarked + (petData.getStatMultiplier() - 1) * 0.10;

            // Effet visuel de prédateur
            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                5, 0.2, 0.2, 0.2, 0.1);

            return damage * (1 + adjustedBonus);
        }

        return damage;
    }

    /**
     * Trouve l'ennemi marqué avec le moins de HP
     */
    public Monster findWeakestMarkedTarget(Player player, double range) {
        Monster weakest = null;
        double lowestHealth = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Monster monster && markedEntities.contains(monster.getUniqueId())) {
                if (monster.getHealth() < lowestHealth) {
                    lowestHealth = monster.getHealth();
                    weakest = monster;
                }
            }
        }

        return weakest;
    }

    /**
     * Trouve l'ennemi avec le moins de HP (même non marqué)
     */
    public Monster findWeakestTarget(Player player, double range) {
        Monster weakest = null;
        double lowestHealthPercent = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Monster monster) {
                double healthPercent = monster.getHealth() / monster.getMaxHealth();
                if (healthPercent < lowestHealthPercent) {
                    lowestHealthPercent = healthPercent;
                    weakest = monster;
                }
            }
        }

        return weakest;
    }

    public boolean isMarked(UUID uuid) {
        return markedEntities.contains(uuid);
    }

    public void unmark(UUID uuid) {
        markedEntities.remove(uuid);
    }
}

@Getter
class DeadlyDiveActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageMultiplier;      // 200% = 2.0
    private final double executeThreshold;       // 20% = 0.20
    private final PredatorInstinctPassive instinctPassive;

    public DeadlyDiveActive(String id, String name, String desc, double damage,
                            double execute, PredatorInstinctPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageMultiplier = damage;
        this.executeThreshold = execute;
        this.instinctPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        // Priorité: cible marquée la plus faible, sinon cible la plus faible
        Monster target = instinctPassive.findWeakestMarkedTarget(player, 15);
        if (target == null) {
            target = instinctPassive.findWeakestTarget(player, 15);
        }

        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucune proie à proximité!");
            return false;
        }

        final Monster finalTarget = target;
        final Location targetLoc = target.getLocation();
        final double targetHealthPercent = target.getHealth() / target.getMaxHealth();

        // Seuil d'exécution ajusté par niveau
        double adjustedExecuteThreshold = executeThreshold + (petData.getStatMultiplier() - 1) * 0.05;

        // Animation du plongeon du phantom
        player.sendMessage("§a[Pet] §5§l\uD83E\uDD87 PLONGEON MORTEL!");
        world.playSound(playerLoc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.5f);

        // Départ depuis le ciel (au-dessus du joueur)
        Location diveStart = playerLoc.clone().add(0, 10, 0);
        Vector direction = targetLoc.toVector().subtract(diveStart.toVector()).normalize();

        new BukkitRunnable() {
            Location currentLoc = diveStart.clone();
            int ticks = 0;
            final int maxTicks = 20; // 1 seconde de plongeon

            @Override
            public void run() {
                if (ticks >= maxTicks || !finalTarget.isValid()) {
                    // Impact!
                    executeImpact(player, petData, finalTarget, adjustedExecuteThreshold);
                    cancel();
                    return;
                }

                // Avancer le phantom en plongeon
                currentLoc.add(direction.clone().multiply(0.8));

                // Particules de phantom en mouvement
                world.spawnParticle(Particle.WITCH, currentLoc, 10, 0.3, 0.3, 0.3, 0.02);
                world.spawnParticle(Particle.SMOKE, currentLoc, 5, 0.2, 0.2, 0.2, 0.01);

                // Son de plongeon
                if (ticks % 5 == 0) {
                    world.playSound(currentLoc, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 1.2f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void executeImpact(Player player, PetData petData, Monster target, double executeThreshold) {
        Location impactLoc = target.getLocation();
        World world = impactLoc.getWorld();

        // Calculer les dégâts (200% des dégâts du joueur)
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double damage = playerDamage * damageMultiplier * petData.getStatMultiplier();

        // Vérifier si c'est une exécution
        double healthPercent = target.getHealth() / target.getMaxHealth();
        boolean isExecute = healthPercent <= executeThreshold;

        if (isExecute) {
            // EXÉCUTION! Dégâts massifs pour tuer instantanément
            damage = target.getHealth() + 100; // Assure la mort

            // Effets visuels d'exécution
            world.spawnParticle(Particle.SOUL, impactLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.CRIT_MAGIC, impactLoc, 20, 0.3, 0.3, 0.3, 0.2);
            world.playSound(impactLoc, Sound.ENTITY_PHANTOM_DEATH, 1.0f, 0.5f);
            world.playSound(impactLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.5f, 1.5f);

            player.sendMessage("§a[Pet] §c§l💀 EXÉCUTION! §7La proie a été achevée!");
        } else {
            // Dégâts normaux
            world.spawnParticle(Particle.CRIT, impactLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.WITCH, impactLoc, 15, 0.3, 0.3, 0.3, 0.05);
            world.playSound(impactLoc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.0f);
            world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);

            player.sendMessage("§a[Pet] §c\uD83E\uDD87 " + (int)damage + " §7dégâts! §8(Execute si <" +
                (int)(executeThreshold * 100) + "% HP)");
        }

        // Appliquer les dégâts
        target.damage(damage, player);
        petData.addDamage((long) damage);

        // Retirer la marque
        instinctPassive.unmark(target.getUniqueId());
        if (target.isValid()) {
            target.setGlowing(false);
        }
    }
}

// ==================== GRENOUILLE BONDISSANTE (Mobilité / Combos) ====================

@Getter
class FrogBouncePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attacksForBounce;          // 4 attaques
    private final double bounceDamageBonus;       // +30%
    private final int stunDurationTicks;          // 0.5s = 10 ticks
    private final Map<UUID, Integer> attackCounters = new HashMap<>();

    public FrogBouncePassive(String id, String name, String desc, int attacks, double bonus, int stunTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attacksForBounce = attacks;
        this.bounceDamageBonus = bonus;
        this.stunDurationTicks = stunTicks;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();

        // Incrémenter le compteur
        int count = attackCounters.getOrDefault(uuid, 0) + 1;

        // Ajuster le nombre d'attaques requis par niveau (4 base, 3 au max)
        int adjustedAttacks = Math.max(3, attacksForBounce - (int)((petData.getStatMultiplier() - 1) * 2));

        if (count >= adjustedAttacks) {
            // BOND! Reset le compteur
            attackCounters.put(uuid, 0);

            // Calculer le bonus de dégâts
            double adjustedBonus = bounceDamageBonus + (petData.getStatMultiplier() - 1) * 0.10;
            double bonusDamage = damage * adjustedBonus;

            // Appliquer le stun
            int adjustedStun = (int) (stunDurationTicks + (petData.getStatMultiplier() - 1) * 5);
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, adjustedStun, 127, false, false));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, adjustedStun, 127, false, false));

            // Animation du bond
            Location targetLoc = target.getLocation();

            // Particules de bond (splash d'eau + slime)
            world.spawnParticle(Particle.SPLASH, targetLoc.add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.1);
            world.spawnParticle(Particle.SLIME, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);

            // Sons de grenouille
            world.playSound(targetLoc, Sound.ENTITY_FROG_LONG_JUMP, 1.0f, 1.2f);
            world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.0f);

            player.sendMessage("§a[Pet] §2🐸 BOND! §7+" + (int)(adjustedBonus * 100) + "% dégâts + stun!");

            return damage + bonusDamage;
        } else {
            attackCounters.put(uuid, count);

            // Indicateur visuel de progression
            if (count == adjustedAttacks - 1) {
                world.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0),
                    3, 0.2, 0.2, 0.2, 0);
            }
        }

        return damage;
    }

    public int getAttackCount(UUID uuid) {
        return attackCounters.getOrDefault(uuid, 0);
    }
}

@Getter
class BouncingAssaultActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int bounceCount;                // 5 bonds
    private final double bounceDamagePercent;     // 50% des dégâts du joueur
    private final int stunPerBounce;              // Stun par bond en ticks

    public BouncingAssaultActive(String id, String name, String desc, int bounces, double damage, int stun) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.bounceCount = bounces;
        this.bounceDamagePercent = damage;
        this.stunPerBounce = stun;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        // Collecter tous les ennemis proches
        List<Monster> targets = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
            if (entity instanceof Monster monster) {
                targets.add(monster);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à proximité!");
            return false;
        }

        // Calculer les dégâts par bond
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double damagePerBounce = playerDamage * bounceDamagePercent * petData.getStatMultiplier();

        // Ajuster le nombre de bonds par niveau
        int adjustedBounces = bounceCount + (int)((petData.getStatMultiplier() - 1) * 2);
        adjustedBounces = Math.min(adjustedBounces, Math.max(targets.size(), bounceCount));

        player.sendMessage("§a[Pet] §2§l🐸 ASSAUT BONDISSANT! §7(" + adjustedBounces + " bonds)");
        world.playSound(playerLoc, Sound.ENTITY_FROG_LONG_JUMP, 1.0f, 0.8f);

        // Lancer la séquence de bonds
        executeBounceSequence(player, petData, targets, damagePerBounce, adjustedBounces, 0, playerLoc);

        return true;
    }

    private void executeBounceSequence(Player player, PetData petData, List<Monster> targets,
                                       double damage, int remainingBounces, int targetIndex, Location lastLoc) {
        if (remainingBounces <= 0 || targets.isEmpty()) {
            player.sendMessage("§a[Pet] §7Assaut terminé!");
            return;
        }

        // Sélectionner la cible (cycle à travers les cibles disponibles)
        Monster target = null;
        int attempts = 0;
        while (target == null && attempts < targets.size()) {
            Monster candidate = targets.get(targetIndex % targets.size());
            if (candidate.isValid() && !candidate.isDead()) {
                target = candidate;
            } else {
                targets.remove(candidate);
                if (targets.isEmpty()) break;
            }
            targetIndex++;
            attempts++;
        }

        if (target == null) {
            player.sendMessage("§a[Pet] §7Plus de cibles!");
            return;
        }

        final Monster finalTarget = target;
        final Location targetLoc = target.getLocation();
        final int nextIndex = targetIndex;
        World world = player.getWorld();

        // Animation du bond (arc de cercle)
        Vector direction = targetLoc.toVector().subtract(lastLoc.toVector());
        double distance = direction.length();
        direction.normalize();

        new BukkitRunnable() {
            Location currentLoc = lastLoc.clone().add(0, 0.5, 0);
            int ticks = 0;
            final int maxTicks = 8; // Bond rapide
            double progress = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    // Impact!
                    executeImpact(player, petData, finalTarget, damage, world);

                    // Continuer la séquence
                    Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("ZombieZ"),
                        () -> executeBounceSequence(player, petData, targets, damage,
                            remainingBounces - 1, nextIndex, finalTarget.getLocation()),
                        3L
                    );
                    cancel();
                    return;
                }

                // Calculer la position avec une courbe parabolique
                progress = (double) ticks / maxTicks;
                double heightOffset = Math.sin(progress * Math.PI) * 2; // Arc

                currentLoc = lastLoc.clone().add(
                    direction.clone().multiply(distance * progress)
                ).add(0, heightOffset, 0);

                // Particules de bond
                world.spawnParticle(Particle.SPLASH, currentLoc, 5, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.SLIME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    private void executeImpact(Player player, PetData petData, Monster target, double damage, World world) {
        Location impactLoc = target.getLocation();

        // Appliquer le stun
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS, stunPerBounce, 127, false, false));
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.WEAKNESS, stunPerBounce, 127, false, false));

        // Appliquer les dégâts
        target.damage(damage, player);
        petData.addDamage((long) damage);

        // Effets visuels d'impact
        world.spawnParticle(Particle.SPLASH, impactLoc.add(0, 0.3, 0), 30, 0.5, 0.2, 0.5, 0.1);
        world.spawnParticle(Particle.CRIT, impactLoc, 10, 0.3, 0.3, 0.3, 0.1);

        // Son d'impact
        world.playSound(impactLoc, Sound.ENTITY_FROG_STEP, 1.0f, 0.8f);
        world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
    }
}

// ==================== OURS POLAIRE GARDIEN (Tank / Protection) ====================

@Getter
class FrostFurPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageReduction;         // -20% = 0.20
    private final double reflectPercent;          // 5% des dégâts retournés = 0.05
    private final int slowDurationTicks;          // 1s = 20 ticks

    public FrostFurPassive(String id, String name, String desc, double reduction, double reflect, int slowTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageReduction = reduction;
        this.reflectPercent = reflect;
        this.slowDurationTicks = slowTicks;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Retourne le pourcentage de réduction de dégâts pour le listener
     */
    public double getDamageReduction(PetData petData) {
        return damageReduction + (petData.getStatMultiplier() - 1) * 0.05;
    }

    /**
     * Appelé quand le joueur reçoit des dégâts - applique le retour de gel
     */
    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Trouver l'attaquant le plus proche (dans un rayon de 5 blocs)
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Monster monster) {
                // Calculer les dégâts de retour
                double adjustedReflect = reflectPercent + (petData.getStatMultiplier() - 1) * 0.03;
                double reflectDamage = damage * adjustedReflect;

                // Appliquer le ralentissement glacial
                int adjustedSlow = (int) (slowDurationTicks + (petData.getStatMultiplier() - 1) * 10);
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedSlow, 1, false, false));

                // Appliquer les dégâts de retour (gel)
                if (reflectDamage > 0) {
                    monster.damage(reflectDamage, player);

                    // Effet visuel de gel
                    world.spawnParticle(Particle.SNOWFLAKE, monster.getLocation().add(0, 1, 0),
                        10, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.BLOCK, monster.getLocation().add(0, 0.5, 0),
                        5, 0.2, 0.2, 0.2, 0, org.bukkit.Material.PACKED_ICE.createBlockData());
                }

                // On ne traite que le premier attaquant trouvé
                break;
            }
        }

        // Effet visuel de protection sur le joueur
        world.spawnParticle(Particle.SNOWFLAKE, playerLoc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.02);
        world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f);
    }
}

@Getter
class ArcticRoarActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double knockbackDistance;       // 8 blocs
    private final int freezeDurationTicks;        // 2s = 40 ticks
    private final double armorBonus;              // +30% = 0.30
    private final int armorDurationTicks;         // 5s = 100 ticks

    public ArcticRoarActive(String id, String name, String desc, int cooldown,
                            double knockback, int freezeTicks, double armor, int armorTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cooldown;
        this.knockbackDistance = knockback;
        this.freezeDurationTicks = freezeTicks;
        this.armorBonus = armor;
        this.armorDurationTicks = armorTicks;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        // Ajuster les valeurs par niveau
        double adjustedKnockback = knockbackDistance + (petData.getStatMultiplier() - 1) * 2;
        int adjustedFreeze = (int) (freezeDurationTicks + (petData.getStatMultiplier() - 1) * 20);
        double adjustedArmor = armorBonus + (petData.getStatMultiplier() - 1) * 0.10;

        int hitCount = 0;

        // Trouver tous les ennemis proches et les repousser
        for (Entity entity : player.getNearbyEntities(adjustedKnockback, adjustedKnockback, adjustedKnockback)) {
            if (entity instanceof Monster monster) {
                // Calculer la direction de knockback (depuis le joueur vers le monstre)
                Vector knockbackDir = monster.getLocation().toVector()
                    .subtract(playerLoc.toVector()).normalize();

                // Appliquer le knockback
                knockbackDir.setY(0.4); // Légère élévation
                knockbackDir.multiply(2.0); // Force du knockback
                monster.setVelocity(knockbackDir);

                // Appliquer le gel (stun via Slowness 127 + Weakness 127)
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedFreeze, 127, false, false));
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, adjustedFreeze, 127, false, false));

                // Effet visuel de gel sur chaque ennemi
                world.spawnParticle(Particle.SNOWFLAKE, monster.getLocation().add(0, 1, 0),
                    20, 0.5, 0.5, 0.5, 0.1);
                world.spawnParticle(Particle.BLOCK, monster.getLocation(),
                    10, 0.3, 0.3, 0.3, 0, org.bukkit.Material.BLUE_ICE.createBlockData());

                hitCount++;
            }
        }

        // Appliquer le buff d'armure au joueur (via Resistance)
        int resistanceLevel = (int) (adjustedArmor * 5); // 30% = niveau 1-2
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, armorDurationTicks, Math.min(resistanceLevel, 2), false, true));

        // Effet visuel du rugissement (cercle expansif de glace)
        for (int ring = 1; ring <= 3; ring++) {
            final int currentRing = ring;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> spawnIceRing(world, playerLoc, currentRing * 3),
                ring * 3L
            );
        }

        // Effet central
        world.spawnParticle(Particle.EXPLOSION, playerLoc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.SNOWFLAKE, playerLoc, 100, 3, 1, 3, 0.2);
        world.spawnParticle(Particle.CLOUD, playerLoc, 50, 2, 0.5, 2, 0.1);

        // Son de rugissement d'ours
        world.playSound(playerLoc, Sound.ENTITY_POLAR_BEAR_WARNING, 1.5f, 0.6f);
        world.playSound(playerLoc, Sound.ENTITY_POLAR_BEAR_HURT, 1.0f, 0.5f);
        world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);

        // Message
        player.sendMessage("§a[Pet] §b§l❄ RUGISSEMENT ARCTIQUE! §7" + hitCount +
            " ennemis repoussés et gelés! §e+" + (int)(adjustedArmor * 100) + "% §7armure pendant 5s!");

        return true;
    }

    private void spawnIceRing(World world, Location center, double radius) {
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            Location ringLoc = center.clone().add(
                Math.cos(rad) * radius,
                0.1,
                Math.sin(rad) * radius
            );
            world.spawnParticle(Particle.SNOWFLAKE, ringLoc, 3, 0.1, 0.1, 0.1, 0.01);
            world.spawnParticle(Particle.BLOCK, ringLoc, 2, 0.1, 0, 0.1, 0,
                org.bukkit.Material.PACKED_ICE.createBlockData());
        }
    }
}

// ==================== CALAMAR DES ABYSSES (Anti-Horde / Contrôle) ====================

@Getter
class InkPuddlePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attacksForTrigger;         // 10 attaques
    private final double slowPercent;            // 30% slow
    private final double damagePerSecondPercent; // % des dégâts du joueur par seconde
    private final int puddleDurationTicks;       // 3s = 60 ticks
    private final double puddleRadius;           // Rayon de la flaque
    private final Map<UUID, Integer> attackCounters = new HashMap<>();
    private final List<InkPuddle> activePuddles = new ArrayList<>();

    public InkPuddlePassive(String id, String name, String desc, int attacksNeeded,
                            double slowPct, double dmgPct, int duration, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attacksForTrigger = attacksNeeded;
        this.slowPercent = slowPct;
        this.damagePerSecondPercent = dmgPct;
        this.puddleDurationTicks = duration;
        this.puddleRadius = radius;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int count = attackCounters.getOrDefault(uuid, 0) + 1;

        // Ajuster le nombre d'attaques nécessaires par niveau
        int adjustedAttacks = attacksForTrigger - (int)((petData.getStatMultiplier() - 1) * 2);
        adjustedAttacks = Math.max(adjustedAttacks, 6); // Minimum 6 attaques

        if (count >= adjustedAttacks) {
            // Créer la flaque d'encre!
            attackCounters.put(uuid, 0);
            createInkPuddle(player, petData, target.getLocation());
        } else {
            attackCounters.put(uuid, count);

            // Indicateur de progression
            if (count == adjustedAttacks - 1) {
                player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0, 1.5, 0),
                    5, 0.2, 0.2, 0.2, 0.01);
            }
        }

        return damage;
    }

    private void createInkPuddle(Player player, PetData petData, Location center) {
        World world = center.getWorld();

        // Calculer les dégâts par tick (20 ticks = 1 seconde)
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedDmgPercent = damagePerSecondPercent + (petData.getStatMultiplier() - 1) * 0.05;
        double damagePerTick = (playerDamage * adjustedDmgPercent) / 20.0;

        // Ajuster le rayon par niveau
        double adjustedRadius = puddleRadius + (petData.getStatMultiplier() - 1) * 1.0;

        // Ajuster la durée par niveau
        int adjustedDuration = (int) (puddleDurationTicks + (petData.getStatMultiplier() - 1) * 20);

        // Effet de création
        world.playSound(center, Sound.ENTITY_GLOW_SQUID_SQUIRT, 1.2f, 0.8f);
        world.playSound(center, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 0.6f);

        player.sendMessage("§a[Pet] §8§l🦑 ENCRE TOXIQUE! §7Flaque créée (" + (adjustedDuration / 20) + "s)");

        // Animation de la flaque
        new BukkitRunnable() {
            int ticksAlive = 0;
            final Set<UUID> damagedThisTick = new HashSet<>();

            @Override
            public void run() {
                if (ticksAlive >= adjustedDuration) {
                    // Effet de dissipation
                    world.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.3, 0),
                        20, adjustedRadius * 0.5, 0.1, adjustedRadius * 0.5, 0.02);
                    cancel();
                    return;
                }

                // Particules de la flaque (toutes les 5 ticks pour les perfs)
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double r = Math.random() * adjustedRadius;
                        Location particleLoc = center.clone().add(
                            Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                        world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                    }
                    // Quelques bulles
                    world.spawnParticle(Particle.BUBBLE_POP, center.clone().add(0, 0.2, 0),
                        3, adjustedRadius * 0.3, 0, adjustedRadius * 0.3, 0);
                }

                // Appliquer les effets toutes les 20 ticks (1 seconde)
                if (ticksAlive % 20 == 0) {
                    damagedThisTick.clear();

                    for (Entity entity : world.getNearbyEntities(center, adjustedRadius, 2, adjustedRadius)) {
                        if (entity instanceof Monster monster && !damagedThisTick.contains(entity.getUniqueId())) {
                            // Vérifier si vraiment dans le rayon (pas un cube)
                            double distSq = entity.getLocation().distanceSquared(center);
                            if (distSq <= adjustedRadius * adjustedRadius) {
                                damagedThisTick.add(entity.getUniqueId());

                                // Appliquer le slow (30%)
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 25, 1, false, false));

                                // Appliquer les dégâts par seconde
                                double secDamage = playerDamage * adjustedDmgPercent;
                                monster.damage(secDamage, player);
                                petData.addDamage((long) secDamage);

                                // Effet visuel sur le monstre
                                world.spawnParticle(Particle.SQUID_INK, monster.getLocation().add(0, 0.5, 0),
                                    5, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    public int getAttackCount(UUID uuid) {
        return attackCounters.getOrDefault(uuid, 0);
    }

    // Classe interne pour tracker les flaques actives
    private static class InkPuddle {
        final Location center;
        final long endTime;

        InkPuddle(Location center, long endTime) {
            this.center = center;
            this.endTime = endTime;
        }
    }
}

@Getter
class DarknessCloudActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double cloudRadius;            // 8 blocs
    private final int confusionDurationTicks;    // 4s = 80 ticks
    private final double damagePercent;          // Dégâts additionnels (upgrade)

    public DarknessCloudActive(String id, String name, String desc, int cd,
                               double radius, int confusionTicks, double dmgPct) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.cloudRadius = radius;
        this.confusionDurationTicks = confusionTicks;
        this.damagePercent = dmgPct;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Ajuster les valeurs par niveau
        double adjustedRadius = cloudRadius + (petData.getStatMultiplier() - 1) * 2;
        int adjustedConfusion = (int) (confusionDurationTicks + (petData.getStatMultiplier() - 1) * 20);

        // Collecter tous les monstres dans le rayon
        List<Monster> affectedMonsters = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius)) {
            if (entity instanceof Monster monster) {
                double distSq = entity.getLocation().distanceSquared(center);
                if (distSq <= adjustedRadius * adjustedRadius) {
                    affectedMonsters.add(monster);
                }
            }
        }

        if (affectedMonsters.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi dans le rayon!");
            return false;
        }

        // Effet sonore de création du nuage
        world.playSound(center, Sound.ENTITY_GLOW_SQUID_SQUIRT, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
        world.playSound(center, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.5f);

        player.sendMessage("§a[Pet] §8§l🦑 NUAGE D'OBSCURITÉ! §7" + affectedMonsters.size() +
            " zombies confus s'attaquent entre eux!");

        // Appliquer la confusion à tous les monstres
        for (Monster monster : affectedMonsters) {
            // Aveuglement pour l'effet visuel
            monster.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, adjustedConfusion, 0, false, false));
            // Lenteur légère
            monster.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, adjustedConfusion, 0, false, false));
        }

        // Animation du nuage d'encre et combat entre monstres
        new BukkitRunnable() {
            int ticksAlive = 0;
            final int cloudDuration = adjustedConfusion;
            int nextInfightTick = 20; // Premier infight après 1 seconde

            @Override
            public void run() {
                if (ticksAlive >= cloudDuration) {
                    // Dissipation du nuage
                    world.spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0),
                        50, adjustedRadius * 0.5, 1, adjustedRadius * 0.5, 0.05);
                    world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.5f);
                    cancel();
                    return;
                }

                // Particules du nuage d'encre (toutes les 3 ticks)
                if (ticksAlive % 3 == 0) {
                    // Nuage sombre
                    for (int i = 0; i < 15; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double r = Math.random() * adjustedRadius;
                        double height = Math.random() * 3;
                        Location particleLoc = center.clone().add(
                            Math.cos(angle) * r, height, Math.sin(angle) * r);
                        world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                    }
                    // Quelques particules de glow
                    world.spawnParticle(Particle.GLOW, center.clone().add(0, 1.5, 0),
                        3, adjustedRadius * 0.3, 1, adjustedRadius * 0.3, 0.01);
                }

                // Faire s'attaquer les monstres entre eux (toutes les secondes)
                if (ticksAlive >= nextInfightTick) {
                    nextInfightTick += 20; // Prochaine attaque dans 1 seconde

                    // Nettoyer les monstres morts
                    affectedMonsters.removeIf(m -> !m.isValid() || m.isDead());

                    if (affectedMonsters.size() >= 2) {
                        // Calculer les dégâts d'infight (basés sur les dégâts du joueur)
                        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
                        double infightDamage = playerDamage * 0.3 * petData.getStatMultiplier();

                        // Chaque monstre attaque un voisin aléatoire
                        for (Monster attacker : new ArrayList<>(affectedMonsters)) {
                            if (!attacker.isValid() || attacker.isDead()) continue;

                            // Trouver une cible proche
                            Monster target = null;
                            double minDist = Double.MAX_VALUE;
                            for (Monster potential : affectedMonsters) {
                                if (potential != attacker && potential.isValid() && !potential.isDead()) {
                                    double dist = attacker.getLocation().distanceSquared(potential.getLocation());
                                    if (dist < minDist) {
                                        minDist = dist;
                                        target = potential;
                                    }
                                }
                            }

                            if (target != null && minDist < 25) { // 5 blocs max
                                // Attaquer!
                                target.damage(infightDamage, attacker);
                                petData.addDamage((long) infightDamage);

                                // Effet visuel de l'attaque
                                Location midPoint = attacker.getLocation().add(
                                    target.getLocation()).multiply(0.5).add(0, 1, 0);
                                world.spawnParticle(Particle.SWEEP_ATTACK, midPoint, 1, 0, 0, 0, 0);
                                world.spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0),
                                    5, 0.2, 0.2, 0.2, 0.05);
                            }
                        }
                    }
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        // Effet visuel initial (explosion d'encre)
        world.spawnParticle(Particle.SQUID_INK, center.clone().add(0, 1, 0),
            100, adjustedRadius * 0.5, 1.5, adjustedRadius * 0.5, 0.1);
        world.spawnParticle(Particle.GLOW, center.clone().add(0, 1.5, 0),
            30, adjustedRadius * 0.3, 1, adjustedRadius * 0.3, 0.05);

        return true;
    }
}

// ==================== ESSAIM FURIEUX (Contre-attaque / Vengeance) ====================

@Getter
class SwarmRetaliationPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePercent;          // 15% des dégâts du joueur
    private final long baseCooldownMs;           // 2000ms = 2s
    private final Map<UUID, Long> lastRetaliation = new HashMap<>();

    public SwarmRetaliationPassive(String id, String name, String desc, double dmgPercent, long cooldownMs) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePercent = dmgPercent;
        this.baseCooldownMs = cooldownMs;
        PassiveAbilityCleanup.registerForCleanup(lastRetaliation);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Ajuster le cooldown par niveau (base 2s, min 1s)
        long adjustedCooldown = (long) (baseCooldownMs - (petData.getStatMultiplier() - 1) * 500);
        adjustedCooldown = Math.max(adjustedCooldown, 1000);

        // Vérifier le cooldown
        long lastTime = lastRetaliation.getOrDefault(uuid, 0L);
        if (now - lastTime < adjustedCooldown) {
            return;
        }

        lastRetaliation.put(uuid, now);

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Calculer les dégâts de contre-attaque
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.05;
        double retaliationDamage = playerDamage * adjustedPercent;

        // Trouver l'attaquant le plus proche et contre-attaquer
        Monster target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Monster monster) {
                double dist = entity.getLocation().distanceSquared(playerLoc);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = monster;
                }
            }
        }

        if (target != null) {
            // Animation des 3 mini-abeilles qui attaquent
            final Monster finalTarget = target;
            final Location targetLoc = target.getLocation();

            // Créer 3 trajectoires d'abeilles animées
            for (int i = 0; i < 3; i++) {
                final int beeIndex = i;
                final double angleOffset = (2 * Math.PI / 3) * i;

                Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("ZombieZ"),
                    () -> animateBeeAttack(player, petData, playerLoc.clone(), finalTarget, retaliationDamage / 3, angleOffset),
                    i * 2L
                );
            }

            // Son de l'essaim
            world.playSound(playerLoc, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, 1.2f);

            player.sendMessage("§a[Pet] §e🐝 REPRÉSAILLES! §7L'essaim contre-attaque!");
        }
    }

    private void animateBeeAttack(Player player, PetData petData, Location start, Monster target, double damage, double angleOffset) {
        World world = start.getWorld();

        new BukkitRunnable() {
            Location currentLoc = start.clone().add(0, 1, 0);
            int ticks = 0;
            final int maxTicks = 15;

            @Override
            public void run() {
                if (ticks >= maxTicks || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation().add(0, 1, 0);
                Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());

                if (direction.lengthSquared() < 1) {
                    // Impact!
                    target.damage(damage, player);
                    petData.addDamage((long) damage);

                    // Effet d'impact
                    world.spawnParticle(Particle.CRIT, targetLoc, 5, 0.2, 0.2, 0.2, 0.1);
                    world.playSound(targetLoc, Sound.ENTITY_BEE_STING, 0.8f, 1.5f);
                    cancel();
                    return;
                }

                direction.normalize().multiply(1.5);

                // Mouvement en spirale
                double spiralOffset = Math.sin(ticks * 0.5 + angleOffset) * 0.3;
                currentLoc.add(direction);
                currentLoc.add(
                    Math.cos(ticks * 0.8 + angleOffset) * spiralOffset,
                    Math.sin(ticks * 0.5) * 0.2,
                    Math.sin(ticks * 0.8 + angleOffset) * spiralOffset
                );

                // Particules d'abeille
                world.spawnParticle(Particle.WAX_ON, currentLoc, 2, 0.1, 0.1, 0.1, 0);
                world.spawnParticle(Particle.FALLING_HONEY, currentLoc, 1, 0, 0, 0, 0);

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

@Getter
class SwarmFuryActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double damagePerStingPercent;  // 10% des dégâts du joueur par piqûre
    private final int furyDurationTicks;         // 6s = 120 ticks
    private final int stingIntervalTicks;        // 0.5s = 10 ticks
    private final double attackRadius;           // 8 blocs

    public SwarmFuryActive(String id, String name, String desc, int cd,
                           double dmgPercent, int duration, int interval, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.damagePerStingPercent = dmgPercent;
        this.furyDurationTicks = duration;
        this.stingIntervalTicks = interval;
        this.attackRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Vérifier qu'il y a des ennemis
        boolean hasEnemies = player.getNearbyEntities(attackRadius, attackRadius, attackRadius).stream()
            .anyMatch(e -> e instanceof Monster);

        if (!hasEnemies) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à proximité!");
            return false;
        }

        // Ajuster les valeurs par niveau
        double adjustedRadius = attackRadius + (petData.getStatMultiplier() - 1) * 2;
        int adjustedDuration = (int) (furyDurationTicks + (petData.getStatMultiplier() - 1) * 40);
        double adjustedDmgPercent = damagePerStingPercent + (petData.getStatMultiplier() - 1) * 0.03;

        // Calculer les dégâts par piqûre
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double damagePerSting = playerDamage * adjustedDmgPercent;

        player.sendMessage("§a[Pet] §e§l🐝 FUREUR DE L'ESSAIM! §73 abeilles déchaînées pendant " +
            (adjustedDuration / 20) + "s!");

        // Son initial de rage
        world.playSound(playerLoc, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 2.0f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 1.5f);

        // Spawner les 3 mini-abeilles
        List<Bee> miniBees = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double angle = (2 * Math.PI / 3) * i;
            Location spawnLoc = playerLoc.clone().add(
                Math.cos(angle) * 2, 1.5, Math.sin(angle) * 2
            );

            Bee bee = world.spawn(spawnLoc, Bee.class, b -> {
                // Réduire la taille par 2.5
                b.getAttribute(org.bukkit.attribute.Attribute.SCALE).setBaseValue(0.4);

                // Configuration
                b.setCustomName("§e§l🐝 §6Abeille Furieuse");
                b.setCustomNameVisible(true);
                b.setPersistent(false);
                b.setRemoveWhenFarAway(true);
                b.setAnger(Integer.MAX_VALUE);
                b.setHasStung(false);
                b.setCannotEnterHiveTicks(Integer.MAX_VALUE);

                // Invincible (c'est une invocation temporaire)
                b.setInvulnerable(true);
                b.setAI(false); // On gère le mouvement manuellement

                // Tag pour identification
                b.addScoreboardTag("zombiez_fury_bee");
                b.addScoreboardTag("owner_" + player.getUniqueId());
            });

            miniBees.add(bee);

            // Effet de spawn
            world.spawnParticle(Particle.WAX_ON, spawnLoc, 20, 0.3, 0.3, 0.3, 0.1);
        }

        // Animation et attaques des abeilles
        new BukkitRunnable() {
            int ticksAlive = 0;
            int nextStingTick = stingIntervalTicks;
            final double[] beeAngles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};
            double rotationSpeed = 0.15;

            @Override
            public void run() {
                if (ticksAlive >= adjustedDuration || !player.isOnline()) {
                    // Supprimer les abeilles
                    for (Bee bee : miniBees) {
                        if (bee.isValid()) {
                            world.spawnParticle(Particle.WAX_OFF, bee.getLocation(), 15, 0.3, 0.3, 0.3, 0.1);
                            bee.remove();
                        }
                    }
                    world.playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP, 0.5f, 0.5f);
                    player.sendMessage("§a[Pet] §7L'essaim se calme...");
                    cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1.5, 0);

                // Faire tourner les abeilles autour du joueur
                for (int i = 0; i < miniBees.size(); i++) {
                    Bee bee = miniBees.get(i);
                    if (!bee.isValid()) continue;

                    beeAngles[i] += rotationSpeed;
                    double orbitRadius = 2.5 + Math.sin(ticksAlive * 0.1) * 0.5;

                    Location newLoc = center.clone().add(
                        Math.cos(beeAngles[i]) * orbitRadius,
                        Math.sin(ticksAlive * 0.2 + i) * 0.5,
                        Math.sin(beeAngles[i]) * orbitRadius
                    );

                    // Orienter l'abeille
                    newLoc.setDirection(center.toVector().subtract(newLoc.toVector()));
                    bee.teleport(newLoc);

                    // Particules de traînée
                    if (ticksAlive % 2 == 0) {
                        world.spawnParticle(Particle.WAX_ON, newLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Son de bourdonnement
                if (ticksAlive % 20 == 0) {
                    world.playSound(center, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.6f, 1.0f);
                }

                // Attaquer tous les ennemis à intervalle régulier
                if (ticksAlive >= nextStingTick) {
                    nextStingTick += stingIntervalTicks;

                    List<Monster> targets = new ArrayList<>();
                    for (Entity entity : player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius)) {
                        if (entity instanceof Monster monster) {
                            targets.add(monster);
                        }
                    }

                    if (!targets.isEmpty()) {
                        // Chaque abeille attaque une cible différente si possible
                        for (int i = 0; i < miniBees.size(); i++) {
                            Bee bee = miniBees.get(i);
                            if (!bee.isValid()) continue;

                            Monster target = targets.get(i % targets.size());
                            if (target.isValid() && !target.isDead()) {
                                // Animation de piqûre
                                animateStingAttack(bee.getLocation(), target, damagePerSting, player, petData, world);
                            }
                        }
                    }
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void animateStingAttack(Location from, Monster target, double damage, Player player, PetData petData, World world) {
        Location targetLoc = target.getLocation().add(0, 1, 0);
        Vector direction = targetLoc.toVector().subtract(from.toVector()).normalize();

        new BukkitRunnable() {
            Location currentLoc = from.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 8 || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }

                currentLoc.add(direction.clone().multiply(1.5));

                // Particules de trajectoire
                world.spawnParticle(Particle.CRIT_MAGIC, currentLoc, 2, 0.05, 0.05, 0.05, 0);

                // Vérifier si on a atteint la cible
                if (currentLoc.distanceSquared(targetLoc) < 2) {
                    // Impact!
                    target.damage(damage, player);
                    petData.addDamage((long) damage);

                    // Effet de piqûre
                    world.spawnParticle(Particle.CRIT, targetLoc, 8, 0.3, 0.3, 0.3, 0.1);
                    world.spawnParticle(Particle.FALLING_HONEY, targetLoc, 3, 0.2, 0.2, 0.2, 0);
                    world.playSound(targetLoc, Sound.ENTITY_BEE_STING, 0.6f, 1.2f);

                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== RENIFLEUR ARCHÉOLOGUE (Découverte / RNG positif) ====================

@Getter
class ArchaeologyDigPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attacksForDig;              // 8 attaques
    private final int bonusDurationTicks;         // 5s = 100 ticks
    private final long procCooldownMs;            // 5000ms = 5s cooldown entre procs
    private final Map<UUID, Integer> attackCounters = new HashMap<>();
    private final Map<UUID, Long> activeBonusEnd = new HashMap<>();
    private final Map<UUID, DigBonus> activeBonus = new HashMap<>();
    private final Map<UUID, Long> lastProcTime = new HashMap<>();

    // Types de bonus possibles
    public enum DigBonus {
        DAMAGE("§c⚔ Force Antique", 0.20, Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_STRONG),
        SPEED("§b⚡ Célérité", 0.30, Particle.CLOUD, Sound.ENTITY_PLAYER_LEVELUP),
        HEAL("§a❤ Régénération", 0.10, Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP),
        SHIELD("§e🛡 Bouclier Doré", 10.0, Particle.WAX_ON, Sound.ITEM_ARMOR_EQUIP_GOLD);

        public final String displayName;
        public final double value;
        public final Particle particle;
        public final Sound sound;

        DigBonus(String name, double val, Particle p, Sound s) {
            this.displayName = name;
            this.value = val;
            this.particle = p;
            this.sound = s;
        }
    }

    public ArchaeologyDigPassive(String id, String name, String desc, int attacksNeeded, int durationTicks, long cooldownMs) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attacksForDig = attacksNeeded;
        this.bonusDurationTicks = durationTicks;
        this.procCooldownMs = cooldownMs;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
        PassiveAbilityCleanup.registerForCleanup(activeBonusEnd);
        PassiveAbilityCleanup.registerForCleanup(activeBonus);
        PassiveAbilityCleanup.registerForCleanup(lastProcTime);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int count = attackCounters.getOrDefault(uuid, 0) + 1;

        // Ajuster le nombre d'attaques par niveau
        int adjustedAttacks = attacksForDig - (int)((petData.getStatMultiplier() - 1) * 2);
        adjustedAttacks = Math.max(adjustedAttacks, 4); // Minimum 4 attaques

        // Vérifier si un bonus est actif
        double bonusDamage = damage;
        if (hasActiveBonus(uuid) && activeBonus.get(uuid) == DigBonus.DAMAGE) {
            double damageBoost = DigBonus.DAMAGE.value + (petData.getStatMultiplier() - 1) * 0.10;
            bonusDamage = damage * (1 + damageBoost);

            // Effet visuel du bonus de dégâts
            player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                5, 0.3, 0.3, 0.3, 0.1);
        }

        if (count >= adjustedAttacks) {
            // Vérifier le cooldown entre procs (5s)
            long now = System.currentTimeMillis();
            long lastProc = lastProcTime.getOrDefault(uuid, 0L);

            if (now - lastProc >= procCooldownMs) {
                // Fouille!
                attackCounters.put(uuid, 0);
                lastProcTime.put(uuid, now);
                performDig(player, petData);
            } else {
                // Cooldown actif - reset le compteur mais pas de proc
                attackCounters.put(uuid, 0);
                long remainingCd = (procCooldownMs - (now - lastProc)) / 1000;
                player.sendMessage("§a[Pet] §7Fouille en cooldown... §e" + remainingCd + "s");
            }
        } else {
            attackCounters.put(uuid, count);

            // Indicateur de progression (animation de fouille)
            if (count >= adjustedAttacks - 2) {
                player.getWorld().spawnParticle(Particle.DUST_PLUME, player.getLocation().add(0, 0.5, 0),
                    3, 0.3, 0.1, 0.3, 0.02);
            }
        }

        return bonusDamage;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();

        // Appliquer les effets des bonus actifs
        if (hasActiveBonus(uuid)) {
            DigBonus bonus = activeBonus.get(uuid);

            // Particules du bonus actif
            player.spawnParticle(bonus.particle, player.getLocation().add(0, 1.5, 0),
                2, 0.3, 0.3, 0.3, 0.01);

            // Effet de vitesse
            if (bonus == DigBonus.SPEED) {
                double speedBoost = DigBonus.SPEED.value + (petData.getStatMultiplier() - 1) * 0.10;
                int speedLevel = (int) (speedBoost * 3); // 30% ≈ niveau 1
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 25, speedLevel, false, false));
            }

            // Effet de régénération
            if (bonus == DigBonus.HEAL) {
                // Régénération légère
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, 25, 0, false, false));
            }
        }
    }

    private void performDig(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Choisir un bonus aléatoire
        DigBonus[] bonuses = DigBonus.values();
        DigBonus chosenBonus = bonuses[(int)(Math.random() * bonuses.length)];

        // Ajuster la durée par niveau
        int adjustedDuration = (int) (bonusDurationTicks + (petData.getStatMultiplier() - 1) * 60);

        // Activer le bonus
        activeBonus.put(uuid, chosenBonus);
        activeBonusEnd.put(uuid, System.currentTimeMillis() + adjustedDuration * 50L);

        // Animation de fouille
        Location digLoc = playerLoc.clone().add(playerLoc.getDirection().multiply(1.5));
        digLoc.setY(playerLoc.getY());

        // Effet de creusement
        world.spawnParticle(Particle.DUST_PLUME, digLoc.clone().add(0, 0.3, 0), 30, 0.5, 0.2, 0.5, 0.1);
        world.spawnParticle(Particle.BLOCK, digLoc, 20, 0.3, 0.2, 0.3, 0,
            org.bukkit.Material.DIRT.createBlockData());

        // Son de fouille
        world.playSound(digLoc, Sound.ITEM_BRUSH_BRUSHING_SAND_COMPLETE, 1.2f, 1.0f);
        world.playSound(digLoc, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.8f);

        // Animation de découverte (après un court délai)
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                // Effet de découverte!
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, digLoc.clone().add(0, 1, 0),
                    20, 0.3, 0.5, 0.3, 0.2);
                world.spawnParticle(chosenBonus.particle, digLoc.clone().add(0, 1.5, 0),
                    15, 0.3, 0.3, 0.3, 0.1);

                world.playSound(digLoc, chosenBonus.sound, 1.0f, 1.2f);
                world.playSound(digLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.5f);

                // Appliquer l'effet immédiat si c'est un bouclier
                if (chosenBonus == DigBonus.SHIELD) {
                    double shieldAmount = DigBonus.SHIELD.value + (petData.getStatMultiplier() - 1) * 5;
                    int absorptionLevel = (int) (shieldAmount / 4); // 10 = niveau 2-3
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.ABSORPTION, adjustedDuration, absorptionLevel, false, true));
                }

                player.sendMessage("§a[Pet] §6🦴 DÉCOUVERTE! " + chosenBonus.displayName +
                    " §7(" + (adjustedDuration / 20) + "s)");
            },
            10L
        );

        // Timer pour fin du bonus
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                if (activeBonus.get(uuid) == chosenBonus) {
                    activeBonus.remove(uuid);
                    activeBonusEnd.remove(uuid);
                    player.sendMessage("§a[Pet] §7Le bonus " + chosenBonus.displayName + " §7s'estompe...");
                }
            },
            adjustedDuration
        );
    }

    public boolean hasActiveBonus(UUID uuid) {
        if (!activeBonusEnd.containsKey(uuid)) return false;
        return System.currentTimeMillis() < activeBonusEnd.get(uuid);
    }

    public DigBonus getActiveBonus(UUID uuid) {
        if (hasActiveBonus(uuid)) {
            return activeBonus.get(uuid);
        }
        return null;
    }

    public int getAttackCount(UUID uuid) {
        return attackCounters.getOrDefault(uuid, 0);
    }
}

@Getter
class AncientRelicActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int relicDurationTicks;         // 8s = 160 ticks
    private final double slowAuraPercent;         // 40% slow
    private final double auraRadius;              // Rayon de l'aura

    public AncientRelicActive(String id, String name, String desc, int cd,
                               int duration, double slowPercent, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.relicDurationTicks = duration;
        this.slowAuraPercent = slowPercent;
        this.auraRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Ajuster les valeurs par niveau
        int adjustedDuration = (int) (relicDurationTicks + (petData.getStatMultiplier() - 1) * 60);
        double adjustedRadius = auraRadius + (petData.getStatMultiplier() - 1) * 2;
        double adjustedSlow = slowAuraPercent + (petData.getStatMultiplier() - 1) * 0.10;

        // Calculer les bonus amplifiés
        double damageBoost = 0.20 + (petData.getStatMultiplier() - 1) * 0.15;  // +20% → +35%
        double speedBoost = 0.30 + (petData.getStatMultiplier() - 1) * 0.15;   // +30% → +45%
        double shieldAmount = 10 + (petData.getStatMultiplier() - 1) * 8;       // 10 → 18 cœurs

        player.sendMessage("§a[Pet] §6§l🏛️ RELIQUE ANCESTRALE! §7Tous les bonus pendant " +
            (adjustedDuration / 20) + "s!");

        // Animation d'invocation de la relique
        Location relicLoc = playerLoc.clone().add(playerLoc.getDirection().multiply(2));
        relicLoc.setY(playerLoc.getY());

        // Effet de fouille épique
        world.spawnParticle(Particle.DUST_PLUME, relicLoc, 100, 1, 0.5, 1, 0.2);
        world.playSound(relicLoc, Sound.ITEM_BRUSH_BRUSHING_SAND_COMPLETE, 2.0f, 0.8f);
        world.playSound(relicLoc, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.5f, 0.5f);

        // Après un court délai, révéler la relique
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> {
                // Explosion de découverte!
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, relicLoc.clone().add(0, 1.5, 0),
                    100, 1, 1.5, 1, 0.5);
                world.spawnParticle(Particle.END_ROD, relicLoc.clone().add(0, 2, 0),
                    50, 0.5, 1, 0.5, 0.1);

                // Sons épiques
                world.playSound(relicLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                world.playSound(relicLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
                world.playSound(relicLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

                // Appliquer TOUS les bonus au joueur
                int speedLevel = (int) (speedBoost * 4);
                int absorptionLevel = (int) (shieldAmount / 4);

                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, adjustedDuration, speedLevel, false, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH, adjustedDuration, 1, false, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, adjustedDuration, 1, false, true));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.ABSORPTION, adjustedDuration, absorptionLevel, false, true));

                player.sendMessage("§a[Pet] §c⚔ +20% Dégâts §7| §b⚡ +30% Vitesse §7| §a❤ Regen §7| §e🛡 Bouclier");
            },
            15L
        );

        // Animation continue de la relique et aura de slow
        new BukkitRunnable() {
            int ticksAlive = 0;
            final Location center = playerLoc.clone();

            @Override
            public void run() {
                if (ticksAlive >= adjustedDuration || !player.isOnline()) {
                    // Fin de l'effet
                    world.spawnParticle(Particle.DUST_PLUME, player.getLocation().add(0, 1, 0),
                        30, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
                    player.sendMessage("§a[Pet] §7La relique retourne dans les profondeurs...");
                    cancel();
                    return;
                }

                Location currentLoc = player.getLocation();

                // Particules d'aura autour du joueur (toutes les 3 ticks)
                if (ticksAlive % 3 == 0) {
                    // Cercle de particules
                    for (int angle = 0; angle < 360; angle += 30) {
                        double rad = Math.toRadians(angle + ticksAlive * 3);
                        Location particleLoc = currentLoc.clone().add(
                            Math.cos(rad) * adjustedRadius * 0.8,
                            0.2,
                            Math.sin(rad) * adjustedRadius * 0.8
                        );
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    }

                    // Particules montantes autour du joueur
                    world.spawnParticle(Particle.ENCHANT, currentLoc.add(0, 1, 0),
                        5, 0.5, 0.5, 0.5, 0.5);
                }

                // Appliquer l'aura de slow aux ennemis (toutes les 10 ticks)
                if (ticksAlive % 10 == 0) {
                    int slowLevel = (int) (adjustedSlow * 3); // 40% ≈ niveau 1

                    for (Entity entity : player.getNearbyEntities(adjustedRadius, adjustedRadius, adjustedRadius)) {
                        if (entity instanceof Monster monster) {
                            double distSq = entity.getLocation().distanceSquared(currentLoc);
                            if (distSq <= adjustedRadius * adjustedRadius) {
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 15, slowLevel, false, false));

                                // Effet visuel sur l'ennemi ralenti
                                world.spawnParticle(Particle.DUST_PLUME, monster.getLocation().add(0, 0.5, 0),
                                    3, 0.2, 0.2, 0.2, 0.01);
                            }
                        }
                    }
                }

                // Son ambiant (toutes les 40 ticks)
                if (ticksAlive % 40 == 0) {
                    world.playSound(currentLoc, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.5f);
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 20L, 1L);

        return true;
    }
}
