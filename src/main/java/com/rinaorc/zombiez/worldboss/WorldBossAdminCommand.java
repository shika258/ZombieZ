package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande admin pour gérer le système de World Boss
 *
 * Usage:
 * /zzworldboss spawn <type> - Spawn un World Boss au joueur
 * /zzworldboss list - Liste les boss actifs
 * /zzworldboss clear - Supprime tous les boss
 * /zzworldboss stats - Affiche les statistiques
 * /zzworldboss force - Force un check de spawn
 * /zzworldboss toggle - Active/désactive le système
 */
public class WorldBossAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final WorldBossManager manager;

    public WorldBossAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getWorldBossManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombiez.admin.worldboss")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "list" -> handleList(sender);
            case "clear" -> handleClear(sender);
            case "stats" -> handleStats(sender);
            case "force" -> handleForce(sender);
            case "toggle" -> handleToggle(sender);
            case "info" -> handleInfo(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== World Boss Admin ===");
        sender.sendMessage("§e/zzworldboss spawn <type> §7- Spawn un boss");
        sender.sendMessage("§e/zzworldboss list §7- Liste les boss actifs");
        sender.sendMessage("§e/zzworldboss clear §7- Supprime tous les boss");
        sender.sendMessage("§e/zzworldboss stats §7- Statistiques");
        sender.sendMessage("§e/zzworldboss force §7- Force un spawn");
        sender.sendMessage("§e/zzworldboss toggle §7- Active/désactive");
        sender.sendMessage("§e/zzworldboss info §7- Info système");
        sender.sendMessage("");
        sender.sendMessage("§7Types disponibles:");
        for (WorldBossType type : WorldBossType.values()) {
            sender.sendMessage("§7- §e" + type.name() + " §7(" + type.getDisplayName() + ")");
        }
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande doit être exécutée par un joueur.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzworldboss spawn <type>");
            sender.sendMessage("§7Types: " + Arrays.stream(WorldBossType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", ")));
            return;
        }

        WorldBossType type;
        try {
            type = WorldBossType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType de boss invalide: " + args[1]);
            return;
        }

        boolean success = manager.forceSpawn(type, player.getLocation());
        if (success) {
            sender.sendMessage("§a✓ " + type.getDisplayName() + " spawné avec succès!");
        } else {
            sender.sendMessage("§c✗ Échec du spawn du boss.");
        }
    }

    private void handleList(CommandSender sender) {
        var activeBosses = manager.getActiveBossInfo();

        if (activeBosses.isEmpty()) {
            sender.sendMessage("§7Aucun World Boss actif.");
            return;
        }

        sender.sendMessage("§6§l=== World Boss Actifs ===");
        for (String info : activeBosses) {
            sender.sendMessage(info);
        }
    }

    private void handleClear(CommandSender sender) {
        int count = manager.getActiveBosses().size();
        manager.clearAllBosses();
        sender.sendMessage("§a✓ " + count + " World Boss supprimés.");
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6§l=== Statistiques World Boss ===");
        sender.sendMessage("§7" + manager.getStats());
    }

    private void handleForce(CommandSender sender) {
        boolean spawned = manager.trySpawnBoss();
        if (spawned) {
            sender.sendMessage("§a✓ World Boss spawné avec succès!");
        } else {
            sender.sendMessage("§e⚠ Aucun boss spawné (conditions non remplies ou pas de chance).");
        }
    }

    private void handleToggle(CommandSender sender) {
        boolean newState = !manager.isEnabled();
        manager.setEnabled(newState);
        sender.sendMessage("§7Système World Boss: " + (newState ? "§aActivé" : "§cDésactivé"));
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage("§6§l=== Info Système World Boss ===");
        sender.sendMessage("§7Activé: " + (manager.isEnabled() ? "§aOui" : "§cNon"));
        sender.sendMessage("§7Boss actifs: §e" + manager.getActiveBosses().size() + "/" + manager.getMaxConcurrentBosses());
        sender.sendMessage("§7Intervalle: §e" + manager.getMinSpawnIntervalMinutes() + "-" + manager.getMaxSpawnIntervalMinutes() + " min");
        sender.sendMessage("§7Chance: §e" + (manager.getSpawnChance() * 100) + "%");
        sender.sendMessage("§7Rayon spawn: §e" + manager.getMinSpawnRadius() + "-" + manager.getMaxSpawnRadius() + " blocs");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("spawn", "list", "clear", "stats", "force", "toggle", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            completions.addAll(Arrays.stream(WorldBossType.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
    }
}
