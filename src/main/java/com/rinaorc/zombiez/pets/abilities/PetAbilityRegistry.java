package com.rinaorc.zombiez.pets.abilities;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.abilities.impl.*;
import lombok.Getter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registre des capacités de tous les Pets
 * Gère l'association entre PetType et leurs capacités
 */
public class PetAbilityRegistry {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<PetType, PetAbility> passiveAbilities = new EnumMap<>(PetType.class);

    @Getter
    private final Map<PetType, PetAbility> activeAbilities = new EnumMap<>(PetType.class);

    public PetAbilityRegistry(ZombieZPlugin plugin) {
        this.plugin = plugin;
        registerAllAbilities();
    }

    /**
     * Enregistre toutes les capacités de tous les pets
     */
    private void registerAllAbilities() {
        // ==================== COMMUNS ====================

        // Chauve-Souris Fantôme
        registerAbilities(PetType.CHAUVE_SOURIS_FANTOME,
            new DetectionPassive("bat_detect", "Détection",
                "Détecte les zombies dans un rayon de 15 blocs", 15),
            new EchoScanActive("bat_echo", "Écho-Scan",
                "Révèle tous les ennemis dans 30 blocs pendant 5s", 30, 5)
        );

        // Rat des Catacombes
        registerAbilities(PetType.RAT_CATACOMBES,
            new LootBonusPassive("rat_loot", "Fouineur",
                "+5% de chance de loot supplémentaire", 0.05),
            new SearchActive("rat_search", "Fouille",
                "Cherche des ressources au sol", 60)
        );

        // Luciole Errante
        registerAbilities(PetType.LUCIOLE_ERRANTE,
            new LightPassive("firefly_light", "Lumière",
                "Éclaire un rayon de 5 blocs", 5),
            new FlashActive("firefly_flash", "Flash Aveuglant",
                "Aveugle les zombies proches 3s", 25, 3)
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
    }

    /**
     * Enregistre les capacités passive et active pour un pet
     */
    private void registerAbilities(PetType type, PetAbility passive, PetAbility active) {
        if (passive != null) {
            passiveAbilities.put(type, passive);
        }
        if (active != null) {
            activeAbilities.put(type, active);
        }
    }

    /**
     * Obtient la capacité passive d'un pet
     */
    public PetAbility getPassive(PetType type) {
        return passiveAbilities.get(type);
    }

    /**
     * Obtient la capacité active d'un pet
     */
    public PetAbility getActive(PetType type) {
        return activeAbilities.get(type);
    }
}
