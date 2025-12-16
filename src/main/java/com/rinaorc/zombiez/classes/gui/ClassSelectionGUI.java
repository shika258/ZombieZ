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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI de sélection de classe
 * Affiche les 6 classes avec leurs descriptions et bonus
 */
public class ClassSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private static final String GUI_TITLE = "§0§l✦ CHOIX DE CLASSE ✦";
    private static final Map<Integer, ClassType> SLOT_TO_CLASS = new HashMap<>();

    static {
        // Positions des classes dans le GUI (format 6x9)
        SLOT_TO_CLASS.put(10, ClassType.COMMANDO);
        SLOT_TO_CLASS.put(12, ClassType.SCOUT);
        SLOT_TO_CLASS.put(14, ClassType.MEDIC);
        SLOT_TO_CLASS.put(16, ClassType.ENGINEER);
        SLOT_TO_CLASS.put(28, ClassType.BERSERKER);
        SLOT_TO_CLASS.put(34, ClassType.SNIPER);
    }

    public ClassSelectionGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le GUI de sélection de classe
     */
    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        ClassData data = classManager.getClassData(player);

        // Bordure décorative
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(" ")
            .build();

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }

        // Titre central
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lCHOISSIS TA CLASSE")
            .lore(
                "",
                "§7Chaque classe a un style de jeu unique",
                "§7avec ses propres talents, compétences",
                "§7et armes exclusives.",
                "",
                data.hasClass()
                    ? "§aClasse actuelle: " + data.getSelectedClass().getColoredName()
                    : "§cAucune classe sélectionnée"
            )
            .build());

        // Afficher les 6 classes
        for (Map.Entry<Integer, ClassType> entry : SLOT_TO_CLASS.entrySet()) {
            ClassType classType = entry.getValue();
            boolean isSelected = data.getSelectedClass() == classType;

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(classType.getDescription());
            lore.add("");
            lore.add("§6Bonus de classe:");
            for (String bonus : classType.getBonusDescription()) {
                lore.add(bonus);
            }
            lore.add("");
            lore.add("§eMultiplicateurs:");
            lore.add("§7• Dégâts: §f" + formatMultiplier(classType.getDamageMultiplier()));
            lore.add("§7• Vitesse: §f" + formatMultiplier(classType.getSpeedMultiplier()));
            lore.add("§7• HP: §f" + formatMultiplier(classType.getHealthMultiplier()));
            lore.add("§7• Critique: §f" + formatMultiplier(classType.getCritMultiplier()));
            lore.add("§7• Loot: §f" + formatMultiplier(classType.getLootMultiplier()));
            lore.add("");

            if (isSelected) {
                lore.add("§a✓ CLASSE ACTUELLE");
            } else {
                lore.add("§e▶ Clic pour sélectionner");
            }

            Material icon = isSelected
                ? Material.ENCHANTED_GOLDEN_APPLE
                : classType.getIcon();

            ItemStack item = new ItemBuilder(icon)
                .name(classType.getColoredName() + (isSelected ? " §a✓" : ""))
                .lore(lore)
                .glow(isSelected)
                .build();

            gui.setItem(entry.getKey(), item);
        }

        // Bouton retour
        gui.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§c← Retour")
            .lore("", "§7Retourner au menu principal")
            .build());

        // Info supplémentaire au milieu bas
        gui.setItem(31, new ItemBuilder(Material.BOOK)
            .name("§e§lINFORMATIONS")
            .lore(
                "",
                "§7• Le changement de classe a un",
                "§7  cooldown de §f24 heures",
                "",
                "§7• Vos talents et buffs sont",
                "§7  conservés par classe",
                "",
                "§7• Chaque classe a §f3 armes uniques",
                "§7  et §f6 compétences actives"
            )
            .build());

        player.openInventory(gui);
    }

    private String formatMultiplier(double mult) {
        if (mult == 1.0) return "§7100%";
        if (mult > 1.0) return "§a+" + (int)((mult - 1) * 100) + "%";
        return "§c" + (int)((mult - 1) * 100) + "%";
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();

        // Clic sur une classe
        if (SLOT_TO_CLASS.containsKey(slot)) {
            ClassType selectedClass = SLOT_TO_CLASS.get(slot);

            if (classManager.selectClass(player, selectedClass)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.closeInventory();

                // Ouvrir le GUI des talents après sélection
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new TalentTreeGUI(plugin, classManager).open(player);
                }, 20L);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        // Bouton retour
        if (slot == 49) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
