package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registre de tous les talents de l'Occultiste
 * 45 talents au total, 5 par palier sur 9 paliers
 * Identite: Mage devastateur, AoE magique, manipulation des elements et des ames
 */
public final class OccultisteTalents {

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
        registerTier9Talents();
    }

    // ==================== PALIER 1 - NIVEAU 0 (Fondation) ====================

    private static void registerTier1Talents() {
        // 1.1 - EMBRASEMENT
        TALENTS.add(Talent.builder()
            .id("occultiste_ignite")
            .name("Embrasement")
            .description("25% chance d'enflammer (Surchauffe)")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Chance d'enflammer les ennemis.",
                "§7Plus ils brulent, plus ils souffrent.",
                "",
                "§6Chance: §e25% §7| §6Duree: §b3s",
                "§6Surchauffe: §c+5%§7 degats/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.BLAZE_POWDER)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.IGNITE)
            .values(new double[]{0.25, 3.0}) // chance, duration_s
            .internalCooldownMs(500)
            .build());

        // 1.2 - GIVRE MORDANT (BUFFÉ: +30% dégâts directs pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_frost_bite")
            .name("Givre Mordant")
            .description("20% chance de geler + degats")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chance de geler et ralentir.",
                "§7Accumule des stacks de Givre.",
                "",
                "§6Chance: §e20% §7| §6Slow: §b40%",
                "§6Duree: §b2s §7| §6Bonus: §c+30%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.BLUE_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.FROST_BITE)
            .values(new double[]{0.20, 0.40, 2.0, 1, 0.30}) // chance, slow%, duration_s, frost_stacks, bonus_damage% (nouveau)
            .internalCooldownMs(600)
            .build());

        // 1.3 - ARC ELECTRIQUE
        TALENTS.add(Talent.builder()
            .id("occultiste_chain_lightning")
            .name("Arc Electrique")
            .description("25% chance d'eclair en chaine")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Chance de declencher un eclair",
                "§7qui rebondit sur plusieurs cibles.",
                "",
                "§6Chance: §e25% §7| §6Cibles: §e3",
                "§6Degats: §c60%§7/cible | §6Portee: §e5 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(2)
            .icon(Material.LIGHTNING_ROD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.CHAIN_LIGHTNING)
            .values(new double[]{0.25, 3, 0.60, 5}) // chance, targets, damage%, range
            .internalCooldownMs(500)
            .build());

        // 1.4 - SIPHON D'AME (BUFFÉ: +25% dégâts pendant 5s après kill pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_siphon")
            .name("Siphon d'Ame")
            .description("Kill = 3% PV + orbe + buff degats")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Chaque kill soigne et genere",
                "§7une orbe d'ame + buff degats.",
                "",
                "§6Soin: §a3% §7PV | §6Orbes max: §e5",
                "§6Buff: §c+25% §7degats | §6Duree: §b5s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.SOUL_LANTERN)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_SIPHON)
            .values(new double[]{0.03, 5, 0.25, 5000}) // heal%, max_orbs, damage_buff% (nouveau), buff_duration_ms (nouveau)
            .build());

        // 1.5 - MOT DE L'OMBRE (Shadow Word: Pain)
        TALENTS.add(Talent.builder()
            .id("occultiste_shadow_word")
            .name("Mot de l'Ombre")
            .description("30% chance d'appliquer un DOT d'ombre")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chance d'appliquer un DOT d'ombre.",
                "§7Genere de l'Insanity a chaque tick.",
                "",
                "§6Chance: §e30% §7| §6Degats: §c15%§7/s",
                "§6Duree: §b4s §7| §6Insanity: §d+5§7/tick"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.WITHER_ROSE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_WORD)
            .values(new double[]{0.30, 0.15, 4.0, 5}) // chance, damage%/s, duration_s, insanity_per_tick
            .internalCooldownMs(500)
            .build());
    }

    // ==================== PALIER 2 - NIVEAU 5 (Amplification) ====================

    private static void registerTier2Talents() {
        // 2.1 - PROPAGATION
        TALENTS.add(Talent.builder()
            .id("occultiste_fire_spread")
            .name("Propagation")
            .description("Le feu se propage aux ennemis proches")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Les ennemis en feu propagent",
                "§7les flammes aux ennemis proches.",
                "",
                "§6Portee: §e2.5 blocs §7| §6Duree: §b2s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.FIRE_SPREAD)
            .values(new double[]{2.5, 2.0}) // range, propagation_duration_s
            .build());

        // 2.2 - COEUR DE GLACE
        TALENTS.add(Talent.builder()
            .id("occultiste_frozen_heart")
            .name("Coeur de Glace")
            .description("Geles prennent +20% degats + brisure")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les geles prennent plus de degats.",
                "§7Explosion de glace si tues geles.",
                "",
                "§6Bonus: §c+20% §7| §6Par stack: §c+5%",
                "§6Explosion: §e2.5 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.PRISMARINE_CRYSTALS)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.FROZEN_HEART)
            .values(new double[]{0.20, 2.5, 0.05}) // bonus_damage%, shatter_radius, bonus_per_stack
            .build());

        // 2.3 - SURCHARGE
        TALENTS.add(Talent.builder()
            .id("occultiste_overcharge")
            .name("Surcharge")
            .description("Eclair peut critiquer + ajoute une cible")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Les eclairs peuvent critiquer.",
                "§7Chaque crit ajoute une cible.",
                "",
                "§6Bonus crit: §e+1 cible§7/crit"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.GLOWSTONE_DUST)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.OVERCHARGE)
            .values(new double[]{1}) // extra_targets_per_crit
            .build());

        // 2.4 - RESERVOIR D'AMES (BUFFÉ: 150% par orbe au lieu de 100% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_reservoir")
            .name("Reservoir d'Ames")
            .description("Consommez les ames pour explosion")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§eSneak + Attaque§7: consomme toutes",
                "§7les orbes pour une explosion.",
                "",
                "§6Degats: §c150%§7/orbe"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.NETHER_STAR)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_RESERVOIR)
            .values(new double[]{1.5}) // damage_per_orb% (buffé 1.0→1.5)
            .build());

        // 2.5 - TOUCHER VAMPIRIQUE (Vampiric Touch)
        TALENTS.add(Talent.builder()
            .id("occultiste_vampiric_touch")
            .name("Toucher Vampirique")
            .description("2eme DOT + soin sur degats d'ombre")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Ajoute un 2eme DOT sur les cibles.",
                "§7Vos DOTs d'ombre vous soignent.",
                "",
                "§6Degats: §c10%§7/s | §6Duree: §b6s",
                "§6Lifesteal: §a25%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.GHAST_TEAR)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.VAMPIRIC_TOUCH)
            .values(new double[]{0.10, 6.0, 0.25}) // damage%/s, duration_s, heal%
            .build());
    }

    // ==================== PALIER 3 - NIVEAU 10 (Specialisation) ====================

    private static void registerTier3Talents() {
        // 3.1 - TEMPETE DE FEU
        TALENTS.add(Talent.builder()
            .id("occultiste_firestorm")
            .name("Tempete de Feu")
            .description("25% chance de pluie de meteores")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Chance de faire pleuvoir des",
                "§7meteores sur la zone.",
                "",
                "§6Chance: §e25% §7| §6Meteores: §e3",
                "§6Degats: §c60%§7/meteore | §6Zone: §e4 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.MAGMA_BLOCK)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FIRESTORM)
            .values(new double[]{0.25, 3, 0.60, 4, 2.0}) // chance, meteors, damage%, zone, burn_extension_s
            .internalCooldownMs(2500)
            .build());

        // 3.2 - BLIZZARD (BUFFÉ: +25%/s dégâts de zone pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_blizzard")
            .name("Blizzard")
            .description("Aura de froid avec degats")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les geles emettent une aura",
                "§7qui ralentit et blesse les proches.",
                "",
                "§6Rayon: §e2.5 blocs §7| §6Slow: §b30%",
                "§6Degats: §c25%§7/s | §6Stacks: §b+1§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.POWDER_SNOW_BUCKET)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.BLIZZARD)
            .values(new double[]{2.5, 0.30, 1, 0.25}) // aura_radius, slow%, stacks_per_second, damage%/s (nouveau)
            .build());

        // 3.3 - TEMPETE ELECTRIQUE
        TALENTS.add(Talent.builder()
            .id("occultiste_lightning_storm")
            .name("Tempete Electrique")
            .description("Eclairs passifs autour de vous")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Eclairs passifs autour de vous.",
                "",
                "§6Intervalle: §b1.5s §7| §6Cibles: §e2",
                "§6Degats: §c40% §7| §6Portee: §e6 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.END_ROD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.LIGHTNING_STORM)
            .values(new double[]{1500, 2, 0.40, 6}) // tick_ms, targets, damage%, range
            .build());

        // 3.4 - PACTE DES AMES (BUFFÉ: +8% par orbe au lieu de +5%, max 40% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_pact")
            .name("Pacte des Ames")
            .description("+8% degats par orbe d'ame")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Chaque orbe augmente vos degats.",
                "",
                "§6Bonus: §c+8%§7/orbe",
                "§6Max: §c+40% §7(5) | §c+80% §7(10 Legion)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.AMETHYST_SHARD)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_PACT)
            .values(new double[]{0.08}) // damage_per_orb% (buffé 0.05→0.08)
            .build());

        // 3.5 - APPARITIONS D'OMBRE (Shadowy Apparitions)
        TALENTS.add(Talent.builder()
            .id("occultiste_shadowy_apparitions")
            .name("Apparitions d'Ombre")
            .description("Les DOT generent des apparitions fantomes")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos DOTs generent des apparitions",
                "§7fantomes qui attaquent les cibles.",
                "",
                "§6Intervalle: §b2s §7| §6Degats: §c50%",
                "§6Insanity: §d+3§7/hit"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.SHADOWY_APPARITIONS)
            .values(new double[]{2000, 0.50, 3}) // spawn_interval_ms, damage%, insanity
            .build());
    }

    // ==================== PALIER 4 - NIVEAU 15 (Evolution) ====================

    private static void registerTier4Talents() {
        // 4.1 - PHOENIX (Ignition Critique)
        TALENTS.add(Talent.builder()
            .id("occultiste_phoenix_flame")
            .name("Phoenix")
            .description("Ignition Critique a Surchauffe max")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Surchauffe max (8s) declenche",
                "§7une Ignition Critique explosive.",
                "",
                "§6Degats: §c25% §7PV | §6Boss: §c12% §7PV",
                "§6Rayon: §e3 blocs §7| §bCD: §f10s§7/cible"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.BLAZE_ROD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.PHOENIX_FLAME)
            .values(new double[]{0.25, 0.12, 3.0, 10000, 3.0}) // dmg%, boss_dmg%, radius, cooldown_ms, spread_burn_s
            .build());

        // 4.2 - ZERO ABSOLU (BUFFÉ: 3%/stack au lieu de 2%, CD 8s au lieu de 10s pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_absolute_zero")
            .name("Zero Absolu")
            .description("Brisure de glace sur ennemis a 5+ stacks")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7A 5+ stacks, Brisure Glaciale.",
                "§7Degats bases sur les stacks.",
                "",
                "§6Degats: §c3%§7 PV/stack | §6Boss: §c1.5%",
                "§6Max: §e20 stacks §7| §bCD: §f8s§7/cible"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ABSOLUTE_ZERO)
            .values(new double[]{5, 0.03, 0.015, 8000}) // min_stacks, damage_per_stack% (buffé 0.02→0.03), boss_damage_per_stack% (buffé 0.01→0.015), cooldown_ms (buffé 10000→8000)
            .build());

        // 4.3 - CONDUCTEUR
        TALENTS.add(Talent.builder()
            .id("occultiste_conductor")
            .name("Conducteur")
            .description("Les eclairs vous soignent")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Les eclairs vous soignent.",
                "",
                "§6Lifesteal: §a5% §7des degats"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.YELLOW_DYE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.CONDUCTOR)
            .values(new double[]{0.05}) // leech%
            .build());

        // 4.4 - MOISSON ETERNELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_eternal_harvest")
            .name("Moisson Eternelle")
            .description("Orbes regen 1% HP/s chacune")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Chaque orbe regenere vos PV.",
                "",
                "§6Regen: §a1%§7 PV/s par orbe",
                "§6Max: §a5%§7 PV/s (5 orbes)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.GOLDEN_APPLE)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.ETERNAL_HARVEST)
            .values(new double[]{0.01}) // regen_per_orb%
            .build());

        // 4.5 - GRAVITE SOMBRE (Dark Gravity)
        TALENTS.add(Talent.builder()
            .id("occultiste_dark_gravity")
            .name("Gravite Sombre")
            .description("Vos attaques ralentissent et amplifient les degats")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos attaques ralentissent.",
                "§7Les ralentis prennent plus de degats.",
                "",
                "§6Slow: §b30% §7| §6Duree: §b3s",
                "§6Bonus: §c+20% §7degats"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.ENDER_EYE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DARK_GRAVITY)
            .values(new double[]{0.30, 3000, 0.20}) // slow_percent, duration_ms, damage_bonus%
            .build());
    }

    // ==================== PALIER 5 - NIVEAU 20 (Maitrise) ====================

    private static void registerTier5Talents() {
        // 5.1 - AVATAR DE FEU
        TALENTS.add(Talent.builder()
            .id("occultiste_fire_avatar")
            .name("Avatar de Feu")
            .description("Aura de flammes + immunite feu")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Aura de flammes autour de vous.",
                "§7Immunite au feu.",
                "",
                "§6Rayon: §e4 blocs §7| §6Degats: §c20%§7/s",
                "§6Surchauffe: §c+1s§7/s aux proches"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.FIRE_CORAL)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FIRE_AVATAR)
            .values(new double[]{4.0, 0.20, 1.0}) // radius, damage_per_second%, burn_extension_per_s
            .build());

        // 5.2 - SEIGNEUR DU GIVRE
        TALENTS.add(Talent.builder()
            .id("occultiste_frost_lord")
            .name("Seigneur du Givre")
            .description("60% freeze, +2 stacks, duree +50%")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chance de gel augmentee.",
                "§7Les geles perdent de l'armure.",
                "",
                "§6Chance: §e60% §7| §6Duree: §b3s",
                "§6Stacks: §e+2 §7| §6Armure: §c-20%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.PACKED_ICE)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.FROST_LORD)
            .values(new double[]{0.60, 3.0, 2, 0.20}) // freeze_chance, duration_s, stacks_per_hit, armor_reduction
            .build());

        // 5.3 - DIEU DE LA FOUDRE
        TALENTS.add(Talent.builder()
            .id("occultiste_thunder_god")
            .name("Dieu de la Foudre")
            .description("Chain lightning illimite")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Les eclairs n'ont plus de limite.",
                "§7Touche tous les ennemis en portee.",
                "",
                "§6Portee chaine: §e6 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.BEACON)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.THUNDER_GOD)
            .values(new double[]{6.0}) // chain_range
            .build());

        // 5.4 - LEGION D'AMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_legion")
            .name("Legion d'Ames")
            .description("10 orbes max + 5% reduction degats chacune")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Stockez plus d'orbes.",
                "§7Chaque orbe reduit les degats.",
                "",
                "§6Max orbes: §e10 §7| §6Reduction: §a5%§7/orbe",
                "§6Max reduction: §a50%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.ENCHANTED_GOLDEN_APPLE)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_LEGION)
            .values(new double[]{10, 0.05}) // max_orbs, dr_per_orb
            .build());

        // 5.5 - IMPLOSION
        TALENTS.add(Talent.builder()
            .id("occultiste_implosion")
            .name("Implosion")
            .description("Chaque kill attire les ennemis")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chaque kill attire les ennemis",
                "§7proches vers le cadavre.",
                "",
                "§6Rayon: §e8 blocs §7| §6Degats: §c50%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.END_CRYSTAL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.IMPLOSION)
            .values(new double[]{8.0, 0.50}) // radius, damage%
            .build());
    }

    // ==================== PALIER 6 - NIVEAU 25 (Ascension) ====================

    private static void registerTier6Talents() {
        // 6.1 - PYROCLASME
        TALENTS.add(Talent.builder()
            .id("occultiste_pyroclasm")
            .name("Pyroclasme")
            .description("Surchauffe persiste + explosion a la mort")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7La Surchauffe persiste.",
                "§7Kill en feu = explosion AoE.",
                "",
                "§6Degats: §c60% §7| §6Rayon: §e3 blocs",
                "§6Proches: §c+3s §7feu"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.MAGMA_BLOCK)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.PYROCLASM)
            .values(new double[]{0.60, 3.0, 3.0}) // damage%, radius, spread_burn_s
            .build());

        // 6.2 - PERMAFROST
        TALENTS.add(Talent.builder()
            .id("occultiste_permafrost")
            .name("Permafrost")
            .description("Les stacks de Givre persistent et se propagent")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les stacks de Givre persistent.",
                "§7Les geles propagent aux proches.",
                "",
                "§6Propagation: §e3 blocs §7| §b+1 stack§7/s",
                "§6Slow: §b5%§7/stack (max §b50%§7)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.PACKED_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.PERMAFROST)
            .values(new double[]{3.0, 0.05, 0.50, 1}) // radius, slow_per_stack, max_slow, propagation_rate
            .build());

        // 6.3 - CHAMP STATIQUE
        TALENTS.add(Talent.builder()
            .id("occultiste_static_field")
            .name("Champ Statique")
            .description("Eclairs laissent une zone de degats")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Les eclairs laissent des zones",
                "§7electriques au sol.",
                "",
                "§6Duree: §b2s §7| §6Rayon: §e2 blocs",
                "§6Degats: §c20%§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.YELLOW_WOOL)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.STATIC_FIELD)
            .values(new double[]{2000, 0.20, 2.0}) // duration_ms, damage%_per_second, radius
            .build());

        // 6.4 - LIEN D'AMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_bond")
            .name("Lien d'Ames")
            .description("Orbes absorbent 5% des degats recus chacune")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Les orbes absorbent les degats.",
                "§7Disparaissent apres 50 PV absorbes.",
                "",
                "§6Absorption: §a5%§7/orbe | §6Max: §a25%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.SOUL_TORCH)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_BOND)
            .values(new double[]{0.05, 50}) // absorption_per_orb, max_absorb_per_orb
            .build());

        // 6.5 - PUITS DE GRAVITE (Gravity Well)
        TALENTS.add(Talent.builder()
            .id("occultiste_gravity_well")
            .name("Puits de Gravite")
            .description("Zone de gravite automatique")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Un puits de gravite apparait",
                "§7automatiquement en combat.",
                "",
                "§6Intervalle: §b15s §7| §6Duree: §b6s",
                "§6Rayon: §e5 blocs §7| §6Slow: §b50%",
                "§6Degats: §c30%§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.CRYING_OBSIDIAN)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.GRAVITY_WELL)
            .values(new double[]{6000, 5.0, 0.50, 0.30, 15000}) // duration_ms, radius, slow%, damage_per_s%, interval_ms
            .build());
    }

    // ==================== PALIER 7 - NIVEAU 30 (Transcendance) ====================

    private static void registerTier7Talents() {
        // 7.1 - INFERNO
        TALENTS.add(Talent.builder()
            .id("occultiste_inferno")
            .name("Inferno")
            .description("Nova de feu toutes les 12s")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Nova de feu periodique.",
                "",
                "§6Intervalle: §b12s §7| §6Rayon: §e5 blocs",
                "§6Degats: §c150% §7| §6Feu: §c+4s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.MAGMA_CREAM)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.INFERNO)
            .values(new double[]{12000, 1.50, 5.0, 4.0}) // cooldown_ms, damage%, radius, burn_extension_s
            .build());

        // 6.2 - ERE GLACIAIRE (BUFFÉ: +50%/s dégâts de zone pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_ice_age")
            .name("Ere Glaciaire")
            .description("Zones de givre avec degats")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Kill a 5+ stacks cree une zone",
                "§7de givre persistante.",
                "",
                "§6Duree: §b4s §7| §6Rayon: §e2.5 blocs",
                "§6Stacks: §b+2§7/s | §6Degats: §c50%§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.BLUE_STAINED_GLASS)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ICE_AGE)
            .values(new double[]{4000, 2.5, 2, 0.35, 5, 0.50}) // duration_ms, radius, stacks_per_sec, slow%, min_stacks_to_trigger, damage%/s (nouveau)
            .build());

        // 6.3 - TEMPETE PERPETUELLE (NERFÉ: tick 0.5s → 1.0s pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_perpetual_storm")
            .name("Tempete Perpetuelle")
            .description("Tempete electrique permanente")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Tempete electrique permanente.",
                "",
                "§6Rayon: §e5 blocs §7| §6Tick: §b1s",
                "§6Cibles: §e3 §7| §6Degats: §c25%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.PRISMARINE_SHARD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.PERPETUAL_STORM)
            .values(new double[]{1000, 5.0, 3, 0.25}) // tick_ms (nerfé 500→1000), radius, targets, damage%
            .build());

        // 7.4 - NECROMANCIEN (BUFFÉ: 120% stats au lieu de 100% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_necromancer")
            .name("Necromancien")
            .description("Invoquez des squelettes archers")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§eSneak + Clic§7: invoque un squelette",
                "§7archer (coute 1 orbe).",
                "",
                "§6Stats: §c120% §7des votres | §6Max: §e8",
                "§6Duree: §b30s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.NECROMANCER)
            .values(new double[]{1.2, 30000, 8}) // stats% (buffé 1.0→1.2), duration_ms, max_summons
            .build());

        // 7.5 - SINGULARITE
        TALENTS.add(Talent.builder()
            .id("occultiste_singularity")
            .name("Singularite")
            .description("Multi-kill cree une singularite")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Multi-kill (3 en 5s) cree une",
                "§7singularite qui aspire tout.",
                "",
                "§6Rayon: §e10 blocs §7| §6Duree: §b3s",
                "§6Degats: §c200% §7+ §c50%§7/s",
                "§bCooldown: §f8s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.SCULK_CATALYST)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SINGULARITY)
            .values(new double[]{10.0, 3000, 2.0, 0.50, 8000, 3, 5000}) // radius, duration_ms, initial_damage%, dps%, cooldown_ms, required_kills, kill_window_ms
            .build());
    }

    // ==================== PALIER 8 - NIVEAU 40 (Apex) ====================

    private static void registerTier8Talents() {
        // 8.1 - SOLEIL NOIR
        TALENTS.add(Talent.builder()
            .id("occultiste_black_sun")
            .name("Soleil Noir")
            .description("Invoquez un soleil de feu")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Invoquez un soleil de feu.",
                "",
                "§6Duree: §b8s §7| §6Rayon: §e6 blocs",
                "§6Degats: §c80%§7/s | §6Feu: §c+2s§7/s",
                "§bCooldown: §f35s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.SUNFLOWER)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BLACK_SUN)
            .values(new double[]{35000, 8000, 0.80, 6.0, 2.0}) // cooldown, duration, damage%/s, radius, burn_extension_per_s
            .build());

        // 7.2 - HIVER ETERNEL
        TALENTS.add(Talent.builder()
            .id("occultiste_eternal_winter")
            .name("Hiver Eternel")
            .description("Aura de froid devastatrice")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Aura permanente de givre.",
                "§7Bonus degats par stack.",
                "",
                "§6Rayon: §e6 blocs §7| §6Stacks: §b+1§7/s",
                "§6Bonus: §c+5%§7/stack (max §c+40%§7)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.FLOWER_BANNER_PATTERN)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.ETERNAL_WINTER)
            .values(new double[]{6.0, 1, 0.05, 0.40}) // radius, stacks_per_sec, damage_per_stack%, max_damage_bonus%
            .build());

        // 7.3 - MJOLNIR (NERFÉ: 3 strikes → 2 strikes pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_mjolnir")
            .name("Mjolnir")
            .description("Chain lightning x2 strikes")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos eclairs frappent 2 fois.",
                "",
                "§6Strikes: §ex2 §7| §6Degats: §c60%§7/strike",
                "§6Total: §c120%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.TRIDENT)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.MJOLNIR)
            .values(new double[]{2, 0.60}) // strikes (nerfé 3→2), damage_per_strike%
            .build());

        // 7.4 - SEIGNEUR DES MORTS
        TALENTS.add(Talent.builder()
            .id("occultiste_lord_of_dead")
            .name("Seigneur des Morts")
            .description("75% chance de relever les morts")
            .loreLines(new String[]{
                "§d§lVOIE DES AMES",
                "",
                "§7Chance de relever les morts tues",
                "§7en serviteurs.",
                "",
                "§6Chance: §e75% §7| §6Stats: §c80%",
                "§6Max: §e15 §7| §6Duree: §b30s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.LORD_OF_THE_DEAD)
            .values(new double[]{0.75, 0.80, 30000, 15}) // chance, stats%, duration_ms, max
            .build());

        // 8.5 - DECHIRURE DIMENSIONNELLE (Dimensional Rift)
        TALENTS.add(Talent.builder()
            .id("occultiste_dimensional_rift")
            .name("Dechirure Dimensionnelle")
            .description("Ennemis faibles bannis automatiquement")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Ennemis a -15% PV bannis",
                "§7automatiquement dans le vide.",
                "",
                "§6Seuil: §c15% §7PV | §6Duree: §b1s",
                "§6Degats: §c250% §7+ §c100% §7AoE",
                "§bCD: §f10s§7/cible"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.END_PORTAL_FRAME)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DIMENSIONAL_RIFT)
            .values(new double[]{0.15, 1000, 2.50, 10000, 1.0, 4.0}) // hp_threshold%, banish_duration_ms, exit_damage%, cooldown_per_target_ms, aoe_damage%, aoe_radius
            .build());
    }

    // ==================== PALIER 9 - NIVEAU 50 (Legendaire) ====================

    private static void registerTier9Talents() {
        // 9.1 - PLUIE DE METEORES
        TALENTS.add(Talent.builder()
            .id("occultiste_meteor_rain")
            .name("Pluie de Meteores")
            .description("12 meteores toutes les 45s")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§c§lVOIE DU FEU",
                "",
                "§7Pluie de meteores periodique.",
                "",
                "§6Meteores: §e12 §7| §6Zone: §e15 blocs",
                "§6Degats: §c150%§7/meteore | §6Feu: §c+3s",
                "§bCooldown: §f45s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(0)
            .icon(Material.DRAGON_EGG)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.METEOR_RAIN)
            .values(new double[]{45000, 12, 1.50, 15, 3.0}) // cooldown_ms, meteors, damage%, zone, burn_per_impact_s
            .build());

        // 8.2 - STASE TEMPORELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_time_stasis")
            .name("Stase Temporelle")
            .description("Gelez le temps + explosion de stacks")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§b§lVOIE DU GIVRE",
                "",
                "§e2x Sneak§7: gele TOUS les ennemis.",
                "§7Brisure Glaciale a la fin.",
                "",
                "§6Duree: §b3s §7| §6Stacks: §b+10",
                "§bCooldown: §f90s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.CLOCK)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.TIME_STASIS)
            .values(new double[]{90000, 3000, 10}) // cooldown_ms, duration_ms, stacks_applied
            .build());

        // 9.3 - JUGEMENT DIVIN (NERFÉ: 300% → 200% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_divine_judgment")
            .name("Jugement Divin")
            .description("Eclair sur TOUS les ennemis proches")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Eclair divin frappe tous les ennemis.",
                "",
                "§6Rayon: §e25 blocs §7| §6Cibles: §e30 max",
                "§6Degats: §c200%§7/cible",
                "§bCooldown: §f30s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(2)
            .icon(Material.NETHER_STAR)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.DIVINE_JUDGMENT)
            .values(new double[]{30000, 2.0, 25.0}) // cooldown_ms, damage% (nerfé 3.0→2.0), range
            .build());

        // 8.4 - ARMEE IMMORTELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_immortal_army")
            .name("Armee Immortelle")
            .description("Serviteurs immortels + buff")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§d§lVOIE DES AMES",
                "",
                "§7Serviteurs immortels qui respawn.",
                "",
                "§6Respawn: §b3s §7| §6Buff stats: §c+75%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(3)
            .icon(Material.TOTEM_OF_UNDYING)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.IMMORTAL_ARMY)
            .values(new double[]{3000, 0.75}) // respawn_ms, stat_buff%
            .build());

        // 9.5 - TROU NOIR (Black Hole) - ULTIMATE
        TALENTS.add(Talent.builder()
            .id("occultiste_black_hole")
            .name("Trou Noir")
            .description("Ultimate: Trou noir automatique")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Trou noir massif automatique.",
                "§7Aspire et detruit tout.",
                "",
                "§6Rayon: §e15 blocs §7| §6Duree: §b5s",
                "§6Degats: §c300% §7+ §c75%§7/s",
                "§bCooldown: §f45s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.NETHER_STAR)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.BLACK_HOLE)
            .values(new double[]{15.0, 5000, 3.0, 0.75, 45000}) // radius, duration_ms, initial_damage%, dps%, interval_ms
            .build());
    }

    // ==================== ACCESSEURS ====================

    public static List<Talent> getAll() {
        return Collections.unmodifiableList(TALENTS);
    }

    public static Talent getById(String id) {
        return TALENTS.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private OccultisteTalents() {
        // Private constructor to prevent instantiation
    }
}
