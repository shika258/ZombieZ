package com.rinaorc.zombiez.progression.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.MissionManager;
import com.rinaorc.zombiez.progression.MissionManager.*;
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

import java.util.*;

/**
 * GUI des missions journalières et hebdomadaires
 */
public class MissionGUI {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final MissionManager missionManager;
    
    private Inventory inventory;
    private ViewMode mode = ViewMode.DAILY;
    
    // Mapping slot -> mission
    private final Map<Integer, MissionProgress> slotMapping;

    public MissionGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.missionManager = plugin.getMissionManager();
        this.slotMapping = new HashMap<>();
    }

    /**
     * Ouvre le GUI
     */
    public void open() {
        createInventory();
        player.openInventory(inventory);
    }

    /**
     * Crée l'inventaire
     */
    private void createInventory() {
        String title = mode == ViewMode.DAILY ? 
            "§e☀ Missions Journalières" : 
            "§d✦ Missions Hebdomadaires";
        
        inventory = Bukkit.createInventory(null, 54, title);
        slotMapping.clear();
        
        PlayerMissions playerMissions = missionManager.getMissions(player.getUniqueId());
        
        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Titre et info
        long resetTime = mode == ViewMode.DAILY ? 
            missionManager.getTimeUntilDailyReset() : 
            missionManager.getTimeUntilWeeklyReset();
        
        inventory.setItem(4, new ItemBuilder(mode == ViewMode.DAILY ? Material.SUNFLOWER : Material.NETHER_STAR)
            .name(mode == ViewMode.DAILY ? "§e☀ Missions Journalières" : "§d✦ Missions Hebdomadaires")
            .lore(
                "§7Complétez vos missions pour",
                "§7gagner des récompenses!",
                "",
                "§7Reset dans: §e" + formatTime(resetTime)
            )
            .build());
        
        // Missions
        Collection<MissionProgress> missions = mode == ViewMode.DAILY ?
            playerMissions.getDailyMissions().values() :
            playerMissions.getWeeklyMissions().values();
        
        int slot = 19;
        for (MissionProgress progress : missions) {
            if (slot == 22) slot = 23; // Sauter le milieu
            if (slot >= 26) break;
            
            inventory.setItem(slot, createMissionItem(progress));
            slotMapping.put(slot, progress);
            slot++;
        }
        
        // Stats
        int completed = mode == ViewMode.DAILY ? 
            playerMissions.getCompletedDailyCount() :
            playerMissions.getCompletedWeeklyCount();
        int total = missions.size();
        
        inventory.setItem(31, new ItemBuilder(Material.BOOK)
            .name("§6Progression")
            .lore(
                "§7Complétées: §a" + completed + "§7/" + total,
                "",
                createProgressBar(completed, total)
            )
            .build());
        
        // Navigation
        inventory.setItem(47, new ItemBuilder(mode == ViewMode.DAILY ? Material.CLOCK : Material.SUNFLOWER)
            .name(mode == ViewMode.DAILY ? "§d→ Missions Hebdomadaires" : "§e→ Missions Journalières")
            .lore("§7Cliquez pour changer")
            .build());
        
        // Bouton fermer
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§cFermer")
            .build());
        
        // Bonus de complétion
        if (completed == total && total > 0) {
            inventory.setItem(51, new ItemBuilder(Material.CHEST)
                .name("§a✓ Bonus de Complétion")
                .lore(
                    "§7Toutes les missions complétées!",
                    "",
                    "§eCliquez pour réclamer:",
                    mode == ViewMode.DAILY ? "§6+100 Points bonus" : "§d+500 Points + 10 Gemmes"
                )
                .glow(true)
                .build());
        }
    }

    /**
     * Crée l'item d'une mission
     */
    private ItemStack createMissionItem(MissionProgress progress) {
        Mission mission = progress.getMission();
        
        Material mat = progress.isCompleted() ? Material.LIME_DYE : mission.getIcon();
        String nameColor = progress.isCompleted() ? "§a" : "§e";
        
        ItemBuilder builder = new ItemBuilder(mat)
            .name(nameColor + mission.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + mission.getDescription());
        lore.add("");
        
        // Catégorie et difficulté
        lore.add("§7Catégorie: " + mission.getCategory().getColor() + mission.getCategory().getDisplayName());
        lore.add("§7Difficulté: " + getDifficultyStars(mission.getDifficulty()));
        lore.add("");
        
        // Progrès
        if (progress.isCompleted()) {
            lore.add("§a✓ Complétée!");
        } else {
            lore.add("§7Progrès: §e" + progress.getProgress() + "§7/" + mission.getGoal());
            lore.add(createMiniProgressBar(progress.getProgressPercent()));
        }
        
        lore.add("");
        
        // Récompenses
        lore.add("§eRécompenses:");
        lore.add("§6  +" + mission.getPointReward() + " Points");
        lore.add("§b  +" + mission.getXpReward() + " XP");
        if (mission.getGemReward() > 0) {
            lore.add("§d  +" + mission.getGemReward() + " Gemmes");
        }
        
        builder.lore(lore);
        
        if (progress.isCompleted()) {
            builder.glow(true);
        }
        
        return builder.build();
    }

    /**
     * Gère un clic
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // Navigation
        if (slot == 47) {
            mode = mode == ViewMode.DAILY ? ViewMode.WEEKLY : ViewMode.DAILY;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }
        
        // Fermer
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        
        // Bonus de complétion
        if (slot == 51) {
            PlayerMissions missions = missionManager.getMissions(player.getUniqueId());
            int completed = mode == ViewMode.DAILY ? 
                missions.getCompletedDailyCount() :
                missions.getCompletedWeeklyCount();
            int total = (mode == ViewMode.DAILY ? missions.getDailyMissions() : missions.getWeeklyMissions()).size();
            
            if (completed == total && total > 0) {
                if (mode == ViewMode.DAILY) {
                    plugin.getEconomyManager().addPoints(player, 100);
                    player.sendMessage("§a+100 Points bonus!");
                } else {
                    plugin.getEconomyManager().addPoints(player, 500);
                    plugin.getEconomyManager().addGems(player, 10);
                    player.sendMessage("§a+500 Points et +10 Gemmes bonus!");
                }
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                player.closeInventory();
            }
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 86400) {
            return String.format("%dj %dh", seconds / 86400, (seconds % 86400) / 3600);
        } else if (seconds >= 3600) {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        } else if (seconds >= 60) {
            return String.format("%dm", seconds / 60);
        }
        return seconds + "s";
    }

    private String getDifficultyStars(int difficulty) {
        String stars = "★".repeat(difficulty);
        String empty = "☆".repeat(5 - difficulty);
        return "§e" + stars + "§7" + empty;
    }

    private String createProgressBar(int current, int max) {
        int percent = max > 0 ? (current * 20 / max) : 0;
        String filled = "§a" + "█".repeat(percent);
        String empty = "§7" + "░".repeat(20 - percent);
        return filled + empty;
    }

    private String createMiniProgressBar(double percent) {
        int filled = (int) (percent / 10);
        String bar = "§a" + "▰".repeat(filled) + "§7" + "▱".repeat(10 - filled);
        return bar + " §e" + String.format("%.0f", percent) + "%";
    }

    /**
     * Modes de vue
     */
    private enum ViewMode {
        DAILY, WEEKLY
    }

    /**
     * Listener pour les événements de GUI
     */
    public static class MissionGUIListener implements Listener {
        
        private final ZombieZPlugin plugin;
        
        public MissionGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }
        
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            
            String title = event.getView().getTitle();
            if (!title.contains("Missions")) return;
            
            MissionGUI gui = new MissionGUI(plugin, player);
            gui.inventory = event.getInventory();
            gui.handleClick(event);
        }
    }
}
