package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Système d'achievements complet
 * Catégories: Combat, Exploration, Collection, Social, Événements
 */
public class AchievementManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<String, Achievement> achievements;
    
    @Getter
    private final Map<AchievementCategory, List<Achievement>> byCategory;

    public AchievementManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.achievements = new LinkedHashMap<>();
        this.byCategory = new EnumMap<>(AchievementCategory.class);
        
        registerAllAchievements();
        organizeByCategory();
    }

    /**
     * Enregistre tous les achievements
     */
    private void registerAllAchievements() {
        // ============ COMBAT ============
        
        // Kills basiques
        register(new Achievement("first_blood", "Premier Sang", 
            "Tue ton premier zombie", AchievementCategory.COMBAT,
            AchievementTier.BRONZE, Material.IRON_SWORD,
            new int[]{1}, new int[]{10}, new int[]{0}));
        
        register(new Achievement("zombie_slayer_1", "Tueur de Zombies I", 
            "Tue 100 zombies", AchievementCategory.COMBAT,
            AchievementTier.BRONZE, Material.IRON_SWORD,
            new int[]{100}, new int[]{50}, new int[]{1}));
        
        register(new Achievement("zombie_slayer_2", "Tueur de Zombies II", 
            "Tue 1,000 zombies", AchievementCategory.COMBAT,
            AchievementTier.SILVER, Material.DIAMOND_SWORD,
            new int[]{1000}, new int[]{200}, new int[]{5}));
        
        register(new Achievement("zombie_slayer_3", "Tueur de Zombies III", 
            "Tue 10,000 zombies", AchievementCategory.COMBAT,
            AchievementTier.GOLD, Material.NETHERITE_SWORD,
            new int[]{10000}, new int[]{1000}, new int[]{25}));
        
        register(new Achievement("zombie_slayer_4", "Exterminateur", 
            "Tue 100,000 zombies", AchievementCategory.COMBAT,
            AchievementTier.DIAMOND, Material.NETHERITE_SWORD,
            new int[]{100000}, new int[]{5000}, new int[]{100}));
        
        // Kills spéciaux
        register(new Achievement("elite_hunter", "Chasseur d'Élites", 
            "Tue 50 zombies élites", AchievementCategory.COMBAT,
            AchievementTier.SILVER, Material.GOLDEN_SWORD,
            new int[]{50}, new int[]{300}, new int[]{10}));
        
        register(new Achievement("boss_slayer", "Tueur de Boss", 
            "Tue 10 boss", AchievementCategory.COMBAT,
            AchievementTier.GOLD, Material.DRAGON_HEAD,
            new int[]{10}, new int[]{1000}, new int[]{50}));
        
        register(new Achievement("patient_zero_defeated", "Patient Zéro Vaincu", 
            "Vaincs Patient Zéro", AchievementCategory.COMBAT,
            AchievementTier.LEGENDARY, Material.WITHER_SKELETON_SKULL,
            new int[]{1}, new int[]{5000}, new int[]{200}));
        
        // Streaks
        register(new Achievement("killing_spree", "Série de Kills", 
            "Tue 20 zombies sans mourir", AchievementCategory.COMBAT,
            AchievementTier.BRONZE, Material.BLAZE_POWDER,
            new int[]{20}, new int[]{100}, new int[]{3}));
        
        register(new Achievement("unstoppable", "Inarrêtable", 
            "Tue 100 zombies sans mourir", AchievementCategory.COMBAT,
            AchievementTier.GOLD, Material.NETHER_STAR,
            new int[]{100}, new int[]{500}, new int[]{20}));
        
        // ============ EXPLORATION ============
        
        register(new Achievement("explorer_1", "Explorateur I", 
            "Visite 3 zones différentes", AchievementCategory.EXPLORATION,
            AchievementTier.BRONZE, Material.COMPASS,
            new int[]{3}, new int[]{30}, new int[]{1}));
        
        register(new Achievement("explorer_2", "Explorateur II", 
            "Visite toutes les zones (11)", AchievementCategory.EXPLORATION,
            AchievementTier.GOLD, Material.FILLED_MAP,
            new int[]{11}, new int[]{500}, new int[]{15}));
        
        register(new Achievement("zone_master", "Maître de Zone", 
            "Passe 10 heures dans une même zone", AchievementCategory.EXPLORATION,
            AchievementTier.SILVER, Material.CLOCK,
            new int[]{36000}, new int[]{200}, new int[]{8})); // 10h en secondes
        
        register(new Achievement("night_walker", "Marcheur Nocturne", 
            "Survie 10 nuits complètes", AchievementCategory.EXPLORATION,
            AchievementTier.BRONZE, Material.BLACK_CANDLE,
            new int[]{10}, new int[]{100}, new int[]{5}));
        
        register(new Achievement("danger_zone", "Zone Dangereuse", 
            "Survie 1 heure en Zone 10+", AchievementCategory.EXPLORATION,
            AchievementTier.GOLD, Material.FIRE_CHARGE,
            new int[]{3600}, new int[]{750}, new int[]{30}));
        
        // ============ COLLECTION ============
        
        register(new Achievement("loot_collector_1", "Collectionneur I", 
            "Ramasse 50 items", AchievementCategory.COLLECTION,
            AchievementTier.BRONZE, Material.CHEST,
            new int[]{50}, new int[]{25}, new int[]{1}));
        
        register(new Achievement("loot_collector_2", "Collectionneur II", 
            "Ramasse 500 items", AchievementCategory.COLLECTION,
            AchievementTier.SILVER, Material.ENDER_CHEST,
            new int[]{500}, new int[]{150}, new int[]{5}));
        
        register(new Achievement("rare_finder", "Trouveur de Raretés", 
            "Trouve 10 items Épiques ou mieux", AchievementCategory.COLLECTION,
            AchievementTier.GOLD, Material.AMETHYST_SHARD,
            new int[]{10}, new int[]{400}, new int[]{15}));
        
        register(new Achievement("legendary_luck", "Chance Légendaire", 
            "Trouve un item Légendaire", AchievementCategory.COLLECTION,
            AchievementTier.GOLD, Material.NETHER_STAR,
            new int[]{1}, new int[]{1000}, new int[]{50}));
        
        register(new Achievement("set_collector", "Collectionneur de Sets", 
            "Complète un set d'équipement", AchievementCategory.COLLECTION,
            AchievementTier.SILVER, Material.ARMOR_STAND,
            new int[]{1}, new int[]{300}, new int[]{10}));
        
        register(new Achievement("millionaire", "Millionnaire", 
            "Accumule 1,000,000 de points", AchievementCategory.COLLECTION,
            AchievementTier.DIAMOND, Material.GOLD_BLOCK,
            new int[]{1000000}, new int[]{2500}, new int[]{100}));
        
        // ============ SOCIAL ============
        
        register(new Achievement("team_player", "Joueur d'Équipe", 
            "Tue un boss en groupe", AchievementCategory.SOCIAL,
            AchievementTier.BRONZE, Material.PLAYER_HEAD,
            new int[]{1}, new int[]{100}, new int[]{5}));
        
        register(new Achievement("reviver", "Réanimateur", 
            "Aide 10 joueurs tombés", AchievementCategory.SOCIAL,
            AchievementTier.SILVER, Material.TOTEM_OF_UNDYING,
            new int[]{10}, new int[]{200}, new int[]{8}));
        
        register(new Achievement("guild_founder", "Fondateur de Guilde", 
            "Crée une guilde", AchievementCategory.SOCIAL,
            AchievementTier.SILVER, Material.SHIELD,
            new int[]{1}, new int[]{500}, new int[]{20}));
        
        register(new Achievement("trader", "Marchand", 
            "Effectue 50 échanges avec d'autres joueurs", AchievementCategory.SOCIAL,
            AchievementTier.SILVER, Material.EMERALD,
            new int[]{50}, new int[]{250}, new int[]{10}));
        
        // ============ ÉVÉNEMENTS ============
        
        register(new Achievement("blood_moon_survivor", "Survivant Blood Moon", 
            "Survie à une Blood Moon complète", AchievementCategory.EVENTS,
            AchievementTier.SILVER, Material.REDSTONE,
            new int[]{1}, new int[]{300}, new int[]{15}));
        
        register(new Achievement("blood_moon_veteran", "Vétéran Blood Moon", 
            "Survie à 10 Blood Moons", AchievementCategory.EVENTS,
            AchievementTier.GOLD, Material.REDSTONE_BLOCK,
            new int[]{10}, new int[]{1500}, new int[]{75}));
        
        register(new Achievement("horde_breaker", "Briseur de Horde", 
            "Élimine 50% d'une horde seul", AchievementCategory.EVENTS,
            AchievementTier.GOLD, Material.TNT,
            new int[]{1}, new int[]{500}, new int[]{25}));
        
        register(new Achievement("event_champion", "Champion des Événements", 
            "Participe à 100 événements", AchievementCategory.EVENTS,
            AchievementTier.DIAMOND, Material.BEACON,
            new int[]{100}, new int[]{3000}, new int[]{150}));
        
        // ============ PROGRESSION ============
        
        register(new Achievement("level_10", "Niveau 10", 
            "Atteins le niveau 10", AchievementCategory.PROGRESSION,
            AchievementTier.BRONZE, Material.EXPERIENCE_BOTTLE,
            new int[]{10}, new int[]{50}, new int[]{2}));
        
        register(new Achievement("level_50", "Niveau 50", 
            "Atteins le niveau 50", AchievementCategory.PROGRESSION,
            AchievementTier.SILVER, Material.EXPERIENCE_BOTTLE,
            new int[]{50}, new int[]{300}, new int[]{15}));
        
        register(new Achievement("level_100", "Niveau Max", 
            "Atteins le niveau 100", AchievementCategory.PROGRESSION,
            AchievementTier.GOLD, Material.EXPERIENCE_BOTTLE,
            new int[]{100}, new int[]{1000}, new int[]{50}));
        
        register(new Achievement("first_prestige", "Premier Prestige", 
            "Effectue ton premier prestige", AchievementCategory.PROGRESSION,
            AchievementTier.GOLD, Material.GOLDEN_APPLE,
            new int[]{1}, new int[]{2000}, new int[]{100}));
        
        register(new Achievement("max_prestige", "Prestige Maximum", 
            "Atteins le prestige 10", AchievementCategory.PROGRESSION,
            AchievementTier.LEGENDARY, Material.ENCHANTED_GOLDEN_APPLE,
            new int[]{10}, new int[]{10000}, new int[]{500}));
    }

    /**
     * Enregistre un achievement
     */
    private void register(Achievement achievement) {
        achievements.put(achievement.id(), achievement);
    }

    /**
     * Organise les achievements par catégorie
     */
    private void organizeByCategory() {
        for (AchievementCategory cat : AchievementCategory.values()) {
            byCategory.put(cat, new ArrayList<>());
        }
        
        for (Achievement achievement : achievements.values()) {
            byCategory.get(achievement.category()).add(achievement);
        }
    }

    /**
     * Vérifie et débloque un achievement pour un joueur
     */
    public boolean checkAndUnlock(Player player, String achievementId, int progress) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return false;
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Déjà débloqué?
        if (data.hasAchievement(achievementId)) return false;
        
        // Vérifier le progrès
        if (progress >= achievement.requirements()[0]) {
            return unlock(player, achievement);
        }
        
        return false;
    }

    /**
     * Incrémente le progrès d'un achievement
     */
    public void incrementProgress(Player player, String achievementId, int amount) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return;
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        if (data.hasAchievement(achievementId)) return;
        
        int currentProgress = data.getAchievementProgress(achievementId);
        int newProgress = currentProgress + amount;
        data.setAchievementProgress(achievementId, newProgress);
        
        // Vérifier si débloqué
        if (newProgress >= achievement.requirements()[0]) {
            unlock(player, achievement);
        }
    }

    /**
     * Débloque un achievement
     */
    private boolean unlock(Player player, Achievement achievement) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        // Marquer comme débloqué
        data.unlockAchievement(achievement.id());
        
        // Notification
        player.sendTitle(
            "§6§l★ ACHIEVEMENT! ★",
            achievement.tier().getColor() + achievement.name(),
            10, 70, 20
        );
        
        player.sendMessage("");
        player.sendMessage("§6§l★ Achievement Débloqué!");
        player.sendMessage(achievement.tier().getColor() + "  " + achievement.name());
        player.sendMessage("§7  " + achievement.description());
        player.sendMessage("§e  +" + achievement.pointReward()[0] + " Points §7| §d+" + achievement.gemReward()[0] + " Gemmes");
        player.sendMessage("");
        
        // Son
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        
        // Récompenses
        plugin.getEconomyManager().addPoints(player, achievement.pointReward()[0]);
        plugin.getEconomyManager().addGems(player, achievement.gemReward()[0]);
        
        // Effets visuels selon le tier
        spawnTierEffects(player, achievement.tier());
        
        // Broadcast pour les achievements rares
        if (achievement.tier() == AchievementTier.LEGENDARY) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p != player) {
                    p.sendMessage("§6§l★ §e" + player.getName() + " §7a débloqué " + 
                        achievement.tier().getColor() + achievement.name() + "§7!");
                }
            }
        }
        
        return true;
    }

    /**
     * Effets visuels selon le tier
     */
    private void spawnTierEffects(Player player, AchievementTier tier) {
        var loc = player.getLocation().add(0, 1, 0);
        
        switch (tier) {
            case BRONZE -> loc.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5);
            case SILVER -> loc.getWorld().spawnParticle(
                org.bukkit.Particle.FIREWORK, loc, 30, 0.5, 0.5, 0.5);
            case GOLD -> loc.getWorld().spawnParticle(
                org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 40, 0.5, 0.5, 0.5);
            case DIAMOND -> loc.getWorld().spawnParticle(
                org.bukkit.Particle.DRAGON_BREATH, loc, 50, 0.5, 0.5, 0.5);
            case LEGENDARY -> {
                loc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc, 100, 1, 1, 1, 0.3);
                loc.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 50, 0.5, 0.5, 0.5);
            }
        }
    }

    /**
     * Obtient le nombre d'achievements débloqués
     */
    public int getUnlockedCount(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return data.getUnlockedAchievements().size();
    }

    /**
     * Obtient le pourcentage de complétion
     */
    public double getCompletionPercent(Player player) {
        int unlocked = getUnlockedCount(player);
        int total = achievements.size();
        return (double) unlocked / total * 100;
    }

    /**
     * Catégories d'achievements
     */
    public enum AchievementCategory {
        COMBAT("Combat", "§c", Material.DIAMOND_SWORD),
        EXPLORATION("Exploration", "§a", Material.COMPASS),
        COLLECTION("Collection", "§e", Material.CHEST),
        SOCIAL("Social", "§b", Material.PLAYER_HEAD),
        EVENTS("Événements", "§d", Material.BEACON),
        PROGRESSION("Progression", "§6", Material.EXPERIENCE_BOTTLE);
        
        @Getter private final String displayName;
        @Getter private final String color;
        @Getter private final Material icon;
        
        AchievementCategory(String displayName, String color, Material icon) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }
    }

    /**
     * Tiers d'achievements
     */
    public enum AchievementTier {
        BRONZE("§6", "Bronze"),
        SILVER("§7", "Argent"),
        GOLD("§e", "Or"),
        DIAMOND("§b", "Diamant"),
        LEGENDARY("§d", "Légendaire");
        
        @Getter private final String color;
        @Getter private final String displayName;
        
        AchievementTier(String color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
    }

    /**
     * Représente un achievement
     */
    public record Achievement(
        String id,
        String name,
        String description,
        AchievementCategory category,
        AchievementTier tier,
        Material icon,
        int[] requirements,
        int[] pointReward,
        int[] gemReward
    ) {}
}
