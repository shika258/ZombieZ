package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.weather.WeatherEffect;
import com.rinaorc.zombiez.weather.WeatherManager;
import com.rinaorc.zombiez.weather.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande admin pour gérer le système de météo dynamique
 *
 * Usage:
 * /zzweather                    - Affiche la météo actuelle
 * /zzweather set <type> [durée] - Force un type de météo
 * /zzweather clear              - Passe au temps clair
 * /zzweather list               - Liste tous les types de météo
 * /zzweather toggle <type>      - Active/désactive un type de météo
 * /zzweather stats              - Statistiques du système
 * /zzweather interval <min> <max> - Définit l'intervalle entre changements
 * /zzweather debug              - Informations de debug détaillées
 * /zzweather reload             - Recharge la configuration
 *
 * Alias joueur (/weather ou /meteo):
 * /weather                      - Affiche la météo actuelle
 */
public class WeatherAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public WeatherAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        WeatherManager weatherManager = plugin.getWeatherManager();
        if (weatherManager == null) {
            sender.sendMessage("§cLe système de météo dynamique n'est pas initialisé.");
            return true;
        }

        // Sans argument, afficher la météo actuelle
        if (args.length == 0) {
            showCurrentWeather(sender, weatherManager);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Commande joueur (sans permission)
        if (subCommand.equals("info") || subCommand.equals("current")) {
            showCurrentWeather(sender, weatherManager);
            return true;
        }

        // Commandes admin (nécessitent la permission)
        if (!sender.hasPermission("zombiez.admin.weather")) {
            showCurrentWeather(sender, weatherManager);
            return true;
        }

        switch (subCommand) {
            case "set", "force" -> handleSet(sender, args, weatherManager);
            case "clear", "stop" -> handleClear(sender, weatherManager);
            case "list", "types" -> handleList(sender, weatherManager);
            case "toggle" -> handleToggle(sender, args, weatherManager);
            case "stats", "statistics" -> handleStats(sender, weatherManager);
            case "interval" -> handleInterval(sender, args, weatherManager);
            case "debug" -> handleDebug(sender, weatherManager);
            case "reload" -> handleReload(sender);
            case "help" -> showHelp(sender);
            default -> {
                // Essayer d'interpréter comme un type de météo direct
                WeatherType directType = WeatherType.fromConfigKey(subCommand);
                if (directType != null) {
                    int duration = args.length > 1 ? parseIntOrDefault(args[1], -1) : -1;
                    if (duration > 0) {
                        weatherManager.forceWeather(directType, duration);
                    } else {
                        weatherManager.forceWeather(directType);
                    }
                    sender.sendMessage("§a✓ Météo forcée: " + directType.getColor() + directType.getDisplayName());
                } else {
                    showHelp(sender);
                }
            }
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== ZombieZ - Météo Dynamique ===");
        sender.sendMessage("§e/zzweather §7- Affiche la météo actuelle");
        sender.sendMessage("§e/zzweather set <type> [durée] §7- Force une météo");
        sender.sendMessage("§e/zzweather clear §7- Passe au temps clair");
        sender.sendMessage("§e/zzweather list §7- Liste tous les types");
        sender.sendMessage("§e/zzweather toggle <type> §7- Active/désactive un type");
        sender.sendMessage("§e/zzweather stats §7- Statistiques du système");
        sender.sendMessage("§e/zzweather interval <min> <max> §7- Intervalle (secondes)");
        sender.sendMessage("§e/zzweather debug §7- Informations de debug");
        sender.sendMessage("§e/zzweather reload §7- Recharge la configuration");
        sender.sendMessage("");
        sender.sendMessage("§7Raccourci: §e/zzweather <type> [durée]");
    }

    private void showCurrentWeather(CommandSender sender, WeatherManager weatherManager) {
        WeatherType currentType = weatherManager.getCurrentWeatherType();
        WeatherEffect effect = weatherManager.getCurrentWeather();

        sender.sendMessage("");
        sender.sendMessage("§6§l=== Météo Actuelle ===");
        sender.sendMessage("");

        // Type de météo
        sender.sendMessage(currentType.getColor() + "§l" + currentType.getIcon() + " " +
            currentType.getDisplayName());
        sender.sendMessage("§7" + currentType.getDescription());
        sender.sendMessage("");

        // Informations supplémentaires
        if (effect != null && effect.isValid()) {
            sender.sendMessage("§7Temps restant: §e" + effect.getRemainingTimeSeconds() + "s");
            sender.sendMessage("§7Progression: §e" + String.format("%.1f%%",
                (1.0 - effect.getRemainingTimePercent()) * 100));
        }

        // Multiplicateurs
        sender.sendMessage("");
        sender.sendMessage("§7Effets actifs:");
        sender.sendMessage("  §7Spawn zombies: §e" + String.format("%.0f%%",
            weatherManager.getCurrentSpawnMultiplier() * 100));
        sender.sendMessage("  §7Dégâts zombies: §e" + String.format("%.0f%%",
            weatherManager.getCurrentZombieDamageMultiplier() * 100));
        sender.sendMessage("  §7Vitesse zombies: §e" + String.format("%.0f%%",
            weatherManager.getCurrentZombieSpeedMultiplier() * 100));

        // Avertissements
        if (currentType.isDangerous()) {
            sender.sendMessage("");
            sender.sendMessage("§c⚠ Météo dangereuse! Dégâts environnementaux actifs.");
        }

        if (currentType.buffZombies()) {
            sender.sendMessage("§c⚠ Les zombies sont renforcés!");
        }

        if (currentType.isBeneficial()) {
            sender.sendMessage("§a✓ Conditions favorables aux survivants.");
        }

        // Info admin
        if (sender.hasPermission("zombiez.admin.weather")) {
            sender.sendMessage("");
            sender.sendMessage("§7[Admin] Utilisez §e/zzweather help §7pour les commandes.");
        }

        sender.sendMessage("");
    }

    private void handleSet(CommandSender sender, String[] args, WeatherManager weatherManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzweather set <type> [durée_secondes]");
            sender.sendMessage("§7Types disponibles:");
            for (WeatherType type : WeatherType.values()) {
                sender.sendMessage("  §7- §e" + type.getConfigKey() + " §7(" +
                    type.getColor() + type.getDisplayName() + "§7)");
            }
            return;
        }

        WeatherType type = WeatherType.fromConfigKey(args[1]);
        if (type == null) {
            sender.sendMessage("§cType de météo invalide: " + args[1]);
            sender.sendMessage("§7Types valides: " + Arrays.stream(WeatherType.values())
                .map(WeatherType::getConfigKey)
                .collect(Collectors.joining(", ")));
            return;
        }

        boolean success;
        if (args.length >= 3) {
            int duration = parseIntOrDefault(args[2], -1);
            if (duration <= 0) {
                sender.sendMessage("§cDurée invalide: " + args[2]);
                return;
            }
            success = weatherManager.forceWeather(type, duration);
            sender.sendMessage("§a✓ Météo §e" + type.getDisplayName() +
                " §aforcée pour §e" + duration + "s");
        } else {
            success = weatherManager.forceWeather(type);
            sender.sendMessage("§a✓ Météo §e" + type.getDisplayName() + " §aforcée");
        }

        if (!success) {
            sender.sendMessage("§cÉchec du changement de météo.");
        }
    }

    private void handleClear(CommandSender sender, WeatherManager weatherManager) {
        weatherManager.clearWeather();
        sender.sendMessage("§a✓ Météo changée: §e☀ Temps Clair");
    }

    private void handleList(CommandSender sender, WeatherManager weatherManager) {
        sender.sendMessage("§6§l=== Types de Météo Disponibles ===");
        sender.sendMessage("");

        for (WeatherType type : WeatherType.values()) {
            boolean enabled = weatherManager.isTypeEnabled(type);
            String status = enabled ? "§a✓" : "§c✗";

            sender.sendMessage(status + " " + type.getColor() + type.getIcon() + " " +
                type.getDisplayName() + " §7(" + type.getConfigKey() + ")");

            // Détails pour admins
            if (sender.hasPermission("zombiez.admin.weather")) {
                StringBuilder details = new StringBuilder();
                details.append("   §8Spawn: ").append(formatMultiplier(type.getSpawnMultiplier()));
                details.append(" §8| Dégâts: ").append(formatMultiplier(type.getZombieDamageMultiplier()));
                details.append(" §8| Vitesse: ").append(formatMultiplier(type.getZombieSpeedMultiplier()));

                if (type.isDangerous()) {
                    details.append(" §c| ⚠ Dangereux");
                }
                if (type.isBeneficial()) {
                    details.append(" §a| ✓ Bénéfique");
                }

                sender.sendMessage(details.toString());
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§7Utilisez §e/zzweather toggle <type> §7pour activer/désactiver.");
    }

    private void handleToggle(CommandSender sender, String[] args, WeatherManager weatherManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzweather toggle <type>");
            return;
        }

        WeatherType type = WeatherType.fromConfigKey(args[1]);
        if (type == null) {
            sender.sendMessage("§cType de météo invalide: " + args[1]);
            return;
        }

        if (type == WeatherType.CLEAR) {
            sender.sendMessage("§cLe temps clair ne peut pas être désactivé.");
            return;
        }

        boolean currentState = weatherManager.isTypeEnabled(type);
        weatherManager.setTypeEnabled(type, !currentState);

        sender.sendMessage("§7Type §e" + type.getDisplayName() + " §7: " +
            (!currentState ? "§aActivé" : "§cDésactivé"));
    }

    private void handleStats(CommandSender sender, WeatherManager weatherManager) {
        sender.sendMessage("§6§l=== Statistiques Météo ===");
        sender.sendMessage("");
        sender.sendMessage(weatherManager.getStats());
        sender.sendMessage("");

        sender.sendMessage("§7Changements par type:");
        for (String line : weatherManager.getWeatherTypesList()) {
            sender.sendMessage("  " + line);
        }
    }

    private void handleInterval(CommandSender sender, String[] args, WeatherManager weatherManager) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzweather interval <min_secondes> <max_secondes>");
            return;
        }

        int min = parseIntOrDefault(args[1], -1);
        int max = parseIntOrDefault(args[2], -1);

        if (min < 60) {
            sender.sendMessage("§cL'intervalle minimum doit être d'au moins 60 secondes.");
            return;
        }

        if (max <= min) {
            sender.sendMessage("§cL'intervalle maximum doit être supérieur au minimum.");
            return;
        }

        weatherManager.setWeatherInterval(min, max);
        sender.sendMessage("§a✓ Intervalle de météo défini: §e" + min + "s §7- §e" + max + "s");
    }

    private void handleDebug(CommandSender sender, WeatherManager weatherManager) {
        for (String line : weatherManager.getDebugInfo()) {
            sender.sendMessage(line);
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            // Recharger la configuration météo
            var weatherConfig = plugin.getConfigManager().reloadConfig("weather.yml");
            if (plugin.getWeatherManager() != null) {
                plugin.getWeatherManager().loadConfig(weatherConfig);
            }
            sender.sendMessage("§a✓ Configuration météo rechargée.");
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors du rechargement: " + e.getMessage());
        }
    }

    private String formatMultiplier(double value) {
        if (value == 1.0) return "§7100%";
        if (value > 1.0) return "§c" + String.format("%.0f%%", value * 100);
        return "§a" + String.format("%.0f%%", value * 100);
    }

    private int parseIntOrDefault(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Commandes de base
            completions.addAll(Arrays.asList("info", "current"));

            // Commandes admin
            if (sender.hasPermission("zombiez.admin.weather")) {
                completions.addAll(Arrays.asList(
                    "set", "force", "clear", "stop", "list", "types",
                    "toggle", "stats", "interval", "debug", "reload", "help"
                ));

                // Ajouter les types de météo comme raccourcis
                for (WeatherType type : WeatherType.values()) {
                    completions.add(type.getConfigKey());
                }
            }
        } else if (args.length == 2 && sender.hasPermission("zombiez.admin.weather")) {
            switch (args[0].toLowerCase()) {
                case "set", "force", "toggle" -> {
                    for (WeatherType type : WeatherType.values()) {
                        completions.add(type.getConfigKey());
                    }
                }
                case "interval" -> {
                    completions.addAll(Arrays.asList("60", "120", "180", "300", "480", "600"));
                }
            }
        } else if (args.length == 3 && sender.hasPermission("zombiez.admin.weather")) {
            switch (args[0].toLowerCase()) {
                case "set", "force" -> {
                    // Suggestions de durée
                    completions.addAll(Arrays.asList("60", "120", "180", "300", "600"));
                }
                case "interval" -> {
                    completions.addAll(Arrays.asList("300", "480", "600", "900", "1200", "1800"));
                }
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix))
            .sorted()
            .collect(Collectors.toList());
    }
}
