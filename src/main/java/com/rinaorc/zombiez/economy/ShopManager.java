package com.rinaorc.zombiez.economy;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ItemManager;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Système de boutique complet
 * Catégories: Armes, Armures, Consommables, Crates, Spécial
 */
public class ShopManager implements Listener {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<String, ShopItem> shopItems;
    
    @Getter
    private final Map<ShopCategory, List<ShopItem>> byCategory;
    
    private static final String SHOP_TITLE = "§6✦ Boutique";

    public ShopManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.shopItems = new LinkedHashMap<>();
        this.byCategory = new EnumMap<>(ShopCategory.class);
        
        registerAllItems();
        organizeByCategory();
    }

    /**
     * Enregistre tous les items de la boutique
     */
    private void registerAllItems() {
        // ============ ARMES ============
        
        register(new ShopItem("weapon_starter", "Épée de Débutant",
            "§7Une épée basique pour commencer", ShopCategory.WEAPONS,
            Material.IRON_SWORD, 100, 0,
            ShopItemType.GENERATED_ITEM, ItemType.SWORD, Rarity.COMMON, null));
        
        register(new ShopItem("weapon_uncommon", "Épée Renforcée",
            "§7Une épée de qualité supérieure", ShopCategory.WEAPONS,
            Material.IRON_SWORD, 500, 0,
            ShopItemType.GENERATED_ITEM, ItemType.SWORD, Rarity.UNCOMMON, null));
        
        register(new ShopItem("weapon_rare", "Lame Rare",
            "§7Une arme forgée avec soin", ShopCategory.WEAPONS,
            Material.DIAMOND_SWORD, 2000, 5,
            ShopItemType.GENERATED_ITEM, ItemType.SWORD, Rarity.RARE, null));
        
        register(new ShopItem("bow_starter", "Arc Simple",
            "§7Un arc basique", ShopCategory.WEAPONS,
            Material.BOW, 150, 0,
            ShopItemType.GENERATED_ITEM, ItemType.BOW, Rarity.COMMON, null));
        
        register(new ShopItem("bow_rare", "Arc de Précision",
            "§7Un arc de qualité", ShopCategory.WEAPONS,
            Material.BOW, 2500, 5,
            ShopItemType.GENERATED_ITEM, ItemType.BOW, Rarity.RARE, null));
        
        // ============ ARMURES ============
        
        register(new ShopItem("helmet_starter", "Casque de Survie",
            "§7Protection basique", ShopCategory.ARMOR,
            Material.IRON_HELMET, 80, 0,
            ShopItemType.GENERATED_ITEM, ItemType.HELMET, Rarity.COMMON, null));
        
        register(new ShopItem("chestplate_starter", "Plastron de Survie",
            "§7Protection basique", ShopCategory.ARMOR,
            Material.IRON_CHESTPLATE, 120, 0,
            ShopItemType.GENERATED_ITEM, ItemType.CHESTPLATE, Rarity.COMMON, null));
        
        register(new ShopItem("leggings_starter", "Jambières de Survie",
            "§7Protection basique", ShopCategory.ARMOR,
            Material.IRON_LEGGINGS, 100, 0,
            ShopItemType.GENERATED_ITEM, ItemType.LEGGINGS, Rarity.COMMON, null));
        
        register(new ShopItem("boots_starter", "Bottes de Survie",
            "§7Protection basique", ShopCategory.ARMOR,
            Material.IRON_BOOTS, 80, 0,
            ShopItemType.GENERATED_ITEM, ItemType.BOOTS, Rarity.COMMON, null));
        
        register(new ShopItem("armor_set_uncommon", "Set d'Armure Amélioré",
            "§7Set complet d'armure renforcée", ShopCategory.ARMOR,
            Material.DIAMOND_CHESTPLATE, 1500, 3,
            ShopItemType.ARMOR_SET, null, Rarity.UNCOMMON, null));
        
        register(new ShopItem("armor_set_rare", "Set d'Armure Rare",
            "§7Set complet de qualité rare", ShopCategory.ARMOR,
            Material.DIAMOND_CHESTPLATE, 6000, 15,
            ShopItemType.ARMOR_SET, null, Rarity.RARE, null));
        
        // ============ CONSOMMABLES ============
        
        register(new ShopItem("health_potion", "Potion de Soin",
            "§7Restaure 8 cœurs", ShopCategory.CONSUMABLES,
            Material.POTION, 50, 0,
            ShopItemType.POTION, null, null, 
            new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2)));
        
        register(new ShopItem("regen_potion", "Potion de Régénération",
            "§7Régénération pendant 30s", ShopCategory.CONSUMABLES,
            Material.POTION, 100, 0,
            ShopItemType.POTION, null, null,
            new PotionEffect(PotionEffectType.REGENERATION, 600, 1)));
        
        register(new ShopItem("speed_potion", "Potion de Vitesse",
            "§7Vitesse pendant 3 min", ShopCategory.CONSUMABLES,
            Material.POTION, 75, 0,
            ShopItemType.POTION, null, null,
            new PotionEffect(PotionEffectType.SPEED, 3600, 0)));
        
        register(new ShopItem("strength_potion", "Potion de Force",
            "§7Force pendant 3 min", ShopCategory.CONSUMABLES,
            Material.POTION, 150, 0,
            ShopItemType.POTION, null, null,
            new PotionEffect(PotionEffectType.STRENGTH, 3600, 0)));
        
        register(new ShopItem("resistance_potion", "Potion de Résistance",
            "§7Résistance pendant 2 min", ShopCategory.CONSUMABLES,
            Material.POTION, 200, 0,
            ShopItemType.POTION, null, null,
            new PotionEffect(PotionEffectType.RESISTANCE, 2400, 0)));
        
        register(new ShopItem("golden_apple", "Pomme Dorée",
            "§7Absorption + Régénération", ShopCategory.CONSUMABLES,
            Material.GOLDEN_APPLE, 100, 0,
            ShopItemType.VANILLA_ITEM, null, null, null));
        
        register(new ShopItem("enchanted_apple", "Pomme Enchantée",
            "§7Effets de survie puissants", ShopCategory.CONSUMABLES,
            Material.ENCHANTED_GOLDEN_APPLE, 0, 25,
            ShopItemType.VANILLA_ITEM, null, null, null));
        
        register(new ShopItem("totem", "Totem de Non-Mort",
            "§7Survie à un coup fatal", ShopCategory.CONSUMABLES,
            Material.TOTEM_OF_UNDYING, 0, 50,
            ShopItemType.VANILLA_ITEM, null, null, null));
        
        // ============ CRATES ============
        
        register(new ShopItem("crate_common", "Crate Commune",
            "§7Contient du loot commun", ShopCategory.CRATES,
            Material.CHEST, 200, 0,
            ShopItemType.CRATE, null, Rarity.COMMON, null));
        
        register(new ShopItem("crate_uncommon", "Crate Peu Commune",
            "§7Contient du loot amélioré", ShopCategory.CRATES,
            Material.CHEST, 500, 1,
            ShopItemType.CRATE, null, Rarity.UNCOMMON, null));
        
        register(new ShopItem("crate_rare", "Crate Rare",
            "§7Contient du loot rare", ShopCategory.CRATES,
            Material.ENDER_CHEST, 0, 10,
            ShopItemType.CRATE, null, Rarity.RARE, null));
        
        register(new ShopItem("crate_epic", "Crate Épique",
            "§7Contient du loot épique", ShopCategory.CRATES,
            Material.ENDER_CHEST, 0, 30,
            ShopItemType.CRATE, null, Rarity.EPIC, null));
        
        register(new ShopItem("crate_legendary", "Crate Légendaire",
            "§7Contient du loot légendaire!", ShopCategory.CRATES,
            Material.ENDER_CHEST, 0, 100,
            ShopItemType.CRATE, null, Rarity.LEGENDARY, null));
        
        // ============ SPÉCIAL ============
        
        register(new ShopItem("skill_reset", "Reset de Compétences",
            "§7Réinitialise ton arbre de skills", ShopCategory.SPECIAL,
            Material.TNT, 0, 50,
            ShopItemType.SERVICE, null, null, null));
        
        register(new ShopItem("name_tag", "Tag Personnalisé",
            "§7Change la couleur de ton nom", ShopCategory.SPECIAL,
            Material.NAME_TAG, 0, 100,
            ShopItemType.SERVICE, null, null, null));
        
        register(new ShopItem("xp_boost_1h", "Boost XP (1h)",
            "§7+50% XP pendant 1 heure", ShopCategory.SPECIAL,
            Material.EXPERIENCE_BOTTLE, 0, 15,
            ShopItemType.BOOST, null, null, null));
        
        register(new ShopItem("loot_boost_1h", "Boost Loot (1h)",
            "§7+25% chance de loot pendant 1h", ShopCategory.SPECIAL,
            Material.RABBIT_FOOT, 0, 20,
            ShopItemType.BOOST, null, null, null));
        
        register(new ShopItem("bank_slot", "Slot de Banque",
            "§7+1 slot de stockage permanent", ShopCategory.SPECIAL,
            Material.ENDER_CHEST, 0, 200,
            ShopItemType.SERVICE, null, null, null));
    }

    /**
     * Enregistre un item
     */
    private void register(ShopItem item) {
        shopItems.put(item.id(), item);
    }

    /**
     * Organise par catégorie
     */
    private void organizeByCategory() {
        for (ShopCategory cat : ShopCategory.values()) {
            byCategory.put(cat, new ArrayList<>());
        }
        
        for (ShopItem item : shopItems.values()) {
            byCategory.get(item.category()).add(item);
        }
    }

    /**
     * Ouvre le menu principal de la boutique
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, SHOP_TITLE);
        
        // Décoration
        fillBorder(inv, Material.GOLD_NUGGET);
        
        // Info joueur
        inv.setItem(4, new ItemBuilder(Material.EMERALD)
            .name("§eTon Solde")
            .lore(
                "§7Points: §e" + plugin.getEconomyManager().getPoints(player),
                "§7Gemmes: §d" + plugin.getEconomyManager().getGems(player)
            )
            .build());
        
        // Catégories
        inv.setItem(20, new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§c⚔ Armes")
            .lore("§7Achète des armes", "", "§eClique pour voir")
            .build());
        
        inv.setItem(21, new ItemBuilder(Material.DIAMOND_CHESTPLATE)
            .name("§b🛡 Armures")
            .lore("§7Achète des armures", "", "§eClique pour voir")
            .build());
        
        inv.setItem(22, new ItemBuilder(Material.POTION)
            .name("§a✚ Consommables")
            .lore("§7Potions et nourriture", "", "§eClique pour voir")
            .build());
        
        inv.setItem(23, new ItemBuilder(Material.CHEST)
            .name("§e📦 Crates")
            .lore("§7Coffres de loot", "", "§eClique pour voir")
            .build());
        
        inv.setItem(24, new ItemBuilder(Material.NETHER_STAR)
            .name("§d★ Spécial")
            .lore("§7Services et boosts", "", "§eClique pour voir")
            .build());
        
        // Fermer
        inv.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§cFermer")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Ouvre une catégorie
     */
    public void openCategory(Player player, ShopCategory category) {
        Inventory inv = Bukkit.createInventory(null, 54, SHOP_TITLE + " - " + category.getDisplayName());
        
        // Décoration
        fillBorder(inv, Material.GOLD_NUGGET);
        
        // Info joueur
        inv.setItem(4, new ItemBuilder(Material.EMERALD)
            .name("§eTon Solde")
            .lore(
                "§7Points: §e" + plugin.getEconomyManager().getPoints(player),
                "§7Gemmes: §d" + plugin.getEconomyManager().getGems(player)
            )
            .build());
        
        // Items
        List<ShopItem> items = byCategory.get(category);
        int slot = 10;
        
        for (ShopItem item : items) {
            List<String> lore = new ArrayList<>();
            lore.add(item.description());
            lore.add("");
            
            // Prix
            if (item.pointPrice() > 0) {
                lore.add("§7Prix: §e" + item.pointPrice() + " Points");
            }
            if (item.gemPrice() > 0) {
                lore.add("§7Prix: §d" + item.gemPrice() + " Gemmes");
            }
            
            // Peut acheter?
            boolean canAfford = canAfford(player, item);
            lore.add("");
            lore.add(canAfford ? "§aClique pour acheter" : "§cFonds insuffisants");
            
            inv.setItem(slot, new ItemBuilder(item.icon())
                .name((item.rarity() != null ? item.rarity().getColor() : "§f") + item.name())
                .lore(lore)
                .glow(canAfford)
                .build());
            
            slot++;
            if ((slot - 10) % 7 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Retour
        inv.setItem(49, new ItemBuilder(Material.ARROW)
            .name("§7Retour")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Vérifie si le joueur peut acheter
     */
    private boolean canAfford(Player player, ShopItem item) {
        if (item.pointPrice() > 0 && !plugin.getEconomyManager().hasPoints(player, item.pointPrice())) {
            return false;
        }
        if (item.gemPrice() > 0 && !plugin.getEconomyManager().hasGems(player, item.gemPrice())) {
            return false;
        }
        return true;
    }

    /**
     * Effectue un achat
     */
    public boolean purchase(Player player, String itemId) {
        ShopItem item = shopItems.get(itemId);
        if (item == null) return false;
        
        // Vérifier les fonds
        if (!canAfford(player, item)) {
            player.sendMessage("§cFonds insuffisants!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }
        
        // Vérifier l'inventaire
        if (player.getInventory().firstEmpty() == -1 && item.type() != ShopItemType.SERVICE) {
            player.sendMessage("§cInventaire plein!");
            return false;
        }
        
        // Débiter
        if (item.pointPrice() > 0) {
            plugin.getEconomyManager().removePoints(player, item.pointPrice());
        }
        if (item.gemPrice() > 0) {
            plugin.getEconomyManager().removeGems(player, item.gemPrice());
        }
        
        // Donner l'item
        giveItem(player, item);
        
        // Feedback
        player.sendMessage("§a✓ Acheté: " + item.name());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        
        return true;
    }

    /**
     * Donne l'item au joueur
     */
    private void giveItem(Player player, ShopItem item) {
        ItemManager im = plugin.getItemManager();
        
        switch (item.type()) {
            case GENERATED_ITEM -> {
                // generate(int zoneId, Rarity rarity, ItemType type, double luckBonus)
                var generatedItem = im.getGenerator().generate(1, item.rarity(), item.itemType(), 0.0);
                player.getInventory().addItem(im.buildItemStack(generatedItem));
            }

            case ARMOR_SET -> {
                // Générer un set complet
                // Using Zone 1 and 0.0 Luck for all shop items
                player.getInventory().addItem(
                    im.buildItemStack(im.getGenerator().generate(1, item.rarity(), ItemType.HELMET, 0.0)),
                    im.buildItemStack(im.getGenerator().generate(1, item.rarity(), ItemType.CHESTPLATE, 0.0)),
                    im.buildItemStack(im.getGenerator().generate(1, item.rarity(), ItemType.LEGGINGS, 0.0)),
                    im.buildItemStack(im.getGenerator().generate(1, item.rarity(), ItemType.BOOTS, 0.0))
                );
            }

            case POTION -> {
                ItemStack potion = new ItemStack(Material.SPLASH_POTION);
                org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§d" + item.name());
                    // Add the custom effect
                    meta.addCustomEffect(item.potionEffect(), true);
                    // Optional: Set color based on effect type if needed
                    // meta.setColor(item.potionEffect().getType().getColor());
                    potion.setItemMeta(meta);
                }
                player.getInventory().addItem(potion);
            }
            
            case VANILLA_ITEM -> {
                player.getInventory().addItem(new ItemStack(item.icon()));
            }
            
            case CRATE -> {
                // Ouvrir la GUI de crate
                plugin.getItemManager().getLootDropSystem().openCrateGUI(player, item.rarity());
            }
            
            case BOOST -> {
                // Appliquer le boost
                applyBoost(player, item.id());
            }
            
            case SERVICE -> {
                // Exécuter le service
                executeService(player, item.id());
            }
        }
    }

    /**
     * Applique un boost
     */
    private void applyBoost(Player player, String boostId) {
        var data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        long duration = 3600; // 1 heure en secondes
        long expireTime = System.currentTimeMillis() + (duration * 1000);
        
        switch (boostId) {
            case "xp_boost_1h" -> {
                data.setXpBoostExpire(expireTime);
                data.setXpBoostMultiplier(1.5);
                player.sendMessage("§a✓ Boost XP +50% activé pour 1 heure!");
            }
            case "loot_boost_1h" -> {
                data.setLootBoostExpire(expireTime);
                data.setLootBoostMultiplier(1.25);
                player.sendMessage("§a✓ Boost Loot +25% activé pour 1 heure!");
            }
        }
    }

    /**
     * Exécute un service
     */
    private void executeService(Player player, String serviceId) {
        switch (serviceId) {
            case "skill_reset" -> {
                plugin.getSkillTreeManager().resetSkills(player, true);
            }
            case "name_tag" -> {
                player.sendMessage("§dUtilise /nametag <couleur> pour changer ton nom!");
            }
            case "bank_slot" -> {
                var data = plugin.getPlayerDataManager().getPlayer(player);
                if (data != null) {
                    data.addBankSlots(1);
                    player.sendMessage("§a✓ +1 slot de banque ajouté!");
                }
            }
        }
    }

    /**
     * Gère les clics
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        if (!title.startsWith(SHOP_TITLE)) return;
        
        event.setCancelled(true);
        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        
        // Menu principal
        if (title.equals(SHOP_TITLE)) {
            switch (event.getSlot()) {
                case 20 -> openCategory(player, ShopCategory.WEAPONS);
                case 21 -> openCategory(player, ShopCategory.ARMOR);
                case 22 -> openCategory(player, ShopCategory.CONSUMABLES);
                case 23 -> openCategory(player, ShopCategory.CRATES);
                case 24 -> openCategory(player, ShopCategory.SPECIAL);
                case 49 -> player.closeInventory();
            }
            return;
        }
        
        // Catégorie
        if (event.getSlot() == 49) {
            openMainMenu(player);
            return;
        }
        
        // Achat
        String itemName = item.getItemMeta().getDisplayName();
        for (ShopItem shopItem : shopItems.values()) {
            if (itemName.contains(shopItem.name())) {
                purchase(player, shopItem.id());
                // Refresh
                for (ShopCategory cat : ShopCategory.values()) {
                    if (title.contains(cat.getDisplayName())) {
                        openCategory(player, cat);
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Remplit les bordures
     */
    private void fillBorder(Inventory inv, Material material) {
        ItemStack pane = new ItemBuilder(material).name(" ").build();
        
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, pane);
            inv.setItem(i + 8, pane);
        }
    }

    /**
     * Catégories de boutique
     */
    @Getter
    public enum ShopCategory {
        WEAPONS("Armes", "§c"),
        ARMOR("Armures", "§b"),
        CONSUMABLES("Consommables", "§a"),
        CRATES("Crates", "§e"),
        SPECIAL("Spécial", "§d");
        
        private final String displayName;
        private final String color;
        
        ShopCategory(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /**
     * Types d'items de boutique
     */
    public enum ShopItemType {
        GENERATED_ITEM,
        ARMOR_SET,
        POTION,
        VANILLA_ITEM,
        CRATE,
        BOOST,
        SERVICE
    }

    /**
     * Représente un item de boutique
     */
    public record ShopItem(
        String id,
        String name,
        String description,
        ShopCategory category,
        Material icon,
        int pointPrice,
        int gemPrice,
        ShopItemType type,
        ItemType itemType,
        Rarity rarity,
        PotionEffect potionEffect
    ) {}
}
