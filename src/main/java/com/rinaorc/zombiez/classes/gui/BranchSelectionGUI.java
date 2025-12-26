package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
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
 * GUI de sélection de branche de talents
 * Le joueur doit choisir une spécialisation avant de voir ses talents
 */
public class BranchSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private static final String GUI_TITLE = "§0§l+ SPÉCIALISATION +";

    // Slots pour les 5 branches (centrées)
    private static final int[] BRANCH_SLOTS = {11, 12, 13, 14, 15};
    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 31;

    public BranchSelectionGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 36, GUI_TITLE);
        ClassType classType = data.getSelectedClass();

        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, border);
        }

        // Info centrale en haut
        TalentBranch currentBranch = data.getSelectedBranch();
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§7Choisissez votre §evoie§7.");
        infoLore.add("§7Tous vos talents seront de cette voie.");
        infoLore.add("");

        if (currentBranch != null) {
            infoLore.add("§7Voie actuelle: " + currentBranch.getColoredName());
            if (data.isOnBranchChangeCooldown()) {
                long remaining = data.getBranchChangeCooldownRemaining();
                long minutes = remaining / (60 * 1000);
                infoLore.add("");
                infoLore.add("§cChangement en cooldown: §f" + minutes + " min");
            }
        } else {
            infoLore.add("§eAucune voie sélectionnée");
        }

        gui.setItem(SLOT_INFO, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lSPÉCIALISATION")
            .lore(infoLore)
            .build());

        // Branches disponibles
        TalentBranch[] branches = TalentBranch.getBranchesForClass(classType);
        for (int i = 0; i < branches.length && i < BRANCH_SLOTS.length; i++) {
            TalentBranch branch = branches[i];
            boolean isSelected = currentBranch == branch;
            boolean canSelect = !data.isOnBranchChangeCooldown() || currentBranch == null;

            gui.setItem(BRANCH_SLOTS[i], createBranchItem(branch, isSelected, canSelect, data));
        }

        // Bouton retour
        gui.setItem(SLOT_BACK, new ItemBuilder(Material.DARK_OAK_DOOR)
            .name("§7< Retour")
            .lore("", "§8Retour aux infos de classe")
            .build());

        player.openInventory(gui);
    }

    private ItemStack createBranchItem(TalentBranch branch, boolean isSelected, boolean canSelect, ClassData data) {
        List<String> lore = new ArrayList<>();

        // Description de la branche
        for (String line : branch.getDescription()) {
            lore.add(line);
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");

        if (isSelected) {
            lore.add("§a✓ SÉLECTIONNÉE");
        } else if (!canSelect) {
            long remaining = data.getBranchChangeCooldownRemaining();
            long minutes = remaining / (60 * 1000);
            lore.add("§cChangement en cooldown");
            lore.add("§7" + minutes + " min restantes");
        } else {
            lore.add("§e> Clic pour sélectionner");
            if (data.hasBranch()) {
                lore.add("");
                lore.add("§c⚠ Changer de voie reset");
                lore.add("§c  tous vos talents!");
            }
        }

        Material icon = isSelected ? branch.getIcon() : Material.GRAY_DYE;

        return new ItemBuilder(icon)
            .name((isSelected ? "§a" : branch.getColor()) + "§l" + branch.getDisplayName().toUpperCase())
            .lore(lore)
            .glow(isSelected)
            .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        ClassData data = classManager.getClassData(player);

        // Retour
        if (slot == SLOT_BACK) {
            player.closeInventory();
            plugin.getClassInfoGUI().open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Clic sur une branche
        for (int i = 0; i < BRANCH_SLOTS.length; i++) {
            if (BRANCH_SLOTS[i] == slot) {
                TalentBranch[] branches = TalentBranch.getBranchesForClass(data.getSelectedClass());
                if (i < branches.length) {
                    handleBranchClick(player, branches[i], data);
                }
                return;
            }
        }
    }

    private void handleBranchClick(Player player, TalentBranch branch, ClassData data) {
        TalentBranch currentBranch = data.getSelectedBranch();

        // Déjà sélectionnée
        if (currentBranch == branch) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
            player.sendMessage("§7Cette voie est déjà sélectionnée.");
            // Ouvrir le menu des talents directement
            player.closeInventory();
            plugin.getTalentSelectionGUI().open(player);
            return;
        }

        // Vérifier le cooldown
        if (data.isOnBranchChangeCooldown() && currentBranch != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            long minutes = data.getBranchChangeCooldownRemaining() / (60 * 1000);
            player.sendMessage("§cVous ne pouvez pas encore changer de voie!");
            player.sendMessage("§7Temps restant: §f" + minutes + " minutes");
            return;
        }

        // Sélectionner la voie
        boolean hadBranch = data.hasBranch();
        if (data.selectBranch(branch)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            if (data.isTalentMessagesEnabled()) {
                player.sendMessage("");
                player.sendMessage("§a§l+ VOIE SÉLECTIONNÉE +");
                player.sendMessage("§7Spécialisation: " + branch.getColoredName());
                if (hadBranch) {
                    player.sendMessage("§c§oVos anciens talents ont été réinitialisés.");
                }
                player.sendMessage("");
            }

            // Synchroniser les bêtes (despawn si changement de voie hors bêtes)
            if (plugin.getBeastManager() != null) {
                plugin.getBeastManager().syncBeastsWithTalents(player);
            }

            // Notifier le système de Journey (étape 2.1 en 2 temps)
            if (plugin.getJourneyListener() != null) {
                plugin.getJourneyListener().onBranchSelect(player);
            }

            // Ouvrir le menu des talents
            player.closeInventory();
            plugin.getTalentSelectionGUI().open(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cImpossible de sélectionner cette voie!");
        }
    }
}
