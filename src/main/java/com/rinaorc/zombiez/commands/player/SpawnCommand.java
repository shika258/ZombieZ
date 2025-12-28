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
    private static final long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

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
        MessageUtils.send(player, "§aTéléporté au spawn! §7(Cooldown: 5 minutes)");

        return true;
    }

    /**
     * Obtient la location de spawn appropriée
     * Priorité: refugeLocation > centre de la zone spawn > spawn du monde
     */
    private Location getSpawnLocation(Zone spawnZone, World world) {
        // 1. Si la zone a une refugeLocation définie, l'utiliser
        if (spawnZone != null && spawnZone.getRefugeLocation() != null) {
            return spawnZone.getRefugeLocation();
        }

        // 2. Si la zone existe, calculer le centre de la zone
        if (spawnZone != null) {
            World targetWorld = Bukkit.getWorld("world");
            if (targetWorld == null) targetWorld = world;

            int centerX = (spawnZone.getMinX() + spawnZone.getMaxX()) / 2;
            int centerZ = (spawnZone.getMinZ() + spawnZone.getMaxZ()) / 2;
            int y = targetWorld.getHighestBlockYAt(centerX, centerZ) + 1;

            return new Location(targetWorld, centerX + 0.5, y, centerZ + 0.5);
        }

        // 3. Fallback: spawn du monde
        return world.getSpawnLocation();
    }
}
