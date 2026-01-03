package com.rinaorc.zombiez.ascension.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionData;
import com.rinaorc.zombiez.ascension.AscensionManager;
import com.rinaorc.zombiez.ascension.Mutation;
import com.rinaorc.zombiez.ascension.MutationStrain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI d'assurance après la mort
 * Permet de conserver UNE mutation en payant des gemmes
 */
public class InsuranceGUI implements Listener {

    private static final String GUI_TITLE = "§0§l☠ ASSURANCE - Choisis une mutation";

    private final ZombieZPlugin plugin;
    private final Player player;
    private final AscensionData data;
    private final Inventory inventory;
    private final List<Mutation> mutations;

    private BukkitTask closeTask;
    private boolean closed = false;

    private static final int SLOT_DECLINE = 49;

    private InsuranceGUI(ZombieZPlugin plugin, Player player, AscensionData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.mutations = new ArrayList<>(data.getActiveMutations());
        this.inventory = Bukkit.createInventory(null, 54, GUI_TITLE);

        buildInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Fermer automatiquement après 15 secondes
        closeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!closed && player.isOnline()) {
                player.closeInventory();
                declineInsurance();
            }
        }, 300L); // 15 secondes
    }

    public static void open(ZombieZPlugin plugin, Player player, AscensionData data) {
        new InsuranceGUI(plugin, player, data).show();
    }

    private void show() {
        player.openInventory(inventory);
    }

    private void buildInventory() {
        // Bordure
        ItemStack border = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Info en haut
        inventory.setItem(4, createInfoItem());

        // Mutations (jusqu'à 10, rangées en 2 lignes)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int slotIndex = 0;

        for (Mutation mutation : mutations) {
            if (slotIndex >= slots.length) break;
            inventory.setItem(slots[slotIndex++], createMutationItem(mutation));
        }

        // Balance de gemmes du joueur
        inventory.setItem(40, createGemsBalanceItem());

        // Bouton refuser
        inventory.setItem(SLOT_DECLINE, createDeclineButton());
    }

    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");
        lore.add("§7Tu es mort avec §f" + mutations.size() + " §7mutations.");
        lore.add("");
        lore.add("§7Tu peux en §aconserver UNE§7 en payant");
        lore.add("§7des §e💎 Gemmes§7.");
        lore.add("");
        lore.add("§cSi tu ne choisis pas, tout sera perdu !");
        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("§7⏱ Fermeture auto dans §c15s");

        return createItem(Material.SKELETON_SKULL, "§c§l☠ TU ES MORT", lore);
    }

    private ItemStack createMutationItem(Mutation mutation) {
        MutationStrain strain = mutation.getStrain();
        Material material = strain.getMaterial();
        int cost = mutation.getInsuranceCost();

        boolean canAfford = plugin.getEconomyManager().hasGems(player, cost);

        String title = (canAfford ? "" : "§c§m") + strain.getColor() + strain.getIcon() + " " + mutation.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add("§8" + mutation.getTier().getStars());
        lore.add("");
        lore.add("§7" + mutation.getDescription());
        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        if (canAfford) {
            lore.add("§ePrix: §f" + cost + " 💎");
            lore.add("");
            lore.add("§a▶ Clic pour assurer");
        } else {
            lore.add("§cPrix: §f" + cost + " 💎 §c(insuffisant)");
            lore.add("");
            lore.add("§c✕ Pas assez de gemmes");
        }

        ItemStack item = createItem(material, title, lore);

        if (canAfford) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private ItemStack createGemsBalanceItem() {
        int gems = plugin.getEconomyManager().getGems(player);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Tu possèdes: §e" + gems + " 💎");
        lore.add("");

        return createItem(Material.DIAMOND, "§e§l💎 Tes Gemmes", lore);
    }

    private ItemStack createDeclineButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Toutes tes mutations seront perdues.");
        lore.add("");
        lore.add("§c▶ Clic pour confirmer");

        return createItem(Material.BARRIER, "§c§l✕ TOUT PERDRE", lore);
    }

    private void declineInsurance() {
        closed = true;
        if (closeTask != null) {
            closeTask.cancel();
        }

        AscensionManager manager = plugin.getAscensionManager();
        if (manager != null) {
            manager.resetPlayer(player);
        }

        player.sendMessage("§8[§6Ascension§8] §cToutes tes mutations ont été perdues.");
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
    }

    private void selectInsurance(Mutation mutation) {
        closed = true;
        if (closeTask != null) {
            closeTask.cancel();
        }

        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Payer et assurer
        if (manager.insureMutation(player, mutation)) {
            // Reset avec la mutation assurée
            manager.resetPlayer(player);

            player.sendMessage("§8[§6Ascension§8] §a" + mutation.getFormattedName() + " §7a été conservée !");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        } else {
            player.sendMessage("§8[§6Ascension§8] §cÉchec de l'assurance.");
            declineInsurance();
        }
    }

    // ==================== EVENTS ====================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.equals(player)) return;

        event.setCancelled(true);
        if (closed) return;

        int slot = event.getRawSlot();

        // Refuser
        if (slot == SLOT_DECLINE) {
            player.closeInventory();
            declineInsurance();
            return;
        }

        // Mutations (slots 10-16, 19-25)
        int[] mutationSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < mutationSlots.length && i < mutations.size(); i++) {
            if (slot == mutationSlots[i]) {
                Mutation mutation = mutations.get(i);
                int cost = mutation.getInsuranceCost();

                if (plugin.getEconomyManager().hasGems(player, cost)) {
                    player.closeInventory();
                    selectInsurance(mutation);
                } else {
                    player.sendMessage("§cPas assez de gemmes ! Il te faut " + cost + " 💎");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!event.getPlayer().equals(player)) return;

        HandlerList.unregisterAll(this);

        // Si fermé sans choix, tout perdre
        if (!closed) {
            Bukkit.getScheduler().runTask(plugin, this::declineInsurance);
        }
    }

    // ==================== UTILITAIRES ====================

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }
}
