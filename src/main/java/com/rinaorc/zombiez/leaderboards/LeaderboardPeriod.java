package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;
import org.bukkit.Material;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Périodes de leaderboards (pour les resets)
 */
@Getter
public enum LeaderboardPeriod {
    ALL_TIME("§6§lAll-Time", Material.DIAMOND_BLOCK, "§7Classement permanent", -1),
    DAILY("§a§lQuotidien", Material.CLOCK, "§7Reset à minuit", 24),
    WEEKLY("§b§lHebdomadaire", Material.COMPASS, "§7Reset lundi 00:00", 168),
    MONTHLY("§d§lMensuel", Material.FILLED_MAP, "§7Reset le 1er du mois", 720),
    SEASONAL("§c§lSaisonnier", Material.DRAGON_EGG, "§7Reset tous les 30 jours", 720);

    private final String displayName;
    private final Material icon;
    private final String description;
    private final int resetHours; // -1 = jamais

    LeaderboardPeriod(String displayName, Material icon, String description, int resetHours) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.resetHours = resetHours;
    }

    /**
     * Calcule le prochain reset pour cette période
     */
    public LocalDateTime getNextReset() {
        LocalDateTime now = LocalDateTime.now();
        return switch (this) {
            case ALL_TIME -> null; // Jamais de reset
            case DAILY -> now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> now.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
            case MONTHLY -> now.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case SEASONAL -> now.plusDays(30).truncatedTo(ChronoUnit.DAYS); // Simplifié, géré par SeasonManager
        };
    }

    /**
     * Calcule le temps restant avant le prochain reset (en secondes)
     */
    public long getSecondsUntilReset() {
        LocalDateTime next = getNextReset();
        if (next == null) return -1;
        return ChronoUnit.SECONDS.between(LocalDateTime.now(), next);
    }

    /**
     * Formate le temps restant de manière lisible
     */
    public String getFormattedTimeUntilReset() {
        long seconds = getSecondsUntilReset();
        if (seconds < 0) return "§7Jamais";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours >= 24) {
            long days = hours / 24;
            hours = hours % 24;
            return String.format("§e%dj %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("§e%dh %dm", hours, minutes);
        } else {
            return String.format("§e%dm", minutes);
        }
    }

    /**
     * Obtient le suffixe de colonne pour cette période
     * Utilisé pour les tables de leaderboards périodiques
     */
    public String getColumnSuffix() {
        return switch (this) {
            case ALL_TIME -> "";
            case DAILY -> "_daily";
            case WEEKLY -> "_weekly";
            case MONTHLY -> "_monthly";
            case SEASONAL -> "_seasonal";
        };
    }
}
