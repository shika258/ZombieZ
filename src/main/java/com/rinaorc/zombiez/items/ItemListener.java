package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Listener pour les événements liés aux items ZombieZ
 * Gère le pickup, l'équipement, et les effets
 */
public class ItemListener implements Listener {

    private final ZombieZPlugin plugin;

    public ItemListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère le ramassage d'items ZombieZ
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        
        if (!ZombieZItem.isZombieZItem(item)) {
            return;
        }

        // Obtenir les infos de l'item
        Rarity rarity = ZombieZItem.getItemRarity(item);
        int itemScore = ZombieZItem.getItemScore(item);
        
        if (rarity == null) return;

        // Message selon rareté
        if (rarity.isAtLeast(Rarity.RARE)) {
            String message = rarity.getChatColor() + "✦ " + rarity.getDisplayName() + 
                " §7ramassé! §8[" + itemScore + " IS]";
            MessageUtils.sendActionBar(player, message);
        }

        // Son différent selon rareté
        if (rarity.isAtLeast(Rarity.LEGENDARY)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else if (rarity.isAtLeast(Rarity.EPIC)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        }
    }

    /**
     * Gère le drop d'items (confirmation pour items rares+)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (!ZombieZItem.isZombieZItem(item)) {
            return;
        }

        Rarity rarity = ZombieZItem.getItemRarity(item);
        
        // Avertissement pour items rares+
        if (rarity != null && rarity.isAtLeast(Rarity.LEGENDARY)) {
            Player player = event.getPlayer();
            
            // TODO: Implémenter système de confirmation
            // Pour l'instant, juste un avertissement
            MessageUtils.sendRaw(player, 
                "§c⚠ Vous avez droppé un item " + rarity.getColoredName() + "§c!");
        }
    }

    /**
     * Gère le changement d'équipement
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Vérifier si c'est un slot d'équipement
        int slot = event.getSlot();
        boolean isArmorSlot = slot >= 36 && slot <= 39;
        boolean isOffhand = slot == 40;
        
        if (!isArmorSlot && !isOffhand) {
            // Vérifier si c'est un shift-click vers l'armure
            if (!event.isShiftClick()) {
                return;
            }
        }

        // Invalider le cache de stats du joueur
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemManager().invalidatePlayerStats(player.getUniqueId());
            
            // Vérifier si un item ZombieZ a été équipé
            ItemStack equipped = event.getCurrentItem();
            if (equipped != null && ZombieZItem.isZombieZItem(equipped)) {
                onItemEquipped(player, equipped);
            }
        }, 1L);
    }

    /**
     * Gère le changement d'item en main
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // Invalider le cache
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemManager().invalidatePlayerStats(player.getUniqueId());
            
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (newItem != null && ZombieZItem.isZombieZItem(newItem)) {
                showItemStats(player, newItem);
            }
        }, 1L);
    }

    /**
     * Appelé quand un item ZombieZ est équipé
     */
    private void onItemEquipped(Player player, ItemStack item) {
        ZombieZItem zItem = plugin.getItemManager().getItem(item);
        if (zItem == null) return;

        // Afficher les stats gagnées
        Map<StatType, Double> stats = zItem.getTotalStats();
        
        StringBuilder message = new StringBuilder("§a✓ Équipé: ");
        message.append(zItem.getRarity().getChatColor()).append(zItem.getGeneratedName());
        
        // Stats principales
        if (stats.containsKey(StatType.DAMAGE)) {
            message.append(" §c+").append(String.format("%.1f", stats.get(StatType.DAMAGE))).append(" DMG");
        }
        if (stats.containsKey(StatType.ARMOR)) {
            message.append(" §9+").append(String.format("%.1f", stats.get(StatType.ARMOR))).append(" ARM");
        }
        
        MessageUtils.sendActionBar(player, message.toString());
    }

    /**
     * Affiche les stats d'un item en main dans l'action bar
     */
    private void showItemStats(Player player, ItemStack item) {
        ZombieZItem zItem = plugin.getItemManager().getItem(item);
        if (zItem == null) return;

        String message = zItem.getRarity().getChatColor() + zItem.getGeneratedName() + 
            " §8| §7IS: §f" + zItem.getItemScore();
        
        MessageUtils.sendActionBar(player, message);
    }

    /**
     * Applique les effets spéciaux des items (appelé depuis CombatListener)
     */
    public void processSpecialEffects(Player attacker, org.bukkit.entity.LivingEntity target, double damage) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        
        if (!ZombieZItem.isZombieZItem(weapon)) {
            return;
        }

        ZombieZItem zItem = plugin.getItemManager().getItem(weapon);
        if (zItem == null) return;

        for (String effect : zItem.getSpecialEffects()) {
            processEffect(effect, attacker, target, damage);
        }
    }

    /**
     * Traite un effet spécial spécifique
     */
    private void processEffect(String effectId, Player attacker, org.bukkit.entity.LivingEntity target, double damage) {
        switch (effectId) {
            case "ignite" -> {
                if (Math.random() < 0.15) { // 15% chance
                    target.setFireTicks(60); // 3 secondes
                }
            }
            case "slow" -> {
                if (Math.random() < 0.20) { // 20% chance
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1
                    ));
                }
            }
            case "freeze" -> {
                if (Math.random() < 0.10) { // 10% chance
                    target.setFreezeTicks(60);
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 2
                    ));
                }
            }
            case "poison" -> {
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.POISON, 60, 0
                ));
            }
            case "chain_lightning" -> {
                if (Math.random() < 0.15) { // 15% chance
                    // Trouver une cible proche
                    target.getWorld().getNearbyEntities(target.getLocation(), 5, 5, 5).stream()
                        .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                        .filter(e -> e != target && e != attacker)
                        .findFirst()
                        .ifPresent(nearby -> {
                            ((org.bukkit.entity.LivingEntity) nearby).damage(damage * 0.5, attacker);
                            // Effet visuel
                            target.getWorld().strikeLightningEffect(nearby.getLocation());
                        });
                }
            }
            case "inferno_burst", "thunderstrike", "flame_nova" -> {
                // Ces effets sont gérés par un compteur de coups
                // TODO: Implémenter le compteur de coups
            }
            case "plague_spread" -> {
                // Propager le poison aux ennemis proches
                target.getWorld().getNearbyEntities(target.getLocation(), 3, 3, 3).stream()
                    .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                    .filter(e -> e != attacker)
                    .forEach(e -> {
                        ((org.bukkit.entity.LivingEntity) e).addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.POISON, 40, 0
                            ));
                    });
            }
        }
    }

    /**
     * Calcule les dégâts modifiés par les stats de l'arme
     */
    public double calculateModifiedDamage(Player attacker, double baseDamage) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(attacker);
        
        double damage = baseDamage;
        
        // Bonus de dégâts en %
        if (stats.containsKey(StatType.DAMAGE_PERCENT)) {
            damage *= (1 + stats.get(StatType.DAMAGE_PERCENT) / 100.0);
        }
        
        // Dégâts élémentaires
        damage += stats.getOrDefault(StatType.FIRE_DAMAGE, 0.0);
        damage += stats.getOrDefault(StatType.ICE_DAMAGE, 0.0);
        damage += stats.getOrDefault(StatType.LIGHTNING_DAMAGE, 0.0);
        
        // Chance critique
        double critChance = stats.getOrDefault(StatType.CRIT_CHANCE, 0.0) / 100.0;
        if (Math.random() < critChance) {
            double critDamage = 1.5 + (stats.getOrDefault(StatType.CRIT_DAMAGE, 0.0) / 100.0);
            damage *= critDamage;
            
            // Effet visuel crit
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        }
        
        return damage;
    }

    /**
     * Calcule les dégâts reçus modifiés par l'armure
     */
    public double calculateDamageReduction(Player defender, double incomingDamage) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(defender);
        
        double damage = incomingDamage;
        
        // Réduction de dégâts en %
        if (stats.containsKey(StatType.DAMAGE_REDUCTION)) {
            damage *= (1 - stats.get(StatType.DAMAGE_REDUCTION) / 100.0);
        }
        
        // Chance d'esquive
        double dodgeChance = stats.getOrDefault(StatType.DODGE_CHANCE, 0.0) / 100.0;
        if (Math.random() < dodgeChance) {
            MessageUtils.sendActionBar(defender, "§a✧ ESQUIVÉ!");
            return 0;
        }
        
        return Math.max(0, damage);
    }
}
