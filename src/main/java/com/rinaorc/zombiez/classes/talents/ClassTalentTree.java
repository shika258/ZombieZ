package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.ClassTalent.TalentBranch;
import com.rinaorc.zombiez.classes.talents.ClassTalent.TalentEffect;
import lombok.Getter;
import org.bukkit.Material;

import java.util.*;

/**
 * Système de talents simplifié - 2 branches par classe
 * - COMBAT: Améliore les dégâts et les attaques
 * - SURVIE: Améliore la défense et l'utilité
 *
 * Chaque branche a 5 talents linéaires, faciles à comprendre
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
        registerGuerrierTalents();
        registerChasseurTalents();
        registerOccultisteTalents();
    }

    private void register(ClassTalent talent) {
        talentsById.put(talent.getId(), talent);
        talentsByClass.computeIfAbsent(talent.getClassType(), k -> new ArrayList<>()).add(talent);
    }

    // ==================== GUERRIER ====================
    // Tank mêlée avec focus sur la survie et les dégâts bruts
    private void registerGuerrierTalents() {
        ClassType c = ClassType.GUERRIER;

        // === BRANCHE COMBAT (Rouge) - Augmente les dégâts mêlée ===
        register(new ClassTalent("gue_dmg_1", "Force Brute",
            "§c+{value}% §7dégâts en mêlée",
            c, TalentBranch.OFFENSE, 1, 3, Material.IRON_SWORD, null, 1,
            TalentEffect.DAMAGE_PERCENT, 5, 3));

        register(new ClassTalent("gue_dmg_2", "Frappe Dévastatrice",
            "§c+{value}% §7dégâts en mêlée",
            c, TalentBranch.OFFENSE, 2, 3, Material.DIAMOND_SWORD, "gue_dmg_1", 1,
            TalentEffect.DAMAGE_PERCENT, 8, 4));

        register(new ClassTalent("gue_cleave", "Balayage",
            "§7Touche §c{value} §7ennemis supplémentaires",
            c, TalentBranch.OFFENSE, 3, 3, Material.GOLDEN_AXE, "gue_dmg_2", 1,
            TalentEffect.CLEAVE_TARGETS, 1, 1));

        register(new ClassTalent("gue_rage", "Rage Guerrière",
            "§c+{value}% §7dégâts quand <50% HP",
            c, TalentBranch.OFFENSE, 4, 3, Material.BLAZE_POWDER, "gue_cleave", 1,
            TalentEffect.RAGE_DAMAGE, 15, 10));

        register(new ClassTalent("gue_execute", "Exécution",
            "§c+{value}% §7dégâts aux cibles <25% HP",
            c, TalentBranch.OFFENSE, 5, 1, Material.NETHERITE_AXE, "gue_rage", 1,
            TalentEffect.EXECUTE_DAMAGE, 50, 0));

        // === BRANCHE SURVIE (Orange) - Augmente la résistance ===
        register(new ClassTalent("gue_hp_1", "Constitution",
            "§6+{value}% §7points de vie max",
            c, TalentBranch.DEFENSE, 1, 3, Material.APPLE, null, 1,
            TalentEffect.HEALTH_PERCENT, 5, 3));

        register(new ClassTalent("gue_armor", "Peau d'Acier",
            "§6-{value}% §7dégâts reçus",
            c, TalentBranch.DEFENSE, 2, 3, Material.IRON_CHESTPLATE, "gue_hp_1", 1,
            TalentEffect.DAMAGE_REDUCTION, 5, 3));

        register(new ClassTalent("gue_lifesteal", "Vampirisme",
            "§6+{value}% §7vol de vie",
            c, TalentBranch.DEFENSE, 3, 3, Material.GHAST_TEAR, "gue_armor", 1,
            TalentEffect.LIFESTEAL, 5, 3));

        register(new ClassTalent("gue_regen", "Régénération",
            "§6+{value}% §7régénération de vie",
            c, TalentBranch.DEFENSE, 4, 3, Material.GOLDEN_APPLE, "gue_lifesteal", 1,
            TalentEffect.REGEN_PERCENT, 10, 5));

        register(new ClassTalent("gue_laststand", "Dernier Souffle",
            "§6Survit §7à un coup fatal (§61x/5min§7)",
            c, TalentBranch.DEFENSE, 5, 1, Material.TOTEM_OF_UNDYING, "gue_regen", 1,
            TalentEffect.LAST_STAND, 1, 0));
    }

    // ==================== CHASSEUR ====================
    // DPS distance avec focus sur les critiques et la mobilité
    private void registerChasseurTalents() {
        ClassType c = ClassType.CHASSEUR;

        // === BRANCHE COMBAT (Vert) - Augmente les critiques et dégâts ===
        register(new ClassTalent("cha_crit_1", "Oeil de Lynx",
            "§a+{value}% §7chance de critique",
            c, TalentBranch.OFFENSE, 1, 3, Material.SPIDER_EYE, null, 1,
            TalentEffect.CRIT_CHANCE, 5, 3));

        register(new ClassTalent("cha_crit_dmg", "Coup Fatal",
            "§a+{value}% §7dégâts critiques",
            c, TalentBranch.OFFENSE, 2, 3, Material.DIAMOND, "cha_crit_1", 1,
            TalentEffect.CRIT_DAMAGE, 15, 10));

        register(new ClassTalent("cha_headshot", "Tir en Tête",
            "§a+{value}% §7dégâts de headshot",
            c, TalentBranch.OFFENSE, 3, 3, Material.TARGET, "cha_crit_dmg", 1,
            TalentEffect.HEADSHOT_DAMAGE, 20, 15));

        register(new ClassTalent("cha_pierce", "Tir Perforant",
            "§7Les tirs traversent §a{value} §7ennemis",
            c, TalentBranch.OFFENSE, 4, 3, Material.SPECTRAL_ARROW, "cha_headshot", 1,
            TalentEffect.PIERCE_TARGETS, 1, 1));

        register(new ClassTalent("cha_deadeye", "Oeil du Mort",
            "§aProchain tir §7= Critique garanti",
            c, TalentBranch.OFFENSE, 5, 1, Material.ENDER_EYE, "cha_pierce", 1,
            TalentEffect.GUARANTEED_CRIT, 1, 0));

        // === BRANCHE SURVIE (Cyan) - Mobilité et évasion ===
        register(new ClassTalent("cha_speed", "Pied Léger",
            "§b+{value}% §7vitesse de déplacement",
            c, TalentBranch.DEFENSE, 1, 3, Material.FEATHER, null, 1,
            TalentEffect.MOVEMENT_SPEED, 5, 3));

        register(new ClassTalent("cha_dodge", "Esquive",
            "§b{value}% §7chance d'esquiver",
            c, TalentBranch.DEFENSE, 2, 3, Material.PHANTOM_MEMBRANE, "cha_speed", 1,
            TalentEffect.DODGE_CHANCE, 5, 3));

        register(new ClassTalent("cha_stealth", "Furtivité",
            "§b+{value}% §7dégâts en sortie de furtivité",
            c, TalentBranch.DEFENSE, 3, 3, Material.POTION, "cha_dodge", 1,
            TalentEffect.STEALTH_DAMAGE, 20, 15));

        register(new ClassTalent("cha_reload", "Rechargement Rapide",
            "§b-{value}% §7cooldown des compétences",
            c, TalentBranch.DEFENSE, 4, 3, Material.CLOCK, "cha_stealth", 1,
            TalentEffect.COOLDOWN_REDUCTION, 10, 5));

        register(new ClassTalent("cha_vanish", "Disparition",
            "§bInvisibilité §7après chaque kill (§b3s§7)",
            c, TalentBranch.DEFENSE, 5, 1, Material.ENDER_PEARL, "cha_reload", 1,
            TalentEffect.STEALTH_ON_KILL, 3, 0));
    }

    // ==================== OCCULTISTE ====================
    // Mage AoE avec focus sur les compétences et le contrôle
    private void registerOccultisteTalents() {
        ClassType c = ClassType.OCCULTISTE;

        // === BRANCHE COMBAT (Violet) - Augmente la puissance des sorts ===
        register(new ClassTalent("occ_power_1", "Pouvoir Arcanique",
            "§5+{value}% §7dégâts des compétences",
            c, TalentBranch.OFFENSE, 1, 3, Material.AMETHYST_SHARD, null, 1,
            TalentEffect.SKILL_POWER, 8, 5));

        register(new ClassTalent("occ_aoe", "Explosion",
            "§5+{value}% §7rayon des AoE",
            c, TalentBranch.OFFENSE, 2, 3, Material.FIRE_CHARGE, "occ_power_1", 1,
            TalentEffect.AOE_RADIUS, 15, 10));

        register(new ClassTalent("occ_dot", "Corruption",
            "§7Les sorts infligent §5{value}% §7dégâts sur 3s",
            c, TalentBranch.OFFENSE, 3, 3, Material.WITHER_ROSE, "occ_aoe", 1,
            TalentEffect.DOT_DAMAGE, 10, 5));

        register(new ClassTalent("occ_cdr", "Canalisation",
            "§5-{value}% §7temps de recharge",
            c, TalentBranch.OFFENSE, 4, 3, Material.ENCHANTED_BOOK, "occ_dot", 1,
            TalentEffect.COOLDOWN_REDUCTION, 10, 5));

        register(new ClassTalent("occ_ultimate", "Pouvoir Suprême",
            "§5Ultime §7sans cooldown (§51x/2min§7)",
            c, TalentBranch.OFFENSE, 5, 1, Material.NETHER_STAR, "occ_cdr", 1,
            TalentEffect.ULTIMATE_RESET, 1, 0));

        // === BRANCHE SURVIE (Bleu) - Énergie et contrôle ===
        register(new ClassTalent("occ_energy_1", "Réserve Magique",
            "§9+{value} §7énergie max",
            c, TalentBranch.DEFENSE, 1, 3, Material.LAPIS_LAZULI, null, 1,
            TalentEffect.ENERGY_MAX, 20, 10));

        register(new ClassTalent("occ_regen_energy", "Flux Magique",
            "§9+{value}% §7régénération d'énergie",
            c, TalentBranch.DEFENSE, 2, 3, Material.GLOWSTONE_DUST, "occ_energy_1", 1,
            TalentEffect.ENERGY_REGEN, 15, 10));

        register(new ClassTalent("occ_shield", "Barrière",
            "§9{value}% §7des dégâts absorbés en énergie",
            c, TalentBranch.DEFENSE, 3, 3, Material.END_CRYSTAL, "occ_regen_energy", 1,
            TalentEffect.DAMAGE_TO_ENERGY, 10, 5));

        register(new ClassTalent("occ_leech", "Drain Vital",
            "§9{value}% §7des dégâts de sorts = vie",
            c, TalentBranch.DEFENSE, 4, 3, Material.DRAGON_BREATH, "occ_shield", 1,
            TalentEffect.SPELL_LIFESTEAL, 5, 3));

        register(new ClassTalent("occ_immortal", "Phylactère",
            "§9Ressuscite §7avec 30% HP (§91x/5min§7)",
            c, TalentBranch.DEFENSE, 5, 1, Material.SOUL_LANTERN, "occ_leech", 1,
            TalentEffect.RESURRECT, 1, 0));
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
            if (prereqLevel == null || prereqLevel < 1) {
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

    /**
     * Obtient le nombre total de points investis par un joueur dans une branche
     */
    public int getPointsInBranch(ClassType classType, TalentBranch branch, Map<String, Integer> unlockedTalents) {
        return getTalentsForBranch(classType, branch).stream()
            .mapToInt(t -> unlockedTalents.getOrDefault(t.getId(), 0))
            .sum();
    }

    /**
     * Calcule le pourcentage de complétion d'une branche
     */
    public double getBranchCompletion(ClassType classType, TalentBranch branch, Map<String, Integer> unlockedTalents) {
        List<ClassTalent> talents = getTalentsForBranch(classType, branch);
        int maxPoints = talents.stream().mapToInt(ClassTalent::getMaxLevel).sum();
        int currentPoints = getPointsInBranch(classType, branch, unlockedTalents);
        return maxPoints > 0 ? (double) currentPoints / maxPoints : 0;
    }
}
