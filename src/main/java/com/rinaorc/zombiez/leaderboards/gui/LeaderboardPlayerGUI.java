package com.rinaorc.zombiez.leaderboards.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.leaderboards.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * GUI affichant le profil d'un joueur avec tous ses classements
 */
public class LeaderboardPlayerGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final Inventory inventory;
    private LeaderboardPeriod selectedPeriod = LeaderboardPeriod.ALL_TIME;

    private static final int GUI_SIZE = 54;

    public LeaderboardPlayerGUI(ZombieZPlugin plugin, Player viewer, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String name = target.getName() != null ? target.getName() : "Joueur";

        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text("📊 Profil de " + name).color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true));
        build();
    }

    private void build() {
        inventory.clear();

        // Bordure
        fillBorder();

        // Tête du joueur avec infos
        inventory.setItem(4, createPlayerHeadItem());

        // Sélecteur de période
        int periodSlot = 10;
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            inventory.setItem(periodSlot, createPeriodItem(period));
            periodSlot++;
        }

        // Classements par catégorie
        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> allRanks =
            manager.getAllPlayerRanks(targetUuid);

        // Regrouper par catégorie
        Map<LeaderboardCategory, List<Map.Entry<LeaderboardType, Integer>>> byCategory = new EnumMap<>(LeaderboardCategory.class);

        for (Map.Entry<LeaderboardType, Map<LeaderboardPeriod, Integer>> entry : allRanks.entrySet()) {
            Integer rank = entry.getValue().get(selectedPeriod);
            if (rank != null) {
                byCategory.computeIfAbsent(entry.getKey().getCategory(), k -> new ArrayList<>())
                    .add(Map.entry(entry.getKey(), rank));
            }
        }

        // Afficher les classements
        int slot = 19;
        for (LeaderboardCategory category : LeaderboardCategory.values()) {
            List<Map.Entry<LeaderboardType, Integer>> categoryRanks = byCategory.get(category);

            if (categoryRanks != null && !categoryRanks.isEmpty()) {
                if (slot == 26) slot = 28;
                if (slot == 35) slot = 37;
                if (slot > 43) break;

                inventory.setItem(slot, createCategoryRanksItem(category, categoryRanks));
                slot++;
            }
        }

        // Meilleur classement
        inventory.setItem(40, createBestRankItem(allRanks));

        // Retour
        inventory.setItem(45, createBackItem());

        // Comparer (si pas soi-même)
        if (!targetUuid.equals(viewer.getUniqueId())) {
            inventory.setItem(53, createCompareItem());
        }
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

    private ItemStack createPlayerHeadItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        meta.setOwningPlayer(target);

        String name = target.getName() != null ? target.getName() : "Joueur";
        meta.displayName(Component.text("§a§l" + name));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Statut en ligne
        if (target.isOnline()) {
            lore.add(Component.text("§a● En ligne"));
        } else {
            lore.add(Component.text("§c○ Hors ligne"));
        }

        lore.add(Component.empty());
        lore.add(Component.text("§ePériode sélectionnée: §f" + selectedPeriod.getDisplayName()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPeriodItem(LeaderboardPeriod period) {
        boolean selected = period == selectedPeriod;
        ItemStack item = new ItemStack(selected ? Material.LIME_DYE : period.getIcon());
        ItemMeta meta = item.getItemMeta();

        String prefix = selected ? "§a§l✓ " : "§7";
        meta.displayName(Component.text(prefix + period.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(period.getDescription()));

        if (!selected) {
            lore.add(Component.empty());
            lore.add(Component.text("§eClique pour sélectionner"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryRanksItem(LeaderboardCategory category,
                                               List<Map.Entry<LeaderboardType, Integer>> ranks) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(category.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(category.getDescription()));
        lore.add(Component.empty());

        // Trier par rang
        ranks.sort(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<LeaderboardType, Integer> entry : ranks) {
            int rank = entry.getValue();
            String color = rank <= 3 ? "§6" : (rank <= 10 ? "§e" : "§7");
            lore.add(Component.text("  " + color + "#" + rank + " §f" + entry.getKey().getDisplayName()));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBestRankItem(Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> allRanks) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§lMeilleur Classement"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Trouver le meilleur rang
        LeaderboardType bestType = null;
        int bestRank = Integer.MAX_VALUE;

        for (Map.Entry<LeaderboardType, Map<LeaderboardPeriod, Integer>> entry : allRanks.entrySet()) {
            Integer rank = entry.getValue().get(selectedPeriod);
            if (rank != null && rank < bestRank) {
                bestRank = rank;
                bestType = entry.getKey();
            }
        }

        if (bestType != null) {
            String color = bestRank <= 3 ? "§6§l" : (bestRank <= 10 ? "§e" : "§7");
            lore.add(Component.text("§7Leaderboard: §f" + bestType.getDisplayName()));
            lore.add(Component.text("§7Rang: " + color + "#" + bestRank));

            // Médaille
            if (bestRank <= 3) {
                String medal = switch (bestRank) {
                    case 1 -> "§6🥇 Champion!";
                    case 2 -> "§f🥈 Argent!";
                    case 3 -> "§c🥉 Bronze!";
                    default -> "";
                };
                lore.add(Component.empty());
                lore.add(Component.text(medal));
            }
        } else {
            lore.add(Component.text("§7Aucun classement trouvé"));
            lore.add(Component.text("§7pour cette période"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c§l← Retour"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCompareItem() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§l⚖ Comparer avec toi"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Compare tes stats avec"));
        lore.add(Component.text("§7celles de ce joueur"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Sélection de période
        if (slot >= 10 && slot <= 14) {
            LeaderboardPeriod[] periods = LeaderboardPeriod.values();
            int index = slot - 10;
            if (index < periods.length) {
                selectedPeriod = periods[index];
                viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                build();
            }
            return;
        }

        // Retour
        if (slot == 45) {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new LeaderboardMainGUI(plugin, viewer).open();
            return;
        }

        // Comparer
        if (slot == 53 && !targetUuid.equals(viewer.getUniqueId())) {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LeaderboardCompareGUI(plugin, viewer, viewer.getUniqueId(), targetUuid).open();
        }
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
