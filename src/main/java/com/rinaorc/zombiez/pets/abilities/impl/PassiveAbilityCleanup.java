package com.rinaorc.zombiez.pets.abilities.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utilitaire pour nettoyer les données en mémoire lors de la déconnexion
 * Évite les fuites mémoire des Maps contenant des UUIDs
 */
public class PassiveAbilityCleanup {

    // Références statiques aux abilities avec état interne
    private static final List<Map<UUID, ?>> uuidMaps = new ArrayList<>();

    /**
     * Enregistre une map pour le nettoyage automatique
     */
    public static void registerForCleanup(Map<UUID, ?> map) {
        if (!uuidMaps.contains(map)) {
            uuidMaps.add(map);
        }
    }

    /**
     * Nettoie toutes les données associées à un joueur
     * Appelé lors de la déconnexion
     */
    public static void cleanupPlayer(UUID playerId) {
        for (Map<UUID, ?> map : uuidMaps) {
            map.remove(playerId);
        }
    }

    /**
     * Nettoie toutes les données (shutdown du serveur)
     */
    public static void cleanupAll() {
        for (Map<UUID, ?> map : uuidMaps) {
            map.clear();
        }
    }
}
