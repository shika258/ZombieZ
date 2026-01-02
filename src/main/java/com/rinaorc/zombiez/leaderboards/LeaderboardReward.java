package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;

/**
 * Représente une récompense de leaderboard
 */
@Getter
public class LeaderboardReward {

    private final long points;
    private final int gems;
    private final String title;
    private final String cosmetic;

    public LeaderboardReward(long points, int gems, String title, String cosmetic) {
        this.points = points;
        this.gems = gems;
        this.title = title;
        this.cosmetic = cosmetic;
    }

    /**
     * Calcule la récompense selon le type, la période et le rang
     */
    public static LeaderboardReward calculateReward(LeaderboardType type, LeaderboardPeriod period, int rank) {
        if (rank < 1 || rank > 100) return null;

        // Multiplicateur de base selon la période
        double periodMultiplier = switch (period) {
            case DAILY -> 0.1;
            case WEEKLY -> 0.5;
            case MONTHLY -> 1.0;
            case SEASONAL -> 2.0;
            case ALL_TIME -> 3.0;
        };

        // Points de base selon le rang
        long basePoints = getBasePoints(rank);
        int baseGems = getBaseGems(rank);
        String title = getTitle(type, period, rank);
        String cosmetic = getCosmetic(type, period, rank);

        // Appliquer le multiplicateur
        long finalPoints = (long) (basePoints * periodMultiplier);
        int finalGems = (int) (baseGems * periodMultiplier);

        // Minimum 1 gem si on est dans le top 10
        if (rank <= 10 && finalGems < 1) finalGems = 1;

        return new LeaderboardReward(finalPoints, finalGems, title, cosmetic);
    }

    private static long getBasePoints(int rank) {
        return switch (rank) {
            case 1 -> 50000;
            case 2 -> 30000;
            case 3 -> 20000;
            default -> {
                if (rank <= 10) yield 10000;
                if (rank <= 25) yield 5000;
                if (rank <= 50) yield 2500;
                yield 1000;
            }
        };
    }

    private static int getBaseGems(int rank) {
        return switch (rank) {
            case 1 -> 500;
            case 2 -> 300;
            case 3 -> 200;
            default -> {
                if (rank <= 10) yield 100;
                if (rank <= 25) yield 50;
                if (rank <= 50) yield 25;
                yield 10;
            }
        };
    }

    private static String getTitle(LeaderboardType type, LeaderboardPeriod period, int rank) {
        // Titres pour le #1 seulement
        if (rank != 1) return null;

        String periodPrefix = switch (period) {
            case DAILY -> "";
            case WEEKLY -> "§7[Hebdo] ";
            case MONTHLY -> "§e[Mensuel] ";
            case SEASONAL -> "§6[Saison] ";
            case ALL_TIME -> "§c§l";
        };

        return switch (type) {
            case KILLS_TOTAL -> periodPrefix + "§4§l☠ Génocidaire ☠";
            case LEVEL -> periodPrefix + "§5§l⚡ Immortel ⚡";
            case MAX_ZONE -> periodPrefix + "§2§l🗺 Pionnier 🗺";
            case BOSS_KILLS -> periodPrefix + "§d§l👹 Fléau des Boss 👹";
            case PLAYTIME -> periodPrefix + "§7§l⏳ Éternel ⏳";
            case ELITE_KILLS -> periodPrefix + "§6§l⚔ Chasseur d'Élites ⚔";
            case KILL_STREAK -> periodPrefix + "§c§l🔥 Inarrêtable 🔥";
            case PRESTIGE -> periodPrefix + "§d§l★ Transcendé ★";
            case ACHIEVEMENTS -> periodPrefix + "§6§l🏆 Complétionniste 🏆";
            case HEADSHOTS -> periodPrefix + "§c§l🎯 Tireur d'Élite 🎯";
            default -> null;
        };
    }

    private static String getCosmetic(LeaderboardType type, LeaderboardPeriod period, int rank) {
        // Cosmétiques pour le top 3 seulement
        if (rank > 3) return null;

        // Auras pour le #1
        if (rank == 1) {
            return switch (type) {
                case KILLS_TOTAL -> "aura_death";
                case LEVEL -> "aura_legendary";
                case BOSS_KILLS -> "aura_flame";
                case PLAYTIME -> "aura_void";
                default -> null;
            };
        }

        // Particules pour #2 et #3
        if (rank == 2 || rank == 3) {
            return switch (type) {
                case KILLS_TOTAL -> "trail_blood";
                case LEVEL -> "trail_enchant";
                case BOSS_KILLS -> "trail_fire";
                default -> null;
            };
        }

        return null;
    }

    /**
     * Vérifie si cette récompense a du contenu
     */
    public boolean hasContent() {
        return points > 0 || gems > 0 || title != null || cosmetic != null;
    }

    /**
     * Formate la récompense pour l'affichage
     */
    public String format() {
        StringBuilder sb = new StringBuilder();

        if (points > 0) {
            sb.append("§e").append(formatNumber(points)).append(" points");
        }

        if (gems > 0) {
            if (sb.length() > 0) sb.append(" §7+ ");
            sb.append("§d").append(gems).append(" gemmes");
        }

        if (title != null) {
            if (sb.length() > 0) sb.append(" §7+ ");
            sb.append("§6Titre: ").append(title);
        }

        if (cosmetic != null) {
            if (sb.length() > 0) sb.append(" §7+ ");
            sb.append("§b").append(cosmetic);
        }

        return sb.toString();
    }

    private static String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }
}
