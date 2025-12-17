package com.rinaorc.zombiez.pets.gacha;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de récompenses quotidiennes pour encourager la connexion
 * Style Brawl Stars avec streak bonus
 */
public class DailyRewardSystem {

    private final ZombieZPlugin plugin;

    // Données des joueurs: UUID -> DailyData
    private final Map<UUID, DailyRewardData> playerData = new ConcurrentHashMap<>();

    // Récompenses du calendrier (7 jours qui se répètent)
    // ÉQUILIBRÉ: Généreux pour encourager la connexion quotidienne
    // En 1 semaine: ~8 oeufs standards, 2 zone, 1 elite + fragments
    private static final DailyReward[] WEEKLY_REWARDS = {
        new DailyReward("Jour 1", EggType.STANDARD, 2, 25, 0, 0),           // 2 standards + fragments
        new DailyReward("Jour 2", EggType.STANDARD, 1, 75, 0, 0),           // 1 standard + fragments
        new DailyReward("Jour 3", EggType.STANDARD, 3, 50, 0, 0.05),        // 3 standards + 5% luck
        new DailyReward("Jour 4", EggType.ZONE, 1, 100, 0, 0),              // 1 zone + fragments
        new DailyReward("Jour 5", EggType.STANDARD, 2, 75, 0, 0.05),        // 2 standards + 5% luck
        new DailyReward("Jour 6", EggType.ZONE, 1, 150, 0, 0),              // 1 zone + fragments
        new DailyReward("Jour 7 §6(BONUS!)", EggType.ELITE, 1, 200, 0, 0.15) // 1 elite + 15% luck!
    };

    public DailyRewardSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Vérifie et donne les récompenses quotidiennes
     * @return true si des récompenses ont été données
     */
    public ClaimResult claimDailyReward(Player player) {
        UUID uuid = player.getUniqueId();
        DailyRewardData data = playerData.computeIfAbsent(uuid, k -> new DailyRewardData());

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate lastClaim = data.getLastClaimDate();

        // Déjà réclamé aujourd'hui?
        if (lastClaim != null && lastClaim.equals(today)) {
            return new ClaimResult(false, null, "Vous avez déjà réclamé votre récompense aujourd'hui!", 0);
        }

        // Vérifier le streak
        boolean streakContinued = lastClaim != null && lastClaim.plusDays(1).equals(today);
        if (!streakContinued && lastClaim != null) {
            // Streak cassé - reset
            data.resetStreak();
        }

        // Incrémenter le streak
        data.incrementStreak();
        data.setLastClaimDate(today);

        // Obtenir la récompense du jour (cycle de 7 jours)
        int dayIndex = (data.getStreak() - 1) % 7;
        DailyReward reward = WEEKLY_REWARDS[dayIndex];

        // Donner les récompenses
        PlayerPetData petData = plugin.getPetManager().getPlayerData(uuid);
        if (petData != null) {
            if (reward.eggType() != null && reward.eggCount() > 0) {
                petData.addEggs(reward.eggType(), reward.eggCount());
            }
            if (reward.fragments() > 0) {
                petData.addFragments(reward.fragments());
            }
        }

        // Appliquer le luck bonus temporaire si présent
        if (reward.luckBonus() > 0) {
            data.setTodayLuckBonus(reward.luckBonus());
        }

        return new ClaimResult(true, reward, buildRewardMessage(reward, data.getStreak()), data.getStreak());
    }

    private String buildRewardMessage(DailyReward reward, int streak) {
        StringBuilder msg = new StringBuilder();
        msg.append("§6§l★ RÉCOMPENSE QUOTIDIENNE! ★\n");
        msg.append("§7Streak: §e").append(streak).append(" jour(s)\n\n");

        if (reward.eggType() != null) {
            msg.append("§a+ ").append(reward.eggCount()).append("x ").append(reward.eggType().getColoredName()).append("\n");
        }
        if (reward.fragments() > 0) {
            msg.append("§a+ §e").append(reward.fragments()).append(" §7fragments\n");
        }
        if (reward.luckBonus() > 0) {
            int percent = (int) (reward.luckBonus() * 100);
            msg.append("§a+ §d").append(percent).append("% §7de chance bonus aujourd'hui!\n");
        }

        // Teaser pour demain
        int nextDayIndex = streak % 7;
        DailyReward nextReward = WEEKLY_REWARDS[nextDayIndex];
        msg.append("\n§7Demain: §f").append(nextReward.dayName());

        return msg.toString();
    }

    /**
     * Vérifie si le joueur peut claim
     */
    public boolean canClaim(Player player) {
        DailyRewardData data = playerData.get(player.getUniqueId());
        if (data == null) return true;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return !today.equals(data.getLastClaimDate());
    }

    /**
     * Obtient le streak actuel
     */
    public int getStreak(Player player) {
        DailyRewardData data = playerData.get(player.getUniqueId());
        return data != null ? data.getStreak() : 0;
    }

    /**
     * Obtient la prochaine récompense
     */
    public DailyReward getNextReward(Player player) {
        int streak = getStreak(player);
        int dayIndex = streak % 7;
        return WEEKLY_REWARDS[dayIndex];
    }

    /**
     * Obtient le bonus de chance actuel
     */
    public double getTodayLuckBonus(Player player) {
        DailyRewardData data = playerData.get(player.getUniqueId());
        if (data == null) return 0;

        // Le bonus n'est valide que si réclamé aujourd'hui
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (today.equals(data.getLastClaimDate())) {
            return data.getTodayLuckBonus();
        }
        return 0;
    }

    /**
     * Obtient toutes les récompenses de la semaine
     */
    public DailyReward[] getWeeklyRewards() {
        return WEEKLY_REWARDS;
    }

    /**
     * Charge les données d'un joueur (depuis BDD)
     */
    public void loadPlayerData(UUID uuid, int streak, LocalDate lastClaim, double luckBonus) {
        DailyRewardData data = new DailyRewardData();
        data.setStreak(streak);
        data.setLastClaimDate(lastClaim);
        data.setTodayLuckBonus(luckBonus);
        playerData.put(uuid, data);
    }

    /**
     * Obtient les données pour sauvegarde
     */
    public DailyRewardData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    // ==================== CLASSES INTERNES ====================

    @Getter
    public static class DailyRewardData {
        private int streak = 0;
        private LocalDate lastClaimDate = null;
        private double todayLuckBonus = 0;

        public void incrementStreak() {
            streak++;
        }

        public void resetStreak() {
            streak = 0;
        }

        public void setStreak(int streak) {
            this.streak = streak;
        }

        public void setLastClaimDate(LocalDate date) {
            this.lastClaimDate = date;
        }

        public void setTodayLuckBonus(double bonus) {
            this.todayLuckBonus = bonus;
        }
    }

    public record DailyReward(
        String dayName,
        EggType eggType,
        int eggCount,
        int fragments,
        int coins,
        double luckBonus
    ) {}

    public record ClaimResult(
        boolean success,
        DailyReward reward,
        String message,
        int newStreak
    ) {}
}
