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
 */
public class Chapter4Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey PRIEST_NPC_KEY;
    private final NamespacedKey GRAVE_VISUAL_KEY;
    private final NamespacedKey GRAVE_HITBOX_KEY;
    private final NamespacedKey GRAVEDIGGER_BOSS_KEY;

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

    // === CONFIGURATION ===
    private static final int HITS_TO_DIG = 10; // Nombre de coups pour creuser une tombe
    private static final double GRAVE_VIEW_DISTANCE = 48;
    private static final double PRIEST_DISPLAY_HEIGHT = 2.5;
    private static final double GRAVE_DISPLAY_HEIGHT = 1.8;

    // === TRACKING ENTITÉS ===
    private Entity priestEntity;
    private TextDisplay priestDisplay;

    // Tombes (ItemDisplay COARSE_DIRT glowing + Interaction hitbox + TextDisplay)
    private final ItemDisplay[] graveVisuals = new ItemDisplay[5];
    private final Interaction[] graveHitboxes = new Interaction[5];
    private final TextDisplay[] graveDisplays = new TextDisplay[5];

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

    public Chapter4Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.PRIEST_NPC_KEY = new NamespacedKey(plugin, "gravedigger_priest");
        this.GRAVE_VISUAL_KEY = new NamespacedKey(plugin, "grave_visual");
        this.GRAVE_HITBOX_KEY = new NamespacedKey(plugin, "grave_hitbox");
        this.GRAVEDIGGER_BOSS_KEY = new NamespacedKey(plugin, "gravedigger_boss");

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

        // Spawn le prêtre
        spawnPriest(world);

        // Spawn les tombes (ArmorStands)
        spawnGraves(world);

        // Démarrer les systèmes de mise à jour
        startPriestRespawnChecker();
        startGraveVisibilityUpdater();
        startGraveRespawnChecker();

        plugin.log(Level.INFO, "§a✓ Chapter4Systems initialisé (Prêtre, Tombes du Fossoyeur)");
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

    /**
     * Spawn le boss "Le Premier Mort"
     */
    private void spawnGravediggerBoss(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(Level.WARNING, "ZombieManager non disponible pour spawn du boss Fossoyeur");
            return;
        }

        // Spawn via ZombieManager
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(
                ZombieType.GRAVEDIGGER_BOSS,
                loc.add(0, 1, 0),
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
        world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_AMBIENT, 2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0, 0, 0);
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
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Nettoyer les données temporaires
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
    }
}
