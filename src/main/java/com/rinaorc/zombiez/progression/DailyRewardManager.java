package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de récompenses quotidiennes avec streaks
 * Ultra généreux pour inciter les joueurs à revenir chaque jour
 */
public class DailyRewardManager {

    private final ZombieZPlugin plugin;
    
    // Récompenses par jour (cycle de 30 jours)
    private final Map<Integer, DailyReward> dailyRewards;
    
    // Cache des dernières réclamations
    private final Map<UUID, LocalDate> lastClaimDates;
    
    // Timezone du serveur
    private static final ZoneId SERVER_TIMEZONE = ZoneId.of("Europe/Paris");

    public DailyRewardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.dailyRewards = new LinkedHashMap<>();
        this.lastClaimDates = new ConcurrentHashMap<>();
        
        initializeRewards();
    }

    /**
     * Initialise toutes les récompenses quotidiennes
     */
    private void initializeRewards() {
        // === SEMAINE 1 - Introduction ===
        dailyRewards.put(1, DailyReward.builder()
            .day(1)
            .points(200)
            .fragments(5)
            .icon(Material.GOLD_INGOT)
            .name("§eJour 1")
            .build());
            
        dailyRewards.put(2, DailyReward.builder()
            .day(2)
            .points(350)
            .fragments(10)
            .icon(Material.GOLD_INGOT)
            .name("§eJour 2")
            .build());
            
        dailyRewards.put(3, DailyReward.builder()
            .day(3)
            .points(500)
            .crateType("common")
            .crateAmount(1)
            .icon(Material.CHEST)
            .name("§eJour 3 §7- Coffre!")
            .build());
            
        dailyRewards.put(4, DailyReward.builder()
            .day(4)
            .points(750)
            .fragments(15)
            .icon(Material.GOLD_INGOT)
            .name("§eJour 4")
            .build());
            
        dailyRewards.put(5, DailyReward.builder()
            .day(5)
            .points(1000)
            .crateType("common")
            .crateAmount(2)
            .icon(Material.CHEST)
            .name("§eJour 5 §7- 2 Coffres!")
            .build());
            
        dailyRewards.put(6, DailyReward.builder()
            .day(6)
            .points(1250)
            .fragments(20)
            .xpBonus(500)
            .icon(Material.EXPERIENCE_BOTTLE)
            .name("§eJour 6")
            .build());
            
        dailyRewards.put(7, DailyReward.builder()
            .day(7)
            .points(2500)
            .gems(15)
            .crateType("rare")
            .crateAmount(1)
            .icon(Material.DIAMOND)
            .name("§b§lJour 7 - BONUS SEMAINE!")
            .special(true)
            .build());
        
        // === SEMAINE 2 - Montée en puissance ===
        dailyRewards.put(8, DailyReward.builder()
            .day(8)
            .points(1500)
            .fragments(25)
            .icon(Material.GOLD_BLOCK)
            .name("§6Jour 8")
            .build());
            
        dailyRewards.put(9, DailyReward.builder()
            .day(9)
            .points(1750)
            .xpBonus(750)
            .icon(Material.EXPERIENCE_BOTTLE)
            .name("§6Jour 9")
            .build());
            
        dailyRewards.put(10, DailyReward.builder()
            .day(10)
            .points(2000)
            .crateType("rare")
            .crateAmount(1)
            .icon(Material.ENDER_CHEST)
            .name("§6Jour 10 §7- Coffre Rare!")
            .build());
            
        dailyRewards.put(11, DailyReward.builder()
            .day(11)
            .points(2250)
            .fragments(30)
            .icon(Material.GOLD_BLOCK)
            .name("§6Jour 11")
            .build());
            
        dailyRewards.put(12, DailyReward.builder()
            .day(12)
            .points(2500)
            .gems(10)
            .icon(Material.DIAMOND)
            .name("§6Jour 12")
            .build());
            
        dailyRewards.put(13, DailyReward.builder()
            .day(13)
            .points(2750)
            .fragments(35)
            .xpBonus(1000)
            .icon(Material.EXPERIENCE_BOTTLE)
            .name("§6Jour 13")
            .build());
            
        dailyRewards.put(14, DailyReward.builder()
            .day(14)
            .points(5000)
            .gems(25)
            .crateType("rare")
            .crateAmount(2)
            .title("daily_devoted")
            .titleDisplay("§eAssidu")
            .icon(Material.NAME_TAG)
            .name("§6§lJour 14 - TITRE DÉBLOQUÉ!")
            .special(true)
            .build());
        
        // === SEMAINE 3 - Récompenses premium ===
        for (int day = 15; day <= 20; day++) {
            int bonus = (day - 14) * 500;
            dailyRewards.put(day, DailyReward.builder()
                .day(day)
                .points(3000 + bonus)
                .fragments(40 + (day - 14) * 5)
                .gems((day - 14) * 3)
                .icon(Material.EMERALD)
                .name("§aJour " + day)
                .build());
        }
        
        dailyRewards.put(21, DailyReward.builder()
            .day(21)
            .points(8000)
            .gems(50)
            .crateType("epic")
            .crateAmount(1)
            .icon(Material.SHULKER_BOX)
            .name("§5§lJour 21 - 3 SEMAINES!")
            .special(true)
            .build());
        
        // === SEMAINE 4 - Final stretch ===
        for (int day = 22; day <= 29; day++) {
            int bonus = (day - 21) * 750;
            dailyRewards.put(day, DailyReward.builder()
                .day(day)
                .points(5000 + bonus)
                .fragments(60 + (day - 21) * 5)
                .gems(10 + (day - 21) * 2)
                .xpBonus(1500)
                .icon(Material.EMERALD_BLOCK)
                .name("§aJour " + day)
                .build());
        }
        
        // === JOUR 30 - RÉCOMPENSE ULTIME ===
        dailyRewards.put(30, DailyReward.builder()
            .day(30)
            .points(25000)
            .gems(100)
            .fragments(200)
            .crateType("legendary")
            .crateAmount(1)
            .title("daily_devoted_gold")
            .titleDisplay("§6§lDévoué")
            .cosmetic("aura_dedication")
            .cosmeticDisplay("§dAura de Dévotion")
            .icon(Material.NETHER_STAR)
            .name("§6§l★ JOUR 30 - RÉCOMPENSE ULTIME! ★")
            .special(true)
            .legendary(true)
            .build());
    }

    /**
     * Vérifie si le joueur peut réclamer sa récompense quotidienne
     */
    public boolean canClaim(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        Instant lastClaim = data.getLastDailyReward();
        if (lastClaim == null) return true;
        
        LocalDate lastClaimDate = lastClaim.atZone(SERVER_TIMEZONE).toLocalDate();
        LocalDate today = LocalDate.now(SERVER_TIMEZONE);
        
        return !lastClaimDate.equals(today);
    }

    /**
     * Réclame la récompense quotidienne
     */
    public boolean claimDailyReward(Player player) {
        if (!canClaim(player)) {
            long secondsUntilReset = getSecondsUntilReset();
            String timeStr = formatSeconds(secondsUntilReset);
            MessageUtils.sendRaw(player, "§cTu as déjà réclamé ta récompense aujourd'hui!");
            MessageUtils.sendRaw(player, "§7Prochaine récompense dans: §e" + timeStr);
            return false;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Vérifier le streak
        int currentStreak = calculateStreak(data);
        int newStreak = currentStreak + 1;
        
        // Obtenir la récompense du jour
        int rewardDay = ((newStreak - 1) % 30) + 1; // Cycle de 30 jours
        DailyReward reward = dailyRewards.get(rewardDay);
        
        if (reward == null) {
            reward = dailyRewards.get(1); // Fallback
        }
        
        // Appliquer le multiplicateur de streak (après 30 jours)
        double streakMultiplier = 1.0;
        if (newStreak > 30) {
            // +5% par cycle complet, max +50%
            int completedCycles = (newStreak - 1) / 30;
            streakMultiplier = 1.0 + Math.min(0.5, completedCycles * 0.05);
        }
        
        // Donner les récompenses
        giveRewards(player, data, reward, streakMultiplier);
        
        // Mettre à jour le streak
        data.setDailyStreak(newStreak);
        data.setLastDailyReward(Instant.now());
        
        // Afficher le message
        displayRewardMessage(player, reward, newStreak, streakMultiplier);
        
        // Sons
        if (reward.isLegendary()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        } else if (reward.isSpecial()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
        
        return true;
    }

    /**
     * Calcule le streak actuel
     */
    private int calculateStreak(PlayerData data) {
        Instant lastClaim = data.getLastDailyReward();
        if (lastClaim == null) return 0;
        
        LocalDate lastClaimDate = lastClaim.atZone(SERVER_TIMEZONE).toLocalDate();
        LocalDate today = LocalDate.now(SERVER_TIMEZONE);
        LocalDate yesterday = today.minusDays(1);
        
        // Si la dernière réclamation était hier, le streak continue
        if (lastClaimDate.equals(yesterday)) {
            return data.getDailyStreak();
        }
        
        // Sinon le streak est cassé
        return 0;
    }

    /**
     * Donne les récompenses au joueur
     */
    private void giveRewards(Player player, PlayerData data, DailyReward reward, double multiplier) {
        // Points
        if (reward.getPoints() > 0) {
            int points = (int) (reward.getPoints() * multiplier);
            plugin.getEconomyManager().addPoints(player, points);
        }
        
        // Gems
        if (reward.getGems() > 0) {
            int gems = (int) (reward.getGems() * multiplier);
            plugin.getEconomyManager().addGems(player, gems);
        }
        
        // Fragments
        if (reward.getFragments() > 0) {
            int fragments = (int) (reward.getFragments() * multiplier);
            data.getFragments().addAndGet(fragments);
        }
        
        // XP Bonus
        if (reward.getXpBonus() > 0) {
            int xp = (int) (reward.getXpBonus() * multiplier);
            plugin.getEconomyManager().addXP(player, xp);
        }
        
        // Crate
        if (reward.getCrateType() != null && !reward.getCrateType().isEmpty()) {
            // TODO: Intégrer avec CrateManager
            // Pour l'instant, donner un item placeholder
            MessageUtils.sendRaw(player, "§a+ " + reward.getCrateAmount() + "x Coffre " + 
                reward.getCrateType().substring(0, 1).toUpperCase() + reward.getCrateType().substring(1));
        }
        
        // Titre
        if (reward.getTitle() != null && !reward.getTitle().isEmpty()) {
            data.addTitle(reward.getTitle());
            MessageUtils.sendRaw(player, "§d+ Titre débloqué: " + reward.getTitleDisplay());
        }
        
        // Cosmétique
        if (reward.getCosmetic() != null && !reward.getCosmetic().isEmpty()) {
            data.addCosmetic(reward.getCosmetic());
            MessageUtils.sendRaw(player, "§d+ Cosmétique débloqué: " + reward.getCosmeticDisplay());
        }
    }

    /**
     * Affiche le message de récompense
     */
    private void displayRewardMessage(Player player, DailyReward reward, int streak, double multiplier) {
        player.sendMessage("");
        
        if (reward.isLegendary()) {
            MessageUtils.sendRaw(player, "§6§l★★★ RÉCOMPENSE QUOTIDIENNE - JOUR 30! ★★★");
        } else if (reward.isSpecial()) {
            MessageUtils.sendRaw(player, "§d§l★ RÉCOMPENSE QUOTIDIENNE SPÉCIALE ★");
        } else {
            MessageUtils.sendRaw(player, "§a§l✓ RÉCOMPENSE QUOTIDIENNE");
        }
        
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, reward.getName());
        MessageUtils.sendRaw(player, "");
        
        // Détails des récompenses
        if (reward.getPoints() > 0) {
            int points = (int) (reward.getPoints() * multiplier);
            MessageUtils.sendRaw(player, "  §e+ " + points + " Points");
        }
        if (reward.getGems() > 0) {
            int gems = (int) (reward.getGems() * multiplier);
            MessageUtils.sendRaw(player, "  §d+ " + gems + " Gems");
        }
        if (reward.getFragments() > 0) {
            int fragments = (int) (reward.getFragments() * multiplier);
            MessageUtils.sendRaw(player, "  §b+ " + fragments + " Fragments");
        }
        if (reward.getXpBonus() > 0) {
            int xp = (int) (reward.getXpBonus() * multiplier);
            MessageUtils.sendRaw(player, "  §a+ " + xp + " XP");
        }
        
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§7Streak: §e" + streak + " jours" + 
            (multiplier > 1.0 ? " §8(§a+" + (int)((multiplier - 1) * 100) + "% bonus§8)" : ""));
        
        // Prochaine récompense
        int nextDay = (streak % 30) + 1;
        DailyReward nextReward = dailyRewards.get(nextDay);
        if (nextReward != null && nextReward.isSpecial()) {
            MessageUtils.sendRaw(player, "§7Prochain jour spécial: §6Jour " + nextDay + "!");
        }
        
        player.sendMessage("");
    }

    /**
     * Obtient les secondes jusqu'au reset quotidien
     */
    public long getSecondsUntilReset() {
        ZonedDateTime now = ZonedDateTime.now(SERVER_TIMEZONE);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(SERVER_TIMEZONE);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    /**
     * Formate les secondes en HH:MM:SS
     */
    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Affiche le statut des récompenses quotidiennes
     */
    public void showStatus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        int currentStreak = calculateStreak(data);
        boolean canClaim = canClaim(player);
        
        player.sendMessage("");
        MessageUtils.sendRaw(player, "§e§l═══ RÉCOMPENSES QUOTIDIENNES ═══");
        MessageUtils.sendRaw(player, "");
        
        if (canClaim) {
            int nextDay = (currentStreak % 30) + 1;
            DailyReward reward = dailyRewards.get(nextDay);
            
            MessageUtils.sendRaw(player, "§a✓ Récompense disponible!");
            MessageUtils.sendRaw(player, "§7Jour: §e" + nextDay + "/30");
            MessageUtils.sendRaw(player, "§7Récompense: " + (reward != null ? reward.getName() : "???"));
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§aClique ici ou utilise §e/daily claim");
        } else {
            long seconds = getSecondsUntilReset();
            MessageUtils.sendRaw(player, "§c✗ Déjà réclamé aujourd'hui");
            MessageUtils.sendRaw(player, "§7Prochaine dans: §e" + formatSeconds(seconds));
        }
        
        MessageUtils.sendRaw(player, "");
        MessageUtils.sendRaw(player, "§7Streak actuel: §e" + (currentStreak + (canClaim ? 0 : 1)) + " jours");
        
        // Afficher les prochains jours spéciaux
        int currentDay = (currentStreak % 30) + 1;
        StringBuilder specialDays = new StringBuilder();
        for (int d = currentDay; d <= 30; d++) {
            DailyReward r = dailyRewards.get(d);
            if (r != null && r.isSpecial()) {
                if (specialDays.length() > 0) specialDays.append(", ");
                specialDays.append("§6").append(d);
            }
        }
        if (specialDays.length() > 0) {
            MessageUtils.sendRaw(player, "§7Jours spéciaux restants: " + specialDays);
        }
        
        player.sendMessage("");
    }

    // ==================== INNER CLASSES ====================

    @Getter
    @Builder
    public static class DailyReward {
        private final int day;
        @Builder.Default private final int points = 0;
        @Builder.Default private final int gems = 0;
        @Builder.Default private final int fragments = 0;
        @Builder.Default private final int xpBonus = 0;
        @Builder.Default private final String crateType = "";
        @Builder.Default private final int crateAmount = 0;
        @Builder.Default private final String title = "";
        @Builder.Default private final String titleDisplay = "";
        @Builder.Default private final String cosmetic = "";
        @Builder.Default private final String cosmeticDisplay = "";
        private final Material icon;
        private final String name;
        @Builder.Default private final boolean special = false;
        @Builder.Default private final boolean legendary = false;
    }
}
