package com.rinaorc.zombiez.worldboss;

import lombok.Getter;

/**
 * Types de World Boss disponibles
 * Chaque type a ses propres mécaniques de combat uniques
 */
@Getter
public enum WorldBossType {

    /**
     * Le Boucher (Tank - Taille x3)
     * Capacité: Attire tous les joueurs proches vers lui toutes les 15s (Hook)
     * Stratégie: Plus il frappe, plus il gagne de la résistance. Il faut le kiter.
     */
    THE_BUTCHER(
        "Le Boucher",
        "§4§lLE BOUCHER",
        3.0,
        2000,
        25,
        15,
        "Hook - Attire les joueurs vers lui"
    ),

    /**
     * L'Ombre Instable (Vitesse - Taille x1.5)
     * Capacité: Devient invisible pendant 3s toutes les 10s et réapparaît derrière un joueur
     * Stratégie: Utiliser des sons/particules pour anticiper son retour
     */
    SHADOW_UNSTABLE(
        "L'Ombre Instable",
        "§8§lL'OMBRE INSTABLE",
        1.5,
        1200,
        40,
        10,
        "Invisibilité - Téléportation derrière les joueurs"
    ),

    /**
     * Le Pyromancien Zombie (Magie - Taille x2)
     * Capacité: Crée un cercle de feu au sol. Si un joueur reste dedans, le boss se soigne.
     * Stratégie: Forcer le boss à sortir de sa zone de feu
     */
    PYROMANCER(
        "Le Pyromancien Zombie",
        "§6§lLE PYROMANCIEN ZOMBIE",
        2.0,
        1500,
        30,
        12,
        "Cercle de feu - Se soigne si joueurs dedans"
    ),

    /**
     * La Reine de la Horde (Invocatrice - Taille x4)
     * Capacité: Invoque 5 zombies rapides tous les 25% de vie perdus. Invincible tant que sbires en vie.
     * Stratégie: Focus les adds avant le boss
     */
    HORDE_QUEEN(
        "La Reine de la Horde",
        "§5§lLA REINE DE LA HORDE",
        4.0,
        2500,
        20,
        0, // Pas de cooldown classique, basé sur vie
        "Invocation - Invincible tant que sbires en vie"
    ),

    /**
     * Le Brise-Glace (Contrôle - Taille x3)
     * Capacité: Applique "Slowness X" et gèle le sol autour de lui
     * Stratégie: Utiliser des projectiles à distance car le corps-à-corps est mortel
     */
    ICE_BREAKER(
        "Le Brise-Glace",
        "§b§lLE BRISE-GLACE",
        3.0,
        1800,
        35,
        8,
        "Gel - Ralentit et gèle le sol"
    );

    private final String displayName;
    private final String titleName;
    private final double scale;
    private final double baseHealth;
    private final double baseDamage;
    private final int abilityCooldownSeconds;
    private final String abilityDescription;

    WorldBossType(String displayName, String titleName, double scale, double baseHealth,
                  double baseDamage, int abilityCooldownSeconds, String abilityDescription) {
        this.displayName = displayName;
        this.titleName = titleName;
        this.scale = scale;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.abilityCooldownSeconds = abilityCooldownSeconds;
        this.abilityDescription = abilityDescription;
    }

    /**
     * Obtient un type aléatoire
     */
    public static WorldBossType random() {
        WorldBossType[] types = values();
        return types[(int) (Math.random() * types.length)];
    }

    /**
     * Calcule la vie du boss selon le niveau de zone
     * Formule: baseHealth * (1 + zoneId * 0.15)
     */
    public double calculateHealth(int zoneId) {
        return baseHealth * (1 + zoneId * 0.15);
    }

    /**
     * Calcule les dégâts du boss selon le niveau de zone
     */
    public double calculateDamage(int zoneId) {
        return baseDamage * (1 + zoneId * 0.05);
    }
}
