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
    private final Map<PassiveMobType, FoodItem> epicDrops;
    private final Map<PassiveMobType, FoodItem> legendaryDrops;

    public FoodItemRegistry() {
        this.allItems = new HashMap<>();
        this.dropsByMob = new HashMap<>();
        this.guaranteedDrops = new HashMap<>();
        this.rareDrops = new HashMap<>();
        this.epicDrops = new HashMap<>();
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


        // ===== LAPIN (RABBIT) =====

        // Commun - Cuisse de Lapin
        register(new FoodItem("rabbit_leg", "Cuisse de Lapin", Material.RABBIT,
            FoodItem.FoodRarity.COMMON, 0, 3, 1.5f)
            .addLore("Un petit morceau de viande tendre."));

        // Peu Commun - Lapin Rôti
        register(new FoodItem("roasted_rabbit", "Lapin Rôti", Material.COOKED_RABBIT,
            FoodItem.FoodRarity.UNCOMMON, 2, 5, 3.5f)
            .addEffect(PotionEffectType.SPEED, 120, 0)
            .addEffect(PotionEffectType.JUMP_BOOST, 120, 0)
            .addLore("Rôti à la perfection.")
            .addLore("Vous vous sentez plus léger!"));

        // Rare - Ragoût du Chasseur
        register(new FoodItem("hunter_stew", "Ragoût du Chasseur", Material.RABBIT_STEW,
            FoodItem.FoodRarity.RARE, 5, 10, 8.0f)
            .addEffect(PotionEffectType.SPEED, 200, 1)
            .addEffect(PotionEffectType.JUMP_BOOST, 200, 1)
            .addEffect(PotionEffectType.REGENERATION, 100, 0)
            .addLore("La recette secrète des chasseurs.")
            .addLore("Un ragoût qui réchauffe l'âme."));

        // Épique - Patte de Lapin Enchantée
        register(new FoodItem("enchanted_rabbit_foot", "Patte de Lapin Enchantée", Material.RABBIT_FOOT,
            FoodItem.FoodRarity.EPIC, 3, 4, 4.0f)
            .addEffect(PotionEffectType.LUCK, 1200, 1) // 1 minute de luck II
            .addEffect(PotionEffectType.SPEED, 600, 1)
            .addEffect(PotionEffectType.JUMP_BOOST, 600, 2)
            .addLore("Une patte porte-bonheur magique!")
            .addLore("§e+Luck II pendant 60 secondes")
            .addLore("§a+Vitesse & Saut améliorés"));

        // Légendaire - Lapin Doré du Destin
        register(new FoodItem("golden_destiny_rabbit", "Lapin Doré du Destin", Material.COOKED_RABBIT,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 10.0f)
            .addEffect(PotionEffectType.LUCK, 2400, 2) // 2 minutes de luck III
            .addEffect(PotionEffectType.SPEED, 400, 2)
            .addEffect(PotionEffectType.JUMP_BOOST, 400, 3)
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addLore("Un lapin béni par les dieux du hasard.")
            .addLore("§6Chance maximale de loot!")
            .addLore("§e+Luck III pendant 2 minutes"));


        // ===== CHEVAL (HORSE) =====

        // Commun - Viande de Cheval
        register(new FoodItem("horse_meat", "Viande de Cheval", Material.BEEF,
            FoodItem.FoodRarity.COMMON, 0, 4, 2.0f)
            .addLore("Une viande dense et nourrissante."));

        // Peu Commun - Steak de Cheval Épicé
        register(new FoodItem("spiced_horse_steak", "Steak de Cheval Épicé", Material.COOKED_BEEF,
            FoodItem.FoodRarity.UNCOMMON, 3, 7, 5.0f)
            .addEffect(PotionEffectType.SPEED, 200, 1)
            .addEffect(PotionEffectType.STRENGTH, 100, 0)
            .addLore("Épicé avec des herbes sauvages.")
            .addLore("Booste votre énergie!"));

        // Rare - Festin du Cavalier
        register(new FoodItem("rider_feast", "Festin du Cavalier", Material.COOKED_BEEF,
            FoodItem.FoodRarity.RARE, 6, 8, 7.0f)
            .addEffect(PotionEffectType.SPEED, 400, 2)
            .addEffect(PotionEffectType.STRENGTH, 200, 1)
            .addEffect(PotionEffectType.REGENERATION, 100, 0)
            .addLore("Le repas des cavaliers d'élite.")
            .addLore("§b+Vitesse II & Force I"));

        // Épique - Coeur de Destrier
        register(new FoodItem("war_horse_heart", "Coeur de Destrier", Material.COOKED_BEEF,
            FoodItem.FoodRarity.EPIC, 8, 10, 8.0f)
            .addEffect(PotionEffectType.SPEED, 600, 2)
            .addEffect(PotionEffectType.STRENGTH, 400, 1)
            .addEffect(PotionEffectType.RESISTANCE, 300, 0)
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addLore("Le coeur d'un noble destrier de guerre.")
            .addLore("§5Pouvoir du guerrier à cheval")
            .addLore("§c+Force I & Résistance"));

        // Légendaire - Essence du Cheval de Feu
        register(new FoodItem("fire_horse_essence", "Essence du Cheval de Feu", Material.COOKED_BEEF,
            FoodItem.FoodRarity.LEGENDARY, 0, 12, 12.0f)
            .addEffect(PotionEffectType.SPEED, 800, 3) // Speed IV pendant 40s
            .addEffect(PotionEffectType.STRENGTH, 600, 2)
            .addEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0)
            .addEffect(PotionEffectType.HEALTH_BOOST, 400, 1)
            .addEffect(PotionEffectType.REGENERATION, 200, 2)
            .addLore("L'essence pure d'un cheval légendaire!")
            .addLore("§6Vitesse fulgurante comme le feu!")
            .addLore("§c+Vitesse IV & Force II"));


        // ===== CHÈVRE (GOAT) =====

        // Commun - Côte de Chèvre
        register(new FoodItem("goat_rib", "Côte de Chèvre", Material.MUTTON,
            FoodItem.FoodRarity.COMMON, 0, 3, 1.5f)
            .addLore("Viande maigre mais robuste."));

        // Peu Commun - Chèvre Grillée des Montagnes
        register(new FoodItem("mountain_goat_grill", "Chèvre Grillée des Montagnes", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.UNCOMMON, 2, 6, 4.0f)
            .addEffect(PotionEffectType.RESISTANCE, 150, 0)
            .addEffect(PotionEffectType.JUMP_BOOST, 150, 1)
            .addLore("Grillée sur les hauteurs.")
            .addLore("Confère l'agilité des montagnes."));

        // Rare - Ragoût de l'Alpiniste
        register(new FoodItem("mountaineer_stew", "Ragoût de l'Alpiniste", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.RARE, 5, 8, 6.0f)
            .addEffect(PotionEffectType.RESISTANCE, 300, 1)
            .addEffect(PotionEffectType.JUMP_BOOST, 300, 2)
            .addEffect(PotionEffectType.SLOW_FALLING, 300, 0)
            .addEffect(PotionEffectType.REGENERATION, 100, 0)
            .addLore("La force des grimpeurs de montagne.")
            .addLore("§b+Résistance I & Chute lente"));

        // Épique - Fromage de Chèvre Vieilli
        register(new FoodItem("aged_goat_cheese", "Fromage de Chèvre Vieilli", Material.PUMPKIN_PIE,
            FoodItem.FoodRarity.EPIC, 4, 6, 6.0f)
            .addEffect(PotionEffectType.RESISTANCE, 600, 1)
            .addEffect(PotionEffectType.REGENERATION, 400, 1)
            .addEffect(PotionEffectType.SATURATION, 100, 0)
            .addLore("Vieilli pendant des années dans une grotte.")
            .addLore("§5Fromage artisanal exceptionnel")
            .addLore("§a+Régénération prolongée"));

        // Légendaire - Esprit de la Chèvre Ancestrale
        register(new FoodItem("ancestral_goat_spirit", "Esprit de la Chèvre Ancestrale", Material.COOKED_MUTTON,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 10.0f)
            .addEffect(PotionEffectType.RESISTANCE, 600, 2) // Résistance III
            .addEffect(PotionEffectType.JUMP_BOOST, 600, 4) // Jump V
            .addEffect(PotionEffectType.SLOW_FALLING, 600, 0)
            .addEffect(PotionEffectType.HEALTH_BOOST, 400, 1)
            .addEffect(PotionEffectType.REGENERATION, 300, 1)
            .addLore("L'esprit des chèvres anciennes vit en vous!")
            .addLore("§6Maître des montagnes!")
            .addLore("§b+Résistance III & Saut V"));


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

        // Pomme Dorée Artisanale (drop épique universel)
        register(new FoodItem("artisan_golden_apple", "Pomme Dorée Artisanale", Material.GOLDEN_APPLE,
            FoodItem.FoodRarity.EPIC, 0, 8, 8.0f)
            .addEffect(PotionEffectType.ABSORPTION, 600, 1)
            .addEffect(PotionEffectType.REGENERATION, 200, 1)
            .addEffect(PotionEffectType.RESISTANCE, 200, 0)
            .addLore("Une pomme dorée de qualité artisanale.")
            .addLore("§e+4 coeurs dorés pendant 30s"));

        // Miel Énergisant (drop rare)
        register(new FoodItem("energizing_honey", "Miel Énergisant", Material.HONEY_BOTTLE,
            FoodItem.FoodRarity.RARE, 3, 6, 5.0f)
            .addEffect(PotionEffectType.SPEED, 300, 1)
            .addEffect(PotionEffectType.HASTE, 300, 1)
            .addEffect(PotionEffectType.REGENERATION, 100, 0)
            .addLore("Du miel aux propriétés magiques.")
            .addLore("§a+Vitesse & Hâte pendant 15s"));

        // Baies des Bois Enchantées (drop rare)
        register(new FoodItem("enchanted_berries", "Baies des Bois Enchantées", Material.SWEET_BERRIES,
            FoodItem.FoodRarity.RARE, 2, 4, 3.0f)
            .addEffect(PotionEffectType.NIGHT_VISION, 600, 0)
            .addEffect(PotionEffectType.REGENERATION, 200, 0)
            .addEffect(PotionEffectType.SPEED, 200, 0)
            .addLore("Des baies cueillies sous la pleine lune.")
            .addLore("§9+Vision nocturne pendant 30s"));

        // ===== NOURRITURE TROUVÉE SUR LES ZOMBIES (drops des zombies) =====
        // Ces items représentent ce que les zombies portaient avant leur transformation

        // 1. Banane - Common, simple et efficace
        register(new FoodItem("banana", "Banane", Material.YELLOW_DYE,
            FoodItem.FoodRarity.COMMON, 2, 4, 2.0f)
            .addLore("§7Une banane bien mûre.")
            .addLore("§aÉnergie rapide!"));

        // 2. Boîte de Conserve - Common, nourrissant
        register(new FoodItem("canned_food", "Boîte de Conserve", Material.IRON_NUGGET,
            FoodItem.FoodRarity.COMMON, 2, 5, 3.0f)
            .addLore("§7Contenu: Haricots.")
            .addLore("§7Se conserve indéfiniment."));

        // 3. Barre Énergétique - Common, boost de vitesse
        register(new FoodItem("energy_bar", "Barre Énergétique", Material.COOKIE,
            FoodItem.FoodRarity.COMMON, 1, 3, 1.5f)
            .addEffect(PotionEffectType.SPEED, 100, 0) // 5 sec speed
            .addLore("§7Chocolat et céréales.")
            .addLore("§a+Vitesse 5s"));

        // 4. Ration Militaire - Uncommon, très nourrissant
        register(new FoodItem("military_ration", "Ration Militaire", Material.BROWN_DYE,
            FoodItem.FoodRarity.UNCOMMON, 4, 8, 6.0f)
            .addEffect(PotionEffectType.REGENERATION, 60, 0) // 3 sec regen
            .addLore("§7Pack de survie complet.")
            .addLore("§7Trouvé sur un soldat infecté.")
            .addLore("§a+Régénération 3s"));

        // 5. Eau Purifiée - Uncommon, régénération
        register(new FoodItem("purified_water", "Eau Purifiée", Material.POTION,
            FoodItem.FoodRarity.UNCOMMON, 2, 2, 1.0f)
            .addEffect(PotionEffectType.REGENERATION, 100, 0) // 5 sec regen
            .addEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0) // 10 sec fire res
            .addLore("§7Une bouteille d'eau filtrée.")
            .addLore("§a+Régénération 5s")
            .addLore("§6+Résistance au feu 10s"));

        // 6. Sandwich Emballé - Uncommon, équilibré
        register(new FoodItem("wrapped_sandwich", "Sandwich Emballé", Material.BREAD,
            FoodItem.FoodRarity.UNCOMMON, 3, 6, 4.0f)
            .addEffect(PotionEffectType.SATURATION, 40, 0) // 2 sec saturation
            .addLore("§7Encore frais sous vide.")
            .addLore("§7Jambon-fromage, un classique.")
            .addLore("§a+Saturation"));

        // 7. Kit de Premiers Soins - Rare, healing
        register(new FoodItem("first_aid_kit", "Kit de Premiers Soins", Material.RED_DYE,
            FoodItem.FoodRarity.RARE, 6, 4, 2.0f)
            .addEffect(PotionEffectType.INSTANT_HEALTH, 1, 1) // Heal instant
            .addEffect(PotionEffectType.REGENERATION, 200, 1) // 10 sec regen II
            .addLore("§7Bandages et antiseptique.")
            .addLore("§c+Soin instantané")
            .addLore("§a+Régénération II 10s"));

        // 8. Boisson Énergisante - Rare, multi-buff
        register(new FoodItem("energy_drink", "Boisson Énergisante", Material.HONEY_BOTTLE,
            FoodItem.FoodRarity.RARE, 3, 4, 3.0f)
            .addEffect(PotionEffectType.SPEED, 300, 1) // 15 sec speed II
            .addEffect(PotionEffectType.HASTE, 300, 0) // 15 sec haste
            .addEffect(PotionEffectType.JUMP_BOOST, 300, 1) // 15 sec jump II
            .addLore("§7Caféine et taurine concentrées.")
            .addLore("§b+Vitesse II 15s")
            .addLore("§e+Hâte & Saut 15s"));

        // 9. Stim-Pack Médical - Epic, puissant healing
        register(new FoodItem("stim_pack", "Stim-Pack Médical", Material.GLOW_INK_SAC,
            FoodItem.FoodRarity.EPIC, 8, 6, 4.0f)
            .addEffect(PotionEffectType.INSTANT_HEALTH, 1, 2) // Gros heal
            .addEffect(PotionEffectType.REGENERATION, 400, 2) // 20 sec regen III
            .addEffect(PotionEffectType.RESISTANCE, 200, 0) // 10 sec resistance
            .addEffect(PotionEffectType.ABSORPTION, 400, 1) // 20 sec absorption II
            .addLore("§5Injection de nano-médicaments.")
            .addLore("§c+Soin puissant instantané")
            .addLore("§a+Régénération III 20s")
            .addLore("§e+4 ❤ dorés 20s"));

        // 10. Seringue d'Adrénaline - Legendary, tous les buffs!
        register(new FoodItem("adrenaline_syringe", "Seringue d'Adrénaline", Material.END_ROD,
            FoodItem.FoodRarity.LEGENDARY, 0, 10, 8.0f)
            .addEffect(PotionEffectType.INSTANT_HEALTH, 1, 2) // Full heal
            .addEffect(PotionEffectType.REGENERATION, 600, 2) // 30 sec regen III
            .addEffect(PotionEffectType.SPEED, 600, 2) // 30 sec speed III
            .addEffect(PotionEffectType.STRENGTH, 600, 1) // 30 sec strength II
            .addEffect(PotionEffectType.RESISTANCE, 600, 1) // 30 sec resistance II
            .addEffect(PotionEffectType.ABSORPTION, 600, 2) // 30 sec absorption III
            .addEffect(PotionEffectType.HEALTH_BOOST, 600, 1) // 30 sec +4 hearts
            .addLore("§6✦ INJECTION D'ADRÉNALINE PURE ✦")
            .addLore("§7Réservée aux situations critiques.")
            .addLore("")
            .addLore("§c+Soin complet instantané")
            .addLore("§a+Régénération III 30s")
            .addLore("§b+Vitesse III & Force II 30s")
            .addLore("§e+Résistance II 30s")
            .addLore("§d+6 ❤ dorés + 4 ❤ bonus 30s"));
    }

    /**
     * Configure les drops pour chaque type de mob
     */
    private void setupMobDrops() {
        // COCHON
        setupMobDropTableFull(PassiveMobType.PIG,
            Arrays.asList(
                allItems.get("pork_rib"),
                allItems.get("smoked_ham")
            ),
            allItems.get("pork_rib"),           // Garanti
            allItems.get("survivor_feast"),      // Rare
            allItems.get("artisan_golden_apple"), // Épique (bonus universel)
            allItems.get("perfect_pork_rib")     // Légendaire
        );

        // POULET
        setupMobDropTableFull(PassiveMobType.CHICKEN,
            Arrays.asList(
                allItems.get("chicken_leg"),
                allItems.get("grilled_chicken"),
                allItems.get("golden_egg")
            ),
            allItems.get("chicken_leg"),         // Garanti
            allItems.get("phoenix_wings"),       // Rare
            allItems.get("artisan_golden_apple"), // Épique (bonus universel)
            allItems.get("golden_farmer_chicken") // Légendaire
        );

        // VACHE
        setupMobDropTableFull(PassiveMobType.COW,
            Arrays.asList(
                allItems.get("raw_steak"),
                allItems.get("juicy_steak"),
                allItems.get("revitalizing_milk")
            ),
            allItems.get("juicy_steak"),         // Garanti
            allItems.get("champion_steak"),      // Rare
            allItems.get("artisan_golden_apple"), // Épique (bonus universel)
            allItems.get("apocalyptic_kobe")     // Légendaire
        );

        // MOUTON
        setupMobDropTableFull(PassiveMobType.SHEEP,
            Arrays.asList(
                allItems.get("lamb_chop"),
                allItems.get("roasted_leg")
            ),
            allItems.get("lamb_chop"),           // Garanti
            allItems.get("shepherd_feast"),      // Rare
            allItems.get("artisan_golden_apple"), // Épique (bonus universel)
            allItems.get("divine_lamb")          // Légendaire
        );

        // LAPIN (nouveau)
        setupMobDropTableFull(PassiveMobType.RABBIT,
            Arrays.asList(
                allItems.get("rabbit_leg"),
                allItems.get("roasted_rabbit"),
                allItems.get("hunter_stew")
            ),
            allItems.get("rabbit_leg"),          // Garanti
            allItems.get("hunter_stew"),         // Rare
            allItems.get("enchanted_rabbit_foot"), // Épique
            allItems.get("golden_destiny_rabbit") // Légendaire
        );

        // CHEVAL (nouveau)
        setupMobDropTableFull(PassiveMobType.HORSE,
            Arrays.asList(
                allItems.get("horse_meat"),
                allItems.get("spiced_horse_steak"),
                allItems.get("rider_feast")
            ),
            allItems.get("horse_meat"),          // Garanti
            allItems.get("rider_feast"),         // Rare
            allItems.get("war_horse_heart"),     // Épique
            allItems.get("fire_horse_essence")   // Légendaire
        );

        // CHÈVRE (nouveau)
        setupMobDropTableFull(PassiveMobType.GOAT,
            Arrays.asList(
                allItems.get("goat_rib"),
                allItems.get("mountain_goat_grill"),
                allItems.get("mountaineer_stew")
            ),
            allItems.get("goat_rib"),            // Garanti
            allItems.get("mountaineer_stew"),    // Rare
            allItems.get("aged_goat_cheese"),    // Épique
            allItems.get("ancestral_goat_spirit") // Légendaire
        );
    }

    /**
     * Configure la table de drops complète pour un mob (avec épique)
     */
    private void setupMobDropTableFull(PassiveMobType mobType, List<FoodItem> possibleDrops,
                                       FoodItem guaranteed, FoodItem rare, FoodItem epic, FoodItem legendary) {
        dropsByMob.put(mobType, possibleDrops);
        guaranteedDrops.put(mobType, guaranteed);
        rareDrops.put(mobType, rare);
        epicDrops.put(mobType, epic);
        legendaryDrops.put(mobType, legendary);
    }

    /**
     * Configure la table de drops pour un mob (rétrocompatibilité)
     */
    private void setupMobDropTable(PassiveMobType mobType, List<FoodItem> possibleDrops,
                                   FoodItem guaranteed, FoodItem rare, FoodItem legendary) {
        setupMobDropTableFull(mobType, possibleDrops, guaranteed, rare, null, legendary);
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
     * Obtient le drop épique pour un type de mob
     */
    public FoodItem getEpicDrop(PassiveMobType mobType) {
        return epicDrops.get(mobType);
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

    /**
     * Obtient une nourriture de zombie aléatoire
     * Pondération basée sur la rareté: Common > Uncommon > Rare > Epic > Legendary
     */
    public FoodItem getRandomZombieFood() {
        // Liste des nourritures de zombie avec leurs poids (total = 100%)
        // Common (3): 20% chacun = 60%
        // Uncommon (3): 10% chacun = 30%
        // Rare (2): 3% chacun = 6%
        // Epic (1): 3% = 3%
        // Legendary (1): 1% = 1%
        String[] zombieFoods = {
            "banana", "canned_food", "energy_bar",           // Common (60%)
            "military_ration", "purified_water", "wrapped_sandwich", // Uncommon (30%)
            "first_aid_kit", "energy_drink",                 // Rare (6%)
            "stim_pack",                                     // Epic (3%)
            "adrenaline_syringe"                             // Legendary (1%)
        };
        double[] weights = {
            0.20, 0.20, 0.20,    // Common: 60% total
            0.10, 0.10, 0.10,    // Uncommon: 30% total
            0.03, 0.03,          // Rare: 6% total
            0.03,                // Epic: 3%
            0.01                 // Legendary: 1%
        };

        double roll = Math.random();
        double cumulative = 0;

        for (int i = 0; i < zombieFoods.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return allItems.get(zombieFoods[i]);
            }
        }

        // Fallback
        return allItems.get("banana");
    }

    /**
     * Vérifie si un item est une nourriture de zombie
     */
    public boolean isZombieFood(String id) {
        return id.equals("banana") || id.equals("canned_food") || id.equals("energy_bar") ||
               id.equals("military_ration") || id.equals("purified_water") || id.equals("wrapped_sandwich") ||
               id.equals("first_aid_kit") || id.equals("energy_drink") ||
               id.equals("stim_pack") || id.equals("adrenaline_syringe");
    }
}
