package com.rinaorc.zombiez.data;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gestionnaire de configuration centralisé
 * Gère tous les fichiers YAML du plugin avec cache
 */
public class ConfigManager {

    private final ZombieZPlugin plugin;
    
    // Cache des configurations
    private final Map<String, FileConfiguration> configCache = new HashMap<>();
    private final Map<String, File> fileCache = new HashMap<>();

    // Configurations principales
    @Getter private FileConfiguration mainConfig;
    @Getter private FileConfiguration zonesConfig;
    @Getter private FileConfiguration messagesConfig;
    @Getter private FileConfiguration zombiesConfig;
    @Getter private FileConfiguration lootConfig;
    @Getter private FileConfiguration economyConfig;
    @Getter private FileConfiguration awakensConfig;
    @Getter private FileConfiguration eventsConfig;

    // Paramètres de jeu (cache pour accès rapide)
    @Getter private int maxPlayers;
    @Getter private boolean debugMode;
    @Getter private String serverLanguage;
    @Getter private int zoneCheckInterval;
    @Getter private int autoSaveInterval;

    // Paramètres de performance (cache pour accès rapide)
    @Getter private double despawnDistance;
    @Getter private int cleanupInterval;
    @Getter private int cleanupBatchSize;
    @Getter private boolean dropCleanupEnabled;
    @Getter private int dropRemovalDelay;
    @Getter private int maxGlobalMobs;

    public ConfigManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge toutes les configurations
     */
    public void loadAllConfigs() {
        // Configuration principale
        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();
        
        // Autres configurations
        zonesConfig = loadConfig("zones.yml");
        messagesConfig = loadConfig("messages.yml");
        zombiesConfig = loadConfig("zombies.yml");
        lootConfig = loadConfig("loot.yml");
        economyConfig = loadConfig("economy.yml");
        awakensConfig = loadConfig("awakens.yml");
        eventsConfig = loadConfig("events.yml");

        // Cache des paramètres fréquemment utilisés
        cacheMainParameters();

        plugin.log(Level.INFO, "§7Configuration chargée: §e" + configCache.size() + " §7fichiers");
    }

    /**
     * Charge un fichier de configuration spécifique
     */
    public FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        // Créer le fichier par défaut s'il n'existe pas
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Merge avec les defaults du jar
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defaultConfig);
        }

        // Cache
        configCache.put(fileName, config);
        fileCache.put(fileName, file);

        return config;
    }

    /**
     * Sauvegarde une configuration spécifique
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configCache.get(fileName);
        File file = fileCache.get(fileName);
        
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.log(Level.SEVERE, "§cErreur sauvegarde " + fileName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Recharge une configuration spécifique
     */
    public void reloadConfig(String fileName) {
        File file = fileCache.get(fileName);
        if (file != null && file.exists()) {
            configCache.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
    }

    /**
     * Cache les paramètres principaux pour accès ultra-rapide
     */
    private void cacheMainParameters() {
        maxPlayers = mainConfig.getInt("server.max-players", 200);
        debugMode = mainConfig.getBoolean("server.debug-mode", false);
        serverLanguage = mainConfig.getString("server.language", "fr");
        zoneCheckInterval = mainConfig.getInt("performance.zone-check-interval", 10);
        autoSaveInterval = mainConfig.getInt("performance.auto-save-interval", 300);

        // Paramètres de performance (clearlag intelligent)
        despawnDistance = mainConfig.getDouble("performance.despawn-distance", 64.0);
        cleanupInterval = mainConfig.getInt("performance.cleanup-interval", 30);
        cleanupBatchSize = mainConfig.getInt("performance.cleanup-batch-size", 50);
        dropCleanupEnabled = mainConfig.getBoolean("performance.drop-cleanup-enabled", true);
        dropRemovalDelay = mainConfig.getInt("performance.drop-removal-delay", 10);
        maxGlobalMobs = mainConfig.getInt("zombies.max-global", 500);
    }

    /**
     * Obtient une valeur de configuration avec cache
     */
    public <T> T get(String configName, String path, T defaultValue) {
        FileConfiguration config = configCache.get(configName);
        if (config == null) return defaultValue;
        
        Object value = config.get(path);
        if (value == null) return defaultValue;
        
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Obtient une configuration depuis le cache
     */
    public FileConfiguration getConfig(String fileName) {
        return configCache.get(fileName);
    }
    
    /**
     * Obtient la configuration principale (config.yml)
     */
    public FileConfiguration getConfig() {
        return mainConfig;
    }

    /**
     * Vérifie si une configuration existe
     */
    public boolean hasConfig(String fileName) {
        return configCache.containsKey(fileName);
    }

    /**
     * Sauvegarde toutes les configurations
     */
    public void saveAllConfigs() {
        for (String fileName : configCache.keySet()) {
            saveConfig(fileName);
        }
    }

    /**
     * Recharge toutes les configurations
     */
    public void reloadAllConfigs() {
        loadAllConfigs();
    }
}
