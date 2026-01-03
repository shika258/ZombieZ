package com.rinaorc.zombiez.ascension.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionData;
import com.rinaorc.zombiez.ascension.AscensionManager;
import com.rinaorc.zombiez.ascension.Mutation;
import com.rinaorc.zombiez.ascension.gui.InsuranceGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listener pour les événements liés à l'Ascension
 */
public class AscensionListener implements Listener {

    private final ZombieZPlugin plugin;

    public AscensionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Créer les données (vides car reset au reboot)
        manager.getOrCreateData(player);

        // Message d'info si nouvelle session
        player.sendMessage("§8[§6Ascension§8] §7Nouvelle session ! Tue des zombies pour muter.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Nettoyer les données pour libérer la RAM
        manager.removeData(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        AscensionData data = manager.getData(player);
        if (data == null) return;

        // Si le joueur a des mutations, proposer l'assurance
        if (!data.getActiveMutations().isEmpty()) {
            // Sauvegarder les mutations pour l'assurance (avant reset)
            // Le GUI d'assurance sera ouvert au respawn
            player.setMetadata("ascension_had_mutations",
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Vérifier si le joueur avait des mutations
        if (player.hasMetadata("ascension_had_mutations")) {
            player.removeMetadata("ascension_had_mutations", plugin);

            AscensionData data = manager.getData(player);
            if (data != null && !data.getActiveMutations().isEmpty()) {
                // Ouvrir le GUI d'assurance avec un délai
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        InsuranceGUI.open(plugin, player, data);
                    }
                }, 20L); // 1 seconde après respawn
                return;
            }
        }

        // Reset normal sans assurance
        manager.resetPlayer(player);
    }
}
