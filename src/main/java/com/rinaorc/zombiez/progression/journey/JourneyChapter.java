package com.rinaorc.zombiez.progression.journey;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Les 21 Chapitres du Journal du Survivant
 *
 * Chaque chapitre représente une étape majeure de la progression
 * avec ses propres objectifs, récompenses et déblocages
 *
 * IMPORTANT: Le joueur DOIT compléter chaque chapitre pour progresser
 * Les zones et fonctionnalités sont BLOQUÉES tant que le chapitre n'est pas terminé
 */
@Getter
public enum JourneyChapter {

    // ==================== PHASE 1: ÉVEIL (Zones 1-6) ====================

    CHAPTER_1(
        1,
        "Premiers Pas",
        "§a",
        Material.WOODEN_SWORD,
        "Apprends les bases de la survie dans ce monde infesté",
        1, 1,  // Zone 1
        new JourneyGate[]{JourneyGate.CLASS_SELECTION, JourneyGate.ZONE_2, JourneyGate.ZONE_3}, // Débloque zones 2-3
        1500,  // Points bonus
        25,    // Gems bonus
        "§7Coffre de Bienvenue",
        "§aTutoriel Complété!"
    ),

    CHAPTER_2(
        2,
        "Choix du Destin",
        "§a",
        Material.NETHERITE_CHESTPLATE,
        "Choisis ta classe et commence à la maîtriser",
        2, 3,  // Zones 2-3
        new JourneyGate[]{JourneyGate.ZONE_4, JourneyGate.ZONE_5, JourneyGate.ZONE_6}, // Débloque zones 4-6
        2000,
        35,
        "§aÉquipement de Classe Débutant",
        "§aClasse Choisie!"
    ),

    CHAPTER_3(
        3,
        "Prendre ses Marques",
        "§a",
        Material.COMPASS,
        "Explore les premières zones dangereuses",
        4, 6,  // Zones 4-6
        new JourneyGate[]{JourneyGate.ZONE_7, JourneyGate.ZONE_8}, // Débloque zones 7-8
        2500,
        45,
        "§eCoffre de Zone (Rare garanti)",
        ""
    ),

    // ==================== PHASE 2: CROISSANCE (Zones 7-12) ====================

    CHAPTER_4(
        4,
        "L'Art du Combat",
        "§e",
        Material.DIAMOND_SWORD,
        "Maîtrise tes premiers talents de classe",
        7, 8,  // Zones 7-8
        new JourneyGate[]{JourneyGate.ZONE_9, JourneyGate.ZONE_10, JourneyGate.DAILY_MISSIONS}, // Débloque zones 9-10
        3500,
        60,
        "§eTalent Booster (XP Classe x2 - 1h)",
        "§eMissions Quotidiennes Débloquées!"
    ),

    CHAPTER_5(
        5,
        "Territoire Hostile",
        "§e",
        Material.IRON_CHESTPLATE,
        "Conquiers les zones intermédiaires et affronte les Élites",
        9, 10,  // Zones 9-10
        new JourneyGate[]{JourneyGate.SKILL_TREE, JourneyGate.ZONE_11, JourneyGate.ZONE_12}, // Débloque zones 11-12
        4000,
        75,
        "§b+3 Points de Skill Gratuits",
        "§eArbre de Compétences Débloqué!"
    ),

    CHAPTER_6(
        6,
        "Spécialisation",
        "§e",
        Material.OAK_SAPLING,
        "Développe ton arbre de compétences passives",
        11, 12,  // Zones 11-12
        new JourneyGate[]{JourneyGate.ZONE_13, JourneyGate.ZONE_14, JourneyGate.ZONE_15, JourneyGate.BATTLE_PASS, JourneyGate.WEEKLY_MISSIONS}, // Débloque zones 13-15
        5000,
        100,
        "§dPremium Pass Trial (3 jours)",
        "§eBattle Pass Débloqué!"
    ),

    // ==================== PHASE 3: AFFIRMATION (Zones 13-20) ====================

    CHAPTER_7(
        7,
        "Épreuve du Sang",
        "§c",
        Material.REDSTONE,
        "Survie aux événements spéciaux et aux Blood Moons",
        13, 15,  // Zones 13-15
        new JourneyGate[]{JourneyGate.ZONE_16, JourneyGate.ZONE_17, JourneyGate.TALENTS_TIER_2}, // Débloque zones 16-17
        6500,
        125,
        "§dCoffre Événement (Épique garanti)",
        "§cBlood Moon Conquise!"
    ),

    CHAPTER_8(
        8,
        "Chasseur d'Élites",
        "§c",
        Material.GOLDEN_SWORD,
        "Traque les zombies Élites et les menaces supérieures",
        16, 17,  // Zones 16-17
        new JourneyGate[]{JourneyGate.TALENTS_TIER_3, JourneyGate.ZONE_18, JourneyGate.ZONE_19, JourneyGate.ZONE_20}, // Débloque zones 18-20
        8000,
        150,
        "§dTitre: §c⚔ Chasseur d'Élites",
        "§cTalents Tier 3 Débloqués!"
    ),

    CHAPTER_9(
        9,
        "Maître de Classe",
        "§c",
        Material.ENCHANTED_BOOK,
        "Atteins la maîtrise complète de ta classe",
        18, 20,  // Zones 18-20
        new JourneyGate[]{JourneyGate.ZONE_21, JourneyGate.ZONE_22, JourneyGate.TALENTS_TIER_4}, // Débloque zones 21-22
        10000,
        175,
        "§5Set d'Équipement Classe (Épique)",
        "§cMaître de Classe!"
    ),

    // ==================== PHASE 4: MAÎTRISE (Zones 21-27) ====================

    CHAPTER_10(
        10,
        "Terres Maudites",
        "§5",
        Material.NETHERITE_INGOT,
        "Explore les zones les plus dangereuses du monde",
        21, 22,  // Zones 21-22
        new JourneyGate[]{JourneyGate.ZONE_23, JourneyGate.ZONE_24, JourneyGate.ZONE_25}, // Débloque zones 23-25
        15000,
        225,
        "§5Équipement Résistance Environnement",
        "§5Zones Maudites Accessibles!"
    ),

    CHAPTER_11(
        11,
        "Titan Slayer",
        "§5",
        Material.DRAGON_HEAD,
        "Affronte les Boss les plus puissants",
        23, 25,  // Zones 23-25
        new JourneyGate[]{JourneyGate.ZONE_26, JourneyGate.ZONE_27, JourneyGate.PRESTIGE}, // Débloque zones 26-27
        25000,
        350,
        "§6Arme Légendaire + Titre: §5⚔ Titan Slayer",
        "§5Prestige Débloqué!"
    ),

    CHAPTER_12(
        12,
        "Légende Vivante",
        "§6",
        Material.NETHER_STAR,
        "Deviens une légende de ZombieZ",
        26, 27,  // Zones 26-27
        new JourneyGate[]{JourneyGate.PRESTIGE_2, JourneyGate.ZONE_28, JourneyGate.ZONE_29}, // Débloque zones 28-29
        50000,
        500,
        "§6§l✦ Set Légendaire Exclusif + Aura ✦",
        "§6§l✦ LÉGENDE VIVANTE ✦"
    ),

    // ==================== PHASE 5: TRANSCENDANCE (Zones 28-40) ====================

    CHAPTER_13(
        13,
        "Renaissance",
        "§b",
        Material.END_CRYSTAL,
        "Transcende tes limites avec le Prestige",
        28, 29,  // Zones 28-29
        new JourneyGate[]{JourneyGate.ZONE_30, JourneyGate.ZONE_31, JourneyGate.ZONE_32}, // Débloque zones 30-32
        60000,
        600,
        "§bCoffre de Renaissance",
        "§b✦ Transcendance Initiée ✦"
    ),

    CHAPTER_14(
        14,
        "Voie des Anciens",
        "§b",
        Material.TOTEM_OF_UNDYING,
        "Découvre les secrets des survivants légendaires",
        30, 32,  // Zones 30-32
        new JourneyGate[]{JourneyGate.ZONE_33, JourneyGate.ZONE_34, JourneyGate.TALENTS_TIER_5}, // Débloque zones 33-34
        75000,
        700,
        "§bRelique des Anciens",
        "§bSagesse Ancestrale!"
    ),

    CHAPTER_15(
        15,
        "Éclipse Sanglante",
        "§4",
        Material.WITHER_SKELETON_SKULL,
        "Survie aux événements cataclysmiques",
        33, 34,  // Zones 33-34
        new JourneyGate[]{JourneyGate.ZONE_35, JourneyGate.ZONE_36, JourneyGate.ZONE_37}, // Débloque zones 35-37
        90000,
        800,
        "§4Arme de l'Éclipse",
        "§4Survivant de l'Éclipse!"
    ),

    CHAPTER_16(
        16,
        "Gardien du Monde",
        "§b",
        Material.BEACON,
        "Protège les derniers bastions de l'humanité",
        35, 37,  // Zones 35-37
        new JourneyGate[]{JourneyGate.ZONE_38, JourneyGate.ZONE_39, JourneyGate.ZONE_40, JourneyGate.PRESTIGE_3}, // Débloque zones 38-40
        110000,
        900,
        "§bBouclier du Gardien",
        "§b⚔ Gardien du Monde ⚔"
    ),

    CHAPTER_17(
        17,
        "Apocalypse",
        "§4",
        Material.TNT,
        "Affronte la horde ultime",
        38, 40,  // Zones 38-40
        new JourneyGate[]{JourneyGate.ZONE_41, JourneyGate.ZONE_42, JourneyGate.ZONE_43}, // Débloque zones 41-43
        130000,
        1000,
        "§4§lArme Apocalyptique",
        "§4§l☠ Survivant de l'Apocalypse ☠"
    ),

    // ==================== PHASE 6: ÉTERNITÉ (Zones 41-50) ====================

    CHAPTER_18(
        18,
        "Au-delà du Voile",
        "§d",
        Material.END_PORTAL_FRAME,
        "Explore les dimensions corrompues",
        41, 43,  // Zones 41-43
        new JourneyGate[]{JourneyGate.DIMENSION_CORRUPTED, JourneyGate.ZONE_44, JourneyGate.ZONE_45, JourneyGate.ZONE_46}, // Débloque zones 44-46
        150000,
        1200,
        "§dClé Dimensionnelle",
        "§d✦ Voyageur Dimensionnel ✦"
    ),

    CHAPTER_19(
        19,
        "Némésis",
        "§4",
        Material.NETHERITE_SWORD,
        "Chasse les Archontes de l'infection",
        44, 46,  // Zones 44-46
        new JourneyGate[]{JourneyGate.PRESTIGE_4, JourneyGate.ZONE_47, JourneyGate.ZONE_48}, // Débloque zones 47-48
        175000,
        1400,
        "§4Lame de Némésis",
        "§4⚔ Chasseur d'Archontes ⚔"
    ),

    CHAPTER_20(
        20,
        "Dernier Bastion",
        "§d",
        Material.RESPAWN_ANCHOR,
        "Établis le sanctuaire ultime de l'humanité",
        47, 48,  // Zones 47-48
        new JourneyGate[]{JourneyGate.SANCTUARY, JourneyGate.ZONE_49, JourneyGate.ZONE_50}, // Débloque zones 49-50
        200000,
        1600,
        "§dPierre du Sanctuaire",
        "§d✦ Fondateur du Bastion ✦"
    ),

    CHAPTER_21(
        21,
        "Éternel",
        "§6",
        Material.DRAGON_EGG,
        "Deviens immortel dans la légende de ZombieZ",
        49, 50,  // Zones 49-50
        new JourneyGate[]{}, // Fin du journal ultime
        300000,
        2000,
        "§6§l✦ Titre Éternel + Cosmétiques Exclusifs ✦",
        "§6§l✦✦✦ ÉTERNEL ✦✦✦"
    );

    private final int id;
    private final String name;
    private final String color;
    private final Material icon;
    private final String description;
    private final int minZone;
    private final int maxZone;
    private final JourneyGate[] unlocks;
    private final int bonusPoints;
    private final int bonusGems;
    private final String bonusReward;
    private final String completionMessage;

    JourneyChapter(int id, String name, String color, Material icon, String description,
                   int minZone, int maxZone, JourneyGate[] unlocks,
                   int bonusPoints, int bonusGems, String bonusReward, String completionMessage) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.minZone = minZone;
        this.maxZone = maxZone;
        this.unlocks = unlocks;
        this.bonusPoints = bonusPoints;
        this.bonusGems = bonusGems;
        this.bonusReward = bonusReward;
        this.completionMessage = completionMessage;
    }

    /**
     * Obtient le texte d'affichage des zones (ex: "Zone 1" ou "Zones 2-3")
     */
    public String getZoneDisplay() {
        if (minZone == maxZone) {
            return "Zone " + minZone;
        }
        return "Zones " + minZone + "-" + maxZone;
    }

    /**
     * Obtient le nom coloré du chapitre
     */
    public String getColoredName() {
        return color + name;
    }

    /**
     * Obtient le titre formaté du chapitre
     */
    public String getFormattedTitle() {
        return color + "§lChapitre " + id + ": " + name;
    }

    /**
     * Obtient la phase du chapitre (1-6)
     */
    public int getPhase() {
        if (id <= 3) return 1;
        if (id <= 6) return 2;
        if (id <= 9) return 3;
        if (id <= 12) return 4;
        if (id <= 17) return 5;
        return 6;
    }

    /**
     * Obtient le nom de la phase
     */
    public String getPhaseName() {
        return switch (getPhase()) {
            case 1 -> "§a⚔ ÉVEIL";
            case 2 -> "§e⚔ CROISSANCE";
            case 3 -> "§c⚔ AFFIRMATION";
            case 4 -> "§5⚔ MAÎTRISE";
            case 5 -> "§b⚔ TRANSCENDANCE";
            case 6 -> "§d⚔ ÉTERNITÉ";
            default -> "§7???";
        };
    }

    /**
     * Obtient le chapitre suivant
     */
    public JourneyChapter getNext() {
        JourneyChapter[] chapters = values();
        int nextIndex = this.ordinal() + 1;
        if (nextIndex < chapters.length) {
            return chapters[nextIndex];
        }
        return null; // Dernier chapitre
    }

    /**
     * Obtient le chapitre précédent
     */
    public JourneyChapter getPrevious() {
        if (this.ordinal() == 0) return null;
        return values()[this.ordinal() - 1];
    }

    /**
     * Obtient un chapitre par son ID
     */
    public static JourneyChapter getById(int id) {
        for (JourneyChapter chapter : values()) {
            if (chapter.id == id) return chapter;
        }
        return CHAPTER_1;
    }

    /**
     * Obtient la liste des gates débloquées par ce chapitre
     */
    public List<JourneyGate> getUnlocksList() {
        return Arrays.asList(unlocks);
    }

    /**
     * Vérifie si ce chapitre débloque une gate spécifique
     */
    public boolean unlocks(JourneyGate gate) {
        for (JourneyGate g : unlocks) {
            if (g == gate) return true;
        }
        return false;
    }

    /**
     * Nombre total de chapitres
     */
    public static int totalChapters() {
        return values().length;
    }
}
