package com.rinaorc.zombiez.progression.journey.chapter4;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère les systèmes spécifiques au Chapitre 4:
 * - Étape 2: Le Fossoyeur
 *   - Phase 1: Parler au prêtre
 *   - Phase 2: Creuser 5 tombes (ArmorStands à frapper)
 *   - Phase 3: Tuer le boss "Le Premier Mort"
 * - Étape 3: La Récolte Maudite
 *   - Collecter 12 champignons rouges dans la zone
 *   - Les livrer au collecteur
 * - Étape 6: Purification des Âmes
 *   - Tuer des Âmes Damnées dans le cimetière (33% drop Purificateur)
 *   - Utiliser le Purificateur sur les Âmes Damnées pour les libérer
 *   - Purifier 5 âmes -> transformation en villageois + nuage de fumée
 */
public class Chapter4Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    // Fossoyeur
    private final NamespacedKey PRIEST_NPC_KEY;
    private final NamespacedKey GRAVE_VISUAL_KEY;
    private final NamespacedKey GRAVE_HITBOX_KEY;
    private final NamespacedKey GRAVEDIGGER_BOSS_KEY;
    // Champignons
    private final NamespacedKey MUSHROOM_COLLECTOR_KEY;
    private final NamespacedKey MUSHROOM_HITBOX_KEY;
    // Âmes Damnées
    private final NamespacedKey DAMNED_SOUL_KEY;
    private final NamespacedKey PURIFIER_ITEM_KEY;
    // Brume Toxique
    private final NamespacedKey CORRUPTION_SOURCE_KEY;
    // Arbre Maudit (Creaking Boss)
    private final NamespacedKey ORB_HITBOX_KEY;

    // === POSITIONS ===
    // Prêtre du cimetière
    private static final Location PRIEST_LOCATION = new Location(null, 656.5, 91, 8682.5, -90, 0);

    // Tombes à creuser (5 positions)
    private static final Location[] GRAVE_LOCATIONS = {
            new Location(null, 665, 90, 8730, 0, 0),   // Tombe 1
            new Location(null, 669, 90, 8717, 0, 0),   // Tombe 2
            new Location(null, 685, 89, 8728, 0, 0),   // Tombe 3
            new Location(null, 676, 89, 8740, 0, 0),   // Tombe 4
            new Location(null, 663, 89, 8740, 0, 0)    // Tombe 5
    };

    // Collecteur de champignons
    private static final Location MUSHROOM_COLLECTOR_LOCATION = new Location(null, 434.5, 113, 8680.5, -90, 0);

    // Zone des champignons (corners)
    private static final int MUSHROOM_ZONE_MIN_X = 453;
    private static final int MUSHROOM_ZONE_MAX_X = 495;
    private static final int MUSHROOM_ZONE_Y = 92;
    private static final int MUSHROOM_ZONE_MIN_Z = 8661;
    private static final int MUSHROOM_ZONE_MAX_Z = 8713;

    // Zone du cimetière pour les Âmes Damnées (corners: 891,87,8607 à 937,86,8673)
    private static final int SOUL_ZONE_MIN_X = 891;
    private static final int SOUL_ZONE_MAX_X = 937;
    private static final int SOUL_ZONE_MIN_Y = 86;
    private static final int SOUL_ZONE_MAX_Y = 87;
    private static final int SOUL_ZONE_MIN_Z = 8607;
    private static final int SOUL_ZONE_MAX_Z = 8673;

    // === CONFIGURATION ===
    private static final int HITS_TO_DIG = 10; // Nombre de coups pour creuser une tombe
    private static final double GRAVE_VIEW_DISTANCE = 48;
    private static final double PRIEST_DISPLAY_HEIGHT = 2.5;
    private static final double GRAVE_DISPLAY_HEIGHT = 1.8;

    // Configuration champignons
    private static final int MUSHROOM_COUNT = 20; // Nombre de champignons à spawner
    private static final int MUSHROOMS_TO_COLLECT = 12; // Nombre à collecter
    private static final int HITS_TO_COLLECT_MUSHROOM = 2; // 1-3 coups pour collecter
    private static final double MUSHROOM_VIEW_DISTANCE = 32;

    // Configuration Âmes Damnées
    private static final int MAX_DAMNED_SOULS = 12; // Nombre max d'âmes damnées en même temps
    private static final double PURIFIER_DROP_CHANCE = 0.40; // 40% de chance de drop
    private static final int SOULS_TO_PURIFY = 5; // Nombre d'âmes à purifier

    // === BRUME TOXIQUE (ÉTAPE 7) ===
    // Zone des marécages (corners: 752,85,8402 à 863,86,8498)
    private static final int SWAMP_ZONE_MIN_X = 752;
    private static final int SWAMP_ZONE_MAX_X = 863;
    private static final int SWAMP_ZONE_MIN_Y = 80;
    private static final int SWAMP_ZONE_MAX_Y = 95;
    private static final int SWAMP_ZONE_MIN_Z = 8402;
    private static final int SWAMP_ZONE_MAX_Z = 8498;

    // Positions des 4 sources de corruption
    private static final Location[] CORRUPTION_SOURCE_LOCATIONS = {
            new Location(null, 770, 85, 8430, 0, 0),   // Source 1 (Sud-Ouest)
            new Location(null, 840, 85, 8420, 0, 0),   // Source 2 (Sud-Est)
            new Location(null, 780, 86, 8480, 0, 0),   // Source 3 (Nord-Ouest)
            new Location(null, 830, 85, 8470, 0, 0)    // Source 4 (Nord-Est) - Mini-boss
    };

    // Configuration Brume Toxique
    private static final int CORRUPTION_SOURCE_COUNT = 4; // Nombre de sources de corruption
    private static final int HITS_TO_DESTROY_SOURCE = 15; // Coups pour détruire une source
    private static final int SWAMP_WALKERS_PER_SOURCE = 3; // Mobs gardiens par source
    private static final double POISON_DAMAGE = 1.0; // Dégâts de poison par tick
    private static final int POISON_TICK_INTERVAL = 40; // Interval en ticks (2 secondes)

    // === ARBRE MAUDIT - CREAKING BOSS (ÉTAPE 8) ===
    // Positions des 8 orbes autour de l'arbre
    private static final Location[] ORB_LOCATIONS = {
            new Location(null, 462.5, 91, 8523.5, 0, 0),   // Orbe 1
            new Location(null, 453.5, 98, 8519.5, 0, 0),   // Orbe 2 (en hauteur)
            new Location(null, 442.5, 92, 8525.5, 0, 0),   // Orbe 3
            new Location(null, 442.5, 91, 8510.5, 0, 0),   // Orbe 4
            new Location(null, 453.5, 96, 8506.5, 0, 0),   // Orbe 5 (en hauteur)
            new Location(null, 460.5, 91, 8505.5, 0, 0),   // Orbe 6
            new Location(null, 469.5, 95, 8510.5, 0, 0),   // Orbe 7 (en hauteur)
            new Location(null, 460.5, 91, 8519.5, 0, 0)    // Orbe 8
    };

    // Position de spawn du boss Creaking
    private static final Location CREAKING_BOSS_SPAWN = new Location(null, 453.5, 91, 8530.5, 0, 0);

    // Configuration Arbre Maudit
    private static final int ORB_COUNT = 8;
    private static final double ORB_VIEW_DISTANCE = 50;

    // === TRACKING ENTITÉS ===
    private Entity priestEntity;
    private TextDisplay priestDisplay;

    // Tombes (ItemDisplay COARSE_DIRT glowing + Interaction hitbox + TextDisplay)
    private final ItemDisplay[] graveVisuals = new ItemDisplay[5];
    private final Interaction[] graveHitboxes = new Interaction[5];
    private final TextDisplay[] graveDisplays = new TextDisplay[5];

    // Champignons (ItemDisplay RED_MUSHROOM scale x3 glowing + Interaction hitbox)
    private final List<ItemDisplay> mushroomVisuals = new ArrayList<>();
    private final List<Interaction> mushroomHitboxes = new ArrayList<>();
    private final List<Location> mushroomLocations = new ArrayList<>();

    // Collecteur de champignons
    private Entity mushroomCollectorEntity;
    private TextDisplay mushroomCollectorDisplay;

    // === TRACKING JOUEURS ===
    // Joueurs ayant parlé au prêtre (Phase 1 complétée)
    private final Set<UUID> playersWhoTalkedToPriest = ConcurrentHashMap.newKeySet();

    // Progression du creusage par joueur: graveIndex -> hits
    private final Map<UUID, int[]> playerGraveHits = new ConcurrentHashMap<>();

    // Ordre des tombes par joueur (randomisé, la 5ème est toujours le boss)
    private final Map<UUID, int[]> playerGraveOrder = new ConcurrentHashMap<>();

    // Nombre de tombes creusées par joueur
    private final Map<UUID, Integer> playerGravesDug = new ConcurrentHashMap<>();

    // Joueurs ayant tué le boss
    private final Set<UUID> playersWhoKilledBoss = ConcurrentHashMap.newKeySet();

    // Boss actifs par joueur
    private final Map<UUID, UUID> playerBossMap = new ConcurrentHashMap<>();

    // Joueurs ayant un boss actif (pour éviter le double spawn)
    private final Set<UUID> playersWithActiveBoss = ConcurrentHashMap.newKeySet();

    // === TRACKING CHAMPIGNONS ===
    // Champignons collectés par joueur (compteur virtuel)
    private final Map<UUID, Integer> playerMushroomsCollected = new ConcurrentHashMap<>();

    // Hits sur chaque champignon par joueur: mushroomIndex -> hits
    private final Map<UUID, int[]> playerMushroomHits = new ConcurrentHashMap<>();

    // Joueurs ayant complété la quête champignons
    private final Set<UUID> playersWhoCompletedMushrooms = ConcurrentHashMap.newKeySet();

    // === TRACKING ÂMES DAMNÉES ===
    // Âmes purifiées par joueur
    private final Map<UUID, Integer> playerSoulsPurified = new ConcurrentHashMap<>();

    // Joueurs ayant complété la quête de purification
    private final Set<UUID> playersWhoCompletedSouls = ConcurrentHashMap.newKeySet();

    // Joueurs ayant reçu l'introduction de la quête (pour éviter spam)
    private final Set<UUID> playersIntroducedToSouls = ConcurrentHashMap.newKeySet();

    // === TRACKING BRUME TOXIQUE ===
    // Sources de corruption (ItemDisplay visuels + Interaction hitbox)
    private final ItemDisplay[] corruptionSourceVisuals = new ItemDisplay[4];
    private final Interaction[] corruptionSourceHitboxes = new Interaction[4];
    private final TextDisplay[] corruptionSourceDisplays = new TextDisplay[4];

    // Hits sur chaque source par joueur: sourceIndex -> hits
    private final Map<UUID, int[]> playerSourceHits = new ConcurrentHashMap<>();

    // Sources détruites par joueur
    private final Map<UUID, Set<Integer>> playerDestroyedSources = new ConcurrentHashMap<>();

    // Joueurs ayant complété la quête
    private final Set<UUID> playersWhoCompletedToxicFog = ConcurrentHashMap.newKeySet();

    // Joueurs ayant reçu l'introduction
    private final Set<UUID> playersIntroducedToToxicFog = ConcurrentHashMap.newKeySet();

    // Joueurs actuellement dans la zone (pour poison)
    private final Set<UUID> playersInSwampZone = ConcurrentHashMap.newKeySet();

    // === TRACKING ARBRE MAUDIT (ÉTAPE 8) ===
    // Orbes (ItemDisplay END_CRYSTAL glowing + Interaction hitbox)
    private final ItemDisplay[] orbVisuals = new ItemDisplay[ORB_COUNT];
    private final Interaction[] orbHitboxes = new Interaction[ORB_COUNT];

    // Orbes collectées par joueur (Set des indices)
    private final Map<UUID, Set<Integer>> playerCollectedOrbs = new ConcurrentHashMap<>();

    // Joueurs ayant complété la quête
    private final Set<UUID> playersWhoCompletedCreakingQuest = ConcurrentHashMap.newKeySet();

    // Joueurs ayant reçu l'introduction
    private final Set<UUID> playersIntroducedToCreaking = ConcurrentHashMap.newKeySet();

    // Boss Creaking actif par joueur
    private final Map<UUID, UUID> playerCreakingBossMap = new ConcurrentHashMap<>();

    // Joueurs ayant un boss Creaking actif
    private final Set<UUID> playersWithActiveCreakingBoss = ConcurrentHashMap.newKeySet();

    // Compteur pour le nombre total de sources détruites (step progress)
    private final Map<UUID, Integer> playerSourcesDestroyed = new ConcurrentHashMap<>();

    public Chapter4Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.PRIEST_NPC_KEY = new NamespacedKey(plugin, "gravedigger_priest");
        this.GRAVE_VISUAL_KEY = new NamespacedKey(plugin, "grave_visual");
        this.GRAVE_HITBOX_KEY = new NamespacedKey(plugin, "grave_hitbox");
        this.GRAVEDIGGER_BOSS_KEY = new NamespacedKey(plugin, "gravedigger_boss");
        this.MUSHROOM_COLLECTOR_KEY = new NamespacedKey(plugin, "mushroom_collector");
        this.MUSHROOM_HITBOX_KEY = new NamespacedKey(plugin, "mushroom_hitbox");
        this.DAMNED_SOUL_KEY = new NamespacedKey(plugin, "damned_soul");
        this.PURIFIER_ITEM_KEY = new NamespacedKey(plugin, "soul_purifier");
        this.CORRUPTION_SOURCE_KEY = new NamespacedKey(plugin, "corruption_source");
        this.ORB_HITBOX_KEY = new NamespacedKey(plugin, "orb_hitbox");

        // Enregistrer le listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Démarrer l'initialisation avec délai
        new BukkitRunnable() {
            @Override
            public void run() {
                initialize();
            }
        }.runTaskLater(plugin, 100L);
    }

    /**
     * Initialise les systèmes du Chapitre 4
     */
    public void initialize() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.log(Level.WARNING, "§cImpossible d'initialiser Chapter4Systems: monde 'world' non trouvé");
            return;
        }

        // Nettoyer les anciennes entités
        cleanupOldEntities(world);

        // === ÉTAPE 2: LE FOSSOYEUR ===
        // Spawn le prêtre
        spawnPriest(world);

        // Spawn les tombes
        spawnGraves(world);

        // Démarrer les systèmes de mise à jour
        startPriestRespawnChecker();
        startGraveVisibilityUpdater();
        startGraveRespawnChecker();

        // === ÉTAPE 3: LA RÉCOLTE MAUDITE ===
        // Spawn le collecteur de champignons
        spawnMushroomCollector(world);

        // Générer les positions des champignons
        generateMushroomLocations(world);

        // Spawn les champignons
        spawnMushrooms(world);

        // Démarrer les systèmes de mise à jour
        startMushroomCollectorRespawnChecker();
        startMushroomVisibilityUpdater();
        startMushroomRespawnChecker();

        // === ÉTAPE 6: PURIFICATION DES ÂMES ===
        // Démarrer le spawn des Âmes Damnées dans la zone du cimetière
        startDamnedSoulSpawner(world);

        // === ÉTAPE 7: LA BRUME TOXIQUE ===
        // Spawn les sources de corruption
        spawnCorruptionSources(world);

        // Démarrer les systèmes de mise à jour
        startCorruptionSourceVisibilityUpdater();
        startCorruptionSourceRespawnChecker();
        startSwampPoisonChecker();

        // === ÉTAPE 8: L'ARBRE MAUDIT ===
        // Spawn les orbes autour de l'arbre
        spawnOrbs(world);

        // Démarrer les systèmes de mise à jour
        startOrbVisibilityUpdater();
        startOrbRespawnChecker();

        plugin.log(Level.INFO, "§a✓ Chapter4Systems initialisé (Fossoyeur, Récolte, Purification, Brume Toxique, Arbre Maudit)");
    }

    /**
     * Nettoie les anciennes entités
     */
    private void cleanupOldEntities(World world) {
        // Nettoyer le prêtre
        Location priestLoc = PRIEST_LOCATION.clone();
        priestLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(priestLoc, 10, 10, 10)) {
            if (entity.getScoreboardTags().contains("chapter4_priest")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter4_priest_display")) {
                entity.remove();
            }
        }

        // Nettoyer les tombes
        for (Location graveLoc : GRAVE_LOCATIONS) {
            Location loc = graveLoc.clone();
            loc.setWorld(world);

            for (Entity entity : world.getNearbyEntities(loc, 10, 10, 10)) {
                if (entity.getScoreboardTags().contains("chapter4_grave")) {
                    entity.remove();
                }
                if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter4_grave_display")) {
                    entity.remove();
                }
            }
        }

        // Nettoyer les boss du fossoyeur
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter4_gravedigger_boss")) {
                entity.remove();
            }
        }

        // Nettoyer le collecteur de champignons
        Location collectorLoc = MUSHROOM_COLLECTOR_LOCATION.clone();
        collectorLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(collectorLoc, 10, 10, 10)) {
            if (entity.getScoreboardTags().contains("chapter4_mushroom_collector")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter4_mushroom_collector_display")) {
                entity.remove();
            }
        }

        // Nettoyer les champignons
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter4_mushroom")) {
                entity.remove();
            }
        }

        // Nettoyer les sources de corruption
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter4_corruption_source")) {
                entity.remove();
            }
        }

        // Nettoyer les orbes et le boss Creaking
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter4_orb") ||
                entity.getScoreboardTags().contains("chapter4_creaking_boss")) {
                entity.remove();
            }
        }
    }

    // ==================== PRÊTRE (PHASE 1) ====================

    /**
     * Spawn le PNJ prêtre
     */
    private void spawnPriest(World world) {
        Location loc = PRIEST_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (priestEntity != null && priestEntity.isValid()) {
            priestEntity.remove();
        }
        if (priestDisplay != null && priestDisplay.isValid()) {
            priestDisplay.remove();
        }

        // Spawn le Villager prêtre
        priestEntity = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("Père Augustin", NamedTextColor.GOLD, TextDecoration.BOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setProfession(Villager.Profession.CLERIC);
            villager.setVillagerType(Villager.Type.PLAINS);

            // Tags
            villager.addScoreboardTag("chapter4_priest");
            villager.addScoreboardTag("no_trading");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(PRIEST_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister
            villager.setPersistent(false);

            // Orientation
            villager.setRotation(-90, 0);
        });

        // Créer le TextDisplay au-dessus
        createPriestDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du prêtre
     */
    private void createPriestDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, PRIEST_DISPLAY_HEIGHT, 0);

        priestDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("✝ ", NamedTextColor.WHITE))
                    .append(Component.text("LE PRÊTRE", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" ✝", NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text("─────────", NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit", NamedTextColor.WHITE))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.8f, 1.8f, 1.8f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_priest_display");
        });
    }

    /**
     * Démarre le vérificateur de respawn du prêtre
     */
    private void startPriestRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                if (priestEntity == null || !priestEntity.isValid() || priestEntity.isDead()) {
                    spawnPriest(world);
                    plugin.log(Level.FINE, "Prêtre respawné (entité invalide)");
                }

                if (priestDisplay == null || !priestDisplay.isValid()) {
                    Location loc = PRIEST_LOCATION.clone();
                    loc.setWorld(world);
                    createPriestDisplay(world, loc);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    /**
     * Gère l'interaction avec le prêtre
     */
    private void handlePriestInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si déjà complété la quête entière
        if (hasPlayerCompletedQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§6§lPère Augustin: §f\"Que la paix soit avec toi, héros.\"");
            player.sendMessage("§6§lPère Augustin: §f\"Tu as libéré les âmes tourmentées du cimetière.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Si déjà parlé au prêtre, rappeler l'objectif
        if (hasPlayerTalkedToPriest(player)) {
            int gravesDug = playerGravesDug.getOrDefault(player.getUniqueId(), 0);

            if (gravesDug >= 5) {
                if (playersWithActiveBoss.contains(player.getUniqueId())) {
                    player.sendMessage("");
                    player.sendMessage("§6§lPère Augustin: §f\"Le Premier Mort est réveillé!\"");
                    player.sendMessage("§6§lPère Augustin: §f\"Tu dois le vaincre pour libérer le cimetière!\"");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
                    player.sendMessage("");
                } else {
                    player.sendMessage("");
                    player.sendMessage("§6§lPère Augustin: §f\"Tu as creusé toutes les tombes...\"");
                    player.sendMessage("§6§lPère Augustin: §f\"Le Premier Mort devrait apparaître bientôt!\"");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
                    player.sendMessage("");
                }
            } else {
                player.sendMessage("");
                player.sendMessage("§6§lPère Augustin: §f\"Continue de creuser les tombes, mon enfant.\"");
                player.sendMessage("§6§lPère Augustin: §f\"Il te reste §e" + (5 - gravesDug) + " tombes§f à examiner.\"");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
                player.sendMessage("");
            }
            return;
        }

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_4_2) {
            player.sendMessage("");
            player.sendMessage("§6§lPère Augustin: §f\"Bonjour, voyageur.\"");
            player.sendMessage("§6§lPère Augustin: §f\"Ce cimetière cache de sombres secrets...\"");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Dialogue d'introduction
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
        player.sendMessage("  §6§lPère Augustin: §f\"Ah, un survivant!\"");
        player.sendMessage("");
        player.sendMessage("  §6§lPère Augustin: §f\"Ce cimetière est maudit depuis");
        player.sendMessage("  §fque l'épidémie a frappé notre village.\"");
        player.sendMessage("");
        player.sendMessage("  §6§lPère Augustin: §f\"§c5 tombes§f renferment des âmes");
        player.sendMessage("  §fimpures qu'il faut libérer.\"");
        player.sendMessage("");
        player.sendMessage("  §6§lPère Augustin: §f\"Mais attention... L'une d'elles");
        player.sendMessage("  §fabrite §4Le Premier Mort§f, l'origine du mal.\"");
        player.sendMessage("");
        player.sendMessage("  §e➤ §fVa creuser les §e5 tombes lumineuses§f!");
        player.sendMessage("  §7(Frappe les tombes pour les creuser)");
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.8f);

        // Marquer comme ayant parlé au prêtre
        playersWhoTalkedToPriest.add(player.getUniqueId());

        // Initialiser la progression
        initializePlayerGraveProgress(player);

        // Incrémenter la progression (Phase 1 complétée)
        journeyManager.incrementProgress(player, JourneyStep.StepType.GRAVEDIGGER_QUEST, 1);

        // Afficher le titre
        player.sendTitle("§6✝ LE FOSSOYEUR", "§7Creuse les 5 tombes maudites", 10, 60, 20);

        // Activer le GPS vers la première tombe
        activateGPSToNearestGrave(player);
    }

    /**
     * Active le GPS vers la tombe non creusée la plus proche
     */
    private void activateGPSToNearestGrave(Player player) {
        Location nearestGrave = findNearestUndugGrave(player);
        if (nearestGrave != null) {
            // Afficher les coordonnées dans le chat comme guide
            player.sendMessage("");
            player.sendMessage("§e§l➤ §7Tombe la plus proche: §e" +
                    nearestGrave.getBlockX() + ", " +
                    nearestGrave.getBlockY() + ", " +
                    nearestGrave.getBlockZ());
            player.sendMessage("§7Suis les §dtombes lumineuses §7dans le cimetière!");
            player.sendMessage("");
        }
    }

    /**
     * Trouve la tombe non creusée la plus proche du joueur
     */
    private Location findNearestUndugGrave(Player player) {
        Location playerLoc = player.getLocation();
        Location nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int i = 0; i < 5; i++) {
            if (!hasPlayerDugGrave(player, i)) {
                Location graveLoc = GRAVE_LOCATIONS[i].clone();
                graveLoc.setWorld(player.getWorld());
                double distSq = playerLoc.distanceSquared(graveLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = graveLoc;
                }
            }
        }

        return nearest;
    }

    /**
     * Initialise la progression des tombes pour un joueur
     */
    private void initializePlayerGraveProgress(Player player) {
        UUID uuid = player.getUniqueId();

        // Initialiser les hits à 0 pour chaque tombe
        playerGraveHits.put(uuid, new int[5]);

        // Créer un ordre aléatoire pour les tombes (la dernière creusée = boss)
        int[] order = {0, 1, 2, 3, 4};
        shuffleArray(order);
        playerGraveOrder.put(uuid, order);

        // Initialiser le compteur
        playerGravesDug.put(uuid, 0);
    }

    /**
     * Mélange un tableau (Fisher-Yates shuffle)
     */
    private void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    // ==================== TOMBES (PHASE 2) ====================

    /**
     * Spawn les ArmorStands des tombes
     */
    private void spawnGraves(World world) {
        for (int i = 0; i < 5; i++) {
            spawnGrave(world, i);
        }
    }

    /**
     * Spawn un ArmorStand de tombe
     */
    private void spawnGrave(World world, int graveIndex) {
        Location loc = GRAVE_LOCATIONS[graveIndex].clone();
        loc.setWorld(world);

        // Supprimer les anciens
        if (graveVisuals[graveIndex] != null && graveVisuals[graveIndex].isValid()) {
            graveVisuals[graveIndex].remove();
        }
        if (graveHitboxes[graveIndex] != null && graveHitboxes[graveIndex].isValid()) {
            graveHitboxes[graveIndex].remove();
        }
        if (graveDisplays[graveIndex] != null && graveDisplays[graveIndex].isValid()) {
            graveDisplays[graveIndex].remove();
        }

        // 1. Créer le VISUEL (ItemDisplay avec COARSE_DIRT glowing)
        graveVisuals[graveIndex] = world.spawn(loc.clone().add(0, 0.5, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.COARSE_DIRT));

            // Taille légèrement plus grande pour visibilité
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 1, 0)
            ));

            display.setBillboard(Display.Billboard.FIXED);

            // Glow effect violet pour effet mystique
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(180, 100, 255));

            display.setViewRange(48f);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_grave_visual");
            display.addScoreboardTag("grave_visual_" + graveIndex);

            // PDC
            display.getPersistentDataContainer().set(GRAVE_VISUAL_KEY, PersistentDataType.INTEGER, graveIndex);
        });

        // 2. Créer l'entité INTERACTION (hitbox cliquable/frappable)
        graveHitboxes[graveIndex] = world.spawn(loc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(1.5f);
            interaction.setInteractionHeight(1.5f);
            interaction.setResponsive(true); // Active la réponse aux attaques (left-click)

            // Tags
            interaction.addScoreboardTag("chapter4_grave_hitbox");
            interaction.addScoreboardTag("grave_hitbox_" + graveIndex);
            interaction.addScoreboardTag("zombiez_npc");

            // PDC
            interaction.getPersistentDataContainer().set(GRAVE_HITBOX_KEY, PersistentDataType.INTEGER, graveIndex);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });

        // 3. Créer le TextDisplay au-dessus
        createGraveDisplay(world, loc, graveIndex);
    }

    /**
     * Crée le TextDisplay au-dessus d'une tombe
     */
    private void createGraveDisplay(World world, Location loc, int graveIndex) {
        Location displayLoc = loc.clone().add(0, GRAVE_DISPLAY_HEIGHT, 0);

        graveDisplays[graveIndex] = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("⚰ ", NamedTextColor.DARK_PURPLE))
                    .append(Component.text("TOMBE " + (graveIndex + 1), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.text(" ⚰", NamedTextColor.DARK_PURPLE))
                    .append(Component.newline())
                    .append(Component.text("▶ Frappe pour creuser", NamedTextColor.GRAY))
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
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.4f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_grave_display");
            display.addScoreboardTag("grave_display_" + graveIndex);

            // Invisible par défaut
            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour les tombes
     */
    private void startGraveVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllGravesForPlayer(player);
                        continue;
                    }

                    // Vérifier si le joueur a parlé au prêtre et n'a pas fini
                    boolean shouldSeeGraves = hasPlayerTalkedToPriest(player) &&
                            !hasPlayerCompletedQuest(player);

                    if (shouldSeeGraves) {
                        updateGraveVisibilityForPlayer(player);
                    } else {
                        hideAllGravesForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des tombes pour un joueur
     */
    private void updateGraveVisibilityForPlayer(Player player) {
        for (int i = 0; i < 5; i++) {
            boolean hasDigThis = hasPlayerDugGrave(player, i);

            // Distance check
            boolean inRange = false;
            if (graveVisuals[i] != null && graveVisuals[i].isValid()) {
                double distSq = player.getLocation().distanceSquared(graveVisuals[i].getLocation());
                inRange = distSq <= GRAVE_VIEW_DISTANCE * GRAVE_VIEW_DISTANCE;
            }

            // Visual (COARSE_DIRT block)
            if (graveVisuals[i] != null && graveVisuals[i].isValid()) {
                if (hasDigThis || !inRange) {
                    player.hideEntity(plugin, graveVisuals[i]);
                } else {
                    player.showEntity(plugin, graveVisuals[i]);
                }
            }

            // Hitbox (Interaction)
            if (graveHitboxes[i] != null && graveHitboxes[i].isValid()) {
                if (hasDigThis || !inRange) {
                    player.hideEntity(plugin, graveHitboxes[i]);
                } else {
                    player.showEntity(plugin, graveHitboxes[i]);
                }
            }

            // TextDisplay
            if (graveDisplays[i] != null && graveDisplays[i].isValid()) {
                if (hasDigThis || !inRange) {
                    player.hideEntity(plugin, graveDisplays[i]);
                } else {
                    player.showEntity(plugin, graveDisplays[i]);
                }
            }
        }
    }

    /**
     * Cache toutes les tombes pour un joueur
     */
    private void hideAllGravesForPlayer(Player player) {
        for (int i = 0; i < 5; i++) {
            if (graveVisuals[i] != null && graveVisuals[i].isValid()) {
                player.hideEntity(plugin, graveVisuals[i]);
            }
            if (graveHitboxes[i] != null && graveHitboxes[i].isValid()) {
                player.hideEntity(plugin, graveHitboxes[i]);
            }
            if (graveDisplays[i] != null && graveDisplays[i].isValid()) {
                player.hideEntity(plugin, graveDisplays[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des tombes
     */
    private void startGraveRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < 5; i++) {
                    boolean needsRespawn = (graveVisuals[i] == null || !graveVisuals[i].isValid()) ||
                            (graveHitboxes[i] == null || !graveHitboxes[i].isValid()) ||
                            (graveDisplays[i] == null || !graveDisplays[i].isValid());

                    if (needsRespawn) {
                        spawnGrave(world, i);
                        plugin.log(Level.FINE, "Tombe " + i + " respawnée");
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Vérifie si un joueur a creusé une tombe spécifique
     */
    private boolean hasPlayerDugGrave(Player player, int graveIndex) {
        int[] hits = playerGraveHits.get(player.getUniqueId());
        if (hits == null) return false;
        return hits[graveIndex] >= HITS_TO_DIG;
    }

    /**
     * Gère le hit sur une tombe
     */
    private void handleGraveHit(Player player, int graveIndex) {
        // Vérifier si le joueur a parlé au prêtre
        if (!hasPlayerTalkedToPriest(player)) {
            player.sendMessage("§7Une vieille tombe... Le prêtre du cimetière sait peut-être quelque chose.");
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1f);
            return;
        }

        // Vérifier si la quête est finie
        if (hasPlayerCompletedQuest(player)) {
            return;
        }

        // Vérifier si cette tombe est déjà creusée
        if (hasPlayerDugGrave(player, graveIndex)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int[] hits = playerGraveHits.get(uuid);
        if (hits == null) {
            initializePlayerGraveProgress(player);
            hits = playerGraveHits.get(uuid);
        }

        // Incrémenter les hits
        hits[graveIndex]++;
        int currentHits = hits[graveIndex];

        // Afficher la progression dans le TextDisplay de la tombe
        double progress = (double) currentHits / HITS_TO_DIG;
        updateGraveDisplayProgress(graveIndex, currentHits, HITS_TO_DIG);

        // Effets de creusage
        Location graveLoc = GRAVE_LOCATIONS[graveIndex].clone();
        graveLoc.setWorld(player.getWorld());

        player.playSound(graveLoc, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.8f + (float) (progress * 0.4));
        player.getWorld().spawnParticle(Particle.BLOCK, graveLoc.add(0, 0.5, 0), 10, 0.5, 0.3, 0.5,
                Material.COARSE_DIRT.createBlockData());

        // Tombe creusée!
        if (currentHits >= HITS_TO_DIG) {
            onGraveDug(player, graveIndex);
        }
    }

    /**
     * Met à jour le TextDisplay d'une tombe avec la progression
     */
    private void updateGraveDisplayProgress(int graveIndex, int currentHits, int maxHits) {
        TextDisplay display = graveDisplays[graveIndex];
        if (display == null || !display.isValid()) return;

        String progressBar = createProgressBar((double) currentHits / maxHits);

        display.text(Component.text()
                .append(Component.text("⚰ ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("TOMBE " + (graveIndex + 1), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" ⚰", NamedTextColor.DARK_PURPLE))
                .append(Component.newline())
                .append(Component.text("⛏ ", NamedTextColor.YELLOW))
                .append(Component.text(progressBar, NamedTextColor.WHITE))
                .append(Component.text(" " + currentHits + "/" + maxHits, NamedTextColor.GRAY))
                .build());
    }

    /**
     * Crée une barre de progression textuelle
     */
    private String createProgressBar(double progress) {
        int filled = (int) (progress * 10);
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        return bar.toString();
    }

    /**
     * Appelé quand une tombe est creusée
     */
    private void onGraveDug(Player player, int graveIndex) {
        UUID uuid = player.getUniqueId();
        int gravesDug = playerGravesDug.getOrDefault(uuid, 0) + 1;
        playerGravesDug.put(uuid, gravesDug);

        Location graveLoc = GRAVE_LOCATIONS[graveIndex].clone();
        graveLoc.setWorld(player.getWorld());

        // Cacher la tombe pour ce joueur
        updateGraveVisibilityForPlayer(player);

        // Incrémenter la progression du Journey (+1 par tombe)
        journeyManager.incrementProgress(player, JourneyStep.StepType.GRAVEDIGGER_QUEST, 1);

        // Effets de découverte
        player.playSound(graveLoc, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);
        player.getWorld().spawnParticle(Particle.SOUL, graveLoc.add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.02);

        // Est-ce la dernière tombe (spawn boss)?
        if (gravesDug >= 5) {
            // Dernière tombe = spawn le boss!
            onFinalGraveDug(player, graveIndex);
        } else {
            // 50/50 trésor ou zombies
            boolean isTreasure = new Random().nextBoolean();

            if (isTreasure) {
                onTreasureFound(player, graveLoc, gravesDug);
            } else {
                onZombiesSpawn(player, graveLoc, gravesDug);
            }

            // Activer le GPS vers la prochaine tombe
            activateGPSToNearestGrave(player);
        }

        // Message de progression
        if (gravesDug < 5) {
            player.sendMessage("§e⚰ Tombe " + gravesDug + "/5 creusée!");
        }
    }

    /**
     * Appelé quand le joueur trouve un trésor dans une tombe
     */
    private void onTreasureFound(Player player, Location loc, int gravesDug) {
        // Points bonus basés sur la progression (25-50 points)
        int bonusPoints = 25 + (gravesDug * 5);

        player.sendMessage("");
        player.sendMessage("§a§l✦ TRÉSOR TROUVÉ!");
        player.sendMessage("§7Tu as découvert §e+" + bonusPoints + " points§7!");
        player.sendMessage("");

        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        // Donner les points
        plugin.getEconomyManager().addPoints(player, bonusPoints, "Trésor du Fossoyeur");
    }

    /**
     * Appelé quand des zombies sortent d'une tombe
     */
    private void onZombiesSpawn(Player player, Location loc, int gravesDug) {
        player.sendMessage("");
        player.sendMessage("§c§l☠ DES MORTS SORTENT DE TERRE!");
        player.sendMessage("§7Élimine-les rapidement!");
        player.sendMessage("");

        player.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.7f);
        player.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 0.5, 0), 30, 0.8, 0.5, 0.8, 0.05);

        // Spawn des zombies via ZombieManager
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        int count = 2 + gravesDug; // Plus de zombies au fur et à mesure
        int zombieLevel = 15 + (gravesDug * 2);

        for (int i = 0; i < count; i++) {
            Location spawnLoc = loc.clone().add(
                    (Math.random() - 0.5) * 3,
                    1,
                    (Math.random() - 0.5) * 3
            );

            ZombieType type = Math.random() < 0.3 ? ZombieType.SKELETON : ZombieType.WALKER;
            zombieManager.spawnZombie(type, spawnLoc, zombieLevel);
        }
    }

    /**
     * Appelé quand la dernière tombe est creusée - spawn le boss
     */
    private void onFinalGraveDug(Player player, int graveIndex) {
        Location graveLoc = GRAVE_LOCATIONS[graveIndex].clone();
        graveLoc.setWorld(player.getWorld());

        // Marquer le joueur comme ayant un boss actif
        playersWithActiveBoss.add(player.getUniqueId());

        // Effets dramatiques
        player.sendTitle("§4§l☠ LE PREMIER MORT", "§cS'éveille de sa tombe...", 10, 60, 20);
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
        player.sendMessage("  §4§lLE PREMIER MORT S'ÉVEILLE!");
        player.sendMessage("");
        player.sendMessage("  §7La terre tremble... Une silhouette");
        player.sendMessage("  §7gigantesque émerge de la dernière tombe!");
        player.sendMessage("");
        player.sendMessage("  §c⚔ Prépare-toi au combat!");
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.7f);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, graveLoc.add(0, 1, 0), 100, 1, 2, 1, 0.1);

        // Spawn le boss après un court délai
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                spawnGravediggerBoss(player, graveLoc);
            }
        }.runTaskLater(plugin, 40L);
    }

    // ==================== BOSS (PHASE 3) ====================

    // Position fixe de spawn du boss Fossoyeur
    private static final Location GRAVEDIGGER_BOSS_SPAWN = new Location(null, 668.5, 90, 8733.5);

    /**
     * Spawn le boss "Le Premier Mort"
     */
    private void spawnGravediggerBoss(Player player, Location loc) {
        World world = player.getWorld();
        if (world == null) return;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(Level.WARNING, "ZombieManager non disponible pour spawn du boss Fossoyeur");
            return;
        }

        // Position fixe du boss (indépendant de la dernière tombe creusée)
        Location bossSpawnLoc = GRAVEDIGGER_BOSS_SPAWN.clone();
        bossSpawnLoc.setWorld(world);

        // Spawn via ZombieManager
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(
                ZombieType.GRAVEDIGGER_BOSS,
                bossSpawnLoc,
                20 // Niveau 20
        );

        if (activeZombie == null) {
            plugin.log(Level.WARNING, "Échec du spawn du boss Fossoyeur");
            return;
        }

        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (!(entity instanceof Zombie bossZombie)) {
            plugin.log(Level.WARNING, "Le boss Fossoyeur n'est pas un Zombie");
            return;
        }

        // Configurer le boss
        configureBoss(bossZombie, player);

        // Tracker le boss pour ce joueur
        playerBossMap.put(player.getUniqueId(), bossZombie.getUniqueId());

        // Effets de spawn
        world.playSound(bossSpawnLoc, Sound.ENTITY_WITHER_SKELETON_AMBIENT, 2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, bossSpawnLoc, 2, 0, 0, 0);

        // Indiquer la position du boss au joueur
        player.sendMessage("§c§l⚠ §7Le boss est apparu en §e" + (int) bossSpawnLoc.getX() + ", " +
                (int) bossSpawnLoc.getY() + ", " + (int) bossSpawnLoc.getZ() + "§7!");
    }

    /**
     * Configure le boss après spawn
     */
    private void configureBoss(Zombie boss, Player player) {
        // Scale x3 (géant)
        var scale = boss.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(3.0);
        }

        // Tags
        boss.addScoreboardTag("chapter4_gravedigger_boss");
        boss.addScoreboardTag("boss_owner_" + player.getUniqueId());

        // PDC
        boss.getPersistentDataContainer().set(GRAVEDIGGER_BOSS_KEY, PersistentDataType.STRING,
                player.getUniqueId().toString());

        // Équipement visuel (style Wither Skeleton)
        boss.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        boss.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        boss.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));

        // Ne pas drop l'équipement
        boss.getEquipment().setHelmetDropChance(0);
        boss.getEquipment().setChestplateDropChance(0);
        boss.getEquipment().setItemInMainHandDropChance(0);

        // Target le joueur
        boss.setTarget(player);

        // Glowing rouge pour visibilité
        boss.setGlowing(true);
        applyRedGlowing(boss);
    }

    /**
     * Applique un glowing rouge au boss via le scoreboard
     */
    private void applyRedGlowing(Entity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team redTeam = scoreboard.getTeam("gravedigger_boss_red");
        if (redTeam == null) {
            redTeam = scoreboard.registerNewTeam("gravedigger_boss_red");
            redTeam.color(NamedTextColor.RED);
        }
        redTeam.addEntity(entity);
    }

    /**
     * Gère la mort du boss Fossoyeur
     */
    private void handleBossKilled(Player killer, Zombie boss) {
        // Vérifier le propriétaire du boss
        String ownerUUID = boss.getPersistentDataContainer().get(GRAVEDIGGER_BOSS_KEY, PersistentDataType.STRING);
        if (ownerUUID == null) return;

        UUID ownerUuid = UUID.fromString(ownerUUID);

        // Donner le crédit au propriétaire original
        Player owner = plugin.getServer().getPlayer(ownerUuid);
        if (owner != null && owner.isOnline()) {
            completeQuest(owner);
        }

        // Si le killer est différent du owner, lui donner aussi du crédit
        if (killer != null && !killer.getUniqueId().equals(ownerUuid)) {
            // Donner juste un message sympa
            killer.sendMessage("§a✦ Tu as aidé à vaincre Le Premier Mort!");
        }

        // Cleanup
        playersWithActiveBoss.remove(ownerUuid);
        playerBossMap.remove(ownerUuid);
    }

    /**
     * Complète la quête pour un joueur
     */
    private void completeQuest(Player player) {
        playersWhoKilledBoss.add(player.getUniqueId());

        // Incrémenter la progression pour compléter l'étape
        // Phase 1 (prêtre) = 1, Phase 2 (5 tombes) = 5, Phase 3 (boss) = 1
        // Total = 7 pour compléter
        int currentProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_2);
        int remaining = 7 - currentProgress;
        if (remaining > 0) {
            journeyManager.incrementProgress(player, JourneyStep.StepType.GRAVEDIGGER_QUEST, remaining);
        }

        // Message de victoire
        player.sendTitle("§a§l✦ VICTOIRE!", "§7Le Premier Mort est vaincu", 10, 60, 20);
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
        player.sendMessage("  §a§l✦ LE PREMIER MORT EST VAINCU!");
        player.sendMessage("");
        player.sendMessage("  §7Tu as libéré les âmes du cimetière");
        player.sendMessage("  §7et mis fin à la malédiction!");
        player.sendMessage("");
        player.sendMessage("  §e+300 Points §7| §a+10 Gems");
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.3);
    }

    // ==================== ÉTAPE 3: LA RÉCOLTE MAUDITE ====================

    /**
     * Spawn le PNJ collecteur de champignons
     */
    private void spawnMushroomCollector(World world) {
        Location loc = MUSHROOM_COLLECTOR_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (mushroomCollectorEntity != null && mushroomCollectorEntity.isValid()) {
            mushroomCollectorEntity.remove();
        }
        if (mushroomCollectorDisplay != null && mushroomCollectorDisplay.isValid()) {
            mushroomCollectorDisplay.remove();
        }

        // Spawn le Villager collecteur
        mushroomCollectorEntity = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("Mère Cueillette", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setProfession(Villager.Profession.FARMER);
            villager.setVillagerType(Villager.Type.SWAMP);

            // Tags
            villager.addScoreboardTag("chapter4_mushroom_collector");
            villager.addScoreboardTag("no_trading");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(MUSHROOM_COLLECTOR_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister
            villager.setPersistent(false);

            // Orientation
            villager.setRotation(-90, 0);
        });

        // Créer le TextDisplay au-dessus
        createMushroomCollectorDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du collecteur
     */
    private void createMushroomCollectorDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, 2.5, 0);

        mushroomCollectorDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("🍄 ", NamedTextColor.RED))
                    .append(Component.text("LA CUEILLEUSE", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                    .append(Component.text(" 🍄", NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.text("─────────────", NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit", NamedTextColor.WHITE))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.8f, 1.8f, 1.8f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_mushroom_collector_display");
        });
    }

    /**
     * Démarre le vérificateur de respawn du collecteur
     */
    private void startMushroomCollectorRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                if (mushroomCollectorEntity == null || !mushroomCollectorEntity.isValid() || mushroomCollectorEntity.isDead()) {
                    spawnMushroomCollector(world);
                    plugin.log(Level.FINE, "Collecteur de champignons respawné");
                }

                if (mushroomCollectorDisplay == null || !mushroomCollectorDisplay.isValid()) {
                    Location loc = MUSHROOM_COLLECTOR_LOCATION.clone();
                    loc.setWorld(world);
                    createMushroomCollectorDisplay(world, loc);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    /**
     * Génère les positions aléatoires des champignons dans la zone
     */
    private void generateMushroomLocations(World world) {
        mushroomLocations.clear();
        Random random = new Random();

        for (int i = 0; i < MUSHROOM_COUNT; i++) {
            int x = MUSHROOM_ZONE_MIN_X + random.nextInt(MUSHROOM_ZONE_MAX_X - MUSHROOM_ZONE_MIN_X + 1);
            int z = MUSHROOM_ZONE_MIN_Z + random.nextInt(MUSHROOM_ZONE_MAX_Z - MUSHROOM_ZONE_MIN_Z + 1);

            // Trouver le bloc solide le plus haut
            Location loc = new Location(world, x + 0.5, MUSHROOM_ZONE_Y, z + 0.5);
            loc = world.getHighestBlockAt(loc).getLocation().add(0.5, 1, 0.5);

            mushroomLocations.add(loc);
        }
    }

    /**
     * Spawn tous les champignons
     */
    private void spawnMushrooms(World world) {
        // Nettoyer les anciens visuels
        for (ItemDisplay visual : mushroomVisuals) {
            if (visual != null && visual.isValid()) {
                visual.remove();
            }
        }
        mushroomVisuals.clear();

        // Nettoyer les anciennes hitboxes
        for (Interaction hitbox : mushroomHitboxes) {
            if (hitbox != null && hitbox.isValid()) {
                hitbox.remove();
            }
        }
        mushroomHitboxes.clear();

        // Spawner les nouveaux
        for (int i = 0; i < mushroomLocations.size(); i++) {
            spawnMushroom(world, i);
        }
    }

    /**
     * Spawn un champignon à l'index donné (ItemDisplay + Interaction)
     */
    private void spawnMushroom(World world, int index) {
        if (index >= mushroomLocations.size()) return;

        Location loc = mushroomLocations.get(index);

        // 1. Créer le VISUEL (ItemDisplay avec RED_MUSHROOM scale x3 glowing rouge)
        ItemDisplay visual = world.spawn(loc.clone().add(0, 0.5, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.RED_MUSHROOM));

            // Scale x3 pour visibilité
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(3.0f, 3.0f, 3.0f),
                    new AxisAngle4f(0, 0, 1, 0)
            ));

            display.setBillboard(Display.Billboard.FIXED);

            // Glow effect rouge
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(255, 50, 50));

            display.setViewRange(48f);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_mushroom");
            display.addScoreboardTag("chapter4_mushroom_visual");
            display.addScoreboardTag("mushroom_visual_" + index);
        });

        // 2. Créer l'entité INTERACTION (hitbox invisible cliquable/frappable)
        Interaction hitbox = world.spawn(loc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(1.5f);
            interaction.setInteractionHeight(1.5f);
            interaction.setResponsive(true); // Active la réponse aux attaques (left-click)

            // Tags
            interaction.addScoreboardTag("chapter4_mushroom");
            interaction.addScoreboardTag("chapter4_mushroom_hitbox");
            interaction.addScoreboardTag("mushroom_hitbox_" + index);
            interaction.addScoreboardTag("zombiez_npc");

            // PDC
            interaction.getPersistentDataContainer().set(MUSHROOM_HITBOX_KEY, PersistentDataType.INTEGER, index);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });

        // Ajouter aux listes
        while (mushroomVisuals.size() <= index) {
            mushroomVisuals.add(null);
        }
        mushroomVisuals.set(index, visual);

        while (mushroomHitboxes.size() <= index) {
            mushroomHitboxes.add(null);
        }
        mushroomHitboxes.set(index, hitbox);
    }

    /**
     * Démarre le système de visibilité per-player pour les champignons
     */
    private void startMushroomVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllMushroomsForPlayer(player);
                        continue;
                    }

                    // Le joueur voit les champignons s'il est à l'étape 4_3 et n'a pas fini
                    JourneyStep currentStep = journeyManager.getCurrentStep(player);
                    boolean shouldSeeMushrooms = currentStep == JourneyStep.STEP_4_3 &&
                            !hasPlayerCompletedMushroomQuest(player);

                    if (shouldSeeMushrooms) {
                        updateMushroomVisibilityForPlayer(player);
                    } else {
                        hideAllMushroomsForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des champignons pour un joueur
     */
    private void updateMushroomVisibilityForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        int[] hits = playerMushroomHits.get(uuid);

        for (int i = 0; i < mushroomVisuals.size(); i++) {
            ItemDisplay visual = mushroomVisuals.get(i);
            Interaction hitbox = i < mushroomHitboxes.size() ? mushroomHitboxes.get(i) : null;

            if (visual == null || !visual.isValid()) continue;

            // Vérifier si ce champignon a été collecté par ce joueur
            boolean collected = hits != null && hits.length > i && hits[i] >= getHitsForMushroom(i);

            // Distance check
            double distSq = player.getLocation().distanceSquared(visual.getLocation());
            boolean inRange = distSq <= MUSHROOM_VIEW_DISTANCE * MUSHROOM_VIEW_DISTANCE;

            if (collected || !inRange) {
                player.hideEntity(plugin, visual);
                if (hitbox != null && hitbox.isValid()) {
                    player.hideEntity(plugin, hitbox);
                }
            } else {
                player.showEntity(plugin, visual);
                if (hitbox != null && hitbox.isValid()) {
                    player.showEntity(plugin, hitbox);
                }
            }
        }
    }

    /**
     * Cache tous les champignons pour un joueur
     */
    private void hideAllMushroomsForPlayer(Player player) {
        for (ItemDisplay visual : mushroomVisuals) {
            if (visual != null && visual.isValid()) {
                player.hideEntity(plugin, visual);
            }
        }
        for (Interaction hitbox : mushroomHitboxes) {
            if (hitbox != null && hitbox.isValid()) {
                player.hideEntity(plugin, hitbox);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des champignons
     */
    private void startMushroomRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < mushroomLocations.size(); i++) {
                    // Vérifier si le visuel ou la hitbox sont invalides
                    boolean needsRespawn = i >= mushroomVisuals.size() ||
                            mushroomVisuals.get(i) == null ||
                            !mushroomVisuals.get(i).isValid() ||
                            i >= mushroomHitboxes.size() ||
                            mushroomHitboxes.get(i) == null ||
                            !mushroomHitboxes.get(i).isValid();

                    if (needsRespawn) {
                        spawnMushroom(world, i);
                        plugin.log(Level.FINE, "Champignon " + i + " respawné");
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Retourne le nombre de hits requis pour collecter un champignon (1-3)
     */
    private int getHitsForMushroom(int index) {
        // Pseudo-random basé sur l'index
        return 1 + (index % 3);
    }

    /**
     * Gère l'interaction avec le collecteur de champignons
     */
    private void handleMushroomCollectorInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si la quête est déjà complète
        if (hasPlayerCompletedMushroomQuest(player)) {
            player.sendMessage("");
            player.sendMessage("§5§lMère Cueillette: §f\"Merci encore pour ton aide, survivant.\"");
            player.sendMessage("§5§lMère Cueillette: §f\"Les champignons nous ont beaucoup appris...\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_4_3) {
            player.sendMessage("");
            player.sendMessage("§5§lMère Cueillette: §f\"Bonjour, étranger...\"");
            player.sendMessage("§5§lMère Cueillette: §f\"Ces champignons renferment des secrets étranges.\"");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        int mushroomsCollected = playerMushroomsCollected.getOrDefault(player.getUniqueId(), 0);

        // Si le joueur n'a pas encore 12 champignons
        if (mushroomsCollected < MUSHROOMS_TO_COLLECT) {
            if (mushroomsCollected == 0) {
                // Premier dialogue - démarrer la quête
                player.sendMessage("");
                player.sendMessage("§8§m                                        ");
                player.sendMessage("");
                player.sendMessage("  §5§lMère Cueillette: §f\"Ah, un courageux!\"");
                player.sendMessage("");
                player.sendMessage("  §5§lMère Cueillette: §f\"Ces §cchampignons rouges§f qui");
                player.sendMessage("  §fpoussent dans la forêt sont §ccontaminés§f.\"");
                player.sendMessage("");
                player.sendMessage("  §5§lMère Cueillette: §f\"J'ai besoin de §c12 échantillons§f");
                player.sendMessage("  §fpour mes recherches sur le virus.\"");
                player.sendMessage("");
                player.sendMessage("  §e➤ §fCollecte §e12 champignons rouges§f dans la forêt!");
                player.sendMessage("  §7(Frappe-les pour les cueillir)");
                player.sendMessage("");
                player.sendMessage("§8§m                                        ");
                player.sendMessage("");

                // Effets
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                // Afficher le titre
                player.sendTitle("§c🍄 LA RÉCOLTE MAUDITE", "§7Collecte 12 champignons rouges", 10, 60, 20);

                // GPS vers la zone des champignons
                activateGPSToMushroomZone(player);
            } else {
                // Rappel
                player.sendMessage("");
                player.sendMessage("§5§lMère Cueillette: §f\"Tu n'as que §e" + mushroomsCollected + " champignons§f.\"");
                player.sendMessage("§5§lMère Cueillette: §f\"Il m'en faut §c12§f au total!\"");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.sendMessage("");

                // GPS vers la zone des champignons
                activateGPSToMushroomZone(player);
            }
            return;
        }

        // Le joueur a 12 champignons - livraison!
        onMushroomQuestComplete(player);
    }

    /**
     * Active le GPS vers la zone des champignons
     */
    private void activateGPSToMushroomZone(Player player) {
        int centerX = (MUSHROOM_ZONE_MIN_X + MUSHROOM_ZONE_MAX_X) / 2;
        int centerZ = (MUSHROOM_ZONE_MIN_Z + MUSHROOM_ZONE_MAX_Z) / 2;

        player.sendMessage("");
        player.sendMessage("§e§l➤ §7Zone des champignons: §e" + centerX + ", " + MUSHROOM_ZONE_Y + ", " + centerZ);
        player.sendMessage("§7Cherche les §cchampignons lumineux §7dans la forêt!");
        player.sendMessage("");
    }

    /**
     * Active le GPS vers le collecteur
     */
    private void activateGPSToCollector(Player player) {
        Location loc = MUSHROOM_COLLECTOR_LOCATION;
        player.sendMessage("");
        player.sendMessage("§e§l➤ §7Retourne voir §5Mère Cueillette§7: §e" +
                (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ());
        player.sendMessage("§7Tu as §c12 champignons§7, livre-les!");
        player.sendMessage("");
    }

    /**
     * Gère le hit sur un champignon
     */
    private void handleMushroomHit(Player player, int mushroomIndex) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_4_3) {
            player.sendMessage("§7Un champignon rouge étrange... Peut-être que quelqu'un en a besoin?");
            player.playSound(player.getLocation(), Sound.BLOCK_FUNGUS_HIT, 0.5f, 1f);
            return;
        }

        // Vérifier si la quête est finie
        if (hasPlayerCompletedMushroomQuest(player)) {
            return;
        }

        // Vérifier si le joueur a déjà 12 champignons
        int collected = playerMushroomsCollected.getOrDefault(player.getUniqueId(), 0);
        if (collected >= MUSHROOMS_TO_COLLECT) {
            player.sendMessage("§7Tu as déjà §c12 champignons§7! Retourne voir §5Mère Cueillette§7.");
            return;
        }

        UUID uuid = player.getUniqueId();
        int[] hits = playerMushroomHits.get(uuid);
        if (hits == null) {
            hits = new int[MUSHROOM_COUNT];
            playerMushroomHits.put(uuid, hits);
        }

        // Vérifier si ce champignon est déjà collecté
        int requiredHits = getHitsForMushroom(mushroomIndex);
        if (hits[mushroomIndex] >= requiredHits) {
            return;
        }

        // Incrémenter les hits
        hits[mushroomIndex]++;
        int currentHits = hits[mushroomIndex];

        // Effets de cueillette
        ItemDisplay visual = mushroomIndex < mushroomVisuals.size() ? mushroomVisuals.get(mushroomIndex) : null;
        if (visual != null) {
            Location loc = visual.getLocation();
            player.playSound(loc, Sound.BLOCK_FUNGUS_BREAK, 0.8f, 1f + (float) currentHits / requiredHits * 0.5f);
            player.getWorld().spawnParticle(Particle.ITEM, loc.add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.05,
                    new ItemStack(Material.RED_MUSHROOM));
        }

        // Champignon collecté!
        if (currentHits >= requiredHits) {
            onMushroomCollected(player, mushroomIndex);
        }
    }

    /**
     * Appelé quand un champignon est collecté
     */
    private void onMushroomCollected(Player player, int mushroomIndex) {
        UUID uuid = player.getUniqueId();
        int collected = playerMushroomsCollected.getOrDefault(uuid, 0) + 1;
        playerMushroomsCollected.put(uuid, collected);

        // Incrémenter la progression Journey (+1 par champignon)
        journeyManager.incrementProgress(player, JourneyStep.StepType.MUSHROOM_COLLECTION, 1);

        // Cacher ce champignon pour le joueur
        updateMushroomVisibilityForPlayer(player);

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

        // Message
        if (collected < MUSHROOMS_TO_COLLECT) {
            player.sendMessage("§c🍄 §7Champignon collecté! §e" + collected + "/" + MUSHROOMS_TO_COLLECT);
        } else {
            // 12 champignons collectés!
            player.sendMessage("");
            player.sendMessage("§a§l✦ §7Tu as collecté §c12 champignons§7!");
            player.sendMessage("§e➤ §7Retourne voir §5Mère Cueillette§7!");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.sendTitle("§c🍄 §a12/12", "§7Retourne voir Mère Cueillette!", 10, 40, 20);

            // GPS vers le collecteur
            activateGPSToCollector(player);
        }
    }

    /**
     * Appelé quand la quête champignons est complétée
     */
    private void onMushroomQuestComplete(Player player) {
        playersWhoCompletedMushrooms.add(player.getUniqueId());

        // Incrémenter la progression pour compléter l'étape (13 = 12 champignons + 1 livraison)
        journeyManager.incrementProgress(player, JourneyStep.StepType.MUSHROOM_COLLECTION, 1);

        // Message avec lore sur Patient Zéro
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");
        player.sendMessage("  §a§l✦ QUÊTE COMPLÉTÉE!");
        player.sendMessage("");
        player.sendMessage("  §5§lMère Cueillette: §f\"Magnifique travail!\"");
        player.sendMessage("");
        player.sendMessage("  §7Elle examine attentivement les champignons...");
        player.sendMessage("");
        player.sendMessage("  §5§lMère Cueillette: §f\"Ces spores... Je les reconnais.\"");
        player.sendMessage("");
        player.sendMessage("  §5§lMère Cueillette: §f\"Elles proviennent d'expériences");
        player.sendMessage("  §fmenées au §4Laboratoire Helix§f, à l'est.\"");
        player.sendMessage("");
        player.sendMessage("  §5§lMère Cueillette: §f\"Le §4Patient Zéro§f... Il travaillait");
        player.sendMessage("  §flà-bas avant que tout ne dégénère.\"");
        player.sendMessage("");
        player.sendMessage("  §5§lMère Cueillette: §f\"Ces champignons absorbent le virus");
        player.sendMessage("  §fet le transforment. Ils pourraient être la §aclé§f");
        player.sendMessage("  §fpour comprendre l'origine de l'épidémie.\"");
        player.sendMessage("");
        player.sendMessage("  §e+400 Points §7| §a+12 Gems");
        player.sendMessage("");
        player.sendMessage("  §8§oIndice: Le Laboratoire Helix cache des secrets...");
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.sendTitle("§a§l✦ QUÊTE COMPLÉTÉE!", "§7Les champignons révèlent leurs secrets...", 10, 60, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.3);
    }

    /**
     * Vérifie si le joueur a complété la quête champignons
     */
    public boolean hasPlayerCompletedMushroomQuest(Player player) {
        if (playersWhoCompletedMushrooms.contains(player.getUniqueId())) {
            return true;
        }
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_3);
        return progress >= 13; // 12 champignons + 1 livraison
    }

    // ==================== HELPERS ====================

    public boolean hasPlayerTalkedToPriest(Player player) {
        if (playersWhoTalkedToPriest.contains(player.getUniqueId())) {
            return true;
        }
        // Vérifier via la progression du Journey
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_2);
        return progress >= 1;
    }

    public boolean hasPlayerCompletedQuest(Player player) {
        if (playersWhoKilledBoss.contains(player.getUniqueId())) {
            return true;
        }
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_2);
        return progress >= 7; // 1 (prêtre) + 5 (tombes) + 1 (boss) = 7
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Interaction avec le prêtre
        if (entity.getPersistentDataContainer().has(PRIEST_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handlePriestInteraction(player);
            return;
        }

        // Interaction avec le collecteur de champignons
        if (entity.getPersistentDataContainer().has(MUSHROOM_COLLECTOR_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleMushroomCollectorInteraction(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();

        // Hit sur une tombe (Interaction hitbox)
        if (damaged instanceof Interaction && damaged.getPersistentDataContainer().has(GRAVE_HITBOX_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true); // Annuler l'événement

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer graveIndex = damaged.getPersistentDataContainer().get(GRAVE_HITBOX_KEY, PersistentDataType.INTEGER);
                if (graveIndex != null) {
                    handleGraveHit(attacker, graveIndex);
                }
            }
            return;
        }

        // Hit sur un champignon (Interaction hitbox)
        if (damaged instanceof Interaction && damaged.getPersistentDataContainer().has(MUSHROOM_HITBOX_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true); // Annuler l'événement

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer mushroomIndex = damaged.getPersistentDataContainer().get(MUSHROOM_HITBOX_KEY, PersistentDataType.INTEGER);
                if (mushroomIndex != null) {
                    handleMushroomHit(attacker, mushroomIndex);
                }
            }
            return;
        }

        // Hit sur une source de corruption (Interaction hitbox)
        if (damaged instanceof Interaction && damaged.getPersistentDataContainer().has(CORRUPTION_SOURCE_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true); // Annuler l'événement

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer sourceIndex = damaged.getPersistentDataContainer().get(CORRUPTION_SOURCE_KEY, PersistentDataType.INTEGER);
                if (sourceIndex != null) {
                    handleCorruptionSourceHit(attacker, sourceIndex);
                }
            }
            return;
        }

        // Hit sur une orbe (Interaction hitbox)
        if (damaged instanceof Interaction && damaged.getPersistentDataContainer().has(ORB_HITBOX_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true); // Annuler l'événement

            Player attacker = null;
            if (event.getDamager() instanceof Player p) {
                attacker = p;
            } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                attacker = p;
            }

            if (attacker != null) {
                Integer orbIndex = damaged.getPersistentDataContainer().get(ORB_HITBOX_KEY, PersistentDataType.INTEGER);
                if (orbIndex != null) {
                    handleOrbCollected(attacker, orbIndex);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Mort du boss Fossoyeur
        if (entity instanceof Zombie zombie && entity.getScoreboardTags().contains("chapter4_gravedigger_boss")) {
            Player killer = zombie.getKiller();
            handleBossKilled(killer, zombie);
        }

        // Mort du boss Creaking (c'est un Zombie avec le tag chapter4_creaking_boss)
        if (entity instanceof Zombie creakingBoss && entity.getScoreboardTags().contains("chapter4_creaking_boss")) {
            Player killer = creakingBoss.getKiller();
            handleCreakingBossKilled(killer, creakingBoss);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Recharger la progression depuis le Journey
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_2);

                if (progress >= 1) {
                    playersWhoTalkedToPriest.add(player.getUniqueId());

                    // Reconstruire la progression des tombes
                    if (!playerGraveHits.containsKey(player.getUniqueId())) {
                        initializePlayerGraveProgress(player);

                        // Estimer les tombes creusées (progress - 1 pour le prêtre)
                        int estimatedGraves = Math.min(5, Math.max(0, progress - 1));
                        playerGravesDug.put(player.getUniqueId(), estimatedGraves);

                        // Marquer les premières tombes comme creusées
                        int[] hits = playerGraveHits.get(player.getUniqueId());
                        for (int i = 0; i < estimatedGraves && i < 5; i++) {
                            hits[i] = HITS_TO_DIG;
                        }
                    }
                }

                if (progress >= 7) {
                    playersWhoKilledBoss.add(player.getUniqueId());
                }

                // Recharger la progression champignons
                int mushroomProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_3);
                if (mushroomProgress >= 13) {
                    playersWhoCompletedMushrooms.add(player.getUniqueId());
                } else if (mushroomProgress > 0) {
                    // Reconstruire le compteur de champignons collectés
                    int estimatedMushrooms = Math.min(12, mushroomProgress);
                    playerMushroomsCollected.put(player.getUniqueId(), estimatedMushrooms);

                    // Initialiser les hits (marquer les premiers comme collectés)
                    int[] hits = new int[MUSHROOM_COUNT];
                    for (int i = 0; i < estimatedMushrooms && i < MUSHROOM_COUNT; i++) {
                        hits[i] = getHitsForMushroom(i);
                    }
                    playerMushroomHits.put(player.getUniqueId(), hits);
                }

                // Recharger la progression purification des âmes
                int soulProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_6);
                if (soulProgress >= SOULS_TO_PURIFY) {
                    playersWhoCompletedSouls.add(player.getUniqueId());
                    playersIntroducedToSouls.add(player.getUniqueId());
                } else if (soulProgress > 0) {
                    playerSoulsPurified.put(player.getUniqueId(), soulProgress);
                    playersIntroducedToSouls.add(player.getUniqueId()); // Déjà introduit si progression > 0
                }

                // Recharger la progression brume toxique
                int toxicFogProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_7);
                if (toxicFogProgress >= CORRUPTION_SOURCE_COUNT) {
                    playersWhoCompletedToxicFog.add(player.getUniqueId());
                    playersIntroducedToToxicFog.add(player.getUniqueId());
                } else if (toxicFogProgress > 0) {
                    playerSourcesDestroyed.put(player.getUniqueId(), toxicFogProgress);
                    playersIntroducedToToxicFog.add(player.getUniqueId());

                    // Reconstruire les hits (marquer les premières sources comme détruites)
                    int[] hits = new int[CORRUPTION_SOURCE_COUNT];
                    for (int i = 0; i < toxicFogProgress && i < CORRUPTION_SOURCE_COUNT; i++) {
                        hits[i] = HITS_TO_DESTROY_SOURCE;
                    }
                    playerSourceHits.put(player.getUniqueId(), hits);
                }

                // Recharger la progression arbre maudit
                int creakingProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_8);
                if (creakingProgress >= ORB_COUNT + 1) { // 8 orbes + boss tué = 9
                    playersWhoCompletedCreakingQuest.add(player.getUniqueId());
                    playersIntroducedToCreaking.add(player.getUniqueId());
                } else if (creakingProgress > 0) {
                    playersIntroducedToCreaking.add(player.getUniqueId());
                    // Reconstruire les orbes collectées
                    Set<Integer> collectedOrbs = ConcurrentHashMap.newKeySet();
                    for (int i = 0; i < Math.min(creakingProgress, ORB_COUNT); i++) {
                        collectedOrbs.add(i);
                    }
                    playerCollectedOrbs.put(player.getUniqueId(), collectedOrbs);
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Nettoyer les données temporaires - Fossoyeur
        playerGraveHits.remove(uuid);
        playerGraveOrder.remove(uuid);
        playerGravesDug.remove(uuid);
        playersWithActiveBoss.remove(uuid);

        // Despawn le boss du joueur s'il existe
        UUID bossUuid = playerBossMap.remove(uuid);
        if (bossUuid != null) {
            Entity boss = plugin.getServer().getEntity(bossUuid);
            if (boss != null && boss.isValid()) {
                boss.remove();
            }
        }

        // Nettoyer les données temporaires - Champignons
        playerMushroomsCollected.remove(uuid);
        playerMushroomHits.remove(uuid);

        // Nettoyer les données temporaires - Âmes Damnées
        playerSoulsPurified.remove(uuid);
        playersIntroducedToSouls.remove(uuid);

        // Nettoyer les données temporaires - Brume Toxique
        playerSourcesDestroyed.remove(uuid);
        playerSourceHits.remove(uuid);
        playersIntroducedToToxicFog.remove(uuid);

        // Nettoyer les données temporaires - Arbre Maudit
        playerCollectedOrbs.remove(uuid);
        playersIntroducedToCreaking.remove(uuid);
        playersWithActiveCreakingBoss.remove(uuid);

        // Despawn le boss Creaking du joueur s'il existe
        UUID creakingBossUuid = playerCreakingBossMap.remove(uuid);
        if (creakingBossUuid != null) {
            Entity creakingBoss = plugin.getServer().getEntity(creakingBossUuid);
            if (creakingBoss != null && creakingBoss.isValid()) {
                creakingBoss.remove();
            }
        }
    }

    // ==================== ÉTAPE 6: PURIFICATION DES ÂMES ====================

    /**
     * Vérifie si une location est proche de la zone du cimetière
     */
    private boolean isNearSoulZone(Location loc) {
        return loc.getX() >= SOUL_ZONE_MIN_X - 50 &&
                loc.getX() <= SOUL_ZONE_MAX_X + 50 &&
                loc.getZ() >= SOUL_ZONE_MIN_Z - 50 &&
                loc.getZ() <= SOUL_ZONE_MAX_Z + 50;
    }

    /**
     * Démarre le spawner d'Âmes Damnées dans la zone du cimetière
     * Spawn prioritaire si un joueur est à l'étape 4_6
     */
    private void startDamnedSoulSpawner(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier si des joueurs sont proches ET à l'étape 4_6
                boolean playersNearby = false;
                for (Player player : world.getPlayers()) {
                    if (!isNearSoulZone(player.getLocation())) continue;

                    JourneyStep currentStep = journeyManager.getCurrentStep(player);
                    if (currentStep == JourneyStep.STEP_4_6 && !playersWhoCompletedSouls.contains(player.getUniqueId())) {
                        playersNearby = true;

                        // Introduction de la quête si première fois
                        if (!playersIntroducedToSouls.contains(player.getUniqueId())) {
                            playersIntroducedToSouls.add(player.getUniqueId());
                            introduceSoulQuest(player);
                        }
                        break;
                    }
                }

                if (!playersNearby) return;

                // Compter les âmes damnées existantes
                long soulCount = world.getEntitiesByClass(Zombie.class).stream()
                        .filter(z -> z.getPersistentDataContainer().has(DAMNED_SOUL_KEY, PersistentDataType.BYTE))
                        .count();

                // Limiter à MAX_DAMNED_SOULS
                if (soulCount >= MAX_DAMNED_SOULS) return;

                // Spawn 1-2 âmes
                int toSpawn = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 3);
                for (int i = 0; i < toSpawn && soulCount + i < MAX_DAMNED_SOULS; i++) {
                    spawnDamnedSoul(world);
                }
            }
        }.runTaskTimer(plugin, 200L, 60L); // Toutes les 3 secondes
    }

    /**
     * Spawn une Âme Damnée via ZombieManager
     */
    private void spawnDamnedSoul(World world) {
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        // Position aléatoire dans la zone
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        double x = random.nextDouble(SOUL_ZONE_MIN_X, SOUL_ZONE_MAX_X);
        double z = random.nextDouble(SOUL_ZONE_MIN_Z, SOUL_ZONE_MAX_Z);
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location spawnLoc = new Location(world, x, y, z);

        // Niveau aléatoire 5-10 pour la zone 7
        int level = random.nextInt(5, 11);

        // Spawn via ZombieManager
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.DAMNED_SOUL, spawnLoc, level);

        if (activeZombie != null) {
            Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
            if (entity instanceof Zombie zombie) {
                // Apparence spectrale - armure de cuir cyan avec effets
                zombie.getEquipment().setHelmet(createSoulArmor(Material.LEATHER_HELMET));
                zombie.getEquipment().setChestplate(createSoulArmor(Material.LEATHER_CHESTPLATE));
                zombie.getEquipment().setLeggings(createSoulArmor(Material.LEATHER_LEGGINGS));
                zombie.getEquipment().setBoots(createSoulArmor(Material.LEATHER_BOOTS));

                // Pas de drop d'armure
                zombie.getEquipment().setHelmetDropChance(0);
                zombie.getEquipment().setChestplateDropChance(0);
                zombie.getEquipment().setLeggingsDropChance(0);
                zombie.getEquipment().setBootsDropChance(0);

                // Effet visuel spectral
                zombie.setGlowing(true);
                zombie.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));

                // Marquer comme Âme Damnée
                zombie.getPersistentDataContainer().set(DAMNED_SOUL_KEY, PersistentDataType.BYTE, (byte) 1);

                // Particules de spawn
                world.spawnParticle(Particle.SOUL, spawnLoc.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
                world.playSound(spawnLoc, Sound.PARTICLE_SOUL_ESCAPE, 0.8f, 0.5f);
            }
        }
    }

    /**
     * Crée une pièce d'armure spectrale pour les Âmes Damnées
     */
    private ItemStack createSoulArmor(Material armorType) {
        ItemStack armor = new ItemStack(armorType);

        if (armor.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
            // Couleur cyan spectrale
            leatherMeta.setColor(Color.fromRGB(80, 180, 180));
            armor.setItemMeta(leatherMeta);
        }

        return armor;
    }

    /**
     * Crée l'item Purificateur d'Âmes
     */
    private ItemStack createSoulPurifier() {
        ItemStack purifier = new ItemStack(Material.HEART_OF_THE_SEA);
        var meta = purifier.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("✦ Purificateur d'Âmes ✦", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(Arrays.asList(
                    Component.text("Utilise cet artefact sur une", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Âme Damnée", NamedTextColor.DARK_AQUA)
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(" pour la libérer.", NamedTextColor.GRAY)),
                    Component.empty(),
                    Component.text("▸ Clic droit sur l'Âme Damnée", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(PURIFIER_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            purifier.setItemMeta(meta);
        }
        return purifier;
    }

    /**
     * Gère la mort d'une Âme Damnée - drop du Purificateur (33%)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamnedSoulDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombie.getPersistentDataContainer().has(DAMNED_SOUL_KEY, PersistentDataType.BYTE)) return;

        Player killer = zombie.getKiller();
        if (killer == null) return;

        // Vérifier si le joueur est à l'étape 4_6
        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep != JourneyStep.STEP_4_6) return;

        // 33% de chance de drop
        if (Math.random() < PURIFIER_DROP_CHANCE) {
            // Drop le purificateur
            zombie.getWorld().dropItemNaturally(zombie.getLocation(), createSoulPurifier());

            // Effets visuels et sonores
            killer.playSound(killer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            killer.sendTitle("§b✦ Purificateur obtenu!", "§7Utilise-le sur une Âme Damnée", 5, 40, 10);
        }

        // Particules de mort
        zombie.getWorld().spawnParticle(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.03);
    }

    /**
     * Gère l'utilisation du Purificateur sur une Âme Damnée
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPurifierUse(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Zombie zombie)) return;
        if (!zombie.getPersistentDataContainer().has(DAMNED_SOUL_KEY, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifier si c'est un purificateur
        if (item.getType() != Material.HEART_OF_THE_SEA) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(PURIFIER_ITEM_KEY, PersistentDataType.BYTE)) return;

        // Vérifier si le joueur est à l'étape 4_6
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep != JourneyStep.STEP_4_6) {
            player.sendMessage(Component.text("✗ Tu n'es pas à l'étape de purification des âmes.", NamedTextColor.RED));
            return;
        }

        // Vérifier si déjà complété
        if (playersWhoCompletedSouls.contains(player.getUniqueId())) return;

        event.setCancelled(true);

        // Consommer un purificateur
        item.setAmount(item.getAmount() - 1);

        // Incrémenter le compteur
        int purified = playerSoulsPurified.merge(player.getUniqueId(), 1, Integer::sum);

        // Transformer en villageois puis disparaître
        transformSoulToVillager(zombie, player);

        // Mettre à jour la progression Journey
        journeyManager.updateProgress(player, JourneyStep.StepType.SOUL_PURIFICATION, purified);

        // Vérifier si quête complète
        if (purified >= SOULS_TO_PURIFY) {
            playersWhoCompletedSouls.add(player.getUniqueId());

            // Message de fin
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendSoulPurificationLore(player);
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    /**
     * Transforme une Âme Damnée en villageois qui disparaît
     */
    private void transformSoulToVillager(Zombie zombie, Player player) {
        Location loc = zombie.getLocation();
        World world = zombie.getWorld();

        // Supprimer le zombie
        zombie.remove();

        // Spawn un villageois temporaire
        Villager villager = world.spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setProfession(org.bukkit.entity.Villager.Profession.CLERIC);
            v.customName(Component.text("Âme Libérée", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            v.setCustomNameVisible(true);
        });

        // Effets immédiats
        world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.05);
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.5f);

        // Message au joueur via Title
        int purified = playerSoulsPurified.getOrDefault(player.getUniqueId(), 0);
        player.sendTitle("§a✦ Âme purifiée!", "§7" + purified + "/" + SOULS_TO_PURIFY + " âmes libérées", 5, 30, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        // Disparition après 2 secondes avec nuage de fumée
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!villager.isValid()) {
                    cancel();
                    return;
                }

                ticks++;

                // Particules pendant 2 secondes
                if (ticks <= 40) {
                    world.spawnParticle(Particle.SOUL, villager.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
                }

                // Disparition à 2 secondes
                if (ticks >= 40) {
                    // Grand nuage de fumée
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, villager.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.02);
                    world.spawnParticle(Particle.SOUL, villager.getLocation().add(0, 1.5, 0), 20, 0.2, 0.4, 0.2, 0.03);
                    world.playSound(villager.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);

                    villager.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Envoie le lore de fin de purification
     */
    private void sendSoulPurificationLore(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA));
        player.sendMessage(Component.text("        ✦ LES ÂMES PARLENT ✦", NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("Les âmes libérées murmurent...", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, true));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("\"Nous étions comme eux... avant que", NamedTextColor.WHITE));
        player.sendMessage(Component.text("le Patient Zéro ne nous transforme.\"", NamedTextColor.WHITE));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("\"Il cherchait l'immortalité... mais", NamedTextColor.WHITE));
        player.sendMessage(Component.text("n'a trouvé qu'une malédiction éternelle.\"", NamedTextColor.WHITE));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("\"Les réponses se trouvent dans les", NamedTextColor.GOLD));
        player.sendMessage(Component.text("profondeurs... là où tout a commencé.\"", NamedTextColor.GOLD));
        player.sendMessage(Component.empty());

        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA));
        player.sendMessage(Component.text("  Quête de purification terminée!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("═══════════════════════════════════", NamedTextColor.DARK_AQUA));

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Introduit la quête de purification au joueur
     */
    private void introduceSoulQuest(Player player) {
        // Titre d'introduction
        player.sendTitle("§b✦ PURIFICATION DES ÂMES", "§7Libère 5 âmes damnées", 10, 60, 20);
        player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1.0f, 0.8f);

        // Message explicatif
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("");
                player.sendMessage("§b§l✦ PURIFICATION DES ÂMES ✦");
                player.sendMessage("§7Des âmes tourmentées errent dans ce cimetière.");
                player.sendMessage("");
                player.sendMessage("§e▸ §fTue les §bÂmes Damnées §fpour obtenir un §bPurificateur");
                player.sendMessage("§e▸ §fUtilise le Purificateur §7(clic droit) §fsur une âme");
                player.sendMessage("§e▸ §fLibère §a5 âmes §fpour terminer la quête");
                player.sendMessage("");

                // Activer le GPS
                activateGPSToSoulZone(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Active le GPS vers la zone des âmes damnées
     */
    private void activateGPSToSoulZone(Player player) {
        int centerX = (SOUL_ZONE_MIN_X + SOUL_ZONE_MAX_X) / 2;
        int centerZ = (SOUL_ZONE_MIN_Z + SOUL_ZONE_MAX_Z) / 2;

        player.sendMessage("§e§l➤ §7Zone du cimetière: §e" + centerX + ", " + SOUL_ZONE_MIN_Y + ", " + centerZ);
        player.sendMessage("");
    }

    // ==================== BRUME TOXIQUE (ÉTAPE 7) ====================

    /**
     * Spawn toutes les sources de corruption
     */
    private void spawnCorruptionSources(World world) {
        for (int i = 0; i < CORRUPTION_SOURCE_LOCATIONS.length; i++) {
            spawnCorruptionSource(world, i);
        }
    }

    /**
     * Spawn une source de corruption (ItemDisplay + Interaction + TextDisplay)
     */
    private void spawnCorruptionSource(World world, int index) {
        if (index >= CORRUPTION_SOURCE_LOCATIONS.length) return;

        Location loc = CORRUPTION_SOURCE_LOCATIONS[index].clone();
        loc.setWorld(world);

        // Supprimer les anciens
        if (corruptionSourceVisuals[index] != null && corruptionSourceVisuals[index].isValid()) {
            corruptionSourceVisuals[index].remove();
        }
        if (corruptionSourceHitboxes[index] != null && corruptionSourceHitboxes[index].isValid()) {
            corruptionSourceHitboxes[index].remove();
        }
        if (corruptionSourceDisplays[index] != null && corruptionSourceDisplays[index].isValid()) {
            corruptionSourceDisplays[index].remove();
        }

        // 1. Créer le VISUEL (ItemDisplay avec DRAGON_BREATH glowing vert toxique)
        corruptionSourceVisuals[index] = world.spawn(loc.clone().add(0, 1, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.DRAGON_BREATH));

            // Scale x2 pour visibilité
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(2.5f, 2.5f, 2.5f),
                    new AxisAngle4f(0, 0, 1, 0)
            ));

            display.setBillboard(Display.Billboard.CENTER);

            // Glow effect vert toxique
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(50, 200, 50));

            display.setViewRange(64f);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_corruption_source");
            display.addScoreboardTag("chapter4_corruption_visual");
            display.addScoreboardTag("corruption_visual_" + index);
        });

        // 2. Créer l'entité INTERACTION (hitbox invisible)
        corruptionSourceHitboxes[index] = world.spawn(loc.clone().add(0, 1, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(2.0f);
            interaction.setInteractionHeight(2.0f);
            interaction.setResponsive(true);

            interaction.addScoreboardTag("chapter4_corruption_source");
            interaction.addScoreboardTag("chapter4_corruption_hitbox");
            interaction.addScoreboardTag("corruption_hitbox_" + index);
            interaction.addScoreboardTag("zombiez_npc");

            interaction.getPersistentDataContainer().set(CORRUPTION_SOURCE_KEY, PersistentDataType.INTEGER, index);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });

        // 3. Créer le TextDisplay au-dessus
        corruptionSourceDisplays[index] = world.spawn(loc.clone().add(0, 3, 0), TextDisplay.class, display -> {
            boolean isLastSource = (index == 3);
            String title = isLastSource ? "§4§lSOURCE PRINCIPALE" : "§2§lSOURCE DE CORRUPTION";
            String subtitle = isLastSource ? "§c☠ Gardien: Marécageux Alpha" : "§a☠ Frappe pour détruire";

            display.text(Component.text()
                    .append(Component.text("☣ ", NamedTextColor.GREEN))
                    .append(Component.text(title.substring(2), isLastSource ? NamedTextColor.DARK_RED : NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                    .append(Component.text(" ☣", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("━━━━━━━━━", NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text(subtitle.substring(2), NamedTextColor.GRAY))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(150, 0, 50, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter4_corruption_source");
            display.addScoreboardTag("chapter4_corruption_display");
            display.addScoreboardTag("corruption_display_" + index);
        });

        // Particules d'ambiance autour de la source
        world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.01);
    }

    /**
     * Démarre le système de visibilité per-player pour les sources
     */
    private void startCorruptionSourceVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllSourcesForPlayer(player);
                        continue;
                    }

                    JourneyStep currentStep = journeyManager.getCurrentStep(player);
                    boolean shouldSeeSources = currentStep == JourneyStep.STEP_4_7 &&
                            !playersWhoCompletedToxicFog.contains(player.getUniqueId());

                    if (shouldSeeSources) {
                        updateSourceVisibilityForPlayer(player);

                        // Introduction si pas encore faite
                        if (!playersIntroducedToToxicFog.contains(player.getUniqueId())) {
                            introducePlayerToToxicFog(player);
                        }
                    } else {
                        hideAllSourcesForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des sources pour un joueur
     */
    private void updateSourceVisibilityForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Integer> destroyed = playerDestroyedSources.getOrDefault(uuid, Set.of());

        for (int i = 0; i < corruptionSourceVisuals.length; i++) {
            ItemDisplay visual = corruptionSourceVisuals[i];
            Interaction hitbox = corruptionSourceHitboxes[i];
            TextDisplay display = corruptionSourceDisplays[i];

            if (visual == null || !visual.isValid()) continue;

            // Si cette source est détruite par le joueur, la cacher
            if (destroyed.contains(i)) {
                if (visual != null) player.hideEntity(plugin, visual);
                if (hitbox != null && hitbox.isValid()) player.hideEntity(plugin, hitbox);
                if (display != null && display.isValid()) player.hideEntity(plugin, display);
            } else {
                // Sinon, la montrer
                if (visual != null) player.showEntity(plugin, visual);
                if (hitbox != null && hitbox.isValid()) player.showEntity(plugin, hitbox);
                if (display != null && display.isValid()) player.showEntity(plugin, display);
            }
        }
    }

    /**
     * Cache toutes les sources pour un joueur
     */
    private void hideAllSourcesForPlayer(Player player) {
        for (int i = 0; i < corruptionSourceVisuals.length; i++) {
            if (corruptionSourceVisuals[i] != null && corruptionSourceVisuals[i].isValid()) {
                player.hideEntity(plugin, corruptionSourceVisuals[i]);
            }
            if (corruptionSourceHitboxes[i] != null && corruptionSourceHitboxes[i].isValid()) {
                player.hideEntity(plugin, corruptionSourceHitboxes[i]);
            }
            if (corruptionSourceDisplays[i] != null && corruptionSourceDisplays[i].isValid()) {
                player.hideEntity(plugin, corruptionSourceDisplays[i]);
            }
        }
    }

    /**
     * Démarre le vérificateur de respawn des sources
     */
    private void startCorruptionSourceRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < CORRUPTION_SOURCE_LOCATIONS.length; i++) {
                    boolean needsRespawn = corruptionSourceVisuals[i] == null ||
                            !corruptionSourceVisuals[i].isValid() ||
                            corruptionSourceHitboxes[i] == null ||
                            !corruptionSourceHitboxes[i].isValid();

                    if (needsRespawn) {
                        spawnCorruptionSource(world, i);
                        plugin.log(Level.FINE, "Source de corruption " + i + " respawnée");
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Démarre le système de poison dans le marais
     */
    private void startSwampPoisonChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    JourneyStep currentStep = journeyManager.getCurrentStep(player);
                    if (currentStep != JourneyStep.STEP_4_7) continue;
                    if (playersWhoCompletedToxicFog.contains(player.getUniqueId())) continue;

                    // Vérifier si dans la zone
                    if (isInSwampZone(player.getLocation())) {
                        playersInSwampZone.add(player.getUniqueId());

                        // Compter les sources non détruites
                        Set<Integer> destroyed = playerDestroyedSources.getOrDefault(player.getUniqueId(), Set.of());
                        int remainingSources = 4 - destroyed.size();

                        if (remainingSources > 0) {
                            // Appliquer poison proportionnel aux sources restantes
                            double damage = POISON_DAMAGE * (remainingSources / 4.0);
                            player.damage(damage);
                            player.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.01);

                            // Message occasionnel
                            if (Math.random() < 0.1) {
                                player.sendMessage("§2§o*La brume toxique te brûle les poumons...*");
                            }
                        }
                    } else {
                        playersInSwampZone.remove(player.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, POISON_TICK_INTERVAL);
    }

    /**
     * Vérifie si une location est dans la zone du marais
     */
    private boolean isInSwampZone(Location loc) {
        return loc.getX() >= SWAMP_ZONE_MIN_X && loc.getX() <= SWAMP_ZONE_MAX_X &&
               loc.getY() >= SWAMP_ZONE_MIN_Y && loc.getY() <= SWAMP_ZONE_MAX_Y &&
               loc.getZ() >= SWAMP_ZONE_MIN_Z && loc.getZ() <= SWAMP_ZONE_MAX_Z;
    }

    /**
     * Gère le hit sur une source de corruption
     */
    private void handleCorruptionSourceHit(Player player, int sourceIndex) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_4_7) {
            player.sendMessage("§7Une étrange source de corruption... Elle semble liée à la brume.");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.5f);
            return;
        }

        // Vérifier si la quête est finie
        if (playersWhoCompletedToxicFog.contains(player.getUniqueId())) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Vérifier si cette source est déjà détruite par ce joueur
        Set<Integer> destroyed = playerDestroyedSources.get(uuid);
        if (destroyed != null && destroyed.contains(sourceIndex)) {
            return;
        }

        // Initialiser les hits si nécessaire
        int[] hits = playerSourceHits.get(uuid);
        if (hits == null) {
            hits = new int[4];
            playerSourceHits.put(uuid, hits);
        }

        // Incrémenter les hits
        hits[sourceIndex]++;
        int currentHits = hits[sourceIndex];

        // Effets de hit
        ItemDisplay visual = corruptionSourceVisuals[sourceIndex];
        if (visual != null && visual.isValid()) {
            Location loc = visual.getLocation();
            player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 0.5f + (float) currentHits / HITS_TO_DESTROY_SOURCE);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 10, 0.5, 0.5, 0.5, 0.02);
        }

        // Progression visuelle
        float progress = (float) currentHits / HITS_TO_DESTROY_SOURCE;
        player.sendMessage("§2☣ §7Source endommagée: §e" + (int)(progress * 100) + "%");

        // Source détruite!
        if (currentHits >= HITS_TO_DESTROY_SOURCE) {
            onSourceDestroyed(player, sourceIndex);
        }
    }

    /**
     * Appelé quand une source est détruite par un joueur
     */
    private void onSourceDestroyed(Player player, int sourceIndex) {
        UUID uuid = player.getUniqueId();

        // Marquer comme détruite
        Set<Integer> destroyed = playerDestroyedSources.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        destroyed.add(sourceIndex);

        // Mettre à jour la progression Journey
        journeyManager.updateProgress(player, JourneyStep.StepType.TOXIC_FOG_QUEST, destroyed.size());

        // Effets visuels
        ItemDisplay visual = corruptionSourceVisuals[sourceIndex];
        if (visual != null && visual.isValid()) {
            Location loc = visual.getLocation();
            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 50, 1, 1, 1, 0.1);
            player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
            player.playSound(loc, Sound.ENTITY_ALLAY_DEATH, 1.0f, 0.5f);
        }

        // Cacher la source pour ce joueur
        updateSourceVisibilityForPlayer(player);

        boolean isLastSource = (sourceIndex == 3);
        int remainingSources = 4 - destroyed.size();

        // Message et effets
        if (remainingSources > 0) {
            player.sendTitle("§a✓ SOURCE DÉTRUITE!", "§7" + destroyed.size() + "/4 - " + remainingSources + " restante(s)", 10, 40, 10);
            player.sendMessage("");
            player.sendMessage("§a§l✦ §7Source de corruption détruite! §e" + destroyed.size() + "/4");

            // GPS vers la prochaine source non détruite
            for (int i = 0; i < CORRUPTION_SOURCE_LOCATIONS.length; i++) {
                if (!destroyed.contains(i)) {
                    Location nextLoc = CORRUPTION_SOURCE_LOCATIONS[i];
                    String warning = (i == 3) ? " §c(Gardien!)" : "";
                    player.sendMessage("§e§l➤ §7Prochaine source: §e" + (int)nextLoc.getX() + ", " +
                            (int)nextLoc.getY() + ", " + (int)nextLoc.getZ() + warning);
                    break;
                }
            }
            player.sendMessage("");
        } else {
            // Quête complétée!
            onToxicFogQuestComplete(player);
        }

        // Spawn mini-boss si c'est la dernière source (index 3)
        if (isLastSource && remainingSources == 0) {
            // Le boss est géré dans onToxicFogQuestComplete
        }
    }

    /**
     * Appelé quand la quête de la brume toxique est complétée
     */
    private void onToxicFogQuestComplete(Player player) {
        playersWhoCompletedToxicFog.add(player.getUniqueId());

        // Effets de victoire
        player.sendTitle("§a§l✦ BRUME DISSIPÉE! ✦", "§7Le marais est purifié!", 10, 60, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Particules de purification autour du joueur
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);

        // Message de récompense
        player.sendMessage("");
        player.sendMessage("§8§m                                            ");
        player.sendMessage("");
        player.sendMessage("  §a§l✦ BRUME TOXIQUE DISSIPÉE! ✦");
        player.sendMessage("");
        player.sendMessage("  §7Tu as détruit les §e4 sources de corruption§7!");
        player.sendMessage("  §7Le marais peut enfin respirer...");
        player.sendMessage("");
        player.sendMessage("  §6Récompenses:");
        player.sendMessage("  §7▸ §e+800 Points");
        player.sendMessage("  §7▸ §a+22 XP");
        player.sendMessage("");
        player.sendMessage("§8§m                                            ");
        player.sendMessage("");

        // Cacher toutes les sources
        hideAllSourcesForPlayer(player);
    }

    /**
     * Introduction à la quête de la brume toxique
     */
    private void introducePlayerToToxicFog(Player player) {
        playersIntroducedToToxicFog.add(player.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle("§2§l☣ LA BRUME TOXIQUE ☣", "§7Une corruption empoisonne le marais...", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.AMBIENT_BASALT_DELTAS_MOOD, 1.0f, 0.5f);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage("");
                        player.sendMessage("§8§m                                            ");
                        player.sendMessage("");
                        player.sendMessage("  §2§l☣ LA BRUME TOXIQUE");
                        player.sendMessage("");
                        player.sendMessage("  §7Une brume empoisonnée a envahi le marais.");
                        player.sendMessage("  §7Quatre §asources de corruption §7en sont");
                        player.sendMessage("  §7la cause. Tu dois les §edétruire§7!");
                        player.sendMessage("");
                        player.sendMessage("  §c⚠ §7La brume te fait des dégâts tant que");
                        player.sendMessage("  §7les sources existent!");
                        player.sendMessage("");
                        player.sendMessage("  §e▸ Frappe les sources pour les détruire");
                        player.sendMessage("  §e▸ Attention à la dernière source (gardien)!");
                        player.sendMessage("");
                        player.sendMessage("§8§m                                            ");
                        player.sendMessage("");

                        // GPS vers la première source
                        Location firstLoc = CORRUPTION_SOURCE_LOCATIONS[0];
                        player.sendMessage("§e§l➤ §7Première source: §e" + (int)firstLoc.getX() + ", " +
                                (int)firstLoc.getY() + ", " + (int)firstLoc.getZ());
                        player.sendMessage("");
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 20L);
    }

    /**
     * Vérifie si un joueur a complété la quête brume toxique
     */
    private boolean hasPlayerCompletedToxicFogQuest(Player player) {
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_7);
        return progress >= 4;
    }

    // ==================== ÉTAPE 8: L'ARBRE MAUDIT - CREAKING BOSS ====================

    /**
     * Spawn les orbes autour de l'arbre maudit
     */
    private void spawnOrbs(World world) {
        for (int i = 0; i < ORB_COUNT; i++) {
            spawnOrb(world, i);
        }
        plugin.log(Level.INFO, "§a  - " + ORB_COUNT + " orbes spawnées autour de l'arbre");
    }

    /**
     * Spawn une orbe individuelle
     */
    private void spawnOrb(World world, int index) {
        Location loc = ORB_LOCATIONS[index].clone();
        loc.setWorld(world);

        // Créer l'ItemDisplay (orbe visuelle - END_CRYSTAL avec glow)
        ItemDisplay display = world.spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.END_CRYSTAL));
            d.setGlowing(true);
            d.setBillboard(Display.Billboard.CENTER);

            // Scale x0.8 pour une taille appropriée
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0.3f, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new AxisAngle4f(0, 0, 1, 0)
            );
            d.setTransformation(transformation);

            d.addScoreboardTag("chapter4_orb");
            d.addScoreboardTag("chapter4_orb_" + index);
            d.setVisibleByDefault(false);
        });

        // Créer l'Interaction (hitbox invisible)
        Interaction interaction = world.spawn(loc, Interaction.class, i -> {
            i.setInteractionWidth(1.2f);
            i.setInteractionHeight(1.2f);
            i.addScoreboardTag("chapter4_orb");
            i.addScoreboardTag("chapter4_orb_hitbox_" + index);

            // PDC pour identifier l'orbe
            i.getPersistentDataContainer().set(ORB_HITBOX_KEY, PersistentDataType.INTEGER, index);
        });

        // Appliquer le glow cyan via team
        applyOrbGlow(display);

        orbVisuals[index] = display;
        orbHitboxes[index] = interaction;
    }

    /**
     * Applique un glow cyan aux orbes
     */
    private void applyOrbGlow(Entity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("orb_glow_cyan");
        if (team == null) {
            team = scoreboard.registerNewTeam("orb_glow_cyan");
            team.color(NamedTextColor.AQUA);
        }
        team.addEntry(entity.getUniqueId().toString());
    }

    /**
     * Démarre le système de mise à jour de visibilité des orbes
     */
    private void startOrbVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (Player player : world.getPlayers()) {
                    updateOrbVisibilityForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Met à jour la visibilité des orbes pour un joueur
     */
    private void updateOrbVisibilityForPlayer(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        boolean isAtStep = currentStep == JourneyStep.STEP_4_8;
        boolean hasCompleted = playersWhoCompletedCreakingQuest.contains(player.getUniqueId());

        if (!isAtStep || hasCompleted) {
            hideAllOrbsForPlayer(player);
            return;
        }

        Location playerLoc = player.getLocation();
        Location treeLoc = ORB_LOCATIONS[0].clone();
        treeLoc.setWorld(player.getWorld());

        // Vérifier si le joueur est proche de l'arbre
        double distance = playerLoc.distance(treeLoc);
        if (distance > ORB_VIEW_DISTANCE) {
            hideAllOrbsForPlayer(player);
            return;
        }

        // Introduction si pas encore faite
        if (!playersIntroducedToCreaking.contains(player.getUniqueId())) {
            introducePlayerToCreaking(player);
        }

        // Obtenir les orbes collectées par ce joueur
        Set<Integer> collected = playerCollectedOrbs.getOrDefault(player.getUniqueId(), ConcurrentHashMap.newKeySet());

        // Montrer les orbes non collectées
        for (int i = 0; i < ORB_COUNT; i++) {
            if (collected.contains(i)) {
                // Orbe déjà collectée - masquer
                if (orbVisuals[i] != null && orbVisuals[i].isValid()) {
                    player.hideEntity(plugin, orbVisuals[i]);
                }
                if (orbHitboxes[i] != null && orbHitboxes[i].isValid()) {
                    player.hideEntity(plugin, orbHitboxes[i]);
                }
            } else {
                // Orbe disponible - montrer
                if (orbVisuals[i] != null && orbVisuals[i].isValid()) {
                    player.showEntity(plugin, orbVisuals[i]);
                }
                if (orbHitboxes[i] != null && orbHitboxes[i].isValid()) {
                    player.showEntity(plugin, orbHitboxes[i]);
                }
            }
        }
    }

    /**
     * Masque toutes les orbes pour un joueur
     */
    private void hideAllOrbsForPlayer(Player player) {
        for (int i = 0; i < ORB_COUNT; i++) {
            if (orbVisuals[i] != null && orbVisuals[i].isValid()) {
                player.hideEntity(plugin, orbVisuals[i]);
            }
            if (orbHitboxes[i] != null && orbHitboxes[i].isValid()) {
                player.hideEntity(plugin, orbHitboxes[i]);
            }
        }
    }

    /**
     * Vérifie et respawn les orbes si nécessaire
     */
    private void startOrbRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                for (int i = 0; i < ORB_COUNT; i++) {
                    if (orbVisuals[i] == null || !orbVisuals[i].isValid() ||
                        orbHitboxes[i] == null || !orbHitboxes[i].isValid()) {

                        // Nettoyer
                        if (orbVisuals[i] != null && orbVisuals[i].isValid()) {
                            orbVisuals[i].remove();
                        }
                        if (orbHitboxes[i] != null && orbHitboxes[i].isValid()) {
                            orbHitboxes[i].remove();
                        }

                        // Respawn
                        spawnOrb(world, i);
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    /**
     * Gère la collecte d'une orbe par un joueur
     */
    private void handleOrbCollected(Player player, int orbIndex) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep != JourneyStep.STEP_4_8) return;

        if (playersWhoCompletedCreakingQuest.contains(player.getUniqueId())) return;

        // Vérifier si l'orbe n'est pas déjà collectée
        Set<Integer> collected = playerCollectedOrbs.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (collected.contains(orbIndex)) return;

        // Marquer comme collectée
        collected.add(orbIndex);
        int totalCollected = collected.size();

        // Effets visuels et sonores
        Location orbLoc = ORB_LOCATIONS[orbIndex].clone();
        orbLoc.setWorld(player.getWorld());

        player.playSound(orbLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1.2f);
        player.playSound(orbLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2f);
        orbLoc.getWorld().spawnParticle(Particle.END_ROD, orbLoc.add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
        orbLoc.getWorld().spawnParticle(Particle.ENCHANT, orbLoc, 20, 0.3, 0.3, 0.3, 0.5);

        // Mettre à jour la progression
        journeyManager.setStepProgress(player, JourneyStep.STEP_4_8, totalCollected);

        // Feedback au joueur
        player.sendTitle("§b✦ ORBE " + totalCollected + "/" + ORB_COUNT + " ✦", "§7Une énergie ancienne vous traverse...", 5, 30, 10);

        // Vérifier si toutes les orbes sont collectées
        if (totalCollected >= ORB_COUNT) {
            onAllOrbsCollected(player);
        }
    }

    /**
     * Appelé quand le joueur a collecté toutes les orbes
     */
    private void onAllOrbsCollected(Player player) {
        if (playersWithActiveCreakingBoss.contains(player.getUniqueId())) return;

        playersWithActiveCreakingBoss.add(player.getUniqueId());

        // Effets dramatiques
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.7f);

        player.sendTitle("§4§l⚠ L'ARBRE S'ÉVEILLE ⚠", "§cLe Gardien de l'Arbre Maudit surgit!", 10, 60, 20);

        // Spawn du boss après un délai dramatique
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    playersWithActiveCreakingBoss.remove(player.getUniqueId());
                    return;
                }

                spawnCreakingBoss(player);
            }
        }.runTaskLater(plugin, 60L);
    }

    /**
     * Spawn le boss Creaking pour un joueur
     */
    private void spawnCreakingBoss(Player player) {
        World world = player.getWorld();
        Location spawnLoc = CREAKING_BOSS_SPAWN.clone();
        spawnLoc.setWorld(world);

        // Calculer la direction vers le joueur
        Location playerLoc = player.getLocation();
        float yaw = (float) Math.toDegrees(Math.atan2(
                playerLoc.getZ() - spawnLoc.getZ(),
                playerLoc.getX() - spawnLoc.getX()
        )) - 90;

        spawnLoc.setYaw(yaw);

        // Utiliser ZombieManager pour spawner le boss
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            playersWithActiveCreakingBoss.remove(player.getUniqueId());
            return;
        }

        // Spawn le boss via le système ZombieZ
        int bossLevel = Math.max(15, journeyManager.getPlayerLevel(player));
        var activeZombie = zombieManager.spawnZombie(ZombieType.CREAKING_BOSS, spawnLoc, bossLevel);

        if (activeZombie != null) {
            Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
            if (entity instanceof Zombie creakingBoss) {
                // Configuration additionnelle du boss
                creakingBoss.addScoreboardTag("chapter4_creaking_boss");
                creakingBoss.addScoreboardTag("journey_boss");
                creakingBoss.addScoreboardTag("player_boss_" + player.getUniqueId());

                // Scale x2.5 pour le rendre géant
                var scale = creakingBoss.getAttribute(Attribute.SCALE);
                if (scale != null) {
                    scale.setBaseValue(2.5);
                }

                // Glow rouge
                applyCreakingGlow(creakingBoss);

                // Tracker le boss
                playerCreakingBossMap.put(player.getUniqueId(), creakingBoss.getUniqueId());

                // Effets de spawn
                world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 2f, 0.5f);
                world.playSound(spawnLoc, Sound.BLOCK_WOOD_BREAK, 2f, 0.3f);
                world.spawnParticle(Particle.BLOCK, spawnLoc.clone().add(0, 1.5, 0), 100, 1.5, 2, 1.5,
                        Material.PALE_OAK_LOG.createBlockData());
                world.spawnParticle(Particle.SOUL, spawnLoc.clone().add(0, 1, 0), 50, 1, 1.5, 1, 0.02);

                // Message au joueur
                player.sendMessage("§c§l⚔ §4Le Gardien de l'Arbre Maudit §c§lémerge des racines!");
                player.sendMessage("§7Vaincs-le pour purifier l'arbre corrompu!");
            }
        } else {
            playersWithActiveCreakingBoss.remove(player.getUniqueId());
            plugin.log(Level.WARNING, "§cImpossible de spawner le boss Creaking pour " + player.getName());
        }
    }

    /**
     * Applique un glow rouge au boss Creaking
     */
    private void applyCreakingGlow(Entity entity) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("creaking_boss_glow");
        if (team == null) {
            team = scoreboard.registerNewTeam("creaking_boss_glow");
            team.color(NamedTextColor.DARK_RED);
        }
        team.addEntry(entity.getUniqueId().toString());
        entity.setGlowing(true);
    }

    /**
     * Gère la mort du boss Creaking
     */
    private void handleCreakingBossKilled(Player killer, Zombie creakingBoss) {
        // Trouver le joueur propriétaire du boss
        UUID ownerUuid = null;
        for (String tag : creakingBoss.getScoreboardTags()) {
            if (tag.startsWith("player_boss_")) {
                String uuidStr = tag.substring("player_boss_".length());
                try {
                    ownerUuid = UUID.fromString(uuidStr);
                    break;
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Si le killer est le propriétaire ou qu'on a trouvé le propriétaire
        Player owner = ownerUuid != null ? plugin.getServer().getPlayer(ownerUuid) : killer;
        if (owner == null) owner = killer;
        if (owner == null) return;

        UUID ownerId = owner.getUniqueId();

        // Nettoyer les trackers
        playerCreakingBossMap.remove(ownerId);
        playersWithActiveCreakingBoss.remove(ownerId);

        // Marquer comme complété
        playersWhoCompletedCreakingQuest.add(ownerId);

        // Finaliser la quête (8 orbes + 1 boss = 9)
        journeyManager.setStepProgress(owner, JourneyStep.STEP_4_8, ORB_COUNT + 1);

        // Effets de victoire
        Location deathLoc = creakingBoss.getLocation();
        deathLoc.getWorld().playSound(deathLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);

        deathLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLoc.add(0, 1.5, 0), 100, 2, 2, 2, 0.3);
        deathLoc.getWorld().spawnParticle(Particle.SOUL, deathLoc, 50, 2, 2, 2, 0.05);

        // Message de victoire
        owner.sendTitle("§a§l✓ VICTOIRE!", "§7L'Arbre Maudit est purifié!", 10, 60, 20);
        owner.sendMessage("");
        owner.sendMessage("§a§l⭐ §eL'Arbre Maudit retrouve sa lumière!");
        owner.sendMessage("§7Le Gardien corrompu a été vaincu.");
        owner.sendMessage("");

        // Compléter l'étape via JourneyManager
        journeyManager.onStepProgress(owner, JourneyStep.STEP_4_8, ORB_COUNT + 1);
    }

    /**
     * Introduction à la quête de l'Arbre Maudit
     */
    private void introducePlayerToCreaking(Player player) {
        playersIntroducedToCreaking.add(player.getUniqueId());

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                switch (step) {
                    case 0 -> {
                        player.sendTitle("§4§lL'ARBRE MAUDIT", "§7Une énergie sombre émane de cet arbre...", 10, 50, 10);
                        player.playSound(player.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1f, 0.5f);
                    }
                    case 1 -> {
                        player.sendMessage("");
                        player.sendMessage("§e§l➤ §fDes orbes d'énergie flottent autour de l'arbre.");
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                    }
                    case 2 -> {
                        player.sendMessage("§e§l➤ §fCollecte les §b8 orbes§f pour réveiller le gardien.");
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
                    }
                    case 3 -> {
                        player.sendMessage("§e§l➤ §7GPS: §e453, 91, 8515 §7(Centre de l'arbre)");
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                        cancel();
                        return;
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    /**
     * Vérifie si un joueur a complété la quête de l'arbre maudit
     */
    private boolean hasPlayerCompletedCreakingQuest(Player player) {
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_4_8);
        return progress >= ORB_COUNT + 1;
    }
}
