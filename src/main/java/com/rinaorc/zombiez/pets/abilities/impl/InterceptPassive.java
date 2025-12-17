package com.rinaorc.zombiez.pets.abilities.impl;

import lombok.Getter;
import com.rinaorc.zombiez.pets.abilities.PetAbility;

@Getter
public class InterceptPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double interceptPercent;

    public InterceptPassive(String id, String name, String desc, double intercept) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.interceptPercent = intercept;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}