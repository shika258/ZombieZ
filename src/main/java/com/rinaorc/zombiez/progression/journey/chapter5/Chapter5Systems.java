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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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

    public Chapter5Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        // Initialiser les clés PDC
        this.QUEST_SALMON_KEY = new NamespacedKey(plugin, "quest_salmon_ch5");
        this.ORE_VISUAL_KEY = new NamespacedKey(plugin, "quest_ore_visual_ch5");
        this.ORE_HITBOX_KEY = new NamespacedKey(plugin, "quest_ore_hitbox_ch5");

        // Enregistrer les événements
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Démarrer les systèmes
        startSalmonSpawnTask();

        // Initialiser les minerais après un délai (attendre que le monde soit chargé)
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    initializeOres(world);
                    startOreVisibilityUpdater();
                    startOreRespawnChecker();
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

                for (UUID playerId : activeSalmonPlayers) {
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    if (!isPlayerNearSalmonZone(player)) {
                        continue;
                    }

                    int currentSalmons = countActiveSalmons();
                    if (currentSalmons >= MAX_SALMONS_IN_ZONE) {
                        continue;
                    }

                    spawnSalmon(player.getWorld());
                }
            }
        }.runTaskTimer(plugin, 20L, SPAWN_INTERVAL_TICKS);
    }

    private boolean isPlayerNearSalmonZone(Player player) {
        Location loc = player.getLocation();
        double distanceSquared = Math.pow(loc.getX() - SALMON_ZONE_CENTER_X, 2) +
                                 Math.pow(loc.getZ() - SALMON_ZONE_CENTER_Z, 2);
        return distanceSquared < 10000;
    }

    private int countActiveSalmons() {
        int count = 0;
        Iterator<UUID> it = activeSalmons.keySet().iterator();
        while (it.hasNext()) {
            UUID salmonId = it.next();
            Entity entity = plugin.getServer().getEntity(salmonId);
            if (entity == null || !entity.isValid() || entity.isDead()) {
                it.remove();
            } else {
                count++;
            }
        }
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

        int remaining = 12 - newProgress;
        if (remaining > 0) {
            killer.sendTitle(
                "§a\u2713 §fSaumon Pêché!",
                "§7" + newProgress + "/12 - Plus que §c" + remaining + " §7saumons!",
                5, 30, 10
            );
        }

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

        // Message de progression
        int remaining = ORES_TO_MINE - newProgress;
        if (remaining > 0) {
            player.sendTitle(
                "§a⛏ " + oreType.displayName + " §aExtrait!",
                "§7" + newProgress + "/" + ORES_TO_MINE + " - Plus que §c" + remaining + " §7minerais!",
                5, 30, 10
            );
        }

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

        plugin.getLogger().info("[Chapter5Systems] Cleanup effectué");
    }
}
