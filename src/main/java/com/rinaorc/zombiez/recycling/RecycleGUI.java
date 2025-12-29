package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface graphique pour configurer le recyclage automatique
 * Permet d'activer/désactiver le recyclage par rareté
 */
public class RecycleGUI implements Listener {

    private static final String GUI_TITLE = "§6§l♻ Recyclage Automatique";
    private static final String MANUAL_GUI_TITLE = "§6§l♻ Recyclage Manuel";
    private static final int GUI_SIZE = 45; // 5 lignes
    private static final int MANUAL_GUI_SIZE = 54; // 6 lignes

    private final ZombieZPlugin plugin;
    private final RecycleManager recycleManager;

    // Slots pour chaque élément (menu principal)
    private static final int SLOT_TOGGLE_MAIN = 4;        // Toggle principal
    private static final int SLOT_TOGGLE_CONSUMABLES = 39; // Toggle consommables
    private static final int SLOT_STATS = 40;             // Statistiques
    private static final int SLOT_PROTECT_HOTBAR = 41;    // Protection hotbar
    private static final int SLOT_MANUAL_RECYCLE = 42;    // Recyclage manuel (ex-milestones)

    // Slots pour les raretés (ligne du milieu)
    private static final int[] RARITY_SLOTS = {10, 12, 14, 16, 28, 30, 32};

    // Slots pour le menu de recyclage manuel
    private static final int SLOT_MANUAL_RECYCLE_BTN = 49;  // Bouton recycler (vert)
    private static final int SLOT_MANUAL_BACK = 45;         // Bouton retour
    // Slots où les joueurs peuvent déposer des items (3 lignes centrales)
    private static final int[] MANUAL_ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Ligne 2
        19, 20, 21, 22, 23, 24, 25,  // Ligne 3
        28, 29, 30, 31, 32, 33, 34   // Ligne 4
    };

    // Tracker pour les joueurs avec le menu manuel ouvert
    private final Map<UUID, Inventory> manualRecycleInventories = new HashMap<>();

    // Matériaux pour représenter chaque rareté
    private static final Material[] RARITY_MATERIALS = {
        Material.WHITE_WOOL,      // COMMON
        Material.LIME_WOOL,       // UNCOMMON
        Material.BLUE_WOOL,       // RARE
        Material.PURPLE_WOOL,     // EPIC
        Material.ORANGE_WOOL,     // LEGENDARY
        Material.MAGENTA_WOOL,    // MYTHIC
        Material.RED_WOOL         // EXALTED
    };

    public RecycleGUI(ZombieZPlugin plugin, RecycleManager recycleManager) {
        this.plugin = plugin;
        this.recycleManager = recycleManager;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le menu de recyclage pour un joueur
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        RecycleSettings settings = recycleManager.getSettings(player.getUniqueId());
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        int currentZone = playerData != null ? playerData.getCurrentZone().get() : 1;

        // Bordure décorative
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, border);
            }
        }

        // Toggle principal (slot 4)
        boolean mainEnabled = settings.isAutoRecycleEnabled();
        inv.setItem(SLOT_TOGGLE_MAIN, new ItemBuilder(mainEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
            .name(mainEnabled ? "§a§l✓ RECYCLAGE ACTIVÉ" : "§c§l✗ RECYCLAGE DÉSACTIVÉ")
            .lore(
                "",
                "§7Quand activé, les items des",
                "§7raretés sélectionnées seront",
                "§7automatiquement recyclés en points",
                "§7au ramassage.",
                "",
                mainEnabled ? "§cClic pour désactiver" : "§aClic pour activer"
            )
            .glow(mainEnabled)
            .build());

        // Info centrale
        inv.setItem(22, new ItemBuilder(Material.BOOK)
            .name("§e§lComment ça marche ?")
            .lore(
                "§7Le recyclage automatique convertit",
                "§7les items en §6points §7instantanément",
                "§7quand vous les ramassez.",
                "",
                "§6⚡ Formule des points:",
                "§7Points = Base × (1 + Zone × 0.15)",
                "",
                "§7Plus la §erareté §7est haute et",
                "§7plus la §bzone §7est élevée,",
                "§7plus vous gagnez de points!",
                "",
                "§7Un §arésumé toutes les minutes",
                "§7vous indique vos gains."
            )
            .build());

        // Items pour chaque rareté
        Rarity[] rarities = Rarity.values();
        for (int i = 0; i < rarities.length && i < RARITY_SLOTS.length; i++) {
            Rarity rarity = rarities[i];
            boolean enabled = settings.isRarityEnabled(rarity);
            int basePoints = RecycleManager.BASE_POINTS_BY_RARITY[i];
            int currentPoints = recycleManager.calculateRecyclePoints(rarity, currentZone);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Chance de drop: §f" + String.format("%.2f%%", rarity.getBaseChance()));
            lore.add("");
            lore.add("§6⚡ Points de recyclage:");
            lore.add("  §7Base: §f" + basePoints + " pts");
            lore.add("  §7Zone " + currentZone + ": §e" + currentPoints + " pts");
            lore.add("  §7Zone 50: §6" + recycleManager.calculateRecyclePoints(rarity, 50) + " pts");
            lore.add("");

            if (rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
                lore.add("§c⚠ Attention: Cette rareté est précieuse!");
                lore.add("§c   Êtes-vous sûr de vouloir l'activer?");
                lore.add("");
            }

            lore.add(enabled ? "§aRecyclage: ACTIVÉ" : "§cRecyclage: DÉSACTIVÉ");
            lore.add("");
            lore.add(enabled ? "§7Clic pour §cdésactiver" : "§7Clic pour §aactiver");

            Material mat = enabled ? RARITY_MATERIALS[i] : Material.GRAY_WOOL;

            inv.setItem(RARITY_SLOTS[i], new ItemBuilder(mat)
                .name(rarity.getChatColor() + "§l" + rarity.getDisplayName().toUpperCase() + " " + rarity.getStars())
                .lore(lore)
                .glow(enabled)
                .build());
        }

        // Toggle consommables (slot 39)
        boolean consumablesEnabled = settings.isRecycleConsumablesEnabled();
        inv.setItem(SLOT_TOGGLE_CONSUMABLES, new ItemBuilder(consumablesEnabled ? Material.BREWING_STAND : Material.GLASS_BOTTLE)
            .name(consumablesEnabled ? "§a§l✓ CONSOMMABLES ACTIVÉS" : "§c§l✗ CONSOMMABLES DÉSACTIVÉS")
            .lore(
                "",
                "§7Recycle automatiquement les",
                "§7consommables (grenades, soins,",
                "§7jetpacks, etc.) en points.",
                "",
                "§6⚡ Points par rareté:",
                "  §f• Commun: §e3 pts §7(base)",
                "  §a• Peu Commun: §e8 pts §7(base)",
                "  §9• Rare: §e20 pts §7(base)",
                "  §5• Épique: §e50 pts §7(base)",
                "  §6• Légendaire: §e150 pts §7(base)",
                "",
                "§7Les points augmentent",
                "§7selon la zone de l'item.",
                "",
                consumablesEnabled ? "§cClic pour désactiver" : "§aClic pour activer"
            )
            .glow(consumablesEnabled)
            .build());

        // Statistiques (slot 40)
        inv.setItem(SLOT_STATS, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§l📊 Statistiques")
            .lore(
                "",
                "§7Session actuelle:",
                "  §fItems recyclés: §e" + settings.getSessionItemsRecycled().get(),
                "  §fPoints gagnés: §6" + formatPoints(settings.getSessionPointsEarned().get()),
                "",
                "§7Total (tous temps):",
                "  §fItems recyclés: §e" + settings.getTotalItemsRecycled().get(),
                "  §fPoints gagnés: §6" + formatPoints(settings.getTotalPointsEarned().get()),
                "",
                "§7Raretés activées: §f" + settings.getEnabledRaritiesCount() + "/7",
                "§7Consommables: " + (consumablesEnabled ? "§aActivé" : "§cDésactivé")
            )
            .build());

        // Protection de la hotbar (slot 41)
        boolean protectHotbar = settings.isProtectHotbarEnabled();
        inv.setItem(SLOT_PROTECT_HOTBAR, new ItemBuilder(protectHotbar ? Material.SHIELD : Material.IRON_SWORD)
            .name(protectHotbar ? "§a§l🛡 HOTBAR PROTÉGÉE" : "§c§l⚔ HOTBAR NON PROTÉGÉE")
            .lore(
                "",
                "§7Quand activé, les items dans",
                "§7votre hotbar (9 premiers slots)",
                "§7ne seront §eJAMAIS §7recyclés.",
                "",
                "§6⚠ Sécurité recommandée!",
                "§7Évite de recycler vos armes,",
                "§7outils et items importants.",
                "",
                protectHotbar ? "§aProtection: ACTIVÉE" : "§cProtection: DÉSACTIVÉE",
                "",
                protectHotbar ? "§7Clic pour §cdésactiver" : "§7Clic pour §aactiver"
            )
            .glow(protectHotbar)
            .build());

        // Bouton "Activer tout" (slot 37)
        inv.setItem(37, new ItemBuilder(Material.EMERALD)
            .name("§a§lActiver Tout")
            .lore(
                "",
                "§7Active le recyclage pour",
                "§7toutes les raretés.",
                "",
                "§c⚠ Inclut les raretés précieuses!"
            )
            .build());

        // Bouton "Désactiver tout" (slot 38)
        inv.setItem(38, new ItemBuilder(Material.BARRIER)
            .name("§c§lDésactiver Tout")
            .lore(
                "",
                "§7Désactive le recyclage",
                "§7pour toutes les raretés."
            )
            .build());

        // Bouton "Recyclage Manuel" (slot 42)
        inv.setItem(SLOT_MANUAL_RECYCLE, new ItemBuilder(Material.HOPPER)
            .name("§e§l⚙ Recyclage Manuel")
            .lore(
                "",
                "§7Ouvrez un menu pour déposer",
                "§7des items à recycler manuellement.",
                "",
                "§7Parfait pour recycler des items",
                "§7spécifiques sans activer le",
                "§7recyclage automatique.",
                "",
                "§eClic pour ouvrir"
            )
            .build());

        // Bouton "Seulement Common/Uncommon" (slot 43)
        inv.setItem(43, new ItemBuilder(Material.DIAMOND)
            .name("§b§lMode Sécurisé")
            .lore(
                "",
                "§7Active uniquement §fCommun",
                "§7et §aPeu Commun§7.",
                "",
                "§7Recommandé pour éviter",
                "§7de recycler des items rares!"
            )
            .build());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        RecycleSettings settings = recycleManager.getSettings(player.getUniqueId());

        // Toggle principal
        if (slot == SLOT_TOGGLE_MAIN) {
            boolean newState = !settings.isAutoRecycleEnabled();
            settings.setAutoRecycleEnabled(newState);

            // Synchroniser immédiatement vers PlayerData
            recycleManager.syncToPlayerData(player.getUniqueId());

            player.playSound(player.getLocation(),
                newState ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                0.7f, newState ? 1.5f : 0.8f);

            if (newState) {
                player.sendMessage("§a§l♻ §aRecyclage automatique §lactivé§a!");
                player.sendMessage("§7Les items des raretés sélectionnées seront recyclés au ramassage.");
            } else {
                player.sendMessage("§c§l♻ §cRecyclage automatique §ldésactivé§c.");
            }

            // Rafraîchir le menu
            open(player);
            return;
        }

        // Toggle consommables
        if (slot == SLOT_TOGGLE_CONSUMABLES) {
            boolean newState = !settings.isRecycleConsumablesEnabled();
            settings.setRecycleConsumablesEnabled(newState);

            // Synchroniser immédiatement vers PlayerData
            recycleManager.syncToPlayerData(player.getUniqueId());

            player.playSound(player.getLocation(),
                newState ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                0.5f, newState ? 1.3f : 0.9f);

            if (newState) {
                player.sendMessage("§a§l♻ §aRecyclage des consommables §lactivé§a!");
                player.sendMessage("§7Les grenades, soins, jetpacks seront recyclés au ramassage.");
            } else {
                player.sendMessage("§c§l♻ §cRecyclage des consommables §ldésactivé§c.");
            }

            // Rafraîchir le menu
            open(player);
            return;
        }

        // Toggle protection hotbar
        if (slot == SLOT_PROTECT_HOTBAR) {
            boolean newState = !settings.isProtectHotbarEnabled();
            settings.setProtectHotbarEnabled(newState);

            // Synchroniser immédiatement vers PlayerData
            recycleManager.syncToPlayerData(player.getUniqueId());

            player.playSound(player.getLocation(),
                newState ? Sound.BLOCK_ANVIL_LAND : Sound.BLOCK_NOTE_BLOCK_BASS,
                0.5f, newState ? 1.2f : 0.8f);

            if (newState) {
                player.sendMessage("§a§l🛡 §aProtection de la hotbar §lactivée§a!");
                player.sendMessage("§7Les items dans vos 9 premiers slots ne seront jamais recyclés.");
            } else {
                player.sendMessage("§c§l⚔ §cProtection de la hotbar §ldésactivée§c!");
                player.sendMessage("§c⚠ Attention: Vos items de hotbar peuvent maintenant être recyclés!");
            }

            // Rafraîchir le menu
            open(player);
            return;
        }

        // Toggle par rareté
        for (int i = 0; i < RARITY_SLOTS.length; i++) {
            if (slot == RARITY_SLOTS[i]) {
                Rarity rarity = Rarity.values()[i];
                boolean newState = settings.toggleRecycleRarity(rarity);

                // Synchroniser immédiatement vers PlayerData
                recycleManager.syncToPlayerData(player.getUniqueId());

                player.playSound(player.getLocation(),
                    newState ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                    0.5f, newState ? 1.3f : 0.9f);

                // Avertissement pour les raretés précieuses
                if (newState && rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
                    player.sendMessage("§c⚠ §eAttention: §7Vous avez activé le recyclage pour §l"
                        + rarity.getColoredName() + "§7!");
                    player.sendMessage("§7Ces items sont précieux, assurez-vous de le vouloir.");
                }

                // Rafraîchir le menu
                open(player);
                return;
            }
        }

        // Activer tout
        if (slot == 37) {
            for (Rarity rarity : Rarity.values()) {
                settings.setRecycleRarity(rarity, true);
            }
            recycleManager.syncToPlayerData(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            player.sendMessage("§a♻ Toutes les raretés sont maintenant recyclées automatiquement!");
            player.sendMessage("§c⚠ Attention: Cela inclut les items Légendaires, Mythiques et Exaltés!");
            open(player);
            return;
        }

        // Désactiver tout
        if (slot == 38) {
            for (Rarity rarity : Rarity.values()) {
                settings.setRecycleRarity(rarity, false);
            }
            recycleManager.syncToPlayerData(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            player.sendMessage("§c♻ Recyclage désactivé pour toutes les raretés.");
            open(player);
            return;
        }

        // Recyclage Manuel
        if (slot == SLOT_MANUAL_RECYCLE) {
            player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.5f, 1.2f);
            openManualRecycleMenu(player);
            return;
        }

        // Mode sécurisé
        if (slot == 43) {
            for (Rarity rarity : Rarity.values()) {
                settings.setRecycleRarity(rarity, rarity == Rarity.COMMON || rarity == Rarity.UNCOMMON);
            }
            recycleManager.syncToPlayerData(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
            player.sendMessage("§b♻ Mode sécurisé activé: seuls §fCommun §7et §aPeu Commun §7seront recyclés.");
            open(player);
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // Fermeture du menu principal
        if (title.equals(GUI_TITLE)) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.3f, 1.2f);
            return;
        }

        // Fermeture du menu de recyclage manuel - rendre les items au joueur
        if (title.equals(MANUAL_GUI_TITLE)) {
            manualRecycleInventories.remove(player.getUniqueId());

            // Rendre les items non recyclés au joueur
            Inventory inv = event.getInventory();
            for (int slot : MANUAL_ITEM_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    // Essayer de donner au joueur, sinon drop au sol
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    for (ItemStack leftover : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }

            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.3f, 1.2f);
        }
    }

    // ==================== MENU RECYCLAGE MANUEL ====================

    /**
     * Ouvre le menu de recyclage manuel
     */
    public void openManualRecycleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, MANUAL_GUI_SIZE, MANUAL_GUI_TITLE);

        // Bordure décorative
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < MANUAL_GUI_SIZE; i++) {
            inv.setItem(i, border);
        }

        // Libérer les slots pour les items
        for (int slot : MANUAL_ITEM_SLOTS) {
            inv.setItem(slot, null);
        }

        // Titre/Info (slot 4)
        inv.setItem(4, new ItemBuilder(Material.HOPPER)
            .name("§e§l⚙ Recyclage Manuel")
            .lore(
                "",
                "§7Glissez les items ZombieZ",
                "§7dans les emplacements vides",
                "§7puis cliquez sur §aRecycler§7.",
                "",
                "§c⚠ Les items seront détruits",
                "§c   et convertis en points!"
            )
            .build());

        // Bouton Retour (slot 45)
        inv.setItem(SLOT_MANUAL_BACK, new ItemBuilder(Material.ARROW)
            .name("§c§l← Retour")
            .lore(
                "",
                "§7Retourner au menu principal",
                "§7du recyclage.",
                "",
                "§7Les items non recyclés vous",
                "§7seront rendus."
            )
            .build());

        // Bouton Recycler (slot 49)
        inv.setItem(SLOT_MANUAL_RECYCLE_BTN, new ItemBuilder(Material.LIME_CONCRETE)
            .name("§a§l♻ RECYCLER")
            .lore(
                "",
                "§7Recycle tous les items",
                "§7placés dans le menu.",
                "",
                "§eClic pour recycler!"
            )
            .glow(true)
            .build());

        // Info sur les points (slot 53)
        PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        int currentZone = playerData != null ? playerData.getCurrentZone().get() : 1;
        inv.setItem(53, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§l💰 Points de Recyclage")
            .lore(
                "",
                "§7Zone actuelle: §e" + currentZone,
                "",
                "§6Points par rareté (zone " + currentZone + "):",
                "  §f• Commun: §e" + recycleManager.calculateRecyclePoints(Rarity.COMMON, currentZone) + " pts",
                "  §a• Peu Commun: §e" + recycleManager.calculateRecyclePoints(Rarity.UNCOMMON, currentZone) + " pts",
                "  §9• Rare: §e" + recycleManager.calculateRecyclePoints(Rarity.RARE, currentZone) + " pts",
                "  §5• Épique: §e" + recycleManager.calculateRecyclePoints(Rarity.EPIC, currentZone) + " pts",
                "  §6• Légendaire: §e" + recycleManager.calculateRecyclePoints(Rarity.LEGENDARY, currentZone) + " pts",
                "  §d• Mythique: §e" + recycleManager.calculateRecyclePoints(Rarity.MYTHIC, currentZone) + " pts",
                "  §c• Exalté: §e" + recycleManager.calculateRecyclePoints(Rarity.EXALTED, currentZone) + " pts"
            )
            .build());

        manualRecycleInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Gère les clics dans le menu de recyclage manuel
     */
    @EventHandler
    public void onManualRecycleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(MANUAL_GUI_TITLE)) return;

        int slot = event.getRawSlot();

        // Permettre le déplacement d'items dans les slots autorisés
        if (slot >= 0 && slot < MANUAL_GUI_SIZE) {
            boolean isItemSlot = false;
            for (int itemSlot : MANUAL_ITEM_SLOTS) {
                if (slot == itemSlot) {
                    isItemSlot = true;
                    break;
                }
            }

            // Si ce n'est pas un slot d'item, bloquer sauf pour les boutons
            if (!isItemSlot) {
                event.setCancelled(true);

                // Bouton Retour
                if (slot == SLOT_MANUAL_BACK) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

                    // Rendre les items avant de retourner
                    Inventory inv = event.getInventory();
                    for (int itemSlot : MANUAL_ITEM_SLOTS) {
                        ItemStack item = inv.getItem(itemSlot);
                        if (item != null && item.getType() != Material.AIR) {
                            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                            for (ItemStack leftover : overflow.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                            }
                            inv.setItem(itemSlot, null);
                        }
                    }

                    manualRecycleInventories.remove(player.getUniqueId());
                    open(player);
                    return;
                }

                // Bouton Recycler
                if (slot == SLOT_MANUAL_RECYCLE_BTN) {
                    processManualRecycle(player, event.getInventory());
                    return;
                }
            }
        }
    }

    /**
     * Traite le recyclage manuel des items dans l'inventaire
     */
    private void processManualRecycle(Player player, Inventory inv) {
        int totalPoints = 0;
        int itemsRecycled = 0;

        for (int slot : MANUAL_ITEM_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            // Vérifier si c'est un item ZombieZ
            if (!ZombieZItem.isZombieZItem(item)) {
                continue; // Ignorer les items non-ZombieZ
            }

            // Calculer les points pour chaque item du stack
            int stackSize = item.getAmount();
            int pointsForOne = recycleManager.recycleItem(player, item.asOne());

            if (pointsForOne > 0) {
                // Recycler tout le stack
                int stackPoints = pointsForOne * stackSize;

                // Ajouter les points (recycleItem ne les a ajoutés que pour 1)
                // On doit ajouter le reste
                if (stackSize > 1) {
                    PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                    if (playerData != null) {
                        playerData.addPoints(pointsForOne * (stackSize - 1));
                    }

                    RecycleSettings settings = recycleManager.getSettings(player.getUniqueId());
                    settings.addRecycledItem(pointsForOne * (stackSize - 1), stackSize - 1);
                }

                totalPoints += stackPoints;
                itemsRecycled += stackSize;
                inv.setItem(slot, null);
            }
        }

        if (itemsRecycled > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            player.sendMessage("");
            player.sendMessage("§a§l♻ RECYCLAGE EFFECTUÉ!");
            player.sendMessage("§7Items recyclés: §e" + itemsRecycled);
            player.sendMessage("§7Points gagnés: §6+" + formatPoints(totalPoints) + " pts");
            player.sendMessage("");

            // Rafraîchir le bouton avec les nouveaux stats
            PlayerData playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            int currentZone = playerData != null ? playerData.getCurrentZone().get() : 1;
            inv.setItem(53, new ItemBuilder(Material.GOLD_INGOT)
                .name("§6§l💰 Points de Recyclage")
                .lore(
                    "",
                    "§7Zone actuelle: §e" + currentZone,
                    "",
                    "§a✓ Dernier recyclage:",
                    "  §7Items: §e" + itemsRecycled,
                    "  §7Points: §6+" + formatPoints(totalPoints),
                    "",
                    "§6Points par rareté (zone " + currentZone + "):",
                    "  §f• Commun: §e" + recycleManager.calculateRecyclePoints(Rarity.COMMON, currentZone) + " pts",
                    "  §a• Peu Commun: §e" + recycleManager.calculateRecyclePoints(Rarity.UNCOMMON, currentZone) + " pts",
                    "  §9• Rare: §e" + recycleManager.calculateRecyclePoints(Rarity.RARE, currentZone) + " pts"
                )
                .build());
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            player.sendMessage("§c§l♻ §cAucun item recyclable trouvé!");
            player.sendMessage("§7Placez des items §eZombieZ §7dans les emplacements vides.");
        }
    }

    /**
     * Formate les points pour affichage (délègue au manager)
     */
    private String formatPoints(long points) {
        return RecycleManager.formatPoints(points);
    }
}
