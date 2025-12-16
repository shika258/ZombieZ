package com.rinaorc.zombiez.classes.seasons;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Gestionnaire des saisons pour le systeme de classes
 *
 * PHILOSOPHIE:
 * - Les saisons durent typiquement 2-3 mois
 * - A chaque nouvelle saison:
 *   - Reset du niveau de classe (power reset)
 *   - Conservation des cosmetiques et achievements
 *   - Nouveaux objectifs saisonniers
 *
 * RESET PARTIEL:
 * Ce qui est reset:
 * - Niveau de classe -> 1
 *
 * Ce qui est conserve:
 * - Classe selectionnee
 * - Cosmetiques debloques
 * - Achievements
 * - Monnaie premium
 * - Historique des saisons passees
 */
@Getter
public class SeasonManager {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;

    // Etat de la saison actuelle
    private int currentSeasonNumber;
    private LocalDate seasonStartDate;
    private LocalDate seasonEndDate;
    private String seasonTheme;

    // Configuration
    private static final int DEFAULT_SEASON_DURATION_DAYS = 90;  // 3 mois

    // Statistiques saisonnieres
    private final Map<UUID, SeasonStats> seasonStats;

    public SeasonManager(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.seasonStats = new HashMap<>();

        loadSeasonConfig();
    }

    private void loadSeasonConfig() {
        // TODO: Charger depuis la config/BDD
        this.currentSeasonNumber = 1;
        this.seasonStartDate = LocalDate.now();
        this.seasonEndDate = seasonStartDate.plusDays(DEFAULT_SEASON_DURATION_DAYS);
        this.seasonTheme = "Eveil des Morts";
    }

    // ==================== GESTION DE SAISON ====================

    /**
     * Verifie si la saison doit etre terminee et lance la transition
     */
    public void checkSeasonEnd() {
        if (LocalDate.now().isAfter(seasonEndDate)) {
            startNewSeason();
        }
    }

    /**
     * Demarre une nouvelle saison
     */
    public void startNewSeason() {
        plugin.getLogger().info("[Saisons] Demarrage de la saison " + (currentSeasonNumber + 1));

        // Archiver les stats de la saison actuelle
        archiveSeasonStats();

        // Reset tous les joueurs
        resetAllPlayers();

        // Mettre a jour la config de saison
        currentSeasonNumber++;
        seasonStartDate = LocalDate.now();
        seasonEndDate = seasonStartDate.plusDays(DEFAULT_SEASON_DURATION_DAYS);

        // TODO: Broadcast a tous les joueurs
        plugin.getLogger().info("[Saisons] Saison " + currentSeasonNumber + " demarree!");
    }

    /**
     * Reset un joueur pour la nouvelle saison
     */
    public void resetPlayerForSeason(Player player) {
        ClassData data = classManager.getClassData(player);
        if (data == null) return;

        // Sauvegarder ce qui doit etre conserve
        SeasonResetResult result = performPartialReset(data);

        // Informer le joueur
        player.sendMessage("");
        player.sendMessage("§6§l+ NOUVELLE SAISON " + currentSeasonNumber + " +");
        player.sendMessage("");
        player.sendMessage("§7Vos progres ont ete partiellement reset:");
        player.sendMessage("§c- Niveau de classe: §f1");
        player.sendMessage("");
        player.sendMessage("§aCe qui est conserve:");
        if (data.hasClass()) {
            player.sendMessage("§a+ Classe: §f" + data.getSelectedClass().getColoredName());
        }
        player.sendMessage("§a+ Achievements");
        player.sendMessage("");
        player.sendMessage("§eBonne chance pour cette nouvelle saison!");
        player.sendMessage("");
    }

    /**
     * Effectue le reset partiel des donnees d'un joueur
     */
    private SeasonResetResult performPartialReset(ClassData data) {
        SeasonResetResult result = new SeasonResetResult();

        // Sauvegarder stats pour l'historique
        result.previousClassLevel = data.getClassLevel().get();

        // === RESET ===
        // Reset niveau de classe a 1
        data.getClassLevel().set(1);

        // Reset XP de classe
        data.getClassXp().set(0);

        // Reset statistiques de session
        data.resetSessionStats();

        // === CONSERVATION ===
        // Classe selectionnee - conservee

        return result;
    }

    /**
     * Reset tous les joueurs connectes
     */
    private void resetAllPlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::resetPlayerForSeason);
        // Note: Les joueurs offline seront reset a leur connexion
    }

    /**
     * Archive les statistiques de la saison
     */
    private void archiveSeasonStats() {
        // TODO: Sauvegarder en BDD
        seasonStats.clear();
    }

    /**
     * Obtient le nombre de jours restants dans la saison
     */
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), seasonEndDate);
    }

    /**
     * Obtient le pourcentage de progression de la saison
     */
    public double getSeasonProgress() {
        long totalDays = ChronoUnit.DAYS.between(seasonStartDate, seasonEndDate);
        long elapsed = ChronoUnit.DAYS.between(seasonStartDate, LocalDate.now());
        return Math.min(100.0, (elapsed * 100.0) / totalDays);
    }

    // ==================== STATISTIQUES SAISONNIERES ====================

    /**
     * Enregistre les stats d'un joueur pour la saison
     */
    public void recordPlayerStats(UUID playerId, SeasonStats stats) {
        seasonStats.put(playerId, stats);
    }

    /**
     * Obtient les stats d'un joueur pour la saison en cours
     */
    public SeasonStats getPlayerSeasonStats(UUID playerId) {
        return seasonStats.computeIfAbsent(playerId, k -> new SeasonStats());
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Resultat d'un reset de saison
     */
    @Getter
    public static class SeasonResetResult {
        int previousClassLevel;
    }

    /**
     * Statistiques d'un joueur pour une saison
     */
    @Getter
    public static class SeasonStats {
        private int maxClassLevel;
        private int maxZoneReached;
        private int bossKills;
        private int totalKills;
        private long playTime;
        private int achievementsUnlocked;

        public void update(int classLevel, int zone, int bosses, int kills) {
            this.maxClassLevel = Math.max(maxClassLevel, classLevel);
            this.maxZoneReached = Math.max(maxZoneReached, zone);
            this.bossKills += bosses;
            this.totalKills += kills;
        }

        public void addPlayTime(long minutes) {
            this.playTime += minutes;
        }

        public void unlockAchievement() {
            this.achievementsUnlocked++;
        }
    }
}
