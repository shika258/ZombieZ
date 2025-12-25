package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import lombok.Builder;
import lombok.Getter;

import java.util.*;
import java.util.UUID;

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
            case THRESHOLD_BONUS -> "Élargi";
            case APPLY_SLOW -> "Entravant";
            case APPLY_VULNERABILITY -> "Perçant";
            case SPEED_BUFF -> "Véloce";
            case HEAL_ON_PROC -> "Vital";
            case SHIELD_ON_PROC -> "Protecteur";
            case XP_BONUS -> "Sage";
            case LOOT_BONUS -> "Fortuné";
            case UNIQUE_EFFECT -> "Unique";
            // Types défensifs pour armures
            case DAMAGE_REDUCTION -> "Blindé";
            case ARMOR_BONUS -> "Cuirassé";
            case THORNS_DAMAGE -> "Épineux";
            case HEALTH_BONUS -> "Robuste";
            case BLOCK_CHANCE -> "Gardien";
            case HEALTH_REGEN -> "Régénérant";
            case CC_RESISTANCE -> "Inébranlable";
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

    // ==================== TEMPLATES SPÉCIALISÉS PAR VOIE ====================

    // ==================== GUERRIER ====================

    /**
     * Template pour la Voie du Séisme (Guerrier) - AoE & Contrôle
     * Priorité: REDUCED_THRESHOLD (compteurs), RADIUS_BONUS, DAMAGE_BONUS
     */
    public static AwakenTemplate forSeismeTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.4); // Critique pour compteurs
        weights.put(AwakenModifierType.RADIUS_BONUS, 1.2);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.8);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.6);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Rempart (Guerrier) - Défense & Riposte
     * Priorité: SHIELD_ON_PROC, EXTRA_STACKS, REDUCED_THRESHOLD, PROC_CHANCE
     */
    public static AwakenTemplate forRempartTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.SHIELD_ON_PROC, 1.3);
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.2);
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.0);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.9);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie de la Rage (Guerrier) - Stacking & Berserker
     * Priorité: EXTRA_STACKS, REDUCED_THRESHOLD, DAMAGE_BONUS, DURATION
     */
    public static AwakenTemplate forRageTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.4);
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.3); // Critique pour les seuils de stacks
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.THRESHOLD_BONUS, 0.7); // Pour Coup de Grâce (<30% HP)

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Sang (Guerrier) - Vampirisme & Os
     * Priorité: HEAL_ON_PROC, EXTRA_STACKS (charges), THRESHOLD_BONUS (HP), DURATION
     */
    public static AwakenTemplate forSangTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.HEAL_ON_PROC, 1.4);
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.2); // Charges d'os
        weights.put(AwakenModifierType.THRESHOLD_BONUS, 1.0); // Seuils HP (Consommation)
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.9);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Fauve (Guerrier) - Dash & Saignement
     * Priorité: DAMAGE_BONUS, EXTRA_STACKS (bleed), REDUCED_THRESHOLD, SPEED_BUFF
     */
    public static AwakenTemplate forFauveTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.3);
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.2); // Stacks bleed
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.0); // Éviscération (5→4 Fentes)
        weights.put(AwakenModifierType.SPEED_BUFF, 0.9);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    // ==================== CHASSEUR ====================

    /**
     * Template pour la Voie du Barrage (Chasseur) - Multi-projectiles & Pluies
     * Priorité: EXTRA_PROJECTILE, REDUCED_THRESHOLD (charges), PROC_CHANCE, RADIUS
     */
    public static AwakenTemplate forBarrageTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_PROJECTILE, 1.4);
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.2); // Rafale (8→6), Furie (5→4)
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 1.0);
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.9);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie des Bêtes (Chasseur) - Invocations & Meute
     * Priorité: EXTRA_SUMMON, DAMAGE_BONUS, COOLDOWN_REDUCTION, DURATION
     */
    public static AwakenTemplate forBetesTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_SUMMON, 1.5); // Priorité maximale!
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.1);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.9);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie de l'Ombre (Chasseur) - Points d'Ombre & Exécution
     * Priorité: EXTRA_STACKS (points), REDUCED_THRESHOLD (5→4 points), DAMAGE_BONUS
     */
    public static AwakenTemplate forOmbreTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.3); // +Points d'Ombre
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.3); // Exécution 5→4 points
        weights.put(AwakenModifierType.DAMAGE_BONUS, 1.0);
        weights.put(AwakenModifierType.PROC_CHANCE_BONUS, 0.8);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Poison (Chasseur) - Virulence & DoT
     * Priorité: EXTRA_STACKS (virulence), THRESHOLD_BONUS (seuils 70%/100%), DURATION, DAMAGE
     */
    public static AwakenTemplate forPoisonTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.3); // +Virulence appliquée
        weights.put(AwakenModifierType.THRESHOLD_BONUS, 1.2); // Nécrose 100%→90%
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.0);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.9);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.7); // Peste Noire lifesteal

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Givre (Chasseur) - Rebonds & Gel
     * Priorité: EXTRA_BOUNCE, REDUCED_THRESHOLD (compteurs critiques), EXTRA_STACKS
     */
    public static AwakenTemplate forGivreTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_BOUNCE, 1.4); // Critique pour toute la voie
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.3); // 4 compteurs critiques!
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.0); // +Givre
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.8);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    // ==================== OCCULTISTE ====================

    /**
     * Template pour la Voie du Feu (Occultiste) - Surchauffe & Météores
     * Priorité: EXTRA_PROJECTILE (météores), THRESHOLD_BONUS (Surchauffe), DURATION, RADIUS
     */
    public static AwakenTemplate forFeuTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_PROJECTILE, 1.3); // +Météores
        weights.put(AwakenModifierType.THRESHOLD_BONUS, 1.2); // Phoenix: Surchauffe 8s→6s
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.0); // Durée du feu
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.9);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.8);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie de la Glace (Occultiste) - Stacks de Givre & Brisure
     * Priorité: EXTRA_STACKS (givre), REDUCED_THRESHOLD (5→4 stacks), RADIUS, DURATION
     */
    public static AwakenTemplate forGlaceTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.4); // +Stacks de givre
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.3); // Zéro Absolu/Ère Glaciaire 5→4
        weights.put(AwakenModifierType.RADIUS_BONUS, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.COOLDOWN_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie de la Foudre (Occultiste) - Chaînes & Multi-strikes
     * Priorité: EXTRA_BOUNCE (chaînes), EXTRA_STACKS (strikes Mjolnir), CRIT_CHANCE, RADIUS
     */
    public static AwakenTemplate forFoudreTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_BOUNCE, 1.4); // +Cibles chaîne
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.2); // Mjolnir +1 strike
        weights.put(AwakenModifierType.CRIT_CHANCE_BONUS, 1.0); // Surcharge synergie
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.9);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.7); // Conducteur

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie de l'Âme (Occultiste) - Orbes & Invocations
     * Priorité: EXTRA_STACKS (orbes), EXTRA_SUMMON (squelettes), HEAL_ON_PROC, DURATION
     */
    public static AwakenTemplate forAmeTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.EXTRA_STACKS, 1.4); // +Orbes d'âme
        weights.put(AwakenModifierType.EXTRA_SUMMON, 1.3); // +Squelettes/Serviteurs
        weights.put(AwakenModifierType.HEAL_ON_PROC, 1.0);
        weights.put(AwakenModifierType.DURATION_EXTENSION, 0.8);
        weights.put(AwakenModifierType.DAMAGE_BONUS, 0.7);

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    /**
     * Template pour la Voie du Vide (Occultiste) - DOTs & Gravité
     * Priorité: DURATION (DOTs), REDUCED_THRESHOLD (kills), THRESHOLD_BONUS (HP), RADIUS
     */
    public static AwakenTemplate forVideTalent(Talent.TalentEffectType effectType) {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DURATION_EXTENSION, 1.3); // DOTs plus longs
        weights.put(AwakenModifierType.REDUCED_THRESHOLD, 1.2); // Singularité 3→2 kills
        weights.put(AwakenModifierType.THRESHOLD_BONUS, 1.1); // Déchirure <15%→<20%
        weights.put(AwakenModifierType.RADIUS_BONUS, 0.9);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.7); // Toucher Vampirique

        return AwakenTemplate.builder()
            .effectType(effectType)
            .modifierWeights(weights)
            .build();
    }

    // ==================== TEMPLATES ARMURES ====================

    /**
     * Template pour les casques - Focus sur la réduction de dégâts et CC resistance
     */
    public static AwakenTemplate forHelmetArmor() {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DAMAGE_REDUCTION, 1.4);
        weights.put(AwakenModifierType.CC_RESISTANCE, 1.2);
        weights.put(AwakenModifierType.HEALTH_BONUS, 1.0);
        weights.put(AwakenModifierType.BLOCK_CHANCE, 0.8);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.6);

        return AwakenTemplate.builder()
            .effectType(null) // Pas de talent requis pour les armures
            .modifierWeights(weights)
            .customName("Éveil de Protection")
            .build();
    }

    /**
     * Template pour les plastrons - Focus sur l'armure et la vie
     */
    public static AwakenTemplate forChestplateArmor() {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.ARMOR_BONUS, 1.4);
        weights.put(AwakenModifierType.HEALTH_BONUS, 1.3);
        weights.put(AwakenModifierType.DAMAGE_REDUCTION, 1.0);
        weights.put(AwakenModifierType.SHIELD_ON_PROC, 0.8);
        weights.put(AwakenModifierType.THORNS_DAMAGE, 0.6);

        return AwakenTemplate.builder()
            .effectType(null)
            .modifierWeights(weights)
            .customName("Éveil de Fortification")
            .build();
    }

    /**
     * Template pour les jambières - Focus sur la survie et régénération
     */
    public static AwakenTemplate forLeggingsArmor() {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.HEALTH_REGEN, 1.3);
        weights.put(AwakenModifierType.HEALTH_BONUS, 1.2);
        weights.put(AwakenModifierType.ARMOR_BONUS, 1.0);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.9);
        weights.put(AwakenModifierType.DAMAGE_REDUCTION, 0.7);

        return AwakenTemplate.builder()
            .effectType(null)
            .modifierWeights(weights)
            .customName("Éveil de Vitalité")
            .build();
    }

    /**
     * Template pour les bottes - Focus sur la mobilité et esquive
     */
    public static AwakenTemplate forBootsArmor() {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.SPEED_BUFF, 1.4);
        weights.put(AwakenModifierType.BLOCK_CHANCE, 1.2);
        weights.put(AwakenModifierType.CC_RESISTANCE, 1.0);
        weights.put(AwakenModifierType.DAMAGE_REDUCTION, 0.8);
        weights.put(AwakenModifierType.HEALTH_REGEN, 0.6);

        return AwakenTemplate.builder()
            .effectType(null)
            .modifierWeights(weights)
            .customName("Éveil d'Agilité")
            .build();
    }

    /**
     * Template générique pour armures (toutes pièces)
     */
    public static AwakenTemplate forGenericArmor() {
        Map<AwakenModifierType, Double> weights = new LinkedHashMap<>();
        weights.put(AwakenModifierType.DAMAGE_REDUCTION, 1.2);
        weights.put(AwakenModifierType.ARMOR_BONUS, 1.1);
        weights.put(AwakenModifierType.HEALTH_BONUS, 1.0);
        weights.put(AwakenModifierType.SHIELD_ON_PROC, 0.9);
        weights.put(AwakenModifierType.HEAL_ON_PROC, 0.8);
        weights.put(AwakenModifierType.BLOCK_CHANCE, 0.7);
        weights.put(AwakenModifierType.THORNS_DAMAGE, 0.6);

        return AwakenTemplate.builder()
            .effectType(null)
            .modifierWeights(weights)
            .customName("Éveil Défensif")
            .build();
    }

    /**
     * Génère un Awaken pour une armure (sans lien avec un talent)
     *
     * @param armorType Type d'armure (HELMET, CHESTPLATE, etc.)
     * @param qualityBonus Bonus de qualité (0.0 à 0.3)
     * @return Un nouvel Awaken pour armure
     */
    public Awaken generateForArmor(com.rinaorc.zombiez.items.types.ItemType armorType, double qualityBonus) {
        // Sélectionner un type de modificateur
        AwakenModifierType modType = selectModifierType();

        // Générer la valeur
        double value = generateValue(modType, qualityBonus);

        // Générer la description
        String description = generateDescription(modType, value);

        // Générer le nom basé sur le type d'armure
        String name = generateArmorName(modType, armorType);

        return Awaken.builder()
            .id("awaken_armor_" + armorType.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8))
            .displayName(name)
            .requiredClass(null) // Pas de classe requise pour les armures
            .requiredBranch(null)
            .targetTalentId(null) // Pas de talent requis
            .targetEffectType(null)
            .modifierType(modType)
            .modifierValue(value)
            .effectDescription(description)
            .isUnique(false)
            .build();
    }

    /**
     * Génère le nom de l'éveil d'armure
     */
    private String generateArmorName(AwakenModifierType modType, com.rinaorc.zombiez.items.types.ItemType armorType) {
        String armorName = armorType.getDisplayName();
        return armorName + " " + switch (modType) {
            case DAMAGE_REDUCTION -> "du Rempart";
            case ARMOR_BONUS -> "de l'Acier";
            case THORNS_DAMAGE -> "des Épines";
            case HEALTH_BONUS -> "de Vitalité";
            case BLOCK_CHANCE -> "du Gardien";
            case HEALTH_REGEN -> "de Régénération";
            case CC_RESISTANCE -> "de l'Inébranlable";
            case SHIELD_ON_PROC -> "de l'Égide";
            case HEAL_ON_PROC -> "du Vampire";
            case SPEED_BUFF -> "du Zéphyr";
            default -> "Éveillé";
        };
    }
}
