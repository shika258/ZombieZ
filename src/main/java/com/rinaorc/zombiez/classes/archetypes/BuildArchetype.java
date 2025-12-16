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

    /**
     * TORNADE - "J'avance non-stop, je rase la horde"
     * Loop: Charge dans les packs, reste en mouvement, AoE en continu
     * Forces: Clear de horde excellent, mobilité
     * Faiblesses: Vulnérable à l'arrêt, single-target faible
     */
    GUERRIER_TORNADE(ClassType.GUERRIER, "Tornade", "§c⚔",
        "Tu clean les hordes en mouvement constant.",
        new String[]{"gue_cleave", "gue_dmg_1", "gue_rage"},  // Talents clés
        new String[]{"gue_charge", "gue_slam"},               // Skills clés
        0.8, 1.3, 0.7),  // tankMod, aoeMod, singleTargetMod

    /**
     * MUR VIVANT - "Je tiens la ligne, je tombe jamais"
     * Loop: Tank stable, absorbe les hits, punit les attaquants
     * Forces: Survie extrême, contrôle de groupe
     * Faiblesses: Mobilité réduite, DPS faible
     */
    GUERRIER_MUR(ClassType.GUERRIER, "Mur Vivant", "§6⛨",
        "Tu encaisses pour l'équipe, tu contrôles les packs.",
        new String[]{"gue_hp_1", "gue_armor", "gue_laststand"},
        new String[]{"gue_warcry", "gue_slam"},
        1.5, 0.8, 0.6),

    /**
     * BOUCHER - "Je marque, je finis, je one-shot"
     * Loop: Focus une cible, prépare, exécute avec burst massif
     * Forces: Destruction d'élites/boss, burst
     * Faiblesses: Mauvais en horde, setup requis
     */
    GUERRIER_BOUCHER(ClassType.GUERRIER, "Boucher", "§4☠",
        "Tu détruis les élites avec des finishers.",
        new String[]{"gue_execute", "gue_rage", "gue_warlord"},
        new String[]{"gue_charge", "gue_rage"},
        0.9, 0.6, 1.5),

    // ==================== CHASSEUR ====================

    /**
     * GATLING - "Je mitraille, je kite, jamais d'arrêt"
     * Loop: Tir continu, gestion de position, stack pressure
     * Forces: DPS soutenu, kiting
     * Faiblesses: Vulnérable si coincé, demande skill
     */
    CHASSEUR_GATLING(ClassType.CHASSEUR, "Gatling", "§a⚡",
        "Tu mitrailles en continu, tu kites au millimètre.",
        new String[]{"cha_reload", "cha_crit_1", "cha_speed"},
        new String[]{"cha_multishot", "cha_roll"},
        0.7, 1.1, 1.2),

    /**
     * FANTÔME - "Je disparais, je headshot, je repars"
     * Loop: Stealth, burst sur une cible, disparition
     * Forces: Pick-off, survie, burst single-target
     * Faiblesses: Mauvais en horde, dépendant de l'invisibilité
     */
    CHASSEUR_FANTOME(ClassType.CHASSEUR, "Fantôme", "§b👻",
        "Tu pick une cible, tu l'effaces, tu t'évanouis.",
        new String[]{"cha_stealth", "cha_headshot", "cha_deadeye"},
        new String[]{"cha_deadeye", "cha_roll"},
        0.8, 0.5, 1.6),

    /**
     * PIÉGEUR - "Je piège le terrain, je fais exploser"
     * Loop: Pose, kite vers les pièges, déclenche, enchaîne
     * Forces: Contrôle de zone, clear défensif
     * Faiblesses: Faible mono-cible, setup requis
     */
    CHASSEUR_PIEGEUR(ClassType.CHASSEUR, "Piégeur", "§e💣",
        "Tu transformes la map en champ de mines.",
        new String[]{"cha_pierce", "cha_speed", "cha_reload"},
        new String[]{"cha_trap", "cha_roll"},
        0.9, 1.4, 0.7),

    // ==================== OCCULTISTE ====================

    /**
     * DÉFLAGRATION - "J'empile, j'explose, j'efface"
     * Loop: Stack corruption, puis détonate en AoE massif
     * Forces: Clear de horde absolu
     * Faiblesses: Setup time, très fragile, coûteux
     */
    OCCULTISTE_DEFLAGRATION(ClassType.OCCULTISTE, "Déflagration", "§5🔥",
        "Tu setup puis tu fais exploser des packs entiers.",
        new String[]{"occ_dot", "occ_aoe", "occ_power_1"},
        new String[]{"occ_orb", "occ_nova", "occ_apocalypse"},
        0.6, 1.6, 0.7),

    /**
     * MAGE DE SANG - "Je draine, je tank à ma façon"
     * Loop: Combat rapproché, drain pour sustain, risque/reward
     * Forces: Sustain unique, mid-range tank
     * Faiblesses: Très punissable si mal joué
     */
    OCCULTISTE_SANG(ClassType.OCCULTISTE, "Mage de Sang", "§4❤",
        "Tu voles la vie, mais tu joues au bord du gouffre.",
        new String[]{"occ_leech", "occ_shield", "occ_immortal"},
        new String[]{"occ_drain", "occ_nova"},
        1.2, 1.0, 1.0),

    /**
     * ARCHIMAGE - "Je gère ma ressource, je contrôle le tempo"
     * Loop: Rotation propre, combos, efficacité maximale
     * Forces: Polyvalent, excellent late-game
     * Faiblesses: Courbe d'apprentissage, faible si spam
     */
    OCCULTISTE_ARCHIMAGE(ClassType.OCCULTISTE, "Archimage", "§9✧",
        "Tu joues propre : combos, tempo, contrôle.",
        new String[]{"occ_energy_1", "occ_regen_energy", "occ_cdr"},
        new String[]{"occ_orb", "occ_drain", "occ_nova"},
        0.8, 1.1, 1.1),

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
