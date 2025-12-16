package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.skills.ActiveSkill;
import com.rinaorc.zombiez.classes.skills.SkillRegistry;
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
 * GUI des compétences simplifié - 4 compétences par classe
 * 2 de base, 1 avancée (nv5), 1 ultime (nv10)
 * Le joueur peut équiper jusqu'à 3 compétences dans les slots 1-2-3
 */
public class SkillsGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final SkillRegistry skillRegistry;

    private static final String GUI_TITLE = "§0§l✦ COMPÉTENCES ✦";
    private final Map<Integer, String> slotToSkill = new HashMap<>();

    public SkillsGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.skillRegistry = classManager.getSkillRegistry();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) {
            player.sendMessage("§cChoisissez d'abord une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);
        slotToSkill.clear();

        // Fond
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, bg);
        }

        // Header
        gui.setItem(4, new ItemBuilder(data.getSelectedClass().getIcon())
            .name(data.getSelectedClass().getColoredName() + " §7- Compétences")
            .lore(
                "",
                "§9Énergie: §f" + data.getEnergy().get() + "/" + data.getMaxEnergy().get(),
                "§7Niveau de classe: §f" + data.getClassLevel().get(),
                "",
                "§8Cliquez sur une compétence",
                "§8pour l'équiper dans un slot"
            )
            .build());

        // Slots équipés (haut)
        int[] equippedSlots = {11, 13, 15};
        Material[] slotColors = {Material.GREEN_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE};
        String[] slotNames = {"§a[1]", "§e[2]", "§c[3]"};

        for (int i = 0; i < 3; i++) {
            String slotKey = "SLOT_" + (i + 1);
            String equippedId = data.getEquippedSkill(slotKey);

            if (equippedId != null) {
                ActiveSkill skill = skillRegistry.getSkill(equippedId);
                if (skill != null) {
                    gui.setItem(equippedSlots[i], new ItemBuilder(skill.getIcon())
                        .name(slotNames[i] + " §f" + skill.getName())
                        .lore(skill.getLore(data.getClassLevel().get(), true))
                        .glow(true)
                        .build());
                    continue;
                }
            }

            gui.setItem(equippedSlots[i], new ItemBuilder(slotColors[i])
                .name(slotNames[i] + " §8Vide")
                .lore("", "§7Aucune compétence", "§7équipée dans ce slot")
                .build());
        }

        // Séparateur
        gui.setItem(20, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        gui.setItem(22, new ItemBuilder(Material.BOOK)
            .name("§e▼ COMPÉTENCES DISPONIBLES ▼")
            .build());
        gui.setItem(24, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());

        // Compétences disponibles (4 compétences en ligne)
        List<ActiveSkill> skills = skillRegistry.getSkillsForClass(data.getSelectedClass());

        int[] skillSlots = {29, 30, 32, 33}; // 4 slots centrés
        int idx = 0;

        for (ActiveSkill skill : skills) {
            if (idx >= skillSlots.length) break;

            boolean isUnlocked = skill.isUnlocked(data.getClassLevel().get());
            boolean isEquipped = isSkillEquipped(data, skill.getId());

            Material icon = isUnlocked ? skill.getIcon() : Material.BARRIER;
            String namePrefix = isEquipped ? "§a✓ " : (isUnlocked ? "§f" : "§8");

            List<String> lore = skill.getLore(data.getClassLevel().get(), isEquipped);

            gui.setItem(skillSlots[idx], new ItemBuilder(icon)
                .name(namePrefix + skill.getName())
                .lore(lore)
                .glow(isEquipped)
                .build());

            if (isUnlocked) {
                slotToSkill.put(skillSlots[idx], skill.getId());
            }

            idx++;
        }

        // Navigation
        gui.setItem(36, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        gui.setItem(40, new ItemBuilder(Material.KNOWLEDGE_BOOK)
            .name("§eAide")
            .lore(
                "",
                "§7Touche 1 = Compétence slot 1",
                "§7Touche 2 = Compétence slot 2",
                "§7Touche 3 = Compétence slot 3",
                "",
                "§7Ou /class use <1-3>"
            )
            .build());

        player.openInventory(gui);
    }

    private boolean isSkillEquipped(ClassData data, String skillId) {
        for (int i = 1; i <= 3; i++) {
            if (skillId.equals(data.getEquippedSkill("SLOT_" + i))) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        ClassData data = classManager.getClassData(player);

        // Clic sur slot équipé = retirer
        if (slot == 11 || slot == 13 || slot == 15) {
            int slotNum = slot == 11 ? 1 : (slot == 13 ? 2 : 3);
            String slotKey = "SLOT_" + slotNum;

            if (data.getEquippedSkill(slotKey) != null) {
                data.unequipSkill(slotKey);
                player.sendMessage("§e✗ Compétence retirée du slot " + slotNum);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
                open(player);
            }
            return;
        }

        // Clic sur compétence disponible = équiper
        if (slotToSkill.containsKey(slot)) {
            String skillId = slotToSkill.get(slot);

            // Trouver le premier slot libre ou remplacer si déjà équipé
            int targetSlot = -1;
            for (int i = 1; i <= 3; i++) {
                String equipped = data.getEquippedSkill("SLOT_" + i);
                if (equipped == null) {
                    targetSlot = i;
                    break;
                }
                if (equipped.equals(skillId)) {
                    // Déjà équipé, retirer
                    data.unequipSkill("SLOT_" + i);
                    player.sendMessage("§e✗ Compétence retirée");
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
                    open(player);
                    return;
                }
            }

            if (targetSlot == -1) {
                // Tous les slots sont pleins, remplacer le slot 1
                targetSlot = 1;
            }

            if (classManager.equipSkill(player, skillId, targetSlot)) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.2f);
                open(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
            return;
        }

        // Retour
        if (slot == 36) {
            new TalentTreeGUI(plugin, classManager).open(player);
        }
    }
}
