package com.rinaorc.zombiez.events.dynamic;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.impl.*;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Gestionnaire des événements dynamiques
 *
 * Fonctionnalités:
 * - Spawn automatique d'événements près des joueurs
 * - Probabilité augmentée si plusieurs joueurs dans la même zone
 * - Exclusion des zones safe (spawn)
 * - Gestion du cycle de vie des événements
 * - Configuration complète via events.yml
 */
public class DynamicEventManager {

    private final ZombieZPlugin plugin;
    private final Random random;

    // Événements actifs
    @Getter
    private final Map<String, DynamicEvent> activeEvents = new ConcurrentHashMap<>();

    // Configuration
    @Getter
    private boolean enabled = true;
    private int minEventInterval = 20 * 60 * 10;     // 10 minutes minimum entre événements
    private int maxEventInterval = 20 * 60 * 20;     // 20 minutes maximum
    private int maxConcurrentEvents = 3;             // Max événements simultanés
    private int minPlayersForEvents = 1;             // Min joueurs en ligne
    private int spawnRadiusMin = 30;                 // Distance min du joueur
    private int spawnRadiusMax = 60;                 // Distance max du joueur
    private double playerCountMultiplier = 0.15;     // Bonus de chance par joueur supplémentaire dans la zone
    private Map<DynamicEventType, Boolean> enabledTypes = new EnumMap<>(DynamicEventType.class);
    private Map<DynamicEventType, Double> typeWeightOverrides = new EnumMap<>(DynamicEventType.class);

    // État
    private long lastEventTime = 0;
    private long nextEventTime = 0;
    private BukkitTask schedulerTask;
    private BukkitTask tickTask;

    // Cooldowns par type d'événement (évite la répétition)
    private final Map<DynamicEventType, Long> typeCooldowns = new EnumMap<>(DynamicEventType.class);
    private long typeCooldownDuration = 1000 * 60 * 5; // 5 minutes entre même type

    // Statistiques
    @Getter
    private int totalEventsSpawned = 0;
    @Getter
    private int totalEventsCompleted = 0;
    @Getter
    private int totalEventsFailed = 0;

    public DynamicEventManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();

        // Activer tous les types par défaut
        for (DynamicEventType type : DynamicEventType.values()) {
            enabledTypes.put(type, true);
        }
    }

    /**
     * Charge la configuration depuis events.yml
     */
    public void loadConfig(FileConfiguration config) {
        if (config == null) {
            plugin.log(Level.WARNING, "§eConfiguration events.yml non trouvée, utilisation des valeurs par défaut");
            return;
        }

        // Paramètres globaux
        enabled = config.getBoolean("dynamic-events.enabled", true);
        minEventInterval = config.getInt("dynamic-events.min-interval-seconds", 600) * 20;
        maxEventInterval = config.getInt("dynamic-events.max-interval-seconds", 1200) * 20;
        maxConcurrentEvents = config.getInt("dynamic-events.max-concurrent", 3);
        minPlayersForEvents = config.getInt("dynamic-events.min-players", 1);
        spawnRadiusMin = config.getInt("dynamic-events.spawn-radius.min", 30);
        spawnRadiusMax = config.getInt("dynamic-events.spawn-radius.max", 60);
        playerCountMultiplier = config.getDouble("dynamic-events.player-count-multiplier", 0.15);

        // Configuration par type d'événement
        ConfigurationSection typesSection = config.getConfigurationSection("dynamic-events.types");
        if (typesSection != null) {
            for (DynamicEventType type : DynamicEventType.values()) {
                ConfigurationSection typeConfig = typesSection.getConfigurationSection(type.getConfigKey());
                if (typeConfig != null) {
                    enabledTypes.put(type, typeConfig.getBoolean("enabled", true));
                    if (typeConfig.contains("weight-override")) {
                        typeWeightOverrides.put(type, typeConfig.getDouble("weight-override"));
                    }
                }
            }
        }

        plugin.log(Level.INFO, "§a✓ Configuration des événements dynamiques chargée");
    }

    /**
     * Démarre le système d'événements dynamiques
     */
    public void start() {
        if (!enabled) {
            plugin.log(Level.INFO, "§7Événements dynamiques désactivés");
            return;
        }

        // Calculer le prochain événement
        scheduleNextEvent();

        // Démarrer le scheduler
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndSpawnEvent();
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // Check toutes les 30 secondes

        // Démarrer le tick des événements actifs
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickActiveEvents();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Tick chaque seconde

        plugin.log(Level.INFO, "§a✓ Système d'événements dynamiques démarré");
    }

    /**
     * Arrête le système
     */
    public void shutdown() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
        if (tickTask != null) {
            tickTask.cancel();
        }

        // Forcer l'arrêt de tous les événements
        for (DynamicEvent event : activeEvents.values()) {
            event.forceStop();
        }
        activeEvents.clear();

        plugin.log(Level.INFO, "§7Système d'événements dynamiques arrêté");
    }

    /**
     * Planifie le prochain événement
     */
    private void scheduleNextEvent() {
        int interval = minEventInterval + random.nextInt(maxEventInterval - minEventInterval);
        nextEventTime = System.currentTimeMillis() + (interval / 20 * 1000L);
    }

    /**
     * Vérifie et spawn un événement si les conditions sont remplies
     */
    private void checkAndSpawnEvent() {
        // Vérifications de base
        if (!enabled) return;
        if (activeEvents.size() >= maxConcurrentEvents) return;
        if (Bukkit.getOnlinePlayers().size() < minPlayersForEvents) return;
        if (System.currentTimeMillis() < nextEventTime) return;

        // Tenter de spawner un événement
        if (trySpawnEvent()) {
            lastEventTime = System.currentTimeMillis();
            scheduleNextEvent();
        }
    }

    /**
     * Tente de spawner un événement
     * @return true si un événement a été spawné
     */
    private boolean trySpawnEvent() {
        // Collecter les données par zone
        Map<Zone, List<Player>> playersByZone = collectPlayersByZone();
        if (playersByZone.isEmpty()) return false;

        // Calculer les poids par zone (plus de joueurs = plus de chance)
        Map<Zone, Double> zoneWeights = calculateZoneWeights(playersByZone);

        // Sélectionner une zone
        Zone selectedZone = selectWeightedZone(zoneWeights);
        if (selectedZone == null) return false;

        // Sélectionner un joueur dans cette zone
        List<Player> playersInZone = playersByZone.get(selectedZone);
        if (playersInZone == null || playersInZone.isEmpty()) return false;

        Player targetPlayer = playersInZone.get(random.nextInt(playersInZone.size()));

        // Sélectionner un type d'événement
        DynamicEventType eventType = selectEventType(playersInZone.size());
        if (eventType == null) return false;

        // Trouver une position de spawn
        Location spawnLocation = findSpawnLocation(targetPlayer.getLocation());
        if (spawnLocation == null) return false;

        // Créer et démarrer l'événement
        DynamicEvent event = createEvent(eventType, spawnLocation, selectedZone);
        if (event == null) return false;

        // Enregistrer le cooldown pour ce type
        recordEventTypeUsed(eventType);

        // Enregistrer et démarrer
        activeEvents.put(event.getId(), event);
        event.start();
        totalEventsSpawned++;

        plugin.log(Level.INFO, "§a✓ Événement " + eventType.getDisplayName() +
            " spawné en Zone " + selectedZone.getId() + " près de " + targetPlayer.getName());

        return true;
    }

    /**
     * Collecte les joueurs par zone (exclut les zones safe)
     */
    private Map<Zone, List<Player>> collectPlayersByZone() {
        Map<Zone, List<Player>> result = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());

            // Exclure les zones safe
            if (zone == null || zone.isSafeZone() || zone.getId() == 0) continue;

            result.computeIfAbsent(zone, k -> new ArrayList<>()).add(player);
        }

        return result;
    }

    /**
     * Calcule les poids de chaque zone basé sur le nombre de joueurs
     */
    private Map<Zone, Double> calculateZoneWeights(Map<Zone, List<Player>> playersByZone) {
        Map<Zone, Double> weights = new HashMap<>();

        for (Map.Entry<Zone, List<Player>> entry : playersByZone.entrySet()) {
            Zone zone = entry.getKey();
            int playerCount = entry.getValue().size();

            // Poids de base = 1.0
            // Chaque joueur supplémentaire ajoute playerCountMultiplier (0.15 par défaut)
            // Donc 5 joueurs = 1.0 + (4 * 0.15) = 1.6x la chance
            double weight = 1.0 + ((playerCount - 1) * playerCountMultiplier);

            // Bonus pour les zones plus avancées (encourage l'exploration)
            weight *= (1.0 + (zone.getId() * 0.02));

            weights.put(zone, weight);
        }

        return weights;
    }

    /**
     * Sélectionne une zone basée sur les poids
     */
    private Zone selectWeightedZone(Map<Zone, Double> weights) {
        if (weights.isEmpty()) return null;

        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = random.nextDouble() * totalWeight;

        double cumulative = 0;
        for (Map.Entry<Zone, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }

        // Fallback au premier
        return weights.keySet().iterator().next();
    }

    /**
     * Sélectionne un type d'événement basé sur les poids
     * OPTIMISÉ: Inclut un système de cooldown pour éviter la répétition
     */
    private DynamicEventType selectEventType(int playerCount) {
        long now = System.currentTimeMillis();

        // Filtrer les types activés et hors cooldown
        List<DynamicEventType> validTypes = Arrays.stream(DynamicEventType.values())
            .filter(t -> enabledTypes.getOrDefault(t, true))
            .filter(t -> playerCount >= t.getMinPlayersRecommended())
            .filter(t -> !isTypeOnCooldown(t, now))
            .collect(Collectors.toList());

        if (validTypes.isEmpty()) {
            // Si tous les types sont en cooldown, ignorer les cooldowns
            validTypes = Arrays.stream(DynamicEventType.values())
                .filter(t -> enabledTypes.getOrDefault(t, true))
                .filter(t -> playerCount >= t.getMinPlayersRecommended())
                .collect(Collectors.toList());
        }

        if (validTypes.isEmpty()) return null;

        // Calculer les poids (réduire le poids des types récemment utilisés)
        Map<DynamicEventType, Double> weights = new HashMap<>();
        for (DynamicEventType type : validTypes) {
            double weight = typeWeightOverrides.getOrDefault(type, (double) type.getSpawnWeight());

            // Réduire le poids si le type a été utilisé récemment (même si hors cooldown)
            Long lastUsed = typeCooldowns.get(type);
            if (lastUsed != null) {
                long timeSince = now - lastUsed;
                // Réduction graduelle: 50% à 0% sur 10 minutes
                if (timeSince < 1000 * 60 * 10) {
                    double reduction = 0.5 * (1.0 - (timeSince / (1000.0 * 60 * 10)));
                    weight *= (1.0 - reduction);
                }
            }

            weights.put(type, Math.max(0.1, weight)); // Minimum 0.1 pour garder une chance
        }

        // Sélection pondérée
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = random.nextDouble() * totalWeight;

        double cumulative = 0;
        for (Map.Entry<DynamicEventType, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }

        return validTypes.get(0);
    }

    /**
     * Vérifie si un type d'événement est en cooldown
     */
    private boolean isTypeOnCooldown(DynamicEventType type, long now) {
        Long lastUsed = typeCooldowns.get(type);
        if (lastUsed == null) return false;
        return (now - lastUsed) < typeCooldownDuration;
    }

    /**
     * Enregistre l'utilisation d'un type d'événement
     */
    private void recordEventTypeUsed(DynamicEventType type) {
        typeCooldowns.put(type, System.currentTimeMillis());
    }

    /**
     * Trouve une position de spawn valide près d'un joueur
     */
    private Location findSpawnLocation(Location playerLocation) {
        World world = playerLocation.getWorld();
        if (world == null) return null;

        // Essayer plusieurs fois de trouver un bon spot
        for (int attempt = 0; attempt < 10; attempt++) {
            // Angle aléatoire
            double angle = random.nextDouble() * Math.PI * 2;

            // Distance aléatoire dans la plage
            double distance = spawnRadiusMin + random.nextDouble() * (spawnRadiusMax - spawnRadiusMin);

            // Calculer les coordonnées
            double x = playerLocation.getX() + Math.cos(angle) * distance;
            double z = playerLocation.getZ() + Math.sin(angle) * distance;

            // Trouver le sol
            Location checkLoc = new Location(world, x, 0, z);

            // Vérifier que c'est dans les limites de la map
            if (!plugin.getZoneManager().isInMapBounds(checkLoc)) continue;

            // Vérifier que ce n'est pas une zone safe
            Zone zone = plugin.getZoneManager().getZoneAt(checkLoc);
            if (zone == null || zone.isSafeZone() || zone.getId() == 0) continue;

            // Trouver la hauteur du sol
            int highestY = world.getHighestBlockYAt((int) x, (int) z);

            // Limiter Y à +5 au-dessus du joueur pour éviter les spawns sur les toits/arbres
            if (highestY > playerLocation.getY() + 5) continue;

            checkLoc.setY(highestY + 1);

            // Vérifier que le bloc du sol est solide
            Block groundBlock = checkLoc.clone().add(0, -1, 0).getBlock();
            if (!groundBlock.getType().isSolid()) continue;

            // Vérifier qu'il y a de l'espace au-dessus
            Block headBlock = checkLoc.clone().add(0, 1, 0).getBlock();
            if (headBlock.getType().isSolid()) continue;

            // Position valide!
            return checkLoc;
        }

        return null;
    }

    /**
     * Crée un événement du type spécifié
     */
    private DynamicEvent createEvent(DynamicEventType type, Location location, Zone zone) {
        return switch (type) {
            case AIRDROP -> new AirdropEvent(plugin, location, zone);
            case ZOMBIE_NEST -> new ZombieNestEvent(plugin, location, zone);
            case HORDE_INVASION -> new HordeInvasionEvent(plugin, location, zone);
            case SURVIVOR_CONVOY -> new SurvivorConvoyEvent(plugin, location, zone);
            case WANDERING_BOSS -> new WanderingBossEvent(plugin, location, zone);
        };
    }

    /**
     * Tick tous les événements actifs
     */
    private void tickActiveEvents() {
        Iterator<Map.Entry<String, DynamicEvent>> iterator = activeEvents.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, DynamicEvent> entry = iterator.next();
            DynamicEvent event = entry.getValue();

            if (!event.isValid()) {
                // Événement terminé
                if (event.isCompleted()) {
                    totalEventsCompleted++;
                } else if (event.isFailed()) {
                    totalEventsFailed++;
                }
                iterator.remove();
                continue;
            }

            // Tick l'événement
            try {
                event.tick();
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Erreur lors du tick de l'événement " + event.getId() + ": " + e.getMessage());
                e.printStackTrace();
                event.forceStop();
                iterator.remove();
            }
        }
    }

    // ==================== API PUBLIQUE ====================

    /**
     * Force le spawn d'un événement
     */
    public DynamicEvent forceSpawnEvent(DynamicEventType type, Location location) {
        Zone zone = plugin.getZoneManager().getZoneAt(location);
        if (zone == null || zone.isSafeZone()) {
            return null;
        }

        DynamicEvent event = createEvent(type, location, zone);
        if (event == null) return null;

        activeEvents.put(event.getId(), event);
        event.start();
        totalEventsSpawned++;

        return event;
    }

    /**
     * Force le spawn d'un événement près d'un joueur
     */
    public DynamicEvent forceSpawnEventNear(DynamicEventType type, Player player) {
        Location spawnLoc = findSpawnLocation(player.getLocation());
        if (spawnLoc == null) return null;

        return forceSpawnEvent(type, spawnLoc);
    }

    /**
     * Force le spawn d'un événement aléatoire
     */
    public DynamicEvent forceSpawnRandomEvent() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return null;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Player target = players.get(random.nextInt(players.size()));

        // Vérifier que le joueur n'est pas en zone safe
        Zone zone = plugin.getZoneManager().getZoneAt(target.getLocation());
        if (zone == null || zone.isSafeZone() || zone.getId() == 0) {
            return null;
        }

        DynamicEventType type = selectEventType(1);
        if (type == null) return null;

        return forceSpawnEventNear(type, target);
    }

    /**
     * Arrête un événement par son ID
     */
    public boolean stopEvent(String eventId) {
        DynamicEvent event = activeEvents.remove(eventId);
        if (event != null) {
            event.forceStop();
            return true;
        }
        return false;
    }

    /**
     * Arrête tous les événements
     */
    public void stopAllEvents() {
        for (DynamicEvent event : activeEvents.values()) {
            event.forceStop();
        }
        activeEvents.clear();
    }

    /**
     * Obtient un événement par son ID
     */
    public DynamicEvent getEvent(String eventId) {
        return activeEvents.get(eventId);
    }

    /**
     * Obtient les statistiques du système
     */
    public String getStats() {
        long nextIn = Math.max(0, (nextEventTime - System.currentTimeMillis()) / 1000);
        return String.format(
            "§7Événements actifs: §e%d/%d §7| Prochain: §e%ds §7| Total: §a%d §7(§2%d✓ §c%d✗§7)",
            activeEvents.size(), maxConcurrentEvents, nextIn,
            totalEventsSpawned, totalEventsCompleted, totalEventsFailed
        );
    }

    /**
     * Obtient les informations de debug
     */
    public List<String> getDebugInfo() {
        List<String> lines = new ArrayList<>();
        lines.add("§6=== Dynamic Event Manager ===");
        lines.add("§7Enabled: " + (enabled ? "§aOui" : "§cNon"));
        lines.add("§7Active Events: §e" + activeEvents.size() + "/" + maxConcurrentEvents);
        lines.add("§7Next Event In: §e" + Math.max(0, (nextEventTime - System.currentTimeMillis()) / 1000) + "s");
        lines.add("§7Stats: §a" + totalEventsCompleted + "✓ §c" + totalEventsFailed + "✗ §7/" + totalEventsSpawned + " total");
        lines.add("");

        if (!activeEvents.isEmpty()) {
            lines.add("§6Active Events:");
            for (DynamicEvent event : activeEvents.values()) {
                lines.add("  §7- " + event.getType().getColor() + event.getType().getDisplayName() +
                    " §7[" + event.getId() + "] Zone " + event.getZone().getId() +
                    " §7(" + event.getRemainingTimeSeconds() + "s)");
            }
        }

        return lines;
    }

    /**
     * Active/désactive un type d'événement
     */
    public void setTypeEnabled(DynamicEventType type, boolean enabled) {
        enabledTypes.put(type, enabled);
    }

    /**
     * Vérifie si un type est activé
     */
    public boolean isTypeEnabled(DynamicEventType type) {
        return enabledTypes.getOrDefault(type, true);
    }

    /**
     * Définit l'intervalle entre événements
     */
    public void setEventInterval(int minSeconds, int maxSeconds) {
        this.minEventInterval = minSeconds * 20;
        this.maxEventInterval = maxSeconds * 20;
        scheduleNextEvent();
    }
}
