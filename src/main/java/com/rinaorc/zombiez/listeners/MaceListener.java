package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DPSTracker;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour le système de masse amélioré ZombieZ
 *
 * Fonctionnalités:
 * - Ground Pound (Smash au sol) : AoE après un saut
 * - Étourdissement (Stun) : Chance d'étourdir les ennemis
 * - Briseur d'Armure (Armor Shatter) : Réduit la défense des ennemis
 */
public class MaceListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // ==================== CONFIGURATION GROUND POUND ====================

    // Seuils de hauteur de chute (en blocs)
    private static final double FALL_THRESHOLD_MIN = 1.0;      // Minimum pour activer le smash
    private static final double FALL_THRESHOLD_MEDIUM = 3.0;   // Seuil pour bonus moyen
    private static final double FALL_THRESHOLD_HIGH = 5.0;     // Seuil pour bonus maximum

    // Bonus de dégâts selon la hauteur
    private static final double FALL_DAMAGE_BONUS_LOW = 1.25;     // +25% (1-3 blocs)
    private static final double FALL_DAMAGE_BONUS_MEDIUM = 1.50;  // +50% (3-5 blocs)
    private static final double FALL_DAMAGE_BONUS_HIGH = 2.00;    // +100% (5+ blocs)

    // Rayon de l'AoE selon la hauteur
    private static final double AOE_RADIUS_LOW = 2.0;      // Petit smash
    private static final double AOE_RADIUS_MEDIUM = 3.0;   // Smash moyen
    private static final double AOE_RADIUS_HIGH = 4.0;     // Grand smash

    // Dégâts AoE (pourcentage des dégâts de la cible principale)
    private static final double AOE_DAMAGE_PERCENT = 0.60;  // 60% des dégâts aux cibles secondaires

    // Cooldown du ground pound
    private static final long GROUND_POUND_COOLDOWN = 2000;  // 2 secondes

    // ==================== CONFIGURATION STUN ====================

    private static final double STUN_BASE_CHANCE = 15.0;        // 15% de base
    private static final double STUN_BONUS_ON_SMASH = 25.0;     // +25% sur smash (total 40%)
    private static final int STUN_DURATION_TICKS = 15;          // 0.75 seconde
    private static final int STUN_DURATION_SMASH_TICKS = 25;    // 1.25 secondes sur smash
    private static final long STUN_COOLDOWN_PER_TARGET = 3000;  // 3 secondes par cible

    // ==================== CONFIGURATION ARMOR SHATTER ====================

    private static final double ARMOR_SHATTER_PERCENT = 10.0;   // -10% armure par coup
    private static final double ARMOR_SHATTER_MAX = 30.0;       // Maximum -30%
    private static final long ARMOR_SHATTER_DURATION = 5000;    // 5 secondes

    // ==================== TRACKING MAPS ====================

    // Tracking de la hauteur de chute des joueurs
    private final Map<UUID, Double> playerFallDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerWasInAir = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastGroundPoundTime = new ConcurrentHashMap<>();

    // Tracking du stun par cible (targetUUID -> timestamp dernier stun)
    private final Map<UUID, Map<UUID, Long>> stunCooldowns = new ConcurrentHashMap<>();

    // Tracking de l'armor shatter par cible (targetUUID -> (attackerUUID, shatterData))
    private final Map<UUID, ArmorShatterData> armorShatterStacks = new ConcurrentHashMap<>();

    public MaceListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    /**
     * Tâche de nettoyage des données expirées
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // Nettoyer les armor shatter expirés
            armorShatterStacks.entrySet().removeIf(entry ->
                now - entry.getValue().lastApplied > ARMOR_SHATTER_DURATION
            );

            // Nettoyer les stun cooldowns expirés
            stunCooldowns.values().forEach(map ->
                map.entrySet().removeIf(entry -> now - entry.getValue() > STUN_COOLDOWN_PER_TARGET)
            );

        }, 100L, 100L); // Toutes les 5 secondes
    }

    // ==================== TRACKING DE LA CHUTE ====================

    /**
     * Track la chute du joueur pour le Ground Pound
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur tient une masse
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.MACE) {
            // Nettoyer les données si pas de masse
            playerFallDistance.remove(playerId);
            playerWasInAir.remove(playerId);
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean isOnGround = player.isOnGround();
        boolean wasInAir = playerWasInAir.getOrDefault(playerId, false);

        // Si le joueur est en l'air et descend
        if (!isOnGround) {
            playerWasInAir.put(playerId, true);

            // Accumuler la distance de chute
            double yDiff = from.getY() - to.getY();
            if (yDiff > 0) {
                double currentFall = playerFallDistance.getOrDefault(playerId, 0.0);
                playerFallDistance.put(playerId, currentFall + yDiff);
            }
        }
        // Si le joueur vient d'atterrir
        else if (wasInAir) {
            // On garde la distance de chute pour la prochaine attaque
            // Elle sera reset après l'attaque ou après un délai
            playerWasInAir.put(playerId, false);

            // Programmer le reset de la distance si pas d'attaque dans 500ms
            double fallDist = playerFallDistance.getOrDefault(playerId, 0.0);
            if (fallDist >= FALL_THRESHOLD_MIN) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Reset seulement si le joueur est toujours au sol
                    if (player.isOnline() && player.isOnGround()) {
                        Double currentFall = playerFallDistance.get(playerId);
                        // Seulement reset si la valeur n'a pas changé (pas de nouveau saut)
                        if (currentFall != null && currentFall.equals(fallDist)) {
                            playerFallDistance.remove(playerId);
                        }
                    }
                }, 10L); // 500ms
            }
        }
    }

    // ==================== GROUND POUND (SMASH AU SOL) ====================

    /**
     * Intercepte les attaques avec la masse pour appliquer les mécaniques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return; // Pas de PvP avec ces mécaniques

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() != Material.MACE) return;

        UUID playerId = player.getUniqueId();

        // Récupérer la distance de chute accumulée
        double fallDistance = playerFallDistance.getOrDefault(playerId, 0.0);
        boolean isGroundPound = fallDistance >= FALL_THRESHOLD_MIN && !isOnGroundPoundCooldown(player);

        // Variables pour les bonus
        double damageMultiplier = 1.0;
        double aoeRadius = 0;
        boolean triggerSmash = false;
        int stunDuration = 0;

        // ==================== CALCUL DU GROUND POUND ====================
        if (isGroundPound) {
            // Déterminer le niveau du smash
            if (fallDistance >= FALL_THRESHOLD_HIGH) {
                damageMultiplier = FALL_DAMAGE_BONUS_HIGH;
                aoeRadius = AOE_RADIUS_HIGH;
                stunDuration = STUN_DURATION_SMASH_TICKS;
                triggerSmash = true;
            } else if (fallDistance >= FALL_THRESHOLD_MEDIUM) {
                damageMultiplier = FALL_DAMAGE_BONUS_MEDIUM;
                aoeRadius = AOE_RADIUS_MEDIUM;
                stunDuration = STUN_DURATION_SMASH_TICKS;
                triggerSmash = true;
            } else {
                damageMultiplier = FALL_DAMAGE_BONUS_LOW;
                aoeRadius = AOE_RADIUS_LOW;
                stunDuration = STUN_DURATION_TICKS;
                triggerSmash = true;
            }

            // Appliquer le cooldown et reset la distance
            lastGroundPoundTime.put(playerId, System.currentTimeMillis());
            playerFallDistance.remove(playerId);
        }

        // ==================== APPLIQUER LES BONUS DE DÉGÂTS ====================
        if (damageMultiplier > 1.0) {
            double newDamage = event.getDamage() * damageMultiplier;
            event.setDamage(newDamage);

            // Marquer pour le CombatListener
            target.setMetadata("zombiez_mace_smash", new FixedMetadataValue(plugin, true));
            target.setMetadata("zombiez_mace_multiplier", new FixedMetadataValue(plugin, damageMultiplier));
        }

        // ==================== ARMOR SHATTER ====================
        applyArmorShatter(player, target);

        // ==================== STUN ====================
        double stunChance = STUN_BASE_CHANCE;
        if (triggerSmash) {
            stunChance += STUN_BONUS_ON_SMASH;
        }

        if (random.nextDouble() * 100 < stunChance) {
            applyStun(player, target, triggerSmash ? STUN_DURATION_SMASH_TICKS : STUN_DURATION_TICKS);
        }

        // ==================== GROUND POUND AoE ====================
        if (triggerSmash && aoeRadius > 0) {
            double finalDamage = event.getDamage();
            double finalAoeRadius = aoeRadius;
            int finalStunDuration = stunDuration;
            double finalFallDistance = fallDistance;

            // Exécuter l'AoE après les dégâts initiaux
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                performGroundPoundAoE(player, target.getLocation(), finalDamage, finalAoeRadius,
                    finalStunDuration, finalFallDistance, target);
            });
        }
    }

    /**
     * Effectue l'AoE du Ground Pound
     */
    private void performGroundPoundAoE(Player player, Location center, double baseDamage,
                                        double radius, int stunDuration, double fallDistance,
                                        LivingEntity primaryTarget) {
        World world = center.getWorld();
        if (world == null) return;

        // Récupérer les stats du joueur pour les dégâts AoE
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        var skillManager = plugin.getSkillTreeManager();

        // Calculer les dégâts AoE
        double aoeDamage = baseDamage * AOE_DAMAGE_PERCENT;

        // Trouver les cibles dans le rayon
        Collection<Entity> nearbyEntities = world.getNearbyEntities(center, radius, radius, radius);
        int hitCount = 0;

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == player) continue;
            if (entity instanceof Player) continue;
            if (entity == primaryTarget) continue; // Déjà touché
            if (living.isDead()) continue;

            // Vérifier la distance réelle (sphérique)
            double distance = entity.getLocation().distance(center);
            if (distance > radius) continue;

            // Calculer les dégâts avec falloff basé sur la distance
            double distanceMultiplier = 1.0 - (distance / radius) * 0.3; // 70-100% selon distance
            double targetDamage = aoeDamage * distanceMultiplier;

            // Marquer comme dégâts secondaires
            living.setMetadata("zombiez_secondary_damage", new FixedMetadataValue(plugin, true));
            living.setMetadata("zombiez_mace_smash", new FixedMetadataValue(plugin, true));

            // Appliquer les dégâts
            living.damage(targetDamage, player);

            // Appliquer le stun
            applyStun(player, living, stunDuration);

            // Appliquer l'armor shatter
            applyArmorShatter(player, living);

            // Knockback vers l'extérieur
            Vector knockback = living.getLocation().toVector().subtract(center.toVector()).normalize();
            knockback.setY(0.3);
            knockback.multiply(0.5 + (fallDistance / 10.0)); // Plus de knockback avec plus de chute
            living.setVelocity(living.getVelocity().add(knockback));

            // DPS tracking
            DPSTracker.getInstance().recordDamage(player, targetDamage);

            // Enregistrer pour le loot
            living.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Mettre à jour l'affichage de vie si c'est un mob ZombieZ
            if (plugin.getZombieManager().isZombieZMob(living)) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (living.isValid()) {
                        plugin.getZombieManager().updateZombieHealthDisplay(living);
                    }
                });
            }

            hitCount++;
        }

        // ==================== EFFETS VISUELS ET SONORES ====================
        spawnGroundPoundEffects(center, radius, fallDistance);

        // Feedback au joueur
        String smashLevel;
        if (fallDistance >= FALL_THRESHOLD_HIGH) {
            smashLevel = "§6§l⚡ SMASH DÉVASTATEUR !";
            player.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 0.8f);
        } else if (fallDistance >= FALL_THRESHOLD_MEDIUM) {
            smashLevel = "§e⚡ Smash Puissant !";
            player.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 1.0f);
        } else {
            smashLevel = "§7⚡ Smash";
            player.playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_AIR, 0.8f, 1.2f);
        }

        if (hitCount > 0) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                smashLevel + " §7(" + (hitCount + 1) + " cibles)"
            ));
        } else {
            player.sendActionBar(net.kyori.adventure.text.Component.text(smashLevel));
        }
    }

    /**
     * Génère les effets visuels du Ground Pound
     */
    private void spawnGroundPoundEffects(Location center, double radius, double fallDistance) {
        World world = center.getWorld();
        if (world == null) return;

        // Onde de choc circulaire
        int particleCount = (int) (radius * 15);
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location particleLoc = center.clone().add(x, 0.1, z);

            world.spawnParticle(Particle.EXPLOSION, particleLoc, 1, 0, 0, 0, 0);
        }

        // Particules de débris au centre
        world.spawnParticle(Particle.BLOCK, center, (int) (radius * 10),
            radius * 0.5, 0.3, radius * 0.5, 0.1,
            Material.STONE.createBlockData());

        // Nuage de poussière
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 0.5, 0),
            (int) (radius * 5), radius * 0.3, 0.2, radius * 0.3, 0.02);

        // Effet de fissure (lignes radiales)
        if (fallDistance >= FALL_THRESHOLD_MEDIUM) {
            int lineCount = fallDistance >= FALL_THRESHOLD_HIGH ? 8 : 5;
            for (int i = 0; i < lineCount; i++) {
                double angle = (2 * Math.PI * i) / lineCount;
                for (double d = 0.5; d <= radius; d += 0.3) {
                    double x = Math.cos(angle) * d;
                    double z = Math.sin(angle) * d;
                    Location lineLoc = center.clone().add(x, 0.05, z);
                    world.spawnParticle(Particle.CRIT, lineLoc, 1, 0.05, 0, 0.05, 0);
                }
            }
        }

        // Éclair visuel pour les gros smash
        if (fallDistance >= FALL_THRESHOLD_HIGH) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0),
                30, radius * 0.5, 0.5, radius * 0.5, 0.1);
        }
    }

    // ==================== SYSTÈME DE STUN ====================

    /**
     * Applique l'étourdissement à une cible
     */
    private void applyStun(Player attacker, LivingEntity target, int durationTicks) {
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Vérifier le cooldown par cible
        Map<UUID, Long> attackerStunCooldowns = stunCooldowns.computeIfAbsent(attackerId, k -> new ConcurrentHashMap<>());
        Long lastStun = attackerStunCooldowns.get(targetId);

        if (lastStun != null && System.currentTimeMillis() - lastStun < STUN_COOLDOWN_PER_TARGET) {
            return; // Encore en cooldown pour cette cible
        }

        // Appliquer le stun
        attackerStunCooldowns.put(targetId, System.currentTimeMillis());

        // Effets de potion pour le stun
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 4, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 2, false, false));

        // Désactiver l'IA temporairement pour les mobs
        if (target instanceof Mob mob) {
            mob.setAI(false);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (mob.isValid() && !mob.isDead()) {
                    mob.setAI(true);
                }
            }, durationTicks);
        }

        // Effets visuels
        target.getWorld().spawnParticle(Particle.CRIT_MAGIC,
            target.getLocation().add(0, target.getHeight() + 0.3, 0),
            10, 0.3, 0.2, 0.3, 0.05);

        // Son d'étourdissement
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 0.6f, 2.0f);

        // Marquer comme stun pour d'autres systèmes
        target.setMetadata("zombiez_stunned", new FixedMetadataValue(plugin, true));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            target.removeMetadata("zombiez_stunned", plugin);
        }, durationTicks);
    }

    // ==================== SYSTÈME D'ARMOR SHATTER ====================

    /**
     * Applique la réduction d'armure à une cible
     */
    private void applyArmorShatter(Player attacker, LivingEntity target) {
        UUID targetId = target.getUniqueId();

        ArmorShatterData shatterData = armorShatterStacks.computeIfAbsent(targetId,
            k -> new ArmorShatterData());

        // Ajouter un stack
        shatterData.addStack(ARMOR_SHATTER_PERCENT, ARMOR_SHATTER_MAX);

        // Marquer la cible avec le débuff pour que le CombatListener puisse l'utiliser
        target.setMetadata("zombiez_armor_shatter", new FixedMetadataValue(plugin, shatterData.totalShatter));

        // Effets visuels si nouveau stack
        if (shatterData.stacks <= 3) { // Limiter les particules
            target.getWorld().spawnParticle(Particle.ITEM,
                target.getLocation().add(0, 1, 0),
                5, 0.3, 0.3, 0.3, 0.05,
                new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE));

            // Son de bris d'armure
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.5f, 0.5f);
        }
    }

    /**
     * Récupère le pourcentage de réduction d'armure d'une cible
     */
    public double getArmorShatterPercent(LivingEntity target) {
        ArmorShatterData data = armorShatterStacks.get(target.getUniqueId());
        if (data == null) return 0.0;

        // Vérifier si expiré
        if (System.currentTimeMillis() - data.lastApplied > ARMOR_SHATTER_DURATION) {
            armorShatterStacks.remove(target.getUniqueId());
            return 0.0;
        }

        return data.totalShatter;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si le joueur est en cooldown de Ground Pound
     */
    private boolean isOnGroundPoundCooldown(Player player) {
        Long lastSmash = lastGroundPoundTime.get(player.getUniqueId());
        if (lastSmash == null) return false;
        return System.currentTimeMillis() - lastSmash < GROUND_POUND_COOLDOWN;
    }

    /**
     * Obtient le cooldown restant du Ground Pound
     */
    public long getGroundPoundCooldownRemaining(Player player) {
        Long lastSmash = lastGroundPoundTime.get(player.getUniqueId());
        if (lastSmash == null) return 0;
        return Math.max(0, GROUND_POUND_COOLDOWN - (System.currentTimeMillis() - lastSmash));
    }

    /**
     * Obtient la hauteur de chute actuelle d'un joueur
     */
    public double getCurrentFallDistance(Player player) {
        return playerFallDistance.getOrDefault(player.getUniqueId(), 0.0);
    }

    // ==================== NETTOYAGE ====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanupPlayer(UUID playerId) {
        playerFallDistance.remove(playerId);
        playerWasInAir.remove(playerId);
        lastGroundPoundTime.remove(playerId);
        stunCooldowns.remove(playerId);
    }

    // ==================== CLASSE INTERNE ====================

    /**
     * Données de l'armor shatter pour une cible
     */
    private static class ArmorShatterData {
        double totalShatter = 0.0;
        int stacks = 0;
        long lastApplied = 0;

        void addStack(double amount, double max) {
            totalShatter = Math.min(max, totalShatter + amount);
            stacks++;
            lastApplied = System.currentTimeMillis();
        }
    }
}
