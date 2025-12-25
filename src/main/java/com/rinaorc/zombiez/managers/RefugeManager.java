package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Refuge;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des refuges (points de sauvegarde et téléportation)
 * Charge et gère les refuges depuis refuges.yml
 * Affiche des TextDisplay informatifs au-dessus des beacons
 */
public class RefugeManager {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<Integer, Refuge> refugesById = new ConcurrentHashMap<>();

    // Cache pour recherche rapide par position de beacon
    private final Map<String, Refuge> refugesByBeaconPos = new ConcurrentHashMap<>();

    // TextDisplays au-dessus des beacons (refuge ID -> TextDisplay)
    private final Map<Integer, TextDisplay> beaconDisplays = new ConcurrentHashMap<>();

    // Cache des chunks contenant des beacons de refuge (chunkKey -> Set<refugeId>)
    private final Map<Long, Set<Integer>> chunkToRefuges = new ConcurrentHashMap<>();

    // Cache des chunks qui intersectent des zones protégées (chunkKey -> Set<refugeId>)
    // Utilisé pour optimiser isInAnyRefugeProtectedArea
    private final Map<Long, Set<Integer>> protectedChunks = new ConcurrentHashMap<>();

    // Listener pour gérer le chargement des chunks
    private ChunkListener chunkListener;

    @Getter
    private FileConfiguration refugesConfig;

    // Configuration des hologrammes
    private static final float HOLOGRAM_SCALE = 2.4f; // Échelle x3 pour meilleure visibilité
    private static final double HOLOGRAM_HEIGHT_OFFSET = 2.5; // Blocs au-dessus du beacon

    public RefugeManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge tous les refuges depuis refuges.yml
     */
    public void loadRefuges() {
        refugesById.clear();
        refugesByBeaconPos.clear();
        chunkToRefuges.clear();
        protectedChunks.clear();

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

        // Enregistrer le listener de chunks si pas encore fait
        if (chunkListener == null) {
            chunkListener = new ChunkListener();
            Bukkit.getPluginManager().registerEvents(chunkListener, plugin);
        }

        // Spawn les hologrammes après un court délai (attendre que les chunks soient chargés)
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnAllBeaconDisplays, 40L); // 2 secondes
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

        // Indexer par chunk pour réactivation des hologrammes
        long beaconChunkKey = getChunkKey(refuge.getBeaconX() >> 4, refuge.getBeaconZ() >> 4);
        chunkToRefuges.computeIfAbsent(beaconChunkKey, k -> ConcurrentHashMap.newKeySet()).add(refuge.getId());

        // Indexer tous les chunks de la zone protégée pour optimisation de isInAnyRefugeProtectedArea
        int minChunkX = refuge.getProtectedMinX() >> 4;
        int maxChunkX = refuge.getProtectedMaxX() >> 4;
        int minChunkZ = refuge.getProtectedMinZ() >> 4;
        int maxChunkZ = refuge.getProtectedMaxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long chunkKey = getChunkKey(cx, cz);
                protectedChunks.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(refuge.getId());
            }
        }

        plugin.log(Level.INFO, "§7  - Refuge " + refuge.getId() + ": §e" + refuge.getName() +
            " §7(" + refuge.getProtectedAreaInfo() + ")");
    }

    /**
     * Génère une clé unique pour un chunk (x, z)
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
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
     * Optimisé avec cache de chunks pour éviter les itérations inutiles
     */
    public boolean isInAnyRefugeProtectedArea(Location loc) {
        return isInAnyRefugeProtectedArea(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Vérifie si une position est dans une zone protégée de refuge
     * Optimisé avec cache de chunks pour éviter les itérations inutiles
     */
    public boolean isInAnyRefugeProtectedArea(int x, int y, int z) {
        // Lookup rapide par chunk - O(1) au lieu de O(n)
        long chunkKey = getChunkKey(x >> 4, z >> 4);
        Set<Integer> refugeIds = protectedChunks.get(chunkKey);

        // Si le chunk ne contient aucune zone protégée, retour rapide
        if (refugeIds == null || refugeIds.isEmpty()) {
            return false;
        }

        // Vérifier uniquement les refuges qui intersectent ce chunk
        for (int refugeId : refugeIds) {
            Refuge refuge = refugesById.get(refugeId);
            if (refuge != null && refuge.isInProtectedArea(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtient le refuge contenant une location (dans sa zone protégée)
     * Optimisé avec cache de chunks
     */
    public Refuge getRefugeAt(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Lookup rapide par chunk
        long chunkKey = getChunkKey(x >> 4, z >> 4);
        Set<Integer> refugeIds = protectedChunks.get(chunkKey);

        if (refugeIds == null || refugeIds.isEmpty()) {
            return null;
        }

        // Vérifier uniquement les refuges qui intersectent ce chunk
        for (int refugeId : refugeIds) {
            Refuge refuge = refugesById.get(refugeId);
            if (refuge != null && refuge.isInProtectedArea(x, y, z)) {
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
        removeAllBeaconDisplays();
        loadRefuges();
    }

    // ==================== SYSTÈME DE TEXTDISPLAY ====================

    /**
     * Spawn tous les hologrammes au-dessus des beacons
     */
    public void spawnAllBeaconDisplays() {
        removeAllBeaconDisplays(); // Nettoyer d'abord

        World world = Bukkit.getWorlds().get(0); // Monde principal
        if (world == null) return;

        for (Refuge refuge : refugesById.values()) {
            spawnBeaconDisplay(refuge, world);
        }

        plugin.log(Level.INFO, "§a✓ " + beaconDisplays.size() + " hologrammes de refuge créés");
    }

    /**
     * Spawn un hologramme au-dessus du beacon d'un refuge
     */
    private void spawnBeaconDisplay(Refuge refuge, World world) {
        Location beaconLoc = new Location(world,
            refuge.getBeaconX() + 0.5,
            refuge.getBeaconY() + HOLOGRAM_HEIGHT_OFFSET,
            refuge.getBeaconZ() + 0.5
        );

        // Vérifier si le chunk est chargé
        if (!beaconLoc.getChunk().isLoaded()) {
            return; // On ne force pas le chargement
        }

        try {
            TextDisplay display = world.spawn(beaconLoc, TextDisplay.class, td -> {
                // Texte multi-lignes
                Component text = createRefugeHologramText(refuge);
                td.text(text);

                // Billboard - toujours face au joueur
                td.setBillboard(Display.Billboard.CENTER);

                // Style
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Fond transparent
                td.setDefaultBackground(false);
                td.setShadowed(true);
                td.setSeeThrough(false);
                td.setAlignment(TextDisplay.TextAlignment.CENTER);

                // Échelle
                td.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(HOLOGRAM_SCALE, HOLOGRAM_SCALE, HOLOGRAM_SCALE),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                // Distance de vue
                td.setViewRange(0.5f); // Visible à ~32 blocs

                // Persistance
                td.setPersistent(false); // Ne pas sauvegarder dans le monde
            });

            beaconDisplays.put(refuge.getId(), display);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§eImpossible de créer l'hologramme pour " + refuge.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Crée le texte de l'hologramme pour un refuge
     * Le texte est générique car visible par tous les joueurs
     * Design amélioré avec plusieurs lignes informatives
     */
    private Component createRefugeHologramText(Refuge refuge) {
        // Ligne 1: Icône REFUGE (vert, gras)
        Component line1 = Component.text("🏠 REFUGE")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.BOLD, true);

        // Ligne 2: Nom du refuge (jaune, gras)
        Component line2 = Component.text(refuge.getName())
            .color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.BOLD, true);

        // Bastion du Réveil (ID 0) : pas besoin d'instruction car c'est le spawn par défaut
        if (refuge.getId() == 0) {
            return line1
                .append(Component.newline())
                .append(line2);
        }

        // Ligne 3: Séparateur
        Component line3 = Component.text("─────────")
            .color(NamedTextColor.DARK_GRAY);

        // Ligne 4: Instruction (blanc)
        Component line4 = Component.text("▶ Clic droit")
            .color(NamedTextColor.WHITE);

        // Combiner avec des retours à la ligne
        return line1
            .append(Component.newline())
            .append(line2)
            .append(Component.newline())
            .append(line3)
            .append(Component.newline())
            .append(line4);
    }

    /**
     * Supprime tous les hologrammes de beacon
     */
    public void removeAllBeaconDisplays() {
        for (TextDisplay display : beaconDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        beaconDisplays.clear();
    }

    /**
     * Met à jour un hologramme spécifique (si le joueur a débloqué le refuge)
     */
    public void updateBeaconDisplay(int refugeId) {
        TextDisplay display = beaconDisplays.get(refugeId);
        Refuge refuge = refugesById.get(refugeId);

        if (display != null && display.isValid() && refuge != null) {
            display.text(createRefugeHologramText(refuge));
        }
    }

    /**
     * Appelé lors du shutdown du plugin pour nettoyer
     */
    public void shutdown() {
        removeAllBeaconDisplays();

        // Désenregistrer le listener de chunks
        if (chunkListener != null) {
            HandlerList.unregisterAll(chunkListener);
            chunkListener = null;
        }
    }

    // ==================== LISTENER DE CHUNKS ====================

    /**
     * Listener interne pour gérer le chargement des chunks
     * et respawn les hologrammes de beacon si nécessaire
     */
    private class ChunkListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onChunkLoad(ChunkLoadEvent event) {
            Chunk chunk = event.getChunk();
            long chunkKey = getChunkKey(chunk.getX(), chunk.getZ());

            // Vérifier si ce chunk contient des beacons de refuge
            Set<Integer> refugeIds = chunkToRefuges.get(chunkKey);
            if (refugeIds == null || refugeIds.isEmpty()) {
                return;
            }

            // Respawn les hologrammes manquants pour les refuges dans ce chunk
            World world = chunk.getWorld();
            for (int refugeId : refugeIds) {
                TextDisplay existingDisplay = beaconDisplays.get(refugeId);

                // Si l'hologramme n'existe pas ou n'est plus valide, le respawn
                if (existingDisplay == null || !existingDisplay.isValid()) {
                    Refuge refuge = refugesById.get(refugeId);
                    if (refuge != null) {
                        // Petit délai pour laisser le chunk se charger complètement
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            spawnBeaconDisplay(refuge, world);
                        }, 5L);
                    }
                }
            }
        }
    }
}
