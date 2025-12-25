package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyChapter;
import com.rinaorc.zombiez.progression.journey.JourneyGUI;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
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
 * Commande /journey pour accéder au système de Parcours du Survivant
 *
 * Usage:
 * - /journey : Ouvre le menu principal du parcours
 * - /journey info : Affiche l'étape actuelle dans le chat
 * - /journey chapter [id] : Ouvre les détails d'un chapitre
 */
public class JourneyCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final JourneyGUI gui;

    public JourneyCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.gui = new JourneyGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        JourneyManager manager = plugin.getJourneyManager();

        if (args.length == 0) {
            // Ouvrir le menu principal
            gui.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info", "i" -> showCurrentStepInfo(player, manager);
            case "chapter", "chapitre", "c" -> {
                if (args.length > 1) {
                    try {
                        int chapterId = Integer.parseInt(args[1]);
                        JourneyChapter chapter = JourneyChapter.getById(chapterId);

                        // Vérifier si le joueur peut voir ce chapitre
                        JourneyChapter current = manager.getCurrentChapter(player);
                        if (chapter.getId() > current.getId() && !manager.isChapterCompleted(player, chapter)) {
                            player.sendMessage("§cCe chapitre n'est pas encore accessible!");
                            return true;
                        }

                        gui.openChapterDetail(player, chapter);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cNuméro de chapitre invalide!");
                    }
                } else {
                    // Ouvrir le chapitre actuel
                    gui.openChapterDetail(player, manager.getCurrentChapter(player));
                }
            }
            case "progress", "p" -> showProgressSummary(player, manager);
            case "help", "?" -> showHelp(player);
            default -> {
                player.sendMessage("§cCommande inconnue. Utilise §e/journey help §cpour l'aide.");
            }
        }

        return true;
    }

    private void showCurrentStepInfo(Player player, JourneyManager manager) {
        JourneyStep step = manager.getCurrentStep(player);
        JourneyChapter chapter = manager.getCurrentChapter(player);

        if (step == null) {
            player.sendMessage("§7Tu as complété tout le journal!");
            return;
        }

        int progress = manager.getStepProgress(player, step);
        double percent = step.getProgressPercent(progress);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§lÉTAPE ACTUELLE");
        player.sendMessage("");
        player.sendMessage("  §7Chapitre: " + chapter.getFormattedTitle());
        player.sendMessage("  §7Étape " + step.getStepNumber() + ": §f" + step.getName());
        player.sendMessage("");
        player.sendMessage("  §7Objectif: §e" + step.getDescription());
        player.sendMessage("  §7Progression: §a" + String.format("%.1f", percent) + "% §7(" + step.getProgressText(progress) + ")");
        player.sendMessage("");
        player.sendMessage("  §7Récompenses: §e+" + step.getPointReward() + " Points §8| §d+" + step.getGemReward() + " Gems");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
    }

    private void showProgressSummary(Player player, JourneyManager manager) {
        double overall = manager.getOverallProgress(player);
        int completedChapters = manager.getCompletedChaptersCount(player);
        JourneyChapter current = manager.getCurrentChapter(player);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §6§lJOURNAL DU SURVIVANT");
        player.sendMessage("");
        player.sendMessage("  §7Progression globale: §e" + String.format("%.1f", overall) + "%");
        player.sendMessage("  §7Chapitres complétés: §a" + completedChapters + "§7/§a12");
        player.sendMessage("  §7Chapitre actuel: " + current.getColoredName());
        player.sendMessage("  §7Phase: " + current.getPhaseName());
        player.sendMessage("");

        // Prochains déblocages
        if (current.getUnlocks().length > 0) {
            player.sendMessage("  §7Prochains déblocages:");
            for (var gate : current.getUnlocks()) {
                player.sendMessage("  §a  🔓 " + gate.getDisplayName());
            }
        }

        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§l/JOURNEY - Aide");
        player.sendMessage("");
        player.sendMessage("  §e/journey §8- §7Ouvre le menu du journal");
        player.sendMessage("  §e/journey info §8- §7Affiche l'étape actuelle");
        player.sendMessage("  §e/journey chapter [n] §8- §7Voir un chapitre");
        player.sendMessage("  §e/journey progress §8- §7Résumé de progression");
        player.sendMessage("");
        player.sendMessage("  §7Le journal te guide dans ta progression.");
        player.sendMessage("  §c⚠ Les zones et fonctionnalités sont BLOQUÉES");
        player.sendMessage("  §ctant que les chapitres ne sont pas complétés!");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("info", "chapter", "progress", "help");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chapter")) {
            for (int i = 1; i <= 12; i++) {
                String num = String.valueOf(i);
                if (num.startsWith(args[1])) {
                    completions.add(num);
                }
            }
        }

        return completions;
    }
}
