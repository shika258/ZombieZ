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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI de la collection de pets avec filtres par rareté
 */
public class PetCollectionGUI implements InventoryHolder {

    private static final int SIZE = 54;
    private static final int PETS_PER_PAGE = 28; // 7x4 grid

    // Slots pour les filtres de rareté (ligne du haut)
    private static final int SLOT_FILTER_ALL = 0;
    private static final int[] SLOT_FILTERS = { 1, 2, 3, 4, 5, 6 }; // COMMON to MYTHIC

    // Slots de navigation (ligne du bas)
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_BACK = 47;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_SORT = 51;

    // Zone d'affichage des pets: slots 9-17, 18-26, 27-35, 36-44 (4 rows x 7 cols)
    private static final int[] PET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;
    private final int page;
    private final PetRarity filterRarity; // null = tous
    private final boolean showOwnedOnly;
    private final PetType[] filteredPets;

    public PetCollectionGUI(ZombieZPlugin plugin, Player player, int page, PetRarity filter, boolean ownedOnly) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        this.filterRarity = filter;
        this.showOwnedOnly = ownedOnly;

        // Filtrer et trier les pets
        this.filteredPets = Arrays.stream(PetType.values())
                .filter(p -> filter == null || p.getRarity() == filter)
                .filter(p -> !ownedOnly || (petData != null && petData.hasPet(p)))
                .sorted((a, b) -> {
                    int rarityCompare = a.getRarity().ordinal() - b.getRarity().ordinal();
                    if (rarityCompare != 0)
                        return rarityCompare;
                    return a.getDisplayName().compareTo(b.getDisplayName());
                })
                .toArray(PetType[]::new);

        String title = "§8§l📦 Collection" + (filter != null ? " §7[" + filter.getColoredName() + "§7]" : "");
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        setupGUI();
    }

    // Constructeur simplifié
    public PetCollectionGUI(ZombieZPlugin plugin, Player player, int page) {
        this(plugin, player, page, null, false);
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordures latérales
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border); // Gauche
            inventory.setItem(row * 9 + 8, border); // Droite
        }

        // Filtres de rareté
        setupFilters();

        // Afficher les pets
        displayPets();

        // Navigation
        setupNavigation();
    }

    private void setupFilters() {
        // Bouton "Tous"
        boolean allSelected = filterRarity == null;
        inventory.setItem(SLOT_FILTER_ALL, new ItemBuilder(allSelected ? Material.NETHER_STAR : Material.GRAY_DYE)
                .name((allSelected ? "§a► " : "§7") + "Tous les Pets")
                .lore(
                        "",
                        "§7Afficher tous les pets",
                        "§7Total: §e" + PetType.values().length,
                        "",
                        allSelected ? "§a✓ Sélectionné" : "§eCliquez pour filtrer")
                .glow(allSelected)
                .build());

        // Filtres par rareté
        PetRarity[] rarities = PetRarity.values();
        for (int i = 0; i < rarities.length && i < SLOT_FILTERS.length; i++) {
            PetRarity rarity = rarities[i];
            boolean selected = filterRarity == rarity;
            int ownedCount = petData != null ? petData.getPetCountByRarity(rarity) : 0;
            int totalCount = PetType.getByRarity(rarity).length;

            Material mat = switch (rarity) {
                case COMMON -> Material.WHITE_DYE;
                case UNCOMMON -> Material.LIME_DYE;
                case RARE -> Material.CYAN_DYE;
                case EPIC -> Material.PURPLE_DYE;
                case LEGENDARY -> Material.ORANGE_DYE;
                case MYTHIC -> Material.MAGENTA_DYE;
                case EXALTED -> Material.RED_DYE;
                default -> Material.GRAY_DYE;
            };

            inventory.setItem(SLOT_FILTERS[i], new ItemBuilder(mat)
                    .name((selected ? "§a► " : "") + rarity.getColoredName())
                    .lore(
                            "",
                            "§7Possédés: §a" + ownedCount + "§7/§e" + totalCount,
                            "",
                            selected ? "§a✓ Sélectionné" : "§eCliquez pour filtrer")
                    .glow(selected)
                    .build());
        }

        // Bouton toggle "Owned only"
        inventory.setItem(8, new ItemBuilder(showOwnedOnly ? Material.ENDER_EYE : Material.ENDER_PEARL)
                .name(showOwnedOnly ? "§a✓ Possédés uniquement" : "§7☐ Possédés uniquement")
                .lore(
                        "",
                        "§7Afficher uniquement les pets",
                        "§7que vous possédez.",
                        "",
                        showOwnedOnly ? "§eCliquez pour voir tous" : "§eCliquez pour activer")
                .glow(showOwnedOnly)
                .build());
    }

    private void displayPets() {
        int startIndex = page * PETS_PER_PAGE;
        int endIndex = Math.min(startIndex + PETS_PER_PAGE, filteredPets.length);

        for (int i = 0; i < PETS_PER_PAGE; i++) {
            int petIndex = startIndex + i;
            int slot = PET_SLOTS[i];

            if (petIndex < endIndex) {
                inventory.setItem(slot, createPetItem(filteredPets[petIndex]));
            } else {
                inventory.setItem(slot, ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    private void setupNavigation() {
        int totalPages = Math.max(1, (int) Math.ceil(filteredPets.length / (double) PETS_PER_PAGE));

        // Page précédente
        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW)
                    .name("§e◄ Page " + page)
                    .lore("", "§7Page actuelle: §a" + (page + 1) + "§7/§e" + totalPages)
                    .build());
        } else {
            inventory.setItem(SLOT_PREV, ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Info collection
        inventory.setItem(SLOT_INFO, createCollectionInfoItem());

        // Retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .name("§c✖ Retour au menu")
                .build());

        // Page suivante
        if ((page + 1) * PETS_PER_PAGE < filteredPets.length) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .name("§ePage " + (page + 2) + " ►")
                    .lore("", "§7Page actuelle: §a" + (page + 1) + "§7/§e" + totalPages)
                    .build());
        } else {
            inventory.setItem(SLOT_NEXT, ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE));
        }
    }

    private ItemStack createPetItem(PetType type) {
        boolean owned = petData != null && petData.hasPet(type);

        if (!owned) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .name("§8??? " + type.getRarity().getStars())
                    .lore(
                            "",
                            "§7Rareté: " + type.getRarity().getColoredName(),
                            "§7Thème: §8" + type.getTheme(),
                            "",
                            "§8╔══════════════════════╗",
                            "§8║  §7Pet non découvert  §8║",
                            "§8╚══════════════════════╝",
                            "",
                            "§8Les capacités de ce pet",
                            "§8restent un mystère...",
                            "",
                            "§7Obtenez ce pet via les",
                            "§7oeufs pour le découvrir!")
                    .build();
        }

        PetData pet = petData.getPet(type);
        boolean isEquipped = type == petData.getEquippedPet();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Rareté: " + type.getRarity().getColoredName());
        lore.add("§7Thème: §f" + type.getTheme());
        lore.add("");
        lore.add("§7Niveau: §a" + pet.getLevel() + "§7/§e9 " + pet.getProgressBar());
        lore.add("§7Copies: §b" + pet.getCopies() + "§7/§e" + type.getRarity().getCopiesForMax());
        lore.add("");
        // Capacité passive avec description complète
        lore.add("§7═══ CAPACITÉS ═══");
        lore.add("");
        lore.add("§7[Passif] §f" + type.getDisplayName());
        for (String line : wrapText(type.getPassiveDescription(), 40)) {
            lore.add("§7  " + line);
        }
        if (pet.hasLevel5Bonus()) {
            lore.add("§a[Bonus Niv.5]");
            for (String line : wrapText(type.getLevel5Bonus(), 40)) {
                lore.add("§a  " + line);
            }
        }
        lore.add("");
        // Capacité ultime avec description complète
        lore.add("§6[Ultime] §e" + type.getUltimateName());
        for (String line : wrapText(type.getUltimateDescription(), 40)) {
            lore.add("§7  " + line);
        }
        lore.add("§7  Auto-activation: §e" + type.getUltimateCooldown() + "s");

        if (pet.getStarPower() > 0) {
            lore.add("");
            lore.add("§e★ Star Power: " + pet.getStarPower() + "/3");
        }

        lore.add("");
        if (isEquipped) {
            lore.add("§a✓ Actuellement équipé");
        } else {
            lore.add("§eClic gauche: Équiper");
        }
        lore.add("§6Clic droit: Détails");

        String stars = pet.getStarPower() > 0 ? " §e" + "★".repeat(pet.getStarPower()) : "";

        return new ItemBuilder(type.getIcon())
                .name(type.getColoredName() + " §7[Lv." + pet.getLevel() + "]" + stars)
                .lore(lore)
                .glow(isEquipped || pet.hasEvolution())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Découpe un texte en plusieurs lignes pour l'affichage
     */
    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("§8Aucune description");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }

    private ItemStack createCollectionInfoItem() {
        int owned = petData != null ? petData.getPetCount() : 0;
        int total = PetType.values().length;
        int filtered = filteredPets.length;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Collection totale: §a" + owned + "§7/§e" + total + " §7("
                + String.format("%.1f", (owned * 100.0 / total)) + "%)");
        lore.add("");
        if (filterRarity != null) {
            int ownedFilter = petData != null ? petData.getPetCountByRarity(filterRarity) : 0;
            lore.add("§7Filtre actif: " + filterRarity.getColoredName());
            lore.add("§7Possédés: §a" + ownedFilter + "§7/§e" + filtered);
        } else {
            lore.add("§7Par rareté:");
            for (PetRarity rarity : PetRarity.values()) {
                int ownedR = petData != null ? petData.getPetCountByRarity(rarity) : 0;
                int totalR = PetType.getByRarity(rarity).length;
                String bar = createMiniBar(ownedR, totalR);
                lore.add("  " + rarity.getColoredName() + " " + bar + " §a" + ownedR + "§7/§e" + totalR);
            }
        }

        return new ItemBuilder(Material.BOOK)
                .name("§6📊 Statistiques Collection")
                .lore(lore)
                .build();
    }

    private String createMiniBar(int current, int max) {
        if (max == 0)
            return "§8▌▌▌▌▌";
        int filled = Math.min(5, (int) ((current * 5.0) / max));
        int empty = 5 - filled;
        return "§a" + "▌".repeat(filled) + "§8" + "▌".repeat(empty);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // Getters pour le listener
    public int getPage() {
        return page;
    }

    public PetRarity getFilterRarity() {
        return filterRarity;
    }

    public boolean isShowOwnedOnly() {
        return showOwnedOnly;
    }

    public PetType[] getFilteredPets() {
        return filteredPets;
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
            if (!(event.getInventory().getHolder() instanceof PetCollectionGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Filtre "Tous"
            if (slot == SLOT_FILTER_ALL) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetCollectionGUI(gui.plugin, player, 0, null, gui.showOwnedOnly).open();
                return;
            }

            // Filtres de rareté
            for (int i = 0; i < SLOT_FILTERS.length; i++) {
                if (slot == SLOT_FILTERS[i]) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetCollectionGUI(gui.plugin, player, 0, PetRarity.values()[i], gui.showOwnedOnly).open();
                    return;
                }
            }

            // Toggle owned only
            if (slot == 8) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetCollectionGUI(gui.plugin, player, 0, gui.filterRarity, !gui.showOwnedOnly).open();
                return;
            }

            // Navigation
            if (slot == SLOT_PREV && gui.page > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetCollectionGUI(gui.plugin, player, gui.page - 1, gui.filterRarity, gui.showOwnedOnly).open();
                return;
            }

            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            if (slot == SLOT_NEXT) {
                int maxPage = Math.max(0, (gui.filteredPets.length - 1) / PETS_PER_PAGE);
                if (gui.page < maxPage) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetCollectionGUI(gui.plugin, player, gui.page + 1, gui.filterRarity, gui.showOwnedOnly).open();
                }
                return;
            }

            // Clic sur un pet
            for (int i = 0; i < PET_SLOTS.length; i++) {
                if (slot == PET_SLOTS[i]) {
                    int index = gui.page * PETS_PER_PAGE + i;
                    if (index >= gui.filteredPets.length)
                        return;

                    PetType type = gui.filteredPets[index];
                    PlayerPetData petData = gui.petData;

                    if (petData == null || !petData.hasPet(type)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    if (event.isLeftClick()) {
                        gui.plugin.getPetManager().equipPet(player, type);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        new PetCollectionGUI(gui.plugin, player, gui.page, gui.filterRarity, gui.showOwnedOnly).open();
                    } else if (event.isRightClick()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        new PetDetailsGUI(gui.plugin, player, type).open();
                    }
                    return;
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof PetCollectionGUI) {
                event.setCancelled(true);
            }
        }
    }
}
