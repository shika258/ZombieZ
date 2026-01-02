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
 * GUI comparant deux joueurs sur leurs classements
 */
public class LeaderboardCompareGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player viewer;
    private final UUID player1Uuid;
    private final UUID player2Uuid;
    private final Inventory inventory;

    private static final int GUI_SIZE = 54;

    public LeaderboardCompareGUI(ZombieZPlugin plugin, Player viewer, UUID player1Uuid, UUID player2Uuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.player1Uuid = player1Uuid;
        this.player2Uuid = player2Uuid;

        OfflinePlayer p1 = Bukkit.getOfflinePlayer(player1Uuid);
        OfflinePlayer p2 = Bukkit.getOfflinePlayer(player2Uuid);
        String name1 = p1.getName() != null ? p1.getName() : "Joueur 1";
        String name2 = p2.getName() != null ? p2.getName() : "Joueur 2";

        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text("⚖ " + name1 + " vs " + name2).color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        build();
    }

    private void build() {
        inventory.clear();

        // Bordure
        fillBorder();

        // Têtes des joueurs
        inventory.setItem(2, createPlayerHead(player1Uuid));
        inventory.setItem(6, createPlayerHead(player2Uuid));

        // VS au centre
        inventory.setItem(4, createVsItem());

        // Comparaisons par catégorie
        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> ranks1 = manager.getAllPlayerRanks(player1Uuid);
        Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> ranks2 = manager.getAllPlayerRanks(player2Uuid);

        // Afficher les comparaisons importantes
        int slot = 19;
        LeaderboardType[] mainTypes = {
            LeaderboardType.KILLS_TOTAL, LeaderboardType.LEVEL, LeaderboardType.MAX_ZONE,
            LeaderboardType.PLAYTIME, LeaderboardType.BOSS_KILLS, LeaderboardType.ELITE_KILLS,
            LeaderboardType.KILL_STREAK, LeaderboardType.ACHIEVEMENTS
        };

        for (LeaderboardType type : mainTypes) {
            if (slot == 26) slot = 28;
            if (slot > 34) break;

            Integer rank1 = ranks1.getOrDefault(type, Collections.emptyMap()).get(LeaderboardPeriod.ALL_TIME);
            Integer rank2 = ranks2.getOrDefault(type, Collections.emptyMap()).get(LeaderboardPeriod.ALL_TIME);

            inventory.setItem(slot, createComparisonItem(type, rank1, rank2));
            slot++;
        }

        // Score final
        inventory.setItem(40, createScoreItem(ranks1, ranks2));

        // Retour
        inventory.setItem(45, createBackItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            if (i != 2 && i != 4 && i != 6) inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private ItemStack createPlayerHead(UUID uuid) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(player);

        String name = player.getName() != null ? player.getName() : "Joueur";
        boolean isViewer = uuid.equals(viewer.getUniqueId());

        meta.displayName(Component.text((isViewer ? "§a§l" : "§b§l") + name));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (isViewer) {
            lore.add(Component.text("§a← C'est toi!"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createVsItem() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§c§lVS"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Comparaison des classements"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createComparisonItem(LeaderboardType type, Integer rank1, Integer rank2) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.getIcon() + " " + type.getDisplayName())
            .color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Déterminer le gagnant
        String winner = "";
        if (rank1 != null && rank2 != null) {
            if (rank1 < rank2) {
                winner = "§a← Gagnant";
            } else if (rank2 < rank1) {
                winner = "§b→ Gagnant";
            } else {
                winner = "§e= Égalité";
            }
        } else if (rank1 != null) {
            winner = "§a← Gagnant";
        } else if (rank2 != null) {
            winner = "§b→ Gagnant";
        }

        OfflinePlayer p1 = Bukkit.getOfflinePlayer(player1Uuid);
        OfflinePlayer p2 = Bukkit.getOfflinePlayer(player2Uuid);
        String name1 = p1.getName() != null ? p1.getName() : "Joueur 1";
        String name2 = p2.getName() != null ? p2.getName() : "Joueur 2";

        String r1Display = rank1 != null ? "#" + rank1 : "Non classé";
        String r2Display = rank2 != null ? "#" + rank2 : "Non classé";

        String color1 = rank1 != null && rank1 <= 10 ? "§e" : "§7";
        String color2 = rank2 != null && rank2 <= 10 ? "§e" : "§7";

        lore.add(Component.text("§a" + name1 + ": " + color1 + r1Display));
        lore.add(Component.text("§b" + name2 + ": " + color2 + r2Display));

        if (!winner.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text(winner));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createScoreItem(Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> ranks1,
                                       Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> ranks2) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l🏆 Résultat Final"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Compter les victoires
        int wins1 = 0, wins2 = 0, ties = 0;

        Set<LeaderboardType> allTypes = new HashSet<>();
        ranks1.keySet().forEach(allTypes::add);
        ranks2.keySet().forEach(allTypes::add);

        for (LeaderboardType type : allTypes) {
            Integer r1 = ranks1.getOrDefault(type, Collections.emptyMap()).get(LeaderboardPeriod.ALL_TIME);
            Integer r2 = ranks2.getOrDefault(type, Collections.emptyMap()).get(LeaderboardPeriod.ALL_TIME);

            if (r1 != null && r2 != null) {
                if (r1 < r2) wins1++;
                else if (r2 < r1) wins2++;
                else ties++;
            } else if (r1 != null) {
                wins1++;
            } else if (r2 != null) {
                wins2++;
            }
        }

        OfflinePlayer p1 = Bukkit.getOfflinePlayer(player1Uuid);
        OfflinePlayer p2 = Bukkit.getOfflinePlayer(player2Uuid);
        String name1 = p1.getName() != null ? p1.getName() : "Joueur 1";
        String name2 = p2.getName() != null ? p2.getName() : "Joueur 2";

        lore.add(Component.text("§a" + name1 + ": §f" + wins1 + " victoires"));
        lore.add(Component.text("§b" + name2 + ": §f" + wins2 + " victoires"));
        lore.add(Component.text("§e= " + ties + " égalités"));

        lore.add(Component.empty());

        // Gagnant global
        if (wins1 > wins2) {
            lore.add(Component.text("§a§l🏆 " + name1 + " gagne!"));
        } else if (wins2 > wins1) {
            lore.add(Component.text("§b§l🏆 " + name2 + " gagne!"));
        } else {
            lore.add(Component.text("§e§l⚖ Match nul!"));
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

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Retour
        if (slot == 45) {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            new LeaderboardMainGUI(plugin, viewer).open();
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
