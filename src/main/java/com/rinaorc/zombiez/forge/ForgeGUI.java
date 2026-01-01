package com.rinaorc.zombiez.forge;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface graphique de la Forge
 *
 * Layout (54 slots = 6 lignes):
 * - Ligne 0: Header avec titre et infos
 * - Ligne 1: Espace
 * - Ligne 2: [Slot Item] -> [Preview +1]
 * - Ligne 3: Infos (coût, chance, pénalité)
 * - Ligne 4: [Pierre Protection] [Pierre Chance] [FORGER]
 * - Ligne 5: Footer
 */
public class ForgeGUI implements InventoryHolder {

    private static final String TITLE = "§0\u2800\u2800\u2800\u2800\u2800\u2800\u2800🔨 Forge de l'Ancien";
    private static final int SIZE = 54;

    // Slots
    private static final int SLOT_ITEM = 20;          // Item à forger
    private static final int SLOT_ARROW = 22;         // Flèche
    private static final int SLOT_PREVIEW = 24;       // Preview du résultat
    private static final int SLOT_COST_INFO = 30;     // Info coût
    private static final int SLOT_CHANCE_INFO = 31;   // Info chance
    private static final int SLOT_PENALTY_INFO = 32;  // Info pénalité
    private static final int SLOT_PROTECTION = 38;    // Pierre protection
    private static final int SLOT_CHANCE = 40;        // Pierre chance
    private static final int SLOT_FORGE = 42;         // Bouton forger
    private static final int SLOT_POINTS = 4;         // Solde points
    private static final int SLOT_STATS = 49;         // Stats du joueur
    private static final int SLOT_CLOSE = 53;         // Fermer

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final ForgeManager forgeManager;

    // État
    private ItemStack itemToForge = null;
    private boolean useProtection = false;
    private boolean useChance = false;

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.#");

    public ForgeGUI(ZombieZPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.forgeManager = plugin.getForgeManager();
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);

        setupGUI();
    }

    private void setupGUI() {
        // Fond gris
        ItemStack grayGlass = ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, grayGlass);
        }

        // Header violet
        ItemStack purpleGlass = ItemBuilder.placeholder(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, purpleGlass);
        }

        // Afficher les points du joueur
        updatePointsDisplay();

        // Slot item (vide au départ)
        inventory.setItem(SLOT_ITEM, createItemSlot());

        // Flèche
        inventory.setItem(SLOT_ARROW, new ItemBuilder(Material.ARROW)
            .name("§7→ §eRésultat")
            .build());

        // Preview (vide au départ)
        inventory.setItem(SLOT_PREVIEW, createPreviewSlot());

        // Infos (désactivées au départ)
        updateForgeInfo();

        // Pierres
        updateStoneSlots();

        // Bouton forger
        updateForgeButton();

        // Stats du joueur
        updateStatsDisplay();

        // Fermer
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
            .name("§c✖ Fermer")
            .build());
    }

    /**
     * Met à jour l'affichage des points
     */
    private void updatePointsDisplay() {
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        long points = playerData != null ? playerData.getPoints().get() : 0;

        inventory.setItem(SLOT_POINTS, new ItemBuilder(Material.GOLD_INGOT)
            .name("§e§l⚡ " + String.format("%,d", points) + " Points")
            .lore(List.of(
                "",
                "§7Vos points disponibles",
                "§7pour la forge."
            ))
            .build());
    }

    /**
     * Crée le slot d'item vide
     */
    private ItemStack createItemSlot() {
        if (itemToForge != null) {
            return itemToForge.clone();
        }

        return new ItemBuilder(Material.GRAY_DYE)
            .name("§7📦 Déposez un item ici")
            .lore(List.of(
                "",
                "§7Cliquez avec un item",
                "§7équipable pour le forger.",
                "",
                "§8Armes et armures uniquement"
            ))
            .build();
    }

    /**
     * Crée le slot de preview
     */
    private ItemStack createPreviewSlot() {
        if (itemToForge == null) {
            return new ItemBuilder(Material.LIGHT_GRAY_DYE)
                .name("§8Résultat")
                .lore(List.of("", "§7Déposez d'abord un item"))
                .build();
        }

        int currentLevel = forgeManager.getForgeLevel(itemToForge);

        if (currentLevel >= ForgeManager.MAX_FORGE_LEVEL) {
            return new ItemBuilder(Material.NETHER_STAR)
                .name("§6§l★ NIVEAU MAXIMUM ★")
                .lore(List.of(
                    "",
                    "§7Cet item est déjà au",
                    "§7niveau maximum §6+10§7!",
                    "",
                    "§aBonus actuel: §e+100% stats"
                ))
                .build();
        }

        // Créer une preview de l'item au niveau suivant
        int nextLevel = currentLevel + 1;
        int nextBonus = forgeManager.getStatBonus(nextLevel);
        int currentBonus = forgeManager.getStatBonus(currentLevel);

        ItemStack preview = itemToForge.clone();
        // On ne modifie pas vraiment l'item, juste l'affichage

        return new ItemBuilder(Material.ENCHANTED_BOOK)
            .name("§a§l↑ Résultat: §e+" + nextLevel)
            .lore(List.of(
                "",
                "§7Niveau actuel: §e+" + currentLevel + " §7(+" + currentBonus + "% stats)",
                "§7Après forge: §a+" + nextLevel + " §7(+" + nextBonus + "% stats)",
                "",
                "§7Gain: §a+" + (nextBonus - currentBonus) + "% §7stats"
            ))
            .glow()
            .build();
    }

    /**
     * Met à jour les informations de forge
     */
    private void updateForgeInfo() {
        if (itemToForge == null || forgeManager.getForgeLevel(itemToForge) >= ForgeManager.MAX_FORGE_LEVEL) {
            // Slots désactivés
            inventory.setItem(SLOT_COST_INFO, new ItemBuilder(Material.GRAY_DYE)
                .name("§8Coût")
                .lore(List.of("§7Déposez un item"))
                .build());

            inventory.setItem(SLOT_CHANCE_INFO, new ItemBuilder(Material.GRAY_DYE)
                .name("§8Chance")
                .lore(List.of("§7Déposez un item"))
                .build());

            inventory.setItem(SLOT_PENALTY_INFO, new ItemBuilder(Material.GRAY_DYE)
                .name("§8Pénalité")
                .lore(List.of("§7Déposez un item"))
                .build());
            return;
        }

        int currentLevel = forgeManager.getForgeLevel(itemToForge);
        int cost = forgeManager.getForgeCost(itemToForge);
        double chance = forgeManager.getSuccessChance(itemToForge);
        int penalty = forgeManager.getFailurePenalty(itemToForge);

        // Ajuster la chance si pierre utilisée
        double displayChance = chance;
        if (useChance) {
            displayChance = Math.min(1.0, chance + 0.15);
        }

        // Coût
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        long playerPoints = playerData != null ? playerData.getPoints().get() : 0;
        boolean canAfford = playerPoints >= cost;

        inventory.setItem(SLOT_COST_INFO, new ItemBuilder(canAfford ? Material.GOLD_INGOT : Material.COAL)
            .name((canAfford ? "§e" : "§c") + "§l💰 Coût: " + String.format("%,d", cost) + " pts")
            .lore(List.of(
                "",
                "§7Vous avez: " + (canAfford ? "§a" : "§c") + String.format("%,d", playerPoints),
                canAfford ? "§a✓ Suffisant" : "§c✗ Insuffisant"
            ))
            .build());

        // Chance
        String chanceColor = displayChance >= 0.7 ? "§a" : (displayChance >= 0.4 ? "§e" : "§c");
        List<String> chanceLore = new ArrayList<>();
        chanceLore.add("");
        chanceLore.add("§7Chance de base: §f" + PERCENT_FORMAT.format(chance * 100) + "%");
        if (useChance) {
            chanceLore.add("§b+15% Pierre de Chance");
            chanceLore.add("");
            chanceLore.add("§7Total: " + chanceColor + PERCENT_FORMAT.format(displayChance * 100) + "%");
        }

        inventory.setItem(SLOT_CHANCE_INFO, new ItemBuilder(Material.RABBIT_FOOT)
            .name(chanceColor + "§l🎲 Chance: " + PERCENT_FORMAT.format(displayChance * 100) + "%")
            .lore(chanceLore)
            .build());

        // Pénalité
        String penaltyText;
        Material penaltyMat;
        List<String> penaltyLore = new ArrayList<>();

        if (penalty == 0) {
            penaltyText = "§a§l✓ Aucune pénalité";
            penaltyMat = Material.LIME_DYE;
            penaltyLore.add("");
            penaltyLore.add("§7Phase §a§lSAFE§7:");
            penaltyLore.add("§7Pas de perte en cas d'échec!");
        } else {
            if (useProtection) {
                penaltyText = "§d§l🛡 Protégé";
                penaltyMat = Material.AMETHYST_SHARD;
                penaltyLore.add("");
                penaltyLore.add("§7Pénalité normale: §c-" + penalty + " niveau(x)");
                penaltyLore.add("§d→ Pierre de Protection active");
                penaltyLore.add("§a→ Aucune perte en cas d'échec!");
            } else {
                penaltyText = "§c§l⚠ -" + penalty + " niveau(x)";
                penaltyMat = Material.REDSTONE;
                penaltyLore.add("");
                penaltyLore.add("§7En cas d'échec, l'item");
                penaltyLore.add("§7perdra §c" + penalty + " niveau(x)§7.");

                int newLevel = Math.max(0, currentLevel - penalty);
                penaltyLore.add("");
                penaltyLore.add("§7+" + (currentLevel + 1) + " → §c+" + newLevel);
            }
        }

        inventory.setItem(SLOT_PENALTY_INFO, new ItemBuilder(penaltyMat)
            .name(penaltyText)
            .lore(penaltyLore)
            .build());
    }

    /**
     * Met à jour les slots des pierres
     */
    private void updateStoneSlots() {
        // Pierre de protection
        int protectionCount = forgeManager.countProtectionStones(player);
        boolean hasProtection = protectionCount > 0;

        List<String> protLore = new ArrayList<>();
        protLore.add("");
        protLore.add("§7Protège l'item en cas d'échec.");
        protLore.add("§7Aucune perte de niveau!");
        protLore.add("");
        protLore.add("§7Vous avez: " + (hasProtection ? "§a" : "§c") + protectionCount);
        protLore.add("");
        if (useProtection) {
            protLore.add("§a§l✓ ACTIVÉE");
            protLore.add("§7Clic pour désactiver");
        } else if (hasProtection) {
            protLore.add("§e§l○ Non utilisée");
            protLore.add("§7Clic pour activer");
        } else {
            protLore.add("§c§l✗ Aucune pierre");
        }

        inventory.setItem(SLOT_PROTECTION, new ItemBuilder(useProtection ? Material.AMETHYST_BLOCK : Material.AMETHYST_SHARD)
            .name((useProtection ? "§d§l" : "§7") + "🛡 Pierre de Protection")
            .lore(protLore)
            .glow(useProtection)
            .build());

        // Pierre de chance
        int chanceCount = countChanceStones();
        boolean hasChance = chanceCount > 0;

        List<String> chanceLore = new ArrayList<>();
        chanceLore.add("");
        chanceLore.add("§7Augmente les chances de");
        chanceLore.add("§7succès de §a+15%§7.");
        chanceLore.add("");
        chanceLore.add("§7Vous avez: " + (hasChance ? "§a" : "§c") + chanceCount);
        chanceLore.add("");
        if (useChance) {
            chanceLore.add("§b§l✓ ACTIVÉE");
            chanceLore.add("§7Clic pour désactiver");
        } else if (hasChance) {
            chanceLore.add("§e§l○ Non utilisée");
            chanceLore.add("§7Clic pour activer");
        } else {
            chanceLore.add("§c§l✗ Aucune pierre");
        }

        inventory.setItem(SLOT_CHANCE, new ItemBuilder(useChance ? Material.PRISMARINE_SHARD : Material.PRISMARINE_CRYSTALS)
            .name((useChance ? "§b§l" : "§7") + "🍀 Pierre de Chance")
            .lore(chanceLore)
            .glow(useChance)
            .build());
    }

    private int countChanceStones() {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (forgeManager.isChanceStone(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Met à jour le bouton forger
     */
    private void updateForgeButton() {
        if (itemToForge == null) {
            inventory.setItem(SLOT_FORGE, new ItemBuilder(Material.GRAY_DYE)
                .name("§8🔨 FORGER")
                .lore(List.of("", "§7Déposez d'abord un item"))
                .build());
            return;
        }

        int currentLevel = forgeManager.getForgeLevel(itemToForge);

        if (currentLevel >= ForgeManager.MAX_FORGE_LEVEL) {
            inventory.setItem(SLOT_FORGE, new ItemBuilder(Material.NETHER_STAR)
                .name("§6§l★ NIVEAU MAXIMUM")
                .lore(List.of("", "§7Cet item ne peut plus être forgé!"))
                .glow()
                .build());
            return;
        }

        int cost = forgeManager.getForgeCost(itemToForge);
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        long playerPoints = playerData != null ? playerData.getPoints().get() : 0;
        boolean canAfford = playerPoints >= cost;

        double chance = forgeManager.getSuccessChance(itemToForge);
        if (useChance) chance = Math.min(1.0, chance + 0.15);

        int nextLevel = currentLevel + 1;

        if (canAfford) {
            inventory.setItem(SLOT_FORGE, new ItemBuilder(Material.ANVIL)
                .name("§a§l🔨 FORGER")
                .lore(List.of(
                    "",
                    "§7Cliquez pour tenter la forge!",
                    "",
                    "§7Résultat: §e+" + currentLevel + " §7→ §a+" + nextLevel,
                    "§7Coût: §e" + String.format("%,d", cost) + " pts",
                    "§7Chance: §e" + PERCENT_FORMAT.format(chance * 100) + "%",
                    "",
                    "§e▶ Clic gauche pour forger"
                ))
                .build());
        } else {
            inventory.setItem(SLOT_FORGE, new ItemBuilder(Material.BARRIER)
                .name("§c§l🔨 FORGER")
                .lore(List.of(
                    "",
                    "§cPoints insuffisants!",
                    "",
                    "§7Besoin: §c" + String.format("%,d", cost),
                    "§7Vous avez: §c" + String.format("%,d", playerPoints)
                ))
                .build());
        }
    }

    /**
     * Met à jour l'affichage des stats
     */
    private void updateStatsDisplay() {
        ForgeManager.ForgeStats stats = forgeManager.getStats(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (stats != null) {
            lore.add("§7Tentatives: §f" + stats.getTotalAttempts());
            lore.add("§7Succès: §a" + stats.getTotalSuccess());
            lore.add("§7Échecs: §c" + stats.getTotalFailures());
            lore.add("");
            lore.add("§7Taux de réussite: §e" + PERCENT_FORMAT.format(stats.getSuccessRate()) + "%");
            lore.add("§7Plus haut niveau: §6+" + stats.getHighestLevel());
            lore.add("§7Items +10: §6" + stats.getItemsAtMax());
        } else {
            lore.add("§8Aucune statistique");
            lore.add("§8Forgez des items!");
        }

        inventory.setItem(SLOT_STATS, new ItemBuilder(Material.BOOK)
            .name("§e§l📊 Vos Statistiques")
            .lore(lore)
            .build());
    }

    // ==================== ACTIONS ====================

    /**
     * Place un item dans la forge
     */
    public void setItem(ItemStack item) {
        if (item != null && !forgeManager.canBeForged(item)) {
            player.sendMessage("§c[Forge] §7Cet item ne peut pas être forgé!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        this.itemToForge = item != null ? item.clone() : null;
        this.useProtection = false;
        this.useChance = false;

        refreshGUI();
    }

    /**
     * Récupère l'item de la forge
     */
    public ItemStack retrieveItem() {
        ItemStack item = this.itemToForge;
        this.itemToForge = null;
        refreshGUI();
        return item;
    }

    /**
     * Toggle pierre de protection
     */
    public void toggleProtection() {
        if (forgeManager.countProtectionStones(player) > 0 || useProtection) {
            useProtection = !useProtection;
            refreshGUI();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, useProtection ? 1.5f : 1f);
        } else {
            player.sendMessage("§c[Forge] §7Vous n'avez pas de Pierre de Protection!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    /**
     * Toggle pierre de chance
     */
    public void toggleChance() {
        if (countChanceStones() > 0 || useChance) {
            useChance = !useChance;
            refreshGUI();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, useChance ? 1.5f : 1f);
        } else {
            player.sendMessage("§c[Forge] §7Vous n'avez pas de Pierre de Chance!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    /**
     * Tente la forge
     */
    public void attemptForge() {
        if (itemToForge == null) {
            player.sendMessage("§c[Forge] §7Déposez d'abord un item!");
            return;
        }

        // Consommer les pierres si utilisées
        if (useProtection) {
            if (!forgeManager.consumeProtectionStone(player)) {
                player.sendMessage("§c[Forge] §7Pierre de Protection introuvable!");
                useProtection = false;
                refreshGUI();
                return;
            }
        }

        if (useChance) {
            if (!forgeManager.consumeChanceStone(player)) {
                player.sendMessage("§c[Forge] §7Pierre de Chance introuvable!");
                useChance = false;
                refreshGUI();
                return;
            }
        }

        // Tenter la forge
        ForgeManager.ForgeResult result = forgeManager.attemptForge(player, itemToForge, useProtection, useChance);

        // Réinitialiser les pierres
        useProtection = false;
        useChance = false;

        // Afficher le résultat
        if (result.success()) {
            player.sendTitle("§a§l✓ SUCCÈS!", "§e+" + result.newLevel(), 5, 30, 10);
            player.sendMessage("§a[Forge] §f" + result.message());

            // Particules de succès
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        } else {
            if (result.type() == ForgeManager.ForgeResultType.PROTECTED) {
                player.sendTitle("§d§l🛡 PROTÉGÉ", "§7Aucune perte!", 5, 30, 10);
            } else if (result.type() == ForgeManager.ForgeResultType.FAILURE) {
                player.sendTitle("§c§l✗ ÉCHEC", "§7→ +" + result.newLevel(), 5, 30, 10);
            }
            player.sendMessage("§c[Forge] §f" + result.message());

            // Particules d'échec
            player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        }

        // Mettre à jour l'affichage
        refreshGUI();
    }

    /**
     * Rafraîchit l'affichage
     */
    public void refreshGUI() {
        inventory.setItem(SLOT_ITEM, createItemSlot());
        inventory.setItem(SLOT_PREVIEW, createPreviewSlot());
        updatePointsDisplay();
        updateForgeInfo();
        updateStoneSlots();
        updateForgeButton();
        updateStatsDisplay();
    }

    /**
     * Ouvre la GUI
     */
    public void open() {
        player.openInventory(inventory);
    }

    // ==================== GETTERS ====================

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public ItemStack getItemToForge() {
        return itemToForge;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isUseProtection() {
        return useProtection;
    }

    public boolean isUseChance() {
        return useChance;
    }

    // Slots accessibles depuis le listener
    public static int getSlotItem() { return SLOT_ITEM; }
    public static int getSlotProtection() { return SLOT_PROTECTION; }
    public static int getSlotChance() { return SLOT_CHANCE; }
    public static int getSlotForge() { return SLOT_FORGE; }
    public static int getSlotClose() { return SLOT_CLOSE; }
}
