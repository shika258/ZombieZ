package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.progression.journey.JourneyChapter;
import com.rinaorc.zombiez.progression.journey.JourneyGate;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import org.bukkit.Bukkit;
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

/**
 * Commandes d'administration pour le système de parcours (Journey)
 * /zzjourneyadmin <subcommand> <player> [args...]
 */
public class JourneyAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "info", "skip", "unlockall", "reset", "setchapter", "setstep"
    );

    public JourneyAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("zombiez.admin.journey")) {
            sender.sendMessage("§cVous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info" -> handleInfo(sender, args);
            case "skip" -> handleSkip(sender, args);
            case "unlockall" -> handleUnlockAll(sender, args);
            case "reset" -> handleReset(sender, args);
            case "setchapter" -> handleSetChapter(sender, args);
            case "setstep" -> handleSetStep(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lZOMBIEZ §7- Commandes Admin Journey");
        sender.sendMessage("");
        sender.sendMessage("§e/zzjourneyadmin info <joueur> §8- §7Voir l'état du journal");
        sender.sendMessage("§e/zzjourneyadmin skip <joueur> §8- §7Passer au chapitre suivant");
        sender.sendMessage("§e/zzjourneyadmin unlockall <joueur> §8- §7Tout débloquer");
        sender.sendMessage("§e/zzjourneyadmin reset <joueur> §8- §7Réinitialiser le journal");
        sender.sendMessage("§e/zzjourneyadmin setchapter <joueur> <1-12> §8- §7Définir le chapitre");
        sender.sendMessage("§e/zzjourneyadmin setstep <joueur> <1-10> §8- §7Définir l'étape");
        sender.sendMessage("§8§m                                        ");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzjourneyadmin info <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        JourneyManager manager = plugin.getJourneyManager();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées joueur non trouvées!");
            return;
        }

        JourneyChapter chapter = manager.getCurrentChapter(target);
        JourneyStep step = manager.getCurrentStep(target);
        int completedChapters = manager.getCompletedChaptersCount(target);
        double progress = manager.getOverallProgress(target);

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lPARCOURS §7- " + target.getName());
        sender.sendMessage("");
        sender.sendMessage("§7Progression: §e" + String.format("%.1f", progress) + "%");
        sender.sendMessage("§7Chapitres: §a" + completedChapters + "§7/12");
        sender.sendMessage("§7Chapitre actuel: " + chapter.getColoredName());
        if (step != null) {
            sender.sendMessage("§7Étape actuelle: §f" + step.getName());
            int stepProgress = manager.getStepProgress(target, step);
            sender.sendMessage("§7Progression étape: §e" + step.getProgressText(stepProgress));
        } else {
            sender.sendMessage("§aJournal complété!");
        }
        sender.sendMessage("§8§m                                        ");
    }

    private void handleSkip(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzjourneyadmin skip <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        JourneyManager manager = plugin.getJourneyManager();
        JourneyChapter currentChapter = manager.getCurrentChapter(target);

        // Compléter le chapitre actuel
        skipChapter(target, currentChapter);

        sender.sendMessage("§a✓ Chapitre " + currentChapter.getId() + " passé pour " + target.getName());

        JourneyChapter newChapter = manager.getCurrentChapter(target);
        sender.sendMessage("§7Nouveau chapitre: " + newChapter.getColoredName());

        // Mettre à jour la BossBar
        manager.createOrUpdateBossBar(target);
    }

    private void handleUnlockAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzjourneyadmin unlockall <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        unlockAllChapters(target);

        sender.sendMessage("§a✓ Tous les chapitres et gates débloqués pour " + target.getName());

        // Mettre à jour la BossBar
        plugin.getJourneyManager().createOrUpdateBossBar(target);
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzjourneyadmin reset <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        resetJourney(target);

        sender.sendMessage("§a✓ Journal réinitialisé pour " + target.getName());

        // Mettre à jour la BossBar
        plugin.getJourneyManager().createOrUpdateBossBar(target);
    }

    private void handleSetChapter(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzjourneyadmin setchapter <joueur> <1-12>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        int chapterId;
        try {
            chapterId = Integer.parseInt(args[2]);
            if (chapterId < 1 || chapterId > 12) {
                sender.sendMessage("§cLe chapitre doit être entre 1 et 12!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cNuméro de chapitre invalide!");
            return;
        }

        setChapter(target, chapterId);

        JourneyChapter chapter = JourneyChapter.getById(chapterId);
        sender.sendMessage("§a✓ Chapitre défini à " + chapter.getColoredName() + " §apour " + target.getName());

        // Mettre à jour la BossBar
        plugin.getJourneyManager().createOrUpdateBossBar(target);
    }

    private void handleSetStep(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzjourneyadmin setstep <joueur> <1-10>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        int stepNum;
        try {
            stepNum = Integer.parseInt(args[2]);
            if (stepNum < 1 || stepNum > 10) {
                sender.sendMessage("§cL'étape doit être entre 1 et 10!");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cNuméro d'étape invalide!");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées joueur non trouvées!");
            return;
        }

        data.setCurrentJourneyStep(stepNum);
        plugin.getJourneyManager().getCurrentStepCache().remove(target.getUniqueId());

        sender.sendMessage("§a✓ Étape définie à " + stepNum + " pour " + target.getName());

        // Mettre à jour la BossBar
        plugin.getJourneyManager().createOrUpdateBossBar(target);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Passe le chapitre actuel et va au suivant
     */
    private void skipChapter(Player player, JourneyChapter chapter) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Marquer toutes les étapes du chapitre comme complétées
        for (JourneyStep step : JourneyStep.getStepsForChapter(chapter)) {
            data.addCompletedJourneyStep(step.getId());
        }

        // Marquer le chapitre comme complété
        data.addCompletedJourneyChapter(chapter.getId());

        // Débloquer les gates
        for (JourneyGate gate : chapter.getUnlocks()) {
            data.addJourneyGate(gate.name());
        }

        // Passer au chapitre suivant
        JourneyChapter next = chapter.getNext();
        if (next != null) {
            data.setCurrentJourneyChapter(next.getId());
            data.setCurrentJourneyStep(1);
        }

        // Recharger le cache
        plugin.getJourneyManager().loadPlayerJourney(player);

        // Mettre à jour le WorldBorder
        if (plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().refreshBorder(player);
        }
    }

    /**
     * Débloque tous les chapitres et toutes les gates
     */
    private void unlockAllChapters(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Compléter tous les chapitres
        for (JourneyChapter chapter : JourneyChapter.values()) {
            data.addCompletedJourneyChapter(chapter.getId());

            // Compléter toutes les étapes
            for (JourneyStep step : JourneyStep.getStepsForChapter(chapter)) {
                data.addCompletedJourneyStep(step.getId());
            }

            // Débloquer les gates
            for (JourneyGate gate : chapter.getUnlocks()) {
                data.addJourneyGate(gate.name());
            }
        }

        // Débloquer toutes les gates (au cas où certaines ne sont pas liées à des chapitres)
        for (JourneyGate gate : JourneyGate.values()) {
            data.addJourneyGate(gate.name());
        }

        // Mettre le joueur au dernier chapitre complété
        data.setCurrentJourneyChapter(12);
        data.setCurrentJourneyStep(5);

        // Recharger le cache
        plugin.getJourneyManager().loadPlayerJourney(player);

        // Mettre à jour le WorldBorder
        if (plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().refreshBorder(player);
        }
    }

    /**
     * Réinitialise complètement le parcours
     */
    private void resetJourney(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Reset les données de parcours
        data.resetJourney();

        // Remettre au chapitre 1, étape 1
        data.setCurrentJourneyChapter(1);
        data.setCurrentJourneyStep(1);

        // Recharger le cache
        plugin.getJourneyManager().loadPlayerJourney(player);

        // Mettre à jour le WorldBorder (retour aux limites initiales)
        if (plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().refreshBorder(player);
        }
    }

    /**
     * Définit le chapitre actuel (en complétant les précédents)
     */
    private void setChapter(Player player, int chapterId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Compléter tous les chapitres précédents
        for (int i = 1; i < chapterId; i++) {
            JourneyChapter chapter = JourneyChapter.getById(i);
            data.addCompletedJourneyChapter(chapter.getId());

            for (JourneyStep step : JourneyStep.getStepsForChapter(chapter)) {
                data.addCompletedJourneyStep(step.getId());
            }

            for (JourneyGate gate : chapter.getUnlocks()) {
                data.addJourneyGate(gate.name());
            }
        }

        // Définir le nouveau chapitre
        data.setCurrentJourneyChapter(chapterId);
        data.setCurrentJourneyStep(1);

        // Recharger le cache
        plugin.getJourneyManager().loadPlayerJourney(player);

        // Mettre à jour le WorldBorder
        if (plugin.getZoneBorderManager() != null) {
            plugin.getZoneBorderManager().refreshBorder(player);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Complétion des joueurs
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setchapter")) {
                for (int i = 1; i <= 12; i++) {
                    completions.add(String.valueOf(i));
                }
            } else if (args[0].equalsIgnoreCase("setstep")) {
                for (int i = 1; i <= 10; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        return completions;
    }
}
