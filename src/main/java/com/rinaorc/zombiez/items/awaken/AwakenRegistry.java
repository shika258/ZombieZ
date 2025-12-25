package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import com.rinaorc.zombiez.items.types.ItemType;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre central des templates et éveils
 *
 * Gère:
 * - Les templates d'éveils par type d'effet de talent
 * - Les overrides personnalisés par talent spécifique
 * - La génération d'éveils
 */
public class AwakenRegistry {

    // ==================== SINGLETON ====================
    private static class Holder {
        static final AwakenRegistry INSTANCE = new AwakenRegistry();
    }

    public static AwakenRegistry getInstance() {
        return Holder.INSTANCE;
    }

    // ==================== DONNÉES ====================

    /**
     * Templates par type d'effet
     */
    private final Map<Talent.TalentEffectType, AwakenTemplate> templatesByEffect = new EnumMap<>(Talent.TalentEffectType.class);

    /**
     * Overrides par talent ID (pour les éveils personnalisés)
     */
    private final Map<String, AwakenTemplate> overridesByTalent = new ConcurrentHashMap<>();

    /**
     * Cache des éveils générés (pour les items existants)
     */
    @Getter
    private final Map<String, Awaken> cachedAwakens = new ConcurrentHashMap<>();

    private AwakenRegistry() {
        registerDefaultTemplates();
    }

    // ==================== ENREGISTREMENT ====================

    /**
     * Enregistre les templates par défaut pour chaque type d'effet
     * REFACTORÉ: Utilise les templates spécialisés par voie pour un meilleur équilibrage
     */
    private void registerDefaultTemplates() {
        // === GUERRIER - VOIE DU SÉISME ===
        registerGuerrierSeismeTemplates();

        // === GUERRIER - VOIE DU REMPART ===
        registerGuerrierRempartTemplates();

        // === GUERRIER - VOIE DE LA RAGE ===
        registerGuerrierRageTemplates();

        // === GUERRIER - VOIE DU SANG ===
        registerGuerrierSangTemplates();

        // === GUERRIER - VOIE DU FAUVE ===
        registerGuerrierFauveTemplates();

        // === CHASSEUR - VOIE DU BARRAGE ===
        registerChasseurBarrageTemplates();

        // === CHASSEUR - VOIE DES BÊTES ===
        registerChasseurBetesTemplates();

        // === CHASSEUR - VOIE DE L'OMBRE ===
        registerChasseurOmbreTemplates();

        // === CHASSEUR - VOIE DU POISON ===
        registerChasseurPoisonTemplates();

        // === CHASSEUR - VOIE DU GIVRE ===
        registerChasseurGivreTemplates();

        // === OCCULTISTE - VOIE DU FEU ===
        registerOccultisteFeuTemplates();

        // === OCCULTISTE - VOIE DE LA GLACE ===
        registerOccultisteGlaceTemplates();

        // === OCCULTISTE - VOIE DE LA FOUDRE ===
        registerOccultisteFoudreTemplates();

        // === OCCULTISTE - VOIE DE L'ÂME ===
        registerOccultisteAmeTemplates();

        // === OCCULTISTE - VOIE DU VIDE ===
        registerOccultisteVideTemplates();

        // === TALENTS ULTIMES (override avec bonus CDR accrus) ===
        registerUltimateTemplates();
    }

    // ==================== GUERRIER ====================

    private void registerGuerrierSeismeTemplates() {
        // Voie du Séisme - AoE & Contrôle (slot 0)
        for (var effect : List.of(
            Talent.TalentEffectType.SEISMIC_STRIKE,      // T1
            Talent.TalentEffectType.WAR_ECHO,            // T2
            Talent.TalentEffectType.FRACTURE_WAVE,       // T3 - compteur!
            Talent.TalentEffectType.SEISMIC_RESONANCE,   // T4
            Talent.TalentEffectType.CATACLYSM,           // T5 - compteur!
            Talent.TalentEffectType.SEISMIC_AFTERMATH,   // T6
            Talent.TalentEffectType.ETERNAL_TREMOR,      // T7
            Talent.TalentEffectType.EARTH_APOCALYPSE     // T8 - compteur!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forSeismeTalent(effect));
        }
    }

    private void registerGuerrierRempartTemplates() {
        // Voie du Rempart - Défense & Riposte (slot 1)
        for (var effect : List.of(
            Talent.TalentEffectType.DEFENSIVE_STANCE,    // T1
            Talent.TalentEffectType.PUNISHMENT,          // T2
            Talent.TalentEffectType.VENGEFUL_SHIELD,     // T3
            Talent.TalentEffectType.FORTIFY,             // T4
            Talent.TalentEffectType.JUDGMENT_HAMMER,     // T5 - seuil HP!
            Talent.TalentEffectType.IRON_ECHO,           // T6
            Talent.TalentEffectType.BASTION_CHARGE,      // T7
            Talent.TalentEffectType.DEFIANCE_AURA        // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forRempartTalent(effect));
        }
    }

    private void registerGuerrierRageTemplates() {
        // Voie de la Rage - Stacking & Berserker (slot 2)
        for (var effect : List.of(
            Talent.TalentEffectType.RISING_FURY,         // T1
            Talent.TalentEffectType.BLOOD_FERVOUR,       // T2
            Talent.TalentEffectType.ANCESTRAL_WRATH,     // T3
            Talent.TalentEffectType.MERCY_STRIKE,        // T4 - seuil HP!
            Talent.TalentEffectType.RAGE_CYCLONE,        // T5
            Talent.TalentEffectType.UNSTOPPABLE_RAGE,    // T6 - compteur!
            Talent.TalentEffectType.BLOOD_CYCLONES,      // T7
            Talent.TalentEffectType.WARRIOR_FRENZY       // T8 - compteur!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forRageTalent(effect));
        }
    }

    private void registerGuerrierSangTemplates() {
        // Voie du Sang - Vampirisme & Os (slot 3)
        for (var effect : List.of(
            Talent.TalentEffectType.DEATH_STRIKE,        // T1
            Talent.TalentEffectType.BONE_SHIELD,         // T2
            Talent.TalentEffectType.MARROWREND,          // T3
            Talent.TalentEffectType.VAMPIRIC_WILL,       // T4
            Talent.TalentEffectType.DEATH_AND_DECAY,     // T5
            Talent.TalentEffectType.CONSUMPTION,         // T6 - seuil HP!
            Talent.TalentEffectType.BLOOD_PACT,          // T7
            Talent.TalentEffectType.VAMPIRIC_HEART       // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forSangTalent(effect));
        }
    }

    private void registerGuerrierFauveTemplates() {
        // Voie du Fauve - Dash & Saignement (slot 4)
        for (var effect : List.of(
            Talent.TalentEffectType.LUNGING_STRIKE,      // T1
            Talent.TalentEffectType.WAR_CRY_MARK,        // T2
            Talent.TalentEffectType.LACERATING_CLAWS,    // T3
            Talent.TalentEffectType.FURIOUS_MOMENTUM,    // T4
            Talent.TalentEffectType.FURY_CONSUMPTION,    // T5
            Talent.TalentEffectType.INSATIABLE_PREDATOR, // T6
            Talent.TalentEffectType.EVISCERATION,        // T7 - compteur!
            Talent.TalentEffectType.WAR_FRENZY           // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forFauveTalent(effect));
        }
    }

    // ==================== CHASSEUR ====================

    private void registerChasseurBarrageTemplates() {
        // Voie du Barrage - Multi-projectiles & Pluies (slot 0)
        for (var effect : List.of(
            Talent.TalentEffectType.MULTI_SHOT,          // T1
            Talent.TalentEffectType.BURST_SHOT,          // T2 - compteur!
            Talent.TalentEffectType.ARROW_RAIN,          // T3
            Talent.TalentEffectType.DELUGE,              // T4
            Talent.TalentEffectType.STEEL_STORM,         // T5
            Talent.TalentEffectType.BARRAGE_FURY,        // T6 - compteur!
            Talent.TalentEffectType.CYCLONE_EYE,         // T7
            Talent.TalentEffectType.DEVASTATING_SWARM    // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forBarrageTalent(effect));
        }
    }

    private void registerChasseurBetesTemplates() {
        // Voie des Bêtes - Invocations & Meute (slot 1)
        for (var effect : List.of(
            Talent.TalentEffectType.BEAST_BAT,           // T1
            Talent.TalentEffectType.BEAST_ENDERMITE,     // T2
            Talent.TalentEffectType.BEAST_WOLF,          // T3
            Talent.TalentEffectType.BEAST_AXOLOTL,       // T4
            Talent.TalentEffectType.BEAST_COW,           // T5
            Talent.TalentEffectType.BEAST_LLAMA,         // T6
            Talent.TalentEffectType.BEAST_FOX,           // T7
            Talent.TalentEffectType.BEAST_BEE            // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forBetesTalent(effect));
        }
    }

    private void registerChasseurOmbreTemplates() {
        // Voie de l'Ombre - Points d'Ombre & Exécution (slot 2)
        for (var effect : List.of(
            Talent.TalentEffectType.SHADOW_BLADE,        // T1
            Talent.TalentEffectType.SHADOW_SHOT,         // T2
            Talent.TalentEffectType.SHADOW_STEP,         // T3
            Talent.TalentEffectType.DEATH_MARK,          // T4
            Talent.TalentEffectType.EXECUTION,           // T5 - compteur!
            Talent.TalentEffectType.DANSE_MACABRE,       // T6
            Talent.TalentEffectType.SHADOW_CLONE,        // T7
            Talent.TalentEffectType.SHADOW_STORM         // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forOmbreTalent(effect));
        }
    }

    private void registerChasseurPoisonTemplates() {
        // Voie du Poison - Virulence & DoT (slot 3)
        for (var effect : List.of(
            Talent.TalentEffectType.VENOMOUS_STRIKE,     // T1
            Talent.TalentEffectType.CORROSIVE_VENOM,     // T2
            Talent.TalentEffectType.DEADLY_TOXINS,       // T3
            Talent.TalentEffectType.PANDEMIC,            // T4
            Talent.TalentEffectType.EPIDEMIC,            // T5 - seuil virulence!
            Talent.TalentEffectType.TOXIC_SYNERGY,       // T6
            Talent.TalentEffectType.BLACK_PLAGUE,        // T7
            Talent.TalentEffectType.BLIGHT               // T8 - seuil virulence!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forPoisonTalent(effect));
        }
    }

    private void registerChasseurGivreTemplates() {
        // Voie du Givre - Rebonds & Gel (slot 4) - CRITIQUE: 4 compteurs!
        for (var effect : List.of(
            Talent.TalentEffectType.PIERCING_ARROWS,     // T1
            Talent.TalentEffectType.CALIBER,             // T2 - compteur!
            Talent.TalentEffectType.FATAL_TRAJECTORY,    // T3
            Talent.TalentEffectType.OVERHEAT,            // T4 - compteur! (Hypothermie)
            Talent.TalentEffectType.ABSOLUTE_PERFORATION,// T5
            Talent.TalentEffectType.HUNTER_MOMENTUM,     // T6 - compteur! (Tempête de Neige)
            Talent.TalentEffectType.CHAIN_PERFORATION,   // T7
            Talent.TalentEffectType.DEVASTATION          // T8 - compteur! (Hiver Éternel)
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forGivreTalent(effect));
        }
    }

    // ==================== OCCULTISTE ====================

    private void registerOccultisteFeuTemplates() {
        // Voie du Feu - Surchauffe & Météores (slot 0)
        for (var effect : List.of(
            Talent.TalentEffectType.IGNITE,              // T1
            Talent.TalentEffectType.FIRE_SPREAD,         // T2
            Talent.TalentEffectType.FIRESTORM,           // T3
            Talent.TalentEffectType.PHOENIX_FLAME,       // T4 - seuil Surchauffe!
            Talent.TalentEffectType.FIRE_AVATAR,         // T5
            Talent.TalentEffectType.PYROCLASM,           // T6
            Talent.TalentEffectType.INFERNO,             // T7
            Talent.TalentEffectType.BLACK_SUN            // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forFeuTalent(effect));
        }
    }

    private void registerOccultisteGlaceTemplates() {
        // Voie de la Glace - Stacks de Givre & Brisure (slot 1)
        for (var effect : List.of(
            Talent.TalentEffectType.FROST_BITE,          // T1
            Talent.TalentEffectType.FROZEN_HEART,        // T2
            Talent.TalentEffectType.BLIZZARD,            // T3
            Talent.TalentEffectType.ABSOLUTE_ZERO,       // T4 - compteur stacks!
            Talent.TalentEffectType.FROST_LORD,          // T5
            Talent.TalentEffectType.PERMAFROST,          // T6
            Talent.TalentEffectType.ICE_AGE,             // T7 - compteur stacks!
            Talent.TalentEffectType.ETERNAL_WINTER       // T8
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forGlaceTalent(effect));
        }
    }

    private void registerOccultisteFoudreTemplates() {
        // Voie de la Foudre - Chaînes & Multi-strikes (slot 2)
        for (var effect : List.of(
            Talent.TalentEffectType.CHAIN_LIGHTNING,     // T1
            Talent.TalentEffectType.OVERCHARGE,          // T2
            Talent.TalentEffectType.LIGHTNING_STORM,     // T3
            Talent.TalentEffectType.CONDUCTOR,           // T4
            Talent.TalentEffectType.THUNDER_GOD,         // T5
            Talent.TalentEffectType.STATIC_FIELD,        // T6
            Talent.TalentEffectType.PERPETUAL_STORM,     // T7
            Talent.TalentEffectType.MJOLNIR              // T8 - compteur strikes!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forFoudreTalent(effect));
        }
    }

    private void registerOccultisteAmeTemplates() {
        // Voie de l'Âme - Orbes & Invocations (slot 3)
        for (var effect : List.of(
            Talent.TalentEffectType.SOUL_SIPHON,         // T1
            Talent.TalentEffectType.SOUL_RESERVOIR,      // T2
            Talent.TalentEffectType.SOUL_PACT,           // T3
            Talent.TalentEffectType.ETERNAL_HARVEST,     // T4
            Talent.TalentEffectType.SOUL_LEGION,         // T5
            Talent.TalentEffectType.SOUL_BOND,           // T6
            Talent.TalentEffectType.NECROMANCER,         // T7 - invocations!
            Talent.TalentEffectType.LORD_OF_THE_DEAD     // T8 - invocations!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forAmeTalent(effect));
        }
    }

    private void registerOccultisteVideTemplates() {
        // Voie du Vide - DOTs & Gravité (slot 4)
        for (var effect : List.of(
            Talent.TalentEffectType.SHADOW_WORD,         // T1
            Talent.TalentEffectType.VAMPIRIC_TOUCH,      // T2
            Talent.TalentEffectType.SHADOWY_APPARITIONS, // T3
            Talent.TalentEffectType.DARK_GRAVITY,        // T4
            Talent.TalentEffectType.IMPLOSION,           // T5
            Talent.TalentEffectType.GRAVITY_WELL,        // T6
            Talent.TalentEffectType.SINGULARITY,         // T7 - compteur kills!
            Talent.TalentEffectType.DIMENSIONAL_RIFT     // T8 - seuil HP!
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forVideTalent(effect));
        }
    }

    // ==================== TALENTS ULTIMES (TIER 9) ====================

    private void registerUltimateTemplates() {
        // Les talents de Tier 9 utilisent des templates ultimes avec des bonus CDR accrus
        // Ces templates OVERRIDE les templates par voie pour les T9
        for (var effect : List.of(
            // Guerrier
            Talent.TalentEffectType.RAGNAROK,            // Séisme T9
            Talent.TalentEffectType.BULWARK_AVATAR,      // Rempart T9
            Talent.TalentEffectType.MEGA_TORNADO,        // Rage T9
            Talent.TalentEffectType.DANCING_RUNE_WEAPON, // Sang T9
            Talent.TalentEffectType.BERSERKER_RAGE,      // Fauve T9
            // Chasseur
            Talent.TalentEffectType.ORBITAL_STRIKE,      // Barrage T9
            Talent.TalentEffectType.BEAST_IRON_GOLEM,    // Bêtes T9
            Talent.TalentEffectType.SHADOW_AVATAR,       // Ombre T9
            Talent.TalentEffectType.PLAGUE_AVATAR,       // Poison T9
            Talent.TalentEffectType.JUDGMENT,            // Givre T9 (Zéro Absolu)
            // Occultiste
            Talent.TalentEffectType.METEOR_RAIN,         // Feu T9
            Talent.TalentEffectType.TIME_STASIS,         // Glace T9 - CDR CRITIQUE!
            Talent.TalentEffectType.DIVINE_JUDGMENT,     // Foudre T9
            Talent.TalentEffectType.IMMORTAL_ARMY,       // Âme T9
            Talent.TalentEffectType.BLACK_HOLE           // Vide T9
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forUltimateTalent(effect));
        }
    }

    // ==================== GÉNÉRATION ====================

    /**
     * Génère un éveil pour un talent spécifique
     *
     * @param talent Le talent cible
     * @param qualityBonus Bonus de qualité (0.0 à 0.3)
     * @return Un nouvel Awaken
     */
    public Awaken generateAwaken(Talent talent, double qualityBonus) {
        // Vérifier s'il y a un override pour ce talent
        AwakenTemplate template = overridesByTalent.get(talent.getId());

        if (template == null) {
            // Utiliser le template par type d'effet
            template = templatesByEffect.get(talent.getEffectType());
        }

        if (template == null) {
            // Fallback au template générique
            template = AwakenTemplate.forGenericTalent(talent.getEffectType());
        }

        return template.generate(talent, qualityBonus);
    }

    /**
     * Obtient un template pour un type d'effet
     */
    public AwakenTemplate getTemplate(Talent.TalentEffectType effectType) {
        return templatesByEffect.get(effectType);
    }

    /**
     * Enregistre un override pour un talent spécifique
     */
    public void registerOverride(String talentId, AwakenTemplate template) {
        overridesByTalent.put(talentId, template);
    }

    /**
     * Enregistre un template pour un type d'effet
     */
    public void registerTemplate(Talent.TalentEffectType effectType, AwakenTemplate template) {
        templatesByEffect.put(effectType, template);
    }

    /**
     * Met en cache un éveil généré
     */
    public void cacheAwaken(String awakenId, Awaken awaken) {
        cachedAwakens.put(awakenId, awaken);
    }

    /**
     * Récupère un éveil du cache
     */
    public Awaken getCachedAwaken(String awakenId) {
        return cachedAwakens.get(awakenId);
    }

    /**
     * Vérifie si un type d'effet a un template
     */
    public boolean hasTemplate(Talent.TalentEffectType effectType) {
        return templatesByEffect.containsKey(effectType);
    }

    /**
     * Obtient tous les types d'effets avec templates
     */
    public Set<Talent.TalentEffectType> getRegisteredEffectTypes() {
        return Collections.unmodifiableSet(templatesByEffect.keySet());
    }

    /**
     * Nombre de templates enregistrés
     */
    public int getTemplateCount() {
        return templatesByEffect.size();
    }

    /**
     * Nombre d'overrides enregistrés
     */
    public int getOverrideCount() {
        return overridesByTalent.size();
    }

    // ==================== TEMPLATES ARMURES ====================

    /**
     * Cache des templates d'armures par type
     */
    private final Map<ItemType, AwakenTemplate> armorTemplates = new EnumMap<>(ItemType.class);

    /**
     * Obtient le template approprié pour un type d'armure
     *
     * @param armorType Le type d'armure (HELMET, CHESTPLATE, LEGGINGS, BOOTS)
     * @return Le template pour ce type d'armure
     */
    public AwakenTemplate getArmorTemplate(ItemType armorType) {
        // Vérifier le cache
        if (armorTemplates.containsKey(armorType)) {
            return armorTemplates.get(armorType);
        }

        // Créer et mettre en cache le template approprié
        AwakenTemplate template = switch (armorType) {
            case HELMET -> AwakenTemplate.forHelmetArmor();
            case CHESTPLATE -> AwakenTemplate.forChestplateArmor();
            case LEGGINGS -> AwakenTemplate.forLeggingsArmor();
            case BOOTS -> AwakenTemplate.forBootsArmor();
            default -> AwakenTemplate.forGenericArmor();
        };

        armorTemplates.put(armorType, template);
        return template;
    }

    /**
     * Nombre de templates d'armures enregistrés
     */
    public int getArmorTemplateCount() {
        return armorTemplates.size();
    }
}
