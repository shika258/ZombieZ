package com.rinaorc.zombiez.recycling;

import lombok.Getter;

/**
 * Définit les milestones (achievements) du système de recyclage
 * Chaque milestone donne une récompense en points bonus
 */
@Getter
public enum RecycleMilestone {

    // ===== MILESTONES - ITEMS RECYCLÉS =====
    ITEMS_10("recycler_debutant", "Recycleur Débutant", "Recycler 10 items", MilestoneType.ITEMS_RECYCLED, 10, 50),
    ITEMS_50("recycler_apprenti", "Recycleur Apprenti", "Recycler 50 items", MilestoneType.ITEMS_RECYCLED, 50, 150),
    ITEMS_100("recycler_confirme", "Recycleur Confirmé", "Recycler 100 items", MilestoneType.ITEMS_RECYCLED, 100, 300),
    ITEMS_500("recycler_expert", "Recycleur Expert", "Recycler 500 items", MilestoneType.ITEMS_RECYCLED, 500, 1000),
    ITEMS_1000("recycler_maitre", "Maître Recycleur", "Recycler 1,000 items", MilestoneType.ITEMS_RECYCLED, 1000, 2500),
    ITEMS_5000("recycler_legendaire", "Recycleur Légendaire", "Recycler 5,000 items", MilestoneType.ITEMS_RECYCLED, 5000, 10000),
    ITEMS_10000("recycler_mythique", "Recycleur Mythique", "Recycler 10,000 items", MilestoneType.ITEMS_RECYCLED, 10000, 25000),

    // ===== MILESTONES - POINTS GAGNÉS =====
    POINTS_1K("points_bronze", "Gains Bronze", "Gagner 1,000 points via recyclage", MilestoneType.POINTS_EARNED, 1000, 100),
    POINTS_10K("points_argent", "Gains Argent", "Gagner 10,000 points via recyclage", MilestoneType.POINTS_EARNED, 10000, 500),
    POINTS_50K("points_or", "Gains Or", "Gagner 50,000 points via recyclage", MilestoneType.POINTS_EARNED, 50000, 2000),
    POINTS_100K("points_platine", "Gains Platine", "Gagner 100,000 points via recyclage", MilestoneType.POINTS_EARNED, 100000, 5000),
    POINTS_500K("points_diamant", "Gains Diamant", "Gagner 500,000 points via recyclage", MilestoneType.POINTS_EARNED, 500000, 15000),
    POINTS_1M("points_eternel", "Gains Éternels", "Gagner 1,000,000 points via recyclage", MilestoneType.POINTS_EARNED, 1000000, 50000),

    // ===== MILESTONES - SESSION =====
    SESSION_100("session_productive", "Session Productive", "Recycler 100 items en une session", MilestoneType.SESSION_ITEMS, 100, 200),
    SESSION_500("session_intensive", "Session Intensive", "Recycler 500 items en une session", MilestoneType.SESSION_ITEMS, 500, 1000),

    // ===== MILESTONES - EFFICACITÉ =====
    SINGLE_RECYCLE_100("gros_lot", "Gros Lot", "Obtenir 100+ pts d'un seul item", MilestoneType.SINGLE_RECYCLE, 100, 150),
    SINGLE_RECYCLE_500("jackpot", "Jackpot!", "Obtenir 500+ pts d'un seul item", MilestoneType.SINGLE_RECYCLE, 500, 500),
    SINGLE_RECYCLE_1000("mega_jackpot", "Méga Jackpot!", "Obtenir 1,000+ pts d'un seul item", MilestoneType.SINGLE_RECYCLE, 1000, 1500);

    private final String id;
    private final String displayName;
    private final String description;
    private final MilestoneType type;
    private final long requiredValue;
    private final int bonusPoints;

    RecycleMilestone(String id, String displayName, String description, MilestoneType type, long requiredValue, int bonusPoints) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.requiredValue = requiredValue;
        this.bonusPoints = bonusPoints;
    }

    /**
     * Retourne l'icône colorée du milestone
     */
    public String getIcon() {
        return switch (type) {
            case ITEMS_RECYCLED -> "§a♻";
            case POINTS_EARNED -> "§6⭐";
            case SESSION_ITEMS -> "§b⚡";
            case SINGLE_RECYCLE -> "§d✦";
        };
    }

    /**
     * Retourne le nom coloré du milestone
     */
    public String getColoredName() {
        return switch (type) {
            case ITEMS_RECYCLED -> "§a" + displayName;
            case POINTS_EARNED -> "§6" + displayName;
            case SESSION_ITEMS -> "§b" + displayName;
            case SINGLE_RECYCLE -> "§d" + displayName;
        };
    }

    /**
     * Formate la valeur requise pour affichage
     */
    public String getFormattedRequirement() {
        if (requiredValue >= 1_000_000) {
            return String.format("%.1fM", requiredValue / 1_000_000.0);
        } else if (requiredValue >= 1_000) {
            return String.format("%.0fK", requiredValue / 1_000.0);
        }
        return String.valueOf(requiredValue);
    }

    /**
     * Types de milestones
     */
    public enum MilestoneType {
        ITEMS_RECYCLED,    // Total items recyclés (tous temps)
        POINTS_EARNED,     // Total points gagnés (tous temps)
        SESSION_ITEMS,     // Items recyclés en une session
        SINGLE_RECYCLE     // Points d'un seul recyclage
    }

    /**
     * Trouve un milestone par son ID
     */
    public static RecycleMilestone fromId(String id) {
        for (RecycleMilestone milestone : values()) {
            if (milestone.getId().equals(id)) {
                return milestone;
            }
        }
        return null;
    }
}
