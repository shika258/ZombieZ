package com.rinaorc.zombiez.managers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.api.events.PlayerDataLoadEvent;
import com.rinaorc.zombiez.api.events.PlayerDataSaveEvent;
import com.rinaorc.zombiez.data.DatabaseManager;
import com.rinaorc.zombiez.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Gestionnaire des données joueurs avec cache Caffeine
 * Optimisé pour 200+ joueurs simultanés
 * 
 * Architecture:
 * - Cache Caffeine en mémoire pour accès ultra-rapide
 * - Sauvegarde async vers la BDD
 * - Batch updates pour optimiser les I/O
 */
public class PlayerDataManager {

    private final ZombieZPlugin plugin;
    private final DatabaseManager db;

    // Cache principal - Joueurs en ligne (accès < 1ms)
    private final Cache<UUID, PlayerData> playerCache;

    // Cache secondaire - Joueurs récemment déconnectés (pour reconnexion rapide)
    private final Cache<UUID, PlayerData> recentCache;

    // Set des joueurs en cours de chargement (évite les doubles loads)
    private final Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();

    // Set des joueurs en cours de sauvegarde
    private final Set<UUID> savingPlayers = ConcurrentHashMap.newKeySet();

    // Statistiques de performance
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long dbQueries = 0;

    public PlayerDataManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();

        // Cache principal pour joueurs en ligne
        // Taille max 250 pour avoir de la marge sur 200 joueurs
        this.playerCache = Caffeine.newBuilder()
                .maximumSize(250)
                .expireAfterAccess(30, TimeUnit.MINUTES) // Expire si pas accédé pendant 30min
                .removalListener((UUID uuid, PlayerData data, RemovalCause cause) -> {
                    if (data != null && data.isDirty() && cause != RemovalCause.REPLACED) {
                        // Sauvegarder automatiquement si données modifiées
                        saveAsync(data);
                    }
                })
                .recordStats() // Pour le monitoring
                .build();

        // Cache des joueurs récemment déconnectés (5 minutes)
        this.recentCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        plugin.log(Level.INFO, "§7PlayerDataManager initialisé (cache: 250 slots)");
    }

    /**
     * Charge les données d'un joueur de manière asynchrone
     * Appelé lors de la connexion du joueur
     */
    public CompletableFuture<PlayerData> loadPlayerAsync(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        // Vérifier le cache principal d'abord
        PlayerData cached = playerCache.getIfPresent(uuid);
        if (cached != null) {
            cacheHits++;
            cached.setName(name); // Update le nom au cas où il a changé
            cached.startSession();
            return CompletableFuture.completedFuture(cached);
        }

        // Vérifier le cache des récents (reconnexion rapide)
        PlayerData recent = recentCache.getIfPresent(uuid);
        if (recent != null) {
            cacheHits++;
            recentCache.invalidate(uuid);
            recent.setName(name);
            recent.startSession();
            playerCache.put(uuid, recent);
            return CompletableFuture.completedFuture(recent);
        }

        cacheMisses++;

        // Éviter les doubles chargements
        if (loadingPlayers.contains(uuid)) {
            // Attendre que le chargement en cours se termine
            return CompletableFuture.supplyAsync(() -> {
                while (loadingPlayers.contains(uuid)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return playerCache.getIfPresent(uuid);
            });
        }

        loadingPlayers.add(uuid);

        // Charger depuis la BDD de manière async
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData data = loadFromDatabase(uuid, name);
                playerCache.put(uuid, data);

                // Fire event sur le main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getPluginManager().callEvent(new PlayerDataLoadEvent(player, data));
                });

                return data;
            } finally {
                loadingPlayers.remove(uuid);
            }
        });
    }

    /**
     * Charge les données depuis la BDD (synchrone, appelé depuis thread async)
     */
    private PlayerData loadFromDatabase(UUID uuid, String name) {
        dbQueries++;

        try (Connection conn = db.getConnection()) {
            String sql = "SELECT * FROM " + db.table("players") + " WHERE uuid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Joueur existant
                        return loadFromResultSet(rs, uuid);
                    } else {
                        // Nouveau joueur
                        PlayerData newPlayer = new PlayerData(uuid, name);
                        insertNewPlayer(conn, newPlayer);
                        return newPlayer;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur chargement joueur " + uuid + ": " + e.getMessage());
            // Retourner un nouveau joueur en cas d'erreur (failsafe)
            return new PlayerData(uuid, name);
        }
    }

    /**
     * Construit un PlayerData depuis un ResultSet
     */
    private PlayerData loadFromResultSet(ResultSet rs, UUID uuid) throws SQLException {
        PlayerData data = new PlayerData(uuid);

        data.setName(rs.getString("name"));
        data.getLevel().set(rs.getInt("level"));
        data.getXp().set(rs.getLong("xp"));
        data.getPrestige().set(rs.getInt("prestige"));
        data.getPoints().set(rs.getLong("points"));
        data.getGems().set(rs.getInt("gems"));
        data.getKills().set(rs.getLong("kills"));
        data.getDeaths().set(rs.getLong("deaths"));
        data.getPlaytime().set(rs.getLong("playtime"));
        data.getCurrentZone().set(rs.getInt("current_zone"));
        data.getMaxZone().set(rs.getInt("max_zone"));
        data.getCurrentCheckpoint().set(rs.getInt("current_checkpoint"));
        data.setVipRank(rs.getString("vip_rank"));

        Timestamp vipExpiry = rs.getTimestamp("vip_expiry");
        if (vipExpiry != null) {
            data.setVipExpiry(vipExpiry.toInstant());
        }

        Timestamp firstJoin = rs.getTimestamp("first_join");
        if (firstJoin != null) {
            data.setFirstJoin(firstJoin.toInstant());
        }

        data.setLastLogin(Instant.now());
        data.startSession();
        data.clearDirty(); // Données fraîches de la BDD

        return data;
    }

    /**
     * Insère un nouveau joueur dans la BDD
     */
    private void insertNewPlayer(Connection conn, PlayerData data) throws SQLException {
        String sql = """
                INSERT INTO %s (uuid, name, level, xp, prestige, points, gems, kills, deaths,
                               playtime, current_zone, max_zone, current_checkpoint, vip_rank,
                               first_join, last_login)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(db.table("players"));

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getName());
            stmt.setInt(3, data.getLevel().get());
            stmt.setLong(4, data.getXp().get());
            stmt.setInt(5, data.getPrestige().get());
            stmt.setLong(6, data.getPoints().get());
            stmt.setInt(7, data.getGems().get());
            stmt.setLong(8, data.getKills().get());
            stmt.setLong(9, data.getDeaths().get());
            stmt.setLong(10, data.getPlaytime().get());
            stmt.setInt(11, data.getCurrentZone().get());
            stmt.setInt(12, data.getMaxZone().get());
            stmt.setInt(13, data.getCurrentCheckpoint().get());
            stmt.setString(14, data.getVipRank());
            stmt.setTimestamp(15, Timestamp.from(data.getFirstJoin()));
            stmt.setTimestamp(16, Timestamp.from(Instant.now()));

            stmt.executeUpdate();
        }

        plugin.log(Level.INFO, "§7Nouveau joueur créé: §e" + data.getName());
    }

    /**
     * Obtient les données d'un joueur depuis le cache
     * 
     * @return null si le joueur n'est pas en cache
     */
    public PlayerData getPlayer(UUID uuid) {
        return playerCache.getIfPresent(uuid);
    }

    /**
     * Obtient les données d'un joueur depuis le cache
     */
    public PlayerData getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Alias pour getPlayer - compatibilité
     */
    public PlayerData getPlayerData(UUID uuid) {
        return getPlayer(uuid);
    }

    /**
     * Alias pour getPlayer - compatibilité
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Alias pour getPlayer - compatibilité
     */
    public PlayerData getData(UUID uuid) {
        return getPlayer(uuid);
    }

    /**
     * Alias pour getPlayer - compatibilité
     */
    public PlayerData getData(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Vérifie si un joueur est en cache
     */
    public boolean isLoaded(UUID uuid) {
        return playerCache.getIfPresent(uuid) != null;
    }

    /**
     * Sauvegarde les données d'un joueur de manière asynchrone
     */
    public CompletableFuture<Void> saveAsync(PlayerData data) {
        if (data == null || !data.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }

        UUID uuid = data.getUuid();

        // Éviter les doubles sauvegardes
        if (savingPlayers.contains(uuid)) {
            return CompletableFuture.completedFuture(null);
        }

        savingPlayers.add(uuid);

        return CompletableFuture.runAsync(() -> {
            try {
                saveToDatabase(data);
                data.clearDirty();

                // Fire event asynchronously (Paper 1.21.4 requires async-only)
                Player player = data.getPlayer();
                if (player != null) {
                    Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(player, data));
                }
            } finally {
                savingPlayers.remove(uuid);
            }
        });
    }

    /**
     * Sauvegarde vers la BDD (synchrone, appelé depuis thread async)
     */
    private void saveToDatabase(PlayerData data) {
        dbQueries++;

        try (Connection conn = db.getConnection()) {
            String sql = """
                    UPDATE %s SET
                        name = ?, level = ?, xp = ?, prestige = ?, points = ?, gems = ?,
                        kills = ?, deaths = ?, playtime = ?, current_zone = ?, max_zone = ?,
                        current_checkpoint = ?, vip_rank = ?, vip_expiry = ?, last_login = ?, last_logout = ?
                    WHERE uuid = ?
                    """.formatted(db.table("players"));

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, data.getName());
                stmt.setInt(2, data.getLevel().get());
                stmt.setLong(3, data.getXp().get());
                stmt.setInt(4, data.getPrestige().get());
                stmt.setLong(5, data.getPoints().get());
                stmt.setInt(6, data.getGems().get());
                stmt.setLong(7, data.getKills().get());
                stmt.setLong(8, data.getDeaths().get());
                stmt.setLong(9, data.getPlaytime().get());
                stmt.setInt(10, data.getCurrentZone().get());
                stmt.setInt(11, data.getMaxZone().get());
                stmt.setInt(12, data.getCurrentCheckpoint().get());
                stmt.setString(13, data.getVipRank());
                stmt.setTimestamp(14, data.getVipExpiry() != null ? Timestamp.from(data.getVipExpiry()) : null);
                stmt.setTimestamp(15, data.getLastLogin() != null ? Timestamp.from(data.getLastLogin()) : null);
                stmt.setTimestamp(16, data.getLastLogout() != null ? Timestamp.from(data.getLastLogout()) : null);
                stmt.setString(17, data.getUuid().toString());

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "§cErreur sauvegarde joueur " + data.getUuid() + ": " + e.getMessage());
        }
    }

    /**
     * Sauvegarde et décharge un joueur (déconnexion)
     */
    public CompletableFuture<Void> unloadPlayer(UUID uuid) {
        PlayerData data = playerCache.getIfPresent(uuid);
        if (data == null)
            return CompletableFuture.completedFuture(null);

        // Terminer la session
        data.endSession();

        // Sauvegarder
        return saveAsync(data).thenRun(() -> {
            // Déplacer vers le cache des récents
            playerCache.invalidate(uuid);
            recentCache.put(uuid, data);
        });
    }

    /**
     * Sauvegarde tous les joueurs de manière asynchrone
     */
    public void saveAllAsync() {
        Collection<PlayerData> allData = playerCache.asMap().values();

        if (allData.isEmpty())
            return;

        long dirtyCount = allData.stream().filter(PlayerData::isDirty).count();
        if (dirtyCount == 0)
            return;

        CompletableFuture.runAsync(() -> {
            for (PlayerData data : allData) {
                if (data.isDirty()) {
                    saveToDatabase(data);
                    data.clearDirty();
                }
            }
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.log(Level.INFO, "§7Auto-save: §e" + dirtyCount + " §7joueurs sauvegardés");
            }
        });
    }

    /**
     * Sauvegarde tous les joueurs de manière synchrone (pour shutdown)
     */
    public void saveAllSync() {
        Collection<PlayerData> allData = playerCache.asMap().values();
        int saved = 0;

        for (PlayerData data : allData) {
            if (data.isDirty()) {
                data.endSession();
                saveToDatabase(data);
                data.clearDirty();
                saved++;
            }
        }

        plugin.log(Level.INFO, "§7Sauvegarde finale: §e" + saved + " §7joueurs");
    }

    /**
     * Obtient tous les joueurs en cache
     */
    public Collection<PlayerData> getAllCached() {
        return Collections.unmodifiableCollection(playerCache.asMap().values());
    }

    /**
     * Obtient le nombre de joueurs en cache
     */
    public int getCachedCount() {
        return (int) playerCache.estimatedSize();
    }

    /**
     * Obtient les statistiques du cache
     */
    public String getCacheStats() {
        var stats = playerCache.stats();
        return String.format(
                "Hits: %d, Misses: %d, Hit Rate: %.1f%%, Size: %d, DB Queries: %d",
                cacheHits, cacheMisses,
                cacheHits + cacheMisses > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0,
                playerCache.estimatedSize(),
                dbQueries);
    }

    /**
     * Force le rechargement d'un joueur depuis la BDD
     */
    public CompletableFuture<PlayerData> reloadPlayer(UUID uuid) {
        playerCache.invalidate(uuid);
        recentCache.invalidate(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return loadPlayerAsync(player);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Invalide le cache d'un joueur
     */
    public void invalidate(UUID uuid) {
        playerCache.invalidate(uuid);
        recentCache.invalidate(uuid);
    }

    /**
     * Nettoie tout le cache
     */
    public void clearCache() {
        saveAllSync();
        playerCache.invalidateAll();
        recentCache.invalidateAll();
    }

    // ==================== PLAYTIME TRACKER ====================

    private org.bukkit.scheduler.BukkitTask playtimeTask;

    // Tracking des nuits - joueurs qui ont commencé la nuit
    private final Set<UUID> playersInNight = ConcurrentHashMap.newKeySet();
    private boolean wasNight = false;

    /**
     * Démarre le tracker de temps de jeu
     * Met à jour le playtime toutes les secondes et les missions toutes les minutes
     * Track aussi les nuits survivées
     */
    public void startPlaytimeTracker() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
        }

        // Compteur pour tracker les minutes (60 ticks = 60 secondes entre updates missions)
        final int[] secondsCounter = {0};

        playtimeTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                secondsCounter[0]++;

                // Vérifier le cycle jour/nuit
                checkNightCycle();

                // Pour chaque joueur en ligne
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = playerCache.getIfPresent(player.getUniqueId());
                    if (data == null) continue;

                    // Incrémenter le playtime (1 seconde)
                    data.getPlaytime().incrementAndGet();

                    // Toutes les 60 secondes, mettre à jour les missions PLAYTIME
                    if (secondsCounter[0] >= 60) {
                        plugin.getMissionManager().updateProgress(player,
                            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.PLAYTIME, 60);

                        // Vérifier les achievements de temps de jeu
                        long totalPlaytime = data.getPlaytime().get();
                        var achievementManager = plugin.getAchievementManager();
                        achievementManager.checkAndUnlock(player, "survivor_1", (int) Math.min(totalPlaytime, Integer.MAX_VALUE));
                        achievementManager.checkAndUnlock(player, "survivor_2", (int) Math.min(totalPlaytime, Integer.MAX_VALUE));
                        achievementManager.checkAndUnlock(player, "survivor_3", (int) Math.min(totalPlaytime, Integer.MAX_VALUE));
                        achievementManager.checkAndUnlock(player, "veteran", (int) Math.min(totalPlaytime, Integer.MAX_VALUE));
                        achievementManager.checkAndUnlock(player, "ancient", (int) Math.min(totalPlaytime, Integer.MAX_VALUE));
                    }
                }

                // Réinitialiser le compteur toutes les 60 secondes
                if (secondsCounter[0] >= 60) {
                    secondsCounter[0] = 0;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Démarre après 1 seconde, répète chaque seconde

        plugin.log(Level.INFO, "§a✓ Tracker de playtime démarré");
    }

    /**
     * Vérifie le cycle jour/nuit et track les joueurs qui survivent la nuit
     */
    private void checkNightCycle() {
        // Obtenir le monde principal
        org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return;

        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;

        // Transition jour → nuit : enregistrer tous les joueurs en ligne
        if (isNight && !wasNight) {
            playersInNight.clear();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playersInNight.add(player.getUniqueId());
            }
        }

        // Transition nuit → jour : récompenser ceux qui ont survécu toute la nuit
        if (!isNight && wasNight) {
            for (UUID uuid : playersInNight) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    PlayerData data = playerCache.getIfPresent(uuid);
                    if (data != null) {
                        // Incrémenter le compteur de nuits
                        data.incrementStat("nights_survived");
                        int nightsSurvived = (int) data.getStat("nights_survived");

                        // Mission
                        plugin.getMissionManager().updateProgress(player,
                            com.rinaorc.zombiez.progression.MissionManager.MissionTracker.NIGHTS_SURVIVED, 1);

                        // Achievement
                        var achievementManager = plugin.getAchievementManager();
                        achievementManager.checkAndUnlock(player, "night_walker", nightsSurvived);
                    }
                }
            }
            playersInNight.clear();
        }

        wasNight = isNight;
    }

    /**
     * Arrête le tracker de temps de jeu
     */
    public void stopPlaytimeTracker() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
            playtimeTask = null;
        }
    }
}
