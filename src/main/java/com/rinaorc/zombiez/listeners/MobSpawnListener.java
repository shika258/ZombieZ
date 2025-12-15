package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * Listener pour contrôler le spawn des mobs
 * Bloque tous les spawns vanilla pour ne garder que les mobs customs du plugin
 */
public class MobSpawnListener implements Listener {

    private final ZombieZPlugin plugin;

    public MobSpawnListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Bloque le spawn de tous les mobs hostiles vanilla
     * Seuls les mobs spawnés par le plugin (via CUSTOM ou SPAWNER_EGG) sont autorisés
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        SpawnReason reason = event.getSpawnReason();

        // Toujours autoriser les spawns custom du plugin
        if (reason == SpawnReason.CUSTOM) {
            return;
        }

        // Autoriser les spawns par commande (pour les admins)
        if (reason == SpawnReason.COMMAND) {
            return;
        }

        // Autoriser les spawns par oeuf (pour les admins)
        if (reason == SpawnReason.SPAWNER_EGG) {
            return;
        }

        // Autoriser les entités avec le tag ZombieZ (spawned par le plugin)
        if (entity.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("zombiez_"))) {
            return;
        }

        // Bloquer tous les mobs hostiles
        if (isHostileMob(entity)) {
            event.setCancelled(true);
            return;
        }

        // Bloquer les animaux neutres qui peuvent devenir hostiles
        if (isNeutralHostileMob(entity)) {
            event.setCancelled(true);
            return;
        }

        // Autoriser les animaux passifs (poules, vaches, etc.) uniquement pour l'ambiance
        // Mais on peut aussi les bloquer si vous voulez un monde 100% custom
        if (isPassiveMob(entity)) {
            // Décommentez la ligne suivante pour bloquer aussi les animaux passifs
            // event.setCancelled(true);
            return;
        }
    }

    /**
     * Vérifie si l'entité est un mob hostile
     */
    private boolean isHostileMob(Entity entity) {
        return entity instanceof Monster ||
               entity instanceof Slime ||
               entity instanceof Ghast ||
               entity instanceof Phantom ||
               entity instanceof Shulker ||
               entity instanceof Hoglin ||
               entity instanceof Piglin ||
               entity instanceof PiglinBrute ||
               entity instanceof Warden ||
               entity instanceof Wither ||
               entity instanceof EnderDragon ||
               entity instanceof Guardian ||
               entity instanceof ElderGuardian ||
               entity instanceof Ravager ||
               entity instanceof Vex ||
               entity instanceof Evoker ||
               entity instanceof Vindicator ||
               entity instanceof Pillager ||
               entity instanceof Witch ||
               entity instanceof Illusioner;
    }

    /**
     * Vérifie si l'entité est un mob neutre qui peut devenir hostile
     */
    private boolean isNeutralHostileMob(Entity entity) {
        return entity instanceof Wolf ||
               entity instanceof Bee ||
               entity instanceof Goat ||
               entity instanceof PolarBear ||
               entity instanceof Dolphin ||
               entity instanceof IronGolem ||
               entity instanceof Llama ||
               entity instanceof Panda ||
               entity instanceof PigZombie; // Zombified Piglin
    }

    /**
     * Vérifie si l'entité est un mob passif
     */
    private boolean isPassiveMob(Entity entity) {
        return entity instanceof Animals ||
               entity instanceof Villager ||
               entity instanceof WanderingTrader ||
               entity instanceof Bat ||
               entity instanceof Squid ||
               entity instanceof GlowSquid ||
               entity instanceof Fish ||
               entity instanceof Axolotl ||
               entity instanceof Allay ||
               entity instanceof Frog ||
               entity instanceof Tadpole ||
               entity instanceof Sniffer ||
               entity instanceof Camel;
    }
}
