package com.rinaorc.zombiez.economy;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Système de récompenses quotidiennes
 */
public class DailyRewardManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<Integer, DailyReward> rewards = new HashMap<>();
    
    private static final int MAX_STREAK = 30;

    public DailyRewardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initRewards();
    }

    /**
     * Initialise les récompenses
     */
    private void initRewards() {
        // Jour 1-7: Récompenses de base
        rewards.put(1, new DailyReward(100, 0, null));
        rewards.put(2, new DailyReward(150, 0, null));
        rewards.put(3, new DailyReward(200, 1, null));
        rewards.put(4, new DailyReward(250, 1, null));
        rewards.put(5, new DailyReward(300, 2, null));
        rewards.put(6, new DailyReward(400, 2, null));
        rewards.put(7, new DailyReward(500, 5, "weekly_chest"));
        
        // Jour 8-14: Récompenses améliorées
        rewards.put(8, new DailyReward(600, 3, null));
        rewards.put(9, new DailyReward(700, 3, null));
        rewards.put(10, new DailyReward(800, 4, null));
        rewards.put(11, new DailyReward(900, 4, null));
        rewards.put(12, new DailyReward(1000, 5, null));
        rewards.put(13, new DailyReward(1200, 5, null));
        rewards.put(14, new DailyReward(1500, 10, "biweekly_chest"));
        
        // Jour 15-30: Récompenses premium
        for (int i = 15; i <= 30; i++) {
            int points = 1000 + (i - 14) * 200;
            int gems = 5 + (i - 14);
            String special = (i == 21 || i == 28) ? "premium_chest" : (i == 30 ? "legendary_chest" : null);
            rewards.put(i, new DailyReward(points, gems, special));
        }
    }

    /**
     * Vérifie si un joueur peut claim sa récompense
     */
    public boolean canClaim(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        Instant lastClaim = data.getLastDailyReward();
        if (lastClaim == null) return true;
        
        // Vérifier si 24h sont passées
        Instant now = Instant.now();
        return lastClaim.plus(24, ChronoUnit.HOURS).isBefore(now);
    }

    /**
     * Vérifie si le streak est cassé
     */
    public boolean isStreakBroken(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return true;
        
        Instant lastClaim = data.getLastDailyReward();
        if (lastClaim == null) return false;
        
        // Le streak est cassé si plus de 48h
        return lastClaim.plus(48, ChronoUnit.HOURS).isBefore(Instant.now());
    }

    /**
     * Claim la récompense quotidienne
     */
    public boolean claim(Player player) {
        if (!canClaim(player)) {
            MessageUtils.send(player, "§cTu as déjà récupéré ta récompense aujourd'hui!");
            return false;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Vérifier le streak
        int streak = data.getDailyStreak();
        if (isStreakBroken(player)) {
            streak = 0;
        }
        
        // Incrémenter le streak
        streak = Math.min(streak + 1, MAX_STREAK);
        data.setDailyStreak(streak);
        data.setLastDailyReward(Instant.now());
        
        // Donner les récompenses
        DailyReward reward = rewards.getOrDefault(streak, rewards.get(MAX_STREAK));
        
        plugin.getEconomyManager().addPoints(player, reward.points(), "Daily");
        if (reward.gems() > 0) {
            plugin.getEconomyManager().addGems(player, reward.gems(), "Daily");
        }
        
        // Message
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        MessageUtils.sendTitle(player, 
            "§e§lJOUR " + streak + "!",
            "§a+" + reward.points() + " Points §d+" + reward.gems() + " Gemmes",
            10, 40, 10);
        
        // Récompense spéciale
        if (reward.specialReward() != null) {
            giveSpecialReward(player, reward.specialReward());
        }
        
        return true;
    }

    /**
     * Donne une récompense spéciale
     */
    private void giveSpecialReward(Player player, String rewardId) {
        switch (rewardId) {
            case "weekly_chest" -> {
                MessageUtils.send(player, "§6Tu as reçu un §eCoffre Hebdomadaire§6!");
                // Ajouter le coffre
            }
            case "biweekly_chest" -> {
                MessageUtils.send(player, "§6Tu as reçu un §dCoffre Bi-Hebdomadaire§6!");
            }
            case "premium_chest" -> {
                MessageUtils.send(player, "§6Tu as reçu un §5Coffre Premium§6!");
            }
            case "legendary_chest" -> {
                MessageUtils.send(player, "§6Tu as reçu un §c§lCOFFRE LÉGENDAIRE§6!");
                plugin.getServer().broadcastMessage("§6" + player.getName() + " §ea obtenu un §c§lCoffre Légendaire §e(30 jours de streak)!");
            }
        }
    }

    /**
     * Obtient le temps restant avant le prochain claim
     */
    public long getTimeUntilNextClaim(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null || data.getLastDailyReward() == null) return 0;
        
        Instant nextClaim = data.getLastDailyReward().plus(24, ChronoUnit.HOURS);
        long remaining = ChronoUnit.MILLIS.between(Instant.now(), nextClaim);
        
        return Math.max(0, remaining);
    }

    /**
     * Récompense quotidienne
     */
    public record DailyReward(long points, int gems, String specialReward) {}
}
