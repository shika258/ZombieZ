package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registre de tous les talents du Chasseur
 * 45 talents au total, 5 par palier sur 9 paliers
 * Identite: DPS a distance, critiques devastateurs, mobilite et precision
 */
public final class ChasseurTalents {

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
        // 1.1 - TIRS MULTIPLES
        TALENTS.add(Talent.builder()
            .id("chasseur_multi_shot")
            .name("Tirs Multiples")
            .description("25% chance de tirer 3 fleches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos tirs a l'arc ou l'arbalete",
                "§7ont §e25%§7 de chance de tirer",
                "§e3 fleches§7 horizontales!",
                "",
                "§8► Pattern: §fI I I",
                "§8► Degats par fleche: §c100%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.ARROW)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.MULTI_SHOT)
            .values(new double[]{0.25, 2}) // chance, extra_projectiles (2 bonus = 3 total)
            .build());

        // 1.2 - OEIL DE LYNX
        TALENTS.add(Talent.builder()
            .id("chasseur_lynx_eye")
            .name("Oeil de Lynx")
            .description("+25% chance critique, +30% degats critiques")
            .loreLines(new String[]{
                "§7Votre precision est accrue:",
                "§7- §e+25%§7 chance de critique",
                "§7- §c+30%§7 degats critiques",
                "",
                "§8Vision de predateur"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.ENDER_EYE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.LYNX_EYE)
            .values(new double[]{0.25, 0.30}) // crit_chance, crit_damage_bonus
            .build());

        // 1.3 - CHASSEUR AGILE
        TALENTS.add(Talent.builder()
            .id("chasseur_agile_hunter")
            .name("Chasseur Agile")
            .description("Esquive = invisible 1s + 50% degats")
            .loreLines(new String[]{
                "§7Esquiver (double appui direction)",
                "§7vous rend §binvisible 1s§7 et augmente",
                "§7les degats suivants de §c+50%§7.",
                "",
                "§8Temps de recharge: 3s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(2)
            .icon(Material.FEATHER)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.AGILE_HUNTER)
            .values(new double[]{3000, 1000, 0.50}) // dodge_cooldown_ms, invis_duration_ms, damage_boost
            .internalCooldownMs(3000)
            .build());

        // 1.4 - MARQUE DU CHASSEUR
        TALENTS.add(Talent.builder()
            .id("chasseur_hunter_mark")
            .name("Marque du Chasseur")
            .description("Marquez les ennemis: +15% degats")
            .loreLines(new String[]{
                "§7Vos attaques §emarquent§7 les ennemis.",
                "§7Les ennemis marques prennent",
                "§7§c+15%§7 de degats de toutes sources.",
                "",
                "§8Duree: 5s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.TARGET)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.HUNTER_MARK)
            .values(new double[]{5000, 0.15}) // duration_ms, damage_amp
            .build());

        // 1.5 - FLECHES PERCANTES
        TALENTS.add(Talent.builder()
            .id("chasseur_piercing_arrows")
            .name("Fleches Percantes")
            .description("Projectiles traversent 1 ennemi")
            .loreLines(new String[]{
                "§7Vos projectiles §atraversent§7",
                "§7le premier ennemi touche.",
                "",
                "§8Degats second: §c80%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.SPECTRAL_ARROW)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.PIERCING_ARROWS)
            .values(new double[]{1, 0.80}) // pierce_count, second_damage%
            .build());
    }

    // ==================== PALIER 2 - NIVEAU 5 (Amplification) ====================

    private static void registerTier2Talents() {
        // 2.1 - RAFALE (Flèches Traqueuses + Combo)
        TALENTS.add(Talent.builder()
            .id("chasseur_burst_shot")
            .name("Rafale")
            .description("Fleches chercheuses de tete + combo x2")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§6FLECHES CHERCHEUSES:",
                "§7Vos fleches §etraquent§7 la §ctête§7",
                "§7des ennemis proches!",
                "",
                "§6COMBO §c(x2 DEGATS!)§6:",
                "§7Toucher §e3x§7 la meme cible →",
                "§7Le 4eme tir fait §c+100%§7 degats!",
                "",
                "§8► Rayon de traque: §f7 blocs",
                "§8► Verrouillage de cible intelligent",
                "§8► S'applique aux pluies de fleches!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.TIPPED_ARROW)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BURST_SHOT)
            .values(new double[]{3, 1.0, 0.30, 7.0}) // hits_needed, bonus_damage%, homing_strength, homing_radius
            .build());

        // 2.2 - SNIPER
        TALENTS.add(Talent.builder()
            .id("chasseur_sniper")
            .name("Sniper")
            .description("Distance = degats (+50% max a 15 blocs)")
            .loreLines(new String[]{
                "§7Plus la cible est loin, plus",
                "§7les degats sont eleves.",
                "",
                "§8+3.33%/bloc au-dela de 5 blocs",
                "§8Max: §c+50%§8 a 15+ blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.SPYGLASS)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SNIPER)
            .values(new double[]{5, 0.0333, 0.50, 15}) // min_dist, bonus_per_block, max_bonus, max_dist
            .build());

        // 2.3 - FANTOME
        TALENTS.add(Talent.builder()
            .id("chasseur_ghost")
            .name("Fantome")
            .description("3s+ invisible: +100% degats critiques")
            .loreLines(new String[]{
                "§7Rester invisible §e3s+§7 confere",
                "§7§c+100%§7 degats critique sur",
                "§7la premiere attaque!",
                "",
                "§8Assassin furtif"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.GHOST)
            .values(new double[]{3000, 1.0}) // invis_time_required_ms, crit_damage_bonus
            .build());

        // 2.4 - VENIN
        TALENTS.add(Talent.builder()
            .id("chasseur_venom")
            .name("Venin")
            .description("Poison: 40% degats sur 3s")
            .loreLines(new String[]{
                "§7Vos attaques §2empoisonnent§7,",
                "§7infligeant §c40%§7 degats bonus",
                "§7sur §a3s§7.",
                "",
                "§8Cumulable!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.VENOM)
            .values(new double[]{0.40, 3000}) // damage%, duration_ms
            .build());

        // 2.5 - RICOCHET
        TALENTS.add(Talent.builder()
            .id("chasseur_ricochet")
            .name("Ricochet")
            .description("25% chance rebond sur ennemi proche")
            .loreLines(new String[]{
                "§7Les projectiles ont §e25%§7 de",
                "§7chance de rebondir sur un",
                "§7ennemi proche.",
                "",
                "§8Degats ricochet: §c70%",
                "§8Portee: §e5§8 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.SLIME_BALL)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.RICOCHET)
            .values(new double[]{0.25, 0.70, 5.0}) // chance, damage%, range
            .build());
    }

    // ==================== PALIER 3 - NIVEAU 10 (Specialisation) ====================

    private static void registerTier3Talents() {
        // 3.1 - PLUIE DE FLECHES
        TALENTS.add(Talent.builder()
            .id("chasseur_arrow_rain")
            .name("Pluie de Fleches")
            .description("25% chance pluie AoE")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos tirs ont §e25%§7 de chance",
                "§7d'invoquer une §bpluie de fleches§7!",
                "",
                "§8► Fleches: §e8",
                "§8► Degats/fleche: §c30%",
                "§8► Zone: §e4x4§8 blocs",
                "§8► Cooldown: §e2s",
                "",
                "§6§lAMELIORABLE:",
                "§8Deluge, Furie, Cyclone, Nuee..."
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.BOW)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.ARROW_RAIN)
            .values(new double[]{0.25, 0.30, 8, 4.0}) // chance, damage_per_arrow%, arrows, radius
            .internalCooldownMs(2000)
            .build());

        // 3.2 - OEIL DU PREDATEUR
        TALENTS.add(Talent.builder()
            .id("chasseur_predator_eye")
            .name("Oeil du Predateur")
            .description("Critique = 30% chance reinit. esquive")
            .loreLines(new String[]{
                "§7Les critiques ont §e30%§7 de",
                "§7chance de reinitialiser le temps",
                "§7de recharge de l'esquive!",
                "",
                "§8Hyper mobilite"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.CONDUIT)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.PREDATOR_EYE)
            .values(new double[]{0.30}) // reset_chance
            .build());

        // 3.3 - TRAQUEUR
        TALENTS.add(Talent.builder()
            .id("chasseur_tracker")
            .name("Traqueur")
            .description("Marques: +25% degats, vision murs")
            .loreLines(new String[]{
                "§7Les ennemis marques sont",
                "§7visibles a travers les murs",
                "§7et prennent §c+25%§7 degats.",
                "",
                "§8(au lieu de +15%)"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.RECOVERY_COMPASS)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.TRACKER)
            .values(new double[]{0.25}) // damage_amp (replaces base 15%)
            .build());

        // 3.4 - TOXINES MORTELLES
        TALENTS.add(Talent.builder()
            .id("chasseur_deadly_toxins")
            .name("Toxines Mortelles")
            .description("Poison peut faire des critiques + ralentit -30%")
            .loreLines(new String[]{
                "§7Le poison peut §ecritiquer§7!",
                "§7Les ennemis empoisonnes ont",
                "§7§c-30%§7 vitesse.",
                "",
                "§8Controle total"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.POISONOUS_POTATO)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.DEADLY_TOXINS)
            .values(new double[]{0.30}) // slow%
            .build());

        // 3.5 - TIREUR D'ELITE
        TALENTS.add(Talent.builder()
            .id("chasseur_sharpshooter")
            .name("Tireur d'Elite")
            .description("Immobile 1.5s = critique garanti")
            .loreLines(new String[]{
                "§7Rester immobile §e1.5s§7 garantit",
                "§7un §ecritique§7 sur le prochain tir!",
                "",
                "§8Precision absolue"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.CROSSBOW)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SHARPSHOOTER)
            .values(new double[]{1500}) // still_time_ms
            .build());
    }

    // ==================== PALIER 4 - NIVEAU 15 (Evolution) ====================

    private static void registerTier4Talents() {
        // 4.1 - DELUGE
        TALENTS.add(Talent.builder()
            .id("chasseur_deluge")
            .name("Deluge")
            .description("+3 vagues, +50% fleches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos pluies de fleches deviennent",
                "§7un §bdeluge devastateur§7!",
                "",
                "§6AMELIORATIONS:",
                "§8► §e+3 vagues§8 supplementaires",
                "§8► §e+50%§8 fleches par vague",
                "",
                "§7Exemple: §e8 → 12 fleches/vague",
                "§7Total: §c48 fleches§7 au lieu de 8!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.TRIDENT)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.DELUGE)
            .values(new double[]{3, 1.50}) // extra_waves, arrow_multiplier
            .build());

        // 4.2 - PREDATEUR SUPREME
        TALENTS.add(Talent.builder()
            .id("chasseur_supreme_predator")
            .name("Predateur Supreme")
            .description("Elimination en invisible: +2s (max 10s)")
            .loreLines(new String[]{
                "§7Les eliminations pendant l'invisibilite",
                "§7prolongent l'invisibilite de §e+2s§7.",
                "",
                "§8Limite: 10s max"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.SUPREME_PREDATOR)
            .values(new double[]{2000, 10000}) // extension_ms, max_duration_ms
            .build());

        // 4.3 - SENTENCE DE MORT
        TALENTS.add(Talent.builder()
            .id("chasseur_death_sentence")
            .name("Sentence de Mort")
            .description("Marque explose a la mort (zone)")
            .loreLines(new String[]{
                "§7Les ennemis marques qui meurent",
                "§7font §cexploser§7 leur marque!",
                "",
                "§8Degats: §c100%§8 du coup fatal",
                "§8Rayon: §e3§8 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.TNT_MINECART)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.DEATH_SENTENCE)
            .values(new double[]{1.0, 3.0}) // damage%, radius
            .build());

        // 4.4 - PANDEMIE
        TALENTS.add(Talent.builder()
            .id("chasseur_pandemic")
            .name("Pandemie")
            .description("Poison se propage a la mort")
            .loreLines(new String[]{
                "§7Quand un ennemi empoisonne meurt,",
                "§7le poison se §apropage§7 aux",
                "§7ennemis proches!",
                "",
                "§8Portee: §e4§8 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.PANDEMIC)
            .values(new double[]{4.0}) // spread_range
            .build());

        // 4.5 - SURCHAUFFE
        TALENTS.add(Talent.builder()
            .id("chasseur_overheat")
            .name("Surchauffe")
            .description("+5% degats/tir (max +50%), recul")
            .loreLines(new String[]{
                "§7Chaque tir augmente les degats",
                "§7de §c+5%§7 mais aussi le recul.",
                "",
                "§8Max: §c+50%",
                "§8Reinitialisation apres 2s sans tirer"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.OVERHEAT)
            .values(new double[]{0.05, 0.50, 2000}) // stack%, max%, reset_ms
            .build());
    }

    // ==================== PALIER 5 - NIVEAU 20 (Maitrise) ====================

    private static void registerTier5Talents() {
        // 5.1 - TEMPETE D'ACIER
        TALENTS.add(Talent.builder()
            .id("chasseur_steel_storm")
            .name("Tempete d'Acier")
            .description("Auto mega-pluie toutes les 15s")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Toutes les §e15s§7, une tempete de",
                "§7fleches §cenflammees§7 s'abat!",
                "",
                "§8► Fleches: §c20 fleches de feu",
                "§8► Degats: §c50%§8 + brulure",
                "§8► Zone: §e8x8§8 blocs",
                "",
                "§6§lAUTOMATIQUE:",
                "§7Se declenche sans action requise!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.NETHER_STAR)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.STEEL_STORM)
            .values(new double[]{15000, 0.50, 20, 8.0}) // cooldown_ms, damage_per_arrow%, arrows, radius
            .internalCooldownMs(15000)
            .build());

        // 5.2 - SPECTRE
        TALENTS.add(Talent.builder()
            .id("chasseur_spectre")
            .name("Spectre")
            .description("3 attaques en invis sans la briser")
            .loreLines(new String[]{
                "§7Vous pouvez attaquer §e3 fois§7",
                "§7pendant l'invisibilite sans",
                "§7la briser!",
                "",
                "§8Les bonus restent actifs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.GHAST_TEAR)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.SPECTRE)
            .values(new double[]{3}) // attacks_allowed
            .build());

        // 5.3 - CHASSEUR DE PRIMES
        TALENTS.add(Talent.builder()
            .id("chasseur_bounty_hunter")
            .name("Chasseur de Primes")
            .description("Tuer marque: +10% PV, +20% degats 5s")
            .loreLines(new String[]{
                "§7Tuer un ennemi marque vous",
                "§7soigne de §c10%§7 PV et donne",
                "§7§c+20%§7 degats pendant §a5s§7.",
                "",
                "§8Prime collectee!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.GOLD_INGOT)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BOUNTY_HUNTER)
            .values(new double[]{0.10, 0.20, 5000}) // heal%, damage_bonus%, buff_duration_ms
            .build());

        // 5.4 - EPIDEMIE
        TALENTS.add(Talent.builder()
            .id("chasseur_epidemic")
            .name("Epidemie")
            .description("Poison cumul infini, 10+ = x2 degats")
            .loreLines(new String[]{
                "§7Le poison se cumule §eindefiniment§7!",
                "§7A §e10+ cumuls§7, les degats de",
                "§7poison sont §cx2§7!",
                "",
                "§8Infection mortelle"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.SLIME_BLOCK)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.EPIDEMIC)
            .values(new double[]{10, 2.0}) // threshold, damage_multiplier
            .build());

        // 5.5 - ZONE DE MORT
        TALENTS.add(Talent.builder()
            .id("chasseur_kill_zone")
            .name("Zone de Mort")
            .description("+100% vitesse d'attaque autour de vous")
            .loreLines(new String[]{
                "§7Creez une zone ou votre",
                "§7vitesse de tir est §c+100%§7!",
                "",
                "§8Rayon: §e4§8 blocs",
                "§8Zone suit votre position"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.RED_CARPET)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.KILL_ZONE)
            .values(new double[]{4.0, 1.0}) // radius, attack_speed_bonus%
            .build());
    }

    // ==================== PALIER 6 - NIVEAU 25 (Ascension) ====================

    private static void registerTier6Talents() {
        // 6.1 - FURIE DU BARRAGE
        TALENTS.add(Talent.builder()
            .id("chasseur_barrage_fury")
            .name("Furie du Barrage")
            .description("5 kills pluie = SUPER PLUIE!")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Chaque §ckill§7 avec une pluie",
                "§7accumule §e1 charge§7.",
                "",
                "§6A 5 CHARGES:",
                "§7La prochaine pluie devient une",
                "§c§lSUPER PLUIE§7 devastatrice!",
                "",
                "§6SUPER PLUIE:",
                "§8► §e2x§8 fleches",
                "§8► §e+50%§8 zone",
                "§8► §c+50%§8 degats",
                "§8► Fleches §6explosives§8!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BARRAGE_FURY)
            .values(new double[]{5, 2.0, 1.5}) // charges_needed, arrow_multiplier, zone_multiplier
            .build());

        // 6.2 - VISION D'AIGLE
        TALENTS.add(Talent.builder()
            .id("chasseur_eagle_sight")
            .name("Vision d'Aigle")
            .description("+5% degats critiques par 3 blocs de distance")
            .loreLines(new String[]{
                "§7Vos degats critiques augmentent",
                "§7de §c+5%§7 par §e3 blocs§7 de distance.",
                "",
                "§8Max: §c+50%§8 degats critiques",
                "§8(a 30+ blocs)",
                "§8Synergie: Build sniper"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.SPYGLASS)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.EAGLE_SIGHT)
            .values(new double[]{0.05, 3, 0.50}) // bonus_per_step, blocks_per_step, max_bonus
            .build());

        // 6.3 - FRAPPE DES OMBRES
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_strike")
            .name("Frappe des Ombres")
            .description("Attaque depuis l'ombre: +75% degats")
            .loreLines(new String[]{
                "§7Vos attaques depuis l'invisibilite",
                "§7infligent §c+75%§7 de degats bonus.",
                "",
                "§8S'applique uniquement a la",
                "§8premiere attaque",
                "§8Synergie: Build assassin"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.BLACK_DYE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.SHADOW_STRIKE)
            .values(new double[]{0.75}) // damage_bonus
            .build());

        // 6.4 - FAIBLESSE DE LA PROIE
        TALENTS.add(Talent.builder()
            .id("chasseur_prey_weakness")
            .name("Faiblesse de la Proie")
            .description("Marques: +40% degats critiques subis")
            .loreLines(new String[]{
                "§7Les ennemis marques prennent",
                "§7§c+40%§7 de degats critiques.",
                "",
                "§8S'applique en plus de l'amplification",
                "§8de base des marques",
                "§8Synergie: Build traqueur"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.CROSSBOW)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.PREY_WEAKNESS)
            .values(new double[]{0.40}) // crit_damage_amp
            .build());

        // 6.5 - DECHIQUETEUR D'ARMURE
        TALENTS.add(Talent.builder()
            .id("chasseur_armor_shred")
            .name("Dechiqueteur d'Armure")
            .description("Tirs percants: -10% armure ennemi (cumul)")
            .loreLines(new String[]{
                "§7Les attaques qui traversent",
                "§7reduisent l'armure de §c-10%§7.",
                "",
                "§8Cumul max: §c-50%§8 armure",
                "§8Duree: 5s par cumul",
                "§8Synergie: Build perforation"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.SHEARS)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.ARMOR_SHRED)
            .values(new double[]{0.10, 0.50, 5000}) // reduction_per_stack, max_reduction, duration_ms
            .build());
    }

    // ==================== PALIER 7 - NIVEAU 30 (Transcendance) ====================

    private static void registerTier7Talents() {
        // 7.1 - OEIL DU CYCLONE
        TALENTS.add(Talent.builder()
            .id("chasseur_cyclone_eye")
            .name("Oeil du Cyclone")
            .description("Vortex qui attire + explose!")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos pluies creent un",
                "§b§lVORTEX DEVASTATEUR§7!",
                "",
                "§6EFFETS DU VORTEX:",
                "§8► §bAttire§8 les ennemis vers le centre",
                "§8► §c+30%§8 degats aux aspires",
                "§8► §6Explosion§8 finale!",
                "",
                "§6EXPLOSION:",
                "§8► Zone: §e5§8 blocs",
                "§8► Degats: §c100%§8 + knockback",
                "",
                "§8Les ennemis peuvent resister au centre",
                "§d§lSYNERGIE: Furie du Barrage!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.CYCLONE_EYE)
            .values(new double[]{0.30, 1.0, 5.0, 0.25}) // dmg_bonus, explosion_dmg_mult, explosion_radius, pull_strength
            .build());

        // 6.2 - MAITRE DES OMBRES
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_master")
            .name("Maitre des Ombres")
            .description("Invis permanente sans degats recus")
            .loreLines(new String[]{
                "§7L'invisibilite est §epermanente§7",
                "§7tant que vous ne prenez pas",
                "§7de degats!",
                "",
                "§8Attaques ne brisent plus l'invis"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.BLACK_WOOL)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.SHADOW_MASTER)
            .values(new double[]{}) // no values needed
            .build());

        // 6.3 - EXECUTEUR DE PRIMES
        TALENTS.add(Talent.builder()
            .id("chasseur_bounty_executioner")
            .name("Executeur de Primes")
            .description("Marques sous 20% PV = mort instantanee")
            .loreLines(new String[]{
                "§7Les ennemis marques sous",
                "§7§c20%§7 PV meurent §cinstantanement§7!",
                "",
                "§8Contrat rempli"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.DIAMOND_SWORD)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BOUNTY_EXECUTIONER)
            .values(new double[]{0.20}) // threshold
            .build());

        // 6.4 - PESTE NOIRE
        TALENTS.add(Talent.builder()
            .id("chasseur_black_plague")
            .name("Peste Noire")
            .description("Poison: -75% soins ennemis, +5% auto-soin")
            .loreLines(new String[]{
                "§7Le poison reduit les soins",
                "§7recus par l'ennemi de §c-75%§7",
                "§7et vous soigne de §a5%§7 des",
                "§7degats de poison!",
                "",
                "§8Peste devorante"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.WITHER_ROSE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.BLACK_PLAGUE)
            .values(new double[]{0.75, 0.05}) // heal_reduction%, self_heal%
            .build());

        // 6.5 - GATLING
        TALENTS.add(Talent.builder()
            .id("chasseur_gatling")
            .name("Gatling")
            .description("20 tirs = mode gatling 5s (+200% vitesse)")
            .loreLines(new String[]{
                "§7Apres §e20 tirs§7 consecutifs,",
                "§7passez en §cmode mitrailleuse§7!",
                "",
                "§8Duree: §a5s",
                "§8Bonus: §c+200%§8 vitesse d'attaque",
                "§8Tir automatique!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.DISPENSER)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.GATLING)
            .values(new double[]{20, 5000, 2.0}) // shots_required, duration_ms, attack_speed_bonus%
            .build());
    }

    // ==================== PALIER 8 - NIVEAU 40 (Apex) ====================

    private static void registerTier8Talents() {
        // 8.1 - NUEE DEVASTATRICE
        TALENTS.add(Talent.builder()
            .id("chasseur_devastating_swarm")
            .name("Nuee Devastatrice")
            .description("x2 zone + fragmentation!")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§6ZONE DOUBLEE:",
                "§7Toutes vos pluies ont",
                "§e2x§7 leur rayon normal!",
                "",
                "§6FRAGMENTATION:",
                "§7Chaque fleche qui touche",
                "§7explose en §e3 eclats§7!",
                "",
                "§8► Degats eclat: §c40%",
                "§8► Rayon eclat: §e2§8 blocs",
                "",
                "§d§lAPEX DU BARRAGE!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.FIREWORK_STAR)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.DEVASTATING_SWARM)
            .values(new double[]{2.0, 3, 0.40, 2.0}) // radius_mult, fragment_count, fragment_damage%, fragment_radius
            .build());

        // 7.2 - FAUCHEUR
        TALENTS.add(Talent.builder()
            .id("chasseur_stealth_reaper")
            .name("Faucheur")
            .description("Attaque furtive: <30% PV = mort instantanee")
            .loreLines(new String[]{
                "§7Vos attaques depuis l'invisibilite",
                "§7§ctuent instantanement§7 les ennemis",
                "§7sous §c30%§7 PV!",
                "",
                "§8Faux de la mort"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.NETHERITE_HOE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.STEALTH_REAPER)
            .values(new double[]{0.30}) // threshold
            .build());

        // 7.3 - CHASSEUR LEGENDAIRE
        TALENTS.add(Talent.builder()
            .id("chasseur_legendary_hunter")
            .name("Chasseur Legendaire")
            .description("5 marques simultanees, duree infinie")
            .loreLines(new String[]{
                "§7Vous pouvez marquer §e5§7 ennemis",
                "§7simultanement et les marques",
                "§7durent §eindefiniment§7!",
                "",
                "§8Chasseur supreme"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.BEACON)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.LEGENDARY_HUNTER)
            .values(new double[]{5}) // max_marks
            .build());

        // 7.4 - FLEAU
        TALENTS.add(Talent.builder()
            .id("chasseur_blight")
            .name("Fleau")
            .description("Poison se propage passivement")
            .loreLines(new String[]{
                "§7Le poison peut se propager",
                "§7meme aux ennemis a §epleine vie§7",
                "§7proches d'un infecte!",
                "",
                "§8Portee: §e3§8 blocs",
                "§8Verification: toutes les 2s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.MYCELIUM)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.BLIGHT)
            .values(new double[]{3.0, 2000}) // range, tick_ms
            .build());

        // 7.5 - ARSENAL VIVANT
        TALENTS.add(Talent.builder()
            .id("chasseur_living_arsenal")
            .name("Arsenal Vivant")
            .description("Tir auto toutes 0.5s (80% degats)")
            .loreLines(new String[]{
                "§7Vous tirez §eautomatiquement§7",
                "§7sur l'ennemi le plus proche",
                "§7toutes les §a0.5s§7!",
                "",
                "§8Degats: §c80%",
                "§8Portee: §e10§8 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.DISPENSER)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.LIVING_ARSENAL)
            .values(new double[]{500, 0.80, 10.0}) // tick_ms, damage%, range
            .build());
    }

    // ==================== PALIER 9 - NIVEAU 50 (Legendaire) ====================

    private static void registerTier9Talents() {
        // 9.1 - FRAPPE ORBITALE
        TALENTS.add(Talent.builder()
            .id("chasseur_orbital_strike")
            .name("Frappe Orbitale")
            .description("2x SNEAK = bombardement!")
            .loreLines(new String[]{
                "§6§l★ TALENT LEGENDAIRE ★",
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Appuyez §e2x SNEAK§7 rapidement",
                "§7pour invoquer un §c§lBOMBARDEMENT§7!",
                "",
                "§6EFFET:",
                "§7Une ligne de §c8 bombes§7 explose",
                "§7dans votre direction!",
                "",
                "§8► Longueur: §e30§8 blocs",
                "§8► Rayon/bombe: §e3§8 blocs",
                "§8► Degats: §c500%§8 + brulure",
                "§8► Cooldown: §c30s",
                "",
                "§c§lDEVASTATION TOTALE!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(0)
            .icon(Material.TNT)
            .iconColor("§6§l")
            .effectType(Talent.TalentEffectType.ORBITAL_STRIKE)
            .values(new double[]{30000, 5.0, 30.0, 3.0, 8}) // cooldown_ms, damage_mult, length, explosion_radius, bomb_count
            .internalCooldownMs(30000)
            .build());

        // 8.2 - MARCHEUR DU VIDE (EQUILIBRE - pas d'invincibilite totale!)
        TALENTS.add(Talent.builder()
            .id("chasseur_void_walker")
            .name("Marcheur du Vide")
            .description("-60% degats en mouvement")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Vous existez dans les ombres.",
                "§7§a-60%§7 degats recus tant que",
                "§7vous etes en mouvement.",
                "",
                "§8Note: Vous pouvez toujours mourir!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.ENDER_PEARL)
            .iconColor("§5§l")
            .effectType(Talent.TalentEffectType.VOID_WALKER)
            // BALANCE: -60% DR au lieu d'invincibilite
            .values(new double[]{0.60}) // damage_reduction%
            .build());

        // 8.3 - CARNET DE LA MORT
        TALENTS.add(Talent.builder()
            .id("chasseur_death_note")
            .name("Carnet de la Mort")
            .description("Marquer = mort apres 10s")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Marquer un ennemi le tue",
                "§7apres §e10s§7, peu importe ses PV!",
                "",
                "§8Boss: §c-50%§8 PV direct",
                "§8Temps de recharge: 60s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(2)
            .icon(Material.WRITTEN_BOOK)
            .iconColor("§0§l")
            .effectType(Talent.TalentEffectType.DEATH_NOTE)
            .values(new double[]{10000, 0.50, 60000}) // delay_ms, boss_damage%, cooldown_ms
            .internalCooldownMs(60000)
            .build());

        // 8.4 - APOCALYPSE TOXIQUE
        TALENTS.add(Talent.builder()
            .id("chasseur_toxic_apocalypse")
            .name("Apocalypse Toxique")
            .description("Aura de poison permanente")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Vous emettez constamment un",
                "§7nuage de poison autour de vous.",
                "",
                "§8Rayon: §e5§8 blocs",
                "§8Degats: §c20%§8/s",
                "§8Tous vos bonus poison s'appliquent"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(3)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§2§l")
            .effectType(Talent.TalentEffectType.TOXIC_APOCALYPSE)
            .values(new double[]{5.0, 0.20}) // radius, damage%_per_second
            .build());

        // 8.5 - TEMPS SUSPENDU
        TALENTS.add(Talent.builder()
            .id("chasseur_bullet_time")
            .name("Temps Suspendu")
            .description("Ralentit le temps a 25% pendant 5s")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Activation (S'accroupir + Sauter):",
                "§7Ralentit le temps a §e25%§7!",
                "§7Vous bougez normalement.",
                "",
                "§8Duree: §a5s",
                "§8Temps de recharge: 60s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.CLOCK)
            .iconColor("§b§l")
            .effectType(Talent.TalentEffectType.BULLET_TIME)
            .values(new double[]{5000, 0.25, 60000}) // duration_ms, time_scale, cooldown_ms
            .internalCooldownMs(60000)
            .build());
    }

    // ==================== ACCESSEURS ====================

    /**
     * Obtient tous les talents du Chasseur
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

    private ChasseurTalents() {
        // Utility class
    }
}
