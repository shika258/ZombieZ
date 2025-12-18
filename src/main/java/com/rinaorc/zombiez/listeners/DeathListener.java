package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.managers.EconomyManager;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener pour la gestion des morts et respawns
 * Gère les pénalités de mort et le système de checkpoint
 */
public class DeathListener implements Listener {

    private final ZombieZPlugin plugin;

    // Pénalité de points à la mort (pourcentage)
    private static final double DEATH_POINT_PENALTY = 0.10; // 10%

    // Points minimum préservés
    private static final long MIN_POINTS_PRESERVED = 100;

    // Durée d'invulnérabilité au respawn (en ticks) - 5 secondes
    private static final int RESPAWN_INVULNERABILITY_TICKS = 100;

    public DeathListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère la mort d'un joueur
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        
        if (data == null) return;

        // Enregistrer la mort
        data.addDeath();

        // Obtenir la cause de la mort
        String deathCause = getDeathCause(player);
        Zone deathZone = plugin.getZoneManager().getPlayerZone(player);

        // Personnaliser le message de mort
        event.deathMessage(null); // Supprimer le message par défaut
        
        String zoneInfo = deathZone != null ? deathZone.getColoredName() : "§7Zone inconnue";
        MessageUtils.broadcast("§c☠ §7" + player.getName() + " §cest mort §7(" + deathCause + " - " + zoneInfo + "§7)");

        // Appliquer la pénalité de points
        applyDeathPenalty(player, data);

        // Log pour debug
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Mort de " + player.getName() + " en zone " + 
                (deathZone != null ? deathZone.getId() : "?") + " - Cause: " + deathCause);
        }
    }

    /**
     * Obtient la cause de la mort formatée
     */
    private String getDeathCause(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        
        if (lastDamage == null) {
            return "§7Mort mystérieuse";
        }

        // Si tué par une entité
        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            var damager = entityEvent.getDamager();

            // Vérifier si c'est un mob ZombieZ (zombie, squelette, etc.)
            if (damager instanceof LivingEntity livingDamager && damager.hasMetadata("zombiez_type")) {
                String mobType = damager.getMetadata("zombiez_type").get(0).asString();
                String mobName = livingDamager.getCustomName();
                if (mobName != null) {
                    // Extraire le nom sans les couleurs et stats
                    return "§c" + mobName.split(" §7")[0].replaceAll("§.", "");
                }
                return "§c" + mobType;
            }

            if (damager instanceof Player killer) {
                return "§c" + killer.getName();
            }

            return "§c" + damager.getType().name();
        }

        // Autres causes
        return switch (lastDamage.getCause()) {
            case FALL -> "§eChute";
            case DROWNING -> "§9Noyade";
            case FIRE, FIRE_TICK, LAVA -> "§6Feu";
            case POISON -> "§2Poison";
            case STARVATION -> "§eFamine";
            case SUFFOCATION -> "§8Suffocation";
            case VOID -> "§0Vide";
            case WITHER -> "§8Wither";
            case FREEZE -> "§bHypothermie";
            case MAGIC -> "§dMagie";
            default -> "§7" + lastDamage.getCause().name();
        };
    }

    /**
     * Applique la pénalité de mort (perte de points)
     */
    private void applyDeathPenalty(Player player, PlayerData data) {
        long currentPoints = data.getPoints().get();
        
        if (currentPoints <= MIN_POINTS_PRESERVED) {
            // Pas de pénalité si déjà au minimum
            return;
        }

        // Calculer la pénalité
        long penalty = (long) (currentPoints * DEATH_POINT_PENALTY);
        long newPoints = Math.max(MIN_POINTS_PRESERVED, currentPoints - penalty);
        long actualPenalty = currentPoints - newPoints;

        if (actualPenalty > 0) {
            data.getPoints().set(newPoints);
            data.markDirty();
            
            // Notifier le joueur
            MessageUtils.send(player, "§c-" + EconomyManager.formatPoints(actualPenalty) + 
                " Points §7(Pénalité de mort)");
        }
    }

    /**
     * Gère le respawn du joueur
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        
        if (data == null) return;

        // Déterminer le point de respawn
        Location respawnLocation = getRespawnLocation(player, data);
        
        if (respawnLocation != null) {
            event.setRespawnLocation(respawnLocation);
        }

        // Appliquer l'invulnérabilité au respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Résistance 255 = invulnérabilité totale
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    RESPAWN_INVULNERABILITY_TICKS,
                    255, // Niveau max = invincible
                    false,
                    false,
                    true // Afficher l'icône
                ));

                int checkpoint = data.getCurrentCheckpoint().get();

                if (checkpoint > 0) {
                    MessageUtils.sendTitle(player, "§c§lMORT", "§7Respawn au checkpoint " + checkpoint, 10, 40, 10);
                } else {
                    MessageUtils.sendTitle(player, "§c§lMORT", "§7Respawn au spawn", 10, 40, 10);
                }

                // Info invulnérabilité
                MessageUtils.send(player, "§a✦ Invulnérable pendant 5 secondes!");
                MessageUtils.send(player, "§7Conseil: Active des §echeckpoints §7dans les refuges pour respawn plus près!");
            }
        }, 1L); // 1 tick après le respawn pour s'assurer que le joueur est bien spawn
    }

    /**
     * Détermine la location de respawn selon le checkpoint actif
     */
    private Location getRespawnLocation(Player player, PlayerData data) {
        int checkpointId = data.getCurrentCheckpoint().get();
        
        if (checkpointId > 0) {
            // Chercher le refuge correspondant au checkpoint
            Zone zone = plugin.getZoneManager().getZoneById(checkpointId);
            
            if (zone != null && zone.getRefugeLocation() != null) {
                return zone.getRefugeLocation();
            }
        }

        // Respawn par défaut au spawn
        Zone spawnZone = plugin.getZoneManager().getSpawnZone();
        if (spawnZone != null && spawnZone.getRefugeLocation() != null) {
            return spawnZone.getRefugeLocation();
        }

        // Fallback au spawn du monde
        return player.getWorld().getSpawnLocation();
    }
}
