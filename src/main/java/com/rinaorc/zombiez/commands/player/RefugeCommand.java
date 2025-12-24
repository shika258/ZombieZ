package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Refuge;
import com.rinaorc.zombiez.zones.Zone;
import com.rinaorc.zombiez.zones.gui.RefugeGUI;
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
 * Commande /refuge - Ouvre le menu des refuges ou affiche des informations
 * Usage:
 *   /refuge - Ouvre le menu GUI
 *   /refuge list - Liste tous les refuges
 *   /refuge info [id] - Affiche les infos d'un refuge
 *   /refuge tp <id> - Téléporte vers un refuge débloqué
 */
public class RefugeCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public RefugeCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        // Sans arguments, ouvrir le menu GUI
        if (args.length == 0) {
            openRefugeMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> showRefugeList(player);
            case "info" -> {
                if (args.length >= 2) {
                    try {
                        int id = Integer.parseInt(args[1]);
                        showRefugeInfo(player, id);
                    } catch (NumberFormatException e) {
                        MessageUtils.send(player, "§cUsage: /refuge info <id>");
                    }
                } else {
                    showNearbyRefugeInfo(player);
                }
            }
            case "tp", "teleport" -> {
                if (args.length >= 2) {
                    try {
                        int id = Integer.parseInt(args[1]);
                        teleportToRefuge(player, id);
                    } catch (NumberFormatException e) {
                        MessageUtils.send(player, "§cUsage: /refuge tp <id>");
                    }
                } else {
                    MessageUtils.send(player, "§cUsage: /refuge tp <id>");
                }
            }
            case "help" -> showHelp(player);
            default -> {
                // Essayer de parser comme un ID de refuge
                try {
                    int id = Integer.parseInt(subCommand);
                    showRefugeInfo(player, id);
                } catch (NumberFormatException e) {
                    openRefugeMenu(player);
                }
            }
        }

        return true;
    }

    /**
     * Ouvre le menu GUI des refuges
     */
    private void openRefugeMenu(Player player) {
        new RefugeGUI(plugin, player).open();
    }

    /**
     * Affiche la liste de tous les refuges
     */
    private void showRefugeList(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int currentCheckpoint = data != null ? data.getCurrentCheckpoint().get() : 0;

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         §e§l🏠 LISTE DES REFUGES");
        player.sendMessage("");

        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null || refugeManager.getAllRefuges().isEmpty()) {
            player.sendMessage("  §7Aucun refuge configuré.");
        } else {
            for (Refuge refuge : refugeManager.getRefugesSorted()) {
                String status = currentCheckpoint >= refuge.getId() ? "§a✓" : "§c✖";
                String checkpoint = currentCheckpoint == refuge.getId() ? " §7[ACTIF]" : "";
                player.sendMessage("  " + status + " §e" + refuge.getId() + ". §f" + refuge.getName() + checkpoint);
            }
        }

        player.sendMessage("");
        player.sendMessage("  §7Checkpoint actuel: §e" + (currentCheckpoint > 0 ? "#" + currentCheckpoint : "Aucun"));
        player.sendMessage("  §7Utilisez §e/refuge §7pour ouvrir le menu!");
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
    }

    /**
     * Affiche les informations d'un refuge spécifique
     */
    private void showRefugeInfo(Player player, int refugeId) {
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null) {
            MessageUtils.send(player, "§cErreur: Système de refuges non initialisé.");
            return;
        }

        Refuge refuge = refugeManager.getRefugeById(refugeId);
        if (refuge == null) {
            MessageUtils.send(player, "§cRefuge #" + refugeId + " introuvable.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int currentCheckpoint = data != null ? data.getCurrentCheckpoint().get() : 0;
        boolean isUnlocked = currentCheckpoint >= refugeId;

        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("  §e§l🏠 " + refuge.getName().toUpperCase());
        player.sendMessage("");

        if (refuge.getDescription() != null && !refuge.getDescription().isEmpty()) {
            player.sendMessage("  §7§o\"" + refuge.getDescription() + "\"");
            player.sendMessage("");
        }

        player.sendMessage("  §7Statut: " + (isUnlocked ? "§a✓ Débloqué" : "§c✖ Verrouillé"));
        if (currentCheckpoint == refugeId) {
            player.sendMessage("  §a§l→ CHECKPOINT ACTIF");
        }
        player.sendMessage("");
        player.sendMessage("  §7Coût d'activation: §6" + EconomyManager.formatPoints(refuge.getCost()));
        player.sendMessage("  §7Niveau requis: §e" + refuge.getRequiredLevel());
        player.sendMessage("");
        player.sendMessage("  §7Zone protégée:");
        player.sendMessage("    §8X: " + refuge.getProtectedMinX() + " → " + refuge.getProtectedMaxX());
        player.sendMessage("    §8Y: " + refuge.getProtectedMinY() + " → " + refuge.getProtectedMaxY());
        player.sendMessage("    §8Z: " + refuge.getProtectedMinZ() + " → " + refuge.getProtectedMaxZ());
        player.sendMessage("");

        if (isUnlocked) {
            player.sendMessage("  §aTip: §7/refuge tp " + refugeId + " §7pour vous téléporter!");
        }

        player.sendMessage("§8§m                                              ");
    }

    /**
     * Affiche les infos du refuge le plus proche
     */
    private void showNearbyRefugeInfo(Player player) {
        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null) {
            MessageUtils.send(player, "§cErreur: Système de refuges non initialisé.");
            return;
        }

        // Trouver le refuge dans lequel le joueur se trouve
        Refuge currentRefuge = refugeManager.getRefugeAt(player.getLocation());
        if (currentRefuge != null) {
            showRefugeInfo(player, currentRefuge.getId());
            return;
        }

        // Sinon afficher la liste
        showRefugeList(player);
    }

    /**
     * Téléporte le joueur vers un refuge débloqué
     */
    private void teleportToRefuge(Player player, int refugeId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        int currentCheckpoint = data != null ? data.getCurrentCheckpoint().get() : 0;

        // Vérifier que le refuge est débloqué
        if (currentCheckpoint < refugeId) {
            MessageUtils.send(player, "§c✖ Ce refuge n'est pas encore débloqué!");
            MessageUtils.send(player, "§7Trouvez le beacon dans le refuge pour l'activer.");
            MessageUtils.playSoundError(player);
            return;
        }

        var refugeManager = plugin.getRefugeManager();
        if (refugeManager == null) {
            MessageUtils.send(player, "§cErreur: Système de refuges non initialisé.");
            return;
        }

        Refuge refuge = refugeManager.getRefugeById(refugeId);
        if (refuge == null) {
            MessageUtils.send(player, "§cRefuge #" + refugeId + " introuvable.");
            return;
        }

        // Téléporter
        org.bukkit.Location spawnLoc = refuge.getSpawnLocation(player.getWorld());
        player.teleport(spawnLoc);

        MessageUtils.sendTitle(player, "§e§l" + refuge.getName(), "§7Téléportation réussie!", 10, 30, 10);
        MessageUtils.send(player, "§a✓ Téléporté vers §e" + refuge.getName() + "§a!");
        MessageUtils.playSoundSuccess(player);
    }

    /**
     * Affiche l'aide de la commande
     */
    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§e§l🏠 Aide - Commande /refuge");
        player.sendMessage("");
        player.sendMessage("§e/refuge §7- Ouvre le menu des refuges");
        player.sendMessage("§e/refuge list §7- Liste tous les refuges");
        player.sendMessage("§e/refuge info [id] §7- Infos d'un refuge");
        player.sendMessage("§e/refuge tp <id> §7- Téléportation vers un refuge");
        player.sendMessage("§e/refuge help §7- Affiche cette aide");
        player.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("info");
            completions.add("tp");
            completions.add("help");
            // Ajouter les IDs de refuges
            var refugeManager = plugin.getRefugeManager();
            if (refugeManager != null) {
                for (Refuge refuge : refugeManager.getAllRefuges()) {
                    completions.add(String.valueOf(refuge.getId()));
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("tp"))) {
            var refugeManager = plugin.getRefugeManager();
            if (refugeManager != null) {
                for (Refuge refuge : refugeManager.getAllRefuges()) {
                    completions.add(String.valueOf(refuge.getId()));
                }
            }
        }

        // Filtrer par ce que l'utilisateur a déjà tapé
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .toList();
    }
}
