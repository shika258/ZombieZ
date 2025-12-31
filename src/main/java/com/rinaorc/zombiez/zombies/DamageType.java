package com.rinaorc.zombiez.zombies;

/**
 * Types de dégâts pour le système de combat ZombieZ
 * Utilisé pour les effets spéciaux et les résistances des mobs
 */
public enum DamageType {
    PHYSICAL("Physique", "§c"),
    MAGIC("Magique", "§d"),
    FIRE("Feu", "§6"),
    ICE("Glace", "§b"),
    LIGHTNING("Foudre", "§e"),
    POISON("Poison", "§2"),
    TRUE("Vrai", "§f");

    private final String displayName;
    private final String color;

    DamageType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }
}
