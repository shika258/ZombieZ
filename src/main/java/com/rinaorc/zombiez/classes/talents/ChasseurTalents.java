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
            .description("40% chance poison, 3+ = Nécrose")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vos attaques ont §a40%§7 de chance",
                "§7d'appliquer §2Venin§7 (stack x5).",
                "",
                "§6À 3+ STACKS - NÉCROSE:",
                "§8► Le poison devient §cNécrose",
                "§8► §c+20%§8 dégâts de poison",
                "§8► Effet visuel de corruption",
                "",
                "§2§lINFECTEZ VOS PROIES"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(3)
            .icon(Material.SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.VENOMOUS_STRIKE)
            .values(new double[]{0.40, 5, 3, 0.20}) // chance, max_stacks, necrosis_threshold, necrosis_bonus (nerfed 25→20%)
            .build());

        // 1.5 - FLECHES PERCANTES (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_piercing_arrows")
            .name("Flèches Perçantes")
            .description("Traverse 2 ennemis, +25%/traversé")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Vos projectiles §atraversent§7",
                "§7jusqu'à §e2 ennemis§7!",
                "",
                "§6MOMENTUM DE PERFORATION:",
                "§8► Chaque ennemi traversé:",
                "§8► §c+25%§8 dégâts au suivant!",
                "",
                "§6EXEMPLE:",
                "§71er ennemi: §c100%§7 dégâts",
                "§72ème ennemi: §c125%§7 dégâts",
                "",
                "§8Compteur affiché dans l'ActionBar",
                "",
                "§a§lPERCEZ VOS ENNEMIS"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_1)
            .slotIndex(4)
            .icon(Material.SPECTRAL_ARROW)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.PIERCING_ARROWS)
            .values(new double[]{2, 0.25}) // pierce_count, bonus_damage_per_pierce%
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
            .description("Poison 50%/3s + -10% armure ennemi")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison devient §2corrosif§7!",
                "",
                "§6DÉGÂTS POISON:",
                "§8► §c50%§8 de vos dégâts sur §e3s",
                "§8► Se cumule avec les stacks",
                "",
                "§6CORROSION:",
                "§8► Empoisonnés: §c-10%§8 armure",
                "§8► Synergie avec Nécrose!",
                "",
                "§2§lRONGEZ LEUR DÉFENSE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(3)
            .icon(Material.FERMENTED_SPIDER_EYE)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.CORROSIVE_VENOM)
            .values(new double[]{0.50, 3000, 0.10}) // damage%, duration_ms, armor_reduction
            .build());

        // 2.5 - CALIBRE (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_caliber")
            .name("Calibre")
            .description("Charge 1-5, à 5 = TIR LOURD!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Système de §eCalibre§7 (1-5).",
                "§7Chaque tir augmente le Calibre.",
                "",
                "§6CALIBRE CROISSANT:",
                "§8► §e+1 Calibre§8 par tir",
                "§8► §e+5%§8 dégâts par niveau",
                "",
                "§c§lÀ CALIBRE 5 - TIR LOURD:",
                "§8► §c+100%§8 dégâts!",
                "§8► §a+1§8 ennemi traversé",
                "§8► Son de railgun satisfaisant",
                "§8► Reset le Calibre à 0",
                "",
                "§8Calibre affiché: §e⬤⬤⬤⬤⬤",
                "",
                "§a§lMONTEZ EN PUISSANCE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_2)
            .slotIndex(4)
            .icon(Material.IRON_NUGGET)
            .iconColor("§e")
            .effectType(Talent.TalentEffectType.CALIBER)
            .values(new double[]{5, 0.05, 1.0, 1}) // max_caliber, damage_per_level%, heavy_shot_bonus, extra_pierce
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
            .description("Shift+Attaque = téléport derrière")
            .loreLines(new String[]{
                "§5§lVOIE DE L'OMBRE",
                "",
                "§6► PAS DE L'OMBRE",
                "§7§eShift + Attaque§7 = téléport derrière",
                "§7la cible + §d+2 Points d'Ombre",
                "",
                "§b⚡ §f5s§7 cooldown",
                "§8Mobilité d'assassin"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(2)
            .icon(Material.ENDER_EYE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_STEP)
            .values(new double[]{5000, 2}) // cooldown_ms, points_gained
            .build());

        // 3.4 - TOXINES MORTELLES (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_deadly_toxins")
            .name("Toxines Mortelles")
            .description("Poison CRIT +50%, Slow -30%, burst possible")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison devient §cléthal§7!",
                "",
                "§6CRITIQUES TOXIQUES:",
                "§8► §e25%§8 chance de crit poison",
                "§8► Crits: §c+50%§8 dégâts DoT",
                "",
                "§6PARALYSIE:",
                "§8► Empoisonnés: §9-30%§8 vitesse",
                "§8► Facilite les combo!",
                "",
                "§6BURST (5 stacks):",
                "§8► Crit sur 5 stacks = explosion!",
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

        // 3.5 - TRAJECTOIRE FATALE (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_fatal_trajectory")
            .name("Trajectoire Fatale")
            .description("Pierce 2+ = Ligne de Mort (+30% dégâts)")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Traverser §e2+ ennemis§7 crée",
                "§7une §c§lLIGNE DE MORT§7!",
                "",
                "§6LIGNE DE MORT:",
                "§8► Zone linéaire de §e12 blocs",
                "§8► Durée: §e3s",
                "§8► Effet visuel subtil",
                "",
                "§c§lENNEMIS DANS LA LIGNE:",
                "§8► Subissent §c+30%§8 de dégâts",
                "§8► De toutes sources!",
                "",
                "§6SYNERGIE:",
                "§7Parfait pour enchaîner les tirs",
                "§7dans la même trajectoire!",
                "",
                "§a§lTRACEZ LEUR FIN"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_3)
            .slotIndex(4)
            .icon(Material.END_ROD)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.FATAL_TRAJECTORY)
            .values(new double[]{2, 12.0, 3000, 0.30}) // pierce_threshold, line_length, duration_ms, damage_bonus%
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
            .description("Kill empoisonné = EXPLOSION toxique 5 blocs!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Tuer un ennemi §2empoisonné§7",
                "§7déclenche une §c§lEXPLOSION§7!",
                "",
                "§6EXPLOSION TOXIQUE:",
                "§8► Zone: §e5§8 blocs",
                "§8► Applique §22 stacks§8 poison",
                "§8► §c30%§8 dégâts de l'ennemi tué",
                "",
                "§6RÉACTION EN CHAÎNE:",
                "§7Si l'explosion tue, nouvelle",
                "§7explosion! (max 3 chaînes)",
                "",
                "§2§lPROPAGEZ LA PESTE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(3)
            .icon(Material.DRAGON_BREATH)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.PANDEMIC)
            .values(new double[]{5.0, 2, 0.30, 3}) // range, stacks_applied, damage%, max_chains
            .build());

        // 4.5 - SURCHAUFFE (Voie de la Perforation - Amélioré)
        TALENTS.add(Talent.builder()
            .id("chasseur_overheat")
            .name("Surchauffe")
            .description("+10%/tir (max +100%), à max = EXPLOSION!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Vos tirs §csurchauffent§7 votre arme!",
                "",
                "§6ACCUMULATION:",
                "§8► §c+10%§8 dégâts par tir",
                "§8► Maximum: §c+100%§8 (10 tirs)",
                "§8► Reset après §e2.5s§8 sans tirer",
                "",
                "§c§lÀ 100% - TIR EXPLOSIF:",
                "§8► Le prochain tir §6EXPLOSE§8!",
                "§8► Zone: §e4 blocs",
                "§8► §c+50%§8 dégâts bonus",
                "§8► Applique §9Lenteur§8 2s",
                "§8► Reset la surchauffe",
                "",
                "§6JAUGE: §8████████████",
                "",
                "§6§lCHAUFFEZ À BLANC!"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_4)
            .slotIndex(4)
            .icon(Material.FIRE_CHARGE)
            .iconColor("§6")
            .effectType(Talent.TalentEffectType.OVERHEAT)
            .values(new double[]{0.10, 1.0, 2500, 4.0, 0.50}) // stack%, max%, reset_ms, explosion_radius, explosion_bonus%
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

        // 5.4 - EPIDEMIE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_epidemic")
            .name("Épidémie")
            .description("Stacks infinis, 10+ = SUPER EXPLOSION auto!")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Le poison §2se cumule à l'infini§7!",
                "",
                "§6STACKS INFINIS:",
                "§8► Plus de limite de 5 stacks",
                "§8► Chaque stack = +10% dégâts DoT",
                "",
                "§c§lÀ 10 STACKS - SUPER EXPLOSION:",
                "§8► §cTous les stacks explosent!",
                "§8► §e150%§8 dégâts par stack",
                "§8► Zone: §e4§8 blocs",
                "§8► Reset les stacks à 0",
                "",
                "§6SATISFACTION GARANTIE!",
                "",
                "§2§lACCUMULEZ, EXPLOSEZ"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(3)
            .icon(Material.SLIME_BLOCK)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.EPIDEMIC)
            .values(new double[]{10, 1.5}) // threshold, damage_multiplier (nerfed 200→150%)
            .build());

        // 5.5 - PERFORATION ABSOLUE (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_absolute_perforation")
            .name("Perforation Absolue")
            .description("-20% armure/pierce (max -80%), expose!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Vos tirs §cdéchirent§7 les armures!",
                "",
                "§6RÉDUCTION D'ARMURE:",
                "§8► §c-20%§8 armure par ennemi traversé",
                "§8► Se cumule sur la même cible",
                "§8► Maximum: §c-80%§8 armure",
                "§8► Durée: §e5s§8 (refresh)",
                "",
                "§c§lÀ -80% - EXPOSÉ:",
                "§8► Cible devient §e§lEXPOSÉE",
                "§8► §cGlowing§8 (visible à travers murs)",
                "§8► Subit §c+35%§8 dégâts de vous",
                "",
                "§6SYNERGIE:",
                "§7Parfait avec Calibre et",
                "§7Trajectoire Fatale!",
                "",
                "§a§lDÉTRUISEZ LEUR DÉFENSE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_5)
            .slotIndex(4)
            .icon(Material.NETHERITE_PICKAXE)
            .iconColor("§c")
            .effectType(Talent.TalentEffectType.ABSOLUTE_PERFORATION)
            .values(new double[]{0.20, 0.80, 5000, 0.35}) // reduction_per_pierce%, max_reduction%, duration_ms, exposed_bonus%
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

        // 6.3 - DANSE MACABRE (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_danse_macabre")
            .name("Danse Macabre")
            .description("Kill marqué = 2s invis + reset Pas + vitesse")
            .loreLines(new String[]{
                "§8§lBRANCHE OMBRE",
                "",
                "§7Tuer une cible §cmarquée§7 déclenche",
                "§7la §5§lDanse Macabre§7!",
                "",
                "§6EFFETS DU KILL:",
                "§8► §7Invisibilité§8 §e2s",
                "§8► §bReset§8 cooldown Pas d'Ombre",
                "§8► §aVitesse II§8 pendant §e3s",
                "§8► §5+1 Point§8 d'Ombre",
                "",
                "§6ENCHAÎNEMENT:",
                "§7Parfait pour tuer plusieurs",
                "§7cibles à la suite!",
                "",
                "§5§lL'OMBRE NE S'ARRÊTE JAMAIS"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(2)
            .icon(Material.PHANTOM_MEMBRANE)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.DANSE_MACABRE)
            .values(new double[]{2000, 3000, 1}) // invis_duration_ms, speed_duration_ms, points_gained
            .build());

        // 6.4 - SYNERGIE TOXIQUE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_toxic_synergy")
            .name("Synergie Toxique")
            .description("+5% AS par stack proche, heal sur explosion")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison vous §arenforce§7!",
                "",
                "§6FRÉNÉSIE TOXIQUE:",
                "§8► §a+5%§8 Attack Speed par stack",
                "§8► Compte tous ennemis à §e8§8 blocs",
                "§8► Maximum: §a+30%§8 AS",
                "",
                "§6DRAIN VITAL:",
                "§8► Explosions de poison: §c+8%§8 heal",
                "§8► Applique aux explosions Épidémie",
                "",
                "§6PLUS ILS SOUFFRENT...",
                "§7...plus vous êtes fort!",
                "",
                "§2§lLE POISON VOUS NOURRIT"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(3)
            .icon(Material.BREWING_STAND)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.TOXIC_SYNERGY)
            .values(new double[]{0.05, 8.0, 0.30, 0.08}) // as_per_stack, range, max_as_bonus (nerfed 40→30%), heal_on_explosion
            .build());

        // 6.5 - MOMENTUM DE CHASSEUR (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_hunter_momentum")
            .name("Momentum de Chasseur")
            .description("Kill surchauffé = vitesse, 3 kills = FRÉNÉSIE!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Les kills pendant §cSurchauffe§7",
                "§7vous propulsent vers l'avant!",
                "",
                "§6KILL SURCHAUFFÉ:",
                "§8► §a+35%§8 vitesse de déplacement",
                "§8► Durée: §e2s§8 (cumule)",
                "§8► Chaque kill étend de §e+1s",
                "",
                "§c§l3 KILLS CONSÉCUTIFS - FRÉNÉSIE:",
                "§8► §c+60%§8 Attack Speed!",
                "§8► §a+50%§8 vitesse déplacement",
                "§8► Durée: §e4s",
                "§8► Tirs §6enflammés§8!",
                "",
                "§6ENCHAÎNEMENT PARFAIT:",
                "§7Tuez vite pour maintenir",
                "§7le momentum!",
                "",
                "§a§lNE VOUS ARRÊTEZ JAMAIS"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_6)
            .slotIndex(4)
            .icon(Material.SUGAR)
            .iconColor("§a")
            .effectType(Talent.TalentEffectType.HUNTER_MOMENTUM)
            .values(new double[]{0.35, 2000, 1000, 3, 0.60, 0.50, 4000}) // speed%, base_duration_ms, extension_ms, frenzy_kills, frenzy_as%, frenzy_speed%, frenzy_duration_ms
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

        // 7.3 - CLONE D'OMBRE (Branche Ombre)
        TALENTS.add(Talent.builder()
            .id("chasseur_shadow_clone")
            .name("Clone d'Ombre")
            .description("5 Points = clone 10s (40% dégâts)")
            .loreLines(new String[]{
                "§8§lBRANCHE OMBRE",
                "",
                "§7Quand vous atteignez §55 Points§7,",
                "§7invoquez automatiquement un",
                "§5§lClone d'Ombre§7!",
                "",
                "§6CLONE D'OMBRE:",
                "§8► Durée: §e10s",
                "§8► Dégâts: §c40%§8 de vos dégâts",
                "§8► §7Attaque votre cible",
                "§8► §5Invulnérable§8 (ombre pure)",
                "",
                "§6SYNERGIE:",
                "§7Le clone peut déclencher vos",
                "§7effets de talents!",
                "",
                "§5§lVOTRE OMBRE COMBAT"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(2)
            .icon(Material.ARMOR_STAND)
            .iconColor("§5")
            .effectType(Talent.TalentEffectType.SHADOW_CLONE)
            .values(new double[]{5, 10000, 0.40}) // points_trigger, duration_ms, damage_percent (40%)
            .build());

        // 7.4 - PESTE NOIRE (Voie du Poison)
        TALENTS.add(Talent.builder()
            .id("chasseur_black_plague")
            .name("Peste Noire")
            .description("Anti-heal, 10% lifesteal DoT, morts = nuages")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Votre poison devient §0§lmortel§r§7!",
                "",
                "§6ANTI-SOIN:",
                "§8► Empoisonnés: §c-75%§8 soins reçus",
                "§8► Bloque régénération naturelle",
                "",
                "§6DRAIN DE VIE:",
                "§8► Dégâts poison: §a+10%§8 lifesteal",
                "§8► Vous soigne passivement!",
                "",
                "§6NUAGE MORTEL:",
                "§8► Morts par poison = nuage 3s",
                "§8► Nuage: §24§8 blocs, 15% dégâts/s",
                "",
                "§0§lLA MORT INCARNÉE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(3)
            .icon(Material.WITHER_ROSE)
            .iconColor("§0")
            .effectType(Talent.TalentEffectType.BLACK_PLAGUE)
            .values(new double[]{0.75, 0.10, 4.0, 0.15, 3000}) // heal_reduction%, lifesteal%, cloud_radius, cloud_dps%, cloud_duration_ms
            .build());

        // 7.5 - PERFORATION EN CHAÎNE (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_chain_perforation")
            .name("Perforation en Chaîne")
            .description("Après dernier pierce, rebondit 3x!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Après avoir traversé le dernier",
                "§7ennemi, vos projectiles §brebondissent§7!",
                "",
                "§6REBONDS EN CHAÎNE:",
                "§8► Jusqu'à §e3§8 rebonds",
                "§8► Vers l'ennemi le plus proche",
                "§8► Portée: §e10 blocs",
                "",
                "§6DÉGÂTS PAR REBOND:",
                "§8► 1er rebond: §c75%",
                "§8► 2ème rebond: §c50%",
                "§8► 3ème rebond: §c25%",
                "",
                "§6BONUS CALIBRE:",
                "§8► §e30%§8 chance de +1 Calibre",
                "§8► Par rebond réussi!",
                "",
                "§6SYNERGIE:",
                "§7Plus vous percez, plus vous",
                "§7rebondissez de fois!",
                "",
                "§a§lILS NE PEUVENT PAS FUIR"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_7)
            .slotIndex(4)
            .icon(Material.ECHO_SHARD)
            .iconColor("§b")
            .effectType(Talent.TalentEffectType.CHAIN_PERFORATION)
            .values(new double[]{3, 10.0, 0.75, 0.50, 0.25, 0.30}) // max_bounces, range, dmg_bounce1%, dmg_bounce2%, dmg_bounce3%, caliber_chance
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
            .description("Aura poison 5 blocs, propagation auto, combo boost")
            .loreLines(new String[]{
                "§2§lVOIE DU POISON",
                "",
                "§7Vous §2irradiez§7 le poison!",
                "",
                "§6AURA TOXIQUE:",
                "§8► §e5 blocs§8 autour de vous",
                "§8► §21 stack§8/seconde aux ennemis",
                "§8► Passive et permanente",
                "",
                "§6PROPAGATION AUTO:",
                "§8► Ennemis 3+ stacks infectent",
                "§8► les autres à §e3 blocs§8",
                "",
                "§6BOOST COMBO:",
                "§8► À 50+ stacks totaux proches:",
                "§8► §c+25%§8 tous vos dégâts!",
                "",
                "§2§lVOUS ÊTES LA PESTE"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(3)
            .icon(Material.MYCELIUM)
            .iconColor("§2")
            .effectType(Talent.TalentEffectType.BLIGHT)
            .values(new double[]{5.0, 1000, 3.0, 50, 0.25}) // aura_range, tick_ms, spread_range, combo_threshold, combo_bonus
            .build());

        // 8.5 - DÉVASTATION (Voie de la Perforation)
        TALENTS.add(Talent.builder()
            .id("chasseur_devastation")
            .name("Dévastation")
            .description("Mode 8s: pierce infini, +60% dégâts, slow!")
            .loreLines(new String[]{
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7Entrez en mode §c§lDÉVASTATION§7!",
                "",
                "§6MODE DÉVASTATION (8s):",
                "§8► §aPierce INFINI§8!",
                "§8► §c+60%§8 dégâts",
                "§8► Tirs créent traînée visuelle",
                "§8► Ennemis touchés: §9-40%§8 vitesse",
                "",
                "§6BONUS CALIBRE:",
                "§8► Calibre monte §e2x§8 plus vite",
                "§8► Tirs Lourds = §c+150%§8 dégâts!",
                "",
                "§6EFFET VISUEL:",
                "§8► Vous brillez §avert§8",
                "§8► Projectiles laissent des traînées",
                "",
                "§8► Cooldown: §c30s",
                "",
                "§a§l★ DÉVASTATION TOTALE ★"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_8)
            .slotIndex(4)
            .icon(Material.NETHERITE_INGOT)
            .iconColor("§a§l")
            .effectType(Talent.TalentEffectType.DEVASTATION)
            .values(new double[]{8000, 0.60, 0.40, 1.50, 30000}) // duration_ms, damage_bonus%, slow%, heavy_shot_bonus, cooldown_ms
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
            .description("Ultime 20s: Max stacks instant, x2 explosions, immunité!")
            .loreLines(new String[]{
                "§2§l★ TALENT LÉGENDAIRE ★",
                "§2§lVOIE DU POISON",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7Devenez l'§2§lAVATAR DE LA PESTE§7!",
                "",
                "§6TRANSFORMATION (20s):",
                "§8► Attaques = §2max stacks§8 instant!",
                "§8► Explosions poison: §cx2§8 rayon",
                "§8► §aImmunité§8 poison/wither",
                "§8► Aura 8 blocs (3 stacks/s)",
                "",
                "§6PESTE FINALE:",
                "§8► À la fin: §cMÉGA EXPLOSION",
                "§8► 10 blocs, 500% dégâts poison",
                "§8► Tous les ennemis = max stacks",
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
            .values(new double[]{20000, 8.0, 3, 10.0, 5.0, 60000}) // duration_ms, aura_range, stacks_per_sec, final_radius, final_damage_mult, cooldown_ms
            .internalCooldownMs(60000)
            .build());

        // 9.5 - JUGEMENT (Voie de la Perforation - LÉGENDAIRE)
        TALENTS.add(Talent.builder()
            .id("chasseur_judgment")
            .name("Jugement")
            .description("Rayon 50 blocs, 1000% dégâts, -100% armure!")
            .loreLines(new String[]{
                "§a§l★ TALENT LÉGENDAIRE ★",
                "§a§lVOIE DE LA PERFORATION",
                "",
                "§7Activation: §e2x SNEAK§7 rapidement",
                "§7puis restez §eimmobile 1.5s§7...",
                "",
                "§c§lTIR DU JUGEMENT:",
                "§8► Tire un §cRAYON§8 de §e50 blocs",
                "§8► Traverse §aTOUS§8 les ennemis",
                "§8► Dégâts: §c§l1000%§8 de base!",
                "",
                "§6EFFETS SUR LES TOUCHÉS:",
                "§8► §c-100%§8 armure pendant §e5s",
                "§8► §6Enflammés§8 pendant §e3s",
                "§8► §9Lenteur III§8 pendant §e3s",
                "",
                "§6TRAÎNÉE DE FEU:",
                "§8► Le rayon laisse une traînée",
                "§8► Zone de feu §e3s§8, brûle!",
                "",
                "§6EFFET VISUEL:",
                "§8► Charge: particules convergent",
                "§8► Tir: éclair §avert§8 massif",
                "§8► Son: railgun épique",
                "",
                "§8► Cooldown: §c45s",
                "",
                "§a§l★ JUGEMENT FINAL ★"
            })
            .classType(ClassType.CHASSEUR)
            .tier(TalentTier.TIER_9)
            .slotIndex(4)
            .icon(Material.END_CRYSTAL)
            .iconColor("§a§l")
            .effectType(Talent.TalentEffectType.JUDGMENT)
            .values(new double[]{1500, 50.0, 10.0, 1.0, 5000, 3000, 45000}) // charge_ms, range, damage_mult, armor_reduction%, armor_duration_ms, fire_duration_ms, cooldown_ms
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
