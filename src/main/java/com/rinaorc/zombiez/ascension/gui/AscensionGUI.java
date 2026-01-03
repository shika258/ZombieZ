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
 * GUI de sélection des mutations
 */
public class AscensionGUI implements Listener {

    private static final String GUI_TITLE = "§0§l✦ ASCENSION ✦";

    private final ZombieZPlugin plugin;
    private final Player player;
    private final AscensionData data;
    private final Inventory inventory;

    // Slots des mutations
    private static final int SLOT_MUTATION_1 = 11;
    private static final int SLOT_MUTATION_2 = 13;
    private static final int SLOT_MUTATION_3 = 15;
    private static final int SLOT_INFO = 4;
    private static final int SLOT_LIST = 22;

    private AscensionGUI(ZombieZPlugin plugin, Player player, AscensionData data) {
        this.plugin = plugin;
        this.player = player;
        this.data = data;
        this.inventory = Bukkit.createInventory(null, 27, GUI_TITLE);

        buildInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ouvre le GUI pour un joueur
     */
    public static void open(ZombieZPlugin plugin, Player player, AscensionData data) {
        new AscensionGUI(plugin, player, data).show();
    }

    private void show() {
        player.openInventory(inventory);
    }

    private void buildInventory() {
        // Bordure
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        // Info du stade
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Si un choix est en attente, afficher les mutations
        if (data.isChoicePending() && data.getPendingChoices() != null) {
            List<Mutation> choices = data.getPendingChoices();
            if (choices.size() >= 1) inventory.setItem(SLOT_MUTATION_1, createMutationItem(choices.get(0)));
            if (choices.size() >= 2) inventory.setItem(SLOT_MUTATION_2, createMutationItem(choices.get(1)));
            if (choices.size() >= 3) inventory.setItem(SLOT_MUTATION_3, createMutationItem(choices.get(2)));
        } else {
            // Pas de choix en attente, afficher la progression
            inventory.setItem(SLOT_MUTATION_1, createProgressItem());
            inventory.setItem(SLOT_MUTATION_2, createEmptySlot());
            inventory.setItem(SLOT_MUTATION_3, createEmptySlot());
        }

        // Bouton liste des mutations
        inventory.setItem(SLOT_LIST, createListButton());
    }

    private ItemStack createInfoItem() {
        int stage = data.getCurrentStage();
        int kills = data.getSessionKills().get();
        int nextKills = data.getKillsForNextStage();

        Material material = stage >= 10 ? Material.NETHER_STAR : Material.EXPERIENCE_BOTTLE;
        String title = stage >= 10 ? "§6§l⬆ ASCENSION MAX" : "§6§l⬆ STADE " + toRoman(stage);

        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        if (data.isChoicePending()) {
            lore.add("§a§l▶ CHOIX DISPONIBLE!");
            lore.add("§7Temps restant: §e" + data.getChoiceTimeRemaining() + "s");
            lore.add("");
            lore.add("§7Clique sur une mutation pour la choisir.");
        } else if (stage >= 10) {
            lore.add("§eTu as atteint le stade maximum!");
            lore.add("");
            lore.add("§7Mutations: §f" + data.getActiveMutations().size() + "/10");
        } else {
            lore.add("§7Progression: §f" + kills + "§8/§f" + nextKills + " kills");
            lore.add("");
            int progress = (int) ((double) kills / nextKills * 20);
            StringBuilder bar = new StringBuilder("§8[");
            for (int i = 0; i < 20; i++) {
                bar.append(i < progress ? "§a█" : "§7░");
            }
            bar.append("§8]");
            lore.add(bar.toString());
        }

        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        // Souches collectées
        if (!data.getActiveMutations().isEmpty()) {
            lore.add("");
            lore.add("§7Souches:");
            if (data.getStrainCarnageCount() > 0)
                lore.add("  §c💀 Carnage: §f" + data.getStrainCarnageCount());
            if (data.getStrainSpectreCount() > 0)
                lore.add("  §b👻 Spectre: §f" + data.getStrainSpectreCount());
            if (data.getStrainButinCount() > 0)
                lore.add("  §e💎 Butin: §f" + data.getStrainButinCount());
        }

        return createItem(material, title, lore);
    }

    private ItemStack createMutationItem(Mutation mutation) {
        MutationStrain strain = mutation.getStrain();
        Material material = strain.getMaterial();

        String title = strain.getColor() + "§l" + strain.getIcon() + " " + mutation.getDisplayName();

        List<String> lore = new ArrayList<>();
        lore.add("§8" + strain.getDisplayName() + " - " + mutation.getTier().getDisplayName());
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");
        lore.add("§7" + mutation.getDescription());
        lore.add("");

        // Afficher les bonus de stats
        if (mutation.hasStatBonuses()) {
            lore.add("§a§lEffets:");
            for (Map.Entry<StatType, Double> entry : mutation.getStatBonuses().entrySet()) {
                StatType stat = entry.getKey();
                double value = entry.getValue();
                String sign = value >= 0 ? "§a+" : "§c";
                lore.add("  " + sign + stat.formatValue(value) + " §7" + stat.getDisplayName());
            }
        } else {
            lore.add("§a§lEffet Spécial:");
            lore.add("  §7" + getEffectDescription(mutation));
        }

        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("§e▶ Clic pour choisir");

        ItemStack item = createItem(material, title, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createProgressItem() {
        int kills = data.getSessionKills().get();
        int nextKills = data.getKillsForNextStage();

        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");
        lore.add("§7Continue à tuer des zombies");
        lore.add("§7pour débloquer ton prochain choix!");
        lore.add("");
        lore.add("§7Kills: §f" + kills + "§8/§f" + nextKills);
        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        return createItem(Material.CLOCK, "§7§lProchaine mutation...", lore);
    }

    private ItemStack createEmptySlot() {
        return createItem(Material.BLACK_STAINED_GLASS_PANE, "§8...");
    }

    private ItemStack createListButton() {
        List<String> lore = new ArrayList<>();
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");

        if (data.getActiveMutations().isEmpty()) {
            lore.add("");
            lore.add("§7Aucune mutation active");
            lore.add("");
        } else {
            lore.add("");
            for (Mutation m : data.getActiveMutations()) {
                lore.add(m.getStrain().getColor() + m.getStrain().getIcon() + " §7" + m.getDisplayName());
            }
            lore.add("");
        }

        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("§e▶ Clic pour plus de détails");

        return createItem(Material.BOOK, "§6§l📜 Mutations Actives §8(" + data.getActiveMutations().size() + ")", lore);
    }

    private String getEffectDescription(Mutation mutation) {
        return switch (mutation.getEffect()) {
            case CONDITIONAL_DAMAGE -> "+20% Dégâts quand < 50% HP";
            case ON_KILL_EXPLOSION -> "15% chance d'explosion au kill";
            case STACKING_LIFESTEAL -> "+5% Lifesteal/kill (max 25%)";
            case NTH_HIT_BONUS -> "Chaque 5ème coup = +50% dégâts";
            case CRIT_BLEED -> "Crits = saignement 3s";
            case LOW_HP_BOOST -> "< 20% HP: +40% Dmg, +20% Speed";
            case KILL_STACK_DAMAGE -> "Kill = +25% prochain hit (stack 3x)";
            case DAMAGE_AURA -> "Mobs proches: 1% HP/s";
            case EXECUTE -> "Instakill sous 10% HP (CD 3s)";
            case HEAL_ON_KILL -> "Kill = +2% HP (cap 5%/s)";
            case KILL_COUNTER_EXPLOSION -> "Explosion AoE tous les 25 kills";
            case CRIT_ARMOR_PEN -> "Crits ignorent 25% armure";
            case REVENGE_DAMAGE -> "+15% aux ennemis qui t'ont touché";
            case KILL_AGGRO_REDUCE -> "Kill = aggro réduit 3s";
            case STACKING_SPEED -> "+3% Speed/kill (max 30%)";
            case DOUBLE_HIT -> "12% chance de double frappe";
            case FIRST_HIT_BONUS -> "Premier hit +30% dégâts";
            case DAMAGE_TELEPORT -> "10% chance TP quand touché";
            case STACKING_CRIT -> "+2% Crit/kill (max 20%)";
            case CRIT_CHAIN -> "20% crit chain à 1 ennemi";
            case LOW_HP_DEFENSE -> "< 30% HP: +25% Esquive/Speed";
            case BACKSTAB_CRIT -> "Attaques dans le dos = crit";
            case PHASE_THROUGH -> "Sprint = traverse les mobs";
            case BONUS_POINTS_CHANCE -> "5% kill = +25 points bonus";
            case ELITE_LOOT_BONUS -> "Élites: +30% drop rare+";
            case KILL_MILESTONE_POINTS -> "+50 points tous les 10 kills";
            case LOOT_TIER_UP -> "10% rare+ → tier supérieur";
            case JACKPOT_POINTS -> "3% kill = +100 points";
            case COMBO_LOOT_BONUS -> "Combo 10+ = +50% loot";
            case ZONE_LOOT_AURA -> "+10% loot dans ta zone";
            case GUARANTEED_RARE_CHANCE -> "1% kill = item Rare+ garanti";
            case RECYCLE_BONUS -> "+50% valeur recyclage";
            case GUARANTEED_RARE_MILESTONE -> "1 Rare+ garanti tous les 50 kills";
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

        int slot = event.getRawSlot();

        // Clic sur une mutation
        if (data.isChoicePending() && data.getPendingChoices() != null) {
            List<Mutation> choices = data.getPendingChoices();
            Mutation selected = null;

            if (slot == SLOT_MUTATION_1 && choices.size() >= 1) selected = choices.get(0);
            else if (slot == SLOT_MUTATION_2 && choices.size() >= 2) selected = choices.get(1);
            else if (slot == SLOT_MUTATION_3 && choices.size() >= 3) selected = choices.get(2);

            if (selected != null) {
                player.closeInventory();
                plugin.getAscensionManager().selectMutation(player, selected, false);
                return;
            }
        }

        // Clic sur le bouton liste
        if (slot == SLOT_LIST) {
            player.closeInventory();
            MutationListGUI.open(plugin, player, data);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        if (!event.getPlayer().equals(player)) return;

        HandlerList.unregisterAll(this);
    }

    // ==================== UTILITAIRES ====================

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }

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

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }
}
