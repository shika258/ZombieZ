package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;

@Getter
public class CritDamagePassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double critBonus;

    public CritDamagePassive(String id, String name, String desc, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.critBonus = bonus;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}