package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.gui.AchievementGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /achievements ou /ach pour ouvrir le menu d'achievements
 */
public class AchievementCommand implements CommandExecutor {

    private final ZombieZPlugin plugin;

    public AchievementCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        // Ouvrir le menu achievements
        new AchievementGUI(plugin).open(player);

        return true;
    }
}
