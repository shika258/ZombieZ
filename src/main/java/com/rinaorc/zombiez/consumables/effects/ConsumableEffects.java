package com.rinaorc.zombiez.consumables.effects;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.combat.DamageIndicator;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implémente tous les effets des consommables
 * Chaque méthode gère un type de consommable spécifique
 */
public class ConsumableEffects {

    private final ZombieZPlugin plugin;

    // Tracking pour cooldowns et états
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, JetpackState> activeJetpacks = new ConcurrentHashMap<>();
    private final Set<UUID> activeGrapples = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ConsumableEffects(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Utilise un consommable
     * @return true si le consommable doit être consommé
     */
    public boolean useConsumable(Player player, Consumable consumable, ItemStack item) {
        ConsumableType type = consumable.getType();

        // Vérifier le cooldown
        if (isOnCooldown(player, type)) {
            long remaining = getRemainingCooldown(player, type);
            player.sendMessage("§c✖ §7Cooldown: §e" + String.format("%.1f", remaining / 1000.0) + "s");
            return false;
        }

        boolean consumed = switch (type) {
            case TNT_GRENADE -> useTntGrenade(player, consumable);
            case INCENDIARY_BOMB -> useIncendiaryBomb(player, consumable);
            case STICKY_CHARGE -> useStickyCharge(player, consumable);
            case ACID_JAR -> useAcidJar(player, consumable);
            case JETPACK -> useJetpack(player, consumable, item);
            case GRAPPLING_HOOK -> useGrapplingHook(player, consumable, item);
            case UNSTABLE_PEARL -> useUnstablePearl(player, consumable);
            case COBWEB_TRAP -> useCobwebTrap(player, consumable);
            case DECOY -> useDecoy(player, consumable);
            case TURRET -> useTurret(player, consumable);
            case BANDAGE -> useBandage(player, consumable);
            case ANTIDOTE -> useAntidote(player, consumable);
            case ADRENALINE_KIT -> useAdrenalineKit(player, consumable);
        };

        // Appliquer cooldown si applicable
        if (consumed && type != ConsumableType.JETPACK) {
            applyCooldown(player, type, getCooldownForType(type, consumable));
        }

        return consumed;
    }

    // ==================== EXPLOSIFS ====================

    /**
     * Grenade TNT - Lance une TNT qui explose après un délai
     */
    private boolean useTntGrenade(Player player, Consumable consumable) {
        double damage = consumable.getStat1();
        double radius = consumable.getStat2();
        double delay = consumable.getStat3();

        // Créer un TNT primed
        Location loc = player.getEyeLocation();
        TNTPrimed tnt = player.getWorld().spawn(loc, TNTPrimed.class, entity -> {
            entity.setFuseTicks((int) (delay * 20));
            // Utiliser une petite explosion pour déclencher EntityExplodeEvent
            // Le listener annulera la destruction de blocs
            entity.setYield(0.1f);
            entity.setIsIncendiary(false);
            entity.setMetadata("zombiez_grenade", new FixedMetadataValue(plugin, damage + ":" + radius));
            entity.setMetadata("zombiez_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        });

        // Lancer la grenade
        Vector velocity = player.getLocation().getDirection().multiply(1.2);
        velocity.setY(velocity.getY() + 0.3);
        tnt.setVelocity(velocity);

        // Effets visuels
        player.playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.2f);
        player.sendMessage("§c💣 §7Grenade lancée!");

        // L'explosion est gérée dans le listener
        return true;
    }

    /**
     * Bombe incendiaire - Crée une zone de feu
     */
    private boolean useIncendiaryBomb(Player player, Consumable consumable) {
        double dps = consumable.getStat1();
        double radius = consumable.getStat2();
        double duration = consumable.getStat3();

        // Lancer le projectile
        Location loc = player.getEyeLocation();
        Item bomb = player.getWorld().dropItem(loc, new ItemStack(Material.FIRE_CHARGE));
        bomb.setPickupDelay(Integer.MAX_VALUE);
        bomb.setMetadata("zombiez_incendiary", new FixedMetadataValue(plugin, dps + ":" + radius + ":" + duration));
        bomb.setMetadata("zombiez_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        Vector velocity = player.getLocation().getDirection().multiply(1.5);
        velocity.setY(velocity.getY() + 0.2);
        bomb.setVelocity(velocity);

        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
        player.sendMessage("§c🔥 §7Bombe incendiaire lancée!");

        // Détecter l'impact
        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = bomb.getLocation();

            @Override
            public void run() {
                if (!bomb.isValid() || ticks++ > 100) {
                    triggerIncendiary(bomb.getLocation(), dps, radius, duration, player);
                    bomb.remove();
                    cancel();
                    return;
                }

                // Vérifier si la bombe a touché quelque chose
                if (bomb.isOnGround() || bomb.getLocation().distance(lastLoc) < 0.01) {
                    triggerIncendiary(bomb.getLocation(), dps, radius, duration, player);
                    bomb.remove();
                    cancel();
                }
                lastLoc = bomb.getLocation();
            }
        }.runTaskTimer(plugin, 5L, 1L);

        return true;
    }

    /**
     * Déclenche l'effet incendiaire
     */
    private void triggerIncendiary(Location center, double dps, double radius, double duration, Player owner) {
        // Effet visuel d'explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

        // Zone de feu DOT
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (duration * 20);

            @Override
            public void run() {
                if (ticks++ >= maxTicks) {
                    cancel();
                    return;
                }

                // Particules de feu
                for (int i = 0; i < 10; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * radius * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * radius * 2;
                    center.getWorld().spawnParticle(Particle.FLAME, x, center.getY() + 0.5, z, 1, 0, 0.1, 0, 0);
                }

                // Dégâts aux zombies dans la zone (toutes les 10 ticks = 0.5s)
                if (ticks % 10 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
                        if (isZombieZMob(entity) && entity instanceof LivingEntity living) {
                            applyConsumableDamage(living, dps / 2, owner);
                            living.setFireTicks(40);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Charge collante - Se colle au premier zombie touché et explose
     */
    private boolean useStickyCharge(Player player, Consumable consumable) {
        double damage = consumable.getStat1();
        double splashRadius = consumable.getStat2();
        double fuseTime = consumable.getStat3();

        // Lancer le projectile
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setMetadata("zombiez_sticky", new FixedMetadataValue(plugin, damage + ":" + splashRadius + ":" + fuseTime));
        projectile.setMetadata("zombiez_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        projectile.setVelocity(projectile.getVelocity().multiply(1.5));

        // Changer l'apparence (particules)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!projectile.isValid()) {
                    cancel();
                    return;
                }
                projectile.getWorld().spawnParticle(Particle.ITEM_SLIME, projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        player.playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.5f);
        player.sendMessage("§a💥 §7Charge collante lancée!");

        return true;
    }

    /**
     * Colle la charge à un zombie et démarre le countdown
     */
    public void attachStickyCharge(Entity target, double damage, double splashRadius, double fuseTime, Player owner) {
        if (!(target instanceof LivingEntity living)) return;

        // Effet visuel de la charge collée
        new BukkitRunnable() {
            int ticks = 0;
            final int fuseTicks = (int) (fuseTime * 20);

            @Override
            public void run() {
                if (!target.isValid() || ticks++ >= fuseTicks) {
                    // Explosion
                    explodeStickyCharge(target.getLocation(), damage, splashRadius, owner);
                    cancel();
                    return;
                }

                // Particules de compte à rebours
                target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0, 1.5, 0), 1);

                // Son de bip accéléré
                if (ticks % Math.max(1, (fuseTicks - ticks) / 5) == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Explosion de la charge collante
     */
    private void explodeStickyCharge(Location center, double damage, double splashRadius, Player owner) {
        // Effet d'explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Dégâts
        for (Entity entity : center.getWorld().getNearbyEntities(center, splashRadius, splashRadius, splashRadius)) {
            if (isZombieZMob(entity) && entity instanceof LivingEntity living) {
                double dist = entity.getLocation().distance(center);
                double damageMultiplier = 1 - (dist / splashRadius) * 0.5; // 100% au centre, 50% au bord
                applyConsumableDamage(living, damage * damageMultiplier, owner);
            }
        }
    }

    /**
     * Bocal d'acide - Zone poison
     */
    private boolean useAcidJar(Player player, Consumable consumable) {
        double dps = consumable.getStat1();
        double radius = consumable.getStat2();
        double duration = consumable.getStat3();

        // Lancer le bocal
        ThrownPotion potion = player.launchProjectile(ThrownPotion.class);
        ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
        potion.setItem(potionItem);
        potion.setMetadata("zombiez_acid", new FixedMetadataValue(plugin, dps + ":" + radius + ":" + duration));
        potion.setMetadata("zombiez_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 0.8f);
        player.sendMessage("§2☠ §7Bocal d'acide lancé!");

        return true;
    }

    /**
     * Crée la zone d'acide
     */
    public void createAcidZone(Location center, double dps, double radius, double duration, Player owner) {
        // Effet visuel initial
        center.getWorld().spawnParticle(Particle.ITEM_SLIME, center, 30, radius / 2, 0.5, radius / 2, 0);
        center.getWorld().playSound(center, Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (duration * 20);

            @Override
            public void run() {
                if (ticks++ >= maxTicks) {
                    cancel();
                    return;
                }

                // Particules vertes de poison
                for (int i = 0; i < 5; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * radius * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * radius * 2;
                    center.getWorld().spawnParticle(Particle.ITEM_SLIME, x, center.getY() + 0.3, z, 1, 0, 0, 0, 0);
                }

                // Dégâts aux zombies (toutes les 10 ticks)
                if (ticks % 10 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
                        if (isZombieZMob(entity) && entity instanceof LivingEntity living) {
                            applyConsumableDamage(living, dps / 2, owner);
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ==================== MOBILITÉ ====================

    /**
     * Jetpack - Vol temporaire avec carburant
     */
    private boolean useJetpack(Player player, Consumable consumable, ItemStack item) {
        UUID playerId = player.getUniqueId();

        // Vérifier si déjà actif
        if (activeJetpacks.containsKey(playerId)) {
            // Toggle off
            JetpackState state = activeJetpacks.remove(playerId);
            state.cancel();
            player.sendMessage("§b⛽ §7Jetpack désactivé.");
            return false; // Ne pas consommer
        }

        // Activer le jetpack
        JetpackState state = new JetpackState(player, consumable, item);
        activeJetpacks.put(playerId, state);

        player.sendMessage("§b⛽ §7Jetpack activé! Maintenez §eSneak §7pour voler.");
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);

        return false; // Consommé quand le carburant est vide
    }

    /**
     * Met à jour les jetpacks actifs (appelé par le listener)
     */
    public void updateJetpacks() {
        Iterator<Map.Entry<UUID, JetpackState>> iterator = activeJetpacks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, JetpackState> entry = iterator.next();
            JetpackState state = entry.getValue();

            if (!state.update()) {
                iterator.remove();
            }
        }
    }

    /**
     * Vérifie si un joueur a un jetpack actif
     */
    public boolean hasActiveJetpack(UUID playerId) {
        return activeJetpacks.containsKey(playerId);
    }

    /**
     * État du jetpack
     */
    private class JetpackState {
        private final Player player;
        private final Consumable consumable;
        private final ItemStack item;
        private int fuelRemaining;
        private boolean cancelled = false;

        public JetpackState(Player player, Consumable consumable, ItemStack item) {
            this.player = player;
            this.consumable = consumable;
            this.item = item;
            this.fuelRemaining = consumable.getUsesRemaining();
        }

        public boolean update() {
            if (cancelled || !player.isOnline() || fuelRemaining <= 0) {
                if (fuelRemaining <= 0) {
                    player.sendMessage("§c⛽ §7Jetpack: carburant épuisé!");
                    // Consommer l'item
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                return false;
            }

            // Vérifier si le joueur est en sneak et tient toujours le jetpack
            if (player.isSneaking() && Consumable.isConsumable(player.getInventory().getItemInMainHand()) &&
                Consumable.getType(player.getInventory().getItemInMainHand()) == ConsumableType.JETPACK) {

                // Propulser le joueur
                Vector velocity = player.getVelocity();
                velocity.setY(Math.min(0.8, velocity.getY() + 0.15));

                // Légère poussée dans la direction regardée
                Vector direction = player.getLocation().getDirection().multiply(0.05);
                velocity.add(direction);

                player.setVelocity(velocity);
                player.setFallDistance(0);

                // Consommer du carburant
                fuelRemaining--;

                // Effets visuels et sonores
                if (fuelRemaining % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 5, 0.2, 0.1, 0.2, 0.02);
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.3, 0), 3, 0.1, 0.1, 0.1, 0.01);
                }
                if (fuelRemaining % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.3f, 1.5f);
                }

                // Mettre à jour le lore de l'item
                consumable.use(); // Décrémenter le compteur interne
                consumable.updateItemStack(item);

                // Avertissement carburant bas
                if (fuelRemaining == 40) {
                    player.sendMessage("§e⚠ §7Carburant faible!");
                }
            }

            return true;
        }

        public void cancel() {
            this.cancelled = true;
        }
    }

    /**
     * Grappin - Se propulse vers un point
     */
    private boolean useGrapplingHook(Player player, Consumable consumable, ItemStack item) {
        if (activeGrapples.contains(player.getUniqueId())) {
            return false;
        }

        double range = consumable.getStat1();

        // Raycast pour trouver le point d'accroche
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            range,
            FluidCollisionMode.NEVER,
            true
        );

        if (result == null || result.getHitBlock() == null) {
            player.sendMessage("§c✖ §7Aucune surface à portée!");
            return false;
        }

        Location hookPoint = result.getHitPosition().toLocation(player.getWorld());
        activeGrapples.add(player.getUniqueId());

        // Effet visuel de la corde
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 20 || !player.isOnline()) {
                    activeGrapples.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Dessiner la ligne
                Location start = player.getLocation().add(0, 1, 0);
                Vector direction = hookPoint.clone().subtract(start).toVector().normalize();
                double distance = start.distance(hookPoint);

                for (double d = 0; d < distance; d += 0.5) {
                    Location point = start.clone().add(direction.clone().multiply(d));
                    player.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
                }

                // Propulser le joueur vers le point
                if (ticks <= 10) {
                    Vector pull = hookPoint.clone().subtract(player.getLocation()).toVector();
                    pull.normalize().multiply(1.5);
                    pull.setY(Math.max(0.3, pull.getY()));
                    player.setVelocity(pull);
                    player.setFallDistance(0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.8f);
        player.sendMessage("§b➹ §7Grappin accroché!");

        // Appliquer le cooldown du grappin (3 secondes fixes) après chaque utilisation
        applyCooldown(player, ConsumableType.GRAPPLING_HOOK, 3000);

        // Décrémenter les utilisations
        boolean fullyUsed = consumable.use();
        consumable.updateItemStack(item);

        return fullyUsed;
    }

    /**
     * Perle instable - Téléportation courte
     */
    private boolean useUnstablePearl(Player player, Consumable consumable) {
        double range = consumable.getStat1();
        double selfDamage = consumable.getStat2();

        // Raycast pour trouver le point de destination
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            range,
            FluidCollisionMode.NEVER,
            true
        );

        Location destination;
        if (result != null && result.getHitBlock() != null) {
            // Téléporter juste avant le bloc
            destination = result.getHitPosition().toLocation(player.getWorld());
            destination.subtract(player.getLocation().getDirection().multiply(0.5));
        } else {
            // Téléporter à la distance max
            destination = player.getEyeLocation().add(player.getLocation().getDirection().multiply(range));
        }

        // Trouver un emplacement sûr
        destination = findSafeLocation(destination);

        // Effets visuels départ
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        // Téléportation
        Location finalDest = destination;
        finalDest.setYaw(player.getLocation().getYaw());
        finalDest.setPitch(player.getLocation().getPitch());
        player.teleport(finalDest);

        // Dégâts de self-damage (réduits)
        if (selfDamage > 0) {
            player.damage(selfDamage);
        }

        // Effets visuels arrivée
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 1, 0.5, 0.1);
        player.sendMessage("§5✦ §7Téléportation!");

        return true;
    }

    // ==================== CONTRÔLE DE FOULE ====================

    /**
     * Piège à toile - Place des toiles temporaires
     */
    private boolean useCobwebTrap(Player player, Consumable consumable) {
        int baseCount = (int) consumable.getStat1();
        double duration = consumable.getStat2();
        int count = baseCount + new Random().nextInt(3); // +0 à +2 toiles

        // Trouver où regarder le joueur
        RayTraceResult result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            10,
            FluidCollisionMode.NEVER,
            true
        );

        Location center;
        if (result != null && result.getHitBlock() != null) {
            center = result.getHitBlock().getLocation().add(0, 1, 0);
        } else {
            center = player.getLocation().add(player.getLocation().getDirection().multiply(5));
        }

        List<Block> placedWebs = new ArrayList<>();

        // Placer les toiles
        for (int i = 0; i < count; i++) {
            int offsetX = new Random().nextInt(5) - 2;
            int offsetZ = new Random().nextInt(5) - 2;
            Location webLoc = center.clone().add(offsetX, 0, offsetZ);

            // Trouver le sol
            Block block = webLoc.getWorld().getHighestBlockAt(webLoc);
            Block above = block.getRelative(0, 1, 0);

            if (above.getType() == Material.AIR) {
                above.setType(Material.COBWEB);
                placedWebs.add(above);
            }
        }

        if (placedWebs.isEmpty()) {
            player.sendMessage("§c✖ §7Impossible de placer les toiles!");
            return false;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_WOOL_PLACE, 1.0f, 0.8f);
        player.sendMessage("§f✧ §7" + placedWebs.size() + " toiles placées!");

        // Retirer les toiles après la durée
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Block web : placedWebs) {
                if (web.getType() == Material.COBWEB) {
                    web.setType(Material.AIR);
                    web.getWorld().spawnParticle(Particle.BLOCK, web.getLocation().add(0.5, 0.5, 0.5),
                        10, 0.3, 0.3, 0.3, 0, Material.COBWEB.createBlockData());
                }
            }
        }, (long) (duration * 20));

        return true;
    }

    /**
     * Leurre - Attire les zombies vers un point
     */
    private boolean useDecoy(Player player, Consumable consumable) {
        double duration = consumable.getStat1();
        double aggroRadius = consumable.getStat2();

        // Lancer le leurre
        Location loc = player.getEyeLocation();
        Item decoy = player.getWorld().dropItem(loc, new ItemStack(Material.BELL));
        decoy.setPickupDelay(Integer.MAX_VALUE);
        decoy.setCustomName("§e§lLeurre");
        decoy.setCustomNameVisible(true);
        decoy.setGlowing(true);

        Vector velocity = player.getLocation().getDirection().multiply(0.8);
        velocity.setY(velocity.getY() + 0.3);
        decoy.setVelocity(velocity);

        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.5f);
        player.sendMessage("§e🔔 §7Leurre déployé!");

        // Attendre que le leurre atterrisse puis commencer l'attraction
        new BukkitRunnable() {
            boolean landed = false;
            int ticks = 0;
            final int maxTicks = (int) (duration * 20) + 60; // +3s pour l'atterrissage

            @Override
            public void run() {
                if (!decoy.isValid() || ticks++ > maxTicks) {
                    if (decoy.isValid()) {
                        decoy.getWorld().spawnParticle(Particle.EXPLOSION, decoy.getLocation(), 1);
                        decoy.remove();
                    }
                    cancel();
                    return;
                }

                // Vérifier si atterri
                if (!landed && decoy.isOnGround()) {
                    landed = true;
                    decoy.setVelocity(new Vector(0, 0, 0));
                }

                if (landed) {
                    // Effets visuels
                    if (ticks % 10 == 0) {
                        decoy.getWorld().spawnParticle(Particle.NOTE, decoy.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
                        decoy.getWorld().playSound(decoy.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
                    }

                    // Attirer les zombies
                    for (Entity entity : decoy.getWorld().getNearbyEntities(decoy.getLocation(), aggroRadius, aggroRadius, aggroRadius)) {
                        if (isZombieZMob(entity) && entity instanceof Mob mob) {
                            // Faire que le zombie cible le leurre (en changeant sa cible temporairement)
                            // On utilise un ArmorStand invisible comme fausse cible
                            mob.setTarget(null);

                            // Pousser le zombie vers le leurre
                            Vector direction = decoy.getLocation().subtract(mob.getLocation()).toVector().normalize().multiply(0.3);
                            mob.setVelocity(direction);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==================== INVOCATION ====================

    /**
     * Tourelle Golem - Invoque une mini tourelle
     */
    private boolean useTurret(Player player, Consumable consumable) {
        double damage = consumable.getStat1();
        double duration = consumable.getStat2();
        double range = consumable.getStat3();

        // Trouver l'emplacement de la tourelle
        Location spawnLoc = player.getLocation();

        // Créer un Snow Golem
        int durationSeconds = (int) duration;
        Snowman turret = player.getWorld().spawn(spawnLoc, Snowman.class, golem -> {
            golem.setDerp(false);
            // Nom initial avec durée de vie
            String healthBar = createHealthBar(1.0);
            golem.setCustomName("§b⚙ §fTourelle " + healthBar + " §a" + durationSeconds + "s");
            golem.setCustomNameVisible(true);
            golem.setAI(true); // AI activée pour le pathfinding
            golem.addScoreboardTag("zombiez_turret");
            golem.setMetadata("zombiez_turret_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            golem.setMetadata("zombiez_turret_damage", new FixedMetadataValue(plugin, damage));
            // Vitesse très lente
            if (golem.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
                golem.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.05); // Très lent (défaut: 0.2)
            }
        });

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.5f);
        player.sendMessage("§b⚙ §7Tourelle déployée! Durée: " + String.format("%.0f", duration) + "s");

        // Stocker la position initiale pour vérifier la distance
        final Location spawnLocation = spawnLoc.clone();
        final double maxDistanceFromOwner = 50.0; // Distance max avant despawn

        // Logique de la tourelle
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (duration * 20);
            int fireCooldown = 0;
            // Système anti-blocage : change de cible si pas de ligne de vue après plusieurs tirs
            UUID currentTargetId = null;
            int missedShots = 0;
            final int maxMissedShots = 3; // Change de cible après 3 tirs ratés
            final Set<UUID> blacklistedTargets = new HashSet<>(); // Cibles temporairement ignorées
            int blacklistClearCooldown = 0;
            int lastDisplayedSeconds = -1; // Pour éviter les mises à jour inutiles

            @Override
            public void run() {
                // Vérifier si le chunk est chargé
                if (!spawnLocation.getChunk().isLoaded()) {
                    if (turret.isValid()) {
                        turret.remove();
                    }
                    cancel();
                    return;
                }

                // Vérifier si le joueur est toujours en ligne et pas trop loin
                if (!player.isOnline() || player.getLocation().distance(spawnLocation) > maxDistanceFromOwner) {
                    if (turret.isValid()) {
                        turret.getWorld().spawnParticle(Particle.CLOUD, turret.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.03);
                        turret.remove();
                    }
                    cancel();
                    return;
                }

                if (!turret.isValid() || ticks >= maxTicks) {
                    if (turret.isValid()) {
                        turret.getWorld().spawnParticle(Particle.CLOUD, turret.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                        turret.remove();
                    }
                    cancel();
                    return;
                }

                // Nettoyer la blacklist périodiquement (toutes les 3 secondes)
                if (blacklistClearCooldown > 0) {
                    blacklistClearCooldown--;
                } else if (!blacklistedTargets.isEmpty()) {
                    blacklistedTargets.clear();
                    blacklistClearCooldown = 60; // 3 secondes
                }

                // Cooldown entre les tirs
                if (fireCooldown > 0) {
                    fireCooldown--;
                    return;
                }

                // Trouver la cible la plus proche (en excluant les cibles blacklistées)
                LivingEntity target = null;
                double closestDist = range;

                for (Entity entity : turret.getWorld().getNearbyEntities(turret.getLocation(), range, range, range)) {
                    if (isZombieZMob(entity) && entity instanceof LivingEntity living) {
                        // Ignorer les cibles blacklistées
                        if (blacklistedTargets.contains(entity.getUniqueId())) {
                            continue;
                        }
                        double dist = entity.getLocation().distance(turret.getLocation());
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = living;
                        }
                    }
                }

                // Tirer sur la cible
                if (target != null) {
                    Location turretLoc = turret.getLocation();
                    Location targetLoc = target.getLocation().add(0, 1, 0);
                    Location shootFrom = turretLoc.clone().add(0, 1.5, 0);

                    // Vérifier la ligne de vue avec un raycast
                    Vector direction = targetLoc.subtract(shootFrom).toVector().normalize();
                    double distToTarget = shootFrom.distance(target.getLocation().add(0, 1, 0));

                    RayTraceResult rayTrace = turret.getWorld().rayTraceBlocks(
                        shootFrom, direction, distToTarget, FluidCollisionMode.NEVER, true
                    );

                    boolean hasLineOfSight = (rayTrace == null || rayTrace.getHitBlock() == null);

                    // Vérifier si c'est une nouvelle cible
                    if (currentTargetId == null || !currentTargetId.equals(target.getUniqueId())) {
                        currentTargetId = target.getUniqueId();
                        missedShots = 0;
                    }

                    // Si pas de ligne de vue, compter comme un tir raté
                    if (!hasLineOfSight) {
                        missedShots++;
                        if (missedShots >= maxMissedShots) {
                            // Blacklister cette cible et en chercher une autre
                            blacklistedTargets.add(target.getUniqueId());
                            currentTargetId = null;
                            missedShots = 0;
                            fireCooldown = 5; // Court délai avant de réessayer
                            return;
                        }
                    } else {
                        // Ligne de vue OK, réinitialiser le compteur
                        missedShots = 0;
                    }

                    // Déplacer lentement le golem vers la cible
                    turret.getPathfinder().moveTo(target);

                    // Projectile (même si pas de ligne de vue, pour que le joueur voie que la tourelle essaie)
                    final LivingEntity finalTarget = target;
                    Snowball snowball = turret.getWorld().spawn(shootFrom, Snowball.class, s -> {
                        Vector shootDir = finalTarget.getLocation().add(0, 1, 0).subtract(shootFrom).toVector().normalize();
                        s.setVelocity(shootDir.multiply(2));
                        s.setMetadata("zombiez_turret_projectile", new FixedMetadataValue(plugin, damage));
                        s.setMetadata("zombiez_owner", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
                    });

                    // Effets
                    turret.getWorld().playSound(turret.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, 1.0f, 1.2f);

                    fireCooldown = 15; // 0.75s entre les tirs
                } else {
                    // Pas de cible, arrêter de bouger
                    turret.getPathfinder().stopPathfinding();
                    currentTargetId = null;
                    missedShots = 0;
                }

                // Mise à jour du nom avec temps restant et barre de vie (à chaque seconde)
                int secondsRemaining = (maxTicks - ticks) / 20;
                if (secondsRemaining != lastDisplayedSeconds) {
                    lastDisplayedSeconds = secondsRemaining;
                    double healthPercent = 1.0 - ((double) ticks / maxTicks);
                    String healthBar = createHealthBar(healthPercent);
                    String timeColor = secondsRemaining <= 5 ? "§c" : (secondsRemaining <= 10 ? "§e" : "§a");
                    turret.setCustomName("§b⚙ §fTourelle " + healthBar + " " + timeColor + secondsRemaining + "s");
                }

                // Incrémenter les ticks à la fin
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // ==================== SOINS ====================

    /**
     * Bandage - Soin + Régénération
     */
    private boolean useBandage(Player player, Consumable consumable) {
        double heal = consumable.getStat1();
        double regenDuration = consumable.getStat2();

        // Soin instantané
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, player.getHealth() + heal));

        // Régénération
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION,
            (int) (regenDuration * 20),
            0, // Regen I
            false, true
        ));

        // Effets
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);

        // Hologramme de soin (positionné sur le côté pour ne pas gêner la vue)
        Location holoLoc = player.getLocation().add(player.getLocation().getDirection().rotateAroundY(Math.PI / 2).multiply(0.5));
        DamageIndicator.displayHeal(plugin, holoLoc.add(0, 0.3, 0), heal, player);

        // Hologramme de régénération (petite icône à côté)
        displayRegenHologram(player, regenDuration, 1);

        player.sendMessage("§a❤ §7Bandage appliqué! §a+" + String.format("%.1f", heal) + " HP");

        return true;
    }

    /**
     * Antidote - Purge debuffs + Immunité
     */
    private boolean useAntidote(Player player, Consumable consumable) {
        double immunityDuration = consumable.getStat2();

        // Liste des debuffs à purger
        PotionEffectType[] debuffs = {
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION,
            PotionEffectType.UNLUCK
        };

        int purgedCount = 0;
        for (PotionEffectType type : debuffs) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
                purgedCount++;
            }
        }

        // Immunité temporaire (resistance aux effets négatifs)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE,
            (int) (immunityDuration * 20),
            0,
            false, true
        ));

        // Marquer l'immunité pour le système
        player.setMetadata("zombiez_debuff_immunity",
            new FixedMetadataValue(plugin, System.currentTimeMillis() + (long) (immunityDuration * 1000)));

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);

        // Hologramme d'immunité (positionné sur le côté)
        displayImmunityHologram(player, immunityDuration);

        player.sendMessage("§a✓ §7Antidote! §d" + purgedCount + " §7effet(s) purgé(s) + §b" +
                           String.format("%.1f", immunityDuration) + "s §7d'immunité");

        return true;
    }

    /**
     * Kit d'adrénaline - Soin + Regen + Speed
     */
    private boolean useAdrenalineKit(Player player, Consumable consumable) {
        double heal = consumable.getStat1();
        double regenDuration = consumable.getStat2();
        double speedDuration = consumable.getStat3();

        // Soin instantané
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, player.getHealth() + heal));

        // Régénération
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION,
            (int) (regenDuration * 20),
            1, // Regen II
            false, true
        ));

        // Speed boost
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            (int) (speedDuration * 20),
            1, // Speed II
            false, true
        ));

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);

        // Hologramme de soin (positionné sur le côté)
        Location holoLoc = player.getLocation().add(player.getLocation().getDirection().rotateAroundY(Math.PI / 2).multiply(0.5));
        DamageIndicator.displayHeal(plugin, holoLoc.add(0, 0.3, 0), heal, player);

        // Hologramme de régénération et vitesse
        displayRegenHologram(player, regenDuration, 2);
        displaySpeedHologram(player, speedDuration);

        player.sendMessage("§c⚡ §7ADRÉNALINE! §a+" + String.format("%.1f", heal) + " HP §7+ §dRegen §7+ §bSpeed!");

        return true;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si une entité est un mob ZombieZ
     */
    private boolean isZombieZMob(Entity entity) {
        return entity.hasMetadata("zombiez_type") || entity.getScoreboardTags().contains("zombiez_mob");
    }

    /**
     * Trouve un emplacement sûr pour la téléportation
     */
    private Location findSafeLocation(Location loc) {
        Location safe = loc.clone();
        World world = safe.getWorld();

        // Descendre jusqu'au sol
        while (safe.getY() > 0 && safe.getBlock().getType() == Material.AIR) {
            safe.subtract(0, 1, 0);
        }
        safe.add(0, 1, 0);

        // Vérifier que l'espace est libre
        if (safe.getBlock().getType() != Material.AIR ||
            safe.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) {
            // Chercher un espace au-dessus
            for (int i = 0; i < 10; i++) {
                safe.add(0, 1, 0);
                if (safe.getBlock().getType() == Material.AIR &&
                    safe.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                    break;
                }
            }
        }

        return safe;
    }

    /**
     * Crée une barre de vie visuelle
     */
    private String createHealthBar(double percent) {
        int filled = (int) (percent * 10);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a|");
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    /**
     * Vérifie si un joueur est en cooldown pour un type
     */
    private boolean isOnCooldown(Player player, ConsumableType type) {
        String key = player.getUniqueId() + ":" + type.name();
        Long endTime = playerCooldowns.get(UUID.nameUUIDFromBytes(key.getBytes()));
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    /**
     * Obtient le temps de cooldown restant
     */
    private long getRemainingCooldown(Player player, ConsumableType type) {
        String key = player.getUniqueId() + ":" + type.name();
        Long endTime = playerCooldowns.get(UUID.nameUUIDFromBytes(key.getBytes()));
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * Applique un cooldown
     */
    private void applyCooldown(Player player, ConsumableType type, long durationMs) {
        if (durationMs <= 0) return;
        String key = player.getUniqueId() + ":" + type.name();
        playerCooldowns.put(UUID.nameUUIDFromBytes(key.getBytes()), System.currentTimeMillis() + durationMs);
    }

    /**
     * Obtient le cooldown par défaut pour un type (en ms)
     */
    private long getCooldownForType(ConsumableType type, Consumable consumable) {
        return switch (type) {
            case TNT_GRENADE, INCENDIARY_BOMB, ACID_JAR -> 2000;
            case STICKY_CHARGE -> 1500;
            case GRAPPLING_HOOK -> 3000; // Cooldown fixe 3s, peu importe la rareté ou zone
            case UNSTABLE_PEARL -> (long) (consumable.getStat3() * 1000);
            case COBWEB_TRAP, DECOY, TURRET -> 5000;
            case BANDAGE, ANTIDOTE, ADRENALINE_KIT -> 3000;
            default -> 1000;
        };
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

    // ==================== HOLOGRAMMES DE SOINS ====================

    /**
     * Affiche un hologramme de régénération non-intrusif
     * Positionné sur le côté gauche du joueur, petit et discret
     */
    private void displayRegenHologram(Player player, double duration, int level) {
        // Position sur le côté gauche du joueur (ne bloque pas la vue)
        Location loc = player.getLocation().add(
            player.getLocation().getDirection().rotateAroundY(-Math.PI / 2).multiply(0.6)
        ).add(0, 0.5, 0);

        player.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            String levelText = level > 1 ? " II" : "";
            Component text = Component.text("♥", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(" Regen" + levelText + " ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(String.format("%.0fs", duration), NamedTextColor.WHITE));
            display.text(text);

            // Petite taille pour ne pas gêner
            float scale = 0.6f;
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
            display.setInterpolationDuration(6);
            display.setInterpolationDelay(0);

            // Visible uniquement pour le joueur concerné
            display.setVisibleByDefault(false);
            player.showEntity(plugin, display);

            // Animation courte vers le haut puis suppression
            animateEffectHologram(display, 25);
        });
    }

    /**
     * Affiche un hologramme d'immunité non-intrusif
     */
    private void displayImmunityHologram(Player player, double duration) {
        // Position légèrement au-dessus et sur le côté
        Location loc = player.getLocation().add(
            player.getLocation().getDirection().rotateAroundY(Math.PI / 2).multiply(0.5)
        ).add(0, 0.6, 0);

        player.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("✦", TextColor.color(0x55FFFF))
                .append(Component.text(" Immunité ", TextColor.color(0x55FFFF)))
                .append(Component.text(String.format("%.0fs", duration), NamedTextColor.WHITE));
            display.text(text);

            float scale = 0.6f;
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
            display.setInterpolationDuration(6);
            display.setInterpolationDelay(0);

            display.setVisibleByDefault(false);
            player.showEntity(plugin, display);

            animateEffectHologram(display, 25);
        });
    }

    /**
     * Affiche un hologramme de vitesse non-intrusif
     */
    private void displaySpeedHologram(Player player, double duration) {
        // Position sur le côté droit, légèrement plus bas
        Location loc = player.getLocation().add(
            player.getLocation().getDirection().rotateAroundY(Math.PI / 2).multiply(0.6)
        ).add(0, 0.2, 0);

        player.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("»", TextColor.color(0x55FFFF))
                .append(Component.text(" Speed II ", TextColor.color(0x55FFFF)))
                .append(Component.text(String.format("%.0fs", duration), NamedTextColor.WHITE));
            display.text(text);

            float scale = 0.6f;
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
            display.setInterpolationDuration(6);
            display.setInterpolationDelay(0);

            display.setVisibleByDefault(false);
            player.showEntity(plugin, display);

            animateEffectHologram(display, 25);
        });
    }

    /**
     * Animation courte pour les hologrammes d'effets
     * Monte légèrement puis disparaît - non-intrusif
     */
    private void animateEffectHologram(TextDisplay display, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            final Location startLoc = display.getLocation().clone();

            @Override
            public void run() {
                if (ticks >= durationTicks || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / durationTicks;

                // Mouvement lent vers le haut
                double yOffset = progress * 0.3;
                display.teleport(startLoc.clone().add(0, yOffset, 0));

                // Fade-out progressif via scale
                float scale = 0.6f;
                if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = 0.6f * (1 - fadeProgress * 0.5f);
                }

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        activeJetpacks.clear();
        activeGrapples.clear();
        playerCooldowns.clear();
    }
}
