package com.rinaorc.zombiez.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

/**
 * Utilitaires pour les barres de vie des entités
 */
public class HealthBarUtils {

    private static final String HEALTH_BAR_FULL = "█";
    private static final String HEALTH_BAR_EMPTY = "░";
    private static final int BAR_LENGTH = 10;

    /**
     * Génère une barre de vie colorée
     * @param current Vie actuelle
     * @param max Vie maximum
     * @return Barre de vie formatée
     */
    public static String createHealthBar(double current, double max) {
        double percent = Math.max(0, Math.min(1, current / max));
        int filledBars = (int) Math.round(percent * BAR_LENGTH);

        StringBuilder bar = new StringBuilder();

        // Couleur selon le pourcentage de vie
        String color;
        if (percent > 0.6) {
            color = "§a"; // Vert
        } else if (percent > 0.3) {
            color = "§e"; // Jaune
        } else {
            color = "§c"; // Rouge
        }

        bar.append(color);
        for (int i = 0; i < filledBars; i++) {
            bar.append(HEALTH_BAR_FULL);
        }

        bar.append("§8");
        for (int i = filledBars; i < BAR_LENGTH; i++) {
            bar.append(HEALTH_BAR_EMPTY);
        }

        return bar.toString();
    }

    /**
     * Génère le nom complet d'un zombie avec sa barre de vie
     * @param baseName Nom de base du zombie
     * @param level Niveau du zombie
     * @param current Vie actuelle
     * @param max Vie maximum
     * @param affixPrefix Préfixe d'affix (peut être null)
     * @param affixColor Couleur de l'affix (peut être null)
     * @return Nom formaté avec barre de vie
     */
    public static String createZombieNameWithHealth(String baseName, int level, double current, double max,
                                                     String affixPrefix, String affixColor) {
        StringBuilder name = new StringBuilder();

        // Affix si présent
        if (affixPrefix != null && !affixPrefix.isEmpty()) {
            name.append(affixColor != null ? affixColor : "§c");
            name.append(affixPrefix).append(" ");
        }

        // Nom du zombie
        name.append("§c").append(baseName);

        // Niveau
        name.append(" §7[Lv.").append(level).append("]");

        // Nouvelle ligne avec barre de vie
        name.append("\n");
        name.append(createHealthBar(current, max));
        name.append(" §f").append(String.format("%.0f", current)).append("§7/§f").append(String.format("%.0f", max));

        return name.toString();
    }

    /**
     * Met à jour le nom d'une entité avec sa barre de vie
     * @param entity L'entité à mettre à jour
     * @param baseName Le nom de base
     * @param level Le niveau
     * @param affixPrefix Préfixe d'affix (peut être null)
     * @param affixColor Couleur de l'affix (peut être null)
     */
    public static void updateEntityHealthBar(LivingEntity entity, String baseName, int level,
                                              String affixPrefix, String affixColor) {
        double currentHealth = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();

        String newName = createZombieNameWithHealth(baseName, level, currentHealth, maxHealth, affixPrefix, affixColor);
        entity.setCustomName(newName);
        entity.setCustomNameVisible(true);
    }

    /**
     * Version simplifiée pour mettre à jour rapidement
     */
    public static void updateHealthDisplay(LivingEntity entity) {
        if (entity.getCustomName() == null) return;

        double currentHealth = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();

        // Extraire le nom de base (première ligne avant \n)
        String currentName = entity.getCustomName();
        String basePart = currentName.contains("\n") ? currentName.split("\n")[0] : currentName;

        // Reconstruire avec nouvelle barre de vie
        String newName = basePart + "\n" + createHealthBar(currentHealth, maxHealth) +
                " §f" + String.format("%.0f", currentHealth) + "§7/§f" + String.format("%.0f", maxHealth);

        entity.setCustomName(newName);
    }
}
