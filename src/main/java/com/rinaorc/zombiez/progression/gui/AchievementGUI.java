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
 * Design ergonomique optimisé pour 200 joueurs:
 * - Vue d'ensemble claire avec statistiques
 * - Navigation intuitive par catégories
 * - Multi-pages fluide
 * - Feedback visuel et sonore satisfaisant
 */
public class AchievementGUI implements Listener {

    private final ZombieZPlugin plugin;

    // Titres des menus (préfixes pour identification)
    private static final String MENU_PREFIX = "§6✦ ";
    private static final String OVERVIEW_TITLE = MENU_PREFIX + "Hauts-faits";
    private static final String CATEGORY_PREFIX = "§e★ ";
    private static final String NEXT_TITLE = "§e🎯 Prochains Objectifs";
    private static final String TIER_PREFIX = "§b◆ Tier: ";

    // Tracking des données de session par joueur (thread-safe)
    private final Map<UUID, SessionData> sessions = new ConcurrentHashMap<>();

    // Slots pour affichage des achievements (3 lignes x 7 colonnes)
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };
    private static final int ITEMS_PER_PAGE = 21;

    // Slots de navigation fixes
    private static final int SLOT_BACK = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 53;

    // Données de session pour un joueur
    private record SessionData(ViewMode mode, AchievementCategory category, AchievementTier tier, int page) {
        static SessionData overview() { return new SessionData(ViewMode.OVERVIEW, null, null, 0); }
        SessionData withPage(int p) { return new SessionData(mode, category, tier, p); }
        SessionData withCategory(AchievementCategory c) { return new SessionData(ViewMode.CATEGORY, c, null, 0); }
        SessionData withTier(AchievementTier t) { return new SessionData(ViewMode.TIER, null, t, 0); }
        SessionData withNext() { return new SessionData(ViewMode.NEXT_OBJECTIVES, null, null, 0); }
    }

    private enum ViewMode {
        OVERVIEW, CATEGORY, NEXT_OBJECTIVES, TIER
    }

    public AchievementGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== API PUBLIQUE ====================

    /**
     * Ouvre le menu principal d'achievements
     */
    public void open(Player player) {
        sessions.put(player.getUniqueId(), SessionData.overview());
        showOverview(player);
    }

    // ==================== VUES ====================

    /**
     * Vue d'ensemble - Menu principal
     */
    private void showOverview(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();
        if (data == null || am == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, OVERVIEW_TITLE);

        // Bordure élégante
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        // === HEADER: Profil du joueur (slot 4) ===
        int total = am.getAchievements().size();
        int unlocked = am.getUnlockedCount(player);
        double percent = am.getCompletionPercent(player);

        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .skullOwner(player.getName())
            .name("§6§l✦ Mes Hauts-faits ✦")
            .lore(
                "",
                "§7Progression globale:",
                progressBar(percent, 20, "§a", "§8"),
                "§e" + unlocked + "§7/§e" + total + " §8(" + String.format("%.1f", percent) + "%)",
                "",
                "§7Prochain palier: " + getNextMilestoneText(unlocked),
                "",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                getTierSummaryLine(player, am)
            )
            .build());

        // === CATÉGORIES: Ligne 1 (slots 10-13) et Ligne 2 (slots 19-21) ===
        AchievementCategory[] cats = AchievementCategory.values();
        int[] catSlots = {10, 11, 12, 13, 19, 20, 21}; // 7 slots pour 7 catégories

        for (int i = 0; i < cats.length && i < catSlots.length; i++) {
            AchievementCategory cat = cats[i];
            int catUnlocked = am.getUnlockedCountByCategory(player, cat);
            int catTotal = am.getByCategory().get(cat).size();
            double catPercent = (double) catUnlocked / catTotal * 100;
            boolean complete = catUnlocked == catTotal;

            inv.setItem(catSlots[i], new ItemBuilder(cat.getIcon())
                .name(cat.getColor() + "§l" + cat.getDisplayName())
                .lore(
                    "§7" + getCategoryDescription(cat),
                    "",
                    "§7Progrès: " + miniProgressBar(catPercent),
                    "§e" + catUnlocked + "§7/§e" + catTotal,
                    "",
                    complete ? "§a§l✓ Complète!" : "§e▶ Clic pour explorer"
                )
                .glow(complete)
                .build());
        }

        // === PROCHAINS OBJECTIFS (slots 15-16, 24-25) ===
        inv.setItem(15, new ItemBuilder(Material.SPYGLASS)
            .name("§e§l🎯 Prochains Objectifs")
            .lore(
                "",
                "§7Découvre les achievements",
                "§7les plus proches à débloquer!",
                "",
                "§e▶ Clic pour voir"
            )
            .build());

        // Aperçu des 3 prochains achievements
        List<Achievement> nextList = am.getNextAchievements(player, 3);
        int[] previewSlots = {16, 24, 25};
        for (int i = 0; i < Math.min(nextList.size(), previewSlots.length); i++) {
            Achievement ach = nextList.get(i);
            int prog = data.getAchievementProgress(ach.id());
            double achPercent = Math.min(100, (double) prog / ach.requirement() * 100);

            inv.setItem(previewSlots[i], new ItemBuilder(ach.icon())
                .name(ach.tier().getColor() + ach.name())
                .lore(
                    "§7" + ach.description(),
                    "",
                    miniProgressBar(achPercent) + " §8" + String.format("%.0f%%", achPercent),
                    "",
                    "§e▶ Clic sur 🎯 pour plus"
                )
                .build());
        }

        // === FILTRES PAR TIER (ligne 4: slots 28-33) ===
        inv.setItem(28, createTierButton(AchievementTier.BRONZE, am, data));
        inv.setItem(29, createTierButton(AchievementTier.SILVER, am, data));
        inv.setItem(30, createTierButton(AchievementTier.GOLD, am, data));
        inv.setItem(31, createTierButton(AchievementTier.DIAMOND, am, data));
        inv.setItem(32, createTierButton(AchievementTier.LEGENDARY, am, data));
        inv.setItem(33, createTierButton(AchievementTier.MYTHIC, am, data));

        // === FOOTER ===
        // Aide
        inv.setItem(48, new ItemBuilder(Material.BOOK)
            .name("§b§l❓ Aide")
            .lore(
                "",
                "§7Les achievements te récompensent",
                "§7pour ta progression dans le jeu.",
                "",
                "§7Chaque achievement débloqué",
                "§7te donne des §ePoints §7et §dGemmes§7.",
                "",
                "§7Les tiers les plus difficiles",
                "§7offrent des §6sous-titres exclusifs§7!"
            )
            .build());

        // Fermer
        inv.setItem(SLOT_INFO, new ItemBuilder(Material.BARRIER)
            .name("§c§lFermer")
            .lore("", "§7Clic pour fermer le menu")
            .build());

        // Stats rapides
        inv.setItem(50, new ItemBuilder(Material.PAPER)
            .name("§a§l📊 Statistiques Rapides")
            .lore(
                "",
                "§7Récompenses totales obtenues:",
                "§e  " + formatNumber(calculateTotalPointsEarned(data, am)) + " Points",
                "§d  " + calculateTotalGemsEarned(data, am) + " Gemmes",
                "",
                "§7Sous-titres débloqués: §6" + countUnlockedTitles(data, am)
            )
            .build());

        player.openInventory(inv);
        playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f);
    }

    /**
     * Vue d'une catégorie spécifique
     */
    private void showCategory(Player player, AchievementCategory category, int page) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();
        if (data == null || am == null) return;

        List<Achievement> achievements = am.getByCategory().get(category);
        int totalPages = Math.max(1, (int) Math.ceil((double) achievements.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        // Mise à jour session
        sessions.put(player.getUniqueId(), sessions.getOrDefault(player.getUniqueId(), SessionData.overview())
            .withCategory(category).withPage(page));

        String title = CATEGORY_PREFIX + category.getDisplayName() +
            (totalPages > 1 ? " §8(" + (page + 1) + "/" + totalPages + ")" : "");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Bordure colorée selon catégorie
        fillBorder(inv, getCategoryPane(category));

        // Header catégorie
        int catUnlocked = am.getUnlockedCountByCategory(player, category);
        int catTotal = achievements.size();
        double catPercent = (double) catUnlocked / catTotal * 100;

        inv.setItem(4, new ItemBuilder(category.getIcon())
            .name(category.getColor() + "§l" + category.getDisplayName())
            .lore(
                "§7" + getCategoryDescription(category),
                "",
                "§7Progrès:",
                progressBar(catPercent, 20, "§a", "§8"),
                "§e" + catUnlocked + "§7/§e" + catTotal + " §8(" + String.format("%.1f", catPercent) + "%)",
                "",
                catUnlocked == catTotal ? "§a§l★ CATÉGORIE COMPLÈTE! ★" : "§7Continue comme ça!"
            )
            .glow(catUnlocked == catTotal)
            .build());

        // Afficher les achievements de cette page
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, achievements.size());

        for (int i = start, slot = 0; i < end && slot < CONTENT_SLOTS.length; i++, slot++) {
            inv.setItem(CONTENT_SLOTS[slot], createAchievementItem(achievements.get(i), data));
        }

        // Navigation
        setupNavigation(inv, page, totalPages, true);

        // Navigation rapide entre catégories (slots 46-48, 50-52)
        int[] quickNavSlots = {46, 47, 48, 50, 51, 52};
        AchievementCategory[] cats = AchievementCategory.values();
        for (int i = 0; i < cats.length && i < quickNavSlots.length; i++) {
            boolean selected = cats[i] == category;
            inv.setItem(quickNavSlots[i], new ItemBuilder(cats[i].getIcon())
                .name(cats[i].getColor() + (selected ? "§l" : "") + cats[i].getDisplayName())
                .lore(selected ? "§a► Sélectionné" : "§7▶ Clic pour voir")
                .amount(selected ? 1 : 1)
                .glow(selected)
                .build());
        }

        player.openInventory(inv);
        if (page == 0) playSound(player, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f);
    }

    /**
     * Vue des prochains objectifs
     */
    private void showNextObjectives(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();
        if (data == null || am == null) return;

        sessions.put(player.getUniqueId(), SessionData.overview().withNext());

        Inventory inv = Bukkit.createInventory(null, 54, NEXT_TITLE);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        // Header
        inv.setItem(4, new ItemBuilder(Material.SPYGLASS)
            .name("§e§l🎯 Prochains Objectifs")
            .lore(
                "",
                "§7Ces achievements sont les plus",
                "§7proches d'être débloqués!",
                "",
                "§7Concentre-toi sur ceux-ci",
                "§7pour progresser rapidement."
            )
            .build());

        // Les 21 prochains achievements les plus proches
        List<Achievement> nextAchievements = am.getNextAchievements(player, ITEMS_PER_PAGE);

        for (int i = 0; i < nextAchievements.size() && i < CONTENT_SLOTS.length; i++) {
            Achievement ach = nextAchievements.get(i);
            int progress = data.getAchievementProgress(ach.id());
            int requirement = ach.requirement();
            double percent = Math.min(100, (double) progress / requirement * 100);

            String proximityTag = getProximityTag(percent);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + ach.description());
            lore.add("");
            lore.add("§7Catégorie: " + ach.category().getColor() + ach.category().getDisplayName());
            lore.add("§7Difficulté: " + ach.tier().getColor() + ach.tier().getDisplayName());
            lore.add("");
            lore.add("§7Progrès:");
            lore.add(progressBar(percent, 15, "§a", "§8") + " §f" + String.format("%.1f%%", percent));
            lore.add("§e" + formatNumber(progress) + "§7/§e" + formatNumber(requirement));
            lore.add("");
            lore.add("§7Récompenses: §e" + formatNumber(ach.pointReward()) + " pts §8| §d" + ach.gemReward() + " Gemmes");
            if (ach.subtitle() != null && !ach.subtitle().isEmpty()) {
                lore.add("§7Sous-titre: " + ach.subtitle());
            }
            lore.add("");
            lore.add(proximityTag);

            inv.setItem(CONTENT_SLOTS[i], new ItemBuilder(ach.icon())
                .name(ach.tier().getColor() + "§l" + ach.name())
                .lore(lore)
                .glow(percent >= 90)
                .build());
        }

        // Retour
        inv.setItem(SLOT_INFO, new ItemBuilder(Material.ARROW)
            .name("§7◀ Retour")
            .lore("", "§7Retour au menu principal")
            .build());

        player.openInventory(inv);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f);
    }

    /**
     * Vue filtrée par tier
     */
    private void showTierView(Player player, AchievementTier tier, int page) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();
        if (data == null || am == null) return;

        List<Achievement> achievements = am.getByTier().get(tier);
        int totalPages = Math.max(1, (int) Math.ceil((double) achievements.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        sessions.put(player.getUniqueId(), SessionData.overview().withTier(tier).withPage(page));

        String title = TIER_PREFIX + tier.getDisplayName() +
            (totalPages > 1 ? " §8(" + (page + 1) + "/" + totalPages + ")" : "");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv, getTierPane(tier));

        // Header tier
        int tierUnlocked = (int) achievements.stream().filter(a -> data.hasAchievement(a.id())).count();
        int tierTotal = achievements.size();
        double tierPercent = (double) tierUnlocked / tierTotal * 100;

        inv.setItem(4, new ItemBuilder(getTierIcon(tier))
            .name(tier.getColor() + "§l" + tier.getStars() + " " + tier.getDisplayName())
            .lore(
                "",
                "§7Hauts-faits de difficulté " + tier.getDisplayName(),
                "",
                "§7Débloqués:",
                progressBar(tierPercent, 20, "§a", "§8"),
                "§e" + tierUnlocked + "§7/§e" + tierTotal,
                "",
                tierUnlocked == tierTotal ? "§a§l★ TOUS DÉBLOQUÉS! ★" : ""
            )
            .glow(tierUnlocked == tierTotal)
            .build());

        // Afficher les achievements
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, achievements.size());

        for (int i = start, slot = 0; i < end && slot < CONTENT_SLOTS.length; i++, slot++) {
            inv.setItem(CONTENT_SLOTS[slot], createAchievementItem(achievements.get(i), data));
        }

        // Navigation
        setupNavigation(inv, page, totalPages, true);

        // Navigation rapide tiers
        int[] tierSlots = {46, 47, 48, 50, 51, 52};
        AchievementTier[] tiers = AchievementTier.values();
        for (int i = 0; i < tiers.length && i < tierSlots.length; i++) {
            boolean selected = tiers[i] == tier;
            inv.setItem(tierSlots[i], new ItemBuilder(getTierIcon(tiers[i]))
                .name(tiers[i].getColor() + (selected ? "§l" : "") + tiers[i].getDisplayName())
                .lore(selected ? "§a► Sélectionné" : "§7▶ Clic pour voir")
                .glow(selected)
                .build());
        }

        player.openInventory(inv);
    }

    // ==================== CRÉATION D'ITEMS ====================

    private ItemStack createAchievementItem(Achievement ach, PlayerData data) {
        boolean unlocked = data.hasAchievement(ach.id());
        int progress = data.getAchievementProgress(ach.id());
        double percent = unlocked ? 100 : Math.min(100, (double) progress / ach.requirement() * 100);

        Material mat = unlocked ? ach.icon() : Material.GRAY_DYE;
        String color = unlocked ? ach.tier().getColor() : "§8";

        List<String> lore = new ArrayList<>();
        lore.add("§7" + ach.description());
        lore.add("");
        lore.add("§7Difficulté: " + ach.tier().getColor() + ach.tier().getStars() + " " + ach.tier().getDisplayName());
        lore.add("");

        if (unlocked) {
            lore.add("§a§l✓ DÉBLOQUÉ!");
        } else {
            lore.add("§7Progrès: " + miniProgressBar(percent));
            lore.add("§e" + formatNumber(progress) + "§7/§e" + formatNumber(ach.requirement()));
        }

        lore.add("");
        lore.add("§7Récompenses:");
        lore.add("§e  ⬤ " + formatNumber(ach.pointReward()) + " Points");
        lore.add("§d  ◆ " + ach.gemReward() + " Gemmes");

        if (ach.subtitle() != null && !ach.subtitle().isEmpty()) {
            lore.add("§6  ★ " + ach.subtitle());
        }

        return new ItemBuilder(mat)
            .name(color + (unlocked ? "§l" : "") + ach.name())
            .lore(lore)
            .glow(unlocked)
            .build();
    }

    private ItemStack createTierButton(AchievementTier tier, AchievementManager am, PlayerData data) {
        List<Achievement> tierAchs = am.getByTier().get(tier);
        int unlocked = (int) tierAchs.stream().filter(a -> data.hasAchievement(a.id())).count();
        int total = tierAchs.size();
        boolean complete = unlocked == total;

        return new ItemBuilder(getTierIcon(tier))
            .name(tier.getColor() + "§l" + tier.getStars() + " " + tier.getDisplayName())
            .lore(
                "",
                "§e" + unlocked + "§7/§e" + total + " débloqués",
                miniProgressBar((double) unlocked / total * 100),
                "",
                complete ? "§a✓ Tous débloqués!" : "§7" + (total - unlocked) + " restant(s)",
                "",
                "§e▶ Clic pour filtrer"
            )
            .glow(complete)
            .build();
    }

    // ==================== UTILITAIRES UI ====================

    private void setupNavigation(Inventory inv, int page, int totalPages, boolean hasBack) {
        // Page précédente
        if (page > 0) {
            inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name("§e« Page précédente")
                .lore("§7Aller à la page " + page)
                .build());
        } else if (hasBack) {
            inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name("§7◀ Retour")
                .lore("§7Retour au menu principal")
                .build());
        }

        // Info page
        inv.setItem(SLOT_INFO, new ItemBuilder(Material.PAPER)
            .name("§7Page " + (page + 1) + "/" + totalPages)
            .lore("", "§8Navigation avec les flèches")
            .build());

        // Page suivante
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                .name("§ePage suivante »")
                .lore("§7Aller à la page " + (page + 2))
                .build());
        }
    }

    private void fillBorder(Inventory inv, Material pane) {
        ItemStack item = new ItemBuilder(pane).name(" ").build();

        // Top row
        for (int i = 0; i < 9; i++) inv.setItem(i, item);
        // Bottom row
        for (int i = 45; i < 54; i++) inv.setItem(i, item);
        // Sides
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, item);
            inv.setItem(i + 8, item);
        }
    }

    private String progressBar(double percent, int length, String filledColor, String emptyColor) {
        int filled = (int) (percent * length / 100);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? filledColor + "█" : emptyColor + "░");
        }
        return sb.toString();
    }

    private String miniProgressBar(double percent) {
        int filled = (int) (percent / 10);
        String color = percent >= 90 ? "§a" : percent >= 50 ? "§e" : "§6";
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? color + "|" : "§7|");
        }
        sb.append("§8] ").append(color).append(String.format("%.0f%%", percent));
        return sb.toString();
    }

    private String getProximityTag(double percent) {
        if (percent >= 95) return "§a§l⚡ IMMINENT!";
        if (percent >= 90) return "§a⚡ Presque là!";
        if (percent >= 75) return "§e🔥 Très proche";
        if (percent >= 50) return "§6📈 Bonne progression";
        if (percent >= 25) return "§7📊 En cours";
        return "§8○ À commencer";
    }

    private String getNextMilestoneText(int current) {
        int[] milestones = {5, 10, 25, 50, 75, 100};
        for (int m : milestones) {
            if (current < m) return "§e" + m + " §7(" + (m - current) + " restants)";
        }
        return "§a✓ Tous atteints!";
    }

    private String getTierSummaryLine(Player player, AchievementManager am) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return "";

        StringBuilder sb = new StringBuilder("§7Tiers: ");
        for (AchievementTier tier : AchievementTier.values()) {
            int count = (int) am.getByTier().get(tier).stream()
                .filter(a -> data.hasAchievement(a.id())).count();
            if (count > 0) sb.append(tier.getColor()).append(count).append(" ");
        }
        return sb.toString();
    }

    private String getCategoryDescription(AchievementCategory cat) {
        return switch (cat) {
            case COMBAT -> "Tue des zombies et boss";
            case EXPLORATION -> "Explore les zones du monde";
            case COLLECTION -> "Accumule richesses et items";
            case SOCIAL -> "Joue avec d'autres survivants";
            case EVENTS -> "Participe aux événements spéciaux";
            case PROGRESSION -> "Progresse et monte en puissance";
            case MASTERY -> "Atteins l'excellence absolue";
        };
    }

    private Material getCategoryPane(AchievementCategory cat) {
        return switch (cat) {
            case COMBAT -> Material.RED_STAINED_GLASS_PANE;
            case EXPLORATION -> Material.LIME_STAINED_GLASS_PANE;
            case COLLECTION -> Material.YELLOW_STAINED_GLASS_PANE;
            case SOCIAL -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case EVENTS -> Material.MAGENTA_STAINED_GLASS_PANE;
            case PROGRESSION -> Material.ORANGE_STAINED_GLASS_PANE;
            case MASTERY -> Material.PURPLE_STAINED_GLASS_PANE;
        };
    }

    private Material getTierPane(AchievementTier tier) {
        return switch (tier) {
            case BRONZE -> Material.ORANGE_STAINED_GLASS_PANE;
            case SILVER -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case GOLD -> Material.YELLOW_STAINED_GLASS_PANE;
            case DIAMOND -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case LEGENDARY -> Material.MAGENTA_STAINED_GLASS_PANE;
            case MYTHIC -> Material.RED_STAINED_GLASS_PANE;
        };
    }

    private Material getTierIcon(AchievementTier tier) {
        return switch (tier) {
            case BRONZE -> Material.BRICK;
            case SILVER -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case DIAMOND -> Material.DIAMOND;
            case LEGENDARY -> Material.AMETHYST_SHARD;
            case MYTHIC -> Material.NETHER_STAR;
        };
    }

    private long calculateTotalPointsEarned(PlayerData data, AchievementManager am) {
        return am.getAchievements().values().stream()
            .filter(a -> data.hasAchievement(a.id()))
            .mapToLong(Achievement::pointReward)
            .sum();
    }

    private int calculateTotalGemsEarned(PlayerData data, AchievementManager am) {
        return am.getAchievements().values().stream()
            .filter(a -> data.hasAchievement(a.id()))
            .mapToInt(Achievement::gemReward)
            .sum();
    }

    private int countUnlockedTitles(PlayerData data, AchievementManager am) {
        return (int) am.getAchievements().values().stream()
            .filter(a -> data.hasAchievement(a.id()))
            .filter(a -> a.subtitle() != null && !a.subtitle().isEmpty())
            .count();
    }

    private String formatNumber(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }

    private void playSound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.5f, pitch);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // Vérifier si c'est notre menu
        if (!title.startsWith(MENU_PREFIX) && !title.startsWith(CATEGORY_PREFIX) &&
            !title.startsWith(NEXT_TITLE) && !title.startsWith(TIER_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR ||
            item.getType().name().endsWith("STAINED_GLASS_PANE")) return;

        int slot = event.getSlot();
        SessionData session = sessions.getOrDefault(player.getUniqueId(), SessionData.overview());
        AchievementManager am = plugin.getAchievementManager();

        playSound(player, Sound.UI_BUTTON_CLICK, 1.0f);

        // === VUE D'ENSEMBLE ===
        if (title.equals(OVERVIEW_TITLE)) {
            handleOverviewClick(player, slot, am);
            return;
        }

        // === VUE CATÉGORIE ===
        if (title.startsWith(CATEGORY_PREFIX)) {
            handleCategoryClick(player, slot, session, am);
            return;
        }

        // === VUE PROCHAINS OBJECTIFS ===
        if (title.startsWith(NEXT_TITLE)) {
            if (slot == SLOT_INFO) {
                showOverview(player);
            }
            return;
        }

        // === VUE TIER ===
        if (title.startsWith(TIER_PREFIX)) {
            handleTierViewClick(player, slot, session, am);
        }
    }

    private void handleOverviewClick(Player player, int slot, AchievementManager am) {
        // Catégories (slots 10-13, 19-21)
        int[] catSlots = {10, 11, 12, 13, 19, 20, 21};
        AchievementCategory[] cats = AchievementCategory.values();

        for (int i = 0; i < catSlots.length && i < cats.length; i++) {
            if (slot == catSlots[i]) {
                showCategory(player, cats[i], 0);
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f);
                return;
            }
        }

        // Prochains objectifs
        if (slot == 15 || slot == 16 || slot == 24 || slot == 25) {
            showNextObjectives(player);
            return;
        }

        // Tiers (slots 28-33)
        int[] tierSlots = {28, 29, 30, 31, 32, 33};
        AchievementTier[] tiers = AchievementTier.values();

        for (int i = 0; i < tierSlots.length && i < tiers.length; i++) {
            if (slot == tierSlots[i]) {
                showTierView(player, tiers[i], 0);
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.3f);
                return;
            }
        }

        // Fermer
        if (slot == SLOT_INFO) {
            player.closeInventory();
        }
    }

    private void handleCategoryClick(Player player, int slot, SessionData session, AchievementManager am) {
        AchievementCategory category = session.category();
        if (category == null) return;

        int totalPages = (int) Math.ceil((double) am.getByCategory().get(category).size() / ITEMS_PER_PAGE);

        // Navigation pages
        if (slot == SLOT_BACK) {
            if (session.page() > 0) {
                showCategory(player, category, session.page() - 1);
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f);
            } else {
                showOverview(player);
            }
            return;
        }

        if (slot == SLOT_NEXT && session.page() < totalPages - 1) {
            showCategory(player, category, session.page() + 1);
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f);
            return;
        }

        // Navigation rapide catégories
        int[] quickNavSlots = {46, 47, 48, 50, 51, 52};
        AchievementCategory[] cats = AchievementCategory.values();

        for (int i = 0; i < quickNavSlots.length && i < cats.length; i++) {
            if (slot == quickNavSlots[i] && cats[i] != category) {
                showCategory(player, cats[i], 0);
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f);
                return;
            }
        }
    }

    private void handleTierViewClick(Player player, int slot, SessionData session, AchievementManager am) {
        AchievementTier tier = session.tier();
        if (tier == null) return;

        int totalPages = (int) Math.ceil((double) am.getByTier().get(tier).size() / ITEMS_PER_PAGE);

        // Navigation pages
        if (slot == SLOT_BACK) {
            if (session.page() > 0) {
                showTierView(player, tier, session.page() - 1);
                playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f);
            } else {
                showOverview(player);
            }
            return;
        }

        if (slot == SLOT_NEXT && session.page() < totalPages - 1) {
            showTierView(player, tier, session.page() + 1);
            playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 1.0f);
            return;
        }

        // Navigation rapide tiers
        int[] tierSlots = {46, 47, 48, 50, 51, 52};
        AchievementTier[] tiers = AchievementTier.values();

        for (int i = 0; i < tierSlots.length && i < tiers.length; i++) {
            if (slot == tierSlots[i] && tiers[i] != tier) {
                showTierView(player, tiers[i], 0);
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.3f);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();

        // Cleanup différé pour permettre la réouverture
        new BukkitRunnable() {
            @Override
            public void run() {
                String openTitle = player.getOpenInventory().getTitle();
                if (openTitle == null ||
                    (!openTitle.startsWith(MENU_PREFIX) && !openTitle.startsWith(CATEGORY_PREFIX) &&
                     !openTitle.startsWith(NEXT_TITLE) && !openTitle.startsWith(TIER_PREFIX))) {
                    sessions.remove(uuid);
                }
            }
        }.runTaskLater(plugin, 5L);
    }
}
