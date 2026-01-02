package com.rinaorc.zombiez.weather;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Gestionnaire du système de météo dynamique pour ZombieZ
 *
 * Fonctionnalités:
 * - Changements de météo automatiques et aléatoires
 * - Transitions douces entre les conditions météo
 * - Effets gameplay (spawn rate, dégâts zombies, débuffs joueurs)
 * - Configuration complète via weather.yml
 * - Support multi-mondes (optionnel)
 * - Intégration avec le système de zones
 *
 * @author ZombieZ Team
 * @version 1.0
 */
public class WeatherManager {

    private final ZombieZPlugin plugin;
    private final Random random;

    // État actuel de la météo
    @Getter
    private WeatherEffect currentWeather;

    // Historique des météos (pour éviter les répétitions)
    private final Deque<WeatherType> weatherHistory = new LinkedList<>();
    private static final int HISTORY_SIZE = 5;

    // Configuration
    @Getter
    private boolean enabled = true;
    private int minWeatherInterval = 20 * 60 * 16;    // 16 minutes min entre changements
    private int maxWeatherInterval = 20 * 60 * 40;    // 40 minutes max
    private int minPlayersForWeather = 1;              // Min joueurs pour déclencher la météo
    private double clearWeatherChance = 0.35;          // 35% chance de temps clair
    private boolean nightOnlyBloodMoon = true;         // Lune de sang uniquement la nuit
    private boolean respectBiomes = true;              // Adapter météo aux biomes
    private double transitionDuration = 20 * 5;        // 5 secondes de transition

    // Types activés et poids personnalisés
    private final Map<WeatherType, Boolean> enabledTypes = new EnumMap<>(WeatherType.class);
    private final Map<WeatherType, Double> typeWeightOverrides = new EnumMap<>(WeatherType.class);
    private final Map<WeatherType, Double> typeDurationMultipliers = new EnumMap<>(WeatherType.class);

    // Cooldowns par type
    private final Map<WeatherType, Long> typeCooldowns = new ConcurrentHashMap<>();
    private long typeCooldownDuration = 1000 * 60 * 10; // 10 minutes entre même type

    // État du scheduler
    private long lastWeatherChange = 0;
    private long nextWeatherChange = 0;
    private BukkitTask schedulerTask;
    private BukkitTask tickTask;

    // Statistiques
    @Getter private int totalWeatherChanges = 0;
    @Getter private int totalDangerousWeathers = 0;
    @Getter private Map<WeatherType, Integer> weatherCounts = new EnumMap<>(WeatherType.class);

    // Monde cible (null = monde principal)
    private World targetWorld;

    public WeatherManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();

        // Activer tous les types par défaut
        for (WeatherType type : WeatherType.values()) {
            enabledTypes.put(type, true);
            weatherCounts.put(type, 0);
            typeDurationMultipliers.put(type, 1.0);
        }
    }

    /**
     * Charge la configuration depuis weather.yml
     */
    public void loadConfig(FileConfiguration config) {
        if (config == null) {
            plugin.log(Level.WARNING, "§eConfiguration weather.yml non trouvée, utilisation des valeurs par défaut");
            return;
        }

        // Paramètres globaux
        ConfigurationSection globalSection = config.getConfigurationSection("weather");
        if (globalSection != null) {
            enabled = globalSection.getBoolean("enabled", true);
            minWeatherInterval = globalSection.getInt("min-interval-seconds", 960) * 20;  // 16 min
            maxWeatherInterval = globalSection.getInt("max-interval-seconds", 2400) * 20; // 40 min
            minPlayersForWeather = globalSection.getInt("min-players", 1);
            clearWeatherChance = globalSection.getDouble("clear-weather-chance", 0.35);
            nightOnlyBloodMoon = globalSection.getBoolean("night-only-blood-moon", true);
            respectBiomes = globalSection.getBoolean("respect-biomes", true);
            transitionDuration = globalSection.getDouble("transition-duration-seconds", 5) * 20;
            typeCooldownDuration = globalSection.getLong("type-cooldown-seconds", 600) * 1000;
        }

        // Configuration par type de météo
        ConfigurationSection typesSection = config.getConfigurationSection("weather.types");
        if (typesSection != null) {
            for (WeatherType type : WeatherType.values()) {
                ConfigurationSection typeConfig = typesSection.getConfigurationSection(type.getConfigKey());
                if (typeConfig != null) {
                    enabledTypes.put(type, typeConfig.getBoolean("enabled", true));

                    if (typeConfig.contains("weight")) {
                        typeWeightOverrides.put(type, typeConfig.getDouble("weight"));
                    }

                    if (typeConfig.contains("duration-multiplier")) {
                        typeDurationMultipliers.put(type, typeConfig.getDouble("duration-multiplier", 1.0));
                    }
                }
            }
        }

        plugin.log(Level.INFO, "§a✓ Configuration météo dynamique chargée");
    }

    /**
     * Démarre le système de météo dynamique
     */
    public void start() {
        if (!enabled) {
            plugin.log(Level.INFO, "§7Système météo désactivé");
            return;
        }

        // Définir le monde cible
        targetWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        // Démarrer avec un temps clair
        startWeather(WeatherType.CLEAR);

        // Planifier le prochain changement
        scheduleNextWeatherChange();

        // Démarrer le scheduler
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkAndChangeWeather();
        }, 20L * 30, 20L * 30); // Check toutes les 30 secondes

        // Démarrer le tick
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickWeather();
        }, 20L, 20L); // Tick chaque seconde

        plugin.log(Level.INFO, "§a✓ Système météo dynamique démarré");
    }

    /**
     * Arrête le système de météo
     */
    public void shutdown() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
        if (tickTask != null) {
            tickTask.cancel();
        }

        // Arrêter la météo actuelle
        if (currentWeather != null) {
            currentWeather.forceStop();
            currentWeather = null;
        }

        // Restaurer la météo Minecraft
        if (targetWorld != null) {
            targetWorld.setStorm(false);
            targetWorld.setThundering(false);
        }

        plugin.log(Level.INFO, "§7Système météo arrêté");
    }

    /**
     * Planifie le prochain changement de météo
     */
    private void scheduleNextWeatherChange() {
        int interval = minWeatherInterval + random.nextInt(Math.max(1, maxWeatherInterval - minWeatherInterval));
        nextWeatherChange = System.currentTimeMillis() + (interval / 20 * 1000L);
    }

    /**
     * Vérifie et change la météo si nécessaire
     */
    private void checkAndChangeWeather() {
        if (!enabled) return;
        if (Bukkit.getOnlinePlayers().size() < minPlayersForWeather) return;
        if (System.currentTimeMillis() < nextWeatherChange) return;

        // Sélectionner une nouvelle météo
        WeatherType newType = selectNextWeatherType();
        if (newType != null && newType != getCurrentWeatherType()) {
            transitionToWeather(newType);
        }

        // Planifier le prochain changement
        scheduleNextWeatherChange();
    }

    /**
     * Tick la météo actuelle
     */
    private void tickWeather() {
        if (currentWeather == null || !currentWeather.isValid()) {
            // Si la météo s'est terminée naturellement, passer au temps clair
            if (currentWeather != null && (currentWeather.isCompleted() || currentWeather.isCancelled())) {
                if (getCurrentWeatherType() != WeatherType.CLEAR) {
                    transitionToWeather(WeatherType.CLEAR);
                }
            }
        }
    }

    /**
     * Sélectionne le prochain type de météo de manière intelligente
     */
    private WeatherType selectNextWeatherType() {
        long now = System.currentTimeMillis();

        // Chance de temps clair
        if (random.nextDouble() < clearWeatherChance && getCurrentWeatherType() != WeatherType.CLEAR) {
            return WeatherType.CLEAR;
        }

        // Filtrer les types disponibles
        List<WeatherType> validTypes = Arrays.stream(WeatherType.values())
            .filter(t -> t != WeatherType.CLEAR)
            .filter(t -> enabledTypes.getOrDefault(t, true))
            .filter(t -> !isTypeOnCooldown(t, now))
            .filter(t -> !isRecentlyUsed(t))
            .filter(t -> isValidForCurrentConditions(t))
            .collect(Collectors.toList());

        if (validTypes.isEmpty()) {
            // Fallback: ignorer les cooldowns
            validTypes = Arrays.stream(WeatherType.values())
                .filter(t -> t != WeatherType.CLEAR)
                .filter(t -> enabledTypes.getOrDefault(t, true))
                .filter(t -> isValidForCurrentConditions(t))
                .collect(Collectors.toList());
        }

        if (validTypes.isEmpty()) {
            return WeatherType.CLEAR;
        }

        // Sélection pondérée
        return selectWeightedType(validTypes);
    }

    /**
     * Sélectionne un type basé sur les poids
     */
    private WeatherType selectWeightedType(List<WeatherType> types) {
        Map<WeatherType, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (WeatherType type : types) {
            double weight = typeWeightOverrides.getOrDefault(type, (double) type.getSpawnWeight());

            // Réduire le poids si récemment utilisé
            if (weatherHistory.contains(type)) {
                weight *= 0.5;
            }

            weights.put(type, weight);
            totalWeight += weight;
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;

        for (Map.Entry<WeatherType, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }

        return types.get(0);
    }

    /**
     * Vérifie si un type est en cooldown
     */
    private boolean isTypeOnCooldown(WeatherType type, long now) {
        Long lastUsed = typeCooldowns.get(type);
        return lastUsed != null && (now - lastUsed) < typeCooldownDuration;
    }

    /**
     * Vérifie si un type a été utilisé récemment
     */
    private boolean isRecentlyUsed(WeatherType type) {
        return weatherHistory.contains(type);
    }

    /**
     * Vérifie si le type est valide pour les conditions actuelles
     */
    private boolean isValidForCurrentConditions(WeatherType type) {
        if (targetWorld == null) return true;

        // Lune de sang uniquement la nuit
        if (type == WeatherType.BLOOD_MOON && nightOnlyBloodMoon) {
            long time = targetWorld.getTime();
            if (time < 13000 || time > 23000) {
                return false;
            }
        }

        return true;
    }

    /**
     * Effectue une transition vers un nouveau type de météo
     */
    public void transitionToWeather(WeatherType newType) {
        // Arrêter l'ancienne météo
        if (currentWeather != null && currentWeather.isValid()) {
            currentWeather.cancel();
        }

        // Enregistrer dans l'historique
        if (getCurrentWeatherType() != null && getCurrentWeatherType() != WeatherType.CLEAR) {
            addToHistory(getCurrentWeatherType());
        }

        // Démarrer la nouvelle météo
        startWeather(newType);

        // Enregistrer le cooldown
        if (newType != WeatherType.CLEAR) {
            typeCooldowns.put(newType, System.currentTimeMillis());
        }

        // Statistiques
        totalWeatherChanges++;
        weatherCounts.merge(newType, 1, Integer::sum);
        if (newType.isDangerous()) {
            totalDangerousWeathers++;
        }

        lastWeatherChange = System.currentTimeMillis();
    }

    /**
     * Démarre un effet météo
     */
    private void startWeather(WeatherType type) {
        // Calculer la durée avec le multiplicateur
        double durationMultiplier = typeDurationMultipliers.getOrDefault(type, 1.0);
        int baseDuration = type.getMinDuration() +
            random.nextInt(Math.max(1, type.getMaxDuration() - type.getMinDuration()));
        int finalDuration = (int) (baseDuration * durationMultiplier);

        // Créer et démarrer l'effet approprié selon le type
        // Certains types ont des effets spéciaux nécessitant des classes dédiées
        currentWeather = switch (type) {
            case GIANT_INVASION -> new GiantInvasionEffect(plugin, finalDuration);
            default -> new WeatherEffect(plugin, type, finalDuration);
        };
        currentWeather.start();

        plugin.log(Level.INFO, "§7Météo changée: §e" + type.getDisplayName() +
            " §7(durée: " + (finalDuration / 20) + "s)");
    }

    /**
     * Ajoute un type à l'historique
     */
    private void addToHistory(WeatherType type) {
        weatherHistory.addFirst(type);
        while (weatherHistory.size() > HISTORY_SIZE) {
            weatherHistory.removeLast();
        }
    }

    // ==================== API PUBLIQUE ====================

    /**
     * Force un changement de météo immédiat
     */
    public boolean forceWeather(WeatherType type) {
        if (type == null) return false;

        transitionToWeather(type);
        scheduleNextWeatherChange(); // Reset le timer

        return true;
    }

    /**
     * Force un changement de météo avec durée personnalisée
     */
    public boolean forceWeather(WeatherType type, int durationSeconds) {
        if (type == null || durationSeconds <= 0) return false;

        // Arrêter l'ancienne météo
        if (currentWeather != null && currentWeather.isValid()) {
            currentWeather.cancel();
        }

        // Créer avec durée personnalisée (utiliser la classe appropriée)
        int durationTicks = durationSeconds * 20;
        currentWeather = switch (type) {
            case GIANT_INVASION -> new GiantInvasionEffect(plugin, durationTicks);
            default -> new WeatherEffect(plugin, type, durationTicks);
        };
        currentWeather.start();

        typeCooldowns.put(type, System.currentTimeMillis());
        totalWeatherChanges++;
        weatherCounts.merge(type, 1, Integer::sum);

        scheduleNextWeatherChange();

        return true;
    }

    /**
     * Arrête la météo actuelle et passe au temps clair
     */
    public void clearWeather() {
        transitionToWeather(WeatherType.CLEAR);
    }

    /**
     * Obtient le type de météo actuel
     */
    public WeatherType getCurrentWeatherType() {
        return currentWeather != null ? currentWeather.getType() : WeatherType.CLEAR;
    }

    /**
     * Obtient le multiplicateur de spawn actuel basé sur la météo
     */
    public double getCurrentSpawnMultiplier() {
        if (currentWeather == null || !currentWeather.isValid()) {
            return 1.0;
        }
        return currentWeather.getEffectiveSpawnMultiplier();
    }

    /**
     * Obtient le multiplicateur de dégâts zombies actuel
     */
    public double getCurrentZombieDamageMultiplier() {
        if (currentWeather == null || !currentWeather.isValid()) {
            return 1.0;
        }
        return currentWeather.getEffectiveZombieDamageMultiplier();
    }

    /**
     * Obtient le multiplicateur de vitesse zombies actuel
     */
    public double getCurrentZombieSpeedMultiplier() {
        if (currentWeather == null || !currentWeather.isValid()) {
            return 1.0;
        }
        return currentWeather.getEffectiveZombieSpeedMultiplier();
    }

    /**
     * Obtient le multiplicateur d'XP actuel basé sur la météo
     */
    public double getCurrentXpMultiplier() {
        if (currentWeather == null || !currentWeather.isValid()) {
            return 1.0;
        }
        return currentWeather.getEffectiveXpMultiplier();
    }

    /**
     * Obtient le multiplicateur de loot actuel basé sur la météo
     */
    public double getCurrentLootMultiplier() {
        if (currentWeather == null || !currentWeather.isValid()) {
            return 1.0;
        }
        return currentWeather.getEffectiveLootMultiplier();
    }

    /**
     * Vérifie si la météo actuelle est dangereuse
     */
    public boolean isCurrentWeatherDangerous() {
        return currentWeather != null && currentWeather.getType().isDangerous();
    }

    /**
     * Vérifie si un joueur est à l'abri
     */
    public boolean isPlayerSheltered(Player player) {
        if (currentWeather == null) return true;
        return currentWeather.getShelteredPlayers().contains(player.getUniqueId());
    }

    /**
     * Active/désactive un type de météo
     */
    public void setTypeEnabled(WeatherType type, boolean enabled) {
        enabledTypes.put(type, enabled);
    }

    /**
     * Vérifie si un type est activé
     */
    public boolean isTypeEnabled(WeatherType type) {
        return enabledTypes.getOrDefault(type, true);
    }

    /**
     * Définit le poids d'un type
     */
    public void setTypeWeight(WeatherType type, double weight) {
        typeWeightOverrides.put(type, weight);
    }

    /**
     * Définit les intervalles de changement
     */
    public void setWeatherInterval(int minSeconds, int maxSeconds) {
        this.minWeatherInterval = minSeconds * 20;
        this.maxWeatherInterval = maxSeconds * 20;
        scheduleNextWeatherChange();
    }

    /**
     * Obtient les statistiques du système
     */
    public String getStats() {
        WeatherType type = getCurrentWeatherType();
        int remaining = currentWeather != null ? currentWeather.getRemainingTimeSeconds() : 0;
        long nextIn = Math.max(0, (nextWeatherChange - System.currentTimeMillis()) / 1000);

        return String.format(
            "%s%s %s §7| Durée: §e%ds §7| Prochain: §e%ds §7| Total: §e%d",
            type.getColor(), type.getIcon(), type.getDisplayName(),
            remaining, nextIn, totalWeatherChanges
        );
    }

    /**
     * Obtient les informations de debug détaillées
     */
    public List<String> getDebugInfo() {
        List<String> lines = new ArrayList<>();
        lines.add("§6=== Weather Manager ===");
        lines.add("§7Enabled: " + (enabled ? "§aOui" : "§cNon"));

        WeatherType type = getCurrentWeatherType();
        lines.add("§7Current: " + type.getColor() + type.getIcon() + " " + type.getDisplayName());

        if (currentWeather != null) {
            lines.add("§7Remaining: §e" + currentWeather.getRemainingTimeSeconds() + "s");
            lines.add("§7Spawn Multiplier: §e" + String.format("%.2f", getCurrentSpawnMultiplier()) + "x");
            lines.add("§7Zombie Damage: §e" + String.format("%.2f", getCurrentZombieDamageMultiplier()) + "x");
            lines.add("§7Zombie Speed: §e" + String.format("%.2f", getCurrentZombieSpeedMultiplier()) + "x");
        }

        long nextIn = Math.max(0, (nextWeatherChange - System.currentTimeMillis()) / 1000);
        lines.add("§7Next Change In: §e" + nextIn + "s");

        lines.add("");
        lines.add("§6Statistics:");
        lines.add("§7Total Changes: §e" + totalWeatherChanges);
        lines.add("§7Dangerous Weathers: §c" + totalDangerousWeathers);

        lines.add("");
        lines.add("§6Weather History:");
        for (WeatherType hist : weatherHistory) {
            lines.add("  §7- " + hist.getColor() + hist.getDisplayName());
        }

        return lines;
    }

    /**
     * Obtient un résumé court pour les joueurs
     */
    public String getPlayerSummary() {
        if (currentWeather == null) {
            return "§e☀ Temps Clair";
        }
        return currentWeather.getSummary();
    }

    /**
     * Obtient la liste de tous les types de météo avec leur statut
     */
    public List<String> getWeatherTypesList() {
        List<String> lines = new ArrayList<>();
        for (WeatherType type : WeatherType.values()) {
            boolean isEnabled = enabledTypes.getOrDefault(type, true);
            int count = weatherCounts.getOrDefault(type, 0);

            String status = isEnabled ? "§a✓" : "§c✗";
            lines.add(status + " " + type.getColor() + type.getIcon() + " " + type.getDisplayName() +
                " §7(×" + count + ")");
        }
        return lines;
    }
}
