package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;

/**
 * Passif qui augmente la chance de coup critique du joueur
 * Se cumule avec les autres sources de critique (stuff, talents, etc.)
 */
@Getter
public class CritChancePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double critChanceBonus; // En pourcentage (0.01 = 1%)

    public CritChancePassive(String id, String name, String desc, double critChance) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.critChanceBonus = critChance;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}
