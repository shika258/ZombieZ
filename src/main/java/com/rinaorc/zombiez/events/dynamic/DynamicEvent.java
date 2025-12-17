package com.rinaorc.zombiez.events.dynamic;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.rinaorc.zombiez.items.scaling.ZoneScaling;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe abstraite représentant un événement dynamique
 * Les événements dynamiques se déclenchent automatiquement près des joueurs
 *
 * OPTIMISATIONS v1.1:
 * - Thread-safe participant tracking avec ConcurrentHashMap
 * - Caching des joueurs proches pour réduire les calculs
 * - Validation des mondes avant les calculs de distance
 * - Meilleure formule de récompenses avec scaling exponentiel
 */
@Getter
public abstract class DynamicEvent {

    protected final ZombieZPlugin plugin;
    protected final String id;
    protected final DynamicEventType type;
    protected final Location location;
    protected final Zone zone;
    protected final long startTime;

    @Setter
    protected boolean active = false;
    protected boolean completed = false;
    protected boolean failed = false;

    // Durée maximale de l'événement (en ticks)
    protected int maxDuration;
    protected int elapsedTicks = 0;

    // Joueurs participants - Thread-safe
    protected final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    // Cache des joueurs proches (mis à jour toutes les secondes)
    protected final Set<Player> nearbyPlayersCache = ConcurrentHashMap.newKeySet();
    protected long lastNearbyUpdateTime = 0;
    protected static final long NEARBY_CACHE_DURATION_MS = 1000; // 1 seconde
    protected static final double MAX_BOSSBAR_VISIBILITY_RADIUS = 64.0; // Rayon max pour afficher la boss bar

    // Système de téléportation
    protected final Set<UUID> teleportedPlayers = ConcurrentHashMap.newKeySet();
    protected final Set<UUID> deadDuringEvent = ConcurrentHashMap.newKeySet();
    protected boolean teleportEnabled = true;
    protected double itemScoreTolerance = 0.4; // 40% de tolérance sur l'item score requis

    // Tâches planifiées
    protected BukkitTask mainTask;
    protected BukkitTask cleanupTask;

    // Boss bar de l'événement
    protected BossBar bossBar;

    // Récompenses
    protected int basePointsReward;
    protected int baseXpReward;
    protected double lootMultiplier = 1.0;

    public DynamicEvent(ZombieZPlugin plugin, DynamicEventType type, Location location, Zone zone) {
        this.plugin = plugin;
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.location = location.clone();
        this.zone = zone;
        this.startTime = System.currentTimeMillis();

        // Configuration par défaut (peut être surchargée)
        this.maxDuration = type.getDefaultDuration();
        this.basePointsReward = type.getBasePointsReward();
        this.baseXpReward = type.getBaseXpReward();
    }

    /**
     * Démarre l'événement
     */
    public void start() {
        if (active) return;

        active = true;

        // Créer la boss bar
        createBossBar();

        // Annoncer l'événement
        announceStart();

        // Démarrer la logique principale
        startMainLogic();

        // Démarrer le timer de cleanup
        startCleanupTimer();
    }

    /**
     * Termine l'événement (succès)
     */
    public void complete() {
        if (!active || completed || failed) return;

        completed = true;
        active = false;

        // Distribuer les récompenses
        distributeRewards();

        // Annoncer la complétion
        announceCompletion();

        // Cleanup
        cleanup();
    }

    /**
     * Échoue l'événement
     */
    public void fail() {
        if (!active || completed || failed) return;

        failed = true;
        active = false;

        // Annoncer l'échec
        announceFailure();

        // Cleanup
        cleanup();
    }

    /**
     * Force l'arrêt de l'événement
     */
    public void forceStop() {
        active = false;
        cleanup();
    }

    /**
     * Nettoyage des ressources
     */
    protected void cleanup() {
        // Annuler les tâches
        if (mainTask != null && !mainTask.isCancelled()) {
            mainTask.cancel();
        }
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        // Supprimer la boss bar
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
            bossBar = null;
        }

        // Cleanup spécifique
        onCleanup();
    }

    /**
     * Créé la boss bar de l'événement
     */
    protected void createBossBar() {
        bossBar = plugin.getServer().createBossBar(
            type.getColor() + "§l" + type.getDisplayName() + " §7- §e" + zone.getDisplayName(),
            type.getBarColor(),
            BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(1.0);
    }

    /**
     * Met à jour la boss bar
     * OPTIMISÉ: Utilise le cache des joueurs proches pour éviter l'itération de tous les joueurs
     */
    protected void updateBossBar(double progress, String suffix) {
        if (bossBar == null) return;

        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
        bossBar.setTitle(type.getColor() + "§l" + type.getDisplayName() + " §7" + suffix);

        // Mettre à jour le cache des joueurs proches si nécessaire
        updateNearbyPlayersCache();

        // Ajouter les joueurs proches
        for (Player player : nearbyPlayersCache) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        // Retirer les joueurs qui sont sortis de la zone
        for (Player player : new ArrayList<>(bossBar.getPlayers())) {
            if (!nearbyPlayersCache.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
    }

    /**
     * Met à jour le cache des joueurs proches si le cache est expiré
     * OPTIMISATION: Évite de recalculer les distances à chaque tick
     * La boss bar s'affiche uniquement dans un rayon de 64 blocs maximum
     */
    protected void updateNearbyPlayersCache() {
        long now = System.currentTimeMillis();
        if (now - lastNearbyUpdateTime < NEARBY_CACHE_DURATION_MS) {
            return; // Cache encore valide
        }

        lastNearbyUpdateTime = now;
        nearbyPlayersCache.clear();

        World eventWorld = location.getWorld();
        if (eventWorld == null) return;

        // Utiliser le rayon le plus petit entre le type et le max (64 blocs)
        double radius = Math.min(type.getVisibilityRadius(), MAX_BOSSBAR_VISIBILITY_RADIUS);
        double radiusSquared = radius * radius;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().equals(eventWorld)) continue;

            // Utiliser distanceSquared pour éviter le calcul de racine carrée
            double distSq = player.getLocation().distanceSquared(location);
            if (distSq <= radiusSquared) {
                nearbyPlayersCache.add(player);
            }
        }
    }

    /**
     * Calcule la distance de manière sécurisée (gère les mondes différents)
     * @return la distance, ou Double.MAX_VALUE si les mondes sont différents
     */
    protected double safeDistance(Location from, Location to) {
        if (from == null || to == null) return Double.MAX_VALUE;
        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();
        if (fromWorld == null || toWorld == null || !fromWorld.equals(toWorld)) {
            return Double.MAX_VALUE;
        }
        return from.distance(to);
    }

    /**
     * Vérifie si un joueur est dans le même monde que l'événement
     */
    protected boolean isInEventWorld(Player player) {
        World eventWorld = location.getWorld();
        return eventWorld != null && eventWorld.equals(player.getWorld());
    }

    /**
     * Démarre le timer de cleanup automatique
     */
    protected void startCleanupTimer() {
        cleanupTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (active && !completed && !failed) {
                fail();
            }
        }, maxDuration);
    }

    /**
     * Ajoute un participant à l'événement
     */
    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    /**
     * Annonce le début de l'événement
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions entre mondes
     * AMÉLIORÉ: Inclut un message clickable pour la téléportation
     */
    protected void announceStart() {
        World eventWorld = location.getWorld();
        if (eventWorld == null) return;

        // Créer le message de téléportation clickable
        Component teleportComponent = createTeleportMessage();
        int minScore = (int) (getRequiredItemScore() * (1.0 - itemScoreTolerance));

        // Notifier les joueurs proches
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Ignorer les joueurs dans d'autres mondes
            if (!player.getWorld().equals(eventWorld)) continue;

            double distance = safeDistance(player.getLocation(), location);
            if (distance == Double.MAX_VALUE) continue;

            if (distance <= type.getAnnouncementRadius()) {
                // Joueurs très proches
                player.sendTitle(
                    type.getColor() + "§l" + type.getIcon() + " " + type.getDisplayName().toUpperCase(),
                    "§7" + getStartSubtitle(),
                    10, 60, 20
                );
                player.sendMessage("");
                player.sendMessage(type.getColor() + "§l" + type.getIcon() + " ÉVÉNEMENT: " + type.getDisplayName());
                player.sendMessage("§7" + type.getDescription());
                player.sendMessage("§7Distance: §e" + (int) distance + " blocs " + getDirectionFrom(player.getLocation()));
                player.sendMessage("§7Zone: §e" + zone.getDisplayName() + " §7| Item Score requis: §e" + minScore + "+");
                player.sendMessage("");

                // Message clickable pour la téléportation (si le joueur est éligible)
                if (teleportEnabled) {
                    player.sendMessage(teleportComponent);
                    player.sendMessage("");
                }

                player.playSound(player.getLocation(), type.getStartSound(), 1f, 1f);
            } else if (distance <= type.getAnnouncementRadius() * 2) {
                // Joueurs moyennement proches (message + téléport)
                player.sendMessage(type.getColor() + "§l" + type.getIcon() + " §7Un événement §e" + type.getDisplayName() + " §7a démarré à §e" + (int) distance + " blocs!");
                player.sendMessage("§7Zone: §e" + zone.getDisplayName() + " §7| Item Score requis: §e" + minScore + "+");

                if (teleportEnabled) {
                    player.sendMessage(teleportComponent);
                }
            } else if (distance <= type.getAnnouncementRadius() * 4) {
                // Joueurs éloignés (notification de téléportation possible)
                if (teleportEnabled) {
                    player.sendMessage(type.getColor() + "§l" + type.getIcon() + " §7Événement §e" + type.getDisplayName() +
                        " §7en Zone §e" + zone.getId() + " §7- Trop loin? ");
                    player.sendMessage(teleportComponent);
                }
            }
        }
    }

    /**
     * Obtient la direction relative depuis une location
     */
    protected String getDirectionFrom(Location from) {
        double dx = location.getX() - from.getX();
        double dz = location.getZ() - from.getZ();

        String direction = "";
        if (Math.abs(dz) > Math.abs(dx)) {
            direction = dz < 0 ? "§bNord" : "§cSud";
        } else {
            direction = dx > 0 ? "§eEst" : "§6Ouest";
        }

        return "§7vers le " + direction;
    }

    /**
     * Distribue les récompenses aux participants
     * OPTIMISÉ: Formule de récompense exponentielle basée sur la zone
     *
     * Formule:
     * - Points = base * (1 + zone * 0.1) * (1 + log(zone+1) * 0.5)
     * - XP = base * (1 + zone * 0.08) * (1 + log(zone+1) * 0.4)
     *
     * Cela donne une progression plus douce mais significative:
     * - Zone 1:  ~1.2x base
     * - Zone 10: ~2.5x base
     * - Zone 25: ~4.5x base
     * - Zone 50: ~8x base
     */
    protected void distributeRewards() {
        // Formule exponentielle pour un meilleur scaling
        double zoneMultiplier = 1.0 + (zone.getId() * 0.1) + (Math.log10(zone.getId() + 1) * 0.5);
        double xpMultiplier = 1.0 + (zone.getId() * 0.08) + (Math.log10(zone.getId() + 1) * 0.4);

        int totalPoints = (int) (basePointsReward * zoneMultiplier);
        int totalXp = (int) (baseXpReward * xpMultiplier);

        // Bonus pour le nombre de participants (encourager la coopération)
        int participantBonus = Math.min(50, (participants.size() - 1) * 10);
        totalPoints += participantBonus;
        totalXp += participantBonus / 2;

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Points
                plugin.getEconomyManager().addPoints(player, totalPoints);

                // XP
                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
                }

                // Message de récompense
                player.sendMessage("");
                player.sendMessage("§a§l✓ ÉVÉNEMENT COMPLÉTÉ!");
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                if (participantBonus > 0) {
                    player.sendMessage("§7Bonus coopération: §a+" + participantBonus + " Points");
                }
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
        }
    }

    /**
     * Annonce la complétion de l'événement
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions entre mondes
     */
    protected void announceCompletion() {
        World eventWorld = location.getWorld();
        if (eventWorld == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().equals(eventWorld)) continue;

            double distance = safeDistance(player.getLocation(), location);
            if (distance <= type.getAnnouncementRadius()) {
                player.sendTitle(
                    "§a§l✓ SUCCÈS!",
                    "§7" + type.getDisplayName() + " terminé!",
                    10, 40, 20
                );
            }
        }
    }

    /**
     * Annonce l'échec de l'événement
     * OPTIMISÉ: Utilise safeDistance pour éviter les exceptions entre mondes
     */
    protected void announceFailure() {
        World eventWorld = location.getWorld();
        if (eventWorld == null) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.getWorld().equals(eventWorld)) continue;

            double distance = safeDistance(player.getLocation(), location);
            if (distance <= type.getAnnouncementRadius()) {
                player.sendTitle(
                    "§c§l✗ ÉCHEC!",
                    "§7" + type.getDisplayName() + " a échoué...",
                    10, 40, 20
                );
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 0.8f);
            }
        }
    }

    /**
     * Vérifie si l'événement est toujours valide
     */
    public boolean isValid() {
        return active && !completed && !failed;
    }

    /**
     * Obtient le temps restant en secondes
     */
    public int getRemainingTimeSeconds() {
        int elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
        int maxSeconds = maxDuration / 20;
        return Math.max(0, maxSeconds - elapsedSeconds);
    }

    /**
     * Obtient le pourcentage de temps restant
     */
    public double getRemainingTimePercent() {
        long elapsed = System.currentTimeMillis() - startTime;
        long max = (maxDuration / 20) * 1000L;
        return Math.max(0, 1.0 - ((double) elapsed / max));
    }

    // ==================== SYSTÈME DE TÉLÉPORTATION ====================

    /**
     * Calcule le score d'item requis pour la zone de l'événement
     * Basé sur ZoneScaling pour cohérence avec le système d'items
     *
     * @return Score minimum recommandé pour participer
     */
    public int getRequiredItemScore() {
        // Score de base pour la zone (progression exponentielle)
        int baseScore = ZoneScaling.getBaseScoreForZone(zone.getId());

        // On prend le score d'un équipement complet moyen (6 pièces)
        // avec un multiplicateur modéré
        return (int) (baseScore * 4.5);
    }

    /**
     * Vérifie si un joueur peut se téléporter à cet événement
     *
     * @param player Le joueur à vérifier
     * @return null si autorisé, sinon le message d'erreur
     */
    public String canPlayerTeleport(Player player) {
        if (!active) {
            return "§c§l⚠ §7Cet événement n'est plus actif!";
        }

        if (!teleportEnabled) {
            return "§c§l⚠ §7La téléportation est désactivée pour cet événement!";
        }

        UUID uuid = player.getUniqueId();

        // Vérifier si le joueur s'est déjà téléporté
        if (teleportedPlayers.contains(uuid)) {
            return "§c§l⚠ §7Vous vous êtes déjà téléporté à cet événement!";
        }

        // Vérifier si le joueur est mort pendant l'événement
        if (deadDuringEvent.contains(uuid)) {
            return "§c§l⚠ §7Vous êtes mort pendant cet événement et ne pouvez plus vous y téléporter!";
        }

        // Vérifier l'item score du joueur
        int playerScore = plugin.getItemManager().calculateTotalItemScore(player);
        int requiredScore = getRequiredItemScore();
        int minScore = (int) (requiredScore * (1.0 - itemScoreTolerance));

        if (playerScore < minScore) {
            return "§c§l⚠ §7Votre équipement est trop faible pour cet événement!\n" +
                   "§7Votre score: §c" + playerScore + " §7| Requis: §e" + minScore + "+";
        }

        // Vérifier que le joueur n'est pas déjà dans la zone de l'événement
        if (isInEventWorld(player)) {
            double distance = safeDistance(player.getLocation(), location);
            if (distance != Double.MAX_VALUE && distance < 50) {
                return "§c§l⚠ §7Vous êtes déjà proche de cet événement!";
            }
        }

        return null; // Autorisé
    }

    /**
     * Téléporte un joueur à l'événement s'il remplit les conditions
     *
     * @param player Le joueur à téléporter
     * @return true si téléporté avec succès
     */
    public boolean teleportPlayer(Player player) {
        String error = canPlayerTeleport(player);
        if (error != null) {
            player.sendMessage(error);
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            player.sendMessage("§c§l⚠ §7Erreur: Monde de l'événement introuvable!");
            return false;
        }

        // Trouver une position de spawn sécurisée près de l'événement
        Location teleportLoc = findSafeTeleportLocation();
        if (teleportLoc == null) {
            player.sendMessage("§c§l⚠ §7Impossible de trouver une position de téléportation sécurisée!");
            return false;
        }

        // Marquer le joueur comme téléporté
        teleportedPlayers.add(player.getUniqueId());
        addParticipant(player);

        // Téléporter le joueur
        player.teleport(teleportLoc);

        // Effets visuels et sonores
        world.spawnParticle(Particle.PORTAL, teleportLoc, 30, 1, 2, 1, 0.1);
        player.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Message de confirmation
        player.sendMessage("");
        player.sendMessage(type.getColor() + "§l" + type.getIcon() + " §aTéléportation réussie!");
        player.sendMessage("§7Vous avez rejoint l'événement §e" + type.getDisplayName() + " §7en §e" + zone.getDisplayName());
        player.sendMessage("§c§l⚠ §7Attention: Si vous mourez, vous ne pourrez pas revenir!");
        player.sendMessage("");

        return true;
    }

    /**
     * Trouve une position de téléportation sécurisée près de l'événement
     */
    protected Location findSafeTeleportLocation() {
        World world = location.getWorld();
        if (world == null) return null;

        Random random = new Random();

        // Essayer plusieurs positions
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 15 + random.nextDouble() * 15; // 15-30 blocs de l'événement

            double x = location.getX() + Math.cos(angle) * distance;
            double z = location.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z);

            Location checkLoc = new Location(world, x, y + 1, z);

            // Vérifier que c'est sûr
            if (checkLoc.getBlock().getType().isAir() &&
                checkLoc.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                checkLoc.clone().add(0, -1, 0).getBlock().getType().isSolid()) {

                // Orienter vers l'événement
                double dx = location.getX() - x;
                double dz = location.getZ() - z;
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                checkLoc.setYaw(yaw);

                return checkLoc;
            }
        }

        // Fallback: position au-dessus de l'événement
        Location fallback = location.clone().add(0, 5, 0);
        return fallback;
    }

    /**
     * Enregistre qu'un joueur est mort pendant l'événement
     * Appelé par le listener quand un participant meurt
     */
    public void recordPlayerDeath(UUID playerUuid) {
        if (participants.contains(playerUuid) || teleportedPlayers.contains(playerUuid)) {
            deadDuringEvent.add(playerUuid);
        }
    }

    /**
     * Vérifie si un joueur s'est téléporté à cet événement
     */
    public boolean hasTeleported(UUID playerUuid) {
        return teleportedPlayers.contains(playerUuid);
    }

    /**
     * Vérifie si un joueur est mort pendant cet événement
     */
    public boolean hasDiedDuringEvent(UUID playerUuid) {
        return deadDuringEvent.contains(playerUuid);
    }

    /**
     * Crée le message clickable pour la téléportation
     */
    protected Component createTeleportMessage() {
        int requiredScore = getRequiredItemScore();
        int minScore = (int) (requiredScore * (1.0 - itemScoreTolerance));

        return Component.text("")
            .append(Component.text("§7[")
                .append(Component.text("⚡ CLIQUEZ ICI POUR VOUS TÉLÉPORTER")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/event tp " + id))
                    .hoverEvent(HoverEvent.showText(
                        Component.text("§aClic pour rejoindre l'événement!\n")
                            .append(Component.text("§7Item Score requis: §e" + minScore + "+\n"))
                            .append(Component.text("§c⚠ Une seule téléportation par événement!"))
                    )))
                .append(Component.text("§7]")));
    }

    // ==================== MÉTHODES ABSTRAITES ====================

    /**
     * Démarre la logique principale de l'événement
     */
    protected abstract void startMainLogic();

    /**
     * Appelé lors du cleanup
     */
    protected abstract void onCleanup();

    /**
     * Obtient le sous-titre de début
     */
    protected abstract String getStartSubtitle();

    /**
     * Appelé à chaque tick de l'événement
     */
    public abstract void tick();

    /**
     * Obtient les informations de debug
     */
    public abstract String getDebugInfo();
}
