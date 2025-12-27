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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère les systèmes spécifiques au Chapitre 3:
 * - NPC Forain Marcel (étape 4) - Puzzle Memory Game
 * - Chat perdu (étape 5) - Visibilité per-player
 */
public class Chapter3Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey FORAIN_NPC_KEY;
    private final NamespacedKey LOST_CAT_KEY;

    // === POSITIONS ===
    // NPC Forain au cirque
    private static final Location FORAIN_LOCATION = new Location(null, 322.5, 93, 9201.5, 0, 0);
    // Chat perdu
    private static final Location CAT_LOCATION = new Location(null, 1025.5, 120, 9136.5, 0, 0);

    // === NPC CONFIG ===
    private static final String FORAIN_NAME = "Marcel le Magnifique";
    private static final double FORAIN_DISPLAY_HEIGHT = 2.5;
    private static final double CAT_DISPLAY_HEIGHT = 1.2;
    private static final double CAT_VIEW_DISTANCE = 64;

    // === TRACKING ===
    private Entity forainEntity;
    private TextDisplay forainDisplay;

    // Chat perdu (per-player visibility)
    private Entity lostCatEntity;
    private TextDisplay lostCatDisplay;

    // Joueurs ayant complété le puzzle (évite de refaire)
    private final Set<UUID> playersWhoCompletedPuzzle = ConcurrentHashMap.newKeySet();
    // Joueurs ayant sauvé le chat
    private final Set<UUID> playersWhoRescuedCat = ConcurrentHashMap.newKeySet();

    // Listener du Memory Game
    private final MemoryGameGUI.MemoryGameListener memoryGameListener;

    public Chapter3Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.FORAIN_NPC_KEY = new NamespacedKey(plugin, "forain_npc");
        this.LOST_CAT_KEY = new NamespacedKey(plugin, "lost_cat");

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

        // Démarrer le système de visibilité per-player pour le chat
        startCatVisibilityUpdater();

        plugin.log(Level.INFO, "§a✓ Chapter3Systems initialisé (Forain au cirque, Chat perdu)");
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
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target == null) return;

        // Empêcher les mobs de cibler le Forain ou le chat
        if (target.getScoreboardTags().contains("chapter3_forain") ||
            target.getScoreboardTags().contains("chapter3_lost_cat")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Le Forain et le chat sont invulnérables
        if (event.getEntity().getScoreboardTags().contains("chapter3_forain") ||
            event.getEntity().getScoreboardTags().contains("chapter3_lost_cat")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        // Nettoyer les caches (sera rechargé via progression au reconnect)
        playersWhoCompletedPuzzle.remove(playerId);
        playersWhoRescuedCat.remove(playerId);
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

        // Nettoyer les caches
        playersWhoCompletedPuzzle.clear();
        playersWhoRescuedCat.clear();
    }
}
