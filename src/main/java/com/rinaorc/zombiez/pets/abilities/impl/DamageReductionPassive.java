package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;

@Getter
public class DamageReductionPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double reductionPercent;

    public DamageReductionPassive(String id, String name, String desc, double reduction) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.reductionPercent = reduction;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}
