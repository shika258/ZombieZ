package com.rinaorc.zombiez.classes.archetypes;

import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
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
 * SCORING ÉQUILIBRÉ:
 * - Talents clés: 2 points chacun (investissement à long terme)
 * - Skills équipés: 2 points chacun (choix actif)
 * - Buffs arcade: 1 point chacun (bonus mineur)
 * - Minimum 5 points requis pour activer un archétype
 *
 * Cela signifie qu'il faut au moins:
 * - 2 talents + 1 skill, OU
 * - 1 talent + 2 skills, OU
 * - 3 talents, etc.
 */
@RequiredArgsConstructor
public class ArchetypeManager {

    // Points minimum pour qu'un archétype soit considéré actif
    // Augmenté de 3 à 5 pour éviter l'activation trop facile
    private static final int MIN_POINTS_FOR_ARCHETYPE = 5;

    // Points attribués par type d'élément (équilibrés)
    private static final int POINTS_PER_KEY_TALENT = 2;
    private static final int POINTS_PER_KEY_SKILL = 2;   // Réduit de 3 à 2 (équilibré)
    private static final int POINTS_PER_KEY_BUFF = 1;    // Buffs arcade (bonus mineur)

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

        ClassType playerClass = data.getSelectedClass();
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
        Set<String> unlockedTalents = data.getUnlockedTalentIds();
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
     * Sans formules/pourcentages - axé sur le feeling
     */
    public String getArchetypeSummary(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return "§7Style: §8Adaptable\n§8Aucune spécialisation dominante.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(archetype.getColoredName()).append("\n");
        sb.append("§7").append(archetype.getDescription()).append("\n");
        sb.append("\n");

        // Afficher forces/faiblesses en texte (pas de %)
        sb.append(getStrengthsWeaknesses(archetype));

        return sb.toString();
    }

    /**
     * Génère les forces/faiblesses en texte lisible
     */
    private String getStrengthsWeaknesses(BuildArchetype archetype) {
        return switch (archetype) {
            // GUERRIER
            case GUERRIER_TORNADE -> """
                §a✦ Fort contre les hordes
                §a✦ Mobile et agressif
                §c✗ Fragile à l'arrêt
                §c✗ Faible contre les boss""";
            case GUERRIER_MUR -> """
                §a✦ Quasi-unkillable
                §a✦ Protège les alliés
                §c✗ Dégâts faibles
                §c✗ Lent à clear""";
            case GUERRIER_BOUCHER -> """
                §a✦ Détruit les boss
                §a✦ Burst dévastateur
                §c✗ Mauvais en horde
                §c✗ Nécessite du setup""";

            // CHASSEUR
            case CHASSEUR_GATLING -> """
                §a✦ DPS constant
                §a✦ Efficace contre tout
                §c✗ Fragile si coincé
                §c✗ Demande du skill""";
            case CHASSEUR_FANTOME -> """
                §a✦ One-shot les cibles
                §a✦ Difficile à toucher
                §c✗ Mauvais en groupe
                §c✗ Dépend de l'invisibilité""";
            case CHASSEUR_PIEGEUR -> """
                §a✦ Contrôle le terrain
                §a✦ Défense excellente
                §c✗ DPS limité
                §c✗ Temps de préparation""";

            // OCCULTISTE
            case OCCULTISTE_DEFLAGRATION -> """
                §a✦ Clear de horde absolu
                §a✦ Explosions en chaîne
                §c✗ Très fragile
                §c✗ Setup obligatoire""";
            case OCCULTISTE_SANG -> """
                §a✦ Sustain unique
                §a✦ Risk/reward intense
                §c✗ Punissable si mal joué
                §c✗ HP instable""";
            case OCCULTISTE_ARCHIMAGE -> """
                §a✦ Polyvalent
                §a✦ Fort en late-game
                §c✗ Complexe à maîtriser
                §c✗ Faible si spam""";

            default -> "§7Style standard";
        };
    }

    /**
     * Obtient les scores détaillés pour debug/affichage
     */
    public Map<BuildArchetype, Integer> getDetailedScores(ClassData data, Set<String> equippedSkills) {
        if (!data.hasClass()) {
            return Collections.emptyMap();
        }

        ClassType playerClass = data.getSelectedClass();
        BuildArchetype[] possibleArchetypes = BuildArchetype.getArchetypesForClass(playerClass);

        Map<BuildArchetype, Integer> scores = new EnumMap<>(BuildArchetype.class);
        for (BuildArchetype archetype : possibleArchetypes) {
            int score = calculateArchetypeScore(archetype, data, equippedSkills);
            scores.put(archetype, score);
        }

        return scores;
    }
}
