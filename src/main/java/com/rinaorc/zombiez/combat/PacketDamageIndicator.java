package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Facade pour le système d'indicateurs de dégâts.
 *
 * Délègue vers FluidDamageIndicator qui utilise une approche "Fire & Forget" :
 * - 2-3 packets seulement au lieu de 20-30 par indicateur
 * - Animation 100% client-side via interpolation native (60Hz+)
 * - Charge serveur minimale, idéal pour 200 joueurs
 *
 * Fallback vers TextDisplayDamageIndicator en cas de besoin.
 *
 * @author Rinaorc Studio
 * @see FluidDamageIndicator
 */
public class PacketDamageIndicator {

    /**
     * Vérifie si le système est disponible.
     * Retourne toujours true car TextDisplay est natif à Bukkit 1.19.4+.
     */
    public static boolean isProtocolLibAvailable() {
        return true;
    }

    /**
     * Affiche un indicateur de dégâts ultra-fluide
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        FluidDamageIndicator.display(plugin, location, damage, critical, viewer);
    }

    /**
     * Version legacy compatible
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        FluidDamageIndicator.display(plugin, location, damage, critical);
    }

    /**
     * Affiche un indicateur de soin
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount, Player viewer) {
        FluidDamageIndicator.displayHeal(plugin, location, amount, viewer);
    }

    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        FluidDamageIndicator.displayHeal(plugin, location, amount);
    }

    /**
     * Affiche un indicateur d'esquive
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location, Player viewer) {
        FluidDamageIndicator.displayDodge(plugin, location, viewer);
    }

    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        FluidDamageIndicator.displayDodge(plugin, location);
    }

    /**
     * Affiche un indicateur de headshot
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        FluidDamageIndicator.displayHeadshot(plugin, location, damage, viewer);
    }

    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage) {
        FluidDamageIndicator.displayHeadshot(plugin, location, damage);
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        FluidDamageIndicator.displayBlock(plugin, location, viewer);
    }

    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        FluidDamageIndicator.displayBlock(plugin, location);
    }

    /**
     * Affiche un indicateur d'immunité
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        FluidDamageIndicator.displayImmune(plugin, location, viewer);
    }

    /**
     * Affiche un indicateur de combo
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        FluidDamageIndicator.displayCombo(plugin, location, comboCount, viewer);
    }

    /**
     * Affiche un indicateur de dégâts pour l'Épée Dansante (couleur violette spéciale)
     */
    public static void displayDancingSword(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        FluidDamageIndicator.displayDancingSword(plugin, location, damage, viewer);
    }

    /**
     * Nettoie le cache des indicateurs
     */
    public static void cleanup() {
        FluidDamageIndicator.cleanup();
    }
}
