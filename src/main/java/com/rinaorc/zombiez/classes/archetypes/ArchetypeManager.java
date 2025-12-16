package com.rinaorc.zombiez.classes.archetypes;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.data.ClassData;
import com.rinaorc.zombiez.classes.talents.ClassTalent;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Gestionnaire des archétypes de build
 *
 * PHILOSOPHIE:
 * - L'archétype est CALCULÉ dynamiquement, pas choisi
 * - Basé sur: talents débloqués, skills équipés, buffs arcade
 * - Change au fil du temps selon les choix du joueur
 * - Pas de UI spécial, c'est une "orientation détectée"
 *
 * SCORING:
 * - Chaque talent/skill "clé" pour un archétype donne des points
 * - L'archétype avec le plus de points devient actif
 * - Minimum 3 points pour qu'un archétype soit considéré
 */
@RequiredArgsConstructor
public class ArchetypeManager {

    // Points minimum pour qu'un archétype soit considéré actif
    private static final int MIN_POINTS_FOR_ARCHETYPE = 3;

    // Points attribués par type d'élément
    private static final int POINTS_PER_KEY_TALENT = 2;
    private static final int POINTS_PER_KEY_SKILL = 3;  // Skills équipés comptent plus
    private static final int POINTS_PER_KEY_BUFF = 1;   // Buffs arcade (bonus)

    /**
     * Calcule l'archétype actif d'un joueur basé sur ses choix
     *
     * @param data Les données de classe du joueur
     * @param equippedSkills Les IDs des skills équipés
     * @return L'archétype détecté, ou NONE si aucun dominant
     */
    public BuildArchetype calculateArchetype(ClassData data, Set<String> equippedSkills) {
        if (!data.hasClass()) {
            return BuildArchetype.NONE;
        }

        ClassType playerClass = data.getClassType();
        BuildArchetype[] possibleArchetypes = BuildArchetype.getArchetypesForClass(playerClass);

        // Calculer les scores pour chaque archétype possible
        Map<BuildArchetype, Integer> scores = new EnumMap<>(BuildArchetype.class);

        for (BuildArchetype archetype : possibleArchetypes) {
            int score = calculateArchetypeScore(archetype, data, equippedSkills);
            scores.put(archetype, score);
        }

        // Trouver l'archétype dominant
        BuildArchetype dominant = BuildArchetype.NONE;
        int maxScore = MIN_POINTS_FOR_ARCHETYPE - 1;  // Minimum pour être considéré

        for (Map.Entry<BuildArchetype, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                dominant = entry.getKey();
            }
        }

        return dominant;
    }

    /**
     * Calcule le score d'un archétype pour un joueur
     */
    private int calculateArchetypeScore(BuildArchetype archetype,
                                        ClassData data,
                                        Set<String> equippedSkills) {
        int score = 0;

        // Points pour les talents clés débloqués
        Set<String> unlockedTalents = data.getUnlockedTalents();
        for (String keyTalent : archetype.getKeyTalents()) {
            if (unlockedTalents.contains(keyTalent)) {
                score += POINTS_PER_KEY_TALENT;
            }
        }

        // Points pour les skills clés équipés
        for (String keySkill : archetype.getKeySkills()) {
            if (equippedSkills.contains(keySkill)) {
                score += POINTS_PER_KEY_SKILL;
            }
        }

        // Points bonus pour certains buffs arcade (optionnel)
        // On peut ajouter une logique plus complexe ici si nécessaire
        score += calculateBuffBonus(archetype, data.getArcadeBuffs());

        return score;
    }

    /**
     * Calcule les points bonus des buffs arcade pour un archétype
     */
    private int calculateBuffBonus(BuildArchetype archetype, Map<String, Integer> arcadeBuffs) {
        int bonus = 0;

        // Mapping archétype -> buffs qui le renforcent
        switch (archetype) {
            // GUERRIER
            case GUERRIER_TORNADE -> {
                // AoE et vitesse favorisent Tornade
                if (arcadeBuffs.containsKey("arcade_area")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_speed")) bonus += POINTS_PER_KEY_BUFF;
            }
            case GUERRIER_MUR -> {
                // Tank et survie favorisent Mur
                if (arcadeBuffs.containsKey("arcade_defense")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_regen")) bonus += POINTS_PER_KEY_BUFF;
            }
            case GUERRIER_BOUCHER -> {
                // Dégâts bruts favorisent Boucher
                if (arcadeBuffs.containsKey("arcade_damage")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_crit")) bonus += POINTS_PER_KEY_BUFF;
            }

            // CHASSEUR
            case CHASSEUR_GATLING -> {
                // Vitesse d'attaque favorise Gatling
                if (arcadeBuffs.containsKey("arcade_attackspeed")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_speed")) bonus += POINTS_PER_KEY_BUFF;
            }
            case CHASSEUR_FANTOME -> {
                // Critique et précision favorisent Fantôme
                if (arcadeBuffs.containsKey("arcade_crit")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_headshot")) bonus += POINTS_PER_KEY_BUFF;
            }
            case CHASSEUR_PIEGEUR -> {
                // AoE et cooldown favorisent Piégeur
                if (arcadeBuffs.containsKey("arcade_area")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_cooldown")) bonus += POINTS_PER_KEY_BUFF;
            }

            // OCCULTISTE
            case OCCULTISTE_DEFLAGRATION -> {
                // AoE massif favorise Déflagration
                if (arcadeBuffs.containsKey("arcade_area")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_spelldmg")) bonus += POINTS_PER_KEY_BUFF;
            }
            case OCCULTISTE_SANG -> {
                // Drain de vie favorise Mage de Sang
                if (arcadeBuffs.containsKey("arcade_lifesteal")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_regen")) bonus += POINTS_PER_KEY_BUFF;
            }
            case OCCULTISTE_ARCHIMAGE -> {
                // Énergie et CDR favorisent Archimage
                if (arcadeBuffs.containsKey("arcade_energy")) bonus += POINTS_PER_KEY_BUFF;
                if (arcadeBuffs.containsKey("arcade_cooldown")) bonus += POINTS_PER_KEY_BUFF;
            }
            default -> {}
        }

        return bonus;
    }

    /**
     * Obtient le modificateur de dégâts AoE pour un archétype
     */
    public double getAoeDamageModifier(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return 1.0;
        }
        return archetype.getAoeModifier();
    }

    /**
     * Obtient le modificateur de dégâts single-target pour un archétype
     */
    public double getSingleTargetModifier(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return 1.0;
        }
        return archetype.getSingleTargetModifier();
    }

    /**
     * Obtient le modificateur de tank pour un archétype
     */
    public double getTankModifier(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return 1.0;
        }
        return archetype.getTankModifier();
    }

    /**
     * Génère un résumé textuel de l'archétype pour affichage
     */
    public String getArchetypeSummary(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return "§7Style de combat: §8Non défini";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§7Style de combat: ").append(archetype.getColoredName()).append("\n");
        sb.append("§8").append(archetype.getDescription()).append("\n");
        sb.append("\n");

        // Afficher les modificateurs significatifs
        if (archetype.getAoeModifier() > 1.0) {
            sb.append("§a▲ ").append(String.format("+%.0f%%", (archetype.getAoeModifier() - 1) * 100))
              .append(" dégâts de zone\n");
        } else if (archetype.getAoeModifier() < 1.0) {
            sb.append("§c▼ ").append(String.format("%.0f%%", (archetype.getAoeModifier() - 1) * 100))
              .append(" dégâts de zone\n");
        }

        if (archetype.getSingleTargetModifier() > 1.0) {
            sb.append("§a▲ ").append(String.format("+%.0f%%", (archetype.getSingleTargetModifier() - 1) * 100))
              .append(" dégâts mono-cible\n");
        } else if (archetype.getSingleTargetModifier() < 1.0) {
            sb.append("§c▼ ").append(String.format("%.0f%%", (archetype.getSingleTargetModifier() - 1) * 100))
              .append(" dégâts mono-cible\n");
        }

        if (archetype.getTankModifier() > 1.0) {
            sb.append("§a▲ ").append(String.format("+%.0f%%", (archetype.getTankModifier() - 1) * 100))
              .append(" efficacité défensive\n");
        } else if (archetype.getTankModifier() < 1.0) {
            sb.append("§c▼ ").append(String.format("%.0f%%", (archetype.getTankModifier() - 1) * 100))
              .append(" efficacité défensive\n");
        }

        return sb.toString();
    }

    /**
     * Obtient les scores détaillés pour debug/affichage
     */
    public Map<BuildArchetype, Integer> getDetailedScores(ClassData data, Set<String> equippedSkills) {
        if (!data.hasClass()) {
            return Collections.emptyMap();
        }

        ClassType playerClass = data.getClassType();
        BuildArchetype[] possibleArchetypes = BuildArchetype.getArchetypesForClass(playerClass);

        Map<BuildArchetype, Integer> scores = new EnumMap<>(BuildArchetype.class);
        for (BuildArchetype archetype : possibleArchetypes) {
            int score = calculateArchetypeScore(archetype, data, equippedSkills);
            scores.put(archetype, score);
        }

        return scores;
    }
}
