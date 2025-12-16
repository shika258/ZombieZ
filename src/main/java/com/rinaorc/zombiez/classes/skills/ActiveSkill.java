package com.rinaorc.zombiez.classes.skills;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.List;

/**
 * Représente une compétence active de classe
 * Chaque classe a 4-6 compétences actives
 * Le joueur peut équiper jusqu'à 3 compétences
 */
@Getter
public class ActiveSkill {

    private final String id;
    private final String name;
    private final String description;
    private final ClassType classType;
    private final SkillSlot slot;        // Slot de compétence (PRIMARY, SECONDARY, ULTIMATE)
    private final Material icon;
    private final int baseCooldown;       // Cooldown en secondes
    private final int manaCost;           // Coût en "énergie de classe"
    private final boolean requiresUnlock; // Nécessite un talent pour débloquer
    private final String unlockTalentId;  // ID du talent qui débloque cette compétence

    // Paramètres de l'effet
    private final SkillEffect effectType;
    private final double baseDamage;
    private final double damageScaling;   // Bonus par niveau de classe
    private final double baseRadius;      // Rayon d'effet (si AoE)
    private final double baseDuration;    // Durée de l'effet (si applicable)
    private final Sound castSound;

    public ActiveSkill(String id, String name, String description, ClassType classType,
                       SkillSlot slot, Material icon, int baseCooldown, int manaCost,
                       boolean requiresUnlock, String unlockTalentId,
                       SkillEffect effectType, double baseDamage, double damageScaling,
                       double baseRadius, double baseDuration, Sound castSound) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.classType = classType;
        this.slot = slot;
        this.icon = icon;
        this.baseCooldown = baseCooldown;
        this.manaCost = manaCost;
        this.requiresUnlock = requiresUnlock;
        this.unlockTalentId = unlockTalentId;
        this.effectType = effectType;
        this.baseDamage = baseDamage;
        this.damageScaling = damageScaling;
        this.baseRadius = baseRadius;
        this.baseDuration = baseDuration;
        this.castSound = castSound;
    }

    /**
     * Calcule les dégâts pour un niveau de classe donné
     */
    public double getDamageAtLevel(int classLevel) {
        return baseDamage + (damageScaling * classLevel);
    }

    /**
     * Génère le lore pour l'affichage GUI
     */
    public List<String> getLore(int classLevel, boolean isEquipped, boolean isUnlocked) {
        double damage = getDamageAtLevel(classLevel);

        return List.of(
            "",
            "§7" + description,
            "",
            "§6Dégâts: §f" + String.format("%.0f", damage),
            baseRadius > 0 ? "§6Rayon: §f" + String.format("%.1f", baseRadius) + " blocs" : "",
            baseDuration > 0 ? "§6Durée: §f" + String.format("%.1f", baseDuration) + "s" : "",
            "",
            "§9Recharge: §f" + baseCooldown + "s",
            "§9Énergie: §f" + manaCost,
            "",
            slot.getColor() + slot.getDisplayName(),
            "",
            !isUnlocked
                ? "§c✗ Talent requis: " + unlockTalentId
                : (isEquipped
                    ? "§a✓ Équipée - Clic pour retirer"
                    : "§e▶ Clic pour équiper")
        ).stream().filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Types de slots de compétence
     */
    @Getter
    public enum SkillSlot {
        PRIMARY("Primaire", "§a", 1),      // Touche 1 - Compétences de base
        SECONDARY("Secondaire", "§e", 2),   // Touche 2 - Compétences intermédiaires
        ULTIMATE("Ultime", "§c", 3);        // Touche 3 - Compétences puissantes

        private final String displayName;
        private final String color;
        private final int slotNumber;

        SkillSlot(String displayName, String color, int slotNumber) {
            this.displayName = displayName;
            this.color = color;
            this.slotNumber = slotNumber;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Types d'effets de compétence
     */
    public enum SkillEffect {
        // Dégâts directs
        SINGLE_TARGET_DAMAGE,    // Dégâts sur une cible
        AOE_DAMAGE,              // Dégâts en zone
        CONE_DAMAGE,             // Dégâts en cône
        LINE_DAMAGE,             // Dégâts en ligne
        DOT_DAMAGE,              // Dégâts sur le temps

        // Soins
        SELF_HEAL,               // Se soigner
        AOE_HEAL,                // Soigner en zone
        HOT_HEAL,                // Soins sur le temps
        RESURRECT,               // Ressusciter

        // Buff/Debuff
        BUFF_SELF,               // Buff sur soi
        BUFF_ALLIES,             // Buff sur alliés
        DEBUFF_ENEMIES,          // Débuff sur ennemis
        STUN,                    // Étourdir
        SLOW,                    // Ralentir
        ROOT,                    // Immobiliser

        // Mobilité
        DASH,                    // Ruée
        TELEPORT,                // Téléportation
        KNOCKBACK,               // Repousser

        // Invocations
        SUMMON_TURRET,           // Invoquer tourelle
        SUMMON_MINION,           // Invoquer créature
        SUMMON_TRAP,             // Poser piège

        // Défensifs
        SHIELD,                  // Bouclier
        INVISIBILITY,            // Invisibilité
        INVULNERABILITY,         // Invulnérabilité temporaire

        // Spéciaux
        EXECUTE,                 // Exécution
        MARK,                    // Marquer une cible
        TRANSFORM                // Transformation
    }
}
