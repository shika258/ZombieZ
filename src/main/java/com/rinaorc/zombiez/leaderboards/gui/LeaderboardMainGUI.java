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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * GUI principal des Leaderboards - Menu de sélection des catégories
 */
public class LeaderboardMainGUI implements InventoryHolder {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private LeaderboardPeriod selectedPeriod = LeaderboardPeriod.ALL_TIME;

    private static final int GUI_SIZE = 54;

    public LeaderboardMainGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE,
            Component.text("🏆 Classements ZombieZ").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        build();
    }

    // Slots pour le layout symétrique
    private static final int[] PERIOD_SLOTS = {11, 12, 13, 14, 15}; // Row 1 centrée
    private static final int SLOT_PROFILE = 38;   // Row 4 - gauche
    private static final int SLOT_REWARDS = 40;   // Row 4 - centre
    private static final int SLOT_SEASON = 42;    // Row 4 - droite
    private static final int SLOT_CLOSE = 49;     // Row 5 - centre

    private void build() {
        inventory.clear();

        // Bordure décorative
        fillBorder();

        // Titre et informations (row 0, centre)
        inventory.setItem(4, createInfoItem());

        // Sélecteur de période - Row 1 centrée (slots 11-15)
        LeaderboardPeriod[] periods = LeaderboardPeriod.values();
        for (int i = 0; i < periods.length && i < PERIOD_SLOTS.length; i++) {
            inventory.setItem(PERIOD_SLOTS[i], createPeriodItem(periods[i]));
        }

        // Catégories - Rows 2-3 (slots définis dans LeaderboardCategory)
        for (LeaderboardCategory category : LeaderboardCategory.values()) {
            inventory.setItem(category.getGuiSlot(), createCategoryItem(category));
        }

        // Actions - Row 4 centrée et espacée
        inventory.setItem(SLOT_PROFILE, createPlayerProfileItem());
        inventory.setItem(SLOT_REWARDS, createPendingRewardsItem());
        inventory.setItem(SLOT_SEASON, createSeasonItem());

        // Fermer - Row 5 centre
        inventory.setItem(SLOT_CLOSE, createCloseItem());
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.empty());
        border.setItemMeta(meta);

        // Ligne du haut et bas
        for (int i = 0; i < 9; i++) {
            if (i != 4) inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }

        // Côtés
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l🏆 CLASSEMENTS ZOMBIEZ 🏆"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Consulte les classements et compare-toi"));
        lore.add(Component.text("§7aux autres survivants!"));
        lore.add(Component.empty());
        lore.add(Component.text("§ePériode: §f" + selectedPeriod.getDisplayName()));

        if (selectedPeriod != LeaderboardPeriod.ALL_TIME) {
            lore.add(Component.text("§7Reset dans: " + selectedPeriod.getFormattedTimeUntilReset()));
        }

        lore.add(Component.empty());
        lore.add(Component.text("§8Dernière MAJ: " + formatLastUpdate()));

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
        lore.add(Component.empty());

        if (period != LeaderboardPeriod.ALL_TIME) {
            lore.add(Component.text("§8─────────────────"));
            lore.add(Component.text("§6⏰ Prochaine distribution:"));
            lore.add(Component.text("§f  " + period.getFormattedTimeUntilReset()));
            lore.add(Component.text("§8─────────────────"));
            lore.add(Component.empty());
            lore.add(Component.text("§7Les récompenses seront"));
            lore.add(Component.text("§7automatiquement distribuées"));
            lore.add(Component.text("§7aux joueurs du Top 100."));
        } else {
            lore.add(Component.text("§8─────────────────"));
            lore.add(Component.text("§7Classement permanent"));
            lore.add(Component.text("§7(jamais réinitialisé)"));
            lore.add(Component.text("§8─────────────────"));
        }

        lore.add(Component.empty());
        if (selected) {
            lore.add(Component.text("§a✓ Sélectionné"));
        } else {
            lore.add(Component.text("§eClique pour sélectionner"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryItem(LeaderboardCategory category) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(category.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(category.getDescription()));
        lore.add(Component.empty());

        // Afficher les leaderboards de cette catégorie
        int count = 0;
        for (LeaderboardType type : LeaderboardType.values()) {
            if (type.getCategory() == category && count < 5) {
                lore.add(Component.text("§7• " + type.getIcon() + " " + type.getDisplayName()));
                count++;
            }
        }

        if (count > 4) {
            int remaining = (int) Arrays.stream(LeaderboardType.values())
                .filter(t -> t.getCategory() == category).count() - 5;
            if (remaining > 0) {
                lore.add(Component.text("§8  ... et " + remaining + " autres"));
            }
        }

        lore.add(Component.empty());
        lore.add(Component.text("§eClique pour voir les classements"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerProfileItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        meta.setOwningPlayer(player);
        meta.displayName(Component.text("§a§lTon Profil"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Obtenir les meilleurs classements
        LeaderboardManager manager = plugin.getNewLeaderboardManager();
        Map.Entry<LeaderboardType, Integer> bestRank = manager.getBestRank(player.getUniqueId());

        if (bestRank != null) {
            lore.add(Component.text("§6Meilleur classement:"));
            lore.add(Component.text("  §f#" + bestRank.getValue() + " §7en §e" + bestRank.getKey().getDisplayName()));
        } else {
            lore.add(Component.text("§7Aucun classement pour le moment"));
        }

        lore.add(Component.empty());

        // Top 3 classements
        Map<LeaderboardType, Map<LeaderboardPeriod, Integer>> allRanks =
            manager.getAllPlayerRanks(player.getUniqueId());

        if (!allRanks.isEmpty()) {
            lore.add(Component.text("§bTes classements:"));

            allRanks.entrySet().stream()
                .filter(e -> e.getValue().containsKey(LeaderboardPeriod.ALL_TIME))
                .sorted(Comparator.comparingInt(e -> e.getValue().get(LeaderboardPeriod.ALL_TIME)))
                .limit(5)
                .forEach(e -> {
                    int rank = e.getValue().get(LeaderboardPeriod.ALL_TIME);
                    String color = rank <= 3 ? "§6" : (rank <= 10 ? "§e" : "§7");
                    lore.add(Component.text("  " + color + "#" + rank + " §f" + e.getKey().getDisplayName()));
                });
        }

        lore.add(Component.empty());
        lore.add(Component.text("§eClique pour voir tous tes classements"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPendingRewardsItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l🎁 Récompenses en Attente"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§7Clique pour voir et réclamer"));
        lore.add(Component.text("§7tes récompenses de classement!"));
        lore.add(Component.empty());
        lore.add(Component.text("§8─────────────────"));
        lore.add(Component.text("§6Comment ça marche?"));
        lore.add(Component.text("§7  1. Grimpe dans les classements"));
        lore.add(Component.text("§7  2. Attends la fin de la période"));
        lore.add(Component.text("§7  3. Réclame tes récompenses ici!"));
        lore.add(Component.text("§8─────────────────"));
        lore.add(Component.empty());
        lore.add(Component.text("§a§l➤ §eClique pour ouvrir"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSeasonItem() {
        LeaderboardManager.SeasonData season = plugin.getNewLeaderboardManager().getCurrentSeason();
        ItemStack item = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = item.getItemMeta();

        if (season != null) {
            meta.displayName(Component.text("§c§l" + season.getName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("§7Temps restant: §e" + season.getDaysRemaining() + " jours"));
            lore.add(Component.empty());
            lore.add(Component.text("§7Les classements saisonniers offrent"));
            lore.add(Component.text("§7des récompenses exclusives!"));
            lore.add(Component.empty());
            lore.add(Component.text("§6Récompenses: §eTitres, Cosmétiques, Points"));

            meta.lore(lore);
        } else {
            meta.displayName(Component.text("§c§lSaison"));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c§lFermer"));
        item.setItemMeta(meta);
        return item;
    }

    private String formatLastUpdate() {
        long lastUpdate = plugin.getNewLeaderboardManager().getLastUpdate();
        if (lastUpdate == 0) return "jamais";

        long secondsAgo = (System.currentTimeMillis() - lastUpdate) / 1000;
        if (secondsAgo < 60) return "il y a " + secondsAgo + "s";
        if (secondsAgo < 3600) return "il y a " + (secondsAgo / 60) + "m";
        return "il y a " + (secondsAgo / 3600) + "h";
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        // Sélection de période (slots 11-15)
        for (int i = 0; i < PERIOD_SLOTS.length; i++) {
            if (slot == PERIOD_SLOTS[i]) {
                LeaderboardPeriod[] periods = LeaderboardPeriod.values();
                if (i < periods.length) {
                    selectedPeriod = periods[i];
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    build();
                }
                return;
            }
        }

        // Clic sur une catégorie
        for (LeaderboardCategory category : LeaderboardCategory.values()) {
            if (slot == category.getGuiSlot()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new LeaderboardCategoryGUI(plugin, player, category, selectedPeriod).open();
                return;
            }
        }

        // Profil joueur
        if (slot == SLOT_PROFILE) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LeaderboardPlayerGUI(plugin, player, player.getUniqueId()).open();
            return;
        }

        // Récompenses
        if (slot == SLOT_REWARDS) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            new LeaderboardRewardsGUI(plugin, player).open();
            return;
        }

        // Fermer
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
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
