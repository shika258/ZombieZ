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
        "Critique / Vision",
        "Petite chauve-souris spectrale grise",
        "+1% de chance de critique",
        "Écho-Scan",
        "Révèle tous les ennemis dans 30 blocs pendant 5s",
        30,
        "+2% de chance de critique",
        "L'écho-scan marque les ennemis, +10% dégâts sur eux"
    ),

    RAT_CATACOMBES(
        "Rat des Catacombes",
        PetRarity.COMMON,
        Material.SILVERFISH_SPAWN_EGG,
        EntityType.SILVERFISH,
        "rat",
        "Loot / Économie",
        "Rat gris avec yeux brillants",
        "+5% de chance de loot supplémentaire",
        "Fouille",
        "Trouve 1 nourriture ou consommable (rareté selon niveau)",
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
        "Soin / Combat",
        "Luciole lumineuse aux reflets dorés",
        "Régénère 0.25❤/s pendant 5s après chaque kill",
        "Pulse de Vie",
        "Soigne 3❤ et donne Régénération I pendant 8s",
        30,
        "Régénération post-kill +0.25❤/s",
        "Pulse de Vie soigne aussi les alliés proches (5 blocs)"
    ),

    SCARABEE_BLINDE(
        "Scarabée Blindé",
        PetRarity.COMMON,
        Material.ENDERMITE_SPAWN_EGG,
        EntityType.ENDERMITE,
        "beetle",
        "Défense / Tank",
        "Scarabée avec carapace métallique",
        "+5% réduction de dégâts (cumule avec armure)",
        "Carapace Blindée",
        "Applique Résistance II pendant 5s (-40% dégâts)",
        45,
        "Réduction +3% (total 8%)",
        "Durée Résistance II augmentée à 8s"
    ),

    CORBEAU_MESSAGER(
        "Corbeau Messager",
        PetRarity.COMMON,
        Material.PARROT_SPAWN_EGG,
        EntityType.PARROT,
        "raven",
        "Mobilité / Attaque",
        "Petit corbeau noir",
        "+5% vitesse de déplacement",
        "Frappe du Corbeau",
        "Le corbeau se rue vers un ennemi et inflige 50% de vos dégâts",
        25,
        "Vitesse +3% (total 8%)",
        "La frappe inflige 75% de vos dégâts et ralentit l'ennemi"
    ),

    // --- NOUVEAUX PETS COMMUNS (Synergies) ---

    ARMADILLO_COMBO(
        "Armadillo Combo",
        PetRarity.COMMON,
        Material.ARMADILLO_SCUTE,
        EntityType.ARMADILLO,
        "combo_armadillo",
        "Combo / Momentum",
        "Armadillo doré qui se renforce avec chaque kill",
        "+0.5% dégâts par kill (max +5%, 15s) - Niv.Max: +15%, 30s",
        "Explosion de Combo",
        "Consomme le combo pour exploser en AoE (8 blocs, 5 dégâts/stack)",
        20,
        "Max +10% dégâts, durée 20s",
        "Max +15% dégâts, durée 30s, explosion +50% dégâts"
    ),

    LARVE_PARASITAIRE(
        "Larve Parasitaire",
        PetRarity.COMMON,
        Material.SLIME_BALL,
        EntityType.SLIME,
        "parasitic_larva",
        "Vampirisme / Survie",
        "Petite larve verte pulsante",
        "+3% de vol de vie sur les attaques",
        "Festin",
        "Le prochain kill restaure 25% HP max",
        35,
        "Vol de vie +2%",
        "Le festin crée aussi une explosion de soins pour les alliés"
    ),

    // ==================== PEU COMMUNS (§a) ====================

    LOUP_SPECTRAL(
        "Loup Spectral",
        PetRarity.UNCOMMON,
        Material.WOLF_SPAWN_EGG,
        EntityType.WOLF,
        "spectral_wolf",
        "Combat / Meute",
        "Loup fantomatique bleu translucide",
        "+3% dégâts par ennemi proche (max +15%, rayon 8 blocs)",
        "Frénésie Sanguinaire",
        "Pendant 8s, chaque kill donne +5% dégâts/vitesse (max +25%)",
        30,
        "Bonus par ennemi +2% (total 5%), max +20%",
        "Frénésie dure 12s, bonus par kill +7%, max +35%"
    ),

    CHAMPIGNON_AMBULANT(
        "Champignon Explosif",
        PetRarity.UNCOMMON,
        Material.RED_MUSHROOM,
        EntityType.MOOSHROOM,
        "mushroom",
        "Explosion / AoE",
        "Champignon rouge pulsant d'énergie volatile",
        "Les kills explosent en spores (20% dégâts AoE, 3 blocs)",
        "Détonation Fongique",
        "Charge 1.5s puis explose (150% dégâts, 6 blocs, knockback)",
        35,
        "Explosion +10% dégâts, rayon 4 blocs",
        "La détonation laisse des spores persistantes (3s de DoT)"
    ),

    GOLEM_POCHE(
        "Golem Sismique",
        PetRarity.UNCOMMON,
        Material.IRON_BLOCK,
        EntityType.IRON_GOLEM,
        "seismic_golem",
        "Contrôle / Stun",
        "Mini golem de fer aux pas lourds",
        "Chaque 5ème attaque crée une secousse (stun 1s, 3 blocs)",
        "Séisme",
        "Frappe le sol: stun 2s + 30 dégâts dans 8 blocs",
        30,
        "Secousse tous les 4 coups, stun +0.5s",
        "Le Séisme fissure le sol (zone de slow 5s)"
    ),

    FEU_FOLLET(
        "Feu Follet",
        PetRarity.UNCOMMON,
        Material.BLAZE_POWDER,
        EntityType.BLAZE,
        "wisp",
        "Dégâts / Projectiles",
        "Flamme verte flottante",
        "Chaque 5ème attaque tire une boule de feu (30% dégâts joueur)",
        "Barrage Infernal",
        "Tire 5 boules de feu en éventail (50% dégâts chacune)",
        20,
        "Boule de feu tous les 3 coups, +10% dégâts",
        "Les boules de feu explosent en AoE (3 blocs)"
    ),

    ARAIGNEE_TISSEUSE(
        "Araignée Chasseuse",
        PetRarity.UNCOMMON,
        Material.SPIDER_SPAWN_EGG,
        EntityType.SPIDER,
        "hunter_spider",
        "Prédateur / Embuscade",
        "Araignée noire aux yeux rouges luisants",
        "+25% dégâts sur ralentis/immobilisés, attaques ralentissent 1.5s",
        "Embuscade",
        "Bond sur l'ennemi, immobilise 3s, marque (+50% dégâts reçus 5s)",
        25,
        "Bonus dégâts +10% (total 35%), slow 2s",
        "L'embuscade rend invisible 2s avant le bond"
    ),

    // --- NOUVEAUX PETS PEU COMMUNS (Synergies Classes) ---

    ESPRIT_RAGE(
        "Esprit de Rage",
        PetRarity.UNCOMMON,
        Material.BLAZE_ROD,
        EntityType.VEX,
        "rage_spirit",
        "Guerrier / Stacking",
        "Flamme rouge pulsante avec visage colérique",
        "Chaque hit augmente les dégâts +2% (reset après 5s sans hit, max +30%)",
        "Déchaînement",
        "Double les stacks actuels pendant 5s",
        25,
        "Stack max +10%",
        "Synergie: +50% efficacité avec talents Guerrier (Fureur, Berserker)"
    ),

    FAUCON_CHASSEUR(
        "Spectre Traqueur",
        PetRarity.UNCOMMON,
        Material.PHANTOM_MEMBRANE,
        EntityType.PHANTOM,
        "stalker_specter",
        "Chasse / Exécution",
        "Phantom noir aux yeux rouges qui traque les proies affaiblies",
        "Marque auto les ennemis <50% HP, +20% dégâts sur marqués",
        "Plongeon Mortel",
        "Plonge sur l'ennemi le plus faible (200% dégâts, execute si <20% HP)",
        20,
        "Seuil de marque 60% HP, dégâts +10%",
        "L'exécution soigne 20% HP max"
    ),

    // --- NOUVEAUX PETS PEU COMMUNS (Visuels) ---

    ETOILE_FILANTE(
        "Grenouille Bondissante",
        PetRarity.UNCOMMON,
        Material.LILY_PAD,
        EntityType.FROG,
        "bouncing_frog",
        "Mobilité / Combos",
        "Grenouille agile aux pattes puissantes",
        "Chaque 4ème attaque = bond sur l'ennemi (+30% dégâts, stun 0.5s)",
        "Assaut Bondissant",
        "Enchaîne 5 bonds sur différents ennemis (50% dégâts, stun chacun)",
        20,
        "Bond toutes les 3 attaques, +10% dégâts",
        "Les bonds laissent des flaques ralentissantes"
    ),

    // ==================== RARES (§b) ====================

    PHENIX_MINEUR(
        "Phénix Mineur",
        PetRarity.RARE,
        Material.FIRE_CHARGE,
        EntityType.CHICKEN,
        "minor_phoenix",
        "Résurrection / Feu",
        "Petit oiseau de feu aux plumes flamboyantes",
        "À la mort, renaissance avec 30% HP (CD: 5min)",
        "Nova de Feu",
        "Explosion de feu (150% dégâts joueur, 5 blocs, enflamme)",
        35,
        "Renaissance avec 40% HP, +1 bloc rayon",
        "La renaissance déclenche automatiquement Nova de Feu"
    ),

    SERPENT_GIVRE(
        "Ours Polaire Gardien",
        PetRarity.RARE,
        Material.PACKED_ICE,
        EntityType.POLAR_BEAR,
        "polar_guardian",
        "Tank / Protection",
        "Imposant ours blanc aux yeux glacés, protecteur féroce",
        "-20% dégâts reçus, attaquants ralentis 1s + 5% dégâts retournés",
        "Rugissement Arctique",
        "Repousse les ennemis (8 blocs), gèle 2s, +30% armure 5s",
        30,
        "Réduction +5%, retour dégâts +3%",
        "Le rugissement inflige 50% dégâts joueur aux gelés"
    ),

    HIBOU_ARCANIQUE(
        "Calamar des Abysses",
        PetRarity.RARE,
        Material.GLOW_INK_SAC,
        EntityType.GLOW_SQUID,
        "abyss_squid",
        "Anti-Horde / Contrôle",
        "Calamar luminescent aux tentacules d'encre noire",
        "Toutes les 10 attaques, crée une flaque d'encre (slow 30%, dégâts/s)",
        "Nuage d'Obscurité",
        "Nuage d'encre géant : zombies confus s'attaquent entre eux 4s",
        25,
        "Flaque toutes les 8 attaques, dégâts +5%",
        "Le nuage inflige aussi 50% dégâts joueur aux zombies"
    ),

    ESSAIM_SCARABEES(
        "Essaim Furieux",
        PetRarity.RARE,
        Material.HONEYCOMB,
        EntityType.BEE,
        "fury_swarm",
        "Contre-attaque / Vengeance",
        "Trio d'abeilles miniatures aux dards venimeux",
        "Quand le joueur subit des dégâts, l'essaim contre-attaque (15% dégâts)",
        "Fureur de l'Essaim",
        "3 mini-abeilles attaquent tous les ennemis pendant 6s (10% dégâts/0.5s)",
        30,
        "Contre-attaque +5%, CD -0.5s",
        "La fureur applique aussi un poison (3% dégâts/s pendant 3s)"
    ),

    SPECTRE_GARDIEN(
        "Tentacule du Vide",
        PetRarity.RARE,
        Material.INK_SAC,
        EntityType.SQUID,
        "void_tentacle",
        "Proc / Invocation",
        "Poulpe ancestral aux tentacules imprégnés du vide",
        "15% chance d'invoquer des tentacules du vide (% dégâts joueur + slow)",
        "Éruption du Vide",
        "Invoque 5 tentacules géants qui ravagent la zone pendant 6s",
        35,
        "Chance +5%, tentacules +1",
        "Les tentacules drainent la vie des ennemis"
    ),

    // --- NOUVEAUX PETS RARES (Synergies Avancées) ---

    ORB_AMES(
        "Orbe d'Âmes",
        PetRarity.RARE,
        Material.SOUL_LANTERN,
        EntityType.VEX,
        "soul_orb",
        "Occultiste / Âmes",
        "Sphère d'énergie violette avec des âmes tourbillonnantes",
        "Les kills génèrent des Orbes d'Âme (+5% dégâts de skill par orbe, max 5)",
        "Libération d'Âmes",
        "Consomme toutes les orbes pour une explosion (15 dégâts par orbe)",
        15,
        "Max 7 orbes, dégâts skill +7% par orbe",
        "Synergie: Triple efficacité avec Soul Siphon et talents Occultiste"
    ),

    SALAMANDRE_ELEMENTAIRE(
        "Salamandre Élémentaire",
        PetRarity.RARE,
        Material.MAGMA_CREAM,
        EntityType.AXOLOTL,
        "elemental_salamander",
        "Multi-Éléments / Adaptation",
        "Salamandre qui change de couleur (rouge/bleu/jaune)",
        "Alterne entre Feu/Glace/Foudre toutes les 10s (+10% dégâts de l'élément actif)",
        "Fusion Élémentaire",
        "Attaque combinant les 3 éléments (brûle, gèle et électrocute)",
        30,
        "Bonus élément +5%",
        "Synergie: +100% efficacité avec talents élémentaires (Ignite, Frost, Lightning)"
    ),

    // --- NOUVEAUX PETS RARES (Visuels) ---

    SERPENT_FOUDROYANT(
        "Serpent Foudroyant",
        PetRarity.RARE,
        Material.LIGHTNING_ROD,
        EntityType.SILVERFISH,
        "lightning_serpent",
        "Foudre / Chaîne",
        "Serpent électrique crépitant d'énergie bleue",
        "20% chance de déclencher un éclair en chaîne (5 cibles, -20% dégâts par rebond)",
        "Tempête de Foudre",
        "Invoque 6 éclairs sur les ennemis proches",
        30,
        "Chaîne jusqu'à 7 cibles",
        "Les éclairs paralysent les cibles 1s"
    ),

    GOLEM_LAVE(
        "Golem de Lave",
        PetRarity.RARE,
        Material.MAGMA_BLOCK,
        EntityType.ARMOR_STAND,
        "lava_golem",
        "Lave / Traînée",
        "Golem de roche incandescente dégoulinant de lave",
        "Laisse une traînée de lave brûlant les ennemis (5 dégâts/s)",
        "Éruption Volcanique",
        "Crée une colonne de feu et projette 10 boules de lave",
        40,
        "Traînée inflige +3 dégâts/s",
        "L'éruption laisse des flaques de lave persistantes"
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

    // --- NOUVEAUX PETS ÉPIQUES (Synergies Uniques) ---

    SPECTRE_VENGEANCE(
        "Spectre de Vengeance",
        PetRarity.EPIC,
        Material.PHANTOM_MEMBRANE,
        EntityType.PHANTOM,
        "vengeance_specter",
        "Contre-attaque / Accumulation",
        "Spectre rouge sang avec chaînes brisées",
        "Accumule 50% des dégâts subis en Rage (max 200). Prochain hit = +Rage en dégâts",
        "Explosion de Vengeance",
        "Libère toute la Rage en dégâts de zone (rayon 8)",
        15,
        "Accumulation +25%",
        "La Rage ne decay plus, et les dégâts de Vengeance ignorent les résistances"
    ),

    DJINN_JACKPOT(
        "Djinn du Jackpot",
        PetRarity.EPIC,
        Material.EMERALD,
        EntityType.VEX,
        "jackpot_djinn",
        "Chance / Jackpot",
        "Génie doré avec pièces flottantes autour de lui",
        "+30% chance de déclencher le Jackpot, +50% récompenses Jackpot",
        "Super Jackpot",
        "Déclenche un Jackpot garanti avec récompenses x3",
        90,
        "Récompenses Jackpot +25%",
        "Les Jackpots peuvent drop des oeufs de pet (rare)"
    ),

    // --- NOUVEAUX PETS ÉPIQUES (Visuels) ---

    PHOENIX_SOLAIRE(
        "Phénix Solaire",
        PetRarity.EPIC,
        Material.FIRE_CHARGE,
        EntityType.CHICKEN,
        "solar_phoenix",
        "Feu / Météores",
        "Phénix doré irradiant de chaleur solaire",
        "25% chance de lancer une boule de feu (15 dégâts, explosion AoE)",
        "Pluie de Météores",
        "Fait pleuvoir 12 météores de feu sur la zone",
        35,
        "Boules de feu +5 dégâts",
        "Les météores laissent des zones de feu persistantes"
    ),

    OMBRE_DECHIRANTE(
        "Ombre Déchirante",
        PetRarity.EPIC,
        Material.BLACK_DYE,
        EntityType.VEX,
        "tearing_shadow",
        "Ombre / Tentacules",
        "Masse d'ombre pure avec des tentacules ondulants",
        "20% chance de faire surgir un tentacule d'ombre (+8 dégâts, aveugle)",
        "Vortex du Néant",
        "Crée un vortex aspirant les ennemis pendant 4s puis explose",
        50,
        "Tentacules infligent +5 dégâts",
        "Le vortex génère des tentacules supplémentaires"
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

    // --- NOUVEAUX PETS LÉGENDAIRES (Synergies de Classe) ---

    DRAGON_CHROMATIQUE(
        "Dragon Chromatique",
        PetRarity.LEGENDARY,
        Material.DRAGON_EGG,
        EntityType.ENDER_DRAGON,
        "chromatic_dragon",
        "Adaptation / Classe",
        "Dragon aux écailles changeantes selon la classe équipée",
        "S'adapte à la classe: Guerrier=+25% mêlée, Chasseur=+25% crit, Occultiste=+25% skill",
        "Souffle Chromatique",
        "Attaque adaptée: Guerrier=onde de choc, Chasseur=multi-projectiles, Occultiste=nova magique",
        30,
        "Bonus de classe +10%",
        "Synergie totale: Amplifie de 50% le talent ultime de la classe"
    ),

    SENTINELLE_ZONES(
        "Sentinelle des Zones",
        PetRarity.LEGENDARY,
        Material.LODESTONE,
        EntityType.SHULKER,
        "zone_sentinel",
        "Zones / Environnement",
        "Golem cristallin qui absorbe l'énergie de la zone",
        "Adapte les bonus selon la zone: +15% résist (toxique), +15% dégâts (feu), +15% speed (froid)",
        "Maîtrise de Zone",
        "Immunité totale aux effets de zone pendant 10s + bonus zone x2",
        60,
        "Bonus de zone +10%",
        "Dans les zones difficiles (40+): +30% XP et loot supplémentaire"
    ),

    // --- NOUVEAUX PETS LÉGENDAIRES (Visuels) ---

    HYDRE_GIVRE(
        "Hydre de Givre",
        PetRarity.LEGENDARY,
        Material.BLUE_ICE,
        EntityType.ENDER_DRAGON,
        "frost_hydra",
        "Glace / Blizzard",
        "Hydre à trois têtes faite de glace cristalline",
        "30% chance de lancer un éclat de glace (ralentit 2s, +10% dégâts)",
        "Blizzard",
        "Déchaîne un blizzard (8s, 10 dégâts/s, gèle les ennemis)",
        45,
        "Ralentissement +1s",
        "Le blizzard peut congeler complètement les ennemis (immobilise 3s)"
    ),

    ESPRIT_PRISMATIQUE(
        "Esprit Prismatique",
        PetRarity.LEGENDARY,
        Material.PRISMARINE_CRYSTALS,
        EntityType.ALLAY,
        "prismatic_spirit",
        "Lumière / Arc-en-ciel",
        "Esprit de lumière pure aux couleurs changeantes",
        "Tire un rayon prismatique toutes les 3s (+10 dégâts)",
        "Nova Prismatique",
        "Onde arc-en-ciel expansive (30 dégâts, rayon 10)",
        35,
        "Rayons +5 dégâts",
        "La nova applique un effet aléatoire (brûle/gèle/stun/poison)"
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
    ),

    // --- NOUVEAUX PETS MYTHIQUES (Synergies Ultimes) ---

    SYMBIOTE_ETERNEL(
        "Symbiote Éternel",
        PetRarity.MYTHIC,
        Material.HEART_OF_THE_SEA,
        EntityType.VEX,
        "eternal_symbiote",
        "Symbiose / Amplification Totale",
        "Créature liquide qui s'enroule autour du joueur",
        "Amplifie TOUS les bonus du joueur de 20% (classe, talents, items, autres pets équipés)",
        "Fusion Symbiotique",
        "Pendant 15s: tous les bonus x2, régénération +50%, immunité aux CC",
        90,
        "Amplification +10%",
        "Synergie ultime: La fusion active aussi tous les talents passifs simultanément"
    ),

    NEXUS_DIMENSIONNEL(
        "Nexus Dimensionnel",
        PetRarity.MYTHIC,
        Material.END_PORTAL_FRAME,
        EntityType.ENDERMAN,
        "dimensional_nexus",
        "Support / Équipe",
        "Portail miniature avec tentacules d'énergie",
        "Aura de 20 blocs: alliés gagnent +15% à tous les stats, ennemis -10% stats",
        "Convergence Dimensionnelle",
        "Téléporte tous les alliés vers vous + bouclier de groupe (absorbe 100 dégâts chacun)",
        75,
        "Aura +10 blocs, bonus alliés +5%",
        "L'aura génère aussi des particules de soin (1 coeur/5s pour tous les alliés)"
    ),

    // --- NOUVEAU PET MYTHIQUE (Visuel) ---

    KRAKEN_MINIATURE(
        "Kraken Miniature",
        PetRarity.MYTHIC,
        Material.INK_SAC,
        EntityType.SQUID,
        "mini_kraken",
        "Eau / Tentacules",
        "Petit kraken aux tentacules chatoyantes et yeux lumineux",
        "15% chance d'immobiliser un ennemi avec un tentacule d'eau (2s, +20% dégâts)",
        "Tsunami",
        "Déclenche une vague géante balayant les ennemis (50 dégâts, knockback massif)",
        50,
        "Immobilisation +1s",
        "Le tsunami noie les ennemis (suffocation 3s) et génère des tentacules"
    );

    private final String displayName;
    private final PetRarity rarity;
    private final Material icon;
    private final EntityType entityType;
    private final String modelId;
    private final String theme;
    private final String appearance;
    private final String passiveDescription;
    private final String ultimateName;
    private final String ultimateDescription;
    private final int ultimateCooldown; // Intervalle d'activation automatique en secondes
    private final String level5Bonus;
    private final String starPowerDescription;

    PetType(String displayName, PetRarity rarity, Material icon, EntityType entityType,
            String modelId, String theme, String appearance,
            String passiveDescription, String ultimateName, String ultimateDescription,
            int ultimateCooldown, String level5Bonus, String starPowerDescription) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.icon = icon;
        this.entityType = entityType;
        this.modelId = modelId;
        this.theme = theme;
        this.appearance = appearance;
        this.passiveDescription = passiveDescription;
        this.ultimateName = ultimateName;
        this.ultimateDescription = ultimateDescription;
        this.ultimateCooldown = ultimateCooldown;
        this.level5Bonus = level5Bonus;
        this.starPowerDescription = starPowerDescription;
    }

    /**
     * @deprecated Utiliser getUltimateName() à la place
     */
    @Deprecated
    public String getActiveName() {
        return ultimateName;
    }

    /**
     * @deprecated Utiliser getUltimateDescription() à la place
     */
    @Deprecated
    public String getActiveDescription() {
        return ultimateDescription;
    }

    /**
     * @deprecated Utiliser getUltimateCooldown() à la place
     */
    @Deprecated
    public int getActiveCooldown() {
        return ultimateCooldown;
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
