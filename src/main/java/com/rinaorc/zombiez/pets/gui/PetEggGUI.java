package com.rinaorc.zombiez.pets.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PetRarity;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.eggs.EggType;
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
import java.util.List;

/**
 * GUI pour ouvrir les oeufs de pet
 */
public class PetEggGUI implements InventoryHolder {

    private static final String TITLE = "§8§l🥚 Oeufs de Pet";
    private static final int SIZE = 45;

    // Slots pour chaque type d'oeuf
    private static final int[] EGG_SLOTS = {10, 12, 14, 16, 22};
    private static final int SLOT_BACK = 40;

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final PlayerPetData petData;

    public PetEggGUI(ZombieZPlugin plugin, Player player) {
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

        // Bordure
        ItemStack border = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(SIZE - 9 + i, border);
        }

        // Afficher les oeufs
        EggType[] types = EggType.values();
        for (int i = 0; i < types.length && i < EGG_SLOTS.length; i++) {
            inventory.setItem(EGG_SLOTS[i], createEggItem(types[i]));
        }

        // Pity info
        inventory.setItem(31, createPityInfoItem());

        // Retour
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
            .name("§c◄ Retour")
            .build());
    }

    private ItemStack createEggItem(EggType type) {
        int count = petData != null ? petData.getEggCount(type) : 0;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Quantité: " + (count > 0 ? "§a" + count : "§c0"));
        lore.add("");
        lore.add("§7Contenu possible:");

        // Afficher les taux de drop
        PetRarity[] rarities = PetRarity.values();
        double[] rates = type.getRarityRates();
        for (int i = 0; i < rarities.length; i++) {
            if (rates[i] > 0) {
                lore.add("  " + rarities[i].getColoredName() + "§7: §e" + rates[i] + "%");
            }
        }

        if (type.getMinimumRarity() != null) {
            lore.add("");
            lore.add("§a✓ Garanti: " + type.getMinimumRarity().getColoredName() + "§a minimum");
        }

        lore.add("");
        if (count > 0) {
            lore.add("§eCliquez pour ouvrir!");
            lore.add("§eShift+Clic: Ouvrir x10");
        } else {
            lore.add("§8Aucun oeuf disponible");
        }

        Material icon = count > 0 ? type.getIcon() : Material.GRAY_DYE;

        return new ItemBuilder(icon)
            .name(type.getColoredName() + (count > 0 ? " §7x" + count : ""))
            .lore(lore)
            .glow(count > 0)
            .build();
    }

    private ItemStack createPityInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Le système Pity vous garantit");
        lore.add("§7une rareté minimum après un");
        lore.add("§7certain nombre d'oeufs ouverts.");
        lore.add("");

        if (petData != null) {
            // Oeuf Standard
            int standardPity = petData.getPityCounter(EggType.STANDARD);
            lore.add("§f◆ Oeuf Standard:");
            lore.add("  " + createPityBar(standardPity, 50) + " §b" + standardPity + "§7/§b50 §7→ Rare");
            lore.add("  " + createPityBar(standardPity, 100) + " §d" + standardPity + "§7/§d100 §7→ Épique");
            lore.add("  " + createPityBar(standardPity, 200) + " §6" + standardPity + "§7/§6200 §7→ Légendaire");

            // Oeuf Zone
            int zonePity = petData.getPityCounter(EggType.ZONE);
            lore.add("");
            lore.add("§e◆ Oeuf de Zone:");
            lore.add("  " + createPityBar(zonePity, 30) + " §d" + zonePity + "§7/§d30 §7→ Épique");
            lore.add("  " + createPityBar(zonePity, 75) + " §6" + zonePity + "§7/§675 §7→ Légendaire");

            // Oeuf Élite
            int elitePity = petData.getPityCounter(EggType.ELITE);
            lore.add("");
            lore.add("§d◆ Oeuf Élite:");
            lore.add("  " + createPityBar(elitePity, 20) + " §6" + elitePity + "§7/§620 §7→ Légendaire");
            lore.add("  " + createPityBar(elitePity, 50) + " §c" + elitePity + "§7/§c50 §7→ Mythique");

            // Oeuf Légendaire
            int legendaryPity = petData.getPityCounter(EggType.LEGENDARY);
            lore.add("");
            lore.add("§6◆ Oeuf Légendaire:");
            lore.add("  " + createPityBar(legendaryPity, 25) + " §c" + legendaryPity + "§7/§c25 §7→ Mythique");

            lore.add("");
            lore.add("§8Le pity se réinitialise quand vous");
            lore.add("§8obtenez la rareté garantie ou mieux.");
        }

        return new ItemBuilder(Material.KNOWLEDGE_BOOK)
            .name("§6📖 Système Pity")
            .lore(lore)
            .build();
    }

    /**
     * Crée une barre de progression visuelle pour le pity
     */
    private String createPityBar(int current, int max) {
        int progress = Math.min(10, (int) ((current * 10.0) / max));
        int remaining = 10 - progress;
        String color = progress >= 8 ? "§a" : (progress >= 5 ? "§e" : "§7");
        return color + "▌".repeat(progress) + "§8" + "▌".repeat(remaining);
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
            if (!(event.getInventory().getHolder() instanceof PetEggGUI gui)) {
                return;
            }

            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == SLOT_BACK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new PetMainGUI(gui.plugin, player).open();
                return;
            }

            // Chercher quel oeuf a été cliqué
            EggType[] types = EggType.values();
            for (int i = 0; i < types.length && i < EGG_SLOTS.length; i++) {
                if (slot == EGG_SLOTS[i]) {
                    EggType eggType = types[i];
                    int count = gui.petData != null ? gui.petData.getEggCount(eggType) : 0;

                    if (count <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendMessage("§c[Pet] §7Vous n'avez pas d'oeuf de ce type!");
                        return;
                    }

                    int toOpen = event.isShiftClick() ? Math.min(10, count) : 1;

                    // Utiliser la nouvelle animation satisfaisante
                    player.closeInventory();
                    new EggOpeningAnimation(gui.plugin, player, eggType, toOpen).open();

                    return;
                }
            }
        }
    }
}
