package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DPSTracker;
import com.rinaorc.zombiez.combat.PacketDamageIndicator;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.StatType;
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

import java.util.Map;
import java.util.Random;

/**
 * Listener pour les événements de combat
 * Intègre: Momentum, Stats d'items, SkillTree, Effets visuels
 */
public class CombatListener implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    public CombatListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
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
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // PvP
        if (damager instanceof Player attacker && victim instanceof Player target) {
            handlePvP(event, attacker, target);
            return;
        }

        // Joueur attaque mob ZombieZ (zombie, squelette, etc.)
        if (damager instanceof Player player && victim instanceof LivingEntity livingVictim) {
            if (plugin.getZombieManager().isZombieZMob(livingVictim)) {
                handlePlayerAttackZombieZMob(event, player, livingVictim);
                return;
            }
        }

        // Joueur attaque mob passif ZombieZ
        if (damager instanceof Player player && victim instanceof Animals animal) {
            if (plugin.getPassiveMobManager().isZombieZPassiveMob(animal)) {
                handlePlayerAttackPassiveMob(event, player, animal);
                return;
            }
        }

        // Joueur attaque n'importe quel autre mob (vanilla) - afficher hologramme de dégâts
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
     * Applique: Stats d'items, Momentum, Skills, Critiques, Effets
     */
    private void handlePlayerAttackZombieZMob(EntityDamageByEntityEvent event, Player player, LivingEntity mob) {
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

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;

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
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (mob.isValid()) {
                plugin.getZombieManager().updateZombieHealthDisplay(mob);
            }
        });

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            // Effet critique
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }

        // Stocker les infos pour le loot (utilisé à la mort)
        mob.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
    }

    /**
     * Gère les attaques de joueur sur mob passif ZombieZ
     * Affiche les indicateurs de dégâts et applique les stats d'équipement
     */
    private void handlePlayerAttackPassiveMob(EntityDamageByEntityEvent event, Player player, Animals animal) {
        // ============ SECONDARY DAMAGE CHECK ============
        boolean isSecondaryDamage = animal.hasMetadata("zombiez_secondary_damage");
        if (isSecondaryDamage) {
            animal.removeMetadata("zombiez_secondary_damage", plugin);
        }

        double baseDamage = event.getDamage();
        double finalDamage = baseDamage;
        boolean isCritical = false;

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
        double finalDamage = event.getDamage();
        boolean isCritical = false;

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

        // Réduction de dégâts (items + skills)
        double damageReduction = playerStats.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0);
        double skillDamageReduction = skillManager.getSkillBonus(player, SkillBonus.DAMAGE_REDUCTION);
        double totalReduction = Math.min(75, damageReduction + skillDamageReduction); // Cap à 75%

        finalDamage *= (1 - totalReduction / 100.0);

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

        // ============ ACHIEVEMENTS ============
        PlayerData data = plugin.getPlayerDataManager().getPlayer(killer);
        if (data != null) {
            int totalKills = (int) data.getTotalKills() + 1;
            plugin.getAchievementManager().incrementProgress(killer, "first_blood", 1);
            plugin.getAchievementManager().checkAndUnlock(killer, "zombie_slayer_1", totalKills);
            plugin.getAchievementManager().checkAndUnlock(killer, "zombie_slayer_2", totalKills);
            plugin.getAchievementManager().checkAndUnlock(killer, "zombie_slayer_3", totalKills);
            plugin.getAchievementManager().checkAndUnlock(killer, "zombie_slayer_4", totalKills);

            // Streak achievements
            int streak = plugin.getMomentumManager().getStreak(killer);
            plugin.getAchievementManager().checkAndUnlock(killer, "killing_spree", streak);
            plugin.getAchievementManager().checkAndUnlock(killer, "unstoppable", streak);
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

        // Vérifier si c'est un mob ZombieZ
        if (!isZombieZMob(victim)) return;

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
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Vérifier si c'est un projectile
        if (!(damager instanceof Projectile projectile)) return;

        // Obtenir le tireur
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        // Ne pas traiter les joueurs comme victimes
        if (victim instanceof Player) return;

        // Vérifier si c'est un mob ZombieZ
        if (!isZombieZMob(victim)) return;

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
     * Vérifie si une entité est un mob ZombieZ
     */
    private boolean isZombieZMob(Entity entity) {
        return entity.hasMetadata("zombiez_type") || entity.getScoreboardTags().contains("zombiez_mob");
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
}
