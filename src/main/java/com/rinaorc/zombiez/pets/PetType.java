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
        "Immunité aux effets négatifs (poison, wither, etc.)",
        "Bénédiction Divine",
        "Invincibilité 3s + full heal",
        120,
        "Immunité partagée aux alliés proches",
        "La bénédiction affecte aussi les alliés dans 10 blocs"
    ),

    PIGLIN_BERSERKER(
        "Piglin Berserker",
        PetRarity.LEGENDARY,
        Material.GOLDEN_AXE,
        EntityType.PIGLIN,
        "piglin_berserker",
        "Guerre / Intimidation",
        "Piglin guerrier avec armure dorée et hache massive",
        "1% chance de déclencher un saut dévastateur (180% dégâts AoE 8 blocs + Slow 60% 3s)",
        "Cri Féroce",
        "Réduit les dégâts des ennemis de 20% dans 25 blocs pendant 15s",
        45,
        "2% chance de saut, 200% dégâts",
        "Le cri applique aussi Weakness aux ennemis"
    ),

    SPECTRE_GIVRE(
        "Spectre du Givre",
        PetRarity.LEGENDARY,
        Material.IRON_SWORD,
        EntityType.STRAY,
        "frost_specter",
        "Vitesse / Assassinat",
        "Stray spectral avec cape sombre et dagues givrées",
        "+33% vitesse de déplacement constante",
        "Lame Fantôme",
        "Lance un couteau tournoyant qui empale l'ennemi (750% dégâts arme)",
        30,
        "+40% vitesse de déplacement",
        "Le couteau traverse et touche tous les ennemis sur son passage"
    ),

    CREAKING_VENGEUR(
        "Creaking Vengeur",
        PetRarity.LEGENDARY,
        Material.PALE_OAK_SAPLING,
        EntityType.CREAKING,
        "vengeful_creaking",
        "Racines / Contrôle de Zone",
        "Creaking spectral aux yeux rougeoyants et racines ondulantes",
        "8% chance par attaque de faire jaillir des racines (root 1.5s + 25% dégâts)",
        "Forêt Éveillée",
        "Explosion de racines (12 blocs) : root 4s + 100% dégâts, puis éruptions 6s",
        45,
        "12% chance, 30% dégâts racines",
        "Les racines appliquent aussi Wither II"
    ),

    SORCIERE_NECRO(
        "Sorcière Nécromancienne",
        PetRarity.LEGENDARY,
        Material.POISONOUS_POTATO,
        EntityType.WITCH,
        "necro_witch",
        "Drain de Vie / Nécromancie",
        "Witch aux yeux verts lumineux avec aura sombre",
        "Toutes les 5s, draine 5 ennemis (18 blocs) : +5% dégâts/ennemi, -5% HP mob",
        "Zombie Suicidaire",
        "Invoque un zombie qui explose en poison (560% dégâts arme) sur son chemin",
        35,
        "Drain toutes les 4s, +7% dégâts/ennemi",
        "Le zombie laisse une traînée de poison permanente"
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
        "Inflige +40% de dégâts aux ennemis ayant moins de 30% de vie",
        "Charge Dévastatrice",
        "Le Zoglin se précipite en avant, repoussant et infligeant 220% dégâts",
        10,
        "+50% dégâts sous 30% HP, seuil augmenté à 35%",
        "La charge laisse une traînée de feu infernal infligeant 50% dégâts/s"
    ),

    ILLUSIONISTE_ARCANIQUE(
        "Illusioniste Arcanique",
        PetRarity.LEGENDARY,
        Material.ENDER_EYE,
        EntityType.ILLUSIONER,
        "arcane_illusionist",
        "Arcane / Sniper",
        "Illusioniste aux yeux violets lumineux avec aura magique",
        "Inflige +30% de dégâts aux ennemis situés au-delà de 15 blocs",
        "Torrent Arcanique",
        "Volée de projectiles (150% dégâts, +50%/s, max 400%)",
        30,
        "+40% dégâts au-delà de 15 blocs, bonus dès 12 blocs",
        "Le torrent crée des explosions arcaniques secondaires (80% dégâts)"
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
        "Les ennemis sous 15% HP sont exécutés instantanément",
        "Jugement Final",
        "Frappe dévastatrice (200% dégâts + 100% des HP manquants)",
        90,
        "Seuil d'exécution 20%, chaque exécution +5% dégâts (max 30%, 10s)",
        "Jugement déclenche une onde de mort (150% dégâts AoE)"
    ),

    ENTITE_VIDE(
        "Sentinelle des Abysses",
        PetRarity.MYTHIC,
        Material.TRIDENT,
        EntityType.DROWNED,
        "abyss_sentinel",
        "Régénération / Tridents",
        "Drowned ancien aux yeux luminescents, trident spectral en main",
        "Régénère 3% HP/s (max 3 stacks). Reset 5s après avoir subi des dégâts",
        "Tempête de Tridents",
        "Lance une volée de tridents infligeant des dégâts basés sur votre arme",
        45,
        "4% regen/stack, 4 stacks max",
        "Tridents percent les ennemis et touchent ceux derrière"
    ),

    CHRONIQUEUR_TEMPOREL(
        "Caravanier du Désert",
        PetRarity.MYTHIC,
        Material.SANDSTONE,
        EntityType.CAMEL,
        "desert_caravan",
        "Blocage / Contre-attaque",
        "Chameau majestueux aux ornements dorés et tapis de soie",
        "Esquive désactivée, +30% blocage, blocages soignent 2% HP",
        "Charge du Caravanier",
        "Stocke les dégâts bloqués 6s, puis explosion AoE (200% stockés)",
        50,
        "+35% blocage, 3% HP par block",
        "L'explosion stun les ennemis 2s"
    ),

    HYDRE_PRIMORDIALE(
        "Marchand de Foudre",
        PetRarity.MYTHIC,
        Material.LIGHTNING_ROD,
        EntityType.WANDERING_TRADER,
        "lightning_merchant",
        "Électricité / Chaos",
        "Marchand mystérieux entouré d'arcs électriques crépitants",
        "3% chance de libérer 3 charges électriques (X% dégâts arme)",
        "Arc Voltaïque",
        "Lance un éclair qui rebondit entre 8 ennemis (80% dégâts)",
        40,
        "5% chance, 4 charges, +10% dégâts",
        "L'éclair peut toucher le même ennemi plusieurs fois"
    ),

    COLOSSUS_OUBLIE(
        "Marcheur de Braise",
        PetRarity.MYTHIC,
        Material.WARPED_FUNGUS_ON_A_STICK,
        EntityType.STRIDER,
        "ember_walker",
        "Feu / Désintégration",
        "Strider des profondeurs du Nether émettant une chaleur intense",
        "+6% crit/ennemi en feu (max 5 stacks, 32 blocs)",
        "Rayon de Désintégration",
        "Rayon brûlant (+X%/s, désintègre les kills)",
        40,
        "+8% crit/stack, portée 40 blocs",
        "Le rayon génère des explosions de cendres"
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
