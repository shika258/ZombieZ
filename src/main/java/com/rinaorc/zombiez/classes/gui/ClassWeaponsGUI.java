package com.rinaorc.zombiez.classes.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponTier;
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

import java.util.List;

/**
 * GUI affichant les armes exclusives de la classe du joueur
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
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        // Fond
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build());
        }

        // Header
        gui.setItem(4, new ItemBuilder(data.getSelectedClass().getIcon())
            .name("§6§lARMES - " + data.getSelectedClass().getDisplayName())
            .lore(
                "",
                "§7Armes exclusives à votre classe",
                "§7Débloquez-les via les talents!",
                "",
                "§8Ces armes ne peuvent être utilisées",
                "§8que par les " + data.getSelectedClass().getDisplayName() + "s"
            )
            .build());

        // Armes de la classe
        List<ClassWeapon> weapons = weaponRegistry.getWeaponsForClass(data.getSelectedClass());

        // Positions: Tier 1 à gauche, Tier 2 au milieu, Tier 3 à droite
        int[] positions = {20, 22, 24};

        for (int i = 0; i < weapons.size() && i < positions.length; i++) {
            ClassWeapon weapon = weapons.get(i);
            boolean isUnlocked = data.hasTalent(weapon.getUnlockTalentId());

            int zoneLevel = plugin.getPlayerDataManager().getPlayer(player).getCurrentZone().get();

            // Label de tier au-dessus
            int labelSlot = positions[i] - 9;
            gui.setItem(labelSlot, new ItemBuilder(getTierIcon(weapon.getTier()))
                .name(weapon.getTier().getColoredName())
                .lore(
                    "",
                    "§7Arme de Tier " + weapon.getTier().getLevel()
                )
                .build());

            // L'arme
            Material icon = isUnlocked ? weapon.getMaterial() : Material.BARRIER;

            gui.setItem(positions[i], new ItemBuilder(icon)
                .name(weapon.getTier().getColor() + weapon.getName() +
                    (isUnlocked ? "" : " §c✗"))
                .lore(weapon.getLore(zoneLevel, true, isUnlocked))
                .glow(isUnlocked)
                .build());

            // Requirement en-dessous
            int reqSlot = positions[i] + 9;
            if (!isUnlocked) {
                gui.setItem(reqSlot, new ItemBuilder(Material.BOOK)
                    .name("§cPrérequis")
                    .lore(
                        "",
                        "§7Talent requis:",
                        "§e" + weapon.getUnlockTalentId(),
                        "",
                        "§8Débloquez ce talent pour",
                        "§8obtenir cette arme!"
                    )
                    .build());
            } else {
                gui.setItem(reqSlot, new ItemBuilder(Material.LIME_DYE)
                    .name("§a✓ DÉBLOQUÉE")
                    .lore(
                        "",
                        "§7Cette arme peut drop",
                        "§7dans le monde!",
                        "",
                        "§8Les stats dépendent de",
                        "§8la zone où elle drop."
                    )
                    .build());
            }
        }

        // Info sur les autres classes
        gui.setItem(40, new ItemBuilder(Material.COMPARATOR)
            .name("§e§lAUTRES CLASSES")
            .lore(
                "",
                "§7Chaque classe a 3 armes uniques:",
                "",
                "§c• Commando §7- Armes lourdes",
                "§a• Éclaireur §7- Armes silencieuses",
                "§d• Médic §7- Armes médicales",
                "§e• Ingénieur §7- Armes technologiques",
                "§4• Berserker §7- Armes de mêlée",
                "§b• Sniper §7- Armes de précision",
                "",
                "§8Les armes d'autres classes",
                "§8sont inutilisables pour vous."
            )
            .build());

        // Retour
        gui.setItem(36, new ItemBuilder(Material.ARROW)
            .name("§c← Retour aux talents")
            .build());

        player.openInventory(gui);
    }

    private Material getTierIcon(WeaponTier tier) {
        return switch (tier) {
            case BASIC -> Material.IRON_INGOT;
            case ADVANCED -> Material.GOLD_INGOT;
            case LEGENDARY -> Material.NETHERITE_INGOT;
        };
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
