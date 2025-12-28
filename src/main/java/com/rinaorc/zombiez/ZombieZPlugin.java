package com.rinaorc.zombiez;

import com.rinaorc.zombiez.commands.admin.ClassAdminCommand;
import com.rinaorc.zombiez.commands.admin.ItemAdminCommand;
import com.rinaorc.zombiez.commands.admin.JourneyAdminCommand;
import com.rinaorc.zombiez.commands.admin.ZombieAdminCommand;
import com.rinaorc.zombiez.commands.admin.ZombieZAdminCommand;
import com.rinaorc.zombiez.commands.player.*;
import com.rinaorc.zombiez.data.ConfigManager;
import com.rinaorc.zombiez.data.DatabaseManager;
import com.rinaorc.zombiez.economy.banking.BankManager;
import com.rinaorc.zombiez.progression.*;
import com.rinaorc.zombiez.progression.gui.*;
import com.rinaorc.zombiez.items.ItemListener;
import com.rinaorc.zombiez.items.ItemManager;
import com.rinaorc.zombiez.items.gui.ItemCompareGUI;
import com.rinaorc.zombiez.items.gui.LootRevealGUI;
import com.rinaorc.zombiez.items.sets.SetBonusManager;
import com.rinaorc.zombiez.listeners.*;
import com.rinaorc.zombiez.managers.*;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zombies.ZombieListener;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.spawning.EventManager;
import com.rinaorc.zombiez.zombies.spawning.SpawnSystem;
import com.rinaorc.zombiez.placeholder.ZombieZExpansion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import java.util.logging.Level;

/**
 * ZombieZ - Looter-Survivor Zombie Game
 * Plugin principal optimisé pour 200+ joueurs
 * 
 * @author Rinaorc Studio
 * @version 1.0.0
 */
public class ZombieZPlugin extends JavaPlugin {

    @Getter
    private static ZombieZPlugin instance;

    // Managers
    @Getter
    private ConfigManager configManager;
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private ZoneManager zoneManager;
    @Getter
    private RefugeManager refugeManager;
    @Getter
    private PlayerDataManager playerDataManager;
    @Getter
    private EconomyManager economyManager;
    @Getter
    private ItemManager itemManager;
    @Getter
    private SetBonusManager setBonusManager;

    // Système Zombies
    @Getter
    private ZombieManager zombieManager;
    @Getter
    private SpawnSystem spawnSystem;
    @Getter
    private EventManager eventManager;

    // Système Économie Phase 4
    @Getter
    private BankManager bankManager;

    // Système Progression Phase 5
    @Getter
    private AchievementManager achievementManager;
    @Getter
    private SkillTreeManager skillTreeManager;
    @Getter
    private LeaderboardManager leaderboardManager;
    @Getter
    private MissionManager missionManager;
    @Getter
    private BattlePassManager battlePassManager;
    @Getter
    private CosmeticManager cosmeticManager;
    @Getter
    private ProgressionManager progressionManager;
    @Getter
    private com.rinaorc.zombiez.economy.PrestigeSystem prestigeSystem;
    @Getter
    private DailyRewardManager dailyRewardManager;

    // Nouveaux systèmes Phase 6
    @Getter
    private com.rinaorc.zombiez.party.PartyManager partyManager;
    @Getter
    private com.rinaorc.zombiez.momentum.MomentumManager momentumManager;
    @Getter
    private com.rinaorc.zombiez.zones.SecretZoneManager secretZoneManager;

    // Système d'Éveils (remplace l'ancien système de Pouvoirs)
    @Getter
    private com.rinaorc.zombiez.items.awaken.AwakenManager awakenManager;

    // Listeners stockés pour accès externe
    @Getter
    private PlayerMoveListener playerMoveListener;
    @Getter
    private ItemListener itemListener;
    @Getter
    private BowListener bowListener;
    @Getter
    private TridentListener tridentListener;
    @Getter
    private MaceListener maceListener;

    // Systèmes de spawn spécialisés
    @Getter
    private com.rinaorc.zombiez.zombies.spawning.BossSpawnSystem bossSpawnSystem;
    @Getter
    private com.rinaorc.zombiez.zombies.spawning.HordeEventSystem hordeEventSystem;

    // Système de mobs passifs et nourriture
    @Getter
    private com.rinaorc.zombiez.mobs.PassiveMobManager passiveMobManager;

    // Système de PNJ survivants dans les refuges
    @Getter
    private com.rinaorc.zombiez.mobs.ShelterNPCManager shelterNPCManager;

    // Système de Boss Bar Dynamique
    @Getter
    private com.rinaorc.zombiez.ui.DynamicBossBarManager dynamicBossBarManager;

    // Système de Consommables
    @Getter
    private com.rinaorc.zombiez.consumables.ConsumableManager consumableManager;

    // Système d'Événements Dynamiques
    @Getter
    private com.rinaorc.zombiez.events.dynamic.DynamicEventManager dynamicEventManager;

    // Système de Micro-Événements
    @Getter
    private com.rinaorc.zombiez.events.micro.MicroEventManager microEventManager;

    // Système de Météo Dynamique
    @Getter
    private com.rinaorc.zombiez.weather.WeatherManager weatherManager;

    // Système de Classes
    @Getter
    private com.rinaorc.zombiez.classes.ClassManager classManager;
    @Getter
    private com.rinaorc.zombiez.classes.talents.TalentManager talentManager;
    @Getter
    private com.rinaorc.zombiez.classes.talents.TalentListener talentListener;
    @Getter
    private com.rinaorc.zombiez.classes.gui.ClassSelectionGUI classSelectionGUI;
    @Getter
    private com.rinaorc.zombiez.classes.gui.ClassInfoGUI classInfoGUI;
    @Getter
    private com.rinaorc.zombiez.classes.gui.TalentSelectionGUI talentSelectionGUI;
    @Getter
    private com.rinaorc.zombiez.classes.gui.BranchSelectionGUI branchSelectionGUI;
    @Getter
    private com.rinaorc.zombiez.classes.beasts.BeastManager beastManager;
    @Getter
    private com.rinaorc.zombiez.classes.shadow.ShadowManager shadowManager;
    @Getter
    private com.rinaorc.zombiez.classes.poison.PoisonManager poisonManager;
    @Getter
    private com.rinaorc.zombiez.classes.perforation.PerforationManager perforationManager;

    // Système de Pets
    @Getter
    private com.rinaorc.zombiez.pets.PetManager petManager;
    @Getter
    private com.rinaorc.zombiez.pets.gacha.PetShopSystem petShopSystem;

    // Système de Performance (clearlag intelligent)
    @Getter
    private com.rinaorc.zombiez.managers.PerformanceManager performanceManager;

    // Système Dopamine - Feedback et engagement
    @Getter
    private com.rinaorc.zombiez.dopamine.LowHealthHeartbeatManager lowHealthHeartbeatManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.LootExplosionManager lootExplosionManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.MultiKillCascadeManager multiKillCascadeManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.PersonalBestManager personalBestManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.TimeLimitedBonusManager timeLimitedBonusManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.DailyLoginStreakManager dailyLoginStreakManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.LiveLeaderboardManager liveLeaderboardManager;
    @Getter
    private com.rinaorc.zombiez.dopamine.AssistManager assistManager;

    // Système ActionBar centralisé
    @Getter
    private com.rinaorc.zombiez.managers.ActionBarManager actionBarManager;

    // Système de Recyclage
    @Getter
    private com.rinaorc.zombiez.recycling.RecycleManager recycleManager;
    @Getter
    private com.rinaorc.zombiez.recycling.RecycleGUI recycleGUI;

    // Système World Boss (événements aléatoires)
    @Getter
    private com.rinaorc.zombiez.worldboss.WorldBossManager worldBossManager;

    // Système de Parcours (Journey) - Progression guidée
    @Getter
    private com.rinaorc.zombiez.progression.journey.JourneyManager journeyManager;
    @Getter
    private com.rinaorc.zombiez.progression.journey.JourneyListener journeyListener;
    @Getter
    private com.rinaorc.zombiez.progression.journey.chapter1.Chapter1Systems chapter1Systems;
    @Getter
    private com.rinaorc.zombiez.progression.journey.chapter2.Chapter2Systems chapter2Systems;
    @Getter
    private com.rinaorc.zombiez.progression.journey.chapter3.Chapter3Systems chapter3Systems;
    @Getter
    private com.rinaorc.zombiez.progression.journey.chapter4.Chapter4Systems chapter4Systems;

    private com.rinaorc.zombiez.navigation.GPSManager gpsManager;

    // Système WorldBorder par joueur (progression zones)
    @Getter
    private com.rinaorc.zombiez.managers.ZoneBorderManager zoneBorderManager;

    // Système TextDisplay zones verrouillées (client-side)
    @Getter
    private com.rinaorc.zombiez.managers.ZoneLockDisplayManager zoneLockDisplayManager;

    // État du plugin
    @Getter
    private boolean fullyLoaded = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        // Banner de démarrage
        logBanner();

        try {
            // Phase 1: Configuration
            log(Level.INFO, "§e[1/6] Chargement de la configuration...");
            loadConfiguration();

            // Phase 2: Base de données
            log(Level.INFO, "§e[2/6] Connexion à la base de données...");
            initializeDatabase();

            // Nettoyage des mobs existants (évite les zombies bugués après reboot)
            log(Level.INFO, "§7Nettoyage des mobs existants...");
            clearAllZombieMobs();

            // Nettoyage des entités d'événements (TextDisplay, ArmorStand, NPCs, coffres)
            log(Level.INFO, "§7Nettoyage des entités d'événements persistantes...");
            clearEventEntities();

            // Phase 3: Managers
            log(Level.INFO, "§e[3/6] Initialisation des managers...");
            initializeManagers();

            // Phase 4: Commandes
            log(Level.INFO, "§e[4/6] Enregistrement des commandes...");
            registerCommands();

            // Phase 5: Listeners
            log(Level.INFO, "§e[5/6] Enregistrement des listeners...");
            registerListeners();

            // Phase 6: Tâches planifiées
            log(Level.INFO, "§e[6/6] Démarrage des tâches planifiées...");
            startScheduledTasks();

            // Phase 7: Hooks externes (PlaceholderAPI)
            registerPlaceholders();

            fullyLoaded = true;
            long loadTime = System.currentTimeMillis() - startTime;
            log(Level.INFO, "§a✓ ZombieZ chargé avec succès en " + loadTime + "ms!");
            log(Level.INFO, "§7Plugin prêt pour §e200+ joueurs §7simultanés.");

        } catch (Exception e) {
            log(Level.SEVERE, "§c✗ Erreur lors du chargement de ZombieZ!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        log(Level.INFO, "§6Arrêt de ZombieZ...");

        // Sauvegarde de toutes les données joueurs
        if (playerDataManager != null) {
            log(Level.INFO, "§7Arrêt du tracker de playtime...");
            playerDataManager.stopPlaytimeTracker();
            log(Level.INFO, "§7Sauvegarde des données joueurs...");
            playerDataManager.saveAllSync();
        }

        // Cleanup du système d'items
        if (itemManager != null) {
            log(Level.INFO, "§7Nettoyage du cache d'items...");
            itemManager.cleanup();
        }

        // Cleanup des hologrammes de refuge
        if (refugeManager != null) {
            log(Level.INFO, "§7Nettoyage des hologrammes de refuge...");
            refugeManager.shutdown();
        }

        // Cleanup des PNJ survivants dans les refuges
        if (shelterNPCManager != null) {
            log(Level.INFO, "§7Nettoyage des PNJ survivants...");
            shelterNPCManager.shutdown();
        }

        // Cleanup du système de boss bars dynamiques
        if (dynamicBossBarManager != null) {
            log(Level.INFO, "§7Nettoyage des boss bars dynamiques...");
            dynamicBossBarManager.shutdown();
        }

        // Cleanup du système de consommables
        if (consumableManager != null) {
            log(Level.INFO, "§7Nettoyage du système de consommables...");
            consumableManager.cleanup();
        }

        // Cleanup du système d'événements dynamiques
        if (dynamicEventManager != null) {
            log(Level.INFO, "§7Arrêt des événements dynamiques...");
            dynamicEventManager.shutdown();
        }

        // Cleanup du système World Boss
        if (worldBossManager != null) {
            log(Level.INFO, "§7Arrêt du système World Boss...");
            worldBossManager.stop();
        }

        // Cleanup du système de micro-événements
        if (microEventManager != null) {
            log(Level.INFO, "§7Arrêt des micro-événements...");
            microEventManager.shutdown();
        }

        // Cleanup du système de météo dynamique
        if (weatherManager != null) {
            log(Level.INFO, "§7Arrêt du système météo...");
            weatherManager.shutdown();
        }

        // Cleanup du système de classes
        if (classManager != null) {
            log(Level.INFO, "§7Sauvegarde des données de classe...");
            classManager.shutdown();
        }

        // Cleanup des ArmorStands visuels (Bouclier d'Os)
        if (talentListener != null) {
            log(Level.INFO, "§7Nettoyage des effets visuels de talents...");
            talentListener.cleanupAllBoneShieldArmorStands();
        }

        // Cleanup du système de bêtes
        if (beastManager != null) {
            log(Level.INFO, "§7Nettoyage des bêtes invoquées...");
            // Despawn toutes les bêtes pour tous les joueurs
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                beastManager.despawnAllBeasts(player);
            }
        }

        // Cleanup du système de pets
        if (petManager != null) {
            log(Level.INFO, "§7Sauvegarde des données de pets...");
            petManager.shutdown();
        }

        // Cleanup du système de performance
        if (performanceManager != null) {
            log(Level.INFO, "§7Arrêt du système de performance...");
            performanceManager.shutdown();
        }

        // Cleanup du système dopamine
        if (lowHealthHeartbeatManager != null) {
            log(Level.INFO, "§7Arrêt du système de battement de cœur...");
            lowHealthHeartbeatManager.shutdown();
        }
        if (multiKillCascadeManager != null) {
            log(Level.INFO, "§7Arrêt du système multi-kill cascade...");
            multiKillCascadeManager.shutdown();
        }
        if (timeLimitedBonusManager != null) {
            log(Level.INFO, "§7Arrêt du système de bonus temporaires...");
            timeLimitedBonusManager.shutdown();
        }
        if (assistManager != null) {
            log(Level.INFO, "§7Arrêt du système d'assists...");
            assistManager.shutdown();
        }

        // Cleanup du système de recyclage
        if (recycleManager != null) {
            log(Level.INFO, "§7Arrêt du système de recyclage...");
            recycleManager.shutdown();
        }

        // Cleanup du système de parcours (Journey)
        if (journeyManager != null) {
            log(Level.INFO, "§7Arrêt du système de journal...");
            journeyManager.shutdown();
        }

        // Cleanup Chapter 2 Systems
        if (chapter2Systems != null) {
            chapter2Systems.cleanup();
        }

        // Cleanup GPS Manager
        if (gpsManager != null) {
            gpsManager.shutdown();
        }

        // Cleanup Zone Border Manager
        if (zoneBorderManager != null) {
            zoneBorderManager.cleanup();
        }

        // Cleanup Zone Lock Display Manager
        if (zoneLockDisplayManager != null) {
            zoneLockDisplayManager.stop();
        }

        // Fermeture de la base de données
        if (databaseManager != null) {
            log(Level.INFO, "§7Fermeture de la connexion BDD...");
            databaseManager.shutdown();
        }

        log(Level.INFO, "§a✓ ZombieZ arrêté proprement.");
        instance = null;
    }

    /**
     * Charge la configuration depuis les fichiers YAML
     */
    private void loadConfiguration() {
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();
    }

    /**
     * Initialise la connexion à la base de données avec HikariCP
     */
    private void initializeDatabase() throws Exception {
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        databaseManager.createTables();
    }

    /**
     * Initialise tous les managers du plugin
     */
    private void initializeManagers() {
        // Zone Manager - Gestion des zones et détection
        zoneManager = new ZoneManager(this);
        zoneManager.loadZones();

        // Refuge Manager - Gestion des refuges (checkpoints et téléportation)
        refugeManager = new RefugeManager(this);
        refugeManager.loadRefuges();

        // Player Data Manager - Cache et persistance des données joueurs
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.startPlaytimeTracker();

        // Economy Manager - Gestion des monnaies
        economyManager = new EconomyManager(this);

        // Item Manager - Système de loot procédural
        itemManager = new ItemManager(this);

        // Set Bonus Manager - Bonus des sets d'équipement
        setBonusManager = new SetBonusManager(this);

        // Zombie Manager - Gestion des zombies
        zombieManager = new ZombieManager(this);

        // Performance Manager - Clearlag intelligent et optimisations
        performanceManager = new com.rinaorc.zombiez.managers.PerformanceManager(this);

        // Spawn System - Spawn dynamique des zombies
        spawnSystem = new SpawnSystem(this, zombieManager);

        // Event Manager - Hordes, Blood Moon, Boss
        eventManager = new EventManager(this, zombieManager, spawnSystem);

        // ===== Système Économie Phase 4 =====

        // Bank Manager - Banque personnelle
        bankManager = new BankManager(this);

        // ===== Système Progression Phase 5 =====

        // Achievement Manager - Succès
        achievementManager = new AchievementManager(this);

        // Skill Tree Manager - Compétences
        skillTreeManager = new SkillTreeManager(this);

        // Leaderboard Manager - Classements
        leaderboardManager = new LeaderboardManager(this);

        // Mission Manager - Missions journalières/hebdomadaires
        missionManager = new MissionManager(this);

        // Battle Pass Manager - Pass de saison
        battlePassManager = new BattlePassManager(this);

        // Cosmetic Manager - Titres et cosmétiques
        cosmeticManager = new CosmeticManager(this);

        // Progression Manager - Gestion globale de la progression
        progressionManager = new ProgressionManager(this);

        // Prestige System - Système de prestige
        prestigeSystem = new com.rinaorc.zombiez.economy.PrestigeSystem(this);

        // Daily Reward Manager - Récompenses quotidiennes
        dailyRewardManager = new DailyRewardManager(this);

        // ===== Nouveaux Systèmes Phase 6 =====

        // Boss Spawn System - Spawn des boss
        bossSpawnSystem = new com.rinaorc.zombiez.zombies.spawning.BossSpawnSystem(this, zombieManager);

        // Horde Event System - Événements de horde
        hordeEventSystem = new com.rinaorc.zombiez.zombies.spawning.HordeEventSystem(this, zombieManager, spawnSystem);

        // World Boss Manager - Événements World Boss aléatoires
        worldBossManager = new com.rinaorc.zombiez.worldboss.WorldBossManager(this);
        worldBossManager.start();

        // ===== Système de Parcours (Journey) =====

        // Journey Manager - Progression guidée avec blocage de zones
        journeyManager = new com.rinaorc.zombiez.progression.journey.JourneyManager(this);
        journeyManager.start(); // Démarre le système de coffres mystères

        // Chapter 1 Systems - Fermier et mini-jeu incendie
        chapter1Systems = new com.rinaorc.zombiez.progression.journey.chapter1.Chapter1Systems(this);

        // Chapter 2 Systems - NPCs, Zombies Incendiés, Boss du Manoir
        chapter2Systems = new com.rinaorc.zombiez.progression.journey.chapter2.Chapter2Systems(this);

        // Chapter 3 Systems - NPC Forain et puzzle Memory Game
        chapter3Systems = new com.rinaorc.zombiez.progression.journey.chapter3.Chapter3Systems(this);

        // Chapter 4 Systems - Le Fossoyeur (Cimetière)
        chapter4Systems = new com.rinaorc.zombiez.progression.journey.chapter4.Chapter4Systems(this);

        // GPS Manager - Navigation vers les objectifs du Journey
        gpsManager = new com.rinaorc.zombiez.navigation.GPSManager(this);

        // Zone Border Manager - WorldBorder par joueur basé sur la progression
        zoneBorderManager = new com.rinaorc.zombiez.managers.ZoneBorderManager(this);
        zoneBorderManager.initialize();

        // Zone Lock Display Manager - TextDisplay client-side pour zones verrouillées
        zoneLockDisplayManager = new com.rinaorc.zombiez.managers.ZoneLockDisplayManager(this);
        zoneLockDisplayManager.start();

        // Party Manager - Système de groupe
        partyManager = new com.rinaorc.zombiez.party.PartyManager(this);

        // Momentum Manager - Streaks, Combos, Fever
        momentumManager = new com.rinaorc.zombiez.momentum.MomentumManager(this);

        // Secret Zone Manager - Zones secrètes et événements
        secretZoneManager = new com.rinaorc.zombiez.zones.SecretZoneManager(this);

        // ===== Système Mobs Passifs et Nourriture =====

        // Passive Mob Manager - Mobs passifs et loot nourriture
        passiveMobManager = new com.rinaorc.zombiez.mobs.PassiveMobManager(this);

        // Shelter NPC Manager - PNJ survivants dans les refuges
        shelterNPCManager = new com.rinaorc.zombiez.mobs.ShelterNPCManager(this);

        // ===== Système Boss Bar Dynamique =====

        // Dynamic Boss Bar Manager - Affichage contextuel XP, Events, Streaks, Boss
        dynamicBossBarManager = new com.rinaorc.zombiez.ui.DynamicBossBarManager(this);

        // ===== Système Consommables =====

        // Consumable Manager - Items consommables droppés par les zombies
        consumableManager = new com.rinaorc.zombiez.consumables.ConsumableManager(this);

        // ===== Système Événements Dynamiques =====

        // Dynamic Event Manager - Événements automatiques près des joueurs
        dynamicEventManager = new com.rinaorc.zombiez.events.dynamic.DynamicEventManager(this);
        dynamicEventManager.loadConfig(configManager.getEventsConfig());
        dynamicEventManager.start();

        // ===== Système Micro-Événements =====

        // Micro Event Manager - Petits événements personnels pour maintenir
        // l'engagement
        microEventManager = new com.rinaorc.zombiez.events.micro.MicroEventManager(this);
        microEventManager.start();

        // ===== Système Météo Dynamique =====

        // Weather Manager - Météo dynamique avec effets gameplay
        weatherManager = new com.rinaorc.zombiez.weather.WeatherManager(this);
        weatherManager.loadConfig(configManager.loadConfig("weather.yml"));
        weatherManager.start();

        // ===== Système de Classes =====

        // Class Manager - Système de classes complet
        classManager = new com.rinaorc.zombiez.classes.ClassManager(this);

        // Talent Manager - Registre et gestion des talents
        talentManager = new com.rinaorc.zombiez.classes.talents.TalentManager(this);

        // Class Selection GUI - Menu de sélection de classe
        classSelectionGUI = new com.rinaorc.zombiez.classes.gui.ClassSelectionGUI(this, classManager);

        // Class Info GUI - Menu info de classe
        classInfoGUI = new com.rinaorc.zombiez.classes.gui.ClassInfoGUI(this, classManager);

        // Talent Selection GUI - Menu de sélection des talents
        talentSelectionGUI = new com.rinaorc.zombiez.classes.gui.TalentSelectionGUI(this, classManager, talentManager);

        // Branch Selection GUI - Menu de sélection de branche/spécialisation
        branchSelectionGUI = new com.rinaorc.zombiez.classes.gui.BranchSelectionGUI(this, classManager);

        // Beast Manager - Système de bêtes pour la Voie des Bêtes du Chasseur
        beastManager = new com.rinaorc.zombiez.classes.beasts.BeastManager(this, talentManager);

        // Shadow Manager - Système de la branche Ombre du Chasseur (Points d'Ombre, Marques, Clones)
        shadowManager = new com.rinaorc.zombiez.classes.shadow.ShadowManager(this, talentManager);

        // Poison Manager - Système de la branche Poison du Chasseur (Stacks, Explosions, Avatar)
        poisonManager = new com.rinaorc.zombiez.classes.poison.PoisonManager(this, talentManager);

        // Perforation Manager - Système de la branche Perforation du Chasseur (Calibre, Surchauffe, Jugement)
        perforationManager = new com.rinaorc.zombiez.classes.perforation.PerforationManager(this, talentManager);

        // ===== Système d'Éveils (Awaken) =====

        // Awaken Manager - Système d'éveils sur les items (dépend de TalentManager)
        awakenManager = new com.rinaorc.zombiez.items.awaken.AwakenManager(this);
        awakenManager.loadFromConfig(configManager.loadConfig("awakens.yml"));

        // ActionBar Manager - Système centralisé d'ActionBar (gestion combat/hors-combat)
        actionBarManager = new com.rinaorc.zombiez.managers.ActionBarManager(this);

        // ===== Système de Pets =====

        // Pet Manager - Système complet de pets avec caching et persistance
        petManager = new com.rinaorc.zombiez.pets.PetManager(this);

        // Pet Shop System - Boutique de pets
        petShopSystem = new com.rinaorc.zombiez.pets.gacha.PetShopSystem(this);

        // ===== Système Dopamine - Feedback et Engagement =====

        // Low Health Heartbeat - Battement de cœur quand vie faible
        lowHealthHeartbeatManager = new com.rinaorc.zombiez.dopamine.LowHealthHeartbeatManager(this);
        lowHealthHeartbeatManager.start();

        // Loot Explosion - Effets visuels pour les drops de loot
        lootExplosionManager = new com.rinaorc.zombiez.dopamine.LootExplosionManager(this);

        // Multi-Kill Cascade - Effets pour les kills rapides en chaîne
        multiKillCascadeManager = new com.rinaorc.zombiez.dopamine.MultiKillCascadeManager(this);

        // Personal Best - Records personnels et popups de célébration
        personalBestManager = new com.rinaorc.zombiez.dopamine.PersonalBestManager(this);

        // Time-Limited Bonus - Défis temporaires avec bonus
        timeLimitedBonusManager = new com.rinaorc.zombiez.dopamine.TimeLimitedBonusManager(this);

        // Daily Login Streak - Récompenses de connexion quotidienne
        dailyLoginStreakManager = new com.rinaorc.zombiez.dopamine.DailyLoginStreakManager(this);

        // Live Leaderboard - Mises à jour en temps réel du classement
        liveLeaderboardManager = new com.rinaorc.zombiez.dopamine.LiveLeaderboardManager(this);

        // Assist System - Système d'assists pour les kills en équipe
        assistManager = new com.rinaorc.zombiez.dopamine.AssistManager(this);

        // ===== Système de Recyclage =====

        // Recycle Manager - Recyclage automatique d'items en points
        recycleManager = new com.rinaorc.zombiez.recycling.RecycleManager(this);

        // Recycle GUI - Interface de configuration du recyclage
        recycleGUI = new com.rinaorc.zombiez.recycling.RecycleGUI(this, recycleManager);
    }

    /**
     * Enregistre toutes les commandes
     */
    private void registerCommands() {
        // Commandes Admin
        getCommand("zombiez").setExecutor(new ZombieZAdminCommand(this));

        // Commandes Admin Items
        ItemAdminCommand itemCmd = new ItemAdminCommand(this);
        getCommand("zzitem").setExecutor(itemCmd);
        getCommand("zzitem").setTabCompleter(itemCmd);

        // Commandes Admin Zombies
        ZombieAdminCommand zombieCmd = new ZombieAdminCommand(this);
        getCommand("zzzombie").setExecutor(zombieCmd);
        getCommand("zzzombie").setTabCompleter(zombieCmd);

        // Commandes Admin Consommables
        com.rinaorc.zombiez.commands.admin.ConsumableAdminCommand consumableCmd = new com.rinaorc.zombiez.commands.admin.ConsumableAdminCommand(
                this);
        getCommand("zzconsumable").setExecutor(consumableCmd);
        getCommand("zzconsumable").setTabCompleter(consumableCmd);

        // Commandes Admin Événements Dynamiques
        com.rinaorc.zombiez.commands.admin.EventAdminCommand eventCmd = new com.rinaorc.zombiez.commands.admin.EventAdminCommand(
                this);
        getCommand("zzevent").setExecutor(eventCmd);
        getCommand("zzevent").setTabCompleter(eventCmd);

        // Commandes Admin Micro-Événements
        com.rinaorc.zombiez.commands.admin.MicroEventAdminCommand microEventCmd = new com.rinaorc.zombiez.commands.admin.MicroEventAdminCommand(
                this);
        getCommand("zzmicro").setExecutor(microEventCmd);
        getCommand("zzmicro").setTabCompleter(microEventCmd);

        // Commandes Admin Météo Dynamique
        com.rinaorc.zombiez.commands.admin.WeatherAdminCommand weatherCmd = new com.rinaorc.zombiez.commands.admin.WeatherAdminCommand(
                this);
        getCommand("zzweather").setExecutor(weatherCmd);
        getCommand("zzweather").setTabCompleter(weatherCmd);

        // Commandes Admin Classes
        ClassAdminCommand classAdminCmd = new ClassAdminCommand(this);
        getCommand("zzclassadmin").setExecutor(classAdminCmd);
        getCommand("zzclassadmin").setTabCompleter(classAdminCmd);

        // Commandes Admin Journey
        JourneyAdminCommand journeyAdminCmd = new JourneyAdminCommand(this);
        getCommand("zzjourneyadmin").setExecutor(journeyAdminCmd);
        getCommand("zzjourneyadmin").setTabCompleter(journeyAdminCmd);

        // Commandes Admin Éveils
        com.rinaorc.zombiez.commands.admin.AwakenAdminCommand awakenCmd = new com.rinaorc.zombiez.commands.admin.AwakenAdminCommand(this);
        getCommand("awaken").setExecutor(awakenCmd);
        getCommand("awaken").setTabCompleter(awakenCmd);

        // Commandes Admin World Boss
        com.rinaorc.zombiez.worldboss.WorldBossAdminCommand worldBossCmd = new com.rinaorc.zombiez.worldboss.WorldBossAdminCommand(this);
        getCommand("zzworldboss").setExecutor(worldBossCmd);
        getCommand("zzworldboss").setTabCompleter(worldBossCmd);

        // Commandes Joueur - Base
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        ZoneCommand zoneCmd = new ZoneCommand(this);
        getCommand("zone").setExecutor(zoneCmd);
        getCommand("zone").setTabCompleter(zoneCmd);
        getCommand("checkpoint").setExecutor(new CheckpointCommand(this));
        RefugeCommand refugeCmd = new RefugeCommand(this);
        getCommand("refuge").setExecutor(refugeCmd);
        getCommand("refuge").setTabCompleter(refugeCmd);

        // Commande Joueur - Parcours (Journey)
        com.rinaorc.zombiez.commands.player.JourneyCommand journeyCmd = new com.rinaorc.zombiez.commands.player.JourneyCommand(this);
        getCommand("journey").setExecutor(journeyCmd);
        getCommand("journey").setTabCompleter(journeyCmd);

        // Commande Joueur - GPS (Navigation Journey)
        com.rinaorc.zombiez.commands.player.GPSCommand gpsCmd = new com.rinaorc.zombiez.commands.player.GPSCommand(this);
        getCommand("gps").setExecutor(gpsCmd);
        getCommand("gps").setTabCompleter(gpsCmd);

        // Commandes Joueur - Économie
        BankCommand bankCmd = new BankCommand(this);
        getCommand("bank").setExecutor(bankCmd);
        getCommand("bank").setTabCompleter(bankCmd);

        // Commandes Joueur - Progression
        ProgressionCommand progressionCmd = new ProgressionCommand(this);
        getCommand("progression").setExecutor(progressionCmd);
        getCommand("progression").setTabCompleter(progressionCmd);

        // Commande Joueur - Achievements
        getCommand("achievements").setExecutor(new com.rinaorc.zombiez.commands.player.AchievementCommand(this));

        // Commande Joueur - Missions
        getCommand("mission").setExecutor(new com.rinaorc.zombiez.commands.player.MissionCommand(this));

        // Commandes Joueur - Classes
        ClassCommand classCmd = new ClassCommand(this);
        getCommand("class").setExecutor(classCmd);
        getCommand("class").setTabCompleter(classCmd);
        getCommand("mutations").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                classManager.getMutationManager().getMutationSummary()
                        .forEach(player::sendMessage);
            }
            return true;
        });

        // Commandes Joueur - Talents
        TalentCommand talentCmd = new TalentCommand(this);
        getCommand("talent").setExecutor(talentCmd);
        getCommand("talent").setTabCompleter(talentCmd);

        // Commandes Joueur - Pets
        com.rinaorc.zombiez.pets.commands.PetCommand petCmd = new com.rinaorc.zombiez.pets.commands.PetCommand(this);
        getCommand("pet").setExecutor(petCmd);
        getCommand("pet").setTabCompleter(petCmd);

        // Commandes Admin - Pets
        com.rinaorc.zombiez.pets.commands.PetAdminCommand petAdminCmd = new com.rinaorc.zombiez.pets.commands.PetAdminCommand(
                this);
        getCommand("petadmin").setExecutor(petAdminCmd);
        getCommand("petadmin").setTabCompleter(petAdminCmd);

        // Commandes Joueur - Recyclage
        com.rinaorc.zombiez.recycling.RecycleCommand recycleCmd = new com.rinaorc.zombiez.recycling.RecycleCommand(this);
        getCommand("recycle").setExecutor(recycleCmd);
        getCommand("recycle").setTabCompleter(recycleCmd);
    }

    /**
     * Enregistre tous les listeners
     */
    private void registerListeners() {
        var pm = getServer().getPluginManager();

        // Listeners principaux
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new DeathListener(this), this);
        pm.registerEvents(new InteractListener(this), this);
        pm.registerEvents(new ZoneChangeListener(this), this);
        pm.registerEvents(new BlockProtectionListener(this), this);

        // Listener système de parcours (Journey) - Blocage de zones et progression
        journeyListener = new com.rinaorc.zombiez.progression.journey.JourneyListener(this);
        pm.registerEvents(journeyListener, this);

        // Listener GPS - Navigation vers objectifs du Journey
        pm.registerEvents(gpsManager, this);

        // Listeners système d'items
        itemListener = new ItemListener(this);
        pm.registerEvents(itemListener, this);
        pm.registerEvents(new ItemCompareGUI.GUIListener(this), this);
        pm.registerEvents(new LootRevealGUI.LootGUIListener(this), this);

        // Listener système de tir amélioré (arcs/arbalètes)
        bowListener = new BowListener(this);
        pm.registerEvents(bowListener, this);

        // Listener système de trident amélioré (charge, pierce, bonus aquatique)
        tridentListener = new TridentListener(this);
        pm.registerEvents(tridentListener, this);

        // Listener système de masse amélioré (ground pound, stun, armor shatter)
        maceListener = new MaceListener(this);
        pm.registerEvents(maceListener, this);

        // Le système d'éveils n'a pas de listener dédié
        // Les éveils sont gérés via le TalentListener existant

        // Listener système de zombies
        pm.registerEvents(new ZombieListener(this), this);

        // Listener pour bloquer les spawns de mobs vanilla
        pm.registerEvents(new MobSpawnListener(this), this);

        // Listener pour nettoyer les mobs quand les chunks sont déchargés
        pm.registerEvents(new ChunkUnloadListener(this), this);

        // Listeners système mobs passifs et nourriture
        if (passiveMobManager != null) {
            pm.registerEvents(passiveMobManager, this);
            pm.registerEvents(new com.rinaorc.zombiez.mobs.food.FoodListener(this), this);
        }

        // Listener système PNJ survivants dans les refuges
        if (shelterNPCManager != null) {
            pm.registerEvents(shelterNPCManager, this);
        }

        // Listener système consommables
        if (consumableManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.consumables.ConsumableListener(this, consumableManager), this);
        }

        // Listener système événements dynamiques
        if (dynamicEventManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.events.dynamic.DynamicEventListener(this, dynamicEventManager),
                    this);
        }

        // Listener système micro-événements
        if (microEventManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.events.micro.MicroEventListener(this, microEventManager), this);
        }

        // Listener système météo dynamique
        if (weatherManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.weather.WeatherListener(this, weatherManager), this);
        }

        // Listeners système de progression
        pm.registerEvents(new MissionGUI.MissionGUIListener(this), this);
        pm.registerEvents(new BattlePassGUI.BattlePassGUIListener(this), this);
        pm.registerEvents(new com.rinaorc.zombiez.progression.gui.AchievementGUI(this), this);

        // Listener système de classes
        if (classManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.ClassListener(this), this);
        }

        // Listener système de talents (effets passifs)
        if (talentManager != null) {
            talentListener = new com.rinaorc.zombiez.classes.talents.TalentListener(this, talentManager);
            pm.registerEvents(talentListener, this);
            pm.registerEvents(new com.rinaorc.zombiez.classes.talents.ChasseurTalentListener(this, talentManager),
                    this);
            pm.registerEvents(new com.rinaorc.zombiez.classes.talents.OccultisteTalentListener(this, talentManager),
                    this);
        }

        // Listener système de bêtes (Voie des Bêtes du Chasseur)
        if (beastManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.beasts.BeastListener(this, beastManager), this);
        }

        // Listener système branche Ombre (Chasseur)
        if (shadowManager != null && talentManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.shadow.ShadowListener(this, talentManager, shadowManager), this);
        }

        // Listener système branche Poison (Chasseur)
        if (poisonManager != null && talentManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.poison.PoisonListener(this, talentManager, poisonManager), this);
        }

        // Listener système branche Perforation (Chasseur)
        if (perforationManager != null && talentManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.perforation.PerforationListener(this, talentManager, perforationManager), this);
        }

        // Listeners système de pets
        if (petManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.pets.listeners.PetCombatListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.listeners.PetConnectionListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetMainGUI.GUIListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetCollectionGUI.GUIListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetDetailsGUI.GUIListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetEggGUI.GUIListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetOptionsGUI.GUIListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.PetShopGUI.ShopListener(this), this);
            pm.registerEvents(new com.rinaorc.zombiez.pets.gui.EggOpeningAnimation.AnimationListener(), this);
        }

        // Listeners système zone wiki GUI
        pm.registerEvents(new com.rinaorc.zombiez.zones.gui.ZoneWikiGUI.GUIListener(this), this);
        pm.registerEvents(new com.rinaorc.zombiez.zones.gui.ZoneDetailGUI.GUIListener(this), this);
        pm.registerEvents(new com.rinaorc.zombiez.zones.gui.RefugeGUI.RefugeGUIListener(this), this);

        // Listener système dopamine - Low Health Heartbeat
        if (lowHealthHeartbeatManager != null) {
            pm.registerEvents(lowHealthHeartbeatManager, this);
        }

        // Listener système dopamine - Multi-Kill Cascade
        if (multiKillCascadeManager != null) {
            pm.registerEvents(multiKillCascadeManager, this);
        }

        // Listener système dopamine - Time-Limited Bonus
        if (timeLimitedBonusManager != null) {
            pm.registerEvents(timeLimitedBonusManager, this);
        }

        // Listener système dopamine - Daily Login Streak
        if (dailyLoginStreakManager != null) {
            pm.registerEvents(dailyLoginStreakManager, this);
        }

        // Listener système dopamine - Assist System
        if (assistManager != null) {
            pm.registerEvents(assistManager, this);
        }

        // Listener système World Boss
        if (worldBossManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.worldboss.WorldBossListener(this, worldBossManager), this);
        }
    }

    /**
     * Enregistre les placeholders PlaceholderAPI si disponible
     */
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZombieZExpansion(this).register();
            log(Level.INFO, "§a✓ PlaceholderAPI hook enregistré!");
        } else {
            log(Level.WARNING, "§ePlaceholderAPI non trouvé - placeholders désactivés");
        }
    }

    /**
     * Démarre les tâches planifiées (async pour les opérations lourdes)
     */
    private void startScheduledTasks() {
        // Sauvegarde automatique toutes les 5 minutes (async)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataManager != null) {
                playerDataManager.saveAllAsync();
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // 5 minutes

        // Vérification des zones toutes les 0.5 secondes (optimisé)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (zoneManager != null) {
                zoneManager.checkPlayersZones();
            }
        }, 10L, 10L); // 0.5 seconde

        // ActionBar permanent - DÉSACTIVÉ: géré par ActionBarManager centralisé
        // new com.rinaorc.zombiez.listeners.ActionBarTask(this).start();

        // Nettoyage périodique du cache des indicateurs de dégâts (toutes les 30 secondes)
        // Nettoie les deux systèmes pour compatibilité (fallback + ProtocolLib)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            com.rinaorc.zombiez.combat.DamageIndicator.cleanup();
            com.rinaorc.zombiez.combat.PacketDamageIndicator.cleanup();
        }, 20L * 30, 20L * 30);
    }

    /**
     * Obtient le GPSManager
     */
    public com.rinaorc.zombiez.navigation.GPSManager getGPSManager() {
        return gpsManager;
    }

    /**
     * Recharge la configuration du plugin
     */
    public void reload() {
        log(Level.INFO, "§6Rechargement de ZombieZ...");

        // Recharger les configs
        configManager.loadAllConfigs();

        // Recharger les zones
        zoneManager.loadZones();

        // Recharger les refuges
        if (refugeManager != null) {
            refugeManager.reloadRefuges();
        }

        // Recharger le système de performance
        if (performanceManager != null) {
            performanceManager.reload();
        }

        // Recharger les paramètres du ZombieManager
        if (zombieManager != null) {
            zombieManager.loadConfigValues();
        }

        // Recharger les messages
        MessageUtils.reload();

        log(Level.INFO, "§a✓ ZombieZ rechargé!");
    }

    /**
     * Nettoie tous les mobs zombies/hostiles au démarrage du serveur
     * Évite les bugs avec des anciens zombies générés avant le reboot
     * AMÉLIORATION: Nettoie TOUS les mobs hostiles + vérifie PDC pour les mobs marqués
     *
     * NOTE: Cette méthode nettoie uniquement les chunks déjà chargés.
     * Les chunks chargés ultérieurement sont gérés par ChunkUnloadListener.onChunkLoad()
     */
    private void clearAllZombieMobs() {
        int clearedHostile = 0;
        int clearedPassive = 0;
        int clearedPDC = 0;

        // Clé PDC pour identifier les mobs ZombieZ
        org.bukkit.NamespacedKey pdcMobKey = new org.bukkit.NamespacedKey(this, "zombiez_mob");

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                EntityType type = entity.getType();

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 1: Tous les mobs hostiles potentiellement du plugin
                // ═══════════════════════════════════════════════════════════════════
                if (isHostileMobType(type)) {
                    entity.remove();
                    clearedHostile++;
                    continue;
                }

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 2: Mobs avec marqueur PDC (mobs ZombieZ persistés)
                // ═══════════════════════════════════════════════════════════════════
                if (entity instanceof LivingEntity living) {
                    if (living.getPersistentDataContainer().has(pdcMobKey,
                            org.bukkit.persistence.PersistentDataType.BYTE)) {
                        entity.remove();
                        clearedPDC++;
                        continue;
                    }
                }

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 3: Mobs passifs gérés par le plugin (avec tag/metadata)
                // ═══════════════════════════════════════════════════════════════════
                if (entity.getScoreboardTags().contains("zombiez_passive") ||
                        entity.getScoreboardTags().contains("zombiez_mob") ||
                        entity.hasMetadata("zombiez_type")) {
                    entity.remove();
                    clearedPassive++;
                }
            }
        }

        int totalCleared = clearedHostile + clearedPassive + clearedPDC;
        if (totalCleared > 0) {
            log(Level.INFO, "§7Nettoyage initial: §c" + clearedHostile + " hostiles§7, §e" + clearedPDC
                    + " PDC§7, §a" + clearedPassive + " passifs §7supprimés (total: " + totalCleared + ")");
        }
    }

    /**
     * Nettoie toutes les entités d'événements persistantes au démarrage
     * Cela inclut:
     * - TextDisplay (hologrammes des événements dynamiques et micro)
     * - ArmorStand utilisés comme marqueurs
     * - Villagers avec le tag convoy_survivor
     * - Entités avec tags event_* ou micro_event_entity
     * - Boss bars orphelines
     */
    private void clearEventEntities() {
        int clearedTextDisplays = 0;
        int clearedArmorStands = 0;
        int clearedVillagers = 0;
        int clearedEventMobs = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                boolean shouldRemove = false;
                String reason = "";

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 1: TextDisplay (hologrammes d'événements et coffres mystères)
                // ═══════════════════════════════════════════════════════════════════
                if (entity instanceof TextDisplay) {
                    // Supprimer tous les TextDisplay - ils sont recréés par les événements et coffres
                    // Note: Les TextDisplay sont utilisés par le système d'événements et coffres mystères
                    shouldRemove = true;
                    reason = "TextDisplay";
                    clearedTextDisplays++;
                }

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 2: ArmorStand avec noms d'événements
                // ═══════════════════════════════════════════════════════════════════
                else if (entity instanceof ArmorStand armorStand) {
                    String name = armorStand.getCustomName();
                    // ArmorStand d'événements (ex: "§b§l✈ LARGAGE AÉRIEN")
                    if (name != null && (
                            name.contains("LARGAGE") ||
                            name.contains("AIRDROP") ||
                            name.contains("ÉVÉNEMENT") ||
                            name.contains("EVENT"))) {
                        shouldRemove = true;
                        reason = "ArmorStand event";
                        clearedArmorStands++;
                    }
                    // ArmorStand invisibles avec équipement (marqueurs)
                    else if (!armorStand.isVisible() && armorStand.getEquipment() != null &&
                            armorStand.getEquipment().getHelmet() != null &&
                            armorStand.getEquipment().getHelmet().getType() == Material.CHEST) {
                        shouldRemove = true;
                        reason = "ArmorStand marker";
                        clearedArmorStands++;
                    }
                }

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 3: Villagers et WanderingTraders (Convoi, Refuges)
                // ═══════════════════════════════════════════════════════════════════
                else if (entity instanceof Villager || entity instanceof WanderingTrader) {
                    if (entity.getScoreboardTags().contains("convoy_survivor") ||
                        entity.getScoreboardTags().contains("no_trading") ||
                        entity.getScoreboardTags().contains("shelter_npc")) {
                        shouldRemove = true;
                        reason = "Custom NPC";
                        clearedVillagers++;
                    }
                }

                // ═══════════════════════════════════════════════════════════════════
                // NETTOYAGE 4: Entités avec tags d'événements
                // ═══════════════════════════════════════════════════════════════════
                else {
                    for (String tag : entity.getScoreboardTags()) {
                        if (tag.startsWith("event_") ||
                            tag.equals("micro_event_entity") ||
                            tag.equals("event_boss") ||
                            tag.equals("survivor_attacker")) {
                            shouldRemove = true;
                            reason = "Event tag: " + tag;
                            clearedEventMobs++;
                            break;
                        }
                    }
                }

                if (shouldRemove) {
                    entity.remove();
                }
            }
        }

        int totalCleared = clearedTextDisplays + clearedArmorStands + clearedVillagers + clearedEventMobs;
        if (totalCleared > 0) {
            log(Level.INFO, "§7Nettoyage événements: §b" + clearedTextDisplays + " TextDisplays§7, §e"
                    + clearedArmorStands + " ArmorStands§7, §a" + clearedVillagers + " Villagers§7, §c"
                    + clearedEventMobs + " mobs event §7supprimés (total: " + totalCleared + ")");
        }
    }

    /**
     * Vérifie si un type d'entité est un mob hostile géré par le plugin
     */
    private boolean isHostileMobType(EntityType type) {
        return type == EntityType.ZOMBIE ||
                type == EntityType.HUSK ||
                type == EntityType.DROWNED ||
                type == EntityType.ZOMBIE_VILLAGER ||
                type == EntityType.ZOMBIFIED_PIGLIN ||
                type == EntityType.ZOGLIN ||
                type == EntityType.RAVAGER ||
                type == EntityType.SKELETON ||
                type == EntityType.WITHER_SKELETON ||
                type == EntityType.STRAY ||
                type == EntityType.CREEPER ||
                type == EntityType.SPIDER ||
                type == EntityType.CAVE_SPIDER ||
                type == EntityType.EVOKER ||
                type == EntityType.VINDICATOR ||
                type == EntityType.PILLAGER ||
                type == EntityType.WITCH ||
                type == EntityType.BLAZE ||
                type == EntityType.GHAST ||
                type == EntityType.PIGLIN_BRUTE ||
                type == EntityType.GIANT ||
                type == EntityType.WOLF; // Loups enragés
    }

    /**
     * Vérifie si une entité est un mob ZombieZ (pour usage runtime)
     */
    private boolean isZombieZMob(Entity entity) {
        if (!(entity instanceof LivingEntity))
            return false;

        // Vérifier les tags de l'entité
        if (entity.getScoreboardTags().contains("zombiez_mob")) {
            return true;
        }

        // Vérifier le nom personnalisé
        String customName = entity.getCustomName();
        if (customName != null && entity.isCustomNameVisible()) {
            // Les mobs ZombieZ ont toujours un nom visible avec leur niveau
            return customName.contains("[Lv.") || customName.contains("❤");
        }

        return false;
    }

    /**
     * Log un message avec le préfixe du plugin
     */
    public void log(Level level, String message) {
        getLogger().log(level, MessageUtils.colorize(message));
    }

    /**
     * Affiche le banner de démarrage
     */
    private void logBanner() {
        getLogger().info("");
        getLogger().info("§c███████╗ §6██████╗ §e███╗   ███╗§a██████╗ §b██╗███████╗§d███████╗");
        getLogger().info("§c╚══███╔╝ §6██╔═══██╗§e████╗ ████║§a██╔══██╗§b██║██╔════╝§d╚══███╔╝");
        getLogger().info("§c  ███╔╝  §6██║   ██║§e██╔████╔██║§a██████╔╝§b██║█████╗  §d  ███╔╝ ");
        getLogger().info("§c ███╔╝   §6██║   ██║§e██║╚██╔╝██║§a██╔══██╗§b██║██╔══╝  §d ███╔╝  ");
        getLogger().info("§c███████╗ §6╚██████╔╝§e██║ ╚═╝ ██║§a██████╔╝§b██║███████╗§d███████╗");
        getLogger().info("§c╚══════╝ §6 ╚═════╝ §e╚═╝     ╚═╝§a╚═════╝ §b╚═╝╚══════╝§d╚══════╝");
        getLogger().info("");
        getLogger().info("§7Version: §e" + getDescription().getVersion() + " §7| §7By: §bRinaorc Studio");
        getLogger().info("");
    }
}
