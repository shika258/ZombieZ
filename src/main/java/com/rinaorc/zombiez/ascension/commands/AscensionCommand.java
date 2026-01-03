package com.rinaorc.zombiez.ascension.commands;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionData;
import com.rinaorc.zombiez.ascension.AscensionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Commande /as (et aliases /asc, /ascension) pour ouvrir le menu d'Ascension
 */
public class AscensionCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public AscensionCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) {
            player.sendMessage("§cLe système d'Ascension n'est pas disponible.");
            return true;
        }

        // Ouvrir le GUI
        manager.openAscensionGUI(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
