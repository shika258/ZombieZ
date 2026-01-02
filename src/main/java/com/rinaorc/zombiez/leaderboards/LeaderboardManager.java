package com.rinaorc.zombiez.leaderboards;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gestionnaire complet des Leaderboards ZombieZ
 * Supporte les périodes ALL_TIME, DAILY, WEEKLY, MONTHLY, SEASONAL
 * Optimisé pour 200+ joueurs avec cache Caffeine
 */
public class LeaderboardManager {

    private final ZombieZPlugin plugin;

    // ═══════════════════════════════════════════════════════════════════════════
    // CACHE ET STOCKAGE
    // ═══════════════════════════════════════════════════════════════════════════

    // Cache principal: (Type, Period) -> Liste triée d'entrées
    private final Cache<String, List<LeaderboardEntry>> leaderboardCache;

    // Cache des rangs des joueurs: UUID -> (Type, Period) -> Rang
    private final Map<UUID, Map<String, Integer>> playerRankCache;

    // Cache des scores des joueurs en ligne (mise à jour temps réel)
    private final Map<UUID, Map<LeaderboardType, Long>> liveScores;

    // Tracking des joueurs flaggés (anti-triche)
    private final Map<UUID, List<AntiCheatFlag>> flaggedPlayers;

    // Saison actuelle
    @Getter
    private SeasonData currentSeason;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    private static final int MAX_ENTRIES = 100;
    private static final long CACHE_DURATION_MINUTES = 5;
    private static final long UPDATE_INTERVAL_TICKS = 60 * 20L; // 1 minute
    private static final long DAILY_CHECK_INTERVAL_TICKS = 20 * 60 * 20L; // 20 minutes

    // Seuils anti-triche
    private static final long MAX_KILLS_PER_HOUR = 3000;
    private static final long MAX_XP_PER_HOUR = 500000;
    private static final double SUSPICIOUS_PROGRESS_MULTIPLIER = 3.0;

    @Getter
    private long lastUpdate = 0;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTEUR
    // ═══════════════════════════════════════════════════════════════════════════

    public LeaderboardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Initialiser le cache Caffeine
        this.leaderboardCache = Caffeine.newBuilder()
            .maximumSize(200) // Type × Period combinations
            .expireAfterWrite(CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
            .build();

        this.playerRankCache = new ConcurrentHashMap<>();
        this.liveScores = new ConcurrentHashMap<>();
        this.flaggedPlayers = new ConcurrentHashMap<>();

        // Créer les tables de manière synchrone pour garantir qu'elles existent
        createTablesSync();

        // Charger la saison actuelle
        loadCurrentSeason();

        // Démarrer les tâches
        startUpdateTask();
        startPeriodCheckTask();
        startAntiCheatTask();

        plugin.log(Level.INFO, "§a✓ LeaderboardManager initialisé avec " + LeaderboardType.values().length + " types");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CRÉATION DES TABLES SQL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Crée les tables de manière synchrone pour garantir qu'elles existent avant les requêtes
     */
    private void createTablesSync() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean isMySQL = plugin.getDatabaseManager().getDatabaseType() ==
                com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.MYSQL;

                // Table principale des leaderboards
                String leaderboardsSQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_leaderboards (
                        uuid VARCHAR(36) NOT NULL,
                        leaderboard_type VARCHAR(50) NOT NULL,
                        period VARCHAR(20) NOT NULL,
                        value BIGINT DEFAULT 0,
                        rank_position INT DEFAULT 0,
                        last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (uuid, leaderboard_type, period)
                    )
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(leaderboardsSQL)) {
                    stmt.executeUpdate();
                }

                // Index pour les requêtes de classement
                if (isMySQL) {
                    executeIgnoreError(conn,
                        "CREATE INDEX idx_lb_type_period_value ON zombiez_leaderboards(leaderboard_type, period, value DESC)");
                    executeIgnoreError(conn,
                        "CREATE INDEX idx_lb_rank ON zombiez_leaderboards(leaderboard_type, period, rank_position)");
                }

                // Table d'historique des périodes
                String historySQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_leaderboard_history (
                        id BIGINT %s PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        leaderboard_type VARCHAR(50) NOT NULL,
                        period VARCHAR(20) NOT NULL,
                        period_start DATETIME NOT NULL,
                        period_end DATETIME NOT NULL,
                        final_value BIGINT,
                        final_rank INT,
                        rewards_claimed BOOLEAN DEFAULT FALSE
                    )
                    """.formatted(isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT");

                try (PreparedStatement stmt = conn.prepareStatement(historySQL)) {
                    stmt.executeUpdate();
                }

                // Table des récompenses
                String rewardsSQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_leaderboard_rewards (
                        id BIGINT %s PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        leaderboard_type VARCHAR(50) NOT NULL,
                        period VARCHAR(20),
                        rank_achieved INT,
                        reward_points BIGINT DEFAULT 0,
                        reward_gems INT DEFAULT 0,
                        reward_title VARCHAR(100),
                        reward_cosmetic VARCHAR(100),
                        claimed BOOLEAN DEFAULT FALSE,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        claimed_at DATETIME
                    )
                    """.formatted(isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT");

                try (PreparedStatement stmt = conn.prepareStatement(rewardsSQL)) {
                    stmt.executeUpdate();
                }

                // Table des saisons
                String seasonsSQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_seasons (
                        season_id INT PRIMARY KEY,
                        season_name VARCHAR(100) NOT NULL,
                        start_date DATETIME NOT NULL,
                        end_date DATETIME NOT NULL,
                        is_active BOOLEAN DEFAULT FALSE
                    )
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(seasonsSQL)) {
                    stmt.executeUpdate();
                }

                // Table anti-triche
                String antiCheatSQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_leaderboard_flags (
                        id BIGINT %s PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        flag_type VARCHAR(50) NOT NULL,
                        flag_reason TEXT,
                        flag_value BIGINT,
                        expected_max BIGINT,
                        flagged_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        resolved BOOLEAN DEFAULT FALSE,
                        resolved_by VARCHAR(36),
                        resolved_at DATETIME
                    )
                    """.formatted(isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT");

                try (PreparedStatement stmt = conn.prepareStatement(antiCheatSQL)) {
                    stmt.executeUpdate();
                }

                // Table des joueurs bannis des leaderboards
                String bannedSQL = """
                    CREATE TABLE IF NOT EXISTS zombiez_leaderboard_banned (
                        uuid VARCHAR(36) PRIMARY KEY,
                        banned_by VARCHAR(36),
                        ban_reason TEXT,
                        banned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        expires_at DATETIME
                    )
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(bannedSQL)) {
                    stmt.executeUpdate();
                }

            plugin.log(Level.INFO, "§a✓ Tables leaderboards créées");

        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§c✗ Erreur création tables leaderboards: " + e.getMessage());
        }
    }

    private void executeIgnoreError(Connection conn, String sql) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // Index existe déjà probablement
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GESTION DES SAISONS
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadCurrentSeason() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "SELECT * FROM zombiez_seasons WHERE is_active = TRUE LIMIT 1";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    if (rs.next()) {
                        currentSeason = new SeasonData(
                            rs.getInt("season_id"),
                            rs.getString("season_name"),
                            rs.getTimestamp("start_date").toInstant(),
                            rs.getTimestamp("end_date").toInstant()
                        );
                    } else {
                        // Créer une nouvelle saison
                        createNewSeason();
                    }
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur chargement saison: " + e.getMessage());
                // Créer une saison par défaut
                createNewSeason();
            }
        });
    }

    private void createNewSeason() {
        Instant now = Instant.now();
        Instant endDate = now.plus(30, java.time.temporal.ChronoUnit.DAYS);
        int seasonId = (int) (now.getEpochSecond() / (30 * 24 * 3600)); // Numéro unique

        currentSeason = new SeasonData(seasonId, "Saison " + seasonId, now, endDate);

        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Désactiver les anciennes saisons
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE zombiez_seasons SET is_active = FALSE")) {
                    stmt.executeUpdate();
                }

                // Insérer la nouvelle saison
                String sql = "INSERT INTO zombiez_seasons (season_id, season_name, start_date, end_date, is_active) VALUES (?, ?, ?, ?, TRUE)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, seasonId);
                    stmt.setString(2, currentSeason.getName());
                    stmt.setTimestamp(3, Timestamp.from(now));
                    stmt.setTimestamp(4, Timestamp.from(endDate));
                    stmt.executeUpdate();
                }

                plugin.log(Level.INFO, "§a✓ Nouvelle saison créée: " + currentSeason.getName());

            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur création saison: " + e.getMessage());
            }
        });
    }

    /**
     * Vérifie si la saison actuelle est terminée et crée une nouvelle si nécessaire
     */
    public void checkSeasonEnd() {
        if (currentSeason == null) return;

        if (Instant.now().isAfter(currentSeason.getEndDate())) {
            plugin.log(Level.INFO, "§6Fin de la " + currentSeason.getName() + " - Distribution des récompenses...");

            // Distribuer les récompenses de fin de saison
            distributeSeasonRewards();

            // Archiver et créer nouvelle saison
            archiveSeasonData();
            createNewSeason();

            // Reset les leaderboards saisonniers
            resetPeriodLeaderboards(LeaderboardPeriod.SEASONAL);
        }
    }

    private void distributeSeasonRewards() {
        // Pour chaque type de leaderboard, distribuer les récompenses aux top joueurs
        for (LeaderboardType type : LeaderboardType.values()) {
            List<LeaderboardEntry> top = getTopEntries(type, LeaderboardPeriod.SEASONAL, 100);

            for (LeaderboardEntry entry : top) {
                LeaderboardReward reward = LeaderboardReward.calculateReward(type, LeaderboardPeriod.SEASONAL, entry.getRank());
                if (reward != null) {
                    saveReward(entry.getUuid(), type, LeaderboardPeriod.SEASONAL, entry.getRank(), reward);
                }
            }
        }
    }

    private void archiveSeasonData() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Copier les données saisonnières vers l'historique
                String sql = """
                    INSERT INTO zombiez_leaderboard_history
                    (uuid, leaderboard_type, period, period_start, period_end, final_value, final_rank)
                    SELECT uuid, leaderboard_type, period, ?, ?, value, rank_position
                    FROM zombiez_leaderboards
                    WHERE period = 'SEASONAL'
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setTimestamp(1, Timestamp.from(currentSeason.getStartDate()));
                    stmt.setTimestamp(2, Timestamp.from(currentSeason.getEndDate()));
                    stmt.executeUpdate();
                }

            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur archivage saison: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TÂCHES PÉRIODIQUES
    // ═══════════════════════════════════════════════════════════════════════════

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshAllLeaderboards();
            }
        }.runTaskTimerAsynchronously(plugin, 100L, UPDATE_INTERVAL_TICKS);
    }

    private void startPeriodCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndResetPeriods();
                checkSeasonEnd();
            }
        }.runTaskTimerAsynchronously(plugin, 200L, DAILY_CHECK_INTERVAL_TICKS);
    }

    private void startAntiCheatTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                runAntiCheatChecks();
            }
        }.runTaskTimerAsynchronously(plugin, 300L, 20 * 60 * 5L); // Toutes les 5 minutes
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RAFRAÎCHISSEMENT DES LEADERBOARDS
    // ═══════════════════════════════════════════════════════════════════════════

    public void refreshAllLeaderboards() {
        for (LeaderboardType type : LeaderboardType.values()) {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                refreshLeaderboard(type, period);
            }
        }
        lastUpdate = System.currentTimeMillis();

        // Mettre à jour les rangs des joueurs en ligne
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerRanks(player.getUniqueId());
        }
    }

    private void refreshLeaderboard(LeaderboardType type, LeaderboardPeriod period) {
        List<LeaderboardEntry> entries = fetchFromDatabase(type, period);
        String cacheKey = type.name() + "_" + period.name();
        leaderboardCache.put(cacheKey, entries);
    }

    private List<LeaderboardEntry> fetchFromDatabase(LeaderboardType type, LeaderboardPeriod period) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql;
            boolean isSQLite = plugin.getDatabaseManager().getDatabaseType() ==
                    com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE;
            String nowFunc = isSQLite ? "datetime('now')" : "NOW()";

            if (period == LeaderboardPeriod.ALL_TIME) {
                // Requête directe sur la table principale
                if (type.isDetailedStat()) {
                    sql = """
                        SELECT p.uuid, p.name, COALESCE(s.stat_value, 0) as value
                        FROM zombiez_players p
                        LEFT JOIN zombiez_stats s ON p.uuid = s.uuid AND s.stat_key = ?
                        WHERE p.uuid NOT IN (SELECT uuid FROM zombiez_leaderboard_banned WHERE expires_at IS NULL OR expires_at > %s)
                        ORDER BY value DESC
                        LIMIT ?
                        """.formatted(nowFunc);
                } else {
                    sql = """
                        SELECT uuid, name, %s as value
                        FROM zombiez_players
                        WHERE uuid NOT IN (SELECT uuid FROM zombiez_leaderboard_banned WHERE expires_at IS NULL OR expires_at > %s)
                        ORDER BY %s DESC
                        LIMIT ?
                        """.formatted(type.getColumn(), nowFunc, type.getColumn());
                }
            } else {
                // Requête sur la table des leaderboards périodiques
                sql = """
                    SELECT l.uuid, p.name, l.value
                    FROM zombiez_leaderboards l
                    JOIN zombiez_players p ON l.uuid = p.uuid
                    WHERE l.leaderboard_type = ? AND l.period = ?
                    AND l.uuid NOT IN (SELECT uuid FROM zombiez_leaderboard_banned WHERE expires_at IS NULL OR expires_at > %s)
                    ORDER BY l.value DESC
                    LIMIT ?
                    """.formatted(nowFunc);
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramIndex = 1;

                if (period == LeaderboardPeriod.ALL_TIME && type.isDetailedStat()) {
                    stmt.setString(paramIndex++, type.getColumn());
                } else if (period != LeaderboardPeriod.ALL_TIME) {
                    stmt.setString(paramIndex++, type.name());
                    stmt.setString(paramIndex++, period.name());
                }

                stmt.setInt(paramIndex, MAX_ENTRIES);
                ResultSet rs = stmt.executeQuery();

                int rank = 1;
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    long value = rs.getLong("value");

                    entries.add(new LeaderboardEntry(uuid, name, value, rank++));
                }
            }

        } catch (SQLException e) {
            plugin.log(Level.WARNING, "§e⚠ Erreur chargement leaderboard " + type + "/" + period + ": " + e.getMessage());
        }

        return entries;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACCÈS AUX DONNÉES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtient le top X d'un leaderboard
     */
    public List<LeaderboardEntry> getTopEntries(LeaderboardType type, LeaderboardPeriod period, int count) {
        String cacheKey = type.name() + "_" + period.name();
        List<LeaderboardEntry> cached = leaderboardCache.getIfPresent(cacheKey);

        if (cached == null) {
            cached = fetchFromDatabase(type, period);
            leaderboardCache.put(cacheKey, cached);
        }

        return cached.subList(0, Math.min(count, cached.size()));
    }

    /**
     * Alias pour compatibilité
     */
    public List<LeaderboardEntry> getTop(LeaderboardType type, int count) {
        return getTopEntries(type, LeaderboardPeriod.ALL_TIME, count);
    }

    /**
     * Obtient le rang d'un joueur (depuis le cache)
     */
    public int getPlayerRank(UUID uuid, LeaderboardType type, LeaderboardPeriod period) {
        Map<String, Integer> ranks = playerRankCache.get(uuid);
        if (ranks == null) return -1;
        return ranks.getOrDefault(type.name() + "_" + period.name(), -1);
    }

    /**
     * Alias pour ALL_TIME
     */
    public int getPlayerRank(UUID uuid, LeaderboardType type) {
        return getPlayerRank(uuid, type, LeaderboardPeriod.ALL_TIME);
    }

    /**
     * Met à jour le cache des rangs d'un joueur
     */
    public void updatePlayerRanks(UUID uuid) {
        Map<String, Integer> ranks = new HashMap<>();

        for (LeaderboardType type : LeaderboardType.values()) {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                String cacheKey = type.name() + "_" + period.name();
                List<LeaderboardEntry> entries = leaderboardCache.getIfPresent(cacheKey);

                if (entries != null) {
                    for (int i = 0; i < entries.size(); i++) {
                        if (entries.get(i).getUuid().equals(uuid)) {
                            ranks.put(cacheKey, i + 1);
                            break;
                        }
                    }
                }
            }
        }

        playerRankCache.put(uuid, ranks);
    }

    /**
     * Obtient tous les classements d'un joueur
     */
    public Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> getAllPlayerRanks(UUID uuid) {
        Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> result = new EnumMap<>(LeaderboardType.class);

        for (LeaderboardType type : LeaderboardType.values()) {
            Map<LeaderboardPeriod, Integer> periodRanks = new EnumMap<>(LeaderboardPeriod.class);
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                int rank = getPlayerRank(uuid, type, period);
                if (rank > 0) {
                    periodRanks.put(period, rank);
                }
            }
            if (!periodRanks.isEmpty()) {
                result.put(type, periodRanks);
            }
        }

        return result;
    }

    /**
     * Trouve le meilleur classement d'un joueur
     */
    public Map.Entry<LeaderboardType, Integer> getBestRank(UUID uuid) {
        Map<String, Integer> ranks = playerRankCache.get(uuid);
        if (ranks == null || ranks.isEmpty()) return null;

        String bestKey = null;
        int bestRank = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            if (entry.getValue() < bestRank) {
                bestRank = entry.getValue();
                bestKey = entry.getKey();
            }
        }

        if (bestKey == null) return null;

        // Extraire le type du cache key
        String typeName = bestKey.split("_")[0];
        try {
            LeaderboardType type = LeaderboardType.valueOf(typeName);
            return Map.entry(type, bestRank);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MISE À JOUR DES SCORES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Met à jour le score d'un joueur pour un type donné
     * Appelé lors des événements (kills, achievements, etc.)
     */
    public void updateScore(UUID uuid, LeaderboardType type, long newValue) {
        // Mettre à jour les scores en temps réel
        liveScores.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(type, newValue);

        // Mettre à jour en base de données pour les périodes
        CompletableFuture.runAsync(() -> {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                if (period == LeaderboardPeriod.ALL_TIME) continue; // Géré par PlayerData directement

                updatePeriodScore(uuid, type, period, newValue);
            }
        });
    }

    /**
     * Incrémente le score d'un joueur
     */
    public void incrementScore(UUID uuid, LeaderboardType type, long amount) {
        Map<LeaderboardType, Long> scores = liveScores.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        long newValue = scores.getOrDefault(type, 0L) + amount;
        scores.put(type, newValue);

        // Mettre à jour en BDD
        CompletableFuture.runAsync(() -> {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                if (period == LeaderboardPeriod.ALL_TIME) continue;

                incrementPeriodScore(uuid, type, period, amount);
            }
        });
    }

    private void updatePeriodScore(UUID uuid, LeaderboardType type, LeaderboardPeriod period, long value) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = """
                INSERT INTO zombiez_leaderboards (uuid, leaderboard_type, period, value, last_updated)
                VALUES (?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE value = ?, last_updated = NOW()
                """;

            // SQLite version
            if (plugin.getDatabaseManager().getDatabaseType() ==
                    com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE) {
                sql = """
                    INSERT OR REPLACE INTO zombiez_leaderboards (uuid, leaderboard_type, period, value, last_updated)
                    VALUES (?, ?, ?, ?, datetime('now'))
                    """;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, type.name());
                stmt.setString(3, period.name());
                stmt.setLong(4, value);

                if (plugin.getDatabaseManager().getDatabaseType() !=
                        com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE) {
                    stmt.setLong(5, value);
                }

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "§e⚠ Erreur MAJ score période: " + e.getMessage());
        }
    }

    private void incrementPeriodScore(UUID uuid, LeaderboardType type, LeaderboardPeriod period, long amount) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql;
            if (plugin.getDatabaseManager().getDatabaseType() ==
                    com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.MYSQL) {
                sql = """
                    INSERT INTO zombiez_leaderboards (uuid, leaderboard_type, period, value, last_updated)
                    VALUES (?, ?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE value = value + ?, last_updated = NOW()
                    """;
            } else {
                sql = """
                    INSERT INTO zombiez_leaderboards (uuid, leaderboard_type, period, value, last_updated)
                    VALUES (?, ?, ?, ?, datetime('now'))
                    ON CONFLICT(uuid, leaderboard_type, period) DO UPDATE SET
                    value = value + ?, last_updated = datetime('now')
                    """;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, type.name());
                stmt.setString(3, period.name());
                stmt.setLong(4, amount);
                stmt.setLong(5, amount);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "§e⚠ Erreur increment score: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESET DES PÉRIODES
    // ═══════════════════════════════════════════════════════════════════════════

    private void checkAndResetPeriods() {
        LocalDateTime now = LocalDateTime.now();

        // Reset quotidien à minuit
        if (now.getHour() == 0 && now.getMinute() < 20) {
            resetPeriodLeaderboards(LeaderboardPeriod.DAILY);
        }

        // Reset hebdomadaire le lundi
        if (now.getDayOfWeek() == java.time.DayOfWeek.MONDAY &&
            now.getHour() == 0 && now.getMinute() < 20) {
            resetPeriodLeaderboards(LeaderboardPeriod.WEEKLY);
        }

        // Reset mensuel le 1er du mois
        if (now.getDayOfMonth() == 1 && now.getHour() == 0 && now.getMinute() < 20) {
            resetPeriodLeaderboards(LeaderboardPeriod.MONTHLY);
        }
    }

    public void resetPeriodLeaderboards(LeaderboardPeriod period) {
        plugin.log(Level.INFO, "§6Reset des leaderboards " + period.getDisplayName());

        // D'abord, distribuer les récompenses
        distributeRewardsForPeriod(period);

        // Ensuite, archiver les données
        archivePeriodData(period);

        // Enfin, reset les scores
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "DELETE FROM zombiez_leaderboards WHERE period = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, period.name());
                    int deleted = stmt.executeUpdate();
                    plugin.log(Level.INFO, "§a✓ " + deleted + " entrées supprimées pour " + period.name());
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur reset période: " + e.getMessage());
            }
        });

        // Vider le cache
        for (LeaderboardType type : LeaderboardType.values()) {
            String cacheKey = type.name() + "_" + period.name();
            leaderboardCache.invalidate(cacheKey);
        }
    }

    private void distributeRewardsForPeriod(LeaderboardPeriod period) {
        for (LeaderboardType type : LeaderboardType.values()) {
            List<LeaderboardEntry> top = getTopEntries(type, period, 100);

            for (LeaderboardEntry entry : top) {
                LeaderboardReward reward = LeaderboardReward.calculateReward(type, period, entry.getRank());
                if (reward != null) {
                    saveReward(entry.getUuid(), type, period, entry.getRank(), reward);
                }
            }
        }
    }

    private void archivePeriodData(LeaderboardPeriod period) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                Instant now = Instant.now();
                Instant start = switch (period) {
                    case DAILY -> now.minus(1, java.time.temporal.ChronoUnit.DAYS);
                    case WEEKLY -> now.minus(7, java.time.temporal.ChronoUnit.DAYS);
                    case MONTHLY -> now.minus(30, java.time.temporal.ChronoUnit.DAYS);
                    default -> now;
                };

                String sql = """
                    INSERT INTO zombiez_leaderboard_history
                    (uuid, leaderboard_type, period, period_start, period_end, final_value, final_rank)
                    SELECT uuid, leaderboard_type, period, ?, ?, value, rank_position
                    FROM zombiez_leaderboards
                    WHERE period = ?
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setTimestamp(1, Timestamp.from(start));
                    stmt.setTimestamp(2, Timestamp.from(now));
                    stmt.setString(3, period.name());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur archivage période: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE RÉCOMPENSES
    // ═══════════════════════════════════════════════════════════════════════════

    private void saveReward(UUID uuid, LeaderboardType type, LeaderboardPeriod period, int rank, LeaderboardReward reward) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = """
                    INSERT INTO zombiez_leaderboard_rewards
                    (uuid, leaderboard_type, period, rank_achieved, reward_points, reward_gems, reward_title, reward_cosmetic)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, type.name());
                    stmt.setString(3, period.name());
                    stmt.setInt(4, rank);
                    stmt.setLong(5, reward.getPoints());
                    stmt.setInt(6, reward.getGems());
                    stmt.setString(7, reward.getTitle());
                    stmt.setString(8, reward.getCosmetic());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur sauvegarde récompense: " + e.getMessage());
            }
        });
    }

    /**
     * Obtient les récompenses non réclamées d'un joueur
     */
    public CompletableFuture<List<PendingReward>> getPendingRewards(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<PendingReward> rewards = new ArrayList<>();

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = """
                    SELECT * FROM zombiez_leaderboard_rewards
                    WHERE uuid = ? AND claimed = FALSE
                    ORDER BY created_at DESC
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        rewards.add(new PendingReward(
                            rs.getLong("id"),
                            LeaderboardType.valueOf(rs.getString("leaderboard_type")),
                            LeaderboardPeriod.valueOf(rs.getString("period")),
                            rs.getInt("rank_achieved"),
                            rs.getLong("reward_points"),
                            rs.getInt("reward_gems"),
                            rs.getString("reward_title"),
                            rs.getString("reward_cosmetic")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur récupération récompenses: " + e.getMessage());
            }

            return rewards;
        });
    }

    /**
     * Réclame une récompense
     */
    public CompletableFuture<Boolean> claimReward(UUID uuid, long rewardId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Récupérer la récompense
                String selectSql = "SELECT * FROM zombiez_leaderboard_rewards WHERE id = ? AND uuid = ? AND claimed = FALSE";

                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setLong(1, rewardId);
                    stmt.setString(2, uuid.toString());
                    ResultSet rs = stmt.executeQuery();

                    if (!rs.next()) return false;

                    long points = rs.getLong("reward_points");
                    int gems = rs.getInt("reward_gems");
                    String title = rs.getString("reward_title");
                    String cosmetic = rs.getString("reward_cosmetic");

                    // Marquer comme réclamée
                    boolean isSQLite = plugin.getDatabaseManager().getDatabaseType() ==
                            com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE;
                    String updateSql = isSQLite
                            ? "UPDATE zombiez_leaderboard_rewards SET claimed = TRUE, claimed_at = datetime('now') WHERE id = ?"
                            : "UPDATE zombiez_leaderboard_rewards SET claimed = TRUE, claimed_at = NOW() WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, rewardId);
                        updateStmt.executeUpdate();
                    }

                    // Appliquer les récompenses
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
                        if (data != null) {
                            if (points > 0) data.addPoints(points);
                            if (gems > 0) data.addGems(gems);
                            if (title != null && !title.isEmpty()) {
                                data.getUnlockedTitles().add(title);
                            }
                            if (cosmetic != null && !cosmetic.isEmpty()) {
                                data.getUnlockedCosmetics().add(cosmetic);
                            }
                        }

                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage("§a§l✓ §aRécompense réclamée!");
                            if (points > 0) player.sendMessage("  §e+" + points + " points");
                            if (gems > 0) player.sendMessage("  §d+" + gems + " gemmes");
                            if (title != null && !title.isEmpty()) player.sendMessage("  §6+Titre: " + title);
                            if (cosmetic != null && !cosmetic.isEmpty()) player.sendMessage("  §b+Cosmétique: " + cosmetic);
                        }
                    });

                    return true;
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur claim récompense: " + e.getMessage());
                return false;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANTI-TRICHE
    // ═══════════════════════════════════════════════════════════════════════════

    private void runAntiCheatChecks() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
            if (data == null) continue;

            // Vérifier les progressions anormales
            checkAbnormalProgress(uuid, data);
        }
    }

    private void checkAbnormalProgress(UUID uuid, PlayerData data) {
        Map<LeaderboardType, Long> currentScores = liveScores.get(uuid);
        if (currentScores == null) return;

        // Vérifier kills par heure
        long sessionKills = data.getSessionKills().get();
        long sessionSeconds = java.time.Duration.between(data.getSessionStart(), Instant.now()).getSeconds();
        if (sessionSeconds > 0) {
            double killsPerHour = (sessionKills * 3600.0) / sessionSeconds;
            if (killsPerHour > MAX_KILLS_PER_HOUR) {
                flagPlayer(uuid, "EXCESSIVE_KILLS",
                    "Kills/heure: " + String.format("%.0f", killsPerHour),
                    (long) killsPerHour, MAX_KILLS_PER_HOUR);
            }
        }

        // Autres vérifications...
    }

    private void flagPlayer(UUID uuid, String flagType, String reason, long value, long expectedMax) {
        AntiCheatFlag flag = new AntiCheatFlag(flagType, reason, value, expectedMax);
        flaggedPlayers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(flag);

        // Sauvegarder en BDD
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = """
                    INSERT INTO zombiez_leaderboard_flags
                    (uuid, flag_type, flag_reason, flag_value, expected_max)
                    VALUES (?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, flagType);
                    stmt.setString(3, reason);
                    stmt.setLong(4, value);
                    stmt.setLong(5, expectedMax);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur flag anti-triche: " + e.getMessage());
            }
        });

        plugin.log(Level.WARNING, "§e⚠ ANTI-CHEAT: " + Bukkit.getOfflinePlayer(uuid).getName() +
            " flaggé pour " + flagType + ": " + reason);
    }

    /**
     * Vérifie si un joueur est flaggé
     */
    public boolean isPlayerFlagged(UUID uuid) {
        List<AntiCheatFlag> flags = flaggedPlayers.get(uuid);
        return flags != null && !flags.isEmpty();
    }

    /**
     * Obtient les flags d'un joueur
     */
    public List<AntiCheatFlag> getPlayerFlags(UUID uuid) {
        return flaggedPlayers.getOrDefault(uuid, Collections.emptyList());
    }

    /**
     * Bannit un joueur des leaderboards
     */
    public CompletableFuture<Void> banFromLeaderboards(UUID uuid, UUID bannedBy, String reason) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = """
                    INSERT INTO zombiez_leaderboard_banned (uuid, banned_by, ban_reason)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE banned_by = ?, ban_reason = ?, banned_at = NOW()
                    """;

                if (plugin.getDatabaseManager().getDatabaseType() ==
                        com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE) {
                    sql = """
                        INSERT OR REPLACE INTO zombiez_leaderboard_banned (uuid, banned_by, ban_reason, banned_at)
                        VALUES (?, ?, ?, datetime('now'))
                        """;
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, bannedBy != null ? bannedBy.toString() : null);
                    stmt.setString(3, reason);

                    if (plugin.getDatabaseManager().getDatabaseType() !=
                            com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE) {
                        stmt.setString(4, bannedBy != null ? bannedBy.toString() : null);
                        stmt.setString(5, reason);
                    }

                    stmt.executeUpdate();
                }

                plugin.log(Level.INFO, "§c✓ " + Bukkit.getOfflinePlayer(uuid).getName() + " banni des leaderboards: " + reason);

            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur ban leaderboard: " + e.getMessage());
            }
        });
    }

    /**
     * Débannit un joueur
     */
    public CompletableFuture<Void> unbanFromLeaderboards(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "DELETE FROM zombiez_leaderboard_banned WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur unban leaderboard: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Force le rafraîchissement d'un leaderboard
     */
    public void forceRefresh(LeaderboardType type, LeaderboardPeriod period) {
        refreshLeaderboard(type, period);
    }

    /**
     * Force le rafraîchissement de tous les leaderboards
     */
    public void forceRefreshAll() {
        CompletableFuture.runAsync(this::refreshAllLeaderboards);
    }

    /**
     * Obtient les statistiques du système
     */
    public String getStats() {
        int cachedEntries = 0;
        for (LeaderboardType type : LeaderboardType.values()) {
            for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                String key = type.name() + "_" + period.name();
                List<LeaderboardEntry> entries = leaderboardCache.getIfPresent(key);
                if (entries != null) cachedEntries += entries.size();
            }
        }

        return String.format("§7Leaderboards: §e%d types §7| §e%d périodes §7| §e%d entrées cachées §7| §e%d joueurs flaggés",
            LeaderboardType.values().length,
            LeaderboardPeriod.values().length,
            cachedEntries,
            flaggedPlayers.size());
    }

    /**
     * Arrêt propre du manager
     */
    public void shutdown() {
        leaderboardCache.invalidateAll();
        playerRankCache.clear();
        liveScores.clear();
        plugin.log(Level.INFO, "§7LeaderboardManager arrêté");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTHODES ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Met à jour le score d'un joueur (version avec nom)
     */
    public void updateScore(UUID uuid, String playerName, LeaderboardType type, long value) {
        updateScore(uuid, type, value);
    }

    /**
     * Incrémente le score d'un joueur (version avec nom)
     */
    public void incrementScore(UUID uuid, String playerName, LeaderboardType type, long amount) {
        incrementScore(uuid, type, amount);
    }

    /**
     * Rafraîchit tout le cache (alias pour forceRefreshAll)
     */
    public void refreshAllCache() {
        forceRefreshAll();
    }

    /**
     * Reset un type de leaderboard pour une période donnée
     */
    public void resetPeriod(LeaderboardType type, LeaderboardPeriod period) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "DELETE FROM zombiez_leaderboards WHERE leaderboard_type = ? AND period = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, type.name());
                    stmt.setString(2, period.name());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur reset type/période: " + e.getMessage());
            }
        });

        // Invalider le cache pour ce type/période
        String cacheKey = type.name() + "_" + period.name();
        leaderboardCache.invalidate(cacheKey);
    }

    /**
     * Distribue les récompenses pour un type et une période
     */
    public void distributeRewards(LeaderboardType type, LeaderboardPeriod period) {
        List<LeaderboardEntry> top = getTopEntries(type, period, 100);

        for (LeaderboardEntry entry : top) {
            LeaderboardReward reward = LeaderboardReward.calculateReward(type, period, entry.getRank());
            if (reward != null && reward.hasContent()) {
                saveReward(entry.getUuid(), type, period, entry.getRank(), reward);
            }
        }
    }

    /**
     * Bannit un joueur des classements
     */
    public void banPlayer(UUID uuid, String reason, String bannedByName) {
        banFromLeaderboards(uuid, null, reason);
    }

    /**
     * Débannit un joueur des classements
     */
    public void unbanPlayer(UUID uuid) {
        unbanFromLeaderboards(uuid);
    }

    /**
     * Flag manuellement un joueur (par admin)
     */
    public void flagPlayer(UUID uuid, String reason) {
        flagPlayer(uuid, "MANUAL_FLAG", reason, 0, 0);
    }

    /**
     * Retire le flag d'un joueur
     */
    public void unflagPlayer(UUID uuid) {
        flaggedPlayers.remove(uuid);

        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                boolean isSQLite = plugin.getDatabaseManager().getDatabaseType() ==
                        com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.SQLITE;
                String sql = isSQLite
                        ? "UPDATE zombiez_leaderboard_flags SET resolved = TRUE, resolved_at = datetime('now') WHERE uuid = ? AND resolved = FALSE"
                        : "UPDATE zombiez_leaderboard_flags SET resolved = TRUE, resolved_at = NOW() WHERE uuid = ? AND resolved = FALSE";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur unflag: " + e.getMessage());
            }
        });
    }

    /**
     * Retourne l'ensemble des UUIDs des joueurs flaggés
     */
    public Set<UUID> getFlaggedPlayers() {
        return new HashSet<>(flaggedPlayers.keySet());
    }

    /**
     * Retourne la taille du cache
     */
    public int getCacheSize() {
        return (int) leaderboardCache.estimatedSize();
    }

    /**
     * Démarre une nouvelle saison manuellement
     */
    public void startNewSeason(String name) {
        // Terminer l'ancienne saison si elle existe
        if (currentSeason != null) {
            endCurrentSeason();
        }

        // Créer la nouvelle saison
        Instant now = Instant.now();
        Instant endDate = now.plus(30, java.time.temporal.ChronoUnit.DAYS);
        int seasonId = (int) (now.getEpochSecond() / (30 * 24 * 3600));

        currentSeason = new SeasonData(seasonId, name, now, endDate);

        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Désactiver les anciennes saisons
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE zombiez_seasons SET is_active = FALSE")) {
                    stmt.executeUpdate();
                }

                // Insérer la nouvelle saison
                String sql = "INSERT INTO zombiez_seasons (season_id, season_name, start_date, end_date, is_active) VALUES (?, ?, ?, ?, TRUE)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, seasonId);
                    stmt.setString(2, name);
                    stmt.setTimestamp(3, Timestamp.from(now));
                    stmt.setTimestamp(4, Timestamp.from(endDate));
                    stmt.executeUpdate();
                }

                plugin.log(Level.INFO, "§a✓ Nouvelle saison démarrée: " + name);

            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur création saison: " + e.getMessage());
            }
        });

        // Reset les leaderboards saisonniers
        resetPeriodLeaderboards(LeaderboardPeriod.SEASONAL);
    }

    /**
     * Termine la saison actuelle et distribue les récompenses
     */
    public void endCurrentSeason() {
        if (currentSeason == null) return;

        plugin.log(Level.INFO, "§6Fin de la " + currentSeason.getName() + " - Distribution des récompenses...");

        // Distribuer les récompenses
        distributeSeasonRewards();

        // Archiver les données
        archiveSeasonData();

        // Marquer comme terminée en BDD
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = "UPDATE zombiez_seasons SET is_active = FALSE WHERE season_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, currentSeason.getId());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§e⚠ Erreur fin saison: " + e.getMessage());
            }
        });

        currentSeason = null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════════

    @Getter
    public static class SeasonData {
        private final int id;
        private final String name;
        private final Instant startDate;
        private final Instant endDate;

        public SeasonData(int id, String name, Instant startDate, Instant endDate) {
            this.id = id;
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public long getDaysRemaining() {
            return java.time.Duration.between(Instant.now(), endDate).toDays();
        }
    }

    @Getter
    public static class AntiCheatFlag {
        private final String type;
        private final String reason;
        private final long value;
        private final long expectedMax;
        private final Instant flaggedAt;

        public AntiCheatFlag(String type, String reason, long value, long expectedMax) {
            this.type = type;
            this.reason = reason;
            this.value = value;
            this.expectedMax = expectedMax;
            this.flaggedAt = Instant.now();
        }
    }

    @Getter
    public static class PendingReward {
        private final long id;
        private final LeaderboardType type;
        private final LeaderboardPeriod period;
        private final int rank;
        private final long points;
        private final int gems;
        private final String title;
        private final String cosmetic;

        public PendingReward(long id, LeaderboardType type, LeaderboardPeriod period, int rank,
                           long points, int gems, String title, String cosmetic) {
            this.id = id;
            this.type = type;
            this.period = period;
            this.rank = rank;
            this.points = points;
            this.gems = gems;
            this.title = title;
            this.cosmetic = cosmetic;
        }
    }
}
