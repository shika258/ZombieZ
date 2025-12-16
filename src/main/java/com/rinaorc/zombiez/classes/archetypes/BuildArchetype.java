package com.rinaorc.zombiez.classes.archetypes;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;

/**
 * Archétypes de build - Définit le style de jeu du joueur
 *
 * Chaque classe a 3 archétypes distincts qui modifient:
 * - Le comportement des compétences
 * - L'efficacité de certains talents
 * - Les synergies avec les buffs arcade
 *
 * L'archétype est calculé dynamiquement basé sur les choix du joueur
 * (talents débloqués, skills équipés, buffs collectés)
 */
@Getter
public enum BuildArchetype {

    // ==================== GUERRIER ====================
    // ÉQUILIBRÉ: Chaque archétype a ~même potentiel total, mais différemment réparti

    /**
     * TORNADE - "J'avance non-stop, je rase la horde"
     * LOOP: Charge → AoE sur le pack → avance → répète
     * SIGNATURE: Traînée de dégâts derrière la charge, dégâts passifs en mouvement
     */
    GUERRIER_TORNADE(ClassType.GUERRIER, "Tornade", "§c⚔",
        "Mouvement constant, destruction de masse.",
        new String[]{"gue_cleave", "gue_dmg_1", "gue_rage"},
        new String[]{"gue_charge", "gue_slam"},
        0.85, 1.35, 0.80),  // tankMod, aoeMod, singleTargetMod (total ~3.0)

    /**
     * MUR VIVANT - "Je tiens la ligne, personne ne passe"
     * LOOP: Taunt → Encaisse → Contre-attaque → Régén
     * SIGNATURE: Réduction massive, taunt de groupe, riposte automatique
     */
    GUERRIER_MUR(ClassType.GUERRIER, "Mur Vivant", "§6⛨",
        "Forteresse mobile. Impossible à tuer.",
        new String[]{"gue_hp_1", "gue_armor", "gue_laststand"},
        new String[]{"gue_warcry", "gue_slam"},
        1.50, 0.85, 0.65),  // (total ~3.0)

    /**
     * BOUCHER - "Je marque, j'exécute, je recommence"
     * LOOP: Marque cible → Charge → Execute → Kill → Reset
     * SIGNATURE: Dégâts massifs single-target, reset sur kill
     */
    GUERRIER_BOUCHER(ClassType.GUERRIER, "Boucher", "§4☠",
        "Chasseur d'élites. Un coup, une mort.",
        new String[]{"gue_execute", "gue_rage", "gue_warlord"},
        new String[]{"gue_charge", "gue_rage"},
        0.90, 0.70, 1.45),  // (total ~3.05)

    // ==================== CHASSEUR ====================
    // ÉQUILIBRÉ: Gatling = DPS stable, Fantôme = burst spike, Piégeur = contrôle

    /**
     * GATLING - "Je tire sans arrêt, je recule sans cesse"
     * LOOP: Tir rapide → Repositionnement → Tir rapide → Esquive → Répète
     * SIGNATURE: DPS constant, mobilité fluide, punishment si stoppé
     */
    CHASSEUR_GATLING(ClassType.CHASSEUR, "Gatling", "§a⚡",
        "Pluie de projectiles. Jamais à l'arrêt.",
        new String[]{"cha_reload", "cha_crit_1", "cha_speed"},
        new String[]{"cha_multishot", "cha_roll"},
        0.75, 1.15, 1.15),  // Équilibré AoE/single (total ~3.05)

    /**
     * FANTÔME - "Une balle, une mort, je disparais"
     * LOOP: Invisibilité → Positionnement → Headshot → Disparition
     * SIGNATURE: Burst massive conditionnel (stealth), faible en combat prolongé
     */
    CHASSEUR_FANTOME(ClassType.CHASSEUR, "Fantôme", "§b👻",
        "Invisible et mortel. Tu meurs avant de le voir.",
        new String[]{"cha_stealth", "cha_headshot", "cha_deadeye"},
        new String[]{"cha_deadeye", "cha_roll"},
        0.85, 0.65, 1.55),  // Très fort single, faible horde (total ~3.05)

    /**
     * PIÉGEUR - "Le terrain est mon arme"
     * LOOP: Pose piège → Kite vers piège → Déclenche → Repose
     * SIGNATURE: Contrôle de zone, clear défensif, setup required
     */
    CHASSEUR_PIEGEUR(ClassType.CHASSEUR, "Piégeur", "§e💣",
        "La map devient un champ de mines mortel.",
        new String[]{"cha_pierce", "cha_speed", "cha_reload"},
        new String[]{"cha_trap", "cha_roll"},
        1.00, 1.30, 0.75),  // Survie + AoE (total ~3.05)

    // ==================== OCCULTISTE ====================
    // ÉQUILIBRÉ: Déflagration = horde, Sang = sustain risqué, Archimage = polyvalent

    /**
     * DÉFLAGRATION - "J'empile, j'explose, je recommence"
     * LOOP: Orb (stack) → Orb (stack) → Nova (detonate) → BOOM
     * SIGNATURE: Clear de horde absolu, explosion en chaîne
     */
    OCCULTISTE_DEFLAGRATION(ClassType.OCCULTISTE, "Déflagration", "§5🔥",
        "Stack. Explose. Recommence.",
        new String[]{"occ_dot", "occ_aoe", "occ_power_1"},
        new String[]{"occ_orb", "occ_nova", "occ_apocalypse"},
        0.65, 1.55, 0.80),  // (total ~3.0)

    /**
     * MAGE DE SANG - "Ma vie pour mon pouvoir"
     * LOOP: Drain (heal) → Nova (damage/heal) → Low HP = boost → Drain
     * SIGNATURE: Risk/reward, HP comme ressource, quasi-immortel si bien joué
     */
    OCCULTISTE_SANG(ClassType.OCCULTISTE, "Mage de Sang", "§4❤",
        "Ta vie est ton arme. Joue au bord du gouffre.",
        new String[]{"occ_leech", "occ_shield", "occ_immortal"},
        new String[]{"occ_drain", "occ_nova"},
        1.25, 0.95, 0.90),  // Tank mage (total ~3.1)

    /**
     * ARCHIMAGE - "Efficacité maximale, combos parfaits"
     * LOOP: Orb → Drain (reset orb) → Orb boost → Nova (finisher)
     * SIGNATURE: Combos, gestion d'énergie, scaling late-game
     */
    OCCULTISTE_ARCHIMAGE(ClassType.OCCULTISTE, "Archimage", "§9✧",
        "Maîtrise absolue. Chaque sort compte.",
        new String[]{"occ_energy_1", "occ_regen_energy", "occ_cdr"},
        new String[]{"occ_orb", "occ_drain", "occ_nova"},
        0.85, 1.10, 1.10),  // Polyvalent (total ~3.05)

    // Archétype par défaut quand pas de dominante claire
    NONE(null, "Aucun", "§7?", "Style de combat non défini.",
        new String[]{}, new String[]{}, 1.0, 1.0, 1.0);

    private final ClassType classType;
    private final String displayName;
    private final String icon;
    private final String description;
    private final String[] keyTalents;      // Talents qui scorent pour cet archétype
    private final String[] keySkills;       // Skills qui scorent pour cet archétype

    // Modificateurs de gameplay
    private final double tankModifier;          // Efficacité défensive
    private final double aoeModifier;           // Efficacité AoE
    private final double singleTargetModifier;  // Efficacité single-target

    BuildArchetype(ClassType classType, String displayName, String icon, String description,
                   String[] keyTalents, String[] keySkills,
                   double tankMod, double aoeMod, double singleTargetMod) {
        this.classType = classType;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.keyTalents = keyTalents;
        this.keySkills = keySkills;
        this.tankModifier = tankMod;
        this.aoeModifier = aoeMod;
        this.singleTargetModifier = singleTargetMod;
    }

    public String getColoredName() {
        return icon + " " + displayName;
    }

    /**
     * Obtient les archétypes disponibles pour une classe
     */
    public static BuildArchetype[] getArchetypesForClass(ClassType classType) {
        return switch (classType) {
            case GUERRIER -> new BuildArchetype[]{GUERRIER_TORNADE, GUERRIER_MUR, GUERRIER_BOUCHER};
            case CHASSEUR -> new BuildArchetype[]{CHASSEUR_GATLING, CHASSEUR_FANTOME, CHASSEUR_PIEGEUR};
            case OCCULTISTE -> new BuildArchetype[]{OCCULTISTE_DEFLAGRATION, OCCULTISTE_SANG, OCCULTISTE_ARCHIMAGE};
        };
    }
}
