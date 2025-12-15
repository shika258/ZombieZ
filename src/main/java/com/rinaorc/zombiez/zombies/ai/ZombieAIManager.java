package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire central pour toutes les IA de zombies
 * Gère le tick des IA et leur cycle de vie
 */
public class ZombieAIManager {

    private final ZombieZPlugin plugin;

    // Map des IA actives par UUID de zombie
    @Getter
    private final Map<UUID, ZombieAI> activeAIs;

    // Statistiques
    private long totalAIsCreated = 0;
    private long totalAIsRemoved = 0;

    public ZombieAIManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.activeAIs = new ConcurrentHashMap<>();

        startAITickTask();
        startCleanupTask();

        plugin.log(Level.INFO, "§a✓ ZombieAIManager initialisé");
    }

    /**
     * Crée et enregistre une IA pour un zombie
     */
    public ZombieAI createAI(Zombie zombie, ZombieType type, int level) {
        ZombieAI ai = createAIForType(zombie, type, level);

        if (ai != null) {
            activeAIs.put(zombie.getUniqueId(), ai);
            totalAIsCreated++;
        }

        return ai;
    }

    /**
     * Crée l'IA appropriée selon le type de zombie
     */
    private ZombieAI createAIForType(Zombie zombie, ZombieType type, int level) {
        return switch (type.getCategory()) {
            case BASIC -> new BasicZombieAI(plugin, zombie, type, level);
            case TANK -> new TankZombieAI(plugin, zombie, type, level);
            case MELEE -> new MeleeZombieAI(plugin, zombie, type, level);
            case RANGED -> new RangedZombieAI(plugin, zombie, type, level);
            case SUPPORT -> new SupportZombieAI(plugin, zombie, type, level);
            case STEALTH -> new StealthZombieAI(plugin, zombie, type, level);
            case EXPLOSIVE -> new ExplosiveZombieAI(plugin, zombie, type, level);
            case HAZARD -> new HazardZombieAI(plugin, zombie, type, level);
            case SUMMONER -> new SummonerZombieAI(plugin, zombie, type, level);
            case ELEMENTAL -> new ElementalZombieAI(plugin, zombie, type, level);
            case SPECIAL, ELITE -> new EliteZombieAI(plugin, zombie, type, level);
            case MINIBOSS, ZONE_BOSS, FINAL_BOSS -> new BossZombieAI(plugin, zombie, type, level);
        };
    }

    /**
     * Obtient l'IA d'un zombie
     */
    public ZombieAI getAI(UUID zombieId) {
        return activeAIs.get(zombieId);
    }

    /**
     * Obtient l'IA d'un zombie depuis l'entité
     */
    public ZombieAI getAI(Entity entity) {
        return activeAIs.get(entity.getUniqueId());
    }

    /**
     * Supprime l'IA d'un zombie
     */
    public void removeAI(UUID zombieId) {
        ZombieAI removed = activeAIs.remove(zombieId);
        if (removed != null) {
            totalAIsRemoved++;
        }
    }

    /**
     * Notifie une attaque de zombie
     */
    public void onZombieAttack(UUID zombieId, Player target) {
        ZombieAI ai = activeAIs.get(zombieId);
        if (ai != null) {
            try {
                ai.onAttack(target);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur dans onAttack pour " + zombieId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Notifie des dégâts sur un zombie
     */
    public void onZombieDamaged(UUID zombieId, Entity attacker, double damage) {
        ZombieAI ai = activeAIs.get(zombieId);
        if (ai != null) {
            try {
                ai.onDamaged(attacker, damage);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur dans onDamaged pour " + zombieId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Notifie la mort d'un zombie
     */
    public void onZombieDeath(UUID zombieId, Player killer) {
        ZombieAI ai = activeAIs.get(zombieId);
        if (ai != null) {
            try {
                ai.onDeath(killer);
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur dans onDeath pour " + zombieId + ": " + e.getMessage());
            }
        }
        removeAI(zombieId);
    }

    /**
     * Démarre la tâche de tick des IA
     */
    private void startAITickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tickAllAIs();
            }
        }.runTaskTimer(plugin, 10L, 5L); // Tick toutes les 5 ticks (0.25 seconde)
    }

    /**
     * Tick toutes les IA actives
     */
    private void tickAllAIs() {
        for (Map.Entry<UUID, ZombieAI> entry : activeAIs.entrySet()) {
            UUID zombieId = entry.getKey();
            ZombieAI ai = entry.getValue();

            // Vérifier si le zombie existe toujours
            Entity entity = plugin.getServer().getEntity(zombieId);
            if (entity == null || !entity.isValid() || entity.isDead()) {
                activeAIs.remove(zombieId);
                totalAIsRemoved++;
                continue;
            }

            // Tick l'IA
            try {
                ai.tick();
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur dans tick pour " + zombieId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Démarre la tâche de nettoyage
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimer(plugin, 600L, 600L); // Toutes les 30 secondes
    }

    /**
     * Nettoie les IA orphelines
     */
    private void cleanup() {
        int removed = 0;
        for (UUID zombieId : activeAIs.keySet()) {
            Entity entity = plugin.getServer().getEntity(zombieId);
            if (entity == null || !entity.isValid() || entity.isDead()) {
                activeAIs.remove(zombieId);
                removed++;
            }
        }

        if (removed > 0) {
            totalAIsRemoved += removed;
            plugin.log(Level.FINE, "Nettoyé " + removed + " IA orphelines");
        }
    }

    /**
     * Obtient le nombre d'IA actives
     */
    public int getActiveAICount() {
        return activeAIs.size();
    }

    /**
     * Obtient les statistiques
     */
    public String getStats() {
        return String.format("Active: %d | Created: %d | Removed: %d",
            activeAIs.size(), totalAIsCreated, totalAIsRemoved);
    }

    /**
     * Arrête toutes les IA
     */
    public void shutdown() {
        activeAIs.clear();
        plugin.log(Level.INFO, "ZombieAIManager arrêté");
    }
}
