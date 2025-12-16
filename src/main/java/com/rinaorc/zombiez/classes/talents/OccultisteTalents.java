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

        // 1.5 - PROJECTILE DU VIDE
        TALENTS.add(Talent.builder()
            .id("occultiste_void_bolt")
            .name("Projectile du Vide")
            .description("20% chance de projectile du vide")
            .loreLines(new String[]{
                "§7§e20%§7 de chance que votre",
                "§7attaque soit remplacee par un",
                "§5projectile du vide§7.",
                "",
                "§8Degats: §c150%§8 de base",
                "§8Traverse tous les ennemis"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.ENDER_PEARL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_BOLT)
            .values(new double[]{0.20, 1.50}) // chance, damage%
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

        // 2.5 - INSTABILITE DU VOID
        TALENTS.add(Talent.builder()
            .id("occultiste_void_instability")
            .name("Instabilite du Void")
            .description("Les Void Bolts explosent")
            .loreLines(new String[]{
                "§7Les §5Void Bolts§7 explosent",
                "§7a l'impact.",
                "",
                "§8Rayon: 3 blocs",
                "§8Degats AoE: §c80%§8 base"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.END_CRYSTAL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_INSTABILITY)
            .values(new double[]{3.0, 0.80}) // radius, aoe_damage%
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

        // 3.5 - RIFT DIMENSIONNEL
        TALENTS.add(Talent.builder()
            .id("occultiste_dimensional_rift")
            .name("Rift Dimensionnel")
            .description("Projectiles du Vide laissent une faille")
            .loreLines(new String[]{
                "§7Les §5Projectiles du Vide§7 laissent",
                "§7une faille qui tire sur les ennemis.",
                "",
                "§8Duree: 3s",
                "§8Intervalle: 1 tir/s",
                "§8Degats: §c50%§8 de base"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.CRYING_OBSIDIAN)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DIMENSIONAL_RIFT)
            .values(new double[]{3000, 1000, 0.50}) // duration_ms, tick_ms, damage%
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

        // 4.5 - ANCRE DU VIDE
        TALENTS.add(Talent.builder()
            .id("occultiste_void_anchor")
            .name("Ancre du Vide")
            .description("Les failles attirent les ennemis")
            .loreLines(new String[]{
                "§7Les §5failles dimensionnelles§7",
                "§7attirent les ennemis vers elles.",
                "",
                "§8Attraction: 30% de leur vitesse",
                "§8vers le centre"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.LODESTONE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_ANCHOR)
            .values(new double[]{0.30}) // pull_strength
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

        // 5.5 - MAITRE DU VIDE
        TALENTS.add(Talent.builder()
            .id("occultiste_void_master")
            .name("Maitre du Vide")
            .description("Detonez les failles")
            .loreLines(new String[]{
                "§7Tirez dans une §5faille§7 pour",
                "§7la faire exploser.",
                "",
                "§8Degats: §c300%§8 de base",
                "§8Rayon: 5 blocs"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.RESPAWN_ANCHOR)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_MASTER)
            .values(new double[]{3.0, 5.0}) // damage_multiplier, radius
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

        // 6.5 - CORRUPTION DU VIDE
        TALENTS.add(Talent.builder()
            .id("occultiste_void_corruption")
            .name("Corruption du Vide")
            .description("Void Bolts corrompent: -20% degats infliges")
            .loreLines(new String[]{
                "§7Les §5Void Bolts§7 corrompent",
                "§7les ennemis touches.",
                "",
                "§8Effet: §c-20%§8 degats infliges",
                "§8Duree: §a4s",
                "§8Synergie: Debuff du vide"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.CHORUS_FRUIT)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.VOID_CORRUPTION)
            .values(new double[]{0.20, 4000}) // damage_reduction%, duration_ms
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

        // 6.5 - DIMENSION CORROMPUE
        TALENTS.add(Talent.builder()
            .id("occultiste_corrupted_dimension")
            .name("Dimension Corrompue")
            .description("25% chance d'eviter les degats")
            .loreLines(new String[]{
                "§7Vous existez partiellement",
                "§7dans le §5vide§7.",
                "",
                "§8§e25%§8 de chance d'ignorer",
                "§8completement les degats"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.OBSIDIAN)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.CORRUPTED_DIMENSION)
            .values(new double[]{0.25}) // dodge_chance
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

        // 7.5 - TROU NOIR
        TALENTS.add(Talent.builder()
            .id("occultiste_black_hole")
            .name("Trou Noir")
            .description("Les failles fusionnent")
            .loreLines(new String[]{
                "§7§e3+ failles§7 proches fusionnent",
                "§7en un §5trou noir§7 devastateur.",
                "",
                "§8Degats: §c500%§8 sur 5s",
                "§8Aspire tous les ennemis"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.BLACK_CONCRETE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.BLACK_HOLE)
            .values(new double[]{3, 5.0, 5000, 5.0}) // rifts_needed, damage_multiplier, duration_ms, radius
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

        // 8.5 - EFFACEMENT
        TALENTS.add(Talent.builder()
            .id("occultiste_erasure")
            .name("Effacement")
            .description("Zone d'instakill")
            .loreLines(new String[]{
                "§7Activation: §eCrouch + Attack§7 (5s charge)",
                "§7Creez une zone de §5neant§7.",
                "",
                "§8Zone: 10 blocs, Duree: 3s",
                "§8Effet: Instakill (Boss: 70% HP)",
                "§8Cooldown: 120s"
            })
            .classType(ClassType.OCCULTISTE)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.BARRIER)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.ERASURE)
            .values(new double[]{120000, 5000, 3000, 10, 0.70}) // cooldown_ms, charge_ms, duration_ms, radius, boss_damage%
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
