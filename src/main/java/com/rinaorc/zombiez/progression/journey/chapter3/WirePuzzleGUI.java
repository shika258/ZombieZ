package com.rinaorc.zombiez.progression.journey.chapter3;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puzzle de connexion de fils pour l'étape 3.8 du Chapitre 3
 * Le joueur doit créer un chemin de la gauche vers la droite
 *
 * Layout (54 slots = 6 lignes de 9):
 * ┌─────────────────────────────────┐
 * │ [▪][▪][▪][▪][ℹ][▪][▪][▪][▪]    │ Ligne 0: Bordure + Info
 * │ [▪][⚡][·][·][·][·][·][💡][▪]    │ Ligne 1: Grille (source -> destination)
 * │ [▪][·][█][·][█][·][·][·][▪]    │ Ligne 2: Grille avec obstacles
 * │ [▪][·][·][·][·][█][·][·][▪]    │ Ligne 3: Grille avec obstacles
 * │ [▪][·][·][█][·][·][·][·][▪]    │ Ligne 4: Grille avec obstacles
 * │ [▪][▪][▪][▪][✖][▪][▪][▪][▪]    │ Ligne 5: Bordure + Abandonner
 * └─────────────────────────────────┘
 *
 * Légende:
 * - ⚡ = Source du fil (gauche)
 * - 💡 = Destination (droite)
 * - █ = Obstacle (ne peut pas être traversé)
 * - · = Case vide (cliquable pour placer un fil)
 */
public class WirePuzzleGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final String TITLE = "§6§l⚡ Panneau de Contrôle";

    // Slots de la grille (4 lignes x 6 colonnes, de slot 10 à 16, etc.)
    // Ligne 1: slots 10, 11, 12, 13, 14, 15, 16
    // Ligne 2: slots 19, 20, 21, 22, 23, 24, 25
    // Ligne 3: slots 28, 29, 30, 31, 32, 33, 34
    // Ligne 4: slots 37, 38, 39, 40, 41, 42, 43
    private static final int GRID_START_ROW = 1;
    private static final int GRID_END_ROW = 4;
    private static final int GRID_START_COL = 1;
    private static final int GRID_END_COL = 7;

    // Source et destination
    private static final int SOURCE_COL = 1;
    private static final int DEST_COL = 7;
    private static final int SOURCE_ROW = 1;
    private static final int DEST_ROW = 3;

    private static final int SLOT_INFO = 4;
    private static final int SLOT_QUIT = 49;

    // Matériaux
    private static final Material EMPTY_CELL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material WIRE_CELL = Material.LIME_STAINED_GLASS_PANE;
    private static final Material OBSTACLE_CELL = Material.BLACK_CONCRETE;
    private static final Material SOURCE_CELL = Material.YELLOW_CONCRETE;
    private static final Material DEST_CELL = Material.ORANGE_CONCRETE;
    private static final Material CONNECTED_WIRE = Material.GREEN_CONCRETE;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final Chapter3Systems chapter3Systems;

    // État du puzzle (grille 4x7)
    // 0 = vide, 1 = fil placé, 2 = obstacle, 3 = source, 4 = destination
    private final int[][] grid = new int[4][7];
    private boolean solved = false;
    private boolean processing = false;

    // Positions des obstacles (row, col)
    private static final int[][] OBSTACLES = {
        {1, 2},  // Ligne 2, colonne 3
        {1, 4},  // Ligne 2, colonne 5
        {2, 5},  // Ligne 3, colonne 6
        {3, 3}   // Ligne 4, colonne 4
    };

    public WirePuzzleGUI(ZombieZPlugin plugin, Player player, Chapter3Systems chapter3Systems) {
        this.plugin = plugin;
        this.player = player;
        this.chapter3Systems = chapter3Systems;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        initializeGrid();
        setupGUI();
    }

    /**
     * Initialise la grille du puzzle
     */
    private void initializeGrid() {
        // Tout à 0 (vide)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 7; col++) {
                grid[row][col] = 0;
            }
        }

        // Placer les obstacles
        for (int[] obs : OBSTACLES) {
            grid[obs[0]][obs[1]] = 2;
        }

        // Placer la source et destination
        grid[SOURCE_ROW - 1][SOURCE_COL - 1] = 3; // Source en haut à gauche de la grille
        grid[DEST_ROW - 1][DEST_COL - 1] = 4;     // Destination à droite
    }

    /**
     * Configure l'affichage du GUI
     */
    private void setupGUI() {
        // Fond noir
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure orange (thème électrique)
        ItemStack border = ItemBuilder.placeholder(Material.ORANGE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);           // Ligne 0
            inventory.setItem(SIZE - 9 + i, border); // Ligne 5
        }
        // Colonnes latérales
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }

        // Info header
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Afficher la grille
        updateGridDisplay();

        // Bouton abandonner
        inventory.setItem(SLOT_QUIT, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Abandonner")
            .lore("", "§7Cliquez pour quitter")
            .build());
    }

    private ItemStack createInfoItem() {
        return new ItemBuilder(Material.REDSTONE_TORCH)
            .name("§6§l⚡ RECONNEXION DES FILS")
            .lore(
                "",
                "§7Créez un chemin continu de la",
                "§esource §7(jaune) vers la §6destination §7(orange).",
                "",
                "§e§l➤ §7Comment jouer:",
                "§7  1. Cliquez sur les cases grises",
                "§7  2. Créez un chemin connecté",
                "§7  3. Les fils doivent se toucher",
                "",
                "§c█ §7= Obstacle (infranchissable)",
                "§a█ §7= Fil placé",
                ""
            )
            .glow()
            .build();
    }

    /**
     * Met à jour l'affichage de la grille
     */
    private void updateGridDisplay() {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 7; col++) {
                int slot = getSlotFromGridPos(row, col);
                ItemStack item = getCellItem(row, col);
                inventory.setItem(slot, item);
            }
        }
    }

    /**
     * Convertit une position de grille en slot d'inventaire
     */
    private int getSlotFromGridPos(int row, int col) {
        return (row + 1) * 9 + (col + 1);
    }

    /**
     * Convertit un slot en position de grille (row, col) ou null si hors grille
     */
    private int[] getGridPosFromSlot(int slot) {
        int row = slot / 9 - 1;
        int col = slot % 9 - 1;

        if (row >= 0 && row < 4 && col >= 0 && col < 7) {
            return new int[]{row, col};
        }
        return null;
    }

    /**
     * Crée l'item pour une cellule de la grille
     */
    private ItemStack getCellItem(int row, int col) {
        int value = grid[row][col];

        return switch (value) {
            case 0 -> new ItemBuilder(EMPTY_CELL)
                .name("§8Case vide")
                .lore("", "§7Cliquez pour placer un fil")
                .build();
            case 1 -> new ItemBuilder(WIRE_CELL)
                .name("§a⚡ Fil connecté")
                .lore("", "§7Cliquez pour retirer")
                .build();
            case 2 -> new ItemBuilder(OBSTACLE_CELL)
                .name("§c✖ Obstacle")
                .lore("", "§7Infranchissable")
                .build();
            case 3 -> new ItemBuilder(SOURCE_CELL)
                .name("§e⚡ SOURCE")
                .lore("", "§7Point de départ du fil")
                .glow()
                .build();
            case 4 -> new ItemBuilder(DEST_CELL)
                .name("§6💡 DESTINATION")
                .lore("", "§7Le fil doit arriver ici")
                .glow()
                .build();
            default -> ItemBuilder.placeholder(Material.AIR);
        };
    }

    /**
     * Gère un clic sur une cellule de la grille
     */
    public void handleCellClick(int slot) {
        if (processing || solved) return;

        int[] pos = getGridPosFromSlot(slot);
        if (pos == null) return;

        int row = pos[0];
        int col = pos[1];
        int value = grid[row][col];

        // Ne peut pas modifier source, destination ou obstacles
        if (value == 2 || value == 3 || value == 4) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Toggle le fil
        if (value == 0) {
            grid[row][col] = 1; // Placer un fil
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.5f);
        } else if (value == 1) {
            grid[row][col] = 0; // Retirer le fil
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 0.5f);
        }

        // Mettre à jour l'affichage
        updateGridDisplay();

        // Vérifier si le puzzle est résolu
        checkSolution();
    }

    /**
     * Vérifie si un chemin valide existe de la source à la destination
     */
    private void checkSolution() {
        // Trouver la source
        int sourceRow = SOURCE_ROW - 1;
        int sourceCol = SOURCE_COL - 1;

        // BFS pour trouver un chemin
        boolean[][] visited = new boolean[4][7];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{sourceRow, sourceCol});
        visited[sourceRow][sourceCol] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // haut, bas, gauche, droite

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];

            // Vérifie si on a atteint la destination
            if (grid[r][c] == 4) {
                handleWin();
                return;
            }

            // Explorer les voisins
            for (int[] dir : directions) {
                int nr = r + dir[0];
                int nc = c + dir[1];

                if (nr >= 0 && nr < 4 && nc >= 0 && nc < 7 && !visited[nr][nc]) {
                    int cellValue = grid[nr][nc];
                    // Peut traverser: fil (1), source (3), destination (4)
                    if (cellValue == 1 || cellValue == 3 || cellValue == 4) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
    }

    /**
     * Gère la victoire
     */
    private void handleWin() {
        solved = true;
        processing = true;

        // Animation de victoire - illuminer le chemin
        highlightPath();

        // Notification
        player.sendTitle(
            "§a§l⚡ CONNEXION ÉTABLIE!",
            "§7Le Zeppelin est réparé!",
            10, 60, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l⚡ ZEPPELIN RÉPARÉ!");
        player.sendMessage("");
        player.sendMessage("  §7Les fils sont maintenant connectés.");
        player.sendMessage("  §7Le Zeppelin peut reprendre son vol!");
        player.sendMessage("");
        player.sendMessage("  §e+800 Points §7| §a+20 Niveaux XP");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.1);

        // Fermer après un délai
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.closeInventory();
                    chapter3Systems.onZeppelinRepaired(player);
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    /**
     * Illumine le chemin de la source à la destination
     */
    private void highlightPath() {
        // Changer tous les fils en vert brillant
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 7; col++) {
                if (grid[row][col] == 1) {
                    int slot = getSlotFromGridPos(row, col);
                    inventory.setItem(slot, new ItemBuilder(CONNECTED_WIRE)
                        .name("§a§l⚡ Connexion!")
                        .glow()
                        .build());
                }
            }
        }
    }

    public void handleQuit() {
        player.closeInventory();
        player.sendMessage("§c✖ Réparation abandonnée. Reviens quand tu veux!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public boolean isGridSlot(int slot) {
        int[] pos = getGridPosFromSlot(slot);
        return pos != null;
    }

    public int getSlotQuit() {
        return SLOT_QUIT;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ==================== LISTENER ====================

    public static class WirePuzzleListener implements Listener {

        private final ZombieZPlugin plugin;
        private final Set<UUID> playersInGame = ConcurrentHashMap.newKeySet();

        public WirePuzzleListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        public void addPlayerInGame(UUID uuid) {
            playersInGame.add(uuid);
        }

        public void removePlayerFromGame(UUID uuid) {
            playersInGame.remove(uuid);
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof WirePuzzleGUI gui)) {
                return;
            }

            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Bouton quitter
            if (slot == gui.getSlotQuit()) {
                gui.handleQuit();
                return;
            }

            // Clic sur une case de la grille
            if (gui.isGridSlot(slot)) {
                gui.handleCellClick(slot);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof WirePuzzleGUI) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof WirePuzzleGUI) {
                removePlayerFromGame(event.getPlayer().getUniqueId());
            }
        }
    }
}
