package com.rinaorc.zombiez.classes.weapons;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

/**
 * Système d'armes de classe simplifié
 * Chaque classe a 2 armes exclusives:
 * - 1 arme de BASE (niveau 1)
 * - 1 arme LÉGENDAIRE (niveau 10)
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

    // Stats
    private final double baseDamage;
    private final double attackSpeed;
    private final double critChance;

    // Effet spécial
    private final WeaponEffect effect;
    private final double effectValue;
    private final Sound attackSound;

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
        this.effectValue = effectValue;
        this.attackSound = attackSound;
    }

    /**
     * Calcule les dégâts scalés selon le niveau de classe
     */
    public double getScaledDamage(int classLevel) {
        double levelMultiplier = 1.0 + (classLevel * 0.05);
        double tierMultiplier = tier == WeaponTier.LEGENDARY ? 1.5 : 1.0;
        return baseDamage * levelMultiplier * tierMultiplier;
    }

    /**
     * Vérifie si l'arme est débloquée
     */
    public boolean isUnlocked(int classLevel) {
        return classLevel >= requiredLevel;
    }

    /**
     * Génère le lore pour l'affichage
     */
    public List<String> getLore(int classLevel, boolean isCorrectClass) {
        List<String> lore = new ArrayList<>();
        double scaledDamage = getScaledDamage(classLevel);

        lore.add("");
        lore.add(tier.getColor() + "● " + tier.getDisplayName());
        lore.add("");
        lore.add("§7" + description);
        lore.add("");

        // Stats
        lore.add("§c⚔ Dégâts: §f" + String.format("%.0f", scaledDamage));
        lore.add("§e⚡ Vitesse: §f" + String.format("%.1f", attackSpeed));
        lore.add("§6✦ Critique: §f" + String.format("%.0f", critChance) + "%");
        lore.add("");

        // Effet spécial
        lore.add("§d✧ Effet: " + effect.getDisplayName());
        lore.add("§8  " + effect.getDescription(effectValue));
        lore.add("");

        // Classe requise
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
