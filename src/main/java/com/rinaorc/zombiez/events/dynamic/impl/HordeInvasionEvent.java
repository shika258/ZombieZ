package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
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
    private int waveIntervalSeconds = 3;  // Temps entre chaque vague (réduit à 3s pour dynamisme)
    private int secondsUntilNextWave;

    // Zombies - Plafonné à 80 max pour vraie horde
    private static final int MAX_TOTAL_ZOMBIES = 80;
    private int baseZombiesPerWave = 15;  // Plus de zombies pour effet de horde
    private int zombiesThisWave = 0;
    private int zombiesKilledThisWave = 0;
    private int totalZombiesKilled = 0;
    private int totalZombiesToKill;  // Objectif total de kills (max 80)

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

    public HordeInvasionEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.HORDE_INVASION, location, zone);

        // Configuration basée sur la zone - Intense et dynamique
        this.totalWaves = 3 + Math.min(zone.getId() / 5, 2);  // 3-5 vagues max
        this.baseZombiesPerWave = 15 + zone.getId() / 2;  // 15-25 zombies par vague de base
        this.secondsUntilNextWave = waveIntervalSeconds;

        // Calculer l'objectif total de kills avec plafond à 80
        this.totalZombiesToKill = 0;
        for (int w = 1; w <= totalWaves; w++) {
            totalZombiesToKill += baseZombiesPerWave + (w * 3);  // +3 zombies par vague
        }
        // Plafonner à 80 zombies max
        this.totalZombiesToKill = Math.min(this.totalZombiesToKill, MAX_TOTAL_ZOMBIES);

        // Durée courte pour garder l'intensité
        this.maxDuration = 20 * 60 * (totalWaves + 1); // 4-6 minutes max
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

            // Tag pour cleanup au redemarrage
            display.addScoreboardTag("dynamic_event_entity");
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

            // Tag pour cleanup au redemarrage
            display.addScoreboardTag("dynamic_event_entity");
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

            // Tag pour cleanup au redemarrage
            display.addScoreboardTag("dynamic_event_entity");
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

        // Calcul dynamique: vraie horde avec scaling agressif
        // +3 zombies par vague, +2 par défenseur supplémentaire
        int defenderBonus = Math.max(0, defendersInArea - 1) * 2;
        zombiesThisWave = baseZombiesPerWave + (currentWave * 3) + defenderBonus;

        // Plafonner pour éviter de dépasser le max total
        int remainingQuota = MAX_TOTAL_ZOMBIES - totalZombiesKilled;
        zombiesThisWave = Math.min(zombiesThisWave, remainingQuota);

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
     * Spawn les zombies de la vague - BURST MASSIF pour vraie sensation de horde
     * Spawn direct comme TemporalRiftEvent (sans passer par SpawnSystem)
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

        // Pré-calculer les positions de spawn pour un burst instantané
        List<Location> spawnLocations = new ArrayList<>();
        for (int i = 0; i < zombiesThisWave; i++) {
            // Distribution en cercle autour du périmètre - PROCHE pour pression immédiate
            double angle = (Math.PI * 2 * i / zombiesThisWave) + (Math.random() * 0.5 - 0.25);
            double distance = defenseRadius - 5 + Math.random() * 15; // 20-35 blocs du centre
            double x = location.getX() + Math.cos(angle) * distance;
            double z = location.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
            spawnLocations.add(new Location(world, x, y, z));
        }

        // BURST SPAWN: Spawn massif en 2 ticks pour effet de horde
        final double finalZombieHealth = zombieHealth;
        waveSpawnTask = new BukkitRunnable() {
            int tick = 0;
            int spawnIndex = 0;
            final int zombiesPerTick = Math.max(zombiesThisWave / 2, 10); // Moitié par tick, min 10

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                // Spawn un gros paquet de zombies
                int toSpawn = Math.min(zombiesPerTick, spawnLocations.size() - spawnIndex);
                for (int i = 0; i < toSpawn; i++) {
                    Location spawnLoc = spawnLocations.get(spawnIndex + i);
                    spawnHordeZombie(spawnLoc, finalZombieHealth);
                }

                // Effet visuel et sonore MASSIF pour le burst
                if (tick == 0) {
                    // Premier burst - effet dramatique au centre
                    world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 2, 0), 3, 2, 1, 2, 0);
                    world.spawnParticle(Particle.SOUL, location.clone().add(0, 1, 0), 50, 15, 2, 15, 0.05);
                    world.playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
                    world.playSound(location, Sound.ENTITY_ZOMBIE_AMBIENT, 2f, 0.5f);
                }

                // Particules autour des points de spawn
                for (int i = 0; i < Math.min(10, toSpawn); i++) {
                    Location spawnLoc = spawnLocations.get(spawnIndex + i);
                    world.spawnParticle(Particle.SMOKE, spawnLoc.clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.02);
                    world.spawnParticle(Particle.FLAME, spawnLoc, 3, 0.3, 0.3, 0.3, 0.01);
                }

                spawnIndex += toSpawn;
                tick++;

                // Terminer après avoir spawné tous les zombies
                if (spawnIndex >= spawnLocations.size()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Tick toutes les 2 ticks = burst ultra rapide
    }

    /**
     * Spawn un zombie de horde custom (style TemporalRiftEvent)
     */
    private void spawnHordeZombie(Location spawnLoc, double zombieHealth) {
        Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        hordeZombies.add(zombie.getUniqueId());

        // Configuration du zombie de horde
        zombie.setCustomName(createHordeZombieName((int) zombieHealth, (int) zombieHealth));
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);
        zombie.setShouldBurnInDay(false);

        // Stats adaptées à la zone et vague
        zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombieHealth);
        zombie.setHealth(zombieHealth);
        zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25 + (zone.getId() * 0.003) + (currentWave * 0.01));
        zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(4.0 + (zone.getId() * 0.4) + (currentWave * 0.5));

        // Armure en cuir rouge foncé (style horde)
        zombie.getEquipment().setHelmet(createHordeArmor(Material.LEATHER_HELMET));
        zombie.getEquipment().setChestplate(createHordeArmor(Material.LEATHER_CHESTPLATE));
        zombie.getEquipment().setLeggings(createHordeArmor(Material.LEATHER_LEGGINGS));
        zombie.getEquipment().setBoots(createHordeArmor(Material.LEATHER_BOOTS));
        zombie.getEquipment().setHelmetDropChance(0f);
        zombie.getEquipment().setChestplateDropChance(0f);
        zombie.getEquipment().setLeggingsDropChance(0f);
        zombie.getEquipment().setBootsDropChance(0f);

        // Tags pour identification
        zombie.addScoreboardTag("dynamic_event_entity");
        zombie.addScoreboardTag("horde_invasion");
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
     * Crée le nom du zombie de horde avec affichage des HP
     */
    private String createHordeZombieName(int currentHealth, int maxHealth) {
        double healthPercent = (double) currentHealth / maxHealth;
        String healthColor;
        if (healthPercent > 0.66) {
            healthColor = "§a";
        } else if (healthPercent > 0.33) {
            healthColor = "§e";
        } else {
            healthColor = "§c";
        }
        return "§4💀 Zombie de Horde " + healthColor + currentHealth + "§7/§a" + maxHealth + " §c❤";
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
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
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
