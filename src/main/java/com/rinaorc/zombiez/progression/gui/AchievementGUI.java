package com.rinaorc.zombiez.progression.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.progression.AchievementManager;
import com.rinaorc.zombiez.progression.AchievementManager.Achievement;
import com.rinaorc.zombiez.progression.AchievementManager.AchievementCategory;
import com.rinaorc.zombiez.progression.AchievementManager.AchievementTier;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menu GUI ultra-satisfaisant pour le système d'achievements
 *
 * Features:
 * - Vue d'ensemble avec stats globales
 * - Navigation par catégories avec barres de progression
 * - Multi-pages pour chaque catégorie
 * - Section "Prochains objectifs"
 * - Design satisfaisant et dopamine-inducing
 */
public class AchievementGUI implements Listener {

    private final ZombieZPlugin plugin;

    // Titres des menus
    private static final String MAIN_TITLE = "§6✦ Achievements §8- ";
    private static final String CATEGORY_TITLE = "§e★ ";

    // Tracking des pages par joueur
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private final Map<UUID, AchievementCategory> playerCategories = new ConcurrentHashMap<>();
    private final Map<UUID, ViewMode> playerViewModes = new ConcurrentHashMap<>();

    // Slots constants
    private static final int[] ACHIEVEMENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    private static final int ACHIEVEMENTS_PER_PAGE = 21;

    public enum ViewMode {
        OVERVIEW,
        CATEGORY,
        NEXT_OBJECTIVES,
        TIER_VIEW
    }

    public AchievementGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ouvre le menu principal d'achievements
     */
    public void open(Player player) {
        openOverview(player);
    }

    /**
     * Ouvre la vue d'ensemble des achievements
     */
    public void openOverview(Player player) {
        playerViewModes.put(player.getUniqueId(), ViewMode.OVERVIEW);

        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE + "Vue d'ensemble");

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();

        if (data == null) return;

        // Bordure avec gradient
        fillGradientBorder(inv);

        // === Header: Stats globales ===
        int totalAchievements = am.getAchievements().size();
        int unlockedCount = am.getUnlockedCount(player);
        double completionPercent = am.getCompletionPercent(player);

        // Tête du joueur avec stats
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .skullOwner(player.getName())
            .name("§6§l✦ " + player.getName() + " ✦")
            .lore(
                "",
                "§7Achievements débloqués:",
                createAnimatedProgressBar(completionPercent),
                "§e" + unlockedCount + "§7/§e" + totalAchievements + " §8(" + String.format("%.1f", completionPercent) + "%)",
                "",
                "§7Prochain milestone: §e" + getNextMilestone(unlockedCount),
                "",
                getTierSummary(player, am)
            )
            .build());

        // === Section: Catégories (ligne 2) ===
        int slot = 10;
        for (AchievementCategory category : AchievementCategory.values()) {
            int catUnlocked = am.getUnlockedCountByCategory(player, category);
            int catTotal = am.getByCategory().get(category).size();
            double catPercent = am.getCompletionPercentByCategory(player, category);

            Material icon = category.getIcon();
            boolean complete = catUnlocked == catTotal;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Progrès: " + createMiniProgressBar(catPercent));
            lore.add("§e" + catUnlocked + "§7/§e" + catTotal + " §8(" + String.format("%.0f", catPercent) + "%)");
            lore.add("");

            // Ajouter aperçu des achievements
            List<Achievement> catAchievements = am.getByCategory().get(category);
            int shown = 0;
            for (Achievement a : catAchievements) {
                if (shown >= 3) {
                    lore.add("§8  ... et " + (catTotal - 3) + " de plus");
                    break;
                }
                boolean unlocked = data.hasAchievement(a.id());
                lore.add((unlocked ? "§a✓ " : "§7○ ") + a.tier().getColor() + a.name());
                shown++;
            }

            lore.add("");
            lore.add(complete ? "§a✓ Catégorie complète!" : "§eClique pour voir");

            inv.setItem(slot, new ItemBuilder(icon)
                .name(category.getColor() + "§l" + category.getEmoji() + " " + category.getDisplayName())
                .lore(lore)
                .glow(complete)
                .build());

            slot++;
        }

        // === Section: Prochains objectifs (ligne 3) ===
        inv.setItem(27, new ItemBuilder(Material.SPYGLASS)
            .name("§e§l🎯 Prochains Objectifs")
            .lore(
                "",
                "§7Tes achievements les plus",
                "§7proches de déblocage!",
                "",
                "§eClique pour voir"
            )
            .build());

        // Afficher les 5 prochains achievements
        List<Achievement> nextAchievements = am.getNextAchievements(player, 5);
        int nextSlot = 28;
        for (Achievement achievement : nextAchievements) {
            int progress = data.getAchievementProgress(achievement.id());
            int requirement = achievement.requirement();
            double progressPercent = Math.min(100, (double) progress / requirement * 100);

            inv.setItem(nextSlot++, new ItemBuilder(achievement.icon())
                .name(achievement.tier().getColor() + achievement.name())
                .lore(
                    "§7" + achievement.description(),
                    "",
                    "§7Progrès: " + createMiniProgressBar(progressPercent),
                    "§e" + formatNumber(progress) + "§7/§e" + formatNumber(requirement),
                    "",
                    "§7Récompenses:",
                    "§e  +" + formatNumber(achievement.pointReward()) + " Points",
                    "§d  +" + achievement.gemReward() + " Gemmes"
                )
                .build());
        }

        // === Section: Filtres par tier (ligne 4) ===
        inv.setItem(37, createTierFilterItem(AchievementTier.BRONZE, am, player));
        inv.setItem(38, createTierFilterItem(AchievementTier.SILVER, am, player));
        inv.setItem(39, createTierFilterItem(AchievementTier.GOLD, am, player));
        inv.setItem(40, createTierFilterItem(AchievementTier.DIAMOND, am, player));
        inv.setItem(41, createTierFilterItem(AchievementTier.LEGENDARY, am, player));
        inv.setItem(42, createTierFilterItem(AchievementTier.MYTHIC, am, player));

        // === Section: Stats (ligne 5) ===
        Map<AchievementTier, Integer> tierCounts = am.getTierCounts();

        inv.setItem(48, new ItemBuilder(Material.BOOK)
            .name("§b§l📊 Statistiques")
            .lore(
                "",
                "§7Total achievements: §e" + totalAchievements,
                "",
                "§7Par tier:",
                "§6  Bronze: §f" + tierCounts.get(AchievementTier.BRONZE),
                "§7  Argent: §f" + tierCounts.get(AchievementTier.SILVER),
                "§e  Or: §f" + tierCounts.get(AchievementTier.GOLD),
                "§b  Diamant: §f" + tierCounts.get(AchievementTier.DIAMOND),
                "§d  Légendaire: §f" + tierCounts.get(AchievementTier.LEGENDARY),
                "§4  Mythique: §f" + tierCounts.get(AchievementTier.MYTHIC)
            )
            .build());

        // Achievements récents
        List<Achievement> recent = am.getRecentlyUnlocked(player);
        List<String> recentLore = new ArrayList<>();
        recentLore.add("");
        if (recent.isEmpty()) {
            recentLore.add("§7Aucun achievement récent");
        } else {
            for (Achievement a : recent) {
                recentLore.add(a.tier().getColor() + "✓ " + a.name());
            }
        }

        inv.setItem(50, new ItemBuilder(Material.CLOCK)
            .name("§a§l🕐 Récemment Débloqués")
            .lore(recentLore)
            .build());

        // Bouton fermer
        inv.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§c§lFermer")
            .build());

        player.openInventory(inv);
        playOpenSound(player);
    }

    /**
     * Ouvre une catégorie d'achievements
     */
    public void openCategory(Player player, AchievementCategory category, int page) {
        playerViewModes.put(player.getUniqueId(), ViewMode.CATEGORY);
        playerCategories.put(player.getUniqueId(), category);
        playerPages.put(player.getUniqueId(), page);

        AchievementManager am = plugin.getAchievementManager();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);

        if (data == null) return;

        List<Achievement> achievements = am.getByCategory().get(category);
        int totalPages = (int) Math.ceil((double) achievements.size() / ACHIEVEMENTS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = CATEGORY_TITLE + category.getDisplayName() + " §8(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Bordure colorée selon la catégorie
        fillCategoryBorder(inv, category);

        // Header avec stats de la catégorie
        int catUnlocked = am.getUnlockedCountByCategory(player, category);
        int catTotal = achievements.size();
        double catPercent = am.getCompletionPercentByCategory(player, category);

        inv.setItem(4, new ItemBuilder(category.getIcon())
            .name(category.getColor() + "§l" + category.getEmoji() + " " + category.getDisplayName())
            .lore(
                "",
                "§7Progrès de la catégorie:",
                createAnimatedProgressBar(catPercent),
                "§e" + catUnlocked + "§7/§e" + catTotal + " §8(" + String.format("%.1f", catPercent) + "%)",
                "",
                catUnlocked == catTotal ? "§a§l✓ CATÉGORIE COMPLÈTE!" : "§7Continue comme ça!"
            )
            .glow(catUnlocked == catTotal)
            .build());

        // Afficher les achievements de cette page
        int startIndex = page * ACHIEVEMENTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACHIEVEMENTS_PER_PAGE, achievements.size());

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Achievement achievement = achievements.get(i);
            inv.setItem(ACHIEVEMENT_SLOTS[slotIndex++], createAchievementItem(achievement, data));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, new ItemBuilder(Material.ARROW)
                .name("§e« Page précédente")
                .lore("§7Page " + page + "/" + totalPages)
                .build());
        }

        if (page < totalPages - 1) {
            inv.setItem(53, new ItemBuilder(Material.ARROW)
                .name("§ePage suivante »")
                .lore("§7Page " + (page + 2) + "/" + totalPages)
                .build());
        }

        // Bouton retour
        inv.setItem(49, new ItemBuilder(Material.DARK_OAK_DOOR)
            .name("§7Retour à la vue d'ensemble")
            .build());

        // Navigation catégories rapide
        int catSlot = 46;
        for (AchievementCategory cat : AchievementCategory.values()) {
            if (catSlot > 52 || catSlot == 49) {
                if (catSlot == 49) catSlot++;
                if (catSlot > 52) break;
            }

            boolean isSelected = cat == category;
            inv.setItem(catSlot++, new ItemBuilder(cat.getIcon())
                .name(cat.getColor() + (isSelected ? "§l" : "") + cat.getDisplayName())
                .lore(isSelected ? "§a► Sélectionné" : "§7Clique pour voir")
                .glow(isSelected)
                .build());
        }

        player.openInventory(inv);
    }

    /**
     * Ouvre la vue des prochains objectifs
     */
    public void openNextObjectives(Player player) {
        playerViewModes.put(player.getUniqueId(), ViewMode.NEXT_OBJECTIVES);

        Inventory inv = Bukkit.createInventory(null, 54, "§e§l🎯 Prochains Objectifs");

        AchievementManager am = plugin.getAchievementManager();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);

        if (data == null) return;

        // Bordure
        fillGradientBorder(inv);

        // Header
        inv.setItem(4, new ItemBuilder(Material.SPYGLASS)
            .name("§e§l🎯 Tes Prochains Objectifs")
            .lore(
                "",
                "§7Les achievements les plus",
                "§7proches de déblocage!",
                "",
                "§7Complète-les pour gagner",
                "§7des récompenses!"
            )
            .build());

        // Obtenir les 15 prochains achievements (triés par progression)
        List<Achievement> nextAchievements = am.getNextAchievements(player, 15);

        int slotIndex = 0;
        for (Achievement achievement : nextAchievements) {
            if (slotIndex >= ACHIEVEMENT_SLOTS.length) break;

            int progress = data.getAchievementProgress(achievement.id());
            int requirement = achievement.requirement();
            double progressPercent = Math.min(100, (double) progress / requirement * 100);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + achievement.description());
            lore.add("");
            lore.add("§7Catégorie: " + achievement.category().getColor() + achievement.category().getDisplayName());
            lore.add("§7Tier: " + achievement.tier().getColor() + achievement.tier().getDisplayName());
            lore.add("");
            lore.add("§7Progrès:");
            lore.add(createAnimatedProgressBar(progressPercent));
            lore.add("§e" + formatNumber(progress) + "§7/§e" + formatNumber(requirement) +
                    " §8(" + String.format("%.1f", progressPercent) + "%)");
            lore.add("");
            lore.add("§7Récompenses:");
            lore.add("§e  +" + formatNumber(achievement.pointReward()) + " Points");
            lore.add("§d  +" + achievement.gemReward() + " Gemmes");
            if (achievement.title() != null && !achievement.title().isEmpty()) {
                lore.add("§7  Titre: " + achievement.title());
            }

            // Indicateur de proximité
            String proximityIndicator;
            if (progressPercent >= 90) {
                proximityIndicator = "§a§l⚡ PRESQUE LÀ!";
            } else if (progressPercent >= 75) {
                proximityIndicator = "§e🔥 Très proche!";
            } else if (progressPercent >= 50) {
                proximityIndicator = "§6📈 Bonne progression";
            } else {
                proximityIndicator = "§7📊 En cours";
            }
            lore.add("");
            lore.add(proximityIndicator);

            inv.setItem(ACHIEVEMENT_SLOTS[slotIndex++], new ItemBuilder(achievement.icon())
                .name(achievement.tier().getColor() + "§l" + achievement.name())
                .lore(lore)
                .glow(progressPercent >= 90)
                .build());
        }

        // Bouton retour
        inv.setItem(49, new ItemBuilder(Material.DARK_OAK_DOOR)
            .name("§7Retour à la vue d'ensemble")
            .build());

        player.openInventory(inv);
    }

    /**
     * Crée un item d'achievement
     */
    private ItemStack createAchievementItem(Achievement achievement, PlayerData data) {
        boolean unlocked = data.hasAchievement(achievement.id());
        int progress = data.getAchievementProgress(achievement.id());
        int requirement = achievement.requirement();
        double progressPercent = unlocked ? 100 : Math.min(100, (double) progress / requirement * 100);

        Material displayMaterial = unlocked ? achievement.icon() : Material.GRAY_DYE;
        String nameColor = unlocked ? achievement.tier().getColor() : "§8";

        List<String> lore = new ArrayList<>();
        lore.add("§7" + achievement.description());
        lore.add("");
        lore.add("§7Tier: " + achievement.tier().getColor() + achievement.tier().getStars() + " " + achievement.tier().getDisplayName());
        lore.add("");

        if (unlocked) {
            lore.add("§a§l✓ DÉBLOQUÉ!");
        } else {
            lore.add("§7Progrès:");
            lore.add(createMiniProgressBar(progressPercent));
            lore.add("§e" + formatNumber(progress) + "§7/§e" + formatNumber(requirement));
        }

        lore.add("");
        lore.add("§7Récompenses:");
        lore.add("§e  +" + formatNumber(achievement.pointReward()) + " Points");
        lore.add("§d  +" + achievement.gemReward() + " Gemmes");

        if (achievement.title() != null && !achievement.title().isEmpty()) {
            lore.add("§7  Titre: " + achievement.title());
        }

        return new ItemBuilder(displayMaterial)
            .name(nameColor + (unlocked ? "§l" : "") + achievement.name())
            .lore(lore)
            .glow(unlocked)
            .build();
    }

    /**
     * Crée un item de filtre par tier
     */
    private ItemStack createTierFilterItem(AchievementTier tier, AchievementManager am, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return new ItemStack(Material.AIR);

        List<Achievement> tierAchievements = am.getByTier().get(tier);
        int total = tierAchievements.size();
        int unlocked = (int) tierAchievements.stream()
            .filter(a -> data.hasAchievement(a.id()))
            .count();

        Material icon = switch (tier) {
            case BRONZE -> Material.BRICK;
            case SILVER -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case DIAMOND -> Material.DIAMOND;
            case LEGENDARY -> Material.AMETHYST_SHARD;
            case MYTHIC -> Material.NETHER_STAR;
        };

        return new ItemBuilder(icon)
            .name(tier.getColor() + "§l" + tier.getStars() + " " + tier.getDisplayName())
            .lore(
                "",
                "§e" + unlocked + "§7/§e" + total + " débloqués",
                "",
                unlocked == total ? "§a✓ Tous débloqués!" : "§7" + (total - unlocked) + " restants"
            )
            .glow(unlocked == total)
            .build();
    }

    // ==================== UTILITAIRES VISUELS ====================

    /**
     * Crée une barre de progression animée
     */
    private String createAnimatedProgressBar(double percent) {
        int totalBars = 20;
        int filled = (int) (percent / 5);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else if (i == filled && percent % 5 > 2.5) {
                bar.append("§e▓");
            } else {
                bar.append("§8░");
            }
        }

        return bar.toString();
    }

    /**
     * Crée une mini barre de progression
     */
    private String createMiniProgressBar(double percent) {
        int totalBars = 15;
        int filled = (int) (percent * totalBars / 100);

        StringBuilder bar = new StringBuilder("§8[");

        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                if (percent >= 90) bar.append("§a");
                else if (percent >= 50) bar.append("§e");
                else bar.append("§6");
                bar.append("|");
            } else {
                bar.append("§7|");
            }
        }

        bar.append("§8] ");

        if (percent >= 100) bar.append("§a");
        else if (percent >= 75) bar.append("§e");
        else if (percent >= 50) bar.append("§6");
        else bar.append("§7");

        bar.append(String.format("%.0f%%", percent));

        return bar.toString();
    }

    /**
     * Remplit la bordure avec un gradient
     */
    private void fillGradientBorder(Inventory inv) {
        Material[] gradient = {
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE
        };

        ItemStack pane1 = new ItemBuilder(gradient[0]).name(" ").build();
        ItemStack pane2 = new ItemBuilder(gradient[1]).name(" ").build();
        ItemStack pane3 = new ItemBuilder(gradient[2]).name(" ").build();
        ItemStack pane4 = new ItemBuilder(gradient[3]).name(" ").build();

        // Top row
        for (int i = 0; i < 9; i++) {
            if (i <= 2) inv.setItem(i, pane1);
            else if (i <= 5) inv.setItem(i, pane2);
            else inv.setItem(i, pane3);
        }

        // Bottom row
        for (int i = 45; i < 54; i++) {
            if (i <= 47) inv.setItem(i, pane3);
            else if (i <= 50) inv.setItem(i, pane2);
            else inv.setItem(i, pane1);
        }

        // Sides
        for (int i = 9; i < 45; i += 9) {
            int row = i / 9;
            if (row <= 2) {
                inv.setItem(i, pane1);
                inv.setItem(i + 8, pane3);
            } else {
                inv.setItem(i, pane3);
                inv.setItem(i + 8, pane1);
            }
        }
    }

    /**
     * Remplit la bordure selon la catégorie
     */
    private void fillCategoryBorder(Inventory inv, AchievementCategory category) {
        Material pane = switch (category.getColor()) {
            case "§c" -> Material.RED_STAINED_GLASS_PANE;
            case "§a" -> Material.LIME_STAINED_GLASS_PANE;
            case "§e" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "§b" -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "§d" -> Material.MAGENTA_STAINED_GLASS_PANE;
            case "§6" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "§5" -> Material.PURPLE_STAINED_GLASS_PANE;
            default -> Material.GRAY_STAINED_GLASS_PANE;
        };

        ItemStack paneItem = new ItemBuilder(pane).name(" ").build();

        // Top & bottom rows
        for (int i = 0; i < 9; i++) inv.setItem(i, paneItem);
        for (int i = 45; i < 54; i++) inv.setItem(i, paneItem);

        // Sides
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, paneItem);
            inv.setItem(i + 8, paneItem);
        }
    }

    /**
     * Obtient le prochain milestone
     */
    private String getNextMilestone(int current) {
        int[] milestones = {5, 10, 25, 50, 75, 100};
        for (int m : milestones) {
            if (current < m) return m + " achievements";
        }
        return "§a✓ Tous atteints!";
    }

    /**
     * Obtient un résumé des tiers
     */
    private String getTierSummary(Player player, AchievementManager am) {
        StringBuilder sb = new StringBuilder("§7Tiers: ");

        for (AchievementTier tier : AchievementTier.values()) {
            List<Achievement> tierAchievements = am.getByTier().get(tier);
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
            if (data == null) continue;

            int unlocked = (int) tierAchievements.stream()
                .filter(a -> data.hasAchievement(a.id()))
                .count();

            if (unlocked > 0) {
                sb.append(tier.getColor()).append(unlocked).append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Joue un son d'ouverture
     */
    private void playOpenSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
    }

    /**
     * Formate un nombre
     */
    private String formatNumber(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // Vérifier si c'est un de nos menus
        if (!title.startsWith(MAIN_TITLE) && !title.startsWith(CATEGORY_TITLE) &&
            !title.equals("§e§l🎯 Prochains Objectifs")) {
            return;
        }

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR ||
            item.getType().name().endsWith("STAINED_GLASS_PANE")) return;

        int slot = event.getSlot();
        AchievementManager am = plugin.getAchievementManager();

        // Son de clic
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        // === Vue d'ensemble ===
        if (title.startsWith(MAIN_TITLE + "Vue d'ensemble")) {
            // Clic sur catégorie (slots 10-16)
            if (slot >= 10 && slot <= 16) {
                int catIndex = slot - 10;
                AchievementCategory[] categories = AchievementCategory.values();
                if (catIndex < categories.length) {
                    openCategory(player, categories[catIndex], 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
                }
                return;
            }

            // Clic sur "Prochains objectifs"
            if (slot == 27) {
                openNextObjectives(player);
                return;
            }

            // Fermer
            if (slot == 49) {
                player.closeInventory();
                return;
            }
        }

        // === Vue catégorie ===
        if (title.startsWith(CATEGORY_TITLE)) {
            AchievementCategory category = playerCategories.get(player.getUniqueId());
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);

            // Navigation pages
            if (slot == 45 && page > 0) {
                openCategory(player, category, page - 1);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
                return;
            }

            if (slot == 53) {
                int totalPages = (int) Math.ceil((double) am.getByCategory().get(category).size() / ACHIEVEMENTS_PER_PAGE);
                if (page < totalPages - 1) {
                    openCategory(player, category, page + 1);
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
                }
                return;
            }

            // Retour
            if (slot == 49) {
                openOverview(player);
                return;
            }

            // Navigation catégories rapide (slots 46-52 sauf 49)
            if (slot >= 46 && slot <= 52 && slot != 49) {
                int adjustedSlot = slot < 49 ? slot - 46 : slot - 47;
                AchievementCategory[] categories = AchievementCategory.values();
                if (adjustedSlot < categories.length) {
                    openCategory(player, categories[adjustedSlot], 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
                }
                return;
            }
        }

        // === Vue prochains objectifs ===
        if (title.equals("§e§l🎯 Prochains Objectifs")) {
            if (slot == 49) {
                openOverview(player);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Nettoyer les données de tracking
        UUID uuid = player.getUniqueId();

        // Delayed cleanup pour permettre la réouverture
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTitle() == null ||
                    (!player.getOpenInventory().getTitle().startsWith(MAIN_TITLE) &&
                     !player.getOpenInventory().getTitle().startsWith(CATEGORY_TITLE))) {
                    playerPages.remove(uuid);
                    playerCategories.remove(uuid);
                    playerViewModes.remove(uuid);
                }
            }
        }.runTaskLater(plugin, 5L);
    }
}
