package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Refuge;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des refuges (points de sauvegarde et téléportation)
 * Charge et gère les refuges depuis refuges.yml
 */
public class RefugeManager {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<Integer, Refuge> refugesById = new ConcurrentHashMap<>();

    // Cache pour recherche rapide par position de beacon
    private final Map<String, Refuge> refugesByBeaconPos = new ConcurrentHashMap<>();

    @Getter
    private FileConfiguration refugesConfig;

    public RefugeManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge tous les refuges depuis refuges.yml
     */
    public void loadRefuges() {
        refugesById.clear();
        refugesByBeaconPos.clear();

        // Charger le fichier refuges.yml
        File refugesFile = new File(plugin.getDataFolder(), "refuges.yml");
        if (!refugesFile.exists()) {
            plugin.saveResource("refuges.yml", false);
        }

        refugesConfig = YamlConfiguration.loadConfiguration(refugesFile);
        ConfigurationSection refugesSection = refugesConfig.getConfigurationSection("refuges");

        if (refugesSection == null) {
            plugin.log(Level.WARNING, "§eAucun refuge configuré dans refuges.yml");
            return;
        }

        int loaded = 0;
        for (String key : refugesSection.getKeys(false)) {
            try {
                int refugeId = Integer.parseInt(key);
                ConfigurationSection section = refugesSection.getConfigurationSection(key);

                if (section != null) {
                    Refuge refuge = loadRefugeFromConfig(refugeId, section);
                    if (refuge != null) {
                        registerRefuge(refuge);
                        loaded++;
                    }
                }
            } catch (NumberFormatException e) {
                plugin.log(Level.WARNING, "§eID de refuge invalide: " + key);
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "§cErreur chargement refuge " + key + ": " + e.getMessage());
            }
        }

        plugin.log(Level.INFO, "§a✓ " + loaded + " refuges chargés");
    }

    /**
     * Charge un refuge depuis la configuration
     */
    private Refuge loadRefugeFromConfig(int id, ConfigurationSection section) {
        String name = section.getString("name");
        if (name == null) return null;

        // Charger la zone protégée
        ConfigurationSection protectedArea = section.getConfigurationSection("protected-area");
        if (protectedArea == null) return null;

        ConfigurationSection corner1 = protectedArea.getConfigurationSection("corner1");
        ConfigurationSection corner2 = protectedArea.getConfigurationSection("corner2");
        if (corner1 == null || corner2 == null) return null;

        int c1x = corner1.getInt("x"), c1y = corner1.getInt("y"), c1z = corner1.getInt("z");
        int c2x = corner2.getInt("x"), c2y = corner2.getInt("y"), c2z = corner2.getInt("z");

        // Normaliser les min/max
        int minX = Math.min(c1x, c2x), maxX = Math.max(c1x, c2x);
        int minY = Math.min(c1y, c2y), maxY = Math.max(c1y, c2y);
        int minZ = Math.min(c1z, c2z), maxZ = Math.max(c1z, c2z);

        // Charger la position du beacon
        ConfigurationSection beacon = section.getConfigurationSection("beacon");
        if (beacon == null) return null;

        int beaconX = beacon.getInt("x"), beaconY = beacon.getInt("y"), beaconZ = beacon.getInt("z");

        // Charger le point de spawn
        ConfigurationSection spawnPoint = section.getConfigurationSection("spawn-point");
        if (spawnPoint == null) return null;

        double spawnX = spawnPoint.getDouble("x");
        double spawnY = spawnPoint.getDouble("y");
        double spawnZ = spawnPoint.getDouble("z");
        float spawnYaw = (float) spawnPoint.getDouble("yaw", 0);
        float spawnPitch = (float) spawnPoint.getDouble("pitch", 0);

        return Refuge.builder()
            .id(id)
            .name(name)
            .description(section.getString("description", ""))
            .protectedMinX(minX).protectedMaxX(maxX)
            .protectedMinY(minY).protectedMaxY(maxY)
            .protectedMinZ(minZ).protectedMaxZ(maxZ)
            .beaconX(beaconX).beaconY(beaconY).beaconZ(beaconZ)
            .spawnX(spawnX).spawnY(spawnY).spawnZ(spawnZ)
            .spawnYaw(spawnYaw).spawnPitch(spawnPitch)
            .cost(section.getLong("cost", 100))
            .requiredLevel(section.getInt("required-level", 1))
            .build();
    }

    /**
     * Enregistre un refuge dans les indexes
     */
    private void registerRefuge(Refuge refuge) {
        refugesById.put(refuge.getId(), refuge);

        // Indexer par position de beacon pour recherche rapide
        String beaconKey = getBeaconKey(refuge.getBeaconX(), refuge.getBeaconY(), refuge.getBeaconZ());
        refugesByBeaconPos.put(beaconKey, refuge);

        plugin.log(Level.INFO, "§7  - Refuge " + refuge.getId() + ": §e" + refuge.getName() +
            " §7(" + refuge.getProtectedAreaInfo() + ")");
    }

    /**
     * Génère une clé unique pour une position de beacon
     */
    private String getBeaconKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    /**
     * Obtient un refuge par son ID
     */
    public Refuge getRefugeById(int id) {
        return refugesById.get(id);
    }

    /**
     * Obtient le refuge à une position de beacon donnée
     */
    public Refuge getRefugeAtBeacon(Location loc) {
        String key = getBeaconKey(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return refugesByBeaconPos.get(key);
    }

    /**
     * Obtient le refuge à une position de beacon donnée
     */
    public Refuge getRefugeAtBeacon(int x, int y, int z) {
        String key = getBeaconKey(x, y, z);
        return refugesByBeaconPos.get(key);
    }

    /**
     * Vérifie si une location est dans une zone protégée de refuge
     */
    public boolean isInAnyRefugeProtectedArea(Location loc) {
        for (Refuge refuge : refugesById.values()) {
            if (refuge.isInProtectedArea(loc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si une position est dans une zone protégée de refuge
     */
    public boolean isInAnyRefugeProtectedArea(int x, int y, int z) {
        for (Refuge refuge : refugesById.values()) {
            if (refuge.isInProtectedArea(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtient le refuge contenant une location (dans sa zone protégée)
     */
    public Refuge getRefugeAt(Location loc) {
        for (Refuge refuge : refugesById.values()) {
            if (refuge.isInProtectedArea(loc)) {
                return refuge;
            }
        }
        return null;
    }

    /**
     * Obtient tous les refuges
     */
    public Collection<Refuge> getAllRefuges() {
        return Collections.unmodifiableCollection(refugesById.values());
    }

    /**
     * Obtient les refuges triés par ID
     */
    public List<Refuge> getRefugesSorted() {
        return refugesById.values().stream()
            .sorted(Comparator.comparingInt(Refuge::getId))
            .toList();
    }

    /**
     * Recharge les refuges depuis le fichier
     */
    public void reloadRefuges() {
        loadRefuges();
    }
}
