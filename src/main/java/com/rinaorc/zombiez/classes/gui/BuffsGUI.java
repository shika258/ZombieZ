package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff;
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
 * GUI des buffs arcade simplifié
 * Affiche tous les buffs collectés avec leurs stacks
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
            player.sendMessage("§cChoisissez d'abord une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        // Fond
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, bg);
        }

        // Header
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lVOS BUFFS")
            .lore(
                "",
                "§7Buffs collectés: §e" + data.getTotalBuffCount(),
                "§7Niveau de classe: §f" + data.getClassLevel().get(),
                "",
                "§8Obtenez des buffs à chaque",
                "§8niveau de classe!"
            )
            .build());

        // Afficher les buffs collectés
        Map<String, Integer> buffs = data.getArcadeBuffs();
        int slot = 10;

        for (Map.Entry<String, Integer> entry : buffs.entrySet()) {
            if (slot >= 35) break;
            if (slot == 17 || slot == 26) slot++; // Skip edges

            ArcadeBuff buff = buffRegistry.getBuff(entry.getKey());
            if (buff == null) continue;

            int stacks = entry.getValue();

            gui.setItem(slot, new ItemBuilder(buff.getIcon())
                .name(buff.getRarity().getColor() + buff.getName() + " §7x" + stacks)
                .lore(buff.getLore(stacks))
                .amount(Math.min(stacks, 64))
                .glow(stacks >= buff.getMaxStacks())
                .build());

            slot++;
        }

        if (buffs.isEmpty()) {
            gui.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§cAucun buff")
                .lore("", "§7Gagnez des niveaux pour", "§7obtenir des buffs!")
                .build());
        }

        // Stats résumées
        gui.setItem(40, createStatsSummary(data));

        // Retour
        gui.setItem(36, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        player.openInventory(gui);
    }

    private ItemStack createStatsSummary(ClassData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§6Bonus totaux:");
        lore.add("");

        double dmg = buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DAMAGE);
        double hp = buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.HEALTH);
        double crit = buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.CRIT_CHANCE);
        double speed = buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.SPEED);

        if (dmg > 0) lore.add("§c⚔ Dégâts: §f+" + String.format("%.0f", dmg) + "%");
        if (hp > 0) lore.add("§a❤ HP: §f+" + String.format("%.0f", hp) + "%");
        if (crit > 0) lore.add("§e✦ Critique: §f+" + String.format("%.0f", crit) + "%");
        if (speed > 0) lore.add("§b⚡ Vitesse: §f+" + String.format("%.0f", speed) + "%");

        if (dmg == 0 && hp == 0 && crit == 0 && speed == 0) {
            lore.add("§8Aucun bonus actif");
        }

        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§e§lSTATS TOTALES")
            .lore(lore)
            .build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getRawSlot() == 36) {
            new TalentTreeGUI(plugin, classManager).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
