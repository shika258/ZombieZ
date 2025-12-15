package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener pour les déclenchements de pouvoirs
 *
 * Détecte les événements qui peuvent déclencher un pouvoir (hit, kill, etc.)
 * et active le pouvoir correspondant si l'item en possède un.
 */
public class PowerTriggerListener implements Listener {

    private final ZombieZPlugin plugin;
    private final PowerManager powerManager;

    // Clés PDC pour stocker les données de pouvoir sur les items
    private final NamespacedKey keyHasPower;
    private final NamespacedKey keyPowerId;
    private final NamespacedKey keyItemLevel;

    public PowerTriggerListener(ZombieZPlugin plugin, PowerManager powerManager) {
        this.plugin = plugin;
        this.powerManager = powerManager;

        this.keyHasPower = new NamespacedKey(plugin, "has_power");
        this.keyPowerId = new NamespacedKey(plugin, "power_id");
        this.keyItemLevel = new NamespacedKey(plugin, "item_level");
    }

    /**
     * Trigger: Quand un joueur frappe une entité
     * Type de trigger le plus commun pour les pouvoirs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Vérifier que c'est un joueur qui attaque
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Vérifier que la victime est une entité vivante
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // Vérifier si le système est activé
        if (!powerManager.isEnabled()) {
            return;
        }

        // Obtenir l'item en main
        ItemStack weapon = player.getInventory().getItemInMainHand();

        // Vérifier si l'item a un pouvoir
        if (!hasPower(weapon)) {
            return;
        }

        // Obtenir les données du pouvoir
        String powerId = getPowerId(weapon);
        int itemLevel = getItemLevel(weapon);

        if (powerId == null) {
            return;
        }

        // Obtenir le pouvoir
        powerManager.getPower(powerId).ifPresent(power -> {
            // Déclencher le pouvoir
            power.trigger(player, target, itemLevel);
        });
    }

    /**
     * Trigger: Quand un joueur tue une entité
     * Peut être utilisé pour des pouvoirs spécifiques "on kill"
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        // Vérifier que c'est un joueur qui a tué
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        // Vérifier si le système est activé
        if (!powerManager.isEnabled()) {
            return;
        }

        // Obtenir l'item en main
        ItemStack weapon = killer.getInventory().getItemInMainHand();

        // Vérifier si l'item a un pouvoir
        if (!hasPower(weapon)) {
            return;
        }

        // Pour l'instant, on ne gère que les triggers "on hit"
        // Mais on peut étendre ce système pour des pouvoirs "on kill" dans le futur
        // en ajoutant un type de trigger dans la classe Power
    }

    /**
     * Vérifie si un item a un pouvoir
     */
    public boolean hasPower(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keyHasPower, PersistentDataType.BYTE) &&
               pdc.get(keyHasPower, PersistentDataType.BYTE) == 1;
    }

    /**
     * Obtient l'ID du pouvoir d'un item
     */
    public String getPowerId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(keyPowerId, PersistentDataType.STRING);
    }

    /**
     * Obtient l'Item Level d'un item
     */
    public int getItemLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer ilvl = pdc.get(keyItemLevel, PersistentDataType.INTEGER);
        return ilvl != null ? ilvl : 1;
    }

    /**
     * Applique un pouvoir à un item
     * Utilisé lors de la génération d'items
     */
    public void applyPowerToItem(ItemStack item, Power power, int itemLevel) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        var meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Stocker les données du pouvoir
        pdc.set(keyHasPower, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyPowerId, PersistentDataType.STRING, power.getId());
        pdc.set(keyItemLevel, PersistentDataType.INTEGER, itemLevel);

        // Ajouter le lore du pouvoir
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        // Ajouter un séparateur avant le pouvoir
        lore.add("");
        lore.add("§8§m                    ");

        // Ajouter les lignes du pouvoir
        lore.addAll(power.getLore(itemLevel));

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Obtient les clés PDC utilisées par ce listener
     */
    public NamespacedKey getKeyHasPower() {
        return keyHasPower;
    }

    public NamespacedKey getKeyPowerId() {
        return keyPowerId;
    }

    public NamespacedKey getKeyItemLevel() {
        return keyItemLevel;
    }
}
