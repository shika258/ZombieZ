package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.api.events.PlayerZoneChangeEvent;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des zones de jeu
 * Optimisé pour vérification rapide de 200+ joueurs
 *
 * Utilise un TreeMap pour une recherche O(log n) de la zone par coordonnée Z
 *
 * IMPORTANT: La progression se fait du SUD vers le NORD
 * - Le spawn est à Z ≈ 10400 (sud de la map)
 * - Les joueurs progressent vers Z = -90 (en allant vers le NORD)
 * - Plus le Z est FAIBLE, plus le joueur a progressé
 */
public class ZoneManager {

    private final ZombieZPlugin plugin;

    // Constantes de progression sur l'axe Z
    public static final int START_Z = 10400; // Zone de départ (spawn au sud)
    public static final int END_Z = -90;     // Zone finale (objectif au nord)

    // Stockage des zones
    @Getter
    private final Map<Integer, Zone> zonesById = new ConcurrentHashMap<>();
    private final Map<String, Zone> zonesByName = new ConcurrentHashMap<>();
    
    // TreeMap pour recherche rapide par Z (clé = minZ de la zone)
    private final TreeMap<Integer, Zone> zonesByZ = new TreeMap<>();

    // Cache de la zone actuelle par joueur (évite les recalculs)
    private final Map<UUID, Integer> playerZoneCache = new ConcurrentHashMap<>();

    // Zone par défaut (spawn)
    @Getter
    private Zone spawnZone;

    // Configuration
    @Getter
    private String gameWorld = "world";
    private int minX = 0;
    private int maxX = 1242;

    public ZoneManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge toutes les zones depuis la configuration
     */
    public void loadZones() {
        zonesById.clear();
        zonesByName.clear();
        zonesByZ.clear();

        FileConfiguration config = plugin.getConfigManager().getZonesConfig();
        
        // Paramètres globaux
        gameWorld = config.getString("settings.world", "world");
        minX = config.getInt("settings.min-x", 0);
        maxX = config.getInt("settings.max-x", 1242);

        // Charger chaque zone
        ConfigurationSection zonesSection = config.getConfigurationSection("zones");
        if (zonesSection == null) {
            plugin.log(Level.WARNING, "§eAucune zone configurée, création des zones par défaut...");
            createDefaultZones();
            return;
        }

        for (String key : zonesSection.getKeys(false)) {
            try {
                Zone zone = loadZoneFromConfig(zonesSection.getConfigurationSection(key));
                registerZone(zone);
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "§cErreur chargement zone " + key + ": " + e.getMessage());
            }
        }

        // S'assurer qu'on a une zone spawn
        if (spawnZone == null) {
            spawnZone = Zone.createSpawnZone();
            registerZone(spawnZone);
        }

        plugin.log(Level.INFO, "§a✓ " + zonesById.size() + " zones chargées");
    }

    /**
     * Charge une zone depuis la configuration
     */
    private Zone loadZoneFromConfig(ConfigurationSection section) {
        int id = section.getInt("id");
        String name = section.getString("name", "unknown");
        
        Zone zone = Zone.builder()
            .id(id)
            .name(name)
            .displayName(section.getString("display-name", name))
            .description(section.getString("description", ""))
            .minZ(section.getInt("min-z", 0))
            .maxZ(section.getInt("max-z", 1000))
            .difficulty(section.getInt("difficulty", 1))
            .stars(section.getInt("stars", 1))
            .biomeType(section.getString("biome", "PLAINS"))
            .theme(section.getString("theme", "default"))
            .xpMultiplier(section.getDouble("multipliers.xp", 1.0))
            .lootMultiplier(section.getDouble("multipliers.loot", 1.0))
            .spawnRateMultiplier(section.getDouble("multipliers.spawn-rate", 1.0))
            .zombieHealthMultiplier(section.getDouble("multipliers.zombie-health", 1.0))
            .zombieDamageMultiplier(section.getDouble("multipliers.zombie-damage", 1.0))
            .zombieSpeedMultiplier(section.getDouble("multipliers.zombie-speed", 1.0))
            .pvpEnabled(section.getBoolean("pvp-enabled", false))
            .safeZone(section.getBoolean("safe-zone", false))
            .bossZone(section.getBoolean("boss-zone", false))
            .environmentalEffect(section.getString("environment.effect", "NONE"))
            .environmentalDamage(section.getDouble("environment.damage", 0))
            .environmentalInterval(section.getInt("environment.interval", 20))
            .refugeId(section.getInt("refuge-id", -1))
            .refugeLocation(null) // Chargé séparément si nécessaire
            .minZombieLevel(section.getInt("zombie-level.min", 1))
            .maxZombieLevel(section.getInt("zombie-level.max", 5))
            .allowedZombieTypes(section.getStringList("zombie-types").toArray(new String[0]))
            .color(section.getString("color", "§f"))
            .build();

        return zone;
    }

    /**
     * Enregistre une zone dans tous les indexes
     */
    private void registerZone(Zone zone) {
        zonesById.put(zone.getId(), zone);
        zonesByName.put(zone.getName().toLowerCase(), zone);
        zonesByZ.put(zone.getMinZ(), zone);
        
        if (zone.isSafeZone() && zone.getId() == 0) {
            spawnZone = zone;
        }
    }

    /**
     * Crée les zones par défaut si aucune configuration
     * Les zones sont ordonnées du SUD (spawn, Z élevé) vers le NORD (citadelle, Z faible)
     */
    private void createDefaultZones() {
        // Zone Spawn (au sud, Z élevé)
        spawnZone = Zone.createSpawnZone();
        registerZone(spawnZone);

        // Zone 1: Village de départ (première zone après le spawn)
        registerZone(Zone.standardZone(1, "Village de Départ", 9200, 10200, 1)
            .description("Les premiers pas dans l'apocalypse")
            .biomeType("PLAINS")
            .theme("medieval_ruins")
            .refugeId(1)
            .minZombieLevel(1)
            .maxZombieLevel(5)
            .allowedZombieTypes(new String[]{"WALKER", "CRAWLER"})
            .color("§a")
            .build());

        // Zone 2: Plaines Abandonnées
        registerZone(Zone.standardZone(2, "Plaines Abandonnées", 8200, 9200, 2)
            .description("Terres agricoles ravagées par l'infection")
            .biomeType("PLAINS")
            .theme("abandoned_farms")
            .refugeId(2)
            .minZombieLevel(5)
            .maxZombieLevel(10)
            .allowedZombieTypes(new String[]{"WALKER", "CRAWLER", "RUNNER"})
            .color("§a")
            .build());

        // Zone 3: Désert
        registerZone(Zone.standardZone(3, "Désert", 7200, 8200, 3)
            .description("Ruines antiques sous un soleil de plomb")
            .biomeType("DESERT")
            .theme("desert_ruins")
            .environmentalEffect("HEAT")
            .environmentalDamage(1.0)
            .environmentalInterval(600) // 30 secondes
            .refugeId(3)
            .minZombieLevel(10)
            .maxZombieLevel(15)
            .allowedZombieTypes(new String[]{"WALKER", "RUNNER", "MUMMY"})
            .color("§e")
            .build());

        // Zone 4: Forêt Sombre
        registerZone(Zone.standardZone(4, "Forêt Sombre", 6200, 7200, 4)
            .description("Une forêt corrompue où la lumière ne pénètre pas")
            .biomeType("DARK_FOREST")
            .theme("dark_forest")
            .refugeId(4)
            .minZombieLevel(15)
            .maxZombieLevel(20)
            .allowedZombieTypes(new String[]{"RUNNER", "CRAWLER", "LURKER", "SHADOW"})
            .color("§2")
            .build());

        // Zone 5: Marécages
        registerZone(Zone.standardZone(5, "Marécages", 5400, 6200, 5)
            .description("Eaux toxiques et brouillard pestilentiel")
            .biomeType("SWAMP")
            .theme("toxic_swamp")
            .environmentalEffect("TOXIC")
            .environmentalDamage(0.5)
            .environmentalInterval(400) // 20 secondes
            .refugeId(5)
            .minZombieLevel(20)
            .maxZombieLevel(25)
            .allowedZombieTypes(new String[]{"BLOATER", "SPITTER", "SWIMMER"})
            .color("§2")
            .build());

        // Zone PvP
        registerZone(Zone.standardZone(6, "Zone PvP - L'Arène", 5200, 5400, 5)
            .description("Seuls les plus forts survivent")
            .biomeType("PLAINS")
            .theme("pvp_arena")
            .pvpEnabled(true)
            .refugeId(-1) // Pas de refuge
            .minZombieLevel(20)
            .maxZombieLevel(25)
            .allowedZombieTypes(new String[]{"WALKER", "RUNNER"})
            .color("§c")
            .build());

        // Zone 6: Montagnes
        registerZone(Zone.standardZone(7, "Montagnes", 4200, 5200, 6)
            .description("Pics escarpés et forteresses oubliées")
            .biomeType("JAGGED_PEAKS")
            .theme("mountain_fortress")
            .refugeId(6)
            .minZombieLevel(25)
            .maxZombieLevel(30)
            .allowedZombieTypes(new String[]{"CLIMBER", "GIANT", "BERSERKER"})
            .color("§7")
            .build());

        // Zone 7: Toundra
        registerZone(Zone.standardZone(8, "Toundra", 3200, 4200, 7)
            .description("Froid mortel et isolation absolue")
            .biomeType("FROZEN_PEAKS")
            .theme("frozen_wasteland")
            .environmentalEffect("COLD")
            .environmentalDamage(1.0)
            .environmentalInterval(400)
            .refugeId(7)
            .minZombieLevel(30)
            .maxZombieLevel(35)
            .allowedZombieTypes(new String[]{"FROZEN", "YETI", "WENDIGO"})
            .color("§b")
            .build());

        // Zone 8: Terres Corrompues
        registerZone(Zone.standardZone(9, "Terres Corrompues", 2200, 3200, 8)
            .description("Radiation et mutation - Pale Garden")
            .biomeType("PALE_GARDEN")
            .theme("corrupted_lands")
            .environmentalEffect("RADIATION")
            .environmentalDamage(2.0)
            .environmentalInterval(200)
            .refugeId(8)
            .minZombieLevel(35)
            .maxZombieLevel(40)
            .allowedZombieTypes(new String[]{"MUTANT", "ABOMINATION", "CREAKING"})
            .color("§5")
            .build());

        // Zone 9: Enfer
        registerZone(Zone.standardZone(10, "Enfer", 700, 2200, 9)
            .description("Le portail vers l'enfer s'est ouvert")
            .biomeType("NETHER_WASTES")
            .theme("hellscape")
            .environmentalEffect("FIRE")
            .environmentalDamage(1.5)
            .environmentalInterval(300)
            .refugeId(9)
            .minZombieLevel(40)
            .maxZombieLevel(47)
            .allowedZombieTypes(new String[]{"DEMON", "INFERNAL", "ARCHON"})
            .color("§4")
            .build());

        // Zone 10: Citadelle Finale (zone boss au nord, Z faible)
        registerZone(Zone.standardZone(11, "Citadelle Finale", -90, 700, 10)
            .description("L'origine du fléau - Affrontez Patient Zéro")
            .biomeType("DEEP_DARK")
            .theme("final_citadel")
            .bossZone(true)
            .refugeId(10)
            .minZombieLevel(47)
            .maxZombieLevel(50)
            .allowedZombieTypes(new String[]{"ELITE", "CHAMPION", "BOSS"})
            .color("§d")
            .build());

        plugin.log(Level.INFO, "§7Zones par défaut créées");
    }

    /**
     * Obtient la zone à une coordonnée Z donnée
     * Complexité: O(log n) grâce au TreeMap
     */
    public Zone getZoneAt(int z) {
        // Trouver la zone dont le minZ est <= z
        Map.Entry<Integer, Zone> entry = zonesByZ.floorEntry(z);
        
        if (entry != null) {
            Zone zone = entry.getValue();
            if (zone.containsZ(z)) {
                return zone;
            }
        }
        
        // Fallback au spawn si hors limites
        return spawnZone;
    }

    /**
     * Obtient la zone à une location donnée
     */
    public Zone getZoneAt(Location location) {
        // Vérifier si c'est le bon monde
        if (!location.getWorld().getName().equals(gameWorld)) {
            return spawnZone;
        }
        
        // Vérifier les limites X
        int x = location.getBlockX();
        if (x < minX || x > maxX) {
            return spawnZone; // Hors limites de la map
        }
        
        return getZoneAt(location.getBlockZ());
    }

    /**
     * Obtient une zone par son ID
     */
    public Zone getZoneById(int id) {
        return zonesById.get(id);
    }

    /**
     * Obtient une zone par son nom
     */
    public Zone getZoneByName(String name) {
        return zonesByName.get(name.toLowerCase());
    }

    /**
     * Vérifie les zones de tous les joueurs en ligne
     * Optimisé pour être appelé fréquemment
     */
    public void checkPlayersZones() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerZone(player);
        }
    }

    /**
     * Vérifie et met à jour la zone d'un joueur
     */
    public void checkPlayerZone(Player player) {
        // Vérifier le monde
        if (!player.getWorld().getName().equals(gameWorld)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int currentZ = player.getLocation().getBlockZ();
        Zone newZone = getZoneAt(currentZ);
        
        // Vérifier si la zone a changé
        Integer cachedZoneId = playerZoneCache.get(uuid);
        
        if (cachedZoneId == null || cachedZoneId != newZone.getId()) {
            // La zone a changé !
            Zone oldZone = cachedZoneId != null ? zonesById.get(cachedZoneId) : null;
            
            // Mettre à jour le cache
            playerZoneCache.put(uuid, newZone.getId());
            
            // Mettre à jour les données du joueur
            PlayerData data = plugin.getPlayerDataManager().getPlayer(uuid);
            if (data != null) {
                boolean isNewZone = data.updateZone(newZone.getId());
                data.setCachedZone(newZone);
                
                // Fire l'événement
                Bukkit.getPluginManager().callEvent(
                    new PlayerZoneChangeEvent(player, oldZone, newZone, isNewZone)
                );
            }
        }
    }

    /**
     * Obtient la zone actuelle d'un joueur (depuis le cache)
     */
    public Zone getPlayerZone(Player player) {
        Integer zoneId = playerZoneCache.get(player.getUniqueId());
        if (zoneId != null) {
            return zonesById.get(zoneId);
        }
        
        // Pas en cache, calculer
        Zone zone = getZoneAt(player.getLocation());
        playerZoneCache.put(player.getUniqueId(), zone.getId());
        return zone;
    }

    /**
     * Obtient la zone actuelle d'un joueur par UUID
     */
    public Zone getPlayerZone(UUID uuid) {
        Integer zoneId = playerZoneCache.get(uuid);
        if (zoneId != null) {
            return zonesById.get(zoneId);
        }
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return getPlayerZone(player);
        }
        
        return spawnZone;
    }

    /**
     * Supprime un joueur du cache (déconnexion)
     */
    public void removeFromCache(UUID uuid) {
        playerZoneCache.remove(uuid);
    }

    /**
     * Obtient toutes les zones
     */
    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zonesById.values());
    }

    /**
     * Obtient les zones triées par ID
     */
    public List<Zone> getZonesSorted() {
        return zonesById.values().stream()
            .sorted(Comparator.comparingInt(Zone::getId))
            .toList();
    }

    /**
     * Vérifie si une location est dans les limites de la map
     */
    public boolean isInMapBounds(Location location) {
        if (!location.getWorld().getName().equals(gameWorld)) {
            return false;
        }

        int x = location.getBlockX();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX && z >= END_Z && z <= START_Z;
    }

    /**
     * Obtient le nombre de joueurs dans une zone
     */
    public int getPlayersInZone(int zoneId) {
        return (int) playerZoneCache.values().stream()
            .filter(id -> id == zoneId)
            .count();
    }

    /**
     * Obtient les joueurs dans une zone
     */
    public List<Player> getPlayersInZone(Zone zone) {
        List<Player> players = new ArrayList<>();
        
        for (Map.Entry<UUID, Integer> entry : playerZoneCache.entrySet()) {
            if (entry.getValue() == zone.getId()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player);
                }
            }
        }
        
        return players;
    }

    /**
     * Obtient la prochaine zone
     */
    public Zone getNextZone(Zone currentZone) {
        return zonesById.get(currentZone.getId() + 1);
    }

    /**
     * Obtient la zone précédente
     */
    public Zone getPreviousZone(Zone currentZone) {
        if (currentZone.getId() <= 1) return spawnZone;
        return zonesById.get(currentZone.getId() - 1);
    }
    
}
