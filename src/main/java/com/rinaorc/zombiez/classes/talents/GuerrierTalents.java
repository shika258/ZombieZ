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
                "§8Compromis survie vs mobilite"
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
            .description("Courir 1.5s puis +200% degats + etourdissement")
            .loreLines(new String[]{
                "§7Courir pendant §e1.5s§7 puis",
                "§7frapper inflige §c+200%§7 degats",
                "§7et §eetourdit§7 la cible 0.5s.",
                "",
                "§8Temps de recharge: 5s"
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

        // 2.4 - BASTION
        TALENTS.add(Talent.builder()
            .id("guerrier_bastion")
            .name("Bastion")
            .description("Bloquer = bouclier 20% PV pendant 3s")
            .loreLines(new String[]{
                "§7Bloquer une attaque vous donne",
                "§7un bouclier de §e20%§7 PV max",
                "§7pendant §a3s§7.",
                "",
                "§8Temps de recharge: 8s"
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
            .description("3 eliminations en 5s = explosion de zone")
            .loreLines(new String[]{
                "§7Tuer §e3§7 ennemis en §a5s§7",
                "§7declenche une explosion!",
                "",
                "§8Degats: §c150%§8 de base",
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

        // 3.4 - TITAN IMMUABLE
        TALENTS.add(Talent.builder()
            .id("guerrier_immovable_titan")
            .name("Titan Immuable")
            .description("Immunite projections/etourdissements, -20% degats si immobile")
            .loreLines(new String[]{
                "§7Immunite aux projections",
                "§7et etourdissements.",
                "",
                "§7Si immobile depuis §a1s§7:",
                "§7§a-20%§7 degats recus"
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
            .description("Ennemis sous 25% PV: +50% degats")
            .loreLines(new String[]{
                "§7Les ennemis sous §c25%§7 PV",
                "§7prennent §c+50%§7 degats de vous.",
                "",
                "§8Coup de grace brutal!"
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

        // 4.3 - VENGEANCE ARDENTE
        TALENTS.add(Talent.builder()
            .id("guerrier_burning_vengeance")
            .name("Vengeance Ardente")
            .description("Contre-attaque = 3 prochaines attaques brulent")
            .loreLines(new String[]{
                "§7Apres une contre-attaque, les §e3§7",
                "§7prochaines attaques brulent",
                "§7les ennemis.",
                "",
                "§8Brulure: §c40%§8 sur 2s"
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
            .description("Achever = +15% PV + reinit. course")
            .loreLines(new String[]{
                "§7Achever un ennemi (sous 25% PV)",
                "§7restaure §c15%§7 PV max",
                "§7et reinitialise la charge.",
                "",
                "§8Enchainez les eliminations!"
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

        // 5.4 - AEGIS ETERNAL
        TALENTS.add(Talent.builder()
            .id("guerrier_eternal_aegis")
            .name("Aegis Eternelle")
            .description("Parade parfaite = 100% degats renvoyes")
            .loreLines(new String[]{
                "§7Chaque parade parfaite (timing 0.3s)",
                "§7renvoie §c100%§7 des degats!",
                "",
                "§8Necessite de la precision"
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
            .description("Achever = prochaine attaque <25% = mort instantanee")
            .loreLines(new String[]{
                "§7Achever un ennemi (sous §c25%§7 PV)",
                "§7permet de tuer instantanement",
                "§7le prochain ennemi sous §c25%§7 PV!",
                "",
                "§8Enchainez les executions!"
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

        // 6.4 - MUR INEBRANLABLE
        TALENTS.add(Talent.builder()
            .id("guerrier_unyielding_wall")
            .name("Mur Inebranlable")
            .description("Immobile: -5% degats/s (max -35%)")
            .loreLines(new String[]{
                "§7Rester immobile accumule",
                "§7§a-5%§7 de reduction de degats",
                "§7par seconde.",
                "",
                "§8Max: §a-35%§8 reduction",
                "§8Reinitialise en bougeant"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.STONE_BRICKS)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.UNYIELDING_WALL)
            .values(new double[]{0.05, 0.35, 1000}) // DR_per_second, max_DR, tick_ms
            .build());

        // 6.5 - ELAN
        TALENTS.add(Talent.builder()
            .id("guerrier_momentum")
            .name("Elan")
            .description("Chaque kill en courant: +25% degats (max +100%)")
            .loreLines(new String[]{
                "§7Les eliminations pendant la course",
                "§7augmentent les degats de §c+25%§7.",
                "",
                "§8Max: §c+100%§8 (4 kills)",
                "§8Reinitialise apres 5s sans course",
                "§8Synergie: Build mobilite"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.WIND_CHARGE)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.MOMENTUM)
            .values(new double[]{0.25, 1.0, 5000}) // bonus_per_kill, max_bonus, reset_ms
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

        // 6.3 - REPRESAILLES INFINIES
        TALENTS.add(Talent.builder()
            .id("guerrier_infinite_retaliation")
            .name("Represailles Infinies")
            .description("Contre-attaques = +25% degats (max 200%)")
            .loreLines(new String[]{
                "§7Chaque contre-attaque augmente les",
                "§7degats de contre-attaque de §c+25%§7.",
                "",
                "§8Max: §c+200%§8 (300% total)",
                "§8Reinit. apres 10s sans contre-attaque"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
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
            .description("Reduction degats x2, +2% PV/s en combat")
            .loreLines(new String[]{
                "§7Votre reduction de degats",
                "§7est §edoublee§7 et vous regenerez",
                "§7§c2%§7 PV/s en combat.",
                "",
                "§8Survie passive"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_7)
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
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.REAPER)
            .values(new double[]{0.15}) // threshold
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

        // 7.3 - NEMESIS
        TALENTS.add(Talent.builder()
            .id("guerrier_nemesis")
            .name("Nemesis")
            .description("Epines passives: 75% degats renvoyes")
            .loreLines(new String[]{
                "§7Chaque ennemi qui vous touche",
                "§7prend §c75%§7 des degats renvoyes.",
                "",
                "§8Renvoi de degats automatique"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
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
            .description("+50% absorption, +30% corps a corps, -25% vitesse")
            .loreLines(new String[]{
                "§7Forme de geant permanente:",
                "§7- §e+50%§7 PV max en §6absorption§7 (regen)",
                "§7- §c+30%§7 degats corps a corps",
                "§7- §c-25%§7 vitesse",
                "",
                "§8Taille +20%",
                "§8L'absorption se regenere avec le temps"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
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
            .description("Aura: ennemis <30% PV = 5% mort instantanee/0.5s")
            .loreLines(new String[]{
                "§7Aura de mort autour de vous:",
                "§7Les ennemis sous §c30%§7 PV ont",
                "§7§c5%§7 de chance de mourir.",
                "",
                "§8Rayon: §e5§8 blocs",
                "§8Intervalle: §80.5s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.WITHER_ROSE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.DEATH_ANGEL)
            .values(new double[]{5.0, 0.30, 0.05, 500}) // radius, hp_threshold, kill_chance, tick_ms
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
            .tier(TalentTier.TIER_9)
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
            .description("3s invincible sans attaque, puis explosion")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Activation (s'accroupir): 3s d'invincibilite",
                "§7totale (pas d'attaque possible),",
                "§7puis explosion massive.",
                "",
                "§8Degats: §c300%§8 de base",
                "§8Rayon: §e5§8 blocs",
                "§8Temps de recharge: 20s"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
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
            .description("1ere attaque du combat = mort instantanee")
            .loreLines(new String[]{
                "§6§lTALENT LEGENDAIRE",
                "",
                "§7Premiere attaque apres §e10s§7",
                "§7sans combat = §cmort instantanee§7.",
                "",
                "§8Boss/Elites: §c-30%§8 PV direct",
                "§8Animation divine!"
            })
            .classType(ClassType.GUERRIER)
            .tier(TalentTier.TIER_9)
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
