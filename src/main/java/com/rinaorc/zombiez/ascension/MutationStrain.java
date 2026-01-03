package com.rinaorc.zombiez.ascension;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Les 3 souches de mutations du système d'Ascension
 * Chaque souche a un thème et des bonus spécifiques
 */
@Getter
public enum MutationStrain {

    CARNAGE(
        "Carnage",
        "💀",
        "§c",
        Material.REDSTONE,
        "La puissance par le sang",
        Sound.ENTITY_WITHER_HURT,
        0.8f
    ),

    SPECTRE(
        "Spectre",
        "👻",
        "§b",
        Material.ENDER_PEARL,
        "La vitesse de l'ombre",
        Sound.ENTITY_ENDERMAN_TELEPORT,
        1.2f
    ),

    BUTIN(
        "Butin",
        "💎",
        "§e",
        Material.GOLD_INGOT,
        "La fortune du chasseur",
        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
        1.5f
    );

    private final String displayName;
    private final String icon;
    private final String color;
    private final Material material;
    private final String description;
    private final Sound selectionSound;
    private final float soundPitch;

    MutationStrain(String displayName, String icon, String color, Material material,
                   String description, Sound selectionSound, float soundPitch) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
        this.material = material;
        this.description = description;
        this.selectionSound = selectionSound;
        this.soundPitch = soundPitch;
    }

    /**
     * Retourne le nom formaté avec icône et couleur
     */
    public String getFormattedName() {
        return color + icon + " " + displayName;
    }

    /**
     * Retourne le tag court pour l'ActionBar
     */
    public String getTag() {
        return color + icon;
    }
}
