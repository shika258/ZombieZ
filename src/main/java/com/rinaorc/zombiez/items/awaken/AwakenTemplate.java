package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Template pour générer des éveils
 *
 * Les templates définissent quels types de modificateurs sont possibles
 * pour chaque type d'effet de talent, avec des valeurs spécifiques.
 */
@Getter
@Builder
public class AwakenTemplate {

    /**
     * Type d'effet du talent ciblé
     */
    private final Talent.TalentEffectType effectType;

    /**
     * Types de modificateurs possibles pour ce template
     * avec leurs poids de sélection
     */
    private final Map<AwakenModifierType, Double> modifierWeights;

    /**
     * Overrides de valeurs par type de modificateur
     * (si null, utilise les valeurs par défaut du type)
     */
    private final Map<AwakenModifierType, double[]> valueOverrides;

    /**
     * Overrides de descriptions par type de modificateur
     */
    private final Map<AwakenModifierType, String> descriptionOverrides;

    /**
     * Nom personnalisé pour l'éveil (optionnel)
     */
    private final String customName;

    // ==================== GÉNÉRATEURS ====================

    /**
     * Génère un Awaken à partir de ce template
     *
     * @param talent Le talent cible
     * @param qualityBonus Bonus de qualité (0.0 à 0.3)
     * @return Un nouvel Awaken
     */
    public Awaken generate(Talent talent, double qualityBonus) {
        // Sélectionner un type de modificateur
        AwakenModifierType modType = selectModifierType();

        // Générer la valeur
        double value = generateValue(modType, qualityBonus);

        // Générer la description
        String description = generateDescription(modType, value);

        // Générer le nom
        String name = generateName(modType, talent);

        return Awaken.builder()
            .id("awaken_" + talent.getClassType().getId() + "_" + talent.getId())
            .displayName(name)
            .requiredClass(talent.getClassType())
            .requiredBranch(TalentBranch.fromId(talent.getClassType().name() + "_" +
                getBranchFromTalent(talent)))
            .targetTalentId(talent.getId())
            .targetEffectType(talent.getEffectType())
            .modifierType(modType)
            .modifierValue(value)
            .effectDescription(description)
            .isUnique(false)
            .build();
    }

    /**
     * Sélectionne un type de modificateur basé sur les poids
     */
    private AwakenModifierType selectModifierType() {
        double totalWeight = modifierWeights.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        double roll = Math.random() * totalWeight;
        double cumulative = 0;

        for (var entry : modifierWeights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        // Fallback au premier
        return modifierWeights.keySet().iterator().next();
    }

    /**
     * Génère une valeur pour le modificateur
     */
    private double generateValue(AwakenModifierType modType, double qualityBonus) {
        double[] override = valueOverrides != null ? valueOverrides.get(modType) : null;

        if (override != null && override.length >= 2) {
            double min = override[0];
            double max = override[1];
            double base = min + Math.random() * (max - min);
            return base * (1.0 + qualityBonus * 0.2);
        }

        return modType.rollValue(qualityBonus);
    }

    /**
     * Génère la description
     */
    private String generateDescription(AwakenModifierType modType, double value) {
        if (descriptionOverrides != null && descriptionOverrides.containsKey(modType)) {
            String template = descriptionOverrides.get(modType);
            if (modType.isIntegerValue()) {
                return String.format(template, (int) Math.round(value));
            }
            return String.format(template, value);
        }

        return modType.formatDescription(value);
    }

    /**
     * Génère le nom de l'éveil
     */
    private String generateName(AwakenModifierType modType, Talent talent) {
        if (customName != null) {
            return customName;
        }

        // Générer un nom basé sur le talent et le type de modificateur
        String talentName = talent.getName();
        String modName = modType.getDisplayName();

        return talentName + " " + switch (modType) {
            case EXTRA_SUMMON -> "Renforcé";
            case DAMAGE_BONUS -> "Dévastateur";
            case CRIT_DAMAGE_BONUS -> "Mortel";
            case CRIT_CHANCE_BONUS -> "Précis";
            case COOLDOWN_REDUCTION -> "Rapide";
            case DURATION_EXTENSION -> "Persistant";
            case RADIUS_BONUS -> "Étendu";
            case EXTRA_PROJECTILE -> "Multiple";
            case EXTRA_BOUNCE -> "Ricochant";
            case PROC_CHANCE_BONUS -> "Fréquent";
            case EXTRA_STACKS -> "Cumulatif";
            case REDUCED_THRESHOLD -> "Optimal";
            case APPLY_SLOW -> "Entravant";
            case APPLY_VULNERABILITY -> "Perçant";
            case SPEED_BUFF -> "Véloce";
            case HEAL_ON_PROC -> "Vital";
            case SHIELD_ON_PROC -> "Protecteur";
            case XP_BONUS -> "Sage";
            case LOOT_BONUS -> "Fortuné";
            case UNIQUE_EFFECT -> "Unique";
        };
    }

    /**
     * Extrait le nom de la branche depuis un talent
     */
    private String getBranchFromTalent(Talent talent) {
        // Le slotIndex correspond à la branche
        int slot = talent.getSlotIndex();
        ClassType classType = talent.getClassType();

        TalentBranch[] branches = TalentBranch.getBranchesForClass(classType);
        if (slot >= 0 && slot < branches.length) {
            return branches[slot].name().replace(classType.name() + "_", "");
        }
        return "UNKNOWN";
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Crée un template pour les talents d'invocation (bêtes, clones)
     */
    public static AwakenTemplate forSummonTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_SUMMON, 1.5);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de dégâts directs
     */
    public static AwakenTemplate forDamageTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.2);
        weights.put(AwakenModifierType.CRIT_DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.CRIT_CHANCE_BONUS, 0.8);
        weights.put(AwakenModifierType.APPLY_VULNERABILITY, 0.6);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de projectiles
     */
    public static AwakenTemplate forProjectileTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_PROJECTILE, 1.2);
        weights.put(AwakenModifierType.EXTRA_BOUNCE, 1.0);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.8);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.6);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de zone (AoE)
     */
    public static AwakenTemplate forAoETalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.RADIUS_BONUS, 1.2);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de DoT/Poison
     */
    public static AwakenTemplate forDoTTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.2);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.EXTRA_STACKS, 0.9);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de buff/stack
     */
    public static AwakenTemplate forStackTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.3);
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.6);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents de contrôle (CC)
     */
    public static AwakenTemplate forControlTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.2);
        weights.put(AwakenModifierType.RADIUS_BONUS, 1.0);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.8);
        weights.put(AwakenModifierType.APPLY_SLOW, 0.6);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents défensifs
     */
    public static AwakenTemplate forDefensiveTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.SHIELD_ON_PROC, 1.2);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Crée un template pour les talents ultimes (Tier 9)
     */
    public static AwakenTemplate forUltimateTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 1.3);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.1);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.8);

        // Bonus accrus pour les ultimes
        Map<AwakenModifierType, double[]> overrides = new HashMap<>();
        overrides.put(AwakenModifierType.COOLDOWN_REDUCTION, new double[]{20.0, 30.0});
        overrides.put(AwakenModifierType.DAMAGE_BONUS, new double[]{20.0, 40.0});

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .valueOverrides(overrides)
            .build();
    }

    /**
     * Crée un template générique pour tout talent
     */
    public static AwakenTemplate forGenericTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.9);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }
}
