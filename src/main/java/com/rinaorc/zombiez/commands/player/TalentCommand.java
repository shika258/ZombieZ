package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.classes.talents.TalentTier;
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
 * Commande /talent pour gerer les talents
 * Usage:
 * - /talent - Ouvre le menu de selection des talents
 * - /talent info - Affiche les talents actifs
 * - /talent branch - Ouvre la selection de branche
 * - /talent help - Affiche l'aide
 */
public class TalentCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final ClassManager classManager;
    private final TalentManager talentManager;

    public TalentCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.classManager = plugin.getClassManager();
        this.talentManager = plugin.getTalentManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est reservee aux joueurs!");
            return true;
        }

        ClassData data = classManager.getClassData(player);

        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            player.sendMessage("§7Utilisez §e/class select §7pour en choisir une.");
            return true;
        }

        if (args.length == 0) {
            openTalentMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info", "list", "actifs" -> {
                showActiveTalents(player, data);
            }

            case "branch", "branche", "specialisation" -> {
                plugin.getBranchSelectionGUI().open(player);
            }

            case "help", "aide", "?" -> {
                showHelp(player);
            }

            default -> {
                player.sendMessage("§cCommande inconnue! Utilisez /talent help");
            }
        }

        return true;
    }

    private void openTalentMenu(Player player) {
        ClassData data = classManager.getClassData(player);

        if (!data.hasBranch()) {
            plugin.getBranchSelectionGUI().open(player);
        } else {
            plugin.getTalentSelectionGUI().open(player);
        }
    }

    private void showActiveTalents(Player player, ClassData data) {
        player.sendMessage("");
        player.sendMessage("§6§l+ VOS TALENTS ACTIFS +");
        player.sendMessage("");

        if (!data.hasBranch()) {
            player.sendMessage("§7Aucune branche selectionnee.");
            player.sendMessage("§7Utilisez §e/talent branch §7pour choisir une specialisation.");
            return;
        }

        player.sendMessage("§7Branche: §e" + data.getSelectedBranch().getDisplayName());
        player.sendMessage("§7Niveau: §e" + data.getClassLevel());
        player.sendMessage("");

        List<Talent> activeTalents = talentManager.getActiveTalents(player);

        if (activeTalents.isEmpty()) {
            player.sendMessage("§7Aucun talent selectionne.");
            player.sendMessage("§7Utilisez §e/talent §7pour choisir vos talents.");
        } else {
            for (Talent talent : activeTalents) {
                TalentTier tier = talent.getTier();
                String status = tier.isUnlocked(data.getClassLevel().get()) ? "§a✓" : "§c✗";
                player.sendMessage(status + " " + tier.getColor() + "[" + tier.getDisplayName() + "] §f" + talent.getName());
                player.sendMessage("   §7" + talent.getDescription());
            }
        }

        player.sendMessage("");
        player.sendMessage("§7Talents debloques: §e" + getUnlockedTiersCount(data) + "/9");
        player.sendMessage("");
    }

    private int getUnlockedTiersCount(ClassData data) {
        int count = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (tier.isUnlocked(data.getClassLevel().get())) {
                count++;
            }
        }
        return count;
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l+ COMMANDES DE TALENTS +");
        player.sendMessage("");
        player.sendMessage("§e/talent §7- Ouvre le menu de selection");
        player.sendMessage("§e/talent info §7- Voir vos talents actifs");
        player.sendMessage("§e/talent branch §7- Changer de specialisation");
        player.sendMessage("§e/talent help §7- Affiche cette aide");
        player.sendMessage("");
        player.sendMessage("§7§oLes talents se debloquent aux niveaux:");
        player.sendMessage("§70, 5, 10, 15, 20, §925§7, 30, 40, 50");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "info", "branch", "help"
            ));
        }

        String prefix = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix))
            .collect(Collectors.toList());
    }
}
