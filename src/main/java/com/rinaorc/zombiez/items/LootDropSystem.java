package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.ItemGenerator;
import com.rinaorc.zombiez.items.types.Rarity;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Système de drop de loot
 */
public class LootDropSystem {

    private final ZombieZPlugin plugin;
    private final ItemGenerator generator;

    public LootDropSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.generator = ItemGenerator.getInstance();
    }

    /**
     * Drop un item de loot à une location
     */
    public void dropLoot(Location location, int zoneId, double luckBonus) {
        ZombieZItem item = generator.generate(zoneId, luckBonus);
        plugin.getItemManager().dropItem(location, item);
    }

    /**
     * Donne un loot directement à un joueur
     */
    public ZombieZItem giveLoot(Player player, int zoneId, double luckBonus) {
        return plugin.getItemManager().giveItem(player, zoneId, luckBonus);
    }

    /**
     * Calcule la chance de drop basée sur la configuration
     */
    public double getDropChance(int zoneId, String mobType) {
        FileConfiguration config = plugin.getConfigManager().getLootConfig();
        if (config == null) return 0.1;
        
        double baseChance = config.getDouble("drop-chances.base", 0.1);
        double zoneBonus = config.getDouble("drop-chances.zone-bonus", 0.02) * zoneId;
        
        return Math.min(1.0, baseChance + zoneBonus);
    }

    /**
     * Obtient la rareté minimum pour une zone
     */
    public Rarity getMinRarity(int zoneId) {
        if (zoneId >= 9) return Rarity.RARE;
        if (zoneId >= 6) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }
    public void openCrateGUI(Player player, Rarity rarity) {
        // TODO: Implémenter la vraie logique de GUI de crate
        // Pour l'instant, on donne juste 3 items aléatoires comme si la crate s'ouvrait

        player.sendMessage("§eOuverture de la Crate " + rarity.getDisplayName() + "...");
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);

        // Simuler le loot (Donne 3 items du même tier que la crate)
        // Utilise le générateur via le plugin
        ZombieZPlugin plugin = ZombieZPlugin.getInstance();

        // Note: Assurez-vous d'avoir accès à l'instance du plugin ou passez-la au constructeur
        // Si vous n'avez pas de singleton, utilisez 'this.plugin' si disponible dans LootDropSystem

        if (plugin != null) {
            List<ZombieZItem> loots = plugin.getItemManager().getGenerator()
                .generateMultiple(1, 3, rarity.ordinal() * 0.5); // Bonus de luck basé sur la rareté de la crate

            for (ZombieZItem loot : loots) {
                plugin.getItemManager().giveItem(player, loot);
            }
        } else {
            player.sendMessage("§cErreur: Impossible d'accéder au générateur d'items.");
        }
    }
}
