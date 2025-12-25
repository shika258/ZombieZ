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
            .description("Chaque attaque cree une onde de choc")
            .loreLines(new String[]{
                "§7Chaque attaque cree une onde de choc",
                "§7autour de la cible touchee.",
                "",
                "§6Degats: §c50% §7| §6Rayon: §e5 blocs",
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
            .name("Posture Defensive")
            .description("25% blocage passif, riposte + absorption")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Chance de bloquer les attaques.",
                "§7Bloquer donne de l'absorption et",
                "§7renvoie des degats a l'attaquant.",
                "",
                "§6Blocage: §e25% §7| §6Absorption: §a+3% §7PV",
                "§6Riposte: §c50% §7des degats"
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
            .description("+2% degats par coup, max 20%")
            .loreLines(new String[]{
                "§7Chaque coup augmente vos degats.",
                "§7Se reset apres 3s sans attaquer.",
                "",
                "§6Bonus: §a+2% §7par coup | §6Max: §a+20%"
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
            .description("Attaquer soigne selon les degats recus")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Chaque attaque vous soigne selon",
                "§7les degats recus recemment.",
                "",
                "§6Soin: §a25% §7des degats recus (5s)",
                "§6Minimum: §a2% §7PV"
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
            .name("Fente Devastatrice")
            .description("Dash traversant + Tempete d'Acier!")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§6Clic Droit §7avec arme: dash traversant",
                "§7qui blesse tous les ennemis.",
                "",
                "§6Distance: §e12 blocs §7| §6Degats: §c+50%",
                "§6Bonus: §c+5% §7par bloc parcouru",
                "",
                "§6TEMPETE D'ACIER §7(2 stacks):",
                "§7Tornade §c×2 degats §7+ projection!",
                "§bCooldown: §f0.8s"
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
            .name("Echo de Guerre")
            .description("30% chance que les degats de zone se repetent")
            .loreLines(new String[]{
                "§7Vos attaques de zone peuvent",
                "§7se repeter automatiquement.",
                "",
                "§6Chance: §e30% §7| §6Delai: §e0.3s"
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
            .name("Chatiment")
            .description("3 coups = prochaine attaque devastatrice")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Apres 3 coups en 6s, votre",
                "§7prochaine attaque est renforcee.",
                "",
                "§6Degats: §c+80% §7| §6Absorption: §a+5% §7PV"
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
            .description("Kills = +15% degats, stack 3x")
            .loreLines(new String[]{
                "§7Chaque elimination augmente",
                "§7vos degats temporairement.",
                "",
                "§6Bonus: §c+15% §7par kill | §6Max: §c+45%",
                "§6Duree: §b4s §7(refresh par kill)"
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
            .description("5 charges d'os protectrices")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Des charges d'os vous entourent",
                "§7et absorbent les degats recus.",
                "",
                "§6Charges: §e5 §7| §6Absorption: §a8% §7/charge",
                "§6Regen: §b1 charge/8s"
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
            .description("Marque les ennemis, propager les degats")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§6Shift + Clic Droit§7: marque tous",
                "§7les ennemis proches. Les degats",
                "§7se propagent entre eux.",
                "",
                "§6Rayon: §e8 blocs §7| §6Propagation: §c40%",
                "§6Duree: §b6s §7| §bCooldown: §f8s"
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
            .description("Tous les 4 coups = onde sismique!")
            .loreLines(new String[]{
                "§7Tous les 4 coups, liberez une",
                "§7onde de fracture en cone.",
                "",
                "§6Degats: §c150% §7+ §c25%§7/ennemi",
                "§6Cone: §e60° §7| §6Portee: §e4 blocs",
                "§6Slow: §c30% §7pendant §b1.5s"
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
            .description("Toutes les 4 attaques: disque pulsant!")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Toutes les 4 attaques, lancez un",
                "§7disque spectral qui pulse et explose.",
                "",
                "§6Pulse: §c60% §7(4x) | §6Rayon: §e2.5 blocs",
                "§6Explosion: §c120% §7| §6Rayon: §e3 blocs"
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
            .name("Colere des Ancetres")
            .description("Apres degats recus: +100% prochaine attaque")
            .loreLines(new String[]{
                "§7Apres avoir recu des degats,",
                "§7votre prochaine attaque est renforcee.",
                "",
                "§6Bonus: §c+100% §7degats | §6Fenetre: §b2s"
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
            .name("Moelle de Dechirement")
            .description("Shift+Attaque regenere 3 charges d'os")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§6Shift + Attaque§7: regenere 3 charges",
                "§7de Bouclier d'Os instantanement.",
                "",
                "§6Degats: §c+50% §7| §bCooldown: §f6s"
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
            .name("Griffes Lacerantes")
            .description("Chaque Fente applique Saignement")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Chaque Fente applique du saignement.",
                "§7Se propage aux ennemis marques.",
                "",
                "§6Stacks: §c3 §7par Fente | §6Max: §c10",
                "§6DoT: §c1% §7PV/s par stack | §6Duree: §b4s"
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
            .name("Resonance Sismique")
            .description("Amplifie les degats de zone contre les cibles debuffs")
            .loreLines(new String[]{
                "§7Les ennemis touches par vos AoE",
                "§7prennent plus de degats de zone.",
                "",
                "§6Amplification: §c+30% §7| §6Duree: §b3s"
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
            .description("Blocages = absorption temporaire")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Chaque blocage donne de l'absorption.",
                "§7Cumulable et refreshable.",
                "",
                "§6Absorption: §a+10% §7PV | §6Max: §a+50%",
                "§6Duree: §b5s §7(refresh par blocage)"
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
            .name("Coup de Grace")
            .description("Ennemis < 30% PV = +80% degats, heal au kill")
            .loreLines(new String[]{
                "§7Degats bonus contre les ennemis",
                "§7affaiblis. Les tuer vous soigne.",
                "",
                "§6Seuil: §c<30% §7PV | §6Bonus: §c+80%",
                "§6Soin au kill: §a5% §7PV max"
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
            .name("Volonte Vampirique")
            .description("Ameliore Death Strike + kills = charges")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Ameliore Frappe de Mort. Les kills",
                "§7regenerent des charges d'os.",
                "",
                "§6Soin: §a35% §7(au lieu de 25%)",
                "§6Kill: §a+1 §7charge Bouclier d'Os"
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
            .name("Elan Furieux")
            .description("Fentes successives = stacks de puissance")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Chaque Fente augmente vos degats",
                "§7et vitesse d'attaque.",
                "",
                "§6Par stack: §c+8% §7dgts + §e+10% §7AS",
                "§6Max: §c+40% §7dgts + §e+50% §7AS (5 stacks)",
                "§6Reset: §b3s §7sans Fente"
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
            .description("Toutes les 10 attaques: explosion de zone massive")
            .loreLines(new String[]{
                "§7Toutes les 10 attaques, declenche",
                "§7une explosion massive autour de vous.",
                "",
                "§6Degats: §c250% §7| §6Rayon: §e5 blocs"
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
            .description("Execute <15% HP = marteau geant du ciel")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Frapper un ennemi sous 15% PV",
                "§7fait tomber un marteau du ciel.",
                "",
                "§6Cible: §c300% §7| §6AoE: §c150% §7(6 blocs)",
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
            .description("Courir = degats continus autour de vous")
            .loreLines(new String[]{
                "§7Courir vous fait tournoyer et",
                "§7blesse les ennemis proches.",
                "",
                "§6Degats: §c60%§7/0.5s | §6Rayon: §e2 blocs"
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
            .name("Mort et Decomposition")
            .description("Aura de mort permanente autour de vous")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Aura permanente qui blesse les",
                "§7ennemis et vous renforce.",
                "",
                "§6Rayon: §e6 blocs §7| §6DoT: §c10%§7/s",
                "§6Bonus: §c+25% §7dgts | §6Reduction: §a15%"
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
            .description("Sacrifie PV pour degats x3 + trainee de feu")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§6Shift + Fente§7: sacrifie 15% PV pour",
                "§7une Fente x3 degats + trainee de feu.",
                "",
                "§6Cout: §c15% §7PV | §6Degats: §c×3",
                "§6Trainee: §c75%§7/s pendant §b3s"
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
            .name("Secousses Residuelles")
            .description("Vos AoE etourdissent brievement les cibles")
            .loreLines(new String[]{
                "§7Vos attaques de zone peuvent",
                "§7etourdir les ennemis touches.",
                "",
                "§6Chance: §e25% §7| §6Stun: §c0.5s",
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
            .name("Echo de Fer")
            .description("Stocke les degats bloques/recus → onde de choc")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Stocke 15% des degats bloques/recus.",
                "§7A 3 stacks: onde de choc AoE.",
                "",
                "§6AoE: §e8 blocs §7| §6Absorption: §a+20%",
                "§6Fenetre: §b5s §7pour accumuler"
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
            .description("A 15+ stacks rage: -50% degats recus 2s")
            .loreLines(new String[]{
                "§7A 15+ stacks de Fureur, vous",
                "§7prenez moins de degats.",
                "",
                "§6Reduction: §a50% §7| §6Duree: §b2s",
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
            .description("Consomme les charges d'os pour soigner")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Sous 30% PV: consomme vos charges",
                "§7d'os pour vous soigner + explosion.",
                "",
                "§6Soin: §a5%§7/charge | §6AoE: §e4 blocs",
                "§bCooldown: §f15s §7(auto)"
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
            .name("Predateur Insatiable")
            .description("Kill avec Fente = reset + vitesse")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Tuer avec Fente reset le cooldown",
                "§7et donne de la vitesse.",
                "",
                "§6Vitesse: §a+25% §7(2s)",
                "§6Kill marque: §c+15% §7dgts (4s)"
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
            .name("Tremor Eternal")
            .description("Courir genere des ondes sismiques")
            .loreLines(new String[]{
                "§7Courir genere des ondes sismiques",
                "§7autour de vous chaque seconde.",
                "",
                "§6Degats: §c50% §7| §6Rayon: §e3 blocs",
                "§6Interval: §b1s"
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
            .description("Double-sneak: charge + absorption par cible")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§6Double Sneak§7: charge vers l'avant.",
                "§7Gagne absorption par ennemi touche.",
                "",
                "§6Distance: §e12 blocs §7| §6Degats: §c200%",
                "§6Absorption: §a+8%§7/ennemi (6s)",
                "§bCooldown: §f8s"
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
            .description("Execution = cyclone chasseur (4s)")
            .loreLines(new String[]{
                "§7Executer un ennemi (<30% PV)",
                "§7invoque un cyclone chasseur.",
                "",
                "§6Duree: §b4s §7| §6Degats: §c50%",
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
            .description("Death Strike ameliore + Larves de Sang kamikazes")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Stockage ameliore. Overheal invoque",
                "§7des larves kamikazes explosives.",
                "",
                "§6Stockage: §e40% §7PV | §6Max: §e3 §7larves",
                "§6Larves: §c50% §7AoE (4 blocs) + §a5% §7lifesteal"
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
            .name("Evisceration")
            .description("Toutes les 5 Fentes = consomme saignements")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Toutes les 5 Fentes: consomme les",
                "§7saignements et inflige les degats.",
                "",
                "§6Rayon: §e8 blocs §7| §6Soin: §a50% §7des dgts"
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
            .description("Proc auto apres 500 degats de zone")
            .loreLines(new String[]{
                "§7Apres 500 degats de zone cumules,",
                "§7declenche un seisme automatique.",
                "",
                "§6Degats: §c500% §7| §6Rayon: §e8 blocs",
                "§6Stun: §c1s"
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
            .name("Aura de Defi")
            .description("Aura: ennemis affaiblis + reflexion")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Aura passive: affaiblit les ennemis",
                "§7et reflechit les degats melee.",
                "",
                "§6Rayon: §e6 blocs §7| §6Reduction: §a20%",
                "§6Reflexion: §c30% §7des degats recus"
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
            .name("Frenesie Guerriere")
            .description("Combo 5 coups en 3s = +150% degats AoE")
            .loreLines(new String[]{
                "§7Enchainer 5 coups en 3s: le 6eme",
                "§7est une explosion AoE puissante.",
                "",
                "§6Bonus: §c+150% §7dgts | §6Rayon: §e5 blocs"
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
            .name("Coeur de Vampire")
            .description("Lifesteal permanent + reduit cooldowns")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Lifesteal passif, cooldowns reduits",
                "§7et regen d'os acceleree.",
                "",
                "§6Lifesteal: §a+8% §7| §6CD reduit: §b0.5s§7/atk",
                "§6Regen Os: §a×2 §7plus rapide"
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
            .name("Chaine de Carnage")
            .description("Kills avec Fente = stacks de Carnage")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE",
                "",
                "§7Kills avec Fente = stacks Carnage.",
                "§7A 5 stacks: onde sanglante AoE.",
                "",
                "§6Par stack: §c+15% §7dgts | §6Max: §e5",
                "§6Onde: §e6 blocs §7+ §c5 §7saign. + §a25% §7soin",
                "§6Decay: §b4s §7sans kill"
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
            .description("ULTIME: Double Sneak = zone d'apocalypse!")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§6Double Sneak§7: zone d'apocalypse",
                "§7avec degats massifs et stun.",
                "",
                "§6Impact: §c800% §7| §6Zone: §e10 blocs",
                "§6DoT: §c150%§7/s (5s) | §6Stun: §c2s",
                "§bCooldown: §f45s"
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
            .description("ULTIME: Transformation apres blocages cumules")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART - LEGENDAIRE",
                "",
                "§7Apres 300 degats bloques: transformation",
                "§7en Avatar du Rempart (10s).",
                "",
                "§6Blocage: §e100% §7| §6Degats: §c+50%",
                "§6Disques: §e×2 §7| §6Immunite CC"
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
            .name("Mega Tornade")
            .description("Double sneak = transformation en mega tornade")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§6Double Sneak§7: devenez une tornade",
                "§7geante qui aspire les ennemis.",
                "",
                "§6Duree: §b10s §7| §6Rayon: §e8 blocs",
                "§6Degats: §c75%§7/tick | §bCooldown: §f35s"
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
            .name("Epee Dansante")
            .description("ULTIME: Epee fantome + double attaques")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG - LEGENDAIRE",
                "",
                "§6Double Sneak§7: epee fantome qui",
                "§7double vos attaques (15s).",
                "",
                "§6Reduction: §a30% §7| §6Lifesteal: §a+20%",
                "§6Regen Os: §a1§7/2s | §bCooldown: §f45s"
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
            .description("ULTIME: Transformation en berserker geant")
            .loreLines(new String[]{
                "§6§lVOIE DU FAUVE - LEGENDAIRE",
                "",
                "§6Double Sneak§7: devenez un berserker",
                "§7geant devastateur (12s).",
                "",
                "§6Taille: §e+75% §7| §6Degats: §c×2",
                "§6Fente: §b0.1s §7CD | §6Portee: §e+4 blocs",
                "§bCooldown: §f60s"
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
