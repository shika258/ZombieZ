package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.skills.ActiveSkill;
import com.rinaorc.zombiez.classes.skills.ActiveSkill.SkillSlot;
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
 * GUI de gestion des compétences actives
 * Permet d'équiper/retirer les compétences dans les 3 slots
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
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);
        slotToSkill.clear();

        // Fond
        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Header
        gui.setItem(4, new ItemBuilder(data.getSelectedClass().getIcon())
            .name("§6§lCOMPÉTENCES - " + data.getSelectedClass().getDisplayName())
            .lore(
                "",
                "§7Équipez vos compétences actives",
                "§7dans les 3 slots disponibles.",
                "",
                "§9Énergie: §f" + data.getEnergy().get() + "/" + data.getMaxEnergy().get()
            )
            .build());

        // Slots équipés (en haut)
        String[] slots = {"PRIMARY", "SECONDARY", "ULTIMATE"};
        int[] equippedSlots = {19, 22, 25};
        Material[] slotMaterials = {Material.GREEN_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE};

        for (int i = 0; i < slots.length; i++) {
            String slot = slots[i];
            String equippedId = data.getEquippedSkill(slot);
            SkillSlot skillSlot = SkillSlot.valueOf(slot);

            if (equippedId != null) {
                ActiveSkill skill = skillRegistry.getSkill(equippedId);
                if (skill != null) {
                    int cooldown = data.getRemainingCooldown(skill.getId());

                    gui.setItem(equippedSlots[i], new ItemBuilder(skill.getIcon())
                        .name(skillSlot.getColor() + "§l[" + skillSlot.getDisplayName() + "] §f" + skill.getName())
                        .lore(skill.getLore(data.getClassLevel().get(), true, true))
                        .glow(cooldown > 0)
                        .build());
                }
            } else {
                gui.setItem(equippedSlots[i], new ItemBuilder(slotMaterials[i])
                    .name(skillSlot.getColor() + "§l[" + skillSlot.getDisplayName() + "] §8Vide")
                    .lore(
                        "",
                        "§7Aucune compétence équipée",
                        "",
                        "§8Cliquez sur une compétence",
                        "§8ci-dessous pour l'équiper"
                    )
                    .build());
            }
        }

        // Séparateur
        for (int i = 27; i < 36; i++) {
            gui.setItem(i, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ")
                .build());
        }

        // Label
        gui.setItem(31, new ItemBuilder(Material.BOOK)
            .name("§e▼ COMPÉTENCES DISPONIBLES ▼")
            .build());

        // Compétences disponibles (en bas)
        List<ActiveSkill> skills = skillRegistry.getSkillsForClass(data.getSelectedClass());
        Set<String> unlockedTalents = data.getUnlockedTalents().keySet();

        int skillSlot = 37;
        for (ActiveSkill skill : skills) {
            if (skillSlot >= 53) break;

            boolean isUnlocked = !skill.isRequiresUnlock() ||
                unlockedTalents.contains(skill.getUnlockTalentId());
            boolean isEquipped = skill.getId().equals(data.getEquippedSkill(skill.getSlot().name()));

            Material icon = skill.getIcon();
            if (!isUnlocked) {
                icon = Material.BARRIER;
            }

            gui.setItem(skillSlot, new ItemBuilder(icon)
                .name(skill.getSlot().getColor() + skill.getName() +
                    (isEquipped ? " §a✓" : ""))
                .lore(skill.getLore(data.getClassLevel().get(), isEquipped, isUnlocked))
                .glow(isEquipped)
                .build());

            slotToSkill.put(skillSlot, skill.getId());
            skillSlot++;
        }

        // Retour
        gui.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        ClassData data = classManager.getClassData(player);

        // Clic sur compétence équipée = retirer
        if (slot == 19 || slot == 22 || slot == 25) {
            String slotName = slot == 19 ? "PRIMARY" : (slot == 22 ? "SECONDARY" : "ULTIMATE");
            String equipped = data.getEquippedSkill(slotName);

            if (equipped != null) {
                data.unequipSkill(slotName);
                player.sendMessage("§e✗ Compétence retirée du slot " + SkillSlot.valueOf(slotName).getDisplayName());
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
                open(player);
            }
            return;
        }

        // Clic sur compétence disponible = équiper
        if (slotToSkill.containsKey(slot)) {
            String skillId = slotToSkill.get(slot);
            if (classManager.equipSkill(player, skillId)) {
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.2f);
                open(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
            return;
        }

        // Retour
        if (slot == 45) {
            new TalentTreeGUI(plugin, classManager).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
