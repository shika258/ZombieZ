package com.rinaorc.zombiez.items.affixes;

import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.StatType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Représente un affix (modificateur) pour un item
 * Les affixes ajoutent des statistiques bonus aux items
 */
@Getter
@Builder
public class Affix {

    private final String id;
    private final String displayName;
    private final AffixType type;          // PREFIX ou SUFFIX
    private final AffixTier tier;          // Tier 1-5
    private final int weight;              // Poids pour le roll aléatoire
    
    // Stats fournies par cet affix
    private final Map<StatType, double[]> stats;  // StatType -> [min, max]
    
    // Restrictions
    private final List<ItemType.ItemCategory> allowedCategories;
    private final int minZone;             // Zone minimum pour drop
    
    // Effets spéciaux (optionnel)
    private final String specialEffect;    // ID de l'effet spécial
    private final String specialDescription;

    /**
     * Génère les valeurs de stats pour cet affix
     * @param rarityBonus Bonus de la rareté de l'item (0.0 à 0.75)
     * @return Map des stats avec leurs valeurs générées
     */
    public Map<StatType, Double> rollStats(double rarityBonus) {
        java.util.HashMap<StatType, Double> rolled = new java.util.HashMap<>();
        
        for (var entry : stats.entrySet()) {
            StatType stat = entry.getKey();
            double[] range = entry.getValue();
            double min = range[0];
            double max = range[1];
            
            // Valeur de base aléatoire
            double value = min + Math.random() * (max - min);
            
            // Appliquer le bonus de rareté
            value *= (1 + rarityBonus);
            
            // Appliquer le bonus de tier
            value *= tier.getStatMultiplier();
            
            rolled.put(stat, value);
        }
        
        return rolled;
    }

    /**
     * Vérifie si cet affix peut être appliqué à un type d'item
     */
    public boolean canApplyTo(ItemType itemType) {
        if (allowedCategories == null || allowedCategories.isEmpty()) {
            return true;
        }
        return allowedCategories.contains(itemType.getCategory());
    }

    /**
     * Vérifie si cet affix peut drop dans une zone
     */
    public boolean canDropInZone(int zoneId) {
        return zoneId >= minZone;
    }

    /**
     * Obtient le nom coloré selon le tier
     */
    public String getColoredName() {
        return tier.getColor() + displayName;
    }

    /**
     * Obtient la description pour le lore
     */
    public List<String> getLoreLines(Map<StatType, Double> rolledStats) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        
        for (var entry : rolledStats.entrySet()) {
            lines.add(entry.getKey().getLoreLine(entry.getValue()));
        }
        
        if (specialDescription != null && !specialDescription.isEmpty()) {
            lines.add("§d✦ " + specialDescription);
        }
        
        return lines;
    }

    /**
     * Types d'affixes (position dans le nom)
     */
    public enum AffixType {
        PREFIX,  // Avant le nom de base (ex: "Vampirique Épée")
        SUFFIX   // Après le nom de base (ex: "Épée de Flammes")
    }

    /**
     * Tiers d'affixes (puissance)
     */
    @Getter
    public enum AffixTier {
        TIER_1("I", "§f", 1.0, 100),
        TIER_2("II", "§a", 1.2, 60),
        TIER_3("III", "§9", 1.5, 30),
        TIER_4("IV", "§5", 1.8, 12),
        TIER_5("V", "§6", 2.2, 4);

        private final String numeral;
        private final String color;
        private final double statMultiplier;
        private final int baseWeight;

        AffixTier(String numeral, String color, double statMultiplier, int baseWeight) {
            this.numeral = numeral;
            this.color = color;
            this.statMultiplier = statMultiplier;
            this.baseWeight = baseWeight;
        }

        /**
         * Obtient le tier depuis un numéro (1-5)
         */
        public static AffixTier fromNumber(int number) {
            if (number < 1) number = 1;
            if (number > 5) number = 5;
            return values()[number - 1];
        }

        /**
         * Tire un tier au sort avec pondération
         */
        public static AffixTier roll(int zoneBonus) {
            int totalWeight = 0;
            for (AffixTier tier : values()) {
                totalWeight += tier.baseWeight + (zoneBonus * tier.ordinal());
            }

            int roll = (int) (Math.random() * totalWeight);
            int cumulative = 0;

            for (AffixTier tier : values()) {
                cumulative += tier.baseWeight + (zoneBonus * tier.ordinal());
                if (roll < cumulative) {
                    return tier;
                }
            }

            return TIER_1;
        }
    }
}
