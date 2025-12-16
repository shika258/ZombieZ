package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.classes.talents.TalentManager;
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

import java.util.*;

/**
 * GUI de selection des talents
 * Affiche uniquement les talents de la branche sélectionnée
 * Tous les 9 talents sur une seule page
 */
public class TalentSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final TalentManager talentManager;

    private static final String GUI_TITLE = "§0§l+ TALENTS +";

    // Layout: 9 talents sur une seule page
    // Row 0: Slot 4 = Info branche
    // Row 2: Slots 18-26 = Tiers 1-9 (centré)
    // Row 5: Navigation
    private static final int SLOT_BRANCH_INFO = 4;
    // 9 talents sur la 3ème ligne (row 2, slots 18-26)
    private static final int[] TALENT_SLOTS = {18, 19, 20, 21, 22, 23, 24, 25, 26};

    // Navigation
    private static final int SLOT_CHANGE_BRANCH = 49;
    private static final int SLOT_BACK = 46;
    private static final int SLOT_CLOSE = 52;

    public TalentSelectionGUI(ZombieZPlugin plugin, ClassManager classManager, TalentManager talentManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.talentManager = talentManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return;
        }

        // Si pas de branche sélectionnée, ouvrir le menu de sélection de branche
        if (!data.hasBranch()) {
            plugin.getBranchSelectionGUI().open(player);
            return;
        }

        TalentBranch branch = data.getSelectedBranch();
        if (branch == null) {
            plugin.getBranchSelectionGUI().open(player);
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }

        // Info de la branche en haut
        gui.setItem(SLOT_BRANCH_INFO, createBranchInfoItem(branch, data));

        // Afficher les 9 tiers avec leurs talents de branche
        TalentTier[] allTiers = TalentTier.values();
        for (int tierIndex = 0; tierIndex < 9 && tierIndex < allTiers.length; tierIndex++) {
            TalentTier tier = allTiers[tierIndex];
            boolean unlocked = data.isTalentTierUnlocked(tier);
            String selectedTalentId = data.getSelectedTalentId(tier);

            // Le talent de la branche pour ce tier
            Talent branchTalent = getBranchTalentForTier(data, tier, branch);
            if (branchTalent != null) {
                boolean isSelected = branchTalent.getId().equals(selectedTalentId);
                gui.setItem(TALENT_SLOTS[tierIndex], createTalentItem(branchTalent, tier, unlocked, isSelected, data));
            }
        }

        // Changer de branche
        List<String> changeLore = new ArrayList<>();
        changeLore.add("");
        changeLore.add("§7Branche actuelle: " + branch.getColoredName());
        changeLore.add("");
        if (data.isOnBranchChangeCooldown()) {
            long minutes = data.getBranchChangeCooldownRemaining() / (60 * 1000);
            changeLore.add("§cChangement en cooldown");
            changeLore.add("§7" + minutes + " min restantes");
        } else {
            changeLore.add("§e> Clic pour changer");
            changeLore.add("§c⚠ Reset tous les talents!");
        }

        gui.setItem(SLOT_CHANGE_BRANCH, new ItemBuilder(Material.COMPARATOR)
            .name("§6§lCHANGER DE BRANCHE")
            .lore(changeLore)
            .build());

        // Retour
        gui.setItem(SLOT_BACK, new ItemBuilder(Material.DARK_OAK_DOOR)
            .name("§7< Retour")
            .lore("", "§8Retour aux infos de classe")
            .build());

        // Fermer
        gui.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c+ Fermer")
            .build());

        player.openInventory(gui);
    }

    private ItemStack createBranchInfoItem(TalentBranch branch, ClassData data) {
        List<String> lore = new ArrayList<>();

        // Ajouter la description de la branche
        for (String line : branch.getDescription()) {
            lore.add(line);
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");
        lore.add("§7Talents actifs: §a" + data.getSelectedTalentCount() + "§7/§a9");

        return new ItemBuilder(branch.getIcon())
            .name(branch.getColor() + "§l" + branch.getDisplayName().toUpperCase())
            .lore(lore)
            .glow(true)
            .build();
    }

    private Talent getBranchTalentForTier(ClassData data, TalentTier tier, TalentBranch branch) {
        List<Talent> talents = talentManager.getTalentsForTier(data.getSelectedClass(), tier);
        int branchSlotIndex = branch.getSlotIndex();

        for (Talent talent : talents) {
            if (talent.getSlotIndex() == branchSlotIndex) {
                return talent;
            }
        }
        return null;
    }

    private ItemStack createTalentItem(Talent talent, TalentTier tier, boolean tierUnlocked, boolean isSelected, ClassData data) {
        List<String> lore = new ArrayList<>();

        // Info du tier
        lore.add(tier.getColor() + tier.getDisplayName() + " §8(Niv. " + tier.getRequiredLevel() + ")");
        lore.add("");

        // Description et effets
        for (String line : talent.getFormattedLore()) {
            lore.add(line);
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");

        if (!tierUnlocked) {
            int playerLevel = data.getClassLevel().get();
            int levelsNeeded = tier.getRequiredLevel() - playerLevel;
            lore.add("§c✗ Palier verrouillé");
            lore.add("§7Niveau " + tier.getRequiredLevel() + " requis");
            lore.add("§8Encore " + levelsNeeded + " niveau(x)");
        } else if (isSelected) {
            lore.add("§a✓ SÉLECTIONNÉ");
        } else {
            lore.add("§e> Clic pour sélectionner");
        }

        // Déterminer l'icône
        Material icon;
        if (!tierUnlocked) {
            icon = Material.BARRIER;
        } else {
            icon = talent.getIcon();
        }

        ItemBuilder builder = new ItemBuilder(icon)
            .name((isSelected ? "§a" : talent.getIconColor()) + talent.getName())
            .lore(lore);

        if (isSelected) {
            builder.glow(true);
        }

        return builder.build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();

        // Navigation
        if (slot == SLOT_CHANGE_BRANCH) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            plugin.getBranchSelectionGUI().open(player);
            return;
        }

        if (slot == SLOT_BACK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            plugin.getClassInfoGUI().open(player);
            return;
        }

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        // Clic sur un talent
        Talent clickedTalent = getTalentFromSlot(slot, player);
        if (clickedTalent != null) {
            handleTalentClick(player, clickedTalent);
        }
    }

    private Talent getTalentFromSlot(int slot, Player player) {
        ClassData data = classManager.getClassData(player);
        if (!data.hasClass() || !data.hasBranch()) return null;

        TalentBranch branch = data.getSelectedBranch();
        if (branch == null) return null;

        // Trouver quel tier correspond au slot
        for (int tierIndex = 0; tierIndex < TALENT_SLOTS.length; tierIndex++) {
            if (TALENT_SLOTS[tierIndex] == slot) {
                if (tierIndex >= TalentTier.values().length) return null;

                TalentTier tier = TalentTier.values()[tierIndex];
                return getBranchTalentForTier(data, tier, branch);
            }
        }
        return null;
    }

    private void handleTalentClick(Player player, Talent talent) {
        ClassData data = classManager.getClassData(player);

        // Vérifier si le tier est débloqué
        if (!data.isTalentTierUnlocked(talent.getTier())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cCe palier de talent est verrouillé!");
            player.sendMessage("§7Niveau requis: §e" + talent.getTier().getRequiredLevel());
            return;
        }

        // Vérifier si c'est déjà le talent sélectionné
        String currentTalentId = data.getSelectedTalentId(talent.getTier());
        if (talent.getId().equals(currentTalentId)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
            player.sendMessage("§7Ce talent est déjà sélectionné.");
            return;
        }

        // Sélectionner le talent
        if (talentManager.selectTalent(player, talent)) {
            // Rafraîchir le GUI
            open(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cImpossible de sélectionner ce talent!");
        }
    }
}
