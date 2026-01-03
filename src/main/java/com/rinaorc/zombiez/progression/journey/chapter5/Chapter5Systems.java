package com.rinaorc.zombiez.progression.journey.chapter5;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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
 *
 * Étape 3: Extraction Minière
 * - Zone de spawn des minerais: c1(957, 92, 8347) à c2(909, 71, 8369)
 * - 20 minerais générés avec types aléatoires
 * - 10 minerais à miner pour compléter
 * - Reset si le joueur meurt
 * - Minerais invisibles une fois la quête terminée
 */
public class Chapter5Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey QUEST_SALMON_KEY;
    private final NamespacedKey ORE_VISUAL_KEY;
    private final NamespacedKey ORE_HITBOX_KEY;
    private final NamespacedKey SUSPECT_NPC_KEY;
    private final NamespacedKey TRAITOR_PILLAGER_KEY;
    private final NamespacedKey LUMBER_VISUAL_KEY;
    private final NamespacedKey LUMBER_HITBOX_KEY;
    private final NamespacedKey LUMBERJACK_NPC_KEY;
    private final NamespacedKey FROG_VISUAL_KEY;
    private final NamespacedKey FROG_HITBOX_KEY;
    private final NamespacedKey BIOLOGIST_NPC_KEY;
    private final NamespacedKey ORACLE_NPC_KEY;

    // === ZONE DE PÊCHE (Étape 5.2) ===
    private static final int SALMON_ZONE_MIN_X = 798;
    private static final int SALMON_ZONE_MAX_X = 845;
    private static final int SALMON_ZONE_Y = 86;
    private static final int SALMON_ZONE_MIN_Z = 8155;
    private static final int SALMON_ZONE_MAX_Z = 8212;
    private static final int SALMON_ZONE_CENTER_X = (SALMON_ZONE_MIN_X + SALMON_ZONE_MAX_X) / 2;
    private static final int SALMON_ZONE_CENTER_Z = (SALMON_ZONE_MIN_Z + SALMON_ZONE_MAX_Z) / 2;

    // === ZONE DE MINAGE (Étape 5.3) ===
    // c1 (957, 92, 8347) à c2 (909, 71, 8369)
    private static final int MINE_ZONE_MIN_X = 909;
    private static final int MINE_ZONE_MAX_X = 957;
    private static final int MINE_ZONE_MIN_Y = 71;
    private static final int MINE_ZONE_MAX_Y = 92;
    private static final int MINE_ZONE_MIN_Z = 8347;
    private static final int MINE_ZONE_MAX_Z = 8369;
    private static final int MINE_ZONE_CENTER_X = (MINE_ZONE_MIN_X + MINE_ZONE_MAX_X) / 2; // ~933
    private static final int MINE_ZONE_CENTER_Y = (MINE_ZONE_MIN_Y + MINE_ZONE_MAX_Y) / 2; // ~81
    private static final int MINE_ZONE_CENTER_Z = (MINE_ZONE_MIN_Z + MINE_ZONE_MAX_Z) / 2; // ~8358

    // === CONFIGURATION SAUMONS ===
    private static final int MAX_SALMONS_IN_ZONE = 8;
    private static final int SPAWN_INTERVAL_TICKS = 100;
    private static final int SALMON_LEVEL = 10;

    // === CONFIGURATION MINERAIS ===
    private static final int TOTAL_ORES = 20;           // Nombre de minerais à spawner
    private static final int ORES_TO_MINE = 10;         // Nombre de minerais requis pour compléter
    private static final int HITS_TO_MINE = 8;          // Nombre de coups pour miner un minerai
    private static final float ORE_VIEW_DISTANCE = 48f; // Distance de vue des minerais
    private static final double ORE_DISPLAY_HEIGHT = 2.0; // Hauteur du TextDisplay au-dessus du minerai

    // === CONFIGURATION TRAÎTRE (Étape 5.5) ===
    private static final int TOTAL_SUSPECTS = 5;
    private static final int TRAITOR_LEVEL = 12;
    // Coordonnées des suspects: x, y, z, yaw, pitch
    private static final double[][] SUSPECT_LOCATIONS = {
        {678.5, 90, 8222.5, 180, 0},
        {703.5, 94, 8202.5, -12, 10},
        {741.5, 100, 8220.5, -140, 0},
        {656.5, 97, 8195.5, 0, 0},
        {719.5, 90, 8246.5, 0, 0}
    };
    private static final String[] SUSPECT_NAMES = {
        "§e§lMarchand Louche",
        "§e§lVoyageur Nerveux",
        "§e§lGarde Silencieux",
        "§e§lFermier Méfiant",
        "§e§lÉtranger Masqué"
    };
    private static final String[] SUSPECT_DIALOGUES = {
        "§7\"Je ne sais rien! Je ne fais que vendre mes potions...\"",
        "§7\"Pourquoi me regardez-vous comme ça? Je suis juste de passage!\"",
        "§7\"Les murs ont des oreilles ici... Faites attention.\"",
        "§7\"La récolte a été mauvaise... mais un traître? Ici?\"",
        "§7\"...\" §8*Il vous fixe en silence*"
    };

    // === CONFIGURATION BÛCHERONNAGE (Étape 5.6) ===
    // Zone de spawn du bois: corner1 (551, 102, 8289) à corner2 (493, 108, 8355)
    private static final int LUMBER_ZONE_MIN_X = 493;
    private static final int LUMBER_ZONE_MAX_X = 551;
    private static final int LUMBER_ZONE_MIN_Y = 102;
    private static final int LUMBER_ZONE_MAX_Y = 108;
    private static final int LUMBER_ZONE_MIN_Z = 8289;
    private static final int LUMBER_ZONE_MAX_Z = 8355;
    private static final int LUMBER_ZONE_CENTER_X = (LUMBER_ZONE_MIN_X + LUMBER_ZONE_MAX_X) / 2; // ~522
    private static final int LUMBER_ZONE_CENTER_Y = (LUMBER_ZONE_MIN_Y + LUMBER_ZONE_MAX_Y) / 2; // ~105
    private static final int LUMBER_ZONE_CENTER_Z = (LUMBER_ZONE_MIN_Z + LUMBER_ZONE_MAX_Z) / 2; // ~8322

    private static final int TOTAL_LUMBER = 24;           // Nombre de bois à spawner
    private static final int LUMBER_TO_COLLECT = 16;      // Nombre de bois requis pour compléter
    private static final float LUMBER_VIEW_DISTANCE = 48f; // Distance de vue des bois

    // Bûcheron NPC: 637.5, 87, 8244.5 avec yaw -45, pitch 0
    private static final double LUMBERJACK_X = 637.5;
    private static final double LUMBERJACK_Y = 87;
    private static final double LUMBERJACK_Z = 8244.5;
    private static final float LUMBERJACK_YAW = -45;
    private static final float LUMBERJACK_PITCH = 0;

    // === CONFIGURATION GRENOUILLES (Étape 5.7) ===
    // Zone de spawn des grenouilles: c1(370, 90, 8361) à c2(299, 90, 8431)
    private static final int FROG_ZONE_MIN_X = 299;
    private static final int FROG_ZONE_MAX_X = 370;
    private static final int FROG_ZONE_MIN_Y = 85;
    private static final int FROG_ZONE_MAX_Y = 95;
    private static final int FROG_ZONE_MIN_Z = 8361;
    private static final int FROG_ZONE_MAX_Z = 8431;
    private static final int FROG_ZONE_CENTER_X = (FROG_ZONE_MIN_X + FROG_ZONE_MAX_X) / 2; // ~334
    private static final int FROG_ZONE_CENTER_Y = 90;
    private static final int FROG_ZONE_CENTER_Z = (FROG_ZONE_MIN_Z + FROG_ZONE_MAX_Z) / 2; // ~8396

    private static final int TOTAL_FROGS = 8;           // Nombre de grenouilles à spawner
    private static final int FROGS_TO_CAPTURE = 5;      // Nombre de grenouilles requises pour compléter
    private static final float FROG_VIEW_DISTANCE = 48f; // Distance de vue des grenouilles

    // Biologiste NPC: près de la zone des grenouilles
    private static final double BIOLOGIST_X = 395.5;
    private static final double BIOLOGIST_Y = 90;
    private static final double BIOLOGIST_Z = 8180.5;
    private static final float BIOLOGIST_YAW = 90;
    private static final float BIOLOGIST_PITCH = 0;

    // === CONFIGURATION ÉNIGMES (Étape 5.8) ===
    // Oracle NPC: 164.5, 96, 8149 avec yaw -90, pitch 0
    private static final double ORACLE_X = 164.5;
    private static final double ORACLE_Y = 96;
    private static final double ORACLE_Z = 8149;
    private static final float ORACLE_YAW = -90;
    private static final float ORACLE_PITCH = 0;
    private static final int TOTAL_RIDDLES = 3;  // Nombre d'énigmes à résoudre

    // === CONFIGURATION BOSS GRENOUILLE (Étape 5.10) ===
    // Boss location: 384, 93, 8034
    private static final Location SWAMP_FROG_BOSS_LOCATION = new Location(null, 384.5, 93, 8034.5, 0, 0);
    private static final int BOSS_LEVEL = 15;  // Niveau du boss
    private static final int BOSS_RESPAWN_SECONDS = 120;  // Respawn après 2 minutes

    // Types de minerais avec leurs couleurs de glow
    private enum OreType {
        REDSTONE(Material.REDSTONE_ORE, Color.RED, "§cRedstone"),
        DIAMOND(Material.DIAMOND_ORE, Color.AQUA, "§bDiamant"),
        EMERALD(Material.EMERALD_ORE, Color.GREEN, "§aEmeraude"),
        GOLD(Material.GOLD_ORE, Color.YELLOW, "§6Or"),
        IRON(Material.IRON_ORE, Color.WHITE, "§7Fer"),
        LAPIS(Material.LAPIS_ORE, Color.BLUE, "§9Lapis"),
        COPPER(Material.COPPER_ORE, Color.ORANGE, "§eCuivre");

        final Material material;
        final Color glowColor;
        final String displayName;

        OreType(Material material, Color glowColor, String displayName) {
            this.material = material;
            this.glowColor = glowColor;
            this.displayName = displayName;
        }

        static OreType random() {
            return values()[ThreadLocalRandom.current().nextInt(values().length)];
        }
    }

    // === TRACKING SAUMONS ===
    private final Set<UUID> activeSalmonPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> activeSalmons = new ConcurrentHashMap<>();
    private BukkitTask salmonSpawnTask;

    // === TRACKING MINERAIS ===
    // Joueurs actifs sur la quête de minage
    private final Set<UUID> activeMiningPlayers = ConcurrentHashMap.newKeySet();
    // Minerais (ItemDisplay ORE glowing + Interaction hitbox + TextDisplay)
    private final ItemDisplay[] oreVisuals = new ItemDisplay[TOTAL_ORES];
    private final Interaction[] oreHitboxes = new Interaction[TOTAL_ORES];
    private final TextDisplay[] oreDisplays = new TextDisplay[TOTAL_ORES];
    private final OreType[] oreTypes = new OreType[TOTAL_ORES];
    private final Location[] oreLocations = new Location[TOTAL_ORES];
    // Tracking per-player des hits sur chaque minerai
    private final Map<UUID, int[]> playerOreHits = new ConcurrentHashMap<>();
    // Minerais minés par chaque joueur (Set des index)
    private final Map<UUID, Set<Integer>> playerMinedOres = new ConcurrentHashMap<>();

    // === TRACKING TRAÎTRE (Étape 5.5) ===
    // Suspects (Villagers glowing)
    private final Villager[] suspectNPCs = new Villager[TOTAL_SUSPECTS];
    private final TextDisplay[] suspectDisplays = new TextDisplay[TOTAL_SUSPECTS];
    // Joueurs actifs sur la quête du traître
    private final Set<UUID> activeTraitorPlayers = ConcurrentHashMap.newKeySet();
    // Suspects interrogés par chaque joueur (Set des index)
    private final Map<UUID, Set<Integer>> playerInterrogatedSuspects = new ConcurrentHashMap<>();
    // Traître actif par joueur (UUID du Pillager spawné)
    private final Map<UUID, UUID> playerActiveTraitors = new ConcurrentHashMap<>();
    // Team pour le glow vert des suspects
    private Team suspectGlowTeam;

    // === TRACKING BÛCHERONNAGE (Étape 5.6) ===
    // Bois (ItemDisplay bois glowing + Interaction hitbox)
    private final ItemDisplay[] lumberVisuals = new ItemDisplay[TOTAL_LUMBER];
    private final Interaction[] lumberHitboxes = new Interaction[TOTAL_LUMBER];
    private final Location[] lumberLocations = new Location[TOTAL_LUMBER];
    // Bûcheron NPC
    private Villager lumberjackNPC;
    private TextDisplay lumberjackDisplay;
    // Joueurs actifs sur la quête de bûcheronnage
    private final Set<UUID> activeLumberPlayers = ConcurrentHashMap.newKeySet();
    // Bois collectés par chaque joueur (Set des index)
    private final Map<UUID, Set<Integer>> playerCollectedLumber = new ConcurrentHashMap<>();
    // Nombre de bois dans l'inventaire par joueur
    private final Map<UUID, Integer> playerLumberInInventory = new ConcurrentHashMap<>();

    // === TRACKING GRENOUILLES (Étape 5.7) ===
    // Grenouilles (vraies entités Frog avec glow)
    private final Frog[] frogEntities = new Frog[TOTAL_FROGS];
    private final Location[] frogLocations = new Location[TOTAL_FROGS];
    // Biologiste NPC
    private Villager biologistNPC;
    private TextDisplay biologistDisplay;
    // Joueurs actifs sur la quête des grenouilles
    private final Set<UUID> activeFrogPlayers = ConcurrentHashMap.newKeySet();
    // Grenouilles capturées par chaque joueur (Set des index)
    private final Map<UUID, Set<Integer>> playerCapturedFrogs = new ConcurrentHashMap<>();
    // Nombre de grenouilles dans l'inventaire par joueur
    private final Map<UUID, Integer> playerFrogsInInventory = new ConcurrentHashMap<>();
    // GUI du mini-jeu de capture
    private FrogCaptureGUI frogCaptureGUI;

    // === TRACKING ÉNIGMES (Étape 5.8) ===
    // Oracle NPC
    private Villager oracleNPC;
    private TextDisplay oracleDisplay;
    // Joueurs actifs sur la quête des énigmes
    private final Set<UUID> activeRiddlePlayers = ConcurrentHashMap.newKeySet();
    // Énigme actuelle par joueur (0, 1, 2)
    private final Map<UUID, Integer> playerCurrentRiddle = new ConcurrentHashMap<>();

    // === TRACKING BOSS GRENOUILLE (Étape 5.10) ===
    private NamespacedKey SWAMP_FROG_BOSS_KEY;
    private Zombie swampFrogBossEntity;
    private TextDisplay swampFrogBossDisplay;
    private final Set<UUID> bossContributors = ConcurrentHashMap.newKeySet();
    private boolean bossRespawnScheduled = false;
    private long bossRespawnTime = 0;

    public Chapter5Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.QUEST_SALMON_KEY = new NamespacedKey(plugin, "quest_salmon_ch5");
        this.ORE_VISUAL_KEY = new NamespacedKey(plugin, "quest_ore_visual_ch5");
        this.ORE_HITBOX_KEY = new NamespacedKey(plugin, "quest_ore_hitbox_ch5");
        this.SUSPECT_NPC_KEY = new NamespacedKey(plugin, "quest_suspect_ch5");
        this.TRAITOR_PILLAGER_KEY = new NamespacedKey(plugin, "quest_traitor_ch5");
        this.LUMBER_VISUAL_KEY = new NamespacedKey(plugin, "quest_lumber_visual_ch5");
        this.LUMBER_HITBOX_KEY = new NamespacedKey(plugin, "quest_lumber_hitbox_ch5");
        this.LUMBERJACK_NPC_KEY = new NamespacedKey(plugin, "quest_lumberjack_ch5");
        this.FROG_VISUAL_KEY = new NamespacedKey(plugin, "quest_frog_visual_ch5");
        this.FROG_HITBOX_KEY = new NamespacedKey(plugin, "quest_frog_hitbox_ch5");
        this.BIOLOGIST_NPC_KEY = new NamespacedKey(plugin, "quest_biologist_ch5");
        this.ORACLE_NPC_KEY = new NamespacedKey(plugin, "quest_oracle_ch5");
        this.SWAMP_FROG_BOSS_KEY = new NamespacedKey(plugin, "swamp_frog_boss_ch5");

        // Initialiser le GUI du mini-jeu de grenouilles
        this.frogCaptureGUI = new FrogCaptureGUI(plugin);

        // Enregistrer les événements
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Démarrer les systèmes
        startSalmonSpawnTask();

        // Initialiser les minerais et suspects après un délai (attendre que le monde soit chargé)
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    initializeOres(world);
                    startOreVisibilityUpdater();
                    startOreRespawnChecker();

                    // Système du traître
                    initializeSuspectGlowTeam();
                    initializeSuspects(world);
                    startSuspectVisibilityUpdater();
                    startSuspectRespawnChecker();

                    // Système de bûcheronnage
                    initializeLumber(world);
                    initializeLumberjack(world);
                    startLumberVisibilityUpdater();
                    startLumberRespawnChecker();

                    // Système des grenouilles
                    initializeFrogs(world);
                    initializeBiologist(world);
                    startFrogVisibilityUpdater();
                    startFrogRespawnChecker();

                    // Système des énigmes
                    initializeOracle(world);
                    startOracleRespawnChecker();

                    // Système du boss Grenouille Géante
                    initializeSwampFrogBoss(world);
                    startBossRespawnChecker();
                }
            }
        }.runTaskLater(plugin, 100L);

        plugin.getLogger().info("[Chapter5Systems] Systèmes du Chapitre 5 initialisés");
    }

    // ==================== SYSTÈME DE PÊCHE AUX SAUMONS (Étape 5.2) ====================

    private void startSalmonSpawnTask() {
        salmonSpawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSalmonPlayers.isEmpty()) {
                    return;
                }

                // Vérifier si AU MOINS un joueur est réellement proche de la zone
                boolean anyPlayerNearby = false;
                World targetWorld = null;

                for (UUID playerId : activeSalmonPlayers) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline() && isPlayerNearSalmonZone(player)) {
                        anyPlayerNearby = true;
                        targetWorld = player.getWorld();
                        break;
                    }
                }

                if (!anyPlayerNearby || targetWorld == null) {
                    return;
                }

                // Compter les saumons une seule fois par tick (optimisation)
                int currentSalmons = countActiveSalmons();

                // Limite de sécurité absolue : si > 2x max, nettoyer les excédents
                if (currentSalmons > MAX_SALMONS_IN_ZONE * 2) {
                    plugin.getLogger().warning("[Chapter5] Trop de saumons détectés (" + currentSalmons + "), nettoyage...");
                    cleanupExcessSalmons(targetWorld, MAX_SALMONS_IN_ZONE);
                    return;
                }

                if (currentSalmons >= MAX_SALMONS_IN_ZONE) {
                    return;
                }

                // Spawner UN SEUL saumon par tick (pas un par joueur)
                spawnSalmon(targetWorld);
            }
        }.runTaskTimer(plugin, 20L, SPAWN_INTERVAL_TICKS);
    }

    /**
     * Nettoie les saumons excédentaires dans la zone (sécurité anti-accumulation)
     */
    private void cleanupExcessSalmons(World world, int maxToKeep) {
        Location zoneCenter = new Location(world, SALMON_ZONE_CENTER_X, SALMON_ZONE_Y, SALMON_ZONE_CENTER_Z);
        List<Entity> salmonsToRemove = new java.util.ArrayList<>();

        for (Entity entity : world.getNearbyEntities(zoneCenter, 80, 40, 80)) {
            if (entity instanceof Salmon salmon) {
                if (salmon.getPersistentDataContainer().has(QUEST_SALMON_KEY, PersistentDataType.BYTE)) {
                    salmonsToRemove.add(salmon);
                }
            }
        }

        // Garder seulement maxToKeep, supprimer le reste
        if (salmonsToRemove.size() > maxToKeep) {
            for (int i = maxToKeep; i < salmonsToRemove.size(); i++) {
                salmonsToRemove.get(i).remove();
            }
            plugin.getLogger().info("[Chapter5] " + (salmonsToRemove.size() - maxToKeep) + " saumons excédentaires supprimés.");
        }

        activeSalmons.clear();
    }

    private boolean isPlayerNearSalmonZone(Player player) {
        Location loc = player.getLocation();
        double distanceSquared = Math.pow(loc.getX() - SALMON_ZONE_CENTER_X, 2) +
                                 Math.pow(loc.getZ() - SALMON_ZONE_CENTER_Z, 2);
        return distanceSquared < 10000;
    }

    /**
     * Compte les saumons mutants RÉELLEMENT présents dans la zone via PDC.
     * Cette méthode est robuste aux chunks non chargés car elle scanne
     * uniquement les entités chargées dans la zone.
     */
    private int countActiveSalmons() {
        World world = plugin.getServer().getWorld("world");
        if (world == null) {
            return 0;
        }

        // Vérifier si le chunk de la zone est chargé
        Location zoneCenter = new Location(world, SALMON_ZONE_CENTER_X, SALMON_ZONE_Y, SALMON_ZONE_CENTER_Z);
        if (!zoneCenter.getChunk().isLoaded()) {
            // Chunk non chargé = pas de joueur à proximité = pas besoin de spawner
            return MAX_SALMONS_IN_ZONE; // Retourne max pour bloquer le spawn
        }

        int count = 0;
        double radius = 60; // Rayon de recherche (zone + marge)

        for (Entity entity : world.getNearbyEntities(zoneCenter, radius, 30, radius)) {
            if (entity instanceof Salmon salmon) {
                if (salmon.getPersistentDataContainer().has(QUEST_SALMON_KEY, PersistentDataType.BYTE)) {
                    count++;
                }
            }
        }

        // Synchroniser la Map activeSalmons avec les entités réelles trouvées
        // Nettoyer les entrées obsolètes
        activeSalmons.keySet().removeIf(uuid -> {
            Entity entity = plugin.getServer().getEntity(uuid);
            // Retirer seulement si l'entité est confirmée morte, pas juste null
            return entity != null && (!entity.isValid() || entity.isDead());
        });

        return count;
    }

    private void spawnSalmon(World world) {
        Location spawnLoc = findWaterSpawnLocation(world);
        if (spawnLoc == null) {
            return;
        }

        ZombieManager zombieManager = plugin.getZombieManager();
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(
            ZombieType.QUEST_SALMON,
            spawnLoc,
            SALMON_LEVEL
        );

        if (activeZombie != null) {
            Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
            if (entity instanceof LivingEntity salmon) {
                salmon.getPersistentDataContainer().set(QUEST_SALMON_KEY, PersistentDataType.BYTE, (byte) 1);
                activeSalmons.put(entity.getUniqueId(), null);
                world.spawnParticle(Particle.BUBBLE_COLUMN_UP, spawnLoc, 20, 0.5, 0.5, 0.5, 0);
                world.playSound(spawnLoc, Sound.ENTITY_SALMON_FLOP, 1.0f, 0.8f);
            }
        }
    }

    private Location findWaterSpawnLocation(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 15; attempt++) {
            int x = random.nextInt(SALMON_ZONE_MIN_X, SALMON_ZONE_MAX_X + 1);
            int z = random.nextInt(SALMON_ZONE_MIN_Z, SALMON_ZONE_MAX_Z + 1);

            for (int y = SALMON_ZONE_Y + 5; y >= SALMON_ZONE_Y - 10; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (above.getType() == Material.WATER || above.getType() == Material.AIR) {
                        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
                    }
                }
            }
        }

        return null;
    }

    public void activateSalmonQuest(Player player) {
        UUID playerId = player.getUniqueId();
        activeSalmonPlayers.add(playerId);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                player.sendTitle(
                    "§b\uD83D\uDC1F §3§lPÊCHE MUTANTE §b\uD83D\uDC1F",
                    "§7Éliminez les saumons contaminés!",
                    10, 60, 20
                );

                player.playSound(player.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);

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

                activateGPSToFishingZone(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    private void activateGPSToFishingZone(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + SALMON_ZONE_CENTER_X + ", " + SALMON_ZONE_Y + ", " + SALMON_ZONE_CENTER_Z + " §7(Zone de pêche)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSalmonDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Salmon salmon)) {
            return;
        }

        if (!salmon.getPersistentDataContainer().has(QUEST_SALMON_KEY, PersistentDataType.BYTE)) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        activeSalmons.remove(entity.getUniqueId());

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep != JourneyStep.STEP_5_2) {
            return;
        }

        int progress = journeyManager.getStepProgress(killer, currentStep);
        int newProgress = progress + 1;
        journeyManager.setStepProgress(killer, currentStep, newProgress);

        Location loc = entity.getLocation();
        killer.getWorld().spawnParticle(Particle.BUBBLE_POP, loc, 30, 0.5, 0.5, 0.5, 0.1);
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        journeyManager.createOrUpdateBossBar(killer);

        if (currentStep.isCompleted(newProgress)) {
            completeSalmonQuest(killer);
        }
    }

    private void completeSalmonQuest(Player player) {
        UUID playerId = player.getUniqueId();
        activeSalmonPlayers.remove(playerId);
        cleanupPlayerSalmons();

        journeyManager.completeStep(player, JourneyStep.STEP_5_2);

        player.sendTitle(
            "§a§l\u2713 QUÊTE TERMINÉE!",
            "§7Tous les saumons mutants ont été éliminés!",
            10, 60, 20
        );

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    private void cleanupPlayerSalmons() {
        for (UUID salmonId : new HashSet<>(activeSalmons.keySet())) {
            Entity entity = plugin.getServer().getEntity(salmonId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeSalmons.clear();
    }

    // ==================== SYSTÈME DE MINAGE (Étape 5.3) ====================

    /**
     * Initialise les minerais dans la zone de minage
     */
    private void initializeOres(World world) {
        // Générer les positions des minerais
        generateOreLocations(world);

        // Spawner les minerais
        for (int i = 0; i < TOTAL_ORES; i++) {
            if (oreLocations[i] != null) {
                spawnOre(world, i);
            }
        }

        plugin.getLogger().info("[Chapter5Systems] " + TOTAL_ORES + " minerais initialisés dans la zone de minage");
    }

    /**
     * Génère les positions des minerais dans la zone
     */
    private void generateOreLocations(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        int maxAttempts = TOTAL_ORES * 50;
        int attempts = 0;

        while (spawned < TOTAL_ORES && attempts < maxAttempts) {
            attempts++;

            int x = random.nextInt(MINE_ZONE_MIN_X, MINE_ZONE_MAX_X + 1);
            int y = random.nextInt(MINE_ZONE_MIN_Y, MINE_ZONE_MAX_Y + 1);
            int z = random.nextInt(MINE_ZONE_MIN_Z, MINE_ZONE_MAX_Z + 1);

            Block block = world.getBlockAt(x, y, z);
            Block below = world.getBlockAt(x, y - 1, z);
            Block above = world.getBlockAt(x, y + 1, z);

            // Vérifier que le bloc actuel est de l'air et que le bloc en dessous est STONE
            if (block.getType() == Material.AIR &&
                below.getType() == Material.STONE &&
                above.getType() == Material.AIR) {

                // Vérifier qu'il n'y a pas déjà un minerai trop proche
                boolean tooClose = false;
                for (int i = 0; i < spawned; i++) {
                    if (oreLocations[i] != null) {
                        double distSq = oreLocations[i].distanceSquared(new Location(world, x, y, z));
                        if (distSq < 9) { // 3 blocs minimum entre les minerais
                            tooClose = true;
                            break;
                        }
                    }
                }

                if (!tooClose) {
                    oreLocations[spawned] = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                    oreTypes[spawned] = OreType.random();
                    spawned++;
                }
            }
        }

        // Si on n'a pas assez de positions, compléter avec des positions de fallback
        if (spawned < TOTAL_ORES) {
            plugin.getLogger().warning("[Chapter5Systems] Seulement " + spawned + "/" + TOTAL_ORES +
                " positions de minerais trouvées. Recherche de positions alternatives...");

            // Fallback: placer sur n'importe quel bloc solide
            while (spawned < TOTAL_ORES && attempts < maxAttempts * 2) {
                attempts++;

                int x = random.nextInt(MINE_ZONE_MIN_X, MINE_ZONE_MAX_X + 1);
                int z = random.nextInt(MINE_ZONE_MIN_Z, MINE_ZONE_MAX_Z + 1);

                // Chercher le premier bloc solide depuis le haut
                for (int y = MINE_ZONE_MAX_Y; y >= MINE_ZONE_MIN_Y; y--) {
                    Block block = world.getBlockAt(x, y, z);
                    Block above = world.getBlockAt(x, y + 1, z);

                    if (block.getType().isSolid() && !block.getType().isAir() && above.getType() == Material.AIR) {
                        Location loc = new Location(world, x + 0.5, y + 1.5, z + 0.5);

                        boolean tooClose = false;
                        for (int i = 0; i < spawned; i++) {
                            if (oreLocations[i] != null && oreLocations[i].distanceSquared(loc) < 9) {
                                tooClose = true;
                                break;
                            }
                        }

                        if (!tooClose) {
                            oreLocations[spawned] = loc;
                            oreTypes[spawned] = OreType.random();
                            spawned++;
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Spawn un minerai à l'index donné
     */
    private void spawnOre(World world, int oreIndex) {
        Location loc = oreLocations[oreIndex];
        if (loc == null) return;

        OreType oreType = oreTypes[oreIndex];
        if (oreType == null) {
            oreType = OreType.random();
            oreTypes[oreIndex] = oreType;
        }

        // Supprimer les anciens
        if (oreVisuals[oreIndex] != null && oreVisuals[oreIndex].isValid()) {
            oreVisuals[oreIndex].remove();
        }
        if (oreHitboxes[oreIndex] != null && oreHitboxes[oreIndex].isValid()) {
            oreHitboxes[oreIndex].remove();
        }
        if (oreDisplays[oreIndex] != null && oreDisplays[oreIndex].isValid()) {
            oreDisplays[oreIndex].remove();
        }

        final OreType finalOreType = oreType;

        // 1. Créer le VISUEL (ItemDisplay avec le minerai glowing)
        oreVisuals[oreIndex] = world.spawn(loc.clone(), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(finalOreType.material));

            // Taille légèrement plus grande pour visibilité
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 1, 0)));

            display.setBillboard(Display.Billboard.FIXED);

            // Glow effect avec la couleur du minerai
            display.setGlowing(true);
            display.setGlowColorOverride(finalOreType.glowColor);

            display.setViewRange(ORE_VIEW_DISTANCE);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter5_ore_visual");
            display.addScoreboardTag("ore_visual_" + oreIndex);

            // PDC
            display.getPersistentDataContainer().set(ORE_VISUAL_KEY, PersistentDataType.INTEGER, oreIndex);
        });

        // 2. Créer l'entité INTERACTION (hitbox cliquable/frappable)
        oreHitboxes[oreIndex] = world.spawn(loc.clone(), Interaction.class, interaction -> {
            interaction.setInteractionWidth(1.2f);
            interaction.setInteractionHeight(1.2f);
            interaction.setResponsive(true);

            // Tags
            interaction.addScoreboardTag("chapter5_ore_hitbox");
            interaction.addScoreboardTag("ore_hitbox_" + oreIndex);
            interaction.addScoreboardTag("zombiez_npc");

            // PDC
            interaction.getPersistentDataContainer().set(ORE_HITBOX_KEY, PersistentDataType.INTEGER, oreIndex);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });

        // 3. Créer le TextDisplay au-dessus
        createOreDisplay(world, loc, oreIndex, oreType);
    }

    /**
     * Crée le TextDisplay au-dessus d'un minerai
     */
    private void createOreDisplay(World world, Location loc, int oreIndex, OreType oreType) {
        Location displayLoc = loc.clone().add(0, ORE_DISPLAY_HEIGHT, 0);

        oreDisplays[oreIndex] = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("⛏ ", NamedTextColor.GOLD))
                    .append(Component.text(oreType.displayName, NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" ⛏", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("▶ Frappe pour miner", NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.3f, 1.3f, 1.3f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.4f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter5_ore_display");
            display.addScoreboardTag("ore_display_" + oreIndex);

            // Invisible par défaut
            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour les minerais
     */
    private void startOreVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllOresForPlayer(player);
                        continue;
                    }

                    // Vérifier si le joueur est sur la quête et ne l'a pas terminée
                    boolean shouldSeeOres = isPlayerOnMiningQuest(player) && !hasPlayerCompletedMiningQuest(player);

                    if (shouldSeeOres) {
                        updateOreVisibilityForPlayer(player);
                    } else {
                        hideAllOresForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des minerais pour un joueur
     */
    private void updateOreVisibilityForPlayer(Player player) {
        Set<Integer> minedOres = playerMinedOres.getOrDefault(player.getUniqueId(), Set.of());

        for (int i = 0; i < TOTAL_ORES; i++) {
            boolean hasMined = minedOres.contains(i);

            // Distance check
            boolean inRange = false;
            if (oreVisuals[i] != null && oreVisuals[i].isValid()) {
                double distSq = player.getLocation().distanceSquared(oreVisuals[i].getLocation());
                inRange = distSq <= ORE_VIEW_DISTANCE * ORE_VIEW_DISTANCE;
            }

            // Visual (ORE block)
            if (oreVisuals[i] != null && oreVisuals[i].isValid()) {
                if (hasMined || !inRange) {
                    player.hideEntity(plugin, oreVisuals[i]);
                } else {
                    player.showEntity(plugin, oreVisuals[i]);
                }
            }

            // Hitbox (Interaction)
            if (oreHitboxes[i] != null && oreHitboxes[i].isValid()) {
                if (hasMined || !inRange) {
                    player.hideEntity(plugin, oreHitboxes[i]);
                } else {
                    player.showEntity(plugin, oreHitboxes[i]);
                }
            }

            // TextDisplay
            if (oreDisplays[i] != null && oreDisplays[i].isValid()) {
                if (hasMined || !inRange) {
                    player.hideEntity(plugin, oreDisplays[i]);
                } else {
                    player.showEntity(plugin, oreDisplays[i]);
                }
            }
        }
    }

    /**
     * Cache tous les minerais pour un joueur
     */
    private void hideAllOresForPlayer(Player player) {
        for (int i = 0; i < TOTAL_ORES; i++) {
            if (oreVisuals[i] != null && oreVisuals[i].isValid()) {
                player.hideEntity(plugin, oreVisuals[i]);
            }
            if (oreHitboxes[i] != null && oreHitboxes[i].isValid()) {
                player.hideEntity(plugin, oreHitboxes[i]);
            }
            if (oreDisplays[i] != null && oreDisplays[i].isValid()) {
                player.hideEntity(plugin, oreDisplays[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des minerais
     */
    private void startOreRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < TOTAL_ORES; i++) {
                    if (oreLocations[i] == null) continue;

                    boolean needsRespawn = (oreVisuals[i] == null || !oreVisuals[i].isValid()) ||
                            (oreHitboxes[i] == null || !oreHitboxes[i].isValid()) ||
                            (oreDisplays[i] == null || !oreDisplays[i].isValid());

                    if (needsRespawn) {
                        spawnOre(world, i);
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur est sur la quête de minage
     */
    private boolean isPlayerOnMiningQuest(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        return currentStep == JourneyStep.STEP_5_3;
    }

    /**
     * Vérifie si un joueur a terminé la quête de minage
     */
    private boolean hasPlayerCompletedMiningQuest(Player player) {
        return journeyManager.isStepCompleted(player, JourneyStep.STEP_5_3);
    }

    /**
     * Vérifie si un joueur a miné un minerai spécifique
     */
    private boolean hasPlayerMinedOre(Player player, int oreIndex) {
        Set<Integer> mined = playerMinedOres.get(player.getUniqueId());
        return mined != null && mined.contains(oreIndex);
    }

    /**
     * Initialise la progression de minage d'un joueur
     */
    private void initializePlayerMiningProgress(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerOreHits.containsKey(uuid)) {
            playerOreHits.put(uuid, new int[TOTAL_ORES]);
        }
        if (!playerMinedOres.containsKey(uuid)) {
            playerMinedOres.put(uuid, ConcurrentHashMap.newKeySet());
        }
    }

    /**
     * Gère le hit sur un minerai
     */
    private void handleOreHit(Player player, int oreIndex) {
        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnMiningQuest(player)) {
            player.sendMessage("§7Un filon de minerai... La quête de minage n'est pas encore active.");
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedMiningQuest(player)) {
            return;
        }

        // Vérifier si ce minerai est déjà miné
        if (hasPlayerMinedOre(player, oreIndex)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int[] hits = playerOreHits.get(uuid);
        if (hits == null) {
            initializePlayerMiningProgress(player);
            hits = playerOreHits.get(uuid);
        }

        // Incrémenter les hits
        hits[oreIndex]++;
        int currentHits = hits[oreIndex];

        // Afficher la progression dans le TextDisplay du minerai
        updateOreDisplayProgress(oreIndex, currentHits, HITS_TO_MINE);

        // Effets de minage
        Location oreLoc = oreLocations[oreIndex].clone();
        OreType oreType = oreTypes[oreIndex];

        double progress = (double) currentHits / HITS_TO_MINE;
        player.playSound(oreLoc, Sound.BLOCK_STONE_BREAK, 0.8f, 0.8f + (float) (progress * 0.4));
        player.getWorld().spawnParticle(Particle.BLOCK, oreLoc, 10, 0.5, 0.3, 0.5,
                oreType.material.createBlockData());

        // Minerai miné!
        if (currentHits >= HITS_TO_MINE) {
            onOreMined(player, oreIndex);
        }
    }

    /**
     * Met à jour le TextDisplay d'un minerai avec la progression
     */
    private void updateOreDisplayProgress(int oreIndex, int currentHits, int maxHits) {
        TextDisplay display = oreDisplays[oreIndex];
        if (display == null || !display.isValid()) return;

        OreType oreType = oreTypes[oreIndex];
        int remaining = maxHits - currentHits;

        // Couleur basée sur la progression
        NamedTextColor healthColor;
        double healthPercent = (double) remaining / maxHits;
        if (healthPercent > 0.6) {
            healthColor = NamedTextColor.GREEN;
        } else if (healthPercent > 0.3) {
            healthColor = NamedTextColor.YELLOW;
        } else {
            healthColor = NamedTextColor.RED;
        }

        display.text(Component.text()
                .append(Component.text("⛏ ", NamedTextColor.GOLD))
                .append(Component.text(oreType.displayName, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" ⛏", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text(remaining, healthColor, TextDecoration.BOLD))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.text(maxHits, NamedTextColor.GRAY))
                .append(Component.text(" ❤", NamedTextColor.RED))
                .build());
    }

    /**
     * Appelé quand un minerai est miné
     */
    private void onOreMined(Player player, int oreIndex) {
        UUID uuid = player.getUniqueId();

        // Marquer ce minerai comme miné pour ce joueur
        playerMinedOres.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(oreIndex);

        Location oreLoc = oreLocations[oreIndex].clone();
        OreType oreType = oreTypes[oreIndex];

        // Cacher le minerai pour ce joueur
        updateOreVisibilityForPlayer(player);

        // Incrémenter la progression
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_5_3);
        int newProgress = progress + 1;
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_3, newProgress);

        // Effets de découverte
        player.playSound(oreLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.2f);
        player.playSound(oreLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, oreLoc, 15, 0.5, 0.5, 0.5, 0);

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);

        // Vérifier si la quête est complète
        if (newProgress >= ORES_TO_MINE) {
            completeMiningQuest(player);
        }
    }

    /**
     * Termine la quête de minage
     */
    private void completeMiningQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Retirer des joueurs actifs
        activeMiningPlayers.remove(playerId);

        // Nettoyer les données du joueur
        playerOreHits.remove(playerId);
        playerMinedOres.remove(playerId);

        // Cacher tous les minerais
        hideAllOresForPlayer(player);

        // Compléter l'étape
        journeyManager.completeStep(player, JourneyStep.STEP_5_3);

        // Message de victoire
        player.sendTitle(
            "§a§l⛏ EXTRACTION TERMINÉE!",
            "§7Vous avez collecté assez de minerais!",
            10, 60, 20
        );

        // Son de victoire
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Particules
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    /**
     * Active la quête de minage pour un joueur
     */
    public void activateMiningQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Initialiser le tracking
        initializePlayerMiningProgress(player);
        activeMiningPlayers.add(playerId);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§6⛏ §e§lEXTRACTION MINIÈRE §6⛏",
                    "§7Des filons précieux vous attendent!",
                    10, 60, 20
                );

                // Son d'ambiance
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§6§l══════ EXTRACTION MINIÈRE ══════");
                player.sendMessage("");
                player.sendMessage("§7Une §emine abandonnée §7regorge de §bminerais précieux§7.");
                player.sendMessage("§7Les filons brillent d'une lueur mystique...");
                player.sendMessage("");
                player.sendMessage("§e▸ §fMinez §c" + ORES_TO_MINE + " minerais §fprécieux");
                player.sendMessage("§e▸ §fFrappez les blocs lumineux");
                player.sendMessage("§e▸ §c⚠ Si vous mourez, vous devrez recommencer!");
                player.sendMessage("");

                // Activer le GPS
                activateGPSToMine(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers la mine
     */
    private void activateGPSToMine(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + MINE_ZONE_CENTER_X + ", " + MINE_ZONE_CENTER_Y + ", " + MINE_ZONE_CENTER_Z + " §7(Mine abandonnée)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Reset la progression de minage pour un joueur (appelé à la mort)
     */
    private void resetMiningProgress(Player player) {
        UUID uuid = player.getUniqueId();

        // Réinitialiser les hits
        playerOreHits.put(uuid, new int[TOTAL_ORES]);

        // Réinitialiser les minerais minés
        playerMinedOres.put(uuid, ConcurrentHashMap.newKeySet());

        // Réinitialiser la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_3, 0);

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_2
     */
    public void onPlayerReachStep52(Player player) {
        activateSalmonQuest(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_3
     */
    public void onPlayerReachStep53(Player player) {
        activateMiningQuest(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_5
     */
    public void onPlayerReachStep55(Player player) {
        activateTraitorQuest(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_6
     */
    public void onPlayerReachStep56(Player player) {
        activateLumberQuest(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_7
     */
    public void onPlayerReachStep57(Player player) {
        activateFrogQuest(player);
    }

    /**
     * Appelé quand un joueur arrive à l'étape STEP_5_8
     */
    public void onPlayerReachStep58(Player player) {
        activateRiddleQuest(player);
    }

    // ==================== SYSTÈME DU TRAÎTRE (Étape 5.5) ====================

    /**
     * Initialise la team pour le glow vert des suspects
     */
    private void initializeSuspectGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        suspectGlowTeam = scoreboard.getTeam("ch5_suspects");
        if (suspectGlowTeam == null) {
            suspectGlowTeam = scoreboard.registerNewTeam("ch5_suspects");
        }
        suspectGlowTeam.color(NamedTextColor.GREEN);
        suspectGlowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    /**
     * Initialise les suspects dans le village
     */
    private void initializeSuspects(World world) {
        for (int i = 0; i < TOTAL_SUSPECTS; i++) {
            spawnSuspect(world, i);
        }
        plugin.getLogger().info("[Chapter5Systems] " + TOTAL_SUSPECTS + " suspects initialisés à Maraisville");
    }

    /**
     * Spawn un suspect à l'index donné
     */
    private void spawnSuspect(World world, int suspectIndex) {
        double[] coords = SUSPECT_LOCATIONS[suspectIndex];
        Location loc = new Location(world, coords[0], coords[1], coords[2], (float) coords[3], (float) coords[4]);

        // Supprimer l'ancien si existant
        if (suspectNPCs[suspectIndex] != null && suspectNPCs[suspectIndex].isValid()) {
            suspectGlowTeam.removeEntity(suspectNPCs[suspectIndex]);
            suspectNPCs[suspectIndex].remove();
        }
        if (suspectDisplays[suspectIndex] != null && suspectDisplays[suspectIndex].isValid()) {
            suspectDisplays[suspectIndex].remove();
        }

        final int index = suspectIndex;

        // Créer le Villager suspect
        suspectNPCs[suspectIndex] = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text(SUSPECT_NAMES[index]));
            villager.setCustomNameVisible(false); // On utilise TextDisplay
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCollidable(false);

            // Profession variée
            Villager.Profession[] professions = {
                Villager.Profession.CLERIC,
                Villager.Profession.CARTOGRAPHER,
                Villager.Profession.WEAPONSMITH,
                Villager.Profession.FARMER,
                Villager.Profession.NITWIT
            };
            villager.setProfession(professions[index]);
            villager.setVillagerLevel(2);

            // Glow vert
            villager.setGlowing(true);

            // Tags
            villager.addScoreboardTag("chapter5_suspect");
            villager.addScoreboardTag("suspect_" + index);
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(SUSPECT_NPC_KEY, PersistentDataType.INTEGER, index);

            // Invisible par défaut
            villager.setVisibleByDefault(false);
        });

        // Ajouter à la team pour le glow vert
        if (suspectGlowTeam != null && suspectNPCs[suspectIndex] != null) {
            suspectGlowTeam.addEntity(suspectNPCs[suspectIndex]);
        }

        // Créer le TextDisplay au-dessus
        Location displayLoc = loc.clone().add(0, 2.5, 0);
        suspectDisplays[suspectIndex] = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("§e🔍 ", NamedTextColor.YELLOW))
                    .append(Component.text(SUSPECT_NAMES[index].replace("§e§l", ""), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" §e🔍", NamedTextColor.YELLOW))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour interroger", NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.3f);
            display.setPersistent(true);
            display.addScoreboardTag("chapter5_suspect_display");
            display.addScoreboardTag("suspect_display_" + index);

            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour les suspects
     */
    private void startSuspectVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllSuspectsForPlayer(player);
                        continue;
                    }

                    // Vérifier si le joueur est sur la quête et ne l'a pas terminée
                    boolean shouldSeeSuspects = isPlayerOnTraitorQuest(player) && !hasPlayerCompletedTraitorQuest(player);

                    if (shouldSeeSuspects) {
                        updateSuspectVisibilityForPlayer(player);
                    } else {
                        hideAllSuspectsForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des suspects pour un joueur
     */
    private void updateSuspectVisibilityForPlayer(Player player) {
        Set<Integer> interrogated = playerInterrogatedSuspects.getOrDefault(player.getUniqueId(), Set.of());

        for (int i = 0; i < TOTAL_SUSPECTS; i++) {
            boolean hasInterrogated = interrogated.contains(i);

            // Villager
            if (suspectNPCs[i] != null && suspectNPCs[i].isValid()) {
                if (hasInterrogated) {
                    player.hideEntity(plugin, suspectNPCs[i]);
                } else {
                    player.showEntity(plugin, suspectNPCs[i]);
                }
            }

            // TextDisplay
            if (suspectDisplays[i] != null && suspectDisplays[i].isValid()) {
                if (hasInterrogated) {
                    player.hideEntity(plugin, suspectDisplays[i]);
                } else {
                    player.showEntity(plugin, suspectDisplays[i]);
                }
            }
        }
    }

    /**
     * Cache tous les suspects pour un joueur
     */
    private void hideAllSuspectsForPlayer(Player player) {
        for (int i = 0; i < TOTAL_SUSPECTS; i++) {
            if (suspectNPCs[i] != null && suspectNPCs[i].isValid()) {
                player.hideEntity(plugin, suspectNPCs[i]);
            }
            if (suspectDisplays[i] != null && suspectDisplays[i].isValid()) {
                player.hideEntity(plugin, suspectDisplays[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des suspects
     */
    private void startSuspectRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < TOTAL_SUSPECTS; i++) {
                    boolean needsRespawn = (suspectNPCs[i] == null || !suspectNPCs[i].isValid()) ||
                            (suspectDisplays[i] == null || !suspectDisplays[i].isValid());

                    if (needsRespawn) {
                        spawnSuspect(world, i);
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur est sur la quête du traître
     */
    private boolean isPlayerOnTraitorQuest(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        return currentStep == JourneyStep.STEP_5_5;
    }

    /**
     * Vérifie si un joueur a terminé la quête du traître
     */
    private boolean hasPlayerCompletedTraitorQuest(Player player) {
        return journeyManager.isStepCompleted(player, JourneyStep.STEP_5_5);
    }

    /**
     * Active la quête du traître pour un joueur
     */
    public void activateTraitorQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Initialiser le tracking
        playerInterrogatedSuspects.put(playerId, ConcurrentHashMap.newKeySet());
        activeTraitorPlayers.add(playerId);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§c⚔ §4§lTRAQUE DU TRAÎTRE §c⚔",
                    "§7Un espion se cache parmi les habitants...",
                    10, 60, 20
                );

                // Son d'ambiance
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.8f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§4§l══════ TRAQUE DU TRAÎTRE ══════");
                player.sendMessage("");
                player.sendMessage("§7Un §ctraître §7s'est infiltré dans le refuge de");
                player.sendMessage("§e§lMaraisville§7. Il communique avec les morts-vivants...");
                player.sendMessage("");
                player.sendMessage("§e▸ §fInterrogez les §c5 suspects §fdu village");
                player.sendMessage("§e▸ §fLe dernier interrogé révélera sa vraie nature!");
                player.sendMessage("§e▸ §c⚠ Si vous mourez, vous devrez recommencer!");
                player.sendMessage("");

                // Activer le GPS vers le premier suspect
                updateGPSToNextSuspect(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Met à jour le GPS vers le prochain suspect non interrogé
     */
    private void updateGPSToNextSuspect(Player player) {
        Set<Integer> interrogated = playerInterrogatedSuspects.getOrDefault(player.getUniqueId(), Set.of());

        // Trouver le prochain suspect non interrogé
        for (int i = 0; i < TOTAL_SUSPECTS; i++) {
            if (!interrogated.contains(i)) {
                double[] coords = SUSPECT_LOCATIONS[i];
                String suspectName = SUSPECT_NAMES[i].replace("§e§l", "");
                player.sendMessage("§e§l➤ §7GPS: §b" + (int) coords[0] + ", " + (int) coords[1] + ", " + (int) coords[2] + " §7(" + suspectName + ")");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

                var gpsManager = plugin.getGPSManager();
                if (gpsManager != null) {
                    gpsManager.enableGPSSilently(player);
                }
                return;
            }
        }
    }

    /**
     * Gère l'interrogation d'un suspect
     */
    private void handleSuspectInterrogation(Player player, int suspectIndex) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnTraitorQuest(player)) {
            return;
        }

        // Vérifier si déjà interrogé
        Set<Integer> interrogated = playerInterrogatedSuspects.get(playerId);
        if (interrogated == null) {
            interrogated = ConcurrentHashMap.newKeySet();
            playerInterrogatedSuspects.put(playerId, interrogated);
        }

        if (interrogated.contains(suspectIndex)) {
            return;
        }

        // Marquer comme interrogé
        interrogated.add(suspectIndex);
        int totalInterrogated = interrogated.size();

        // Cacher ce suspect pour le joueur
        if (suspectNPCs[suspectIndex] != null && suspectNPCs[suspectIndex].isValid()) {
            player.hideEntity(plugin, suspectNPCs[suspectIndex]);
        }
        if (suspectDisplays[suspectIndex] != null && suspectDisplays[suspectIndex].isValid()) {
            player.hideEntity(plugin, suspectDisplays[suspectIndex]);
        }

        // Afficher le dialogue
        player.sendMessage("");
        player.sendMessage("§e§l[" + SUSPECT_NAMES[suspectIndex].replace("§e§l", "") + "]");
        player.sendMessage(SUSPECT_DIALOGUES[suspectIndex]);
        player.sendMessage("");

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);

        // Mettre à jour la progression
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_5, totalInterrogated);
        journeyManager.createOrUpdateBossBar(player);

        // Si c'est le 5ème suspect, il se transforme en traître!
        if (totalInterrogated >= 5) {
            spawnTraitor(player, suspectIndex);
        } else {
            // Mettre à jour le GPS vers le prochain suspect
            int remaining = 5 - totalInterrogated;
            player.sendMessage("§7[§e" + totalInterrogated + "/5§7] Suspects interrogés. §cEncore " + remaining + "...");
            updateGPSToNextSuspect(player);
        }
    }

    /**
     * Spawn le traître (Pillager) à la place du 5ème suspect
     */
    private void spawnTraitor(Player player, int suspectIndex) {
        double[] coords = SUSPECT_LOCATIONS[suspectIndex];
        World world = player.getWorld();
        Location loc = new Location(world, coords[0], coords[1], coords[2]);

        // Effets de transformation
        player.sendMessage("");
        player.sendMessage("§c§l[!!!] §4LE TRAÎTRE SE RÉVÈLE!");
        player.sendMessage("§7\"§cVous m'avez démasqué... mais vous ne quitterez pas ce village vivant!§7\"");
        player.sendMessage("");

        player.sendTitle(
            "§c☠ §4§lTRAÎTRE DÉMASQUÉ! §c☠",
            "§7Éliminez-le!",
            10, 40, 10
        );

        player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
        world.spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5, 0);
        world.spawnParticle(Particle.SMOKE, loc, 30, 0.5, 1, 0.5, 0.1);

        // Spawner le Pillager via ZombieManager
        new BukkitRunnable() {
            @Override
            public void run() {
                ZombieManager zombieManager = plugin.getZombieManager();
                ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(
                    ZombieType.PILLAGER,
                    loc,
                    TRAITOR_LEVEL
                );

                if (activeZombie != null) {
                    Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
                    if (entity instanceof LivingEntity traitor) {
                        // Marquer comme traître de quête
                        traitor.getPersistentDataContainer().set(TRAITOR_PILLAGER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
                        traitor.addScoreboardTag("chapter5_traitor");
                        traitor.addScoreboardTag("traitor_owner_" + player.getUniqueId());

                        // Stocker l'UUID du traître
                        playerActiveTraitors.put(player.getUniqueId(), entity.getUniqueId());

                        // Effets de spawn
                        world.playSound(loc, Sound.ENTITY_PILLAGER_CELEBRATE, 1.0f, 0.8f);

                        // GPS vers le traître
                        player.sendMessage("§c§l➤ §7Tuez le traître!");
                    }
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Gère la mort du traître
     */
    private void handleTraitorDeath(Player killer, Entity traitor) {
        UUID killerId = killer.getUniqueId();

        // Vérifier que c'est bien le traître de ce joueur
        UUID expectedTraitorId = playerActiveTraitors.get(killerId);
        if (expectedTraitorId == null || !expectedTraitorId.equals(traitor.getUniqueId())) {
            return;
        }

        // Nettoyer
        playerActiveTraitors.remove(killerId);
        activeTraitorPlayers.remove(killerId);
        playerInterrogatedSuspects.remove(killerId);

        // Compléter la quête
        journeyManager.setStepProgress(killer, JourneyStep.STEP_5_5, 6); // 5 + 1 pour le kill
        journeyManager.completeStep(killer, JourneyStep.STEP_5_5);

        // Effets de victoire
        killer.sendTitle(
            "§a§l✓ TRAÎTRE ÉLIMINÉ!",
            "§7Maraisville est en sécurité...",
            10, 60, 20
        );

        killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);

        killer.sendMessage("");
        killer.sendMessage("§a§l[QUÊTE TERMINÉE] §7Le traître a été éliminé!");
        killer.sendMessage("§7Les habitants de Maraisville vous remercient.");
        killer.sendMessage("");
    }

    /**
     * Reset la progression du traître pour un joueur (appelé à la mort)
     */
    private void resetTraitorProgress(Player player) {
        UUID uuid = player.getUniqueId();

        // Supprimer le traître actif s'il existe
        UUID traitorId = playerActiveTraitors.remove(uuid);
        if (traitorId != null) {
            Entity traitor = plugin.getServer().getEntity(traitorId);
            if (traitor != null && traitor.isValid()) {
                traitor.remove();
            }
        }

        // Réinitialiser les suspects interrogés
        playerInterrogatedSuspects.put(uuid, ConcurrentHashMap.newKeySet());

        // Réinitialiser la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_5, 0);

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);
    }

    // ==================== SYSTÈME DE BÛCHERONNAGE (Étape 5.6) ====================

    /**
     * Initialise les bois dans la zone de bûcheronnage
     */
    private void initializeLumber(World world) {
        // Générer les positions des bois
        generateLumberLocations(world);

        // Spawner les bois
        for (int i = 0; i < TOTAL_LUMBER; i++) {
            if (lumberLocations[i] != null) {
                spawnLumber(world, i);
            }
        }

        plugin.getLogger().info("[Chapter5Systems] " + TOTAL_LUMBER + " bois initialisés dans la zone forestière");
    }

    /**
     * Génère les positions des bois dans la zone
     */
    private void generateLumberLocations(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        int maxAttempts = TOTAL_LUMBER * 50;
        int attempts = 0;

        while (spawned < TOTAL_LUMBER && attempts < maxAttempts) {
            attempts++;

            int x = random.nextInt(LUMBER_ZONE_MIN_X, LUMBER_ZONE_MAX_X + 1);
            int z = random.nextInt(LUMBER_ZONE_MIN_Z, LUMBER_ZONE_MAX_Z + 1);

            // Chercher le premier bloc solide depuis le haut
            for (int y = LUMBER_ZONE_MAX_Y; y >= LUMBER_ZONE_MIN_Y; y--) {
                Block block = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);

                if (block.getType().isSolid() && !block.getType().isAir() && above.getType() == Material.AIR) {
                    Location loc = new Location(world, x + 0.5, y + 1.2, z + 0.5);

                    // Vérifier qu'il n'y a pas déjà un bois trop proche
                    boolean tooClose = false;
                    for (int i = 0; i < spawned; i++) {
                        if (lumberLocations[i] != null && lumberLocations[i].distanceSquared(loc) < 16) { // 4 blocs min
                            tooClose = true;
                            break;
                        }
                    }

                    if (!tooClose) {
                        lumberLocations[spawned] = loc;
                        spawned++;
                    }
                    break;
                }
            }
        }

        if (spawned < TOTAL_LUMBER) {
            plugin.getLogger().warning("[Chapter5Systems] Seulement " + spawned + "/" + TOTAL_LUMBER +
                " positions de bois trouvées");
        }
    }

    /**
     * Spawn un bois à l'index donné
     */
    private void spawnLumber(World world, int lumberIndex) {
        Location loc = lumberLocations[lumberIndex];
        if (loc == null) return;

        // Supprimer les anciens
        if (lumberVisuals[lumberIndex] != null && lumberVisuals[lumberIndex].isValid()) {
            lumberVisuals[lumberIndex].remove();
        }
        if (lumberHitboxes[lumberIndex] != null && lumberHitboxes[lumberIndex].isValid()) {
            lumberHitboxes[lumberIndex].remove();
        }

        // 1. Créer le VISUEL (ItemDisplay avec le bois glowing)
        lumberVisuals[lumberIndex] = world.spawn(loc.clone(), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.DARK_OAK_WOOD));

            // Taille et rotation pour ressembler à un rondin au sol
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0), // Couché sur le côté
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new AxisAngle4f(0, 0, 1, 0)));

            display.setBillboard(Display.Billboard.FIXED);

            // Glow effect marron/orange
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(139, 90, 43)); // Brun

            display.setViewRange(LUMBER_VIEW_DISTANCE);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter5_lumber_visual");
            display.addScoreboardTag("lumber_visual_" + lumberIndex);

            // PDC
            display.getPersistentDataContainer().set(LUMBER_VISUAL_KEY, PersistentDataType.INTEGER, lumberIndex);
        });

        // 2. Créer l'entité INTERACTION (hitbox cliquable)
        lumberHitboxes[lumberIndex] = world.spawn(loc.clone(), Interaction.class, interaction -> {
            interaction.setInteractionWidth(0.9f);
            interaction.setInteractionHeight(0.9f);
            interaction.setResponsive(true);

            // Tags
            interaction.addScoreboardTag("chapter5_lumber_hitbox");
            interaction.addScoreboardTag("lumber_hitbox_" + lumberIndex);
            interaction.addScoreboardTag("zombiez_npc");

            // PDC
            interaction.getPersistentDataContainer().set(LUMBER_HITBOX_KEY, PersistentDataType.INTEGER, lumberIndex);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });
    }

    /**
     * Initialise le bûcheron NPC
     */
    private void initializeLumberjack(World world) {
        Location loc = new Location(world, LUMBERJACK_X, LUMBERJACK_Y, LUMBERJACK_Z, LUMBERJACK_YAW, LUMBERJACK_PITCH);

        // Chercher si le bûcheron existe déjà
        for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof Villager v && v.getPersistentDataContainer().has(LUMBERJACK_NPC_KEY, PersistentDataType.BYTE)) {
                lumberjackNPC = v;
                // Chercher aussi le display
                for (Entity displayEntity : world.getNearbyEntities(loc.clone().add(0, 2.5, 0), 2, 2, 2)) {
                    if (displayEntity instanceof TextDisplay td && td.getScoreboardTags().contains("chapter5_lumberjack_display")) {
                        lumberjackDisplay = td;
                        break;
                    }
                }
                plugin.getLogger().info("[Chapter5Systems] Bûcheron existant trouvé");
                return;
            }
        }

        // Créer le bûcheron
        lumberjackNPC = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("§6§lBûcheron Aldric").decorate(TextDecoration.BOLD));
            villager.setCustomNameVisible(false);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCollidable(false);

            villager.setProfession(Villager.Profession.TOOLSMITH);
            villager.setVillagerLevel(3);

            // Tags
            villager.addScoreboardTag("chapter5_lumberjack");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(LUMBERJACK_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Visible par défaut pour ce NPC (il reste visible même après la quête)
            villager.setVisibleByDefault(true);
        });

        // Créer le TextDisplay au-dessus
        Location displayLoc = loc.clone().add(0, 2.5, 0);
        lumberjackDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("🪓 ", NamedTextColor.GOLD))
                    .append(Component.text("Bûcheron Aldric", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" 🪓", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour livrer", NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.3f);
            display.setPersistent(true);
            display.addScoreboardTag("chapter5_lumberjack_display");

            display.setVisibleByDefault(true);
        });

        plugin.getLogger().info("[Chapter5Systems] Bûcheron Aldric initialisé");
    }

    /**
     * Démarre le système de visibilité per-player pour les bois
     */
    private void startLumberVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllLumberForPlayer(player);
                        continue;
                    }

                    // Vérifier si le joueur est sur la quête et ne l'a pas terminée
                    boolean shouldSeeLumber = isPlayerOnLumberQuest(player) && !hasPlayerCompletedLumberQuest(player);

                    if (shouldSeeLumber) {
                        updateLumberVisibilityForPlayer(player);
                    } else {
                        hideAllLumberForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des bois pour un joueur
     */
    private void updateLumberVisibilityForPlayer(Player player) {
        Set<Integer> collectedLumber = playerCollectedLumber.getOrDefault(player.getUniqueId(), Set.of());

        for (int i = 0; i < TOTAL_LUMBER; i++) {
            boolean hasCollected = collectedLumber.contains(i);

            // Distance check
            boolean inRange = false;
            if (lumberVisuals[i] != null && lumberVisuals[i].isValid()) {
                double distSq = player.getLocation().distanceSquared(lumberVisuals[i].getLocation());
                inRange = distSq <= LUMBER_VIEW_DISTANCE * LUMBER_VIEW_DISTANCE;
            }

            // Visual (bois)
            if (lumberVisuals[i] != null && lumberVisuals[i].isValid()) {
                if (hasCollected || !inRange) {
                    player.hideEntity(plugin, lumberVisuals[i]);
                } else {
                    player.showEntity(plugin, lumberVisuals[i]);
                }
            }

            // Hitbox (Interaction)
            if (lumberHitboxes[i] != null && lumberHitboxes[i].isValid()) {
                if (hasCollected || !inRange) {
                    player.hideEntity(plugin, lumberHitboxes[i]);
                } else {
                    player.showEntity(plugin, lumberHitboxes[i]);
                }
            }
        }
    }

    /**
     * Cache tous les bois pour un joueur
     */
    private void hideAllLumberForPlayer(Player player) {
        for (int i = 0; i < TOTAL_LUMBER; i++) {
            if (lumberVisuals[i] != null && lumberVisuals[i].isValid()) {
                player.hideEntity(plugin, lumberVisuals[i]);
            }
            if (lumberHitboxes[i] != null && lumberHitboxes[i].isValid()) {
                player.hideEntity(plugin, lumberHitboxes[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des bois
     */
    private void startLumberRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < TOTAL_LUMBER; i++) {
                    if (lumberLocations[i] == null) continue;

                    boolean needsRespawn = (lumberVisuals[i] == null || !lumberVisuals[i].isValid()) ||
                            (lumberHitboxes[i] == null || !lumberHitboxes[i].isValid());

                    if (needsRespawn) {
                        spawnLumber(world, i);
                    }
                }

                // Vérifier aussi le bûcheron
                if (lumberjackNPC == null || !lumberjackNPC.isValid()) {
                    initializeLumberjack(world);
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur est sur la quête de bûcheronnage
     */
    private boolean isPlayerOnLumberQuest(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        return currentStep == JourneyStep.STEP_5_6;
    }

    /**
     * Vérifie si un joueur a terminé la quête de bûcheronnage
     */
    private boolean hasPlayerCompletedLumberQuest(Player player) {
        return journeyManager.isStepCompleted(player, JourneyStep.STEP_5_6);
    }

    /**
     * Initialise la progression de bûcheronnage d'un joueur
     */
    private void initializePlayerLumberProgress(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerCollectedLumber.containsKey(uuid)) {
            playerCollectedLumber.put(uuid, ConcurrentHashMap.newKeySet());
        }
        if (!playerLumberInInventory.containsKey(uuid)) {
            playerLumberInInventory.put(uuid, 0);
        }
    }

    /**
     * Active la quête de bûcheronnage pour un joueur
     */
    public void activateLumberQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Initialiser le tracking
        initializePlayerLumberProgress(player);
        activeLumberPlayers.add(playerId);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§6🪓 §e§lLIVRAISON DE BOIS §6🪓",
                    "§7Le bûcheron a besoin de votre aide!",
                    10, 60, 20
                );

                // Son d'ambiance
                player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§6§l══════ LIVRAISON DE BOIS ══════");
                player.sendMessage("");
                player.sendMessage("§7Le §6§lBûcheron Aldric §7a besoin de bois pour");
                player.sendMessage("§7renforcer les défenses du refuge...");
                player.sendMessage("");
                player.sendMessage("§e▸ §fCollectez §c" + LUMBER_TO_COLLECT + " bûches §fdans la forêt");
                player.sendMessage("§e▸ §fApprochez-vous et §afrappez §fles bûches glowing");
                player.sendMessage("§e▸ §fLivrez ensuite au §6Bûcheron Aldric");
                player.sendMessage("");

                // Activer le GPS vers la zone
                activateGPSToLumberZone(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers la zone de bûcheronnage
     */
    private void activateGPSToLumberZone(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + LUMBER_ZONE_CENTER_X + ", " + LUMBER_ZONE_CENTER_Y + ", " + LUMBER_ZONE_CENTER_Z + " §7(Zone forestière)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Active le GPS vers le bûcheron avec destination custom
     */
    private void activateGPSToLumberjack(Player player) {
        player.sendMessage("§e§l➤ §7GPS mis à jour: §b" + (int) LUMBERJACK_X + ", " + (int) LUMBERJACK_Y + ", " + (int) LUMBERJACK_Z + " §7(Bûcheron Aldric)");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            // Définir la destination custom vers le bûcheron
            World world = player.getWorld();
            Location lumberjackLoc = new Location(world, LUMBERJACK_X, LUMBERJACK_Y, LUMBERJACK_Z);
            gpsManager.setCustomDestination(player, lumberjackLoc);

            // Activer/rafraîchir le GPS
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Gère la collecte d'un bois
     */
    private void handleLumberCollection(Player player, int lumberIndex) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnLumberQuest(player)) {
            player.sendMessage("§7Une bûche de bois... La quête n'est pas encore active.");
            player.playSound(player.getLocation(), Sound.BLOCK_WOOD_HIT, 0.5f, 1f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedLumberQuest(player)) {
            return;
        }

        // Vérifier si ce bois est déjà collecté
        Set<Integer> collected = playerCollectedLumber.get(playerId);
        if (collected == null) {
            initializePlayerLumberProgress(player);
            collected = playerCollectedLumber.get(playerId);
        }

        if (collected.contains(lumberIndex)) {
            return;
        }

        // Marquer ce bois comme collecté
        collected.add(lumberIndex);

        // Ajouter le bois à l'inventaire du joueur
        ItemStack woodItem = new ItemStack(Material.DARK_OAK_WOOD, 1);
        var meta = woodItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Bûche de Chêne Noir §7(Quête)").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                Component.text("§8─────────────────").decoration(TextDecoration.ITALIC, false),
                Component.text("§7Bois récupéré pour le").decoration(TextDecoration.ITALIC, false),
                Component.text("§6Bûcheron Aldric§7.").decoration(TextDecoration.ITALIC, false),
                Component.text("").decoration(TextDecoration.ITALIC, false),
                Component.text("§e▶ Livrez au bûcheron!").decoration(TextDecoration.ITALIC, false)
            ));
            woodItem.setItemMeta(meta);
        }
        player.getInventory().addItem(woodItem);

        // Cacher ce bois pour le joueur
        updateLumberVisibilityForPlayer(player);

        // Mettre à jour le compteur
        int lumberInInv = playerLumberInInventory.getOrDefault(playerId, 0) + 1;
        playerLumberInInventory.put(playerId, lumberInInv);

        // Mettre à jour la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_6, lumberInInv);

        // Effets de collecte
        Location loc = lumberLocations[lumberIndex];
        if (loc != null) {
            player.playSound(loc, Sound.BLOCK_WOOD_BREAK, 1f, 1f);
            player.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            player.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 0.3, 0.3, 0.3,
                    Material.DARK_OAK_WOOD.createBlockData());
        }

        // Message de progression
        int remaining = LUMBER_TO_COLLECT - lumberInInv;
        if (remaining > 0) {
            player.sendMessage("§6[BOIS] §f+" + 1 + " bûche §7(" + lumberInInv + "/" + LUMBER_TO_COLLECT + ")");
        }

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);

        // Si on a assez de bois, indiquer d'aller voir le bûcheron
        if (lumberInInv >= LUMBER_TO_COLLECT) {
            player.sendMessage("");
            player.sendMessage("§a§l[✓] §7Vous avez assez de bois!");
            player.sendMessage("§e▸ §fAllez livrer au §6Bûcheron Aldric§f!");
            player.sendMessage("");

            player.sendTitle(
                "§a✓ BOIS COLLECTÉ!",
                "§7Livrez au bûcheron pour terminer",
                10, 60, 20
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

            // GPS vers le bûcheron
            activateGPSToLumberjack(player);
        }
    }

    /**
     * Gère la livraison au bûcheron
     */
    private void handleLumberjackDelivery(Player player) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnLumberQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§6§l[Bûcheron Aldric]");
            player.sendMessage("§7\"Hé là, voyageur! Si tu cherches du travail, reviens");
            player.sendMessage("§7quand tu auras débloqué ma quête!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedLumberQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§6§l[Bûcheron Aldric]");
            player.sendMessage("§7\"Merci encore pour le bois, ami! Les défenses");
            player.sendMessage("§7sont renforcées grâce à toi!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            return;
        }

        // Vérifier si le joueur a assez de bois
        int lumberInInv = playerLumberInInventory.getOrDefault(playerId, 0);
        if (lumberInInv < LUMBER_TO_COLLECT) {
            int remaining = LUMBER_TO_COLLECT - lumberInInv;
            player.sendMessage("");
            player.sendMessage("§6§l[Bûcheron Aldric]");
            player.sendMessage("§7\"Hum... il me faut encore §c" + remaining + " bûches§7!\"");
            player.sendMessage("§7\"Retourne dans la forêt et ramène-moi ce bois!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            // GPS vers la zone
            activateGPSToLumberZone(player);
            return;
        }

        // Retirer le bois de quête de l'inventaire (uniquement les bûches marquées)
        int toRemove = LUMBER_TO_COLLECT;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DARK_OAK_WOOD && toRemove > 0) {
                // Vérifier que c'est bien une bûche de quête (pas une bûche normale)
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();
                    if (displayName.contains("Quête") || displayName.contains("Bûche de Chêne Noir")) {
                        int amount = item.getAmount();
                        if (amount <= toRemove) {
                            toRemove -= amount;
                            item.setAmount(0);
                        } else {
                            item.setAmount(amount - toRemove);
                            toRemove = 0;
                        }
                    }
                }
            }
            if (toRemove <= 0) break;
        }

        // Compléter la quête
        completeLumberQuest(player);
    }

    /**
     * Termine la quête de bûcheronnage
     */
    private void completeLumberQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Nettoyer les données
        activeLumberPlayers.remove(playerId);
        playerCollectedLumber.remove(playerId);
        playerLumberInInventory.remove(playerId);

        // Nettoyer la destination GPS custom
        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.clearCustomDestination(player);
        }

        // Cacher les bois
        hideAllLumberForPlayer(player);

        // Compléter l'étape
        journeyManager.completeStep(player, JourneyStep.STEP_5_6);

        // Dialogue du bûcheron
        player.sendMessage("");
        player.sendMessage("§6§l[Bûcheron Aldric]");
        player.sendMessage("§7\"Excellent travail! Ce bois sera parfait pour");
        player.sendMessage("§7renforcer nos défenses contre les morts-vivants!\"");
        player.sendMessage("");

        // Title de victoire
        player.sendTitle(
            "§a§l✓ QUÊTE TERMINÉE!",
            "§7Le bûcheron vous remercie!",
            10, 60, 20
        );

        // Effets
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    /**
     * Reset la progression de bûcheronnage pour un joueur (appelé à la mort)
     */
    private void resetLumberProgress(Player player) {
        UUID uuid = player.getUniqueId();

        // Réinitialiser les bois collectés
        playerCollectedLumber.put(uuid, ConcurrentHashMap.newKeySet());

        // Retirer le bois de l'inventaire
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DARK_OAK_WOOD) {
                var meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    var displayName = meta.displayName();
                    if (displayName != null && displayName.toString().contains("Quête")) {
                        item.setAmount(0);
                    }
                }
            }
        }

        // Réinitialiser le compteur
        playerLumberInInventory.put(uuid, 0);

        // Réinitialiser la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_6, 0);

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);
    }

    // ==================== SYSTÈME DE CAPTURE DES GRENOUILLES (Étape 5.7) ====================

    /**
     * Initialise les grenouilles dans la zone marécageuse
     */
    private void initializeFrogs(World world) {
        // Générer les positions des grenouilles
        generateFrogLocations(world);

        // Spawner les grenouilles
        for (int i = 0; i < TOTAL_FROGS; i++) {
            if (frogLocations[i] != null) {
                spawnFrog(world, i);
            }
        }

        plugin.getLogger().info("[Chapter5Systems] " + TOTAL_FROGS + " grenouilles initialisées dans la zone marécageuse");
    }

    /**
     * Génère les positions des grenouilles dans la zone
     */
    private void generateFrogLocations(World world) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;
        int maxAttempts = TOTAL_FROGS * 50;
        int attempts = 0;

        while (spawned < TOTAL_FROGS && attempts < maxAttempts) {
            attempts++;

            int x = random.nextInt(FROG_ZONE_MIN_X, FROG_ZONE_MAX_X + 1);
            int z = random.nextInt(FROG_ZONE_MIN_Z, FROG_ZONE_MAX_Z + 1);

            // Chercher le premier bloc solide depuis le haut
            for (int y = FROG_ZONE_MAX_Y; y >= FROG_ZONE_MIN_Y; y--) {
                Block block = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);

                // Les grenouilles peuvent spawn sur l'eau ou sur la terre
                boolean validGround = block.getType().isSolid() || block.getType() == Material.WATER;
                boolean validAbove = above.getType() == Material.AIR || above.getType() == Material.WATER;

                if (validGround && validAbove) {
                    Location loc = new Location(world, x + 0.5, y + 1.2, z + 0.5);

                    // Vérifier qu'il n'y a pas déjà une grenouille trop proche
                    boolean tooClose = false;
                    for (int i = 0; i < spawned; i++) {
                        if (frogLocations[i] != null && frogLocations[i].distanceSquared(loc) < 64) { // 8 blocs min
                            tooClose = true;
                            break;
                        }
                    }

                    if (!tooClose) {
                        frogLocations[spawned] = loc;
                        spawned++;
                    }
                    break;
                }
            }
        }

        if (spawned < TOTAL_FROGS) {
            plugin.getLogger().warning("[Chapter5Systems] Seulement " + spawned + "/" + TOTAL_FROGS +
                " positions de grenouilles trouvées");
        }
    }

    /**
     * Spawn une grenouille à l'index donné
     */
    private void spawnFrog(World world, int frogIndex) {
        Location loc = frogLocations[frogIndex];
        if (loc == null) return;

        // Supprimer l'ancienne grenouille si existante
        if (frogEntities[frogIndex] != null && frogEntities[frogIndex].isValid()) {
            frogEntities[frogIndex].remove();
        }

        // Spawner une vraie grenouille
        frogEntities[frogIndex] = world.spawn(loc.clone(), Frog.class, frog -> {
            // Configuration de base
            frog.setAI(false);
            frog.setInvulnerable(true);
            frog.setSilent(true);
            frog.setPersistent(false);
            frog.setRemoveWhenFarAway(false);
            frog.setCollidable(false);

            // Variante aléatoire pour diversité visuelle
            Frog.Variant[] variants = Frog.Variant.values();
            frog.setVariant(variants[frogIndex % variants.length]);

            // Glow effect vert pour les identifier
            frog.setGlowing(true);

            // Nom custom pour les identifier
            frog.customName(Component.text("§a§lGrenouille Mutante", NamedTextColor.GREEN));
            frog.setCustomNameVisible(false);

            // Tags
            frog.addScoreboardTag("chapter5_frog");
            frog.addScoreboardTag("frog_" + frogIndex);
            frog.addScoreboardTag("zombiez_npc");

            // PDC pour l'index
            frog.getPersistentDataContainer().set(FROG_VISUAL_KEY, PersistentDataType.INTEGER, frogIndex);

            // Visibilité par défaut désactivée (géré per-player)
            frog.setVisibleByDefault(false);
        });
    }

    /**
     * Initialise le biologiste NPC
     */
    private void initializeBiologist(World world) {
        Location loc = new Location(world, BIOLOGIST_X, BIOLOGIST_Y, BIOLOGIST_Z, BIOLOGIST_YAW, BIOLOGIST_PITCH);

        // Chercher si le biologiste existe déjà
        for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof Villager v && v.getPersistentDataContainer().has(BIOLOGIST_NPC_KEY, PersistentDataType.BYTE)) {
                biologistNPC = v;
                // Chercher aussi le display
                for (Entity displayEntity : world.getNearbyEntities(loc.clone().add(0, 2.5, 0), 2, 2, 2)) {
                    if (displayEntity instanceof TextDisplay td && td.getScoreboardTags().contains("chapter5_biologist_display")) {
                        biologistDisplay = td;
                        break;
                    }
                }
                plugin.getLogger().info("[Chapter5Systems] Biologiste existant trouvé");
                return;
            }
        }

        // Créer le biologiste
        biologistNPC = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("§a§lDr. Marlow").decorate(TextDecoration.BOLD));
            villager.setCustomNameVisible(false);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCollidable(false);

            villager.setProfession(Villager.Profession.LIBRARIAN);
            villager.setVillagerLevel(4);

            // Tags
            villager.addScoreboardTag("chapter5_biologist");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(BIOLOGIST_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Visible par défaut
            villager.setVisibleByDefault(true);
        });

        // Créer le TextDisplay au-dessus
        Location displayLoc = loc.clone().add(0, 2.5, 0);
        biologistDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("🐸 ", NamedTextColor.GREEN))
                    .append(Component.text("Dr. Marlow", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" 🐸", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("§7Biologiste - Spécialiste Faune Mutante", NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour livrer", NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.3f);
            display.setPersistent(true);
            display.addScoreboardTag("chapter5_biologist_display");

            display.setVisibleByDefault(true);
        });

        plugin.getLogger().info("[Chapter5Systems] Dr. Marlow (Biologiste) initialisé");
    }

    /**
     * Démarre le système de visibilité per-player pour les grenouilles
     */
    private void startFrogVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllFrogsForPlayer(player);
                        continue;
                    }

                    // Vérifier si le joueur est sur la quête et ne l'a pas terminée
                    boolean shouldSeeFrogs = isPlayerOnFrogQuest(player) && !hasPlayerCompletedFrogQuest(player);

                    if (shouldSeeFrogs) {
                        updateFrogVisibilityForPlayer(player);
                    } else {
                        hideAllFrogsForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des grenouilles pour un joueur
     */
    private void updateFrogVisibilityForPlayer(Player player) {
        Set<Integer> captured = playerCapturedFrogs.getOrDefault(player.getUniqueId(), Set.of());

        for (int i = 0; i < TOTAL_FROGS; i++) {
            boolean hasCaptured = captured.contains(i);

            // Vérifier la validité de la grenouille
            if (frogEntities[i] == null || !frogEntities[i].isValid()) {
                continue;
            }

            // Distance check
            double distSq = player.getLocation().distanceSquared(frogEntities[i].getLocation());
            boolean inRange = distSq <= FROG_VIEW_DISTANCE * FROG_VIEW_DISTANCE;

            // Afficher ou cacher la grenouille
            if (hasCaptured || !inRange) {
                player.hideEntity(plugin, frogEntities[i]);
            } else {
                player.showEntity(plugin, frogEntities[i]);
            }
        }
    }

    /**
     * Cache toutes les grenouilles pour un joueur
     */
    private void hideAllFrogsForPlayer(Player player) {
        for (int i = 0; i < TOTAL_FROGS; i++) {
            if (frogEntities[i] != null && frogEntities[i].isValid()) {
                player.hideEntity(plugin, frogEntities[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des grenouilles
     */
    private void startFrogRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < TOTAL_FROGS; i++) {
                    if (frogLocations[i] == null) continue;

                    boolean needsRespawn = frogEntities[i] == null || !frogEntities[i].isValid();

                    if (needsRespawn) {
                        spawnFrog(world, i);
                    }
                }

                // Vérifier aussi le biologiste
                if (biologistNPC == null || !biologistNPC.isValid()) {
                    initializeBiologist(world);
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur est sur la quête des grenouilles
     */
    private boolean isPlayerOnFrogQuest(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        return currentStep == JourneyStep.STEP_5_7;
    }

    /**
     * Vérifie si un joueur a terminé la quête des grenouilles
     */
    private boolean hasPlayerCompletedFrogQuest(Player player) {
        return journeyManager.isStepCompleted(player, JourneyStep.STEP_5_7);
    }

    /**
     * Initialise la progression de grenouilles d'un joueur
     */
    private void initializePlayerFrogProgress(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerCapturedFrogs.containsKey(uuid)) {
            playerCapturedFrogs.put(uuid, ConcurrentHashMap.newKeySet());
        }
        if (!playerFrogsInInventory.containsKey(uuid)) {
            playerFrogsInInventory.put(uuid, 0);
        }
    }

    /**
     * Active la quête des grenouilles pour un joueur
     */
    public void activateFrogQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Initialiser le tracking
        initializePlayerFrogProgress(player);
        activeFrogPlayers.add(playerId);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§a🐸 §2§lCHASSE AUX GRENOUILLES §a🐸",
                    "§7Le biologiste a besoin de spécimens!",
                    10, 60, 20
                );

                // Son d'ambiance
                player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 1.0f, 1.0f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§a§l══════ CHASSE AUX GRENOUILLES MUTANTES ══════");
                player.sendMessage("");
                player.sendMessage("§7Le §a§lDr. Marlow §7étudie les mutations causées par");
                player.sendMessage("§7l'infection. Il a besoin de §agrouilles mutantes §7vivantes!");
                player.sendMessage("");
                player.sendMessage("§e▸ §fCapturez §c" + FROGS_TO_CAPTURE + " grenouilles mutantes");
                player.sendMessage("§e▸ §fFaites §aclic droit §fsur une grenouille pour la capturer");
                player.sendMessage("§e▸ §fComplétez le §emini-jeu §fpour réussir la capture!");
                player.sendMessage("§e▸ §fLivrez ensuite au §aDr. Marlow");
                player.sendMessage("");
                player.sendMessage("§c⚠ §7Si vous mourez, vous perdrez vos grenouilles!");

                // Activer le GPS vers la zone
                activateGPSToFrogZone(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers la zone des grenouilles
     */
    private void activateGPSToFrogZone(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + FROG_ZONE_CENTER_X + ", " + FROG_ZONE_CENTER_Y + ", " + FROG_ZONE_CENTER_Z + " §7(Zone marécageuse)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Active le GPS vers le biologiste
     */
    private void activateGPSToBiologist(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + (int) BIOLOGIST_X + ", " + (int) BIOLOGIST_Y + ", " + (int) BIOLOGIST_Z + " §7(Dr. Marlow)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Gère l'interaction avec une grenouille (ouvre le mini-jeu)
     */
    private void handleFrogInteraction(Player player, int frogIndex) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnFrogQuest(player)) {
            player.sendMessage("§7Une grenouille mutante... La quête n'est pas encore active.");
            player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 0.5f, 0.8f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedFrogQuest(player)) {
            return;
        }

        // Vérifier si cette grenouille est déjà capturée
        Set<Integer> captured = playerCapturedFrogs.get(playerId);
        if (captured == null) {
            initializePlayerFrogProgress(player);
            captured = playerCapturedFrogs.get(playerId);
        }

        if (captured.contains(frogIndex)) {
            return;
        }

        // Vérifier si le joueur a déjà un mini-jeu en cours
        if (frogCaptureGUI.hasActiveGame(player)) {
            return;
        }

        // Ouvrir le mini-jeu de capture
        player.sendMessage("§a[GRENOUILLE] §fTentative de capture...");

        frogCaptureGUI.openGame(player, frogIndex,
            // Callback de succès
            p -> handleFrogCaptureSuccess(p, frogIndex),
            // Callback d'échec
            p -> handleFrogCaptureFailed(p, frogIndex)
        );
    }

    /**
     * Gère une capture réussie
     */
    private void handleFrogCaptureSuccess(Player player, int frogIndex) {
        UUID playerId = player.getUniqueId();

        // Marquer cette grenouille comme capturée
        Set<Integer> captured = playerCapturedFrogs.get(playerId);
        if (captured == null) {
            initializePlayerFrogProgress(player);
            captured = playerCapturedFrogs.get(playerId);
        }
        captured.add(frogIndex);

        // Ajouter la grenouille à l'inventaire du joueur (item spécial)
        ItemStack frogItem = new ItemStack(Material.FROG_SPAWN_EGG, 1);
        var meta = frogItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§a🐸 Grenouille Mutante §7(Quête)").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                Component.text("§8─────────────────").decoration(TextDecoration.ITALIC, false),
                Component.text("§7Spécimen capturé pour le").decoration(TextDecoration.ITALIC, false),
                Component.text("§aDr. Marlow§7.").decoration(TextDecoration.ITALIC, false),
                Component.text("").decoration(TextDecoration.ITALIC, false),
                Component.text("§e▶ Livrez au biologiste!").decoration(TextDecoration.ITALIC, false)
            ));
            frogItem.setItemMeta(meta);
        }
        player.getInventory().addItem(frogItem);

        // Cacher cette grenouille pour le joueur
        updateFrogVisibilityForPlayer(player);

        // Mettre à jour le compteur
        int frogsInInv = playerFrogsInInventory.getOrDefault(playerId, 0) + 1;
        playerFrogsInInventory.put(playerId, frogsInInv);

        // Mettre à jour la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_7, frogsInInv);

        // Effets de capture
        Location loc = frogLocations[frogIndex];
        if (loc != null) {
            player.playSound(loc, Sound.ENTITY_FROG_AMBIENT, 1f, 1.5f);
            player.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 15, 0.5, 0.5, 0.5, 0);
        }

        // Message de progression
        int remaining = FROGS_TO_CAPTURE - frogsInInv;
        if (remaining > 0) {
            player.sendMessage("§a[GRENOUILLE] §f+1 grenouille capturée! §7(" + frogsInInv + "/" + FROGS_TO_CAPTURE + ")");
        }

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);

        // Si on a assez de grenouilles, indiquer d'aller voir le biologiste
        if (frogsInInv >= FROGS_TO_CAPTURE) {
            player.sendMessage("");
            player.sendMessage("§a§l[✓] §7Vous avez assez de grenouilles!");
            player.sendMessage("§e▸ §fAllez livrer au §aDr. Marlow§f!");
            player.sendMessage("");

            player.sendTitle(
                "§a✓ GRENOUILLES CAPTURÉES!",
                "§7Livrez au biologiste pour terminer",
                10, 60, 20
            );

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

            // GPS vers le biologiste
            activateGPSToBiologist(player);
        }
    }

    /**
     * Gère une capture échouée
     */
    private void handleFrogCaptureFailed(Player player, int frogIndex) {
        player.sendMessage("§c[GRENOUILLE] §fLa grenouille s'est échappée! Réessayez...");
        player.playSound(player.getLocation(), Sound.ENTITY_FROG_LONG_JUMP, 1f, 0.8f);
    }

    /**
     * Gère la livraison au biologiste
     */
    private void handleBiologistDelivery(Player player) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnFrogQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§a§l[Dr. Marlow]");
            player.sendMessage("§7\"Bonjour, aventurier! Je suis le Dr. Marlow,");
            player.sendMessage("§7spécialiste de la faune mutante. Reviens me voir");
            player.sendMessage("§7quand tu auras débloqué ma quête!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedFrogQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§a§l[Dr. Marlow]");
            player.sendMessage("§7\"Merci encore pour ces spécimens extraordinaires!");
            player.sendMessage("§7Mes recherches avancent bien grâce à toi!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            return;
        }

        // Vérifier si le joueur a assez de grenouilles
        int frogsInInv = playerFrogsInInventory.getOrDefault(playerId, 0);
        if (frogsInInv < FROGS_TO_CAPTURE) {
            int remaining = FROGS_TO_CAPTURE - frogsInInv;
            player.sendMessage("");
            player.sendMessage("§a§l[Dr. Marlow]");
            player.sendMessage("§7\"Hmm... il me faut encore §c" + remaining + " grenouilles§7!\"");
            player.sendMessage("§7\"Retourne dans les marais et captures-en d'autres!\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

            // GPS vers la zone
            activateGPSToFrogZone(player);
            return;
        }

        // Retirer les grenouilles de quête de l'inventaire
        int toRemove = FROGS_TO_CAPTURE;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FROG_SPAWN_EGG && toRemove > 0) {
                // Vérifier que c'est bien une grenouille de quête
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();
                    if (displayName.contains("Quête") || displayName.contains("Grenouille Mutante")) {
                        int amount = item.getAmount();
                        if (amount <= toRemove) {
                            toRemove -= amount;
                            item.setAmount(0);
                        } else {
                            item.setAmount(amount - toRemove);
                            toRemove = 0;
                        }
                    }
                }
            }
            if (toRemove <= 0) break;
        }

        // Compléter la quête
        completeFrogQuest(player);
    }

    /**
     * Termine la quête des grenouilles
     */
    private void completeFrogQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Nettoyer les données
        activeFrogPlayers.remove(playerId);
        playerCapturedFrogs.remove(playerId);
        playerFrogsInInventory.remove(playerId);

        // Cacher les grenouilles
        hideAllFrogsForPlayer(player);

        // Compléter l'étape
        journeyManager.completeStep(player, JourneyStep.STEP_5_7);

        // Dialogue du biologiste
        player.sendMessage("");
        player.sendMessage("§a§l[Dr. Marlow]");
        player.sendMessage("§7\"Extraordinaire! Ces spécimens sont parfaits!");
        player.sendMessage("§7Leur mutation est fascinante... Je vais pouvoir");
        player.sendMessage("§7développer un antidote encore plus puissant!\"");
        player.sendMessage("");

        // Title de victoire
        player.sendTitle(
            "§a§l✓ QUÊTE TERMINÉE!",
            "§7Le Dr. Marlow vous remercie!",
            10, 60, 20
        );

        // Effets
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    /**
     * Reset la progression des grenouilles pour un joueur (appelé à la mort)
     */
    private void resetFrogProgress(Player player) {
        UUID uuid = player.getUniqueId();

        // Réinitialiser les grenouilles capturées
        playerCapturedFrogs.put(uuid, ConcurrentHashMap.newKeySet());

        // Retirer les grenouilles de l'inventaire
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FROG_SPAWN_EGG) {
                var meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    var displayName = meta.displayName();
                    if (displayName != null && displayName.toString().contains("Quête")) {
                        item.setAmount(0);
                    }
                }
            }
        }

        // Réinitialiser le compteur
        playerFrogsInInventory.put(uuid, 0);

        // Réinitialiser la progression dans JourneyManager
        journeyManager.setStepProgress(player, JourneyStep.STEP_5_7, 0);

        // Mettre à jour la BossBar
        journeyManager.createOrUpdateBossBar(player);
    }

    // ==================== SYSTÈME DES ÉNIGMES DE L'ORACLE (Étape 5.8) ====================

    /**
     * Structure d'une énigme
     */
    private record Riddle(String question, String[] choices, int correctAnswer, String explanation) {}

    /**
     * Les 3 énigmes de l'Oracle (adaptées à des joueurs de 12 ans, thème ZombieZ)
     */
    private static final Riddle[] RIDDLES = {
        new Riddle(
            "§e\"Je suis mort mais je marche encore,\n§eje cherche les vivants pour les dévorer.\n§eQue suis-je?\"",
            new String[]{"§aUn Zombie", "§7Un Fantôme", "§7Un Squelette", "§7Un Vampire"},
            0,
            "§7Les zombies sont des morts-vivants qui errent à la recherche de chair fraîche!"
        ),
        new Riddle(
            "§e\"Je repousse les ténèbres et protège\n§eles survivants des dangers de la nuit.\n§eQue suis-je?\"",
            new String[]{"§7Une Épée", "§aUne Torche", "§7Un Bouclier", "§7Une Armure"},
            1,
            "§7La lumière des torches empêche les zombies d'apparaître près des survivants!"
        ),
        new Riddle(
            "§e\"Dans l'apocalypse zombie, les survivants\n§ese rassemblent en un lieu sûr.\n§eComment appelle-t-on cet endroit?\"",
            new String[]{"§7Une Forêt", "§7Une Grotte", "§aUn Refuge", "§7Un Marais"},
            2,
            "§7Le refuge est l'endroit où les survivants sont en sécurité!"
        )
    };

    /**
     * Initialise l'Oracle NPC
     */
    private void initializeOracle(World world) {
        Location loc = new Location(world, ORACLE_X, ORACLE_Y, ORACLE_Z, ORACLE_YAW, ORACLE_PITCH);

        // Chercher si l'Oracle existe déjà
        for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof Villager v && v.getPersistentDataContainer().has(ORACLE_NPC_KEY, PersistentDataType.BYTE)) {
                oracleNPC = v;
                // Chercher aussi le display
                for (Entity displayEntity : world.getNearbyEntities(loc.clone().add(0, 2.5, 0), 2, 2, 2)) {
                    if (displayEntity instanceof TextDisplay td && td.getScoreboardTags().contains("chapter5_oracle_display")) {
                        oracleDisplay = td;
                        break;
                    }
                }
                plugin.getLogger().info("[Chapter5Systems] Oracle existant trouvé");
                return;
            }
        }

        // Créer l'Oracle
        oracleNPC = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("§5§lOracle des Marais").decorate(TextDecoration.BOLD));
            villager.setCustomNameVisible(false);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCollidable(false);

            villager.setProfession(Villager.Profession.CLERIC);
            villager.setVillagerLevel(5);

            // Tags
            villager.addScoreboardTag("chapter5_oracle");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(ORACLE_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Visible par défaut
            villager.setVisibleByDefault(true);
        });

        // Créer le TextDisplay au-dessus
        Location displayLoc = loc.clone().add(0, 2.5, 0);
        oracleDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("✦ ", NamedTextColor.DARK_PURPLE))
                    .append(Component.text("Oracle des Marais", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                    .append(Component.text(" ✦", NamedTextColor.DARK_PURPLE))
                    .append(Component.newline())
                    .append(Component.text("§7Gardien des Savoirs Anciens", NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour les énigmes", NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.3f);
            display.setPersistent(true);
            display.addScoreboardTag("chapter5_oracle_display");

            display.setVisibleByDefault(true);
        });

        plugin.getLogger().info("[Chapter5Systems] Oracle des Marais initialisé");
    }

    /**
     * Démarre le vérificateur de respawn de l'Oracle
     */
    private void startOracleRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                if (oracleNPC == null || !oracleNPC.isValid()) {
                    initializeOracle(world);
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur est sur la quête des énigmes
     */
    private boolean isPlayerOnRiddleQuest(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        return currentStep == JourneyStep.STEP_5_8;
    }

    /**
     * Vérifie si un joueur a terminé la quête des énigmes
     */
    private boolean hasPlayerCompletedRiddleQuest(Player player) {
        return journeyManager.isStepCompleted(player, JourneyStep.STEP_5_8);
    }

    /**
     * Active la quête des énigmes pour un joueur
     */
    public void activateRiddleQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Initialiser le tracking
        activeRiddlePlayers.add(playerId);
        playerCurrentRiddle.put(playerId, 0);

        // Afficher l'introduction
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Title d'introduction
                player.sendTitle(
                    "§5✦ §d§lLES ÉNIGMES DE L'ORACLE §5✦",
                    "§7Prouvez votre sagesse!",
                    10, 60, 20
                );

                // Son mystique
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);

                // Message de briefing
                player.sendMessage("");
                player.sendMessage("§5§l══════ LES ÉNIGMES DE L'ORACLE ══════");
                player.sendMessage("");
                player.sendMessage("§7L'§5§lOracle des Marais §7détient des savoirs anciens");
                player.sendMessage("§7sur la survie face aux morts-vivants...");
                player.sendMessage("");
                player.sendMessage("§e▸ §fRésolvez §c" + TOTAL_RIDDLES + " énigmes §fpour prouver votre valeur");
                player.sendMessage("§e▸ §fParlez à l'§5Oracle §fpour commencer");
                player.sendMessage("§e▸ §fChoisissez la bonne réponse dans le menu");
                player.sendMessage("");

                // Activer le GPS vers l'Oracle
                activateGPSToOracle(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers l'Oracle
     */
    private void activateGPSToOracle(Player player) {
        player.sendMessage("§e§l➤ §7GPS: §b" + (int) ORACLE_X + ", " + (int) ORACLE_Y + ", " + (int) ORACLE_Z + " §7(Oracle des Marais)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        var gpsManager = plugin.getGPSManager();
        if (gpsManager != null) {
            gpsManager.enableGPSSilently(player);
        }
    }

    /**
     * Gère l'interaction avec l'Oracle
     */
    private void handleOracleInteraction(Player player) {
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur est sur la bonne étape
        if (!isPlayerOnRiddleQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§5§l[Oracle des Marais]");
            player.sendMessage("§7\"Jeune voyageur... Les étoiles ne m'ont pas");
            player.sendMessage("§7encore révélé ton destin. Reviens quand tu");
            player.sendMessage("§7auras débloqué ma quête...\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
            return;
        }

        // Vérifier si la quête est terminée
        if (hasPlayerCompletedRiddleQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§5§l[Oracle des Marais]");
            player.sendMessage("§7\"Tu as prouvé ta sagesse, survivant.");
            player.sendMessage("§7Que les anciens esprits te guident...\"");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
            return;
        }

        // Obtenir l'énigme actuelle
        int riddleIndex = playerCurrentRiddle.getOrDefault(playerId, 0);

        if (riddleIndex >= TOTAL_RIDDLES) {
            // Quête terminée
            completeRiddleQuest(player);
            return;
        }

        // Ouvrir le GUI de l'énigme
        openRiddleGUI(player, riddleIndex);
    }

    /**
     * Ouvre le GUI d'une énigme
     */
    private void openRiddleGUI(Player player, int riddleIndex) {
        Riddle riddle = RIDDLES[riddleIndex];

        // Créer l'inventaire (3 lignes)
        Inventory gui = Bukkit.createInventory(null, 27,
            Component.text("✦ Énigme " + (riddleIndex + 1) + "/" + TOTAL_RIDDLES + " ✦", NamedTextColor.DARK_PURPLE));

        // Remplir le fond
        ItemStack glass = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }

        // Question au centre (slot 4)
        ItemStack questionItem = new ItemStack(Material.ENCHANTED_BOOK);
        var questionMeta = questionItem.getItemMeta();
        if (questionMeta != null) {
            questionMeta.displayName(Component.text("§5§l✦ L'Oracle demande... ✦").decoration(TextDecoration.ITALIC, false));
            String[] lines = riddle.question().split("\n");
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§8───────────────").decoration(TextDecoration.ITALIC, false));
            for (String line : lines) {
                lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("§8───────────────").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§7Choisissez la bonne réponse!").decoration(TextDecoration.ITALIC, false));
            questionMeta.lore(lore);
            questionItem.setItemMeta(questionMeta);
        }
        gui.setItem(4, questionItem);

        // Les 4 choix (slots 10, 12, 14, 16)
        int[] choiceSlots = {10, 12, 14, 16};
        Material[] choiceMaterials = {Material.LIME_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.RED_TERRACOTTA};

        for (int i = 0; i < 4; i++) {
            ItemStack choiceItem = new ItemStack(choiceMaterials[i]);
            var choiceMeta = choiceItem.getItemMeta();
            if (choiceMeta != null) {
                choiceMeta.displayName(Component.text(riddle.choices()[i]).decoration(TextDecoration.ITALIC, false));
                choiceMeta.lore(List.of(
                    Component.text("§7Cliquez pour répondre").decoration(TextDecoration.ITALIC, false)
                ));
                // Stocker l'index du choix dans le PDC
                choiceMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "riddle_choice"),
                    PersistentDataType.INTEGER,
                    i
                );
                choiceItem.setItemMeta(choiceMeta);
            }
            gui.setItem(choiceSlots[i], choiceItem);
        }

        // Ouvrir le GUI
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.0f);
    }

    /**
     * Crée un panneau de verre décoratif
     */
    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Gère le clic sur une réponse dans le GUI des énigmes
     */
    public void handleRiddleAnswer(Player player, int choiceIndex) {
        UUID playerId = player.getUniqueId();

        if (!isPlayerOnRiddleQuest(player) || hasPlayerCompletedRiddleQuest(player)) {
            return;
        }

        int riddleIndex = playerCurrentRiddle.getOrDefault(playerId, 0);
        if (riddleIndex >= TOTAL_RIDDLES) {
            return;
        }

        Riddle riddle = RIDDLES[riddleIndex];

        // Fermer le GUI
        player.closeInventory();

        if (choiceIndex == riddle.correctAnswer()) {
            // Bonne réponse!
            int newProgress = riddleIndex + 1;
            playerCurrentRiddle.put(playerId, newProgress);
            journeyManager.setStepProgress(player, JourneyStep.STEP_5_8, newProgress);

            // Effets de succès
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);

            // Message de l'Oracle
            player.sendMessage("");
            player.sendMessage("§5§l[Oracle des Marais]");
            player.sendMessage("§a\"Excellente réponse, jeune sage!\"");
            player.sendMessage(riddle.explanation());
            player.sendMessage("");

            // Title
            player.sendTitle(
                "§a✓ BONNE RÉPONSE!",
                "§7Énigme " + newProgress + "/" + TOTAL_RIDDLES + " résolue",
                10, 40, 10
            );

            // Mettre à jour la BossBar
            journeyManager.createOrUpdateBossBar(player);

            // Si toutes les énigmes sont résolues
            if (newProgress >= TOTAL_RIDDLES) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            completeRiddleQuest(player);
                        }
                    }
                }.runTaskLater(plugin, 40L);
            } else {
                // Indiquer de reparler à l'Oracle
                player.sendMessage("§e▸ §fParlez à nouveau à l'§5Oracle §fpour la prochaine énigme!");
            }
        } else {
            // Mauvaise réponse
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);

            // Message de l'Oracle
            player.sendMessage("");
            player.sendMessage("§5§l[Oracle des Marais]");
            player.sendMessage("§c\"Hélas, ce n'est pas la bonne réponse...\"");
            player.sendMessage("§7\"Réfléchis bien et réessaye, jeune voyageur.\"");
            player.sendMessage("");

            // Title
            player.sendTitle(
                "§c✗ MAUVAISE RÉPONSE",
                "§7Réessayez en parlant à l'Oracle",
                10, 40, 10
            );
        }
    }

    /**
     * Termine la quête des énigmes
     */
    private void completeRiddleQuest(Player player) {
        UUID playerId = player.getUniqueId();

        // Nettoyer les données
        activeRiddlePlayers.remove(playerId);
        playerCurrentRiddle.remove(playerId);

        // Compléter l'étape
        journeyManager.completeStep(player, JourneyStep.STEP_5_8);

        // Dialogue de l'Oracle
        player.sendMessage("");
        player.sendMessage("§5§l[Oracle des Marais]");
        player.sendMessage("§d\"Tu as prouvé ta sagesse, survivant!");
        player.sendMessage("§dLes anciens esprits reconnaissent ta valeur.");
        player.sendMessage("§dQue ce savoir te guide dans les ténèbres...\"");
        player.sendMessage("");

        // Title de victoire
        player.sendTitle(
            "§5§l✦ QUÊTE TERMINÉE! ✦",
            "§7L'Oracle reconnaît votre sagesse!",
            10, 60, 20
        );

        // Effets
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.5, 0), 100, 1, 1, 1, 0.5);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
    }

    // ==================== ÉVÉNEMENTS ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();

        // Hit sur un minerai (Interaction hitbox)
        if (damaged instanceof Interaction
                && damaged.getPersistentDataContainer().has(ORE_HITBOX_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer oreIndex = damaged.getPersistentDataContainer().get(ORE_HITBOX_KEY, PersistentDataType.INTEGER);
                if (oreIndex != null) {
                    handleOreHit(attacker, oreIndex);
                }
            }
        }

        // Hit sur un bois (Interaction hitbox)
        if (damaged instanceof Interaction
                && damaged.getPersistentDataContainer().has(LUMBER_HITBOX_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer lumberIndex = damaged.getPersistentDataContainer().get(LUMBER_HITBOX_KEY, PersistentDataType.INTEGER);
                if (lumberIndex != null) {
                    handleLumberCollection(attacker, lumberIndex);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        Player player = event.getPlayer();

        // Interaction avec un suspect
        if (clicked instanceof Villager
                && clicked.getPersistentDataContainer().has(SUSPECT_NPC_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);

            Integer suspectIndex = clicked.getPersistentDataContainer().get(SUSPECT_NPC_KEY, PersistentDataType.INTEGER);
            if (suspectIndex != null) {
                handleSuspectInterrogation(player, suspectIndex);
            }
        }

        // Interaction avec le bûcheron
        if (clicked instanceof Villager
                && clicked.getPersistentDataContainer().has(LUMBERJACK_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleLumberjackDelivery(player);
        }

        // Interaction avec le biologiste
        if (clicked instanceof Villager
                && clicked.getPersistentDataContainer().has(BIOLOGIST_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleBiologistDelivery(player);
        }

        // Interaction avec une grenouille (vraie entité Frog)
        if (clicked instanceof Frog frog
                && frog.getPersistentDataContainer().has(FROG_VISUAL_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            Integer frogIndex = frog.getPersistentDataContainer().get(FROG_VISUAL_KEY, PersistentDataType.INTEGER);
            if (frogIndex != null) {
                handleFrogInteraction(player, frogIndex);
            }
        }

        // Interaction avec l'Oracle
        if (clicked instanceof Villager
                && clicked.getPersistentDataContainer().has(ORACLE_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleOracleInteraction(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Vérifier si c'est le GUI des énigmes
        String title = event.getView().title().toString();
        if (!title.contains("Énigme")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        // Vérifier si l'item a un choix de réponse
        var meta = clicked.getItemMeta();
        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "riddle_choice"), PersistentDataType.INTEGER)) {
            Integer choiceIndex = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "riddle_choice"),
                PersistentDataType.INTEGER
            );
            if (choiceIndex != null) {
                handleRiddleAnswer(player, choiceIndex);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTraitorDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Vérifier si c'est un traître de quête
        if (!entity.getPersistentDataContainer().has(TRAITOR_PILLAGER_KEY, PersistentDataType.STRING)) {
            return;
        }

        String ownerIdStr = entity.getPersistentDataContainer().get(TRAITOR_PILLAGER_KEY, PersistentDataType.STRING);
        if (ownerIdStr == null) {
            return;
        }

        Player killer = null;
        if (event.getEntity() instanceof LivingEntity living) {
            killer = living.getKiller();
        }

        if (killer != null && killer.getUniqueId().toString().equals(ownerIdStr)) {
            handleTraitorDeath(killer, entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Si le joueur est sur la quête de minage, reset sa progression
        if (isPlayerOnMiningQuest(player)) {
            // Afficher un message d'avertissement après le respawn
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    resetMiningProgress(player);

                    player.sendTitle(
                        "§c☠ QUÊTE ÉCHOUÉE!",
                        "§7Votre progression de minage a été réinitialisée",
                        10, 60, 20
                    );

                    player.sendMessage("");
                    player.sendMessage("§c§l[MINAGE] §7Vous êtes mort! Votre progression a été §créinitialisée§7.");
                    player.sendMessage("§e▸ §fRetournez à la mine et recommencez!");
                    player.sendMessage("");

                    // Réactiver le GPS
                    activateGPSToMine(player);

                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                }
            }.runTaskLater(plugin, 40L); // Après le respawn
        }

        // Si le joueur est sur la quête du traître, reset sa progression
        if (isPlayerOnTraitorQuest(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    resetTraitorProgress(player);

                    player.sendTitle(
                        "§c☠ QUÊTE ÉCHOUÉE!",
                        "§7Votre progression d'enquête a été réinitialisée",
                        10, 60, 20
                    );

                    player.sendMessage("");
                    player.sendMessage("§c§l[TRAÎTRE] §7Vous êtes mort! Votre enquête a été §créinitialisée§7.");
                    player.sendMessage("§e▸ §fRetournez à Maraisville et recommencez!");
                    player.sendMessage("");

                    // Réactiver le GPS
                    updateGPSToNextSuspect(player);

                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                }
            }.runTaskLater(plugin, 40L);
        }

        // Si le joueur est sur la quête de bûcheronnage, reset sa progression
        if (isPlayerOnLumberQuest(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    resetLumberProgress(player);

                    player.sendTitle(
                        "§c☠ QUÊTE ÉCHOUÉE!",
                        "§7Votre collecte de bois a été réinitialisée",
                        10, 60, 20
                    );

                    player.sendMessage("");
                    player.sendMessage("§c§l[BOIS] §7Vous êtes mort! Votre progression a été §créinitialisée§7.");
                    player.sendMessage("§e▸ §fRetournez dans la forêt et recommencez!");
                    player.sendMessage("");

                    // Réactiver le GPS
                    activateGPSToLumberZone(player);

                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                }
            }.runTaskLater(plugin, 40L);
        }

        // Si le joueur est sur la quête des grenouilles, reset sa progression
        if (isPlayerOnFrogQuest(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;

                    resetFrogProgress(player);

                    player.sendTitle(
                        "§c☠ QUÊTE ÉCHOUÉE!",
                        "§7Vos grenouilles capturées ont été perdues",
                        10, 60, 20
                    );

                    player.sendMessage("");
                    player.sendMessage("§c§l[GRENOUILLES] §7Vous êtes mort! Vos grenouilles ont été §cperdues§7.");
                    player.sendMessage("§e▸ §fRetournez au marais et recommencez!");
                    player.sendMessage("");

                    // Réactiver le GPS
                    activateGPSToFrogZone(player);

                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                JourneyStep currentStep = journeyManager.getCurrentStep(player);

                // Quête des saumons
                if (currentStep == JourneyStep.STEP_5_2) {
                    activeSalmonPlayers.add(player.getUniqueId());

                    int progress = journeyManager.getStepProgress(player, currentStep);
                    int remaining = 12 - progress;

                    player.sendMessage("");
                    player.sendMessage("§b§l[QUÊTE] §7Pêche aux Saumons Mutants en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/12");
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fsaumons");
                    }

                    activateGPSToFishingZone(player);
                }

                // Quête de minage
                if (currentStep == JourneyStep.STEP_5_3) {
                    initializePlayerMiningProgress(player);
                    activeMiningPlayers.add(player.getUniqueId());

                    int progress = journeyManager.getStepProgress(player, currentStep);
                    int remaining = ORES_TO_MINE - progress;

                    player.sendMessage("");
                    player.sendMessage("§6§l[QUÊTE] §7Extraction Minière en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/" + ORES_TO_MINE);
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fminerais");
                    }
                    player.sendMessage("§c⚠ §7Si vous mourez, vous recommencerez à zéro!");

                    activateGPSToMine(player);
                }

                // Quête du traître
                if (currentStep == JourneyStep.STEP_5_5) {
                    if (!playerInterrogatedSuspects.containsKey(player.getUniqueId())) {
                        playerInterrogatedSuspects.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());
                    }
                    activeTraitorPlayers.add(player.getUniqueId());

                    int progress = journeyManager.getStepProgress(player, currentStep);

                    player.sendMessage("");
                    player.sendMessage("§4§l[QUÊTE] §7Traque du Traître en cours!");
                    if (progress >= 5) {
                        player.sendMessage("§e▸ §fTuez le traître!");
                        player.sendMessage("§c§l➤ §7Le traître est dans le village!");
                    } else {
                        int remaining = 5 - progress;
                        player.sendMessage("§e▸ §fProgression: §c" + progress + "/5 §fsuspects interrogés");
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fsuspects");
                        player.sendMessage("§c⚠ §7Si vous mourez, vous recommencerez à zéro!");
                        updateGPSToNextSuspect(player);
                    }
                }

                // Quête de bûcheronnage
                if (currentStep == JourneyStep.STEP_5_6) {
                    initializePlayerLumberProgress(player);
                    activeLumberPlayers.add(player.getUniqueId());

                    int progress = journeyManager.getStepProgress(player, currentStep);
                    int remaining = LUMBER_TO_COLLECT - progress;

                    player.sendMessage("");
                    player.sendMessage("§6§l[QUÊTE] §7Livraison de Bois en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/" + LUMBER_TO_COLLECT);
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fbûches à collecter");
                        player.sendMessage("§c⚠ §7Si vous mourez, vous recommencerez à zéro!");
                        activateGPSToLumberZone(player);
                    } else {
                        player.sendMessage("§e▸ §fLivrez au §6Bûcheron Aldric§f!");
                        activateGPSToLumberjack(player);
                    }
                }

                // Quête des grenouilles
                if (currentStep == JourneyStep.STEP_5_7) {
                    initializePlayerFrogProgress(player);
                    activeFrogPlayers.add(player.getUniqueId());

                    int progress = journeyManager.getStepProgress(player, currentStep);
                    int remaining = FROGS_TO_CAPTURE - progress;

                    player.sendMessage("");
                    player.sendMessage("§a§l[QUÊTE] §7Chasse aux Grenouilles Mutantes en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/" + FROGS_TO_CAPTURE);
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fgrenouilles à capturer");
                        player.sendMessage("§c⚠ §7Si vous mourez, vous perdrez vos grenouilles!");
                        activateGPSToFrogZone(player);
                    } else {
                        player.sendMessage("§e▸ §fLivrez au §aDr. Marlow§f!");
                        activateGPSToBiologist(player);
                    }
                }

                // Quête des énigmes
                if (currentStep == JourneyStep.STEP_5_8) {
                    activeRiddlePlayers.add(player.getUniqueId());
                    // Restaurer la progression si nécessaire
                    int progress = journeyManager.getStepProgress(player, currentStep);
                    playerCurrentRiddle.put(player.getUniqueId(), progress);

                    int remaining = TOTAL_RIDDLES - progress;

                    player.sendMessage("");
                    player.sendMessage("§5§l[QUÊTE] §7Les Énigmes de l'Oracle en cours!");
                    player.sendMessage("§e▸ §fProgression: §c" + progress + "/" + TOTAL_RIDDLES + " §fénigmes résolues");
                    if (remaining > 0) {
                        player.sendMessage("§e▸ §fRestant: §c" + remaining + " §fénigmes");
                        activateGPSToOracle(player);
                    }
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Saumons
        activeSalmonPlayers.remove(playerId);

        // Minage - on garde les données pour la reconnexion
        activeMiningPlayers.remove(playerId);

        // Traître - on garde les données pour la reconnexion
        activeTraitorPlayers.remove(playerId);
        // Note: on ne supprime PAS playerInterrogatedSuspects pour que le joueur puisse reprendre

        // Bûcheronnage - on garde les données pour la reconnexion
        activeLumberPlayers.remove(playerId);
        // Note: on ne supprime PAS playerCollectedLumber pour que le joueur puisse reprendre

        // Grenouilles - on garde les données pour la reconnexion
        activeFrogPlayers.remove(playerId);
        // Note: on ne supprime PAS playerCapturedFrogs pour que le joueur puisse reprendre

        // Annuler le mini-jeu de grenouille si en cours
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && frogCaptureGUI.hasActiveGame(player)) {
            frogCaptureGUI.cancelGame(player);
        }

        // Énigmes - on garde les données pour la reconnexion
        activeRiddlePlayers.remove(playerId);
        // Note: on ne supprime PAS playerCurrentRiddle pour que le joueur puisse reprendre
    }

    // ==================== SYSTÈME DU BOSS GRENOUILLE GÉANTE (Étape 5.10) ====================

    /**
     * Initialise le boss de la Grenouille Géante
     */
    private void initializeSwampFrogBoss(World world) {
        spawnSwampFrogBoss(world);
    }

    /**
     * Démarre le checker de respawn du boss
     */
    private void startBossRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                // Vérifier si le boss doit être respawné
                if (swampFrogBossEntity == null || !swampFrogBossEntity.isValid() || swampFrogBossEntity.isDead()) {
                    if (!bossRespawnScheduled) {
                        // Check si joueur proche pour spawn
                        Location bossLoc = SWAMP_FROG_BOSS_LOCATION.clone();
                        bossLoc.setWorld(world);

                        boolean playerNearby = world.getPlayers().stream()
                            .anyMatch(p -> p.getLocation().distanceSquared(bossLoc) < 10000); // 100 blocs

                        if (playerNearby && bossLoc.getChunk().isLoaded()) {
                            spawnSwampFrogBoss(world);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 100L); // Check toutes les 5 secondes
    }

    /**
     * Fait spawn le boss Grenouille Géante via le système ZombieZ
     */
    private void spawnSwampFrogBoss(World world) {
        // Protection anti-spawn multiple
        if (swampFrogBossEntity != null && swampFrogBossEntity.isValid() && !swampFrogBossEntity.isDead()) {
            return;
        }

        Location loc = SWAMP_FROG_BOSS_LOCATION.clone();
        loc.setWorld(world);

        if (!loc.getChunk().isLoaded()) {
            return;
        }

        // Chercher un boss existant dans le monde
        for (Entity entity : world.getNearbyEntities(loc, 50, 30, 50)) {
            if (entity instanceof Zombie z
                    && z.getPersistentDataContainer().has(SWAMP_FROG_BOSS_KEY, PersistentDataType.BYTE)) {
                swampFrogBossEntity = z;
                return;
            }
        }

        // Nettoyer les contributeurs
        bossContributors.clear();
        bossRespawnScheduled = false;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.getLogger().warning("ZombieManager non disponible pour le boss grenouille");
            return;
        }

        // Spawn via ZombieManager avec l'IA SwampFrogBossAI
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.SWAMP_FROG_BOSS, loc, BOSS_LEVEL);

        if (activeZombie == null) {
            plugin.getLogger().warning("Échec du spawn du boss Grenouille Géante");
            return;
        }

        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (!(entity instanceof Zombie boss)) {
            plugin.getLogger().warning("Boss Grenouille n'est pas un Zombie valide");
            return;
        }

        swampFrogBossEntity = boss;

        // Appliquer les visuels
        applySwampFrogBossVisuals(boss);

        // Marquer comme boss pour le tracking
        boss.getPersistentDataContainer().set(SWAMP_FROG_BOSS_KEY, PersistentDataType.BYTE, (byte) 1);
        boss.setPersistent(true);

        // Créer le TextDisplay au-dessus
        spawnBossDisplay(world, loc);

        // Annoncer le spawn
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(loc) < 80) {
                player.sendMessage("");
                player.sendMessage("§2§l🐸 Une Grenouille Géante émerge du marais!");
                player.playSound(player.getLocation(), Sound.ENTITY_FROG_LONG_JUMP, 1.5f, 0.3f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.5f);
            }
        }

        plugin.getLogger().info("§a§lBoss Grenouille Géante spawné (Chapitre 5)");
    }

    /**
     * Applique les visuels au boss
     */
    private void applySwampFrogBossVisuals(Zombie boss) {
        // Scale x4 (grenouille géante)
        var scale = boss.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(3.5);
        }

        // Pas d'équipement (c'est une grenouille)
        boss.getEquipment().clear();

        // Couleur verte (effet de potion pour le glow)
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        boss.setGlowing(true);
        boss.setCustomNameVisible(true);
    }

    /**
     * Crée le TextDisplay au-dessus du boss
     */
    private void spawnBossDisplay(World world, Location loc) {
        if (swampFrogBossDisplay != null && swampFrogBossDisplay.isValid()) {
            swampFrogBossDisplay.remove();
        }

        Location displayLoc = loc.clone().add(0, 4.5, 0);
        swampFrogBossDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text("🐸 ", NamedTextColor.GREEN)
                .append(Component.text("GRENOUILLE GÉANTE", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text(" 🐸", NamedTextColor.GREEN)));
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(180, 0, 80, 0));
            display.setSeeThrough(false);
            display.setViewRange(48f);

            Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(2.0f, 2.0f, 2.0f),
                new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transform);

            display.setPersistent(false);
        });
    }

    /**
     * Appelé quand un joueur atteint l'étape 5.10
     */
    public void onPlayerReachStep510(Player player) {
        player.sendTitle("§2§l🐸 BOSS FINAL 🐸", "§7Terrasse la Grenouille Géante!", 10, 60, 20);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§8§m                                              ");
            player.sendMessage("§2§l    🐸 GRENOUILLE GÉANTE DU MARAIS 🐸");
            player.sendMessage("§8§m                                              ");
            player.sendMessage("");
            player.sendMessage("§7Une §2créature monstrueuse §7hante les marais.");
            player.sendMessage("§7Cette grenouille mutante est la source de la corruption.");
            player.sendMessage("");
            player.sendMessage("§e§l➤ §7Zone: §2Marais - 384, 93, 8034");
            player.sendMessage("");
            player.sendMessage("§c⚠ §7Attaques du boss:");
            player.sendMessage("§e  • §fLangue Venimeuse §7- T'attire vers elle");
            player.sendMessage("§e  • §fSaut Écrasant §7- Saute et atterrit sur toi");
            player.sendMessage("§e  • §fCrachat Toxique §7- Projectiles empoisonnés");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_FROG_AMBIENT, 1f, 0.5f);
        }, 40L);
    }

    /**
     * Tracker les joueurs qui attaquent le boss
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwampFrogBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie boss)) return;
        if (!boss.getPersistentDataContainer().has(SWAMP_FROG_BOSS_KEY, PersistentDataType.BYTE)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Projectile projectile &&
                projectile.getShooter() instanceof Player player) {
            damager = player;
        }

        if (damager != null) {
            bossContributors.add(damager.getUniqueId());
        }
    }

    /**
     * Gère la mort du boss
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwampFrogBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie boss)) return;
        if (!boss.getPersistentDataContainer().has(SWAMP_FROG_BOSS_KEY, PersistentDataType.BYTE)) return;

        Location deathLoc = boss.getLocation();
        World world = boss.getWorld();

        // Effets de mort épiques
        world.playSound(deathLoc, Sound.ENTITY_FROG_DEATH, 2f, 0.3f);
        world.playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 5, 2, 1, 2, 0);
        world.spawnParticle(Particle.ITEM_SLIME, deathLoc, 100, 3, 2, 3, 0);

        // Valider l'étape pour tous les contributeurs
        for (UUID uuid : bossContributors) {
            Player contributor = Bukkit.getPlayer(uuid);
            if (contributor != null && contributor.isOnline()) {
                JourneyStep currentStep = journeyManager.getCurrentStep(contributor);
                if (currentStep != null && currentStep == JourneyStep.STEP_5_10) {
                    journeyManager.updateProgress(contributor, JourneyStep.StepType.KILL_SWAMP_FROG_BOSS, 1);

                    contributor.sendMessage("");
                    contributor.sendMessage("§2§l🐸 La Grenouille Géante a été vaincue!");
                    contributor.sendMessage("§7Tu as contribué à sa défaite.");
                    contributor.sendMessage("§a§lChapitre 5 terminé!");
                    contributor.sendMessage("");

                    // Son de victoire
                    contributor.playSound(contributor.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }
        }

        // Nettoyer
        bossContributors.clear();
        swampFrogBossEntity = null;

        // Supprimer le display
        if (swampFrogBossDisplay != null && swampFrogBossDisplay.isValid()) {
            swampFrogBossDisplay.remove();
        }

        // Programmer le respawn
        if (!bossRespawnScheduled) {
            bossRespawnScheduled = true;
            bossRespawnTime = System.currentTimeMillis() + (BOSS_RESPAWN_SECONDS * 1000L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnSwampFrogBoss(world);
                    bossRespawnTime = 0;
                }
            }.runTaskLater(plugin, 20L * BOSS_RESPAWN_SECONDS);

            // Annoncer le respawn
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(deathLoc) < 100) {
                    player.sendMessage("§7La Grenouille Géante réapparaîtra dans §e" + BOSS_RESPAWN_SECONDS + " secondes§7...");
                }
            }
        }
    }

    /**
     * Nettoyage lors de la désactivation du plugin
     */
    public void cleanup() {
        // Saumons
        if (salmonSpawnTask != null) {
            salmonSpawnTask.cancel();
        }
        cleanupPlayerSalmons();
        activeSalmonPlayers.clear();

        // Minerais
        for (int i = 0; i < TOTAL_ORES; i++) {
            if (oreVisuals[i] != null && oreVisuals[i].isValid()) {
                oreVisuals[i].remove();
            }
            if (oreHitboxes[i] != null && oreHitboxes[i].isValid()) {
                oreHitboxes[i].remove();
            }
            if (oreDisplays[i] != null && oreDisplays[i].isValid()) {
                oreDisplays[i].remove();
            }
        }
        playerOreHits.clear();
        playerMinedOres.clear();
        activeMiningPlayers.clear();

        // Suspects
        for (int i = 0; i < TOTAL_SUSPECTS; i++) {
            if (suspectNPCs[i] != null && suspectNPCs[i].isValid()) {
                suspectGlowTeam.removeEntity(suspectNPCs[i]);
                suspectNPCs[i].remove();
            }
            if (suspectDisplays[i] != null && suspectDisplays[i].isValid()) {
                suspectDisplays[i].remove();
            }
        }
        playerInterrogatedSuspects.clear();
        activeTraitorPlayers.clear();

        // Traîtres actifs
        for (UUID traitorId : playerActiveTraitors.values()) {
            Entity traitor = plugin.getServer().getEntity(traitorId);
            if (traitor != null && traitor.isValid()) {
                traitor.remove();
            }
        }
        playerActiveTraitors.clear();

        // Bois
        for (int i = 0; i < TOTAL_LUMBER; i++) {
            if (lumberVisuals[i] != null && lumberVisuals[i].isValid()) {
                lumberVisuals[i].remove();
            }
            if (lumberHitboxes[i] != null && lumberHitboxes[i].isValid()) {
                lumberHitboxes[i].remove();
            }
        }
        playerCollectedLumber.clear();
        playerLumberInInventory.clear();
        activeLumberPlayers.clear();

        // Bûcheron
        if (lumberjackNPC != null && lumberjackNPC.isValid()) {
            lumberjackNPC.remove();
        }
        if (lumberjackDisplay != null && lumberjackDisplay.isValid()) {
            lumberjackDisplay.remove();
        }

        // Grenouilles
        for (int i = 0; i < TOTAL_FROGS; i++) {
            if (frogEntities[i] != null && frogEntities[i].isValid()) {
                frogEntities[i].remove();
            }
        }
        playerCapturedFrogs.clear();
        playerFrogsInInventory.clear();
        activeFrogPlayers.clear();

        // Biologiste
        if (biologistNPC != null && biologistNPC.isValid()) {
            biologistNPC.remove();
        }
        if (biologistDisplay != null && biologistDisplay.isValid()) {
            biologistDisplay.remove();
        }

        // GUI du mini-jeu
        if (frogCaptureGUI != null) {
            frogCaptureGUI.cleanup();
        }

        // Oracle
        if (oracleNPC != null && oracleNPC.isValid()) {
            oracleNPC.remove();
        }
        if (oracleDisplay != null && oracleDisplay.isValid()) {
            oracleDisplay.remove();
        }
        activeRiddlePlayers.clear();
        playerCurrentRiddle.clear();

        // Boss Grenouille Géante
        if (swampFrogBossEntity != null && swampFrogBossEntity.isValid()) {
            swampFrogBossEntity.remove();
        }
        if (swampFrogBossDisplay != null && swampFrogBossDisplay.isValid()) {
            swampFrogBossDisplay.remove();
        }
        bossContributors.clear();

        plugin.getLogger().info("[Chapter5Systems] Cleanup effectué");
    }
}
