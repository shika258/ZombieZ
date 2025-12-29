package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Commande /stats - Affiche les statistiques du joueur
 */
public class StatsCommand implements CommandExecutor {

    private final ZombieZPlugin plugin;

    public StatsCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) {
            MessageUtils.send(player, "§cErreur: Données non chargées!");
            return true;
        }

        // Obtenir la zone actuelle
        var zone = plugin.getZoneManager().getPlayerZone(player);
        String zoneName = zone != null ? zone.getColoredName() : "§7Inconnue";

        // Calculer la progression du niveau
        String levelProgress = MessageUtils.progressBar(data.getLevelProgress(), 20, "§a", "§7");

        // Construire le message
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("         §6§l⚔ VOS STATISTIQUES ⚔");
        player.sendMessage("");
        
        // Progression
        player.sendMessage("  §7Niveau: §e§l" + data.getLevel().get() + " §7Prestige: §d" + data.getPrestige().get());
        player.sendMessage("  " + levelProgress + " §f" + String.format("%.1f", data.getLevelProgress()) + "%");
        player.sendMessage("");
        
        // Économie
        player.sendMessage("  §6💰 Points: §f" + EconomyManager.formatPoints(data.getPoints().get()));
        player.sendMessage("  §d💎 Gemmes: §f" + data.getGems().get());
        player.sendMessage("");
        
        // Combat
        player.sendMessage("  §c⚔ Kills: §f" + EconomyManager.formatCompact(data.getKills().get()) + 
            "    §4☠ Morts: §f" + data.getDeaths().get());
        player.sendMessage("  §e📊 K/D Ratio: §f" + String.format("%.2f", data.getKDRatio()));
        
        int streak = data.getKillStreak().get();
        if (streak > 0) {
            player.sendMessage("  §6🔥 Kill Streak: §f" + streak + " §7(Best: " + data.getBestKillStreak().get() + ")");
        }
        player.sendMessage("");
        
        // Progression zones
        player.sendMessage("  §b🗺 Zone actuelle: " + zoneName);
        player.sendMessage("  §b🏆 Zone max atteinte: §f" + data.getMaxZone().get() + " / 10");
        player.sendMessage("  §a⛳ Checkpoint actif: §f" + 
            (data.getCurrentCheckpoint().get() > 0 ? "Refuge " + data.getCurrentCheckpoint().get() : "Spawn"));
        player.sendMessage("");
        
        // Temps de jeu
        player.sendMessage("  §7⏱ Temps de jeu: §f" + data.getFormattedPlaytime());
        
        // VIP
        if (data.isVip()) {
            player.sendMessage("  §e⭐ Rang VIP: §f" + data.getVipRank());
        }
        
        player.sendMessage("");
        player.sendMessage("§8§m                                              ");
        player.sendMessage("");

        return true;
    }
}
