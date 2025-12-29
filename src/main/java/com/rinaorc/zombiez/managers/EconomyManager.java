package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Random;
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

        // Tracker le total des points gagnés
        data.addTotalPointsEarned(finalAmount);

        // ============ ACHIEVEMENTS DE POINTS ============
        var achievementManager = plugin.getAchievementManager();
        long totalEarned = data.getTotalPointsEarned().get();
        long currentPoints = data.getPoints().get();

        // Gains cumulés (earning_1/2/3, millionaire, billionaire)
        achievementManager.checkAndUnlock(player, "earning_1", (int) Math.min(totalEarned, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "earning_2", (int) Math.min(totalEarned, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "earning_3", (int) Math.min(totalEarned, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "millionaire", (int) Math.min(totalEarned, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "billionaire", (int) Math.min(totalEarned, Integer.MAX_VALUE));

        // Richesse simultanée (wealthy - avoir 10M en même temps)
        achievementManager.checkAndUnlock(player, "wealthy", (int) Math.min(currentPoints, Integer.MAX_VALUE));

        // ============ MISSIONS DE POINTS ============
        plugin.getMissionManager().updateProgress(player,
            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.POINTS_EARNED, (int) finalAmount);

        // Notification
        if (finalAmount > 0) {
            MessageUtils.sendActionBar(player, "§a+" + formatPoints(finalAmount) + " Points §7(" + reason + ")");

            // Effet pluie de pièces si gros gain
            spawnCoinRain(player, finalAmount);
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
        MessageUtils.send(player, "§d+" + amount + " 💎 Gemmes §7(" + reason + ")");
        
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
            MessageUtils.send(player, "§d-" + amount + " 💎 Gemmes §7(" + reason + ")");
            return true;
        }

        MessageUtils.send(player, "§cGemmes insuffisantes! §7(Requis: " + amount + " 💎)");
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

        // Tracker le total d'XP gagné
        data.addTotalXp(finalAmount);

        // ============ XP DE CLASSE (30% de l'XP gagnée) ============
        // Tous les bonus d'XP (events, élites, assists, etc.) comptent aussi pour les classes
        var classManager = plugin.getClassManager();
        if (classManager != null) {
            var classData = classManager.getClassData(player);
            if (classData != null && classData.hasClass()) {
                long classXp = Math.round(finalAmount * 0.3); // 30% de l'XP standard
                if (classXp > 0 && classData.addClassXp(classXp)) {
                    classManager.handleClassLevelUp(player);
                }
            }
        }

        // ============ ACHIEVEMENTS D'XP ============
        var achievementManager = plugin.getAchievementManager();
        long totalXp = data.getTotalXp().get();
        achievementManager.checkAndUnlock(player, "xp_grinder_1", (int) Math.min(totalXp, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "xp_grinder_2", (int) Math.min(totalXp, Integer.MAX_VALUE));
        achievementManager.checkAndUnlock(player, "xp_master", (int) Math.min(totalXp, Integer.MAX_VALUE));

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

        // ============ ACHIEVEMENTS DE NIVEAU ============
        var achievementManager = plugin.getAchievementManager();
        achievementManager.checkAndUnlock(player, "level_10", newLevel);
        achievementManager.checkAndUnlock(player, "level_25", newLevel);
        achievementManager.checkAndUnlock(player, "level_50", newLevel);
        achievementManager.checkAndUnlock(player, "level_75", newLevel);
        achievementManager.checkAndUnlock(player, "level_100", newLevel);

        // ============ MISSIONS DE PROGRESSION ============
        plugin.getMissionManager().updateProgress(player,
            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.LEVELS_GAINED, 1);

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

        // ====== FIRST BLOOD OF THE DAY ======
        if (data.canClaimFirstBlood()) {
            double streakMultiplier = data.claimFirstBlood();
            int fbStreak = data.getFirstBloodStreak();
            triggerFirstBloodBonus(player, fbStreak, streakMultiplier);
        }

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
     * Déclenche le bonus First Blood of the Day avec effets visuels
     */
    private void triggerFirstBloodBonus(Player player, int streak, double multiplier) {
        // Calculer les récompenses de base (augmentent avec le streak)
        int basePoints = 50;
        int baseXp = 100;

        long bonusPoints = Math.round(basePoints * multiplier);
        long bonusXp = Math.round(baseXp * multiplier);

        // Ajouter les récompenses via les méthodes standard pour inclure l'XP de classe
        addPoints(player, (int) bonusPoints);
        addXp(player, (int) bonusXp, "First Blood");

        // ====== EFFETS VISUELS ET SONORES ======

        // Titre épique
        String streakText = streak > 1 ? "§6" + streak + " jours d'affilée!" : "§7Premier kill du jour!";
        MessageUtils.sendTitle(player,
            "§c§l⚔ FIRST BLOOD! ⚔",
            streakText,
            10, 50, 20
        );

        // Message détaillé
        player.sendMessage("");
        player.sendMessage("§c§l⚔ ════════ FIRST BLOOD! ════════ ⚔");
        player.sendMessage("");
        player.sendMessage("  §fPremier kill du jour!");
        if (streak > 1) {
            player.sendMessage("  §6Streak: §e" + streak + " jours §7(x" + String.format("%.1f", multiplier) + " bonus)");
        }
        player.sendMessage("");
        player.sendMessage("  §aRécompenses:");
        player.sendMessage("    §e+" + bonusPoints + " Points");
        player.sendMessage("    §b+" + bonusXp + " XP");
        player.sendMessage("");
        player.sendMessage("§c§l⚔ ══════════════════════════ ⚔");
        player.sendMessage("");

        // Sons épiques
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        // Particules de sang autour du joueur
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= 20 || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                // Particules rouges (sang)
                player.getWorld().spawnParticle(
                    Particle.DUST,
                    loc,
                    15,
                    1.5, 1.5, 1.5,
                    0,
                    new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.5f) // Dark red
                );

                // Particules dorées si streak
                if (streak > 1) {
                    player.getWorld().spawnParticle(
                        Particle.DUST,
                        loc,
                        8,
                        1.2, 1.2, 1.2,
                        0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f) // Gold
                    );
                }

                // Flash effect
                if (tick == 0) {
                    player.getWorld().spawnParticle(Particle.FLASH, loc, 1);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
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

    // ==================== EFFETS VISUELS ====================

    // Seuils pour les effets visuels
    private static final long COIN_RAIN_THRESHOLD = 100;      // Pluie légère
    private static final long COIN_SHOWER_THRESHOLD = 500;    // Pluie moyenne
    private static final long COIN_STORM_THRESHOLD = 1000;    // Tempête de pièces!

    private static final Random random = new Random();

    /**
     * Déclenche une pluie de pièces autour du joueur
     * Intensité basée sur le montant gagné
     */
    private void spawnCoinRain(Player player, long amount) {
        if (amount < COIN_RAIN_THRESHOLD) return;

        // Déterminer l'intensité
        int particles;
        int duration;
        float pitch;

        if (amount >= COIN_STORM_THRESHOLD) {
            // Tempête de pièces!
            particles = 30;
            duration = 20; // 1 seconde
            pitch = 1.5f;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, pitch);
        } else if (amount >= COIN_SHOWER_THRESHOLD) {
            // Pluie moyenne
            particles = 15;
            duration = 12;
            pitch = 1.3f;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, pitch);
        } else {
            // Pluie légère
            particles = 8;
            duration = 8;
            pitch = 1.2f;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, pitch);
        }

        final int finalParticles = particles;
        final int finalDuration = duration;

        // Animation asynchrone de pluie de pièces
        new BukkitRunnable() {
            int tick = 0;
            final Location center = player.getLocation().add(0, 2.5, 0);

            @Override
            public void run() {
                if (tick >= finalDuration || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Spawn des particules dorées qui tombent
                for (int i = 0; i < finalParticles / 4; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 3;
                    double offsetZ = (random.nextDouble() - 0.5) * 3;
                    double offsetY = random.nextDouble() * 0.5;

                    Location particleLoc = center.clone().add(offsetX, offsetY - (tick * 0.15), offsetZ);

                    // Particules dorées (pièces)
                    player.getWorld().spawnParticle(
                        Particle.DUST,
                        particleLoc,
                        1,
                        0, -0.1, 0,
                        0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f) // Gold color
                    );

                    // Quelques particules brillantes
                    if (random.nextFloat() < 0.3f) {
                        player.getWorld().spawnParticle(
                            Particle.END_ROD,
                            particleLoc,
                            1,
                            0.1, -0.05, 0.1,
                            0.01
                        );
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
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
