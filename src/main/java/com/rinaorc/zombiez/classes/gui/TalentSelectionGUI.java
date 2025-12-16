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
 * 2 pages: Page 1 = Tiers 1-4, Page 2 = Tiers 5-8
 */
public class TalentSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final TalentManager talentManager;

    private static final String GUI_TITLE_PAGE_1 = "§0§l+ TALENTS §0(1/2) +";
    private static final String GUI_TITLE_PAGE_2 = "§0§l+ TALENTS §0(2/2) +";

    // Nouveau layout simplifié (1 talent par tier, centré)
    // Row 0: Slot 4 = Info branche
    // Row 1: Slot 11 = Tier info, Slot 13 = Talent
    // Row 2: Slot 20 = Tier info, Slot 22 = Talent
    // Row 3: Slot 29 = Tier info, Slot 31 = Talent
    // Row 4: Slot 38 = Tier info, Slot 40 = Talent
    private static final int SLOT_BRANCH_INFO = 4;
    private static final int[] TIER_INFO_SLOTS = {11, 20, 29, 38};
    private static final int[] TALENT_SLOTS = {13, 22, 31, 40};

    // Navigation
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_CHANGE_BRANCH = 49;
    private static final int SLOT_NEXT_PAGE = 53;
    private static final int SLOT_BACK = 47;
    private static final int SLOT_CLOSE = 51;

    // Map pour suivre la page actuelle de chaque joueur
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public TalentSelectionGUI(ZombieZPlugin plugin, ClassManager classManager, TalentManager talentManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.talentManager = talentManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
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

        playerPages.put(player.getUniqueId(), page);

        String title = page == 1 ? GUI_TITLE_PAGE_1 : GUI_TITLE_PAGE_2;
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }

        // Info de la branche en haut
        gui.setItem(SLOT_BRANCH_INFO, createBranchInfoItem(branch, data));

        // Tiers à afficher selon la page
        TalentTier[] tiersToShow = page == 1
            ? new TalentTier[]{TalentTier.TIER_1, TalentTier.TIER_2, TalentTier.TIER_3, TalentTier.TIER_4}
            : new TalentTier[]{TalentTier.TIER_5, TalentTier.TIER_6, TalentTier.TIER_7, TalentTier.TIER_8};

        // Afficher chaque tier avec son talent de branche
        for (int tierIndex = 0; tierIndex < 4; tierIndex++) {
            TalentTier tier = tiersToShow[tierIndex];
            boolean unlocked = data.isTalentTierUnlocked(tier);
            String selectedTalentId = data.getSelectedTalentId(tier);

            // Info du tier
            gui.setItem(TIER_INFO_SLOTS[tierIndex], createTierInfoItem(tier, data, selectedTalentId));

            // Le talent de la branche pour ce tier
            Talent branchTalent = getBranchTalentForTier(data, tier, branch);
            if (branchTalent != null) {
                boolean isSelected = branchTalent.getId().equals(selectedTalentId);
                gui.setItem(TALENT_SLOTS[tierIndex], createTalentItem(branchTalent, unlocked, isSelected, data));
            }
        }

        // Navigation - Page précédente
        if (page > 1) {
            gui.setItem(SLOT_PREV_PAGE, new ItemBuilder(Material.ARROW)
                .name("§e< Page précédente")
                .lore("", "§7Tiers 1-4 (Niv. 0-15)")
                .build());
        } else {
            gui.setItem(SLOT_PREV_PAGE, border);
        }

        // Page suivante
        if (page < 2) {
            gui.setItem(SLOT_NEXT_PAGE, new ItemBuilder(Material.ARROW)
                .name("§ePage suivante >")
                .lore("", "§7Tiers 5-8 (Niv. 20-50)")
                .build());
        } else {
            gui.setItem(SLOT_NEXT_PAGE, border);
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
        lore.add("§7Talents actifs: §a" + data.getSelectedTalentCount() + "§7/§a8");

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

    private ItemStack createTierInfoItem(TalentTier tier, ClassData data, String selectedTalentId) {
        boolean unlocked = data.isTalentTierUnlocked(tier);
        int playerLevel = data.getClassLevel().get();

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (unlocked) {
            lore.add("§aDébloqué!");
            lore.add("");
            if (selectedTalentId != null) {
                Talent selected = talentManager.getTalent(selectedTalentId);
                if (selected != null) {
                    lore.add("§7Talent actif:");
                    lore.add(selected.getColoredName());
                }
            } else {
                lore.add("§cAucun talent sélectionné!");
                lore.add("§7Cliquez sur le talent.");
            }
        } else {
            lore.add("§cVerrouillé");
            lore.add("");
            lore.add("§7Niveau requis: §e" + tier.getRequiredLevel());
            lore.add("§7Votre niveau: §f" + playerLevel);
            lore.add("");
            int levelsNeeded = tier.getRequiredLevel() - playerLevel;
            lore.add("§8Encore " + levelsNeeded + " niveau(x)");
        }

        Material icon = unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        String color = unlocked ? tier.getColor() : "§8";

        return new ItemBuilder(icon)
            .name(color + "§l" + tier.getDisplayName())
            .lore(lore)
            .build();
    }

    private ItemStack createTalentItem(Talent talent, boolean tierUnlocked, boolean isSelected, ClassData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        // Description et effets
        for (String line : talent.getFormattedLore()) {
            lore.add(line);
        }

        lore.add("");
        lore.add("§8──────────────");
        lore.add("");

        if (!tierUnlocked) {
            lore.add("§c✗ Palier verrouillé");
            lore.add("§7Niveau " + talent.getTier().getRequiredLevel() + " requis");
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
        if (!title.equals(GUI_TITLE_PAGE_1) && !title.equals(GUI_TITLE_PAGE_2)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int slot = event.getRawSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);
        ClassData data = classManager.getClassData(player);

        // Navigation
        if (slot == SLOT_PREV_PAGE && currentPage > 1) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            open(player, currentPage - 1);
            return;
        }

        if (slot == SLOT_NEXT_PAGE && currentPage < 2) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            open(player, currentPage + 1);
            return;
        }

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
        Talent clickedTalent = getTalentFromSlot(slot, currentPage, player);
        if (clickedTalent != null) {
            handleTalentClick(player, clickedTalent);
        }
    }

    private Talent getTalentFromSlot(int slot, int page, Player player) {
        ClassData data = classManager.getClassData(player);
        if (!data.hasClass() || !data.hasBranch()) return null;

        TalentBranch branch = data.getSelectedBranch();
        if (branch == null) return null;

        // Trouver quel tier correspond au slot
        for (int tierIndex = 0; tierIndex < TALENT_SLOTS.length; tierIndex++) {
            if (TALENT_SLOTS[tierIndex] == slot) {
                // Calculer le tier réel
                int realTierIndex = (page - 1) * 4 + tierIndex;
                if (realTierIndex >= TalentTier.values().length) return null;

                TalentTier tier = TalentTier.values()[realTierIndex];
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
            int page = playerPages.getOrDefault(player.getUniqueId(), 1);
            open(player, page);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cImpossible de sélectionner ce talent!");
        }
    }
}
