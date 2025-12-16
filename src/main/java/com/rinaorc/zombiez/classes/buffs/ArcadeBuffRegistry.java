package com.rinaorc.zombiez.classes.buffs;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffCategory;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffEffect;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffRarity;
import lombok.Getter;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registre de tous les buffs arcade disponibles
 * Système style MegaBonk: à chaque level up de classe, 3 buffs sont proposés
 */
@Getter
public class ArcadeBuffRegistry {

    private final List<ArcadeBuff> allBuffs;
    private final Map<String, ArcadeBuff> buffsById;
    private final Map<BuffRarity, List<ArcadeBuff>> buffsByRarity;
    private final Map<ClassType, List<ArcadeBuff>> buffsByPreferredClass;

    public ArcadeBuffRegistry() {
        this.allBuffs = new ArrayList<>();
        this.buffsById = new LinkedHashMap<>();
        this.buffsByRarity = new EnumMap<>(BuffRarity.class);
        this.buffsByPreferredClass = new EnumMap<>(ClassType.class);

        registerAllBuffs();
        organizeBuffs();
    }

    private void register(ArcadeBuff buff) {
        allBuffs.add(buff);
        buffsById.put(buff.getId(), buff);
    }

    private void organizeBuffs() {
        for (BuffRarity rarity : BuffRarity.values()) {
            buffsByRarity.put(rarity, new ArrayList<>());
        }
        for (ClassType classType : ClassType.values()) {
            buffsByPreferredClass.put(classType, new ArrayList<>());
        }

        for (ArcadeBuff buff : allBuffs) {
            buffsByRarity.get(buff.getRarity()).add(buff);
            if (buff.getPreferredClass() != null) {
                buffsByPreferredClass.get(buff.getPreferredClass()).add(buff);
            }
        }
    }

    private void registerAllBuffs() {
        // ==================== BUFFS COMMUNS (50% pool) ====================

        // Offensifs légers
        register(new ArcadeBuff("buff_dmg_1", "Puissance Mineure",
            "+{value} dégâts de base", BuffCategory.OFFENSE, BuffRarity.COMMON,
            Material.IRON_SWORD, null, BuffEffect.DAMAGE_FLAT, 2, 10));

        register(new ArcadeBuff("buff_crit_1", "Œil Aiguisé",
            "+{value} chance de critique", BuffCategory.OFFENSE, BuffRarity.COMMON,
            Material.SPIDER_EYE, null, BuffEffect.CRIT_CHANCE, 1, 15));

        register(new ArcadeBuff("buff_atk_spd_1", "Réflexes",
            "+{value} vitesse d'attaque", BuffCategory.OFFENSE, BuffRarity.COMMON,
            Material.SUGAR, null, BuffEffect.ATTACK_SPEED, 2, 10));

        // Défensifs légers
        register(new ArcadeBuff("buff_hp_1", "Vitalité Mineure",
            "+{value} HP maximum", BuffCategory.DEFENSE, BuffRarity.COMMON,
            Material.APPLE, null, BuffEffect.HEALTH_FLAT, 5, 20));

        register(new ArcadeBuff("buff_armor_1", "Peau Dure",
            "+{value} réduction de dégâts", BuffCategory.DEFENSE, BuffRarity.COMMON,
            Material.IRON_CHESTPLATE, null, BuffEffect.DAMAGE_REDUCTION, 1, 15));

        register(new ArcadeBuff("buff_regen_1", "Récupération",
            "+{value} régénération", BuffCategory.DEFENSE, BuffRarity.COMMON,
            Material.GLISTERING_MELON_SLICE, null, BuffEffect.REGEN, 2, 10));

        // Utilitaires légers
        register(new ArcadeBuff("buff_speed_1", "Pied Léger",
            "+{value} vitesse de déplacement", BuffCategory.UTILITY, BuffRarity.COMMON,
            Material.FEATHER, null, BuffEffect.MOVEMENT_SPEED, 1, 15));

        register(new ArcadeBuff("buff_xp_1", "Apprenti",
            "+{value} XP gagné", BuffCategory.UTILITY, BuffRarity.COMMON,
            Material.EXPERIENCE_BOTTLE, null, BuffEffect.XP_BONUS, 2, 10));

        register(new ArcadeBuff("buff_loot_1", "Chanceux",
            "+{value} chance de loot", BuffCategory.UTILITY, BuffRarity.COMMON,
            Material.RABBIT_FOOT, null, BuffEffect.LOOT_BONUS, 1, 15));

        // ==================== BUFFS PEU COMMUNS (30% pool) ====================

        // Offensifs moyens
        register(new ArcadeBuff("buff_dmg_2", "Puissance",
            "+{value} dégâts", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.DIAMOND_SWORD, null, BuffEffect.DAMAGE_PERCENT, 2, 8));

        register(new ArcadeBuff("buff_crit_dmg_1", "Coup Critique",
            "+{value} dégâts critiques", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.DIAMOND, null, BuffEffect.CRIT_DAMAGE, 5, 8));

        register(new ArcadeBuff("buff_lifesteal_1", "Sanguinaire",
            "+{value} vol de vie", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.GHAST_TEAR, null, BuffEffect.LIFESTEAL, 1, 10));

        register(new ArcadeBuff("buff_armor_pen_1", "Perforant",
            "+{value} pénétration d'armure", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.ARROW, null, BuffEffect.ARMOR_PEN, 2, 8));

        // Défensifs moyens
        register(new ArcadeBuff("buff_hp_2", "Vitalité",
            "+{value} HP maximum", BuffCategory.DEFENSE, BuffRarity.UNCOMMON,
            Material.GOLDEN_APPLE, null, BuffEffect.HEALTH_PERCENT, 3, 8));

        register(new ArcadeBuff("buff_dodge_1", "Évasif",
            "+{value} chance d'esquive", BuffCategory.DEFENSE, BuffRarity.UNCOMMON,
            Material.PHANTOM_MEMBRANE, null, BuffEffect.DODGE_CHANCE, 1, 10));

        register(new ArcadeBuff("buff_thorns_1", "Épineux",
            "+{value} dégâts de renvoi", BuffCategory.DEFENSE, BuffRarity.UNCOMMON,
            Material.CACTUS, null, BuffEffect.THORNS, 3, 8));

        // Utilitaires moyens
        register(new ArcadeBuff("buff_cdr_1", "Tacticien",
            "-{value} temps de recharge", BuffCategory.UTILITY, BuffRarity.UNCOMMON,
            Material.CLOCK, null, BuffEffect.COOLDOWN_RED, 2, 10));

        register(new ArcadeBuff("buff_energy_1", "Énergique",
            "+{value} régénération d'énergie", BuffCategory.UTILITY, BuffRarity.UNCOMMON,
            Material.GLOWSTONE_DUST, null, BuffEffect.ENERGY_REGEN, 3, 8));

        // ==================== BUFFS RARES (15% pool) ====================

        // Offensifs forts
        register(new ArcadeBuff("buff_dmg_3", "Force Majeure",
            "+{value} dégâts", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.NETHERITE_SWORD, null, BuffEffect.DAMAGE_PERCENT, 4, 5));

        register(new ArcadeBuff("buff_crit_2", "Assassin",
            "+{value} chance et dégâts critiques", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.WITHER_SKELETON_SKULL, null, BuffEffect.CRIT_CHANCE, 3, 5));

        register(new ArcadeBuff("buff_headshot_1", "Tireur d'Élite",
            "+{value} dégâts headshot", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.TARGET, null, BuffEffect.HEADSHOT_DMG, 8, 5));

        // Défensifs forts
        register(new ArcadeBuff("buff_hp_3", "Colosse",
            "+{value} HP maximum", BuffCategory.DEFENSE, BuffRarity.RARE,
            Material.ENCHANTED_GOLDEN_APPLE, null, BuffEffect.HEALTH_PERCENT, 5, 5));

        register(new ArcadeBuff("buff_armor_2", "Forteresse",
            "+{value} réduction de dégâts", BuffCategory.DEFENSE, BuffRarity.RARE,
            Material.DIAMOND_CHESTPLATE, null, BuffEffect.DAMAGE_REDUCTION, 3, 5));

        // Hybrides
        register(new ArcadeBuff("buff_skill_1", "Maître des Arts",
            "+{value} puissance des compétences", BuffCategory.HYBRID, BuffRarity.RARE,
            Material.ENCHANTED_BOOK, null, BuffEffect.SKILL_POWER, 5, 5));

        // ==================== BUFFS ÉPIQUES (5% pool) ====================

        // Buffs légendaires universels
        register(new ArcadeBuff("buff_berserker", "Berserker Né",
            "+{value} dégâts quand <30% HP", BuffCategory.OFFENSE, BuffRarity.EPIC,
            Material.BLAZE_POWDER, null, BuffEffect.DAMAGE_PERCENT, 15, 2));

        register(new ArcadeBuff("buff_immortal", "Second Souffle",
            "+{value} HP sur kill", BuffCategory.DEFENSE, BuffRarity.EPIC,
            Material.TOTEM_OF_UNDYING, null, BuffEffect.HEALTH_FLAT, 10, 3));

        register(new ArcadeBuff("buff_ultimate", "Ultime Rapide",
            "-{value} cooldown ultime", BuffCategory.UTILITY, BuffRarity.EPIC,
            Material.NETHER_STAR, null, BuffEffect.ULTIMATE_CDR, 10, 3));

        register(new ArcadeBuff("buff_class_mastery", "Maîtrise de Classe",
            "+{value} bonus de classe", BuffCategory.HYBRID, BuffRarity.EPIC,
            Material.DRAGON_EGG, null, BuffEffect.CLASS_BONUS, 5, 3));

        // ==================== BUFFS SPÉCIFIQUES PAR CLASSE ====================

        // Commando
        register(new ArcadeBuff("buff_cmd_heavy", "Spécialiste Armes Lourdes",
            "+{value} dégâts avec armes lourdes", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.CROSSBOW, ClassType.COMMANDO, BuffEffect.DAMAGE_PERCENT, 4, 6));

        register(new ArcadeBuff("buff_cmd_explosive", "Expert en Explosifs",
            "+{value} dégâts explosifs", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.TNT, ClassType.COMMANDO, BuffEffect.DAMAGE_PERCENT, 8, 4));

        // Scout
        register(new ArcadeBuff("buff_sct_crit", "Maître Assassin",
            "+{value} dégâts critiques", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.IRON_SWORD, ClassType.SCOUT, BuffEffect.CRIT_DAMAGE, 8, 6));

        register(new ArcadeBuff("buff_sct_stealth", "Ombre Mortelle",
            "+{value} dégâts depuis furtivité", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.ENDER_EYE, ClassType.SCOUT, BuffEffect.DAMAGE_PERCENT, 10, 4));

        // Medic
        register(new ArcadeBuff("buff_med_heal", "Guérisseur",
            "+{value} puissance de soin", BuffCategory.DEFENSE, BuffRarity.UNCOMMON,
            Material.GLISTERING_MELON_SLICE, ClassType.MEDIC, BuffEffect.HEALTH_PERCENT, 5, 6));

        register(new ArcadeBuff("buff_med_regen", "Régénérateur",
            "+{value} régénération passive", BuffCategory.DEFENSE, BuffRarity.RARE,
            Material.GOLDEN_CARROT, ClassType.MEDIC, BuffEffect.REGEN, 8, 4));

        // Engineer
        register(new ArcadeBuff("buff_eng_turret", "Maître Tourelles",
            "+{value} dégâts des tourelles", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.DISPENSER, ClassType.ENGINEER, BuffEffect.DAMAGE_PERCENT, 6, 6));

        register(new ArcadeBuff("buff_eng_tech", "Génie Technologique",
            "+{value} durée des constructions", BuffCategory.UTILITY, BuffRarity.RARE,
            Material.REDSTONE, ClassType.ENGINEER, BuffEffect.COOLDOWN_RED, 5, 4));

        // Berserker
        register(new ArcadeBuff("buff_ber_melee", "Maître Mêlée",
            "+{value} dégâts mêlée", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.NETHERITE_AXE, ClassType.BERSERKER, BuffEffect.DAMAGE_PERCENT, 5, 6));

        register(new ArcadeBuff("buff_ber_rage", "Rage Incontrôlable",
            "+{value} vol de vie en rage", BuffCategory.DEFENSE, BuffRarity.RARE,
            Material.BLAZE_POWDER, ClassType.BERSERKER, BuffEffect.LIFESTEAL, 5, 4));

        // Sniper
        register(new ArcadeBuff("buff_snp_precision", "Précision Mortelle",
            "+{value} dégâts headshot", BuffCategory.OFFENSE, BuffRarity.UNCOMMON,
            Material.SPYGLASS, ClassType.SNIPER, BuffEffect.HEADSHOT_DMG, 10, 6));

        register(new ArcadeBuff("buff_snp_lethal", "Tir Létal",
            "+{value} dégâts à distance", BuffCategory.OFFENSE, BuffRarity.RARE,
            Material.SPECTRAL_ARROW, ClassType.SNIPER, BuffEffect.DAMAGE_PERCENT, 8, 4));
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient un buff par son ID
     */
    public ArcadeBuff getBuff(String id) {
        return buffsById.get(id);
    }

    /**
     * Génère 3 buffs aléatoires pour un level up
     * Prend en compte la classe du joueur et les buffs déjà obtenus
     */
    public List<ArcadeBuff> generateLevelUpChoices(ClassType playerClass, Map<String, Integer> currentBuffs) {
        List<ArcadeBuff> choices = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Random random = ThreadLocalRandom.current();

        // Poids de rareté ajusté
        int totalWeight = 0;
        for (BuffRarity rarity : BuffRarity.values()) {
            totalWeight += rarity.getWeight();
        }

        int attempts = 0;
        while (choices.size() < 3 && attempts < 50) {
            attempts++;

            // Sélectionner une rareté
            int roll = random.nextInt(totalWeight);
            BuffRarity selectedRarity = BuffRarity.COMMON;
            int cumulative = 0;
            for (BuffRarity rarity : BuffRarity.values()) {
                cumulative += rarity.getWeight();
                if (roll < cumulative) {
                    selectedRarity = rarity;
                    break;
                }
            }

            // Sélectionner un buff de cette rareté
            List<ArcadeBuff> pool = new ArrayList<>(buffsByRarity.get(selectedRarity));

            // Bonus pour les buffs de classe (50% de chance d'être préféré)
            if (random.nextDouble() < 0.5) {
                List<ArcadeBuff> classBuffs = pool.stream()
                    .filter(b -> b.getPreferredClass() == playerClass)
                    .toList();
                if (!classBuffs.isEmpty()) {
                    pool = new ArrayList<>(classBuffs);
                }
            }

            // Filtrer les buffs déjà au max ou déjà sélectionnés
            pool = pool.stream()
                .filter(b -> !usedIds.contains(b.getId()))
                .filter(b -> currentBuffs.getOrDefault(b.getId(), 0) < b.getMaxStacks())
                .toList();

            if (!pool.isEmpty()) {
                ArcadeBuff selected = pool.get(random.nextInt(pool.size()));
                choices.add(selected);
                usedIds.add(selected.getId());
            }
        }

        // Si on n'a pas assez de choix, compléter avec des buffs communs
        if (choices.size() < 3) {
            List<ArcadeBuff> fallback = allBuffs.stream()
                .filter(b -> !usedIds.contains(b.getId()))
                .filter(b -> currentBuffs.getOrDefault(b.getId(), 0) < b.getMaxStacks())
                .toList();

            while (choices.size() < 3 && !fallback.isEmpty()) {
                ArcadeBuff selected = fallback.get(random.nextInt(fallback.size()));
                choices.add(selected);
                usedIds.add(selected.getId());
                fallback = fallback.stream()
                    .filter(b -> !usedIds.contains(b.getId()))
                    .toList();
            }
        }

        return choices;
    }

    /**
     * Calcule le bonus total d'un effet spécifique
     */
    public double calculateTotalBonus(Map<String, Integer> currentBuffs, ArcadeBuff.BuffEffect effectType) {
        double total = 0;

        for (Map.Entry<String, Integer> entry : currentBuffs.entrySet()) {
            ArcadeBuff buff = getBuff(entry.getKey());
            if (buff != null && buff.getEffectType() == effectType) {
                total += buff.getValueAtStacks(entry.getValue());
            }
        }

        return total;
    }

    /**
     * Obtient tous les buffs actifs regroupés par catégorie
     */
    public Map<BuffCategory, List<Map.Entry<ArcadeBuff, Integer>>> getBuffsByCategory(Map<String, Integer> currentBuffs) {
        Map<BuffCategory, List<Map.Entry<ArcadeBuff, Integer>>> result = new EnumMap<>(BuffCategory.class);

        for (BuffCategory category : BuffCategory.values()) {
            result.put(category, new ArrayList<>());
        }

        for (Map.Entry<String, Integer> entry : currentBuffs.entrySet()) {
            ArcadeBuff buff = getBuff(entry.getKey());
            if (buff != null && entry.getValue() > 0) {
                result.get(buff.getCategory()).add(Map.entry(buff, entry.getValue()));
            }
        }

        return result;
    }

    /**
     * Obtient le nombre total de buffs collectés
     */
    public int getTotalBuffCount(Map<String, Integer> currentBuffs) {
        return currentBuffs.values().stream().mapToInt(Integer::intValue).sum();
    }
}
