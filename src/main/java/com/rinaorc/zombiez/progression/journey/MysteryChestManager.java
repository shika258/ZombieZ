package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les coffres mystères du parcours
 *
 * Responsabilités:
 * 1. Créer et maintenir les hologrammes au-dessus des coffres
 * 2. Détecter quand les joueurs approchent des coffres
 * 3. Tracker quels coffres ont été découverts par chaque joueur
 * 4. Notifier la progression du parcours lors de la découverte
 */
public class MysteryChestManager {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // TextDisplays par coffre (MysteryChest -> Map<PlayerUUID, TextDisplay>)
    // Chaque joueur voit un TextDisplay personnalisé selon son état de découverte
    private final Map<MysteryChest, Map<UUID, TextDisplay>> chestDisplays = new ConcurrentHashMap<>();

    // Cache des coffres découverts par joueur (stocké aussi en BDD via PlayerData)
    private final Map<UUID, Set<String>> discoveredChests = new ConcurrentHashMap<>();

    // Tâche de mise à jour périodique
    private BukkitTask updateTask;
    private BukkitTask detectionTask;

    // Configuration
    private static final double HOLOGRAM_HEIGHT = 1.5; // Hauteur au-dessus du coffre
    private static final int UPDATE_INTERVAL = 20; // Ticks entre mises à jour (1 seconde)
    private static final int DETECTION_INTERVAL = 5; // Ticks entre détections (0.25 seconde)
    private static final double VIEW_DISTANCE = 32.0; // Distance max pour voir l'hologramme

    public MysteryChestManager(ZombieZPlugin plugin, JourneyManager journeyManager) {
        this.plugin = plugin;
        this.journeyManager = journeyManager;

        // Initialiser les maps pour chaque coffre
        for (MysteryChest chest : MysteryChest.values()) {
            chestDisplays.put(chest, new ConcurrentHashMap<>());
        }
    }

    /**
     * Démarre le système de coffres mystères
     */
    public void start() {
        // Tâche de mise à jour des hologrammes
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllHolograms();
            }
        }.runTaskTimer(plugin, 40L, UPDATE_INTERVAL);

        // Tâche de détection de proximité
        detectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayerProximity();
            }
        }.runTaskTimer(plugin, 60L, DETECTION_INTERVAL);

        plugin.getLogger().info("§a✓ MysteryChestManager démarré");
    }

    /**
     * Arrête le système et nettoie les entités
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (detectionTask != null) {
            detectionTask.cancel();
        }

        // Supprimer tous les TextDisplays
        for (Map<UUID, TextDisplay> displays : chestDisplays.values()) {
            for (TextDisplay display : displays.values()) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
            displays.clear();
        }

        discoveredChests.clear();
        plugin.getLogger().info("§7MysteryChestManager arrêté");
    }

    /**
     * Charge les coffres découverts par un joueur depuis ses données
     */
    public void loadPlayerData(Player player, Set<String> discoveredChestIds) {
        if (discoveredChestIds != null) {
            discoveredChests.put(player.getUniqueId(), new HashSet<>(discoveredChestIds));
        } else {
            discoveredChests.put(player.getUniqueId(), new HashSet<>());
        }
    }

    /**
     * Décharge les données d'un joueur (lors de la déconnexion)
     */
    public void unloadPlayer(UUID playerId) {
        discoveredChests.remove(playerId);

        // Supprimer les TextDisplays de ce joueur
        for (Map<UUID, TextDisplay> displays : chestDisplays.values()) {
            TextDisplay display = displays.remove(playerId);
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
    }

    /**
     * Vérifie si un joueur a découvert un coffre
     */
    public boolean hasDiscovered(UUID playerId, MysteryChest chest) {
        Set<String> discovered = discoveredChests.get(playerId);
        return discovered != null && discovered.contains(chest.getId());
    }

    /**
     * Marque un coffre comme découvert par un joueur
     */
    public void markDiscovered(Player player, MysteryChest chest) {
        UUID playerId = player.getUniqueId();
        Set<String> discovered = discoveredChests.computeIfAbsent(playerId, k -> new HashSet<>());

        if (discovered.add(chest.getId())) {
            // Nouveau coffre découvert!
            onChestDiscovered(player, chest);
        }
    }

    /**
     * Appelé quand un joueur découvre un nouveau coffre
     */
    private void onChestDiscovered(Player player, MysteryChest chest) {
        // Effets visuels et sonores
        playDiscoveryEffects(player, chest);

        // Sauvegarder dans PlayerData
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData != null) {
            playerData.addDiscoveredChest(chest.getId());
        }

        // Notifier le JourneyManager pour la progression
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep != null && currentStep.getType() == JourneyStep.StepType.DISCOVER_CHEST) {
            // Compter le nombre de coffres découverts pour ce chapitre
            int discovered = countDiscoveredForChapter(player.getUniqueId(), chest.getChapter());
            journeyManager.updateProgress(player, JourneyStep.StepType.DISCOVER_CHEST, discovered);
        }

        // Mettre à jour l'hologramme immédiatement
        updateHologramForPlayer(player, chest);
    }

    /**
     * Compte le nombre de coffres découverts par un joueur pour un chapitre
     */
    private int countDiscoveredForChapter(UUID playerId, JourneyChapter chapter) {
        Set<String> discovered = discoveredChests.get(playerId);
        if (discovered == null) return 0;

        int count = 0;
        for (MysteryChest chest : MysteryChest.getChestsForChapter(chapter)) {
            if (discovered.contains(chest.getId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Joue les effets de découverte d'un coffre
     */
    private void playDiscoveryEffects(Player player, MysteryChest chest) {
        World world = player.getWorld();
        Location chestLoc = chest.getLocation(world);

        // Son de découverte (utilise un son moderne 1.21.4)
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        player.playSound(chestLoc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

        // Particules
        world.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, chestLoc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(org.bukkit.Particle.END_ROD, chestLoc.clone().add(0, 1.5, 0), 20, 0.3, 0.3, 0.3, 0.05);

        // Message
        player.sendMessage("");
        player.sendMessage("§6§l✦ COFFRE MYSTÉRIEUX DÉCOUVERT! ✦");
        player.sendMessage("§e" + chest.getDisplayName());
        player.sendMessage("§7Tu as trouvé un coffre secret du journal!");
        player.sendMessage("");

        // Title
        player.sendTitle(
            "§6✦ DÉCOUVERT! ✦",
            "§e" + chest.getDisplayName(),
            10, 40, 10
        );
    }

    /**
     * Vérifie la proximité des joueurs avec les coffres
     */
    private void checkPlayerProximity() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Vérifier si le joueur est sur une étape DISCOVER_CHEST
            JourneyStep currentStep = journeyManager.getCurrentStep(player);
            if (currentStep == null || currentStep.getType() != JourneyStep.StepType.DISCOVER_CHEST) {
                continue;
            }

            Location playerLoc = player.getLocation();

            for (MysteryChest chest : MysteryChest.values()) {
                // Skip si déjà découvert
                if (hasDiscovered(player.getUniqueId(), chest)) {
                    continue;
                }

                // Vérifier si le joueur est à portée
                if (chest.isPlayerInRange(playerLoc)) {
                    markDiscovered(player, chest);
                }
            }
        }
    }

    /**
     * Met à jour tous les hologrammes pour tous les joueurs
     */
    private void updateAllHolograms() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();

            for (MysteryChest chest : MysteryChest.values()) {
                Location chestLoc = chest.getLocation(playerLoc.getWorld());

                // Vérifier la distance de vue
                if (playerLoc.distanceSquared(chestLoc) > VIEW_DISTANCE * VIEW_DISTANCE) {
                    // Trop loin - supprimer l'hologramme si présent
                    removeHologramForPlayer(player, chest);
                } else {
                    // À portée - créer/mettre à jour l'hologramme
                    updateHologramForPlayer(player, chest);
                }
            }
        }
    }

    /**
     * Met à jour l'hologramme d'un coffre pour un joueur spécifique
     */
    private void updateHologramForPlayer(Player player, MysteryChest chest) {
        UUID playerId = player.getUniqueId();
        Map<UUID, TextDisplay> displays = chestDisplays.get(chest);

        TextDisplay display = displays.get(playerId);
        Location hologramLoc = chest.getLocation(player.getWorld()).add(0, HOLOGRAM_HEIGHT, 0);

        boolean isDiscovered = hasDiscovered(playerId, chest);
        String text = isDiscovered
            ? "§a✓ §7Coffre Découvert"
            : "§e§l✦ §6Coffre Mystérieux §e§l✦\n§7Approche-toi pour découvrir!";

        if (display == null || !display.isValid()) {
            // Créer un nouveau TextDisplay
            display = createHologram(hologramLoc, text, player);
            displays.put(playerId, display);
        } else {
            // Mettre à jour le texte si nécessaire
            String currentText = display.getText();
            if (currentText == null || !currentText.equals(text)) {
                display.setText(text);
            }
        }
    }

    /**
     * Crée un hologramme TextDisplay
     */
    private TextDisplay createHologram(Location location, String text, Player viewer) {
        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(true);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(128, 0, 0, 0));
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);

            // Visible uniquement par ce joueur (par packet plus tard si besoin)
            // Pour l'instant on utilise un système simplifié
            entity.setVisibleByDefault(true);

            // Tag pour identification
            entity.addScoreboardTag("mystery_chest_hologram");
            entity.addScoreboardTag("zombiez_display");

            // Persistance désactivée
            entity.setPersistent(false);
        });

        return display;
    }

    /**
     * Supprime l'hologramme d'un coffre pour un joueur
     */
    private void removeHologramForPlayer(Player player, MysteryChest chest) {
        Map<UUID, TextDisplay> displays = chestDisplays.get(chest);
        TextDisplay display = displays.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Obtient le nombre de coffres découverts par un joueur
     */
    public int getDiscoveredCount(UUID playerId) {
        Set<String> discovered = discoveredChests.get(playerId);
        return discovered != null ? discovered.size() : 0;
    }

    /**
     * Obtient les IDs des coffres découverts par un joueur
     */
    public Set<String> getDiscoveredChests(UUID playerId) {
        return discoveredChests.getOrDefault(playerId, Set.of());
    }
}
