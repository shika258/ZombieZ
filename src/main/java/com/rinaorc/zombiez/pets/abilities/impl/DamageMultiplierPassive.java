package com.rinaorc.zombiez.pets.abilities.impl;

import lombok.Getter;
import com.rinaorc.zombiez.pets.abilities.PetAbility;

@Getter
public class DamageMultiplierPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double multiplier;

    DamageMultiplierPassive(String id, String name, String desc, double mult) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.multiplier = mult;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}