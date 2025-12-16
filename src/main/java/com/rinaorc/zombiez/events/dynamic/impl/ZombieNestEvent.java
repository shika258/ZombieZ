package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Événement Nid de Zombies
 *
 * Déroulement:
 * 1. Un nid de zombies apparaît (bloc de spawner visuel)
 * 2. Le nid spawn des zombies de plus en plus vite
 * 3. Les joueurs doivent détruire le nid (frapper dessus)
 * 4. Le nid a une barre de vie
 * 5. Récompenses à la destruction
 */
public class ZombieNestEvent extends DynamicEvent {

    // État du nid
    @Getter
    private double nestHealth;
    private double maxNestHealth;
    private int zombiesSpawned = 0;
    private int maxTotalZombies = 50;

    // Spawn rate (augmente avec le temps)
    private int currentSpawnInterval = 8; // Secondes entre spawns
    private int minSpawnInterval = 2;     // Intervalle minimum
    private int zombiesPerSpawn = 2;      // Zombies par spawn

    // Entités visuelles
    private Block nestBlock;
    private ArmorStand healthMarker;
    private final List<Block> originalBlocks = new ArrayList<>();
    private final Map<Location, BlockData> savedBlocks = new HashMap<>();

    // Particules
    private int tickCounter = 0;

    public ZombieNestEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.ZOMBIE_NEST, location, zone);

        // Vie basée sur la zone
        this.maxNestHealth = 100 + (zone.getId() * 20);
        this.nestHealth = maxNestHealth;

        // Plus de zombies dans les zones avancées
        this.maxTotalZombies = 30 + zone.getId();
        this.zombiesPerSpawn = 2 + zone.getId() / 15;
    }

    @Override
    protected void startMainLogic() {
        // Créer le nid
        createNest();

        // Démarrer la logique principale
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Crée le nid visuel
     */
    private void createNest() {
        World world = location.getWorld();
        if (world == null) return;

        // Trouver le sol
        int y = world.getHighestBlockYAt(location);
        Location nestLoc = new Location(world, location.getX(), y + 1, location.getZ());

        // Sauvegarder les blocs originaux
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    Location blockLoc = nestLoc.clone().add(dx, dy, dz);
                    Block block = blockLoc.getBlock();
                    savedBlocks.put(blockLoc.clone(), block.getBlockData().clone());
                }
            }
        }

        // Créer la structure du nid (forme organique)
        nestBlock = nestLoc.getBlock();
        nestBlock.setType(Material.SPAWNER);

        // Blocs décoratifs autour
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Block decorBlock = nestLoc.clone().add(dx, 0, dz).getBlock();
                if (decorBlock.getType().isAir()) {
                    decorBlock.setType(Material.SCULK);
                    originalBlocks.add(decorBlock);
                }
            }
        }

        // Veines de sculk
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 / 8) * i;
            int dx = (int) Math.round(Math.cos(angle) * 2);
            int dz = (int) Math.round(Math.sin(angle) * 2);
            Block veinBlock = nestLoc.clone().add(dx, 0, dz).getBlock();
            if (veinBlock.getType().isAir() || veinBlock.getType() == Material.GRASS_BLOCK) {
                veinBlock.setType(Material.SCULK_VEIN);
                originalBlocks.add(veinBlock);
            }
        }

        // Marqueur de santé
        healthMarker = (ArmorStand) world.spawnEntity(nestLoc.clone().add(0.5, 2, 0.5), EntityType.ARMOR_STAND);
        healthMarker.setVisible(false);
        healthMarker.setGravity(false);
        healthMarker.setMarker(true);
        healthMarker.setCustomName(getHealthDisplay());
        healthMarker.setCustomNameVisible(true);

        // Effet de spawn
        world.playSound(nestLoc, Sound.ENTITY_WARDEN_EMERGE, 1.5f, 0.8f);
        world.spawnParticle(Particle.SCULK_CHARGE_POP, nestLoc.clone().add(0.5, 1, 0.5),
            30, 1, 1, 1, 0.1);
    }

    @Override
    public void tick() {
        tickCounter++;
        elapsedTicks += 20;

        World world = location.getWorld();
        if (world == null) return;

        // Vérifier si le nid existe encore
        if (nestBlock == null || nestBlock.getType() != Material.SPAWNER) {
            // Le nid a été cassé par d'autres moyens
            complete();
            return;
        }

        // Spawn de zombies
        if (tickCounter % currentSpawnInterval == 0 && zombiesSpawned < maxTotalZombies) {
            spawnNestZombies();

            // Accélérer le spawn au fil du temps
            if (currentSpawnInterval > minSpawnInterval) {
                currentSpawnInterval--;
            }
        }

        // Particules ambiantes
        if (tickCounter % 2 == 0) {
            Location particleLoc = nestBlock.getLocation().clone().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.SOUL, particleLoc, 3, 0.5, 0.5, 0.5, 0.02);
            world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 2, 0.3, 0.3, 0.3, 0.01);
        }

        // Son ambient
        if (tickCounter % 5 == 0) {
            world.playSound(nestBlock.getLocation(), Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.8f);
        }

        // Mettre à jour la boss bar
        double healthPercent = nestHealth / maxNestHealth;
        updateBossBar(healthPercent, "- §c" + (int) nestHealth + "/" + (int) maxNestHealth + " ❤");
    }

    /**
     * Spawn des zombies depuis le nid
     */
    private void spawnNestZombies() {
        World world = location.getWorld();
        if (world == null) return;

        Location nestLoc = nestBlock.getLocation();

        for (int i = 0; i < zombiesPerSpawn; i++) {
            if (zombiesSpawned >= maxTotalZombies) break;

            // Position autour du nid
            double angle = Math.random() * Math.PI * 2;
            double distance = 2 + Math.random() * 4;
            double x = nestLoc.getX() + Math.cos(angle) * distance;
            double z = nestLoc.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);

            // Effet de spawn
            world.spawnParticle(Particle.SCULK_CHARGE_POP, spawnLoc, 10, 0.3, 0.5, 0.3, 0.05);
            world.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 0.8f, 0.9f);

            // Spawn le zombie
            plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, zone.getId());
            zombiesSpawned++;
        }

        // Message aux joueurs proches
        if (zombiesSpawned % 10 == 0) {
            for (Player player : world.getNearbyEntities(nestLoc, 50, 30, 50).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList()) {
                player.sendMessage("§c§l⚠ §7Le nid pulse! §e" + zombiesSpawned + "/" + maxTotalZombies + " §7zombies générés!");
            }
        }
    }

    /**
     * Inflige des dégâts au nid
     * Appelé par le listener quand un joueur frappe le spawner
     */
    public void damageNest(Player player, double damage) {
        if (!active) return;

        addParticipant(player);
        nestHealth -= damage;

        // Mettre à jour l'affichage
        if (healthMarker != null && healthMarker.isValid()) {
            healthMarker.setCustomName(getHealthDisplay());
        }

        World world = location.getWorld();
        if (world != null) {
            // Effet de dégât
            world.spawnParticle(Particle.DAMAGE_INDICATOR, nestBlock.getLocation().clone().add(0.5, 0.5, 0.5),
                3, 0.3, 0.3, 0.3, 0);
            world.playSound(nestBlock.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.2f);

            // Spawn de représailles
            if (Math.random() < 0.3) { // 30% de chance de spawn en représailles
                spawnNestZombies();
            }
        }

        // Vérifier la mort
        if (nestHealth <= 0) {
            destroyNest();
        }
    }

    /**
     * Détruit le nid
     */
    private void destroyNest() {
        World world = location.getWorld();
        if (world == null) return;

        Location nestLoc = nestBlock.getLocation();

        // Effet de destruction
        world.playSound(nestLoc, Sound.ENTITY_WARDEN_DEATH, 1.5f, 1f);
        world.playSound(nestLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
        world.spawnParticle(Particle.EXPLOSION, nestLoc.clone().add(0.5, 0.5, 0.5), 5, 1, 1, 1, 0);
        world.spawnParticle(Particle.SCULK_SOUL, nestLoc.clone().add(0.5, 0.5, 0.5), 50, 2, 2, 2, 0.1);

        // Supprimer le nid
        nestBlock.setType(Material.AIR);

        // Supprimer les blocs décoratifs
        for (Block block : originalBlocks) {
            if (block != null) {
                block.setType(Material.AIR);
            }
        }

        complete();
    }

    /**
     * Obtient l'affichage de la santé
     */
    private String getHealthDisplay() {
        double percent = (nestHealth / maxNestHealth) * 100;
        String color = percent > 66 ? "§a" : (percent > 33 ? "§e" : "§c");

        // Barre de vie visuelle
        int bars = 20;
        int filled = (int) (percent / 100 * bars);
        StringBuilder healthBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            healthBar.append(i < filled ? "§a|" : "§7|");
        }

        return "§c§l🪺 NID " + color + (int) percent + "% " + healthBar;
    }

    /**
     * Obtient le bloc du nid (pour le listener)
     */
    public Block getNestBlock() {
        return nestBlock;
    }

    @Override
    protected void onCleanup() {
        // Supprimer le marqueur
        if (healthMarker != null && healthMarker.isValid()) {
            healthMarker.remove();
        }

        // Restaurer les blocs originaux si l'événement échoue
        if (failed) {
            for (Map.Entry<Location, BlockData> entry : savedBlocks.entrySet()) {
                entry.getKey().getBlock().setBlockData(entry.getValue());
            }
        } else {
            // Supprimer les blocs du nid
            if (nestBlock != null) {
                nestBlock.setType(Material.AIR);
            }
            for (Block block : originalBlocks) {
                if (block != null) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    @Override
    protected String getStartSubtitle() {
        return "Détruisez le nid avant qu'il ne soit trop tard!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Health: %.0f/%.0f | Zombies: %d/%d | SpawnInterval: %ds",
            nestHealth, maxNestHealth, zombiesSpawned, maxTotalZombies, currentSpawnInterval);
    }
}
