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

    // Anti-farming
    private int maxEventDuration = 180;   // 3 minutes max avant explosion du nid
    private int lastDamageTime = 0;       // Dernier moment où le nid a été endommagé
    private int inactivityTimeout = 30;   // Si pas de dégâts pendant 30 secondes, le nid se régénère
    private boolean isUnstable = false;   // Le nid devient instable avant d'exploser
    private int unstableTimer = 0;

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

        // Anti-farming: Vérifier le timeout global
        int elapsedSeconds = elapsedTicks / 20;
        if (elapsedSeconds >= maxEventDuration) {
            onNestOverload();
            return;
        }

        // Anti-farming: Si max zombies atteint, le nid devient instable
        if (zombiesSpawned >= maxTotalZombies && !isUnstable) {
            triggerUnstable();
        }

        // Anti-farming: Régénération si pas de dégâts récents
        int timeSinceLastDamage = tickCounter - lastDamageTime;
        if (timeSinceLastDamage >= inactivityTimeout && nestHealth < maxNestHealth) {
            // Régénérer 5% de vie par seconde
            double regenAmount = maxNestHealth * 0.05;
            nestHealth = Math.min(maxNestHealth, nestHealth + regenAmount);

            // Avertir les joueurs
            if (timeSinceLastDamage == inactivityTimeout) {
                for (Player player : world.getNearbyEntities(nestBlock.getLocation(), 50, 30, 50).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    player.sendMessage("§c§l⚠ §7Le nid se régénère! Continuez à l'attaquer!");
                }
            }

            // Mettre à jour l'affichage
            if (healthMarker != null && healthMarker.isValid()) {
                healthMarker.setCustomName(getHealthDisplay() + " §d[REGEN]");
            }
        }

        // Mode instable: compte à rebours avant explosion
        if (isUnstable) {
            unstableTimer++;

            // Effets visuels instables
            if (unstableTimer % 2 == 0) {
                world.spawnParticle(Particle.LAVA, nestBlock.getLocation().clone().add(0.5, 1, 0.5),
                    5, 0.5, 0.5, 0.5, 0.02);
                world.playSound(nestBlock.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 2f);
            }

            // Explosion après 30 secondes instables
            if (unstableTimer >= 30) {
                onNestOverload();
                return;
            }

            // Avertissements
            if (unstableTimer == 10 || unstableTimer == 20 || unstableTimer == 25) {
                int remaining = 30 - unstableTimer;
                for (Player player : world.getNearbyEntities(nestBlock.getLocation(), 60, 40, 60).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .toList()) {
                    player.sendMessage("§c§l⚠ §7Le nid est instable! Explosion dans §c" + remaining + "s§7!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                }
            }
        }

        // Spawn de zombies (seulement si pas déjà au max)
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
            if (isUnstable) {
                world.spawnParticle(Particle.FLAME, particleLoc, 5, 0.5, 0.5, 0.5, 0.05);
            } else {
                world.spawnParticle(Particle.SOUL, particleLoc, 3, 0.5, 0.5, 0.5, 0.02);
            }
            world.spawnParticle(Particle.SCULK_SOUL, particleLoc, 2, 0.3, 0.3, 0.3, 0.01);
        }

        // Son ambient
        if (tickCounter % 5 == 0) {
            Sound ambientSound = isUnstable ? Sound.BLOCK_RESPAWN_ANCHOR_CHARGE : Sound.BLOCK_SCULK_SPREAD;
            world.playSound(nestBlock.getLocation(), ambientSound, 0.5f, isUnstable ? 1.5f : 0.8f);
        }

        // Mettre à jour la boss bar
        double healthPercent = nestHealth / maxNestHealth;
        int timeRemaining = maxEventDuration - elapsedSeconds;
        String timerColor = timeRemaining > 60 ? "§a" : (timeRemaining > 30 ? "§e" : "§c");
        String unstableText = isUnstable ? " §4[INSTABLE]" : "";
        updateBossBar(healthPercent, "- §c" + (int) nestHealth + "/" + (int) maxNestHealth +
            " ❤ §7| " + timerColor + timeRemaining + "s" + unstableText);
    }

    /**
     * Déclenche le mode instable du nid
     */
    private void triggerUnstable() {
        isUnstable = true;
        unstableTimer = 0;

        World world = location.getWorld();
        if (world == null) return;

        for (Player player : world.getNearbyEntities(nestBlock.getLocation(), 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§c§l⚠ NID INSTABLE!", "§7Détruisez-le ou éloignez-vous!", 10, 40, 10);
            player.sendMessage("§c§l⚠ §7Le nid a généré trop de zombies et devient §cinstable§7!");
            player.sendMessage("§7Détruisez-le dans les §e30 secondes §7ou il explosera!");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);
        }

        world.playSound(nestBlock.getLocation(), Sound.ENTITY_WARDEN_ANGRY, 2f, 0.5f);
    }

    /**
     * Appelé quand le nid surcharge/explose (échec de l'événement)
     */
    private void onNestOverload() {
        World world = location.getWorld();
        if (world == null) {
            fail();
            return;
        }

        Location nestLoc = nestBlock.getLocation();

        // Grosse explosion
        world.playSound(nestLoc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        world.playSound(nestLoc, Sound.ENTITY_WITHER_DEATH, 2f, 0.8f);
        world.createExplosion(nestLoc.clone().add(0.5, 0.5, 0.5), 0f, false, false); // Explosion visuelle sans dégâts
        world.spawnParticle(Particle.EXPLOSION_EMITTER, nestLoc.clone().add(0.5, 0.5, 0.5), 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.SCULK_SOUL, nestLoc.clone().add(0.5, 0.5, 0.5), 100, 3, 3, 3, 0.2);

        // Message d'échec
        for (Player player : world.getNearbyEntities(nestLoc, 100, 50, 100).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§c§l✗ NID EXPLOSÉ!", "§7Le nid s'est autodétruit...", 10, 60, 20);
            player.sendMessage("§c§l✗ §7Le nid de zombies a explosé! Aucune récompense.");
        }

        // Supprimer le nid
        nestBlock.setType(Material.AIR);
        for (Block block : originalBlocks) {
            if (block != null) {
                block.setType(Material.AIR);
            }
        }

        fail();
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

        // Anti-farming: Réinitialiser le timer de dégâts
        lastDamageTime = tickCounter;

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
