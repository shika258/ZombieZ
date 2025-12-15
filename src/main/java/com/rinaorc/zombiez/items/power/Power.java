package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.items.scaling.ZoneScaling;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Représente un pouvoir qui peut être appliqué à un item
 *
 * Les pouvoirs sont des effets spéciaux qui se déclenchent dans certaines conditions
 * (on hit, on kill, on damaged, etc.).
 *
 * SYSTÈME DE SCALING:
 * - La ZONE est le facteur PRINCIPAL de puissance (dégâts, rayon, durée)
 * - La RARETÉ influence les chances de proc (bonus de proc)
 * - L'Item Level est utilisé comme référence pour la compatibilité
 *
 * Scaling par zone:
 * - Dégâts: Zone 1 = base, Zone 50 = base × 7
 * - Rayon/Durée: Zone 1 = base, Zone 50 = base × 3
 * - Proc Chance: Légère augmentation avec la zone
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
     *
     * @deprecated Utiliser canProc(Player, int, int, Rarity) pour le nouveau système
     */
    @Deprecated
    public boolean canProc(Player player, int itemLevel) {
        return canProc(player, itemLevel, 1, Rarity.RARE);
    }

    /**
     * Vérifie si le pouvoir peut se déclencher avec le nouveau système de scaling
     *
     * @param player Joueur qui déclenche
     * @param itemLevel Item Level (pour compatibilité)
     * @param zoneId Zone de l'item (facteur principal pour le scaling)
     * @param rarity Rareté de l'item (influence la chance de proc)
     */
    public boolean canProc(Player player, int itemLevel, int zoneId, Rarity rarity) {
        // Vérifier le cooldown
        if (isOnCooldown(player)) {
            return false;
        }

        // Roll de la chance de proc avec bonus de zone et rareté
        double procChance = calculateProcChance(zoneId, rarity);
        return Math.random() < procChance;
    }

    /**
     * @deprecated Utiliser calculateProcChance(int zoneId, Rarity rarity)
     */
    @Deprecated
    protected double calculateProcChance(int itemLevel) {
        // Légère augmentation avec l'ILVL (max +20% de la base)
        double ilvlBonus = (itemLevel / 100.0) * 0.2;
        return Math.min(1.0, baseProcChance * (1.0 + ilvlBonus));
    }

    /**
     * Calcule la chance de proc selon la zone et la rareté
     *
     * - La zone ajoute un léger bonus (max +50%)
     * - La rareté ajoute son bonus de proc (max +40%)
     */
    protected double calculateProcChance(int zoneId, Rarity rarity) {
        // Bonus de zone (léger, max +50%)
        double zoneBonus = ZoneScaling.getPowerProcMultiplier(zoneId) - 1.0;

        // Bonus de rareté (important pour les raretés élevées)
        double rarityBonus = rarity.getProcChanceBonus();

        return Math.min(1.0, baseProcChance * (1.0 + zoneBonus + rarityBonus));
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
     * @deprecated Utiliser trigger(Player, LivingEntity, int, int) avec le zoneId
     * @param player Le joueur qui déclenche le pouvoir
     * @param target La cible (peut être null selon le type de pouvoir)
     * @param itemLevel L'Item Level de l'item qui a le pouvoir
     */
    public abstract void trigger(Player player, LivingEntity target, int itemLevel);

    /**
     * Déclenche le pouvoir avec scaling par zone
     *
     * Les sous-classes doivent surcharger cette méthode pour utiliser le zoneId
     * dans leurs calculs de dégâts, rayon, durée, etc.
     *
     * @param player Le joueur qui déclenche le pouvoir
     * @param target La cible (peut être null selon le type de pouvoir)
     * @param itemLevel L'Item Level de l'item qui a le pouvoir
     * @param zoneId Zone de l'item (facteur principal de puissance)
     */
    public void trigger(Player player, LivingEntity target, int itemLevel, int zoneId) {
        // Par défaut, déléguer à l'ancienne méthode pour compatibilité
        // Les sous-classes doivent surcharger pour utiliser le zoneId
        trigger(player, target, itemLevel);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MÉTHODES DE SCALING UTILITAIRES POUR LES SOUS-CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Calcule les dégâts scalés par zone
     * Zone 1 → baseDamage, Zone 50 → baseDamage × 7
     */
    protected double getScaledDamage(double baseDamage, int zoneId) {
        return ZoneScaling.scaleDamage(baseDamage, zoneId);
    }

    /**
     * Calcule le rayon scalé par zone
     * Zone 1 → baseRadius, Zone 50 → baseRadius × 3
     */
    protected double getScaledRadius(double baseRadius, int zoneId) {
        return ZoneScaling.scaleRadius(baseRadius, zoneId);
    }

    /**
     * Calcule la durée scalée par zone (en ticks)
     * Zone 1 → baseDuration, Zone 50 → baseDuration × 3
     */
    protected int getScaledDuration(int baseDuration, int zoneId) {
        return ZoneScaling.scaleDuration(baseDuration, zoneId);
    }

    /**
     * Obtient le lore à afficher sur l'item
     *
     * @deprecated Utiliser getLore(int itemLevel, int zoneId, Rarity rarity)
     * @param itemLevel L'Item Level de l'item
     * @return Les lignes de lore décrivant le pouvoir
     */
    public List<String> getLore(int itemLevel) {
        return getLore(itemLevel, 1, Rarity.RARE);
    }

    /**
     * Obtient le lore à afficher sur l'item avec scaling par zone
     *
     * @param itemLevel L'Item Level de l'item
     * @param zoneId Zone de l'item
     * @param rarity Rareté de l'item
     * @return Les lignes de lore décrivant le pouvoir
     */
    public List<String> getLore(int itemLevel, int zoneId, Rarity rarity) {
        List<String> lore = new ArrayList<>();

        lore.add("§d§l✦ " + displayName);
        lore.add("§7" + description);
        lore.add("§8Chance: §e" + String.format("%.1f%%", calculateProcChance(zoneId, rarity) * 100));

        // Ajouter des infos spécifiques au pouvoir (dégâts, durée, etc.)
        List<String> powerStats = getPowerStats(itemLevel, zoneId);
        lore.addAll(powerStats);

        if (cooldownMs > 0) {
            lore.add("§8Cooldown: §e" + (cooldownMs / 1000) + "s");
        }

        return lore;
    }

    /**
     * @deprecated Utiliser getPowerStats(int itemLevel, int zoneId)
     */
    protected List<String> getPowerStats(int itemLevel) {
        return getPowerStats(itemLevel, 1);
    }

    /**
     * Obtient les statistiques spécifiques du pouvoir pour le lore
     * À surcharger dans les sous-classes pour afficher les valeurs scalées par zone
     *
     * @param itemLevel Item Level (compatibilité)
     * @param zoneId Zone de l'item (pour scaling)
     */
    protected List<String> getPowerStats(int itemLevel, int zoneId) {
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
