package com.rinaorc.zombiez.progression.journey.chapter3;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
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
 * Jeu de Memory pour l'étape 3.4 du Chapitre 3
 *
 * Layout (54 slots = 6 lignes de 9):
 * ┌─────────────────────────────────┐
 * │ [▪][▪][▪][▪][ℹ][▪][▪][▪][▪]    │ Ligne 0: Bordure + Info
 * │ [▪][·][①][②][③][④][⑤][·][▪]    │ Ligne 1: Cartes 1-5
 * │ [▪][·][⑥][⑦][⑧][⑨][⑩][·][▪]    │ Ligne 2: Cartes 6-10
 * │ [▪][·][⑪][⑫][⑬][⑭][⑮][·][▪]    │ Ligne 3: Cartes 11-15 (dernière = vide si impair)
 * │ [▪][·][·][·][·][·][·][·][▪]    │ Ligne 4: Espace
 * │ [▪][▪][▪][▪][✖][▪][▪][▪][▪]    │ Ligne 5: Bordure + Abandonner
 * └─────────────────────────────────┘
 *
 * 7 paires = 14 cartes
 */
public class MemoryGameGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final String TITLE = "§d§l🎪 Jeu de Mémoire";

    // Slots pour les cartes (14 cartes = 7 paires)
    private static final int[] CARD_SLOTS = {
        11, 12, 13, 14, 15,  // Ligne 1
        20, 21, 22, 23, 24,  // Ligne 2
        29, 30, 31, 32       // Ligne 3 (4 cartes seulement pour 14 total)
    };

    // Couleurs des paires (7 couleurs = 7 paires)
    private static final Material[] PAIR_MATERIALS = {
        Material.RED_WOOL,
        Material.ORANGE_WOOL,
        Material.YELLOW_WOOL,
        Material.LIME_WOOL,
        Material.LIGHT_BLUE_WOOL,
        Material.BLUE_WOOL,
        Material.MAGENTA_WOOL
    };

    private static final Material CARD_BACK = Material.GRAY_WOOL;
    private static final Material MATCHED_CARD = Material.WHITE_STAINED_GLASS_PANE;

    private static final int SLOT_INFO = 4;
    private static final int SLOT_QUIT = 49;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final Chapter3Systems chapter3Systems;

    // État du jeu
    private final Material[] cardValues; // Valeur réelle de chaque carte
    private final boolean[] revealed;    // Carte retournée ?
    private final boolean[] matched;     // Paire trouvée ?
    private int firstCardSlot = -1;      // Premier clic
    private int secondCardSlot = -1;     // Deuxième clic
    private int pairsFound = 0;          // Paires trouvées
    private int attempts = 0;            // Nombre de tentatives
    private boolean processing = false;  // En cours de traitement (empêche les clics)

    public MemoryGameGUI(ZombieZPlugin plugin, Player player, Chapter3Systems chapter3Systems) {
        this.plugin = plugin;
        this.player = player;
        this.chapter3Systems = chapter3Systems;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        this.cardValues = new Material[CARD_SLOTS.length];
        this.revealed = new boolean[CARD_SLOTS.length];
        this.matched = new boolean[CARD_SLOTS.length];

        initializeCards();
        setupGUI();
    }

    /**
     * Initialise les cartes avec des paires mélangées
     */
    private void initializeCards() {
        List<Material> cards = new ArrayList<>();

        // Ajouter 2 de chaque couleur (7 paires = 14 cartes)
        for (Material mat : PAIR_MATERIALS) {
            cards.add(mat);
            cards.add(mat);
        }

        // Mélanger les cartes
        Collections.shuffle(cards, new Random());

        // Assigner aux slots
        for (int i = 0; i < CARD_SLOTS.length && i < cards.size(); i++) {
            cardValues[i] = cards.get(i);
            revealed[i] = false;
            matched[i] = false;
        }
    }

    /**
     * Configure l'affichage initial du GUI
     */
    private void setupGUI() {
        // Fond gris
        ItemStack filler = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure rose (thème cirque)
        ItemStack border = ItemBuilder.placeholder(Material.MAGENTA_STAINED_GLASS_PANE);
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

        // Cartes (face cachée)
        for (int i = 0; i < CARD_SLOTS.length; i++) {
            inventory.setItem(CARD_SLOTS[i], createCardBack(i + 1));
        }

        // Bouton abandonner
        inventory.setItem(SLOT_QUIT, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Abandonner")
            .lore("", "§7Cliquez pour quitter le jeu")
            .build());
    }

    private ItemStack createInfoItem() {
        return new ItemBuilder(Material.FIREWORK_ROCKET)
            .name("§d§l🎪 JEU DE MÉMOIRE")
            .lore(
                "",
                "§7Trouve toutes les paires de couleurs!",
                "",
                "§e§l➤ §7Comment jouer:",
                "§7  1. Clique sur une carte pour la retourner",
                "§7  2. Retourne une 2ème carte",
                "§7  3. Si elles matchent, tu gagnes la paire!",
                "",
                "§7Paires trouvées: §a" + pairsFound + "§7/§e7",
                "§7Tentatives: §e" + attempts,
                ""
            )
            .glow()
            .build();
    }

    private ItemStack createCardBack(int cardNumber) {
        return new ItemBuilder(CARD_BACK)
            .name("§8Carte #" + cardNumber)
            .lore("", "§7Cliquez pour retourner")
            .build();
    }

    private ItemStack createRevealedCard(Material material, int cardNumber) {
        String colorName = getColorName(material);
        return new ItemBuilder(material)
            .name("§f" + colorName)
            .lore("", "§7Carte #" + cardNumber)
            .build();
    }

    private ItemStack createMatchedCard() {
        return new ItemBuilder(MATCHED_CARD)
            .name("§a✓ Paire trouvée!")
            .lore("")
            .glow()
            .build();
    }

    private String getColorName(Material material) {
        return switch (material) {
            case RED_WOOL -> "§cRouge";
            case ORANGE_WOOL -> "§6Orange";
            case YELLOW_WOOL -> "§eJaune";
            case LIME_WOOL -> "§aVert";
            case LIGHT_BLUE_WOOL -> "§bCyan";
            case BLUE_WOOL -> "§9Bleu";
            case MAGENTA_WOOL -> "§dMagenta";
            default -> "§7Inconnu";
        };
    }

    /**
     * Gère un clic sur une carte
     */
    public void handleCardClick(int slot) {
        if (processing) return;

        // Trouver l'index de la carte
        int cardIndex = -1;
        for (int i = 0; i < CARD_SLOTS.length; i++) {
            if (CARD_SLOTS[i] == slot) {
                cardIndex = i;
                break;
            }
        }

        if (cardIndex == -1) return;

        // Ignorer si déjà retournée ou matchée
        if (revealed[cardIndex] || matched[cardIndex]) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            return;
        }

        // Premier clic
        if (firstCardSlot == -1) {
            firstCardSlot = cardIndex;
            revealed[cardIndex] = true;
            inventory.setItem(CARD_SLOTS[cardIndex], createRevealedCard(cardValues[cardIndex], cardIndex + 1));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.2f);
        }
        // Deuxième clic
        else if (secondCardSlot == -1 && cardIndex != firstCardSlot) {
            secondCardSlot = cardIndex;
            revealed[cardIndex] = true;
            inventory.setItem(CARD_SLOTS[cardIndex], createRevealedCard(cardValues[cardIndex], cardIndex + 1));
            attempts++;

            // Vérifier si c'est une paire
            processing = true;

            if (cardValues[firstCardSlot] == cardValues[secondCardSlot]) {
                // Match trouvé !
                handleMatch();
            } else {
                // Pas de match
                handleNoMatch();
            }
        }
    }

    private void handleMatch() {
        final int first = firstCardSlot;
        final int second = secondCardSlot;

        // Petit délai pour montrer les cartes
        new BukkitRunnable() {
            @Override
            public void run() {
                matched[first] = true;
                matched[second] = true;
                inventory.setItem(CARD_SLOTS[first], createMatchedCard());
                inventory.setItem(CARD_SLOTS[second], createMatchedCard());

                pairsFound++;

                // Mettre à jour l'info
                inventory.setItem(SLOT_INFO, createInfoItem());

                // Effets
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5);

                // Vérifier victoire
                if (pairsFound >= 7) {
                    handleWin();
                } else {
                    // Reset pour la prochaine paire
                    firstCardSlot = -1;
                    secondCardSlot = -1;
                    processing = false;
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    private void handleNoMatch() {
        final int first = firstCardSlot;
        final int second = secondCardSlot;

        // Délai pour montrer les cartes avant de les retourner
        new BukkitRunnable() {
            @Override
            public void run() {
                // Retourner les cartes
                revealed[first] = false;
                revealed[second] = false;
                inventory.setItem(CARD_SLOTS[first], createCardBack(first + 1));
                inventory.setItem(CARD_SLOTS[second], createCardBack(second + 1));

                // Mettre à jour l'info
                inventory.setItem(SLOT_INFO, createInfoItem());

                // Son d'échec
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);

                // Reset pour la prochaine paire
                firstCardSlot = -1;
                secondCardSlot = -1;
                processing = false;
            }
        }.runTaskLater(plugin, 20L); // 1 seconde pour voir les cartes
    }

    private void handleWin() {
        // Fermer le GUI
        player.closeInventory();

        // Notification victoire
        player.sendTitle(
            "§a§l✓ PUZZLE RÉSOLU!",
            "§7" + attempts + " tentatives",
            10, 60, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l🎪 PUZZLE COMPLÉTÉ!");
        player.sendMessage("");
        player.sendMessage("  §7Paires trouvées: §a7§7/§e7");
        player.sendMessage("  §7Tentatives: §e" + attempts);
        player.sendMessage("");

        // Bonus selon les tentatives
        String bonus = "";
        if (attempts <= 10) {
            bonus = "§6§l★ PARFAIT! §eBonus +50 Points";
            plugin.getEconomyManager().addPoints(player, 50);
        } else if (attempts <= 15) {
            bonus = "§e★ Très bien! §eBonus +25 Points";
            plugin.getEconomyManager().addPoints(player, 25);
        } else {
            bonus = "§7★ Réussi!";
        }
        player.sendMessage("  " + bonus);
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 2, 0), 30, 1, 1, 1, 0.1);

        // Marquer comme complété
        chapter3Systems.onPuzzleCompleted(player);
    }

    public void handleQuit() {
        player.closeInventory();
        player.sendMessage("§c✖ Puzzle abandonné. Reparle au Forain pour réessayer!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public boolean isCardSlot(int slot) {
        for (int cardSlot : CARD_SLOTS) {
            if (cardSlot == slot) return true;
        }
        return false;
    }

    public int getSlotQuit() {
        return SLOT_QUIT;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ==================== LISTENER ====================

    public static class MemoryGameListener implements Listener {

        private final ZombieZPlugin plugin;
        // Track des joueurs en jeu pour éviter de réouvrir le GUI à la fermeture
        private final Set<UUID> playersInGame = ConcurrentHashMap.newKeySet();

        public MemoryGameListener(ZombieZPlugin plugin) {
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
            if (!(event.getInventory().getHolder() instanceof MemoryGameGUI gui)) {
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

            // Clic sur une carte
            if (gui.isCardSlot(slot)) {
                gui.handleCardClick(slot);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof MemoryGameGUI) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof MemoryGameGUI) {
                removePlayerFromGame(event.getPlayer().getUniqueId());
            }
        }
    }
}
