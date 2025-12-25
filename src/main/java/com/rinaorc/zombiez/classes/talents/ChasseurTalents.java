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
            .description("35% chance de tirer 3 fleches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Chance de tirer 3 fleches",
                "§7au lieu d'une seule.",
                "",
                "§6Chance: §e35% §7| §6Degats: §c100%§7/fleche"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(0)
            .icon(Material.ARROW)
            .iconColor("§f")
            .effectType(Talent.TalentEffectType.MULTI_SHOT)
            .values(new double[]{0.35, 2}) // chance (buffed 30→35%), extra_projectiles (2 bonus = 3 total)
            .build());

        // 1.2 - CHAUVE-SOURIS (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_bat")
            .name("Chauve-souris")
            .description("Invoque une chauve-souris a ultrasons")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque une chauve-souris qui tire",
                "§7des ultrasons transperçants.",
                "",
                "§6Portee: §e12 blocs §7| §6Cadence: §b1.2s",
                "§aInvincible"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.BAT_SPAWN_EGG)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.BEAST_BAT)
            .values(new double[]{2.5}) // base_damage (buffed 1.5→2.5)
            .build());

        // 1.3 - LAME D'OMBRE (Branche Ombre - Refonte)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_blade")
            .name("Lame d'Ombre")
            .description("Attaques = Points d'Ombre")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Attaques generent des Points d'Ombre.",
                "§7A 3+ Points: bonus vitesse d'attaque.",
                "",
                "§6Points: §e+1§7/atk | §6Max: §e5 §7| §6Bonus: §a+30%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(2)
            .icon(Material.NETHERITE_SWORD)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_BLADE)
            .values(new double[]{1, 3, 0.30}) // points_per_hit, threshold, attack_speed_bonus
            .build());

        // 1.4 - FRAPPE VENIMEUSE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_venomous_strike")
            .name("Frappe Venimeuse")
            .description("50% virulence, 70%+ = Nécrose (+30%)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Chance d'appliquer de la Virulence.",
                "§7A 70%+: Necrose (+30% DoT).",
                "§7A 100%: Corrompu (+35% degats).",
                "",
                "§6Chance: §e50% §7| §6Virulence: §c+18"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.VENOMOUS_STRIKE)
            .values(new double[]{0.50, 100, 70, 0.30}) // chance (buffed 40→50%), max_virulence, necrosis_threshold, necrosis_bonus (buffed 25→30%)
            .build());

        // 1.5 - FLÈCHES REBONDISSANTES (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_piercing_arrows")
            .name("Fleches Rebondissantes")
            .description("Rebondit vers 2 ennemis, applique GIVRE!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Fleches rebondissent et appliquent",
                "§7du Givre. 50% = Ralenti, 100% = Gele.",
                "",
                "§6Rebonds: §e2 §7| §6Givre: §b+15%§7/touche",
                "§6Bonus/rebond: §c+25% §7dgts + §b12.5% §7givre"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.BLUE_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.PIERCING_ARROWS)
            .values(new double[]{2, 0.25}) // bounce_count, bonus_damage_per_bounce%
            .build());
    }

    // ==================== PALIER 2 - NIVEAU 5 (Amplification) ====================

    private static void registerTier2Talents() {
        // 2.1 - RAFALE (Salve en Éventail)
        TALENTS.add(Talent.builder()
            .id("chasseur_burst_shot")
            .name("Rafale")
            .description("Accumule des charges, declenche 3 salves!")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Accumulez des charges par touche.",
                "§7A 8: lancez 3 salves de 5 fleches!",
                "",
                "§6Charges: §e8 §7| §6Total: §c15 fleches",
                "§6Degats: §c100%§7/fleche"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(0)
            .icon(Material.TIPPED_ARROW)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.BURST_SHOT)
            .values(new double[]{8, 5, 1.0}) // charges_needed, bonus_arrows, damage_percent
            .build());

        // 2.2 - ENDERMITE (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_endermite")
            .name("Endermite")
            .description("Invoque un parasite du Vide qui corrompt les ennemis!")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un endermite qui parasite",
                "§7les ennemis (+25% degats subis).",
                "",
                "§6Duree: §b3s §7| §6Explosion: §c50%",
                "§6AoE: §e4 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(1)
            .icon(Material.ENDER_PEARL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.BEAST_ENDERMITE)
            .values(new double[]{3000, 0.25, 0.50, 4.0}) // infestation_duration_ms, corruption_bonus, explosion_damage, aoe_radius
            .build());

        // 2.3 - TIR D'OMBRE (Branche Ombre) - Refonte
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_shot")
            .name("Tir d'Ombre")
            .description("15% chance: tir de pistolet + stun")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chance de declencher un tir bonus",
                "§7qui etourdit la cible.",
                "",
                "§6Chance: §e15% §7| §6Degats: §c150%",
                "§6Stun: §c1s §7| §6Portee: §e20 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(2)
            .icon(Material.ECHO_SHARD)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_SHOT)
            .values(new double[]{0.15, 1.50, 20, 1000}) // proc_chance, damage_mult, range, stun_duration_ms
            .build());

        // 2.4 - VENIN CORROSIF (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_corrosive_venom")
            .name("Venin Corrosif")
            .description("+20% dégâts sur empoisonnés!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Degats bonus contre les ennemis",
                "§7empoisonnes (virulence > 0).",
                "",
                "§6Bonus: §c+20% §7degats"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.FERMENTED_SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.CORROSIVE_VENOM)
            .values(new double[]{0.20, 6000, 0.20}) // damage_bonus% (buffed 15→20%), virulence_duration_ms, damage_bonus%
            .build());

        // 2.5 - CHARGE GLACIALE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_caliber")
            .name("Charge Glaciale")
            .description("Charge 1-5, à 5 = TIR GLACIAL!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chaque tir ajoute +1 Charge.",
                "§7A 5: Tir Glacial (gel instantane)!",
                "",
                "§6Max: §e5 §7| §6Givre/niveau: §b+5%",
                "§6Bonus Charge 5: §b+1 §7rebond"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.PACKED_ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.CALIBER)
            .values(new double[]{5, 0.05, 1.0, 1}) // max_charge, frost_per_level%, glacial_shot_bonus, extra_bounce
            .build());
    }

    // ==================== PALIER 3 - NIVEAU 10 (Specialisation) ====================

    private static void registerTier3Talents() {
        // 3.1 - PLUIE DE FLECHES
        TALENTS.add(Talent.builder()
            .id("chasseur_arrow_rain")
            .name("Pluie de Fleches")
            .description("30% chance pluie AoE")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Chance de declencher une pluie",
                "§7de fleches sur la zone.",
                "",
                "§6Chance: §e30% §7| §6Fleches: §e10",
                "§6Degats: §c45%§7/fleche | §6Zone: §e5 blocs",
                "§bCooldown: §f1.5s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(0)
            .icon(Material.BOW)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.ARROW_RAIN)
            .values(new double[]{0.30, 0.45, 10, 5.0}) // chance (buffed 25→30%), damage_per_arrow% (buffed 30→45%), arrows (buffed 8→10), radius (buffed 4→5)
            .internalCooldownMs(1500) // cooldown reduced 2000→1500
            .build());

        // 3.2 - LOUP (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_wolf")
            .name("Loup")
            .description("Invoque un loup qui inflige du saignement")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un loup dont les morsures",
                "§7infligent du saignement.",
                "",
                "§6Degats: §c4 §7| §6Saignement: §c2§7/s",
                "§6Duree: §b5s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.WOLF_SPAWN_EGG)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.BEAST_WOLF)
            .values(new double[]{4.0, 5000, 2.0}) // base_damage (buffed 3→4), bleed_duration_ms, bleed_damage_per_tick (buffed 1.5→2.0)
            .build());

        // 3.3 - PAS DE L'OMBRE (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_step")
            .name("Pas de l'Ombre")
            .description("Shift+Attaque = téléport derrière (16 blocs)")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§6Shift + Attaque§7: teleport derriere",
                "§7la cible + frappe + vitesse.",
                "",
                "§6Portee: §e16 blocs §7| §6Degats: §c125%",
                "§6Vitesse: §a+50% §7(3s) | §6Points: §d+2",
                "§bCooldown: §f5s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.ENDER_EYE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_STEP)
            .values(new double[]{5000, 2, 16, 60, 1.25}) // cooldown_ms, points_gained, range (16 blocs), speed_buff_ticks, damage_mult
            .build());

        // 3.4 - TOXINES MORTELLES (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_deadly_toxins")
            .name("Toxines Mortelles")
            .description("DoT peut CRIT! +20% dmg DoT, Slow progressif")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Les DoT peuvent critiquer et",
                "§7ralentissent progressivement.",
                "",
                "§6Bonus DoT: §c+20% §7| §6Crit: §eOui",
                "§6Slow: §cI§7(0-49) §cII§7(50-69) §cIII§7(70+)"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.POISONOUS_POTATO)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.DEADLY_TOXINS)
            .values(new double[]{0.25, 0.50, 0.30, 0.20}) // crit_chance, crit_bonus, slow%, dot_bonus (NEW +20%)
            .build());

        // 3.5 - LIGNE DE GLACE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_fatal_trajectory")
            .name("Ligne de Glace")
            .description("2+ rebonds = Zone de Givre (+30% givre)")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Rebondir sur 2+ ennemis cree",
                "§7une Ligne de Glace qui donne",
                "§7du givre bonus aux ennemis.",
                "",
                "§6Longueur: §e12 blocs §7| §6Duree: §b3s",
                "§6Bonus givre: §b+30%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.PRISMARINE_CRYSTALS)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.FATAL_TRAJECTORY)
            .values(new double[]{2, 12.0, 3000, 0.30}) // bounce_threshold, line_length, duration_ms, frost_bonus%
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
                "§7Ameliore les pluies de fleches",
                "§7avec plus de vagues et fleches.",
                "",
                "§6Bonus: §e+3 vagues §7| §6Fleches: §e+50%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(0)
            .icon(Material.TRIDENT)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.DELUGE)
            .values(new double[]{3, 1.50}) // extra_waves, arrow_multiplier
            .build());

        // 4.2 - AXOLOTL (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_axolotl")
            .name("Axolotl")
            .description("Invoque un axolotl qui tire des bulles d'eau")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un axolotl qui tire",
                "§7des bulles d'eau sur les ennemis.",
                "",
                "§6Degats: §c3.5 §7| §6Portee: §e8 blocs",
                "§6Cadence: §b1.5s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(1)
            .icon(Material.AXOLOTL_BUCKET)
            .iconColor("§d")
            .effectType(Talent.TalentEffectType.BEAST_AXOLOTL)
            .values(new double[]{3.5, 8.0, 1500}) // base_damage (buffed 2.5→3.5), range, cooldown_ms
            .build());

        // 4.3 - MARQUE DE MORT (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_death_mark")
            .name("Marque de Mort")
            .description("Crits marquent 8s (+25% dégâts)")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Les coups critiques marquent",
                "§7les ennemis (degats bonus).",
                "",
                "§6Duree: §b8s §7| §6Bonus: §c+25% §7degats"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(2)
            .icon(Material.WITHER_SKELETON_SKULL)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DEATH_MARK)
            .values(new double[]{8000, 0.25}) // duration_ms, damage_bonus
            .build());

        // 4.4 - PANDEMIE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_pandemic")
            .name("Pandemie")
            .description("Kill empoisonné = PROPAGATION en chaîne!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Tuer un empoisonne propage le",
                "§7poison aux ennemis proches.",
                "",
                "§6Cibles: §e3 §7| §6Rayon: §e5 blocs",
                "§6Virulence: §c+40 §7(§d+60 §7si corrompu)"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.PANDEMIC)
            .values(new double[]{5.0, 40, 60, 3}) // range, base_virulence, corrupted_virulence, max_targets
            .build());

        // 4.5 - HYPOTHERMIE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_overheat")
            .name("Hypothermie")
            .description("+10%/tir (max 100%), à max = VAGUE DE FROID!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chaque tir accumule +10% froid.",
                "§7A 100%: Vague de Froid AoE!",
                "",
                "§6Max: §e100% §7(10 tirs) | §6Reset: §b3s",
                "§6Vague: §e4 blocs §7+ §b50% §7givre"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.SNOWBALL)
            .iconColor("§9")
            .effectType(Talent.TalentEffectType.OVERHEAT)
            .values(new double[]{0.10, 1.0, 3000, 4.0, 0.50}) // stack%, max%, reset_ms, wave_radius, frost_bonus%
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
                "§7Toutes les 15s, tempete auto de",
                "§7fleches enflammees.",
                "",
                "§6Fleches: §c20 §7| §6Degats: §c60%§7+brulure",
                "§6Zone: §e8 blocs §7| §bAuto: §f15s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(0)
            .icon(Material.NETHER_STAR)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.STEEL_STORM)
            .values(new double[]{15000, 0.60, 20, 8.0}) // cooldown_ms, damage_per_arrow% (buffed 50→60%), arrows, radius
            .internalCooldownMs(15000)
            .build());

        // 5.2 - VACHE (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_cow")
            .name("Vache")
            .description("Invoque une vache qui lance des bouses explosives")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque une vache qui lance des",
                "§7projectiles explosifs AoE.",
                "",
                "§6Degats: §c100% §7| §6AoE: §e4 blocs",
                "§6Portee: §e12 blocs §7| §6Cadence: §b3s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(1)
            .icon(Material.COW_SPAWN_EGG)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BEAST_COW)
            .values(new double[]{3000, 1.00, 4.0}) // cooldown_ms, damage_percent (buffed 80→100%), explosion_radius
            .build());

        // 5.3 - EXÉCUTION (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_execution")
            .name("Execution")
            .description("5 Points sur marqué = 250%/400% dégâts")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7A 5 Points d'Ombre: frappe devastatrice.",
                "§7Bonus sur cibles marquees.",
                "",
                "§6Normal: §c250% §7| §6Marque: §c400%",
                "§6Cout: §c5 §7Points | §6Kill: §a+2 §7Points"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(2)
            .icon(Material.NETHERITE_SWORD)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.EXECUTION)
            .values(new double[]{5, 2.5, 4.0, 2}) // points_needed, dmg_normal (250%), dmg_marked (400%), points_on_kill
            .build());

        // 5.4 - NÉCROSE (Voie du Poison) - Passif améliorant les dégâts sur cibles corrompues
        TALENTS.add(Talent.builder()
            .id("chasseur_epidemic")
            .name("Necrose")
            .description("+30% dégâts sur CORROMPUS (100% virulence)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Degats bonus sur les cibles",
                "§7a 100% de virulence (Corrompus).",
                "",
                "§6Bonus: §c+30% §7degats directs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.SLIME_BLOCK)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.EPIDEMIC)
            .values(new double[]{100, 0.30}) // virulence_threshold, damage_bonus
            .build());

        // 5.5 - GIVRE PÉNÉTRANT (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_absolute_perforation")
            .name("Givre Penetrant")
            .description("Givre ignore résistance, vulnérabilité!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Le givre ignore les resistances",
                "§7et amplifie les eclats de glace.",
                "",
                "§6Givre/rebond: §b+15% §7| §6Max: §b+60%",
                "§6Eclat: §c+30% §7rayon/degats"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ABSOLUTE_PERFORATION)
            .values(new double[]{0.15, 0.60, 5000, 0.30}) // frost_per_bounce% (nerfed 20→15%), max_bonus% (nerfed 80→60%), duration_ms, shatter_bonus% (nerfed 35→30%)
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
                "§7Kills avec pluie = charges. A 5:",
                "§7Super Pluie explosive!",
                "",
                "§6Fleches: §e×2 §7| §6Zone: §e+50%",
                "§6Degats: §c+50% §7+ explosions"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(0)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BARRAGE_FURY)
            .values(new double[]{5, 2.0, 1.5}) // charges_needed, arrow_multiplier, zone_multiplier
            .build());

        // 6.2 - LAMA (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_llama")
            .name("Lama")
            .description("Invoque un lama qui crache sur plusieurs cibles")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un lama qui crache sur",
                "§7plusieurs cibles avec Lenteur II.",
                "",
                "§6Cibles: §e3 §7| §6Portee: §e6 blocs",
                "§6Lenteur: §c3s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(1)
            .icon(Material.LLAMA_SPAWN_EGG)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.BEAST_LLAMA)
            .values(new double[]{3.5, 6.0, 3, 3000}) // base_damage, range, max_targets, slow_duration_ms
            .build());

        // 6.3 - DANSE MACABRE (Branche Ombre) - REFONTE
        TALENTS.add(Talent.builder()
            .id("chasseur_danse_macabre")
            .name("Danse Macabre")
            .description("Kill marqué = cascade de marques + frénésie!")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Kill marque = marque tous proches",
                "§7+ frenesie + Execution reduite.",
                "",
                "§6Rayon: §e8 blocs §7| §6Vitesse: §a+60%",
                "§6Prochaine Exec: §e3 §7Points | §6Points: §d+2"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DANSE_MACABRE)
            .values(new double[]{8.0, 5000, 5000, 0.60, 0.25, 6000, 3, 2}) // cascade_radius, mark_duration_ms, frenzy_duration_ms, speed_bonus (nerfed 80→60%), attack_speed_bonus (nerfed 30→25%), prepared_exec_duration_ms, prepared_exec_cost, points_gained
            .build());

        // 6.4 - SYNERGIE TOXIQUE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_toxic_synergy")
            .name("Synergie Toxique")
            .description("+1% dégâts par 10 virulence proche (max 25%)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Degats bonus selon la virulence",
                "§7totale des ennemis proches.",
                "",
                "§6Bonus: §c+1%§7/10 vir | §6Rayon: §e8 blocs",
                "§6Max: §c+25% §7degats"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.BREWING_STAND)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.TOXIC_SYNERGY)
            .values(new double[]{0.01, 8.0, 0.25}) // damage_per_10_virulence, range, max_bonus
            .build());

        // 6.5 - TEMPÊTE DE NEIGE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_hunter_momentum")
            .name("Tempete de Neige")
            .description("3 éclats = TEMPÊTE (+30% givre, vitesse)!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§73 morts givrees = Tempete de Neige.",
                "§7Boost de givre, vitesse et degats.",
                "",
                "§6Givre: §b+30% §7| §6Vitesse: §a+50%",
                "§6Degats: §c+30% §7| §6Duree: §b4s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.POWDER_SNOW_BUCKET)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.HUNTER_MOMENTUM)
            .values(new double[]{0.35, 2000, 1000, 3, 0.60, 0.50, 4000}) // speed%, base_duration_ms, extension_ms, shatter_kills, blizzard_bonus%, blizzard_speed%, blizzard_duration_ms
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
                "§7Pluies creent un vortex qui",
                "§7attire puis explose.",
                "",
                "§6Bonus aspires: §c+30% §7| §6Explosion: §c100%",
                "§6Rayon: §e5 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(0)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.CYCLONE_EYE)
            .values(new double[]{0.30, 1.0, 5.0, 0.25}) // dmg_bonus, explosion_dmg_mult, explosion_radius, pull_strength
            .build());

        // 7.2 - RENARD (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_fox")
            .name("Renard")
            .description("Invoque un renard qui traque et marque les proies")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un renard qui marque",
                "§7les ennemis (+35% degats subis).",
                "",
                "§6Marque: §b5s §7| §6Portee: §e10 blocs",
                "§6Cadence: §b4s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(1)
            .icon(Material.FOX_SPAWN_EGG)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.BEAST_FOX)
            .values(new double[]{4000, 5000, 0.35}) // pounce_cooldown_ms, mark_duration_ms, mark_damage_bonus (buffed 30→35%)
            .build());

        // 7.3 - LAMES SPECTRALES (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_spectral_blades")
            .name("Lames Spectrales")
            .description("10% chance sur kill = lames orbitales!")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chance sur kill: 5 lames orbitales",
                "§7tournent et blessent les ennemis.",
                "",
                "§6Chance: §e10% §7| §6Lames: §e5 §7| §6Duree: §b8s",
                "§6Degats: §c35%§7/lame | §6Marques: §c+50%"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.AMETHYST_SHARD)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_CLONE)
            .values(new double[]{0.10, 8000, 5, 3.0, 0.35, 2000}) // proc_chance (10%), duration_ms, blade_count, orbit_radius, damage_percent, rotation_period_ms
            .build());

        // 7.4 - PESTE NOIRE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_black_plague")
            .name("Peste Noire")
            .description("15% Lifesteal sur tous vos DoT!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos DoT de poison vous soignent.",
                "",
                "§6Lifesteal: §a15% §7des degats DoT"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.WITHER_ROSE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.BLACK_PLAGUE)
            .values(new double[]{0.15}) // lifesteal%
            .build());

        // 7.5 - ÉCHO GLACIAL (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_chain_perforation")
            .name("Echo Glacial")
            .description("Rebonds terminés = givre propage 3x!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Quand rebonds finissent, le givre",
                "§7se propage encore 3 fois.",
                "",
                "§6Echos: §e3 §7| §6Portee: §e10 blocs",
                "§6Givre: §b75%§7→§b50%§7→§b25% §7| §6Eclat: §e+2 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.ICE)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.CHAIN_PERFORATION)
            .values(new double[]{3, 10.0, 0.75, 0.50, 0.25, 2.0}) // max_echoes, range, frost_echo1%, frost_echo2%, frost_echo3%, shatter_radius_bonus
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
                "§7Vos pluies ont un rayon double.",
                "§7Chaque fleche explose en eclats.",
                "",
                "§6Rayon: §ex2 §7| §6Eclats: §e3§7/fleche",
                "§6Degats eclat: §c50% §7| §6Zone: §e2 blocs"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(0)
            .icon(Material.FIREWORK_STAR)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.DEVASTATING_SWARM)
            .values(new double[]{2.0, 3, 0.50, 2.0}) // radius_mult, fragment_count, fragment_damage% (buffed 40→50%), fragment_radius
            .build());

        // 8.2 - ABEILLE (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_bee")
            .name("Abeille")
            .description("Invoque une abeille avec essaim venimeux")
            .loreLines(new String[]{
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque une abeille qui pique 3 cibles.",
                "§7A 5 stacks: explosion de venin!",
                "",
                "§6Cibles: §e3 §7| §6Stacks max: §e5",
                "§6Explosion: §cx1.5 §7+ §2Poison II",
                "§bCadence: §f2s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(1)
            .icon(Material.BEE_SPAWN_EGG)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.BEAST_BEE)
            .values(new double[]{2000, 5, 1.5}) // sting_cooldown_ms, max_stacks, explosion_damage_mult
            .build());

        // 8.3 - TEMPÊTE D'OMBRE (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_storm")
            .name("Tempête d'Ombre")
            .description("Exécution kill = AoE + marque tous")
            .loreLines(new String[]{
                "§8§lBRANCHE OMBRE",
                "",
                "§7Quand Execution tue, declenche une",
                "§7tempete qui marque tous les ennemis.",
                "",
                "§6Zone: §e6 blocs §7| §6Degats: §c150%",
                "§6Bonus: §5+1 Point§7/ennemi touche"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(2)
            .icon(Material.WITHER_ROSE)
            .iconColor("§5§l")
            .effectType(Talent.TalentEffectType.SHADOW_STORM)
            .values(new double[]{6.0, 1.50, 1}) // radius, damage_mult, points_per_enemy
            .build());

        // 8.4 - FLEAU (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_blight")
            .name("Fléau")
            .description("Aura +5 virulence/s, COMBO à 200+!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Aura passive autour de vous.",
                "§7Combo a 200+ virulence proche.",
                "",
                "§6Aura: §e4 blocs §7| §6Virulence: §2+5§7/s",
                "§6Combo 200+: §c+20% §7degats"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.MYCELIUM)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.BLIGHT)
            .values(new double[]{4.0, 5, 200, 0.20}) // aura_range, virulence_per_tick, combo_threshold, combo_bonus
            .build());

        // 8.5 - HIVER ÉTERNEL (Voie du Givre) - PASSIF: s'active après 5 gels
        TALENTS.add(Talent.builder()
            .id("chasseur_devastation")
            .name("Hiver Éternel")
            .description("5 ennemis gelés = MODE HIVER 6s!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Apres 5 gels, mode Hiver Eternel.",
                "§7Aura de givre autour de vous.",
                "",
                "§6Activation: §e5 gels §7| §6Duree: §b6s",
                "§6Bonus: §b+40% givre §7| §c+35% degats",
                "§6Eclat: §c+40% §7| §6Rebonds: §e10 max",
                "§6Aura: §e4 blocs §7| §b+5%§7 givre/tick",
                "§bCooldown: §f30s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.BLUE_ICE)
            .iconColor("§b§l")
            .effectType(Talent.TalentEffectType.DEVASTATION)
            .values(new double[]{6000, 0.35, 0.40, 1.40, 30000, 5}) // duration_ms (nerfed 8s→6s), damage_bonus% (nerfed 40→35%), frost_bonus% (nerfed 50→40%), shatter_bonus (nerfed 1.50→1.40), cooldown_ms (nerfed 25s→30s), freezes_needed
            .internalCooldownMs(30000)
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
                "§6§l★ LEGENDAIRE ★",
                "§f§lVOIE DU BARRAGE",
                "",
                "§e2x Sneak§7: bombardement en ligne.",
                "§78 bombes explosent devant vous.",
                "",
                "§6Bombes: §e8 §7| §6Portee: §e30 blocs",
                "§6Rayon: §e3 blocs §7| §6Degats: §c500%",
                "§bCooldown: §f30s"
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

        // 9.2 - GOLEM DE FER (Voie des Bêtes - LÉGENDAIRE)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_iron_golem")
            .name("Golem de Fer")
            .description("Invoque un golem devastateur avec frappe titanesque")
            .loreLines(new String[]{
                "§6§l★ LEGENDAIRE ★",
                "§6§lVOIE DES BETES",
                "",
                "§7Invoque un Golem qui charge et",
                "§7frappe le sol avec onde de choc.",
                "",
                "§6Degats: §c12 §7| §6Zone: §e6 blocs",
                "§6Stun: §b1.5s §7| §bCadence: §f10s",
                "",
                "§6Synergie: §7Marques/Abeille = §cx2"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(1)
            .icon(Material.IRON_BLOCK)
            .iconColor("§7§l")
            .effectType(Talent.TalentEffectType.BEAST_IRON_GOLEM)
            .values(new double[]{10000, 12.0, 6.0}) // slam_cooldown_ms, damage (buffed 8→12), radius (buffed 5→6)
            .build());

        // 9.3 - AVATAR D'OMBRE (Branche Ombre - LÉGENDAIRE)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_avatar")
            .name("Avatar d'Ombre")
            .description("Ultime 15s: 2 clones, +1 Point/s, +40% dégâts")
            .loreLines(new String[]{
                "§5§l★ LEGENDAIRE ★",
                "§8§lBRANCHE OMBRE",
                "",
                "§e2x Sneak§7: transformation en Avatar.",
                "§72 clones permanents vous assistent.",
                "",
                "§6Duree: §b15s §7| §6Clones: §e2",
                "§6Bonus: §5+1 Point§7/s | §c+40% degats",
                "§bCooldown: §f45s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(2)
            .icon(Material.DRAGON_HEAD)
            .iconColor("§5§l")
            .effectType(Talent.TalentEffectType.SHADOW_AVATAR)
            .values(new double[]{15000, 2, 1000, 0.40, 45000}) // duration_ms, clone_count, point_interval_ms, damage_bonus (40%), cooldown_ms
            .internalCooldownMs(45000)
            .build());

        // 9.4 - AVATAR DE LA PESTE (Voie du Poison - LÉGENDAIRE)
        TALENTS.add(Talent.builder()
            .id("chasseur_plague_avatar")
            .name("Avatar de la Peste")
            .description("Ultime 15s: x3 virulence, aura 6 blocs, explosion finale!")
            .loreLines(new String[]{
                "§2§l★ LEGENDAIRE ★",
                "§2§lVOIE DU POISON",
                "",
                "§e2x Sneak§7: transformation en Avatar.",
                "§7Explosion finale a la fin du mode.",
                "",
                "§6Duree: §b15s §7| §6Virulence: §2x3",
                "§6Aura: §e6 blocs §7| §2+30 vir§7/s",
                "§6Explosion: §e8 blocs §7| §c500% degats",
                "§bCooldown: §f60s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(3)
            .icon(Material.DRAGON_HEAD)
            .iconColor("§2§l")
            .effectType(Talent.TalentEffectType.PLAGUE_AVATAR)
            .values(new double[]{15000, 6.0, 10, 8.0, 5.0, 60000}) // duration_ms, aura_range, virulence_per_tick, final_radius, final_damage_mult, cooldown_ms
            .internalCooldownMs(60000)
            .build());

        // 9.5 - ZÉRO ABSOLU (Voie du Givre - LÉGENDAIRE) - Activation instantanée
        TALENTS.add(Talent.builder()
            .id("chasseur_judgment")
            .name("Zéro Absolu")
            .description("2x SNEAK = Gel instantané 12 blocs!")
            .loreLines(new String[]{
                "§b§l★ LEGENDAIRE ★",
                "§b§lVOIE DU GIVRE",
                "",
                "§e2x Sneak§7: vague de gel instantane.",
                "§7Tous les ennemis sont immobilises.",
                "",
                "§6Zone: §e12 blocs §7| §6Degats: §c500%",
                "§6Givre: §b100% §7| §6Immob: §b2s",
                "§6Bonus degats: §c+50% §7sur geles",
                "§bCooldown: §f45s"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.NETHER_STAR)
            .iconColor("§b§l")
            .effectType(Talent.TalentEffectType.JUDGMENT)
            .values(new double[]{12.0, 5.0, 2000, 45000}) // range, damage_mult, freeze_duration_ms, cooldown_ms
            .internalCooldownMs(45000)
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
