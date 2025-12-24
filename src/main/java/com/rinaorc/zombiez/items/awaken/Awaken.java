package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import lombok.Builder;
import lombok.Getter;

/**
 * Représente un Éveil (Awaken) attaché à une arme
 *
 * Un éveil est un modificateur ultra rare qui booste un talent spécifique.
 * Il n'est actif que si le joueur:
 * - Porte l'arme en main principale
 * - Est de la bonne classe
 * - A le talent correspondant sélectionné et actif
 */
@Getter
@Builder
public class Awaken {

    // ==================== IDENTIFICATION ====================

    /**
     * ID unique de l'éveil (format: awaken_{classId}_{talentId})
     */
    private final String id;

    /**
     * Nom d'affichage
     */
    private final String displayName;

    // ==================== CIBLAGE ====================

    /**
     * Classe requise pour activer cet éveil
     */
    private final ClassType requiredClass;

    /**
     * Branche de talents (optionnel - null = toutes les branches)
     */
    private final TalentBranch requiredBranch;

    /**
     * ID du talent que cet éveil booste
     */
    private final String targetTalentId;

    /**
     * Type d'effet du talent ciblé (pour validation)
     */
    private final Talent.TalentEffectType targetEffectType;

    // ==================== MODIFICATEUR ====================

    /**
     * Type de modification appliquée
     */
    private final AwakenModifierType modifierType;

    /**
     * Valeur du modificateur (rollée lors de la génération)
     */
    private final double modifierValue;

    /**
     * Description de l'effet (générée ou custom)
     */
    private final String effectDescription;

    // ==================== TEMPLATE INFO ====================

    /**
     * Si cet éveil utilise un template générique ou est unique
     */
    @Builder.Default
    private final boolean isUnique = false;

    /**
     * Données custom pour les éveils uniques (JSON ou map)
     */
    private final String customData;

    // ==================== MÉTHODES ====================

    /**
     * Vérifie si cet éveil peut être actif pour un joueur
     *
     * @param playerClass Classe du joueur
     * @param playerBranch Branche sélectionnée par le joueur
     * @param activeTalentId ID du talent actif du joueur
     * @return true si l'éveil est compatible
     */
    public boolean isCompatible(ClassType playerClass, TalentBranch playerBranch, String activeTalentId) {
        // Vérifier la classe
        if (requiredClass != null && requiredClass != playerClass) {
            return false;
        }

        // Vérifier la branche (si spécifiée)
        if (requiredBranch != null && requiredBranch != playerBranch) {
            return false;
        }

        // Vérifier le talent
        return targetTalentId != null && targetTalentId.equals(activeTalentId);
    }

    /**
     * Vérifie si la classe est compatible (même si le talent n'est pas actif)
     */
    public boolean isClassCompatible(ClassType playerClass) {
        return requiredClass == null || requiredClass == playerClass;
    }

    /**
     * Génère le lore de l'éveil pour l'affichage sur l'item
     *
     * @param isActive true si l'éveil est actuellement actif
     * @param playerClass Classe du joueur (peut être null)
     * @return Tableau de lignes de lore
     */
    public String[] generateLore(boolean isActive, ClassType playerClass) {
        String statusColor = isActive ? "§a" : "§8";
        String statusText = isActive ? "§a✔ ACTIF" : "§8✖ INACTIF";

        if (!isActive && playerClass != null && requiredClass != playerClass) {
            statusText = "§c✖ Classe: " + requiredClass.getColoredName();
        } else if (!isActive) {
            statusText = "§c✖ Talent requis";
        }

        return new String[]{
            "",
            "§8§m                    ",
            "§d§l✦ ÉVEIL §8- " + statusText,
            statusColor + displayName,
            "§7Talent: §e" + (targetTalentId != null ? formatTalentName(targetTalentId) : "Inconnu"),
            "§7Effet: " + statusColor + effectDescription,
            "§8§m                    "
        };
    }

    /**
     * Génère le lore compact (sans bordures)
     */
    public String[] generateCompactLore(boolean isActive) {
        String statusColor = isActive ? "§a" : "§8";
        return new String[]{
            "§d✦ " + displayName + (isActive ? " §a✔" : " §8✖"),
            "§7" + effectDescription
        };
    }

    /**
     * Formate le nom d'un talent depuis son ID
     */
    private String formatTalentName(String talentId) {
        // Convertit "chasseur_beast_bat" en "Chauve-souris"
        // Cette méthode sera enrichie avec le TalentManager
        if (talentId == null) return "Inconnu";

        String[] parts = talentId.split("_");
        if (parts.length > 1) {
            // Prendre les parties après le préfixe de classe
            StringBuilder name = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (name.length() > 0) name.append(" ");
                String part = parts[i];
                name.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            }
            return name.toString();
        }
        return talentId;
    }

    /**
     * Applique le modificateur à une valeur
     *
     * @param baseValue Valeur de base
     * @return Valeur modifiée
     */
    public double applyModifier(double baseValue) {
        return switch (modifierType) {
            case DAMAGE_BONUS, CRIT_DAMAGE_BONUS, CRIT_CHANCE_BONUS,
                 DURATION_EXTENSION, RADIUS_BONUS, PROC_CHANCE_BONUS,
                 SPEED_BUFF, HEAL_ON_PROC, SHIELD_ON_PROC,
                 XP_BONUS, LOOT_BONUS, APPLY_VULNERABILITY ->
                baseValue * (1.0 + modifierValue / 100.0);

            case COOLDOWN_REDUCTION ->
                baseValue * (1.0 - modifierValue / 100.0);

            case EXTRA_SUMMON, EXTRA_PROJECTILE, EXTRA_BOUNCE,
                 EXTRA_STACKS, REDUCED_THRESHOLD ->
                baseValue + modifierValue;

            case APPLY_SLOW -> modifierValue; // Durée du slow

            case UNIQUE_EFFECT -> modifierValue; // Dépend du talent
        };
    }

    /**
     * Obtient la valeur entière du modificateur (pour les counts)
     */
    public int getModifierValueAsInt() {
        return (int) Math.round(modifierValue);
    }

    @Override
    public String toString() {
        return String.format("Awaken{id=%s, class=%s, talent=%s, type=%s, value=%.1f}",
            id, requiredClass, targetTalentId, modifierType, modifierValue);
    }
}
