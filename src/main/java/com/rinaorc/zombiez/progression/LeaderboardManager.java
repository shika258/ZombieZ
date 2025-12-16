package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de leaderboards avec cache optimisé
 * Met à jour les classements périodiquement
 */
public class LeaderboardManager {

    private final ZombieZPlugin plugin;
    
    // Cache des leaderboards (Type -> Liste triée)
    private final Map<LeaderboardType, List<LeaderboardEntry>> leaderboardCache;
    
    // Cache du rang des joueurs (UUID -> Type -> Rang)
    private final Map<UUID, Map<LeaderboardType, Integer>> playerRankCache;
    
    // Timestamp de dernière mise à jour
    @Getter
    private long lastUpdate = 0;
    
    // Configuration
    private static final int MAX_ENTRIES = 100;
    private static final long UPDATE_INTERVAL = 60 * 20L; // 1 minute

    public LeaderboardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.leaderboardCache = new ConcurrentHashMap<>();
        this.playerRankCache = new ConcurrentHashMap<>();
        
        // Initialiser les caches vides
        for (LeaderboardType type : LeaderboardType.values()) {
            leaderboardCache.put(type, new ArrayList<>());
        }
        
        // Démarrer la mise à jour périodique
        startUpdateTask();
    }

    /**
     * Démarre la tâche de mise à jour périodique
     */
    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshAllLeaderboards();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, UPDATE_INTERVAL);
    }

    /**
     * Rafraîchit tous les leaderboards
     */
    public void refreshAllLeaderboards() {
        for (LeaderboardType type : LeaderboardType.values()) {
            refreshLeaderboard(type);
        }
        lastUpdate = System.currentTimeMillis();
        
        // Mettre à jour les rangs des joueurs en ligne
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerRanks(player.getUniqueId());
        }
    }

    /**
     * Rafraîchit un leaderboard spécifique
     */
    private void refreshLeaderboard(LeaderboardType type) {
        List<LeaderboardEntry> entries = fetchFromDatabase(type);
        leaderboardCache.put(type, entries);
    }

    /**
     * Récupère les données depuis la BDD
     */
    private List<LeaderboardEntry> fetchFromDatabase(LeaderboardType type) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        String column = type.getColumn();
        String sql = "SELECT uuid, name, " + column + " FROM zombiez_players " +
                    "ORDER BY " + column + " DESC LIMIT ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, MAX_ENTRIES);
            ResultSet rs = stmt.executeQuery();
            
            int rank = 1;
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                long value = rs.getLong(column);
                
                entries.add(new LeaderboardEntry(rank++, uuid, name, value));
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du chargement du leaderboard " + type + ": " + e.getMessage());
        }
        
        return entries;
    }

    /**
     * Met à jour le cache des rangs d'un joueur
     */
    private void updatePlayerRanks(UUID uuid) {
        Map<LeaderboardType, Integer> ranks = new HashMap<>();
        
        for (LeaderboardType type : LeaderboardType.values()) {
            List<LeaderboardEntry> entries = leaderboardCache.get(type);
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getUuid().equals(uuid)) {
                    ranks.put(type, i + 1);
                    break;
                }
            }
        }
        
        playerRankCache.put(uuid, ranks);
    }

    /**
     * Obtient le top X d'un leaderboard
     */
    public List<LeaderboardEntry> getTop(LeaderboardType type, int count) {
        List<LeaderboardEntry> all = leaderboardCache.get(type);
        if (all == null) return Collections.emptyList();
        
        return all.subList(0, Math.min(count, all.size()));
    }
    
    /**
     * Alias pour getTop - compatibilité
     */
    public List<LeaderboardEntry> getTopEntries(LeaderboardType type, int count) {
        return getTop(type, count);
    }
    
    /**
     * Alias pour getTop avec String
     */
    public List<LeaderboardEntry> getTopEntries(String typeName, int count) {
        try {
            LeaderboardType type = LeaderboardType.valueOf(typeName.toUpperCase());
            return getTop(type, count);
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Obtient le rang d'un joueur (depuis le cache)
     */
    public int getPlayerRank(UUID uuid, LeaderboardType type) {
        Map<LeaderboardType, Integer> ranks = playerRankCache.get(uuid);
        if (ranks == null) return -1;
        return ranks.getOrDefault(type, -1);
    }
    
    /**
     * Surcharge avec String pour le type
     */
    public int getPlayerRank(UUID uuid, String typeName) {
        try {
            LeaderboardType type = LeaderboardType.valueOf(typeName.toUpperCase());
            return getPlayerRank(uuid, type);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /**
     * Obtient le rang d'un joueur (calcul en temps réel)
     */
    public int getPlayerRankRealtime(UUID uuid, LeaderboardType type) {
        String column = type.getColumn();
        String sql = "SELECT COUNT(*) + 1 as rank FROM zombiez_players p1 " +
                    "WHERE " + column + " > (SELECT " + column + " FROM zombiez_players WHERE uuid = ?)";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("rank");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur calcul rang: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Obtient les joueurs autour d'un rang
     */
    public List<LeaderboardEntry> getAroundPlayer(UUID uuid, LeaderboardType type, int range) {
        List<LeaderboardEntry> entries = leaderboardCache.get(type);
        if (entries == null) return Collections.emptyList();
        
        // Trouver le joueur
        int playerIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getUuid().equals(uuid)) {
                playerIndex = i;
                break;
            }
        }
        
        if (playerIndex == -1) return Collections.emptyList();
        
        // Extraire la plage
        int start = Math.max(0, playerIndex - range);
        int end = Math.min(entries.size(), playerIndex + range + 1);
        
        return entries.subList(start, end);
    }

    /**
     * Formate un leaderboard pour l'affichage
     */
    public List<String> formatLeaderboard(LeaderboardType type, int count) {
        List<String> lines = new ArrayList<>();
        List<LeaderboardEntry> entries = getTop(type, count);
        
        lines.add("§6§l══ " + type.getDisplayName() + " ══");
        lines.add("");
        
        for (LeaderboardEntry entry : entries) {
            String rankColor = getRankColor(entry.getRank());
            String value = formatValue(entry.getValue(), type);
            
            lines.add(rankColor + "#" + entry.getRank() + " §f" + entry.getName() + " §7- §e" + value);
        }
        
        return lines;
    }

    /**
     * Formate la valeur selon le type
     */
    private String formatValue(long value, LeaderboardType type) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    /**
     * Obtient la couleur du rang
     */
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6§l"; // Or
            case 2 -> "§7§l"; // Argent
            case 3 -> "§c§l"; // Bronze
            default -> rank <= 10 ? "§e" : "§7";
        };
    }

    /**
     * Types de leaderboards
     */
    @Getter
    public enum LeaderboardType {
        KILLS("Kills", "kills", "§c⚔"),
        DEATHS("Morts", "deaths", "§4☠"),
        LEVEL("Niveau", "level", "§a✦"),
        PRESTIGE("Prestige", "prestige", "§d★"),
        POINTS("Points", "points", "§e⛃"),
        GEMS("Gemmes", "gems", "§d💎"),
        PLAYTIME("Temps de Jeu", "playtime", "§b⏱"),
        ACHIEVEMENTS("Achievements", "achievement_count", "§6🏆"),
        BOSS_KILLS("Boss Tués", "boss_kills", "§4👹"),
        MAX_ZONE("Zone Max", "max_zone", "§2🗺"),
        KILL_STREAK("Meilleur Streak", "best_kill_streak", "§c🔥");
        
        private final String displayName;
        private final String column;
        private final String icon;
        
        LeaderboardType(String displayName, String column, String icon) {
            this.displayName = displayName;
            this.column = column;
            this.icon = icon;
        }
    }

    /**
     * Entrée de leaderboard
     */
    @Getter
    public static class LeaderboardEntry {
        private final int rank;
        private final UUID uuid;
        private final String name;
        private final long value;
        
        public LeaderboardEntry(int rank, UUID uuid, String name, long value) {
            this.rank = rank;
            this.uuid = uuid;
            this.name = name;
            this.value = value;
        }
        
        // Aliases pour compatibilité
        public String getPlayerName() { return name; }
        public UUID getPlayerUuid() { return uuid; }
    }

    /**
     * Créer un hologramme de leaderboard (intégration DecentHolograms)
     */
    public void createHologram(LeaderboardType type, org.bukkit.Location location) {
        // Intégration avec DecentHolograms si disponible
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }
        
        List<String> lines = formatLeaderboard(type, 10);
        
        // Créer via commande ou API
        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            content.append(line).append("\n");
        }
        
        // Stocker la location pour mise à jour
        // Cette partie serait implémentée avec l'API DecentHolograms
    }
    
    /**
     * Entrée de leaderboard
     */
}
