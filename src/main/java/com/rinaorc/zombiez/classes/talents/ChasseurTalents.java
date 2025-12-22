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
            .description("30% chance de tirer 3 fleches")
            .loreLines(new String[]{
                "§f§lVOIE DU BARRAGE",
                "",
                "§7Vos tirs a l'arc ou l'arbalete",
                "§7ont §e30%§7 de chance de tirer",
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
            .values(new double[]{0.30, 2}) // chance (buffed 25→30%), extra_projectiles (2 bonus = 3 total)
            .build());

        // 1.2 - CHAUVE-SOURIS (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_bat")
            .name("Chauve-souris")
            .description("Invoque une chauve-souris a ultrasons")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque une §8chauve-souris§7 qui",
                "§7emet des ultrasons devastateurs.",
                "",
                "§6CAPACITE - ULTRASON:",
                "§7Tire une onde sonore vers",
                "§7l'ennemi le plus proche.",
                "§cTransperce §7tous les ennemis",
                "§7sur sa trajectoire!",
                "",
                "§b~ Portee: §e12 blocs",
                "§b~ Cadence: §e1.5s",
                "§a✦ INVINCIBLE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(1)
            .icon(Material.BAT_SPAWN_EGG)
            .iconColor("§8")
            .effectType(Talent.TalentEffectType.BEAST_BAT)
            .values(new double[]{1.5}) // base_damage
            .build());

        // 1.3 - LAME D'OMBRE (Branche Ombre - Refonte)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_blade")
            .name("Lame d'Ombre")
            .description("Attaques = Points d'Ombre")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§6► LAME D'OMBRE",
                "§7Attaques génèrent §d+1 Point d'Ombre",
                "§7À §f3+ Points§7: §a+30%§7 vitesse d'attaque",
                "",
                "§8Points max: 5 (affichés dans l'ActionBar)"
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
            .description("40% virulence, 70%+ = Nécrose (+25%)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos attaques ont §a40%§7 de chance",
                "§7d'appliquer §2+15 Virulence§7.",
                "",
                "§6SYSTÈME DE VIRULENCE (0-100):",
                "§8► DoT: §c8%§8 dégâts par 10 virulence",
                "§8► Tick: §e2x/seconde§8 (rapide!)",
                "",
                "§c§lNÉCROSE (70%+):",
                "§8► §c+25%§8 dégâts de DoT!",
                "",
                "§d§lCORROMPU (100%):",
                "§8► §d+30%§8 dégâts directs",
                "§8► Propagation à la mort!",
                "",
                "§2§lINFECTEZ VOS PROIES"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.VENOMOUS_STRIKE)
            .values(new double[]{0.40, 100, 70, 0.25}) // chance, max_virulence, necrosis_threshold (70%), necrosis_bonus (25%)
            .build());

        // 1.5 - FLÈCHES REBONDISSANTES (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_piercing_arrows")
            .name("Flèches Rebondissantes")
            .description("Rebondit vers 2 ennemis, applique GIVRE!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Vos projectiles §brebondissent§7",
                "§7vers jusqu'à §e2 ennemis§7 et",
                "§7appliquent du §b§lGIVRE§7!",
                "",
                "§6SYSTÈME DE REBOND:",
                "§8► Touche un ennemi → rebondit",
                "§8► vers le mob le plus proche!",
                "§8► Portée rebond: §e10 blocs",
                "",
                "§6SYSTÈME DE GIVRE:",
                "§8► §b+15%§8 givre par touche",
                "§8► §b50%§8 = §9RALENTI§8 (-30% vitesse)",
                "§8► §b100%§8 = §b§lGELÉ§8 (2s immobile)",
                "",
                "§c§lBONUS GELÉ:",
                "§8► Cibles gelées: §c+50%§8 dégâts!",
                "",
                "§6MOMENTUM DE REBOND:",
                "§8► Chaque rebond: §c+25%§8 dégâts",
                "§8► §b+12.5%§8 givre bonus!",
                "",
                "§b§lGELEZ VOS ENNEMIS EN CHAÎNE"
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
                "§6ACCUMULATION:",
                "§7Chaque fleche qui touche",
                "§7accumule §e1 charge§7 de Rafale.",
                "",
                "§6RAFALE EN EVENTAIL:",
                "§7A §e8 charges§7, votre prochain",
                "§7tir libere §c3 salves§7 de",
                "§c5 fleches§7 en eventail!",
                "",
                "§6CADENCE DE FEU:",
                "§7Intervalle: §e0.4s§7 entre salves",
                "§7Total: §c15 fleches§7 devastatrices!",
                "",
                "§8► Charges max: §f8",
                "§8► Salves: §c3",
                "§8► Fleches/salve: §c5",
                "§8► Degats/fleche: §c100%",
                "",
                "§d§lSYNERGIE: Tirs Multiples!"
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
                "§5§lVOIE DES BÊTES",
                "",
                "§7Invoque un §5endermite§7 qui",
                "§7parasite et corrompt ses proies.",
                "",
                "§6CAPACITÉ - INFESTATION DU VIDE:",
                "§7Se §5téléporte§7 sur un ennemi,",
                "§7s'accroche §e3s§7 et applique:",
                "",
                "§5✦ CORRUPTION DU VIDE:",
                "§7Cible subit §c+25%§7 de dégâts!",
                "",
                "§c✦ EXPLOSION DU VIDE:",
                "§7Après 3s: dégâts AoE + téléport",
                "§7vers une §enouvelle cible§7!",
                "",
                "§d§lSYNERGIE:",
                "§7+Marque Renard = §c+55%§7 dégâts!",
                "§7Priorise cibles §csaignantes§7/§6marquées§7"
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
                "§6► TIR D'OMBRE",
                "§7Vos attaques ont §e15%§7 de chance",
                "§7de déclencher un §5tir de pistolet§7!",
                "",
                "§6EFFET DU TIR:",
                "§8► §c150%§8 des dégâts du joueur",
                "§8► §9Étourdit§8 la cible §e1s",
                "§8► Portée max: §b20 blocs",
                "",
                "§5§lFRAPPEZ DEPUIS LES OMBRES"
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
            .description("+15% dégâts sur empoisonnés!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison §2ronge§7 les défenses!",
                "",
                "§6BONUS DÉGÂTS:",
                "§8► §c+15%§8 dégâts sur cibles",
                "§8► empoisonnées (virulence > 0)",
                "",
                "§6SYNERGIE VIRULENCE:",
                "§8► Plus de virulence = plus",
                "§8► d'opportunités de bonus!",
                "§8► Stack avec Nécrose/Corrompu",
                "",
                "§2§lRONGEZ LEUR DÉFENSE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.FERMENTED_SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.CORROSIVE_VENOM)
            .values(new double[]{0.15, 6000, 0.15}) // damage_bonus%, virulence_duration_ms, damage_bonus%
            .build());

        // 2.5 - CHARGE GLACIALE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_caliber")
            .name("Charge Glaciale")
            .description("Charge 1-5, à 5 = TIR GLACIAL!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Système de §bCharge Glaciale§7 (1-5).",
                "§7Chaque tir augmente la Charge.",
                "",
                "§6CHARGE CROISSANTE:",
                "§8► §b+1 Charge§8 par tir",
                "§8► §b+5%§8 givre appliqué par niveau",
                "",
                "§b§lÀ CHARGE 5 - TIR GLACIAL:",
                "§8► §b§lGEL INSTANTANÉ§8 de la cible!",
                "§8► §a+1§8 rebond supplémentaire",
                "§8► Son de glace satisfaisant",
                "§8► Reset la Charge à 0",
                "",
                "§8Charge affichée: §b❄❄❄❄❄",
                "",
                "§b§lCHARGEZ LE FROID"
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

        // 3.2 - LOUP (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_wolf")
            .name("Loup")
            .description("Invoque un loup qui inflige du saignement")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un §7loup sauvage§7 aux",
                "§7crocs empoisonnés de venin.",
                "",
                "§6CAPACITÉ - SAIGNEMENT:",
                "§7Ses morsures infligent un §cDoT§7",
                "§7pendant §e5s§7 (dégâts/seconde).",
                "",
                "§8Traque les ennemis blessés"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(1)
            .icon(Material.WOLF_SPAWN_EGG)
            .iconColor("§7")
            .effectType(Talent.TalentEffectType.BEAST_WOLF)
            .values(new double[]{3.0, 5000, 1.5}) // base_damage, bleed_duration_ms, bleed_damage_per_tick
            .build());

        // 3.3 - PAS DE L'OMBRE (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_step")
            .name("Pas de l'Ombre")
            .description("Shift+Attaque = téléport derrière (16 blocs)")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§6► PAS DE L'OMBRE",
                "§7§eShift + Attaque§7 = téléport derrière",
                "§7la cible (portée §e16 blocs§7)",
                "",
                "§6FRAPPE D'OMBRE:",
                "§8► Inflige §c125%§8 dégâts à l'arrivée",
                "",
                "§6ÉLAN SPECTRAL:",
                "§8► §b+50% vitesse§8 pendant §f3s",
                "",
                "§7+ §d+2 Points d'Ombre",
                "§b⚡ §f5s§7 cooldown"
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
            .description("DoT peut CRIT! Ralentissement toxique")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison devient §cléthal§7!",
                "",
                "§6CRITIQUES TOXIQUES:",
                "§8► Le DoT peut §eCRIT§8!",
                "§8► Utilise vos stats de crit",
                "§8► Particules spéciales sur crit",
                "",
                "§6PARALYSIE:",
                "§8► Empoisonnés: §9Ralentis§8",
                "§8► §9Slow II§8 à 70%+ virulence",
                "§8► Facilite les combos!",
                "",
                "§2§lLA MORT LENTE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(3)
            .icon(Material.POISONOUS_POTATO)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.DEADLY_TOXINS)
            .values(new double[]{0.25, 0.50, 0.30}) // crit_chance, crit_bonus, slow%
            .build());

        // 3.5 - LIGNE DE GLACE (Voie du Givre)
        TALENTS.add(Talent.builder()
            .id("chasseur_fatal_trajectory")
            .name("Ligne de Glace")
            .description("2+ rebonds = Zone de Givre (+30% givre)")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Rebondir sur §e2+ ennemis§7 crée",
                "§7une §b§lLIGNE DE GLACE§7!",
                "",
                "§6LIGNE DE GLACE:",
                "§8► Zone linéaire de §e12 blocs",
                "§8► Entre la 1ère et dernière cible",
                "§8► Durée: §e3s",
                "§8► Particules de neige",
                "",
                "§b§lENNEMIS DANS LA LIGNE:",
                "§8► Reçoivent §b+30%§8 givre bonus",
                "§8► Facilite le gel en zone!",
                "",
                "§6SYNERGIE:",
                "§7Parfait pour geler les groupes",
                "§7sur la trajectoire de rebond!",
                "",
                "§b§lTRACEZ LE FROID"
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

        // 4.2 - AXOLOTL (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_axolotl")
            .name("Axolotl")
            .description("Invoque un axolotl qui tire des bulles d'eau")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un §daxolotl mystique§7",
                "§7qui maitrise l'eau comme arme.",
                "",
                "§6CAPACITÉ - BULLES D'EAU:",
                "§7Tire des projectiles aquatiques",
                "§7sur les ennemis proches.",
                "",
                "§b~ Portée: §e8 blocs",
                "§b~ Cadence: §e1.5s"
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
                "§8§lBRANCHE OMBRE",
                "",
                "§7Vos §ecoups critiques§7 marquent",
                "§7l'ennemi pendant §c8s§7!",
                "",
                "§6MARQUE DE MORT:",
                "§8► §c+25%§8 dégâts subis",
                "§8► §eGlowing§8 (visible à travers murs)",
                "§8► §5Synergie§8 avec Exécution",
                "",
                "§5§lDÉVOILEZ VOS PROIES"
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
            .description("Kill empoisonné = PROPAGATION en chaîne!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Tuer un ennemi §2empoisonné§7",
                "§7§dpropage§7 le poison!",
                "",
                "§6PROPAGATION TOXIQUE:",
                "§8► Jusqu'à §e3§8 cibles proches",
                "§8► Rayon: §e5§8 blocs",
                "§8► §2+40 Virulence§8 transmise",
                "",
                "§d§lBONUS CORROMPU (100%):",
                "§8► §d+60 Virulence§8 transmise!",
                "§8► Chaînes de poison animées",
                "",
                "§2§lPROPAGEZ LA PESTE"
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
                "§7Vos tirs accumulent le §bfroid§7!",
                "",
                "§6ACCUMULATION:",
                "§8► §b+10%§8 hypothermie par tir",
                "§8► Maximum: §b100%§8 (10 tirs)",
                "§8► Reset après §e3s§8 sans tirer",
                "",
                "§b§lÀ 100% - VAGUE DE FROID:",
                "§8► Le prochain tir déclenche une",
                "§8► §b§lVAGUE DE FROID§8 AoE!",
                "§8► Zone: §e4 blocs",
                "§8► §b30-70%§8 givre appliqué",
                "§8► Reset l'hypothermie",
                "",
                "§6JAUGE: §9████████████",
                "",
                "§b§lREFROIDISSEZ-LES!"
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
            .values(new double[]{15000, 0.60, 20, 8.0}) // cooldown_ms, damage_per_arrow% (buffed 50→60%), arrows, radius
            .internalCooldownMs(15000)
            .build());

        // 5.2 - VACHE (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_cow")
            .name("Vache")
            .description("Invoque une vache qui lance des bouses explosives")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque une §6vache§7... explosive.",
                "§7Ne posez pas de questions.",
                "",
                "§6CAPACITÉ - BOUSE PROPULSÉE:",
                "§7Lance une bouse explosive vers",
                "§7les groupes d'ennemis! Explose",
                "§7à l'impact avec dégâts AoE.",
                "",
                "§b~ Portée: §e12 blocs",
                "§b~ Cadence: §e3s",
                "§c✦ Dégâts de zone + Knockback"
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
            .description("5 Points sur marqué = 250%/400% dégâts")
            .loreLines(new String[]{
                "§8§lBRANCHE OMBRE",
                "",
                "§7À §55 Points d'Ombre§7, attaquez",
                "§7une cible §cmarquée§7 pour déclencher",
                "§7une §c§lEXÉCUTION§7!",
                "",
                "§6DÉGÂTS:",
                "§8► Cible normale: §c250%§8 dégâts",
                "§8► Cible marquée: §c§l400%§8 dégâts!",
                "",
                "§6EFFET:",
                "§8► Consomme §55 Points§8",
                "§8► Génère +2 Points si kill",
                "",
                "§5§lLE MOMENT DE VÉRITÉ"
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
            .description("+30% dégâts sur CORROMPUS (100% virulence)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos cibles §dcorrompues§7 sont",
                "§7condamnées à une mort certaine!",
                "",
                "§d§lBONUS CORROMPU (passif):",
                "§8► §c+30%§8 dégâts directs",
                "§8► Sur cibles à §d100% virulence§8",
                "§8► S'applique à TOUT vos dégâts!",
                "",
                "§6STRATÉGIE:",
                "§8► Montez la virulence à 100%",
                "§8► Profitez du bonus massif!",
                "§8► Les DoT font le reste...",
                "",
                "§2§lLEUR DESTIN EST SCELLÉ"
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
            .description("Givre ignore résistance, vulnérabilité!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Votre givre §bpénètre§7 les défenses!",
                "",
                "§6GIVRE PÉNÉTRANT:",
                "§8► §b+20%§8 givre par rebond",
                "§8► Ignore la résistance au froid",
                "§8► Maximum: §b+80%§8 givre bonus",
                "",
                "§b§lÉCLAT AMPLIFIÉ:",
                "§8► Morts de gelés: §b+35%§8 rayon",
                "§8► §c+35%§8 dégâts d'éclat!",
                "§8► Propagation givre améliorée",
                "",
                "§6SYNERGIE:",
                "§7Parfait avec Charge Glaciale",
                "§7et Ligne de Glace!",
                "",
                "§b§lAUCUNE DÉFENSE NE RÉSISTE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.HEART_OF_THE_SEA)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.ABSOLUTE_PERFORATION)
            .values(new double[]{0.20, 0.80, 5000, 0.35}) // frost_per_bounce%, max_bonus%, duration_ms, shatter_bonus%
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

        // 6.2 - LAMA (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_llama")
            .name("Lama")
            .description("Invoque un lama qui crache sur plusieurs cibles")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un §elama hautain§7 qui",
                "§7méprise tous vos ennemis.",
                "",
                "§6CAPACITÉ - CRACHAT ACIDE:",
                "§7Crache sur §e3 cibles§7 simultanément.",
                "§7Inflige des dégâts + §9Lenteur II§7.",
                "",
                "§b~ Portée: §e6 blocs",
                "§b~ Durée lenteur: §e3s"
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
                "§5§lBRANCHE OMBRE",
                "",
                "§7Tuer une cible §cmarquée§7 déclenche",
                "§7la §5§lDanse Macabre§7!",
                "",
                "§c§l⚔ CASCADE DE MORT:",
                "§8► §dMarque§8 TOUS les ennemis à §e8 blocs",
                "§8► Durée des marques: §e5s",
                "",
                "§6§l⚡ FRÉNÉSIE D'OMBRE (5s):",
                "§8► §a+80%§8 vitesse de déplacement",
                "§8► §c+30%§8 vitesse d'attaque",
                "",
                "§5§l✧ EXÉCUTION PRÉPARÉE:",
                "§8► Prochaine Exécution: §e3 Points§8 seulement!",
                "§8► Durée du bonus: §e6s",
                "",
                "§8► §bReset§8 Pas de l'Ombre",
                "§8► §5+2 Points§8 d'Ombre",
                "",
                "§d§lSYNERGIE:",
                "§7Cascade → Exécutions en chaîne",
                "§7→ Tempête d'Ombre sur groupes!",
                "",
                "§5§l★ LE BAL DES MORTS ★"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DANSE_MACABRE)
            .values(new double[]{8.0, 5000, 5000, 0.80, 0.30, 6000, 3, 2}) // cascade_radius, mark_duration_ms, frenzy_duration_ms, speed_bonus, attack_speed_bonus, prepared_exec_duration_ms, prepared_exec_cost, points_gained
            .build());

        // 6.4 - SYNERGIE TOXIQUE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_toxic_synergy")
            .name("Synergie Toxique")
            .description("+1% dégâts par 10 virulence proche (max 25%)")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7La virulence vous §arenforce§7!",
                "",
                "§6SYNERGIE DE VIRULENCE:",
                "§8► §c+1%§8 dégâts par 10 virulence",
                "§8► Compte dans §e8 blocs§8 autour",
                "§8► Maximum: §c+25%§8 dégâts!",
                "",
                "§6EXEMPLE:",
                "§7- 5 ennemis à 40 virulence",
                "§7- Total = 200 virulence",
                "§7- Bonus = §c+20%§8 dégâts!",
                "",
                "§2§lLE POISON VOUS NOURRIT"
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
            .description("3 éclats = TEMPÊTE (+30% givre, vitesse)!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Les §béclats§7 de glace vous",
                "§7propulsent vers l'avant!",
                "",
                "§6ÉCLAT DE GLACE:",
                "§8► Chaque mort givrée compte",
                "§8► Compteur d'éclats",
                "",
                "§b§l3 ÉCLATS - TEMPÊTE DE NEIGE:",
                "§8► §b+30%§8 givre appliqué!",
                "§8► §a+Vitesse§8 de déplacement",
                "§8► §c+30%§8 dégâts",
                "§8► Durée: §e4s",
                "",
                "§6RÉACTION EN CHAÎNE:",
                "§7Les éclats en chaîne maintiennent",
                "§7la tempête active!",
                "",
                "§b§lDÉCHAÎNEZ LE BLIZZARD"
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

        // 7.2 - RENARD (Voie des Bêtes)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_fox")
            .name("Renard")
            .description("Invoque un renard qui traque et marque les proies")
            .loreLines(new String[]{
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un §6renard rusé§7 qui",
                "§7traque et marque ses proies.",
                "",
                "§6CAPACITÉ - TRAQUE & BOND:",
                "§7Bondit sur les ennemis blessés",
                "§7et les §cmarque§7 pendant §e5s§7.",
                "",
                "§c✦ MARQUE: §f+30% §7dégâts subis!",
                "",
                "§b~ Portée: §e10 blocs",
                "§b~ Cadence: §e4s"
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
        // Lames d'ombre orbitales style Vampire Survivors - 10% chance sur kill
        TALENTS.add(Talent.builder()
            .id("chasseur_spectral_blades")
            .name("Lames Spectrales")
            .description("10% chance sur kill = lames orbitales!")
            .loreLines(new String[]{
                "§5§lBRANCHE OMBRE",
                "",
                "§7Chaque §ckill§7 a §e10%§7 de chance",
                "§7d'invoquer des §5§lLAMES SPECTRALES§7!",
                "",
                "§6LAMES D'OMBRE:",
                "§8► §e5 lames§8 tournent autour de vous",
                "§8► Durée: §e8s",
                "§8► Rayon orbital: §b3 blocs",
                "§8► Vitesse: §e1 tour/2s",
                "",
                "§c§lDÉGÂTS AUTOMATIQUES:",
                "§8► §c35%§8 de vos dégâts par lame",
                "§8► Frappe chaque ennemi traversé",
                "§8► Cooldown par cible: §e0.5s",
                "",
                "§d§lSYNERGIE MARQUE:",
                "§8► Cibles marquées: §c+50%§8 dégâts",
                "§8► §e15%§8 chance de marquer",
                "",
                "§6EFFET VISUEL:",
                "§7Trainées violettes spectaculaires!",
                "",
                "§5§l★ DANSE DES LAMES ★"
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
                "§7Votre poison vous §aSUSTENTE§7!",
                "",
                "§a§lDRAIN DE VIE:",
                "§8► §a15%§8 lifesteal sur DoT",
                "§8► Chaque tick vous soigne!",
                "§8► Stack avec vos stats de vie",
                "",
                "§6SYNERGIE:",
                "§8► Plus de virulence = plus de DoT",
                "§8► Plus de DoT = plus de soin!",
                "§8► Crits DoT = gros heal!",
                "",
                "§0§lLA MORT INCARNÉE"
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
            .description("Après dernier rebond, givre se propage 3x!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Après le dernier §brebond§7,",
                "§7le §bgivre se propage§7!",
                "",
                "§6PROPAGATION EN CHAÎNE:",
                "§8► Jusqu'à §e3§8 propagations",
                "§8► Vers l'ennemi le plus proche",
                "§8► Portée: §e10 blocs",
                "",
                "§6GIVRE PAR ÉCHO:",
                "§8► 1er écho: §b75%§8 givre",
                "§8► 2ème écho: §b50%§8 givre",
                "§8► 3ème écho: §b25%§8 givre",
                "",
                "§6BONUS ÉCLAT:",
                "§8► §b+2 blocs§8 rayon d'éclat",
                "§8► Plus de cibles touchées!",
                "",
                "§6SYNERGIE:",
                "§7Continue le travail des rebonds",
                "§7pour geler encore plus d'ennemis!",
                "",
                "§b§lLE FROID SE RÉPAND"
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
                "§7Invoque une §eabeille guerrière§7",
                "§7qui déchaîne son essaim.",
                "",
                "§6CAPACITÉ - ESSAIM VENIMEUX:",
                "§7Lance des piqûres sur §e3 cibles§7.",
                "§7Chaque piqûre ajoute §c1 stack§7.",
                "",
                "§c✦ À 5 STACKS: EXPLOSION DE VENIN!",
                "§7Dégâts massifs + §2Poison II",
                "",
                "§b~ Cadence: §e2s"
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
                "§7Quand une §cExécution§7 tue une",
                "§7cible, déclenchez une",
                "§5§lTEMPÊTE D'OMBRE§7!",
                "",
                "§6TEMPÊTE:",
                "§8► Zone: §e6§8 blocs",
                "§8► Dégâts: §c150%§8 de l'Exécution",
                "§8► §cMarque§8 tous les touchés",
                "§8► §5+1 Point§8 par ennemi touché",
                "",
                "§6RÉACTION EN CHAÎNE:",
                "§7Parfait pour nettoyer les groupes!",
                "",
                "§5§lL'OMBRE CONSUME TOUT"
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
                "§7Vous §2IRRADIEZ§7 le poison!",
                "",
                "§6AURA DE FLÉAU (passive):",
                "§8► §e4 blocs§8 autour de vous",
                "§8► §2+5 virulence§8/seconde",
                "§8► Empoisonne automatiquement!",
                "",
                "§c§lCOMBO FLÉAU (200+ virulence):",
                "§8► 200+ virulence totale proche",
                "§8► = §c+20%§8 TOUS vos dégâts!",
                "§8► Indicateur §c§lCOMBO!§8 visible",
                "",
                "§2§lVOUS ÊTES LA PESTE"
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
            .description("5 ennemis gelés = MODE HIVER 8s!")
            .loreLines(new String[]{
                "§b§lVOIE DU GIVRE",
                "",
                "§7Après avoir §bgelé 5 ennemis§7,",
                "§7entrez en mode §b§lHIVER ÉTERNEL§7!",
                "",
                "§6ACTIVATION AUTOMATIQUE:",
                "§8► Gelez §e5 ennemis§8",
                "§8► Le mode s'active seul!",
                "§8► Compteur visible dans l'ActionBar",
                "",
                "§b§lMODE HIVER ÉTERNEL (8s):",
                "§8► §b+50%§8 givre appliqué!",
                "§8► §c+40%§8 dégâts",
                "§8► §c+50%§8 dégâts d'éclat",
                "§8► §a∞ Rebonds infinis§8!",
                "",
                "§6AURA DE GIVRE:",
                "§8► §e4 blocs§8 autour de vous",
                "§8► §b+8%§8 givre/tick aux ennemis",
                "§8► Gel passif automatique!",
                "",
                "§8► Cooldown: §c25s§8 après fin",
                "",
                "§b§l★ L'HIVER EST ÉTERNEL ★"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.BLUE_ICE)
            .iconColor("§b§l")
            .effectType(Talent.TalentEffectType.DEVASTATION)
            .values(new double[]{8000, 0.40, 0.50, 1.50, 25000, 5}) // duration_ms, damage_bonus%, frost_bonus%, shatter_bonus, cooldown_ms, freezes_needed
            .internalCooldownMs(25000)
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

        // 9.2 - GOLEM DE FER (Voie des Bêtes - LÉGENDAIRE)
        TALENTS.add(Talent.builder()
            .id("chasseur_beast_iron_golem")
            .name("Golem de Fer")
            .description("Invoque un golem devastateur avec frappe titanesque")
            .loreLines(new String[]{
                "§6§l★ TALENT LÉGENDAIRE ★",
                "§6§lVOIE DES BÊTES",
                "",
                "§7Invoque un §7Golem de Fer§7",
                "§7colosse qui écrase tout!",
                "",
                "§6CAPACITÉ - FRAPPE TITANESQUE:",
                "§7Toutes les §e5s§7, charge vers",
                "§7un ennemi et frappe le sol!",
                "",
                "§c1. §7Charge: écrase les ennemis",
                "§c2. §7Onde de choc §e8 blocs§7 devant",
                "§c3. §7§eStun 1.5s§7 sur tous les touchés!",
                "",
                "§6✦ SYNERGIE:",
                "§c• §7Cibles §cmarquées§7 = §ex2 dégâts",
                "§c• §73+ stacks abeille = §ex2 dégâts",
                "",
                "§6§l★ PUISSANCE ULTIME ★"
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
                "§5§l★ TALENT LÉGENDAIRE ★",
                "§8§lBRANCHE OMBRE",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7Transformez-vous en §5§lAVATAR D'OMBRE§7!",
                "",
                "§6TRANSFORMATION (15s):",
                "§8► §52 Clones§8 permanents",
                "§8► §5+1 Point/s§8 automatique",
                "§8► §c+40%§8 dégâts toutes sources",
                "§8► §7Semi-transparent§8 (ombre)",
                "",
                "§6SYNERGIE ULTIME:",
                "§7Les Exécutions des clones",
                "§7peuvent déclencher Tempête!",
                "",
                "§8► Cooldown: §c45s",
                "",
                "§5§l★ MAÎTRE DES OMBRES ★"
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
                "§2§l★ TALENT LÉGENDAIRE ★",
                "§2§lVOIE DU POISON",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7Devenez l'§2§lAVATAR DE LA PESTE§7!",
                "",
                "§6TRANSFORMATION (15s):",
                "§8► §2x3 VIRULENCE§8 appliquée!",
                "§8► Aura §e6 blocs§8 (+30 vir/s)",
                "§8► §aImmunité§8 poison/wither",
                "§8► Bonus de vitesse",
                "",
                "§c§lPESTE FINALE (fin):",
                "§8► Explosion §e8 blocs§8",
                "§8► §c500%§8 dégâts (cap 1000)",
                "§8► Tous = §d100% virulence§8!",
                "",
                "§8► Cooldown: §c60s",
                "",
                "§2§l★ MAÎTRE DE LA PESTE ★"
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
                "§b§l★ TALENT LÉGENDAIRE ★",
                "§b§lVOIE DU GIVRE",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7Déclenche §b§lZÉRO ABSOLU§7!",
                "",
                "§b§lZÉRO ABSOLU:",
                "§8► Vague de froid §e12 blocs§8",
                "§8► §b§lGÈLE INSTANTANÉMENT§8 tous!",
                "§8► Dégâts: §c500%§8 de base!",
                "",
                "§6EFFETS SUR LES GELÉS:",
                "§8► §b100%§8 givre instantané",
                "§8► §bImmobilisés§8 pendant §e2s",
                "§8► Subissent §c+50%§8 dégâts",
                "",
                "§6VAGUE VISUELLE:",
                "§8► Anneau de glace qui s'étend",
                "§8► Particules de glace épiques",
                "",
                "§8► Cooldown: §c45s",
                "",
                "§b§l★ ZÉRO ABSOLU ★"
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
