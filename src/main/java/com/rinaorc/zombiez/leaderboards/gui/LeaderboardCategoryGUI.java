package com.rinaorc.zombiez.leaderboards.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI affichant les leaderboards d'une catégorie spécifique
 */
public class LeaderboardCategoryGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final LeaderboardCategory category;
    private final LeaderboardPeriod period;
    private final Inventory inventory;

    private static final int GUI_SIZE = 54;

    // Slots pour navigation
    private static final int SLOT_BACK = 45;
    private static final int SLOT_REFRESH = 49;

    // Rows disponibles pour les leaderboards (slots 10-16, 19-25, 28-34)
    private static final int[][] CONTENT_ROWS = {
        {10, 11, 12, 13, 14, 15, 16}, // Row 1
        {19, 20, 21, 22, 23, 24, 25}, // Row 2
        {28, 29, 30, 31, 32, 33, 34}  // Row 3
    };

    public LeaderboardCategoryGUI(ZombieZPlugin plugin, Player player,
                                   LeaderboardCategory category, LeaderboardPeriod period) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        this.period = period;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text(category.getDisplayName() + " §7- " + period.getDisplayName())
                .decoration(TextDecoration.BOLD, true));
        build();
    }

    private void build() {
        inventory.clear();

        // Bordure
        fillBorder();

        // Titre de la catégorie
        inventory.setItem(4, createCategoryInfoItem());

        // Liste des leaderboards de cette catégorie
        List<LeaderboardType> types = Arrays.stream(LeaderboardType.values())
            .filter(t -> t.getCategory() == category)
            .toList();

        // Placement centré des leaderboards
        placeLeaderboardsCentered(types);

        // Navigation
        inventory.setItem(SLOT_BACK, createBackItem());
        inventory.setItem(SLOT_REFRESH, createRefreshItem());
    }

    /**
     * Place les leaderboards de manière centrée sur les rows disponibles
     */
    private void placeLeaderboardsCentered(List<LeaderboardType> types) {
        int total = types.size();
        if (total == 0) return;

        // Répartir sur les rows (max 7 par row, 3 rows max = 21 items)
        int itemsPerRow = Math.min(7, (total + 2) / 3); // Répartition équilibrée
        if (total <= 4) itemsPerRow = total; // Tout sur une row si <= 4
        else if (total <= 8) itemsPerRow = (total + 1) / 2; // 2 rows

        int index = 0;
        for (int rowIdx = 0; rowIdx < CONTENT_ROWS.length && index < total; rowIdx++) {
            int remaining = total - index;
            int itemsThisRow = Math.min(itemsPerRow, remaining);

            // Ajuster si dernière row pour équilibrer
            if (rowIdx == CONTENT_ROWS.length - 1 || remaining <= itemsPerRow) {
                itemsThisRow = remaining;
            }

            // Centrer sur la row
            int[] centeredSlots = getCenteredSlots(CONTENT_ROWS[rowIdx], itemsThisRow);

            for (int slot : centeredSlots) {
                if (index < total) {
                    inventory.setItem(slot, createLeaderboardItem(types.get(index)));
                    index++;
                }
            }
        }
    }

    /**
     * Retourne les slots centrés pour N items sur une row
     */
    private int[] getCenteredSlots(int[] rowSlots, int count) {
        if (count >= rowSlots.length) return rowSlots;

        int[] result = new int[count];
        int startOffset = (rowSlots.length - count) / 2;

        for (int i = 0; i < count; i++) {
            result[i] = rowSlots[startOffset + i];
        }
        return result;
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private ItemStack createCategoryInfoItem() {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(category.getDisplayName()).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(category.getDescription()));
        lore.add(Component.empty());
        lore.add(Component.text("§ePériode: §f" + period.getDisplayName()));

        if (period != LeaderboardPeriod.ALL_TIME) {
            lore.add(Component.text("§7Reset dans: " + period.getFormattedTimeUntilReset()));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLeaderboardItem(LeaderboardType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.getIcon() + " " + type.getDisplayName())
            .color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + type.getDescription()));
        lore.add(Component.empty());

        // Top 5 preview
        LeaderboardManager manager = plugin.getNewLeaderboardManager();
        List<LeaderboardEntry> top5 = manager.getTopEntries(type, period, 5);

        lore.add(Component.text("§6§lTop 5:"));
        if (top5.isEmpty()) {
            lore.add(Component.text("  §7Aucune donnée"));
        } else {
            for (LeaderboardEntry entry : top5) {
                String rankIcon = entry.getRankIcon();
                String value = type == LeaderboardType.PLAYTIME ?
                    entry.getFormattedTime() : entry.getFormattedValue();
                lore.add(Component.text("  " + rankIcon + " §f" + entry.getPlayerName() + " §7- §e" + value));
            }
        }

        // Rang du joueur
        int playerRank = manager.getPlayerRank(player.getUniqueId(), type, period);
        lore.add(Component.empty());
        if (playerRank > 0) {
            String color = playerRank <= 3 ? "§6" : (playerRank <= 10 ? "§e" : "§7");
            lore.add(Component.text("§bTon rang: " + color + "#" + playerRank));
        } else {
            lore.add(Component.text("§7Tu n'es pas classé"));
        }

        lore.add(Component.empty());
        lore.add(Component.text("§eClique pour voir le classement complet"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c§l← Retour"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Retour au menu principal"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRefreshItem() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§a§l⟳ Rafraîchir"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Actualise les classements"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Retour
        if (slot == SLOT_BACK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new LeaderboardMainGUI(plugin, player).open();
            return;
        }

        // Rafraîchir
        if (slot == SLOT_REFRESH) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            plugin.getNewLeaderboardManager().forceRefreshAll();
            player.sendMessage("§a§l⟳ §aClassements actualisés!");
            build();
            return;
        }

        // Vérifier si c'est un slot de contenu
        boolean isContentSlot = false;
        for (int[] row : CONTENT_ROWS) {
            for (int s : row) {
                if (s == slot) {
                    isContentSlot = true;
                    break;
                }
            }
        }
        if (!isContentSlot) return;

        // Vérifier si c'est un item de leaderboard
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Trouver le type correspondant
        for (LeaderboardType type : LeaderboardType.values()) {
            if (type.getCategory() == category && type.getMaterial() == clicked.getType()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new LeaderboardDetailGUI(plugin, player, type, period, 1).open();
                return;
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
