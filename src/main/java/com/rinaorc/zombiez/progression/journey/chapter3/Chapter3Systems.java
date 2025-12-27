package com.rinaorc.zombiez.progression.journey.chapter3;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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
 */
public class Chapter3Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey FORAIN_NPC_KEY;
    private final NamespacedKey LOST_CAT_KEY;
    private final NamespacedKey INVESTIGATION_CLUE_KEY;
    private final NamespacedKey VILLAGE_SURVIVOR_KEY;
    private final NamespacedKey DEFENSE_ZOMBIE_KEY;

    // === POSITIONS ===
    // NPC Forain au cirque
    private static final Location FORAIN_LOCATION = new Location(null, 322.5, 93, 9201.5, 0, 0);
    // Chat perdu
    private static final Location CAT_LOCATION = new Location(null, 1025.5, 120, 9136.5, 0, 0);
    // Maison du Patient Zéro (centre)
    private static final Location PATIENT_ZERO_HOUSE = new Location(null, 875, 88, 8944, 0, 0);

    // === POSITIONS DES INDICES (autour de la maison) ===
    private static final Location[] CLUE_LOCATIONS = {
        new Location(null, 873.5, 88.5, 8942.5, 0, 0),   // Indice 1: Journal - près de l'entrée
        new Location(null, 877.5, 89.5, 8945.5, 0, 0),   // Indice 2: Fiole - intérieur
        new Location(null, 874.5, 92.5, 8946.5, 0, 0),   // Indice 3: Photo - étage/grenier
        new Location(null, 876.5, 88.5, 8941.5, 0, 0)    // Indice 4: Lettre - caché dehors
    };

    // Village à défendre (NPC survivant)
    private static final Location VILLAGE_SURVIVOR_LOCATION = new Location(null, 527.5, 90, 8994.5, 180, 0);

    // Points de spawn des zombies autour du village (8 points en cercle)
    private static final Location[] ZOMBIE_SPAWN_POINTS = {
        new Location(null, 547, 90, 8994, 0, 0),   // Est
        new Location(null, 507, 90, 8994, 0, 0),   // Ouest
        new Location(null, 527, 90, 8974, 0, 0),   // Sud
        new Location(null, 527, 90, 9014, 0, 0),   // Nord
        new Location(null, 542, 90, 8979, 0, 0),   // Sud-Est
        new Location(null, 512, 90, 8979, 0, 0),   // Sud-Ouest
        new Location(null, 542, 90, 9009, 0, 0),   // Nord-Est
        new Location(null, 512, 90, 9009, 0, 0)    // Nord-Ouest
    };

    // Configuration de la défense
    private static final int DEFENSE_DURATION_SECONDS = 90;
    private static final int ZOMBIE_SPAWN_INTERVAL_TICKS = 60; // 3 secondes
    private static final int ZOMBIES_PER_WAVE = 3;
    private static final double DEFENSE_RADIUS = 25.0;
    private static final double SURVIVOR_MAX_DAMAGE = 100.0; // PV du survivant

    // === NPC CONFIG ===
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

    // Indices du Patient Zéro (per-player visibility)
    private final Entity[] clueEntities = new Entity[4];
    private final TextDisplay[] clueDisplays = new TextDisplay[4];

    // Survivant du village (étape 7)
    private Entity villageSurvivorEntity;
    private TextDisplay villageSurvivorDisplay;

    // Joueurs ayant complété le puzzle (évite de refaire)
    private final Set<UUID> playersWhoCompletedPuzzle = ConcurrentHashMap.newKeySet();
    // Joueurs ayant sauvé le chat
    private final Set<UUID> playersWhoRescuedCat = ConcurrentHashMap.newKeySet();
    // Indices trouvés par joueur (bitmask: bit 0 = indice 1, bit 1 = indice 2, etc.)
    private final Map<UUID, Integer> playerCluesFound = new ConcurrentHashMap<>();
    // Joueurs ayant défendu le village avec succès
    private final Set<UUID> playersWhoDefendedVillage = ConcurrentHashMap.newKeySet();
    // Événements de défense actifs par joueur
    private final Map<UUID, VillageDefenseEvent> activeDefenseEvents = new ConcurrentHashMap<>();

    // Listener du Memory Game
    private final MemoryGameGUI.MemoryGameListener memoryGameListener;

    public Chapter3Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.FORAIN_NPC_KEY = new NamespacedKey(plugin, "forain_npc");
        this.LOST_CAT_KEY = new NamespacedKey(plugin, "lost_cat");
        this.INVESTIGATION_CLUE_KEY = new NamespacedKey(plugin, "investigation_clue");
        this.VILLAGE_SURVIVOR_KEY = new NamespacedKey(plugin, "village_survivor");
        this.DEFENSE_ZOMBIE_KEY = new NamespacedKey(plugin, "defense_zombie");

        // Créer et enregistrer le listener du jeu de mémoire
        this.memoryGameListener = new MemoryGameGUI.MemoryGameListener(plugin);
        Bukkit.getPluginManager().registerEvents(memoryGameListener, plugin);

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

        // Spawn le Forain
        spawnForain(world);

        // Spawn le chat perdu
        spawnLostCat(world);

        // Spawn les indices du Patient Zéro
        spawnInvestigationClues(world);

        // Spawn le survivant du village
        spawnVillageSurvivor(world);

        // Démarrer les systèmes de visibilité per-player
        startCatVisibilityUpdater();
        startClueVisibilityUpdater();
        startSurvivorVisibilityUpdater();

        plugin.log(Level.INFO, "§a✓ Chapter3Systems initialisé (Forain, Chat, Investigation, Défense Village)");
    }

    /**
     * Nettoie les anciennes entités du chapitre 3
     */
    private void cleanupOldEntities(World world) {
        // Nettoyer le Forain
        Location forainLoc = FORAIN_LOCATION.clone();
        forainLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(forainLoc, 10, 10, 10)) {
            if (entity.getScoreboardTags().contains("chapter3_forain")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter3_forain_display")) {
                entity.remove();
            }
        }

        // Nettoyer le chat perdu
        Location catLoc = CAT_LOCATION.clone();
        catLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(catLoc, 10, 10, 10)) {
            if (entity.getScoreboardTags().contains("chapter3_lost_cat")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter3_cat_display")) {
                entity.remove();
            }
        }

        // Nettoyer les indices de l'investigation
        Location houseLoc = PATIENT_ZERO_HOUSE.clone();
        houseLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(houseLoc, 20, 20, 20)) {
            if (entity.getScoreboardTags().contains("chapter3_investigation_clue")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter3_clue_display")) {
                entity.remove();
            }
        }

        // Nettoyer le survivant du village
        Location villageLoc = VILLAGE_SURVIVOR_LOCATION.clone();
        villageLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(villageLoc, 10, 10, 10)) {
            if (entity.getScoreboardTags().contains("chapter3_village_survivor")) {
                entity.remove();
            }
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter3_survivor_display")) {
                entity.remove();
            }
        }

        // Nettoyer les zombies de défense restants
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter3_defense_zombie")) {
                entity.remove();
            }
        }
    }

    /**
     * Spawn le PNJ Forain Marcel
     */
    private void spawnForain(World world) {
        Location loc = FORAIN_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (forainEntity != null && forainEntity.isValid()) {
            forainEntity.remove();
        }
        if (forainDisplay != null && forainDisplay.isValid()) {
            forainDisplay.remove();
        }

        // Spawn le Villager
        forainEntity = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text(FORAIN_NAME, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setProfession(Villager.Profession.NITWIT);
            villager.setVillagerType(Villager.Type.PLAINS);

            // Tags
            villager.addScoreboardTag("chapter3_forain");
            villager.addScoreboardTag("no_trading");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(FORAIN_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister (évite les doublons au reboot)
            villager.setPersistent(false);

            // Orientation
            villager.setRotation(0, 0);
        });

        // Créer le TextDisplay au-dessus
        createForainDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du Forain
     */
    private void createForainDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, FORAIN_DISPLAY_HEIGHT, 0);

        forainDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                .append(Component.text("🎪 ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("LE FORAIN", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" 🎪", NamedTextColor.LIGHT_PURPLE))
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
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_forain_display");
        });
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
                new AxisAngle4f(0, 0, 0, 1)
            ));

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
     */
    private void startCatVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lostCatEntity == null || !lostCatEntity.isValid()) {
                    return;
                }

                if (lostCatDisplay == null || !lostCatDisplay.isValid()) {
                    World world = lostCatEntity.getWorld();
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
            10, 60, 20
        );

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
     */
    private void spawnInvestigationClues(World world) {
        for (int i = 0; i < 4; i++) {
            spawnClue(world, i);
        }
    }

    /**
     * Spawn un indice spécifique
     */
    private void spawnClue(World world, int clueIndex) {
        Location loc = CLUE_LOCATIONS[clueIndex].clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (clueEntities[clueIndex] != null && clueEntities[clueIndex].isValid()) {
            clueEntities[clueIndex].remove();
        }
        if (clueDisplays[clueIndex] != null && clueDisplays[clueIndex].isValid()) {
            clueDisplays[clueIndex].remove();
        }

        // Spawn un ArmorStand invisible comme point d'interaction
        final int index = clueIndex;
        clueEntities[clueIndex] = world.spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setCollidable(false);

            // Tags
            stand.addScoreboardTag("chapter3_investigation_clue");
            stand.addScoreboardTag("clue_index_" + index);
            stand.addScoreboardTag("zombiez_npc");

            // PDC avec l'index de l'indice
            stand.getPersistentDataContainer().set(INVESTIGATION_CLUE_KEY, PersistentDataType.INTEGER, index);

            stand.setPersistent(false);
            stand.setVisibleByDefault(false);
        });

        // TextDisplay au-dessus
        createClueDisplay(world, loc, clueIndex);
    }

    /**
     * Crée le TextDisplay pour un indice
     */
    private void createClueDisplay(World world, Location loc, int clueIndex) {
        Location displayLoc = loc.clone().add(0, 1.5, 0);

        clueDisplays[clueIndex] = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                .append(Component.text("❓ ", NamedTextColor.GOLD))
                .append(Component.text("Indice", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" ❓", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("▶ Clic droit", NamedTextColor.WHITE))
                .build());

            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.3f, 1.3f, 1.3f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setViewRange(0.3f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_clue_display");
            display.addScoreboardTag("clue_display_" + clueIndex);

            display.setVisibleByDefault(false);
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
                if (world == null) return;
                houseLoc.setWorld(world);

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

            if (clueEntities[i] != null && clueEntities[i].isValid()) {
                if (hasFoundThis) {
                    player.hideEntity(plugin, clueEntities[i]);
                } else {
                    player.showEntity(plugin, clueEntities[i]);
                }
            }

            if (clueDisplays[i] != null && clueDisplays[i].isValid()) {
                if (hasFoundThis) {
                    player.hideEntity(plugin, clueDisplays[i]);
                } else {
                    player.showEntity(plugin, clueDisplays[i]);
                }
            }
        }
    }

    /**
     * Cache tous les indices pour un joueur
     */
    private void hideAllCluesForPlayer(Player player) {
        for (int i = 0; i < 4; i++) {
            if (clueEntities[i] != null && clueEntities[i].isValid()) {
                player.hideEntity(plugin, clueEntities[i]);
            }
            if (clueDisplays[i] != null && clueDisplays[i].isValid()) {
                player.hideEntity(plugin, clueDisplays[i]);
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
            if ((bitmask & (1 << i)) != 0) count++;
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
            10, 80, 20
        );

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

            // Orientation
            villager.setRotation(180, 0);

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
                .append(Component.text("🛡️ ", NamedTextColor.GOLD))
                .append(Component.text("SURVIVANT", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" 🛡️", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("─────────", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("▶ Clic droit pour aider", NamedTextColor.WHITE))
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
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setViewRange(0.5f);
            display.setPersistent(false);
            display.addScoreboardTag("chapter3_survivor_display");

            display.setVisibleByDefault(false);
        });
    }

    /**
     * Démarre le système de visibilité per-player pour le survivant
     */
    private void startSurvivorVisibilityUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (villageSurvivorEntity == null || !villageSurvivorEntity.isValid()) {
                    return;
                }

                Location survivorLoc = villageSurvivorEntity.getLocation();

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

                // Afficher le temps restant dans l'action bar
                String healthBar = createHealthBar(defenseEvent.survivorHealth, SURVIVOR_MAX_DAMAGE);
                player.sendActionBar(Component.text()
                    .append(Component.text("⏱ ", NamedTextColor.GOLD))
                    .append(Component.text(remaining + "s", NamedTextColor.YELLOW))
                    .append(Component.text(" │ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Henri: ", NamedTextColor.GREEN))
                    .append(Component.text(healthBar, NamedTextColor.RED))
                    .append(Component.text(" │ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("☠ " + defenseEvent.zombiesKilled, NamedTextColor.RED))
                    .build());

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
                        Location spawnPoint = ZOMBIE_SPAWN_POINTS[(int) (Math.random() * ZOMBIE_SPAWN_POINTS.length)].clone();
                        spawnPoint.setWorld(world);

                        // Ajuster Y pour le terrain
                        spawnPoint.setY(world.getHighestBlockYAt(spawnPoint) + 1);

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
     * Spawn un zombie de défense
     */
    private void spawnDefenseZombie(World world, Location loc, Player player, VillageDefenseEvent defenseEvent) {
        // Essayer d'utiliser ZombieManager pour un zombie custom
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager != null) {
            try {
                // Utiliser un type de zombie basique
                ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.BASIC, loc, 8);
                Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
                if (entity != null) {
                    entity.addScoreboardTag("chapter3_defense_zombie");
                    entity.addScoreboardTag("defense_owner_" + player.getUniqueId());
                    defenseEvent.spawnedZombies.add(entity.getUniqueId());

                    // Orienter le zombie vers le survivant
                    if (entity instanceof org.bukkit.entity.Mob mob) {
                        if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
                            mob.setTarget(null); // Le zombie va vers le survivant, pas le joueur
                        }
                    }
                }
                return;
            } catch (Exception e) {
                // Fallback vers zombie vanilla
            }
        }

        // Fallback: spawn zombie vanilla
        Zombie zombie = world.spawn(loc, Zombie.class, z -> {
            z.addScoreboardTag("chapter3_defense_zombie");
            z.addScoreboardTag("defense_owner_" + player.getUniqueId());
            z.setTarget(null); // Pas de cible initiale
            z.customName(Component.text("Zombie", NamedTextColor.RED));
            z.setCustomNameVisible(false);
        });
        defenseEvent.spawnedZombies.add(zombie.getUniqueId());
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
            "§a§l🛡️ VILLAGE SAUVÉ!",
            "§7" + defenseEvent.zombiesKilled + " zombies tués",
            10, 80, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §a§l🛡️ DÉFENSE RÉUSSIE!");
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
        player.sendTitle(
            "§c§l💀 ÉCHEC!",
            "§7Henri est mort...",
            10, 80, 20
        );

        player.sendMessage("");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("  §c§l💀 DÉFENSE ÉCHOUÉE!");
        player.sendMessage("");
        player.sendMessage("  §7Henri a été tué par les zombies...");
        player.sendMessage("  §7Zombies tués: §c" + defenseEvent.zombiesKilled);
        player.sendMessage("");
        player.sendMessage("  §e➤ Reparle à Henri pour réessayer!");
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
                    5, 0.3, 0.3, 0.3, 0
                );
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

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Interaction avec le Forain
        if (entity.getScoreboardTags().contains("chapter3_forain")) {
            event.setCancelled(true);
            handleForainInteraction(player);
            return;
        }

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
            Integer clueIndex = entity.getPersistentDataContainer().get(INVESTIGATION_CLUE_KEY, PersistentDataType.INTEGER);
            if (clueIndex != null) {
                handleClueInteraction(player, clueIndex);
            }
            return;
        }

        // Interaction avec le survivant du village
        if (entity.getScoreboardTags().contains("chapter3_village_survivor")) {
            event.setCancelled(true);
            handleSurvivorInteraction(player);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target == null) return;

        // Empêcher les mobs de cibler nos entités
        if (target.getScoreboardTags().contains("chapter3_forain") ||
            target.getScoreboardTags().contains("chapter3_lost_cat") ||
            target.getScoreboardTags().contains("chapter3_investigation_clue") ||
            target.getScoreboardTags().contains("chapter3_village_survivor")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Nos entités sont invulnérables
        if (event.getEntity().getScoreboardTags().contains("chapter3_forain") ||
            event.getEntity().getScoreboardTags().contains("chapter3_lost_cat") ||
            event.getEntity().getScoreboardTags().contains("chapter3_investigation_clue") ||
            event.getEntity().getScoreboardTags().contains("chapter3_village_survivor")) {
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
                } catch (IllegalArgumentException ignored) {}
                break;
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
        }
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
            if (clueEntities[i] != null && clueEntities[i].isValid()) {
                clueEntities[i].remove();
            }
            if (clueDisplays[i] != null && clueDisplays[i].isValid()) {
                clueDisplays[i].remove();
            }
        }

        // Nettoyer le survivant du village
        if (villageSurvivorEntity != null && villageSurvivorEntity.isValid()) {
            villageSurvivorEntity.remove();
        }
        if (villageSurvivorDisplay != null && villageSurvivorDisplay.isValid()) {
            villageSurvivorDisplay.remove();
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

        // Nettoyer les caches
        playersWhoCompletedPuzzle.clear();
        playersWhoRescuedCat.clear();
        playerCluesFound.clear();
        playersWhoDefendedVillage.clear();
    }
}
