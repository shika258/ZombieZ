package com.rinaorc.zombiez.shops;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manager principal des shops
 * Gère les shops de zone, le vendeur, et le shop de gemmes
 */
public class ShopManager {

    private final ZombieZPlugin plugin;
    
    // Shops par zone
    @Getter
    private final Map<Integer, ZoneShop> zoneShops;
    
    // Prix de vente des items par rareté
    private final Map<Rarity, Integer> sellPrices;
    
    // Items du shop de gemmes
    @Getter
    private final List<GemShopItem> gemShopItems;

    public ShopManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.zoneShops = new HashMap<>();
        this.sellPrices = new EnumMap<>(Rarity.class);
        this.gemShopItems = new ArrayList<>();
        
        initializeSellPrices();
        initializeZoneShops();
        initializeGemShop();
    }

    /**
     * Initialise les prix de vente par rareté
     */
    private void initializeSellPrices() {
        sellPrices.put(Rarity.COMMON, 10);
        sellPrices.put(Rarity.UNCOMMON, 25);
        sellPrices.put(Rarity.RARE, 75);
        sellPrices.put(Rarity.EPIC, 200);
        sellPrices.put(Rarity.LEGENDARY, 500);
        sellPrices.put(Rarity.MYTHIC, 1500);
        sellPrices.put(Rarity.EXALTED, 5000);
    }

    /**
     * Initialise les shops par zone
     */
    private void initializeZoneShops() {
        // Zone 0 - Spawn (Shop principal)
        ZoneShop spawnShop = new ZoneShop(0, "§6Marchand du Refuge");
        spawnShop.addItem(new ShopItem("health_potion", Material.POTION, "§cPotion de Vie", 
            "§7Restaure 4 cœurs", 50, ShopItem.Currency.POINTS));
        spawnShop.addItem(new ShopItem("food_pack", Material.COOKED_BEEF, "§6Pack de Nourriture x16",
            "§7Steak cuit x16", 30, ShopItem.Currency.POINTS));
        spawnShop.addItem(new ShopItem("torch_pack", Material.TORCH, "§eTorches x64",
            "§7Éclairez votre chemin", 20, ShopItem.Currency.POINTS));
        spawnShop.addItem(new ShopItem("basic_sword", Material.IRON_SWORD, "§fÉpée de Fer",
            "§7Arme de base", 100, ShopItem.Currency.POINTS));
        spawnShop.addItem(new ShopItem("basic_armor", Material.IRON_CHESTPLATE, "§fPlastron de Fer",
            "§7Protection de base", 150, ShopItem.Currency.POINTS));
        zoneShops.put(0, spawnShop);

        // Zone 1 - Village
        ZoneShop zone1Shop = new ZoneShop(1, "§eMarchand du Village");
        zone1Shop.addItem(new ShopItem("wooden_sword", Material.WOODEN_SWORD, "§6Épée en Bois",
            "§7Premier équipement", 25, ShopItem.Currency.POINTS));
        zone1Shop.addItem(new ShopItem("leather_armor", Material.LEATHER_CHESTPLATE, "§6Armure en Cuir",
            "§7Protection légère", 40, ShopItem.Currency.POINTS));
        zone1Shop.addItem(new ShopItem("bandage", Material.PAPER, "§cBandage",
            "§7Soigne 2 cœurs", 15, ShopItem.Currency.POINTS));
        zoneShops.put(1, zone1Shop);

        // Zone 3 - Désert
        ZoneShop zone3Shop = new ZoneShop(3, "§eMarchand du Désert");
        zone3Shop.addItem(new ShopItem("water_bottle", Material.POTION, "§bBouteille d'Eau",
            "§7Élimine les effets de soif", 20, ShopItem.Currency.POINTS));
        zone3Shop.addItem(new ShopItem("sun_hat", Material.LEATHER_HELMET, "§eChapeau de Soleil",
            "§7Résistance à la chaleur", 75, ShopItem.Currency.POINTS));
        zone3Shop.addItem(new ShopItem("cactus_bomb", Material.CACTUS, "§aBombe Cactus x3",
            "§7Explosif naturel", 100, ShopItem.Currency.POINTS));
        zoneShops.put(3, zone3Shop);

        // Zone 5 - Marécages
        ZoneShop zone5Shop = new ZoneShop(5, "§2Marchand des Marécages");
        zone5Shop.addItem(new ShopItem("antidote", Material.MILK_BUCKET, "§aAntidote",
            "§7Élimine le poison", 50, ShopItem.Currency.POINTS));
        zone5Shop.addItem(new ShopItem("swamp_boots", Material.LEATHER_BOOTS, "§2Bottes des Marais",
            "§7Vitesse dans l'eau", 120, ShopItem.Currency.POINTS));
        zone5Shop.addItem(new ShopItem("gas_mask", Material.CARVED_PUMPKIN, "§8Masque à Gaz",
            "§7Immunité aux nuages toxiques", 200, ShopItem.Currency.POINTS));
        zoneShops.put(5, zone5Shop);

        // Zone 7 - Montagnes
        ZoneShop zone7Shop = new ZoneShop(7, "§7Marchand des Montagnes");
        zone7Shop.addItem(new ShopItem("climbing_gear", Material.LEAD, "§7Équipement d'Escalade",
            "§7Moins de chutes", 150, ShopItem.Currency.POINTS));
        zone7Shop.addItem(new ShopItem("warm_coat", Material.LEATHER_CHESTPLATE, "§bManteau Chaud",
            "§7Résistance au froid", 180, ShopItem.Currency.POINTS));
        zone7Shop.addItem(new ShopItem("pickaxe_steel", Material.IRON_PICKAXE, "§8Pioche d'Acier",
            "§7Mine plus vite", 200, ShopItem.Currency.POINTS));
        zoneShops.put(7, zone7Shop);

        // Zone 8 - Toundra
        ZoneShop zone8Shop = new ZoneShop(8, "§bMarchand de la Toundra");
        zone8Shop.addItem(new ShopItem("heat_pack", Material.MAGMA_CREAM, "§6Pack Chauffant",
            "§7Élimine le gel", 75, ShopItem.Currency.POINTS));
        zone8Shop.addItem(new ShopItem("fur_armor", Material.LEATHER_CHESTPLATE, "§fArmure de Fourrure",
            "§7Immunité au gel", 300, ShopItem.Currency.POINTS));
        zone8Shop.addItem(new ShopItem("ice_pick", Material.DIAMOND_PICKAXE, "§bPic à Glace",
            "§7Brise la glace facilement", 250, ShopItem.Currency.POINTS));
        zoneShops.put(8, zone8Shop);

        // Zone 10 - Enfer
        ZoneShop zone10Shop = new ZoneShop(10, "§4Marchand de l'Enfer");
        zone10Shop.addItem(new ShopItem("fire_resist_potion", Material.POTION, "§6Potion Anti-Feu",
            "§7Immunité au feu 5min", 200, ShopItem.Currency.POINTS));
        zone10Shop.addItem(new ShopItem("demon_blade", Material.NETHERITE_SWORD, "§4Lame Démoniaque",
            "§7+20% dégâts de feu", 1000, ShopItem.Currency.POINTS));
        zone10Shop.addItem(new ShopItem("soul_shield", Material.SHIELD, "§5Bouclier des Âmes",
            "§7Absorbe les dégâts magiques", 800, ShopItem.Currency.POINTS));
        zoneShops.put(10, zone10Shop);
    }

    /**
     * Initialise le shop de gemmes (cosmétiques et boosters)
     */
    private void initializeGemShop() {
        // Titres
        gemShopItems.add(new GemShopItem("title_survivor", GemShopItem.Type.TITLE,
            "§aTitre: Survivant", "§7Affiche §aSurvivant §7devant votre nom", 10));
        gemShopItems.add(new GemShopItem("title_hunter", GemShopItem.Type.TITLE,
            "§cTitre: Chasseur", "§7Affiche §cChasseur §7devant votre nom", 25));
        gemShopItems.add(new GemShopItem("title_legend", GemShopItem.Type.TITLE,
            "§6Titre: Légende", "§7Affiche §6Légende §7devant votre nom", 100));
        gemShopItems.add(new GemShopItem("title_immortal", GemShopItem.Type.TITLE,
            "§5Titre: Immortel", "§7Affiche §5Immortel §7devant votre nom", 250));

        // Particules de mort
        gemShopItems.add(new GemShopItem("death_flames", GemShopItem.Type.DEATH_EFFECT,
            "§6Mort: Flammes", "§7Explosez en flammes à votre mort", 50));
        gemShopItems.add(new GemShopItem("death_souls", GemShopItem.Type.DEATH_EFFECT,
            "§5Mort: Âmes", "§7Libérez des âmes à votre mort", 75));
        gemShopItems.add(new GemShopItem("death_lightning", GemShopItem.Type.DEATH_EFFECT,
            "§eMort: Foudre", "§7Un éclair frappe à votre mort", 100));

        // Trails de marche
        gemShopItems.add(new GemShopItem("trail_fire", GemShopItem.Type.TRAIL,
            "§6Trail: Feu", "§7Laissez une traînée de feu", 30));
        gemShopItems.add(new GemShopItem("trail_hearts", GemShopItem.Type.TRAIL,
            "§cTrail: Cœurs", "§7Laissez une traînée de cœurs", 30));
        gemShopItems.add(new GemShopItem("trail_magic", GemShopItem.Type.TRAIL,
            "§dTrail: Magie", "§7Laissez une traînée magique", 50));

        // Boosters
        gemShopItems.add(new GemShopItem("booster_xp_1h", GemShopItem.Type.BOOSTER,
            "§aBooster XP 1h", "§7+50% XP pendant 1 heure", 15));
        gemShopItems.add(new GemShopItem("booster_points_1h", GemShopItem.Type.BOOSTER,
            "§eBooster Points 1h", "§7+50% Points pendant 1 heure", 15));
        gemShopItems.add(new GemShopItem("booster_luck_1h", GemShopItem.Type.BOOSTER,
            "§bBooster Chance 1h", "§7+25% Chance de loot pendant 1 heure", 20));
        gemShopItems.add(new GemShopItem("booster_all_1h", GemShopItem.Type.BOOSTER,
            "§6Booster Total 1h", "§7Tous les boosters pendant 1 heure", 40));

        // Pets cosmétiques
        gemShopItems.add(new GemShopItem("pet_bat", GemShopItem.Type.PET,
            "§8Pet: Chauve-souris", "§7Une chauve-souris vous suit", 100));
        gemShopItems.add(new GemShopItem("pet_ghost", GemShopItem.Type.PET,
            "§fPet: Fantôme", "§7Un petit fantôme vous suit", 150));
        gemShopItems.add(new GemShopItem("pet_demon", GemShopItem.Type.PET,
            "§4Pet: Démon", "§7Un mini-démon vous suit", 300));

        // Caisses
        gemShopItems.add(new GemShopItem("crate_common", GemShopItem.Type.CRATE,
            "§fCaisse Commune", "§7Contient un item aléatoire Rare+", 5));
        gemShopItems.add(new GemShopItem("crate_rare", GemShopItem.Type.CRATE,
            "§9Caisse Rare", "§7Contient un item aléatoire Épique+", 15));
        gemShopItems.add(new GemShopItem("crate_legendary", GemShopItem.Type.CRATE,
            "§6Caisse Légendaire", "§7Contient un item aléatoire Légendaire+", 50));
    }

    /**
     * Obtient le shop d'une zone
     */
    public ZoneShop getZoneShop(int zoneId) {
        // Fallback au shop du spawn si pas de shop dans cette zone
        return zoneShops.getOrDefault(zoneId, zoneShops.get(0));
    }

    /**
     * Calcule le prix de vente d'un item ZombieZ
     */
    public int calculateSellPrice(ZombieZItem item) {
        int basePrice = sellPrices.getOrDefault(item.getRarity(), 10);
        
        // Bonus selon le score de l'item
        double scoreMultiplier = 1 + (item.getItemScore() / 1000.0);
        
        // Bonus selon le tier
        double tierMultiplier = 1 + (item.getTier() * 0.2);
        
        return (int) (basePrice * scoreMultiplier * tierMultiplier);
    }

    /**
     * Vend un item et donne les points au joueur
     */
    public boolean sellItem(Player player, ItemStack itemStack) {
        if (!ZombieZItem.isZombieZItem(itemStack)) {
            return false;
        }
        
        ZombieZItem item = ZombieZItem.fromItemStack(itemStack);
        if (item == null) return false;
        
        int price = calculateSellPrice(item);
        
        // Donner les points
        plugin.getEconomyManager().addPoints(player, price);
        
        // Retirer l'item
        player.getInventory().removeItem(itemStack);
        
        // Notification
        player.sendMessage("§aVendu §e" + item.getGeneratedName() + " §apour §6" + price + " Points§a!");
        
        return true;
    }

    /**
     * Achète un item du shop
     */
    public boolean buyShopItem(Player player, ShopItem item) {
        // Vérifier la monnaie
        boolean hasEnough = switch (item.getCurrency()) {
            case POINTS -> plugin.getEconomyManager().getPoints(player) >= item.getPrice();
            case GEMS -> plugin.getEconomyManager().getGems(player) >= item.getPrice();
        };
        
        if (!hasEnough) {
            player.sendMessage("§cVous n'avez pas assez de " + 
                (item.getCurrency() == ShopItem.Currency.POINTS ? "Points" : "Gemmes") + "!");
            return false;
        }
        
        // Vérifier l'inventaire
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cVotre inventaire est plein!");
            return false;
        }
        
        // Retirer la monnaie
        switch (item.getCurrency()) {
            case POINTS -> plugin.getEconomyManager().removePoints(player, item.getPrice());
            case GEMS -> plugin.getEconomyManager().removeGems(player, item.getPrice());
        }
        
        // Donner l'item
        ItemStack itemStack = item.createItemStack();
        player.getInventory().addItem(itemStack);
        
        player.sendMessage("§aAchat réussi: §e" + item.getName() + " §apour §6" + 
            item.getPrice() + " " + (item.getCurrency() == ShopItem.Currency.POINTS ? "Points" : "Gemmes") + "§a!");
        
        return true;
    }

    /**
     * Achète un item du shop de gemmes
     */
    public boolean buyGemShopItem(Player player, GemShopItem item) {
        if (plugin.getEconomyManager().getGems(player) < item.getPrice()) {
            player.sendMessage("§cVous n'avez pas assez de Gemmes!");
            return false;
        }
        
        // Vérifier si déjà possédé (pour les cosmétiques)
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null && item.getType() != GemShopItem.Type.BOOSTER && 
            item.getType() != GemShopItem.Type.CRATE) {
            if (playerData.hasCosmetic(item.getId())) {
                player.sendMessage("§cVous possédez déjà cet item!");
                return false;
            }
        }
        
        // Retirer les gemmes
        plugin.getEconomyManager().removeGems(player, item.getPrice());
        
        // Appliquer l'achat
        applyGemPurchase(player, item);
        
        player.sendMessage("§d✓ Achat réussi: §e" + item.getName() + " §dpour §b" + item.getPrice() + " Gemmes§d!");
        
        return true;
    }

    /**
     * Applique un achat du shop de gemmes
     */
    private void applyGemPurchase(Player player, GemShopItem item) {
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData == null) return;
        
        switch (item.getType()) {
            case TITLE, DEATH_EFFECT, TRAIL, PET -> {
                playerData.addCosmetic(item.getId());
            }
            case BOOSTER -> {
                // Activer le booster
                long duration = 3600000; // 1 heure
                long expiry = System.currentTimeMillis() + duration;
                
                if (item.getId().contains("xp")) {
                    playerData.setBooster("xp", 1.5, expiry);
                } else if (item.getId().contains("points")) {
                    playerData.setBooster("points", 1.5, expiry);
                } else if (item.getId().contains("luck")) {
                    playerData.setBooster("luck", 1.25, expiry);
                } else if (item.getId().contains("all")) {
                    playerData.setBooster("xp", 1.5, expiry);
                    playerData.setBooster("points", 1.5, expiry);
                    playerData.setBooster("luck", 1.25, expiry);
                }
                player.sendMessage("§aBooster activé pour 1 heure!");
            }
            case CRATE -> {
                // Ouvrir une caisse avec loot aléatoire
                openCrate(player, item);
            }
        }
    }

    /**
     * Ouvre une caisse de loot
     */
    private void openCrate(Player player, GemShopItem crateItem) {
        Rarity minRarity = switch (crateItem.getId()) {
            case "crate_common" -> Rarity.RARE;
            case "crate_rare" -> Rarity.EPIC;
            case "crate_legendary" -> Rarity.LEGENDARY;
            default -> Rarity.COMMON;
        };
        
        // Générer un item avec la rareté minimum
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        int zoneLevel = playerData != null ? playerData.getHighestZone() : 1;
        
        ZombieZItem item = plugin.getItemManager().getGenerator()
            .generateWithMinRarity(zoneLevel, minRarity, 0);
        
        if (item != null) {
            ItemStack itemStack = item.toItemStack();
            
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(itemStack);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
            
            player.sendMessage("§6✦ Vous avez obtenu: " + item.getRarity().getColor() + item.getGeneratedName());
        }
    }

    /**
     * Obtient un item du shop de gemmes par son ID
     */
    public GemShopItem getGemShopItem(String id) {
        return gemShopItems.stream()
            .filter(i -> i.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Représente un shop de zone
     */
    @Getter
    public static class ZoneShop {
        private final int zoneId;
        private final String name;
        private final List<ShopItem> items;

        public ZoneShop(int zoneId, String name) {
            this.zoneId = zoneId;
            this.name = name;
            this.items = new ArrayList<>();
        }

        public void addItem(ShopItem item) {
            items.add(item);
        }
    }

    /**
     * Représente un item de shop
     */
    @Getter
    public static class ShopItem {
        private final String id;
        private final Material material;
        private final String name;
        private final String description;
        private final int price;
        private final Currency currency;

        public enum Currency {
            POINTS, GEMS
        }

        public ShopItem(String id, Material material, String name, String description, 
                       int price, Currency currency) {
            this.id = id;
            this.material = material;
            this.name = name;
            this.description = description;
            this.price = price;
            this.currency = currency;
        }

        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material);
            var meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(List.of(description));
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    /**
     * Représente un item du shop de gemmes
     */
    @Getter
    public static class GemShopItem {
        private final String id;
        private final Type type;
        private final String name;
        private final String description;
        private final int price;

        public enum Type {
            TITLE,        // Titre devant le nom
            DEATH_EFFECT, // Effet à la mort
            TRAIL,        // Particules de marche
            PET,          // Pet cosmétique
            BOOSTER,      // Booster temporaire
            CRATE         // Caisse de loot
        }

        public GemShopItem(String id, Type type, String name, String description, int price) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }
}
