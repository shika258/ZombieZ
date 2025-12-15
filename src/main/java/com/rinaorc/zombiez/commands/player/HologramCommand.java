package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande pour gérer les hologrammes de dégâts
 * /holo [on|off|toggle]
 */
public class HologramCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public HologramCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        var holoManager = plugin.getDamageHologramManager();
        if (holoManager == null) {
            player.sendMessage("§cLe système d'hologrammes n'est pas disponible.");
            return true;
        }

        String action = args.length > 0 ? args[0].toLowerCase() : "toggle";

        switch (action) {
            case "on", "enable", "activer" -> {
                holoManager.enableHolograms(player);
                holoManager.savePreferences(player);
                sendStatusMessage(player, true);
            }
            case "off", "disable", "desactiver" -> {
                holoManager.disableHolograms(player);
                holoManager.savePreferences(player);
                sendStatusMessage(player, false);
            }
            case "toggle", "switch" -> {
                boolean enabled = holoManager.toggleHolograms(player);
                holoManager.savePreferences(player);
                sendStatusMessage(player, enabled);
            }
            case "status", "info" -> {
                boolean enabled = holoManager.areHologramsEnabled(player);
                player.sendMessage("");
                player.sendMessage("§6§l⚙ Hologrammes de Dégâts");
                player.sendMessage("");
                player.sendMessage("§7  Statut: " + (enabled ? "§a✓ Activé" : "§c✗ Désactivé"));
                player.sendMessage("");
                player.sendMessage("§7  §8Types affichés:");
                player.sendMessage("§7    §f• Normal §8- §7Dégâts de base");
                player.sendMessage("§7    §c§l• Critique §8- §7Coups critiques");
                player.sendMessage("§7    §6• Feu §8- §7Dégâts de feu 🔥");
                player.sendMessage("§7    §b• Glace §8- §7Dégâts de glace ❄");
                player.sendMessage("§7    §e• Foudre §8- §7Dégâts électriques ⚡");
                player.sendMessage("§7    §2• Poison §8- §7Dégâts poison ☠");
                player.sendMessage("§7    §4§l• Execute §8- §7Bonus cible faible");
                player.sendMessage("§7    §5§l• Rage §8- §7Bonus berserker");
                player.sendMessage("§7    §a• Soin §8- §7Vol de vie ❤");
                player.sendMessage("§7    §a§l• Esquive §8- §7Attaque évitée ↷");
                player.sendMessage("");
                player.sendMessage("§7  §8Utilise §e/holo toggle §8pour changer.");
                player.sendMessage("");
            }
            default -> {
                player.sendMessage("§cUsage: §e/holo [on|off|toggle|status]");
            }
        }

        return true;
    }

    /**
     * Envoie le message de changement de statut
     */
    private void sendStatusMessage(Player player, boolean enabled) {
        player.sendMessage("");
        if (enabled) {
            player.sendMessage("§a§l✓ §aHologrammes de dégâts §2activés!");
            player.sendMessage("§7  Les nombres de dégâts s'afficheront au combat.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        } else {
            player.sendMessage("§c§l✗ §cHologrammes de dégâts §4désactivés!");
            player.sendMessage("§7  Les nombres ne s'afficheront plus.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> options = List.of("on", "off", "toggle", "status");

            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}
