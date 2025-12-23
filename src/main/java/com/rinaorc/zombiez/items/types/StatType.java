package com.rinaorc.zombiez.items.types;

import lombok.Getter;
import org.bukkit.attribute.Attribute;

/**
 * Types de statistiques pour les items
 * Définit les stats de base et les stats d'affixes
 */
@Getter
public enum StatType {

    // ==================== STATS OFFENSIVES ====================
    
    DAMAGE(
        "Dégâts",
        "⚔",
        "§c",
        StatCategory.OFFENSIVE,
        true,  // Peut être une stat de base
        false, // N'est pas un pourcentage
        Attribute.ATTACK_DAMAGE,
        0, 50,
        "{value}"
    ),
    
    DAMAGE_PERCENT(
        "Dégâts",
        "⚔",
        "§c",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        -20, 50,
        "{value}%"
    ),
    
    ATTACK_SPEED(
        "Vitesse d'attaque",
        "⚡",
        "§e",
        StatCategory.OFFENSIVE,
        true,
        false,
        Attribute.ATTACK_SPEED,
        -0.5, 0.5,
        "{value}"
    ),
    
    ATTACK_SPEED_PERCENT(
        "Vitesse d'attaque",
        "⚡",
        "§e",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        -10, 20,
        "{value}%"
    ),
    
    CRIT_CHANCE(
        "Chance critique",
        "✦",
        "§6",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 25,
        "{value}%"
    ),
    
    CRIT_DAMAGE(
        "Dégâts critiques",
        "✦",
        "§6",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 100,
        "+{value}%"
    ),
    
    ARMOR_PENETRATION(
        "Pénétration d'armure",
        "➤",
        "§4",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 30,
        "{value}%"
    ),

    DRAW_SPEED(
        "Cadence de tir",
        "➹",
        "§b",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 50,
        "+{value}%"
    ),
    
    LIFESTEAL(
        "Vol de vie",
        "❤",
        "§4",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 15,
        "{value}%"
    ),

    // ==================== STATS ÉLÉMENTAIRES ====================
    
    FIRE_DAMAGE(
        "Dégâts Feu",
        "🔥",
        "§6",
        StatCategory.ELEMENTAL,
        false,
        false,
        null,
        0, 30,
        "+{value}"
    ),
    
    ICE_DAMAGE(
        "Dégâts Glace",
        "❄",
        "§b",
        StatCategory.ELEMENTAL,
        false,
        false,
        null,
        0, 30,
        "+{value}"
    ),
    
    LIGHTNING_DAMAGE(
        "Dégâts Foudre",
        "⚡",
        "§e",
        StatCategory.ELEMENTAL,
        false,
        false,
        null,
        0, 30,
        "+{value}"
    ),
    
    POISON_DAMAGE(
        "Dégâts Poison",
        "☠",
        "§2",
        StatCategory.ELEMENTAL,
        false,
        false,
        null,
        0, 20,
        "+{value}/s"
    ),

    // ==================== STATS DÉFENSIVES ====================
    
    ARMOR(
        "Armure",
        "🛡",
        "§7",
        StatCategory.DEFENSIVE,
        true,
        false,
        Attribute.ARMOR,
        0, 20,
        "{value}"
    ),
    
    ARMOR_PERCENT(
        "Armure",
        "🛡",
        "§7",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        -10, 40,
        "{value}%"
    ),
    
    BLOCK_CHANCE(
        "Chance de Blocage",
        "🛡",
        "§9",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 25,
        "{value}%"
    ),
    
    MAX_HEALTH(
        "Vie maximale",
        "❤",
        "§c",
        StatCategory.DEFENSIVE,
        false,
        false,
        Attribute.MAX_HEALTH,
        0, 40,
        "+{value}"
    ),
    
    HEALTH_REGEN(
        "Régénération",
        "❤",
        "§a",
        StatCategory.DEFENSIVE,
        false,
        false,
        null,
        0, 3,
        "+{value}/s"
    ),
    
    DAMAGE_REDUCTION(
        "Réduction de dégâts",
        "⬇",
        "§9",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 25,
        "-{value}%"
    ),
    
    DODGE_CHANCE(
        "Esquive",
        "↷",
        "§f",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 20,
        "{value}%"
    ),
    
    THORNS(
        "Épines",
        "⚔",
        "§4",
        StatCategory.DEFENSIVE,
        false,
        false,
        null,
        0, 25,
        "{value}"
    ),

    // ==================== RÉSISTANCES ÉLÉMENTAIRES ====================
    
    FIRE_RESISTANCE(
        "Résist. Feu",
        "🔥",
        "§6",
        StatCategory.RESISTANCE,
        false,
        true,
        null,
        0, 50,
        "{value}%"
    ),
    
    ICE_RESISTANCE(
        "Résist. Glace",
        "❄",
        "§b",
        StatCategory.RESISTANCE,
        false,
        true,
        null,
        0, 50,
        "{value}%"
    ),
    
    LIGHTNING_RESISTANCE(
        "Résist. Foudre",
        "⚡",
        "§e",
        StatCategory.RESISTANCE,
        false,
        true,
        null,
        0, 50,
        "{value}%"
    ),
    
    POISON_RESISTANCE(
        "Résist. Poison",
        "☠",
        "§2",
        StatCategory.RESISTANCE,
        false,
        true,
        null,
        0, 50,
        "{value}%"
    ),

    // ==================== STATS UTILITAIRES ====================
    
    MOVEMENT_SPEED(
        "Vitesse",
        "➜",
        "§f",
        StatCategory.UTILITY,
        false,
        true,
        Attribute.MOVEMENT_SPEED,
        -5, 100,  // Max 100% pour bottes Exalted zone 50
        "{value}%"
    ),
    
    KNOCKBACK_RESISTANCE(
        "Résist. Recul",
        "⬛",
        "§8",
        StatCategory.UTILITY,
        false,
        true,
        Attribute.KNOCKBACK_RESISTANCE,
        0, 30,
        "{value}%"
    ),
    
    LUCK(
        "Chance",
        "☘",
        "§a",
        StatCategory.UTILITY,
        false,
        true,
        Attribute.LUCK,
        0, 25,
        "+{value}%"
    ),
    
    XP_BONUS(
        "Bonus XP",
        "✧",
        "§b",
        StatCategory.UTILITY,
        false,
        true,
        null,
        0, 50,
        "+{value}%"
    ),
    
    POINTS_BONUS(
        "Bonus Points",
        "⚡",
        "§6",
        StatCategory.UTILITY,
        false,
        true,
        null,
        0, 50,
        "+{value}%"
    ),

    // ==================== STATS MOMENTUM ====================
    
    STREAK_DAMAGE_BONUS(
        "Bonus Streak",
        "🔥",
        "§c",
        StatCategory.MOMENTUM,
        false,
        true,
        null,
        0, 5,
        "+{value}%/kill"
    ),
    
    COMBO_SPEED_BONUS(
        "Bonus Combo",
        "⚡",
        "§e",
        StatCategory.MOMENTUM,
        false,
        true,
        null,
        0, 10,
        "+{value}%/combo"
    ),
    
    FEVER_DURATION_BONUS(
        "Durée Fever",
        "🔥",
        "§6",
        StatCategory.MOMENTUM,
        false,
        true,
        null,
        0, 100,
        "+{value}%"
    ),
    
    FEVER_DAMAGE_BONUS(
        "Bonus Fever",
        "🔥",
        "§6",
        StatCategory.MOMENTUM,
        false,
        true,
        null,
        0, 75,
        "+{value}%"
    ),

    // ==================== STATS DE GROUPE ====================
    
    PARTY_BONUS(
        "Bonus Groupe",
        "♦",
        "§d",
        StatCategory.GROUP,
        false,
        true,
        null,
        0, 50,
        "+{value}%"
    ),
    
    PARTY_DAMAGE_SHARE(
        "Partage Dégâts",
        "♦",
        "§d",
        StatCategory.GROUP,
        false,
        true,
        null,
        0, 25,
        "{value}%"
    ),
    
    PARTY_HEAL_ON_KILL(
        "Soin Groupe",
        "❤",
        "§a",
        StatCategory.GROUP,
        false,
        false,
        null,
        0, 5,
        "+{value} PV"
    ),

    // ==================== STATS D'EXÉCUTION ====================
    
    EXECUTE_DAMAGE(
        "Dégâts Exécution",
        "☠",
        "§4",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 100,
        "+{value}%"
    ),
    
    EXECUTE_THRESHOLD(
        "Seuil Exécution",
        "☠",
        "§4",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 15,
        "<{value}% PV"
    ),

    // ==================== STATS DE CHANCE ====================
    
    DOUBLE_LOOT_CHANCE(
        "Double Loot",
        "☘",
        "§a",
        StatCategory.UTILITY,
        false,
        true,
        null,
        0, 20,
        "{value}%"
    ),
    
    LEGENDARY_DROP_BONUS(
        "Chance Légendaire",
        "★",
        "§6",
        StatCategory.UTILITY,
        false,
        true,
        null,
        0, 100,
        "+{value}%"
    ),

    // ==================== STATS SPÉCIALES ====================
    
    CHEAT_DEATH_CHANCE(
        "Chance Survie",
        "❤",
        "§c",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 15,
        "{value}%"
    ),
    
    REVIVE_DAMAGE_BOOST(
        "Boost Résurrection",
        "⚔",
        "§c",
        StatCategory.OFFENSIVE,
        false,
        true,
        null,
        0, 150,
        "+{value}%"
    ),

    // ==================== NOUVELLES STATS DÉFENSIVES (PATCH) ====================

    STUN_RESISTANCE(
        "Résist. Étourdissement",
        "◎",
        "§7",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 50,
        "{value}%"
    ),

    LOW_HEALTH_DAMAGE_REDUCTION(
        "Protection Critique",
        "❤",
        "§4",
        StatCategory.DEFENSIVE,
        false,
        true,
        null,
        0, 40,
        "-{value}% <30% PV"
    ),

    LOW_HEALTH_REGEN(
        "Regen Critique",
        "❤",
        "§a",
        StatCategory.DEFENSIVE,
        false,
        false,
        null,
        0, 5,
        "+{value}/s <30% PV"
    );

    private final String displayName;
    private final String icon;
    private final String color;
    private final StatCategory category;
    private final boolean isBaseStat;        // Peut être une stat de base de l'item
    private final boolean isPercentage;      // Affichage en pourcentage
    private final Attribute bukkitAttribute; // Attribut Bukkit correspondant (peut être null)
    private final double minValue;           // Valeur minimum possible
    private final double maxValue;           // Valeur maximum possible
    private final String displayFormat;      // Format d'affichage

    StatType(String displayName, String icon, String color, StatCategory category,
             boolean isBaseStat, boolean isPercentage, Attribute bukkitAttribute,
             double minValue, double maxValue, String displayFormat) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
        this.category = category;
        this.isBaseStat = isBaseStat;
        this.isPercentage = isPercentage;
        this.bukkitAttribute = bukkitAttribute;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.displayFormat = displayFormat;
    }

    /**
     * Formate une valeur pour l'affichage
     */
    public String formatValue(double value) {
        String formatted;
        if (isPercentage || value == (int) value) {
            formatted = String.valueOf((int) value);
        } else {
            formatted = String.format("%.1f", value);
        }
        
        String result = displayFormat.replace("{value}", formatted);
        
        // Ajouter le signe + si positif et pas déjà présent
        if (value > 0 && !result.startsWith("+") && !result.startsWith("-")) {
            result = "+" + result;
        }
        
        return result;
    }

    /**
     * Obtient la ligne d'affichage complète
     */
    public String getDisplayLine(double value) {
        return color + icon + " " + displayName + ": §f" + formatValue(value);
    }

    /**
     * Obtient la ligne d'affichage pour le lore
     */
    public String getLoreLine(double value) {
        String sign = value >= 0 ? "§a" : "§c";
        return "§7" + icon + " " + displayName + ": " + sign + formatValue(value);
    }

    /**
     * Roule une valeur aléatoire dans la plage
     */
    public double rollValue() {
        return minValue + Math.random() * (maxValue - minValue);
    }

    /**
     * Roule une valeur avec un bonus de rareté
     */
    public double rollValue(double rarityBonus) {
        double base = rollValue();
        return base * (1 + rarityBonus);
    }

    /**
     * Vérifie si la valeur est proche du max (god roll)
     */
    public boolean isGodRoll(double value) {
        double threshold = minValue + (maxValue - minValue) * 0.9;
        return value >= threshold;
    }

    /**
     * Catégories de stats
     */
    public enum StatCategory {
        OFFENSIVE("Offensif", "§c"),
        DEFENSIVE("Défensif", "§9"),
        ELEMENTAL("Élémentaire", "§d"),
        RESISTANCE("Résistance", "§e"),
        UTILITY("Utilitaire", "§a"),
        MOMENTUM("Momentum", "§6"),
        GROUP("Groupe", "§d");

        @Getter
        private final String displayName;
        @Getter
        private final String color;

        StatCategory(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }
}
