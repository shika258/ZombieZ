package com.rinaorc.zombiez.events.dynamic.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.events.dynamic.DynamicEvent;
import com.rinaorc.zombiez.events.dynamic.DynamicEventType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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
    private ArmorStand fallingCrate;
    private TextDisplay crateMarker;
    private Location landingLocation;
    private Block chestBlock;

    // Tâches planifiées (pour cleanup)
    private BukkitTask fallingTask;
    private BukkitTask lootingTimeoutTask;

    // Défense - OPTIMISÉ: utiliser double pour précision
    private int defenseTimeRequired = 45; // Secondes pour ouvrir
    private double currentDefenseTime = 0; // Double pour précision avec multiplicateur
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
     * Utilise un ArmorStand avec un item chest pour éviter le problème de double coffre
     */
    private void spawnFallingCrate() {
        World world = location.getWorld();
        if (world == null) return;

        // Position de départ en hauteur
        Location spawnLoc = location.clone();
        spawnLoc.setY(world.getMaxHeight() - 10);

        // Créer un ArmorStand invisible avec un coffre sur la tête (évite le placement automatique de bloc)
        fallingCrate = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        fallingCrate.setVisible(false);
        fallingCrate.setGravity(true);
        fallingCrate.setMarker(false); // Permet la collision et la gravité
        fallingCrate.setSmall(false);
        fallingCrate.getEquipment().setHelmet(new ItemStack(Material.CHEST));
        fallingCrate.setCustomName("§b§l✈ LARGAGE AÉRIEN");
        fallingCrate.setCustomNameVisible(true);
        fallingCrate.setGlowing(true);

        // Définir la position d'atterrissage cible
        landingLocation = location.clone();
        landingLocation.setY(world.getHighestBlockYAt(location) + 1);

        // Effets de chute - stocker la tâche pour cleanup
        fallingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (fallingCrate == null || !fallingCrate.isValid()) {
                    onCrateLanded();
                    cancel();
                    return;
                }

                Location crateLoc = fallingCrate.getLocation();

                // Vérifier si l'ArmorStand a atterri (proche du sol)
                if (crateLoc.getY() <= landingLocation.getY() + 0.5) {
                    // Supprimer l'ArmorStand et créer le coffre
                    fallingCrate.remove();
                    onCrateLanded();
                    cancel();
                    return;
                }

                // Particules de fumée pendant la chute
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

        // Éviter la double exécution
        if (phase != Phase.FALLING) return;

        // Utiliser la position d'atterrissage déjà définie ou la calculer
        if (landingLocation == null) {
            landingLocation = location.clone();
            landingLocation.setY(world.getHighestBlockYAt(location) + 1);
        }

        // Supprimer l'ArmorStand de chute si encore présent
        if (fallingCrate != null && fallingCrate.isValid()) {
            fallingCrate.remove();
        }

        // Créer UN SEUL coffre à la position d'atterrissage
        Block block = landingLocation.getBlock();
        // Nettoyer tout coffre existant pour éviter les doublons
        if (block.getType() == Material.CHEST) {
            if (block.getState() instanceof Chest existingChest) {
                existingChest.getInventory().clear();
            }
        }
        block.setType(Material.CHEST);
        chestBlock = block;

        // Créer un marqueur visuel avec TextDisplay (plus grand et plus visible)
        Location displayLoc = landingLocation.clone().add(0.5, 2.0, 0.5);
        crateMarker = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0)); // Fond semi-transparent

            // Texte initial avec style
            Component text = Component.text("📦 ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("LARGAGE", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("0%", NamedTextColor.YELLOW, TextDecoration.BOLD));
            display.text(text);

            // Échelle plus grande pour meilleure visibilité
            float scale = 2.0f;
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage
            display.addScoreboardTag("dynamic_event_entity");
        });

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
            // 1 joueur = 1x, 2 joueurs = 1.25x, 3 = 1.5x, 4 = 1.75x, 5+ = 2x
            double speedMultiplier = Math.min(2.0, 1.0 + (defendersNearby - 1) * 0.25);
            currentDefenseTime += speedMultiplier;

            // Mise à jour du marqueur TextDisplay
            int percent = (int) (currentDefenseTime / defenseTimeRequired * 100);
            percent = Math.min(100, percent); // Cap at 100%
            if (crateMarker != null && crateMarker.isValid()) {
                NamedTextColor percentColor = percent < 33 ? NamedTextColor.RED : (percent < 66 ? NamedTextColor.YELLOW : NamedTextColor.GREEN);
                Component text = Component.text("📦 ", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .append(Component.text("LARGAGE", NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(percent + "%", percentColor, TextDecoration.BOLD));
                crateMarker.text(text);
            }

            // Mise à jour de la boss bar
            double progress = currentDefenseTime / defenseTimeRequired;
            int remainingSeconds = (int) Math.ceil(defenseTimeRequired - currentDefenseTime);
            updateBossBar(1.0 - Math.min(1.0, progress), "- §e" + Math.max(0, remainingSeconds) + "s");

            // Particules de progression
            world.spawnParticle(Particle.HAPPY_VILLAGER, landingLocation.clone().add(0.5, 1, 0.5),
                3, 0.5, 0.5, 0.5, 0);
        } else {
            // Régression lente si personne n'est là (0.5 par tick = plus lent que la progression)
            if (currentDefenseTime > 0) {
                currentDefenseTime = Math.max(0, currentDefenseTime - 0.5);

                // Mise à jour du marqueur TextDisplay de régression
                int percent = (int) (currentDefenseTime / defenseTimeRequired * 100);
                if (crateMarker != null && crateMarker.isValid()) {
                    Component text = Component.text("⚠ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("DÉFENSEURS REQUIS!", NamedTextColor.RED, TextDecoration.BOLD))
                        .append(Component.text(" (" + percent + "%)", NamedTextColor.GRAY));
                    crateMarker.text(text);
                }
            } else {
                if (crateMarker != null && crateMarker.isValid()) {
                    Component text = Component.text("⚠ ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("DÉFENSEURS REQUIS!", NamedTextColor.RED, TextDecoration.BOLD));
                    crateMarker.text(text);
                }
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
                // Supprimer le coffre après pillage
                chest.getInventory().clear();
                chestBlock.setType(Material.AIR);
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

        // Mettre à jour le marqueur TextDisplay
        if (crateMarker != null && crateMarker.isValid()) {
            Component text = Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text("OUVERT!", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" Récupérez le loot!", NamedTextColor.WHITE));
            crateMarker.text(text);
        }

        // Annoncer
        for (Player player : world.getNearbyEntities(landingLocation, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§a§l✓ COFFRE OUVERT!", "§7Récupérez le loot!", 10, 40, 20);
        }

        // Timer pour compléter automatiquement après 60s - stocker pour cleanup
        lootingTimeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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
                Consumable consumable = plugin.getConsumableManager().generateConsumable(zone.getId(), 0.0);
                if (consumable != null) {
                    lootItems.add(consumable.createItemStack());
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
        // Annuler les tâches planifiées
        if (fallingTask != null && !fallingTask.isCancelled()) {
            fallingTask.cancel();
            fallingTask = null;
        }
        if (lootingTimeoutTask != null && !lootingTimeoutTask.isCancelled()) {
            lootingTimeoutTask.cancel();
            lootingTimeoutTask = null;
        }

        // Supprimer le marqueur
        if (crateMarker != null && crateMarker.isValid()) {
            crateMarker.remove();
            crateMarker = null;
        }

        // Supprimer le falling block si encore présent
        if (fallingCrate != null && fallingCrate.isValid()) {
            fallingCrate.remove();
            fallingCrate = null;
        }

        // Supprimer le coffre pour éviter de spammer la map
        if (chestBlock != null && chestBlock.getType() == Material.CHEST) {
            if (chestBlock.getState() instanceof Chest chest) {
                chest.getInventory().clear();
            }
            chestBlock.setType(Material.AIR);
            chestBlock = null;
        }
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
