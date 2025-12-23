package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
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
import java.util.List;

/**
 * Interface graphique pour configurer le recyclage automatique
 * Permet d'activer/désactiver le recyclage par rareté
 */
public class RecycleGUI implements Listener {

    private static final String GUI_TITLE = "§6§l♻ Recyclage Automatique";
    private static final int GUI_SIZE = 45; // 5 lignes

    private final ZombieZPlugin plugin;
    private final RecycleManager recycleManager;

    // Slots pour chaque élément
    private static final int SLOT_TOGGLE_MAIN = 4;        // Toggle principal
    private static final int SLOT_STATS = 40;             // Statistiques

    // Slots pour les raretés (ligne du milieu)
    private static final int[] RARITY_SLOTS = {10, 12, 14, 16, 28, 30, 32};

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
                "§7Raretés activées: §f" + settings.getEnabledRaritiesCount() + "/7"
            )
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

        // Bouton "Milestones" (slot 42)
        int unlockedCount = settings.getUnlockedMilestonesCount();
        int totalCount = settings.getTotalMilestonesCount();
        inv.setItem(42, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§l✦ Milestones")
            .lore(
                "",
                "§7Progression: §f" + unlockedCount + "/" + totalCount,
                "",
                "§7Débloquez des milestones en",
                "§7recyclant des items pour",
                "§7gagner des §6bonus de points§7!",
                "",
                "§eClic pour voir les détails"
            )
            .glow(unlockedCount > 0)
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

        // Milestones
        if (slot == 42) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
            List<String> milestones = recycleManager.getMilestonesList(player.getUniqueId());
            for (String line : milestones) {
                player.sendMessage(line);
            }
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
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        // Jouer un son de fermeture
        if (event.getPlayer() instanceof Player player) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.3f, 1.2f);
        }
    }

    /**
     * Formate les points pour affichage (délègue au manager)
     */
    private String formatPoints(long points) {
        return RecycleManager.formatPoints(points);
    }
}
