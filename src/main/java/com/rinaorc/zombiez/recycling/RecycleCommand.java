package com.rinaorc.zombiez.recycling;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande /recycle pour gérer le recyclage automatique
 *
 * Usages:
 * - /recycle - Ouvre le menu de configuration
 * - /recycle toggle - Active/désactive le recyclage
 * - /recycle hand - Recycle l'item en main
 * - /recycle stats - Affiche les statistiques
 * - /recycle help - Affiche l'aide
 */
public class RecycleCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public RecycleCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        RecycleManager recycleManager = plugin.getRecycleManager();
        if (recycleManager == null) {
            player.sendMessage("§cLe système de recyclage n'est pas disponible.");
            return true;
        }

        // Sans argument: ouvrir le menu
        if (args.length == 0) {
            plugin.getRecycleGUI().open(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle", "on", "off" -> {
                RecycleSettings settings = recycleManager.getSettings(player.getUniqueId());
                boolean newState = subCommand.equals("on") || (subCommand.equals("toggle") && !settings.isAutoRecycleEnabled());
                settings.setAutoRecycleEnabled(newState);

                // Synchroniser vers PlayerData
                recycleManager.syncToPlayerData(player.getUniqueId());

                if (newState) {
                    player.sendMessage("§a§l♻ §aRecyclage automatique §lactivé§a!");
                    player.sendMessage("§7Les items des raretés sélectionnées seront recyclés au ramassage.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                } else {
                    player.sendMessage("§c§l♻ §cRecyclage automatique §ldésactivé§c.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
                }
            }

            case "hand", "main" -> {
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item.isEmpty()) {
                    player.sendMessage("§cVous n'avez pas d'item en main.");
                    return true;
                }

                if (!ZombieZItem.isZombieZItem(item)) {
                    player.sendMessage("§cCet item n'est pas un item ZombieZ recyclable.");
                    return true;
                }

                Rarity rarity = ZombieZItem.getItemRarity(item);
                int zoneLevel = ZombieZItem.getItemZoneLevel(item);
                int points = recycleManager.recycleItem(player, item);

                if (points > 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.sendMessage("§a§l♻ §7Item recyclé: " + rarity.getColoredName() + " §8(Zone " + zoneLevel + ")");
                    player.sendMessage("§7Vous avez gagné §6+" + points + " points§7!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                } else {
                    player.sendMessage("§cImpossible de recycler cet item.");
                }
            }

            case "stats", "statistiques" -> {
                String[] stats = recycleManager.getPlayerStats(player.getUniqueId());
                for (String line : stats) {
                    player.sendMessage(line);
                }
            }

            case "preview", "apercu" -> {
                player.sendMessage("§6§l♻ Aperçu des Points de Recyclage");
                player.sendMessage("");

                com.rinaorc.zombiez.data.PlayerData pData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                int currentZone = pData != null ? pData.getCurrentZone().get() : 1;

                for (Rarity rarity : Rarity.values()) {
                    int points = recycleManager.calculateRecyclePoints(rarity, currentZone);
                    int maxPoints = recycleManager.calculateRecyclePoints(rarity, 50);
                    player.sendMessage(rarity.getColoredName() + " §8→ §e" + points + " pts §8(Zone " + currentZone + ") §7| §6" + maxPoints + " pts §8(Zone 50)");
                }
            }

            case "all", "tout", "inventory", "inventaire" -> {
                RecycleSettings settings = recycleManager.getSettings(player.getUniqueId());
                int totalPoints = 0;
                int itemsRecycled = 0;

                ItemStack[] contents = player.getInventory().getStorageContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item == null || item.isEmpty()) continue;
                    if (!ZombieZItem.isZombieZItem(item)) continue;

                    Rarity rarity = ZombieZItem.getItemRarity(item);
                    if (rarity == null) continue;

                    // Respecter les paramètres de rareté du joueur
                    if (!settings.shouldRecycle(rarity)) continue;

                    int points = recycleManager.recycleItem(player, item);
                    if (points > 0) {
                        player.getInventory().setItem(i, null);
                        totalPoints += points;
                        itemsRecycled++;
                    }
                }

                if (itemsRecycled > 0) {
                    player.sendMessage("§a§l♻ §aRecyclage massif terminé!");
                    player.sendMessage("§7Items recyclés: §e" + itemsRecycled);
                    player.sendMessage("§7Points gagnés: §6+" + RecycleManager.formatPoints(totalPoints));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                } else {
                    player.sendMessage("§cAucun item recyclable trouvé dans votre inventaire.");
                    player.sendMessage("§7Vérifiez vos paramètres de rareté avec §e/recycle§7.");
                }
            }

            case "help", "aide", "?" -> {
                sendHelp(player);
            }

            default -> {
                player.sendMessage("§cCommande inconnue. Utilisez §e/recycle help §cpour l'aide.");
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l♻ Aide - Recyclage Automatique");
        player.sendMessage("");
        player.sendMessage("§e/recycle §7- Ouvre le menu de configuration");
        player.sendMessage("§e/recycle toggle §7- Active/désactive le recyclage");
        player.sendMessage("§e/recycle hand §7- Recycle l'item en main");
        player.sendMessage("§e/recycle all §7- Recycle tout l'inventaire");
        player.sendMessage("§e/recycle stats §7- Affiche vos statistiques");
        player.sendMessage("§e/recycle preview §7- Aperçu des points par rareté");
        player.sendMessage("");
        player.sendMessage("§7Le recyclage convertit automatiquement");
        player.sendMessage("§7vos items en §6points §7au ramassage.");
        player.sendMessage("§7Plus l'item est §erare §7et plus la §bzone §7est haute,");
        player.sendMessage("§7plus vous gagnez de points!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = List.of("toggle", "on", "off", "hand", "all", "stats", "preview", "help");
            String input = args[0].toLowerCase();
            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}
