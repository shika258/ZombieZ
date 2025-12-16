package com.rinaorc.zombiez.classes;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Classes jouables - Inspirées de Fallout, DayZ et jeux de survie
 * Chaque classe a un style de jeu unique avec talents, compétences et armes exclusives
 */
@Getter
public enum ClassType {

    /**
     * COMMANDO - Le Soldat d'Élite
     * DPS brut, armes lourdes, contrôle de zone
     * Inspiré des Heavy Gunners de Fallout
     */
    COMMANDO(
        "Commando",
        "§c",
        Material.IRON_CHESTPLATE,
        "§7Soldat d'élite spécialisé dans les armes lourdes",
        new String[]{
            "§8• §7+15% dégâts avec armes lourdes",
            "§8• §7+10% résistance aux explosions",
            "§8• §7Accès aux armes militaires exclusives"
        },
        // Stats de base
        1.15,   // Multiplicateur de dégâts
        1.0,    // Multiplicateur de vitesse
        1.10,   // Multiplicateur de HP
        0.95,   // Multiplicateur de critique
        1.0     // Multiplicateur de loot
    ),

    /**
     * ÉCLAIREUR (SCOUT) - Le Fantôme
     * Mobilité, critiques, embuscades
     * Inspiré des survivants furtifs de DayZ
     */
    SCOUT(
        "Éclaireur",
        "§a",
        Material.LEATHER_BOOTS,
        "§7Survivant agile spécialisé dans la reconnaissance",
        new String[]{
            "§8• §7+20% vitesse de déplacement",
            "§8• §7+15% chance de critique",
            "§8• §7Accès aux armes silencieuses exclusives"
        },
        0.95,   // Moins de dégâts bruts
        1.20,   // Plus rapide
        0.90,   // Moins de HP
        1.15,   // Plus de critiques
        1.10    // Plus de loot
    ),

    /**
     * MÉDIC - Le Soigneur de Guerre
     * Soins, support d'équipe, survie
     * Inspiré des Medics de Fallout
     */
    MEDIC(
        "Médic",
        "§d",
        Material.SPLASH_POTION,
        "§7Spécialiste médical expert en survie",
        new String[]{
            "§8• §7+25% efficacité des soins",
            "§8• §7+20% régénération passive",
            "§8• §7Accès aux armes médicales exclusives"
        },
        0.90,   // Moins de dégâts
        1.05,   // Légèrement plus rapide
        1.15,   // Plus de HP
        1.0,    // Critiques normaux
        1.0     // Loot normal
    ),

    /**
     * INGÉNIEUR - Le Technicien
     * Tourelles, gadgets, support technologique
     * Inspiré des crafters de Fallout
     */
    ENGINEER(
        "Ingénieur",
        "§e",
        Material.REDSTONE,
        "§7Expert en technologie et constructions défensives",
        new String[]{
            "§8• §7Tourelles et gadgets exclusifs",
            "§8• §7+15% dégâts des constructions",
            "§8• §7Accès aux armes technologiques exclusives"
        },
        0.95,   // Dégâts légèrement réduits
        1.0,    // Vitesse normale
        1.05,   // Légèrement plus de HP
        1.0,    // Critiques normaux
        1.15    // Plus de loot (récupération)
    ),

    /**
     * BERSERKER - La Bête de Guerre
     * Mêlée pure, rage, drain de vie
     * Style arcade brutal
     */
    BERSERKER(
        "Berserker",
        "§4",
        Material.NETHERITE_AXE,
        "§7Guerrier brutal qui se nourrit du combat",
        new String[]{
            "§8• §7+25% dégâts en mêlée",
            "§8• §7+10% vol de vie naturel",
            "§8• §7Accès aux armes de mêlée exclusives"
        },
        1.25,   // Beaucoup plus de dégâts mêlée
        0.95,   // Légèrement plus lent
        1.20,   // Beaucoup plus de HP
        0.90,   // Moins de critiques
        0.90    // Moins de loot
    ),

    /**
     * SNIPER - Le Tireur d'Élite
     * Précision, headshots, distance
     * Inspiré des snipers de DayZ
     */
    SNIPER(
        "Sniper",
        "§b",
        Material.SPYGLASS,
        "§7Tireur d'élite maître des kills à distance",
        new String[]{
            "§8• §7+50% dégâts de headshot",
            "§8• §7+25% portée effective",
            "§8• §7Accès aux armes de précision exclusives"
        },
        1.10,   // Plus de dégâts
        0.90,   // Plus lent
        0.85,   // Moins de HP
        1.25,   // Beaucoup plus de critiques
        1.05    // Légèrement plus de loot
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final String description;
    private final String[] bonusDescription;

    // Multiplicateurs de base de la classe
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double healthMultiplier;
    private final double critMultiplier;
    private final double lootMultiplier;

    ClassType(String displayName, String color, Material icon, String description,
              String[] bonusDescription, double damageMultiplier, double speedMultiplier,
              double healthMultiplier, double critMultiplier, double lootMultiplier) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.bonusDescription = bonusDescription;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.critMultiplier = critMultiplier;
        this.lootMultiplier = lootMultiplier;
    }

    /**
     * Obtient le nom coloré de la classe
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtient l'ID de la classe (pour la base de données)
     */
    public String getId() {
        return name().toLowerCase();
    }

    /**
     * Obtient une classe depuis son ID
     */
    public static ClassType fromId(String id) {
        if (id == null) return null;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Vérifie si cette classe est orientée mêlée
     */
    public boolean isMeleeClass() {
        return this == BERSERKER || this == COMMANDO;
    }

    /**
     * Vérifie si cette classe est orientée distance
     */
    public boolean isRangedClass() {
        return this == SNIPER || this == SCOUT;
    }

    /**
     * Vérifie si cette classe est orientée support
     */
    public boolean isSupportClass() {
        return this == MEDIC || this == ENGINEER;
    }
}
