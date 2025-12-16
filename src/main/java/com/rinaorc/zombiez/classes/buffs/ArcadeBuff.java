package com.rinaorc.zombiez.classes.buffs;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Système de buffs arcade simplifié
 * À chaque niveau de classe, le joueur choisit 1 buff parmi 3 proposés.
 * Les buffs sont permanents et peuvent se cumuler (jusqu'à un maximum).
 */
@Getter
public class ArcadeBuff {

    private final String id;
    private final String name;
    private final String description;
    private final BuffCategory category;
    private final BuffRarity rarity;
    private final Material icon;
    private final ClassType preferredClass;  // null = universel

    // Effet
    private final BuffEffect effect;
    private final double value;
    private final int maxStacks;

    public ArcadeBuff(String id, String name, String description,
                      BuffCategory category, BuffRarity rarity, Material icon,
                      ClassType preferredClass, BuffEffect effect, double value, int maxStacks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.icon = icon;
        this.preferredClass = preferredClass;
        this.effect = effect;
        this.value = value;
        this.maxStacks = maxStacks;
    }

    /**
     * Calcule la valeur totale pour un nombre de stacks
     */
    public double getTotalValue(int stacks) {
        return value * Math.min(stacks, maxStacks);
    }

    /**
     * Formate la description avec la valeur
     */
    public String getFormattedDescription() {
        String formatted = effect.isPercent()
            ? String.format("%.0f%%", value)
            : String.format("%.0f", value);
        return description.replace("{value}", formatted);
    }

    /**
     * Génère le lore pour l'affichage GUI
     */
    public List<String> getLore(int currentStacks) {
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add(rarity.getColor() + "● " + rarity.getDisplayName());
        lore.add("");
        lore.add("§7" + getFormattedDescription());
        lore.add("");

        if (currentStacks > 0) {
            String total = effect.isPercent()
                ? String.format("+%.0f%%", getTotalValue(currentStacks))
                : String.format("+%.0f", getTotalValue(currentStacks));
            lore.add("§aActuel: §f" + currentStacks + "x §7(" + total + " total)");
        } else {
            lore.add("§8Pas encore obtenu");
        }

        lore.add("");

        if (currentStacks < maxStacks) {
            lore.add("§e▶ Clic pour sélectionner");
        } else {
            lore.add("§a✓ Maximum atteint");
        }

        if (preferredClass != null) {
            lore.add("");
            lore.add("§9Bonus pour: " + preferredClass.getColoredName());
        }

        return lore;
    }

    /**
     * Catégories de buffs
     */
    @Getter
    public enum BuffCategory {
        OFFENSE("Offensive", "§c"),
        DEFENSE("Défensive", "§6"),
        UTILITY("Utilitaire", "§b");

        private final String displayName;
        private final String color;

        BuffCategory(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /**
     * Raretés des buffs (affecte la fréquence)
     */
    @Getter
    public enum BuffRarity {
        COMMON("Commun", "§f", 50),
        UNCOMMON("Peu commun", "§a", 35),
        RARE("Rare", "§9", 15);

        private final String displayName;
        private final String color;
        private final int weight;

        BuffRarity(String displayName, String color, int weight) {
            this.displayName = displayName;
            this.color = color;
            this.weight = weight;
        }
    }

    /**
     * Types d'effets simplifié
     */
    @Getter
    public enum BuffEffect {
        // Offensifs
        DAMAGE(true),           // +% dégâts
        CRIT_CHANCE(true),      // +% chance critique
        CRIT_DAMAGE(true),      // +% dégâts critiques
        LIFESTEAL(true),        // +% vol de vie

        // Défensifs
        HEALTH(true),           // +% HP max
        DAMAGE_REDUCTION(true), // +% réduction de dégâts
        ARMOR(true),            // +% réduction dégâts (alias)
        REGEN(true),            // +% régénération
        DODGE(true),            // +% chance d'esquive

        // Utilitaires
        SPEED(true),            // +% vitesse
        COOLDOWN(true),         // -% cooldown
        ENERGY(true),           // +% régén énergie
        XP(true);               // +% XP gagné

        private final boolean percent;

        BuffEffect(boolean percent) {
            this.percent = percent;
        }
    }
}
