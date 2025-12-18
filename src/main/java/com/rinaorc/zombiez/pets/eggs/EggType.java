package com.rinaorc.zombiez.pets.eggs;

import com.rinaorc.zombiez.pets.PetRarity;
import com.rinaorc.zombiez.pets.PetType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;

/**
 * Types d'oeufs de Pet
 * Chaque type a des taux de drop et garanties différents
 */
@Getter
public enum EggType {

    STANDARD(
        "Oeuf Standard",
        "§f",
        Material.EGG,
        null,           // Pas de rareté minimum garantie
        0.0,            // Pas de boost légendaire
        500,            // Prix en points
        new double[]{45, 30, 15, 7, 2.5, 0.5}  // Drop rates par rareté
    ),

    ZONE(
        "Oeuf de Zone",
        "§e",
        Material.TURTLE_EGG,
        PetRarity.RARE, // Rare minimum garanti
        0.0,
        2000,
        new double[]{0, 0, 60, 30, 8, 2}
    ),

    ELITE(
        "Oeuf Élite",
        "§d",
        Material.DRAGON_EGG,
        PetRarity.EPIC, // Épique minimum garanti
        0.10,           // 10% chance légendaire boost
        5000,
        new double[]{0, 0, 0, 70, 25, 5}
    ),

    LEGENDARY(
        "Oeuf Légendaire",
        "§6",
        Material.SNIFFER_EGG,
        PetRarity.LEGENDARY, // Légendaire garanti
        0.05,                // 5% chance mythique
        15000,
        new double[]{0, 0, 0, 0, 95, 5}
    ),

    MYTHIC(
        "Oeuf Mythique",
        "§c",
        Material.ENDER_EYE,
        PetRarity.MYTHIC, // Mythique garanti
        0.0,
        -1,              // Non achetable
        new double[]{0, 0, 0, 0, 0, 100}
    );

    private final String displayName;
    private final String color;
    private final Material icon;
    private final PetRarity minimumRarity;
    private final double legendaryBoost;
    private final int pointsCost;
    private final double[] rarityRates;

    EggType(String displayName, String color, Material icon, PetRarity minimumRarity,
            double legendaryBoost, int pointsCost, double[] rarityRates) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.minimumRarity = minimumRarity;
        this.legendaryBoost = legendaryBoost;
        this.pointsCost = pointsCost;
        this.rarityRates = rarityRates;
    }

    /**
     * Obtient le nom coloré
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtient une description des raretés possibles
     */
    public String getRarityInfo() {
        StringBuilder sb = new StringBuilder();
        PetRarity[] rarities = PetRarity.values();
        boolean first = true;

        for (int i = 0; i < rarities.length && i < rarityRates.length; i++) {
            if (rarityRates[i] > 0) {
                if (!first) sb.append("§7, ");
                sb.append(rarities[i].getColor()).append(rarities[i].getDisplayName());
                first = false;
            }
        }

        if (minimumRarity != null) {
            sb.append(" §7(").append(minimumRarity.getColor()).append(minimumRarity.getDisplayName()).append("§7+ garanti)");
        }

        return sb.toString();
    }

    /**
     * Vérifie si cet oeuf est achetable
     */
    public boolean isPurchasable() {
        return pointsCost > 0;
    }

    /**
     * Tire une rareté au sort selon les taux de cet oeuf
     */
    public PetRarity rollRarity(double luckBonus) {
        double roll = Math.random() * 100;
        double cumulative = 0;

        PetRarity[] rarities = PetRarity.values();

        // Parcourir de la plus rare à la moins rare
        for (int i = rarities.length - 1; i >= 0; i--) {
            double rate = rarityRates[i] * (1 + luckBonus);
            if (legendaryBoost > 0 && i >= PetRarity.LEGENDARY.ordinal()) {
                rate *= (1 + legendaryBoost);
            }
            cumulative += rate;

            if (roll < cumulative) {
                PetRarity rolled = rarities[i];
                // Appliquer le minimum garanti
                if (minimumRarity != null && rolled.ordinal() < minimumRarity.ordinal()) {
                    return minimumRarity;
                }
                return rolled;
            }
        }

        // Fallback avec minimum garanti
        return minimumRarity != null ? minimumRarity : PetRarity.COMMON;
    }

    /**
     * Tire un pet au sort depuis cet oeuf
     */
    public PetType rollPet(double luckBonus, PetRarity pityGuarantee) {
        PetRarity rarity = rollRarity(luckBonus);

        // Appliquer la garantie pity si supérieure
        if (pityGuarantee != null && pityGuarantee.ordinal() > rarity.ordinal()) {
            rarity = pityGuarantee;
        }

        // Obtenir un pet aléatoire de cette rareté
        PetType[] petsOfRarity = PetType.getByRarity(rarity);
        if (petsOfRarity.length == 0) {
            // Fallback: prendre n'importe quel pet
            return PetType.values()[(int) (Math.random() * PetType.values().length)];
        }

        return petsOfRarity[(int) (Math.random() * petsOfRarity.length)];
    }

    /**
     * Tire un pet au sort depuis cet oeuf avec pool restreint (zone)
     */
    public PetType rollPetFromPool(double luckBonus, PetRarity pityGuarantee, PetType[] pool) {
        if (pool == null || pool.length == 0) {
            return rollPet(luckBonus, pityGuarantee);
        }

        PetRarity rarity = rollRarity(luckBonus);
        if (pityGuarantee != null && pityGuarantee.ordinal() > rarity.ordinal()) {
            rarity = pityGuarantee;
        }

        // Filtrer le pool par rareté
        PetRarity finalRarity = rarity;
        PetType[] filteredPool = Arrays.stream(pool)
            .filter(p -> p.getRarity() == finalRarity)
            .toArray(PetType[]::new);

        if (filteredPool.length == 0) {
            // Prendre n'importe lequel du pool
            return pool[(int) (Math.random() * pool.length)];
        }

        return filteredPool[(int) (Math.random() * filteredPool.length)];
    }

    /**
     * Obtient un EggType depuis son nom
     */
    public static EggType fromName(String name) {
        if (name == null) return null;
        for (EggType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
