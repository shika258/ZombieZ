package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système d'achievements ultra-complet avec 75+ objectifs
 *
 * Catégories:
 * - COMBAT: Kills, streaks, boss, élites
 * - EXPLORATION: Zones, survie, temps de jeu
 * - COLLECTION: Items, points, richesse
 * - SOCIAL: Équipe, trades, aide
 * - EVENTS: Blood Moon, hordes, événements
 * - PROGRESSION: Niveaux, prestige, compétences
 * - MASTERY: Objectifs extrêmes pour les hardcore gamers
 *
 * Tiers: Bronze → Argent → Or → Diamant → Légendaire → Mythique
 */
public class AchievementManager {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<String, Achievement> achievements;

    @Getter
    private final Map<AchievementCategory, List<Achievement>> byCategory;

    @Getter
    private final Map<AchievementTier, List<Achievement>> byTier;

    // Milestones pour broadcasts
    private static final int[] MILESTONES = {5, 10, 25, 50, 75, 100};

    // Cache pour éviter les broadcasts répétés
    private final Set<String> broadcastedMilestones = ConcurrentHashMap.newKeySet();

    public AchievementManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.achievements = new LinkedHashMap<>();
        this.byCategory = new EnumMap<>(AchievementCategory.class);
        this.byTier = new EnumMap<>(AchievementTier.class);

        registerAllAchievements();
        organizeByCategory();
        organizeByTier();
    }

    /**
     * Enregistre tous les achievements (75+)
     */
    private void registerAllAchievements() {
        registerCombatAchievements();
        registerExplorationAchievements();
        registerCollectionAchievements();
        registerSocialAchievements();
        registerEventsAchievements();
        registerProgressionAchievements();
        registerMasteryAchievements();
    }

    // ==================== COMBAT (20 achievements) ====================
    private void registerCombatAchievements() {
        // Kills de base - Progression
        register(Achievement.builder("first_blood")
            .name("Premier Sang")
            .description("Tue ton premier zombie")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.BRONZE)
            .icon(Material.WOODEN_SWORD)
            .requirement(1)
            .pointReward(25)
            .gemReward(1)
            .build());

        register(Achievement.builder("zombie_hunter_1")
            .name("Chasseur I")
            .description("Tue 50 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.BRONZE)
            .icon(Material.STONE_SWORD)
            .requirement(50)
            .pointReward(100)
            .gemReward(3)
            .build());

        register(Achievement.builder("zombie_hunter_2")
            .name("Chasseur II")
            .description("Tue 250 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.BRONZE)
            .icon(Material.IRON_SWORD)
            .requirement(250)
            .pointReward(250)
            .gemReward(5)
            .build());

        register(Achievement.builder("zombie_slayer_1")
            .name("Tueur de Zombies I")
            .description("Tue 1,000 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.SILVER)
            .icon(Material.IRON_SWORD)
            .requirement(1000)
            .pointReward(500)
            .gemReward(10)
            .build());

        register(Achievement.builder("zombie_slayer_2")
            .name("Tueur de Zombies II")
            .description("Tue 5,000 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.GOLD)
            .icon(Material.DIAMOND_SWORD)
            .requirement(5000)
            .pointReward(1500)
            .gemReward(25)
            .build());

        register(Achievement.builder("zombie_slayer_3")
            .name("Tueur de Zombies III")
            .description("Tue 25,000 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.NETHERITE_SWORD)
            .requirement(25000)
            .pointReward(5000)
            .gemReward(75)
            .build());

        register(Achievement.builder("exterminator")
            .name("Exterminateur")
            .description("Tue 100,000 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.NETHERITE_SWORD)
            .requirement(100000)
            .pointReward(15000)
            .gemReward(200)
            .title("§4☠ Exterminateur")
            .build());

        register(Achievement.builder("genocide")
            .name("Génocide")
            .description("Tue 500,000 zombies")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.MYTHIC)
            .icon(Material.WITHER_SKELETON_SKULL)
            .requirement(500000)
            .pointReward(50000)
            .gemReward(500)
            .title("§4§l☠ Fléau des Morts")
            .build());

        // Kills spéciaux
        register(Achievement.builder("elite_hunter_1")
            .name("Chasseur d'Élites I")
            .description("Tue 25 zombies élites")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.SILVER)
            .icon(Material.GOLDEN_SWORD)
            .requirement(25)
            .pointReward(400)
            .gemReward(10)
            .build());

        register(Achievement.builder("elite_hunter_2")
            .name("Chasseur d'Élites II")
            .description("Tue 100 zombies élites")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.GOLD)
            .icon(Material.GOLDEN_SWORD)
            .requirement(100)
            .pointReward(1000)
            .gemReward(30)
            .build());

        register(Achievement.builder("elite_master")
            .name("Maître des Élites")
            .description("Tue 500 zombies élites")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.GOLDEN_SWORD)
            .requirement(500)
            .pointReward(3000)
            .gemReward(75)
            .build());

        // Boss
        register(Achievement.builder("boss_slayer_1")
            .name("Tueur de Boss I")
            .description("Tue 5 boss")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.SILVER)
            .icon(Material.DRAGON_HEAD)
            .requirement(5)
            .pointReward(750)
            .gemReward(20)
            .build());

        register(Achievement.builder("boss_slayer_2")
            .name("Tueur de Boss II")
            .description("Tue 25 boss")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.GOLD)
            .icon(Material.DRAGON_HEAD)
            .requirement(25)
            .pointReward(2500)
            .gemReward(50)
            .build());

        register(Achievement.builder("boss_master")
            .name("Maître des Boss")
            .description("Tue 100 boss")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.DRAGON_HEAD)
            .requirement(100)
            .pointReward(10000)
            .gemReward(150)
            .title("§5⚔ Tombeur de Titans")
            .build());

        register(Achievement.builder("patient_zero")
            .name("Patient Zéro Vaincu")
            .description("Vaincs Patient Zéro")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.WITHER_SKELETON_SKULL)
            .requirement(1)
            .pointReward(10000)
            .gemReward(250)
            .title("§d✦ Survivant Ultime")
            .build());

        // Streaks
        register(Achievement.builder("killing_spree_1")
            .name("Série de Kills I")
            .description("Tue 15 zombies sans mourir")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.BRONZE)
            .icon(Material.BLAZE_POWDER)
            .requirement(15)
            .pointReward(100)
            .gemReward(3)
            .build());

        register(Achievement.builder("killing_spree_2")
            .name("Série de Kills II")
            .description("Tue 50 zombies sans mourir")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.SILVER)
            .icon(Material.BLAZE_POWDER)
            .requirement(50)
            .pointReward(300)
            .gemReward(8)
            .build());

        register(Achievement.builder("unstoppable")
            .name("Inarrêtable")
            .description("Tue 100 zombies sans mourir")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.GOLD)
            .icon(Material.NETHER_STAR)
            .requirement(100)
            .pointReward(750)
            .gemReward(25)
            .build());

        register(Achievement.builder("immortal")
            .name("Immortel")
            .description("Tue 250 zombies sans mourir")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.TOTEM_OF_UNDYING)
            .requirement(250)
            .pointReward(2500)
            .gemReward(60)
            .build());

        register(Achievement.builder("deathless")
            .name("Sans Mort")
            .description("Tue 500 zombies sans mourir")
            .category(AchievementCategory.COMBAT)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.END_CRYSTAL)
            .requirement(500)
            .pointReward(7500)
            .gemReward(150)
            .title("§6✧ L'Invincible")
            .build());
    }

    // ==================== EXPLORATION (15 achievements) ====================
    private void registerExplorationAchievements() {
        register(Achievement.builder("first_steps")
            .name("Premiers Pas")
            .description("Entre dans ta première zone")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.LEATHER_BOOTS)
            .requirement(1)
            .pointReward(25)
            .gemReward(1)
            .build());

        register(Achievement.builder("explorer_1")
            .name("Explorateur I")
            .description("Visite 3 zones différentes")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.COMPASS)
            .requirement(3)
            .pointReward(75)
            .gemReward(3)
            .build());

        register(Achievement.builder("explorer_2")
            .name("Explorateur II")
            .description("Visite 7 zones différentes")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.SILVER)
            .icon(Material.COMPASS)
            .requirement(7)
            .pointReward(200)
            .gemReward(8)
            .build());

        register(Achievement.builder("world_traveler")
            .name("Globe-Trotter")
            .description("Visite toutes les zones (11)")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.GOLD)
            .icon(Material.FILLED_MAP)
            .requirement(11)
            .pointReward(1000)
            .gemReward(30)
            .build());

        register(Achievement.builder("danger_zone_1")
            .name("Zone Dangereuse I")
            .description("Atteins la Zone 5")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.SILVER)
            .icon(Material.FIRE_CHARGE)
            .requirement(5)
            .pointReward(300)
            .gemReward(10)
            .build());

        register(Achievement.builder("danger_zone_2")
            .name("Zone Dangereuse II")
            .description("Atteins la Zone 8")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.GOLD)
            .icon(Material.FIRE_CHARGE)
            .requirement(8)
            .pointReward(750)
            .gemReward(25)
            .build());

        register(Achievement.builder("hell_walker")
            .name("Marcheur des Enfers")
            .description("Atteins la Zone 11")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.NETHERITE_INGOT)
            .requirement(11)
            .pointReward(2500)
            .gemReward(60)
            .title("§c⚡ Marcheur des Enfers")
            .build());

        // Temps de survie
        register(Achievement.builder("survivor_1")
            .name("Survivant I")
            .description("Survie 1 heure en jeu")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.CLOCK)
            .requirement(3600) // 1h en secondes
            .pointReward(50)
            .gemReward(2)
            .build());

        register(Achievement.builder("survivor_2")
            .name("Survivant II")
            .description("Survie 10 heures en jeu")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.SILVER)
            .icon(Material.CLOCK)
            .requirement(36000)
            .pointReward(300)
            .gemReward(10)
            .build());

        register(Achievement.builder("survivor_3")
            .name("Survivant III")
            .description("Survie 50 heures en jeu")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.GOLD)
            .icon(Material.CLOCK)
            .requirement(180000)
            .pointReward(1000)
            .gemReward(30)
            .build());

        register(Achievement.builder("veteran")
            .name("Vétéran")
            .description("Survie 100 heures en jeu")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.CLOCK)
            .requirement(360000)
            .pointReward(3000)
            .gemReward(75)
            .build());

        register(Achievement.builder("ancient")
            .name("Ancien")
            .description("Survie 500 heures en jeu")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.CLOCK)
            .requirement(1800000)
            .pointReward(10000)
            .gemReward(200)
            .title("§e⏳ L'Ancien")
            .build());

        // Survie consécutive
        register(Achievement.builder("night_walker")
            .name("Marcheur Nocturne")
            .description("Survie 10 nuits complètes")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.SILVER)
            .icon(Material.BLACK_CANDLE)
            .requirement(10)
            .pointReward(200)
            .gemReward(8)
            .build());

        register(Achievement.builder("zone_master")
            .name("Maître de Zone")
            .description("Passe 5 heures dans une zone difficile (8+)")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.GOLD)
            .icon(Material.LODESTONE)
            .requirement(18000)
            .pointReward(750)
            .gemReward(25)
            .build());

        register(Achievement.builder("distance_walker")
            .name("Grand Marcheur")
            .description("Parcours 100km")
            .category(AchievementCategory.EXPLORATION)
            .tier(AchievementTier.GOLD)
            .icon(Material.IRON_BOOTS)
            .requirement(100000) // 100km en blocs
            .pointReward(500)
            .gemReward(15)
            .build());
    }

    // ==================== COLLECTION (12 achievements) ====================
    private void registerCollectionAchievements() {
        // Points
        register(Achievement.builder("earning_1")
            .name("Premiers Gains")
            .description("Gagne 1,000 points au total")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.GOLD_NUGGET)
            .requirement(1000)
            .pointReward(50)
            .gemReward(2)
            .build());

        register(Achievement.builder("earning_2")
            .name("Petit Épargnant")
            .description("Gagne 25,000 points au total")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.SILVER)
            .icon(Material.GOLD_INGOT)
            .requirement(25000)
            .pointReward(200)
            .gemReward(8)
            .build());

        register(Achievement.builder("earning_3")
            .name("Investisseur")
            .description("Gagne 100,000 points au total")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.GOLD)
            .icon(Material.GOLD_BLOCK)
            .requirement(100000)
            .pointReward(750)
            .gemReward(25)
            .build());

        register(Achievement.builder("millionaire")
            .name("Millionnaire")
            .description("Gagne 1,000,000 points au total")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.DIAMOND_BLOCK)
            .requirement(1000000)
            .pointReward(5000)
            .gemReward(100)
            .build());

        register(Achievement.builder("billionaire")
            .name("Milliardaire")
            .description("Gagne 10,000,000 points au total")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.NETHERITE_BLOCK)
            .requirement(10000000)
            .pointReward(25000)
            .gemReward(300)
            .title("§e§l💰 Milliardaire")
            .build());

        // Items
        register(Achievement.builder("loot_collector_1")
            .name("Collectionneur I")
            .description("Ramasse 100 items")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.CHEST)
            .requirement(100)
            .pointReward(75)
            .gemReward(3)
            .build());

        register(Achievement.builder("loot_collector_2")
            .name("Collectionneur II")
            .description("Ramasse 500 items")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.SILVER)
            .icon(Material.CHEST)
            .requirement(500)
            .pointReward(250)
            .gemReward(10)
            .build());

        register(Achievement.builder("hoarder")
            .name("Amasseur")
            .description("Ramasse 2,500 items")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.GOLD)
            .icon(Material.ENDER_CHEST)
            .requirement(2500)
            .pointReward(750)
            .gemReward(25)
            .build());

        // Raretés
        register(Achievement.builder("rare_finder")
            .name("Trouveur de Raretés")
            .description("Trouve 10 items Épiques ou mieux")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.GOLD)
            .icon(Material.AMETHYST_SHARD)
            .requirement(10)
            .pointReward(500)
            .gemReward(20)
            .build());

        register(Achievement.builder("legendary_luck")
            .name("Chance Légendaire")
            .description("Trouve 5 items Légendaires")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.NETHER_STAR)
            .requirement(5)
            .pointReward(2000)
            .gemReward(50)
            .build());

        register(Achievement.builder("mythic_finder")
            .name("Trouveur Mythique")
            .description("Trouve un item Mythique")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.END_CRYSTAL)
            .requirement(1)
            .pointReward(5000)
            .gemReward(100)
            .title("§d✦ Béni des Dieux")
            .build());

        register(Achievement.builder("set_collector")
            .name("Collectionneur de Sets")
            .description("Complète 3 sets d'équipement")
            .category(AchievementCategory.COLLECTION)
            .tier(AchievementTier.GOLD)
            .icon(Material.ARMOR_STAND)
            .requirement(3)
            .pointReward(1500)
            .gemReward(40)
            .build());
    }

    // ==================== SOCIAL (10 achievements) ====================
    private void registerSocialAchievements() {
        register(Achievement.builder("first_friend")
            .name("Premier Ami")
            .description("Rejoins une partie avec un ami")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.BRONZE)
            .icon(Material.PLAYER_HEAD)
            .requirement(1)
            .pointReward(50)
            .gemReward(2)
            .build());

        register(Achievement.builder("team_player_1")
            .name("Joueur d'Équipe I")
            .description("Tue 10 zombies en groupe")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.BRONZE)
            .icon(Material.SHIELD)
            .requirement(10)
            .pointReward(75)
            .gemReward(3)
            .build());

        register(Achievement.builder("team_player_2")
            .name("Joueur d'Équipe II")
            .description("Tue 100 zombies en groupe")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.SILVER)
            .icon(Material.SHIELD)
            .requirement(100)
            .pointReward(300)
            .gemReward(10)
            .build());

        register(Achievement.builder("group_boss")
            .name("Boss en Équipe")
            .description("Tue 10 boss en groupe")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.GOLD)
            .icon(Material.DRAGON_HEAD)
            .requirement(10)
            .pointReward(1000)
            .gemReward(30)
            .build());

        register(Achievement.builder("reviver_1")
            .name("Réanimateur I")
            .description("Aide 5 joueurs tombés")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.SILVER)
            .icon(Material.TOTEM_OF_UNDYING)
            .requirement(5)
            .pointReward(200)
            .gemReward(8)
            .build());

        register(Achievement.builder("reviver_2")
            .name("Réanimateur II")
            .description("Aide 25 joueurs tombés")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.GOLD)
            .icon(Material.TOTEM_OF_UNDYING)
            .requirement(25)
            .pointReward(750)
            .gemReward(25)
            .build());

        register(Achievement.builder("guardian_angel")
            .name("Ange Gardien")
            .description("Aide 100 joueurs tombés")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.TOTEM_OF_UNDYING)
            .requirement(100)
            .pointReward(3000)
            .gemReward(75)
            .title("§b✦ Ange Gardien")
            .build());

        register(Achievement.builder("trader_1")
            .name("Marchand I")
            .description("Effectue 10 échanges")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.BRONZE)
            .icon(Material.EMERALD)
            .requirement(10)
            .pointReward(100)
            .gemReward(5)
            .build());

        register(Achievement.builder("trader_2")
            .name("Marchand II")
            .description("Effectue 50 échanges")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.SILVER)
            .icon(Material.EMERALD_BLOCK)
            .requirement(50)
            .pointReward(400)
            .gemReward(15)
            .build());

        register(Achievement.builder("merchant_king")
            .name("Roi Marchand")
            .description("Effectue 200 échanges")
            .category(AchievementCategory.SOCIAL)
            .tier(AchievementTier.GOLD)
            .icon(Material.EMERALD_BLOCK)
            .requirement(200)
            .pointReward(1500)
            .gemReward(40)
            .title("§a§l💎 Roi Marchand")
            .build());
    }

    // ==================== ÉVÉNEMENTS (10 achievements) ====================
    private void registerEventsAchievements() {
        register(Achievement.builder("first_event")
            .name("Premier Événement")
            .description("Participe à un événement")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.BRONZE)
            .icon(Material.BEACON)
            .requirement(1)
            .pointReward(50)
            .gemReward(2)
            .build());

        register(Achievement.builder("blood_moon_survivor_1")
            .name("Survivant Blood Moon I")
            .description("Survie à une Blood Moon complète")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.SILVER)
            .icon(Material.REDSTONE)
            .requirement(1)
            .pointReward(400)
            .gemReward(15)
            .build());

        register(Achievement.builder("blood_moon_survivor_2")
            .name("Survivant Blood Moon II")
            .description("Survie à 10 Blood Moons")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.GOLD)
            .icon(Material.REDSTONE_BLOCK)
            .requirement(10)
            .pointReward(1500)
            .gemReward(50)
            .build());

        register(Achievement.builder("blood_moon_master")
            .name("Maître Blood Moon")
            .description("Survie à 50 Blood Moons")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.REDSTONE_BLOCK)
            .requirement(50)
            .pointReward(7500)
            .gemReward(150)
            .title("§4§l🌙 Lune de Sang")
            .build());

        register(Achievement.builder("horde_breaker_1")
            .name("Briseur de Horde I")
            .description("Élimine 25% d'une horde")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.SILVER)
            .icon(Material.TNT)
            .requirement(1)
            .pointReward(300)
            .gemReward(10)
            .build());

        register(Achievement.builder("horde_breaker_2")
            .name("Briseur de Horde II")
            .description("Élimine 50% d'une horde seul")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.GOLD)
            .icon(Material.TNT)
            .requirement(1)
            .pointReward(1000)
            .gemReward(35)
            .build());

        register(Achievement.builder("horde_destroyer")
            .name("Destructeur de Horde")
            .description("Élimine une horde entière")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.TNT_MINECART)
            .requirement(1)
            .pointReward(3000)
            .gemReward(75)
            .build());

        register(Achievement.builder("event_veteran")
            .name("Vétéran des Événements")
            .description("Participe à 50 événements")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.GOLD)
            .icon(Material.BEACON)
            .requirement(50)
            .pointReward(1500)
            .gemReward(40)
            .build());

        register(Achievement.builder("event_champion")
            .name("Champion des Événements")
            .description("Participe à 200 événements")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.BEACON)
            .requirement(200)
            .pointReward(5000)
            .gemReward(100)
            .build());

        register(Achievement.builder("event_legend")
            .name("Légende des Événements")
            .description("Participe à 500 événements")
            .category(AchievementCategory.EVENTS)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.BEACON)
            .requirement(500)
            .pointReward(15000)
            .gemReward(250)
            .title("§5§l⚡ Légende Vivante")
            .build());
    }

    // ==================== PROGRESSION (12 achievements) ====================
    private void registerProgressionAchievements() {
        // Niveaux
        register(Achievement.builder("level_10")
            .name("Niveau 10")
            .description("Atteins le niveau 10")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(10)
            .pointReward(100)
            .gemReward(5)
            .build());

        register(Achievement.builder("level_25")
            .name("Niveau 25")
            .description("Atteins le niveau 25")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.BRONZE)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(25)
            .pointReward(250)
            .gemReward(10)
            .build());

        register(Achievement.builder("level_50")
            .name("Niveau 50")
            .description("Atteins le niveau 50")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.SILVER)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(50)
            .pointReward(500)
            .gemReward(20)
            .build());

        register(Achievement.builder("level_75")
            .name("Niveau 75")
            .description("Atteins le niveau 75")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.GOLD)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(75)
            .pointReward(1000)
            .gemReward(35)
            .build());

        register(Achievement.builder("level_100")
            .name("Niveau Maximum")
            .description("Atteins le niveau 100")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(100)
            .pointReward(3000)
            .gemReward(75)
            .build());

        // Prestige
        register(Achievement.builder("prestige_1")
            .name("Premier Prestige")
            .description("Effectue ton premier prestige")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.GOLD)
            .icon(Material.GOLDEN_APPLE)
            .requirement(1)
            .pointReward(2500)
            .gemReward(75)
            .build());

        register(Achievement.builder("prestige_5")
            .name("Prestige V")
            .description("Atteins le prestige 5")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.ENCHANTED_GOLDEN_APPLE)
            .requirement(5)
            .pointReward(7500)
            .gemReward(150)
            .build());

        register(Achievement.builder("prestige_10")
            .name("Prestige Maximum")
            .description("Atteins le prestige 10")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.MYTHIC)
            .icon(Material.ENCHANTED_GOLDEN_APPLE)
            .requirement(10)
            .pointReward(25000)
            .gemReward(500)
            .title("§6§l✦ Maître Suprême ✦")
            .build());

        // XP
        register(Achievement.builder("xp_grinder_1")
            .name("Grindeur XP I")
            .description("Gagne 100,000 XP au total")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.SILVER)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(100000)
            .pointReward(300)
            .gemReward(10)
            .build());

        register(Achievement.builder("xp_grinder_2")
            .name("Grindeur XP II")
            .description("Gagne 1,000,000 XP au total")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.GOLD)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(1000000)
            .pointReward(1500)
            .gemReward(40)
            .build());

        register(Achievement.builder("xp_master")
            .name("Maître de l'XP")
            .description("Gagne 10,000,000 XP au total")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.DIAMOND)
            .icon(Material.EXPERIENCE_BOTTLE)
            .requirement(10000000)
            .pointReward(5000)
            .gemReward(100)
            .build());

        // Skills
        register(Achievement.builder("skill_master")
            .name("Maître des Skills")
            .description("Débloque 15 compétences")
            .category(AchievementCategory.PROGRESSION)
            .tier(AchievementTier.GOLD)
            .icon(Material.BOOK)
            .requirement(15)
            .pointReward(1000)
            .gemReward(30)
            .build());
    }

    // ==================== MASTERY - Ultra Hardcore (8 achievements) ====================
    private void registerMasteryAchievements() {
        register(Achievement.builder("dedication")
            .name("Dévouement Total")
            .description("Joue 30 jours consécutifs")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.DIAMOND)
            .requirement(30)
            .pointReward(10000)
            .gemReward(200)
            .title("§b§l✦ Le Dévoué")
            .build());

        register(Achievement.builder("completionist")
            .name("Complétionniste")
            .description("Débloque 50 achievements")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.NETHER_STAR)
            .requirement(50)
            .pointReward(15000)
            .gemReward(250)
            .title("§e§l★ Complétionniste")
            .build());

        register(Achievement.builder("true_master")
            .name("Vrai Maître")
            .description("Débloque 75 achievements")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.MYTHIC)
            .icon(Material.END_CRYSTAL)
            .requirement(75)
            .pointReward(30000)
            .gemReward(500)
            .title("§d§l✦ Vrai Maître ✦")
            .build());

        register(Achievement.builder("perfectionist")
            .name("Perfectionniste")
            .description("Débloque TOUS les achievements")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.MYTHIC)
            .icon(Material.DRAGON_EGG)
            .requirement(1) // Special: checked against total
            .pointReward(100000)
            .gemReward(1000)
            .title("§6§l⚜ PERFECTIONNISTE ⚜")
            .build());

        register(Achievement.builder("no_death_run")
            .name("Run Sans Mort")
            .description("Atteins le niveau 50 sans mourir")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.TOTEM_OF_UNDYING)
            .requirement(50)
            .pointReward(20000)
            .gemReward(400)
            .title("§6§l⚔ Intouchable")
            .build());

        register(Achievement.builder("speedrunner")
            .name("Speedrunner")
            .description("Atteins le niveau 100 en moins de 24h de jeu")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.CLOCK)
            .requirement(1)
            .pointReward(15000)
            .gemReward(300)
            .title("§a§l⚡ Speedrunner")
            .build());

        register(Achievement.builder("wealthy")
            .name("Richissime")
            .description("Possède 10,000,000 points simultanément")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.LEGENDARY)
            .icon(Material.GOLD_BLOCK)
            .requirement(10000000)
            .pointReward(0) // Pas de récompense, déjà riche!
            .gemReward(200)
            .title("§e§l💎 Richissime")
            .build());

        register(Achievement.builder("legend")
            .name("Légende")
            .description("Tue 1,000,000 zombies")
            .category(AchievementCategory.MASTERY)
            .tier(AchievementTier.MYTHIC)
            .icon(Material.DRAGON_HEAD)
            .requirement(1000000)
            .pointReward(100000)
            .gemReward(1000)
            .title("§4§l☠ LA LÉGENDE ☠")
            .build());
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
     * Organise les achievements par tier
     */
    private void organizeByTier() {
        for (AchievementTier tier : AchievementTier.values()) {
            byTier.put(tier, new ArrayList<>());
        }

        for (Achievement achievement : achievements.values()) {
            byTier.get(achievement.tier()).add(achievement);
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

        if (data.hasAchievement(achievementId)) return false;

        if (progress >= achievement.requirement()) {
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

        if (newProgress >= achievement.requirement()) {
            unlock(player, achievement);
        }
    }

    /**
     * Définit directement le progrès d'un achievement
     */
    public void setProgress(Player player, String achievementId, int progress) {
        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        if (data.hasAchievement(achievementId)) return;

        data.setAchievementProgress(achievementId, progress);

        if (progress >= achievement.requirement()) {
            unlock(player, achievement);
        }
    }

    /**
     * Débloque un achievement avec tous les effets
     */
    public boolean unlock(Player player, Achievement achievement) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        if (data.hasAchievement(achievement.id())) return false;

        // Marquer comme débloqué
        data.unlockAchievement(achievement.id());

        // Notification principale
        sendUnlockNotification(player, achievement);

        // Récompenses
        applyRewards(player, achievement, data);

        // Effets visuels
        spawnTierEffects(player, achievement.tier());

        // Sons
        playUnlockSound(player, achievement.tier());

        // Broadcast si achievement rare
        if (achievement.tier().ordinal() >= AchievementTier.LEGENDARY.ordinal()) {
            broadcastRareAchievement(player, achievement);
        }

        // Vérifier les milestones
        checkMilestones(player);

        // Notifier le système de Parcours (Journey)
        if (plugin.getJourneyListener() != null) {
            int totalAchievements = getUnlockedCount(player);
            plugin.getJourneyListener().onAchievementUnlock(player, totalAchievements);
        }

        // Mettre à jour le leaderboard d'achievements
        var lbManager = plugin.getNewLeaderboardManager();
        if (lbManager != null) {
            int totalAchievements = getUnlockedCount(player);
            lbManager.updateScore(player.getUniqueId(), player.getName(),
                com.rinaorc.zombiez.leaderboards.LeaderboardType.ACHIEVEMENTS, totalAchievements);
        }

        // Débloquer le titre si présent
        if (achievement.title() != null && !achievement.title().isEmpty()) {
            data.addTitle(achievement.id());
        }

        return true;
    }

    /**
     * Envoie la notification de déblocage
     */
    private void sendUnlockNotification(Player player, Achievement achievement) {
        // Title principal
        player.sendTitle(
            "§6§l✦ ACHIEVEMENT ✦",
            achievement.tier().getColor() + achievement.name(),
            10, 70, 20
        );

        // Messages détaillés
        player.sendMessage("");
        player.sendMessage("§8§m                                                            ");
        player.sendMessage("");
        player.sendMessage("       §6§l★ " + achievement.tier().getColor() + "§lACHIEVEMENT DÉBLOQUÉ! §6§l★");
        player.sendMessage("");
        player.sendMessage("       " + achievement.tier().getColor() + "§l" + achievement.name());
        player.sendMessage("       §7" + achievement.description());
        player.sendMessage("");
        player.sendMessage("       §7Tier: " + achievement.tier().getColor() + achievement.tier().getDisplayName());
        player.sendMessage("");
        player.sendMessage("       §e§l+" + formatNumber(achievement.pointReward()) + " Points " +
                          "§8| §d§l+" + achievement.gemReward() + " Gemmes");

        if (achievement.title() != null && !achievement.title().isEmpty()) {
            player.sendMessage("       §7Titre débloqué: " + achievement.title());
        }

        player.sendMessage("");
        player.sendMessage("§8§m                                                            ");
        player.sendMessage("");
    }

    /**
     * Applique les récompenses
     */
    private void applyRewards(Player player, Achievement achievement, PlayerData data) {
        plugin.getEconomyManager().addPoints(player, achievement.pointReward());
        plugin.getEconomyManager().addGems(player, achievement.gemReward());
    }

    /**
     * Joue le son de déblocage selon le tier
     */
    private void playUnlockSound(Player player, AchievementTier tier) {
        switch (tier) {
            case BRONZE -> player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            case SILVER -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            case GOLD -> player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            case DIAMOND -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
            }
            case LEGENDARY -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.3f, 1.5f);
            }
            case MYTHIC -> {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.6f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);
            }
        }
    }

    /**
     * Effets visuels selon le tier
     */
    private void spawnTierEffects(Player player, AchievementTier tier) {
        var loc = player.getLocation().add(0, 1, 0);

        switch (tier) {
            case BRONZE -> loc.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER, loc, 25, 0.5, 0.5, 0.5);
            case SILVER -> loc.getWorld().spawnParticle(
                Particle.FIREWORK, loc, 40, 0.5, 0.5, 0.5, 0.1);
            case GOLD -> {
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.5, 0.5, 0.5, 0.1);
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 30, 0.3, 0.3, 0.3, 0.05);
            }
            case DIAMOND -> {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 1, 1, 1, 0.1);
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.5, 0.5, 0.5, 0);
            }
            case LEGENDARY -> {
                spawnLegendaryEffects(player);
            }
            case MYTHIC -> {
                spawnMythicEffects(player);
            }
        }
    }

    /**
     * Effets légendaires (feu d'artifice)
     */
    private void spawnLegendaryEffects(Player player) {
        Location loc = player.getLocation();

        // Particules immédiates
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0.2);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 75, 1, 1, 1, 0.5);

        // Feu d'artifice
        Firework fw = loc.getWorld().spawn(loc.clone().add(0, 1, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(Color.PURPLE, Color.FUCHSIA)
            .withFade(Color.WHITE)
            .trail(true)
            .flicker(true)
            .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        new BukkitRunnable() {
            @Override
            public void run() {
                fw.detonate();
            }
        }.runTaskLater(plugin, 2L);
    }

    /**
     * Effets mythiques (encore plus épiques!)
     */
    private void spawnMythicEffects(Player player) {
        Location loc = player.getLocation();

        // Particules immédiates massives
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 150, 2, 2, 2, 0.3);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 100, 1.5, 1.5, 1.5, 0.7);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 0.5, 0), 50, 1, 0.5, 1, 0);

        // Multiple feux d'artifice
        for (int i = 0; i < 3; i++) {
            final int delay = i * 5;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Firework fw = loc.getWorld().spawn(loc.clone().add(
                        Math.random() * 2 - 1, 1, Math.random() * 2 - 1), Firework.class);
                    FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.STAR)
                        .withColor(Color.ORANGE, Color.YELLOW, Color.RED)
                        .withFade(Color.WHITE, Color.PURPLE)
                        .trail(true)
                        .flicker(true)
                        .build());
                    meta.setPower(0);
                    fw.setFireworkMeta(meta);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            fw.detonate();
                        }
                    }.runTaskLater(plugin, 2L);
                }
            }.runTaskLater(plugin, delay);
        }
    }

    /**
     * Broadcast pour les achievements rares
     */
    private void broadcastRareAchievement(Player player, Achievement achievement) {
        String tierPrefix = achievement.tier() == AchievementTier.MYTHIC ? "§6§l✦ §d§lMYTHIQUE! §6§l✦" : "§d§l★ LÉGENDAIRE! ★";

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage("§8§m                                                  ");
            p.sendMessage("");
            p.sendMessage("  " + tierPrefix);
            p.sendMessage("");
            p.sendMessage("  §e" + player.getName() + " §7a débloqué");
            p.sendMessage("  " + achievement.tier().getColor() + "§l" + achievement.name());
            p.sendMessage("");
            p.sendMessage("§8§m                                                  ");
            p.sendMessage("");

            if (p != player) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        }
    }

    /**
     * Vérifie et célèbre les milestones
     */
    private void checkMilestones(Player player) {
        int totalUnlocked = getUnlockedCount(player);

        for (int milestone : MILESTONES) {
            if (totalUnlocked == milestone) {
                String key = player.getUniqueId() + "_" + milestone;

                if (!broadcastedMilestones.contains(key)) {
                    broadcastedMilestones.add(key);
                    celebrateMilestone(player, milestone);
                }
                break;
            }
        }
    }

    /**
     * Célèbre un milestone atteint
     */
    private void celebrateMilestone(Player player, int milestone) {
        // Récompense bonus
        int bonusPoints = milestone * 100;
        int bonusGems = milestone / 2;

        plugin.getEconomyManager().addPoints(player, bonusPoints);
        plugin.getEconomyManager().addGems(player, bonusGems);

        // Notification au joueur
        player.sendTitle(
            "§6§l✦ MILESTONE! ✦",
            "§e" + milestone + " achievements débloqués!",
            10, 70, 20
        );

        player.sendMessage("");
        player.sendMessage("§6§l✦ MILESTONE ATTEINT! ✦");
        player.sendMessage("§7Tu as débloqué §e" + milestone + " achievements§7!");
        player.sendMessage("§7Bonus: §e+" + formatNumber(bonusPoints) + " Points §8| §d+" + bonusGems + " Gemmes");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);

        // Broadcast à tous
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p != player) {
                p.sendMessage("");
                p.sendMessage("§6§l✦ §e" + player.getName() + " §7a atteint §e" + milestone + " achievements§7! §6§l✦");
                p.sendMessage("");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.3f);
            }
        }

        // Effets visuels
        spawnMilestoneEffects(player);
    }

    /**
     * Effets visuels pour les milestones
     */
    private void spawnMilestoneEffects(Player player) {
        Location loc = player.getLocation();

        // Spirale de particules
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 3; i++) {
                    double x = Math.cos(angle + i * 2) * (1 + ticks * 0.03);
                    double z = Math.sin(angle + i * 2) * (1 + ticks * 0.03);
                    double y = ticks * 0.05;

                    loc.getWorld().spawnParticle(
                        Particle.END_ROD,
                        loc.clone().add(x, y, z),
                        1, 0, 0, 0, 0
                    );
                }

                angle += 0.3;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== UTILITAIRES ====================

    /**
     * Obtient le nombre d'achievements débloqués
     */
    public int getUnlockedCount(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return (int) achievements.keySet().stream()
            .filter(data::hasAchievement)
            .count();
    }

    /**
     * Obtient le nombre d'achievements débloqués par catégorie
     */
    public int getUnlockedCountByCategory(Player player, AchievementCategory category) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return (int) byCategory.get(category).stream()
            .filter(a -> data.hasAchievement(a.id()))
            .count();
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
     * Obtient le pourcentage de complétion par catégorie
     */
    public double getCompletionPercentByCategory(Player player, AchievementCategory category) {
        int unlocked = getUnlockedCountByCategory(player, category);
        int total = byCategory.get(category).size();
        return (double) unlocked / total * 100;
    }

    /**
     * Obtient les achievements récents débloqués (dernières 24h simulé)
     */
    public List<Achievement> getRecentlyUnlocked(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return List.of();

        return achievements.values().stream()
            .filter(a -> data.hasAchievement(a.id()))
            .limit(5)
            .toList();
    }

    /**
     * Obtient le prochain achievement proche de déblocage
     */
    public List<Achievement> getNextAchievements(Player player, int limit) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return List.of();

        return achievements.values().stream()
            .filter(a -> !data.hasAchievement(a.id()))
            .sorted((a1, a2) -> {
                double p1 = (double) data.getAchievementProgress(a1.id()) / a1.requirement();
                double p2 = (double) data.getAchievementProgress(a2.id()) / a2.requirement();
                return Double.compare(p2, p1); // Plus proche en premier
            })
            .limit(limit)
            .toList();
    }

    // Cache pour les comptages par tier (ne change jamais)
    private Map<AchievementTier, Integer> tierCountsCache;

    /**
     * Obtient le nombre total d'achievements par tier (cached)
     */
    public Map<AchievementTier, Integer> getTierCounts() {
        if (tierCountsCache == null) {
            tierCountsCache = new EnumMap<>(AchievementTier.class);
            for (AchievementTier tier : AchievementTier.values()) {
                tierCountsCache.put(tier, byTier.get(tier).size());
            }
        }
        return tierCountsCache;
    }

    /**
     * Obtient le nombre d'achievements débloqués par tier pour un joueur
     */
    public int getUnlockedCountByTier(Player player, AchievementTier tier) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return (int) byTier.get(tier).stream()
            .filter(a -> data.hasAchievement(a.id()))
            .count();
    }

    /**
     * Formate un nombre
     */
    private String formatNumber(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    /**
     * Obtient les achievements non débloqués d'un joueur
     */
    public Set<String> getUnlockedAchievements(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return Set.of();
        return data.getUnlockedAchievements();
    }

    // ==================== ENUMS & RECORDS ====================

    /**
     * Catégories d'achievements
     */
    public enum AchievementCategory {
        COMBAT("Combat", "§c", Material.DIAMOND_SWORD, "⚔"),
        EXPLORATION("Exploration", "§a", Material.COMPASS, "🗺"),
        COLLECTION("Collection", "§e", Material.CHEST, "📦"),
        SOCIAL("Social", "§b", Material.PLAYER_HEAD, "👥"),
        EVENTS("Événements", "§d", Material.BEACON, "⚡"),
        PROGRESSION("Progression", "§6", Material.EXPERIENCE_BOTTLE, "📈"),
        MASTERY("Maîtrise", "§5", Material.DRAGON_EGG, "👑");

        @Getter private final String displayName;
        @Getter private final String color;
        @Getter private final Material icon;
        @Getter private final String emoji;

        AchievementCategory(String displayName, String color, Material icon, String emoji) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
            this.emoji = emoji;
        }
    }

    /**
     * Tiers d'achievements
     */
    public enum AchievementTier {
        BRONZE("§6", "Bronze", "★"),
        SILVER("§7", "Argent", "★★"),
        GOLD("§e", "Or", "★★★"),
        DIAMOND("§b", "Diamant", "✦"),
        LEGENDARY("§d", "Légendaire", "✦✦"),
        MYTHIC("§4", "Mythique", "⚜");

        @Getter private final String color;
        @Getter private final String displayName;
        @Getter private final String stars;

        AchievementTier(String color, String displayName, String stars) {
            this.color = color;
            this.displayName = displayName;
            this.stars = stars;
        }
    }

    /**
     * Record d'un achievement
     */
    public record Achievement(
        String id,
        String name,
        String description,
        AchievementCategory category,
        AchievementTier tier,
        Material icon,
        int requirement,
        int pointReward,
        int gemReward,
        String title
    ) {
        public static Builder builder(String id) {
            return new Builder(id);
        }

        public static class Builder {
            private final String id;
            private String name;
            private String description;
            private AchievementCategory category;
            private AchievementTier tier;
            private Material icon;
            private int requirement;
            private int pointReward;
            private int gemReward;
            private String title;

            public Builder(String id) {
                this.id = id;
            }

            public Builder name(String name) { this.name = name; return this; }
            public Builder description(String desc) { this.description = desc; return this; }
            public Builder category(AchievementCategory cat) { this.category = cat; return this; }
            public Builder tier(AchievementTier tier) { this.tier = tier; return this; }
            public Builder icon(Material icon) { this.icon = icon; return this; }
            public Builder requirement(int req) { this.requirement = req; return this; }
            public Builder pointReward(int points) { this.pointReward = points; return this; }
            public Builder gemReward(int gems) { this.gemReward = gems; return this; }
            public Builder title(String title) { this.title = title; return this; }

            public Achievement build() {
                return new Achievement(id, name, description, category, tier, icon,
                    requirement, pointReward, gemReward, title);
            }
        }
    }
}
