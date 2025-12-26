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
 * Layout amélioré pour 7 daily et 21 weekly
 */
public class MissionGUI {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final MissionManager missionManager;

    private Inventory inventory;
    private ViewMode mode = ViewMode.DAILY;

    // Mapping slot -> mission
    private final Map<Integer, MissionProgress> slotMapping;

    // Slots pour les missions daily (7 slots sur une ligne)
    private static final int[] DAILY_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    // Slots pour les missions weekly (21 slots sur 3 lignes de 7)
    private static final int[] WEEKLY_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Ligne 1
        19, 20, 21, 22, 23, 24, 25,  // Ligne 2
        28, 29, 30, 31, 32, 33, 34   // Ligne 3
    };

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
            "§e§l☀ MISSIONS JOURNALIÈRES" :
            "§d§l✦ MISSIONS HEBDOMADAIRES";

        inventory = Bukkit.createInventory(null, 54, title);
        slotMapping.clear();

        PlayerMissions playerMissions = missionManager.getMissions(player.getUniqueId());

        // === BORDURES ET DÉCORATIONS ===
        fillBorders();

        // === EN-TÊTE : Onglets et Timer ===
        createHeader(playerMissions);

        // === CONTENU : Missions ===
        if (mode == ViewMode.DAILY) {
            createDailyLayout(playerMissions);
        } else {
            createWeeklyLayout(playerMissions);
        }

        // === PIED : Stats, Bonus, Navigation ===
        createFooter(playerMissions);
    }

    /**
     * Remplit les bordures de l'inventaire
     */
    private void fillBorders() {
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        ItemStack accent = new ItemBuilder(mode == ViewMode.DAILY ?
            Material.YELLOW_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE).name(" ").build();

        // Bordure haute
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, i == 4 ? accent : border);
        }

        // Bordures latérales
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }

        // Bordure basse
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Si mode daily, remplir les lignes 2-3 avec des décorations
        if (mode == ViewMode.DAILY) {
            ItemStack empty = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
            for (int i = 10; i <= 16; i++) inventory.setItem(i, empty);
            for (int i = 28; i <= 34; i++) inventory.setItem(i, empty);
        }
    }

    /**
     * Crée l'en-tête avec les onglets
     */
    private void createHeader(PlayerMissions playerMissions) {
        long resetTime = mode == ViewMode.DAILY ?
            missionManager.getTimeUntilDailyReset() :
            missionManager.getTimeUntilWeeklyReset();

        // Onglet Daily (slot 2)
        boolean isDailyActive = mode == ViewMode.DAILY;
        inventory.setItem(2, new ItemBuilder(Material.SUNFLOWER)
            .name((isDailyActive ? "§e§l" : "§7") + "☀ Journalières")
            .lore(
                isDailyActive ? "§a▶ Sélectionné" : "§7Cliquez pour voir",
                "",
                "§77 missions par jour",
                "§7Reset: §e" + formatTime(missionManager.getTimeUntilDailyReset())
            )
            .glow(isDailyActive)
            .build());

        // Timer central (slot 4)
        inventory.setItem(4, new ItemBuilder(Material.CLOCK)
            .name("§6⏱ Reset dans")
            .lore(
                "§e§l" + formatTime(resetTime),
                "",
                "§7Les missions se",
                "§7réinitialisent à minuit"
            )
            .build());

        // Onglet Weekly (slot 6)
        boolean isWeeklyActive = mode == ViewMode.WEEKLY;
        inventory.setItem(6, new ItemBuilder(Material.NETHER_STAR)
            .name((isWeeklyActive ? "§d§l" : "§7") + "✦ Hebdomadaires")
            .lore(
                isWeeklyActive ? "§a▶ Sélectionné" : "§7Cliquez pour voir",
                "",
                "§721 missions par semaine",
                "§7Reset: §d" + formatTime(missionManager.getTimeUntilWeeklyReset())
            )
            .glow(isWeeklyActive)
            .build());
    }

    /**
     * Affiche le layout des missions journalières (7 missions sur une ligne)
     */
    private void createDailyLayout(PlayerMissions playerMissions) {
        Collection<MissionProgress> missions = playerMissions.getDailyMissions().values();
        List<MissionProgress> missionList = new ArrayList<>(missions);

        // Titre de section
        inventory.setItem(13, new ItemBuilder(Material.PAPER)
            .name("§e§lMissions du Jour")
            .lore(
                "§7Complétez vos 7 missions",
                "§7pour obtenir le bonus!",
                "",
                "§eRécompenses variées:",
                "§6• Points §7et §bXP",
                "§7• Bonus de complétion"
            )
            .build());

        // Afficher les 7 missions (ligne du milieu)
        for (int i = 0; i < Math.min(DAILY_SLOTS.length, missionList.size()); i++) {
            int slot = DAILY_SLOTS[i];
            MissionProgress progress = missionList.get(i);
            inventory.setItem(slot, createMissionItem(progress, i + 1));
            slotMapping.put(slot, progress);
        }

        // Indicateur de catégories sous les missions
        inventory.setItem(31, new ItemBuilder(Material.BOOK)
            .name("§6Catégories")
            .lore(
                "§c⚔ Combat §7- Tuer des zombies",
                "§a🧭 Exploration §7- Découvrir le monde",
                "§e📦 Collection §7- Ramasser des items",
                "§b👥 Social §7- Jouer avec d'autres",
                "§d⚡ Événements §7- Participer aux events"
            )
            .build());
    }

    /**
     * Affiche le layout des missions hebdomadaires (21 missions sur 3 lignes)
     */
    private void createWeeklyLayout(PlayerMissions playerMissions) {
        Collection<MissionProgress> missions = playerMissions.getWeeklyMissions().values();
        List<MissionProgress> missionList = new ArrayList<>(missions);

        // Afficher les 21 missions (3 lignes de 7)
        for (int i = 0; i < Math.min(WEEKLY_SLOTS.length, missionList.size()); i++) {
            int slot = WEEKLY_SLOTS[i];
            MissionProgress progress = missionList.get(i);
            inventory.setItem(slot, createMissionItem(progress, i + 1));
            slotMapping.put(slot, progress);
        }
    }

    /**
     * Crée le pied de page avec stats et boutons
     */
    private void createFooter(PlayerMissions playerMissions) {
        int completed, total;
        if (mode == ViewMode.DAILY) {
            completed = playerMissions.getCompletedDailyCount();
            total = playerMissions.getDailyMissions().size();
        } else {
            completed = playerMissions.getCompletedWeeklyCount();
            total = playerMissions.getWeeklyMissions().size();
        }

        // Stats de progression (slot 47)
        String progressBar = createProgressBar(completed, total);
        double percent = total > 0 ? (double) completed / total * 100 : 0;

        inventory.setItem(47, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§6§lProgression")
            .lore(
                "",
                progressBar,
                "",
                "§7Complétées: §a" + completed + "§7/§e" + total,
                "§7Pourcentage: §e" + String.format("%.0f", percent) + "%",
                "",
                completed == total && total > 0 ? "§a✓ Toutes complétées!" : "§7Continue comme ça!"
            )
            .build());

        // Bouton fermer (slot 49)
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§c✕ Fermer")
            .lore("§7Cliquez pour fermer")
            .build());

        // Bonus de complétion (slot 51)
        if (completed == total && total > 0) {
            String bonusText = mode == ViewMode.DAILY ?
                "§6+500 Points" :
                "§6+2000 Points §7+ §d+25 Gemmes";

            inventory.setItem(51, new ItemBuilder(Material.CHEST)
                .name("§a§l✓ BONUS DISPONIBLE!")
                .lore(
                    "§7Toutes les missions complétées!",
                    "",
                    "§eCliquez pour réclamer:",
                    bonusText,
                    "",
                    "§a▶ Cliquez ici!"
                )
                .glow(true)
                .build());
        } else {
            // Afficher ce qu'il reste à faire
            int remaining = total - completed;
            inventory.setItem(51, new ItemBuilder(Material.CHEST_MINECART)
                .name("§7Bonus de Complétion")
                .lore(
                    "§7Complétez toutes les missions",
                    "§7pour débloquer le bonus!",
                    "",
                    "§7Restantes: §c" + remaining + " missions",
                    "",
                    mode == ViewMode.DAILY ?
                        "§6Bonus: +500 Points" :
                        "§6Bonus: +2000 Points + 25 Gemmes"
                )
                .build());
        }
    }

    /**
     * Crée l'item d'une mission
     */
    private ItemStack createMissionItem(MissionProgress progress, int index) {
        Mission mission = progress.getMission();

        Material mat = progress.isCompleted() ? Material.LIME_DYE : mission.getIcon();
        String statusIcon = progress.isCompleted() ? "§a✓" : "§e" + index;
        String nameColor = progress.isCompleted() ? "§a§m" : "§f";

        ItemBuilder builder = new ItemBuilder(mat)
            .name(statusIcon + " " + nameColor + mission.getName());

        List<String> lore = new ArrayList<>();

        // Description
        lore.add("§7" + mission.getDescription());
        lore.add("");

        // Catégorie avec couleur
        lore.add("§7Type: " + mission.getCategory().getColor() + mission.getCategory().getDisplayName());

        // Difficulté avec étoiles colorées
        lore.add("§7Difficulté: " + getDifficultyStars(mission.getDifficulty()));
        lore.add("");

        // Progrès avec barre
        if (progress.isCompleted()) {
            lore.add("§a§l✓ COMPLÉTÉE!");
            lore.add("");
        } else {
            int current = progress.getProgress();
            int goal = mission.getGoal();
            double percent = progress.getProgressPercent();

            lore.add("§7Progrès:");
            lore.add(createMiniProgressBar(percent) + " §7" + current + "/" + goal);
            lore.add("");
        }

        // Récompenses avec icônes
        lore.add("§e§lRécompenses:");
        lore.add("§6  ⛁ " + formatNumber(mission.getPointReward()) + " Points");
        lore.add("§b  ✧ " + formatNumber(mission.getXpReward()) + " XP");
        if (mission.getGemReward() > 0) {
            lore.add("§d  💎 " + mission.getGemReward() + " Gemmes");
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
        if (slot < 0 || slot >= 54) return;

        // Onglet Daily
        if (slot == 2 && mode != ViewMode.DAILY) {
            mode = ViewMode.DAILY;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            return;
        }

        // Onglet Weekly
        if (slot == 6 && mode != ViewMode.WEEKLY) {
            mode = ViewMode.WEEKLY;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            return;
        }

        // Fermer
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }

        // Bonus de complétion
        if (slot == 51) {
            claimBonus();
        }
    }

    /**
     * Réclame le bonus de complétion
     */
    private void claimBonus() {
        PlayerMissions missions = missionManager.getMissions(player.getUniqueId());
        int completed = mode == ViewMode.DAILY ?
            missions.getCompletedDailyCount() :
            missions.getCompletedWeeklyCount();
        int total = (mode == ViewMode.DAILY ? missions.getDailyMissions() : missions.getWeeklyMissions()).size();

        if (completed == total && total > 0) {
            if (mode == ViewMode.DAILY) {
                plugin.getEconomyManager().addPoints(player, 500);
                player.sendMessage("");
                player.sendMessage("§a§l★ BONUS JOURNALIER RÉCLAMÉ! §r§a+500 Points");
                player.sendMessage("");
            } else {
                plugin.getEconomyManager().addPoints(player, 2000);
                plugin.getEconomyManager().addGems(player, 25);
                player.sendMessage("");
                player.sendMessage("§d§l★ BONUS HEBDOMADAIRE RÉCLAMÉ!");
                player.sendMessage("§6   +2000 Points §7+ §d+25 Gemmes");
                player.sendMessage("");
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 86400) {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "j " + hours + "h";
        } else if (seconds >= 3600) {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            return hours + "h " + mins + "m";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format("%.1fk", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private String getDifficultyStars(int difficulty) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < difficulty) {
                // Couleur selon difficulté
                if (difficulty <= 2) sb.append("§a");
                else if (difficulty <= 3) sb.append("§e");
                else if (difficulty <= 4) sb.append("§6");
                else sb.append("§c");
                sb.append("★");
            } else {
                sb.append("§8☆");
            }
        }
        return sb.toString();
    }

    private String createProgressBar(int current, int max) {
        int total = 20;
        int filled = max > 0 ? (current * total / max) : 0;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§8░");
            }
        }
        return bar.toString();
    }

    private String createMiniProgressBar(double percent) {
        int filled = (int) (percent / 10);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a▰");
            } else {
                bar.append("§7▱");
            }
        }

        // Couleur du pourcentage selon progression
        String color;
        if (percent >= 100) color = "§a";
        else if (percent >= 75) color = "§e";
        else if (percent >= 50) color = "§6";
        else color = "§c";

        return bar + " " + color + String.format("%.0f%%", percent);
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
        private final Map<Player, MissionGUI> activeGUIs = new WeakHashMap<>();

        public MissionGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;

            String title = event.getView().getTitle();
            if (!title.contains("MISSIONS")) return;

            // Récupérer ou créer le GUI
            MissionGUI gui = activeGUIs.computeIfAbsent(player, p -> {
                MissionGUI newGui = new MissionGUI(plugin, p);
                newGui.inventory = event.getInventory();
                // Détecter le mode actuel
                if (title.contains("HEBDOMADAIRES")) {
                    newGui.mode = ViewMode.WEEKLY;
                }
                return newGui;
            });

            gui.inventory = event.getInventory();
            gui.handleClick(event);
        }
    }
}
