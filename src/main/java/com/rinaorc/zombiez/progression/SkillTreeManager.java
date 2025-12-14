package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Système de compétences passives
 * 3 arbres: Combat, Survie, Utility
 * Points gagnés tous les 5 niveaux
 */
public class SkillTreeManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<String, Skill> skills;
    
    @Getter
    private final Map<SkillTree, List<Skill>> skillsByTree;
    
    // Points par niveau
    private static final int LEVELS_PER_SKILL_POINT = 5;

    public SkillTreeManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.skills = new LinkedHashMap<>();
        this.skillsByTree = new EnumMap<>(SkillTree.class);
        
        registerAllSkills();
        organizeByTree();
    }

    /**
     * Enregistre toutes les compétences
     */
    private void registerAllSkills() {
        // ============ ARBRE COMBAT ============
        
        // Tier 1 (niveau requis: 1)
        register(new Skill("damage_1", "Puissance I", 
            "§7+5% de dégâts", SkillTree.COMBAT, 1,
            Material.IRON_SWORD, 1, null,
            SkillBonus.DAMAGE_PERCENT, 5));
        
        register(new Skill("damage_2", "Puissance II", 
            "§7+10% de dégâts", SkillTree.COMBAT, 2,
            Material.DIAMOND_SWORD, 1, "damage_1",
            SkillBonus.DAMAGE_PERCENT, 10));
        
        register(new Skill("damage_3", "Puissance III", 
            "§7+15% de dégâts", SkillTree.COMBAT, 3,
            Material.NETHERITE_SWORD, 2, "damage_2",
            SkillBonus.DAMAGE_PERCENT, 15));
        
        register(new Skill("crit_chance_1", "Œil du Prédateur I", 
            "§7+5% chance de critique", SkillTree.COMBAT, 1,
            Material.SPIDER_EYE, 1, null,
            SkillBonus.CRIT_CHANCE, 5));
        
        register(new Skill("crit_chance_2", "Œil du Prédateur II", 
            "§7+10% chance de critique", SkillTree.COMBAT, 2,
            Material.FERMENTED_SPIDER_EYE, 2, "crit_chance_1",
            SkillBonus.CRIT_CHANCE, 10));
        
        register(new Skill("crit_damage", "Coup Fatal", 
            "§7+25% dégâts critiques", SkillTree.COMBAT, 2,
            Material.GOLDEN_SWORD, 2, "crit_chance_1",
            SkillBonus.CRIT_DAMAGE, 25));
        
        register(new Skill("attack_speed", "Frénésie", 
            "§7+10% vitesse d'attaque", SkillTree.COMBAT, 2,
            Material.SUGAR, 1, "damage_1",
            SkillBonus.ATTACK_SPEED, 10));
        
        register(new Skill("lifesteal", "Vol de Vie", 
            "§7+5% vol de vie sur attaque", SkillTree.COMBAT, 3,
            Material.GHAST_TEAR, 3, "damage_2",
            SkillBonus.LIFESTEAL, 5));
        
        register(new Skill("execute", "Exécution", 
            "§7+20% dégâts aux ennemis <20% HP", SkillTree.COMBAT, 3,
            Material.WITHER_SKELETON_SKULL, 3, "crit_damage",
            SkillBonus.EXECUTE_DAMAGE, 20));
        
        register(new Skill("berserker", "Berserker", 
            "§7+30% dégâts quand <30% HP", SkillTree.COMBAT, 4,
            Material.BLAZE_POWDER, 4, "lifesteal",
            SkillBonus.BERSERKER, 30));
        
        // ============ ARBRE SURVIE ============
        
        register(new Skill("health_1", "Constitution I", 
            "§7+10% HP max", SkillTree.SURVIVAL, 1,
            Material.APPLE, 1, null,
            SkillBonus.HEALTH_PERCENT, 10));
        
        register(new Skill("health_2", "Constitution II", 
            "§7+20% HP max", SkillTree.SURVIVAL, 2,
            Material.GOLDEN_APPLE, 2, "health_1",
            SkillBonus.HEALTH_PERCENT, 20));
        
        register(new Skill("health_3", "Constitution III", 
            "§7+30% HP max", SkillTree.SURVIVAL, 3,
            Material.ENCHANTED_GOLDEN_APPLE, 3, "health_2",
            SkillBonus.HEALTH_PERCENT, 30));
        
        register(new Skill("armor_1", "Peau Épaisse I", 
            "§7+10% réduction de dégâts", SkillTree.SURVIVAL, 1,
            Material.LEATHER_CHESTPLATE, 1, null,
            SkillBonus.DAMAGE_REDUCTION, 10));
        
        register(new Skill("armor_2", "Peau Épaisse II", 
            "§7+15% réduction de dégâts", SkillTree.SURVIVAL, 2,
            Material.IRON_CHESTPLATE, 2, "armor_1",
            SkillBonus.DAMAGE_REDUCTION, 15));
        
        register(new Skill("regen_1", "Régénération I", 
            "§7Régénération lente en combat", SkillTree.SURVIVAL, 2,
            Material.GLISTERING_MELON_SLICE, 2, "health_1",
            SkillBonus.REGEN, 1));
        
        register(new Skill("regen_2", "Régénération II", 
            "§7Régénération améliorée", SkillTree.SURVIVAL, 3,
            Material.GOLDEN_CARROT, 3, "regen_1",
            SkillBonus.REGEN, 2));
        
        register(new Skill("resistance", "Résistance", 
            "§7Résistance aux effets négatifs -20%", SkillTree.SURVIVAL, 2,
            Material.MILK_BUCKET, 2, "armor_1",
            SkillBonus.DEBUFF_RESISTANCE, 20));
        
        register(new Skill("second_wind", "Second Souffle", 
            "§7+50% HP quand tu tuerais un zombie <10% HP", SkillTree.SURVIVAL, 3,
            Material.TOTEM_OF_UNDYING, 3, "regen_1",
            SkillBonus.SECOND_WIND, 50));
        
        register(new Skill("immortal", "Immortel", 
            "§7Survie à un coup fatal 1x/5min", SkillTree.SURVIVAL, 4,
            Material.NETHER_STAR, 5, "second_wind",
            SkillBonus.IMMORTAL, 1));
        
        // ============ ARBRE UTILITÉ ============
        
        register(new Skill("xp_boost_1", "Sagesse I", 
            "§7+10% XP gagné", SkillTree.UTILITY, 1,
            Material.EXPERIENCE_BOTTLE, 1, null,
            SkillBonus.XP_BOOST, 10));
        
        register(new Skill("xp_boost_2", "Sagesse II", 
            "§7+20% XP gagné", SkillTree.UTILITY, 2,
            Material.EXPERIENCE_BOTTLE, 2, "xp_boost_1",
            SkillBonus.XP_BOOST, 20));
        
        register(new Skill("loot_boost_1", "Chance I", 
            "§7+10% chance de loot", SkillTree.UTILITY, 1,
            Material.RABBIT_FOOT, 1, null,
            SkillBonus.LOOT_CHANCE, 10));
        
        register(new Skill("loot_boost_2", "Chance II", 
            "§7+20% chance de loot", SkillTree.UTILITY, 2,
            Material.RABBIT_FOOT, 2, "loot_boost_1",
            SkillBonus.LOOT_CHANCE, 20));
        
        register(new Skill("rare_find", "Trouveur de Raretés", 
            "§7+5% chance de rareté supérieure", SkillTree.UTILITY, 3,
            Material.AMETHYST_SHARD, 3, "loot_boost_2",
            SkillBonus.RARE_CHANCE, 5));
        
        register(new Skill("speed_1", "Vélocité I", 
            "§7+10% vitesse de déplacement", SkillTree.UTILITY, 1,
            Material.FEATHER, 1, null,
            SkillBonus.SPEED, 10));
        
        register(new Skill("speed_2", "Vélocité II", 
            "§7+15% vitesse de déplacement", SkillTree.UTILITY, 2,
            Material.FEATHER, 2, "speed_1",
            SkillBonus.SPEED, 15));
        
        register(new Skill("points_boost", "Économiste", 
            "§7+15% points gagnés", SkillTree.UTILITY, 2,
            Material.GOLD_INGOT, 2, null,
            SkillBonus.POINTS_BOOST, 15));
        
        register(new Skill("gem_finder", "Chercheur de Gemmes", 
            "§7+10% chance de gemmes sur kill", SkillTree.UTILITY, 3,
            Material.DIAMOND, 3, "points_boost",
            SkillBonus.GEM_CHANCE, 10));
        
        register(new Skill("treasure_hunter", "Chasseur de Trésors", 
            "§7Double loot des boss", SkillTree.UTILITY, 4,
            Material.CHEST, 5, "rare_find",
            SkillBonus.BOSS_LOOT, 100));
    }

    /**
     * Enregistre une compétence
     */
    private void register(Skill skill) {
        skills.put(skill.id(), skill);
    }

    /**
     * Organise par arbre
     */
    private void organizeByTree() {
        for (SkillTree tree : SkillTree.values()) {
            skillsByTree.put(tree, new ArrayList<>());
        }
        
        for (Skill skill : skills.values()) {
            skillsByTree.get(skill.tree()).add(skill);
        }
    }

    /**
     * Calcule les points de compétence disponibles
     */
    public int getAvailablePoints(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        
        int level = data.getLevel().get();
        int prestige = data.getPrestige().get();
        
        // Points par niveau
        int levelPoints = level / LEVELS_PER_SKILL_POINT;
        
        // Bonus de prestige (5 points par prestige)
        int prestigePoints = prestige * 5;
        
        // Total disponible
        int total = levelPoints + prestigePoints;
        
        // Moins les points déjà dépensés
        int spent = data.getSpentSkillPoints();
        
        return total - spent;
    }

    /**
     * Débloque une compétence
     */
    public boolean unlockSkill(Player player, String skillId) {
        Skill skill = skills.get(skillId);
        if (skill == null) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Vérifications
        if (data.hasSkill(skillId)) {
            player.sendMessage("§cTu possèdes déjà cette compétence!");
            return false;
        }
        
        if (skill.prerequisite() != null && !data.hasSkill(skill.prerequisite())) {
            Skill prereq = skills.get(skill.prerequisite());
            player.sendMessage("§cTu dois d'abord débloquer: §e" + prereq.name());
            return false;
        }
        
        int available = getAvailablePoints(player);
        if (available < skill.cost()) {
            player.sendMessage("§cPas assez de points! (Requis: " + skill.cost() + ", Disponible: " + available + ")");
            return false;
        }
        
        // Débloquer
        data.addSkill(skillId);
        data.addSpentSkillPoints(skill.cost());
        
        // Notification
        player.sendMessage("§a✓ Compétence débloquée: §e" + skill.name());
        player.sendMessage("§7" + skill.description());
        
        // Appliquer immédiatement les effets passifs
        applySkillEffects(player);
        
        return true;
    }

    /**
     * Applique les effets des compétences
     */
    public void applySkillEffects(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        // Effets de régénération
        if (data.hasSkill("regen_2")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));
        } else if (data.hasSkill("regen_1")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));
        }
        
        // Effets de vitesse
        if (data.hasSkill("speed_2")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        } else if (data.hasSkill("speed_1")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
        }
    }

    /**
     * Obtient le bonus total d'un type
     */
    public double getSkillBonus(Player player, SkillBonus bonusType) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        
        double total = 0;
        
        for (String skillId : data.getUnlockedSkills()) {
            Skill skill = skills.get(skillId);
            if (skill != null && skill.bonusType() == bonusType) {
                total += skill.bonusValue();
            }
        }
        
        return total;
    }

    /**
     * Réinitialise les compétences d'un joueur
     */
    public boolean resetSkills(Player player, boolean free) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Coût de reset (sauf si gratuit)
        if (!free) {
            int gemCost = 50;
            if (!plugin.getEconomyManager().hasGems(player, gemCost)) {
                player.sendMessage("§cCoût du reset: " + gemCost + " Gemmes");
                return false;
            }
            plugin.getEconomyManager().removeGems(player, gemCost);
        }
        
        // Reset
        data.clearSkills();
        data.setSpentSkillPoints(0);
        
        // Retirer les effets
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SPEED);
        
        player.sendMessage("§a✓ Compétences réinitialisées!");
        return true;
    }

    /**
     * Arbres de compétences
     */
    @Getter
    public enum SkillTree {
        COMBAT("Combat", "§c", Material.DIAMOND_SWORD, "Améliore tes capacités offensives"),
        SURVIVAL("Survie", "§a", Material.GOLDEN_APPLE, "Améliore ta résistance et régénération"),
        UTILITY("Utilitaire", "§e", Material.CHEST, "Améliore tes gains et ta mobilité");
        
        private final String displayName;
        private final String color;
        private final Material icon;
        private final String description;
        
        SkillTree(String displayName, String color, Material icon, String description) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
            this.description = description;
        }
    }

    /**
     * Types de bonus
     */
    public enum SkillBonus {
        DAMAGE_PERCENT,
        CRIT_CHANCE,
        CRIT_DAMAGE,
        ATTACK_SPEED,
        LIFESTEAL,
        EXECUTE_DAMAGE,
        BERSERKER,
        HEALTH_PERCENT,
        DAMAGE_REDUCTION,
        REGEN,
        DEBUFF_RESISTANCE,
        SECOND_WIND,
        IMMORTAL,
        XP_BOOST,
        LOOT_CHANCE,
        RARE_CHANCE,
        SPEED,
        POINTS_BOOST,
        GEM_CHANCE,
        BOSS_LOOT
    }

    /**
     * Représente une compétence
     */
    public record Skill(
        String id,
        String name,
        String description,
        SkillTree tree,
        int tier,
        Material icon,
        int cost,
        String prerequisite,
        SkillBonus bonusType,
        double bonusValue
    ) {}
}
