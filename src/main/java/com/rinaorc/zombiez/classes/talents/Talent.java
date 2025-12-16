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

        // === GUERRIER - Tier 6 ===
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
        LYNX_EYE,                // Crit chance + crit damage
        AGILE_HUNTER,            // Dodge gives invis + damage boost
        HUNTER_MARK,             // Mark enemies for extra damage
        PIERCING_ARROWS,         // Projectiles pierce enemies

        // === CHASSEUR - Tier 2 ===
        BURST_SHOT,              // Combo shot bonus
        SNIPER,                  // Distance = damage
        GHOST,                   // Invis bonus crit damage
        VENOM,                   // Poison DoT
        RICOCHET,                // Bouncing projectiles

        // === CHASSEUR - Tier 3 ===
        ARROW_RAIN,              // AoE arrow rain proc
        PREDATOR_EYE,            // Crit resets dodge cooldown
        TRACKER,                 // Enhanced mark
        DEADLY_TOXINS,           // Poison can crit + slow
        SHARPSHOOTER,            // Stand still = guaranteed crit

        // === CHASSEUR - Tier 4 ===
        DELUGE,                  // Arrow rain upgrade
        SUPREME_PREDATOR,        // Stealth chain kills
        DEATH_SENTENCE,          // Mark explosion on death
        PANDEMIC,                // Poison spreads on death
        OVERHEAT,                // Ramping damage

        // === CHASSEUR - Tier 5 ===
        STEEL_STORM,             // Auto arrow storm
        SPECTRE,                 // Attack from stealth
        BOUNTY_HUNTER,           // Mark kill rewards
        EPIDEMIC,                // Infinite poison stacking
        KILL_ZONE,               // Personal attack speed zone

        // === CHASSEUR - Tier 6 ===
        AERIAL_ARMAGEDDON,       // Arrow rain can crit
        SHADOW_MASTER,           // Permanent stealth
        BOUNTY_EXECUTIONER,      // Mark execute threshold
        BLACK_PLAGUE,            // Poison anti-heal + self heal
        GATLING,                 // Full auto mode

        // === CHASSEUR - Tier 7 ===
        METEOR_SHOWER,           // Arrows become meteors
        STEALTH_REAPER,          // Stealth execute
        LEGENDARY_HUNTER,        // Multi-mark permanent
        BLIGHT,                  // Passive contagion
        LIVING_ARSENAL,          // Auto-targeting shots

        // === CHASSEUR - Tier 8 ===
        ORBITAL_STRIKE,          // Nuke from orbit
        VOID_WALKER,             // Moving = invincible (EQUILIBRE: -60% DR instead)
        DEATH_NOTE,              // Delayed instakill
        TOXIC_APOCALYPSE,        // Passive poison aura
        BULLET_TIME,             // Time slow

        // === PLACEHOLDER pour autres classes ===
        PLACEHOLDER
    }
}
