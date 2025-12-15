package com.rinaorc.zombiez.mobs.food;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener pour gérer la consommation de nourriture custom
 * Intercepte les événements de clic droit pour appliquer les effets
 */
public class FoodListener implements Listener {

    private final ZombieZPlugin plugin;

    public FoodListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère la consommation de nourriture custom via clic droit
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Vérifier que c'est un clic droit
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ignorer la main secondaire pour éviter les doublons
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        // Vérifier si c'est un item de nourriture ZombieZ
        String foodId = getFoodId(item);
        if (foodId == null) return;

        Player player = event.getPlayer();

        // Récupérer l'item de nourriture
        FoodItem foodItem = plugin.getPassiveMobManager().getFoodRegistry().getItem(foodId);
        if (foodItem == null) return;

        // Annuler l'événement vanilla pour éviter la consommation normale
        event.setCancelled(true);

        // Vérifier si le joueur peut manger (faim pas pleine ou besoin de soin)
        boolean canEat = player.getFoodLevel() < 20 ||
                         player.getHealth() < player.getAttribute(
                             org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() ||
                         !foodItem.getEffects().isEmpty();

        if (!canEat) {
            player.sendMessage("§c✖ §7Vous n'avez pas besoin de manger pour le moment!");
            return;
        }

        // Consommer l'item
        consumeFood(player, item, foodItem);
    }

    /**
     * Intercepte aussi la consommation normale pour les items custom
     * (au cas où le joueur mange via le système vanilla)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        String foodId = getFoodId(item);

        if (foodId != null) {
            // C'est un item ZombieZ, on gère nous-mêmes
            event.setCancelled(true);

            FoodItem foodItem = plugin.getPassiveMobManager().getFoodRegistry().getItem(foodId);
            if (foodItem != null) {
                consumeFood(event.getPlayer(), item, foodItem);
            }
        }
    }

    /**
     * Consomme un item de nourriture et applique ses effets
     */
    private void consumeFood(Player player, ItemStack item, FoodItem foodItem) {
        // Appliquer les effets
        foodItem.applyEffects(player);

        // Réduire la quantité de l'item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            // Si c'est un seau de lait, remplacer par un seau vide
            if (item.getType() == Material.MILK_BUCKET) {
                item.setType(Material.BUCKET);
                item.setAmount(1);
                // Retirer les métadonnées
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(null);
                    meta.setLore(null);
                    meta.getPersistentDataContainer().remove(FoodItem.FOOD_KEY);
                    item.setItemMeta(meta);
                }
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        // Effets visuels et sonores
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);

        // Message selon la rareté
        String message = switch (foodItem.getRarity()) {
            case COMMON -> "§7Vous mangez: " + foodItem.getRarity().getColor() + foodItem.getDisplayName();
            case UNCOMMON -> "§aVous savourez: " + foodItem.getRarity().getColor() + foodItem.getDisplayName();
            case RARE -> "§9Vous dégustez: " + foodItem.getRarity().getColor() + foodItem.getDisplayName();
            case EPIC -> "§5Vous vous régalez avec: " + foodItem.getRarity().getColor() + foodItem.getDisplayName();
            case LEGENDARY -> "§6✦ FESTIN LÉGENDAIRE! §6" + foodItem.getDisplayName() + " §6✦";
        };

        player.sendMessage("§a🍖 " + message);

        // Message pour les effets spéciaux
        if (foodItem.getHealAmount() > 0) {
            player.sendMessage("   §c+" + foodItem.getHealAmount() + " ❤ §7Vie restaurée");
        }

        if (!foodItem.getEffects().isEmpty()) {
            player.sendMessage("   §d✨ §7Effets spéciaux appliqués!");
        }

        // Effet sonore bonus pour les items légendaires
        if (foodItem.getRarity() == FoodItem.FoodRarity.LEGENDARY) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            // Particules
            player.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                player.getLocation().add(0, 2, 0),
                10,
                0.5, 0.5, 0.5
            );
        }
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
}
