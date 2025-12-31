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
    private static final int SIZE = 54;

    // Slots - Layout 6 lignes centré
    private static final int SLOT_EQUIPPED_PET = 13;     // Centre ligne 2 (remonté d'une ligne)
    private static final int SLOT_COLLECTION = 29;       // Ligne 4 gauche
    private static final int SLOT_SHOP = 31;             // Ligne 4 centre
    private static final int SLOT_EGGS = 33;             // Ligne 4 droite
    private static final int SLOT_OPTIONS = 40;          // Ligne 5 centre-gauche
    private static final int SLOT_FRAGMENTS = 38;        // Ligne 5 gauche
    private static final int SLOT_STATS = 42;            // Ligne 5 centre-droite

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
        // === LIGNE 0 : HEADER (BLACK) ===
        ItemStack headerGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, headerGlass);
        }

        // === LIGNES 1-4 : CENTRE (GRAY) ===
        ItemStack centerGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 9; i < 45; i++) {
            inventory.setItem(i, centerGlass);
        }

        // === LIGNE 5 : FOOTER (BLACK) ===
        ItemStack footerGlass = ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, footerGlass);
        }

        // === ZONE DU PET : Entouré de LIME ===
        ItemStack limeGlass = ItemBuilder.placeholder(Material.LIME_STAINED_GLASS_PANE);
        inventory.setItem(4, limeGlass);   // Au dessus
        inventory.setItem(12, limeGlass);  // À gauche
        inventory.setItem(14, limeGlass);  // À droite
        inventory.setItem(22, limeGlass);  // En dessous

        // Pet équipé au centre (slot 13)
        inventory.setItem(SLOT_EQUIPPED_PET, createEquippedPetItem());

        // === BOUTONS DE MENU ===
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
        lore.add("§6§l[ULTIME] §e" + type.getUltimateName());
        lore.add("§7" + type.getUltimateDescription());
        lore.add("§7S'active automatiquement toutes les §e" + type.getUltimateCooldown() + "s");
        lore.add("");

        int cooldownRemaining = plugin.getPetManager().getCooldownRemainingSeconds(player.getUniqueId(), type);
        if (cooldownRemaining > 0) {
            lore.add("§7Prochaine activation: §e" + cooldownRemaining + "s");
        } else {
            lore.add("§a✓ Ultime prête!");
        }

        lore.add("");
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

        if (petData != null) {
            // Section Collection
            lore.add("");
            lore.add("§e§l═══ COLLECTION ═══");
            int totalPets = PetType.values().length;
            int ownedPets = petData.getPetCount();
            lore.add("§7Progression: §a" + ownedPets + "§7/§e" + totalPets + " §7(" + String.format("%.1f", petData.getCollectionCompletion()) + "%)");
            lore.add(createProgressBar(ownedPets, totalPets, 20));

            // Répartition par rareté
            lore.add("§7Par rareté:");
            for (PetRarity rarity : PetRarity.values()) {
                int owned = petData.getPetCountByRarity(rarity);
                int total = PetType.getByRarity(rarity).length;
                String bar = createMiniBar(owned, total);
                lore.add("  " + rarity.getColoredName() + " " + bar + " §7" + owned + "/" + total);
            }

            // Section Oeufs
            lore.add("");
            lore.add("§b§l═══ OEUFS ═══");
            lore.add("§7Oeufs ouverts: §e" + formatNumber(petData.getTotalEggsOpened()));
            lore.add("§7Légendaires: §6" + petData.getLegendariesObtained() + " §7| Mythiques: §d" + petData.getMythicsObtained() + " §7| Exaltés: §c" + petData.getExaltedObtained());
            lore.add("§7Fragments gagnés: §d" + formatNumber(petData.getTotalFragmentsEarned()));

            // Calculer les totaux de tous les pets
            long totalDamage = 0;
            long totalKills = 0;
            long totalTimeEquipped = 0;
            int totalCopies = 0;
            PetType mostUsedPet = null;
            int maxTimesUsed = 0;
            PetType strongestPet = null;
            long maxDamage = 0;

            for (PetType type : PetType.values()) {
                if (petData.hasPet(type)) {
                    PetData pet = petData.getPet(type);
                    totalDamage += pet.getTotalDamageDealt();
                    totalKills += pet.getTotalKills();
                    totalTimeEquipped += pet.getTimeEquipped();
                    totalCopies += pet.getCopies();

                    if (pet.getTimesUsed() > maxTimesUsed) {
                        maxTimesUsed = pet.getTimesUsed();
                        mostUsedPet = type;
                    }
                    if (pet.getTotalDamageDealt() > maxDamage) {
                        maxDamage = pet.getTotalDamageDealt();
                        strongestPet = type;
                    }
                }
            }

            // Section Performance
            lore.add("");
            lore.add("§c§l═══ COMBAT ═══");
            lore.add("§7Dégâts totaux: §c" + formatNumber(totalDamage));
            lore.add("§7Kills assistés: §a" + formatNumber(totalKills));
            lore.add("§7Temps équipé: §b" + formatTime(totalTimeEquipped));
            lore.add("§7Copies totales: §e" + totalCopies);

            // Section Records
            lore.add("");
            lore.add("§6§l═══ RECORDS ═══");
            lore.add("§7Pets niveau max: §e" + petData.getMaxLevelPetCount() + "§7/§e" + ownedPets);
            if (mostUsedPet != null) {
                lore.add("§7Pet favori: " + mostUsedPet.getColoredName());
            }
            if (strongestPet != null && maxDamage > 0) {
                lore.add("§7Plus fort: " + strongestPet.getColoredName() + " §7(" + formatNumber(maxDamage) + " dégâts)");
            }
        } else {
            lore.add("");
            lore.add("§8Aucune statistique disponible");
        }

        return new ItemBuilder(Material.BOOK)
            .name("§6📊 Statistiques")
            .lore(lore)
            .build();
    }

    private String createProgressBar(int current, int max, int length) {
        if (max == 0) return "§8" + "▌".repeat(length);
        int filled = Math.min(length, (int) ((current * (double) length) / max));
        int empty = length - filled;
        return "§a" + "▌".repeat(filled) + "§8" + "▌".repeat(empty);
    }

    private String createMiniBar(int current, int max) {
        if (max == 0) return "§8▌▌▌▌▌";
        int filled = Math.min(5, (int) ((current * 5.0) / max));
        int empty = 5 - filled;
        return "§a" + "▌".repeat(filled) + "§8" + "▌".repeat(empty);
    }

    private String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fB", number / 1000000000.0);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "j " + (hours % 24) + "h";
        }
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
                    if (event.isRightClick()) {
                        // Déséquiper
                        plugin.getPetManager().unequipPet(player);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
                        new PetMainGUI(plugin, player).open();
                    } else {
                        // Informer que les ultimes s'activent automatiquement
                        player.sendMessage("§6[Pet] §7L'ultime s'active §eautomatiquement§7! Pas besoin de cliquer.");
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
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
