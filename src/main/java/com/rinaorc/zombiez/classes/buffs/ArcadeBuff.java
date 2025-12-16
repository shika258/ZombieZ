package com.rinaorc.zombiez.classes.buffs;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.List;

/**
 * Représente un buff arcade sélectionnable au level up de classe
 * Style MegaBonk: à chaque level up, le joueur choisit 1 buff parmi 3 proposés
 * Les buffs sont permanents mais légers et peuvent se cumuler
 */
@Getter
public class ArcadeBuff {

    private final String id;
    private final String name;
    private final String description;
    private final BuffCategory category;
    private final BuffRarity rarity;
    private final Material icon;
    private final ClassType preferredClass;  // Classe qui voit ce buff plus souvent (null = toutes)

    // Effet du buff
    private final BuffEffect effectType;
    private final double baseValue;
    private final int maxStacks;     // Nombre maximum de fois qu'on peut prendre ce buff

    public ArcadeBuff(String id, String name, String description, BuffCategory category,
                      BuffRarity rarity, Material icon, ClassType preferredClass,
                      BuffEffect effectType, double baseValue, int maxStacks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.icon = icon;
        this.preferredClass = preferredClass;
        this.effectType = effectType;
        this.baseValue = baseValue;
        this.maxStacks = maxStacks;
    }

    /**
     * Calcule la valeur totale pour un nombre de stacks
     */
    public double getValueAtStacks(int stacks) {
        return baseValue * Math.min(stacks, maxStacks);
    }

    /**
     * Génère la description formatée
     */
    public String getFormattedDescription(int currentStacks) {
        String value = effectType.isPercent()
            ? String.format("%.1f%%", baseValue)
            : String.format("%.0f", baseValue);

        return description.replace("{value}", value);
    }

    /**
     * Génère le lore pour l'affichage GUI
     */
    public List<String> getLore(int currentStacks) {
        double totalValue = getValueAtStacks(currentStacks);

        return List.of(
            "",
            rarity.getColor() + rarity.getDisplayName(),
            "",
            "§7" + getFormattedDescription(currentStacks),
            "",
            currentStacks > 0
                ? "§aActuel: §f" + currentStacks + "x (" + formatValue(totalValue) + " total)"
                : "§8Pas encore obtenu",
            "",
            currentStacks < maxStacks
                ? "§e▶ Clic pour sélectionner"
                : "§c✗ Maximum atteint (" + maxStacks + "x)",
            "",
            preferredClass != null
                ? "§9Bonus pour: " + preferredClass.getColoredName()
                : "§7Universel"
        );
    }

    private String formatValue(double value) {
        return effectType.isPercent()
            ? String.format("+%.1f%%", value)
            : String.format("+%.0f", value);
    }

    /**
     * Catégories de buffs
     */
    @Getter
    public enum BuffCategory {
        OFFENSE("Offensive", "§c", Material.DIAMOND_SWORD),
        DEFENSE("Défensive", "§a", Material.SHIELD),
        UTILITY("Utilitaire", "§e", Material.COMPASS),
        HYBRID("Hybride", "§d", Material.NETHER_STAR);

        private final String displayName;
        private final String color;
        private final Material icon;

        BuffCategory(String displayName, String color, Material icon) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Raretés des buffs (affecte la fréquence d'apparition)
     */
    @Getter
    public enum BuffRarity {
        COMMON("Commun", "§f", 50),      // 50% chance pool
        UNCOMMON("Peu commun", "§a", 30), // 30% chance pool
        RARE("Rare", "§9", 15),           // 15% chance pool
        EPIC("Épique", "§d", 5);          // 5% chance pool

        private final String displayName;
        private final String color;
        private final int weight;

        BuffRarity(String displayName, String color, int weight) {
            this.displayName = displayName;
            this.color = color;
            this.weight = weight;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Types d'effets de buffs
     */
    @Getter
    public enum BuffEffect {
        // Offensifs
        DAMAGE_PERCENT(true),       // +% dégâts
        DAMAGE_FLAT(false),         // +dégâts fixes
        CRIT_CHANCE(true),          // +% chance critique
        CRIT_DAMAGE(true),          // +% dégâts critiques
        ATTACK_SPEED(true),         // +% vitesse d'attaque
        ARMOR_PEN(true),            // +% pénétration armure
        HEADSHOT_DMG(true),         // +% dégâts headshot
        LIFESTEAL(true),            // +% vol de vie

        // Défensifs
        HEALTH_PERCENT(true),       // +% HP max
        HEALTH_FLAT(false),         // +HP fixe
        DAMAGE_REDUCTION(true),     // +% réduction dégâts
        DODGE_CHANCE(true),         // +% chance esquive
        REGEN(true),                // +% régénération
        THORNS(true),               // +% dégâts de renvoi

        // Utilitaires
        MOVEMENT_SPEED(true),       // +% vitesse
        LOOT_BONUS(true),           // +% loot
        XP_BONUS(true),             // +% XP
        COOLDOWN_RED(true),         // -% cooldown compétences
        ENERGY_REGEN(true),         // +% régén énergie

        // Spéciaux de classe
        CLASS_BONUS(true),          // +% bonus général de classe
        SKILL_POWER(true),          // +% puissance compétences
        ULTIMATE_CDR(true);         // -% cooldown ultime

        private final boolean percent;

        BuffEffect(boolean percent) {
            this.percent = percent;
        }
    }
}
