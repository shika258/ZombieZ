package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.talents.Talent;
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
 * 2 pages: Page 1 = Tiers 1-4, Page 2 = Tiers 5-8
 */
public class TalentSelectionGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final TalentManager talentManager;

    private static final String GUI_TITLE_PAGE_1 = "§0§l+ TALENTS §0(1/2) +";
    private static final String GUI_TITLE_PAGE_2 = "§0§l+ TALENTS §0(2/2) +";

    // Slots pour chaque tier (4 tiers par page, 5 talents par tier)
    // Layout: Chaque tier prend une ligne (decale de 2 pour centrer)
    // Row 1: Tier 1 - slots 11 (info), 12, 13, 14, 15, 16 (talents)
    // Row 2: Tier 2 - slots 20 (info), 21, 22, 23, 24, 25 (talents)
    // Row 3: Tier 3 - slots 29 (info), 30, 31, 32, 33, 34 (talents)
    // Row 4: Tier 4 - slots 38 (info), 39, 40, 41, 42, 43 (talents)
    private static final int[] TIER_INFO_SLOTS = {11, 20, 29, 38};
    private static final int[][] TALENT_SLOTS = {
        {12, 13, 14, 15, 16}, // Tier 1
        {21, 22, 23, 24, 25}, // Tier 2
        {30, 31, 32, 33, 34}, // Tier 3
        {39, 40, 41, 42, 43}  // Tier 4
    };

    // Navigation
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_INFO = 49;
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

        playerPages.put(player.getUniqueId(), page);

        String title = page == 1 ? GUI_TITLE_PAGE_1 : GUI_TITLE_PAGE_2;
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Bordure
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }

        // Tiers a afficher selon la page
        TalentTier[] tiersToShow = page == 1
            ? new TalentTier[]{TalentTier.TIER_1, TalentTier.TIER_2, TalentTier.TIER_3, TalentTier.TIER_4}
            : new TalentTier[]{TalentTier.TIER_5, TalentTier.TIER_6, TalentTier.TIER_7, TalentTier.TIER_8};

        // Afficher chaque tier
        for (int tierIndex = 0; tierIndex < 4; tierIndex++) {
            TalentTier tier = tiersToShow[tierIndex];
            boolean unlocked = data.isTalentTierUnlocked(tier);
            String selectedTalentId = data.getSelectedTalentId(tier);

            // Info du tier (colonne de gauche)
            gui.setItem(TIER_INFO_SLOTS[tierIndex], createTierInfoItem(tier, data, selectedTalentId));

            // Talents du tier
            List<Talent> talents = talentManager.getTalentsForTier(data.getSelectedClass(), tier);
            int[] slots = TALENT_SLOTS[tierIndex];

            for (int i = 0; i < Math.min(5, talents.size()); i++) {
                Talent talent = talents.get(i);
                boolean isSelected = talent.getId().equals(selectedTalentId);
                gui.setItem(slots[i], createTalentItem(talent, unlocked, isSelected, data));
            }
        }

        // Navigation
        // Page precedente
        if (page > 1) {
            gui.setItem(SLOT_PREV_PAGE, new ItemBuilder(Material.ARROW)
                .name("§e< Page precedente")
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

        // Info centrale
        gui.setItem(SLOT_INFO, new ItemBuilder(Material.BOOK)
            .name("§6§lTALENTS")
            .lore(
                "",
                "§7Selectionnez §e1 talent§7 par palier.",
                "",
                "§7Chaque talent modifie votre",
                "§7facon de jouer de maniere unique.",
                "",
                "§8Clic sur un talent pour le selectionner.",
                "",
                talentManager.getTalentSummary(player)
            )
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

    private ItemStack createTierInfoItem(TalentTier tier, ClassData data, String selectedTalentId) {
        boolean unlocked = data.isTalentTierUnlocked(tier);
        int playerLevel = data.getClassLevel().get();

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (unlocked) {
            lore.add("§aDebloque!");
            lore.add("");
            if (selectedTalentId != null) {
                Talent selected = talentManager.getTalent(selectedTalentId);
                if (selected != null) {
                    lore.add("§7Talent actif:");
                    lore.add(selected.getColoredName());
                }
            } else {
                lore.add("§cAucun talent selectionne!");
                lore.add("§7Cliquez sur un talent.");
            }
        } else {
            lore.add("§cVerrouille");
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
            lore.add("§c✗ Palier verrouille");
            lore.add("§7Niveau " + talent.getTier().getRequiredLevel() + " requis");
        } else if (isSelected) {
            lore.add("§a✓ SELECTIONNE");
        } else {
            // Verifier cooldown
            if (data.isOnTalentChangeCooldown(talent.getTier()) && data.getSelectedTalentId(talent.getTier()) != null) {
                long remaining = data.getTalentChangeCooldownRemaining(talent.getTier());
                long minutes = remaining / (60 * 1000);
                lore.add("§cChangement en cooldown");
                lore.add("§7" + minutes + " min restantes");
            } else {
                lore.add("§e> Clic pour selectionner");
            }
        }

        // Determiner l'icone
        Material icon;
        if (!tierUnlocked) {
            icon = Material.BARRIER;
        } else if (isSelected) {
            icon = talent.getIcon();
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
        if (!data.hasClass()) return null;

        // Trouver quel tier et quel index dans le tier
        for (int tierIndex = 0; tierIndex < 4; tierIndex++) {
            int[] slots = TALENT_SLOTS[tierIndex];
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    // Calculer le tier reel
                    int realTierIndex = (page - 1) * 4 + tierIndex;
                    if (realTierIndex >= TalentTier.values().length) return null;

                    TalentTier tier = TalentTier.values()[realTierIndex];
                    List<Talent> talents = talentManager.getTalentsForTier(data.getSelectedClass(), tier);

                    if (i < talents.size()) {
                        return talents.get(i);
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private void handleTalentClick(Player player, Talent talent) {
        ClassData data = classManager.getClassData(player);

        // Verifier si le tier est debloque
        if (!data.isTalentTierUnlocked(talent.getTier())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cCe palier de talent est verrouille!");
            player.sendMessage("§7Niveau requis: §e" + talent.getTier().getRequiredLevel());
            return;
        }

        // Verifier si c'est deja le talent selectionne
        String currentTalentId = data.getSelectedTalentId(talent.getTier());
        if (talent.getId().equals(currentTalentId)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
            player.sendMessage("§7Ce talent est deja selectionne.");
            return;
        }

        // Verifier cooldown
        if (currentTalentId != null && data.isOnTalentChangeCooldown(talent.getTier())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            long remaining = data.getTalentChangeCooldownRemaining(talent.getTier());
            long minutes = remaining / (60 * 1000);
            player.sendMessage("§cVous ne pouvez pas encore changer ce talent!");
            player.sendMessage("§7Temps restant: §f" + minutes + " minutes");
            return;
        }

        // Selectionner le talent
        if (talentManager.selectTalent(player, talent)) {
            // Rafraichir le GUI
            int page = playerPages.getOrDefault(player.getUniqueId(), 1);
            open(player, page);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§cImpossible de selectionner ce talent!");
        }
    }
}
