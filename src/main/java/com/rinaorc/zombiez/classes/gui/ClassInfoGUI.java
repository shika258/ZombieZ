package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.TalentTier;
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

    // Slots - Layout réorganisé
    private static final int SLOT_CLASS_INFO = 4;       // Haut centre - Info générale
    private static final int SLOT_LEVEL_INFO = 19;      // Milieu gauche - Niveau (décalé 2 slots à gauche)
    private static final int SLOT_TALENTS = 21;         // Milieu - Talents (monté 1 ligne)
    private static final int SLOT_TRAITS_INFO = 23;     // Milieu droite - Traits de classe
    private static final int SLOT_TALENT_MESSAGES_TOGGLE = 25;  // Milieu droite - Toggle (monté 1 ligne)
    private static final int SLOT_CHANGE_CLASS = 31;    // Bas centre - Changer (décalé 1 slot à gauche)
    private static final int SLOT_CLOSE = 40;           // Fermer

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

        // === INFO CLASSE (haut centre) ===
        gui.setItem(SLOT_CLASS_INFO, createClassInfoItem(classType, data));

        // Séparateur visuel
        ItemStack separator = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 9; i < 18; i++) {
            gui.setItem(i, separator);
        }

        // === TRAITS DE CLASSE (milieu droite) - Seule source de bonus de classe ===
        gui.setItem(SLOT_TRAITS_INFO, createTraitsItem(classType));

        // Séparateur bas (autour du bouton changer de classe)
        for (int i = 27; i < 31; i++) {
            gui.setItem(i, separator);
        }
        for (int i = 32; i < 36; i++) {
            gui.setItem(i, separator);
        }

        // === NIVEAU DE CLASSE (milieu gauche) ===
        gui.setItem(SLOT_LEVEL_INFO, createLevelItem(data));

        // === TALENTS (milieu) ===
        gui.setItem(SLOT_TALENTS, createTalentsButton(data));

        // === TOGGLE MESSAGES DE TALENTS (milieu droite) ===
        gui.setItem(SLOT_TALENT_MESSAGES_TOGGLE, createTalentMessagesToggle(data));

        // === CHANGER DE CLASSE (bas centre) ===
        gui.setItem(SLOT_CHANGE_CLASS, createChangeClassButton(data));

        // Bouton fermer
        gui.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c✕ Fermer")
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

    private ItemStack createTraitsItem(ClassType classType) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§6§lTraits uniques de classe:");
        lore.add("");

        // Afficher chaque trait
        for (ClassType.ClassTrait trait : classType.getClassTraits()) {
            lore.add(trait.getName());
            lore.add("  " + trait.getDescription());
            lore.add("");
        }

        lore.add("§8──────────────");
        lore.add("§8Ces bonus passifs sont");
        lore.add("§8toujours actifs en combat");

        return new ItemBuilder(Material.NETHER_STAR)
            .name(classType.getColor() + "§lTRAITS DE CLASSE")
            .lore(lore)
            .glow(true)
            .build();
    }

    private ItemStack createTalentsButton(ClassData data) {
        int unlockedTiers = data.getUnlockedTierCount();
        int selectedTalents = data.getSelectedTalentCount();
        int totalTiers = TalentTier.values().length;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Paliers debloques: §e" + unlockedTiers + "§7/§e" + totalTiers);
        lore.add("§7Talents actifs: §a" + selectedTalents);
        lore.add("");

        // Verifier s'il y a des talents non selectionnes
        int unselected = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (data.isTalentTierUnlocked(tier) && data.getSelectedTalentId(tier) == null) {
                unselected++;
            }
        }

        if (unselected > 0) {
            lore.add("§c! " + unselected + " talent(s) a selectionner !");
            lore.add("");
        }

        lore.add("§e> Clic pour gerer vos talents");

        return new ItemBuilder(unselected > 0 ? Material.ENCHANTED_BOOK : Material.BOOK)
            .name("§d§lVOIE DES TALENTS")
            .lore(lore)
            .glow(unselected > 0)
            .build();
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

    private ItemStack createTalentMessagesToggle(ClassData data) {
        boolean enabled = data.isTalentMessagesEnabled();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Statut: " + (enabled ? "§aActivé" : "§cDésactivé"));
        lore.add("");
        lore.add("§7Contrôle les messages de notification");
        lore.add("§7des talents dans le chat:");
        lore.add("§8- Sélection de talent");
        lore.add("§8- Talents disponibles");
        lore.add("");
        lore.add("§e> Clic pour " + (enabled ? "désactiver" : "activer"));

        return new ItemBuilder(enabled ? Material.BELL : Material.BARRIER)
            .name(enabled ? "§a§lMESSAGES TALENTS" : "§c§lMESSAGES TALENTS")
            .lore(lore)
            .glow(enabled)
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
            case SLOT_TALENTS -> {
                player.closeInventory();
                plugin.getTalentSelectionGUI().open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            case SLOT_CHANGE_CLASS -> {
                ClassData data = classManager.getClassData(player);
                long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
                long cooldown = 24 * 60 * 60 * 1000;

                if (timeSinceChange >= cooldown) {
                    player.closeInventory();
                    plugin.getClassSelectionGUI().open(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            case SLOT_TALENT_MESSAGES_TOGGLE -> {
                ClassData data = classManager.getClassData(player);
                boolean newState = data.toggleTalentMessages();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(newState
                    ? "§a§l+ Messages de talents activés"
                    : "§c§l- Messages de talents désactivés");
                // Refresh le menu
                open(player);
            }
            case SLOT_CLOSE -> player.closeInventory();
        }
    }
}
