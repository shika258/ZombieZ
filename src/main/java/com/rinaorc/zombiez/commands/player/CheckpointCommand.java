package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Commande /checkpoint - Affiche le checkpoint actuel
 */
public class CheckpointCommand implements CommandExecutor {

    private final ZombieZPlugin plugin;

    public CheckpointCommand(ZombieZPlugin plugin) {
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

        int checkpointId = data.getCurrentCheckpoint().get();

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("      §a§l⛳ CHECKPOINT ACTUEL");
        player.sendMessage("");

        if (checkpointId <= 0) {
            player.sendMessage("  §7Checkpoint: §eSpawn (défaut)");
            player.sendMessage("");
            player.sendMessage("  §7Tu respawneras au village de départ");
            player.sendMessage("  §7si tu meurs.");
            player.sendMessage("");
            player.sendMessage("  §eTip: §7Active un checkpoint dans un");
            player.sendMessage("  §7refuge pour respawn plus près!");
        } else {
            Zone zone = plugin.getZoneManager().getZoneById(checkpointId);
            String zoneName = zone != null ? zone.getColoredName() : "Zone " + checkpointId;
            
            player.sendMessage("  §7Checkpoint: §aRefuge " + checkpointId);
            player.sendMessage("  §7Zone: " + zoneName);
            player.sendMessage("");
            player.sendMessage("  §7Tu respawneras à ce refuge si tu meurs.");
        }

        player.sendMessage("");
        
        // Afficher les checkpoints disponibles
        player.sendMessage("  §7Checkpoints débloqués:");
        int maxZone = data.getMaxZone().get();
        StringBuilder checkpoints = new StringBuilder("  ");
        
        for (int i = 1; i <= maxZone && i <= 10; i++) {
            boolean isActive = (i == checkpointId);
            if (isActive) {
                checkpoints.append("§a[").append(i).append("] ");
            } else {
                checkpoints.append("§7").append(i).append(" ");
            }
        }
        player.sendMessage(checkpoints.toString());
        
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        return true;
    }
}
