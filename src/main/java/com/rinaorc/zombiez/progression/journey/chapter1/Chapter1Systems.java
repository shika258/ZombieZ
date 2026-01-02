package com.rinaorc.zombiez.progression.journey.chapter1;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyNPCManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gère les systèmes spécifiques au Chapitre 1:
 * - Étape 7: Aide le Fermier - Mini-jeu GUI d'extinction d'incendie
 *
 * IMPORTANT: Utilise JourneyNPCManager pour les NPCs (Citizens API).
 */
public class Chapter1Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;
    private final JourneyNPCManager npcManager;

    // === NPC ID ===
    private static final String FARMER_NPC_ID = "chapter1_farmer";

    // === POSITIONS ===
    private static final Location FARMER_LOCATION = new Location(null, 474.5, 95, 9999.5, -90, 0);

    // === CONFIGURATION MINI-JEU ===
    private static final String FIRE_GUI_TITLE = "§c§l🔥 ÉTEINS L'INCENDIE! 🔥";
    private static final int FIRE_COUNT = 9;
    private static final int[] FIRE_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 25 };

    // === TRACKING JOUEURS ===
    private final Set<UUID> playersWhoCompletedFire = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersIntroducedToFarmer = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Integer>> playerExtinguishedFires = new ConcurrentHashMap<>();
    private final Set<UUID> playersInFireMinigame = ConcurrentHashMap.newKeySet();

    public Chapter1Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();
        this.npcManager = plugin.getJourneyNPCManager();

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

        // Spawn le fermier via JourneyNPCManager
        spawnFarmer(world);

        plugin.log(Level.INFO, "§a✓ Chapter1Systems initialisé (Fermier via " +
            (npcManager.isCitizensEnabled() ? "Citizens" : "Vanilla") + ")");
    }

    /**
     * Spawn le PNJ fermier via JourneyNPCManager
     */
    private void spawnFarmer(World world) {
        Location loc = FARMER_LOCATION.clone();
        loc.setWorld(world);

        // Créer le NPC via le manager centralisé
        // NOTE: Le nom natif est CACHÉ - toutes les infos passent par TextDisplay
        JourneyNPCManager.NPCConfig config = new JourneyNPCManager.NPCConfig(
            FARMER_NPC_ID,
            "Gérard le Fermier", // Nom interne (non affiché)
            loc
        )
        .entityType(EntityType.VILLAGER)
        .profession(Villager.Profession.FARMER)
        .lookClose(true)
        .display(
            "§e🌾 §6§lLE FERMIER §e🌾",
            "§fGérard",
            "§8─────────────",
            "§7▶ §fClic droit §7pour parler"
        )
        .displayScale(2.0f)
        .displayHeight(2.6)
        .onInteract(event -> {
            event.setCancelled(true);
            handleFarmerInteraction(event.getPlayer());
        });

        npcManager.createOrGetNPC(config);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = event.getView().getOriginalTitle();
        if (!title.equals(FIRE_GUI_TITLE))
            return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR)
            return;

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

    // ==================== FERMIER INTERACTION ====================

    /**
     * Gère l'interaction avec le fermier
     */
    private void handleFarmerInteraction(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);

        if (hasPlayerCompletedFire(player)) {
            player.sendMessage("");
            player.sendMessage("§6§lGérard: §f\"Merci encore pour ton aide, héros!\"");
            player.sendMessage("§6§lGérard: §f\"Mon moulin est sauvé grâce à toi.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.sendMessage("");
            return;
        }

        if (currentStep != JourneyStep.STEP_1_7) {
            player.sendMessage("");
            player.sendMessage("§6§lGérard: §f\"Bonjour voyageur!\"");
            player.sendMessage("§6§lGérard: §f\"Reviens me voir si tu as besoin d'aide.\"");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
            player.sendMessage("");
            return;
        }

        if (!playersIntroducedToFarmer.contains(player.getUniqueId())) {
            introducePlayerToFarmer(player);
            return;
        }

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
        playerExtinguishedFires.put(player.getUniqueId(), ConcurrentHashMap.newKeySet());

        Inventory gui = Bukkit.createInventory(null, 27, FIRE_GUI_TITLE);

        ItemStack wall = new ItemBuilder(Material.BROWN_STAINED_GLASS_PANE)
                .name("§8Mur du moulin")
                .build();

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, wall);
        }

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

        if (extinguished.contains(slot))
            return;

        boolean isFireSlot = false;
        for (int fireSlot : FIRE_SLOTS) {
            if (fireSlot == slot) {
                isFireSlot = true;
                break;
            }
        }
        if (!isFireSlot)
            return;

        extinguished.add(slot);

        ItemStack smoke = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("§7§l✓ Éteint!")
                .lore("§aFlamme éteinte")
                .build();
        gui.setItem(slot, smoke);

        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);

        int remaining = FIRE_COUNT - extinguished.size();

        if (remaining > 0) {
            player.sendMessage("§a✓ §7Flamme éteinte! §e" + remaining + " §7restante(s)");
        }

        if (extinguished.size() >= FIRE_COUNT) {
            onFireMinigameComplete(player);
        }
    }

    /**
     * Appelé quand le mini-jeu est complété
     */
    private void onFireMinigameComplete(Player player) {
        UUID uuid = player.getUniqueId();

        player.closeInventory();

        playersWhoCompletedFire.add(uuid);
        playersInFireMinigame.remove(uuid);
        playerExtinguishedFires.remove(uuid);

        journeyManager.setStepProgress(player, JourneyStep.STEP_1_7, 1);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);

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
