package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.ClassTalent;
import com.rinaorc.zombiez.classes.talents.ClassTalent.TalentBranch;
import com.rinaorc.zombiez.classes.talents.ClassTalentTree;
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
 * GUI des talents simplifié - 2 branches par classe
 * Branche COMBAT (gauche) et Branche SURVIE (droite)
 * 5 talents par branche, progression linéaire
 */
public class TalentTreeGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final ClassTalentTree talentTree;

    private static final String GUI_TITLE_PREFIX = "§0§l✦ TALENTS: ";
    private final Map<Integer, String> slotToTalent = new HashMap<>();

    // Layout: 2 branches, 5 tiers chacune
    // Branche OFFENSE: colonne 2
    // Branche DEFENSE: colonne 6

    public TalentTreeGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.talentTree = classManager.getTalentTree();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cChoisissez d'abord une classe!");
            new ClassSelectionGUI(plugin, classManager).open(player);
            return;
        }

        ClassType classType = data.getSelectedClass();
        String title = GUI_TITLE_PREFIX + classType.getDisplayName().toUpperCase() + " ✦";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        slotToTalent.clear();

        // Fond
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, bg);
        }

        // Header - Info classe
        gui.setItem(4, new ItemBuilder(classType.getIcon())
            .name(classType.getColoredName() + " §7Nv." + data.getClassLevel().get())
            .lore(
                "",
                "§ePoints disponibles: §f" + data.getAvailableTalentPoints(),
                "§7XP: §f" + data.getClassXp().get() + "/" + data.getRequiredXpForNextClassLevel(),
                "",
                "§8Clic droit = Changer de classe"
            )
            .build());

        // Titre branche COMBAT
        gui.setItem(1, new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§c§lBRANCHE COMBAT")
            .lore("", "§7Augmente vos dégâts", "§7et votre potentiel offensif")
            .build());

        // Titre branche SURVIE
        gui.setItem(7, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§6§lBRANCHE SURVIE")
            .lore("", "§7Augmente votre résistance", "§7et votre utilité")
            .build());

        // Afficher les talents OFFENSE (colonne 2)
        List<ClassTalent> offenseTalents = talentTree.getTalentsForBranch(classType, TalentBranch.OFFENSE);
        for (ClassTalent talent : offenseTalents) {
            int slot = 11 + (talent.getTier() - 1) * 9; // Tiers 1-5
            placeTalent(gui, slot, talent, data);
        }

        // Afficher les talents DEFENSE (colonne 6)
        List<ClassTalent> defenseTalents = talentTree.getTalentsForBranch(classType, TalentBranch.DEFENSE);
        for (ClassTalent talent : defenseTalents) {
            int slot = 15 + (talent.getTier() - 1) * 9; // Tiers 1-5
            placeTalent(gui, slot, talent, data);
        }

        // Lignes de connexion (visuelles)
        for (int tier = 1; tier <= 4; tier++) {
            int offenseSlot = 11 + tier * 9 - 9 + 9;
            int defenseSlot = 15 + tier * 9 - 9 + 9;

            // Indicateur de progression
            ItemStack connector = new ItemBuilder(Material.CHAIN)
                .name("§8↓")
                .build();
        }

        // Navigation
        gui.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c← Retour")
            .build());

        gui.setItem(49, new ItemBuilder(Material.TNT)
            .name("§4Reset Talents")
            .lore("", "§7Coût: §c100 Gemmes", "", "§cShift+Clic pour confirmer")
            .build());

        gui.setItem(47, new ItemBuilder(Material.COMPASS)
            .name("§eCompétences")
            .lore("", "§7Voir vos compétences", "", "§e▶ Clic pour ouvrir")
            .build());

        gui.setItem(51, new ItemBuilder(Material.NETHERITE_SWORD)
            .name("§6Armes de Classe")
            .lore("", "§7Voir vos armes", "", "§e▶ Clic pour ouvrir")
            .build());

        gui.setItem(53, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§bBuffs Collectés")
            .lore("", "§7Total: §f" + data.getTotalBuffCount(), "", "§e▶ Clic pour voir")
            .build());

        player.openInventory(gui);
    }

    private void placeTalent(Inventory gui, int slot, ClassTalent talent, ClassData data) {
        int currentLevel = data.getTalentLevel(talent.getId());
        boolean canUnlock = talentTree.canUnlock(data.getUnlockedTalents(), talent.getId());
        boolean isMaxed = currentLevel >= talent.getMaxLevel();

        Material icon;
        String namePrefix;

        if (isMaxed) {
            icon = Material.ENCHANTED_GOLDEN_APPLE;
            namePrefix = "§a✓ ";
        } else if (currentLevel > 0) {
            icon = talent.getIcon();
            namePrefix = "§e";
        } else if (canUnlock) {
            icon = talent.getIcon();
            namePrefix = "§7";
        } else {
            icon = Material.BARRIER;
            namePrefix = "§8";
        }

        List<String> lore = talent.getLore(currentLevel, data.getAvailableTalentPoints());

        gui.setItem(slot, new ItemBuilder(icon)
            .name(namePrefix + talent.getName())
            .lore(lore)
            .glow(isMaxed)
            .build());

        slotToTalent.put(slot, talent.getId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();

        // Clic sur talent
        if (slotToTalent.containsKey(slot)) {
            String talentId = slotToTalent.get(slot);
            if (classManager.unlockTalent(player, talentId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                open(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
            return;
        }

        // Navigation
        switch (slot) {
            case 4 -> { // Header = clic droit pour changer classe
                if (event.isRightClick()) {
                    new ClassSelectionGUI(plugin, classManager).open(player);
                }
            }
            case 45 -> player.closeInventory();
            case 47 -> new SkillsGUI(plugin, classManager).open(player);
            case 49 -> { // Reset
                if (event.isShiftClick()) {
                    if (classManager.resetTalents(player, false)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                        open(player);
                    }
                } else {
                    player.sendMessage("§c⚠ Shift+Clic pour confirmer");
                }
            }
            case 51 -> new ClassWeaponsGUI(plugin, classManager).open(player);
            case 53 -> new BuffsGUI(plugin, classManager).open(player);
        }
    }
}
