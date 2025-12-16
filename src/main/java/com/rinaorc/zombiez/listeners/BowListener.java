package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour le système de tir amélioré des arcs et arbalètes
 *
 * Fonctionnalités:
 * - Tir instantané sans avoir besoin de charger
 * - Pas besoin de flèches pour tirer
 * - Cooldown basé sur la stat DRAW_SPEED
 * - Intégration avec le système de stats ZombieZ
 */
public class BowListener implements Listener {

    private final ZombieZPlugin plugin;

    // Cooldowns par joueur (UUID -> timestamp de dernier tir)
    private final Map<UUID, Long> bowCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> crossbowCooldowns = new ConcurrentHashMap<>();

    // Cooldowns de base en millisecondes
    private static final long BASE_BOW_COOLDOWN = 500;       // 0.5 seconde pour l'arc
    private static final long BASE_CROSSBOW_COOLDOWN = 800;  // 0.8 seconde pour l'arbalète

    // Clé PDC pour marquer les projectiles ZombieZ
    private final NamespacedKey zombiezProjectileKey;

    public BowListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.zombiezProjectileKey = new NamespacedKey(plugin, "zombiez_projectile");
    }

    /**
     * Gère le clic droit pour le tir instantané
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Vérifier que c'est un clic droit
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null) return;

        Material type = item.getType();

        // Gérer les arcs
        if (type == Material.BOW) {
            handleBowShoot(player, item, event);
        }
        // Gérer les arbalètes
        else if (type == Material.CROSSBOW) {
            handleCrossbowShoot(player, item, event);
        }
    }

    /**
     * Annule le comportement vanilla des arcs pour empêcher le chargement
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Annuler le tir vanilla - on gère tout nous-mêmes
        event.setCancelled(true);
    }

    /**
     * Gère le tir d'un arc
     */
    private void handleBowShoot(Player player, ItemStack bow, PlayerInteractEvent event) {
        // Annuler l'événement vanilla
        event.setCancelled(true);

        // Vérifier le cooldown
        if (!checkCooldown(player, bowCooldowns, BASE_BOW_COOLDOWN, bow)) {
            return;
        }

        // Créer et lancer la flèche
        shootArrow(player, bow, 2.5f, false);

        // Son de tir
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);

        // Mettre à jour le cooldown
        bowCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Gère le tir d'une arbalète
     */
    private void handleCrossbowShoot(Player player, ItemStack crossbow, PlayerInteractEvent event) {
        // Annuler l'événement vanilla
        event.setCancelled(true);

        // Vérifier le cooldown
        if (!checkCooldown(player, crossbowCooldowns, BASE_CROSSBOW_COOLDOWN, crossbow)) {
            return;
        }

        // Créer et lancer la flèche (plus puissante pour l'arbalète)
        shootArrow(player, crossbow, 3.2f, true);

        // Son de tir d'arbalète
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);

        // Mettre à jour le cooldown
        crossbowCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Vérifie si le joueur peut tirer (cooldown écoulé)
     */
    private boolean checkCooldown(Player player, Map<UUID, Long> cooldowns, long baseCooldown, ItemStack weapon) {
        UUID playerId = player.getUniqueId();
        Long lastShot = cooldowns.get(playerId);

        if (lastShot == null) {
            return true;
        }

        // Calculer le cooldown avec la stat DRAW_SPEED
        long actualCooldown = calculateCooldown(player, baseCooldown);
        long timeSinceLastShot = System.currentTimeMillis() - lastShot;

        return timeSinceLastShot >= actualCooldown;
    }

    /**
     * Calcule le cooldown en tenant compte de la stat DRAW_SPEED
     * DRAW_SPEED augmente la cadence de tir (réduit le cooldown)
     */
    private long calculateCooldown(Player player, long baseCooldown) {
        Map<StatType, Double> stats = plugin.getItemManager().calculatePlayerStats(player);

        double drawSpeedBonus = stats.getOrDefault(StatType.DRAW_SPEED, 0.0);

        // Chaque point de DRAW_SPEED réduit le cooldown de 1%
        // Cap à 70% de réduction maximum pour garder un minimum de cooldown
        double reductionPercent = Math.min(drawSpeedBonus, 70.0);

        double multiplier = 1.0 - (reductionPercent / 100.0);

        return (long) (baseCooldown * multiplier);
    }

    /**
     * Tire une flèche dans la direction où regarde le joueur
     */
    private void shootArrow(Player player, ItemStack weapon, float velocity, boolean isCrossbow) {
        // Créer la flèche à la position des yeux du joueur
        Arrow arrow = player.launchProjectile(Arrow.class);

        // Configurer la direction et la vélocité
        Vector direction = player.getLocation().getDirection();
        arrow.setVelocity(direction.multiply(velocity));

        // Configurer les propriétés de la flèche
        arrow.setShooter(player);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setCritical(true);

        // Les arbalètes ont plus de pénétration par défaut
        if (isCrossbow) {
            arrow.setPierceLevel(1);
        }

        // Marquer la flèche comme projectile ZombieZ
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(zombiezProjectileKey, PersistentDataType.BYTE, (byte) 1);

        // Transférer les données de pouvoir si présentes
        transferPowerData(weapon, arrow);
    }

    /**
     * Transfère les données de pouvoir de l'arme vers le projectile
     */
    private void transferPowerData(ItemStack weapon, Arrow arrow) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return;
        }

        PersistentDataContainer weaponPdc = weapon.getItemMeta().getPersistentDataContainer();
        PersistentDataContainer arrowPdc = arrow.getPersistentDataContainer();

        // Clés de pouvoir
        NamespacedKey hasPowerKey = new NamespacedKey(plugin, "has_power");
        NamespacedKey powerIdKey = new NamespacedKey(plugin, "power_id");
        NamespacedKey itemLevelKey = new NamespacedKey(plugin, "item_level");

        // Clés pour les projectiles
        NamespacedKey projHasPowerKey = new NamespacedKey(plugin, "proj_has_power");
        NamespacedKey projPowerIdKey = new NamespacedKey(plugin, "proj_power_id");
        NamespacedKey projItemLevelKey = new NamespacedKey(plugin, "proj_item_level");

        // Transférer si l'arme a un pouvoir
        if (weaponPdc.has(hasPowerKey, PersistentDataType.BYTE)) {
            Byte hasPower = weaponPdc.get(hasPowerKey, PersistentDataType.BYTE);
            if (hasPower != null && hasPower == 1) {
                String powerId = weaponPdc.get(powerIdKey, PersistentDataType.STRING);
                Integer itemLevel = weaponPdc.get(itemLevelKey, PersistentDataType.INTEGER);

                if (powerId != null) {
                    arrowPdc.set(projHasPowerKey, PersistentDataType.BYTE, (byte) 1);
                    arrowPdc.set(projPowerIdKey, PersistentDataType.STRING, powerId);
                    arrowPdc.set(projItemLevelKey, PersistentDataType.INTEGER, itemLevel != null ? itemLevel : 1);
                }
            }
        }
    }

    /**
     * Nettoie les cooldowns des joueurs déconnectés
     */
    public void cleanupPlayer(UUID playerId) {
        bowCooldowns.remove(playerId);
        crossbowCooldowns.remove(playerId);
    }

    /**
     * Obtient le cooldown restant pour un arc (en millisecondes)
     */
    public long getBowCooldownRemaining(Player player) {
        return getCooldownRemaining(player, bowCooldowns, BASE_BOW_COOLDOWN);
    }

    /**
     * Obtient le cooldown restant pour une arbalète (en millisecondes)
     */
    public long getCrossbowCooldownRemaining(Player player) {
        return getCooldownRemaining(player, crossbowCooldowns, BASE_CROSSBOW_COOLDOWN);
    }

    /**
     * Calcule le cooldown restant
     */
    private long getCooldownRemaining(Player player, Map<UUID, Long> cooldowns, long baseCooldown) {
        UUID playerId = player.getUniqueId();
        Long lastShot = cooldowns.get(playerId);

        if (lastShot == null) {
            return 0;
        }

        long actualCooldown = calculateCooldown(player, baseCooldown);
        long timeSinceLastShot = System.currentTimeMillis() - lastShot;

        return Math.max(0, actualCooldown - timeSinceLastShot);
    }
}
