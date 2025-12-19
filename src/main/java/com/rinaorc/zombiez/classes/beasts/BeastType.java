package com.rinaorc.zombiez.classes.beasts;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

/**
 * Types de bêtes invocables par la Voie des Bêtes du Chasseur.
 * Chaque bête possède une capacité unique et un comportement distinct.
 */
@Getter
public enum BeastType {

    /**
     * Tier 1 - Chauve-souris
     * Attaque la cible focus du joueur. Invincible.
     */
    BAT(
        1, "Chauve-souris",
        EntityType.BAT,
        Material.BAT_SPAWN_EGG,
        "§8",
        new String[]{
            "§8§lCHAUVE-SOURIS",
            "",
            "§7Une chauve-souris fidèle qui",
            "§7attaque vos ennemis focalisés.",
            "",
            "§6CAPACITÉ:",
            "§7Attaque automatiquement la cible",
            "§7que vous frappez.",
            "",
            "§a✦ INVINCIBLE"
        },
        Sound.ENTITY_BAT_AMBIENT,
        Sound.ENTITY_BAT_HURT,
        1.5, // Dégâts de base
        true, // Invincible
        0.0, // Offset angle (en degrés)
        2.0 // Distance du joueur
    ),

    /**
     * Tier 2 - Ours
     * Tank qui rugit pour aggro les monstres. Partage la vie du joueur.
     */
    BEAR(
        2, "Ours",
        EntityType.POLAR_BEAR,
        Material.POLAR_BEAR_SPAWN_EGG,
        "§f",
        new String[]{
            "§f§lOURS",
            "",
            "§7Un ours puissant qui protège",
            "§7sa meute avec férocité.",
            "",
            "§6CAPACITÉ - RUGISSEMENT:",
            "§7Toutes les §e8s§7, rugit pour",
            "§7provoquer les mobs dans §e5 blocs§7.",
            "",
            "§c♥ PARTAGE VOTRE VIE",
            "§7Respawn: §e10s§7 après la mort"
        },
        Sound.ENTITY_POLAR_BEAR_WARNING,
        Sound.ENTITY_POLAR_BEAR_HURT,
        4.0,
        false,
        45.0,
        3.0
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
            "§7pendant §e5s§7 (dégâts/seconde).",
            "",
            "§8Traque les ennemis blessés"
        },
        Sound.ENTITY_WOLF_GROWL,
        Sound.ENTITY_WOLF_HURT,
        3.0,
        false,
        90.0,
        2.5
    ),

    /**
     * Tier 4 - Axolotl
     * Attaque à distance avec des bulles d'eau.
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
            "§b~ Portée: §e8 blocs",
            "§b~ Cadence: §e1.5s"
        },
        Sound.ENTITY_AXOLOTL_SPLASH,
        Sound.ENTITY_AXOLOTL_HURT,
        2.5,
        false,
        135.0,
        2.5
    ),

    /**
     * Tier 5 - Vache
     * Lâche des mines explosives au sol.
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
            "§6CAPACITÉ - BOUSE EXPLOSIVE:",
            "§7Toutes les §e15s§7, dépose une",
            "§7mine qui explose au contact.",
            "",
            "§c✦ Dégâts de zone",
            "§c✦ Knockback"
        },
        Sound.ENTITY_COW_AMBIENT,
        Sound.ENTITY_COW_HURT,
        0.0, // Pas de dégâts directs
        false,
        180.0,
        3.0
    ),

    /**
     * Tier 6 - Lama
     * Crache sur plusieurs cibles avec effet de lenteur.
     */
    LLAMA(
        6, "Lama",
        EntityType.LLAMA,
        Material.LLAMA_SPAWN_EGG,
        "§e",
        new String[]{
            "§e§lLAMA",
            "",
            "§7Un lama hautain qui méprise",
            "§7tous vos ennemis.",
            "",
            "§6CAPACITÉ - CRACHAT ACIDE:",
            "§7Crache sur §e3 cibles§7 simultanément.",
            "§7Inflige des dégâts + §9Lenteur II§7.",
            "",
            "§b~ Portée: §e6 blocs",
            "§b~ Durée lenteur: §e3s"
        },
        Sound.ENTITY_LLAMA_SPIT,
        Sound.ENTITY_LLAMA_HURT,
        3.5,
        false,
        225.0,
        3.5
    ),

    /**
     * Tier 7 - Renard
     * Chasseur de trésors - bonus de stats sur kill proxy.
     */
    FOX(
        7, "Renard",
        EntityType.FOX,
        Material.FOX_SPAWN_EGG,
        "§6",
        new String[]{
            "§6§lRENARD",
            "",
            "§7Un renard rusé qui déniche",
            "§7des trésors cachés.",
            "",
            "§6CAPACITÉ - CHASSEUR DE TRÉSORS:",
            "§7Quand un mob meurt près de lui,",
            "§e20%§7 de chance de vous donner",
            "§7un bonus §cForce§7 ou §bVitesse§7!",
            "",
            "§a✦ Durée bonus: §e10s"
        },
        Sound.ENTITY_FOX_AMBIENT,
        Sound.ENTITY_FOX_HURT,
        2.0,
        false,
        270.0,
        2.0
    ),

    /**
     * Tier 8 - Abeille
     * Frénésie de la Ruche - active par double-sneak.
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
            "§7galvanise la meute.",
            "",
            "§6CAPACITÉ - FRÉNÉSIE DE LA RUCHE:",
            "§7§eDouble-Sneak§7 pour activer!",
            "§7+50%§7 vitesse d'attaque pour",
            "§7TOUTES les bêtes pendant §e10s§7.",
            "",
            "§c⚡ Cooldown: §e20s"
        },
        Sound.ENTITY_BEE_LOOP_AGGRESSIVE,
        Sound.ENTITY_BEE_HURT,
        1.0,
        false,
        315.0,
        1.5
    ),

    /**
     * Tier 9 - Golem de Fer
     * Onde de choc - attire puis projette les ennemis.
     */
    IRON_GOLEM(
        9, "Golem de Fer",
        EntityType.IRON_GOLEM,
        Material.IRON_BLOCK,
        "§7",
        new String[]{
            "§7§l★ GOLEM DE FER ★",
            "",
            "§7Un gardien de fer qui",
            "§7écrase vos ennemis.",
            "",
            "§6CAPACITÉ - ONDE DE CHOC:",
            "§7Toutes les §e12s§7, frappe le sol!",
            "",
            "§c1. §7Attire les ennemis (vortex)",
            "§c2. §7Les projette en l'air",
            "§c3. §7Dégâts massifs!",
            "",
            "§6§l★ TALENT LÉGENDAIRE ★"
        },
        Sound.ENTITY_IRON_GOLEM_ATTACK,
        Sound.ENTITY_IRON_GOLEM_HURT,
        8.0,
        false,
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
    private final double baseDamage;
    private final boolean invincible;
    private final double offsetAngle; // Position dans la formation (degrés)
    private final double distanceFromPlayer; // Distance du joueur

    BeastType(int tier, String displayName, EntityType entityType, Material icon,
              String color, String[] lore, Sound ambientSound, Sound hurtSound,
              double baseDamage, boolean invincible, double offsetAngle, double distanceFromPlayer) {
        this.tier = tier;
        this.displayName = displayName;
        this.entityType = entityType;
        this.icon = icon;
        this.color = color;
        this.lore = lore;
        this.ambientSound = ambientSound;
        this.hurtSound = hurtSound;
        this.baseDamage = baseDamage;
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
