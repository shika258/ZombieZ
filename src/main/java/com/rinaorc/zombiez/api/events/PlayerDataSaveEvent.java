package com.rinaorc.zombiez.api.events;

import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Événement déclenché quand les données d'un joueur sont sauvegardées
 */
@Getter
public class PlayerDataSaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerData data;

    public PlayerDataSaveEvent(Player player, PlayerData data) {
        super(true); // Async event
        this.player = player;
        this.data = data;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
