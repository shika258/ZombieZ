package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface graphique pour le système de Parcours
 *
 * Affiche:
 * - La progression globale
 * - Les 12 chapitres avec leur statut
 * - L'étape actuelle avec ses détails
 * - Les prochains déblocages
 */
public class JourneyGUI implements Listener {

    private final ZombieZPlugin plugin;
    private static final String MAIN_TITLE = "§8§l《 §6§lParcours du Survivant §8§l》";
    private static final String CHAPTER_TITLE = "§8§l《 §e§lChapitre %d §8§l》";

    public JourneyGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le menu principal du parcours
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(MAIN_TITLE));
        JourneyManager manager = plugin.getJourneyManager();

        // Remplir le fond
        fillBackground(inv);

        // === LIGNE 1: Titre et stats générales ===

        // Item de progression globale (centre)
        inv.setItem(4, createProgressItem(player, manager));

        // === LIGNE 2-3: Les 12 chapitres (2 lignes de 6) ===
        int[] chapterSlots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};
        JourneyChapter[] chapters = JourneyChapter.values();

        for (int i = 0; i < chapters.length && i < chapterSlots.length; i++) {
            JourneyChapter chapter = chapters[i];
            inv.setItem(chapterSlots[i], createChapterItem(player, chapter, manager));
        }

        // === LIGNE 4: Étape actuelle ===
        inv.setItem(31, createCurrentStepItem(player, manager));

        // === LIGNE 5: Prochains déblocages ===
        JourneyChapter currentChapter = manager.getCurrentChapter(player);
        JourneyGate[] unlocks = currentChapter.getUnlocks();
        int[] unlockSlots = {39, 40, 41, 42};

        for (int i = 0; i < unlocks.length && i < unlockSlots.length; i++) {
            inv.setItem(unlockSlots[i], createUnlockItem(unlocks[i], true));
        }

        // === LIGNE 6: Boutons ===
        inv.setItem(49, createInfoItem());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
    }

    /**
     * Ouvre le détail d'un chapitre
     */
    public void openChapterDetail(Player player, JourneyChapter chapter) {
        String title = String.format(CHAPTER_TITLE, chapter.getId());
        Inventory inv = Bukkit.createInventory(null, 45, Component.text(title));
        JourneyManager manager = plugin.getJourneyManager();

        fillBackground(inv);

        // === En-tête du chapitre ===
        inv.setItem(4, createChapterHeaderItem(chapter, manager.isChapterCompleted(player, chapter)));

        // === Les 5 étapes du chapitre ===
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int[] stepSlots = {20, 21, 22, 23, 24};

        for (int i = 0; i < steps.size() && i < stepSlots.length; i++) {
            JourneyStep step = steps.get(i);
            int progress = manager.getStepProgress(player, step);
            boolean completed = step.isCompleted(progress);
            boolean current = step.equals(manager.getCurrentStep(player));

            inv.setItem(stepSlots[i], createStepItem(step, progress, completed, current));
        }

        // === Récompenses du chapitre ===
        inv.setItem(31, createChapterRewardsItem(chapter));

        // === Déblocages du chapitre ===
        JourneyGate[] unlocks = chapter.getUnlocks();
        int[] unlockSlots = {38, 39, 40, 41, 42};
        boolean chapterCompleted = manager.isChapterCompleted(player, chapter);

        for (int i = 0; i < unlocks.length && i < unlockSlots.length; i++) {
            inv.setItem(unlockSlots[i], createUnlockItem(unlocks[i], chapterCompleted));
        }

        // === Bouton retour ===
        inv.setItem(36, createBackButton());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
    }

    // ==================== CRÉATION DES ITEMS ====================

    private ItemStack createProgressItem(Player player, JourneyManager manager) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        double progress = manager.getOverallProgress(player);
        int completedChapters = manager.getCompletedChaptersCount(player);
        JourneyChapter current = manager.getCurrentChapter(player);

        meta.displayName(Component.text("§6§lProgression Globale"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7Progression: " + createProgressBar(progress) + " §e" + String.format("%.1f", progress) + "%"));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Chapitres complétés: §a" + completedChapters + "§7/§a12"));
        lore.add(Component.text("§7Chapitre actuel: " + current.getColoredName()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Phase: " + current.getPhaseName()));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterItem(Player player, JourneyChapter chapter, JourneyManager manager) {
        boolean completed = manager.isChapterCompleted(player, chapter);
        boolean current = chapter.equals(manager.getCurrentChapter(player));
        boolean locked = !completed && !current && chapter.getId() > manager.getCurrentChapter(player).getId();

        Material material;
        if (completed) {
            material = Material.LIME_STAINED_GLASS_PANE;
        } else if (current) {
            material = chapter.getIcon();
        } else if (locked) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        } else {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = completed ? "§a✓ Complété" : (current ? "§e▶ En cours" : "§8🔒 Verrouillé");
        meta.displayName(Component.text(chapter.getFormattedTitle() + " " + status));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + chapter.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Niveaux: §e" + chapter.getMinLevel() + " - " + chapter.getMaxLevel()));
        lore.add(Component.text(""));

        if (completed) {
            lore.add(Component.text("§a§l✓ Chapitre terminé!"));
        } else if (current) {
            JourneyStep currentStep = manager.getCurrentStep(player);
            if (currentStep != null) {
                int progress = manager.getStepProgress(player, currentStep);
                lore.add(Component.text("§eÉtape actuelle:"));
                lore.add(Component.text("§f" + currentStep.getName()));
                lore.add(Component.text("§7" + currentStep.getProgressText(progress)));
            }
        } else {
            lore.add(Component.text("§8Termine le chapitre précédent"));
            lore.add(Component.text("§8pour débloquer celui-ci."));
        }

        lore.add(Component.text(""));
        if (!locked) {
            lore.add(Component.text("§eClique pour voir les détails!"));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentStepItem(Player player, JourneyManager manager) {
        JourneyStep step = manager.getCurrentStep(player);
        if (step == null) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("§cAucune étape active"));
            item.setItemMeta(meta);
            return item;
        }

        int progress = manager.getStepProgress(player, step);
        double percent = step.getProgressPercent(progress);

        ItemStack item = new ItemStack(step.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l▶ Étape Actuelle"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text(step.getFormattedName()));
        lore.add(Component.text("§7" + step.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Progression: " + createProgressBar(percent)));
        lore.add(Component.text("§e" + step.getProgressText(progress)));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Récompenses:"));
        lore.add(Component.text("§e  +" + step.getPointReward() + " Points"));
        lore.add(Component.text("§d  +" + step.getGemReward() + " Gems"));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnlockItem(JourneyGate gate, boolean unlocked) {
        Material material = unlocked ? Material.LIME_DYE : gate.getIcon();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = unlocked ? "§a✓ Débloqué" : "§c🔒 Verrouillé";
        meta.displayName(Component.text(status + " §7- §f" + gate.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        if (!unlocked) {
            lore.add(Component.text("§7Condition: §e" + gate.getRequirement()));
        } else {
            lore.add(Component.text("§aAccès disponible!"));
        }
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterHeaderItem(JourneyChapter chapter, boolean completed) {
        ItemStack item = new ItemStack(chapter.getIcon());
        ItemMeta meta = item.getItemMeta();

        String status = completed ? " §a§l✓" : "";
        meta.displayName(Component.text(chapter.getFormattedTitle() + status));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + chapter.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Phase: " + chapter.getPhaseName()));
        lore.add(Component.text("§7Niveaux: §e" + chapter.getMinLevel() + " - " + chapter.getMaxLevel()));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStepItem(JourneyStep step, int progress, boolean completed, boolean current) {
        Material material;
        if (completed) {
            material = Material.LIME_DYE;
        } else if (current) {
            material = step.getIcon();
        } else {
            material = Material.GRAY_DYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String prefix = completed ? "§a✓ " : (current ? "§e▶ " : "§7");
        meta.displayName(Component.text(prefix + "Étape " + step.getStepNumber() + ": §f" + step.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + step.getDescription()));
        lore.add(Component.text(""));

        if (completed) {
            lore.add(Component.text("§a§lComplété!"));
        } else if (current) {
            double percent = step.getProgressPercent(progress);
            lore.add(Component.text("§7Progression: " + createProgressBar(percent)));
            lore.add(Component.text("§e" + step.getProgressText(progress)));
        } else {
            lore.add(Component.text("§8Complète les étapes précédentes"));
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§7Récompenses:"));
        lore.add(Component.text("§e  +" + step.getPointReward() + " Points"));
        lore.add(Component.text("§d  +" + step.getGemReward() + " Gems"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterRewardsItem(JourneyChapter chapter) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§lRécompenses du Chapitre"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§e+" + formatNumber(chapter.getBonusPoints()) + " Points"));
        lore.add(Component.text("§d+" + chapter.getBonusGems() + " Gems"));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Bonus: " + chapter.getBonusReward()));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§lInformations"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7Le Parcours du Survivant te guide"));
        lore.add(Component.text("§7à travers 12 chapitres d'aventure."));
        lore.add(Component.text(""));
        lore.add(Component.text("§c⚠ IMPORTANT:"));
        lore.add(Component.text("§7Les zones et fonctionnalités sont"));
        lore.add(Component.text("§7§lBLOQUÉES §7tant que tu n'as pas"));
        lore.add(Component.text("§7complété les étapes requises!"));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Commande: §e/journey"));
        lore.add(Component.text(""));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§c◀ Retour"));
        item.setItemMeta(meta);
        return item;
    }

    // ==================== UTILITAIRES ====================

    private void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }

    private String createProgressBar(double percent) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (percent / 10);
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a■");
            } else {
                bar.append("§7□");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    private String formatNumber(int value) {
        if (value >= 1000) {
            return String.format("%.1fK", value / 1000.0);
        }
        return String.valueOf(value);
    }

    // ==================== GESTION DES CLICS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().title().toString();

        // Menu principal
        if (title.contains("Parcours du Survivant")) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            int[] chapterSlots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};

            for (int i = 0; i < chapterSlots.length; i++) {
                if (slot == chapterSlots[i]) {
                    JourneyChapter chapter = JourneyChapter.values()[i];
                    // Ne pas permettre d'ouvrir les chapitres verrouillés
                    JourneyChapter current = plugin.getJourneyManager().getCurrentChapter(player);
                    if (chapter.getId() <= current.getId() ||
                        plugin.getJourneyManager().isChapterCompleted(player, chapter)) {
                        openChapterDetail(player, chapter);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    }
                    return;
                }
            }
        }

        // Menu de détail d'un chapitre
        if (title.contains("Chapitre")) {
            event.setCancelled(true);

            if (event.getRawSlot() == 36) {
                openMainMenu(player);
            }
        }
    }
}
