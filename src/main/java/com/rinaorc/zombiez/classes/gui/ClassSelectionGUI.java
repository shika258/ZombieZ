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
 * GUI de sélection de classe simplifié - 3 classes
 * Interface accessible et claire avec descriptions détaillées
 */
public class ClassSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private static final String GUI_TITLE = "§0§l✦ CHOIX DE CLASSE ✦";

    // Slots des 3 classes (centré)
    private static final int SLOT_GUERRIER = 11;
    private static final int SLOT_CHASSEUR = 13;
    private static final int SLOT_OCCULTISTE = 15;

    public ClassSelectionGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        // Vérifier si le joueur peut accéder à la sélection de classe via le système de Journey
        if (!plugin.getJourneyManager().canAccessClassSelection(player)) {
            player.sendMessage("");
            player.sendMessage("§c§l⚠ SÉLECTION DE CLASSE VERROUILLÉE");
            player.sendMessage("");
            player.sendMessage("§7Tu dois d'abord terminer le §eChapitre 1§7 du Parcours");
            player.sendMessage("§7pour débloquer la sélection de classe.");
            player.sendMessage("");
            player.sendMessage("§7Tape §e/journey §7pour voir ta progression!");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);
        ClassData data = classManager.getClassData(player);

        // Bordure
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, border);
        }

        // Titre
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lCHOIX DE CLASSE")
            .lore(
                "",
                "§7Choisissez une classe qui",
                "§7correspond à votre style de jeu.",
                "",
                data.hasClass()
                    ? "§aClasse actuelle: " + data.getSelectedClass().getColoredName()
                    : "§eCliquez sur une classe pour la sélectionner"
            )
            .build());

        // === GUERRIER ===
        gui.setItem(SLOT_GUERRIER, createClassItem(ClassType.GUERRIER, data));

        // === CHASSEUR ===
        gui.setItem(SLOT_CHASSEUR, createClassItem(ClassType.CHASSEUR, data));

        // === OCCULTISTE ===
        gui.setItem(SLOT_OCCULTISTE, createClassItem(ClassType.OCCULTISTE, data));

        // Bouton fermer
        gui.setItem(22, new ItemBuilder(Material.BARRIER)
            .name("§c✕ Fermer")
            .build());

        player.openInventory(gui);
    }

    private ItemStack createClassItem(ClassType classType, ClassData data) {
        boolean isSelected = data.hasClass() && data.getSelectedClass() == classType;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(classType.getDescription());
        lore.add("");
        lore.add("§7Difficulté: " + classType.getDifficultyDisplay());
        lore.add("");

        // Section Stats de base
        lore.add("§e§lStats de base:");
        lore.add(formatStatCompact("Dégâts", classType.getDamageMultiplier()));
        lore.add(formatStatCompact("Vie", classType.getHealthMultiplier()));
        lore.add(formatStatCompact("Vitesse", classType.getSpeedMultiplier()));
        lore.add(formatStatCompact("Critique", classType.getCritMultiplier()));
        if (classType.getLifesteal() > 0) {
            lore.add("§7Vol de vie: §a+" + (int)(classType.getLifesteal() * 100) + "%");
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");

        // Section Traits de classe
        lore.add(classType.getColor() + "§lTraits de classe:");
        for (ClassType.ClassTrait trait : classType.getClassTraits()) {
            lore.add(trait.getName());
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");

        if (isSelected) {
            lore.add("§a✓ CLASSE ACTUELLE");
        } else {
            lore.add("§e▶ Clic pour sélectionner");
        }

        return new ItemBuilder(isSelected ? Material.ENCHANTED_GOLDEN_APPLE : classType.getIcon())
            .name(classType.getColoredName() + (isSelected ? " §a✓" : ""))
            .lore(lore)
            .glow(isSelected)
            .build();
    }

    private String formatStatCompact(String name, double value) {
        int percent = (int) ((value - 1.0) * 100);
        String color = percent > 0 ? "§a+" : (percent < 0 ? "§c" : "§f");
        return "§7" + name + ": " + color + percent + "%";
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        ClassType selectedClass = switch (slot) {
            case SLOT_GUERRIER -> ClassType.GUERRIER;
            case SLOT_CHASSEUR -> ClassType.CHASSEUR;
            case SLOT_OCCULTISTE -> ClassType.OCCULTISTE;
            default -> null;
        };

        if (selectedClass != null) {
            if (classManager.selectClass(player, selectedClass)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.closeInventory();

                // Ouvrir le menu d'info de classe apres
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getClassInfoGUI().open(player), 20L);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }

        if (slot == 22) {
            player.closeInventory();
        }
    }
}
