package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon;
import com.rinaorc.zombiez.classes.weapons.ClassWeaponRegistry;
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

import java.util.List;

/**
 * GUI des armes de classe simplifié - 2 armes par classe
 * 1 arme de base (niveau 1), 1 arme légendaire (niveau 10)
 */
public class ClassWeaponsGUI implements Listener {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final ClassWeaponRegistry weaponRegistry;

    private static final String GUI_TITLE = "§0§l✦ ARMES DE CLASSE ✦";

    public ClassWeaponsGUI(ZombieZPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.weaponRegistry = classManager.getWeaponRegistry();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ClassData data = classManager.getClassData(player);
        if (!data.hasClass()) {
            player.sendMessage("§cChoisissez d'abord une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 27, GUI_TITLE);

        // Fond
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, bg);
        }

        // Header
        gui.setItem(4, new ItemBuilder(data.getSelectedClass().getIcon())
            .name(data.getSelectedClass().getColoredName() + " §7- Armes")
            .lore(
                "",
                "§7Armes exclusives à votre classe.",
                "§7Niveau de classe: §f" + data.getClassLevel().get(),
                "",
                "§8Ces armes ne peuvent être utilisées",
                "§8que par votre classe."
            )
            .build());

        // Récupérer les armes
        List<ClassWeapon> weapons = weaponRegistry.getWeaponsForClass(data.getSelectedClass());

        // Arme de base (slot 11)
        ClassWeapon baseWeapon = weapons.stream()
            .filter(w -> w.getTier() == ClassWeapon.WeaponTier.BASE)
            .findFirst().orElse(null);

        if (baseWeapon != null) {
            boolean unlocked = baseWeapon.isUnlocked(data.getClassLevel().get());
            gui.setItem(11, new ItemBuilder(unlocked ? baseWeapon.getMaterial() : Material.BARRIER)
                .name((unlocked ? "§a" : "§8") + baseWeapon.getName())
                .lore(baseWeapon.getLore(data.getClassLevel().get(), true))
                .glow(unlocked)
                .build());
        }

        // Arme légendaire (slot 15)
        ClassWeapon legendaryWeapon = weapons.stream()
            .filter(w -> w.getTier() == ClassWeapon.WeaponTier.LEGENDARY)
            .findFirst().orElse(null);

        if (legendaryWeapon != null) {
            boolean unlocked = legendaryWeapon.isUnlocked(data.getClassLevel().get());
            gui.setItem(15, new ItemBuilder(unlocked ? legendaryWeapon.getMaterial() : Material.BARRIER)
                .name((unlocked ? "§c§l" : "§8") + legendaryWeapon.getName())
                .lore(legendaryWeapon.getLore(data.getClassLevel().get(), true))
                .glow(unlocked)
                .build());
        }

        // Info centrale
        gui.setItem(13, new ItemBuilder(Material.BOOK)
            .name("§eComment obtenir les armes?")
            .lore(
                "",
                "§a● Arme de Base",
                "§7  Disponible au niveau 1",
                "",
                "§c● Arme Légendaire",
                "§7  Disponible au niveau 10",
                "",
                "§8Les armes drop des boss et",
                "§8zombies élites en jeu."
            )
            .build());

        // Retour
        gui.setItem(22, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        if (event.getRawSlot() == 22) {
            new TalentTreeGUI(plugin, classManager).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
