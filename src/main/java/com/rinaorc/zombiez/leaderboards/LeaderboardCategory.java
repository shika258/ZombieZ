package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Catégories de leaderboards pour le GUI
 */
@Getter
public enum LeaderboardCategory {
    PRINCIPAL("§6§lPrincipal", Material.GOLDEN_SWORD, "§7Classements généraux", 10),
    COMBAT("§c§lCombat", Material.DIAMOND_SWORD, "§7Stats de combat", 12),
    EXPLORATION("§a§lExploration", Material.COMPASS, "§7Découverte du monde", 14),
    PROGRESSION("§b§lProgression", Material.EXPERIENCE_BOTTLE, "§7Avancement du joueur", 16),
    PETS("§d§lPets", Material.WOLF_SPAWN_EGG, "§7Compagnons", 20),
    ECONOMIE("§e§lÉconomie", Material.GOLD_INGOT, "§7Richesse et commerce", 22),
    EVENEMENTS("§6§lÉvénements", Material.FIREWORK_ROCKET, "§7Participation aux events", 24),
    MISSIONS("§a§lMissions", Material.BOOK, "§7Quêtes accomplies", 30);

    private final String displayName;
    private final Material icon;
    private final String description;
    private final int guiSlot;

    LeaderboardCategory(String displayName, Material icon, String description, int guiSlot) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.guiSlot = guiSlot;
    }
}
