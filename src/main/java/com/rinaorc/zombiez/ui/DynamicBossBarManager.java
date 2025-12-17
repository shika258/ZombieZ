package com.rinaorc.zombiez.ui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.momentum.MomentumManager;
import com.rinaorc.zombiez.zombies.spawning.EventManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de Boss Bar Dynamique Ultra Complet
 * Affiche contextuellement: XP, Events, Streaks, Momentum, Boss, Combo, Fever
 *
 * Priorité d'affichage (du plus important au moins important):
 * 1. BOSS - Quand un boss est présent
 * 2. EVENT - Blood Moon, Horde, etc.
 * 3. FEVER - Mode Fever actif (double dégâts)
 * 4. LEVEL_UP - Animation de level up
 * 5. STREAK - Streak significatif (10+)
 * 6. XP_GAIN - Affichage temporaire du gain d'XP
 * 7. COMBO - Combo actif
 * 8. IDLE - Progression générale
 */
public class DynamicBossBarManager {

    private final ZombieZPlugin plugin;

    // Boss bars par joueur
    private final Map<UUID, PlayerBossBarData> playerBars;

    // Task de mise à jour
    private BukkitTask updateTask;

    // Configuration
    private static final long XP_DISPLAY_DURATION = 3000; // 3 secondes
    private static final long STREAK_DISPLAY_DURATION = 5000; // 5 secondes
    private static final long LEVEL_UP_DISPLAY_DURATION = 4000; // 4 secondes
    private static final int STREAK_THRESHOLD = 10; // Afficher à partir de 10 kills
    private static final int COMBO_THRESHOLD = 5; // Afficher à partir de 5 combo

    public DynamicBossBarManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.playerBars = new ConcurrentHashMap<>();
        startUpdateTask();
    }

    /**
     * Démarre la tâche de mise à jour périodique
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L); // Mise à jour toutes les 0.25 secondes
    }

    /**
     * Met à jour la boss bar d'un joueur
     */
    private void updatePlayerBossBar(Player player) {
        PlayerBossBarData data = getOrCreate(player);
        BossBarState newState = determineState(player, data);

        // Si l'état a changé, mettre à jour la boss bar
        if (newState != data.currentState) {
            transitionToState(player, data, newState);
        }

        // Mettre à jour le contenu selon l'état
        updateBossBarContent(player, data);
    }

    /**
     * Détermine l'état actuel de la boss bar
     */
    private BossBarState determineState(Player player, PlayerBossBarData data) {
        long now = System.currentTimeMillis();

        // 1. Boss actif (priorité maximale)
        if (data.activeBossUUID != null) {
            return BossBarState.BOSS;
        }

        // 2. Event global en cours
        EventManager eventManager = plugin.getEventManager();
        if (eventManager != null && eventManager.getCurrentEvent() != null
                && eventManager.getCurrentEvent().isActive()) {
            return BossBarState.EVENT;
        }

        // 3. Fever Mode
        MomentumManager momentum = plugin.getMomentumManager();
        if (momentum != null && momentum.isInFever(player)) {
            return BossBarState.FEVER;
        }

        // 4. Level Up récent
        if (data.levelUpTime > 0 && now - data.levelUpTime < LEVEL_UP_DISPLAY_DURATION) {
            return BossBarState.LEVEL_UP;
        }

        // 5. Streak significatif
        if (momentum != null) {
            int streak = momentum.getStreak(player);
            if (streak >= STREAK_THRESHOLD && data.lastStreakUpdate > 0
                    && now - data.lastStreakUpdate < STREAK_DISPLAY_DURATION) {
                return BossBarState.STREAK;
            }
        }

        // 6. Gain d'XP récent
        if (data.lastXpGainTime > 0 && now - data.lastXpGainTime < XP_DISPLAY_DURATION) {
            return BossBarState.XP_GAIN;
        }

        // 7. Combo actif
        if (momentum != null) {
            int combo = momentum.getCombo(player);
            if (combo >= COMBO_THRESHOLD) {
                return BossBarState.COMBO;
            }
        }

        // 8. État par défaut - Progression
        return BossBarState.IDLE;
    }

    /**
     * Transition vers un nouvel état
     */
    private void transitionToState(Player player, PlayerBossBarData data, BossBarState newState) {
        data.currentState = newState;
        BossBar bar = data.bossBar;

        // Configurer l'apparence selon l'état
        switch (newState) {
            case BOSS -> {
                bar.setColor(BarColor.RED);
                bar.setStyle(BarStyle.SEGMENTED_10);
                bar.addFlag(BarFlag.CREATE_FOG);
            }
            case EVENT -> {
                bar.setColor(BarColor.PURPLE);
                bar.setStyle(BarStyle.SEGMENTED_20);
                bar.removeFlag(BarFlag.CREATE_FOG);
            }
            case FEVER -> {
                bar.setColor(BarColor.RED);
                bar.setStyle(BarStyle.SOLID);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.addFlag(BarFlag.DARKEN_SKY);
            }
            case LEVEL_UP -> {
                bar.setColor(BarColor.GREEN);
                bar.setStyle(BarStyle.SOLID);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.removeFlag(BarFlag.DARKEN_SKY);
            }
            case STREAK -> {
                bar.setColor(BarColor.YELLOW);
                bar.setStyle(BarStyle.SEGMENTED_12);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.removeFlag(BarFlag.DARKEN_SKY);
            }
            case XP_GAIN -> {
                bar.setColor(BarColor.BLUE);
                bar.setStyle(BarStyle.SOLID);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.removeFlag(BarFlag.DARKEN_SKY);
            }
            case COMBO -> {
                bar.setColor(BarColor.PINK);
                bar.setStyle(BarStyle.SEGMENTED_6);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.removeFlag(BarFlag.DARKEN_SKY);
            }
            case IDLE -> {
                bar.setColor(BarColor.WHITE);
                bar.setStyle(BarStyle.SOLID);
                bar.removeFlag(BarFlag.CREATE_FOG);
                bar.removeFlag(BarFlag.DARKEN_SKY);
            }
        }
    }

    /**
     * Met à jour le contenu de la boss bar
     */
    private void updateBossBarContent(Player player, PlayerBossBarData data) {
        BossBar bar = data.bossBar;
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player);
        MomentumManager momentum = plugin.getMomentumManager();

        switch (data.currentState) {
            case BOSS -> updateBossContent(player, data, bar);
            case EVENT -> updateEventContent(player, data, bar);
            case FEVER -> updateFeverContent(player, data, bar, momentum);
            case LEVEL_UP -> updateLevelUpContent(player, data, bar, playerData);
            case STREAK -> updateStreakContent(player, data, bar, momentum);
            case XP_GAIN -> updateXpGainContent(player, data, bar, playerData);
            case COMBO -> updateComboContent(player, data, bar, momentum);
            case IDLE -> updateIdleContent(player, data, bar, playerData, momentum);
        }
    }

    /**
     * Mise à jour pour l'affichage de boss
     */
    private void updateBossContent(Player player, PlayerBossBarData data, BossBar bar) {
        bar.setTitle("§4§l☠ " + data.bossName + " §4§l☠ §c" +
                String.format("%.0f", data.bossHealth) + "/" + String.format("%.0f", data.bossMaxHealth) + " ❤");
        bar.setProgress(Math.max(0, Math.min(1, data.bossHealth / data.bossMaxHealth)));
    }

    /**
     * Mise à jour pour l'affichage d'event
     */
    private void updateEventContent(Player player, PlayerBossBarData data, BossBar bar) {
        EventManager eventManager = plugin.getEventManager();
        if (eventManager.getCurrentEvent() != null) {
            String eventName = eventManager.getCurrentEvent().getName();
            String icon = getEventIcon(eventName);
            bar.setTitle("§5§l" + icon + " " + eventName.toUpperCase() + " " + icon + " §d» §7Survivez!");

            // Animation de progression
            double progress = (System.currentTimeMillis() % 5000) / 5000.0;
            bar.setProgress(progress);
        }
    }

    /**
     * Mise à jour pour l'affichage de Fever
     */
    private void updateFeverContent(Player player, PlayerBossBarData data, BossBar bar, MomentumManager momentum) {
        if (momentum == null) return;

        MomentumManager.MomentumData momentumData = momentum.getMomentum(player);
        if (momentumData == null) return;

        int streak = momentumData.getKillStreak();
        double multiplier = momentum.getDamageMultiplier(player);

        // Animation pulsante
        long time = System.currentTimeMillis();
        boolean pulse = (time / 250) % 2 == 0;
        String fireColor = pulse ? "§c" : "§6";

        // Obtenir le vrai temps restant
        long timeRemaining = momentum.getFeverTimeRemaining(player);
        int secondsRemaining = (int) (timeRemaining / 1000);

        bar.setTitle(fireColor + "§l🔥 FEVER MODE 🔥 §e" + String.format("%.1fx", multiplier) +
                " DÉGÂTS §7| §c" + streak + " KILLS §7| §e" + secondsRemaining + "s");

        // Utiliser la vraie progression
        double feverProgress = momentum.getFeverProgress(player);
        bar.setProgress(Math.max(0.05, feverProgress));

        // Changer la couleur selon le temps restant
        if (feverProgress < 0.3) {
            bar.setColor(BarColor.YELLOW); // Attention, bientôt fini!
        } else {
            bar.setColor(BarColor.RED);
        }
    }

    /**
     * Mise à jour pour l'affichage de Level Up
     */
    private void updateLevelUpContent(Player player, PlayerBossBarData data, BossBar bar, PlayerData playerData) {
        if (playerData == null) return;

        int level = playerData.getLevel().get();
        long elapsed = System.currentTimeMillis() - data.levelUpTime;

        // Animation de remplissage
        double progress = Math.min(1.0, elapsed / 2000.0);

        // Animation de couleur arc-en-ciel
        BarColor[] colors = {BarColor.GREEN, BarColor.YELLOW, BarColor.BLUE, BarColor.PURPLE, BarColor.PINK};
        int colorIndex = (int) ((elapsed / 200) % colors.length);
        bar.setColor(colors[colorIndex]);

        bar.setTitle("§a§l✦ NIVEAU " + level + " ATTEINT! ✦ §7Félicitations!");
        bar.setProgress(progress);
    }

    /**
     * Mise à jour pour l'affichage de Streak
     */
    private void updateStreakContent(Player player, PlayerBossBarData data, BossBar bar, MomentumManager momentum) {
        if (momentum == null) return;

        int streak = momentum.getStreak(player);
        double multiplier = momentum.getDamageMultiplier(player);

        // Icône selon le niveau de streak
        String icon;
        BarColor color;
        if (streak >= 100) {
            icon = "§c§l💀";
            color = BarColor.RED;
        } else if (streak >= 50) {
            icon = "§6§l⚡";
            color = BarColor.YELLOW;
        } else if (streak >= 25) {
            icon = "§e§l🔥";
            color = BarColor.YELLOW;
        } else {
            icon = "§a§l✦";
            color = BarColor.GREEN;
        }

        bar.setColor(color);
        bar.setTitle(icon + " STREAK x" + streak + " " + icon + " §7| §e+" +
                String.format("%.1f", (multiplier - 1) * 100) + "% §7dégâts");
        bar.setProgress(Math.min(1.0, streak / 100.0)); // Max à 100
    }

    /**
     * Mise à jour pour l'affichage de gain d'XP
     */
    private void updateXpGainContent(Player player, PlayerBossBarData data, BossBar bar, PlayerData playerData) {
        if (playerData == null) return;

        int level = playerData.getLevel().get();
        double progress = playerData.getLevelProgress() / 100.0;
        long xpGained = data.lastXpGained;

        // Calcul de l'XP restant
        long currentXp = playerData.getXp().get();
        long requiredXp = playerData.getRequiredXpForNextLevel();

        bar.setTitle("§b§l✧ +" + formatXp(xpGained) + " XP §b✧ §7Niveau " + level +
                " §8| §7" + formatXp(currentXp) + "/" + formatXp(requiredXp));
        bar.setProgress(Math.max(0, Math.min(1, progress)));

        // Animation de fade
        long elapsed = System.currentTimeMillis() - data.lastXpGainTime;
        if (elapsed > XP_DISPLAY_DURATION * 0.7) {
            bar.setColor(BarColor.WHITE); // Fade out
        } else {
            bar.setColor(BarColor.BLUE);
        }
    }

    /**
     * Mise à jour pour l'affichage de Combo
     */
    private void updateComboContent(Player player, PlayerBossBarData data, BossBar bar, MomentumManager momentum) {
        if (momentum == null) return;

        MomentumManager.MomentumData momentumData = momentum.getMomentum(player);
        if (momentumData == null) return;

        int combo = momentumData.getCurrentCombo();
        double timer = momentumData.getComboTimer();
        double speedBonus = momentum.getSpeedMultiplier(player);

        // Animation selon le niveau de combo
        String comboText;
        if (combo >= 50) {
            comboText = "§c§lMEGA COMBO";
        } else if (combo >= 25) {
            comboText = "§6§lSUPER COMBO";
        } else if (combo >= 10) {
            comboText = "§e§lCOMBO";
        } else {
            comboText = "§a§lCOMBO";
        }

        bar.setTitle(comboText + " x" + combo + " §7| §a+" +
                String.format("%.0f", (speedBonus - 1) * 100) + "% §7vitesse §8| §f" +
                String.format("%.1f", timer) + "s");
        bar.setProgress(Math.max(0, Math.min(1, timer / 5.0))); // 5 secondes max
    }

    /**
     * Mise à jour pour l'affichage idle (progression générale)
     */
    private void updateIdleContent(Player player, PlayerBossBarData data, BossBar bar,
                                   PlayerData playerData, MomentumManager momentum) {
        if (playerData == null) return;

        int level = playerData.getLevel().get();
        int prestige = playerData.getPrestige().get();
        double progress = playerData.getLevelProgress() / 100.0;
        long kills = playerData.getKills().get();

        // Construire le titre
        StringBuilder title = new StringBuilder();

        // Prestige
        if (prestige > 0) {
            title.append("§6✦").append(prestige).append(" ");
        }

        // Niveau et progression
        title.append("§7Niveau §e").append(level);
        title.append(" §8| §7").append(String.format("%.1f", progress * 100)).append("%");

        // Kills totaux
        title.append(" §8| §c☠ ").append(formatNumber(kills));

        // Streak actuel si > 0
        if (momentum != null) {
            int streak = momentum.getStreak(player);
            if (streak > 0) {
                title.append(" §8| §e🔥 ").append(streak);
            }
        }

        bar.setTitle(title.toString());
        bar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    // ==================== MÉTHODES PUBLIQUES ====================

    /**
     * Notifie un gain d'XP
     */
    public void notifyXpGain(Player player, long xpGained) {
        PlayerBossBarData data = getOrCreate(player);
        data.lastXpGainTime = System.currentTimeMillis();
        data.lastXpGained = xpGained;
    }

    /**
     * Notifie un kill (update streak)
     */
    public void notifyKill(Player player) {
        PlayerBossBarData data = getOrCreate(player);
        data.lastStreakUpdate = System.currentTimeMillis();
    }

    /**
     * Notifie un level up
     */
    public void notifyLevelUp(Player player, int newLevel) {
        PlayerBossBarData data = getOrCreate(player);
        data.levelUpTime = System.currentTimeMillis();
        data.lastLevel = newLevel;
    }

    /**
     * Enregistre un boss actif
     */
    public void registerBoss(Player player, UUID bossUUID, String bossName, double maxHealth) {
        PlayerBossBarData data = getOrCreate(player);
        data.activeBossUUID = bossUUID;
        data.bossName = bossName;
        data.bossMaxHealth = maxHealth;
        data.bossHealth = maxHealth;
    }

    /**
     * Met à jour la santé d'un boss
     */
    public void updateBossHealth(Player player, UUID bossUUID, double currentHealth) {
        PlayerBossBarData data = playerBars.get(player.getUniqueId());
        if (data != null && bossUUID.equals(data.activeBossUUID)) {
            data.bossHealth = currentHealth;
        }
    }

    /**
     * Retire un boss
     */
    public void removeBoss(Player player, UUID bossUUID) {
        PlayerBossBarData data = playerBars.get(player.getUniqueId());
        if (data != null && bossUUID.equals(data.activeBossUUID)) {
            data.activeBossUUID = null;
            data.bossName = null;
        }
    }

    /**
     * Ajoute un joueur au système
     */
    public void addPlayer(Player player) {
        getOrCreate(player);
    }

    /**
     * Retire un joueur du système
     */
    public void removePlayer(Player player) {
        PlayerBossBarData data = playerBars.remove(player.getUniqueId());
        if (data != null && data.bossBar != null) {
            data.bossBar.removeAll();
        }
    }

    /**
     * Arrête le manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        for (PlayerBossBarData data : playerBars.values()) {
            if (data.bossBar != null) {
                data.bossBar.removeAll();
            }
        }
        playerBars.clear();
    }

    // ==================== UTILITAIRES ====================

    private PlayerBossBarData getOrCreate(Player player) {
        return playerBars.computeIfAbsent(player.getUniqueId(), k -> {
            PlayerBossBarData data = new PlayerBossBarData();
            data.bossBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
            data.bossBar.addPlayer(player);
            data.bossBar.setVisible(true);
            return data;
        });
    }

    private String getEventIcon(String eventName) {
        return switch (eventName.toLowerCase()) {
            case "blood moon" -> "☠";
            case "horde" -> "⚔";
            case "patient zéro" -> "💀";
            default -> "★";
        };
    }

    private String formatXp(long xp) {
        if (xp >= 1000000) {
            return String.format("%.1fM", xp / 1000000.0);
        } else if (xp >= 1000) {
            return String.format("%.1fK", xp / 1000.0);
        }
        return String.valueOf(xp);
    }

    private String formatNumber(long num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        } else if (num >= 1000) {
            return String.format("%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * États possibles de la boss bar
     */
    public enum BossBarState {
        BOSS,       // Boss actif
        EVENT,      // Event global
        FEVER,      // Fever mode
        LEVEL_UP,   // Animation level up
        STREAK,     // Streak significatif
        XP_GAIN,    // Gain d'XP récent
        COMBO,      // Combo actif
        IDLE        // Progression générale
    }

    /**
     * Données de boss bar par joueur
     */
    @Getter
    private static class PlayerBossBarData {
        BossBar bossBar;
        BossBarState currentState = BossBarState.IDLE;

        // XP
        long lastXpGainTime = 0;
        long lastXpGained = 0;

        // Level Up
        long levelUpTime = 0;
        int lastLevel = 0;

        // Streak
        long lastStreakUpdate = 0;

        // Boss
        UUID activeBossUUID = null;
        String bossName = null;
        double bossMaxHealth = 0;
        double bossHealth = 0;
    }
}
