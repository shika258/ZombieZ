package com.rinaorc.zombiez.pets.abilities.impl;

import lombok.Getter;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Getter
public class ParryPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldownSeconds;
    private final Map<UUID, Long> lastParry = new HashMap<>();

    public ParryPassive(String id, String name, String desc, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldownSeconds = cooldown;
        PassiveAbilityCleanup.registerForCleanup(lastParry);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    public boolean canParry(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastParry.getOrDefault(uuid, 0L);
        return now - last >= (cooldownSeconds * 1000L);
    }

    public void triggerParry(UUID uuid) {
        lastParry.put(uuid, System.currentTimeMillis());
    }
}