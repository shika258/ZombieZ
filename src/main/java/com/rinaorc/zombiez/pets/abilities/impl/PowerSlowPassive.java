package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.PetData;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class PowerSlowPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageBonus;
    private final double speedMalus;

    public PowerSlowPassive(String id, String name, String desc, double damage, double speed) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageBonus = damage;
        this.speedMalus = speed;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onEquip(Player player, PetData petData) {
        float newSpeed = (float) Math.max(0.05, 0.2 + (0.2 * speedMalus));
        player.setWalkSpeed(newSpeed);
    }

    @Override
    public void onUnequip(Player player, PetData petData) {
        player.setWalkSpeed(0.2f);
    }
}