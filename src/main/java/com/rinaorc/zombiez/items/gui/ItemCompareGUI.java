package com.rinaorc.zombiez.items.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.forge.ForgeManager;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.HashMap;

/**
 * GUI de comparaison entre deux items
 */
public class ItemCompareGUI implements InventoryHolder, Listener {

    private static final String TITLE = "§8Comparaison d'Items";
    private static final int SIZE = 45;
    
    // Slots
    private static final int SLOT_ITEM_1 = 11;
    private static final int SLOT_ITEM_2 = 15;
    private static final int SLOT_STATS_START = 28;
    
    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    private ZombieZItem item1;
    private ZombieZItem item2;
    private int forgeLevel1;
    private int forgeLevel2;

    public ItemCompareGUI(ZombieZPlugin plugin, Player player, ZombieZItem item1, ZombieZItem item2) {
        this(plugin, player, item1, item2, 0, 0);
    }

    public ItemCompareGUI(ZombieZPlugin plugin, Player player, ZombieZItem item1, ZombieZItem item2,
                          int forgeLevel1, int forgeLevel2) {
        this.plugin = plugin;
        this.player = player;
        this.item1 = item1;
        this.item2 = item2;
        this.forgeLevel1 = forgeLevel1;
        this.forgeLevel2 = forgeLevel2;

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
        
        // Item 1
        if (item1 != null) {
            inventory.setItem(SLOT_ITEM_1, item1.toItemStack());
            
            // Score Item 1
            inventory.setItem(SLOT_ITEM_1 + 9, createScoreItem(item1, true));
        } else {
            inventory.setItem(SLOT_ITEM_1, createEmptySlot("Item 1"));
        }
        
        // Item 2
        if (item2 != null) {
            inventory.setItem(SLOT_ITEM_2, item2.toItemStack());
            
            // Score Item 2
            inventory.setItem(SLOT_ITEM_2 + 9, createScoreItem(item2, false));
        } else {
            inventory.setItem(SLOT_ITEM_2, createEmptySlot("Item 2"));
        }
        
        // Flèche de comparaison
        inventory.setItem(13, createComparisonArrow());
        
        // Stats comparison
        if (item1 != null && item2 != null) {
            displayStatComparison();
        }
        
        // Bouton fermer
        inventory.setItem(40, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Fermer")
            .build());
    }

    private ItemStack createEmptySlot(String name) {
        return new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            .name("§7" + name)
            .lore("§8Placez un item ici")
            .build();
    }

    private ItemStack createScoreItem(ZombieZItem item, boolean isLeft) {
        int baseScore = item.getItemScore();
        int forgeLevel = isLeft ? forgeLevel1 : forgeLevel2;

        // Appliquer le bonus de forge au score
        ForgeManager forgeManager = plugin.getForgeManager();
        double multiplier = 1.0 + (forgeManager.getStatBonus(forgeLevel) / 100.0);
        int score = (int) (baseScore * multiplier);

        String comparison = "";

        if (item1 != null && item2 != null) {
            int otherBaseScore = isLeft ? item2.getItemScore() : item1.getItemScore();
            int otherForgeLevel = isLeft ? forgeLevel2 : forgeLevel1;
            double otherMultiplier = 1.0 + (forgeManager.getStatBonus(otherForgeLevel) / 100.0);
            int otherScore = (int) (otherBaseScore * otherMultiplier);
            int diff = score - otherScore;

            if (diff > 0) {
                comparison = " §a(+" + diff + ")";
            } else if (diff < 0) {
                comparison = " §c(" + diff + ")";
            } else {
                comparison = " §7(=)";
            }
        }

        String forgeLevelStr = forgeLevel > 0 ? " §e+" + forgeLevel : "";

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§6Item Score: §f" + score + comparison)
            .lore(
                "",
                "§7Rareté: " + item.getRarity().getColoredName(),
                "§7Zone: §e" + item.getZoneLevel(),
                "§7Forge: " + (forgeLevel > 0 ? "§a+" + forgeLevel + " §7(+" + forgeManager.getStatBonus(forgeLevel) + "%)" : "§7Aucun"),
                "§7Affixes: §b" + item.getAffixes().size()
            )
            .build();
    }

    private ItemStack createComparisonArrow() {
        List<String> lore = new ArrayList<>();

        if (item1 != null && item2 != null) {
            // Calculer les scores avec le multiplicateur de forge
            ForgeManager forgeManager = plugin.getForgeManager();
            double multiplier1 = 1.0 + (forgeManager.getStatBonus(forgeLevel1) / 100.0);
            double multiplier2 = 1.0 + (forgeManager.getStatBonus(forgeLevel2) / 100.0);

            int score1 = (int) (item1.getItemScore() * multiplier1);
            int score2 = (int) (item2.getItemScore() * multiplier2);

            if (score1 > score2) {
                lore.add("§a◄ Item 1 est meilleur");
                lore.add("§7+" + (score1 - score2) + " Item Score");
            } else if (score2 > score1) {
                lore.add("§aItem 2 est meilleur ►");
                lore.add("§7+" + (score2 - score1) + " Item Score");
            } else {
                lore.add("§eLes deux items sont équivalents");
            }

            // Afficher les niveaux de forge si différents de 0
            if (forgeLevel1 > 0 || forgeLevel2 > 0) {
                lore.add("");
                lore.add("§8Forge: +" + forgeLevel1 + " vs +" + forgeLevel2);
            }
        } else {
            lore.add("§7Placez deux items pour comparer");
        }

        return new ItemBuilder(Material.ARROW)
            .name("§e⟷ Comparaison")
            .lore(lore)
            .build();
    }

    private void displayStatComparison() {
        Map<StatType, Double> stats1 = item1.getTotalStats();
        Map<StatType, Double> stats2 = item2.getTotalStats();

        // Appliquer le multiplicateur de forge aux stats
        ForgeManager forgeManager = plugin.getForgeManager();
        double multiplier1 = 1.0 + (forgeManager.getStatBonus(forgeLevel1) / 100.0);
        double multiplier2 = 1.0 + (forgeManager.getStatBonus(forgeLevel2) / 100.0);

        Map<StatType, Double> forgedStats1 = new HashMap<>();
        Map<StatType, Double> forgedStats2 = new HashMap<>();

        for (var entry : stats1.entrySet()) {
            forgedStats1.put(entry.getKey(), entry.getValue() * multiplier1);
        }
        for (var entry : stats2.entrySet()) {
            forgedStats2.put(entry.getKey(), entry.getValue() * multiplier2);
        }

        // Fusionner toutes les stats
        Set<StatType> allStats = new HashSet<>();
        allStats.addAll(forgedStats1.keySet());
        allStats.addAll(forgedStats2.keySet());

        // Trier par catégorie
        List<StatType> sortedStats = allStats.stream()
            .sorted(Comparator.comparing(StatType::getCategory))
            .toList();

        int slot = SLOT_STATS_START;
        for (StatType stat : sortedStats) {
            if (slot >= SIZE - 9) break;

            double value1 = forgedStats1.getOrDefault(stat, 0.0);
            double value2 = forgedStats2.getOrDefault(stat, 0.0);

            inventory.setItem(slot, createStatCompareItem(stat, value1, value2));
            slot++;
        }
    }

    private ItemStack createStatCompareItem(StatType stat, double value1, double value2) {
        Material material = switch (stat.getCategory()) {
            case OFFENSIVE -> Material.IRON_SWORD;
            case DEFENSIVE -> Material.IRON_CHESTPLATE;
            case ELEMENTAL -> Material.BLAZE_POWDER;
            case RESISTANCE -> Material.SHIELD;
            case UTILITY -> Material.FEATHER;
            default -> Material.BOOK;
        };
        
        String v1Str = stat.formatValue(value1);
        String v2Str = stat.formatValue(value2);
        
        String comparison;
        if (value1 > value2) {
            comparison = "§a◄ " + v1Str + " §7vs §c" + v2Str;
        } else if (value2 > value1) {
            comparison = "§c" + v1Str + " §7vs §a" + v2Str + " ►";
        } else {
            comparison = "§e" + v1Str + " §7= §e" + v2Str;
        }
        
        return new ItemBuilder(material)
            .name(stat.getColor() + stat.getIcon() + " " + stat.getDisplayName())
            .lore(
                "",
                comparison,
                "",
                "§8Différence: " + formatDifference(value1 - value2, stat.isPercentage())
            )
            .build();
    }

    private String formatDifference(double diff, boolean isPercent) {
        String formatted = isPercent ? 
            String.format("%.1f%%", diff) : 
            String.format("%.1f", diff);
        
        if (diff > 0) return "§a+" + formatted;
        if (diff < 0) return "§c" + formatted;
        return "§7" + formatted;
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
            if (!(event.getInventory().getHolder() instanceof ItemCompareGUI gui)) {
                return;
            }
            
            event.setCancelled(true);
            
            if (event.getRawSlot() == 40) {
                event.getWhoClicked().closeInventory();
            }
        }
        
        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            // Cleanup si nécessaire
        }
    }
}
