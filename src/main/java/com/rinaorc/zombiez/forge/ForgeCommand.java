package com.rinaorc.zombiez.forge;

import com.rinaorc.zombiez.ZombieZPlugin;
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
 * Commande /forge pour ouvrir la GUI de forge
 *
 * Commandes admin:
 * - /forge admin give <joueur> protection <quantité>
 * - /forge admin give <joueur> chance <quantité>
 * - /forge admin give <joueur> blessed <quantité>
 * - /forge admin setlevel <niveau>
 */
public class ForgeCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ForgeCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Commandes admin
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("zombiez.forge.admin")) {
                sender.sendMessage("§c[Forge] §7Vous n'avez pas la permission!");
                return true;
            }
            return handleAdmin(sender, args);
        }

        // Commande joueur - ouvrir la GUI
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Forge] §7Cette commande est réservée aux joueurs!");
            return true;
        }

        ForgeGUI gui = new ForgeGUI(plugin, player);
        gui.open();

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "stats" -> handleStats(sender, args);
            default -> sendAdminHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§c[Forge] §7Usage: /forge admin give <joueur> <type> <quantité>");
            sender.sendMessage("§7Types: §eprotection§7, §echance§7, §eblessed");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c[Forge] §7Joueur introuvable!");
            return;
        }

        String type = args[3].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
            amount = Math.max(1, Math.min(64, amount));
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[Forge] §7Quantité invalide!");
            return;
        }

        ForgeManager forgeManager = plugin.getForgeManager();

        switch (type) {
            case "protection" -> {
                target.getInventory().addItem(forgeManager.createProtectionStone(amount));
                sender.sendMessage("§a[Forge] §7Donné §d" + amount + "x Pierre de Protection §7à §e" + target.getName());
                target.sendMessage("§a[Forge] §7Vous avez reçu §d" + amount + "x Pierre de Protection§7!");
            }
            case "chance" -> {
                target.getInventory().addItem(forgeManager.createChanceStone(amount));
                sender.sendMessage("§a[Forge] §7Donné §b" + amount + "x Pierre de Chance §7à §e" + target.getName());
                target.sendMessage("§a[Forge] §7Vous avez reçu §b" + amount + "x Pierre de Chance§7!");
            }
            case "blessed", "benie" -> {
                target.getInventory().addItem(forgeManager.createBlessedStone(amount));
                sender.sendMessage("§a[Forge] §7Donné §6" + amount + "x Pierre Bénie §7à §e" + target.getName());
                target.sendMessage("§a[Forge] §7Vous avez reçu §6" + amount + "x Pierre Bénie§7!");
            }
            default -> sender.sendMessage("§c[Forge] §7Type invalide! (protection, chance, blessed)");
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[Forge] §7Tenez l'item en main!");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§c[Forge] §7Usage: /forge admin setlevel <0-10>");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            level = Math.max(0, Math.min(ForgeManager.MAX_FORGE_LEVEL, level));
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[Forge] §7Niveau invalide! (0-10)");
            return;
        }

        var item = player.getInventory().getItemInMainHand();
        if (!plugin.getForgeManager().canBeForged(item)) {
            sender.sendMessage("§c[Forge] §7L'item en main ne peut pas être forgé!");
            return;
        }

        plugin.getForgeManager().setForgeLevel(item, level);
        sender.sendMessage("§a[Forge] §7Item mis au niveau §e+" + level);
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (args.length < 3) {
            if (sender instanceof Player player) {
                showStats(sender, player.getUniqueId(), player.getName());
            } else {
                sender.sendMessage("§c[Forge] §7Usage: /forge admin stats <joueur>");
            }
            return;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c[Forge] §7Joueur introuvable!");
            return;
        }

        showStats(sender, target.getUniqueId(), target.getName());
    }

    private void showStats(CommandSender sender, java.util.UUID uuid, String name) {
        ForgeManager.ForgeStats stats = plugin.getForgeManager().getStats(uuid);

        sender.sendMessage("");
        sender.sendMessage("§6§l🔨 Stats Forge - §e" + name);
        sender.sendMessage("");

        if (stats != null) {
            sender.sendMessage("§7Tentatives: §f" + stats.getTotalAttempts());
            sender.sendMessage("§7Succès: §a" + stats.getTotalSuccess());
            sender.sendMessage("§7Échecs: §c" + stats.getTotalFailures());
            sender.sendMessage("§7Taux: §e" + String.format("%.1f", stats.getSuccessRate()) + "%");
            sender.sendMessage("§7Plus haut niveau: §6+" + stats.getHighestLevel());
            sender.sendMessage("§7Items +10: §6" + stats.getItemsAtMax());
            sender.sendMessage("§7Points dépensés: §e" + String.format("%,d", stats.getPointsSpent()));
        } else {
            sender.sendMessage("§8Aucune statistique disponible.");
        }
        sender.sendMessage("");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l🔨 Forge Admin");
        sender.sendMessage("");
        sender.sendMessage("§e/forge admin give <joueur> <type> <qté>");
        sender.sendMessage("§7  Types: protection, chance, blessed");
        sender.sendMessage("");
        sender.sendMessage("§e/forge admin setlevel <0-10>");
        sender.sendMessage("§7  Définit le niveau de l'item en main");
        sender.sendMessage("");
        sender.sendMessage("§e/forge admin stats [joueur]");
        sender.sendMessage("§7  Affiche les stats de forge");
        sender.sendMessage("");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("zombiez.forge.admin")) {
                if ("admin".startsWith(args[0].toLowerCase())) {
                    completions.add("admin");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            for (String sub : List.of("give", "setlevel", "stats")) {
                if (sub.startsWith(args[1].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("stats")) {
                // Compléter avec les noms de joueurs
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (args[1].equalsIgnoreCase("setlevel")) {
                for (int i = 0; i <= 10; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            for (String type : List.of("protection", "chance", "blessed")) {
                if (type.startsWith(args[3].toLowerCase())) {
                    completions.add(type);
                }
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            for (int i : List.of(1, 5, 10, 32, 64)) {
                completions.add(String.valueOf(i));
            }
        }

        return completions;
    }
}
