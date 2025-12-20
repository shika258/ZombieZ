package com.rinaorc.zombiez.classes.beasts;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

/**
 * Types de bêtes invocables par la Voie des Bêtes du Chasseur.
 * Chaque bête possède une capacité unique et un comportement distinct.
 * Les dégâts sont calculés en % des dégâts du joueur (comme les minions Occultiste).
 */
@Getter
public enum BeastType {

    /**
     * Tier 1 - Chauve-souris
     * Émet des ultrasons qui transpercent les ennemis en ligne.
     */
    BAT(
        1, "Chauve-souris",
        EntityType.BAT,
        Material.BAT_SPAWN_EGG,
        "§8",
        new String[]{
            "§8§lCHAUVE-SOURIS",
            "",
            "§7Une chauve-souris qui émet",
            "§7des ultrasons dévastateurs.",
            "",
            "§6CAPACITÉ - ULTRASON:",
            "§7Émet une onde sonore vers",
            "§7l'ennemi le plus proche.",
            "§cTransperce §7tous les ennemis",
            "§7sur sa trajectoire!",
            "",
            "§e⚔ Dégâts: §f25% §7de vos dégâts",
            "§b~ Portée: §e12 blocs",
            "§b~ Cadence: §e1.5s",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_BAT_AMBIENT,
        Sound.ENTITY_BAT_HURT,
        0.25, // 25% des dégâts du joueur
        true, // Invincible
        0.0, // Offset angle (en degrés)
        2.0 // Distance du joueur
    ),

    /**
     * Tier 2 - Endermite
     * Parasite du Vide - se téléporte sur les ennemis et les corrompt.
     * Cycle: Téléportation → Corruption → Explosion → Nouvelle cible
     */
    ENDERMITE(
        2, "Endermite",
        EntityType.ENDERMITE,
        Material.ENDER_PEARL,
        "§5",
        new String[]{
            "§5§lENDERMITE",
            "",
            "§7Un parasite du Vide qui",
            "§7infeste et corrompt ses proies.",
            "",
            "§6CAPACITÉ - INFESTATION DU VIDE:",
            "§7Se §5téléporte§7 sur un ennemi",
            "§7et s'y accroche pendant §e3s§7.",
            "",
            "§5✦ CORRUPTION DU VIDE:",
            "§7La cible subit §c+25%§7 de dégâts",
            "§7de §etoutes les sources§7!",
            "",
            "§c✦ EXPLOSION DU VIDE:",
            "§7Après 3s, explose en particules",
            "§7d'ender! Dégâts AoE + téléport",
            "§7vers une §enouvelle cible§7.",
            "",
            "§e⚔ DoT: §f15% §7/s pendant 3s",
            "§e⚔ Explosion: §f50% §7AoE",
            "§b~ Portée: §e32 blocs",
            "§a✦ INVINCIBLE",
            "",
            "§d§lSYNERGIE: §7+Marque Renard = §c+55%§7!"
        },
        Sound.ENTITY_ENDERMAN_TELEPORT,
        Sound.ENTITY_ENDERMITE_HURT,
        0.15, // 15% des dégâts du joueur (DoT)
        true, // Invincible
        45.0,
        2.0
    ),

    /**
     * Tier 3 - Loup
     * Applique un saignement (DoT) aux ennemis mordus.
     */
    WOLF(
        3, "Loup",
        EntityType.WOLF,
        Material.WOLF_SPAWN_EGG,
        "§7",
        new String[]{
            "§7§lLOUP",
            "",
            "§7Un loup sauvage aux crocs",
            "§7empoisonnés de venin.",
            "",
            "§6CAPACITÉ - SAIGNEMENT:",
            "§7Ses morsures infligent un §cDoT§7",
            "§7de §e15%§7 pendant §e5s§7.",
            "",
            "§e⚔ Dégâts: §f30% §7de vos dégâts",
            "§8Traque les ennemis blessés",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_WOLF_GROWL,
        Sound.ENTITY_WOLF_HURT,
        0.30, // 30% des dégâts du joueur
        true, // Invincible
        90.0,
        2.5
    ),

    /**
     * Tier 4 - Axolotl
     * Attaque à distance avec des bulles d'eau.
     * Vitesse d'attaque progressive: +10% par hit, max +150%
     */
    AXOLOTL(
        4, "Axolotl",
        EntityType.AXOLOTL,
        Material.AXOLOTL_BUCKET,
        "§d",
        new String[]{
            "§d§lAXOLOTL",
            "",
            "§7Un axolotl mystique qui",
            "§7maîtrise l'eau comme arme.",
            "",
            "§6CAPACITÉ - BULLES D'EAU:",
            "§7Tire des projectiles aquatiques",
            "§7sur les ennemis proches.",
            "",
            "§6FRÉNÉSIE AQUATIQUE:",
            "§7Chaque hit augmente sa vitesse",
            "§7d'attaque de §e+10%§7!",
            "§7Max: §c+150%§7 (cadence x2.5)",
            "",
            "§8► Décroît après 10s sans attaque",
            "",
            "§e⚔ Dégâts: §f25% §7de vos dégâts",
            "§b~ Portée: §e8 blocs",
            "§b~ Cadence: §e1.5s §7→ §e0.6s",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_AXOLOTL_SPLASH,
        Sound.ENTITY_AXOLOTL_HURT,
        0.25, // 25% des dégâts du joueur
        true, // Invincible
        135.0,
        2.5
    ),

    /**
     * Tier 5 - Vache
     * Lance des bouses explosives sur les groupes d'ennemis.
     */
    COW(
        5, "Vache",
        EntityType.COW,
        Material.COW_SPAWN_EGG,
        "§6",
        new String[]{
            "§6§lVACHE",
            "",
            "§7Une vache... explosive.",
            "§7Ne posez pas de questions.",
            "",
            "§6CAPACITÉ - BOUSE PROPULSÉE:",
            "§7Lance une bouse explosive vers",
            "§7les groupes d'ennemis! Explose",
            "§7à l'impact avec dégâts AoE.",
            "",
            "§e⚔ Dégâts: §f80% §7de vos dégâts",
            "§b~ Portée: §e12 blocs",
            "§b~ Cadence: §e5s",
            "§c✦ Knockback de zone",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_COW_AMBIENT,
        Sound.ENTITY_COW_HURT,
        0.80, // 80% des dégâts du joueur (mines)
        true, // Invincible
        180.0,
        3.0
    ),

    /**
     * Tier 6 - Lama
     * Crache de l'acide corrosif sur plusieurs cibles.
     * Applique Lenteur III + DoT Acide.
     */
    LLAMA(
        6, "Lama",
        EntityType.LLAMA,
        Material.LLAMA_SPAWN_EGG,
        "§e",
        new String[]{
            "§e§lLAMA",
            "",
            "§7Un lama hautain qui crache",
            "§7un acide corrosif dévastateur.",
            "",
            "§6CAPACITÉ - CRACHAT CORROSIF:",
            "§7Crache sur §e5 cibles§7 simultanément.",
            "",
            "§c✦ ACIDE CORROSIF:",
            "§7• §9Lenteur III§7 pendant §e5s",
            "§7• §2DoT Acide§7: §f15%§7/s pendant §e4s",
            "§7• Les cibles §cfondent§7 lentement!",
            "",
            "§e⚔ Dégâts impact: §f55% §7de vos dégâts",
            "§e⚔ DoT total: §f60% §7sur 4s",
            "§b~ Portée: §e16 blocs",
            "§b~ Cadence: §e2.5s",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_LLAMA_SPIT,
        Sound.ENTITY_LLAMA_HURT,
        0.55, // 55% des dégâts du joueur (impact)
        true, // Invincible
        225.0,
        3.5
    ),

    /**
     * Tier 7 - Renard
     * Prédateur agile qui bondit et marque ses proies.
     * Les cibles marquées sont en glowing.
     */
    FOX(
        7, "Renard",
        EntityType.FOX,
        Material.FOX_SPAWN_EGG,
        "§6",
        new String[]{
            "§6§lRENARD",
            "",
            "§7Un renard rusé qui traque",
            "§7et marque ses proies.",
            "",
            "§6CAPACITÉ - TRAQUE & BOND:",
            "§7Bondit sur les ennemis blessés",
            "§7et les §cmarque§7 pendant §e5s§7.",
            "",
            "§c✦ MARQUE:",
            "§7• §f+30% §7dégâts subis",
            "§7• §eGlowing §7- visible à travers les murs!",
            "",
            "§e⚔ Dégâts bond: §f20% §7de vos dégâts",
            "§b~ Portée: §e10 blocs",
            "§b~ Cadence: §e3s",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_FOX_AMBIENT,
        Sound.ENTITY_FOX_HURT,
        0.20, // 20% des dégâts du joueur
        true, // Invincible
        270.0,
        2.0
    ),

    /**
     * Tier 8 - Abeille
     * Essaim venimeux - attaque en groupe avec piqûres empilables.
     */
    BEE(
        8, "Abeille",
        EntityType.BEE,
        Material.BEE_SPAWN_EGG,
        "§e",
        new String[]{
            "§e§lABEILLE",
            "",
            "§7Une abeille guerrière qui",
            "§7déchaîne son essaim.",
            "",
            "§6CAPACITÉ - ESSAIM VENIMEUX:",
            "§7Lance des piqûres sur §e3 cibles§7.",
            "§7Chaque piqûre ajoute §c1 stack§7.",
            "",
            "§c✦ À 5 STACKS: §fEXPLOSION DE VENIN!",
            "§7Dégâts massifs + Poison II",
            "",
            "§e⚔ Dégâts/piqûre: §f10%",
            "§e⚔ Explosion: §f150%",
            "§b~ Cadence: §e2s",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_BEE_LOOP_AGGRESSIVE,
        Sound.ENTITY_BEE_HURT,
        0.10, // 10% des dégâts du joueur (support, pas DPS)
        true, // Invincible
        315.0,
        1.5
    ),

    /**
     * Tier 9 - Golem de Fer
     * Frappe Titanesque - charge + onde de choc linéaire avec synergies.
     */
    IRON_GOLEM(
        9, "Golem de Fer",
        EntityType.IRON_GOLEM,
        Material.IRON_BLOCK,
        "§7",
        new String[]{
            "§7§l★ GOLEM DE FER ★",
            "",
            "§7Un colosse de fer qui",
            "§7écrase tout sur son passage!",
            "",
            "§6CAPACITÉ - FRAPPE TITANESQUE:",
            "§7Toutes les §e5s§7, charge vers",
            "§7un ennemi et frappe le sol!",
            "",
            "§c1. §7Charge: écrase les ennemis",
            "§c2. §7Onde de choc §e8 blocs§7 devant",
            "§c3. §7§eStun 1.5s§7 sur tous les touchés!",
            "",
            "§6✦ SYNERGIE:",
            "§c• §7Cibles §cmarquées§7 = §ex2 dégâts",
            "§c• §73+ stacks abeille = §ex2 dégâts",
            "",
            "§e⚔ Dégâts: §f50% §7de vos dégâts",
            "§6§l★ TALENT LÉGENDAIRE ★",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_IRON_GOLEM_ATTACK,
        Sound.ENTITY_IRON_GOLEM_HURT,
        0.50, // 50% des dégâts du joueur (AoE compense)
        true, // Invincible
        0.0,
        4.0
    );

    private final int tier;
    private final String displayName;
    private final EntityType entityType;
    private final Material icon;
    private final String color;
    private final String[] lore;
    private final Sound ambientSound;
    private final Sound hurtSound;
    private final double damagePercent; // % des dégâts du joueur (0.25 = 25%)
    private final boolean invincible;
    private final double offsetAngle; // Position dans la formation (degrés)
    private final double distanceFromPlayer; // Distance du joueur

    BeastType(int tier, String displayName, EntityType entityType, Material icon,
              String color, String[] lore, Sound ambientSound, Sound hurtSound,
              double damagePercent, boolean invincible, double offsetAngle, double distanceFromPlayer) {
        this.tier = tier;
        this.displayName = displayName;
        this.entityType = entityType;
        this.icon = icon;
        this.color = color;
        this.lore = lore;
        this.ambientSound = ambientSound;
        this.hurtSound = hurtSound;
        this.damagePercent = damagePercent;
        this.invincible = invincible;
        this.offsetAngle = offsetAngle;
        this.distanceFromPlayer = distanceFromPlayer;
    }

    /**
     * Obtient le nom coloré de la bête
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Génère le nom d'affichage pour une bête appartenant à un joueur
     */
    public String getDisplayNameFor(String playerName) {
        return color + "Bête de " + playerName;
    }

    /**
     * Obtient un type de bête par son tier
     */
    public static BeastType getByTier(int tier) {
        for (BeastType type : values()) {
            if (type.tier == tier) {
                return type;
            }
        }
        return null;
    }
}
