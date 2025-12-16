package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registre de tous les talents du Guerrier
 * 40 talents au total, 5 par palier sur 8 paliers
 *
 * Valeurs dans le tableau values[]:
 * - Index 0: Valeur principale (pourcentage, degats, etc.)
 * - Index 1: Valeur secondaire (rayon, duree, etc.)
 * - Index 2+: Valeurs additionnelles specifiques au talent
 */
public final class GuerrierTalents {

    private static final List<Talent> TALENTS = new ArrayList<>();

    static {
        registerTier1Talents();
        registerTier2Talents();
        registerTier3Talents();
        registerTier4Talents();
        registerTier5Talents();
        registerTier6Talents();
        registerTier7Talents();
        registerTier8Talents();
    }

    // ==================== PALIER 1 - NIVEAU 0 (Fondation) ====================

    private static void registerTier1Talents() {
        // 1.1 - FRAPPE SISMIQUE
        TALENTS.add(Talent.builder()
            .id("guerrier_seismic_strike")
            .name("Frappe Sismique")
            .description("15% de chance de creer une onde de choc")
            .loreLines(new String[]{
                "§7Vos attaques ont §e15%§7 de chance",
                "§7de creer une onde de choc.",
                "",
                "§8Degats: §c80%§8 des degats de base",
                "§8Rayon: §e3§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.COBBLESTONE)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SEISMIC_STRIKE)
            .values(new double[]{0.15, 0.80, 3.0}) // chance, damage%, radius
            .internalCooldownMs(800)
            .build());

        // 1.2 - SOIF DE SANG
        TALENTS.add(Talent.builder()
            .id("guerrier_bloodthirst")
            .name("Soif de Sang")
            .description("Chaque kill soigne 5% PV max")
            .loreLines(new String[]{
                "§7Chaque elimination vous soigne",
                "§7de §c5%§7 de vos PV max.",
                "",
                "§8Effet instantane"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.REDSTONE)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BLOODTHIRST)
            .values(new double[]{0.05}) // heal%
            .build());

        // 1.3 - FUREUR CROISSANTE
        TALENTS.add(Talent.builder()
            .id("guerrier_rising_fury")
            .name("Fureur Croissante")
            .description("+2% degats par coup, max 20%")
            .loreLines(new String[]{
                "§7Chaque coup augmente vos degats",
                "§7de §c+2%§7 (max §c20%§7).",
                "",
                "§8Reset apres 3s sans attaquer"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(2)
            .icon(Material.BLAZE_POWDER)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.RISING_FURY)
            .values(new double[]{0.02, 0.20, 3000}) // stack%, max%, reset_ms
            .build());

        // 1.4 - PEAU DE FER
        TALENTS.add(Talent.builder()
            .id("guerrier_iron_skin")
            .name("Peau de Fer")
            .description("-15% degats recus, -10% vitesse")
            .loreLines(new String[]{
                "§7Les degats recus sont reduits",
                "§7de §a15%§7 mais vous etes",
                "§710% plus lent.",
                "",
                "§8Trade-off survie vs mobilite"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.IRON_CHESTPLATE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.IRON_SKIN)
            .values(new double[]{0.15, 0.10}) // DR%, slow%
            .build());

        // 1.5 - CHARGE DEVASTATRICE
        TALENTS.add(Talent.builder()
            .id("guerrier_devastating_charge")
            .name("Charge Devastatrice")
            .description("Sprint 1.5s puis +200% degats + stun")
            .loreLines(new String[]{
                "§7Sprinter pendant §e1.5s§7 puis",
                "§7frapper inflige §c+200%§7 degats",
                "§7et §eētourdit§7 la cible 0.5s.",
                "",
                "§8Cooldown: 5s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.LEATHER_BOOTS)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.DEVASTATING_CHARGE)
            .values(new double[]{1500, 2.0, 500, 5000}) // sprint_ms, damage_mult, stun_ms, cooldown_ms
            .internalCooldownMs(5000)
            .build());
    }

    // ==================== PALIER 2 - NIVEAU 5 (Amplification) ====================

    private static void registerTier2Talents() {
        // 2.1 - ECHO DE GUERRE
        TALENTS.add(Talent.builder()
            .id("guerrier_war_echo")
            .name("Echo de Guerre")
            .description("30% chance AoE double")
            .loreLines(new String[]{
                "§7Vos AoE ont §e30%§7 de chance",
                "§7de se repeter une seconde fois.",
                "",
                "§8Delay: 0.3s entre les deux"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.ECHO_SHARD)
            .iconColor("§9")
            .effectType(Talent.TalentEffectType.WAR_ECHO)
            .values(new double[]{0.30, 300}) // chance, delay_ms
            .build());

        // 2.2 - FRENETIQUE
        TALENTS.add(Talent.builder()
            .id("guerrier_frenetic")
            .name("Frenetique")
            .description("Sous 40% PV: +40% AS, +10% lifesteal")
            .loreLines(new String[]{
                "§7Sous §c40%§7 PV:",
                "§7- §e+40%§7 vitesse d'attaque",
                "§7- §c+10%§7 vol de vie",
                "",
                "§8Mode berserk!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.FERMENTED_SPIDER_EYE)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FRENETIC)
            .values(new double[]{0.40, 0.40, 0.10}) // hp_threshold, attack_speed_bonus, lifesteal
            .build());

        // 2.3 - MASSE D'ARMES
        TALENTS.add(Talent.builder()
            .id("guerrier_mace_impact")
            .name("Masse d'Armes")
            .description("Crits = knockback 3 blocs")
            .loreLines(new String[]{
                "§7Les coups critiques projettent",
                "§7les ennemis en arriere.",
                "",
                "§8Distance: §e3§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.MACE)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.MACE_IMPACT)
            .values(new double[]{3.0}) // knockback_blocks
            .build());

        // 2.4 - BASTION
        TALENTS.add(Talent.builder()
            .id("guerrier_bastion")
            .name("Bastion")
            .description("Bloquer = bouclier 20% PV 3s")
            .loreLines(new String[]{
                "§7Bloquer une attaque vous donne",
                "§7un bouclier de §e20%§7 PV max",
                "§7pendant §a3s§7.",
                "",
                "§8Cooldown: 8s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.SHIELD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BASTION)
            .values(new double[]{0.20, 3000, 8000}) // shield%, duration_ms, cooldown_ms
            .internalCooldownMs(8000)
            .build());

        // 2.5 - DECHAÎNEMENT
        TALENTS.add(Talent.builder()
            .id("guerrier_unleash")
            .name("Dechainement")
            .description("3 kills en 5s = explosion AoE")
            .loreLines(new String[]{
                "§7Tuer §e3§7 ennemis en §a5s§7",
                "§7declenche une explosion!",
                "",
                "§8Degats: §c150%§8 base",
                "§8Rayon: §e4§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.TNT)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.UNLEASH)
            .values(new double[]{3, 5000, 1.50, 4.0}) // kills_needed, window_ms, damage%, radius
            .build());
    }

    // ==================== PALIER 3 - NIVEAU 10 (Specialisation) ====================

    private static void registerTier3Talents() {
        // 3.1 - TOURBILLON DE LAMES
        TALENTS.add(Talent.builder()
            .id("guerrier_blade_whirlwind")
            .name("Tourbillon de Lames")
            .description("25% chance spin attack AoE")
            .loreLines(new String[]{
                "§7§e25%§7 de chance sur attaque",
                "§7de tournoyer, frappant tout",
                "§7autour de vous.",
                "",
                "§8Degats: §c120%§8 base",
                "§8Rayon: §e2.5§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.IRON_SWORD)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.BLADE_WHIRLWIND)
            .values(new double[]{0.25, 1.20, 2.5}) // chance, damage%, radius
            .internalCooldownMs(600)
            .build());

        // 3.2 - VAMPIRE DE GUERRE
        TALENTS.add(Talent.builder()
            .id("guerrier_war_vampire")
            .name("Vampire de Guerre")
            .description("10% lifesteal, 20% sous 30% PV")
            .loreLines(new String[]{
                "§7§c10%§7 des degats infliges",
                "§7sont convertis en PV.",
                "",
                "§7Double (§c20%§7) sous §c30%§7 PV!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.GHAST_TEAR)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.WAR_VAMPIRE)
            .values(new double[]{0.10, 0.20, 0.30}) // lifesteal_base, lifesteal_boosted, hp_threshold
            .build());

        // 3.3 - COLERE DES ANCETRES
        TALENTS.add(Talent.builder()
            .id("guerrier_ancestral_wrath")
            .name("Colere des Ancetres")
            .description("Apres degats recus: +100% next hit")
            .loreLines(new String[]{
                "§7Apres avoir recu des degats,",
                "§7votre prochaine attaque dans §a2s§7",
                "§7inflige §c+100%§7 degats!",
                "",
                "§8Riposte puissante"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.GOLDEN_SWORD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.ANCESTRAL_WRATH)
            .values(new double[]{2000, 1.0}) // window_ms, bonus_damage%
            .build());

        // 3.4 - TITAN IMMUABLE
        TALENTS.add(Talent.builder()
            .id("guerrier_immovable_titan")
            .name("Titan Immuable")
            .description("No knockback/stun, -20% DR immobile")
            .loreLines(new String[]{
                "§7Immunite au knockback et stun.",
                "",
                "§7Si immobile depuis §a1s§7:",
                "§7§a-20%§7 degats recus",
                "",
                "§8Tank indeplacable"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.ANVIL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.IMMOVABLE_TITAN)
            .values(new double[]{1000, 0.20}) // still_time_ms, DR_bonus
            .build());

        // 3.5 - EXECUTEUR
        TALENTS.add(Talent.builder()
            .id("guerrier_executioner")
            .name("Executeur")
            .description("Ennemis <25% PV: +50% degats")
            .loreLines(new String[]{
                "§7Les ennemis sous §c25%§7 PV",
                "§7prennent §c+50%§7 degats de vous.",
                "",
                "§8Finisher brutal!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.NETHERITE_AXE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.EXECUTIONER)
            .values(new double[]{0.25, 0.50}) // hp_threshold, bonus_damage%
            .build());
    }

    // ==================== PALIER 4 - NIVEAU 15 (Evolution) ====================

    private static void registerTier4Talents() {
        // 4.1 - RESONANCE SISMIQUE
        TALENTS.add(Talent.builder()
            .id("guerrier_seismic_resonance")
            .name("Resonance Sismique")
            .description("AoE laissent zone de degats 3s")
            .loreLines(new String[]{
                "§7Vos AoE laissent une zone de",
                "§7fracture pendant §a3s§7 qui",
                "§7inflige des degats.",
                "",
                "§8Degats: §c30%§8/s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.CRACKED_STONE_BRICKS)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SEISMIC_RESONANCE)
            .values(new double[]{3000, 0.30}) // duration_ms, damage%_per_second
            .build());

        // 4.2 - FRISSON DU COMBAT
        TALENTS.add(Talent.builder()
            .id("guerrier_combat_thrill")
            .name("Frisson du Combat")
            .description("Lifesteal = +50% AS temporaire")
            .loreLines(new String[]{
                "§7Chaque HP vole genere §a0.5s§7",
                "§7de §e+50%§7 vitesse d'attaque.",
                "",
                "§8Cap: 3s de duree"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.COMBAT_THRILL)
            .values(new double[]{500, 0.50, 3000}) // duration_per_hp_ms, AS_bonus%, max_duration_ms
            .build());

        // 4.3 - VENGEANCE ARDENTE
        TALENTS.add(Talent.builder()
            .id("guerrier_burning_vengeance")
            .name("Vengeance Ardente")
            .description("Riposte = 3 prochaines attaques brulent")
            .loreLines(new String[]{
                "§7Apres une riposte, les §e3§7",
                "§7prochaines attaques brulent",
                "§7les ennemis.",
                "",
                "§8Burn: §c40%§8 sur 2s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BURNING_VENGEANCE)
            .values(new double[]{3, 0.40, 2000}) // stacks, burn_damage%, burn_duration_ms
            .build());

        // 4.4 - FORTERESSE
        TALENTS.add(Talent.builder()
            .id("guerrier_fortress")
            .name("Forteresse")
            .description("Bouclier expire = explosion 200%")
            .loreLines(new String[]{
                "§7Quand votre bouclier temporaire",
                "§7expire, il explose!",
                "",
                "§8Degats: §c200%§8 du bouclier restant",
                "§8Rayon: §e3§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.GOLDEN_CHESTPLATE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.FORTRESS)
            .values(new double[]{2.0, 3.0}) // damage_multiplier, radius
            .build());

        // 4.5 - MOISSON SANGLANTE
        TALENTS.add(Talent.builder()
            .id("guerrier_bloody_harvest")
            .name("Moisson Sanglante")
            .description("Execute = +15% PV + reset sprint")
            .loreLines(new String[]{
                "§7Les executions (kill <25% PV)",
                "§7restaurent §c15%§7 PV max",
                "§7et reset le sprint.",
                "",
                "§8Chain kills!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.SWEET_BERRIES)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BLOODY_HARVEST)
            .values(new double[]{0.25, 0.15}) // execute_threshold, heal%
            .build());
    }

    // ==================== PALIER 5 - NIVEAU 20 (Maitrise) ====================

    private static void registerTier5Talents() {
        // 5.1 - CATACLYSME
        TALENTS.add(Talent.builder()
            .id("guerrier_cataclysm")
            .name("Cataclysme")
            .description("Toutes les 10 attaques: mega AoE")
            .loreLines(new String[]{
                "§7Toutes les §e10§7 attaques,",
                "§7declenche une mega AoE!",
                "",
                "§8Degats: §c250%§8 base",
                "§8Rayon: §e5§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.NETHER_STAR)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.CATACLYSM)
            .values(new double[]{10, 2.50, 5.0}) // attacks_needed, damage%, radius
            .build());

        // 5.2 - IMMORTEL
        TALENTS.add(Talent.builder()
            .id("guerrier_immortal")
            .name("Immortel")
            .description("1x/min: survie a 1 PV + invuln 2s")
            .loreLines(new String[]{
                "§7Une fois par minute, si vous",
                "§7devez mourir, restez a §c1 PV§7",
                "§7et devenez §einvincible 2s§7.",
                "",
                "§8Cooldown: 60s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.TOTEM_OF_UNDYING)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.IMMORTAL)
            .values(new double[]{60000, 2000}) // cooldown_ms, invuln_duration_ms
            .internalCooldownMs(60000)
            .build());

        // 5.3 - CYCLONE DE RAGE
        TALENTS.add(Talent.builder()
            .id("guerrier_rage_cyclone")
            .name("Cyclone de Rage")
            .description("Sprint = degats continus autour")
            .loreLines(new String[]{
                "§7Sprinter vous fait tournoyer,",
                "§7infligeant des degats continus.",
                "",
                "§8Degats: §c60%§8 / 0.5s",
                "§8Rayon: §e2§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.WIND_CHARGE)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.RAGE_CYCLONE)
            .values(new double[]{500, 0.60, 2.0}) // tick_ms, damage%, radius
            .build());

        // 5.4 - AEGIS ETERNAL
        TALENTS.add(Talent.builder()
            .id("guerrier_eternal_aegis")
            .name("Aegis Eternal")
            .description("Parade parfaite = 100% reflect")
            .loreLines(new String[]{
                "§7Chaque bloc parfait (timing 0.3s)",
                "§7reflete §c100%§7 des degats!",
                "",
                "§8Skill expression maximale"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.DIAMOND_CHESTPLATE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ETERNAL_AEGIS)
            .values(new double[]{300, 1.0}) // window_ms, reflect%
            .build());

        // 5.5 - SEIGNEUR DE GUERRE
        TALENTS.add(Talent.builder()
            .id("guerrier_warlord")
            .name("Seigneur de Guerre")
            .description("Execute chain: next <25% = instakill")
            .loreLines(new String[]{
                "§7Les kills sur ennemis §c<25%§7 PV",
                "§7rendent la prochaine attaque",
                "§7§cinstakill§7 sur ennemi <25% PV!",
                "",
                "§8Chain execute!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.NETHERITE_SWORD)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.WARLORD)
            .values(new double[]{0.25, 5000}) // threshold, buff_duration_ms
            .build());
    }

    // ==================== PALIER 6 - NIVEAU 30 (Transcendance) ====================

    private static void registerTier6Talents() {
        // 6.1 - TREMOR ETERNAL
        TALENTS.add(Talent.builder()
            .id("guerrier_eternal_tremor")
            .name("Tremor Eternal")
            .description("Ondes sismiques passives toutes 2s")
            .loreLines(new String[]{
                "§7En combat, vous generez des",
                "§7ondes sismiques toutes les §a2s§7.",
                "",
                "§8Degats: §c50%§8 base",
                "§8Rayon: §e3§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.DEEPSLATE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.ETERNAL_TREMOR)
            .values(new double[]{2000, 0.50, 3.0}) // interval_ms, damage%, radius
            .build());

        // 6.2 - AVATAR DE SANG
        TALENTS.add(Talent.builder()
            .id("guerrier_blood_avatar")
            .name("Avatar de Sang")
            .description("100 HP voles = explosion massive")
            .loreLines(new String[]{
                "§7Apres avoir vole §c100 HP§7,",
                "§7explosez pour des degats massifs!",
                "",
                "§8Degats: §c400%§8 base",
                "§8Rayon: §e4§8 blocs",
                "§8Self-heal: §c30%§8 PV max"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.BLOOD_AVATAR)
            .values(new double[]{100, 4.0, 4.0, 0.30}) // hp_to_trigger, damage%, radius, self_heal%
            .build());

        // 6.3 - REPRESAILLES INFINIES
        TALENTS.add(Talent.builder()
            .id("guerrier_infinite_retaliation")
            .name("Represailles Infinies")
            .description("Ripostes = +25% degats (max 200%)")
            .loreLines(new String[]{
                "§7Chaque riposte augmente les",
                "§7degats de riposte de §c+25%§7.",
                "",
                "§8Max: §c+200%§8 (300% total)",
                "§8Reset apres 10s sans riposte"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.SPECTRAL_ARROW)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.INFINITE_RETALIATION)
            .values(new double[]{0.25, 2.0, 10000}) // stack%, max_bonus%, reset_ms
            .build());

        // 6.4 - BASTILLE IMPRENABLE
        TALENTS.add(Talent.builder()
            .id("guerrier_impregnable_bastion")
            .name("Bastille Imprenable")
            .description("DR x2, +2% HP/s en combat")
            .loreLines(new String[]{
                "§7Votre reduction de degats",
                "§7est §edoublee§7 et vous regenerez",
                "§7§c2%§7 HP/s en combat.",
                "",
                "§8Immortalite passive"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.NETHERITE_CHESTPLATE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.IMPREGNABLE_BASTION)
            .values(new double[]{2.0, 0.02}) // DR_multiplier, regen%_per_second
            .build());

        // 6.5 - FAUCHEUR
        TALENTS.add(Talent.builder()
            .id("guerrier_reaper")
            .name("Faucheur")
            .description("Ennemis touches <15% PV = mort auto")
            .loreLines(new String[]{
                "§7Les ennemis que vous avez",
                "§7endommages meurent automatiquement",
                "§7sous §c15%§7 PV.",
                "",
                "§8Execute passif"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.REAPER)
            .values(new double[]{0.15}) // threshold
            .build());
    }

    // ==================== PALIER 7 - NIVEAU 40 (Apex) ====================

    private static void registerTier7Talents() {
        // 7.1 - APOCALYPSE TERRESTRE
        TALENTS.add(Talent.builder()
            .id("guerrier_earth_apocalypse")
            .name("Apocalypse Terrestre")
            .description("10% chance seisme geant sur AoE")
            .loreLines(new String[]{
                "§7Vos AoE ont §e10%§7 de chance",
                "§7de declencher un seisme geant!",
                "",
                "§8Degats: §c500%§8 base",
                "§8Rayon: §e8§8 blocs",
                "§8Stun: §e1s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.BEDROCK)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.EARTH_APOCALYPSE)
            .values(new double[]{0.10, 5.0, 8.0, 1000}) // chance, damage%, radius, stun_ms
            .internalCooldownMs(3000)
            .build());

        // 7.2 - SEIGNEUR VAMPIRE
        TALENTS.add(Talent.builder()
            .id("guerrier_vampire_lord")
            .name("Seigneur Vampire")
            .description("Lifesteal = bouclier de sang (50% max)")
            .loreLines(new String[]{
                "§7Le lifesteal peut depasser",
                "§7vos PV max, creant un",
                "§7§cbouclier de sang§7.",
                "",
                "§8Max: §c50%§8 PV max",
                "§8Decay: §8-5%/s hors combat"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.REDSTONE_BLOCK)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.VAMPIRE_LORD)
            .values(new double[]{0.50, 0.05}) // max_shield%, decay%_per_second
            .build());

        // 7.3 - NEMESIS
        TALENTS.add(Talent.builder()
            .id("guerrier_nemesis")
            .name("Nemesis")
            .description("Thorns passif: 75% degats renvoyes")
            .loreLines(new String[]{
                "§7Chaque ennemi qui vous touche",
                "§7prend §c75%§7 des degats renvoyes.",
                "",
                "§8Thorns automatique"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.CACTUS)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.NEMESIS)
            .values(new double[]{0.75}) // reflect%
            .build());

        // 7.4 - COLOSSE
        TALENTS.add(Talent.builder()
            .id("guerrier_colossus")
            .name("Colosse")
            .description("+50% PV, +30% melee, -25% speed")
            .loreLines(new String[]{
                "§7Forme de geant permanent:",
                "§7- §a+50%§7 PV max",
                "§7- §c+30%§7 degats melee",
                "§7- §c-25%§7 vitesse",
                "",
                "§8Taille +20%"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.IRON_BLOCK)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.COLOSSUS)
            .values(new double[]{0.50, 0.30, 0.25, 0.20}) // hp_bonus%, damage_bonus%, speed_malus%, size_bonus%
            .build());

        // 7.5 - ANGE DE LA MORT
        TALENTS.add(Talent.builder()
            .id("guerrier_death_angel")
            .name("Ange de la Mort")
            .description("Aura: <30% PV = 5% instakill/tick")
            .loreLines(new String[]{
                "§7Aura de mort autour de vous:",
                "§7Les ennemis §c<30%§7 PV ont",
                "§7§c5%§7 chance de mourir par tick.",
                "",
                "§8Rayon: §e5§8 blocs",
                "§8Tick: §80.5s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.WITHER_ROSE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.DEATH_ANGEL)
            .values(new double[]{5.0, 0.30, 0.05, 500}) // radius, hp_threshold, kill_chance, tick_ms
            .build());
    }

    // ==================== PALIER 8 - NIVEAU 50 (Legendaire) ====================

    private static void registerTier8Talents() {
        // 8.1 - RAGNAROK
        TALENTS.add(Talent.builder()
            .id("guerrier_ragnarok")
            .name("Ragnarok")
            .description("Toutes 30s: apocalypse sismique massive")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Toutes les §e30s§7, declenche",
                "§7une apocalypse sismique!",
                "",
                "§8Degats: §c800%§8 base",
                "§8Rayon: §e12§8 blocs",
                "§8Stun: §e2s§8 + knockback"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.END_CRYSTAL)
            .iconColor("§6§l")
            .effectType(Talent.TalentEffectType.RAGNAROK)
            .values(new double[]{30000, 8.0, 12.0, 2000}) // cooldown_ms, damage%, radius, stun_ms
            .internalCooldownMs(30000)
            .build());

        // 8.2 - DIEU DU SANG (EQUILIBRE - pas d'immortalite totale!)
        TALENTS.add(Talent.builder()
            .id("guerrier_blood_god")
            .name("Dieu du Sang")
            .description("-70% degats et +8% regen en attaquant")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Tant que vous infligez des degats:",
                "§7- §a-70%§7 degats recus",
                "§7- §c+8%§7 HP/s regeneration",
                "",
                "§8Condition: Attaque dans les 2s",
                "§8Note: Vous pouvez toujours mourir!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.DRAGON_EGG)
            .iconColor("§4§l")
            .effectType(Talent.TalentEffectType.BLOOD_GOD)
            // BALANCE: -70% DR et +8% regen au lieu d'immortalite
            .values(new double[]{2000, 0.70, 0.08}) // window_ms, DR%, regen%_per_second
            .build());

        // 8.3 - AVATAR DE VENGEANCE
        TALENTS.add(Talent.builder()
            .id("guerrier_vengeance_avatar")
            .name("Avatar de Vengeance")
            .description("Stocke degats recus, liberation massive")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Les degats recus sont stockes",
                "§7(cap §c500%§7 PV max).",
                "",
                "§8Crouch + Attack = Explosion",
                "§8Degats: §c100%§8 du stocke",
                "§8Rayon: §e6§8 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.NETHERITE_BLOCK)
            .iconColor("§5§l")
            .effectType(Talent.TalentEffectType.VENGEANCE_AVATAR)
            .values(new double[]{5.0, 1.0, 6.0}) // max_stored_multiplier, release_damage%, radius
            .build());

        // 8.4 - CITADELLE VIVANTE
        TALENTS.add(Talent.builder()
            .id("guerrier_living_citadel")
            .name("Citadelle Vivante")
            .description("3s invuln sans attaque, puis explosion")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Activation (shift): 3s d'invuln",
                "§7totale (pas d'attaque possible),",
                "§7puis explosion massive.",
                "",
                "§8Degats: §c300%§8 base",
                "§8Rayon: §e5§8 blocs",
                "§8Cooldown: 20s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.BEACON)
            .iconColor("§b§l")
            .effectType(Talent.TalentEffectType.LIVING_CITADEL)
            .values(new double[]{3000, 3.0, 5.0, 20000}) // invuln_ms, damage%, radius, cooldown_ms
            .internalCooldownMs(20000)
            .build());

        // 8.5 - EXTINCTION
        TALENTS.add(Talent.builder()
            .id("guerrier_extinction")
            .name("Extinction")
            .description("1ere attaque du combat = instakill")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Premiere attaque apres §e10s§7",
                "§7sans combat = §cinstakill§7.",
                "",
                "§8Boss/Elites: §c-30%§8 HP direct",
                "§8Animation divine!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.LIGHTNING_ROD)
            .iconColor("§e§l")
            .effectType(Talent.TalentEffectType.EXTINCTION)
            .values(new double[]{10000, 0.30}) // out_of_combat_ms, boss_damage%
            .build());
    }

    // ==================== ACCESSEURS ====================

    /**
     * Obtient tous les talents du Guerrier
     */
    public static List<Talent> getAll() {
        return Collections.unmodifiableList(TALENTS);
    }

    /**
     * Obtient les talents d'un palier specifique
     */
    public static List<Talent> getByTier(TalentTier tier) {
        return TALENTS.stream()
            .filter(t -> t.getTier() == tier)
            .toList();
    }

    /**
     * Obtient un talent par son ID
     */
    public static Talent getById(String id) {
        return TALENTS.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Obtient un talent par palier et slot
     */
    public static Talent getByTierAndSlot(TalentTier tier, int slot) {
        return TALENTS.stream()
            .filter(t -> t.getTier() == tier && t.getSlotIndex() == slot)
            .findFirst()
            .orElse(null);
    }

    private GuerrierTalents() {
        // Utility class
    }
}
