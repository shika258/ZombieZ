package com.rinaorc.zombiez.combat;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de tracking DPS ultra-optimisé
 *
 * Utilise un ring buffer pour stocker les dégâts récents
 * et calcule le DPS sur une fenêtre glissante.
 *
 * Optimisations:
 * - Ring buffer de taille fixe (pas d'allocation)
 * - Calcul incrémental du DPS
 * - Structures primitives (pas de boxing)
 * - Thread-safe via ConcurrentHashMap
 * - Nettoyage automatique des vieilles données
 */
public class DPSTracker {

    // Singleton avec holder idiom (thread-safe, lazy)
    private static class Holder {
        static final DPSTracker INSTANCE = new DPSTracker();
    }

    public static DPSTracker getInstance() {
        return Holder.INSTANCE;
    }

    // Configuration
    private static final int BUFFER_SIZE = 64;           // Taille du ring buffer (puissance de 2 pour modulo rapide)
    private static final long WINDOW_MS = 5000;          // Fenêtre de calcul: 5 secondes
    private static final long CLEANUP_INTERVAL = 60000;  // Nettoyage toutes les 60 secondes

    // Données par joueur
    private final Map<UUID, PlayerDPSData> playerData = new ConcurrentHashMap<>();

    // Timestamp du dernier nettoyage
    private long lastCleanup = System.currentTimeMillis();

    private DPSTracker() {}

    /**
     * Enregistre des dégâts infligés par un joueur
     * Appelé depuis CombatListener à chaque hit
     *
     * @param player Le joueur qui inflige les dégâts
     * @param damage Les dégâts infligés
     */
    public void recordDamage(Player player, double damage) {
        if (player == null || damage <= 0) return;

        PlayerDPSData data = playerData.computeIfAbsent(
            player.getUniqueId(),
            k -> new PlayerDPSData()
        );

        data.addDamage(damage);

        // Nettoyage périodique (léger, vérifie juste le temps)
        maybeCleanup();
    }

    /**
     * Obtient le DPS actuel d'un joueur
     *
     * @param player Le joueur
     * @return Le DPS (dégâts par seconde) sur la fenêtre glissante
     */
    public double getDPS(Player player) {
        if (player == null) return 0.0;

        PlayerDPSData data = playerData.get(player.getUniqueId());
        if (data == null) return 0.0;

        return data.calculateDPS();
    }

    /**
     * Obtient le DPS formaté pour l'affichage
     *
     * @param player Le joueur
     * @return Le DPS formaté (ex: "1.2k", "156.3")
     */
    public String getFormattedDPS(Player player) {
        double dps = getDPS(player);
        return formatDPS(dps);
    }

    /**
     * Formate le DPS pour l'affichage
     */
    public static String formatDPS(double dps) {
        if (dps < 0.1) return "0";
        if (dps < 1000) {
            // Moins de 1000: afficher avec 1 décimale
            if (dps == (int) dps) {
                return String.valueOf((int) dps);
            }
            return String.format("%.1f", dps);
        }
        if (dps < 10000) {
            // Entre 1k et 10k: afficher X.Xk
            return String.format("%.1fk", dps / 1000);
        }
        if (dps < 1000000) {
            // Entre 10k et 1M: afficher XXk
            return String.format("%.0fk", dps / 1000);
        }
        // Plus de 1M: afficher X.XM
        return String.format("%.1fM", dps / 1000000);
    }

    /**
     * Réinitialise les données d'un joueur (à appeler quand il se déconnecte)
     */
    public void clearPlayer(Player player) {
        if (player != null) {
            playerData.remove(player.getUniqueId());
        }
    }

    /**
     * Nettoyage périodique des joueurs inactifs
     */
    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL) return;

        lastCleanup = now;

        // Supprimer les données des joueurs inactifs depuis plus de 2 minutes
        long threshold = now - 120000;
        playerData.entrySet().removeIf(entry -> entry.getValue().getLastActivity() < threshold);
    }

    /**
     * Données DPS pour un joueur
     * Utilise un ring buffer pour un stockage O(1) constant
     */
    private static class PlayerDPSData {
        // Ring buffer: timestamps et dégâts
        private final long[] timestamps = new long[BUFFER_SIZE];
        private final double[] damages = new double[BUFFER_SIZE];

        // Index courant dans le buffer (utilise masque binaire pour modulo rapide)
        private int index = 0;
        private static final int MASK = BUFFER_SIZE - 1;

        // Cache du DPS pour éviter les recalculs fréquents
        private double cachedDPS = 0.0;
        private long lastCalculation = 0;
        private static final long CACHE_DURATION_MS = 100; // Recalculer max toutes les 100ms

        // Dernière activité (pour cleanup)
        private long lastActivity = System.currentTimeMillis();

        /**
         * Ajoute des dégâts au buffer
         */
        synchronized void addDamage(double damage) {
            long now = System.currentTimeMillis();

            timestamps[index] = now;
            damages[index] = damage;
            index = (index + 1) & MASK; // Équivalent à (index + 1) % BUFFER_SIZE mais plus rapide

            lastActivity = now;

            // Invalider le cache
            lastCalculation = 0;
        }

        /**
         * Calcule le DPS sur la fenêtre glissante
         */
        synchronized double calculateDPS() {
            long now = System.currentTimeMillis();

            // Utiliser le cache si encore valide
            if (now - lastCalculation < CACHE_DURATION_MS) {
                return cachedDPS;
            }

            // Calculer la somme des dégâts dans la fenêtre
            long windowStart = now - WINDOW_MS;
            double totalDamage = 0.0;
            long oldestTimestamp = now;
            long newestTimestamp = 0;
            int validEntries = 0;

            for (int i = 0; i < BUFFER_SIZE; i++) {
                long ts = timestamps[i];
                if (ts >= windowStart && ts <= now) {
                    totalDamage += damages[i];
                    validEntries++;
                    if (ts < oldestTimestamp) oldestTimestamp = ts;
                    if (ts > newestTimestamp) newestTimestamp = ts;
                }
            }

            // Calculer le DPS
            if (validEntries == 0 || totalDamage <= 0) {
                cachedDPS = 0.0;
            } else {
                // Utiliser le temps réel entre le premier et dernier hit, minimum 1 seconde
                double timeSpanSeconds = Math.max(1.0, (newestTimestamp - oldestTimestamp) / 1000.0);

                // Si un seul hit, utiliser la fenêtre complète
                if (validEntries == 1) {
                    timeSpanSeconds = WINDOW_MS / 1000.0;
                }

                cachedDPS = totalDamage / timeSpanSeconds;
            }

            lastCalculation = now;
            return cachedDPS;
        }

        long getLastActivity() {
            return lastActivity;
        }
    }

    /**
     * Obtient la couleur du DPS pour l'affichage
     */
    public static String getDPSColor(double dps) {
        if (dps >= 5000) return "§c§l";   // Rouge brillant (très haut DPS)
        if (dps >= 2000) return "§c";     // Rouge
        if (dps >= 1000) return "§6";     // Orange
        if (dps >= 500) return "§e";      // Jaune
        if (dps >= 100) return "§a";      // Vert
        if (dps >= 10) return "§f";       // Blanc
        return "§7";                       // Gris (très bas ou inactif)
    }
}
