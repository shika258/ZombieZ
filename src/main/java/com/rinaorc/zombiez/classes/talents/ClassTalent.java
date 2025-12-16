package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

/**
 * Représente un talent de classe
 * Les talents sont organisés en arbres avec des prérequis
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
        return description.replace("{value}", String.format("%.1f", value))
                         .replace("{value_int}", String.valueOf((int) value));
    }

    /**
     * Génère le lore complet pour l'affichage GUI
     */
    public List<String> getLore(int currentLevel, int availablePoints) {
        return new java.util.ArrayList<>(List.of(
            "",
            "§7" + branch.getColor() + branch.getDisplayName(),
            "",
            getDescriptionAtLevel(Math.max(1, currentLevel)),
            "",
            currentLevel > 0
                ? "§aActuel: §f" + currentLevel + "/" + maxLevel
                : "§8Non débloqué",
            "",
            currentLevel < maxLevel
                ? (availablePoints >= pointCost
                    ? "§e▶ Clic pour améliorer (" + pointCost + " points)"
                    : "§cPoints insuffisants (" + pointCost + " requis)")
                : "§a✓ Niveau maximum atteint"
        ));
    }

    /**
     * Branches des arbres de talents
     */
    @Getter
    public enum TalentBranch {
        // Branches communes (chaque classe a ces 3 branches)
        OFFENSE("Offensive", "§c", Material.DIAMOND_SWORD, "Améliore vos capacités offensives"),
        DEFENSE("Défensive", "§a", Material.SHIELD, "Améliore votre survie"),
        SPECIALTY("Spécialité", "§6", Material.NETHER_STAR, "Talents uniques de votre classe");

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
     * Types d'effets des talents
     */
    public enum TalentEffect {
        // Offensifs
        DAMAGE_PERCENT,         // +% dégâts
        DAMAGE_FLAT,            // +dégâts fixes
        CRIT_CHANCE,            // +% chance critique
        CRIT_DAMAGE,            // +% dégâts critiques
        ATTACK_SPEED,           // +% vitesse d'attaque
        ARMOR_PENETRATION,      // +% pénétration d'armure
        HEADSHOT_DAMAGE,        // +% dégâts headshot
        EXECUTE_DAMAGE,         // +% dégâts sur cibles low HP

        // Défensifs
        HEALTH_PERCENT,         // +% HP max
        HEALTH_FLAT,            // +HP fixe
        DAMAGE_REDUCTION,       // +% réduction de dégâts
        DODGE_CHANCE,           // +% chance d'esquive
        BLOCK_CHANCE,           // +% chance de bloquer
        LIFESTEAL,              // +% vol de vie
        REGEN_PERCENT,          // +% régénération
        THORNS,                 // +% dégâts de renvoi

        // Utilitaires
        MOVEMENT_SPEED,         // +% vitesse de déplacement
        LOOT_CHANCE,            // +% chance de loot
        XP_BONUS,               // +% XP gagné
        COOLDOWN_REDUCTION,     // -% temps de recharge compétences
        SKILL_DURATION,         // +% durée des compétences

        // Spéciaux (propres à certaines classes)
        TURRET_DAMAGE,          // +% dégâts des tourelles (Ingénieur)
        TURRET_DURATION,        // +durée des tourelles (Ingénieur)
        HEAL_POWER,             // +% puissance de soin (Médic)
        HEAL_RANGE,             // +portée des soins (Médic)
        RAGE_GENERATION,        // +% génération de rage (Berserker)
        RAGE_DAMAGE,            // +% dégâts en rage (Berserker)
        STEALTH_DURATION,       // +durée invisibilité (Éclaireur)
        STEALTH_DAMAGE,         // +% dégâts depuis furtivité (Éclaireur)
        SUPPRESSION_RANGE,      // +portée suppression (Commando)
        EXPLOSIVE_DAMAGE,       // +% dégâts explosifs (Commando)
        SCOPE_ZOOM,             // +% zoom (Sniper)
        MARK_DURATION,          // +durée des marques (Sniper)

        // Passifs spéciaux
        UNLOCK_SKILL,           // Débloque une compétence active
        UNLOCK_WEAPON           // Débloque une arme de classe
    }
}
