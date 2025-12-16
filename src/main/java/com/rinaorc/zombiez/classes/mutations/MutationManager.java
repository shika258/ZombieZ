package com.rinaorc.zombiez.classes.mutations;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.mutations.DailyMutation.MutationEffect;
import com.rinaorc.zombiez.classes.mutations.DailyMutation.MutationRarity;
import com.rinaorc.zombiez.classes.mutations.DailyMutation.MutationType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gestionnaire des mutations quotidiennes
 * Chaque jour, 3 mutations sont activées pour tous les joueurs
 * Cela crée une expérience de jeu unique chaque jour
 */
@Getter
public class MutationManager {

    private final ZombieZPlugin plugin;
    private final List<DailyMutation> allMutations;
    private final Map<String, DailyMutation> mutationsById;
    private final Map<MutationRarity, List<DailyMutation>> mutationsByRarity;

    // Mutations actives du jour
    private List<DailyMutation> activeMutations;
    private LocalDate mutationDate;

    // Cache des modificateurs actifs
    private final Map<MutationEffect, Double> activeModifiers;

    public MutationManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.allMutations = new ArrayList<>();
        this.mutationsById = new LinkedHashMap<>();
        this.mutationsByRarity = new EnumMap<>(MutationRarity.class);
        this.activeMutations = new ArrayList<>();
        this.activeModifiers = new EnumMap<>(MutationEffect.class);

        registerAllMutations();
        organizeMutations();
        refreshDailyMutations();
    }

    private void register(DailyMutation mutation) {
        allMutations.add(mutation);
        mutationsById.put(mutation.getId(), mutation);
    }

    private void organizeMutations() {
        for (MutationRarity rarity : MutationRarity.values()) {
            mutationsByRarity.put(rarity, new ArrayList<>());
        }
        for (DailyMutation mutation : allMutations) {
            mutationsByRarity.get(mutation.getRarity()).add(mutation);
        }
    }

    /**
     * Rafraîchit les mutations quotidiennes
     * Appelé au démarrage et à minuit
     */
    public void refreshDailyMutations() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // Si c'est un nouveau jour, générer de nouvelles mutations
        if (mutationDate == null || !mutationDate.equals(today)) {
            mutationDate = today;
            generateDailyMutations();
            calculateActiveModifiers();
            broadcastMutations();
        }
    }

    /**
     * Génère 3 mutations pour la journée
     * Utilise la date comme seed pour être déterministe
     */
    private void generateDailyMutations() {
        activeMutations.clear();

        // Seed basé sur la date pour être déterministe
        long seed = mutationDate.toEpochDay();
        Random random = new Random(seed);

        Set<String> usedIds = new HashSet<>();
        Set<MutationType> usedTypes = new HashSet<>();

        // Poids totaux
        int totalWeight = 0;
        for (MutationRarity rarity : MutationRarity.values()) {
            totalWeight += rarity.getWeight();
        }

        // Générer 3 mutations de types différents
        int attempts = 0;
        while (activeMutations.size() < 3 && attempts < 100) {
            attempts++;

            // Sélectionner une rareté
            int roll = random.nextInt(totalWeight);
            MutationRarity selectedRarity = MutationRarity.COMMON;
            int cumulative = 0;
            for (MutationRarity rarity : MutationRarity.values()) {
                cumulative += rarity.getWeight();
                if (roll < cumulative) {
                    selectedRarity = rarity;
                    break;
                }
            }

            // Sélectionner une mutation
            List<DailyMutation> pool = mutationsByRarity.get(selectedRarity).stream()
                .filter(m -> !usedIds.contains(m.getId()))
                .filter(m -> !usedTypes.contains(m.getType()))
                .toList();

            if (!pool.isEmpty()) {
                DailyMutation selected = pool.get(random.nextInt(pool.size()));
                activeMutations.add(selected);
                usedIds.add(selected.getId());
                usedTypes.add(selected.getType());
            }
        }

        plugin.getLogger().info("[Mutations] Mutations du jour générées: " +
            activeMutations.stream().map(DailyMutation::getName).toList());
    }

    /**
     * Calcule les modificateurs actifs basés sur les mutations
     */
    private void calculateActiveModifiers() {
        activeModifiers.clear();

        for (DailyMutation mutation : activeMutations) {
            if (mutation.getPositiveEffect() != null) {
                activeModifiers.merge(mutation.getPositiveEffect(),
                    mutation.getPositiveValue(), Double::sum);
            }
            if (mutation.getNegativeEffect() != null) {
                activeModifiers.merge(mutation.getNegativeEffect(),
                    mutation.getNegativeValue(), Double::sum);
            }
        }
    }

    /**
     * Annonce les mutations à tous les joueurs connectés
     */
    private void broadcastMutations() {
        if (activeMutations.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§6§l✦ MUTATIONS DU JOUR ✦");
            Bukkit.broadcastMessage("§7Les règles ont changé pour aujourd'hui!");
            Bukkit.broadcastMessage("");

            for (DailyMutation mutation : activeMutations) {
                Bukkit.broadcastMessage(mutation.getRarity().getColor() + "§l" +
                    mutation.getName() + " §7- " + mutation.getDescription());
            }

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8Utilisez /mutations pour plus de détails");
            Bukkit.broadcastMessage("");
        });
    }

    /**
     * Obtient le modificateur actif pour un effet donné
     */
    public double getModifier(MutationEffect effect) {
        return activeModifiers.getOrDefault(effect, 0.0);
    }

    /**
     * Obtient le multiplicateur (1.0 + modificateur/100)
     */
    public double getMultiplier(MutationEffect effect) {
        return 1.0 + (getModifier(effect) / 100.0);
    }

    /**
     * Vérifie si une mutation spécifique est active
     */
    public boolean isMutationActive(String mutationId) {
        return activeMutations.stream().anyMatch(m -> m.getId().equals(mutationId));
    }

    // ==================== ENREGISTREMENT DES MUTATIONS ====================

    private void registerAllMutations() {
        // ==================== COMBAT (Communes) ====================

        register(new DailyMutation("mut_glass_cannon", "Canon de Verre",
            "Dégâts augmentés, mais vous êtes plus fragile",
            MutationType.COMBAT, MutationRarity.COMMON, Material.GLASS,
            MutationEffect.PLAYER_DAMAGE, 30,
            MutationEffect.PLAYER_HEALTH, -20,
            false, true, false));

        register(new DailyMutation("mut_tank_mode", "Mode Tank",
            "Plus résistant mais moins de dégâts",
            MutationType.COMBAT, MutationRarity.COMMON, Material.IRON_CHESTPLATE,
            MutationEffect.PLAYER_HEALTH, 30,
            MutationEffect.PLAYER_DAMAGE, -15,
            false, true, false));

        register(new DailyMutation("mut_berserker_day", "Jour du Berserker",
            "Mêlée surpuissante, distance affaiblie",
            MutationType.COMBAT, MutationRarity.COMMON, Material.NETHERITE_AXE,
            MutationEffect.MELEE_DAMAGE, 40,
            MutationEffect.RANGED_DAMAGE, -25,
            false, true, false));

        register(new DailyMutation("mut_marksman_day", "Jour du Tireur",
            "Distance surpuissante, mêlée affaiblie",
            MutationType.COMBAT, MutationRarity.COMMON, Material.BOW,
            MutationEffect.RANGED_DAMAGE, 40,
            MutationEffect.MELEE_DAMAGE, -25,
            false, true, false));

        // ==================== COMBAT (Peu Communes) ====================

        register(new DailyMutation("mut_headhunter", "Chasseur de Têtes",
            "Headshots dévastateurs, mais les zombies sont plus résistants",
            MutationType.COMBAT, MutationRarity.UNCOMMON, Material.SKELETON_SKULL,
            MutationEffect.HEADSHOT_DAMAGE, 100,
            MutationEffect.ZOMBIE_HEALTH, 25,
            true, true, false));

        register(new DailyMutation("mut_vampire", "Nuit du Vampire",
            "Vol de vie augmenté, régénération naturelle désactivée",
            MutationType.COMBAT, MutationRarity.UNCOMMON, Material.GHAST_TEAR,
            MutationEffect.PLAYER_LIFESTEAL, 50,
            MutationEffect.PLAYER_REGEN, -100,
            false, true, false));

        register(new DailyMutation("mut_rapid_fire", "Tir Rapide",
            "Attaques plus rapides, mais moins précises",
            MutationType.COMBAT, MutationRarity.UNCOMMON, Material.SUGAR,
            MutationEffect.PLAYER_COOLDOWN, 30,
            MutationEffect.PLAYER_CRIT, -20,
            false, true, false));

        // ==================== SURVIE (Communes) ====================

        register(new DailyMutation("mut_famine", "Famine",
            "Moins de soins reçus, mais plus d'XP",
            MutationType.SURVIVAL, MutationRarity.COMMON, Material.ROTTEN_FLESH,
            MutationEffect.XP_GAIN, 30,
            MutationEffect.HEALING_RECEIVED, -30,
            false, true, false));

        register(new DailyMutation("mut_prosperity", "Prospérité",
            "Plus de points et loot, zombies plus forts",
            MutationType.SURVIVAL, MutationRarity.COMMON, Material.GOLD_INGOT,
            MutationEffect.POINTS_GAIN, 40,
            MutationEffect.ZOMBIE_DAMAGE, 20,
            true, false, true));

        register(new DailyMutation("mut_regeneration", "Régénération Accélérée",
            "Régénération boostée, moins de vol de vie",
            MutationType.SURVIVAL, MutationRarity.COMMON, Material.GLISTERING_MELON_SLICE,
            MutationEffect.PLAYER_REGEN, 50,
            MutationEffect.PLAYER_LIFESTEAL, -30,
            false, true, false));

        // ==================== SURVIE (Peu Communes) ====================

        register(new DailyMutation("mut_hard_mode", "Mode Difficile",
            "Zombies renforcés, récompenses doublées",
            MutationType.SURVIVAL, MutationRarity.UNCOMMON, Material.WITHER_SKELETON_SKULL,
            MutationEffect.LOOT_RARITY, 50,
            MutationEffect.ZOMBIE_HEALTH, 40,
            true, false, true));

        register(new DailyMutation("mut_elite_invasion", "Invasion d'Élites",
            "Plus d'élites, plus de gemmes",
            MutationType.SURVIVAL, MutationRarity.UNCOMMON, Material.DIAMOND,
            MutationEffect.GEM_CHANCE, 75,
            MutationEffect.ZOMBIE_ELITE_CHANCE, 100,
            true, false, true));

        // ==================== EXPLORATION (Communes) ====================

        register(new DailyMutation("mut_speedrun", "Speedrun",
            "Vitesse augmentée, cooldowns réduits",
            MutationType.EXPLORATION, MutationRarity.COMMON, Material.FEATHER,
            MutationEffect.PLAYER_SPEED, 25,
            MutationEffect.PLAYER_COOLDOWN, 15,
            false, true, false));

        register(new DailyMutation("mut_treasure_hunter", "Chasseur de Trésors",
            "Meilleur loot, plus d'ennemis",
            MutationType.EXPLORATION, MutationRarity.COMMON, Material.CHEST,
            MutationEffect.LOOT_CHANCE, 50,
            MutationEffect.ZOMBIE_SPAWN_RATE, 30,
            true, false, true));

        // ==================== EXPLORATION (Peu Communes) ====================

        register(new DailyMutation("mut_night_vision", "Vision Nocturne",
            "Meilleure vision, compétences boostées",
            MutationType.EXPLORATION, MutationRarity.UNCOMMON, Material.ENDER_EYE,
            MutationEffect.SKILL_POWER, 30,
            MutationEffect.VISION_RANGE, 0,
            false, true, false));

        register(new DailyMutation("mut_zone_rush", "Rush de Zone",
            "XP massif, zones plus difficiles",
            MutationType.EXPLORATION, MutationRarity.UNCOMMON, Material.EXPERIENCE_BOTTLE,
            MutationEffect.XP_GAIN, 75,
            MutationEffect.ZONE_DIFFICULTY, 1,
            false, true, false));

        // ==================== CHAOS (Rares) ====================

        register(new DailyMutation("mut_double_edge", "Double Tranchant",
            "Tout fait plus de dégâts - vous ET les zombies",
            MutationType.CHAOS, MutationRarity.RARE, Material.DIAMOND_SWORD,
            MutationEffect.PLAYER_DAMAGE, 50,
            MutationEffect.ZOMBIE_DAMAGE, 50,
            true, true, false));

        register(new DailyMutation("mut_lottery", "Loterie",
            "Loot légendaire possible, mais beaucoup de déchets",
            MutationType.CHAOS, MutationRarity.RARE, Material.AMETHYST_SHARD,
            MutationEffect.LOOT_RARITY, 100,
            MutationEffect.LOOT_CHANCE, -40,
            false, false, true));

        register(new DailyMutation("mut_glass_world", "Monde de Verre",
            "Tout meurt plus vite - joueurs et zombies",
            MutationType.CHAOS, MutationRarity.RARE, Material.GLASS_PANE,
            MutationEffect.PLAYER_DAMAGE, 80,
            MutationEffect.PLAYER_HEALTH, -40,
            true, true, false));

        register(new DailyMutation("mut_bullet_hell", "Bullet Hell",
            "Spawns massifs, compétences instantanées",
            MutationType.CHAOS, MutationRarity.RARE, Material.BLAZE_POWDER,
            MutationEffect.PLAYER_COOLDOWN, 50,
            MutationEffect.ZOMBIE_SPAWN_RATE, 100,
            true, true, false));

        // ==================== BÉNÉDICTIONS (Légendaires) ====================

        register(new DailyMutation("mut_golden_age", "Âge d'Or",
            "Tout est amélioré sans contrepartie",
            MutationType.BLESSING, MutationRarity.LEGENDARY, Material.GOLD_BLOCK,
            MutationEffect.POINTS_GAIN, 50,
            null, 0,
            false, true, true));

        register(new DailyMutation("mut_power_surge", "Surge de Puissance",
            "Dégâts et compétences décuplés",
            MutationType.BLESSING, MutationRarity.LEGENDARY, Material.NETHER_STAR,
            MutationEffect.PLAYER_DAMAGE, 40,
            MutationEffect.SKILL_POWER, 40,
            false, true, false));

        register(new DailyMutation("mut_loot_paradise", "Paradis du Loot",
            "Loot exceptionnel garanti",
            MutationType.BLESSING, MutationRarity.LEGENDARY, Material.BEACON,
            MutationEffect.LOOT_CHANCE, 100,
            MutationEffect.LOOT_RARITY, 50,
            false, false, true));

        // ==================== MAUDITES (Très Rares) ====================

        register(new DailyMutation("mut_apocalypse", "Apocalypse",
            "Difficulté extrême, récompenses ultimes",
            MutationType.CHAOS, MutationRarity.CURSED, Material.DRAGON_EGG,
            MutationEffect.LOOT_RARITY, 200,
            MutationEffect.ZOMBIE_HEALTH, 100,
            true, false, true));

        register(new DailyMutation("mut_nightmare", "Cauchemar",
            "Les boss apparaissent partout, gemmes garanties",
            MutationType.CHAOS, MutationRarity.CURSED, Material.WITHER_ROSE,
            MutationEffect.GEM_CHANCE, 200,
            MutationEffect.ZOMBIE_BOSS_HEALTH, 100,
            true, false, true));
    }

    /**
     * Obtient le nombre de mutations actives
     */
    public int getActiveMutationCount() {
        return activeMutations.size();
    }

    /**
     * Obtient une mutation par ID
     */
    public DailyMutation getMutation(String id) {
        return mutationsById.get(id);
    }

    /**
     * Vérifie si aujourd'hui est un jour spécial (mutation légendaire ou maudite)
     */
    public boolean isSpecialDay() {
        return activeMutations.stream()
            .anyMatch(m -> m.getRarity() == MutationRarity.LEGENDARY ||
                          m.getRarity() == MutationRarity.CURSED);
    }

    /**
     * Obtient un résumé des mutations pour l'affichage
     */
    public List<String> getMutationSummary() {
        List<String> summary = new ArrayList<>();
        summary.add("§6§l✦ MUTATIONS ACTIVES ✦");
        summary.add("");

        for (DailyMutation mutation : activeMutations) {
            summary.add(mutation.getRarity().getColor() + "§l" + mutation.getName());
            summary.add("§7" + mutation.getDescription());

            if (mutation.getPositiveEffect() != null) {
                summary.add("  §a+ " + mutation.getPositiveEffect()
                    .getDescription(mutation.getPositiveValue()));
            }
            if (mutation.getNegativeEffect() != null) {
                summary.add("  §c- " + mutation.getNegativeEffect()
                    .getDescription(Math.abs(mutation.getNegativeValue())));
            }
            summary.add("");
        }

        return summary;
    }
}
