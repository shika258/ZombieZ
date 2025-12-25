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
 * Gestionnaire des zones de jeu - Version 50 zones
 * Optimisé pour vérification rapide de 200+ joueurs
 *
 * Utilise un TreeMap pour une recherche O(log n) de la zone par coordonnée Z
 *
 * IMPORTANT: La progression se fait du SUD vers le NORD
 * - Le spawn est à Z = 10200 (sud de la map, zone sécurisée)
 * - Zone 1 commence à Z = 10000
 * - Les joueurs progressent vers Z = 0 (en allant vers le NORD)
 * - Chaque zone = 200 blocs
 * - 50 zones au total, niveaux 1-100
 * - Zone PVP: Zone 26 (Z = 5000 à 4800)
 */
public class ZoneManager {

    private final ZombieZPlugin plugin;

    // Constantes de progression sur l'axe Z (50 zones)
    public static final int START_Z = 10200; // Zone de départ (spawn au sud, zone sécurisée)
    public static final int ZONE_START_Z = 10000; // Début de la zone 1
    public static final int END_Z = 0;       // Zone finale (L'Origine au nord)
    public static final int ZONE_SIZE = 200; // Taille de chaque zone en blocs
    public static final int TOTAL_ZONES = 50; // Nombre total de zones

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

    // Point de spawn personnalisé
    @Getter
    private Location spawnLocation;

    public ZoneManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Définit le point de spawn
     */
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    /**
     * Obtient le point de spawn (depuis config ou valeur par défaut)
     */
    public Location getSpawnPoint() {
        if (spawnLocation != null) {
            return spawnLocation;
        }

        // Charger depuis la config si pas défini
        var config = plugin.getConfig();
        String worldName = config.getString("gameplay.spawn.world", gameWorld);
        var world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);

        double x = config.getDouble("gameplay.spawn.x", 621);
        double y = config.getDouble("gameplay.spawn.y", 70);
        double z = config.getDouble("gameplay.spawn.z", 10300);
        float yaw = (float) config.getDouble("gameplay.spawn.yaw", 0);
        float pitch = (float) config.getDouble("gameplay.spawn.pitch", 0);

        spawnLocation = new Location(world, x, y, z, yaw, pitch);
        return spawnLocation;
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
            .minX(minX)  // Utiliser les limites X globales pour l'exploration par chunks
            .maxX(maxX)  // Utiliser les limites X globales pour l'exploration par chunks
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
     * 50 zones ordonnées du SUD (spawn, Z élevé) vers le NORD (L'Origine, Z=0)
     * Chaque zone = 200 blocs, niveaux 1-100
     */
    private void createDefaultZones() {
        // Zone Spawn (au sud, Z élevé - zone sécurisée)
        spawnZone = Zone.createSpawnZone();
        registerZone(spawnZone);

        // Données des 50 zones avec leurs noms et configurations
        String[][] zoneData = {
            // ACTE I - LES DERNIERS JOURS (Zones 1-10)
            {"1", "Bastion du Reveil", "PLAINS", "medieval_castle", "NONE", "0", "0", "1", "WALKER,SHAMBLER"},
            {"2", "Faubourgs Oublies", "PLAINS", "abandoned_suburbs", "NONE", "0", "0", "1", "WALKER,SHAMBLER,CRAWLER"},
            {"3", "Champs du Silence", "PLAINS", "silent_fields", "NONE", "0", "0", "1", "WALKER,CRAWLER,RUNNER"},
            {"4", "Verger des Pendus", "FOREST", "hanged_orchard", "NONE", "0", "0", "1", "WALKER,CRAWLER,RUNNER"},
            {"5", "Route des Fuyards", "PLAINS", "refugee_road", "NONE", "0", "0", "2", "WALKER,RUNNER,SHAMBLER"},
            {"6", "Hameau Brise", "PLAINS", "broken_hamlet", "NONE", "0", "0", "2", "WALKER,RUNNER,CRAWLER,ARMORED"},
            {"7", "Bois des Soupirs", "FOREST", "sighing_woods", "NONE", "0", "0", "2", "WALKER,RUNNER,LURKER,SHADOW"},
            {"8", "Ruines de Clairval", "PLAINS", "clairval_ruins", "NONE", "0", "0", "2", "WALKER,RUNNER,ARMORED,SCREAMER"},
            {"9", "Pont des Disparus", "RIVER", "bridge_of_lost", "NONE", "0", "0", "2", "WALKER,RUNNER,DROWNER,SCREAMER"},
            {"10", "Avant-Poste Deserte", "PLAINS", "deserted_outpost", "NONE", "0", "0", "3", "WALKER,RUNNER,ARMORED,ARMORED_ELITE"},
            // ACTE II - LA CONTAMINATION (Zones 11-20)
            {"11", "Foret Putrefiee", "DARK_FOREST", "putrid_forest", "TOXIC", "0.25", "600", "3", "RUNNER,LURKER,SHADOW,TOXIC"},
            {"12", "Clairiere des Hurlements", "FOREST", "screaming_clearing", "NONE", "0", "0", "3", "RUNNER,SCREAMER,LURKER,BERSERKER"},
            {"13", "Marais Infect", "SWAMP", "infected_marsh", "TOXIC", "0.5", "400", "3", "BLOATER,SPITTER,DROWNER,TOXIC"},
            {"14", "Jardins Devoyes", "JUNGLE", "corrupted_gardens", "TOXIC", "0.5", "500", "3", "CRAWLER,SPITTER,TOXIC,LURKER"},
            {"15", "Village Moisi", "SWAMP", "moldy_village", "TOXIC", "0.75", "400", "4", "BLOATER,SPITTER,TOXIC,NECROMANCER"},
            {"16", "Ronces Noires", "DARK_FOREST", "black_thorns", "NONE", "0", "0", "4", "LURKER,SHADOW,BERSERKER,CRAWLER"},
            {"17", "Territoire des Errants", "PLAINS", "wanderer_territory", "NONE", "0", "0", "4", "WALKER,RUNNER,ARMORED,GIANT"},
            {"18", "Campement Calcine", "SAVANNA", "burned_camp", "HEAT", "0.5", "500", "4", "EXPLOSIVE,BERSERKER,RUNNER,ARMORED"},
            {"19", "Bois Rouge", "DARK_FOREST", "red_woods", "NONE", "0", "0", "4", "SHADOW,BERSERKER,NECROMANCER,SPECTRE"},
            {"20", "Lisiere de la Peur", "DARK_FOREST", "fear_edge", "NONE", "0", "0", "5", "SHADOW,SPECTRE,BERSERKER,GIANT"},
            // ACTE III - LE CHAOS (Zones 21-30)
            {"21", "Faille Incandescente", "NETHER_WASTES", "burning_rift", "FIRE", "1.0", "400", "5", "EXPLOSIVE,DEMON,INFERNAL,BERSERKER"},
            {"22", "Crateres de Cendre", "BADLANDS", "ash_craters", "HEAT", "1.0", "400", "5", "EXPLOSIVE,COLOSSUS,RAVAGER,DEMON"},
            {"23", "Plaines Brulees", "BADLANDS", "burned_plains", "HEAT", "1.0", "350", "5", "BERSERKER,RAVAGER,GIANT,INFERNAL"},
            {"24", "Fournaise Antique", "NETHER_WASTES", "ancient_furnace", "FIRE", "1.5", "350", "5", "DEMON,INFERNAL,COLOSSUS,EXPLOSIVE"},
            {"25", "Terres de Soufre", "BADLANDS", "sulfur_lands", "TOXIC", "1.5", "300", "6", "TOXIC,BLOATER,SPITTER,MUTANT"},
            {"26", "L'Arene des Damnes", "PLAINS", "pvp_arena", "NONE", "0", "0", "6", "WALKER,RUNNER,BERSERKER"}, // PVP ZONE
            {"27", "Riviere de Lave", "NETHER_WASTES", "lava_river", "FIRE", "2.0", "300", "6", "DEMON,INFERNAL,EXPLOSIVE,COLOSSUS"},
            {"28", "Canyon des Damnes", "BADLANDS", "damned_canyon", "NONE", "0", "0", "6", "CLIMBER,SPECTRE,RAVAGER,BERSERKER"},
            {"29", "Forteresse Effondree", "PLAINS", "collapsed_fortress", "NONE", "0", "0", "6", "ARMORED,ARMORED_ELITE,GIANT,COLOSSUS"},
            {"30", "No Mans Land", "BADLANDS", "no_mans_land", "RADIATION", "1.0", "400", "7", "MUTANT,RAVAGER,COLOSSUS,SPECTRE"},
            // ACTE IV - L'EXTINCTION (Zones 31-40)
            {"31", "Toundra Morte", "FROZEN_PEAKS", "dead_tundra", "COLD", "1.5", "350", "7", "FROZEN,YETI,WENDIGO,COLOSSUS"},
            {"32", "Neiges Hurlantes", "SNOWY_PLAINS", "howling_snow", "COLD", "1.5", "300", "7", "FROZEN,YETI,WENDIGO,SCREAMER"},
            {"33", "Plaines Gelees", "ICE_SPIKES", "frozen_plains", "COLD", "2.0", "300", "7", "FROZEN,YETI,COLOSSUS,RAVAGER"},
            {"34", "Lac de Verre", "FROZEN_OCEAN", "glass_lake", "COLD", "2.0", "250", "7", "FROZEN,DROWNER,YETI,SPECTRE"},
            {"35", "Ruines Englouties", "ICE_SPIKES", "drowned_ruins", "COLD", "2.0", "250", "8", "FROZEN,SPECTRE,YETI,WENDIGO"},
            {"36", "Pics du Desespoir", "FROZEN_PEAKS", "despair_peaks", "COLD", "2.5", "250", "8", "CLIMBER,YETI,WENDIGO,COLOSSUS"},
            {"37", "Blizzard Eternel", "SNOWY_PLAINS", "eternal_blizzard", "COLD", "2.5", "200", "8", "FROZEN,YETI,WENDIGO,SPECTRE"},
            {"38", "Tombe Blanche", "SNOWY_TAIGA", "white_tomb", "COLD", "2.5", "200", "8", "NECROMANCER,SPECTRE,YETI,WENDIGO"},
            {"39", "Sanctuaire Abandonne", "PALE_GARDEN", "abandoned_sanctuary", "DARKNESS", "1.0", "300", "8", "SPECTRE,NECROMANCER,CREAKING,CORRUPTED_WARDEN"},
            {"40", "Seuil de l'Oblivion", "DEEP_DARK", "oblivion_threshold", "DARKNESS", "1.5", "250", "9", "SPECTRE,CREAKING,CORRUPTED_WARDEN,ARCHON"},
            // ACTE V - L'ORIGINE DU MAL (Zones 41-50)
            {"41", "Terres Corrompues", "PALE_GARDEN", "corrupted_lands", "RADIATION", "2.0", "200", "9", "MUTANT,CREAKING,CORRUPTED_WARDEN,ARCHON"},
            {"42", "Foret Noire", "DARK_FOREST", "black_forest", "DARKNESS", "2.0", "200", "9", "SHADOW,SPECTRE,LURKER,CORRUPTED_WARDEN"},
            {"43", "Racines du Mal", "PALE_GARDEN", "roots_of_evil", "TOXIC", "2.5", "200", "9", "CREAKING,MUTANT,NECROMANCER,ARCHON"},
            {"44", "Marecages Carmine", "SWAMP", "carmine_swamps", "TOXIC", "2.5", "175", "9", "BLOATER,SPITTER,MUTANT,CORRUPTED_WARDEN"},
            {"45", "Veines du Monde", "DEEP_DARK", "world_veins", "RADIATION", "3.0", "175", "10", "MUTANT,CREAKING,CORRUPTED_WARDEN,ARCHON"},
            {"46", "Citadelle Profanee", "DEEP_DARK", "defiled_citadel", "DARKNESS", "2.5", "150", "10", "CORRUPTED_WARDEN,ARCHON,SPECTRE,NECROMANCER"},
            {"47", "Coeur Putride", "DEEP_DARK", "putrid_heart", "TOXIC", "3.0", "150", "10", "BLOATER,MUTANT,CORRUPTED_WARDEN,ARCHON"},
            {"48", "Trone des Infectes", "DEEP_DARK", "infected_throne", "RADIATION", "3.5", "125", "10", "ARCHON,CORRUPTED_WARDEN,NECROMANCER,COLOSSUS"},
            {"49", "Dernier Rempart", "DEEP_DARK", "last_bastion", "DARKNESS", "3.0", "125", "10", "ARCHON,CORRUPTED_WARDEN,COLOSSUS,RAVAGER"},
            {"50", "L'Origine", "DEEP_DARK", "the_origin", "DARKNESS", "2.0", "100", "10", "ARCHON,CORRUPTED_WARDEN,PATIENT_ZERO"}
        };

        // Couleurs par acte
        String[] colors = {
            "§a", "§a", "§a", "§a", "§a", "§a", "§2", "§e", "§e", "§e",  // Acte I (1-10)
            "§2", "§2", "§2", "§2", "§2", "§2", "§e", "§e", "§c", "§c",  // Acte II (11-20)
            "§c", "§c", "§c", "§4", "§e", "§c", "§4", "§4", "§7", "§5",  // Acte III (21-30)
            "§b", "§b", "§b", "§b", "§3", "§3", "§3", "§3", "§5", "§5",  // Acte IV (31-40)
            "§5", "§0", "§0", "§4", "§4", "§8", "§8", "§8", "§d", "§d"   // Acte V (41-50)
        };

        // Créer toutes les zones
        for (int i = 0; i < zoneData.length; i++) {
            String[] data = zoneData[i];
            int zoneId = Integer.parseInt(data[0]);
            String name = data[1];
            String biome = data[2];
            String theme = data[3];
            String envEffect = data[4];
            double envDamage = Double.parseDouble(data[5]);
            int envInterval = Integer.parseInt(data[6]);
            int refugeId = Integer.parseInt(data[7]);
            String[] zombieTypes = data[8].split(",");

            // Calculer les coordonnées Z (chaque zone = 200 blocs)
            int maxZ = ZONE_START_Z - ((zoneId - 1) * ZONE_SIZE);
            int minZ = maxZ - ZONE_SIZE;

            // Calculer les niveaux de zombies (niveau 1-100 sur 50 zones)
            int minLevel = (zoneId - 1) * 2 + 1;
            int maxLevel = zoneId * 2;

            // Calculer la difficulté (1-10)
            int difficulty = Math.min(10, (zoneId / 5) + 1);

            // Zone PVP spéciale
            boolean isPvp = zoneId == 26;

            Zone.ZoneBuilder builder = Zone.standardZone(zoneId, name, minZ, maxZ, difficulty)
                .description(getZoneDescription(zoneId))
                .biomeType(biome)
                .theme(theme)
                .environmentalEffect(envEffect)
                .environmentalDamage(envDamage)
                .environmentalInterval(envInterval)
                .refugeId(isPvp ? -1 : refugeId)
                .minZombieLevel(minLevel)
                .maxZombieLevel(maxLevel)
                .allowedZombieTypes(zombieTypes)
                .pvpEnabled(isPvp)
                .bossZone(zoneId == 50)
                .color(colors[i]);

            registerZone(builder.build());
        }

        plugin.log(Level.INFO, "§a✓ " + TOTAL_ZONES + " zones par défaut créées (niveaux 1-100)");
    }

    /**
     * Retourne la description d'une zone par son ID
     */
    private String getZoneDescription(int zoneId) {
        return switch (zoneId) {
            case 1 -> "Le dernier refuge de l'humanite";
            case 2 -> "Les quartiers periferiques abandonnes";
            case 3 -> "Des terres agricoles desormais silencieuses";
            case 4 -> "Un verger macabre aux fruits sinistres";
            case 5 -> "La route de l'exode";
            case 6 -> "Un petit village ravage";
            case 7 -> "Une foret aux gemissements des morts";
            case 8 -> "Les vestiges d'une ville prospere";
            case 9 -> "Un pont ou tant ont trouve leur fin";
            case 10 -> "Un camp militaire abandonne";
            case 11 -> "Les arbres eux-memes semblent malades";
            case 12 -> "Les cris resonnent sans cesse";
            case 13 -> "Des eaux stagnantes mortelles";
            case 14 -> "Un jardin botanique mute";
            case 15 -> "Un village envahi par la moisissure";
            case 16 -> "Des ronces geantes bloquent le passage";
            case 17 -> "Le domaine des errants eternels";
            case 18 -> "Un camp brule jusqu'aux cendres";
            case 19 -> "Une foret aux feuilles rouge sang";
            case 20 -> "La frontiere vers le chaos";
            case 21 -> "Une fissure crachant des flammes";
            case 22 -> "Un paysage lunaire de crateres";
            case 23 -> "Des plaines carbonisees a perte de vue";
            case 24 -> "Les ruines d'une forge titanesque";
            case 25 -> "L'air irrespirable, le sol toxique";
            case 26 -> "Zone PvP - Seuls les plus forts survivent";
            case 27 -> "Un fleuve de roche en fusion";
            case 28 -> "Un canyon aux ames perdues";
            case 29 -> "Les ruines d'une forteresse militaire";
            case 30 -> "Une terre ou rien ne survit";
            case 31 -> "Une toundra ou meme la glace semble morte";
            case 32 -> "Le vent hurle sans cesse";
            case 33 -> "Des plaines de glace eternelle";
            case 34 -> "Un lac gele comme du verre";
            case 35 -> "Les vestiges d'une cite sous la glace";
            case 36 -> "Des montagnes ou l'espoir meurt";
            case 37 -> "Une tempete de neige eternelle";
            case 38 -> "Un cimetiere sous la neige";
            case 39 -> "Un temple sacre desormais profane";
            case 40 -> "La frontiere vers l'oubli";
            case 41 -> "La corruption dans chaque parcelle";
            case 42 -> "Une foret ou la lumiere n'ose pas";
            case 43 -> "Les racines de l'infection originelle";
            case 44 -> "Des marecages rouge sang";
            case 45 -> "Des tunnels organiques pulsants";
            case 46 -> "Une citadelle souilee par le mal";
            case 47 -> "Le coeur battant de l'infection";
            case 48 -> "Le siege du pouvoir des morts";
            case 49 -> "La derniere barriere";
            case 50 -> "L'Origine du fleau - Affrontez Patient Zero";
            default -> "Zone inconnue";
        };
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
