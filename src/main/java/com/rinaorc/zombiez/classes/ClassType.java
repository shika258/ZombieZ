package com.rinaorc.zombiez.classes;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Système de classes simplifié - 3 classes accessibles et distinctes
 * Chaque classe a une identité claire et un style de jeu unique
 */
@Getter
public enum ClassType {

    /**
     * GUERRIER - Le Tank Implacable
     * ★☆☆ Difficulté: Facile
     * Style: Mêlée, survie, vol de vie
     * Idéal pour: Nouveaux joueurs, solo, front line
     */
    GUERRIER(
        "Guerrier",
        "§c",
        Material.NETHERITE_CHESTPLATE,
        "§7Tank brutal spécialisé en combat rapproché",
        new String[]{
            "§c⚔ §7+20% dégâts en mêlée",
            "§c❤ §7+25% points de vie max",
            "§c✦ §710% vol de vie naturel"
        },
        "§e★§7☆☆ Facile",
        // Stats de base
        1.20,   // Multiplicateur de dégâts mêlée
        0.95,   // Vitesse légèrement réduite
        1.25,   // +25% HP
        0.90,   // Moins de critiques
        0.10    // Vol de vie naturel
    ),

    /**
     * CHASSEUR - Le Tireur Mortel
     * ★★☆ Difficulté: Moyen
     * Style: Distance, critiques, mobilité
     * Idéal pour: Joueurs agressifs, kill à distance
     */
    CHASSEUR(
        "Chasseur",
        "§a",
        Material.BOW,
        "§7Tireur d'élite maîtrisant les coups critiques",
        new String[]{
            "§a⚡ §7+30% chance de critique",
            "§a✧ §7+50% dégâts critiques",
            "§a➤ §7+15% vitesse de déplacement"
        },
        "§e★★§7☆ Moyen",
        // Stats de base
        1.05,   // Dégâts légèrement augmentés
        1.15,   // Plus rapide
        0.90,   // Moins de HP
        1.30,   // +30% critiques
        0.0     // Pas de vol de vie
    ),

    /**
     * OCCULTISTE - Le Mage Dévastateur
     * ★★★ Difficulté: Expert
     * Style: AoE, sorts puissants, contrôle de zone
     * Idéal pour: Joueurs expérimentés, hordes, groupe
     */
    OCCULTISTE(
        "Occultiste",
        "§5",
        Material.AMETHYST_SHARD,
        "§7Mage sombre aux pouvoirs dévastateurs",
        new String[]{
            "§5✦ §7+40% dégâts des compétences",
            "§5⏱ §7-25% temps de recharge",
            "§5☠ §7Dégâts AoE améliorés"
        },
        "§e★★★ §7Expert",
        // Stats de base
        1.40,   // +40% dégâts de compétences
        1.0,    // Vitesse normale
        0.80,   // Moins de HP (verre cannon)
        1.10,   // Critiques légèrement augmentés
        0.0     // Pas de vol de vie
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final String description;
    private final String[] bonusDescription;
    private final String difficultyDisplay;

    // Multiplicateurs de base de la classe
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double healthMultiplier;
    private final double critMultiplier;
    private final double lifesteal;

    ClassType(String displayName, String color, Material icon, String description,
              String[] bonusDescription, String difficultyDisplay,
              double damageMultiplier, double speedMultiplier,
              double healthMultiplier, double critMultiplier, double lifesteal) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.bonusDescription = bonusDescription;
        this.difficultyDisplay = difficultyDisplay;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.critMultiplier = critMultiplier;
        this.lifesteal = lifesteal;
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
            // Compatibilité avec anciens noms
            return switch (id.toLowerCase()) {
                case "commando", "berserker" -> GUERRIER;
                case "scout", "sniper" -> CHASSEUR;
                case "medic", "engineer" -> OCCULTISTE;
                default -> null;
            };
        }
    }

    /**
     * Vérifie si cette classe est orientée mêlée
     */
    public boolean isMeleeClass() {
        return this == GUERRIER;
    }

    /**
     * Vérifie si cette classe est orientée distance
     */
    public boolean isRangedClass() {
        return this == CHASSEUR;
    }

    /**
     * Vérifie si cette classe est orientée magie/compétences
     */
    public boolean isMagicClass() {
        return this == OCCULTISTE;
    }

    /**
     * Obtient le niveau de difficulté (1-3)
     */
    public int getDifficultyLevel() {
        return switch (this) {
            case GUERRIER -> 1;
            case CHASSEUR -> 2;
            case OCCULTISTE -> 3;
        };
    }
}
