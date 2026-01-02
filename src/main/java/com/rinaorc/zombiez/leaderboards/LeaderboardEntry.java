package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Entrée d'un leaderboard (un joueur avec son score)
 */
@Getter
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {

    private final UUID uuid;
    private final String playerName;
    private final long value;
    @Setter private int rank;
    private final long lastUpdated;

    public LeaderboardEntry(UUID uuid, String playerName, long value, int rank) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.value = value;
        this.rank = rank;
        this.lastUpdated = System.currentTimeMillis();
    }

    public LeaderboardEntry(UUID uuid, String playerName, long value) {
        this(uuid, playerName, value, 0);
    }

    // Aliases pour compatibilité avec l'ancien système
    public String getName() { return playerName; }
    public UUID getPlayerUuid() { return uuid; }

    /**
     * Formate la valeur pour l'affichage
     */
    public String getFormattedValue() {
        if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000.0);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    /**
     * Formate la valeur en temps (pour playtime)
     */
    public String getFormattedTime() {
        long seconds = value;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours >= 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%dj %dh", days, hours);
        }
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * Obtient la couleur du rang pour l'affichage
     */
    public String getRankColor() {
        return switch (rank) {
            case 1 -> "§6§l"; // Or
            case 2 -> "§f§l"; // Argent
            case 3 -> "§c§l"; // Bronze
            default -> rank <= 10 ? "§e" : (rank <= 50 ? "§7" : "§8");
        };
    }

    /**
     * Obtient l'icône du rang (médaille)
     */
    public String getRankIcon() {
        return switch (rank) {
            case 1 -> "§6🥇";
            case 2 -> "§f🥈";
            case 3 -> "§c🥉";
            default -> "§7#" + rank;
        };
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        // Tri descendant (plus haute valeur = meilleur rang)
        return Long.compare(other.value, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderboardEntry that = (LeaderboardEntry) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
