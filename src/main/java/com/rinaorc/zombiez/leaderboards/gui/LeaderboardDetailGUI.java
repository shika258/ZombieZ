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
 * GUI affichant le classement détaillé d'un leaderboard avec pagination
 */
public class LeaderboardDetailGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final LeaderboardType type;
    private final LeaderboardPeriod period;
    private int page;
    private final Inventory inventory;

    private static final int GUI_SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 28;

    public LeaderboardDetailGUI(ZombieZPlugin plugin, Player player,
                                 LeaderboardType type, LeaderboardPeriod period, int page) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
        this.period = period;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text(type.getIcon() + " " + type.getDisplayName() + " §7- Page " + page)
                .decoration(TextDecoration.BOLD, true));
        build();
    }

    private void build() {
        inventory.clear();

        // Bordure
        fillBorder();

        // Titre
        inventory.setItem(4, createTitleItem());

        // Charger les entrées
        LeaderboardManager manager = plugin.getLeaderboardManager();
        List<LeaderboardEntry> entries = manager.getTopEntries(type, period, 100);

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, entries.size());

        // Afficher les entrées
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot > 43) break;

            LeaderboardEntry entry = entries.get(i);
            inventory.setItem(slot, createEntryItem(entry));
            slot++;
        }

        // Position du joueur
        int playerRank = manager.getPlayerRank(player.getUniqueId(), type, period);
        inventory.setItem(40, createPlayerPositionItem(playerRank, entries));

        // Navigation
        int totalPages = (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (page > 1) {
            inventory.setItem(48, createPreviousPageItem());
        }

        inventory.setItem(49, createPageInfoItem(totalPages));

        if (page < totalPages) {
            inventory.setItem(50, createNextPageItem());
        }

        // Retour
        inventory.setItem(45, createBackItem());

        // Chercher joueur
        inventory.setItem(53, createSearchItem());
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

    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.getIcon() + " " + type.getDisplayName())
            .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + type.getDescription()));
        lore.add(Component.empty());
        lore.add(Component.text("§ePériode: §f" + period.getDisplayName()));
        lore.add(Component.text("§eCategorie: §f" + type.getCategory().getDisplayName()));

        if (period != LeaderboardPeriod.ALL_TIME) {
            lore.add(Component.empty());
            lore.add(Component.text("§7Reset dans: " + period.getFormattedTimeUntilReset()));
        }

        // Récompenses
        lore.add(Component.empty());
        lore.add(Component.text("§6Récompenses Top 3:"));

        for (int rank = 1; rank <= 3; rank++) {
            LeaderboardReward reward = LeaderboardReward.calculateReward(type, period, rank);
            if (reward != null && reward.hasContent()) {
                String icon = rank == 1 ? "§6🥇" : (rank == 2 ? "§f🥈" : "§c🥉");
                lore.add(Component.text("  " + icon + " " + reward.format()));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEntryItem(LeaderboardEntry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Charger la tête du joueur
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getUuid());
        meta.setOwningPlayer(offlinePlayer);

        // Couleur selon le rang
        String rankColor = entry.getRankColor();
        boolean isPlayer = entry.getUuid().equals(player.getUniqueId());

        String displayName = rankColor + "#" + entry.getRank() + " §f" + entry.getPlayerName();
        if (isPlayer) displayName = "§a§l→ " + displayName + " §a§l←";

        meta.displayName(Component.text(displayName));

        List<Component> lore = new ArrayList<>();

        // Valeur
        String value = type == LeaderboardType.PLAYTIME ?
            entry.getFormattedTime() : entry.getFormattedValue();
        lore.add(Component.text("§7" + type.getDisplayName() + ": §e" + value));

        // Rang avec médaille
        lore.add(Component.empty());
        lore.add(Component.text(entry.getRankIcon() + " §7Rang §f#" + entry.getRank()));

        // Récompense potentielle
        if (entry.getRank() <= 100) {
            LeaderboardReward reward = LeaderboardReward.calculateReward(type, period, entry.getRank());
            if (reward != null && reward.hasContent()) {
                lore.add(Component.empty());
                lore.add(Component.text("§6Récompense:"));
                if (reward.getPoints() > 0) {
                    lore.add(Component.text("  §e+" + reward.getPoints() + " points"));
                }
                if (reward.getGems() > 0) {
                    lore.add(Component.text("  §d+" + reward.getGems() + " gemmes"));
                }
                if (reward.getTitle() != null) {
                    lore.add(Component.text("  §6Titre: " + reward.getTitle()));
                }
            }
        }

        if (isPlayer) {
            lore.add(Component.empty());
            lore.add(Component.text("§a✓ C'est toi!"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerPositionItem(int playerRank, List<LeaderboardEntry> entries) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);

        meta.displayName(Component.text("§a§lTa Position"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (playerRank > 0) {
            String color = playerRank <= 3 ? "§6§l" : (playerRank <= 10 ? "§e" : "§7");
            lore.add(Component.text("§7Rang: " + color + "#" + playerRank));

            // Trouver le score du joueur
            entries.stream()
                .filter(e -> e.getUuid().equals(player.getUniqueId()))
                .findFirst()
                .ifPresent(e -> {
                    String value = type == LeaderboardType.PLAYTIME ?
                        e.getFormattedTime() : e.getFormattedValue();
                    lore.add(Component.text("§7Score: §e" + value));
                });

            // Distance au rang supérieur
            if (playerRank > 1 && playerRank <= entries.size()) {
                LeaderboardEntry above = entries.get(playerRank - 2);
                LeaderboardEntry current = entries.stream()
                    .filter(e -> e.getUuid().equals(player.getUniqueId()))
                    .findFirst().orElse(null);

                if (current != null) {
                    long diff = above.getValue() - current.getValue();
                    if (diff > 0) {
                        lore.add(Component.empty());
                        lore.add(Component.text("§7Pour monter #" + (playerRank - 1) + ":"));
                        lore.add(Component.text("  §e+" + formatNumber(diff) + " " + type.getDisplayName().toLowerCase()));
                    }
                }
            }
        } else {
            lore.add(Component.text("§7Tu n'es pas encore classé"));
            lore.add(Component.text("§7dans ce leaderboard."));
            lore.add(Component.empty());
            lore.add(Component.text("§eJoue plus pour apparaître!"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreviousPageItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§l← Page Précédente"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextPageItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§lPage Suivante →"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageInfoItem(int totalPages) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§f§lPage " + page + "/" + totalPages));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Affiche " + ENTRIES_PER_PAGE + " joueurs par page"));
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

    private ItemStack createSearchItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§e§l🔍 Chercher Joueur"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Utilise §e/lb search <pseudo>"));
        lore.add(Component.text("§7pour chercher un joueur"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Retour
        if (slot == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new LeaderboardCategoryGUI(plugin, player, type.getCategory(), period).open();
            return;
        }

        // Page précédente
        if (slot == 48 && page > 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            page--;
            build();
            return;
        }

        // Page suivante
        LeaderboardManager manager = plugin.getLeaderboardManager();
        List<LeaderboardEntry> entries = manager.getTopEntries(type, period, 100);
        int totalPages = (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE);

        if (slot == 50 && page < totalPages) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            page++;
            build();
            return;
        }

        // Clic sur un joueur (tête)
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.PLAYER_HEAD && slot != 40) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new LeaderboardPlayerGUI(plugin, player, meta.getOwningPlayer().getUniqueId()).open();
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
