package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Catégories de leaderboards pour le GUI
 */
@Getter
public enum LeaderboardCategory {
    // Row 2 - Slots 19, 21, 23, 25 (espacés symétriquement)
    PRINCIPAL("§6§lPrincipal", Material.GOLDEN_SWORD, "§7Classements généraux", 19),
    COMBAT("§c§lCombat", Material.DIAMOND_SWORD, "§7Stats de combat", 21),
    EXPLORATION("§a§lExploration", Material.COMPASS, "§7Découverte du monde", 23),
    PROGRESSION("§b§lProgression", Material.EXPERIENCE_BOTTLE, "§7Avancement du joueur", 25),
    // Row 3 - Slots 28, 30, 32, 34 (espacés symétriquement)
    PETS("§d§lPets", Material.WOLF_SPAWN_EGG, "§7Compagnons", 28),
    ECONOMIE("§e§lÉconomie", Material.GOLD_INGOT, "§7Richesse et commerce", 30),
    EVENEMENTS("§6§lÉvénements", Material.FIREWORK_ROCKET, "§7Participation aux events", 32),
    MISSIONS("§a§lMissions", Material.BOOK, "§7Quêtes accomplies", 34);

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
