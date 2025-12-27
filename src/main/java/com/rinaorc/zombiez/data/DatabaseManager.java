package com.rinaorc.zombiez.data;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Gestionnaire de base de données optimisé avec HikariCP
 * Support MySQL et SQLite pour flexibilité
 * Conçu pour 200+ joueurs simultanés
 */
public class DatabaseManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private HikariDataSource dataSource;
    
    @Getter
    private DatabaseType databaseType;
    
    private String tablePrefix;

    public enum DatabaseType {
        MYSQL,
        SQLITE
    }

    public DatabaseManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise la connexion à la base de données
     */
    public void initialize() throws Exception {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite").toLowerCase();

        if (type.equals("mysql")) {
            initializeMySQL(config);
        } else {
            initializeSQLite();
        }

        tablePrefix = config.getString("database.table-prefix", "zombiez_");
        
        // Test de connexion
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                plugin.log(Level.INFO, "§a✓ Connexion BDD établie (" + databaseType + ")");
            }
        }
    }

    /**
     * Initialise une connexion MySQL avec pool optimisé
     */
    private void initializeMySQL(FileConfiguration config) {
        databaseType = DatabaseType.MYSQL;

        HikariConfig hikariConfig = new HikariConfig();
        
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "zombiez");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
            "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Optimisations pour 200+ joueurs
        configurePoolForHighLoad(hikariConfig);

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Initialise une connexion SQLite
     */
    private void initializeSQLite() {
        databaseType = DatabaseType.SQLITE;

        File dbFile = new File(plugin.getDataFolder(), "database.db");
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");

        // SQLite a des limitations sur les connexions simultanées
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Configure le pool de connexions pour haute charge
     */
    private void configurePoolForHighLoad(HikariConfig config) {
        // Pool sizing pour 200 joueurs
        // Règle: connections = (core_count * 2) + spindle_count
        // Pour un serveur typique: 20-30 connexions
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        
        // Timeouts
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30));
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(10));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        
        // Validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));

        // Optimisations MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Pool name pour monitoring
        config.setPoolName("ZombieZ-HikariPool");
    }

    /**
     * Obtient une connexion depuis le pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource non initialisé!");
        }
        return dataSource.getConnection();
    }

    /**
     * Crée toutes les tables nécessaires
     */
    public void createTables() {
        executeAsync(this::createTablesSync);
    }

    /**
     * Création synchrone des tables
     */
    private void createTablesSync() {
        try (Connection conn = getConnection()) {
            // Syntaxe adaptée selon le type de BDD
            boolean isMySQL = databaseType == DatabaseType.MYSQL;
            String autoIncrement = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
            String jsonType = isMySQL ? "JSON" : "TEXT";
            
            // Table des joueurs
            String playersSQL;
            if (isMySQL) {
                playersSQL = """
                    CREATE TABLE IF NOT EXISTS %splayers (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        level INT DEFAULT 1,
                        xp BIGINT DEFAULT 0,
                        prestige INT DEFAULT 0,
                        points BIGINT DEFAULT 0,
                        gems INT DEFAULT 0,
                        kills BIGINT DEFAULT 0,
                        deaths INT DEFAULT 0,
                        playtime BIGINT DEFAULT 0,
                        current_zone INT DEFAULT 1,
                        max_zone INT DEFAULT 1,
                        current_checkpoint INT DEFAULT 0,
                        achievement_count INT DEFAULT 0,
                        boss_kills BIGINT DEFAULT 0,
                        best_kill_streak INT DEFAULT 0,
                        vip_rank VARCHAR(32) DEFAULT 'FREE',
                        vip_expiry DATETIME NULL,
                        first_join DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_login DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_logout DATETIME NULL,
                        INDEX idx_name (name),
                        INDEX idx_level (level),
                        INDEX idx_points (points)
                    )
                    """.formatted(tablePrefix);
            } else {
                // SQLite - pas d'INDEX dans CREATE TABLE
                playersSQL = """
                    CREATE TABLE IF NOT EXISTS %splayers (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        level INT DEFAULT 1,
                        xp BIGINT DEFAULT 0,
                        prestige INT DEFAULT 0,
                        points BIGINT DEFAULT 0,
                        gems INT DEFAULT 0,
                        kills BIGINT DEFAULT 0,
                        deaths INT DEFAULT 0,
                        playtime BIGINT DEFAULT 0,
                        current_zone INT DEFAULT 1,
                        max_zone INT DEFAULT 1,
                        current_checkpoint INT DEFAULT 0,
                        achievement_count INT DEFAULT 0,
                        boss_kills BIGINT DEFAULT 0,
                        best_kill_streak INT DEFAULT 0,
                        vip_rank VARCHAR(32) DEFAULT 'FREE',
                        vip_expiry DATETIME NULL,
                        first_join DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_login DATETIME DEFAULT CURRENT_TIMESTAMP,
                        last_logout DATETIME NULL
                    )
                    """.formatted(tablePrefix);
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(playersSQL)) {
                stmt.executeUpdate();
            }
            
            // Index SQLite séparés
            if (!isMySQL) {
                try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_%splayers_name ON %splayers(name)".formatted(tablePrefix, tablePrefix))) {
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS idx_%splayers_level ON %splayers(level)".formatted(tablePrefix, tablePrefix))) {
                    stmt.executeUpdate();
                }
            }

            // Table des statistiques détaillées
            String statsSQL;
            if (isMySQL) {
                statsSQL = """
                    CREATE TABLE IF NOT EXISTS %sstats (
                        uuid VARCHAR(36) NOT NULL,
                        stat_key VARCHAR(64) NOT NULL,
                        stat_value BIGINT DEFAULT 0,
                        PRIMARY KEY (uuid, stat_key),
                        INDEX idx_stat_key (stat_key)
                    )
                    """.formatted(tablePrefix);
            } else {
                statsSQL = """
                    CREATE TABLE IF NOT EXISTS %sstats (
                        uuid VARCHAR(36) NOT NULL,
                        stat_key VARCHAR(64) NOT NULL,
                        stat_value BIGINT DEFAULT 0,
                        PRIMARY KEY (uuid, stat_key)
                    )
                    """.formatted(tablePrefix);
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(statsSQL)) {
                stmt.executeUpdate();
            }

            // Table de la zone mastery
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %szone_mastery (
                    uuid VARCHAR(36) NOT NULL,
                    zone_id INT NOT NULL,
                    mastery_points BIGINT DEFAULT 0,
                    mastery_level VARCHAR(16) DEFAULT 'BRONZE',
                    kills_in_zone BIGINT DEFAULT 0,
                    time_in_zone BIGINT DEFAULT 0,
                    PRIMARY KEY (uuid, zone_id)
                )
                """.formatted(tablePrefix))) {
                stmt.executeUpdate();
            }

            // Table des checkpoints
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %scheckpoints (
                    uuid VARCHAR(36) NOT NULL,
                    checkpoint_id INT NOT NULL,
                    unlocked BOOLEAN DEFAULT FALSE,
                    unlocked_at DATETIME NULL,
                    PRIMARY KEY (uuid, checkpoint_id)
                )
                """.formatted(tablePrefix))) {
                stmt.executeUpdate();
            }

            // Table de session (pour tracking temps de jeu)
            String sessionsSQL;
            if (isMySQL) {
                sessionsSQL = """
                    CREATE TABLE IF NOT EXISTS %ssessions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        login_time DATETIME NOT NULL,
                        logout_time DATETIME NULL,
                        duration_seconds INT DEFAULT 0,
                        zones_visited TEXT,
                        kills_session INT DEFAULT 0,
                        deaths_session INT DEFAULT 0,
                        INDEX idx_uuid (uuid),
                        INDEX idx_login (login_time)
                    )
                    """.formatted(tablePrefix);
            } else {
                sessionsSQL = """
                    CREATE TABLE IF NOT EXISTS %ssessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid VARCHAR(36) NOT NULL,
                        login_time DATETIME NOT NULL,
                        logout_time DATETIME NULL,
                        duration_seconds INT DEFAULT 0,
                        zones_visited TEXT,
                        kills_session INT DEFAULT 0,
                        deaths_session INT DEFAULT 0
                    )
                    """.formatted(tablePrefix);
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sessionsSQL)) {
                stmt.executeUpdate();
            }

            // Table du leaderboard cache (pour éviter les requêtes lourdes)
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %sleaderboard_cache (
                    type VARCHAR(32) PRIMARY KEY,
                    data %s,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(tablePrefix, jsonType))) {
                stmt.executeUpdate();
            }
            
            // Table Battle Pass (nouvelle)
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %sbattle_pass (
                    uuid VARCHAR(36) PRIMARY KEY,
                    season_id INT NOT NULL,
                    xp INT DEFAULT 0,
                    level INT DEFAULT 1,
                    premium BOOLEAN DEFAULT FALSE,
                    claimed_free TEXT DEFAULT '',
                    claimed_premium TEXT DEFAULT ''
                )
                """.formatted(tablePrefix))) {
                stmt.executeUpdate();
            }
            
            // Table Party/Groupe
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %sparty_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    total_party_kills BIGINT DEFAULT 0,
                    total_party_time BIGINT DEFAULT 0,
                    parties_joined INT DEFAULT 0
                )
                """.formatted(tablePrefix))) {
                stmt.executeUpdate();
            }

            // Table Classes/Talents
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS %sclass_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    class_type VARCHAR(32) NULL,
                    class_level INT DEFAULT 1,
                    class_xp BIGINT DEFAULT 0,
                    selected_branch VARCHAR(64) NULL,
                    selected_talents TEXT DEFAULT '',
                    class_kills BIGINT DEFAULT 0,
                    class_deaths BIGINT DEFAULT 0,
                    damage_dealt BIGINT DEFAULT 0,
                    damage_received BIGINT DEFAULT 0,
                    total_playtime_as_class BIGINT DEFAULT 0,
                    last_class_change BIGINT DEFAULT 0,
                    last_branch_change BIGINT DEFAULT 0,
                    talent_messages_enabled BOOLEAN DEFAULT TRUE
                )
                """.formatted(tablePrefix))) {
                stmt.executeUpdate();
            }

            // Migration: ajouter les colonnes manquantes pour les bases existantes
            migratePlayersTable(conn, isMySQL);

            plugin.log(Level.INFO, "§a✓ Tables créées/vérifiées (" + databaseType + ")");

        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur création tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migre la table players pour ajouter les nouvelles colonnes si nécessaires
     */
    private void migratePlayersTable(Connection conn, boolean isMySQL) {
        String[] columnsToAdd = {
            "achievement_count INT DEFAULT 0",
            "boss_kills BIGINT DEFAULT 0",
            "best_kill_streak INT DEFAULT 0",
            // Journey progression columns
            "journey_chapter INT DEFAULT 1",
            "journey_step INT DEFAULT 1",
            "journey_completed_steps TEXT",
            "journey_completed_chapters TEXT",
            "journey_unlocked_gates TEXT",
            "journey_step_progress TEXT"
        };

        for (String columnDef : columnsToAdd) {
            String columnName = columnDef.split(" ")[0];
            try {
                // Vérifie si la colonne existe
                String checkSql;
                if (isMySQL) {
                    checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
                } else {
                    // SQLite - utilise PRAGMA
                    checkSql = null; // On va utiliser une approche différente
                }

                boolean columnExists = false;

                if (isMySQL) {
                    try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                        stmt.setString(1, tablePrefix + "players");
                        stmt.setString(2, columnName);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            columnExists = rs.getInt(1) > 0;
                        }
                    }
                } else {
                    // SQLite - essayer de sélectionner la colonne
                    try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT " + columnName + " FROM " + tablePrefix + "players LIMIT 1")) {
                        stmt.executeQuery();
                        columnExists = true;
                    } catch (SQLException e) {
                        columnExists = false;
                    }
                }

                if (!columnExists) {
                    String alterSql = "ALTER TABLE " + tablePrefix + "players ADD COLUMN " + columnDef;
                    try (PreparedStatement stmt = conn.prepareStatement(alterSql)) {
                        stmt.executeUpdate();
                        plugin.log(Level.INFO, "§a✓ Colonne " + columnName + " ajoutée à la table players");
                    }
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "§eImpossible d'ajouter la colonne " + columnName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Exécute une requête de manière asynchrone
     */
    public CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task);
    }

    /**
     * Exécute une requête avec résultat de manière asynchrone
     */
    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return task.apply(conn);
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "§cErreur requête async: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Exécute une mise à jour de manière asynchrone
     */
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "§cErreur update async: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Exécute une requête SELECT de manière asynchrone
     */
    public CompletableFuture<Void> selectAsync(String sql, Consumer<ResultSet> consumer, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    consumer.accept(rs);
                }
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "§cErreur select async: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Batch insert optimisé pour insertions massives
     */
    public CompletableFuture<int[]> batchInsertAsync(String sql, Consumer<PreparedStatement> batcher) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    batcher.accept(stmt);
                    int[] results = stmt.executeBatch();
                    conn.commit();
                    return results;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "§cErreur batch insert: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Obtient le préfixe des tables
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Obtient le nom complet d'une table
     */
    public String table(String name) {
        return tablePrefix + name;
    }

    /**
     * Ferme proprement le pool de connexions
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.log(Level.INFO, "§a✓ Pool de connexions fermé");
        }
    }

    /**
     * Vérifie si la connexion est active
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Obtient des statistiques sur le pool
     */
    public String getPoolStats() {
        if (dataSource == null) return "Pool non initialisé";
        return String.format(
            "Actives: %d, Idle: %d, Total: %d, En attente: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}
