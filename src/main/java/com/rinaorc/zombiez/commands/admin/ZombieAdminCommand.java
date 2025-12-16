package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.weather.WeatherType;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.spawning.BossSpawnSystem;
import com.rinaorc.zombiez.zombies.spawning.HordeEventSystem;
import com.rinaorc.zombiez.zombies.spawning.SpawnSystem;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commandes d'administration pour le système de zombies
 * /zzzombie <spawn|wave|boss|event|stats|toggle|kill>
 */
public class ZombieAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ZombieAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin.zombies")) {
            sender.sendMessage("§cVous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "wave" -> handleWave(sender, args);
            case "boss" -> handleBoss(sender, args);
            case "event" -> handleEvent(sender, args);
            case "stats" -> handleStats(sender);
            case "toggle" -> handleToggle(sender, args);
            case "kill" -> handleKill(sender, args);
            case "bloodmoon" -> handleBloodMoon(sender, args);
            case "patientzero" -> handlePatientZero(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== ZombieZ Zombie Admin ===");
        sender.sendMessage("§e/zzzombie spawn <type> [level] §7- Spawn un zombie");
        sender.sendMessage("§e/zzzombie wave <count> [zone] §7- Spawn une vague");
        sender.sendMessage("§e/zzzombie boss <type|zone> §7- Spawn un boss");
        sender.sendMessage("§e/zzzombie event <type> §7- Déclencher un événement");
        sender.sendMessage("§e/zzzombie stats §7- Statistiques du système");
        sender.sendMessage("§e/zzzombie toggle <spawn|events> §7- Toggle les systèmes");
        sender.sendMessage("§e/zzzombie kill <all|zone|radius> §7- Tuer des zombies");
        sender.sendMessage("§e/zzzombie bloodmoon <start|stop> §7- Blood Moon");
        sender.sendMessage("§e/zzzombie patientzero §7- Spawn Patient Zéro");
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie spawn <type> [level]");
            sender.sendMessage("§7Types: " + Arrays.stream(ZombieType.values())
                .filter(t -> !t.isBoss())
                .limit(10)
                .map(ZombieType::name)
                .collect(Collectors.joining(", ")) + "...");
            return;
        }

        ZombieType type;
        try {
            type = ZombieType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType invalide: " + args[1]);
            return;
        }

        int level = 1;
        if (args.length >= 3) {
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNiveau invalide!");
                return;
            }
        }

        var zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        int zoneId = zone != null ? zone.getId() : 1;

        ZombieManager.ActiveZombie zombie = plugin.getZombieManager().spawnZombie(
            type, player.getLocation().add(3, 0, 3), level
        );

        if (zombie != null) {
            sender.sendMessage("§aZombie spawné: §e" + type.getDisplayName() + " §7Lv." + level);
        } else {
            sender.sendMessage("§cÉchec du spawn!");
        }
    }

    private void handleWave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie wave <count> [zone]");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cNombre invalide!");
            return;
        }

        var zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        int zoneId = zone != null ? zone.getId() : 1;

        if (args.length >= 3) {
            try {
                zoneId = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cZone invalide!");
                return;
            }
        }

        plugin.getSpawnSystem().spawnWave(player.getLocation(), count, zoneId);
        sender.sendMessage("§aVague de §e" + count + " §azombies spawnée dans la zone §e" + zoneId);
    }

    private void handleBoss(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie boss <type|zone>");
            sender.sendMessage("§7Types: BUTCHER, WIDOW, THE_GIANT, THE_PHANTOM");
            sender.sendMessage("§7Zones: 1, 4, 8, 9 (boss de zone)");
            return;
        }

        // Essayer comme type de boss
        try {
            ZombieType type = ZombieType.valueOf(args[1].toUpperCase());
            if (type.isBoss()) {
                var zombie = plugin.getZombieManager().spawnZombie(type, player.getLocation(), 10);
                if (zombie != null) {
                    sender.sendMessage("§aBoss spawné: §e" + type.getDisplayName());
                } else {
                    sender.sendMessage("§cÉchec du spawn!");
                }
                return;
            }
        } catch (IllegalArgumentException ignored) {}

        // Essayer comme zone
        try {
            int zoneId = Integer.parseInt(args[1]);
            plugin.getBossSpawnSystem().spawnZoneBoss(zoneId, player.getLocation());
            sender.sendMessage("§aBoss de zone §e" + zoneId + " §aspawné!");
        } catch (NumberFormatException e) {
            sender.sendMessage("§cArgument invalide!");
        }
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie event <type>");
            sender.sendMessage("§7Types: " + Arrays.stream(HordeEventSystem.HordeEventType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", ")));
            return;
        }

        HordeEventSystem.HordeEventType eventType;
        try {
            eventType = HordeEventSystem.HordeEventType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType d'événement invalide!");
            return;
        }

        var zone = plugin.getZoneManager().getZoneAt(player.getLocation());
        int zoneId = zone != null ? zone.getId() : 1;

        plugin.getHordeEventSystem().startEvent(zoneId, player.getLocation(), eventType);
        sender.sendMessage("§aÉvénement §e" + eventType.getDisplayName() + " §adémarré!");
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6=== ZombieZ Stats ===");
        sender.sendMessage("§7Zombies: §e" + plugin.getZombieManager().getStats());
        sender.sendMessage("§7Spawn: §e" + plugin.getSpawnSystem().getStats());
        sender.sendMessage("§7Events: §e" + plugin.getHordeEventSystem().getStats());
        sender.sendMessage("§7Boss: §e" + plugin.getBossSpawnSystem().getStats());
        
        // Zombies par zone
        sender.sendMessage("§7Zombies par zone:");
        for (int i = 1; i <= 11; i++) {
            int count = plugin.getZombieManager().getZombieCount(i);
            if (count > 0) {
                sender.sendMessage("  §7Zone " + i + ": §e" + count);
            }
        }
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie toggle <spawn|events>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                boolean newState = !plugin.getSpawnSystem().isEnabled();
                plugin.getSpawnSystem().setEnabled(newState);
                sender.sendMessage("§aSpawn système: " + (newState ? "§eACTIVÉ" : "§cDÉSACTIVÉ"));
            }
            default -> sender.sendMessage("§cOption invalide!");
        }
    }

    private void handleKill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie kill <all|zone|radius>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "all" -> {
                int count = killAllZombies();
                sender.sendMessage("§a" + count + " zombies tués!");
            }
            case "zone" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement!");
                    return;
                }
                var zone = plugin.getZoneManager().getZoneAt(player.getLocation());
                int zoneId = zone != null ? zone.getId() : 0;
                int count = killZombiesInZone(zoneId);
                sender.sendMessage("§a" + count + " zombies tués dans la zone " + zoneId + "!");
            }
            case "radius" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement!");
                    return;
                }
                int radius = args.length >= 3 ? Integer.parseInt(args[2]) : 50;
                int count = killZombiesNear(player, radius);
                sender.sendMessage("§a" + count + " zombies tués dans un rayon de " + radius + "!");
            }
            default -> sender.sendMessage("§cOption invalide!");
        }
    }

    private void handleBloodMoon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzzombie bloodmoon <start|stop>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "start" -> {
                plugin.getWeatherManager().forceWeather(WeatherType.BLOOD_MOON);
                sender.sendMessage("§cBlood Moon démarrée!");
            }
            case "stop" -> {
                plugin.getWeatherManager().clearWeather();
                sender.sendMessage("§aBlood Moon terminée!");
            }
            default -> sender.sendMessage("§cOption invalide!");
        }
    }

    private void handlePatientZero(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return;
        }

        sender.sendMessage("§4Spawning Patient Zéro...");
        plugin.getBossSpawnSystem().spawnPatientZero(player.getLocation());
    }

    private int killAllZombies() {
        int count = 0;
        for (var world : plugin.getServer().getWorlds()) {
            for (var entity : world.getEntities()) {
                if (plugin.getZombieManager().isZombieZMob(entity)) {
                    entity.remove();
                    count++;
                }
            }
        }
        return count;
    }

    private int killZombiesInZone(int zoneId) {
        int count = 0;
        for (var world : plugin.getServer().getWorlds()) {
            for (var entity : world.getEntities()) {
                if (plugin.getZombieManager().isZombieZMob(entity)) {
                    var zone = plugin.getZoneManager().getZoneAt(entity.getLocation());
                    if (zone != null && zone.getId() == zoneId) {
                        entity.remove();
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private int killZombiesNear(Player player, int radius) {
        int count = 0;
        for (var entity : player.getNearbyEntities(radius, radius, radius)) {
            if (plugin.getZombieManager().isZombieZMob(entity)) {
                entity.remove();
                count++;
            }
        }
        return count;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "spawn", "wave", "boss", "event", "stats", "toggle", "kill", "bloodmoon", "patientzero"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn" -> {
                    for (ZombieType type : ZombieType.values()) {
                        if (!type.isBoss()) {
                            completions.add(type.name());
                        }
                    }
                }
                case "boss" -> {
                    completions.addAll(Arrays.asList("BUTCHER", "WIDOW", "THE_GIANT", "THE_PHANTOM", "1", "4", "8", "9"));
                }
                case "event" -> {
                    for (HordeEventSystem.HordeEventType type : HordeEventSystem.HordeEventType.values()) {
                        completions.add(type.name());
                    }
                }
                case "toggle" -> completions.addAll(Arrays.asList("spawn", "events"));
                case "kill" -> completions.addAll(Arrays.asList("all", "zone", "radius"));
                case "bloodmoon" -> completions.addAll(Arrays.asList("start", "stop"));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
