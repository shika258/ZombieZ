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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère les systèmes spécifiques au Chapitre 3:
 * - NPC Forain Marcel (étape 4) - Puzzle Memory Game
 */
public class Chapter3Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey FORAIN_NPC_KEY;

    // === POSITIONS ===
    // NPC Forain au cirque
    private static final Location FORAIN_LOCATION = new Location(null, 322.5, 93, 9201.5, 0, 0);

    // === NPC CONFIG ===
    private static final String FORAIN_NAME = "Marcel le Magnifique";
    private static final double FORAIN_DISPLAY_HEIGHT = 2.5;

    // === TRACKING ===
    private Entity forainEntity;
    private TextDisplay forainDisplay;

    // Joueurs ayant complété le puzzle (évite de refaire)
    private final Set<UUID> playersWhoCompletedPuzzle = ConcurrentHashMap.newKeySet();

    // Listener du Memory Game
    private final MemoryGameGUI.MemoryGameListener memoryGameListener;

    public Chapter3Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.FORAIN_NPC_KEY = new NamespacedKey(plugin, "forain_npc");

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

        plugin.log(Level.INFO, "§a✓ Chapter3Systems initialisé (Forain au cirque)");
    }

    /**
     * Nettoie les anciennes entités du chapitre 3
     */
    private void cleanupOldEntities(World world) {
        Location forainLoc = FORAIN_LOCATION.clone();
        forainLoc.setWorld(world);

        for (Entity entity : world.getNearbyEntities(forainLoc, 10, 10, 10)) {
            // Nettoyer les anciens NPCs Forain
            if (entity.getScoreboardTags().contains("chapter3_forain")) {
                entity.remove();
            }
            // Nettoyer les anciens TextDisplays
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("chapter3_forain_display")) {
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

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Interaction avec le Forain
        if (entity.getScoreboardTags().contains("chapter3_forain")) {
            event.setCancelled(true);
            handleForainInteraction(player);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        // Empêcher les mobs de cibler le Forain
        if (event.getTarget() != null && event.getTarget().getScoreboardTags().contains("chapter3_forain")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Le Forain est invulnérable
        if (event.getEntity().getScoreboardTags().contains("chapter3_forain")) {
            event.setCancelled(true);
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
        if (forainEntity != null && forainEntity.isValid()) {
            forainEntity.remove();
        }
        if (forainDisplay != null && forainDisplay.isValid()) {
            forainDisplay.remove();
        }
        playersWhoCompletedPuzzle.clear();
    }
}
