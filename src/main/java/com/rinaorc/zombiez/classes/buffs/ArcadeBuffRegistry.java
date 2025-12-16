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
     * Génère 3 buffs aléatoires pour un level up
     */
    public List<ArcadeBuff> generateChoices(ClassType playerClass, Map<String, Integer> currentBuffs) {
        List<ArcadeBuff> choices = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Random random = ThreadLocalRandom.current();

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
                    if (!usedIds.contains(classBuff.getId()) &&
                        currentBuffs.getOrDefault(classBuff.getId(), 0) < classBuff.getMaxStacks()) {
                        choices.add(classBuff);
                        usedIds.add(classBuff.getId());
                        continue;
                    }
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

        // Compléter si nécessaire
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
    public double getTotalBonus(Map<String, Integer> currentBuffs, BuffEffect effect) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : currentBuffs.entrySet()) {
            ArcadeBuff buff = getBuff(entry.getKey());
            if (buff != null && buff.getEffect() == effect) {
                total += buff.getTotalValue(entry.getValue());
            }
        }
        return total;
    }

    /**
     * Obtient le nombre total de buffs collectés
     */
    public int getTotalBuffCount(Map<String, Integer> currentBuffs) {
        return currentBuffs.values().stream().mapToInt(Integer::intValue).sum();
    }
}
