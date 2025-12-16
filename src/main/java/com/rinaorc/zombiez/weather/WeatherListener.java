package com.rinaorc.zombiez.weather;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Listener pour les événements liés au système de météo dynamique
 *
 * Gère:
 * - Modification des dégâts des zombies selon la météo
 * - Empêcher le changement de météo Minecraft naturel
 * - Informer les nouveaux joueurs de la météo actuelle
 * - Intégration avec d'autres systèmes
 */
public class WeatherListener implements Listener {

    private final ZombieZPlugin plugin;
    private final WeatherManager weatherManager;

    public WeatherListener(ZombieZPlugin plugin, WeatherManager weatherManager) {
        this.plugin = plugin;
        this.weatherManager = weatherManager;
    }

    /**
     * Modifie les dégâts des zombies selon la météo actuelle
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Vérifier si c'est un zombie qui attaque un joueur
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (!(damager instanceof Zombie) || !(victim instanceof Player)) {
            return;
        }

        // Appliquer le multiplicateur de dégâts de la météo
        double damageMultiplier = weatherManager.getCurrentZombieDamageMultiplier();

        if (damageMultiplier != 1.0) {
            double originalDamage = event.getDamage();
            double modifiedDamage = originalDamage * damageMultiplier;
            event.setDamage(modifiedDamage);
        }
    }

    /**
     * Gère les dégâts environnementaux et les protections
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Réduire les dégâts de feu pendant la pluie
        WeatherType currentWeather = weatherManager.getCurrentWeatherType();

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
            event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {

            if (currentWeather == WeatherType.RAIN || currentWeather == WeatherType.STORM) {
                // Réduction de 30% des dégâts de feu sous la pluie
                event.setDamage(event.getDamage() * 0.7);
            } else if (currentWeather == WeatherType.ACID_RAIN) {
                // Augmentation de 20% des dégâts de feu sous pluie acide
                event.setDamage(event.getDamage() * 1.2);
            }
        }

        // Protection contre les dégâts de froid (blizzard) si le joueur a une armure en cuir
        if (currentWeather == WeatherType.BLIZZARD) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
                // Vérifier l'armure en cuir
                int leatherPieces = countLeatherArmor(player);
                if (leatherPieces > 0) {
                    double reduction = leatherPieces * 0.15; // 15% par pièce
                    event.setDamage(event.getDamage() * (1.0 - reduction));
                }
            }
        }
    }

    /**
     * Compte les pièces d'armure en cuir du joueur
     */
    private int countLeatherArmor(Player player) {
        int count = 0;
        var inventory = player.getInventory();

        if (inventory.getHelmet() != null &&
            inventory.getHelmet().getType().name().contains("LEATHER")) {
            count++;
        }
        if (inventory.getChestplate() != null &&
            inventory.getChestplate().getType().name().contains("LEATHER")) {
            count++;
        }
        if (inventory.getLeggings() != null &&
            inventory.getLeggings().getType().name().contains("LEATHER")) {
            count++;
        }
        if (inventory.getBoots() != null &&
            inventory.getBoots().getType().name().contains("LEATHER")) {
            count++;
        }

        return count;
    }

    /**
     * Empêche le changement de météo naturel de Minecraft
     * Le système de météo ZombieZ contrôle entièrement la météo
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!weatherManager.isEnabled()) {
            return;
        }

        // Annuler les changements de météo naturels
        // Le WeatherManager gère la météo Minecraft directement
        WeatherType currentType = weatherManager.getCurrentWeatherType();

        // Si la météo actuelle nécessite de la pluie, ne pas l'annuler
        if (currentType.isMinecraftRain() && event.toWeatherState()) {
            return;
        }

        // Si la météo actuelle ne nécessite pas de pluie, annuler la pluie
        if (!currentType.isMinecraftRain() && event.toWeatherState()) {
            event.setCancelled(true);
        }

        // Si la météo actuelle nécessite de la pluie, empêcher l'arrêt
        if (currentType.isMinecraftRain() && !event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    /**
     * Informe le joueur de la météo actuelle lors de la connexion
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Attendre un peu avant d'afficher la météo (laisser le temps au joueur de se connecter)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            WeatherType currentWeather = weatherManager.getCurrentWeatherType();

            // Ne pas notifier pour le temps clair
            if (currentWeather == WeatherType.CLEAR) {
                return;
            }

            // Envoyer les informations sur la météo actuelle
            player.sendMessage("");
            player.sendMessage(currentWeather.getColor() + "§l" + currentWeather.getIcon() +
                " MÉTÉO ACTUELLE: " + currentWeather.getDisplayName());

            if (currentWeather.isDangerous()) {
                player.sendMessage("§c⚠ Attention: Cette météo est dangereuse! Trouvez un abri!");
            }

            if (currentWeather.buffZombies()) {
                player.sendMessage("§c⚠ Les zombies sont renforcés!");
            }

            if (currentWeather.isBeneficial()) {
                player.sendMessage("§a✓ Conditions favorables aux survivants.");
            }

            if (currentWeather.debuffZombies()) {
                player.sendMessage("§a✓ Les zombies sont affaiblis!");
            }

            // Afficher les bonus actifs
            StringBuilder bonuses = new StringBuilder();
            if (currentWeather.getXpMultiplier() > 1.0) {
                bonuses.append("§e+").append((int)((currentWeather.getXpMultiplier()-1)*100)).append("%XP ");
            }
            if (currentWeather.getLootMultiplier() > 1.0) {
                bonuses.append("§b+").append((int)((currentWeather.getLootMultiplier()-1)*100)).append("%Loot ");
            }
            if (bonuses.length() > 0) {
                player.sendMessage("§aBonus: " + bonuses);
            }

            WeatherEffect effect = weatherManager.getCurrentWeather();
            if (effect != null) {
                player.sendMessage("§7Temps restant: §e" + effect.getRemainingTimeSeconds() + "s");
            }

            player.sendMessage("");

        }, 40L); // 2 secondes de délai
    }

    /**
     * Nettoie les données du joueur lors de la déconnexion
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Retirer le joueur des listes de la météo actuelle
        WeatherEffect effect = weatherManager.getCurrentWeather();
        if (effect != null) {
            effect.getAffectedPlayers();
            effect.getShelteredPlayers().remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Intercepte la commande /weather clear pour également arrêter la météo ZombieZ
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherCommand(PlayerCommandPreprocessEvent event) {
        if (!weatherManager.isEnabled()) {
            return;
        }

        String message = event.getMessage().toLowerCase().trim();

        // Vérifier si c'est une commande /weather clear
        if (message.startsWith("/weather clear") || message.startsWith("/weather ") && message.contains("clear")) {
            // Arrêter la météo ZombieZ
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                weatherManager.clearWeather();
                event.getPlayer().sendMessage("§a✓ Météo ZombieZ également réinitialisée.");
            });
        }
    }

    /**
     * Intercepte la commande /weather clear depuis la console
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerWeatherCommand(ServerCommandEvent event) {
        if (!weatherManager.isEnabled()) {
            return;
        }

        String command = event.getCommand().toLowerCase().trim();

        // Vérifier si c'est une commande weather clear
        if (command.startsWith("weather clear") || command.startsWith("weather ") && command.contains("clear")) {
            // Arrêter la météo ZombieZ
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                weatherManager.clearWeather();
                plugin.getServer().getConsoleSender().sendMessage("§a✓ Météo ZombieZ également réinitialisée.");
            });
        }
    }
}
