package com.rinaorc.zombiez.classes.mutations;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Représente une mutation quotidienne
 * Les mutations changent les règles du jeu pendant 24h
 * Elles ajoutent de la variété et donnent une raison de revenir chaque jour
 */
@Getter
public class DailyMutation {

    private final String id;
    private final String name;
    private final String description;
    private final MutationType type;
    private final MutationRarity rarity;
    private final Material icon;

    // Effets positifs et négatifs
    private final MutationEffect positiveEffect;
    private final double positiveValue;
    private final MutationEffect negativeEffect;
    private final double negativeValue;

    // Modificateurs spéciaux
    private final boolean affectsZombies;
    private final boolean affectsPlayers;
    private final boolean affectsLoot;

    public DailyMutation(String id, String name, String description, MutationType type,
                         MutationRarity rarity, Material icon,
                         MutationEffect positiveEffect, double positiveValue,
                         MutationEffect negativeEffect, double negativeValue,
                         boolean affectsZombies, boolean affectsPlayers, boolean affectsLoot) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.rarity = rarity;
        this.icon = icon;
        this.positiveEffect = positiveEffect;
        this.positiveValue = positiveValue;
        this.negativeEffect = negativeEffect;
        this.negativeValue = negativeValue;
        this.affectsZombies = affectsZombies;
        this.affectsPlayers = affectsPlayers;
        this.affectsLoot = affectsLoot;
    }

    /**
     * Génère la description complète pour l'affichage
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("§7").append(description).append("\n\n");

        if (positiveEffect != null) {
            sb.append("§a✓ ").append(positiveEffect.getDescription(positiveValue)).append("\n");
        }
        if (negativeEffect != null) {
            sb.append("§c✗ ").append(negativeEffect.getDescription(negativeValue)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Types de mutations
     */
    @Getter
    public enum MutationType {
        COMBAT("Combat", "§c", Material.DIAMOND_SWORD, "Modifie les mécaniques de combat"),
        SURVIVAL("Survie", "§a", Material.GOLDEN_APPLE, "Modifie la survie et les ressources"),
        EXPLORATION("Exploration", "§e", Material.COMPASS, "Modifie la progression et les zones"),
        CHAOS("Chaos", "§d", Material.TNT, "Effets imprévisibles et extrêmes"),
        BLESSING("Bénédiction", "§6", Material.NETHER_STAR, "Purement bénéfique (rare)");

        private final String displayName;
        private final String color;
        private final Material icon;
        private final String description;

        MutationType(String displayName, String color, Material icon, String description) {
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
            this.description = description;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Raretés des mutations
     */
    @Getter
    public enum MutationRarity {
        COMMON("Commune", "§f", 40),
        UNCOMMON("Peu Commune", "§a", 35),
        RARE("Rare", "§9", 18),
        LEGENDARY("Légendaire", "§6", 6),
        CURSED("Maudite", "§4", 1);  // Très difficile mais très récompensante

        private final String displayName;
        private final String color;
        private final int weight;

        MutationRarity(String displayName, String color, int weight) {
            this.displayName = displayName;
            this.color = color;
            this.weight = weight;
        }

        public String getColoredName() {
            return color + displayName;
        }
    }

    /**
     * Effets des mutations
     */
    @Getter
    public enum MutationEffect {
        // Effets sur les joueurs
        PLAYER_DAMAGE("Dégâts joueurs +{value}%"),
        PLAYER_HEALTH("HP joueurs +{value}%"),
        PLAYER_SPEED("Vitesse joueurs +{value}%"),
        PLAYER_CRIT("Critique joueurs +{value}%"),
        PLAYER_LIFESTEAL("Vol de vie +{value}%"),
        PLAYER_REGEN("Régénération +{value}%"),
        PLAYER_COOLDOWN("Cooldowns -{value}%"),

        // Effets sur les zombies
        ZOMBIE_DAMAGE("Dégâts zombies +{value}%"),
        ZOMBIE_HEALTH("HP zombies +{value}%"),
        ZOMBIE_SPEED("Vitesse zombies +{value}%"),
        ZOMBIE_SPAWN_RATE("Taux de spawn +{value}%"),
        ZOMBIE_ELITE_CHANCE("Chance élite +{value}%"),
        ZOMBIE_BOSS_HEALTH("HP boss +{value}%"),

        // Effets sur le loot
        LOOT_CHANCE("Chance de loot +{value}%"),
        LOOT_RARITY("Rareté du loot +{value}%"),
        XP_GAIN("Gain d'XP +{value}%"),
        POINTS_GAIN("Gain de points +{value}%"),
        GEM_CHANCE("Chance de gemmes +{value}%"),

        // Effets spéciaux
        HEADSHOT_DAMAGE("Dégâts headshot +{value}%"),
        MELEE_DAMAGE("Dégâts mêlée +{value}%"),
        RANGED_DAMAGE("Dégâts distance +{value}%"),
        SKILL_POWER("Puissance compétences +{value}%"),
        ZONE_DIFFICULTY("Difficulté des zones +{value}"),

        // Effets environnementaux
        VISION_RANGE("Portée de vision -{value}%"),
        WEATHER_INTENSITY("Intensité météo +{value}%"),
        AMMO_CONSUMPTION("Consommation munitions +{value}%"),
        HEALING_RECEIVED("Soins reçus +{value}%");

        private final String template;

        MutationEffect(String template) {
            this.template = template;
        }

        public String getDescription(double value) {
            String sign = value >= 0 ? "+" : "";
            return template.replace("+{value}", sign + String.format("%.0f", Math.abs(value)))
                          .replace("-{value}", String.format("%.0f", Math.abs(value)))
                          .replace("{value}", String.format("%.0f", value));
        }
    }
}
