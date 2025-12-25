package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
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
 * Commande /class pour gerer les classes
 * Usage:
 * - /class - Ouvre le menu principal
 * - /class select - Ouvre la selection de classe
 * - /class info - Affiche les infos de classe
 * - /mutations - Affiche les mutations du jour
 */
public class ClassCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;

    public ClassCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getClassManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est reservee aux joueurs!");
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select", "choose", "choisir" -> {
                plugin.getClassSelectionGUI().open(player);
            }

            case "info", "stats" -> {
                ClassData data = classManager.getClassData(player);
                if (data.hasClass()) {
                    plugin.getClassInfoGUI().open(player);
                } else {
                    player.sendMessage("§cVous n'avez pas encore de classe!");
                    player.sendMessage("§7Utilisez /class select pour en choisir une.");
                }
            }

            case "mutations", "mutation", "daily" -> {
                showMutations(player);
            }

            case "help", "aide" -> {
                showHelp(player);
            }

            default -> {
                // Essayer comme nom de classe direct
                ClassType classType = ClassType.fromId(subCommand);
                if (classType != null) {
                    // Vérifier si le joueur peut accéder à la sélection de classe
                    if (!plugin.getJourneyManager().canAccessClassSelection(player)) {
                        player.sendMessage("§c§l⚠ §cTu dois terminer le Chapitre 1 du Parcours pour choisir une classe!");
                        player.sendMessage("§7Tape §e/journey §7pour voir ta progression.");
                        return true;
                    }
                    classManager.selectClass(player, classType);
                } else {
                    player.sendMessage("§cCommande inconnue! Utilisez /class help");
                }
            }
        }

        return true;
    }

    private void openMainMenu(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            plugin.getClassSelectionGUI().open(player);
        } else {
            plugin.getClassInfoGUI().open(player);
        }
    }

    private void showMutations(Player player) {
        List<String> summary = classManager.getMutationManager().getMutationSummary();
        for (String line : summary) {
            player.sendMessage(line);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l+ COMMANDES DE CLASSE +");
        player.sendMessage("");
        player.sendMessage("§e/class §7- Ouvre le menu principal");
        player.sendMessage("§e/class select §7- Choisir une classe");
        player.sendMessage("§e/class info §7- Informations de classe");
        player.sendMessage("§e/class mutations §7- Voir les mutations du jour");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "select", "info", "mutations", "help"
            ));
            // Ajouter les noms de classe
            for (ClassType type : ClassType.values()) {
                completions.add(type.name().toLowerCase());
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
    }
}
