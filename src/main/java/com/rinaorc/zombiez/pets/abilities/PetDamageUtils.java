package com.rinaorc.zombiez.pets.abilities;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Utilitaire pour calculer les dégâts effectifs du joueur pour les pets.
 * Prend en compte les armes à distance (arc, arbalète) en plus des armes de mêlée.
 */
public class PetDamageUtils {

    /**
     * Calcule les dégâts effectifs du joueur basés sur l'arme en main.
     * - Pour les armes de mêlée: utilise ATTACK_DAMAGE + stats ZombieZ
     * - Pour les armes à distance (arc/arbalète): utilise les stats DAMAGE de l'item ZombieZ
     *
     * @param player Le joueur
     * @return Les dégâts effectifs
     */
    public static double getEffectiveDamage(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand == null || mainHand.getType() == Material.AIR) {
            // Mains nues - utiliser l'attribut de base
            return getAttributeDamage(player);
        }

        // Vérifier si c'est une arme à distance
        if (isRangedWeapon(mainHand.getType())) {
            // Pour les armes à distance, utiliser les stats ZombieZ de l'item
            return getRangedWeaponDamage(player, mainHand);
        }

        // Pour les armes de mêlée, utiliser ATTACK_DAMAGE (inclut déjà les bonus d'arme)
        return getMeleeWeaponDamage(player, mainHand);
    }

    /**
     * Vérifie si le matériau est une arme à distance
     */
    private static boolean isRangedWeapon(Material material) {
        return material == Material.BOW ||
               material == Material.CROSSBOW;
    }

    /**
     * Récupère les dégâts pour une arme à distance (arc/arbalète)
     */
    private static double getRangedWeaponDamage(Player player, ItemStack weapon) {
        // Essayer de récupérer les stats ZombieZ
        if (ZombieZItem.isZombieZItem(weapon)) {
            ZombieZPlugin plugin = ZombieZPlugin.getInstance();
            if (plugin != null && plugin.getItemManager() != null) {
                ZombieZItem zItem = plugin.getItemManager().getOrRestoreItem(weapon);
                if (zItem != null) {
                    Map<StatType, Double> stats = zItem.getTotalStats();
                    double baseDamage = stats.getOrDefault(StatType.DAMAGE, 0.0);
                    double damagePercent = stats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);

                    // Appliquer le pourcentage de bonus
                    double totalDamage = baseDamage * (1 + damagePercent / 100.0);

                    // Ajouter les bonus de stats du joueur (équipement)
                    Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
                    double playerDamageBonus = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
                    double playerDamagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);

                    totalDamage += playerDamageBonus;
                    totalDamage *= (1 + playerDamagePercent / 100.0);

                    if (totalDamage > 0) {
                        return totalDamage;
                    }
                }
            }
        }

        // Fallback: dégâts de base d'un arc vanilla (6-12 selon charge, moyenne ~9)
        return 9.0;
    }

    /**
     * Récupère les dégâts pour une arme de mêlée
     */
    private static double getMeleeWeaponDamage(Player player, ItemStack weapon) {
        double baseDamage = getAttributeDamage(player);

        // Si c'est un item ZombieZ, ajouter les bonus de stats
        if (ZombieZItem.isZombieZItem(weapon)) {
            ZombieZPlugin plugin = ZombieZPlugin.getInstance();
            if (plugin != null && plugin.getItemManager() != null) {
                ZombieZItem zItem = plugin.getItemManager().getOrRestoreItem(weapon);
                if (zItem != null) {
                    Map<StatType, Double> stats = zItem.getTotalStats();
                    double damagePercent = stats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);

                    // Appliquer le pourcentage de bonus
                    baseDamage *= (1 + damagePercent / 100.0);
                }

                // Ajouter les bonus de stats du joueur (équipement)
                Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
                double playerDamagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);

                baseDamage *= (1 + playerDamagePercent / 100.0);
            }
        }

        return baseDamage;
    }

    /**
     * Récupère l'attribut ATTACK_DAMAGE du joueur
     */
    private static double getAttributeDamage(Player player) {
        var attribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attribute != null) {
            return attribute.getValue();
        }
        return 1.0; // Dégâts à mains nues par défaut
    }
}
