package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zombies.ZombieManager;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

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

    // Le boss
    @Getter
    private LivingEntity boss;
    private String bossName;
    private double bossMaxHealth;
    private double bossCurrentHealth;

    // Destination
    private Location destination;
    private double totalDistance;
    private double moveSpeed = 0.08;

    // Capacités
    private int specialAbilityTimer = 0;
    private int specialAbilityInterval = 15; // Secondes
    private int addSpawnTimer = 0;
    private int addSpawnInterval = 20; // Secondes

    // Boss bar spéciale
    private BossBar bossBossBar;

    // État
    private boolean isEnraged = false;
    private int tickCounter = 0;

    // Types de boss selon la zone
    private static final String[] BOSS_NAMES = {
        "Le Boucher Maudit",
        "L'Abomination",
        "Le Général Mort-Vivant",
        "Le Faucheur",
        "Le Colosse Putride"
    };

    public WanderingBossEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.WANDERING_BOSS, location, zone);

        // Choisir un nom de boss
        this.bossName = BOSS_NAMES[new Random().nextInt(BOSS_NAMES.length)];

        // Stats basées sur la zone
        this.bossMaxHealth = 500 + (zone.getId() * 50);
        this.bossCurrentHealth = bossMaxHealth;

        // Plus rapide et plus dangereux en zone avancée
        this.moveSpeed = 0.06 + (zone.getId() * 0.002);
        this.specialAbilityInterval = Math.max(8, 15 - zone.getId() / 10);
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
     * Spawn le boss
     */
    private void spawnBoss() {
        World world = location.getWorld();
        if (world == null) return;

        // Choisir le type de mob basé sur la zone
        EntityType bossType = zone.getId() < 20 ? EntityType.ZOMBIE :
            (zone.getId() < 35 ? EntityType.HUSK : EntityType.WITHER_SKELETON);

        boss = (LivingEntity) world.spawnEntity(location, bossType);

        // Nom personnalisé
        boss.setCustomName("§4§l" + bossName + " §c[BOSS]");
        boss.setCustomNameVisible(true);

        // Stats
        boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(bossMaxHealth);
        boss.setHealth(bossMaxHealth);
        boss.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0); // On gère le mouvement
        boss.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15 + zone.getId());

        // Effets visuels
        boss.setGlowing(true);
        boss.setFireTicks(0);

        // Taille si c'est un zombie
        if (boss instanceof Zombie zombie) {
            zombie.setBaby(false);
        }

        // Armure
        if (boss instanceof Zombie zombie) {
            zombie.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            zombie.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
        }

        // Marquer comme boss d'événement
        boss.addScoreboardTag("event_boss");
        boss.addScoreboardTag("event_" + id);

        // Effet de spawn
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 2f, 0.7f);
        world.spawnParticle(Particle.EXPLOSION, location, 5, 2, 2, 2, 0);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 50, 2, 2, 2, 0.1);

        // Strikle lightning
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

        // Enrage à 30% de vie
        if (!isEnraged && bossCurrentHealth < bossMaxHealth * 0.3) {
            triggerEnrage();
        }

        // Mouvement vers la destination
        moveBoss();

        // Capacités spéciales
        specialAbilityTimer++;
        if (specialAbilityTimer >= specialAbilityInterval * 2) {
            useSpecialAbility();
            specialAbilityTimer = 0;
        }

        // Spawn d'adds
        addSpawnTimer++;
        if (addSpawnTimer >= addSpawnInterval * 2) {
            spawnAdds();
            addSpawnTimer = 0;
        }

        // Vérifier si arrivé à destination (échec)
        double distToDest = safeDistance(boss.getLocation(), destination);
        if (distToDest != Double.MAX_VALUE && distToDest < 10) {
            onBossEscaped();
            return;
        }

        // Mettre à jour les boss bars
        updateBossBars();

        // Gérer la visibilité des boss bars
        updateBossBarVisibility();

        // Particules ambiantes
        if (tickCounter % 4 == 0) {
            Location bossLoc = boss.getLocation();
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, bossLoc.clone().add(0, 1, 0),
                5, 0.5, 0.5, 0.5, 0.02);

            if (isEnraged) {
                world.spawnParticle(Particle.ANGRY_VILLAGER, bossLoc.clone().add(0, 2, 0),
                    3, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /**
     * Déplace le boss vers sa destination
     */
    private void moveBoss() {
        if (boss == null || !boss.isValid()) return;

        Location bossLoc = boss.getLocation();
        Vector direction = destination.toVector().subtract(bossLoc.toVector()).normalize();

        // Vitesse augmentée si enragé
        double speed = isEnraged ? moveSpeed * 1.5 : moveSpeed;

        Location newLoc = bossLoc.clone().add(direction.multiply(speed));

        World world = bossLoc.getWorld();
        if (world != null) {
            int groundY = world.getHighestBlockYAt(newLoc);
            newLoc.setY(groundY + 1);
        }

        // Téléporter (mouvement smooth)
        boss.teleport(newLoc);

        // Faire face à la direction
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        boss.setRotation(yaw, 0);
    }

    /**
     * Active le mode enragé
     */
    private void triggerEnrage() {
        isEnraged = true;

        World world = boss.getLocation().getWorld();
        if (world == null) return;

        // Annonce
        for (Player player : world.getNearbyEntities(boss.getLocation(), 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§4§l⚠ " + bossName.toUpperCase() + " EST ENRAGÉ!", "§cIl devient plus dangereux!", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
        }

        // Effet visuel
        world.spawnParticle(Particle.EXPLOSION_EMITTER, boss.getLocation(), 3, 1, 1, 1, 0);

        // Boost de stats
        boss.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
            boss.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5
        );
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 99999, 1));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));

        // Changer la couleur de la boss bar
        bossBossBar.setColor(BarColor.PURPLE);
    }

    /**
     * Utilise une capacité spéciale
     */
    private void useSpecialAbility() {
        if (boss == null || !boss.isValid()) return;

        World world = boss.getLocation().getWorld();
        if (world == null) return;

        // Choisir une capacité
        int ability = new Random().nextInt(4);

        Location bossLoc = boss.getLocation();

        switch (ability) {
            case 0 -> { // Onde de choc
                for (Player player : world.getNearbyEntities(bossLoc, 8, 4, 8).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    Vector knockback = player.getLocation().toVector()
                        .subtract(bossLoc.toVector()).normalize().multiply(2).setY(0.8);
                    player.setVelocity(knockback);
                    player.damage(8 + zone.getId() / 5);
                    player.sendMessage("§c" + bossName + " §7vous repousse!");
                }
                world.playSound(bossLoc, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
                world.spawnParticle(Particle.EXPLOSION, bossLoc, 20, 3, 1, 3, 0);
            }
            case 1 -> { // Rugissement terrifiant
                for (Player player : world.getNearbyEntities(bossLoc, 15, 10, 15).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                    player.sendMessage("§c" + bossName + " §7vous terrorise!");
                }
                world.playSound(bossLoc, Sound.ENTITY_WARDEN_ROAR, 2f, 0.6f);
            }
            case 2 -> { // Frappe au sol
                for (Player player : world.getNearbyEntities(bossLoc, 6, 4, 6).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    player.damage(15 + zone.getId() / 3);
                    player.setVelocity(new Vector(0, 1, 0));
                }
                world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                world.spawnParticle(Particle.BLOCK, bossLoc, 50, 3, 0.5, 3, 0,
                    Material.DIRT.createBlockData());
            }
            case 3 -> { // Auto-régénération
                double healAmount = bossMaxHealth * 0.05; // 5% de regen
                boss.setHealth(Math.min(bossMaxHealth, boss.getHealth() + healAmount));
                world.playSound(bossLoc, Sound.ENTITY_WITCH_DRINK, 1f, 0.8f);
                world.spawnParticle(Particle.HEART, bossLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);

                for (Player player : world.getNearbyEntities(bossLoc, 30, 20, 30).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    player.sendMessage("§c" + bossName + " §7se régénère!");
                }
            }
        }
    }

    /**
     * Spawn des zombies supplémentaires
     */
    private void spawnAdds() {
        if (boss == null || !boss.isValid()) return;

        World world = boss.getLocation().getWorld();
        if (world == null) return;

        int addCount = 2 + zone.getId() / 15;
        Location bossLoc = boss.getLocation();

        for (int i = 0; i < addCount; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 3 + Math.random() * 3;
            double x = bossLoc.getX() + Math.cos(angle) * distance;
            double z = bossLoc.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);
            plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, zone.getId());
        }

        world.playSound(bossLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 0.8f);
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
     * Drop le loot du boss
     */
    private void dropBossLoot(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Loot garanti de haute qualité
        int itemCount = 3 + zone.getId() / 10;

        for (int i = 0; i < itemCount; i++) {
            Rarity rarity = i == 0 ? Rarity.LEGENDARY : // Premier item toujours légendaire
                (Math.random() < 0.3 ? Rarity.EPIC : Rarity.RARE);

            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item != null) {
                world.dropItemNaturally(location, item);
            }
        }

        // Consommables bonus
        if (plugin.getConsumableManager() != null) {
            for (int i = 0; i < 5; i++) {
                ItemStack consumable = plugin.getConsumableManager().generateRandomConsumable(zone.getId());
                if (consumable != null) {
                    world.dropItemNaturally(location, consumable);
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
            double healthPercent = bossCurrentHealth / bossMaxHealth;
            bossBossBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
            bossBossBar.setTitle("§4§l" + bossName +
                (isEnraged ? " §5[ENRAGÉ]" : "") +
                " §c" + (int) bossCurrentHealth + "/" + (int) bossMaxHealth);
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

                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
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
        }

        // Supprimer le boss s'il existe encore
        if (boss != null && boss.isValid()) {
            boss.remove();
        }
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
        return String.format("Boss: %s | Health: %.0f/%.0f | DistToDest: %.0f | Enraged: %s",
            bossName, bossCurrentHealth, bossMaxHealth, distToDestination, isEnraged);
    }
}
