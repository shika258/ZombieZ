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
            .description("Chance de tirer 3 flèches simultanément")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos tirs ont une chance de se",
                "§7transformer en salve de 3 flèches.",
                "",
                "§6Chance: §e35%",
                "§6Dégâts: §c100% §7par flèche"
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
            .description("Invoque une chauve-souris qui tire des ultrasons")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque une chauve-souris fidèle",
                "§7qui tire des ultrasons transperçants",
                "§7sur les ennemis proches.",
                "",
                "§6Portée: §e12 blocs",
                "§6Cadence: §b1.2s",
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
            .description("Génère des Points d'Ombre à chaque attaque")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chaque attaque génère des Points",
                "§7d'Ombre. À 3+ points, gagne un",
                "§7bonus de vitesse d'attaque.",
                "",
                "§6Points par attaque: §e+1",
                "§6Maximum: §e5 §7points",
                "§6Bonus (3+ pts): §a+30% §7vitesse d'attaque"
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
            .description("Chance d'appliquer de la Virulence aux ennemis")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Chance d'appliquer de la Virulence.",
                "§7Les paliers de Virulence débloquent",
                "§7des effets supplémentaires.",
                "",
                "§6Chance: §e50%",
                "§6Virulence appliquée: §c+18",
                "",
                "§e70%+ Virulence: §7Nécrose (§c+30% §7DoT)",
                "§e100% Virulence: §7Corrompu (§c+35% §7dégâts)"
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
            .name("Flèches Rebondissantes")
            .description("Les flèches rebondissent et appliquent du Givre")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Vos flèches rebondissent vers les",
                "§7ennemis proches et appliquent du",
                "§7Givre à chaque touche.",
                "",
                "§6Rebonds: §e2",
                "§6Givre par touche: §b+15%",
                "§6Bonus par rebond: §c+25% §7dégâts + §b12.5% §7givre",
                "",
                "§750% Givre: §7Ralenti | §7100%: §7Gelé"
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
            .description("Accumule des charges pour déclencher une salve massive")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Chaque tir réussi accumule une charge.",
                "§7À 8 charges, déclenche 3 salves de",
                "§75 flèches en éventail.",
                "",
                "§6Charges requises: §e8",
                "§6Flèches totales: §e15",
                "§6Dégâts: §c100% §7par flèche"
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
            .description("Invoque un parasite qui corrompt les ennemis")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un endermite du Vide qui",
                "§7s'accroche aux ennemis et les rend",
                "§7vulnérables avant d'exploser.",
                "",
                "§6Vulnérabilité: §c+25% §7dégâts subis",
                "§6Durée: §b3s",
                "§6Explosion: §c50% §7(§e4 blocs§7)"
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
            .description("Chance de déclencher un tir étourdissant")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chance de déclencher un tir d'ombre",
                "§7bonus qui étourdit la cible.",
                "",
                "§6Chance: §e15%",
                "§6Dégâts: §c150%",
                "§6Étourdissement: §c1s",
                "§6Portée: §e20 blocs"
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
            .description("Dégâts bonus contre les ennemis empoisonnés")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Inflige des dégâts bonus contre",
                "§7les ennemis affectés par la",
                "§7Virulence (empoisonnés).",
                "",
                "§6Bonus dégâts: §c+20%"
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
            .description("Accumule des charges pour un Tir Glacial")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chaque tir accumule une Charge.",
                "§7À 5 charges, le prochain tir devient",
                "§7un Tir Glacial dévastateur.",
                "",
                "§6Maximum: §e5 §7charges",
                "§6Givre par niveau: §b+5%",
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
            .name("Pluie de Flèches")
            .description("Chance de déclencher une pluie de flèches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos tirs ont une chance de",
                "§7déclencher une pluie de flèches",
                "§7sur la zone ciblée.",
                "",
                "§6Chance: §e30%",
                "§6Flèches: §e10",
                "§6Dégâts: §c45% §7par flèche",
                "§6Zone: §e5 blocs",
                "",
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
            .description("Invoque un loup dont les morsures font saigner")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un loup de chasse dont",
                "§7les morsures infligent un effet",
                "§7de saignement persistant.",
                "",
                "§6Dégâts: §c4",
                "§6Saignement: §c2§7/s",
                "§6Durée: §b5s"
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
            .description("Téléportation derrière la cible avec frappe bonus")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Se téléporte instantanément derrière",
                "§7la cible et assène une frappe",
                "§7puissante avec bonus de vitesse.",
                "",
                "§6Portée: §e16 blocs",
                "§6Dégâts: §c125%",
                "§6Vitesse: §a+50% §7(§b3s§7)",
                "§6Points d'Ombre: §d+2",
                "",
                "§bCooldown: §f5s §7| §eActivation: §fSneak + Attaque"
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
            .description("Les dégâts sur le temps peuvent critiquer")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos dégâts sur le temps peuvent",
                "§7désormais critiquer et appliquent",
                "§7un ralentissement progressif.",
                "",
                "§6Bonus DoT: §c+20%",
                "§6Critiques: §eOui",
                "§6Ralentissement progressif:",
                "§7  0-49% vir: §cNiveau I",
                "§7  50-69% vir: §cNiveau II",
                "§7  70%+ vir: §cNiveau III"
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
            .description("Crée une ligne de givre après 2+ rebonds")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Après avoir rebondi sur 2+ ennemis,",
                "§7crée une Ligne de Glace persistante",
                "§7qui applique du Givre bonus.",
                "",
                "§6Longueur: §e12 blocs",
                "§6Durée: §b3s",
                "§6Givre bonus: §b+30%"
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
            .name("Déluge")
            .description("Améliore les pluies de flèches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Améliore considérablement les",
                "§7pluies de flèches avec plus de",
                "§7vagues et de projectiles.",
                "",
                "§6Vagues bonus: §e+3",
                "§6Flèches: §e+50%"
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
            .description("Invoque un axolotl tireur de bulles")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un axolotl mignon mais",
                "§7redoutable qui tire des bulles",
                "§7d'eau sur les ennemis.",
                "",
                "§6Dégâts: §c3.5",
                "§6Portée: §e8 blocs",
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
            .description("Les coups critiques marquent les ennemis")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Vos coups critiques appliquent",
                "§7une Marque de Mort qui augmente",
                "§7les dégâts infligés à la cible.",
                "",
                "§6Durée: §b8s",
                "§6Bonus dégâts: §c+25%"
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
            .name("Pandémie")
            .description("Éliminer un empoisonné propage le poison")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Tuer un ennemi empoisonné propage",
                "§7la Virulence aux ennemis proches",
                "§7en réaction en chaîne.",
                "",
                "§6Cibles maximum: §e3",
                "§6Zone: §e5 blocs",
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

        // 4.5 - HYPOTHERMIA (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_overheat")
            .name("Hypothermie")
            .description("Accumule du froid pour déclencher une Vague de Froid")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Chaque tir accumule du froid.",
                "§7À 100%, déclenche une Vague de",
                "§7Froid dévastatrice.",
                "",
                "§6Accumulation: §b+10% §7par tir",
                "§6Maximum: §e100% §7(10 tirs)",
                "§6Réinitialisation: §b3s",
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
            .name("Tempête d'Acier")
            .description("Déclenche automatiquement une tempête de flèches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Toutes les 15 secondes, déclenche",
                "§7automatiquement une tempête de",
                "§7flèches enflammées.",
                "",
                "§6Flèches: §e20",
                "§6Dégâts: §c60% §7+ brûlure",
                "§6Zone: §e8 blocs",
                "§6Intervalle: §b15s §7(automatique)"
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
            .description("Invoque une vache lanceuse de projectiles explosifs")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque une vache de combat qui",
                "§7lance des projectiles explosifs",
                "§7sur les ennemis.",
                "",
                "§6Dégâts: §c100%",
                "§6Zone d'explosion: §e4 blocs",
                "§6Portée: §e12 blocs",
                "§6Cadence: §b3s"
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
            .name("Exécution")
            .description("Consomme 5 Points d'Ombre pour une frappe dévastatrice")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7À 5 Points d'Ombre, déclenche une",
                "§7frappe dévastatrice. Bonus massif",
                "§7contre les cibles marquées.",
                "",
                "§6Dégâts: §c250%",
                "§6Sur marqué: §c400%",
                "§6Coût: §e5 §7Points d'Ombre",
                "§6Kill: §a+2 §7Points"
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
            .name("Nécrose")
            .description("Dégâts bonus sur les cibles corrompues")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Inflige des dégâts bonus contre",
                "§7les cibles à 100% de Virulence",
                "§7(statut Corrompu).",
                "",
                "§6Seuil: §e100% §7Virulence",
                "§6Bonus dégâts: §c+30%"
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
            .name("Givre Pénétrant")
            .description("Le Givre ignore les résistances")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Votre Givre ignore les résistances",
                "§7ennemies et amplifie les éclats",
                "§7de glace à chaque rebond.",
                "",
                "§6Givre par rebond: §b+15%",
                "§6Maximum: §b+60%",
                "§6Bonus éclat: §c+30% §7rayon/dégâts"
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
            .description("Accumule des kills pour déclencher une Super Pluie")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Les kills avec Pluie de Flèches",
                "§7accumulent des charges. À 5 charges,",
                "§7déclenche une Super Pluie explosive.",
                "",
                "§6Charges requises: §e5 §7kills",
                "§6Flèches: §e×2",
                "§6Zone: §e+50%",
                "§6Dégâts: §c+50% §7+ explosions"
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
            .description("Invoque un lama cracheur multi-cibles")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un lama qui crache sur",
                "§7plusieurs cibles simultanément,",
                "§7appliquant un ralentissement.",
                "",
                "§6Cibles: §e3",
                "§6Portée: §e6 blocs",
                "§6Ralentissement: §cNiveau II §7(§b3s§7)"
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
            .description("Éliminer une cible marquée déclenche une cascade")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Éliminer une cible marquée propage",
                "§7la marque à tous les ennemis proches",
                "§7et octroie un état de frénésie.",
                "",
                "§6Zone: §e8 blocs",
                "§6Vitesse: §a+60%",
                "§6Prochaine Exécution: §e3 §7Points",
                "§6Points gagnés: §d+2"
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
            .description("Dégâts bonus selon la Virulence totale proche")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Gagne des dégâts bonus selon la",
                "§7Virulence totale accumulée sur",
                "§7les ennemis à proximité.",
                "",
                "§6Bonus: §c+1% §7par 10 Virulence",
                "§6Zone: §e8 blocs",
                "§6Maximum: §c+25%"
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
            .name("Tempête de Neige")
            .description("Déclenche une tempête après 3 morts givrées")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Après 3 éliminations d'ennemis givrés,",
                "§7déclenche une Tempête de Neige qui",
                "§7amplifie vos capacités.",
                "",
                "§6Givre bonus: §b+30%",
                "§6Vitesse: §a+50%",
                "§6Dégâts: §c+30%",
                "§6Durée: §b4s"
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
            .name("Œil du Cyclone")
            .description("Les pluies créent un vortex qui attire et explose")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos Pluies de Flèches créent un",
                "§7vortex qui attire les ennemis",
                "§7avant d'exploser violemment.",
                "",
                "§6Bonus sur aspirés: §c+30%",
                "§6Explosion: §c100%",
                "§6Zone: §e5 blocs"
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
            .description("Invoque un renard traqueur qui marque les proies")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un renard rusé qui traque",
                "§7et marque les ennemis, les rendant",
                "§7vulnérables à vos attaques.",
                "",
                "§6Vulnérabilité: §c+35%",
                "§6Durée marque: §b5s",
                "§6Portée: §e10 blocs",
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
            .description("Chance d'invoquer des lames orbitales sur kill")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Chance sur élimination d'invoquer",
                "§75 lames spectrales qui orbitent",
                "§7et blessent les ennemis proches.",
                "",
                "§6Chance: §e10%",
                "§6Lames: §e5",
                "§6Durée: §b8s",
                "§6Dégâts: §c35% §7par lame",
                "§6Bonus marqués: §c+50%"
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
            .description("Vos dégâts sur le temps vous soignent")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos dégâts sur le temps (DoT)",
                "§7de poison vous soignent d'un",
                "§7pourcentage des dégâts infligés.",
                "",
                "§6Lifesteal: §a15%"
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
            .name("Écho Glacial")
            .description("Le Givre se propage 3 fois après les rebonds")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Quand les rebonds se terminent,",
                "§7le Givre se propage encore 3 fois",
                "§7aux ennemis proches.",
                "",
                "§6Échos: §e3",
                "§6Portée: §e10 blocs",
                "§6Givre: §b75% §7→ §b50% §7→ §b25%",
                "§6Bonus éclat: §e+2 blocs"
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
            .name("Nuée Dévastatrice")
            .description("Double la zone et ajoute la fragmentation")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos Pluies de Flèches ont un rayon",
                "§7doublé et chaque flèche explose",
                "§7en éclats meurtriers.",
                "",
                "§6Rayon: §e×2",
                "§6Éclats par flèche: §e3",
                "§6Dégâts éclat: §c50%",
                "§6Zone éclat: §e2 blocs"
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
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque une abeille qui pique",
                "§7plusieurs cibles. À 5 stacks de",
                "§7piqûres, provoque une explosion.",
                "",
                "§6Cibles simultanées: §e3",
                "§6Stacks maximum: §e5",
                "§6Explosion: §c×1.5 §7+ §2Poison II",
                "§6Cadence: §b2s"
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
            .description("Exécution létale déclenche une tempête")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Lorsque Exécution tue un ennemi,",
                "§7déclenche une tempête d'ombre qui",
                "§7marque tous les ennemis proches.",
                "",
                "§6Zone: §e6 blocs",
                "§6Dégâts: §c150%",
                "§6Bonus: §d+1 Point §7par ennemi touché"
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
            .description("Aura de virulence avec bonus de combo")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Émet une aura passive qui applique",
                "§7de la Virulence. À 200+ Virulence",
                "§7totale proche, gagne un bonus.",
                "",
                "§6Zone aura: §e4 blocs",
                "§6Virulence: §2+5§7/s",
                "§6Combo (200+): §c+20% §7dégâts"
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
            .description("Après 5 gels, entre en mode Hiver Éternel")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Après avoir gelé 5 ennemis, entre",
                "§7en mode Hiver Éternel avec une",
                "§7aura de givre dévastatrice.",
                "",
                "§6Activation: §e5 §7gels",
                "§6Durée: §b6s",
                "§6Givre bonus: §b+40%",
                "§6Dégâts: §c+35%",
                "§6Éclat: §c+40%",
                "§6Rebonds max: §e10",
                "§6Aura: §e4 blocs §7(§b+5%§7 givre/tick)",
                "",
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
            .description("Déclenche un bombardement orbital dévastateur")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Invoque une pluie de 8 bombes en",
                "§7ligne devant vous qui explosent",
                "§7en chaîne dévastatrice.",
                "",
                "§6Bombes: §e8",
                "§6Portée: §e30 blocs",
                "§6Rayon explosion: §e3 blocs",
                "§6Dégâts: §c500%",
                "",
                "§bCooldown: §f30s §7| §eActivation: §fDouble Sneak"
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
            .description("Invoque un golem dévastateur avec frappe titanesque")
            .loreLines(new String[]{
                "§6§l★ LÉGENDAIRE ★",
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un Golem de Fer qui charge",
                "§7les ennemis et frappe le sol avec",
                "§7une onde de choc dévastatrice.",
                "",
                "§6Dégâts: §c12",
                "§6Zone: §e6 blocs",
                "§6Étourdissement: §b1.5s",
                "§6Cadence: §b10s",
                "",
                "§aSynergie: §7×2 dégâts sur marqués/piqués"
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
            .description("Se transforme en Avatar d'Ombre avec des clones")
            .loreLines(new String[]{
                "§5§l★ LÉGENDAIRE ★",
                "§5§lVOIE DE L'OMBRE",
                "",
                "§7Se transforme en Avatar d'Ombre.",
                "§72 clones permanents vous assistent",
                "§7pendant toute la durée.",
                "",
                "§6Durée: §b15s",
                "§6Clones: §e2",
                "§6Points d'Ombre: §d+1§7/s",
                "§6Bonus dégâts: §c+40%",
                "",
                "§bCooldown: §f45s §7| §eActivation: §fDouble Sneak"
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
            .description("Se transforme en Avatar de pestilence avec explosion finale")
            .loreLines(new String[]{
                "§2§l★ LÉGENDAIRE ★",
                "§2§lVOIE DU POISON",
                "",
                "§7Se transforme en Avatar de la Peste.",
                "§7Déclenche une explosion cataclysmique",
                "§7à la fin de la transformation.",
                "",
                "§6Durée: §b15s",
                "§6Virulence: §2×3",
                "§6Aura: §e6 blocs §7(§2+30§7/s)",
                "§6Explosion finale: §e8 blocs §7| §c500%",
                "",
                "§bCooldown: §f60s §7| §eActivation: §fDouble Sneak"
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
            .description("Déclenche une vague de gel instantané")
            .loreLines(new String[]{
                "§b§l★ LÉGENDAIRE ★",
                "§b§lVOIE DU GIVRE",
                "",
                "§7Déclenche une vague de froid absolu",
                "§7qui gèle instantanément tous les",
                "§7ennemis dans une zone massive.",
                "",
                "§6Zone: §e12 blocs",
                "§6Dégâts: §c500%",
                "§6Givre: §b100% §7(instantané)",
                "§6Immobilisation: §b2s",
                "§6Bonus dégâts sur gelés: §c+50%",
                "",
                "§bCooldown: §f45s §7| §eActivation: §fDouble Sneak"
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
