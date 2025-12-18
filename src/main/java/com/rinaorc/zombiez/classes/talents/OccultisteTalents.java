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
                "§7Vos attaques ont §e25%§7 de chance",
                "§7d'§cenflammer§7 l'ennemi §e3s§7.",
                "",
                "§8Systeme §6Surchauffe§8:",
                "§8Plus un ennemi brule longtemps,",
                "§8plus il prend de degats (§c+5%/s§8)"
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
                "§7Vos attaques ont §e20%§7 de chance",
                "§7de §bgeler§7 l'ennemi et infliger",
                "§c+30%§7 degats bonus.",
                "",
                "§8Ralentissement: §b40%§8 pendant 2s",
                "§8Accumule des §3stacks de Givre§8",
                "§8Synergie: Mage de givre"
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
                "§7Vos attaques ont §e25%§7 de chance",
                "§7de declencher un §earc electrique§7.",
                "",
                "§8Cibles: §e3§8 ennemis proches",
                "§8Degats: §c60%§8 par cible",
                "§8Portee: 5 blocs"
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
                "§7Chaque elimination restaure §a3%§7",
                "§7de vos PV max, genere une",
                "§dorbe d'ame§7 (max 5) et vous",
                "§7confere §c+25%§7 degats pendant §e5s§7.",
                "",
                "§8Synergie: Mage des ames"
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
                "§7Vos attaques ont §e30%§7 de chance",
                "§7d'appliquer un §5DOT d'ombre§7.",
                "",
                "§8Degats: §c15%§8 base/s pendant §a4s",
                "§8Genere §d+5 Insanity§8 par tick",
                "§8Synergie: Shadow Priest core"
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
                "§7Les ennemis en feu propagent",
                "§7les §cflammes§7 aux ennemis proches.",
                "",
                "§8Portee: 2.5 blocs",
                "§8Enflamme: §c2s§8 par propagation",
                "§8Synergie: §6Surchauffe§8 partagee"
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
                "§7Les ennemis §bgeles§7 prennent",
                "§c+20%§7 de degats.",
                "",
                "§8Chaque §3stack de Givre§8: §c+5%§8 bonus",
                "§8Si tues pendant le gel:",
                "§8Explosion de glace (2.5 blocs)"
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
                "§7Les §eeclairs en chaine§7 peuvent",
                "§7faire des coups critiques.",
                "",
                "§8Chaque crit ajoute §e+1§8 cible",
                "§8Synergie: Mage de foudre"
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
                "§7Activation: §eS'accroupir + Attaque§7",
                "§7Consomme toutes les §dorbes§7 pour",
                "§7une explosion de degats.",
                "",
                "§8Degats par orbe: §c150%§8 de base"
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
                "§7Les ennemis avec §5Mot de l'Ombre§7",
                "§7recoivent un §d2eme DOT§7.",
                "",
                "§8Degats: §c10%§8 base/s pendant §a6s",
                "§8Vous §asoigne§8 pour §a25%§8 des degats",
                "§8infliges par vos DOTs d'ombre"
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
                "§7§e25%§7 de chance sur attaque de",
                "§7faire pleuvoir §c3 meteores§7.",
                "",
                "§8Degats: §c60%§8 par meteore",
                "§8Zone: 4 blocs, sans knockback",
                "§8Enflamme: §c+2s§8 par meteore",
                "§8Synergie: §6Surchauffe§8 acceleree"
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
                "§7Les ennemis §bgeles§7 generent",
                "§7une aura qui §bralentit§7 et",
                "§7§cblesse§7 les autres.",
                "",
                "§8Rayon aura: 2.5 blocs",
                "§8Slow: §b30%§8 + §c25%§8 degats/s",
                "§8Ajoute §3+1 stack de Givre§8/s"
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
                "§7Vous generez des §eeclairs§7",
                "§7en permanence autour de vous.",
                "",
                "§8Intervalle: Toutes les 1.5s",
                "§8Cibles: 2 ennemis proches",
                "§8Degats: §c40%§8 base, Portee: 6"
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
                "§7Chaque §dorbe d'ame§7 augmente",
                "§7vos degats de §c+8%§7.",
                "",
                "§8Max: §c+40%§8 (5 orbes)",
                "§8Max: §c+80%§8 (10 orbes avec Legion)",
                "§8Synergie: Soul stacking core"
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
                "§7Les ennemis affectes par vos",
                "§5DOTs d'ombre§7 generent des",
                "§8apparitions fantomes§7 qui les attaquent.",
                "",
                "§8Intervalle: Toutes les §a2s",
                "§8Degats: §c50%§8 de base",
                "§8Genere §d+3 Insanity§8 par hit"
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
                "§7Les ennemis en §6Surchauffe max§7",
                "§7(8s de feu) declenchent une",
                "§c§lIgnition Critique§7:",
                "",
                "§8Degats: §c25%§8 PV max",
                "§8Boss: §c12%§8 PV max",
                "§8Rayon: 3 blocs, enflamme proches",
                "§8Cooldown: §e10s§8 par ennemi"
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
                "§7Les ennemis avec §35+ stacks de Givre§7",
                "§7declenchent une §bBrisure Glaciale§7:",
                "",
                "§8Degats: §c3%§8 PV max par stack",
                "§8Boss/Elite: §c1.5%§8 PV max par stack",
                "§8Max stacks: §b20",
                "§8Cooldown: §e8s§8 par ennemi"
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
                "§7Les §eeclairs§7 vous soignent",
                "§7pour §a5%§7 des degats infliges.",
                "",
                "§8Synergie: Survie par la foudre"
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
                "§7Les §dorbes d'ames§7 regenerent",
                "§a1%§7 de vos PV/s chacune.",
                "",
                "§8Max: §a5%§8 HP/s (5 orbes)",
                "§8Synergie: Soul sustain"
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
                "§7Vos attaques §5distordent§7 l'espace.",
                "",
                "§8Effet: §7Ralentit de §e30%§7 pendant §a3s",
                "§8Bonus: §c+20%§8 degats aux ralentis",
                "§8Passif simple et efficace"
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
                "§7Vous etes entoure de §cflammes§7",
                "§7qui brulent les ennemis proches.",
                "",
                "§8Rayon: 4 blocs, sans knockback",
                "§8Degats: §c20%§8 base/s",
                "§8Enflamme: §c+1s§8/s (§6Surchauffe§8)",
                "§8Bonus: Immunite au feu"
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
                "§7Vos attaques ont §b60%§7 de",
                "§7chance de §bgeler§7.",
                "",
                "§8Duree gel: §b3s§8 (+50%)",
                "§8Ajoute §3+2 stacks§8 au lieu de 1",
                "§8Les geles subissent §c-20%§8 armure"
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
                "§7Les §echain lightning§7 n'ont",
                "§7plus de limite de cibles.",
                "",
                "§8Tous les ennemis en range",
                "§8Range chain: 6 blocs"
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
                "§7Vous pouvez stocker §d10 orbes§7.",
                "§7Chaque orbe reduit les degats de §e5%§7.",
                "",
                "§8Max reduction: §e50%§8 (10 orbes)",
                "§8Synergie: Defenseur d'ames"
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
                "§7§aPassif:§7 Quand vous tuez un ennemi,",
                "§7les mobs proches sont §5attires§7 vers le cadavre.",
                "",
                "§8Rayon: §e8§8 blocs",
                "§8Degats: §c50%§8 base aux attires",
                "§8Force d'attraction: §5Moyenne"
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
                "§7La §6Surchauffe§7 ne diminue plus",
                "§7quand l'ennemi ne brule pas.",
                "",
                "§8Kill en feu: §c60%§8 degats AoE",
                "§8Rayon: §e3§8 blocs, sans knockback",
                "§8Enflamme: §c3s§8 les proches"
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
                "§7Les §3stacks de Givre§7 ne",
                "§7disparaissent plus avec le temps.",
                "",
                "§8Les geles propagent §3+1 stack§8/s",
                "§8aux ennemis dans §e3§8 blocs",
                "§8Slow par stack: §b5%§8 (max 50%)"
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
                "§7Les §eeclairs§7 creent des zones",
                "§7electriques pendant §a2s§7.",
                "",
                "§8Degats: §c20%§8/s",
                "§8Rayon zone: §e2§8 blocs",
                "§8Synergie: Controle de zone"
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
                "§7Chaque §dorbe d'ame§7 absorbe",
                "§7§a5%§7 des degats que vous recevez.",
                "",
                "§8Max: §a25%§8 (5 orbes)",
                "§8L'orbe disparait apres 50 PV absorbes",
                "§8Synergie: Defenseur d'ames"
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
                "§7§aPassif:§7 Toutes les §e15s§7 en combat,",
                "§7un §5puits de gravite§7 apparait a votre position.",
                "",
                "§8Duree: §a6s§8, Rayon: §e5§8 blocs",
                "§8Ralentit de §e50%§8 les ennemis",
                "§8Attire lentement vers le centre",
                "§8Degats: §c30%§8 base/s"
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
                "§7Toutes les §e12s§7, une vague",
                "§7de §cfeu§7 emane de vous.",
                "",
                "§8Degats: §c150%§8 base, sans knockback",
                "§8Rayon: 5 blocs",
                "§8Enflamme: §c+4s§8 (§6Surchauffe§8 rapide)"
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
                "§7La mort d'un ennemi avec §35+",
                "§3stacks§7 cree une zone de givre.",
                "",
                "§8Duree: §b4s§8, Rayon: §e2.5§8 blocs",
                "§8Ennemis dedans: §3+2 stacks§8/s",
                "§8Slow zone: §b35%§8 + §c50%§8 degats/s"
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
                "§7Vous etes entoure d'une",
                "§etempete electrique§7 permanente.",
                "",
                "§8Rayon: 5 blocs",
                "§8Tick: Toutes les 1s",
                "§8Cibles: 3 random, §c25%§8 dmg"
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
                "§7Activation: §eS'accroupir + Attaque§7",
                "§7Depensez §d1 orbe§7 pour invoquer",
                "§7un §8squelette archer§7 puissant.",
                "",
                "§8Stats: §c120%§8 des votres",
                "§8Duree: §e30s§8, Max: §a8§8 squelettes",
                "§8Activation: §eSneak + Clic Droit"
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
                "§7§aPassif:§7 Tuer §e3+ ennemis§7 en §a5s§7",
                "§7declenche une §5singularite§7 devastatrice!",
                "",
                "§8Rayon: §e10§8 blocs, Duree: §a3s",
                "§8Aspire §cviolemment§8 les ennemis",
                "§8Degats: §c200%§8 base + §c50%/s",
                "§8Cooldown interne: §e8s"
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
                "§7Invoquez un §csoleil de feu§7",
                "§7qui brule tout pendant §e8s§7.",
                "",
                "§8Cooldown: §e35s§8",
                "§8Degats: §c80%§8/s, sans knockback",
                "§8Rayon: 6 blocs",
                "§8Enflamme: §c+2s§8/s (§6Surchauffe§8 max)"
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
                "§7Aura permanente de froid glacial.",
                "",
                "§8Rayon: §e6§8 blocs",
                "§8Effet: §3+1 stack§8/s a tous",
                "§8Bonus: §c+5%§8 degats par stack",
                "§8Max bonus: §c+40%§8 degats"
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
                "§7Vos §echain lightning§7 frappent",
                "§c2 fois§7 chaque cible.",
                "",
                "§8Degats: 60% x2 = §c120%§8 total",
                "§8Synergie: Lightning legendary"
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
                "§7Les ennemis tues ont §e75%§7",
                "§7de chance de revenir comme",
                "§7vos §5serviteurs morts-vivants§7.",
                "",
                "§8Stats: §c80%§8 des leurs",
                "§8Duree: §e30s§8, Max: §a15§8 serviteurs",
                "§8Aura de terreur et vie volee"
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
                "§7§aPassif:§7 Les ennemis en dessous de §c15% HP§7",
                "§7sont §5bannis dans le vide§7 automatiquement!",
                "",
                "§8Duree du bannissement: §a1s",
                "§8Degats a la sortie: §c250%§8 base",
                "§8Explosion du vide: §5100%§8 base AoE",
                "§8Les bannis sont §8immobilises",
                "§8Cooldown par cible: §e10s"
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
                "§7Toutes les §e45s§7, une pluie",
                "§7de §c12 meteores§7 devastateurs.",
                "",
                "§8Degats: §c150%§8 chacun, sans knockback",
                "§8Zone: 15 blocs (max)",
                "§8Impact: §c+3s§8 feu + §cIgnition§8 auto"
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
                "§7Activation: §eDouble Sneak§7",
                "§7Gelez TOUS les ennemis §b3s§7",
                "§7et appliquez §310 stacks§7 a chacun.",
                "",
                "§8A la fin: §bBrisure Glaciale§8 auto",
                "§8Cooldown: §e120s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.CLOCK)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.TIME_STASIS)
            .values(new double[]{120000, 3000, 10}) // cooldown_ms, duration_ms, stacks_applied
            .build());

        // 9.3 - JUGEMENT DIVIN (NERFÉ: 300% → 200% pour équilibrage)
        TALENTS.add(Talent.builder()
            .id("occultiste_divine_judgment")
            .name("Jugement Divin")
            .description("Eclair sur TOUS les ennemis proches")
            .loreLines(new String[]{
                "§7Toutes les §e30s§7, un eclair",
                "§7divin frappe §cTOUS§7 les ennemis.",
                "",
                "§8Rayon: §e25§8 blocs, sans knockback",
                "§8Degats: §c200%§8 a chacun",
                "§8Max cibles: §e30"
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
                "§7Vos serviteurs sont §dimmortels§7!",
                "§7Ils respawn §e3s§7 apres leur mort.",
                "",
                "§8Buff serviteurs: §c+75%§8 stats totales",
                "§8Serviteurs plus resistants et agressifs",
                "§8Ultime: Armee de morts-vivants"
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
                "§7§aPassif:§7 Toutes les §e45s§7 en combat,",
                "§7un §5TROU NOIR MASSIF§7 apparait!",
                "",
                "§8Rayon: §e15§8 blocs, Duree: §a5s",
                "§8Aspire §cTOUS§8 les mobs environnants",
                "§8Degats: §c300%§8 + §c75%/s§8 aux aspires",
                "§8Effet visuel §5spectaculaire§8!"
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
