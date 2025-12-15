package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.logging.Level;

/**
 * Gestionnaire de l'économie du jeu
 * Gère les Points, Fragments et Gems
 */
public class EconomyManager {

    private final ZombieZPlugin plugin;

    // Formats d'affichage
    private static final DecimalFormat POINTS_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("#.#");

    // Configuration des gains
    @Getter private int baseKillPoints = 5;
    @Getter private int eliteKillMultiplier = 3;
    @Getter private int bossKillMultiplier = 20;

    public EconomyManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Charge la configuration économique
     */
    private void loadConfig() {
        var config = plugin.getConfigManager().getEconomyConfig();
        if (config != null) {
            baseKillPoints = config.getInt("points.base-kill", 5);
            eliteKillMultiplier = config.getInt("points.elite-multiplier", 3);
            bossKillMultiplier = config.getInt("points.boss-multiplier", 20);
        }
    }

    // ==================== POINTS ====================

    /**
     * Ajoute des points à un joueur avec multiplicateurs
     */
    public void addPoints(Player player, long amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Appliquer les multiplicateurs
        double multiplier = data.getPointsMultiplier();
        
        // Multiplicateur de zone
        var zone = plugin.getZoneManager().getPlayerZone(player);
        if (zone != null) {
            multiplier *= zone.getLootMultiplier();
        }

        long finalAmount = Math.round(amount * multiplier);
        data.addPoints(finalAmount);

        // Notification
        if (finalAmount > 0) {
            MessageUtils.sendActionBar(player, "§a+" + formatPoints(finalAmount) + " Points §7(" + reason + ")");
        }
    }

    /**
     * Retire des points à un joueur
     * @return true si succès
     */
    public boolean removePoints(Player player, long amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        if (data.removePoints(amount)) {
            MessageUtils.sendActionBar(player, "§c-" + formatPoints(amount) + " Points §7(" + reason + ")");
            return true;
        }
        
        MessageUtils.send(player, "§cPoints insuffisants! §7(Requis: " + formatPoints(amount) + ")");
        return false;
    }

    /**
     * Vérifie si un joueur a assez de points
     */
    public boolean hasPoints(Player player, long amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null && data.hasPoints(amount);
    }

    /**
     * Obtient les points d'un joueur
     */
    public long getPoints(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null ? data.getPoints().get() : 0;
    }

    // ==================== GEMS ====================

    /**
     * Ajoute des gems à un joueur
     */
    public void addGems(Player player, int amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        data.addGems(amount);
        MessageUtils.send(player, "§d+" + amount + " 💎 Gems §7(" + reason + ")");
        
        // Son de récompense premium
        player.playSound(player.getLocation(), "entity.player.levelup", 1f, 1.5f);
    }

    /**
     * Retire des gems à un joueur
     * @return true si succès
     */
    public boolean removeGems(Player player, int amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        if (data.removeGems(amount)) {
            MessageUtils.send(player, "§d-" + amount + " 💎 Gems §7(" + reason + ")");
            return true;
        }
        
        MessageUtils.send(player, "§cGems insuffisantes! §7(Requis: " + amount + " 💎)");
        return false;
    }

    /**
     * Vérifie si un joueur a assez de gems
     */
    public boolean hasGems(Player player, int amount) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null && data.getGems().get() >= amount;
    }

    /**
     * Obtient les gems d'un joueur
     */
    public int getGems(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null ? data.getGems().get() : 0;
    }

    // ==================== XP ====================

    /**
     * Ajoute de l'XP à un joueur avec multiplicateurs
     * @return true si level up
     */
    public boolean addXp(Player player, long amount, String reason) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        // Appliquer les multiplicateurs
        double multiplier = data.getXpMultiplier();
        
        // Multiplicateur de zone
        var zone = plugin.getZoneManager().getPlayerZone(player);
        if (zone != null) {
            multiplier *= zone.getXpMultiplier();
        }

        long finalAmount = Math.round(amount * multiplier);
        boolean levelUp = data.addXp(finalAmount);

        // Notifier le système de Boss Bar Dynamique
        if (plugin.getDynamicBossBarManager() != null) {
            plugin.getDynamicBossBarManager().notifyXpGain(player, finalAmount);
        }

        // Level up !
        if (levelUp) {
            onLevelUp(player, data);
        }

        return levelUp;
    }

    /**
     * Appelé lors d'un level up
     */
    private void onLevelUp(Player player, PlayerData data) {
        int newLevel = data.getLevel().get();

        // Notifier le système de Boss Bar Dynamique
        if (plugin.getDynamicBossBarManager() != null) {
            plugin.getDynamicBossBarManager().notifyLevelUp(player, newLevel);
        }

        // Message
        MessageUtils.sendTitle(player, "§6§lNIVEAU " + newLevel, "§7Félicitations!", 10, 40, 10);

        // Son
        player.playSound(player.getLocation(), "entity.player.levelup", 1f, 1f);
        
        // Récompenses par niveau
        if (newLevel % 5 == 0) {
            // Récompense tous les 5 niveaux
            long pointsReward = newLevel * 100L;
            data.addPoints(pointsReward);
            MessageUtils.send(player, "§6§l★ §eRécompense de niveau: §6" + formatPoints(pointsReward) + " Points!");
        }
        
        if (newLevel % 10 == 0) {
            // Coffre tous les 10 niveaux
            MessageUtils.send(player, "§6§l★ §eVous avez débloqué un §6Coffre Rare§e!");
            // TODO: Donner le coffre via CrateManager
        }

        plugin.log(Level.INFO, "§7" + player.getName() + " a atteint le niveau §e" + newLevel);
    }

    // ==================== RÉCOMPENSES DE KILL ====================

    /**
     * Récompense un joueur pour un kill de zombie
     */
    public void rewardZombieKill(Player player, String zombieType, int zombieLevel) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Calculer les points de base selon le niveau du zombie
        long basePoints = baseKillPoints + (zombieLevel * 2L);
        
        // Multiplicateur selon le type
        double typeMultiplier = switch (zombieType.toUpperCase()) {
            case "ELITE", "SPECIAL" -> eliteKillMultiplier;
            case "BOSS", "MINIBOSS" -> bossKillMultiplier;
            case "CHAMPION" -> bossKillMultiplier * 2;
            default -> 1.0;
        };

        long finalPoints = Math.round(basePoints * typeMultiplier);

        // XP basée sur les points
        long xp = finalPoints * 2;

        // Streak bonus
        int streak = data.getKillStreak().get();
        if (streak > 0 && streak % 10 == 0) {
            // Bonus tous les 10 kills
            long streakBonus = streak * 5L;
            finalPoints += streakBonus;
            xp += streakBonus;
            MessageUtils.sendActionBar(player, "§6§l" + streak + " KILLS STREAK! §e+" + streakBonus + " Bonus!");
        }

        // Ajouter les récompenses
        addPoints(player, finalPoints, "Kill " + zombieType);
        addXp(player, xp, "Kill");

        // Mettre à jour les stats
        data.addKill();
        data.incrementStat("kills_" + zombieType.toLowerCase());
    }

    /**
     * Récompense un joueur pour avoir tué un boss
     */
    public void rewardBossKill(Player player, String bossName, int bossLevel) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Grosses récompenses pour les boss
        long points = bossLevel * 100L * bossKillMultiplier;
        long xp = bossLevel * 200L;

        addPoints(player, points, "Boss " + bossName);
        addXp(player, xp, "Boss Kill");

        // Stats
        data.incrementStat("boss_kills");
        data.incrementStat("boss_" + bossName.toLowerCase().replace(" ", "_"));

        // Annonce
        MessageUtils.broadcast("§6§l★ §e" + player.getName() + " §7a vaincu le boss §c" + bossName + "§7!");
    }

    // ==================== FORMATAGE ====================

    /**
     * Formate un nombre de points pour l'affichage
     */
    public static String formatPoints(long amount) {
        return POINTS_FORMAT.format(amount);
    }

    /**
     * Formate un nombre de manière compacte (1.2K, 3.5M, etc.)
     */
    public static String formatCompact(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1_000_000) return COMPACT_FORMAT.format(amount / 1000.0) + "K";
        if (amount < 1_000_000_000) return COMPACT_FORMAT.format(amount / 1_000_000.0) + "M";
        return COMPACT_FORMAT.format(amount / 1_000_000_000.0) + "B";
    }

    /**
     * Formate les gems
     */
    public static String formatGems(int amount) {
        return amount + " 💎";
    }
    
    // ==================== SURCHARGES POUR COMPATIBILITÉ ====================
    
    /**
     * Ajoute des points (surcharge int)
     */
    public void addPoints(Player player, int amount) {
        addPoints(player, (long) amount, "Récompense");
    }
    
    /**
     * Ajoute des points (surcharge long sans reason)
     */
    public void addPoints(Player player, long amount) {
        addPoints(player, amount, "Récompense");
    }
    
    /**
     * Retire des points (surcharge int)
     */
    public boolean removePoints(Player player, int amount) {
        return removePoints(player, (long) amount, "Achat");
    }
    
    /**
     * Retire des points (surcharge long sans reason)
     */
    public boolean removePoints(Player player, long amount) {
        return removePoints(player, amount, "Achat");
    }
    
    /**
     * Ajoute des gems (surcharge sans reason)
     */
    public void addGems(Player player, int amount) {
        addGems(player, amount, "Récompense");
    }
    
    /**
     * Retire des gems (surcharge sans reason)
     */
    public boolean removeGems(Player player, int amount) {
        return removeGems(player, amount, "Achat");
    }
    
    /**
     * Ajoute de l'XP (surcharge int)
     */
    public boolean addXP(Player player, int amount) {
        return addXp(player, (long) amount, "XP");
    }
    
    /**
     * Ajoute de l'XP (surcharge long)
     */
    public boolean addXP(Player player, long amount) {
        return addXp(player, amount, "XP");
    }
    
    /**
     * Ajoute de l'XP (alias)
     */
    public boolean addXp(Player player, int amount) {
        return addXp(player, (long) amount, "XP");
    }
}
