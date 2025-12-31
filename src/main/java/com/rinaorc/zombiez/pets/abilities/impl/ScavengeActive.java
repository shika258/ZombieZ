package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableRarity;
import com.rinaorc.zombiez.consumables.ConsumableType;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.mobs.food.FoodItemRegistry;
import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Ultimate du Rat des Catacombes
 * Trouve 1 nourriture OU 1 consommable toutes les 60s
 * La rareté augmente avec le niveau du pet
 */
@Getter
public class ScavengeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final Random random = new Random();

    public ScavengeActive(String id, String name, String desc) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        ZombieZPlugin plugin = (ZombieZPlugin) Bukkit.getPluginManager().getPlugin("ZombieZ");
        if (plugin == null) return false;

        // Effets visuels de fouille
        player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.BLOCK,
            player.getLocation().add(0, 0.5, 0),
            25, 0.5, 0.2, 0.5, 0,
            org.bukkit.Material.DIRT.createBlockData());

        // Déterminer la rareté basée sur le niveau du pet (1-11)
        int petLevel = petData.getLevel();
        ConsumableRarity rarity = rollRarityByLevel(petLevel, petData.getStarPower());

        // 50/50 entre nourriture et consommable
        boolean isFood = random.nextBoolean();
        ItemStack reward;
        String itemName;

        if (isFood) {
            // Trouver une nourriture
            FoodItem food = rollFoodByRarity(plugin, rarity);
            if (food != null) {
                reward = food.createItemStack(1);
                itemName = food.getDisplayName();
            } else {
                // Fallback
                reward = new ItemStack(org.bukkit.Material.BREAD);
                itemName = "Pain";
            }
        } else {
            // Trouver un consommable
            ConsumableType type = rollConsumableType();
            int zoneId = getPlayerZone(player, plugin);
            Consumable consumable = new Consumable(type, rarity, zoneId);
            reward = consumable.createItemStack();
            itemName = rarity.getColor() + type.getDisplayName();
        }

        // Donner l'item au joueur
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(reward);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), reward);
        }

        // Message et effets selon rareté
        String rarityColor = rarity.getColor();
        String rarityName = rarity.getDisplayName();

        player.playSound(player.getLocation(), getSoundForRarity(rarity), 1.0f, 1.0f);
        player.sendMessage("§a[Pet] §7Votre rat a trouvé: " + rarityColor + itemName + " §7(" + rarityColor + rarityName + "§7)");

        // Particules bonus pour les items rares+
        if (rarity.ordinal() >= ConsumableRarity.RARE.ordinal()) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
        }

        return true;
    }

    /**
     * Détermine la rareté selon le niveau du pet
     * Niveau 1-3: Principalement Common/Uncommon
     * Niveau 4-6: Principalement Uncommon/Rare
     * Niveau 7-9: Principalement Rare/Epic
     * Niveau 10-11: Principalement Epic/Legendary
     * Star Power augmente encore les chances
     */
    private ConsumableRarity rollRarityByLevel(int level, int starPower) {
        double roll = random.nextDouble();

        // Bonus de star power (0-3 étoiles = 0-15% bonus)
        double starBonus = starPower * 0.05;

        // Chances de base selon le niveau
        double legendaryChance, epicChance, rareChance, uncommonChance;

        if (level >= 10) {
            // Niveau 10-11: Epic/Legendary focus
            legendaryChance = 0.15 + starBonus;
            epicChance = 0.40 + starBonus;
            rareChance = 0.30;
            uncommonChance = 0.10;
        } else if (level >= 7) {
            // Niveau 7-9: Rare/Epic focus
            legendaryChance = 0.05 + starBonus;
            epicChance = 0.20 + starBonus;
            rareChance = 0.40;
            uncommonChance = 0.25;
        } else if (level >= 4) {
            // Niveau 4-6: Uncommon/Rare focus
            legendaryChance = 0.02 + starBonus;
            epicChance = 0.08 + starBonus;
            rareChance = 0.25;
            uncommonChance = 0.40;
        } else {
            // Niveau 1-3: Common/Uncommon focus
            legendaryChance = 0.01 + starBonus;
            epicChance = 0.04 + starBonus;
            rareChance = 0.10;
            uncommonChance = 0.30;
        }

        // Roll
        if (roll < legendaryChance) return ConsumableRarity.LEGENDARY;
        if (roll < legendaryChance + epicChance) return ConsumableRarity.EPIC;
        if (roll < legendaryChance + epicChance + rareChance) return ConsumableRarity.RARE;
        if (roll < legendaryChance + epicChance + rareChance + uncommonChance) return ConsumableRarity.UNCOMMON;
        return ConsumableRarity.COMMON;
    }

    /**
     * Obtient une nourriture aléatoire selon la rareté cible
     */
    private FoodItem rollFoodByRarity(ZombieZPlugin plugin, ConsumableRarity targetRarity) {
        var passiveMobManager = plugin.getPassiveMobManager();
        if (passiveMobManager == null) return null;

        FoodItemRegistry registry = passiveMobManager.getFoodRegistry();
        if (registry == null) return null;

        // Mapper ConsumableRarity vers FoodItem.FoodRarity
        FoodItem.FoodRarity foodRarity = switch (targetRarity) {
            case LEGENDARY -> FoodItem.FoodRarity.LEGENDARY;
            case EPIC -> FoodItem.FoodRarity.EPIC;
            case RARE -> FoodItem.FoodRarity.RARE;
            case UNCOMMON -> FoodItem.FoodRarity.UNCOMMON;
            default -> FoodItem.FoodRarity.COMMON;
        };

        // Filtrer les items par rareté et en prendre un au hasard
        var matchingItems = registry.getAllItems().stream()
            .filter(item -> item.getRarity() == foodRarity)
            .toList();

        if (matchingItems.isEmpty()) {
            // Fallback: prendre n'importe quelle nourriture
            var allItems = registry.getAllItems().stream().toList();
            if (allItems.isEmpty()) return null;
            return allItems.get(random.nextInt(allItems.size()));
        }

        return matchingItems.get(random.nextInt(matchingItems.size()));
    }

    /**
     * Obtient un type de consommable aléatoire
     */
    private ConsumableType rollConsumableType() {
        ConsumableType[] types = ConsumableType.values();
        return types[random.nextInt(types.length)];
    }

    /**
     * Obtient la zone actuelle du joueur
     */
    private int getPlayerZone(Player player, ZombieZPlugin plugin) {
        var zoneManager = plugin.getZoneManager();
        if (zoneManager == null) return 1;

        var zone = zoneManager.getPlayerZone(player);
        return zone != null ? zone.getId() : 1;
    }

    /**
     * Son approprié selon la rareté
     */
    private Sound getSoundForRarity(ConsumableRarity rarity) {
        return switch (rarity) {
            case LEGENDARY -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case EPIC -> Sound.ENTITY_PLAYER_LEVELUP;
            case RARE -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> Sound.ENTITY_ITEM_PICKUP;
        };
    }
}
