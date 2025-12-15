package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Représente un pouvoir qui peut être appliqué à un item
 *
 * Les pouvoirs sont des effets spéciaux qui se déclenchent dans certaines conditions
 * (on hit, on kill, on damaged, etc.) et scalent avec l'Item Level.
 */
@Getter
public abstract class Power {

    // Identifiant unique du pouvoir
    protected final String id;

    // Nom d'affichage
    protected final String displayName;

    // Description courte
    protected final String description;

    // Chance de proc (0.0 à 1.0)
    protected double baseProcChance;

    // Cooldown interne en millisecondes
    protected long cooldownMs;

    // Raretés minimum pour obtenir ce pouvoir
    protected Rarity minimumRarity;

    // Raretés autorisées pour ce pouvoir
    protected final Set<Rarity> allowedRarities;

    // Cooldowns par joueur (UUID -> timestamp)
    protected final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public Power(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.baseProcChance = 0.1; // 10% par défaut
        this.cooldownMs = 10000; // 10 secondes par défaut
        this.minimumRarity = Rarity.RARE;
        this.allowedRarities = EnumSet.allOf(Rarity.class);
    }

    /**
     * Vérifie si le pouvoir peut se déclencher
     */
    public boolean canProc(Player player, int itemLevel) {
        // Vérifier le cooldown
        if (isOnCooldown(player)) {
            return false;
        }

        // Roll de la chance de proc (peut légèrement augmenter avec l'ILVL)
        double procChance = calculateProcChance(itemLevel);
        return Math.random() < procChance;
    }

    /**
     * Calcule la chance de proc selon l'ILVL
     */
    protected double calculateProcChance(int itemLevel) {
        // Légère augmentation avec l'ILVL (max +20% de la base)
        double ilvlBonus = (itemLevel / 100.0) * 0.2;
        return Math.min(1.0, baseProcChance * (1.0 + ilvlBonus));
    }

    /**
     * Vérifie si le pouvoir est en cooldown pour un joueur
     */
    public boolean isOnCooldown(Player player) {
        Long lastUse = playerCooldowns.get(player.getUniqueId());
        if (lastUse == null) return false;

        return System.currentTimeMillis() - lastUse < cooldownMs;
    }

    /**
     * Applique le cooldown pour un joueur
     */
    protected void applyCooldown(Player player) {
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Obtient le temps de cooldown restant en secondes
     */
    public double getRemainingCooldown(Player player) {
        Long lastUse = playerCooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;

        long remaining = cooldownMs - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining / 1000.0);
    }

    /**
     * Déclenche le pouvoir
     *
     * @param player Le joueur qui déclenche le pouvoir
     * @param target La cible (peut être null selon le type de pouvoir)
     * @param itemLevel L'Item Level de l'item qui a le pouvoir
     */
    public abstract void trigger(Player player, LivingEntity target, int itemLevel);

    /**
     * Obtient le lore à afficher sur l'item
     *
     * @param itemLevel L'Item Level de l'item
     * @return Les lignes de lore décrivant le pouvoir
     */
    public List<String> getLore(int itemLevel) {
        List<String> lore = new ArrayList<>();

        lore.add("§d§l✦ " + displayName);
        lore.add("§7" + description);
        lore.add("§8Chance: §e" + String.format("%.1f%%", calculateProcChance(itemLevel) * 100));

        // Ajouter des infos spécifiques au pouvoir (dégâts, durée, etc.)
        List<String> powerStats = getPowerStats(itemLevel);
        lore.addAll(powerStats);

        if (cooldownMs > 0) {
            lore.add("§8Cooldown: §e" + (cooldownMs / 1000) + "s");
        }

        return lore;
    }

    /**
     * Obtient les statistiques spécifiques du pouvoir pour le lore
     * À surcharger dans les sous-classes
     */
    protected List<String> getPowerStats(int itemLevel) {
        return new ArrayList<>();
    }

    /**
     * Vérifie si ce pouvoir peut être appliqué à une rareté donnée
     */
    public boolean canApplyToRarity(Rarity rarity) {
        return rarity.isAtLeast(minimumRarity) && allowedRarities.contains(rarity);
    }

    /**
     * Builder pattern pour faciliter la création de pouvoirs
     */
    public static abstract class PowerBuilder<T extends Power> {
        protected double procChance = 0.1;
        protected long cooldown = 10000;
        protected Rarity minimumRarity = Rarity.RARE;
        protected Set<Rarity> allowedRarities = EnumSet.allOf(Rarity.class);

        public PowerBuilder<T> procChance(double chance) {
            this.procChance = Math.max(0.0, Math.min(1.0, chance));
            return this;
        }

        public PowerBuilder<T> cooldown(long cooldownMs) {
            this.cooldown = cooldownMs;
            return this;
        }

        public PowerBuilder<T> minimumRarity(Rarity rarity) {
            this.minimumRarity = rarity;
            return this;
        }

        public PowerBuilder<T> allowedRarities(Rarity... rarities) {
            this.allowedRarities = EnumSet.copyOf(Arrays.asList(rarities));
            return this;
        }

        public abstract T build();
    }
}
