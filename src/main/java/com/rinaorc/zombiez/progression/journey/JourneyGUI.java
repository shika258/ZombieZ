package com.rinaorc.zombiez.progression.journey;

import com.rinaorc.zombiez.ZombieZPlugin;
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
 *
 * Supporte jusqu'à 10 étapes par chapitre
 */
public class JourneyGUI implements Listener {

    private final ZombieZPlugin plugin;
    private static final String MAIN_TITLE = "§8§l« §6§lJournal du Survivant §8§l»";
    private static final String CHAPTER_TITLE = "§8§l« §e§lChapitre %d §7- §f%s §8§l»";

    // Identifiants pour reconnaître les inventaires
    private static final String MAIN_MENU_ID = "journey_main";
    private static final String CHAPTER_MENU_ID = "journey_chapter";

    // Cache des inventaires ouverts par joueur
    private final java.util.Map<java.util.UUID, String> openMenus = new java.util.concurrent.ConcurrentHashMap<>();
    // Cache du chapitre actuellement visualisé
    private final java.util.Map<java.util.UUID, Integer> viewingChapter = new java.util.concurrent.ConcurrentHashMap<>();

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

        // === BORDURE DÉCORATIVE ===
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        // === LIGNE 1: Titre et stats générales ===
        inv.setItem(4, createProgressItem(player, manager));

        // Décorations titre
        inv.setItem(3, createDecorItem(Material.YELLOW_STAINED_GLASS_PANE, "§6§l★"));
        inv.setItem(5, createDecorItem(Material.YELLOW_STAINED_GLASS_PANE, "§6§l★"));

        // === LIGNE 2-3: Les 12 chapitres (2 lignes de 6, centrées) ===
        // Ligne 2: Chapitres 1-6 (slots 10-15)
        // Ligne 3: Chapitres 7-12 (slots 19-24)
        int[] chapterSlots = {10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24};
        JourneyChapter[] chapters = JourneyChapter.values();

        for (int i = 0; i < chapters.length && i < chapterSlots.length; i++) {
            JourneyChapter chapter = chapters[i];
            inv.setItem(chapterSlots[i], createChapterItem(player, chapter, manager));
        }

        // === LIGNE 4: Séparateur + Étape actuelle (centrée) ===
        inv.setItem(28, createDecorItem(Material.ORANGE_STAINED_GLASS_PANE, "§6"));
        inv.setItem(29, createDecorItem(Material.ORANGE_STAINED_GLASS_PANE, "§6"));
        inv.setItem(30, createDecorItem(Material.YELLOW_STAINED_GLASS_PANE, "§e▶"));
        inv.setItem(31, createCurrentStepItem(player, manager));
        inv.setItem(32, createDecorItem(Material.YELLOW_STAINED_GLASS_PANE, "§e◀"));
        inv.setItem(33, createDecorItem(Material.ORANGE_STAINED_GLASS_PANE, "§6"));
        inv.setItem(34, createDecorItem(Material.ORANGE_STAINED_GLASS_PANE, "§6"));

        // === LIGNE 5: Prochains déblocages ===
        JourneyChapter currentChapter = manager.getCurrentChapter(player);
        JourneyGate[] unlocks = currentChapter.getUnlocks();

        // Titre des déblocages
        inv.setItem(37, createDecorItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§lDéblocages"));

        // Afficher jusqu'à 4 déblocages
        int[] unlockSlots = {39, 40, 41, 42};
        for (int i = 0; i < unlocks.length && i < unlockSlots.length; i++) {
            boolean unlocked = manager.hasUnlockedGate(player, unlocks[i]);
            inv.setItem(unlockSlots[i], createUnlockItem(unlocks[i], unlocked));
        }

        inv.setItem(43, createDecorItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l◆"));

        // === LIGNE 6: Boutons ===
        inv.setItem(49, createInfoItem());

        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), MAIN_MENU_ID);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
    }

    /**
     * Ouvre le détail d'un chapitre (supporte jusqu'à 10 étapes)
     */
    public void openChapterDetail(Player player, JourneyChapter chapter) {
        String title = String.format(CHAPTER_TITLE, chapter.getId(), chapter.getName());
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));
        JourneyManager manager = plugin.getJourneyManager();

        // Mémoriser le chapitre visualisé
        viewingChapter.put(player.getUniqueId(), chapter.getId());

        // === BORDURE DÉCORATIVE ===
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        // === LIGNE 1: En-tête du chapitre ===
        inv.setItem(4, createChapterHeaderItem(chapter, manager.isChapterCompleted(player, chapter), player, manager));

        // Phase du chapitre (couleur selon la phase)
        Material phaseGlass = getPhaseGlass(chapter);
        inv.setItem(3, createDecorItem(phaseGlass, chapter.getColor() + "§l◆"));
        inv.setItem(5, createDecorItem(phaseGlass, chapter.getColor() + "§l◆"));

        // === LIGNES 2-3: Les étapes du chapitre (jusqu'à 10) ===
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int totalSteps = steps.size();

        // Layout adaptatif selon le nombre d'étapes
        int[] stepSlots = getStepSlots(totalSteps);

        for (int i = 0; i < steps.size() && i < stepSlots.length; i++) {
            JourneyStep step = steps.get(i);
            int progress = manager.getStepProgress(player, step);
            boolean completed = manager.isStepCompleted(player, step);
            boolean current = step.equals(manager.getCurrentStep(player));
            boolean locked = !completed && !current;

            inv.setItem(stepSlots[i], createStepItem(step, progress, completed, current, locked));

            // Ajouter des flèches de connexion entre les étapes (sauf la dernière de chaque ligne)
            if (i < totalSteps - 1 && shouldAddArrow(i, totalSteps)) {
                int arrowSlot = getArrowSlot(stepSlots[i], stepSlots[i + 1], totalSteps);
                if (arrowSlot >= 0 && arrowSlot < 54) {
                    inv.setItem(arrowSlot, createArrowItem(completed));
                }
            }
        }

        // === LIGNE 4: Séparateur avec couleur de phase ===
        for (int i = 28; i <= 34; i++) {
            inv.setItem(i, createDecorItem(phaseGlass, chapter.getColor()));
        }

        // === LIGNE 5: Récompenses + Progression (centrées) ===
        inv.setItem(40, createChapterRewardsItem(chapter));
        inv.setItem(42, createChapterProgressItem(player, chapter, manager));

        // === LIGNE 6: Navigation ===
        // Navigation entre chapitres
        if (chapter.getId() > 1) {
            inv.setItem(48, createNavButton(false, chapter.getId() - 1));
        }
        if (chapter.getId() < 12) {
            inv.setItem(50, createNavButton(true, chapter.getId() + 1));
        }

        player.openInventory(inv);
        openMenus.put(player.getUniqueId(), CHAPTER_MENU_ID);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
    }

    /**
     * Calcule les slots pour les étapes selon leur nombre
     * Ligne 2 (slots 11-15): étapes 1-5
     * Ligne 3 (slots 20-24): étapes 6-10 (directement en dessous)
     */
    private int[] getStepSlots(int totalSteps) {
        return switch (totalSteps) {
            case 1 -> new int[]{22};
            case 2 -> new int[]{21, 23};
            case 3 -> new int[]{20, 22, 24};
            case 4 -> new int[]{20, 21, 23, 24};
            case 5 -> new int[]{20, 21, 22, 23, 24};
            case 6 -> new int[]{11, 12, 13, 14, 15, 22};
            case 7 -> new int[]{11, 12, 13, 14, 15, 21, 23};
            case 8 -> new int[]{11, 12, 13, 14, 15, 20, 22, 24};
            case 9 -> new int[]{11, 12, 13, 14, 15, 20, 21, 23, 24};
            case 10 -> new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
            default -> new int[]{11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
        };
    }

    /**
     * Vérifie si on doit ajouter une flèche après cette étape
     */
    private boolean shouldAddArrow(int index, int totalSteps) {
        if (totalSteps <= 5) {
            return true; // Toujours des flèches sur une seule ligne
        }
        // Pour 2 lignes, pas de flèche entre la fin de la ligne 1 et le début de la ligne 2
        return index != 4; // Pas de flèche après l'étape 5 (index 4)
    }

    /**
     * Calcule le slot de la flèche entre deux étapes
     */
    private int getArrowSlot(int currentSlot, int nextSlot, int totalSteps) {
        // Les flèches ne sont pas gérées visuellement pour simplifier
        // On les ignore pour le moment
        return -1;
    }

    private ItemStack createArrowItem(boolean passed) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(passed ? "§a→" : "§7→"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== CRÉATION DES ITEMS ====================

    private ItemStack createProgressItem(Player player, JourneyManager manager) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        double progress = manager.getOverallProgress(player);
        int completedChapters = manager.getCompletedChaptersCount(player);
        JourneyChapter current = manager.getCurrentChapter(player);
        JourneyStep currentStep = manager.getCurrentStep(player);

        meta.displayName(Component.text("§6§l✦ Progression Globale ✦"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(Component.text(""));
        lore.add(Component.text("  " + createProgressBar(progress, 15) + " §e" + String.format("%.1f", progress) + "%"));
        lore.add(Component.text(""));
        lore.add(Component.text("  §7Chapitres: §a" + completedChapters + "§7/§a12 §8complétés"));
        lore.add(Component.text("  §7Chapitre actuel: " + current.getColoredName()));
        lore.add(Component.text("  §7Phase: " + current.getPhaseName()));
        lore.add(Component.text(""));

        if (currentStep != null) {
            int stepProgress = manager.getStepProgress(player, currentStep);
            lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            lore.add(Component.text(""));
            lore.add(Component.text("  §e§lObjectif Actuel:"));
            lore.add(Component.text("  §f" + currentStep.getName()));
            lore.add(Component.text("  §7" + currentStep.getProgressText(stepProgress)));
            lore.add(Component.text(""));
        }

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
        String statusIcon;
        if (completed) {
            material = Material.LIME_STAINED_GLASS_PANE;
            statusIcon = "§a✓";
        } else if (current) {
            material = chapter.getIcon();
            statusIcon = "§e▶";
        } else if (locked) {
            material = Material.GRAY_STAINED_GLASS_PANE;
            statusIcon = "§8🔒";
        } else {
            material = Material.YELLOW_STAINED_GLASS_PANE;
            statusIcon = "§7○";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(statusIcon + " " + chapter.getFormattedTitle()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + chapter.getDescription()));
        lore.add(Component.text(""));

        if (completed) {
            lore.add(Component.text("§a§l✓ Chapitre terminé!"));
        } else if (current) {
            JourneyStep currentStep = manager.getCurrentStep(player);
            if (currentStep != null && currentStep.getChapter().equals(chapter)) {
                int progress = manager.getStepProgress(player, currentStep);
                double percent = currentStep.getProgressPercent(progress);
                lore.add(Component.text("§7Étape §e" + currentStep.getStepNumber() + "§7:"));
                lore.add(Component.text("§f" + currentStep.getName()));
                lore.add(Component.text("  " + createProgressBar(percent, 10)));
            }
        } else {
            lore.add(Component.text("§8Termine les chapitres"));
            lore.add(Component.text("§8précédents pour débloquer."));
        }

        lore.add(Component.text(""));
        if (!locked) {
            lore.add(Component.text("§eClique pour voir les détails"));
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
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("§7Tu as complété tous les chapitres!"));
            lore.add(Component.text(""));
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        int progress = manager.getStepProgress(player, step);
        double percent = step.getProgressPercent(progress);

        ItemStack item = new ItemStack(step.getIcon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l▶ " + step.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + step.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(Component.text(""));
        lore.add(Component.text("  " + createProgressBar(percent, 12)));
        lore.add(Component.text("  §e" + step.getProgressText(progress)));
        lore.add(Component.text(""));
        lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Récompenses:"));
        lore.add(Component.text("§e  ⬧ " + formatNumber(step.getPointReward()) + " Points"));
        lore.add(Component.text("§d  ⬧ " + step.getGemReward() + " Gems"));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnlockItem(JourneyGate gate, boolean unlocked) {
        Material material = unlocked ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String icon = unlocked ? "§a✓" : "§c🔒";
        meta.displayName(Component.text(icon + " §f" + gate.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        if (!unlocked) {
            lore.add(Component.text("§7Condition:"));
            lore.add(Component.text("§e" + gate.getRequirement()));
        } else {
            lore.add(Component.text("§a✓ Débloqué!"));
        }
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterHeaderItem(JourneyChapter chapter, boolean completed, Player player, JourneyManager manager) {
        ItemStack item = new ItemStack(chapter.getIcon());
        ItemMeta meta = item.getItemMeta();

        String status = completed ? " §a§l✓" : "";
        meta.displayName(Component.text(chapter.getFormattedTitle() + status));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7" + chapter.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(Component.text(""));
        lore.add(Component.text("  §7Phase: " + chapter.getPhaseName()));
        lore.add(Component.text("  §7Niveaux: §e" + chapter.getMinLevel() + " - " + chapter.getMaxLevel()));
        lore.add(Component.text(""));

        // Afficher le nombre d'étapes complétées
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int completedSteps = 0;
        for (JourneyStep step : steps) {
            if (manager.isStepCompleted(player, step)) {
                completedSteps++;
            }
        }
        lore.add(Component.text("  §7Étapes: §a" + completedSteps + "§7/§a" + steps.size()));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStepItem(JourneyStep step, int progress, boolean completed, boolean current, boolean locked) {
        Material material;
        String prefix;

        if (completed) {
            material = Material.LIME_DYE;
            prefix = "§a✓ ";
        } else if (current) {
            material = step.getIcon();
            prefix = "§e▶ ";
        } else {
            material = Material.GRAY_DYE;
            prefix = "§8○ ";
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(prefix + "§fÉtape " + step.getStepNumber()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text(step.getChapter().getColor() + step.getName()));
        lore.add(Component.text("§7" + step.getDescription()));
        lore.add(Component.text(""));

        if (completed) {
            lore.add(Component.text("§a§l✓ Complété!"));
        } else if (current) {
            double percent = step.getProgressPercent(progress);
            lore.add(Component.text(createProgressBar(percent, 10)));
            lore.add(Component.text("§e" + step.getProgressText(progress)));
        } else {
            lore.add(Component.text("§8Complète les étapes"));
            lore.add(Component.text("§8précédentes d'abord."));
        }

        lore.add(Component.text(""));
        lore.add(Component.text("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        lore.add(Component.text("§7Récompenses:"));
        lore.add(Component.text("§e  +" + formatNumber(step.getPointReward()) + " Points"));
        lore.add(Component.text("§d  +" + step.getGemReward() + " Gems"));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterRewardsItem(JourneyChapter chapter) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6§l✦ Récompenses du Chapitre ✦"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7En terminant ce chapitre:"));
        lore.add(Component.text(""));
        lore.add(Component.text("§e  ⬧ " + formatNumber(chapter.getBonusPoints()) + " Points bonus"));
        lore.add(Component.text("§d  ⬧ " + chapter.getBonusGems() + " Gems bonus"));
        lore.add(Component.text(""));

        String bonus = chapter.getBonusReward();
        if (bonus != null && !bonus.isEmpty()) {
            lore.add(Component.text("§a  ⬧ " + bonus));
            lore.add(Component.text(""));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChapterProgressItem(Player player, JourneyChapter chapter, JourneyManager manager) {
        List<JourneyStep> steps = JourneyStep.getStepsForChapter(chapter);
        int completedSteps = 0;
        for (JourneyStep step : steps) {
            if (manager.isStepCompleted(player, step)) {
                completedSteps++;
            }
        }

        double percent = steps.isEmpty() ? 0 : (completedSteps * 100.0 / steps.size());
        boolean chapterCompleted = manager.isChapterCompleted(player, chapter);

        Material material = chapterCompleted ? Material.EMERALD : Material.CLOCK;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(chapterCompleted ? "§a§lChapitre Terminé!" : "§e§lProgression"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("  " + createProgressBar(percent, 12)));
        lore.add(Component.text(""));
        lore.add(Component.text("  §7Étapes: §a" + completedSteps + "§7/§a" + steps.size()));
        lore.add(Component.text(""));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e§l? Informations"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7Le Journal du Survivant te guide"));
        lore.add(Component.text("§7à travers §e12 chapitres §7d'aventure."));
        lore.add(Component.text(""));
        lore.add(Component.text("§c§l⚠ IMPORTANT:"));
        lore.add(Component.text("§7Les zones et fonctionnalités sont"));
        lore.add(Component.text("§c§lBLOQUÉES §7tant que tu n'as pas"));
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
        meta.displayName(Component.text("§c§l◀ Retour au menu"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7Clique pour revenir"));
        lore.add(Component.text("§7au menu principal."));
        lore.add(Component.text(""));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavButton(boolean next, int targetChapter) {
        ItemStack item = new ItemStack(next ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();

        String arrow = next ? "▶" : "◀";
        String label = next ? "Chapitre Suivant" : "Chapitre Précédent";
        meta.displayName(Component.text((next ? "§a" : "§c") + "§l" + arrow + " " + label));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7Aller au chapitre §e" + targetChapter));
        lore.add(Component.text(""));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDecorItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    // ==================== UTILITAIRES ====================

    private void fillBorder(Inventory inv, Material glass) {
        ItemStack borderItem = new ItemStack(glass);
        ItemMeta meta = borderItem.getItemMeta();
        meta.displayName(Component.text(" "));
        borderItem.setItemMeta(meta);

        // Remplir tout d'abord
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, borderItem.clone());
        }
    }

    private Material getPhaseGlass(JourneyChapter chapter) {
        return switch (chapter.getPhase()) {
            case 1 -> Material.LIME_STAINED_GLASS_PANE;
            case 2 -> Material.YELLOW_STAINED_GLASS_PANE;
            case 3 -> Material.ORANGE_STAINED_GLASS_PANE;
            case 4 -> Material.MAGENTA_STAINED_GLASS_PANE;
            default -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    private String createProgressBar(double percent, int length) {
        StringBuilder bar = new StringBuilder("§8[");
        int filled = (int) (percent / (100.0 / length));
        for (int i = 0; i < length; i++) {
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

        String menuType = openMenus.get(player.getUniqueId());
        if (menuType == null) return; // Pas notre inventaire

        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Menu principal
        if (MAIN_MENU_ID.equals(menuType)) {
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
        if (CHAPTER_MENU_ID.equals(menuType)) {
            // Bouton retour
            if (slot == 45) {
                openMainMenu(player);
                return;
            }

            // Navigation chapitre précédent
            if (slot == 48) {
                Integer currentChapterId = viewingChapter.get(player.getUniqueId());
                if (currentChapterId != null && currentChapterId > 1) {
                    JourneyChapter prevChapter = JourneyChapter.getById(currentChapterId - 1);
                    if (prevChapter != null) {
                        openChapterDetail(player, prevChapter);
                    }
                }
                return;
            }

            // Navigation chapitre suivant
            if (slot == 50) {
                Integer currentChapterId = viewingChapter.get(player.getUniqueId());
                if (currentChapterId != null && currentChapterId < 12) {
                    JourneyChapter nextChapter = JourneyChapter.getById(currentChapterId + 1);
                    if (nextChapter != null) {
                        // Vérifier que le joueur peut voir ce chapitre
                        JourneyChapter playerChapter = plugin.getJourneyManager().getCurrentChapter(player);
                        if (nextChapter.getId() <= playerChapter.getId() ||
                            plugin.getJourneyManager().isChapterCompleted(player, nextChapter)) {
                            openChapterDetail(player, nextChapter);
                        } else {
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                        }
                    }
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openMenus.remove(player.getUniqueId());
            viewingChapter.remove(player.getUniqueId());
        }
    }
}
