package com.rinaorc.zombiez.classes;

import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

/**
 * Système de classes simplifié - 3 classes accessibles et distinctes
 * Chaque classe a une identité claire et un style de jeu unique
 *
 * Équilibrage v2.0 - Stats rééquilibrées et traits de classe ajoutés
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
        "§7Tank brutal au cœur de la mêlée",
        new String[]{
            "§c⚔ §7Spécialiste du corps à corps",
            "§c❤ §7Résistance exceptionnelle",
            "§c✦ §7Récupère de la vie en frappant"
        },
        "§e★§7☆☆ Facile",
        // Stats de base - ÉQUILIBRÉES v2.0
        1.15,   // Multiplicateur de dégâts (+15%)
        0.90,   // Vitesse réduite (-10%)
        1.30,   // +30% HP (tank principal)
        0.85,   // Moins de critiques (-15%)
        0.08,   // Vol de vie naturel (8%)
        // Traits de classe uniques
        new ClassTrait[]{
            new ClassTrait("§c⚔ Brutalité", "§7+25% dégâts mêlée supplémentaires", 0.25),
            new ClassTrait("§c🛡 Cuirasse", "§7-15% dégâts subis", 0.15),
            new ClassTrait("§c💪 Inébranlable", "§7Résistance au recul +50%", 0.50),
            new ClassTrait("§c❤ Vitalité", "§7Régénération +2 HP/5s hors combat", 2.0)
        }
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
        "§7Tireur d'élite rapide et mortel",
        new String[]{
            "§a⚡ §7Coups critiques dévastateurs",
            "§a✧ §7Agilité et esquive",
            "§a➤ §7Maître de la distance"
        },
        "§e★★§7☆ Moyen",
        // Stats de base - ÉQUILIBRÉES v2.0
        1.20,   // Multiplicateur de dégâts (+20%)
        1.20,   // Vitesse augmentée (+20%)
        0.85,   // Moins de HP (-15%)
        1.35,   // +35% critiques
        0.0,    // Pas de vol de vie
        // Traits de classe uniques
        new ClassTrait[]{
            new ClassTrait("§a🎯 Précision", "§7+30% dégâts à distance", 0.30),
            new ClassTrait("§a💨 Vélocité", "§7+15% d'esquive", 0.15),
            new ClassTrait("§a⚡ Adrénaline", "§7Kill = +10% vitesse 3s", 0.10),
            new ClassTrait("§a🏹 Tir Critique", "§7Critiques: +50% dégâts bonus", 0.50)
        }
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
            "§5✦ §7Sorts et effets dévastateurs",
            "§5☠ §7Destruction de masse",
            "§5⚡ §7Canon de verre tactique"
        },
        "§e★★★ §7Expert",
        // Stats de base - ÉQUILIBRÉES v2.0
        1.30,   // +30% dégâts (réduit de 40%)
        0.95,   // Vitesse légèrement réduite (-5%)
        0.75,   // Moins de HP (-25%) - vrai glass cannon
        1.15,   // +15% critiques
        0.03,   // Faible vol de vie (3% - siphon d'âme)
        // Traits de classe uniques
        new ClassTrait[]{
            new ClassTrait("§5✦ Arcane", "§7+40% dégâts de zone (AoE)", 0.40),
            new ClassTrait("§5🔮 Canalisation", "§7-20% cooldown des talents", 0.20),
            new ClassTrait("§5☠ Malédiction", "§7Ennemis touchés: -10% résist.", 0.10),
            new ClassTrait("§5💀 Siphon", "§7Kill = +5% HP max temporaire", 0.05)
        }
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

    // Traits de classe uniques
    private final ClassTrait[] classTraits;

    ClassType(String displayName, String color, Material icon, String description,
              String[] bonusDescription, String difficultyDisplay,
              double damageMultiplier, double speedMultiplier,
              double healthMultiplier, double critMultiplier, double lifesteal,
              ClassTrait[] classTraits) {
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
        this.classTraits = classTraits;
    }

    /**
     * Record représentant un trait de classe unique
     * Chaque classe possède 4 traits qui définissent son identité
     */
    @Getter
    public static class ClassTrait {
        private final String name;
        private final String description;
        private final double value;

        public ClassTrait(String name, String description, double value) {
            this.name = name;
            this.description = description;
            this.value = value;
        }

        /**
         * Retourne le trait formaté pour l'affichage
         */
        public String getFormattedDisplay() {
            return name + "\n  " + description;
        }
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
