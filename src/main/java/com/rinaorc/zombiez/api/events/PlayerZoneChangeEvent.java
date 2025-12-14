package com.rinaorc.zombiez.api.events;

import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Événement déclenché quand un joueur change de zone
 */
@Getter
public class PlayerZoneChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    
    @Nullable
    private final Zone fromZone;
    
    private final Zone toZone;
    
    /**
     * True si c'est la première fois que le joueur atteint cette zone
     */
    private final boolean firstTime;

    @Setter
    private boolean cancelled = false;

    public PlayerZoneChangeEvent(Player player, @Nullable Zone fromZone, Zone toZone, boolean firstTime) {
        this.player = player;
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.firstTime = firstTime;
    }

    /**
     * Vérifie si le joueur avance (vers le nord, zones plus difficiles)
     */
    public boolean isAdvancing() {
        if (fromZone == null) return true;
        return toZone.getId() > fromZone.getId();
    }

    /**
     * Vérifie si le joueur recule (vers le sud, zones plus faciles)
     */
    public boolean isRetreating() {
        if (fromZone == null) return false;
        return toZone.getId() < fromZone.getId();
    }

    /**
     * Obtient la différence de difficulté entre les zones
     */
    public int getDifficultyChange() {
        if (fromZone == null) return toZone.getDifficulty();
        return toZone.getDifficulty() - fromZone.getDifficulty();
    }

    /**
     * Vérifie si le joueur entre dans une zone PvP
     */
    public boolean isEnteringPvP() {
        return toZone.isPvpEnabled() && (fromZone == null || !fromZone.isPvpEnabled());
    }

    /**
     * Vérifie si le joueur quitte une zone PvP
     */
    public boolean isLeavingPvP() {
        return fromZone != null && fromZone.isPvpEnabled() && !toZone.isPvpEnabled();
    }

    /**
     * Vérifie si le joueur entre dans une zone safe
     */
    public boolean isEnteringSafeZone() {
        return toZone.isSafeZone() && (fromZone == null || !fromZone.isSafeZone());
    }

    /**
     * Vérifie si le joueur entre dans une zone de boss
     */
    public boolean isEnteringBossZone() {
        return toZone.isBossZone() && (fromZone == null || !fromZone.isBossZone());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
