package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableRarity;
import com.rinaorc.zombiez.consumables.ConsumableType;
import org.bukkit.Bukkit;
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
 * Commande admin pour gérer les consommables
 * /zzconsumable give <type> [rarity] [zone] [player]
 * /zzconsumable list
 */
public class ConsumableAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ConsumableAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zombiez.admin")) {
            sender.sendMessage("§c✖ §7Vous n'avez pas la permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "giveall" -> handleGiveAll(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c✖ §7Usage: /zzconsumable give <type> [rarity] [zone] [player]");
            return;
        }

        // Parsé le type
        ConsumableType type;
        try {
            type = ConsumableType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c✖ §7Type invalide! Utilisez /zzconsumable list");
            return;
        }

        // Rareté (défaut: UNCOMMON)
        ConsumableRarity rarity = ConsumableRarity.UNCOMMON;
        if (args.length >= 3) {
            try {
                rarity = ConsumableRarity.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§c✖ §7Rareté invalide! (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)");
                return;
            }
        }

        // Zone (défaut: 25)
        int zoneId = 25;
        if (args.length >= 4) {
            try {
                zoneId = Integer.parseInt(args[3]);
                if (zoneId < 1 || zoneId > 50) {
                    sender.sendMessage("§c✖ §7Zone doit être entre 1 et 50!");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✖ §7Zone invalide!");
                return;
            }
        }

        // Joueur cible
        Player target;
        if (args.length >= 5) {
            target = Bukkit.getPlayer(args[4]);
            if (target == null) {
                sender.sendMessage("§c✖ §7Joueur non trouvé!");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("§c✖ §7Spécifiez un joueur!");
            return;
        }

        // Créer et donner le consommable
        Consumable consumable = plugin.getConsumableManager().generateConsumable(type, zoneId, rarity);
        plugin.getConsumableManager().giveConsumable(target, consumable);

        sender.sendMessage("§a✓ §7Consommable donné: " + rarity.getColor() + type.getDisplayName() +
                           " §7(Zone " + zoneId + ") à §e" + target.getName());

        if (target != sender) {
            target.sendMessage("§a✓ §7Vous avez reçu un consommable admin!");
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage("§6=== §eConsommables Disponibles §6===");
        sender.sendMessage("");

        for (ConsumableType.ConsumableCategory category : ConsumableType.ConsumableCategory.values()) {
            sender.sendMessage(category.getColor() + "§l" + category.getDisplayName() + ":");

            for (ConsumableType type : ConsumableType.values()) {
                if (type.getCategory() == category) {
                    sender.sendMessage("  §7- §f" + type.name() + " §7(" + type.getDisplayName() + ")");
                }
            }
            sender.sendMessage("");
        }

        sender.sendMessage("§7Raretés: §fCOMMON§7, §aUNCOMMON§7, §9RARE§7, §5EPIC§7, §6LEGENDARY");
    }

    private void handleGiveAll(CommandSender sender, String[] args) {
        // Rareté
        ConsumableRarity rarity = ConsumableRarity.RARE;
        if (args.length >= 2) {
            try {
                rarity = ConsumableRarity.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // Zone
        int zoneId = 30;
        if (args.length >= 3) {
            try {
                zoneId = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        // Joueur cible
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage("§c✖ §7Joueur non trouvé!");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("§c✖ §7Spécifiez un joueur!");
            return;
        }

        // Donner tous les types
        for (ConsumableType type : ConsumableType.values()) {
            Consumable consumable = plugin.getConsumableManager().generateConsumable(type, zoneId, rarity);
            plugin.getConsumableManager().giveConsumable(target, consumable);
        }

        sender.sendMessage("§a✓ §7Tous les consommables (" + ConsumableType.values().length +
                           ") donnés à §e" + target.getName() + " §7en rareté " + rarity.getColor() + rarity.getDisplayName());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== §eCommandes Consommables Admin §6===");
        sender.sendMessage("§e/zzconsumable give <type> [rarity] [zone] [player]");
        sender.sendMessage("  §7Donne un consommable spécifique");
        sender.sendMessage("§e/zzconsumable list");
        sender.sendMessage("  §7Liste tous les types de consommables");
        sender.sendMessage("§e/zzconsumable giveall [rarity] [zone] [player]");
        sender.sendMessage("  §7Donne un de chaque consommable");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "list", "giveall"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.stream(ConsumableType.values())
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("giveall")) {
                completions.addAll(Arrays.stream(ConsumableRarity.values())
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.stream(ConsumableRarity.values())
                    .map(Enum::name)
                    .collect(Collectors.toList()));
            } else {
                for (int i = 1; i <= 50; i += 10) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                for (int i = 1; i <= 50; i += 10) {
                    completions.add(String.valueOf(i));
                }
            } else {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 5) {
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList()));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
