package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
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
 * GUI d'information de classe
 * Affiche les details de la classe selectionnee et le niveau
 */
public class ClassInfoGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private static final String GUI_TITLE = "§0§l+ MA CLASSE +";

    // Slots
    private static final int SLOT_CLASS_INFO = 13;
    private static final int SLOT_LEVEL_INFO = 11;
    private static final int SLOT_STATS_INFO = 15;
    private static final int SLOT_CHANGE_CLASS = 31;
    private static final int SLOT_CLOSE = 36;

    public ClassInfoGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cVous n'avez pas encore de classe!");
            player.sendMessage("§7Utilisez §e/class §7pour en choisir une.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        ClassType classType = data.getSelectedClass();

        // Bordure
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, border);
        }

        // Titre
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lMA CLASSE")
            .lore(
                "",
                "§7Consultez les informations",
                "§7de votre classe actuelle."
            )
            .build());

        // === NIVEAU DE CLASSE ===
        gui.setItem(SLOT_LEVEL_INFO, createLevelItem(data));

        // === INFO CLASSE ===
        gui.setItem(SLOT_CLASS_INFO, createClassInfoItem(classType, data));

        // === STATS DE CLASSE ===
        gui.setItem(SLOT_STATS_INFO, createStatsItem(classType));

        // === CHANGER DE CLASSE ===
        gui.setItem(SLOT_CHANGE_CLASS, createChangeClassButton(data));

        // Bouton fermer
        gui.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c+ Fermer")
            .build());

        player.openInventory(gui);
    }

    private ItemStack createLevelItem(ClassData data) {
        int level = data.getClassLevel().get();
        double progress = data.getClassLevelProgress();
        long currentXp = data.getClassXp().get();
        long requiredXp = data.getRequiredXpForNextClassLevel();

        // Barre de progression
        int progressBars = (int) (progress / 5); // 20 barres max
        StringBuilder progressBar = new StringBuilder("§a");
        for (int i = 0; i < 20; i++) {
            if (i < progressBars) {
                progressBar.append("|");
            } else {
                if (i == progressBars) progressBar.append("§7");
                progressBar.append("|");
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§eNiveau actuel: §f" + level);
        lore.add("");
        lore.add("§7Progression:");
        lore.add(progressBar.toString());
        lore.add("§7" + String.format("%.1f", progress) + "%");
        lore.add("");
        lore.add("§7XP: §f" + currentXp + " §7/ §f" + requiredXp);
        lore.add("");
        lore.add("§8Gagnez de l'XP en tuant des zombies");

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§a§lNIVEAU DE CLASSE")
            .lore(lore)
            .build();
    }

    private ItemStack createClassInfoItem(ClassType classType, ClassData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(classType.getDescription());
        lore.add("");
        lore.add("§7Difficulte: " + classType.getDifficultyDisplay());
        lore.add("");
        lore.add("§6Bonus de classe:");
        for (String bonus : classType.getBonusDescription()) {
            lore.add(bonus);
        }
        lore.add("");
        lore.add("§8──────────────");
        lore.add("");
        lore.add("§7Kills: §f" + data.getClassKills().get());
        lore.add("§7Morts: §f" + data.getClassDeaths().get());
        lore.add("§7K/D: §f" + String.format("%.2f", data.getClassKDRatio()));

        return new ItemBuilder(classType.getIcon())
            .name(classType.getColoredName())
            .lore(lore)
            .glow(true)
            .build();
    }

    private ItemStack createStatsItem(ClassType classType) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Les multiplicateurs de votre classe:");
        lore.add("");

        // Formatage des stats avec couleurs
        double dmg = classType.getDamageMultiplier();
        double hp = classType.getHealthMultiplier();
        double spd = classType.getSpeedMultiplier();
        double crit = classType.getCritMultiplier();
        double ls = classType.getLifesteal() * 100;

        lore.add(formatStat("Degats", dmg, 1.0));
        lore.add(formatStat("Vie", hp, 1.0));
        lore.add(formatStat("Vitesse", spd, 1.0));
        lore.add(formatStat("Critique", crit, 1.0));

        if (ls > 0) {
            lore.add("§7Vol de vie: §a+" + (int) ls + "%");
        }

        lore.add("");
        lore.add("§8Ces stats sont appliquees automatiquement");

        return new ItemBuilder(Material.GOLDEN_SWORD)
            .name("§e§lSTATISTIQUES")
            .lore(lore)
            .build();
    }

    private String formatStat(String name, double value, double base) {
        String color;
        String sign;
        int percent = (int) ((value - base) * 100);

        if (percent > 0) {
            color = "§a";
            sign = "+";
        } else if (percent < 0) {
            color = "§c";
            sign = "";
        } else {
            color = "§f";
            sign = "";
        }

        return "§7" + name + ": " + color + sign + percent + "%";
    }

    private ItemStack createChangeClassButton(ClassData data) {
        long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
        long cooldown = 24 * 60 * 60 * 1000;
        boolean canChange = timeSinceChange >= cooldown;

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (canChange) {
            lore.add("§aVous pouvez changer de classe!");
            lore.add("");
            lore.add("§e> Clic pour ouvrir le menu de selection");
        } else {
            long remaining = cooldown - timeSinceChange;
            long hours = remaining / (60 * 60 * 1000);
            long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
            lore.add("§cChangement en cooldown");
            lore.add("");
            lore.add("§7Temps restant: §f" + hours + "h " + minutes + "min");
        }

        return new ItemBuilder(canChange ? Material.LIME_DYE : Material.GRAY_DYE)
            .name(canChange ? "§a§lCHANGER DE CLASSE" : "§7§lCHANGER DE CLASSE")
            .lore(lore)
            .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_CHANGE_CLASS -> {
                ClassData data = classManager.getClassData(player);
                long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
                long cooldown = 24 * 60 * 60 * 1000;

                if (timeSinceChange >= cooldown) {
                    player.closeInventory();
                    new ClassSelectionGUI(plugin, classManager).open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            case SLOT_CLOSE -> player.closeInventory();
        }
    }
}
