package com.rinaorc.zombiez.api.events;

import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Événement déclenché quand les données d'un joueur sont chargées
 */
@Getter
public class PlayerDataLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerData data;
    private final boolean newPlayer;

    public PlayerDataLoadEvent(Player player, PlayerData data) {
        this.player = player;
        this.data = data;
        // Nouveau joueur si pas de kills et peu de temps de jeu
        this.newPlayer = data.getKills().get() == 0 && data.getPlaytime().get() < 60;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
