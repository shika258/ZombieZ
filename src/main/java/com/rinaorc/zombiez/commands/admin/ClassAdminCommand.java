package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassManager;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.ClassTalent;
import com.rinaorc.zombiez.classes.talents.ClassTalentTree;
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
 * Commandes d'administration pour le système de classes
 * /zzclassadmin <subcommand> [args...]
 */
public class ClassAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "info", "setclass", "setlevel", "givexp", "givepoints", "unlockall", "reset", "resettalents"
    );

    private static final int MAX_CLASS_LEVEL = 50;

    public ClassAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("zombiez.admin.class")) {
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
            case "setclass" -> handleSetClass(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "givexp" -> handleGiveXp(sender, args);
            case "givepoints" -> handleGivePoints(sender, args);
            case "unlockall" -> handleUnlockAll(sender, args);
            case "reset" -> handleReset(sender, args);
            case "resettalents" -> handleResetTalents(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Affiche l'aide
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lZOMBIEZ §7- Commandes Admin Classes");
        sender.sendMessage("");
        sender.sendMessage("§e/zzclassadmin info <joueur> §7- Infos classe d'un joueur");
        sender.sendMessage("§e/zzclassadmin setclass <joueur> <classe> §7- Définir la classe");
        sender.sendMessage("§e/zzclassadmin setlevel <joueur> <niveau> §7- Définir le niveau de classe");
        sender.sendMessage("§e/zzclassadmin givexp <joueur> <montant> §7- Donner XP de classe");
        sender.sendMessage("§e/zzclassadmin givepoints <joueur> <montant> §7- Donner points de talent");
        sender.sendMessage("§e/zzclassadmin unlockall <joueur> §7- Débloquer tout (max level, talents, armes)");
        sender.sendMessage("§e/zzclassadmin reset <joueur> §7- Reset complet de la classe");
        sender.sendMessage("§e/zzclassadmin resettalents <joueur> §7- Reset les talents uniquement");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Affiche les informations de classe d'un joueur
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin info <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lCLASSE §7- " + target.getName());
        sender.sendMessage("");

        if (!data.hasClass()) {
            sender.sendMessage("§7Classe: §cAucune classe sélectionnée");
        } else {
            sender.sendMessage("§7Classe: " + data.getSelectedClass().getColoredName());
            sender.sendMessage("§7Niveau: §e" + data.getClassLevel().get() + " §7/ " + MAX_CLASS_LEVEL);
            sender.sendMessage("§7XP: §b" + data.getClassXp().get() + " §7/ " + data.getRequiredXpForNextClassLevel());
            sender.sendMessage("§7Progression: §a" + String.format("%.1f", data.getClassLevelProgress()) + "%");
            sender.sendMessage("");
            sender.sendMessage("§7Points de talent disponibles: §e" + data.getAvailableTalentPoints());
            sender.sendMessage("§7Points dépensés: §c" + data.getSpentTalentPoints().get());
            sender.sendMessage("§7Talents débloqués: §a" + data.getUnlockedTalents().size());
            sender.sendMessage("");
            sender.sendMessage("§7Énergie: §9" + data.getEnergy().get() + " §7/ " + data.getMaxEnergy().get());
            sender.sendMessage("§7Kills de classe: §c" + data.getClassKills().get());
            sender.sendMessage("§7Deaths de classe: §4" + data.getClassDeaths().get());
            sender.sendMessage("§7K/D classe: §e" + String.format("%.2f", data.getClassKDRatio()));
        }

        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Définit la classe d'un joueur (bypass le cooldown)
     */
    private void handleSetClass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin setclass <joueur> <classe>");
            sender.sendMessage("§7Classes disponibles: §eguerrier, chasseur, occultiste");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassType classType = ClassType.fromId(args[2]);
        if (classType == null) {
            sender.sendMessage("§cClasse invalide! Utilisez: guerrier, chasseur, occultiste");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);

        // Bypass le cooldown en forçant le changement
        data.changeClass(classType);
        data.markDirty();

        sender.sendMessage("§a✓ Classe de " + target.getName() + " définie à " + classType.getColoredName());
        target.sendMessage("§6[Admin] §7Votre classe a été changée en " + classType.getColoredName());
    }

    /**
     * Définit le niveau de classe d'un joueur
     */
    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin setlevel <joueur> <niveau>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe sélectionnée!");
            return;
        }

        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > MAX_CLASS_LEVEL) {
                sender.sendMessage("§cNiveau invalide! (1-" + MAX_CLASS_LEVEL + ")");
                return;
            }

            int oldLevel = data.getClassLevel().get();
            data.getClassLevel().set(level);
            data.getClassXp().set(0);

            // Ajuster les points de talent en fonction du niveau
            int newTalentPoints = level - 1;
            data.getTalentPoints().set(newTalentPoints);

            data.markDirty();

            sender.sendMessage("§a✓ Niveau de classe de " + target.getName() + " défini à §e" + level);
            sender.sendMessage("§7Points de talent disponibles: §e" + data.getAvailableTalentPoints());
            target.sendMessage("§6[Admin] §7Votre niveau de classe a été changé de §c" + oldLevel + " §7à §a" + level);

        } catch (NumberFormatException e) {
            sender.sendMessage("§cNiveau invalide!");
        }
    }

    /**
     * Donne de l'XP de classe à un joueur
     */
    private void handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin givexp <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe sélectionnée!");
            return;
        }

        try {
            long amount = Long.parseLong(args[2]);
            if (amount <= 0) {
                sender.sendMessage("§cMontant invalide!");
                return;
            }

            int oldLevel = data.getClassLevel().get();
            int levelsGained = 0;

            // Ajouter l'XP et compter les level ups
            while (amount > 0) {
                long required = data.getRequiredXpForNextClassLevel();
                long currentXp = data.getClassXp().get();

                if (currentXp + amount >= required && data.getClassLevel().get() < MAX_CLASS_LEVEL) {
                    amount -= (required - currentXp);
                    data.getClassXp().set(0);
                    data.getClassLevel().incrementAndGet();
                    data.getTalentPoints().incrementAndGet();
                    levelsGained++;
                } else {
                    data.getClassXp().addAndGet(amount);
                    break;
                }
            }

            data.markDirty();

            sender.sendMessage("§a✓ §b" + args[2] + " §aXP de classe donnés à " + target.getName());
            if (levelsGained > 0) {
                sender.sendMessage("§6✦ " + levelsGained + " niveau(x) gagné(s)! (Niveau " + data.getClassLevel().get() + ")");
            }

            target.sendMessage("§6[Admin] §7Vous avez reçu §b" + args[2] + " §7XP de classe!");
            if (levelsGained > 0) {
                target.sendMessage("§6§l✦ NIVEAU " + data.getClassLevel().get() + " ✦");
                target.sendMessage("§a+" + levelsGained + " Point(s) de Talent");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
    }

    /**
     * Donne des points de talent à un joueur
     */
    private void handleGivePoints(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin givepoints <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe sélectionnée!");
            return;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage("§cMontant invalide!");
                return;
            }

            data.getTalentPoints().addAndGet(amount);
            data.markDirty();

            sender.sendMessage("§a✓ §e" + amount + " §apoints de talent donnés à " + target.getName());
            sender.sendMessage("§7Points disponibles: §e" + data.getAvailableTalentPoints());
            target.sendMessage("§6[Admin] §7Vous avez reçu §e" + amount + " §7points de talent!");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
    }

    /**
     * Débloque tout sur la classe d'un joueur (niveau max, tous les talents, armes)
     */
    private void handleUnlockAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin unlockall <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe sélectionnée!");
            return;
        }

        ClassType classType = data.getSelectedClass();
        ClassManager classManager = plugin.getClassManager();
        ClassTalentTree talentTree = classManager.getTalentTree();

        // 1. Mettre au niveau max
        data.getClassLevel().set(MAX_CLASS_LEVEL);
        data.getClassXp().set(0);

        // 2. Donner tous les points de talent du niveau max
        int maxTalentPoints = MAX_CLASS_LEVEL - 1;
        data.getTalentPoints().set(maxTalentPoints);

        // 3. Débloquer tous les talents de la classe au niveau max
        data.getUnlockedTalents().clear();
        data.getSpentTalentPoints().set(0);

        int totalSpent = 0;
        List<ClassTalent> talents = talentTree.getTalentsForClass(classType);

        for (ClassTalent talent : talents) {
            int maxLevel = talent.getMaxLevel();
            data.getUnlockedTalents().put(talent.getId(), maxLevel);
            totalSpent += maxLevel * talent.getPointCost();
        }

        data.getSpentTalentPoints().set(totalSpent);

        // 4. Donner suffisamment de points pour couvrir tous les talents
        if (totalSpent > maxTalentPoints) {
            data.getTalentPoints().set(totalSpent);
        }

        // 5. Max énergie
        data.getMaxEnergy().set(200);
        data.getEnergy().set(200);

        data.invalidateStatsCache();
        data.invalidateArchetypeCache();
        data.markDirty();

        sender.sendMessage("§a✓ Tout débloqué pour " + target.getName() + " (" + classType.getColoredName() + "§a)");
        sender.sendMessage("§7- Niveau: §e" + MAX_CLASS_LEVEL);
        sender.sendMessage("§7- Talents débloqués: §e" + talents.size() + " §7(tous au max)");
        sender.sendMessage("§7- Toutes les armes débloquées");

        target.sendMessage("");
        target.sendMessage("§6§l✦ DÉBLOCAGE COMPLET ✦");
        target.sendMessage("§7Votre classe " + classType.getColoredName() + " §7est maintenant au maximum!");
        target.sendMessage("§7- Niveau §e" + MAX_CLASS_LEVEL);
        target.sendMessage("§7- §a" + talents.size() + " §7talents débloqués");
        target.sendMessage("§7- Toutes les armes disponibles!");
        target.sendMessage("");
    }

    /**
     * Reset complet de la classe d'un joueur
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin reset <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        ClassType oldClass = data.getSelectedClass();

        // Reset tout
        data.getClassLevel().set(1);
        data.getClassXp().set(0);
        data.getTalentPoints().set(0);
        data.getSpentTalentPoints().set(0);
        data.getUnlockedTalents().clear();
        data.getEquippedSkills().clear();
        data.getArcadeBuffs().clear();
        data.getEnergy().set(100);
        data.getMaxEnergy().set(100);
        data.getClassKills().set(0);
        data.getClassDeaths().set(0);
        data.getDamageDealt().set(0);
        data.getDamageReceived().set(0);
        data.getSkillsUsed().set(0);

        data.invalidateStatsCache();
        data.invalidateArchetypeCache();
        data.markDirty();

        String className = oldClass != null ? oldClass.getColoredName() : "§cAucune";
        sender.sendMessage("§a✓ Classe de " + target.getName() + " réinitialisée (" + className + "§a)");
        target.sendMessage("§6[Admin] §7Votre classe a été réinitialisée au niveau 1.");
    }

    /**
     * Reset uniquement les talents d'un joueur
     */
    private void handleResetTalents(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin resettalents <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe sélectionnée!");
            return;
        }

        int talentsReset = data.getUnlockedTalents().size();
        int pointsRecovered = data.getSpentTalentPoints().get();

        data.resetTalents();
        data.invalidateStatsCache();
        data.invalidateArchetypeCache();

        sender.sendMessage("§a✓ Talents de " + target.getName() + " réinitialisés!");
        sender.sendMessage("§7- " + talentsReset + " talents supprimés");
        sender.sendMessage("§7- " + pointsRecovered + " points récupérés");
        sender.sendMessage("§7- Points disponibles: §e" + data.getAvailableTalentPoints());

        target.sendMessage("§6[Admin] §7Vos talents ont été réinitialisés!");
        target.sendMessage("§a✓ " + data.getAvailableTalentPoints() + " points de talent disponibles");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin.class")) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Complétion des noms de joueurs pour toutes les sous-commandes
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            String partial = args[2].toLowerCase();

            if (subCmd.equals("setclass")) {
                // Complétion des classes
                for (ClassType type : ClassType.values()) {
                    String id = type.getId();
                    if (id.startsWith(partial)) {
                        completions.add(id);
                    }
                }
            } else if (subCmd.equals("setlevel")) {
                // Suggestions de niveaux
                completions.addAll(Arrays.asList("1", "10", "25", "50"));
            } else if (subCmd.equals("givexp")) {
                // Suggestions de montants d'XP
                completions.addAll(Arrays.asList("1000", "5000", "10000", "50000", "100000"));
            } else if (subCmd.equals("givepoints")) {
                // Suggestions de points
                completions.addAll(Arrays.asList("1", "5", "10", "20", "50"));
            }
        }

        return completions;
    }
}
