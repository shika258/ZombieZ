package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Gestionnaire du système d'Item Level (ILVL)
 * Inspiré du système de World of Warcraft
 *
 * L'Item Level représente la puissance réelle d'un objet,
 * indépendamment de sa rareté.
 */
public class ItemLevelManager {

    private static final Random RANDOM = new Random();

    // Plages d'ILVL par rareté (configurables)
    @Getter
    private final Map<Rarity, ILVLRange> ilvlRanges = new HashMap<>();

    // Progression d'ILVL par zone (configurable)
    private int baseILVLPerZone = 10;
    private int maxILVL = 100;

    public ItemLevelManager() {
        initializeDefaultRanges();
    }

    /**
     * Initialise les plages d'ILVL par défaut
     */
    private void initializeDefaultRanges() {
        ilvlRanges.put(Rarity.COMMON, new ILVLRange(1, 20));
        ilvlRanges.put(Rarity.UNCOMMON, new ILVLRange(10, 35));
        ilvlRanges.put(Rarity.RARE, new ILVLRange(15, 40));
        ilvlRanges.put(Rarity.EPIC, new ILVLRange(35, 70));
        ilvlRanges.put(Rarity.LEGENDARY, new ILVLRange(50, 100));
        ilvlRanges.put(Rarity.MYTHIC, new ILVLRange(70, 100));
        ilvlRanges.put(Rarity.EXALTED, new ILVLRange(85, 100));
    }

    /**
     * Charge la configuration depuis un ConfigurationSection
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        baseILVLPerZone = config.getInt("base-ilvl-per-zone", 10);
        maxILVL = config.getInt("max-ilvl", 100);

        ConfigurationSection rangesSection = config.getConfigurationSection("rarity-ranges");
        if (rangesSection != null) {
            for (Rarity rarity : Rarity.values()) {
                String key = rarity.name().toLowerCase();
                if (rangesSection.contains(key)) {
                    int min = rangesSection.getInt(key + ".min", 1);
                    int max = rangesSection.getInt(key + ".max", 20);
                    ilvlRanges.put(rarity, new ILVLRange(min, max));
                }
            }
        }
    }

    /**
     * Calcule l'Item Level pour un item
     *
     * @param zoneLevel Niveau de la zone où l'item est droppé
     * @param rarity Rareté de l'item
     * @return L'Item Level calculé
     */
    public int calculateItemLevel(int zoneLevel, Rarity rarity) {
        ILVLRange range = ilvlRanges.getOrDefault(rarity, new ILVLRange(1, 20));

        // ILVL de base selon la zone
        int baseILVL = Math.min(zoneLevel * baseILVLPerZone, maxILVL);

        // Ajuster selon la plage de la rareté
        int minForRarity = range.getMin();
        int maxForRarity = range.getMax();

        // Calculer l'ILVL final dans la plage de la rareté
        // Plus la zone est élevée, plus on se rapproche du max de la plage
        double zoneProgression = Math.min(1.0, zoneLevel / 10.0); // 0.0 à 1.0

        int rangeSpan = maxForRarity - minForRarity;
        int progressionBonus = (int) (rangeSpan * zoneProgression);

        int finalILVL = minForRarity + progressionBonus;

        // Variation aléatoire de ±10%
        int variation = (int) (finalILVL * 0.1);
        finalILVL += RANDOM.nextInt(variation * 2 + 1) - variation;

        // Clamp dans les limites
        finalILVL = Math.max(minForRarity, Math.min(maxForRarity, finalILVL));
        finalILVL = Math.min(finalILVL, maxILVL);

        return finalILVL;
    }

    /**
     * Obtient la plage d'ILVL pour une rareté
     */
    public ILVLRange getRangeForRarity(Rarity rarity) {
        return ilvlRanges.getOrDefault(rarity, new ILVLRange(1, 20));
    }

    /**
     * Calcule un facteur de scaling basé sur l'ILVL
     * Utilisé pour scaler les pouvoirs
     *
     * @param itemLevel L'Item Level
     * @return Un facteur multiplicateur (ex: 1.0 pour ILVL 10, 10.0 pour ILVL 100)
     */
    public double getScalingFactor(int itemLevel) {
        return itemLevel / 10.0;
    }

    /**
     * Obtient la couleur d'affichage selon l'ILVL
     */
    public String getILVLColor(int itemLevel) {
        if (itemLevel >= 90) return "§c§l"; // Rouge gras
        if (itemLevel >= 75) return "§6§l"; // Orange gras
        if (itemLevel >= 60) return "§d";   // Rose
        if (itemLevel >= 45) return "§5";   // Violet
        if (itemLevel >= 30) return "§9";   // Bleu
        if (itemLevel >= 15) return "§a";   // Vert
        return "§f";                        // Blanc
    }

    /**
     * Représente une plage d'Item Level
     */
    @Getter
    public static class ILVLRange {
        private final int min;
        private final int max;

        public ILVLRange(int min, int max) {
            this.min = Math.max(1, min);
            this.max = Math.max(this.min, max);
        }

        public boolean contains(int ilvl) {
            return ilvl >= min && ilvl <= max;
        }

        public int getSpan() {
            return max - min;
        }
    }
}
