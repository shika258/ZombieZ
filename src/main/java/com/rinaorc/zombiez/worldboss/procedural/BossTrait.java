package com.rinaorc.zombiez.worldboss.procedural;

import lombok.Getter;
import org.bukkit.Color;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Traits procéduraux que les boss peuvent avoir
 * Chaque trait modifie le comportement et l'apparence du boss de manière unique
 */
@Getter
public enum BossTrait {

    // Traits offensifs
    ENRAGED("Enragé", "§c", Color.RED, 1.5, 1.0, 0.7, 1.0, "+50% dégâts, capacité 30% plus rapide"),
    BERSERKER("Berserker", "§4", Color.MAROON, 1.3, 0.7, 1.2, 1.0, "+30% dégâts, -30% vie"),
    VENOMOUS("Venimeux", "§2", Color.GREEN, 1.0, 1.0, 1.0, 1.0, "Empoisonne les joueurs touchés"),
    EXPLOSIVE("Explosif", "§6", Color.ORANGE, 1.0, 0.9, 1.1, 1.0, "Explose à la mort"),

    // Traits défensifs
    ARMORED("Blindé", "§7", Color.GRAY, 0.7, 1.3, 0.8, 1.2, "Très résistant mais plus lent"),
    REGENERATING("Régénérant", "§a", Color.LIME, 0.8, 1.1, 1.0, 1.0, "Se soigne lentement"),
    VAMPIRIC("Vampirique", "§5", Color.PURPLE, 1.0, 1.0, 1.0, 1.0, "Vol de vie sur attaque"),
    THORNS("Épineux", "§8", Color.GRAY, 0.9, 1.2, 0.9, 1.0, "Renvoie 20% des dégâts reçus"),

    // Traits de mobilité
    SWIFT("Rapide", "§e", Color.YELLOW, 1.2, 0.8, 1.0, 1.4, "Se déplace très rapidement"),
    TELEPORTER("Téléporteur", "§d", Color.FUCHSIA, 1.0, 0.9, 1.0, 1.1, "Se téléporte aléatoirement"),
    PHASING("Spectral", "§f", Color.WHITE, 1.1, 0.85, 1.0, 1.0, "Devient intangible brièvement"),

    // Traits de capacité
    EMPOWERED("Surpuissant", "§b", Color.AQUA, 1.0, 1.0, 0.7, 1.0, "Capacité 30% plus rapide"),
    RELENTLESS("Implacable", "§4", Color.MAROON, 1.0, 1.0, 0.9, 1.0, "Ne peut pas être repoussé"),
    CURSED("Maudit", "§5", Color.PURPLE, 1.0, 1.0, 1.0, 1.0, "Applique Darkness aux joueurs proches"),

    // Traits environnementaux
    BURNING("Ardent", "§6", Color.ORANGE, 1.0, 1.0, 1.1, 1.0, "Enflamme les joueurs proches"),
    FROZEN("Glacial", "§b", Color.fromRGB(173, 216, 230), 0.9, 1.1, 1.0, 0.85, "Ralentit les joueurs proches"),
    STORMY("Orageux", "§9", Color.BLUE, 1.0, 1.0, 1.0, 1.0, "Invoque des éclairs périodiquement");

    private final String displayName;
    private final String colorCode;
    private final Color particleColor;
    private final double damageMultiplier;
    private final double healthMultiplier;
    private final double abilityMultiplier;
    private final double speedMultiplier;
    private final String description;

    BossTrait(String displayName, String colorCode, Color particleColor,
              double damageMultiplier, double healthMultiplier,
              double abilityMultiplier, double speedMultiplier, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.particleColor = particleColor;
        this.damageMultiplier = damageMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.abilityMultiplier = abilityMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.description = description;
    }

    /**
     * Obtient un trait aléatoire
     */
    public static BossTrait random() {
        BossTrait[] traits = values();
        return traits[ThreadLocalRandom.current().nextInt(traits.length)];
    }

    /**
     * Obtient plusieurs traits aléatoires uniques
     */
    public static BossTrait[] randomUnique(int count) {
        return randomUnique(count, ThreadLocalRandom.current());
    }

    /**
     * Obtient plusieurs traits aléatoires uniques avec un Random spécifique (pour seed)
     */
    public static BossTrait[] randomUnique(int count, java.util.Random random) {
        BossTrait[] all = values();
        if (count >= all.length) {
            return all.clone();
        }

        java.util.List<BossTrait> available = new java.util.ArrayList<>(java.util.Arrays.asList(all));
        java.util.Collections.shuffle(available, random);

        BossTrait[] result = new BossTrait[count];
        for (int i = 0; i < count; i++) {
            result[i] = available.get(i);
        }
        return result;
    }
}
