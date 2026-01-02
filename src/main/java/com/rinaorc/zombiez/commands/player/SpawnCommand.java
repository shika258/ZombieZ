package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Commande /spawn - Téléportation au spawn
 */
public class SpawnCommand implements CommandExecutor {

    private final ZombieZPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 30 * 1000; // 30 secondes

    public SpawnCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement!");
            return true;
        }

        // Vérifier le cooldown
        long lastUse = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = (lastUse + COOLDOWN_MS) - System.currentTimeMillis();

        if (remaining > 0) {
            long seconds = remaining / 1000;
            MessageUtils.send(player, "§cTu dois attendre encore §e" +
                MessageUtils.formatTime(seconds) + " §cavant de te téléporter!");
            return true;
        }

        // Vérifier si le joueur est en combat
        // TODO: Implémenter le système de combat tag

        // Téléporter au spawn
        Zone spawnZone = plugin.getZoneManager().getSpawnZone();
        Location spawnLoc = getSpawnLocation(spawnZone, player.getWorld());

        player.teleport(spawnLoc);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        MessageUtils.sendTitle(player, "§a§lSPAWN", "§7Téléporté au village de départ", 10, 30, 10);
        MessageUtils.send(player, "§aTéléporté au spawn! §7(Cooldown: 30s)");

        return true;
    }

    /**
     * Obtient la location de spawn appropriée
     * Location fixe du village de départ ZombieZ
     */
    private Location getSpawnLocation(Zone spawnZone, World world) {
        World targetWorld = Bukkit.getWorld("world");
        if (targetWorld == null) targetWorld = world;

        // Location fixe du spawn ZombieZ (yaw 180 = face au sud, pitch 0 = droit)
        return new Location(targetWorld, 728.5, 94, 9987.5, 180f, 0f);
    }
}
