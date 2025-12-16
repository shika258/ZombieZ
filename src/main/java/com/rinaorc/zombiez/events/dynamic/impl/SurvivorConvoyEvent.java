package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Événement Convoi de Survivants
 *
 * Déroulement:
 * 1. Un groupe de survivants (villageois) apparaît
 * 2. Ils doivent être escortés vers un point de destination
 * 3. Des zombies attaquent régulièrement le convoi
 * 4. Les survivants suivent les joueurs proches
 * 5. Récompense basée sur le nombre de survivants arrivés vivants
 */
public class SurvivorConvoyEvent extends DynamicEvent {

    // Survivants
    @Getter
    private final List<Villager> survivors = new ArrayList<>();
    private int initialSurvivorCount;
    private int survivorsAlive;

    // Destination
    private Location destination;
    private double totalDistance;
    private ArmorStand destinationMarker;
    private BukkitTask destinationParticleTask; // Pour cleanup propre

    // Mouvement
    private double moveSpeed = 0.15;
    private int stuckTimer = 0;
    private Location lastPosition;

    // Attaques
    private int attackWaveTimer = 0;
    private int attackWaveInterval = 30; // Secondes entre les vagues d'attaque
    private int zombiesPerAttack = 3;

    // Progress
    private boolean escorting = false;
    private int escortersNearby = 0;

    public SurvivorConvoyEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.SURVIVOR_CONVOY, location, zone);

        // Configuration basée sur la zone
        this.initialSurvivorCount = 3 + zone.getId() / 20; // 3-5 survivants
        this.zombiesPerAttack = 3 + zone.getId() / 10;
        this.attackWaveInterval = Math.max(15, 30 - zone.getId() / 5); // Plus fréquent en zone avancée
    }

    @Override
    protected void startMainLogic() {
        // Trouver la destination
        calculateDestination();

        // Spawn les survivants
        spawnSurvivors();

        // Créer le marqueur de destination
        createDestinationMarker();

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
        }.runTaskTimer(plugin, 0L, 10L); // Tick plus fréquent pour le mouvement
    }

    /**
     * Calcule la destination du convoi
     */
    private void calculateDestination() {
        World world = location.getWorld();
        if (world == null) return;

        // Destination: vers le nord (progression) à 80-120 blocs
        double distance = 80 + Math.random() * 40;

        // Trouver un point valide
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = -Math.PI / 2 + (Math.random() - 0.5) * Math.PI / 3; // Principalement nord
            double x = location.getX() + Math.cos(angle) * distance;
            double z = location.getZ() + Math.sin(angle) * distance;

            Location testLoc = new Location(world, x, 0, z);

            // Vérifier que c'est dans la map
            if (!plugin.getZoneManager().isInMapBounds(testLoc)) continue;

            // Vérifier que ce n'est pas une zone safe
            Zone destZone = plugin.getZoneManager().getZoneAt(testLoc);
            if (destZone != null && destZone.isSafeZone()) continue;

            // Trouver le sol
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
            destination = new Location(world, x, y, z);
            break;
        }

        // Fallback si aucune destination valide
        if (destination == null) {
            destination = location.clone().add(0, 0, -100); // 100 blocs au nord
            destination.setY(world.getHighestBlockYAt(destination));
        }

        totalDistance = location.distance(destination);
    }

    /**
     * Spawn les survivants
     */
    private void spawnSurvivors() {
        World world = location.getWorld();
        if (world == null) return;

        Villager.Profession[] professions = {
            Villager.Profession.FARMER,
            Villager.Profession.LIBRARIAN,
            Villager.Profession.CLERIC,
            Villager.Profession.TOOLSMITH,
            Villager.Profession.WEAPONSMITH
        };

        String[] names = {"Marie", "Jean", "Pierre", "Sophie", "Lucas", "Emma", "Hugo", "Léa"};

        for (int i = 0; i < initialSurvivorCount; i++) {
            // Position légèrement dispersée
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetZ = (Math.random() - 0.5) * 3;
            Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

            Villager villager = (Villager) world.spawnEntity(spawnLoc, EntityType.VILLAGER);
            villager.setProfession(professions[i % professions.length]);
            villager.setVillagerLevel(1);
            villager.setCustomName("§e" + names[i % names.length] + " §7[Survivant]");
            villager.setCustomNameVisible(true);
            villager.setAI(false); // Désactiver l'IA par défaut, on gère le mouvement
            villager.setInvulnerable(false);

            // Marquer comme survivant du convoi
            villager.addScoreboardTag("convoy_survivor");
            villager.addScoreboardTag("event_" + id);

            survivors.add(villager);
        }

        survivorsAlive = survivors.size();
        lastPosition = location.clone();

        // Annoncer
        world.playSound(location, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
    }

    /**
     * Crée le marqueur de destination
     * OPTIMISÉ: Stocke la tâche de particules pour cleanup propre
     */
    private void createDestinationMarker() {
        World world = destination.getWorld();
        if (world == null) return;

        destinationMarker = (ArmorStand) world.spawnEntity(destination.clone().add(0, 2, 0), EntityType.ARMOR_STAND);
        destinationMarker.setVisible(false);
        destinationMarker.setGravity(false);
        destinationMarker.setMarker(true);
        destinationMarker.setCustomName("§a§l⬇ REFUGE ⬇");
        destinationMarker.setCustomNameVisible(true);
        destinationMarker.setGlowing(true);

        // Particules au sol - stocker la référence pour cleanup
        destinationParticleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || destinationMarker == null || !destinationMarker.isValid()) {
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.HAPPY_VILLAGER, destination.clone().add(0, 1, 0),
                    5, 1, 0.5, 1, 0);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void tick() {
        elapsedTicks += 10;

        World world = location.getWorld();
        if (world == null) return;

        // Vérifier les survivants morts
        checkSurvivors();

        if (survivorsAlive <= 0) {
            fail();
            return;
        }

        // Compter les escorteurs
        countEscorters();

        // Mouvement des survivants
        if (escortersNearby > 0) {
            escorting = true;
            moveSurvivors();
            attackWaveTimer++;
        } else {
            escorting = false;
        }

        // Vagues d'attaque
        if (attackWaveTimer >= attackWaveInterval * 2) { // *2 car tick à 10 ticks
            spawnAttackWave();
            attackWaveTimer = 0;
        }

        // Vérifier si arrivé à destination
        checkArrival();

        // Mettre à jour la boss bar
        double progress = 1.0 - (getAverageDistanceToDestination() / totalDistance);
        progress = Math.max(0, Math.min(1, progress));
        updateBossBar(progress, "- §e" + survivorsAlive + "/" + initialSurvivorCount +
            " survivants §7| §a" + (int) (progress * 100) + "%");

        // Particules de suivi
        if (elapsedTicks % 40 == 0) {
            for (Villager v : survivors) {
                if (v.isValid() && !v.isDead()) {
                    world.spawnParticle(Particle.HEART, v.getLocation().add(0, 2, 0),
                        1, 0.2, 0.2, 0.2, 0);
                }
            }
        }
    }

    /**
     * Vérifie l'état des survivants
     */
    private void checkSurvivors() {
        Iterator<Villager> iterator = survivors.iterator();
        while (iterator.hasNext()) {
            Villager v = iterator.next();
            if (!v.isValid() || v.isDead()) {
                iterator.remove();
                survivorsAlive = survivors.size();

                // Annoncer la mort
                World world = location.getWorld();
                if (world != null) {
                    for (Player player : world.getNearbyEntities(location, 50, 30, 50).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .toList()) {
                        player.sendMessage("§c§l☠ §7Un survivant est mort! §e" + survivorsAlive + "/" + initialSurvivorCount + " restants");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 0.8f, 1f);
                    }
                }
            }
        }
    }

    /**
     * Compte les escorteurs proches du convoi
     */
    private void countEscorters() {
        escortersNearby = 0;
        Location convoyCenter = getConvoyCenter();
        if (convoyCenter == null) return;

        World world = convoyCenter.getWorld();
        if (world == null) return;

        for (Player player : world.getNearbyEntities(convoyCenter, 15, 10, 15).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            escortersNearby++;
            addParticipant(player);
        }
    }

    /**
     * Obtient le centre du convoi
     */
    private Location getConvoyCenter() {
        if (survivors.isEmpty()) return null;

        double x = 0, y = 0, z = 0;
        int count = 0;

        for (Villager v : survivors) {
            if (v.isValid() && !v.isDead()) {
                Location loc = v.getLocation();
                x += loc.getX();
                y += loc.getY();
                z += loc.getZ();
                count++;
            }
        }

        if (count == 0) return null;
        return new Location(survivors.get(0).getWorld(), x / count, y / count, z / count);
    }

    /**
     * Déplace les survivants vers la destination
     */
    private void moveSurvivors() {
        Location convoyCenter = getConvoyCenter();
        if (convoyCenter == null) return;

        // Direction vers la destination
        Vector direction = destination.toVector().subtract(convoyCenter.toVector()).normalize();

        // Vitesse bonus avec plusieurs escorteurs
        double speedBonus = 1.0 + (escortersNearby - 1) * 0.1;
        double actualSpeed = moveSpeed * speedBonus;

        World world = convoyCenter.getWorld();
        if (world == null) return;

        for (Villager v : survivors) {
            if (!v.isValid() || v.isDead()) continue;

            Location currentLoc = v.getLocation();
            Location targetLoc = currentLoc.clone().add(direction.clone().multiply(actualSpeed));

            // Trouver le sol
            int groundY = world.getHighestBlockYAt(targetLoc);
            targetLoc.setY(groundY + 1);

            // Vérifier que c'est traversable
            if (targetLoc.getBlock().getType().isSolid()) {
                // Essayer de contourner
                continue;
            }

            // Téléporter (mouvement smooth)
            v.teleport(targetLoc);

            // Faire face à la destination
            v.setRotation(getYaw(direction), 0);
        }

        // Vérifier si bloqué - utiliser safeDistance pour éviter les exceptions
        double distanceFromLast = safeDistance(convoyCenter, lastPosition);
        if (distanceFromLast < 0.5 && distanceFromLast != Double.MAX_VALUE) {
            stuckTimer++;
            if (stuckTimer > 100) {
                // Téléporter légèrement vers l'avant
                for (Villager v : survivors) {
                    if (v.isValid() && !v.isDead()) {
                        Location unstuckLoc = v.getLocation().add(direction.clone().multiply(2));
                        unstuckLoc.setY(world.getHighestBlockYAt(unstuckLoc) + 1);
                        v.teleport(unstuckLoc);
                    }
                }
                stuckTimer = 0;
            }
        } else {
            stuckTimer = 0;
            lastPosition = convoyCenter.clone();
        }
    }

    private float getYaw(Vector direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
    }

    /**
     * Spawn une vague d'attaque
     */
    private void spawnAttackWave() {
        Location convoyCenter = getConvoyCenter();
        if (convoyCenter == null) return;

        World world = convoyCenter.getWorld();
        if (world == null) return;

        // Annoncer
        for (Player player : world.getNearbyEntities(convoyCenter, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§c§l⚠ §7Des zombies attaquent le convoi!");
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
        }

        // Spawn les zombies autour du convoi
        for (int i = 0; i < zombiesPerAttack; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 15 + Math.random() * 10;
            double x = convoyCenter.getX() + Math.cos(angle) * distance;
            double z = convoyCenter.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);
            plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, zone.getId());
        }
    }

    /**
     * Vérifie si le convoi est arrivé
     */
    private void checkArrival() {
        double avgDistance = getAverageDistanceToDestination();
        if (avgDistance < 10) {
            // Arrivé!
            complete();
        }
    }

    /**
     * Obtient la distance moyenne des survivants à la destination
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions entre mondes
     */
    private double getAverageDistanceToDestination() {
        if (survivors.isEmpty()) return Double.MAX_VALUE;

        double total = 0;
        int count = 0;

        for (Villager v : survivors) {
            if (v.isValid() && !v.isDead()) {
                double dist = safeDistance(v.getLocation(), destination);
                if (dist != Double.MAX_VALUE) {
                    total += dist;
                    count++;
                }
            }
        }

        return count > 0 ? total / count : Double.MAX_VALUE;
    }

    @Override
    protected void distributeRewards() {
        // Bonus basé sur le nombre de survivants
        double survivalRate = (double) survivorsAlive / initialSurvivorCount;
        int bonusMultiplier = (int) (survivalRate * 2); // 0-2x bonus

        int totalPoints = (int) (basePointsReward * (1 + bonusMultiplier * 0.5));
        int totalXp = (int) (baseXpReward * (1 + bonusMultiplier * 0.5));

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, totalPoints);

                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
                }

                player.sendMessage("");
                player.sendMessage("§a§l✓ CONVOI ARRIVÉ!");
                player.sendMessage("§7Survivants sauvés: §e" + survivorsAlive + "/" + initialSurvivorCount +
                    " §7(" + (int) (survivalRate * 100) + "%)");
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Annuler la tâche de particules
        if (destinationParticleTask != null && !destinationParticleTask.isCancelled()) {
            destinationParticleTask.cancel();
        }

        // Supprimer le marqueur de destination
        if (destinationMarker != null && destinationMarker.isValid()) {
            destinationMarker.remove();
        }

        // Supprimer les survivants
        for (Villager v : survivors) {
            if (v != null && v.isValid()) {
                v.remove();
            }
        }
        survivors.clear();
    }

    @Override
    protected String getStartSubtitle() {
        return "Escortez " + initialSurvivorCount + " survivants vers le refuge!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Survivors: %d/%d | Distance: %.0f/%.0f | Escorters: %d | Escorting: %s",
            survivorsAlive, initialSurvivorCount, getAverageDistanceToDestination(), totalDistance,
            escortersNearby, escorting);
    }
}
