package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff.BuffCategory;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuffRegistry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI affichant les buffs arcade collectés par le joueur
 * Organisé par catégorie
 */
public class BuffsGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final ArcadeBuffRegistry buffRegistry;

    private static final String GUI_TITLE = "§0§l✦ BUFFS ARCADE ✦";

    public BuffsGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.buffRegistry = classManager.getBuffRegistry();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fond
        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Header
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lVOS BUFFS ARCADE")
            .lore(
                "",
                "§7Buffs collectés: §e" + data.getTotalBuffCount(),
                "§7Niveau de classe: §f" + data.getClassLevel().get(),
                "",
                "§8Obtenez des buffs à chaque",
                "§8level up de classe!"
            )
            .build());

        // Catégories
        Map<BuffCategory, List<Map.Entry<ArcadeBuff, Integer>>> byCategory =
            buffRegistry.getBuffsByCategory(data.getArcadeBuffs());

        // Position des catégories
        int[][] categoryPositions = {
            {10, 11, 12, 19, 20, 21}, // Offensive
            {14, 15, 16, 23, 24, 25}, // Défensive
            {28, 29, 30, 37, 38, 39}, // Utilitaire
            {32, 33, 34, 41, 42, 43}  // Hybride
        };

        BuffCategory[] categories = BuffCategory.values();
        for (int c = 0; c < categories.length; c++) {
            BuffCategory category = categories[c];
            List<Map.Entry<ArcadeBuff, Integer>> buffs = byCategory.get(category);

            // Header de catégorie
            int headerSlot = switch (c) {
                case 0 -> 1;
                case 1 -> 7;
                case 2 -> 46;
                case 3 -> 52;
                default -> 4;
            };

            gui.setItem(headerSlot, new ItemBuilder(category.getIcon())
                .name(category.getColoredName())
                .lore(
                    "",
                    "§7Buffs actifs: §f" + (buffs != null ? buffs.size() : 0)
                )
                .build());

            // Buffs de la catégorie
            if (buffs != null && !buffs.isEmpty()) {
                int[] slots = categoryPositions[c];
                int i = 0;
                for (Map.Entry<ArcadeBuff, Integer> entry : buffs) {
                    if (i >= slots.length) break;

                    ArcadeBuff buff = entry.getKey();
                    int stacks = entry.getValue();

                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(buff.getRarity().getColor() + buff.getRarity().getDisplayName());
                    lore.add("");
                    lore.add("§7" + buff.getFormattedDescription(stacks));
                    lore.add("");
                    lore.add("§aStacks: §f" + stacks + "/" + buff.getMaxStacks());
                    lore.add("§7Valeur totale: §f" + formatValue(buff, stacks));

                    if (buff.getPreferredClass() != null) {
                        lore.add("");
                        lore.add("§9Bonus pour: " + buff.getPreferredClass().getColoredName());
                    }

                    gui.setItem(slots[i], new ItemBuilder(buff.getIcon())
                        .name(buff.getRarity().getColor() + buff.getName() +
                            " §7x" + stacks)
                        .lore(lore)
                        .amount(Math.min(stacks, 64))
                        .glow(stacks >= buff.getMaxStacks())
                        .build());

                    i++;
                }
            }
        }

        // Stats totales
        gui.setItem(49, createStatsSummary(data));

        // Retour
        gui.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        player.openInventory(gui);
    }

    private String formatValue(ArcadeBuff buff, int stacks) {
        double value = buff.getValueAtStacks(stacks);
        if (buff.getEffectType().isPercent()) {
            return "+" + String.format("%.1f", value) + "%";
        }
        return "+" + String.format("%.0f", value);
    }

    private ItemStack createStatsSummary(ClassData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§6Bonus totaux des buffs:");
        lore.add("");

        // Calculer les bonus par type
        double dmgBonus = buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.DAMAGE_PERCENT);
        double hpBonus = buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.HEALTH_PERCENT);
        double critBonus = buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.CRIT_CHANCE);
        double speedBonus = buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.MOVEMENT_SPEED);
        double lifestealBonus = buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.LIFESTEAL);

        if (dmgBonus > 0) lore.add("§c⚔ Dégâts: §f+" + String.format("%.1f", dmgBonus) + "%");
        if (hpBonus > 0) lore.add("§a❤ HP: §f+" + String.format("%.1f", hpBonus) + "%");
        if (critBonus > 0) lore.add("§e✦ Critique: §f+" + String.format("%.1f", critBonus) + "%");
        if (speedBonus > 0) lore.add("§b⚡ Vitesse: §f+" + String.format("%.1f", speedBonus) + "%");
        if (lifestealBonus > 0) lore.add("§d♥ Vol de vie: §f+" + String.format("%.1f", lifestealBonus) + "%");

        if (dmgBonus == 0 && hpBonus == 0 && critBonus == 0) {
            lore.add("§8Aucun bonus actif");
        }

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§e§lRÉSUMÉ DES STATS")
            .lore(lore)
            .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getRawSlot() == 45) {
            new TalentTreeGUI(plugin, classManager).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
