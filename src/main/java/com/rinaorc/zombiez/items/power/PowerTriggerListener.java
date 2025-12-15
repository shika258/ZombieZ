package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

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

    // Clés PDC pour stocker les données de pouvoir sur les projectiles
    private final NamespacedKey keyProjectileHasPower;
    private final NamespacedKey keyProjectilePowerId;
    private final NamespacedKey keyProjectileItemLevel;

    public PowerTriggerListener(ZombieZPlugin plugin, PowerManager powerManager) {
        this.plugin = plugin;
        this.powerManager = powerManager;

        this.keyHasPower = new NamespacedKey(plugin, "has_power");
        this.keyPowerId = new NamespacedKey(plugin, "power_id");
        this.keyItemLevel = new NamespacedKey(plugin, "item_level");

        // Clés pour les projectiles
        this.keyProjectileHasPower = new NamespacedKey(plugin, "proj_has_power");
        this.keyProjectilePowerId = new NamespacedKey(plugin, "proj_power_id");
        this.keyProjectileItemLevel = new NamespacedKey(plugin, "proj_item_level");
    }

    /**
     * Trigger: Quand un joueur frappe une entité (mêlée ou projectile)
     * Type de trigger le plus commun pour les pouvoirs
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Vérifier que la victime est une entité vivante
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // Vérifier si le système est activé
        if (!powerManager.isEnabled()) {
            return;
        }

        Entity damager = event.getDamager();

        // ═══════════════════════════════════════════════════════════════
        // CAS 1: Attaque directe par un joueur (mêlée)
        // ═══════════════════════════════════════════════════════════════
        if (damager instanceof Player player) {
            handleMeleeAttack(player, target);
            return;
        }

        // ═══════════════════════════════════════════════════════════════
        // CAS 2: Attaque par projectile (arc, arbalète, trident)
        // ═══════════════════════════════════════════════════════════════
        if (damager instanceof Projectile projectile) {
            handleProjectileAttack(projectile, target);
        }
    }

    /**
     * Gère les attaques au corps à corps
     */
    private void handleMeleeAttack(Player player, LivingEntity target) {
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
     * Gère les attaques par projectile (arc, arbalète, trident)
     */
    private void handleProjectileAttack(Projectile projectile, LivingEntity target) {
        // Vérifier que le tireur est un joueur
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }

        // Vérifier si le projectile a un pouvoir stocké
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();

        if (!pdc.has(keyProjectileHasPower, PersistentDataType.BYTE)) {
            return;
        }

        Byte hasPower = pdc.get(keyProjectileHasPower, PersistentDataType.BYTE);
        if (hasPower == null || hasPower != 1) {
            return;
        }

        String powerId = pdc.get(keyProjectilePowerId, PersistentDataType.STRING);
        Integer itemLevel = pdc.get(keyProjectileItemLevel, PersistentDataType.INTEGER);

        if (powerId == null) {
            return;
        }

        int finalItemLevel = itemLevel != null ? itemLevel : 1;

        // Déclencher le pouvoir
        powerManager.getPower(powerId).ifPresent(power -> {
            power.trigger(player, target, finalItemLevel);
        });
    }

    /**
     * Quand un joueur tire avec un arc/arbalète, stocker les données du pouvoir sur le projectile
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }

        // Vérifier si le système est activé
        if (!powerManager.isEnabled()) {
            return;
        }

        // L'arc/arbalète utilisé
        ItemStack bow = event.getBow();
        if (bow == null || !hasPower(bow)) {
            return;
        }

        // Transférer les données du pouvoir vers le projectile
        String powerId = getPowerId(bow);
        int itemLevel = getItemLevel(bow);

        if (powerId != null) {
            PersistentDataContainer pdc = projectile.getPersistentDataContainer();
            pdc.set(keyProjectileHasPower, PersistentDataType.BYTE, (byte) 1);
            pdc.set(keyProjectilePowerId, PersistentDataType.STRING, powerId);
            pdc.set(keyProjectileItemLevel, PersistentDataType.INTEGER, itemLevel);
        }
    }

    /**
     * Quand un trident est lancé, stocker les données du pouvoir sur le projectile
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();

        // Gérer spécifiquement les tridents
        if (!(projectile instanceof Trident trident)) {
            return;
        }

        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }

        // Vérifier si le système est activé
        if (!powerManager.isEnabled()) {
            return;
        }

        // Pour les tridents, l'item est dans la main du joueur
        ItemStack tridentItem = player.getInventory().getItemInMainHand();

        // Vérifier si c'est un trident avec pouvoir
        if (tridentItem.getType() != Material.TRIDENT || !hasPower(tridentItem)) {
            return;
        }

        // Transférer les données du pouvoir vers le projectile
        String powerId = getPowerId(tridentItem);
        int itemLevel = getItemLevel(tridentItem);

        if (powerId != null) {
            PersistentDataContainer pdc = projectile.getPersistentDataContainer();
            pdc.set(keyProjectileHasPower, PersistentDataType.BYTE, (byte) 1);
            pdc.set(keyProjectilePowerId, PersistentDataType.STRING, powerId);
            pdc.set(keyProjectileItemLevel, PersistentDataType.INTEGER, itemLevel);
        }
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
     *
     * @deprecated Utiliser applyPowerToItem(ItemStack, Power, int, int, Rarity) pour le scaling correct
     */
    @Deprecated
    public void applyPowerToItem(ItemStack item, Power power, int itemLevel) {
        applyPowerToItem(item, power, itemLevel, 1, Rarity.RARE);
    }

    /**
     * Applique un pouvoir à un item avec le scaling correct par zone et rareté
     * Utilisé lors de la génération d'items
     *
     * @param item L'item sur lequel appliquer le pouvoir
     * @param power Le pouvoir à appliquer
     * @param itemLevel L'Item Level de l'item
     * @param zoneId Zone de l'item (pour scaling des dégâts/effets)
     * @param rarity Rareté de l'item (pour bonus de proc chance)
     */
    public void applyPowerToItem(ItemStack item, Power power, int itemLevel, int zoneId, Rarity rarity) {
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

        // Ajouter l'en-tête "Pouvoir : nom"
        lore.add("");
        lore.add("§d✦ Pouvoir : §l" + power.getDisplayName());

        // Ajouter la description
        lore.add("§7" + power.getDescription());

        // Ajouter les stats du pouvoir (dégâts, durée, etc.)
        List<String> powerStats = power.getPowerStats(itemLevel, zoneId);
        for (String stat : powerStats) {
            lore.add(stat);
        }

        // Ajouter la chance et le cooldown
        lore.add("§8Chance: §e" + String.format("%.1f%%", power.calculateProcChance(zoneId, rarity) * 100));
        if (power.getCooldownMs() > 0) {
            lore.add("§8Cooldown: §e" + (power.getCooldownMs() / 1000) + "s");
        }

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
