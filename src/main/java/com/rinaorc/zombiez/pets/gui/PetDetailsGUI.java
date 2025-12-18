package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GUI des détails d'un pet spécifique
 */
public class PetDetailsGUI implements InventoryHolder {

    private static final int SIZE = 45;

    // Slots
    private static final int SLOT_PET_ICON = 13;
    private static final int SLOT_PASSIVE = 20;
    private static final int SLOT_ACTIVE = 22;
    private static final int SLOT_STAR_POWER = 24;
    private static final int SLOT_STATS = 31;
    private static final int SLOT_BACK = 40;
    private static final int SLOT_EQUIP = 42;
    private static final int SLOT_FAVORITE = 38;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PetType type;
    private final PlayerPetData playerData;
    private final PetData petData;

    public PetDetailsGUI(ZombieZPlugin plugin, Player player, PetType type) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
        this.playerData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        this.petData = playerData != null ? playerData.getPet(type) : null;

        String title = "§8§l" + type.getDisplayName();
        this.inventory = Bukkit.createInventory(this, SIZE, title);

        setupGUI();
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure colorée selon la rareté
        Material borderMat = switch (type.getRarity()) {
            case COMMON -> Material.WHITE_STAINED_GLASS_PANE;
            case UNCOMMON -> Material.LIME_STAINED_GLASS_PANE;
            case RARE -> Material.CYAN_STAINED_GLASS_PANE;
            case EPIC -> Material.PURPLE_STAINED_GLASS_PANE;
            case LEGENDARY -> Material.ORANGE_STAINED_GLASS_PANE;
            case MYTHIC -> Material.RED_STAINED_GLASS_PANE;
        };
        ItemStack border = ItemBuilder.placeholder(borderMat);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }

        // Icône du pet
        inventory.setItem(SLOT_PET_ICON, createMainPetItem());

        // Capacité passive
        inventory.setItem(SLOT_PASSIVE, createPassiveItem());

        // Capacité active
        inventory.setItem(SLOT_ACTIVE, createActiveItem());

        // Star Power
        inventory.setItem(SLOT_STAR_POWER, createStarPowerItem());

        // Statistiques
        inventory.setItem(SLOT_STATS, createStatsItem());

        // Boutons
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());

        if (petData != null) {
            boolean isEquipped = type == playerData.getEquippedPet();
            inventory.setItem(SLOT_EQUIP, new ItemBuilder(isEquipped ? Material.ENDER_EYE : Material.ENDER_PEARL)
                .name(isEquipped ? "§c✖ Déséquiper" : "§a✓ Équiper")
                .lore(
                    "",
                    isEquipped ? "§7Ce pet est actuellement équipé." : "§7Cliquez pour équiper ce pet."
                )
                .glow(isEquipped)
                .build());

            inventory.setItem(SLOT_FAVORITE, new ItemBuilder(petData.isFavorite() ? Material.NETHER_STAR : Material.FIREWORK_STAR)
                .name(petData.isFavorite() ? "§e★ Favori" : "§7☆ Ajouter aux favoris")
                .lore(
                    "",
                    "§7Marquez vos pets préférés",
                    "§7comme favoris!"
                )
                .glow(petData.isFavorite())
                .build());
        }
    }

    private ItemStack createMainPetItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Rareté: " + type.getRarity().getColoredName());
        lore.add("§7Thème: §f" + type.getTheme());
        lore.add("");
        lore.add("§7Apparence:");
        lore.add("§8" + type.getAppearance());

        if (petData != null) {
            lore.add("");
            lore.add("§7═══ PROGRESSION ═══");
            lore.add("");
            lore.add("§7Niveau: §a" + petData.getLevel() + "§7/§e9");
            lore.add("§7Copies: §b" + petData.getCopies());
            lore.add("");

            int copiesNeeded = petData.getCopiesForNextLevel();
            if (copiesNeeded > 0) {
                lore.add("§7Prochain niveau: §e" + copiesNeeded + " copies");
            } else {
                lore.add("§a✓ Niveau maximum atteint!");
            }

            lore.add("");
            lore.add(petData.getProgressBar());
            lore.add("§7" + String.format("%.1f", petData.getProgressPercent()) + "% complété");
        } else {
            lore.add("");
            lore.add("§8Pet non possédé");
        }

        String stars = (petData != null && petData.getStarPower() > 0) ?
            " §e" + "★".repeat(petData.getStarPower()) : "";

        return new ItemBuilder(type.getIcon())
            .name(type.getColoredName() + stars)
            .lore(lore)
            .glow(petData != null && petData.hasEvolution())
            .build();
    }

    private ItemStack createPassiveItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§f" + type.getPassiveDescription());
        lore.add("");

        if (petData != null) {
            double multiplier = petData.getStatMultiplier();
            lore.add("§7Puissance actuelle: §a" + String.format("%.0f", multiplier * 100) + "%");
            lore.add("§8(+10% par niveau)");

            if (petData.hasLevel5Bonus()) {
                lore.add("");
                lore.add("§a✓ Bonus Niveau 5:");
                lore.add("§f" + type.getLevel5Bonus());
            } else {
                lore.add("");
                lore.add("§c✖ Bonus Niveau 5:");
                lore.add("§8" + type.getLevel5Bonus());
                lore.add("§8(Requiert niveau 5)");
            }
        } else {
            lore.add("§8Obtenez ce pet pour activer");
        }

        return new ItemBuilder(Material.BREWING_STAND)
            .name("§7[Passif] §f" + type.getDisplayName())
            .lore(lore)
            .build();
    }

    private ItemStack createActiveItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§b" + type.getActiveName());
        lore.add("§f" + type.getActiveDescription());
        lore.add("");
        lore.add("§7Cooldown de base: §e" + type.getActiveCooldown() + "s");

        if (petData != null) {
            int adjustedCd = (int) (type.getActiveCooldown() * (1 - (petData.getLevel() - 1) * 0.02));
            lore.add("§7Cooldown actuel: §a" + adjustedCd + "s");
            lore.add("§8(-2% par niveau)");

            int remaining = plugin.getPetManager().getCooldownRemainingSeconds(player.getUniqueId(), type);
            if (remaining > 0) {
                lore.add("");
                lore.add("§c⏳ En cooldown: " + remaining + "s");
            } else if (type == playerData.getEquippedPet()) {
                lore.add("");
                lore.add("§a✓ Prêt à utiliser!");
                lore.add("§7Appuyez sur §eR §7ou cliquez!");
            }
        } else {
            lore.add("");
            lore.add("§8Obtenez ce pet pour utiliser");
        }

        return new ItemBuilder(Material.BLAZE_POWDER)
            .name("§b[Actif] §f" + type.getActiveName())
            .lore(lore)
            .build();
    }

    private ItemStack createStarPowerItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Star Power: Améliorations ultimes");
        lore.add("§7débloquées après le niveau 9.");
        lore.add("");

        boolean hasLevel9 = petData != null && petData.getLevel() >= 9;

        // SP1
        boolean hasSP1 = petData != null && petData.hasStarPower(1);
        lore.add((hasSP1 ? "§e★ " : "§8☆ ") + "Star Power 1");
        if (!hasLevel9) {
            lore.add("§8  Requiert niveau 9");
        } else if (!hasSP1) {
            int needed = type.getRarity().getCopiesForStarPower(1);
            lore.add("§7  Requiert " + needed + " copies supplémentaires");
        }

        // SP2
        boolean hasSP2 = petData != null && petData.hasStarPower(2);
        lore.add((hasSP2 ? "§e★★ " : "§8☆☆ ") + "Star Power 2");
        if (!hasSP1) {
            lore.add("§8  Requiert SP1");
        } else if (!hasSP2) {
            int needed = type.getRarity().getCopiesForStarPower(2);
            lore.add("§7  Requiert " + needed + " copies supplémentaires");
        }

        // SP3
        boolean hasSP3 = petData != null && petData.hasStarPower(3);
        lore.add((hasSP3 ? "§e★★★ " : "§8☆☆☆ ") + "Star Power 3");
        if (!hasSP2) {
            lore.add("§8  Requiert SP2");
        } else if (!hasSP3) {
            int needed = type.getRarity().getCopiesForStarPower(3);
            lore.add("§7  Requiert " + needed + " copies supplémentaires");
        }

        lore.add("");
        lore.add("§7═══ EFFET STAR POWER ═══");
        lore.add("§d" + type.getStarPowerDescription());

        return new ItemBuilder(Material.NETHER_STAR)
            .name("§e★ Star Powers")
            .lore(lore)
            .glow(petData != null && petData.getStarPower() > 0)
            .build();
    }

    private ItemStack createStatsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (petData != null) {
            lore.add("§7Dégâts infligés: §c" + formatNumber(petData.getTotalDamageDealt()));
            lore.add("§7Kills assistés: §a" + formatNumber(petData.getTotalKills()));
            lore.add("§7Fois équipé: §e" + petData.getTimesUsed());
            lore.add("§7Temps équipé: §b" + formatTime(petData.getTimeEquipped()));
            lore.add("");

            if (petData.getObtainedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                lore.add("§7Obtenu le: §f" + sdf.format(new Date(petData.getObtainedAt())));
            }
        } else {
            lore.add("§8Aucune statistique disponible");
        }

        return new ItemBuilder(Material.BOOK)
            .name("§6📊 Statistiques")
            .lore(lore)
            .build();
    }

    private String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        return String.format("%.1fM", number / 1000000.0);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Gestionnaire d'événements
     */
    public static class GUIListener implements Listener {

        private final ZombieZPlugin plugin;

        public GUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof PetDetailsGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            switch (slot) {
                case SLOT_BACK -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetCollectionGUI(gui.plugin, player, 0).open();
                }
                case SLOT_EQUIP -> {
                    if (gui.petData == null) return;

                    if (gui.type == gui.playerData.getEquippedPet()) {
                        gui.plugin.getPetManager().unequipPet(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                    } else {
                        gui.plugin.getPetManager().equipPet(player, gui.type);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                    new PetDetailsGUI(gui.plugin, player, gui.type).open();
                }
                case SLOT_FAVORITE -> {
                    if (gui.petData == null) return;

                    gui.petData.setFavorite(!gui.petData.isFavorite());
                    gui.playerData.markDirty();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    new PetDetailsGUI(gui.plugin, player, gui.type).open();
                }
                case SLOT_ACTIVE -> {
                    // Activer la capacité si c'est le pet équipé
                    if (gui.petData != null && gui.type == gui.playerData.getEquippedPet()) {
                        gui.plugin.getPetManager().activateAbility(player);
                    }
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof PetDetailsGUI) {
                event.setCancelled(true);
            }
        }
    }
}
