package com.rinaorc.zombiez.progression.journey;

import lombok.Getter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une étape individuelle dans un chapitre du parcours
 *
 * Chaque étape a:
 * - Un objectif à accomplir (type + valeur cible)
 * - Des récompenses (points, gems)
 * - Un statut (verrouillé, en cours, complété)
 */
@Getter
public enum JourneyStep {

    // ==================== CHAPITRE 1: PREMIERS PAS ====================

    STEP_1_1(JourneyChapter.CHAPTER_1, 1, "Tue ton premier zombie",
        "Première victime...", StepType.ZOMBIE_KILLS, 1,
        50, 1, Material.ROTTEN_FLESH),

    STEP_1_2(JourneyChapter.CHAPTER_1, 2, "Tue 10 zombies",
        "Tu prends le coup de main", StepType.ZOMBIE_KILLS, 10,
        100, 2, Material.IRON_SWORD),

    STEP_1_3(JourneyChapter.CHAPTER_1, 3, "Atteins le niveau 2",
        "Premier level up!", StepType.LEVEL, 2,
        150, 3, Material.EXPERIENCE_BOTTLE),

    STEP_1_4(JourneyChapter.CHAPTER_1, 4, "Explore la Zone 1 (50%)",
        "Découvre ton environnement", StepType.ZONE_PROGRESS, 50,
        200, 4, Material.COMPASS),

    STEP_1_5(JourneyChapter.CHAPTER_1, 5, "Trouve le Coffre Mystérieux",
        "Coordonnées: §b625, 93, 9853", StepType.DISCOVER_CHEST, 1,
        250, 4, Material.CHEST),

    STEP_1_6(JourneyChapter.CHAPTER_1, 6, "Chasse 1 animal",
        "Les animaux peuvent te nourrir!", StepType.PASSIVE_ANIMAL_KILLS, 1,
        275, 4, Material.COOKED_BEEF),

    STEP_1_7(JourneyChapter.CHAPTER_1, 7, "Atteins le niveau 3",
        "Prêt à choisir ta voie", StepType.LEVEL, 3,
        300, 5, Material.NETHER_STAR),

    // ==================== CHAPITRE 2: CHOIX DU DESTIN ====================

    STEP_2_1(JourneyChapter.CHAPTER_2, 1, "Choisis ta classe et ta voie",
        "Classe puis spécialisation!", StepType.SELECT_CLASS_AND_BRANCH, 2,
        100, 3, Material.NETHERITE_CHESTPLATE),

    STEP_2_2(JourneyChapter.CHAPTER_2, 2, "Recycle 5 items",
        "Libère ton inventaire avec /recycle", StepType.RECYCLE_ITEMS, 5,
        200, 5, Material.GRINDSTONE),

    STEP_2_3(JourneyChapter.CHAPTER_2, 3, "Explore la Zone 2 (50%)",
        "Découvre les faubourgs oubliés", StepType.ZONE_EXPLORATION, 2,
        250, 6, Material.COMPASS),

    STEP_2_4(JourneyChapter.CHAPTER_2, 4, "Soigne le mineur blessé",
        "Utilise un bandage - Coords: §b1036, 82, 9627", StepType.HEAL_NPC, 1,
        300, 8, Material.PAPER),

    STEP_2_5(JourneyChapter.CHAPTER_2, 5, "Trouve le coffre mystérieux (Zone 2)",
        "Coordonnées: §b373, 94, 9767", StepType.DISCOVER_CHEST, 2,
        350, 8, Material.CHEST),

    STEP_2_6(JourneyChapter.CHAPTER_2, 6, "Tue 20 Pyromorts",
        "Zone météore: §b~345, ~86, ~9500", StepType.FIRE_ZOMBIE_KILLS, 20,
        400, 10, Material.FIRE_CHARGE),

    STEP_2_7(JourneyChapter.CHAPTER_2, 7, "Aide Igor le survivant",
        "Récupère 5 caisses - Coords: §b898, 90, 9469", StepType.COLLECT_SUPPLY_CRATES, 5,
        450, 12, Material.CHEST_MINECART),

    STEP_2_8(JourneyChapter.CHAPTER_2, 8, "Explore la Zone 3 (50%)",
        "Les champs du silence t'attendent", StepType.ZONE_EXPLORATION, 3,
        500, 12, Material.FILLED_MAP),

    STEP_2_9(JourneyChapter.CHAPTER_2, 9, "Trouve le coffre mystérieux (Zone 3)",
        "Coordonnées: §b463, 121, 9440", StepType.DISCOVER_CHEST, 3,
        550, 14, Material.CHEST),

    STEP_2_10(JourneyChapter.CHAPTER_2, 10, "Tue le Seigneur du Manoir",
        "Manoir: §b728, 89, 9503", StepType.KILL_MANOR_BOSS, 1,
        750, 20, Material.DRAGON_HEAD),

    // ==================== CHAPITRE 3: PRENDRE SES MARQUES ====================

    STEP_3_1(JourneyChapter.CHAPTER_3, 1, "Découvre les Compagnons",
        "Ouvre un oeuf puis équipe ton pet", StepType.OPEN_AND_EQUIP_PET, 2,
        150, 5, Material.EGG),

    STEP_3_2(JourneyChapter.CHAPTER_3, 2, "Débloque Fort Havegris",
        "Active le beacon - Coords: §b675, 90, 9174", StepType.ACTIVATE_REFUGE_BEACON, 1,
        200, 6, Material.BEACON),

    STEP_3_3(JourneyChapter.CHAPTER_3, 3, "Tue 25 zombies",
        "Prouve ta valeur au combat!", StepType.ZOMBIE_KILLS, 25,
        300, 8, Material.ZOMBIE_HEAD),

    STEP_3_4(JourneyChapter.CHAPTER_3, 4, "L'Énigme du Forain",
        "Résous le puzzle - Coords: §b322, 93, 9201", StepType.SOLVE_CIRCUS_PUZZLE, 1,
        400, 10, Material.FIREWORK_ROCKET),

    STEP_3_5(JourneyChapter.CHAPTER_3, 5, "Sauve le chat perdu",
        "Trouve le chat - Coords: §b1025, 120, 9136", StepType.RESCUE_LOST_CAT, 1,
        500, 12, Material.STRING),

    STEP_3_6(JourneyChapter.CHAPTER_3, 6, "L'Enquête du Patient Zéro",
        "Explore la maison - Coords: §b875, 88, 8944", StepType.INVESTIGATE_PATIENT_ZERO, 4,
        600, 15, Material.WRITABLE_BOOK),

    STEP_3_7(JourneyChapter.CHAPTER_3, 7, "Protège le Village",
        "Défends le survivant - Coords: §b527, 90, 8994", StepType.DEFEND_VILLAGE, 1,
        700, 18, Material.SHIELD),

    STEP_3_8(JourneyChapter.CHAPTER_3, 8, "Répare le Zeppelin",
        "Monte à bord et répare - Coords: §b345, 148, 8907", StepType.REPAIR_ZEPPELIN, 1,
        800, 20, Material.COMMAND_BLOCK),

    STEP_3_9(JourneyChapter.CHAPTER_3, 9, "Maîtrise ta Classe",
        "Atteins niveau 5 et active ton 2ème talent", StepType.CLASS_MASTERY, 2,
        900, 22, Material.ENCHANTING_TABLE),

    STEP_3_10(JourneyChapter.CHAPTER_3, 10, "Tue le Seigneur des Profondeurs",
        "Mine abandonnée: §b1063, 76, 9127", StepType.KILL_MINE_BOSS, 1,
        1000, 25, Material.DRAGON_HEAD),

    // ==================== CHAPITRE 4: L'ART DU COMBAT ====================

    STEP_4_1(JourneyChapter.CHAPTER_4, 1, "Accomplis un événement",
        "Airdrop, Horde, Convoy, Boss ou Nid", StepType.PARTICIPATE_EVENT, 1,
        200, 8, Material.BEACON),

    STEP_4_2(JourneyChapter.CHAPTER_4, 2, "Le Fossoyeur",
        "Cimetière: §b656, 91, 8682", StepType.GRAVEDIGGER_QUEST, 7,
        300, 10, Material.IRON_SHOVEL),

    STEP_4_3(JourneyChapter.CHAPTER_4, 3, "Tue 25 zombies en 1 vie",
        "Série de kills!", StepType.KILL_STREAK, 25,
        400, 12, Material.GOLDEN_SWORD),

    STEP_4_4(JourneyChapter.CHAPTER_4, 4, "Atteins niveau de classe 15",
        "Expert de ta classe", StepType.CLASS_LEVEL, 15,
        500, 15, Material.DIAMOND_CHESTPLATE),

    STEP_4_5(JourneyChapter.CHAPTER_4, 5, "Atteins le niveau 25",
        "Les zones 4-5 s'ouvrent à toi", StepType.LEVEL, 25,
        600, 18, Material.SPRUCE_FENCE_GATE),

    // ==================== CHAPITRE 5: TERRITOIRE HOSTILE ====================

    STEP_5_1(JourneyChapter.CHAPTER_5, 1, "Entre dans la Zone 4",
        "Le danger s'intensifie", StepType.REACH_ZONE, 4,
        300, 10, Material.IRON_CHESTPLATE),

    STEP_5_2(JourneyChapter.CHAPTER_5, 2, "Recycle 25 items",
        "Le recyclage, c'est la vie!", StepType.RECYCLE_ITEMS, 25,
        400, 12, Material.GRINDSTONE),

    STEP_5_3(JourneyChapter.CHAPTER_5, 3, "Tue ton premier zombie Élite",
        "Les élites sont plus dangereux", StepType.ELITE_KILLS, 1,
        500, 15, Material.GOLDEN_APPLE),

    STEP_5_4(JourneyChapter.CHAPTER_5, 4, "Atteins la Zone 5",
        "Toujours plus loin", StepType.REACH_ZONE, 5,
        600, 18, Material.DIAMOND_BOOTS),

    STEP_5_5(JourneyChapter.CHAPTER_5, 5, "Atteins le niveau 30",
        "L'arbre de compétences t'attend", StepType.LEVEL, 30,
        750, 22, Material.OAK_SAPLING),

    // ==================== CHAPITRE 6: SPÉCIALISATION ====================

    STEP_6_1(JourneyChapter.CHAPTER_6, 1, "Débloque 3 skills passifs",
        "Commence à te spécialiser", StepType.UNLOCK_SKILLS, 3,
        400, 15, Material.BOOK),

    STEP_6_2(JourneyChapter.CHAPTER_6, 2, "Complète une branche jusqu'au Tier 2",
        "Approfondis une spécialité", StepType.SKILL_TIER, 2,
        500, 18, Material.BOOKSHELF),

    STEP_6_3(JourneyChapter.CHAPTER_6, 3, "Tue 500 zombies au total",
        "Exterminateur confirmé", StepType.TOTAL_KILLS, 500,
        600, 20, Material.WITHER_SKELETON_SKULL),

    STEP_6_4(JourneyChapter.CHAPTER_6, 4, "Tue 10 zombies Élites",
        "Chasseur d'élites débutant", StepType.ELITE_KILLS, 10,
        700, 22, Material.GOLDEN_SWORD),

    STEP_6_5(JourneyChapter.CHAPTER_6, 5, "Atteins le niveau 35",
        "Battle Pass accessible!", StepType.LEVEL, 35,
        850, 28, Material.NETHER_STAR),

    // ==================== CHAPITRE 7: ÉPREUVE DU SANG ====================

    STEP_7_1(JourneyChapter.CHAPTER_7, 1, "Participe à un événement",
        "Les événements sont plus dangereux", StepType.PARTICIPATE_EVENT, 1,
        500, 20, Material.BEACON),

    STEP_7_2(JourneyChapter.CHAPTER_7, 2, "Survie à une Blood Moon",
        "La nuit rouge...", StepType.SURVIVE_BLOOD_MOON, 1,
        750, 25, Material.REDSTONE_BLOCK),

    STEP_7_3(JourneyChapter.CHAPTER_7, 3, "Recycle 100 items",
        "Maître du recyclage", StepType.RECYCLE_ITEMS, 100,
        1000, 30, Material.GRINDSTONE),

    STEP_7_4(JourneyChapter.CHAPTER_7, 4, "Tue 25 Élites au total",
        "Chasseur d'élites confirmé", StepType.ELITE_KILLS, 25,
        1000, 30, Material.DIAMOND_SWORD),

    STEP_7_5(JourneyChapter.CHAPTER_7, 5, "Atteins le niveau 45",
        "Talents Tier 2 accessibles!", StepType.LEVEL, 45,
        1200, 35, Material.ENCHANTED_BOOK),

    // ==================== CHAPITRE 8: CHASSEUR D'ÉLITES ====================

    STEP_8_1(JourneyChapter.CHAPTER_8, 1, "Tue 50 zombies Élites",
        "Spécialiste anti-élites", StepType.ELITE_KILLS, 50,
        1000, 30, Material.GOLDEN_APPLE),

    STEP_8_2(JourneyChapter.CHAPTER_8, 2, "Atteins la Zone 7",
        "Les bois sombres t'attendent", StepType.REACH_ZONE, 7,
        1200, 32, Material.DARK_OAK_FENCE_GATE),

    STEP_8_3(JourneyChapter.CHAPTER_8, 3, "Tue 100 zombies en 1 vie",
        "Inarrêtable!", StepType.KILL_STREAK, 100,
        1500, 38, Material.TOTEM_OF_UNDYING),

    STEP_8_4(JourneyChapter.CHAPTER_8, 4, "Atteins niveau de classe 35",
        "Expert de classe avancé", StepType.CLASS_LEVEL, 35,
        1500, 40, Material.DIAMOND_CHESTPLATE),

    STEP_8_5(JourneyChapter.CHAPTER_8, 5, "Atteins le niveau 55",
        "Talents Tier 3 accessibles!", StepType.LEVEL, 55,
        1800, 45, Material.ENCHANTED_BOOK),

    // ==================== CHAPITRE 9: MAÎTRE DE CLASSE ====================

    STEP_9_1(JourneyChapter.CHAPTER_9, 1, "Débloque tous les talents Tier 1-3",
        "Arsenal complet", StepType.TALENTS_UNLOCKED, 3,
        1500, 40, Material.ENCHANTED_BOOK),

    STEP_9_2(JourneyChapter.CHAPTER_9, 2, "Tue 5 Boss",
        "Chasseur de boss débutant", StepType.BOSS_KILLS, 5,
        2000, 45, Material.DRAGON_HEAD),

    STEP_9_3(JourneyChapter.CHAPTER_9, 3, "Tue 2,000 zombies au total",
        "Exterminateur vétéran", StepType.TOTAL_KILLS, 2000,
        2000, 45, Material.WITHER_SKELETON_SKULL),

    STEP_9_4(JourneyChapter.CHAPTER_9, 4, "Atteins niveau de classe 40",
        "Proche de la maîtrise", StepType.CLASS_LEVEL, 40,
        2000, 48, Material.NETHERITE_CHESTPLATE),

    STEP_9_5(JourneyChapter.CHAPTER_9, 5, "Atteins le niveau 60",
        "Talents Tier 4 + Zones avancées!", StepType.LEVEL, 60,
        2500, 55, Material.END_CRYSTAL),

    // ==================== CHAPITRE 10: TERRES MAUDITES ====================

    STEP_10_1(JourneyChapter.CHAPTER_10, 1, "Atteins la Zone 8",
        "Les ruines anciennes...", StepType.REACH_ZONE, 8,
        2000, 50, Material.CRACKED_STONE_BRICKS),

    STEP_10_2(JourneyChapter.CHAPTER_10, 2, "Survie aux effets environnementaux 10min",
        "Résistance aux éléments", StepType.SURVIVE_ENVIRONMENT, 600,
        2500, 55, Material.POTION),

    STEP_10_3(JourneyChapter.CHAPTER_10, 3, "Tue 100 zombies en Zone 8+",
        "Combat extrême", StepType.ADVANCED_ZONE_KILLS, 100,
        3000, 60, Material.NETHERITE_SWORD),

    STEP_10_4(JourneyChapter.CHAPTER_10, 4, "Atteins la Zone 10",
        "L'avant-poste abandonné", StepType.REACH_ZONE, 10,
        3500, 65, Material.CRIMSON_FENCE_GATE),

    STEP_10_5(JourneyChapter.CHAPTER_10, 5, "Atteins le niveau 75",
        "La zone finale s'approche...", StepType.LEVEL, 75,
        4000, 75, Material.NETHERITE_INGOT),

    // ==================== CHAPITRE 11: TITAN SLAYER ====================

    STEP_11_1(JourneyChapter.CHAPTER_11, 1, "Tue 25 Boss",
        "Chasseur de titans", StepType.BOSS_KILLS, 25,
        4000, 70, Material.DRAGON_HEAD),

    STEP_11_2(JourneyChapter.CHAPTER_11, 2, "Tue un Boss en Zone 8+",
        "Boss en territoire hostile", StepType.ADVANCED_BOSS_KILL, 1,
        5000, 80, Material.END_CRYSTAL),

    STEP_11_3(JourneyChapter.CHAPTER_11, 3, "Atteins la Zone 11",
        "La forêt putréfiée...", StepType.REACH_ZONE, 11,
        5000, 85, Material.WARPED_FENCE_GATE),

    STEP_11_4(JourneyChapter.CHAPTER_11, 4, "Tue 5,000 zombies au total",
        "Véritable fléau", StepType.TOTAL_KILLS, 5000,
        5000, 85, Material.WITHER_SKELETON_SKULL),

    STEP_11_5(JourneyChapter.CHAPTER_11, 5, "Atteins le niveau 100",
        "Prestige accessible!", StepType.LEVEL, 100,
        7500, 120, Material.ENCHANTED_GOLDEN_APPLE),

    // ==================== CHAPITRE 12: LÉGENDE VIVANTE ====================

    STEP_12_1(JourneyChapter.CHAPTER_12, 1, "Effectue ton premier Prestige",
        "Recommence plus fort", StepType.PRESTIGE, 1,
        10000, 100, Material.ENCHANTED_GOLDEN_APPLE),

    STEP_12_2(JourneyChapter.CHAPTER_12, 2, "Atteins niveau 50 en Prestige 1",
        "Progression accélérée", StepType.PRESTIGE_LEVEL, 50,
        7500, 80, Material.EXPERIENCE_BOTTLE),

    STEP_12_3(JourneyChapter.CHAPTER_12, 3, "Tue Patient Zéro",
        "Le boss final...", StepType.KILL_PATIENT_ZERO, 1,
        15000, 150, Material.WITHER_SKELETON_SKULL),

    STEP_12_4(JourneyChapter.CHAPTER_12, 4, "Complète 50 achievements",
        "Collectionneur accompli", StepType.ACHIEVEMENTS, 50,
        10000, 100, Material.NETHER_STAR),

    STEP_12_5(JourneyChapter.CHAPTER_12, 5, "Atteins Prestige 2",
        "La légende continue...", StepType.PRESTIGE, 2,
        15000, 200, Material.DRAGON_EGG);

    private final JourneyChapter chapter;
    private final int stepNumber;
    private final String name;
    private final String description;
    private final StepType type;
    private final int targetValue;
    private final int pointReward;
    private final int gemReward;
    private final Material icon;

    // Cache statique pour accès rapide
    private static final Map<JourneyChapter, List<JourneyStep>> BY_CHAPTER = new HashMap<>();
    private static final Map<String, JourneyStep> BY_ID = new HashMap<>();

    static {
        // Initialiser les caches
        for (JourneyChapter chapter : JourneyChapter.values()) {
            BY_CHAPTER.put(chapter, new ArrayList<>());
        }
        for (JourneyStep step : values()) {
            BY_CHAPTER.get(step.chapter).add(step);
            BY_ID.put(step.getId(), step);
        }
    }

    JourneyStep(JourneyChapter chapter, int stepNumber, String name, String description,
                StepType type, int targetValue, int pointReward, int gemReward, Material icon) {
        this.chapter = chapter;
        this.stepNumber = stepNumber;
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.pointReward = pointReward;
        this.gemReward = gemReward;
        this.icon = icon;
    }

    /**
     * Obtient l'ID unique de l'étape (ex: "1_3" pour Chapitre 1, Étape 3)
     */
    public String getId() {
        return chapter.getId() + "_" + stepNumber;
    }

    /**
     * Obtient le nom formaté
     */
    public String getFormattedName() {
        return chapter.getColor() + name;
    }

    /**
     * Obtient le texte de progression (ex: "0/25 zombies")
     */
    public String getProgressText(int current) {
        return switch (type) {
            case ZOMBIE_KILLS, CLASS_KILLS, TOTAL_KILLS, ZONE_KILLS,
                 ADVANCED_ZONE_KILLS, EVENT_KILLS -> current + "/" + targetValue + " zombies";
            case ELITE_KILLS -> current + "/" + targetValue + " élites";
            case BOSS_KILLS, ADVANCED_BOSS_KILL -> current + "/" + targetValue + " boss";
            case LEVEL, CLASS_LEVEL, PRESTIGE_LEVEL -> "Niveau " + current + "/" + targetValue;
            case REACH_ZONE -> "Zone " + (current >= targetValue ? "✓" : current + "/" + targetValue);
            case ZONE_PROGRESS -> current + "/" + targetValue + "%";
            case SURVIVE_ZONE_TIME, SURVIVE_ENVIRONMENT -> formatTime(current) + "/" + formatTime(targetValue);
            case KILL_STREAK -> current + "/" + targetValue + " kills d'affilée";
            case SELECT_CLASS -> current >= 1 ? "✓ Classe choisie" : "Fais /classe";
            case SELECT_CLASS_AND_BRANCH -> {
                if (current >= 2) yield "✓ Classe et voie choisies";
                else if (current >= 1) yield "1/2 - Choisis ta voie!";
                else yield "0/2 - Fais /classe";
            }
            case USE_CLASS_TRAIT, USE_TALENT -> current + "/" + targetValue + " utilisations";
            case UNLOCK_TALENT, UNLOCK_SKILLS, TALENTS_UNLOCKED -> current + "/" + targetValue + " débloqué(s)";
            case SKILL_TIER -> "Tier " + current + "/" + targetValue;
            case PARTICIPATE_EVENT -> current + "/" + targetValue + " événement(s)";
            case SURVIVE_BLOOD_MOON -> current + "/" + targetValue + " Blood Moon(s)";
            case PRESTIGE -> "Prestige " + current + "/" + targetValue;
            case KILL_PATIENT_ZERO -> current >= 1 ? "✓ Patient Zéro vaincu" : "Vaincs Patient Zéro";
            case ACHIEVEMENTS -> current + "/" + targetValue + " achievements";
            case RECYCLE_ITEMS -> current + "/" + targetValue + " items recyclés";
            case DISCOVER_CHEST -> current >= targetValue ? "✓ Coffre découvert!" : "Cherche le coffre...";
            case PASSIVE_ANIMAL_KILLS -> current + "/" + targetValue + " animal chassé";
            case ZONE_EXPLORATION -> current + "/" + 50 + "% exploré";
            case HEAL_NPC -> current >= targetValue ? "✓ PNJ soigné!" : "Trouve et soigne le PNJ";
            case COLLECT_SUPPLY_CRATES -> current + "/" + targetValue + " caisses récupérées";
            case FIRE_ZOMBIE_KILLS -> current + "/" + targetValue + " Pyromorts";
            case KILL_MANOR_BOSS -> current >= targetValue ? "✓ Boss vaincu!" : "Tue le boss du manoir";
            case KILL_MINE_BOSS -> current >= targetValue ? "✓ Boss vaincu!" : "Tue le Seigneur des Profondeurs";
            case OPEN_AND_EQUIP_PET -> {
                if (current >= 2) yield "✓ Compagnon équipé!";
                else if (current >= 1) yield "1/2 - Équipe ton pet! /pet";
                else yield "0/2 - Ouvre un oeuf /pet";
            }
            case ACTIVATE_REFUGE_BEACON -> current >= targetValue ? "✓ Refuge débloqué!" : "Active le beacon";
            case SOLVE_CIRCUS_PUZZLE -> current >= targetValue ? "✓ Puzzle résolu!" : "Parle au Forain";
            case RESCUE_LOST_CAT -> current >= targetValue ? "✓ Chat sauvé!" : "Trouve le chat perdu";
            case INVESTIGATE_PATIENT_ZERO -> current >= targetValue ? "✓ Enquête terminée!" : current + "/4 indices trouvés";
            case DEFEND_VILLAGE -> current >= targetValue ? "✓ Village défendu!" : "Parle au survivant";
            case REPAIR_ZEPPELIN -> current >= targetValue ? "✓ Zeppelin réparé!" : "Trouve le panneau de contrôle";
            case CLASS_MASTERY -> {
                if (current >= targetValue) yield "✓ Classe maîtrisée!";
                else if (current == 1) yield "1/2 - Active ton 2ème talent";
                else yield "0/2 - Atteins niveau 5 de classe";
            }
            case GRAVEDIGGER_QUEST -> {
                if (current >= 7) yield "✓ Fossoyeur terminé!";
                else if (current >= 6) yield "Tue le boss!";
                else if (current >= 1) yield (current - 1) + "/5 tombes creusées";
                else yield "Parle au prêtre";
            }
        };
    }

    /**
     * Formate le temps en mm:ss
     */
    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    /**
     * Vérifie si l'étape est complétée
     */
    public boolean isCompleted(int currentProgress) {
        // Pour ZONE_EXPLORATION, targetValue = zone ID, pas le pourcentage cible
        // Le pourcentage cible est toujours 50%
        if (type == StepType.ZONE_EXPLORATION) {
            return currentProgress >= 50;
        }
        return currentProgress >= targetValue;
    }

    /**
     * Obtient le pourcentage de progression
     */
    public double getProgressPercent(int currentProgress) {
        // Pour ZONE_EXPLORATION, le target est 50% (pas le zone ID)
        if (type == StepType.ZONE_EXPLORATION) {
            return Math.min(100.0, (double) currentProgress / 50 * 100);
        }
        return Math.min(100.0, (double) currentProgress / targetValue * 100);
    }

    /**
     * Obtient l'étape suivante dans le chapitre
     */
    public JourneyStep getNextInChapter() {
        List<JourneyStep> chapterSteps = BY_CHAPTER.get(chapter);
        int nextIndex = stepNumber; // stepNumber est 1-based, donc c'est déjà le bon index
        if (nextIndex < chapterSteps.size()) {
            return chapterSteps.get(nextIndex);
        }
        return null; // Dernière étape du chapitre
    }

    /**
     * Obtient toutes les étapes d'un chapitre
     */
    public static List<JourneyStep> getStepsForChapter(JourneyChapter chapter) {
        return BY_CHAPTER.getOrDefault(chapter, List.of());
    }

    /**
     * Obtient une étape par son ID
     */
    public static JourneyStep getById(String id) {
        return BY_ID.get(id);
    }

    /**
     * Obtient la première étape d'un chapitre
     */
    public static JourneyStep getFirstStep(JourneyChapter chapter) {
        List<JourneyStep> steps = BY_CHAPTER.get(chapter);
        return steps.isEmpty() ? null : steps.get(0);
    }

    /**
     * Obtient la dernière étape d'un chapitre
     */
    public static JourneyStep getLastStep(JourneyChapter chapter) {
        List<JourneyStep> steps = BY_CHAPTER.get(chapter);
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    /**
     * Types d'objectifs pour les étapes
     */
    @Getter
    public enum StepType {
        // Kills
        ZOMBIE_KILLS("Tue des zombies"),
        CLASS_KILLS("Tue des zombies avec ta classe"),
        TOTAL_KILLS("Tue des zombies au total"),
        ZONE_KILLS("Tue des zombies dans une zone spécifique"),
        ADVANCED_ZONE_KILLS("Tue des zombies en zone 8+"),
        ELITE_KILLS("Tue des zombies Élites"),
        BOSS_KILLS("Tue des Boss"),
        ADVANCED_BOSS_KILL("Tue un Boss en zone avancée"),
        EVENT_KILLS("Tue des zombies pendant un événement"),
        KILL_STREAK("Tue des zombies sans mourir"),
        KILL_PATIENT_ZERO("Vaincs Patient Zéro"),

        // Progression
        LEVEL("Atteins un niveau"),
        CLASS_LEVEL("Atteins un niveau de classe"),
        PRESTIGE("Effectue un prestige"),
        PRESTIGE_LEVEL("Atteins un niveau après prestige"),

        // Zones
        REACH_ZONE("Atteins une zone"),
        ZONE_PROGRESS("Progresse dans une zone"),
        SURVIVE_ZONE_TIME("Survie dans une zone"),
        SURVIVE_ENVIRONMENT("Survie aux effets environnementaux"),

        // Classe & Talents
        SELECT_CLASS("Choisis une classe"),
        SELECT_CLASS_AND_BRANCH("Choisis une classe et une voie"),
        USE_CLASS_TRAIT("Utilise ton trait de classe"),
        UNLOCK_TALENT("Débloque un talent"),
        USE_TALENT("Utilise un talent"),
        TALENTS_UNLOCKED("Débloque des talents de plusieurs tiers"),

        // Skills
        UNLOCK_SKILLS("Débloque des skills passifs"),
        SKILL_TIER("Atteins un tier de skill"),

        // Événements
        PARTICIPATE_EVENT("Participe à un événement"),
        SURVIVE_BLOOD_MOON("Survie à une Blood Moon"),

        // Achievements
        ACHIEVEMENTS("Complète des achievements"),

        // Recyclage
        RECYCLE_ITEMS("Recycle des items"),

        // Coffres mystères
        DISCOVER_CHEST("Découvre un coffre mystérieux"),

        // Chasse
        PASSIVE_ANIMAL_KILLS("Tue des animaux passifs"),

        // Exploration de zone (pourcentage)
        ZONE_EXPLORATION("Explore une zone"),

        // NPC interactions
        HEAL_NPC("Soigne un PNJ"),
        COLLECT_SUPPLY_CRATES("Récupère des caisses de ravitaillement"),

        // Zombies spéciaux
        FIRE_ZOMBIE_KILLS("Tue des Pyromorts"),

        // Boss spécifiques
        KILL_MANOR_BOSS("Tue le boss du manoir"),
        KILL_MINE_BOSS("Tue le boss de la mine"),

        // Pets
        OPEN_AND_EQUIP_PET("Ouvre et équipe un compagnon"),

        // Refuges
        ACTIVATE_REFUGE_BEACON("Active un beacon de refuge"),

        // Mini-jeux / Puzzles
        SOLVE_CIRCUS_PUZZLE("Résous le puzzle du cirque"),

        // Sauvetage
        RESCUE_LOST_CAT("Sauve le chat perdu"),

        // Enquêtes
        INVESTIGATE_PATIENT_ZERO("Enquête sur le Patient Zéro"),

        // Défense de zone
        DEFEND_VILLAGE("Défends le village"),

        // Réparation
        REPAIR_ZEPPELIN("Répare le Zeppelin"),

        // Maîtrise de classe
        CLASS_MASTERY("Maîtrise ta classe"),

        // Quête du Fossoyeur (Chapitre 4)
        GRAVEDIGGER_QUEST("Creuse les tombes et vaincs le boss");

        private final String description;

        StepType(String description) {
            this.description = description;
        }
    }
}
