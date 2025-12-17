package com.rinaorc.zombiez.pets.abilities;

import com.rinaorc.zombiez.pets.PetData;
import org.bukkit.entity.Player;

/**
 * Interface pour les capacités de Pet
 * Chaque pet implémente ses propres capacités passive et active
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
     * Applique la capacité passive (appelée périodiquement)
     * @param player Le joueur propriétaire
     * @param petData Les données du pet
     */
    default void applyPassive(Player player, PetData petData) {}

    /**
     * Active la capacité active (sur demande du joueur)
     * @param player Le joueur propriétaire
     * @param petData Les données du pet
     * @return true si l'activation a réussi
     */
    default boolean activate(Player player, PetData petData) {
        return false;
    }

    /**
     * Obtient le cooldown de base en secondes
     */
    default int getCooldown() {
        return 30;
    }

    /**
     * Obtient le cooldown ajusté selon le niveau du pet
     */
    default int getAdjustedCooldown(PetData petData) {
        // Réduction de 2% par niveau après le 1
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
}
