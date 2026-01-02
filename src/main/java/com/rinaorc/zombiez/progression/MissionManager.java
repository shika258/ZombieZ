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
    private static final int DAILY_MISSION_COUNT = 7;
    private static final int WEEKLY_MISSION_COUNT = 21;
    private static final LocalTime DAILY_RESET_TIME = LocalTime.of(0, 0);
    private static final DayOfWeek WEEKLY_RESET_DAY = DayOfWeek.MONDAY;

    private final Random random = new Random();

    // ============ TRACKING POUR NUITS SURVÉCUES ET REFUGES VISITÉS ============
    // Joueurs en vie à la nuit tombante (pour tracker les nuits survécues)
    private final Set<UUID> playersAliveAtNightfall = ConcurrentHashMap.newKeySet();
    // Était-ce la nuit au dernier check? (pour détecter la transition nuit -> jour)
    private boolean wasNight = false;
    // Refuges visités par joueur (pour éviter de re-tracker le même refuge)
    private final Map<UUID, Set<Integer>> visitedRefuges = new ConcurrentHashMap<>();

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
        // ============ MISSIONS JOURNALIÈRES (25 missions) ============

        // === COMBAT (10 missions) ===
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

        register(Mission.builder()
            .id("daily_crit_kills_15")
            .name("Coup Fatal")
            .description("Tue 15 zombies avec un coup critique")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.DIAMOND_AXE)
            .goal(15)
            .tracker(MissionTracker.CRIT_KILLS)
            .pointReward(250)
            .xpReward(125)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("daily_ranged_kills_30")
            .name("Archer")
            .description("Tue 30 zombies à distance")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.CROSSBOW)
            .goal(30)
            .tracker(MissionTracker.RANGED_KILLS)
            .pointReward(200)
            .xpReward(100)
            .difficulty(2)
            .build());

        register(Mission.builder()
            .id("daily_melee_kills_50")
            .name("Berserker")
            .description("Tue 50 zombies au corps à corps")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.NETHERITE_AXE)
            .goal(50)
            .tracker(MissionTracker.MELEE_KILLS)
            .pointReward(150)
            .xpReward(75)
            .difficulty(2)
            .build());

        register(Mission.builder()
            .id("daily_night_kills_25")
            .name("Noctambule")
            .description("Tue 25 zombies pendant la nuit")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.ENDER_EYE)
            .goal(25)
            .tracker(MissionTracker.NIGHT_KILLS)
            .pointReward(300)
            .xpReward(150)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("daily_streak_20")
            .name("Inarrêtable")
            .description("Atteins une série de 20 kills sans mourir")
            .type(MissionType.DAILY)
            .category(MissionCategory.COMBAT)
            .icon(Material.BLAZE_POWDER)
            .goal(20)
            .tracker(MissionTracker.KILLS_IN_STREAK)
            .pointReward(350)
            .xpReward(175)
            .difficulty(4)
            .build());

        // === EXPLORATION (5 missions) ===
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

        register(Mission.builder()
            .id("daily_refuge_visit")
            .name("Voyageur")
            .description("Visite 2 refuges différents")
            .type(MissionType.DAILY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.CAMPFIRE)
            .goal(2)
            .tracker(MissionTracker.REFUGES_VISITED)
            .pointReward(150)
            .xpReward(75)
            .difficulty(2)
            .build());

        register(Mission.builder()
            .id("daily_danger_zone_5min")
            .name("Téméraire")
            .description("Passe 5 minutes dans une zone dangereuse")
            .type(MissionType.DAILY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.FIRE_CHARGE)
            .goal(300)
            .tracker(MissionTracker.TIME_IN_DANGER_ZONE)
            .pointReward(350)
            .xpReward(175)
            .difficulty(4)
            .build());

        // === COLLECTION (5 missions) ===
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

        register(Mission.builder()
            .id("daily_consumable_3")
            .name("Survivaliste")
            .description("Utilise 3 consommables")
            .type(MissionType.DAILY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.POTION)
            .goal(3)
            .tracker(MissionTracker.CONSUMABLES_USED)
            .pointReward(150)
            .xpReward(75)
            .difficulty(2)
            .build());

        register(Mission.builder()
            .id("daily_recycle_5")
            .name("Recycleur")
            .description("Recycle 5 items")
            .type(MissionType.DAILY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.GRINDSTONE)
            .goal(5)
            .tracker(MissionTracker.ITEMS_RECYCLED)
            .pointReward(100)
            .xpReward(50)
            .difficulty(1)
            .build());

        // === SOCIAL (3 missions) ===
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
            .id("daily_group_kills_10")
            .name("Travail d'Équipe")
            .description("Tue 10 zombies avec d'autres joueurs")
            .type(MissionType.DAILY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.PLAYER_HEAD)
            .goal(10)
            .tracker(MissionTracker.GROUP_KILLS)
            .pointReward(250)
            .xpReward(125)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("daily_assists_5")
            .name("Soutien")
            .description("Assiste 5 kills d'autres joueurs")
            .type(MissionType.DAILY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.SHIELD)
            .goal(5)
            .tracker(MissionTracker.ASSISTS)
            .pointReward(200)
            .xpReward(100)
            .difficulty(2)
            .build());

        // === EVENTS (2 missions) ===
        register(Mission.builder()
            .id("daily_event_1")
            .name("Chasseur d'Événements")
            .description("Participe à un événement dynamique")
            .type(MissionType.DAILY)
            .category(MissionCategory.EVENTS)
            .icon(Material.BEACON)
            .goal(1)
            .tracker(MissionTracker.EVENTS_PARTICIPATED)
            .pointReward(300)
            .xpReward(150)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("daily_micro_event_2")
            .name("Opportuniste")
            .description("Complète 2 micro-événements")
            .type(MissionType.DAILY)
            .category(MissionCategory.EVENTS)
            .icon(Material.LIGHTNING_ROD)
            .goal(2)
            .tracker(MissionTracker.MICRO_EVENTS_COMPLETED)
            .pointReward(250)
            .xpReward(125)
            .difficulty(3)
            .build());

        // ============ MISSIONS HEBDOMADAIRES (35 missions) ============

        // === COMBAT (12 missions) ===
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
            .id("weekly_kill_2000")
            .name("Apocalypse")
            .description("Tue 2000 zombies")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.WITHER_SKELETON_SKULL)
            .goal(2000)
            .tracker(MissionTracker.ZOMBIE_KILLS)
            .pointReward(5000)
            .xpReward(2000)
            .gemReward(50)
            .difficulty(5)
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
            .id("weekly_boss_10")
            .name("Fléau des Titans")
            .description("Tue 10 boss")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.DRAGON_HEAD)
            .goal(10)
            .tracker(MissionTracker.BOSS_KILLS)
            .pointReward(5000)
            .xpReward(2000)
            .gemReward(50)
            .difficulty(5)
            .build());

        register(Mission.builder()
            .id("weekly_elite_30")
            .name("Purificateur")
            .description("Tue 30 zombies élites")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.GOLDEN_SWORD)
            .goal(30)
            .tracker(MissionTracker.ELITE_KILLS)
            .pointReward(1500)
            .xpReward(750)
            .gemReward(15)
            .difficulty(4)
            .build());

        register(Mission.builder()
            .id("weekly_headshots_100")
            .name("Maître Tireur")
            .description("Fais 100 headshots")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.SPECTRAL_ARROW)
            .goal(100)
            .tracker(MissionTracker.HEADSHOTS)
            .pointReward(1200)
            .xpReward(600)
            .gemReward(12)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_crit_100")
            .name("Exécuteur")
            .description("Tue 100 zombies avec des coups critiques")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.DIAMOND_AXE)
            .goal(100)
            .tracker(MissionTracker.CRIT_KILLS)
            .pointReward(1200)
            .xpReward(600)
            .gemReward(12)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_streak_50")
            .name("Invincible")
            .description("Atteins une série de 50 kills sans mourir")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.TOTEM_OF_UNDYING)
            .goal(50)
            .tracker(MissionTracker.KILLS_IN_STREAK)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(20)
            .difficulty(5)
            .build());

        register(Mission.builder()
            .id("weekly_night_kills_150")
            .name("Cauchemar Nocturne")
            .description("Tue 150 zombies pendant la nuit")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.ENDER_EYE)
            .goal(150)
            .tracker(MissionTracker.NIGHT_KILLS)
            .pointReward(1500)
            .xpReward(750)
            .gemReward(15)
            .difficulty(4)
            .build());

        register(Mission.builder()
            .id("weekly_ranged_200")
            .name("Sniper")
            .description("Tue 200 zombies à distance")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.CROSSBOW)
            .goal(200)
            .tracker(MissionTracker.RANGED_KILLS)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_melee_300")
            .name("Gladiateur")
            .description("Tue 300 zombies au corps à corps")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COMBAT)
            .icon(Material.NETHERITE_AXE)
            .goal(300)
            .tracker(MissionTracker.MELEE_KILLS)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        // === EXPLORATION (6 missions) ===
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
            .id("weekly_distance_10k")
            .name("Vagabond")
            .description("Parcours 10 000 blocs")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.DIAMOND_BOOTS)
            .goal(10000)
            .tracker(MissionTracker.DISTANCE_TRAVELED)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_nights_7")
            .name("Roi de la Nuit")
            .description("Survie 7 nuits complètes")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.BLACK_CANDLE)
            .goal(7)
            .tracker(MissionTracker.NIGHTS_SURVIVED)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_refuges_all")
            .name("Nomade")
            .description("Visite tous les refuges")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.CAMPFIRE)
            .goal(10)
            .tracker(MissionTracker.REFUGES_VISITED)
            .pointReward(1200)
            .xpReward(600)
            .gemReward(12)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_danger_zone_30min")
            .name("Casse-cou")
            .description("Passe 30 minutes en zones dangereuses")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.FIRE_CHARGE)
            .goal(1800)
            .tracker(MissionTracker.TIME_IN_DANGER_ZONE)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(20)
            .difficulty(5)
            .build());

        register(Mission.builder()
            .id("weekly_distance_25k")
            .name("Globe-trotter")
            .description("Parcours 25 000 blocs")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EXPLORATION)
            .icon(Material.ELYTRA)
            .goal(25000)
            .tracker(MissionTracker.DISTANCE_TRAVELED)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(20)
            .difficulty(4)
            .build());

        // === COLLECTION (6 missions) ===
        register(Mission.builder()
            .id("weekly_loot_100")
            .name("Accumulateur")
            .description("Ramasse 100 items")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.ENDER_CHEST)
            .goal(100)
            .tracker(MissionTracker.ITEMS_LOOTED)
            .pointReward(800)
            .xpReward(400)
            .gemReward(8)
            .difficulty(2)
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
            .id("weekly_legendary_1")
            .name("Légendaire")
            .description("Trouve un item Légendaire")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.NETHER_STAR)
            .goal(1)
            .tracker(MissionTracker.LEGENDARY_ITEMS_FOUND)
            .pointReward(2500)
            .xpReward(1000)
            .gemReward(25)
            .difficulty(5)
            .build());

        register(Mission.builder()
            .id("weekly_points_5000")
            .name("Capitaliste")
            .description("Gagne 5000 points")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.GOLD_BLOCK)
            .goal(5000)
            .tracker(MissionTracker.POINTS_EARNED)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_consumables_20")
            .name("Apothicaire")
            .description("Utilise 20 consommables")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.BREWING_STAND)
            .goal(20)
            .tracker(MissionTracker.CONSUMABLES_USED)
            .pointReward(1000)
            .xpReward(500)
            .gemReward(10)
            .difficulty(3)
            .build());

        register(Mission.builder()
            .id("weekly_recycle_50")
            .name("Écologiste")
            .description("Recycle 50 items")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.COLLECTION)
            .icon(Material.GRINDSTONE)
            .goal(50)
            .tracker(MissionTracker.ITEMS_RECYCLED)
            .pointReward(800)
            .xpReward(400)
            .gemReward(8)
            .difficulty(2)
            .build());

        // === SOCIAL (4 missions) ===
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
            .id("weekly_group_kills_100")
            .name("Frères d'Armes")
            .description("Tue 100 zombies avec d'autres joueurs")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.PLAYER_HEAD)
            .goal(100)
            .tracker(MissionTracker.GROUP_KILLS)
            .pointReward(1500)
            .xpReward(750)
            .gemReward(15)
            .difficulty(4)
            .build());

        register(Mission.builder()
            .id("weekly_party_kills_200")
            .name("Escouade Mortelle")
            .description("Tue 200 zombies en groupe")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.GOLDEN_HELMET)
            .goal(200)
            .tracker(MissionTracker.PARTY_KILLS)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(20)
            .difficulty(4)
            .build());

        register(Mission.builder()
            .id("weekly_assists_30")
            .name("Ange Gardien")
            .description("Assiste 30 kills d'autres joueurs")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.SOCIAL)
            .icon(Material.SHIELD)
            .goal(30)
            .tracker(MissionTracker.ASSISTS)
            .pointReward(1200)
            .xpReward(600)
            .gemReward(12)
            .difficulty(3)
            .build());

        // === EVENTS (4 missions) ===
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
            .id("weekly_blood_moon_3")
            .name("Lunatic")
            .description("Survie à 3 Blood Moons")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EVENTS)
            .icon(Material.CRYING_OBSIDIAN)
            .goal(3)
            .tracker(MissionTracker.BLOOD_MOONS_SURVIVED)
            .pointReward(4000)
            .xpReward(2000)
            .gemReward(40)
            .difficulty(5)
            .build());

        register(Mission.builder()
            .id("weekly_events_5")
            .name("Acteur de l'Ombre")
            .description("Participe à 5 événements dynamiques")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EVENTS)
            .icon(Material.BEACON)
            .goal(5)
            .tracker(MissionTracker.EVENTS_PARTICIPATED)
            .pointReward(1500)
            .xpReward(750)
            .gemReward(15)
            .difficulty(4)
            .build());

        register(Mission.builder()
            .id("weekly_micro_events_10")
            .name("Réactif")
            .description("Complète 10 micro-événements")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.EVENTS)
            .icon(Material.LIGHTNING_ROD)
            .goal(10)
            .tracker(MissionTracker.MICRO_EVENTS_COMPLETED)
            .pointReward(1200)
            .xpReward(600)
            .gemReward(12)
            .difficulty(3)
            .build());

        // === PROGRESSION (3 missions) ===
        register(Mission.builder()
            .id("weekly_level_5")
            .name("Montée en Puissance")
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

        register(Mission.builder()
            .id("weekly_daily_missions_21")
            .name("Assidu")
            .description("Complète 21 missions journalières")
            .type(MissionType.WEEKLY)
            .category(MissionCategory.PROGRESSION)
            .icon(Material.WRITABLE_BOOK)
            .goal(21)
            .tracker(MissionTracker.DAILY_MISSIONS_COMPLETED)
            .pointReward(2000)
            .xpReward(1000)
            .gemReward(20)
            .difficulty(4)
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

        // ============ TRACKER MISSIONS: MISSIONS JOURNALIÈRES COMPLÉTÉES ============
        // Tracker uniquement les missions journalières (pas les hebdomadaires)
        if (mission.getType() == MissionType.DAILY) {
            updateProgress(player, MissionTracker.DAILY_MISSIONS_COMPLETED, 1);
        }

        // ============ LEADERBOARD MISSIONS COMPLÉTÉES ============
        var lbManager = plugin.getNewLeaderboardManager();
        if (lbManager != null) {
            lbManager.incrementScore(player.getUniqueId(), player.getName(),
                com.rinaorc.zombiez.leaderboards.LeaderboardType.TOTAL_MISSIONS, 1);
        }
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

                // ============ TRACKING PÉRIODIQUE (chaque minute) ============

                // Vérifier le cycle jour/nuit pour NIGHTS_SURVIVED
                org.bukkit.World world = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0);
                if (world != null) {
                    long time = world.getTime();
                    boolean isNight = time >= 13000 && time <= 23000;

                    // Transition jour -> nuit : enregistrer les joueurs en vie
                    if (isNight && !wasNight) {
                        playersAliveAtNightfall.clear();
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            if (!player.isDead()) {
                                playersAliveAtNightfall.add(player.getUniqueId());
                            }
                        }
                    }

                    // Transition nuit -> jour : tracker les survivants
                    if (!isNight && wasNight) {
                        for (UUID uuid : playersAliveAtNightfall) {
                            Player player = plugin.getServer().getPlayer(uuid);
                            if (player != null && player.isOnline() && !player.isDead()) {
                                // Le joueur a survécu la nuit entière
                                updateProgress(player, MissionTracker.NIGHTS_SURVIVED, 1);
                            }
                        }
                        playersAliveAtNightfall.clear();
                    }

                    wasNight = isNight;
                }

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Tracker PLAYTIME (+1 minute de jeu)
                    updateProgress(player, MissionTracker.PLAYTIME, 1);

                    // Tracker TIME_IN_DANGER_ZONE (si dans une zone dangereuse)
                    var zone = plugin.getZoneManager().getPlayerZone(player);
                    if (zone != null && zone.getId() >= 5) {
                        // Zone 5+ est considérée comme "zone de danger"
                        updateProgress(player, MissionTracker.TIME_IN_DANGER_ZONE, 1);
                    }

                    // Tracker REFUGES_VISITED (si dans un refuge non encore visité)
                    var refugeManager = plugin.getRefugeManager();
                    if (refugeManager != null) {
                        var refuge = refugeManager.getRefugeAt(player.getLocation());
                        if (refuge != null) {
                            UUID uuid = player.getUniqueId();
                            Set<Integer> playerVisited = visitedRefuges.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
                            if (!playerVisited.contains(refuge.getId())) {
                                playerVisited.add(refuge.getId());
                                updateProgress(player, MissionTracker.REFUGES_VISITED, 1);
                            }
                        }
                    }
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
        // Combat de base
        ZOMBIE_KILLS,
        ELITE_KILLS,
        BOSS_KILLS,
        HEADSHOTS,
        KILLS_NO_DAMAGE,
        GROUP_KILLS,

        // Combat avancé
        CRIT_KILLS,           // Kills avec coup critique
        RANGED_KILLS,         // Kills à distance (arc)
        MELEE_KILLS,          // Kills au corps à corps
        NIGHT_KILLS,          // Kills pendant la nuit
        KILLS_IN_STREAK,      // Kills sans mourir
        QUICK_KILLS,          // Kills rapides (X en 30sec)
        DIFFERENT_ZOMBIE_TYPES, // Types de zombies différents tués
        DAMAGE_DEALT,         // Dégâts infligés

        // Exploration
        ZONES_VISITED,
        NIGHTS_SURVIVED,
        DISTANCE_TRAVELED,
        REFUGES_VISITED,      // Refuges découverts/visités
        TIME_IN_DANGER_ZONE,  // Temps passé en zone dangereuse

        // Collection
        ITEMS_LOOTED,
        RARE_ITEMS_FOUND,
        EPIC_ITEMS_FOUND,
        LEGENDARY_ITEMS_FOUND, // Items légendaires
        POINTS_EARNED,
        CONSUMABLES_USED,     // Consommables utilisés
        ITEMS_RECYCLED,       // Items recyclés

        // Social & Événements
        TRADES_COMPLETED,
        BLOOD_MOONS_SURVIVED,
        EVENTS_PARTICIPATED,  // Participation aux événements dynamiques
        MICRO_EVENTS_COMPLETED, // Micro-événements complétés
        PARTY_KILLS,          // Kills en groupe/party
        ASSISTS,              // Assistances

        // Progression
        LEVELS_GAINED,
        PLAYTIME,
        DAILY_MISSIONS_COMPLETED, // Missions journalières complétées
        ACHIEVEMENTS_UNLOCKED,    // Achievements débloqués
        TALENTS_USED,            // Utilisations de talents
        DEATHS,                  // Morts (pour les défis inversés)
        RESPAWNS_AT_REFUGE,      // Respawn au refuge

        // Forge
        FORGE_SUCCESS,           // Forges réussies
        FORGE_MAX_LEVEL          // Items forgés +10
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
