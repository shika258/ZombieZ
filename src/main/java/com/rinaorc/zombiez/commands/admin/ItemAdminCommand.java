package com.rinaorc.zombiez.commands.admin;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.generator.ItemGenerator;
import com.rinaorc.zombiez.items.gui.ItemCompareGUI;
import com.rinaorc.zombiez.items.gui.LootRevealGUI;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande admin pour le système d'items
 * /zzitem <generate|give|compare|crate|stats|debug>
 */
public class ItemAdminCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;
    private final ItemGenerator generator;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "generate", "give", "compare", "crate", "stats", "debug"
    );

    public ItemAdminCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.generator = ItemGenerator.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("zombiez.admin.items")) {
            sender.sendMessage("§cVous n'avez pas la permission!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "generate", "gen" -> handleGenerate(player, args);
            case "give" -> handleGive(player, args);
            case "compare" -> handleCompare(player);
            case "crate" -> handleCrate(player, args);
            case "stats" -> handleStats(player);
            case "debug" -> handleDebug(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8§m                                        ");
        player.sendMessage("§6§lITEM ADMIN §7- Commandes");
        player.sendMessage("");
        player.sendMessage("§e/zzitem generate [zone] [rarity] §7- Génère un item");
        player.sendMessage("§e/zzitem give <joueur> [zone] [rarity] §7- Donne un item");
        player.sendMessage("§e/zzitem compare §7- Compare 2 items en main");
        player.sendMessage("§e/zzitem crate [count] [zone] §7- Ouvre une crate test");
        player.sendMessage("§e/zzitem stats §7- Stats de votre équipement");
        player.sendMessage("§e/zzitem debug <item|affix|all> §7- Infos debug");
        player.sendMessage("§8§m                                        ");
    }

    /**
     * /zzitem generate [zone] [rarity] [type]
     */
    private void handleGenerate(Player player, String[] args) {
        int zone = 5; // Zone par défaut
        Rarity rarity = null;
        ItemType type = null;

        // Parse zone
        if (args.length >= 2) {
            try {
                zone = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cZone invalide! (1-10)");
                return;
            }
        }

        // Parse rarity
        if (args.length >= 3) {
            try {
                rarity = Rarity.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cRareté invalide! (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC, EXALTED)");
                return;
            }
        }

        // Parse type
        if (args.length >= 4) {
            try {
                type = ItemType.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cType invalide! (SWORD, AXE, HELMET, CHESTPLATE, etc.)");
                return;
            }
        }

        // Générer l'item
        ZombieZItem item;
        if (rarity != null && type != null) {
            item = generator.generate(zone, rarity, type, 0);
        } else if (rarity != null) {
            item = generator.generate(zone, rarity, 0);
        } else if (type != null) {
            item = generator.generate(zone, type, 0);
        } else {
            item = generator.generate(zone, 0);
        }

        // Donner l'item
        plugin.getItemManager().giveItem(player, item);

        player.sendMessage("§a✓ Item généré: " + item.getRarity().getColoredName() + " " + 
            item.getGeneratedName() + " §8[" + item.getItemScore() + " IS]");
    }

    /**
     * /zzitem give <joueur> [zone] [rarity]
     */
    private void handleGive(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /zzitem give <joueur> [zone] [rarity]");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJoueur non trouvé!");
            return;
        }

        int zone = args.length >= 3 ? Integer.parseInt(args[2]) : 5;
        double luck = 0;

        Rarity rarity = null;
        if (args.length >= 4) {
            try {
                rarity = Rarity.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        ZombieZItem item = rarity != null ? 
            generator.generate(zone, rarity, luck) : 
            generator.generate(zone, luck);

        plugin.getItemManager().giveItem(target, item);

        player.sendMessage("§a✓ Item donné à " + target.getName() + ": " + 
            item.getRarity().getColoredName() + " " + item.getGeneratedName());
        target.sendMessage("§a✓ Vous avez reçu: " + item.getRarity().getColoredName() + 
            " " + item.getGeneratedName());
    }

    /**
     * /zzitem compare
     */
    private void handleCompare(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (!ZombieZItem.isZombieZItem(mainHand) && !ZombieZItem.isZombieZItem(offHand)) {
            player.sendMessage("§cTenez deux items ZombieZ pour les comparer!");
            player.sendMessage("§7Main principale + Seconde main");
            return;
        }

        ZombieZItem item1 = plugin.getItemManager().getItem(mainHand);
        ZombieZItem item2 = plugin.getItemManager().getItem(offHand);

        ItemCompareGUI gui = new ItemCompareGUI(plugin, player, item1, item2);
        gui.open();
    }

    /**
     * /zzitem crate [count] [zone]
     */
    private void handleCrate(Player player, String[] args) {
        int count = args.length >= 2 ? Integer.parseInt(args[1]) : 3;
        int zone = args.length >= 3 ? Integer.parseInt(args[2]) : 5;

        count = Math.min(count, 4); // Max 4 items

        List<ZombieZItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Bonus de luck simulé pour une crate
            items.add(generator.generate(zone, 0.2));
        }

        LootRevealGUI gui = new LootRevealGUI(plugin, player, items, "Test Crate");
        gui.open();
    }

    /**
     * /zzitem stats
     */
    private void handleStats(Player player) {
        var stats = plugin.getItemManager().calculatePlayerStats(player);
        int totalScore = plugin.getItemManager().calculateTotalItemScore(player);

        player.sendMessage("§8§m                                        ");
        player.sendMessage("§6§lSTATS D'ÉQUIPEMENT");
        player.sendMessage("");
        player.sendMessage("§7Item Score Total: §6" + totalScore);
        player.sendMessage("");

        if (stats.isEmpty()) {
            player.sendMessage("§7Aucun item ZombieZ équipé.");
        } else {
            // Grouper par catégorie
            player.sendMessage("§c§lOffensif:");
            stats.entrySet().stream()
                .filter(e -> e.getKey().getCategory() == com.rinaorc.zombiez.items.types.StatType.StatCategory.OFFENSIVE)
                .forEach(e -> player.sendMessage("  " + e.getKey().getLoreLine(e.getValue())));

            player.sendMessage("§9§lDéfensif:");
            stats.entrySet().stream()
                .filter(e -> e.getKey().getCategory() == com.rinaorc.zombiez.items.types.StatType.StatCategory.DEFENSIVE)
                .forEach(e -> player.sendMessage("  " + e.getKey().getLoreLine(e.getValue())));

            player.sendMessage("§d§lÉlémentaire:");
            stats.entrySet().stream()
                .filter(e -> e.getKey().getCategory() == com.rinaorc.zombiez.items.types.StatType.StatCategory.ELEMENTAL)
                .forEach(e -> player.sendMessage("  " + e.getKey().getLoreLine(e.getValue())));

            player.sendMessage("§a§lUtilitaire:");
            stats.entrySet().stream()
                .filter(e -> e.getKey().getCategory() == com.rinaorc.zombiez.items.types.StatType.StatCategory.UTILITY)
                .forEach(e -> player.sendMessage("  " + e.getKey().getLoreLine(e.getValue())));
        }

        player.sendMessage("§8§m                                        ");
    }

    /**
     * /zzitem debug <item|affix|all>
     */
    private void handleDebug(Player player, String[] args) {
        String mode = args.length >= 2 ? args[1].toLowerCase() : "all";

        player.sendMessage("§8§m                                        ");
        player.sendMessage("§6§lDEBUG ITEMS");
        player.sendMessage("");

        switch (mode) {
            case "item" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (!ZombieZItem.isZombieZItem(hand)) {
                    player.sendMessage("§cTenez un item ZombieZ!");
                    return;
                }
                ZombieZItem item = plugin.getItemManager().getItem(hand);
                if (item != null) {
                    player.sendMessage("§7UUID: §e" + item.getUuid());
                    player.sendMessage("§7Type: §e" + item.getItemType().name());
                    player.sendMessage("§7Material: §e" + item.getMaterial().name());
                    player.sendMessage("§7Rarity: " + item.getRarity().getColoredName());
                    player.sendMessage("§7Tier: §e" + item.getTier());
                    player.sendMessage("§7Zone: §e" + item.getZoneLevel());
                    player.sendMessage("§7Score: §6" + item.getItemScore());
                    player.sendMessage("§7Affixes: §b" + item.getAffixes().size());
                    for (var affix : item.getAffixes()) {
                        player.sendMessage("  §7- " + affix.getAffix().getColoredName());
                    }
                }
            }
            case "affix" -> {
                var registry = com.rinaorc.zombiez.items.affixes.AffixRegistry.getInstance();
                player.sendMessage("§7Affixes chargés: §e" + registry.getAffixCount());
                player.sendMessage("§7Préfixes: §e" + registry.getAffixesByType(
                    com.rinaorc.zombiez.items.affixes.Affix.AffixType.PREFIX).size());
                player.sendMessage("§7Suffixes: §e" + registry.getAffixesByType(
                    com.rinaorc.zombiez.items.affixes.Affix.AffixType.SUFFIX).size());
            }
            default -> {
                player.sendMessage("§7Cache stats: §e" + plugin.getItemManager().getCacheStats());
                player.sendMessage("§7Raretés: §e" + Rarity.values().length);
                player.sendMessage("§7Types d'items: §e" + ItemType.values().length);
                player.sendMessage("§7Stats types: §e" + com.rinaorc.zombiez.items.types.StatType.values().length);
            }
        }

        player.sendMessage("§8§m                                        ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("zombiez.admin.items")) {
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
            if (subCmd.equals("generate") || subCmd.equals("gen") || subCmd.equals("crate")) {
                // Zones 1-10
                for (int i = 1; i <= 10; i++) {
                    completions.add(String.valueOf(i));
                }
            } else if (subCmd.equals("give")) {
                // Noms de joueurs
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (subCmd.equals("debug")) {
                completions.addAll(Arrays.asList("item", "affix", "all"));
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("generate") || subCmd.equals("gen") || subCmd.equals("give")) {
                // Raretés
                for (Rarity r : Rarity.values()) {
                    completions.add(r.name());
                }
            }
        } else if (args.length == 4) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("generate") || subCmd.equals("gen")) {
                // Types
                for (ItemType t : ItemType.values()) {
                    completions.add(t.name());
                }
            }
        }

        return completions;
    }
}
