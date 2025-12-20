package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;

/**
 * Represente un talent selectionnable par le joueur
 * Chaque talent a un effet passif unique qui change le gameplay
 */
@Getter
@Builder
public class Talent {

    // Identification
    private final String id;
    private final String name;
    private final String description;
    private final String[] loreLines;

    // Classification
    private final ClassType classType;
    private final TalentTier tier;
    private final int slotIndex; // 0-4 dans le tier

    // Visuel
    private final Material icon;
    private final String iconColor; // Couleur du nom

    // Effet
    private final TalentEffectType effectType;
    private final double[] values; // Valeurs configurables de l'effet

    // Cooldown interne (si applicable)
    @Builder.Default
    private final int internalCooldownMs = 500;

    /**
     * Obtient la description complete formatee pour le GUI
     */
    public String[] getFormattedLore() {
        if (loreLines == null || loreLines.length == 0) {
            return new String[]{description};
        }
        return loreLines;
    }

    /**
     * Obtient le nom colore
     */
    public String getColoredName() {
        return (iconColor != null ? iconColor : tier.getColor()) + name;
    }

    /**
     * Verifie si le talent est debloque pour un niveau donne
     */
    public boolean isUnlocked(int classLevel) {
        return tier.isUnlocked(classLevel);
    }

    /**
     * Obtient une valeur de configuration
     */
    public double getValue(int index) {
        if (values == null || index < 0 || index >= values.length) return 0;
        return values[index];
    }

    /**
     * Types d'effets de talents
     * Chaque type correspond a un comportement specifique dans TalentListener
     */
    public enum TalentEffectType {
        // === GUERRIER - Tier 1 ===
        SEISMIC_STRIKE,          // Onde de choc sur attaque
        BLOODTHIRST,             // Heal on kill
        RISING_FURY,             // Damage stack on hit
        IRON_SKIN,               // DR mais slow
        DEVASTATING_CHARGE,      // Bonus apres sprint

        // === GUERRIER - Tier 2 ===
        WAR_ECHO,                // Double AoE chance
        FRENETIC,                // Low HP = attack speed + lifesteal
        MACE_IMPACT,             // Knockback on crit
        BASTION,                 // Shield on block
        UNLEASH,                 // Explosion on multi-kill

        // === GUERRIER - Tier 3 ===
        BLADE_WHIRLWIND,         // Spin attack on hit
        WAR_VAMPIRE,             // Lifesteal passif
        ANCESTRAL_WRATH,         // Riposte damage boost
        IMMOVABLE_TITAN,         // No knockback, DR when still
        EXECUTIONER,             // Bonus damage to low HP

        // === GUERRIER - Tier 4 ===
        SEISMIC_RESONANCE,       // AoE leaves damage zone
        COMBAT_THRILL,           // Attack speed on lifesteal
        BURNING_VENGEANCE,       // Fire DoT on riposte
        FORTRESS,                // Shield explodes on expire
        BLOODY_HARVEST,          // Execute heal + sprint reset

        // === GUERRIER - Tier 5 ===
        CATACLYSM,               // Big AoE every X attacks
        IMMORTAL,                // Cheat death
        RAGE_CYCLONE,            // Sprint = spin damage
        ETERNAL_AEGIS,           // Perfect parry reflect
        WARLORD,                 // Chain execute

        // === GUERRIER - Tier 6 (Niveau 25 - Ascension) ===
        SEISMIC_AFTERMATH,       // AoE attacks leave lingering damage zones
        BLOOD_FRENZY,            // Attack speed per missing HP
        UNSTOPPABLE_RAGE,        // Damage immunity during rage buildup
        UNYIELDING_WALL,         // Damage reduction scales with stillness
        MOMENTUM,                // Sprint damage bonus stacks

        // === GUERRIER - Tier 7 ===
        ETERNAL_TREMOR,          // Passive AoE pulse
        BLOOD_AVATAR,            // Blood bomb on lifesteal cap
        INFINITE_RETALIATION,    // Riposte scaling
        IMPREGNABLE_BASTION,     // Double DR + regen
        REAPER,                  // Auto-kill low HP in range

        // === GUERRIER - Tier 7 ===
        EARTH_APOCALYPSE,        // Mega earthquake proc
        VAMPIRE_LORD,            // Overheal shield
        NEMESIS,                 // Passive thorns
        COLOSSUS,                // Giant form
        DEATH_ANGEL,             // Death aura

        // === GUERRIER - Tier 8 ===
        RAGNAROK,                // Auto mega nuke
        BLOOD_GOD,               // Reduced damage while attacking (EQUILIBRE)
        VENGEANCE_AVATAR,        // Stored damage release
        LIVING_CITADEL,          // Invuln then explode
        EXTINCTION,              // First hit mega damage

        // === CHASSEUR - Tier 1 ===
        MULTI_SHOT,              // Chance extra projectiles
        BEAST_BAT,               // Summon bat - attacks focus target, invincible
        AGILE_HUNTER,            // Dodge gives invis + damage boost
        HUNTER_MARK,             // Mark enemies for extra damage
        PIERCING_ARROWS,         // Projectiles pierce enemies
        LYNX_EYE,                // Crit chance + crit damage bonus
        PREDATOR_EYE,            // Crit resets dodge cooldown

        // === CHASSEUR - Tier 2 ===
        BURST_SHOT,              // Combo shot bonus
        BEAST_ENDERMITE,         // Summon endermite - void parasite, corruption, teleport explosion
        GHOST,                   // Invis bonus crit damage
        VENOM,                   // Poison DoT
        RICOCHET,                // Bouncing projectiles
        SNIPER,                  // Distance bonus damage

        // === CHASSEUR - Tier 3 ===
        ARROW_RAIN,              // AoE arrow rain proc
        BEAST_WOLF,              // Summon wolf - bleed DoT on bite
        TRACKER,                 // Enhanced mark
        DEADLY_TOXINS,           // Poison can crit + slow
        SHARPSHOOTER,            // Stand still = guaranteed crit

        // === CHASSEUR - Tier 4 ===
        DELUGE,                  // Arrow rain upgrade
        BEAST_AXOLOTL,           // Summon axolotl - ranged water bubble attacks
        DEATH_SENTENCE,          // Mark explosion on death
        PANDEMIC,                // Poison spreads on death
        OVERHEAT,                // Ramping damage

        // === CHASSEUR - Tier 5 ===
        STEEL_STORM,             // Auto arrow storm
        BEAST_COW,               // Summon cow - drops explosive mines
        EPIDEMIC,                // Infinite poison stacking
        KILL_ZONE,               // Personal attack speed zone

        // === CHASSEUR - BRANCHE OMBRE (Refonte Diablo-style) ===
        SHADOW_BLADE,            // T1: Attaques = +1 Point d'Ombre, 3+ = +30% AS
        INSIDIOUS_POISON,        // T2: Attaques empoisonnent (stack x5)
        SHADOW_STEP,             // T3: Shift+Attaque = téléport derrière + 2 Points
        DEATH_MARK,              // T4: Crits marquent 8s (+25% dégâts, Glowing)
        EXECUTION,               // T5: 5 Points sur marqué = 300%/500% dégâts
        DANSE_MACABRE,           // T6: Kill marqué = 2s invis + reset Pas + vitesse
        SHADOW_CLONE,            // T7: 5 Points = clone 10s (50% dégâts)
        SHADOW_STORM,            // T8: Exécution kill = AoE + marque tous
        SHADOW_AVATAR,           // T9: Ultime 15s, 2 clones, +1 Point/s

        // === CHASSEUR - BRANCHE POISON (Refonte dynamique) ===
        VENOMOUS_STRIKE,         // T1: 40% chance poison, 3+ stacks = Nécrose (+25%)
        CORROSIVE_VENOM,         // T2: 50% DoT + -10% armure ennemi
        // DEADLY_TOXINS,        // T3: déjà défini, poison crit + slow
        // PANDEMIC,             // T4: déjà défini, explosion chaîne à la mort
        // EPIDEMIC,             // T5: déjà défini, stacks infinis + explosion auto
        TOXIC_SYNERGY,           // T6: +5% AS par stack proche (max +40%), heal explosion
        // BLACK_PLAGUE,         // T7: déjà défini, anti-heal + lifesteal + nuages
        // BLIGHT,               // T8: déjà défini, aura + propagation + combo boost
        PLAGUE_AVATAR,           // T9: Ultime 20s, max stacks instant, x2 explosions

        // === CHASSEUR - BRANCHE PERFORATION (Refonte dynamique) ===
        // PIERCING_ARROWS,      // T1: déjà défini, maintenant traverse 2 + bonus dégâts
        CALIBER,                 // T2: Système de charge (1-5), 5 = Tir Lourd
        FATAL_TRAJECTORY,        // T3: Pierce 2+ = Ligne de Mort (zone +30% dégâts)
        // OVERHEAT,             // T4: déjà défini, +10%/tir, max = explosion
        ABSOLUTE_PERFORATION,    // T5: -20% armure/pierce (max -80%), expose à -80%
        HUNTER_MOMENTUM,         // T6: Kill surchauffé = vitesse, 3 kills = Frénésie
        CHAIN_PERFORATION,       // T7: Après dernier pierce, rebondit 3x
        DEVASTATION,             // T8: Mode 8s: pierce infini, +60% dégâts, slow
        JUDGMENT,                // T9: Ultime - Rayon 50 blocs, 1000% dégâts

        // === Legacy (compatibilité - ne plus utiliser) ===
        SPECTRE,                 // (Legacy)
        SHADOW_MASTER,           // (Legacy)
        STEALTH_REAPER,          // (Legacy)
        VOID_WALKER,             // (Legacy)
        SUPREME_PREDATOR,        // (Legacy)
        BOUNTY_HUNTER,           // (Legacy)
        BOUNTY_EXECUTIONER,      // (Legacy)
        LEGENDARY_HUNTER,        // (Legacy)
        GHOST,                   // (Legacy)
        TRACKER,                 // (Legacy)
        DEATH_SENTENCE,          // (Legacy)
        SHADOW_STRIKE,           // (Legacy)
        DEATH_NOTE,              // (Legacy)
        AGILE_HUNTER,            // (Legacy)
        HUNTER_MARK,             // (Legacy - remplacé par VENOMOUS_STRIKE)
        VENOM,                   // (Legacy - remplacé par CORROSIVE_VENOM)
        PREY_WEAKNESS,           // (Legacy - remplacé par TOXIC_SYNERGY)
        TOXIC_APOCALYPSE,        // (Legacy - remplacé par PLAGUE_AVATAR)
        RICOCHET,                // (Legacy - remplacé par CALIBER)
        SHARPSHOOTER,            // (Legacy - remplacé par FATAL_TRAJECTORY)
        KILL_ZONE,               // (Legacy - remplacé par ABSOLUTE_PERFORATION)
        ARMOR_SHRED,             // (Legacy - remplacé par HUNTER_MOMENTUM)
        GATLING,                 // (Legacy - remplacé par CHAIN_PERFORATION)
        LIVING_ARSENAL,          // (Legacy - remplacé par DEVASTATION)
        BULLET_TIME,             // (Legacy - remplacé par JUDGMENT)

        // === CHASSEUR - Tier 6 (Niveau 25 - Ascension) ===
        BARRAGE_FURY,            // Arrow rain kills charge meter -> SUPER rain
        BEAST_LLAMA,             // Summon llama - spits on 3 targets, applies slowness
        PREY_WEAKNESS,           // Marked enemies take more crit damage
        ARMOR_SHRED,             // Piercing attacks reduce enemy armor

        // === CHASSEUR - Tier 7 ===
        CYCLONE_EYE,             // Arrow rain creates vortex that pulls + explodes
        BEAST_FOX,               // Summon fox - treasure hunter, gives stat buffs on kill
        BLACK_PLAGUE,            // Poison anti-heal + self heal
        GATLING,                 // Full auto mode

        // === CHASSEUR - Tier 8 ===
        DEVASTATING_SWARM,       // Doubles all rain radius + arrows fragment on impact
        BEAST_BEE,               // Summon bee - double-sneak frenzy, +50% attack speed for all beasts
        BLIGHT,                  // Passive contagion
        LIVING_ARSENAL,          // Auto-targeting shots

        // === CHASSEUR - Tier 9 (Legendary) ===
        ORBITAL_STRIKE,          // Nuke from orbit
        BEAST_IRON_GOLEM,        // Summon iron golem - shockwave vortex + explosion
        TOXIC_APOCALYPSE,        // Passive poison aura
        BULLET_TIME,             // Time slow

        // === OCCULTISTE - Tier 1 ===
        IGNITE,                  // Fire DoT on attack
        FROST_BITE,              // Slow on attack
        CHAIN_LIGHTNING,         // Chain lightning proc
        SOUL_SIPHON,             // Heal on kill + soul orbs
        SHADOW_WORD,             // Shadow DoT + Insanity generation

        // === OCCULTISTE - Tier 2 ===
        FIRE_SPREAD,             // Fire spreads to nearby enemies
        FROZEN_HEART,            // Frozen enemies take more damage + shatter
        OVERCHARGE,              // Lightning can crit + bonus target
        SOUL_RESERVOIR,          // Consume souls for burst damage
        VAMPIRIC_TOUCH,          // Second DoT + heal on damage

        // === OCCULTISTE - Tier 3 ===
        FIRESTORM,               // Meteor proc on attack
        BLIZZARD,                // Frozen enemies create freeze aura
        LIGHTNING_STORM,         // Passive lightning around you
        SOUL_PACT,               // Damage per soul orb
        SHADOWY_APPARITIONS,     // Phantasms attack DoT targets

        // === OCCULTISTE - Tier 4 ===
        PHOENIX_FLAME,           // Fire kills can explode
        ABSOLUTE_ZERO,           // Deep freeze execute
        CONDUCTOR,               // Lightning heals you
        ETERNAL_HARVEST,         // Soul orbs regen HP
        DARK_GRAVITY,            // Attacks slow enemies + bonus damage

        // === OCCULTISTE - Tier 5 ===
        FIRE_AVATAR,             // Fire aura around you
        FROST_LORD,              // 100% freeze chance, double duration
        THUNDER_GOD,             // Unlimited chain targets
        SOUL_LEGION,             // 10 orbs + DR per orb
        IMPLOSION,               // Pull all enemies to a point

        // === OCCULTISTE - Tier 6 (Niveau 25 - Ascension) ===
        PYROCLASM,               // Fire kills explode in chain reaction
        PERMAFROST,              // Frozen enemies slow nearby enemies
        STATIC_FIELD,            // Lightning creates damaging field
        SOUL_BOND,               // Soul orbs share damage taken
        GRAVITY_WELL,            // Place persistent gravity zone

        // === OCCULTISTE - Tier 7 ===
        INFERNO,                 // Periodic fire nova
        ICE_AGE,                 // Frost zones on kill
        PERPETUAL_STORM,         // Lightning storm aura
        NECROMANCER,             // Summon skeletons from souls
        SINGULARITY,             // Create violent pulling singularity

        // === OCCULTISTE - Tier 8 ===
        BLACK_SUN,               // Summon damaging fire sun
        ETERNAL_WINTER,          // Permanent slow aura + damage bonus
        MJOLNIR,                 // Triple strike lightning
        LORD_OF_THE_DEAD,        // Raise killed enemies as minions
        DIMENSIONAL_RIFT,        // Banish enemies to void dimension

        // === OCCULTISTE - Tier 9 ===
        METEOR_RAIN,             // Periodic massive meteor shower
        TIME_STASIS,             // Freeze all enemies in time
        DIVINE_JUDGMENT,         // Screen-wide lightning strike
        IMMORTAL_ARMY,           // Minions respawn + buffed
        BLACK_HOLE               // Ultimate: massive black hole
    }
}
