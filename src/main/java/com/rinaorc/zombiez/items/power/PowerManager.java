package com.rinaorc.zombiez.items.power;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.power.impl.BeeSwarmPower;
import com.rinaorc.zombiez.items.power.impl.PandaRollPower;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Gestionnaire du système de Pouvoirs
 *
 * Gère l'enregistrement, la configuration et l'attribution
 * des pouvoirs sur les items.
 */
public class PowerManager {

    private final ZombieZPlugin plugin;

    @Getter
    private final ItemLevelManager itemLevelManager;

    // Registry de tous les pouvoirs disponibles
    private final Map<String, Power> registeredPowers = new HashMap<>();

    // Configuration globale
    private boolean systemEnabled = true;
    private final Map<Rarity, Double> powerChanceByRarity = new EnumMap<>(Rarity.class);

    public PowerManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.itemLevelManager = new ItemLevelManager();

        initializeDefaultChances();
        registerDefaultPowers();
    }

    /**
     * Initialise les chances par défaut d'obtenir un pouvoir par rareté
     */
    private void initializeDefaultChances() {
        powerChanceByRarity.put(Rarity.COMMON, 0.0);      // 0% - Pas de pouvoir sur commun
        powerChanceByRarity.put(Rarity.UNCOMMON, 0.05);   // 5%
        powerChanceByRarity.put(Rarity.RARE, 0.15);       // 15%
        powerChanceByRarity.put(Rarity.EPIC, 0.35);       // 35%
        powerChanceByRarity.put(Rarity.LEGENDARY, 0.60);  // 60%
        powerChanceByRarity.put(Rarity.MYTHIC, 0.85);     // 85%
        powerChanceByRarity.put(Rarity.EXALTED, 1.0);     // 100% - Toujours un pouvoir
    }

    /**
     * Enregistre les pouvoirs par défaut
     */
    private void registerDefaultPowers() {
        // Pouvoirs de base
        registerPower(new PandaRollPower());
        registerPower(new BeeSwarmPower());

        // Nouveaux pouvoirs
        registerPower(new com.rinaorc.zombiez.items.power.impl.LightningStrikePower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.IceNovaPower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.BloodSiphonPower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.ChainLightningPower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.MeteorShowerPower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.ShadowClonePower());
        registerPower(new com.rinaorc.zombiez.items.power.impl.PhoenixRebirthPower());
    }

    /**
     * Enregistre un nouveau pouvoir
     */
    public void registerPower(Power power) {
        registeredPowers.put(power.getId(), power);
        plugin.getLogger().info("Pouvoir enregistré: " + power.getDisplayName() + " [" + power.getId() + "]");
    }

    /**
     * Obtient un pouvoir par son ID
     */
    public Optional<Power> getPower(String id) {
        return Optional.ofNullable(registeredPowers.get(id));
    }

    /**
     * Obtient tous les pouvoirs enregistrés
     */
    public Collection<Power> getAllPowers() {
        return Collections.unmodifiableCollection(registeredPowers.values());
    }

    /**
     * Détermine si un item devrait avoir un pouvoir
     *
     * @param rarity La rareté de l'item
     * @param luckBonus Bonus de chance (0.0 = aucun, 1.0 = double chance)
     * @return true si l'item devrait avoir un pouvoir
     */
    public boolean shouldHavePower(Rarity rarity, double luckBonus) {
        if (!systemEnabled) return false;

        double baseChance = powerChanceByRarity.getOrDefault(rarity, 0.0);
        double finalChance = baseChance * (1.0 + luckBonus);

        return Math.random() < finalChance;
    }

    /**
     * Sélectionne un pouvoir aléatoire compatible avec la rareté
     *
     * @param rarity La rareté de l'item
     * @return Un pouvoir aléatoire, ou null si aucun compatible
     */
    public Power selectRandomPower(Rarity rarity) {
        List<Power> compatiblePowers = registeredPowers.values().stream()
            .filter(p -> p.canApplyToRarity(rarity))
            .toList();

        if (compatiblePowers.isEmpty()) {
            return null;
        }

        return compatiblePowers.get(new Random().nextInt(compatiblePowers.size()));
    }

    /**
     * Charge la configuration depuis un ConfigurationSection
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        // Configuration globale
        systemEnabled = config.getBoolean("enabled", true);

        // Charger les chances par rareté
        ConfigurationSection chancesSection = config.getConfigurationSection("power-chances");
        if (chancesSection != null) {
            for (Rarity rarity : Rarity.values()) {
                String key = rarity.name().toLowerCase();
                if (chancesSection.contains(key)) {
                    double chance = chancesSection.getDouble(key);
                    powerChanceByRarity.put(rarity, Math.max(0.0, Math.min(1.0, chance)));
                }
            }
        }

        // Charger la configuration de l'Item Level Manager
        ConfigurationSection ilvlSection = config.getConfigurationSection("item-level");
        if (ilvlSection != null) {
            itemLevelManager.loadFromConfig(ilvlSection);
        }

        // Charger la configuration de chaque pouvoir
        ConfigurationSection powersSection = config.getConfigurationSection("powers");
        if (powersSection != null) {
            for (String powerId : powersSection.getKeys(false)) {
                ConfigurationSection powerConfig = powersSection.getConfigurationSection(powerId);
                if (powerConfig != null) {
                    getPower(powerId).ifPresent(power -> loadPowerConfig(power, powerConfig));
                }
            }
        }

        plugin.getLogger().info("Système de pouvoirs chargé: " + registeredPowers.size() + " pouvoirs enregistrés");
    }

    /**
     * Charge la configuration d'un pouvoir spécifique
     */
    private void loadPowerConfig(Power power, ConfigurationSection config) {
        if (config.contains("proc-chance")) {
            power.baseProcChance = config.getDouble("proc-chance");
        }
        if (config.contains("cooldown")) {
            power.cooldownMs = config.getLong("cooldown") * 1000; // Convertir secondes en ms
        }
        if (config.contains("minimum-rarity")) {
            String rarityName = config.getString("minimum-rarity");
            try {
                power.minimumRarity = Rarity.valueOf(rarityName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Rareté invalide pour le pouvoir " + power.getId() + ": " + rarityName);
            }
        }
    }

    /**
     * Obtient les statistiques du système
     */
    public String getStats() {
        return String.format("Pouvoirs: %d | Système: %s | Joueurs avec cooldowns actifs: %d",
            registeredPowers.size(),
            systemEnabled ? "§aActivé" : "§cDésactivé",
            registeredPowers.values().stream()
                .mapToInt(p -> p.getPlayerCooldowns().size())
                .sum()
        );
    }

    /**
     * Nettoie les cooldowns expirés
     */
    public void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        for (Power power : registeredPowers.values()) {
            power.getPlayerCooldowns().entrySet()
                .removeIf(entry -> now - entry.getValue() > power.getCooldownMs() * 2);
        }
    }

    /**
     * Vérifie si le système est activé
     */
    public boolean isEnabled() {
        return systemEnabled;
    }

    /**
     * Active ou désactive le système
     */
    public void setEnabled(boolean enabled) {
        this.systemEnabled = enabled;
    }
}
