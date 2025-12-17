package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;

@Getter
public class MultiAttackPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int attackCount;

    public MultiAttackPassive(String id, String name, String desc, int count) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.attackCount = count;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    // Le bonus est appliqué via PetCombatListener
}