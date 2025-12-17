package com.rinaorc.zombiez.pets;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Enum de tous les types de Pets disponibles
 * Chaque pet a une identité unique avec apparence et capacités
 */
@Getter
public enum PetType {

    // ==================== COMMUNS (§7) ====================

    CHAUVE_SOURIS_FANTOME(
        "Chauve-Souris Fantôme",
        PetRarity.COMMON,
        Material.BAT_SPAWN_EGG,
        EntityType.BAT,
        "bat_ghost",
        "Détection / Vision",
        "Petite chauve-souris spectrale grise",
        "Détecte les zombies dans un rayon de 15 blocs",
        "Écho-Scan",
        "Révèle tous les ennemis dans 30 blocs pendant 5s",
        30,
        "Détection des coffres cachés",
        "L'écho-scan marque les ennemis, +10% dégâts sur eux"
    ),

    RAT_CATACOMBES(
        "Rat des Catacombes",
        PetRarity.COMMON,
        Material.RABBIT_SPAWN_EGG,
        EntityType.RABBIT,
        "rat",
        "Loot / Économie",
        "Rat gris avec yeux brillants",
        "+5% de chance de loot supplémentaire",
        "Fouille",
        "Cherche des ressources au sol, trouve 1-3 items",
        60,
        "+10% Points des zombies",
        "Peut trouver des oeufs de pet (très rare)"
    ),

    LUCIOLE_ERRANTE(
        "Luciole Errante",
        PetRarity.COMMON,
        Material.GLOW_BERRIES,
        EntityType.ALLAY,
        "firefly",
        "Lumière / Support",
        "Luciole lumineuse",
        "Éclaire un rayon de 5 blocs autour du joueur",
        "Flash Aveuglant",
        "Aveugle les zombies proches 3s",
        25,
        "Rayon lumineux +5 blocs",
        "Le flash inflige des dégâts aux morts-vivants"
    ),

    SCARABEE_BLINDE(
        "Scarabée Blindé",
        PetRarity.COMMON,
        Material.ENDERMITE_SPAWN_EGG,
        EntityType.ENDERMITE,
        "beetle",
        "Défense",
        "Scarabée avec carapace métallique",
        "+5% de réduction de dégâts",
        "Carapace",
        "Bouclier absorbant 20 dégâts",
        45,
        "Réduction +3%",
        "Carapace reflète 30% des dégâts"
    ),

    CORBEAU_MESSAGER(
        "Corbeau Messager",
        PetRarity.COMMON,
        Material.PARROT_SPAWN_EGG,
        EntityType.PARROT,
        "raven",
        "Mobilité / Communication",
        "Petit corbeau noir",
        "+5% vitesse de déplacement",
        "Vol Éclaireur",
        "Le corbeau part en éclaireur et révèle une zone",
        40,
        "Vitesse +3%",
        "Peut transporter un petit objet vers un autre joueur"
    ),

    // ==================== PEU COMMUNS (§a) ====================

    LOUP_SPECTRAL(
        "Loup Spectral",
        PetRarity.UNCOMMON,
        Material.WOLF_SPAWN_EGG,
        EntityType.WOLF,
        "spectral_wolf",
        "Combat / Meute",
        "Loup fantomatique vert translucide",
        "Attaque les zombies proches (5 dégâts/2s)",
        "Hurlement",
        "Boost de 20% dégâts pendant 8s",
        35,
        "Attaque +3 dégâts",
        "Invoque 2 loups spectraux temporaires (10s)"
    ),

    CHAMPIGNON_AMBULANT(
        "Champignon Ambulant",
        PetRarity.UNCOMMON,
        Material.RED_MUSHROOM,
        EntityType.CHICKEN,
        "mushroom",
        "Soin / Régénération",
        "Petit champignon avec pattes",
        "Régénère 0.5 coeur/5s au joueur",
        "Spore Curative",
        "Soigne 6 coeurs instantanément",
        40,
        "Régénération +0.25 coeur/5s",
        "Spores laissent une zone de soin (3s)"
    ),

    GOLEM_POCHE(
        "Golem de Poche",
        PetRarity.UNCOMMON,
        Material.IRON_BLOCK,
        EntityType.ARMOR_STAND,
        "pocket_golem",
        "Tank / Protection",
        "Mini golem de pierre",
        "Intercepte 10% des dégâts subis par le joueur",
        "Mur de Pierre",
        "Crée un mur temporaire 3x2 (5s)",
        30,
        "Interception +5%",
        "Le mur repousse les zombies"
    ),

    FEU_FOLLET(
        "Feu Follet",
        PetRarity.UNCOMMON,
        Material.BLAZE_POWDER,
        EntityType.BLAZE,
        "wisp",
        "Dégâts / Brûlure",
        "Flamme verte flottante",
        "Les attaques du joueur ont 10% de chance d'enflammer",
        "Embrasement",
        "Enflamme tous les zombies dans 5 blocs",
        25,
        "Chance +5%",
        "Les ennemis en feu prennent +25% dégâts"
    ),

    ARAIGNEE_TISSEUSE(
        "Araignée Tisseuse",
        PetRarity.UNCOMMON,
        Material.SPIDER_SPAWN_EGG,
        EntityType.SPIDER,
        "weaver_spider",
        "Contrôle / Ralentissement",
        "Araignée cyan luminescente",
        "Les zombies touchés sont ralentis 1s",
        "Toile Géante",
        "Piège les zombies dans une zone 5x5 (4s)",
        30,
        "Ralentissement +0.5s",
        "La toile inflige des dégâts de poison"
    ),

    // ==================== RARES (§b) ====================

    PHENIX_MINEUR(
        "Phénix Mineur",
        PetRarity.RARE,
        Material.FIRE_CHARGE,
        EntityType.CHICKEN,
        "minor_phoenix",
        "Résurrection / Feu",
        "Petit oiseau de feu",
        "À la mort, renaissance avec 30% HP (CD: 5min)",
        "Nova de Feu",
        "Explosion de feu (15 dégâts, 5 blocs)",
        35,
        "Renaissance avec 40% HP",
        "La renaissance crée une explosion de feu"
    ),

    SERPENT_GIVRE(
        "Serpent de Givre",
        PetRarity.RARE,
        Material.PACKED_ICE,
        EntityType.SILVERFISH,
        "frost_serpent",
        "Glace / Contrôle",
        "Serpent de glace",
        "+15% dégâts de glace",
        "Souffle Glacial",
        "Gèle les ennemis devant (3s)",
        30,
        "Dégâts de glace +10%",
        "Les ennemis gelés explosent en mourant"
    ),

    HIBOU_ARCANIQUE(
        "Hibou Arcanique",
        PetRarity.RARE,
        Material.FEATHER,
        EntityType.PARROT,
        "arcane_owl",
        "Magie / Cooldowns",
        "Hibou avec runes brillantes",
        "-10% cooldown des capacités de classe",
        "Reset Arcanique",
        "Reset le cooldown d'une capacité",
        90,
        "Cooldown -5% supplémentaire",
        "Réduit aussi les cooldowns des items actifs"
    ),

    ESSAIM_SCARABEES(
        "Essaim de Scarabées",
        PetRarity.RARE,
        Material.GOLD_NUGGET,
        EntityType.BEE,
        "beetle_swarm",
        "DPS / Essaim",
        "Nuage de scarabées dorés",
        "Inflige 3 dégâts/s aux zombies proches (2 blocs)",
        "Nuée",
        "L'essaim attaque une cible (50 dégâts sur 5s)",
        25,
        "Dégâts passifs +2/s",
        "La nuée se propage aux ennemis proches"
    ),

    SPECTRE_GARDIEN(
        "Spectre Gardien",
        PetRarity.RARE,
        Material.CHAINMAIL_CHESTPLATE,
        EntityType.VEX,
        "guardian_specter",
        "Protection / Contre-attaque",
        "Fantôme en armure",
        "Pare automatiquement 1 attaque/30s",
        "Riposte Spectrale",
        "Prochaine attaque subie = contre-attaque x2",
        20,
        "Parade toutes les 25s",
        "La parade stun l'attaquant"
    ),

    // ==================== ÉPIQUES (§d) ====================

    DRAGON_PYGMEE(
        "Dragon Pygmée",
        PetRarity.EPIC,
        Material.DRAGON_HEAD,
        EntityType.VEX,
        "pygmy_dragon",
        "Puissance / Multi-éléments",
        "Mini dragon (taille d'un chat)",
        "+15% dégâts globaux",
        "Souffle Draconique",
        "Souffle de feu en cône (40 dégâts)",
        25,
        "Dégâts +10%",
        "Alterne entre feu/glace/foudre"
    ),

    FAMILIER_NECROMANTIQUE(
        "Familier Nécromantique",
        PetRarity.EPIC,
        Material.WITHER_SKELETON_SKULL,
        EntityType.VEX,
        "necro_familiar",
        "Nécromancie / Minions",
        "Crâne flottant avec aura violette",
        "Les zombies tués ont 10% de chance de devenir alliés (15s)",
        "Résurrection",
        "Ressuscite le dernier zombie tué comme allié (30s)",
        45,
        "Chance +5%, durée +5s",
        "Les alliés morts-vivants explosent en mourant"
    ),

    GOLEM_CRISTAL(
        "Golem de Cristal",
        PetRarity.EPIC,
        Material.AMETHYST_CLUSTER,
        EntityType.ARMOR_STAND,
        "crystal_golem",
        "Tank Ultime / Sacrifice",
        "Golem fait de cristaux violets",
        "+25% HP max au joueur",
        "Sacrifice Cristallin",
        "Absorbe 100% des dégâts pendant 5s, puis explose",
        60,
        "HP +10%",
        "L'explosion soigne le joueur"
    ),

    FELIN_OMBRE(
        "Félin de l'Ombre",
        PetRarity.EPIC,
        Material.CAT_SPAWN_EGG,
        EntityType.CAT,
        "shadow_cat",
        "Assassinat / Critique",
        "Chat noir avec yeux dorés, semi-transparent",
        "+20% dégâts critiques",
        "Embuscade",
        "Prochaine attaque = critique garanti x3",
        20,
        "Critique +10%",
        "L'embuscade rend invisible 3s avant l'attaque"
    ),

    ELEMENTAIRE_INSTABLE(
        "Élémentaire Instable",
        PetRarity.EPIC,
        Material.END_CRYSTAL,
        EntityType.VEX,
        "unstable_elemental",
        "Chaos / Aléatoire",
        "Sphère d'énergie multicolore changeante",
        "Effet aléatoire toutes les 30s (buff ou dégâts zone)",
        "Implosion Chaotique",
        "Effet puissant aléatoire",
        30,
        "Les effets positifs durent +50%",
        "Peut déclencher plusieurs effets à la fois"
    ),

    // ==================== LÉGENDAIRES (§6) ====================

    GARDIEN_ANGELIQUE(
        "Gardien Angélique",
        PetRarity.LEGENDARY,
        Material.TOTEM_OF_UNDYING,
        EntityType.ALLAY,
        "angelic_guardian",
        "Protection Divine / Immunité",
        "Mini ange en armure dorée",
        "Immunité aux effets négatifs (poison, wither, etc.)",
        "Bénédiction Divine",
        "Invincibilité 3s + full heal",
        120,
        "Immunité partagée aux alliés proches",
        "La bénédiction affecte aussi les alliés dans 10 blocs"
    ),

    WYRM_NEANT(
        "Wyrm du Néant",
        PetRarity.LEGENDARY,
        Material.ENDER_EYE,
        EntityType.SILVERFISH,
        "void_wyrm",
        "Espace / Téléportation",
        "Serpent cosmique avec étoiles dans le corps",
        "Téléportation courte (5 blocs) en prenant des dégâts (CD: 10s)",
        "Portail du Néant",
        "Crée un portail vers un point visible",
        30,
        "Téléportation +3 blocs, CD -3s",
        "Peut emmener les alliés dans le portail"
    ),

    TITAN_MINIATURE(
        "Titan Miniature",
        PetRarity.LEGENDARY,
        Material.IRON_GOLEM_SPAWN_EGG,
        EntityType.ARMOR_STAND,
        "mini_titan",
        "Force Brute / Écrasement",
        "Géant humanoïde miniature (1 bloc de haut)",
        "+30% dégâts de mêlée",
        "Coup Titanesque",
        "Frappe le sol (80 dégâts zone, knockback)",
        25,
        "Dégâts mêlée +15%",
        "Le coup laisse une fissure qui inflige des dégâts continus"
    ),

    ESPRIT_FORET(
        "Esprit de la Forêt",
        PetRarity.LEGENDARY,
        Material.OAK_SAPLING,
        EntityType.ALLAY,
        "forest_spirit",
        "Nature / Régénération Ultime",
        "Dryade miniature avec feuilles et fleurs",
        "Régénération de 1 coeur/3s, +50% efficacité des soins reçus",
        "Sanctuaire Naturel",
        "Zone de soin massive (5 coeurs/s pendant 10s)",
        60,
        "Régénération +0.5 coeur/3s",
        "Le sanctuaire fait aussi repousser les morts-vivants"
    ),

    PHENIX_ANCESTRAL(
        "Phénix Ancestral",
        PetRarity.LEGENDARY,
        Material.NETHER_STAR,
        EntityType.CHICKEN,
        "ancestral_phoenix",
        "Renaissance / Puissance de Feu",
        "Grand phénix doré majestueux",
        "Renaissance automatique une fois par vie (full HP)",
        "Apocalypse de Feu",
        "Pluie de feu (zone 10x10, 100 dégâts total)",
        45,
        "Renaissance donne 5s d'invincibilité",
        "La renaissance déclenche automatiquement l'apocalypse"
    ),

    // ==================== MYTHIQUES (§c) ====================

    AVATAR_MORT(
        "Avatar de la Mort",
        PetRarity.MYTHIC,
        Material.WITHER_SKELETON_SKULL,
        EntityType.VEX,
        "death_avatar",
        "Exécution / Mortalité",
        "Faucheuse miniature avec faux scintillante",
        "Les ennemis sous 15% HP sont exécutés instantanément",
        "Sentence Mortelle",
        "Marque une cible, elle meurt dans 5s (boss: -50% HP)",
        90,
        "Seuil d'exécution 20%",
        "L'exécution soigne le joueur de 20% HP max"
    ),

    ENTITE_VIDE(
        "Entité du Vide",
        PetRarity.MYTHIC,
        Material.OBSIDIAN,
        EntityType.VEX,
        "void_entity",
        "Annihilation / Néant",
        "Silhouette noire avec yeux blancs, distorsion visuelle",
        "5% des dégâts infligés ignorent toute résistance",
        "Dévoration",
        "Crée un trou noir aspirant les ennemis (5s)",
        60,
        "Dégâts purs +3%",
        "Le trou noir désintègre les ennemis à faible HP"
    ),

    CHRONIQUEUR_TEMPOREL(
        "Chroniqueur Temporel",
        PetRarity.MYTHIC,
        Material.CLOCK,
        EntityType.ALLAY,
        "time_chronicler",
        "Temps / Manipulation",
        "Horloge vivante avec engrenages dorés",
        "+25% vitesse d'attaque et de déplacement",
        "Arrêt du Temps",
        "Freeze tous les ennemis 4s",
        75,
        "Vitesse +10%",
        "Pendant l'arrêt, le joueur inflige x2 dégâts"
    ),

    HYDRE_PRIMORDIALE(
        "Hydre Primordiale",
        PetRarity.MYTHIC,
        Material.DRAGON_HEAD,
        EntityType.ENDER_DRAGON,
        "primordial_hydra",
        "Multi-attaque / Régénération",
        "Mini hydre à 3 têtes",
        "Chaque attaque frappe 3 fois (3 têtes)",
        "Souffle Tricolore",
        "3 souffles simultanés (feu/glace/poison)",
        35,
        "Si une tête est tuée, elle repousse en 2 (+dégâts)",
        "Peut faire repousser jusqu'à 5 têtes temporairement"
    ),

    COLOSSUS_OUBLIE(
        "Colossus Oublié",
        PetRarity.MYTHIC,
        Material.ANCIENT_DEBRIS,
        EntityType.ARMOR_STAND,
        "forgotten_colossus",
        "Puissance Ancienne / Destruction",
        "Fragment d'un ancien colosse de pierre avec runes brillantes",
        "+50% dégâts, mais -20% vitesse",
        "Éveil du Colosse",
        "Transformation géante (10s) - dégâts x3, immunité",
        120,
        "Malus de vitesse réduit à -10%",
        "L'éveil génère des ondes de choc continues"
    );

    private final String displayName;
    private final PetRarity rarity;
    private final Material icon;
    private final EntityType entityType;
    private final String modelId;
    private final String theme;
    private final String appearance;
    private final String passiveDescription;
    private final String activeName;
    private final String activeDescription;
    private final int activeCooldown;
    private final String level5Bonus;
    private final String starPowerDescription;

    PetType(String displayName, PetRarity rarity, Material icon, EntityType entityType,
            String modelId, String theme, String appearance,
            String passiveDescription, String activeName, String activeDescription,
            int activeCooldown, String level5Bonus, String starPowerDescription) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.icon = icon;
        this.entityType = entityType;
        this.modelId = modelId;
        this.theme = theme;
        this.appearance = appearance;
        this.passiveDescription = passiveDescription;
        this.activeName = activeName;
        this.activeDescription = activeDescription;
        this.activeCooldown = activeCooldown;
        this.level5Bonus = level5Bonus;
        this.starPowerDescription = starPowerDescription;
    }

    /**
     * Obtient le nom coloré du pet
     */
    public String getColoredName() {
        return rarity.getColor() + displayName;
    }

    /**
     * Obtient l'ID du pet (pour base de données)
     */
    public String getId() {
        return name().toLowerCase();
    }

    /**
     * Obtient un PetType depuis son ID
     */
    public static PetType fromId(String id) {
        if (id == null) return null;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Obtient tous les pets d'une rareté donnée
     */
    public static PetType[] getByRarity(PetRarity rarity) {
        return java.util.Arrays.stream(values())
            .filter(p -> p.rarity == rarity)
            .toArray(PetType[]::new);
    }

    /**
     * Tire un pet au sort selon les drop rates
     */
    public static PetType rollRandom(double luckBonus) {
        PetRarity rarity = PetRarity.roll(luckBonus);
        PetType[] petsOfRarity = getByRarity(rarity);
        if (petsOfRarity.length == 0) return CHAUVE_SOURIS_FANTOME;
        return petsOfRarity[(int) (Math.random() * petsOfRarity.length)];
    }
}
