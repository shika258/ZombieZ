package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.classes.talents.TalentTier;
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
     */
    private void registerDefaultTemplates() {
        // === TALENTS D'INVOCATION (BÊTES) ===
        registerSummonTemplates();

        // === TALENTS DE DÉGÂTS ===
        registerDamageTemplates();

        // === TALENTS DE PROJECTILES ===
        registerProjectileTemplates();

        // === TALENTS DE ZONE ===
        registerAoETemplates();

        // === TALENTS DE DOT ===
        registerDoTTemplates();

        // === TALENTS DE STACKS ===
        registerStackTemplates();

        // === TALENTS DE CONTRÔLE ===
        registerControlTemplates();

        // === TALENTS DÉFENSIFS ===
        registerDefensiveTemplates();

        // === TALENTS ULTIMES ===
        registerUltimateTemplates();
    }

    private void registerSummonTemplates() {
        // Bêtes du Chasseur
        for (var effect : List.of(
            Talent.TalentEffectType.BEAST_BAT,
            Talent.TalentEffectType.BEAST_ENDERMITE,
            Talent.TalentEffectType.BEAST_WOLF,
            Talent.TalentEffectType.BEAST_AXOLOTL,
            Talent.TalentEffectType.BEAST_COW,
            Talent.TalentEffectType.BEAST_LLAMA,
            Talent.TalentEffectType.BEAST_FOX,
            Talent.TalentEffectType.BEAST_BEE,
            Talent.TalentEffectType.BEAST_IRON_GOLEM
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forSummonTalent(effect));
        }

        // Clones et invocations
        templatesByEffect.put(Talent.TalentEffectType.SHADOW_CLONE, AwakenTemplate.forSummonTalent(Talent.TalentEffectType.SHADOW_CLONE));
        templatesByEffect.put(Talent.TalentEffectType.SHADOW_AVATAR, AwakenTemplate.forSummonTalent(Talent.TalentEffectType.SHADOW_AVATAR));
        templatesByEffect.put(Talent.TalentEffectType.NECROMANCER, AwakenTemplate.forSummonTalent(Talent.TalentEffectType.NECROMANCER));
        templatesByEffect.put(Talent.TalentEffectType.LORD_OF_THE_DEAD, AwakenTemplate.forSummonTalent(Talent.TalentEffectType.LORD_OF_THE_DEAD));
        templatesByEffect.put(Talent.TalentEffectType.IMMORTAL_ARMY, AwakenTemplate.forSummonTalent(Talent.TalentEffectType.IMMORTAL_ARMY));
    }

    private void registerDamageTemplates() {
        for (var effect : List.of(
            // Guerrier
            Talent.TalentEffectType.SEISMIC_STRIKE,
            Talent.TalentEffectType.RISING_FURY,
            Talent.TalentEffectType.LUNGING_STRIKE,
            Talent.TalentEffectType.BLOOD_FERVOUR,
            Talent.TalentEffectType.MERCY_STRIKE,
            Talent.TalentEffectType.WARRIOR_FRENZY,
            Talent.TalentEffectType.FURY_CONSUMPTION,
            // Chasseur
            Talent.TalentEffectType.SHADOW_SHOT,
            Talent.TalentEffectType.EXECUTION,
            Talent.TalentEffectType.SHADOW_STORM,
            // Occultiste
            Talent.TalentEffectType.IGNITE,
            Talent.TalentEffectType.CHAIN_LIGHTNING,
            Talent.TalentEffectType.OVERCHARGE,
            Talent.TalentEffectType.MJOLNIR,
            Talent.TalentEffectType.DIVINE_JUDGMENT
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forDamageTalent(effect));
        }
    }

    private void registerProjectileTemplates() {
        for (var effect : List.of(
            Talent.TalentEffectType.MULTI_SHOT,
            Talent.TalentEffectType.BURST_SHOT,
            Talent.TalentEffectType.PIERCING_ARROWS,
            Talent.TalentEffectType.CALIBER,
            Talent.TalentEffectType.CHAIN_PERFORATION,
            Talent.TalentEffectType.RICOCHET
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forProjectileTalent(effect));
        }
    }

    private void registerAoETemplates() {
        for (var effect : List.of(
            // Guerrier
            Talent.TalentEffectType.WAR_ECHO,
            Talent.TalentEffectType.FRACTURE_WAVE,
            Talent.TalentEffectType.SEISMIC_RESONANCE,
            Talent.TalentEffectType.CATACLYSM,
            Talent.TalentEffectType.SEISMIC_AFTERMATH,
            Talent.TalentEffectType.ETERNAL_TREMOR,
            Talent.TalentEffectType.EARTH_APOCALYPSE,
            Talent.TalentEffectType.RAGNAROK,
            Talent.TalentEffectType.RAGE_CYCLONE,
            Talent.TalentEffectType.BLOOD_CYCLONES,
            Talent.TalentEffectType.MEGA_TORNADO,
            // Chasseur
            Talent.TalentEffectType.ARROW_RAIN,
            Talent.TalentEffectType.DELUGE,
            Talent.TalentEffectType.STEEL_STORM,
            Talent.TalentEffectType.BARRAGE_FURY,
            Talent.TalentEffectType.CYCLONE_EYE,
            Talent.TalentEffectType.DEVASTATING_SWARM,
            Talent.TalentEffectType.ORBITAL_STRIKE,
            // Occultiste
            Talent.TalentEffectType.FIRESTORM,
            Talent.TalentEffectType.BLIZZARD,
            Talent.TalentEffectType.LIGHTNING_STORM,
            Talent.TalentEffectType.FIRE_AVATAR,
            Talent.TalentEffectType.INFERNO,
            Talent.TalentEffectType.BLACK_SUN,
            Talent.TalentEffectType.METEOR_RAIN,
            Talent.TalentEffectType.IMPLOSION,
            Talent.TalentEffectType.SINGULARITY,
            Talent.TalentEffectType.BLACK_HOLE
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forAoETalent(effect));
        }
    }

    private void registerDoTTemplates() {
        for (var effect : List.of(
            Talent.TalentEffectType.VENOMOUS_STRIKE,
            Talent.TalentEffectType.CORROSIVE_VENOM,
            Talent.TalentEffectType.DEADLY_TOXINS,
            Talent.TalentEffectType.PANDEMIC,
            Talent.TalentEffectType.EPIDEMIC,
            Talent.TalentEffectType.TOXIC_SYNERGY,
            Talent.TalentEffectType.BLACK_PLAGUE,
            Talent.TalentEffectType.BLIGHT,
            Talent.TalentEffectType.PLAGUE_AVATAR,
            Talent.TalentEffectType.LACERATING_CLAWS,
            Talent.TalentEffectType.FIRE_SPREAD,
            Talent.TalentEffectType.PYROCLASM,
            Talent.TalentEffectType.DEATH_AND_DECAY
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forDoTTalent(effect));
        }
    }

    private void registerStackTemplates() {
        for (var effect : List.of(
            Talent.TalentEffectType.SHADOW_BLADE,
            Talent.TalentEffectType.SHADOW_STEP,
            Talent.TalentEffectType.DEATH_MARK,
            Talent.TalentEffectType.DANSE_MACABRE,
            Talent.TalentEffectType.OVERHEAT,
            Talent.TalentEffectType.HUNTER_MOMENTUM,
            Talent.TalentEffectType.FATAL_TRAJECTORY,
            Talent.TalentEffectType.ABSOLUTE_PERFORATION,
            Talent.TalentEffectType.DEVASTATION,
            Talent.TalentEffectType.DEFENSIVE_STANCE,
            Talent.TalentEffectType.PUNISHMENT,
            Talent.TalentEffectType.FORTIFY,
            Talent.TalentEffectType.IRON_ECHO,
            Talent.TalentEffectType.FURIOUS_MOMENTUM,
            Talent.TalentEffectType.INSATIABLE_PREDATOR,
            Talent.TalentEffectType.WAR_FRENZY,
            Talent.TalentEffectType.SOUL_SIPHON,
            Talent.TalentEffectType.SOUL_RESERVOIR,
            Talent.TalentEffectType.SOUL_PACT,
            Talent.TalentEffectType.SOUL_LEGION,
            Talent.TalentEffectType.SOUL_BOND
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forStackTalent(effect));
        }
    }

    private void registerControlTemplates() {
        for (var effect : List.of(
            Talent.TalentEffectType.FROST_BITE,
            Talent.TalentEffectType.FROZEN_HEART,
            Talent.TalentEffectType.FROST_LORD,
            Talent.TalentEffectType.PERMAFROST,
            Talent.TalentEffectType.ICE_AGE,
            Talent.TalentEffectType.ETERNAL_WINTER,
            Talent.TalentEffectType.TIME_STASIS,
            Talent.TalentEffectType.JUDGMENT,
            Talent.TalentEffectType.ABSOLUTE_ZERO,
            Talent.TalentEffectType.DARK_GRAVITY,
            Talent.TalentEffectType.GRAVITY_WELL,
            Talent.TalentEffectType.DIMENSIONAL_RIFT,
            Talent.TalentEffectType.WAR_CRY_MARK
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forControlTalent(effect));
        }
    }

    private void registerDefensiveTemplates() {
        for (var effect : List.of(
            Talent.TalentEffectType.DEATH_STRIKE,
            Talent.TalentEffectType.BONE_SHIELD,
            Talent.TalentEffectType.MARROWREND,
            Talent.TalentEffectType.VAMPIRIC_WILL,
            Talent.TalentEffectType.CONSUMPTION,
            Talent.TalentEffectType.BLOOD_PACT,
            Talent.TalentEffectType.VAMPIRIC_HEART,
            Talent.TalentEffectType.DANCING_RUNE_WEAPON,
            Talent.TalentEffectType.VENGEFUL_SHIELD,
            Talent.TalentEffectType.JUDGMENT_HAMMER,
            Talent.TalentEffectType.BASTION_CHARGE,
            Talent.TalentEffectType.DEFIANCE_AURA,
            Talent.TalentEffectType.BULWARK_AVATAR,
            Talent.TalentEffectType.AGILE_HUNTER,
            Talent.TalentEffectType.CONDUCTOR,
            Talent.TalentEffectType.ETERNAL_HARVEST,
            Talent.TalentEffectType.PHOENIX_FLAME
        )) {
            templatesByEffect.put(effect, AwakenTemplate.forDefensiveTalent(effect));
        }
    }

    private void registerUltimateTemplates() {
        // Les talents de Tier 9 utilisent des templates ultimes avec des bonus plus forts
        for (var effect : List.of(
            Talent.TalentEffectType.RAGNAROK,
            Talent.TalentEffectType.BULWARK_AVATAR,
            Talent.TalentEffectType.MEGA_TORNADO,
            Talent.TalentEffectType.DANCING_RUNE_WEAPON,
            Talent.TalentEffectType.BERSERKER_RAGE,
            Talent.TalentEffectType.ORBITAL_STRIKE,
            Talent.TalentEffectType.BEAST_IRON_GOLEM,
            Talent.TalentEffectType.SHADOW_AVATAR,
            Talent.TalentEffectType.PLAGUE_AVATAR,
            Talent.TalentEffectType.JUDGMENT,
            Talent.TalentEffectType.METEOR_RAIN,
            Talent.TalentEffectType.TIME_STASIS,
            Talent.TalentEffectType.DIVINE_JUDGMENT,
            Talent.TalentEffectType.IMMORTAL_ARMY,
            Talent.TalentEffectType.BLACK_HOLE
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
}
