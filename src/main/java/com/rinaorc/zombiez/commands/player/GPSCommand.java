package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.navigation.GPSManager;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande /gps - Active/Désactive le système GPS pour les étapes du Journey
 *
 * Usage:
 * - /gps - Toggle le GPS
 * - /gps on - Active le GPS
 * - /gps off - Désactive le GPS
 * - /gps info - Affiche les informations de destination
 */
public class GPSCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public GPSCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        GPSManager gpsManager = plugin.getGPSManager();
        if (gpsManager == null) {
            player.sendMessage("§cLe système GPS n'est pas disponible.");
            return true;
        }

        // Sans argument: toggle
        if (args.length == 0) {
            gpsManager.toggleGPS(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "on", "activer", "enable" -> {
                if (gpsManager.isGPSActive(player)) {
                    player.sendMessage("§e§l! §7Le GPS est déjà activé.");
                } else {
                    gpsManager.enableGPS(player);
                }
            }

            case "off", "desactiver", "disable" -> {
                if (!gpsManager.isGPSActive(player)) {
                    player.sendMessage("§e§l! §7Le GPS est déjà désactivé.");
                } else {
                    gpsManager.disableGPS(player);
                }
            }

            case "info", "status" -> {
                showGPSInfo(player, gpsManager);
            }

            case "help", "?" -> {
                showHelp(player);
            }

            default -> {
                player.sendMessage("§cUsage: /gps [on|off|info|help]");
            }
        }

        return true;
    }

    /**
     * Affiche les informations GPS
     */
    private void showGPSInfo(Player player, GPSManager gpsManager) {
        JourneyManager journeyManager = plugin.getJourneyManager();
        JourneyStep step = journeyManager.getCurrentStep(player);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§l⚲ §6§lGPS §8- §7Informations");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Statut
        boolean active = gpsManager.isGPSActive(player);
        player.sendMessage("  §7Statut: " + (active ? "§a§lACTIF" : "§c§lINACTIF"));

        // Étape actuelle
        if (step != null) {
            player.sendMessage("  §7Étape: §f" + step.getName());
            player.sendMessage("  §7Description: §7" + step.getDescription());

            // Destination
            Location dest = gpsManager.getDestinationForPlayer(player);
            if (dest != null) {
                player.sendMessage("");
                player.sendMessage("  §a§l➤ §eDestination:");
                player.sendMessage("    §7X: §f" + dest.getBlockX());
                player.sendMessage("    §7Y: §f" + dest.getBlockY());
                player.sendMessage("    §7Z: §f" + dest.getBlockZ());

                // Distance
                double distance = player.getLocation().distance(dest);
                String distanceStr = distance < 100
                    ? String.format("%.1f", distance) + " blocs"
                    : String.format("%.0f", distance) + " blocs";
                player.sendMessage("    §7Distance: §e" + distanceStr);
            } else {
                player.sendMessage("");
                player.sendMessage("  §c§l✗ §7Aucune coordonnée disponible pour cette étape.");
            }
        } else {
            player.sendMessage("  §7Aucune étape active.");
        }

        player.sendMessage("");
        player.sendMessage("  §8Utilise §e/gps §8pour activer/désactiver.");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
    }

    /**
     * Affiche l'aide
     */
    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§l⚲ §6§lGPS §8- §7Aide");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
        player.sendMessage("  §e/gps §8- §7Active/Désactive le GPS");
        player.sendMessage("  §e/gps on §8- §7Active le GPS");
        player.sendMessage("  §e/gps off §8- §7Désactive le GPS");
        player.sendMessage("  §e/gps info §8- §7Affiche les informations");
        player.sendMessage("");
        player.sendMessage("  §7Le GPS affiche une flèche devant toi");
        player.sendMessage("  §7qui pointe vers ta destination.");
        player.sendMessage("");
        player.sendMessage("  §8Fonctionne pour les étapes du Journey");
        player.sendMessage("  §8qui ont des coordonnées.");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String option : List.of("on", "off", "info", "help")) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }

            return completions;
        }

        return List.of();
    }
}
