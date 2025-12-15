package com.rinaorc.zombiez.mobs.food;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un item de nourriture custom
 * avec des effets spéciaux lors de la consommation
 */
@Getter
public class FoodItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final FoodRarity rarity;

    // Effets de la nourriture
    private final int healAmount;           // Points de vie restaurés
    private final int hungerRestored;       // Points de faim restaurés
    private final float saturationBonus;    // Bonus de saturation

    // Effets temporaires
    private final List<FoodEffect> effects;

    // Clé pour identifier les items ZombieZ
    public static final NamespacedKey FOOD_KEY = new NamespacedKey("zombiez", "food_id");

    public FoodItem(String id, String displayName, Material material, FoodRarity rarity,
                    int healAmount, int hungerRestored, float saturationBonus) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.rarity = rarity;
        this.healAmount = healAmount;
        this.hungerRestored = hungerRestored;
        this.saturationBonus = saturationBonus;
        this.effects = new ArrayList<>();
        this.lore = new ArrayList<>();
    }

    /**
     * Ajoute un effet temporaire à la nourriture
     */
    public FoodItem addEffect(PotionEffectType type, int durationTicks, int amplifier) {
        effects.add(new FoodEffect(type, durationTicks, amplifier));
        return this;
    }

    /**
     * Ajoute une ligne de lore
     */
    public FoodItem addLore(String line) {
        lore.add(line);
        return this;
    }

    /**
     * Crée l'ItemStack avec les métadonnées
     */
    public ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nom avec couleur de rareté
            meta.setDisplayName(rarity.getColor() + displayName);

            // Construire le lore
            List<String> fullLore = new ArrayList<>();

            // Rareté
            fullLore.add(rarity.getColor() + rarity.getDisplayName());
            fullLore.add("");

            // Stats de base
            fullLore.add("§7Restaure:");
            if (healAmount > 0) {
                fullLore.add("  §c+" + healAmount + " §c❤ §7Vie");
            }
            if (hungerRestored > 0) {
                fullLore.add("  §6+" + hungerRestored + " §6🍖 §7Faim");
            }
            if (saturationBonus > 0) {
                fullLore.add("  §e+" + String.format("%.1f", saturationBonus) + " §7Saturation");
            }

            // Effets spéciaux
            if (!effects.isEmpty()) {
                fullLore.add("");
                fullLore.add("§dEffets spéciaux:");
                for (FoodEffect effect : effects) {
                    fullLore.add("  §d▸ §7" + effect.getDescription());
                }
            }

            // Lore custom
            if (!lore.isEmpty()) {
                fullLore.add("");
                for (String line : lore) {
                    fullLore.add("§7§o" + line);
                }
            }

            // Footer
            fullLore.add("");
            fullLore.add("§8Clic droit pour consommer");

            meta.setLore(fullLore);

            // Stocker l'ID dans les données persistantes
            meta.getPersistentDataContainer().set(FOOD_KEY, PersistentDataType.STRING, id);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Applique les effets de la nourriture à un joueur
     */
    public void applyEffects(org.bukkit.entity.Player player) {
        // Soigner le joueur
        if (healAmount > 0) {
            double newHealth = Math.min(
                player.getHealth() + healAmount,
                player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
            );
            player.setHealth(newHealth);
        }

        // Restaurer la faim
        if (hungerRestored > 0) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + hungerRestored));
        }

        // Bonus de saturation
        if (saturationBonus > 0) {
            player.setSaturation(Math.min(20, player.getSaturation() + saturationBonus));
        }

        // Appliquer les effets de potion
        for (FoodEffect effect : effects) {
            player.addPotionEffect(new PotionEffect(
                effect.type,
                effect.durationTicks,
                effect.amplifier,
                false,
                true
            ));
        }
    }

    /**
     * Effet de nourriture (potion temporaire)
     */
    @Getter
    public static class FoodEffect {
        private final PotionEffectType type;
        private final int durationTicks;
        private final int amplifier;

        public FoodEffect(PotionEffectType type, int durationTicks, int amplifier) {
            this.type = type;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }

        public String getDescription() {
            String effectName = formatEffectName(type);
            int seconds = durationTicks / 20;
            String level = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
            return effectName + level + " §8(" + seconds + "s)";
        }

        private String formatEffectName(PotionEffectType type) {
            return switch (type.getKey().getKey()) {
                case "speed" -> "Vitesse";
                case "strength" -> "Force";
                case "regeneration" -> "Régénération";
                case "resistance" -> "Résistance";
                case "fire_resistance" -> "Résistance au Feu";
                case "health_boost" -> "Boost de Vie";
                case "absorption" -> "Absorption";
                case "saturation" -> "Saturation";
                case "haste" -> "Célérité";
                case "jump_boost" -> "Saut";
                case "night_vision" -> "Vision Nocturne";
                default -> type.getKey().getKey().replace("_", " ");
            };
        }

        private String toRoman(int num) {
            return switch (num) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(num);
            };
        }
    }

    /**
     * Rareté de la nourriture
     */
    public enum FoodRarity {
        COMMON("§f", "Commun"),
        UNCOMMON("§a", "Peu Commun"),
        RARE("§9", "Rare"),
        EPIC("§5", "Épique"),
        LEGENDARY("§6", "Légendaire");

        @Getter
        private final String color;
        @Getter
        private final String displayName;

        FoodRarity(String color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
    }
}
