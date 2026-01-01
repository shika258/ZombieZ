package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
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
 * Commande d'administration pour gérer l'économie des joueurs (points et gemmes)
 * /zzeco <give|set|take> <joueur> <montant> <points|gems>
 */
public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "set", "take", "info");
    private static final List<String> CURRENCY_TYPES = Arrays.asList("points", "gems");

    public EconomyAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {

        // Permission check
        if (!sender.hasPermission("zombiez.admin.economy")) {
            sender.sendMessage("§cVous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "set" -> handleSet(sender, args);
            case "take" -> handleTake(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Affiche l'aide de la commande
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lZOMBIEZ ECO §7- Gestion Économie");
        sender.sendMessage("");
        sender.sendMessage("§e/zzeco give <joueur> <montant> <points|gems>");
        sender.sendMessage("§7  → Ajouter des points ou gemmes");
        sender.sendMessage("");
        sender.sendMessage("§e/zzeco set <joueur> <montant> <points|gems>");
        sender.sendMessage("§7  → Définir le montant exact");
        sender.sendMessage("");
        sender.sendMessage("§e/zzeco take <joueur> <montant> <points|gems>");
        sender.sendMessage("§7  → Retirer des points ou gemmes");
        sender.sendMessage("");
        sender.sendMessage("§e/zzeco info <joueur>");
        sender.sendMessage("§7  → Voir le solde d'un joueur");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * Ajoute des points ou gemmes à un joueur
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /zzeco give <joueur> <montant> <points|gems>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        String currencyType = args[3].toLowerCase();
        if (!currencyType.equals("points") && !currencyType.equals("gems")) {
            sender.sendMessage("§cType invalide! Utilisez §epoints §cou §dgems");
            return;
        }

        try {
            if (currencyType.equals("points")) {
                long amount = Long.parseLong(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cLe montant doit être positif!");
                    return;
                }
                plugin.getEconomyManager().addPoints(target, amount, "Admin: " + sender.getName());
                sender.sendMessage("§a✓ §6" + EconomyManager.formatPoints(amount) + " Points §aajoutés à §e" + target.getName());
                target.sendMessage("§a✓ Vous avez reçu §6" + EconomyManager.formatPoints(amount) + " Points §ade la part d'un admin!");
            } else {
                int amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cLe montant doit être positif!");
                    return;
                }
                plugin.getEconomyManager().addGems(target, amount, "Admin: " + sender.getName());
                sender.sendMessage("§a✓ §d" + amount + " Gemmes §aajoutées à §e" + target.getName());
                target.sendMessage("§a✓ Vous avez reçu §d" + amount + " Gemmes §ade la part d'un admin!");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide: " + args[2]);
        }
    }

    /**
     * Définit le montant exact de points ou gemmes d'un joueur
     */
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /zzeco set <joueur> <montant> <points|gems>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées non chargées pour ce joueur!");
            return;
        }

        String currencyType = args[3].toLowerCase();
        if (!currencyType.equals("points") && !currencyType.equals("gems")) {
            sender.sendMessage("§cType invalide! Utilisez §epoints §cou §dgems");
            return;
        }

        try {
            if (currencyType.equals("points")) {
                long amount = Long.parseLong(args[2]);
                if (amount < 0) {
                    sender.sendMessage("§cLe montant ne peut pas être négatif!");
                    return;
                }
                long oldAmount = data.getPoints().get();
                data.getPoints().set(amount);
                data.markDirty();
                sender.sendMessage("§a✓ Points de §e" + target.getName() + " §adéfinis à §6" + EconomyManager.formatPoints(amount));
                sender.sendMessage("§7  (Ancien: " + EconomyManager.formatPoints(oldAmount) + ")");
                target.sendMessage("§e⚠ Vos points ont été modifiés à §6" + EconomyManager.formatPoints(amount) + " §epar un admin.");
            } else {
                int amount = Integer.parseInt(args[2]);
                if (amount < 0) {
                    sender.sendMessage("§cLe montant ne peut pas être négatif!");
                    return;
                }
                int oldAmount = data.getGems().get();
                data.getGems().set(amount);
                data.markDirty();
                sender.sendMessage("§a✓ Gemmes de §e" + target.getName() + " §adéfinies à §d" + amount);
                sender.sendMessage("§7  (Ancien: " + oldAmount + ")");
                target.sendMessage("§e⚠ Vos gemmes ont été modifiées à §d" + amount + " §epar un admin.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide: " + args[2]);
        }
    }

    /**
     * Retire des points ou gemmes à un joueur
     */
    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /zzeco take <joueur> <montant> <points|gems>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées non chargées pour ce joueur!");
            return;
        }

        String currencyType = args[3].toLowerCase();
        if (!currencyType.equals("points") && !currencyType.equals("gems")) {
            sender.sendMessage("§cType invalide! Utilisez §epoints §cou §dgems");
            return;
        }

        try {
            if (currencyType.equals("points")) {
                long amount = Long.parseLong(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cLe montant doit être positif!");
                    return;
                }
                long currentPoints = data.getPoints().get();
                if (currentPoints < amount) {
                    sender.sendMessage("§cLe joueur n'a que §6" + EconomyManager.formatPoints(currentPoints) + " Points§c!");
                    sender.sendMessage("§7Utilisez §e/zzeco set §7pour définir à 0 ou retirer un montant plus petit.");
                    return;
                }
                data.removePoints(amount);
                sender.sendMessage("§a✓ §6" + EconomyManager.formatPoints(amount) + " Points §aretirés à §e" + target.getName());
                sender.sendMessage("§7  Nouveau solde: " + EconomyManager.formatPoints(data.getPoints().get()));
                target.sendMessage("§c⚠ §6" + EconomyManager.formatPoints(amount) + " Points §cont été retirés de votre compte.");
            } else {
                int amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cLe montant doit être positif!");
                    return;
                }
                int currentGems = data.getGems().get();
                if (currentGems < amount) {
                    sender.sendMessage("§cLe joueur n'a que §d" + currentGems + " Gemmes§c!");
                    sender.sendMessage("§7Utilisez §e/zzeco set §7pour définir à 0 ou retirer un montant plus petit.");
                    return;
                }
                data.removeGems(amount);
                sender.sendMessage("§a✓ §d" + amount + " Gemmes §aretirées à §e" + target.getName());
                sender.sendMessage("§7  Nouveau solde: " + data.getGems().get());
                target.sendMessage("§c⚠ §d" + amount + " Gemmes §cont été retirées de votre compte.");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide: " + args[2]);
        }
    }

    /**
     * Affiche les informations économiques d'un joueur
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /zzeco info <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJoueur non trouvé: " + args[1]);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(target);
        if (data == null) {
            sender.sendMessage("§cDonnées non chargées pour ce joueur!");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6§lÉCONOMIE §7- " + target.getName());
        sender.sendMessage("");
        sender.sendMessage("§6⬤ Points: §f" + EconomyManager.formatPoints(data.getPoints().get()));
        sender.sendMessage("§d⬤ Gemmes: §f" + data.getGems().get());
        sender.sendMessage("§e⬤ Banque: §f" + EconomyManager.formatPoints(data.getBankPoints()));
        sender.sendMessage("");
        sender.sendMessage("§7Total Points gagnés: §f" + EconomyManager.formatPoints(data.getTotalPointsEarned().get()));
        sender.sendMessage("§8§m                                        ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin.economy")) {
            return List.of();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Sous-commandes
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Noms de joueurs
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            // Montant - proposer quelques valeurs
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("give") || subCmd.equals("set") || subCmd.equals("take")) {
                completions.addAll(Arrays.asList("100", "1000", "10000", "100000"));
            }
        } else if (args.length == 4) {
            // Type de monnaie
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("give") || subCmd.equals("set") || subCmd.equals("take")) {
                String partial = args[3].toLowerCase();
                for (String type : CURRENCY_TYPES) {
                    if (type.startsWith(partial)) {
                        completions.add(type);
                    }
                }
            }
        }

        return completions;
    }
}
