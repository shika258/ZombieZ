package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.economy.banking.BankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /bank pour la banque personnelle
 */
public class BankCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public BankCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande nécessite d'être un joueur.");
            return true;
        }

        BankManager bankManager = plugin.getBankManager();
        if (bankManager == null) {
            player.sendMessage("§cLa banque n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            // Ouvrir le menu principal
            bankManager.openBankMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "vault", "coffre" -> bankManager.openVault(player);
            case "deposit", "deposer" -> handleDeposit(player, args);
            case "withdraw", "retirer" -> handleWithdraw(player, args);
            case "balance", "solde" -> showBalance(player);
            case "help", "aide" -> sendHelp(player);
            default -> bankManager.openBankMenu(player);
        }

        return true;
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bank deposit <montant|all>");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return;

        int amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = (int) Math.min(Integer.MAX_VALUE, data.getPoints().get());
        } else {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cMontant invalide.");
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage("§cMontant invalide.");
            return;
        }

        if (plugin.getBankManager().depositPoints(player, amount)) {
            player.sendMessage("§aVous avez déposé §6" + amount + " points §aen banque.");
            showBalance(player);
        } else {
            player.sendMessage("§cVous n'avez pas assez de points!");
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bank withdraw <montant|all>");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return;

        int amount;
        if (args[1].equalsIgnoreCase("all")) {
            amount = data.getBankPoints();
        } else {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cMontant invalide.");
                return;
            }
        }

        if (amount <= 0) {
            player.sendMessage("§cMontant invalide.");
            return;
        }

        if (plugin.getBankManager().withdrawPoints(player, amount)) {
            player.sendMessage("§aVous avez retiré §6" + amount + " points §ade la banque.");
            showBalance(player);
        } else {
            player.sendMessage("§cVotre solde bancaire est insuffisant!");
        }
    }

    private void showBalance(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) return;

        player.sendMessage("§6=== Votre Solde ===");
        player.sendMessage("§7Portefeuille: §6" + data.getPoints().get() + " points");
        player.sendMessage("§7En banque: §6" + data.getBankPoints() + " points");
        player.sendMessage("§7Gemmes: §d" + data.getGems().get());
        player.sendMessage("§7Coffre: §e" + plugin.getBankManager().getVaultSize(player.getUniqueId()) + " slots");
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Banque ===");
        player.sendMessage("§e/bank §7- Ouvrir le menu");
        player.sendMessage("§e/bank vault §7- Ouvrir le coffre");
        player.sendMessage("§e/bank deposit <montant> §7- Déposer des points");
        player.sendMessage("§e/bank withdraw <montant> §7- Retirer des points");
        player.sendMessage("§e/bank balance §7- Voir votre solde");
        player.sendMessage("");
        player.sendMessage("§7Les points en banque génèrent §a0.1%§7 d'intérêts/jour");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("vault", "deposit", "withdraw", "balance", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw")) {
                completions.addAll(List.of("100", "500", "1000", "5000", "all"));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
