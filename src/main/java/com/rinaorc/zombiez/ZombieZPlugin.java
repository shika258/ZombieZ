package com.rinaorc.zombiez;

import com.rinaorc.zombiez.commands.admin.ClassAdminCommand;
import com.rinaorc.zombiez.commands.admin.ItemAdminCommand;
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

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
    @Getter private ConfigManager configManager;
    @Getter private DatabaseManager databaseManager;
    @Getter private ZoneManager zoneManager;
    @Getter private PlayerDataManager playerDataManager;
    @Getter private EconomyManager economyManager;
    @Getter private ItemManager itemManager;
    @Getter private SetBonusManager setBonusManager;
    
    // Système Zombies
    @Getter private ZombieManager zombieManager;
    @Getter private SpawnSystem spawnSystem;
    @Getter private EventManager eventManager;
    
    // Système Économie Phase 4
    @Getter private BankManager bankManager;
    
    // Système Progression Phase 5
    @Getter private AchievementManager achievementManager;
    @Getter private SkillTreeManager skillTreeManager;
    @Getter private LeaderboardManager leaderboardManager;
    @Getter private MissionManager missionManager;
    @Getter private BattlePassManager battlePassManager;
    @Getter private CosmeticManager cosmeticManager;
    @Getter private ProgressionManager progressionManager;
    @Getter private com.rinaorc.zombiez.economy.PrestigeSystem prestigeSystem;
    @Getter private DailyRewardManager dailyRewardManager;
    
    // Nouveaux systèmes Phase 6
    @Getter private com.rinaorc.zombiez.party.PartyManager partyManager;
    @Getter private com.rinaorc.zombiez.momentum.MomentumManager momentumManager;
    @Getter private com.rinaorc.zombiez.zones.SecretZoneManager secretZoneManager;

    // Système de Pouvoirs et Item Level
    @Getter private com.rinaorc.zombiez.items.power.PowerManager powerManager;
    @Getter private com.rinaorc.zombiez.items.power.PowerTriggerListener powerTriggerListener;

    // Listeners stockés pour accès externe
    @Getter private PlayerMoveListener playerMoveListener;
    @Getter private ItemListener itemListener;
    @Getter private BowListener bowListener;

    // Systèmes de spawn spécialisés
    @Getter private com.rinaorc.zombiez.zombies.spawning.BossSpawnSystem bossSpawnSystem;
    @Getter private com.rinaorc.zombiez.zombies.spawning.HordeEventSystem hordeEventSystem;

    // Système de mobs passifs et nourriture
    @Getter private com.rinaorc.zombiez.mobs.PassiveMobManager passiveMobManager;

    // Système de Boss Bar Dynamique
    @Getter private com.rinaorc.zombiez.ui.DynamicBossBarManager dynamicBossBarManager;

    // Système de Consommables
    @Getter private com.rinaorc.zombiez.consumables.ConsumableManager consumableManager;

    // Système d'Événements Dynamiques
    @Getter private com.rinaorc.zombiez.events.dynamic.DynamicEventManager dynamicEventManager;

    // Système de Météo Dynamique
    @Getter private com.rinaorc.zombiez.weather.WeatherManager weatherManager;

    // Système de Classes
    @Getter private com.rinaorc.zombiez.classes.ClassManager classManager;

    // État du plugin
    @Getter private boolean fullyLoaded = false;

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
            log(Level.INFO, "§7Sauvegarde des données joueurs...");
            playerDataManager.saveAllSync();
        }

        // Cleanup du système d'items
        if (itemManager != null) {
            log(Level.INFO, "§7Nettoyage du cache d'items...");
            itemManager.cleanup();
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

        // Player Data Manager - Cache et persistance des données joueurs
        playerDataManager = new PlayerDataManager(this);

        // Economy Manager - Gestion des monnaies
        economyManager = new EconomyManager(this);

        // Item Manager - Système de loot procédural
        itemManager = new ItemManager(this);

        // Set Bonus Manager - Bonus des sets d'équipement
        setBonusManager = new SetBonusManager(this);

        // Power Manager - Système de pouvoirs et Item Level
        powerManager = new com.rinaorc.zombiez.items.power.PowerManager(this);
        powerManager.loadFromConfig(configManager.getPowersConfig());

        // Power Trigger Listener - Écoute les événements pour déclencher les pouvoirs
        powerTriggerListener = new com.rinaorc.zombiez.items.power.PowerTriggerListener(this, powerManager);

        // Zombie Manager - Gestion des zombies
        zombieManager = new ZombieManager(this);
        
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
        
        // Party Manager - Système de groupe
        partyManager = new com.rinaorc.zombiez.party.PartyManager(this);
        
        // Momentum Manager - Streaks, Combos, Fever
        momentumManager = new com.rinaorc.zombiez.momentum.MomentumManager(this);
        
        // Secret Zone Manager - Zones secrètes et événements
        secretZoneManager = new com.rinaorc.zombiez.zones.SecretZoneManager(this);

        // ===== Système Mobs Passifs et Nourriture =====

        // Passive Mob Manager - Mobs passifs et loot nourriture
        passiveMobManager = new com.rinaorc.zombiez.mobs.PassiveMobManager(this);

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

        // ===== Système Météo Dynamique =====

        // Weather Manager - Météo dynamique avec effets gameplay
        weatherManager = new com.rinaorc.zombiez.weather.WeatherManager(this);
        weatherManager.loadConfig(configManager.loadConfig("weather.yml"));
        weatherManager.start();

        // ===== Système de Classes =====

        // Class Manager - Système de classes complet
        classManager = new com.rinaorc.zombiez.classes.ClassManager(this);
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
        com.rinaorc.zombiez.commands.admin.ConsumableAdminCommand consumableCmd =
            new com.rinaorc.zombiez.commands.admin.ConsumableAdminCommand(this);
        getCommand("zzconsumable").setExecutor(consumableCmd);
        getCommand("zzconsumable").setTabCompleter(consumableCmd);

        // Commandes Admin Événements Dynamiques
        com.rinaorc.zombiez.commands.admin.EventAdminCommand eventCmd =
            new com.rinaorc.zombiez.commands.admin.EventAdminCommand(this);
        getCommand("zzevent").setExecutor(eventCmd);
        getCommand("zzevent").setTabCompleter(eventCmd);

        // Commandes Admin Météo Dynamique
        com.rinaorc.zombiez.commands.admin.WeatherAdminCommand weatherCmd =
            new com.rinaorc.zombiez.commands.admin.WeatherAdminCommand(this);
        getCommand("zzweather").setExecutor(weatherCmd);
        getCommand("zzweather").setTabCompleter(weatherCmd);

        // Commandes Admin Classes
        ClassAdminCommand classAdminCmd = new ClassAdminCommand(this);
        getCommand("zzclassadmin").setExecutor(classAdminCmd);
        getCommand("zzclassadmin").setTabCompleter(classAdminCmd);

        // Commandes Joueur - Base
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("zone").setExecutor(new ZoneCommand(this));
        getCommand("checkpoint").setExecutor(new CheckpointCommand(this));
        getCommand("refuge").setExecutor(new RefugeCommand(this));
        
        // Commandes Joueur - Économie
        BankCommand bankCmd = new BankCommand(this);
        getCommand("bank").setExecutor(bankCmd);
        getCommand("bank").setTabCompleter(bankCmd);
        
        // Commandes Joueur - Progression
        ProgressionCommand progressionCmd = new ProgressionCommand(this);
        getCommand("progression").setExecutor(progressionCmd);
        getCommand("progression").setTabCompleter(progressionCmd);

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
        
        // Listeners système d'items
        itemListener = new ItemListener(this);
        pm.registerEvents(itemListener, this);
        pm.registerEvents(new ItemCompareGUI.GUIListener(this), this);
        pm.registerEvents(new LootRevealGUI.LootGUIListener(this), this);

        // Listener système de tir amélioré (arcs/arbalètes)
        bowListener = new BowListener(this);
        pm.registerEvents(bowListener, this);

        // Listener système de pouvoirs
        if (powerTriggerListener != null) {
            pm.registerEvents(powerTriggerListener, this);
        }
        
        // Listener système de zombies
        pm.registerEvents(new ZombieListener(this), this);

        // Listener pour bloquer les spawns de mobs vanilla
        pm.registerEvents(new MobSpawnListener(this), this);

        // Listeners système mobs passifs et nourriture
        if (passiveMobManager != null) {
            pm.registerEvents(passiveMobManager, this);
            pm.registerEvents(new com.rinaorc.zombiez.mobs.food.FoodListener(this), this);
        }

        // Listener système consommables
        if (consumableManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.consumables.ConsumableListener(this, consumableManager), this);
        }

        // Listener système événements dynamiques
        if (dynamicEventManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.events.dynamic.DynamicEventListener(this, dynamicEventManager), this);
        }

        // Listener système météo dynamique
        if (weatherManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.weather.WeatherListener(this, weatherManager), this);
        }

        // Listeners système de progression
        pm.registerEvents(new MissionGUI.MissionGUIListener(this), this);
        pm.registerEvents(new BattlePassGUI.BattlePassGUIListener(this), this);

        // Listener système de classes
        if (classManager != null) {
            pm.registerEvents(new com.rinaorc.zombiez.classes.ClassListener(this), this);
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

        // ActionBar permanent pour tous les joueurs (Zone, Combo, Streak, Points)
        new com.rinaorc.zombiez.listeners.ActionBarTask(this).start();

        // Nettoyage périodique du cache des indicateurs de dégâts (toutes les 30 secondes)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            com.rinaorc.zombiez.combat.DamageIndicator.cleanup();
        }, 20L * 30, 20L * 30);
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
        
        // Recharger les messages
        MessageUtils.reload();
        
        log(Level.INFO, "§a✓ ZombieZ rechargé!");
    }

    /**
     * Nettoie tous les mobs zombies/hostiles personnalisés au démarrage du serveur
     * Évite les bugs avec des anciens zombies générés avant le reboot
     */
    private void clearAllZombieMobs() {
        int cleared = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // Vérifier si c'est un mob zombie/hostile du plugin
                if (isZombieZMob(entity)) {
                    entity.remove();
                    cleared++;
                }
            }
        }
        if (cleared > 0) {
            log(Level.INFO, "§7" + cleared + " mobs zombies nettoyés.");
        }
    }

    /**
     * Vérifie si une entité est un mob ZombieZ
     */
    private boolean isZombieZMob(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

        // Vérifier le nom personnalisé ou le type
        EntityType type = entity.getType();

        // Types de mobs hostiles du plugin
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK ||
            type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER ||
            type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.ZOGLIN ||
            type == EntityType.RAVAGER || type == EntityType.SKELETON ||
            type == EntityType.WITHER_SKELETON || type == EntityType.STRAY) {

            // Vérifier si c'est un mob ZombieZ via le nom ou les tags
            String customName = entity.getCustomName();
            if (customName != null && customName.contains("ZZ_")) {
                return true;
            }

            // Vérifier les tags de l'entité
            if (entity.getScoreboardTags().contains("zombiez_mob")) {
                return true;
            }

            // Si le mob a un nom personnalisé visible, c'est probablement un mob du plugin
            if (entity.isCustomNameVisible()) {
                return true;
            }
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
