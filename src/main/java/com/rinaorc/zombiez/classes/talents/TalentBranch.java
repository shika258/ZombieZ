package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;

/**
 * Branches de spécialisation pour chaque classe
 * Chaque branche correspond à un slotIndex (0-4) dans les talents
 */
@Getter
public enum TalentBranch {

    // ==================== GUERRIER ====================
    GUERRIER_SEISME(
        ClassType.GUERRIER, 0,
        "Séisme",
        "§7",
        Material.COBBLESTONE,
        new String[]{
            "§7§lVOIE DU SÉISME",
            "",
            "§7Maîtrisez les ondes de choc et",
            "§7les dégâts de zone dévastateurs.",
            "",
            "§8Spécialité: §fAoE, Contrôle de zone",
            "§8Style: §fDégâts multi-cibles"
        }
    ),
    GUERRIER_SANG(
        ClassType.GUERRIER, 1,
        "Sang",
        "§c",
        Material.REDSTONE,
        new String[]{
            "§c§lVOIE DU SANG",
            "",
            "§7Drainez la vie de vos ennemis",
            "§7pour alimenter votre survie.",
            "",
            "§8Spécialité: §cVol de vie, Régénération",
            "§8Style: §cSurvie agressive"
        }
    ),
    GUERRIER_RAGE(
        ClassType.GUERRIER, 2,
        "Rage",
        "§6",
        Material.BLAZE_POWDER,
        new String[]{
            "§6§lVOIE DE LA RAGE",
            "",
            "§7Accumulez la fureur pour",
            "§7déchaîner des dégâts croissants.",
            "",
            "§8Spécialité: §6Dégâts cumulatifs",
            "§8Style: §6Berserker"
        }
    ),
    GUERRIER_FORTERESSE(
        ClassType.GUERRIER, 3,
        "Forteresse",
        "§8",
        Material.IRON_CHESTPLATE,
        new String[]{
            "§8§lVOIE DE LA FORTERESSE",
            "",
            "§7Devenez une muraille impénétrable",
            "§7absorbant les coups ennemis.",
            "",
            "§8Spécialité: §7Réduction de dégâts",
            "§8Style: §7Tank incassable"
        }
    ),
    GUERRIER_CHARGE(
        ClassType.GUERRIER, 4,
        "Charge",
        "§e",
        Material.LEATHER_BOOTS,
        new String[]{
            "§e§lVOIE DE LA CHARGE",
            "",
            "§7Foncez sur vos ennemis avec",
            "§7une puissance dévastatrice.",
            "",
            "§8Spécialité: §eMobilité, Étourdissement",
            "§8Style: §eAssaut éclair"
        }
    ),

    // ==================== CHASSEUR ====================
    CHASSEUR_BARRAGE(
        ClassType.CHASSEUR, 0,
        "Barrage",
        "§f",
        Material.ARROW,
        new String[]{
            "§f§lVOIE DU BARRAGE",
            "",
            "§7Submergez vos ennemis sous",
            "§7une pluie de projectiles.",
            "",
            "§8Spécialité: §fMulti-tirs, Volume",
            "§8Style: §fSuppression de zone"
        }
    ),
    CHASSEUR_BETES(
        ClassType.CHASSEUR, 1,
        "Bêtes",
        "§6",
        Material.WOLF_SPAWN_EGG,
        new String[]{
            "§6§lVOIE DES BÊTES",
            "",
            "§7Invoquez une meute de créatures",
            "§7fidèles pour combattre à vos côtés.",
            "",
            "§8Spécialité: §6Invocations, Meute",
            "§8Style: §6Maître des Bêtes"
        }
    ),
    CHASSEUR_OMBRE(
        ClassType.CHASSEUR, 2,
        "Ombre",
        "§b",
        Material.FEATHER,
        new String[]{
            "§b§lVOIE DE L'OMBRE",
            "",
            "§7Frappez depuis l'invisibilité",
            "§7et disparaissez sans trace.",
            "",
            "§8Spécialité: §bFurtivité, Esquive",
            "§8Style: §bAssassin agile"
        }
    ),
    CHASSEUR_POISON(
        ClassType.CHASSEUR, 3,
        "Poison",
        "§2",
        Material.SPIDER_EYE,
        new String[]{
            "§2§lVOIE DU POISON",
            "",
            "§7Infectez vos ennemis avec des",
            "§7toxines dévastatrices qui explosent!",
            "",
            "§8Spécialité: §2Stacks, Explosions, Nécrose",
            "§8Style: §2Maître de la Peste"
        }
    ),
    CHASSEUR_PERFORATION(
        ClassType.CHASSEUR, 4,
        "Givre",
        "§b",
        Material.BLUE_ICE,
        new String[]{
            "§b§lVOIE DU GIVRE",
            "",
            "§7Gelez vos ennemis avec des tirs",
            "§7glacials et faites-les éclater!",
            "",
            "§8Spécialité: §bGel, Éclat, Contrôle",
            "§8Style: §bCryomancien archer"
        }
    ),

    // ==================== OCCULTISTE ====================
    OCCULTISTE_FEU(
        ClassType.OCCULTISTE, 0,
        "Feu",
        "§c",
        Material.BLAZE_POWDER,
        new String[]{
            "§c§lVOIE DU FEU",
            "",
            "§7Consumez vos ennemis dans",
            "§7les flammes de la destruction.",
            "",
            "§8Spécialité: §cBrûlure, Propagation",
            "§8Style: §cPyromancien"
        }
    ),
    OCCULTISTE_GLACE(
        ClassType.OCCULTISTE, 1,
        "Glace",
        "§b",
        Material.BLUE_ICE,
        new String[]{
            "§b§lVOIE DE LA GLACE",
            "",
            "§7Gelez et ralentissez vos ennemis",
            "§7jusqu'à l'immobilisation totale.",
            "",
            "§8Spécialité: §bGel, Contrôle",
            "§8Style: §bCryomancien"
        }
    ),
    OCCULTISTE_FOUDRE(
        ClassType.OCCULTISTE, 2,
        "Foudre",
        "§e",
        Material.LIGHTNING_ROD,
        new String[]{
            "§e§lVOIE DE LA FOUDRE",
            "",
            "§7Déchaînez la puissance des",
            "§7éclairs en chaîne.",
            "",
            "§8Spécialité: §eChaînes, Multi-cibles",
            "§8Style: §eFulmimancien"
        }
    ),
    OCCULTISTE_AME(
        ClassType.OCCULTISTE, 3,
        "Âme",
        "§d",
        Material.SOUL_LANTERN,
        new String[]{
            "§d§lVOIE DE L'ÂME",
            "",
            "§7Récoltez les âmes des vaincus",
            "§7pour renforcer vos pouvoirs.",
            "",
            "§8Spécialité: §dOrbes d'âme, Invocations",
            "§8Style: §dNécromancien"
        }
    ),
    OCCULTISTE_VIDE(
        ClassType.OCCULTISTE, 4,
        "Vide",
        "§5",
        Material.ENDER_PEARL,
        new String[]{
            "§5§lVOIE DU VIDE",
            "",
            "§7Manipulez les forces obscures",
            "§7du néant absolu.",
            "",
            "§8Spécialité: §5Pénétration, Chaos",
            "§8Style: §5Maître du Vide"
        }
    );

    private final ClassType classType;
    private final int slotIndex;
    private final String displayName;
    private final String color;
    private final Material icon;
    private final String[] description;

    TalentBranch(ClassType classType, int slotIndex, String displayName, String color,
                 Material icon, String[] description) {
        this.classType = classType;
        this.slotIndex = slotIndex;
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
    }

    /**
     * Nom coloré de la branche
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtient toutes les branches d'une classe
     */
    public static TalentBranch[] getBranchesForClass(ClassType classType) {
        return java.util.Arrays.stream(values())
            .filter(b -> b.classType == classType)
            .toArray(TalentBranch[]::new);
    }

    /**
     * Obtient une branche par classe et slotIndex
     */
    public static TalentBranch getBranch(ClassType classType, int slotIndex) {
        for (TalentBranch branch : values()) {
            if (branch.classType == classType && branch.slotIndex == slotIndex) {
                return branch;
            }
        }
        return null;
    }

    /**
     * Obtient une branche depuis son ID
     */
    public static TalentBranch fromId(String id) {
        if (id == null) return null;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Obtient l'ID de la branche
     */
    public String getId() {
        return name().toLowerCase();
    }
}
