package com.rinaorc.zombiez.items.types;

import lombok.Getter;
import org.bukkit.Color;

/**
 * Raretés des items
 * Chaque rareté définit les caractéristiques de génération
 */
@Getter
public enum Rarity {
    
    COMMON(
        "Commun",
        "&f",
        "§f",
        60.0,
        0,
        0.0, 0.1,
        1, 100,
        Color.WHITE,
        false
    ),
    
    UNCOMMON(
        "Peu Commun",
        "&a",
        "§a",
        25.0,
        1,
        0.05, 0.2,
        100, 300,
        Color.LIME,
        false
    ),
    
    RARE(
        "Rare",
        "&9",
        "§9",
        10.0,
        2,
        0.1, 0.3,
        300, 700,
        Color.BLUE,
        true
    ),
    
    EPIC(
        "Épique",
        "&5",
        "§5",
        4.0,
        3,
        0.15, 0.4,
        700, 1500,
        Color.PURPLE,
        true
    ),
    
    LEGENDARY(
        "Légendaire",
        "&6",
        "§6",
        0.9,
        4,
        0.2, 0.5,
        1500, 3000,
        Color.ORANGE,
        true
    ),
    
    MYTHIC(
        "Mythique",
        "&d",
        "§d",
        0.1,
        5,
        0.3, 0.6,
        3000, 5000,
        Color.FUCHSIA,
        true
    ),
    
    EXALTED(
        "EXALTED",
        "&c&l",
        "§c§l",
        0.01,
        6,
        0.4, 0.75,
        5000, 10000,
        Color.RED,
        true
    );

    private final String displayName;
    private final String colorCode;      // Pour Adventure/Legacy
    private final String chatColor;      // Pour le chat
    private final double baseChance;     // Chance de drop de base (%)
    private final int affixCount;        // Nombre d'affixes garantis
    private final double statBonusMin;   // Bonus min sur les stats (%)
    private final double statBonusMax;   // Bonus max sur les stats (%)
    private final int itemScoreMin;      // Item Score minimum
    private final int itemScoreMax;      // Item Score maximum
    private final Color glowColor;       // Couleur pour les effets visuels
    private final boolean hasLightBeam;  // Affiche un beam de lumière au drop

    Rarity(String displayName, String colorCode, String chatColor, double baseChance,
           int affixCount, double statBonusMin, double statBonusMax,
           int itemScoreMin, int itemScoreMax, Color glowColor, boolean hasLightBeam) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.chatColor = chatColor;
        this.baseChance = baseChance;
        this.affixCount = affixCount;
        this.statBonusMin = statBonusMin;
        this.statBonusMax = statBonusMax;
        this.itemScoreMin = itemScoreMin;
        this.itemScoreMax = itemScoreMax;
        this.glowColor = glowColor;
        this.hasLightBeam = hasLightBeam;
    }

    /**
     * Obtient le nom coloré complet
     */
    public String getColoredName() {
        return chatColor + displayName;
    }
    
    /**
     * Obtient la couleur chat (alias)
     */
    public String getColor() {
        return chatColor;
    }

    /**
     * Obtient les étoiles de rareté pour l'affichage
     */
    public String getStars() {
        int stars = ordinal() + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return chatColor + sb.toString();
    }

    /**
     * Obtient la rareté suivante (upgrade)
     */
    public Rarity getNext() {
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return this;
        }
        return values()[nextOrdinal];
    }

    /**
     * Obtient la rareté précédente
     */
    public Rarity getPrevious() {
        int prevOrdinal = ordinal() - 1;
        if (prevOrdinal < 0) {
            return this;
        }
        return values()[prevOrdinal];
    }

    /**
     * Vérifie si cette rareté est supérieure ou égale à une autre
     */
    public boolean isAtLeast(Rarity other) {
        return ordinal() >= other.ordinal();
    }

    /**
     * Génère un bonus de stat aléatoire dans la plage de cette rareté
     */
    public double rollStatBonus() {
        return statBonusMin + Math.random() * (statBonusMax - statBonusMin);
    }

    /**
     * Génère un Item Score aléatoire dans la plage de cette rareté
     */
    public int rollItemScore() {
        return itemScoreMin + (int) (Math.random() * (itemScoreMax - itemScoreMin));
    }

    /**
     * Obtient une rareté depuis son nom
     */
    public static Rarity fromName(String name) {
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name) || r.displayName.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return COMMON;
    }

    /**
     * Tire une rareté au sort selon les chances de base
     */
    public static Rarity roll(double luckBonus) {
        double roll = Math.random() * 100;
        double cumulative = 0;

        // Parcourir de la plus rare à la moins rare
        Rarity[] rarities = values();
        for (int i = rarities.length - 1; i >= 0; i--) {
            Rarity rarity = rarities[i];
            double chance = rarity.baseChance * (1 + luckBonus);
            cumulative += chance;
            
            if (roll < cumulative) {
                return rarity;
            }
        }

        return COMMON;
    }

    /**
     * Tire une rareté avec une chance minimum garantie
     */
    public static Rarity rollWithMinimum(double luckBonus, Rarity minimum) {
        Rarity rolled = roll(luckBonus);
        if (rolled.ordinal() < minimum.ordinal()) {
            return minimum;
        }
        return rolled;
    }
}
