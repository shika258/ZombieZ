package com.rinaorc.zombiez.zones.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.ItemBuilder;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Refuge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI des refuges - Affiche tous les refuges avec possibilité de téléportation
 *
 * Layout symétrique (54 slots = 6 lignes de 9):
 * ┌─────────────────────────────────┐
 * │ [▪][▪][▪][▪][ℹ][▪][▪][▪][▪]    │ Ligne 0: Bordure + Info
 * │ [▪][·][①][②][③][④][⑤][·][▪]    │ Ligne 1: Refuges 1-5
 * │ [▪][·][⑥][⑦][⑧][⑨][⑩][·][▪]    │ Ligne 2: Refuges 6-10
 * │ [▪][·][·][·][⚓][·][·][·][▪]    │ Ligne 3: Checkpoint actuel
 * │ [▪][·][·][·][·][·][·][·][▪]    │ Ligne 4: Espace
 * │ [▪][▪][▪][▪][✖][▪][▪][▪][▪]    │ Ligne 5: Bordure + Fermer
 * └─────────────────────────────────┘
 */
public class RefugeGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final String TITLE = "§8§l🏠 Refuges";

    // Slots pour les refuges (2 lignes de 5, parfaitement centrées)
    // Ligne 1: slots 11, 12, 13, 14, 15 (refuges 1-5)
    // Ligne 2: slots 20, 21, 22, 23, 24 (refuges 6-10)
    private static final int[] REFUGE_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    // Slots spéciaux (tous centrés sur la colonne 4)
    private static final int SLOT_INFO = 4;
    private static final int SLOT_CURRENT_CHECKPOINT = 31;
    private static final int SLOT_CLOSE = 49;

    // Bordures latérales (colonne 0 et 8)
    private static final int[] SIDE_BORDER_SLOTS = {9, 17, 18, 26, 27, 35, 36, 44};

    // Décoration intérieure (colonnes 1 et 7)
    private static final int[] INNER_DECOR_SLOTS = {10, 16, 19, 25, 28, 29, 30, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final ZombieZPlugin plugin;
    private final Player player;
    private final PlayerData playerData;
    private final Inventory inventory;

    // Mapping slot -> refuge ID pour les clics
    private final Map<Integer, Integer> slotToRefugeId = new HashMap<>();

    public RefugeGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerData = plugin.getPlayerDataManager().getPlayer(player);
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        setupGUI();
    }

    private void setupGUI() {
        // Fond gris foncé pour l'intérieur
        ItemStack filler = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure jaune (ligne du haut et du bas)
        ItemStack border = ItemBuilder.placeholder(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);           // Ligne 0
            inventory.setItem(SIZE - 9 + i, border); // Ligne 5
        }

        // Bordures latérales jaunes (colonnes 0 et 8)
        for (int slot : SIDE_BORDER_SLOTS) {
            inventory.setItem(slot, border);
        }

        // Décoration intérieure (noir pour contraste)
        ItemStack innerDecor = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot : INNER_DECOR_SLOTS) {
            inventory.setItem(slot, innerDecor);
        }

        // Info header (centre de la ligne 0)
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Afficher les refuges (parfaitement centrés)
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager != null) {
            List<Refuge> refuges = refugeManager.getRefugesSorted();
            int slotIndex = 0;

            for (Refuge refuge : refuges) {
                if (slotIndex < REFUGE_SLOTS.length) {
                    int slot = REFUGE_SLOTS[slotIndex];
                    inventory.setItem(slot, createRefugeItem(refuge));
                    slotToRefugeId.put(slot, refuge.getId());
                    slotIndex++;
                }
            }
        }

        // Checkpoint actuel (centre de la ligne 3)
        inventory.setItem(SLOT_CURRENT_CHECKPOINT, createCurrentCheckpointItem());

        // Bouton fermer (centre de la ligne 5)
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Fermer")
            .lore("", "§7Cliquez pour fermer le menu")
            .build());
    }

    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Les refuges sont des zones sécurisées");
        lore.add("§7où les zombies ne peuvent pas spawn.");
        lore.add("");
        lore.add("§e§l➤ §7Fonctionnalités:");
        lore.add("§a  ✓ §7Zone protégée contre les zombies");
        lore.add("§a  ✓ §7Activation de checkpoint");
        lore.add("§a  ✓ §7Téléportation rapide");
        lore.add("§a  ✓ §7Marchands et services");
        lore.add("");
        lore.add("§e§l➤ §7Comment débloquer:");
        lore.add("§7  Trouvez le §ebeacon §7du refuge");
        lore.add("§7  et faites §eclic droit §7dessus!");
        lore.add("");

        int unlockedCount = getUnlockedRefugesCount();
        int totalCount = plugin.getRefugeManager() != null ? plugin.getRefugeManager().getAllRefuges().size() : 0;
        lore.add("§7Refuges débloqués: §a" + unlockedCount + "§7/§e" + totalCount);

        return new ItemBuilder(Material.BEACON)
            .name("§e§l🏠 SYSTÈME DE REFUGES")
            .lore(lore)
            .glow()
            .build();
    }

    private ItemStack createRefugeItem(Refuge refuge) {
        int currentCheckpoint = playerData != null ? playerData.getCurrentCheckpoint().get() : 0;
        int playerLevel = playerData != null ? playerData.getLevel().get() : 1;
        long playerPoints = playerData != null ? playerData.getPoints().get() : 0;

        boolean isUnlocked = currentCheckpoint >= refuge.getId();
        boolean isCurrentCheckpoint = currentCheckpoint == refuge.getId();
        boolean canUnlock = playerLevel >= refuge.getRequiredLevel();
        boolean canAfford = playerPoints >= refuge.getCost();

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description
        if (refuge.getDescription() != null && !refuge.getDescription().isEmpty()) {
            lore.add("§7§o\"" + refuge.getDescription() + "\"");
            lore.add("");
        }

        // Statut
        if (isCurrentCheckpoint) {
            lore.add("§a§l✓ CHECKPOINT ACTIF");
            lore.add("");
        } else if (isUnlocked) {
            lore.add("§a✓ Débloqué");
            lore.add("");
        } else {
            lore.add("§c✖ Verrouillé");
            lore.add("");
        }

        // Informations
        lore.add("§7═══════════════════════");
        lore.add("");
        lore.add("§7Zone: §f" + getZoneRange(refuge.getId()));
        lore.add("§7Coût: §6" + EconomyManager.formatPoints(refuge.getCost()) + " Points");
        lore.add("§7Niveau requis: §e" + refuge.getRequiredLevel());
        lore.add("");

        // Zone protégée
        lore.add("§7Zone protégée:");
        lore.add("§8  X: " + refuge.getProtectedMinX() + " → " + refuge.getProtectedMaxX());
        lore.add("§8  Z: " + refuge.getProtectedMinZ() + " → " + refuge.getProtectedMaxZ());
        lore.add("");

        // Actions possibles
        if (isUnlocked) {
            lore.add("§a▶ Clic gauche: §7Se téléporter");
            if (!isCurrentCheckpoint) {
                lore.add("§e▶ Clic droit: §7Définir comme checkpoint");
            }
        } else {
            if (!canUnlock) {
                lore.add("§c⚠ Niveau insuffisant!");
                lore.add("§7  (Vous: niveau §c" + playerLevel + "§7)");
            } else {
                lore.add("§7Trouvez le beacon pour débloquer!");
            }
        }

        // Matériau et effet selon le statut
        Material material;
        boolean glow = false;

        if (isCurrentCheckpoint) {
            material = Material.BEACON;
            glow = true;
        } else if (isUnlocked) {
            material = Material.LIME_CONCRETE;
        } else if (canUnlock) {
            material = Material.YELLOW_CONCRETE;
        } else {
            material = Material.RED_CONCRETE;
        }

        // Préfixe du nom
        String prefix = isCurrentCheckpoint ? "§a§l✓ " : (isUnlocked ? "§a" : "§c");
        String suffix = isCurrentCheckpoint ? " §7[ACTIF]" : "";

        ItemBuilder builder = new ItemBuilder(material)
            .name(prefix + refuge.getName() + suffix)
            .lore(lore);

        if (glow) {
            builder.glow();
        }

        return builder.build();
    }

    private ItemStack createCurrentCheckpointItem() {
        int currentCheckpoint = playerData != null ? playerData.getCurrentCheckpoint().get() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (currentCheckpoint == 0) {
            lore.add("§7Vous n'avez pas encore");
            lore.add("§7activé de checkpoint.");
            lore.add("");
            lore.add("§7Trouvez un §ebeacon §7dans");
            lore.add("§7un refuge pour commencer!");

            return new ItemBuilder(Material.GRAY_DYE)
                .name("§7Aucun Checkpoint Actif")
                .lore(lore)
                .build();
        }

        var refugeManager = plugin.getRefugeManager();
        Refuge currentRefuge = refugeManager != null ? refugeManager.getRefugeById(currentCheckpoint) : null;

        if (currentRefuge != null) {
            lore.add("§7Checkpoint actuel:");
            lore.add("§e§l" + currentRefuge.getName());
            lore.add("");
            lore.add("§7Zone: §f" + getZoneRange(currentRefuge.getId()));
            lore.add("");
            lore.add("§7Si vous mourrez, vous");
            lore.add("§7réapparaîtrez ici!");
        } else {
            lore.add("§7Refuge #" + currentCheckpoint);
        }

        return new ItemBuilder(Material.RESPAWN_ANCHOR)
            .name("§a§l⚓ Checkpoint Actuel")
            .lore(lore)
            .glow()
            .build();
    }

    private String getZoneRange(int refugeId) {
        // Les refuges correspondent aux zones: 1→4-5, 2→9-10, etc.
        return switch (refugeId) {
            case 1 -> "4-5";
            case 2 -> "9-10";
            case 3 -> "14-15";
            case 4 -> "19-20";
            case 5 -> "24-25";
            case 6 -> "29-30";
            case 7 -> "34-35";
            case 8 -> "39-40";
            case 9 -> "44-45";
            case 10 -> "49-50";
            default -> String.valueOf(refugeId);
        };
    }

    private int getUnlockedRefugesCount() {
        if (playerData == null) return 0;
        int currentCheckpoint = playerData.getCurrentCheckpoint().get();
        return Math.min(currentCheckpoint, 10);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // Getters pour le listener
    public Player getPlayer() { return player; }
    public PlayerData getPlayerData() { return playerData; }
    public ZombieZPlugin getPlugin() { return plugin; }
    public Map<Integer, Integer> getSlotToRefugeId() { return slotToRefugeId; }

    /**
     * Listener pour les événements du GUI
     */
    public static class RefugeGUIListener implements Listener {

        private final ZombieZPlugin plugin;

        // Cooldown pour éviter le spam de téléportation
        private final Map<Player, Long> teleportCooldowns = new HashMap<>();
        private static final long TELEPORT_COOLDOWN_MS = 3000; // 3 secondes

        public RefugeGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof RefugeGUI gui)) {
                return;
            }

            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Bouton fermer
            if (slot == SLOT_CLOSE) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }

            // Clic sur un refuge
            Integer refugeId = gui.getSlotToRefugeId().get(slot);
            if (refugeId != null) {
                handleRefugeClick(gui, player, refugeId, event.isLeftClick(), event.isRightClick());
            }
        }

        private void handleRefugeClick(RefugeGUI gui, Player player, int refugeId, boolean leftClick, boolean rightClick) {
            PlayerData playerData = gui.getPlayerData();
            if (playerData == null) return;

            int currentCheckpoint = playerData.getCurrentCheckpoint().get();

            // Vérifier si le refuge est débloqué
            if (currentCheckpoint < refugeId) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                MessageUtils.send(player, "§c✖ Ce refuge n'est pas encore débloqué!");
                MessageUtils.send(player, "§7Trouvez le beacon dans le refuge pour l'activer.");
                return;
            }

            var refugeManager = plugin.getRefugeManager();
            Refuge refuge = refugeManager != null ? refugeManager.getRefugeById(refugeId) : null;

            if (refuge == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                MessageUtils.send(player, "§cErreur: Refuge introuvable!");
                return;
            }

            // Clic gauche = Téléportation
            if (leftClick) {
                handleTeleport(player, refuge, playerData);
            }
            // Clic droit = Définir comme checkpoint
            else if (rightClick && currentCheckpoint != refugeId) {
                handleSetCheckpoint(player, refuge, playerData);
            }
        }

        private void handleTeleport(Player player, Refuge refuge, PlayerData playerData) {
            // Vérifier le cooldown
            Long lastTeleport = teleportCooldowns.get(player);
            long now = System.currentTimeMillis();

            if (lastTeleport != null && now - lastTeleport < TELEPORT_COOLDOWN_MS) {
                long remaining = (TELEPORT_COOLDOWN_MS - (now - lastTeleport)) / 1000 + 1;
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                MessageUtils.send(player, "§c✖ Veuillez patienter §e" + remaining + "s §cavant de vous téléporter!");
                return;
            }

            // Vérifier si le joueur est en combat (optionnel)
            // TODO: Implémenter la vérification de combat

            // Particules avant téléportation
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);

            // Fermer l'inventaire
            player.closeInventory();

            // Téléporter après un court délai pour l'effet
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.Location spawnLoc = refuge.getSpawnLocation(player.getWorld());
                player.teleport(spawnLoc);

                // Effets après téléportation
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, spawnLoc.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);

                MessageUtils.sendTitle(player, "§e§l" + refuge.getName(), "§7Téléportation réussie!", 10, 30, 10);
                MessageUtils.send(player, "§a✓ Téléporté vers §e" + refuge.getName() + "§a!");

                teleportCooldowns.put(player, System.currentTimeMillis());
            }, 10L); // 0.5 secondes
        }

        private void handleSetCheckpoint(Player player, Refuge refuge, PlayerData playerData) {
            int currentCheckpoint = playerData.getCurrentCheckpoint().get();
            int refugeId = refuge.getId();

            // Ne peut pas définir un checkpoint plus bas que l'actuel (progression)
            if (refugeId < currentCheckpoint) {
                // Permettre de revenir en arrière gratuitement
                playerData.setCheckpoint(refugeId);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                MessageUtils.send(player, "§a✓ Checkpoint changé vers §e" + refuge.getName() + "§a!");

                // Rafraîchir le GUI
                new RefugeGUI(plugin, player).open();
                return;
            }

            // Ce cas ne devrait pas arriver car on a déjà vérifié que le refuge est débloqué
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            MessageUtils.send(player, "§a✓ §e" + refuge.getName() + " §aest votre checkpoint actuel!");
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof RefugeGUI) {
                event.setCancelled(true);
            }
        }
    }
}
