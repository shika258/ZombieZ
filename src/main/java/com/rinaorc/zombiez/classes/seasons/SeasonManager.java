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
 * Gestionnaire des saisons pour le système de classes
 *
 * PHILOSOPHIE:
 * - Les saisons durent typiquement 2-3 mois
 * - À chaque nouvelle saison:
 *   - Reset des talents et buffs arcade (power reset)
 *   - Conservation des cosmétiques et achievements
 *   - Rotation du pool de buffs disponibles
 *   - Nouveaux objectifs saisonniers
 *
 * RESET PARTIEL:
 * Ce qui est reset:
 * - Niveau de classe -> 1
 * - Points de talent -> remboursés
 * - Buffs arcade -> 0
 * - Armes collectées -> gardées mais évolution reset
 *
 * Ce qui est conservé:
 * - Classe sélectionnée
 * - Cosmétiques débloqués
 * - Achievements
 * - Monnaie premium
 * - Historique des saisons passées
 */
@Getter
public class SeasonManager {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;

    // État de la saison actuelle
    private int currentSeasonNumber;
    private LocalDate seasonStartDate;
    private LocalDate seasonEndDate;
    private String seasonTheme;

    // Configuration
    private static final int DEFAULT_SEASON_DURATION_DAYS = 90;  // 3 mois

    // Pool de buffs actif pour la saison (rotation)
    private final Set<String> activeBuffPool;

    // Statistiques saisonnières
    private final Map<UUID, SeasonStats> seasonStats;

    public SeasonManager(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.activeBuffPool = new HashSet<>();
        this.seasonStats = new HashMap<>();

        loadSeasonConfig();
    }

    private void loadSeasonConfig() {
        // TODO: Charger depuis la config/BDD
        this.currentSeasonNumber = 1;
        this.seasonStartDate = LocalDate.now();
        this.seasonEndDate = seasonStartDate.plusDays(DEFAULT_SEASON_DURATION_DAYS);
        this.seasonTheme = "Éveil des Morts";

        // Pool de buffs par défaut (tous actifs en saison 1)
        initializeDefaultBuffPool();
    }

    private void initializeDefaultBuffPool() {
        // En saison 1, tous les buffs sont actifs
        // Les saisons suivantes peuvent désactiver certains buffs pour forcer la diversité
        activeBuffPool.addAll(Arrays.asList(
            // Offensifs
            "dmg_up", "crit_chance", "crit_dmg", "lifesteal",
            // Défensifs
            "hp_up", "armor_up", "regen_up",
            // Utilitaires
            "speed_up", "xp_up", "cdr_up", "energy_up",
            // Rares
            "berserker", "tank", "assassin", "survivor",
            // Classe - Guerrier
            "gue_melee", "gue_tank", "gue_sustain",
            // Classe - Chasseur
            "cha_precision", "cha_lethality", "cha_agility",
            // Classe - Occultiste
            "occ_power", "occ_haste", "occ_mana"
        ));
    }

    // ==================== GESTION DE SAISON ====================

    /**
     * Vérifie si la saison doit être terminée et lance la transition
     */
    public void checkSeasonEnd() {
        if (LocalDate.now().isAfter(seasonEndDate)) {
            startNewSeason();
        }
    }

    /**
     * Démarre une nouvelle saison
     */
    public void startNewSeason() {
        plugin.getLogger().info("[Saisons] Démarrage de la saison " + (currentSeasonNumber + 1));

        // Archiver les stats de la saison actuelle
        archiveSeasonStats();

        // Reset tous les joueurs
        resetAllPlayers();

        // Mettre à jour la config de saison
        currentSeasonNumber++;
        seasonStartDate = LocalDate.now();
        seasonEndDate = seasonStartDate.plusDays(DEFAULT_SEASON_DURATION_DAYS);

        // Rotation des buffs
        rotateBuffPool();

        // TODO: Broadcast à tous les joueurs
        plugin.getLogger().info("[Saisons] Saison " + currentSeasonNumber + " démarrée!");
    }

    /**
     * Reset un joueur pour la nouvelle saison
     */
    public void resetPlayerForSeason(Player player) {
        ClassData data = classManager.getClassData(player);
        if (data == null) return;

        // Sauvegarder ce qui doit être conservé
        SeasonResetResult result = performPartialReset(data);

        // Informer le joueur
        player.sendMessage("");
        player.sendMessage("§6§l✦ NOUVELLE SAISON " + currentSeasonNumber + " ✦");
        player.sendMessage("");
        player.sendMessage("§7Vos progrès ont été partiellement reset:");
        player.sendMessage("§c- Niveau de classe: §f1");
        player.sendMessage("§c- Talents: §fRéinitialisés (" + result.talentPointsRefunded + " points)");
        player.sendMessage("§c- Buffs arcade: §fReset");
        player.sendMessage("");
        player.sendMessage("§aCe qui est conservé:");
        player.sendMessage("§a+ Classe: §f" + data.getSelectedClass().getColoredName());
        player.sendMessage("§a+ Armes débloquées");
        player.sendMessage("§a+ Achievements");
        player.sendMessage("");
        player.sendMessage("§eBonne chance pour cette nouvelle saison!");
        player.sendMessage("");
    }

    /**
     * Effectue le reset partiel des données d'un joueur
     */
    private SeasonResetResult performPartialReset(ClassData data) {
        SeasonResetResult result = new SeasonResetResult();

        // Compter les points de talent à rembourser
        result.talentPointsRefunded = data.getUnlockedTalents().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        // Sauvegarder stats pour l'historique
        result.previousClassLevel = data.getClassLevel().get();
        result.previousBuffCount = data.getTotalBuffCount();

        // === RESET ===
        // Reset niveau de classe à 1
        data.getClassLevel().set(1);

        // Reset talents
        data.resetTalents();

        // Reset buffs arcade
        data.getArcadeBuffs().clear();

        // Reset XP de classe
        data.getClassXp().set(0);

        // Reset statistiques de session
        data.resetSessionStats();

        // === CONSERVATION ===
        // Classe sélectionnée - conservée
        // Armes débloquées - conservées (mais évolution potentiellement reset selon config)
        // Note: L'évolution d'arme dépend de maxZoneCompleted qui peut être reset ou non

        return result;
    }

    /**
     * Reset tous les joueurs connectés
     */
    private void resetAllPlayers() {
        plugin.getServer().getOnlinePlayers().forEach(this::resetPlayerForSeason);
        // Note: Les joueurs offline seront reset à leur connexion
    }

    /**
     * Archive les statistiques de la saison
     */
    private void archiveSeasonStats() {
        // TODO: Sauvegarder en BDD
        seasonStats.clear();
    }

    // ==================== ROTATION DES BUFFS ====================

    /**
     * Fait tourner le pool de buffs disponibles
     * Retire certains buffs et en ajoute de nouveaux
     */
    private void rotateBuffPool() {
        // Exemple de rotation: retirer 20% des buffs les moins utilisés
        // et possiblement ajouter de nouveaux buffs saisonniers

        // Pour l'instant, on garde tous les buffs
        // Cette méthode sera étendue avec les stats d'utilisation

        plugin.getLogger().info("[Saisons] Pool de buffs mis à jour: " + activeBuffPool.size() + " buffs actifs");
    }

    /**
     * Vérifie si un buff est actif dans le pool de la saison
     */
    public boolean isBuffActiveThisSeason(String buffId) {
        return activeBuffPool.contains(buffId);
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

    // ==================== STATISTIQUES SAISONNIÈRES ====================

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
     * Résultat d'un reset de saison
     */
    @Getter
    public static class SeasonResetResult {
        int talentPointsRefunded;
        int previousClassLevel;
        int previousBuffCount;
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
