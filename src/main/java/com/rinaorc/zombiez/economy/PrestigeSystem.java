package com.rinaorc.zombiez.economy;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Système de prestige
 */
public class PrestigeSystem {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<Integer, PrestigeRewards> prestigeRewards = new HashMap<>();
    
    public static final int MAX_PRESTIGE = 10;
    public static final int REQUIRED_LEVEL = 100;

    public PrestigeSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initRewards();
    }

    /**
     * Initialise les récompenses de prestige
     */
    private void initRewards() {
        prestigeRewards.put(1, new PrestigeRewards(500, 10, 1.05));
        prestigeRewards.put(2, new PrestigeRewards(1000, 25, 1.10));
        prestigeRewards.put(3, new PrestigeRewards(2000, 50, 1.15));
        prestigeRewards.put(4, new PrestigeRewards(3500, 75, 1.20));
        prestigeRewards.put(5, new PrestigeRewards(5000, 100, 1.30));
        prestigeRewards.put(6, new PrestigeRewards(7500, 150, 1.40));
        prestigeRewards.put(7, new PrestigeRewards(10000, 200, 1.50));
        prestigeRewards.put(8, new PrestigeRewards(15000, 300, 1.65));
        prestigeRewards.put(9, new PrestigeRewards(20000, 400, 1.80));
        prestigeRewards.put(10, new PrestigeRewards(30000, 500, 2.00));
    }

    /**
     * Vérifie si un joueur peut prestige
     */
    public boolean canPrestige(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        return data.getLevel().get() >= REQUIRED_LEVEL && 
               data.getPrestige().get() < MAX_PRESTIGE;
    }

    /**
     * Effectue le prestige
     */
    public boolean prestige(Player player) {
        if (!canPrestige(player)) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        int newPrestige = data.getPrestige().get() + 1;
        PrestigeRewards rewards = prestigeRewards.get(newPrestige);
        
        // Reset
        data.setLevel(1);
        data.getXp().set(0);
        data.setHighestZone(1);
        
        // Appliquer prestige
        data.getPrestige().set(newPrestige);
        
        // Donner récompenses
        if (rewards != null) {
            plugin.getEconomyManager().addPoints(player, rewards.points());
            plugin.getEconomyManager().addGems(player, rewards.gems());
        }
        
        // Annonce
        plugin.getServer().broadcastMessage(
            "§6§l★ " + player.getName() + " §ea atteint le Prestige " + newPrestige + "!"
        );
        
        return true;
    }

    /**
     * Obtient le multiplicateur de prestige
     */
    public double getMultiplier(int prestige) {
        PrestigeRewards rewards = prestigeRewards.get(prestige);
        return rewards != null ? rewards.multiplier() : 1.0;
    }

    /**
     * Récompenses de prestige
     */
    public record PrestigeRewards(long points, int gems, double multiplier) {}
}
