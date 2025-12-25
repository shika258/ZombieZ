package com.rinaorc.zombiez.api.events;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Événement déclenché quand un zombie meurt
 * Utilisé pour tracker les kills et mettre à jour la progression
 */
@Getter
public class ZombieDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Le joueur qui a tué le zombie (null si mort naturelle ou par autre cause)
     */
    @Nullable
    private final Player killer;

    /**
     * L'entité zombie qui est morte
     */
    private final Entity zombie;

    /**
     * Le type de zombie (WALKER, RUNNER, ELITE, BOSS, etc.)
     */
    private final String zombieType;

    /**
     * Le niveau du zombie
     */
    private final int zombieLevel;

    /**
     * Si c'est un zombie Élite
     */
    private final boolean elite;

    /**
     * Si c'est un Boss
     */
    private final boolean boss;

    /**
     * L'XP gagné pour ce kill
     */
    private final long xpReward;

    /**
     * Les points gagnés pour ce kill
     */
    private final int pointsReward;

    /**
     * La zone où le zombie est mort
     */
    private final int zoneId;

    /**
     * La location de mort
     */
    private final Location deathLocation;

    public ZombieDeathEvent(@Nullable Player killer, Entity zombie, String zombieType,
                            int zombieLevel, boolean elite, boolean boss,
                            long xpReward, int pointsReward, int zoneId) {
        this.killer = killer;
        this.zombie = zombie;
        this.zombieType = zombieType;
        this.zombieLevel = zombieLevel;
        this.elite = elite;
        this.boss = boss;
        this.xpReward = xpReward;
        this.pointsReward = pointsReward;
        this.zoneId = zoneId;
        this.deathLocation = zombie.getLocation();
    }

    /**
     * Vérifie si le zombie a été tué par un joueur
     */
    public boolean wasKilledByPlayer() {
        return killer != null;
    }

    /**
     * Vérifie si c'est un zombie spécial (élite ou boss)
     */
    public boolean isSpecialZombie() {
        return elite || boss;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
