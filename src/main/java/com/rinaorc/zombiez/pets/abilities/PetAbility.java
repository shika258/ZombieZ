package com.rinaorc.zombiez.pets.abilities;

import com.rinaorc.zombiez.pets.PetData;
import org.bukkit.entity.Player;

/**
 * Interface pour les capacités de Pet
 * Chaque pet implémente ses propres capacités passive et ultime
 * Les capacités ultimes s'activent automatiquement selon un intervalle défini
 */
public interface PetAbility {

    /**
     * Obtient l'ID unique de la capacité
     */
    String getId();

    /**
     * Obtient le nom d'affichage
     */
    String getDisplayName();

    /**
     * Obtient la description
     */
    String getDescription();

    /**
     * Vérifie si cette capacité est passive
     */
    boolean isPassive();

    /**
     * Vérifie si cette capacité est une ultime (s'active automatiquement)
     */
    default boolean isUltimate() {
        return !isPassive();
    }

    /**
     * Applique la capacité passive (appelée périodiquement)
     * @param player Le joueur propriétaire
     * @param petData Les données du pet
     */
    default void applyPassive(Player player, PetData petData) {}

    /**
     * Active la capacité ultime (automatiquement par le système)
     * @param player Le joueur propriétaire
     * @param petData Les données du pet
     * @return true si l'activation a réussi
     */
    default boolean activate(Player player, PetData petData) {
        return false;
    }

    /**
     * Obtient l'intervalle d'activation automatique en secondes
     * C'est le temps entre chaque déclenchement automatique de l'ultime
     */
    default int getCooldown() {
        return 30;
    }

    /**
     * Obtient l'intervalle ajusté selon le niveau du pet
     * Réduction de 2% par niveau après le niveau 1
     */
    default int getAdjustedCooldown(PetData petData) {
        double reduction = (petData.getLevel() - 1) * 0.02;
        return (int) (getCooldown() * (1 - reduction));
    }

    /**
     * Obtient la puissance de la capacité selon le niveau
     */
    default double getPower(PetData petData) {
        return petData.getStatMultiplier();
    }

    /**
     * Vérifie si l'ultime peut s'activer dans les conditions actuelles
     * Par exemple: joueur en combat, cibles à proximité, etc.
     */
    default boolean canAutoActivate(Player player, PetData petData) {
        // Par défaut, active si le joueur est vivant et pas en spectateur
        return !player.isDead() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR;
    }

    /**
     * Appelé quand le pet est équipé
     */
    default void onEquip(Player player, PetData petData) {}

    /**
     * Appelé quand le pet est déséquipé
     */
    default void onUnequip(Player player, PetData petData) {}

    /**
     * Appelé quand le joueur tue un mob
     */
    default void onKill(Player player, PetData petData, org.bukkit.entity.LivingEntity killed) {}

    /**
     * Appelé quand le joueur prend des dégâts
     */
    default void onDamageReceived(Player player, PetData petData, double damage) {}

    /**
     * Appelé quand le joueur inflige des dégâts
     */
    default void onDamageDealt(Player player, PetData petData, org.bukkit.entity.LivingEntity target, double damage) {}

    /**
     * Appelé quand le joueur inflige des dégâts - version avec modification possible
     * @return les dégâts modifiés
     */
    default double onDamageDealt(Player player, PetData petData, double damage, org.bukkit.entity.LivingEntity target) {
        return damage;
    }
}
