package com.rinaorc.zombiez.items.affixes;

import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.StatType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registre de tous les affixes disponibles
 * Singleton thread-safe avec cache des filtres
 */
public class AffixRegistry {

    private static AffixRegistry instance;
    
    private final Map<String, Affix> affixes = new ConcurrentHashMap<>();
    private final Map<Affix.AffixType, List<Affix>> byType = new ConcurrentHashMap<>();
    private final Map<ItemType.ItemCategory, List<Affix>> byCategory = new ConcurrentHashMap<>();
    
    private boolean initialized = false;

    private AffixRegistry() {
        initialize();
    }

    public static AffixRegistry getInstance() {
        if (instance == null) {
            synchronized (AffixRegistry.class) {
                if (instance == null) {
                    instance = new AffixRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Initialise tous les affixes
     */
    private void initialize() {
        if (initialized) return;

        // ==================== PREFIXES OFFENSIFS ====================
        // Thème: Survie / Combat anti-zombie

        registerAffix(Affix.builder()
            .id("sharp")
            .displayName("Aiguisé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(100)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("keen")
            .displayName("Brutal")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(70)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("vicious")
            .displayName("Meurtrier")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{12, 18}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("deadly")
            .displayName("Tueur de Z")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{16, 22}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("annihilating")
            .displayName("Exterminateur")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(5)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{20, 28}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(9)
            .build());

        // ==================== PREFIXES VAMPIRIQUES ====================
        // Thème: Vol de vie / Régénération

        registerAffix(Affix.builder()
            .id("leeching")
            .displayName("Draineur")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(60)
            .stats(Map.of(StatType.LIFESTEAL, new double[]{1, 3}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("vampiric")
            .displayName("Vampirique")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.LIFESTEAL, new double[]{2, 4}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("sanguine")
            .displayName("Assoiffé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.LIFESTEAL, new double[]{4, 6}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== PREFIXES CRITIQUES ====================
        // Thème: Précision / Headshots

        registerAffix(Affix.builder()
            .id("precise")
            .displayName("Précis")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(80)
            .stats(Map.of(StatType.CRIT_CHANCE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("critical")
            .displayName("Chirurgical")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(
                StatType.CRIT_CHANCE, new double[]{3, 6},
                StatType.CRIT_DAMAGE, new double[]{5, 12}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("devastating")
            .displayName("Décapiteur")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(10)
            .stats(Map.of(
                StatType.CRIT_CHANCE, new double[]{6, 10},
                StatType.CRIT_DAMAGE, new double[]{15, 30}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== PREFIXES PÉNÉTRATION D'ARMURE ====================
        // Thème: Perforant / Anti-Tank

        registerAffix(Affix.builder()
            .id("piercing")
            .displayName("Perforant")
            .specialDescription("Ignore une partie de l'armure ennemie")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.ARMOR_PENETRATION, new double[]{5, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("sundering")
            .displayName("Brise-Armure")
            .specialDescription("Pénétration d'armure améliorée")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(18)
            .stats(Map.of(
                StatType.ARMOR_PENETRATION, new double[]{10, 18},
                StatType.DAMAGE_PERCENT, new double[]{3, 6}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_armor_breaking")
            .displayName("du Destructeur")
            .specialDescription("Détruit les défenses ennemies")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(
                StatType.ARMOR_PENETRATION, new double[]{8, 15},
                StatType.CRIT_DAMAGE, new double[]{5, 10}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(5)
            .build());

        // ==================== PREFIXES DÉFENSIFS ====================
        // Thème: Protection / Survie

        registerAffix(Affix.builder()
            .id("sturdy")
            .displayName("Solide")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(100)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("reinforced")
            .displayName("Blindé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(70)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("fortified")
            .displayName("Barricadé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{12, 18}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("ironclad")
            .displayName("Blindage Renforcé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{16, 22}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("impenetrable")
            .displayName("Bunker")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{20, 28}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(8)
            .build());

        // ==================== PREFIXES VITALITÉ ====================
        // Thème: Endurance / Survie

        registerAffix(Affix.builder()
            .id("healthy")
            .displayName("Tenace")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(90)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{2, 6}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("vital")
            .displayName("Résistant")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(45)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{4, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("titanic")
            .displayName("Indestructible")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(10)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{8, 14}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(8)
            .build());

        // ==================== SUFFIXES ÉLÉMENTAIRES FEU ====================
        // Thème: Purification / Crémation (très thématique zombie)

        registerAffix(Affix.builder()
            .id("of_embers")
            .displayName("des Braises")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(70)
            .stats(Map.of(StatType.FIRE_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_flames")
            .displayName("du Crématorium")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(StatType.FIRE_DAMAGE, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .specialEffect("ignite")
            .specialDescription("15% chance d'enflammer l'ennemi")
            .build());

        registerAffix(Affix.builder()
            .id("of_inferno")
            .displayName("du Purificateur")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.FIRE_DAMAGE, new double[]{20, 35}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(9)
            .specialEffect("inferno_burst")
            .specialDescription("Explosion de feu tous les 5 coups")
            .build());

        // ==================== SUFFIXES ÉLÉMENTAIRES GLACE ====================
        // Thème: Cryogénie / Conservation

        registerAffix(Affix.builder()
            .id("of_frost")
            .displayName("du Givre")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(70)
            .stats(Map.of(StatType.ICE_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_ice")
            .displayName("Cryogénique")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(StatType.ICE_DAMAGE, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .specialEffect("slow")
            .specialDescription("20% chance de ralentir l'ennemi")
            .build());

        registerAffix(Affix.builder()
            .id("of_blizzard")
            .displayName("de l'Hiver Nucléaire")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.ICE_DAMAGE, new double[]{20, 35}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .specialEffect("freeze")
            .specialDescription("10% chance de geler l'ennemi")
            .build());

        // ==================== SUFFIXES ÉLÉMENTAIRES FOUDRE ====================
        // Thème: Électricité / Générateur

        registerAffix(Affix.builder()
            .id("of_sparks")
            .displayName("Électrifié")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(70)
            .stats(Map.of(StatType.LIGHTNING_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_lightning")
            .displayName("du Générateur")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(StatType.LIGHTNING_DAMAGE, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .specialEffect("chain_lightning")
            .specialDescription("15% chance de toucher un ennemi proche")
            .build());

        registerAffix(Affix.builder()
            .id("of_storms")
            .displayName("de la Tempête")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.LIGHTNING_DAMAGE, new double[]{20, 35}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .specialEffect("thunderstrike")
            .specialDescription("Invoque un éclair tous les 10 coups")
            .build());

        // ==================== SUFFIXES POISON ====================
        // Thème: Infection / Contamination (très zombie)

        registerAffix(Affix.builder()
            .id("of_venom")
            .displayName("Contaminé")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.POISON_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(3)
            .specialEffect("poison")
            .specialDescription("Empoisonne l'ennemi (3s)")
            .build());

        registerAffix(Affix.builder()
            .id("of_plague")
            .displayName("de la Peste")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(10)
            .stats(Map.of(StatType.POISON_DAMAGE, new double[]{10, 20}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .specialEffect("plague_spread")
            .specialDescription("Le poison se propage aux ennemis proches")
            .build());

        // ==================== SUFFIXES RÉSISTANCES ARMURE ====================
        // Thème: Protection / Équipement spécialisé

        registerAffix(Affix.builder()
            .id("of_fire_ward")
            .displayName("Ignifugé")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.FIRE_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_frost_ward")
            .displayName("Isolé")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.ICE_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_grounding")
            .displayName("Anti-Choc")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.LIGHTNING_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_antidote")
            .displayName("Immunisé")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.POISON_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        // ==================== SUFFIXES UTILITAIRES ====================
        // Thème: Survie / Agilité

        registerAffix(Affix.builder()
            .id("of_haste")
            .displayName("du Sprinter")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(60)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_wind")
            .displayName("du Marathonien")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(20)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{5, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_fortune")
            .displayName("du Pilleur")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.LUCK, new double[]{5, 15}))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_wisdom")
            .displayName("du Vétéran")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.XP_BONUS, new double[]{5, 15}))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_prosperity")
            .displayName("du Chasseur")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.POINTS_BONUS, new double[]{5, 15}))
            .minZone(2)
            .build());

        // ==================== SUFFIXES VITESSE BOTTES ====================
        // Thème: Mobilité exclusive aux bottes
        // Calcul: Zone 50 Exalted Tier 5 = base × 3.5 × 1.5 × 1.3 ≈ base × 6.83
        // Objectif: Max ~100% vitesse pour Tier 5 Zone 50 Exalted

        registerAffix(Affix.builder()
            .id("of_swift_feet")
            .displayName("des Pieds Rapides")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(80)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{2, 4}))
            .allowedTypes(List.of(ItemType.BOOTS))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_agility")
            .displayName("de l'Agilité")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{4, 6}))
            .allowedTypes(List.of(ItemType.BOOTS))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_wind")
            .displayName("du Vent")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{5, 8}))
            .allowedTypes(List.of(ItemType.BOOTS))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_gale")
            .displayName("de la Bourrasque")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(12)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{7, 11}))
            .allowedTypes(List.of(ItemType.BOOTS))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_hurricane")
            .displayName("de l'Ouragan")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(4)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{10, 15}))
            .allowedTypes(List.of(ItemType.BOOTS))
            .minZone(9)
            .build());

        // ==================== SUFFIXES DÉFENSIFS SPÉCIAUX ====================
        // Thème: Auto-soins / Défense active

        registerAffix(Affix.builder()
            .id("of_regeneration")
            .displayName("Auto-Soins")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(StatType.HEALTH_REGEN, new double[]{0.5, 1.5}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_thorns")
            .displayName("Barbelé")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(40)
            .stats(Map.of(StatType.THORNS, new double[]{3, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_evasion")
            .displayName("Furtif")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.DODGE_CHANCE, new double[]{3, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_wall")
            .displayName("de la Forteresse")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(
                StatType.DAMAGE_REDUCTION, new double[]{5, 12},
                StatType.KNOCKBACK_RESISTANCE, new double[]{10, 25}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(7)
            .build());

        // ==================== AFFIXES BLOCAGE (BOUCLIERS/ARMURES) ====================
        // Thème: Parade / Blocage

        registerAffix(Affix.builder()
            .id("blocking")
            .displayName("Parade")
            .specialDescription("Chance de bloquer les attaques")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.BLOCK_CHANCE, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("shieldwall")
            .displayName("Mur de Boucliers")
            .specialDescription("Blocage amélioré")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(
                StatType.BLOCK_CHANCE, new double[]{8, 15},
                StatType.DAMAGE_REDUCTION, new double[]{3, 6}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_defender")
            .displayName("du Défenseur")
            .specialDescription("Maîtrise du blocage")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(
                StatType.BLOCK_CHANCE, new double[]{5, 12},
                StatType.ARMOR_PERCENT, new double[]{3, 8}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        // ==================== NOUVEAUX AFFIXES DÉFENSIFS (PATCH) ====================
        // Thème: Survie avancée / Protection spécialisée

        registerAffix(Affix.builder()
            .id("stalwart")
            .displayName("Inébranlable")
            .specialDescription("Résistance aux effets de contrôle")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(55)
            .stats(Map.of(
                StatType.KNOCKBACK_RESISTANCE, new double[]{15, 30},
                StatType.STUN_RESISTANCE, new double[]{10, 20}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("shielded")
            .displayName("Bouclier")
            .specialDescription("Absorbe une partie des dégâts")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(
                StatType.DAMAGE_REDUCTION, new double[]{4, 8},
                StatType.ARMOR_PERCENT, new double[]{5, 10}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("adaptive")
            .displayName("Adaptatif")
            .specialDescription("Résistances à tous les éléments")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(20)
            .stats(Map.of(
                StatType.FIRE_RESISTANCE, new double[]{6, 12},
                StatType.ICE_RESISTANCE, new double[]{6, 12},
                StatType.LIGHTNING_RESISTANCE, new double[]{6, 12},
                StatType.POISON_RESISTANCE, new double[]{6, 12}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_guardian")
            .displayName("du Gardien")
            .specialDescription("Protection totale améliorée")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(
                StatType.MAX_HEALTH, new double[]{3, 6},
                StatType.DAMAGE_REDUCTION, new double[]{3, 6},
                StatType.HEALTH_REGEN, new double[]{0.3, 0.8}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_resilience")
            .displayName("de Résilience")
            .specialDescription("Récupération après dégâts")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(18)
            .stats(Map.of(
                StatType.HEALTH_REGEN, new double[]{1.0, 2.0},
                StatType.DAMAGE_REDUCTION, new double[]{4, 8}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_survivor")
            .displayName("du Dernier Survivant")
            .specialDescription("Bonus défensif quand PV bas")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(6)
            .stats(Map.of(
                StatType.LOW_HEALTH_DAMAGE_REDUCTION, new double[]{15, 25},
                StatType.LOW_HEALTH_REGEN, new double[]{2, 4},
                StatType.CHEAT_DEATH_CHANCE, new double[]{1, 2}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(8)
            .build());

        // ==================== AFFIXES MOMENTUM ====================
        // Thème: Rage / Frénésie de combat

        registerAffix(Affix.builder()
            .id("frenzied")
            .displayName("Déchaîné")
            .specialDescription("Bonus de dégâts par kill streak")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(25)
            .stats(Map.of(StatType.STREAK_DAMAGE_BONUS, new double[]{0.5, 1.5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("relentless")
            .displayName("Machine à Tuer")
            .specialDescription("Bonus de dégâts massif par streak")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.STREAK_DAMAGE_BONUS, new double[]{0.8, 1.5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        registerAffix(Affix.builder()
            .id("of_momentum")
            .displayName("du Berserker")
            .specialDescription("Bonus de vitesse par combo")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.COMBO_SPEED_BONUS, new double[]{1, 3}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_storm")
            .displayName("de la Fureur")
            .specialDescription("Vitesse d'attaque augmentée en combo")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(
                StatType.COMBO_SPEED_BONUS, new double[]{3, 6},
                StatType.ATTACK_SPEED_PERCENT, new double[]{5, 12}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(6)
            .build());

        // ==================== AFFIXES FEVER ====================
        // Thème: Rage / Mode Enragé

        registerAffix(Affix.builder()
            .id("fevered")
            .displayName("Enragé")
            .specialDescription("Durée de Fever augmentée")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(20)
            .stats(Map.of(StatType.FEVER_DURATION_BONUS, new double[]{8, 18}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("blazing")
            .displayName("Possédé")
            .specialDescription("Bonus massif pendant Fever")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(12)
            .stats(Map.of(
                StatType.FEVER_DAMAGE_BONUS, new double[]{6, 12},
                StatType.FEVER_DURATION_BONUS, new double[]{5, 12}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("of_inferno_fever")
            .displayName("de l'Apocalypse")
            .specialDescription("Effets Fever amplifiés")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(5)
            .stats(Map.of(
                StatType.FEVER_DAMAGE_BONUS, new double[]{12, 20},
                StatType.FEVER_DURATION_BONUS, new double[]{12, 25}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(9)
            .build());

        // ==================== AFFIXES DE GROUPE ====================
        // Thème: Escouade / Groupe de survivants

        registerAffix(Affix.builder()
            .id("of_synergy")
            .displayName("de l'Escouade")
            .specialDescription("Bonus de proximité de groupe amélioré")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(25)
            .stats(Map.of(StatType.PARTY_BONUS, new double[]{10, 25}))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_pack")
            .displayName("de la Horde")
            .specialDescription("Bonus de dégâts par membre proche")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(
                StatType.PARTY_BONUS, new double[]{15, 35},
                StatType.PARTY_DAMAGE_SHARE, new double[]{5, 15}
            ))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("rallying")
            .displayName("Protecteur")
            .specialDescription("Soigne les alliés proches")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(10)
            .stats(Map.of(StatType.PARTY_HEAL_ON_KILL, new double[]{1, 3}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        // ==================== AFFIXES D'EXÉCUTION ====================
        // Thème: Achèvement / Coup de grâce

        registerAffix(Affix.builder()
            .id("executioner")
            .displayName("Exécuteur")
            .specialDescription("Dégâts bonus contre les cibles blessées")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.EXECUTE_DAMAGE, new double[]{5, 12}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("culling")
            .displayName("Faucheur")
            .specialDescription("Exécution instantanée des cibles très faibles")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(4)
            .stats(Map.of(
                StatType.EXECUTE_DAMAGE, new double[]{15, 25},
                StatType.EXECUTE_THRESHOLD, new double[]{3, 6}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== AFFIXES DE CHANCE ====================
        // Thème: Butin / Récupération

        registerAffix(Affix.builder()
            .id("of_fortune_loot")
            .displayName("du Récupérateur")
            .specialDescription("Chance de double loot")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(20)
            .stats(Map.of(StatType.DOUBLE_LOOT_CHANCE, new double[]{3, 8}))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_jackpot")
            .displayName("du Jackpot")
            .specialDescription("Chance de drop légendaire augmentée")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(3)
            .stats(Map.of(
                StatType.LEGENDARY_DROP_BONUS, new double[]{10, 25},
                StatType.DOUBLE_LOOT_CHANCE, new double[]{3, 6}
            ))
            .minZone(8)
            .build());

        // ==================== AFFIXES ARCS/ARBALÈTES (CADENCE DE TIR) ====================
        // Thème: Tireur / Précision tactique

        registerAffix(Affix.builder()
            .id("swift")
            .displayName("Tireur")
            .specialDescription("Cadence de tir améliorée")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(80)
            .stats(Map.of(StatType.DRAW_SPEED, new double[]{5, 12}))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("rapid")
            .displayName("Rafale")
            .specialDescription("Cadence de tir grandement améliorée")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(60)
            .stats(Map.of(StatType.DRAW_SPEED, new double[]{12, 20}))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("hasty")
            .displayName("Automatique")
            .specialDescription("Tir en rafale")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(
                StatType.DRAW_SPEED, new double[]{20, 30},
                StatType.CRIT_CHANCE, new double[]{3, 8}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("gatling")
            .displayName("Suppresseur")
            .specialDescription("Cadence de tir extrême")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(20)
            .stats(Map.of(
                StatType.DRAW_SPEED, new double[]{12, 18},
                StatType.DAMAGE_PERCENT, new double[]{3, 7}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("machinegun")
            .displayName("Nettoyeur")
            .specialDescription("Cadence de tir légendaire")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(6)
            .stats(Map.of(
                StatType.DRAW_SPEED, new double[]{18, 25},
                StatType.DAMAGE_PERCENT, new double[]{5, 10},
                StatType.CRIT_CHANCE, new double[]{3, 6}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(9)
            .build());

        // ==================== SUFFIXES SPÉCIAUX ARCS/ARBALÈTES ====================
        // Thème: Tir de précision / Survie à distance

        registerAffix(Affix.builder()
            .id("of_the_hunter")
            .displayName("du Survivant")
            .specialDescription("Bonus de précision et cadence")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(
                StatType.DRAW_SPEED, new double[]{8, 15},
                StatType.CRIT_CHANCE, new double[]{3, 8}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_sniper")
            .displayName("du Sniper")
            .specialDescription("Critiques dévastateurs")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(
                StatType.CRIT_CHANCE, new double[]{4, 8},
                StatType.CRIT_DAMAGE, new double[]{8, 18}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_marksman")
            .displayName("du Tireur d'Élite")
            .specialDescription("Maîtrise ultime de l'arc")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(
                StatType.DRAW_SPEED, new double[]{8, 14},
                StatType.CRIT_CHANCE, new double[]{5, 10},
                StatType.CRIT_DAMAGE, new double[]{12, 25}
            ))
            .allowedTypes(List.of(ItemType.BOW, ItemType.CROSSBOW))
            .minZone(8)
            .build());

        // ==================== AFFIXES SPÉCIAUX UNIQUES ====================
        // Thème: Survie ultime / Résurrection

        registerAffix(Affix.builder()
            .id("undying")
            .displayName("Survivant")
            .specialDescription("Chance d'ignorer un coup fatal")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(2)
            .stats(Map.of(StatType.CHEAT_DEATH_CHANCE, new double[]{1, 2}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(9)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_phoenix")
            .displayName("du Dernier Debout")
            .specialDescription("Renaître avec bonus après éviter la mort")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(1)
            .stats(Map.of(
                StatType.CHEAT_DEATH_CHANCE, new double[]{2, 3},
                StatType.REVIVE_DAMAGE_BOOST, new double[]{20, 40}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(10)
            .build());

        // Construire les caches
        buildCaches();
        initialized = true;
    }

    /**
     * Enregistre un affix
     */
    private void registerAffix(Affix affix) {
        affixes.put(affix.getId(), affix);
    }

    /**
     * Construit les caches de filtrage
     */
    private void buildCaches() {
        // Cache par type
        byType.put(Affix.AffixType.PREFIX, new ArrayList<>());
        byType.put(Affix.AffixType.SUFFIX, new ArrayList<>());
        
        for (Affix affix : affixes.values()) {
            byType.get(affix.getType()).add(affix);
        }

        // Cache par catégorie
        for (ItemType.ItemCategory cat : ItemType.ItemCategory.values()) {
            byCategory.put(cat, new ArrayList<>());
        }
        
        for (Affix affix : affixes.values()) {
            if (affix.getAllowedCategories() == null || affix.getAllowedCategories().isEmpty()) {
                // Affix universel - ajouter à toutes les catégories
                for (ItemType.ItemCategory cat : ItemType.ItemCategory.values()) {
                    byCategory.get(cat).add(affix);
                }
            } else {
                for (ItemType.ItemCategory cat : affix.getAllowedCategories()) {
                    byCategory.get(cat).add(affix);
                }
            }
        }
    }

    /**
     * Obtient un affix par son ID
     */
    public Affix getAffix(String id) {
        return affixes.get(id);
    }

    /**
     * Obtient tous les affixes
     */
    public Collection<Affix> getAllAffixes() {
        return Collections.unmodifiableCollection(affixes.values());
    }

    /**
     * Obtient les affixes par type (PREFIX ou SUFFIX)
     */
    public List<Affix> getAffixesByType(Affix.AffixType type) {
        return Collections.unmodifiableList(byType.getOrDefault(type, List.of()));
    }

    /**
     * Obtient les affixes applicables à un type d'item
     */
    public List<Affix> getAffixesForItemType(ItemType itemType, int zoneId) {
        // Récupérer les affixes par catégorie puis filtrer par type spécifique
        return byCategory.getOrDefault(itemType.getCategory(), List.of()).stream()
            .filter(a -> a.canDropInZone(zoneId))
            .filter(a -> a.canApplyTo(itemType))
            .collect(Collectors.toList());
    }

    /**
     * Tire un affix au sort pour un item
     */
    public Affix rollAffix(ItemType itemType, Affix.AffixType type, int zoneId, Set<String> excludeIds) {
        return rollAffix(itemType, type, zoneId, excludeIds, 5); // Par défaut, tous les tiers
    }

    /**
     * Tire un affix au sort pour un item avec restriction de tier maximum
     *
     * La rareté détermine le tier maximum accessible:
     * - COMMON: tier 1
     * - UNCOMMON: tier 2
     * - RARE: tier 3
     * - EPIC: tier 4
     * - LEGENDARY+: tier 5
     *
     * @param itemType Type d'item
     * @param type PREFIX ou SUFFIX
     * @param zoneId Zone pour filtrer par minZone
     * @param excludeIds IDs d'affixes à exclure
     * @param maxTier Tier maximum accessible (1-5)
     */
    public Affix rollAffix(ItemType itemType, Affix.AffixType type, int zoneId, Set<String> excludeIds, int maxTier) {
        List<Affix> candidates = getAffixesForItemType(itemType, zoneId).stream()
            .filter(a -> a.getType() == type)
            .filter(a -> !excludeIds.contains(a.getId()))
            .filter(a -> a.getTier().ordinal() < maxTier) // Filtrer par tier max
            .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        // Calcul du poids total avec bonus de zone pour les tiers élevés
        int totalWeight = 0;
        for (Affix affix : candidates) {
            // Zone bonus: les zones hautes favorisent les tiers élevés
            int zoneBonus = zoneId * affix.getTier().ordinal();
            totalWeight += affix.getWeight() + zoneBonus;
        }

        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;

        for (Affix affix : candidates) {
            int zoneBonus = zoneId * affix.getTier().ordinal();
            cumulative += affix.getWeight() + zoneBonus;
            if (roll < cumulative) {
                return affix;
            }
        }

        return candidates.get(0);
    }

    /**
     * Obtient le nombre total d'affixes
     */
    public int getAffixCount() {
        return affixes.size();
    }
}
