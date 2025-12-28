package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Événement Invasion de Horde
 *
 * Déroulement:
 * 1. Les joueurs doivent défendre une position pendant un temps donné
 * 2. Des vagues de zombies de plus en plus fortes arrivent
 * 3. Le nombre de défenseurs affecte la difficulté
 * 4. Récompenses basées sur le nombre de vagues survivées
 */
public class HordeInvasionEvent extends DynamicEvent {

    // Configuration
    private int totalWaves;
    @Getter
    private int currentWave = 0;
    private int waveIntervalSeconds = 5;  // Temps entre chaque vague
    private int secondsUntilNextWave;

    // Zombies - Plafonné à 70 max
    private static final int MAX_TOTAL_ZOMBIES = 70;
    private static final int MIN_ZOMBIES_PER_WAVE = 8;
    private static final int MAX_ZOMBIES_PER_WAVE = 16;
    private int zombiesThisWave = 0;
    private int zombiesKilledThisWave = 0;
    private int totalZombiesKilled = 0;
    private int totalZombiesToKill;  // Objectif total de kills (max 70)

    // Défense
    private int defendersInArea = 0;
    private int defenseRadius = 25;

    // Marqueur visuel avec TextDisplay (scalable)
    private TextDisplay waveMarker;
    private TextDisplay centerMarker;
    private TextDisplay killCounterMarker;

    // Tâches planifiées (pour cleanup)
    private BukkitTask particleTask;
    private BukkitTask waveSpawnTask;

    // Statistiques
    private boolean waveClear = true;

    // Tracking des zombies de la horde
    private final Set<UUID> hordeZombies = new HashSet<>();

    // Configuration des zombies (style TemporalRift)
    private static final double BASE_ZOMBIE_HEALTH = 40.0;
    private static final double HEALTH_PER_ZONE = 10.0;
    private static final Color HORDE_COLOR = Color.fromRGB(139, 0, 0); // Rouge foncé

    // Random pour le loot
    private final Random random = new Random();

    public HordeInvasionEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.HORDE_INVASION, location, zone);

        // Configuration basée sur la zone - 5 vagues de 8-16 zombies
        this.totalWaves = 5;
        this.secondsUntilNextWave = waveIntervalSeconds;

        // Total plafonné à 70 zombies
        this.totalZombiesToKill = MAX_TOTAL_ZOMBIES;

        // Durée: 5 minutes max
        this.maxDuration = 20 * 60 * 5;
    }

    @Override
    protected void startMainLogic() {
        // Créer les marqueurs visuels
        createMarkers();

        // Démarrer la première vague après un court délai (dynamique!)
        secondsUntilNextWave = 5; // 5 secondes avant la première vague

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
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Crée les marqueurs visuels du centre de défense avec TextDisplay scalable
     */
    private void createMarkers() {
        World world = location.getWorld();
        if (world == null) return;

        // Marqueur central - Grand et visible
        centerMarker = world.spawn(location.clone().add(0, 3.5, 0), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            display.text(Component.text("💀 INVASION DE HORDE 💀", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(2.5f, 2.5f, 2.5f), // Grande taille!
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage + ne pas persister
            display.addScoreboardTag("dynamic_event_entity");
            display.setPersistent(false);
        });

        // Marqueur de vague - Sous le titre principal
        waveMarker = world.spawn(location.clone().add(0, 2.5, 0), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(80, 0, 0, 0));
            display.text(Component.text("⏳ Préparation...", NamedTextColor.YELLOW));
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.8f, 1.8f, 1.8f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage + ne pas persister
            display.addScoreboardTag("dynamic_event_entity");
            display.setPersistent(false);
        });

        // Marqueur de compteur de kills - Encore plus bas
        killCounterMarker = world.spawn(location.clone().add(0, 1.5, 0), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(60, 0, 0, 0));
            display.text(Component.text("☠ 0/" + totalZombiesToKill + " zombies tués", NamedTextColor.GRAY));
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.5f, 1.5f, 1.5f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage + ne pas persister
            display.addScoreboardTag("dynamic_event_entity");
            display.setPersistent(false);
        });

        // Particules de zone
        showDefenseZone();
    }

    /**
     * Affiche la zone de défense avec des particules
     * OPTIMISÉ: Stocke la tâche pour cleanup propre
     */
    private void showDefenseZone() {
        World world = location.getWorld();
        if (world == null) return;

        particleTask = new BukkitRunnable() {
            int angle = 0;

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                // Cercle de particules
                for (int i = 0; i < 360; i += 15) {
                    double radians = Math.toRadians(i + angle);
                    double x = location.getX() + Math.cos(radians) * defenseRadius;
                    double z = location.getZ() + Math.sin(radians) * defenseRadius;
                    Location particleLoc = new Location(world, x, location.getY() + 0.5, z);

                    Particle.DustOptions dust = new Particle.DustOptions(
                        currentWave > 0 ? Color.RED : Color.YELLOW, 1);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
                }
                angle += 5;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public void tick() {
        elapsedTicks += 20;

        World world = location.getWorld();
        if (world == null) return;

        // Compter les défenseurs
        countDefenders();

        // Gérer le timing des vagues
        if (waveClear) {
            secondsUntilNextWave--;

            if (secondsUntilNextWave <= 0) {
                if (currentWave >= totalWaves) {
                    // Toutes les vagues complétées!
                    complete();
                    return;
                }
                startNextWave();
            } else {
                // Mise à jour du marqueur TextDisplay
                if (waveMarker != null && waveMarker.isValid()) {
                    waveMarker.text(Component.text("⏳ Vague " + (currentWave + 1) + " dans ", NamedTextColor.YELLOW)
                        .append(Component.text(secondsUntilNextWave + "s", NamedTextColor.RED, TextDecoration.BOLD)));
                }
            }
        } else {
            // Vérifier si la vague est terminée
            checkWaveComplete();

            // MISE À JOUR EN TEMPS RÉEL du marqueur de vague pendant le combat
            if (waveMarker != null && waveMarker.isValid()) {
                NamedTextColor progressColor = zombiesKilledThisWave > zombiesThisWave / 2 ? NamedTextColor.GREEN : NamedTextColor.GOLD;
                waveMarker.text(Component.text("⚔ VAGUE " + currentWave + " ⚔", NamedTextColor.RED, TextDecoration.BOLD)
                    .appendNewline()
                    .append(Component.text(zombiesKilledThisWave + "/" + zombiesThisWave + " tués", progressColor, TextDecoration.BOLD)));
            }
        }

        // Mettre à jour le compteur de kills total
        if (killCounterMarker != null && killCounterMarker.isValid()) {
            NamedTextColor killColor = totalZombiesKilled > totalZombiesToKill / 2 ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            killCounterMarker.text(Component.text("☠ " + totalZombiesKilled + "/" + totalZombiesToKill + " zombies tués", killColor));
        }

        // Mettre à jour la boss bar
        double progress = (double) currentWave / totalWaves;
        String status = waveClear ? "§eProchaine vague: " + secondsUntilNextWave + "s" :
            "§cVague " + currentWave + "/" + totalWaves + " - " + zombiesKilledThisWave + "/" + zombiesThisWave + " tués";
        updateBossBar(progress, status);

        // Particules ambient pendant les vagues - Plus intenses
        if (!waveClear && elapsedTicks % 20 == 0) {
            world.spawnParticle(Particle.SMOKE, location.clone().add(0, 2, 0),
                15, 4, 2, 4, 0.03);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, location.clone().add(0, 1, 0),
                5, 2, 0.5, 2, 0.01);
        }
    }

    /**
     * Compte les défenseurs dans la zone
     */
    private void countDefenders() {
        World world = location.getWorld();
        if (world == null) return;

        defendersInArea = 0;
        for (Player player : world.getNearbyEntities(location, defenseRadius, 20, defenseRadius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            defendersInArea++;
            addParticipant(player);
        }

        // Mettre à jour le marqueur central TextDisplay
        if (centerMarker != null && centerMarker.isValid()) {
            NamedTextColor countColor = defendersInArea > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
            centerMarker.text(Component.text("💀 INVASION DE HORDE 💀", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text(defendersInArea + " Défenseur(s)", countColor, TextDecoration.BOLD)));
        }
    }

    /**
     * Démarre la prochaine vague
     */
    private void startNextWave() {
        currentWave++;
        waveClear = false;
        zombiesKilledThisWave = 0;

        // Calcul: 8-16 zombies par vague, avec léger bonus par défenseur
        int defenderBonus = Math.max(0, defendersInArea - 1) * 2;
        int baseCount = MIN_ZOMBIES_PER_WAVE + (currentWave * 2) + defenderBonus;
        zombiesThisWave = Math.min(baseCount, MAX_ZOMBIES_PER_WAVE);

        // Plafonner pour éviter de dépasser le max total de 70
        int remainingQuota = MAX_TOTAL_ZOMBIES - totalZombiesKilled;
        zombiesThisWave = Math.min(zombiesThisWave, remainingQuota);

        // Minimum 1 zombie si quota restant
        if (remainingQuota > 0 && zombiesThisWave < 1) {
            zombiesThisWave = 1;
        }

        World world = location.getWorld();
        if (world == null) return;

        // Annoncer la vague avec effet dramatique
        for (Player player : world.getNearbyEntities(location, 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle(
                "§c§lVAGUE " + currentWave + "/" + totalWaves,
                "§7" + zombiesThisWave + " zombies approchent!",
                10, 40, 10
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.5f);
        }

        // Mettre à jour le marqueur TextDisplay - Animation visuelle
        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.text(Component.text("⚔ VAGUE " + currentWave + " ⚔", NamedTextColor.RED, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text(zombiesKilledThisWave + "/" + zombiesThisWave + " tués", NamedTextColor.GOLD)));
            // Animation de scale pour l'impact
            waveMarker.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(2.2f, 2.2f, 2.2f),
                new AxisAngle4f(0, 0, 0, 1)
            ));
        }

        // Spawn les zombies en plusieurs fois - Plus agressif
        spawnWaveZombies();
    }

    /**
     * Spawn les zombies de la vague - BURST de 8-16 zombies d'un coup
     */
    private void spawnWaveZombies() {
        World world = location.getWorld();
        if (world == null) return;

        // Annuler la tâche précédente si elle existe encore
        if (waveSpawnTask != null && !waveSpawnTask.isCancelled()) {
            waveSpawnTask.cancel();
        }

        // Calculer les HP selon la zone et la vague
        double zombieHealth = BASE_ZOMBIE_HEALTH + (zone.getId() * HEALTH_PER_ZONE) + (currentWave * 5);

        // Pré-calculer les positions de spawn en cercle autour du centre
        List<Location> spawnLocations = new ArrayList<>();
        for (int i = 0; i < zombiesThisWave; i++) {
            double angle = (Math.PI * 2 * i / zombiesThisWave) + (Math.random() * 0.3 - 0.15);
            // Distance: 20-30 blocs du centre
            double distance = 20 + Math.random() * 10;
            double x = location.getX() + Math.cos(angle) * distance;
            double z = location.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
            spawnLocations.add(new Location(world, x, y, z));
        }

        // Effet visuel et sonore au début de la vague
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 2, 0), 2, 2, 1, 2, 0);
        world.spawnParticle(Particle.SOUL, location.clone().add(0, 1, 0), 30, 10, 2, 10, 0.05);
        world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.8f);

        // SPAWN TOUS LES ZOMBIES D'UN COUP (synchrone pour garantir le spawn)
        final double finalZombieHealth = zombieHealth;
        int spawned = 0;
        for (Location spawnLoc : spawnLocations) {
            if (spawnHordeZombie(spawnLoc, finalZombieHealth)) {
                spawned++;
                // Particules de spawn
                world.spawnParticle(Particle.SMOKE, spawnLoc.clone().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
            }
        }

        plugin.log(java.util.logging.Level.INFO, "[HordeInvasion] Spawned " + spawned + "/" + zombiesThisWave + " zombies for wave " + currentWave);
    }

    /**
     * Spawn un zombie de horde via ZombieManager pour bénéficier du système de dégâts ZombieZ
     * Le niveau est calculé à partir de la zone et de la vague courante
     * @return true si le spawn a réussi
     */
    private boolean spawnHordeZombie(Location spawnLoc, double zombieHealth) {
        // Calculer le niveau du zombie basé sur la zone et la vague
        int zombieLevel = zone.getId() + currentWave;

        // Spawn via ZombieManager pour bénéficier du système ZombieZ complet
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(java.util.logging.Level.WARNING, "[HordeInvasion] ZombieManager is null!");
            return false;
        }

        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.HORDE_ZOMBIE, spawnLoc, zombieLevel);

        // Si le spawn a échoué (limite atteinte, etc.), ne pas continuer
        if (activeZombie == null) {
            plugin.log(java.util.logging.Level.WARNING, "[HordeInvasion] Failed to spawn zombie via ZombieManager");
            return false;
        }

        // Récupérer l'entité spawnée
        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (entity == null || !(entity instanceof LivingEntity living)) {
            plugin.log(java.util.logging.Level.WARNING, "[HordeInvasion] Entity not found after spawn");
            return false;
        }

        // Ajouter au tracking de la horde
        hordeZombies.add(entity.getUniqueId());

        // Appliquer l'armure custom en cuir rouge foncé (style horde)
        if (living instanceof Zombie zombie && zombie.getEquipment() != null) {
            zombie.getEquipment().setHelmet(createHordeArmor(Material.LEATHER_HELMET));
            zombie.getEquipment().setChestplate(createHordeArmor(Material.LEATHER_CHESTPLATE));
            zombie.getEquipment().setLeggings(createHordeArmor(Material.LEATHER_LEGGINGS));
            zombie.getEquipment().setBoots(createHordeArmor(Material.LEATHER_BOOTS));
            zombie.getEquipment().setHelmetDropChance(0f);
            zombie.getEquipment().setChestplateDropChance(0f);
            zombie.getEquipment().setLeggingsDropChance(0f);
            zombie.getEquipment().setBootsDropChance(0f);
        }

        // Tags pour identification de l'événement dynamique
        entity.addScoreboardTag("dynamic_event_entity");
        entity.addScoreboardTag("horde_invasion");

        // Ne pas persister au reboot (évite les entités orphelines)
        entity.setPersistent(false);

        return true;
    }

    /**
     * Crée une pièce d'armure en cuir colorée (style horde)
     */
    private ItemStack createHordeArmor(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(HORDE_COLOR);
        item.setItemMeta(meta);
        return item;
    }


    /**
     * Vérifie si la vague est terminée
     */
    private void checkWaveComplete() {
        // La vague est terminée quand tous les zombies sont tués
        // On compte les zombies tués via le listener

        if (zombiesKilledThisWave >= zombiesThisWave) {
            onWaveComplete();
        }
    }

    /**
     * Appelé quand une vague est terminée
     */
    private void onWaveComplete() {
        waveClear = true;
        secondsUntilNextWave = waveIntervalSeconds;

        World world = location.getWorld();
        if (world == null) return;

        // Annoncer avec plus d'impact
        for (Player player : world.getNearbyEntities(location, 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {

            if (currentWave >= totalWaves) {
                player.sendTitle(
                    "§a§l✓ VICTOIRE!",
                    "§7Toutes les vagues repoussées! §e" + totalZombiesKilled + " zombies éliminés!",
                    10, 60, 20
                );
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                player.sendTitle(
                    "§a§l✓ Vague " + currentWave + "/" + totalWaves + " repoussée!",
                    "§7Prochaine vague dans " + secondsUntilNextWave + "s",
                    10, 40, 10
                );
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        }

        // Bonus de points pour la vague - Augmenté
        int waveBonus = 35 * currentWave;
        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, waveBonus);
                player.sendMessage("§a+§e" + waveBonus + " Points §7(Bonus vague " + currentWave + ")");
            }
        }

        // Mettre à jour le marqueur TextDisplay
        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.text(Component.text("✓ Vague " + currentWave + " terminée!", NamedTextColor.GREEN, TextDecoration.BOLD));
            waveMarker.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.8f, 1.8f, 1.8f),
                new AxisAngle4f(0, 0, 0, 1)
            ));
        }
    }

    /**
     * Appelé quand un zombie est tué dans la zone
     * (Appelé par le listener)
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions
     */
    public void onZombieKilled(Location killLocation) {
        if (!active || waveClear) return;

        // Vérifier que le kill est dans la zone (avec validation du monde)
        double distance = safeDistance(killLocation, location);
        if (distance <= defenseRadius + 20) {
            zombiesKilledThisWave++;
            totalZombiesKilled++;
        }
    }

    /**
     * Vérifie si une location est dans la zone de défense
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions
     */
    public boolean isInDefenseZone(Location loc) {
        double distance = safeDistance(loc, location);
        return distance != Double.MAX_VALUE && distance <= defenseRadius + 20;
    }

    @Override
    protected void distributeRewards() {
        // Bonus basé sur les vagues complétées
        int wavesCompleted = waveClear ? currentWave : currentWave - 1;
        int bonusMultiplier = wavesCompleted;

        int totalPoints = basePointsReward + (zone.getId() * 10) + (bonusMultiplier * 50);
        int totalXp = baseXpReward + (zone.getId() * 5) + (bonusMultiplier * 25);

        // EXPLOSION DE LOOT au centre de la zone de défense!
        explodeLoot(location.clone().add(0, 1, 0), wavesCompleted);

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, totalPoints);

                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
                }

                player.sendMessage("");
                player.sendMessage("§a§l✓ INVASION REPOUSSÉE!");
                player.sendMessage("§7Vagues complétées: §e" + wavesCompleted + "/" + totalWaves);
                player.sendMessage("§7Zombies tués: §e" + totalZombiesKilled);
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                player.sendMessage("§7Loot déposé au centre!");
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    /**
     * Fait exploser le loot dans toutes les directions à la victoire
     */
    private void explodeLoot(Location center, int wavesCompleted) {
        World world = center.getWorld();
        if (world == null) return;

        // Calculer le nombre de loot basé sur les vagues complétées (4-8 items)
        int lootCount = 4 + Math.min(4, wavesCompleted);

        // Effet sonore de récompense
        world.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 40, 1, 1, 1, 0.3);
        world.spawnParticle(Particle.FIREWORK, center, 20, 0.5, 0.5, 0.5, 0.1);

        for (int i = 0; i < lootCount; i++) {
            // Déterminer la rareté (meilleure avec plus de vagues)
            double roll = random.nextDouble() * 100;
            double bonusRarity = wavesCompleted * 2; // +2% par vague pour meilleure rareté
            Rarity rarity;
            if (roll < (45 - bonusRarity)) {
                rarity = Rarity.COMMON;
            } else if (roll < (70 - bonusRarity / 2)) {
                rarity = Rarity.UNCOMMON;
            } else if (roll < 88) {
                rarity = Rarity.RARE;
            } else if (roll < 96) {
                rarity = Rarity.EPIC;
            } else {
                rarity = Rarity.LEGENDARY;
            }

            // Générer l'item
            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item == null) continue;

            // Spawn avec vélocité explosive
            Item droppedItem = world.dropItem(center, item);

            double angle = random.nextDouble() * Math.PI * 2;
            double upward = 0.35 + random.nextDouble() * 0.35;
            double outward = 0.25 + random.nextDouble() * 0.3;

            Vector velocity = new Vector(
                Math.cos(angle) * outward,
                upward,
                Math.sin(angle) * outward
            );
            droppedItem.setVelocity(velocity);

            // Appliquer effets visuels (glow + nom visible) - toujours
            droppedItem.setGlowing(true);
            plugin.getItemManager().applyGlowForRarity(droppedItem, rarity);

            if (item.hasItemMeta()) {
                var meta = item.getItemMeta();
                var displayName = meta.displayName();
                if (displayName != null) {
                    droppedItem.customName(displayName);
                    droppedItem.setCustomNameVisible(true);
                }
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Annuler la tâche de particules
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
            particleTask = null;
        }

        // Annuler la tâche de spawn de zombies
        if (waveSpawnTask != null && !waveSpawnTask.isCancelled()) {
            waveSpawnTask.cancel();
            waveSpawnTask = null;
        }

        // Supprimer tous les zombies de horde restants
        for (UUID zombieId : hordeZombies) {
            var entity = plugin.getServer().getEntity(zombieId);
            if (entity != null && entity.isValid() && !entity.isDead()) {
                entity.remove();
            }
        }
        hordeZombies.clear();

        // Supprimer les marqueurs TextDisplay
        if (centerMarker != null && centerMarker.isValid()) {
            centerMarker.remove();
        }
        centerMarker = null;

        if (waveMarker != null && waveMarker.isValid()) {
            waveMarker.remove();
        }
        waveMarker = null;

        if (killCounterMarker != null && killCounterMarker.isValid()) {
            killCounterMarker.remove();
        }
        killCounterMarker = null;
    }

    @Override
    protected String getStartSubtitle() {
        return "Défendez votre position contre " + totalWaves + " vagues!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Wave: %d/%d | Zombies: %d/%d | Defenders: %d | NextWave: %ds",
            currentWave, totalWaves, zombiesKilledThisWave, zombiesThisWave,
            defendersInArea, secondsUntilNextWave);
    }
}
