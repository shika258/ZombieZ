package com.rinaorc.zombiez.progression.journey;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Les 12 Chapitres du Parcours du Survivant
 *
 * Chaque chapitre représente une étape majeure de la progression
 * avec ses propres objectifs, récompenses et déblocages
 *
 * IMPORTANT: Le joueur DOIT compléter chaque chapitre pour progresser
 * Les zones et fonctionnalités sont BLOQUÉES tant que le chapitre n'est pas terminé
 */
@Getter
public enum JourneyChapter {

    // ==================== PHASE 1: ÉVEIL (Niveau 1-15) ====================

    CHAPTER_1(
        1,
        "Premiers Pas",
        "§a",
        Material.WOODEN_SWORD,
        "Apprends les bases de la survie dans ce monde infesté",
        1, 5,  // Niveau requis: 1-5
        new JourneyGate[]{JourneyGate.CLASS_SELECTION, JourneyGate.ZONE_2}, // Débloque à la fin
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
        5, 10,
        new JourneyGate[]{}, // Zone 2 déjà débloquée au chapitre 1
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
        10, 15,
        new JourneyGate[]{JourneyGate.ZONE_3, JourneyGate.TALENTS_TIER_1},
        2500,
        45,
        "§eCoffre de Zone (Rare garanti)",
        "§aTalents Tier 1 Débloqués!"
    ),

    // ==================== PHASE 2: CROISSANCE (Niveau 15-35) ====================

    CHAPTER_4(
        4,
        "L'Art du Combat",
        "§e",
        Material.DIAMOND_SWORD,
        "Maîtrise tes premiers talents de classe",
        15, 25,
        new JourneyGate[]{JourneyGate.ZONE_4, JourneyGate.ZONE_5, JourneyGate.DAILY_MISSIONS},
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
        25, 30,
        new JourneyGate[]{JourneyGate.SKILL_TREE},
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
        30, 35,
        new JourneyGate[]{JourneyGate.ZONE_6, JourneyGate.BATTLE_PASS, JourneyGate.WEEKLY_MISSIONS},
        5000,
        100,
        "§dPremium Pass Trial (3 jours)",
        "§eBattle Pass Débloqué!"
    ),

    // ==================== PHASE 3: AFFIRMATION (Niveau 35-60) ====================

    CHAPTER_7(
        7,
        "Épreuve du Sang",
        "§c",
        Material.REDSTONE,
        "Survie aux événements spéciaux et aux Blood Moons",
        35, 45,
        new JourneyGate[]{JourneyGate.ZONE_7, JourneyGate.TALENTS_TIER_2},
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
        45, 55,
        new JourneyGate[]{JourneyGate.TALENTS_TIER_3},
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
        55, 60,
        new JourneyGate[]{JourneyGate.ZONE_8, JourneyGate.ZONE_9, JourneyGate.TALENTS_TIER_4},
        10000,
        175,
        "§5Set d'Équipement Classe (Épique)",
        "§cMaître de Classe!"
    ),

    // ==================== PHASE 4: MAÎTRISE (Niveau 60-100) ====================

    CHAPTER_10(
        10,
        "Terres Maudites",
        "§5",
        Material.NETHERITE_INGOT,
        "Explore les zones les plus dangereuses du monde",
        60, 75,
        new JourneyGate[]{JourneyGate.ZONE_10, JourneyGate.ZONE_11, JourneyGate.ZONE_15},
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
        75, 100,
        new JourneyGate[]{JourneyGate.ZONE_20, JourneyGate.ZONE_25, JourneyGate.ZONE_30,
                         JourneyGate.ZONE_40, JourneyGate.ZONE_50, JourneyGate.PRESTIGE},
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
        "Deviens une légende de ZombieZ - Le parcours ultime",
        100, 150, // Prestige 1 niveau 50 = ~150 en progression
        new JourneyGate[]{}, // Fin du parcours
        50000,
        500,
        "§6§l✦ Set Légendaire Exclusif + Aura + Titre ✦",
        "§6§l✦ LÉGENDE VIVANTE ✦"
    );

    private final int id;
    private final String name;
    private final String color;
    private final Material icon;
    private final String description;
    private final int minLevel;
    private final int maxLevel;
    private final JourneyGate[] unlocks;
    private final int bonusPoints;
    private final int bonusGems;
    private final String bonusReward;
    private final String completionMessage;

    JourneyChapter(int id, String name, String color, Material icon, String description,
                   int minLevel, int maxLevel, JourneyGate[] unlocks,
                   int bonusPoints, int bonusGems, String bonusReward, String completionMessage) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.description = description;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.unlocks = unlocks;
        this.bonusPoints = bonusPoints;
        this.bonusGems = bonusGems;
        this.bonusReward = bonusReward;
        this.completionMessage = completionMessage;
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
     * Obtient la phase du chapitre (1-4)
     */
    public int getPhase() {
        if (id <= 3) return 1;
        if (id <= 6) return 2;
        if (id <= 9) return 3;
        return 4;
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
