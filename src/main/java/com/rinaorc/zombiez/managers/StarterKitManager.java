package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.List;

/**
 * Manager pour le kit de départ des nouveaux joueurs
 */
public class StarterKitManager {

    private final ZombieZPlugin plugin;

    public StarterKitManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Donne le kit de départ à un nouveau joueur
     */
    public void giveStarterKit(Player player) {
        // Vider l'inventaire d'abord
        player.getInventory().clear();

        // === ARME ===
        ItemStack sword = createStarterSword();
        player.getInventory().setItem(0, sword);

        // === NOURRITURE ===
        ItemStack food = createStarterFood();
        player.getInventory().setItem(1, food);

        // === TORCHES ===
        ItemStack torches = new ItemStack(Material.TORCH, 16);
        player.getInventory().setItem(2, torches);

        // === ARMURE ===
        player.getInventory().setHelmet(createArmorPiece(Material.LEATHER_HELMET, "Casque de Survivant"));
        player.getInventory().setChestplate(createArmorPiece(Material.LEATHER_CHESTPLATE, "Plastron de Survivant"));
        player.getInventory().setLeggings(createArmorPiece(Material.LEATHER_LEGGINGS, "Jambières de Survivant"));
        player.getInventory().setBoots(createArmorPiece(Material.LEATHER_BOOTS, "Bottes de Survivant"));

        // === BOUCLIER ===
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta shieldMeta = shield.getItemMeta();
        if (shieldMeta != null) {
            shieldMeta.setDisplayName("§7Bouclier de Fortune");
            shieldMeta.setLore(List.of(
                "§8Équipement de Départ",
                "",
                "§7Un bouclier rudimentaire",
                "§7pour survivre aux premiers zombies."
            ));
            shieldMeta.setUnbreakable(true);
            shieldMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            shield.setItemMeta(shieldMeta);
        }
        player.getInventory().setItemInOffHand(shield);

        // Message
        player.sendMessage("");
        player.sendMessage("§a§l✓ §7Tu as reçu ton §eKit de Survivant§7!");
        player.sendMessage("§7  ▸ §fÉpée de Bois Renforcée §7(Slot 1)");
        player.sendMessage("§7  ▸ §fPain x16 §7(Slot 2)");
        player.sendMessage("§7  ▸ §fArmure en Cuir Complète");
        player.sendMessage("§7  ▸ §fBouclier de Fortune");
        player.sendMessage("");
    }

    /**
     * Crée l'épée de départ
     */
    private ItemStack createStarterSword() {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aÉpée de Survivant");
            meta.setLore(List.of(
                "§8Équipement de Départ",
                "",
                "§7Une épée en bois renforcée,",
                "§7parfaite pour débuter.",
                "",
                "§9+2 Dégâts",
                "§9+5% Chance de Critique",
                "",
                "§8Conseil: Cherche de meilleures armes",
                "§8sur les zombies que tu tues!"
            ));
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            sword.setItemMeta(meta);
        }
        return sword;
    }

    /**
     * Crée la nourriture de départ
     */
    private ItemStack createStarterFood() {
        ItemStack food = new ItemStack(Material.BREAD, 16);
        ItemMeta meta = food.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§ePain du Refuge");
            meta.setLore(List.of(
                "§8Provisions de Départ",
                "",
                "§7Du pain frais pour",
                "§7tenir pendant l'exploration.",
                "",
                "§8Tu peux obtenir plus de nourriture",
                "§8dans les refuges ou sur les zombies."
            ));
            food.setItemMeta(meta);
        }
        return food;
    }

    /**
     * Crée une pièce d'armure de départ
     */
    private ItemStack createArmorPiece(Material material, String name) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7" + name);
            meta.setLore(List.of(
                "§8Équipement de Départ",
                "",
                "§7Armure de fortune",
                "§7pour les nouveaux survivants.",
                "",
                "§9+1 Armure"
            ));
            // Couleur marron/beige pour look survivant
            meta.setColor(Color.fromRGB(139, 90, 43));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            armor.setItemMeta(meta);
        }
        return armor;
    }

    /**
     * Vérifie si un joueur a besoin du kit de départ
     * (inventaire vide et nouveau joueur)
     */
    public boolean needsStarterKit(Player player) {
        // Vérifier si l'inventaire est vide
        boolean emptyInventory = player.getInventory().isEmpty();
        boolean noArmor = player.getInventory().getHelmet() == null &&
                          player.getInventory().getChestplate() == null &&
                          player.getInventory().getLeggings() == null &&
                          player.getInventory().getBoots() == null;

        return emptyInventory && noArmor;
    }

    /**
     * Donne le kit si nécessaire (pour les reconnexions)
     */
    public void giveStarterKitIfNeeded(Player player) {
        if (needsStarterKit(player)) {
            giveStarterKit(player);
        }
    }
}
