package com.rinaorc.zombiez.consumables;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.effects.ConsumableEffects;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.UUID;

/**
 * Listener pour gérer l'utilisation des consommables
 * Intercepte les clics droits et les événements de projectiles
 */
public class ConsumableListener implements Listener {

    private final ZombieZPlugin plugin;
    private final ConsumableManager consumableManager;

    public ConsumableListener(ZombieZPlugin plugin, ConsumableManager consumableManager) {
        this.plugin = plugin;
        this.consumableManager = consumableManager;
    }

    /**
     * Gère l'utilisation des consommables via clic droit
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Vérifier que c'est un clic droit
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ignorer la main secondaire
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;

        // Vérifier si c'est un consommable ZombieZ
        if (!Consumable.isConsumable(item)) {
            return;
        }

        Player player = event.getPlayer();

        // Charger le consommable depuis l'item
        Consumable consumable = Consumable.fromItemStack(item);
        if (consumable == null) {
            return;
        }

        // Annuler l'événement vanilla
        event.setCancelled(true);

        // Utiliser le consommable
        boolean consumed = consumableManager.useConsumable(player, consumable, item);

        // Retirer l'item si consommé
        if (consumed) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    /**
     * Empêche la consommation vanilla des items de consommables
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (Consumable.isConsumable(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Gère l'explosion des grenades TNT
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof TNTPrimed tnt && tnt.hasMetadata("zombiez_grenade")) {
            // C'est une grenade ZombieZ - empêcher la destruction de blocs mais garder l'événement
            event.blockList().clear();
            event.setYield(0);

            // Récupérer les stats
            String data = tnt.getMetadata("zombiez_grenade").get(0).asString();
            String[] parts = data.split(":");
            double damage = Double.parseDouble(parts[0]);
            double radius = Double.parseDouble(parts[1]);

            // Récupérer le propriétaire
            Player owner = null;
            if (tnt.hasMetadata("zombiez_owner")) {
                String ownerId = tnt.getMetadata("zombiez_owner").get(0).asString();
                owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
            }

            // Appliquer les dégâts aux zombies et autres mobs ZombieZ
            Location center = tnt.getLocation();
            for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (nearby instanceof LivingEntity living && isZombieZMob(nearby)) {
                    double dist = nearby.getLocation().distance(center);
                    // Distance-based falloff: 100% au centre, 50% au bord
                    double distRatio = Math.min(1.0, dist / radius);
                    double damageMultiplier = 1.0 - (distRatio * 0.5);
                    double finalDamage = damage * damageMultiplier;

                    // Appliquer les dégâts avec marqueur de consommable
                    applyConsumableDamage(living, finalDamage, owner);

                    // Knockback
                    if (dist > 0.1) {
                        org.bukkit.util.Vector knockback = nearby.getLocation().subtract(center).toVector().normalize().multiply(0.8);
                        knockback.setY(0.4);
                        nearby.setVelocity(knockback);
                    }
                }
            }

            // Effets visuels
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        }
    }

    /**
     * Gère l'impact des charges collantes et bocals d'acide
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // Charge collante
        if (projectile.hasMetadata("zombiez_sticky")) {
            Entity hitEntity = event.getHitEntity();

            if (hitEntity != null && isZombieZMob(hitEntity)) {
                // Coller au zombie
                String data = projectile.getMetadata("zombiez_sticky").get(0).asString();
                String[] parts = data.split(":");
                double damage = Double.parseDouble(parts[0]);
                double splashRadius = Double.parseDouble(parts[1]);
                double fuseTime = Double.parseDouble(parts[2]);

                Player owner = null;
                if (projectile.hasMetadata("zombiez_owner")) {
                    String ownerId = projectile.getMetadata("zombiez_owner").get(0).asString();
                    owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
                }

                consumableManager.getConsumableEffects().attachStickyCharge(hitEntity, damage, splashRadius, fuseTime, owner);
            } else {
                // A touché le sol, exploser immédiatement
                Location loc = projectile.getLocation();
                String data = projectile.getMetadata("zombiez_sticky").get(0).asString();
                String[] parts = data.split(":");
                double damage = Double.parseDouble(parts[0]);
                double splashRadius = Double.parseDouble(parts[1]);

                Player owner = null;
                if (projectile.hasMetadata("zombiez_owner")) {
                    String ownerId = projectile.getMetadata("zombiez_owner").get(0).asString();
                    owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
                }

                // Explosion immédiate
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                for (Entity entity : loc.getWorld().getNearbyEntities(loc, splashRadius, splashRadius, splashRadius)) {
                    if (isZombieZMob(entity) && entity instanceof LivingEntity living) {
                        double dist = entity.getLocation().distance(loc);
                        double damageMultiplier = 1 - (dist / splashRadius) * 0.5;
                        applyConsumableDamage(living, damage * damageMultiplier, owner);
                    }
                }
            }

            projectile.remove();
        }

        // Bocal d'acide
        if (projectile.hasMetadata("zombiez_acid")) {
            String data = projectile.getMetadata("zombiez_acid").get(0).asString();
            String[] parts = data.split(":");
            double dps = Double.parseDouble(parts[0]);
            double radius = Double.parseDouble(parts[1]);
            double duration = Double.parseDouble(parts[2]);

            Player owner = null;
            if (projectile.hasMetadata("zombiez_owner")) {
                String ownerId = projectile.getMetadata("zombiez_owner").get(0).asString();
                owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
            }

            consumableManager.getConsumableEffects().createAcidZone(projectile.getLocation(), dps, radius, duration, owner);
        }

        // Projectile de tourelle
        if (projectile.hasMetadata("zombiez_turret_projectile")) {
            Entity hitEntity = event.getHitEntity();

            if (hitEntity != null && isZombieZMob(hitEntity) && hitEntity instanceof LivingEntity living) {
                double damage = projectile.getMetadata("zombiez_turret_projectile").get(0).asDouble();

                Player owner = null;
                if (projectile.hasMetadata("zombiez_owner")) {
                    String ownerId = projectile.getMetadata("zombiez_owner").get(0).asString();
                    owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
                }

                // Appliquer les dégâts avec marqueur de consommable
                applyConsumableDamage(living, damage, owner);

                // Effet de givre
                living.setFreezeTicks(40);
                hitEntity.getWorld().spawnParticle(Particle.SNOWFLAKE, hitEntity.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /**
     * Gère les dégâts des tourelles - elles sont indestructibles
     * Annule tous types de dégâts (entités, feu, chute, suffocation, etc.)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        // Les tourelles sont indestructibles - seule leur durée peut les enlever
        if (event.getEntity() instanceof Snowman snowman && snowman.hasMetadata("zombiez_turret_owner")) {
            event.setCancelled(true);
        }
    }

    /**
     * Gère la mort des tourelles
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Tourelle morte
        if (entity instanceof Snowman && entity.hasMetadata("zombiez_turret_owner")) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.5f);

            // Notifier le propriétaire
            String ownerId = entity.getMetadata("zombiez_turret_owner").get(0).asString();
            Player owner = plugin.getServer().getPlayer(UUID.fromString(ownerId));
            if (owner != null) {
                owner.sendMessage("§c⚙ §7Votre tourelle a été détruite!");
            }
        }
    }

    /**
     * Empêche le ciblage des tourelles par les zombies
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        Entity target = event.getTarget();

        // Les zombies ne peuvent pas cibler les tourelles
        if (target instanceof Snowman snowman && snowman.hasMetadata("zombiez_turret_owner")) {
            if (isZombieZMob(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Ramassage de consommables - message spécial pour les rares
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!Consumable.isConsumable(item)) return;

        Consumable consumable = Consumable.fromItemStack(item);
        if (consumable == null) return;

        // Message selon la rareté
        ConsumableRarity rarity = consumable.getRarity();
        if (rarity.ordinal() >= ConsumableRarity.RARE.ordinal()) {
            String message = rarity.getColor() + rarity.getDisplayName() + " " +
                            consumable.getType().getDisplayName();
            player.sendActionBar(net.kyori.adventure.text.Component.text("§a📦 " + message));

            // Son pour les items rares+
            if (rarity == ConsumableRarity.LEGENDARY) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
            } else if (rarity == ConsumableRarity.EPIC) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        }
    }

    /**
     * Applique les dégâts d'un consommable avec un marqueur pour éviter
     * que CombatListener n'applique les stats d'arme du joueur
     */
    private void applyConsumableDamage(LivingEntity target, double damage, Player owner) {
        // Marquer l'entité pour indiquer que les dégâts viennent d'un consommable
        target.setMetadata("zombiez_consumable_damage", new FixedMetadataValue(plugin, damage));

        // Appliquer les dégâts
        if (owner != null) {
            target.damage(damage, owner);
        } else {
            target.damage(damage);
        }

        // Retirer le marqueur après l'application (au tick suivant pour être sûr)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            target.removeMetadata("zombiez_consumable_damage", plugin);
        });
    }

    /**
     * Vérifie si une entité est un mob ZombieZ
     */
    private boolean isZombieZMob(Entity entity) {
        return entity.hasMetadata("zombiez_type") || entity.getScoreboardTags().contains("zombiez_mob");
    }
}
