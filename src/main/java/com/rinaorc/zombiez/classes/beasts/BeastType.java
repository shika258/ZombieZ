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
            "§6► ULTRASON",
            "§7Onde sonore qui §ctransperce§7 les ennemis",
            "",
            "§e⚔ §f25%§7 dégâts  §b⚡ §f1.5s",
            "§b◎ §f32 blocs"
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
     */
    ENDERMITE(
        2, "Endermite",
        EntityType.ENDERMITE,
        Material.ENDER_PEARL,
        "§5",
        new String[]{
            "§5§lENDERMITE",
            "",
            "§6► INFESTATION DU VIDE",
            "§7Téléport sur ennemi → §5+25%§7 dégâts subis",
            "§7Après 3s: §cexplosion AoE§7 + nouvelle cible",
            "",
            "§e⚔ §f15%§7/s + §f50%§7 explosion",
            "§b◎ §f32 blocs"
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
            "§6► MORSURE VENIMEUSE",
            "§7Morsures + §csaignement§7 5s",
            "§8Traque les ennemis blessés",
            "",
            "§e⚔ §f30%§7 + §f15%§7/s DoT"
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
            "§6► BULLES D'EAU",
            "§7Projectiles aquatiques à distance",
            "",
            "§d► FRÉNÉSIE: §f+10%§7 vitesse/hit",
            "§7Max §c+150%§7 (cadence §f1.5s§7→§f0.6s§7)",
            "",
            "§e⚔ §f25%§7 dégâts  §b◎ §f32 blocs"
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
            "§6► BOUSE EXPLOSIVE",
            "§7Projectile en arc → §cexplosion AoE§7",
            "§7Cible les groupes + §eknockback",
            "",
            "§e⚔ §f80%§7 dégâts  §b⚡ §f5s",
            "§b◎ §f32 blocs"
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
            "§6► CRACHAT CORROSIF",
            "§7Crache sur §f5 cibles§7 simultanément",
            "§9Lenteur III §75s + §2DoT Acide §74s",
            "",
            "§e⚔ §f55%§7 + §f60%§7 DoT  §b⚡ §f2.5s",
            "§b◎ §f32 blocs"
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
            "§6► TRAQUE & BOND",
            "§7Bondit sur blessés → §cmarque§7 5s",
            "§c+30%§7 dégâts subis + §eGlowing",
            "",
            "§e⚔ §f20%§7 dégâts  §b⚡ §f3s",
            "§b◎ §f10 blocs"
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
            "§6► ESSAIM VENIMEUX",
            "§7Piqûres sur §f3 cibles§7 → §c+1 stack",
            "§c5 stacks§7: §fExplosion §7+ Poison II",
            "",
            "§e⚔ §f10%§7/piqûre  §c⚔ §f150%§7 explosion",
            "§b⚡ §f2s"
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
            "§6§l★ GOLEM DE FER ★",
            "",
            "§6► FRAPPE TITANESQUE",
            "§7Charge + onde de choc §f8 blocs",
            "§eStun 1.5s§7 sur tous les touchés",
            "",
            "§d► SYNERGIE: §7Marque/Abeille = §cx2",
            "",
            "§e⚔ §f50%§7 dégâts  §b⚡ §f5s"
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
