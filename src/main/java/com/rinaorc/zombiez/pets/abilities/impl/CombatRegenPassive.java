package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passif de la Luciole Errante - Guérisseuse de Combat
 * Régénère de la vie après chaque kill pendant une durée limitée
 */
@Getter
public class CombatRegenPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double regenPerSecond; // En demi-coeurs (0.5 = 0.25 coeur)
    private final int durationSeconds;

    // Tracking des régénérations actives
    private static final Map<UUID, BukkitRunnable> activeRegens = new ConcurrentHashMap<>();

    public CombatRegenPassive(String id, String name, String desc, double regenPerSec, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.regenPerSecond = regenPerSec;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        // Annuler la régénération précédente si elle existe (refresh)
        BukkitRunnable existing = activeRegens.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        // Calculer la régénération avec le multiplicateur de stats du pet
        double actualRegen = regenPerSecond * petData.getStatMultiplier();

        // Démarrer la nouvelle régénération
        BukkitRunnable regenTask = new BukkitRunnable() {
            int ticksRemaining = durationSeconds * 20;

            @Override
            public void run() {
                if (ticksRemaining <= 0 || !player.isOnline()) {
                    activeRegens.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Régénérer chaque seconde (20 ticks)
                if (ticksRemaining % 20 == 0) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                    if (currentHealth < maxHealth) {
                        double newHealth = Math.min(currentHealth + actualRegen, maxHealth);
                        player.setHealth(newHealth);

                        // Particules de soin
                        player.getWorld().spawnParticle(Particle.HEART,
                            player.getLocation().add(0, 1.5, 0),
                            2, 0.3, 0.3, 0.3, 0);
                    }
                }

                ticksRemaining--;
            }
        };

        regenTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
        activeRegens.put(player.getUniqueId(), regenTask);

        // Feedback visuel au kill
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
    }

    /**
     * Nettoie les données d'un joueur déconnecté
     */
    public static void cleanupPlayer(UUID playerId) {
        BukkitRunnable task = activeRegens.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}
