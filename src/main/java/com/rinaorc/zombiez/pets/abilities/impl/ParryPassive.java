package com.rinaorc.zombiez.pets.abilities.impl;

import java.util.UUID;

/**
 * Passif qui permet de parer des attaques
 */
public interface ParryPassive {
    boolean canParry(UUID playerId);
    void triggerParry(UUID playerId);
}
