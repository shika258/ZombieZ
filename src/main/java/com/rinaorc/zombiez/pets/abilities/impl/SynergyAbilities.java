package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.abilities.PetDamageUtils;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémentations des capacités de synergie des nouveaux pets
 * Ces abilities interagissent avec les systèmes de classe, talents, et mécaniques de jeu
 */
public class SynergyAbilities {
    // Classe conteneur pour les abilities de synergie
    private SynergyAbilities() {}

    /**
     * Classe abstraite de base pour les capacités de pet dans SynergyAbilities
     * Fournit une implémentation de base pour PetAbility
     */
    @Getter
    public static abstract class BasePetAbility implements PetAbility {
        protected final String id;
        protected final String displayName;
        protected final String description;
        protected final boolean active;

        protected BasePetAbility(String id, String name, String description, boolean isActive) {
            this.id = id;
            this.displayName = name;
            this.description = description;
            this.active = isActive;
        }

        @Override
        public boolean isPassive() {
            return !active;
        }

        @Override
        public void applyPassive(Player player, PetData petData) {
            // À implémenter par les sous-classes si passif
        }

        /**
         * Implémentation de l'interface PetAbility.activate()
         * Appelle doActivate() et retourne toujours true
         */
        @Override
        public boolean activate(Player player, PetData petData) {
            doActivate(player, petData);
            return true;
        }

        /**
         * Méthode d'activation pour les sous-classes
         * Les sous-classes doivent override cette méthode au lieu de activate()
         */
        protected void doActivate(Player player, PetData petData) {
            // À implémenter par les sous-classes si actif
        }

        // Méthodes pour les sous-classes qui utilisent des signatures différentes
        public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
            // Signature alternative pour compatibilité
        }

        public void onKill(Player player, LivingEntity victim, PetData petData) {
            // Signature alternative pour compatibilité
        }

        public void onDamageReceived(Player player, double damage, PetData petData) {
            // Signature alternative pour compatibilité
        }

        // Wrapper pour adapter les signatures vers l'interface PetAbility
        @Override
        public void onKill(Player player, PetData petData, LivingEntity killed) {
            onKill(player, killed, petData);
        }

        @Override
        public void onDamageReceived(Player player, PetData petData, double damage) {
            onDamageReceived(player, damage, petData);
        }

        @Override
        public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
            onDamageDealt(player, target, damage, petData);
        }
    }
}

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

    // Cooldown de 3 secondes sur le message d'erreur pour éviter le spam
    private static final Map<UUID, Long> lastErrorMessage = new ConcurrentHashMap<>();
    private static final long ERROR_MESSAGE_COOLDOWN = 3000L; // 3 secondes

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
            // Cooldown sur le message d'erreur pour éviter le spam
            long now = System.currentTimeMillis();
            Long lastMsg = lastErrorMessage.get(player.getUniqueId());
            if (lastMsg == null || now - lastMsg >= ERROR_MESSAGE_COOLDOWN) {
                player.sendMessage("§c[Pet] §7Pas assez de combo! (§e" + stacks + "§7/3 minimum)");
                lastErrorMessage.put(player.getUniqueId(), now);
            }
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
    public int getCooldown() { return 20; }

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
    // Thread-safe Set pour éviter les race conditions
    private final Set<UUID> markedEntities = ConcurrentHashMap.newKeySet();

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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
    private final Map<UUID, Integer> currentElement = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRotation = new ConcurrentHashMap<>();
    // Marques élémentaires sur les mobs: EntityUUID -> Set d'éléments (0,1,2)
    private final Map<UUID, Set<Integer>> elementalMarks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReactionTime = new ConcurrentHashMap<>();

    // Protection anti-récursion pour éviter les boucles infinies
    private final Set<UUID> processingReaction = ConcurrentHashMap.newKeySet();

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
        // Vérifier que la cible est valide et un Monster
        if (!(target instanceof Monster) || !target.isValid() || target.isDead()) {
            return damage;
        }

        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // Protection anti-récursion : ignorer si on est déjà en train de traiter une réaction pour ce joueur
        if (processingReaction.contains(playerUuid)) {
            return damage;
        }

        int currentEl = currentElement.getOrDefault(playerUuid, 0);
        World world = target.getWorld();

        // Récupérer les marques existantes sur la cible
        Set<Integer> marks = elementalMarks.computeIfAbsent(targetUuid, k -> ConcurrentHashMap.newKeySet());

        // Vérifier si on peut déclencher une réaction
        double bonusDamage = 0;

        if (!marks.isEmpty() && !marks.contains(currentEl)) {
            // Vérifier le cooldown de réaction
            long now = System.currentTimeMillis();
            long lastReaction = lastReactionTime.getOrDefault(targetUuid, 0L);

            int adjustedCooldown = (int) (reactionCooldownMs - (petData.getStatMultiplier() - 1) * 200);
            adjustedCooldown = Math.max(adjustedCooldown, 1500); // Min 1.5s

            if (now - lastReaction >= adjustedCooldown) {
                // Activer la protection anti-récursion AVANT de déclencher la réaction
                processingReaction.add(playerUuid);

                try {
                    // Copier les marques pour éviter ConcurrentModificationException
                    Set<Integer> marksCopy = new HashSet<>(marks);

                    // Nettoyer les marques AVANT la réaction pour éviter les boucles
                    marks.clear();
                    lastReactionTime.put(targetUuid, now);

                    // Déclencher la réaction avec les marques copiées
                    for (int existingMark : marksCopy) {
                        bonusDamage += triggerReaction(player, petData, target, existingMark, currentEl);
                    }
                } finally {
                    // Désactiver la protection anti-récursion
                    processingReaction.remove(playerUuid);
                }
            }
        }

        // Ajouter la nouvelle marque seulement si la cible est encore valide
        if (target.isValid() && !target.isDead()) {
            marks.add(currentEl);

            // Effet visuel de marquage (léger)
            Particle markParticle = getElementParticle(currentEl);
            world.spawnParticle(markParticle, target.getLocation().add(0, 1.5, 0), 5, 0.2, 0.2, 0.2, 0.02);

            // Appliquer l'effet élémentaire de base
            applyElementEffect(target, currentEl);
        }

        // Nettoyer les marques des mobs morts (moins fréquemment)
        if (Math.random() < 0.1) {
            cleanupDeadMarks();
        }

        return damage + bonusDamage;
    }

    private double triggerReaction(Player player, PetData petData, LivingEntity target,
                                    int element1, int element2) {
        // Vérifier que la cible est valide
        if (!target.isValid() || target.isDead()) {
            return 0;
        }

        World world = target.getWorld();
        Location loc = target.getLocation().clone();

        // Calculer les dégâts de réaction
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double adjustedPercent = reactionDamagePercent + (petData.getStatMultiplier() - 1) * 0.15;
        double reactionDamage = playerDamage * adjustedPercent;

        // Déterminer le type de réaction (utiliser les plus petits indices pour cohérence)
        int min = Math.min(element1, element2);
        int max = Math.max(element1, element2);

        if (min == 0 && max == 1) {
            // VAPORISATION (Feu + Glace) - Burst de dégâts
            double actualDamage = Math.min(reactionDamage, target.getHealth() + 10);
            target.damage(reactionDamage, player);
            petData.addDamage((long) actualDamage);

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
            int hitCount = 0;
            double totalAoeDamage = 0;

            for (Entity entity : target.getNearbyEntities(4, 3, 4)) {
                if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                    double actualDamage = Math.min(aoeDamage, monster.getHealth() + 10);
                    monster.damage(aoeDamage, player);
                    totalAoeDamage += actualDamage;
                    hitCount++;

                    // Knockback
                    Vector knockback = monster.getLocation().toVector()
                        .subtract(loc.toVector()).normalize().multiply(1.2);
                    knockback.setY(0.5);
                    monster.setVelocity(knockback);

                    // Particules sur chaque cible
                    world.spawnParticle(Particle.EXPLOSION, monster.getLocation(), 1);
                }
            }

            petData.addDamage((long) totalAoeDamage);

            // Effet central
            world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 1, 1, 1, 0.3);
            world.spawnParticle(Particle.FLAME, loc, 20, 0.8, 0.5, 0.8, 0.1);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
            world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);

            if (hitCount > 0) {
                player.sendMessage("§a[Pet] §e§l⚡ SURCHARGE! §7" + hitCount + " cibles touchées (§c" + (int)totalAoeDamage + " §7dégâts AoE)");
            } else {
                player.sendMessage("§a[Pet] §e§l⚡ SURCHARGE! §7Explosion!");
            }

            return totalAoeDamage;

        } else if (min == 1 && max == 2) {
            // SUPRACONDUCTION (Glace + Foudre) - Réduction de défense
            double supraconductionDamage = reactionDamage * 0.5;
            double actualDamage = Math.min(supraconductionDamage, target.getHealth() + 10);
            target.damage(supraconductionDamage, player);
            petData.addDamage((long) actualDamage);

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

            player.sendMessage("§a[Pet] §b§l❄ SUPRACONDUCTION! §c+" + (int)supraconductionDamage + " §7dégâts, -20% défense 5s!");

            return supraconductionDamage;
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

    void updateAxolotlColor(Player player, int element) {
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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

        Map<com.rinaorc.zombiez.items.types.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);

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
            var foodRegistry = plugin.getPassiveMobManager() != null ? plugin.getPassiveMobManager().getFoodRegistry() : null;
            if (foodRegistry != null) {
                var food = foodRegistry.getRandomZombieFood();
                if (food != null) {
                    org.bukkit.inventory.ItemStack foodItem = food.createItemStack(1);
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
            var playerZone = plugin.getZoneManager().getPlayerZone(player);
            int zone = playerZone != null ? playerZone.getId() : 1;

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

        Map<com.rinaorc.zombiez.items.types.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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

        Map<com.rinaorc.zombiez.items.types.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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
                target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, targetLoc, 25, 0.5, 0.5, 0.5, 0.2);
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

        Map<com.rinaorc.zombiez.items.types.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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

        Map<com.rinaorc.zombiez.items.types.StatType, Double> playerStats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 7.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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
        double baseDamage = PetDamageUtils.getEffectiveDamage(player);
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

        // Message final (affiche les dégâts totaux infligés)
        int totalDamage = (int)(damage * enemiesHit);
        player.sendMessage("§a[Pet] §c§l💥 DÉTONATION FONGIQUE! §c" + totalDamage +
            " §7dégâts totaux (§c" + (int)damage + "§7/cible) → §e" + enemiesHit + " §7ennemi" + (enemiesHit > 1 ? "s" : "") +
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
    private final double procChance;        // 5% de chance (0.05)
    private final int stunDurationTicks;    // 1s = 20 ticks
    private final int stunRadius;           // 3 blocs

    public HeavyStepsPassive(String id, String name, String desc, double procChance, int stunTicks, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = procChance;
        this.stunDurationTicks = stunTicks;
        this.stunRadius = radius;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        // Chance de proc ajustée selon le niveau (+1% par multiplicateur)
        double adjustedChance = procChance + (petData.getStatMultiplier() - 1) * 0.01;

        if (Math.random() < adjustedChance) {
            triggerQuake(player, petData);
        }

        return damage;
    }

    private void triggerQuake(Player player, PetData petData) {
        Location center = player.getLocation();
        World world = center.getWorld();

        // Durée de stun ajustée par niveau
        int adjustedStunDuration = (int) (stunDurationTicks + (petData.getStatMultiplier() - 1) * 10);

        // Calcul des dégâts AoE (20% des dégâts de l'arme du joueur)
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double quakeDamage = playerDamage * 0.20 * petData.getStatMultiplier();

        // Stun et dégâts à tous les mobs dans le rayon
        int enemiesStunned = 0;
        double totalDamage = 0;
        for (Entity entity : world.getNearbyEntities(center, stunRadius, stunRadius, stunRadius)) {
            if (entity instanceof Monster monster) {
                // Infliger les dégâts AoE
                monster.damage(quakeDamage, player);
                petData.addDamage((long) quakeDamage);
                totalDamage += quakeDamage;

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
            player.sendMessage("§a[Pet] §6Pas Lourds! §c" + String.format("%.1f", totalDamage) +
                " §7dégâts → §e" + enemiesStunned +
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
    private final double damagePercent;     // 80% des dégâts de l'arme (0.80)
    private final int stunDurationTicks;    // 2s = 40 ticks
    private final int slamRadius;           // 8 blocs

    public SeismicSlamActive(String id, String name, String desc, double damagePercent, int stunTicks, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePercent = damagePercent;
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

        // Calcul des dégâts basés sur l'arme du joueur (80%)
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double damage = playerDamage * damagePercent * petData.getStatMultiplier();
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
    private final double procChance;              // 5% de chance de base (0.05)
    private final double damagePercent;           // 30% des dégâts du joueur

    public WispFireballPassive(String id, String name, String desc, double procChance, double damagePercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = procChance;
        this.damagePercent = damagePercent;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        // Chance de proc ajustée selon le niveau
        double adjustedChance = procChance + (petData.getStatMultiplier() - 1) * 0.02;

        if (Math.random() < adjustedChance) {
            // Délai de 2 ticks pour éviter le conflit avec les dégâts de l'attaque du joueur
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("ZombieZ"),
                () -> {
                    if (target.isValid() && !target.isDead()) {
                        shootFireball(player, petData, target);
                    }
                },
                2L
            );
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double fireballDamage = playerDamage * adjustedDamagePercent;

        // Son de lancement
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);

        // Animation de la boule de feu (projectile visuel)
        new BukkitRunnable() {
            Location currentLoc = origin.clone();
            int ticks = 0;
            final int maxTicks = 40; // 2 secondes max
            boolean hasHit = false;

            @Override
            public void run() {
                if (ticks >= maxTicks || hasHit) {
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
                    if (entity instanceof Monster monster && !hasHit) {
                        hasHit = true;

                        // Impact avec dégâts
                        monster.damage(fireballDamage, player);
                        monster.setFireTicks(60); // Brûle pendant 3s
                        petData.addDamage((long) fireballDamage);

                        // Effets d'impact
                        world.spawnParticle(Particle.LAVA, currentLoc, 15, 0.3, 0.3, 0.3, 0);
                        world.spawnParticle(Particle.FLAME, currentLoc, 20, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(currentLoc, Sound.ENTITY_GENERIC_BURN, 1.0f, 1.0f);

                        // Message uniquement si dégâts significatifs (pas de spam)
                        if (fireballDamage >= 1) {
                            player.sendMessage("§a[Pet] §6🔥 Boule de feu! §c" + String.format("%.1f", fireballDamage) + " §7dégâts");
                        }

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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
        final double finalMinDist = minDist;

        // Calcul des durées ajustées par niveau
        int adjustedImmobilize = (int) (immobilizeDurationTicks + (petData.getStatMultiplier() - 1) * 20);
        int adjustedMarkDuration = (int) (markDurationTicks + (petData.getStatMultiplier() - 1) * 40);

        // Son de préparation
        world.playSound(playerLoc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.5f);
        player.sendMessage("§a[Pet] §c§l\uD83D\uDD77 EMBUSCADE!");

        // Spawn d'une araignée temporaire pour l'animation de bond
        Location spiderStart = playerLoc.clone().add(0, 0.5, 0);
        org.bukkit.entity.Spider leapSpider = (org.bukkit.entity.Spider) world.spawnEntity(spiderStart, org.bukkit.entity.EntityType.SPIDER);

        // Configurer l'araignée temporaire
        leapSpider.setAI(false);
        leapSpider.setSilent(true);
        leapSpider.setInvulnerable(true);
        leapSpider.setPersistent(false);
        leapSpider.setGravity(false);
        leapSpider.setCustomName("§c§l🕷");
        leapSpider.setCustomNameVisible(true);

        // Réduire la taille si possible (via scale attribute)
        if (leapSpider.getAttribute(org.bukkit.attribute.Attribute.SCALE) != null) {
            leapSpider.getAttribute(org.bukkit.attribute.Attribute.SCALE).setBaseValue(0.7);
        }

        // Calculer la direction et la vitesse du bond
        Vector direction = targetLoc.toVector().subtract(spiderStart.toVector());
        double distance = direction.length();
        int flightTicks = Math.max(8, (int) (distance * 1.5)); // Durée basée sur la distance
        Vector velocity = direction.normalize().multiply(distance / flightTicks);
        // Ajouter une courbe parabolique (arc de saut)
        velocity.setY(velocity.getY() + 0.15);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= flightTicks || !finalTarget.isValid() || !leapSpider.isValid()) {
                    // Arrivée sur la cible - Impact!
                    if (leapSpider.isValid()) {
                        // Effet de disparition
                        world.spawnParticle(Particle.POOF, leapSpider.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
                        leapSpider.remove();
                    }
                    executeImpact(player, petData, finalTarget, adjustedImmobilize, adjustedMarkDuration);
                    cancel();
                    return;
                }

                // Déplacer l'araignée
                Location newLoc = leapSpider.getLocation().add(velocity);
                // Courbe parabolique descendante après la moitié du trajet
                if (ticks > flightTicks / 2) {
                    newLoc.add(0, -0.08 * (ticks - flightTicks / 2), 0);
                }
                leapSpider.teleport(newLoc);

                // Particules de traînée
                world.spawnParticle(Particle.BLOCK, newLoc, 3, 0.1, 0.1, 0.1, 0,
                    org.bukkit.Material.COBWEB.createBlockData());

                // Son de saut
                if (ticks % 5 == 0) {
                    world.playSound(newLoc, Sound.ENTITY_SPIDER_STEP, 0.6f, 1.8f);
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
    private final Set<UUID> markedEntities = ConcurrentHashMap.newKeySet();

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
                JavaPlugin.getPlugin(ZombieZPlugin.class),
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

        // Seuil d'exécution ajusté par niveau (20% base, jusqu'à 25% au niveau max)
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
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);

        return true;
    }

    private void executeImpact(Player player, PetData petData, Monster target, double adjustedExecuteThreshold) {
        Location impactLoc = target.getLocation();
        World world = impactLoc.getWorld();

        // Calculer les dégâts (200% des dégâts du joueur)
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double damage = playerDamage * damageMultiplier * petData.getStatMultiplier();

        // Vérifier si c'est une exécution
        double healthPercent = target.getHealth() / target.getMaxHealth();
        boolean isExecute = healthPercent <= adjustedExecuteThreshold;

        if (isExecute) {
            // EXÉCUTION! Dégâts massifs pour tuer instantanément
            damage = target.getHealth() + 100; // Assure la mort

            // Effets visuels d'exécution
            world.spawnParticle(Particle.SOUL, impactLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.ENCHANTED_HIT, impactLoc, 20, 0.3, 0.3, 0.3, 0.2);
            world.playSound(impactLoc, Sound.ENTITY_PHANTOM_DEATH, 1.0f, 0.5f);
            world.playSound(impactLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.5f, 1.5f);

            player.sendMessage("§a[Pet] §c§l💀 EXÉCUTION! §7La proie a été achevée!");

            // Starpower: L'exécution soigne 20% HP au joueur si le pet a des étoiles
            if (petData.getStarPower() > 0) {
                double healAmount = player.getMaxHealth() * 0.20;
                double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
                player.setHealth(newHealth);
                player.sendMessage("§a[Pet] §d✦ Starpower: §a+" + (int)healAmount + " HP §7récupérés!");
                world.spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 8, 0.3, 0.3, 0.3, 0);
                world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        } else {
            // Dégâts normaux
            world.spawnParticle(Particle.CRIT, impactLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.WITCH, impactLoc, 15, 0.3, 0.3, 0.3, 0.05);
            world.playSound(impactLoc, Sound.ENTITY_PHANTOM_BITE, 1.0f, 1.0f);
            world.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);

            player.sendMessage("§a[Pet] §c\uD83E\uDD87 " + (int)damage + " §7dégâts! §8(Execute si <" +
                (int)(adjustedExecuteThreshold * 100) + "% HP)");
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
    private final double procChance;              // 5% de chance (0.05)
    private final double bounceDamageBonus;       // +30%
    private final int stunDurationTicks;          // 0.5s = 10 ticks

    public FrogBouncePassive(String id, String name, String desc, double procChance, double bonus, int stunTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = procChance;
        this.bounceDamageBonus = bonus;
        this.stunDurationTicks = stunTicks;
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        World world = player.getWorld();

        // Chance de proc ajustée par niveau (+1% par multiplicateur)
        double adjustedChance = procChance + (petData.getStatMultiplier() - 1) * 0.01;

        if (Math.random() < adjustedChance) {
            // BOND!
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
            world.spawnParticle(Particle.SPLASH, targetLoc.clone().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.1);
            world.spawnParticle(Particle.ITEM_SLIME, targetLoc, 10, 0.3, 0.3, 0.3, 0.05);

            // Sons de grenouille
            world.playSound(targetLoc, Sound.ENTITY_FROG_LONG_JUMP, 1.0f, 1.2f);
            world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.0f);

            player.sendMessage("§a[Pet] §2🐸 BOND! §c" + String.format("%.1f", bonusDamage) + " §7dégâts bonus + stun!");

            return damage + bonusDamage;
        }

        return damage;
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
                world.spawnParticle(Particle.ITEM_SLIME, currentLoc, 3, 0.1, 0.1, 0.1, 0.01);

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
                JavaPlugin.getPlugin(ZombieZPlugin.class),
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
    private final Map<UUID, Integer> attackCounters = new ConcurrentHashMap<>();

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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);
    }

    public int getAttackCount(UUID uuid) {
        return attackCounters.getOrDefault(uuid, 0);
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
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
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

        // Appliquer la confusion à tous les monstres et les faire se cibler entre eux
        for (int i = 0; i < affectedMonsters.size(); i++) {
            Monster monster = affectedMonsters.get(i);

            // Aveuglement pour l'effet visuel
            monster.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, adjustedConfusion, 0, false, false));
            // Lenteur légère
            monster.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, adjustedConfusion, 0, false, false));

            // Faire cibler un autre monstre (rotation circulaire)
            if (affectedMonsters.size() >= 2) {
                Monster target = affectedMonsters.get((i + 1) % affectedMonsters.size());
                if (monster instanceof Mob mob) {
                    mob.setTarget(target);
                }
            }
        }

        // Calculer les dégâts d'infight (basés sur les dégâts du joueur)
        final double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        final double infightDamage = playerDamage * 0.3 * petData.getStatMultiplier();

        // Animation du nuage d'encre et combat entre monstres
        new BukkitRunnable() {
            int ticksAlive = 0;
            final int cloudDuration = adjustedConfusion;
            int nextInfightTick = 20; // Premier infight après 1 seconde

            @Override
            public void run() {
                if (ticksAlive >= cloudDuration) {
                    // Dissipation du nuage - remettre le ciblage normal
                    for (Monster monster : affectedMonsters) {
                        if (monster.isValid() && !monster.isDead() && monster instanceof Mob mob) {
                            mob.setTarget(player); // Remettre le joueur comme cible
                        }
                    }
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
                        // Chaque monstre attaque un voisin aléatoire
                        for (int i = 0; i < affectedMonsters.size(); i++) {
                            Monster attacker = affectedMonsters.get(i);
                            if (!attacker.isValid() || attacker.isDead()) continue;

                            // Trouver la cible la plus proche
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

                            if (target != null && minDist < 36) { // 6 blocs max
                                // Maintenir le ciblage
                                if (attacker instanceof Mob mob) {
                                    mob.setTarget(target);
                                }

                                // Appliquer les dégâts (attribués au joueur pour le système ZombieZ)
                                target.damage(infightDamage, player);
                                petData.addDamage((long) infightDamage);

                                // Effet visuel de l'attaque
                                Location attackerLoc = attacker.getLocation();
                                Location targetLoc = target.getLocation();
                                Location midPoint = attackerLoc.clone().add(targetLoc).multiply(0.5).add(0, 1, 0);

                                world.spawnParticle(Particle.SWEEP_ATTACK, midPoint, 1, 0, 0, 0, 0);
                                world.spawnParticle(Particle.SQUID_INK, targetLoc.add(0, 1, 0),
                                    8, 0.3, 0.3, 0.3, 0.05);
                                world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc,
                                    3, 0.2, 0.2, 0.2, 0.1);

                                // Son d'attaque
                                if (Math.random() < 0.3) {
                                    world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.8f);
                                }
                            }
                        }
                    }
                }

                ticksAlive++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);

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
    private final Map<UUID, Long> lastRetaliation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 5000; // 5s entre les messages

    public SwarmRetaliationPassive(String id, String name, String desc, double dmgPercent, long cooldownMs) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePercent = dmgPercent;
        this.baseCooldownMs = cooldownMs;
        PassiveAbilityCleanup.registerForCleanup(lastRetaliation);
        PassiveAbilityCleanup.registerForCleanup(lastMessage);
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
        double adjustedPercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.05;
        double retaliationDamage = playerDamage * adjustedPercent;

        // Trouver l'attaquant le plus proche et contre-attaquer
        Monster target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
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

            // Créer 3 trajectoires d'abeilles animées
            for (int i = 0; i < 3; i++) {
                final double angleOffset = (2 * Math.PI / 3) * i;

                Bukkit.getScheduler().runTaskLater(
                    JavaPlugin.getPlugin(ZombieZPlugin.class),
                    () -> animateBeeAttack(player, petData, playerLoc.clone(), finalTarget, retaliationDamage / 3, angleOffset),
                    i * 2L
                );
            }

            // Son de l'essaim
            world.playSound(playerLoc, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, 1.2f);

            // Limiter les messages (1 message max toutes les 5 secondes)
            long lastMsgTime = lastMessage.getOrDefault(uuid, 0L);
            if (now - lastMsgTime >= MESSAGE_COOLDOWN_MS) {
                lastMessage.put(uuid, now);
                player.sendMessage("§a[Pet] §e🐝 REPRÉSAILLES! §7L'essaim contre-attaque!");
            }
        }
    }

    private void animateBeeAttack(Player player, PetData petData, Location start, Monster target, double damage, double angleOffset) {
        if (!target.isValid() || target.isDead()) return;

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
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);

        return true;
    }

    private void animateStingAttack(Location from, Monster target, double damage, Player player, PetData petData, World world) {
        if (!target.isValid() || target.isDead()) return;

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
                world.spawnParticle(Particle.ENCHANTED_HIT, currentLoc, 2, 0.05, 0.05, 0.05, 0);

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
        }.runTaskTimer(JavaPlugin.getPlugin(ZombieZPlugin.class), 0L, 1L);
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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
                                world.spawnParticle(Particle.ENCHANTED_HIT, monsterLoc, 5, 0.2, 0.2, 0.2, 0.1);

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
        double playerDamage = PetDamageUtils.getEffectiveDamage(player);
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
                                world.spawnParticle(Particle.ENCHANTED_HIT, monsterLoc, 8, 0.3, 0.3, 0.3, 0.15);
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
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
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
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        // Calculer les dégâts du joueur
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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
            return false;
        }

        // Lancer la chaîne de téléportation
        player.sendMessage("§5[Pet] §d⚡ §5Frappe Fantôme activée! §7(" + maxChains + " cibles)");
        world.playSound(start, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);

        executePhantomChain(player, firstTarget, strikeDamage, echoDamage, maxChains, finalVortex, plugin);
        return true;
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

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
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
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
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

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
        return true;
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
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
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        // Calculer les dégâts du joueur
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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
        return true;
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

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
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
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
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        Location center = player.getLocation();
        World world = player.getWorld();

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
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
        return true;
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

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== NECROMANCER WITCH SYSTEM (Sorcière Nécromancienne) ====================

/**
 * Drain de Vie - Toutes les 5s, draine jusqu'à 5 ennemis
 * +5% dégâts par ennemi drainé, -5% HP des mobs drainés
 */
@Getter
class LifeDrainPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double drainDamagePercent;     // 5% = 0.05 HP retiré aux mobs
    private final double damageBuffPerEnemy;     // 5% = 0.05 par ennemi
    private final double drainRadius;            // 18 blocs
    private final int maxEnemies;                // Max 5 ennemis

    // Tracking des buffs actifs et timing
    private static final Map<UUID, Long> lastDrainTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> activeDamageBuffs = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> buffExpireTime = new ConcurrentHashMap<>();

    public LifeDrainPassive(String id, String name, String desc, double drainDmgPercent,
                             double dmgBuffPerEnemy, double radius, int maxTargets) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.drainDamagePercent = drainDmgPercent;
        this.damageBuffPerEnemy = dmgBuffPerEnemy;
        this.drainRadius = radius;
        this.maxEnemies = maxTargets;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Récupère l'intervalle de drain selon le niveau
     * Base: 5s, Niveau 5+: 4s
     */
    private long getDrainInterval(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 4000; // 4 secondes
        }
        return 5000; // 5 secondes
    }

    /**
     * Récupère le buff de dégâts par ennemi selon le niveau
     * Base: 5%, Niveau 5+: 7%
     */
    private double getEffectiveBuffPerEnemy(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.07; // 7%
        }
        return damageBuffPerEnemy; // 5%
    }

    /**
     * Récupère le buff de dégâts actif pour un joueur
     */
    public double getActiveDamageBuff(UUID playerId) {
        Long expireTime = buffExpireTime.get(playerId);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            activeDamageBuffs.remove(playerId);
            buffExpireTime.remove(playerId);
            return 0.0;
        }
        return activeDamageBuffs.getOrDefault(playerId, 0.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Tick toutes les secondes environ
        long currentTime = System.currentTimeMillis();
        long interval = getDrainInterval(petData);

        Long lastDrain = lastDrainTime.get(player.getUniqueId());
        if (lastDrain != null && currentTime - lastDrain < interval) {
            return; // Pas encore le moment
        }

        // Effectuer le drain
        performLifeDrain(player, petData, currentTime);
    }

    /**
     * Effectue le drain de vie
     */
    private void performLifeDrain(Player player, PetData petData, long currentTime) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Trouver les ennemis à drainer
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(playerLoc, drainRadius, drainRadius, drainRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(playerLoc);
                if (distSq <= drainRadius * drainRadius) {
                    targets.add(living);
                    if (targets.size() >= maxEnemies) break;
                }
            }
        }

        if (targets.isEmpty()) return;

        // Marquer le temps de drain
        lastDrainTime.put(player.getUniqueId(), currentTime);

        // Calculer le buff de dégâts
        double buffPerEnemy = getEffectiveBuffPerEnemy(petData);
        double totalBuff = buffPerEnemy * targets.size();

        // Appliquer le buff (dure jusqu'au prochain drain)
        long buffDuration = getDrainInterval(petData) + 1000; // Dure légèrement plus que l'intervalle
        activeDamageBuffs.put(player.getUniqueId(), totalBuff);
        buffExpireTime.put(player.getUniqueId(), currentTime + buffDuration);

        // Effet visuel et sonore de la Witch
        world.playSound(playerLoc, Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_WITCH_AMBIENT, 0.8f, 1.2f);

        // Drainer chaque cible
        var zombieManager = plugin.getZombieManager();
        for (LivingEntity target : targets) {
            drainTarget(player, target, zombieManager, world);
        }

        // Message au joueur
        int buffPercent = (int) (totalBuff * 100);
        player.sendMessage("§5[Pet] §d🔮 §7Drain! §e" + targets.size() +
            " §7ennemis → §c+" + buffPercent + "% §7dégâts");

        // Bonus étoiles max: Régénération du joueur
        if (petData.getStarPower() > 0) {
            double healAmount = 0.02 * targets.size() * player.getMaxHealth(); // 2% HP par ennemi
            double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + healAmount);
            player.setHealth(newHealth);

            world.spawnParticle(Particle.HEART, playerLoc.add(0, 2, 0),
                targets.size(), 0.3, 0.2, 0.3, 0);
        }
    }

    /**
     * Drain une cible spécifique
     */
    private void drainTarget(Player player, LivingEntity target,
                              com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {
        Location targetLoc = target.getLocation();

        // Calculer et infliger les dégâts (5% des HP max du mob)
        double maxHealth = target.getMaxHealth();
        double drainDamage = maxHealth * drainDamagePercent;

        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, drainDamage,
                    com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
            } else {
                target.damage(drainDamage, player);
            }
        } else {
            target.damage(drainDamage, player);
        }

        // Effet visuel: ligne de drain vers le joueur
        spawnDrainBeam(targetLoc.add(0, 1, 0), player.getLocation().add(0, 1.5, 0), world);

        // Particules sur la cible
        world.spawnParticle(Particle.WITCH, targetLoc.add(0, 0.5, 0),
            10, 0.3, 0.5, 0.3, 0.02);
        world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc.add(0, 1, 0),
            3, 0.2, 0.2, 0.2, 0.1);
    }

    /**
     * Crée un faisceau visuel de drain
     */
    private void spawnDrainBeam(Location from, Location to, World world) {
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        int steps = (int) (distance * 3);
        for (int i = 0; i < steps; i++) {
            Location particleLoc = from.clone().add(direction.clone().multiply(i / 3.0));
            world.spawnParticle(Particle.WITCH, particleLoc, 1, 0, 0, 0, 0);
            if (i % 2 == 0) {
                world.spawnParticle(Particle.SOUL, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        // Le buff de dégâts est appliqué via getActiveDamageBuff()
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    /**
     * Nettoyer les données d'un joueur
     */
    public static void cleanup(UUID playerId) {
        lastDrainTime.remove(playerId);
        activeDamageBuffs.remove(playerId);
        buffExpireTime.remove(playerId);
    }
}

/**
 * Zombie Suicidaire - Invoque un zombie qui explose en poison sur son chemin
 * 560% des dégâts de l'arme
 */
@Getter
class SuicidalZombieActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final LifeDrainPassive linkedPassive;
    private final double damagePercent;         // 560% = 5.60
    private final double explosionRadius;       // Rayon de l'explosion de poison
    private final int zombieLifetimeTicks;      // Durée de vie du zombie

    public SuicidalZombieActive(String id, String name, String desc, LifeDrainPassive passive,
                                 double dmgPercent, double radius, int lifetime) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damagePercent = dmgPercent;
        this.explosionRadius = radius;
        this.zombieLifetimeTicks = lifetime;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    @Override
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        World world = player.getWorld();
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercentBonus = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercentBonus);
        double poisonDamage = baseDamage * damagePercent * petData.getStatMultiplier();

        boolean leaveTrail = petData.getStarPower() > 0; // Traînée de poison si étoiles max

        // Sons d'invocation
        world.playSound(spawnLoc, Sound.ENTITY_WITCH_CELEBRATE, 1.5f, 0.6f);
        world.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
        world.playSound(spawnLoc, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.8f);

        // Particules d'invocation
        world.spawnParticle(Particle.WITCH, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(Particle.SOUL, spawnLoc, 15, 0.3, 0.5, 0.3, 0.05);

        // Créer le zombie suicidaire (visuel uniquement, pas via ZombieManager car c'est un allié)
        Zombie suicidalZombie = world.spawn(spawnLoc, Zombie.class, zombie -> {
            zombie.setBaby(true);
            zombie.setCustomName("§5Serviteur Nécromantique");
            zombie.setCustomNameVisible(true);
            zombie.setPersistent(false);
            zombie.setRemoveWhenFarAway(true);
            zombie.setSilent(true);
            zombie.setAI(false); // On gère le mouvement manuellement

            // Apparence de zombie empoisonné
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 0, false, false));

            // Tag pour identification
            zombie.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "necro_zombie"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1
            );
        });

        // Message
        player.sendMessage("§5[Pet] §d💀 §7Zombie Suicidaire invoqué! §8(560% dégâts poison)");

        // Contrôler le zombie
        controlZombie(player, suicidalZombie, poisonDamage, leaveTrail, plugin);
        return true;
    }

    /**
     * Contrôle le mouvement et les explosions du zombie
     */
    private void controlZombie(Player player, Zombie zombie, double poisonDamage,
                                boolean leaveTrail, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = zombie.getWorld();
        Set<UUID> hitEntities = new HashSet<>();
        List<Location> trailLocations = new ArrayList<>();

        new BukkitRunnable() {
            int ticksAlive = 0;
            LivingEntity currentTarget = null;
            int explosionCount = 0;
            final int maxExplosions = 3; // Explose 3 fois avant de mourir

            @Override
            public void run() {
                // Vérifier si le zombie est toujours vivant
                if (!zombie.isValid() || zombie.isDead() || ticksAlive >= zombieLifetimeTicks) {
                    // Explosion finale de décomposition
                    finalExplosion(zombie.getLocation(), world, player, poisonDamage * 0.5, plugin);
                    zombie.remove();

                    // Nettoyer la traînée de poison si pas étoiles max
                    if (!leaveTrail && !trailLocations.isEmpty()) {
                        // La traînée disparaît automatiquement
                    }

                    cancel();
                    return;
                }

                ticksAlive++;
                Location zombieLoc = zombie.getLocation();

                // Traînée de poison
                if (ticksAlive % 5 == 0) { // Toutes les 0.25s
                    spawnPoisonCloud(zombieLoc, world);
                    if (leaveTrail) {
                        trailLocations.add(zombieLoc.clone());
                        // Dégâts de traînée persistante
                        damageNearbyEnemies(player, zombieLoc, poisonDamage * 0.1, 2.0, hitEntities, plugin, true);
                    }
                }

                // Particules constantes
                if (ticksAlive % 2 == 0) {
                    world.spawnParticle(Particle.WITCH, zombieLoc.add(0, 0.5, 0),
                        3, 0.2, 0.3, 0.2, 0.02);
                    world.spawnParticle(Particle.ITEM_SLIME, zombieLoc,
                        2, 0.1, 0.1, 0.1, 0.05);
                }

                // Trouver une cible
                if (currentTarget == null || !currentTarget.isValid() || currentTarget.isDead()
                    || currentTarget.getLocation().distanceSquared(zombieLoc) > 400) { // 20 blocs
                    currentTarget = findNearestEnemy(zombieLoc, world, 15);
                }

                // Se déplacer vers la cible
                if (currentTarget != null) {
                    moveTowardsTarget(zombie, currentTarget.getLocation(), 0.25);

                    // Vérifier collision pour explosion
                    if (zombieLoc.distanceSquared(currentTarget.getLocation()) < 4) { // 2 blocs
                        // Explosion de poison!
                        explodePoison(zombieLoc, world, player, poisonDamage, hitEntities, plugin);
                        explosionCount++;

                        if (explosionCount >= maxExplosions) {
                            // Décomposition finale
                            world.playSound(zombieLoc, Sound.ENTITY_ZOMBIE_DEATH, 1.0f, 0.5f);
                            finalExplosion(zombieLoc, world, player, poisonDamage * 0.5, plugin);
                            zombie.remove();
                            cancel();
                            return;
                        }

                        // Téléporter le zombie plus loin pour chercher une nouvelle cible
                        currentTarget = null;
                        hitEntities.clear(); // Reset pour la prochaine explosion
                    }
                } else {
                    // Pas de cible, errer autour du joueur
                    if (player.isOnline() && player.getWorld().equals(world)) {
                        Location playerLoc = player.getLocation();
                        if (zombieLoc.distanceSquared(playerLoc) > 100) { // Plus de 10 blocs
                            moveTowardsTarget(zombie, playerLoc, 0.2);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Déplace le zombie vers une cible
     */
    private void moveTowardsTarget(Zombie zombie, Location target, double speed) {
        Location from = zombie.getLocation();
        org.bukkit.util.Vector direction = target.toVector().subtract(from.toVector()).normalize();

        Location newLoc = from.add(direction.multiply(speed));
        newLoc.setY(from.getWorld().getHighestBlockYAt(newLoc) + 1);

        // Orienter le zombie vers la cible
        newLoc.setDirection(direction);
        zombie.teleport(newLoc);
    }

    /**
     * Trouve l'ennemi le plus proche
     */
    private LivingEntity findNearestEnemy(Location center, World world, double radius) {
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

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
     * Explosion de poison
     */
    private void explodePoison(Location center, World world, Player player, double damage,
                                Set<UUID> hitEntities, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        // Sons
        world.playSound(center, Sound.ENTITY_SPLASH_POTION_BREAK, 1.5f, 0.8f);
        world.playSound(center, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.2f);

        // Particules d'explosion
        world.spawnParticle(Particle.WITCH, center, 50, 1.5, 1, 1.5, 0.1);
        world.spawnParticle(Particle.ITEM_SLIME, center, 30, 1, 0.5, 1, 0.2);
        world.spawnParticle(Particle.EFFECT, center, 40, 1.5, 1, 1.5, 0);

        // Dégâts
        damageNearbyEnemies(player, center, damage, explosionRadius, hitEntities, plugin, false);

        player.sendMessage("§5[Pet] §d💥 §7Explosion poison! §c" + String.format("%.0f", damage) + " §7dégâts!");
    }

    /**
     * Explosion finale de décomposition
     */
    private void finalExplosion(Location center, World world, Player player, double damage,
                                 com.rinaorc.zombiez.ZombieZPlugin plugin) {
        // Sons dramatiques
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        world.playSound(center, Sound.ENTITY_ZOMBIE_DEATH, 1.5f, 0.3f);

        // Particules de décomposition
        world.spawnParticle(Particle.WITCH, center, 80, 2, 1.5, 2, 0.1);
        world.spawnParticle(Particle.EFFECT, center, 60, 2.5, 1.5, 2.5, 0);
        world.spawnParticle(Particle.SOUL, center, 20, 1, 1, 1, 0.05);
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);

        // Dégâts finaux dans un rayon plus large
        Set<UUID> hitEntities = new HashSet<>();
        damageNearbyEnemies(player, center, damage, explosionRadius * 1.5, hitEntities, plugin, false);
    }

    /**
     * Inflige des dégâts aux ennemis proches
     */
    private void damageNearbyEnemies(Player player, Location center, double damage, double radius,
                                      Set<UUID> hitEntities, com.rinaorc.zombiez.ZombieZPlugin plugin,
                                      boolean allowRepeatedHits) {
        World world = center.getWorld();
        if (world == null) return;

        var zombieManager = plugin.getZombieManager();

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                UUID entityId = entity.getUniqueId();
                if (!allowRepeatedHits && hitEntities.contains(entityId)) continue;

                hitEntities.add(entityId);

                // Infliger les dégâts de poison
                if (zombieManager != null) {
                    var activeZombie = zombieManager.getActiveZombie(entityId);
                    if (activeZombie != null) {
                        zombieManager.damageZombie(player, activeZombie, damage,
                            com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                    } else {
                        living.damage(damage, player);
                    }
                } else {
                    living.damage(damage, player);
                }

                // Effet de poison visuel
                living.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1)); // Poison II 3s

                // Particules sur la cible
                world.spawnParticle(Particle.WITCH, living.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    /**
     * Spawn un nuage de poison
     */
    private void spawnPoisonCloud(Location loc, World world) {
        world.spawnParticle(Particle.WITCH, loc, 5, 0.3, 0.2, 0.3, 0.01);
        world.spawnParticle(Particle.EFFECT, loc, 3, 0.2, 0.1, 0.2, 0);
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== VENGEFUL PILLAGER SYSTEM (Pillard Vengeur) ====================

/**
 * Tir à Distance - Augmente les dégâts à l'arc et l'arbalète
 * +30% (base), +40% (niveau 5+), perce armure (étoiles max)
 */
@Getter
class RangedDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseDamageBonus;       // 30% = 0.30

    // Tracking pour éviter double application
    private static final Set<UUID> activeBuffs = ConcurrentHashMap.newKeySet();

    public RangedDamagePassive(String id, String name, String desc, double damageBonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseDamageBonus = damageBonus;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Récupère le bonus de dégâts selon le niveau
     * Base: 30%, Niveau 5+: 40%
     */
    public double getEffectiveDamageBonus(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.40; // 40%
        }
        return baseDamageBonus; // 30%
    }

    /**
     * Vérifie si l'arme est un arc ou une arbalète
     */
    public boolean isRangedWeapon(org.bukkit.inventory.ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.BOW || type == Material.CROSSBOW;
    }

    /**
     * Vérifie si les attaques percent l'armure (étoiles max)
     */
    public boolean hasArmorPiercing(PetData petData) {
        return petData.getStarPower() > 0;
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        // Ce passif est appliqué dans le système de combat via getEffectiveDamageBonus()
        // Le check d'arme à distance est fait ici pour le feedback visuel

        if (!isRangedWeapon(player.getInventory().getItemInMainHand())) return;

        // Effet visuel de tir puissant
        Location targetLoc = target.getLocation().add(0, 1, 0);
        World world = target.getWorld();

        // Particules de flèche améliorée
        world.spawnParticle(Particle.CRIT, targetLoc, 5, 0.2, 0.3, 0.2, 0.1);

        // Son de tir puissant
        if (Math.random() < 0.3) { // 30% chance
            world.playSound(targetLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.2f);
        }

        // Effet de perce-armure si étoiles max
        if (hasArmorPiercing(petData)) {
            world.spawnParticle(Particle.ENCHANT, targetLoc, 8, 0.3, 0.4, 0.3, 0.5);
        }
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
}

/**
 * Volée de Flèches - Tire une pluie de flèches massive
 * 360% des dégâts de l'arme à tous les ennemis dans la zone
 */
@Getter
class ArrowVolleyActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final RangedDamagePassive linkedPassive;
    private final double damagePercent;         // 360% = 3.60
    private final double volleyRadius;          // Rayon de la volée
    private final int arrowCount;               // Nombre de flèches visuelles

    public ArrowVolleyActive(String id, String name, String desc, RangedDamagePassive passive,
                              double dmgPercent, double radius, int arrows) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damagePercent = dmgPercent;
        this.volleyRadius = radius;
        this.arrowCount = arrows;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    /**
     * Récupère le nombre de ricochets (étoiles max)
     */
    private int getBounceCount(PetData petData) {
        if (petData.getStarPower() > 0) {
            return 3; // 3 ricochets supplémentaires
        }
        return 0;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        Location targetLoc = player.getTargetBlockExact(30) != null
            ? player.getTargetBlockExact(30).getLocation()
            : playerLoc.add(player.getLocation().getDirection().multiply(15));

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercentBonus = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercentBonus);
        double volleyDamage = baseDamage * damagePercent * petData.getStatMultiplier();

        int bounces = getBounceCount(petData);
        boolean hasArmorPiercing = linkedPassive != null && linkedPassive.hasArmorPiercing(petData);

        // Son d'activation
        world.playSound(playerLoc, Sound.ENTITY_PILLAGER_CELEBRATE, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.ITEM_CROSSBOW_SHOOT, 2.0f, 0.6f);

        // Message
        player.sendMessage("§6[Pet] §e🏹 §7Volée de Flèches! §c" +
            String.format("%.0f", volleyDamage) + " §7dégâts!");

        // Lancer la pluie de flèches
        launchArrowRain(player, targetLoc, volleyDamage, bounces, hasArmorPiercing, plugin);
        return true;
    }

    /**
     * Lance la pluie de flèches depuis le ciel
     */
    private void launchArrowRain(Player player, Location center, double damage,
                                  int bounces, boolean armorPiercing,
                                  com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        // Créer les flèches qui tombent du ciel
        Location skyCenter = center.clone().add(0, 15, 0);

        new BukkitRunnable() {
            int wave = 0;
            final int totalWaves = 3;
            final Random random = new Random();

            @Override
            public void run() {
                if (wave >= totalWaves) {
                    cancel();
                    return;
                }

                // Sons de flèches
                world.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.5f, 0.8f + random.nextFloat() * 0.4f);

                // Spawn des flèches visuelles
                int arrowsThisWave = arrowCount / totalWaves;
                for (int i = 0; i < arrowsThisWave; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * volleyRadius * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * volleyRadius * 2;

                    Location arrowStart = skyCenter.clone().add(offsetX, random.nextDouble() * 3, offsetZ);
                    Location arrowEnd = center.clone().add(offsetX, 0, offsetZ);

                    // Animation de flèche
                    spawnArrowTrail(arrowStart, arrowEnd, world);
                }

                // Dégâts à la zone (une seule fois par vague)
                if (wave == 1) { // Vague du milieu = impact principal
                    damageEnemiesInZone(player, center, damage, bounces, armorPiercing, plugin);
                }

                wave++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // Une vague toutes les 0.25s
    }

    /**
     * Crée une traînée de flèche animée
     */
    private void spawnArrowTrail(Location start, Location end, World world) {
        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        new BukkitRunnable() {
            double traveled = 0;
            final double speed = 2.0;
            Location current = start.clone();

            @Override
            public void run() {
                if (traveled >= distance) {
                    // Impact
                    world.spawnParticle(Particle.CRIT, current, 10, 0.3, 0.1, 0.3, 0.1);
                    world.spawnParticle(Particle.BLOCK, current, 5, 0.2, 0.1, 0.2, 0,
                        Material.DIRT.createBlockData());
                    world.playSound(current, Sound.ENTITY_ARROW_HIT, 0.5f, 1.0f);
                    cancel();
                    return;
                }

                // Déplacer
                current.add(direction.clone().multiply(speed));
                traveled += speed;

                // Particules de traînée
                world.spawnParticle(Particle.CRIT, current, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Inflige des dégâts à tous les ennemis dans la zone
     */
    private void damageEnemiesInZone(Player player, Location center, double damage,
                                      int bounces, boolean armorPiercing,
                                      com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        var zombieManager = plugin.getZombieManager();
        Set<UUID> hitEntities = new HashSet<>();
        List<LivingEntity> hitTargets = new ArrayList<>();

        // Première passe: tous les ennemis dans la zone
        for (Entity entity : world.getNearbyEntities(center, volleyRadius, volleyRadius, volleyRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(center);
                if (distSq > volleyRadius * volleyRadius) continue;

                hitEntities.add(entity.getUniqueId());
                hitTargets.add(living);

                // Appliquer les dégâts
                double finalDamage = damage;

                // Bonus perce-armure (ignore 30% de la défense)
                if (armorPiercing) {
                    finalDamage *= 1.15; // +15% dégâts effectifs
                }

                dealDamage(player, living, finalDamage, zombieManager);

                // Effet visuel
                Location targetLoc = living.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.CRIT, targetLoc, 15, 0.3, 0.5, 0.3, 0.1);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 5, 0.2, 0.3, 0.2, 0.1);

                // Flèche plantée dans la cible
                world.playSound(targetLoc, Sound.ENTITY_ARROW_HIT, 1.0f, 0.9f);
            }
        }

        // Ricochets (étoiles max)
        if (bounces > 0 && !hitTargets.isEmpty()) {
            performBounces(player, hitTargets, damage * 0.5, bounces, hitEntities, zombieManager, world);
        }

        // Message de résultat
        if (!hitTargets.isEmpty()) {
            player.sendMessage("§6[Pet] §e⚔ §7Touché §e" + hitTargets.size() +
                " §7ennemis!" + (bounces > 0 ? " §8(+" + bounces + " ricochets)" : ""));
        }
    }

    /**
     * Effectue les ricochets vers des ennemis supplémentaires
     */
    private void performBounces(Player player, List<LivingEntity> initialTargets, double bounceDamage,
                                 int bounceCount, Set<UUID> hitEntities,
                                 com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {

        new BukkitRunnable() {
            int remainingBounces = bounceCount;
            List<LivingEntity> currentTargets = new ArrayList<>(initialTargets);

            @Override
            public void run() {
                if (remainingBounces <= 0 || currentTargets.isEmpty()) {
                    cancel();
                    return;
                }

                List<LivingEntity> nextTargets = new ArrayList<>();

                for (LivingEntity source : currentTargets) {
                    if (!source.isValid() || source.isDead()) continue;

                    // Trouver un nouvel ennemi proche
                    LivingEntity bounceTarget = findNearestUnhitEnemy(source.getLocation(), 8, hitEntities, world);
                    if (bounceTarget != null) {
                        hitEntities.add(bounceTarget.getUniqueId());
                        nextTargets.add(bounceTarget);

                        // Traînée visuelle du ricochet
                        spawnBounceTrail(source.getLocation().add(0, 1, 0),
                            bounceTarget.getLocation().add(0, 1, 0), world);

                        // Dégâts
                        dealDamage(player, bounceTarget, bounceDamage, zombieManager);

                        // Effet
                        Location targetLoc = bounceTarget.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.CRIT, targetLoc, 8, 0.2, 0.3, 0.2, 0.1);
                        world.playSound(targetLoc, Sound.ENTITY_ARROW_HIT, 0.8f, 1.2f);
                    }
                }

                currentTargets = nextTargets;
                remainingBounces--;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 5L, 5L);
    }

    /**
     * Trouve l'ennemi le plus proche non encore touché
     */
    private LivingEntity findNearestUnhitEnemy(Location center, double radius,
                                                Set<UUID> hitEntities, World world) {
        LivingEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()
                && !hitEntities.contains(entity.getUniqueId())) {

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
     * Crée une traînée visuelle de ricochet
     */
    private void spawnBounceTrail(Location from, Location to, World world) {
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        int steps = (int) (distance * 2);
        for (int i = 0; i < steps; i++) {
            Location particleLoc = from.clone().add(direction.clone().multiply(i / 2.0));
            world.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                             com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== INFERNAL CUBE SYSTEM (Cube Infernal) ====================

/**
 * Pyromanie - Augmente les dégâts contre les mobs en feu
 * +40% (base), +50% (niveau 5+), auto-enflamme (étoiles max)
 */
@Getter
class FireDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseDamageBonus;       // 40% = 0.40

    public FireDamagePassive(String id, String name, String desc, double damageBonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseDamageBonus = damageBonus;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Récupère le bonus de dégâts selon le niveau
     * Base: 40%, Niveau 5+: 50%
     */
    public double getEffectiveDamageBonus(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.50; // 50%
        }
        return baseDamageBonus; // 40%
    }

    /**
     * Vérifie si les attaques enflamment automatiquement (étoiles max)
     */
    public boolean hasAutoIgnite(PetData petData) {
        return petData.getStarPower() > 0;
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        World world = target.getWorld();
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Auto-enflamme si étoiles max
        if (hasAutoIgnite(petData) && target.getFireTicks() <= 0) {
            target.setFireTicks(60); // 3 secondes de feu

            // Effet visuel d'ignition
            world.spawnParticle(Particle.FLAME, targetLoc, 15, 0.3, 0.5, 0.3, 0.05);
            world.playSound(targetLoc, Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.2f);
        }

        // Effet visuel si la cible est en feu
        if (target.getFireTicks() > 0) {
            world.spawnParticle(Particle.LAVA, targetLoc, 3, 0.2, 0.3, 0.2, 0);

            // Son occasionnel
            if (Math.random() < 0.2) {
                world.playSound(targetLoc, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.0f);
            }
        }
    }

    /**
     * Vérifie si la cible est en feu et retourne le bonus applicable
     */
    public double getBonusForTarget(LivingEntity target, PetData petData) {
        if (target.getFireTicks() > 0) {
            return getEffectiveDamageBonus(petData);
        }
        return 0.0;
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
}

/**
 * Frappe Météoritique - Invoque un météore géant
 * 450% dégâts feu + zone enflammée (120% sur 3s)
 */
@Getter
class MeteorStrikeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final FireDamagePassive linkedPassive;
    private final double impactDamagePercent;    // 450% = 4.50
    private final double groundFirePercent;      // 120% = 1.20 sur 3s
    private final double impactRadius;           // Rayon d'impact

    public MeteorStrikeActive(String id, String name, String desc, FireDamagePassive passive,
                               double impactDmg, double groundFireDmg, double radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.impactDamagePercent = impactDmg;
        this.groundFirePercent = groundFireDmg;
        this.impactRadius = radius;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 35; }

    /**
     * Vérifie si le météore crée un lac de lave (étoiles max)
     */
    private boolean hasLavaLake(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Calculer le point d'impact (devant le joueur ou bloc visé)
        Location targetLoc = player.getTargetBlockExact(30) != null
            ? player.getTargetBlockExact(30).getLocation().add(0, 1, 0)
            : playerLoc.add(player.getLocation().getDirection().multiply(10));

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercentBonus = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercentBonus);
        double impactDamage = baseDamage * impactDamagePercent * petData.getStatMultiplier();
        double groundFireDamage = baseDamage * groundFirePercent * petData.getStatMultiplier();

        boolean createLavaLake = hasLavaLake(petData);
        double lavaDamagePerSecond = baseDamage * 0.50 * petData.getStatMultiplier(); // 50% par seconde

        // Son d'avertissement
        world.playSound(targetLoc, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);
        world.playSound(playerLoc, Sound.ENTITY_MAGMA_CUBE_JUMP, 1.5f, 0.8f);

        // Message
        player.sendMessage("§c[Pet] §6☄ §7Frappe Météoritique! §c" +
            String.format("%.0f", impactDamage) + " §7dégâts feu!");

        // Lancer le météore
        launchMeteor(player, targetLoc, impactDamage, groundFireDamage, createLavaLake, lavaDamagePerSecond, plugin);
        return true;
    }

    /**
     * Lance le météore depuis le ciel
     */
    private void launchMeteor(Player player, Location target, double impactDamage, double groundFireDamage,
                               boolean createLavaLake, double lavaDamage,
                               com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = target.getWorld();
        if (world == null) return;

        // Position de départ du météore (haut dans le ciel)
        Location meteorStart = target.clone().add(0, 25, 0);
        final Location impactPoint = target.clone();

        // Animation du météore qui tombe
        new BukkitRunnable() {
            Location current = meteorStart.clone();
            final org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(0, -1.5, 0);
            int ticks = 0;

            @Override
            public void run() {
                // Vérifier si on a atteint le sol
                if (current.getY() <= impactPoint.getY() || ticks > 40) {
                    // IMPACT!
                    performImpact(player, impactPoint, impactDamage, groundFireDamage,
                        createLavaLake, lavaDamage, plugin);
                    cancel();
                    return;
                }

                // Déplacer le météore
                current.add(velocity);
                ticks++;

                // Particules du météore
                world.spawnParticle(Particle.FLAME, current, 30, 0.8, 0.8, 0.8, 0.1);
                world.spawnParticle(Particle.LAVA, current, 10, 0.5, 0.5, 0.5, 0);
                world.spawnParticle(Particle.SMOKE, current, 15, 0.6, 0.6, 0.6, 0.05);

                // Traînée de feu
                for (int i = 1; i <= 3; i++) {
                    Location trailLoc = current.clone().add(0, i * 0.8, 0);
                    world.spawnParticle(Particle.FLAME, trailLoc, 8, 0.3, 0.3, 0.3, 0.02);
                }

                // Son de chute
                if (ticks % 5 == 0) {
                    world.playSound(current, Sound.ENTITY_BLAZE_BURN, 1.0f, 0.5f);
                }

                // Indicateur au sol
                world.spawnParticle(Particle.FLAME, impactPoint, 5, impactRadius * 0.5, 0.1, impactRadius * 0.5, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Effectue l'impact du météore
     */
    private void performImpact(Player player, Location center, double impactDamage, double groundFireDamage,
                                boolean createLavaLake, double lavaDamage,
                                com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        // Sons d'explosion
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
        world.playSound(center, Sound.ENTITY_BLAZE_DEATH, 1.5f, 0.5f);
        world.playSound(center, Sound.BLOCK_LAVA_POP, 2.0f, 0.8f);

        // Explosion visuelle massive
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.FLAME, center, 100, impactRadius, 1.5, impactRadius, 0.2);
        world.spawnParticle(Particle.LAVA, center, 50, impactRadius * 0.8, 1, impactRadius * 0.8, 0);
        world.spawnParticle(Particle.SMOKE, center, 60, impactRadius, 2, impactRadius, 0.1);

        // Onde de choc visuelle
        spawnShockwave(center, world);

        // Infliger les dégâts d'impact
        Set<UUID> hitEntities = new HashSet<>();
        var zombieManager = plugin.getZombieManager();
        int hitCount = 0;

        for (Entity entity : world.getNearbyEntities(center, impactRadius, impactRadius, impactRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distSq = entity.getLocation().distanceSquared(center);
                if (distSq > impactRadius * impactRadius) continue;

                hitEntities.add(entity.getUniqueId());
                hitCount++;

                // Enflammer la cible
                living.setFireTicks(100); // 5 secondes

                // Dégâts d'impact
                dealDamage(player, living, impactDamage, zombieManager);

                // Knockback depuis le centre
                org.bukkit.util.Vector knockback = living.getLocation().toVector()
                    .subtract(center.toVector()).normalize().multiply(1.5);
                knockback.setY(0.5);
                living.setVelocity(knockback);

                // Effet sur la cible
                Location targetLoc = living.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.FLAME, targetLoc, 20, 0.3, 0.5, 0.3, 0.1);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 8, 0.2, 0.3, 0.2, 0.1);
            }
        }

        // Message de résultat
        if (hitCount > 0) {
            player.sendMessage("§c[Pet] §6⚔ §7Impact! §e" + hitCount + " §7ennemis touchés + enflammés!");
        }

        // Zone enflammée au sol
        startGroundFire(player, center, groundFireDamage, hitEntities, plugin);

        // Lac de lave si étoiles max
        if (createLavaLake) {
            startLavaLake(player, center, lavaDamage, plugin);
        }
    }

    /**
     * Crée l'onde de choc visuelle
     */
    private void spawnShockwave(Location center, World world) {
        new BukkitRunnable() {
            double radius = 0;

            @Override
            public void run() {
                if (radius >= impactRadius * 1.5) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    Location particleLoc = new Location(world, x, center.getY() + 0.2, z);

                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.05, 0.1, 0.02);
                }

                radius += 1.0;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Zone de flammes au sol (120% sur 3s = 40% par seconde)
     */
    private void startGroundFire(Player player, Location center, double totalDamage,
                                  Set<UUID> alreadyHit, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        double damagePerTick = totalDamage / 60.0; // 3 secondes = 60 ticks
        var zombieManager = plugin.getZombieManager();

        new BukkitRunnable() {
            int ticksRemaining = 60; // 3 secondes

            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }

                // Particules de flammes au sol
                if (ticksRemaining % 3 == 0) {
                    for (double angle = 0; angle < 360; angle += 30) {
                        double rad = Math.toRadians(angle);
                        double r = Math.random() * impactRadius;
                        double x = center.getX() + r * Math.cos(rad);
                        double z = center.getZ() + r * Math.sin(rad);
                        Location flameLoc = new Location(world, x, center.getY() + 0.1, z);

                        world.spawnParticle(Particle.FLAME, flameLoc, 2, 0.1, 0.2, 0.1, 0.02);
                    }
                }

                // Dégâts toutes les 10 ticks (0.5s)
                if (ticksRemaining % 10 == 0) {
                    for (Entity entity : world.getNearbyEntities(center, impactRadius, 2, impactRadius)) {
                        if (entity instanceof LivingEntity living && !(entity instanceof Player)
                            && !(entity instanceof ArmorStand) && !living.isDead()) {

                            double distSq = entity.getLocation().distanceSquared(center);
                            if (distSq > impactRadius * impactRadius) continue;

                            // Maintenir en feu
                            if (living.getFireTicks() < 40) {
                                living.setFireTicks(40);
                            }

                            // Dégâts de brûlure
                            dealDamage(player, living, damagePerTick * 10, zombieManager);
                        }
                    }
                }

                // Son de feu
                if (ticksRemaining % 20 == 0) {
                    world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.0f);
                }

                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Lac de lave permanent (étoiles max) - 10 secondes, 50% dégâts/s
     */
    private void startLavaLake(Player player, Location center, double damagePerSecond,
                                com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = center.getWorld();
        if (world == null) return;

        var zombieManager = plugin.getZombieManager();
        double lakeRadius = impactRadius * 0.8;

        new BukkitRunnable() {
            int ticksRemaining = 200; // 10 secondes

            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }

                // Particules de lave
                if (ticksRemaining % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 360;
                        double rad = Math.toRadians(angle);
                        double r = Math.random() * lakeRadius;
                        double x = center.getX() + r * Math.cos(rad);
                        double z = center.getZ() + r * Math.sin(rad);
                        Location lavaLoc = new Location(world, x, center.getY() + 0.1, z);

                        world.spawnParticle(Particle.LAVA, lavaLoc, 1, 0.1, 0.05, 0.1, 0);
                        world.spawnParticle(Particle.FLAME, lavaLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }

                // Bulles de lave
                if (ticksRemaining % 15 == 0) {
                    double angle = Math.random() * 360;
                    double rad = Math.toRadians(angle);
                    double r = Math.random() * lakeRadius;
                    Location bubbleLoc = center.clone().add(r * Math.cos(rad), 0.2, r * Math.sin(rad));

                    world.spawnParticle(Particle.LAVA, bubbleLoc, 3, 0.1, 0.1, 0.1, 0);
                    world.playSound(bubbleLoc, Sound.BLOCK_LAVA_POP, 0.5f, 1.0f);
                }

                // Dégâts toutes les 20 ticks (1s)
                if (ticksRemaining % 20 == 0) {
                    for (Entity entity : world.getNearbyEntities(center, lakeRadius, 2, lakeRadius)) {
                        if (entity instanceof LivingEntity living && !(entity instanceof Player)
                            && !(entity instanceof ArmorStand) && !living.isDead()) {

                            double distSq = entity.getLocation().distanceSquared(center);
                            if (distSq > lakeRadius * lakeRadius) continue;

                            // Enflammer
                            living.setFireTicks(60);

                            // Dégâts de lave
                            dealDamage(player, living, damagePerSecond, zombieManager);

                            // Effet
                            Location targetLoc = living.getLocation().add(0, 0.5, 0);
                            world.spawnParticle(Particle.LAVA, targetLoc, 5, 0.2, 0.3, 0.2, 0);
                        }
                    }
                }

                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 60L, 1L); // Commence après les flammes initiales
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                             com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.FIRE, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== ENRAGED ZOGLIN SYSTEM (Zoglin Enragé) ====================

/**
 * Instinct de Tueur - Dégâts bonus contre les ennemis à faible vie
 * +40% (base), +50% (niveau 5+) sous 30% HP (35% niveau 5+)
 */
@Getter
class ExecuteDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double baseDamageBonus;       // 40% = 0.40
    private final double baseThreshold;          // 30% = 0.30

    public ExecuteDamagePassive(String id, String name, String desc, double damageBonus, double threshold) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.baseDamageBonus = damageBonus;
        this.baseThreshold = threshold;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Récupère le bonus de dégâts selon le niveau
     * Base: 40%, Niveau 5+: 50%
     */
    public double getEffectiveDamageBonus(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.50; // 50%
        }
        return baseDamageBonus; // 40%
    }

    /**
     * Récupère le seuil de HP selon le niveau
     * Base: 30%, Niveau 5+: 35%
     */
    public double getEffectiveThreshold(PetData petData) {
        if (petData.getStatMultiplier() >= 1.5) { // Niveau 5+
            return 0.35; // 35%
        }
        return baseThreshold; // 30%
    }

    /**
     * Vérifie si la cible est sous le seuil et retourne le bonus
     */
    public double getBonusForTarget(LivingEntity target, PetData petData) {
        double healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent <= getEffectiveThreshold(petData)) {
            return getEffectiveDamageBonus(petData);
        }
        return 0.0;
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        double healthPercent = target.getHealth() / target.getMaxHealth();
        double threshold = getEffectiveThreshold(petData);

        if (healthPercent <= threshold) {
            World world = target.getWorld();
            Location targetLoc = target.getLocation().add(0, 1, 0);

            // Effet visuel d'exécution
            world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 8, 0.3, 0.4, 0.3, 0.1);
            world.spawnParticle(Particle.ANGRY_VILLAGER, targetLoc, 3, 0.2, 0.3, 0.2, 0);

            // Son de rage
            if (Math.random() < 0.25) {
                world.playSound(targetLoc, Sound.ENTITY_ZOGLIN_ANGRY, 0.5f, 1.2f);
            }
        }
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) {
        // Effet de kill satisfaisant
        World world = victim.getWorld();
        Location loc = victim.getLocation().add(0, 1, 0);

        world.spawnParticle(Particle.ANGRY_VILLAGER, loc, 10, 0.5, 0.5, 0.5, 0);
        world.playSound(loc, Sound.ENTITY_ZOGLIN_DEATH, 0.6f, 1.0f);
    }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }
}

/**
 * Charge Dévastatrice - Le Zoglin se précipite et repousse les ennemis
 * 220% dégâts, knockback, cooldown 10s
 */
@Getter
class ZoglinChargeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final ExecuteDamagePassive linkedPassive;
    private final double damagePercent;         // 220% = 2.20
    private final double chargeDistance;        // Distance de charge
    private final double chargeWidth;           // Largeur de la charge

    public ZoglinChargeActive(String id, String name, String desc, ExecuteDamagePassive passive,
                               double dmgPercent, double distance, double width) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.linkedPassive = passive;
        this.damagePercent = dmgPercent;
        this.chargeDistance = distance;
        this.chargeWidth = width;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 10; } // 10 secondes pour proc fréquent

    /**
     * Vérifie si la charge laisse une traînée de feu (étoiles max)
     */
    private boolean hasFireTrail(PetData petData) {
        return petData.getStarPower() > 0;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        World world = player.getWorld();
        Location startLoc = player.getLocation();
        org.bukkit.util.Vector direction = startLoc.getDirection().setY(0).normalize();

        // Calculer les dégâts
        var playerStats = plugin.getItemManager().calculatePlayerStats(player);
        double flatDamage = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercentBonus = playerStats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);
        double baseDamage = (7.0 + flatDamage) * (1.0 + damagePercentBonus);
        double chargeDamage = baseDamage * damagePercent * petData.getStatMultiplier();

        boolean leaveFireTrail = hasFireTrail(petData);
        double fireTrailDamage = baseDamage * 0.50 * petData.getStatMultiplier(); // 50% par seconde

        // Son d'activation
        world.playSound(startLoc, Sound.ENTITY_ZOGLIN_ANGRY, 2.0f, 0.6f);
        world.playSound(startLoc, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);

        // Message
        player.sendMessage("§4[Pet] §c🐗 §7Charge Dévastatrice! §c" +
            String.format("%.0f", chargeDamage) + " §7dégâts!");

        // Lancer la charge
        performCharge(player, startLoc, direction, chargeDamage, leaveFireTrail, fireTrailDamage, plugin);
        return true;
    }

    /**
     * Effectue la charge du Zoglin
     */
    private void performCharge(Player player, Location start, org.bukkit.util.Vector direction,
                                double damage, boolean leaveFireTrail, double fireTrailDamage,
                                com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = start.getWorld();
        if (world == null) return;

        Set<UUID> hitEntities = new HashSet<>();
        List<Location> trailLocations = new ArrayList<>();
        var zombieManager = plugin.getZombieManager();

        new BukkitRunnable() {
            double traveled = 0;
            Location current = start.clone();
            final double speed = 1.5; // Blocs par tick

            @Override
            public void run() {
                if (traveled >= chargeDistance) {
                    // Fin de la charge - explosion finale
                    performImpact(current, world);

                    // Démarrer la traînée de feu si étoiles max
                    if (leaveFireTrail && !trailLocations.isEmpty()) {
                        startFireTrail(player, trailLocations, fireTrailDamage, plugin);
                    }

                    cancel();
                    return;
                }

                // Déplacer
                current.add(direction.clone().multiply(speed));
                traveled += speed;

                // Sauvegarder pour la traînée
                if (leaveFireTrail) {
                    trailLocations.add(current.clone());
                }

                // Particules de charge
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, current, 8, 0.3, 0.2, 0.3, 0.02);
                world.spawnParticle(Particle.CRIT, current, 5, 0.2, 0.1, 0.2, 0.1);

                // Son de course
                if (traveled % 3 < speed) {
                    world.playSound(current, Sound.ENTITY_ZOGLIN_STEP, 1.0f, 0.8f);
                }

                // Détecter les collisions
                for (Entity entity : world.getNearbyEntities(current, chargeWidth, 2, chargeWidth)) {
                    if (entity instanceof LivingEntity living && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand) && !living.isDead()) {

                        UUID entityId = entity.getUniqueId();
                        if (hitEntities.contains(entityId)) continue;

                        hitEntities.add(entityId);

                        // Dégâts
                        dealDamage(player, living, damage, zombieManager);

                        // Knockback puissant
                        org.bukkit.util.Vector knockback = direction.clone().multiply(2.0);
                        knockback.setY(0.6);
                        living.setVelocity(knockback);

                        // Effet visuel
                        Location targetLoc = living.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.DAMAGE_INDICATOR, targetLoc, 10, 0.3, 0.4, 0.3, 0.1);
                        world.spawnParticle(Particle.ANGRY_VILLAGER, targetLoc, 5, 0.2, 0.3, 0.2, 0);
                        world.playSound(targetLoc, Sound.ENTITY_ZOGLIN_ATTACK, 1.0f, 0.9f);
                    }
                }

                // Particules de traînée de feu si étoiles max
                if (leaveFireTrail) {
                    world.spawnParticle(Particle.FLAME, current, 5, 0.2, 0.1, 0.2, 0.03);
                    world.spawnParticle(Particle.LAVA, current, 2, 0.1, 0.05, 0.1, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Impact final de la charge
     */
    private void performImpact(Location loc, World world) {
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        world.playSound(loc, Sound.ENTITY_ZOGLIN_ANGRY, 1.5f, 0.5f);

        world.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, 1, 0.5, 1, 0.05);
        world.spawnParticle(Particle.CRIT, loc, 20, 0.8, 0.3, 0.8, 0.2);
    }

    /**
     * Traînée de feu (étoiles max) - 5 secondes, 50% dégâts/s
     */
    private void startFireTrail(Player player, List<Location> trailLocations, double damagePerSecond,
                                 com.rinaorc.zombiez.ZombieZPlugin plugin) {
        World world = trailLocations.get(0).getWorld();
        if (world == null) return;

        var zombieManager = plugin.getZombieManager();
        Set<UUID> hitThisTick = new HashSet<>();

        new BukkitRunnable() {
            int ticksRemaining = 100; // 5 secondes

            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }

                // Particules de feu sur la traînée
                if (ticksRemaining % 3 == 0) {
                    for (Location loc : trailLocations) {
                        if (Math.random() < 0.3) {
                            world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.2, 0),
                                2, 0.2, 0.1, 0.2, 0.02);
                        }
                    }
                }

                // Dégâts toutes les 20 ticks (1s)
                if (ticksRemaining % 20 == 0) {
                    hitThisTick.clear();

                    for (Location loc : trailLocations) {
                        for (Entity entity : world.getNearbyEntities(loc, 1.5, 2, 1.5)) {
                            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                                && !(entity instanceof ArmorStand) && !living.isDead()) {

                                UUID entityId = entity.getUniqueId();
                                if (hitThisTick.contains(entityId)) continue;
                                hitThisTick.add(entityId);

                                // Enflammer
                                living.setFireTicks(40);

                                // Dégâts
                                dealDamage(player, living, damagePerSecond, zombieManager);

                                // Effet
                                world.spawnParticle(Particle.FLAME, living.getLocation().add(0, 0.5, 0),
                                    5, 0.2, 0.3, 0.2, 0.03);
                            }
                        }
                    }
                }

                // Son de feu
                if (ticksRemaining % 30 == 0 && !trailLocations.isEmpty()) {
                    Location midPoint = trailLocations.get(trailLocations.size() / 2);
                    world.playSound(midPoint, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.0f);
                }

                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                             com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== ARCANE ILLUSIONIST SYSTEM ====================

/**
 * Passif: +30% dégâts aux ennemis au-delà de 15 blocs
 * Niveau 5+: +40% dégâts au-delà de 15 blocs, bonus dès 12 blocs
 */
class SniperDamagePassive extends SynergyAbilities.BasePetAbility {

    private final double baseBonusPercent;
    private final double baseDistanceThreshold;
    private final double level5BonusPercent;
    private final double level5DistanceThreshold;

    public SniperDamagePassive(String id, String name, String description,
                               double baseBonusPercent, double baseDistanceThreshold,
                               double level5BonusPercent, double level5DistanceThreshold) {
        super(id, name, description, false);
        this.baseBonusPercent = baseBonusPercent;
        this.baseDistanceThreshold = baseDistanceThreshold;
        this.level5BonusPercent = level5BonusPercent;
        this.level5DistanceThreshold = level5DistanceThreshold;
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Passif calculé dans onDamageDealt
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        double distance = player.getLocation().distance(target.getLocation());

        boolean isLevel5Plus = petData.getStatMultiplier() >= 1.5;
        double threshold = isLevel5Plus ? level5DistanceThreshold : baseDistanceThreshold;
        double bonusPercent = isLevel5Plus ? level5BonusPercent : baseBonusPercent;

        if (distance >= threshold) {
            // Calculer le bonus basé sur les dégâts de l'arme du joueur
            var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
            var zombieManager = plugin.getZombieManager();

            // Récupérer stats joueur
            Map<com.rinaorc.zombiez.items.types.StatType, Double> stats =
                plugin.getItemManager().calculatePlayerStats(player);

            double flatDamage = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
            double damagePercent = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);

            // Dégâts de base de l'arme
            double weaponDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

            // Bonus de dégâts à distance
            double bonusDamage = weaponDamage * bonusPercent * petData.getStatMultiplier();

            // Appliquer les dégâts bonus
            if (zombieManager != null) {
                var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
                if (activeZombie != null) {
                    zombieManager.damageZombie(player, activeZombie, bonusDamage,
                        com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                }
            } else {
                target.damage(bonusDamage, player);
            }

            // Effet visuel arcanique
            World world = target.getWorld();
            Location targetLoc = target.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.WITCH, targetLoc, 8, 0.3, 0.4, 0.3, 0.02);
            world.spawnParticle(Particle.END_ROD, targetLoc, 3, 0.2, 0.3, 0.2, 0.01);
        }
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

/**
 * Ultimate: Torrent Arcanique - volée de projectiles magiques
 * Dégâts démarrent à 150% et augmentent de 50%/s jusqu'à 400% max
 * Max stars: crée des explosions arcaniques secondaires (80% dégâts)
 */
class ArcaneTorrentActive extends SynergyAbilities.BasePetAbility {

    private final SniperDamagePassive linkedPassive;
    private final double initialDamagePercent;
    private final double damageIncreasePerSecond;
    private final double maxDamagePercent;
    private final double secondaryExplosionPercent;

    public ArcaneTorrentActive(String id, String name, String description,
                               SniperDamagePassive linkedPassive,
                               double initialDamagePercent, double damageIncreasePerSecond,
                               double maxDamagePercent, double secondaryExplosionPercent) {
        super(id, name, description, true);
        this.linkedPassive = linkedPassive;
        this.initialDamagePercent = initialDamagePercent;
        this.damageIncreasePerSecond = damageIncreasePerSecond;
        this.maxDamagePercent = maxDamagePercent;
        this.secondaryExplosionPercent = secondaryExplosionPercent;
    }

    @Override
    protected void doActivate(Player player, PetData petData) {
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Récupérer stats joueur
        Map<com.rinaorc.zombiez.items.types.StatType, Double> stats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);

        // Dégâts de base de l'arme
        double weaponDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        boolean hasMaxStars = petData.getStarPower() > 0;
        double statMultiplier = petData.getStatMultiplier();

        // Son d'invocation
        world.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 0.8f);
        world.playSound(playerLoc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);

        // Effet de lancement
        world.spawnParticle(Particle.WITCH, playerLoc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.END_ROD, playerLoc.clone().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);

        // Collecter les cibles initiales
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(playerLoc, 12, 6, 12)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§5§l✦ §dTorrent Arcanique: §7Aucune cible à proximité!");
            return;
        }

        player.sendMessage("§5§l✦ §dTorrent Arcanique §7activé! §e" + targets.size() + " §7cibles détectées.");

        // Projectiles qui frappent au fil du temps avec dégâts croissants
        // Durée: 5 secondes (100 ticks), 1 vague de projectiles par seconde
        final int DURATION_TICKS = 100;
        final int WAVES = 5;

        new BukkitRunnable() {
            int ticksElapsed = 0;
            int wavesFired = 0;

            @Override
            public void run() {
                if (ticksElapsed >= DURATION_TICKS || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Tirer une vague tous les 20 ticks (1 seconde)
                if (ticksElapsed % 20 == 0 && wavesFired < WAVES) {
                    // Calculer les dégâts pour cette vague (augmentent avec le temps)
                    double secondsElapsed = ticksElapsed / 20.0;
                    double currentDamagePercent = Math.min(
                        initialDamagePercent + (damageIncreasePerSecond * secondsElapsed),
                        maxDamagePercent
                    );

                    double waveDamage = weaponDamage * currentDamagePercent * statMultiplier;

                    // Lancer des projectiles vers les cibles
                    Location origin = player.getLocation().add(0, 1.5, 0);

                    // Effet de lancement de vague
                    world.playSound(origin, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.7f, 1.2f + (wavesFired * 0.1f));
                    world.spawnParticle(Particle.REVERSE_PORTAL, origin, 20, 0.3, 0.3, 0.3, 0.05);

                    // Tirer vers chaque cible (ou leurs dernières positions connues)
                    for (LivingEntity target : new ArrayList<>(targets)) {
                        if (target.isDead() || !target.isValid()) {
                            targets.remove(target);
                            continue;
                        }

                        // Créer le projectile arcanique (visuel)
                        Location targetLoc = target.getLocation().add(0, 1, 0);
                        Vector direction = targetLoc.toVector().subtract(origin.toVector()).normalize();

                        // Animation du projectile
                        fireArcaneProjectile(player, origin.clone(), targetLoc, waveDamage,
                            hasMaxStars, weaponDamage * secondaryExplosionPercent * statMultiplier,
                            zombieManager, world);
                    }

                    wavesFired++;

                    // Message de progression
                    int percentDamage = (int) (currentDamagePercent * 100);
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§5✦ Torrent Arcanique: §d" + percentDamage + "% §7dégâts"));
                }

                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void fireArcaneProjectile(Player player, Location origin, Location targetLoc, double damage,
                                      boolean hasMaxStars, double secondaryDamage,
                                      com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();

        Vector direction = targetLoc.toVector().subtract(origin.toVector());
        double distance = direction.length();
        direction.normalize();

        // Vitesse du projectile
        final double SPEED = 1.5;
        final int maxTicks = (int) (distance / SPEED) + 10;

        new BukkitRunnable() {
            Location current = origin.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Avancer le projectile
                current.add(direction.clone().multiply(SPEED));

                // Particules de traînée
                world.spawnParticle(Particle.WITCH, current, 3, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticle(Particle.DRAGON_BREATH, current, 2, 0.05, 0.05, 0.05, 0.01);

                // Vérifier les collisions
                for (Entity entity : world.getNearbyEntities(current, 1.2, 1.2, 1.2)) {
                    if (entity instanceof LivingEntity living && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand) && !living.isDead()) {

                        // Impact principal
                        dealDamage(player, living, damage, zombieManager);

                        // Effet d'impact
                        world.spawnParticle(Particle.WITCH, current, 20, 0.4, 0.4, 0.4, 0.1);
                        world.spawnParticle(Particle.REVERSE_PORTAL, current, 15, 0.3, 0.3, 0.3, 0.05);
                        world.playSound(current, Sound.ENTITY_ILLUSIONER_HURT, 0.8f, 1.5f);

                        // Explosion secondaire si max stars
                        if (hasMaxStars) {
                            createSecondaryExplosion(player, current, secondaryDamage, zombieManager, world);
                        }

                        cancel();
                        return;
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createSecondaryExplosion(Player player, Location center, double damage,
                                          com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {
        // Explosion arcanique secondaire
        world.spawnParticle(Particle.WITCH, center, 40, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.END_ROD, center, 20, 1.5, 0.5, 1.5, 0.05);
        world.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.3f);

        Set<UUID> hit = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, 3, 2, 3)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (hit.add(entity.getUniqueId())) {
                    dealDamage(player, living, damage, zombieManager);
                }
            }
        }
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                            com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
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
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== DEATH AVATAR SYSTEM ====================

/**
 * Ultimate: Jugement Final - Frappe dévastatrice du Vindicator
 * Dégâts: 200% arme + 100% bonus basé sur HP manquants de la cible
 * Max stars: Onde de mort (150% dégâts AoE)
 */
class FinalJudgmentActive extends SynergyAbilities.BasePetAbility {

    private final double baseDamagePercent;
    private final double missingHpBonusPercent;
    private final double deathWaveDamagePercent;
    private final double waveRadius;

    public FinalJudgmentActive(String id, String name, String description,
                               double baseDamagePercent, double missingHpBonusPercent,
                               double deathWaveDamagePercent, double waveRadius) {
        super(id, name, description, true);
        this.baseDamagePercent = baseDamagePercent;
        this.missingHpBonusPercent = missingHpBonusPercent;
        this.deathWaveDamagePercent = deathWaveDamagePercent;
        this.waveRadius = waveRadius;
    }

    @Override
    protected void doActivate(Player player, PetData petData) {
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Récupérer stats joueur
        Map<com.rinaorc.zombiez.items.types.StatType, Double> stats =
            plugin.getItemManager().calculatePlayerStats(player);

        double flatDamage = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE, 0.0);
        double damagePercent = stats.getOrDefault(com.rinaorc.zombiez.items.types.StatType.DAMAGE_PERCENT, 0.0);

        // Dégâts de base de l'arme
        double weaponDamage = (7.0 + flatDamage) * (1.0 + damagePercent);

        boolean hasMaxStars = petData.getStarPower() > 0;
        double statMultiplier = petData.getStatMultiplier();

        // Son de préparation - Vindicator
        world.playSound(playerLoc, Sound.ENTITY_VINDICATOR_CELEBRATE, 1.2f, 0.6f);
        world.playSound(playerLoc, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 0.5f);

        // Effet de charge - aura mortelle
        world.spawnParticle(Particle.SMOKE, playerLoc.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.05);
        world.spawnParticle(Particle.SOUL, playerLoc.clone().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);

        // Trouver la cible la plus proche (priorité aux faibles HP)
        LivingEntity primaryTarget = null;
        double lowestHpPercent = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(playerLoc, 8, 4, 8)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double hpPercent = living.getHealth() / living.getMaxHealth();
                if (hpPercent < lowestHpPercent) {
                    lowestHpPercent = hpPercent;
                    primaryTarget = living;
                }
            }
        }

        if (primaryTarget == null) {
            player.sendMessage("§4§l☠ §cJugement Final: §7Aucune cible à proximité!");
            return;
        }

        final LivingEntity target = primaryTarget;
        Location targetLoc = target.getLocation();

        // Animation de dash vers la cible
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
        double distance = playerLoc.distance(targetLoc);

        // Téléporter le joueur vers la cible (effet de rush)
        Location strikePos = targetLoc.clone().subtract(direction.clone().multiply(1.5));
        strikePos.setDirection(direction);
        player.teleport(strikePos);

        // Délai court puis frappe
        new BukkitRunnable() {
            @Override
            public void run() {
                // Calculer les dégâts
                double baseDamage = weaponDamage * baseDamagePercent * statMultiplier;

                // Bonus basé sur HP manquants (% de HP max manquants × bonus)
                double targetHpPercent = target.getHealth() / target.getMaxHealth();
                double missingHpPercent = 1.0 - targetHpPercent;
                double missingHpBonus = weaponDamage * missingHpBonusPercent * missingHpPercent * statMultiplier;

                double totalDamage = baseDamage + missingHpBonus;

                // Effet de frappe
                world.playSound(target.getLocation(), Sound.ENTITY_VINDICATOR_HURT, 1.5f, 0.5f);
                world.playSound(target.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.2f, 0.8f);

                // Particules d'impact
                world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.3);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.1);
                world.spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.5, 0.6, 0.5, 0.05);

                // Infliger les dégâts à la cible principale
                dealDamage(player, target, totalDamage, zombieManager);

                // Message avec dégâts
                int displayDamage = (int) totalDamage;
                int bonusPercent = (int) (missingHpPercent * 100);
                player.sendMessage("§4§l☠ §cJugement Final §7sur " + target.getName() +
                    " §7(§c" + displayDamage + " §7dégâts, §6+" + bonusPercent + "% §7bonus HP manquants)");

                // Onde de mort si étoiles max
                if (hasMaxStars) {
                    // Délai court pour l'onde
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            createDeathWave(player, target.getLocation(), weaponDamage, statMultiplier, zombieManager, world);
                        }
                    }.runTaskLater(plugin, 5L);
                }
            }
        }.runTaskLater(plugin, 3L);
    }

    private void createDeathWave(Player player, Location center, double weaponDamage, double statMultiplier,
                                  com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {
        double waveDamage = weaponDamage * deathWaveDamagePercent * statMultiplier;

        // Effet visuel de l'onde
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.5f);

        // Cercle de particules expansif
        for (double radius = 1; radius <= waveRadius; radius += 1.5) {
            final double r = radius;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        Location particleLoc = center.clone().add(x, 0.5, z);
                        world.spawnParticle(Particle.SOUL, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        world.spawnParticle(Particle.SMOKE, particleLoc, 3, 0.1, 0.2, 0.1, 0.02);
                    }
                }
            }.runTaskLater(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), (long) (radius / 1.5));
        }

        // Dégâts aux ennemis dans la zone
        Set<UUID> hit = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, waveRadius, 3, waveRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (hit.add(entity.getUniqueId())) {
                    dealDamage(player, living, waveDamage, zombieManager);

                    // Effet sur chaque cible
                    world.spawnParticle(Particle.SOUL, living.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.02);
                }
            }
        }

        player.sendMessage("§4§l☠ §8Onde de mort: §7" + hit.size() + " ennemis frappés!");
    }

    /**
     * Inflige des dégâts à une cible
     */
    private void dealDamage(Player player, LivingEntity target, double damage,
                            com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== SENTINELLE DES ABYSSES (Régénération / Tridents) ====================

/**
 * Passif: Régénération stackable
 * - 3% HP/s par stack (max 3 stacks = 9% HP/s)
 * - Accumule 1 stack toutes les secondes sans prendre de dégâts
 * - Reset complet 5s après avoir subi des dégâts
 */
@Getter
class AbyssalRegenPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double regenPerStack;    // 3% = 0.03
    private final int maxStacks;           // 3 stacks max
    private final int resetDelaySeconds;   // 5s après dégâts

    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Integer> regenStacks = new HashMap<>();
    private final Map<UUID, Long> lastStackGainTime = new HashMap<>();

    public AbyssalRegenPassive(String id, String name, String desc, double regenPerStack, int maxStacks, int resetDelay) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.regenPerStack = regenPerStack;
        this.maxStacks = maxStacks;
        this.resetDelaySeconds = resetDelay;
        PassiveAbilityCleanup.registerForCleanup(lastDamageTime);
        PassiveAbilityCleanup.registerForCleanup(regenStacks);
        PassiveAbilityCleanup.registerForCleanup(lastStackGainTime);
    }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastDamage = lastDamageTime.getOrDefault(uuid, 0L);
        long timeSinceDamage = now - lastDamage;

        // Ajuster le max stacks et regen par niveau
        int adjustedMaxStacks = maxStacks + (petData.getStarPower() >= 1 ? 1 : 0);
        double adjustedRegen = regenPerStack + (petData.getStatMultiplier() - 1) * 0.01;

        // Si on a pris des dégâts dans les 5 dernières secondes, pas de regen
        if (timeSinceDamage < (resetDelaySeconds * 1000L)) {
            // Reset les stacks si on vient de prendre des dégâts
            if (regenStacks.getOrDefault(uuid, 0) > 0) {
                regenStacks.put(uuid, 0);
                World world = player.getWorld();
                world.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 0.5f, 1.2f);
            }
            return;
        }

        // Accumuler les stacks (1 par seconde sans dégâts)
        int currentStacks = regenStacks.getOrDefault(uuid, 0);
        long lastGain = lastStackGainTime.getOrDefault(uuid, 0L);

        if (now - lastGain >= 1000L && currentStacks < adjustedMaxStacks) {
            currentStacks++;
            regenStacks.put(uuid, currentStacks);
            lastStackGainTime.put(uuid, now);

            // Feedback visuel pour gain de stack
            World world = player.getWorld();
            Location loc = player.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.BUBBLE_POP, loc, 8, 0.3, 0.5, 0.3, 0.02);
            world.playSound(loc, Sound.ENTITY_DROWNED_AMBIENT_WATER, 0.3f, 1.5f + (currentStacks * 0.2f));

            // Message de stack
            String stackBar = "§b" + "▮".repeat(currentStacks) + "§7" + "▯".repeat(adjustedMaxStacks - currentStacks);
            player.sendMessage("§a[Pet] §3⚓ Régénération Abyssale: " + stackBar + " §8(" +
                String.format("%.0f", currentStacks * adjustedRegen * 100) + "% HP/s)");
        }

        // Appliquer la régénération
        if (currentStacks > 0) {
            double maxHealth = player.getMaxHealth();
            double currentHealth = player.getHealth();
            double healAmount = maxHealth * adjustedRegen * currentStacks;

            if (currentHealth < maxHealth) {
                double newHealth = Math.min(maxHealth, currentHealth + healAmount);
                player.setHealth(newHealth);

                // Particules de heal
                World world = player.getWorld();
                Location loc = player.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.HEART, loc, 1, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.DRIPPING_WATER, loc, 5, 0.4, 0.5, 0.4, 0);
            }
        }
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        UUID uuid = player.getUniqueId();
        lastDamageTime.put(uuid, System.currentTimeMillis());

        // Reset les stacks immédiatement
        int previousStacks = regenStacks.getOrDefault(uuid, 0);
        if (previousStacks > 0) {
            regenStacks.put(uuid, 0);

            World world = player.getWorld();
            world.playSound(player.getLocation(), Sound.ENTITY_DROWNED_HURT, 0.6f, 0.8f);
            world.spawnParticle(Particle.FALLING_WATER, player.getLocation().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0);
            player.sendMessage("§a[Pet] §c⚓ Régénération interrompue! §7(5s de récupération)");
        }
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        player.sendMessage("§a[Pet] §3⚓ Sentinelle des Abysses équipée!");
        player.sendMessage("§7Régénère §b" + String.format("%.0f", regenPerStack * 100) + "% HP/s §7par stack (max " + maxStacks + ")");
        player.sendMessage("§7Les dégâts réinitialisent la régénération pendant §c" + resetDelaySeconds + "s");
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        regenStacks.remove(uuid);
        lastDamageTime.remove(uuid);
        lastStackGainTime.remove(uuid);
    }

    public int getCurrentStacks(UUID uuid) {
        return regenStacks.getOrDefault(uuid, 0);
    }
}

/**
 * Ultimate: Tempête de Tridents
 * - Lance une volée de tridents sur les ennemis proches
 * - Dégâts basés sur % de l'arme du joueur
 * - Star Power: Les tridents percent et touchent les ennemis derrière
 */
@Getter
class TridentStormActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePercent;     // % des dégâts de l'arme
    private final int tridentCount;         // Nombre de tridents
    private final double radius;            // Rayon de ciblage
    private final AbyssalRegenPassive linkedPassive;

    public TridentStormActive(String id, String name, String desc, double dmgPercent, int count, double radius, AbyssalRegenPassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePercent = dmgPercent;
        this.tridentCount = count;
        this.radius = radius;
        this.linkedPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 45; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Ajuster les valeurs par niveau
        double adjustedDamagePercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.20;
        int adjustedCount = tridentCount + (int)((petData.getStatMultiplier() - 1) * 3);
        boolean piercing = petData.getStarPower() >= 3; // Star 3: les tridents percent

        // Calculer les dégâts basés sur l'arme du joueur
        double weaponDamage = PetDamageUtils.getEffectiveDamage(player);
        double tridentDamage = weaponDamage * adjustedDamagePercent;

        // Bonus si le passif a des stacks
        int regenStacks = linkedPassive != null ? linkedPassive.getCurrentStacks(player.getUniqueId()) : 0;
        if (regenStacks > 0) {
            tridentDamage *= (1 + regenStacks * 0.15); // +15% par stack
            player.sendMessage("§a[Pet] §3⚡ Bonus de stack: §b+" + (regenStacks * 15) + "% §7dégâts!");
        }

        // Trouver les cibles
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(playerLoc, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage("§a[Pet] §c⚓ Aucune cible à portée!");
            return false;
        }

        // Son d'activation
        world.playSound(playerLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_DROWNED_SHOOT, 1.0f, 1.2f);

        player.sendMessage("§a[Pet] §3§l⚓ TEMPÊTE DE TRIDENTS! §7" + adjustedCount + " tridents lancés!");

        // Référence pour le ZombieManager
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();

        // Lancer les tridents avec un délai entre chaque
        final double finalTridentDamage = tridentDamage;
        new BukkitRunnable() {
            int launched = 0;
            int targetIndex = 0;
            Set<UUID> hitByPiercing = new HashSet<>();

            @Override
            public void run() {
                if (launched >= adjustedCount || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Cibler en rotation
                if (targets.isEmpty()) {
                    cancel();
                    return;
                }
                LivingEntity target = targets.get(targetIndex % targets.size());
                if (target.isDead()) {
                    targets.remove(target);
                    if (targets.isEmpty()) {
                        cancel();
                        return;
                    }
                    target = targets.get(targetIndex % targets.size());
                }

                // Position de départ (au-dessus du joueur)
                Location startLoc = playerLoc.clone().add(
                    (Math.random() - 0.5) * 2,
                    2.5 + Math.random(),
                    (Math.random() - 0.5) * 2
                );

                // Direction vers la cible
                Vector direction = target.getLocation().add(0, 1, 0).toVector()
                    .subtract(startLoc.toVector()).normalize();

                // Créer le trident visuel
                spawnTridentProjectile(world, startLoc, direction, target, finalTridentDamage,
                    player, zombieManager, piercing, hitByPiercing);

                // Son de lancer
                world.playSound(startLoc, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.0f + (float)(Math.random() * 0.4));

                launched++;
                targetIndex++;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Un trident toutes les 2 ticks (0.1s)

        return true;
    }

    private void spawnTridentProjectile(World world, Location start, Vector direction,
            LivingEntity primaryTarget, double damage, Player player,
            com.rinaorc.zombiez.zombies.ZombieManager zombieManager,
            boolean piercing, Set<UUID> alreadyHit) {

        new BukkitRunnable() {
            Location currentLoc = start.clone();
            int ticks = 0;
            final int maxTicks = 40; // 2 secondes max
            boolean hasHitPrimary = false;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Déplacer le trident
                currentLoc.add(direction.clone().multiply(1.5));

                // Particules de traînée
                world.spawnParticle(Particle.BUBBLE_POP, currentLoc, 3, 0.1, 0.1, 0.1, 0);
                world.spawnParticle(Particle.DRIPPING_WATER, currentLoc, 2, 0.1, 0.1, 0.1, 0);

                // Vérifier les collisions
                for (Entity entity : world.getNearbyEntities(currentLoc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity living && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand) && !living.isDead()) {

                        // Si piercing, ne pas toucher deux fois la même cible
                        if (piercing && alreadyHit.contains(entity.getUniqueId())) {
                            continue;
                        }

                        // Impact!
                        world.spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.1);
                        world.playSound(living.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.0f, 1.0f);

                        // Infliger les dégâts
                        dealDamage(player, living, damage, zombieManager);

                        if (piercing) {
                            alreadyHit.add(entity.getUniqueId());
                            // Continuer le trajet pour le piercing
                            if (entity.equals(primaryTarget)) {
                                hasHitPrimary = true;
                            }
                        } else {
                            cancel();
                            return;
                        }
                    }
                }

                // Vérifier si on a atteint la cible principale ou si on est hors de portée
                if (!piercing && currentLoc.distanceSquared(primaryTarget.getLocation()) < 1) {
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), 0L, 1L);
    }

    private void dealDamage(Player player, LivingEntity target, double damage,
                            com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== CARAVANIER DU DÉSERT (Blocage / Contre-attaque) ====================

/**
 * Passif: Endurance du Désert
 * - Désactive l'esquive (0% dodge)
 * - +30% de chance de blocage
 * - Les blocages restaurent 2% HP max
 */
@Getter
class DesertEndurancePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double blockBonus;         // +30% = 0.30
    private final double healPerBlock;       // 2% HP = 0.02

    private final Map<UUID, Boolean> dodgeDisabled = new HashMap<>();

    public DesertEndurancePassive(String id, String name, String desc, double blockBonus, double healPerBlock) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.blockBonus = blockBonus;
        this.healPerBlock = healPerBlock;
        PassiveAbilityCleanup.registerForCleanup(dodgeDisabled);
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Retourne le bonus de blocage ajusté par niveau
     */
    public double getAdjustedBlockBonus(PetData petData) {
        // Star 1+: +35% au lieu de +30%
        double bonus = petData.getStarPower() >= 1 ? 0.35 : blockBonus;
        return bonus + (petData.getStatMultiplier() - 1) * 0.05;
    }

    /**
     * Retourne le heal par block ajusté par niveau
     */
    public double getAdjustedHealPerBlock(PetData petData) {
        // Star 1+: 3% au lieu de 2%
        double heal = petData.getStarPower() >= 1 ? 0.03 : healPerBlock;
        return heal + (petData.getStatMultiplier() - 1) * 0.005;
    }

    /**
     * Vérifie si l'esquive est désactivée pour ce joueur
     */
    public boolean isDodgeDisabled(UUID uuid) {
        return dodgeDisabled.getOrDefault(uuid, false);
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        dodgeDisabled.put(uuid, true);

        double adjustedBlock = getAdjustedBlockBonus(petData);
        double adjustedHeal = getAdjustedHealPerBlock(petData);

        player.sendMessage("§a[Pet] §e🐫 Caravanier du Désert équipé!");
        player.sendMessage("§c⚠ Esquive désactivée §7| §a+" + String.format("%.0f", adjustedBlock * 100) + "% blocage");
        player.sendMessage("§a❤ Les blocages soignent §b" + String.format("%.0f", adjustedHeal * 100) + "% HP");

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_CAMEL_AMBIENT, 1.0f, 1.0f);
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        dodgeDisabled.remove(uuid);
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Le bonus de block est appliqué dans le CombatListener via getAdjustedBlockBonus()
        // Ici on maintient juste l'état du joueur
    }

    /**
     * Appelé quand le joueur bloque une attaque (depuis CombatListener ou PetCombatListener)
     * Retourne le montant de heal effectué
     */
    public double onBlock(Player player, PetData petData, double blockedDamage) {
        double adjustedHeal = getAdjustedHealPerBlock(petData);
        double maxHealth = player.getMaxHealth();
        double healAmount = maxHealth * adjustedHeal;

        double currentHealth = player.getHealth();
        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
        player.setHealth(newHealth);

        // Feedback visuel
        World world = player.getWorld();
        Location loc = player.getLocation().add(0, 1, 0);
        world.spawnParticle(Particle.HEART, loc, 2, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.BLOCK, loc, 8, 0.3, 0.5, 0.3, 0, Material.SAND.createBlockData());
        world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.2f);

        return healAmount;
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        // La logique de block est gérée par le système de combat ZombieZ
        // et appelle onBlock() quand un block se produit
    }
}

/**
 * Ultimate: Charge du Caravanier
 * - Pendant 6 secondes, stocke tous les dégâts bloqués
 * - À la fin, explosion AoE de 200% des dégâts stockés
 * - Pendant le stockage, +20% block supplémentaire
 * - Star 3: L'explosion stun les ennemis 2s
 */
@Getter
class CaravanChargeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;       // 6 secondes
    private final double damageMultiplier;   // 200% = 2.0
    private final double extraBlockBonus;    // +20% pendant l'ultimate
    private final double radius;             // 8 blocs
    private final DesertEndurancePassive linkedPassive;

    private final Map<UUID, Double> storedDamage = new HashMap<>();
    private final Map<UUID, Boolean> chargeActive = new HashMap<>();
    private final Map<UUID, Long> chargeStartTime = new HashMap<>();

    public CaravanChargeActive(String id, String name, String desc, int duration, double dmgMult,
                               double extraBlock, double radius, DesertEndurancePassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
        this.damageMultiplier = dmgMult;
        this.extraBlockBonus = extraBlock;
        this.radius = radius;
        this.linkedPassive = passive;
        PassiveAbilityCleanup.registerForCleanup(storedDamage);
        PassiveAbilityCleanup.registerForCleanup(chargeActive);
        PassiveAbilityCleanup.registerForCleanup(chargeStartTime);
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 50; }

    /**
     * Vérifie si le joueur est en phase de stockage
     */
    public boolean isCharging(UUID uuid) {
        return chargeActive.getOrDefault(uuid, false);
    }

    /**
     * Retourne le bonus de block supplémentaire pendant l'ultimate
     */
    public double getExtraBlockBonus(PetData petData) {
        return extraBlockBonus + (petData.getStatMultiplier() - 1) * 0.05;
    }

    /**
     * Appelé quand le joueur bloque pendant la charge
     */
    public void addBlockedDamage(UUID uuid, double damage) {
        if (isCharging(uuid)) {
            double current = storedDamage.getOrDefault(uuid, 0.0);
            storedDamage.put(uuid, current + damage);
        }
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Démarrer la phase de stockage
        chargeActive.put(uuid, true);
        chargeStartTime.put(uuid, System.currentTimeMillis());
        storedDamage.put(uuid, 0.0);

        // Ajuster les valeurs par niveau
        double adjustedMultiplier = damageMultiplier + (petData.getStatMultiplier() - 1) * 0.30;
        double adjustedRadius = radius + (petData.getStatMultiplier() - 1) * 2;
        boolean stunOnExplosion = petData.getStarPower() >= 3;
        int adjustedDuration = durationSeconds + (petData.getStarPower() >= 2 ? 2 : 0);

        // Effets de démarrage
        world.playSound(playerLoc, Sound.ENTITY_CAMEL_DASH, 1.2f, 0.8f);
        world.playSound(playerLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.6f);
        world.spawnParticle(Particle.BLOCK, playerLoc.add(0, 1, 0), 30, 1, 1, 1, 0, Material.SAND.createBlockData());

        player.sendMessage("§a[Pet] §e§l🐫 CHARGE DU CARAVANIER! §7Bloquez pour stocker les dégâts!");
        player.sendMessage("§7Bonus blocage: §a+" + String.format("%.0f", getExtraBlockBonus(petData) * 100) + "% §7pendant " + adjustedDuration + "s");

        // Référence pour le ZombieManager
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();

        // Timer pour l'explosion finale
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = adjustedDuration * 20;

            @Override
            public void run() {
                if (!player.isOnline() || !chargeActive.getOrDefault(uuid, false)) {
                    cleanup();
                    cancel();
                    return;
                }

                // Particules de charge pendant la durée
                if (ticks % 10 == 0) {
                    Location loc = player.getLocation().add(0, 1, 0);
                    double stored = storedDamage.getOrDefault(uuid, 0.0);

                    // Plus de particules si plus de dégâts stockés
                    int particleCount = Math.min(20, (int)(stored / 10) + 3);
                    world.spawnParticle(Particle.DUST, loc, particleCount, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(194, 178, 128), 1.5f));

                    // Son de charge croissant
                    if (stored > 50 && ticks % 20 == 0) {
                        float pitch = Math.min(1.5f, 0.8f + (float)(stored / 200));
                        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.3f, pitch);
                    }
                }

                // Temps restant à afficher
                if (ticks % 20 == 0) {
                    int secondsLeft = (maxTicks - ticks) / 20;
                    double stored = storedDamage.getOrDefault(uuid, 0.0);
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§e🐫 Stocké: §c" + String.format("%.0f", stored) + " dégâts §7| " +
                        "§fExplosion dans §e" + secondsLeft + "s"));
                }

                if (ticks >= maxTicks) {
                    // EXPLOSION FINALE!
                    triggerExplosion(player, petData, adjustedMultiplier, adjustedRadius, stunOnExplosion, zombieManager);
                    cleanup();
                    cancel();
                    return;
                }

                ticks++;
            }

            private void cleanup() {
                chargeActive.remove(uuid);
                chargeStartTime.remove(uuid);
                storedDamage.remove(uuid);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void triggerExplosion(Player player, PetData petData, double multiplier, double explosionRadius,
                                  boolean stun, com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        UUID uuid = player.getUniqueId();
        World world = player.getWorld();
        Location center = player.getLocation();

        double stored = storedDamage.getOrDefault(uuid, 0.0);
        double explosionDamage = stored * multiplier;

        // Minimum de dégâts même sans block (basé sur l'arme)
        if (explosionDamage < 20) {
            double weaponDamage = PetDamageUtils.getEffectiveDamage(player);
            explosionDamage = Math.max(explosionDamage, weaponDamage * 2);
        }

        // Effets visuels d'explosion
        world.playSound(center, Sound.ENTITY_CAMEL_DASH, 1.5f, 0.5f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        world.playSound(center, Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);

        // Vague de particules de sable
        for (int ring = 0; ring < 3; ring++) {
            final int ringIndex = ring;
            Bukkit.getScheduler().runTaskLater(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), () -> {
                double ringRadius = (ringIndex + 1) * (explosionRadius / 3);
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = center.clone().add(
                        Math.cos(rad) * ringRadius,
                        0.5,
                        Math.sin(rad) * ringRadius
                    );
                    world.spawnParticle(Particle.BLOCK, particleLoc, 5, 0.3, 0.3, 0.3, 0.1,
                        Material.SAND.createBlockData());
                    world.spawnParticle(Particle.EXPLOSION, particleLoc, 1, 0, 0, 0, 0);
                }
            }, ringIndex * 3L);
        }

        // Colonne centrale
        for (double y = 0; y < 3; y += 0.5) {
            Location colLoc = center.clone().add(0, y, 0);
            world.spawnParticle(Particle.DUST, colLoc, 10, 0.5, 0.2, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(194, 128, 64), 2.0f));
        }

        // Infliger les dégâts aux ennemis
        Set<UUID> hit = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, explosionRadius, explosionRadius, explosionRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (hit.add(entity.getUniqueId())) {
                    // Dégâts
                    dealDamage(player, living, explosionDamage, zombieManager);

                    // Effet de recul
                    Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                    knockback.setY(0.4);
                    living.setVelocity(knockback);

                    // Stun si Star 3
                    if (stun && living instanceof org.bukkit.entity.Mob mob) {
                        mob.setAI(false);
                        world.spawnParticle(Particle.FLASH, living.getLocation().add(0, 1, 0), 1);

                        Bukkit.getScheduler().runTaskLater(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), () -> {
                            if (mob.isValid() && !mob.isDead()) {
                                mob.setAI(true);
                            }
                        }, 40L); // 2 secondes
                    }

                    // Particules d'impact
                    world.spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.2);
                }
            }
        }

        // Message de résultat
        player.sendMessage("§a[Pet] §e§l🐫 EXPLOSION! §c" + String.format("%.0f", explosionDamage) +
            " dégâts §7(" + String.format("%.0f", stored) + " stockés x" + String.format("%.0f", multiplier * 100) + "%)");
        player.sendMessage("§7Ennemis touchés: §e" + hit.size() + (stun ? " §c(Stunned 2s)" : ""));
    }

    private void dealDamage(Player player, LivingEntity target, double damage,
                            com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== MARCHAND DE FOUDRE (Électricité / Chaos) ====================

/**
 * Passif: Marchandise Instable
 * - 3% de chance par attaque de libérer 3 charges électriques
 * - Les charges voyagent aléatoirement vers les ennemis proches
 * - Dégâts: X% des dégâts de l'arme (dégâts foudre)
 */
@Getter
class UnstableMerchandisePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double procChance;         // 3% = 0.03
    private final int chargeCount;           // 3 charges
    private final double damagePercent;      // % des dégâts de l'arme
    private final double searchRadius;       // Rayon de recherche des cibles

    public UnstableMerchandisePassive(String id, String name, String desc, double procChance,
                                       int chargeCount, double damagePercent, double searchRadius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.procChance = procChance;
        this.chargeCount = chargeCount;
        this.damagePercent = damagePercent;
        this.searchRadius = searchRadius;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Retourne la chance de proc ajustée par niveau
     */
    public double getAdjustedProcChance(PetData petData) {
        // Star 1+: 5% au lieu de 3%
        double chance = petData.getStarPower() >= 1 ? 0.05 : procChance;
        return chance + (petData.getStatMultiplier() - 1) * 0.01;
    }

    /**
     * Retourne le nombre de charges ajusté par niveau
     */
    public int getAdjustedChargeCount(PetData petData) {
        // Star 1+: 4 charges au lieu de 3
        int charges = petData.getStarPower() >= 1 ? 4 : chargeCount;
        return charges + (int)((petData.getStatMultiplier() - 1) * 1);
    }

    /**
     * Retourne les dégâts ajustés par niveau
     */
    public double getAdjustedDamagePercent(PetData petData) {
        // Star 1+: +10% dégâts
        double dmg = petData.getStarPower() >= 1 ? damagePercent + 0.10 : damagePercent;
        return dmg + (petData.getStatMultiplier() - 1) * 0.05;
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        double adjustedChance = getAdjustedProcChance(petData);
        int adjustedCharges = getAdjustedChargeCount(petData);

        player.sendMessage("§a[Pet] §e⚡ Marchand de Foudre équipé!");
        player.sendMessage("§7Chance: §e" + String.format("%.0f", adjustedChance * 100) + "% §7de libérer §e" +
            adjustedCharges + " charges électriques");

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_AMBIENT, 1.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.5f);
    }

    @Override
    public void onDamageDealt(Player player, PetData petData, LivingEntity target, double damage) {
        double adjustedChance = getAdjustedProcChance(petData);

        if (Math.random() > adjustedChance) {
            return; // Pas de proc
        }

        // PROC! Libérer les charges électriques
        int adjustedCharges = getAdjustedChargeCount(petData);
        double adjustedDamagePercent = getAdjustedDamagePercent(petData);

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Calculer les dégâts des charges
        double weaponDamage = PetDamageUtils.getEffectiveDamage(player);
        double chargeDamage = weaponDamage * adjustedDamagePercent;

        // Son d'activation
        world.playSound(playerLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.8f);
        world.playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.5f);

        player.sendMessage("§a[Pet] §e⚡ CHARGES LIBÉRÉES! §7" + adjustedCharges + " charges électriques!");

        // Trouver les cibles proches
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(playerLoc, searchRadius, searchRadius, searchRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {
                potentialTargets.add(living);
            }
        }

        if (potentialTargets.isEmpty()) {
            return;
        }

        // Référence pour le ZombieManager
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();

        // Lancer les charges avec un petit délai entre chaque
        for (int i = 0; i < adjustedCharges; i++) {
            final int chargeIndex = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || potentialTargets.isEmpty()) return;

                // Choisir une cible aléatoire
                LivingEntity chargeTarget = potentialTargets.get((int)(Math.random() * potentialTargets.size()));
                if (chargeTarget.isDead()) {
                    potentialTargets.remove(chargeTarget);
                    if (potentialTargets.isEmpty()) return;
                    chargeTarget = potentialTargets.get((int)(Math.random() * potentialTargets.size()));
                }

                // Créer l'effet visuel de la charge électrique
                spawnElectricCharge(world, playerLoc.clone().add(0, 1, 0), chargeTarget, chargeDamage, player, zombieManager);

            }, i * 3L); // 3 ticks entre chaque charge
        }
    }

    /**
     * Spawne une charge électrique visuelle qui voyage vers la cible
     */
    private void spawnElectricCharge(World world, Location start, LivingEntity target, double damage,
                                      Player player, com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {

        new BukkitRunnable() {
            Location currentLoc = start.clone();
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (ticks >= maxTicks || target.isDead()) {
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation().add(0, 1, 0);
                Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
                double distance = direction.length();

                if (distance < 1.0) {
                    // Impact!
                    spawnLightningImpact(world, targetLoc);
                    dealLightningDamage(player, target, damage, zombieManager);
                    cancel();
                    return;
                }

                // Déplacer la charge avec un mouvement erratique
                direction.normalize().multiply(Math.min(1.5, distance));
                // Ajouter du chaos au mouvement
                direction.add(new Vector(
                    (Math.random() - 0.5) * 0.5,
                    (Math.random() - 0.5) * 0.3,
                    (Math.random() - 0.5) * 0.5
                ));
                currentLoc.add(direction);

                // Particules de la charge électrique
                world.spawnParticle(Particle.ELECTRIC_SPARK, currentLoc, 5, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.END_ROD, currentLoc, 2, 0.05, 0.05, 0.05, 0);
                world.spawnParticle(Particle.DUST, currentLoc, 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 100), 0.8f));

                // Son crépitant occasionnel
                if (ticks % 5 == 0) {
                    world.playSound(currentLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 2.0f);
                }

                ticks++;
            }
        }.runTaskTimer(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), 0L, 1L);
    }

    /**
     * Effet visuel d'impact de foudre (sans LightningStrike vanilla)
     */
    private void spawnLightningImpact(World world, Location loc) {
        // Explosion de particules électriques
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 25, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.FIREWORK, loc, 8, 0.2, 0.2, 0.2, 0.1);
        world.spawnParticle(Particle.DUST, loc, 10, 0.4, 0.4, 0.4, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 255, 255), 1.2f));

        // Colonne de lumière vers le haut
        for (double y = 0; y < 2; y += 0.3) {
            Location colLoc = loc.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, colLoc, 2, 0.1, 0, 0.1, 0);
        }

        // Son d'impact
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.8f);
    }

    /**
     * Inflige des dégâts de foudre
     */
    private void dealLightningDamage(Player player, LivingEntity target, double damage,
                                      com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.LIGHTNING, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) { }
}

/**
 * Ultimate: Arc Voltaïque
 * - Lance un éclair qui rebondit entre les ennemis
 * - Chaque rebond: 80% des dégâts de l'arme
 * - Chaque ennemi touché a 50% de chance d'être paralysé 1s
 * - Star 3: L'éclair peut toucher le même ennemi plusieurs fois
 */
@Getter
class VoltaicArcActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int maxBounces;            // 8 rebonds
    private final double damagePercent;      // 80% des dégâts
    private final double paralyzeChance;     // 50% de paralyser
    private final double bounceRadius;       // Rayon de recherche pour rebondir
    private final UnstableMerchandisePassive linkedPassive;

    public VoltaicArcActive(String id, String name, String desc, int bounces, double dmgPercent,
                            double paralyzeChance, double bounceRadius, UnstableMerchandisePassive passive) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.maxBounces = bounces;
        this.damagePercent = dmgPercent;
        this.paralyzeChance = paralyzeChance;
        this.bounceRadius = bounceRadius;
        this.linkedPassive = passive;
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return 40; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Ajuster les valeurs par niveau
        int adjustedBounces = maxBounces + (int)((petData.getStatMultiplier() - 1) * 3);
        double adjustedDamagePercent = damagePercent + (petData.getStatMultiplier() - 1) * 0.15;
        double adjustedParalyzeChance = paralyzeChance + (petData.getStatMultiplier() - 1) * 0.10;
        boolean canHitSameTarget = petData.getStarPower() >= 3;

        // Calculer les dégâts
        double weaponDamage = PetDamageUtils.getEffectiveDamage(player);
        double arcDamage = weaponDamage * adjustedDamagePercent;

        // Trouver la première cible (la plus proche dans la direction du regard)
        LivingEntity firstTarget = findTargetInSight(player, 20);
        if (firstTarget == null) {
            // Chercher n'importe quel ennemi proche
            for (Entity entity : world.getNearbyEntities(playerLoc, 15, 15, 15)) {
                if (entity instanceof LivingEntity living && !(entity instanceof Player)
                    && !(entity instanceof ArmorStand) && !living.isDead()) {
                    firstTarget = living;
                    break;
                }
            }
        }

        if (firstTarget == null) {
            player.sendMessage("§a[Pet] §c⚡ Aucune cible à portée!");
            return false;
        }

        // Son d'activation
        world.playSound(playerLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        world.playSound(playerLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_WANDERING_TRADER_TRADE, 1.0f, 1.2f);

        player.sendMessage("§a[Pet] §e§l⚡ ARC VOLTAÏQUE! §7" + adjustedBounces + " rebonds max!");

        // Référence pour le ZombieManager
        var plugin = com.rinaorc.zombiez.ZombieZPlugin.getInstance();
        var zombieManager = plugin.getZombieManager();

        // Démarrer la chaîne d'éclairs
        startLightningChain(world, playerLoc.clone().add(0, 1.5, 0), firstTarget, arcDamage,
            adjustedBounces, adjustedParalyzeChance, canHitSameTarget, player, zombieManager, petData);

        return true;
    }

    /**
     * Trouve une cible dans la direction du regard du joueur
     */
    private LivingEntity findTargetInSight(Player player, double maxDistance) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        for (double d = 1; d <= maxDistance; d += 0.5) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(d));
            for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity living && !(entity instanceof Player)
                    && !(entity instanceof ArmorStand) && !living.isDead()) {
                    return living;
                }
            }
        }
        return null;
    }

    /**
     * Lance la chaîne d'éclairs qui rebondit entre les ennemis
     */
    private void startLightningChain(World world, Location startLoc, LivingEntity firstTarget, double damage,
                                      int remainingBounces, double paralyzeChance, boolean canHitSame,
                                      Player player, com.rinaorc.zombiez.zombies.ZombieManager zombieManager,
                                      PetData petData) {

        Set<UUID> hitTargets = canHitSame ? null : new HashSet<>();
        List<LivingEntity> chainTargets = new ArrayList<>();
        chainTargets.add(firstTarget);

        // Construire la chaîne de cibles
        LivingEntity currentTarget = firstTarget;
        Location currentLoc = startLoc.clone();

        for (int i = 0; i < remainingBounces && currentTarget != null; i++) {
            if (!canHitSame && hitTargets != null) {
                hitTargets.add(currentTarget.getUniqueId());
            }

            // Chercher la prochaine cible
            LivingEntity nextTarget = findNextTarget(world, currentTarget.getLocation(),
                bounceRadius, canHitSame ? null : hitTargets);

            if (nextTarget != null) {
                chainTargets.add(nextTarget);
                currentTarget = nextTarget;
            } else {
                break;
            }
        }

        // Animer la chaîne d'éclairs
        final List<LivingEntity> finalChain = chainTargets;
        new BukkitRunnable() {
            int currentIndex = 0;
            Location arcStart = startLoc.clone();

            @Override
            public void run() {
                if (currentIndex >= finalChain.size()) {
                    cancel();
                    return;
                }

                LivingEntity target = finalChain.get(currentIndex);
                if (target.isDead()) {
                    currentIndex++;
                    return;
                }

                Location targetLoc = target.getLocation().add(0, 1, 0);

                // Dessiner l'arc électrique entre arcStart et targetLoc
                drawLightningArc(world, arcStart, targetLoc);

                // Impact et dégâts
                spawnLightningImpact(world, targetLoc);
                dealLightningDamage(player, target, damage, zombieManager);

                // Chance de paralyser
                if (Math.random() < paralyzeChance && target instanceof org.bukkit.entity.Mob mob) {
                    mob.setAI(false);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 15, 0.3, 0.5, 0.3, 0.05);

                    Bukkit.getScheduler().runTaskLater(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), () -> {
                        if (mob.isValid() && !mob.isDead()) {
                            mob.setAI(true);
                        }
                    }, 20L); // 1 seconde
                }

                arcStart = targetLoc.clone();
                currentIndex++;
            }
        }.runTaskTimer(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), 0L, 4L);

        // Message de résultat
        Bukkit.getScheduler().runTaskLater(com.rinaorc.zombiez.ZombieZPlugin.getInstance(), () -> {
            player.sendMessage("§a[Pet] §e⚡ Arc terminé: §7" + finalChain.size() + " ennemis frappés!");
        }, (finalChain.size() + 1) * 4L);
    }

    /**
     * Trouve la prochaine cible pour le rebond
     */
    private LivingEntity findNextTarget(World world, Location fromLoc, double radius, Set<UUID> excludeIds) {
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : world.getNearbyEntities(fromLoc, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (excludeIds != null && excludeIds.contains(entity.getUniqueId())) {
                    continue;
                }

                double dist = living.getLocation().distanceSquared(fromLoc);
                if (dist > 1 && dist < closestDist) { // > 1 pour éviter la même position
                    closestDist = dist;
                    closest = living;
                }
            }
        }

        return closest;
    }

    /**
     * Dessine un arc électrique entre deux points (avec particules)
     */
    private void drawLightningArc(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        int segments = (int)(distance * 3);
        Location currentPoint = from.clone();

        for (int i = 0; i < segments; i++) {
            // Avancer le long de la ligne
            currentPoint.add(direction.clone().multiply(distance / segments));

            // Ajouter du zigzag aléatoire
            Location particleLoc = currentPoint.clone().add(
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3,
                (Math.random() - 0.5) * 0.3
            );

            // Particules de l'arc
            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.05, 0.05, 0.05, 0);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);

            // Particules colorées tous les 2 segments
            if (i % 2 == 0) {
                world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 200, 255), 1.0f));
            }
        }

        // Son de crépitement
        world.playSound(from, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
        world.playSound(to, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.5f);
    }

    /**
     * Effet visuel d'impact de foudre
     */
    private void spawnLightningImpact(World world, Location loc) {
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 0.5, 0.5, 0.5, 0.15);
        world.spawnParticle(Particle.END_ROD, loc, 12, 0.3, 0.4, 0.3, 0.05);
        world.spawnParticle(Particle.FIREWORK, loc, 6, 0.2, 0.2, 0.2, 0.1);
        world.spawnParticle(Particle.DUST, loc, 8, 0.3, 0.3, 0.3, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 150), 1.5f));

        // Colonne de lumière
        for (double y = 0; y < 1.5; y += 0.2) {
            Location colLoc = loc.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, colLoc, 1, 0.05, 0, 0.05, 0);
        }

        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.3f);
    }

    /**
     * Inflige des dégâts de foudre
     */
    private void dealLightningDamage(Player player, LivingEntity target, double damage,
                                      com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, damage,
                    com.rinaorc.zombiez.zombies.DamageType.LIGHTNING, false);
                return;
            }
        }
        target.damage(damage, player);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }
}

// ==================== MARCHEUR DE BRAISE (STRIDER) ====================

/**
 * Passif: Braises Critiques
 * +6% crit par ennemi en feu dans un rayon de 32 blocs (max 5 stacks = 30%)
 * Le bonus dure 3 secondes après que l'ennemi cesse de brûler
 */
class BurningCritPassive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double critPerBurning;  // +6% par ennemi
    private final int maxStacks;          // Max 5
    private final double range;           // 32 blocs
    private final long stackDuration;     // 3 secondes

    // Track des stacks par joueur
    private static final Map<UUID, Integer> currentStacks = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastStackUpdate = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(currentStacks);
        PassiveAbilityCleanup.registerForCleanup(lastStackUpdate);
    }

    public BurningCritPassive(String id, String name, String description,
                               double critPerBurning, int maxStacks, double range, long stackDurationSeconds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.critPerBurning = critPerBurning;
        this.maxStacks = maxStacks;
        this.range = range;
        this.stackDuration = stackDurationSeconds * 1000;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // Compte les ennemis en feu dans le rayon
        int burningEnemies = countBurningEnemies(player);

        // Calcule les stacks (max 5)
        int stacks = Math.min(burningEnemies, maxStacks);

        // Met à jour les stacks
        UUID playerId = player.getUniqueId();
        int previousStacks = currentStacks.getOrDefault(playerId, 0);

        if (stacks > 0) {
            currentStacks.put(playerId, stacks);
            lastStackUpdate.put(playerId, System.currentTimeMillis());

            // Affiche le changement de stacks
            if (stacks != previousStacks) {
                double totalCrit = stacks * critPerBurning * 100;
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§6🔥 Braises Critiques: §e+" + String.format("%.0f", totalCrit) + "% crit §7(" + stacks + "/" + maxStacks + ")"
                ));

                // Effet visuel
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0),
                    stacks * 3, 0.5, 0.5, 0.5, 0.02);
            }
        } else {
            // Vérifie si le bonus a expiré
            Long lastUpdate = lastStackUpdate.get(playerId);
            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate > stackDuration) {
                currentStacks.remove(playerId);
                lastStackUpdate.remove(playerId);
            }
        }
    }

    /**
     * Compte les ennemis en feu dans le rayon
     */
    private int countBurningEnemies(Player player) {
        int count = 0;
        Location playerLoc = player.getLocation();
        double rangeSquared = range * range;

        for (Entity entity : player.getWorld().getNearbyEntities(playerLoc, range, range, range)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                if (living.getFireTicks() > 0 && living.getLocation().distanceSquared(playerLoc) <= rangeSquared) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Obtient le bonus de crit actuel pour un joueur
     */
    public double getCritBonus(Player player) {
        int stacks = currentStacks.getOrDefault(player.getUniqueId(), 0);
        return stacks * critPerBurning;
    }

    /**
     * Obtient le nombre de stacks actuel
     */
    public int getCurrentStacks(Player player) {
        return currentStacks.getOrDefault(player.getUniqueId(), 0);
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        // Le bonus de crit est appliqué via le système de combat
    }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) {
        currentStacks.remove(player.getUniqueId());
        lastStackUpdate.remove(player.getUniqueId());
    }
}

/**
 * Ultimate: Rayon de Désintégration
 * Canalise un rayon d'énergie brûlante qui inflige des dégâts croissants
 * et désintègre les ennemis tués
 */
class DisintegrationRayActive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double baseDamagePercent;    // 100% arme/s
    private final double damageIncreasePercent; // +50% arme/s
    private final double maxDamagePercent;      // 400% max
    private final double range;                 // Portée du rayon
    private final int durationTicks;            // Durée de la canalisation
    private final BurningCritPassive linkedPassive;

    // Track des joueurs qui canalisent
    private static final Map<UUID, BukkitTask> activeChannels = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(activeChannels);
    }

    public DisintegrationRayActive(String id, String name, String description,
                                    double baseDamagePercent, double damageIncreasePercent,
                                    double maxDamagePercent, double range, int durationSeconds,
                                    BurningCritPassive linkedPassive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseDamagePercent = baseDamagePercent;
        this.damageIncreasePercent = damageIncreasePercent;
        this.maxDamagePercent = maxDamagePercent;
        this.range = range;
        this.durationTicks = durationSeconds * 20;
        this.linkedPassive = linkedPassive;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();

        // Annule le canal précédent si existant
        BukkitTask existingTask = activeChannels.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Obtient le plugin et le ZombieManager
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;
        var zombieManager = plugin.getZombieManager();

        // Calculer les dégâts de base (% de l'arme)
        double weaponDamage = getPlayerWeaponDamage(player);
        double statMultiplier = petData.getStatMultiplier();

        // Effet d'activation
        player.sendMessage("§6§l⚡ RAYON DE DÉSINTÉGRATION ACTIVÉ!");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);

        // Démarre la canalisation
        final int[] ticksElapsed = {0};
        final double[] currentDamageMultiplier = {baseDamagePercent};

        BukkitTask channelTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifie si le joueur est toujours en ligne et vivant
                if (!player.isOnline() || player.isDead()) {
                    endChannel(player, "§cRayon interrompu!");
                    return;
                }

                ticksElapsed[0]++;

                // Fin de la canalisation
                if (ticksElapsed[0] >= durationTicks) {
                    endChannel(player, "§6Rayon de Désintégration terminé.");
                    return;
                }

                // Augmente les dégâts chaque seconde
                if (ticksElapsed[0] % 20 == 0) {
                    currentDamageMultiplier[0] = Math.min(
                        currentDamageMultiplier[0] + damageIncreasePercent,
                        maxDamagePercent
                    );
                }

                // Tire le rayon toutes les 10 ticks (0.5s)
                if (ticksElapsed[0] % 10 == 0) {
                    fireDisintegrationRay(player, zombieManager, weaponDamage,
                        currentDamageMultiplier[0], statMultiplier, petData);
                }

                // Effet visuel continu sur le joueur
                if (ticksElapsed[0] % 5 == 0) {
                    spawnChannelParticles(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeChannels.put(playerId, channelTask);
        return true;
    }

    /**
     * Termine la canalisation
     */
    private void endChannel(Player player, String message) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = activeChannels.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        player.sendMessage(message);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.2f);
    }

    /**
     * Tire le rayon de désintégration
     */
    private void fireDisintegrationRay(Player player, com.rinaorc.zombiez.zombies.ZombieManager zombieManager,
                                        double weaponDamage, double damageMultiplier, double statMultiplier,
                                        PetData petData) {
        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        // Trouve l'ennemi ciblé
        LivingEntity target = findTargetInRay(player, eyeLoc, direction, range);

        // Dessine le rayon (toujours, même sans cible)
        Location endPoint = target != null ? target.getLocation().add(0, 1, 0)
            : eyeLoc.clone().add(direction.clone().multiply(range));
        drawDisintegrationBeam(world, eyeLoc, endPoint, target != null);

        // Inflige des dégâts si cible trouvée
        if (target != null) {
            double damage = weaponDamage * damageMultiplier * statMultiplier;

            // Bonus si le passif est actif
            if (linkedPassive != null) {
                int stacks = linkedPassive.getCurrentStacks(player);
                if (stacks > 0) {
                    damage *= (1.0 + stacks * 0.10); // +10% par stack
                }
            }

            // Met le feu à la cible
            target.setFireTicks(60);

            // Inflige les dégâts
            boolean killed = false;
            if (zombieManager != null) {
                var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
                if (activeZombie != null) {
                    double healthBefore = target.getHealth();
                    zombieManager.damageZombie(player, activeZombie, damage,
                        com.rinaorc.zombiez.zombies.DamageType.FIRE, false);
                    killed = target.isDead() || target.getHealth() <= 0;
                } else {
                    target.damage(damage, player);
                    killed = target.isDead();
                }
            } else {
                target.damage(damage, player);
                killed = target.isDead();
            }

            // Effet de désintégration si tué
            if (killed) {
                spawnDisintegrationEffect(world, target.getLocation());
            }
        }
    }

    /**
     * Trouve une cible dans la direction du rayon
     */
    private LivingEntity findTargetInRay(Player player, Location start, Vector direction, double maxRange) {
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getWorld().getNearbyEntities(start, maxRange, maxRange, maxRange)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                Location targetLoc = living.getLocation().add(0, 1, 0);
                Vector toTarget = targetLoc.toVector().subtract(start.toVector());

                // Vérifie si dans le cône du rayon (angle < 10 degrés)
                double angle = direction.angle(toTarget.clone().normalize());
                if (angle < Math.toRadians(10)) {
                    double dist = toTarget.length();
                    if (dist < closestDist && dist <= maxRange) {
                        closestDist = dist;
                        closest = living;
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Dessine le rayon de désintégration
     */
    private void drawDisintegrationBeam(World world, Location from, Location to, boolean hasTarget) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        int segments = (int)(distance * 4);
        Location currentPoint = from.clone();

        for (int i = 0; i < segments; i++) {
            currentPoint.add(direction.clone().multiply(distance / segments));

            // Couleur selon si on touche une cible
            if (hasTarget) {
                // Rayon orange/rouge intense
                world.spawnParticle(Particle.FLAME, currentPoint, 2, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.DUST, currentPoint, 3, 0.08, 0.08, 0.08, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 1.2f));
            } else {
                // Rayon jaune/orange
                world.spawnParticle(Particle.DUST, currentPoint, 2, 0.06, 0.06, 0.06, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 180, 50), 0.8f));
            }

            // Étincelles tous les 3 segments
            if (i % 3 == 0) {
                world.spawnParticle(Particle.LAVA, currentPoint, 1, 0.1, 0.1, 0.1, 0);
            }
        }

        // Son du rayon
        world.playSound(from, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.5f);
    }

    /**
     * Effet de désintégration quand un ennemi meurt
     */
    private void spawnDisintegrationEffect(World world, Location loc) {
        // Explosion de cendres et particules
        world.spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.15);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 20, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.ASH, loc, 80, 1.0, 1.0, 1.0, 0.1);
        world.spawnParticle(Particle.DUST, loc, 30, 0.6, 0.8, 0.6, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 50, 50), 1.5f));

        // Spirale montante de cendres
        for (double y = 0; y < 2.0; y += 0.15) {
            double angle = y * Math.PI * 2;
            double radius = 0.4 * (1 - y/2);
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc.clone().add(x, y, z),
                1, 0, 0, 0, 0);
        }

        // Sons
        world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.6f);
        world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.8f, 1.5f);

        // Message
        world.getPlayers().stream()
            .filter(p -> p.getLocation().distanceSquared(loc) < 400)
            .forEach(p -> p.sendMessage("§8§o*Un ennemi a été réduit en cendres*"));
    }

    /**
     * Particules de canalisation autour du joueur
     */
    private void spawnChannelParticles(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        // Aura de chaleur
        for (int i = 0; i < 3; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 0.5 + Math.random() * 0.3;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0, z), 1, 0, 0.2, 0, 0.02);
        }

        // Particules sur les mains
        Location handLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
        world.spawnParticle(Particle.DUST, handLoc, 3, 0.1, 0.1, 0.1, 0,
            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 150, 0), 0.6f));
    }

    /**
     * Obtient les dégâts de l'arme du joueur
     */
    private double getPlayerWeaponDamage(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 5.0;

        double damage = 5.0;
        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (modifiers != null) {
                for (var mod : modifiers) {
                    damage += mod.getAmount();
                }
            }
        }
        return Math.max(damage, 5.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) {
        BukkitTask task = activeChannels.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }
}

// ==================== ARCHONTE AQUATIQUE (DOLPHIN) ====================

/**
 * Passif: Sensibilité Élémentaire
 * Les dégâts élémentaires appliquent des stacks de vulnérabilité (+5% dégâts subis/type)
 * Max 4 types différents = +20% dégâts subis, durée 5s par stack
 */
class ElementalSensitivityPassive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double damagePerStack;  // +5% par type élémentaire
    private final int maxStacks;          // Max 4 types
    private final long stackDuration;     // 5 secondes

    // Track des stacks par ennemi: UUID ennemi -> Map<ElementType, expiration>
    private static final Map<UUID, Map<ElementType, Long>> enemyStacks = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(enemyStacks);
    }

    // Types élémentaires supportés
    public enum ElementType {
        FIRE("§6🔥", org.bukkit.Color.ORANGE),
        ICE("§b❄", org.bukkit.Color.AQUA),
        LIGHTNING("§e⚡", org.bukkit.Color.YELLOW),
        POISON("§2☠", org.bukkit.Color.GREEN);

        public final String icon;
        public final org.bukkit.Color color;

        ElementType(String icon, org.bukkit.Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public ElementalSensitivityPassive(String id, String name, String description,
                                        double damagePerStack, int maxStacks, long stackDurationSeconds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.damagePerStack = damagePerStack;
        this.maxStacks = maxStacks;
        this.stackDuration = stackDurationSeconds * 1000;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return true; }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        // Détecte le type de dégât élémentaire infligé
        // Ceci sera appelé via le système de combat avec le DamageType
    }

    /**
     * Applique un stack élémentaire à un ennemi
     * Appelé depuis le système de combat quand des dégâts élémentaires sont infligés
     */
    public void applyElementalStack(Player player, LivingEntity target, ElementType element) {
        UUID targetId = target.getUniqueId();
        long expiration = System.currentTimeMillis() + stackDuration;

        Map<ElementType, Long> stacks = enemyStacks.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());

        // Vérifie si c'est un nouveau type
        boolean isNewStack = !stacks.containsKey(element) || stacks.get(element) < System.currentTimeMillis();

        stacks.put(element, expiration);

        // Nettoie les stacks expirés
        stacks.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());

        // Affiche les stacks actifs
        int activeStacks = Math.min(stacks.size(), maxStacks);

        if (isNewStack && activeStacks > 0) {
            // Effet visuel sur la cible
            Location loc = target.getLocation().add(0, 1, 0);
            target.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(element.color, 1.2f));

            // Construit l'affichage des stacks
            StringBuilder stackDisplay = new StringBuilder("§7[");
            for (ElementType et : ElementType.values()) {
                if (stacks.containsKey(et) && stacks.get(et) >= System.currentTimeMillis()) {
                    stackDisplay.append(et.icon);
                }
            }
            stackDisplay.append("§7]");

            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§d✧ Sensibilité: " + stackDisplay + " §7(+" + String.format("%.0f", activeStacks * damagePerStack * 100) + "% dégâts)"
            ));

            // Son
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f + activeStacks * 0.2f);
        }
    }

    /**
     * Obtient le multiplicateur de dégâts bonus contre un ennemi
     */
    public double getDamageMultiplier(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        Map<ElementType, Long> stacks = enemyStacks.get(targetId);

        if (stacks == null || stacks.isEmpty()) {
            return 1.0;
        }

        // Nettoie les stacks expirés
        long now = System.currentTimeMillis();
        stacks.entrySet().removeIf(entry -> entry.getValue() < now);

        int activeStacks = Math.min(stacks.size(), maxStacks);
        return 1.0 + (activeStacks * damagePerStack);
    }

    /**
     * Vérifie le nombre de stacks actifs sur un ennemi
     */
    public int getActiveStacks(LivingEntity target) {
        UUID targetId = target.getUniqueId();
        Map<ElementType, Long> stacks = enemyStacks.get(targetId);

        if (stacks == null) return 0;

        long now = System.currentTimeMillis();
        stacks.entrySet().removeIf(entry -> entry.getValue() < now);

        return Math.min(stacks.size(), maxStacks);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) {
        // Nettoie les stacks de l'ennemi tué
        enemyStacks.remove(victim.getUniqueId());
    }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) { }
}

/**
 * Ultimate: Forme d'Archonte
 * Transformation en être d'énergie arcanique - scale x1.5, glowing violet
 * +30% dégâts, +150% armure/résistances, +6% dégâts par kill
 */
class ArchonFormActive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double baseDamageBonus;     // +30%
    private final double armorBonus;          // +150%
    private final double damagePerKill;       // +6%
    private final int durationTicks;          // 20 secondes
    private final ElementalSensitivityPassive linkedPassive;

    // Track des joueurs en forme d'archonte
    private static final Map<UUID, ArchonState> activeArchons = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(activeArchons);
    }

    private static class ArchonState {
        BukkitTask task;
        int killCount = 0;
        double originalScale = 1.0;

        void cancel() {
            if (task != null) task.cancel();
        }
    }

    public ArchonFormActive(String id, String name, String description,
                             double baseDamageBonus, double armorBonus,
                             double damagePerKill, int durationSeconds,
                             ElementalSensitivityPassive linkedPassive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseDamageBonus = baseDamageBonus;
        this.armorBonus = armorBonus;
        this.damagePerKill = damagePerKill;
        this.durationTicks = durationSeconds * 20;
        this.linkedPassive = linkedPassive;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();

        // Annule la transformation précédente si existante
        ArchonState existingState = activeArchons.remove(playerId);
        if (existingState != null) {
            existingState.cancel();
            revertTransformation(player, existingState);
        }

        // Obtient le plugin
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        // Crée le nouvel état
        ArchonState state = new ArchonState();

        // Sauvegarde le scale original
        var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            state.originalScale = scaleAttr.getBaseValue();
        }

        // Applique la transformation
        applyTransformation(player, state);

        // Message d'activation
        player.sendTitle("§5§l✦ ARCHONTE ✦", "§dForme d'énergie pure activée!", 5, 40, 10);
        player.sendMessage("§5§l⚡ FORME D'ARCHONTE ACTIVÉE!");
        player.sendMessage("§7» §a+30% dégâts §7| §b+150% armure §7| §e+6% par kill");

        // Sons et effets
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.2f);
        spawnArchonNovaEffect(player);

        // Timer de la transformation
        final int[] ticksRemaining = {durationTicks};

        state.task = new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifie si le joueur est toujours en ligne et vivant
                if (!player.isOnline() || player.isDead()) {
                    endArchonForm(player, state, "§cForme d'Archonte interrompue!");
                    return;
                }

                ticksRemaining[0]--;

                // Fin de la transformation
                if (ticksRemaining[0] <= 0) {
                    endArchonForm(player, state, "§5Forme d'Archonte terminée. §7(Kills: " + state.killCount + ")");
                    return;
                }

                // Effets visuels continus
                if (ticksRemaining[0] % 10 == 0) {
                    spawnArchonAura(player);
                }

                // Affichage du temps restant toutes les secondes
                if (ticksRemaining[0] % 20 == 0) {
                    int secondsLeft = ticksRemaining[0] / 20;
                    double totalDamageBonus = baseDamageBonus + (state.killCount * damagePerKill);
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§5✦ Archonte: §f" + secondsLeft + "s §7| §a+" +
                        String.format("%.0f", totalDamageBonus * 100) + "% dégâts §7| §eKills: " + state.killCount
                    ));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeArchons.put(playerId, state);
        return true;
    }

    /**
     * Applique la transformation d'Archonte
     */
    private void applyTransformation(Player player, ArchonState state) {
        // Scale x1.5
        var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.5);
        }

        // Glowing effet (violet via scoreboard team)
        applyGlowingEffect(player, true);

        // Bonus d'armure temporaire (via potion effect pour simplicité)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 2, false, false));
    }

    /**
     * Retire la transformation d'Archonte
     */
    private void revertTransformation(Player player, ArchonState state) {
        // Restaure le scale
        var scaleAttr = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(state.originalScale);
        }

        // Retire le glowing
        applyGlowingEffect(player, false);

        // Retire l'effet de résistance
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    /**
     * Applique/retire l'effet de glowing violet
     */
    private void applyGlowingEffect(Player player, boolean apply) {
        if (apply) {
            player.setGlowing(true);
            // Le glowing utilise la couleur de la team du joueur
            // Pour un effet violet, on pourrait utiliser une team temporaire
            // Mais pour simplicité, on utilise juste le glowing standard
        } else {
            player.setGlowing(false);
        }
    }

    /**
     * Termine la forme d'Archonte
     */
    private void endArchonForm(Player player, ArchonState state, String message) {
        UUID playerId = player.getUniqueId();
        activeArchons.remove(playerId);

        if (state.task != null) {
            state.task.cancel();
        }

        revertTransformation(player, state);

        player.sendMessage(message);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);

        // Effet de fin
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 50, 0.5, 0.8, 0.5, 0.1);
    }

    /**
     * Nova d'activation
     */
    private void spawnArchonNovaEffect(Player player) {
        Location center = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        // Onde de choc circulaire
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            for (double r = 0.5; r < 4; r += 0.5) {
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location particleLoc = center.clone().add(x, 0, z);

                world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 255), 1.5f));
            }
        }

        // Colonne centrale
        for (double y = 0; y < 3; y += 0.2) {
            Location colLoc = center.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, colLoc, 2, 0.1, 0, 0.1, 0.02);
            world.spawnParticle(Particle.PORTAL, colLoc, 5, 0.2, 0.2, 0.2, 0.5);
        }
    }

    /**
     * Aura continue de l'Archonte
     */
    private void spawnArchonAura(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        // Spirale violette autour du joueur
        double time = System.currentTimeMillis() / 200.0;
        for (int i = 0; i < 3; i++) {
            double angle = time + (i * Math.PI * 2 / 3);
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;

            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 3, 0.1, 0.3, 0.1, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 50, 255), 1.0f));
        }

        // Particules de portail
        world.spawnParticle(Particle.PORTAL, loc, 8, 0.4, 0.6, 0.4, 0.3);
    }

    /**
     * Vérifie si un joueur est en forme d'Archonte
     */
    public boolean isInArchonForm(Player player) {
        return activeArchons.containsKey(player.getUniqueId());
    }

    /**
     * Obtient le multiplicateur de dégâts total (base + kills)
     */
    public double getDamageMultiplier(Player player) {
        ArchonState state = activeArchons.get(player.getUniqueId());
        if (state == null) return 1.0;

        return 1.0 + baseDamageBonus + (state.killCount * damagePerKill);
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) {
        ArchonState state = activeArchons.get(player.getUniqueId());
        if (state != null) {
            state.killCount++;

            // Effet visuel de power-up
            Location loc = player.getLocation().add(0, 1.5, 0);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 10, 0.3, 0.3, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f + state.killCount * 0.05f);

            double totalBonus = baseDamageBonus + (state.killCount * damagePerKill);
            player.sendMessage("§5§l⚡ §dKill Archonte! §7Dégâts: §a+" + String.format("%.0f", totalBonus * 100) + "%");
        }
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) {
        ArchonState state = activeArchons.remove(player.getUniqueId());
        if (state != null) {
            state.cancel();
            revertTransformation(player, state);
        }
    }
}

// ==================== ANCRAGE DU NÉANT (ENDERMAN) ====================

/**
 * Passif: Gravité du Vide
 * Zone de 6 blocs autour du joueur
 * Ennemis: -20% vitesse, attraction vers le joueur
 * +15% dégâts contre les ennemis dans la zone
 */
class VoidGravityPassive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double zoneRadius;       // 6 blocs
    private final double slowPercent;      // -20%
    private final double pullStrength;     // Force d'attraction
    private final double damageBonus;      // +15%

    // Track des ennemis dans la zone par joueur
    private static final Map<UUID, Set<UUID>> enemiesInZone = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> zoneTasks = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(enemiesInZone);
        PassiveAbilityCleanup.registerForCleanup(zoneTasks);
    }

    public VoidGravityPassive(String id, String name, String description,
                               double zoneRadius, double slowPercent, double pullStrength, double damageBonus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.zoneRadius = zoneRadius;
        this.slowPercent = slowPercent;
        this.pullStrength = pullStrength;
        this.damageBonus = damageBonus;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public void applyPassive(Player player, PetData petData) {
        // La zone est gérée via onEquip avec un task répétitif
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();
        enemiesInZone.put(playerId, ConcurrentHashMap.newKeySet());

        // Obtient le plugin
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;

        // Crée le task de zone
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    return;
                }

                processZone(player, plugin);
            }
        }.runTaskTimer(plugin, 0L, 5L); // Toutes les 5 ticks (0.25s)

        zoneTasks.put(playerId, task);
    }

    /**
     * Traite la zone gravitationnelle
     */
    private void processZone(Player player, com.rinaorc.zombiez.ZombieZPlugin plugin) {
        Location center = player.getLocation();
        World world = player.getWorld();
        Set<UUID> currentEnemies = enemiesInZone.get(player.getUniqueId());
        if (currentEnemies == null) return;

        Set<UUID> foundEnemies = new HashSet<>();

        // Parcourt les entités dans la zone
        for (Entity entity : world.getNearbyEntities(center, zoneRadius, zoneRadius, zoneRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                double distance = living.getLocation().distance(center);
                if (distance <= zoneRadius && distance > 1.0) {
                    foundEnemies.add(entity.getUniqueId());

                    // Applique le slow
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 1, false, false));

                    // Attraction vers le joueur
                    Vector pullDirection = center.toVector().subtract(living.getLocation().toVector()).normalize();
                    double pullForce = pullStrength * (1 - distance / zoneRadius); // Plus fort quand proche
                    living.setVelocity(living.getVelocity().add(pullDirection.multiply(pullForce)));

                    // Particules de vide sur l'ennemi
                    world.spawnParticle(Particle.PORTAL, living.getLocation().add(0, 1, 0),
                        5, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }

        // Met à jour les ennemis dans la zone
        currentEnemies.clear();
        currentEnemies.addAll(foundEnemies);

        // Effet visuel de la zone
        spawnZoneEffect(player, world);
    }

    /**
     * Effet visuel de la zone gravitationnelle
     */
    private void spawnZoneEffect(Player player, World world) {
        Location center = player.getLocation().add(0, 0.1, 0);

        // Cercle de particules au sol
        double time = System.currentTimeMillis() / 500.0;
        for (int i = 0; i < 8; i++) {
            double angle = time + (i * Math.PI * 2 / 8);
            double x = Math.cos(angle) * zoneRadius;
            double z = Math.sin(angle) * zoneRadius;

            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(75, 0, 130), 1.0f));
        }

        // Spirale d'aspiration
        for (double r = zoneRadius; r > 0.5; r -= 1.5) {
            double angle = time * 2 + r;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;

            Location particleLoc = center.clone().add(x, 0.5, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Vérifie si un ennemi est dans la zone
     */
    public boolean isInZone(Player player, LivingEntity target) {
        Set<UUID> enemies = enemiesInZone.get(player.getUniqueId());
        return enemies != null && enemies.contains(target.getUniqueId());
    }

    /**
     * Obtient le multiplicateur de dégâts
     */
    public double getDamageMultiplier(Player player, LivingEntity target) {
        if (isInZone(player, target)) {
            return 1.0 + damageBonus;
        }
        return 1.0;
    }

    /**
     * Obtient le nombre d'ennemis dans la zone
     */
    public int getEnemyCount(Player player) {
        Set<UUID> enemies = enemiesInZone.get(player.getUniqueId());
        return enemies != null ? enemies.size() : 0;
    }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        // Le bonus de dégâts est appliqué via getDamageMultiplier
    }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    @Override
    public void onUnequip(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();
        enemiesInZone.remove(playerId);
        BukkitTask task = zoneTasks.remove(playerId);
        if (task != null) task.cancel();
    }
}

/**
 * Ultimate: Singularité
 * Crée un trou noir qui aspire tous les ennemis pendant 2s
 * Puis explosion de vide (300% dégâts) + dispersion violente
 */
class SingularityActive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final double pullRadius;       // 12 blocs
    private final double chargeDuration;   // 2 secondes
    private final double explosionDamage;  // 300% arme
    private final double knockbackForce;   // Force de dispersion
    private final VoidGravityPassive linkedPassive;

    // Track des singularités actives
    private static final Map<UUID, BukkitTask> activeSingularities = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(activeSingularities);
    }

    public SingularityActive(String id, String name, String description,
                              double pullRadius, double chargeDuration, double explosionDamage,
                              double knockbackForce, VoidGravityPassive linkedPassive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pullRadius = pullRadius;
        this.chargeDuration = chargeDuration;
        this.explosionDamage = explosionDamage;
        this.knockbackForce = knockbackForce;
        this.linkedPassive = linkedPassive;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();

        // Annule la singularité précédente si existante
        BukkitTask existingTask = activeSingularities.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Obtient le plugin et le ZombieManager
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;
        var zombieManager = plugin.getZombieManager();

        // Position du trou noir (devant le joueur)
        Location singularityLoc = player.getLocation().add(player.getLocation().getDirection().multiply(3)).add(0, 1.5, 0);
        World world = player.getWorld();

        // Calcul des dégâts
        double weaponDamage = getPlayerWeaponDamage(player);
        double statMultiplier = petData.getStatMultiplier();

        // Message d'activation
        player.sendMessage("§5§l✦ SINGULARITÉ CRÉÉE!");
        world.playSound(singularityLoc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        world.playSound(singularityLoc, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.3f);

        // Phase d'aspiration
        final int chargeTicks = (int)(chargeDuration * 20);
        final int[] ticksElapsed = {0};
        final Set<LivingEntity> caughtEnemies = ConcurrentHashMap.newKeySet();

        BukkitTask singularityTask = new BukkitRunnable() {
            @Override
            public void run() {
                ticksElapsed[0]++;

                // Vérifie si le joueur est toujours en ligne
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    activeSingularities.remove(playerId);
                    return;
                }

                // Phase d'aspiration
                if (ticksElapsed[0] <= chargeTicks) {
                    // Attire les ennemis
                    pullEnemies(world, singularityLoc, caughtEnemies);

                    // Effets visuels du trou noir
                    spawnBlackHoleEffect(world, singularityLoc, ticksElapsed[0], chargeTicks);

                    // Affichage du compte à rebours
                    if (ticksElapsed[0] % 20 == 0) {
                        int secondsLeft = (chargeTicks - ticksElapsed[0]) / 20;
                        player.sendActionBar(net.kyori.adventure.text.Component.text(
                            "§5✦ Singularité: §f" + (secondsLeft + 1) + "s §7| §e" + caughtEnemies.size() + " ennemis"
                        ));
                    }
                }
                // Explosion
                else if (ticksElapsed[0] == chargeTicks + 1) {
                    explodeSingularity(player, world, singularityLoc, caughtEnemies,
                        weaponDamage, statMultiplier, zombieManager);
                    cancel();
                    activeSingularities.remove(playerId);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeSingularities.put(playerId, singularityTask);
        return true;
    }

    /**
     * Attire les ennemis vers le trou noir
     */
    private void pullEnemies(World world, Location center, Set<LivingEntity> caughtEnemies) {
        for (Entity entity : world.getNearbyEntities(center, pullRadius, pullRadius, pullRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)
                && !(entity instanceof ArmorStand) && !living.isDead()) {

                caughtEnemies.add(living);

                // Force d'attraction
                Vector pullDirection = center.toVector().subtract(living.getLocation().toVector());
                double distance = pullDirection.length();

                if (distance > 0.5) {
                    pullDirection.normalize();
                    double pullForce = 0.5 * (1 - distance / pullRadius); // Plus fort quand proche
                    living.setVelocity(pullDirection.multiply(pullForce));
                }

                // Slow massif
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 3, false, false));

                // Particules sur l'ennemi
                world.spawnParticle(Particle.PORTAL, living.getLocation().add(0, 1, 0),
                    8, 0.3, 0.5, 0.3, 0.5);
            }
        }
    }

    /**
     * Effet visuel du trou noir
     */
    private void spawnBlackHoleEffect(World world, Location center, int ticksElapsed, int maxTicks) {
        double progress = (double) ticksElapsed / maxTicks;
        double radius = 1.5 + progress * 1.5; // Grossit avec le temps

        // Sphère de vide
        for (int i = 0; i < 20; i++) {
            double theta = Math.random() * Math.PI * 2;
            double phi = Math.random() * Math.PI;
            double x = Math.sin(phi) * Math.cos(theta) * radius;
            double y = Math.sin(phi) * Math.sin(theta) * radius;
            double z = Math.cos(phi) * radius;

            Location particleLoc = center.clone().add(x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(20, 0, 40), 1.5f));
        }

        // Spirale d'aspiration
        double time = ticksElapsed / 2.0;
        for (double r = pullRadius; r > radius; r -= 1.0) {
            double angle = time + r * 0.5;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;

            Location spiralLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.END_ROD, spiralLoc, 1, 0, 0, 0, 0);
        }

        // Centre brillant
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 15, 0.3, 0.3, 0.3, 0.1);

        // Son de charge
        if (ticksElapsed % 10 == 0) {
            world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f + (float)progress * 0.5f);
        }
    }

    /**
     * Explosion de la singularité
     */
    private void explodeSingularity(Player player, World world, Location center,
                                     Set<LivingEntity> caughtEnemies, double weaponDamage,
                                     double statMultiplier, com.rinaorc.zombiez.zombies.ZombieManager zombieManager) {
        double damage = weaponDamage * explosionDamage * statMultiplier;

        // Bonus si le passif est actif
        if (linkedPassive != null) {
            int enemiesInZone = linkedPassive.getEnemyCount(player);
            if (enemiesInZone > 0) {
                damage *= (1.0 + enemiesInZone * 0.05); // +5% par ennemi dans la zone passive
            }
        }

        // Inflige les dégâts et disperse les ennemis
        int killed = 0;
        for (LivingEntity target : caughtEnemies) {
            if (target.isDead()) continue;

            // Dégâts
            boolean targetKilled = false;
            if (zombieManager != null) {
                var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
                if (activeZombie != null) {
                    zombieManager.damageZombie(player, activeZombie, damage,
                        com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                    targetKilled = target.isDead() || target.getHealth() <= 0;
                } else {
                    target.damage(damage, player);
                    targetKilled = target.isDead();
                }
            } else {
                target.damage(damage, player);
                targetKilled = target.isDead();
            }

            if (targetKilled) {
                killed++;
            } else {
                // Dispersion violente (knockback)
                Vector knockback = target.getLocation().toVector().subtract(center.toVector()).normalize();
                knockback.setY(0.5); // Légère élévation
                target.setVelocity(knockback.multiply(knockbackForce));

                // Effet de dispersion
                world.spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
            }
        }

        // Effet d'explosion
        spawnExplosionEffect(world, center);

        // Message
        player.sendMessage("§5§l✦ SINGULARITÉ EXPLOSE! §7" + caughtEnemies.size() + " touchés, §c" + killed + " tués");
    }

    /**
     * Effet visuel de l'explosion
     */
    private void spawnExplosionEffect(World world, Location center) {
        // Onde de choc
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            for (double r = 0.5; r < pullRadius; r += 0.8) {
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location particleLoc = center.clone().add(x, 0, z);

                world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 255), 2.0f));
            }
        }

        // Explosion centrale
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 100, 0.5, 0.5, 0.5, 0.5);
        world.spawnParticle(Particle.END_ROD, center, 50, 1, 1, 1, 0.3);

        // Sons
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.8f);
        world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);
    }

    /**
     * Obtient les dégâts de l'arme du joueur
     */
    private double getPlayerWeaponDamage(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 5.0;

        double damage = 5.0;
        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (modifiers != null) {
                for (var mod : modifiers) {
                    damage += mod.getAmount();
                }
            }
        }
        return Math.max(damage, 5.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) {
        BukkitTask task = activeSingularities.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }
}

// ==================== GARDIEN LÉVITANT (SHULKER) ====================

/**
 * Passif: Balles de Shulker
 * Chaque 4ème attaque tire une balle de Shulker
 * La balle inflige 40% dégâts arme + Lévitation 2s
 * Les ennemis en lévitation subissent +20% dégâts
 */
class ShulkerBulletPassive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final int attacksRequired;      // 4 attaques
    private final double bulletDamagePercent; // 40% arme
    private final int levitationDuration;   // 2 secondes (en ticks)
    private final double levitationDamageBonus; // +20%

    // Track des attaques et ennemis lévités par joueur
    private static final Map<UUID, Integer> attackCounters = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> levitatingEnemies = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
        PassiveAbilityCleanup.registerForCleanup(levitatingEnemies);
    }

    public ShulkerBulletPassive(String id, String name, String description,
                                 int attacksRequired, double bulletDamagePercent,
                                 int levitationDurationSeconds, double levitationDamageBonus) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.attacksRequired = attacksRequired;
        this.bulletDamagePercent = bulletDamagePercent;
        this.levitationDuration = levitationDurationSeconds * 20;
        this.levitationDamageBonus = levitationDamageBonus;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return true; }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        UUID playerId = player.getUniqueId();

        // Incrémente le compteur d'attaques
        int count = attackCounters.getOrDefault(playerId, 0) + 1;
        attackCounters.put(playerId, count);

        // Vérifie si on doit tirer une balle
        if (count >= attacksRequired) {
            attackCounters.put(playerId, 0);
            fireShulkerBullet(player, target, damage, petData);
        }
    }

    /**
     * Tire une balle de Shulker sur la cible
     */
    private void fireShulkerBullet(Player player, LivingEntity target, double baseDamage, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return;
        var zombieManager = plugin.getZombieManager();

        UUID playerId = player.getUniqueId();
        World world = player.getWorld();
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Calcule les dégâts de la balle
        double weaponDamage = getPlayerWeaponDamage(player);
        double bulletDamage = weaponDamage * bulletDamagePercent * petData.getStatMultiplier();

        // Bonus si déjà en lévitation
        Set<UUID> levitated = levitatingEnemies.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        boolean wasLevitating = levitated.contains(target.getUniqueId());
        if (wasLevitating) {
            bulletDamage *= (1.0 + levitationDamageBonus);
        }

        // Animation de la balle (projectile visuel)
        Location startLoc = player.getLocation().add(0, 1.5, 0);
        spawnBulletTrail(world, startLoc, targetLoc);

        // Inflige les dégâts
        if (zombieManager != null) {
            var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
            if (activeZombie != null) {
                zombieManager.damageZombie(player, activeZombie, bulletDamage,
                    com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
            } else {
                target.damage(bulletDamage, player);
            }
        } else {
            target.damage(bulletDamage, player);
        }

        // Applique la lévitation
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDuration, 1, false, true));
        levitated.add(target.getUniqueId());

        // Planifie le retrait de la lévitation après la durée
        new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> lev = levitatingEnemies.get(playerId);
                if (lev != null) {
                    lev.remove(target.getUniqueId());
                }

                // Dégâts de chute simulés si l'ennemi est toujours vivant
                if (!target.isDead() && target.isValid()) {
                    double fallHeight = target.getFallDistance();
                    if (fallHeight > 3) {
                        double fallDamage = fallHeight * 2;
                        target.damage(fallDamage, player);

                        // Effet d'impact
                        world.spawnParticle(Particle.BLOCK, target.getLocation(), 20,
                            0.5, 0.1, 0.5, 0.1, org.bukkit.Material.PURPLE_CONCRETE.createBlockData());
                        world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 1.0f, 0.8f);
                    }
                }
            }
        }.runTaskLater(plugin, levitationDuration + 10);

        // Effet visuel et sonore
        world.spawnParticle(Particle.END_ROD, targetLoc, 15, 0.3, 0.5, 0.3, 0.1);
        world.playSound(targetLoc, Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.2f);

        // Message
        String bonusText = wasLevitating ? " §d(+20% Aérien!)" : "";
        player.sendActionBar(net.kyori.adventure.text.Component.text(
            "§5✦ Balle de Shulker! §f" + String.format("%.1f", bulletDamage) + " dégâts" + bonusText
        ));
    }

    /**
     * Crée la traînée visuelle de la balle
     */
    private void spawnBulletTrail(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();

        int segments = (int)(distance * 3);
        Location currentPoint = from.clone();

        for (int i = 0; i < segments; i++) {
            currentPoint.add(direction.clone().multiply(distance / segments));
            world.spawnParticle(Particle.DUST, currentPoint, 2, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 100, 255), 0.8f));
        }
    }

    /**
     * Vérifie si un ennemi est en lévitation
     */
    public boolean isLevitating(Player player, LivingEntity target) {
        Set<UUID> levitated = levitatingEnemies.get(player.getUniqueId());
        return levitated != null && levitated.contains(target.getUniqueId());
    }

    /**
     * Obtient le multiplicateur de dégâts contre un ennemi lévitant
     */
    public double getDamageMultiplier(Player player, LivingEntity target) {
        if (isLevitating(player, target)) {
            return 1.0 + levitationDamageBonus;
        }
        return 1.0;
    }

    /**
     * Obtient les dégâts de l'arme du joueur
     */
    private double getPlayerWeaponDamage(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 5.0;

        double damage = 5.0;
        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (modifiers != null) {
                for (var mod : modifiers) {
                    damage += mod.getAmount();
                }
            }
        }
        return Math.max(damage, 5.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) {
        // Nettoie l'ennemi de la liste des lévités
        Set<UUID> levitated = levitatingEnemies.get(player.getUniqueId());
        if (levitated != null) {
            levitated.remove(victim.getUniqueId());
        }
    }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    @Override
    public void onEquip(Player player, PetData petData) {
        levitatingEnemies.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        attackCounters.remove(player.getUniqueId());
        levitatingEnemies.remove(player.getUniqueId());
    }
}

/**
 * Ultimate: Barrage Gravitationnel
 * Tire 8 balles de Shulker en éventail
 * Après 3s: tous les ennemis lévitants sont ramenés au sol violemment
 */
class GravitationalBarrageActive implements PetAbility {

    private final String id;
    private final String name;
    private final String description;
    private final int bulletCount;          // 8 balles
    private final double bulletDamagePercent; // 60% arme
    private final int levitationDuration;   // 3 secondes
    private final double slamDamageMultiplier; // 150% dégâts de chute
    private final double doubleDamageIfLevitating; // x2 si déjà lévitant
    private final ShulkerBulletPassive linkedPassive;

    public GravitationalBarrageActive(String id, String name, String description,
                                       int bulletCount, double bulletDamagePercent,
                                       int levitationDurationSeconds, double slamDamageMultiplier,
                                       double doubleDamageIfLevitating, ShulkerBulletPassive linkedPassive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.bulletCount = bulletCount;
        this.bulletDamagePercent = bulletDamagePercent;
        this.levitationDuration = levitationDurationSeconds * 20;
        this.slamDamageMultiplier = slamDamageMultiplier;
        this.doubleDamageIfLevitating = doubleDamageIfLevitating;
        this.linkedPassive = linkedPassive;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public boolean activate(Player player, PetData petData) {
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;
        var zombieManager = plugin.getZombieManager();

        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        Vector baseDirection = playerLoc.getDirection();
        baseDirection.setY(0).normalize();

        // Calcule les dégâts
        double weaponDamage = getPlayerWeaponDamage(player);
        double statMultiplier = petData.getStatMultiplier();

        // Message d'activation
        player.sendMessage("§5§l✦ BARRAGE GRAVITATIONNEL!");
        world.playSound(playerLoc, Sound.ENTITY_SHULKER_OPEN, 1.5f, 0.8f);
        world.playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        // Track des ennemis touchés
        Set<LivingEntity> hitEnemies = ConcurrentHashMap.newKeySet();

        // Tire les balles en éventail
        double spreadAngle = Math.PI / 2; // 90 degrés total
        double angleStep = spreadAngle / (bulletCount - 1);
        double startAngle = -spreadAngle / 2;

        for (int i = 0; i < bulletCount; i++) {
            double angle = startAngle + (i * angleStep);
            Vector bulletDir = rotateVector(baseDirection.clone(), angle);

            // Trouve les ennemis dans cette direction
            fireBulletInDirection(player, playerLoc.clone().add(0, 1.5, 0), bulletDir,
                weaponDamage, statMultiplier, zombieManager, hitEnemies, world);
        }

        // Planifie le slam après la lévitation
        new BukkitRunnable() {
            @Override
            public void run() {
                slamEnemies(player, hitEnemies, weaponDamage, statMultiplier, zombieManager, world);
            }
        }.runTaskLater(plugin, levitationDuration);
        return true;
    }

    /**
     * Tire une balle dans une direction spécifique
     */
    private void fireBulletInDirection(Player player, Location start, Vector direction,
                                        double weaponDamage, double statMultiplier,
                                        com.rinaorc.zombiez.zombies.ZombieManager zombieManager,
                                        Set<LivingEntity> hitEnemies, World world) {
        double range = 15.0;

        // Trace un rayon pour trouver les ennemis
        for (double d = 1; d <= range; d += 0.5) {
            Location checkLoc = start.clone().add(direction.clone().multiply(d));

            // Particules de traînée
            world.spawnParticle(Particle.DUST, checkLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 100, 255), 1.0f));

            // Cherche des ennemis proches
            for (Entity entity : world.getNearbyEntities(checkLoc, 1.5, 1.5, 1.5)) {
                if (entity instanceof LivingEntity living && !(entity instanceof Player)
                    && !(entity instanceof ArmorStand) && !living.isDead()
                    && !hitEnemies.contains(living)) {

                    hitEnemies.add(living);

                    // Calcule les dégâts
                    double bulletDamage = weaponDamage * bulletDamagePercent * statMultiplier;

                    // Double dégâts si déjà en lévitation
                    if (linkedPassive != null && linkedPassive.isLevitating(player, living)) {
                        bulletDamage *= doubleDamageIfLevitating;
                        world.spawnParticle(Particle.ENCHANTED_HIT, living.getLocation().add(0, 1, 0),
                            20, 0.3, 0.5, 0.3, 0.2);
                    }

                    // Inflige les dégâts
                    if (zombieManager != null) {
                        var activeZombie = zombieManager.getActiveZombie(living.getUniqueId());
                        if (activeZombie != null) {
                            zombieManager.damageZombie(player, activeZombie, bulletDamage,
                                com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                        } else {
                            living.damage(bulletDamage, player);
                        }
                    } else {
                        living.damage(bulletDamage, player);
                    }

                    // Applique la lévitation
                    living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, levitationDuration, 2, false, true));

                    // Effet d'impact
                    world.spawnParticle(Particle.END_ROD, living.getLocation().add(0, 1, 0),
                        12, 0.3, 0.4, 0.3, 0.1);
                    world.playSound(living.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 0.8f, 1.3f);
                }
            }
        }
    }

    /**
     * Slam tous les ennemis au sol
     */
    private void slamEnemies(Player player, Set<LivingEntity> enemies,
                              double weaponDamage, double statMultiplier,
                              com.rinaorc.zombiez.zombies.ZombieManager zombieManager, World world) {
        int killed = 0;
        int slammed = 0;

        for (LivingEntity target : enemies) {
            if (target.isDead() || !target.isValid()) continue;

            slammed++;

            // Retire la lévitation et applique une vélocité vers le bas
            target.removePotionEffect(PotionEffectType.LEVITATION);
            target.setVelocity(new Vector(0, -2.5, 0));

            // Dégâts de slam
            double slamDamage = weaponDamage * slamDamageMultiplier * statMultiplier;

            // Planifie l'impact au sol
            Location targetLoc = target.getLocation();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isDead()) return;

                    // Inflige les dégâts d'impact
                    boolean targetKilled = false;
                    if (zombieManager != null) {
                        var activeZombie = zombieManager.getActiveZombie(target.getUniqueId());
                        if (activeZombie != null) {
                            zombieManager.damageZombie(player, activeZombie, slamDamage,
                                com.rinaorc.zombiez.zombies.DamageType.PHYSICAL, false);
                            targetKilled = target.isDead() || target.getHealth() <= 0;
                        } else {
                            target.damage(slamDamage, player);
                            targetKilled = target.isDead();
                        }
                    } else {
                        target.damage(slamDamage, player);
                        targetKilled = target.isDead();
                    }

                    // Stun
                    if (!targetKilled) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 127, false, false));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20, 128, false, false));
                    }

                    // Effet d'impact au sol
                    Location impactLoc = target.getLocation();
                    world.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
                    world.spawnParticle(Particle.BLOCK, impactLoc, 30, 1, 0.2, 1, 0.1,
                        org.bukkit.Material.PURPLE_CONCRETE.createBlockData());
                    world.spawnParticle(Particle.DUST, impactLoc, 20, 0.8, 0.3, 0.8, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(150, 50, 200), 1.5f));
                    world.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.2f, 0.6f);
                    world.playSound(impactLoc, Sound.ENTITY_PLAYER_BIG_FALL, 1.5f, 0.7f);
                }
            }.runTaskLater((com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ"), 5L);
        }

        // Message final
        player.sendMessage("§5§l✦ SLAM! §7" + slammed + " ennemis écrasés au sol!");

        // Son de fin
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.2f);
    }

    /**
     * Tourne un vecteur autour de l'axe Y
     */
    private Vector rotateVector(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    /**
     * Obtient les dégâts de l'arme du joueur
     */
    private double getPlayerWeaponDamage(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 5.0;

        double damage = 5.0;
        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (modifiers != null) {
                for (var mod : modifiers) {
                    damage += mod.getAmount();
                }
            }
        }
        return Math.max(damage, 5.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) { }
}

// ==================== EXALTED PET: SENTINELLE SONIQUE ====================

/**
 * Passif composite de la Sentinelle Sonique (EXALTED - WARDEN)
 * Combine deux passifs:
 * - Détection Sismique: Les attaquants sont marqués 8s, +25% dégâts sur eux
 * - Onde de Choc: Chaque 6ème attaque = onde sonique (40% dégâts AoE, 8 blocs)
 */
class SonicSentinelPassive implements PetAbility {
    private final String id;
    private final String name;
    private final String description;

    // Paramètres Détection Sismique
    private final double markDamageBonus;      // +25% dégâts sur marqués
    private final int markDuration;             // 8 secondes

    // Paramètres Onde de Choc
    private final int attacksForShockwave;      // 6 attaques
    private final double shockwaveDamagePercent; // 40% dégâts
    private final double shockwaveRadius;        // 8 blocs

    // Tracking
    private static final Map<UUID, Map<UUID, Long>> markedEnemies = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> attackCounters = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(markedEnemies);
        PassiveAbilityCleanup.registerForCleanup(attackCounters);
    }

    public SonicSentinelPassive(String id, String name, String description,
                                 double markDamageBonus, int markDuration,
                                 int attacksForShockwave, double shockwaveDamagePercent, double shockwaveRadius) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.markDamageBonus = markDamageBonus;
        this.markDuration = markDuration;
        this.attacksForShockwave = attacksForShockwave;
        this.shockwaveDamagePercent = shockwaveDamagePercent;
        this.shockwaveRadius = shockwaveRadius;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return true; }

    @Override
    public boolean activate(Player player, PetData petData) { return false; }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) {
        UUID playerId = player.getUniqueId();
        World world = player.getWorld();

        // Vérifier si la cible est marquée pour le bonus de dégâts
        double bonusDamage = 0;
        if (isMarked(player, target)) {
            double effectiveBonus = markDamageBonus * petData.getStatMultiplier();
            bonusDamage = damage * effectiveBonus;

            // Appliquer les dégâts bonus
            var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
            if (plugin != null && plugin.getZombieManager() != null) {
                var activeZombie = plugin.getZombieManager().getActiveZombie(target.getUniqueId());
                if (activeZombie != null) {
                    plugin.getZombieManager().damageZombie(player, activeZombie, bonusDamage, com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                }
            }

            // Effet visuel de dégâts bonus
            world.spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1);
        }

        // Compteur pour onde de choc
        int count = attackCounters.getOrDefault(playerId, 0) + 1;
        int effectiveAttacks = petData.getLevel() >= 5 ? attacksForShockwave - 1 : attacksForShockwave;

        if (count >= effectiveAttacks) {
            attackCounters.put(playerId, 0);
            triggerShockwave(player, target.getLocation(), damage, petData);
        } else {
            attackCounters.put(playerId, count);
        }
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        // Marquer l'attaquant (on utilise un système simplifié - marque les ennemis proches)
        UUID playerId = player.getUniqueId();
        Location loc = player.getLocation();
        World world = player.getWorld();

        // Marquer tous les ennemis hostiles proches (simule la détection de l'attaquant)
        for (Entity entity : world.getNearbyEntities(loc, 5, 3, 5)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !entity.isDead()) {
                markEnemy(player, living, petData);
            }
        }
    }

    /**
     * Marque un ennemi comme détecté
     */
    private void markEnemy(Player player, LivingEntity target, PetData petData) {
        UUID playerId = player.getUniqueId();
        UUID targetId = target.getUniqueId();

        Map<UUID, Long> playerMarks = markedEnemies.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        // Durée de marque (niveau 5+ = 12s au lieu de 8s)
        int effectiveDuration = petData.getLevel() >= 5 ? markDuration + 4 : markDuration;
        long expireTime = System.currentTimeMillis() + (effectiveDuration * 1000L);
        playerMarks.put(targetId, expireTime);

        // Effet visuel de détection
        World world = player.getWorld();
        Location targetLoc = target.getLocation().add(0, target.getHeight() + 0.5, 0);

        world.spawnParticle(Particle.SCULK_SOUL, targetLoc, 15, 0.3, 0.3, 0.3, 0.02);
        world.playSound(targetLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 1.2f);

        // Message
        player.sendMessage("§8§l⦿ §7Cible détectée: §c" + target.getName() + " §7(+25% dégâts)");
    }

    /**
     * Vérifie si un ennemi est marqué
     */
    public boolean isMarked(Player player, LivingEntity target) {
        Map<UUID, Long> playerMarks = markedEnemies.get(player.getUniqueId());
        if (playerMarks == null) return false;

        Long expireTime = playerMarks.get(target.getUniqueId());
        if (expireTime == null) return false;

        if (System.currentTimeMillis() > expireTime) {
            playerMarks.remove(target.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * Déclenche une onde de choc sonique
     */
    private void triggerShockwave(Player player, Location center, double baseDamage, PetData petData) {
        World world = player.getWorld();
        double effectiveDamagePercent = petData.getLevel() >= 5 ? shockwaveDamagePercent + 0.10 : shockwaveDamagePercent;
        double shockwaveDamage = baseDamage * effectiveDamagePercent * petData.getStatMultiplier();

        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");
        int hit = 0;

        // Expansion visuelle de l'onde
        new BukkitRunnable() {
            double radius = 1.0;
            int ticks = 0;

            @Override
            public void run() {
                if (radius > shockwaveRadius || ticks > 10) {
                    cancel();
                    return;
                }

                // Cercle de particules en expansion
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                    world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
                }

                radius += 1.5;
                ticks++;
            }
        }.runTaskTimer((com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ"), 0L, 2L);

        // Dégâts aux ennemis
        for (Entity entity : world.getNearbyEntities(center, shockwaveRadius, 4, shockwaveRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !entity.isDead()) {
                if (plugin != null && plugin.getZombieManager() != null) {
                    var activeZombie = plugin.getZombieManager().getActiveZombie(living.getUniqueId());
                    if (activeZombie != null) {
                        plugin.getZombieManager().damageZombie(player, activeZombie, shockwaveDamage, com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                        hit++;

                        // Knockback
                        Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                        knockback.setY(0.3);
                        living.setVelocity(knockback);
                    }
                }
            }
        }

        // Sons
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.8f);

        // Message
        if (hit > 0) {
            player.sendMessage("§8§l◉ §cOnde de Choc! §7" + hit + " ennemis frappés!");
        }
    }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) {
        player.sendMessage("§4§l⦿ SENTINELLE SONIQUE §7équipée!");
        player.sendMessage("§8  ► Détection Sismique: §7Attaquants marqués (+25% dégâts)");
        player.sendMessage("§8  ► Onde de Choc: §7Chaque 6ème attaque = onde sonique AoE");
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();
        markedEnemies.remove(playerId);
        attackCounters.remove(playerId);
    }
}

/**
 * Ultimate de la Sentinelle Sonique: Boom Sonique Dévastatrice
 * Charge 2s puis inflige 500% dégâts à tous les ennemis + stun 3s
 * Max stars: Désintègre les ennemis <20% HP et crée des ondes secondaires
 */
class SonicBoomActive implements PetAbility {
    private final String id;
    private final String name;
    private final String description;

    private final double chargeTime;            // 2 secondes
    private final double damageMultiplier;       // 500% dégâts
    private final double stunDuration;           // 3 secondes
    private final double radius;                 // Rayon d'effet
    private final double executeThreshold;       // Seuil d'exécution (max stars)

    private final SonicSentinelPassive passiveRef;

    private static final Map<UUID, Boolean> chargingPlayers = new ConcurrentHashMap<>();

    static {
        PassiveAbilityCleanup.registerForCleanup(chargingPlayers);
    }

    public SonicBoomActive(String id, String name, String description,
                           double chargeTime, double damageMultiplier, double stunDuration,
                           double radius, double executeThreshold, SonicSentinelPassive passiveRef) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.chargeTime = chargeTime;
        this.damageMultiplier = damageMultiplier;
        this.stunDuration = stunDuration;
        this.radius = radius;
        this.executeThreshold = executeThreshold;
        this.passiveRef = passiveRef;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID playerId = player.getUniqueId();

        // Éviter les activations multiples
        if (chargingPlayers.getOrDefault(playerId, false)) {
            return false;
        }

        chargingPlayers.put(playerId, true);
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Calculer les dégâts
        double weaponDamage = getPlayerWeaponDamage(player);
        double effectiveMultiplier = damageMultiplier * petData.getStatMultiplier();
        double totalDamage = weaponDamage * effectiveMultiplier;

        boolean hasStarPower = petData.getStarPower() > 0;

        // Message de charge
        player.sendMessage("§4§l⦿ BOOM SONIQUE §7en charge...");
        player.sendTitle("§4§l⦿ CHARGE ⦿", "§7Boom Sonique imminente...", 5, 40, 5);

        // Phase de charge avec effets visuels
        new BukkitRunnable() {
            int ticks = 0;
            final int chargeTicks = (int) (chargeTime * 20);

            @Override
            public void run() {
                if (ticks >= chargeTicks) {
                    cancel();
                    executeBoum(player, playerLoc, totalDamage, hasStarPower, petData);
                    chargingPlayers.put(playerId, false);
                    return;
                }

                // Vibrations autour du joueur pendant la charge
                double progress = (double) ticks / chargeTicks;

                // Cercle de particules qui se resserre
                double currentRadius = radius * (1 - progress * 0.5);
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                    double x = playerLoc.getX() + Math.cos(angle) * currentRadius;
                    double z = playerLoc.getZ() + Math.sin(angle) * currentRadius;
                    Location particleLoc = new Location(world, x, playerLoc.getY() + 0.5, z);
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }

                // Particules montantes vers le joueur
                world.spawnParticle(Particle.SCULK_SOUL, playerLoc.clone().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0.02);

                // Sons de charge crescendo
                if (ticks % 5 == 0) {
                    float pitch = 0.5f + (float) progress * 1.5f;
                    world.playSound(playerLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer((com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
        return true;
    }

    /**
     * Exécute la boom sonique après la charge
     */
    private void executeBoum(Player player, Location center, double damage, boolean hasStarPower, PetData petData) {
        World world = player.getWorld();
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");

        int hit = 0;
        int executed = 0;
        List<Location> secondaryWaveLocations = new ArrayList<>();

        // Effet visuel principal - explosion sonique massive
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 30, 2, 1, 2, 0);
        world.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1.5, 0), 100, 3, 2, 3, 0.1);
        world.spawnParticle(Particle.FLASH, center.clone().add(0, 2, 0), 3, 0, 0, 0, 0);

        // Ondes concentriques visuelles
        new BukkitRunnable() {
            double currentRadius = 2.0;
            int wave = 0;

            @Override
            public void run() {
                if (currentRadius > radius || wave > 5) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 24) {
                    double x = center.getX() + Math.cos(angle) * currentRadius;
                    double z = center.getZ() + Math.sin(angle) * currentRadius;
                    Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                    world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.BLOCK, particleLoc, 3, 0.2, 0.1, 0.2, 0,
                        Material.SCULK.createBlockData());
                }

                currentRadius += 3;
                wave++;
            }
        }.runTaskTimer((com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ"), 0L, 3L);

        // Sons dévastateurs
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.7f);
        world.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.5f, 0.5f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        world.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);

        // Dégâts aux ennemis
        for (Entity entity : world.getNearbyEntities(center, radius, 5, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !entity.isDead()) {

                // Vérifier si exécution (max stars et <20% HP)
                boolean shouldExecute = false;
                if (hasStarPower) {
                    double healthPercent = living.getHealth() / living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    if (healthPercent < executeThreshold) {
                        shouldExecute = true;
                    }
                }

                if (shouldExecute) {
                    // Exécution instantanée
                    if (plugin != null && plugin.getZombieManager() != null) {
                        var activeZombie = plugin.getZombieManager().getActiveZombie(living.getUniqueId());
                        if (activeZombie != null) {
                            plugin.getZombieManager().damageZombie(player, activeZombie, 99999, com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                        }
                    }
                    executed++;

                    // Effet de désintégration
                    Location entityLoc = living.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.SCULK_SOUL, entityLoc, 30, 0.5, 1, 0.5, 0.1);
                    world.spawnParticle(Particle.ASH, entityLoc, 50, 0.5, 1, 0.5, 0.05);
                    world.playSound(entityLoc, Sound.ENTITY_WARDEN_DEATH, 0.5f, 1.5f);

                    // Location pour onde secondaire
                    if (hasStarPower) {
                        secondaryWaveLocations.add(entityLoc);
                    }
                } else {
                    // Dégâts normaux + stun
                    if (plugin != null && plugin.getZombieManager() != null) {
                        // Bonus sur marqués
                        double effectiveDamage = damage;
                        if (passiveRef != null && passiveRef.isMarked(player, living)) {
                            effectiveDamage *= 1.25; // +25% sur marqués
                        }

                        var activeZombie = plugin.getZombieManager().getActiveZombie(living.getUniqueId());
                        if (activeZombie != null) {
                            plugin.getZombieManager().damageZombie(player, activeZombie, effectiveDamage, com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                        }
                    }
                    hit++;

                    // Stun (Slowness V + No AI temporaire simulé)
                    int stunTicks = (int) (stunDuration * 20);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunTicks, 254, false, false)); // Slow max
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunTicks, 254, false, false));

                    // Knockback massif
                    Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.0);
                    knockback.setY(0.5);
                    living.setVelocity(knockback);
                }
            }
        }

        // Ondes secondaires (max stars)
        if (hasStarPower && !secondaryWaveLocations.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Location loc : secondaryWaveLocations) {
                        triggerSecondaryWave(player, loc, damage * 0.3, petData);
                    }
                }
            }.runTaskLater((com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ"), 10L);
        }

        // Messages
        player.sendTitle("§4§l⦿ BOOM SONIQUE ⦿", "§7" + hit + " frappés, " + executed + " désintégrés", 5, 30, 10);
        player.sendMessage("§4§l⦿ BOOM SONIQUE DÉVASTATRICE! §7" + hit + " ennemis frappés, " + executed + " désintégrés!");

        if (hasStarPower && !secondaryWaveLocations.isEmpty()) {
            player.sendMessage("§8  ► §7Ondes secondaires déclenchées: " + secondaryWaveLocations.size());
        }
    }

    /**
     * Déclenche une onde secondaire (max stars)
     */
    private void triggerSecondaryWave(Player player, Location center, double damage, PetData petData) {
        World world = player.getWorld();
        var plugin = (com.rinaorc.zombiez.ZombieZPlugin) player.getServer().getPluginManager().getPlugin("ZombieZ");

        double secondaryRadius = 4.0;

        // Effet visuel
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double x = center.getX() + Math.cos(angle) * secondaryRadius;
            double z = center.getZ() + Math.sin(angle) * secondaryRadius;
            Location particleLoc = new Location(world, x, center.getY() + 0.3, z);
            world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
        }

        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);

        // Dégâts
        for (Entity entity : world.getNearbyEntities(center, secondaryRadius, 3, secondaryRadius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player) && !entity.isDead()) {
                if (plugin != null && plugin.getZombieManager() != null) {
                    var activeZombie = plugin.getZombieManager().getActiveZombie(living.getUniqueId());
                    if (activeZombie != null) {
                        plugin.getZombieManager().damageZombie(player, activeZombie, damage, com.rinaorc.zombiez.zombies.DamageType.MAGIC, false);
                    }
                }
            }
        }
    }

    /**
     * Obtient les dégâts de l'arme du joueur
     */
    private double getPlayerWeaponDamage(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 5.0;

        double damage = 5.0;
        var meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (modifiers != null) {
                for (var mod : modifiers) {
                    damage += mod.getAmount();
                }
            }
        }
        return Math.max(damage, 5.0);
    }

    @Override
    public void applyPassive(Player player, PetData petData) { }

    public void onKill(Player player, LivingEntity victim, PetData petData) { }

    public void onDamageDealt(Player player, LivingEntity target, double damage, PetData petData) { }

    public void onDamageReceived(Player player, double damage, PetData petData) { }

    @Override
    public void onEquip(Player player, PetData petData) { }

    @Override
    public void onUnequip(Player player, PetData petData) {
        chargingPlayers.remove(player.getUniqueId());
    }
}
