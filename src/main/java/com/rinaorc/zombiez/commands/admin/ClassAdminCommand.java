package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentTier;
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
 * Commandes d'administration pour le systeme de classes
 * /zzclassadmin <subcommand> [args...]
 */
public class ClassAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "info", "setclass", "setlevel", "maxlevel", "givexp", "reset",
        "settalent", "resettalents", "unlockalltalents", "listtalents"
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
            case "maxlevel" -> handleMaxLevel(sender, args);
            case "givexp" -> handleGiveXp(sender, args);
            case "reset" -> handleReset(sender, args);
            case "settalent" -> handleSetTalent(sender, args);
            case "resettalents" -> handleResetTalents(sender, args);
            case "unlockalltalents" -> handleUnlockAllTalents(sender, args);
            case "listtalents" -> handleListTalents(sender, args);
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
        sender.sendMessage("§e/zzclassadmin setclass <joueur> <classe> §7- Definir la classe");
        sender.sendMessage("§e/zzclassadmin setlevel <joueur> <niveau> §7- Definir le niveau");
        sender.sendMessage("§e/zzclassadmin maxlevel <joueur> §7- Mettre au niveau max (50)");
        sender.sendMessage("§e/zzclassadmin givexp <joueur> <montant> §7- Donner XP de classe");
        sender.sendMessage("§e/zzclassadmin reset <joueur> §7- Reset complet de la classe");
        sender.sendMessage("");
        sender.sendMessage("§d§lTalents:");
        sender.sendMessage("§e/zzclassadmin settalent <joueur> <talentId> §7- Definir un talent");
        sender.sendMessage("§e/zzclassadmin resettalents <joueur> §7- Reset tous les talents");
        sender.sendMessage("§e/zzclassadmin unlockalltalents <joueur> §7- Debloquer tous les tiers");
        sender.sendMessage("§e/zzclassadmin listtalents <classe> §7- Lister les talents d'une classe");
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
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lCLASSE §7- " + target.getName());
        sender.sendMessage("");

        if (!data.hasClass()) {
            sender.sendMessage("§7Classe: §cAucune classe selectionnee");
        } else {
            sender.sendMessage("§7Classe: " + data.getSelectedClass().getColoredName());
            sender.sendMessage("§7Niveau: §e" + data.getClassLevel().get() + " §7/ " + MAX_CLASS_LEVEL);
            sender.sendMessage("§7XP: §b" + data.getClassXp().get() + " §7/ " + data.getRequiredXpForNextClassLevel());
            sender.sendMessage("§7Progression: §a" + String.format("%.1f", data.getClassLevelProgress()) + "%");
            sender.sendMessage("");
            sender.sendMessage("§7Kills de classe: §c" + data.getClassKills().get());
            sender.sendMessage("§7Deaths de classe: §4" + data.getClassDeaths().get());
            sender.sendMessage("§7K/D classe: §e" + String.format("%.2f", data.getClassKDRatio()));
        }

        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Definit la classe d'un joueur (bypass le cooldown)
     */
    private void handleSetClass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin setclass <joueur> <classe>");
            sender.sendMessage("§7Classes disponibles: §eguerrier, chasseur, occultiste");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassType classType = ClassType.fromId(args[2]);
        if (classType == null) {
            sender.sendMessage("§cClasse invalide! Utilisez: guerrier, chasseur, occultiste");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);

        // Bypass le cooldown en forcant le changement
        data.changeClass(classType);
        data.markDirty();

        sender.sendMessage("§a+ Classe de " + target.getName() + " definie a " + classType.getColoredName());
        target.sendMessage("§6[Admin] §7Votre classe a ete changee en " + classType.getColoredName());
    }

    /**
     * Definit le niveau de classe d'un joueur
     */
    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin setlevel <joueur> <niveau>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
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
            data.markDirty();

            sender.sendMessage("§a+ Niveau de classe de " + target.getName() + " defini a §e" + level);
            target.sendMessage("§6[Admin] §7Votre niveau de classe a ete change de §c" + oldLevel + " §7a §a" + level);

        } catch (NumberFormatException e) {
            sender.sendMessage("§cNiveau invalide!");
        }
    }

    /**
     * Donne de l'XP de classe a un joueur
     */
    private void handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin givexp <joueur> <montant>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
            return;
        }

        try {
            long amount = Long.parseLong(args[2]);
            if (amount <= 0) {
                sender.sendMessage("§cMontant invalide!");
                return;
            }

            int levelsGained = 0;

            // Ajouter l'XP et compter les level ups
            while (amount > 0) {
                long required = data.getRequiredXpForNextClassLevel();
                long currentXp = data.getClassXp().get();

                if (currentXp + amount >= required && data.getClassLevel().get() < MAX_CLASS_LEVEL) {
                    amount -= (required - currentXp);
                    data.getClassXp().set(0);
                    data.getClassLevel().incrementAndGet();
                    levelsGained++;
                } else {
                    data.getClassXp().addAndGet(amount);
                    break;
                }
            }

            data.markDirty();

            sender.sendMessage("§a+ §b" + args[2] + " §aXP de classe donnes a " + target.getName());
            if (levelsGained > 0) {
                sender.sendMessage("§6+ " + levelsGained + " niveau(x) gagne(s)! (Niveau " + data.getClassLevel().get() + ")");
            }

            target.sendMessage("§6[Admin] §7Vous avez recu §b" + args[2] + " §7XP de classe!");
            if (levelsGained > 0) {
                target.sendMessage("§6§l+ NIVEAU " + data.getClassLevel().get() + " +");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide!");
        }
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
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        ClassType oldClass = data.getSelectedClass();

        // Reset tout
        data.getClassLevel().set(1);
        data.getClassXp().set(0);
        data.getClassKills().set(0);
        data.getClassDeaths().set(0);
        data.getDamageDealt().set(0);
        data.getDamageReceived().set(0);
        data.markDirty();

        String className = oldClass != null ? oldClass.getColoredName() : "§cAucune";
        sender.sendMessage("§a+ Classe de " + target.getName() + " reinitialisee (" + className + "§a)");
        target.sendMessage("§6[Admin] §7Votre classe a ete reinitialisee au niveau 1.");
    }

    /**
     * Met un joueur au niveau max de classe (50)
     */
    private void handleMaxLevel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin maxlevel <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
            return;
        }

        int oldLevel = data.getClassLevel().get();
        data.getClassLevel().set(MAX_CLASS_LEVEL);
        data.getClassXp().set(0);
        data.markDirty();

        sender.sendMessage("§a+ " + target.getName() + " est maintenant niveau §e" + MAX_CLASS_LEVEL + "§a!");
        target.sendMessage("§6[Admin] §7Votre niveau de classe a ete change de §c" + oldLevel + " §7a §a§l" + MAX_CLASS_LEVEL + " §7(MAX)!");
        target.sendMessage("§6§l+ NIVEAU MAXIMUM ATTEINT! +");

        // Notifier des talents disponibles
        plugin.getTalentManager().notifyUnselectedTalents(target);
    }

    /**
     * Definit un talent pour un joueur
     */
    private void handleSetTalent(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /zzclassadmin settalent <joueur> <talentId>");
            sender.sendMessage("§7Utilisez /zzclassadmin listtalents <classe> pour voir les IDs");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
            return;
        }

        String talentId = args[2].toLowerCase();
        Talent talent = plugin.getTalentManager().getTalent(talentId);

        if (talent == null) {
            sender.sendMessage("§cTalent non trouve: " + talentId);
            sender.sendMessage("§7Utilisez /zzclassadmin listtalents " + data.getSelectedClass().getId());
            return;
        }

        // Verifier que le talent est de la bonne classe
        if (talent.getClassType() != data.getSelectedClass()) {
            sender.sendMessage("§cCe talent n'est pas pour la classe " + data.getSelectedClass().getColoredName());
            return;
        }

        // Forcer la selection (bypass cooldown et tier lock)
        data.selectTalent(talent.getTier(), talent.getId());
        data.markDirty();

        sender.sendMessage("§a+ Talent §e" + talent.getName() + "§a defini pour " + target.getName());
        target.sendMessage("§6[Admin] §7Talent §e" + talent.getColoredName() + "§7 active!");
    }

    /**
     * Reset tous les talents d'un joueur
     */
    private void handleResetTalents(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin resettalents <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
            return;
        }

        // Reset tous les talents
        for (TalentTier tier : TalentTier.values()) {
            data.selectTalent(tier, null);
        }
        data.markDirty();

        sender.sendMessage("§a+ Talents de " + target.getName() + " reinitialises!");
        target.sendMessage("§6[Admin] §7Tous vos talents ont ete reinitialises.");
        plugin.getTalentManager().notifyUnselectedTalents(target);
    }

    /**
     * Debloque tous les tiers de talents pour un joueur
     */
    private void handleUnlockAllTalents(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin unlockalltalents <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouve!");
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(target);
        if (!data.hasClass()) {
            sender.sendMessage("§cCe joueur n'a pas de classe selectionnee!");
            return;
        }

        // Mettre au niveau max pour debloquer tous les tiers
        data.getClassLevel().set(MAX_CLASS_LEVEL);
        data.markDirty();

        int tiersUnlocked = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (data.isTalentTierUnlocked(tier)) {
                tiersUnlocked++;
            }
        }

        sender.sendMessage("§a+ Tous les " + tiersUnlocked + " tiers de talents debloques pour " + target.getName() + "!");
        target.sendMessage("§6[Admin] §7Tous vos tiers de talents ont ete debloques!");
        plugin.getTalentManager().notifyUnselectedTalents(target);
    }

    /**
     * Liste les talents d'une classe
     */
    private void handleListTalents(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzclassadmin listtalents <classe>");
            sender.sendMessage("§7Classes disponibles: guerrier, chasseur, occultiste");
            return;
        }

        ClassType classType = ClassType.fromId(args[1]);
        if (classType == null) {
            sender.sendMessage("§cClasse invalide! Utilisez: guerrier, chasseur, occultiste");
            return;
        }

        List<Talent> talents = plugin.getTalentManager().getTalentsForClass(classType);

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lTALENTS §7- " + classType.getColoredName());
        sender.sendMessage("");

        TalentTier currentTier = null;
        for (Talent talent : talents) {
            if (talent.getTier() != currentTier) {
                currentTier = talent.getTier();
                sender.sendMessage("");
                sender.sendMessage("§e§l" + currentTier.getName() + " §7(Niveau " + currentTier.getRequiredLevel() + "+)");
            }
            sender.sendMessage("  §7- §f" + talent.getId() + " §7: " + talent.getColoredName());
        }

        sender.sendMessage("§8§m                                        ");
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
            String subCmd = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            if (subCmd.equals("listtalents")) {
                // listtalents prend une classe, pas un joueur
                for (ClassType type : ClassType.values()) {
                    String id = type.getId();
                    if (id.startsWith(partial)) {
                        completions.add(id);
                    }
                }
            } else {
                // Completion des noms de joueurs pour les autres sous-commandes
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            String partial = args[2].toLowerCase();

            if (subCmd.equals("setclass")) {
                // Completion des classes
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
            } else if (subCmd.equals("settalent")) {
                // Completion des talents du joueur cible
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    ClassData data = plugin.getClassManager().getClassData(target);
                    if (data.hasClass()) {
                        List<Talent> talents = plugin.getTalentManager().getTalentsForClass(data.getSelectedClass());
                        for (Talent talent : talents) {
                            if (talent.getId().startsWith(partial)) {
                                completions.add(talent.getId());
                            }
                        }
                    }
                }
            }
        }

        return completions;
    }
}
