package com.rinaorc.zombiez.progression.journey.chapter3;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyNPCManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Gère les systèmes spécifiques au Chapitre 3:
 * - NPC Forain Marcel (étape 4) - Puzzle Memory Game
 * - Chat perdu (étape 5) - Visibilité per-player
 * - Investigation Patient Zéro (étape 6) - Indices cachés
 * - Défense du village (étape 7) - Protection d'un survivant
 * - Réparation du Zeppelin (étape 8) - Puzzle de connexion de fils
 * - Boss Seigneur des Profondeurs (étape 10) - Boss de fin de chapitre
 */
public class Chapter3Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;
    private final JourneyNPCManager npcManager;

    // === CLÉS PDC ===
    private final NamespacedKey FORAIN_NPC_KEY;
    private final NamespacedKey LOST_CAT_KEY;
    private final NamespacedKey INVESTIGATION_CLUE_KEY;
    private final NamespacedKey VILLAGE_SURVIVOR_KEY;
    private final NamespacedKey DEFENSE_ZOMBIE_KEY;
    private final NamespacedKey ZEPPELIN_CONTROL_KEY;
    private final NamespacedKey MINE_BOSS_KEY;

    // === POSITIONS ===
    // NPC Forain au cirque
    private static final Location FORAIN_LOCATION = new Location(null, 322.5, 93, 9201.5, 0, 0);
    // Chat perdu
    private static final Location CAT_LOCATION = new Location(null, 1025.5, 120, 9136.5, 0, 0);
    // Maison du Patient Zéro (centre)
    private static final Location PATIENT_ZERO_HOUSE = new Location(null, 875, 88, 8944, 0, 0);

    // === POSITIONS DES INDICES (autour de la maison) ===
    private static final Location[] CLUE_LOCATIONS = {
            new Location(null, 876.5, 88, 8936.5, 0, 0), // Indice 1: Journal
            new Location(null, 878.5, 87, 8947.5, 0, 0), // Indice 2: Fiole
            new Location(null, 872.5, 94, 8943.5, 0, 0), // Indice 3: Photo
            new Location(null, 876.5, 92, 8938.5, 0, 0) // Indice 4: Lettre
    };

    // Village à défendre (NPC survivant)
    private static final Location VILLAGE_SURVIVOR_LOCATION = new Location(null, 527.5, 90, 8994.5, -90, 0);

    // Points de spawn des zombies autour du village (8 points en cercle)
    private static final Location[] ZOMBIE_SPAWN_POINTS = {
            new Location(null, 547, 90, 8994, 0, 0), // Est
            new Location(null, 507, 90, 8994, 0, 0), // Ouest
            new Location(null, 527, 90, 8974, 0, 0), // Sud
            new Location(null, 527, 90, 9014, 0, 0), // Nord
            new Location(null, 542, 90, 8979, 0, 0), // Sud-Est
            new Location(null, 512, 90, 8979, 0, 0), // Sud-Ouest
            new Location(null, 542, 90, 9009, 0, 0), // Nord-Est
            new Location(null, 512, 90, 9009, 0, 0) // Nord-Ouest
    };

    // Panneau de contrôle du Zeppelin (étape 8)
    private static final Location ZEPPELIN_CONTROL_LOCATION = new Location(null, 345.5, 148, 8907.5, 0, 0);

    // Boss de la mine (étape 10)
    private static final Location MINE_BOSS_LOCATION = new Location(null, 1063, 76, 9127, 0, 0);
    private static final String MINE_BOSS_NAME = "Seigneur des Profondeurs";
    private static final double BOSS_LEASH_RANGE = 35.0;
    private static final double BOSS_LEASH_RANGE_SQUARED = BOSS_LEASH_RANGE * BOSS_LEASH_RANGE;
    private static final int BOSS_RESPAWN_SECONDS = 60;
    private static final double BOSS_DISPLAY_HEIGHT = 5.0;

    // Configuration de la défense
    private static final int DEFENSE_DURATION_SECONDS = 90;
    private static final int ZOMBIE_SPAWN_INTERVAL_TICKS = 60; // 3 secondes
    private static final int ZOMBIES_PER_WAVE = 3;
    private static final double DEFENSE_RADIUS = 25.0;
    private static final double SURVIVOR_MAX_DAMAGE = 100.0; // PV du survivant

    // === NPC CONFIG ===
    private static final String FORAIN_NPC_ID = "chapter3_forain";
    private static final String FORAIN_NAME = "Marcel le Magnifique";
    private static final double FORAIN_DISPLAY_HEIGHT = 2.5;
    private static final double CAT_DISPLAY_HEIGHT = 1.2;
    private static final double CAT_VIEW_DISTANCE = 64;
    private static final double CLUE_VIEW_DISTANCE = 32;

    // === TRACKING ===
    private Entity forainEntity;
    private TextDisplay forainDisplay;

    // Chat perdu (per-player visibility)
    private Entity lostCatEntity;
    private TextDisplay lostCatDisplay;

    // Indices du Patient Zéro (per-player visibility) - ItemDisplay + Interaction
    // comme supply crates
    private final Interaction[] clueHitboxes = new Interaction[4];
    private final ItemDisplay[] clueVisuals = new ItemDisplay[4];

    // Survivant du village (étape 7)
    private Entity villageSurvivorEntity;
    private TextDisplay villageSurvivorDisplay;

    // Panneau de contrôle du Zeppelin (étape 8) - ItemDisplay + Interaction
    private ItemDisplay zeppelinControlVisual;
    private Interaction zeppelinControlHitbox;
    private TextDisplay zeppelinControlDisplay;

    // Boss de la mine (étape 10)
    private Entity mineBossEntity;
    private TextDisplay bossSpawnDisplay;
    private final Set<UUID> bossContributors = ConcurrentHashMap.newKeySet();
    private boolean bossRespawnScheduled = false;
    private long bossRespawnTime = 0;

    // Joueurs ayant complété le puzzle (évite de refaire)
    private final Set<UUID> playersWhoCompletedPuzzle = ConcurrentHashMap.newKeySet();
    // Joueurs ayant sauvé le chat
    private final Set<UUID> playersWhoRescuedCat = ConcurrentHashMap.newKeySet();
    // Indices trouvés par joueur (bitmask: bit 0 = indice 1, bit 1 = indice 2,
    // etc.)
    private final Map<UUID, Integer> playerCluesFound = new ConcurrentHashMap<>();
    // Joueurs ayant défendu le village avec succès
    private final Set<UUID> playersWhoDefendedVillage = ConcurrentHashMap.newKeySet();
    // Joueurs ayant réparé le Zeppelin
    private final Set<UUID> playersWhoRepairedZeppelin = ConcurrentHashMap.newKeySet();
    // Événements de défense actifs par joueur
    private final Map<UUID, VillageDefenseEvent> activeDefenseEvents = new ConcurrentHashMap<>();
    // Cooldown de défense après échec (timestamp de fin du cooldown)
    private final Map<UUID, Long> defenseCooldowns = new ConcurrentHashMap<>();
    private static final int DEFENSE_COOLDOWN_SECONDS = 10; // Cooldown après mort d'Henri

    // Listener du Memory Game
    private final MemoryGameGUI.MemoryGameListener memoryGameListener;
    // Listener du Wire Puzzle (Zeppelin)
    private final WirePuzzleGUI.WirePuzzleListener wirePuzzleListener;

    public Chapter3Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();
        this.npcManager = plugin.getJourneyNPCManager();

        // Initialiser les clés PDC
        this.FORAIN_NPC_KEY = new NamespacedKey(plugin, "forain_npc");
        this.LOST_CAT_KEY = new NamespacedKey(plugin, "lost_cat");
        this.INVESTIGATION_CLUE_KEY = new NamespacedKey(plugin, "investigation_clue");
        this.VILLAGE_SURVIVOR_KEY = new NamespacedKey(plugin, "village_survivor");
        this.DEFENSE_ZOMBIE_KEY = new NamespacedKey(plugin, "defense_zombie");
        this.ZEPPELIN_CONTROL_KEY = new NamespacedKey(plugin, "zeppelin_control");
        this.MINE_BOSS_KEY = new NamespacedKey(plugin, "mine_boss");

        // Créer et enregistrer le listener du jeu de mémoire
        this.memoryGameListener = new MemoryGameGUI.MemoryGameListener(plugin);
        Bukkit.getPluginManager().registerEvents(memoryGameListener, plugin);

        // Créer et enregistrer le listener du puzzle de fils (Zeppelin)
        this.wirePuzzleListener = new WirePuzzleGUI.WirePuzzleListener(plugin);
        Bukkit.getPluginManager().registerEvents(wirePuzzleListener, plugin);

        // Enregistrer le listener principal
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Démarrer l'initialisation avec délai pour attendre le chargement du monde
        new BukkitRunnable() {
            @Override
            public void run() {
                initialize();
            }
        }.runTaskLater(plugin, 100L);
    }

    /**
     * Initialise les systèmes du Chapitre 3
     */
    public void initialize() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.log(Level.WARNING, "§cImpossible d'initialiser Chapter3Systems: monde 'world' non trouvé");
            return;
        }

        // Nettoyer les anciens NPCs
        cleanupOldEntities(world);

        // Spawn le Forain et démarrer le respawn checker
        spawnForain(world);
        startForainRespawnChecker();

        // Spawn le chat perdu
        spawnLostCat(world);

        // Spawn les indices du Patient Zéro
        spawnInvestigationClues(world);

        // Spawn le survivant du village
        spawnVillageSurvivor(world);

        // Spawn le panneau de contrôle du Zeppelin
        spawnZeppelinControl(world);

        // Initialiser le boss de la mine
        initializeBossDisplay(world);
        spawnMineBoss(world);
        startBossDisplayUpdater();

        // Démarrer les systèmes de visibilité per-player
        startCatVisibilityUpdater();
        startClueVisibilityUpdater();
        startSurvivorVisibilityUpdater();
        startZeppelinControlVisibilityUpdater();

        plugin.log(Level.INFO,
                "§a✓ Chapter3Systems initialisé (Forain, Chat, Investigation, Défense Village, Zeppelin, Boss Mine)");
    }

    /**
     * Récupère ou nettoie les entités du chapitre 3 dans le MONDE ENTIER.
     * RÉUTILISE les entités persistantes existantes au lieu de les supprimer.
     * Garantit MAXMOBS=1 (une seule instance de chaque NPC).
     */
    private void cleanupOldEntities(World world) {
        int removed = 0;
        boolean foundForain = false;
        boolean foundForainDisplay = false;
        boolean foundCat = false;
        boolean foundSurvivor = false;

        // RECHERCHE GLOBALE dans tout le monde pour garantir maxmobs=1
        for (Entity entity : world.getEntities()) {
            Set<String> tags = entity.getScoreboardTags();

            // === FORAIN: Réutiliser le premier VILLAGER valide, supprimer les doublons ===
            if (tags.contains("chapter3_forain") ||
                    (entity instanceof Villager v && v.getPersistentDataContainer().has(FORAIN_NPC_KEY, PersistentDataType.BYTE))) {
                // FIX: Vérifier que c'est bien un Villager avant d'assigner
                if (entity instanceof Villager villager) {
                    if (!foundForain) {
                        forainEntity = villager;
                        foundForain = true;
                    } else {
                        entity.remove();
                        removed++;
                    }
                } else {
                    // Entité avec le tag mais pas un Villager = corruption, supprimer
                    entity.remove();
                    removed++;
                }
                continue;
            }

            // Forain display: réutiliser le premier
            if (tags.contains("chapter3_forain_display")) {
                if (!foundForainDisplay && entity instanceof TextDisplay td) {
                    forainDisplay = td;
                    foundForainDisplay = true;
                } else {
                    entity.remove();
                    removed++;
                }
                continue;
            }

            // === CHAT PERDU: Réutiliser le premier ===
            if (tags.contains("chapter3_lost_cat") ||
                    (entity instanceof Cat cat && cat.getPersistentDataContainer().has(LOST_CAT_KEY, PersistentDataType.BYTE))) {
                if (!foundCat) {
                    lostCatEntity = entity;
                    foundCat = true;
                } else {
                    entity.remove();
                    removed++;
                }
                continue;
            }

            // Chat display: nettoyer (recréé dynamiquement per-player)
            if (tags.contains("chapter3_cat_display")) {
                entity.remove();
                removed++;
                continue;
            }

            // Nettoyer les indices de l'investigation (recréés dynamiquement)
            if (tags.contains("chapter3_investigation_clue") || tags.contains("chapter3_clue_display")) {
                entity.remove();
                removed++;
                continue;
            }

            // === SURVIVANT: Réutiliser le premier ===
            if (tags.contains("chapter3_village_survivor") ||
                    (entity instanceof Villager v && v.getPersistentDataContainer().has(VILLAGE_SURVIVOR_KEY, PersistentDataType.BYTE))) {
                if (!foundSurvivor) {
                    villageSurvivorEntity = entity;
                    foundSurvivor = true;
                } else {
                    entity.remove();
                    removed++;
                }
                continue;
            }

            // Survivor display: nettoyer (recréé dynamiquement)
            if (tags.contains("chapter3_survivor_display")) {
                entity.remove();
                removed++;
                continue;
            }

            // Nettoyer les zombies de défense restants
            if (tags.contains("chapter3_defense_zombie")) {
                entity.remove();
                removed++;
                continue;
            }

            // Nettoyer le panneau de contrôle du Zeppelin
            if (tags.contains("chapter3_zeppelin_control") || tags.contains("chapter3_zeppelin_display")) {
                entity.remove();
                removed++;
                continue;
            }

            // Nettoyer le boss de la mine et son display
            if (tags.contains("chapter3_mine_boss") || tags.contains("chapter3_boss_display")) {
                entity.remove();
                removed++;
                continue;
            }
        }

        if (removed > 0) {
            plugin.log(Level.INFO,
                    "§e⚠ Nettoyage global Chapter3: " + removed + " entité(s) orpheline(s) supprimée(s)");
        }
    }

    /**
     * Spawn le PNJ Forain Marcel via JourneyNPCManager (Citizens API).
     */
    private void spawnForain(World world) {
        Location loc = FORAIN_LOCATION.clone();
        loc.setWorld(world);
        loc.setYaw(0); // Face au sud

        // Créer le NPC via JourneyNPCManager
        JourneyNPCManager.NPCConfig config = new JourneyNPCManager.NPCConfig(
            FORAIN_NPC_ID, "§d§l" + FORAIN_NAME, loc
        )
        .entityType(EntityType.VILLAGER)
        .profession(Villager.Profession.NITWIT)
        .lookClose(true)
        .display("§d🎪 §6§lLE FORAIN §d🎪", "§8─────────", "§f▶ Clic droit")
        .displayHeight(FORAIN_DISPLAY_HEIGHT)
        .displayScale(1.8f)
        .onInteract(event -> handleForainInteraction(event.getPlayer()));

        Entity npcEntity = npcManager.createOrGetNPC(config);
        if (npcEntity != null) {
            forainEntity = npcEntity;

            // Ajouter tag supplémentaire pour compatibilité avec l'ancien système
            npcEntity.getPersistentDataContainer().set(FORAIN_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
            npcEntity.addScoreboardTag("chapter3_forain");
        }
    }

    /**
     * Démarre le vérificateur de respawn du Forain.
     * Utilise JourneyNPCManager pour la récupération/création automatique.
     */
    private void startForainRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location forainLoc = FORAIN_LOCATION.clone();
                forainLoc.setWorld(world);

                // Ne rien faire si aucun joueur n'est à proximité
                boolean playerNearby = world.getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(forainLoc) < 10000); // 100 blocs
                if (!playerNearby) {
                    return;
                }

                // Skip si chunk non chargé
                if (!forainLoc.getChunk().isLoaded()) {
                    return;
                }

                // Vérifier si le NPC est valide via JourneyNPCManager
                if (forainEntity == null || !forainEntity.isValid()) {
                    spawnForain(world); // Utilise JourneyNPCManager
                }
            }
        }.runTaskTimer(plugin, 100L, 200L);
    }

    // ==================== CHAT PERDU (STEP 5) ====================

    /**
     * Spawn le chat perdu avec visibilité per-player
     */
    private void spawnLostCat(World world) {
        Location loc = CAT_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (lostCatEntity != null && lostCatEntity.isValid()) {
            lostCatEntity.remove();
        }
        if (lostCatDisplay != null && lostCatDisplay.isValid()) {
            lostCatDisplay.remove();
        }

        // Spawn le chat
        lostCatEntity = world.spawn(loc, Cat.class, cat -> {
            cat.setCustomNameVisible(false); // On utilise un TextDisplay per-player
            cat.setAI(false);
            cat.setInvulnerable(true);
            cat.setSilent(true);
            cat.setCollidable(false);
            cat.setCatType(Cat.Type.TABBY);
            cat.setTamed(false);
            cat.setSitting(true);

            // Tags
            cat.addScoreboardTag("chapter3_lost_cat");
            cat.addScoreboardTag("zombiez_npc");

            // PDC
            cat.getPersistentDataContainer().set(LOST_CAT_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister
            cat.setPersistent(false);

            // INVISIBLE PAR DÉFAUT - on contrôle la visibilité per-player
            cat.setVisibleByDefault(false);
        });

        // Créer le TextDisplay au-dessus (également invisible par défaut)
        createCatDisplay(world, loc);

        // Initialiser la visibilité pour les joueurs en ligne
        initializeCatVisibility();
    }

    /**
     * Crée le TextDisplay au-dessus du chat perdu (visibilité per-player)
     */
    private void createCatDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, CAT_DISPLAY_HEIGHT, 0);

        lostCatDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("🐱 ", NamedTextColor.GOLD))
                    .append(Component.text("Chat Perdu", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" 🐱", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour sauver", NamedTextColor.WHITE))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(128, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_cat_display");

            // INVISIBLE PAR DÉFAUT - on contrôle la visibilité per-player
            display.setVisibleByDefault(false);
        });
    }

    /**
     * Initialise la visibilité du chat pour tous les joueurs en ligne
     */
    private void initializeCatVisibility() {
        if (lostCatEntity == null || !lostCatEntity.isValid() ||
                lostCatDisplay == null || !lostCatDisplay.isValid()) {
            return;
        }

        Location catLoc = lostCatEntity.getLocation();

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inRange = player.getWorld().equals(catLoc.getWorld()) &&
                    player.getLocation().distanceSquared(catLoc) <= CAT_VIEW_DISTANCE * CAT_VIEW_DISTANCE;

            if (inRange) {
                boolean hasRescued = hasPlayerRescuedCat(player);
                updateCatVisibilityForPlayer(player, hasRescued);
            }
        }
    }

    /**
     * Démarre le système de mise à jour de visibilité per-player pour le chat
     * Inclut aussi le respawn si le chat a disparu (chunk unload, etc.)
     */
    private void startCatVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                // Respawn le chat s'il a disparu
                if (lostCatEntity == null || !lostCatEntity.isValid() || lostCatEntity.isDead()) {
                    spawnLostCat(world);
                    plugin.log(Level.FINE, "Chat perdu respawné (entité invalide)");
                    return; // Attendre le prochain tick pour la visibilité
                }

                // Recréer le TextDisplay si nécessaire
                if (lostCatDisplay == null || !lostCatDisplay.isValid()) {
                    createCatDisplay(world, lostCatEntity.getLocation());
                }

                Location catLoc = lostCatEntity.getLocation();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(catLoc.getWorld())) {
                        hideCatForPlayer(player);
                        continue;
                    }

                    double distSq = player.getLocation().distanceSquared(catLoc);
                    boolean inRange = distSq <= CAT_VIEW_DISTANCE * CAT_VIEW_DISTANCE;

                    if (inRange) {
                        boolean hasRescued = hasPlayerRescuedCat(player);
                        updateCatVisibilityForPlayer(player, hasRescued);
                    } else {
                        hideCatForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L); // Toutes les secondes
    }

    /**
     * Met à jour la visibilité du chat pour un joueur
     */
    private void updateCatVisibilityForPlayer(Player player, boolean hasRescued) {
        if (lostCatEntity == null || !lostCatEntity.isValid() ||
                lostCatDisplay == null || !lostCatDisplay.isValid()) {
            return;
        }

        if (hasRescued) {
            // Le joueur a déjà sauvé le chat: tout cacher
            player.hideEntity(plugin, lostCatEntity);
            player.hideEntity(plugin, lostCatDisplay);
        } else {
            // Le joueur n'a pas encore sauvé le chat: montrer
            player.showEntity(plugin, lostCatEntity);
            player.showEntity(plugin, lostCatDisplay);
        }
    }

    /**
     * Cache le chat et son display pour un joueur
     */
    private void hideCatForPlayer(Player player) {
        if (lostCatEntity != null && lostCatEntity.isValid()) {
            player.hideEntity(plugin, lostCatEntity);
        }
        if (lostCatDisplay != null && lostCatDisplay.isValid()) {
            player.hideEntity(plugin, lostCatDisplay);
        }
    }

    /**
     * Vérifie si le joueur a déjà sauvé le chat
     */
    public boolean hasPlayerRescuedCat(Player player) {
        // Vérifier le cache mémoire
        if (playersWhoRescuedCat.contains(player.getUniqueId())) {
            return true;
        }

        // Vérifier la progression dans le Journey
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_5);
        return progress >= 1;
    }

    /**
     * Gère l'interaction avec le chat perdu
     */
    private void handleCatInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si le joueur a déjà sauvé le chat
        if (hasPlayerRescuedCat(player)) {
            return; // Le chat ne devrait pas être visible, mais au cas où
        }

        // Vérifier si le joueur est à l'étape de sauvetage
        if (currentStep != JourneyStep.STEP_3_5) {
            player.sendMessage("");
            player.sendMessage("§e§l🐱 §fUn chat perdu... Il a l'air effrayé.");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.ENTITY_CAT_AMBIENT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Sauver le chat!
        playersWhoRescuedCat.add(player.getUniqueId());

        // Cacher le chat pour ce joueur
        updateCatVisibilityForPlayer(player, true);

        // Incrémenter la progression
        journeyManager.incrementProgress(player, JourneyStep.StepType.RESCUE_LOST_CAT, 1);

        // Notification de succès
        player.sendTitle(
                "§a§l🐱 CHAT SAUVÉ!",
                "§7Il te remercie chaleureusement",
                10, 60, 20);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l🐱 CHAT SAUVÉ!");
        player.sendMessage("");
        player.sendMessage("  §7Le chat ronronne de bonheur...");
        player.sendMessage("  §7Il te fait un câlin avant de partir!");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURREOW, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5);
    }

    // ==================== INVESTIGATION PATIENT ZÉRO (STEP 6) ====================

    // Contenu des indices (histoire du Patient Zéro)
    private static final String[][] CLUE_CONTENT = {
            // Indice 1: Journal du Docteur
            {
                    "§6§l📖 JOURNAL DU DOCTEUR",
                    "",
                    "§7\"Jour 1 - J'ai enfin isolé le virus.",
                    "§7Mon sérum expérimental pourrait",
                    "§7être la clé de notre salut...\"",
                    "",
                    "§8[Le journal est taché de sang séché]"
            },
            // Indice 2: Fiole Brisée
            {
                    "§c§l🧪 FIOLE BRISÉE",
                    "",
                    "§7Une fiole cassée repose au sol.",
                    "§7L'étiquette indique: §c\"SÉRUM-X\"",
                    "§7avec la mention §c\"NE PAS INHALER\"",
                    "",
                    "§8[L'échec de la cure originelle...]"
            },
            // Indice 3: Photo de Famille
            {
                    "§e§l📷 PHOTO DE FAMILLE",
                    "",
                    "§7Une photo ternie montre un homme",
                    "§7souriant avec sa femme et ses enfants.",
                    "§7Au dos: §e\"Dr. Marcus Vern - 2019\"",
                    "",
                    "§8[Il avait une vie avant tout ça...]"
            },
            // Indice 4: Lettre d'Adieu
            {
                    "§d§l✉ LETTRE D'ADIEU",
                    "",
                    "§7\"À qui trouvera ceci...",
                    "§7Je suis le Patient Zéro.",
                    "§7Mon sérum devait sauver l'humanité,",
                    "§7mais il a créé cette apocalypse.",
                    "§7Pardonnez-moi... §8- Dr. Marcus Vern\"",
                    "",
                    "§c[La vérité sur l'origine du virus]"
            }
    };

    private static final String[] CLUE_NAMES = {
            "§6📖 Journal",
            "§c🧪 Fiole",
            "§e📷 Photo",
            "§d✉ Lettre"
    };

    /**
     * Spawn les indices de l'investigation autour de la maison
     * Utilise ItemDisplay + Interaction comme les caisses de ravitaillement
     * (Chapitre 2)
     */
    private void spawnInvestigationClues(World world) {
        for (int i = 0; i < 4; i++) {
            spawnClue(world, i);
        }
    }

    /**
     * Spawn un indice spécifique avec ItemDisplay (Paper glowing) + Interaction
     * (hitbox)
     */
    private void spawnClue(World world, int clueIndex) {
        Location loc = CLUE_LOCATIONS[clueIndex].clone();
        loc.setWorld(world);

        // Supprimer les anciens si existants
        if (clueHitboxes[clueIndex] != null && clueHitboxes[clueIndex].isValid()) {
            clueHitboxes[clueIndex].remove();
        }
        if (clueVisuals[clueIndex] != null && clueVisuals[clueIndex].isValid()) {
            clueVisuals[clueIndex].remove();
        }

        final int index = clueIndex;

        // 1. Créer le VISUEL (ItemDisplay avec Paper glowing)
        clueVisuals[clueIndex] = world.spawn(loc, ItemDisplay.class, display -> {
            // Item affiché: Paper
            display.setItemStack(new ItemStack(Material.PAPER));

            // Taille x1.2 pour visibilité
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0.5f, 0), // Translation (légèrement au-dessus du sol)
                    new AxisAngle4f(0, 0, 1, 0), // Left rotation
                    new Vector3f(1.2f, 1.2f, 1.2f), // Scale x1.2
                    new AxisAngle4f(0, 0, 1, 0) // Right rotation
            ));

            // Billboard CENTER pour que le papier tourne vers le joueur
            display.setBillboard(Display.Billboard.CENTER);

            // Glow effect jaune/or pour visibilité
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(255, 220, 100)); // Jaune doré

            // Distance de vue
            display.setViewRange(48f);

            // INVISIBLE PAR DÉFAUT - visible uniquement à proximité
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_clue_visual");
            display.addScoreboardTag("clue_visual_" + index);
        });

        // 2. Créer l'entité INTERACTION (invisible mais cliquable)
        clueHitboxes[clueIndex] = world.spawn(loc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
            // Taille de la hitbox (largeur et hauteur)
            interaction.setInteractionWidth(1.0f);
            interaction.setInteractionHeight(1.0f);

            // Marquer comme indice d'investigation
            interaction.getPersistentDataContainer().set(INVESTIGATION_CLUE_KEY, PersistentDataType.INTEGER, index);

            // Tags pour identification
            interaction.addScoreboardTag("chapter3_investigation_clue");
            interaction.addScoreboardTag("clue_index_" + index);
            interaction.addScoreboardTag("zombiez_npc");

            // INVISIBLE PAR DÉFAUT - visible uniquement à proximité
            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour les indices
     */
    private void startClueVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Location houseLoc = PATIENT_ZERO_HOUSE.clone();
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;
                houseLoc.setWorld(world);

                // Respawn les indices si les entités sont devenues invalides (chunk unload,
                // etc.)
                for (int i = 0; i < 4; i++) {
                    boolean visualInvalid = clueVisuals[i] == null || !clueVisuals[i].isValid();
                    boolean hitboxInvalid = clueHitboxes[i] == null || !clueHitboxes[i].isValid();
                    if (visualInvalid || hitboxInvalid) {
                        spawnClue(world, i);
                    }
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(world)) {
                        hideAllCluesForPlayer(player);
                        continue;
                    }

                    double distSq = player.getLocation().distanceSquared(houseLoc);
                    boolean inRange = distSq <= CLUE_VIEW_DISTANCE * CLUE_VIEW_DISTANCE;

                    if (inRange) {
                        updateClueVisibilityForPlayer(player);
                    } else {
                        hideAllCluesForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité des indices pour un joueur
     */
    private void updateClueVisibilityForPlayer(Player player) {
        int found = getPlayerCluesFound(player);

        for (int i = 0; i < 4; i++) {
            boolean hasFoundThis = (found & (1 << i)) != 0;

            if (clueHitboxes[i] != null && clueHitboxes[i].isValid()) {
                if (hasFoundThis) {
                    player.hideEntity(plugin, clueHitboxes[i]);
                } else {
                    player.showEntity(plugin, clueHitboxes[i]);
                }
            }

            if (clueVisuals[i] != null && clueVisuals[i].isValid()) {
                if (hasFoundThis) {
                    player.hideEntity(plugin, clueVisuals[i]);
                } else {
                    player.showEntity(plugin, clueVisuals[i]);
                }
            }
        }
    }

    /**
     * Cache tous les indices pour un joueur
     */
    private void hideAllCluesForPlayer(Player player) {
        for (int i = 0; i < 4; i++) {
            if (clueHitboxes[i] != null && clueHitboxes[i].isValid()) {
                player.hideEntity(plugin, clueHitboxes[i]);
            }
            if (clueVisuals[i] != null && clueVisuals[i].isValid()) {
                player.hideEntity(plugin, clueVisuals[i]);
            }
        }
    }

    /**
     * Obtient le bitmask des indices trouvés par un joueur
     */
    private int getPlayerCluesFound(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cache
        if (playerCluesFound.containsKey(uuid)) {
            return playerCluesFound.get(uuid);
        }

        // Reconstruire depuis la progression du Journey
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_6);
        // On ne peut pas savoir exactement quels indices, donc on assume les premiers
        int mask = 0;
        for (int i = 0; i < progress && i < 4; i++) {
            mask |= (1 << i);
        }
        playerCluesFound.put(uuid, mask);
        return mask;
    }

    /**
     * Compte le nombre d'indices trouvés
     */
    private int countCluesFound(int bitmask) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if ((bitmask & (1 << i)) != 0)
                count++;
        }
        return count;
    }

    /**
     * Vérifie si le joueur a terminé l'investigation
     */
    public boolean hasPlayerCompletedInvestigation(Player player) {
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_6);
        return progress >= 4;
    }

    /**
     * Gère l'interaction avec un indice
     */
    private void handleClueInteraction(Player player, int clueIndex) {
        // Vérifier si déjà terminé
        if (hasPlayerCompletedInvestigation(player)) {
            player.sendMessage("§7Tu as déjà terminé cette enquête.");
            return;
        }

        // Vérifier si cet indice a déjà été trouvé
        int found = getPlayerCluesFound(player);
        if ((found & (1 << clueIndex)) != 0) {
            return; // Déjà trouvé
        }

        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_3_6) {
            player.sendMessage("");
            player.sendMessage("§e§l❓ §fUn objet mystérieux...");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            player.sendMessage("");
            return;
        }

        // Marquer l'indice comme trouvé
        found |= (1 << clueIndex);
        playerCluesFound.put(player.getUniqueId(), found);

        // Cacher l'indice pour ce joueur
        updateClueVisibilityForPlayer(player);

        // Incrémenter la progression
        journeyManager.incrementProgress(player, JourneyStep.StepType.INVESTIGATE_PATIENT_ZERO, 1);

        // Afficher le contenu de l'indice
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        for (String line : CLUE_CONTENT[clueIndex]) {
            player.sendMessage("  " + line);
        }
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Compter les indices trouvés
        int cluesFoundCount = countCluesFound(found);

        // Effets
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.5);

        // Message de progression
        if (cluesFoundCount < 4) {
            player.sendMessage("§e§l🔍 Indice " + cluesFoundCount + "/4 trouvé: " + CLUE_NAMES[clueIndex]);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        } else {
            // Investigation terminée!
            handleInvestigationComplete(player);
        }
    }

    /**
     * Gère la fin de l'investigation
     */
    private void handleInvestigationComplete(Player player) {
        player.sendTitle(
                "§a§l🔍 ENQUÊTE TERMINÉE!",
                "§7Tu connais maintenant la vérité...",
                10, 80, 20);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l🔍 ENQUÊTE COMPLÈTE!");
        player.sendMessage("");
        player.sendMessage("  §7Tu as découvert l'origine du virus:");
        player.sendMessage("  §7Le §cDr. Marcus Vern§7 a créé le sérum");
        player.sendMessage("  §7qui a déclenché l'apocalypse zombie.");
        player.sendMessage("");
        player.sendMessage("  §e+600 Points §7| §a+15 Niveaux XP");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets épiques
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.3);
    }

    // ==================== DÉFENSE DU VILLAGE (STEP 7) ====================

    /**
     * Spawn le PNJ survivant du village
     */
    private void spawnVillageSurvivor(World world) {
        Location loc = VILLAGE_SURVIVOR_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
            villageSurvivorEntity.remove();
        }
        if (villageSurvivorDisplay != null && villageSurvivorDisplay.isValid()) {
            villageSurvivorDisplay.remove();
        }

        // Spawn le villageois survivant
        villageSurvivorEntity = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("Henri le Fermier", NamedTextColor.GREEN, TextDecoration.BOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setProfession(Villager.Profession.FARMER);
            villager.setVillagerType(Villager.Type.PLAINS);

            // Tags
            villager.addScoreboardTag("chapter3_village_survivor");
            villager.addScoreboardTag("no_trading");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(VILLAGE_SURVIVOR_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister
            villager.setPersistent(false);

            // Orientation (-90 = regarde vers l'Est)
            villager.setRotation(-90, 0);

            // Invisible par défaut (visibilité per-player)
            villager.setVisibleByDefault(false);
        });

        // Créer le TextDisplay au-dessus
        createSurvivorDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du survivant
     */
    private void createSurvivorDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, 2.5, 0);

        villageSurvivorDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("\u2694 ", NamedTextColor.GOLD)) // Crossed swords
                    .append(Component.text("SURVIVANT", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" \u2694", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501",
                            NamedTextColor.DARK_GRAY)) // Box drawing line
                    .append(Component.newline())
                    .append(Component.text("\u25B6 Clic droit pour aider", NamedTextColor.WHITE))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.8f, 1.8f, 1.8f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_survivor_display");

            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour le survivant.
     * SÉCURITÉ MAXMOBS=1: Respawne automatiquement avec nettoyage préalable.
     */
    private void startSurvivorVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location survivorLoc = VILLAGE_SURVIVOR_LOCATION.clone();
                survivorLoc.setWorld(world);

                // === SÉCURITÉ SURVIVANT (maxmobs=1) ===
                // Ne respawn QUE si le chunk est déjà chargé par un joueur (évite boucle
                // infinie)
                if (villageSurvivorEntity == null || !villageSurvivorEntity.isValid()) {
                    if (survivorLoc.getChunk().isLoaded()) {
                        cleanupSurvivorEntities(world);
                        spawnVillageSurvivor(world);
                    }
                    return;
                }

                survivorLoc = villageSurvivorEntity.getLocation();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(survivorLoc.getWorld())) {
                        hideSurvivorForPlayer(player);
                        continue;
                    }

                    double distSq = player.getLocation().distanceSquared(survivorLoc);
                    boolean inRange = distSq <= 64 * 64;

                    if (inRange) {
                        boolean hasDefended = hasPlayerDefendedVillage(player);
                        boolean inDefenseEvent = activeDefenseEvents.containsKey(player.getUniqueId());
                        updateSurvivorVisibilityForPlayer(player, hasDefended, inDefenseEvent);
                    } else {
                        hideSurvivorForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité du survivant pour un joueur
     */
    private void updateSurvivorVisibilityForPlayer(Player player, boolean hasDefended, boolean inDefenseEvent) {
        if (villageSurvivorEntity == null || !villageSurvivorEntity.isValid() ||
                villageSurvivorDisplay == null || !villageSurvivorDisplay.isValid()) {
            return;
        }

        if (hasDefended && !inDefenseEvent) {
            // Le joueur a déjà défendu le village
            player.hideEntity(plugin, villageSurvivorEntity);
            player.hideEntity(plugin, villageSurvivorDisplay);
        } else {
            // Montrer le survivant
            player.showEntity(plugin, villageSurvivorEntity);
            player.showEntity(plugin, villageSurvivorDisplay);
        }
    }

    /**
     * Cache le survivant pour un joueur
     */
    private void hideSurvivorForPlayer(Player player) {
        if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
            player.hideEntity(plugin, villageSurvivorEntity);
        }
        if (villageSurvivorDisplay != null && villageSurvivorDisplay.isValid()) {
            player.hideEntity(plugin, villageSurvivorDisplay);
        }
    }

    /**
     * Nettoie TOUS les survivants orphelins dans le monde entier (maxmobs=1)
     */
    private void cleanupSurvivorEntities(World world) {
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter3_village_survivor") ||
                    entity.getScoreboardTags().contains("chapter3_survivor_display")) {
                entity.remove();
                removed++;
            } else if (entity instanceof Villager v
                    && v.getPersistentDataContainer().has(VILLAGE_SURVIVOR_KEY, PersistentDataType.BYTE)) {
                v.remove();
                removed++;
            }
        }
        // Log supprimé pour éviter le spam
    }

    /**
     * Vérifie si le joueur a déjà défendu le village
     */
    public boolean hasPlayerDefendedVillage(Player player) {
        if (playersWhoDefendedVillage.contains(player.getUniqueId())) {
            return true;
        }
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_7);
        return progress >= 1;
    }

    /**
     * Gère l'interaction avec le survivant du village
     */
    private void handleSurvivorInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si déjà en train de défendre
        if (activeDefenseEvents.containsKey(player.getUniqueId())) {
            player.sendMessage("§c✖ Tu es déjà en train de défendre le village!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        // Vérifier si en cooldown après échec (Henri mort ou joueur mort)
        Long cooldownEnd = defenseCooldowns.get(player.getUniqueId());
        if (cooldownEnd != null) {
            long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("");
                player.sendMessage("§a§lHenri: §f\"Attends un peu, je dois me remettre...\"");
                player.sendMessage("§7(Réessaie dans §e" + remaining + " seconde" + (remaining > 1 ? "s" : "") + "§7)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 0.8f);
                player.sendMessage("");
                return;
            } else {
                // Cooldown expiré, nettoyer
                defenseCooldowns.remove(player.getUniqueId());
            }
        }

        // Vérifier si déjà complété
        if (hasPlayerDefendedVillage(player)) {
            player.sendMessage("");
            player.sendMessage("§a§lHenri: §f\"Merci encore pour ton aide, héros!\"");
            player.sendMessage("§a§lHenri: §f\"Le village est en sécurité grâce à toi.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_3_7) {
            player.sendMessage("");
            player.sendMessage("§a§lHenri: §f\"Aide-moi s'il te plaît... Les zombies arrivent!\"");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Démarrer l'événement de défense
        startVillageDefense(player);
    }

    /**
     * Démarre l'événement de défense du village
     */
    private void startVillageDefense(Player player) {
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§lHenri: §f\"Les zombies approchent!\"");
        player.sendMessage("  §a§lHenri: §f\"Protège-moi pendant §e90 secondes§f!\"");
        player.sendMessage("");
        player.sendMessage("  §c⚠ Les zombies vont attaquer de tous les côtés!");
        player.sendMessage("  §e➤ Tue-les avant qu'ils n'atteignent Henri!");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        // Activer le mode boss bar personnalisée pour éviter le clignotement
        journeyManager.enterCustomBossBarMode(player);

        // Créer l'événement de défense
        VillageDefenseEvent defenseEvent = new VillageDefenseEvent(player.getUniqueId());
        activeDefenseEvents.put(player.getUniqueId(), defenseEvent);

        // Timer de countdown avant le début
        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    player.sendTitle("§c" + countdown, "§7Prépare-toi!", 0, 25, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                    countdown--;
                } else {
                    player.sendTitle("§c§l⚔ DÉFENSE!", "§7Protège Henri!", 0, 40, 10);
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                    startDefenseTimer(player, defenseEvent);
                    startZombieSpawner(player, defenseEvent);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Démarre le timer de défense
     */
    private void startDefenseTimer(Player player, VillageDefenseEvent defenseEvent) {
        defenseEvent.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeDefenseEvents.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                defenseEvent.secondsElapsed++;

                int remaining = DEFENSE_DURATION_SECONDS - defenseEvent.secondsElapsed;

                // Afficher la progression via la BossBar du Journey
                double healthPercent = defenseEvent.survivorHealth / SURVIVOR_MAX_DAMAGE;
                String healthBar = createHealthBar(defenseEvent.survivorHealth, SURVIVOR_MAX_DAMAGE);

                // Couleur selon la santé d'Henri
                org.bukkit.boss.BarColor barColor;
                if (healthPercent > 0.6) {
                    barColor = org.bukkit.boss.BarColor.GREEN;
                } else if (healthPercent > 0.3) {
                    barColor = org.bukkit.boss.BarColor.YELLOW;
                } else {
                    barColor = org.bukkit.boss.BarColor.RED;
                }

                String title = String.format("§c⚔ §fDéfense du Village §8| §e⏱ %ds §8| §aHenri: %s §8| §c☠ %d",
                        remaining, healthBar, defenseEvent.zombiesKilled);

                journeyManager.updateBossBarCustom(player, title, healthPercent, barColor);

                // Alertes de temps
                if (remaining == 60 || remaining == 30 || remaining == 10) {
                    player.sendMessage("§e⏱ Plus que §c" + remaining + " secondes§e!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                }

                // Vérifier si Henri est mort
                if (defenseEvent.survivorHealth <= 0) {
                    endDefense(player, defenseEvent, false);
                    cancel();
                    return;
                }

                // Vérifier si le temps est écoulé
                if (remaining <= 0) {
                    endDefense(player, defenseEvent, true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Démarre le spawner de zombies
     */
    private void startZombieSpawner(Player player, VillageDefenseEvent defenseEvent) {
        World world = player.getWorld();

        defenseEvent.spawnerTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !activeDefenseEvents.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                tickCounter++;

                // Vérifier la proximité des zombies au survivant (toutes les 10 ticks)
                if (tickCounter % 10 == 0) {
                    checkZombieProximityToSurvivor(player, defenseEvent);
                }

                // Spawn des zombies toutes les ZOMBIE_SPAWN_INTERVAL_TICKS
                if (tickCounter % (ZOMBIE_SPAWN_INTERVAL_TICKS / 5) == 0) {
                    for (int i = 0; i < ZOMBIES_PER_WAVE; i++) {
                        Location spawnPoint = ZOMBIE_SPAWN_POINTS[(int) (Math.random() * ZOMBIE_SPAWN_POINTS.length)]
                                .clone();
                        spawnPoint.setWorld(world);

                        // Ajuster Y pour le terrain (limite Y max = Henri.Y + 5 pour éviter spawn dans
                        // les arbres)
                        int maxY = (int) VILLAGE_SURVIVOR_LOCATION.getY() + 5; // 90 + 5 = 95
                        int terrainY = world.getHighestBlockYAt(spawnPoint) + 1;
                        spawnPoint.setY(Math.min(terrainY, maxY));

                        // Spawn un zombie via ZombieManager si disponible, sinon un zombie vanilla
                        spawnDefenseZombie(world, spawnPoint, player, defenseEvent);
                    }

                    // Effets d'alerte
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.7f, 0.8f);
                }
            }
        }.runTaskTimer(plugin, 20L, 5L); // Tick plus fréquent pour la vérification de proximité
    }

    /**
     * Vérifie si des zombies sont proches du survivant
     */
    private void checkZombieProximityToSurvivor(Player player, VillageDefenseEvent defenseEvent) {
        if (villageSurvivorEntity == null || !villageSurvivorEntity.isValid()) {
            return;
        }

        Location survivorLoc = villageSurvivorEntity.getLocation();
        double attackRange = 2.5; // Distance pour "attaquer" Henri

        // Vérifier chaque zombie
        java.util.List<UUID> zombiesToRemove = new java.util.ArrayList<>();

        for (UUID zombieId : defenseEvent.spawnedZombies) {
            Entity entity = plugin.getServer().getEntity(zombieId);
            if (entity == null || !entity.isValid()) {
                zombiesToRemove.add(zombieId);
                continue;
            }

            // Vérifier la distance au survivant
            if (entity.getLocation().distanceSquared(survivorLoc) <= attackRange * attackRange) {
                onDefenseZombieReachSurvivor(entity, player);
            }
        }

        // Nettoyer les zombies invalides
        defenseEvent.spawnedZombies.removeAll(zombiesToRemove);
    }

    /**
     * Spawn un zombie de défense via ZombieManager (système ZombieZ)
     */
    private void spawnDefenseZombie(World world, Location loc, Player player, VillageDefenseEvent defenseEvent) {
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            return; // Pas de ZombieManager, pas de spawn
        }

        // Spawn via ZombieManager pour bénéficier du système de dégâts ZombieZ
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.WALKER, loc, 8);
        if (activeZombie == null) {
            return; // Limite atteinte, on ne spawn pas
        }

        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (entity != null) {
            entity.addScoreboardTag("chapter3_defense_zombie");
            entity.addScoreboardTag("defense_owner_" + player.getUniqueId());
            defenseEvent.spawnedZombies.add(entity.getUniqueId());

            // Orienter le zombie vers le survivant
            if (entity instanceof org.bukkit.entity.Mob mob) {
                if (villageSurvivorEntity instanceof LivingEntity target) {
                    mob.setTarget(target);
                }
            }
        }
    }

    /**
     * Crée une barre de vie visuelle
     */
    private String createHealthBar(double current, double max) {
        int bars = 10;
        int filled = (int) Math.ceil((current / max) * bars);
        filled = Math.max(0, Math.min(bars, filled));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a█");
            } else {
                sb.append("§8█");
            }
        }
        return sb.toString();
    }

    /**
     * Termine l'événement de défense
     */
    private void endDefense(Player player, VillageDefenseEvent defenseEvent, boolean success) {
        // Annuler les tâches
        if (defenseEvent.timerTask != null) {
            defenseEvent.timerTask.cancel();
        }
        if (defenseEvent.spawnerTask != null) {
            defenseEvent.spawnerTask.cancel();
        }

        // Supprimer les zombies restants
        World world = player.getWorld();
        for (UUID zombieId : defenseEvent.spawnedZombies) {
            Entity entity = plugin.getServer().getEntity(zombieId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }

        // Retirer l'événement
        activeDefenseEvents.remove(player.getUniqueId());

        // Désactiver le mode boss bar personnalisée et restaurer la BossBar normale du
        // Journey
        journeyManager.exitCustomBossBarMode(player);
        journeyManager.createOrUpdateBossBar(player);

        if (success) {
            handleDefenseSuccess(player, defenseEvent);
        } else {
            handleDefenseFailure(player, defenseEvent);
        }
    }

    /**
     * Gère le succès de la défense
     */
    private void handleDefenseSuccess(Player player, VillageDefenseEvent defenseEvent) {
        playersWhoDefendedVillage.add(player.getUniqueId());

        // Incrémenter la progression
        journeyManager.incrementProgress(player, JourneyStep.StepType.DEFEND_VILLAGE, 1);

        // Cacher le survivant pour ce joueur
        updateSurvivorVisibilityForPlayer(player, true, false);

        // Calculer le bonus selon la santé restante d'Henri
        int healthPercent = (int) ((defenseEvent.survivorHealth / SURVIVOR_MAX_DAMAGE) * 100);
        int bonusPoints = 0;
        String bonusText = "";

        if (healthPercent >= 90) {
            bonusPoints = 200;
            bonusText = "§6§l★ PARFAIT! §eBonus +200 Points";
        } else if (healthPercent >= 70) {
            bonusPoints = 100;
            bonusText = "§e★ Excellent! §eBonus +100 Points";
        } else if (healthPercent >= 50) {
            bonusPoints = 50;
            bonusText = "§7★ Bien joué! §eBonus +50 Points";
        }

        if (bonusPoints > 0) {
            plugin.getEconomyManager().addPoints(player, bonusPoints);
        }

        player.sendTitle(
                "§a§lVILLAGE SAUVE!",
                "§7" + defenseEvent.zombiesKilled + " zombies tues",
                10, 80, 20);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l\u2694 DEFENSE REUSSIE!");
        player.sendMessage("");
        player.sendMessage("  §7Zombies tués: §c" + defenseEvent.zombiesKilled);
        player.sendMessage("  §7Santé d'Henri: §a" + healthPercent + "%");
        if (!bonusText.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("  " + bonusText);
        }
        player.sendMessage("");
        player.sendMessage("  §e+700 Points §7| §a+18 Niveaux XP");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Dialogue de remerciement
        player.sendMessage("§a§lHenri: §f\"Merci, héros! Tu as sauvé le village!\"");
        player.sendMessage("");

        // Effets épiques
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.3);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 2, 0), 30, 2, 2, 2, 0.1);
    }

    /**
     * Gère l'échec de la défense
     */
    private void handleDefenseFailure(Player player, VillageDefenseEvent defenseEvent) {
        // Ajouter le cooldown de 10 secondes
        defenseCooldowns.put(player.getUniqueId(),
                System.currentTimeMillis() + (DEFENSE_COOLDOWN_SECONDS * 1000L));

        player.sendTitle(
                "§c§lECHEC!",
                "§7Henri est mort...",
                10, 80, 20);

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §c§l\u2620 DEFENSE ECHOUEE!");
        player.sendMessage("");
        player.sendMessage("  §7Henri a été tué par les zombies...");
        player.sendMessage("  §7Zombies tués: §c" + defenseEvent.zombiesKilled);
        player.sendMessage("");
        player.sendMessage("  §e➤ Henri se remet... Réessaie dans §c" + DEFENSE_COOLDOWN_SECONDS + "s§e!");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        // Effets d'échec
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.05);
    }

    /**
     * Appelé quand un zombie de défense est tué
     */
    public void onDefenseZombieKilled(Player killer, Entity zombie) {
        VillageDefenseEvent defenseEvent = activeDefenseEvents.get(killer.getUniqueId());
        if (defenseEvent != null && defenseEvent.spawnedZombies.contains(zombie.getUniqueId())) {
            defenseEvent.zombiesKilled++;
            defenseEvent.spawnedZombies.remove(zombie.getUniqueId());
        }
    }

    /**
     * Vérifie si le survivant est attaqué par un zombie de défense
     */
    public void onDefenseZombieReachSurvivor(Entity zombie, Player owner) {
        VillageDefenseEvent defenseEvent = activeDefenseEvents.get(owner.getUniqueId());
        if (defenseEvent != null && defenseEvent.spawnedZombies.contains(zombie.getUniqueId())) {
            // Le zombie inflige des dégâts à Henri
            defenseEvent.survivorHealth -= 10.0;

            // Effet visuel
            if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
                villageSurvivorEntity.getWorld().spawnParticle(
                        Particle.DAMAGE_INDICATOR,
                        villageSurvivorEntity.getLocation().add(0, 1, 0),
                        5, 0.3, 0.3, 0.3, 0);
            }

            owner.playSound(owner.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 0.8f);

            // Le zombie disparaît après l'attaque
            zombie.remove();
            defenseEvent.spawnedZombies.remove(zombie.getUniqueId());
        }
    }

    /**
     * Classe interne pour suivre un événement de défense
     */
    private static class VillageDefenseEvent {
        final UUID playerId;
        int secondsElapsed = 0;
        double survivorHealth = SURVIVOR_MAX_DAMAGE;
        int zombiesKilled = 0;
        final Set<UUID> spawnedZombies = ConcurrentHashMap.newKeySet();
        BukkitTask timerTask;
        BukkitTask spawnerTask;

        VillageDefenseEvent(UUID playerId) {
            this.playerId = playerId;
        }
    }

    // ==================== RÉPARATION DU ZEPPELIN (STEP 8) ====================

    /**
     * Spawn le panneau de contrôle du Zeppelin (ItemDisplay + Interaction)
     */
    private void spawnZeppelinControl(World world) {
        Location loc = ZEPPELIN_CONTROL_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer les anciens si existants
        if (zeppelinControlVisual != null && zeppelinControlVisual.isValid()) {
            zeppelinControlVisual.remove();
        }
        if (zeppelinControlHitbox != null && zeppelinControlHitbox.isValid()) {
            zeppelinControlHitbox.remove();
        }
        if (zeppelinControlDisplay != null && zeppelinControlDisplay.isValid()) {
            zeppelinControlDisplay.remove();
        }

        // 1. Créer le VISUEL (ItemDisplay avec Command Block glowing)
        zeppelinControlVisual = world.spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.COMMAND_BLOCK));

            // Taille x1.2 pour visibilité sans être trop imposant
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0.5f, 0), // Translation (au-dessus du sol)
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f), // Scale x1.2
                    new AxisAngle4f(0, 0, 1, 0)));

            display.setBillboard(Display.Billboard.FIXED);

            // Glow effect jaune
            display.setGlowing(true);
            display.setGlowColorOverride(Color.fromRGB(255, 200, 50));

            display.setViewRange(64f);
            display.setVisibleByDefault(false);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_zeppelin_visual");
        });

        // 2. Créer l'entité INTERACTION (hitbox cliquable)
        zeppelinControlHitbox = world.spawn(loc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(1.2f);
            interaction.setInteractionHeight(1.2f);

            // Tags
            interaction.addScoreboardTag("chapter3_zeppelin_control");
            interaction.addScoreboardTag("zombiez_npc");

            // PDC
            interaction.getPersistentDataContainer().set(ZEPPELIN_CONTROL_KEY, PersistentDataType.BYTE, (byte) 1);

            interaction.setVisibleByDefault(false);
            interaction.setPersistent(false);
        });

        // Créer le TextDisplay au-dessus
        createZeppelinControlDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du panneau de contrôle
     */
    private void createZeppelinControlDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, 1.5, 0);

        zeppelinControlDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("⚡ ", NamedTextColor.GOLD))
                    .append(Component.text("PANNEAU DE CONTRÔLE", NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" ⚡", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("─────────────", NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text("▶ Clic droit pour réparer", NamedTextColor.WHITE))
                    .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.5f, 1.5f, 1.5f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_zeppelin_display");

            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour le panneau de contrôle
     */
    private void startZeppelinControlVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                // Respawn le panneau de contrôle si les entités sont devenues invalides (chunk
                // unload, etc.)
                boolean visualInvalid = zeppelinControlVisual == null || !zeppelinControlVisual.isValid();
                boolean hitboxInvalid = zeppelinControlHitbox == null || !zeppelinControlHitbox.isValid();
                boolean displayInvalid = zeppelinControlDisplay == null || !zeppelinControlDisplay.isValid();
                if (visualInvalid || hitboxInvalid || displayInvalid) {
                    spawnZeppelinControl(world);
                    return; // Attendre le prochain tick pour la visibilité
                }

                Location controlLoc = zeppelinControlHitbox.getLocation();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(controlLoc.getWorld())) {
                        hideZeppelinControlForPlayer(player);
                        continue;
                    }

                    double distSq = player.getLocation().distanceSquared(controlLoc);
                    boolean inRange = distSq <= 64 * 64;

                    if (inRange) {
                        boolean hasRepaired = hasPlayerRepairedZeppelin(player);
                        updateZeppelinControlVisibilityForPlayer(player, hasRepaired);
                    } else {
                        hideZeppelinControlForPlayer(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L);
    }

    /**
     * Met à jour la visibilité du panneau de contrôle pour un joueur
     */
    private void updateZeppelinControlVisibilityForPlayer(Player player, boolean hasRepaired) {
        if (zeppelinControlVisual == null || !zeppelinControlVisual.isValid() ||
                zeppelinControlHitbox == null || !zeppelinControlHitbox.isValid() ||
                zeppelinControlDisplay == null || !zeppelinControlDisplay.isValid()) {
            return;
        }

        if (hasRepaired) {
            // Le joueur a déjà réparé le Zeppelin
            player.hideEntity(plugin, zeppelinControlVisual);
            player.hideEntity(plugin, zeppelinControlHitbox);
            player.hideEntity(plugin, zeppelinControlDisplay);
        } else {
            // Montrer le panneau de contrôle
            player.showEntity(plugin, zeppelinControlVisual);
            player.showEntity(plugin, zeppelinControlHitbox);
            player.showEntity(plugin, zeppelinControlDisplay);
        }
    }

    /**
     * Cache le panneau de contrôle pour un joueur
     */
    private void hideZeppelinControlForPlayer(Player player) {
        if (zeppelinControlVisual != null && zeppelinControlVisual.isValid()) {
            player.hideEntity(plugin, zeppelinControlVisual);
        }
        if (zeppelinControlHitbox != null && zeppelinControlHitbox.isValid()) {
            player.hideEntity(plugin, zeppelinControlHitbox);
        }
        if (zeppelinControlDisplay != null && zeppelinControlDisplay.isValid()) {
            player.hideEntity(plugin, zeppelinControlDisplay);
        }
    }

    /**
     * Vérifie si le joueur a déjà réparé le Zeppelin
     */
    public boolean hasPlayerRepairedZeppelin(Player player) {
        if (playersWhoRepairedZeppelin.contains(player.getUniqueId())) {
            return true;
        }
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_8);
        return progress >= 1;
    }

    /**
     * Gère l'interaction avec le panneau de contrôle du Zeppelin
     */
    private void handleZeppelinControlInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si déjà réparé
        if (hasPlayerRepairedZeppelin(player)) {
            player.sendMessage("");
            player.sendMessage("§e⚡ §fLe panneau de contrôle fonctionne parfaitement!");
            player.sendMessage("§7Le Zeppelin est prêt à voler.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 1.5f);
            player.sendMessage("");
            return;
        }

        // Vérifier si à la bonne étape
        if (currentStep != JourneyStep.STEP_3_8) {
            player.sendMessage("");
            player.sendMessage("§e⚡ §fUn panneau de contrôle endommagé...");
            player.sendMessage("§7Les fils semblent déconnectés.");
            player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            player.sendMessage("");
            return;
        }

        // Ouvrir le puzzle de fils
        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §e§l⚡ PANNEAU DE CONTRÔLE ENDOMMAGÉ");
        player.sendMessage("");
        player.sendMessage("  §7Les fils du Zeppelin sont déconnectés!");
        player.sendMessage("  §7Reconnecte-les pour réparer le système.");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f);

        // Ouvrir le GUI du puzzle après un court délai
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    wirePuzzleListener.addPlayerInGame(player.getUniqueId());
                    WirePuzzleGUI gui = new WirePuzzleGUI(plugin, player, Chapter3Systems.this);
                    gui.open();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 30L);
    }

    /**
     * Appelé quand le joueur a réparé le Zeppelin avec succès
     */
    public void onZeppelinRepaired(Player player) {
        playersWhoRepairedZeppelin.add(player.getUniqueId());

        // Incrémenter la progression de l'étape
        journeyManager.incrementProgress(player, JourneyStep.StepType.REPAIR_ZEPPELIN, 1);

        // Cacher le panneau de contrôle pour ce joueur
        updateZeppelinControlVisibilityForPlayer(player, true);

        // Effets supplémentaires
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.3);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    // ==================== BOSS DE LA MINE (STEP 10) ====================

    /**
     * Initialise le TextDisplay au-dessus du spawn du boss
     */
    private void initializeBossDisplay(World world) {
        Location displayLoc = MINE_BOSS_LOCATION.clone();
        displayLoc.setWorld(world);
        displayLoc.add(0.5, BOSS_DISPLAY_HEIGHT, 0.5);

        // Ne créer le display que si le chunk est chargé
        if (!displayLoc.getChunk().isLoaded()) {
            return;
        }

        // Si on a déjà un display valide, ne rien faire
        if (bossSpawnDisplay != null && bossSpawnDisplay.isValid()) {
            return;
        }

        // Chercher un display existant (persisté après reboot)
        for (Entity entity : world.getNearbyEntities(displayLoc, 20, 20, 20)) {
            if (entity instanceof TextDisplay td && entity.getScoreboardTags().contains("chapter3_boss_display")) {
                bossSpawnDisplay = td;
                return; // Réutiliser l'existant
            }
        }

        // Aucun display trouvé, créer un nouveau
        bossSpawnDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));

            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(2f, 2f, 2f),
                    new AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(1f);
            display.setPersistent(true);
            display.addScoreboardTag("chapter3_boss_display");

            display.text(Component.text("§4§l☠ " + MINE_BOSS_NAME + " §4§l☠\n§7En attente de spawn..."));
        });
    }

    /**
     * Met à jour le texte du display selon l'état du boss
     * UN SEUL TextDisplay qui affiche:
     * - Boss vivant: Nom + barre de vie (le customNameVisible est désactivé sur le
     * boss)
     * - Boss mort: Countdown de respawn
     */
    private void updateBossDisplayText(TextDisplay display, boolean bossAlive, int respawnSeconds) {
        if (display == null || !display.isValid())
            return;

        StringBuilder text = new StringBuilder();
        text.append("§4§l☠ ").append(MINE_BOSS_NAME).append(" §4§l☠\n");

        if (bossAlive && mineBossEntity != null && mineBossEntity.isValid()) {
            // Boss vivant - afficher les HP
            if (mineBossEntity instanceof LivingEntity livingBoss) {
                double currentHealth = livingBoss.getHealth();
                double maxHealth = livingBoss.getAttribute(Attribute.MAX_HEALTH).getValue();
                int healthPercent = (int) ((currentHealth / maxHealth) * 100);

                // Couleur selon le pourcentage de vie
                String healthColor;
                if (healthPercent > 50) {
                    healthColor = "§a"; // Vert
                } else if (healthPercent > 25) {
                    healthColor = "§e"; // Jaune
                } else {
                    healthColor = "§c"; // Rouge
                }

                text.append(healthColor).append("❤ ")
                        .append((int) currentHealth).append("§7/§f").append((int) maxHealth);
            }
        } else {
            // Boss mort - afficher countdown de respawn
            if (respawnSeconds > 0) {
                text.append("§e⏱ Respawn dans: §f").append(respawnSeconds).append("s");
            } else {
                text.append("§7En attente de spawn...");
            }
        }

        display.text(Component.text(text.toString()));
    }

    /**
     * Démarre la tâche de mise à jour du display et du leash range
     */
    private void startBossDisplayUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location bossLoc = MINE_BOSS_LOCATION.clone();
                bossLoc.setWorld(world);

                // IMPORTANT: Ne rien faire si aucun joueur n'est à proximité (100 blocs)
                boolean playerNearby = world.getPlayers().stream()
                        .anyMatch(p -> p.getLocation().distanceSquared(bossLoc) < 10000); // 100^2
                if (!playerNearby) {
                    return;
                }

                // Recréer le display s'il est invalide
                if (bossSpawnDisplay == null || !bossSpawnDisplay.isValid()) {
                    initializeBossDisplay(world);
                }

                // Vérifier si le boss doit être respawné
                boolean bossAlive = mineBossEntity != null && mineBossEntity.isValid() && !mineBossEntity.isDead();
                int respawnSeconds = 0;

                if (bossAlive) {
                    checkBossLeashRange(world);
                } else {
                    if (!bossRespawnScheduled && bossRespawnTime == 0) {
                        spawnMineBoss(world);
                        bossAlive = mineBossEntity != null && mineBossEntity.isValid();
                    } else if (bossRespawnTime > 0) {
                        long remaining = (bossRespawnTime - System.currentTimeMillis()) / 1000;
                        respawnSeconds = Math.max(0, (int) remaining);
                    }
                }

                // Mettre à jour le texte du display
                if (bossSpawnDisplay != null && bossSpawnDisplay.isValid()) {
                    updateBossDisplayText(bossSpawnDisplay, bossAlive, respawnSeconds);
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    /**
     * Vérifie si le boss est trop loin de son spawn et le téléporte si nécessaire
     */
    private void checkBossLeashRange(World world) {
        if (mineBossEntity == null || !mineBossEntity.isValid() || mineBossEntity.isDead()) {
            return;
        }

        Location spawnLoc = MINE_BOSS_LOCATION.clone();
        spawnLoc.setWorld(world);
        spawnLoc.add(0.5, 0, 0.5);

        Location bossLoc = mineBossEntity.getLocation();

        if (!bossLoc.getWorld().equals(world)) {
            return;
        }

        double distanceSquared = bossLoc.distanceSquared(spawnLoc);

        if (distanceSquared > BOSS_LEASH_RANGE_SQUARED) {
            teleportBossToSpawn(world, spawnLoc);
        }
    }

    /**
     * Téléporte le boss à son point de spawn avec effets visuels
     */
    private void teleportBossToSpawn(World world, Location spawnLoc) {
        if (mineBossEntity == null || !mineBossEntity.isValid())
            return;

        Location oldLoc = mineBossEntity.getLocation();

        // Effets de disparition
        world.spawnParticle(Particle.SMOKE, oldLoc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
        world.playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);

        // Téléporter le boss
        spawnLoc.setYaw(oldLoc.getYaw());
        spawnLoc.setPitch(0);
        mineBossEntity.teleport(spawnLoc);

        // Effets d'apparition
        world.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);

        // Reset de la cible
        if (mineBossEntity instanceof Mob mob) {
            mob.setTarget(null);
        }

        // Heal partiel au retour (5%)
        if (mineBossEntity instanceof org.bukkit.entity.LivingEntity livingBoss) {
            double currentHealth = livingBoss.getHealth();
            double maxHealth = livingBoss.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healAmount = maxHealth * 0.05;
            livingBoss.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }

        // Message aux joueurs proches
        for (Player player : world.getNearbyEntities(spawnLoc, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§4§l☠ §cLe " + MINE_BOSS_NAME + " §7retourne dans les profondeurs!");
        }
    }

    /**
     * Fait spawn le boss de la mine via le système ZombieZ
     */
    private void spawnMineBoss(World world) {
        // Protection anti-spawn multiple
        if (mineBossEntity != null && mineBossEntity.isValid() && !mineBossEntity.isDead()) {
            return;
        }

        Location loc = MINE_BOSS_LOCATION.clone();
        loc.setWorld(world);

        // Ne spawn que si le chunk est chargé
        if (!loc.getChunk().isLoaded()) {
            return;
        }

        // Chercher un boss existant dans le monde (persisté après reboot)
        for (Entity entity : world.getNearbyEntities(loc, 50, 30, 50)) {
            if (entity instanceof Zombie z
                    && z.getPersistentDataContainer().has(MINE_BOSS_KEY, PersistentDataType.BYTE)) {
                mineBossEntity = z;
                return; // Réutiliser l'existant
            }
        }

        // Nettoyer les contributeurs
        bossContributors.clear();
        bossRespawnScheduled = false;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(Level.WARNING, "ZombieManager non disponible, spawn du boss de la mine annulé");
            return;
        }

        // Spawn via ZombieManager (niveau 15 pour la zone 6)
        int bossLevel = 15;
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.MINE_OVERLORD, loc, bossLevel);

        if (activeZombie == null) {
            plugin.log(Level.WARNING, "Échec du spawn du boss de la mine via ZombieManager");
            return;
        }

        // Récupérer l'entité pour appliquer les modifications visuelles
        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (!(entity instanceof Zombie boss)) {
            plugin.log(Level.WARNING, "Boss de la mine n'est pas un Zombie valide");
            return;
        }

        mineBossEntity = boss;

        // Appliquer les modifications visuelles
        applyMineBossVisuals(boss);

        // Marquer comme boss de la mine pour le tracking Journey
        boss.getPersistentDataContainer().set(MINE_BOSS_KEY, PersistentDataType.BYTE, (byte) 1);
        boss.addScoreboardTag("chapter3_mine_boss");
        boss.setPersistent(true); // IMPORTANT: survit au déchargement de chunk

        // Annoncer le spawn
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(loc) < 100) {
                player.sendMessage("");
                player.sendMessage("§4§l☠ Le Seigneur des Profondeurs émerge des ténèbres de la mine!");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
            }
        }

        plugin.log(Level.INFO, "§c§lBoss de la Mine spawné avec succès (système ZombieZ)");
    }

    /**
     * Applique les modifications visuelles au boss de la mine
     */
    private void applyMineBossVisuals(Zombie boss) {
        // Scale x3 via Paper API
        var scale = boss.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(3.0);
        }

        // Équipement diamant avec teinte sombre
        boss.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        boss.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        boss.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        boss.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        boss.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

        // Pas de drop d'équipement
        boss.getEquipment().setHelmetDropChance(0);
        boss.getEquipment().setChestplateDropChance(0);
        boss.getEquipment().setLeggingsDropChance(0);
        boss.getEquipment().setBootsDropChance(0);
        boss.getEquipment().setItemInMainHandDropChance(0);

        // Effets visuels
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2, false, true));
        boss.setGlowing(true);

        // Activer l'affichage du nom avec vie (système ZombieZ dynamique)
        boss.setCustomNameVisible(true);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // NOTE: Le Forain est géré par JourneyNPCManager

        // Interaction avec le chat perdu
        if (entity.getScoreboardTags().contains("chapter3_lost_cat")) {
            event.setCancelled(true);
            handleCatInteraction(player);
            return;
        }

        // Interaction avec un indice de l'investigation
        if (entity.getScoreboardTags().contains("chapter3_investigation_clue")) {
            event.setCancelled(true);
            // Récupérer l'index de l'indice depuis le PDC
            Integer clueIndex = entity.getPersistentDataContainer().get(INVESTIGATION_CLUE_KEY,
                    PersistentDataType.INTEGER);
            if (clueIndex != null) {
                handleClueInteraction(player, clueIndex);
            }
            return;
        }

        // Interaction avec le survivant du village
        if (entity.getScoreboardTags().contains("chapter3_village_survivor")) {
            event.setCancelled(true);
            handleSurvivorInteraction(player);
            return;
        }

        // Interaction avec le panneau de contrôle du Zeppelin
        if (entity.getScoreboardTags().contains("chapter3_zeppelin_control")) {
            event.setCancelled(true);
            handleZeppelinControlInteraction(player);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target == null)
            return;

        // Empêcher les mobs de cibler nos entités
        // NOTE: Le Forain est protégé par Citizens (setProtected)
        if (target.getScoreboardTags().contains("chapter3_lost_cat") ||
                target.getScoreboardTags().contains("chapter3_investigation_clue") ||
                target.getScoreboardTags().contains("chapter3_village_survivor") ||
                target.getScoreboardTags().contains("chapter3_zeppelin_control")) {
            event.setCancelled(true);
        }

        // Le boss de la mine ne cible que les joueurs
        if (event.getEntity().getScoreboardTags().contains("chapter3_mine_boss")) {
            if (!(target instanceof Player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Nos entités sont invulnérables
        // NOTE: Le Forain est protégé par Citizens (setProtected)
        if (event.getEntity().getScoreboardTags().contains("chapter3_lost_cat") ||
                event.getEntity().getScoreboardTags().contains("chapter3_investigation_clue") ||
                event.getEntity().getScoreboardTags().contains("chapter3_village_survivor") ||
                event.getEntity().getScoreboardTags().contains("chapter3_zeppelin_control")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeathForDefense(org.bukkit.event.entity.EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Vérifier si c'est un zombie de défense
        if (!entity.getScoreboardTags().contains("chapter3_defense_zombie")) {
            return;
        }

        // Trouver le propriétaire de l'événement de défense
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("defense_owner_")) {
                String uuidStr = tag.substring("defense_owner_".length());
                try {
                    UUID ownerId = UUID.fromString(uuidStr);
                    Player owner = Bukkit.getPlayer(ownerId);
                    if (owner != null && owner.isOnline()) {
                        onDefenseZombieKilled(owner, entity);
                    }
                } catch (IllegalArgumentException ignored) {
                }
                break;
            }
        }
    }

    /**
     * Tracker les joueurs qui attaquent le boss de la mine
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie boss))
            return;
        if (!boss.getPersistentDataContainer().has(MINE_BOSS_KEY, PersistentDataType.BYTE))
            return;

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
     * Gère la mort du boss de la mine
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie boss))
            return;
        if (!boss.getPersistentDataContainer().has(MINE_BOSS_KEY, PersistentDataType.BYTE))
            return;

        Location deathLoc = boss.getLocation();
        World world = boss.getWorld();

        // Effets de mort épiques
        world.playSound(deathLoc, Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 3, 1, 1, 1, 0);
        world.spawnParticle(Particle.SOUL, deathLoc, 50, 2, 2, 2, 0.1);

        // Valider l'étape pour TOUS les contributeurs
        for (UUID uuid : bossContributors) {
            Player contributor = Bukkit.getPlayer(uuid);
            if (contributor != null && contributor.isOnline()) {
                JourneyStep currentStep = journeyManager.getCurrentStep(contributor);
                if (currentStep != null && currentStep == JourneyStep.STEP_3_10) {
                    journeyManager.updateProgress(contributor, JourneyStep.StepType.KILL_MINE_BOSS, 1);

                    contributor.sendMessage("");
                    contributor.sendMessage("§6§l✦ §4Le Seigneur des Profondeurs a été vaincu!");
                    contributor.sendMessage("§7Tu as contribué à sa défaite.");
                    contributor.sendMessage("");
                }
            }
        }

        // Nettoyer
        bossContributors.clear();
        mineBossEntity = null;

        // Programmer le respawn
        if (!bossRespawnScheduled) {
            bossRespawnScheduled = true;
            bossRespawnTime = System.currentTimeMillis() + (BOSS_RESPAWN_SECONDS * 1000L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnMineBoss(world);
                    bossRespawnTime = 0;
                }
            }.runTaskLater(plugin, 20L * BOSS_RESPAWN_SECONDS);

            // Annoncer le respawn
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(deathLoc) < 100) {
                    player.sendMessage("§8Le Seigneur des Profondeurs reviendra dans §c" + BOSS_RESPAWN_SECONDS
                            + " secondes§8...");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        // Nettoyer les caches (sera rechargé via progression au reconnect)
        playersWhoCompletedPuzzle.remove(playerId);
        playersWhoRescuedCat.remove(playerId);
        playerCluesFound.remove(playerId);
        playersWhoDefendedVillage.remove(playerId);
        playersWhoRepairedZeppelin.remove(playerId);

        // Annuler l'événement de défense si actif
        VillageDefenseEvent defenseEvent = activeDefenseEvents.remove(playerId);
        if (defenseEvent != null) {
            if (defenseEvent.timerTask != null) {
                defenseEvent.timerTask.cancel();
            }
            if (defenseEvent.spawnerTask != null) {
                defenseEvent.spawnerTask.cancel();
            }
            // Supprimer les zombies du joueur qui quitte
            for (UUID zombieId : defenseEvent.spawnedZombies) {
                Entity entity = plugin.getServer().getEntity(zombieId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
            // Nettoyer le mode boss bar personnalisée
            journeyManager.exitCustomBossBarMode(event.getPlayer());
        }

        // Nettoyer le cooldown de défense
        defenseCooldowns.remove(playerId);
    }

    /**
     * Gère la mort du joueur pendant la défense du village
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Vérifier si le joueur était en train de défendre le village
        VillageDefenseEvent defenseEvent = activeDefenseEvents.get(playerId);
        if (defenseEvent == null) {
            return;
        }

        // Le joueur est mort pendant la défense - c'est un échec
        player.sendMessage("");
        player.sendMessage("§c§l☠ Tu es mort pendant la défense!");
        player.sendMessage("§7Henri n'a plus personne pour le protéger...");
        player.sendMessage("");

        // Terminer la défense comme un échec
        endDefense(player, defenseEvent, false);
    }

    // ==================== FORAIN INTERACTION ====================

    /**
     * Gère l'interaction avec le Forain
     */
    private void handleForainInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si le joueur est à l'étape du puzzle
        if (currentStep != JourneyStep.STEP_3_4) {
            // Pas encore à cette étape ou déjà complétée
            if (hasPlayerCompletedPuzzle(player)) {
                sendForainDialogue(player, DialogueType.ALREADY_COMPLETED);
            } else {
                sendForainDialogue(player, DialogueType.NOT_YET);
            }
            return;
        }

        // Vérifier si déjà complété (cas d'un reload)
        if (hasPlayerCompletedPuzzle(player)) {
            sendForainDialogue(player, DialogueType.ALREADY_COMPLETED);
            return;
        }

        // Proposer le puzzle
        sendForainDialogue(player, DialogueType.CHALLENGE);

        // Ouvrir le GUI après un court délai
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    openMemoryGame(player);
                }
            }
        }.runTaskLater(plugin, 40L); // 2 secondes
    }

    private void openMemoryGame(Player player) {
        memoryGameListener.addPlayerInGame(player.getUniqueId());
        MemoryGameGUI gui = new MemoryGameGUI(plugin, player, this);
        gui.open();

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
    }

    /**
     * Appelé quand le joueur complète le puzzle
     */
    public void onPuzzleCompleted(Player player) {
        playersWhoCompletedPuzzle.add(player.getUniqueId());

        // Incrémenter la progression de l'étape
        journeyManager.incrementProgress(player, JourneyStep.StepType.SOLVE_CIRCUS_PUZZLE, 1);

        // Dialogue de félicitations
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    sendForainDialogue(player, DialogueType.VICTORY);
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    /**
     * Vérifie si le joueur a déjà complété le puzzle
     */
    public boolean hasPlayerCompletedPuzzle(Player player) {
        // Vérifier le cache mémoire
        if (playersWhoCompletedPuzzle.contains(player.getUniqueId())) {
            return true;
        }

        // Vérifier la progression dans le Journey
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_3_4);
        return progress >= 1;
    }

    // ==================== DIALOGUES ====================

    private enum DialogueType {
        NOT_YET,
        CHALLENGE,
        VICTORY,
        ALREADY_COMPLETED
    }

    private void sendForainDialogue(Player player, DialogueType type) {
        player.sendMessage("");

        switch (type) {
            case NOT_YET -> {
                player.sendMessage("§d§lMarcel: §f\"Hé toi! Tu n'es pas encore prêt pour mon défi...\"");
                player.sendMessage("§7(Progresse dans ton Journal pour débloquer cette quête)");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            case CHALLENGE -> {
                player.sendMessage("§d§lMarcel: §f\"Bienvenue dans mon cirque, survivant!\"");
                player.sendMessage("");
                player.sendMessage("§d§lMarcel: §f\"J'ai un petit jeu pour toi...\"");
                player.sendMessage("§d§lMarcel: §f\"Un jeu de §emémoire§f! Trouve toutes les paires!\"");
                player.sendMessage("");
                player.sendMessage("§e§l➤ Le puzzle s'ouvre dans 2 secondes...");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1.2f);
            }
            case VICTORY -> {
                player.sendMessage("§d§lMarcel: §f\"Bravo! Tu as l'esprit vif!\"");
                player.sendMessage("§d§lMarcel: §f\"Le cirque te salue, champion!\"");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            }
            case ALREADY_COMPLETED -> {
                player.sendMessage("§d§lMarcel: §f\"Tu as déjà résolu mon énigme!\"");
                player.sendMessage("§d§lMarcel: §f\"Continue ton aventure, survivant!\"");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            }
        }

        player.sendMessage("");
    }

    // ==================== CLEANUP ====================

    /**
     * Nettoie les ressources du chapitre 3
     */
    public void shutdown() {
        // Nettoyer le Forain
        if (forainEntity != null && forainEntity.isValid()) {
            forainEntity.remove();
        }
        if (forainDisplay != null && forainDisplay.isValid()) {
            forainDisplay.remove();
        }

        // Nettoyer le chat perdu
        if (lostCatEntity != null && lostCatEntity.isValid()) {
            lostCatEntity.remove();
        }
        if (lostCatDisplay != null && lostCatDisplay.isValid()) {
            lostCatDisplay.remove();
        }

        // Nettoyer les indices de l'investigation
        for (int i = 0; i < 4; i++) {
            if (clueHitboxes[i] != null && clueHitboxes[i].isValid()) {
                clueHitboxes[i].remove();
            }
            if (clueVisuals[i] != null && clueVisuals[i].isValid()) {
                clueVisuals[i].remove();
            }
        }

        // Nettoyer le survivant du village
        if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
            villageSurvivorEntity.remove();
        }
        if (villageSurvivorDisplay != null && villageSurvivorDisplay.isValid()) {
            villageSurvivorDisplay.remove();
        }

        // Nettoyer le panneau de contrôle du Zeppelin
        if (zeppelinControlVisual != null && zeppelinControlVisual.isValid()) {
            zeppelinControlVisual.remove();
        }
        if (zeppelinControlHitbox != null && zeppelinControlHitbox.isValid()) {
            zeppelinControlHitbox.remove();
        }
        if (zeppelinControlDisplay != null && zeppelinControlDisplay.isValid()) {
            zeppelinControlDisplay.remove();
        }

        // Annuler tous les événements de défense actifs
        for (VillageDefenseEvent defenseEvent : activeDefenseEvents.values()) {
            if (defenseEvent.timerTask != null) {
                defenseEvent.timerTask.cancel();
            }
            if (defenseEvent.spawnerTask != null) {
                defenseEvent.spawnerTask.cancel();
            }
            for (UUID zombieId : defenseEvent.spawnedZombies) {
                Entity entity = plugin.getServer().getEntity(zombieId);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
        }
        activeDefenseEvents.clear();

        // Nettoyer le boss de la mine
        if (mineBossEntity != null && mineBossEntity.isValid()) {
            mineBossEntity.remove();
        }
        if (bossSpawnDisplay != null && bossSpawnDisplay.isValid()) {
            bossSpawnDisplay.remove();
        }
        bossContributors.clear();

        // Nettoyer les caches
        playersWhoCompletedPuzzle.clear();
        playersWhoRescuedCat.clear();
        playerCluesFound.clear();
        playersWhoDefendedVillage.clear();
        playersWhoRepairedZeppelin.clear();
    }
}
