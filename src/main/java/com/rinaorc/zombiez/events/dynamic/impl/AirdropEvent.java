package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Événement Largage Aérien
 *
 * Déroulement:
 * 1. Un crate tombe du ciel avec des particules
 * 2. Le crate atterrit et devient un coffre
 * 3. Les joueurs doivent le défendre pendant l'ouverture (45s)
 * 4. Des zombies spawn autour pendant la défense
 * 5. Une fois ouvert, le coffre contient du loot rare
 */
public class AirdropEvent extends DynamicEvent {

    // État de l'événement
    private enum Phase {
        FALLING,    // Le crate tombe
        DEFENDING,  // Les joueurs défendent
        LOOTING,    // Le coffre est ouvert
        DONE        // Terminé
    }

    @Getter
    private Phase phase = Phase.FALLING;

    // Entités
    private FallingBlock fallingCrate;
    private ArmorStand crateMarker;
    private Location landingLocation;
    private Block chestBlock;

    // Défense
    private int defenseTimeRequired = 45; // Secondes pour ouvrir
    private int currentDefenseTime = 0;
    private int defendersNearby = 0;

    // Spawn de zombies pendant la défense
    private int zombiesSpawned = 0;
    private int maxZombies = 15;
    private int zombieSpawnInterval = 4; // Secondes

    // Loot
    private final List<ItemStack> lootItems = new ArrayList<>();
    private boolean lootGenerated = false;

    public AirdropEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.AIRDROP, location, zone);

        // Ajuster la durée basée sur la zone
        this.defenseTimeRequired = 45 + (zone.getId() * 2); // Plus long dans les zones avancées
        this.maxZombies = 10 + zone.getId(); // Plus de zombies dans les zones avancées
    }

    @Override
    protected void startMainLogic() {
        // Démarrer la chute du crate
        spawnFallingCrate();

        // Générer le loot à l'avance
        generateLoot();

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
        }.runTaskTimer(plugin, 0L, 20L); // Tick chaque seconde
    }

    /**
     * Fait tomber le crate depuis le ciel
     */
    private void spawnFallingCrate() {
        World world = location.getWorld();
        if (world == null) return;

        // Position de départ en hauteur
        Location spawnLoc = location.clone();
        spawnLoc.setY(world.getMaxHeight() - 10);

        // Créer un bloc qui tombe (chest)
        fallingCrate = world.spawnFallingBlock(spawnLoc, Material.CHEST.createBlockData());
        fallingCrate.setDropItem(false);
        fallingCrate.setGravity(true);
        fallingCrate.setVelocity(new Vector(0, -0.5, 0));
        fallingCrate.setCustomName("§b§l✈ LARGAGE AÉRIEN");
        fallingCrate.setCustomNameVisible(true);
        fallingCrate.setGlowing(true);

        // Effets de chute
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fallingCrate == null || fallingCrate.isDead() || !fallingCrate.isValid()) {
                    // Le crate a atterri
                    onCrateLanded();
                    cancel();
                    return;
                }

                // Particules de fumée pendant la chute
                Location crateLoc = fallingCrate.getLocation();
                world.spawnParticle(Particle.CLOUD, crateLoc, 5, 0.3, 0.3, 0.3, 0.02);
                world.spawnParticle(Particle.FIREWORK, crateLoc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.01);
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    /**
     * Appelé quand le crate atterrit
     */
    private void onCrateLanded() {
        World world = location.getWorld();
        if (world == null) return;

        // Déterminer la position d'atterrissage
        if (fallingCrate != null && fallingCrate.isValid()) {
            landingLocation = fallingCrate.getLocation().clone();
        } else {
            landingLocation = location.clone();
            landingLocation.setY(world.getHighestBlockYAt(location) + 1);
        }

        // Créer le coffre
        Block block = landingLocation.getBlock();
        block.setType(Material.CHEST);
        chestBlock = block;

        // Créer un marqueur visuel
        crateMarker = (ArmorStand) world.spawnEntity(landingLocation.clone().add(0.5, 1.5, 0.5), EntityType.ARMOR_STAND);
        crateMarker.setVisible(false);
        crateMarker.setGravity(false);
        crateMarker.setMarker(true);
        crateMarker.setCustomName("§b§l📦 LARGAGE - §e0%");
        crateMarker.setCustomNameVisible(true);

        // Effet d'atterrissage
        world.playSound(landingLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION, landingLocation, 3, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.DUST, landingLocation, 50, 1, 1, 1, 0,
            new Particle.DustOptions(Color.AQUA, 2));

        // Passer à la phase de défense
        phase = Phase.DEFENDING;

        // Annoncer aux joueurs proches
        for (Player player : world.getNearbyEntities(landingLocation, 80, 50, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§b§l📦 §7Le largage a atterri! Défendez-le pendant §e" + defenseTimeRequired + "s §7pour l'ouvrir!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
        }
    }

    @Override
    public void tick() {
        elapsedTicks += 20;

        switch (phase) {
            case FALLING -> tickFalling();
            case DEFENDING -> tickDefending();
            case LOOTING -> tickLooting();
            case DONE -> {} // Rien à faire
        }
    }

    private void tickFalling() {
        // La logique de chute est gérée par le runnable séparé
        // Ici on vérifie juste le timeout
        if (elapsedTicks > 20 * 30) { // 30 secondes max pour atterrir
            onCrateLanded();
        }
    }

    private void tickDefending() {
        World world = landingLocation.getWorld();
        if (world == null) return;

        // Compter les défenseurs à proximité
        defendersNearby = 0;
        for (Player player : world.getNearbyEntities(landingLocation, 15, 10, 15).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            defendersNearby++;
            addParticipant(player);
        }

        // Progression seulement si des joueurs sont présents
        if (defendersNearby > 0) {
            // Bonus de vitesse avec plusieurs défenseurs (cap à 2x)
            double speedMultiplier = Math.min(2.0, 1.0 + (defendersNearby - 1) * 0.25);
            currentDefenseTime += speedMultiplier;

            // Mise à jour du marqueur
            int percent = (int) ((double) currentDefenseTime / defenseTimeRequired * 100);
            if (crateMarker != null && crateMarker.isValid()) {
                String color = percent < 33 ? "§c" : (percent < 66 ? "§e" : "§a");
                crateMarker.setCustomName("§b§l📦 LARGAGE - " + color + percent + "%");
            }

            // Mise à jour de la boss bar
            double progress = (double) currentDefenseTime / defenseTimeRequired;
            updateBossBar(1.0 - progress, "- §e" + (defenseTimeRequired - currentDefenseTime) + "s");

            // Particules de progression
            world.spawnParticle(Particle.HAPPY_VILLAGER, landingLocation.clone().add(0.5, 1, 0.5),
                3, 0.5, 0.5, 0.5, 0);
        } else {
            // Régression lente si personne n'est là
            if (currentDefenseTime > 0) {
                currentDefenseTime = Math.max(0, currentDefenseTime - 1);
            }

            if (crateMarker != null && crateMarker.isValid()) {
                crateMarker.setCustomName("§c§l⚠ DÉFENSEURS REQUIS!");
            }
        }

        // Spawn de zombies périodiquement
        if (elapsedTicks % (zombieSpawnInterval * 20) == 0 && zombiesSpawned < maxZombies) {
            spawnDefenseZombies();
        }

        // Vérifier si la défense est complète
        if (currentDefenseTime >= defenseTimeRequired) {
            openCrate();
        }
    }

    private void tickLooting() {
        // Vérifier si le coffre a été vidé
        if (chestBlock != null && chestBlock.getState() instanceof Chest chest) {
            if (Arrays.stream(chest.getInventory().getContents())
                    .allMatch(item -> item == null || item.getType() == Material.AIR)) {
                complete();
            }
        }
    }

    /**
     * Spawn des zombies pour la défense
     */
    private void spawnDefenseZombies() {
        World world = landingLocation.getWorld();
        if (world == null) return;

        int count = 2 + plugin.getZoneManager().getZoneAt(landingLocation).getId() / 10;
        count = Math.min(count, maxZombies - zombiesSpawned);

        for (int i = 0; i < count; i++) {
            // Position aléatoire autour du crate
            double angle = Math.random() * Math.PI * 2;
            double distance = 10 + Math.random() * 10;
            double x = landingLocation.getX() + Math.cos(angle) * distance;
            double z = landingLocation.getZ() + Math.sin(angle) * distance;
            int y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);

            // Spawn un zombie via le système existant
            plugin.getSpawnSystem().spawnSingleZombie(spawnLoc, zone.getId());
            zombiesSpawned++;
        }

        // Annoncer
        for (Player player : world.getNearbyEntities(landingLocation, 30, 20, 30).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§c§l⚠ §7Des zombies approchent du largage!");
        }
    }

    /**
     * Ouvre le crate avec le loot
     */
    private void openCrate() {
        phase = Phase.LOOTING;

        World world = landingLocation.getWorld();
        if (world == null) return;

        // Remplir le coffre
        if (chestBlock != null && chestBlock.getState() instanceof Chest chest) {
            for (ItemStack item : lootItems) {
                chest.getInventory().addItem(item);
            }
        }

        // Effets
        world.playSound(landingLocation, Sound.BLOCK_CHEST_OPEN, 1.5f, 1f);
        world.playSound(landingLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, landingLocation.clone().add(0.5, 1.5, 0.5),
            50, 0.5, 1, 0.5, 0.1);

        // Mettre à jour le marqueur
        if (crateMarker != null && crateMarker.isValid()) {
            crateMarker.setCustomName("§a§l✓ OUVERT! Récupérez le loot!");
        }

        // Annoncer
        for (Player player : world.getNearbyEntities(landingLocation, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§a§l✓ COFFRE OUVERT!", "§7Récupérez le loot!", 10, 40, 20);
        }

        // Timer pour compléter automatiquement après 60s
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (phase == Phase.LOOTING) {
                complete();
            }
        }, 20L * 60);
    }

    /**
     * Génère le loot du crate
     */
    private void generateLoot() {
        if (lootGenerated) return;
        lootGenerated = true;

        // Nombre d'items basé sur la zone
        int itemCount = 3 + zone.getId() / 10;

        // Générer des items avec le système existant
        for (int i = 0; i < itemCount; i++) {
            // Rarité garantie plus élevée
            Rarity rarity = rollLootRarity();
            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item != null) {
                lootItems.add(item);
            }
        }

        // Ajouter des consommables bonus
        if (plugin.getConsumableManager() != null) {
            int consumableCount = 2 + zone.getId() / 15;
            for (int i = 0; i < consumableCount; i++) {
                ItemStack consumable = plugin.getConsumableManager().generateRandomConsumable(zone.getId());
                if (consumable != null) {
                    lootItems.add(consumable);
                }
            }
        }
    }

    private Rarity rollLootRarity() {
        double roll = Math.random() * 100;
        double zoneBonus = zone.getId() * 0.5; // Bonus de rareté par zone

        if (roll < 5 + zoneBonus) return Rarity.LEGENDARY;
        if (roll < 20 + zoneBonus) return Rarity.EPIC;
        if (roll < 45 + zoneBonus) return Rarity.RARE;
        if (roll < 75) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    @Override
    protected void onCleanup() {
        // Supprimer le marqueur
        if (crateMarker != null && crateMarker.isValid()) {
            crateMarker.remove();
        }

        // Supprimer le falling block si encore présent
        if (fallingCrate != null && fallingCrate.isValid()) {
            fallingCrate.remove();
        }

        // Ne pas supprimer le coffre - les joueurs peuvent encore le looter
        // Il disparaîtra naturellement ou sera cassé
    }

    @Override
    protected String getStartSubtitle() {
        return "Un ravitaillement tombe du ciel!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Phase: %s | Defense: %d/%d | Defenders: %d | Zombies: %d/%d",
            phase, currentDefenseTime, defenseTimeRequired, defendersNearby, zombiesSpawned, maxZombies);
    }
}
