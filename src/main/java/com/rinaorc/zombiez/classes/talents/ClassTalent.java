package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

/**
 * Représente un talent de classe simplifié
 * Les talents sont organisés en 2 branches: OFFENSE et DEFENSE
 * Progression linéaire facile à comprendre
 */
@Getter
public class ClassTalent {

    private final String id;
    private final String name;
    private final String description;
    private final ClassType classType;
    private final TalentBranch branch;
    private final int tier;          // Position dans l'arbre (1-5)
    private final int maxLevel;      // Niveau maximum du talent (1-3)
    private final Material icon;
    private final String prerequisiteId;  // Talent requis avant celui-ci
    private final int pointCost;     // Points par niveau

    // Effets du talent
    private final TalentEffect effectType;
    private final double baseValue;      // Valeur au niveau 1
    private final double valuePerLevel;  // Valeur ajoutée par niveau

    public ClassTalent(String id, String name, String description,
                       ClassType classType, TalentBranch branch, int tier, int maxLevel,
                       Material icon, String prerequisiteId, int pointCost,
                       TalentEffect effectType, double baseValue, double valuePerLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.classType = classType;
        this.branch = branch;
        this.tier = tier;
        this.maxLevel = maxLevel;
        this.icon = icon;
        this.prerequisiteId = prerequisiteId;
        this.pointCost = pointCost;
        this.effectType = effectType;
        this.baseValue = baseValue;
        this.valuePerLevel = valuePerLevel;
    }

    /**
     * Calcule la valeur totale pour un niveau de talent donné
     */
    public double getValueAtLevel(int level) {
        if (level <= 0) return 0;
        if (level > maxLevel) level = maxLevel;
        return baseValue + (valuePerLevel * (level - 1));
    }

    /**
     * Génère la description avec la valeur actuelle
     */
    public String getDescriptionAtLevel(int level) {
        double value = getValueAtLevel(level);
        return description.replace("{value}", String.format("%.0f", value))
                         .replace("{value_int}", String.valueOf((int) value));
    }

    /**
     * Génère le lore complet pour l'affichage GUI
     */
    public List<String> getLore(int currentLevel, int availablePoints) {
        return new java.util.ArrayList<>(List.of(
            "",
            branch.getColor() + branch.getDisplayName(),
            "",
            getDescriptionAtLevel(Math.max(1, currentLevel)),
            "",
            currentLevel > 0
                ? "§7Niveau: " + branch.getColor() + currentLevel + "§7/" + maxLevel
                : "§8Non débloqué",
            "",
            currentLevel < maxLevel
                ? (availablePoints >= pointCost
                    ? "§e▶ Clic pour améliorer (§6" + pointCost + " point" + (pointCost > 1 ? "s" : "") + "§e)"
                    : "§cPoints insuffisants (§4" + pointCost + "§c requis)")
                : "§a✓ Maximum atteint!"
        ));
    }

    /**
     * Vérifie si ce talent est un talent capstone (tier 5)
     */
    public boolean isCapstone() {
        return tier == 5;
    }

    /**
     * Branches des arbres de talents - Simplifié à 2 branches
     */
    @Getter
    public enum TalentBranch {
        OFFENSE("Combat", "§c", Material.DIAMOND_SWORD, "Améliore vos dégâts et attaques"),
        DEFENSE("Survie", "§6", Material.GOLDEN_APPLE, "Améliore votre défense et utilité");

        private final String displayName;
        private final String color;
        private final Material icon;
        private final String description;

        TalentBranch(String displayName, String color, Material icon, String description) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
            this.description = description;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Types d'effets des talents - Simplifié
     */
    public enum TalentEffect {
        // === OFFENSIFS ===
        DAMAGE_PERCENT,         // +% dégâts
        CRIT_CHANCE,            // +% chance critique
        CRIT_DAMAGE,            // +% dégâts critiques
        HEADSHOT_DAMAGE,        // +% dégâts headshot
        EXECUTE_DAMAGE,         // +% dégâts aux cibles low HP
        CLEAVE_TARGETS,         // +nombre d'ennemis touchés
        PIERCE_TARGETS,         // +nombre d'ennemis traversés
        RAGE_DAMAGE,            // +% dégâts quand low HP
        GUARANTEED_CRIT,        // Critique garanti
        SKILL_POWER,            // +% puissance des compétences
        AOE_RADIUS,             // +% rayon des AoE
        DOT_DAMAGE,             // +% dégâts sur la durée

        // === DÉFENSIFS ===
        HEALTH_PERCENT,         // +% HP max
        DAMAGE_REDUCTION,       // +% réduction de dégâts
        DODGE_CHANCE,           // +% chance d'esquive
        LIFESTEAL,              // +% vol de vie
        REGEN_PERCENT,          // +% régénération de vie
        LAST_STAND,             // Survit à un coup fatal
        RESURRECT,              // Ressuscite après mort

        // === UTILITAIRES ===
        MOVEMENT_SPEED,         // +% vitesse de déplacement
        COOLDOWN_REDUCTION,     // -% temps de recharge
        STEALTH_DAMAGE,         // +% dégâts depuis furtivité
        STEALTH_ON_KILL,        // Invisibilité après kill
        ENERGY_MAX,             // +énergie max
        ENERGY_REGEN,           // +% régénération d'énergie
        DAMAGE_TO_ENERGY,       // Dégâts absorbés en énergie
        SPELL_LIFESTEAL,        // +% vol de vie des sorts
        ULTIMATE_RESET          // Reset du cooldown ultime
    }
}
