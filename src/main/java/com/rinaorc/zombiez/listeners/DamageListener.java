package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener pour réduire le temps d'invincibilité (noDamageTicks) des mobs.
 *
 * Par défaut, Minecraft donne 10 ticks (0.5s) d'invincibilité après chaque coup.
 * Ce listener réduit ce temps à 2 ticks (0.1s) pour les mobs uniquement.
 */
public class DamageListener implements Listener {

    private final ZombieZPlugin plugin;

    // Temps d'invincibilité réduit (2 ticks = 0.1 seconde)
    private static final int REDUCED_NO_DAMAGE_TICKS = 2;

    public DamageListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Seulement pour les LivingEntity (pas les ArmorStands, etc.)
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        // Ne pas modifier pour les joueurs (garder l'invincibilité standard)
        if (living instanceof Player) {
            return;
        }

        // Le serveur définit noDamageTicks APRÈS cet événement,
        // donc on utilise runTaskLater pour écraser la valeur
        new BukkitRunnable() {
            @Override
            public void run() {
                if (living.isValid() && !living.isDead()) {
                    living.setNoDamageTicks(REDUCED_NO_DAMAGE_TICKS);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
