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
     * Vérifie si une entité est un pet (animal de compagnie).
     * Les pets sont identifiés par les tags scoreboard "zombiez_pet" ou "zombiez_pet_ally".
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est un pet
     */
    public static boolean isPet(Entity entity) {
        if (entity == null) return false;
        var tags = entity.getScoreboardTags();
        return tags.contains("zombiez_pet") || tags.contains("zombiez_pet_ally");
    }

    /**
     * Vérifie si une entité est un minion invoqué par un joueur.
     *
     * @param entity L'entité à vérifier
     * @param owner Le joueur propriétaire potentiel
     * @return true si l'entité est un minion du joueur
     */
    public static boolean isPlayerMinion(Entity entity, Player owner) {
        if (entity == null || owner == null) return false;
        var tags = entity.getScoreboardTags();
        return tags.contains("player_minion") && tags.contains("owner_" + owner.getUniqueId());
    }

    /**
     * Vérifie si une entité est amicale (pet, minion du joueur, ou allié).
     *
     * @param entity L'entité à vérifier
     * @param player Le joueur pour vérifier les minions
     * @return true si l'entité est amicale
     */
    public static boolean isFriendlyEntity(Entity entity, Player player) {
        if (entity == null) return false;
        if (isPet(entity)) return true;
        if (isPlayerMinion(entity, player)) return true;
        return false;
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
     * Vérifie si une entité est protégée (NPC ou pet).
     * Combine isAnyNPC et isPet pour une vérification complète.
     * À utiliser dans les talents pour éviter d'affecter les entités amicales.
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est protégée (NPC ou pet)
     */
    public static boolean isProtectedEntity(Entity entity) {
        if (entity == null) return true;
        return isAnyNPC(entity) || isPet(entity);
    }

    /**
     * Vérifie si une entité est une cible valide pour les talents.
     * Exclut les joueurs, les NPCs (Citizens/ZombieZ), les pets et les minions du joueur.
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
        if (isFriendlyEntity(entity, attacker)) return false;
        return true;
    }
}
