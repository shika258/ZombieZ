package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventManager;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande admin pour gérer les événements dynamiques
 *
 * Usage:
 * /zzevent spawn <type> [player] - Force le spawn d'un événement
 * /zzevent list - Liste les événements actifs
 * /zzevent stop <id|all> - Arrête un ou tous les événements
 * /zzevent info <id> - Informations sur un événement
 * /zzevent stats - Statistiques du système
 * /zzevent toggle <type> - Active/désactive un type d'événement
 * /zzevent interval <min> <max> - Définit l'intervalle entre événements
 * /zzevent debug - Affiche les informations de debug
 */
public class EventAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public EventAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombiez.admin.events")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        DynamicEventManager eventManager = plugin.getDynamicEventManager();
        if (eventManager == null) {
            sender.sendMessage("§cLe système d'événements dynamiques n'est pas initialisé.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn" -> handleSpawn(sender, args, eventManager);
            case "list" -> handleList(sender, eventManager);
            case "stop" -> handleStop(sender, args, eventManager);
            case "info" -> handleInfo(sender, args, eventManager);
            case "stats" -> handleStats(sender, eventManager);
            case "toggle" -> handleToggle(sender, args, eventManager);
            case "interval" -> handleInterval(sender, args, eventManager);
            case "debug" -> handleDebug(sender, eventManager);
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== ZombieZ - Événements Dynamiques ===");
        sender.sendMessage("§e/zzevent spawn <type> [player] §7- Force le spawn d'un événement");
        sender.sendMessage("§e/zzevent list §7- Liste les événements actifs");
        sender.sendMessage("§e/zzevent stop <id|all> §7- Arrête un ou tous les événements");
        sender.sendMessage("§e/zzevent info <id> §7- Informations sur un événement");
        sender.sendMessage("§e/zzevent stats §7- Statistiques du système");
        sender.sendMessage("§e/zzevent toggle <type> §7- Active/désactive un type");
        sender.sendMessage("§e/zzevent interval <min> <max> §7- Définit l'intervalle (secondes)");
        sender.sendMessage("§e/zzevent debug §7- Informations de debug");
        sender.sendMessage("");
        sender.sendMessage("§7Types disponibles:");
        for (DynamicEventType type : DynamicEventType.values()) {
            sender.sendMessage("  §7- §e" + type.getConfigKey() + " §7(" + type.getDisplayName() + ")");
        }
    }

    private void handleSpawn(CommandSender sender, String[] args, DynamicEventManager eventManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzevent spawn <type> [player]");
            return;
        }

        DynamicEventType type = DynamicEventType.fromConfigKey(args[1]);
        if (type == null) {
            sender.sendMessage("§cType d'événement invalide: " + args[1]);
            sender.sendMessage("§7Types valides: " + Arrays.stream(DynamicEventType.values())
                .map(DynamicEventType::getConfigKey)
                .collect(Collectors.joining(", ")));
            return;
        }

        Player target = null;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJoueur non trouvé: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cSpécifiez un joueur: /zzevent spawn " + args[1] + " <player>");
            return;
        }

        DynamicEvent event = eventManager.forceSpawnEventNear(type, target);
        if (event != null) {
            sender.sendMessage("§a✓ Événement " + type.getDisplayName() + " spawné près de " + target.getName());
            sender.sendMessage("§7ID: §e" + event.getId());
            sender.sendMessage("§7Zone: §e" + event.getZone().getDisplayName());
        } else {
            sender.sendMessage("§cÉchec du spawn de l'événement. Le joueur est peut-être dans une zone safe.");
        }
    }

    private void handleList(CommandSender sender, DynamicEventManager eventManager) {
        Map<String, DynamicEvent> events = eventManager.getActiveEvents();

        if (events.isEmpty()) {
            sender.sendMessage("§7Aucun événement actif.");
            return;
        }

        sender.sendMessage("§6§l=== Événements Actifs (" + events.size() + ") ===");
        for (DynamicEvent event : events.values()) {
            String status = event.isCompleted() ? "§a[TERMINÉ]" :
                (event.isFailed() ? "§c[ÉCHOUÉ]" : "§e[EN COURS]");

            sender.sendMessage("");
            sender.sendMessage(event.getType().getColor() + "§l" + event.getType().getIcon() +
                " " + event.getType().getDisplayName() + " " + status);
            sender.sendMessage("§7  ID: §e" + event.getId());
            sender.sendMessage("§7  Zone: §e" + event.getZone().getDisplayName() + " §7(ID: " + event.getZone().getId() + ")");
            sender.sendMessage("§7  Temps restant: §e" + event.getRemainingTimeSeconds() + "s");
            sender.sendMessage("§7  Participants: §e" + event.getParticipants().size());
        }
    }

    private void handleStop(CommandSender sender, String[] args, DynamicEventManager eventManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzevent stop <id|all>");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            int count = eventManager.getActiveEvents().size();
            eventManager.stopAllEvents();
            sender.sendMessage("§a✓ " + count + " événement(s) arrêté(s).");
        } else {
            if (eventManager.stopEvent(args[1])) {
                sender.sendMessage("§a✓ Événement " + args[1] + " arrêté.");
            } else {
                sender.sendMessage("§cÉvénement non trouvé: " + args[1]);
            }
        }
    }

    private void handleInfo(CommandSender sender, String[] args, DynamicEventManager eventManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzevent info <id>");
            return;
        }

        DynamicEvent event = eventManager.getEvent(args[1]);
        if (event == null) {
            sender.sendMessage("§cÉvénement non trouvé: " + args[1]);
            return;
        }

        sender.sendMessage("§6§l=== Événement: " + event.getType().getDisplayName() + " ===");
        sender.sendMessage("§7ID: §e" + event.getId());
        sender.sendMessage("§7Type: " + event.getType().getColor() + event.getType().getDisplayName());
        sender.sendMessage("§7Zone: §e" + event.getZone().getDisplayName() + " §7(ID: " + event.getZone().getId() + ")");
        sender.sendMessage("§7Location: §e" + formatLocation(event.getLocation()));
        sender.sendMessage("§7Status: " + (event.isCompleted() ? "§aTerminé" :
            (event.isFailed() ? "§cÉchoué" : "§eEn cours")));
        sender.sendMessage("§7Temps restant: §e" + event.getRemainingTimeSeconds() + "s §7(" +
            String.format("%.1f%%", event.getRemainingTimePercent() * 100) + ")");
        sender.sendMessage("§7Participants: §e" + event.getParticipants().size());
        sender.sendMessage("§7Debug: §e" + event.getDebugInfo());
    }

    private void handleStats(CommandSender sender, DynamicEventManager eventManager) {
        sender.sendMessage("§6§l=== Statistiques Événements ===");
        sender.sendMessage(eventManager.getStats());
        sender.sendMessage("");
        sender.sendMessage("§7Types activés:");
        for (DynamicEventType type : DynamicEventType.values()) {
            boolean enabled = eventManager.isTypeEnabled(type);
            sender.sendMessage("  §7- " + type.getColor() + type.getDisplayName() +
                " §7[" + (enabled ? "§aON" : "§cOFF") + "§7]");
        }
    }

    private void handleToggle(CommandSender sender, String[] args, DynamicEventManager eventManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzevent toggle <type>");
            return;
        }

        DynamicEventType type = DynamicEventType.fromConfigKey(args[1]);
        if (type == null) {
            sender.sendMessage("§cType d'événement invalide: " + args[1]);
            return;
        }

        boolean currentState = eventManager.isTypeEnabled(type);
        eventManager.setTypeEnabled(type, !currentState);

        sender.sendMessage("§7Type §e" + type.getDisplayName() + " §7: " +
            (!currentState ? "§aActivé" : "§cDésactivé"));
    }

    private void handleInterval(CommandSender sender, String[] args, DynamicEventManager eventManager) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzevent interval <min_secondes> <max_secondes>");
            return;
        }

        try {
            int min = Integer.parseInt(args[1]);
            int max = Integer.parseInt(args[2]);

            if (min < 30) {
                sender.sendMessage("§cL'intervalle minimum doit être d'au moins 30 secondes.");
                return;
            }
            if (max <= min) {
                sender.sendMessage("§cL'intervalle maximum doit être supérieur au minimum.");
                return;
            }

            eventManager.setEventInterval(min, max);
            sender.sendMessage("§a✓ Intervalle défini: §e" + min + "s §7- §e" + max + "s");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cNombres invalides.");
        }
    }

    private void handleDebug(CommandSender sender, DynamicEventManager eventManager) {
        for (String line : eventManager.getDebugInfo()) {
            sender.sendMessage(line);
        }
    }

    private String formatLocation(org.bukkit.Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("zombiez.admin.events")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("spawn", "list", "stop", "info", "stats", "toggle", "interval", "debug"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn", "toggle" -> {
                    for (DynamicEventType type : DynamicEventType.values()) {
                        completions.add(type.getConfigKey());
                    }
                }
                case "stop", "info" -> {
                    DynamicEventManager eventManager = plugin.getDynamicEventManager();
                    if (eventManager != null) {
                        completions.addAll(eventManager.getActiveEvents().keySet());
                    }
                    if (args[0].equalsIgnoreCase("stop")) {
                        completions.add("all");
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            // Complétion des joueurs
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
