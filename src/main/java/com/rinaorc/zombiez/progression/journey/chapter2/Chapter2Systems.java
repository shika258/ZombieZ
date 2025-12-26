package com.rinaorc.zombiez.progression.journey.chapter2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableType;
import com.rinaorc.zombiez.zombies.ZombieManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;

/**
 * Gère tous les systèmes spécifiques au Chapitre 2:
 * - NPC Mineur Blessé (étape 4)
 * - Zombies Incendiés (étape 6)
 * - Récolte de Bois pour Igor (étape 7)
 * - Boss du Manoir (étape 10)
 */
public class Chapter2Systems implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;
    private final ProtocolManager protocolManager;

    // === CLÉS PDC ===
    private final NamespacedKey INJURED_MINER_KEY;
    private final NamespacedKey IGOR_NPC_KEY;
    private final NamespacedKey FIRE_ZOMBIE_KEY;
    private final NamespacedKey MANOR_BOSS_KEY;
    private final NamespacedKey BOSS_CONTRIBUTORS_KEY;
    private final NamespacedKey SUPPLY_CRATE_KEY;
    private final NamespacedKey CRATE_OWNER_KEY;

    // === POSITIONS ===
    private static final Location MINER_LOCATION = new Location(null, 1036.5, 82, 9627.5);
    private static final Location IGOR_LOCATION = new Location(null, 898.5, 90, 9469.5);
    private static final Location MANOR_BOSS_LOCATION = new Location(null, 728, 89, 9503);

    // Zone des zombies incendiés (crash de météore)
    private static final BoundingBox FIRE_ZOMBIE_ZONE = new BoundingBox(273, 70, 9449, 416, 103, 9550);

    // === TRACKING ===
    private Entity injuredMinerEntity;
    private Entity igorEntity;
    private Entity manorBossEntity;
    private final Map<UUID, List<Entity>> playerSupplyCrates = new ConcurrentHashMap<>(); // Caisses par joueur
    private final Set<UUID> bossContributors = ConcurrentHashMap.newKeySet();
    private boolean bossRespawnScheduled = false;

    // === VIRTUAL TEXTDISPLAY PER-PLAYER ===
    private final Map<UUID, Integer> playerMinerDisplayIds = new ConcurrentHashMap<>(); // Player UUID -> Virtual Entity ID
    private static int nextVirtualEntityId = -1000000; // IDs négatifs pour éviter conflits
    private static final double MINER_DISPLAY_HEIGHT = 2.3; // Hauteur au-dessus du mineur

    // === SUPPLY CRATES CONFIG ===
    private static final int SUPPLY_CRATE_COUNT = 5;
    private static final double CRATE_SPAWN_RADIUS_MIN = 15.0;
    private static final double CRATE_SPAWN_RADIUS_MAX = 40.0;

    // === BOSS DISPLAY ===
    private TextDisplay bossSpawnDisplay;
    private long bossRespawnTime = 0; // Timestamp du prochain respawn
    private static final int BOSS_RESPAWN_SECONDS = 60; // Temps de respawn en secondes
    private static final double BOSS_DISPLAY_HEIGHT = 5.0; // Hauteur au-dessus du spawn

    // Stats du boss pour l'affichage
    private static final String BOSS_NAME = "Seigneur du Manoir";
    private static final int BOSS_MAX_HP = 500;
    private static final int BOSS_DAMAGE = 15;
    private static final double BOSS_LEASH_RANGE = 32.0; // Distance max avant retour au spawn
    private static final double BOSS_LEASH_RANGE_SQUARED = BOSS_LEASH_RANGE * BOSS_LEASH_RANGE;

    // Trims disponibles pour randomisation (chargés depuis Registry)
    private static final List<TrimPattern> TRIM_PATTERNS = new ArrayList<>();
    private static final List<TrimMaterial> TRIM_MATERIALS = new ArrayList<>();

    static {
        // Charger les patterns depuis Registry
        String[] patterns = {"coast", "dune", "wild", "sentry", "vex", "rib", "snout", "tide", "ward", "eye"};
        for (String key : patterns) {
            try {
                TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(key));
                if (pattern != null) TRIM_PATTERNS.add(pattern);
            } catch (Exception ignored) {}
        }

        // Charger les matériaux depuis Registry
        String[] materials = {"copper", "iron", "gold", "redstone", "lapis", "amethyst", "diamond"};
        for (String key : materials) {
            try {
                TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(key));
                if (material != null) TRIM_MATERIALS.add(material);
            } catch (Exception ignored) {}
        }
    }

    public Chapter2Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        // Initialiser les clés PDC
        INJURED_MINER_KEY = new NamespacedKey(plugin, "injured_miner");
        IGOR_NPC_KEY = new NamespacedKey(plugin, "igor_npc");
        FIRE_ZOMBIE_KEY = new NamespacedKey(plugin, "fire_zombie");
        MANOR_BOSS_KEY = new NamespacedKey(plugin, "manor_boss");
        BOSS_CONTRIBUTORS_KEY = new NamespacedKey(plugin, "boss_contributors");
        SUPPLY_CRATE_KEY = new NamespacedKey(plugin, "supply_crate");
        CRATE_OWNER_KEY = new NamespacedKey(plugin, "crate_owner");

        // Enregistrer le listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Démarrer les spawns avec délai pour attendre le chargement du monde
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world != null) {
                    initializeNPCs(world);
                    startFireZombieSpawner(world);
                    initializeBossDisplay(world);
                    spawnManorBoss(world);
                    startBossDisplayUpdater();
                    startNPCNameUpdater(); // Mise à jour personnalisée des noms de NPC
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    // ==================== BOSS DISPLAY ====================

    /**
     * Initialise le TextDisplay au-dessus du spawn du boss
     */
    private void initializeBossDisplay(World world) {
        Location displayLoc = MANOR_BOSS_LOCATION.clone();
        displayLoc.setWorld(world);
        displayLoc.add(0.5, BOSS_DISPLAY_HEIGHT, 0.5);

        // Forcer le chargement du chunk pour garantir le spawn
        if (!displayLoc.getChunk().isLoaded()) {
            displayLoc.getChunk().load();
        }

        // Supprimer l'ancien display si existant
        if (bossSpawnDisplay != null && bossSpawnDisplay.isValid()) {
            bossSpawnDisplay.remove();
        }

        bossSpawnDisplay = world.spawn(displayLoc, TextDisplay.class, display -> {
            // Configuration de base
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(180, 0, 0, 0));

            // Scale x3
            display.setTransformation(new org.bukkit.util.Transformation(
                new org.joml.Vector3f(0, 0, 0),           // Translation
                new org.joml.AxisAngle4f(0, 0, 0, 1),    // Left rotation
                new org.joml.Vector3f(3f, 3f, 3f),        // Scale x3
                new org.joml.AxisAngle4f(0, 0, 0, 1)     // Right rotation
            ));

            // Distance de vue
            display.setViewRange(100f);

            // Texte initial
            updateBossDisplayText(display, true, 0);
        });
    }

    /**
     * Met à jour le texte du display selon l'état du boss
     * NOTE: Quand le boss est vivant, on CACHE le display statique pour éviter
     * le doublon avec le nom ZombieZ (qui affiche déjà les HP).
     * Le display statique n'apparaît que pour le countdown de respawn.
     */
    private void updateBossDisplayText(TextDisplay display, boolean bossAlive, int respawnSeconds) {
        if (display == null || !display.isValid()) return;

        if (bossAlive && manorBossEntity != null && manorBossEntity.isValid()) {
            // Boss vivant - CACHER le display (le système ZombieZ affiche déjà les HP)
            display.text(Component.empty());
        } else {
            // Boss mort - afficher countdown de respawn
            StringBuilder text = new StringBuilder();
            text.append("§4§l☠ ").append(BOSS_NAME).append(" §4§l☠\n");

            if (respawnSeconds > 0) {
                text.append("§e⏱ Respawn dans: §f").append(respawnSeconds).append("s");
            } else {
                text.append("§7En attente de spawn...");
            }

            display.text(Component.text(text.toString()));
        }
    }

    /**
     * Démarre la tâche de mise à jour du display (toutes les secondes)
     * Recrée le display et le boss s'ils sont invalides
     */
    private void startBossDisplayUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null) return;

                // Recréer le display s'il est invalide
                if (bossSpawnDisplay == null || !bossSpawnDisplay.isValid()) {
                    plugin.log(Level.INFO, "TextDisplay du boss invalide, recréation...");
                    initializeBossDisplay(world);
                }

                // Vérifier si le boss doit être respawné
                boolean bossAlive = manorBossEntity != null && manorBossEntity.isValid() && !manorBossEntity.isDead();
                int respawnSeconds = 0;

                if (!bossAlive) {
                    // Si pas de respawn programmé et pas de boss, spawn immédiatement
                    if (!bossRespawnScheduled && bossRespawnTime == 0) {
                        plugin.log(Level.INFO, "Boss du Manoir absent, spawn automatique...");
                        spawnManorBoss(world);
                        bossAlive = manorBossEntity != null && manorBossEntity.isValid();
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
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes
    }

    // ==================== NPC MINEUR BLESSÉ (ÉTAPE 4) ====================

    /**
     * Initialise les NPCs du chapitre 2
     */
    private void initializeNPCs(World world) {
        // Spawn le mineur blessé
        spawnInjuredMiner(world);
        // Spawn Igor
        spawnIgor(world);
    }

    /**
     * Fait spawn le mineur blessé
     */
    private void spawnInjuredMiner(World world) {
        Location loc = MINER_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (injuredMinerEntity != null && injuredMinerEntity.isValid()) {
            injuredMinerEntity.remove();
        }

        // Créer un villageois comme NPC
        Villager miner = world.spawn(loc, Villager.class, npc -> {
            // NE PAS mettre de customName visible - on utilise un TextDisplay virtuel per-player
            npc.setCustomNameVisible(false);
            npc.setProfession(Villager.Profession.TOOLSMITH);
            npc.setVillagerLevel(1);
            npc.setAI(false); // Immobile
            npc.setInvulnerable(true); // Invincible
            npc.setSilent(true);
            npc.setCollidable(false);

            // Équiper avec une pioche
            npc.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));

            // Marquer comme notre NPC
            npc.getPersistentDataContainer().set(INJURED_MINER_KEY, PersistentDataType.BYTE, (byte) 1);

            // Ajouter l'effet visuel de blessure (particules)
            npc.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false));
        });

        injuredMinerEntity = miner;

        // Particules de blessure périodiques
        new BukkitRunnable() {
            @Override
            public void run() {
                if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
                    cancel();
                    return;
                }
                Location particleLoc = injuredMinerEntity.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, particleLoc, 3, 0.3, 0.3, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Fait spawn Igor le survivant
     */
    private void spawnIgor(World world) {
        Location loc = IGOR_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (igorEntity != null && igorEntity.isValid()) {
            igorEntity.remove();
        }

        // Créer un villageois comme NPC
        Villager igor = world.spawn(loc, Villager.class, npc -> {
            npc.setCustomName("§6§lIgor le Survivant");
            npc.setCustomNameVisible(true);
            npc.setProfession(Villager.Profession.MASON);
            npc.setVillagerLevel(3);
            npc.setAI(false);
            npc.setInvulnerable(true);
            npc.setSilent(true);
            npc.setCollidable(false);

            // Équiper avec une hache
            npc.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));

            // Marquer comme notre NPC
            npc.getPersistentDataContainer().set(IGOR_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        igorEntity = igor;
    }

    /**
     * Gère l'interaction avec le mineur blessé (utiliser un bandage)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractMiner(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Vérifier si c'est le mineur blessé
        if (!entity.getPersistentDataContainer().has(INJURED_MINER_KEY, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);

        // Vérifier si le joueur est à l'étape 4 du chapitre 2
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_4) {
            player.sendMessage("§7Le mineur te regarde avec gratitude...");
            player.sendMessage("§8(Tu as déjà aidé ce pauvre homme)");
            return;
        }

        // Vérifier si le joueur a un bandage dans la main
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!isBandage(handItem)) {
            player.sendMessage("");
            player.sendMessage("§c§l⚠ §eLe mineur a besoin d'un bandage!");
            player.sendMessage("§7Utilise un §fbandage §7sur lui pour le soigner.");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Consommer le bandage
        handItem.setAmount(handItem.getAmount() - 1);

        // Animation de soin
        Location loc = entity.getLocation();
        player.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0);
        player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Message de remerciement
        player.sendMessage("");
        player.sendMessage("§a§l✓ §eLe mineur blessé: §f\"Merci, survivant! Tu m'as sauvé la vie!\"");
        player.sendMessage("§7Il te tend une vieille carte de la zone...");
        player.sendMessage("");

        // Valider l'étape
        journeyManager.updateProgress(player, JourneyStep.StepType.HEAL_NPC, 1);

        // Mettre à jour immédiatement l'hologramme de CE joueur pour afficher "Soigné"
        // Le display est virtuel et per-player, donc les autres joueurs ne voient pas le changement
        if (entity instanceof Villager villager) {
            villager.removePotionEffect(PotionEffectType.SLOWNESS);
            // Mettre à jour le TextDisplay virtuel immédiatement
            updateVirtualDisplayText(player, true); // true = healed
        }
    }

    /**
     * Vérifie si un item est un bandage (utilise le système Consumable)
     */
    private boolean isBandage(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta()) return false;

        // Vérifier via le PDC du système de consommables
        var meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Utiliser la clé du système Consumable
        if (pdc.has(Consumable.CONSUMABLE_KEY, PersistentDataType.STRING)) {
            String typeStr = pdc.get(Consumable.CONSUMABLE_KEY, PersistentDataType.STRING);
            return ConsumableType.BANDAGE.name().equals(typeStr);
        }

        return false;
    }

    // ==================== IGOR ET CAISSES DE RAVITAILLEMENT (ÉTAPE 7) ====================

    /**
     * Gère l'interaction avec Igor pour afficher la progression
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractIgor(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (!entity.getPersistentDataContainer().has(IGOR_NPC_KEY, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);

        // Vérifier si le joueur est à l'étape 7 du chapitre 2
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_7) {
            player.sendMessage("§6§lIgor: §f\"Merci pour ton aide, survivant!\"");
            return;
        }

        int currentProgress = journeyManager.getStepProgress(player, currentStep);

        if (currentProgress >= SUPPLY_CRATE_COUNT) {
            player.sendMessage("");
            player.sendMessage("§6§lIgor: §f\"Merci infiniment! Grâce à ces ravitaillements, je peux tenir!\"");
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
            player.sendMessage("");
            return;
        }

        // Vérifier si les caisses ont été spawnées pour ce joueur
        List<Entity> crates = playerSupplyCrates.get(player.getUniqueId());
        if (crates == null || crates.isEmpty() || crates.stream().noneMatch(Entity::isValid)) {
            // Spawner de nouvelles caisses
            spawnSupplyCratesForPlayer(player);
            player.sendMessage("");
            player.sendMessage("§6§lIgor: §f\"J'ai besoin de récupérer des caisses de ravitaillement!\"");
            player.sendMessage("§7Des §ecaisses lumineuses §7sont apparues autour de moi.");
            player.sendMessage("§7Trouve-les et §eclique dessus §7pour les récupérer!");
            player.sendMessage("§7Progression: §e" + currentProgress + "§7/§e" + SUPPLY_CRATE_COUNT + " §7caisses");
            player.sendMessage("");
        } else {
            long remainingCrates = crates.stream().filter(Entity::isValid).count();
            player.sendMessage("");
            player.sendMessage("§6§lIgor: §f\"Il reste encore §e" + remainingCrates + " §fcaisse(s) à récupérer!\"");
            player.sendMessage("§7Progression: §e" + currentProgress + "§7/§e" + SUPPLY_CRATE_COUNT + " §7caisses");
            player.sendMessage("§8(Cherche les caisses lumineuses autour de moi)");
            player.sendMessage("");
        }
    }

    /**
     * Spawn des caisses de ravitaillement autour d'Igor pour un joueur
     * Chaque joueur a ses propres caisses (visibles par tous mais collectables par le propriétaire)
     * Utilise ItemDisplay (visuel) + Interaction (cliquable) pour chaque caisse
     */
    public void spawnSupplyCratesForPlayer(Player player) {
        World world = player.getWorld();
        Location igorLoc = IGOR_LOCATION.clone();
        igorLoc.setWorld(world);

        // Nettoyer les anciennes caisses du joueur
        cleanupPlayerCrates(player.getUniqueId());

        List<Entity> crates = new ArrayList<>();
        int currentProgress = journeyManager.getStepProgress(player, JourneyStep.STEP_2_7);
        int cratesToSpawn = SUPPLY_CRATE_COUNT - currentProgress;

        for (int i = 0; i < cratesToSpawn; i++) {
            // Position aléatoire autour d'Igor
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = CRATE_SPAWN_RADIUS_MIN + ThreadLocalRandom.current().nextDouble() * (CRATE_SPAWN_RADIUS_MAX - CRATE_SPAWN_RADIUS_MIN);
            double x = igorLoc.getX() + Math.cos(angle) * distance;
            double z = igorLoc.getZ() + Math.sin(angle) * distance;
            double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            Location spawnLoc = new Location(world, x, y, z);

            // 1. Créer le VISUEL (ItemDisplay) - ne peut pas être cliqué
            ItemDisplay visual = world.spawn(spawnLoc, ItemDisplay.class, display -> {
                // Item affiché: Chest
                display.setItemStack(new ItemStack(Material.CHEST));

                // Taille x1.5 pour visibilité
                display.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0.5f, 0),        // Translation (légèrement au-dessus du sol)
                    new org.joml.AxisAngle4f(0, 0, 1, 0),     // Left rotation
                    new org.joml.Vector3f(1.5f, 1.5f, 1.5f),  // Scale x1.5
                    new org.joml.AxisAngle4f(0, 0, 1, 0)      // Right rotation
                ));

                // FIXED pour que la caisse ne tourne pas (plus réaliste)
                display.setBillboard(Display.Billboard.FIXED);

                // Glow effect pour visibilité
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 200, 50)); // Jaune/Or

                // Distance de vue
                display.setViewRange(64f);
            });

            // 2. Créer l'entité INTERACTION (invisible mais cliquable)
            Interaction hitbox = world.spawn(spawnLoc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
                // Taille de la hitbox (largeur et hauteur)
                interaction.setInteractionWidth(1.5f);
                interaction.setInteractionHeight(1.5f);

                // Marquer comme caisse de ravitaillement
                PersistentDataContainer pdc = interaction.getPersistentDataContainer();
                pdc.set(SUPPLY_CRATE_KEY, PersistentDataType.BYTE, (byte) 1);
                pdc.set(CRATE_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

                // Lier au visuel pour le supprimer ensemble
                pdc.set(new NamespacedKey(plugin, "visual_uuid"), PersistentDataType.STRING, visual.getUniqueId().toString());
            });

            // Stocker les deux entités (on gère la hitbox principalement)
            crates.add(hitbox);
            crates.add(visual);

            // Particules de spawn
            world.spawnParticle(Particle.END_ROD, spawnLoc.clone().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
        }

        playerSupplyCrates.put(player.getUniqueId(), crates);

        // Son d'apparition
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
    }

    /**
     * Gère l'interaction avec une caisse de ravitaillement (via entité Interaction)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSupplyCrateInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Vérifier si c'est une caisse de ravitaillement (entité Interaction)
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(SUPPLY_CRATE_KEY, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);

        // Vérifier le propriétaire
        String ownerUuid = pdc.get(CRATE_OWNER_KEY, PersistentDataType.STRING);
        if (ownerUuid == null || !ownerUuid.equals(player.getUniqueId().toString())) {
            player.sendMessage("§c§l✗ §7Cette caisse n'est pas pour toi!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Vérifier si le joueur est à l'étape 7
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_7) {
            player.sendMessage("§7Cette caisse ne t'est plus utile.");
            return;
        }

        // Collecter la caisse
        Location crateLoc = entity.getLocation();

        // Effets visuels et sonores
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, crateLoc.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0);
        player.getWorld().spawnParticle(Particle.POOF, crateLoc.clone().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
        player.playSound(crateLoc, Sound.ENTITY_ITEM_PICKUP, 1f, 1.2f);
        player.playSound(crateLoc, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.3f);

        // Supprimer l'entité visuelle liée (ItemDisplay)
        String visualUuidStr = pdc.get(new NamespacedKey(plugin, "visual_uuid"), PersistentDataType.STRING);
        if (visualUuidStr != null) {
            try {
                UUID visualUuid = UUID.fromString(visualUuidStr);
                Entity visualEntity = Bukkit.getEntity(visualUuid);
                if (visualEntity != null && visualEntity.isValid()) {
                    visualEntity.remove();

                    // Retirer de la liste
                    List<Entity> crates = playerSupplyCrates.get(player.getUniqueId());
                    if (crates != null) {
                        crates.remove(visualEntity);
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        // Supprimer l'entité d'interaction
        entity.remove();

        // Nettoyer de la liste
        List<Entity> crates = playerSupplyCrates.get(player.getUniqueId());
        if (crates != null) {
            crates.remove(entity);
        }

        // Mettre à jour la progression
        int progress = journeyManager.getStepProgress(player, currentStep);
        journeyManager.updateProgress(player, JourneyStep.StepType.COLLECT_SUPPLY_CRATES, progress + 1);

        // Feedback
        int newProgress = progress + 1;
        player.sendActionBar(Component.text("§a+1 §eCaisse récupérée §7(" + newProgress + "/" + SUPPLY_CRATE_COUNT + ")"));

        if (newProgress >= SUPPLY_CRATE_COUNT) {
            player.sendMessage("");
            player.sendMessage("§a§l✓ §6Toutes les caisses récupérées!");
            player.sendMessage("§7Retourne voir §eIgor §7pour terminer la quête.");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    /**
     * Nettoie les caisses d'un joueur spécifique
     */
    private void cleanupPlayerCrates(UUID playerUuid) {
        List<Entity> crates = playerSupplyCrates.remove(playerUuid);
        if (crates != null) {
            for (Entity crate : crates) {
                if (crate != null && crate.isValid()) {
                    crate.remove();
                }
            }
        }
    }

    /**
     * Nettoie toutes les caisses de ravitaillement (appelé au reload/disable)
     */
    public void cleanupAllSupplyCrates() {
        for (List<Entity> crates : playerSupplyCrates.values()) {
            for (Entity crate : crates) {
                if (crate != null && crate.isValid()) {
                    crate.remove();
                }
            }
        }
        playerSupplyCrates.clear();
    }

    // ==================== ZOMBIES INCENDIÉS (ÉTAPE 6) ====================

    /**
     * Lance le spawner de zombies incendiés dans la zone du météore
     */
    private void startFireZombieSpawner(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier s'il y a des joueurs dans ou près de la zone
                boolean playersNearby = false;
                for (Player player : world.getPlayers()) {
                    if (isNearFireZombieZone(player.getLocation())) {
                        playersNearby = true;
                        break;
                    }
                }

                if (!playersNearby) return;

                // Compter les zombies incendiés existants
                long fireZombieCount = world.getEntitiesByClass(Zombie.class).stream()
                    .filter(z -> z.getPersistentDataContainer().has(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE))
                    .count();

                // Limiter à 15 zombies max
                if (fireZombieCount >= 15) return;

                // Spawn 1-3 zombies
                int toSpawn = ThreadLocalRandom.current().nextInt(1, 4);
                for (int i = 0; i < toSpawn && fireZombieCount + i < 15; i++) {
                    spawnFireZombie(world);
                }
            }
        }.runTaskTimer(plugin, 200L, 40L); // Toutes les 2 secondes (spawn rate x2.5)
    }

    private boolean isNearFireZombieZone(Location loc) {
        return loc.getX() >= FIRE_ZOMBIE_ZONE.getMinX() - 50 &&
               loc.getX() <= FIRE_ZOMBIE_ZONE.getMaxX() + 50 &&
               loc.getZ() >= FIRE_ZOMBIE_ZONE.getMinZ() - 50 &&
               loc.getZ() <= FIRE_ZOMBIE_ZONE.getMaxZ() + 50;
    }

    /**
     * Fait spawn un zombie incendié custom ZombieZ avec IA, nom dynamique et stats
     */
    private void spawnFireZombie(World world) {
        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        // Position aléatoire dans la zone
        double x = ThreadLocalRandom.current().nextDouble(FIRE_ZOMBIE_ZONE.getMinX(), FIRE_ZOMBIE_ZONE.getMaxX());
        double z = ThreadLocalRandom.current().nextDouble(FIRE_ZOMBIE_ZONE.getMinZ(), FIRE_ZOMBIE_ZONE.getMaxZ());
        double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

        Location spawnLoc = new Location(world, x, y, z);

        // Niveau aléatoire 3-7 pour la zone 2
        int level = ThreadLocalRandom.current().nextInt(3, 8);

        // Spawn via ZombieManager (avec IA, nom dynamique, stats, etc.)
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.FIRE_ZOMBIE, spawnLoc, level);

        if (activeZombie != null) {
            // Récupérer l'entité pour appliquer les effets visuels de feu
            Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
            if (entity instanceof Zombie zombie) {
                // Équiper avec armure de cuir rouge + trims
                zombie.getEquipment().setHelmet(createFireZombieArmor(Material.LEATHER_HELMET));
                zombie.getEquipment().setChestplate(createFireZombieArmor(Material.LEATHER_CHESTPLATE));
                zombie.getEquipment().setLeggings(createFireZombieArmor(Material.LEATHER_LEGGINGS));
                zombie.getEquipment().setBoots(createFireZombieArmor(Material.LEATHER_BOOTS));

                // Pas de drop d'armure
                zombie.getEquipment().setHelmetDropChance(0);
                zombie.getEquipment().setChestplateDropChance(0);
                zombie.getEquipment().setLeggingsDropChance(0);
                zombie.getEquipment().setBootsDropChance(0);

                // Toujours en feu visuellement
                zombie.setVisualFire(true);
                zombie.setFireTicks(Integer.MAX_VALUE);

                // Marquer comme zombie incendié pour le tracking Journey
                zombie.getPersistentDataContainer().set(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE, (byte) 1);

                // Effet de spawn minimal (le visuel principal vient de setVisualFire)
                world.spawnParticle(Particle.FLAME, spawnLoc.clone().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    /**
     * Crée une pièce d'armure de cuir rouge avec trim aléatoire
     */
    private ItemStack createFireZombieArmor(Material armorType) {
        ItemStack armor = new ItemStack(armorType);

        if (armor.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            // Couleur rouge feu
            leatherMeta.setColor(Color.fromRGB(180, 30, 30));

            // Ajouter un trim aléatoire (50% de chance)
            if (ThreadLocalRandom.current().nextBoolean() && !TRIM_PATTERNS.isEmpty() && !TRIM_MATERIALS.isEmpty()) {
                TrimPattern pattern = TRIM_PATTERNS.get(ThreadLocalRandom.current().nextInt(TRIM_PATTERNS.size()));
                TrimMaterial material = TRIM_MATERIALS.get(ThreadLocalRandom.current().nextInt(TRIM_MATERIALS.size()));

                if (leatherMeta instanceof ArmorMeta armorMeta) {
                    armorMeta.setTrim(new ArmorTrim(material, pattern));
                }
            }

            armor.setItemMeta(leatherMeta);
        }

        return armor;
    }

    /**
     * Gère la mort d'un zombie incendié
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombie.getPersistentDataContainer().has(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE)) return;

        Player killer = zombie.getKiller();
        if (killer == null) return;

        // Vérifier si le joueur est à l'étape 6
        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_6) return;

        // Mettre à jour la progression
        int progress = journeyManager.getStepProgress(killer, currentStep);
        journeyManager.updateProgress(killer, JourneyStep.StepType.FIRE_ZOMBIE_KILLS, progress + 1);

        // Effets
        killer.playSound(killer.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.5f, 1.5f);
    }

    // ==================== BOSS DU MANOIR (ÉTAPE 10) ====================

    /**
     * Fait spawn le boss du manoir via le système ZombieZ
     * Utilise ZombieType.MANOR_LORD avec IA JourneyBossAI
     */
    private void spawnManorBoss(World world) {
        // Protection anti-spawn multiple: si le boss existe déjà et est valide, ne pas en créer un nouveau
        if (manorBossEntity != null && manorBossEntity.isValid() && !manorBossEntity.isDead()) {
            plugin.log(Level.FINE, "Boss du Manoir déjà présent, spawn ignoré");
            return;
        }

        Location loc = MANOR_BOSS_LOCATION.clone();
        loc.setWorld(world);

        // Forcer le chargement du chunk pour garantir le spawn
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }

        // Nettoyer l'ancien boss s'il existe mais est invalide
        if (manorBossEntity != null) {
            manorBossEntity.remove();
        }

        // Nettoyer les contributeurs
        bossContributors.clear();
        bossRespawnScheduled = false;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(Level.WARNING, "ZombieManager non disponible, spawn du boss annulé");
            return;
        }

        // Spawn via ZombieManager (avec IA JourneyBossAI, display name dynamique, système de dégâts ZombieZ)
        int bossLevel = 10; // Niveau du boss pour le chapitre 2
        ZombieManager.ActiveZombie activeZombie = zombieManager.spawnZombie(ZombieType.MANOR_LORD, loc, bossLevel);

        if (activeZombie == null) {
            plugin.log(Level.WARNING, "Échec du spawn du boss du Manoir via ZombieManager");
            return;
        }

        // Récupérer l'entité pour appliquer les modifications visuelles
        Entity entity = plugin.getServer().getEntity(activeZombie.getEntityId());
        if (!(entity instanceof Zombie boss)) {
            plugin.log(Level.WARNING, "Boss du Manoir n'est pas un Zombie valide");
            return;
        }

        manorBossEntity = boss;

        // Appliquer les modifications visuelles spécifiques au boss du Manoir
        applyManorBossVisuals(boss);

        // Marquer comme boss du manoir pour le tracking Journey
        boss.getPersistentDataContainer().set(MANOR_BOSS_KEY, PersistentDataType.BYTE, (byte) 1);

        // Annoncer le spawn
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(loc) < 100) {
                player.sendMessage("");
                player.sendMessage("§4§l☠ Le Seigneur du Manoir a émergé des ténèbres!");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
            }
        }

        plugin.log(Level.INFO, "§c§lBoss du Manoir spawné avec succès (système ZombieZ)");
    }

    /**
     * Applique les modifications visuelles au boss du Manoir
     * (Scale x3, équipement netherite, effets visuels)
     */
    private void applyManorBossVisuals(Zombie boss) {
        // Scale x3 via Paper API
        var scale = boss.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(2.5);
        }

        // Équipement netherite épique
        boss.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        boss.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        boss.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        boss.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        boss.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));

        // Pas de drop d'équipement
        boss.getEquipment().setHelmetDropChance(0);
        boss.getEquipment().setChestplateDropChance(0);
        boss.getEquipment().setLeggingsDropChance(0);
        boss.getEquipment().setBootsDropChance(0);
        boss.getEquipment().setItemInMainHandDropChance(0);

        // Effets visuels
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, true));
        boss.setGlowing(true);
    }

    /**
     * Tracker les joueurs qui attaquent le boss
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie boss)) return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE)) return;

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
     * Empêcher le boss de cibler autre chose que les joueurs
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie boss)) return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE)) return;

        // Ne cibler que les joueurs
        if (!(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Gère la mort du boss du manoir
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onManorBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie boss)) return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE)) return;

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
                if (currentStep != null && currentStep == JourneyStep.STEP_2_10) {
                    journeyManager.updateProgress(contributor, JourneyStep.StepType.KILL_MANOR_BOSS, 1);

                    contributor.sendMessage("");
                    contributor.sendMessage("§6§l✦ §4Le Seigneur du Manoir a été vaincu!");
                    contributor.sendMessage("§7Tu as contribué à sa défaite.");
                    contributor.sendMessage("");
                }
            }
        }

        // Nettoyer
        bossContributors.clear();
        manorBossEntity = null;

        // Programmer le respawn (1 minute)
        if (!bossRespawnScheduled) {
            bossRespawnScheduled = true;
            bossRespawnTime = System.currentTimeMillis() + (BOSS_RESPAWN_SECONDS * 1000L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnManorBoss(world);
                    bossRespawnTime = 0;
                }
            }.runTaskLater(plugin, 20L * BOSS_RESPAWN_SECONDS);

            // Annoncer le respawn
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(deathLoc) < 100) {
                    player.sendMessage("§8Le Seigneur du Manoir reviendra dans §c" + BOSS_RESPAWN_SECONDS + " secondes§8...");
                }
            }
        }
    }

    // ==================== UTILITAIRES ====================

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private boolean isOnCooldown(UUID uuid, String action) {
        String key = uuid.toString() + "_" + action;
        Long lastTime = cooldowns.get(key);
        return lastTime != null && System.currentTimeMillis() - lastTime < 500;
    }

    private void setCooldown(UUID uuid, String action, long ms) {
        String key = uuid.toString() + "_" + action;
        cooldowns.put(key, System.currentTimeMillis());
    }

    // ==================== TEXTDISPLAY VIRTUEL PER-PLAYER (MINEUR) ====================

    /**
     * Démarre le système de TextDisplay virtuel per-player.
     * Chaque joueur voit son propre hologramme au-dessus du mineur
     * selon sa progression de quête.
     */
    private void startNPCNameUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
                    // Détruire tous les displays virtuels si le mineur n'existe plus
                    destroyAllVirtualDisplays();
                    return;
                }

                Location minerLoc = injuredMinerEntity.getLocation();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    boolean inRange = player.getWorld().equals(minerLoc.getWorld()) &&
                                      player.getLocation().distanceSquared(minerLoc) <= 50 * 50;

                    Integer existingDisplayId = playerMinerDisplayIds.get(playerId);

                    if (inRange) {
                        // Joueur à portée: créer ou mettre à jour le display
                        boolean hasHealed = hasPlayerHealedMiner(player);
                        if (existingDisplayId == null) {
                            // Créer un nouveau display virtuel
                            spawnVirtualTextDisplay(player, hasHealed);
                        } else {
                            // Mettre à jour la position du display existant
                            updateVirtualDisplayPosition(player, existingDisplayId);
                        }
                    } else if (existingDisplayId != null) {
                        // Joueur hors de portée: détruire son display
                        destroyVirtualDisplay(player, existingDisplayId);
                        playerMinerDisplayIds.remove(playerId);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 5L); // Toutes les 5 ticks (position fluide)
    }

    /**
     * Vérifie si le joueur a déjà soigné le mineur (étape 4 du chapitre 2 complétée ou dépassée)
     */
    private boolean hasPlayerHealedMiner(Player player) {
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null) return false;

        // Si le joueur est au chapitre 2 étape 5 ou plus, ou dans un chapitre supérieur
        if (currentStep.getChapter().getId() > 2) return true;
        if (currentStep.getChapter().getId() == 2 && currentStep.getStepNumber() > 4) return true;

        // Si le joueur est exactement à l'étape 4, vérifier la progression
        if (currentStep == JourneyStep.STEP_2_4) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            return progress >= 1; // 1 = a soigné le mineur
        }

        return false;
    }

    /**
     * Génère un ID d'entité virtuelle unique (négatif pour éviter les conflits)
     */
    private synchronized int generateVirtualEntityId() {
        return nextVirtualEntityId--;
    }

    /**
     * Spawn un TextDisplay virtuel pour un joueur spécifique
     */
    private void spawnVirtualTextDisplay(Player player, boolean healed) {
        if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) return;

        int entityId = generateVirtualEntityId();
        playerMinerDisplayIds.put(player.getUniqueId(), entityId);

        Location displayLoc = injuredMinerEntity.getLocation().clone().add(0, MINER_DISPLAY_HEIGHT, 0);

        try {
            // 1. Envoyer le packet de spawn de l'entité TextDisplay
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

            spawnPacket.getIntegers().write(0, entityId);  // Entity ID
            spawnPacket.getUUIDs().write(0, UUID.randomUUID()); // Entity UUID
            spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY); // Type

            // Position
            spawnPacket.getDoubles().write(0, displayLoc.getX());
            spawnPacket.getDoubles().write(1, displayLoc.getY());
            spawnPacket.getDoubles().write(2, displayLoc.getZ());

            // Rotation (0)
            spawnPacket.getBytes().write(0, (byte) 0); // Yaw
            spawnPacket.getBytes().write(1, (byte) 0); // Pitch
            spawnPacket.getBytes().write(2, (byte) 0); // Head yaw

            // Data (0 pour TextDisplay)
            spawnPacket.getIntegers().write(1, 0);

            // Velocity (0)
            spawnPacket.getShorts().write(0, (short) 0);
            spawnPacket.getShorts().write(1, (short) 0);
            spawnPacket.getShorts().write(2, (short) 0);

            protocolManager.sendServerPacket(player, spawnPacket);

            // 2. Envoyer les métadonnées du TextDisplay
            sendVirtualDisplayMetadata(player, entityId, healed);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "Erreur spawn TextDisplay virtuel: " + e.getMessage());
            playerMinerDisplayIds.remove(player.getUniqueId());
        }
    }

    /**
     * Envoie les métadonnées du TextDisplay virtuel (texte, style, etc.)
     */
    private void sendVirtualDisplayMetadata(Player player, int entityId, boolean healed) {
        try {
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            // Index 0: Entity flags (byte) - pas de flags spéciaux
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));

            // Index 8: Billboard constraint (byte) - CENTER = 3
            dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 3));

            // Index 23: Text (Component) - Le texte affiché
            String text = healed
                    ? "§a§l✓ §7Mineur (Soigné)"
                    : "§c§l❤ §eMineur Blessé §c§l❤";

            Component textComponent = Component.text(text);
            String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
            WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);

            var chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(false);
            dataValues.add(new WrappedDataValue(23, chatSerializer, wrappedText.getHandle()));

            // Index 25: Background color (int) - Noir semi-transparent (ARGB)
            dataValues.add(new WrappedDataValue(25, WrappedDataWatcher.Registry.get(Integer.class), 0x40000000));

            // Index 27: See through (byte) - 0 = false
            dataValues.add(new WrappedDataValue(27, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));

            // Écrire les métadonnées
            var dataValueModifier = metadataPacket.getDataValueCollectionModifier();
            if (dataValueModifier.size() > 0) {
                dataValueModifier.write(0, dataValues);
                protocolManager.sendServerPacket(player, metadataPacket);
            }

        } catch (Exception e) {
            plugin.log(Level.WARNING, "Erreur envoi métadonnées TextDisplay: " + e.getMessage());
        }
    }

    /**
     * Met à jour la position d'un TextDisplay virtuel
     */
    private void updateVirtualDisplayPosition(Player player, int entityId) {
        if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) return;

        Location displayLoc = injuredMinerEntity.getLocation().clone().add(0, MINER_DISPLAY_HEIGHT, 0);

        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, displayLoc.getX());
            teleportPacket.getDoubles().write(1, displayLoc.getY());
            teleportPacket.getDoubles().write(2, displayLoc.getZ());
            teleportPacket.getBytes().write(0, (byte) 0); // Yaw
            teleportPacket.getBytes().write(1, (byte) 0); // Pitch
            teleportPacket.getBooleans().write(0, false); // On ground

            protocolManager.sendServerPacket(player, teleportPacket);

        } catch (Exception e) {
            plugin.log(Level.FINE, "Erreur téléport TextDisplay: " + e.getMessage());
        }
    }

    /**
     * Met à jour immédiatement le texte d'un TextDisplay virtuel pour un joueur
     * (appelé quand le joueur soigne le mineur)
     */
    private void updateVirtualDisplayText(Player player, boolean healed) {
        Integer entityId = playerMinerDisplayIds.get(player.getUniqueId());
        if (entityId == null) {
            // Pas de display existant, en créer un nouveau
            spawnVirtualTextDisplay(player, healed);
        } else {
            // Mettre à jour le texte du display existant
            sendVirtualDisplayMetadata(player, entityId, healed);
        }
    }

    /**
     * Détruit un TextDisplay virtuel pour un joueur
     */
    private void destroyVirtualDisplay(Player player, int entityId) {
        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            // En 1.21.4, ENTITY_DESTROY utilise une IntList
            destroyPacket.getIntLists().write(0, List.of(entityId));

            protocolManager.sendServerPacket(player, destroyPacket);

        } catch (Exception e) {
            plugin.log(Level.FINE, "Erreur destruction TextDisplay: " + e.getMessage());
        }
    }

    /**
     * Détruit tous les TextDisplays virtuels (nettoyage)
     */
    private void destroyAllVirtualDisplays() {
        for (Map.Entry<UUID, Integer> entry : playerMinerDisplayIds.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                destroyVirtualDisplay(player, entry.getValue());
            }
        }
        playerMinerDisplayIds.clear();
    }

    /**
     * Nettoie le TextDisplay virtuel d'un joueur qui se déconnecte
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Integer displayId = playerMinerDisplayIds.remove(playerId);
        // Pas besoin d'envoyer de packet de destruction, le joueur se déconnecte
        // On enlève juste l'entrée de la map

        // Nettoyer aussi les caisses de ravitaillement du joueur
        cleanupPlayerCrates(playerId);
    }

    /**
     * Nettoie les ressources lors de la désactivation du plugin
     */
    public void cleanup() {
        // Détruire les TextDisplays virtuels per-player
        destroyAllVirtualDisplays();

        if (injuredMinerEntity != null) injuredMinerEntity.remove();
        if (igorEntity != null) igorEntity.remove();
        if (manorBossEntity != null) manorBossEntity.remove();
        if (bossSpawnDisplay != null) bossSpawnDisplay.remove();
        cleanupAllSupplyCrates(); // Nettoyer toutes les caisses de ravitaillement
    }
}
