package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.ai.WanderingBossAI;
import com.rinaorc.zombiez.zombies.ai.ZombieAI;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Random;

/**
 * Événement Boss Errant
 *
 * Déroulement:
 * 1. Un boss puissant apparaît et commence à se déplacer
 * 2. Le boss a une destination et s'y dirige lentement
 * 3. Les joueurs doivent le tuer avant qu'il n'atteigne sa destination
 * 4. Le boss a des capacités spéciales et spawn des adds
 * 5. Grosses récompenses si tué
 */
public class WanderingBossEvent extends DynamicEvent {

    // Le boss et son IA
    @Getter
    private LivingEntity boss;
    private WanderingBossAI bossAI;
    private String bossName;
    private double bossMaxHealth;
    private double bossCurrentHealth;

    // Destination
    private Location destination;
    private double totalDistance;

    // Boss bar spéciale
    private BossBar bossBossBar;

    // État
    private int tickCounter = 0;

    // Types de boss selon la zone
    private static final String[] BOSS_NAMES = {
        "Le Boucher Maudit",
        "L'Abomination",
        "Le Général Mort-Vivant",
        "Le Faucheur",
        "Le Colosse Putride"
    };

    // Random pour le loot
    private final Random random = new Random();

    public WanderingBossEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.WANDERING_BOSS, location, zone);

        // Choisir un nom de boss
        this.bossName = BOSS_NAMES[new Random().nextInt(BOSS_NAMES.length)];

        // Stats basées sur la zone (utilisées pour la BossBar)
        this.bossMaxHealth = 500 + (zone.getId() * 50);
        this.bossCurrentHealth = bossMaxHealth;
    }

    @Override
    protected void startMainLogic() {
        // Calculer la destination
        calculateDestination();

        // Spawn le boss
        spawnBoss();

        // Créer la boss bar spéciale
        createBossBossBar();

        // Démarrer le tick
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                tick();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * Calcule la destination du boss
     */
    private void calculateDestination() {
        World world = location.getWorld();
        if (world == null) return;

        // Destination: ~150 blocs dans une direction (préférence nord - vers les joueurs avancés)
        double distance = 120 + Math.random() * 60;
        double angle = -Math.PI / 2 + (Math.random() - 0.5) * Math.PI; // Principalement nord

        double x = location.getX() + Math.cos(angle) * distance;
        double z = location.getZ() + Math.sin(angle) * distance;

        // Vérifier les limites
        Location testLoc = new Location(world, x, 0, z);
        if (!plugin.getZoneManager().isInMapBounds(testLoc)) {
            // Inverser la direction
            x = location.getX() - Math.cos(angle) * distance;
            z = location.getZ() - Math.sin(angle) * distance;
        }

        int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
        destination = new Location(world, x, y, z);
        totalDistance = location.distance(destination);
    }

    /**
     * Spawn le boss via ZombieManager pour bénéficier du système ZombieZ complet
     */
    private void spawnBoss() {
        World world = location.getWorld();
        if (world == null) return;

        // Calculer le niveau du boss basé sur la zone
        int bossLevel = zone.getId();

        // Spawn via ZombieManager pour bénéficier du système ZombieZ complet
        ZombieManager zombieManager = plugin.getZombieManager();
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.WANDERING_BOSS, location, bossLevel);

        // Si le spawn a échoué, annuler l'événement
        if (activeZombie == null) {
            plugin.getLogger().warning("Échec du spawn du Wandering Boss - événement annulé");
            fail();
            return;
        }

        // Récupérer l'entité spawnée
        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (entity == null || !(entity instanceof LivingEntity living)) {
            plugin.getLogger().warning("Entité du Wandering Boss invalide - événement annulé");
            fail();
            return;
        }

        boss = living;

        // Récupérer et configurer l'IA
        ZombieAI ai = plugin.getZombieManager().getAiManager().getAI(boss.getUniqueId());
        if (ai instanceof WanderingBossAI wanderingAI) {
            bossAI = wanderingAI;
            // Configurer l'IA avec la destination et le nom
            bossAI.setDestinationWithDistance(destination);
            bossAI.setBossName(bossName);
            // Vitesse adaptée à la zone
            bossAI.setMoveSpeed(0.12 + (zone.getId() * 0.003));
        }

        // Ajuster les stats du boss (override les stats par défaut du ZombieType)
        var maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(bossMaxHealth);
            boss.setHealth(bossMaxHealth);
        }

        var damageAttr = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(15 + zone.getId());
        }

        // Vitesse de mouvement normale (l'IA gère le pathfinding)
        var speedAttr = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.25 + zone.getId() * 0.002);
        }

        // Effets visuels
        boss.setGlowing(true);

        // Armure custom - SANS DROP
        if (boss.getEquipment() != null) {
            boss.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            boss.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            boss.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));

            boss.getEquipment().setHelmetDropChance(0f);
            boss.getEquipment().setChestplateDropChance(0f);
            boss.getEquipment().setLeggingsDropChance(0f);
            boss.getEquipment().setBootsDropChance(0f);
            boss.getEquipment().setItemInMainHandDropChance(0f);
            boss.getEquipment().setItemInOffHandDropChance(0f);
        }

        // Tags pour identification de l'événement
        boss.addScoreboardTag("event_boss");
        boss.addScoreboardTag("event_" + id);
        boss.addScoreboardTag("dynamic_event_entity");

        // Ne pas persister au reboot
        boss.setPersistent(false);

        // Effet de spawn épique
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 2f, 0.7f);
        world.spawnParticle(Particle.EXPLOSION, location, 5, 2, 2, 2, 0);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 50, 2, 2, 2, 0.1);
        world.strikeLightningEffect(location);
    }

    /**
     * Crée la boss bar pour le boss
     */
    private void createBossBossBar() {
        bossBossBar = Bukkit.createBossBar(
            "§4§l" + bossName + " §c" + (int) bossCurrentHealth + "/" + (int) bossMaxHealth,
            BarColor.RED,
            BarStyle.SEGMENTED_10
        );
        bossBossBar.setProgress(1.0);
    }

    @Override
    public void tick() {
        tickCounter++;
        elapsedTicks += 10;

        World world = location.getWorld();
        if (world == null) return;

        // Vérifier si le boss est mort
        if (boss == null || !boss.isValid() || boss.isDead()) {
            onBossKilled();
            return;
        }

        // Mettre à jour la santé
        bossCurrentHealth = boss.getHealth();

        // Vérifier si arrivé à destination (échec) - via l'IA ou calcul direct
        boolean hasReachedDestination = false;
        if (bossAI != null) {
            hasReachedDestination = bossAI.hasReachedDestination();
        } else {
            double distToDest = safeDistance(boss.getLocation(), destination);
            hasReachedDestination = distToDest != Double.MAX_VALUE && distToDest < 10;
        }

        if (hasReachedDestination) {
            onBossEscaped();
            return;
        }

        // Mettre à jour les boss bars
        updateBossBars();

        // Gérer la visibilité des boss bars
        updateBossBarVisibility();

        // Note: L'IA (WanderingBossAI) gère automatiquement:
        // - Le mouvement vers la destination
        // - Le ciblage des joueurs
        // - Les capacités spéciales
        // - Le spawn d'adds
        // - Le mode enragé
        // - Les particules ambiantes
    }

    /**
     * Appelé quand le boss est tué
     */
    private void onBossKilled() {
        World world = location.getWorld();

        // Effets de mort
        if (world != null && boss != null) {
            Location deathLoc = boss.getLocation();

            world.playSound(deathLoc, Sound.ENTITY_WITHER_DEATH, 2f, 1f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 5, 2, 2, 2, 0);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, deathLoc, 100, 3, 3, 3, 0.3);

            // Drop de loot spécial
            dropBossLoot(deathLoc);
        }

        complete();
    }

    /**
     * Drop le loot du boss avec effet d'explosion spectaculaire
     */
    private void dropBossLoot(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Effets visuels d'explosion de loot
        world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 60, 1.5, 1.5, 1.5, 0.4);
        world.spawnParticle(Particle.FIREWORK, center, 30, 1, 1, 1, 0.2);

        // Loot garanti de haute qualité (4-8 items pour un boss)
        int itemCount = 4 + Math.min(4, zone.getId() / 12);

        for (int i = 0; i < itemCount; i++) {
            Rarity rarity;
            if (i == 0) {
                rarity = Rarity.LEGENDARY; // Premier item toujours légendaire
            } else if (i == 1) {
                rarity = Rarity.EPIC; // 1 item épique garanti
            } else if (random.nextDouble() < 0.35) {
                rarity = Rarity.EPIC;
            } else if (random.nextDouble() < 0.65) {
                rarity = Rarity.RARE;
            } else {
                rarity = Rarity.UNCOMMON;
            }

            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item == null) continue;

            // Spawn avec vélocité explosive
            Item droppedItem = world.dropItem(center, item);

            double angle = random.nextDouble() * Math.PI * 2;
            double upward = 0.4 + random.nextDouble() * 0.4;
            double outward = 0.3 + random.nextDouble() * 0.35;

            Vector velocity = new Vector(
                Math.cos(angle) * outward,
                upward,
                Math.sin(angle) * outward
            );
            droppedItem.setVelocity(velocity);

            // Appliquer effets visuels (glow + nom visible) - toujours, pas de condition
            droppedItem.setGlowing(true);
            plugin.getItemManager().applyGlowForRarity(droppedItem, rarity);

            // Utiliser l'API Adventure pour le nom
            if (item.hasItemMeta()) {
                var meta = item.getItemMeta();
                var displayName = meta.displayName();
                if (displayName != null) {
                    droppedItem.customName(displayName);
                    droppedItem.setCustomNameVisible(true);
                }
            }
        }

        // Consommables bonus avec explosion (1-2)
        if (plugin.getConsumableManager() != null) {
            int consumableCount = 1 + (zone.getId() >= 25 ? 1 : 0);
            for (int i = 0; i < consumableCount; i++) {
                Consumable consumable = plugin.getConsumableManager().generateConsumable(zone.getId(), 0.0);
                if (consumable == null) continue;

                ItemStack consumableItem = consumable.createItemStack();
                Item droppedConsumable = world.dropItem(center, consumableItem);

                double angle = random.nextDouble() * Math.PI * 2;
                double upward = 0.3 + random.nextDouble() * 0.3;
                double outward = 0.2 + random.nextDouble() * 0.25;

                droppedConsumable.setVelocity(new Vector(
                    Math.cos(angle) * outward,
                    upward,
                    Math.sin(angle) * outward
                ));

                // Glow et nom visible pour les consommables
                droppedConsumable.setGlowing(true);
                plugin.getItemManager().applyGlowForRarity(droppedConsumable, Rarity.UNCOMMON);

                if (consumableItem.hasItemMeta()) {
                    var meta = consumableItem.getItemMeta();
                    var displayName = meta.displayName();
                    if (displayName != null) {
                        droppedConsumable.customName(displayName);
                        droppedConsumable.setCustomNameVisible(true);
                    }
                }
            }
        }
    }

    /**
     * Appelé quand le boss s'échappe
     */
    private void onBossEscaped() {
        World world = boss.getLocation().getWorld();

        // Effet de fuite
        if (world != null) {
            world.playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.5f);
            world.spawnParticle(Particle.PORTAL, boss.getLocation(), 100, 2, 2, 2, 0.5);

            // Message de raillerie
            for (Player player : world.getNearbyEntities(boss.getLocation(), 100, 50, 100).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList()) {
                player.sendMessage("§c§l" + bossName + ": §7\"Vous êtes trop faibles... Je reviendrai!\"");
            }
        }

        // Supprimer le boss
        if (boss != null && boss.isValid()) {
            boss.remove();
        }

        fail();
    }

    /**
     * Met à jour les boss bars
     * OPTIMISÉ: Vérifie les null et utilise safeDistance
     */
    private void updateBossBars() {
        if (boss == null || !boss.isValid()) return;

        // Boss bar principale (progression vers destination)
        double distanceToDestination = safeDistance(boss.getLocation(), destination);
        if (distanceToDestination == Double.MAX_VALUE) {
            distanceToDestination = totalDistance; // Fallback
        }
        double progress = 1.0 - (distanceToDestination / totalDistance);
        updateBossBar(Math.max(0, Math.min(1, progress)),
            "- §e" + (int) distanceToDestination + "m restants");

        // Boss bar de santé du boss
        if (bossBossBar != null) {
            // Vérifier l'état enragé via l'IA
            boolean isEnraged = bossAI != null && bossAI.isEnraged();

            double healthPercent = bossCurrentHealth / bossMaxHealth;
            bossBossBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
            bossBossBar.setTitle("§4§l" + bossName +
                (isEnraged ? " §5[ENRAGÉ]" : "") +
                " §c" + (int) bossCurrentHealth + "/" + (int) bossMaxHealth);

            // Changer la couleur si enragé
            bossBossBar.setColor(isEnraged ? BarColor.PURPLE : BarColor.RED);
        }
    }

    /**
     * Met à jour la visibilité des boss bars
     * OPTIMISÉ: Vérifie les mondes et utilise safeDistance
     */
    private void updateBossBarVisibility() {
        if (boss == null || !boss.isValid() || bossBossBar == null) return;

        Location bossLoc = boss.getLocation();
        World bossWorld = bossLoc.getWorld();
        if (bossWorld == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Vérifier le monde
            if (!player.getWorld().equals(bossWorld)) {
                bossBossBar.removePlayer(player);
                continue;
            }

            double distance = safeDistance(player.getLocation(), bossLoc);
            if (distance != Double.MAX_VALUE && distance <= 60) {
                if (!bossBossBar.getPlayers().contains(player)) {
                    bossBossBar.addPlayer(player);
                }
            } else {
                bossBossBar.removePlayer(player);
            }
        }
    }

    @Override
    protected void distributeRewards() {
        // Grosses récompenses pour avoir tué le boss - formule exponentielle
        double zoneMultiplier = 1.0 + (zone.getId() * 0.12) + (Math.log10(zone.getId() + 1) * 0.6);
        int totalPoints = (int) (basePointsReward * zoneMultiplier);
        int totalXp = (int) (baseXpReward * zoneMultiplier);

        // Bonus si tué rapidement (avant 50% du chemin)
        boolean fastKill = false;
        if (boss != null && boss.isValid()) {
            double distanceToDestination = safeDistance(boss.getLocation(), destination);
            if (distanceToDestination != Double.MAX_VALUE && distanceToDestination > totalDistance * 0.5) {
                totalPoints = (int) (totalPoints * 1.5);
                totalXp = (int) (totalXp * 1.5);
                fastKill = true;
            }
        }

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, totalPoints);
                // XP via EconomyManager pour inclure l'XP de classe (30%)
                plugin.getEconomyManager().addXp(player, totalXp);

                // ============ TRACKER ÉVÉNEMENTS ============
                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.incrementStat("events_completed");
                    int eventsCompleted = (int) playerData.getStat("events_completed");

                    // Notifier le système de Parcours (Journey)
                    if (plugin.getJourneyListener() != null) {
                        plugin.getJourneyListener().onEventParticipation(player, eventsCompleted);
                    }

                    // Tracker missions
                    plugin.getMissionManager().updateProgress(player,
                        com.rinaorc.zombiez.progression.MissionManager.MissionTracker.EVENTS_PARTICIPATED, 1);

                    // Achievements
                    var achievementManager = plugin.getAchievementManager();
                    achievementManager.incrementProgress(player, "first_event", 1);
                    achievementManager.checkAndUnlock(player, "event_veteran", eventsCompleted);
                    achievementManager.checkAndUnlock(player, "event_champion", eventsCompleted);
                    achievementManager.checkAndUnlock(player, "event_legend", eventsCompleted);
                }

                player.sendMessage("");
                player.sendMessage("§a§l✓ BOSS VAINCU!");
                player.sendMessage("§7Vous avez terrassé §c" + bossName + "§7!");
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                if (fastKill) {
                    player.sendMessage("§6Bonus §ekill rapide §6appliqué!");
                }
                player.sendMessage("§7Loot déposé au sol!");
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Supprimer la boss bar
        if (bossBossBar != null) {
            bossBossBar.removeAll();
            bossBossBar = null;
        }

        // Supprimer le boss s'il existe encore
        if (boss != null && boss.isValid()) {
            boss.remove();
        }
        boss = null;

        // Nettoyer la destination
        destination = null;
    }

    @Override
    protected String getStartSubtitle() {
        return "Éliminez-le avant qu'il ne s'échappe!";
    }

    @Override
    public String getDebugInfo() {
        double distToDestination = 0;
        if (boss != null && boss.isValid()) {
            distToDestination = safeDistance(boss.getLocation(), destination);
            if (distToDestination == Double.MAX_VALUE) {
                distToDestination = -1; // Indication d'erreur
            }
        }
        boolean isEnraged = bossAI != null && bossAI.isEnraged();
        return String.format("Boss: %s | Health: %.0f/%.0f | DistToDest: %.0f | Enraged: %s",
            bossName, bossCurrentHealth, bossMaxHealth, distToDestination, isEnraged);
    }
}
