package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Facade pour le systeme d'indicateurs de degats.
 *
 * Cette classe delegue maintenant vers TextDisplayDamageIndicator qui utilise
 * de vraies entites TextDisplay Bukkit au lieu de packets ProtocolLib virtuels.
 *
 * Avantages du nouveau systeme:
 * - Aucune dependance a ProtocolLib
 * - Utilise l'API TextDisplay native de Bukkit 1.19.4+
 * - Animations fluides via setTransformation() et setInterpolationDuration()
 * - Nettoyage automatique avec display.remove()
 * - Compatible avec PaperMC et Spigot
 *
 * Le fallback vers DamageIndicator est gere directement par TextDisplayDamageIndicator.
 *
 * @author Rinaorc Studio
 * @see TextDisplayDamageIndicator
 */
public class PacketDamageIndicator {

    /**
     * Verifie si le systeme TextDisplay est disponible.
     * Retourne toujours true car TextDisplay est natif a Bukkit 1.19.4+.
     */
    public static boolean isProtocolLibAvailable() {
        // TextDisplay est toujours disponible dans Bukkit 1.19.4+
        return true;
    }

    /**
     * Affiche un indicateur de degats
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        TextDisplayDamageIndicator.display(plugin, location, damage, critical, viewer);
    }

    /**
     * Version legacy compatible
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        TextDisplayDamageIndicator.display(plugin, location, damage, critical);
    }

    /**
     * Affiche un indicateur de soin
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount, Player viewer) {
        TextDisplayDamageIndicator.displayHeal(plugin, location, amount, viewer);
    }

    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        TextDisplayDamageIndicator.displayHeal(plugin, location, amount);
    }

    /**
     * Affiche un indicateur d'esquive
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location, Player viewer) {
        TextDisplayDamageIndicator.displayDodge(plugin, location, viewer);
    }

    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        TextDisplayDamageIndicator.displayDodge(plugin, location);
    }

    /**
     * Affiche un indicateur de headshot
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        TextDisplayDamageIndicator.displayHeadshot(plugin, location, damage, viewer);
    }

    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage) {
        TextDisplayDamageIndicator.displayHeadshot(plugin, location, damage);
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        TextDisplayDamageIndicator.displayBlock(plugin, location, viewer);
    }

    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        TextDisplayDamageIndicator.displayBlock(plugin, location);
    }

    /**
     * Affiche un indicateur d'immunite
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        TextDisplayDamageIndicator.displayImmune(plugin, location, viewer);
    }

    /**
     * Affiche un indicateur de combo
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        TextDisplayDamageIndicator.displayCombo(plugin, location, comboCount, viewer);
    }

    /**
     * Affiche un indicateur de dégâts pour l'Épée Dansante (couleur violette spéciale)
     */
    public static void displayDancingSword(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        TextDisplayDamageIndicator.displayDancingSword(plugin, location, damage, viewer);
    }

    /**
     * Nettoie le cache des indicateurs
     */
    public static void cleanup() {
        TextDisplayDamageIndicator.cleanup();
    }
}
