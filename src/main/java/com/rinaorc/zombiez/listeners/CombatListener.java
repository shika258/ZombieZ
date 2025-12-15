package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DamageIndicator;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

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

        // Joueur attaque zombie
        if (damager instanceof Player player && victim instanceof Zombie zombie) {
            handlePlayerAttackZombie(event, player, zombie);
            return;
        }

        // Zombie attaque joueur
        if (damager instanceof Zombie zombie && victim instanceof Player player) {
            handleZombieAttackPlayer(event, zombie, player);
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
     * Gère les attaques de joueur sur zombie
     * Applique: Stats d'items, Momentum, Skills, Critiques, Effets
     */
    private void handlePlayerAttackZombie(EntityDamageByEntityEvent event, Player player, Zombie zombie) {
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
        double zombieHealthPercent = zombie.getHealth() / zombie.getMaxHealth() * 100;
        double executeThreshold = playerStats.getOrDefault(StatType.EXECUTE_THRESHOLD, 20.0);

        if (zombieHealthPercent <= executeThreshold) {
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
            zombie.setFireTicks((int) (fireDamage * 20)); // Brûle
            finalDamage += fireDamage * 0.5;
        }
        if (iceDamage > 0) {
            zombie.setFreezeTicks((int) (iceDamage * 20)); // Gèle
            finalDamage += iceDamage * 0.5;
        }
        if (lightningDamage > 0 && random.nextDouble() < 0.15) {
            // 15% chance de proc lightning
            zombie.getWorld().strikeLightningEffect(zombie.getLocation());
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

        // ============ INDICATEUR DE DÉGÂTS FLOTTANT (Client-side) ============
        DamageIndicator.display(plugin, zombie.getLocation(), finalDamage, isCritical, player);

        // ============ FEEDBACK VISUEL ============
        if (isCritical) {
            // Effet critique
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            zombie.getWorld().spawnParticle(Particle.CRIT, zombie.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);

            // Message critique avec dégâts
            com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(player,
                "§6§l✦ CRITIQUE! §c" + String.format("%.1f", finalDamage) + " dégâts");
        }

        // Stocker les infos pour le loot (utilisé à la mort)
        zombie.setMetadata("last_damage_player", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
    }

    /**
     * Gère les attaques de zombie sur joueur
     * Applique: Réduction de dégâts, Esquive, Skills défensifs
     */
    private void handleZombieAttackPlayer(EntityDamageByEntityEvent event, Zombie zombie, Player player) {
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
            // Afficher l'indicateur d'esquive flottant (client-side)
            DamageIndicator.displayDodge(plugin, player.getLocation(), player);
            return;
        }

        // ============ THORNS (Épines) ============
        double thorns = playerStats.getOrDefault(StatType.THORNS, 0.0);
        if (thorns > 0) {
            zombie.damage(thorns, player);
            zombie.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, zombie.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
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

        // Zombie tué par un joueur
        if (entity instanceof Zombie zombie) {
            handleZombieDeath(killer, zombie);
        }
    }

    /**
     * Gère la mort d'un zombie
     */
    private void handleZombieDeath(Player killer, Zombie zombie) {
        Zone zone = plugin.getZoneManager().getPlayerZone(killer);
        int zombieLevel = zone != null ? zone.getZombieLevelAt(killer.getLocation().getBlockZ()) : 1;

        // Déterminer le type de zombie depuis les métadonnées
        String zombieType = "WALKER";
        if (zombie.hasMetadata("zombiez_type")) {
            zombieType = zombie.getMetadata("zombiez_type").get(0).asString();
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
}
