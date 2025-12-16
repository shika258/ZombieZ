package com.rinaorc.zombiez.classes.buffs;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.StatCalculator;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffCategory;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffEffect;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffRarity;
import lombok.Getter;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Registre des buffs arcade simplifié
 * À chaque level up de classe, 3 buffs sont proposés et le joueur en choisit 1.
 *
 * Pool réduit mais plus impactant:
 * - 15 buffs universels (accessible à tous)
 * - 3 buffs par classe (bonus de chance d'apparition)
 */
@Getter
public class ArcadeBuffRegistry {

    private final List<ArcadeBuff> allBuffs;
    private final Map<String, ArcadeBuff> buffsById;
    private final Map<BuffRarity, List<ArcadeBuff>> buffsByRarity;
    private final Map<ClassType, List<ArcadeBuff>> buffsByClass;

    public ArcadeBuffRegistry() {
        this.allBuffs = new ArrayList<>();
        this.buffsById = new LinkedHashMap<>();
        this.buffsByRarity = new EnumMap<>(BuffRarity.class);
        this.buffsByClass = new EnumMap<>(ClassType.class);

        for (BuffRarity rarity : BuffRarity.values()) {
            buffsByRarity.put(rarity, new ArrayList<>());
        }
        for (ClassType type : ClassType.values()) {
            buffsByClass.put(type, new ArrayList<>());
        }

        registerAllBuffs();
    }

    private void register(ArcadeBuff buff) {
        allBuffs.add(buff);
        buffsById.put(buff.getId(), buff);
        buffsByRarity.get(buff.getRarity()).add(buff);
        if (buff.getPreferredClass() != null) {
            buffsByClass.get(buff.getPreferredClass()).add(buff);
        }
    }

    private void registerAllBuffs() {
        registerUniversalBuffs();
        registerClassBuffs();
    }

    // ==================== BUFFS UNIVERSELS ====================
    private void registerUniversalBuffs() {

        // === OFFENSIFS (Communs) ===
        register(new ArcadeBuff("dmg_up", "Puissance",
            "+{value} dégâts",
            BuffCategory.OFFENSE, BuffRarity.COMMON, Material.IRON_SWORD,
            null, BuffEffect.DAMAGE, 3, 10));

        register(new ArcadeBuff("crit_chance", "Oeil Aiguisé",
            "+{value} chance de critique",
            BuffCategory.OFFENSE, BuffRarity.COMMON, Material.SPIDER_EYE,
            null, BuffEffect.CRIT_CHANCE, 2, 10));

        register(new ArcadeBuff("crit_dmg", "Coup Fatal",
            "+{value} dégâts critiques",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.DIAMOND,
            null, BuffEffect.CRIT_DAMAGE, 5, 8));

        register(new ArcadeBuff("lifesteal", "Vampirisme",
            "+{value} vol de vie",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.GHAST_TEAR,
            null, BuffEffect.LIFESTEAL, 2, 5));

        // === DÉFENSIFS (Communs) ===
        register(new ArcadeBuff("hp_up", "Vitalité",
            "+{value} points de vie max",
            BuffCategory.DEFENSE, BuffRarity.COMMON, Material.APPLE,
            null, BuffEffect.HEALTH, 5, 10));

        register(new ArcadeBuff("armor_up", "Endurance",
            "+{value} réduction de dégâts",
            BuffCategory.DEFENSE, BuffRarity.COMMON, Material.IRON_CHESTPLATE,
            null, BuffEffect.ARMOR, 2, 10));

        register(new ArcadeBuff("regen_up", "Régénération",
            "+{value} régénération",
            BuffCategory.DEFENSE, BuffRarity.UNCOMMON, Material.GOLDEN_APPLE,
            null, BuffEffect.REGEN, 5, 8));

        // === UTILITAIRES (Communs) ===
        register(new ArcadeBuff("speed_up", "Pied Léger",
            "+{value} vitesse",
            BuffCategory.UTILITY, BuffRarity.COMMON, Material.FEATHER,
            null, BuffEffect.SPEED, 2, 10));

        register(new ArcadeBuff("xp_up", "Apprentissage",
            "+{value} XP gagné",
            BuffCategory.UTILITY, BuffRarity.COMMON, Material.EXPERIENCE_BOTTLE,
            null, BuffEffect.XP, 3, 10));

        register(new ArcadeBuff("cdr_up", "Tacticien",
            "-{value} temps de recharge",
            BuffCategory.UTILITY, BuffRarity.UNCOMMON, Material.CLOCK,
            null, BuffEffect.COOLDOWN, 3, 8));

        register(new ArcadeBuff("energy_up", "Flux Vital",
            "+{value} régén. d'énergie",
            BuffCategory.UTILITY, BuffRarity.UNCOMMON, Material.GLOWSTONE_DUST,
            null, BuffEffect.ENERGY, 5, 8));

        // === RARES (Puissants) ===
        register(new ArcadeBuff("berserker", "Rage du Berserker",
            "+{value} dégâts quand <30% HP",
            BuffCategory.OFFENSE, BuffRarity.RARE, Material.BLAZE_POWDER,
            null, BuffEffect.DAMAGE, 15, 3));

        register(new ArcadeBuff("tank", "Forteresse",
            "+{value} HP et armure",
            BuffCategory.DEFENSE, BuffRarity.RARE, Material.NETHERITE_CHESTPLATE,
            null, BuffEffect.HEALTH, 10, 3));

        register(new ArcadeBuff("assassin", "Assassin",
            "+{value} critique et critique damage",
            BuffCategory.OFFENSE, BuffRarity.RARE, Material.WITHER_SKELETON_SKULL,
            null, BuffEffect.CRIT_CHANCE, 5, 3));

        register(new ArcadeBuff("survivor", "Survivant",
            "+{value} vol de vie et régén",
            BuffCategory.DEFENSE, BuffRarity.RARE, Material.TOTEM_OF_UNDYING,
            null, BuffEffect.LIFESTEAL, 5, 3));
    }

    // ==================== BUFFS PAR CLASSE ====================
    private void registerClassBuffs() {

        // === GUERRIER ===
        register(new ArcadeBuff("gue_melee", "Maître d'Armes",
            "+{value} dégâts mêlée",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.NETHERITE_AXE,
            ClassType.GUERRIER, BuffEffect.DAMAGE, 5, 5));

        register(new ArcadeBuff("gue_tank", "Colosse",
            "+{value} HP max",
            BuffCategory.DEFENSE, BuffRarity.UNCOMMON, Material.NETHERITE_CHESTPLATE,
            ClassType.GUERRIER, BuffEffect.HEALTH, 8, 5));

        register(new ArcadeBuff("gue_sustain", "Immortel",
            "+{value} vol de vie",
            BuffCategory.DEFENSE, BuffRarity.RARE, Material.GOLDEN_APPLE,
            ClassType.GUERRIER, BuffEffect.LIFESTEAL, 4, 3));

        // === CHASSEUR ===
        register(new ArcadeBuff("cha_precision", "Tireur d'Élite",
            "+{value} chance de critique",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.TARGET,
            ClassType.CHASSEUR, BuffEffect.CRIT_CHANCE, 4, 5));

        register(new ArcadeBuff("cha_lethality", "Létalité",
            "+{value} dégâts critiques",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.SPECTRAL_ARROW,
            ClassType.CHASSEUR, BuffEffect.CRIT_DAMAGE, 8, 5));

        register(new ArcadeBuff("cha_agility", "Agilité",
            "+{value} vitesse",
            BuffCategory.UTILITY, BuffRarity.RARE, Material.RABBIT_FOOT,
            ClassType.CHASSEUR, BuffEffect.SPEED, 5, 3));

        // === OCCULTISTE ===
        register(new ArcadeBuff("occ_power", "Pouvoir Arcanique",
            "+{value} dégâts des sorts",
            BuffCategory.OFFENSE, BuffRarity.UNCOMMON, Material.AMETHYST_SHARD,
            ClassType.OCCULTISTE, BuffEffect.DAMAGE, 6, 5));

        register(new ArcadeBuff("occ_haste", "Célérité",
            "-{value} temps de recharge",
            BuffCategory.UTILITY, BuffRarity.UNCOMMON, Material.ENCHANTED_BOOK,
            ClassType.OCCULTISTE, BuffEffect.COOLDOWN, 5, 5));

        register(new ArcadeBuff("occ_mana", "Puit de Mana",
            "+{value} régén. d'énergie",
            BuffCategory.UTILITY, BuffRarity.RARE, Material.NETHER_STAR,
            ClassType.OCCULTISTE, BuffEffect.ENERGY, 10, 3));
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient un buff par son ID
     */
    public ArcadeBuff getBuff(String id) {
        return buffsById.get(id);
    }

    /**
     * Compte les stacks par catégorie
     * Utilisé pour les caps de catégorie anti-power creep
     */
    public Map<BuffCategory, Integer> getCategoryStacks(Map<String, Integer> currentBuffs) {
        Map<BuffCategory, Integer> counts = new EnumMap<>(BuffCategory.class);
        for (BuffCategory cat : BuffCategory.values()) {
            counts.put(cat, 0);
        }

        for (Map.Entry<String, Integer> entry : currentBuffs.entrySet()) {
            ArcadeBuff buff = getBuff(entry.getKey());
            if (buff != null) {
                counts.merge(buff.getCategory(), entry.getValue(), Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Vérifie si une catégorie est au cap
     */
    public boolean isCategoryAtCap(BuffCategory category, Map<String, Integer> currentBuffs) {
        int currentStacks = getCategoryStacks(currentBuffs).getOrDefault(category, 0);
        return switch (category) {
            case OFFENSE -> currentStacks >= StatCalculator.MAX_OFFENSE_BUFF_STACKS;
            case DEFENSE -> currentStacks >= StatCalculator.MAX_DEFENSE_BUFF_STACKS;
            case UTILITY -> currentStacks >= StatCalculator.MAX_UTILITY_BUFF_STACKS;
        };
    }

    /**
     * Génère 3 buffs aléatoires pour un level up
     * Respecte les caps de catégorie pour éviter le power creep
     */
    public List<ArcadeBuff> generateChoices(ClassType playerClass, Map<String, Integer> currentBuffs) {
        List<ArcadeBuff> choices = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Random random = ThreadLocalRandom.current();

        // Calculer les catégories encore disponibles
        Map<BuffCategory, Integer> categoryStacks = getCategoryStacks(currentBuffs);

        int attempts = 0;
        while (choices.size() < 3 && attempts < 30) {
            attempts++;

            // Sélectionner une rareté
            int roll = random.nextInt(100);
            BuffRarity rarity;
            if (roll < 50) rarity = BuffRarity.COMMON;
            else if (roll < 85) rarity = BuffRarity.UNCOMMON;
            else rarity = BuffRarity.RARE;

            // Pool de buffs
            List<ArcadeBuff> pool = new ArrayList<>(buffsByRarity.get(rarity));

            // 40% de chance de favoriser les buffs de classe
            if (random.nextDouble() < 0.4) {
                List<ArcadeBuff> classBuffs = buffsByClass.get(playerClass);
                if (!classBuffs.isEmpty()) {
                    ArcadeBuff classBuff = classBuffs.get(random.nextInt(classBuffs.size()));
                    if (isBuffAvailable(classBuff, usedIds, currentBuffs, categoryStacks)) {
                        choices.add(classBuff);
                        usedIds.add(classBuff.getId());
                        continue;
                    }
                }
            }

            // Filtrer les buffs non disponibles
            pool = pool.stream()
                .filter(b -> isBuffAvailable(b, usedIds, currentBuffs, categoryStacks))
                .toList();

            if (!pool.isEmpty()) {
                ArcadeBuff selected = pool.get(random.nextInt(pool.size()));
                choices.add(selected);
                usedIds.add(selected.getId());
            }
        }

        // Compléter si nécessaire
        if (choices.size() < 3) {
            List<ArcadeBuff> fallback = allBuffs.stream()
                .filter(b -> isBuffAvailable(b, usedIds, currentBuffs, categoryStacks))
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
     * Vérifie si un buff peut être sélectionné
     */
    private boolean isBuffAvailable(ArcadeBuff buff, Set<String> usedIds,
                                    Map<String, Integer> currentBuffs,
                                    Map<BuffCategory, Integer> categoryStacks) {
        if (usedIds.contains(buff.getId())) return false;
        if (currentBuffs.getOrDefault(buff.getId(), 0) >= buff.getMaxStacks()) return false;

        // Vérifier le cap de catégorie
        int catCap = switch (buff.getCategory()) {
            case OFFENSE -> StatCalculator.MAX_OFFENSE_BUFF_STACKS;
            case DEFENSE -> StatCalculator.MAX_DEFENSE_BUFF_STACKS;
            case UTILITY -> StatCalculator.MAX_UTILITY_BUFF_STACKS;
        };

        return categoryStacks.getOrDefault(buff.getCategory(), 0) < catCap;
    }

    /**
     * Calcule le bonus total d'un effet spécifique
     * Gère les alias (ARMOR = DAMAGE_REDUCTION)
     */
    public double getTotalBonus(Map<String, Integer> currentBuffs, BuffEffect effect) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : currentBuffs.entrySet()) {
            ArcadeBuff buff = getBuff(entry.getKey());
            if (buff != null && matchesEffect(buff.getEffect(), effect)) {
                total += buff.getTotalValue(entry.getValue());
            }
        }
        return total;
    }

    /**
     * Vérifie si un effet correspond (avec gestion des alias)
     */
    private boolean matchesEffect(BuffEffect buffEffect, BuffEffect requestedEffect) {
        if (buffEffect == requestedEffect) return true;
        // ARMOR et DAMAGE_REDUCTION sont équivalents
        if ((buffEffect == BuffEffect.ARMOR && requestedEffect == BuffEffect.DAMAGE_REDUCTION) ||
            (buffEffect == BuffEffect.DAMAGE_REDUCTION && requestedEffect == BuffEffect.ARMOR)) {
            return true;
        }
        return false;
    }

    /**
     * Obtient le nombre total de buffs collectés
     */
    public int getTotalBuffCount(Map<String, Integer> currentBuffs) {
        return currentBuffs.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Génère un résumé des caps pour l'affichage
     */
    public List<String> getCategoryCapInfo(Map<String, Integer> currentBuffs) {
        Map<BuffCategory, Integer> stacks = getCategoryStacks(currentBuffs);
        List<String> info = new ArrayList<>();

        info.add("§c⚔ Offensif: §f" + stacks.get(BuffCategory.OFFENSE) + "/" + StatCalculator.MAX_OFFENSE_BUFF_STACKS);
        info.add("§6⛨ Défensif: §f" + stacks.get(BuffCategory.DEFENSE) + "/" + StatCalculator.MAX_DEFENSE_BUFF_STACKS);
        info.add("§b✧ Utilitaire: §f" + stacks.get(BuffCategory.UTILITY) + "/" + StatCalculator.MAX_UTILITY_BUFF_STACKS);

        return info;
    }
}
