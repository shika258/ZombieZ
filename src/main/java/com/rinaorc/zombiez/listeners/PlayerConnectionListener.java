package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

/**
 * Listener pour les connexions et déconnexions des joueurs
 * Gère le chargement/sauvegarde des données et l'initialisation du HUD
 */
public class PlayerConnectionListener implements Listener {

    private final ZombieZPlugin plugin;

    public PlayerConnectionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Pré-chargement des données (async, avant que le joueur soit vraiment connecté)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // On pourrait pré-charger les données ici si nécessaire
        // Mais on préfère le faire au PlayerJoinEvent pour avoir accès au Player
    }

    /**
     * Connexion d'un joueur
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Supprimer le message de join par défaut
        event.joinMessage(null);

        // Charger les données du joueur de manière async
        plugin.getPlayerDataManager().loadPlayerAsync(player).thenAccept(data -> {
            // Une fois chargé, initialiser sur le main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                onPlayerDataLoaded(player, data);
            });
        }).exceptionally(e -> {
            plugin.log(Level.SEVERE, "§cErreur chargement données de " + player.getName() + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Appelé quand les données du joueur sont chargées
     */
    private void onPlayerDataLoaded(Player player, PlayerData data) {
        if (!player.isOnline()) return;

        // Vérifier la zone actuelle
        plugin.getZoneManager().checkPlayerZone(player);

        // Message de bienvenue
        boolean isNew = data.getKills().get() == 0 && data.getPlaytime().get() < 60;
        
        if (isNew) {
            // Nouveau joueur
            sendWelcomeMessage(player);
            MessageUtils.broadcast("§a+ §7Bienvenue à §e" + player.getName() + " §7dans l'apocalypse!");

            // Donner le kit de départ après un délai pour laisser le temps au joueur de voir les messages
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getStarterKitManager().giveStarterKit(player);
                    // Téléporter au spawn si configuré
                    teleportToSpawn(player);
                }
            }, 60L); // 3 secondes après la connexion
        } else {
            // Joueur existant
            sendReturnMessage(player, data);
            MessageUtils.broadcast("§a+ §7" + player.getName() + " §7a rejoint le serveur");
        }

        // Log
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.log(Level.INFO, "§7Joueur " + player.getName() + " chargé (Niveau " + 
                data.getLevel().get() + ", Zone " + data.getCurrentZone().get() + ")");
        }
    }

    /**
     * Téléporte le joueur au spawn configuré
     */
    private void teleportToSpawn(Player player) {
        // Lire les coordonnées de spawn depuis la config
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();

        if (config.contains("gameplay.spawn.x")) {
            double x = config.getDouble("gameplay.spawn.x", 621);
            double y = config.getDouble("gameplay.spawn.y", 70);
            double z = config.getDouble("gameplay.spawn.z", 10300);
            float yaw = (float) config.getDouble("gameplay.spawn.yaw", 0);
            float pitch = (float) config.getDouble("gameplay.spawn.pitch", 0);

            org.bukkit.World world = plugin.getServer().getWorlds().get(0);
            org.bukkit.Location spawnLoc = new org.bukkit.Location(world, x, y, z, yaw, pitch);

            player.teleport(spawnLoc);
        }
    }

    /**
     * Envoie le message de bienvenue aux nouveaux joueurs
     */
    private void sendWelcomeMessage(Player player) {
        MessageUtils.sendTitle(player, "§6§lZOMBIEZ", "§7Bienvenue dans l'apocalypse", 20, 60, 20);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§8§m                                                  ");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §6§lBIENVENUE SUR ZOMBIEZ!");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §7▸ Tue des zombies pour gagner des §ePoints");
            MessageUtils.sendRaw(player, "  §7▸ Avance vers le §bNord §7pour plus de défis");
            MessageUtils.sendRaw(player, "  §7▸ Collecte des items §5Légendaires §7uniques");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "  §a/zone §7- Voir ta zone actuelle");
            MessageUtils.sendRaw(player, "  §a/stats §7- Voir tes statistiques");
            MessageUtils.sendRaw(player, "  §a/refuge §7- Trouver le refuge le plus proche");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§8§m                                                  ");
            MessageUtils.sendRaw(player, "");
        }, 40L);
    }

    /**
     * Envoie le message de retour aux joueurs existants
     */
    private void sendReturnMessage(Player player, PlayerData data) {
        String zone = "Zone " + data.getCurrentZone().get();
        String time = MessageUtils.formatTime(data.getPlaytime().get());
        
        MessageUtils.sendTitle(player, "§a§lBon retour!", "§7" + zone + " • " + time + " de jeu", 10, 40, 10);
        
        // Résumé rapide
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.send(player, "§7Niveau §e" + data.getLevel().get() + 
                " §7| §c" + data.getKills().get() + " §7kills | §6" + 
                EconomyManager.formatCompact(data.getPoints().get()) + " §7points");
        }, 20L);
    }

    /**
     * Déconnexion d'un joueur
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Supprimer le message de quit par défaut
        event.quitMessage(null);
        
        // Broadcast
        MessageUtils.broadcast("§c- §7" + player.getName() + " §7a quitté le serveur");

        // Supprimer du cache de zone
        plugin.getZoneManager().removeFromCache(player.getUniqueId());
        
        // Nettoyer le cache de déplacement (FIX: fuite mémoire)
        if (plugin.getPlayerMoveListener() != null) {
            plugin.getPlayerMoveListener().removeFromCache(player.getUniqueId());
        }
        
        // Nettoyer le momentum (garder les records mais nettoyer l'état temporaire)
        if (plugin.getMomentumManager() != null) {
            plugin.getMomentumManager().onPlayerQuit(player);
        }
        
        // Nettoyer les invitations de party en attente
        if (plugin.getPartyManager() != null) {
            plugin.getPartyManager().onPlayerQuit(player);
        }

        // Sauvegarder et décharger les données (async)
        plugin.getPlayerDataManager().unloadPlayer(player.getUniqueId()).thenRun(() -> {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.log(Level.INFO, "§7Joueur " + player.getName() + " sauvegardé et déchargé");
            }
        });
    }
}
