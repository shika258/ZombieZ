package com.rinaorc.zombiez.economy.shops;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.economy.gui.ShopGUI;
import com.rinaorc.zombiez.items.types.Rarity;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de tous les magasins
 */
public class ShopManager {

    private final ZombieZPlugin plugin;

    // Tous les shops
    @Getter
    private final Map<String, Shop> shops;

    // Shops par zone
    private final Map<Integer, Shop> zoneShops;

    // Historique des achats par joueur (pour limites journalières)
    private final Map<UUID, Map<String, Integer>> dailyPurchases;

    // GUI ouvertes
    private final Map<UUID, ShopGUI> openGUIs;

    public ShopManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.shops = new ConcurrentHashMap<>();
        this.zoneShops = new ConcurrentHashMap<>();
        this.dailyPurchases = new ConcurrentHashMap<>();
        this.openGUIs = new ConcurrentHashMap<>();

        initializeDefaultShops();
        startRestockTask();
        startDailyResetTask();
    }

    /**
     * Initialise les shops par défaut
     */
    private void initializeDefaultShops() {
        // Shops de zone (1-11)
        for (int i = 0; i <= 11; i++) {
            Shop zoneShop = Shop.createZoneShop(i);
            registerShop(zoneShop);
            zoneShops.put(i, zoneShop);
        }

        // Shop général (spawn)
        Shop generalShop = createGeneralShop();
        registerShop(generalShop);

        // Shop d'armes spécial
        Shop weaponShop = createWeaponShop();
        registerShop(weaponShop);

        // Shop d'armures
        Shop armorShop = createArmorShop();
        registerShop(armorShop);

        // Shop de consommables
        Shop consumableShop = createConsumableShop();
        registerShop(consumableShop);

        // Shop d'améliorations
        Shop upgradeShop = createUpgradeShop();
        registerShop(upgradeShop);

        // Marché noir
        Shop blackMarket = createBlackMarket();
        registerShop(blackMarket);

        plugin.getLogger().info("[ShopManager] " + shops.size() + " shops initialisés");
    }

    /**
     * Enregistre un shop
     */
    public void registerShop(Shop shop) {
        shops.put(shop.getId(), shop);
    }

    /**
     * Obtient un shop par ID
     */
    public Shop getShop(String shopId) {
        return shops.get(shopId);
    }

    /**
     * Obtient le shop de zone
     */
    public Shop getZoneShop(int zoneId) {
        return zoneShops.get(zoneId);
    }

    /**
     * Ouvre un shop pour un joueur
     */
    public void openShop(Player player, String shopId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            player.sendMessage("§cShop introuvable!");
            return;
        }

        openShop(player, shop);
    }

    /**
     * Ouvre un shop pour un joueur
     */
    public void openShop(Player player, Shop shop) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cErreur: données joueur non chargées.");
            return;
        }

        // Vérifier l'accès
        if (!shop.canAccess(data.getLevel().get(), data.getMaxZoneReached())) {
            player.sendMessage("§cVous n'avez pas accès à ce magasin!");
            player.sendMessage("§7Niveau requis: §e" + shop.getRequiredLevel() +
                " §7| Zone requise: §e" + shop.getRequiredZone());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Fermer l'ancienne GUI si ouverte
        ShopGUI oldGui = openGUIs.remove(player.getUniqueId());
        if (oldGui != null) {
            player.closeInventory();
        }

        // Ouvrir la nouvelle GUI
        ShopGUI gui = new ShopGUI(plugin, player, shop, this);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Ferme le shop d'un joueur
     */
    public void closeShop(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    /**
     * Traite un achat
     */
    public PurchaseResult processPurchase(Player player, Shop shop, ShopItem item, int quantity, boolean useGems) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) {
            return new PurchaseResult(false, "Données joueur non chargées");
        }

        // Vérifier le stock
        if (item.getStock() == 0) {
            return new PurchaseResult(false, "Rupture de stock!");
        }
        if (item.getStock() > 0 && item.getStock() < quantity) {
            return new PurchaseResult(false, "Stock insuffisant (reste: " + item.getStock() + ")");
        }

        // Vérifier les limites journalières
        if (item.getMaxPurchasePerDay() > 0) {
            int purchased = getDailyPurchases(player.getUniqueId(), item.getId());
            if (purchased + quantity > item.getMaxPurchasePerDay()) {
                return new PurchaseResult(false, "Limite journalière atteinte (" + item.getMaxPurchasePerDay() + "/jour)");
            }
        }

        // Vérifier les restrictions
        if (item.getRequiredLevel() > data.getLevel().get()) {
            return new PurchaseResult(false, "Niveau " + item.getRequiredLevel() + " requis");
        }
        if (item.getRequiredZone() > data.getMaxZoneReached()) {
            return new PurchaseResult(false, "Zone " + item.getRequiredZone() + " requise");
        }

        // Calculer le prix total
        int totalPrice;
        String currency;

        if (useGems && item.getGemPrice() > 0) {
            totalPrice = item.getGemPrice() * quantity;
            if (data.getGems().get() < totalPrice) {
                return new PurchaseResult(false, "Gemmes insuffisantes (besoin: " + totalPrice + ")");
            }
            data.removeGems(totalPrice);
            currency = "gemmes";
        } else {
            totalPrice = shop.getAdjustedBuyPrice(item) * quantity;
            if (data.getPoints().get() < totalPrice) {
                return new PurchaseResult(false, "Points insuffisants (besoin: " + totalPrice + ")");
            }
            data.removePoints(totalPrice);
            currency = "points";
        }

        // Donner les items
        for (int i = 0; i < quantity; i++) {
            ItemStack realItem = item.createRealItem();
            if (realItem != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(realItem);
                if (!overflow.isEmpty()) {
                    // Drop les items qui ne rentrent pas
                    overflow.values().forEach(drop ->
                        player.getWorld().dropItemNaturally(player.getLocation(), drop));
                }
            }
        }

        // Mettre à jour le stock
        if (item.getStock() > 0) {
            item.setStock(item.getStock() - quantity);
        }

        // Enregistrer l'achat journalier
        if (item.getMaxPurchasePerDay() > 0) {
            addDailyPurchase(player.getUniqueId(), item.getId(), quantity);
        }

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        return new PurchaseResult(true,
            "Acheté §e" + quantity + "x " + item.getDisplayName() + " §apour §6" + totalPrice + " " + currency);
    }

    /**
     * Traite une vente
     */
    public SellResult processSale(Player player, Shop shop, ItemStack itemToSell, int quantity) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) {
            return new SellResult(false, "Données joueur non chargées", 0);
        }

        // Trouver l'item correspondant dans le shop
        ShopItem shopItem = findMatchingShopItem(shop, itemToSell);
        if (shopItem == null || shopItem.getSellPrice() <= 0) {
            return new SellResult(false, "Cet item ne peut pas être vendu ici", 0);
        }

        // Vérifier que le joueur a assez d'items
        int playerHas = countItem(player, itemToSell);
        if (playerHas < quantity) {
            return new SellResult(false, "Vous n'avez pas assez d'items (vous avez: " + playerHas + ")", 0);
        }

        // Calculer le prix
        int totalPrice = shop.getAdjustedSellPrice(shopItem) * quantity;

        // Retirer les items
        removeItems(player, itemToSell, quantity);

        // Donner les points
        data.addPoints(totalPrice);

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);

        return new SellResult(true,
            "Vendu §e" + quantity + "x " + shopItem.getDisplayName() + " §apour §6" + totalPrice + " points", totalPrice);
    }

    /**
     * Trouve un ShopItem correspondant à un ItemStack
     */
    private ShopItem findMatchingShopItem(Shop shop, ItemStack item) {
        for (ShopItem shopItem : shop.getItems().values()) {
            if (shopItem.getMaterial() == item.getType()) {
                return shopItem;
            }
        }
        return null;
    }

    /**
     * Compte les items d'un type dans l'inventaire
     */
    private int countItem(Player player, ItemStack item) {
        int count = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                count += invItem.getAmount();
            }
        }
        return count;
    }

    /**
     * Retire des items de l'inventaire
     */
    private void removeItems(Player player, ItemStack item, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack invItem = contents[i];
            if (invItem != null && invItem.isSimilar(item)) {
                int take = Math.min(invItem.getAmount(), remaining);
                invItem.setAmount(invItem.getAmount() - take);
                remaining -= take;

                if (invItem.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    /**
     * Obtient les achats journaliers d'un joueur pour un item
     */
    private int getDailyPurchases(UUID playerId, String itemId) {
        return dailyPurchases.getOrDefault(playerId, Collections.emptyMap())
            .getOrDefault(itemId, 0);
    }

    /**
     * Ajoute un achat journalier
     */
    private void addDailyPurchase(UUID playerId, String itemId, int quantity) {
        dailyPurchases.computeIfAbsent(playerId, k -> new HashMap<>())
            .merge(itemId, quantity, Integer::sum);
    }

    /**
     * Démarre la tâche de restock
     */
    private void startRestockTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                shops.values().forEach(Shop::restockAll);
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Toutes les minutes
    }

    /**
     * Démarre la tâche de reset journalier
     */
    private void startDailyResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                dailyPurchases.clear();
                plugin.getLogger().info("[ShopManager] Limites d'achat journalières réinitialisées");
            }
        }.runTaskTimer(plugin, 24000L, 24000L * 20); // Toutes les 24h Minecraft (~20min réel)
    }

    // ==================== CRÉATION DES SHOPS ====================

    private Shop createGeneralShop() {
        Shop shop = Shop.builder()
            .id("general")
            .displayName("§aMagasin Général")
            .type(Shop.ShopType.GENERAL)
            .icon(Material.CHEST)
            .requiredZone(0)
            .requiredLevel(1)
            .build();

        shop.addItem(ShopItem.vanilla("bread", "Pain", Material.BREAD, 10, 2));
        shop.addItem(ShopItem.vanilla("cooked_beef", "Steak", Material.COOKED_BEEF, 25, 5));
        shop.addItem(ShopItem.vanilla("golden_carrot", "Carotte Dorée", Material.GOLDEN_CARROT, 100, 20));
        shop.addItem(ShopItem.vanilla("torch", "Torches (x16)", Material.TORCH, 15, 2));
        shop.addItem(ShopItem.vanilla("crafting_table", "Table de Craft", Material.CRAFTING_TABLE, 50, 10));
        shop.addItem(ShopItem.vanilla("chest", "Coffre", Material.CHEST, 30, 6));
        shop.addItem(ShopItem.vanilla("ender_chest", "Coffre de l'Ender", Material.ENDER_CHEST, 500, 100));

        return shop;
    }

    private Shop createWeaponShop() {
        Shop shop = Shop.builder()
            .id("weapons")
            .displayName("§cArmurerie - Armes")
            .type(Shop.ShopType.WEAPONS)
            .icon(Material.IRON_SWORD)
            .requiredZone(0)
            .requiredLevel(1)
            .build();

        // Épées
        shop.addItem(ShopItem.vanilla("wooden_sword", "Épée en Bois", Material.WOODEN_SWORD, 30, 6));
        shop.addItem(ShopItem.vanilla("stone_sword", "Épée en Pierre", Material.STONE_SWORD, 80, 16));
        shop.addItem(ShopItem.vanilla("iron_sword", "Épée en Fer", Material.IRON_SWORD, 250, 50));
        shop.addItem(ShopItem.vanilla("diamond_sword", "Épée en Diamant", Material.DIAMOND_SWORD, 1200, 240));
        shop.addItem(ShopItem.vanilla("netherite_sword", "Épée en Netherite", Material.NETHERITE_SWORD, 8000, 1600));

        // Haches
        shop.addItem(ShopItem.vanilla("iron_axe", "Hache en Fer", Material.IRON_AXE, 300, 60));
        shop.addItem(ShopItem.vanilla("diamond_axe", "Hache en Diamant", Material.DIAMOND_AXE, 1400, 280));

        // Arcs
        shop.addItem(ShopItem.vanilla("bow", "Arc", Material.BOW, 200, 40));
        shop.addItem(ShopItem.vanilla("crossbow", "Arbalète", Material.CROSSBOW, 400, 80));
        shop.addItem(ShopItem.vanilla("arrow", "Flèches (x16)", Material.ARROW, 50, 8));
        shop.addItem(ShopItem.vanilla("spectral_arrow", "Flèches Spectrales (x8)", Material.SPECTRAL_ARROW, 150, 25));

        // Trident
        shop.addItem(ShopItem.builder()
            .id("trident")
            .displayName("Trident")
            .material(Material.TRIDENT)
            .buyPrice(5000)
            .sellPrice(1000)
            .stock(3)
            .maxStock(3)
            .restockInterval(3600000)
            .restockAmount(1)
            .requiredZone(5)
            .type(ShopItem.ShopItemType.VANILLA)
            .build());

        return shop;
    }

    private Shop createArmorShop() {
        Shop shop = Shop.builder()
            .id("armor")
            .displayName("§9Armurerie - Armures")
            .type(Shop.ShopType.ARMOR)
            .icon(Material.IRON_CHESTPLATE)
            .requiredZone(0)
            .requiredLevel(1)
            .build();

        // Cuir
        shop.addItem(ShopItem.vanilla("leather_helmet", "Casque en Cuir", Material.LEATHER_HELMET, 40, 8));
        shop.addItem(ShopItem.vanilla("leather_chestplate", "Plastron en Cuir", Material.LEATHER_CHESTPLATE, 80, 16));
        shop.addItem(ShopItem.vanilla("leather_leggings", "Jambières en Cuir", Material.LEATHER_LEGGINGS, 70, 14));
        shop.addItem(ShopItem.vanilla("leather_boots", "Bottes en Cuir", Material.LEATHER_BOOTS, 35, 7));

        // Fer
        shop.addItem(ShopItem.vanilla("iron_helmet", "Casque en Fer", Material.IRON_HELMET, 200, 40));
        shop.addItem(ShopItem.vanilla("iron_chestplate", "Plastron en Fer", Material.IRON_CHESTPLATE, 400, 80));
        shop.addItem(ShopItem.vanilla("iron_leggings", "Jambières en Fer", Material.IRON_LEGGINGS, 350, 70));
        shop.addItem(ShopItem.vanilla("iron_boots", "Bottes en Fer", Material.IRON_BOOTS, 175, 35));

        // Diamant
        shop.addItem(ShopItem.vanilla("diamond_helmet", "Casque en Diamant", Material.DIAMOND_HELMET, 1000, 200));
        shop.addItem(ShopItem.vanilla("diamond_chestplate", "Plastron en Diamant", Material.DIAMOND_CHESTPLATE, 2000, 400));
        shop.addItem(ShopItem.vanilla("diamond_leggings", "Jambières en Diamant", Material.DIAMOND_LEGGINGS, 1750, 350));
        shop.addItem(ShopItem.vanilla("diamond_boots", "Bottes en Diamant", Material.DIAMOND_BOOTS, 875, 175));

        // Netherite
        shop.addItem(ShopItem.vanilla("netherite_helmet", "Casque en Netherite", Material.NETHERITE_HELMET, 6000, 1200));
        shop.addItem(ShopItem.vanilla("netherite_chestplate", "Plastron en Netherite", Material.NETHERITE_CHESTPLATE, 12000, 2400));
        shop.addItem(ShopItem.vanilla("netherite_leggings", "Jambières en Netherite", Material.NETHERITE_LEGGINGS, 10500, 2100));
        shop.addItem(ShopItem.vanilla("netherite_boots", "Bottes en Netherite", Material.NETHERITE_BOOTS, 5250, 1050));

        // Bouclier
        shop.addItem(ShopItem.vanilla("shield", "Bouclier", Material.SHIELD, 250, 50));

        return shop;
    }

    private Shop createConsumableShop() {
        Shop shop = Shop.builder()
            .id("consumables")
            .displayName("§dAlchimiste")
            .type(Shop.ShopType.CONSUMABLES)
            .icon(Material.POTION)
            .requiredZone(0)
            .requiredLevel(1)
            .build();

        // Potions de soin
        shop.addItem(ShopItem.consumable("healing_1", "Potion de Soin I", Material.POTION, 75,
            List.of("§7Restaure §c4 cœurs", "§7Instantané")));
        shop.addItem(ShopItem.consumable("healing_2", "Potion de Soin II", Material.POTION, 200,
            List.of("§7Restaure §c8 cœurs", "§7Instantané")));

        // Régénération
        shop.addItem(ShopItem.consumable("regen_1", "Potion de Régénération", Material.POTION, 150,
            List.of("§7Régénération pendant", "§7§e45 secondes")));

        // Force
        shop.addItem(ShopItem.consumable("strength_1", "Potion de Force I", Material.POTION, 300,
            List.of("§7+§c3 dégâts", "§7Durée: §e3 minutes")));
        shop.addItem(ShopItem.consumable("strength_2", "Potion de Force II", Material.POTION, 750,
            List.of("§7+§c6 dégâts", "§7Durée: §e1:30 minutes")));

        // Vitesse
        shop.addItem(ShopItem.consumable("speed_1", "Potion de Vitesse I", Material.POTION, 200,
            List.of("§7+20% vitesse", "§7Durée: §e3 minutes")));
        shop.addItem(ShopItem.consumable("speed_2", "Potion de Vitesse II", Material.POTION, 500,
            List.of("§7+40% vitesse", "§7Durée: §e1:30 minutes")));

        // Résistance au feu
        shop.addItem(ShopItem.consumable("fire_resistance", "Potion Anti-Feu", Material.POTION, 250,
            List.of("§7Immunité au feu", "§7Durée: §e3 minutes")));

        // Pommes
        shop.addItem(ShopItem.vanilla("golden_apple", "Pomme Dorée", Material.GOLDEN_APPLE, 500, 100));
        shop.addItem(ShopItem.builder()
            .id("enchanted_golden_apple")
            .displayName("Pomme Enchantée")
            .material(Material.ENCHANTED_GOLDEN_APPLE)
            .buyPrice(5000)
            .sellPrice(1000)
            .stock(5)
            .maxStock(5)
            .restockInterval(7200000)
            .restockAmount(1)
            .requiredLevel(10)
            .type(ShopItem.ShopItemType.VANILLA)
            .build());

        // Totem
        shop.addItem(ShopItem.builder()
            .id("totem")
            .displayName("Totem d'Immortalité")
            .material(Material.TOTEM_OF_UNDYING)
            .buyPrice(10000)
            .sellPrice(2000)
            .stock(2)
            .maxStock(2)
            .restockInterval(14400000)
            .restockAmount(1)
            .requiredZone(7)
            .type(ShopItem.ShopItemType.VANILLA)
            .build());

        return shop;
    }

    private Shop createUpgradeShop() {
        Shop shop = Shop.builder()
            .id("upgrades")
            .displayName("§6Forge Mystique")
            .type(Shop.ShopType.UPGRADES)
            .icon(Material.ANVIL)
            .requiredZone(3)
            .requiredLevel(5)
            .build();

        shop.addItem(ShopItem.builder()
            .id("upgrade_stone_common")
            .displayName("Pierre d'Amélioration Commune")
            .material(Material.AMETHYST_SHARD)
            .lore(List.of("§7Améliore un item COMMUN", "§7vers UNCOMMON", "§7Taux de succès: §a90%"))
            .buyPrice(500)
            .sellPrice(0)
            .gemPrice(5)
            .stock(-1)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        shop.addItem(ShopItem.builder()
            .id("upgrade_stone_uncommon")
            .displayName("Pierre d'Amélioration Uncommon")
            .material(Material.PRISMARINE_SHARD)
            .lore(List.of("§7Améliore un item UNCOMMON", "§7vers RARE", "§7Taux de succès: §e70%"))
            .buyPrice(1500)
            .sellPrice(0)
            .gemPrice(15)
            .stock(-1)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        shop.addItem(ShopItem.builder()
            .id("upgrade_stone_rare")
            .displayName("Pierre d'Amélioration Rare")
            .material(Material.LAPIS_LAZULI)
            .lore(List.of("§7Améliore un item RARE", "§7vers EPIC", "§7Taux de succès: §650%"))
            .buyPrice(5000)
            .sellPrice(0)
            .gemPrice(50)
            .stock(10)
            .maxStock(10)
            .restockInterval(3600000)
            .restockAmount(2)
            .requiredZone(5)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        shop.addItem(ShopItem.builder()
            .id("upgrade_stone_epic")
            .displayName("Pierre d'Amélioration Épique")
            .material(Material.DIAMOND)
            .lore(List.of("§7Améliore un item EPIC", "§7vers LEGENDARY", "§7Taux de succès: §c30%"))
            .buyPrice(20000)
            .sellPrice(0)
            .gemPrice(200)
            .stock(5)
            .maxStock(5)
            .restockInterval(7200000)
            .restockAmount(1)
            .requiredZone(7)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        shop.addItem(ShopItem.builder()
            .id("reroll_stone")
            .displayName("Pierre de Reforge")
            .material(Material.ECHO_SHARD)
            .lore(List.of("§7Réinitialise les stats", "§7d'un item ZombieZ", "§7Conserve la rareté"))
            .buyPrice(3000)
            .sellPrice(0)
            .gemPrice(30)
            .stock(-1)
            .requiredZone(4)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        shop.addItem(ShopItem.builder()
            .id("affix_scroll")
            .displayName("Parchemin d'Affix")
            .material(Material.PAPER)
            .lore(List.of("§7Ajoute un affix aléatoire", "§7à un item ZombieZ", "§7(Max affixes selon rareté)"))
            .buyPrice(8000)
            .sellPrice(0)
            .gemPrice(80)
            .stock(3)
            .maxStock(3)
            .restockInterval(7200000)
            .restockAmount(1)
            .requiredZone(6)
            .type(ShopItem.ShopItemType.UPGRADE)
            .build());

        return shop;
    }

    private Shop createBlackMarket() {
        Shop shop = Shop.builder()
            .id("black_market")
            .displayName("§8Marché Noir")
            .type(Shop.ShopType.BLACK_MARKET)
            .icon(Material.ENDER_CHEST)
            .requiredZone(6)
            .requiredLevel(15)
            .build();

        shop.addItem(ShopItem.builder()
            .id("mystery_weapon")
            .displayName("Arme Mystère")
            .material(Material.NETHER_STAR)
            .lore(List.of("§7Recevez une arme aléatoire", "§7de rareté §9RARE §7ou plus"))
            .buyPrice(10000)
            .gemPrice(100)
            .stock(1)
            .maxStock(1)
            .restockInterval(3600000)
            .restockAmount(1)
            .zombiezItemType(null) // Aléatoire
            .minRarity(Rarity.RARE)
            .itemTier(3)
            .type(ShopItem.ShopItemType.ZOMBIEZ_ITEM)
            .build());

        shop.addItem(ShopItem.builder()
            .id("epic_gear_box")
            .displayName("Coffre Équipement Épique")
            .material(Material.SHULKER_BOX)
            .lore(List.of("§7Recevez un équipement", "§7de rareté §5ÉPIQUE §7garantie"))
            .buyPrice(0)
            .gemPrice(500)
            .stock(1)
            .maxStock(1)
            .restockInterval(86400000)
            .restockAmount(1)
            .minRarity(Rarity.EPIC)
            .itemTier(4)
            .type(ShopItem.ShopItemType.ZOMBIEZ_ITEM)
            .requiredZone(8)
            .build());

        shop.addItem(ShopItem.builder()
            .id("legendary_key")
            .displayName("§6Clé Légendaire")
            .material(Material.TRIPWIRE_HOOK)
            .lore(List.of("§7Ouvre un coffre légendaire", "§7au Spawn"))
            .buyPrice(0)
            .gemPrice(1000)
            .stock(1)
            .maxStock(1)
            .restockInterval(172800000)
            .restockAmount(1)
            .requiredZone(9)
            .type(ShopItem.ShopItemType.SPECIAL)
            .build());

        return shop;
    }

    /**
     * Résultat d'un achat
     */
    public record PurchaseResult(boolean success, String message) {}

    /**
     * Résultat d'une vente
     */
    public record SellResult(boolean success, String message, int earned) {}
}
