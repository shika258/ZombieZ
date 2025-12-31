package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DPSTracker;
import com.rinaorc.zombiez.combat.PacketDamageIndicator;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les événements de combat
 * Intègre: Momentum, Stats d'items, SkillTree, Effets visuels
 */
public class CombatListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // Tracking des joueurs qui n'ont pas pris de dégâts depuis leur dernier kill
    // Si un joueur est dans ce set, il n'a pas pris de dégâts depuis son dernier kill
    private final Set<UUID> playersWithoutDamage = ConcurrentHashMap.newKeySet();

    // Tracking du temps de la dernière attaque pour chaque joueur (en millisecondes)
    // Utilisé pour calculer le cooldown manuellement car player.getAttackCooldown()
    // renvoie 1.0 après le reset effectué par Minecraft avant l'événement
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();

    // Mode debug pour afficher les valeurs de cooldown dans la console
    private static final boolean DEBUG_COOLDOWN = false;

    // ============ CONFIGURATION DU SYSTÈME DE COOLDOWN PUNITIF ============
    // Seuil minimum de charge en dessous duquel les dégâts sont plafonnés
    private static final double COOLDOWN_MIN_THRESHOLD = 0.20; // 20%
    // Multiplicateur de dégâts appliqué en dessous du seuil minimum
    private static final double COOLDOWN_MIN_DAMAGE_MULT = 0.05; // 5% des dégâts

    public CombatListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startHealthRegenTask();
    }

    /**
     * Tâche de régénération passive de vie basée sur la stat HEALTH_REGEN
     * S'exécute toutes les secondes et soigne les joueurs selon leur stat
     */
    private void startHealthRegenTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // Ne pas régénérer si le joueur est mort ou en mode spectateur
                if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    continue;
                }

                // Récupérer la stat HEALTH_REGEN
                Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
                double healthRegen = playerStats.getOrDefault(StatType.HEALTH_REGEN, 0.0);

                // Ajouter la regen du SkillTree si disponible
                var skillManager = plugin.getSkillTreeManager();
                if (skillManager != null) {
                    healthRegen += skillManager.getSkillBonus(player,
                        com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus.REGEN);
                }

                if (healthRegen > 0) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();

                    // Ne pas soigner au-delà du max
                    if (currentHealth < maxHealth) {
                        double newHealth = Math.min(maxHealth, currentHealth + healthRegen);
                        player.setHealth(newHealth);

                        // Effet visuel discret si regen significative (> 0.5 par seconde)
                        if (healthRegen >= 0.5 && currentHealth < maxHealth - 0.1) {
                            player.getWorld().spawnParticle(Particle.HEART,
                                player.getLocation().add(0, 2.2, 0), 1, 0.1, 0, 0.1, 0);
                        }
                    }
                }
            }
        }, 20L, 20L); // Toutes les secondes
    }

    /**
     * Calcule le cooldown d'attaque d'un joueur manuellement.
     * Cette méthode contourne le bug où player.getAttackCooldown() renvoie toujours 1.0
     * car Minecraft reset le cooldown AVANT de déclencher l'événement de dégâts.
     *
     * La formule Minecraft est:
     * - Temps de recharge complet = 1 / attackSpeed (en secondes)
     * - Cooldown ratio = min(1.0, tempsÉcoulé * attackSpeed)
     *
     * @param player Le joueur dont on calcule le cooldown
     * @return Une valeur entre 0.0 (spam instantané) et 1.0 (pleinement rechargé)
     */
    private float calculateAttackCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Récupérer le temps de la dernière attaque
        Long lastTime = lastAttackTime.get(playerId);

        // Mettre à jour le temps de la dernière attaque pour la prochaine fois
        lastAttackTime.put(playerId, currentTime);

        // Si c'est la première attaque, considérer comme pleinement rechargé
        if (lastTime == null) {
            return 1.0f;
        }

        // Calculer le temps écoulé en secondes
        double elapsedSeconds = (currentTime - lastTime) / 1000.0;

        // Récupérer la vitesse d'attaque effective du joueur
        // La valeur de base est 4.0 pour une main vide, les armes peuvent la modifier
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.ATTACK_SPEED);
        double attackSpeed = attackSpeedAttr != null ? attackSpeedAttr.getValue() : 4.0;

        // Calculer le ratio de cooldown
        // À attackSpeed = 4.0, le temps de recharge complet = 0.25s (5 ticks / 20)
        // Minecraft utilise: cooldown = min(1.0, (ticks * 20 * attackSpeed + 0.5) / 20)
        // Simplifié en secondes: cooldown = min(1.0, elapsedSeconds * attackSpeed)
        double cooldownRatio = Math.min(1.0, elapsedSeconds * attackSpeed);

        if (DEBUG_COOLDOWN) {
            plugin.getLogger().info(String.format(
                "[COOLDOWN DEBUG] %s: elapsed=%.3fs, attackSpeed=%.2f, cooldown=%.2f (%.0f%%)",
                player.getName(), elapsedSeconds, attackSpeed, cooldownRatio, cooldownRatio * 100
            ));
        }

        return (float) cooldownRatio;
    }

    /**
     * Applique une pénalité de dégâts basée sur le ratio de cooldown.
     *
     * Le système est conçu pour punir sévèrement le spam de clics :
     * - Courbe QUADRATIQUE : 50% charge = 25% dégâts (au lieu de 50% linéaire)
     * - Seuil MINIMUM : En dessous de 20% de charge, dégâts plafonnés à 5%
     *
     * Comparaison DPS (sur 1 seconde avec attackSpeed = 4.0) :
     * - Spam (4 attaques à 25%) : 4 × 0.0625 = 25% DPS effectif
     * - Timing parfait (1 attaque à 100%) : 1.0² = 100% DPS effectif
     *
     * @param cooldownRatio Le ratio de cooldown linéaire (0.0 à 1.0)
     * @return Le multiplicateur de dégâts après pénalité (0.05 à 1.0)
     */
    private double applyAttackCooldownPenalty(double cooldownRatio) {
        // En dessous du seuil minimum, les dégâts sont fortement réduits
        if (cooldownRatio < COOLDOWN_MIN_THRESHOLD) {
            return COOLDOWN_MIN_DAMAGE_MULT;
        }

        // Courbe quadratique : pénalise les attaques partiellement chargées
        // 50% charge → 25% dégâts
        // 75% charge → 56% dégâts
        // 90% charge → 81% dégâts
        return cooldownRatio * cooldownRatio;
    }

    /**
     * Extrait le joueur depuis un damager (direct ou projectile)
     */
    private Player getPlayerFromDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    /**
     * Gère tous les types de dégâts sur les mobs ZombieZ (feu, chute, etc.)
     * pour mettre à jour leur affichage de vie
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnyEntityDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();

        // Ne traiter que les mobs ZombieZ (zombies, squelettes, etc.)
        if (!(victim instanceof LivingEntity livingVictim)) return;
        if (!plugin.getZombieManager().isZombieZMob(livingVictim)) return;

        // Ne pas traiter les dégâts d'entité (déjà gérés par onEntityDamage)
        if (event instanceof EntityDamageByEntityEvent) return;

        // Mettre à jour l'affichage de vie au tick suivant
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (livingVictim.isValid()) {
                plugin.getZombieManager().updateZombieHealthDisplay(livingVictim);
            }
        });
    }

    /**
     * Track les dégâts reçus par les joueurs pour KILLS_NO_DAMAGE
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Le joueur a pris des dégâts - retirer du set
        playersWithoutDamage.remove(player.getUniqueId());
    }

    /**
     * Gère les résistances élémentaires (Feu, Poison, Lightning, Ice)
     * Réduit les dégâts selon les stats du joueur
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onElementalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Récupérer les stats de résistance du joueur
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        EntityDamageEvent.DamageCause cause = event.getCause();
        double resistance = 0.0;
        String resistType = null;

        switch (cause) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> {
                resistance = playerStats.getOrDefault(StatType.FIRE_RESISTANCE, 0.0);
                resistType = "§6🔥 Résist. Feu";
            }
            case FREEZE -> {
                resistance = playerStats.getOrDefault(StatType.ICE_RESISTANCE, 0.0);
                resistType = "§b❄ Résist. Glace";
            }
            case LIGHTNING -> {
                resistance = playerStats.getOrDefault(StatType.LIGHTNING_RESISTANCE, 0.0);
                resistType = "§e⚡ Résist. Foudre";
            }
            case POISON -> {
                resistance = playerStats.getOrDefault(StatType.POISON_RESISTANCE, 0.0);
                resistType = "§2☠ Résist. Poison";
            }
        }

        // Appliquer la réduction si résistance > 0
        if (resistance > 0) {
            // Cap à 80% de réduction
            double reductionPercent = Math.min(80.0, resistance);
            double reductionMultiplier = 1.0 - (reductionPercent / 100.0);
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * reductionMultiplier;

            event.setDamage(reducedDamage);

            // Effet visuel si réduction significative (> 25%)
            if (reductionPercent >= 25.0 && originalDamage > 1.0) {
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /**
     * MONITOR: Affiche l'indicateur de dégâts APRÈS toutes les modifications de talents
     * Ce handler s'exécute en DERNIER, donc event.getDamage() contient les dégâts finaux
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDisplayMonitor(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        // Vérifier si on doit afficher un indicateur (metadata définie par les handlers HIGH)
        if (!victim.hasMetadata("zombiez_show_indicator")) return;

        // Récupérer les infos stockées
        boolean isCritical = victim.hasMetadata("zombiez_damage_critical") &&
                             victim.getMetadata("zombiez_damage_critical").get(0).asBoolean();
        boolean isHeadshot = victim.hasMetadata("zombiez_damage_headshot") &&
                             victim.getMetadata("zombiez_damage_headshot").get(0).asBoolean();
        Player viewer = null;

        if (victim.hasMetadata("zombiez_damage_viewer")) {
            String viewerUuid = victim.getMetadata("zombiez_damage_viewer").get(0).asString();
            viewer = plugin.getServer().getPlayer(java.util.UUID.fromString(viewerUuid));
        }

        // DÉGÂTS FINAUX après toutes les modifications (talents inclus)
        double finalDamage = event.getFinalDamage();

        // Afficher l'indicateur avec les dégâts RÉELS (via packets virtuels ProtocolLib)
        if (isHeadshot) {
            PacketDamageIndicator.displayHeadshot(plugin, victim.getLocation().add(0, victim.getHeight(), 0), finalDamage, viewer);
        } else {
            PacketDamageIndicator.display(plugin, victim.getLocation(), finalDamage, isCritical, viewer);
        }

        // Nettoyer les metadata
        victim.removeMetadata("zombiez_show_indicator", plugin);
        victim.removeMetadata("zombiez_damage_critical", plugin);
        victim.removeMetadata("zombiez_damage_headshot", plugin);
        victim.removeMetadata("zombiez_damage_viewer", plugin);
    }

    /**
     * Gère les dégâts entre entités
     * Priorité HIGHEST pour que nos multiplicateurs (cooldown, stats) soient appliqués après d'autres plugins
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // === MARQUER LE COMBAT POUR L'ACTIONBAR ===
        // Joueur qui attaque = en combat
        Player attackingPlayer = getPlayerFromDamager(damager);
        if (attackingPlayer != null && plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().markInCombat(attackingPlayer.getUniqueId());
        }
        // Joueur qui reçoit des dégâts = en combat
        if (victim instanceof Player victimPlayer && plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().markInCombat(victimPlayer.getUniqueId());
        }

        // ============ BLOQUER DÉGÂTS MÊLÉE AVEC ARC/ARBALÈTE ============
        // Les arcs et arbalètes ne font pas de dégâts au corps à corps (clic gauche)
        // Exception: dégâts des bêtes du Chasseur (marqués par metadata)
        if (damager instanceof Player meleeAttacker) {
            // Ne pas bloquer si c'est un dégât de bête (Chasseur spé Bêtes)
            if (victim.hasMetadata("zombiez_beast_damage")) {
                // C'est un dégât de bête, laisser passer
            } else {
                ItemStack heldItem = meleeAttacker.getInventory().getItemInMainHand();
                if (heldItem != null && isRangedWeapon(heldItem.getType())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // PvP
        if (damager instanceof Player attacker && victim instanceof Player target) {
            handlePvP(event, attacker, target);
            return;
        }

        // Joueur attaque mob ZombieZ (zombie, squelette, etc.) OU World Boss
        if (damager instanceof Player player && victim instanceof LivingEntity livingVictim) {
            if (plugin.getZombieManager().isZombieZMob(livingVictim) || isWorldBoss(livingVictim)) {
                handlePlayerAttackZombieZMob(event, player, livingVictim);
                return;
            }
        }

        // Joueur attaque mob passif (ZombieZ ou vanilla)
        if (damager instanceof Player player && victim instanceof Animals animal) {
            if (plugin.getPassiveMobManager().isZombieZPassiveMob(animal)) {
                handlePlayerAttackPassiveMob(event, player, animal);
            } else {
                // Animal vanilla - appliquer les stats d'arme ZombieZ
                handlePlayerAttackGenericMob(event, player, animal);
            }
            return;
        }

        // Joueur attaque n'importe quel autre mob (vanilla non-animal) - afficher hologramme de dégâts
        if (damager instanceof Player player && victim instanceof LivingEntity livingVictim) {
            if (!(livingVictim instanceof Player)) {
                handlePlayerAttackGenericMob(event, player, livingVictim);
                return;
            }
        }

        // Mob ZombieZ attaque joueur (zombie, squelette, etc.)
        if (damager instanceof LivingEntity livingDamager && victim instanceof Player player) {
            if (plugin.getZombieManager().isZombieZMob(livingDamager)) {
                handleZombieZMobAttackPlayer(event, livingDamager, player);
            }
        }
    }

    /**
     * Gère le PvP
     */
    private void handlePvP(EntityDamageByEntityEvent event, Player attacker, Player target) {
        Zone attackerZone = plugin.getZoneManager().getPlayerZone(attacker);
        Zone targetZone = plugin.getZoneManager().getPlayerZone(target);

        // Vérifier si le PvP est activé dans la zone
        boolean pvpAllowed = (attackerZone != null && attackerZone.isPvpEnabled()) ||
                            (targetZone != null && targetZone.isPvpEnabled());

        if (!pvpAllowed) {
            event.setCancelled(true);
            com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(attacker, "§cPvP désactivé dans cette zone!");
        }
    }

    /**
     * Gère les attaques de joueur sur mob ZombieZ (zombie, squelette, etc.)
     * Applique: Cooldown d'attaque, Stats d'items, Momentum, Skills, Critiques, Effets
     */
    private void handlePlayerAttackZombieZMob(EntityDamageByEntityEvent event, Player player, LivingEntity mob) {
        // ============ DUPLICATE EVENT PROTECTION ============
        // Éviter le double traitement si l'événement est déclenché plusieurs fois
        if (mob.hasMetadata("zombiez_melee_processed")) {
            return;
        }
        mob.setMetadata("zombiez_melee_processed", new FixedMetadataValue(plugin, true));
        // Nettoyer après 1 tick pour permettre les prochaines attaques
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                mob.removeMetadata("zombiez_melee_processed", plugin);
            }
        }, 1L);

        // ============ SECONDARY DAMAGE CHECK ============
        // Si les dégâts sont secondaires (AoE talents, pet multi-attack, etc.), ne pas afficher d'indicateur
        boolean isSecondaryDamage = mob.hasMetadata("zombiez_secondary_damage");
        if (isSecondaryDamage) {
            mob.removeMetadata("zombiez_secondary_damage", plugin);
        }

        // ============ CONSUMABLE DAMAGE CHECK ============
        // Si les dégâts viennent d'un consommable, ne pas appliquer les stats d'arme
        if (mob.hasMetadata("zombiez_consumable_damage")) {
            // Préparer l'indicateur pour le MONITOR (affichera les dégâts finaux après talents)
            mob.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            mob.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, false));
            mob.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Mise à jour de l'affichage de vie
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (mob.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(mob);
                }
            });

            // Enregistrer le joueur pour le loot
            mob.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            return; // Ne pas appliquer les stats d'arme
        }

        // ============ TALENT DAMAGE CHECK (Lames Spectrales, etc.) ============
        // Si les dégâts sont calculés par un talent (déjà avec stats/crits/etc.), skip le traitement
        // Le talent a déjà préparé les metadatas pour l'indicateur
        if (mob.hasMetadata("zombiez_talent_damage")) {
            mob.removeMetadata("zombiez_talent_damage", plugin);

            // DPS tracking
            DPSTracker.getInstance().recordDamage(player, event.getDamage());

            // Mise à jour de l'affichage de vie
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (mob.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(mob);
                }
            });

            // Enregistrer le joueur pour le loot
            mob.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            return; // Ne pas re-appliquer les stats (déjà fait par le talent)
        }

        // ============ SHADOW STEP DAMAGE CHECK ============
        // Si les dégâts viennent de Pas de l'Ombre, ne pas appliquer le cooldown penalty
        // mais appliquer quand même les stats/crits/etc.
        boolean isShadowStepDamage = mob.hasMetadata("zombiez_shadowstep_damage");
        if (isShadowStepDamage) {
            mob.removeMetadata("zombiez_shadowstep_damage", plugin);
        }

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;

        // ============ TRIDENT AQUATIC BONUS ============
        // Le trident bénéficie d'un bonus de dégâts dans l'eau ou sous la pluie
        ItemStack heldWeapon = player.getInventory().getItemInMainHand();
        if (heldWeapon != null && heldWeapon.getType() == Material.TRIDENT) {
            double aquaticBonus = calculateTridentAquaticBonus(player);
            if (aquaticBonus > 1.0) {
                finalDamage *= aquaticBonus;
                // Particules d'eau
                if (player.isInWater()) {
                    mob.getWorld().spawnParticle(Particle.BUBBLE_POP, mob.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
                } else {
                    mob.getWorld().spawnParticle(Particle.DRIPPING_WATER, mob.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        // ============ 0. ATTACK COOLDOWN SYSTEM (PUNITIF) ============
        // Courbe quadratique pour punir le spam : 50% charge = 25% dégâts
        // EXCEPTION: Les talents (Shadow Step) bypass le cooldown penalty
        if (!isShadowStepDamage) {
            float attackCooldown = calculateAttackCooldown(player);
            double damageMultiplier = applyAttackCooldownPenalty(attackCooldown);
            finalDamage *= damageMultiplier;

            if (DEBUG_COOLDOWN) {
                plugin.getLogger().info(String.format(
                    "[COOLDOWN] %s -> ZombieZ: charge=%.0f%%, mult=%.2f, base=%.1f, result=%.1f",
                    player.getName(), attackCooldown * 100, damageMultiplier, baseDamage, finalDamage
                ));
            }
        } else if (DEBUG_COOLDOWN) {
            plugin.getLogger().info(String.format(
                "[COOLDOWN] %s -> ZombieZ: SHADOW_STEP bypass, base=%.1f (no penalty)",
                player.getName(), baseDamage
            ));
        }

        // ============ 1. STATS D'ÉQUIPEMENT ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // ============ 2. SKILL TREE BONUSES ============
        var skillManager = plugin.getSkillTreeManager();

        // Bonus dégâts passifs (Puissance I/II/III)
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // ============ 3. SYSTÈME DE CRITIQUE ============
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (random.nextDouble() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0; // 150% de base
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);

            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // ============ 4. MOMENTUM SYSTEM ============
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(player);
        finalDamage *= momentumMultiplier;

        // ============ 4.5 MACE ARMOR SHATTER ============
        // L'armor shatter de la masse réduit la défense effective des mobs
        if (mob.hasMetadata("zombiez_armor_shatter")) {
            double armorShatter = mob.getMetadata("zombiez_armor_shatter").get(0).asDouble();
            // Convertir la réduction d'armure en bonus de dégâts
            // -30% armure = +30% dégâts (simplifié)
            finalDamage *= (1 + armorShatter / 100.0);
        }

        // ============ 5. EXECUTE DAMAGE (<20% HP) ============
        double mobHealthPercent = mob.getHealth() / mob.getMaxHealth() * 100;
        double executeThreshold = playerStats.getOrDefault(StatType.EXECUTE_THRESHOLD, 20.0);

        if (mobHealthPercent <= executeThreshold) {
            double executeBonus = playerStats.getOrDefault(StatType.EXECUTE_DAMAGE, 0.0);
            double skillExecuteBonus = skillManager.getSkillBonus(player, SkillBonus.EXECUTE_DAMAGE);
            finalDamage *= (1 + (executeBonus + skillExecuteBonus) / 100.0);
        }

        // ============ 6. BERSERKER (<30% HP joueur) ============
        double playerHealthPercent = player.getHealth() / player.getMaxHealth() * 100;
        if (playerHealthPercent <= 30) {
            double berserkerBonus = skillManager.getSkillBonus(player, SkillBonus.BERSERKER);
            if (berserkerBonus > 0) {
                finalDamage *= (1 + berserkerBonus / 100.0);
                // Effet visuel berserker
                player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3);
            }
        }

        // ============ 7. DÉGÂTS ÉLÉMENTAIRES ============
        double fireDamage = playerStats.getOrDefault(StatType.FIRE_DAMAGE, 0.0);
        double iceDamage = playerStats.getOrDefault(StatType.ICE_DAMAGE, 0.0);
        double lightningDamage = playerStats.getOrDefault(StatType.LIGHTNING_DAMAGE, 0.0);

        if (fireDamage > 0) {
            mob.setFireTicks((int) (fireDamage * 20)); // Brûle
            finalDamage += fireDamage * 0.5;
        }
        if (iceDamage > 0) {
            mob.setFreezeTicks((int) (iceDamage * 20)); // Gèle
            finalDamage += iceDamage * 0.5;
        }
        if (lightningDamage > 0 && random.nextDouble() < 0.15) {
            // 15% chance de proc lightning - animation particules au lieu de vrai éclair
            spawnLightningParticles(mob.getLocation());
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);
            finalDamage += lightningDamage * 2;
        }

        // Poison DoT - applique un effet de poison proportionnel
        double poisonDamage = playerStats.getOrDefault(StatType.POISON_DAMAGE, 0.0);
        if (poisonDamage > 0) {
            // L'amplificateur augmente avec les dégâts poison (0-20 → 0-2 amplifier)
            int poisonAmplifier = Math.min(2, (int) (poisonDamage / 8));
            // Durée: 3 secondes de base + 0.1s par point de poison damage
            int poisonDuration = (int) (60 + poisonDamage * 2);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier, false, true));
            // Particules de poison (optimisées)
            mob.getWorld().spawnParticle(Particle.ITEM_SLIME, mob.getLocation().add(0, 1, 0), 4, 0.3, 0.4, 0.3, 0.02);
        }

        // ============ 8. LIFESTEAL ============
        double lifestealPercent = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);
        double skillLifesteal = skillManager.getSkillBonus(player, SkillBonus.LIFESTEAL);
        double totalLifesteal = lifestealPercent + skillLifesteal;

        if (totalLifesteal > 0) {
            double healAmount = finalDamage * (totalLifesteal / 100.0);
            double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
            player.setHealth(newHealth);

            // Effet visuel lifesteal
            if (healAmount > 1) {
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);
            }
        }

        // ============ APPLIQUER LES DÉGÂTS FINAUX ============
        event.setDamage(finalDamage);

        // ============ ENREGISTRER DPS ============
        DPSTracker.getInstance().recordDamage(player, finalDamage);

        // ============ PRÉPARER L'INDICATEUR DE DÉGÂTS (affiché par MONITOR après talents) ============
        // Ne pas afficher d'indicateur pour les dégâts secondaires (AoE, multi-attack, etc.)
        if (!isSecondaryDamage) {
            mob.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            mob.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
            mob.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }

        // ============ MISE À JOUR DE L'AFFICHAGE DE VIE ============
        // Exécuté au tick suivant pour avoir la vie mise à jour après les dégâts
        // Note: Les World Boss utilisent une BossBar (gérée par WorldBossListener), pas le display name
        if (!isWorldBoss(mob)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (mob.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(mob);
                }
            });
        }

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            // Effet critique
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }

        // Stocker les infos pour le loot (utilisé à la mort)
        mob.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // ============ STOCKER LES INFOS POUR LE TRACKING DES MISSIONS ============
        // Ces métadonnées persistent jusqu'à la mort du mob (non nettoyées par MONITOR)
        mob.setMetadata("zombiez_last_hit_critical", new FixedMetadataValue(plugin, isCritical));
        // Vérifier si c'est une attaque à distance (projectile)
        boolean isRangedAttack = event.getDamager() instanceof Projectile;
        mob.setMetadata("zombiez_last_hit_ranged", new FixedMetadataValue(plugin, isRangedAttack));
    }

    /**
     * Gère les attaques de joueur sur mob passif ZombieZ
     * Affiche les indicateurs de dégâts et applique les stats d'équipement
     */
    private void handlePlayerAttackPassiveMob(EntityDamageByEntityEvent event, Player player, Animals animal) {
        // ============ DUPLICATE EVENT PROTECTION ============
        if (animal.hasMetadata("zombiez_melee_processed")) {
            return;
        }
        animal.setMetadata("zombiez_melee_processed", new FixedMetadataValue(plugin, true));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (animal.isValid()) {
                animal.removeMetadata("zombiez_melee_processed", plugin);
            }
        }, 1L);

        // ============ SECONDARY DAMAGE CHECK ============
        boolean isSecondaryDamage = animal.hasMetadata("zombiez_secondary_damage");
        if (isSecondaryDamage) {
            animal.removeMetadata("zombiez_secondary_damage", plugin);
        }

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;

        // ============ 0. ATTACK COOLDOWN SYSTEM (PUNITIF) ============
        // Courbe quadratique pour punir le spam : 50% charge = 25% dégâts
        float attackCooldown = calculateAttackCooldown(player);
        double damageMultiplier = applyAttackCooldownPenalty(attackCooldown);
        finalDamage *= damageMultiplier;

        // ============ STATS D'ÉQUIPEMENT ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // ============ SKILL TREE BONUSES ============
        var skillManager = plugin.getSkillTreeManager();
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // ============ SYSTÈME DE CRITIQUE ============
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (random.nextDouble() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0;
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);
            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // ============ MOMENTUM SYSTEM ============
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(player);
        finalDamage *= momentumMultiplier;

        // ============ APPLIQUER LES DÉGÂTS FINAUX ============
        event.setDamage(finalDamage);

        // ============ ENREGISTRER DPS ============
        DPSTracker.getInstance().recordDamage(player, finalDamage);

        // ============ PRÉPARER L'INDICATEUR DE DÉGÂTS (affiché par MONITOR après talents) ============
        // Ne pas afficher d'indicateur pour les dégâts secondaires (AoE, multi-attack, etc.)
        if (!isSecondaryDamage) {
            animal.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            animal.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
            animal.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }

        // ============ MISE À JOUR DE L'AFFICHAGE DE VIE ============
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (animal.isValid()) {
                plugin.getPassiveMobManager().updatePassiveMobHealthDisplay(animal);
            }
        });

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            animal.getWorld().spawnParticle(Particle.CRIT, animal.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /**
     * Gère les attaques de joueur sur n'importe quel mob (vanilla/non-ZombieZ)
     * Affiche simplement l'indicateur de dégâts sans appliquer de bonus spéciaux
     */
    private void handlePlayerAttackGenericMob(EntityDamageByEntityEvent event, Player player, LivingEntity mob) {
        // ============ DUPLICATE EVENT PROTECTION ============
        if (mob.hasMetadata("zombiez_melee_processed")) {
            return;
        }
        mob.setMetadata("zombiez_melee_processed", new FixedMetadataValue(plugin, true));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (mob.isValid()) {
                mob.removeMetadata("zombiez_melee_processed", plugin);
            }
        }, 1L);

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;

        // ============ 0. ATTACK COOLDOWN SYSTEM (PUNITIF) ============
        // Courbe quadratique pour punir le spam : 50% charge = 25% dégâts
        float attackCooldown = calculateAttackCooldown(player);
        double damageMultiplier = applyAttackCooldownPenalty(attackCooldown);
        finalDamage *= damageMultiplier;

        // ============ STATS D'ÉQUIPEMENT ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // ============ SKILL TREE BONUSES ============
        var skillManager = plugin.getSkillTreeManager();
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // ============ SYSTÈME DE CRITIQUE ============
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (random.nextDouble() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0;
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);
            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // ============ APPLIQUER LES DÉGÂTS FINAUX ============
        event.setDamage(finalDamage);

        // ============ ENREGISTRER DPS ============
        DPSTracker.getInstance().recordDamage(player, finalDamage);

        // ============ PRÉPARER L'INDICATEUR DE DÉGÂTS ============
        mob.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
        mob.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
        mob.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /**
     * Gère les attaques de mob ZombieZ sur joueur (zombie, squelette, etc.)
     * Applique: Réduction de dégâts, Esquive, Skills défensifs
     */
    private void handleZombieZMobAttackPlayer(EntityDamageByEntityEvent event, LivingEntity mob, Player player) {
        // Vérifier si c'est un clone d'ombre allié - ne pas infliger de dégâts au joueur
        if (mob.hasMetadata("zombiez_friendly_clone")) {
            String ownerUuid = mob.getMetadata("zombiez_friendly_clone").get(0).asString();
            // Les clones ne peuvent pas attaquer leur propriétaire ni les autres joueurs
            event.setCancelled(true);
            return;
        }

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;

        // Multiplicateur de zone
        Zone zone = plugin.getZoneManager().getPlayerZone(player);
        if (zone != null) {
            finalDamage *= zone.getZombieDamageMultiplier();
        }

        // ============ STATS DÉFENSIVES ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        var skillManager = plugin.getSkillTreeManager();

        // ============ ARMURE (réduction asymptotique améliorée) ============
        // Formule: reduction = armor / (armor + 50)
        // 25 armure = 33% réduction, 50 = 50%, 100 = 66%, 150 = 75%
        double armor = playerStats.getOrDefault(StatType.ARMOR, 0.0);
        double armorPercent = playerStats.getOrDefault(StatType.ARMOR_PERCENT, 0.0);
        double totalArmor = armor * (1 + armorPercent / 100.0);

        if (totalArmor > 0) {
            double armorReduction = totalArmor / (totalArmor + 50.0);
            finalDamage *= (1 - armorReduction);
        }

        // ============ BONUS DE RARETÉ D'ARMURE ============
        // Les armures de haute rareté offrent une réduction de dégâts bonus
        double rarityBonus = calculateArmorRarityBonus(player);
        if (rarityBonus > 0) {
            finalDamage *= (1 - rarityBonus / 100.0);
        }

        // ============ RÉDUCTION DE DÉGÂTS % (items + skills) ============
        double damageReduction = playerStats.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0);
        double skillDamageReduction = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_REDUCTION);
        double totalReduction = Math.min(75, damageReduction + skillDamageReduction); // Cap à 75%

        finalDamage *= (1 - totalReduction / 100.0);

        // ============ BLOCAGE (chance de réduire 50% des dégâts) ============
        double blockChance = playerStats.getOrDefault(StatType.BLOCK_CHANCE, 0.0);
        if (blockChance > 0 && random.nextDouble() * 100 < blockChance) {
            finalDamage *= 0.5; // Bloque 50% des dégâts
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.2f);
            // Afficher l'indicateur de blocage flottant (via TextDisplay)
            PacketDamageIndicator.displayBlock(plugin, player.getLocation(), player);
        }

        // ============ ESQUIVE ============
        double dodgeChance = playerStats.getOrDefault(StatType.DODGE_CHANCE, 0.0);
        if (random.nextDouble() * 100 < dodgeChance) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.3, 0.5, 0.3, 0.05);
            com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(player, "§a§l↷ ESQUIVE!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
            // Afficher l'indicateur d'esquive flottant (via packets virtuels ProtocolLib)
            PacketDamageIndicator.displayDodge(plugin, player.getLocation(), player);
            return;
        }

        // ============ THORNS (Épines) ============
        double thorns = playerStats.getOrDefault(StatType.THORNS, 0.0);
        if (thorns > 0) {
            mob.damage(thorns, player);
            mob.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, mob.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
        }

        event.setDamage(finalDamage);
    }

    /**
     * Gère la mort d'une entité (pour les rewards)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // Mob ZombieZ tué par un joueur (zombie, squelette, etc.)
        if (plugin.getZombieManager().isZombieZMob(entity)) {
            handleZombieZMobDeath(killer, entity);
        }
    }

    /**
     * Gère la mort d'un mob ZombieZ (zombie, squelette, etc.)
     */
    private void handleZombieZMobDeath(Player killer, LivingEntity mob) {
        Zone zone = plugin.getZoneManager().getPlayerZone(killer);
        int zombieLevel = zone != null ? zone.getZombieLevelAt(killer.getLocation().getBlockZ()) : 1;

        // Déterminer le type de mob depuis les métadonnées
        String zombieType = "WALKER";
        if (mob.hasMetadata("zombiez_type")) {
            zombieType = mob.getMetadata("zombiez_type").get(0).asString();
        }

        // ============ ENREGISTRER LE KILL DANS MOMENTUM ============
        plugin.getMomentumManager().registerKill(killer);

        // ============ METTRE À JOUR LES MISSIONS ============
        plugin.getMissionManager().updateProgress(killer,
            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.ZOMBIE_KILLS, 1);

        // Elite kill tracking
        if (zombieType.contains("ELITE") || zombieType.contains("SPECIAL")) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.ELITE_KILLS, 1);
        }

        // Boss kill tracking
        if (zombieType.contains("BOSS")) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.BOSS_KILLS, 1);
        }

        // Headshot kill tracking - vérifie si le mob a été tué par un headshot
        if (mob.getScoreboardTags().contains("zombiez_headshot_kill")) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.HEADSHOTS, 1);
        }

        // Group kill tracking - vérifie si d'autres joueurs sont proches (< 20 blocs)
        boolean isGroupKill = checkGroupKill(killer, mob.getLocation());
        if (isGroupKill) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.GROUP_KILLS, 1);
        }

        // Kills sans dégâts tracking - vérifie si le joueur n'a pas pris de dégâts depuis son dernier kill
        UUID killerUuid = killer.getUniqueId();
        boolean killWithoutDamage = playersWithoutDamage.contains(killerUuid);
        if (killWithoutDamage) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.KILLS_NO_DAMAGE, 1);
        }
        // Réinitialiser pour le prochain kill (le joueur n'a pas pris de dégâts pour ce kill-ci)
        playersWithoutDamage.add(killerUuid);

        // Critical kill tracking - vérifie si le dernier coup était critique
        if (mob.hasMetadata("zombiez_last_hit_critical") &&
            mob.getMetadata("zombiez_last_hit_critical").get(0).asBoolean()) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.CRIT_KILLS, 1);
        }

        // Ranged/Melee kill tracking - vérifie si le kill était à distance ou au corps à corps
        boolean wasRangedKill = mob.hasMetadata("zombiez_last_hit_ranged") &&
                                mob.getMetadata("zombiez_last_hit_ranged").get(0).asBoolean();
        if (wasRangedKill) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.RANGED_KILLS, 1);
        } else {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.MELEE_KILLS, 1);
        }

        // Night kill tracking - vérifie si c'est la nuit (13000-23000 ticks)
        long worldTime = killer.getWorld().getTime();
        if (worldTime >= 13000 && worldTime <= 23000) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.NIGHT_KILLS, 1);
        }

        // Kills in streak tracking - ajoute le streak actuel
        int currentStreak = plugin.getMomentumManager().getStreak(killer);
        if (currentStreak > 0) {
            plugin.getMissionManager().updateProgress(killer,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.KILLS_IN_STREAK, 1);
        }

        // ============ ACHIEVEMENTS ============
        PlayerData data = plugin.getPlayerDataManager().getPlayer(killer);
        if (data != null) {
            var achievementManager = plugin.getAchievementManager();
            int totalKills = (int) data.getTotalKills() + 1;

            // Premier kill
            achievementManager.incrementProgress(killer, "first_blood", 1);

            // Achievements de kills - Chasseur (50, 250)
            achievementManager.checkAndUnlock(killer, "zombie_hunter_1", totalKills);
            achievementManager.checkAndUnlock(killer, "zombie_hunter_2", totalKills);

            // Achievements de kills - Tueur de Zombies (1000, 5000, 25000)
            achievementManager.checkAndUnlock(killer, "zombie_slayer_1", totalKills);
            achievementManager.checkAndUnlock(killer, "zombie_slayer_2", totalKills);
            achievementManager.checkAndUnlock(killer, "zombie_slayer_3", totalKills);

            // Achievements de kills extrêmes (100k, 500k, 1M)
            achievementManager.checkAndUnlock(killer, "exterminator", totalKills);
            achievementManager.checkAndUnlock(killer, "genocide", totalKills);
            achievementManager.checkAndUnlock(killer, "legend", totalKills);

            // Streak achievements (kills sans mourir)
            int streak = plugin.getMomentumManager().getStreak(killer);
            achievementManager.checkAndUnlock(killer, "killing_spree_1", streak);
            achievementManager.checkAndUnlock(killer, "killing_spree_2", streak);
            achievementManager.checkAndUnlock(killer, "unstoppable", streak);
            achievementManager.checkAndUnlock(killer, "immortal", streak);
            achievementManager.checkAndUnlock(killer, "deathless", streak);

            // Elite kill achievements
            if (zombieType.contains("ELITE") || zombieType.contains("SPECIAL")) {
                int eliteKills = (int) data.getEliteKills().get() + 1;
                achievementManager.checkAndUnlock(killer, "elite_hunter_1", eliteKills);
                achievementManager.checkAndUnlock(killer, "elite_hunter_2", eliteKills);
                achievementManager.checkAndUnlock(killer, "elite_master", eliteKills);
            }

            // Boss kill achievements
            if (zombieType.contains("BOSS")) {
                int bossKills = (int) data.getBossKills().get() + 1;
                achievementManager.checkAndUnlock(killer, "boss_slayer_1", bossKills);
                achievementManager.checkAndUnlock(killer, "boss_slayer_2", bossKills);
                achievementManager.checkAndUnlock(killer, "boss_master", bossKills);

                // Achievement spécial Patient Zéro
                if (zombieType.equals("PATIENT_ZERO")) {
                    achievementManager.incrementProgress(killer, "patient_zero", 1);
                }

                // Achievement group_boss - tuer un boss en groupe
                if (isGroupKill) {
                    achievementManager.incrementProgress(killer, "group_boss", 1);
                }
            }

            // Group kill achievements (kills avec des coéquipiers proches)
            if (isGroupKill) {
                data.incrementStat("group_kills");
                int groupKills = (int) data.getStat("group_kills");

                // Premier ami
                achievementManager.incrementProgress(killer, "first_friend", 1);

                // Joueur d'équipe I/II
                achievementManager.checkAndUnlock(killer, "team_player_1", groupKills);
                achievementManager.checkAndUnlock(killer, "team_player_2", groupKills);
            }
        }

        // Récompenser le joueur
        plugin.getEconomyManager().rewardZombieKill(killer, zombieType, zombieLevel);
    }

    // ==================== SYSTÈME DE HEADSHOT ====================

    /**
     * Détecte les headshots pour les projectiles (arc, arbalète, trident)
     * Applique un bonus de dégâts et joue une animation légère
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        Entity hitEntity = event.getHitEntity();

        // Vérifier que c'est un projectile valide pour headshot
        if (!isHeadshotProjectile(projectile)) return;
        if (hitEntity == null) return;
        if (!(hitEntity instanceof LivingEntity victim)) return;

        // Ne pas appliquer aux joueurs
        if (victim instanceof Player) return;

        // Vérifier si c'est un mob ZombieZ ou World Boss
        if (!isZombieZMob(victim) && !isWorldBoss(victim)) return;

        // Obtenir le tireur
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player)) return;

        // Calculer si c'est un headshot
        if (isHeadshot(projectile, victim)) {
            // Marquer le projectile pour le bonus de dégâts
            projectile.setMetadata("zombiez_headshot", new FixedMetadataValue(plugin, true));

            // Animation de headshot
            playHeadshotEffect(victim, player);
        }
    }

    /**
     * Applique le bonus de dégâts pour les headshots et affiche les indicateurs de dégâts pour tous les projectiles
     * Applique aussi les stats d'équipement, skills, et autres bonus (comme pour les attaques de mêlée)
     * Priorité HIGHEST pour cohérence avec onEntityDamage
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Vérifier si c'est un projectile
        if (!(damager instanceof Projectile projectile)) return;

        // Obtenir le tireur
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // Ne pas traiter les joueurs comme victimes
        if (victim instanceof Player) return;

        // Vérifier si c'est un mob ZombieZ ou World Boss
        if (!isZombieZMob(victim) && !isWorldBoss(victim)) return;

        // ============ SECONDARY DAMAGE CHECK ============
        boolean isSecondaryDamage = victim.hasMetadata("zombiez_secondary_damage");
        if (isSecondaryDamage) {
            victim.removeMetadata("zombiez_secondary_damage", plugin);
        }

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;
        boolean isHeadshot = projectile.hasMetadata("zombiez_headshot");

        // ============ 1. STATS D'ÉQUIPEMENT ============
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus de dégâts flat
        double flatDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        finalDamage += flatDamageBonus;

        // Bonus de dégâts en pourcentage
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        finalDamage *= (1 + damagePercent / 100.0);

        // ============ 2. SKILL TREE BONUSES ============
        var skillManager = plugin.getSkillTreeManager();

        // Bonus dégâts passifs (Puissance I/II/III)
        double skillDamageBonus = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_PERCENT);
        finalDamage *= (1 + skillDamageBonus / 100.0);

        // ============ 3. SYSTÈME DE CRITIQUE ============
        double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
        double totalCritChance = baseCritChance + skillCritChance;

        if (random.nextDouble() * 100 < totalCritChance) {
            isCritical = true;
            double baseCritDamage = 150.0; // 150% de base
            double bonusCritDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
            double skillCritDamage = skillManager.getSkillBonus(player, SkillBonus.CRIT_DAMAGE);

            double critMultiplier = (baseCritDamage + bonusCritDamage + skillCritDamage) / 100.0;
            finalDamage *= critMultiplier;
        }

        // ============ 4. MOMENTUM SYSTEM ============
        var momentumManager = plugin.getMomentumManager();
        double momentumMultiplier = momentumManager.getDamageMultiplier(player);
        finalDamage *= momentumMultiplier;

        // ============ 5. HEADSHOT BONUS (+50%) ============
        if (isHeadshot) {
            double headshotMultiplier = 1.5;
            finalDamage *= headshotMultiplier;
        }

        // ============ 6. EXECUTE DAMAGE (<20% HP) - pour tous les mobs ZombieZ ============
        double mobHealthPercent = victim.getHealth() / victim.getMaxHealth() * 100;
        double executeThreshold = playerStats.getOrDefault(StatType.EXECUTE_THRESHOLD, 20.0);

        if (mobHealthPercent <= executeThreshold) {
            double executeBonus = playerStats.getOrDefault(StatType.EXECUTE_DAMAGE, 0.0);
            double skillExecuteBonus = skillManager.getSkillBonus(player, SkillBonus.EXECUTE_DAMAGE);
            finalDamage *= (1 + (executeBonus + skillExecuteBonus) / 100.0);
        }

        // ============ 7. BERSERKER (<30% HP joueur) ============
        double playerHealthPercent = player.getHealth() / player.getMaxHealth() * 100;
        if (playerHealthPercent <= 30) {
            double berserkerBonus = skillManager.getSkillBonus(player, SkillBonus.BERSERKER);
            if (berserkerBonus > 0) {
                finalDamage *= (1 + berserkerBonus / 100.0);
            }
        }

        // ============ 8. LIFESTEAL ============
        double lifestealPercent = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);
        double skillLifesteal = skillManager.getSkillBonus(player, SkillBonus.LIFESTEAL);
        double totalLifesteal = lifestealPercent + skillLifesteal;

        if (totalLifesteal > 0) {
            double healAmount = finalDamage * (totalLifesteal / 100.0);
            double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
            player.setHealth(newHealth);

            // Effet visuel lifesteal
            if (healAmount > 1) {
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);
            }
        }

        // ============ APPLIQUER LES DÉGÂTS FINAUX ============
        event.setDamage(finalDamage);

        // ============ ENREGISTRER DPS ============
        DPSTracker.getInstance().recordDamage(player, finalDamage);

        // ============ PRÉPARER L'INDICATEUR DE DÉGÂTS (affiché par MONITOR après talents) ============
        // Ne pas afficher d'indicateur pour les dégâts secondaires (AoE, multi-attack, etc.)
        if (!isSecondaryDamage) {
            victim.setMetadata("zombiez_show_indicator", new FixedMetadataValue(plugin, true));
            victim.setMetadata("zombiez_damage_critical", new FixedMetadataValue(plugin, isCritical));
            victim.setMetadata("zombiez_damage_headshot", new FixedMetadataValue(plugin, isHeadshot));
            victim.setMetadata("zombiez_damage_viewer", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            // Tag scoreboard pour le système de défis (persiste jusqu'à la mort)
            if (isHeadshot) {
                victim.addScoreboardTag("zombiez_headshot_kill");
            }
        }

        // ============ FEEDBACK VISUEL CRITIQUE ============
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }

        // ============ MISE À JOUR DE L'AFFICHAGE DE VIE MOB ZOMBIEZ ============
        if (plugin.getZombieManager().isZombieZMob(victim)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (victim.isValid()) {
                    plugin.getZombieManager().updateZombieHealthDisplay(victim);
                }
            });

            // Stocker les infos pour le loot (utilisé à la mort)
            victim.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }
    }

    /**
     * Vérifie si un projectile peut faire un headshot
     */
    private boolean isHeadshotProjectile(Projectile projectile) {
        return projectile instanceof Arrow ||
               projectile instanceof SpectralArrow ||
               projectile instanceof Trident;
    }

    /**
     * Détermine si le projectile a touché la tête de la victime
     * Calcule la position d'impact relative à la hitbox de l'entité
     */
    private boolean isHeadshot(Projectile projectile, LivingEntity victim) {
        Location projectileLoc = projectile.getLocation();
        Location victimLoc = victim.getLocation();

        // Hauteur de l'entité
        double entityHeight = victim.getHeight();

        // Position Y relative du projectile par rapport à l'entité
        double relativeY = projectileLoc.getY() - victimLoc.getY();

        // Zone de la tête = 25% supérieur de la hitbox (environ les épaules et au-dessus)
        double headThreshold = entityHeight * 0.75;

        // Le projectile a touché la tête si la position Y est dans le quart supérieur
        return relativeY >= headThreshold;
    }

    /**
     * Joue l'effet visuel et sonore de headshot
     */
    private void playHeadshotEffect(LivingEntity victim, Player shooter) {
        Location headLoc = victim.getEyeLocation();

        // Particules de headshot (petite explosion de sang/étoiles)
        victim.getWorld().spawnParticle(Particle.ENCHANTED_HIT, headLoc, 15, 0.2, 0.2, 0.2, 0.1);
        victim.getWorld().spawnParticle(Particle.DUST, headLoc, 8, 0.15, 0.15, 0.15, 0,
            new Particle.DustOptions(Color.fromRGB(255, 50, 50), 0.8f));

        // Son de headshot (satisfaisant et distinct)
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.8f);
        shooter.playSound(shooter.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.5f);
    }

    /**
     * Vérifie si le kill est un kill en groupe (autres joueurs à moins de 20 blocs)
     * Retourne true si au moins un autre joueur est proche
     */
    private boolean checkGroupKill(Player killer, Location mobLocation) {
        // Rayon pour considérer un "groupe"
        final double GROUP_RADIUS = 20.0;

        for (Player nearbyPlayer : killer.getWorld().getPlayers()) {
            // Ignorer le tueur lui-même
            if (nearbyPlayer.equals(killer)) continue;

            // Vérifier la distance
            if (nearbyPlayer.getLocation().distanceSquared(mobLocation) <= GROUP_RADIUS * GROUP_RADIUS) {
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si une entité est un mob ZombieZ
     */
    private boolean isZombieZMob(Entity entity) {
        return entity.hasMetadata("zombiez_type") || entity.getScoreboardTags().contains("zombiez_mob");
    }

    /**
     * Vérifie si une entité est un World Boss
     */
    private boolean isWorldBoss(Entity entity) {
        return entity.getScoreboardTags().contains("world_boss");
    }

    /**
     * Vérifie si le matériau est une arme à distance (arc, arbalète)
     * Ces armes ne font pas de dégâts au corps à corps
     */
    private boolean isRangedWeapon(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }

    /**
     * Calcule le bonus de dégâts aquatique pour le trident
     * +20% dans l'eau, +10% sous la pluie
     */
    private double calculateTridentAquaticBonus(Player player) {
        // Bonus dans l'eau
        if (player.isInWater()) {
            return 1.20; // +20% dégâts
        }

        // Bonus sous la pluie (si exposé au ciel)
        org.bukkit.World world = player.getWorld();
        if (world.hasStorm()) {
            Location loc = player.getLocation();
            if (world.getHighestBlockYAt(loc) <= loc.getBlockY()) {
                return 1.10; // +10% dégâts
            }
        }

        return 1.0; // Pas de bonus
    }

    /**
     * Génère une animation d'éclair en particules (remplace strikeLightningEffect)
     * Crée un effet visuel de foudre sans le flash et le bruit intrusifs
     */
    private void spawnLightningParticles(Location loc) {
        if (loc.getWorld() == null) return;

        // Point de départ en hauteur
        Location top = loc.clone().add(0, 5, 0);
        Location bottom = loc.clone().add(0, 0.5, 0);

        // Ligne principale d'éclair (END_ROD pour l'effet lumineux)
        for (double y = 0; y <= 4.5; y += 0.3) {
            // Légère déviation aléatoire pour effet zigzag
            double offsetX = (Math.random() - 0.5) * 0.3;
            double offsetZ = (Math.random() - 0.5) * 0.3;
            Location point = top.clone().subtract(0, y, 0).add(offsetX, 0, offsetZ);
            loc.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }

        // Impact au sol (flash électrique)
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, bottom, 15, 0.4, 0.2, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.FIREWORK, bottom, 8, 0.3, 0.1, 0.3, 0.02);

        // Lueur ambiante
        loc.getWorld().spawnParticle(Particle.DUST, bottom.add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0,
            new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.2f));
    }

    /**
     * Calcule le bonus de réduction de dégâts basé sur la rareté moyenne des armures équipées.
     * Les armures de haute rareté offrent une protection supplémentaire.
     *
     * @param player Le joueur
     * @return Le pourcentage de réduction bonus (0-15%)
     */
    private double calculateArmorRarityBonus(Player player) {
        // Bonus par rareté d'armure (par pièce)
        // COMMON = 0%, UNCOMMON = 1%, RARE = 2%, EPIC = 3%, LEGENDARY = 4%
        // Full set bonus: COMMON = 0%, UNCOMMON = 4%, RARE = 8%, EPIC = 12%, LEGENDARY = 16%

        double totalBonus = 0;
        int armorPieces = 0;

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armor : armorContents) {
            if (armor == null || armor.getType().isAir()) continue;

            Rarity rarity = ZombieZItem.getItemRarity(armor);
            if (rarity != null) {
                armorPieces++;
                totalBonus += switch (rarity) {
                    case COMMON -> 0.0;
                    case UNCOMMON -> 1.0;
                    case RARE -> 2.0;
                    case EPIC -> 3.0;
                    case LEGENDARY -> 4.0;
                    case MYTHIC -> 5.0;
                    case EXALTED -> 6.0;
                };
            }
        }

        // Bonus supplémentaire si full set de même rareté haute (Epic+)
        if (armorPieces == 4) {
            Rarity firstRarity = ZombieZItem.getItemRarity(armorContents[0]);
            boolean fullSet = true;
            for (ItemStack armor : armorContents) {
                if (armor == null || ZombieZItem.getItemRarity(armor) != firstRarity) {
                    fullSet = false;
                    break;
                }
            }
            if (fullSet && firstRarity != null && firstRarity.ordinal() >= Rarity.EPIC.ordinal()) {
                totalBonus += 2.0; // Bonus set complet
            }
        }

        return totalBonus;
    }
}
