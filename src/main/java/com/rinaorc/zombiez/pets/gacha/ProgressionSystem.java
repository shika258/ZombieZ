package com.rinaorc.zombiez.pets.gacha;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.eggs.EggType;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de progression alternatif pour les Pets
 * Wild Cards, récompenses de gameplay, milestones
 *
 * OBJECTIF: Offrir des chemins de progression multiples pour éviter la frustration
 * - Les joueurs actifs progressent via le gameplay
 * - Les joueurs patients progressent via les daily
 * - Les joueurs "payants" progressent plus vite mais pas exclusivement
 */
public class ProgressionSystem {

    private final ZombieZPlugin plugin;

    // Tracking des achievements par joueur
    private final Map<UUID, PlayerProgressionData> playerProgress = new ConcurrentHashMap<>();

    // ==================== MILESTONES ====================
    // Récompenses pour atteindre des objectifs

    @Getter
    private static final List<Milestone> MILESTONES = Arrays.asList(
        // Collection milestones
        new Milestone("collect_5", "Premier Collectionneur", "Collecter 5 pets différents",
            MilestoneType.COLLECTION, 5, new MilestoneReward(EggType.ZONE, 2, 100)),
        new Milestone("collect_15", "Collectionneur Amateur", "Collecter 15 pets différents",
            MilestoneType.COLLECTION, 15, new MilestoneReward(EggType.ELITE, 1, 300)),
        new Milestone("collect_30", "Collectionneur Confirmé", "Collecter 30 pets différents",
            MilestoneType.COLLECTION, 30, new MilestoneReward(EggType.ELITE, 3, 500)),
        new Milestone("collect_all", "Maître Collectionneur", "Collecter TOUS les pets",
            MilestoneType.COLLECTION, 50, new MilestoneReward(EggType.LEGENDARY, 2, 2000)),

        // Level milestones
        new Milestone("maxlevel_1", "Premier Max", "Monter un pet au niveau max",
            MilestoneType.MAX_LEVEL, 1, new MilestoneReward(EggType.ZONE, 3, 200)),
        new Milestone("maxlevel_5", "Éleveur Dévoué", "Monter 5 pets au niveau max",
            MilestoneType.MAX_LEVEL, 5, new MilestoneReward(EggType.ELITE, 2, 500)),
        new Milestone("maxlevel_15", "Maître Éleveur", "Monter 15 pets au niveau max",
            MilestoneType.MAX_LEVEL, 15, new MilestoneReward(EggType.LEGENDARY, 1, 1000)),

        // Rarity milestones
        new Milestone("rare_5", "Chasseur de Raretés", "Obtenir 5 pets Rares ou mieux",
            MilestoneType.RARITY_COUNT, 5, new MilestoneReward(EggType.ZONE, 2, 150)),
        new Milestone("epic_3", "Épique Aventure", "Obtenir 3 pets Épiques ou mieux",
            MilestoneType.EPIC_COUNT, 3, new MilestoneReward(EggType.ELITE, 2, 300)),
        new Milestone("legendary_1", "Premier Légendaire!", "Obtenir votre premier Légendaire",
            MilestoneType.LEGENDARY_COUNT, 1, new MilestoneReward(null, 0, 500)),
        new Milestone("legendary_5", "Collectionneur Légendaire", "Obtenir 5 pets Légendaires",
            MilestoneType.LEGENDARY_COUNT, 5, new MilestoneReward(EggType.LEGENDARY, 1, 1000)),
        new Milestone("mythic_1", "L'Impossible!", "Obtenir votre premier Mythique",
            MilestoneType.MYTHIC_COUNT, 1, new MilestoneReward(EggType.LEGENDARY, 2, 2000)),

        // Exalted milestones
        new Milestone("exalted_1", "L'Ultime!", "Obtenir votre premier Exalté",
            MilestoneType.EXALTED_COUNT, 1, new MilestoneReward(EggType.MYTHIC, 1, 3000)),
        new Milestone("exalted_3", "Collectionneur Exalté", "Obtenir 3 pets Exaltés",
            MilestoneType.EXALTED_COUNT, 3, new MilestoneReward(EggType.MYTHIC, 2, 5000)),

        // Eggs opened milestones
        new Milestone("eggs_50", "Ouvreur Novice", "Ouvrir 50 oeufs",
            MilestoneType.EGGS_OPENED, 50, new MilestoneReward(EggType.STANDARD, 5, 100)),
        new Milestone("eggs_200", "Ouvreur Confirmé", "Ouvrir 200 oeufs",
            MilestoneType.EGGS_OPENED, 200, new MilestoneReward(EggType.ZONE, 3, 300)),
        new Milestone("eggs_500", "Ouvreur Expert", "Ouvrir 500 oeufs",
            MilestoneType.EGGS_OPENED, 500, new MilestoneReward(EggType.ELITE, 2, 500)),
        new Milestone("eggs_1000", "Ouvreur Légendaire", "Ouvrir 1000 oeufs",
            MilestoneType.EGGS_OPENED, 1000, new MilestoneReward(EggType.LEGENDARY, 1, 1000)),

        // Daily streak milestones
        new Milestone("streak_7", "Une Semaine!", "Atteindre 7 jours de streak",
            MilestoneType.DAILY_STREAK, 7, new MilestoneReward(EggType.ZONE, 2, 150)),
        new Milestone("streak_30", "Un Mois!", "Atteindre 30 jours de streak",
            MilestoneType.DAILY_STREAK, 30, new MilestoneReward(EggType.ELITE, 3, 500)),
        new Milestone("streak_100", "Dévouement Total", "Atteindre 100 jours de streak",
            MilestoneType.DAILY_STREAK, 100, new MilestoneReward(EggType.LEGENDARY, 2, 2000))
    );

    // ==================== GAMEPLAY REWARDS ====================
    // Récompenses pour les actions in-game

    @Getter
    private static final Map<GameplayAction, GameplayReward> GAMEPLAY_REWARDS = Map.of(
        // Zone completion
        GameplayAction.ZONE_CLEAR, new GameplayReward(
            "Zone complétée", 0.15, EggType.STANDARD, 10, 0.05, EggType.ZONE
        ),
        GameplayAction.ZONE_CLEAR_PERFECT, new GameplayReward(
            "Zone parfaite (sans mort)", 0.30, EggType.ZONE, 25, 0.10, EggType.ELITE
        ),

        // Boss kills
        GameplayAction.BOSS_KILL, new GameplayReward(
            "Boss tué", 0.20, EggType.ZONE, 20, 0.08, EggType.ELITE
        ),
        GameplayAction.ELITE_BOSS_KILL, new GameplayReward(
            "Boss élite tué", 0.35, EggType.ELITE, 50, 0.15, EggType.LEGENDARY
        ),

        // PvE achievements
        GameplayAction.ZOMBIES_100, new GameplayReward(
            "100 zombies tués (session)", 0.10, EggType.STANDARD, 5, 0, null
        ),
        GameplayAction.ZOMBIES_500, new GameplayReward(
            "500 zombies tués (session)", 0.25, EggType.ZONE, 15, 0.05, EggType.ELITE
        ),

        // Special events
        GameplayAction.EVENT_PARTICIPATION, new GameplayReward(
            "Participation événement", 0.50, EggType.ZONE, 30, 0.20, EggType.ELITE
        ),
        GameplayAction.EVENT_TOP_10, new GameplayReward(
            "Top 10 événement", 1.0, EggType.ELITE, 100, 0.50, EggType.LEGENDARY
        )
    );

    public ProgressionSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== WILD CARD SYSTEM ====================

    /**
     * Achète une copie d'un pet spécifique avec des fragments (Wild Card)
     * Permet de cibler un pet précis au lieu de dépendre de la RNG
     */
    public WildCardResult buyWildCard(Player player, PetType targetPet) {
        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (petData == null) {
            return new WildCardResult(false, "Données joueur introuvables!", 0);
        }

        // Vérifier que le joueur possède déjà ce pet (wild card = duplicata ciblé)
        if (!petData.hasPet(targetPet)) {
            return new WildCardResult(false,
                "§cVous devez d'abord obtenir ce pet avant de pouvoir acheter des copies!", 0);
        }

        // Vérifier le coût
        int cost = targetPet.getRarity().getFragmentCost();
        if (cost <= 0) {
            return new WildCardResult(false,
                "§cCe pet ne peut pas être acheté avec des Wild Cards!", 0);
        }

        if (!petData.hasFragments(cost)) {
            return new WildCardResult(false,
                "§cPas assez de fragments! (Besoin: §e" + cost + "§c, Vous avez: §e" + petData.getFragments() + "§c)", 0);
        }

        // Effectuer l'achat
        PetData pet = petData.getPet(targetPet);
        if (pet == null) {
            return new WildCardResult(false, "§cErreur: données du pet introuvables!", 0);
        }

        petData.removeFragments(cost);
        pet.addCopies(1);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        return new WildCardResult(true,
            "§a+1 copie de " + targetPet.getColoredName() + "§a!\n§7Copies totales: §f" + pet.getCopies(),
            pet.getCopies());
    }

    /**
     * Calcule le nombre de wild cards nécessaires pour max un pet
     */
    public int getWildCardsToMax(PetType pet, int currentCopies) {
        int needed = pet.getRarity().getCopiesForMax() - currentCopies;
        return Math.max(0, needed);
    }

    // ==================== MILESTONE SYSTEM ====================

    /**
     * Vérifie et réclame les milestones débloquées
     */
    public List<Milestone> checkAndClaimMilestones(Player player) {
        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (petData == null) return Collections.emptyList();

        PlayerProgressionData progress = playerProgress.computeIfAbsent(
            player.getUniqueId(), k -> new PlayerProgressionData()
        );

        List<Milestone> claimed = new ArrayList<>();

        for (Milestone milestone : MILESTONES) {
            // Déjà réclamé?
            if (progress.claimedMilestones.contains(milestone.id())) continue;

            // Vérifier si atteint
            boolean achieved = switch (milestone.type()) {
                case COLLECTION -> petData.getPetCount() >= milestone.target();
                case MAX_LEVEL -> petData.getMaxLevelPetCount() >= milestone.target();
                case RARITY_COUNT -> countPetsOfRarityOrBetter(petData, PetRarity.RARE) >= milestone.target();
                case EPIC_COUNT -> countPetsOfRarityOrBetter(petData, PetRarity.EPIC) >= milestone.target();
                case LEGENDARY_COUNT -> petData.getLegendariesObtained() >= milestone.target();
                case MYTHIC_COUNT -> petData.getMythicsObtained() >= milestone.target();
                case EXALTED_COUNT -> petData.getExaltedObtained() >= milestone.target();
                case EGGS_OPENED -> petData.getTotalEggsOpened() >= milestone.target();
                case DAILY_STREAK -> {
                    if (plugin.getDailyRewardManager() != null) {
                        yield plugin.getDailyRewardManager().getStreak(player) >= milestone.target();
                    }
                    yield false;
                }
            };

            if (achieved) {
                // Donner les récompenses
                MilestoneReward reward = milestone.reward();
                if (reward.eggType() != null && reward.eggCount() > 0) {
                    petData.addEggs(reward.eggType(), reward.eggCount());
                }
                if (reward.fragments() > 0) {
                    petData.addFragments(reward.fragments());
                }

                progress.claimedMilestones.add(milestone.id());
                claimed.add(milestone);
            }
        }

        return claimed;
    }

    private int countPetsOfRarityOrBetter(PlayerPetData petData, PetRarity minRarity) {
        return (int) petData.getAllPets().stream()
            .filter(p -> p.getType().getRarity().isAtLeast(minRarity))
            .count();
    }

    /**
     * Obtient la progression d'un milestone
     */
    public MilestoneProgress getMilestoneProgress(Player player, Milestone milestone) {
        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (petData == null) return new MilestoneProgress(0, milestone.target(), false);

        PlayerProgressionData progress = playerProgress.get(player.getUniqueId());
        boolean claimed = progress != null && progress.claimedMilestones.contains(milestone.id());

        int current = switch (milestone.type()) {
            case COLLECTION -> petData.getPetCount();
            case MAX_LEVEL -> petData.getMaxLevelPetCount();
            case RARITY_COUNT -> countPetsOfRarityOrBetter(petData, PetRarity.RARE);
            case EPIC_COUNT -> countPetsOfRarityOrBetter(petData, PetRarity.EPIC);
            case LEGENDARY_COUNT -> petData.getLegendariesObtained();
            case MYTHIC_COUNT -> petData.getMythicsObtained();
            case EXALTED_COUNT -> petData.getExaltedObtained();
            case EGGS_OPENED -> petData.getTotalEggsOpened();
            case DAILY_STREAK -> plugin.getDailyRewardManager() != null ?
                plugin.getDailyRewardManager().getStreak(player) : 0;
        };

        return new MilestoneProgress(current, milestone.target(), claimed);
    }

    // ==================== GAMEPLAY REWARDS ====================

    /**
     * Traite une action de gameplay et donne potentiellement des récompenses
     */
    public GameplayRewardResult processGameplayAction(Player player, GameplayAction action) {
        GameplayReward reward = GAMEPLAY_REWARDS.get(action);
        if (reward == null) return new GameplayRewardResult(false, null, null);

        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (petData == null) return new GameplayRewardResult(false, null, null);

        // Roll pour l'oeuf principal
        EggType eggWon = null;
        if (Math.random() < reward.eggChance()) {
            eggWon = reward.eggType();
            petData.addEggs(eggWon, 1);
        }

        // Fragments garantis
        petData.addFragments(reward.fragments());

        // Roll pour l'oeuf bonus (rare)
        EggType bonusEgg = null;
        if (reward.bonusEggType() != null && Math.random() < reward.bonusChance()) {
            bonusEgg = reward.bonusEggType();
            petData.addEggs(bonusEgg, 1);
        }

        // Sons
        if (eggWon != null || bonusEgg != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        }

        return new GameplayRewardResult(true, eggWon, bonusEgg);
    }

    // ==================== DATA MANAGEMENT ====================

    public void loadPlayerProgress(UUID uuid, Set<String> claimedMilestones) {
        PlayerProgressionData data = new PlayerProgressionData();
        data.claimedMilestones.addAll(claimedMilestones);
        playerProgress.put(uuid, data);
    }

    public Set<String> getClaimedMilestones(UUID uuid) {
        PlayerProgressionData data = playerProgress.get(uuid);
        return data != null ? new HashSet<>(data.claimedMilestones) : new HashSet<>();
    }

    // ==================== INNER CLASSES ====================

    private static class PlayerProgressionData {
        final Set<String> claimedMilestones = ConcurrentHashMap.newKeySet();
    }

    public enum MilestoneType {
        COLLECTION,      // Nombre de pets différents
        MAX_LEVEL,       // Nombre de pets niveau max
        RARITY_COUNT,    // Nombre de pets rare+
        EPIC_COUNT,      // Nombre de pets epic+
        LEGENDARY_COUNT, // Nombre de légendaires
        MYTHIC_COUNT,    // Nombre de mythiques
        EXALTED_COUNT,   // Nombre d'exaltés
        EGGS_OPENED,     // Oeufs ouverts
        DAILY_STREAK     // Jours de streak
    }

    public enum GameplayAction {
        ZONE_CLEAR,
        ZONE_CLEAR_PERFECT,
        BOSS_KILL,
        ELITE_BOSS_KILL,
        ZOMBIES_100,
        ZOMBIES_500,
        EVENT_PARTICIPATION,
        EVENT_TOP_10
    }

    public record Milestone(
        String id,
        String name,
        String description,
        MilestoneType type,
        int target,
        MilestoneReward reward
    ) {}

    public record MilestoneReward(
        EggType eggType,
        int eggCount,
        int fragments
    ) {}

    public record MilestoneProgress(
        int current,
        int target,
        boolean claimed
    ) {}

    public record GameplayReward(
        String description,
        double eggChance,      // Chance d'obtenir l'oeuf principal
        EggType eggType,       // Type d'oeuf principal
        int fragments,         // Fragments garantis
        double bonusChance,    // Chance d'obtenir un oeuf bonus
        EggType bonusEggType   // Type d'oeuf bonus (plus rare)
    ) {}

    public record GameplayRewardResult(
        boolean success,
        EggType eggWon,
        EggType bonusEggWon
    ) {}

    public record WildCardResult(
        boolean success,
        String message,
        int newCopyCount
    ) {}
}
