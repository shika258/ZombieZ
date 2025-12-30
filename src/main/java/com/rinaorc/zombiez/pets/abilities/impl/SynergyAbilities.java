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

// ==================== TORTUE MATRIARCHE (Invocation / Essaim) ====================

@Getter
class TurtleOffspringPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int killsForEgg;               // 8 kills pour pondre
    private final double damagePercent;          // 10% dégâts joueur par hit
    private final int babyDurationTicks;         // 5s = 100 ticks
    private final int maxActiveBabies;           // Max 2 bébés actifs
    private final Map<UUID, Integer> killCounters = new HashMap<>();
    private final Map<UUID, Integer> activeBabyCount = new HashMap<>();

    public TurtleOffspringPassive(String id, String name, String desc, int killsNeeded,
                                   double dmgPercent, int duration, int maxBabies) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.killsForEgg = killsNeeded;
        this.damagePercent = dmgPercent;
        this.babyDurationTicks = duration;
        this.maxActiveBabies = maxBabies;
        PassiveAbilityCleanup.registerForCleanup(killCounters);
        PassiveAbilityCleanup.registerForCleanup(activeBabyCount);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cap de bébés actifs
        int currentBabies = activeBabyCount.getOrDefault(uuid, 0);
        if (currentBabies >= maxActiveBabies) {
            return; // Cap atteint
        }

        // Ajuster le nombre de kills nécessaires par niveau
        int adjustedKills = killsForEgg - (int)((petData.getStatMultiplier() - 1) * 2);
        adjustedKills = Math.max(adjustedKills, 5); // Minimum 5 kills

        int count = killCounters.getOrDefault(uuid, 0) + 1;

        if (count >= adjustedKills) {
            killCounters.put(uuid, 0);
            spawnTurtleEgg(player, petData, killed.getLocation(), false);
        } else {
            killCounters.put(uuid, count);

            // Indicateur de progression
            if (count == adjustedKills - 1) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    public void spawnTurtleEgg(Player player, PetData petData, Location center, boolean isUltimate) {
        World world = center.getWorld();
        UUID uuid = player.getUniqueId();

        // Ajuster les valeurs par niveau
        double adjustedDmgPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.05;
        int adjustedDuration = isUltimate
            ? (int) (babyDurationTicks * 1.6 + (petData.getStatMultiplier() - 1) * 40)  // 8s pour ultimate
            : (int) (babyDurationTicks + (petData.getStatMultiplier() - 1) * 20);

        // Incrémenter le compteur (sauf pour ultimate qui gère son propre count)
        if (!isUltimate) {
            activeBabyCount.merge(uuid, 1, Integer::sum);
        }

        // Effet de ponte
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 15, 0.3, 0.2, 0.3, 0.05);
        world.playSound(center, Sound.ENTITY_TURTLE_LAY_EGG, 1.0f, 1.0f);

        if (!isUltimate) {
            int currentBabies = activeBabyCount.getOrDefault(uuid, 0);
            player.sendMessage("§a[Pet] §2🥚 PONTE! §7Œuf pondu... §8(" + currentBabies + "/" + maxActiveBabies + " bébés)");
        }

        // Délai d'éclosion (2 secondes)
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("ZombieZ"),
            () -> hatchBabyTurtle(player, petData, center, adjustedDmgPercent, adjustedDuration, isUltimate),
            40L // 2 secondes
        );
    }

    private void hatchBabyTurtle(Player player, PetData petData, Location spawnLoc,
                                  double dmgPercent, int duration, boolean isUltimate) {
        if (!player.isOnline()) return;

        World world = spawnLoc.getWorld();
        UUID uuid = player.getUniqueId();

        // Calculer les dégâts du bébé
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double babyDamage = playerDamage * dmgPercent;

        // Effet d'éclosion
        world.spawnParticle(Particle.BLOCK, spawnLoc.add(0, 0.3, 0), 20, 0.3, 0.2, 0.3, 0,
            org.bukkit.Material.TURTLE_EGG.createBlockData());
        world.playSound(spawnLoc, Sound.ENTITY_TURTLE_EGG_HATCH, 1.2f, 1.2f);
        world.playSound(spawnLoc, Sound.ENTITY_TURTLE_SHAMBLE_BABY, 1.0f, 1.0f);

        // Spawner le bébé tortue
        org.bukkit.entity.Turtle babyTurtle = world.spawn(spawnLoc, org.bukkit.entity.Turtle.class, turtle -> {
            turtle.setBaby();
            turtle.setCustomName(isUltimate ? "§c§lTortue Enragée" : "§a§lBébé Tortue");
            turtle.setCustomNameVisible(true);
            turtle.setInvulnerable(true);
            turtle.setPersistent(false);
            turtle.setAI(false); // On gère l'IA manuellement

            // Réduire encore plus la taille avec l'attribut scale
            var scaleAttr = turtle.getAttribute(org.bukkit.attribute.Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(isUltimate ? 0.6 : 0.4);
            }

            // Tag pour identification
            turtle.addScoreboardTag("pet_baby_turtle_" + uuid);
        });

        // Animation et comportement du bébé tortue
        new BukkitRunnable() {
            int ticksAlive = 0;
            int attackTick = 0;

            @Override
            public void run() {
                if (ticksAlive >= duration || !player.isOnline() || !babyTurtle.isValid()) {
                    // Décrémenter le compteur pour le passif
                    if (!isUltimate) {
                        decrementBabyCount(uuid);
                    }

                    // Disparition du bébé
                    if (babyTurtle.isValid()) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, babyTurtle.getLocation().add(0, 0.3, 0),
                            15, 0.3, 0.2, 0.3, 0.05);
                        world.playSound(babyTurtle.getLocation(), Sound.ENTITY_TURTLE_SHAMBLE_BABY, 0.8f, 1.5f);
                        babyTurtle.remove();
                    }
                    cancel();
                    return;
                }

                // Trouver et suivre la cible la plus proche
                Monster target = null;
                double closestDist = 8 * 8; // 8 blocs rayon de recherche

                for (Entity entity : babyTurtle.getNearbyEntities(8, 4, 8)) {
                    if (entity instanceof Monster monster) {
                        double dist = monster.getLocation().distanceSquared(babyTurtle.getLocation());
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = monster;
                        }
                    }
                }

                if (target != null) {
                    // Se déplacer vers la cible
                    Location turtleLoc = babyTurtle.getLocation();
                    Location targetLoc = target.getLocation();
                    Vector dir = targetLoc.toVector().subtract(turtleLoc.toVector());

                    if (dir.lengthSquared() > 0.1) {
                        dir.normalize().multiply(isUltimate ? 0.25 : 0.18); // Vitesse de déplacement
                        dir.setY(0);
                        babyTurtle.setVelocity(dir);

                        // Orienter la tortue vers la cible
                        turtleLoc.setDirection(dir);
                        babyTurtle.teleport(babyTurtle.getLocation().setDirection(dir));
                    }

                    // Attaquer toutes les 10 ticks (0.5s)
                    if (ticksAlive >= attackTick + 10 && closestDist < 2.5 * 2.5) {
                        attackTick = ticksAlive;

                        // Morsure!
                        target.damage(babyDamage, player);
                        petData.addDamage((long) babyDamage);

                        // Slow pour l'ultimate
                        if (isUltimate) {
                            target.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 20, 1, false, false));
                        }

                        // Effets visuels de morsure
                        world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0),
                            8, 0.2, 0.2, 0.2, 0.1);
                        world.playSound(target.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, 0.8f, 1.2f);
                    }
                }

                // Particules de trail
                if (ticksAlive % 5 == 0) {
                    Particle trailParticle = isUltimate ? Particle.ANGRY_VILLAGER : Particle.HAPPY_VILLAGER;
                    world.spawnParticle(trailParticle, babyTurtle.getLocation().add(0, 0.2, 0),
                        2, 0.1, 0.1, 0.1, 0);
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    private void decrementBabyCount(UUID uuid) {
        activeBabyCount.merge(uuid, -1, (old, dec) -> Math.max(0, old + dec));
    }

    public int getActiveBabyCount(UUID uuid) {
        return activeBabyCount.getOrDefault(uuid, 0);
    }

    public int getMaxBabies() {
        return maxActiveBabies;
    }
}

@Getter
class WarNestActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int eggCount;                   // 4 œufs
    private final double damagePercent;           // 15% dégâts joueur
    private final int babyDurationTicks;          // 8s = 160 ticks
    private final TurtleOffspringPassive turtlePassive;

    public WarNestActive(String id, String name, String desc, int cd, int eggs,
                          double dmgPercent, int duration, TurtleOffspringPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.eggCount = eggs;
        this.damagePercent = dmgPercent;
        this.babyDurationTicks = duration;
        this.turtlePassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();

        // Vérifier qu'il y a des ennemis
        boolean hasEnemies = player.getNearbyEntities(12, 8, 12).stream()
            .anyMatch(e -> e instanceof Monster);

        if (!hasEnemies) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à proximité!");
            return false;
        }

        // Ajuster le nombre d'œufs par niveau
        int adjustedEggs = eggCount + (int)((petData.getStatMultiplier() - 1) * 1.5);

        // Effet de préparation du nid
        world.playSound(playerLoc, Sound.ENTITY_TURTLE_AMBIENT_LAND, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.BLOCK_GRASS_BREAK, 1.0f, 0.6f);

        player.sendMessage("§a[Pet] §2§l🥚 NID DE GUERRE! §7" + adjustedEggs + " œufs pondus!");

        // Spawner les œufs en cercle autour du joueur
        for (int i = 0; i < adjustedEggs; i++) {
            double angle = (2 * Math.PI / adjustedEggs) * i;
            Location eggLoc = playerLoc.clone().add(
                Math.cos(angle) * 2.5,
                0,
                Math.sin(angle) * 2.5
            );

            // Délai entre chaque ponte
            final int eggIndex = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    // Effet de ponte
                    world.spawnParticle(Particle.BLOCK, eggLoc, 15, 0.2, 0.1, 0.2, 0,
                        org.bukkit.Material.SAND.createBlockData());
                    world.playSound(eggLoc, Sound.ENTITY_TURTLE_LAY_EGG, 1.0f, 1.0f + (eggIndex * 0.1f));

                    // Spawner l'œuf via le passif (avec flag ultimate=true)
                    turtlePassive.spawnTurtleEgg(player, petData, eggLoc, true);
                },
                i * 5L // 0.25s entre chaque œuf
            );
        }

        // Effet visuel central
        world.spawnParticle(Particle.HAPPY_VILLAGER, playerLoc, 30, 2, 0.5, 2, 0.1);

        return true;
    }
}

// ==================== AXOLOTL PRISMATIQUE (Réactions Élémentaires) ====================

@Getter
class ElementalCatalystPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double reactionDamagePercent;      // 50% dégâts joueur pour les réactions
    private final int elementCycleTicks;              // 5s = 100 ticks entre rotations
    private final int reactionCooldownMs;             // Cooldown entre réactions sur même cible

    // 0=Fire (LUCY), 1=Ice (CYAN), 2=Lightning (GOLD)
    private final Map<UUID, Integer> currentElement = new HashMap<>();
    private final Map<UUID, Long> lastRotation = new HashMap<>();
    // Marques élémentaires sur les mobs: EntityUUID -> Set d'éléments (0,1,2)
    private final Map<UUID, Set<Integer>> elementalMarks = new HashMap<>();
    private final Map<UUID, Long> lastReactionTime = new HashMap<>();

    public ElementalCatalystPassive(String id, String name, String desc, double reactionDmg,
                                     int cycleTicks, int reactionCooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.reactionDamagePercent = reactionDmg;
        this.elementCycleTicks = cycleTicks;
        this.reactionCooldownMs = reactionCooldown;
        PassiveAbilityCleanup.registerForCleanup(currentElement);
        PassiveAbilityCleanup.registerForCleanup(lastRotation);
        PassiveAbilityCleanup.registerForCleanup(lastReactionTime);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastRotation.getOrDefault(uuid, 0L);

        // Ajuster le cycle par niveau (plus rapide à haut niveau)
        int adjustedCycle = (int) (elementCycleTicks * 50 - (petData.getStatMultiplier() - 1) * 500);
        adjustedCycle = Math.max(adjustedCycle, 3000); // Minimum 3s

        // Rotation automatique des éléments
        if (now - last > adjustedCycle) {
            int current = currentElement.getOrDefault(uuid, 0);
            int newElement = (current + 1) % 3;
            currentElement.put(uuid, newElement);
            lastRotation.put(uuid, now);

            // Changer la couleur de l'axolotl
            updateAxolotlColor(player, newElement);

            String element = getElementName(newElement);
            player.sendMessage("§a[Pet] §7Élément actif: " + element);
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        int currentEl = currentElement.getOrDefault(playerUuid, 0);
        World world = target.getWorld();

        // Récupérer les marques existantes sur la cible
        Set<Integer> marks = elementalMarks.computeIfAbsent(targetUuid, k -> new HashSet<>());

        // Vérifier si on peut déclencher une réaction
        boolean reactionTriggered = false;
        double bonusDamage = 0;

        if (!marks.isEmpty() && !marks.contains(currentEl)) {
            // Vérifier le cooldown de réaction
            long now = System.currentTimeMillis();
            long lastReaction = lastReactionTime.getOrDefault(targetUuid, 0L);

            int adjustedCooldown = (int) (reactionCooldownMs - (petData.getStatMultiplier() - 1) * 200);
            adjustedCooldown = Math.max(adjustedCooldown, 1500); // Min 1.5s

            if (now - lastReaction >= adjustedCooldown) {
                // Déclencher la réaction!
                for (int existingMark : marks) {
                    bonusDamage += triggerReaction(player, petData, target, existingMark, currentEl);
                    reactionTriggered = true;
                }

                if (reactionTriggered) {
                    lastReactionTime.put(targetUuid, now);
                    // Nettoyer les marques après réaction
                    marks.clear();
                }
            }
        }

        // Ajouter la nouvelle marque
        marks.add(currentEl);

        // Effet visuel de marquage
        Particle markParticle = getElementParticle(currentEl);
        world.spawnParticle(markParticle, target.getLocation().add(0, 1.5, 0), 8, 0.2, 0.2, 0.2, 0.02);

        // Appliquer l'effet élémentaire de base
        applyElementEffect(target, currentEl);

        // Nettoyer les marques des mobs morts
        cleanupDeadMarks();

        return damage + bonusDamage;
    }

    private double triggerReaction(Player player, PetData petData, LivingEntity target,
                                    int element1, int element2) {
        World world = target.getWorld();
        Location loc = target.getLocation();

        // Calculer les dégâts de réaction
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double adjustedPercent = reactionDamagePercent + (petData.getStatMultiplier() - 1) * 0.15;
        double reactionDamage = playerDamage * adjustedPercent;

        // Déterminer le type de réaction (utiliser les plus petits indices pour cohérence)
        int min = Math.min(element1, element2);
        int max = Math.max(element1, element2);

        if (min == 0 && max == 1) {
            // VAPORISATION (Feu + Glace) - Burst de dégâts
            target.damage(reactionDamage, player);
            petData.addDamage((long) reactionDamage);

            // Effet visuel de vapeur
            world.spawnParticle(Particle.CLOUD, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.FLAME, loc, 15, 0.3, 0.3, 0.3, 0.05);
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 1.0f);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0f, 0.8f);

            player.sendMessage("§a[Pet] §6§l⚗ VAPORISATION! §c+" + (int)reactionDamage + " §7dégâts!");

            return reactionDamage;

        } else if (min == 0 && max == 2) {
            // SURCHARGE (Feu + Foudre) - Explosion AoE + knockback
            double aoeDamage = reactionDamage * 0.6;

            for (Entity entity : target.getNearbyEntities(4, 3, 4)) {
                if (entity instanceof Monster monster) {
                    monster.damage(aoeDamage, player);
                    petData.addDamage((long) aoeDamage);

                    // Knockback
                    Vector knockback = monster.getLocation().toVector()
                        .subtract(loc.toVector()).normalize().multiply(1.2);
                    knockback.setY(0.5);
                    monster.setVelocity(knockback);

                    // Particules sur chaque cible
                    world.spawnParticle(Particle.EXPLOSION, monster.getLocation(), 1);
                }
            }

            // Effet central
            world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 1, 1, 1, 0.3);
            world.spawnParticle(Particle.FLAME, loc, 20, 0.8, 0.5, 0.8, 0.1);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
            world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);

            player.sendMessage("§a[Pet] §e§l⚡ SURCHARGE! §7Explosion AoE + knockback!");

            return aoeDamage;

        } else if (min == 1 && max == 2) {
            // SUPRACONDUCTION (Glace + Foudre) - Réduction de défense
            target.damage(reactionDamage * 0.5, player);
            petData.addDamage((long) (reactionDamage * 0.5));

            // Appliquer la réduction de défense (via Weakness)
            int weaknessDuration = (int) (100 + (petData.getStatMultiplier() - 1) * 40); // 5s+
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 1, false, true));

            // Appliquer aussi un slow renforcé
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, weaknessDuration, 2, false, false));

            // Effet visuel électrique + givré
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc.add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.1);
            world.spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.5, 0.5, 0.5, 0.05);
            world.spawnParticle(Particle.BLOCK, loc, 10, 0.3, 0.3, 0.3, 0,
                org.bukkit.Material.BLUE_ICE.createBlockData());
            world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);

            player.sendMessage("§a[Pet] §b§l❄ SUPRACONDUCTION! §7-20% défense pendant 5s!");

            return reactionDamage * 0.5;
        }

        return 0;
    }

    private void applyElementEffect(LivingEntity target, int element) {
        switch (element) {
            case 0 -> target.setFireTicks(40); // Feu léger
            case 1 -> target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 30, 0, false, false)); // Slow léger
            case 2 -> {} // Foudre n'a pas d'effet passif
        }
    }

    private void updateAxolotlColor(Player player, int element) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(20, 10, 20)) {
            if (entity instanceof org.bukkit.entity.Axolotl axolotl
                && entity.getScoreboardTags().contains(ownerTag)) {

                org.bukkit.entity.Axolotl.Variant variant = switch (element) {
                    case 0 -> org.bukkit.entity.Axolotl.Variant.LUCY;      // Rose/Rouge = Feu
                    case 1 -> org.bukkit.entity.Axolotl.Variant.CYAN;      // Cyan = Glace
                    case 2 -> org.bukkit.entity.Axolotl.Variant.GOLD;      // Or = Foudre
                    default -> org.bukkit.entity.Axolotl.Variant.WILD;
                };

                axolotl.setVariant(variant);

                // Particules de transition
                Particle particle = getElementParticle(element);
                axolotl.getWorld().spawnParticle(particle, axolotl.getLocation().add(0, 0.3, 0),
                    15, 0.3, 0.2, 0.3, 0.05);

                break;
            }
        }
    }

    private String getElementName(int element) {
        return switch (element) {
            case 0 -> "§c🔥 Feu";
            case 1 -> "§b❄ Glace";
            case 2 -> "§e⚡ Foudre";
            default -> "§7?";
        };
    }

    private Particle getElementParticle(int element) {
        return switch (element) {
            case 0 -> Particle.FLAME;
            case 1 -> Particle.SNOWFLAKE;
            case 2 -> Particle.ELECTRIC_SPARK;
            default -> Particle.CRIT;
        };
    }

    private void cleanupDeadMarks() {
        elementalMarks.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            return entity == null || entity.isDead();
        });
    }

    public int getCurrentElement(UUID uuid) {
        return currentElement.getOrDefault(uuid, 0);
    }

    public void setElement(UUID uuid, int element) {
        currentElement.put(uuid, element % 3);
        lastRotation.put(uuid, System.currentTimeMillis());
    }

    public void applyAllMarks(LivingEntity target) {
        Set<Integer> marks = elementalMarks.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>());
        marks.add(0);
        marks.add(1);
        marks.add(2);
    }
}

@Getter
class ChainReactionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final double reactionDamagePercent;
    private final ElementalCatalystPassive catalystPassive;

    public ChainReactionActive(String id, String name, String desc, int cd,
                                double reactionDmg, ElementalCatalystPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.reactionDamagePercent = reactionDmg;
        this.catalystPassive = passive;
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
        double adjustedPercent = reactionDamagePercent + (petData.getStatMultiplier() - 1) * 0.20;

        // Changer l'axolotl en variante BLUE (rare) temporairement
        setAxolotlBlue(player);

        // Effet de préparation
        world.playSound(playerLoc, Sound.ENTITY_AXOLOTL_ATTACK, 1.5f, 0.6f);
        world.playSound(playerLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        player.sendMessage("§a[Pet] §d§l⚗ RÉACTION EN CHAÎNE! §7Toutes les réactions déclenchées!");

        // Appliquer les 3 éléments et déclencher les réactions sur chaque cible
        int hitCount = 0;
        for (Monster target : targets) {
            // Appliquer toutes les marques
            catalystPassive.applyAllMarks(target);

            Location targetLoc = target.getLocation();

            // Déclencher les 3 réactions avec délai pour l'effet visuel
            final int targetIndex = hitCount;

            // Vaporisation (Feu + Glace)
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    if (target.isValid() && !target.isDead()) {
                        double vapDamage = playerDamage * adjustedPercent * 0.4;
                        target.damage(vapDamage, player);
                        petData.addDamage((long) vapDamage);

                        world.spawnParticle(Particle.CLOUD, targetLoc.add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.08);
                        world.spawnParticle(Particle.FLAME, targetLoc, 10, 0.2, 0.2, 0.2, 0.03);
                        world.playSound(targetLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
                    }
                },
                targetIndex * 2L + 5L
            );

            // Surcharge (Feu + Foudre)
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    if (target.isValid() && !target.isDead()) {
                        double surgeDamage = playerDamage * adjustedPercent * 0.3;
                        target.damage(surgeDamage, player);
                        petData.addDamage((long) surgeDamage);

                        // Mini knockback
                        Vector kb = target.getLocation().toVector()
                            .subtract(playerLoc.toVector()).normalize().multiply(0.6);
                        kb.setY(0.3);
                        target.setVelocity(kb);

                        world.spawnParticle(Particle.EXPLOSION, targetLoc, 1);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 15, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(targetLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.0f);
                    }
                },
                targetIndex * 2L + 10L
            );

            // Supraconduction (Glace + Foudre)
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    if (target.isValid() && !target.isDead()) {
                        double supDamage = playerDamage * adjustedPercent * 0.3;
                        target.damage(supDamage, player);
                        petData.addDamage((long) supDamage);

                        target.addPotionEffect(new PotionEffect(
                            PotionEffectType.WEAKNESS, 100, 1, false, true));
                        target.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 80, 2, false, false));

                        world.spawnParticle(Particle.SNOWFLAKE, targetLoc, 15, 0.4, 0.4, 0.4, 0.05);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);
                        world.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);
                    }
                },
                targetIndex * 2L + 15L
            );

            // Effets élémentaires de base
            target.setFireTicks(60);

            hitCount++;
        }

        // Effet central multicolore
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 30) {
                    // Restaurer la couleur normale de l'axolotl
                    catalystPassive.updateAxolotlColor(player, catalystPassive.getCurrentElement(uuid));
                    cancel();
                    return;
                }

                // Particules arc-en-ciel autour du joueur
                double angle = ticks * 0.5;
                for (int i = 0; i < 3; i++) {
                    double offset = (2 * Math.PI / 3) * i;
                    Location particleLoc = playerLoc.clone().add(
                        Math.cos(angle + offset) * 2,
                        0.5 + Math.sin(ticks * 0.2) * 0.5,
                        Math.sin(angle + offset) * 2
                    );

                    Particle particle = switch (i) {
                        case 0 -> Particle.FLAME;
                        case 1 -> Particle.SNOWFLAKE;
                        case 2 -> Particle.ELECTRIC_SPARK;
                        default -> Particle.CRIT;
                    };

                    world.spawnParticle(particle, particleLoc, 3, 0.1, 0.1, 0.1, 0.01);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    private void setAxolotlBlue(Player player) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(20, 10, 20)) {
            if (entity instanceof org.bukkit.entity.Axolotl axolotl
                && entity.getScoreboardTags().contains(ownerTag)) {

                // Variante BLUE (rare) pour l'ultimate
                axolotl.setVariant(org.bukkit.entity.Axolotl.Variant.BLUE);

                // Effet de transformation
                axolotl.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, axolotl.getLocation(),
                    30, 0.5, 0.5, 0.5, 0.3);
                axolotl.getWorld().playSound(axolotl.getLocation(),
                    Sound.ENTITY_AXOLOTL_SPLASH, 1.0f, 0.8f);

                break;
            }
        }
    }
}

// ==================== GOURMAND SYSTEM (Panda Gourmand) ====================

@Getter
class GourmandPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int baseKillsRequired;
    private final int baseDurationTicks;
    private final Map<UUID, Integer> killCounts = new HashMap<>();
    private final Map<UUID, GourmandBuff> activeBuffs = new HashMap<>();

    // Types de buffs possibles
    public enum GourmandBuff {
        FORCE("§c⚔ Force", "+25% dégâts", 0.25),
        VITESSE("§b⚡ Vélocité", "+30% vitesse", 0.30),
        REGEN("§a❤ Régénération", "+3♥/s", 0.0), // Spécial: applique regen directement
        CRIT("§e✦ Précision", "+20% crit chance", 0.20);

        @Getter private final String displayName;
        @Getter private final String desc;
        @Getter private final double value;

        GourmandBuff(String displayName, String desc, double value) {
            this.displayName = displayName;
            this.desc = desc;
            this.value = value;
        }
    }

    public GourmandPassive(String id, String name, String desc, int killsRequired, int durationTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseKillsRequired = killsRequired;
        this.baseDurationTicks = durationTicks;
        PassiveAbilityCleanup.registerForCleanup(killCounts);
        PassiveAbilityCleanup.registerForCleanup(activeBuffs);
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Kills requis selon le niveau (niveau élevé = moins de kills)
     */
    private int getEffectiveKillsRequired(PetData petData) {
        // Base 5, réduit à 4 avec niveau 5+
        double mult = petData.getStatMultiplier();
        return mult >= 1.5 ? baseKillsRequired - 1 : baseKillsRequired;
    }

    /**
     * Durée des buffs selon le niveau
     */
    private int getEffectiveDuration(PetData petData) {
        // Base 8s (160 ticks), monte à 12s (240 ticks) avec niveau max
        double mult = petData.getStatMultiplier();
        return (int) (baseDurationTicks * (mult >= 1.5 ? 1.5 : 1.0));
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();
        int kills = killCounts.getOrDefault(uuid, 0) + 1;
        int required = getEffectiveKillsRequired(petData);

        if (kills >= required) {
            // Reset le compteur
            killCounts.put(uuid, 0);

            // Choix aléatoire du buff
            GourmandBuff buff = GourmandBuff.values()[new Random().nextInt(GourmandBuff.values().length)];
            activeBuffs.put(uuid, buff);

            int duration = getEffectiveDuration(petData);

            // Appliquer le buff spécifique
            if (buff == GourmandBuff.REGEN) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1)); // Regen II
            } else if (buff == GourmandBuff.VITESSE) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1)); // Speed II
            }

            // Effets visuels
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PANDA_EAT, 1.0f, 1.2f);
            player.sendMessage("§a[Pet] §e" + buff.getDisplayName() + " §7activé! (" + buff.getDesc() + " §7pendant " + (duration / 20) + "s)");

            // Programmer la fin du buff
            new BukkitRunnable() {
                @Override
                public void run() {
                    activeBuffs.remove(uuid);
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), duration);

        } else {
            killCounts.put(uuid, kills);

            // Notification de progression tous les kills
            if (kills == required - 1) {
                player.spawnParticle(Particle.COMPOSTER, player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        GourmandBuff buff = activeBuffs.get(player.getUniqueId());
        if (buff != null) {
            return switch (buff) {
                case FORCE -> damage * (1.0 + buff.getValue() * petData.getStatMultiplier());
                case CRIT -> damage; // Le crit est géré différemment
                default -> damage;
            };
        }
        return damage;
    }

    /**
     * Retourne le buff actif pour le joueur (utilisé par d'autres systèmes)
     */
    public GourmandBuff getActiveBuff(UUID uuid) {
        return activeBuffs.get(uuid);
    }

    /**
     * Retourne le nombre de kills actuel
     */
    public int getKillCount(UUID uuid) {
        return killCounts.getOrDefault(uuid, 0);
    }
}

@Getter
class SneezingExplosiveActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final GourmandPassive gourmandPassive;
    private final double baseDamagePercent; // % des dégâts du joueur
    private final double baseDropChance;    // Chance de drop
    private static final Random random = new Random();

    public SneezingExplosiveActive(String id, String name, String desc, GourmandPassive passive,
                                    double damagePercent, double dropChance) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.gourmandPassive = passive;
        this.baseDamagePercent = damagePercent;
        this.baseDropChance = dropChance;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Récupérer les dégâts du joueur via le plugin
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        Map<com.rinaorc.zombiez.items.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);

        // Calculer les dégâts finaux: (base + flat) * (1 + percent) * damagePercent de l'ultime
        double playerBaseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);
        double effectiveDamagePercent = baseDamagePercent * petData.getStatMultiplier();
        double ultimateDamage = playerBaseDamage * effectiveDamagePercent;

        // Effet visuel de l'éternuement
        Location loc = player.getLocation();
        Vector direction = loc.getDirection().normalize();

        // Particules d'éternuement (vert bambou)
        for (int i = 0; i < 30; i++) {
            double spread = 0.5;
            Vector offset = new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            );
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                loc.clone().add(direction.clone().multiply(2)).add(offset).add(0, 1, 0),
                1, 0, 0, 0, 0);
        }

        // Onde de choc circulaire
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * 6;
            double z = Math.sin(angle) * 6;
            player.getWorld().spawnParticle(Particle.SNEEZE,
                loc.clone().add(x, 0.5, z), 2, 0.2, 0.1, 0.2, 0.02);
        }

        // Son de l'éternuement
        player.getWorld().playSound(loc, Sound.ENTITY_PANDA_SNEEZE, 2.0f, 0.8f);
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        // Infliger les dégâts aux ennemis dans le rayon
        int enemiesHit = 0;
        Collection<Entity> nearby = player.getNearbyEntities(6, 6, 6);
        for (Entity entity : nearby) {
            if (entity instanceof Monster monster) {
                monster.damage(ultimateDamage, player);
                petData.addDamage((long) ultimateDamage);
                enemiesHit++;

                // Appliquer knockback léger
                Vector knockback = monster.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize().multiply(0.8).setY(0.3);
                monster.setVelocity(knockback);

                // Particule d'impact
                monster.getWorld().spawnParticle(Particle.SNEEZE,
                    monster.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
            }
        }

        // Appliquer tous les buffs pendant 5s (100 ticks)
        int buffDuration = (int) (100 * petData.getStatMultiplier());
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, buffDuration, 0));      // Force I
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, buffDuration, 1));         // Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, buffDuration, 1));  // Regen II

        // Chance de drop (30% au max level)
        double dropChance = petData.getStarPower() > 0 ? baseDropChance : 0;
        if (random.nextDouble() < dropChance) {
            dropRandomConsumableOrFood(player, plugin);
        }

        player.sendMessage("§a[Pet] §e§lATCHOUM! §7" + enemiesHit + " ennemis touchés! (§c" +
            String.format("%.0f", ultimateDamage) + " §7dégâts)");

        return true;
    }

    /**
     * Drop un consommable ou nourriture aléatoire
     */
    private void dropRandomConsumableOrFood(Player player, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        // 50% chance nourriture, 50% chance consommable
        if (random.nextBoolean()) {
            // Drop nourriture depuis FoodItemRegistry
            var foodRegistry = plugin.getFoodItemRegistry();
            if (foodRegistry != null) {
                var food = foodRegistry.getRandomZombieFood();
                if (food != null) {
                    org.bukkit.inventory.ItemStack foodItem = food.createItemStack();
                    player.getWorld().dropItemNaturally(player.getLocation(), foodItem);
                    player.sendMessage("§a[Pet] §6✦ §eLe Panda a trouvé: " + food.getRarity().getColor() + food.getDisplayName());
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                }
            }
        } else {
            // Drop consommable aléatoire
            var consumableTypes = com.rinaorc.zombiez.consumables.ConsumableType.values();
            var randomType = consumableTypes[random.nextInt(consumableTypes.length)];

            // Rareté aléatoire pondérée (plus de commun/uncommon)
            var rarities = com.rinaorc.zombiez.consumables.ConsumableRarity.values();
            double roll = random.nextDouble();
            com.rinaorc.zombiez.consumables.ConsumableRarity rarity;
            if (roll < 0.5) rarity = rarities[0];      // 50% Common
            else if (roll < 0.8) rarity = rarities[1]; // 30% Uncommon
            else if (roll < 0.95) rarity = rarities[2]; // 15% Rare
            else rarity = rarities[3];                  // 5% Epic

            // Zone du joueur pour le scaling
            int zone = plugin.getZoneManager().getPlayerZone(player);

            var consumable = new com.rinaorc.zombiez.consumables.Consumable(randomType, rarity, zone);
            org.bukkit.inventory.ItemStack consumableItem = consumable.createItemStack();
            player.getWorld().dropItemNaturally(player.getLocation(), consumableItem);
            player.sendMessage("§a[Pet] §6✦ §eLe Panda a trouvé: " + rarity.getColor() + randomType.getDisplayName());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        }
    }
}

// ==================== FLIPPER SYSTEM (Chèvre Flipper) ====================

@Getter
class FlipperPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseDamagePercent;      // % des dégâts du joueur par rebond
    private final double damageIncreasePerBounce; // Bonus par rebond
    private final int baseMaxBounces;             // Nombre max de rebonds
    private static final Random random = new Random();

    public FlipperPassive(String id, String name, String desc, double baseDamage, double increasePerBounce, int maxBounces) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseDamagePercent = baseDamage;
        this.damageIncreasePerBounce = increasePerBounce;
        this.baseMaxBounces = maxBounces;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Max rebonds selon le niveau
     */
    public int getMaxBounces(PetData petData) {
        return petData.getStatMultiplier() >= 1.5 ? baseMaxBounces + 2 : baseMaxBounces;
    }

    /**
     * Bonus de dégâts par rebond selon le niveau
     */
    public double getDamagePerBounce(PetData petData) {
        return petData.getStatMultiplier() >= 1.5 ? damageIncreasePerBounce + 0.05 : damageIncreasePerBounce;
    }

    /**
     * Vérifie si l'explosion au kill est active (max étoiles)
     */
    public boolean hasExplosionOnKill(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        // Récupérer les dégâts du joueur
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        Map<com.rinaorc.zombiez.items.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double playerBaseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        // Démarrer la chaîne de rebonds
        startBounceChain(player, petData, killed.getLocation(), playerBaseDamage, 1, new HashSet<>());
    }

    /**
     * Chaîne de rebonds récursive
     */
    private void startBounceChain(Player player, PetData petData, Location fromLocation,
                                   double playerDamage, int bounceNumber, Set<UUID> alreadyHit) {
        int maxBounces = getMaxBounces(petData);
        if (bounceNumber > maxBounces) return;

        // Trouver l'ennemi le plus proche non encore touché
        Monster target = findNearestEnemy(fromLocation, 8.0, alreadyHit);
        if (target == null) return;

        alreadyHit.add(target.getUniqueId());

        // Calculer les dégâts avec bonus croissant
        double damageMultiplier = baseDamagePercent + (getDamagePerBounce(petData) * (bounceNumber - 1));
        double bounceDamage = playerDamage * damageMultiplier * petData.getStatMultiplier();

        // Animation de projectile rebondissant
        Location targetLoc = target.getLocation().add(0, 1, 0);
        spawnBounceTrail(fromLocation.clone().add(0, 1, 0), targetLoc, bounceNumber);

        // Infliger les dégâts avec délai pour l'effet visuel
        int finalBounceNumber = bounceNumber;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) return;

                target.damage(bounceDamage, player);
                petData.addDamage((long) bounceDamage);

                // Effet d'impact
                target.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.3, 0.3, 0.3, 0.1);
                target.getWorld().playSound(targetLoc, Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.2f + (finalBounceNumber * 0.1f));

                // Notification avec numéro de rebond
                String bounceText = "§a[Pet] §e⚡ Rebond #" + finalBounceNumber + " §7→ §c" +
                    String.format("%.0f", bounceDamage) + " §7dégâts!";
                player.sendMessage(bounceText);

                // Vérifier si la cible est morte pour continuer la chaîne
                if (target.isDead() || target.getHealth() <= 0) {
                    // Explosion si max étoiles
                    if (hasExplosionOnKill(petData)) {
                        explodeOnKill(player, petData, target.getLocation(), playerDamage * 0.4);
                    }
                    // Continuer la chaîne
                    startBounceChain(player, petData, target.getLocation(), playerDamage, finalBounceNumber + 1, alreadyHit);
                }
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), 3L * bounceNumber);
    }

    /**
     * Explosion lors d'un kill par ricochet (bonus max étoiles)
     */
    private void explodeOnKill(Player player, PetData petData, Location loc, double damage) {
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof Monster monster && !entity.equals(player)) {
                monster.damage(damage, player);
                petData.addDamage((long) damage);
            }
        }
    }

    /**
     * Effet visuel de traînée de rebond
     */
    private void spawnBounceTrail(Location from, Location to, int bounceNumber) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        // Couleur selon le numéro de rebond (vert → jaune → orange → rouge)
        Particle.DustOptions dust = switch (bounceNumber) {
            case 1 -> new Particle.DustOptions(org.bukkit.Color.LIME, 1.2f);
            case 2 -> new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.3f);
            case 3 -> new Particle.DustOptions(org.bukkit.Color.ORANGE, 1.4f);
            case 4 -> new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
            default -> new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.6f);
        };

        new BukkitRunnable() {
            double traveled = 0;
            final Location current = from.clone();

            @Override
            public void run() {
                if (traveled >= distance) {
                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(0.8));
                current.getWorld().spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, 0, dust);
                current.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, current, 1, 0.1, 0.1, 0.1, 0);
                traveled += 0.8;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Trouve l'ennemi le plus proche
     */
    private Monster findNearestEnemy(Location loc, double radius, Set<UUID> exclude) {
        Monster nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Monster monster && !exclude.contains(entity.getUniqueId())) {
                double dist = monster.getLocation().distanceSquared(loc);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = monster;
                }
            }
        }
        return nearest;
    }
}

@Getter
class FlipperBallActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final FlipperPassive flipperPassive;
    private final double baseDamagePercent;
    private final int maxTargets;
    private static final Random random = new Random();

    public FlipperBallActive(String id, String name, String desc, FlipperPassive passive,
                              double damagePercent, int targets) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.flipperPassive = passive;
        this.baseDamagePercent = damagePercent;
        this.maxTargets = targets;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 25; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Récupérer les dégâts du joueur
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        Map<com.rinaorc.zombiez.items.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double playerBaseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        // Trouver tous les ennemis dans le rayon
        List<Monster> targets = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Monster monster) {
                targets.add(monster);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à portée!");
            return false;
        }

        // Limiter au nombre max et mélanger
        Collections.shuffle(targets);
        List<Monster> finalTargets = targets.subList(0, Math.min(targets.size(), maxTargets));

        // Son de départ
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GOAT_SCREAMING_LONG_JUMP, 1.5f, 0.8f);
        player.sendMessage("§a[Pet] §e§l🎱 BOULE DE FLIPPER! §7Rebondissement sur " + finalTargets.size() + " ennemis!");

        // Lancer la séquence de rebonds
        executeFlipperBall(player, petData, player.getLocation(), finalTargets, 0, playerBaseDamage);

        return true;
    }

    /**
     * Exécute la séquence de rebonds de la boule de flipper
     */
    private void executeFlipperBall(Player player, PetData petData, Location fromLoc,
                                     List<Monster> targets, int index, double playerDamage) {
        if (index >= targets.size()) {
            // Fin de la séquence
            player.sendMessage("§a[Pet] §6§l★ COMBO PARFAIT! §7Tous les rebonds effectués!");
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
            return;
        }

        Monster target = targets.get(index);
        if (!target.isValid() || target.isDead()) {
            // Passer au suivant si la cible n'est plus valide
            executeFlipperBall(player, petData, fromLoc, targets, index + 1, playerDamage);
            return;
        }

        int bounceNumber = index + 1;
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Dégâts croissants: 50% × numéro du rebond
        double damageMultiplier = baseDamagePercent * bounceNumber * petData.getStatMultiplier();
        double bounceDamage = playerDamage * damageMultiplier;

        // Animation de la boule
        spawnFlipperBallTrail(fromLoc.clone().add(0, 1, 0), targetLoc, bounceNumber);

        // Délai pour synchroniser avec l'animation
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    executeFlipperBall(player, petData, targetLoc, targets, index + 1, playerDamage);
                    return;
                }

                // Infliger les dégâts
                target.damage(bounceDamage, player);
                petData.addDamage((long) bounceDamage);

                // Knockback puissant
                Vector knockback = target.getLocation().toVector()
                    .subtract(fromLoc.toVector())
                    .normalize().multiply(1.2).setY(0.5);
                target.setVelocity(knockback);

                // Effets visuels d'impact
                target.getWorld().spawnParticle(Particle.CRIT_MAGIC, targetLoc, 25, 0.5, 0.5, 0.5, 0.2);
                target.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 1, 0, 0, 0, 0);

                // Son d'impact crescendo
                float pitch = 0.5f + (bounceNumber * 0.15f);
                target.getWorld().playSound(targetLoc, Sound.ENTITY_GOAT_RAM_IMPACT, 1.2f, pitch);
                target.getWorld().playSound(targetLoc, Sound.BLOCK_SLIME_BLOCK_HIT, 1.0f, pitch);

                // Message avec dégâts croissants
                String damageColor = bounceNumber <= 3 ? "§e" : (bounceNumber <= 6 ? "§6" : "§c");
                player.sendMessage("§a[Pet] §e⚡ Rebond #" + bounceNumber + " §7→ " +
                    damageColor + String.format("%.0f", bounceDamage) + " §7dégâts! " +
                    (bounceNumber >= 6 ? "§c§l(×" + bounceNumber + "!)" : ""));

                // Continuer vers le prochain
                executeFlipperBall(player, petData, target.getLocation(), targets, index + 1, playerDamage);
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), 5L + (index * 4L));
    }

    /**
     * Effet visuel de la boule de flipper
     */
    private void spawnFlipperBallTrail(Location from, Location to, int bounceNumber) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        // Couleur arc-en-ciel selon le rebond
        org.bukkit.Color color = switch (bounceNumber % 7) {
            case 1 -> org.bukkit.Color.RED;
            case 2 -> org.bukkit.Color.ORANGE;
            case 3 -> org.bukkit.Color.YELLOW;
            case 4 -> org.bukkit.Color.LIME;
            case 5 -> org.bukkit.Color.AQUA;
            case 6 -> org.bukkit.Color.PURPLE;
            default -> org.bukkit.Color.WHITE;
        };
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f + (bounceNumber * 0.2f));

        new BukkitRunnable() {
            double traveled = 0;
            final Location current = from.clone();

            @Override
            public void run() {
                if (traveled >= distance) {
                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.2));
                current.getWorld().spawnParticle(Particle.DUST, current, 5, 0.15, 0.15, 0.15, 0, dust);
                current.getWorld().spawnParticle(Particle.END_ROD, current, 2, 0.1, 0.1, 0.1, 0.02);
                current.getWorld().spawnParticle(Particle.FIREWORK, current, 1, 0.05, 0.05, 0.05, 0);
                traveled += 1.2;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== EGG BOMBER SYSTEM (Poulet Bombardier) ====================

@Getter
class EggBomberPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int baseAttacksNeeded;       // Nombre d'attaques pour déclencher
    private final double baseDamagePercent;    // % des dégâts du joueur
    private final double baseGoldenChance;     // Chance d'œuf doré (bonus max étoiles)
    private final Map<UUID, Integer> attackCounters = new HashMap<>();
    private static final Random random = new Random();

    public EggBomberPassive(String id, String name, String desc, int attacksNeeded,
                             double damagePercent, double goldenChance) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseAttacksNeeded = attacksNeeded;
        this.baseDamagePercent = damagePercent;
        this.baseGoldenChance = goldenChance;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Nombre d'attaques nécessaires (réduit avec le niveau)
     */
    private int getAttacksNeeded(PetData petData) {
        return petData.getStatMultiplier() >= 1.5 ? baseAttacksNeeded - 1 : baseAttacksNeeded;
    }

    /**
     * % de dégâts (augmente avec le niveau)
     */
    private double getDamagePercent(PetData petData) {
        return petData.getStatMultiplier() >= 1.5 ? baseDamagePercent + 0.05 : baseDamagePercent;
    }

    /**
     * Chance d'œuf doré (uniquement avec star power)
     */
    private double getGoldenChance(PetData petData) {
        return petData.getStarPower() > 0 ? baseGoldenChance : 0;
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        int count = attackCounters.getOrDefault(uuid, 0) + 1;
        int needed = getAttacksNeeded(petData);

        if (count >= needed) {
            attackCounters.put(uuid, 0);
            launchExplosiveEgg(player, petData, target);
        } else {
            attackCounters.put(uuid, count);

            // Indicateur de charge (œuf qui se forme)
            if (count == needed - 1) {
                player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    player.getLocation().add(0, 2, 0), 5, 0.2, 0.2, 0.2, 0.01);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 0.8f);
            }
        }
        return damage;
    }

    /**
     * Lance un œuf explosif vers la cible
     */
    private void launchExplosiveEgg(Player player, PetData petData, LivingEntity target) {
        // Récupérer les dégâts du joueur
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        Map<com.rinaorc.zombiez.items.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double playerBaseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        // Déterminer si c'est un œuf doré
        boolean isGolden = random.nextDouble() < getGoldenChance(petData);

        // Calculer les dégâts
        double damageMultiplier = getDamagePercent(petData) * petData.getStatMultiplier();
        if (isGolden) damageMultiplier *= 2.0; // Œuf doré = x2 dégâts
        double eggDamage = playerBaseDamage * damageMultiplier;

        Location start = player.getLocation().add(0, 1.5, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Son de ponte
        player.playSound(start, Sound.ENTITY_CHICKEN_EGG, 1.0f, isGolden ? 1.5f : 1.2f);

        // Animation de l'œuf volant
        spawnEggProjectile(player, start, targetLoc, eggDamage, isGolden, petData);
    }

    /**
     * Crée un projectile d'œuf animé
     */
    private void spawnEggProjectile(Player player, Location from, Location to, double damage,
                                     boolean isGolden, PetData petData) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        // Couleur de l'œuf
        Particle.DustOptions dust = isGolden
            ? new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f)
            : new Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f);

        new BukkitRunnable() {
            double traveled = 0;
            final Location current = from.clone();

            @Override
            public void run() {
                if (traveled >= distance) {
                    // Explosion à l'arrivée
                    explodeEgg(player, current, damage, isGolden, petData);
                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.5));
                current.getWorld().spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, 0, dust);
                if (isGolden) {
                    current.getWorld().spawnParticle(Particle.WAX_ON, current, 2, 0.05, 0.05, 0.05, 0);
                }
                traveled += 1.5;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Explosion de l'œuf
     */
    private void explodeEgg(Player player, Location loc, double damage, boolean isGolden, PetData petData) {
        // Effets visuels
        if (isGolden) {
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 30, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);
        } else {
            loc.getWorld().spawnParticle(Particle.BLOCK, loc, 20, 0.5, 0.5, 0.5, 0.1,
                org.bukkit.Material.BONE_BLOCK.createBlockData());
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
            loc.getWorld().playSound(loc, Sound.ENTITY_TURTLE_EGG_BREAK, 1.5f, 1.0f);
        }

        // Dégâts de zone (rayon 3)
        int hits = 0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                petData.addDamage((long) damage);
                hits++;

                // Stun si œuf doré
                if (isGolden) {
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10)); // 1s stun
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
                }
            }
        }

        // Message
        String eggType = isGolden ? "§6§l★ ŒUF DORÉ" : "§f💣 Œuf";
        player.sendMessage("§a[Pet] " + eggType + " §7explosé! §c" +
            String.format("%.0f", damage) + " §7dégâts sur §e" + hits + " §7ennemis!");
    }

    public int getAttackCount(UUID uuid) {
        return attackCounters.getOrDefault(uuid, 0);
    }
}

@Getter
class AirstrikeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final EggBomberPassive eggBomberPassive;
    private final double baseDamagePercent;
    private final int eggCount;
    private static final Random random = new Random();

    public AirstrikeActive(String id, String name, String desc, EggBomberPassive passive,
                           double damagePercent, int eggs) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.eggBomberPassive = passive;
        this.baseDamagePercent = damagePercent;
        this.eggCount = eggs;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 20; }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Récupérer les dégâts du joueur
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        Map<com.rinaorc.zombiez.items.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double playerBaseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        // Trouver les ennemis cibles
        List<Monster> targets = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
            if (entity instanceof Monster monster) {
                targets.add(monster);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à portée!");
            return false;
        }

        // Son d'alerte
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 2.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 2.0f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 2.0f, 0.9f);
        player.sendMessage("§a[Pet] §c§l🐔 FRAPPE AÉRIENNE! §e" + eggCount + " §7œufs en approche!");

        // Calculer les dégâts par œuf
        double eggDamage = playerBaseDamage * baseDamagePercent * petData.getStatMultiplier();
        double goldenChance = petData.getStarPower() > 0 ? 0.15 : 0; // 15% avec star power

        // Lancer les œufs avec délai
        for (int i = 0; i < eggCount; i++) {
            int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Choisir une cible (cycle ou random)
                    Monster target = targets.get(index % targets.size());
                    if (!target.isValid() || target.isDead()) {
                        // Chercher une autre cible
                        for (Monster m : targets) {
                            if (m.isValid() && !m.isDead()) {
                                target = m;
                                break;
                            }
                        }
                    }

                    if (target.isValid() && !target.isDead()) {
                        boolean isGolden = random.nextDouble() < goldenChance;
                        double finalDamage = isGolden ? eggDamage * 2 : eggDamage;

                        // Position de départ (depuis le ciel)
                        Location targetLoc = target.getLocation();
                        Location skyPos = targetLoc.clone().add(
                            (random.nextDouble() - 0.5) * 3,
                            15 + random.nextDouble() * 5,
                            (random.nextDouble() - 0.5) * 3
                        );

                        // Lancer l'œuf
                        launchAirstrikeEgg(player, skyPos, targetLoc, finalDamage, isGolden, petData, index + 1);
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("ZombieZ"), i * 4L);
        }

        return true;
    }

    /**
     * Lance un œuf depuis le ciel
     */
    private void launchAirstrikeEgg(Player player, Location from, Location to, double damage,
                                     boolean isGolden, PetData petData, int eggNumber) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();

        // Couleur selon type
        Particle.DustOptions dust = isGolden
            ? new Particle.DustOptions(org.bukkit.Color.YELLOW, 2.0f)
            : new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f);

        new BukkitRunnable() {
            final Location current = from.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 40 || current.getY() <= to.getY()) {
                    // Impact!
                    explodeAirstrikeEgg(player, current, damage, isGolden, petData, eggNumber);
                    cancel();
                    return;
                }

                // Mouvement avec gravité
                current.add(direction.clone().multiply(1.2));

                // Particules
                current.getWorld().spawnParticle(Particle.DUST, current, 5, 0.15, 0.15, 0.15, 0, dust);
                if (isGolden) {
                    current.getWorld().spawnParticle(Particle.END_ROD, current, 2, 0.1, 0.1, 0.1, 0.02);
                }

                // Son de chute
                if (ticks % 5 == 0) {
                    current.getWorld().playSound(current, Sound.ENTITY_CHICKEN_EGG, 0.3f, 1.5f + (ticks * 0.02f));
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Explosion d'un œuf de frappe aérienne
     */
    private void explodeAirstrikeEgg(Player player, Location loc, double damage, boolean isGolden,
                                      PetData petData, int eggNumber) {
        // Effets visuels épiques
        if (isGolden) {
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 1, 1, 1, 0.2);
            loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.5f);
        } else {
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2, 0.3, 0.3, 0.3, 0);
            loc.getWorld().spawnParticle(Particle.BLOCK, loc, 30, 0.8, 0.8, 0.8, 0.1,
                org.bukkit.Material.BONE_BLOCK.createBlockData());
            loc.getWorld().playSound(loc, Sound.ENTITY_TURTLE_EGG_BREAK, 1.5f, 0.8f);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
        }

        // Dégâts de zone (rayon 3.5)
        int hits = 0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3.5, 3.5, 3.5)) {
            if (entity instanceof Monster monster) {
                monster.damage(damage, player);
                petData.addDamage((long) damage);
                hits++;

                // Knockback
                Vector knockback = monster.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize().multiply(0.5).setY(0.3);
                monster.setVelocity(knockback);

                // Stun si œuf doré
                if (isGolden) {
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10));
                }
            }
        }

        // Message pour les gros impacts
        if (hits > 0) {
            String eggIcon = isGolden ? "§6★" : "§f●";
            player.sendMessage("§a[Pet] " + eggIcon + " §7Œuf #" + eggNumber + " → §c" +
                String.format("%.0f", damage) + " §7sur §e" + hits + " §7cibles!");
        }
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

// ==================== TENTACULE DU VIDE (Proc / Invocation - Style Gur'thalak) ====================

@Getter
class VoidTentaclePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double procChance;              // 15% de chance
    private final double damagePercent;           // % des dégâts du joueur
    private final int tentacleDurationTicks;      // Durée des tentacules
    private final double tentacleRadius;          // Rayon d'attaque des tentacules
    private final int tentacleCount;              // Nombre de tentacules invoqués
    private final int maxActiveTentacles;         // Cap de tentacules actifs (passif uniquement)
    private final Map<UUID, Long> lastProcTime = new HashMap<>();
    private final Map<UUID, Integer> activeTentacleCount = new HashMap<>();

    public VoidTentaclePassive(String id, String name, String desc, double chance, double dmgPercent,
                                int duration, double radius, int count, int maxActive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = chance;
        this.damagePercent = dmgPercent;
        this.tentacleDurationTicks = duration;
        this.tentacleRadius = radius;
        this.tentacleCount = count;
        this.maxActiveTentacles = maxActive;
        PassiveAbilityCleanup.registerForCleanup(lastProcTime);
        PassiveAbilityCleanup.registerForCleanup(activeTentacleCount);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cap de tentacules actifs (passif uniquement)
        int currentActive = activeTentacleCount.getOrDefault(uuid, 0);
        if (currentActive >= maxActiveTentacles) {
            return damage; // Cap atteint, pas de nouveau proc
        }

        // Ajuster la chance par niveau
        double adjustedChance = procChance + (petData.getStatMultiplier() - 1) * 0.05;

        // Cooldown interne de 3s pour éviter le spam (augmenté de 2s à 3s)
        long now = System.currentTimeMillis();
        long lastProc = lastProcTime.getOrDefault(uuid, 0L);
        if (now - lastProc < 3000) {
            return damage;
        }

        // Test de proc
        if (Math.random() < adjustedChance) {
            lastProcTime.put(uuid, now);
            spawnVoidTentacles(player, petData, target.getLocation(), false);
        }

        return damage;
    }

    public void spawnVoidTentacles(Player player, PetData petData, Location center, boolean isUltimate) {
        World world = center.getWorld();
        UUID uuid = player.getUniqueId();

        // Ajuster les valeurs par niveau
        double adjustedRadius = tentacleRadius + (petData.getStatMultiplier() - 1) * 1.5;
        int adjustedDuration = (int) (tentacleDurationTicks + (petData.getStatMultiplier() - 1) * 20);
        double adjustedDmgPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.05;
        int adjustedCount = isUltimate ? 5 : tentacleCount + (int)((petData.getStatMultiplier() - 1) * 0.5);

        // Pour le passif, limiter au nombre restant disponible
        if (!isUltimate) {
            int currentActive = activeTentacleCount.getOrDefault(uuid, 0);
            int remainingSlots = maxActiveTentacles - currentActive;
            adjustedCount = Math.min(adjustedCount, remainingSlots);

            if (adjustedCount <= 0) {
                return; // Pas de slot disponible
            }
        }

        // Calculer les dégâts des tentacules
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double tentacleDamage = playerDamage * (isUltimate ? adjustedDmgPercent * 1.5 : adjustedDmgPercent);

        // Son d'invocation
        world.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.5f);
        world.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.5f);

        if (!isUltimate) {
            int currentActive = activeTentacleCount.getOrDefault(uuid, 0);
            player.sendMessage("§a[Pet] §5🦑 TENTACULES DU VIDE! §7+" + adjustedCount +
                " §8(" + (currentActive + adjustedCount) + "/" + maxActiveTentacles + " actifs)");
        }

        // Spawner les tentacules en cercle autour du point d'impact
        for (int i = 0; i < adjustedCount; i++) {
            double angle = (2 * Math.PI / adjustedCount) * i;
            double spawnRadius = isUltimate ? 3.0 : 1.5;
            Location tentacleLoc = center.clone().add(
                Math.cos(angle) * spawnRadius,
                0,
                Math.sin(angle) * spawnRadius
            );

            // Incrémenter le compteur pour le passif
            if (!isUltimate) {
                activeTentacleCount.merge(uuid, 1, Integer::sum);
            }

            // Délai entre chaque spawn de tentacule
            final int tentacleIndex = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> spawnSingleTentacle(player, petData, tentacleLoc, tentacleDamage, adjustedRadius,
                    adjustedDuration, isUltimate),
                i * 3L
            );
        }
    }

    private void decrementTentacleCount(UUID uuid) {
        activeTentacleCount.merge(uuid, -1, (old, dec) -> Math.max(0, old + dec));
    }

    private void spawnSingleTentacle(Player player, PetData petData, Location spawnLoc,
                                      double damage, double radius, int duration, boolean isUltimate) {
        World world = spawnLoc.getWorld();
        UUID uuid = player.getUniqueId();

        // Effet d'émergence
        world.spawnParticle(Particle.PORTAL, spawnLoc, 30, 0.3, 0.5, 0.3, 0.5);
        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 15, 0.2, 0.3, 0.2, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_SQUID_SQUIRT, 1.0f, 0.6f);

        // Animation du tentacule
        new BukkitRunnable() {
            int ticksAlive = 0;
            double tentacleHeight = 0;
            final double maxHeight = isUltimate ? 4.0 : 2.5;
            boolean emerging = true;
            int attackTick = 0;

            @Override
            public void run() {
                if (ticksAlive >= duration || !player.isOnline()) {
                    // Décrémenter le compteur pour le passif
                    if (!isUltimate) {
                        decrementTentacleCount(uuid);
                    }

                    // Disparition du tentacule
                    world.spawnParticle(Particle.PORTAL, spawnLoc.clone().add(0, tentacleHeight / 2, 0),
                        20, 0.3, tentacleHeight / 2, 0.3, 0.3);
                    world.playSound(spawnLoc, Sound.ENTITY_SQUID_HURT, 0.8f, 0.8f);
                    cancel();
                    return;
                }

                // Phase d'émergence (10 premiers ticks)
                if (emerging && ticksAlive < 10) {
                    tentacleHeight = maxHeight * (ticksAlive / 10.0);
                } else {
                    emerging = false;
                }

                // Dessiner le tentacule (particules verticales ondulantes)
                if (ticksAlive % 2 == 0) {
                    for (double h = 0; h < tentacleHeight; h += 0.4) {
                        double wave = Math.sin(ticksAlive * 0.3 + h * 2) * 0.3;
                        double wave2 = Math.cos(ticksAlive * 0.3 + h * 2) * 0.3;
                        Location particleLoc = spawnLoc.clone().add(wave, h, wave2);

                        // Couleur violette/sombre pour le vide
                        world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                        if (h > tentacleHeight - 0.8) {
                            // Bout du tentacule plus visible
                            world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }

                // Attaquer toutes les 15 ticks (0.75s)
                if (!emerging && ticksAlive >= attackTick + 15) {
                    attackTick = ticksAlive;

                    // Chercher des cibles dans le rayon
                    for (Entity entity : world.getNearbyEntities(spawnLoc, radius, radius, radius)) {
                        if (entity instanceof Monster monster) {
                            double distSq = entity.getLocation().distanceSquared(spawnLoc);
                            if (distSq <= radius * radius) {
                                // Attaque!
                                monster.damage(damage, player);
                                petData.addDamage((long) damage);

                                // Appliquer le slow
                                int slowLevel = isUltimate ? 2 : 1;
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 30, slowLevel, false, false));

                                // Effet visuel d'attaque
                                Location monsterLoc = monster.getLocation().add(0, 1, 0);
                                world.spawnParticle(Particle.SQUID_INK, monsterLoc, 8, 0.3, 0.3, 0.3, 0.05);
                                world.spawnParticle(Particle.CRIT_MAGIC, monsterLoc, 5, 0.2, 0.2, 0.2, 0.1);

                                // Ligne de particules du tentacule vers la cible
                                Vector dir = monsterLoc.toVector().subtract(spawnLoc.clone().add(0, tentacleHeight, 0).toVector());
                                double dist = dir.length();
                                dir.normalize();
                                for (double d = 0; d < dist; d += 0.5) {
                                    Location lineLoc = spawnLoc.clone().add(0, tentacleHeight, 0).add(dir.clone().multiply(d));
                                    world.spawnParticle(Particle.PORTAL, lineLoc, 1, 0, 0, 0, 0);
                                }
                            }
                        }
                    }

                    // Son d'attaque
                    world.playSound(spawnLoc, Sound.ENTITY_SQUID_SQUIRT, 0.6f, 1.2f);
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

@Getter
class VoidEruptionActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int tentacleCount;              // 5 tentacules
    private final double damagePercent;           // % des dégâts du joueur
    private final int tentacleDurationTicks;      // 6s = 120 ticks
    private final double tentacleRadius;          // Rayon d'attaque
    private final boolean drainLife;              // Upgrade: drain de vie

    public VoidEruptionActive(String id, String name, String desc, int cd, int count,
                               double dmgPercent, int duration, double radius, boolean drain) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.tentacleCount = count;
        this.damagePercent = dmgPercent;
        this.tentacleDurationTicks = duration;
        this.tentacleRadius = radius;
        this.drainLife = drain;
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
        boolean hasEnemies = player.getNearbyEntities(10, 10, 10).stream()
            .anyMatch(e -> e instanceof Monster);

        if (!hasEnemies) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à proximité!");
            return false;
        }

        // Ajuster les valeurs par niveau
        int adjustedCount = tentacleCount + (int)((petData.getStatMultiplier() - 1) * 2);
        double adjustedDmgPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.10;
        int adjustedDuration = (int) (tentacleDurationTicks + (petData.getStatMultiplier() - 1) * 40);
        double adjustedRadius = tentacleRadius + (petData.getStatMultiplier() - 1) * 1.5;

        // Calculer les dégâts
        double playerDamage = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue();
        double tentacleDamage = playerDamage * adjustedDmgPercent;

        player.sendMessage("§a[Pet] §5§l🦑 ÉRUPTION DU VIDE! §7" + adjustedCount +
            " tentacules géants pendant " + (adjustedDuration / 20) + "s!");

        // Effet d'éruption centrale
        world.spawnParticle(Particle.EXPLOSION, playerLoc, 3, 1, 0.5, 1, 0);
        world.spawnParticle(Particle.PORTAL, playerLoc, 100, 2, 1, 2, 1);
        world.spawnParticle(Particle.SQUID_INK, playerLoc, 50, 1.5, 0.5, 1.5, 0.2);

        // Sons épiques
        world.playSound(playerLoc, Sound.ENTITY_WARDEN_ROAR, 0.6f, 1.5f);
        world.playSound(playerLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.3f);
        world.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.5f);

        // Spawner les tentacules en cercle élargi
        for (int i = 0; i < adjustedCount; i++) {
            double angle = (2 * Math.PI / adjustedCount) * i;
            double spawnRadius = 4.0 + Math.random() * 2;
            Location tentacleLoc = playerLoc.clone().add(
                Math.cos(angle) * spawnRadius,
                0,
                Math.sin(angle) * spawnRadius
            );

            final int tentacleIndex = i;
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> spawnGiantTentacle(player, petData, tentacleLoc, tentacleDamage,
                    adjustedRadius, adjustedDuration, drainLife),
                i * 5L
            );
        }

        return true;
    }

    private void spawnGiantTentacle(Player player, PetData petData, Location spawnLoc,
                                     double damage, double radius, int duration, boolean drain) {
        World world = spawnLoc.getWorld();

        // Effet d'émergence épique
        world.spawnParticle(Particle.PORTAL, spawnLoc, 50, 0.5, 1, 0.5, 1);
        world.spawnParticle(Particle.SQUID_INK, spawnLoc, 30, 0.3, 0.5, 0.3, 0.2);
        world.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 20, 0.5, 0.1, 0.5, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.2f);

        // Animation du tentacule géant
        new BukkitRunnable() {
            int ticksAlive = 0;
            double tentacleHeight = 0;
            final double maxHeight = 5.0;
            boolean emerging = true;
            int attackTick = 0;

            @Override
            public void run() {
                if (ticksAlive >= duration || !player.isOnline()) {
                    // Disparition épique
                    for (double h = 0; h < tentacleHeight; h += 0.5) {
                        Location particleLoc = spawnLoc.clone().add(0, h, 0);
                        world.spawnParticle(Particle.PORTAL, particleLoc, 5, 0.3, 0.3, 0.3, 0.5);
                    }
                    world.playSound(spawnLoc, Sound.ENTITY_SQUID_DEATH, 1.0f, 0.6f);
                    cancel();
                    return;
                }

                // Phase d'émergence (15 premiers ticks)
                if (emerging && ticksAlive < 15) {
                    tentacleHeight = maxHeight * (ticksAlive / 15.0);
                } else {
                    emerging = false;
                }

                // Dessiner le tentacule géant (plus épais et menaçant)
                if (ticksAlive % 2 == 0) {
                    for (double h = 0; h < tentacleHeight; h += 0.3) {
                        double wave = Math.sin(ticksAlive * 0.25 + h * 1.5) * 0.5;
                        double wave2 = Math.cos(ticksAlive * 0.25 + h * 1.5) * 0.5;

                        // Tentacule principal
                        Location particleLoc = spawnLoc.clone().add(wave, h, wave2);
                        world.spawnParticle(Particle.PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                        world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.1, 0.1, 0);

                        // Bout du tentacule (tête)
                        if (h > tentacleHeight - 1.0) {
                            world.spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.2, 0.2, 0.2, 0.02);
                            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0.15, 0.15, 0.15, 0);
                        }
                    }

                    // Cercle au sol (zone de danger)
                    for (int angle = 0; angle < 360; angle += 20) {
                        double rad = Math.toRadians(angle + ticksAlive * 2);
                        Location ringLoc = spawnLoc.clone().add(
                            Math.cos(rad) * radius * 0.8,
                            0.1,
                            Math.sin(rad) * radius * 0.8
                        );
                        world.spawnParticle(Particle.PORTAL, ringLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Attaquer toutes les 12 ticks (0.6s) - plus rapide que le passif
                if (!emerging && ticksAlive >= attackTick + 12) {
                    attackTick = ticksAlive;

                    double totalDamageDealt = 0;

                    // Chercher des cibles dans le rayon
                    for (Entity entity : world.getNearbyEntities(spawnLoc, radius, radius, radius)) {
                        if (entity instanceof Monster monster) {
                            double distSq = entity.getLocation().distanceSquared(spawnLoc);
                            if (distSq <= radius * radius) {
                                // Attaque!
                                monster.damage(damage, player);
                                petData.addDamage((long) damage);
                                totalDamageDealt += damage;

                                // Slow plus puissant
                                monster.addPotionEffect(new PotionEffect(
                                    PotionEffectType.SLOWNESS, 40, 2, false, false));

                                // Effet visuel d'attaque
                                Location monsterLoc = monster.getLocation().add(0, 1, 0);
                                world.spawnParticle(Particle.SQUID_INK, monsterLoc, 12, 0.4, 0.4, 0.4, 0.08);
                                world.spawnParticle(Particle.CRIT_MAGIC, monsterLoc, 8, 0.3, 0.3, 0.3, 0.15);
                                world.spawnParticle(Particle.DRAGON_BREATH, monsterLoc, 3, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }

                    // Drain de vie (upgrade)
                    if (drain && totalDamageDealt > 0) {
                        double healAmount = totalDamageDealt * 0.15; // 15% des dégâts en heal
                        double newHealth = Math.min(player.getHealth() + healAmount,
                            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                        player.setHealth(newHealth);

                        // Effet visuel de drain
                        world.spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0),
                            3, 0.3, 0.3, 0.3, 0);
                    }

                    // Son d'attaque
                    world.playSound(spawnLoc, Sound.ENTITY_SQUID_SQUIRT, 0.8f, 0.8f);
                    world.playSound(spawnLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.3f, 1.5f);
                }

                // Son ambiant toutes les 30 ticks
                if (ticksAlive % 30 == 0) {
                    world.playSound(spawnLoc, Sound.ENTITY_SQUID_AMBIENT, 0.4f, 0.5f);
                }

                ticksAlive++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}

// ==================== VOID SCREAMER SYSTEM (Hurleur du Vide) ====================

@Getter
class VoidScreamPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int baseAttacksNeeded;       // Nombre d'attaques pour déclencher (4)
    private final double baseDotPercent;       // % dégâts joueur par seconde (0.08 = 8%)
    private final int dotDurationTicks;        // Durée du DoT en ticks (60 = 3s)
    private final double screamRadius;         // Rayon du cri (6 blocs)
    private final Map<UUID, Integer> attackCounters = new HashMap<>();
    private static final Random random = new Random();

    public VoidScreamPassive(String id, String name, String desc, int attacksNeeded,
                              double dotPercent, int dotDuration, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseAttacksNeeded = attacksNeeded;
        this.baseDotPercent = dotPercent;
        this.dotDurationTicks = dotDuration;
        this.screamRadius = radius;
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Calcule le nombre d'attaques nécessaires selon le niveau
     * Base: 4 attaques, Niveau 5+: 3 attaques
     */
    private int getEffectiveAttacksNeeded(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return baseAttacksNeeded - 1; // 3 attaques
        }
        return baseAttacksNeeded; // 4 attaques
    }

    /**
     * Calcule le % de dégâts par tick selon le niveau
     * Base: 8%/s, Niveau 5+: 10%/s
     */
    private double getEffectiveDotPercent(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.10; // 10%/s
        }
        return baseDotPercent; // 8%/s
    }

    /**
     * Vérifie si on applique Blindness (étoiles max uniquement)
     */
    private boolean shouldApplyBlindness(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        UUID playerId = player.getUniqueId();
        int count = attackCounters.getOrDefault(playerId, 0) + 1;
        int needed = getEffectiveAttacksNeeded(petData);

        if (count >= needed) {
            attackCounters.put(playerId, 0);
            triggerVoidScream(player, petData);
        } else {
            attackCounters.put(playerId, count);
        }
    }

    /**
     * Déclenche le cri du vide - ralentit et applique un DoT aux ennemis
     */
    private void triggerVoidScream(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = player.getWorld();

        // Récupérer les stats du joueur pour calculer les dégâts
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        double dotPercentPerSecond = getEffectiveDotPercent(petData);
        double damagePerTick = baseDamage * dotPercentPerSecond / 20.0; // Dégâts par tick
        boolean applyBlindness = shouldApplyBlindness(petData);

        // Effet visuel du cri
        world.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0.5f);
        world.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.7f);

        // Particules d'onde de choc violette
        for (double angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            for (double r = 1; r <= screamRadius; r += 1) {
                double x = center.getX() + r * Math.cos(rad);
                double z = center.getZ() + r * Math.sin(rad);
                Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                world.spawnParticle(Particle.PORTAL, particleLoc, 2, 0.1, 0.3, 0.1, 0);
                world.spawnParticle(Particle.WITCH, particleLoc, 1, 0.1, 0.2, 0.1, 0);
            }
        }

        // Collecter les ennemis affectés
        int affected = 0;
        for (Entity entity : world.getNearbyEntities(center, screamRadius, screamRadius, screamRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && entity.getLocation().distanceSquared(center) <= screamRadius * screamRadius) {

                affected++;

                // Slowness II pendant 3 secondes
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dotDurationTicks, 1));

                // Blindness si étoiles max
                if (applyBlindness) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0)); // 1s
                }

                // Appliquer le DoT
                applyVoidDoT(player, living, damagePerTick, dotDurationTicks, plugin);

                // Particules sur chaque cible
                world.spawnParticle(Particle.REVERSE_PORTAL, living.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.05);
            }
        }

        // Feedback
        if (affected > 0) {
            String blindMsg = applyBlindness ? " §8+ Blindness" : "";
            player.sendMessage("§5[Pet] §d⚡ §7Cri du Vide → §e" + affected +
                " §7ennemis (Slow II + " + String.format("%.0f", dotPercentPerSecond * 100) + "%/s DoT)" + blindMsg);
        }
    }

    /**
     * Applique un Damage over Time aux ennemis
     */
    private void applyVoidDoT(Player player, LivingEntity target, double damagePerTick,
                               int totalTicks, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        new BukkitRunnable() {
            int ticksRemaining = totalTicks;

            @Override
            public void run() {
                if (ticksRemaining <= 0 || target.isDead() || !target.isValid()) {
                    cancel();
                    return;
                }

                // Dégâts toutes les 10 ticks (0.5s) pour éviter le spam
                if (ticksRemaining % 10 == 0) {
                    double dotDamage = damagePerTick * 10; // Dégâts accumulés sur 10 ticks

                    // Vérifier si c'est un zombie ZombieZ
                    var zombieManager = plugin.getZombieManager();
                    if (zombieManager != null) {
                        var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
                        if (activeZombie != null) {
                            zombieManager.damageZombie(player, activeZombie, dotDamage,
                                com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                        } else {
                            target.damage(dotDamage, player);
                        }
                    } else {
                        target.damage(dotDamage, player);
                    }

                    // Petites particules de dégâts
                    target.getWorld().spawnParticle(Particle.WITCH,
                        target.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0);
                }

                ticksRemaining--;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void activate(Player player, PetData petData) { }
}

/**
 * Frappe Fantôme - L'Enderman se téléporte en chaîne sur plusieurs ennemis
 * Chaque TP inflige des dégâts et laisse une écho d'ombre explosive
 */
@Getter
class PhantomStrikeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final VoidScreamPassive linkedPassive;
    private final double damagePercent;        // % dégâts joueur par frappe (0.40 = 40%)
    private final double echoDamagePercent;    // % dégâts de l'écho (0.20 = 20%)
    private final int baseChainCount;          // Nombre de TP de base (5)
    private final double chainRadius;          // Rayon pour trouver la prochaine cible (8 blocs)

    public PhantomStrikeActive(String id, String name, String desc, VoidScreamPassive passive,
                                double dmgPercent, double echoPercent, int chains, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damagePercent = dmgPercent;
        this.echoDamagePercent = echoPercent;
        this.baseChainCount = chains;
        this.chainRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    /**
     * Calcule le nombre de TP selon le niveau
     * Base: 5, Niveau 5+: 7
     */
    private int getEffectiveChainCount(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return baseChainCount + 2; // 7 TP
        }
        return baseChainCount; // 5 TP
    }

    /**
     * Vérifie si on crée un vortex attractif sur l'écho final (étoiles max)
     */
    private boolean shouldCreateFinalVortex(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        // Calculer les dégâts du joueur
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent) * petData.getStatMultiplier();

        double strikeDamage = baseDamage * this.damagePercent;
        double echoDamage = baseDamage * echoDamagePercent;
        int maxChains = getEffectiveChainCount(petData);
        boolean finalVortex = shouldCreateFinalVortex(petData);

        // Trouver la première cible
        Location start = player.getLocation();
        World world = player.getWorld();
        LivingEntity firstTarget = findNearestEnemy(start, world, null, chainRadius * 2);

        if (firstTarget == null) {
            player.sendMessage("§5[Pet] §7Aucun ennemi à portée pour la Frappe Fantôme!");
            return;
        }

        // Lancer la chaîne de téléportation
        player.sendMessage("§5[Pet] §d⚡ §5Frappe Fantôme activée! §7(" + maxChains + " cibles)");
        world.playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);

        executePhantomChain(player, firstTarget, strikeDamage, echoDamage, maxChains, finalVortex, plugin);
    }

    /**
     * Exécute la chaîne de téléportation fantôme
     */
    private void executePhantomChain(Player player, LivingEntity firstTarget, double strikeDamage,
                                      double echoDamage, int maxChains, boolean finalVortex,
                                      com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = player.getWorld();
        Set<UUID> hitTargets = new HashSet<>();
        List<Location> echoLocations = new ArrayList<>();

        new BukkitRunnable() {
            int chainsRemaining = maxChains;
            LivingEntity currentTarget = firstTarget;
            Location phantomLoc = player.getLocation().clone();

            @Override
            public void run() {
                if (chainsRemaining <= 0 || currentTarget == null || currentTarget.isDead()) {
                    // Fin de la chaîne - explosions des échos
                    triggerEchoExplosions(player, echoLocations, echoDamage, finalVortex, plugin);
                    cancel();
                    return;
                }

                hitTargets.add(currentTarget.getUniqueId());
                Location targetLoc = currentTarget.getLocation();

                // Téléporter le "fantôme" sur la cible
                phantomLoc = targetLoc.clone();

                // Effet visuel de téléportation
                world.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                world.spawnParticle(Particle.PORTAL, targetLoc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                world.spawnParticle(Particle.REVERSE_PORTAL, targetLoc, 20, 0.3, 0.8, 0.3, 0.05);

                // Infliger les dégâts
                dealPhantomDamage(player, currentTarget, strikeDamage, plugin);

                // Laisser une écho d'ombre
                echoLocations.add(targetLoc.clone());
                spawnEchoMarker(targetLoc, world);

                chainsRemaining--;

                // Chercher la prochaine cible
                currentTarget = findNearestEnemy(phantomLoc, world, hitTargets, chainRadius);
            }
        }.runTaskTimer(plugin, 0L, 6L); // 6 ticks = 0.3s entre chaque TP
    }

    /**
     * Cherche l'ennemi le plus proche non encore touché
     */
    private LivingEntity findNearestEnemy(Location center, World world, Set<UUID> exclude, double radius) {
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (exclude != null && exclude.contains(entity.getUniqueId())) continue;

                double dist = entity.getLocation().distanceSquared(center);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = living;
                }
            }
        }
        return nearest;
    }

    /**
     * Inflige les dégâts de frappe fantôme
     */
    private void dealPhantomDamage(Player player, LivingEntity target, double damage,
                                    com.rinaorc.zombiez.ZombieZPlugin plugin) {
        var zombieManager = plugin.getZombieManager();
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    /**
     * Spawn un marqueur visuel d'écho d'ombre
     */
    private void spawnEchoMarker(Location loc, World world) {
        // Particules sombres indiquant où l'écho va exploser
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) { // 0.5s d'attente avant explosion
                    cancel();
                    return;
                }
                world.spawnParticle(Particle.WITCH, loc, 5, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.2, 0.2, 0.01);
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Déclenche les explosions de tous les échos d'ombre
     */
    private void triggerEchoExplosions(Player player, List<Location> echoLocations, double echoDamage,
                                        boolean finalVortex, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        if (echoLocations.isEmpty()) return;

        World world = player.getWorld();
        double echoRadius = 3.0;
        int totalHits = 0;
        double totalDamage = 0;

        for (int i = 0; i < echoLocations.size(); i++) {
            Location echoLoc = echoLocations.get(i);
            boolean isLast = (i == echoLocations.size() - 1);

            // Effet d'explosion
            world.playSound(echoLoc, Sound.ENTITY_ENDERMAN_HURT, 0.8f, 0.6f);
            world.playSound(echoLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
            world.spawnParticle(Particle.EXPLOSION, echoLoc, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.PORTAL, echoLoc, 50, 1, 1, 1, 0.5);
            world.spawnParticle(Particle.REVERSE_PORTAL, echoLoc, 30, 0.5, 0.5, 0.5, 0.2);

            // Dégâts AoE
            for (Entity entity : world.getNearbyEntities(echoLoc, echoRadius, echoRadius, echoRadius)) {
                if (entity instanceof LivingEntity living && !(entity instanceof Player)
                    && !(entity instanceof ArmorStand) && !living.isDead()) {

                    dealPhantomDamage(player, living, echoDamage, plugin);
                    totalHits++;
                    totalDamage += echoDamage;
                }
            }

            // Vortex attractif sur le dernier écho si étoiles max
            if (isLast && finalVortex) {
                createFinalVortex(echoLoc, world, plugin);
            }
        }

        // Message récapitulatif
        String vortexMsg = finalVortex ? " §8+ Vortex!" : "";
        player.sendMessage("§5[Pet] §d💥 §7Échos d'ombre → §c" +
            String.format("%.0f", totalDamage) + " §7total sur §e" + totalHits + " §7impacts" + vortexMsg);
    }

    /**
     * Crée un vortex attractif sur l'écho final (bonus étoiles max)
     */
    private void createFinalVortex(Location center, World world, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        double vortexRadius = 5.0;
        int vortexDuration = 40; // 2 secondes

        world.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.5f, 0.3f);
        world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= vortexDuration) {
                    // Explosion finale
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
                    cancel();
                    return;
                }

                // Effet visuel du vortex
                for (double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle + ticks * 18);
                    double r = vortexRadius * (1.0 - (double) ticks / vortexDuration);
                    double x = center.getX() + r * Math.cos(rad);
                    double z = center.getZ() + r * Math.sin(rad);
                    Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.2, 0.1, 0);
                }

                // Attirer les ennemis vers le centre
                for (Entity entity : world.getNearbyEntities(center, vortexRadius, vortexRadius, vortexRadius)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand)) {

                        Vector direction = center.toVector().subtract(entity.getLocation().toVector());
                        if (direction.lengthSquared() > 0.5) {
                            direction.normalize().multiply(0.3);
                            entity.setVelocity(entity.getVelocity().add(direction));
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== PIGLIN BERSERKER SYSTEM ====================

/**
 * Saut Dévastateur - Le Piglin saute sur les ennemis et inflige des dégâts AoE
 * 1% chance par attaque, 180% des dégâts du joueur, ralentit de 60%
 */
@Getter
class WarLeapPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseChance;           // 1% = 0.01
    private final double baseDamagePercent;    // 180% = 1.80
    private final double impactRadius;         // 8 blocs
    private final int slowDurationTicks;       // 3s = 60 ticks
    private final double slowStrength;         // 60% = niveau 2-3 de slowness
    private static final Random random = new Random();

    public WarLeapPassive(String id, String name, String desc, double chance,
                           double damagePercent, double radius, int slowDuration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseChance = chance;
        this.baseDamagePercent = damagePercent;
        this.impactRadius = radius;
        this.slowDurationTicks = slowDuration;
        this.slowStrength = 0.60; // 60% slow
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Calcule la chance de déclencher selon le niveau
     * Base: 1%, Niveau 5+: 2%
     */
    private double getEffectiveChance(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return baseChance * 2; // 2%
        }
        return baseChance; // 1%
    }

    /**
     * Calcule le % de dégâts selon le niveau
     * Base: 180%, Niveau 5+: 200%
     */
    private double getEffectiveDamagePercent(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 2.00; // 200%
        }
        return baseDamagePercent; // 180%
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        double chance = getEffectiveChance(petData);

        if (random.nextDouble() < chance) {
            triggerWarLeap(player, target.getLocation(), petData);
        }
    }

    /**
     * Déclenche le saut de guerre du Piglin
     */
    private void triggerWarLeap(Player player, Location targetLoc, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Calculer les dégâts du joueur
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        double leapDamage = baseDamage * getEffectiveDamagePercent(petData) * petData.getStatMultiplier();

        // Son de saut
        world.playSound(playerLoc, Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_RAVAGER_ATTACK, 0.8f, 1.2f);

        // Animation de saut (particules arc)
        animateLeapTrail(playerLoc, targetLoc, world);

        // Impact après un court délai
        new BukkitRunnable() {
            @Override
            public void run() {
                executeLeapImpact(player, targetLoc, leapDamage, plugin);
            }
        }.runTaskLater(plugin, 8L); // 0.4s de délai pour l'animation
    }

    /**
     * Anime la trajectoire du saut
     */
    private void animateLeapTrail(Location start, Location end, World world) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 8;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;
                double x = start.getX() + (end.getX() - start.getX()) * progress;
                double z = start.getZ() + (end.getZ() - start.getZ()) * progress;
                // Arc parabolique
                double height = Math.sin(progress * Math.PI) * 4;
                double y = start.getY() + (end.getY() - start.getY()) * progress + height;

                Location trailLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.FLAME, trailLoc, 5, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticle(Particle.SMOKE, trailLoc, 3, 0.1, 0.1, 0.1, 0.01);

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Exécute l'impact du saut
     */
    private void executeLeapImpact(Player player, Location impactLoc, double damage,
                                    com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = impactLoc.getWorld();
        if (world == null) return;

        // Effet d'impact
        world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        world.playSound(impactLoc, Sound.ENTITY_PIGLIN_BRUTE_HURT, 1.0f, 0.6f);
        world.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);

        // Particules d'impact au sol
        world.spawnParticle(Particle.EXPLOSION, impactLoc, 3, 0.5, 0.2, 0.5, 0);
        world.spawnParticle(Particle.FLAME, impactLoc, 40, 2, 0.5, 2, 0.1);
        world.spawnParticle(Particle.SMOKE, impactLoc, 30, 1.5, 0.3, 1.5, 0.05);

        // Onde de choc circulaire
        for (double angle = 0; angle < 360; angle += 20) {
            double rad = Math.toRadians(angle);
            for (double r = 1; r <= impactRadius; r += 1.5) {
                double x = impactLoc.getX() + r * Math.cos(rad);
                double z = impactLoc.getZ() + r * Math.sin(rad);
                Location particleLoc = new Location(world, x, impactLoc.getY() + 0.1, z);
                world.spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0.1);
            }
        }

        // Dégâts et slow aux ennemis
        int hits = 0;
        var zombieManager = plugin.getZombieManager();

        for (Entity entity : world.getNearbyEntities(impactLoc, impactRadius, impactRadius, impactRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(impactLoc);
                if (distSq > impactRadius * impactRadius) continue;

                hits++;

                // Dégâts
                if (zombieManager != null) {
                    var activeZombie = zombieManager.getActiveZombie(entity.getUniqueId());
                    if (activeZombie != null) {
                        zombieManager.damageZombie(player, activeZombie, damage,
                            com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                    } else {
                        living.damage(damage, player);
                    }
                } else {
                    living.damage(damage, player);
                }

                // Slowness (60% = environ niveau 3)
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDurationTicks, 2));

                // Petit knockback vers l'extérieur
                Vector knockback = living.getLocation().toVector().subtract(impactLoc.toVector());
                if (knockback.lengthSquared() > 0) {
                    knockback.normalize().multiply(0.5).setY(0.3);
                    living.setVelocity(living.getVelocity().add(knockback));
                }
            }
        }

        // Message
        if (hits > 0) {
            player.sendMessage("§6[Pet] §c⚔ §7Saut Dévastateur! §c" +
                String.format("%.0f", damage) + " §7dégâts sur §e" + hits + " §7ennemis!");
        }
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void activate(Player player, PetData petData) { }
}

/**
 * Cri Féroce - Réduit les dégâts des ennemis proches pendant une durée
 * 20% réduction de dégâts dans 25 blocs pendant 15s
 */
@Getter
class FerociousCryActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final WarLeapPassive linkedPassive;
    private final double damageReduction;      // 20% = 0.20
    private final double cryRadius;            // 25 blocs
    private final int durationTicks;           // 15s = 300 ticks

    // Tracking des ennemis affectés pour le système de réduction
    private static final Map<UUID, Long> affectedEnemies = new HashMap<>();
    private static final Map<UUID, Double> reductionAmount = new HashMap<>();

    public FerociousCryActive(String id, String name, String desc, WarLeapPassive passive,
                               double reduction, double radius, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damageReduction = reduction;
        this.cryRadius = radius;
        this.durationTicks = duration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    /**
     * Vérifie si on applique Weakness en plus (étoiles max)
     */
    private boolean shouldApplyWeakness(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        Location center = player.getLocation();
        World world = player.getWorld();
        boolean applyWeakness = shouldApplyWeakness(petData);

        // Cri de guerre
        world.playSound(center, Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);

        // Onde de choc visuelle
        spawnCryShockwave(center, world);

        // Affecter les ennemis
        int affected = 0;
        long expirationTime = System.currentTimeMillis() + (durationTicks * 50L);

        for (Entity entity : world.getNearbyEntities(center, cryRadius, cryRadius, cryRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(center);
                if (distSq > cryRadius * cryRadius) continue;

                affected++;
                UUID entityId = entity.getUniqueId();

                // Enregistrer la réduction de dégâts (sera utilisée par le système de combat)
                affectedEnemies.put(entityId, expirationTime);
                reductionAmount.put(entityId, damageReduction);

                // Appliquer Weakness si étoiles max (cumulatif avec la réduction)
                if (applyWeakness) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, durationTicks, 0));
                }

                // Effet visuel sur chaque ennemi
                world.spawnParticle(Particle.ANGRY_VILLAGER, living.getLocation().add(0, 1.5, 0),
                    5, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.SMOKE, living.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.02);
            }
        }

        // Effet de réduction de dégâts via un marker visuel
        spawnIntimidationAura(player, plugin, durationTicks);

        // Message
        String weaknessMsg = applyWeakness ? " §8+ Weakness" : "";
        player.sendMessage("§6[Pet] §c🔊 §7Cri Féroce! §e" + affected +
            " §7ennemis intimidés (-" + (int)(damageReduction * 100) + "% dégâts, " +
            (durationTicks / 20) + "s)" + weaknessMsg);
    }

    /**
     * Vérifie si un ennemi est affecté par le cri et retourne la réduction
     * Méthode statique pour être appelée par le système de combat
     */
    public static double getDamageReduction(UUID entityId) {
        Long expiration = affectedEnemies.get(entityId);
        if (expiration != null && System.currentTimeMillis() < expiration) {
            return reductionAmount.getOrDefault(entityId, 0.0);
        }
        // Nettoyer si expiré
        affectedEnemies.remove(entityId);
        reductionAmount.remove(entityId);
        return 0.0;
    }

    /**
     * Crée l'onde de choc du cri
     */
    private void spawnCryShockwave(Location center, World world) {
        new BukkitRunnable() {
            double radius = 0;

            @Override
            public void run() {
                if (radius >= cryRadius) {
                    cancel();
                    return;
                }

                // Cercle de particules qui s'expand
                for (double angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                    world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
                }

                radius += 3;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 2L);
    }

    /**
     * Affiche une aura d'intimidation autour du joueur
     */
    private void spawnIntimidationAura(Player player, com.rinaorc.zombiez.ZombieZPlugin plugin, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Aura toutes les 20 ticks (1s)
                if (ticks % 20 == 0) {
                    Location loc = player.getLocation();
                    player.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 0.5, 0),
                        8, 0.5, 0.3, 0.5, 0.01);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== FROST SPECTER SYSTEM (Spectre du Givre) ====================

/**
 * Agilité Spectrale - Augmente constamment la vitesse de déplacement du joueur
 * +33% de vitesse de base, +40% au niveau 5+
 */
@Getter
class SpectralSwiftPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseSpeedBonus;       // 33% = 0.33

    public SpectralSwiftPassive(String id, String name, String desc, double speedBonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseSpeedBonus = speedBonus;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Calcule le bonus de vitesse selon le niveau
     * Base: 33%, Niveau 5+: 40%
     */
    private double getEffectiveSpeedBonus(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.40; // 40%
        }
        return baseSpeedBonus; // 33%
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        double speedBonus = getEffectiveSpeedBonus(petData);

        // Vitesse de base: 0.2, max safe: ~0.4
        // +33% = 0.2 * 1.33 = 0.266
        // +40% = 0.2 * 1.40 = 0.28
        float newSpeed = (float) (0.2 * (1.0 + speedBonus));

        // Appliquer la vitesse si différente
        if (Math.abs(player.getWalkSpeed() - newSpeed) > 0.01f) {
            player.setWalkSpeed(newSpeed);
        }

        // Effet visuel subtil (particules de givre occasionnelles)
        if (Math.random() < 0.1) { // 10% chance par tick
            player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                player.getLocation().add(0, 0.5, 0), 2, 0.3, 0.2, 0.3, 0.01);
        }
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void activate(Player player, PetData petData) { }
}

/**
 * Lame Fantôme - Lance un couteau tournoyant qui empale l'ennemi
 * 750% des dégâts de l'arme, effet visuel d'ArmorStand avec épée
 */
@Getter
class PhantomBladeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final SpectralSwiftPassive linkedPassive;
    private final double baseDamagePercent;    // 750% = 7.50
    private final double projectileSpeed;      // Blocs par tick

    public PhantomBladeActive(String id, String name, String desc, SpectralSwiftPassive passive,
                               double damagePercent, double speed) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.baseDamagePercent = damagePercent;
        this.projectileSpeed = speed;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 30; }

    /**
     * Vérifie si le couteau traverse les ennemis (étoiles max)
     */
    private boolean shouldPierce(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        // Calculer les dégâts du joueur
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        double bladeDamage = baseDamage * baseDamagePercent * petData.getStatMultiplier();
        boolean piercing = shouldPierce(petData);

        // Direction du regard du joueur
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();

        // Son de lancement
        player.getWorld().playSound(startLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.2f);
        player.getWorld().playSound(startLoc, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.8f);

        // Lancer le couteau
        launchPhantomBlade(player, startLoc, direction, bladeDamage, piercing, plugin);

        String pierceMsg = piercing ? " §8(Perçant)" : "";
        player.sendMessage("§b[Pet] §f🗡 §7Lame Fantôme lancée! §c" +
            String.format("%.0f", bladeDamage) + " §7dégâts" + pierceMsg);
    }

    /**
     * Lance le couteau fantôme avec effet d'ArmorStand
     */
    private void launchPhantomBlade(Player player, Location start, Vector direction,
                                     double damage, boolean piercing,
                                     com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = start.getWorld();
        if (world == null) return;

        // Créer l'ArmorStand avec l'épée
        ArmorStand blade = world.spawn(start, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(true);
            as.setInvulnerable(true);
            as.setPersistent(false);

            // Épée dans la main
            as.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));

            // Orientation initiale
            as.setRotation(start.getYaw(), start.getPitch());
        });

        Set<UUID> hitEntities = new HashSet<>();

        // Animation du couteau
        new BukkitRunnable() {
            double traveled = 0;
            final double maxDistance = 30;
            int rotationAngle = 0;
            Location currentLoc = start.clone();

            @Override
            public void run() {
                // Vérifier si le couteau doit s'arrêter
                if (traveled >= maxDistance || !blade.isValid()) {
                    blade.remove();
                    cancel();
                    return;
                }

                // Déplacer le couteau
                currentLoc.add(direction.clone().multiply(projectileSpeed));
                traveled += projectileSpeed;

                // Vérifier collision avec bloc solide
                if (currentLoc.getBlock().getType().isSolid()) {
                    spawnImpactEffect(currentLoc, world);
                    blade.remove();
                    cancel();
                    return;
                }

                // Téléporter l'ArmorStand
                blade.teleport(currentLoc);

                // Rotation de l'épée (effet tournoyant)
                rotationAngle += 45;
                org.bukkit.util.EulerAngle armPose = new org.bukkit.util.EulerAngle(
                    Math.toRadians(rotationAngle),
                    Math.toRadians(rotationAngle / 2.0),
                    Math.toRadians(rotationAngle)
                );
                blade.setRightArmPose(armPose);

                // Particules de traînée
                world.spawnParticle(Particle.SNOWFLAKE, currentLoc, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.CRIT, currentLoc, 2, 0.05, 0.05, 0.05, 0.1);

                // Son de sifflement
                if (traveled % 3 < projectileSpeed) {
                    world.playSound(currentLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.5f);
                }

                // Détecter les collisions avec les ennemis
                boolean hitSomething = checkEnemyCollisions(player, currentLoc, damage, hitEntities, plugin);

                // Si touché et non-perçant, arrêter
                if (hitSomething && !piercing) {
                    spawnImpactEffect(currentLoc, world);
                    blade.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Vérifie les collisions avec les ennemis
     */
    private boolean checkEnemyCollisions(Player player, Location loc, double damage,
                                          Set<UUID> hitEntities,
                                          com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = loc.getWorld();
        if (world == null) return false;

        boolean hit = false;
        double hitRadius = 1.2;

        for (Entity entity : world.getNearbyEntities(loc, hitRadius, hitRadius, hitRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                UUID entityId = entity.getUniqueId();
                if (hitEntities.contains(entityId)) continue;

                hitEntities.add(entityId);
                hit = true;

                // Infliger les dégâts
                var zombieManager = plugin.getZombieManager();
                if (zombieManager != null) {
                    var activeZombie = zombieManager.getActiveZombie(entityId);
                    if (activeZombie != null) {
                        zombieManager.damageZombie(player, activeZombie, damage,
                            com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                    } else {
                        living.damage(damage, player);
                    }
                } else {
                    living.damage(damage, player);
                }

                // Effet d'empalement
                world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                world.playSound(loc, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
                world.spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0),
                    20, 0.3, 0.5, 0.3, 0.2);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, living.getLocation().add(0, 1.5, 0),
                    5, 0.2, 0.2, 0.2, 0.1);

                // Message de hit
                player.sendMessage("§b[Pet] §f🎯 §7Lame Fantôme → §c" +
                    String.format("%.0f", damage) + " §7dégâts!");
            }
        }

        return hit;
    }

    /**
     * Effet d'impact quand le couteau s'arrête
     */
    private void spawnImpactEffect(Location loc, World world) {
        world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
        world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.8f);
        world.spawnParticle(Particle.SNOWFLAKE, loc, 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.CRIT, loc, 15, 0.3, 0.3, 0.3, 0.2);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== VENGEFUL CREAKING SYSTEM (Creaking Vengeur) ====================

/**
 * Emprise Racinaire - Chance de faire jaillir des racines sous les ennemis
 * 8% chance, root 1.5s + 25% dégâts joueur
 */
@Getter
class RootGraspPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseChance;           // 8% = 0.08
    private final double baseDamagePercent;    // 25% = 0.25
    private final int rootDurationTicks;       // 1.5s = 30 ticks
    private static final Random random = new Random();

    public RootGraspPassive(String id, String name, String desc, double chance,
                             double damagePercent, int rootDuration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseChance = chance;
        this.baseDamagePercent = damagePercent;
        this.rootDurationTicks = rootDuration;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Calcule la chance selon le niveau
     * Base: 8%, Niveau 5+: 12%
     */
    private double getEffectiveChance(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.12; // 12%
        }
        return baseChance; // 8%
    }

    /**
     * Calcule le % de dégâts selon le niveau
     * Base: 25%, Niveau 5+: 30%
     */
    private double getEffectiveDamagePercent(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.30; // 30%
        }
        return baseDamagePercent; // 25%
    }

    /**
     * Vérifie si on applique Wither (étoiles max)
     */
    private boolean shouldApplyWither(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        double chance = getEffectiveChance(petData);

        if (random.nextDouble() < chance) {
            spawnRoots(player, target, petData);
        }
    }

    /**
     * Fait jaillir des racines sous l'ennemi
     */
    private void spawnRoots(Player player, LivingEntity target, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        Location loc = target.getLocation();
        World world = target.getWorld();

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);
        double rootDamage = baseDamage * getEffectiveDamagePercent(petData) * petData.getStatMultiplier();

        boolean applyWither = shouldApplyWither(petData);

        // Effet visuel des racines
        world.playSound(loc, Sound.BLOCK_ROOTS_BREAK, 1.5f, 0.6f);
        world.playSound(loc, Sound.BLOCK_WOOD_BREAK, 1.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_CREAKING_SPAWN, 0.8f, 1.0f);

        // Particules de racines qui jaillissent
        spawnRootParticles(loc, world);

        // Root (immobilisation via slowness extrême + no jump)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, rootDurationTicks, 100)); // Immobile
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, rootDurationTicks, 128)); // No jump

        // Wither si étoiles max
        if (applyWither) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, rootDurationTicks + 20, 1)); // Wither II
        }

        // Infliger les dégâts
        var zombieManager = plugin.getZombieManager();
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, rootDamage,
                    com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
            } else {
                target.damage(rootDamage, player);
            }
        } else {
            target.damage(rootDamage, player);
        }

        // Message
        String witherMsg = applyWither ? " §8+ Wither" : "";
        player.sendMessage("§2[Pet] §a🌿 §7Racines! §c" +
            String.format("%.0f", rootDamage) + " §7dégâts + Root " +
            String.format("%.1f", rootDurationTicks / 20.0) + "s" + witherMsg);
    }

    /**
     * Crée l'effet visuel des racines
     */
    private void spawnRootParticles(Location center, World world) {
        // Racines qui montent du sol
        new BukkitRunnable() {
            int ticks = 0;
            double height = 0;

            @Override
            public void run() {
                if (ticks >= 15) {
                    cancel();
                    return;
                }

                // Cercle de racines
                for (double angle = 0; angle < 360; angle += 45) {
                    double rad = Math.toRadians(angle + ticks * 20);
                    double r = 0.5 + ticks * 0.05;
                    double x = center.getX() + r * Math.cos(rad);
                    double z = center.getZ() + r * Math.sin(rad);
                    Location particleLoc = new Location(world, x, center.getY() + height, z);

                    world.spawnParticle(Particle.BLOCK, particleLoc,
                        3, 0.1, 0.2, 0.1, 0,
                        Material.PALE_OAK_WOOD.createBlockData());
                    world.spawnParticle(Particle.BLOCK, particleLoc,
                        2, 0.1, 0.1, 0.1, 0,
                        Material.PALE_OAK_LEAVES.createBlockData());
                }

                height += 0.1;
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void activate(Player player, PetData petData) { }
}

/**
 * Forêt Éveillée - Explosion massive de racines avec éruptions continues
 * Root 4s + 100% dégâts joueur dans 12 blocs, puis éruptions pendant 6s
 */
@Getter
class AwakenedForestActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final RootGraspPassive linkedPassive;
    private final double damagePercent;        // 100% = 1.00
    private final double explosionRadius;      // 12 blocs
    private final int rootDurationTicks;       // 4s = 80 ticks
    private final int eruptionDurationTicks;   // 6s = 120 ticks

    public AwakenedForestActive(String id, String name, String desc, RootGraspPassive passive,
                                 double dmgPercent, double radius, int rootDuration, int eruptionDuration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damagePercent = dmgPercent;
        this.explosionRadius = radius;
        this.rootDurationTicks = rootDuration;
        this.eruptionDurationTicks = eruptionDuration;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    @Override
    public void activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        Location center = player.getLocation();
        World world = player.getWorld();

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercent);
        double explosionDamage = baseDamage * this.damagePercent * petData.getStatMultiplier();
        double eruptionDamage = explosionDamage * 0.3; // 30% pour les éruptions

        boolean applyWither = petData.getStarPower() > 0;

        // Son d'activation
        world.playSound(center, Sound.ENTITY_CREAKING_ACTIVATE, 2.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
        world.playSound(center, Sound.BLOCK_ROOTS_BREAK, 2.0f, 0.4f);

        // Explosion initiale de racines
        spawnForestExplosion(center, world);

        // Affecter tous les ennemis dans le rayon
        int affected = 0;
        for (Entity entity : world.getNearbyEntities(center, explosionRadius, explosionRadius, explosionRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(center);
                if (distSq > explosionRadius * explosionRadius) continue;

                affected++;

                // Root (immobilisation)
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, rootDurationTicks, 100));
                living.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, rootDurationTicks, 128));

                // Wither si étoiles max
                if (applyWither) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, rootDurationTicks + 40, 1));
                }

                // Dégâts
                dealDamage(player, living, explosionDamage, plugin);

                // Effet visuel sur chaque cible
                spawnRootOnTarget(living.getLocation(), world);
            }
        }

        // Lancer les éruptions continues
        startEruptions(player, center, eruptionDamage, applyWither, plugin);

        // Message
        String witherMsg = applyWither ? " §8+ Wither" : "";
        player.sendMessage("§2[Pet] §a🌲 §7Forêt Éveillée! §e" + affected +
            " §7ennemis rootés + §c" + String.format("%.0f", explosionDamage) + " §7dégâts" + witherMsg);
    }

    /**
     * Crée l'explosion visuelle de la forêt
     */
    private void spawnForestExplosion(Location center, World world) {
        // Onde de racines qui s'expand
        new BukkitRunnable() {
            double radius = 0;

            @Override
            public void run() {
                if (radius >= explosionRadius) {
                    cancel();
                    return;
                }

                // Cercle de racines
                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    Location particleLoc = new Location(world, x, center.getY() + 0.2, z);

                    world.spawnParticle(Particle.BLOCK, particleLoc,
                        5, 0.2, 0.1, 0.2, 0,
                        Material.PALE_OAK_WOOD.createBlockData());
                    world.spawnParticle(Particle.BLOCK, particleLoc,
                        3, 0.1, 0.1, 0.1, 0,
                        Material.PALE_OAK_LEAVES.createBlockData());
                }

                // Son de craquement
                if (radius % 3 < 1) {
                    world.playSound(center.clone().add(radius, 0, 0), Sound.BLOCK_WOOD_BREAK, 0.5f, 0.6f);
                }

                radius += 1.5;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Spawn des racines sur une cible
     */
    private void spawnRootOnTarget(Location loc, World world) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.random() * 360;
            double rad = Math.toRadians(angle);
            double r = Math.random() * 0.8;
            double x = loc.getX() + r * Math.cos(rad);
            double z = loc.getZ() + r * Math.sin(rad);

            for (double y = 0; y < 1.5; y += 0.3) {
                Location particleLoc = new Location(world, x, loc.getY() + y, z);
                world.spawnParticle(Particle.BLOCK, particleLoc,
                    2, 0.05, 0.1, 0.05, 0,
                    Material.PALE_OAK_WOOD.createBlockData());
            }
        }
    }

    /**
     * Lance les éruptions de racines continues
     */
    private void startEruptions(Player player, Location center, double eruptionDamage,
                                 boolean applyWither, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        Random random = new Random();

        new BukkitRunnable() {
            int ticksRemaining = eruptionDurationTicks;

            @Override
            public void run() {
                if (ticksRemaining <= 0 || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Éruption toutes les 15 ticks (0.75s)
                if (ticksRemaining % 15 == 0) {
                    // Position aléatoire dans le rayon
                    double angle = random.nextDouble() * 360;
                    double rad = Math.toRadians(angle);
                    double r = random.nextDouble() * explosionRadius;
                    double x = center.getX() + r * Math.cos(rad);
                    double z = center.getZ() + r * Math.sin(rad);
                    Location eruptLoc = new Location(world, x, center.getY(), z);

                    // Ajuster Y au sol
                    eruptLoc = world.getHighestBlockAt(eruptLoc).getLocation().add(0, 1, 0);

                    // Effet d'éruption
                    spawnEruption(eruptLoc, world);

                    // Dégâts aux ennemis proches de l'éruption
                    for (Entity entity : world.getNearbyEntities(eruptLoc, 2, 2, 2)) {
                        if (entity instanceof LivingEntity living && !(entity instanceof Player)
                            && !(entity instanceof ArmorStand) && !living.isDead()) {

                            dealDamage(player, living, eruptionDamage, plugin);

                            // Mini root
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 50));

                            // Wither si étoiles max
                            if (applyWither) {
                                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                            }
                        }
                    }
                }

                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Crée une éruption de racine ponctuelle
     */
    private void spawnEruption(Location loc, World world) {
        world.playSound(loc, Sound.BLOCK_ROOTS_BREAK, 1.0f, 0.8f);
        world.playSound(loc, Sound.ENTITY_CREAKING_STEP, 0.8f, 1.2f);

        // Racines qui jaillissent
        new BukkitRunnable() {
            int ticks = 0;
            double height = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 360; angle += 60) {
                    double rad = Math.toRadians(angle + ticks * 30);
                    double r = 0.3;
                    double x = loc.getX() + r * Math.cos(rad);
                    double z = loc.getZ() + r * Math.sin(rad);
                    Location particleLoc = new Location(world, x, loc.getY() + height, z);

                    world.spawnParticle(Particle.BLOCK, particleLoc,
                        3, 0.1, 0.15, 0.1, 0,
                        Material.PALE_OAK_WOOD.createBlockData());
                }

                // Pointe de la racine
                world.spawnParticle(Particle.CRIT, loc.clone().add(0, height + 0.3, 0),
                    3, 0.1, 0.1, 0.1, 0.1);

                height += 0.2;
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                             com.rinaorc.zombiez.ZombieZPlugin plugin) {
        var zombieManager = plugin.getZombieManager();
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, double damage, PetData petData) { }
}
