package com.rinaorc.zombiez.progression.journey.chapter1;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import com.rinaorc.zombiez.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
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
 * Gère les systèmes spécifiques au Chapitre 1:
 * - Étape 7: Aide le Fermier - Mini-jeu GUI d'extinction d'incendie
 */
public class Chapter1Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // === CLÉS PDC ===
    private final NamespacedKey FARMER_NPC_KEY;

    // === POSITIONS ===
    private static final Location FARMER_LOCATION = new Location(null, 474.5, 95, 9999.5, -90, 0);
    private static final double FARMER_DISPLAY_HEIGHT = 2.5;

    // === CONFIGURATION MINI-JEU ===
    private static final String FIRE_GUI_TITLE = "§c§l🔥 ÉTEINS L'INCENDIE! 🔥";
    private static final int FIRE_COUNT = 9; // Nombre de flammes à éteindre
    private static final int[] FIRE_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 25 }; // Slots des flammes

    // === ENTITÉS ===
    private Entity farmerEntity;
    private TextDisplay farmerDisplay;

    // === TRACKING JOUEURS ===
    private final Set<UUID> playersWhoCompletedFire = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersIntroducedToFarmer = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Integer>> playerExtinguishedFires = new ConcurrentHashMap<>();
    private final Set<UUID> playersInFireMinigame = ConcurrentHashMap.newKeySet();

    public Chapter1Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.FARMER_NPC_KEY = new NamespacedKey(plugin, "farmer_npc");

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
     * Initialise les systèmes du Chapitre 1
     */
    private void initialize() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.log(Level.WARNING, "§cMonde 'world' non trouvé pour Chapter1Systems");
            return;
        }

        // Nettoyer les anciennes entités
        cleanupOldEntities(world);

        // Spawn le fermier
        spawnFarmer(world);

        // Démarrer le respawn checker
        startFarmerRespawnChecker();

        plugin.log(Level.INFO, "§a✓ Chapter1Systems initialisé (Fermier)");
    }

    /**
     * Nettoie TOUTES les anciennes entités du fermier dans le MONDE ENTIER.
     * Garantit MAXMOBS=1 (une seule instance du fermier).
     * Recherche globale, pas seulement à proximité.
     */
    private void cleanupOldEntities(World world) {
        int removed = 0;

        // RECHERCHE GLOBALE dans tout le monde pour garantir maxmobs=1
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chapter1_farmer")) {
                entity.remove();
                removed++;
                continue;
            }
            if (entity.getScoreboardTags().contains("chapter1_farmer_display")) {
                entity.remove();
                removed++;
                continue;
            }
            // Fallback: vérifier par PDC
            if (entity instanceof Villager villager) {
                if (villager.getPersistentDataContainer().has(FARMER_NPC_KEY, PersistentDataType.BYTE)) {
                    villager.remove();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            plugin.log(Level.INFO,
                    "§e⚠ Nettoyage global Chapter1: " + removed + " entité(s) fermier orpheline(s) supprimée(s)");
        }
    }

    /**
     * Spawn le PNJ fermier
     */
    private void spawnFarmer(World world) {
        Location loc = FARMER_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (farmerEntity != null && farmerEntity.isValid()) {
            farmerEntity.remove();
        }
        if (farmerDisplay != null && farmerDisplay.isValid()) {
            farmerDisplay.remove();
        }

        // Spawn le Villager fermier
        farmerEntity = world.spawn(loc, Villager.class, villager -> {
            villager.customName(Component.text("Gérard le Fermier", NamedTextColor.GOLD, TextDecoration.BOLD));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setProfession(Villager.Profession.FARMER);
            villager.setVillagerType(Villager.Type.PLAINS);

            // Tags
            villager.addScoreboardTag("chapter1_farmer");
            villager.addScoreboardTag("no_trading");
            villager.addScoreboardTag("zombiez_npc");

            // PDC
            villager.getPersistentDataContainer().set(FARMER_NPC_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ne pas persister
            villager.setPersistent(false);

            // Orientation
            villager.setRotation(loc.getYaw(), 0);
        });

        // Créer le TextDisplay au-dessus
        createFarmerDisplay(world, loc);
    }

    /**
     * Crée le TextDisplay au-dessus du fermier
     */
    private void createFarmerDisplay(World world, Location loc) {
        Location displayLoc = loc.clone().add(0, FARMER_DISPLAY_HEIGHT, 0);

        farmerDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            display.text(Component.text()
                    .append(Component.text("🌾 ", NamedTextColor.YELLOW))
                    .append(Component.text("LE FERMIER", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" 🌾", NamedTextColor.YELLOW))
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
            display.addScoreboardTag("chapter1_farmer_display");
        });
    }

    /**
     * Démarre le vérificateur de respawn du fermier.
     * SÉCURITÉ MAXMOBS=1: Respawne automatiquement le fermier avec nettoyage
     * préalable.
     */
    private void startFarmerRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location farmerLoc = FARMER_LOCATION.clone();
                farmerLoc.setWorld(world);

                // === SÉCURITÉ FERMIER (maxmobs=1) ===
                // Ne respawn QUE si le chunk est déjà chargé par un joueur (évite boucle
                // infinie)
                if (farmerEntity == null || !farmerEntity.isValid() || farmerEntity.isDead()) {
                    if (farmerLoc.getChunk().isLoaded()) {
                        // Nettoyage global avant respawn pour garantir maxmobs=1
                        cleanupOldEntities(world);
                        spawnFarmer(world);
                    }
                }

                // TextDisplay: même logique
                if ((farmerDisplay == null || !farmerDisplay.isValid()) && farmerLoc.getChunk().isLoaded()) {
                    createFarmerDisplay(world, farmerLoc);
                }
            }
        }.runTaskTimer(plugin, 100L, 200L); // Intervalle augmenté pour réduire le spam
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Interaction avec le fermier
        if (entity.getPersistentDataContainer().has(FARMER_NPC_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            handleFarmerInteraction(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // Vérifier le titre du GUI avec getOriginalTitle()
        String title = event.getView().getOriginalTitle();
        if (!title.equals(FIRE_GUI_TITLE))
            return;

        // IMPORTANT: Annuler l'événement EN PREMIER pour empêcher le vol d'items
        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Vérifier si c'est une flamme
        if (clicked.getType() == Material.CAMPFIRE) {
            handleFireClick(player, slot, event.getInventory());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        String title = event.getView().getOriginalTitle();
        if (title.equals(FIRE_GUI_TITLE)) {
            playersInFireMinigame.remove(player.getUniqueId());
            // Reset les flammes éteintes si pas terminé
            if (!playersWhoCompletedFire.contains(player.getUniqueId())) {
                playerExtinguishedFires.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline())
                    return;

                // Recharger la progression
                int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_1_7);
                if (progress >= 1) {
                    playersWhoCompletedFire.add(player.getUniqueId());
                    playersIntroducedToFarmer.add(player.getUniqueId());
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        playersIntroducedToFarmer.remove(uuid);
        playersInFireMinigame.remove(uuid);
        playerExtinguishedFires.remove(uuid);
    }

    /**
     * Protège le fermier contre TOUS les types de dégâts.
     * Même si setInvulnerable(true) est défini, certains plugins/explosions peuvent
     * bypass.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onFarmerDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Vérifier par tag scoreboard (plus rapide)
        if (entity.getScoreboardTags().contains("chapter1_farmer")) {
            event.setCancelled(true);
            return;
        }

        // Fallback: vérifier par PDC
        if (entity instanceof Villager villager) {
            if (villager.getPersistentDataContainer().has(FARMER_NPC_KEY, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }

    // ==================== FERMIER INTERACTION ====================

    /**
     * Gère l'interaction avec le fermier
     */
    private void handleFarmerInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        // Vérifier si déjà complété
        if (hasPlayerCompletedFire(player)) {
            player.sendMessage("");
            player.sendMessage("§6§lGérard: §f\"Merci encore pour ton aide, héros!\"");
            player.sendMessage("§6§lGérard: §f\"Mon moulin est sauvé grâce à toi.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Si le joueur n'est pas à l'étape 7
        if (currentStep != JourneyStep.STEP_1_7) {
            player.sendMessage("");
            player.sendMessage("§6§lGérard: §f\"Bonjour voyageur!\"");
            player.sendMessage("§6§lGérard: §f\"Reviens me voir si tu as besoin d'aide.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        // Introduction si première fois
        if (!playersIntroducedToFarmer.contains(player.getUniqueId())) {
            introducePlayerToFarmer(player);
            return;
        }

        // Ouvrir le mini-jeu
        openFireMinigame(player);
    }

    /**
     * Introduction à la quête du fermier
     */
    private void introducePlayerToFarmer(Player player) {
        playersIntroducedToFarmer.add(player.getUniqueId());

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
                        player.sendTitle("§c§l🔥 AU SECOURS! 🔥", "§7Le moulin est en feu!", 10, 50, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.5f);
                    }
                    case 1 -> {
                        player.sendMessage("");
                        player.sendMessage("§6§lGérard: §f\"Mon moulin! Il brûle!\"");
                    }
                    case 2 -> {
                        player.sendMessage("§6§lGérard: §f\"S'il te plaît, aide-moi à éteindre\"");
                        player.sendMessage("§6§lGérard: §f\"les flammes avant qu'il soit trop tard!\"");
                    }
                    case 3 -> {
                        player.sendMessage("");
                        player.sendMessage("§e§l➤ §7Clique sur §cles flammes §7pour les éteindre!");
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

                        // Ouvrir automatiquement le mini-jeu
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.isOnline()) {
                                    openFireMinigame(player);
                                }
                            }
                        }.runTaskLater(plugin, 20L);

                        cancel();
                        return;
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 30L);
    }

    /**
     * Ouvre le mini-jeu d'extinction d'incendie
     */
    private void openFireMinigame(Player player) {
        playersInFireMinigame.add(player.getUniqueId());

        // Reset les flammes éteintes
        playerExtinguishedFires.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());

        // Créer l'inventaire
        Inventory gui = Bukkit.createInventory(null, 27, FIRE_GUI_TITLE);

        // Remplir avec du verre gris (mur du moulin)
        ItemStack wall = new ItemBuilder(Material.BROWN_STAINED_GLASS_PANE)
                .name("§8Mur du moulin")
                .build();

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, wall);
        }

        // Placer les flammes
        ItemStack fire = new ItemBuilder(Material.CAMPFIRE)
                .name("§c§l🔥 FLAMME 🔥")
                .lore("§7Clique pour éteindre!")
                .build();

        for (int slot : FIRE_SLOTS) {
            gui.setItem(slot, fire);
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
    }

    /**
     * Gère le clic sur une flamme
     */
    private void handleFireClick(Player player, int slot, Inventory gui) {
        UUID uuid = player.getUniqueId();

        Set<Integer> extinguished = playerExtinguishedFires.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        // Vérifier si déjà éteinte
        if (extinguished.contains(slot))
            return;

        // Vérifier si c'est un slot valide
        boolean isFireSlot = false;
        for (int fireSlot : FIRE_SLOTS) {
            if (fireSlot == slot) {
                isFireSlot = true;
                break;
            }
        }
        if (!isFireSlot)
            return;

        // Éteindre la flamme
        extinguished.add(slot);

        // Remplacer par de la fumée
        ItemStack smoke = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("§7§l✓ Éteint!")
                .lore("§aFlamme éteinte")
                .build();
        gui.setItem(slot, smoke);

        // Effets
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);

        int remaining = FIRE_COUNT - extinguished.size();

        // Message de progression
        if (remaining > 0) {
            player.sendMessage("§a✓ §7Flamme éteinte! §e" + remaining + " §7restante(s)");
        }

        // Vérifier si toutes les flammes sont éteintes
        if (extinguished.size() >= FIRE_COUNT) {
            onFireMinigameComplete(player);
        }
    }

    /**
     * Appelé quand le mini-jeu est complété
     */
    private void onFireMinigameComplete(Player player) {
        UUID uuid = player.getUniqueId();

        // Fermer l'inventaire
        player.closeInventory();

        // Marquer comme complété
        playersWhoCompletedFire.add(uuid);
        playersInFireMinigame.remove(uuid);
        playerExtinguishedFires.remove(uuid);

        // Mettre à jour la progression
        journeyManager.setStepProgress(player, JourneyStep.STEP_1_7, 1);

        // Effets visuels et sonores
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);

        // Message de victoire
        player.sendTitle("§a§l✓ INCENDIE ÉTEINT!", "§7Le fermier te remercie!", 10, 60, 20);

        player.sendMessage("");
        player.sendMessage("§8§m                                            ");
        player.sendMessage("");
        player.sendMessage("  §a§l✦ INCENDIE MAÎTRISÉ! ✦");
        player.sendMessage("");
        player.sendMessage("  §6§lGérard: §f\"Tu m'as sauvé! Merci infiniment!\"");
        player.sendMessage("  §6§lGérard: §f\"Mon moulin peut continuer à tourner.\"");
        player.sendMessage("");
        player.sendMessage("  §6Récompenses:");
        player.sendMessage("  §7▸ §e+290 Points");
        player.sendMessage("  §7▸ §a+4 XP");
        player.sendMessage("");
        player.sendMessage("§8§m                                            ");
        player.sendMessage("");

        // Compléter l'étape via JourneyManager
        journeyManager.onStepProgress(player, JourneyStep.STEP_1_7, 1);
    }

    /**
     * Vérifie si un joueur a déjà éteint l'incendie
     */
    public boolean hasPlayerCompletedFire(Player player) {
        if (playersWhoCompletedFire.contains(player.getUniqueId())) {
            return true;
        }
        int progress = journeyManager.getStepProgress(player, JourneyStep.STEP_1_7);
        return progress >= 1;
    }
}
