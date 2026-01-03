package com.rinaorc.zombiez.ascension.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionData;
import com.rinaorc.zombiez.ascension.Mutation;
import com.rinaorc.zombiez.ascension.MutationStrain;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI affichant la liste des mutations actives
 */
public class MutationListGUI implements Listener {

    private static final String GUI_TITLE = "§0§l📜 Mutations Actives";

    private final ZombieZPlugin plugin;
    private final Player player;
    private final AscensionData data;
    private final Inventory inventory;

    private static final int SLOT_BACK = 49;

    private MutationListGUI(ZombieZPlugin plugin, Player player, AscensionData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(null, 54, GUI_TITLE);

        buildInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void open(ZombieZPlugin plugin, Player player, AscensionData data) {
        new MutationListGUI(plugin, player, data).show();
    }

    private void show() {
        player.openInventory(inventory);
    }

    private void buildInventory() {
        // Bordure du bas
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Afficher les mutations par souche
        List<Mutation> mutations = data.getActiveMutations();

        // Slots pour les 3 souches (rangées)
        int carnageStart = 0;
        int spectreStart = 18;
        int butinStart = 36;

        // Compteurs par souche
        int carnageSlot = carnageStart;
        int spectreSlot = spectreStart;
        int butinSlot = butinStart;

        for (Mutation mutation : mutations) {
            ItemStack item = createMutationItem(mutation);

            switch (mutation.getStrain()) {
                case CARNAGE -> {
                    if (carnageSlot < carnageStart + 9) {
                        inventory.setItem(carnageSlot++, item);
                    }
                }
                case SPECTRE -> {
                    if (spectreSlot < spectreStart + 9) {
                        inventory.setItem(spectreSlot++, item);
                    }
                }
                case BUTIN -> {
                    if (butinSlot < butinStart + 9) {
                        inventory.setItem(butinSlot++, item);
                    }
                }
            }
        }

        // Labels des souches (colonne de droite)
        inventory.setItem(8, createStrainLabel(MutationStrain.CARNAGE, data.getStrainCarnageCount()));
        inventory.setItem(26, createStrainLabel(MutationStrain.SPECTRE, data.getStrainSpectreCount()));
        inventory.setItem(44, createStrainLabel(MutationStrain.BUTIN, data.getStrainButinCount()));

        // Bonus actifs
        inventory.setItem(45, createBonusItem());

        // Bouton retour
        inventory.setItem(SLOT_BACK, createBackButton());
    }

    private ItemStack createMutationItem(Mutation mutation) {
        MutationStrain strain = mutation.getStrain();
        Material material = strain.getMaterial();

        String title = strain.getColor() + strain.getIcon() + " " + mutation.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add("§8" + mutation.getTier().getStars());
        lore.add("");
        lore.add("§7" + mutation.getDescription());
        lore.add("");

        if (mutation.hasStatBonuses()) {
            for (Map.Entry<StatType, Double> entry : mutation.getStatBonuses().entrySet()) {
                StatType stat = entry.getKey();
                double value = entry.getValue();
                String sign = value >= 0 ? "§a+" : "§c";
                lore.add(sign + stat.formatValue(value) + " §7" + stat.getDisplayName());
            }
        } else {
            lore.add("§d" + getShortEffectDescription(mutation));
        }

        ItemStack item = createItem(material, title, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStrainLabel(MutationStrain strain, int count) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Mutations: §f" + count);

        if (count >= 3) {
            int bonus = count >= 7 ? 15 : count >= 5 ? 10 : 5;
            String bonusType = switch (strain) {
                case CARNAGE -> "Dégâts";
                case SPECTRE -> "Vitesse";
                case BUTIN -> "Chance";
            };
            lore.add("");
            lore.add("§aBonus actif: §f+" + bonus + "% " + bonusType);
        } else {
            lore.add("");
            lore.add("§8Bonus à 3 mutations");
        }

        return createItem(strain.getMaterial(),
            strain.getColor() + "§l" + strain.getIcon() + " " + strain.getDisplayName().toUpperCase(),
            lore);
    }

    private ItemStack createBonusItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        Map<StatType, Double> bonuses = data.getCachedStatBonuses();
        if (bonuses.isEmpty()) {
            lore.add("");
            lore.add("§7Aucun bonus actif");
            lore.add("");
        } else {
            lore.add("");
            for (Map.Entry<StatType, Double> entry : bonuses.entrySet()) {
                StatType stat = entry.getKey();
                double value = entry.getValue();
                String sign = value >= 0 ? "§a+" : "§c";
                lore.add(sign + stat.formatValue(value) + " §7" + stat.getDisplayName());
            }
            lore.add("");
        }

        // Bonus équilibriste
        int c = data.getStrainCarnageCount();
        int s = data.getStrainSpectreCount();
        int b = data.getStrainButinCount();
        if (c >= 2 && s >= 2 && b >= 2) {
            int equiBonus = (c >= 3 && s >= 3 && b >= 3) ? 8 : 5;
            lore.add("§d§lÉquilibriste: §f+" + equiBonus + "% §7à toutes les stats");
            lore.add("");
        }

        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        return createItem(Material.BEACON, "§e§l✦ Bonus Totaux", lore);
    }

    private ItemStack createBackButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Retourner au menu principal");
        return createItem(Material.ARROW, "§c§l← Retour", lore);
    }

    private String getShortEffectDescription(Mutation mutation) {
        return switch (mutation.getEffect()) {
            case CONDITIONAL_DAMAGE -> "+20% Dmg < 50% HP";
            case ON_KILL_EXPLOSION -> "15% explosion kill";
            case STACKING_LIFESTEAL -> "+5% LS/kill";
            case NTH_HIT_BONUS -> "5ème hit +50%";
            case CRIT_BLEED -> "Crit = bleed";
            case LOW_HP_BOOST -> "< 20% HP boost";
            case KILL_STACK_DAMAGE -> "+25%/kill stack";
            case DAMAGE_AURA -> "1% HP/s aura";
            case EXECUTE -> "Instakill < 15%";
            case HEAL_ON_KILL -> "+2% HP/kill";
            case KILL_COUNTER_EXPLOSION -> "Nova 25 kills";
            case CRIT_ARMOR_PEN -> "Crit -25% armor";
            case REVENGE_DAMAGE -> "+15% revenge";
            case KILL_AGGRO_REDUCE -> "Aggro réduit";
            case STACKING_SPEED -> "+3% speed/kill";
            case DOUBLE_HIT -> "12% double hit";
            case FIRST_HIT_BONUS -> "Premier +30%";
            case DAMAGE_TELEPORT -> "10% TP défensif";
            case STACKING_CRIT -> "+2% crit/kill";
            case CRIT_CHAIN -> "20% crit chain";
            case LOW_HP_DEFENSE -> "< 30% HP défense";
            case BACKSTAB_CRIT -> "Dos = crit";
            case PHASE_THROUGH -> "Traverse mobs";
            case BONUS_POINTS_CHANCE -> "5% +25 pts";
            case ELITE_LOOT_BONUS -> "Elite +30%";
            case KILL_MILESTONE_POINTS -> "+50 pts/10 kills";
            case LOOT_TIER_UP -> "10% tier up";
            case JACKPOT_POINTS -> "3% +100 pts";
            case COMBO_LOOT_BONUS -> "Combo +50% loot";
            case ZONE_LOOT_AURA -> "+10% zone loot";
            case GUARANTEED_RARE_CHANCE -> "1% rare+";
            case RECYCLE_BONUS -> "+50% recycle";
            case GUARANTEED_RARE_MILESTONE -> "50 kills = rare+";
            default -> "Effet spécial";
        };
    }

    // ==================== EVENTS ====================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!clicker.equals(player)) return;

        event.setCancelled(true);

        if (event.getRawSlot() == SLOT_BACK) {
            player.closeInventory();
            plugin.getAscensionManager().openAscensionGUI(player);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!event.getPlayer().equals(player)) return;

        HandlerList.unregisterAll(this);
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
