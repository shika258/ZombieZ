package com.rinaorc.zombiez.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Utilitaires pour la gestion des entités
 */
public final class EntityUtils {

    private EntityUtils() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Vérifie si une entité est un NPC Citizens.
     * Citizens met la métadonnée "NPC" sur toutes ses entités.
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est un NPC Citizens
     */
    public static boolean isCitizensNPC(Entity entity) {
        if (entity == null) return false;
        return entity.hasMetadata("NPC");
    }

    /**
     * Vérifie si une entité est un NPC ZombieZ (quête, refuge, etc.)
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est un NPC ZombieZ
     */
    public static boolean isZombieZNPC(Entity entity) {
        if (entity == null) return false;
        return entity.getScoreboardTags().contains("zombiez_npc") ||
               entity.hasMetadata("zombiez_shelter_npc");
    }

    /**
     * Vérifie si une entité est un NPC quelconque (Citizens ou ZombieZ).
     * Utilisé pour éviter que les talents n'affectent les NPCs.
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est un NPC
     */
    public static boolean isAnyNPC(Entity entity) {
        return isCitizensNPC(entity) || isZombieZNPC(entity);
    }

    /**
     * Vérifie si une entité est une cible valide pour les talents.
     * Exclut les joueurs, les NPCs Citizens et les NPCs ZombieZ.
     *
     * @param entity L'entité à vérifier
     * @param attacker Le joueur qui attaque (pour éviter l'auto-ciblage)
     * @return true si l'entité est une cible valide
     */
    public static boolean isValidTalentTarget(Entity entity, Player attacker) {
        if (entity == null) return false;
        if (entity == attacker) return false;
        if (entity instanceof Player) return false;
        if (isAnyNPC(entity)) return false;
        return true;
    }
}
