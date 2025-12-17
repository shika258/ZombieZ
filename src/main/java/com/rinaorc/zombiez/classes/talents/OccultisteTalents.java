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
            .description("25% chance d'enflammer l'ennemi")
            .loreLines(new String[]{
                "§7Vos attaques ont §e25%§7 de chance",
                "§7d'§cenflammer§7 l'ennemi.",
                "",
                "§8Degats: §c50%§8 sur 3s",
                "§8Synergie: Fire mage core"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.BLAZE_POWDER)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.IGNITE)
            .values(new double[]{0.25, 0.50, 3.0}) // chance, damage%, duration_s
            .internalCooldownMs(500)
            .build());

        // 1.2 - GIVRE MORDANT
        TALENTS.add(Talent.builder()
            .id("occultiste_frost_bite")
            .name("Givre Mordant")
            .description("25% chance de geler l'ennemi")
            .loreLines(new String[]{
                "§7Vos attaques ont §e25%§7 de chance",
                "§7de §bgeler§7 l'ennemi.",
                "",
                "§8Ralentissement: §b50%§8 pendant 2s",
                "§8Synergie: Mage de givre"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.BLUE_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.FROST_BITE)
            .values(new double[]{0.25, 0.50, 2.0}) // chance, slow%, duration_s
            .internalCooldownMs(500)
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

        // 1.4 - SIPHON D'AME
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_siphon")
            .name("Siphon d'Ame")
            .description("Elimination = 3% PV + orbe d'ame")
            .loreLines(new String[]{
                "§7Chaque elimination restaure §a3%§7",
                "§7de vos PV max et genere une",
                "§dorbe d'ame§7 (max 5).",
                "",
                "§8Synergie: Mage des ames"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.SOUL_LANTERN)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_SIPHON)
            .values(new double[]{0.03, 5}) // heal%, max_orbs
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
                "§7Les ennemis en feu §cenflamment§7",
                "§7les ennemis proches.",
                "",
                "§8Portee: 2 blocs",
                "§8Verification: Chaque seconde"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.FIRE_SPREAD)
            .values(new double[]{2.0}) // range
            .build());

        // 2.2 - COEUR DE GLACE
        TALENTS.add(Talent.builder()
            .id("occultiste_frozen_heart")
            .name("Coeur de Glace")
            .description("Geles prennent +30% degats + brisure")
            .loreLines(new String[]{
                "§7Les ennemis §bgeles§7 prennent",
                "§c+30%§7 de degats.",
                "",
                "§8Si tues pendant le gel:",
                "§8Explosion de glace (3 blocs)"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.PRISMARINE_CRYSTALS)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.FROZEN_HEART)
            .values(new double[]{0.30, 3.0}) // bonus_damage%, shatter_radius
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

        // 2.4 - RESERVOIR D'AMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_reservoir")
            .name("Reservoir d'Ames")
            .description("Consommez les ames pour explosion")
            .loreLines(new String[]{
                "§7Activation: §eS'accroupir + Attaque§7",
                "§7Consomme toutes les §dorbes§7 pour",
                "§7une explosion de degats.",
                "",
                "§8Degats par orbe: §c100%§8 de base"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.NETHER_STAR)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_RESERVOIR)
            .values(new double[]{1.0}) // damage_per_orb%
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
            .description("30% chance de pluie de meteores")
            .loreLines(new String[]{
                "§7§e30%§7 de chance sur attaque de",
                "§7faire pleuvoir §c3 meteores§7.",
                "",
                "§8Degats: §c80%§8 par meteore",
                "§8Zone: 5 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.MAGMA_BLOCK)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FIRESTORM)
            .values(new double[]{0.30, 3, 0.80, 5}) // chance, meteors, damage%, zone
            .internalCooldownMs(2000)
            .build());

        // 3.2 - BLIZZARD
        TALENTS.add(Talent.builder()
            .id("occultiste_blizzard")
            .name("Blizzard")
            .description("Les geles creent une aura de froid")
            .loreLines(new String[]{
                "§7Les ennemis §bgeles§7 generent",
                "§7une aura qui gele les autres.",
                "",
                "§8Rayon aura: 3 blocs",
                "§8Duree: Tant qu'ils sont geles"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.POWDER_SNOW_BUCKET)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.BLIZZARD)
            .values(new double[]{3.0}) // aura_radius
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

        // 3.4 - PACTE DES AMES
        TALENTS.add(Talent.builder()
            .id("occultiste_soul_pact")
            .name("Pacte des Ames")
            .description("+5% degats par orbe d'ame")
            .loreLines(new String[]{
                "§7Chaque §dorbe d'ame§7 augmente",
                "§7vos degats de §c+5%§7.",
                "",
                "§8Max: §c+25%§8 (5 orbes)",
                "§8Synergie: Soul stacking core"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.AMETHYST_SHARD)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.SOUL_PACT)
            .values(new double[]{0.05}) // damage_per_orb%
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
        // 4.1 - PHOENIX
        TALENTS.add(Talent.builder()
            .id("occultiste_phoenix_flame")
            .name("Phoenix")
            .description("30% chance d'explosion sur kill feu")
            .loreLines(new String[]{
                "§7Les ennemis tues par le §cfeu§7",
                "§7ont §e30%§7 de chance d'exploser.",
                "",
                "§8Degats: §c150%§8 base",
                "§8Rayon: 4 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.BLAZE_ROD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.PHOENIX_FLAME)
            .values(new double[]{0.30, 1.50, 4.0}) // chance, damage%, radius
            .build());

        // 4.2 - ZERO ABSOLU
        TALENTS.add(Talent.builder()
            .id("occultiste_absolute_zero")
            .name("Zero Absolu")
            .description("Gel 3s+ = instakill ou 500% degats")
            .loreLines(new String[]{
                "§7Ennemis geles §b3s+§7:",
                "§7- Petits: §cInstakill",
                "§7- Gros/Boss: §c500%§7 degats burst",
                "",
                "§8Synergie: Frost execute"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ABSOLUTE_ZERO)
            .values(new double[]{3000, 5.0}) // freeze_time_ms, boss_damage_multiplier
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

        // 4.5 - FOLIE (Insanity)
        TALENTS.add(Talent.builder()
            .id("occultiste_insanity")
            .name("Folie")
            .description("L'Insanity augmente vos degats d'ombre")
            .loreLines(new String[]{
                "§7Votre §dInsanity§7 accumulee",
                "§7augmente tous vos §5degats d'ombre§7.",
                "",
                "§8Bonus: §c+1%§8 degats par point",
                "§8Max: §d100 Insanity§8 = §c+100%§8 degats",
                "§8L'Insanity decay §c-2/s§8 hors combat"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.INSANITY)
            .values(new double[]{0.01, 100, 2}) // damage_per_insanity%, max_insanity, decay_per_second
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
                "§8Rayon: 3 blocs",
                "§8Degats: §c30%§8 base/s",
                "§8Bonus: Immunite au feu"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.FIRE_CORAL)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FIRE_AVATAR)
            .values(new double[]{3.0, 0.30}) // radius, damage_per_second%
            .build());

        // 5.2 - SEIGNEUR DU GIVRE
        TALENTS.add(Talent.builder()
            .id("occultiste_frost_lord")
            .name("Seigneur du Givre")
            .description("100% freeze, duree doublee")
            .loreLines(new String[]{
                "§7Vos attaques ont §b100%§7 de",
                "§7chance de §bgeler§7.",
                "",
                "§8Duree gel: §bx2§8 (4s)",
                "§8Synergie: Frost perma-CC"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.PACKED_ICE)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.FROST_LORD)
            .values(new double[]{1.0, 4.0}) // freeze_chance, duration_s
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

        // 5.5 - EXPLOSION DU VIDE (Void Eruption)
        TALENTS.add(Talent.builder()
            .id("occultiste_void_eruption")
            .name("Explosion du Vide")
            .description("Explosion massive + entree en Voidform")
            .loreLines(new String[]{
                "§7Activation: §eCrouch + Attaque§7",
                "§7Necessite §d50+ Insanity§7.",
                "",
                "§8Explosion: §c200%§8 + §c5%§8 par Insanity",
                "§8Rayon: §e6§8 blocs",
                "§8Entre en §5Voidform§8 pendant §a8s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.END_CRYSTAL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_ERUPTION)
            .values(new double[]{50, 2.0, 0.05, 6.0, 8000}) // min_insanity, base_damage%, damage_per_insanity%, radius, voidform_duration_ms
            .build());
    }

    // ==================== PALIER 6 - NIVEAU 25 (Ascension) ====================

    private static void registerTier6Talents() {
        // 6.1 - PYROCLASME
        TALENTS.add(Talent.builder()
            .id("occultiste_pyroclasm")
            .name("Pyroclasme")
            .description("Kill feu: 50% chance explosion en chaine")
            .loreLines(new String[]{
                "§7Les ennemis tues par le feu ont",
                "§7§e50%§7 de chance d'exploser et",
                "§7d'§cenflammer§7 les ennemis proches.",
                "",
                "§8Degats: §c100%§8 de base",
                "§8Rayon: §e3§8 blocs",
                "§8Peut se propager!"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.MAGMA_BLOCK)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.PYROCLASM)
            .values(new double[]{0.50, 1.0, 3.0}) // chance, damage%, radius
            .build());

        // 6.2 - PERMAFROST
        TALENTS.add(Talent.builder()
            .id("occultiste_permafrost")
            .name("Permafrost")
            .description("Geles ralentissent les ennemis autour d'eux")
            .loreLines(new String[]{
                "§7Les ennemis §bgeles§7 emettent",
                "§7une aura de froid qui §bralentit§7",
                "§7les autres de §b-40%§7.",
                "",
                "§8Rayon: §e2.5§8 blocs",
                "§8Synergie: Controle de zone"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.PACKED_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.PERMAFROST)
            .values(new double[]{0.40, 2.5}) // slow%, radius
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

        // 6.5 - FORME DU VIDE (Voidform)
        TALENTS.add(Talent.builder()
            .id("occultiste_voidform")
            .name("Forme du Vide")
            .description("Etat de transformation puissant")
            .loreLines(new String[]{
                "§7En §5Voidform§7, vous gagnez:",
                "",
                "§8• §c+30%§8 degats d'ombre",
                "§8• DOTs tick §a50%§8 plus vite",
                "§8• §d+2 Insanity§8 par seconde",
                "",
                "§8Drain: §c-5 Insanity/s§8",
                "§8Sort a §d0 Insanity"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.CRYING_OBSIDIAN)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOIDFORM)
            .values(new double[]{0.30, 0.50, 2, 5}) // damage_bonus%, dot_speed_bonus%, insanity_gain/s, insanity_drain/s
            .build());
    }

    // ==================== PALIER 7 - NIVEAU 30 (Transcendance) ====================

    private static void registerTier7Talents() {
        // 6.1 - INFERNO
        TALENTS.add(Talent.builder()
            .id("occultiste_inferno")
            .name("Inferno")
            .description("Nova de feu toutes les 10s")
            .loreLines(new String[]{
                "§7Toutes les §e10s§7, une vague",
                "§7de §cfeu§7 emane de vous.",
                "",
                "§8Degats: §c200%§8 base",
                "§8Rayon: 6 blocs",
                "§8Enflamme tous les touches"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.MAGMA_CREAM)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.INFERNO)
            .values(new double[]{10000, 2.0, 6.0}) // cooldown_ms, damage%, radius
            .build());

        // 6.2 - ERE GLACIAIRE
        TALENTS.add(Talent.builder()
            .id("occultiste_ice_age")
            .name("Ere Glaciaire")
            .description("Zones de gel persistantes")
            .loreLines(new String[]{
                "§7Les zones de gel persistent",
                "§7au sol §b5s§7 apres la mort",
                "§7d'un ennemi gele.",
                "",
                "§8Gele les ennemis qui passent",
                "§8Rayon: 2 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.BLUE_STAINED_GLASS)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ICE_AGE)
            .values(new double[]{5000, 2.0}) // duration_ms, radius
            .build());

        // 6.3 - TEMPETE PERPETUELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_perpetual_storm")
            .name("Tempete Perpetuelle")
            .description("Tempete electrique permanente")
            .loreLines(new String[]{
                "§7Vous etes entoure d'une",
                "§etempete electrique§7 permanente.",
                "",
                "§8Rayon: 5 blocs",
                "§8Tick: Toutes les 0.5s",
                "§8Cibles: 3 random, §c25%§8 dmg"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.PRISMARINE_SHARD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.PERPETUAL_STORM)
            .values(new double[]{500, 5.0, 3, 0.25}) // tick_ms, radius, targets, damage%
            .build());

        // 6.4 - NECROMANCIEN
        TALENTS.add(Talent.builder()
            .id("occultiste_necromancer")
            .name("Necromancien")
            .description("Invoquez des squelettes")
            .loreLines(new String[]{
                "§7Les §dorbes§7 peuvent etre",
                "§7depensees pour invoquer des",
                "§8squelettes§7 (1 orbe = 1 squelette).",
                "",
                "§8Stats: 50% des votres",
                "§8Duree: 10s, Max: 5"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.NECROMANCER)
            .values(new double[]{0.50, 10000, 5}) // stats%, duration_ms, max_summons
            .build());

        // 7.5 - TOURMENT PSYCHIQUE (Psychic Horror)
        TALENTS.add(Talent.builder()
            .id("occultiste_psychic_horror")
            .name("Tourment Psychique")
            .description("Terrorisez periodiquement les ennemis")
            .loreLines(new String[]{
                "§7Toutes les §e8s§7, vous emettez",
                "§7une vague de §5terreur psychique§7.",
                "",
                "§8Rayon: §e5§8 blocs",
                "§8Effet: §cStun 2s§8 + §c-30%§8 degats",
                "§8Duree debuff: §a4s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.SCULK_SHRIEKER)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.PSYCHIC_HORROR)
            .values(new double[]{8000, 5.0, 2000, 0.30, 4000}) // cooldown_ms, radius, stun_ms, damage_reduction%, debuff_duration_ms
            .build());
    }

    // ==================== PALIER 8 - NIVEAU 40 (Apex) ====================

    private static void registerTier8Talents() {
        // 7.1 - SOLEIL NOIR
        TALENTS.add(Talent.builder()
            .id("occultiste_black_sun")
            .name("Soleil Noir")
            .description("Invoquez un soleil de feu")
            .loreLines(new String[]{
                "§7Invoquez un §csoleil de feu§7",
                "§7qui brule tout pendant §e10s§7.",
                "",
                "§8Cooldown: 30s",
                "§8Degats: §c100%§8/s",
                "§8Rayon: 8 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.SUNFLOWER)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BLACK_SUN)
            .values(new double[]{30000, 10000, 1.0, 8.0}) // cooldown, duration, damage%/s, radius
            .build());

        // 7.2 - HIVER ETERNEL
        TALENTS.add(Talent.builder()
            .id("occultiste_eternal_winter")
            .name("Hiver Eternel")
            .description("Slow 70% permanent + bonus degats")
            .loreLines(new String[]{
                "§7Les ennemis dans votre zone",
                "§7sont §bralentis de 70%§7.",
                "",
                "§8Rayon: 8 blocs",
                "§8Bonus: §c+50%§8 degats aux slowed"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.FLOWER_BANNER_PATTERN)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.ETERNAL_WINTER)
            .values(new double[]{8.0, 0.70, 0.50}) // radius, slow%, damage_bonus%
            .build());

        // 7.3 - MJOLNIR
        TALENTS.add(Talent.builder()
            .id("occultiste_mjolnir")
            .name("Mjolnir")
            .description("Chain lightning x3 strikes")
            .loreLines(new String[]{
                "§7Vos §echain lightning§7 frappent",
                "§c3 fois§7 chaque cible.",
                "",
                "§8Degats: 60% x3 = §c180%§8 total",
                "§8Synergie: Lightning legendary"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.TRIDENT)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.MJOLNIR)
            .values(new double[]{3, 0.60}) // strikes, damage_per_strike%
            .build());

        // 7.4 - SEIGNEUR DES MORTS
        TALENTS.add(Talent.builder()
            .id("occultiste_lord_of_dead")
            .name("Seigneur des Morts")
            .description("50% chance de relever les morts")
            .loreLines(new String[]{
                "§7Les ennemis tues ont §e50%§7",
                "§7de chance de revenir comme",
                "§7vos §8serviteurs§7.",
                "",
                "§8Stats: 30% des leurs",
                "§8Duree: 15s, Max: 10"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.LORD_OF_THE_DEAD)
            .values(new double[]{0.50, 0.30, 15000, 10}) // chance, stats%, duration_ms, max
            .build());

        // 8.5 - DEVOREUR DU VIDE (Voidling/Shadowfiend)
        TALENTS.add(Talent.builder()
            .id("occultiste_voidling")
            .name("Devoreur du Vide")
            .description("Invoquez une entite du vide")
            .loreLines(new String[]{
                "§7Activation: §eCrouch + Jump§7",
                "§7Invoquez un §5Devoreur du Vide§7",
                "§7qui attaque vos ennemis.",
                "",
                "§8Degats: §c80%§8 base/attaque",
                "§8Genere §d+5 Insanity§8 par attaque",
                "§8Duree: §a15s§8, Cooldown: §e45s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.ENDERMAN_SPAWN_EGG)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOIDLING)
            .values(new double[]{45000, 15000, 0.80, 5}) // cooldown_ms, duration_ms, damage%, insanity_per_hit
            .build());
    }

    // ==================== PALIER 9 - NIVEAU 50 (Legendaire) ====================

    private static void registerTier9Talents() {
        // 8.1 - PLUIE DE METEORES
        TALENTS.add(Talent.builder()
            .id("occultiste_meteor_rain")
            .name("Pluie de Meteores")
            .description("20 meteores toutes les 60s")
            .loreLines(new String[]{
                "§7Toutes les §e60s§7, une pluie",
                "§7de §c20 meteores§7 devastateurs.",
                "",
                "§8Degats: §c200%§8 chacun",
                "§8Zone: 20 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(0)
            .icon(Material.DRAGON_EGG)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.METEOR_RAIN)
            .values(new double[]{60000, 20, 2.0, 20}) // cooldown_ms, meteors, damage%, zone
            .build());

        // 8.2 - STASE TEMPORELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_time_stasis")
            .name("Stase Temporelle")
            .description("Gelez le temps 5s")
            .loreLines(new String[]{
                "§7Activation: §eCrouch + Jump§7",
                "§7Gelez le temps pour tous",
                "§7les ennemis pendant §b5s§7.",
                "",
                "§8Cooldown: 90s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.CLOCK)
            .iconColor("§3")
            .effectType(Talent.TalentEffectType.TIME_STASIS)
            .values(new double[]{90000, 5000}) // cooldown_ms, duration_ms
            .build());

        // 8.3 - JUGEMENT DIVIN
        TALENTS.add(Talent.builder()
            .id("occultiste_divine_judgment")
            .name("Jugement Divin")
            .description("Eclair sur TOUS les ennemis")
            .loreLines(new String[]{
                "§7Toutes les §e30s§7, un eclair",
                "§7divin frappe §cTOUS§7 les ennemis.",
                "",
                "§8Degats: §c300%§8 a chacun",
                "§8Synergie: Ultimate lightning"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(2)
            .icon(Material.NETHER_STAR)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.DIVINE_JUDGMENT)
            .values(new double[]{30000, 3.0}) // cooldown_ms, damage%
            .build());

        // 8.4 - ARMEE IMMORTELLE
        TALENTS.add(Talent.builder()
            .id("occultiste_immortal_army")
            .name("Armee Immortelle")
            .description("Serviteurs immortels + buff")
            .loreLines(new String[]{
                "§7Vos serviteurs sont §dimmortels§7",
                "§7et respawn §e5s§7 apres leur mort.",
                "",
                "§8Buff serviteurs: §c+50%§8 stats",
                "§8Synergie: Ultimate soul fantasy"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(3)
            .icon(Material.TOTEM_OF_UNDYING)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.IMMORTAL_ARMY)
            .values(new double[]{5000, 0.50}) // respawn_ms, stat_buff%
            .build());

        // 9.5 - ASCENSION SOMBRE (Dark Ascension)
        TALENTS.add(Talent.builder()
            .id("occultiste_dark_ascension")
            .name("Ascension Sombre")
            .description("Ultimate: Voidform instantanee + reset DOTs")
            .loreLines(new String[]{
                "§7Activation: §eCrouch + Jump§7 (maintenu 2s)",
                "§7Ascendez dans le §5vide§7!",
                "",
                "§8• Insanity §dreset a 100§8",
                "§8• Entre en §5Voidform§8 immediatement",
                "§8• Reset tous les DOTs actifs",
                "§8• §c+50%§8 duree DOT pendant §a10s",
                "§8Cooldown: §e90s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.NETHER_STAR)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DARK_ASCENSION)
            .values(new double[]{90000, 2000, 100, 10000, 0.50}) // cooldown_ms, charge_ms, insanity_set, buff_duration_ms, dot_duration_bonus%
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
