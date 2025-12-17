package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de Battle Pass / Pass de Saison
 * 100 niveaux avec récompenses gratuites et premium
 */
public class BattlePassManager {

    private final ZombieZPlugin plugin;
    
    // Saison actuelle
    @Getter
    private Season currentSeason;
    
    // Données des joueurs
    private final Map<UUID, PlayerBattlePass> playerPasses;
    
    // Configurations
    private static final int MAX_LEVEL = 100;
    private static final int XP_PER_LEVEL_BASE = 1000;
    private static final int XP_PER_LEVEL_INCREASE = 100;
    private static final int PREMIUM_COST_GEMS = 1000;

    public BattlePassManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.playerPasses = new ConcurrentHashMap<>();
        
        initializeCurrentSeason();
    }

    /**
     * Initialise la saison actuelle
     */
    private void initializeCurrentSeason() {
        // Saison 1 - L'Éveil des Morts
        List<BattlePassLevel> levels = new ArrayList<>();
        
        for (int i = 1; i <= MAX_LEVEL; i++) {
            levels.add(createLevel(i));
        }
        
        currentSeason = Season.builder()
            .id(1)
            .name("L'Éveil des Morts")
            .description("La première saison de ZombieZ")
            .theme("§4")
            .icon(Material.ZOMBIE_HEAD)
            .startDate(Instant.now())
            .endDate(Instant.now().plus(90, ChronoUnit.DAYS))
            .levels(levels)
            .build();
    }

    /**
     * Crée un niveau de Battle Pass
     */
    private BattlePassLevel createLevel(int level) {
        BattlePassLevel.BattlePassLevelBuilder builder = BattlePassLevel.builder()
            .level(level)
            .xpRequired(calculateXpRequired(level));
        
        // Récompenses gratuites (tous les niveaux)
        builder.freeReward(createFreeReward(level));
        
        // Récompenses premium (tous les 2-3 niveaux ou spéciales)
        if (level % 2 == 0 || level % 10 == 0 || level == MAX_LEVEL) {
            builder.premiumReward(createPremiumReward(level));
        }
        
        return builder.build();
    }

    /**
     * Calcule l'XP requis pour un niveau
     */
    private int calculateXpRequired(int level) {
        return XP_PER_LEVEL_BASE + (level - 1) * XP_PER_LEVEL_INCREASE;
    }

    /**
     * Crée une récompense gratuite
     */
    private BattlePassReward createFreeReward(int level) {
        // Points basiques
        if (level % 5 != 0) {
            int points = 100 + (level * 10);
            return BattlePassReward.builder()
                .type(RewardType.POINTS)
                .amount(points)
                .icon(Material.GOLD_INGOT)
                .name("§6" + points + " Points")
                .build();
        }
        
        // Tous les 5 niveaux: Gemmes
        if (level % 10 != 0) {
            int gems = 5 + (level / 10);
            return BattlePassReward.builder()
                .type(RewardType.GEMS)
                .amount(gems)
                .icon(Material.DIAMOND)
                .name("§d" + gems + " Gemmes")
                .build();
        }
        
        // Tous les 10 niveaux: Crate
        String crateTier = level < 30 ? "common" : (level < 60 ? "rare" : (level < 90 ? "epic" : "legendary"));
        return BattlePassReward.builder()
            .type(RewardType.CRATE)
            .stringValue(crateTier)
            .amount(1)
            .icon(Material.CHEST)
            .name("§eCoffre " + crateTier.substring(0, 1).toUpperCase() + crateTier.substring(1))
            .build();
    }

    /**
     * Crée une récompense premium
     */
    private BattlePassReward createPremiumReward(int level) {
        // Niveaux milestone (10, 20, 30...)
        if (level % 10 == 0) {
            return switch (level) {
                case 10 -> BattlePassReward.builder()
                    .type(RewardType.TITLE)
                    .stringValue("survivor")
                    .icon(Material.NAME_TAG)
                    .name("§eTitre: §7Survivant")
                    .build();
                case 20 -> BattlePassReward.builder()
                    .type(RewardType.COSMETIC)
                    .stringValue("trail_blood")
                    .icon(Material.REDSTONE)
                    .name("§cTrainée de Sang")
                    .build();
                case 30 -> BattlePassReward.builder()
                    .type(RewardType.ITEM)
                    .stringValue("weapon_rare")
                    .amount(3)
                    .icon(Material.DIAMOND_SWORD)
                    .name("§9Arme Rare Garantie")
                    .build();
                case 40 -> BattlePassReward.builder()
                    .type(RewardType.TITLE)
                    .stringValue("slayer")
                    .icon(Material.NAME_TAG)
                    .name("§6Titre: §cTueur")
                    .build();
                case 50 -> BattlePassReward.builder()
                    .type(RewardType.GEMS)
                    .amount(100)
                    .icon(Material.DIAMOND_BLOCK)
                    .name("§d100 Gemmes")
                    .build();
                case 60 -> BattlePassReward.builder()
                    .type(RewardType.COSMETIC)
                    .stringValue("aura_death")
                    .icon(Material.SOUL_LANTERN)
                    .name("§5Aura de Mort")
                    .build();
                case 70 -> BattlePassReward.builder()
                    .type(RewardType.ITEM)
                    .stringValue("weapon_epic")
                    .amount(4)
                    .icon(Material.NETHERITE_SWORD)
                    .name("§5Arme Épique Garantie")
                    .build();
                case 80 -> BattlePassReward.builder()
                    .type(RewardType.TITLE)
                    .stringValue("legend")
                    .icon(Material.NAME_TAG)
                    .name("§6Titre: §6Légende")
                    .build();
                case 90 -> BattlePassReward.builder()
                    .type(RewardType.COSMETIC)
                    .stringValue("pet_zombie")
                    .icon(Material.ZOMBIE_HEAD)
                    .name("§aPet Zombie")
                    .build();
                case 100 -> BattlePassReward.builder()
                    .type(RewardType.EXCLUSIVE)
                    .stringValue("s1_exclusive_set")
                    .icon(Material.NETHER_STAR)
                    .name("§6§lSet Exclusif Saison 1")
                    .description("Set unique §c§lNe reviendra jamais!")
                    .build();
                default -> null;
            };
        }
        
        // Niveaux pairs normaux
        if (level < 30) {
            return BattlePassReward.builder()
                .type(RewardType.GEMS)
                .amount(10 + level / 5)
                .icon(Material.DIAMOND)
                .name("§d" + (10 + level / 5) + " Gemmes")
                .build();
        } else if (level < 60) {
            return BattlePassReward.builder()
                .type(RewardType.CRATE)
                .stringValue("rare")
                .amount(1)
                .icon(Material.ENDER_CHEST)
                .name("§9Coffre Rare")
                .build();
        } else {
            return BattlePassReward.builder()
                .type(RewardType.CRATE)
                .stringValue("epic")
                .amount(1)
                .icon(Material.SHULKER_BOX)
                .name("§5Coffre Épique")
                .build();
        }
    }

    /**
     * Obtient les données Battle Pass d'un joueur
     */
    public PlayerBattlePass getPlayerPass(UUID playerId) {
        return playerPasses.computeIfAbsent(playerId, 
            id -> new PlayerBattlePass(id, currentSeason.getId()));
    }

    /**
     * Ajoute de l'XP au Battle Pass
     */
    public void addXp(Player player, int xp) {
        PlayerBattlePass pass = getPlayerPass(player.getUniqueId());
        
        int oldLevel = pass.getLevel();
        pass.addXp(xp);
        int newLevel = pass.getLevel();
        
        // Level up?
        if (newLevel > oldLevel) {
            for (int lvl = oldLevel + 1; lvl <= newLevel; lvl++) {
                onLevelUp(player, pass, lvl);
            }
        }
    }

    /**
     * Gère un level up
     */
    private void onLevelUp(Player player, PlayerBattlePass pass, int level) {
        if (level > MAX_LEVEL) return;
        
        BattlePassLevel passLevel = currentSeason.getLevels().get(level - 1);
        
        // Notification
        player.sendTitle(
            "§6§lBATTLE PASS",
            "§eNiveau " + level + " atteint!",
            10, 40, 10
        );
        
        player.sendMessage("");
        player.sendMessage("§6§l★ Battle Pass Niveau " + level + "!");
        
        // Récompense gratuite auto-claim
        if (passLevel.getFreeReward() != null && !pass.hasClaimedFree(level)) {
            claimReward(player, pass, level, false);
        }
        
        // Notifier de la récompense premium
        if (passLevel.getPremiumReward() != null) {
            if (pass.isPremium()) {
                player.sendMessage("§d  Récompense Premium disponible: " + passLevel.getPremiumReward().getName());
            } else {
                player.sendMessage("§7  §oPasse Premium pour débloquer: " + passLevel.getPremiumReward().getName());
            }
        }
        
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    /**
     * Réclame une récompense
     */
    public boolean claimReward(Player player, PlayerBattlePass pass, int level, boolean premium) {
        if (level > pass.getLevel()) {
            player.sendMessage("§cVous n'avez pas encore atteint ce niveau!");
            return false;
        }
        
        BattlePassLevel passLevel = currentSeason.getLevels().get(level - 1);
        BattlePassReward reward;
        
        if (premium) {
            if (!pass.isPremium()) {
                player.sendMessage("§cVous devez acheter le Pass Premium!");
                return false;
            }
            if (pass.hasClaimedPremium(level)) {
                player.sendMessage("§cRécompense déjà réclamée!");
                return false;
            }
            reward = passLevel.getPremiumReward();
            if (reward == null) {
                player.sendMessage("§cPas de récompense premium à ce niveau.");
                return false;
            }
            pass.claimPremium(level);
        } else {
            if (pass.hasClaimedFree(level)) {
                player.sendMessage("§cRécompense déjà réclamée!");
                return false;
            }
            reward = passLevel.getFreeReward();
            if (reward == null) {
                player.sendMessage("§cPas de récompense gratuite à ce niveau.");
                return false;
            }
            pass.claimFree(level);
        }
        
        // Donner la récompense
        giveReward(player, reward);
        
        player.sendMessage("§a✓ Récompense réclamée: " + reward.getName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        
        return true;
    }

    /**
     * Donne une récompense au joueur
     */
    private void giveReward(Player player, BattlePassReward reward) {
        switch (reward.getType()) {
            case POINTS -> plugin.getEconomyManager().addPoints(player, reward.getAmount());
            case GEMS -> plugin.getEconomyManager().addGems(player, reward.getAmount());
            case XP -> plugin.getEconomyManager().addXp(player, reward.getAmount());
            case CRATE -> {
                // Ouvrir un coffre selon le tier
                String tier = reward.getStringValue();
                int itemTier = switch (tier) {
                    case "common" -> 1;
                    case "rare" -> 2;
                    case "epic" -> 3;
                    case "legendary" -> 4;
                    default -> 1;
                };
                
                Rarity minRarity = switch (tier) {
                    case "rare" -> Rarity.RARE;
                    case "epic" -> Rarity.EPIC;
                    case "legendary" -> Rarity.LEGENDARY;
                    default -> Rarity.COMMON;
                };
                
                ZombieZItem item = plugin.getItemManager().getGenerator()
                    .generateWithMinRarity(itemTier, minRarity, 0);
                if (item != null) {
                    player.getInventory().addItem(item.toItemStack());
                }
            }
            case ITEM -> {
                // Générer un item selon le type
                String itemType = reward.getStringValue();
                int tier = reward.getAmount();
                Rarity rarity = itemType.contains("epic") ? Rarity.EPIC : 
                               itemType.contains("rare") ? Rarity.RARE : Rarity.COMMON;
                
                ZombieZItem item = plugin.getItemManager().getGenerator()
                    .generateWithMinRarity(tier, rarity, 0);
                if (item != null) {
                    player.getInventory().addItem(item.toItemStack());
                }
            }
            case TITLE -> {
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (data != null) {
                    data.addTitle(reward.getStringValue());
                }
            }
            case COSMETIC -> {
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (data != null) {
                    data.addCosmetic(reward.getStringValue());
                }
            }
            case EXCLUSIVE -> {
                // Récompense exclusive de fin de saison
                PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (data != null) {
                    data.addExclusive(reward.getStringValue());
                }
                // Générer l'item exclusif
                // TODO: Implémenter les sets exclusifs
            }
        }
    }

    /**
     * Achète le pass premium
     */
    public boolean purchasePremium(Player player) {
        PlayerBattlePass pass = getPlayerPass(player.getUniqueId());
        
        if (pass.isPremium()) {
            player.sendMessage("§cVous avez déjà le Pass Premium!");
            return false;
        }
        
        if (!plugin.getEconomyManager().hasGems(player, PREMIUM_COST_GEMS)) {
            player.sendMessage("§cVous avez besoin de " + PREMIUM_COST_GEMS + " Gemmes!");
            return false;
        }
        
        plugin.getEconomyManager().removeGems(player, PREMIUM_COST_GEMS);
        pass.setPremium(true);
        
        player.sendTitle(
            "§d§l★ PASS PREMIUM ★",
            "§eActivé avec succès!",
            10, 70, 20
        );
        
        player.sendMessage("");
        player.sendMessage("§d§l★ Pass Premium Activé!");
        player.sendMessage("§7Toutes les récompenses premium sont maintenant disponibles.");
        player.sendMessage("§7Réclamez les récompenses des niveaux déjà atteints!");
        player.sendMessage("");
        
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        
        return true;
    }

    /**
     * Obtient les jours restants de la saison
     */
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(Instant.now(), currentSeason.getEndDate());
    }

    // ==================== INNER CLASSES ====================

    /**
     * Types de récompenses
     */
    public enum RewardType {
        POINTS,
        GEMS,
        XP,
        CRATE,
        ITEM,
        TITLE,
        COSMETIC,
        EXCLUSIVE
    }

    /**
     * Représente une saison
     */
    @Getter
    @Builder
    @lombok.AllArgsConstructor
    public static class Season {
        private final int id;
        private final String name;
        private final String description;
        private final String theme;
        private final Material icon;
        private final Instant startDate;
        private final Instant endDate;
        private final List<BattlePassLevel> levels;
    }

    /**
     * Représente un niveau du Battle Pass
     */
    @Getter
    @Builder
    @lombok.AllArgsConstructor
    public static class BattlePassLevel {
        private final int level;
        private final int xpRequired;
        private final BattlePassReward freeReward;
        private final BattlePassReward premiumReward;
    }

    /**
     * Représente une récompense
     */
    @Getter
    @Builder
    @lombok.AllArgsConstructor
    public static class BattlePassReward {
        private final RewardType type;
        private final String name;
        @Builder.Default
        private final String description = "";
        private final Material icon;
        @Builder.Default
        private final int amount = 1;
        @Builder.Default
        private final String stringValue = "";
    }

    /**
     * Données Battle Pass d'un joueur
     */
    @Getter
    public static class PlayerBattlePass {
        private final UUID playerId;
        private final int seasonId;
        private int xp;
        private int level;
        private boolean premium;
        private final Set<Integer> claimedFree;
        private final Set<Integer> claimedPremium;
        
        public PlayerBattlePass(UUID playerId, int seasonId) {
            this.playerId = playerId;
            this.seasonId = seasonId;
            this.xp = 0;
            this.level = 1;
            this.premium = false;
            this.claimedFree = new HashSet<>();
            this.claimedPremium = new HashSet<>();
        }
        
        public void addXp(int amount) {
            this.xp += amount;
            recalculateLevel();
        }
        
        private void recalculateLevel() {
            int totalXp = xp;
            int lvl = 1;
            
            while (lvl < MAX_LEVEL) {
                int required = XP_PER_LEVEL_BASE + (lvl - 1) * XP_PER_LEVEL_INCREASE;
                if (totalXp < required) break;
                totalXp -= required;
                lvl++;
            }
            
            this.level = Math.min(lvl, MAX_LEVEL);
        }
        
        public int getXpForNextLevel() {
            if (level >= MAX_LEVEL) return 0;
            return XP_PER_LEVEL_BASE + (level - 1) * XP_PER_LEVEL_INCREASE;
        }
        
        public int getCurrentLevelXp() {
            int totalXp = xp;
            for (int i = 1; i < level; i++) {
                totalXp -= XP_PER_LEVEL_BASE + (i - 1) * XP_PER_LEVEL_INCREASE;
            }
            return Math.max(0, totalXp);
        }
        
        public double getProgressPercent() {
            if (level >= MAX_LEVEL) return 100;
            int current = getCurrentLevelXp();
            int required = getXpForNextLevel();
            return (double) current / required * 100;
        }
        
        public void setPremium(boolean premium) {
            this.premium = premium;
        }
        
        public boolean hasClaimedFree(int level) {
            return claimedFree.contains(level);
        }
        
        public boolean hasClaimedPremium(int level) {
            return claimedPremium.contains(level);
        }
        
        public void claimFree(int level) {
            claimedFree.add(level);
        }
        
        public void claimPremium(int level) {
            claimedPremium.add(level);
        }
    }
}
