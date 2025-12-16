package com.rinaorc.zombiez.classes.weapons;

import com.rinaorc.zombiez.classes.ClassType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.List;

/**
 * Représente une arme exclusive à une classe
 * Ces armes ne peuvent être utilisées que par les joueurs de la classe appropriée
 */
@Getter
public class ClassWeapon {

    private final String id;
    private final String name;
    private final String description;
    private final ClassType requiredClass;
    private final WeaponTier tier;         // Tier de l'arme (BASIC, ADVANCED, LEGENDARY)
    private final Material material;        // Matériau Minecraft utilisé
    private final WeaponType type;          // Type d'arme (melee, ranged, special)
    private final String unlockTalentId;    // Talent qui débloque cette arme

    // Stats de base
    private final double baseDamage;
    private final double attackSpeed;
    private final double critChance;
    private final double critMultiplier;

    // Effet spécial de l'arme
    private final WeaponEffect specialEffect;
    private final double effectChance;      // Chance de proc (0-100)
    private final double effectValue;       // Valeur de l'effet
    private final Sound attackSound;

    public ClassWeapon(String id, String name, String description, ClassType requiredClass,
                       WeaponTier tier, Material material, WeaponType type, String unlockTalentId,
                       double baseDamage, double attackSpeed, double critChance, double critMultiplier,
                       WeaponEffect specialEffect, double effectChance, double effectValue,
                       Sound attackSound) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredClass = requiredClass;
        this.tier = tier;
        this.material = material;
        this.type = type;
        this.unlockTalentId = unlockTalentId;
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
        this.critChance = critChance;
        this.critMultiplier = critMultiplier;
        this.specialEffect = specialEffect;
        this.effectChance = effectChance;
        this.effectValue = effectValue;
        this.attackSound = attackSound;
    }

    /**
     * Calcule les dégâts scalés selon le niveau de zone
     */
    public double getScaledDamage(int zoneLevel) {
        double zoneMultiplier = 1.0 + (zoneLevel * 0.08);
        double tierMultiplier = switch (tier) {
            case BASIC -> 1.0;
            case ADVANCED -> 1.35;
            case LEGENDARY -> 1.75;
        };
        return baseDamage * zoneMultiplier * tierMultiplier;
    }

    /**
     * Génère le lore pour l'affichage
     */
    public List<String> getLore(int zoneLevel, boolean canUse, boolean isUnlocked) {
        double scaledDamage = getScaledDamage(zoneLevel);

        return List.of(
            "",
            tier.getColor() + tier.getDisplayName(),
            "",
            "§7" + description,
            "",
            "§c⚔ Dégâts: §f" + String.format("%.1f", scaledDamage),
            "§e⚡ Vitesse: §f" + String.format("%.2f", attackSpeed),
            "§6✦ Critique: §f" + String.format("%.1f", critChance) + "% (x" + String.format("%.1f", critMultiplier) + ")",
            "",
            "§d✧ Effet Spécial: §f" + specialEffect.getDisplayName(),
            "§8  " + specialEffect.getDescription(effectValue),
            "§8  Chance: " + String.format("%.0f", effectChance) + "%",
            "",
            "§9Classe requise: " + requiredClass.getColoredName(),
            "",
            !isUnlocked
                ? "§c✗ Talent requis pour débloquer"
                : (!canUse
                    ? "§c✗ Vous n'êtes pas " + requiredClass.getDisplayName()
                    : "§a✓ Vous pouvez utiliser cette arme")
        );
    }

    /**
     * Tiers d'armes de classe
     */
    @Getter
    public enum WeaponTier {
        BASIC("Basique", "§a", 1),
        ADVANCED("Avancée", "§6", 2),
        LEGENDARY("Légendaire", "§c", 3);

        private final String displayName;
        private final String color;
        private final int level;

        WeaponTier(String displayName, String color, int level) {
            this.displayName = displayName;
            this.color = color;
            this.level = level;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Types d'armes
     */
    @Getter
    public enum WeaponType {
        MELEE("Mêlée", Material.IRON_SWORD),
        RANGED("Distance", Material.BOW),
        HEAVY("Lourde", Material.CROSSBOW),
        SPECIAL("Spéciale", Material.BLAZE_ROD);

        private final String displayName;
        private final Material icon;

        WeaponType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    /**
     * Effets spéciaux des armes
     */
    @Getter
    public enum WeaponEffect {
        // Effets de dégâts
        EXPLOSIVE("Explosion", "Explose à l'impact ({value} rayon)"),
        CHAIN("Chaîne", "Les dégâts rebondissent sur {value} cibles"),
        PIERCE("Perforant", "Traverse {value} ennemis"),
        BLEED("Saignement", "Inflige {value} dégâts/s pendant 5s"),
        BURN("Brûlure", "Enflamme la cible pendant {value}s"),
        FREEZE("Gel", "Ralentit de {value}% pendant 3s"),
        SHOCK("Électrocution", "Étourdit pendant {value}s"),

        // Effets utilitaires
        LIFESTEAL("Vol de Vie", "Soigne de {value}% des dégâts"),
        EXECUTE("Exécution", "+{value}% dégâts aux <20% HP"),
        CLEAVE("Tranchant", "Touche {value} ennemis supplémentaires"),
        KNOCKBACK("Recul", "Repousse de {value} blocs"),

        // Effets spéciaux
        SUMMON("Invocation", "Invoque une créature alliée (durée {value}s)"),
        HEAL_PULSE("Impulsion Soin", "Soigne les alliés proches de {value} HP"),
        TURRET_BOOST("Boost Tourelles", "+{value}% dégâts des tourelles"),
        RAGE_BUILD("Génération Rage", "+{value} rage par coup"),
        MARK_TARGET("Marquage", "Marque la cible {value}s"),
        STEALTH_BREAK("Briseur Furtif", "+{value}% dégâts depuis l'invisibilité");

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
