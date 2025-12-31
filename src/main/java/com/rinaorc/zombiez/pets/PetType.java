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
        "+3% de chance de critique",
        "Écho-Scan",
        "Révèle les ennemis (30 blocs, 5s)",
        40,
        "+5% de chance de critique",
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
        "+5% de loot supplémentaire",
        "Fouille",
        "Trouve un consommable aléatoire",
        45,
        "+10% Points des zombies",
        "Peut trouver des œufs de pet (très rare)"
    ),

    LUCIOLE_ERRANTE(
        "Luciole Errante",
        PetRarity.COMMON,
        Material.GLOW_BERRIES,
        EntityType.ALLAY,
        "firefly",
        "Soin / Combat",
        "Luciole lumineuse aux reflets dorés",
        "Régénère 0.5❤/s (5s) après chaque kill",
        "Pulse de Vie",
        "Soigne 3❤ + Régén I (8s)",
        40,
        "Régénération +0.5❤/s",
        "Le pulse soigne aussi les alliés (5 blocs)"
    ),

    SCARABEE_BLINDE(
        "Scarabée Blindé",
        PetRarity.COMMON,
        Material.ENDERMITE_SPAWN_EGG,
        EntityType.ENDERMITE,
        "beetle",
        "Défense / Tank",
        "Scarabée avec carapace métallique",
        "-5% dégâts reçus",
        "Carapace Blindée",
        "Résistance II (5s) : -40% dégâts",
        40,
        "-8% dégâts reçus",
        "Résistance II dure 8s"
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
        "Attaque un ennemi (50% dégâts)",
        40,
        "+8% vitesse",
        "La frappe inflige 75% dégâts + slow"
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
        "+0.5% dégâts/kill (max 5%, reset 15s)",
        "Explosion de Combo",
        "Consomme le combo : AoE 8 blocs (5 dég/stack)",
        38,
        "Max 10%, reset 20s",
        "Max 15%, reset 30s, explosion +50%"
    ),

    LARVE_PARASITAIRE(
        "Larve Parasitaire",
        PetRarity.COMMON,
        Material.SLIME_BALL,
        EntityType.SLIME,
        "parasitic_larva",
        "Vampirisme / Survie",
        "Petite larve verte pulsante",
        "+3% vol de vie",
        "Festin",
        "Prochain kill : +25% HP max",
        42,
        "+5% vol de vie",
        "Le festin soigne aussi les alliés proches"
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
        "+3% dégâts/ennemi proche (max 15%, 8 blocs)",
        "Frénésie Sanguinaire",
        "8s: chaque kill +5% dégâts/vitesse (max 25%)",
        28,
        "+5%/ennemi, max 20%",
        "Frénésie 12s, +7%/kill, max 35%"
    ),

    CHAMPIGNON_AMBULANT(
        "Champignon Explosif",
        PetRarity.UNCOMMON,
        Material.RED_MUSHROOM,
        EntityType.MOOSHROOM,
        "mushroom",
        "Explosion / AoE",
        "Champignon rouge pulsant d'énergie volatile",
        "Les kills explosent (20% dégâts AoE, 3 blocs)",
        "Détonation Fongique",
        "Charge 1.5s → explosion 150% (6 blocs, knockback)",
        30,
        "30% dégâts, 4 blocs",
        "Spores persistantes (DoT 3s)"
    ),

    GOLEM_POCHE(
        "Golem Sismique",
        PetRarity.UNCOMMON,
        Material.IRON_BLOCK,
        EntityType.IRON_GOLEM,
        "seismic_golem",
        "Contrôle / Stun",
        "Mini golem de fer aux pas lourds",
        "5ème attaque : secousse (stun 1s, 3 blocs)",
        "Séisme",
        "Stun 2s + 30 dégâts (8 blocs)",
        28,
        "Secousse /4 coups, stun 1.5s",
        "Le Séisme crée zone de slow (5s)"
    ),

    FEU_FOLLET(
        "Feu Follet",
        PetRarity.UNCOMMON,
        Material.BLAZE_POWDER,
        EntityType.BLAZE,
        "wisp",
        "Dégâts / Projectiles",
        "Flamme verte flottante",
        "5ème attaque : boule de feu (30% dégâts)",
        "Barrage Infernal",
        "5 boules de feu en éventail (50% chacune)",
        26,
        "/3 coups, 40% dégâts",
        "Les boules explosent en AoE (3 blocs)"
    ),

    ARAIGNEE_TISSEUSE(
        "Araignée Chasseuse",
        PetRarity.UNCOMMON,
        Material.SPIDER_SPAWN_EGG,
        EntityType.SPIDER,
        "hunter_spider",
        "Prédateur / Embuscade",
        "Araignée noire aux yeux rouges luisants",
        "+25% dégâts sur ralentis, attaques slow 1.5s",
        "Embuscade",
        "Bond + immobilise 3s + marque (+50% dégâts 5s)",
        26,
        "+35% sur ralentis, slow 2s",
        "Invisibilité 2s avant le bond"
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
        "+2% dégâts/hit (max 30%, reset 5s)",
        "Déchaînement",
        "Double les stacks pendant 5s",
        26,
        "Max 40%",
        "Synergie Guerrier +50%"
    ),

    FAUCON_CHASSEUR(
        "Spectre Traqueur",
        PetRarity.UNCOMMON,
        Material.PHANTOM_MEMBRANE,
        EntityType.PHANTOM,
        "stalker_specter",
        "Chasse / Exécution",
        "Phantom noir aux yeux rouges qui traque les proies affaiblies",
        "Marque auto <50% HP, +20% dégâts sur marqués",
        "Plongeon Mortel",
        "Plonge sur le plus faible (200% dégâts, execute <20%)",
        26,
        "Seuil 60% HP, +30% dégâts",
        "L'exécution soigne 20% HP"
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
        "4ème attaque : bond (+30% dégâts, stun 0.5s)",
        "Assaut Bondissant",
        "5 bonds enchaînés (50% dégâts, stun chacun)",
        26,
        "/3 attaques, +40% dégâts",
        "Les bonds laissent flaques slow"
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
        "Tortue Matriarche",
        PetRarity.RARE,
        Material.TURTLE_EGG,
        EntityType.TURTLE,
        "turtle_matriarch",
        "Invocation / Essaim",
        "Ancienne tortue de guerre qui protège sa progéniture",
        "Tous les 8 kills, pond un œuf qui éclot en bébé tortue combative (5s)",
        "Nid de Guerre",
        "Pond 4 œufs qui éclosent en bébés tortues enragées (8s, slow)",
        30,
        "6 kills pour pondre, bébés +50% dégâts",
        "Les bébés tortues héritent des bonus du joueur"
    ),

    SALAMANDRE_ELEMENTAIRE(
        "Axolotl Prismatique",
        PetRarity.RARE,
        Material.AXOLOTL_BUCKET,
        EntityType.AXOLOTL,
        "prismatic_axolotl",
        "Réactions / Combos",
        "Axolotl qui change de couleur selon l'élément actif",
        "Marque élémentaire (Feu/Glace/Foudre), combiner 2 éléments = RÉACTION",
        "Réaction en Chaîne",
        "Applique les 3 éléments et déclenche toutes les réactions",
        35,
        "Réactions +30% dégâts, CD réactions -1s",
        "Vaporisation (Feu+Glace), Surcharge (Feu+Foudre), Supraconduction (Glace+Foudre)"
    ),

    // --- NOUVEAUX PETS RARES (Visuels) ---

    SERPENT_FOUDROYANT(
        "Mouton Arc-en-Ciel",
        PetRarity.RARE,
        Material.WHITE_WOOL,
        EntityType.SHEEP,
        "rainbow_sheep",
        "Buffs / Polyvalent",
        "Mouton mystique qui change de couleur comme jeb_",
        "Cycle les couleurs (3s), chaque couleur = bonus unique (+dmg/crit/lifesteal...)",
        "Nova Prismatique",
        "Explosion arc-en-ciel, applique tous les bonus pendant 6s",
        35,
        "Cycle plus rapide, bonus +50%",
        "Rouge=Dégâts, Orange=Vitesse, Jaune=Crit, Vert=Lifesteal, Bleu=Défense, Violet=Slow"
    ),

    GOLEM_LAVE(
        "Renard des Neiges",
        PetRarity.RARE,
        Material.POWDER_SNOW_BUCKET,
        EntityType.FOX,
        "snow_fox",
        "Contrôle / Gel",
        "Renard arctique aux pouvoirs de glace",
        "Les attaques appliquent des stacks de Givre (5 stacks = gel 1.5s, +30% dégâts)",
        "Tempête Arctique",
        "Blizzard (8 blocs, 6s) : Slow III + 10% dégâts/s",
        35,
        "Gel dure 2s, +40% dégâts pendant gel",
        "Le renard devient blanc pendant l'ultimate"
    ),

    // ==================== ÉPIQUES (§d) ====================

    DRAGON_PYGMEE(
        "Gardien des Abysses",
        PetRarity.EPIC,
        Material.PRISMARINE_SHARD,
        EntityType.GUARDIAN,
        "abyss_guardian",
        "Puissance / Laser",
        "Guardian miniature aux yeux lumineux",
        "+15% dégâts globaux",
        "Rayon des Abysses",
        "Laser balayant (8 blocs, marque puis explose)",
        30,
        "Dégâts +20%, laser traverse les ennemis",
        "L'œil du guardian brille pendant l'ultimate"
    ),

    FAMILIER_NECROMANTIQUE(
        "Invocateur Maudit",
        PetRarity.EPIC,
        Material.TOTEM_OF_UNDYING,
        EntityType.EVOKER,
        "cursed_evoker",
        "Invocation / Crocs",
        "Evoker miniature aux mains lumineuses",
        "20% chance d'invoquer des crocs d'Evoker sous l'ennemi frappé",
        "Nuée de Vex",
        "Invoque 4 Vex alliés qui attaquent pendant 8s",
        35,
        "Chance +10%, crocs infligent +50% dégâts",
        "L'Evoker lève les bras pendant l'invocation"
    ),

    GOLEM_CRISTAL(
        "Nuage de Bonheur",
        PetRarity.EPIC,
        Material.GHAST_TEAR,
        EntityType.GHAST,
        "happy_cloud",
        "Soin / Régénération",
        "Petit Ghast souriant flottant (scale 0.2)",
        "Chaque kill restore 5% HP au joueur",
        "Pluie Bienfaisante",
        "Zone (6 blocs, 5s) : regen joueur + slow ennemis",
        40,
        "Heal +3% par kill, zone +2 blocs",
        "Le Ghast pleure des larmes arc-en-ciel"
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
        "Marque du Prédateur",
        "Marque un ennemi 8s : +50% dégâts, 100% crit",
        25,
        "Critique +10%, marque +10% dégâts",
        "La marque dure 2s de plus, le chat laisse une traînée d'ombre"
    ),

    ELEMENTAIRE_INSTABLE(
        "Tempête Vivante",
        PetRarity.EPIC,
        Material.WIND_CHARGE,
        EntityType.BREEZE,
        "living_storm",
        "Vent / Contrôle / Propulsion",
        "Breeze miniature aux vents tourbillonnants",
        "25% chance de rafale de vent (knockback + 15% dégâts)",
        "Vortex Chaotique",
        "Aspire puis expulse violemment les ennemis (60 dégâts)",
        35,
        "Rafales +10% dégâts, knockback +30%",
        "Le vortex désoriente les ennemis 3s après l'éjection"
    ),

    // --- NOUVEAUX PETS ÉPIQUES (Synergies Uniques) ---

    PANDA_GOURMAND(
        "Panda Gourmand",
        PetRarity.EPIC,
        Material.BAMBOO,
        EntityType.PANDA,
        "gourmand_panda",
        "Buffs / Nourriture",
        "Panda dodu mâchant constamment du bambou avec des particules vertes",
        "Chaque 5 kills accorde un buff aléatoire 8s (+25% dégâts, +30% vitesse, régén ou +20% crit)",
        "Éternuement Explosif",
        "Onde de choc (rayon 6) infligeant 80% de vos dégâts. Applique tous les buffs 5s",
        20,
        "Buffs 12s, 4 kills requis",
        "L'éternuement a 30% de chance de drop un consommable ou nourriture"
    ),

    CHEVRE_FLIPPER(
        "Chèvre Flipper",
        PetRarity.EPIC,
        Material.SLIME_BALL,
        EntityType.GOAT,
        "flipper_goat",
        "Ricochets / Réactions en chaîne",
        "Chèvre bondissante avec traînée de particules rebondissantes",
        "Les ennemis tués sont projetés et rebondissent sur d'autres (30% dégâts, +10%/rebond, max 5)",
        "Boule de Flipper",
        "Rebondit entre 8 ennemis avec dégâts croissants (50% × n° rebond)",
        25,
        "7 rebonds max, +15%/rebond",
        "Les ennemis tués par ricochet explosent en dégâts de zone"
    ),

    // --- NOUVEAUX PETS ÉPIQUES (Visuels) ---

    POULET_BOMBARDIER(
        "Poulet Bombardier",
        PetRarity.EPIC,
        Material.EGG,
        EntityType.CHICKEN,
        "bomber_chicken",
        "Œufs Explosifs / Bombardement",
        "Poulet de combat avec casque militaire et ceinture d'œufs",
        "Toutes les 3 attaques, lance un œuf explosif (35% dégâts AoE, rayon 3)",
        "Frappe Aérienne",
        "Bombarde la zone de 10 œufs explosifs sur les ennemis (50% dégâts chacun)",
        20,
        "2 attaques pour un œuf, 40% dégâts",
        "10% chance d'œuf doré (dégâts x2 + stun 1s)"
    ),

    HURLEUR_DU_VIDE(
        "Hurleur du Vide",
        PetRarity.EPIC,
        Material.ENDER_EYE,
        EntityType.ENDERMAN,
        "void_screamer",
        "Cris du Vide / Téléportation",
        "Mini Enderman spectral aux yeux violets incandescents (scale 0.5)",
        "Toutes les 4 attaques, cri strident : Slow II + DoT 8%/s (3s) dans 6 blocs",
        "Frappe Fantôme",
        "Se TP sur 5 ennemis enchaînés, 40% dégâts + écho d'ombre explosif (20% AoE)",
        25,
        "3 attaques pour cri, 10% dégâts/s",
        "Blindness sur le cri, vortex attractif sur l'écho final"
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
        "Immunité aux effets négatifs",
        "Bénédiction Divine",
        "Invincibilité 3s + full heal",
        90,
        "Immunité partagée aux alliés",
        "Bénédiction affecte alliés (10 blocs)"
    ),

    PIGLIN_BERSERKER(
        "Piglin Berserker",
        PetRarity.LEGENDARY,
        Material.GOLDEN_AXE,
        EntityType.PIGLIN,
        "piglin_berserker",
        "Guerre / Intimidation",
        "Piglin guerrier avec armure dorée et hache massive",
        "3% chance saut dévastateur (180% AoE 8 blocs + Slow 3s)",
        "Cri Féroce",
        "Ennemis -20% dégâts (25 blocs, 15s)",
        40,
        "5% chance, 200% dégâts",
        "Le cri applique aussi Weakness"
    ),

    SPECTRE_GIVRE(
        "Spectre du Givre",
        PetRarity.LEGENDARY,
        Material.IRON_SWORD,
        EntityType.STRAY,
        "frost_specter",
        "Vitesse / Assassinat",
        "Stray spectral avec cape sombre et dagues givrées",
        "+33% vitesse de déplacement",
        "Lame Fantôme",
        "Couteau tournoyant (750% dégâts arme)",
        35,
        "+40% vitesse",
        "Le couteau traverse tous les ennemis"
    ),

    CREAKING_VENGEUR(
        "Creaking Vengeur",
        PetRarity.LEGENDARY,
        Material.PALE_OAK_SAPLING,
        EntityType.CREAKING,
        "vengeful_creaking",
        "Racines / Contrôle de Zone",
        "Creaking spectral aux yeux rougeoyants et racines ondulantes",
        "8% chance racines (root 1.5s + 25% dégâts)",
        "Forêt Éveillée",
        "Explosion racines (12 blocs): root 4s + 100% dégâts",
        40,
        "12% chance, 30% dégâts",
        "Racines appliquent Wither II"
    ),

    SORCIERE_NECRO(
        "Sorcière Nécromancienne",
        PetRarity.LEGENDARY,
        Material.POISONOUS_POTATO,
        EntityType.WITCH,
        "necro_witch",
        "Drain de Vie / Nécromancie",
        "Witch aux yeux verts lumineux avec aura sombre",
        "/5s: draine 5 ennemis (18 blocs), +5% dégâts/ennemi",
        "Zombie Suicidaire",
        "Zombie explosif (560% dégâts poison)",
        38,
        "/4s, +7% dégâts/ennemi",
        "Traînée de poison permanente"
    ),

    // --- NOUVEAUX PETS LÉGENDAIRES (Synergies de Classe) ---

    PILLARD_VENGEUR(
        "Pillard Vengeur",
        PetRarity.LEGENDARY,
        Material.CROSSBOW,
        EntityType.PILLAGER,
        "vengeful_pillager",
        "Tir à Distance / Volée de Flèches",
        "Pillager aux yeux rouges lumineux avec arbalète enchantée",
        "Augmente les dégâts à l'arc et à l'arbalète de +30%",
        "Volée de Flèches",
        "Tire une pluie de flèches massive infligeant 360% des dégâts de l'arme",
        35,
        "+40% dégâts arc/arbalète, attaques percent les armures",
        "La volée de flèches ricoche sur 3 ennemis supplémentaires"
    ),

    CUBE_INFERNAL(
        "Cube Infernal",
        PetRarity.LEGENDARY,
        Material.MAGMA_BLOCK,
        EntityType.MAGMA_CUBE,
        "infernal_cube",
        "Feu / Météore",
        "Cube de magma aux yeux incandescents avec particules de lave",
        "Augmente les dégâts infligés aux mobs en feu de +40%",
        "Frappe Météoritique",
        "Invoque un météore géant infligeant 450% dégâts feu + zone enflammée (120% sur 3s)",
        35,
        "+50% dégâts sur mobs en feu, enflamme automatiquement les cibles",
        "Le météore laisse un lac de lave permanent (10s) infligeant 50% dégâts/s"
    ),

    // --- NOUVEAUX PETS LÉGENDAIRES (Visuels) ---

    ZOGLIN_ENRAGE(
        "Zoglin Enragé",
        PetRarity.LEGENDARY,
        Material.CRIMSON_FUNGUS,
        EntityType.ZOGLIN,
        "enraged_zoglin",
        "Exécution / Charge",
        "Zoglin aux yeux rouges flamboyants avec aura de rage",
        "+40% dégâts sur ennemis <30% HP",
        "Charge Dévastatrice",
        "Charge + knockback (220% dégâts)",
        35,
        "+50% dégâts, seuil 35% HP",
        "Traînée de feu (50% dégâts/s)"
    ),

    ILLUSIONISTE_ARCANIQUE(
        "Illusioniste Arcanique",
        PetRarity.LEGENDARY,
        Material.ENDER_EYE,
        EntityType.ILLUSIONER,
        "arcane_illusionist",
        "Arcane / Sniper",
        "Illusioniste aux yeux violets lumineux avec aura magique",
        "+30% dégâts au-delà de 15 blocs",
        "Torrent Arcanique",
        "Volée de projectiles (150% → 400% dégâts)",
        36,
        "+40% dès 12 blocs",
        "Explosions secondaires (80% dégâts)"
    ),

    // ==================== MYTHIQUES (§c) ====================

    AVATAR_MORT(
        "Avatar de la Mort",
        PetRarity.MYTHIC,
        Material.IRON_AXE,
        EntityType.VINDICATOR,
        "death_avatar",
        "Exécution / Mortalité",
        "Bourreau sinistre avec hache ensanglantée et aura mortelle",
        "Exécute instantanément les ennemis <15% HP",
        "Jugement Final",
        "200% dégâts + 100% HP manquants de la cible",
        55,
        "Seuil 20%, +5% dégâts/exécution (max 30%)",
        "Onde de mort (150% AoE)"
    ),

    ENTITE_VIDE(
        "Sentinelle des Abysses",
        PetRarity.MYTHIC,
        Material.TRIDENT,
        EntityType.DROWNED,
        "abyss_sentinel",
        "Régénération / Tridents",
        "Drowned ancien aux yeux luminescents, trident spectral en main",
        "+3% HP/s par stack (max 3), reset 5s après dégâts",
        "Tempête de Tridents",
        "Volée de tridents (150% dégâts arme)",
        50,
        "+4%/stack, 4 stacks max",
        "Tridents percent les ennemis"
    ),

    CHRONIQUEUR_TEMPOREL(
        "Caravanier du Désert",
        PetRarity.MYTHIC,
        Material.SANDSTONE,
        EntityType.CAMEL,
        "desert_caravan",
        "Blocage / Contre-attaque",
        "Chameau majestueux aux ornements dorés et tapis de soie",
        "Esquive OFF, +30% block, block soigne 2% HP",
        "Charge du Caravanier",
        "Stocke dégâts bloqués 6s → explosion 200%",
        50,
        "+35% block, 3% HP/block",
        "Explosion stun 2s"
    ),

    HYDRE_PRIMORDIALE(
        "Marchand de Foudre",
        PetRarity.MYTHIC,
        Material.LIGHTNING_ROD,
        EntityType.WANDERING_TRADER,
        "lightning_merchant",
        "Électricité / Chaos",
        "Marchand mystérieux entouré d'arcs électriques crépitants",
        "5% chance : 3 charges électriques (50% dégâts arme)",
        "Arc Voltaïque",
        "Éclair rebondissant sur 8 ennemis (80% dégâts)",
        48,
        "7% chance, 4 charges",
        "L'éclair peut toucher plusieurs fois"
    ),

    COLOSSUS_OUBLIE(
        "Marcheur de Braise",
        PetRarity.MYTHIC,
        Material.WARPED_FUNGUS_ON_A_STICK,
        EntityType.STRIDER,
        "ember_walker",
        "Feu / Désintégration",
        "Strider des profondeurs du Nether émettant une chaleur intense",
        "+6% crit/ennemi en feu (max 5, 32 blocs)",
        "Rayon de Désintégration",
        "Rayon 100% → 400%/s, désintègre les kills",
        48,
        "+8% crit/stack, 40 blocs",
        "Explosions de cendres"
    ),

    // --- NOUVEAUX PETS MYTHIQUES (Synergies Ultimes) ---

    SYMBIOTE_ETERNEL(
        "Archonte Aquatique",
        PetRarity.MYTHIC,
        Material.HEART_OF_THE_SEA,
        EntityType.DOLPHIN,
        "aquatic_archon",
        "Élémentaire / Transformation",
        "Dauphin mystique irradiant une énergie arcanique prismatique",
        "+5% dégâts reçus/type élémentaire (max 4, 5s)",
        "Forme d'Archonte",
        "20s: +30% dégâts, +150% armure, +6%/kill",
        55,
        "+7%/stack, 5 stacks max",
        "Nova arcanique au déclenchement"
    ),

    NEXUS_DIMENSIONNEL(
        "Ancrage du Néant",
        PetRarity.MYTHIC,
        Material.END_PORTAL_FRAME,
        EntityType.ENDERMAN,
        "void_anchor",
        "Contrôle / Gravité",
        "Enderman entouré d'une aura gravitationnelle violette",
        "Zone 6 blocs: -20% vitesse, attraction, +15% dégâts",
        "Singularité",
        "Trou noir 2s → explosion 300%",
        55,
        "Zone 8 blocs, +20% dégâts",
        "Vulnérabilité 5s"
    ),

    // --- NOUVEAU PET MYTHIQUE (Visuel) ---

    KRAKEN_MINIATURE(
        "Gardien Lévitant",
        PetRarity.MYTHIC,
        Material.SHULKER_SHELL,
        EntityType.SHULKER,
        "levitating_guardian",
        "Lévitation / Contrôle Aérien",
        "Shulker mystique entouré d'orbes gravitationnels",
        "4ème attaque : balle (40% dégâts + Lévitation 2s), +20% sur lévités",
        "Barrage Gravitationnel",
        "8 balles en éventail + slam 150% dégâts",
        52,
        "/3 attaques, +25% sur lévités",
        "Double dégâts sur lévités"
    ),

    // ==================== EXALTÉS (§4§l) ====================

    SENTINELLE_SONIQUE(
        "Sentinelle Sonique",
        PetRarity.EXALTED,
        Material.SCULK_CATALYST,
        EntityType.WARDEN,
        "sonic_sentinel",
        "Détection / Destruction Sonique",
        "Warden ancestral irradiant une énergie sonique destructrice",
        "Marquage: attaquants +25% dégâts (8s). 6ème attaque: onde 40% AoE (8 blocs)",
        "Boom Sonique Dévastatrice",
        "Charge 2s → 500% dégâts + stun 3s (tous ennemis)",
        100,
        "+30% sur marqués (12s), onde /5 attaques (50%)",
        "Désintègre <20% HP + ondes secondaires"
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
