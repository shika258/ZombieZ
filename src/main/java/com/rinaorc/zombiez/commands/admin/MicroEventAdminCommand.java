package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventManager;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande admin pour gérer les micro-événements
 *
 * Usage:
 * /zzmicro spawn <type> [player] - Force le spawn d'un micro-event
 * /zzmicro list - Liste les micro-events actifs
 * /zzmicro stop <player|all> - Arrête le micro-event d'un joueur
 * /zzmicro stats - Statistiques du système
 * /zzmicro toggle - Active/désactive le système
 * /zzmicro records - Affiche les records de Course Mortelle
 */
public class MicroEventAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public MicroEventAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MicroEventManager manager = plugin.getMicroEventManager();
        if (manager == null) {
            sender.sendMessage("§cLe système de micro-événements n'est pas initialisé.");
            return true;
        }

        if (!sender.hasPermission("zombiez.admin.microevents")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn" -> handleSpawn(sender, args, manager);
            case "list" -> handleList(sender, manager);
            case "stop" -> handleStop(sender, args, manager);
            case "stats" -> handleStats(sender, manager);
            case "toggle" -> handleToggle(sender, manager);
            case "records" -> handleRecords(sender, manager);
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== ZombieZ - Micro-Événements ===");
        sender.sendMessage("§e/zzmicro spawn <type> [player] §7- Force le spawn d'un micro-event");
        sender.sendMessage("§e/zzmicro list §7- Liste les micro-events actifs");
        sender.sendMessage("§e/zzmicro stop <player|all> §7- Arrête le micro-event d'un joueur");
        sender.sendMessage("§e/zzmicro stats §7- Statistiques du système");
        sender.sendMessage("§e/zzmicro toggle §7- Active/désactive le système");
        sender.sendMessage("§e/zzmicro records §7- Affiche les records de Course Mortelle");
        sender.sendMessage("");
        sender.sendMessage("§7Types disponibles:");
        for (MicroEventType type : MicroEventType.values()) {
            sender.sendMessage("  §7- " + type.getColor() + type.getIcon() + " " + type.getDisplayName() +
                " §7(" + type.getConfigKey() + ")");
        }
    }

    private void handleSpawn(CommandSender sender, String[] args, MicroEventManager manager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzmicro spawn <type> [player]");
            return;
        }

        MicroEventType type = MicroEventType.fromConfigKey(args[1]);
        if (type == null) {
            sender.sendMessage("§cType de micro-event invalide: " + args[1]);
            sender.sendMessage("§7Types valides: " + Arrays.stream(MicroEventType.values())
                .map(MicroEventType::getConfigKey)
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
            sender.sendMessage("§cSpécifiez un joueur: /zzmicro spawn " + args[1] + " <player>");
            return;
        }

        MicroEvent event = manager.forceSpawnEvent(type, target);
        if (event != null) {
            sender.sendMessage("§a✓ Micro-event " + type.getColor() + type.getIcon() + " " +
                type.getDisplayName() + " §aspawné pour §e" + target.getName());
        } else {
            sender.sendMessage("§cÉchec du spawn. Le joueur est peut-être dans une zone safe.");
        }
    }

    private void handleList(CommandSender sender, MicroEventManager manager) {
        Map<UUID, MicroEvent> events = manager.getActiveEvents();

        if (events.isEmpty()) {
            sender.sendMessage("§7Aucun micro-event actif.");
            return;
        }

        sender.sendMessage("§6§l=== Micro-Événements Actifs (" + events.size() + ") ===");

        for (MicroEvent event : events.values()) {
            String playerName = event.getPlayer() != null ? event.getPlayer().getName() : "???";
            String status = event.isCompleted() ? "§a[TERMINÉ]" :
                (event.isFailed() ? "§c[ÉCHOUÉ]" : "§e[EN COURS]");

            sender.sendMessage("");
            sender.sendMessage(event.getType().getColor() + "§l" + event.getType().getIcon() +
                " " + event.getType().getDisplayName() + " " + status);
            sender.sendMessage("§7  Joueur: §e" + playerName);
            sender.sendMessage("§7  Zone: §e" + event.getZone().getDisplayName());
            sender.sendMessage("§7  Temps restant: §e" + event.getRemainingTimeSeconds() + "s");
        }
    }

    private void handleStop(CommandSender sender, String[] args, MicroEventManager manager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzmicro stop <player|all>");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            int count = manager.getActiveEvents().size();
            for (UUID playerId : new ArrayList<>(manager.getActiveEvents().keySet())) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    manager.stopPlayerEvent(player);
                }
            }
            sender.sendMessage("§a✓ " + count + " micro-event(s) arrêté(s).");
        } else {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJoueur non trouvé: " + args[1]);
                return;
            }

            if (manager.stopPlayerEvent(target)) {
                sender.sendMessage("§a✓ Micro-event de " + target.getName() + " arrêté.");
            } else {
                sender.sendMessage("§c" + target.getName() + " n'a pas de micro-event actif.");
            }
        }
    }

    private void handleStats(CommandSender sender, MicroEventManager manager) {
        sender.sendMessage("§6§l=== Statistiques Micro-Événements ===");
        sender.sendMessage(manager.getStats());
        sender.sendMessage("");
        sender.sendMessage("§7Système: " + (manager.isEnabled() ? "§aActivé" : "§cDésactivé"));
    }

    private void handleToggle(CommandSender sender, MicroEventManager manager) {
        boolean newState = !manager.isEnabled();
        manager.setEnabled(newState);
        sender.sendMessage("§7Système de micro-événements: " + (newState ? "§aActivé" : "§cDésactivé"));
    }

    private void handleRecords(CommandSender sender, MicroEventManager manager) {
        Map<Integer, MicroEventManager.DeathRaceRecord> records = manager.getDeathRaceRecords();

        if (records.isEmpty()) {
            sender.sendMessage("§7Aucun record de Course Mortelle enregistré.");
            return;
        }

        sender.sendMessage("§6§l=== Records Course Mortelle 🏃 ===");

        List<Integer> sortedZones = new ArrayList<>(records.keySet());
        Collections.sort(sortedZones);

        for (int zoneId : sortedZones) {
            MicroEventManager.DeathRaceRecord record = records.get(zoneId);
            sender.sendMessage("§7Zone §e" + zoneId + "§7: §b" + record.getPlayerName() +
                " §7- §a" + String.format("%.2f", record.getTimeSeconds()) + "s");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("zombiez.admin.microevents")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("spawn", "list", "stop", "stats", "toggle", "records"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn" -> {
                    for (MicroEventType type : MicroEventType.values()) {
                        completions.add(type.getConfigKey());
                    }
                }
                case "stop" -> {
                    completions.add("all");
                    MicroEventManager manager = plugin.getMicroEventManager();
                    if (manager != null) {
                        for (UUID playerId : manager.getActiveEvents().keySet()) {
                            Player player = plugin.getServer().getPlayer(playerId);
                            if (player != null) {
                                completions.add(player.getName());
                            }
                        }
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
