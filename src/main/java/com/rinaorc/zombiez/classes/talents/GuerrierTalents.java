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
                "§a§lGARANTI§7: Chaque attaque genere",
                "§7une onde de choc autour de la cible!",
                "",
                "§8Degats: §c50%§8 des degats de base",
                "§8Rayon: §e5§8 blocs",
                "§8Cooldown: §e0.6s"
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
                "§7Vous avez §e25%§7 de chance de",
                "§7§ebloquer§7 les attaques ennemies.",
                "",
                "§7Bloquer une attaque:",
                "§7- §e+3%§7 PV max en §6absorption",
                "§7- §cInflige 50%§7 des degats a l'attaquant",
                "",
                "§8Effet: Coeurs d'absorption dores"
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

        // 1.4 - FRAPPE DE MORT (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_death_strike")
            .name("Frappe de Mort")
            .description("Attaquer soigne selon les degats recus")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG",
                "",
                "§7Chaque attaque vous §4soigne§7 de",
                "§c25%§7 des degats recus dans",
                "§7les §e5 dernieres secondes§7.",
                "",
                "§7Plus vous encaissez, plus vous",
                "§7volez de vie aux ennemis!",
                "",
                "§8Fenetre: §e5s§8 | Min heal: §c2%§8 PV"
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
                "§6ACTIVATION: §eClic Droit§7 avec arme",
                "",
                "§7Foncez en ligne droite sur §e12 blocs§7",
                "§7et §ctraversez§7 tous les ennemis!",
                "",
                "§7Degats: §c+50%§7 de base",
                "§7Bonus: §c+5%§7 par bloc parcouru",
                "",
                "§b§lTEMPÊTE D'ACIER:",
                "§7Chaque Fente reussie octroie un",
                "§7effet §bTempete menaçante§7 (§e6s§7).",
                "",
                "§7A §e2 stacks§7, la prochaine Fente",
                "§7declenche une §b§lTORNADE§7 qui:",
                "§7- §c×2§7 degats de base",
                "§7- §bProjette§7 les ennemis en l'air!",
                "§7- Voyage sur §e16 blocs§7",
                "",
                "§b🌪 HASAGI!",
                "§8Cooldown: §e0.8s"
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
                "§7Vos attaques de zone ont §e30%§7",
                "§7de chance de se repeter.",
                "",
                "§8Delai: 0.3s entre les deux"
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
                "§7Frapper accumule des §echarges§7.",
                "§7A §e3 charges§7 (en 6s):",
                "",
                "§7Prochaine attaque:",
                "§7- §c+80%§7 degats",
                "§7- §e+5%§7 PV max en §6absorption",
                "",
                "§8Style: §6Maintenir le rythme",
                "§8Inspiré: Clash (Punishment)"
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
                "§c§lSOIF DE SANG!",
                "",
                "§7Chaque elimination donne",
                "§c+15%§7 de degats pendant §e4s§7.",
                "",
                "§8Cumulable §c3x§8 (max +45%)",
                "§8Chaque kill refresh la duree"
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
                "§7Vous etes entoure de §f5 charges",
                "§7d'os§7 qui §aabsorbent 8%§7 des",
                "§7degats chacune.",
                "",
                "§7Les charges se §eregenerent§7:",
                "§7- §e1 charge§7 toutes les §a8s§7",
                "§7- Affichees autour de vous",
                "",
                "§8Gardez toujours 3+ charges!",
                "§8Max: §f5§8 charges"
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
                "§6ACTIVATION: §eShift + Clic Droit",
                "",
                "§7Poussez un §ecri de guerre§7 qui",
                "§7§emarque§7 tous les ennemis a §e8 blocs§7!",
                "",
                "§7Frapper un ennemi marque:",
                "§7→ §c40%§7 des degats propages aux",
                "§7  autres ennemis marques!",
                "",
                "§e⚔ Transforme une attaque solo en AoE!",
                "§8Duree marque: §e6s§8 | Cooldown: §e8s"
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
                "§e§lBUILD-UP ACTIF!",
                "",
                "§7Tous les §e4 coups§7, liberez une",
                "§conde de fracture§7 en cone!",
                "",
                "§8Degats: §c150%§8 + §c25%§8/ennemi touche",
                "§8Cone: §e60°§8 devant vous, §e4§8 blocs",
                "§8Effet: §bRalentissement 30%§8 (1.5s)",
                "",
                "§7§oPositionnez-vous bien!"
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
                "§6§lTALENT SIGNATURE - REMPART",
                "",
                "§7Toutes les §e4 attaques§7, lancez un",
                "§6disque spectral§7 devant vous!",
                "",
                "§7Le disque avance lentement et",
                "§7§epulse 4 fois§7 avant d'exploser:",
                "",
                "§8Degats/pulse: §c60%§8 de base",
                "§8Rayon pulse: §e2.5§8 blocs",
                "§8Explosion finale: §c120%§8 + §e3§8 blocs",
                "",
                "§7§oPlaquez-vous a mi-distance!",
                "§8Inspiré: Blessed Shield (D4)"
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
                "§7votre prochaine attaque dans §a2s§7",
                "§7inflige §c+100%§7 degats!",
                "",
                "§8Contre-attaque puissante"
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
                "§6ACTIVATION: §eShift + Attaque",
                "",
                "§7Regenere instantanement §f3 charges§7",
                "§7de §fBouclier d'Os§7!",
                "",
                "§7Bonus: Inflige §c+50%§7 degats",
                "§7sur cette attaque.",
                "",
                "§8Cooldown: §e6s"
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
                "§7Chaque §6Fente§7 lacere votre proie",
                "§7et applique §c3 stacks de Saignement§7!",
                "",
                "§c§lSAIGNEMENT:",
                "§7- §c1%§7 PV max/seconde par stack",
                "§7- Dure §e4 secondes§7",
                "§7- Cumulable jusqu'a §c10 stacks§7",
                "",
                "§7Frapper un ennemi §emarque§7:",
                "§7→ Propage les saignements aux",
                "§7  autres ennemis marques!",
                "",
                "§4🩸 Dechiquetez vos proies!",
                "§8Synergie: Cri de Marquage"
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
                "§7Les ennemis touches par vos",
                "§7attaques de zone prennent",
                "§c+30%§7 degats supplementaires",
                "§7de vos futures attaques AoE.",
                "",
                "§8Duree: §a3s",
                "§8Effet: §eAmplification AoE"
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
                "§7Chaque §eblocage§7 vous donne",
                "§e10%§7 PV max en §6absorption§7!",
                "",
                "§7- Cumulable §e5 fois§7 (max +50%)",
                "§7- Dure §a5 secondes§7",
                "§7- Chaque blocage §erefresh§7 le timer",
                "",
                "§8Effet: §6Coeurs dores d'absorption",
                "§8Inspiré: Fortify (D4)"
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
                "§4§lLES FAIBLES NE MERITENT PAS DE VIVRE",
                "",
                "§7Contre les ennemis a §c< 30%§7 PV:",
                "§7- §c+80%§7 de degats",
                "§7- Kill = heal §a5%§7 PV max",
                "",
                "§8Finisher ultime"
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
                "§7Frappe de Mort soigne maintenant",
                "§c35%§7 des degats recus (au lieu de 25%).",
                "",
                "§7Bonus: Chaque §ckill§7 regenere",
                "§f1 charge§7 de Bouclier d'Os!",
                "",
                "§8Synergie: Sang + Os"
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
                "§7Chaque §eFente§7 augmente votre",
                "§7puissance de combat!",
                "",
                "§7Par stack (max §e5§7):",
                "§7- §c+8%§7 degats",
                "§7- §e+10%§7 vitesse d'attaque",
                "",
                "§7Max: §c+40%§7 degats, §e+50%§7 AS",
                "",
                "§6⚡ Enchainez les fentes!",
                "§8Reset apres §e3s§8 sans Fente"
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
                "§7Toutes les §e10§7 attaques,",
                "§7declenche une explosion massive!",
                "",
                "§8Degats: §c250%§8 de base",
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

        // 5.2 - MARTEAU DU JUGEMENT (REMPART)
        TALENTS.add(Talent.builder()
            .id("guerrier_judgment_hammer")
            .name("Marteau du Jugement")
            .description("Execute <15% HP = marteau geant du ciel")
            .loreLines(new String[]{
                "§6§lVOIE DU REMPART",
                "",
                "§7Frapper un ennemi en dessous",
                "§7de §c15% PV§7 invoque le §6JUGEMENT§7!",
                "",
                "§7Un §6marteau dore geant§7 tombe",
                "§7du ciel et s'ecrase sur la cible!",
                "",
                "§7Effets:",
                "§7- §c300%§7 degats a la cible",
                "§7- §cAoE 6 blocs§7 (150% degats)",
                "§7- §eKnockback§7 puissant",
                "",
                "§8Cooldown: 6s",
                "§8Inspiré: Hammer of the Ancients"
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
                "§7Courir vous fait tournoyer,",
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

        // 5.4 - MORT ET DECOMPOSITION (SANG)
        TALENTS.add(Talent.builder()
            .id("guerrier_death_and_decay")
            .name("Mort et Decomposition")
            .description("Aura de mort permanente autour de vous")
            .loreLines(new String[]{
                "§4§lVOIE DU SANG - AURA DE MORT",
                "",
                "§6§lTOUJOURS ACTIF",
                "",
                "§7Une §4aura de decomposition§7 vous entoure",
                "§7en permanence (§e6 blocs§7).",
                "",
                "§7Effets dans l'aura:",
                "§7- §c10%§7 de vos degats/seconde aux ennemis",
                "§7- §c+25%§7 degats infliges",
                "§7- §a+15%§7 reduction degats",
                "§7- Attaques touchent §ctous§7 les ennemis",
                "",
                "§4§lLA MORT VOUS ACCOMPAGNE!"
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
                "§6ACTIVATION: §eShift + Fente",
                "",
                "§7Sacrifiez §c15%§7 de vos PV max",
                "§7pour une §cFente devastatrice§7!",
                "",
                "§7Cette Fente speciale:",
                "§7- §c×3§7 multiplicateur de degats",
                "§7- §6Trainee de flammes§7 sur la trajectoire",
                "",
                "§6§lTRAINEE DE FLAMMES:",
                "§7- Reste au sol §e3 secondes§7",
                "§7- Inflige §c75%§7 degats/seconde",
                "§7- §c3 applications§7 de degats",
                "",
                "§c🔥 Embrasez votre passage!",
                "§8Synergie: Griffes Lacerantes"
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
                "§7Vos attaques de zone ont",
                "§e25%§7 de chance d'etourdir",
                "§7brievement les ennemis touches.",
                "",
                "§8Stun: §e0.5s",
                "§8Cooldown interne: §e2s§8 par cible",
                "§8Synergie: §6Cataclysme/Tremor"
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
                "§7Chaque §eblocage§7 ou §edegat recu§7",
                "§7stocke §c15%§7 de ces degats.",
                "",
                "§7A §e3 stacks§7 (en 5s):",
                "§7- §6ONDE DE CHOC§7 automatique!",
                "§7- §cInflige§7 tous les degats stockes",
                "§7- §aAoE 8 blocs§7 autour de vous",
                "§7- §e+20%§7 des degats en §6absorption",
                "",
                "§8Effet: §6Explosion doree + gong",
                "§8Inspiré: Iron Skin (D4)"
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
                "§7Quand vous atteignez §e15+ stacks§7",
                "§7de Fureur Croissante, gagnez",
                "§7§a-50%§7 degats recus pendant §a2s§7.",
                "",
                "§8Cooldown: 10s",
                "§8Synergie: Build rage stacking"
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
                "§7Quand vous tombez sous §c30%§7 PV,",
                "§7consomme §fTOUTES§7 vos charges d'os",
                "§7pour vous §asoigner§7!",
                "",
                "§7Soin: §a5%§7 PV par charge consommee",
                "§7+ §6Explosion sanglante§7 autour de vous!",
                "",
                "§8Declenchement: §eAutomatique",
                "§8Cooldown: §e15s"
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
                "§7Eliminer un ennemi avec une §eFente§7:",
                "§7- §aReset§7 instantane du cooldown",
                "§7- §b+25%§7 vitesse mouvement (2s)",
                "",
                "§7Kill sur ennemi §emarque§7:",
                "§7- §c+15%§7 degats pendant §e4s§7",
                "",
                "§e🔥 Chain-killing ultra fluide!",
                "§8Synergie: Cri de Marquage"
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
                "§e§lSPIN TO WIN!",
                "",
                "§7En §ecourant§7, vous generez des",
                "§7ondes sismiques chaque seconde!",
                "",
                "§8Degats: §c50%§8 de vos degats de base",
                "§8Rayon: §e3§8 blocs",
                "§8Contribue a §6Apocalypse Terrestre"
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
                "§6ACTIVATION: §eDouble Sneak",
                "",
                "§7Chargez vers l'avant (§e12 blocs§7)!",
                "",
                "§7Effets sur les ennemis touches:",
                "§7- §c200%§7 degats",
                "§7- §eKnockback§7 puissant",
                "",
                "§7Vous gagnez:",
                "§7- §e+8%§7 PV max en §6absorption§7 par ennemi",
                "§7- Dure §a6 secondes§7",
                "§7- §eCumulable§7 sans limite!",
                "",
                "§8Cooldown: 8s",
                "§8Inspiré: Falling Star (D4)"
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
                "§4§lLA MORT ENGENDRE LA MORT",
                "",
                "§7Executer un ennemi §c(<30% PV)§7",
                "§7invoque un §4cyclone sanglant§7.",
                "",
                "§7Le cyclone §cchasse§7 les ennemis",
                "§7proches pendant §e4s§7.",
                "",
                "§8Degats: §c50%§8 degats de base",
                "§8Soin: §a1.5%§8 PV max par touche"
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
                "§7Frappe de Mort peut maintenant",
                "§7stocker jusqu'a §c40%§7 de vos PV max",
                "§7en degats recus (au lieu de 25%).",
                "",
                "§4§lLARVES DE SANG KAMIKAZES",
                "§7L'exces de soin invoque des larves!",
                "",
                "§7Les larves §4se ruent§7 sur le mob",
                "§7le plus proche et §cexplosent§7!",
                "",
                "§7Explosion:",
                "§7- §cAoE 4 blocs§7 de degats",
                "§7- §a+5%§7 des degats en §4lifesteal§7",
                "§7- Durent §e5 secondes§7 max",
                "",
                "§8Max: §43 larves§8 par overheal"
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
                "§7Toutes les §e5 Fentes§7, declenchez",
                "§7une §4§lEVISCERATION§7 devastatrice!",
                "",
                "§c§lEFFET:",
                "§7Consomme §cTOUS les stacks§7 de",
                "§7Saignement sur les ennemis proches",
                "§7(§e8 blocs§7) et inflige les degats",
                "§7restants §cinstantanement§7!",
                "",
                "§7Bonus: §a+50%§7 des degats de",
                "§7saignements consommes en §asoin§7!",
                "",
                "§4🩸 DEVOREZ VOS PROIES!",
                "§8Synergie: Griffes Lacerantes"
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
                "§6§lAUTOMATIQUE!",
                "",
                "§7Apres avoir inflige §e500 degats§7",
                "§7de zone, declenche un seisme!",
                "",
                "§8Degats: §c500%§8 de base",
                "§8Rayon: §e8§8 blocs",
                "§8Etourdissement: §e1s",
                "",
                "§7Progression affichee en ActionBar"
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
                "§7Aura passive (§e6 blocs§7):",
                "",
                "§7Ennemis dans l'aura:",
                "§7- §c-20%§7 degats infliges",
                "§7- §eGlowing§7 (visibles)",
                "§7- §6Aura doree§7 visible autour de vous",
                "",
                "§7Quand vous recevez des degats melee:",
                "§7- §c30%§7 des degats reflechis",
                "",
                "§8Effet: Domination de zone",
                "§8Inspiré: Defiance Aura (D4)"
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
                "§7Chaque coup te rapproche de l'extase!",
                "",
                "§7Enchaine §e5 coups§7 en §e3 secondes§7",
                "§7Le §c6eme coup§7 inflige §c+150%§7 degats",
                "§7et frappe tous les ennemis a §e5 blocs§7!",
                "",
                "§6⚡ Combo crescendo avec explosion finale",
                "§8Synergie: L'AoE peut proc Dechaînement"
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
                "§4§lVOIE DU SANG - APEX",
                "",
                "§7Votre coeur bat au rythme du sang:",
                "",
                "§7- §c+8%§7 lifesteal permanent",
                "§7- Chaque attaque reduit §eMarrowrend§7",
                "§7  et §eConsommation§7 de §a0.5s§7",
                "§7- §fBouclier d'Os§7 regenere §e2x§7 plus vite",
                "",
                "§8Synergie totale du build Sang!"
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
                "§6§lVOIE DU FAUVE - APEX",
                "",
                "§7Chaque §ckill avec Fente§7 donne",
                "§7un stack de §4§lCARNAGE§7! (max 5)",
                "",
                "§c§lSTACKS DE CARNAGE:",
                "§7- §c+15%§7 degats par stack",
                "§7- Decay apres §e4s§7 sans kill",
                "",
                "§4§lA 5 STACKS - EXPLOSION!",
                "§7Votre prochaine Fente declenche",
                "§7une §4onde sanglante§7 (6 blocs):",
                "§7→ Applique §c5 stacks saignement§7",
                "§7→ §a+25%§7 de vos degats en soin",
                "",
                "§4🩸 ENCHAINEZ LES VICTIMES!",
                "§8Synergie: Griffes Lacerantes"
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
                "§6§lTALENT LEGENDAIRE - ULTIME",
                "",
                "§6ACTIVATION: §eDouble Sneak",
                "",
                "§7Declenche l'apocalypse ultime!",
                "§7Cree une §czone de devastation§7!",
                "",
                "§8Impact: §c800%§8 de vos degats",
                "§8Zone: §e10§8 blocs pendant §e5s",
                "§8Tick: §c150%§8/s aux ennemis dedans",
                "§8Stun: §e2s§8 + projection",
                "§8Cooldown: §e45s"
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
                "§6§lTALENT LEGENDAIRE - ULTIME",
                "",
                "§7Apres avoir §ebloque 300 degats§7",
                "§7cumules, transformez-vous!",
                "",
                "§6AVATAR DU REMPART §7(10s):",
                "§7- §e100%§7 chance de blocage",
                "§7- §c+50%§7 degats infliges",
                "§7- §6Disques x2§7 frequence",
                "§7- §eImmunite CC§7 totale",
                "",
                "§7Activation: §eAutomatique§7 a 300 dmg",
                "§7Compteur affiche en ActionBar",
                "",
                "§8Inspiré: Juggernaut (D4)"
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
                "§7Upgrade du §cCyclone de Rage§7:",
                "§7Double sneak = §c§lMEGA TORNADE§7!",
                "",
                "§7Vous doublez de taille et aspirez",
                "§7les mobs vers vous en courant,",
                "§7infligeant de §clourds degats§7.",
                "",
                "§8Duree: §e10s§8 | Cooldown: §e35s",
                "§8Rayon d'aspiration: §e8§8 blocs"
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
                "§4§lTALENT LEGENDAIRE - ULTIME",
                "",
                "§6ACTIVATION: §eDouble Sneak",
                "",
                "§7Invoque une §4epee runique fantome§7",
                "§7qui combat a vos cotes pendant §e15s§7!",
                "",
                "§7Pendant l'effet:",
                "§7- Vos attaques sont §cdoublees§7",
                "§7- §a+30%§7 reduction de degats",
                "§7- §c+20%§7 lifesteal supplementaire",
                "§7- Regenere §f1 charge d'os§7/2s",
                "",
                "§4§lVOUS ETES LE BOSS DU COMBAT!",
                "§8Cooldown: §e45s"
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
                "§e§lTALENT LEGENDAIRE - ULTIME",
                "",
                "§6ACTIVATION: §eDouble Sneak",
                "",
                "§7Transformez-vous en §c§lBERSERKER§7!",
                "",
                "§6RAGE DU BERSERKER§7 (12s):",
                "§7- §c+75%§7 taille (geant!)",
                "§7- §c×2§7 multiplicateur de degats",
                "§7- §aFente ultra-rapide§7 (0.1s cooldown)",
                "§7- §e+4 blocs§7 de portee Fente (16 total)",
                "§7- §4Glowing rouge§7 intimidant",
                "§7- §6Aura de feu§7 orange",
                "§7- §bImmunite knockback§7",
                "",
                "§c🔥 WRATH OF THE BERSERKER!",
                "§8Cooldown: §e60s"
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
