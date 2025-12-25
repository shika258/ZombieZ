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
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Événement Protection des Survivants (Refonte complète)
 *
 * Déroulement:
 * 1. Un groupe de survivants (villageois) apparaît et demande de l'aide
 * 2. Des vagues de zombies attaquent les survivants - ils focus les NPCs!
 * 3. Les joueurs doivent protéger les survivants pendant un temps donné
 * 4. La vie des survivants est affichée via hologramme
 * 5. À la fin, les survivants remercient et un coffre de récompense spawn
 * 6. Les joueurs ne peuvent PAS trader avec les villageois
 * 7. L'événement s'annule si pas de joueur autour ou si le temps est écoulé
 */
public class SurvivorConvoyEvent extends DynamicEvent {

    // Survivants
    @Getter
    private final List<Villager> survivors = new ArrayList<>();
    private final Map<Villager, TextDisplay> healthDisplays = new HashMap<>();
    private int initialSurvivorCount;
    private int survivorsAlive;

    // Configuration
    private int protectionDuration = 60; // Durée entre 50-70 secondes (défaut 60)
    private int elapsedProtectionTime = 0;
    private int zombieWaveInterval = 8; // Secondes entre vagues
    private int zombiesPerWave = 4;
    private int waveTimer = 0;
    private int wavesSpawned = 0;

    // Délai avant le spawn des mobs (en secondes)
    private static final int MOB_SPAWN_DELAY = 5;
    private boolean mobSpawningEnabled = false;

    // Zombies qui focus les survivants
    private final List<Zombie> attackingZombies = new ArrayList<>();

    // Titre de l'événement
    private TextDisplay titleDisplay;
    private TextDisplay timerDisplay;

    // Timer d'inactivité (pas de joueur autour)
    private int noPlayerTimer = 0;
    private int noPlayerTimeout = 30; // 30 secondes sans joueur = annulation

    // Coffre de récompense
    private Block rewardChestBlock;
    private BukkitTask rewardChestTask; // Tâche de despawn du coffre

    // Tâche de zombies targeting
    private BukkitTask zombieTargetTask;

    public SurvivorConvoyEvent(ZombieZPlugin plugin, Location location, Zone zone) {
        super(plugin, DynamicEventType.SURVIVOR_CONVOY, location, zone);

        // Configuration basée sur la zone
        this.initialSurvivorCount = 3 + zone.getId() / 20; // 3-5 survivants
        this.zombiesPerWave = 4 + zone.getId() / 8;
        this.zombieWaveInterval = Math.max(5, 8 - zone.getId() / 15); // Plus fréquent en zone avancée
        // Durée aléatoire entre 50 et 70 secondes
        this.protectionDuration = 50 + (int) (Math.random() * 21); // 50-70 secondes
    }

    @Override
    protected void startMainLogic() {
        // Spawn les survivants
        spawnSurvivors();

        // Créer les hologrammes
        createDisplays();

        // Démarrer la tâche de ciblage des zombies
        startZombieTargeting();

        // Démarrer le tick principal
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
     * Spawn les survivants
     */
    private void spawnSurvivors() {
        World world = location.getWorld();
        if (world == null) return;

        Villager.Profession[] professions = {
            Villager.Profession.FARMER,
            Villager.Profession.LIBRARIAN,
            Villager.Profession.CLERIC,
            Villager.Profession.TOOLSMITH,
            Villager.Profession.WEAPONSMITH
        };

        String[] names = {"Marie", "Jean", "Pierre", "Sophie", "Lucas", "Emma", "Hugo", "Léa"};

        for (int i = 0; i < initialSurvivorCount; i++) {
            // Position légèrement dispersée autour du centre
            double offsetX = (Math.random() - 0.5) * 4;
            double offsetZ = (Math.random() - 0.5) * 4;
            Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

            Villager villager = (Villager) world.spawnEntity(spawnLoc, EntityType.VILLAGER);
            villager.setProfession(professions[i % professions.length]);
            villager.setVillagerLevel(1);
            villager.setCustomName("§e" + names[i % names.length] + " §7[Survivant]");
            villager.setCustomNameVisible(true);
            villager.setAI(false); // Désactiver l'IA - ils restent sur place
            villager.setInvulnerable(false);
            villager.setSilent(false);

            // Vie des survivants: entre 35 et 45 HP (aléatoire)
            double baseHealth = 35 + (Math.random() * 11); // 35-45 HP
            villager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            villager.setHealth(baseHealth);

            // Marquer comme survivant de l'événement
            villager.addScoreboardTag("convoy_survivor");
            villager.addScoreboardTag("event_" + id);
            villager.addScoreboardTag("no_trading"); // Tag pour empêcher le trade

            survivors.add(villager);

            // Créer l'affichage de vie pour ce survivant
            createHealthDisplay(villager);
        }

        survivorsAlive = survivors.size();

        // Annonce
        world.playSound(location, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 0.8f);

        // Message aux joueurs proches
        for (Player player : world.getNearbyEntities(location, 80, 40, 80).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("");
            player.sendMessage("§e§l🛡 SURVIVANTS EN DÉTRESSE!");
            player.sendMessage("§7Des survivants ont besoin de votre protection!");
            player.sendMessage("§7Protégez-les des zombies pendant §e" + protectionDuration + " secondes§7!");
            player.sendMessage("§b⏳ Les zombies arrivent dans §e" + MOB_SPAWN_DELAY + " secondes§b!");
            player.sendMessage("§a💉 Vous pouvez soigner les survivants avec des bandages!");
            player.sendMessage("§c⚠ Les zombies vont les attaquer directement!");
            player.sendMessage("");
        }
    }

    /**
     * Crée l'affichage de vie au-dessus d'un survivant
     */
    private void createHealthDisplay(Villager villager) {
        World world = villager.getWorld();

        TextDisplay display = world.spawn(villager.getLocation().add(0, 2.3, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(80, 0, 0, 0));
            d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.2f, 1.2f, 1.2f),
                new AxisAngle4f(0, 0, 0, 1)
            ));
            updateHealthDisplayText(d, villager);

            // Tag pour cleanup au redemarrage
            d.addScoreboardTag("dynamic_event_entity");
        });

        healthDisplays.put(villager, display);
    }

    /**
     * Met à jour le texte d'affichage de vie
     */
    private void updateHealthDisplayText(TextDisplay display, Villager villager) {
        double health = villager.getHealth();
        double maxHealth = villager.getAttribute(Attribute.MAX_HEALTH).getValue();
        double percent = (health / maxHealth) * 100;

        NamedTextColor color;
        if (percent > 66) color = NamedTextColor.GREEN;
        else if (percent > 33) color = NamedTextColor.YELLOW;
        else color = NamedTextColor.RED;

        // Barre de vie visuelle
        int bars = 10;
        int filled = (int) (percent / 100 * bars);
        StringBuilder healthBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            healthBar.append(i < filled ? "§a█" : "§7█");
        }

        display.text(Component.text("❤ ", NamedTextColor.RED)
            .append(Component.text((int) health + "/" + (int) maxHealth, color))
            .appendNewline()
            .append(Component.text(healthBar.toString())));
    }

    /**
     * Crée les affichages principaux
     */
    private void createDisplays() {
        World world = location.getWorld();
        if (world == null) return;

        // Titre de l'événement
        titleDisplay = world.spawn(location.clone().add(0, 4, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            d.text(Component.text("🛡 PROTÉGEZ LES SURVIVANTS 🛡", NamedTextColor.GOLD, TextDecoration.BOLD));
            d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(2.5f, 2.5f, 2.5f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage
            d.addScoreboardTag("dynamic_event_entity");
        });

        // Timer
        timerDisplay = world.spawn(location.clone().add(0, 3, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(80, 0, 0, 0));
            d.text(Component.text("⏱ " + protectionDuration + "s restantes", NamedTextColor.YELLOW));
            d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.8f, 1.8f, 1.8f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Tag pour cleanup au redemarrage
            d.addScoreboardTag("dynamic_event_entity");
        });
    }

    /**
     * Démarre la tâche qui fait focus les zombies sur les survivants
     */
    private void startZombieTargeting() {
        zombieTargetTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                // Faire focus les zombies sur les survivants
                for (Zombie zombie : new ArrayList<>(attackingZombies)) {
                    if (!zombie.isValid() || zombie.isDead()) {
                        attackingZombies.remove(zombie);
                        continue;
                    }

                    // Trouver le survivant le plus proche
                    Villager target = findNearestSurvivor(zombie.getLocation());
                    if (target != null && target.isValid() && !target.isDead()) {
                        zombie.setTarget(target);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes
    }

    /**
     * Trouve le survivant le plus proche
     */
    private Villager findNearestSurvivor(Location from) {
        Villager nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Villager v : survivors) {
            if (!v.isValid() || v.isDead()) continue;

            double dist = safeDistance(from, v.getLocation());
            if (dist < nearestDist && dist != Double.MAX_VALUE) {
                nearestDist = dist;
                nearest = v;
            }
        }

        return nearest;
    }

    @Override
    public void tick() {
        elapsedTicks += 20;
        elapsedProtectionTime++;
        waveTimer++;

        World world = location.getWorld();
        if (world == null) return;

        // Vérifier les survivants morts
        checkSurvivors();

        if (survivorsAlive <= 0) {
            fail();
            return;
        }

        // Vérifier la présence de joueurs
        int playersNearby = countNearbyPlayers();
        if (playersNearby == 0) {
            noPlayerTimer++;
            if (noPlayerTimer >= noPlayerTimeout) {
                // Annuler l'événement - pas de joueur
                cancelNoPlayers();
                return;
            }
        } else {
            noPlayerTimer = 0;
            // Ajouter les joueurs comme participants
            for (Player player : world.getNearbyEntities(location, 30, 20, 30).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList()) {
                addParticipant(player);
            }
        }

        // Activer le spawn de mobs après le délai initial
        if (!mobSpawningEnabled && elapsedProtectionTime >= MOB_SPAWN_DELAY) {
            mobSpawningEnabled = true;
            // Annoncer l'arrivée des zombies
            for (Player player : world.getNearbyEntities(location, 50, 30, 50).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList()) {
                player.sendMessage("§c§l⚠ §7Les zombies arrivent! Défendez les survivants!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            }
        }

        // Spawn de vagues de zombies (seulement après le délai)
        if (mobSpawningEnabled && waveTimer >= zombieWaveInterval) {
            spawnZombieWave();
            waveTimer = 0;
            wavesSpawned++;
        }

        // Mettre à jour les affichages
        updateDisplays();

        // Mettre à jour les barres de vie des survivants
        updateHealthDisplays();

        // Vérifier si la protection est terminée
        if (elapsedProtectionTime >= protectionDuration) {
            onProtectionComplete();
        }

        // Mettre à jour la boss bar
        double progress = (double) elapsedProtectionTime / protectionDuration;
        int remaining = protectionDuration - elapsedProtectionTime;
        updateBossBar(progress, "- §e" + survivorsAlive + "/" + initialSurvivorCount +
            " survivants §7| §a" + remaining + "s");
    }

    /**
     * Compte les joueurs à proximité
     */
    private int countNearbyPlayers() {
        World world = location.getWorld();
        if (world == null) return 0;

        return (int) world.getNearbyEntities(location, 40, 25, 40).stream()
            .filter(e -> e instanceof Player)
            .count();
    }

    /**
     * Vérifie l'état des survivants
     */
    private void checkSurvivors() {
        Iterator<Villager> iterator = survivors.iterator();
        while (iterator.hasNext()) {
            Villager v = iterator.next();
            if (!v.isValid() || v.isDead()) {
                // Supprimer l'affichage de vie
                TextDisplay display = healthDisplays.remove(v);
                if (display != null && display.isValid()) {
                    display.remove();
                }

                iterator.remove();
                survivorsAlive = survivors.size();

                // Annoncer la mort
                World world = location.getWorld();
                if (world != null) {
                    world.playSound(location, Sound.ENTITY_VILLAGER_DEATH, 1f, 1f);

                    for (Player player : world.getNearbyEntities(location, 50, 30, 50).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .toList()) {
                        player.sendMessage("§c§l☠ §7Un survivant est mort! §e" + survivorsAlive + "/" + initialSurvivorCount + " restants");
                    }
                }
            }
        }
    }

    /**
     * Spawn une vague de zombies qui attaquent les survivants
     */
    private void spawnZombieWave() {
        World world = location.getWorld();
        if (world == null) return;

        // Annoncer la vague
        for (Player player : world.getNearbyEntities(location, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§c§l⚠ §7Une vague de zombies attaque les survivants!");
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
        }

        // Spawn les zombies autour des survivants
        int actualZombies = zombiesPerWave + (wavesSpawned / 2); // Augmente avec le temps
        for (int i = 0; i < actualZombies; i++) {
            // Position autour du groupe de survivants
            double angle = Math.random() * Math.PI * 2;
            double distance = 12 + Math.random() * 8;
            double spawnX = location.getX() + Math.cos(angle) * distance;
            double spawnZ = location.getZ() + Math.sin(angle) * distance;
            int spawnY = world.getHighestBlockYAt((int) spawnX, (int) spawnZ) + 1;

            Location spawnLoc = new Location(world, spawnX, spawnY, spawnZ);

            // Créer un zombie configuré comme mob ZombieZ
            Zombie zombie = world.spawn(spawnLoc, Zombie.class, zomb -> {
                zomb.setBaby(false);
                zomb.setShouldBurnInDay(false); // Ne brûle pas au soleil
                zomb.setRemoveWhenFarAway(true);

                // Tags d'identification ZombieZ
                zomb.addScoreboardTag("zombiez_mob");
                zomb.addScoreboardTag("event_" + id);
                zomb.addScoreboardTag("survivor_attacker");

                // Metadata pour le système ZombieZ
                zomb.setMetadata("zombiez_type", new org.bukkit.metadata.FixedMetadataValue(plugin, "EVENT_ZOMBIE"));
                zomb.setMetadata("zombiez_zone", new org.bukkit.metadata.FixedMetadataValue(plugin, zone.getId()));

                // Stats basées sur la zone
                double healthMultiplier = 1.0 + (zone.getId() * 0.05);
                double damageMultiplier = 1.0 + (zone.getId() * 0.03);

                var maxHealthAttr = zomb.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    double health = 20 * healthMultiplier;
                    maxHealthAttr.setBaseValue(health);
                    zomb.setHealth(health);
                }

                var damageAttr = zomb.getAttribute(Attribute.ATTACK_DAMAGE);
                if (damageAttr != null) {
                    damageAttr.setBaseValue(3 * damageMultiplier);
                }

                // Nom visible
                zomb.setCustomName("§c☠ §7Zombie [§e" + zone.getDisplayName() + "§7]");
                zomb.setCustomNameVisible(false);
            });

            // Trouver un survivant à cibler
            Villager target = findNearestSurvivor(spawnLoc);
            if (target != null) {
                zombie.setTarget(target);
            }

            attackingZombies.add(zombie);

            // Effet de spawn
            world.spawnParticle(Particle.SMOKE, spawnLoc, 10, 0.3, 0.5, 0.3, 0.02);
        }
    }

    /**
     * Met à jour les affichages principaux
     */
    private void updateDisplays() {
        int remaining = protectionDuration - elapsedProtectionTime;

        if (timerDisplay != null && timerDisplay.isValid()) {
            NamedTextColor color;
            if (remaining > 60) color = NamedTextColor.GREEN;
            else if (remaining > 30) color = NamedTextColor.YELLOW;
            else color = NamedTextColor.RED;

            timerDisplay.text(Component.text("⏱ " + remaining + "s restantes", color)
                .appendNewline()
                .append(Component.text(survivorsAlive + "/" + initialSurvivorCount + " survivants", NamedTextColor.GOLD)));
        }

        if (titleDisplay != null && titleDisplay.isValid()) {
            NamedTextColor titleColor = survivorsAlive == initialSurvivorCount ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
            titleDisplay.text(Component.text("🛡 PROTÉGEZ LES SURVIVANTS 🛡", titleColor, TextDecoration.BOLD));
        }
    }

    /**
     * Met à jour les affichages de vie des survivants
     */
    private void updateHealthDisplays() {
        for (Map.Entry<Villager, TextDisplay> entry : healthDisplays.entrySet()) {
            Villager villager = entry.getKey();
            TextDisplay display = entry.getValue();

            if (!villager.isValid() || villager.isDead() || !display.isValid()) continue;

            // Suivre le villageois
            display.teleport(villager.getLocation().add(0, 2.3, 0));

            // Mettre à jour le texte
            updateHealthDisplayText(display, villager);
        }
    }

    /**
     * Appelé quand la protection est terminée avec succès
     */
    private void onProtectionComplete() {
        World world = location.getWorld();
        if (world == null) {
            complete();
            return;
        }

        // Les survivants remercient
        for (Villager v : survivors) {
            if (v.isValid() && !v.isDead()) {
                world.playSound(v.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
                world.spawnParticle(Particle.HEART, v.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            }
        }

        // Message de remerciement
        for (Player player : world.getNearbyEntities(location, 60, 40, 60).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendTitle("§a§l✓ SURVIVANTS SAUVÉS!", "§7Ils vous remercient avec un coffre de récompenses!", 10, 60, 20);
            player.sendMessage("");
            player.sendMessage("§a§l✓ §eLes survivants vous remercient!");
            player.sendMessage("§7\"Merci de nous avoir protégés! Prenez ceci en récompense!\"");
            player.sendMessage("");
        }

        // Spawn le coffre de récompense
        spawnRewardChest();

        complete();
    }

    /**
     * Spawn le coffre de récompense
     */
    private void spawnRewardChest() {
        World world = location.getWorld();
        if (world == null) return;

        // Position du coffre (au centre)
        Location chestLoc = location.clone();
        chestLoc.setY(world.getHighestBlockYAt(location) + 1);

        // Créer le coffre
        Block block = chestLoc.getBlock();
        block.setType(Material.CHEST);
        rewardChestBlock = block;

        // Effet de spawn
        world.playSound(chestLoc, Sound.BLOCK_CHEST_OPEN, 1f, 0.8f);
        world.playSound(chestLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, chestLoc.clone().add(0.5, 1, 0.5), 50, 0.5, 1, 0.5, 0.1);

        // Remplir le coffre
        if (block.getState() instanceof Chest chest) {
            fillRewardChest(chest);
        }

        // Démarrer la tâche de despawn automatique (60 secondes ou si vide)
        startChestDespawnTask();
    }

    /**
     * Démarre la tâche de despawn du coffre (après 60s ou si vide)
     */
    private void startChestDespawnTask() {
        if (rewardChestBlock == null) return;

        final int DESPAWN_DELAY_SECONDS = 60;

        rewardChestTask = new BukkitRunnable() {
            int secondsRemaining = DESPAWN_DELAY_SECONDS;

            @Override
            public void run() {
                // Vérifier si le coffre existe toujours
                if (rewardChestBlock == null || rewardChestBlock.getType() != Material.CHEST) {
                    cancel();
                    return;
                }

                // Vérifier si le coffre est vide
                if (rewardChestBlock.getState() instanceof Chest chest) {
                    boolean isEmpty = true;
                    for (ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            isEmpty = false;
                            break;
                        }
                    }

                    if (isEmpty) {
                        // Despawn immédiat si le coffre est vide
                        despawnRewardChest("§7Le coffre a été vidé!");
                        cancel();
                        return;
                    }
                }

                secondsRemaining--;

                // Avertissements aux joueurs proches
                if (secondsRemaining == 30 || secondsRemaining == 10 || secondsRemaining == 5) {
                    notifyChestDespawn(secondsRemaining);
                }

                // Despawn après le délai
                if (secondsRemaining <= 0) {
                    despawnRewardChest("§c⚠ Le coffre a disparu! (60s écoulées)");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Vérifier chaque seconde
    }

    /**
     * Notifie les joueurs proches du despawn imminent du coffre
     */
    private void notifyChestDespawn(int secondsRemaining) {
        if (rewardChestBlock == null) return;
        World world = rewardChestBlock.getWorld();

        for (Player player : world.getNearbyEntities(rewardChestBlock.getLocation(), 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§e⚠ §7Le coffre de récompense disparaîtra dans §c" + secondsRemaining + " secondes§7!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.8f);
        }
    }

    /**
     * Fait despawn le coffre de récompense
     */
    private void despawnRewardChest(String message) {
        if (rewardChestBlock == null) return;

        World world = rewardChestBlock.getWorld();
        Location chestLoc = rewardChestBlock.getLocation();

        // Effets visuels
        world.spawnParticle(Particle.CLOUD, chestLoc.clone().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.05);
        world.playSound(chestLoc, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);

        // Supprimer le coffre
        rewardChestBlock.setType(Material.AIR);

        // Message aux joueurs proches
        for (Player player : world.getNearbyEntities(chestLoc, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage(message);
        }

        rewardChestBlock = null;
    }

    /**
     * Remplit le coffre de récompenses
     */
    private void fillRewardChest(Chest chest) {
        // Bonus basé sur le nombre de survivants sauvés
        double survivalRate = (double) survivorsAlive / initialSurvivorCount;
        int itemCount = (int) (3 + survivalRate * 4 + zone.getId() / 10); // 3-7+ items

        // Items de qualité
        for (int i = 0; i < itemCount; i++) {
            Rarity rarity = rollLootRarity(survivalRate);
            ItemStack item = plugin.getItemManager().generateItem(zone.getId(), rarity);
            if (item != null) {
                chest.getInventory().addItem(item);
            }
        }

        // Consommables bonus
        if (plugin.getConsumableManager() != null) {
            int consumableCount = 2 + (int) (survivalRate * 3);
            for (int i = 0; i < consumableCount; i++) {
                Consumable consumable = plugin.getConsumableManager().generateConsumable(zone.getId(), 0.0);
                if (consumable != null) {
                    chest.getInventory().addItem(consumable.createItemStack());
                }
            }
        }
    }

    private Rarity rollLootRarity(double survivalBonus) {
        double roll = Math.random() * 100;
        double bonus = survivalBonus * 15 + zone.getId() * 0.5; // Bonus survie + zone

        if (roll < 5 + bonus) return Rarity.LEGENDARY;
        if (roll < 20 + bonus) return Rarity.EPIC;
        if (roll < 45 + bonus) return Rarity.RARE;
        if (roll < 75) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    /**
     * Annule l'événement car pas de joueurs à proximité
     */
    private void cancelNoPlayers() {
        World world = location.getWorld();

        // Message d'annulation
        if (world != null) {
            for (Player player : world.getNearbyEntities(location, 100, 50, 100).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList()) {
                player.sendMessage("§c§l⚠ §7L'événement §eSurvivants §7a été annulé - aucun joueur présent!");
            }
        }

        fail();
    }

    @Override
    protected void distributeRewards() {
        // Bonus basé sur le nombre de survivants sauvés
        double survivalRate = (double) survivorsAlive / initialSurvivorCount;
        int bonusMultiplier = (int) (survivalRate * 2);

        int totalPoints = (int) (basePointsReward * (1 + bonusMultiplier * 0.5));
        int totalXp = (int) (baseXpReward * (1 + bonusMultiplier * 0.5));

        for (UUID uuid : participants) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getEconomyManager().addPoints(player, totalPoints);

                var playerData = plugin.getPlayerDataManager().getPlayer(uuid);
                if (playerData != null) {
                    playerData.addXp(totalXp);
                }

                player.sendMessage("");
                player.sendMessage("§a§l✓ SURVIVANTS PROTÉGÉS!");
                player.sendMessage("§7Survivants sauvés: §e" + survivorsAlive + "/" + initialSurvivorCount +
                    " §7(" + (int) (survivalRate * 100) + "%)");
                player.sendMessage("§7Récompenses: §e+" + totalPoints + " Points §7| §b+" + totalXp + " XP");
                player.sendMessage("§7Coffre de récompense déposé!");
                player.sendMessage("");

                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            }
        }
    }

    @Override
    protected void onCleanup() {
        // Annuler la tâche de ciblage
        if (zombieTargetTask != null && !zombieTargetTask.isCancelled()) {
            zombieTargetTask.cancel();
            zombieTargetTask = null;
        }

        // Annuler la tâche de despawn du coffre
        if (rewardChestTask != null && !rewardChestTask.isCancelled()) {
            rewardChestTask.cancel();
            rewardChestTask = null;
        }

        // Supprimer les affichages de titre
        if (titleDisplay != null && titleDisplay.isValid()) {
            titleDisplay.remove();
        }
        titleDisplay = null;

        if (timerDisplay != null && timerDisplay.isValid()) {
            timerDisplay.remove();
        }
        timerDisplay = null;

        // Supprimer les affichages de vie
        for (TextDisplay display : healthDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        healthDisplays.clear();

        // Supprimer les survivants
        for (Villager v : survivors) {
            if (v != null && v.isValid()) {
                v.remove();
            }
        }
        survivors.clear();

        // Supprimer les zombies de l'événement
        for (Zombie zombie : attackingZombies) {
            if (zombie != null && zombie.isValid()) {
                zombie.remove();
            }
        }
        attackingZombies.clear();

        // Nettoyer le coffre de récompense (évite les coffres orphelins après reboot/crash)
        // Le coffre est nettoyé lors du cleanup pour éviter la persistance indésirable
        if (rewardChestBlock != null && rewardChestBlock.getType() == Material.CHEST) {
            if (rewardChestBlock.getState() instanceof Chest chest) {
                chest.getInventory().clear();
            }
            rewardChestBlock.setType(Material.AIR);
        }
        rewardChestBlock = null;
    }

    @Override
    protected String getStartSubtitle() {
        return "Protégez " + initialSurvivorCount + " survivants des zombies!";
    }

    @Override
    public String getDebugInfo() {
        return String.format("Survivors: %d/%d | Time: %d/%d | Waves: %d | Players: %d | NoPlayerTimer: %d",
            survivorsAlive, initialSurvivorCount, elapsedProtectionTime, protectionDuration,
            wavesSpawned, countNearbyPlayers(), noPlayerTimer);
    }
}
