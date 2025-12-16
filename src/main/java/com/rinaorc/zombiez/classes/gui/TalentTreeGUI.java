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
 * GUI de l'arbre de talents
 * Affiche les 3 branches de talents avec possibilité de débloquer
 */
public class TalentTreeGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final ClassTalentTree talentTree;

    private static final String GUI_TITLE_PREFIX = "§0§l✦ TALENTS: ";
    private final Map<UUID, TalentBranch> playerBranch = new HashMap<>();
    private final Map<Integer, String> slotToTalent = new HashMap<>();

    // Layout: 6 rows x 9 cols
    // Colonne 1 = Tier indicators
    // Colonnes 2-3 = Branche 1
    // Colonnes 4-5 = Branche 2
    // Colonnes 6-7 = Branche 3
    // Colonne 8 = Navigation

    public TalentTreeGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.talentTree = classManager.getTalentTree();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le GUI des talents pour un joueur
     */
    public void open(Player player) {
        open(player, TalentBranch.OFFENSE);
    }

    /**
     * Ouvre le GUI des talents avec une branche spécifique
     */
    public void open(Player player, TalentBranch branch) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            new ClassSelectionGUI(plugin, classManager).open(player);
            return;
        }

        ClassType classType = data.getSelectedClass();
        String title = GUI_TITLE_PREFIX + classType.getDisplayName().toUpperCase() + " ✦";
        Inventory gui = Bukkit.createInventory(null, 54, title);

        playerBranch.put(player.getUniqueId(), branch);
        slotToTalent.clear();

        // Fond
        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Header - Info classe
        gui.setItem(4, new ItemBuilder(classType.getIcon())
            .name(classType.getColoredName() + " §7- Niveau " + data.getClassLevel().get())
            .lore(
                "",
                "§7Points de talent: §e" + data.getAvailableTalentPoints(),
                "§7XP de classe: §f" + data.getClassXp().get() + "/" + data.getRequiredXpForNextClassLevel(),
                "",
                "§8Clic droit pour changer de classe"
            )
            .build());

        // Onglets de branches (en haut)
        int[] branchSlots = {1, 4, 7}; // Sera ajusté pour le layout réel
        for (int i = 0; i < TalentBranch.values().length; i++) {
            TalentBranch b = TalentBranch.values()[i];
            boolean isSelected = b == branch;

            gui.setItem(i == 0 ? 1 : (i == 1 ? 4 : 7), new ItemBuilder(b.getIcon())
                .name(b.getColoredName() + (isSelected ? " §e◄" : ""))
                .lore(
                    "",
                    "§7" + b.getDescription(),
                    "",
                    isSelected ? "§a✓ Sélectionné" : "§e▶ Clic pour voir"
                )
                .glow(isSelected)
                .build());
        }

        // Afficher les talents de la branche sélectionnée
        List<ClassTalent> branchTalents = talentTree.getTalentsForBranch(classType, branch);

        // Layout: 5 tiers, 2 talents par tier possible
        // Lignes 1-5 pour les tiers
        for (ClassTalent talent : branchTalents) {
            int tier = talent.getTier();
            int row = tier; // Tier 1 = row 1, etc.

            // Trouver le slot disponible dans cette ligne
            int baseSlot = row * 9 + 2; // Commence colonne 2
            List<ClassTalent> sameTierTalents = branchTalents.stream()
                .filter(t -> t.getTier() == tier)
                .toList();

            int indexInTier = sameTierTalents.indexOf(talent);
            int slot = baseSlot + (indexInTier * 3); // Espacer les talents

            if (slot < 54 && slot >= 0) {
                int currentLevel = data.getTalentLevel(talent.getId());
                boolean canUnlock = talentTree.canUnlock(data.getUnlockedTalents(), talent.getId());
                boolean isMaxed = currentLevel >= talent.getMaxLevel();

                Material icon = talent.getIcon();
                if (isMaxed) {
                    icon = Material.ENCHANTED_GOLDEN_APPLE;
                } else if (currentLevel > 0) {
                    icon = Material.GOLDEN_APPLE;
                } else if (!canUnlock) {
                    icon = Material.BARRIER;
                }

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(talent.getDescriptionAtLevel(Math.max(1, currentLevel)));
                lore.add("");
                lore.add("§7Tier " + tier + " | Coût: §e" + talent.getPointCost() + " point(s)");
                lore.add("");

                if (currentLevel > 0) {
                    lore.add("§aNiveau actuel: §f" + currentLevel + "/" + talent.getMaxLevel());
                } else {
                    lore.add("§8Non débloqué");
                }

                if (talent.getPrerequisiteId() != null) {
                    ClassTalent prereq = talentTree.getTalent(talent.getPrerequisiteId());
                    boolean hasPrereq = data.getTalentLevel(talent.getPrerequisiteId()) >= prereq.getMaxLevel();
                    lore.add("");
                    lore.add(hasPrereq
                        ? "§a✓ Prérequis: " + prereq.getName()
                        : "§c✗ Requiert: " + prereq.getName() + " max");
                }

                lore.add("");
                if (isMaxed) {
                    lore.add("§a✓ NIVEAU MAXIMUM");
                } else if (canUnlock && data.getAvailableTalentPoints() >= talent.getPointCost()) {
                    lore.add("§e▶ Clic pour améliorer");
                } else if (!canUnlock) {
                    lore.add("§c✗ Prérequis manquant");
                } else {
                    lore.add("§c✗ Points insuffisants");
                }

                gui.setItem(slot, new ItemBuilder(icon)
                    .name((currentLevel > 0 ? "§a" : "§7") + talent.getName() +
                        (isMaxed ? " §6✦" : ""))
                    .lore(lore)
                    .glow(isMaxed)
                    .build());

                slotToTalent.put(slot, talent.getId());
            }
        }

        // Indicateurs de tier à gauche
        for (int tier = 1; tier <= 5; tier++) {
            int slot = tier * 9;
            String tierName = switch (tier) {
                case 1 -> "§7Tier I - Novice";
                case 2 -> "§a Tier II - Apprenti";
                case 3 -> "§bTier III - Expert";
                case 4 -> "§dTier IV - Maître";
                case 5 -> "§6Tier V - Ultime";
                default -> "";
            };

            gui.setItem(slot, new ItemBuilder(
                tier == 5 ? Material.NETHER_STAR : Material.BOOK)
                .name(tierName)
                .lore("", "§8Débloquez les talents", "§8pour accéder au tier suivant")
                .build());
        }

        // Navigation à droite
        gui.setItem(8, new ItemBuilder(Material.ARROW)
            .name("§c← Retour")
            .lore("", "§7Retour au menu principal")
            .build());

        gui.setItem(17, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§6Buffs Arcade")
            .lore(
                "",
                "§7Buffs collectés: §f" + data.getTotalBuffCount(),
                "",
                "§e▶ Clic pour voir vos buffs"
            )
            .build());

        gui.setItem(26, new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§9Compétences Actives")
            .lore(
                "",
                "§7Gérez vos compétences équipées",
                "",
                "§e▶ Clic pour ouvrir"
            )
            .build());

        gui.setItem(35, new ItemBuilder(Material.NETHERITE_SWORD)
            .name("§cArmes de Classe")
            .lore(
                "",
                "§7Armes exclusives à votre classe",
                "",
                "§e▶ Clic pour voir"
            )
            .build());

        gui.setItem(44, new ItemBuilder(Material.TNT)
            .name("§4Reset Talents")
            .lore(
                "",
                "§7Réinitialise tous vos talents",
                "§7Coût: §c100 Gemmes",
                "",
                "§c⚠ Cette action est définitive!"
            )
            .build());

        gui.setItem(53, new ItemBuilder(Material.COMPASS)
            .name("§eMutations du Jour")
            .lore(
                "",
                "§7Voir les mutations actives",
                "",
                "§e▶ Clic pour voir"
            )
            .build());

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        ClassData data = classManager.getClassData(player);

        // Onglets de branches
        if (slot == 1) {
            open(player, TalentBranch.OFFENSE);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        if (slot == 4) {
            open(player, TalentBranch.DEFENSE);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        if (slot == 7) {
            open(player, TalentBranch.SPECIALTY);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        // Clic sur un talent
        if (slotToTalent.containsKey(slot)) {
            String talentId = slotToTalent.get(slot);
            if (classManager.unlockTalent(player, talentId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                open(player, playerBranch.getOrDefault(player.getUniqueId(), TalentBranch.OFFENSE));
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            }
            return;
        }

        // Boutons de navigation
        switch (slot) {
            case 8 -> { // Retour
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            case 17 -> { // Buffs Arcade
                new BuffsGUI(plugin, classManager).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            case 26 -> { // Compétences
                new SkillsGUI(plugin, classManager).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            case 35 -> { // Armes
                new ClassWeaponsGUI(plugin, classManager).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            case 44 -> { // Reset talents
                if (event.isShiftClick()) {
                    if (classManager.resetTalents(player, false)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                        open(player, playerBranch.getOrDefault(player.getUniqueId(), TalentBranch.OFFENSE));
                    }
                } else {
                    player.sendMessage("§c⚠ Shift+Clic pour confirmer le reset (100 gemmes)");
                }
            }
            case 53 -> { // Mutations
                showMutations(player);
            }
        }

        // Clic droit sur header = changer de classe
        if (slot == 4 && event.isRightClick()) {
            new ClassSelectionGUI(plugin, classManager).open(player);
        }
    }

    private void showMutations(Player player) {
        List<String> summary = classManager.getMutationManager().getMutationSummary();
        for (String line : summary) {
            player.sendMessage(line);
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}
