package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.*;
import com.rinaorc.zombiez.pets.eggs.EggType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal des Pets
 */
public class PetMainGUI implements InventoryHolder {

    private static final String TITLE = "§8§l🐾 Mes Compagnons";
    private static final int SIZE = 45;

    // Slots
    private static final int SLOT_EQUIPPED_PET = 13;
    private static final int SLOT_SHOP = 22;
    private static final int SLOT_COLLECTION = 29;
    private static final int SLOT_EGGS = 31;
    private static final int SLOT_OPTIONS = 33;
    private static final int SLOT_FRAGMENTS = 39;
    private static final int SLOT_STATS = 41;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;

    public PetMainGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        setupGUI();
    }

    private void setupGUI() {
        // Remplir le fond
        ItemStack filler = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Bordure décorative
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }

        // Pet équipé
        inventory.setItem(SLOT_EQUIPPED_PET, createEquippedPetItem());

        // Boutons de menu
        inventory.setItem(SLOT_COLLECTION, new ItemBuilder(Material.CHEST)
            .name("§e📦 Collection")
            .lore(
                "",
                "§7Voir tous vos pets",
                "§7Pets possédés: §a" + (petData != null ? petData.getPetCount() : 0) + "§7/§e" + PetType.values().length,
                "",
                "§eCliquez pour ouvrir"
            )
            .build());

        inventory.setItem(SLOT_EGGS, createEggsItem());

        inventory.setItem(SLOT_OPTIONS, new ItemBuilder(Material.COMPARATOR)
            .name("§6⚙ Options")
            .lore(
                "",
                "§7Gérer vos préférences",
                "",
                "§eCliquez pour ouvrir"
            )
            .build());

        // Boutique
        inventory.setItem(SLOT_SHOP, new ItemBuilder(Material.EMERALD)
            .name("§a💎 Boutique")
            .lore(
                "",
                "§7Achetez des oeufs et fragments",
                "§7avec vos points de jeu!",
                "",
                "§7Offres flash disponibles!",
                "",
                "§eCliquez pour ouvrir"
            )
            .glow(true)
            .build());

        // Fragments
        inventory.setItem(SLOT_FRAGMENTS, new ItemBuilder(Material.PRISMARINE_SHARD)
            .name("§d💎 Fragments")
            .lore(
                "",
                "§7Fragments de Pet: §d" + (petData != null ? petData.getFragments() : 0),
                "",
                "§7Utilisez les fragments pour",
                "§7acheter des copies de pets",
                "§7dans la boutique rotative."
            )
            .build());

        // Stats
        inventory.setItem(SLOT_STATS, createStatsItem());
    }

    private ItemStack createEquippedPetItem() {
        if (petData == null || petData.getEquippedPet() == null) {
            return new ItemBuilder(Material.BARRIER)
                .name("§c✖ Aucun Pet Équipé")
                .lore(
                    "",
                    "§7Ouvrez votre collection",
                    "§7pour équiper un pet!",
                    "",
                    "§e[📦 Collection]"
                )
                .build();
        }

        PetType type = petData.getEquippedPet();
        PetData pet = petData.getPet(type);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Niveau: §a" + pet.getLevel() + "§7/§e9");
        lore.add("§7Copies: §b" + pet.getCopies() + "§7/" + type.getRarity().getTotalCopiesForLevel(pet.getLevel() + 1));
        lore.add(pet.getProgressBar() + " §7" + String.format("%.1f", pet.getProgressPercent()) + "%");
        lore.add("");
        lore.add("§7Rareté: " + type.getRarity().getColoredName());
        lore.add("§7Thème: §f" + type.getTheme());
        lore.add("");
        lore.add("§7═══ CAPACITÉS ═══");
        lore.add("");
        lore.add("§7[Passif] §f" + type.getPassiveDescription());
        if (pet.hasLevel5Bonus()) {
            lore.add("§a[Passif Niv.5] §f" + type.getLevel5Bonus());
        }
        lore.add("");
        lore.add("§b[Actif] " + type.getActiveName());
        lore.add("§7" + type.getActiveDescription());
        lore.add("§7Cooldown: §e" + type.getActiveCooldown() + "s");
        lore.add("");

        int cooldownRemaining = plugin.getPetManager().getCooldownRemainingSeconds(player.getUniqueId(), type);
        if (cooldownRemaining > 0) {
            lore.add("§c⏳ Cooldown: " + cooldownRemaining + "s");
        } else {
            lore.add("§a✓ Capacité prête!");
        }

        lore.add("");
        lore.add("§eClic gauche: Activer capacité");
        lore.add("§eClic droit: Déséquiper");

        return new ItemBuilder(type.getIcon())
            .name(type.getColoredName() + " " + type.getRarity().getStars())
            .lore(lore)
            .glow(pet.hasEvolution())
            .build();
    }

    private ItemStack createEggsItem() {
        int totalEggs = petData != null ? petData.getTotalEggs() : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Oeufs disponibles: §e" + totalEggs);
        lore.add("");

        if (petData != null) {
            for (EggType type : EggType.values()) {
                int count = petData.getEggCount(type);
                if (count > 0) {
                    lore.add(type.getColoredName() + " §7x§e" + count);
                }
            }
        }

        if (totalEggs == 0) {
            lore.add("§8Aucun oeuf disponible");
        }

        lore.add("");
        lore.add("§eCliquez pour ouvrir un oeuf");

        return new ItemBuilder(Material.EGG)
            .name("§e🥚 Oeufs")
            .lore(lore)
            .build();
    }

    private ItemStack createStatsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (petData != null) {
            lore.add("§7Oeufs ouverts: §e" + petData.getTotalEggsOpened());
            lore.add("§7Légendaires obtenus: §6" + petData.getLegendariesObtained());
            lore.add("§7Mythiques obtenus: §c" + petData.getMythicsObtained());
            lore.add("");
            lore.add("§7Collection: §a" + String.format("%.1f", petData.getCollectionCompletion()) + "%");
            lore.add("§7Pets max level: §e" + petData.getMaxLevelPetCount());
        }

        return new ItemBuilder(Material.BOOK)
            .name("§6📊 Statistiques")
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
     * Gestionnaire d'événements pour le GUI
     */
    public static class GUIListener implements Listener {

        private final ZombieZPlugin plugin;

        public GUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof PetMainGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            switch (slot) {
                case SLOT_EQUIPPED_PET -> {
                    if (event.isLeftClick()) {
                        // Activer la capacité
                        plugin.getPetManager().activateAbility(player);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    } else if (event.isRightClick()) {
                        // Déséquiper
                        plugin.getPetManager().unequipPet(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                        new PetMainGUI(plugin, player).open();
                    }
                }
                case SLOT_COLLECTION -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetCollectionGUI(plugin, player, 0).open();
                }
                case SLOT_EGGS -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetEggGUI(plugin, player).open();
                }
                case SLOT_OPTIONS -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetOptionsGUI(gui.plugin, player).open();
                }
                case SLOT_SHOP -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new PetShopGUI(gui.plugin, player).open();
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof PetMainGUI) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            // Cleanup si nécessaire
        }
    }
}
