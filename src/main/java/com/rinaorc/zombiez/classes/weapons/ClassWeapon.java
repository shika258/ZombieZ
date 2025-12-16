package com.rinaorc.zombiez.classes.weapons;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

/**
 * Système d'armes de classe avec évolution par zone
 *
 * PHILOSOPHIE:
 * - Les armes de base sont débloquées niveau 1 de classe
 * - Les armes légendaires sont débloquées niveau 10 de classe
 * - TOUTES les armes peuvent évoluer en fonction des zones complétées
 * - L'évolution apporte +10% dégâts et +1% effet par niveau d'évolution
 * - Max evolution = 10 niveaux (zone 5 à 15)
 *
 * Cela évite que le joueur niveau 10 de classe n'ait plus de progression d'arme.
 */
@Getter
public class ClassWeapon {

    private final String id;
    private final String name;
    private final String description;
    private final ClassType requiredClass;
    private final WeaponTier tier;
    private final Material material;
    private final int requiredLevel;      // 0 = de base, 10 = légendaire

    // Stats de base
    private final double baseDamage;
    private final double attackSpeed;
    private final double critChance;

    // Effet spécial
    private final WeaponEffect effect;
    private final double baseEffectValue;  // Renommé pour clarifier
    private final Sound attackSound;

    // Constantes d'évolution
    public static final int MIN_ZONE_FOR_EVOLUTION = 5;
    public static final int MAX_EVOLUTION_LEVEL = 10;
    public static final double DAMAGE_PER_EVOLUTION = 0.10;  // +10% par niveau
    public static final double EFFECT_PER_EVOLUTION = 0.05;  // +5% par niveau

    public ClassWeapon(String id, String name, String description, ClassType requiredClass,
                       WeaponTier tier, Material material, int requiredLevel,
                       double baseDamage, double attackSpeed, double critChance,
                       WeaponEffect effect, double effectValue, Sound attackSound) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredClass = requiredClass;
        this.tier = tier;
        this.material = material;
        this.requiredLevel = requiredLevel;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.critChance = critChance;
        this.effect = effect;
        this.baseEffectValue = effectValue;
        this.attackSound = attackSound;
    }

    /**
     * Calcule le niveau d'évolution basé sur la zone maximale atteinte
     * Zone 5 = évolution 1, Zone 6 = évolution 2, etc.
     * Max évolution 10 à zone 15+
     */
    public static int getEvolutionLevel(int maxZoneCompleted) {
        if (maxZoneCompleted < MIN_ZONE_FOR_EVOLUTION) return 0;
        return Math.min(maxZoneCompleted - MIN_ZONE_FOR_EVOLUTION + 1, MAX_EVOLUTION_LEVEL);
    }

    /**
     * Calcule les dégâts avec niveau de classe ET évolution
     */
    public double getScaledDamage(int classLevel, int maxZoneCompleted) {
        // Multiplicateur de niveau de classe (+5% par niveau)
        double levelMultiplier = 1.0 + (classLevel * 0.05);

        // Multiplicateur de tier
        double tierMultiplier = tier == WeaponTier.LEGENDARY ? 1.5 : 1.0;

        // Multiplicateur d'évolution (+10% par niveau d'évolution)
        int evolutionLevel = getEvolutionLevel(maxZoneCompleted);
        double evolutionMultiplier = 1.0 + (evolutionLevel * DAMAGE_PER_EVOLUTION);

        return baseDamage * levelMultiplier * tierMultiplier * evolutionMultiplier;
    }

    /**
     * Version legacy pour compatibilité (sans évolution)
     */
    public double getScaledDamage(int classLevel) {
        return getScaledDamage(classLevel, 0);
    }

    /**
     * Calcule la valeur de l'effet avec évolution
     */
    public double getScaledEffectValue(int maxZoneCompleted) {
        int evolutionLevel = getEvolutionLevel(maxZoneCompleted);
        return baseEffectValue * (1.0 + evolutionLevel * EFFECT_PER_EVOLUTION);
    }

    /**
     * Vérifie si l'arme est débloquée
     */
    public boolean isUnlocked(int classLevel) {
        return classLevel >= requiredLevel;
    }

    /**
     * Génère le lore avec informations d'évolution
     */
    public List<String> getLore(int classLevel, int maxZoneCompleted, boolean isCorrectClass) {
        List<String> lore = new ArrayList<>();
        double scaledDamage = getScaledDamage(classLevel, maxZoneCompleted);
        int evolutionLevel = getEvolutionLevel(maxZoneCompleted);
        double scaledEffect = getScaledEffectValue(maxZoneCompleted);

        lore.add("");
        lore.add(tier.getColor() + "● " + tier.getDisplayName());

        // Afficher le niveau d'évolution
        if (evolutionLevel > 0) {
            lore.add(getEvolutionDisplay(evolutionLevel));
        }

        lore.add("");
        lore.add("§7" + description);
        lore.add("");

        // Stats
        lore.add("§c⚔ Dégâts: §f" + String.format("%.0f", scaledDamage));
        lore.add("§e⚡ Vitesse: §f" + String.format("%.1f", attackSpeed));
        lore.add("§6✦ Critique: §f" + String.format("%.0f", critChance) + "%");
        lore.add("");

        // Effet spécial avec évolution
        lore.add("§d✧ Effet: " + effect.getDisplayName());
        lore.add("§8  " + effect.getDescription(scaledEffect));

        // Progression d'évolution
        if (evolutionLevel < MAX_EVOLUTION_LEVEL) {
            int nextZone = MIN_ZONE_FOR_EVOLUTION + evolutionLevel;
            lore.add("");
            lore.add("§8Évolution suivante: Zone " + nextZone);
        }

        lore.add("");
        lore.add("§9Classe: " + requiredClass.getColoredName());
        lore.add("");

        // État
        if (!isCorrectClass) {
            lore.add("§c✗ Classe incorrecte");
        } else if (!isUnlocked(classLevel)) {
            lore.add("§c✗ Niveau §4" + requiredLevel + " §crequis");
        } else {
            lore.add("§a✓ Utilisable");
        }

        return lore;
    }

    /**
     * Version legacy pour compatibilité
     */
    public List<String> getLore(int classLevel, boolean isCorrectClass) {
        return getLore(classLevel, 0, isCorrectClass);
    }

    /**
     * Génère l'affichage du niveau d'évolution
     */
    private String getEvolutionDisplay(int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("★");
        }
        for (int i = level; i < MAX_EVOLUTION_LEVEL; i++) {
            stars.append("☆");
        }

        String color;
        if (level >= 8) color = "§6";       // Or
        else if (level >= 5) color = "§e";  // Jaune
        else color = "§f";                  // Blanc

        return color + "⚔ Évolution " + level + "/" + MAX_EVOLUTION_LEVEL + " §8[" + stars + "§8]";
    }

    /**
     * Obtient le nom avec niveau d'évolution
     */
    public String getDisplayName(int maxZoneCompleted) {
        int evolutionLevel = getEvolutionLevel(maxZoneCompleted);
        if (evolutionLevel == 0) {
            return tier.getColor() + name;
        }
        return tier.getColor() + name + " §8+§f" + evolutionLevel;
    }

    /**
     * Tiers d'armes simplifié
     */
    @Getter
    public enum WeaponTier {
        BASE("Arme de Classe", "§a"),
        LEGENDARY("Arme Légendaire", "§c");

        private final String displayName;
        private final String color;

        WeaponTier(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Effets spéciaux simplifiés
     */
    @Getter
    public enum WeaponEffect {
        // Guerrier
        LIFESTEAL("Vol de Vie", "Soigne de {value}% des dégâts infligés"),
        CLEAVE("Balayage", "Touche {value} ennemis supplémentaires"),

        // Chasseur
        PIERCE("Perforant", "Traverse jusqu'à {value} ennemis"),
        CRIT_BOOST("Critique Amélioré", "+{value}% dégâts critiques"),

        // Occultiste
        AOE_BLAST("Explosion Magique", "Explose et inflige {value}% des dégâts en zone"),
        MANA_DRAIN("Drain de Mana", "Restaure {value} énergie par coup");

        private final String displayName;
        private final String descriptionTemplate;

        WeaponEffect(String displayName, String descriptionTemplate) {
            this.displayName = displayName;
            this.descriptionTemplate = descriptionTemplate;
        }

        public String getDescription(double value) {
            return descriptionTemplate.replace("{value}", String.format("%.0f", value));
        }
    }
}
