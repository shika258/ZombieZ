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
        
        registerAffix(Affix.builder()
            .id("sharp")
            .displayName("Tranchant")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(100)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("keen")
            .displayName("Affûté")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(70)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("vicious")
            .displayName("Vicieux")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{15, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("deadly")
            .displayName("Mortel")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{25, 40}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("annihilating")
            .displayName("Annihilant")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(5)
            .stats(Map.of(StatType.DAMAGE_PERCENT, new double[]{40, 60}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(9)
            .build());

        // ==================== PREFIXES VAMPIRIQUES ====================

        registerAffix(Affix.builder()
            .id("leeching")
            .displayName("Sangsue")
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
            .stats(Map.of(StatType.LIFESTEAL, new double[]{4, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("sanguine")
            .displayName("Sanguinaire")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.LIFESTEAL, new double[]{10, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== PREFIXES CRITIQUES ====================

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
            .displayName("Critique")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(
                StatType.CRIT_CHANCE, new double[]{5, 10},
                StatType.CRIT_DAMAGE, new double[]{10, 25}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("devastating")
            .displayName("Dévastateur")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(10)
            .stats(Map.of(
                StatType.CRIT_CHANCE, new double[]{12, 20},
                StatType.CRIT_DAMAGE, new double[]{40, 75}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== PREFIXES DÉFENSIFS ====================

        registerAffix(Affix.builder()
            .id("sturdy")
            .displayName("Robuste")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(100)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("reinforced")
            .displayName("Renforcé")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(70)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{8, 15}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("fortified")
            .displayName("Fortifié")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(40)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{15, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("impenetrable")
            .displayName("Impénétrable")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.ARMOR_PERCENT, new double[]{30, 45}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(8)
            .build());

        // ==================== PREFIXES VITALITÉ ====================

        registerAffix(Affix.builder()
            .id("healthy")
            .displayName("Sain")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(90)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{2, 6}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("vital")
            .displayName("Vital")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(45)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{10, 20}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("titanic")
            .displayName("Titanique")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(10)
            .stats(Map.of(StatType.MAX_HEALTH, new double[]{25, 40}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(8)
            .build());

        // ==================== SUFFIXES ÉLÉMENTAIRES FEU ====================

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
            .displayName("de Flammes")
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
            .displayName("de l'Inferno")
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

        registerAffix(Affix.builder()
            .id("of_frost")
            .displayName("de Givre")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(70)
            .stats(Map.of(StatType.ICE_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_ice")
            .displayName("de Glace")
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
            .displayName("du Blizzard")
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

        registerAffix(Affix.builder()
            .id("of_sparks")
            .displayName("d'Étincelles")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_1)
            .weight(70)
            .stats(Map.of(StatType.LIGHTNING_DAMAGE, new double[]{2, 5}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(1)
            .build());

        registerAffix(Affix.builder()
            .id("of_lightning")
            .displayName("de Foudre")
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
            .displayName("des Tempêtes")
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

        registerAffix(Affix.builder()
            .id("of_venom")
            .displayName("de Venin")
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

        registerAffix(Affix.builder()
            .id("of_fire_ward")
            .displayName("de Protection Feu")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.FIRE_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_frost_ward")
            .displayName("de Protection Glace")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.ICE_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_grounding")
            .displayName("de Mise à Terre")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.LIGHTNING_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_antidote")
            .displayName("d'Antidote")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.POISON_RESISTANCE, new double[]{10, 25}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        // ==================== SUFFIXES UTILITAIRES ====================

        registerAffix(Affix.builder()
            .id("of_haste")
            .displayName("de Célérité")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(60)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_wind")
            .displayName("du Vent")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(20)
            .stats(Map.of(StatType.MOVEMENT_SPEED, new double[]{10, 18}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(6)
            .build());

        registerAffix(Affix.builder()
            .id("of_fortune")
            .displayName("de Fortune")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.LUCK, new double[]{5, 15}))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_wisdom")
            .displayName("de Sagesse")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.XP_BONUS, new double[]{5, 15}))
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_prosperity")
            .displayName("de Prospérité")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.POINTS_BONUS, new double[]{5, 15}))
            .minZone(2)
            .build());

        // ==================== SUFFIXES DÉFENSIFS SPÉCIAUX ====================

        registerAffix(Affix.builder()
            .id("of_regeneration")
            .displayName("de Régénération")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(35)
            .stats(Map.of(StatType.HEALTH_REGEN, new double[]{0.5, 1.5}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_thorns")
            .displayName("d'Épines")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(40)
            .stats(Map.of(StatType.THORNS, new double[]{3, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(3)
            .build());

        registerAffix(Affix.builder()
            .id("of_evasion")
            .displayName("d'Évasion")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.DODGE_CHANCE, new double[]{3, 10}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_wall")
            .displayName("du Rempart")
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

        // ==================== NOUVEAUX AFFIXES MOMENTUM ====================
        // Synergies avec le nouveau système de combo/streak

        registerAffix(Affix.builder()
            .id("frenzied")
            .displayName("Frénétique")
            .specialDescription("Bonus de dégâts par kill streak")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(25)
            .stats(Map.of(StatType.STREAK_DAMAGE_BONUS, new double[]{0.5, 1.5})) // +0.5-1.5% par kill streak
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("relentless")
            .displayName("Implacable")
            .specialDescription("Bonus de dégâts massif par streak")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(8)
            .stats(Map.of(StatType.STREAK_DAMAGE_BONUS, new double[]{2.0, 4.0}))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        registerAffix(Affix.builder()
            .id("of_momentum")
            .displayName("de l'Élan")
            .specialDescription("Bonus de vitesse par combo")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_2)
            .weight(50)
            .stats(Map.of(StatType.COMBO_SPEED_BONUS, new double[]{1, 3})) // +1-3% vitesse par combo
            .minZone(2)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_storm")
            .displayName("de la Tempête")
            .specialDescription("Vitesse d'attaque augmentée en combo")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(15)
            .stats(Map.of(
                StatType.COMBO_SPEED_BONUS, new double[]{3, 6},
                StatType.ATTACK_SPEED, new double[]{5, 12}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(6)
            .build());

        // ==================== AFFIXES FEVER ====================

        registerAffix(Affix.builder()
            .id("fevered")
            .displayName("Fiévreux")
            .specialDescription("Durée de Fever augmentée")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(20)
            .stats(Map.of(StatType.FEVER_DURATION_BONUS, new double[]{20, 50})) // +20-50% durée fever
            .minZone(5)
            .build());

        registerAffix(Affix.builder()
            .id("blazing")
            .displayName("Embrasé")
            .specialDescription("Bonus massif pendant Fever")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(12)
            .stats(Map.of(
                StatType.FEVER_DAMAGE_BONUS, new double[]{15, 30},
                StatType.FEVER_DURATION_BONUS, new double[]{10, 25}
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        registerAffix(Affix.builder()
            .id("of_inferno")
            .displayName("de l'Inferno")
            .specialDescription("Effets Fever amplifiés")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(5)
            .stats(Map.of(
                StatType.FEVER_DAMAGE_BONUS, new double[]{30, 50},
                StatType.FEVER_DURATION_BONUS, new double[]{30, 60}
            ))
            .minZone(9)
            .build());

        // ==================== AFFIXES DE GROUPE ====================

        registerAffix(Affix.builder()
            .id("of_synergy")
            .displayName("de Synergie")
            .specialDescription("Bonus de proximité de groupe amélioré")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(25)
            .stats(Map.of(StatType.PARTY_BONUS, new double[]{10, 25})) // +10-25% bonus party
            .minZone(4)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_pack")
            .displayName("de la Meute")
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
            .displayName("Galvanisant")
            .specialDescription("Soigne les alliés proches")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_4)
            .weight(10)
            .stats(Map.of(StatType.PARTY_HEAL_ON_KILL, new double[]{1, 3})) // 1-3 PV aux alliés par kill
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(7)
            .build());

        // ==================== AFFIXES D'EXÉCUTION ====================

        registerAffix(Affix.builder()
            .id("executioner")
            .displayName("Bourreau")
            .specialDescription("Dégâts bonus contre les cibles blessées")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_3)
            .weight(30)
            .stats(Map.of(StatType.EXECUTE_DAMAGE, new double[]{10, 25})) // +10-25% vs <30% HP
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
                StatType.EXECUTE_DAMAGE, new double[]{35, 60},
                StatType.EXECUTE_THRESHOLD, new double[]{5, 10} // Exécute <5-10% HP
            ))
            .allowedCategories(List.of(ItemType.ItemCategory.WEAPON))
            .minZone(8)
            .build());

        // ==================== AFFIXES DE CHANCE ====================

        registerAffix(Affix.builder()
            .id("of_fortune")
            .displayName("de Fortune")
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
                StatType.LEGENDARY_DROP_BONUS, new double[]{25, 75},
                StatType.DOUBLE_LOOT_CHANCE, new double[]{5, 12}
            ))
            .minZone(8)
            .build());

        // ==================== AFFIXES SPÉCIAUX UNIQUES ====================

        registerAffix(Affix.builder()
            .id("undying")
            .displayName("Immortel")
            .specialDescription("Chance d'ignorer un coup fatal")
            .type(Affix.AffixType.PREFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(2)
            .stats(Map.of(StatType.CHEAT_DEATH_CHANCE, new double[]{3, 8}))
            .allowedCategories(List.of(ItemType.ItemCategory.ARMOR))
            .minZone(9)
            .build());

        registerAffix(Affix.builder()
            .id("of_the_phoenix")
            .displayName("du Phénix")
            .specialDescription("Renaître avec bonus après éviter la mort")
            .type(Affix.AffixType.SUFFIX)
            .tier(Affix.AffixTier.TIER_5)
            .weight(1)
            .stats(Map.of(
                StatType.CHEAT_DEATH_CHANCE, new double[]{5, 12},
                StatType.REVIVE_DAMAGE_BOOST, new double[]{50, 100} // +50-100% dégâts pendant 10s
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
        return byCategory.getOrDefault(itemType.getCategory(), List.of()).stream()
            .filter(a -> a.canDropInZone(zoneId))
            .collect(Collectors.toList());
    }

    /**
     * Tire un affix au sort pour un item
     */
    public Affix rollAffix(ItemType itemType, Affix.AffixType type, int zoneId, Set<String> excludeIds) {
        List<Affix> candidates = getAffixesForItemType(itemType, zoneId).stream()
            .filter(a -> a.getType() == type)
            .filter(a -> !excludeIds.contains(a.getId()))
            .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        // Calcul du poids total
        int totalWeight = candidates.stream().mapToInt(Affix::getWeight).sum();
        int roll = (int) (Math.random() * totalWeight);
        int cumulative = 0;

        for (Affix affix : candidates) {
            cumulative += affix.getWeight();
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
