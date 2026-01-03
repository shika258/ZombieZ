package com.rinaorc.zombiez.progression.journey.chapter5;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Salmon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Systèmes du Chapitre 5 - Territoire Hostile
 *
 * Étape 2: Pêche aux Saumons Mutants
 * - Zone de spawn des saumons: c1(798, 86, 8212) à c2(845, 86, 8155)
 * - Saumons spawnent uniquement dans l'eau
 * - 12 saumons à tuer
 * - 100 HP chacun, dégâts des armes ZombieZ
 * - Pas de drop d'items
 */
public class Chapter5Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey QUEST_SALMON_KEY;

    // === ZONE DE PÊCHE ===
    // Coordonnées de la zone (corners)
    private static final int SALMON_ZONE_MIN_X = 798;
    private static final int SALMON_ZONE_MAX_X = 845;
    private static final int SALMON_ZONE_Y = 86;
    private static final int SALMON_ZONE_MIN_Z = 8155;
    private static final int SALMON_ZONE_MAX_Z = 8212;

    // Centre de la zone pour le GPS
    private static final int SALMON_ZONE_CENTER_X = (SALMON_ZONE_MIN_X + SALMON_ZONE_MAX_X) / 2; // ~821
    private static final int SALMON_ZONE_CENTER_Z = (SALMON_ZONE_MIN_Z + SALMON_ZONE_MAX_Z) / 2; // ~8183

    // === CONFIGURATION ===
    private static final int MAX_SALMONS_IN_ZONE = 8;  // Maximum de saumons simultanés
    private static final int SPAWN_INTERVAL_TICKS = 100; // 5 secondes entre chaque spawn check
    private static final int SALMON_LEVEL = 10; // Niveau des saumons (pour les stats)

    // === TRACKING ===
    // Joueurs actifs sur cette quête
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    // Saumons actifs (UUID de l'entité -> UUID du joueur qui l'a spawné)
    private final Map<UUID, UUID> activeSalmons = new ConcurrentHashMap<>();
    // Tâche de spawn périodique
    private BukkitTask spawnTask;

    public Chapter5Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.QUEST_SALMON_KEY = new NamespacedKey(plugin, "quest_salmon_ch5");

        // Enregistrer les événements
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Démarrer le système de spawn
        startSpawnTask();

        plugin.getLogger().info("[Chapter5Systems] Système de pêche aux saumons mutants initialisé");
    }

    /**
     * Démarre la tâche de spawn périodique des saumons
     */
    private void startSpawnTask() {
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activePlayers.isEmpty()) {
                    return; // Pas de joueurs actifs, pas besoin de spawner
                }

                // Pour chaque joueur actif, vérifier si on doit spawner des saumons
                for (UUID playerId : activePlayers) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    // Vérifier si le joueur est proche de la zone
                    if (!isPlayerNearZone(player)) {
                        continue;
                    }

                    // Compter les saumons actifs
                    int currentSalmons = countActiveSalmons();
                    if (currentSalmons >= MAX_SALMONS_IN_ZONE) {
                        continue; // Zone pleine
                    }

                    // Spawner un saumon
                    spawnSalmon(player.getWorld());
                }
            }
        }.runTaskTimer(plugin, 20L, SPAWN_INTERVAL_TICKS);
    }

    /**
     * Vérifie si un joueur est proche de la zone de pêche
     */
    private boolean isPlayerNearZone(Player player) {
        Location loc = player.getLocation();
        double distanceSquared = Math.pow(loc.getX() - SALMON_ZONE_CENTER_X, 2) +
                                 Math.pow(loc.getZ() - SALMON_ZONE_CENTER_Z, 2);
        return distanceSquared < 10000; // 100 blocs de rayon
    }

    /**
     * Compte les saumons actifs dans la zone
     */
    private int countActiveSalmons() {
        int count = 0;
        Iterator<UUID> it = activeSalmons.keySet().iterator();
        while (it.hasNext()) {
            UUID salmonId = it.next();
            Entity entity = plugin.getServer().getEntity(salmonId);
            if (entity == null || !entity.isValid() || entity.isDead()) {
                it.remove(); // Nettoyer les entrées invalides
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Spawne un saumon dans la zone de pêche
     * Le saumon ne spawn que dans l'eau
     */
    private void spawnSalmon(World world) {
        // Chercher une position valide dans l'eau
        Location spawnLoc = findWaterSpawnLocation(world);
        if (spawnLoc == null) {
            return; // Pas de position valide trouvée
        }

        // Utiliser ZombieManager pour spawner le saumon
        ZombieManager zombieManager = plugin.getZombieManager();
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(
            ZombieType.QUEST_SALMON,
            spawnLoc,
            SALMON_LEVEL
        );

        if (activeZombie != null) {
            Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
            if (entity instanceof LivingEntity salmon) {
                // Marquer le saumon comme saumon de quête
                salmon.getPersistentDataContainer().set(QUEST_SALMON_KEY, PersistentDataType.BYTE, (byte) 1);

                // Ajouter au tracking
                activeSalmons.put(entity.getUniqueId(), null);

                // Effets visuels de spawn
                world.spawnParticle(Particle.BUBBLE_COLUMN_UP, spawnLoc, 20, 0.5, 0.5, 0.5, 0);
                world.playSound(spawnLoc, Sound.ENTITY_SALMON_FLOP, 1.0f, 0.8f);
            }
        }
    }

    /**
     * Trouve une position de spawn valide dans l'eau
     * Retourne null si aucune position valide n'est trouvée après plusieurs essais
     */
    private Location findWaterSpawnLocation(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Essayer plusieurs fois de trouver une position valide
        for (int attempt = 0; attempt < 15; attempt++) {
            int x = random.nextInt(SALMON_ZONE_MIN_X, SALMON_ZONE_MAX_X + 1);
            int z = random.nextInt(SALMON_ZONE_MIN_Z, SALMON_ZONE_MAX_Z + 1);

            // Chercher de l'eau autour de Y = 86
            for (int y = SALMON_ZONE_Y + 5; y >= SALMON_ZONE_Y - 10; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    // Vérifier qu'il y a assez d'espace
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (above.getType() == Material.WATER || above.getType() == Material.AIR) {
                        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
                    }
                }
            }
        }

        return null; // Aucune position valide trouvée
    }

    /**
     * Active la quête de pêche pour un joueur
     */
    public void activateSalmonQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Ajouter aux joueurs actifs
        activePlayers.add(playerId);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§b\uD83D\uDC1F §3§lPÊCHE MUTANTE §b\uD83D\uDC1F",
                    "§7Éliminez les saumons contaminés!",
                    10, 60, 20
                );

                // Son d'ambiance aquatique
                player.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§b§l══════ PÊCHE AUX SAUMONS MUTANTS ══════");
                player.sendMessage("");
                player.sendMessage("§7Des saumons §cmutants §7ont été repérés dans les");
                player.sendMessage("§7marécages. Ils sont §cdangereux §7et §ccontaminés§7.");
                player.sendMessage("");
                player.sendMessage("§e▸ §fTuez §c12 saumons mutants");
                player.sendMessage("§e▸ §fUtilisez vos armes ZombieZ");
                player.sendMessage("§e▸ §fIls ont §c100 HP §fchacun");
                player.sendMessage("");

                // Activer le GPS
                activateGPSToFishingZone(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers la zone de pêche
     */
    private void activateGPSToFishingZone(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + SALMON_ZONE_CENTER_X + ", " + SALMON_ZONE_Y + ", " + SALMON_ZONE_CENTER_Z + " §7(Zone de pêche)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        // Activer le GPS système si disponible
        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Appelé quand un joueur tue un saumon de quête
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSalmonDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Vérifier si c'est un saumon de quête
        if (!(entity instanceof Salmon salmon)) {
            return;
        }

        if (!salmon.getPersistentDataContainer().has(QUEST_SALMON_KEY, PersistentDataType.BYTE)) {
            return;
        }

        // Supprimer les drops (pas de loot)
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Retirer du tracking
        activeSalmons.remove(entity.getUniqueId());

        // Trouver le tueur
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        // Vérifier si le joueur est sur la bonne étape
        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep != JourneyStep.STEP_5_2) {
            return;
        }

        // Incrémenter la progression
        int progress = journeyManager.getStepProgress(killer, currentStep);
        int newProgress = progress + 1;
        journeyManager.setStepProgress(killer, currentStep, newProgress);

        // Effets visuels
        Location loc = entity.getLocation();
        killer.getWorld().spawnParticle(Particle.BUBBLE_POP, loc, 30, 0.5, 0.5, 0.5, 0.1);
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        // Message de progression
        int remaining = 12 - newProgress;
        if (remaining > 0) {
            killer.sendTitle(
                "§a\u2713 §fSaumon Pêché!",
                "§7" + newProgress + "/12 - Plus que §c" + remaining + " §7saumons!",
                5, 30, 10
            );
        }

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(killer);

        // Vérifier si la quête est complète
        if (currentStep.isCompleted(newProgress)) {
            completeQuest(killer);
        }
    }

    /**
     * Termine la quête de pêche
     */
    private void completeQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Retirer des joueurs actifs
        activePlayers.remove(playerId);

        // Nettoyer les saumons restants
        cleanupPlayerSalmons();

        // Compléter l'étape via JourneyManager (il gère les récompenses et la transition)
        journeyManager.completeStep(player, JourneyStep.STEP_5_2);

        // Message de victoire
        player.sendTitle(
            "§a§l\u2713 QUÊTE TERMINÉE!",
            "§7Tous les saumons mutants ont été éliminés!",
            10, 60, 20
        );

        // Son de victoire
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Feu d'artifice
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    /**
     * Nettoie tous les saumons de quête actifs
     */
    private void cleanupPlayerSalmons() {
        for (UUID salmonId : new HashSet<>(activeSalmons.keySet())) {
            Entity entity = plugin.getServer().getEntity(salmonId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeSalmons.clear();
    }

    // ==================== ÉVÉNEMENTS DE CONNEXION ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Vérifier si le joueur est sur l'étape 5_2
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                JourneyStep currentStep = journeyManager.getCurrentStep(player);
                if (currentStep == JourneyStep.STEP_5_2) {
                    // Réactiver la quête
                    activePlayers.add(player.getUniqueId());

                    // Rappel de la quête
                    int progress = journeyManager.getStepProgress(player, currentStep);
                    int remaining = 12 - progress;

                    player.sendMessage("");
                    player.sendMessage("§b§l[QUÊTE] §7Pêche aux Saumons Mutants en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/12");
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fsaumons");
                    }

                    // Réactiver le GPS
                    activateGPSToFishingZone(player);
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        activePlayers.remove(playerId);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_2
     * (appelé depuis JourneyManager lors de la progression)
     */
    public void onPlayerReachStep52(Player player) {
        activateSalmonQuest(player);
    }

    /**
     * Nettoyage lors de la désactivation du plugin
     */
    public void cleanup() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        cleanupPlayerSalmons();
        activePlayers.clear();
        plugin.getLogger().info("[Chapter5Systems] Cleanup effectué");
    }
}
