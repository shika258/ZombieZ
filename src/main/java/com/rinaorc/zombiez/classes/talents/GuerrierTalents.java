package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registre de tous les talents du Guerrier
 * 45 talents au total, 5 par palier sur 9 paliers
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
        registerTier9Talents();
    }

    // ==================== PALIER 1 - NIVEAU 0 (Fondation) ====================

    private static void registerTier1Talents() {
        // 1.1 - FRAPPE SISMIQUE
        TALENTS.add(Talent.builder()
            .id("guerrier_seismic_strike")
            .name("Frappe Sismique")
            .description("Génère une onde de choc à chaque attaque")
            .loreLines(new String[]{
                "§7Libère une onde de choc autour",
                "§7de la cible à chaque attaque.",
                "",
                "§6Dégâts: §c50%",
                "§6Zone: §e5 blocs",
                "",
                "§bCooldown: §f0.6s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.COBBLESTONE)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SEISMIC_STRIKE)
            .values(new double[]{0.50, 5.0}) // damage%, radius
            .internalCooldownMs(600)
            .build());

        // 1.2 - POSTURE DEFENSIVE (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_defensive_stance")
            .name("Posture Défensive")
            .description("Bloque les attaques et riposte")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Adopte une posture défensive qui",
                "§7bloque les attaques ennemies.",
                "§7Un blocage réussi octroie de",
                "§7l'absorption et renvoie les dégâts.",
                "",
                "§6Chance de blocage: §e25%",
                "§6Absorption: §a+3% §7PV max",
                "§6Riposte: §c50% §7des dégâts bloqués"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.SHIELD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.DEFENSIVE_STANCE)
            .values(new double[]{0.25, 0.03, 0.50}) // block_chance, heal%, riposte_damage%
            .build());

        // 1.3 - FUREUR CROISSANTE
        TALENTS.add(Talent.builder()
            .id("guerrier_rising_fury")
            .name("Fureur Croissante")
            .description("Accumule de la puissance à chaque coup")
            .loreLines(new String[]{
                "§7Chaque attaque augmente vos dégâts.",
                "§7Les stacks se réinitialisent après",
                "§73 secondes sans attaquer.",
                "",
                "§6Bonus par coup: §c+2%",
                "§6Maximum: §c+20% §7(10 stacks)",
                "§6Réinitialisation: §b3s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(2)
            .icon(Material.BLAZE_POWDER)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.RISING_FURY)
            .values(new double[]{0.02, 0.20, 3000}) // stack%, max%, reset_ms
            .build());

        // 1.4 - FRAPPE DE MORT (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_death_strike")
            .name("Frappe de Mort")
            .description("Vos attaques soignent selon les dégâts récents")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Chaque attaque vous soigne en",
                "§7fonction des dégâts subis récemment.",
                "",
                "§6Soin: §a25% §7des dégâts reçus (5s)",
                "§6Soin minimum: §a2% §7PV max"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.REDSTONE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.DEATH_STRIKE)
            .values(new double[]{0.25, 5000, 0.02}) // heal%, window_ms, min_heal%
            .build());

        // 1.5 - FENTE DÉVASTATRICE (VOIE DU FAUVE) - Style Yasuo
        TALENTS.add(Talent.builder()
            .id("guerrier_lunging_strike")
            .name("Fente Dévastatrice")
            .description("Dash traversant qui blesse tous les ennemis")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Effectue un dash traversant qui",
                "§7inflige des dégâts à tous les",
                "§7ennemis sur votre passage.",
                "",
                "§6Distance: §e12 blocs",
                "§6Dégâts: §c+50% §7+ §c5%§7/bloc parcouru",
                "",
                "§e§lTEMPÊTE D'ACIER §7(2 stacks):",
                "§7Déclenche une tornade dévastatrice.",
                "§6Dégâts: §c×2 §7+ projection",
                "",
                "§bCooldown: §f0.8s §7| §eActivation: §fClic Droit"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.NETHERITE_SWORD)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.LUNGING_STRIKE)
            .values(new double[]{12.0, 0.50, 0.05, 800}) // range, base_bonus%, per_block_bonus%, cooldown_ms
            .internalCooldownMs(800)
            .build());
    }

    // ==================== PALIER 2 - NIVEAU 5 (Amplification) ====================

    private static void registerTier2Talents() {
        // 2.1 - ECHO DE GUERRE
        TALENTS.add(Talent.builder()
            .id("guerrier_war_echo")
            .name("Écho de Guerre")
            .description("Vos attaques de zone peuvent se répéter")
            .loreLines(new String[]{
                "§7Vos attaques de zone ont une",
                "§7chance de se déclencher une",
                "§7seconde fois automatiquement.",
                "",
                "§6Chance: §e30%",
                "§6Délai: §b0.3s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.ECHO_SHARD)
            .iconColor("§9")
            .effectType(Talent.TalentEffectType.WAR_ECHO)
            .values(new double[]{0.30, 300}) // chance, delay_ms
            .build());

        // 2.2 - CHATIMENT (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_punishment")
            .name("Châtiment")
            .description("Après 3 coups, la prochaine attaque est dévastatrice")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Après avoir infligé 3 coups en 6s,",
                "§7votre prochaine attaque devient",
                "§7dévastatrice et vous renforce.",
                "",
                "§6Dégâts bonus: §c+80%",
                "§6Absorption: §a+5% §7PV max",
                "§6Fenêtre: §b6s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.GOLDEN_SWORD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.PUNISHMENT)
            .values(new double[]{3, 6000, 0.80, 0.05}) // stacks_needed, window_ms, damage_bonus%, heal%
            .build());

        // 2.3 - FERVEUR SANGUINAIRE
        TALENTS.add(Talent.builder()
            .id("guerrier_blood_fervour")
            .name("Ferveur Sanguinaire")
            .description("Chaque élimination augmente vos dégâts")
            .loreLines(new String[]{
                "§7Chaque ennemi tué augmente",
                "§7temporairement vos dégâts.",
                "§7Les stacks se cumulent.",
                "",
                "§6Bonus par kill: §c+15%",
                "§6Maximum: §c+45% §7(3 stacks)",
                "§6Durée: §b4s §7(rafraîchi par kill)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.REDSTONE)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BLOOD_FERVOUR)
            .values(new double[]{0.15, 4000, 3}) // damage_bonus%, duration_ms, max_stacks
            .build());

        // 2.4 - BOUCLIER D'OS (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_bone_shield")
            .name("Bouclier d'Os")
            .description("Des charges d'os protectrices vous entourent")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Des fragments d'os tournent autour",
                "§7de vous et absorbent les dégâts.",
                "",
                "§6Charges maximum: §e5",
                "§6Réduction par charge: §a8%",
                "§6Régénération: §b1 charge/8s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.BONE)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.BONE_SHIELD)
            .values(new double[]{5, 0.08, 8000}) // max_charges, DR_per_charge, regen_ms
            .build());

        // 2.5 - CRI DE MARQUAGE (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_war_cry_mark")
            .name("Cri de Marquage")
            .description("Marque les ennemis proches pour propager les dégâts")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Pousse un cri qui marque tous les",
                "§7ennemis proches. Les dégâts infligés",
                "§7à une cible se propagent aux autres.",
                "",
                "§6Zone: §e8 blocs",
                "§6Propagation: §c40% §7des dégâts",
                "§6Durée: §b6s",
                "",
                "§bCooldown: §f8s §7| §eActivation: §fSneak + Clic Droit"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.GOAT_HORN)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.WAR_CRY_MARK)
            .values(new double[]{8.0, 0.40, 6000, 8000}) // radius, propagation%, mark_duration_ms, cooldown_ms
            .internalCooldownMs(8000)
            .build());
    }

    // ==================== PALIER 3 - NIVEAU 10 (Specialisation) ====================

    private static void registerTier3Talents() {
        // 3.1 - ONDE DE FRACTURE
        TALENTS.add(Talent.builder()
            .id("guerrier_fracture_wave")
            .name("Onde de Fracture")
            .description("Déclenche une onde sismique tous les 4 coups")
            .loreLines(new String[]{
                "§7Tous les 4 coups, libère une onde",
                "§7de fracture en cône devant vous.",
                "§7Plus il y a d'ennemis touchés,",
                "§7plus les dégâts augmentent.",
                "",
                "§6Dégâts: §c150% §7+ §c25%§7/ennemi touché",
                "§6Cône: §e60° §7| §6Portée: §e4 blocs",
                "§6Ralentissement: §c30% §7(§b1.5s§7)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.CRACKED_DEEPSLATE_TILES)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.FRACTURE_WAVE)
            .values(new double[]{4, 1.50, 0.25, 4.0, 60, 0.30, 1500}) // hits_needed, base_damage%, bonus_per_hit%, range, cone_angle, slow%, slow_duration_ms
            .build());

        // 3.2 - BOUCLIER VENGEUR (REMPART) - TALENT SIGNATURE
        TALENTS.add(Talent.builder()
            .id("guerrier_vengeful_shield")
            .name("Bouclier Vengeur")
            .description("Lance un disque spectral qui pulse et explose")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Toutes les 4 attaques, projette un",
                "§7disque spectral qui pulse 4 fois",
                "§7avant d'exploser violemment.",
                "",
                "§6Dégâts par pulse: §c60% §7(×4)",
                "§6Zone pulse: §e2.5 blocs",
                "§6Explosion finale: §c120%",
                "§6Zone explosion: §e3 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.VENGEFUL_SHIELD)
            .values(new double[]{4, 0.60, 2.5, 4, 1.20, 3.0, 8.0}) // hits_needed, pulse_damage%, pulse_radius, pulse_count, explosion_damage%, explosion_radius, travel_distance
            .build());

        // 3.3 - COLERE DES ANCETRES
        TALENTS.add(Talent.builder()
            .id("guerrier_ancestral_wrath")
            .name("Colère des Ancêtres")
            .description("Après avoir subi des dégâts, votre prochaine attaque est décuplée")
            .loreLines(new String[]{
                "§7Après avoir reçu des dégâts,",
                "§7canalisez la rage ancestrale pour",
                "§7renforcer votre prochaine attaque.",
                "",
                "§6Bonus dégâts: §c+100%",
                "§6Fenêtre d'activation: §b2s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.GOLDEN_SWORD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.ANCESTRAL_WRATH)
            .values(new double[]{2000, 1.0}) // window_ms, bonus_damage%
            .build());

        // 3.4 - MOELLE DE DECHIREMENT (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_marrowrend")
            .name("Moelle de Déchirement")
            .description("Régénère instantanément des charges d'os")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Frappe puissante qui régénère",
                "§7instantanément 3 charges de",
                "§7Bouclier d'Os.",
                "",
                "§6Charges régénérées: §e3",
                "§6Dégâts bonus: §c+50%",
                "",
                "§bCooldown: §f6s §7| §eActivation: §fSneak + Attaque"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.BONE_BLOCK)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.MARROWREND)
            .values(new double[]{3, 0.50, 6000}) // charges_regen, damage_bonus%, cooldown_ms
            .internalCooldownMs(6000)
            .build());

        // 3.5 - GRIFFES LACÉRANTES (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_lacerating_claws")
            .name("Griffes Lacérantes")
            .description("Chaque Fente inflige un saignement")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Chaque Fente Dévastatrice applique",
                "§7des stacks de saignement qui se",
                "§7propagent aux ennemis marqués.",
                "",
                "§6Stacks par Fente: §c3",
                "§6Maximum: §c10 §7stacks",
                "§6Dégâts: §c1% §7PV/s par stack",
                "§6Durée: §b4s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.PRISMARINE_SHARD)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.LACERATING_CLAWS)
            .values(new double[]{3, 0.01, 4000, 10}) // stacks_per_hit, damage_per_stack%, duration_ms, max_stacks
            .build());
    }

    // ==================== PALIER 4 - NIVEAU 15 (Evolution) ====================

    private static void registerTier4Talents() {
        // 4.1 - RESONANCE SISMIQUE
        TALENTS.add(Talent.builder()
            .id("guerrier_seismic_resonance")
            .name("Résonance Sismique")
            .description("Les ennemis touchés par vos AoE deviennent vulnérables")
            .loreLines(new String[]{
                "§7Les ennemis touchés par vos",
                "§7attaques de zone deviennent",
                "§7vulnérables aux dégâts de zone.",
                "",
                "§6Amplification: §c+30%",
                "§6Durée: §b3s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.CRACKED_STONE_BRICKS)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SEISMIC_RESONANCE)
            .values(new double[]{3000, 0.30}) // duration_ms, damage_amplification%
            .build());

        // 4.2 - FORTIFICATION (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_fortify")
            .name("Fortification")
            .description("Chaque blocage octroie de l'absorption cumulable")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Chaque blocage réussi octroie de",
                "§7l'absorption. Les stacks se cumulent",
                "§7et se rafraîchissent à chaque blocage.",
                "",
                "§6Absorption par blocage: §a+10% §7PV",
                "§6Maximum: §a+50% §7(5 stacks)",
                "§6Durée: §b5s §7(rafraîchi par blocage)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.IRON_CHESTPLATE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.FORTIFY)
            .values(new double[]{0.10, 5, 5000}) // hp_bonus_per_stack, max_stacks, duration_ms
            .build());

        // 4.3 - COUP DE GRÂCE
        TALENTS.add(Talent.builder()
            .id("guerrier_mercy_strike")
            .name("Coup de Grâce")
            .description("Dégâts amplifiés contre les ennemis affaiblis")
            .loreLines(new String[]{
                "§7Inflige des dégâts bonus contre les",
                "§7ennemis affaiblis. Les achever vous",
                "§7soigne d'un pourcentage de vos PV.",
                "",
                "§6Seuil: §c<30% §7PV ennemi",
                "§6Bonus dégâts: §c+80%",
                "§6Soin à l'élimination: §a5% §7PV max"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.NETHERITE_AXE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.MERCY_STRIKE)
            .values(new double[]{0.30, 0.80, 0.05}) // threshold%, damage_bonus%, heal%
            .build());

        // 4.4 - VOLONTE VAMPIRIQUE (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_vampiric_will")
            .name("Volonté Vampirique")
            .description("Améliore Frappe de Mort et les éliminations")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Améliore Frappe de Mort. Chaque",
                "§7élimination régénère une charge",
                "§7de Bouclier d'Os.",
                "",
                "§6Soin amélioré: §a35% §7(au lieu de 25%)",
                "§6Charge par kill: §e+1"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.WITHER_ROSE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.VAMPIRIC_WILL)
            .values(new double[]{0.35, 1}) // upgraded_heal%, charges_on_kill
            .build());

        // 4.5 - ÉLAN FURIEUX (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_furious_momentum")
            .name("Élan Furieux")
            .description("Les Fentes successives augmentent votre puissance")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Chaque Fente Dévastatrice augmente",
                "§7vos dégâts et vitesse d'attaque.",
                "§7Les stacks se réinitialisent après",
                "§73 secondes sans Fente.",
                "",
                "§6Par stack: §c+8% §7dégâts + §e+10% §7vitesse",
                "§6Maximum: §c+40% §7+ §e+50% §7(5 stacks)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.WIND_CHARGE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.FURIOUS_MOMENTUM)
            .values(new double[]{0.08, 0.10, 5, 3000}) // damage_per_stack%, speed_per_stack%, max_stacks, reset_ms
            .build());
    }

    // ==================== PALIER 5 - NIVEAU 20 (Maitrise) ====================

    private static void registerTier5Talents() {
        // 5.1 - CATACLYSME
        TALENTS.add(Talent.builder()
            .id("guerrier_cataclysm")
            .name("Cataclysme")
            .description("Déclenche une explosion massive toutes les 10 attaques")
            .loreLines(new String[]{
                "§7Toutes les 10 attaques, déclenche",
                "§7une explosion cataclysmique autour",
                "§7de vous, dévastant tous les ennemis.",
                "",
                "§6Dégâts: §c250%",
                "§6Zone: §e5 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.NETHER_STAR)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.CATACLYSM)
            .values(new double[]{10, 2.50, 5.0}) // attacks_needed, damage%, radius
            .build());

        // 5.2 - MARTEAU DU JUGEMENT (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_judgment_hammer")
            .name("Marteau du Jugement")
            .description("Invoque un marteau céleste sur les ennemis affaiblis")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Frapper un ennemi sous 15% PV",
                "§7fait tomber un marteau céleste",
                "§7qui écrase la cible et les proches.",
                "",
                "§6Seuil: §c<15% §7PV ennemi",
                "§6Dégâts cible: §c300%",
                "§6Dégâts zone: §c150% §7(§e6 blocs§7)",
                "",
                "§bCooldown: §f6s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.GOLDEN_AXE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.JUDGMENT_HAMMER)
            .values(new double[]{0.15, 3.0, 1.5, 6.0, 6000}) // hp_threshold, main_damage%, aoe_damage%, aoe_radius, cooldown_ms
            .internalCooldownMs(6000)
            .build());

        // 5.3 - CYCLONE DE RAGE
        TALENTS.add(Talent.builder()
            .id("guerrier_rage_cyclone")
            .name("Cyclone de Rage")
            .description("Tournoie en courant et blesse les ennemis proches")
            .loreLines(new String[]{
                "§7Lorsque vous courez, vous tourbillonnez",
                "§7comme un cyclone, infligeant des dégâts",
                "§7continus aux ennemis à proximité.",
                "",
                "§6Dégâts: §c60% §7toutes les §b0.5s",
                "§6Zone: §e2 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.WIND_CHARGE)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.RAGE_CYCLONE)
            .values(new double[]{500, 0.60, 2.0}) // tick_ms, damage%, radius
            .build());

        // 5.4 - MORT ET DECOMPOSITION (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_death_and_decay")
            .name("Mort et Décomposition")
            .description("Génère une aura de mort permanente")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Émet une aura de décomposition qui",
                "§7ronge les ennemis proches tout en",
                "§7vous renforçant considérablement.",
                "",
                "§6Zone: §e6 blocs",
                "§6Dégâts sur le temps: §c10%§7/s",
                "§6Bonus dégâts: §c+25%",
                "§6Réduction des dégâts: §a15%"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.CRIMSON_NYLIUM)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.DEATH_AND_DECAY)
            .values(new double[]{6.0, 0.25, 0.15, 0.10}) // radius, damage_bonus%, DR_bonus%, aura_damage%
            .build());

        // 5.5 - CONSOMMATION DE FUREUR (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_fury_consumption")
            .name("Consommation de Fureur")
            .description("Sacrifie des PV pour une Fente dévastatrice")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Sacrifie une partie de vos PV pour",
                "§7déclencher une Fente surpuissante",
                "§7qui laisse une traînée de feu.",
                "",
                "§6Coût: §c15% §7PV",
                "§6Multiplicateur: §c×3 §7dégâts",
                "§6Traînée: §c75%§7/s pendant §b3s",
                "",
                "§eActivation: §fSneak + Fente"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.FURY_CONSUMPTION)
            .values(new double[]{0.15, 3.0, 0.75, 3000}) // hp_cost%, damage_multiplier, trail_damage%, trail_duration_ms
            .build());
    }

    // ==================== PALIER 6 - NIVEAU 25 (Ascension) ====================

    private static void registerTier6Talents() {
        // 6.1 - SECOUSSES RESIDUELLES
        TALENTS.add(Talent.builder()
            .id("guerrier_seismic_aftermath")
            .name("Secousses Résiduelles")
            .description("Vos attaques de zone peuvent étourdir")
            .loreLines(new String[]{
                "§7Vos attaques de zone ont une",
                "§7chance d'étourdir brièvement",
                "§7les ennemis touchés.",
                "",
                "§6Chance: §e25%",
                "§6Étourdissement: §c0.5s",
                "§bCooldown: §f2s §7par cible"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.CRACKED_DEEPSLATE_BRICKS)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.SEISMIC_AFTERMATH)
            .values(new double[]{0.25, 500, 2000}) // stun_chance, stun_duration_ms, cooldown_ms
            .build());

        // 6.2 - ÉCHO DE FER (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_iron_echo")
            .name("Écho de Fer")
            .description("Accumule les dégâts pour déclencher une onde de choc")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Stocke 15% des dégâts bloqués ou",
                "§7reçus. À 3 stacks, libère une",
                "§7puissante onde de choc.",
                "",
                "§6Zone: §e8 blocs",
                "§6Absorption bonus: §a+20%",
                "§6Fenêtre: §b5s §7pour accumuler"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.ECHO_SHARD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.IRON_ECHO)
            .values(new double[]{0.15, 3, 5000, 8.0, 0.20}) // storage_percent, stacks_needed, window_ms, aoe_radius, heal_percent
            .build());

        // 6.3 - RAGE IMPARABLE
        TALENTS.add(Talent.builder()
            .id("guerrier_unstoppable_rage")
            .name("Rage Imparable")
            .description("À haute fureur, réduit massivement les dégâts reçus")
            .loreLines(new String[]{
                "§7Lorsque vous atteignez 15+ stacks",
                "§7de Fureur, vous devenez temporairement",
                "§7résistant aux dégâts.",
                "",
                "§6Seuil: §e15+ §7stacks de Fureur",
                "§6Réduction: §a50%",
                "§6Durée: §b2s",
                "",
                "§bCooldown: §f10s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.BLAZE_ROD)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.UNSTOPPABLE_RAGE)
            .values(new double[]{15, 0.50, 2000, 10000}) // stacks_required, DR%, duration_ms, cooldown_ms
            .internalCooldownMs(10000)
            .build());

        // 6.4 - CONSOMMATION (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_consumption")
            .name("Consommation")
            .description("Consomme les charges d'os pour se soigner")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Lorsque vos PV tombent sous 30%,",
                "§7consomme automatiquement vos charges",
                "§7d'os pour vous soigner et exploser.",
                "",
                "§6Seuil: §c<30% §7PV",
                "§6Soin: §a5% §7par charge",
                "§6Explosion: §e4 blocs",
                "",
                "§bCooldown: §f15s §7(automatique)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.NETHER_WART)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.CONSUMPTION)
            .values(new double[]{0.30, 0.05, 4.0, 15000}) // hp_threshold, heal_per_charge%, explosion_radius, cooldown_ms
            .internalCooldownMs(15000)
            .build());

        // 6.5 - PRÉDATEUR INSATIABLE (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_insatiable_predator")
            .name("Prédateur Insatiable")
            .description("Éliminer avec Fente réinitialise le cooldown")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Tuer un ennemi avec Fente Dévastatrice",
                "§7réinitialise son cooldown et octroie",
                "§7un bonus de vitesse.",
                "",
                "§6Vitesse: §a+25% §7(§b2s§7)",
                "§6Kill sur marqué: §c+15% §7dégâts (§b4s§7)"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.INSATIABLE_PREDATOR)
            .values(new double[]{0.25, 2000, 0.15, 4000}) // speed_bonus%, speed_duration_ms, marked_kill_damage%, marked_buff_ms
            .build());
    }

    // ==================== PALIER 7 - NIVEAU 30 (Transcendance) ====================

    private static void registerTier7Talents() {
        // 6.1 - TREMOR ETERNAL
        TALENTS.add(Talent.builder()
            .id("guerrier_eternal_tremor")
            .name("Tremor Éternel")
            .description("Génère des ondes sismiques en courant")
            .loreLines(new String[]{
                "§7Lorsque vous courez, génère des",
                "§7ondes sismiques autour de vous",
                "§7à intervalles réguliers.",
                "",
                "§6Dégâts: §c50%",
                "§6Zone: §e3 blocs",
                "§6Intervalle: §b1s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.DEEPSLATE)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.ETERNAL_TREMOR)
            .values(new double[]{1000, 0.50, 3.0}) // interval_ms, damage%, radius
            .build());

        // 7.2 - CHARGE DU BASTION (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_bastion_charge")
            .name("Charge du Bastion")
            .description("Charge vers l'avant et gagne de l'absorption")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Effectue une charge dévastatrice",
                "§7vers l'avant. Chaque ennemi touché",
                "§7octroie de l'absorption.",
                "",
                "§6Distance: §e12 blocs",
                "§6Dégâts: §c200%",
                "§6Absorption: §a+8% §7par ennemi (§b6s§7)",
                "",
                "§bCooldown: §f8s §7| §eActivation: §fDouble Sneak"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.TRIDENT)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BASTION_CHARGE)
            .values(new double[]{12.0, 2.0, 0.08, 6000, 8000}) // distance, damage%, hp_per_enemy%, duration_ms, cooldown_ms
            .internalCooldownMs(8000)
            .build());

        // 6.3 - CYCLONES SANGLANTS
        TALENTS.add(Talent.builder()
            .id("guerrier_blood_cyclones")
            .name("Cyclones Sanglants")
            .description("Exécuter un ennemi invoque un cyclone chasseur")
            .loreLines(new String[]{
                "§7Achever un ennemi sous 30% PV",
                "§7invoque un cyclone de sang qui",
                "§7pourchasse et blesse les ennemis.",
                "",
                "§6Durée: §b4s",
                "§6Dégâts: §c50%",
                "§6Soin: §a1.5% §7PV par touche"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.BLOOD_CYCLONES)
            .values(new double[]{4000, 0.50, 0.015, 3.0}) // duration_ms, damage%, heal%, radius
            .build());

        // 6.4 - PACTE DE SANG (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_blood_pact")
            .name("Pacte de Sang")
            .description("Améliore Frappe de Mort et invoque des larves")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Améliore le stockage de dégâts.",
                "§7Le sursoins invoque des larves de",
                "§7sang kamikazes et explosives.",
                "",
                "§6Stockage amélioré: §e40% §7PV",
                "§6Maximum: §e3 §7larves",
                "§6Explosion: §c50% §7(§e4 blocs§7)",
                "§6Lifesteal: §a5%"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.FERMENTED_SPIDER_EYE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.BLOOD_PACT)
            .values(new double[]{0.40, 3, 5000, 4.0, 0.05}) // max_stored_damage%, max_larvae, larvae_duration_ms, aoe_radius, lifesteal%
            .build());

        // 7.5 - ÉVISCÉRATION (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_evisceration")
            .name("Éviscération")
            .description("Consomme tous les saignements pour des dégâts massifs")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Toutes les 5 Fentes, consomme tous",
                "§7les stacks de saignement sur les",
                "§7ennemis proches et inflige leurs",
                "§7dégâts restants instantanément.",
                "",
                "§6Zone: §e8 blocs",
                "§6Soin: §a50% §7des dégâts infligés"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.GHAST_TEAR)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.EVISCERATION)
            .values(new double[]{5, 8.0, 0.50}) // lunges_needed, radius, heal_percent_of_damage
            .build());
    }

    // ==================== PALIER 8 - NIVEAU 40 (Apex) ====================

    private static void registerTier8Talents() {
        // 7.1 - APOCALYPSE TERRESTRE
        TALENTS.add(Talent.builder()
            .id("guerrier_earth_apocalypse")
            .name("Apocalypse Terrestre")
            .description("Déclenche un séisme automatique après 500 dégâts de zone")
            .loreLines(new String[]{
                "§7Après avoir infligé 500 dégâts de",
                "§7zone cumulés, déclenche un séisme",
                "§7dévastateur qui étourdit les ennemis.",
                "",
                "§6Dégâts: §c500%",
                "§6Zone: §e8 blocs",
                "§6Étourdissement: §c1s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.BEDROCK)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.EARTH_APOCALYPSE)
            .values(new double[]{500, 5.0, 8.0, 1000}) // threshold, damage%, radius, stun_ms
            .internalCooldownMs(5000)
            .build());

        // 8.2 - AURA DE DEFI (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_defiance_aura")
            .name("Aura de Défi")
            .description("Émet une aura qui affaiblit les ennemis et réfléchit les dégâts")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Émet une aura permanente qui",
                "§7affaiblit les ennemis proches et",
                "§7réfléchit les dégâts de mêlée.",
                "",
                "§6Zone: §e6 blocs",
                "§6Réduction ennemis: §a20%",
                "§6Réflexion: §c30% §7des dégâts"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.TOTEM_OF_UNDYING)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.DEFIANCE_AURA)
            .values(new double[]{6.0, 0.20, 0.30}) // radius, damage_reduction%, reflect%
            .build());

        // 7.3 - FRÉNÉSIE GUERRIÈRE
        TALENTS.add(Talent.builder()
            .id("guerrier_warrior_frenzy")
            .name("Frénésie Guerrière")
            .description("Enchaîner 5 coups déclenche une explosion dévastatrice")
            .loreLines(new String[]{
                "§7Enchaîner 5 coups en 3 secondes",
                "§7déclenche une explosion de zone",
                "§7sur la 6ème attaque.",
                "",
                "§6Combo requis: §e5 §7coups en §b3s",
                "§6Bonus dégâts: §c+150%",
                "§6Zone: §e5 blocs"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.BLAZE_POWDER)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.WARRIOR_FRENZY)
            .values(new double[]{5, 3000, 1.50, 5.0}) // combo_hits, timeout_ms, damage_bonus%, aoe_radius
            .build());

        // 7.4 - COEUR DE VAMPIRE (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_vampiric_heart")
            .name("Cœur de Vampire")
            .description("Lifesteal permanent et régénération accélérée")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Octroie un lifesteal passif et",
                "§7accélère la régénération des",
                "§7charges d'os et des cooldowns.",
                "",
                "§6Lifesteal: §a+8%",
                "§6Réduction cooldown: §b0.5s §7par attaque",
                "§6Régénération os: §a×2 §7plus rapide"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.VAMPIRIC_HEART)
            .values(new double[]{0.08, 500, 2.0}) // lifesteal%, cooldown_reduction_ms, bone_regen_multiplier
            .build());

        // 8.5 - CHAÎNE DE CARNAGE (VOIE DU FAUVE)
        TALENTS.add(Talent.builder()
            .id("guerrier_carnage_chain")
            .name("Chaîne de Carnage")
            .description("Les éliminations avec Fente déclenchent une onde sanglante")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Chaque kill avec Fente accumule",
                "§7des stacks de Carnage. À 5 stacks,",
                "§7déclenche une onde sanglante.",
                "",
                "§6Bonus par stack: §c+15% §7dégâts",
                "§6Maximum: §e5 §7stacks",
                "§6Onde: §e6 blocs §7+ §c5 §7saignements",
                "§6Soin: §a25%",
                "§6Déclin: §b4s §7sans kill"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.REDSTONE)
            .iconColor("§4")
            .effectType(Talent.TalentEffectType.WAR_FRENZY)
            .values(new double[]{5, 0.15, 4000, 6.0, 5, 0.25}) // max_stacks, damage_per_stack%, decay_ms, explosion_radius, bleed_stacks, heal_percent
            .build());
    }

    // ==================== PALIER 9 - NIVEAU 50 (Legendaire) ====================

    private static void registerTier9Talents() {
        // 8.1 - RAGNAROK
        TALENTS.add(Talent.builder()
            .id("guerrier_ragnarok")
            .name("Ragnarok")
            .description("Invoque une zone d'apocalypse dévastatrice")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "",
                "§7Invoque une zone d'apocalypse qui",
                "§7inflige des dégâts massifs, étourdit",
                "§7les ennemis et brûle la terre.",
                "",
                "§6Impact: §c800%",
                "§6Zone: §e10 blocs",
                "§6Dégâts sur le temps: §c150%§7/s (§b5s§7)",
                "§6Étourdissement: §c2s",
                "",
                "§bCooldown: §f45s §7| §eActivation: §fDouble Sneak"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
            .slotIndex(0)
            .icon(Material.END_CRYSTAL)
            .iconColor("§6§l")
            .effectType(Talent.TalentEffectType.RAGNAROK)
            .values(new double[]{45000, 8.0, 12.0, 2000}) // cooldown_ms, damage%, radius, stun_ms
            .internalCooldownMs(45000)
            .build());

        // 9.2 - AVATAR DU REMPART (REMPART) - TALENT LEGENDAIRE ULTIME
        TALENTS.add(Talent.builder()
            .id("guerrier_bulwark_avatar")
            .name("Avatar du Rempart")
            .description("Se transforme en Avatar invincible après blocages cumulés")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "§6§lVOIE DU REMPART",
                "",
                "§7Après avoir bloqué 300 dégâts,",
                "§7se transforme en Avatar du Rempart,",
                "§7une forme de combat ultime.",
                "",
                "§6Durée: §b10s",
                "§6Blocage: §e100%",
                "§6Dégâts: §c+50%",
                "§6Disques: §e×2 §7fréquence",
                "§6Immunité: §7Contrôles de foule"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.NETHER_STAR)
            .iconColor("§6§l")
            .effectType(Talent.TalentEffectType.BULWARK_AVATAR)
            .values(new double[]{300, 10000, 1.0, 0.50, 2.0}) // damage_threshold, duration_ms, block_chance, damage_bonus%, disc_frequency_mult
            .build());

        // 8.3 - MÉGA TORNADE
        TALENTS.add(Talent.builder()
            .id("guerrier_mega_tornado")
            .name("Méga Tornade")
            .description("Se transforme en tornade géante aspirant les ennemis")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "",
                "§7Devenez une tornade géante qui",
                "§7aspire et déchiquète tous les",
                "§7ennemis à proximité.",
                "",
                "§6Durée: §b10s",
                "§6Zone: §e8 blocs",
                "§6Dégâts: §c75%§7/tick",
                "",
                "§bCooldown: §f35s §7| §eActivation: §fDouble Sneak"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
            .slotIndex(2)
            .icon(Material.BREEZE_ROD)
            .iconColor("§c§l")
            .effectType(Talent.TalentEffectType.MEGA_TORNADO)
            .values(new double[]{10000, 35000, 8.0, 2.0, 0.75}) // duration_ms, cooldown_ms, radius, scale, damage%_per_tick
            .build());

        // 8.4 - EPEE DANSANTE (SANG) - TALENT LEGENDAIRE
        TALENTS.add(Talent.builder()
            .id("guerrier_dancing_rune_weapon")
            .name("Épée Dansante")
            .description("Invoque une épée fantôme qui double vos attaques")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "§4§lVOIE DU SANG",
                "",
                "§7Invoque une épée runique fantôme",
                "§7qui reproduit toutes vos attaques",
                "§7et renforce vos capacités.",
                "",
                "§6Durée: §b15s",
                "§6Réduction dégâts: §a30%",
                "§6Lifesteal: §a+20%",
                "§6Régénération os: §e1§7/§b2s",
                "",
                "§bCooldown: §f45s §7| §eActivation: §fDouble Sneak"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
            .slotIndex(3)
            .icon(Material.NETHERITE_SWORD)
            .iconColor("§4§l")
            .effectType(Talent.TalentEffectType.DANCING_RUNE_WEAPON)
            .values(new double[]{15000, 0.30, 0.20, 2000, 45000}) // duration_ms, DR_bonus%, lifesteal_bonus%, bone_regen_ms, cooldown_ms
            .internalCooldownMs(45000)
            .build());

        // 9.5 - RAGE DU BERSERKER (VOIE DU FAUVE - ULTIME)
        TALENTS.add(Talent.builder()
            .id("guerrier_berserker_rage")
            .name("Rage du Berserker")
            .description("Se transforme en berserker géant dévastateur")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "§6§lVOIE DU FAUVE",
                "",
                "§7Devenez un berserker géant aux",
                "§7capacités de combat décuplées.",
                "§7Les Fentes deviennent instantanées.",
                "",
                "§6Durée: §b12s",
                "§6Taille: §e+75%",
                "§6Dégâts: §c×2",
                "§6Cooldown Fente: §b0.1s",
                "§6Portée: §e+4 blocs",
                "",
                "§bCooldown: §f60s §7| §eActivation: §fDouble Sneak"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.NETHER_STAR)
            .iconColor("§c§l")
            .effectType(Talent.TalentEffectType.BERSERKER_RAGE)
            .values(new double[]{12000, 2.0, 0.75, 4.0, 60000}) // duration_ms, damage_multiplier, size_bonus%, range_bonus, cooldown_ms
            .internalCooldownMs(60000)
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
