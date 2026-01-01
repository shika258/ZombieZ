package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registre des capacités de tous les Pets
 * Gère l'association entre PetType et leurs capacités
 * Les capacités ultimes s'activent automatiquement selon un intervalle
 */
public class PetAbilityRegistry {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<PetType, PetAbility> passiveAbilities = new EnumMap<>(PetType.class);

    @Getter
    private final Map<PetType, PetAbility> ultimateAbilities = new EnumMap<>(PetType.class);

    public PetAbilityRegistry(ZombieZPlugin plugin) {
        this.plugin = plugin;
        registerAllAbilities();
    }

    /**
     * @deprecated Utiliser getUltimateAbilities() à la place
     */
    @Deprecated
    public Map<PetType, PetAbility> getActiveAbilities() {
        return ultimateAbilities;
    }

    /**
     * Enregistre toutes les capacités de tous les pets
     */
    private void registerAllAbilities() {
        // ==================== COMMUNS ====================

        // Chauve-Souris Fantôme
        registerAbilities(PetType.CHAUVE_SOURIS_FANTOME,
            new CritChancePassive("bat_crit", "Vision Nocturne",
                "+1% de chance de critique", 0.01),
            new EchoScanActive("bat_echo", "Écho-Scan",
                "Révèle tous les ennemis dans 30 blocs pendant 5s", 30, 5)
        );

        // Rat des Catacombes
        registerAbilities(PetType.RAT_CATACOMBES,
            new LootBonusPassive("rat_loot", "Fouineur",
                "+5% de chance de loot supplémentaire", 0.05),
            new ScavengeActive("rat_scavenge", "Fouille",
                "Trouve 1 nourriture ou consommable (rareté selon niveau)")
        );

        // Luciole Errante - Guérisseuse de Combat
        registerAbilities(PetType.LUCIOLE_ERRANTE,
            new CombatRegenPassive("firefly_regen", "Aura Curative",
                "Régénère 0.25❤/s pendant 5s après chaque kill", 0.5, 5),
            new LifePulseActive("firefly_pulse", "Pulse de Vie",
                "Soigne 3❤ et donne Régénération I pendant 8s", 3.0, 8)
        );

        // Scarabée Blindé
        registerAbilities(PetType.SCARABEE_BLINDE,
            new DamageReductionPassive("beetle_armor", "Carapace Passive",
                "+5% réduction de dégâts (cumule avec armure)", 0.05),
            new ResistanceActive("beetle_resistance", "Carapace Blindée",
                "Applique Résistance II pendant 5s (-40% dégâts)", 5)
        );

        // Corbeau Messager
        registerAbilities(PetType.CORBEAU_MESSAGER,
            new SpeedPassive("raven_speed", "Ailes Rapides",
                "+5% vitesse de déplacement", 0.05),
            new RavenStrikeActive("raven_strike", "Frappe du Corbeau",
                "Le corbeau se rue vers un ennemi et inflige 50% de vos dégâts", 0.50)
        );

        // ==================== PEU COMMUNS ====================

        // Loup Spectral
        // Passif: +3% dégâts par ennemi proche (max +15%)
        // Ultimate: Frénésie 8s, chaque kill = +5% dégâts/vitesse (max +25%)
        registerAbilities(PetType.LOUP_SPECTRAL,
            new PackHunterPassive("wolf_pack", "Instinct de Meute",
                "+3% dégâts par ennemi proche (max +15%, rayon 8 blocs)", 0.03, 0.15, 8),
            new BloodFrenzyActive("wolf_frenzy", "Frénésie Sanguinaire",
                "Pendant 8s, chaque kill donne +5% dégâts/vitesse (max +25%)", 8, 0.05, 0.25)
        );

        // Champignon Explosif (anciennement Champignon Ambulant)
        // Passif: Les kills explosent en spores (20% dégâts, 4 blocs)
        // Ultimate: Charge 1.5s puis explose (150% dégâts, 6 blocs, knockback)
        registerAbilities(PetType.CHAMPIGNON_AMBULANT,
            new VolatileSporesPassive("shroom_volatile", "Spores Volatiles",
                "Les kills explosent en spores (20% dégâts AoE, 4 blocs)", 0.20, 4),
            new FungalDetonationActive("shroom_detonate", "Détonation Fongique",
                "Charge 1.5s puis explose (150% dégâts, 6 blocs, knockback)", 1.5, 6, 30)
        );

        // Golem Sismique (anciennement Golem de Poche)
        // Passif: Chaque 5ème attaque crée une secousse (stun 1s, 3 blocs)
        // Ultimate: Séisme - stun 2s + 30 dégâts dans 8 blocs
        registerAbilities(PetType.GOLEM_POCHE,
            new HeavyStepsPassive("golem_steps", "Pas Lourds",
                "Chaque 5ème attaque crée une secousse (stun 1s, 3 blocs)", 5, 20, 3),
            new SeismicSlamActive("golem_seism", "Séisme",
                "Frappe le sol: stun 2s + 30 dégâts dans 8 blocs", 30, 40, 8)
        );

        // Feu Follet - Boules de Feu
        // Passif: 5% chance par attaque de tirer une boule de feu (30% dégâts joueur)
        // Ultimate: Tire 5 boules de feu en éventail (50% dégâts chacune)
        registerAbilities(PetType.FEU_FOLLET,
            new WispFireballPassive("wisp_fireball", "Tir Enflammé",
                "5% chance par attaque de tirer une boule de feu (30% dégâts)", 0.05, 0.30),
            new InfernalBarrageActive("wisp_barrage", "Barrage Infernal",
                "Tire 5 boules de feu en éventail (50% dégâts chacune)", 5, 0.50)
        );

        // Araignée Chasseuse (anciennement Araignée Tisseuse)
        // Passif: +25% dégâts sur ralentis/immobilisés, attaques ralentissent 1.5s
        // Ultimate: Bond sur l'ennemi, immobilise 3s, marque (+50% dégâts 5s)
        PredatorPassive predatorPassive = new PredatorPassive("spider_predator", "Prédateur Patient",
            "+25% dégâts sur ralentis/immobilisés, attaques ralentissent 1.5s", 0.25, 30);
        registerAbilities(PetType.ARAIGNEE_TISSEUSE,
            predatorPassive,
            new SpiderAmbushActive("spider_ambush", "Embuscade",
                "Bond sur l'ennemi, immobilise 3s, marque (+50% dégâts 5s)", 60, 0.50, 100, predatorPassive)
        );

        // ==================== RARES ====================

        // Phénix Mineur
        // Passif: Renaissance avec 30% HP (CD: 5min)
        // Ultimate: Nova de Feu (150% dégâts joueur, 5 blocs, enflamme)
        registerAbilities(PetType.PHENIX_MINEUR,
            new RebornPassive("phoenix_reborn", "Renaissance",
                "À la mort, renaissance avec 30% HP (CD: 5min)", 0.30, 300),
            new FireNovaActive("phoenix_nova", "Nova de Feu",
                "Explosion de feu (150% dégâts joueur, 5 blocs, enflamme)", 35, 1.5, 5)
        );

        // Ours Polaire Gardien (anciennement Serpent de Givre)
        // Passif: -20% dégâts reçus, attaquants ralentis 1s + 5% dégâts retournés
        // Ultimate: Repousse les ennemis (8 blocs), gèle 2s, +30% armure 5s
        registerAbilities(PetType.SERPENT_GIVRE,
            new FrostFurPassive("polar_fur", "Fourrure Glaciale",
                "-20% dégâts reçus, attaquants ralentis 1s + 5% dégâts retournés", 0.20, 0.05, 20),
            new ArcticRoarActive("polar_roar", "Rugissement Arctique",
                "Repousse (8 blocs), gèle 2s, +30% armure 5s", 30, 8, 40, 0.30, 100)
        );

        // Calamar des Abysses (anciennement Hibou Arcanique)
        // Passif: Toutes les 10 attaques, crée une flaque d'encre (3s, slow 30%, 15% dégâts/s)
        // Ultimate: Nuage d'encre géant (8 blocs), zombies confus s'attaquent entre eux 4s
        registerAbilities(PetType.HIBOU_ARCANIQUE,
            new InkPuddlePassive("squid_ink", "Encre Toxique",
                "Toutes les 10 attaques, flaque d'encre (slow + dégâts/s)", 10, 0.30, 0.15, 60, 3.0),
            new DarknessCloudActive("squid_cloud", "Nuage d'Obscurité",
                "Nuage d'encre (8 blocs), zombies confus s'attaquent 4s", 25, 8.0, 80, 0.30)
        );

        // Essaim Furieux (anciennement Essaim de Scarabées)
        // Passif: Contre-attaque automatique quand le joueur subit des dégâts (15% dégâts, CD 2s)
        // Ultimate: 3 mini-abeilles (scale 0.4) attaquent tous les ennemis pendant 6s (10% dégâts/0.5s)
        registerAbilities(PetType.ESSAIM_SCARABEES,
            new SwarmRetaliationPassive("swarm_retaliate", "Représailles de la Ruche",
                "Contre-attaque auto quand touché (15% dégâts, CD 2s)", 0.15, 2000),
            new SwarmFuryActive("swarm_fury", "Fureur de l'Essaim",
                "3 mini-abeilles attaquent tous les ennemis 6s (10% dégâts/0.5s)", 30, 0.10, 120, 10, 8.0)
        );

        // Tentacule du Vide (Style Gur'thalak WoW)
        // Passif: 15% chance d'invoquer des tentacules du vide (% dégâts joueur + slow), CD 3s, max 4 actifs
        // Ultimate: Invoque 5 tentacules géants qui ravagent la zone pendant 6s
        registerAbilities(PetType.SPECTRE_GARDIEN,
            new VoidTentaclePassive("void_tentacle", "Appel du Vide",
                "15% chance d'invoquer des tentacules (20% dégâts, slow, max 4 actifs)", 0.15, 0.20, 60, 4.0, 2, 4),
            new VoidEruptionActive("void_eruption", "Éruption du Vide",
                "Invoque 5 tentacules géants (30% dégâts, slow 2, 6s)", 35, 5, 0.30, 120, 5.0, false)
        );

        // ==================== ÉPIQUES ====================

        // Gardien des Abysses (Guardian)
        registerAbilities(PetType.DRAGON_PYGMEE,
            new DamageMultiplierPassive("abyss_power", "Puissance Abyssale",
                "+15% dégâts globaux", 0.15),
            new AbyssLaserActive("abyss_laser", "Rayon des Abysses",
                "Laser chargeant (1.5s) puis frappe tous les ennemis (80% dégâts)", 30, 0.80, 8)
        );

        // Invocateur Maudit (Evoker)
        registerAbilities(PetType.FAMILIER_NECROMANTIQUE,
            new EvokerFangsPassive("evoker_fangs", "Mâchoires Spectrales",
                "20% chance d'invoquer des crocs d'Evoker (50% dégâts joueur)", 0.20, 0.50),
            new VexSwarmActive("vex_swarm", "Nuée de Vex",
                "Invoque 4 Vex alliés pendant 8s (15% dégâts chacun)", 35, 4, 8, 0.15)
        );

        // Nuage de Bonheur (Happy Ghast)
        registerAbilities(PetType.GOLEM_CRISTAL,
            new JoyfulTearsPassive("joyful_tears", "Larmes de Joie",
                "Chaque kill restore 5% HP au joueur", 0.05),
            new BenevolentRainActive("benevolent_rain", "Pluie Bienfaisante",
                "Zone (6 blocs, 5s) : 3% HP/s + Slow II ennemis", 40, 6, 5, 0.03, 1)
        );

        // Félin de l'Ombre
        registerAbilities(PetType.FELIN_OMBRE,
            new CritDamagePassive("cat_crit", "Griffes Assassines",
                "+20% dégâts critiques", 0.20),
            new PredatorMarkActive("cat_predator_mark", "Marque du Prédateur",
                "Marque un ennemi 8s : +50% dégâts, 100% crit", 25, 8, 0.50)
        );

        // Tempête Vivante (Breeze)
        registerAbilities(PetType.ELEMENTAIRE_INSTABLE,
            new WindGustPassive("breeze_gust", "Rafales Imprévisibles",
                "25% chance de rafale (knockback + 15% dégâts)", 0.25, 0.15, 1.2),
            new ChaoticVortexActive("breeze_vortex", "Vortex Chaotique",
                "Aspire puis expulse violemment (60 dégâts)", 35, 60, 8)
        );

        // ==================== LÉGENDAIRES ====================

        // Gardien Angélique
        registerAbilities(PetType.GARDIEN_ANGELIQUE,
            new DebuffImmunityPassive("angel_immunity", "Grâce Divine",
                "Immunité aux effets négatifs"),
            new DivineActive("angel_blessing", "Bénédiction Divine",
                "Invincibilité 3s + full heal", 120, 3)
        );

        // Piglin Berserker (Saut Dévastateur / Cri Féroce)
        WarLeapPassive warLeapPassive = new WarLeapPassive("war_leap", "Saut Dévastateur",
            "1% chance de saut AoE (180% dégâts, 8 blocs, Slow 60% 3s)", 0.01, 1.80, 8.0, 60);
        registerAbilities(PetType.PIGLIN_BERSERKER,
            warLeapPassive,
            new FerociousCryActive("ferocious_cry", "Cri Féroce",
                "Ennemis 25 blocs: -20% dégâts pendant 15s", warLeapPassive, 0.20, 25.0, 300)
        );

        // Spectre du Givre (Vitesse / Lame Fantôme)
        SpectralSwiftPassive spectralSwiftPassive = new SpectralSwiftPassive("spectral_swift", "Agilité Spectrale",
            "+33% vitesse de déplacement constante", 0.33);
        registerAbilities(PetType.SPECTRE_GIVRE,
            spectralSwiftPassive,
            new PhantomBladeActive("phantom_blade", "Lame Fantôme",
                "Couteau tournoyant (750% dégâts arme)", spectralSwiftPassive, 7.50, 1.5)
        );

        // Creaking Vengeur (Racines / Contrôle de Zone)
        RootGraspPassive rootGraspPassive = new RootGraspPassive("root_grasp", "Emprise Racinaire",
            "8% chance de root (1.5s + 25% dégâts)", 0.08, 0.25, 30);
        registerAbilities(PetType.CREAKING_VENGEUR,
            rootGraspPassive,
            new AwakenedForestActive("awakened_forest", "Forêt Éveillée",
                "Explosion 12 blocs (root 4s + 100% dégâts) + éruptions 6s", rootGraspPassive, 1.00, 12.0, 80, 120)
        );

        // Sorcière Nécromancienne (Drain de Vie / Zombie Suicidaire)
        LifeDrainPassive lifeDrainPassive = new LifeDrainPassive("life_drain", "Drain de Vie",
            "5s: draine 5 ennemis (18 blocs), +5% dégâts/ennemi, -5% HP mob", 0.05, 0.05, 18.0, 5);
        registerAbilities(PetType.SORCIERE_NECRO,
            lifeDrainPassive,
            new SuicidalZombieActive("suicidal_zombie", "Nécromancie",
                "Zombie suicidaire (560% dégâts poison sur son chemin)", lifeDrainPassive, 5.60, 4.0, 200)
        );

        // ==================== MYTHIQUES ====================

        // Avatar de la Mort (Vindicator - Exécuteur)
        // Passif: Exécute les ennemis sous 15% HP
        // Ultimate: Jugement Final - 200% dégâts + 100% bonus HP manquants
        // Max stars: Onde de mort (150% dégâts AoE, 6 blocs)
        registerAbilities(PetType.AVATAR_MORT,
            new ExecutionPassive("death_execute", "Faucheuse",
                "Exécute les ennemis sous 15% HP", 0.15),
            new FinalJudgmentActive("final_judgment", "Jugement Final",
                "Frappe dévastatrice (200% + bonus HP manquants)", 2.00, 1.00, 1.50, 6.0)
        );

        // Sentinelle des Abysses (anciennement Entité du Vide)
        // Passif: Regen stackable 3% HP/s (max 3 stacks), reset 5s après dégâts
        // Ultimate: Volée de tridents basée sur % de l'arme
        AbyssalRegenPassive abyssalRegenPassive = new AbyssalRegenPassive("abyssal_regen", "Régénération Abyssale",
            "3% HP/s par stack (max 3), reset 5s après dégâts", 0.03, 3, 5);
        registerAbilities(PetType.ENTITE_VIDE,
            abyssalRegenPassive,
            new TridentStormActive("trident_storm", "Tempête de Tridents",
                "Lance une volée de tridents (150% dégâts arme)", 1.50, 8, 15.0, abyssalRegenPassive)
        );

        // Caravanier du Désert (anciennement Chroniqueur Temporel)
        // Passif: Désactive esquive, +30% block, heal sur block
        // Ultimate: Stocke dégâts bloqués 6s, puis explosion AoE
        DesertEndurancePassive desertEndurancePassive = new DesertEndurancePassive("desert_endurance", "Endurance du Désert",
            "Esquive désactivée, +30% blocage, blocages soignent 2% HP", 0.30, 0.02);
        registerAbilities(PetType.CHRONIQUEUR_TEMPOREL,
            desertEndurancePassive,
            new CaravanChargeActive("caravan_charge", "Charge du Caravanier",
                "Stocke dégâts bloqués 6s, explosion 200%", 6, 2.0, 0.20, 8.0, desertEndurancePassive)
        );

        // Marchand de Foudre (anciennement Hydre Primordiale)
        // Passif: 3% chance de libérer 3 charges électriques (50% dégâts arme)
        // Ultimate: Arc Voltaïque - éclair rebondissant entre 8 ennemis
        UnstableMerchandisePassive unstableMerchandisePassive = new UnstableMerchandisePassive(
            "unstable_merchandise", "Marchandise Instable",
            "3% chance de libérer 3 charges électriques (50% dégâts arme)", 0.03, 3, 0.50, 12.0);
        registerAbilities(PetType.HYDRE_PRIMORDIALE,
            unstableMerchandisePassive,
            new VoltaicArcActive("voltaic_arc", "Arc Voltaïque",
                "Éclair rebondissant entre 8 ennemis (80% dégâts)", 8, 0.80, 0.50, 10.0, unstableMerchandisePassive)
        );

        // Marcheur de Braise (anciennement Colossus Oublié)
        // Passif: +6% crit par ennemi en feu (max 5 stacks = 30%, rayon 32 blocs)
        // Ultimate: Rayon de désintégration (100% → 400% dégâts, 8s)
        BurningCritPassive burningCritPassive = new BurningCritPassive(
            "burning_crit", "Braises Critiques",
            "+6% crit/ennemi en feu (max 5, 32 blocs)", 0.06, 5, 32.0, 3);
        registerAbilities(PetType.COLOSSUS_OUBLIE,
            burningCritPassive,
            new DisintegrationRayActive("disintegration_ray", "Rayon de Désintégration",
                "Rayon brûlant (100% → 400%/s, désintègre)", 1.00, 0.50, 4.00, 20.0, 8, burningCritPassive)
        );

        // ==================== NOUVEAUX PETS SYNERGIES ====================

        // Armadillo Combo (Combo/Momentum)
        // Base: +0.5% par kill, max +5%, reset après 15s
        // Niveau max: +15% max, reset après 30s
        ComboPassive comboPassive = new ComboPassive("combo_stack", "Momentum",
            "+0.5% dégâts par kill (max +5%, 15s) - Niv.Max: +15%, 30s", 0.005, 0.05, 15);
        registerAbilities(PetType.ARMADILLO_COMBO,
            comboPassive,
            new ComboExplosionActive("combo_explode", "Explosion de Combo",
                "Consomme le combo pour exploser en AoE (8 blocs, 5 dégâts/stack)", comboPassive, 5.0, 8)
        );

        // Larve Parasitaire (Lifesteal)
        LifestealPassive lifestealPassive = new LifestealPassive("leech_passive", "Parasitisme",
            "+3% de vol de vie sur les attaques", 3);
        FeastActive feastActive = new FeastActive("feast_active", "Festin",
            "Le prochain kill restaure 25% HP max", 25);
        registerAbilities(PetType.LARVE_PARASITAIRE, lifestealPassive, feastActive);

        // Esprit de Rage (Guerrier Synergy)
        RageStackPassive ragePassive = new RageStackPassive("rage_stack", "Furie Croissante",
            "+2% dégâts par hit (reset après 5s, max +30%)", 0.02, 0.30);
        registerAbilities(PetType.ESPRIT_RAGE,
            ragePassive,
            new UnleashActive("rage_unleash", "Déchaînement",
                "Double les stacks pendant 5s", ragePassive)
        );

        // Spectre Traqueur (anciennement Faucon Chasseur)
        // Passif: Marque auto les ennemis <50% HP, +20% dégâts sur marqués
        // Ultimate: Plonge sur l'ennemi le plus faible (200% dégâts, execute si <20% HP)
        PredatorInstinctPassive instinctPassive = new PredatorInstinctPassive("phantom_instinct", "Instinct du Prédateur",
            "Marque auto les ennemis <50% HP, +20% dégâts sur marqués", 0.50, 0.20);
        registerAbilities(PetType.FAUCON_CHASSEUR,
            instinctPassive,
            new DeadlyDiveActive("phantom_dive", "Plongeon Mortel",
                "Plonge sur l'ennemi le plus faible (200% dégâts, execute si <20% HP)", 2.0, 0.20, instinctPassive)
        );

        // Tortue Matriarche (Invocation / Essaim)
        // Passif: Tous les 8 kills, pond un œuf qui éclot en bébé tortue (5s, 10% dégâts)
        // Ultimate: Pond 4 œufs qui éclosent en tortues enragées (8s, 15% dégâts, slow)
        TurtleOffspringPassive turtlePassive = new TurtleOffspringPassive("turtle_offspring", "Progéniture Combative",
            "Tous les 8 kills, pond un œuf → bébé tortue (5s, 10% dégâts)", 8, 0.10, 100, 2);
        registerAbilities(PetType.ORB_AMES,
            turtlePassive,
            new WarNestActive("war_nest", "Nid de Guerre",
                "Pond 4 œufs → tortues enragées (8s, 15% dégâts, slow)", 30, 4, 0.15, 160, turtlePassive)
        );

        // Axolotl Prismatique (Réactions Élémentaires style Genshin)
        // Passif: Marque élémentaire (Feu/Glace/Foudre), combiner = RÉACTION
        // Ultimate: Applique les 3 éléments et déclenche toutes les réactions
        ElementalCatalystPassive catalystPassive = new ElementalCatalystPassive("elemental_catalyst", "Catalyseur Élémentaire",
            "Marque élémentaire, 2 éléments différents = RÉACTION (50% dégâts)", 0.50, 100, 3000);
        registerAbilities(PetType.SALAMANDRE_ELEMENTAIRE,
            catalystPassive,
            new ChainReactionActive("chain_reaction", "Réaction en Chaîne",
                "Applique les 3 éléments, déclenche toutes les réactions (100% dégâts)", 35, 1.0, catalystPassive)
        );

        // Panda Gourmand (Buffs & Food Drops)
        GourmandPassive gourmandPassive = new GourmandPassive("gourmand_feast", "Festin de Bambou",
            "5 kills = buff aléatoire 8s (Force/Vitesse/Régén/Crit)", 5, 160);
        registerAbilities(PetType.PANDA_GOURMAND,
            gourmandPassive,
            new SneezingExplosiveActive("sneeze_explode", "Éternuement Explosif",
                "Onde de choc 80% dégâts, tous buffs 5s", gourmandPassive, 0.80, 0.30)
        );

        // Chèvre Flipper (Ricochet System)
        FlipperPassive flipperPassive = new FlipperPassive("flipper_bounce", "Rebond Dévastateur",
            "Kill = projectile rebondissant (30% dégâts, +10%/rebond, max 5)", 0.30, 0.10, 5);
        registerAbilities(PetType.CHEVRE_FLIPPER,
            flipperPassive,
            new FlipperBallActive("flipper_ball", "Boule de Flipper",
                "Rebondit entre 8 ennemis (50% dégâts × n° rebond)", flipperPassive, 0.50, 8)
        );

        // Pillard Vengeur (Dégâts Arc/Arbalète / Volée de Flèches)
        RangedDamagePassive rangedDamagePassive = new RangedDamagePassive("ranged_damage", "Tir à Distance",
            "+30% dégâts arc/arbalète", 0.30);
        registerAbilities(PetType.PILLARD_VENGEUR,
            rangedDamagePassive,
            new ArrowVolleyActive("arrow_volley", "Volée de Flèches",
                "Pluie de flèches (360% dégâts à tous les ennemis)", rangedDamagePassive, 3.60, 10.0, 15)
        );

        // Cube Infernal (Dégâts Feu / Météore)
        FireDamagePassive fireDamagePassive = new FireDamagePassive("fire_damage", "Pyromanie",
            "+40% dégâts sur mobs en feu", 0.40);
        registerAbilities(PetType.CUBE_INFERNAL,
            fireDamagePassive,
            new MeteorStrikeActive("meteor_strike", "Frappe Météoritique",
                "Météore géant (450% dégâts) + zone enflammée (120% sur 3s)", fireDamagePassive, 4.50, 1.20, 8.0)
        );

        // Archonte Aquatique (anciennement Symbiote Éternel)
        // Passif: +5% dégâts subis par type élémentaire (max 4 stacks = 20%)
        // Ultimate: Forme d'Archonte - scale x1.5, +30% dégâts, +150% armure, +6% par kill
        ElementalSensitivityPassive elementalSensitivityPassive = new ElementalSensitivityPassive(
            "elemental_sensitivity", "Sensibilité Élémentaire",
            "+5% dégâts subis/type élémentaire (max 4, 5s)", 0.05, 4, 5);
        registerAbilities(PetType.SYMBIOTE_ETERNEL,
            elementalSensitivityPassive,
            new ArchonFormActive("archon_form", "Forme d'Archonte",
                "Transformation 20s: +30% dégâts, +150% armure, +6%/kill", 0.30, 1.50, 0.06, 20, elementalSensitivityPassive)
        );

        // Ancrage du Néant (anciennement Nexus Dimensionnel)
        // Passif: Zone 6 blocs avec slow, attraction et +15% dégâts
        // Ultimate: Singularité - trou noir 2s puis explosion 300%
        VoidGravityPassive voidGravityPassive = new VoidGravityPassive(
            "void_gravity", "Gravité du Vide",
            "Zone 6 blocs: -20% vitesse, attraction, +15% dégâts", 6.0, 0.20, 0.15, 0.15);
        registerAbilities(PetType.NEXUS_DIMENSIONNEL,
            voidGravityPassive,
            new SingularityActive("singularity", "Singularité",
                "Trou noir (2s) puis explosion 300% + dispersion", 12.0, 2.0, 3.00, 2.5, voidGravityPassive)
        );

        // ==================== NOUVEAUX PETS VISUELS ====================

        // Grenouille Bondissante (anciennement Étoile Filante)
        // Passif: Chaque 4ème attaque = bond sur l'ennemi (+30% dégâts, stun 0.5s)
        // Ultimate: Enchaîne 5 bonds sur différents ennemis (50% dégâts, stun chacun)
        registerAbilities(PetType.ETOILE_FILANTE,
            new FrogBouncePassive("frog_bounce", "Rebond",
                "Chaque 4ème attaque = bond (+30% dégâts, stun 0.5s)", 4, 0.30, 10),
            new BouncingAssaultActive("frog_assault", "Assaut Bondissant",
                "Enchaîne 5 bonds sur différents ennemis (50% dégâts, stun)", 5, 0.50, 15)
        );

        // Mouton Arc-en-Ciel (Buffs / Polyvalent)
        // Passif: Cycle les couleurs (3s), chaque couleur = bonus unique
        // Ultimate: Nova arc-en-ciel, applique tous les bonus pendant 6s
        ChromaticSpectrumPassive spectrumPassive = new ChromaticSpectrumPassive("chromatic_spectrum", "Spectre Chromatique",
            "Cycle couleurs (3s), chaque couleur = bonus unique", 3000, 0.15, 0.05);
        registerAbilities(PetType.SERPENT_FOUDROYANT,
            spectrumPassive,
            new PrismaticNovaActive("prismatic_nova", "Nova Prismatique",
                "Explosion arc-en-ciel, tous les bonus pendant 6s (50% dégâts)", 35, 0.50, 120, spectrumPassive)
        );

        // Renard des Neiges (Givre / Gel)
        FrostBitePassive frostBitePassive = new FrostBitePassive("frost_bite", "Morsure Glaciale",
            "5 attaques = gel 1.5s, +30% dégâts sur gelé", 5, 1.5, 0.30);
        registerAbilities(PetType.GOLEM_LAVE,
            frostBitePassive,
            new ArcticStormActive("arctic_storm", "Tempête Arctique",
                "Blizzard (8 blocs, 6s) : Slow III + 10% dégâts joueur/s", 35, 8, 6, 0.10, frostBitePassive)
        );

        // Poulet Bombardier (Œufs Explosifs)
        EggBomberPassive eggBomberPassive = new EggBomberPassive("egg_bomber", "Ponte Explosive",
            "3 attaques = œuf explosif (35% dégâts AoE)", 3, 0.35, 0.10);
        registerAbilities(PetType.POULET_BOMBARDIER,
            eggBomberPassive,
            new AirstrikeActive("airstrike", "Frappe Aérienne",
                "10 œufs explosifs depuis le ciel (50% dégâts chacun)", eggBomberPassive, 0.50, 10)
        );

        // Hurleur du Vide (Cris / Téléportation)
        VoidScreamPassive voidScreamPassive = new VoidScreamPassive("void_scream", "Cri du Vide",
            "4 attaques = cri (Slow II + 8%/s DoT, 3s)", 4, 0.08, 60, 6.0);
        registerAbilities(PetType.HURLEUR_DU_VIDE,
            voidScreamPassive,
            new PhantomStrikeActive("phantom_strike", "Frappe Fantôme",
                "5 TP enchaînés (40% dégâts) + échos explosifs (20% AoE)", voidScreamPassive, 0.40, 0.20, 5, 8.0)
        );

        // Zoglin Enragé (Exécution / Charge)
        ExecuteDamagePassive executeDamagePassive = new ExecuteDamagePassive("execute_damage", "Instinct de Tueur",
            "+40% dégâts sur ennemis <30% HP", 0.40, 0.30);
        registerAbilities(PetType.ZOGLIN_ENRAGE,
            executeDamagePassive,
            new ZoglinChargeActive("zoglin_charge", "Charge Dévastatrice",
                "Charge (220% dégâts, knockback)", executeDamagePassive, 2.20, 12.0, 2.5)
        );

        // Illusioniste Arcanique (Sniper / Arcane)
        // Passif: +30% dégâts au-delà de 15 blocs (Niv5+: +40%, dès 12 blocs)
        // Ultimate: Torrent de projectiles (150% → 400% dégâts, +50%/s)
        // Max stars: explosions secondaires (80% dégâts)
        SniperDamagePassive sniperDamagePassive = new SniperDamagePassive("sniper_damage", "Tir de Précision",
            "+30% dégâts aux ennemis au-delà de 15 blocs", 0.30, 15.0, 0.40, 12.0);
        registerAbilities(PetType.ILLUSIONISTE_ARCANIQUE,
            sniperDamagePassive,
            new ArcaneTorrentActive("arcane_torrent", "Torrent Arcanique",
                "Volée de projectiles (150% → 400% dégâts, +50%/s)", sniperDamagePassive, 1.50, 0.50, 4.00, 0.80)
        );

        // Gardien Lévitant (anciennement Kraken Miniature)
        // Passif: 4ème attaque = balle Shulker (40% dégâts + Lévitation 2s), +20% sur lévités
        // Ultimate: 8 balles en éventail puis slam au sol (150% dégâts chute + stun)
        ShulkerBulletPassive shulkerBulletPassive = new ShulkerBulletPassive(
            "shulker_bullet", "Balles de Shulker",
            "4ème attaque = balle (40% dégâts + Lévitation 2s), +20% sur lévités", 4, 0.40, 2, 0.20);
        registerAbilities(PetType.KRAKEN_MINIATURE,
            shulkerBulletPassive,
            new GravitationalBarrageActive("gravitational_barrage", "Barrage Gravitationnel",
                "8 balles en éventail, slam 150% + stun", 8, 0.60, 3, 1.50, 2.0, shulkerBulletPassive)
        );

        // ==================== EXALTÉS ====================

        // Sentinelle Sonique (WARDEN - EXALTED)
        // Passif composite: Détection Sismique (+25% dégâts sur attaquants marqués 8s)
        //                   Onde de Choc (6ème attaque = onde sonique 40% AoE, 8 blocs)
        // Ultimate: Boom Sonique Dévastatrice (charge 2s, 500% dégâts, stun 3s)
        // Max stars: Désintègre <20% HP + ondes secondaires
        SonicSentinelPassive sonicSentinelPassive = new SonicSentinelPassive(
            "sonic_sentinel", "Sentinelle Sonique",
            "Détection: marqués +25% dégâts. Onde: 6ème attaque = AoE 40%",
            0.25, 8, 6, 0.40, 8.0);
        registerAbilities(PetType.SENTINELLE_SONIQUE,
            sonicSentinelPassive,
            new SonicBoomActive("sonic_boom", "Boom Sonique Dévastatrice",
                "Charge 2s → 500% dégâts à tous + stun 3s", 2.0, 5.00, 3.0, 15.0, 0.20, sonicSentinelPassive)
        );
    }

    /**
     * Enregistre les capacités passive et ultime pour un pet
     */
    private void registerAbilities(PetType type, PetAbility passive, PetAbility ultimate) {
        if (passive != null) {
            passiveAbilities.put(type, passive);
        }
        if (ultimate != null) {
            ultimateAbilities.put(type, ultimate);
        }
    }

    /**
     * Obtient la capacité passive d'un pet
     */
    public PetAbility getPassive(PetType type) {
        return passiveAbilities.get(type);
    }

    /**
     * Obtient la capacité ultime d'un pet
     */
    public PetAbility getUltimate(PetType type) {
        return ultimateAbilities.get(type);
    }

    /**
     * @deprecated Utiliser getUltimate() à la place
     */
    @Deprecated
    public PetAbility getActive(PetType type) {
        return ultimateAbilities.get(type);
    }
}
