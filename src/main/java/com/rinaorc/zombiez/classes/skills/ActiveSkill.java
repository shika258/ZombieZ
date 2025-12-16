package com.rinaorc.zombiez.classes.skills;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

/**
 * Système de compétences actives simplifié
 * Chaque classe a 4 compétences:
 * - 2 compétences de base (débloquées automatiquement)
 * - 1 compétence avancée (niveau 5 de classe requis)
 * - 1 ultime (niveau 10 de classe requis)
 */
@Getter
public class ActiveSkill {

    private final String id;
    private final String name;
    private final String description;
    private final ClassType classType;
    private final SkillType type;
    private final Material icon;
    private final int cooldown;           // En secondes
    private final int energyCost;
    private final int requiredLevel;      // 0 = débloqué de base

    // Paramètres de l'effet
    private final SkillEffect effectType;
    private final double damage;
    private final double radius;          // Rayon AoE (0 = cible unique)
    private final double duration;        // Durée de l'effet
    private final Sound castSound;

    public ActiveSkill(String id, String name, String description, ClassType classType,
                       SkillType type, Material icon, int cooldown, int energyCost,
                       int requiredLevel, SkillEffect effectType, double damage,
                       double radius, double duration, Sound castSound) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.classType = classType;
        this.type = type;
        this.icon = icon;
        this.cooldown = cooldown;
        this.energyCost = energyCost;
        this.requiredLevel = requiredLevel;
        this.effectType = effectType;
        this.damage = damage;
        this.radius = radius;
        this.duration = duration;
        this.castSound = castSound;
    }

    /**
     * Calcule les dégâts scalés avec le niveau de classe
     */
    public double getScaledDamage(int classLevel) {
        // +5% par niveau de classe
        return damage * (1.0 + (classLevel * 0.05));
    }

    /**
     * Vérifie si la compétence est débloquée pour un niveau donné
     */
    public boolean isUnlocked(int classLevel) {
        return classLevel >= requiredLevel;
    }

    /**
     * Génère le lore pour l'affichage GUI
     */
    public List<String> getLore(int classLevel, boolean isEquipped) {
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.add("§7" + description);
        lore.add("");

        // Stats
        if (damage > 0) {
            lore.add("§c⚔ Dégâts: §f" + String.format("%.0f", getScaledDamage(classLevel)));
        }
        if (radius > 0) {
            lore.add("§e◎ Rayon: §f" + String.format("%.0f", radius) + " blocs");
        }
        if (duration > 0) {
            lore.add("§b⏱ Durée: §f" + String.format("%.0f", duration) + "s");
        }

        lore.add("");
        lore.add("§9⟳ Recharge: §f" + cooldown + "s");
        lore.add("§d✦ Énergie: §f" + energyCost);
        lore.add("");
        lore.add(type.getColor() + "● " + type.getDisplayName());
        lore.add("");

        if (!isUnlocked(classLevel)) {
            lore.add("§c✗ Niveau §4" + requiredLevel + " §crequis");
        } else if (isEquipped) {
            lore.add("§a✓ Équipée");
            lore.add("§7Clic pour retirer");
        } else {
            lore.add("§e▶ Clic pour équiper");
        }

        return lore;
    }

    /**
     * Types de compétences - simplifié
     */
    @Getter
    public enum SkillType {
        BASE("Compétence de base", "§a", 0),
        ADVANCED("Compétence avancée", "§e", 5),
        ULTIMATE("Ultime", "§c", 10);

        private final String displayName;
        private final String color;
        private final int defaultRequiredLevel;

        SkillType(String displayName, String color, int defaultRequiredLevel) {
            this.displayName = displayName;
            this.color = color;
            this.defaultRequiredLevel = defaultRequiredLevel;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Types d'effets simplifié
     */
    public enum SkillEffect {
        // Dégâts
        DAMAGE,              // Dégâts directs
        AOE_DAMAGE,          // Dégâts en zone
        DOT_DAMAGE,          // Dégâts sur le temps

        // Mobilité
        DASH,                // Ruée vers l'avant
        LEAP,                // Saut

        // Défensifs
        HEAL,                // Soins
        SHIELD,              // Bouclier absorbant

        // Buff/Debuff
        BUFF,                // Amélioration temporaire
        STUN,                // Étourdir
        SLOW,                // Ralentir

        // Spéciaux
        SUMMON,              // Invocation
        EXECUTE              // Exécution (bonus dégâts low HP)
    }
}
