package com.rinaorc.zombiez.events.micro;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.impl.*;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire des micro-evenements
 *
 * Les micro-evenements sont des evenements rapides et personnels:
 * - Un seul joueur concerne (pas global)
 * - Duree courte (20-60 secondes)
 * - Spawn frequent (toutes les 3-5 minutes par joueur)
 * - Mecanique simple et immediate
 *
 * Objectif: Maintenir l'engagement entre les gros evenements
 */
public class MicroEventManager {

    private final ZombieZPlugin plugin;
    private final Random random;

    // Micro-evenements actifs par joueur (1 seul a la fois par joueur)
    @Getter
    private final Map<UUID, MicroEvent> activeEvents = new ConcurrentHashMap<>();

    // Cooldown par joueur avant le prochain micro-event
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    // Configuration
    @Getter
    private boolean enabled = true;
    private int minInterval = 180;          // 3 minutes minimum entre micro-events
    private int maxInterval = 300;          // 5 minutes maximum
    private int spawnRadiusMin = 20;        // Distance min du joueur
    private int spawnRadiusMax = 40;        // Distance max du joueur

    // Anti-spam: Limites par zone (scaling inversé + cap)
    private int maxMicroEventsPerZone = 8;  // Cap absolu par zone
    private boolean useInverseScaling = true; // Active le scaling inversé

    // Records de Course Mortelle par zone
    @Getter
    private final Map<Integer, DeathRaceRecord> deathRaceRecords = new ConcurrentHashMap<>();

    // Tache de spawn
    private BukkitTask spawnTask;

    // Statistiques
    @Getter
    private int totalEventsSpawned = 0;
    @Getter
    private int totalEventsCompleted = 0;
    @Getter
    private int totalEventsFailed = 0;

    public MicroEventManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * Charge la configuration depuis events.yml
     */
    public void loadConfig(org.bukkit.configuration.file.FileConfiguration config) {
        if (config == null) return;

        // Paramètres anti-spam (zone limits)
        useInverseScaling = config.getBoolean("dynamic-events.zone-limits.inverse-scaling-enabled", true);
        maxMicroEventsPerZone = config.getInt("dynamic-events.zone-limits.max-micro-events-per-zone", 8);

        plugin.log(java.util.logging.Level.INFO, "§a✓ Configuration micro-events chargée (max/zone: " +
            maxMicroEventsPerZone + ", scaling inversé: " + useInverseScaling + ")");
    }

    /**
     * Demarre le systeme de micro-evenements
     */
    public void start() {
        if (!enabled) {
            plugin.log(Level.INFO, "§7Micro-evenements desactives");
            return;
        }

        // Nettoyer les entites orphelines des sessions precedentes (crash, restart, etc.)
        cleanupOrphanedEntities();

        // Verifier periodiquement si des joueurs peuvent avoir un micro-event
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndSpawnEvents();
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // Check toutes les 30 secondes

        plugin.log(Level.INFO, "§a✓ Systeme de micro-evenements demarre");
    }

    /**
     * Nettoie les entites orphelines des micro-events (TextDisplay, zombies, etc.)
     * qui peuvent persister apres un crash ou un arret brusque du serveur
     */
    private void cleanupOrphanedEntities() {
        int removedCount = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // Verifier si l'entite a le tag des micro-events
                if (entity.getScoreboardTags().contains("micro_event_entity")) {
                    entity.remove();
                    removedCount++;
                }
            }
        }

        if (removedCount > 0) {
            plugin.log(Level.INFO, "§e⚠ Nettoyage: " + removedCount + " entite(s) orpheline(s) de micro-events supprimee(s)");
        }
    }

    /**
     * Arrete le systeme
     */
    public void shutdown() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }

        // Forcer l'arret de tous les evenements
        for (MicroEvent event : activeEvents.values()) {
            event.forceStop();
        }
        activeEvents.clear();

        plugin.log(Level.INFO, "§7Systeme de micro-evenements arrete");
    }

    /**
     * Verifie et spawn des micro-evenements pour les joueurs eligibles
     * Utilise un scaling inversé pour éviter la surcharge quand trop de joueurs sont dans la même zone
     */
    private void checkAndSpawnEvents() {
        if (!enabled) return;

        long now = System.currentTimeMillis();

        // Pré-calculer les joueurs par zone pour le scaling inversé
        Map<Integer, List<Player>> playersByZone = new HashMap<>();
        Map<Integer, Integer> activeEventsByZone = new HashMap<>();

        // Grouper les joueurs par zone
        for (Player player : Bukkit.getOnlinePlayers()) {
            Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
            if (zone != null && !zone.isSafeZone() && zone.getId() != 0) {
                playersByZone.computeIfAbsent(zone.getId(), k -> new ArrayList<>()).add(player);
            }
        }

        // Compter les micro-events actifs par zone
        for (MicroEvent event : activeEvents.values()) {
            if (event.isValid() && event.getZone() != null) {
                int zoneId = event.getZone().getId();
                activeEventsByZone.merge(zoneId, 1, Integer::sum);
            }
        }

        // Traiter chaque zone avec scaling inversé
        for (Map.Entry<Integer, List<Player>> entry : playersByZone.entrySet()) {
            int zoneId = entry.getKey();
            List<Player> playersInZone = entry.getValue();
            int activeInZone = activeEventsByZone.getOrDefault(zoneId, 0);

            // Vérifier le cap absolu par zone
            if (activeInZone >= maxMicroEventsPerZone) {
                continue; // Zone saturée
            }

            // Calculer combien de joueurs peuvent tenter de spawn
            // Scaling inversé: moins de chance individuelle quand plus de joueurs
            int playersCount = playersInZone.size();
            int maxCandidates = calculateMaxCandidates(playersCount, activeInZone);

            // Mélanger pour équité
            List<Player> shuffledPlayers = new ArrayList<>(playersInZone);
            Collections.shuffle(shuffledPlayers, random);

            int spawned = 0;
            for (Player player : shuffledPlayers) {
                if (spawned >= maxCandidates) break;
                if (activeInZone + spawned >= maxMicroEventsPerZone) break;

                UUID playerId = player.getUniqueId();

                // Skip si le joueur a deja un micro-event actif
                if (activeEvents.containsKey(playerId)) continue;

                // Skip si en cooldown
                Long cooldownEnd = playerCooldowns.get(playerId);
                if (cooldownEnd != null && now < cooldownEnd) continue;

                // Skip si un gros evenement est actif pres du joueur
                if (hasNearbyDynamicEvent(player)) continue;

                // Appliquer la chance de spawn avec scaling inversé
                if (useInverseScaling && playersCount > 1) {
                    double spawnChance = 1.0 / Math.sqrt(playersCount);
                    if (random.nextDouble() > spawnChance) continue;
                }

                Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
                if (zone == null) continue;

                // Spawn un micro-event
                if (trySpawnEventForPlayer(player, zone)) {
                    spawned++;
                }
            }
        }
    }

    /**
     * Calcule le nombre maximum de candidats pour spawn dans une zone
     * Basé sur le scaling inversé (racine carrée)
     */
    private int calculateMaxCandidates(int playersInZone, int activeEventsInZone) {
        if (!useInverseScaling) {
            return playersInZone; // Pas de limite si scaling désactivé
        }

        // Formule: sqrt(joueurs) arrondi au supérieur
        // 1 joueur -> 1 candidat
        // 4 joueurs -> 2 candidats
        // 9 joueurs -> 3 candidats
        // 16 joueurs -> 4 candidats
        // 50 joueurs -> 7-8 candidats
        int maxFromScaling = (int) Math.ceil(Math.sqrt(playersInZone));

        // Ne pas dépasser le cap restant
        int remainingCap = maxMicroEventsPerZone - activeEventsInZone;

        return Math.min(maxFromScaling, remainingCap);
    }

    /**
     * Verifie si un gros evenement dynamique est actif pres du joueur
     */
    private boolean hasNearbyDynamicEvent(Player player) {
        var dynamicManager = plugin.getDynamicEventManager();
        if (dynamicManager == null) return false;

        for (var event : dynamicManager.getActiveEvents().values()) {
            if (event.getLocation().getWorld() != null &&
                event.getLocation().getWorld().equals(player.getWorld())) {
                double distance = event.getLocation().distance(player.getLocation());
                if (distance < 100) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tente de spawner un micro-event pour un joueur
     */
    private boolean trySpawnEventForPlayer(Player player, Zone zone) {
        // Selectionner un type
        MicroEventType type = selectEventType();
        if (type == null) return false;

        // Trouver une position de spawn
        Location spawnLoc = findSpawnLocation(player.getLocation());
        if (spawnLoc == null) return false;

        // Creer l'evenement
        MicroEvent event = createEvent(type, player, spawnLoc, zone);
        if (event == null) return false;

        // Enregistrer et demarrer
        activeEvents.put(player.getUniqueId(), event);
        event.start();
        totalEventsSpawned++;

        // Definir le cooldown pour le prochain
        int interval = minInterval + random.nextInt(maxInterval - minInterval);
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (interval * 1000L));

        plugin.log(Level.INFO, "§a✓ Micro-event " + type.getDisplayName() +
            " spawne pour " + player.getName() + " en Zone " + zone.getId());

        return true;
    }

    /**
     * Selectionne un type de micro-event aleatoire base sur les poids
     */
    private MicroEventType selectEventType() {
        int totalWeight = MicroEventType.getTotalWeight();
        int roll = random.nextInt(totalWeight);

        int cumulative = 0;
        for (MicroEventType type : MicroEventType.values()) {
            cumulative += type.getSpawnWeight();
            if (roll < cumulative) {
                return type;
            }
        }

        return MicroEventType.values()[0];
    }

    /**
     * Trouve une position de spawn valide pres du joueur
     */
    private Location findSpawnLocation(Location playerLocation) {
        World world = playerLocation.getWorld();
        if (world == null) return null;

        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = spawnRadiusMin + random.nextDouble() * (spawnRadiusMax - spawnRadiusMin);

            double x = playerLocation.getX() + Math.cos(angle) * distance;
            double z = playerLocation.getZ() + Math.sin(angle) * distance;

            Location checkLoc = new Location(world, x, 0, z);

            // Verifier limites de la map
            if (!plugin.getZoneManager().isInMapBounds(checkLoc)) continue;

            // Verifier zone valide
            Zone zone = plugin.getZoneManager().getZoneAt(checkLoc);
            if (zone == null || zone.isSafeZone() || zone.getId() == 0) continue;

            // Trouver le sol
            int highestY = world.getHighestBlockYAt((int) x, (int) z);

            // Limiter Y à +5 au-dessus du joueur pour éviter les spawns sur les toits/arbres
            if (highestY > playerLocation.getY() + 5) continue;

            checkLoc.setY(highestY + 1);

            // Verifier sol solide et espace libre
            Block groundBlock = checkLoc.clone().add(0, -1, 0).getBlock();
            Block headBlock = checkLoc.clone().add(0, 1, 0).getBlock();
            if (!groundBlock.getType().isSolid()) continue;
            if (headBlock.getType().isSolid()) continue;

            return checkLoc;
        }

        return null;
    }

    /**
     * Cree un micro-event du type specifie
     */
    private MicroEvent createEvent(MicroEventType type, Player player, Location location, Zone zone) {
        return switch (type) {
            case ELITE_HUNTER -> new EliteHunterEvent(plugin, player, location, zone);
            case TEMPORAL_RIFT -> new TemporalRiftEvent(plugin, player, location, zone);
            case PINATA_ZOMBIE -> new PinataZombieEvent(plugin, player, location, zone);
            case DEATH_RACE -> new DeathRaceEvent(plugin, player, location, zone, this);
            case JACKPOT_ZOMBIE -> new JackpotZombieEvent(plugin, player, location, zone);
            case SUPPLY_DROP -> new SupplyDropMicroEvent(plugin, player, location, zone);
        };
    }

    // ==================== API PUBLIQUE ====================

    /**
     * Trouve l'evenement qui possede une entite donnee
     * Permet a n'importe quel joueur de participer a un micro-event
     */
    private MicroEvent findEventByEntity(LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        for (MicroEvent event : activeEvents.values()) {
            if (event.isValid() && event.getSpawnedEntities().contains(entityId)) {
                return event;
            }
        }
        return null;
    }

    /**
     * Gere un evenement de degats sur une entite
     * Appele par le listener
     * N'importe quel joueur peut infliger des degats aux entites de micro-event
     */
    public boolean handleEntityDamage(LivingEntity entity, Player attacker, double damage) {
        // Chercher l'evenement par l'entite attaquee (pas par l'attaquant)
        MicroEvent event = findEventByEntity(entity);
        if (event == null || !event.isValid()) return false;

        return event.handleDamage(entity, attacker, damage);
    }

    /**
     * Gere la mort d'une entite
     * Appele par le listener
     * N'importe quel joueur peut tuer les entites de micro-event
     */
    public boolean handleEntityDeath(LivingEntity entity, Player killer) {
        if (killer == null) return false;

        // Chercher l'evenement par l'entite tuee (pas par le tueur)
        MicroEvent event = findEventByEntity(entity);
        if (event == null || !event.isValid()) return false;

        return event.handleDeath(entity, killer);
    }

    /**
     * Notifie qu'un micro-event est termine
     */
    public void notifyEventComplete(MicroEvent event) {
        activeEvents.remove(event.getPlayer().getUniqueId());
        if (event.isCompleted()) {
            totalEventsCompleted++;
        } else if (event.isFailed()) {
            totalEventsFailed++;
        }
    }

    /**
     * Force le spawn d'un micro-event specifique pour un joueur
     */
    public MicroEvent forceSpawnEvent(MicroEventType type, Player player) {
        Zone zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        if (zone == null || zone.isSafeZone()) return null;

        // Annuler l'event actuel si existant
        MicroEvent current = activeEvents.get(player.getUniqueId());
        if (current != null) {
            current.forceStop();
        }

        Location spawnLoc = findSpawnLocation(player.getLocation());
        if (spawnLoc == null) {
            spawnLoc = player.getLocation().add(5, 0, 5);
        }

        MicroEvent event = createEvent(type, player, spawnLoc, zone);
        if (event == null) return null;

        activeEvents.put(player.getUniqueId(), event);
        event.start();
        totalEventsSpawned++;

        return event;
    }

    /**
     * Arrete le micro-event d'un joueur
     */
    public boolean stopPlayerEvent(Player player) {
        MicroEvent event = activeEvents.remove(player.getUniqueId());
        if (event != null) {
            event.forceStop();
            return true;
        }
        return false;
    }

    /**
     * Obtient le micro-event actif d'un joueur
     */
    public MicroEvent getPlayerEvent(Player player) {
        return activeEvents.get(player.getUniqueId());
    }

    /**
     * Met a jour le record de Course Mortelle
     */
    public boolean updateDeathRaceRecord(int zoneId, String playerName, double timeSeconds) {
        DeathRaceRecord current = deathRaceRecords.get(zoneId);
        if (current == null || timeSeconds < current.timeSeconds) {
            deathRaceRecords.put(zoneId, new DeathRaceRecord(playerName, timeSeconds));
            return true;
        }
        return false;
    }

    /**
     * Obtient le record de Course Mortelle pour une zone
     */
    public DeathRaceRecord getDeathRaceRecord(int zoneId) {
        return deathRaceRecords.get(zoneId);
    }

    /**
     * Statistiques
     */
    public String getStats() {
        // Calculer la répartition par zone
        Map<Integer, Integer> eventsByZone = getActiveEventsByZone();
        int maxInAnyZone = eventsByZone.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        return String.format(
            "§7Micro-events: §e%d §7(max/zone: §e%d§7/§6%d§7) | Total: §a%d §7(§2%d✓ §c%d✗§7)",
            activeEvents.size(), maxInAnyZone, maxMicroEventsPerZone,
            totalEventsSpawned, totalEventsCompleted, totalEventsFailed
        );
    }

    /**
     * Compte les micro-events actifs par zone
     */
    public Map<Integer, Integer> getActiveEventsByZone() {
        Map<Integer, Integer> result = new HashMap<>();
        for (MicroEvent event : activeEvents.values()) {
            if (event.isValid() && event.getZone() != null) {
                result.merge(event.getZone().getId(), 1, Integer::sum);
            }
        }
        return result;
    }

    /**
     * Obtient le nombre max de micro-events par zone
     */
    public int getMaxMicroEventsPerZone() {
        return maxMicroEventsPerZone;
    }

    /**
     * Définit le nombre max de micro-events par zone
     */
    public void setMaxMicroEventsPerZone(int max) {
        this.maxMicroEventsPerZone = Math.max(1, max);
    }

    /**
     * Active/désactive le scaling inversé
     */
    public void setUseInverseScaling(boolean use) {
        this.useInverseScaling = use;
    }

    /**
     * Vérifie si le scaling inversé est actif
     */
    public boolean isUseInverseScaling() {
        return useInverseScaling;
    }

    /**
     * Active/desactive le systeme
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Record de Course Mortelle
     */
    @Getter
    public static class DeathRaceRecord {
        private final String playerName;
        private final double timeSeconds;

        public DeathRaceRecord(String playerName, double timeSeconds) {
            this.playerName = playerName;
            this.timeSeconds = timeSeconds;
        }
    }
}
