package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.ClassTalent.TalentBranch;
import com.rinaorc.zombiez.classes.talents.ClassTalent.TalentEffect;
import lombok.Getter;
import org.bukkit.Material;

import java.util.*;

/**
 * Définit tous les arbres de talents pour chaque classe
 * Chaque classe a 3 branches avec 5 tiers de talents
 */
@Getter
public class ClassTalentTree {

    private final Map<ClassType, List<ClassTalent>> talentsByClass;
    private final Map<String, ClassTalent> talentsById;

    public ClassTalentTree() {
        this.talentsByClass = new EnumMap<>(ClassType.class);
        this.talentsById = new LinkedHashMap<>();

        registerAllTalents();
    }

    private void registerAllTalents() {
        registerCommandoTalents();
        registerScoutTalents();
        registerMedicTalents();
        registerEngineerTalents();
        registerBerserkerTalents();
        registerSniperTalents();
    }

    private void register(ClassTalent talent) {
        talentsById.put(talent.getId(), talent);
        talentsByClass.computeIfAbsent(talent.getClassType(), k -> new ArrayList<>()).add(talent);
    }

    // ==================== COMMANDO ====================
    private void registerCommandoTalents() {
        ClassType c = ClassType.COMMANDO;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("cmd_dmg_1", "Tir Nourri I", "§7+{value}% dégâts avec armes à feu",
            c, TalentBranch.OFFENSE, 1, 3, Material.IRON_INGOT, null, 1,
            TalentEffect.DAMAGE_PERCENT, 5, 3));

        register(new ClassTalent("cmd_dmg_2", "Tir Nourri II", "§7+{value}% dégâts avec armes à feu",
            c, TalentBranch.OFFENSE, 2, 3, Material.GOLD_INGOT, "cmd_dmg_1", 1,
            TalentEffect.DAMAGE_PERCENT, 8, 4));

        register(new ClassTalent("cmd_crit", "Tir de Précision", "§7+{value}% chance de critique",
            c, TalentBranch.OFFENSE, 2, 3, Material.TARGET, "cmd_dmg_1", 1,
            TalentEffect.CRIT_CHANCE, 5, 3));

        register(new ClassTalent("cmd_suppression", "Tir de Suppression", "§7Les ennemis touchés sont ralentis de {value}%",
            c, TalentBranch.OFFENSE, 3, 3, Material.CHAIN, "cmd_dmg_2", 2,
            TalentEffect.SUPPRESSION_RANGE, 10, 5));

        register(new ClassTalent("cmd_explosive", "Expert en Explosifs", "§7+{value}% dégâts explosifs",
            c, TalentBranch.OFFENSE, 4, 3, Material.TNT, "cmd_suppression", 2,
            TalentEffect.EXPLOSIVE_DAMAGE, 15, 10));

        register(new ClassTalent("cmd_barrage", "Barrage Final", "§c[ULTIME] §7Débloque: Pluie de Feu",
            c, TalentBranch.OFFENSE, 5, 1, Material.FIRE_CHARGE, "cmd_explosive", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("cmd_armor_1", "Blindage I", "§7+{value}% réduction de dégâts",
            c, TalentBranch.DEFENSE, 1, 3, Material.IRON_CHESTPLATE, null, 1,
            TalentEffect.DAMAGE_REDUCTION, 5, 3));

        register(new ClassTalent("cmd_armor_2", "Blindage II", "§7+{value}% réduction de dégâts",
            c, TalentBranch.DEFENSE, 2, 3, Material.DIAMOND_CHESTPLATE, "cmd_armor_1", 1,
            TalentEffect.DAMAGE_REDUCTION, 8, 4));

        register(new ClassTalent("cmd_hp", "Endurance de Combat", "§7+{value}% HP maximum",
            c, TalentBranch.DEFENSE, 2, 3, Material.GOLDEN_APPLE, "cmd_armor_1", 1,
            TalentEffect.HEALTH_PERCENT, 10, 5));

        register(new ClassTalent("cmd_regen", "Seconde Ligne", "§7+{value}% régénération en combat",
            c, TalentBranch.DEFENSE, 3, 3, Material.GLISTERING_MELON_SLICE, "cmd_hp", 2,
            TalentEffect.REGEN_PERCENT, 10, 5));

        register(new ClassTalent("cmd_laststand", "Dernier Rempart", "§7Survie à un coup fatal (1x/{value_int}min)",
            c, TalentBranch.DEFENSE, 4, 3, Material.TOTEM_OF_UNDYING, "cmd_regen", 2,
            TalentEffect.HEALTH_FLAT, 10, -2)); // 10min, 8min, 6min cooldown

        register(new ClassTalent("cmd_fortress", "Forteresse Mobile", "§c[ULTIME] §7Débloque: Mode Siège",
            c, TalentBranch.DEFENSE, 5, 1, Material.NETHERITE_CHESTPLATE, "cmd_laststand", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("cmd_ammo", "Munitions Lourdes", "§7+{value}% dégâts des armes lourdes",
            c, TalentBranch.SPECIALTY, 1, 3, Material.IRON_NUGGET, null, 1,
            TalentEffect.DAMAGE_PERCENT, 8, 4));

        register(new ClassTalent("cmd_lmg", "Maître du LMG", "§7Débloque: §eLMG Ravageur",
            c, TalentBranch.SPECIALTY, 2, 1, Material.CROSSBOW, "cmd_ammo", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("cmd_grenade", "Grenadier", "§7+{value} grenades max, +25% dégâts",
            c, TalentBranch.SPECIALTY, 3, 3, Material.FIRE_CHARGE, "cmd_lmg", 2,
            TalentEffect.EXPLOSIVE_DAMAGE, 1, 1));

        register(new ClassTalent("cmd_minigun", "Maître du Minigun", "§7Débloque: §6Minigun Apocalypse",
            c, TalentBranch.SPECIALTY, 4, 1, Material.BLAZE_ROD, "cmd_grenade", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("cmd_rocket", "Arsenal Nucléaire", "§7Débloque: §cLance-Roquettes",
            c, TalentBranch.SPECIALTY, 5, 1, Material.FIREWORK_ROCKET, "cmd_minigun", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== ÉCLAIREUR (SCOUT) ====================
    private void registerScoutTalents() {
        ClassType c = ClassType.SCOUT;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("sct_crit_1", "Frappe Précise I", "§7+{value}% chance de critique",
            c, TalentBranch.OFFENSE, 1, 3, Material.FLINT, null, 1,
            TalentEffect.CRIT_CHANCE, 5, 3));

        register(new ClassTalent("sct_crit_2", "Frappe Précise II", "§7+{value}% dégâts critiques",
            c, TalentBranch.OFFENSE, 2, 3, Material.DIAMOND, "sct_crit_1", 1,
            TalentEffect.CRIT_DAMAGE, 10, 5));

        register(new ClassTalent("sct_backstab", "Coup dans le Dos", "§7+{value}% dégâts par derrière",
            c, TalentBranch.OFFENSE, 2, 3, Material.IRON_SWORD, "sct_crit_1", 1,
            TalentEffect.DAMAGE_PERCENT, 15, 10));

        register(new ClassTalent("sct_stealth_dmg", "Assassin Silencieux", "§7+{value}% dégâts depuis furtivité",
            c, TalentBranch.OFFENSE, 3, 3, Material.WITHER_ROSE, "sct_backstab", 2,
            TalentEffect.STEALTH_DAMAGE, 25, 15));

        register(new ClassTalent("sct_execute", "Exécution", "§7+{value}% dégâts aux ennemis <25% HP",
            c, TalentBranch.OFFENSE, 4, 3, Material.WITHER_SKELETON_SKULL, "sct_stealth_dmg", 2,
            TalentEffect.EXECUTE_DAMAGE, 20, 10));

        register(new ClassTalent("sct_shadow", "Frappe de l'Ombre", "§c[ULTIME] §7Débloque: Danse des Lames",
            c, TalentBranch.OFFENSE, 5, 1, Material.ENDER_EYE, "sct_execute", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("sct_dodge_1", "Agilité I", "§7+{value}% chance d'esquive",
            c, TalentBranch.DEFENSE, 1, 3, Material.FEATHER, null, 1,
            TalentEffect.DODGE_CHANCE, 5, 3));

        register(new ClassTalent("sct_dodge_2", "Agilité II", "§7+{value}% chance d'esquive",
            c, TalentBranch.DEFENSE, 2, 3, Material.PHANTOM_MEMBRANE, "sct_dodge_1", 1,
            TalentEffect.DODGE_CHANCE, 8, 4));

        register(new ClassTalent("sct_speed", "Sprint Tactique", "§7+{value}% vitesse de déplacement",
            c, TalentBranch.DEFENSE, 2, 3, Material.SUGAR, "sct_dodge_1", 1,
            TalentEffect.MOVEMENT_SPEED, 10, 5));

        register(new ClassTalent("sct_evasion", "Évasion Totale", "§7{value}% chance d'éviter les AoE",
            c, TalentBranch.DEFENSE, 3, 3, Material.ENDER_PEARL, "sct_speed", 2,
            TalentEffect.DODGE_CHANCE, 15, 10));

        register(new ClassTalent("sct_vanish", "Disparition", "§7Invisibilité de {value}s après un kill",
            c, TalentBranch.DEFENSE, 4, 3, Material.POTION, "sct_evasion", 2,
            TalentEffect.STEALTH_DURATION, 2, 1));

        register(new ClassTalent("sct_phantom", "Fantôme", "§c[ULTIME] §7Débloque: Voile d'Ombre",
            c, TalentBranch.DEFENSE, 5, 1, Material.SPECTRAL_ARROW, "sct_vanish", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("sct_trap_1", "Poseur de Pièges", "§7+{value} pièges max",
            c, TalentBranch.SPECIALTY, 1, 3, Material.TRIPWIRE_HOOK, null, 1,
            TalentEffect.DAMAGE_FLAT, 1, 1));

        register(new ClassTalent("sct_crossbow", "Maître de l'Arbalète", "§7Débloque: §eArbalète Silencieuse",
            c, TalentBranch.SPECIALTY, 2, 1, Material.CROSSBOW, "sct_trap_1", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("sct_trap_dmg", "Pièges Mortels", "§7+{value}% dégâts des pièges",
            c, TalentBranch.SPECIALTY, 3, 3, Material.POINTED_DRIPSTONE, "sct_crossbow", 2,
            TalentEffect.DAMAGE_PERCENT, 20, 15));

        register(new ClassTalent("sct_daggers", "Maître des Dagues", "§7Débloque: §6Dagues Jumelles",
            c, TalentBranch.SPECIALTY, 4, 1, Material.IRON_SWORD, "sct_trap_dmg", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("sct_bow", "Arc du Prédateur", "§7Débloque: §cArc Composite",
            c, TalentBranch.SPECIALTY, 5, 1, Material.BOW, "sct_daggers", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== MÉDIC ====================
    private void registerMedicTalents() {
        ClassType c = ClassType.MEDIC;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("med_poison_1", "Doses Toxiques I", "§7+{value}% dégâts de poison",
            c, TalentBranch.OFFENSE, 1, 3, Material.SPIDER_EYE, null, 1,
            TalentEffect.DAMAGE_PERCENT, 10, 5));

        register(new ClassTalent("med_poison_2", "Doses Toxiques II", "§7+{value}% durée du poison",
            c, TalentBranch.OFFENSE, 2, 3, Material.FERMENTED_SPIDER_EYE, "med_poison_1", 1,
            TalentEffect.SKILL_DURATION, 20, 10));

        register(new ClassTalent("med_weakness", "Affaiblissement", "§7Les ennemis empoisonnés font -{value}% dégâts",
            c, TalentBranch.OFFENSE, 2, 3, Material.POISONOUS_POTATO, "med_poison_1", 1,
            TalentEffect.DAMAGE_REDUCTION, 10, 5));

        register(new ClassTalent("med_plague", "Propagation", "§7Le poison se propage à {value} ennemis proches",
            c, TalentBranch.OFFENSE, 3, 3, Material.ROTTEN_FLESH, "med_weakness", 2,
            TalentEffect.DAMAGE_FLAT, 1, 1));

        register(new ClassTalent("med_lethal", "Injection Létale", "§7+{value}% dégâts aux ennemis empoisonnés",
            c, TalentBranch.OFFENSE, 4, 3, Material.WITHER_ROSE, "med_plague", 2,
            TalentEffect.DAMAGE_PERCENT, 25, 15));

        register(new ClassTalent("med_pandemic", "Pandémie", "§c[ULTIME] §7Débloque: Nuage Toxique",
            c, TalentBranch.OFFENSE, 5, 1, Material.DRAGON_BREATH, "med_lethal", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("med_heal_1", "Soins Améliorés I", "§7+{value}% puissance de soin",
            c, TalentBranch.DEFENSE, 1, 3, Material.GLISTERING_MELON_SLICE, null, 1,
            TalentEffect.HEAL_POWER, 10, 5));

        register(new ClassTalent("med_heal_2", "Soins Améliorés II", "§7+{value}% puissance de soin",
            c, TalentBranch.DEFENSE, 2, 3, Material.GOLDEN_CARROT, "med_heal_1", 1,
            TalentEffect.HEAL_POWER, 15, 8));

        register(new ClassTalent("med_regen", "Régénération Passive", "§7+{value}% régénération naturelle",
            c, TalentBranch.DEFENSE, 2, 3, Material.GOLDEN_APPLE, "med_heal_1", 1,
            TalentEffect.REGEN_PERCENT, 20, 10));

        register(new ClassTalent("med_aura", "Aura de Guérison", "§7Soigne les alliés proches de {value} HP/s",
            c, TalentBranch.DEFENSE, 3, 3, Material.ENCHANTED_GOLDEN_APPLE, "med_regen", 2,
            TalentEffect.HEAL_RANGE, 1, 0.5));

        register(new ClassTalent("med_revive", "Résurrection Rapide", "§7Réanime en {value}s (au lieu de 5s)",
            c, TalentBranch.DEFENSE, 4, 3, Material.TOTEM_OF_UNDYING, "med_aura", 2,
            TalentEffect.COOLDOWN_REDUCTION, 5, -1)); // 5s, 4s, 3s

        register(new ClassTalent("med_immortal", "Ange Gardien", "§c[ULTIME] §7Débloque: Résurrection de Masse",
            c, TalentBranch.DEFENSE, 5, 1, Material.NETHER_STAR, "med_revive", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("med_stim", "Stimulants", "§7+{value}% efficacité des consommables",
            c, TalentBranch.SPECIALTY, 1, 3, Material.POTION, null, 1,
            TalentEffect.HEAL_POWER, 15, 10));

        register(new ClassTalent("med_syringe", "Pistolet Seringue", "§7Débloque: §ePistolet Seringue",
            c, TalentBranch.SPECIALTY, 2, 1, Material.BLAZE_ROD, "med_stim", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("med_overdose", "Surdosage", "§7Les stims donnent +{value}% dégâts temporaire",
            c, TalentBranch.SPECIALTY, 3, 3, Material.BLAZE_POWDER, "med_syringe", 2,
            TalentEffect.DAMAGE_PERCENT, 10, 5));

        register(new ClassTalent("med_staff", "Bâton Médical", "§7Débloque: §6Bâton de Vie",
            c, TalentBranch.SPECIALTY, 4, 1, Material.BLAZE_ROD, "med_overdose", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("med_defibrillator", "Défibrillateur", "§7Débloque: §cDéfibrillateur",
            c, TalentBranch.SPECIALTY, 5, 1, Material.LIGHTNING_ROD, "med_staff", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== INGÉNIEUR ====================
    private void registerEngineerTalents() {
        ClassType c = ClassType.ENGINEER;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("eng_turret_dmg_1", "Calibrage I", "§7+{value}% dégâts des tourelles",
            c, TalentBranch.OFFENSE, 1, 3, Material.REDSTONE, null, 1,
            TalentEffect.TURRET_DAMAGE, 10, 5));

        register(new ClassTalent("eng_turret_dmg_2", "Calibrage II", "§7+{value}% dégâts des tourelles",
            c, TalentBranch.OFFENSE, 2, 3, Material.REDSTONE_BLOCK, "eng_turret_dmg_1", 1,
            TalentEffect.TURRET_DAMAGE, 15, 8));

        register(new ClassTalent("eng_turret_speed", "Cadence Rapide", "§7+{value}% vitesse de tir tourelles",
            c, TalentBranch.OFFENSE, 2, 3, Material.COMPARATOR, "eng_turret_dmg_1", 1,
            TalentEffect.ATTACK_SPEED, 15, 10));

        register(new ClassTalent("eng_overcharge", "Surcharge", "§7Les tourelles peuvent surcharger (+{value}% dégâts)",
            c, TalentBranch.OFFENSE, 3, 3, Material.GLOWSTONE_DUST, "eng_turret_speed", 2,
            TalentEffect.TURRET_DAMAGE, 25, 15));

        register(new ClassTalent("eng_emp", "Impulsion EMP", "§7Les tourelles étourdissent {value}s",
            c, TalentBranch.OFFENSE, 4, 3, Material.END_CRYSTAL, "eng_overcharge", 2,
            TalentEffect.SKILL_DURATION, 1, 0.5));

        register(new ClassTalent("eng_artillery", "Frappe Orbitale", "§c[ULTIME] §7Débloque: Bombardement",
            c, TalentBranch.OFFENSE, 5, 1, Material.TNT_MINECART, "eng_emp", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("eng_shield_1", "Bouclier Énergétique I", "§7+{value}% durée du bouclier",
            c, TalentBranch.DEFENSE, 1, 3, Material.ENDER_PEARL, null, 1,
            TalentEffect.SKILL_DURATION, 20, 10));

        register(new ClassTalent("eng_shield_2", "Bouclier Énergétique II", "§7Le bouclier absorbe +{value}% dégâts",
            c, TalentBranch.DEFENSE, 2, 3, Material.ENDER_EYE, "eng_shield_1", 1,
            TalentEffect.DAMAGE_REDUCTION, 15, 10));

        register(new ClassTalent("eng_repair", "Auto-Réparation", "§7+{value}% régénération près des tourelles",
            c, TalentBranch.DEFENSE, 2, 3, Material.IRON_INGOT, "eng_shield_1", 1,
            TalentEffect.REGEN_PERCENT, 20, 10));

        register(new ClassTalent("eng_drone", "Drone Réparateur", "§7Un drone vous soigne de {value} HP/s",
            c, TalentBranch.DEFENSE, 3, 3, Material.BAT_SPAWN_EGG, "eng_repair", 2,
            TalentEffect.REGEN_PERCENT, 2, 1));

        register(new ClassTalent("eng_fortress", "Fortification", "§7+{value}% réduction dégâts en zone tourelle",
            c, TalentBranch.DEFENSE, 4, 3, Material.OBSIDIAN, "eng_drone", 2,
            TalentEffect.DAMAGE_REDUCTION, 20, 10));

        register(new ClassTalent("eng_bunker", "Bunker Mobile", "§c[ULTIME] §7Débloque: Dôme Protecteur",
            c, TalentBranch.DEFENSE, 5, 1, Material.BEACON, "eng_fortress", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("eng_scrap", "Récupérateur", "§7+{value}% loot de composants",
            c, TalentBranch.SPECIALTY, 1, 3, Material.IRON_NUGGET, null, 1,
            TalentEffect.LOOT_CHANCE, 15, 10));

        register(new ClassTalent("eng_tesla", "Pistolet Tesla", "§7Débloque: §ePistolet Tesla",
            c, TalentBranch.SPECIALTY, 2, 1, Material.LIGHTNING_ROD, "eng_scrap", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("eng_mines", "Expert en Mines", "§7+{value} mines posables, +50% dégâts",
            c, TalentBranch.SPECIALTY, 3, 3, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "eng_tesla", 2,
            TalentEffect.DAMAGE_FLAT, 2, 1));

        register(new ClassTalent("eng_flamethrower", "Lance-Flammes", "§7Débloque: §6Lance-Flammes",
            c, TalentBranch.SPECIALTY, 4, 1, Material.FIRE_CHARGE, "eng_mines", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("eng_railgun", "Canon à Rails", "§7Débloque: §cRailgun",
            c, TalentBranch.SPECIALTY, 5, 1, Material.SPYGLASS, "eng_flamethrower", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== BERSERKER ====================
    private void registerBerserkerTalents() {
        ClassType c = ClassType.BERSERKER;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("ber_melee_1", "Force Brute I", "§7+{value}% dégâts mêlée",
            c, TalentBranch.OFFENSE, 1, 3, Material.IRON_AXE, null, 1,
            TalentEffect.DAMAGE_PERCENT, 8, 4));

        register(new ClassTalent("ber_melee_2", "Force Brute II", "§7+{value}% dégâts mêlée",
            c, TalentBranch.OFFENSE, 2, 3, Material.DIAMOND_AXE, "ber_melee_1", 1,
            TalentEffect.DAMAGE_PERCENT, 12, 6));

        register(new ClassTalent("ber_rage_gen", "Soif de Sang", "§7+{value}% génération de rage",
            c, TalentBranch.OFFENSE, 2, 3, Material.REDSTONE, "ber_melee_1", 1,
            TalentEffect.RAGE_GENERATION, 15, 10));

        register(new ClassTalent("ber_rage_dmg", "Rage Dévastatrice", "§7+{value}% dégâts en mode rage",
            c, TalentBranch.OFFENSE, 3, 3, Material.BLAZE_POWDER, "ber_rage_gen", 2,
            TalentEffect.RAGE_DAMAGE, 20, 10));

        register(new ClassTalent("ber_cleave", "Frappe Circulaire", "§7Les attaques touchent {value} ennemis proches",
            c, TalentBranch.OFFENSE, 4, 3, Material.GOLDEN_AXE, "ber_rage_dmg", 2,
            TalentEffect.DAMAGE_FLAT, 2, 1));

        register(new ClassTalent("ber_rampage", "Carnage", "§c[ULTIME] §7Débloque: Tourbillon Sanglant",
            c, TalentBranch.OFFENSE, 5, 1, Material.NETHERITE_AXE, "ber_cleave", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("ber_lifesteal_1", "Vampirisme I", "§7+{value}% vol de vie",
            c, TalentBranch.DEFENSE, 1, 3, Material.GHAST_TEAR, null, 1,
            TalentEffect.LIFESTEAL, 5, 3));

        register(new ClassTalent("ber_lifesteal_2", "Vampirisme II", "§7+{value}% vol de vie",
            c, TalentBranch.DEFENSE, 2, 3, Material.NETHER_WART, "ber_lifesteal_1", 1,
            TalentEffect.LIFESTEAL, 8, 4));

        register(new ClassTalent("ber_hp", "Constitution Monstrueuse", "§7+{value}% HP maximum",
            c, TalentBranch.DEFENSE, 2, 3, Material.BEEF, "ber_lifesteal_1", 1,
            TalentEffect.HEALTH_PERCENT, 15, 10));

        register(new ClassTalent("ber_bloodlust", "Frénésie Sanglante", "§7Chaque kill donne +{value}% vitesse",
            c, TalentBranch.DEFENSE, 3, 3, Material.RABBIT_FOOT, "ber_hp", 2,
            TalentEffect.MOVEMENT_SPEED, 5, 3));

        register(new ClassTalent("ber_unstoppable", "Inarrêtable", "§7-{value}% effets de contrôle",
            c, TalentBranch.DEFENSE, 4, 3, Material.OBSIDIAN, "ber_bloodlust", 2,
            TalentEffect.DAMAGE_REDUCTION, 25, 15));

        register(new ClassTalent("ber_undying", "Immortel", "§c[ULTIME] §7Débloque: Rage Éternelle",
            c, TalentBranch.DEFENSE, 5, 1, Material.TOTEM_OF_UNDYING, "ber_unstoppable", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("ber_fists", "Poings d'Acier", "§7+{value}% dégâts à mains nues",
            c, TalentBranch.SPECIALTY, 1, 3, Material.IRON_INGOT, null, 1,
            TalentEffect.DAMAGE_PERCENT, 20, 10));

        register(new ClassTalent("ber_warhammer", "Marteau de Guerre", "§7Débloque: §eMasse Dévastatrice",
            c, TalentBranch.SPECIALTY, 2, 1, Material.MACE, "ber_fists", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("ber_groundslam", "Frappe Sismique", "§7Les coups chargés créent une onde ({value}m)",
            c, TalentBranch.SPECIALTY, 3, 3, Material.CRACKED_DEEPSLATE_BRICKS, "ber_warhammer", 2,
            TalentEffect.DAMAGE_FLAT, 3, 1));

        register(new ClassTalent("ber_battleaxe", "Hache de Bataille", "§7Débloque: §6Hache Démoniaque",
            c, TalentBranch.SPECIALTY, 4, 1, Material.NETHERITE_AXE, "ber_groundslam", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("ber_gauntlets", "Gantelets Destructeurs", "§7Débloque: §cPoings du Titan",
            c, TalentBranch.SPECIALTY, 5, 1, Material.NETHERITE_INGOT, "ber_battleaxe", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== SNIPER ====================
    private void registerSniperTalents() {
        ClassType c = ClassType.SNIPER;

        // === BRANCHE OFFENSIVE ===
        register(new ClassTalent("snp_headshot_1", "Tir de Précision I", "§7+{value}% dégâts headshot",
            c, TalentBranch.OFFENSE, 1, 3, Material.ARROW, null, 1,
            TalentEffect.HEADSHOT_DAMAGE, 15, 10));

        register(new ClassTalent("snp_headshot_2", "Tir de Précision II", "§7+{value}% dégâts headshot",
            c, TalentBranch.OFFENSE, 2, 3, Material.TIPPED_ARROW, "snp_headshot_1", 1,
            TalentEffect.HEADSHOT_DAMAGE, 25, 15));

        register(new ClassTalent("snp_crit", "Point Faible", "§7+{value}% chance de critique",
            c, TalentBranch.OFFENSE, 2, 3, Material.TARGET, "snp_headshot_1", 1,
            TalentEffect.CRIT_CHANCE, 10, 5));

        register(new ClassTalent("snp_pierce", "Tir Perforant", "§7Les tirs traversent {value} ennemis",
            c, TalentBranch.OFFENSE, 3, 3, Material.SPECTRAL_ARROW, "snp_crit", 2,
            TalentEffect.ARMOR_PENETRATION, 1, 1));

        register(new ClassTalent("snp_execute", "Coup de Grâce", "§7+{value}% dégâts aux marqués <30% HP",
            c, TalentBranch.OFFENSE, 4, 3, Material.WITHER_SKELETON_SKULL, "snp_pierce", 2,
            TalentEffect.EXECUTE_DAMAGE, 50, 25));

        register(new ClassTalent("snp_deadeye", "Œil du Mort", "§c[ULTIME] §7Débloque: Tir Parfait",
            c, TalentBranch.OFFENSE, 5, 1, Material.ENDER_EYE, "snp_execute", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE DÉFENSIVE ===
        register(new ClassTalent("snp_stealth_1", "Camouflage I", "§7+{value}s durée du camouflage",
            c, TalentBranch.DEFENSE, 1, 3, Material.GREEN_DYE, null, 1,
            TalentEffect.STEALTH_DURATION, 2, 1));

        register(new ClassTalent("snp_stealth_2", "Camouflage II", "§7+{value}s durée du camouflage",
            c, TalentBranch.DEFENSE, 2, 3, Material.LIME_DYE, "snp_stealth_1", 1,
            TalentEffect.STEALTH_DURATION, 3, 1.5));

        register(new ClassTalent("snp_distance", "Position Éloignée", "§7+{value}% réduction dégâts à distance",
            c, TalentBranch.DEFENSE, 2, 3, Material.SPYGLASS, "snp_stealth_1", 1,
            TalentEffect.DAMAGE_REDUCTION, 10, 5));

        register(new ClassTalent("snp_escape", "Repli Tactique", "§7+{value}% vitesse après un tir",
            c, TalentBranch.DEFENSE, 3, 3, Material.FEATHER, "snp_distance", 2,
            TalentEffect.MOVEMENT_SPEED, 15, 10));

        register(new ClassTalent("snp_ghillie", "Combinaison Ghillie", "§7Invisible immobile après {value}s",
            c, TalentBranch.DEFENSE, 4, 3, Material.GRASS_BLOCK, "snp_escape", 2,
            TalentEffect.STEALTH_DURATION, 3, -0.5)); // 3s, 2.5s, 2s

        register(new ClassTalent("snp_shadow_sniper", "Tireur Fantôme", "§c[ULTIME] §7Débloque: Tir Spectral",
            c, TalentBranch.DEFENSE, 5, 1, Material.SCULK_SENSOR, "snp_ghillie", 3,
            TalentEffect.UNLOCK_SKILL, 1, 0));

        // === BRANCHE SPÉCIALITÉ ===
        register(new ClassTalent("snp_mark", "Marquage", "§7Les tirs marquent les ennemis {value}s",
            c, TalentBranch.SPECIALTY, 1, 3, Material.GLOW_INK_SAC, null, 1,
            TalentEffect.MARK_DURATION, 5, 2));

        register(new ClassTalent("snp_silencer", "Fusil Silencieux", "§7Débloque: §eCarabine Silencieuse",
            c, TalentBranch.SPECIALTY, 2, 1, Material.CROSSBOW, "snp_mark", 2,
            TalentEffect.UNLOCK_WEAPON, 1, 0));

        register(new ClassTalent("snp_scope", "Lunette Avancée", "§7+{value}% zoom de la lunette",
            c, TalentBranch.SPECIALTY, 3, 3, Material.SPYGLASS, "snp_silencer", 2,
            TalentEffect.SCOPE_ZOOM, 25, 15));

        register(new ClassTalent("snp_antimat", "Fusil Antimatériel", "§7Débloque: §6Fusil .50 Cal",
            c, TalentBranch.SPECIALTY, 4, 1, Material.NETHERITE_INGOT, "snp_scope", 3,
            TalentEffect.UNLOCK_WEAPON, 2, 0));

        register(new ClassTalent("snp_railgun", "Canon Gauss", "§7Débloque: §cCanon Gauss",
            c, TalentBranch.SPECIALTY, 5, 1, Material.END_CRYSTAL, "snp_antimat", 3,
            TalentEffect.UNLOCK_WEAPON, 3, 0));
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient un talent par son ID
     */
    public ClassTalent getTalent(String id) {
        return talentsById.get(id);
    }

    /**
     * Obtient tous les talents d'une classe
     */
    public List<ClassTalent> getTalentsForClass(ClassType classType) {
        return talentsByClass.getOrDefault(classType, Collections.emptyList());
    }

    /**
     * Obtient les talents d'une branche spécifique
     */
    public List<ClassTalent> getTalentsForBranch(ClassType classType, TalentBranch branch) {
        return getTalentsForClass(classType).stream()
            .filter(t -> t.getBranch() == branch)
            .sorted((a, b) -> Integer.compare(a.getTier(), b.getTier()))
            .toList();
    }

    /**
     * Obtient les talents d'un tier spécifique
     */
    public List<ClassTalent> getTalentsAtTier(ClassType classType, int tier) {
        return getTalentsForClass(classType).stream()
            .filter(t -> t.getTier() == tier)
            .toList();
    }

    /**
     * Vérifie si un joueur peut débloquer un talent
     */
    public boolean canUnlock(Map<String, Integer> unlockedTalents, String talentId) {
        ClassTalent talent = getTalent(talentId);
        if (talent == null) return false;

        // Vérifier le prérequis
        if (talent.getPrerequisiteId() != null) {
            Integer prereqLevel = unlockedTalents.get(talent.getPrerequisiteId());
            ClassTalent prereq = getTalent(talent.getPrerequisiteId());
            if (prereqLevel == null || prereqLevel < prereq.getMaxLevel()) {
                return false;
            }
        }

        // Vérifier si pas déjà au max
        Integer currentLevel = unlockedTalents.getOrDefault(talentId, 0);
        return currentLevel < talent.getMaxLevel();
    }

    /**
     * Calcule le coût total pour améliorer un talent
     */
    public int getUpgradeCost(String talentId, int currentLevel) {
        ClassTalent talent = getTalent(talentId);
        if (talent == null) return 0;
        return talent.getPointCost();
    }
}
