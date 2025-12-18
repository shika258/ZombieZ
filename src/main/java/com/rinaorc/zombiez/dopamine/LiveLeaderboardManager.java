package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Système de mise à jour en direct du classement
 *
 * Effet dopamine: Notifie les joueurs quand ils montent ou descendent dans le classement,
 * créant une compétition sociale stimulante et un sentiment de progression visible.
 *
 * Fonctionnalités:
 * - Notifications en temps réel des changements de position
 * - Annonces serveur pour les entrées dans le top 10
 * - Alertes quand un joueur est sur le point de dépasser
 * - Classements multiples (kills, XP, points, etc.)
 *
 * @author ZombieZ Dopamine System
 */
public class LiveLeaderboardManager {

    private final ZombieZPlugin plugin;

    // Cache des positions précédentes par joueur et type de leaderboard
    private final Map<UUID, Map<LeaderboardType, Integer>> previousPositions = new ConcurrentHashMap<>();

    // Cache des valeurs pour comparaison
    private final Map<UUID, Map<LeaderboardType, Long>> previousValues = new ConcurrentHashMap<>();

    // Configuration
    private static final int UPDATE_INTERVAL_TICKS = 20 * 30; // 30 secondes
    private static final int TOP_ANNOUNCEMENT_THRESHOLD = 10; // Annonce pour entrée dans le top 10

    public LiveLeaderboardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    /**
     * Notifie un changement de stat (appelé quand une stat change significativement)
     */
    public void notifyStatChange(Player player, LeaderboardType type, long newValue) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();

        // Obtenir les valeurs précédentes de manière thread-safe
        Map<LeaderboardType, Long> playerValues = previousValues.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        long oldValue = playerValues.getOrDefault(type, 0L);

        // Mettre à jour la valeur
        playerValues.put(type, newValue);

        // Vérifier si le changement est significatif (augmentation de 5%+ ou top 20)
        if (newValue <= oldValue) return;
        if (newValue < oldValue * 1.05 && oldValue > 100) return; // Ignorer les petits changements

        // Calculer la nouvelle position
        int newPosition = calculatePosition(player, type, newValue);

        // Obtenir la position précédente de manière thread-safe
        Map<LeaderboardType, Integer> playerPositions = previousPositions.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int oldPosition = playerPositions.getOrDefault(type, Integer.MAX_VALUE);

        // Vérifier les changements de position
        if (newPosition < oldPosition) {
            // Montée dans le classement!
            handlePositionGain(player, type, oldPosition, newPosition);
        }

        // Mettre à jour la position en cache de manière thread-safe
        playerPositions.put(type, newPosition);

        // Vérifier si quelqu'un est sur le point d'être dépassé (seulement pour top 20)
        if (newPosition <= 20) {
            checkOvertakeWarning(player, type, newValue);
        }
    }

    /**
     * Gère une montée dans le classement
     */
    private void handlePositionGain(Player player, LeaderboardType type, int oldPosition, int newPosition) {
        // Notification différente selon l'importance
        if (newPosition <= 3) {
            // Entrée dans le top 3!
            handleTop3Entry(player, type, newPosition);
        } else if (newPosition <= TOP_ANNOUNCEMENT_THRESHOLD && oldPosition > TOP_ANNOUNCEMENT_THRESHOLD) {
            // Entrée dans le top 10
            handleTop10Entry(player, type, newPosition);
        } else if (newPosition < oldPosition) {
            // Progression normale
            handleNormalProgress(player, type, oldPosition, newPosition);
        }
    }

    /**
     * Gère une entrée dans le top 3
     */
    private void handleTop3Entry(Player player, LeaderboardType type, int position) {
        String medal = switch (position) {
            case 1 -> "§6§l🥇 #1";
            case 2 -> "§7§l🥈 #2";
            case 3 -> "§c§l🥉 #3";
            default -> "§e#" + position;
        };

        // Titre spectaculaire
        player.sendTitle(medal, "§f" + type.getDisplayName(), 10, 60, 15);

        // Annonce serveur
        String announcement = medal + " §e" + player.getName() + " §7est maintenant §f" +
            getPositionText(position) + " §7en " + type.getDisplayName() + "!";
        plugin.getServer().broadcastMessage(announcement);

        // Sons épiques
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        }, 5L);

        // Particules
        spawnTopPlayerParticles(player, position);

        // Message chat
        player.sendMessage("");
        player.sendMessage("§6§l★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");
        player.sendMessage("§6§l★ " + medal + " §eTu es " + getPositionText(position) + " du serveur! §6§l★");
        player.sendMessage("§6§l★ §7Classement: §f" + type.getDisplayName());
        player.sendMessage("§6§l★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");
        player.sendMessage("");
    }

    /**
     * Gère une entrée dans le top 10
     */
    private void handleTop10Entry(Player player, LeaderboardType type, int position) {
        // Titre
        player.sendTitle("§e§lTOP 10!", "§7#" + position + " en " + type.getDisplayName(), 10, 50, 10);

        // Annonce serveur
        String announcement = "§e⬆ " + player.getName() + " §7entre dans le §fTOP 10 §7en " +
            type.getDisplayName() + " §8(#" + position + ")";
        plugin.getServer().broadcastMessage(announcement);

        // Sons
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);

        // Message personnel
        player.sendMessage("§a§l★ Félicitations! §eTu es maintenant #" + position + " en " + type.getDisplayName() + "!");
    }

    /**
     * Gère une progression normale
     */
    private void handleNormalProgress(Player player, LeaderboardType type, int oldPosition, int newPosition) {
        int positionsGained = oldPosition - newPosition;

        // Ne notifier que pour des progressions significatives
        if (positionsGained < 5 && newPosition > 20) return;

        // Message subtil
        String message = "§a⬆ §7Tu as gagné §a" + positionsGained + " place" +
            (positionsGained > 1 ? "s" : "") + " §7en " + type.getDisplayName() +
            "! §8(Maintenant #" + newPosition + ")";
        player.sendMessage(message);

        // Son subtil
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

    /**
     * Vérifie si quelqu'un est sur le point d'être dépassé
     */
    private void checkOvertakeWarning(Player player, LeaderboardType type, long playerValue) {
        // Obtenir les joueurs juste devant
        List<PlayerScore> topPlayers = getTopPlayers(type, 100);
        int playerIndex = -1;

        for (int i = 0; i < topPlayers.size(); i++) {
            if (topPlayers.get(i).uuid.equals(player.getUniqueId())) {
                playerIndex = i;
                break;
            }
        }

        if (playerIndex <= 0) return; // Déjà premier ou pas trouvé

        // Vérifier le joueur juste devant
        PlayerScore playerAhead = topPlayers.get(playerIndex - 1);
        long difference = playerAhead.value - playerValue;

        // Si proche de dépasser (moins de 5% d'écart)
        if (difference > 0 && difference < playerValue * 0.05) {
            Player aheadPlayer = plugin.getServer().getPlayer(playerAhead.uuid);

            // Notifier les deux joueurs
            player.sendMessage("§e⚡ §7Tu es à §e" + difference + " §7de dépasser §f" + playerAhead.name + " §7en " + type.getDisplayName() + "!");

            if (aheadPlayer != null && aheadPlayer.isOnline()) {
                aheadPlayer.sendMessage("§c⚠ §7" + player.getName() + " §7est sur le point de te dépasser en " + type.getDisplayName() + "!");
                aheadPlayer.playSound(aheadPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            }
        }
    }

    /**
     * Spawn des particules pour les joueurs du top
     */
    private void spawnTopPlayerParticles(Player player, int position) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                org.bukkit.Location loc = player.getLocation().add(0, 1, 0);

                // Couronne de particules
                for (int i = 0; i < 8; i++) {
                    double angle = (ticks * 0.2) + (i * Math.PI / 4);
                    double radius = 1.0;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Particle particle = position == 1 ? Particle.TOTEM_OF_UNDYING : Particle.END_ROD;
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                }

                if (position == 1 && ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Calcule la position d'un joueur dans un classement
     * Optimisé pour ne compter que les joueurs avec une valeur supérieure
     */
    private int calculatePosition(Player player, LeaderboardType type, long value) {
        int position = 1;

        // Compter uniquement les joueurs avec une valeur supérieure (O(n) mais sans tri)
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            PlayerData data = plugin.getPlayerDataManager().getPlayer(online);
            if (data == null) continue;

            long otherValue = getStatValue(data, type);
            if (otherValue > value) {
                position++;
            }
        }
        return position;
    }

    /**
     * Obtient la valeur d'une stat pour un PlayerData
     */
    private long getStatValue(PlayerData data, LeaderboardType type) {
        return switch (type) {
            case KILLS -> data.getTotalKills();
            case XP -> data.getTotalXp().get();
            case POINTS -> data.getPoints().get();
            case LEVEL -> data.getLevel().get();
            case STREAK -> data.getBestKillStreak().get();
        };
    }

    /**
     * Obtient les meilleurs joueurs pour un type de classement
     * Limité à 100 joueurs max pour les performances
     */
    private List<PlayerScore> getTopPlayers(LeaderboardType type, int limit) {
        List<PlayerScore> scores = new ArrayList<>();
        int effectiveLimit = Math.min(limit, 100); // Maximum 100 joueurs

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
            if (data == null) continue;

            long value = getStatValue(data, type);
            scores.add(new PlayerScore(player.getUniqueId(), player.getName(), value));
        }

        // Trier par valeur décroissante
        scores.sort((a, b) -> Long.compare(b.value, a.value));

        // Limiter le résultat
        if (scores.size() > effectiveLimit) {
            return scores.subList(0, effectiveLimit);
        }
        return scores;
    }

    /**
     * Obtient le texte de position (1er, 2ème, etc.)
     */
    private String getPositionText(int position) {
        return switch (position) {
            case 1 -> "1er";
            case 2 -> "2ème";
            case 3 -> "3ème";
            default -> position + "ème";
        };
    }

    /**
     * Démarre la tâche de mise à jour périodique
     */
    private void startUpdateTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                updatePlayerPositions(player);
            }
        }, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
    }

    /**
     * Met à jour les positions d'un joueur (appelé périodiquement)
     */
    private void updatePlayerPositions(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Mettre à jour chaque type de leaderboard
        for (LeaderboardType type : LeaderboardType.values()) {
            long value = switch (type) {
                case KILLS -> data.getTotalKills();
                case XP -> data.getTotalXp().get();
                case POINTS -> data.getPoints().get();
                case LEVEL -> data.getLevel().get();
                case STREAK -> data.getBestKillStreak().get();
            };

            notifyStatChange(player, type, value);
        }
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void clearPlayer(UUID uuid) {
        previousPositions.remove(uuid);
        previousValues.remove(uuid);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ENUMS ET CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Types de classements
     */
    public enum LeaderboardType {
        KILLS("Kills Totaux"),
        XP("XP Total"),
        POINTS("Points"),
        LEVEL("Niveau"),
        STREAK("Meilleur Streak");

        private final String displayName;

        LeaderboardType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Score d'un joueur
     */
    private record PlayerScore(UUID uuid, String name, long value) {}
}
