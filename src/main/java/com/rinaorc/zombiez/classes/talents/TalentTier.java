package com.rinaorc.zombiez.classes.talents;

import lombok.Getter;

/**
 * Les 9 paliers de talents
 * Chaque palier correspond a un niveau de classe requis
 */
@Getter
public enum TalentTier {

    TIER_1(0, "Fondation", "§7"),
    TIER_2(5, "Amplification", "§a"),
    TIER_3(10, "Specialisation", "§e"),
    TIER_4(15, "Evolution", "§6"),
    TIER_5(20, "Maitrise", "§c"),
    TIER_6(25, "Ascension", "§9"),
    TIER_7(30, "Transcendance", "§d"),
    TIER_8(40, "Apex", "§5"),
    TIER_9(50, "Legendaire", "§6§l");

    private final int requiredLevel;
    private final String displayName;
    private final String color;

    TalentTier(int requiredLevel, String displayName, String color) {
        this.requiredLevel = requiredLevel;
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Obtient le palier suivant
     */
    public TalentTier getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) return null;
        return values()[nextOrdinal];
    }

    /**
     * Obtient le palier precedent
     */
    public TalentTier getPrevious() {
        int prevOrdinal = this.ordinal() - 1;
        if (prevOrdinal < 0) return null;
        return values()[prevOrdinal];
    }

    /**
     * Verifie si un niveau de classe permet d'acceder a ce palier
     */
    public boolean isUnlocked(int classLevel) {
        return classLevel >= requiredLevel;
    }

    /**
     * Obtient le palier pour un niveau de classe donne
     */
    public static TalentTier getHighestUnlocked(int classLevel) {
        TalentTier highest = null;
        for (TalentTier tier : values()) {
            if (tier.isUnlocked(classLevel)) {
                highest = tier;
            }
        }
        return highest;
    }

    /**
     * Obtient le palier par son index (0-8)
     */
    public static TalentTier fromIndex(int index) {
        if (index < 0 || index >= values().length) return null;
        return values()[index];
    }

    /**
     * Retourne le numero du palier (1-9)
     */
    public int getNumber() {
        return ordinal() + 1;
    }
}
