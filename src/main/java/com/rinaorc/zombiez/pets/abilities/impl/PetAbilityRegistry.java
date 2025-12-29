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
                "+5% de réduction de dégâts", 0.05),
            new ShieldActive("beetle_shield", "Carapace",
                "Bouclier absorbant 20 dégâts", 45, 20)
        );

        // Corbeau Messager
        registerAbilities(PetType.CORBEAU_MESSAGER,
            new SpeedPassive("raven_speed", "Ailes Rapides",
                "+5% vitesse de déplacement", 0.05),
            new ScoutActive("raven_scout", "Vol Éclaireur",
                "Révèle une zone éloignée", 40)
        );

        // ==================== PEU COMMUNS ====================

        // Loup Spectral
        registerAbilities(PetType.LOUP_SPECTRAL,
            new AttackPassive("wolf_attack", "Morsure Spectrale",
                "Attaque les zombies proches (5 dégâts/2s)", 5, 40),
            new HowlActive("wolf_howl", "Hurlement",
                "Boost de 20% dégâts pendant 8s", 35, 0.20, 8)
        );

        // Champignon Ambulant
        registerAbilities(PetType.CHAMPIGNON_AMBULANT,
            new RegenPassive("shroom_regen", "Spores Curatives",
                "Régénère 0.5 coeur/5s", 0.5, 100),
            new HealActive("shroom_heal", "Spore Curative",
                "Soigne 6 coeurs instantanément", 40, 6)
        );

        // Golem de Poche
        registerAbilities(PetType.GOLEM_POCHE,
            new InterceptPassive("golem_intercept", "Protection",
                "Intercepte 10% des dégâts", 0.10),
            new WallActive("golem_wall", "Mur de Pierre",
                "Crée un mur temporaire 3x2", 30, 5)
        );

        // Feu Follet
        registerAbilities(PetType.FEU_FOLLET,
            new IgnitePassive("wisp_ignite", "Toucher Enflammé",
                "10% de chance d'enflammer", 0.10),
            new IgniteAreaActive("wisp_burst", "Embrasement",
                "Enflamme tous les zombies dans 5 blocs", 25, 5)
        );

        // Araignée Tisseuse
        registerAbilities(PetType.ARAIGNEE_TISSEUSE,
            new SlowPassive("spider_slow", "Toile Collante",
                "Ralentit les zombies touchés 1s", 20),
            new WebActive("spider_web", "Toile Géante",
                "Piège les zombies dans une zone 5x5", 30, 4)
        );

        // ==================== RARES ====================

        // Phénix Mineur
        registerAbilities(PetType.PHENIX_MINEUR,
            new RebornPassive("phoenix_reborn", "Renaissance",
                "Renaissance avec 30% HP (CD: 5min)", 0.30, 300),
            new FireNovaActive("phoenix_nova", "Nova de Feu",
                "Explosion de feu (15 dégâts, 5 blocs)", 35, 15, 5)
        );

        // Serpent de Givre
        registerAbilities(PetType.SERPENT_GIVRE,
            new ElementalDamagePassive("frost_damage", "Froid Mordant",
                "+15% dégâts de glace", 0.15, "ICE"),
            new FreezeActive("frost_freeze", "Souffle Glacial",
                "Gèle les ennemis devant (3s)", 30, 3)
        );

        // Hibou Arcanique
        registerAbilities(PetType.HIBOU_ARCANIQUE,
            new CooldownReductionPassive("owl_cdr", "Sagesse Arcanique",
                "-10% cooldown des capacités", 0.10),
            new ResetCooldownActive("owl_reset", "Reset Arcanique",
                "Reset le cooldown d'une capacité", 90)
        );

        // Essaim de Scarabées
        registerAbilities(PetType.ESSAIM_SCARABEES,
            new AuraPassive("swarm_aura", "Nuée",
                "3 dégâts/s aux zombies proches", 3, 2),
            new SwarmActive("swarm_attack", "Nuée",
                "L'essaim attaque une cible (50 dégâts sur 5s)", 25, 50, 5)
        );

        // Spectre Gardien
        registerAbilities(PetType.SPECTRE_GARDIEN,
            new ParryPassive("specter_parry", "Parade Spectrale",
                "Pare automatiquement 1 attaque/30s", 30),
            new RiposteActive("specter_riposte", "Riposte Spectrale",
                "Prochaine attaque = contre-attaque x2", 20, 2.0)
        );

        // ==================== ÉPIQUES ====================

        // Dragon Pygmée
        registerAbilities(PetType.DRAGON_PYGMEE,
            new DamageMultiplierPassive("dragon_power", "Puissance Draconique",
                "+15% dégâts globaux", 0.15),
            new BreathActive("dragon_breath", "Souffle Draconique",
                "Souffle de feu en cône (40 dégâts)", 25, 40)
        );

        // Familier Nécromantique
        registerAbilities(PetType.FAMILIER_NECROMANTIQUE,
            new NecromancyPassive("necro_passive", "Domination",
                "10% de chance de contrôler un zombie tué", 0.10, 15),
            new ResurrectActive("necro_resurrect", "Résurrection",
                "Ressuscite le dernier zombie comme allié", 45, 30)
        );

        // Golem de Cristal
        registerAbilities(PetType.GOLEM_CRISTAL,
            new HealthBonusPassive("crystal_hp", "Corps de Cristal",
                "+25% HP max", 0.25),
            new SacrificeActive("crystal_sacrifice", "Sacrifice Cristallin",
                "Absorbe 100% des dégâts pendant 5s", 60, 5)
        );

        // Félin de l'Ombre
        registerAbilities(PetType.FELIN_OMBRE,
            new CritDamagePassive("cat_crit", "Griffes Assassines",
                "+20% dégâts critiques", 0.20),
            new AmbushActive("cat_ambush", "Embuscade",
                "Prochaine attaque = critique garanti x3", 20, 3.0)
        );

        // Élémentaire Instable
        registerAbilities(PetType.ELEMENTAIRE_INSTABLE,
            new ChaosPassive("elemental_chaos", "Instabilité",
                "Effet aléatoire toutes les 30s", 30),
            new ChaosActive("elemental_implode", "Implosion Chaotique",
                "Effet puissant aléatoire", 30)
        );

        // ==================== LÉGENDAIRES ====================

        // Gardien Angélique
        registerAbilities(PetType.GARDIEN_ANGELIQUE,
            new DebuffImmunityPassive("angel_immunity", "Grâce Divine",
                "Immunité aux effets négatifs"),
            new DivineActive("angel_blessing", "Bénédiction Divine",
                "Invincibilité 3s + full heal", 120, 3)
        );

        // Wyrm du Néant
        registerAbilities(PetType.WYRM_NEANT,
            new TeleportOnDamagePassive("wyrm_blink", "Distorsion",
                "Téléportation 5 blocs en prenant des dégâts", 5, 10),
            new PortalActive("wyrm_portal", "Portail du Néant",
                "Crée un portail vers un point visible", 30, 50)
        );

        // Titan Miniature
        registerAbilities(PetType.TITAN_MINIATURE,
            new MeleeDamagePassive("titan_melee", "Force Titanesque",
                "+30% dégâts de mêlée", 0.30),
            new SmashActive("titan_smash", "Coup Titanesque",
                "Frappe le sol (80 dégâts zone, knockback)", 25, 80, 5)
        );

        // Esprit de la Forêt
        registerAbilities(PetType.ESPRIT_FORET,
            new AdvancedRegenPassive("forest_regen", "Symbiose Naturelle",
                "1 coeur/3s, +50% efficacité soins", 1.0, 60, 0.50),
            new SanctuaryActive("forest_sanctuary", "Sanctuaire Naturel",
                "Zone de soin massive (5 coeurs/s, 10s)", 60, 5, 10)
        );

        // Phénix Ancestral
        registerAbilities(PetType.PHENIX_ANCESTRAL,
            new AdvancedRebornPassive("phoenix_immortal", "Immortalité",
                "Renaissance automatique une fois par vie", 1.0),
            new ApocalypseActive("phoenix_apocalypse", "Apocalypse de Feu",
                "Pluie de feu (zone 10x10, 100 dégâts)", 45, 100, 10)
        );

        // ==================== MYTHIQUES ====================

        // Avatar de la Mort
        registerAbilities(PetType.AVATAR_MORT,
            new ExecutionPassive("death_execute", "Faucheuse",
                "Exécute les ennemis sous 15% HP", 0.15),
            new DeathSentenceActive("death_sentence", "Sentence Mortelle",
                "Marque une cible, elle meurt dans 5s", 90, 5)
        );

        // Entité du Vide
        registerAbilities(PetType.ENTITE_VIDE,
            new TrueDamagePassive("void_damage", "Néant",
                "5% des dégâts ignorent les résistances", 0.05),
            new BlackHoleActive("void_hole", "Dévoration",
                "Crée un trou noir aspirant les ennemis", 60, 5)
        );

        // Chroniqueur Temporel
        registerAbilities(PetType.CHRONIQUEUR_TEMPOREL,
            new SpeedBoostPassive("time_speed", "Accélération Temporelle",
                "+25% vitesse attaque et déplacement", 0.25),
            new TimeStopActive("time_stop", "Arrêt du Temps",
                "Freeze tous les ennemis 4s", 75, 4)
        );

        // Hydre Primordiale
        registerAbilities(PetType.HYDRE_PRIMORDIALE,
            new MultiAttackPassive("hydra_multi", "Trois Têtes",
                "Chaque attaque frappe 3 fois", 3),
            new TripleBreathActive("hydra_breath", "Souffle Tricolore",
                "3 souffles simultanés (feu/glace/poison)", 35)
        );

        // Colossus Oublié
        registerAbilities(PetType.COLOSSUS_OUBLIE,
            new PowerSlowPassive("colossus_power", "Puissance Ancienne",
                "+50% dégâts, -20% vitesse", 0.50, -0.20),
            new ColossusActive("colossus_awaken", "Éveil du Colosse",
                "Transformation géante (10s) - dégâts x3, immunité", 120, 10)
        );

        // ==================== NOUVEAUX PETS SYNERGIES ====================

        // Scarabée de Combo (Combo/Momentum)
        ComboPassive comboPassive = new ComboPassive("combo_stack", "Momentum",
            "+0.5% dégâts par kill consécutif (max +15%)", 0.005, 0.15);
        registerAbilities(PetType.SCARABEE_COMBO,
            comboPassive,
            new ComboExplosionActive("combo_explode", "Explosion de Combo",
                "Consomme le combo pour infliger dégâts = combo × 5", comboPassive)
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

        // Faucon Chasseur (Chasseur Synergy)
        MarkPassive markPassive = new MarkPassive("mark_passive", "Œil du Prédateur",
            "Marque les cibles (+15% dégâts sur marqués)", 0.15);
        registerAbilities(PetType.FAUCON_CHASSEUR,
            markPassive,
            new PredatorStrikeActive("predator_strike", "Frappe Prédatrice",
                "Attaque une cible marquée pour 3x dégâts", markPassive)
        );

        // Orbe d'Âmes (Occultiste Synergy)
        SoulOrbPassive soulPassive = new SoulOrbPassive("soul_orb", "Collecteur d'Âmes",
            "+5% dégâts de skill par orbe (max 5)", 0.05, 5);
        registerAbilities(PetType.ORB_AMES,
            soulPassive,
            new SoulReleaseActive("soul_release", "Libération d'Âmes",
                "Consomme les orbes pour explosion (15 dégâts par orbe)", 15, soulPassive)
        );

        // Salamandre Élémentaire (Multi-Element)
        registerAbilities(PetType.SALAMANDRE_ELEMENTAIRE,
            new ElementalRotationPassive("elemental_rotate", "Rotation Élémentaire",
                "Alterne Feu/Glace/Foudre (+10% dégâts élément actif)", 0.10),
            new ElementalFusionActive("elemental_fusion", "Fusion Élémentaire",
                "Attaque combinant les 3 éléments")
        );

        // Spectre de Vengeance (Damage Taken)
        VengeancePassive vengeancePassive = new VengeancePassive("vengeance_stack", "Accumulation de Rage",
            "50% des dégâts subis = Rage (max 200)", 50, 200);
        registerAbilities(PetType.SPECTRE_VENGEANCE,
            vengeancePassive,
            new VengeanceExplosionActive("vengeance_explode", "Explosion de Vengeance",
                "Libère la Rage en dégâts de zone", vengeancePassive)
        );

        // Djinn du Jackpot (Jackpot System)
        registerAbilities(PetType.DJINN_JACKPOT,
            new JackpotPassive("jackpot_passive", "Fortune du Djinn",
                "+30% chance Jackpot, +50% récompenses", 0.30, 0.50),
            new SuperJackpotActive("super_jackpot", "Super Jackpot",
                "Déclenche un Jackpot garanti x3")
        );

        // Dragon Chromatique (Class Adaptive)
        registerAbilities(PetType.DRAGON_CHROMATIQUE,
            new ClassAdaptivePassive("chromatic_adapt", "Adaptation Chromatique",
                "Bonus +25% selon classe (mêlée/crit/skill)", 0.25),
            new ChromaticBreathActive("chromatic_breath", "Souffle Chromatique",
                "Attaque adaptée à la classe")
        );

        // Sentinelle des Zones (Zone Environment)
        registerAbilities(PetType.SENTINELLE_ZONES,
            new ZoneAdaptPassive("zone_adapt", "Adaptation Environnementale",
                "+15% bonus selon effet de zone actif", 0.15),
            new ZoneMasteryActive("zone_mastery", "Maîtrise de Zone",
                "Immunité zone 10s + bonus zone x2")
        );

        // Symbiote Éternel (Total Amplification)
        registerAbilities(PetType.SYMBIOTE_ETERNEL,
            new SymbiotePassive("symbiote_amp", "Symbiose Éternelle",
                "Amplifie tous les bonus du joueur de 20%", 0.20),
            new SymbioticFusionActive("symbiote_fusion", "Fusion Symbiotique",
                "15s: tous bonus x2, régén +50%, immunité CC")
        );

        // Nexus Dimensionnel (Team Support)
        registerAbilities(PetType.NEXUS_DIMENSIONNEL,
            new NexusAuraPassive("nexus_aura", "Aura Dimensionnelle",
                "Alliés +15% stats, ennemis -10% stats", 0.15, 0.10, 20),
            new DimensionalConvergenceActive("nexus_converge", "Convergence Dimensionnelle",
                "TP alliés vers vous + bouclier groupe", 100)
        );

        // ==================== NOUVEAUX PETS VISUELS ====================

        // Étoile Filante (Stellaire / Traînée)
        registerAbilities(PetType.ETOILE_FILANTE,
            new StardustTrailPassive("stardust_trail", "Traînée Stellaire",
                "Laisse une traînée infligeant 3 dégâts", 3),
            new ShootingStarActive("shooting_stars", "Pluie d'Étoiles",
                "8 étoiles filantes sur la zone", 12, 8)
        );

        // Serpent Foudroyant (Foudre / Chaîne)
        registerAbilities(PetType.SERPENT_FOUDROYANT,
            new ChainLightningPassive("chain_lightning", "Foudre en Chaîne",
                "20% de déclencher un éclair en chaîne", 0.20, 5, 10),
            new ThunderstormActive("thunderstorm", "Tempête de Foudre",
                "6 éclairs sur les ennemis", 25, 6)
        );

        // Golem de Lave (Lave / Traînée)
        registerAbilities(PetType.GOLEM_LAVE,
            new LavaTrailPassive("lava_trail", "Traînée de Lave",
                "Laisse une traînée brûlant 5 dégâts/s", 5),
            new VolcanicEruptionActive("volcanic_eruption", "Éruption Volcanique",
                "Colonne de feu + 10 boules de lave", 15, 10)
        );

        // Phénix Solaire (Feu / Météores)
        registerAbilities(PetType.PHOENIX_SOLAIRE,
            new FireballPassive("solar_fireball", "Boule de Feu Solaire",
                "25% de lancer une boule de feu", 0.25, 15),
            new MeteorShowerActive("meteor_shower", "Pluie de Météores",
                "12 météores de feu sur la zone", 20, 12)
        );

        // Ombre Déchirante (Ombre / Tentacules)
        registerAbilities(PetType.OMBRE_DECHIRANTE,
            new ShadowTentaclePassive("shadow_tentacle", "Tentacule d'Ombre",
                "20% de faire surgir un tentacule", 0.20, 8),
            new VoidVortexActive("void_vortex", "Vortex du Néant",
                "Aspire les ennemis puis explose", 20, 4)
        );

        // Hydre de Givre (Glace / Blizzard)
        registerAbilities(PetType.HYDRE_GIVRE,
            new IceShardPassive("ice_shard", "Éclat de Glace",
                "30% de lancer un éclat de glace", 0.30, 2),
            new BlizzardActive("blizzard", "Blizzard",
                "8s de tempête, 10 dégâts/s", 10, 8, 8)
        );

        // Esprit Prismatique (Lumière / Arc-en-ciel)
        registerAbilities(PetType.ESPRIT_PRISMATIQUE,
            new PrismaticBeamPassive("prismatic_beam", "Rayon Prismatique",
                "Tire un rayon arc-en-ciel (+10 dégâts)", 10),
            new RainbowNovaActive("rainbow_nova", "Nova Prismatique",
                "Onde arc-en-ciel expansive (30 dégâts)", 30, 10)
        );

        // Kraken Miniature (Eau / Tentacules)
        registerAbilities(PetType.KRAKEN_MINIATURE,
            new WaterTentaclePassive("water_tentacle", "Tentacule d'Eau",
                "15% d'immobiliser avec un tentacule", 0.15, 2),
            new TsunamiActive("tsunami", "Tsunami",
                "Vague géante (50 dégâts, knockback)", 50, 25)
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
