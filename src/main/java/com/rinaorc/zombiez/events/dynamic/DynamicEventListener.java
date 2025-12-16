package com.rinaorc.zombiez.events.dynamic;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.impl.HordeInvasionEvent;
import com.rinaorc.zombiez.events.dynamic.impl.ZombieNestEvent;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener pour les interactions avec les événements dynamiques
 */
public class DynamicEventListener implements Listener {

    private final ZombieZPlugin plugin;
    private final DynamicEventManager eventManager;

    public DynamicEventListener(ZombieZPlugin plugin, DynamicEventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    /**
     * Gère les dégâts au nid de zombies
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();

        // Vérifier si c'est un nid d'événement
        for (DynamicEvent dynamicEvent : eventManager.getActiveEvents().values()) {
            if (dynamicEvent instanceof ZombieNestEvent nestEvent) {
                if (nestEvent.getNestBlock() != null &&
                    nestEvent.getNestBlock().getLocation().equals(block.getLocation())) {

                    // Calculer les dégâts basés sur l'arme
                    double damage = calculatePlayerDamage(player);
                    nestEvent.damageNest(player, damage);

                    // Empêcher la casse normale du bloc
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Empêche la casse du spawner d'événement
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        // Vérifier si c'est un nid d'événement
        for (DynamicEvent dynamicEvent : eventManager.getActiveEvents().values()) {
            if (dynamicEvent instanceof ZombieNestEvent nestEvent) {
                if (nestEvent.getNestBlock() != null &&
                    nestEvent.getNestBlock().getLocation().equals(block.getLocation())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§c§l⚠ §7Frappez le nid pour l'endommager, ne le cassez pas!");
                    return;
                }
            }
        }
    }

    /**
     * Gère la mort des zombies pour les événements Horde
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Location deathLoc = entity.getLocation();

        // Vérifier si c'est un zombie
        if (!isZombie(entity)) return;

        // Notifier les événements Horde
        for (DynamicEvent dynamicEvent : eventManager.getActiveEvents().values()) {
            if (dynamicEvent instanceof HordeInvasionEvent hordeEvent) {
                if (hordeEvent.isInDefenseZone(deathLoc)) {
                    hordeEvent.onZombieKilled(deathLoc);
                }
            }
        }
    }

    /**
     * Empêche le trading avec les villageois survivants
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        // Vérifier si c'est un survivant de l'événement (ne peut pas trader)
        if (villager.getScoreboardTags().contains("no_trading") ||
            villager.getScoreboardTags().contains("convoy_survivor")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§e§l🛡 §7Ce survivant a besoin de votre protection, pas de commerce!");
        }
    }

    /**
     * Gère les dégâts aux survivants du convoi
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSurvivorDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        // Vérifier si c'est un survivant de convoi
        if (!(victim instanceof Villager villager)) return;
        if (!villager.getScoreboardTags().contains("convoy_survivor")) return;

        // Les joueurs ne peuvent pas blesser les survivants
        if (event.getDamager() instanceof Player) {
            event.setCancelled(true);
            ((Player) event.getDamager()).sendMessage("§c§l⚠ §7Ne blessez pas les survivants!");
            return;
        }

        // Réduire les dégâts des zombies pour que l'escorte soit faisable
        event.setDamage(event.getDamage() * 0.5);
    }

    /**
     * Gère les dégâts au boss errant
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        // Vérifier si c'est un boss d'événement
        if (!(victim instanceof LivingEntity)) return;
        if (!victim.getScoreboardTags().contains("event_boss")) return;

        // Ajouter le joueur aux participants
        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }

        if (damager != null) {
            // Trouver l'événement correspondant
            for (String tag : victim.getScoreboardTags()) {
                if (tag.startsWith("event_")) {
                    String eventId = tag.substring(6);
                    DynamicEvent dynamicEvent = eventManager.getEvent(eventId);
                    if (dynamicEvent != null) {
                        dynamicEvent.addParticipant(damager);
                    }
                }
            }
        }
    }

    /**
     * Calcule les dégâts d'un joueur basé sur son équipement
     */
    private double calculatePlayerDamage(Player player) {
        double baseDamage = 5.0;

        // Bonus selon l'arme en main
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        switch (mainHand) {
            case NETHERITE_SWORD, NETHERITE_AXE -> baseDamage = 15;
            case DIAMOND_SWORD, DIAMOND_AXE -> baseDamage = 12;
            case IRON_SWORD, IRON_AXE -> baseDamage = 10;
            case STONE_SWORD, STONE_AXE -> baseDamage = 7;
            case WOODEN_SWORD, WOODEN_AXE -> baseDamage = 5;
            default -> baseDamage = 3;
        }

        // Bonus des stats d'item ZombieZ (si applicable)
        var itemData = plugin.getItemManager().getItemData(player.getInventory().getItemInMainHand());
        if (itemData != null) {
            baseDamage += (int) itemData.getStat(StatType.DAMAGE);
        }

        return baseDamage;
    }

    /**
     * Vérifie si une entité est un zombie
     */
    private boolean isZombie(Entity entity) {
        return entity.getType().name().toLowerCase().contains("zombie") ||
               entity.getType() == org.bukkit.entity.EntityType.HUSK ||
               entity.getType() == org.bukkit.entity.EntityType.DROWNED ||
               entity.getScoreboardTags().contains("zombiez_mob");
    }

    /**
     * Gère la mort d'un joueur pour bloquer la re-téléportation
     * Si un joueur meurt pendant un événement, il ne peut plus s'y téléporter
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        // Vérifier si le joueur est près d'un événement actif
        for (DynamicEvent dynamicEvent : eventManager.getActiveEvents().values()) {
            if (!dynamicEvent.isActive()) continue;

            // Calculer la distance de manière sécurisée
            Location eventLoc = dynamicEvent.getLocation();
            if (eventLoc.getWorld() == null || deathLoc.getWorld() == null) continue;
            if (!eventLoc.getWorld().equals(deathLoc.getWorld())) continue;

            double distance = deathLoc.distance(eventLoc);

            // Si le joueur est mort dans un rayon raisonnable de l'événement (100 blocs)
            if (distance <= 100) {
                dynamicEvent.recordPlayerDeath(player.getUniqueId());

                // Message au joueur
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage("");
                        player.sendMessage("§c§l☠ §7Vous êtes mort près de l'événement §e" +
                            dynamicEvent.getType().getDisplayName() + "§7!");
                        player.sendMessage("§7Vous ne pourrez §cplus §7vous y téléporter.");
                        player.sendMessage("");
                    }
                }, 40L); // 2 secondes après le respawn
            }
        }
    }
}
