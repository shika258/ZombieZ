package com.rinaorc.zombiez.progression.journey.chapter5;

import com.rinaorc.zombiez.ZombieZPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Mini-jeu de capture de grenouille dans un GUI
 *
 * Mécanique:
 * - Grille 5x5 où la grenouille saute aléatoirement
 * - Le joueur doit cliquer sur la grenouille X fois avant qu'elle s'échappe
 * - Difficulté variable par grenouille (vitesse, nombre de clics, nombre de sauts)
 * - Si la grenouille s'échappe, le joueur perd cette tentative
 */
public class FrogCaptureGUI implements Listener {

    private final ZombieZPlugin plugin;

    // Tracking des joueurs avec un mini-jeu actif
    private final Map<UUID, FrogGame> activeGames = new ConcurrentHashMap<>();

    // Configuration du GUI
    private static final int GUI_SIZE = 45; // 5 rangées
    private static final int GRID_START_ROW = 1; // Commence à la rangée 2 (index 1)
    private static final int GRID_SIZE = 5; // 5x5 grid

    // Items
    private static final Material FROG_MATERIAL = Material.FROG_SPAWN_EGG;
    private static final Material EMPTY_MATERIAL = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final Material BORDER_MATERIAL = Material.CYAN_STAINED_GLASS_PANE;
    private static final Material SUCCESS_MATERIAL = Material.LIME_STAINED_GLASS_PANE;
    private static final Material FAIL_MATERIAL = Material.RED_STAINED_GLASS_PANE;

    public FrogCaptureGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le mini-jeu pour un joueur
     *
     * @param player Le joueur
     * @param frogIndex L'index de la grenouille (pour générer une difficulté unique)
     * @param onSuccess Callback appelé si la capture réussit
     * @param onFailure Callback appelé si la grenouille s'échappe
     */
    public void openGame(Player player, int frogIndex, Consumer<Player> onSuccess, Consumer<Player> onFailure) {
        UUID playerId = player.getUniqueId();

        // Fermer un jeu existant s'il y en a un
        FrogGame existingGame = activeGames.remove(playerId);
        if (existingGame != null) {
            existingGame.cancel();
        }

        // Générer les paramètres aléatoires basés sur l'index de la grenouille
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Plus l'index est élevé, plus c'est difficile (légèrement)
        int baseClicksRequired = 2 + (frogIndex / 3); // 2-3 clics selon la grenouille
        int clicksRequired = Math.min(4, baseClicksRequired + random.nextInt(0, 2)); // 2-4 clics

        int baseJumps = 6 - (frogIndex / 2); // 6-4 sauts max selon la grenouille
        int maxJumps = Math.max(4, baseJumps + random.nextInt(-1, 2)); // 4-7 sauts avant escape

        int baseInterval = 1200 - (frogIndex * 100); // Intervalle de base
        int jumpInterval = Math.max(600, baseInterval + random.nextInt(-200, 200)); // 600-1400ms

        // Créer le GUI
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
            Component.text("🐸 Attrapez la grenouille! ", NamedTextColor.GREEN)
                .append(Component.text("[" + clicksRequired + " clics]", NamedTextColor.YELLOW)));

        // Remplir le GUI
        fillGUI(gui);

        // Créer le jeu
        FrogGame game = new FrogGame(player, gui, clicksRequired, maxJumps, jumpInterval, onSuccess, onFailure);
        activeGames.put(playerId, game);

        // Ouvrir le GUI
        player.openInventory(gui);

        // Démarrer le jeu
        game.start();

        // Son d'introduction
        player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 1f, 1.2f);
    }

    /**
     * Remplit le GUI avec les éléments de base
     */
    private void fillGUI(Inventory gui) {
        // Bordure supérieure (rangée 0)
        ItemStack border = createItem(BORDER_MATERIAL, " ", null);
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
        }

        // Zone de jeu (rangées 1-4, colonnes 2-6)
        ItemStack empty = createItem(EMPTY_MATERIAL, "§7·", null);
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int slot = getGridSlot(row, col);
                gui.setItem(slot, empty);
            }

            // Bordures gauche et droite
            int rowStart = (row + GRID_START_ROW) * 9;
            gui.setItem(rowStart, border); // Colonne 0
            gui.setItem(rowStart + 1, border); // Colonne 1
            gui.setItem(rowStart + 7, border); // Colonne 7
            gui.setItem(rowStart + 8, border); // Colonne 8
        }
    }

    /**
     * Calcule le slot dans l'inventaire pour une position dans la grille 5x5
     */
    private int getGridSlot(int row, int col) {
        // La grille 5x5 occupe les colonnes 2-6 (indices 2 à 6) des rangées 1-5 (indices 9 à 44)
        return (row + GRID_START_ROW) * 9 + (col + 2);
    }

    /**
     * Obtient la position dans la grille depuis un slot d'inventaire
     * @return [row, col] ou null si pas dans la grille
     */
    private int[] getGridPosition(int slot) {
        int row = slot / 9 - GRID_START_ROW;
        int col = slot % 9 - 2;

        if (row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE) {
            return new int[]{row, col};
        }
        return null;
    }

    /**
     * Crée un ItemStack avec un nom et une lore
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            if (lore != null) {
                meta.lore(lore.stream()
                    .map(line -> Component.text(line).decoration(TextDecoration.ITALIC, false))
                    .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        FrogGame game = activeGames.get(player.getUniqueId());
        if (game == null || !event.getInventory().equals(game.gui)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) {
            return;
        }

        // Vérifier si le clic est sur la grenouille
        if (slot == game.currentFrogSlot) {
            game.onFrogClicked();
        } else {
            // Clic raté - petit son
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        FrogGame game = activeGames.remove(player.getUniqueId());
        if (game != null && !game.isEnded) {
            game.cancel();
            // Si le joueur ferme le GUI avant la fin, c'est un échec
            if (game.onFailure != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        game.onFailure.accept(player);
                    }
                }.runTaskLater(plugin, 5L);
            }
        }
    }

    /**
     * Classe interne représentant une partie en cours
     */
    private class FrogGame {
        final Player player;
        final Inventory gui;
        final int clicksRequired;
        final int maxJumps;
        final int jumpInterval;
        final Consumer<Player> onSuccess;
        final Consumer<Player> onFailure;

        int currentClicks = 0;
        int currentJumps = 0;
        int currentFrogSlot = -1;
        BukkitTask jumpTask;
        boolean isEnded = false;

        FrogGame(Player player, Inventory gui, int clicksRequired, int maxJumps, int jumpInterval,
                 Consumer<Player> onSuccess, Consumer<Player> onFailure) {
            this.player = player;
            this.gui = gui;
            this.clicksRequired = clicksRequired;
            this.maxJumps = maxJumps;
            this.jumpInterval = jumpInterval;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        void start() {
            // Placer la grenouille à une position aléatoire
            moveFrog();

            // Démarrer les sauts
            jumpTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (isEnded || !player.isOnline()) {
                        cancel();
                        return;
                    }

                    currentJumps++;

                    if (currentJumps >= maxJumps) {
                        // La grenouille s'échappe!
                        endGame(false);
                    } else {
                        moveFrog();
                    }
                }
            }.runTaskTimer(plugin, jumpInterval / 50, jumpInterval / 50); // Conversion ms -> ticks
        }

        void moveFrog() {
            // Retirer la grenouille de l'ancienne position
            if (currentFrogSlot >= 0) {
                gui.setItem(currentFrogSlot, createItem(EMPTY_MATERIAL, "§7·", null));
            }

            // Choisir une nouvelle position aléatoire
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int newRow = random.nextInt(GRID_SIZE);
            int newCol = random.nextInt(GRID_SIZE);
            currentFrogSlot = getGridSlot(newRow, newCol);

            // Créer l'item grenouille
            int remaining = clicksRequired - currentClicks;
            ItemStack frog = createItem(FROG_MATERIAL,
                "§a§l🐸 GRENOUILLE MUTANTE",
                List.of(
                    "§8───────────────",
                    "§7Clics restants: §e" + remaining,
                    "§7Sauts restants: §c" + (maxJumps - currentJumps),
                    "",
                    "§a▶ Cliquez pour capturer!"
                ));

            gui.setItem(currentFrogSlot, frog);

            // Son de saut
            player.playSound(player.getLocation(), Sound.ENTITY_FROG_STEP, 0.8f, 1.0f + random.nextFloat() * 0.4f);
        }

        void onFrogClicked() {
            currentClicks++;
            int remaining = clicksRequired - currentClicks;

            if (remaining <= 0) {
                // Grenouille capturée!
                endGame(true);
            } else {
                // Bon clic! Mise à jour visuelle
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);

                // Flash vert sur la position actuelle
                gui.setItem(currentFrogSlot, createItem(SUCCESS_MATERIAL, "§a§l✓", null));

                // Forcer un saut immédiat
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!isEnded && player.isOnline()) {
                            moveFrog();
                        }
                    }
                }.runTaskLater(plugin, 3L);
            }
        }

        void endGame(boolean success) {
            if (isEnded) return;
            isEnded = true;

            if (jumpTask != null) {
                jumpTask.cancel();
            }

            // Animation de fin
            if (success) {
                // Animation de succès
                for (int slot = 0; slot < GUI_SIZE; slot++) {
                    int[] pos = getGridPosition(slot);
                    if (pos != null) {
                        gui.setItem(slot, createItem(SUCCESS_MATERIAL, "§a§l✓ CAPTURÉ!", null));
                    }
                }

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 1f, 0.8f);
            } else {
                // Animation d'échec
                for (int slot = 0; slot < GUI_SIZE; slot++) {
                    int[] pos = getGridPosition(slot);
                    if (pos != null) {
                        gui.setItem(slot, createItem(FAIL_MATERIAL, "§c§l✗ ÉCHAPPÉ!", null));
                    }
                }

                player.playSound(player.getLocation(), Sound.ENTITY_FROG_LONG_JUMP, 1f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }

            // Fermer le GUI et appeler le callback
            final boolean wasSuccess = success;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.closeInventory();
                        activeGames.remove(player.getUniqueId());

                        if (wasSuccess && onSuccess != null) {
                            onSuccess.accept(player);
                        } else if (!wasSuccess && onFailure != null) {
                            onFailure.accept(player);
                        }
                    }
                }
            }.runTaskLater(plugin, 30L); // 1.5 secondes pour voir l'animation
        }

        void cancel() {
            isEnded = true;
            if (jumpTask != null) {
                jumpTask.cancel();
            }
        }
    }

    /**
     * Vérifie si un joueur a un jeu en cours
     */
    public boolean hasActiveGame(Player player) {
        return activeGames.containsKey(player.getUniqueId());
    }

    /**
     * Annule le jeu d'un joueur
     */
    public void cancelGame(Player player) {
        FrogGame game = activeGames.remove(player.getUniqueId());
        if (game != null) {
            game.cancel();
        }
    }

    /**
     * Nettoyage lors de la désactivation
     */
    public void cleanup() {
        for (FrogGame game : activeGames.values()) {
            game.cancel();
            if (game.player.isOnline()) {
                game.player.closeInventory();
            }
        }
        activeGames.clear();
        HandlerList.unregisterAll(this);
    }
}
