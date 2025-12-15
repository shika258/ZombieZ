package com.rinaorc.zombiez.mobs.food;

import com.rinaorc.zombiez.mobs.PassiveMobManager.PassiveMobType;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Registre de tous les items de nourriture custom
 * Définit les drops pour chaque type de mob passif
 */
public class FoodItemRegistry {

    private final Map<String, FoodItem> allItems;
    private final Map<PassiveMobType, List<FoodItem>> dropsByMob;
    private final Map<PassiveMobType, FoodItem> guaranteedDrops;
    private final Map<PassiveMobType, FoodItem> rareDrops;
    private final Map<PassiveMobType, FoodItem> legendaryDrops;

    public FoodItemRegistry() {
        this.allItems = new HashMap<>();
        this.dropsByMob = new HashMap<>();
        this.guaranteedDrops = new HashMap<>();
        this.rareDrops = new HashMap<>();
        this.legendaryDrops = new HashMap<>();

        registerAllItems();
        setupMobDrops();
    }

    /**
     * Enregistre tous les items de nourriture
     */
    private void registerAllItems() {
        // ===== COCHON (PIG) =====

        // Commun - Côte de Porc
        register(new FoodItem("pork_rib", "Côte de Porc", Material.PORKCHOP,
            FoodItem.FoodRarity.COMMON, 0, 4, 2.0f)
            .addLore("Une côte juteuse fraîchement préparée."));

        // Peu Commun - Jambon Fumé
        register(new FoodItem("smoked_ham", "Jambon Fumé", Material.COOKED_PORKCHOP,
            FoodItem.FoodRarity.UNCOMMON, 2, 6, 4.0f)
            .addEffect(PotionEffectType.REGENERATION, 60, 0)
            .addLore("Fumé à la perfection pendant des heures."));

        // Rare - Festin du Survivant
        register(new FoodItem("survivor_feast", "Festin du Survivant", Material.COOKED_PORKCHOP,
            FoodItem.FoodRarity.RARE, 6, 8, 6.0f)
            .addEffect(PotionEffectType.REGENERATION, 100, 1)
            .addEffect(PotionEffectType.STRENGTH, 200, 0)
            .addLore("Un repas complet qui redonne espoir.")
            .addLore("Les survivants en rêvent la nuit."));

        // Légendaire - Côte de Porc Parfaite
        register(new FoodItem("perfect_pork_rib", "Côte de Porc Parfaite", Material.COOKED_PORKCHOP,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 10.0f)
            .addEffect(PotionEffectType.HEALTH_BOOST, 200, 1) // +4 coeurs pendant 10s
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addEffect(PotionEffectType.RESISTANCE, 200, 0)
            .addLore("Une côte d'une qualité exceptionnelle!")
            .addLore("Augmente temporairement votre vitalité.")
            .addLore("§6+4 ❤ pendant 10 secondes"));


        // ===== POULET (CHICKEN) =====

        // Commun - Pilon de Poulet
        register(new FoodItem("chicken_leg", "Pilon de Poulet", Material.CHICKEN,
            FoodItem.FoodRarity.COMMON, 0, 3, 1.5f)
            .addLore("Un classique rapide à manger."));

        // Peu Commun - Poulet Grillé
        register(new FoodItem("grilled_chicken", "Poulet Grillé", Material.COOKED_CHICKEN,
            FoodItem.FoodRarity.UNCOMMON, 2, 5, 3.0f)
            .addEffect(PotionEffectType.SPEED, 100, 0)
            .addLore("Grillé sur feu de bois."));

        // Rare - Ailes Épicées du Phoenix
        register(new FoodItem("phoenix_wings", "Ailes Épicées du Phoenix", Material.COOKED_CHICKEN,
            FoodItem.FoodRarity.RARE, 4, 6, 5.0f)
            .addEffect(PotionEffectType.SPEED, 200, 1)
            .addEffect(PotionEffectType.FIRE_RESISTANCE, 300, 0)
            .addLore("Tellement épicées qu'elles brûlent!")
            .addLore("Confère une résistance au feu."));

        // Légendaire - Poulet Doré du Fermier
        register(new FoodItem("golden_farmer_chicken", "Poulet Doré du Fermier", Material.COOKED_CHICKEN,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 8.0f)
            .addEffect(PotionEffectType.ABSORPTION, 400, 1) // 4 coeurs dorés pendant 20s
            .addEffect(PotionEffectType.SPEED, 300, 1)
            .addEffect(PotionEffectType.REGENERATION, 200, 0)
            .addLore("Le secret bien gardé d'un vieux fermier.")
            .addLore("Procure une protection divine.")
            .addLore("§e+4 ❤ dorés pendant 20 secondes"));


        // ===== VACHE (COW) =====

        // Commun - Steak Cru
        register(new FoodItem("raw_steak", "Steak Cru", Material.BEEF,
            FoodItem.FoodRarity.COMMON, 0, 3, 1.5f)
            .addLore("Mieux vaut le cuire avant..."));

        // Peu Commun - Steak Juteux
        register(new FoodItem("juicy_steak", "Steak Juteux", Material.COOKED_BEEF,
            FoodItem.FoodRarity.UNCOMMON, 3, 7, 5.0f)
            .addEffect(PotionEffectType.REGENERATION, 80, 0)
            .addLore("Cuit à point, comme il se doit."));

        // Rare - Steak du Champion
        register(new FoodItem("champion_steak", "Steak du Champion", Material.COOKED_BEEF,
            FoodItem.FoodRarity.RARE, 6, 8, 7.0f)
            .addEffect(PotionEffectType.STRENGTH, 200, 0)
            .addEffect(PotionEffectType.REGENERATION, 100, 1)
            .addEffect(PotionEffectType.HASTE, 200, 0)
            .addLore("Le repas des vrais guerriers.")
            .addLore("Booste toutes vos capacités."));

        // Légendaire - Boeuf de Kobe Apocalyptique
        register(new FoodItem("apocalyptic_kobe", "Boeuf de Kobe Apocalyptique", Material.COOKED_BEEF,
            FoodItem.FoodRarity.LEGENDARY, 0, 12, 12.0f)
            .addEffect(PotionEffectType.HEALTH_BOOST, 300, 2) // +6 coeurs pendant 15s
            .addEffect(PotionEffectType.STRENGTH, 300, 1)
            .addEffect(PotionEffectType.RESISTANCE, 300, 0)
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addLore("Le boeuf le plus précieux du monde.")
            .addLore("Élevé dans des conditions parfaites.")
            .addLore("§c+6 ❤ pendant 15 secondes")
            .addLore("§c+Force II & Résistance"));


        // ===== MOUTON (SHEEP) =====

        // Commun - Côtelette d'Agneau
        register(new FoodItem("lamb_chop", "Côtelette d'Agneau", Material.MUTTON,
            FoodItem.FoodRarity.COMMON, 0, 3, 1.5f)
            .addLore("Simple mais nourrissant."));

        // Peu Commun - Gigot Rôti
        register(new FoodItem("roasted_leg", "Gigot Rôti", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.UNCOMMON, 2, 6, 4.0f)
            .addEffect(PotionEffectType.RESISTANCE, 100, 0)
            .addLore("Rôti lentement au four."));

        // Rare - Festin de l'Éleveur
        register(new FoodItem("shepherd_feast", "Festin de l'Éleveur", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.RARE, 5, 8, 6.0f)
            .addEffect(PotionEffectType.RESISTANCE, 200, 1)
            .addEffect(PotionEffectType.REGENERATION, 100, 0)
            .addEffect(PotionEffectType.NIGHT_VISION, 400, 0)
            .addLore("La recette secrète des bergers.")
            .addLore("Améliore la vision dans l'obscurité."));

        // Légendaire - Agneau des Dieux
        register(new FoodItem("divine_lamb", "Agneau des Dieux", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 10.0f)
            .addEffect(PotionEffectType.HEALTH_BOOST, 200, 1) // +4 coeurs pendant 10s
            .addEffect(PotionEffectType.RESISTANCE, 300, 1)
            .addEffect(PotionEffectType.ABSORPTION, 200, 0)
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addLore("Un met digne des divinités.")
            .addLore("Protection et vitalité ultimes.")
            .addLore("§b+4 ❤ & Résistance II pendant 10s"));


        // ===== ITEMS SPÉCIAUX (drops bonus possibles) =====

        // Oeuf Doré (drop rare de poulet)
        register(new FoodItem("golden_egg", "Oeuf Doré", Material.EGG,
            FoodItem.FoodRarity.RARE, 4, 4, 3.0f)
            .addEffect(PotionEffectType.LUCK, 600, 0) // 30 secondes de luck
            .addEffect(PotionEffectType.ABSORPTION, 100, 0)
            .addLore("Un oeuf aux propriétés magiques.")
            .addLore("Augmente votre chance de loot!"));

        // Lait Revigorant (drop de vache)
        register(new FoodItem("revitalizing_milk", "Lait Revigorant", Material.MILK_BUCKET,
            FoodItem.FoodRarity.RARE, 6, 0, 0f)
            .addEffect(PotionEffectType.REGENERATION, 200, 2)
            .addLore("Purifie le corps et l'esprit.")
            .addLore("Retire tous les effets négatifs."));

        // Cuir Comestible de Survie (emergency food)
        register(new FoodItem("survival_jerky", "Viande Séchée de Survie", Material.DRIED_KELP,
            FoodItem.FoodRarity.UNCOMMON, 1, 2, 0.5f)
            .addLore("Quand il n'y a plus rien d'autre...")
            .addLore("Mieux que rien!"));
    }

    /**
     * Configure les drops pour chaque type de mob
     */
    private void setupMobDrops() {
        // COCHON
        setupMobDropTable(PassiveMobType.PIG,
            Arrays.asList(
                allItems.get("pork_rib"),
                allItems.get("smoked_ham")
            ),
            allItems.get("pork_rib"),           // Garanti
            allItems.get("survivor_feast"),      // Rare
            allItems.get("perfect_pork_rib")     // Légendaire
        );

        // POULET
        setupMobDropTable(PassiveMobType.CHICKEN,
            Arrays.asList(
                allItems.get("chicken_leg"),
                allItems.get("grilled_chicken"),
                allItems.get("golden_egg")
            ),
            allItems.get("chicken_leg"),         // Garanti
            allItems.get("phoenix_wings"),       // Rare
            allItems.get("golden_farmer_chicken") // Légendaire
        );

        // VACHE
        setupMobDropTable(PassiveMobType.COW,
            Arrays.asList(
                allItems.get("raw_steak"),
                allItems.get("juicy_steak"),
                allItems.get("revitalizing_milk")
            ),
            allItems.get("juicy_steak"),         // Garanti
            allItems.get("champion_steak"),      // Rare
            allItems.get("apocalyptic_kobe")     // Légendaire
        );

        // MOUTON
        setupMobDropTable(PassiveMobType.SHEEP,
            Arrays.asList(
                allItems.get("lamb_chop"),
                allItems.get("roasted_leg")
            ),
            allItems.get("lamb_chop"),           // Garanti
            allItems.get("shepherd_feast"),      // Rare
            allItems.get("divine_lamb")          // Légendaire
        );
    }

    /**
     * Configure la table de drops pour un mob
     */
    private void setupMobDropTable(PassiveMobType mobType, List<FoodItem> possibleDrops,
                                   FoodItem guaranteed, FoodItem rare, FoodItem legendary) {
        dropsByMob.put(mobType, possibleDrops);
        guaranteedDrops.put(mobType, guaranteed);
        rareDrops.put(mobType, rare);
        legendaryDrops.put(mobType, legendary);
    }

    /**
     * Enregistre un item de nourriture
     */
    private void register(FoodItem item) {
        allItems.put(item.getId(), item);
    }

    /**
     * Obtient un item par son ID
     */
    public FoodItem getItem(String id) {
        return allItems.get(id);
    }

    /**
     * Obtient les drops possibles pour un type de mob
     */
    public List<FoodItem> getDropsForMob(PassiveMobType mobType) {
        return dropsByMob.getOrDefault(mobType, Collections.emptyList());
    }

    /**
     * Obtient le drop garanti pour un type de mob
     */
    public FoodItem getGuaranteedDrop(PassiveMobType mobType) {
        return guaranteedDrops.get(mobType);
    }

    /**
     * Obtient le drop rare pour un type de mob
     */
    public FoodItem getRareDrop(PassiveMobType mobType) {
        return rareDrops.get(mobType);
    }

    /**
     * Obtient le drop légendaire pour un type de mob
     */
    public FoodItem getLegendaryDrop(PassiveMobType mobType) {
        return legendaryDrops.get(mobType);
    }

    /**
     * Obtient tous les items de nourriture enregistrés
     */
    public Collection<FoodItem> getAllItems() {
        return allItems.values();
    }
}
