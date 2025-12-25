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
            .description("Chance d'enflammer les ennemis avec la Surchauffe")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Vos attaques ont une chance",
                "§7d'enflammer les ennemis. Plus ils",
                "§7brûlent longtemps, plus ils souffrent.",
                "",
                "§6Chance: §e25%",
                "§6Durée: §b3s",
                "§6Surchauffe: §c+5% §7dégâts/s"
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
            .description("Chance de geler et ralentir les ennemis")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Vos attaques ont une chance de",
                "§7geler et ralentir les ennemis,",
                "§7accumulant des stacks de Givre.",
                "",
                "§6Chance: §e20%",
                "§6Ralentissement: §b40%",
                "§6Durée: §b2s",
                "§6Bonus dégâts: §c+30%"
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

        // 1.3 - ARC ÉLECTRIQUE
        TALENTS.add(Talent.builder()
            .id("occultiste_chain_lightning")
            .name("Arc Électrique")
            .description("Chance de déclencher un éclair en chaîne")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos attaques ont une chance de",
                "§7déclencher un éclair qui rebondit",
                "§7sur plusieurs cibles proches.",
                "",
                "§6Chance: §e25%",
                "§6Cibles: §e3",
                "§6Dégâts: §c60% §7par cible",
                "§6Portée: §e5 blocs"
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

        // 1.4 - SIPHON D'ÂME (BUFFÉ: +25% dégâts pendant 5s après kill pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_siphon")
            .name("Siphon d'Âme")
            .description("Les éliminations génèrent des orbes d'âme")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Chaque élimination vous soigne,",
                "§7génère une orbe d'âme et octroie",
                "§7un buff de dégâts temporaire.",
                "",
                "§6Soin: §a3% §7PV",
                "§6Orbes maximum: §e5",
                "§6Buff dégâts: §c+25%",
                "§6Durée buff: §b5s"
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
            .description("Chance d'appliquer un DoT d'ombre")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos attaques ont une chance",
                "§7d'appliquer un DoT d'ombre qui",
                "§7génère de l'Insanity à chaque tick.",
                "",
                "§6Chance: §e30%",
                "§6Dégâts: §c15%§7/s",
                "§6Durée: §b4s",
                "§6Insanity: §d+5§7/tick"
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
            .description("Les flammes se propagent aux ennemis proches")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Les ennemis en feu propagent",
                "§7automatiquement les flammes aux",
                "§7ennemis proches.",
                "",
                "§6Portée: §e2.5 blocs",
                "§6Durée propagée: §b2s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.FIRE_SPREAD)
            .values(new double[]{2.5, 2.0}) // range, propagation_duration_s
            .build());

        // 2.2 - CŒUR DE GLACE
        TALENTS.add(Talent.builder()
            .id("occultiste_frozen_heart")
            .name("Cœur de Glace")
            .description("Les ennemis gelés subissent plus de dégâts")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les ennemis gelés subissent des",
                "§7dégâts bonus. Explosion de glace",
                "§7s'ils meurent gelés.",
                "",
                "§6Bonus dégâts: §c+20%",
                "§6Par stack: §c+5%",
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
            .description("Les éclairs peuvent critiquer et rebondir davantage")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos éclairs peuvent désormais",
                "§7critiquer. Chaque critique ajoute",
                "§7une cible supplémentaire.",
                "",
                "§6Bonus critique: §e+1 cible §7par crit"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.GLOWSTONE_DUST)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.OVERCHARGE)
            .values(new double[]{1}) // extra_targets_per_crit
            .build());

        // 2.4 - RÉSERVOIR D'ÂMES (BUFFÉ: 150% par orbe au lieu de 100% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_reservoir")
            .name("Réservoir d'Âmes")
            .description("Consomme les orbes pour une explosion dévastatrice")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Consomme toutes vos orbes d'âme",
                "§7pour déclencher une explosion",
                "§7dévastatrice autour de vous.",
                "",
                "§6Dégâts: §c150% §7par orbe",
                "",
                "§eActivation: §fSneak + Attaque"
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
            .description("Applique un second DoT qui vous soigne")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Applique un second DoT sur les",
                "§7cibles touchées. Vos DoTs d'ombre",
                "§7vous soignent d'un pourcentage.",
                "",
                "§6Dégâts: §c10%§7/s",
                "§6Durée: §b6s",
                "§6Vol de vie: §a25%"
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
        // 3.1 - TEMPÊTE DE FEU
        TALENTS.add(Talent.builder()
            .id("occultiste_firestorm")
            .name("Tempête de Feu")
            .description("Chance de faire pleuvoir des météores")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Vos attaques ont une chance de",
                "§7faire pleuvoir des météores",
                "§7sur la zone ciblée.",
                "",
                "§6Chance: §e25%",
                "§6Météores: §e3",
                "§6Dégâts: §c60% §7par météore",
                "§6Zone: §e4 blocs",
                "",
                "§bCooldown: §f2.5s"
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
            .description("Les ennemis gelés émettent une aura de froid")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les ennemis gelés émettent une",
                "§7aura qui ralentit et blesse les",
                "§7ennemis à proximité.",
                "",
                "§6Rayon: §e2.5 blocs",
                "§6Ralentissement: §b30%",
                "§6Dégâts: §c25%§7/s",
                "§6Stacks: §b+1§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.POWDER_SNOW_BUCKET)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.BLIZZARD)
            .values(new double[]{2.5, 0.30, 1, 0.25}) // aura_radius, slow%, stacks_per_second, damage%/s (nouveau)
            .build());

        // 3.3 - TEMPÊTE ÉLECTRIQUE
        TALENTS.add(Talent.builder()
            .id("occultiste_lightning_storm")
            .name("Tempête Électrique")
            .description("Génère des éclairs passifs autour de vous")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Génère automatiquement des éclairs",
                "§7qui frappent les ennemis proches",
                "§7à intervalles réguliers.",
                "",
                "§6Intervalle: §b1.5s",
                "§6Cibles: §e2",
                "§6Dégâts: §c40%",
                "§6Portée: §e6 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.END_ROD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.LIGHTNING_STORM)
            .values(new double[]{1500, 2, 0.40, 6}) // tick_ms, targets, damage%, range
            .build());

        // 3.4 - PACTE DES ÂMES (BUFFÉ: +8% par orbe au lieu de +5%, max 40% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_pact")
            .name("Pacte des Âmes")
            .description("Chaque orbe augmente vos dégâts")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Chaque orbe d'âme que vous",
                "§7possédez augmente vos dégâts.",
                "",
                "§6Bonus: §c+8% §7par orbe",
                "§6Maximum: §c+40% §7(5 orbes)",
                "§6Avec Légion: §c+80% §7(10 orbes)"
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
            .description("Les DoT génèrent des apparitions fantômes")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos DoTs génèrent des apparitions",
                "§7fantômes qui attaquent les cibles",
                "§7et génèrent de l'Insanity.",
                "",
                "§6Intervalle: §b2s",
                "§6Dégâts: §c50%",
                "§6Insanity: §d+3 §7par coup"
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
            .description("La Surchauffe maximale déclenche une Ignition Critique")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Quand la Surchauffe atteint son",
                "§7maximum (8s), déclenche une",
                "§7Ignition Critique explosive.",
                "",
                "§6Dégâts: §c25% §7PV cible",
                "§6Sur Boss: §c12% §7PV",
                "§6Rayon: §e3 blocs",
                "",
                "§bCooldown: §f10s §7par cible"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.BLAZE_ROD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.PHOENIX_FLAME)
            .values(new double[]{0.25, 0.12, 3.0, 10000, 3.0}) // dmg%, boss_dmg%, radius, cooldown_ms, spread_burn_s
            .build());

        // 4.2 - ZÉRO ABSOLU (BUFFÉ: 3%/stack au lieu de 2%, CD 8s au lieu de 10s pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_absolute_zero")
            .name("Zéro Absolu")
            .description("Brisure Glaciale sur les ennemis à 5+ stacks")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les ennemis avec 5+ stacks de",
                "§7Givre subissent une Brisure Glaciale.",
                "§7Dégâts basés sur les stacks.",
                "",
                "§6Dégâts: §c3% §7PV par stack",
                "§6Sur Boss: §c1.5% §7PV",
                "§6Maximum: §e20 stacks",
                "",
                "§bCooldown: §f8s §7par cible"
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
            .description("Les éclairs vous soignent")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos éclairs vous soignent d'un",
                "§7pourcentage des dégâts infligés.",
                "",
                "§6Vol de vie: §a5% §7des dégâts"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.YELLOW_DYE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.CONDUCTOR)
            .values(new double[]{0.05}) // leech%
            .build());

        // 4.4 - MOISSON ÉTERNELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_eternal_harvest")
            .name("Moisson Éternelle")
            .description("Les orbes régénèrent vos PV")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Chaque orbe d'âme que vous",
                "§7possédez régénère vos PV.",
                "",
                "§6Régénération: §a1% §7PV/s par orbe",
                "§6Maximum: §a5% §7PV/s (5 orbes)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.GOLDEN_APPLE)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.ETERNAL_HARVEST)
            .values(new double[]{0.01}) // regen_per_orb%
            .build());

        // 4.5 - GRAVITÉ SOMBRE (Dark Gravity)
        TALENTS.add(Talent.builder()
            .id("occultiste_dark_gravity")
            .name("Gravité Sombre")
            .description("Les attaques ralentissent et amplifient les dégâts")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos attaques ralentissent les",
                "§7ennemis. Les cibles ralenties",
                "§7subissent des dégâts bonus.",
                "",
                "§6Ralentissement: §b30%",
                "§6Durée: §b3s",
                "§6Bonus dégâts: §c+20%"
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
            .description("Génère une aura de flammes et immunité au feu")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Génère une aura de flammes autour",
                "§7de vous et vous rend immunisé",
                "§7aux dégâts de feu.",
                "",
                "§6Rayon: §e4 blocs",
                "§6Dégâts: §c20%§7/s",
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
            .description("Chance de gel augmentée et réduction d'armure")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Augmente votre chance de gel.",
                "§7Les ennemis gelés perdent de",
                "§7l'armure.",
                "",
                "§6Chance: §e60%",
                "§6Durée: §b3s",
                "§6Stacks: §e+2",
                "§6Réduction armure: §c-20%"
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
            .description("Les éclairs en chaîne n'ont plus de limite")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos éclairs en chaîne n'ont plus",
                "§7de limite de cibles. Touche tous",
                "§7les ennemis à portée.",
                "",
                "§6Portée de chaîne: §e6 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.BEACON)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.THUNDER_GOD)
            .values(new double[]{6.0}) // chain_range
            .build());

        // 5.4 - LÉGION D'ÂMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_legion")
            .name("Légion d'Âmes")
            .description("Augmente la capacité d'orbes et réduit les dégâts")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Augmente votre capacité d'orbes.",
                "§7Chaque orbe réduit les dégâts",
                "§7que vous subissez.",
                "",
                "§6Maximum orbes: §e10",
                "§6Réduction: §a5% §7par orbe",
                "§6Réduction max: §a50%"
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
            .description("Les éliminations attirent les ennemis proches")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chaque élimination attire les",
                "§7ennemis proches vers le cadavre",
                "§7et leur inflige des dégâts.",
                "",
                "§6Rayon: §e8 blocs",
                "§6Dégâts: §c50%"
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
            .description("La Surchauffe persiste et explose à la mort")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7La Surchauffe persiste après",
                "§7extinction. Tuer un ennemi en feu",
                "§7déclenche une explosion de zone.",
                "",
                "§6Dégâts: §c60%",
                "§6Rayon: §e3 blocs",
                "§6Propagation: §c+3s §7feu aux proches"
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
                "§7Les stacks de Givre persistent",
                "§7et se propagent aux ennemis",
                "§7à proximité des gelés.",
                "",
                "§6Propagation: §e3 blocs",
                "§6Stacks: §b+1§7/s aux proches",
                "§6Ralentissement: §b5% §7par stack",
                "§6Maximum: §b50%"
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
            .description("Les éclairs laissent des zones électriques")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos éclairs laissent des zones",
                "§7électriques persistantes au sol",
                "§7qui blessent les ennemis.",
                "",
                "§6Durée: §b2s",
                "§6Rayon: §e2 blocs",
                "§6Dégâts: §c20%§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.YELLOW_WOOL)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.STATIC_FIELD)
            .values(new double[]{2000, 0.20, 2.0}) // duration_ms, damage%_per_second, radius
            .build());

        // 6.4 - LIEN D'ÂMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_bond")
            .name("Lien d'Âmes")
            .description("Les orbes absorbent une partie des dégâts")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Vos orbes d'âme absorbent une",
                "§7partie des dégâts reçus. Elles",
                "§7disparaissent après saturation.",
                "",
                "§6Absorption: §a5% §7par orbe",
                "§6Maximum: §a25%",
                "§6Saturation: §e50 PV §7par orbe"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.SOUL_TORCH)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_BOND)
            .values(new double[]{0.05, 50}) // absorption_per_orb, max_absorb_per_orb
            .build());

        // 6.5 - PUITS DE GRAVITÉ (Gravity Well)
        TALENTS.add(Talent.builder()
            .id("occultiste_gravity_well")
            .name("Puits de Gravité")
            .description("Génère automatiquement une zone de gravité")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Un puits de gravité apparaît",
                "§7automatiquement en combat,",
                "§7attirant et ralentissant les ennemis.",
                "",
                "§6Intervalle: §b15s",
                "§6Durée: §b6s",
                "§6Rayon: §e5 blocs",
                "§6Ralentissement: §b50%",
                "§6Dégâts: §c30%§7/s"
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
            .description("Déclenche une nova de feu périodique")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Déclenche automatiquement une",
                "§7nova de feu dévastatrice autour",
                "§7de vous à intervalles réguliers.",
                "",
                "§6Intervalle: §b12s",
                "§6Rayon: §e5 blocs",
                "§6Dégâts: §c150%",
                "§6Feu: §c+4s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.MAGMA_CREAM)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.INFERNO)
            .values(new double[]{12000, 1.50, 5.0, 4.0}) // cooldown_ms, damage%, radius, burn_extension_s
            .build());

        // 7.2 - ÈRE GLACIAIRE (BUFFÉ: +50%/s dégâts de zone pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_ice_age")
            .name("Ère Glaciaire")
            .description("Les éliminations créent des zones de givre")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Tuer un ennemi avec 5+ stacks",
                "§7crée une zone de givre persistante",
                "§7qui gèle et blesse les proches.",
                "",
                "§6Durée: §b4s",
                "§6Rayon: §e2.5 blocs",
                "§6Stacks: §b+2§7/s",
                "§6Dégâts: §c50%§7/s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.BLUE_STAINED_GLASS)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ICE_AGE)
            .values(new double[]{4000, 2.5, 2, 0.35, 5, 0.50}) // duration_ms, radius, stacks_per_sec, slow%, min_stacks_to_trigger, damage%/s (nouveau)
            .build());

        // 7.3 - TEMPÊTE PERPÉTUELLE (NERFÉ: tick 0.5s → 1.0s pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_perpetual_storm")
            .name("Tempête Perpétuelle")
            .description("Génère une tempête électrique permanente")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Génère une tempête électrique",
                "§7permanente autour de vous qui",
                "§7frappe les ennemis proches.",
                "",
                "§6Rayon: §e5 blocs",
                "§6Tick: §b1s",
                "§6Cibles: §e3",
                "§6Dégâts: §c25%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.PRISMARINE_SHARD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.PERPETUAL_STORM)
            .values(new double[]{1000, 5.0, 3, 0.25}) // tick_ms (nerfé 500→1000), radius, targets, damage%
            .build());

        // 7.4 - NÉCROMANCIEN (BUFFÉ: 120% stats au lieu de 100% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_necromancer")
            .name("Nécromancien")
            .description("Invoque des squelettes archers")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Invoque un squelette archer qui",
                "§7vous assiste. Coûte 1 orbe d'âme.",
                "",
                "§6Stats: §c120% §7des vôtres",
                "§6Maximum: §e8",
                "§6Durée: §b30s",
                "",
                "§eActivation: §fSneak + Clic"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.NECROMANCER)
            .values(new double[]{1.2, 30000, 8}) // stats% (buffé 1.0→1.2), duration_ms, max_summons
            .build());

        // 7.5 - SINGULARITÉ
        TALENTS.add(Talent.builder()
            .id("occultiste_singularity")
            .name("Singularité")
            .description("Les multi-kills créent une singularité")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Éliminer 3 ennemis en 5s crée",
                "§7une singularité qui aspire tout",
                "§7et inflige des dégâts massifs.",
                "",
                "§6Rayon: §e10 blocs",
                "§6Durée: §b3s",
                "§6Dégâts: §c200% §7+ §c50%§7/s",
                "",
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
            .description("Invoque un soleil de feu dévastateur")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Invoque un soleil de feu massif",
                "§7qui brûle et enflamme tous les",
                "§7ennemis dans sa zone.",
                "",
                "§6Durée: §b8s",
                "§6Rayon: §e6 blocs",
                "§6Dégâts: §c80%§7/s",
                "§6Feu: §c+2s§7/s",
                "",
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

        // 8.2 - HIVER ÉTERNEL
        TALENTS.add(Talent.builder()
            .id("occultiste_eternal_winter")
            .name("Hiver Éternel")
            .description("Génère une aura de givre permanente")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Génère une aura permanente de",
                "§7givre qui accumule des stacks",
                "§7et augmente vos dégâts.",
                "",
                "§6Rayon: §e6 blocs",
                "§6Stacks: §b+1§7/s",
                "§6Bonus: §c+5% §7par stack",
                "§6Maximum: §c+40%"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.FLOWER_BANNER_PATTERN)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.ETERNAL_WINTER)
            .values(new double[]{6.0, 1, 0.05, 0.40}) // radius, stacks_per_sec, damage_per_stack%, max_damage_bonus%
            .build());

        // 8.3 - MJÖLNIR (NERFÉ: 3 strikes → 2 strikes pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_mjolnir")
            .name("Mjölnir")
            .description("Les éclairs frappent deux fois")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Vos éclairs en chaîne frappent",
                "§7désormais deux fois chaque cible,",
                "§7doublant leur efficacité.",
                "",
                "§6Frappes: §e×2",
                "§6Dégâts: §c60% §7par frappe",
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

        // 8.4 - SEIGNEUR DES MORTS
        TALENTS.add(Talent.builder()
            .id("occultiste_lord_of_dead")
            .name("Seigneur des Morts")
            .description("Chance de relever les ennemis tués en serviteurs")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Chance de relever les ennemis",
                "§7tués en serviteurs morts-vivants",
                "§7qui combattent à vos côtés.",
                "",
                "§6Chance: §e75%",
                "§6Stats: §c80% §7des leurs",
                "§6Maximum: §e15",
                "§6Durée: §b30s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.LORD_OF_THE_DEAD)
            .values(new double[]{0.75, 0.80, 30000, 15}) // chance, stats%, duration_ms, max
            .build());

        // 8.5 - DÉCHIRURE DIMENSIONNELLE (Dimensional Rift)
        TALENTS.add(Talent.builder()
            .id("occultiste_dimensional_rift")
            .name("Déchirure Dimensionnelle")
            .description("Bannit automatiquement les ennemis faibles")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Les ennemis sous le seuil de PV",
                "§7sont automatiquement bannis dans",
                "§7le vide puis expulsés violemment.",
                "",
                "§6Seuil: §c15% §7PV",
                "§6Durée banissement: §b1s",
                "§6Dégâts retour: §c250%",
                "§6Dégâts AoE: §c100%",
                "",
                "§bCooldown: §f10s §7par cible"
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
        // 9.1 - PLUIE DE MÉTÉORES
        TALENTS.add(Talent.builder()
            .id("occultiste_meteor_rain")
            .name("Pluie de Météores")
            .description("Déclenche une pluie de météores dévastatrice")
            .loreLines(new String[]{
                "§c§lVOIE DU FEU",
                "",
                "§7Déclenche automatiquement une",
                "§7pluie de météores massive qui",
                "§7calcine la zone ciblée.",
                "",
                "§6Météores: §e12",
                "§6Zone: §e15 blocs",
                "§6Dégâts: §c150% §7par météore",
                "§6Feu: §c+3s",
                "",
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

        // 9.2 - STASE TEMPORELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_time_stasis")
            .name("Stase Temporelle")
            .description("Gèle le temps et tous les ennemis")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Gèle le temps et TOUS les ennemis",
                "§7dans la zone. Déclenche une Brisure",
                "§7Glaciale massive à la fin.",
                "",
                "§6Durée: §b3s",
                "§6Stacks appliqués: §b+10",
                "",
                "§bCooldown: §f90s §7| §eActivation: §fDouble Sneak"
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
            .description("Frappe tous les ennemis proches d'un éclair divin")
            .loreLines(new String[]{
                "§e§lVOIE DE LA FOUDRE",
                "",
                "§7Invoque un éclair divin qui",
                "§7frappe simultanément tous les",
                "§7ennemis dans une zone massive.",
                "",
                "§6Rayon: §e25 blocs",
                "§6Cibles max: §e30",
                "§6Dégâts: §c200% §7par cible",
                "",
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

        // 9.4 - ARMÉE IMMORTELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_immortal_army")
            .name("Armée Immortelle")
            .description("Les serviteurs deviennent immortels")
            .loreLines(new String[]{
                "§d§lVOIE DES ÂMES",
                "",
                "§7Vos serviteurs morts-vivants",
                "§7deviennent immortels et réapparaissent",
                "§7automatiquement après destruction.",
                "",
                "§6Réapparition: §b3s",
                "§6Buff stats: §c+75%"
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
            .description("Invoque un trou noir qui aspire et détruit tout")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Invoque un trou noir massif qui",
                "§7aspire tous les ennemis proches",
                "§7et les détruit progressivement.",
                "",
                "§6Rayon: §e15 blocs",
                "§6Durée: §b5s",
                "§6Dégâts initiaux: §c300%",
                "§6Dégâts/s: §c75%",
                "",
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
