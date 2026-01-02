package com.rinaorc.zombiez.progression.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.progression.AchievementManager;
import com.rinaorc.zombiez.progression.AchievementManager.Achievement;
import com.rinaorc.zombiez.progression.AchievementManager.AchievementCategory;
import com.rinaorc.zombiez.leaderboards.LeaderboardManager;
import com.rinaorc.zombiez.leaderboards.LeaderboardType;
import com.rinaorc.zombiez.progression.ProgressionManager;
import com.rinaorc.zombiez.progression.SkillTreeManager;
import com.rinaorc.zombiez.progression.SkillTreeManager.Skill;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillTree;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUIs du système de progression
 */
public class ProgressionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private Player currentPlayer;
    
    private static final String MAIN_TITLE = "§6✦ Progression";
    private static final String SKILLS_TITLE = "§c⚔ Compétences";
    private static final String ACHIEVEMENTS_TITLE = "§e★ Achievements";
    private static final String LEADERBOARD_TITLE = "§b🏆 Classements";

    public ProgressionGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }
    
    public ProgressionGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.currentPlayer = player;
    }
    
    /**
     * Alias pour openMainMenu
     */
    public void open(Player player) {
        openMainMenu(player);
    }
    
    /**
     * Ouvre le skill tree
     */
    public void openSkillTree(Player player) {
        openSkillsMenu(player, SkillTree.COMBAT);
    }

    /**
     * Ouvre le menu principal de progression
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        ProgressionManager pm = plugin.getProgressionManager();
        
        // Décoration
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        
        // Stats du joueur (centre haut)
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e" + player.getName())
            .lore(
                "§7Niveau: §a" + data.getLevel().get() + "§7/100",
                "§7Prestige: " + pm.getPrestigeBonus(data.getPrestige().get()).displayName(),
                "",
                "§7XP: §e" + formatNumber(data.getXp().get()) + "§7/§e" + formatNumber(pm.getXpForLevel(data.getLevel().get() + 1)),
                "",
                "§7Kills: §c" + formatNumber(data.getKills().get()),
                "§7Points: §e" + formatNumber(data.getPoints().get()),
                "§7Gemmes: §d" + formatNumber(data.getGems().get())
            )
            .build());
        
        // Compétences
        inv.setItem(20, new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§c⚔ Compétences")
            .lore(
                "§7Débloque des bonus passifs",
                "",
                "§7Points disponibles: §e" + plugin.getSkillTreeManager().getAvailablePoints(player),
                "",
                "§eClique pour ouvrir"
            )
            .build());
        
        // Achievements
        int achievementCount = plugin.getAchievementManager().getUnlockedCount(player);
        int totalAchievements = plugin.getAchievementManager().getAchievements().size();
        
        inv.setItem(22, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§e★ Achievements")
            .lore(
                "§7Débloque des récompenses",
                "",
                "§7Débloqués: §e" + achievementCount + "§7/§e" + totalAchievements,
                "§7Progression: §a" + String.format("%.1f", plugin.getAchievementManager().getCompletionPercent(player)) + "%",
                "",
                "§eClique pour ouvrir"
            )
            .build());
        
        // Classements
        var lbManager = plugin.getNewLeaderboardManager();
        int killRank = lbManager != null ? lbManager.getPlayerRank(player.getUniqueId(), LeaderboardType.KILLS_TOTAL) : -1;
        int levelRank = lbManager != null ? lbManager.getPlayerRank(player.getUniqueId(), LeaderboardType.LEVEL) : -1;
        inv.setItem(24, new ItemBuilder(Material.DIAMOND)
            .name("§b🏆 Classements")
            .lore(
                "§7Vois les meilleurs joueurs",
                "",
                "§7Ton rang Kills: §e#" + (killRank > 0 ? killRank : "-"),
                "§7Ton rang Niveau: §e#" + (levelRank > 0 ? levelRank : "-"),
                "",
                "§eClique pour ouvrir"
            )
            .build());
        
        // Prestige
        boolean canPrestige = data.getLevel().get() >= ProgressionManager.MAX_LEVEL && 
                             data.getPrestige().get() < ProgressionManager.MAX_PRESTIGE;
        
        inv.setItem(40, new ItemBuilder(canPrestige ? Material.NETHER_STAR : Material.COAL)
            .name(canPrestige ? "§d★ PRESTIGE!" : "§7★ Prestige")
            .lore(
                canPrestige 
                    ? List.of(
                        "§aTu peux prestige!",
                        "",
                        "§7Prochain: " + pm.getPrestigeBonus(data.getPrestige().get() + 1).displayName(),
                        "",
                        "§c⚠ Reset niveau à 1",
                        "§a✓ Bonus permanents",
                        "",
                        "§eClique pour prestige!"
                    )
                    : List.of(
                        "§7Atteins le niveau 100",
                        "§7pour débloquer le prestige",
                        "",
                        "§7Progression: §e" + data.getLevel().get() + "§7/100"
                    )
            )
            .glow(canPrestige)
            .build());
        
        // Fermer
        inv.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§cFermer")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Ouvre le menu des compétences
     */
    public void openSkillsMenu(Player player, SkillTree tree) {
        Inventory inv = Bukkit.createInventory(null, 54, SKILLS_TITLE + " - " + tree.getDisplayName());
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        SkillTreeManager stm = plugin.getSkillTreeManager();
        
        // Décoration
        fillBorder(inv, Material.valueOf(tree.getColor().replace("§c", "RED").replace("§a", "LIME").replace("§e", "YELLOW") + "_STAINED_GLASS_PANE"));
        
        // Info points
        inv.setItem(4, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§ePoints Disponibles: §a" + stm.getAvailablePoints(player))
            .lore(
                "§7Gagne 1 point tous les 5 niveaux",
                "§7+5 points par prestige"
            )
            .build());
        
        // Skills de cet arbre
        List<Skill> treeSkills = stm.getSkillsByTree().get(tree);
        int slot = 19;
        
        for (Skill skill : treeSkills) {
            boolean unlocked = data.hasSkill(skill.id());
            boolean canUnlock = !unlocked && 
                (skill.prerequisite() == null || data.hasSkill(skill.prerequisite())) &&
                stm.getAvailablePoints(player) >= skill.cost();
            
            List<String> lore = new ArrayList<>();
            lore.add(skill.description());
            lore.add("");
            lore.add("§7Coût: §e" + skill.cost() + " point(s)");
            lore.add("§7Tier: §e" + skill.tier());
            
            if (skill.prerequisite() != null) {
                Skill prereq = stm.getSkills().get(skill.prerequisite());
                boolean hasPrereq = data.hasSkill(skill.prerequisite());
                lore.add("§7Prérequis: " + (hasPrereq ? "§a✓ " : "§c✗ ") + prereq.name());
            }
            
            lore.add("");
            if (unlocked) {
                lore.add("§a✓ Débloqué");
            } else if (canUnlock) {
                lore.add("§eClique pour débloquer");
            } else {
                lore.add("§cConditions non remplies");
            }
            
            inv.setItem(slot, new ItemBuilder(skill.icon())
                .name((unlocked ? "§a" : canUnlock ? "§e" : "§7") + skill.name())
                .lore(lore)
                .glow(unlocked)
                .build());
            
            slot++;
            if ((slot - 10) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Navigation arbres
        int treeSlot = 45;
        for (SkillTree t : SkillTree.values()) {
            inv.setItem(treeSlot++, new ItemBuilder(t.getIcon())
                .name(t.getColor() + t.getDisplayName())
                .lore("§7" + t.getDescription(), "", t == tree ? "§a► Sélectionné" : "§eClique pour voir")
                .glow(t == tree)
                .build());
        }
        
        // Reset
        inv.setItem(52, new ItemBuilder(Material.TNT)
            .name("§cRéinitialiser")
            .lore("§7Coût: §d50 Gemmes", "", "§eClique pour reset")
            .build());
        
        // Retour
        inv.setItem(53, new ItemBuilder(Material.ARROW)
            .name("§7Retour")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Ouvre le menu des achievements
     */
    public void openAchievementsMenu(Player player, AchievementCategory category) {
        Inventory inv = Bukkit.createInventory(null, 54, ACHIEVEMENTS_TITLE + " - " + category.getDisplayName());
        
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        AchievementManager am = plugin.getAchievementManager();
        
        // Décoration
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);
        
        // Stats
        inv.setItem(4, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§e" + category.getDisplayName())
            .lore(
                "§7Progression totale: §a" + String.format("%.1f", am.getCompletionPercent(player)) + "%"
            )
            .build());
        
        // Achievements de cette catégorie
        List<Achievement> categoryAchievements = am.getByCategory().get(category);
        int slot = 10;
        
        for (Achievement achievement : categoryAchievements) {
            boolean unlocked = data.hasAchievement(achievement.id());
            int progress = data.getAchievementProgress(achievement.id());
            int required = achievement.requirement();
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + achievement.description());
            lore.add("");
            lore.add("§7Tier: " + achievement.tier().getColor() + achievement.tier().getDisplayName());
            lore.add("");
            
            if (unlocked) {
                lore.add("§a✓ Débloqué!");
            } else {
                lore.add("§7Progrès: §e" + progress + "§7/§e" + required);
                double percent = (double) progress / required * 100;
                lore.add(createProgressBar(percent));
            }
            
            lore.add("");
            lore.add("§7Récompenses:");
            lore.add("  §e+" + achievement.pointReward() + " Points");
            lore.add("  §d+" + achievement.gemReward() + " Gemmes");
            
            inv.setItem(slot, new ItemBuilder(unlocked ? achievement.icon() : Material.GRAY_DYE)
                .name((unlocked ? achievement.tier().getColor() : "§8") + achievement.name())
                .lore(lore)
                .glow(unlocked)
                .build());
            
            slot++;
            if ((slot - 10) % 7 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Navigation catégories
        int catSlot = 45;
        for (AchievementCategory cat : AchievementCategory.values()) {
            inv.setItem(catSlot++, new ItemBuilder(cat.getIcon())
                .name(cat.getColor() + cat.getDisplayName())
                .lore(cat == category ? "§a► Sélectionné" : "§eClique pour voir")
                .glow(cat == category)
                .build());
        }
        
        // Retour
        inv.setItem(53, new ItemBuilder(Material.ARROW)
            .name("§7Retour")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Ouvre le menu des classements
     */
    public void openLeaderboardMenu(Player player, LeaderboardType type) {
        Inventory inv = Bukkit.createInventory(null, 54, LEADERBOARD_TITLE + " - " + type.getDisplayName());

        LeaderboardManager lm = plugin.getNewLeaderboardManager();
        
        // Décoration
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Top 10
        var top = lm.getTop(type, 10);
        int slot = 10;
        
        for (var entry : top) {
            Material skull = switch (entry.getRank()) {
                case 1 -> Material.GOLDEN_APPLE;
                case 2 -> Material.IRON_INGOT;
                case 3 -> Material.COPPER_INGOT;
                default -> Material.PLAYER_HEAD;
            };
            
            String rankColor = switch (entry.getRank()) {
                case 1 -> "§6§l";
                case 2 -> "§7§l";
                case 3 -> "§c§l";
                default -> "§e";
            };
            
            inv.setItem(slot, new ItemBuilder(skull)
                .name(rankColor + "#" + entry.getRank() + " §f" + entry.getName())
                .lore(
                    "§7" + type.getDisplayName() + ": §e" + formatNumber(entry.getValue())
                )
                .build());
            
            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
        }
        
        // Ton rang
        int myRank = lm.getPlayerRank(player.getUniqueId(), type);
        inv.setItem(40, new ItemBuilder(Material.COMPASS)
            .name("§eTon Classement")
            .lore(
                "§7Rang: §e#" + (myRank > 0 ? myRank : "N/A"),
                "",
                "§7Dernière mise à jour:",
                "§7Il y a " + ((System.currentTimeMillis() - lm.getLastUpdate()) / 1000) + "s"
            )
            .build());
        
        // Navigation types
        int typeSlot = 45;
        for (LeaderboardType t : LeaderboardType.values()) {
            if (typeSlot > 51) break;
            inv.setItem(typeSlot++, new ItemBuilder(Material.PAPER)
                .name("§b" + t.getIcon() + " " + t.getDisplayName())
                .lore(t == type ? "§a► Sélectionné" : "§eClique pour voir")
                .glow(t == type)
                .build());
        }
        
        // Retour
        inv.setItem(53, new ItemBuilder(Material.ARROW)
            .name("§7Retour")
            .build());
        
        player.openInventory(inv);
    }

    /**
     * Gère les clics dans les inventaires
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        if (title.startsWith(MAIN_TITLE) || title.startsWith(SKILLS_TITLE) || 
            title.startsWith(ACHIEVEMENTS_TITLE) || title.startsWith(LEADERBOARD_TITLE)) {
            
            event.setCancelled(true);
            
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            
            handleClick(player, title, event.getSlot(), item);
        }
    }

    /**
     * Traite un clic
     */
    private void handleClick(Player player, String title, int slot, ItemStack item) {
        // Menu principal
        if (title.equals(MAIN_TITLE)) {
            switch (slot) {
                case 20 -> openSkillsMenu(player, SkillTree.COMBAT);
                case 22 -> new AchievementGUI(plugin).open(player); // Nouveau menu achievements
                case 24 -> openLeaderboardMenu(player, LeaderboardType.KILLS);
                case 40 -> {
                    // Prestige
                    if (plugin.getProgressionManager().prestige(player)) {
                        player.closeInventory();
                    }
                }
                case 49 -> player.closeInventory();
            }
            return;
        }
        
        // Menu compétences
        if (title.startsWith(SKILLS_TITLE)) {
            if (slot == 53) {
                openMainMenu(player);
                return;
            }
            if (slot == 52) {
                plugin.getSkillTreeManager().resetSkills(player, false);
                String currentTree = title.split(" - ")[1];
                for (SkillTree t : SkillTree.values()) {
                    if (t.getDisplayName().equals(currentTree)) {
                        openSkillsMenu(player, t);
                        break;
                    }
                }
                return;
            }
            
            // Navigation arbres
            if (slot >= 45 && slot <= 47) {
                SkillTree[] trees = SkillTree.values();
                if (slot - 45 < trees.length) {
                    openSkillsMenu(player, trees[slot - 45]);
                }
                return;
            }
            
            // Clic sur un skill
            String itemName = item.getItemMeta().getDisplayName();
            for (Skill skill : plugin.getSkillTreeManager().getSkills().values()) {
                if (itemName.contains(skill.name())) {
                    plugin.getSkillTreeManager().unlockSkill(player, skill.id());
                    // Refresh
                    String currentTree = title.split(" - ")[1];
                    for (SkillTree t : SkillTree.values()) {
                        if (t.getDisplayName().equals(currentTree)) {
                            openSkillsMenu(player, t);
                            break;
                        }
                    }
                    break;
                }
            }
            return;
        }
        
        // Menu achievements
        if (title.startsWith(ACHIEVEMENTS_TITLE)) {
            if (slot == 53) {
                openMainMenu(player);
                return;
            }
            
            // Navigation catégories
            if (slot >= 45 && slot <= 50) {
                AchievementCategory[] cats = AchievementCategory.values();
                if (slot - 45 < cats.length) {
                    openAchievementsMenu(player, cats[slot - 45]);
                }
            }
            return;
        }
        
        // Menu leaderboards
        if (title.startsWith(LEADERBOARD_TITLE)) {
            if (slot == 53) {
                openMainMenu(player);
                return;
            }
            
            // Navigation types
            if (slot >= 45 && slot <= 51) {
                LeaderboardType[] types = LeaderboardType.values();
                if (slot - 45 < types.length) {
                    openLeaderboardMenu(player, types[slot - 45]);
                }
            }
        }
    }

    /**
     * Remplit les bordures
     */
    private void fillBorder(Inventory inv, Material material) {
        ItemStack pane = new ItemBuilder(material).name(" ").build();
        
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, pane);
            inv.setItem(i + 8, pane);
        }
    }

    /**
     * Crée une barre de progression
     */
    private String createProgressBar(double percent) {
        int filled = (int) (percent / 5);
        int empty = 20 - filled;
        
        return "§a" + "█".repeat(Math.max(0, filled)) + 
               "§7" + "█".repeat(Math.max(0, empty)) + 
               " §e" + String.format("%.1f", percent) + "%";
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
}
