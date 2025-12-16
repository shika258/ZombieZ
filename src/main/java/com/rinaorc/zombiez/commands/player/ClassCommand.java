package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.gui.ClassSelectionGUI;
import com.rinaorc.zombiez.classes.gui.SkillsGUI;
import com.rinaorc.zombiez.classes.gui.TalentTreeGUI;
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
 * Commande /class pour gérer les classes
 * Usage:
 * - /class - Ouvre le menu principal
 * - /class select - Ouvre la sélection de classe
 * - /class talents - Ouvre l'arbre de talents
 * - /class skills - Ouvre la gestion des compétences
 * - /class info - Affiche les infos de classe
 * - /class buff <1-3> - Sélectionne un buff au level up
 * - /class use <1-3> - Utilise une compétence
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
            sender.sendMessage("§cCette commande est réservée aux joueurs!");
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "select", "choose", "choisir" -> {
                new ClassSelectionGUI(plugin, classManager).open(player);
            }

            case "talents", "talent", "tree", "arbre" -> {
                new TalentTreeGUI(plugin, classManager).open(player);
            }

            case "skills", "skill", "competences", "compétences" -> {
                new SkillsGUI(plugin, classManager).open(player);
            }

            case "info", "stats" -> {
                showClassInfo(player);
            }

            case "buff", "buffs" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /class buff <1-3>");
                    return true;
                }
                try {
                    int choice = Integer.parseInt(args[1]);
                    classManager.selectLevelUpBuff(player, choice);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUsage: /class buff <1-3>");
                }
            }

            case "use", "skill" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /class use <1|2|3|primary|secondary|ultimate>");
                    return true;
                }
                String slot = parseSkillSlot(args[1]);
                if (slot == null) {
                    player.sendMessage("§cSlot invalide! Utilisez 1, 2, 3, primary, secondary ou ultimate");
                    return true;
                }
                classManager.useSkill(player, slot);
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
            new ClassSelectionGUI(plugin, classManager).open(player);
        } else {
            new TalentTreeGUI(plugin, classManager).open(player);
        }
    }

    private void showClassInfo(Player player) {
        ClassData data = classManager.getClassData(player);

        player.sendMessage("");
        player.sendMessage("§6§l✦ INFORMATIONS DE CLASSE ✦");
        player.sendMessage("");

        if (!data.hasClass()) {
            player.sendMessage("§cVous n'avez pas encore de classe!");
            player.sendMessage("§7Utilisez /class select pour en choisir une.");
            return;
        }

        ClassType classType = data.getSelectedClass();

        player.sendMessage("§7Classe: " + classType.getColoredName());
        player.sendMessage("§7Niveau de classe: §f" + data.getClassLevel().get());
        player.sendMessage("§7XP: §f" + data.getClassXp().get() + "/" + data.getRequiredXpForNextClassLevel());
        player.sendMessage("");

        player.sendMessage("§7Points de talent: §e" + data.getAvailableTalentPoints());
        player.sendMessage("§7Talents débloqués: §f" + data.getUnlockedTalents().size());
        player.sendMessage("§7Buffs collectés: §f" + data.getTotalBuffCount());
        player.sendMessage("");

        player.sendMessage("§7Énergie: §b" + data.getEnergy().get() + "/" + data.getMaxEnergy().get());
        player.sendMessage("");

        // Stats
        player.sendMessage("§eStatistiques de classe:");
        player.sendMessage("§7• Kills: §f" + data.getClassKills().get());
        player.sendMessage("§7• Morts: §f" + data.getClassDeaths().get());
        player.sendMessage("§7• K/D: §f" + String.format("%.2f", data.getClassKDRatio()));
        player.sendMessage("§7• Compétences utilisées: §f" + data.getSkillsUsed().get());
        player.sendMessage("");

        // Multiplicateurs actuels
        player.sendMessage("§6Multiplicateurs actuels:");
        player.sendMessage("§c⚔ §7Dégâts: §f" + String.format("%.0f%%", classManager.getTotalDamageMultiplier(player) * 100));
        player.sendMessage("§a❤ §7HP: §f" + String.format("%.0f%%", classManager.getTotalHealthMultiplier(player) * 100));
        player.sendMessage("§e✦ §7Critique: §f" + String.format("%.1f%%", classManager.getTotalCritChance(player)));
        player.sendMessage("");
    }

    private void showMutations(Player player) {
        List<String> summary = classManager.getMutationManager().getMutationSummary();
        for (String line : summary) {
            player.sendMessage(line);
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l✦ COMMANDES DE CLASSE ✦");
        player.sendMessage("");
        player.sendMessage("§e/class §7- Ouvre le menu principal");
        player.sendMessage("§e/class select §7- Choisir une classe");
        player.sendMessage("§e/class talents §7- Arbre de talents");
        player.sendMessage("§e/class skills §7- Gérer les compétences");
        player.sendMessage("§e/class info §7- Informations de classe");
        player.sendMessage("§e/class buff <1-3> §7- Choisir un buff (level up)");
        player.sendMessage("§e/class use <1-3> §7- Utiliser une compétence");
        player.sendMessage("§e/class mutations §7- Voir les mutations du jour");
        player.sendMessage("");
        player.sendMessage("§8Raccourcis de compétences:");
        player.sendMessage("§8• 1/primary = Compétence primaire");
        player.sendMessage("§8• 2/secondary = Compétence secondaire");
        player.sendMessage("§8• 3/ultimate = Compétence ultime");
        player.sendMessage("");
    }

    private String parseSkillSlot(String input) {
        return switch (input.toLowerCase()) {
            case "1", "primary", "primaire" -> "PRIMARY";
            case "2", "secondary", "secondaire" -> "SECONDARY";
            case "3", "ultimate", "ultime" -> "ULTIMATE";
            default -> null;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "select", "talents", "skills", "info", "buff", "use", "mutations", "help"
            ));
            // Ajouter les noms de classe
            for (ClassType type : ClassType.values()) {
                completions.add(type.name().toLowerCase());
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("buff")) {
                completions.addAll(Arrays.asList("1", "2", "3"));
            } else if (args[0].equalsIgnoreCase("use")) {
                completions.addAll(Arrays.asList("1", "2", "3", "primary", "secondary", "ultimate"));
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
    }
}
