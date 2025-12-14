package com.rinaorc.zombiez.zombies;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.sets.SetBonusManager;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.zombies.affixes.ZombieAffix;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Listener pour les événements liés aux zombies ZombieZ
 */
public class ZombieListener implements Listener {

    private final ZombieZPlugin plugin;
    private final ZombieManager zombieManager;

    public ZombieListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.zombieManager = plugin.getZombieManager();
    }

    /**
     * Gère la mort d'un zombie
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onZombieDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Vérifier si c'est un zombie ZombieZ
        if (!zombieManager.isZombieZMob(entity)) {
            return;
        }
        
        // Annuler les drops vanilla
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Obtenir le tueur
        Player killer = entity.getKiller();
        
        // Traiter la mort via le ZombieManager
        zombieManager.onZombieDeath(entity.getUniqueId(), killer);
        
        // Bonus de sets si applicable
        if (killer != null) {
            SetBonusManager setBonusManager = plugin.getItemManager().getSetBonusManager();
            if (setBonusManager != null) {
                setBonusManager.onPlayerKill(killer, entity);
            }
        }
    }

    /**
     * Gère les dégâts infligés PAR un zombie
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onZombieAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // Vérifier si le zombie est l'attaquant
        if (!zombieManager.isZombieZMob(damager)) {
            return;
        }
        
        if (!(victim instanceof Player player)) {
            return;
        }
        
        // Obtenir l'ActiveZombie
        ZombieManager.ActiveZombie zombie = zombieManager.getActiveZombie(damager.getUniqueId());
        if (zombie == null) {
            return;
        }
        
        // Appliquer les effets d'affix si présent
        if (zombie.hasAffix()) {
            ZombieAffix affix = zombie.getAffix();
            affix.applyAttackEffects(player);
            
            // Gérer les abilities spéciales
            handleAffixSpecialAttack(affix, (LivingEntity) damager, player);
        }
        
        // Calculer la réduction de dégâts du joueur
        double reduction = calculatePlayerDamageReduction(player);
        double finalDamage = event.getDamage() * (1 - reduction);
        event.setDamage(Math.max(0.5, finalDamage));
    }

    /**
     * Gère les dégâts infligés À un zombie
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onZombieDamaged(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        
        // Vérifier si le zombie est la victime
        if (!zombieManager.isZombieZMob(victim)) {
            return;
        }
        
        // Obtenir l'attaquant (joueur)
        Player attacker = getPlayerAttacker(event);
        if (attacker == null) {
            return;
        }
        
        // Obtenir l'ActiveZombie
        ZombieManager.ActiveZombie zombie = zombieManager.getActiveZombie(victim.getUniqueId());
        if (zombie == null) {
            return;
        }
        
        // Calculer les dégâts modifiés du joueur
        double modifiedDamage = calculatePlayerDamage(attacker, event.getDamage(), zombie);
        event.setDamage(modifiedDamage);
        
        // Gérer les abilities spéciales d'affix défensif
        if (zombie.hasAffix()) {
            handleAffixSpecialDefense(zombie.getAffix(), (LivingEntity) victim, attacker, event);
        }
    }

    /**
     * Gère le ciblage des zombies
     */
    @EventHandler
    public void onZombieTarget(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        
        if (!zombieManager.isZombieZMob(entity)) {
            return;
        }
        
        // Vérifier si la cible est un zombie allié (pour les nécromanciens)
        if (event.getTarget() != null && event.getTarget().getScoreboardTags().stream()
                .anyMatch(tag -> tag.startsWith("zombiez_ally_"))) {
            event.setCancelled(true);
        }
    }

    /**
     * Obtient le joueur attaquant depuis un événement de dégâts
     */
    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        
        if (damager instanceof Player player) {
            return player;
        }
        
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        
        return null;
    }

    /**
     * Calcule la réduction de dégâts d'un joueur
     */
    private double calculatePlayerDamageReduction(Player player) {
        double reduction = 0;
        
        // Réduction depuis les stats d'items
        reduction += plugin.getItemManager().getPlayerStat(player, StatType.DAMAGE_REDUCTION) / 100.0;
        
        // Réduction depuis l'armure (déjà gérée par Minecraft mais on peut ajouter un bonus)
        reduction += plugin.getItemManager().getPlayerStat(player, StatType.ARMOR_PERCENT) / 200.0;
        
        // Cap à 80%
        return Math.min(0.8, reduction);
    }

    /**
     * Calcule les dégâts modifiés d'un joueur
     */
    private double calculatePlayerDamage(Player player, double baseDamage, ZombieManager.ActiveZombie zombie) {
        double damage = baseDamage;
        
        // Bonus de dégâts %
        double damagePercent = plugin.getItemManager().getPlayerStat(player, StatType.DAMAGE_PERCENT);
        damage *= (1 + damagePercent / 100.0);
        
        // Dégâts élémentaires
        damage += plugin.getItemManager().getPlayerStat(player, StatType.FIRE_DAMAGE);
        damage += plugin.getItemManager().getPlayerStat(player, StatType.ICE_DAMAGE);
        damage += plugin.getItemManager().getPlayerStat(player, StatType.LIGHTNING_DAMAGE);
        damage += plugin.getItemManager().getPlayerStat(player, StatType.POISON_DAMAGE);
        
        // Critique
        double critChance = plugin.getItemManager().getPlayerStat(player, StatType.CRIT_CHANCE) / 100.0;
        if (Math.random() < critChance) {
            double critDamage = 1.5 + plugin.getItemManager().getPlayerStat(player, StatType.CRIT_DAMAGE) / 100.0;
            damage *= critDamage;
            
            // Effet visuel de critique
            player.getWorld().spawnParticle(
                org.bukkit.Particle.CRIT,
                player.getLocation().add(0, 1, 0),
                10, 0.3, 0.3, 0.3, 0.1
            );
        }
        
        // Pénétration d'armure (réduit l'armure effective du zombie)
        // Géré par MythicMobs ou via les attributs
        
        return damage;
    }

    /**
     * Gère les abilities spéciales offensives d'affix
     */
    private void handleAffixSpecialAttack(ZombieAffix affix, LivingEntity zombie, Player victim) {
        String ability = affix.getSpecialAbility();
        if (ability == null) return;
        
        switch (ability) {
            case "ignite_on_hit" -> {
                victim.setFireTicks(60); // 3 secondes de feu
            }
            case "freeze_on_hit" -> {
                victim.setFreezeTicks(victim.getMaxFreezeTicks());
            }
            case "chain_lightning" -> {
                // Dégâts aux joueurs proches
                victim.getWorld().getNearbyEntities(victim.getLocation(), 5, 3, 5).stream()
                    .filter(e -> e instanceof Player && e != victim)
                    .limit(2)
                    .forEach(e -> {
                        ((Player) e).damage(3, zombie);
                        victim.getWorld().spawnParticle(
                            org.bukkit.Particle.ELECTRIC_SPARK,
                            ((Player) e).getLocation().add(0, 1, 0),
                            20, 0.3, 0.5, 0.3, 0.1
                        );
                    });
            }
            case "lifesteal" -> {
                // Soigner le zombie de 20% des dégâts infligés
                double healAmount = victim.getLastDamage() * 0.2;
                zombie.setHealth(Math.min(zombie.getHealth() + healAmount, 
                    zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
            }
            case "mana_drain" -> {
                // Réduire temporairement la vitesse d'attaque
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 60, 1
                ));
            }
            case "corruption_spread" -> {
                // Appliquer wither à tous les joueurs proches
                victim.getWorld().getNearbyEntities(victim.getLocation(), 4, 2, 4).stream()
                    .filter(e -> e instanceof Player)
                    .forEach(e -> ((Player) e).addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WITHER, 40, 0
                        )
                    ));
            }
        }
    }

    /**
     * Gère les abilities spéciales défensives d'affix
     */
    private void handleAffixSpecialDefense(ZombieAffix affix, LivingEntity zombie, Player attacker, 
                                           EntityDamageByEntityEvent event) {
        String ability = affix.getSpecialAbility();
        if (ability == null) return;
        
        switch (ability) {
            case "damage_reflect" -> {
                // Refléter 20% des dégâts
                double reflectDamage = event.getDamage() * 0.2;
                attacker.damage(reflectDamage);
                attacker.getWorld().spawnParticle(
                    org.bukkit.Particle.ENCHANTED_HIT,
                    attacker.getLocation().add(0, 1, 0),
                    15, 0.3, 0.3, 0.3, 0.1
                );
            }
            case "death_prevention" -> {
                // Si le zombie devrait mourir, le sauver une fois à 10% HP
                if (!zombie.hasMetadata("death_prevented")) {
                    double maxHealth = zombie.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    if (zombie.getHealth() - event.getDamage() <= 0) {
                        event.setDamage(0);
                        zombie.setHealth(maxHealth * 0.1);
                        zombie.setMetadata("death_prevented", 
                            new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        
                        // Effet visuel
                        zombie.getWorld().spawnParticle(
                            org.bukkit.Particle.TOTEM_OF_UNDYING,
                            zombie.getLocation().add(0, 1, 0),
                            30, 0.5, 1, 0.5, 0.2
                        );
                        zombie.getWorld().playSound(
                            zombie.getLocation(),
                            org.bukkit.Sound.ITEM_TOTEM_USE,
                            1f, 1f
                        );
                    }
                }
            }
        }
    }
}
