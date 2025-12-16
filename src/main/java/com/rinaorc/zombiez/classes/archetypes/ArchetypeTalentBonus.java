package com.rinaorc.zombiez.classes.archetypes;

import com.rinaorc.zombiez.classes.talents.ClassTalent;
import lombok.Getter;

import java.util.*;

/**
 * Bonus de talents basés sur l'archétype actif
 *
 * PHILOSOPHIE:
 * - Certains talents sont "clés" pour un archétype
 * - Ces talents reçoivent un bonus de +50% d'efficacité
 * - Les talents "anti-synergiques" peuvent avoir une pénalité
 * - Encourage à spécialiser son build selon l'archétype
 *
 * EXEMPLE:
 * - Archétype "Tornade" + talent "Balayage" = +50% cibles touchées
 * - Archétype "Boucher" + talent "Exécution" = +50% dégâts d'exécution
 */
public class ArchetypeTalentBonus {

    // Bonus standard pour les talents synergiques
    private static final double SYNERGY_BONUS = 0.50;  // +50%

    // Pénalité pour les talents anti-synergiques (optionnel, généralement pas utilisé)
    private static final double ANTI_SYNERGY_PENALTY = -0.20;  // -20%

    // Cache des bonus par archétype
    private static final Map<BuildArchetype, Map<String, TalentSynergy>> SYNERGY_CACHE = new EnumMap<>(BuildArchetype.class);

    static {
        initializeSynergies();
    }

    /**
     * Initialise toutes les synergies talents/archétypes
     */
    private static void initializeSynergies() {
        // ==================== GUERRIER ====================
        initGuerrierTornade();
        initGuerrierMur();
        initGuerrierBoucher();

        // ==================== CHASSEUR ====================
        initChasseurGatling();
        initChasseurFantome();
        initChasseurPiegeur();

        // ==================== OCCULTISTE ====================
        initOccultisteDeflagration();
        initOccultisteSang();
        initOccultisteArchimage();
    }

    // ==================== GUERRIER SYNERGIES ====================

    private static void initGuerrierTornade() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Tornade (AoE en mouvement)
        synergies.put("gue_cleave", new TalentSynergy(
            "Balayage Cyclonique",
            "+50% cibles touchées par le cleave",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("gue_dmg_1", new TalentSynergy(
            "Force du Vortex",
            "+50% dégâts de base en mouvement",
            SYNERGY_BONUS, SynergyType.CONDITIONAL  // Bonus seulement si en mouvement
        ));
        synergies.put("gue_rage", new TalentSynergy(
            "Rage Tournoyante",
            "+50% dégâts de rage, actif même au-dessus de 40% HP si en mouvement",
            SYNERGY_BONUS, SynergyType.BONUS
        ));

        // Anti-synergies (style stationnaire)
        synergies.put("gue_laststand", new TalentSynergy(
            "Survie Instable",
            "Moins efficace pour un style mobile",
            ANTI_SYNERGY_PENALTY, SynergyType.PENALTY
        ));

        SYNERGY_CACHE.put(BuildArchetype.GUERRIER_TORNADE, synergies);
    }

    private static void initGuerrierMur() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Mur Vivant (Tank)
        synergies.put("gue_hp_1", new TalentSynergy(
            "Constitution du Titan",
            "+50% bonus de HP",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("gue_armor", new TalentSynergy(
            "Armure Impénétrable",
            "+50% réduction de dégâts",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("gue_laststand", new TalentSynergy(
            "Dernier Rempart",
            "Cooldown réduit de 1min",
            0, SynergyType.SPECIAL  // Effet spécial, pas un multiplicateur
        ));
        synergies.put("gue_regen", new TalentSynergy(
            "Régénération Fortifiée",
            "+50% régén, actif à tout moment (pas seulement hors combat)",
            SYNERGY_BONUS, SynergyType.CONDITIONAL
        ));

        // Anti-synergies (style offensif)
        synergies.put("gue_execute", new TalentSynergy(
            "Exécution Défensive",
            "Moins efficace pour un style défensif",
            ANTI_SYNERGY_PENALTY, SynergyType.PENALTY
        ));

        SYNERGY_CACHE.put(BuildArchetype.GUERRIER_MUR, synergies);
    }

    private static void initGuerrierBoucher() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Boucher (Single-target, Execution)
        synergies.put("gue_execute", new TalentSynergy(
            "Coup de Grâce",
            "+50% dégâts d'exécution, seuil augmenté à 40% HP",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("gue_rage", new TalentSynergy(
            "Rage du Prédateur",
            "+50% dégâts de rage, dure 2s de plus",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("gue_warlord", new TalentSynergy(
            "Chasseur de Têtes",
            "Reset ultime après 2 kills au lieu de 3",
            0, SynergyType.SPECIAL
        ));
        synergies.put("gue_dmg_1", new TalentSynergy(
            "Force Brutale Concentrée",
            "+50% dégâts de base",
            SYNERGY_BONUS, SynergyType.BONUS
        ));

        // Anti-synergies (style AoE)
        synergies.put("gue_cleave", new TalentSynergy(
            "Balayage Inefficace",
            "Moins efficace pour un style mono-cible",
            ANTI_SYNERGY_PENALTY, SynergyType.PENALTY
        ));

        SYNERGY_CACHE.put(BuildArchetype.GUERRIER_BOUCHER, synergies);
    }

    // ==================== CHASSEUR SYNERGIES ====================

    private static void initChasseurGatling() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Gatling (Tir rapide, DPS soutenu)
        synergies.put("cha_reload", new TalentSynergy(
            "Rechargement Rapide",
            "+50% réduction de cooldown",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_crit_1", new TalentSynergy(
            "Tirs Chanceux",
            "+50% chance de critique",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_speed", new TalentSynergy(
            "Kiting Parfait",
            "+50% vitesse, bonus supplémentaire si en mouvement",
            SYNERGY_BONUS, SynergyType.CONDITIONAL
        ));
        synergies.put("cha_pierce", new TalentSynergy(
            "Perforation Multiple",
            "+1 cible traversée supplémentaire",
            0, SynergyType.SPECIAL
        ));

        SYNERGY_CACHE.put(BuildArchetype.CHASSEUR_GATLING, synergies);
    }

    private static void initChasseurFantome() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Fantôme (Stealth, Burst)
        synergies.put("cha_stealth", new TalentSynergy(
            "Maître Assassin",
            "+50% dégâts de furtivité",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_headshot", new TalentSynergy(
            "Tir Mortel",
            "+50% dégâts de headshot",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_deadeye", new TalentSynergy(
            "Oeil du Chasseur",
            "Le crit garanti dure 2 tirs au lieu de 1",
            0, SynergyType.SPECIAL
        ));
        synergies.put("cha_vanish", new TalentSynergy(
            "Évanescence",
            "Invisibilité prolongée à 3s, cooldown réduit",
            0, SynergyType.SPECIAL
        ));
        synergies.put("cha_crit_dmg", new TalentSynergy(
            "Frappe Assassine",
            "+50% dégâts critiques",
            SYNERGY_BONUS, SynergyType.BONUS
        ));

        SYNERGY_CACHE.put(BuildArchetype.CHASSEUR_FANTOME, synergies);
    }

    private static void initChasseurPiegeur() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Piégeur (Contrôle de zone, Pièges)
        synergies.put("cha_pierce", new TalentSynergy(
            "Tirs de Recul",
            "Les tirs repoussent les ennemis vers les pièges",
            0, SynergyType.SPECIAL
        ));
        synergies.put("cha_speed", new TalentSynergy(
            "Repositionnement Rapide",
            "+50% vitesse",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_reload", new TalentSynergy(
            "Pose Rapide",
            "+50% réduction cooldown pour poser plus de pièges",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("cha_dodge", new TalentSynergy(
            "Esquive du Trappeur",
            "+50% chance d'esquive près de vos pièges",
            SYNERGY_BONUS, SynergyType.CONDITIONAL
        ));

        SYNERGY_CACHE.put(BuildArchetype.CHASSEUR_PIEGEUR, synergies);
    }

    // ==================== OCCULTISTE SYNERGIES ====================

    private static void initOccultisteDeflagration() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Déflagration (Stack & Explode)
        synergies.put("occ_dot", new TalentSynergy(
            "Corruption Explosive",
            "+50% dégâts DoT, les DoT peuvent exploser",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_aoe", new TalentSynergy(
            "Rayon Cataclysmique",
            "+50% rayon AoE",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_power_1", new TalentSynergy(
            "Puissance Dévastatrice",
            "+50% dégâts de compétences",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_execute_reset", new TalentSynergy(
            "Réaction en Chaîne",
            "Les explosions comptent comme des kills pour le reset",
            0, SynergyType.SPECIAL
        ));

        SYNERGY_CACHE.put(BuildArchetype.OCCULTISTE_DEFLAGRATION, synergies);
    }

    private static void initOccultisteSang() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Mage de Sang (Drain, Risque/Récompense)
        synergies.put("occ_leech", new TalentSynergy(
            "Drain Vorace",
            "+50% vol de vie des sorts",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_shield", new TalentSynergy(
            "Barrière de Sang",
            "+50% conversion dégâts->énergie",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_immortal", new TalentSynergy(
            "Pacte de Survie",
            "Ressuscite avec 40% HP au lieu de 25%",
            0, SynergyType.SPECIAL
        ));
        synergies.put("occ_power_1", new TalentSynergy(
            "Pouvoir du Sang",
            "Dégâts augmentés quand HP < 50%",
            0, SynergyType.CONDITIONAL
        ));

        SYNERGY_CACHE.put(BuildArchetype.OCCULTISTE_SANG, synergies);
    }

    private static void initOccultisteArchimage() {
        Map<String, TalentSynergy> synergies = new HashMap<>();

        // Talents clés pour Archimage (Combos, Efficacité)
        synergies.put("occ_energy_1", new TalentSynergy(
            "Réserves Profondes",
            "+50% énergie max",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_regen_energy", new TalentSynergy(
            "Flux Constant",
            "+50% régén d'énergie",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_cdr", new TalentSynergy(
            "Maîtrise du Temps",
            "+50% réduction de cooldown",
            SYNERGY_BONUS, SynergyType.BONUS
        ));
        synergies.put("occ_execute_reset", new TalentSynergy(
            "Combo Master",
            "Les combos réussis réduisent aussi le CD de l'ultime",
            0, SynergyType.SPECIAL
        ));

        SYNERGY_CACHE.put(BuildArchetype.OCCULTISTE_ARCHIMAGE, synergies);
    }

    // ==================== MÉTHODES PUBLIQUES ====================

    /**
     * Obtient la synergie entre un talent et un archétype
     */
    public static TalentSynergy getSynergy(BuildArchetype archetype, String talentId) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return TalentSynergy.NONE;
        }

        Map<String, TalentSynergy> synergies = SYNERGY_CACHE.get(archetype);
        if (synergies == null) {
            return TalentSynergy.NONE;
        }

        return synergies.getOrDefault(talentId, TalentSynergy.NONE);
    }

    /**
     * Calcule la valeur effective d'un talent avec les bonus d'archétype
     */
    public static double calculateEffectiveValue(BuildArchetype archetype, String talentId,
                                                  double baseValue, int talentLevel) {
        TalentSynergy synergy = getSynergy(archetype, talentId);

        if (synergy.getType() == SynergyType.NONE || synergy.getType() == SynergyType.SPECIAL) {
            return baseValue * talentLevel;
        }

        double modifier = synergy.getModifier();
        return baseValue * talentLevel * (1.0 + modifier);
    }

    /**
     * Vérifie si un talent a un effet spécial avec l'archétype actif
     */
    public static boolean hasSpecialEffect(BuildArchetype archetype, String talentId) {
        TalentSynergy synergy = getSynergy(archetype, talentId);
        return synergy.getType() == SynergyType.SPECIAL;
    }

    /**
     * Obtient tous les talents synergiques pour un archétype
     */
    public static List<String> getSynergyTalents(BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return Collections.emptyList();
        }

        Map<String, TalentSynergy> synergies = SYNERGY_CACHE.get(archetype);
        if (synergies == null) {
            return Collections.emptyList();
        }

        return synergies.entrySet().stream()
            .filter(e -> e.getValue().getType() == SynergyType.BONUS ||
                        e.getValue().getType() == SynergyType.CONDITIONAL)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Type de synergie
     */
    public enum SynergyType {
        NONE,         // Pas de synergie
        BONUS,        // Bonus direct (+X%)
        CONDITIONAL,  // Bonus conditionnel (situation requise)
        PENALTY,      // Pénalité (-X%)
        SPECIAL       // Effet spécial (pas un multiplicateur)
    }

    /**
     * Représente une synergie talent/archétype
     */
    @Getter
    public static class TalentSynergy {
        public static final TalentSynergy NONE = new TalentSynergy(
            null, null, 0, SynergyType.NONE
        );

        private final String bonusName;
        private final String bonusDescription;
        private final double modifier;
        private final SynergyType type;

        public TalentSynergy(String bonusName, String bonusDescription,
                           double modifier, SynergyType type) {
            this.bonusName = bonusName;
            this.bonusDescription = bonusDescription;
            this.modifier = modifier;
            this.type = type;
        }

        public boolean hasSynergy() {
            return type != SynergyType.NONE;
        }

        /**
         * Génère les lignes de lore pour l'affichage
         */
        public String[] getLoreLines(String archetypeColor) {
            if (!hasSynergy()) {
                return new String[0];
            }

            String modifierText;
            if (type == SynergyType.SPECIAL) {
                modifierText = "§d✧ Effet spécial";
            } else if (modifier > 0) {
                modifierText = "§a+" + String.format("%.0f%%", modifier * 100);
            } else if (modifier < 0) {
                modifierText = "§c" + String.format("%.0f%%", modifier * 100);
            } else {
                modifierText = "";
            }

            return new String[]{
                "",
                archetypeColor + "§l» " + bonusName + " " + modifierText,
                "§7" + bonusDescription
            };
        }
    }
}
