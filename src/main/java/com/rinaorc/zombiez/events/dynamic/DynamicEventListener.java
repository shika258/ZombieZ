package com.rinaorc.zombiez.events.dynamic;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableType;
import com.rinaorc.zombiez.events.dynamic.impl.HordeInvasionEvent;
import com.rinaorc.zombiez.events.dynamic.impl.ZombieNestEvent;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
     * Gère les dégâts aux zombies de horde pour mettre à jour leur affichage de vie
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHordeZombieDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Vérifier si c'est un zombie de horde
        if (!entity.getScoreboardTags().contains("horde_invasion")) return;

        // Mettre à jour l'affichage de vie au tick suivant (après que les dégâts soient appliqués)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!entity.isValid() || entity.isDead()) return;

            // Trouver l'événement de horde correspondant
            for (DynamicEvent dynamicEvent : eventManager.getActiveEvents().values()) {
                if (dynamicEvent instanceof HordeInvasionEvent hordeEvent) {
                    if (hordeEvent.isHordeZombie(entity.getUniqueId())) {
                        hordeEvent.updateHordeZombieHealth(entity);
                        return;
                    }
                }
            }
        });
    }

    /**
     * Gère l'interaction avec les villageois survivants (trading interdit, soin autorisé)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        // Vérifier si c'est un survivant de l'événement
        if (!villager.getScoreboardTags().contains("convoy_survivor")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifier si le joueur tient un item de soin
        if (tryHealSurvivor(player, villager, item)) {
            return;
        }

        // Sinon, message standard
        player.sendMessage("§e§l🛡 §7Ce survivant a besoin de votre protection!");
        player.sendMessage("§a💉 §7Utilisez un §eBandage §7ou un §eKit d'Adrénaline §7pour le soigner!");
    }

    /**
     * Tente de soigner un survivant avec un item de soin
     * @return true si le soin a été effectué
     */
    private boolean tryHealSurvivor(Player player, Villager villager, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        double healAmount = 0;
        boolean consumeItem = false;
        String healMessage = "";

        // Vérifier si c'est un consommable ZombieZ de type soin
        if (Consumable.isConsumable(item)) {
            ConsumableType type = Consumable.getType(item);
            if (type != null && type.getCategory() == ConsumableType.ConsumableCategory.HEALING) {
                Consumable consumable = Consumable.fromItemStack(item);
                if (consumable != null) {
                    switch (type) {
                        case BANDAGE -> {
                            healAmount = consumable.getStat1(); // Le soin du bandage
                            healMessage = "§a💉 §7Vous avez soigné le survivant avec un §eBandage§7!";
                        }
                        case ADRENALINE_KIT -> {
                            healAmount = consumable.getStat1() * 1.5; // Bonus pour le kit
                            healMessage = "§c⚡ §7Vous avez soigné le survivant avec un §eKit d'Adrénaline§7!";
                        }
                        case ANTIDOTE -> {
                            healAmount = 5; // L'antidote soigne peu mais peut être utilisé
                            healMessage = "§a✓ §7Vous avez utilisé un §eAntidote §7sur le survivant!";
                        }
                    }
                    consumeItem = true;
                }
            }
        }

        // Vérifier les items vanilla de soin (pour plus de flexibilité)
        if (healAmount == 0) {
            switch (item.getType()) {
                case GOLDEN_APPLE -> {
                    healAmount = 8;
                    healMessage = "§6🍎 §7Vous avez donné une §ePomme Dorée §7au survivant!";
                    consumeItem = true;
                }
                case ENCHANTED_GOLDEN_APPLE -> {
                    healAmount = 20;
                    healMessage = "§6§l🍎 §7Vous avez donné une §ePomme Dorée Enchantée §7au survivant!";
                    consumeItem = true;
                }
                case POTION -> {
                    // Vérifier si c'est une potion de soin
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                        item.getItemMeta().getDisplayName().toLowerCase().contains("soin")) {
                        healAmount = 8;
                        healMessage = "§d💧 §7Vous avez donné une §ePotion de Soin §7au survivant!";
                        consumeItem = true;
                    }
                }
            }
        }

        // Appliquer le soin si applicable
        if (healAmount > 0) {
            var maxHealthAttr = villager.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20;
            double currentHealth = villager.getHealth();

            // Vérifier si le survivant a besoin de soin
            if (currentHealth >= maxHealth) {
                player.sendMessage("§e§l🛡 §7Ce survivant est déjà en pleine santé!");
                return true; // Ne pas consommer l'item mais annuler l'interaction
            }

            // Appliquer le soin
            double newHealth = Math.min(maxHealth, currentHealth + healAmount);
            villager.setHealth(newHealth);

            // Effets visuels et sonores
            villager.getWorld().spawnParticle(Particle.HEART, villager.getLocation().add(0, 2, 0), 8, 0.4, 0.4, 0.4, 0);
            villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.2f);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);

            // Message au joueur
            player.sendMessage(healMessage);
            player.sendMessage("§7Vie: §a" + String.format("%.0f", newHealth) + "/" + String.format("%.0f", maxHealth) +
                " §7(§a+" + String.format("%.1f", newHealth - currentHealth) + " HP§7)");

            // Consommer l'item
            if (consumeItem) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            }

            return true;
        }

        return false;
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
