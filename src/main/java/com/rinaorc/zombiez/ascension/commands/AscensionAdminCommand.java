package com.rinaorc.zombiez.ascension.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Commande admin /asadmin pour gérer le système d'Ascension
 */
public class AscensionAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public AscensionAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("zombiez.admin.ascension")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) {
            sender.sendMessage("§cLe système d'Ascension n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reset" -> handleReset(sender, args, manager);
            case "info" -> handleInfo(sender, args, manager);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * /asadmin reset <player>
     */
    private void handleReset(CommandSender sender, String[] args, AscensionManager manager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /asadmin reset <joueur>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage("§cJoueur '§e" + targetName + "§c' non trouvé ou non connecté.");
            return;
        }

        // Reset les données d'ascension du joueur
        manager.resetPlayer(target);

        sender.sendMessage("§a✓ §7Ascension de §e" + target.getName() + " §7réinitialisée.");
        sender.sendMessage("§8  → Mutations: §c0");
        sender.sendMessage("§8  → Stade: §c0");
        sender.sendMessage("§8  → Kills session: §c0");

        // Notifier le joueur ciblé
        target.sendMessage("§c⚠ §7Ton ascension a été réinitialisée par un administrateur.");
    }

    /**
     * /asadmin info <player>
     */
    private void handleInfo(CommandSender sender, String[] args, AscensionManager manager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /asadmin info <joueur>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage("§cJoueur '§e" + targetName + "§c' non trouvé ou non connecté.");
            return;
        }

        var data = manager.getData(target);
        if (data == null) {
            sender.sendMessage("§cAucune donnée d'ascension pour §e" + target.getName());
            return;
        }

        sender.sendMessage("§6§l⬆ Ascension de §e" + target.getName());
        sender.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§7Stade: §f" + data.getCurrentStage() + "/10");
        sender.sendMessage("§7Kills session: §f" + data.getSessionKills().get());
        sender.sendMessage("§7Mutations actives: §f" + data.getActiveMutations().size());

        if (!data.getActiveMutations().isEmpty()) {
            sender.sendMessage("§7Liste:");
            for (var mutation : data.getActiveMutations()) {
                sender.sendMessage("  §8- " + mutation.getFormattedName());
            }
        }

        sender.sendMessage("§7Choix en attente: " + (data.isChoicePending() ? "§aOui" : "§cNon"));
        sender.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l⬆ Ascension Admin");
        sender.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§e/asadmin reset <joueur> §8- §7Reset l'ascension d'un joueur");
        sender.sendMessage("§e/asadmin info <joueur> §8- §7Voir les infos d'ascension");
        sender.sendMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin.ascension")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Sous-commandes
            List<String> subCommands = List.of("reset", "info");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Noms de joueurs
            String input = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}
