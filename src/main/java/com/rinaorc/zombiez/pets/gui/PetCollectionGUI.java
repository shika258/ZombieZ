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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI de la collection de pets
 */
public class PetCollectionGUI implements InventoryHolder {

    private static final String TITLE = "§8§l📦 Collection de Pets";
    private static final int SIZE = 54;
    private static final int PETS_PER_PAGE = 36;

    // Slots de navigation
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;
    private final int page;
    private final PetType[] allPets;

    public PetCollectionGUI(ZombieZPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.petData = plugin.getPetManager().getPlayerData(player.getUniqueId());

        // Trier les pets par rareté puis par nom
        this.allPets = Arrays.stream(PetType.values())
            .sorted((a, b) -> {
                int rarityCompare = a.getRarity().ordinal() - b.getRarity().ordinal();
                if (rarityCompare != 0) return rarityCompare;
                return a.getDisplayName().compareTo(b.getDisplayName());
            })
            .toArray(PetType[]::new);

        this.inventory = Bukkit.createInventory(this, SIZE, TITLE + " §7(Page " + (page + 1) + ")");
        setupGUI();
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Afficher les pets de cette page
        int startIndex = page * PETS_PER_PAGE;
        int endIndex = Math.min(startIndex + PETS_PER_PAGE, allPets.length);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            if (slot >= 36) break;

            // Ajuster le slot pour éviter la barre de navigation
            int row = slot / 9;
            int col = slot % 9;
            int actualSlot = row * 9 + col;

            inventory.setItem(actualSlot, createPetItem(allPets[i]));
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW)
                .name("§e◄ Page précédente")
                .build());
        }

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Retour")
            .build());

        if (endIndex < allPets.length) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                .name("§ePage suivante ►")
                .build());
        }

        // Info collection
        inventory.setItem(48, createCollectionInfoItem());
    }

    private ItemStack createPetItem(PetType type) {
        boolean owned = petData != null && petData.hasPet(type);

        if (!owned) {
            // Pet non possédé - silhouette
            return new ItemBuilder(Material.GRAY_DYE)
                .name("§8??? " + type.getRarity().getStars())
                .lore(
                    "",
                    "§7Rareté: " + type.getRarity().getColoredName(),
                    "",
                    "§8Pet non découvert",
                    "",
                    "§7Obtenez ce pet via",
                    "§7les oeufs de pet!"
                )
                .build();
        }

        PetData pet = petData.getPet(type);
        boolean isEquipped = type == petData.getEquippedPet();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Rareté: " + type.getRarity().getColoredName());
        lore.add("§7Thème: §f" + type.getTheme());
        lore.add("");
        lore.add("§7Niveau: §a" + pet.getLevel() + "§7/§e9");
        lore.add("§7Copies: §b" + pet.getCopies());
        lore.add(pet.getProgressBar());
        lore.add("");
        lore.add("§7[Passif] §f" + type.getPassiveDescription());
        if (pet.hasLevel5Bonus()) {
            lore.add("§a[+Niv.5] §f" + type.getLevel5Bonus());
        }
        lore.add("");
        lore.add("§b[Actif] " + type.getActiveName());
        lore.add("§7" + type.getActiveDescription());

        if (pet.getStarPower() > 0) {
            lore.add("");
            lore.add("§e★ Star Power: " + pet.getStarPower());
        }

        lore.add("");
        if (isEquipped) {
            lore.add("§a✓ Actuellement équipé");
        } else {
            lore.add("§eCliquez pour équiper");
        }
        lore.add("§6Clic droit: Voir détails");

        String stars = pet.getStarPower() > 0 ? " §e" + "★".repeat(pet.getStarPower()) : "";

        return new ItemBuilder(type.getIcon())
            .name(type.getColoredName() + " §7[Lv." + pet.getLevel() + "]" + stars)
            .lore(lore)
            .glow(isEquipped || pet.hasEvolution())
            .build();
    }

    private ItemStack createCollectionInfoItem() {
        int owned = petData != null ? petData.getPetCount() : 0;
        int total = PetType.values().length;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Progression: §a" + owned + "§7/§e" + total);
        lore.add("§7Complétion: §a" + String.format("%.1f", (owned * 100.0 / total)) + "%");
        lore.add("");

        // Compter par rareté
        for (PetRarity rarity : PetRarity.values()) {
            int ownedOfRarity = petData != null ? petData.getPetCountByRarity(rarity) : 0;
            int totalOfRarity = PetType.getByRarity(rarity).length;
            lore.add(rarity.getColoredName() + "§7: §a" + ownedOfRarity + "§7/§e" + totalOfRarity);
        }

        return new ItemBuilder(Material.BOOK)
            .name("§6📊 Collection")
            .lore(lore)
            .build();
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
            if (!(event.getInventory().getHolder() instanceof PetCollectionGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // Navigation
            if (slot == SLOT_PREV && gui.page > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetCollectionGUI(gui.plugin, player, gui.page - 1).open();
                return;
            }

            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            if (slot == SLOT_NEXT) {
                int maxPage = (gui.allPets.length - 1) / PETS_PER_PAGE;
                if (gui.page < maxPage) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetCollectionGUI(gui.plugin, player, gui.page + 1).open();
                }
                return;
            }

            // Clic sur un pet
            if (slot < 36) {
                int index = gui.page * PETS_PER_PAGE + slot;
                if (index >= gui.allPets.length) return;

                PetType type = gui.allPets[index];
                PlayerPetData petData = gui.petData;

                if (petData == null || !petData.hasPet(type)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                if (event.isLeftClick()) {
                    // Équiper le pet
                    gui.plugin.getPetManager().equipPet(player, type);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    new PetCollectionGUI(gui.plugin, player, gui.page).open();
                } else if (event.isRightClick()) {
                    // Voir les détails
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetDetailsGUI(gui.plugin, player, type).open();
                }
            }
        }
    }
}
