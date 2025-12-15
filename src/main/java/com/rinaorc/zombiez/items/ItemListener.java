package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableRarity;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
     * Gère le drop d'items - applique le glow et le nom visible pour TOUS les items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();
        Item droppedItem = event.getItemDrop();
        Player player = event.getPlayer();

        // Vérifier si c'est un ZombieZItem
        if (ZombieZItem.isZombieZItem(itemStack)) {
            Rarity rarity = ZombieZItem.getItemRarity(itemStack);

            if (rarity != null) {
                // Appliquer le glow et le nom visible
                String itemName = getItemDisplayName(itemStack);
                plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, getRarityChatColor(rarity));

                // Avertissement pour items légendaires+
                if (rarity.isAtLeast(Rarity.LEGENDARY)) {
                    MessageUtils.sendRaw(player,
                        "§c⚠ Vous avez droppé un item " + rarity.getColoredName() + "§c!");
                }
            }
            return;
        }

        // Vérifier si c'est un Consommable
        if (Consumable.isConsumable(itemStack)) {
            Consumable consumable = Consumable.fromItemStack(itemStack);
            if (consumable != null) {
                ConsumableRarity rarity = consumable.getRarity();
                String itemName = consumable.getType().getDisplayName();
                ChatColor color = getConsumableRarityChatColor(rarity);
                plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, color);
            }
            return;
        }

        // Vérifier si c'est un FoodItem
        String foodId = getFoodId(itemStack);
        if (foodId != null) {
            var foodRegistry = plugin.getPassiveMobManager().getFoodRegistry();
            if (foodRegistry != null) {
                FoodItem foodItem = foodRegistry.getItem(foodId);
                if (foodItem != null) {
                    ChatColor color = getFoodRarityChatColor(foodItem.getRarity());
                    plugin.getItemManager().applyDroppedItemEffects(droppedItem, foodItem.getDisplayName(), color);
                }
            }
            return;
        }

        // Pour tous les autres items (vanilla ou autres), appliquer un glow blanc et le nom
        String itemName = getItemDisplayName(itemStack);
        if (itemName != null && !itemName.isEmpty()) {
            plugin.getItemManager().applyDroppedItemEffects(droppedItem, itemName, ChatColor.WHITE);
        }
    }

    /**
     * Obtient le nom d'affichage d'un ItemStack
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) return null;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            // Utiliser le nom custom si présent
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(item.getItemMeta().displayName());
        }

        // Sinon utiliser le nom du matériau formaté
        return formatMaterialName(item.getType());
    }

    /**
     * Formate le nom d'un matériau pour l'affichage
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Obtient la ChatColor correspondant à une Rarity
     */
    private ChatColor getRarityChatColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
            case MYTHIC -> ChatColor.LIGHT_PURPLE;
            case EXALTED -> ChatColor.RED;
        };
    }

    /**
     * Obtient la ChatColor correspondant à une ConsumableRarity
     */
    private ChatColor getConsumableRarityChatColor(ConsumableRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Obtient la ChatColor correspondant à une FoodRarity
     */
    private ChatColor getFoodRarityChatColor(FoodItem.FoodRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Obtient l'ID de nourriture ZombieZ d'un item
     */
    private String getFoodId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        if (!meta.getPersistentDataContainer().has(FoodItem.FOOD_KEY, PersistentDataType.STRING)) {
            return null;
        }
        return meta.getPersistentDataContainer().get(FoodItem.FOOD_KEY, PersistentDataType.STRING);
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

        // Invalider le cache de stats du joueur et recalculer les attributs
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getItemManager().invalidatePlayerStats(player.getUniqueId());

            // Recalculer et appliquer les attributs du joueur
            applyPlayerAttributes(player);

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

    // Clé unique pour le modifier de vie max ZombieZ
    private static final NamespacedKey ZOMBIEZ_HEALTH_KEY = new NamespacedKey("zombiez", "max_health_bonus");

    /**
     * Applique les attributs du joueur basés sur son équipement ZombieZ
     * Notamment le bonus de vie maximale (MAX_HEALTH)
     */
    public void applyPlayerAttributes(Player player) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(player);

        // Appliquer le bonus de vie maximale
        double healthBonus = stats.getOrDefault(StatType.MAX_HEALTH, 0.0);
        applyMaxHealthBonus(player, healthBonus);
    }

    /**
     * Applique le bonus de vie maximale au joueur
     */
    private void applyMaxHealthBonus(Player player, double bonus) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // Supprimer l'ancien modifier ZombieZ s'il existe
        maxHealthAttr.getModifiers().stream()
            .filter(mod -> mod.getKey().equals(ZOMBIEZ_HEALTH_KEY))
            .findFirst()
            .ifPresent(maxHealthAttr::removeModifier);

        // Ajouter le nouveau modifier si le bonus est positif
        if (bonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                ZOMBIEZ_HEALTH_KEY,
                bonus,
                AttributeModifier.Operation.ADD_NUMBER
            );
            maxHealthAttr.addModifier(modifier);
        }

        // S'assurer que la vie actuelle ne dépasse pas le nouveau max
        double newMaxHealth = maxHealthAttr.getValue();
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(newMaxHealth);
        }
    }

    /**
     * Supprime tous les modifiers ZombieZ d'un joueur (pour cleanup)
     */
    public void removeAllModifiers(Player player) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.getModifiers().stream()
                .filter(mod -> mod.getKey().equals(ZOMBIEZ_HEALTH_KEY))
                .findFirst()
                .ifPresent(maxHealthAttr::removeModifier);
        }
    }
}
