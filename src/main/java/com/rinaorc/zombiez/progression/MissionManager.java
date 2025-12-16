package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de missions journalières et hebdomadaires
 */
public class MissionManager {

    private final ZombieZPlugin plugin;
    
    // Pool de missions disponibles
    @Getter
    private final Map<String, Mission> missionPool;
    
    // Missions actives par joueur
    private final Map<UUID, PlayerMissions> playerMissions;
    
    // Configuration
    private static final int DAILY_MISSION_COUNT = 3;
    private static final int WEEKLY_MISSION_COUNT = 5;
    private static final LocalTime DAILY_RESET_TIME = LocalTime.of(0, 0);
    private static final DayOfWeek WEEKLY_RESET_DAY = DayOfWeek.MONDAY;
    
    private final Random random = new Random();

    public MissionManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.missionPool = new LinkedHashMap<>();
        this.playerMissions = new ConcurrentHashMap<>();
        
        registerAllMissions();
        startResetTask();
    }

    /**
     * Enregistre toutes les missions disponibles
     */
    private void registerAllMissions() {
        // ============ MISSIONS JOURNALIÈRES ============
        
        // Combat
        register(Mission.builder()
            .id("daily_kill_50")
            .name("Chasseur")
            .description("Tue 50 zombies")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.IRON_SWORD)
            .goal(50)
            .tracker(MissionTracker.ZOMBIE_KILLS)
            .pointReward(100)
            .xpReward(50)
            .difficulty(1)
            .build());
        
        register(Mission.builder()
            .id("daily_kill_100")
            .name("Exterminateur")
            .description("Tue 100 zombies")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.DIAMOND_SWORD)
            .goal(100)
            .tracker(MissionTracker.ZOMBIE_KILLS)
            .pointReward(200)
            .xpReward(100)
            .difficulty(2)
            .build());
        
        register(Mission.builder()
            .id("daily_elite_5")
            .name("Chasseur d'Élites")
            .description("Tue 5 zombies élites")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.GOLDEN_SWORD)
            .goal(5)
            .tracker(MissionTracker.ELITE_KILLS)
            .pointReward(300)
            .xpReward(150)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("daily_headshots_20")
            .name("Tireur d'Élite")
            .description("Fais 20 headshots")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.BOW)
            .goal(20)
            .tracker(MissionTracker.HEADSHOTS)
            .pointReward(250)
            .xpReward(125)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("daily_no_damage_10")
            .name("Intouchable")
            .description("Tue 10 zombies sans prendre de dégâts")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.SHIELD)
            .goal(10)
            .tracker(MissionTracker.KILLS_NO_DAMAGE)
            .pointReward(400)
            .xpReward(200)
            .difficulty(4)
            .build());
        
        // Exploration
        register(Mission.builder()
            .id("daily_zones_3")
            .name("Explorateur")
            .description("Visite 3 zones différentes")
            .type(MissionType.DAILY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.COMPASS)
            .goal(3)
            .tracker(MissionTracker.ZONES_VISITED)
            .pointReward(150)
            .xpReward(75)
            .difficulty(2)
            .build());
        
        register(Mission.builder()
            .id("daily_night_survive")
            .name("Marcheur Nocturne")
            .description("Survie une nuit complète")
            .type(MissionType.DAILY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.BLACK_CANDLE)
            .goal(1)
            .tracker(MissionTracker.NIGHTS_SURVIVED)
            .pointReward(200)
            .xpReward(100)
            .difficulty(2)
            .build());
        
        register(Mission.builder()
            .id("daily_distance_1000")
            .name("Marathonien")
            .description("Parcours 1000 blocs")
            .type(MissionType.DAILY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.LEATHER_BOOTS)
            .goal(1000)
            .tracker(MissionTracker.DISTANCE_TRAVELED)
            .pointReward(100)
            .xpReward(50)
            .difficulty(1)
            .build());
        
        // Collection
        register(Mission.builder()
            .id("daily_loot_10")
            .name("Pilleur")
            .description("Ramasse 10 items")
            .type(MissionType.DAILY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.CHEST)
            .goal(10)
            .tracker(MissionTracker.ITEMS_LOOTED)
            .pointReward(100)
            .xpReward(50)
            .difficulty(1)
            .build());
        
        register(Mission.builder()
            .id("daily_rare_1")
            .name("Trouveur")
            .description("Trouve un item Rare ou mieux")
            .type(MissionType.DAILY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.AMETHYST_SHARD)
            .goal(1)
            .tracker(MissionTracker.RARE_ITEMS_FOUND)
            .pointReward(300)
            .xpReward(150)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("daily_points_500")
            .name("Économiste")
            .description("Gagne 500 points")
            .type(MissionType.DAILY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.GOLD_INGOT)
            .goal(500)
            .tracker(MissionTracker.POINTS_EARNED)
            .pointReward(100)
            .xpReward(50)
            .difficulty(1)
            .build());
        
        // Social
        register(Mission.builder()
            .id("daily_trade_1")
            .name("Marchand")
            .description("Effectue un échange")
            .type(MissionType.DAILY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.EMERALD)
            .goal(1)
            .tracker(MissionTracker.TRADES_COMPLETED)
            .pointReward(200)
            .xpReward(100)
            .difficulty(2)
            .build());
        
        register(Mission.builder()
            .id("daily_group_kill")
            .name("Travail d'Équipe")
            .description("Tue un zombie avec un autre joueur à proximité")
            .type(MissionType.DAILY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.PLAYER_HEAD)
            .goal(1)
            .tracker(MissionTracker.GROUP_KILLS)
            .pointReward(150)
            .xpReward(75)
            .difficulty(2)
            .build());
        
        // ============ MISSIONS HEBDOMADAIRES ============
        
        register(Mission.builder()
            .id("weekly_kill_500")
            .name("Fléau des Zombies")
            .description("Tue 500 zombies")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.NETHERITE_SWORD)
            .goal(500)
            .tracker(MissionTracker.ZOMBIE_KILLS)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("weekly_kill_1000")
            .name("Légende Vivante")
            .description("Tue 1000 zombies")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.NETHERITE_SWORD)
            .goal(1000)
            .tracker(MissionTracker.ZOMBIE_KILLS)
            .pointReward(2500)
            .xpReward(1000)
            .gemReward(25)
            .difficulty(4)
            .build());
        
        register(Mission.builder()
            .id("weekly_boss_3")
            .name("Tueur de Boss")
            .description("Tue 3 boss")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.DRAGON_HEAD)
            .goal(3)
            .tracker(MissionTracker.BOSS_KILLS)
            .pointReward(2000)
            .xpReward(800)
            .gemReward(20)
            .difficulty(4)
            .build());
        
        register(Mission.builder()
            .id("weekly_all_zones")
            .name("Grand Explorateur")
            .description("Visite toutes les zones")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.FILLED_MAP)
            .goal(11)
            .tracker(MissionTracker.ZONES_VISITED)
            .pointReward(1500)
            .xpReward(600)
            .gemReward(15)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("weekly_blood_moon")
            .name("Survivant Blood Moon")
            .description("Survie à une Blood Moon")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EVENTS)
            .icon(Material.REDSTONE_BLOCK)
            .goal(1)
            .tracker(MissionTracker.BLOOD_MOONS_SURVIVED)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(25)
            .difficulty(4)
            .build());
        
        register(Mission.builder()
            .id("weekly_epic_items_3")
            .name("Chasseur de Trésors")
            .description("Trouve 3 items Épiques ou mieux")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.NETHER_STAR)
            .goal(3)
            .tracker(MissionTracker.EPIC_ITEMS_FOUND)
            .pointReward(1500)
            .xpReward(750)
            .gemReward(15)
            .difficulty(4)
            .build());
        
        register(Mission.builder()
            .id("weekly_level_5")
            .name("Progression")
            .description("Gagne 5 niveaux")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.PROGRESSION)
            .icon(Material.EXPERIENCE_BOTTLE)
            .goal(5)
            .tracker(MissionTracker.LEVELS_GAINED)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("weekly_trades_10")
            .name("Maître Marchand")
            .description("Effectue 10 échanges")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.EMERALD_BLOCK)
            .goal(10)
            .tracker(MissionTracker.TRADES_COMPLETED)
            .pointReward(1000)
            .xpReward(400)
            .gemReward(10)
            .difficulty(3)
            .build());
        
        register(Mission.builder()
            .id("weekly_playtime_10h")
            .name("Dédié")
            .description("Joue 10 heures cette semaine")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.PROGRESSION)
            .icon(Material.CLOCK)
            .goal(36000)
            .tracker(MissionTracker.PLAYTIME)
            .pointReward(1500)
            .xpReward(600)
            .gemReward(15)
            .difficulty(3)
            .build());
    }

    /**
     * Enregistre une mission
     */
    private void register(Mission mission) {
        missionPool.put(mission.getId(), mission);
    }

    /**
     * Génère les missions d'un joueur
     */
    public PlayerMissions generateMissions(UUID playerId) {
        PlayerMissions missions = new PlayerMissions(playerId);

        // Générer les missions journalières
        List<Mission> dailyPool = missionPool.values().stream()
            .filter(m -> m.getType() == MissionType.DAILY)
            .toList();

        List<Mission> shuffledDaily = new ArrayList<>(dailyPool);
        Collections.shuffle(shuffledDaily);
        for (int i = 0; i < Math.min(DAILY_MISSION_COUNT, shuffledDaily.size()); i++) {
            missions.addDailyMission(shuffledDaily.get(i));
        }

        // Générer les missions hebdomadaires
        List<Mission> weeklyPool = missionPool.values().stream()
            .filter(m -> m.getType() == MissionType.WEEKLY)
            .toList();

        List<Mission> shuffledWeekly = new ArrayList<>(weeklyPool);
        Collections.shuffle(shuffledWeekly);
        for (int i = 0; i < Math.min(WEEKLY_MISSION_COUNT, shuffledWeekly.size()); i++) {
            missions.addWeeklyMission(shuffledWeekly.get(i));
        }

        // Stocker et retourner les missions
        playerMissions.put(playerId, missions);
        return missions;
    }

    /**
     * Obtient les missions d'un joueur
     */
    public PlayerMissions getMissions(UUID playerId) {
        PlayerMissions missions = playerMissions.get(playerId);
        if (missions == null) {
            missions = loadOrGenerate(playerId);
        }
        return missions;
    }

    private PlayerMissions loadOrGenerate(UUID playerId) {
        // Essayer de charger depuis la BDD, sinon générer
        // TODO: Implémenter le chargement depuis la BDD
        return generateMissions(playerId);
    }

    /**
     * Met à jour le progrès d'une mission
     */
    public void updateProgress(Player player, MissionTracker tracker, int amount) {
        UUID playerId = player.getUniqueId();
        PlayerMissions missions = getMissions(playerId);
        
        List<MissionProgress> allMissions = new ArrayList<>();
        allMissions.addAll(missions.getDailyMissions().values());
        allMissions.addAll(missions.getWeeklyMissions().values());
        
        for (MissionProgress progress : allMissions) {
            if (progress.isCompleted()) continue;
            if (progress.getMission().getTracker() != tracker) continue;
            
            progress.addProgress(amount);
            
            // Vérifier si complétée
            if (progress.getProgress() >= progress.getMission().getGoal()) {
                completeMission(player, progress);
            }
        }
    }

    /**
     * Complète une mission
     */
    private void completeMission(Player player, MissionProgress progress) {
        progress.setCompleted(true);
        progress.setCompletedAt(System.currentTimeMillis());
        
        Mission mission = progress.getMission();
        
        // Notification
        player.sendTitle(
            "§a§l✓ MISSION COMPLÈTE!",
            "§e" + mission.getName(),
            10, 60, 20
        );
        
        player.sendMessage("");
        player.sendMessage("§a§l✓ Mission Complétée: §e" + mission.getName());
        player.sendMessage("§7  " + mission.getDescription());
        player.sendMessage("§e  Récompenses:");
        player.sendMessage("§6    +" + mission.getPointReward() + " Points");
        player.sendMessage("§b    +" + mission.getXpReward() + " XP");
        if (mission.getGemReward() > 0) {
            player.sendMessage("§d    +" + mission.getGemReward() + " Gemmes");
        }
        player.sendMessage("");
        
        // Son
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        
        // Récompenses
        plugin.getEconomyManager().addPoints(player, mission.getPointReward());
        plugin.getEconomyManager().addXp(player, mission.getXpReward());
        if (mission.getGemReward() > 0) {
            plugin.getEconomyManager().addGems(player, mission.getGemReward());
        }
        
        // Tracker achievement
        plugin.getAchievementManager().incrementProgress(player, "mission_master", 1);
    }

    /**
     * Réinitialise les missions journalières
     */
    public void resetDailyMissions() {
        for (PlayerMissions missions : playerMissions.values()) {
            missions.getDailyMissions().clear();
            
            // Générer de nouvelles missions journalières
            List<Mission> dailyPool = missionPool.values().stream()
                .filter(m -> m.getType() == MissionType.DAILY)
                .toList();
            
            List<Mission> shuffled = new ArrayList<>(dailyPool);
            Collections.shuffle(shuffled);
            for (int i = 0; i < Math.min(DAILY_MISSION_COUNT, shuffled.size()); i++) {
                missions.addDailyMission(shuffled.get(i));
            }
        }
        
        // Notifier les joueurs en ligne
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage("§e[Missions] §aDe nouvelles missions journalières sont disponibles!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
    }

    /**
     * Réinitialise les missions hebdomadaires
     */
    public void resetWeeklyMissions() {
        for (PlayerMissions missions : playerMissions.values()) {
            missions.getWeeklyMissions().clear();
            
            List<Mission> weeklyPool = missionPool.values().stream()
                .filter(m -> m.getType() == MissionType.WEEKLY)
                .toList();
            
            List<Mission> shuffled = new ArrayList<>(weeklyPool);
            Collections.shuffle(shuffled);
            for (int i = 0; i < Math.min(WEEKLY_MISSION_COUNT, shuffled.size()); i++) {
                missions.addWeeklyMission(shuffled.get(i));
            }
        }
        
        // Notifier les joueurs en ligne
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage("§d[Missions] §aDe nouvelles missions hebdomadaires sont disponibles!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    /**
     * Démarre la tâche de reset
     */
    private void startResetTask() {
        new BukkitRunnable() {
            private LocalDate lastDailyReset = LocalDate.now();
            private LocalDate lastWeeklyReset = LocalDate.now();
            
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                LocalDate today = now.toLocalDate();
                
                // Reset journalier
                if (!today.equals(lastDailyReset) && now.toLocalTime().isAfter(DAILY_RESET_TIME)) {
                    lastDailyReset = today;
                    resetDailyMissions();
                }
                
                // Reset hebdomadaire
                if (today.getDayOfWeek() == WEEKLY_RESET_DAY && 
                    !today.equals(lastWeeklyReset) && 
                    now.toLocalTime().isAfter(DAILY_RESET_TIME)) {
                    lastWeeklyReset = today;
                    resetWeeklyMissions();
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Check toutes les minutes
    }

    /**
     * Obtient le temps avant le prochain reset journalier
     */
    public long getTimeUntilDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = LocalDate.now().plusDays(1).atTime(DAILY_RESET_TIME);
        return Duration.between(now, nextReset).toSeconds();
    }

    /**
     * Obtient le temps avant le prochain reset hebdomadaire
     */
    public long getTimeUntilWeeklyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        if (daysUntilMonday == 0 && now.toLocalTime().isAfter(DAILY_RESET_TIME)) {
            daysUntilMonday = 7;
        }
        
        LocalDateTime nextReset = today.plusDays(daysUntilMonday).atTime(DAILY_RESET_TIME);
        return Duration.between(now, nextReset).toSeconds();
    }

    // ==================== INNER CLASSES ====================

    /**
     * Types de missions
     */
    public enum MissionType {
        DAILY("Journalière", "§e"),
        WEEKLY("Hebdomadaire", "§d");
        
        @Getter private final String displayName;
        @Getter private final String color;
        
        MissionType(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /**
     * Catégories de missions
     */
    public enum MissionCategory {
        COMBAT("Combat", "§c", Material.DIAMOND_SWORD),
        EXPLORATION("Exploration", "§a", Material.COMPASS),
        COLLECTION("Collection", "§e", Material.CHEST),
        SOCIAL("Social", "§b", Material.PLAYER_HEAD),
        EVENTS("Événements", "§d", Material.BEACON),
        PROGRESSION("Progression", "§6", Material.EXPERIENCE_BOTTLE);
        
        @Getter private final String displayName;
        @Getter private final String color;
        @Getter private final Material icon;
        
        MissionCategory(String displayName, String color, Material icon) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }
    }

    /**
     * Trackers de missions
     */
    public enum MissionTracker {
        ZOMBIE_KILLS,
        ELITE_KILLS,
        BOSS_KILLS,
        HEADSHOTS,
        KILLS_NO_DAMAGE,
        ZONES_VISITED,
        NIGHTS_SURVIVED,
        DISTANCE_TRAVELED,
        ITEMS_LOOTED,
        RARE_ITEMS_FOUND,
        EPIC_ITEMS_FOUND,
        POINTS_EARNED,
        TRADES_COMPLETED,
        GROUP_KILLS,
        BLOOD_MOONS_SURVIVED,
        LEVELS_GAINED,
        PLAYTIME
    }

    /**
     * Représente une mission
     */
    @Getter
    @Builder
    public static class Mission {
        private final String id;
        private final String name;
        private final String description;
        private final MissionType type;
        private final MissionCategory category;
        private final Material icon;
        private final int goal;
        private final MissionTracker tracker;
        private final int pointReward;
        private final int xpReward;
        @Builder.Default
        private final int gemReward = 0;
        private final int difficulty; // 1-5
    }

    /**
     * Progrès d'une mission pour un joueur
     */
    @Getter
    public static class MissionProgress {
        private final Mission mission;
        private int progress;
        private boolean completed;
        private long completedAt;
        
        public MissionProgress(Mission mission) {
            this.mission = mission;
            this.progress = 0;
            this.completed = false;
        }
        
        public void addProgress(int amount) {
            if (!completed) {
                this.progress += amount;
            }
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public void setCompletedAt(long time) {
            this.completedAt = time;
        }
        
        public double getProgressPercent() {
            return Math.min(100, (double) progress / mission.getGoal() * 100);
        }
    }

    /**
     * Missions d'un joueur
     */
    @Getter
    public static class PlayerMissions {
        private final UUID playerId;
        private final Map<String, MissionProgress> dailyMissions;
        private final Map<String, MissionProgress> weeklyMissions;
        private long lastDailyReset;
        private long lastWeeklyReset;
        
        public PlayerMissions(UUID playerId) {
            this.playerId = playerId;
            this.dailyMissions = new LinkedHashMap<>();
            this.weeklyMissions = new LinkedHashMap<>();
            this.lastDailyReset = System.currentTimeMillis();
            this.lastWeeklyReset = System.currentTimeMillis();
        }
        
        public void addDailyMission(Mission mission) {
            dailyMissions.put(mission.getId(), new MissionProgress(mission));
        }
        
        public void addWeeklyMission(Mission mission) {
            weeklyMissions.put(mission.getId(), new MissionProgress(mission));
        }
        
        public int getCompletedDailyCount() {
            return (int) dailyMissions.values().stream().filter(MissionProgress::isCompleted).count();
        }
        
        public int getCompletedWeeklyCount() {
            return (int) weeklyMissions.values().stream().filter(MissionProgress::isCompleted).count();
        }
    }
}
