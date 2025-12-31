package com.rinaorc.zombiez.progression.journey.chapter2;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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

    // === TEXTDISPLAY PER-PLAYER (visibilité via Paper API) ===
    // Deux TextDisplays fixes: un pour "Blessé", un pour "Soigné"
    // La visibilité est gérée per-player via showEntity/hideEntity
    private TextDisplay minerDisplayInjured; // Visible par ceux qui n'ont pas soigné
    private TextDisplay minerDisplayHealed; // Visible par ceux qui ont soigné
    private final Set<UUID> playersWhoHealedMiner = ConcurrentHashMap.newKeySet(); // Joueurs ayant soigné (en plus de
                                                                                   // la progression)
    private static final double MINER_DISPLAY_HEIGHT = 2.3; // Hauteur au-dessus du mineur
    private static final double MINER_VIEW_DISTANCE = 50.0; // Distance max pour voir l'hologramme

    // === SUPPLY CRATES CONFIG ===
    private static final int SUPPLY_CRATE_COUNT = 5;
    private static final double CRATE_SPAWN_RADIUS_MIN = 15.0;
    private static final double CRATE_SPAWN_RADIUS_MAX = 40.0;

    // === BOSS DISPLAY ===
    private TextDisplay bossSpawnDisplay;
    private long bossRespawnTime = 0; // Timestamp du prochain respawn
    private static final int BOSS_RESPAWN_SECONDS = 60; // Temps de respawn en secondes
    private static final double BOSS_DISPLAY_HEIGHT = 5.0; // Hauteur au-dessus du spawn

    // Config du boss (stats gérées par ZombieType.MANOR_LORD +
    // calculateHealth/Damage)
    private static final String BOSS_NAME = "Seigneur du Manoir";
    private static final double BOSS_LEASH_RANGE = 32.0; // Distance max avant retour au spawn
    private static final double BOSS_LEASH_RANGE_SQUARED = BOSS_LEASH_RANGE * BOSS_LEASH_RANGE;

    // Trims disponibles pour randomisation (chargés depuis Registry)
    private static final List<TrimPattern> TRIM_PATTERNS = new ArrayList<>();
    private static final List<TrimMaterial> TRIM_MATERIALS = new ArrayList<>();

    static {
        // Charger les patterns depuis Registry
        String[] patterns = { "coast", "dune", "wild", "sentry", "vex", "rib", "snout", "tide", "ward", "eye" };
        for (String key : patterns) {
            try {
                TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(key));
                if (pattern != null)
                    TRIM_PATTERNS.add(pattern);
            } catch (Exception ignored) {
            }
        }

        // Charger les matériaux depuis Registry
        String[] materials = { "copper", "iron", "gold", "redstone", "lapis", "amethyst", "diamond" };
        for (String key : materials) {
            try {
                TrimMaterial material = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(key));
                if (material != null)
                    TRIM_MATERIALS.add(material);
            } catch (Exception ignored) {
            }
        }
    }

    public Chapter2Systems(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

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
            if (entity instanceof TextDisplay td && entity.getScoreboardTags().contains("manor_boss_display")) {
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

            display.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    new org.joml.AxisAngle4f(0, 0, 0, 1),
                    new org.joml.Vector3f(3f, 3f, 3f),
                    new org.joml.AxisAngle4f(0, 0, 0, 1)));

            display.setViewRange(100f);
            display.addScoreboardTag("manor_boss_display");
            display.setPersistent(true);

            updateBossDisplayText(display, true, 0);
        });
    }

    /**
     * Nettoie les TextDisplays orphelins du boss (persistés après un crash/reboot)
     */
    private void cleanupOrphanedBossDisplays(World world) {
        Location bossLoc = MANOR_BOSS_LOCATION.clone();
        bossLoc.setWorld(world);

        int removed = 0;
        // Chercher les TextDisplays avec le tag manor_boss_display dans un rayon de 20
        // blocs
        for (Entity entity : world.getNearbyEntities(bossLoc, 20, 20, 20)) {
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains("manor_boss_display")) {
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            plugin.log(Level.INFO, "§e⚠ Nettoyage: " + removed + " TextDisplay(s) orphelin(s) du boss supprimé(s)");
        }
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

        World world = display.getWorld();
        StringBuilder text = new StringBuilder();
        text.append("§4§l☠ ").append(BOSS_NAME).append(" §4§l☠\n");

        if (bossAlive && manorBossEntity != null && manorBossEntity.isValid()) {
            // Boss vivant - afficher les HP
            if (manorBossEntity instanceof LivingEntity livingBoss) {
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
     * Démarre la tâche de mise à jour du display (toutes les secondes)
     * Recrée le display et le boss s'ils sont invalides
     * Vérifie aussi le leash range du boss
     */
    private void startBossDisplayUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location bossLoc = MANOR_BOSS_LOCATION.clone();
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
                boolean bossAlive = manorBossEntity != null && manorBossEntity.isValid() && !manorBossEntity.isDead();
                int respawnSeconds = 0;

                if (bossAlive) {
                    checkBossLeashRange(world);
                } else {
                    if (!bossRespawnScheduled && bossRespawnTime == 0) {
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
        }.runTaskTimer(plugin, 20L, 40L);
    }

    /**
     * Vérifie si le boss est trop loin de son spawn et le téléporte si nécessaire
     */
    private void checkBossLeashRange(World world) {
        if (manorBossEntity == null || !manorBossEntity.isValid() || manorBossEntity.isDead()) {
            return;
        }

        Location spawnLoc = MANOR_BOSS_LOCATION.clone();
        spawnLoc.setWorld(world);
        spawnLoc.add(0.5, 0, 0.5); // Centrer sur le bloc

        Location bossLoc = manorBossEntity.getLocation();

        // Vérifier uniquement si dans le même monde
        if (!bossLoc.getWorld().equals(world)) {
            return;
        }

        double distanceSquared = bossLoc.distanceSquared(spawnLoc);

        if (distanceSquared > BOSS_LEASH_RANGE_SQUARED) {
            // Boss trop loin - le téléporter au spawn
            teleportBossToSpawn(world, spawnLoc);
        }
    }

    /**
     * Téléporte le boss à son point de spawn avec effets visuels
     */
    private void teleportBossToSpawn(World world, Location spawnLoc) {
        if (manorBossEntity == null || !manorBossEntity.isValid())
            return;

        Location oldLoc = manorBossEntity.getLocation();

        // Effets de disparition
        world.spawnParticle(Particle.SMOKE, oldLoc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
        world.playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);

        // Téléporter le boss
        spawnLoc.setYaw(oldLoc.getYaw());
        spawnLoc.setPitch(0);
        manorBossEntity.teleport(spawnLoc);

        // Effets d'apparition
        world.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        world.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.2f);

        // Reset de la cible - le boss cherchera une nouvelle cible
        if (manorBossEntity instanceof Mob mob) {
            mob.setTarget(null);
        }

        // Effet de régénération partielle au retour (récompense pour le leash)
        if (manorBossEntity instanceof org.bukkit.entity.LivingEntity livingBoss) {
            double currentHealth = livingBoss.getHealth();
            double maxHealth = livingBoss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double healAmount = maxHealth * 0.05; // 5% de heal
            livingBoss.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }

        // Message aux joueurs proches
        for (Player player : world.getNearbyEntities(spawnLoc, 50, 30, 50).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {
            player.sendMessage("§4§l☠ §cLe " + BOSS_NAME + " §7retourne à son domaine!");
        }
    }

    // ==================== NPC MINEUR BLESSÉ (ÉTAPE 4) ====================

    /**
     * Initialise les NPCs du chapitre 2
     */
    private void initializeNPCs(World world) {
        // Nettoyage GLOBAL de tous les NPCs orphelins avant spawn (sécurité maxmobs=1)
        cleanupAllOrphanedNPCs(world);

        // Spawn le mineur blessé
        spawnInjuredMiner(world);
        // Spawn Igor
        spawnIgor(world);
    }

    /**
     * Nettoyage GLOBAL de tous les NPCs de quête orphelins dans le monde entier.
     * Garantit maxmobs=1 en supprimant TOUTES les instances avant de respawner.
     * Recherche dans le monde entier, pas juste à proximité des spawns.
     */
    private void cleanupAllOrphanedNPCs(World world) {
        int totalRemoved = 0;

        for (Entity entity : world.getEntities()) {
            // Nettoyer les mineurs blessés orphelins
            if (entity.getScoreboardTags().contains("zombiez_injured_miner")) {
                entity.remove();
                totalRemoved++;
                continue;
            }

            // Nettoyer les Igor orphelins
            if (entity.getScoreboardTags().contains("zombiez_igor_npc")) {
                entity.remove();
                totalRemoved++;
                continue;
            }

            // Nettoyer les displays du mineur
            if (entity.getScoreboardTags().contains("miner_display")) {
                entity.remove();
                totalRemoved++;
                continue;
            }

            // Fallback: vérifier par PDC pour les anciennes entités sans tag
            if (entity instanceof Villager villager) {
                PersistentDataContainer pdc = villager.getPersistentDataContainer();
                if (pdc.has(INJURED_MINER_KEY, PersistentDataType.BYTE) ||
                        pdc.has(IGOR_NPC_KEY, PersistentDataType.BYTE)) {
                    villager.remove();
                    totalRemoved++;
                }
            }
        }

        if (totalRemoved > 0) {
            plugin.log(Level.INFO,
                    "§e⚠ Nettoyage global Chapter2: " + totalRemoved + " entité(s) orpheline(s) supprimée(s)");
        }
    }

    /**
     * Fait spawn le mineur blessé
     * IMPORTANT: Nettoie tous les villageois orphelins avec la clé PDC avant de
     * spawner
     */
    private void spawnInjuredMiner(World world) {
        Location loc = MINER_LOCATION.clone();
        loc.setWorld(world);

        // Ne spawner que si le chunk est chargé
        if (!loc.getChunk().isLoaded()) {
            return;
        }

        // Supprimer l'ancien si existant
        if (injuredMinerEntity != null && injuredMinerEntity.isValid()) {
            injuredMinerEntity.remove();
        }

        // CLEANUP CRITIQUE: Supprimer TOUS les villageois orphelins avec la clé PDC
        // Cela évite la duplication en cas de bug, explosion AOE, ou rechargement de
        // chunk
        cleanupOrphanedInjuredMiners(world);

        // Créer un villageois comme NPC
        Villager miner = world.spawn(loc, Villager.class, npc -> {
            // NE PAS mettre de customName visible - on utilise un TextDisplay per-player
            npc.setCustomNameVisible(false);
            npc.setProfession(Villager.Profession.TOOLSMITH);
            npc.setVillagerLevel(1);
            npc.setAI(false); // Immobile
            npc.setInvulnerable(true); // Invincible
            npc.setSilent(true);
            npc.setCollidable(false);

            // Équiper avec une pioche
            npc.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));

            // Marquer comme notre NPC (PDC + tag pour cleanup facile)
            npc.getPersistentDataContainer().set(INJURED_MINER_KEY, PersistentDataType.BYTE, (byte) 1);
            npc.addScoreboardTag("zombiez_injured_miner");
            npc.setPersistent(false); // Ne pas persister entre les redémarrages

            // Ajouter l'effet visuel de blessure (particules)
            npc.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false));
        });

        injuredMinerEntity = miner;

        // Créer les deux TextDisplays pour la visibilité per-player
        spawnMinerTextDisplays(world, loc);

        // Particules de blessure périodiques (visibles uniquement pour les joueurs qui
        // n'ont pas soigné)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
                    cancel();
                    return;
                }
                Location particleLoc = injuredMinerEntity.getLocation().add(0, 1, 0);
                // Afficher les particules uniquement aux joueurs qui n'ont pas soigné le mineur
                for (Player nearbyPlayer : world.getNearbyPlayers(particleLoc, 30)) {
                    if (!hasPlayerHealedMiner(nearbyPlayer)) {
                        nearbyPlayer.spawnParticle(Particle.DAMAGE_INDICATOR, particleLoc, 1, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }

    /**
     * Nettoie TOUS les villageois orphelins avec la clé PDC du mineur blessé dans
     * le MONDE ENTIER.
     * Appelé avant chaque spawn pour garantir MAXMOBS=1 (une seule instance).
     * Recherche globale, pas seulement à proximité.
     */
    private void cleanupOrphanedInjuredMiners(World world) {
        int removed = 0;

        // RECHERCHE GLOBALE dans tout le monde pour garantir maxmobs=1
        for (Entity entity : world.getEntities()) {
            // Vérifier par tag scoreboard (plus rapide)
            if (entity.getScoreboardTags().contains("zombiez_injured_miner")) {
                entity.remove();
                removed++;
                continue;
            }

            // Nettoyer aussi les displays du mineur
            if (entity.getScoreboardTags().contains("miner_display")) {
                entity.remove();
                removed++;
                continue;
            }

            // Fallback: vérifier par PDC (pour les anciens villageois sans tag)
            if (entity instanceof Villager villager) {
                if (villager.getPersistentDataContainer().has(INJURED_MINER_KEY, PersistentDataType.BYTE)) {
                    villager.remove();
                    removed++;
                }
            }
        }

        // Log supprimé pour éviter le spam
    }

    /**
     * Spawn les deux TextDisplays au-dessus du mineur (Blessé / Soigné)
     * Utilise setVisibleByDefault(false) pour contrôler la visibilité per-player
     */
    private void spawnMinerTextDisplays(World world, Location minerLoc) {
        // Nettoyer les anciens displays
        if (minerDisplayInjured != null && minerDisplayInjured.isValid()) {
            minerDisplayInjured.remove();
        }
        if (minerDisplayHealed != null && minerDisplayHealed.isValid()) {
            minerDisplayHealed.remove();
        }

        Location displayLoc = minerLoc.clone().add(0, MINER_DISPLAY_HEIGHT, 0);

        // TextDisplay "Mineur Blessé" - VISIBLE PAR DÉFAUT (tous les joueurs le voient
        // initialement)
        minerDisplayInjured = world.spawn(displayLoc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(128, 0, 0, 0));
            entity.setText("§c§l❤ §eMineur Blessé §c§l❤");
            entity.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));
            entity.setViewRange(64f);
            entity.addScoreboardTag("miner_display");
            entity.addScoreboardTag("zombiez_display");
            // VISIBLE PAR DÉFAUT - tous les joueurs voient "Blessé" initialement
            entity.setVisibleByDefault(true);
        });

        // TextDisplay "Mineur Soigné" - INVISIBLE PAR DÉFAUT (montré uniquement aux
        // joueurs qui ont soigné)
        minerDisplayHealed = world.spawn(displayLoc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(128, 0, 0, 0));
            entity.setText("§a§l✓ §fMineur §a§lSoigné");
            entity.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 1)));
            entity.setViewRange(64f);
            entity.addScoreboardTag("miner_display");
            entity.addScoreboardTag("zombiez_display");
            // INVISIBLE PAR DÉFAUT - seulement visible pour ceux qui ont soigné
            entity.setVisibleByDefault(false);
        });

        // IMPORTANT: Initialiser immédiatement la visibilité pour tous les joueurs en
        // ligne
        // Sans cet appel, les displays restent invisibles jusqu'au prochain tick du
        // updater
        initializeMinerDisplayVisibility();
    }

    /**
     * Initialise la visibilité des TextDisplays pour tous les joueurs en ligne.
     * Le display "Blessé" est visible par défaut, on doit juste basculer vers
     * "Soigné"
     * pour les joueurs qui ont déjà soigné le mineur.
     */
    private void initializeMinerDisplayVisibility() {
        if (minerDisplayInjured == null || !minerDisplayInjured.isValid() ||
                minerDisplayHealed == null || !minerDisplayHealed.isValid() ||
                injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
            return;
        }

        Location minerLoc = injuredMinerEntity.getLocation();

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inRange = player.getWorld().equals(minerLoc.getWorld()) &&
                    player.getLocation().distanceSquared(minerLoc) <= MINER_VIEW_DISTANCE * MINER_VIEW_DISTANCE;

            if (inRange) {
                boolean hasHealed = hasPlayerHealedMiner(player);
                // Seuls les joueurs qui ont soigné ont besoin d'une mise à jour
                // (pour voir "Soigné" au lieu de "Blessé" visible par défaut)
                if (hasHealed) {
                    updateMinerDisplayVisibilityForPlayer(player, true);
                }
            }
        }
    }

    /**
     * Fait spawn Igor le survivant
     * IMPORTANT: Nettoie les orphelins et ajoute les protections
     */
    private void spawnIgor(World world) {
        Location loc = IGOR_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien si existant
        if (igorEntity != null && igorEntity.isValid()) {
            igorEntity.remove();
        }

        // CLEANUP: Supprimer les Igor orphelins
        cleanupOrphanedIgor(world);

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

            // Marquer comme notre NPC (PDC + tag pour cleanup facile)
            npc.getPersistentDataContainer().set(IGOR_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
            npc.addScoreboardTag("zombiez_igor_npc");
            npc.setPersistent(false); // Ne pas persister entre les redémarrages
        });

        igorEntity = igor;
    }

    /**
     * Nettoie TOUS les Igor orphelins dans le MONDE ENTIER.
     * Garantit MAXMOBS=1 (une seule instance d'Igor).
     * Recherche globale, pas seulement à proximité.
     */
    private void cleanupOrphanedIgor(World world) {
        int removed = 0;

        // RECHERCHE GLOBALE dans tout le monde pour garantir maxmobs=1
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("zombiez_igor_npc")) {
                entity.remove();
                removed++;
                continue;
            }
            if (entity instanceof Villager villager) {
                if (villager.getPersistentDataContainer().has(IGOR_NPC_KEY, PersistentDataType.BYTE)) {
                    villager.remove();
                    removed++;
                }
            }
        }
        // Log supprimé pour éviter le spam
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

        // Marquer le joueur comme ayant soigné le mineur et mettre à jour sa visibilité
        if (entity instanceof Villager villager) {
            villager.removePotionEffect(PotionEffectType.SLOWNESS);

            // Ajouter au set des joueurs ayant soigné
            playersWhoHealedMiner.add(player.getUniqueId());

            // Mettre à jour IMMÉDIATEMENT la visibilité des TextDisplays pour ce joueur
            updateMinerDisplayVisibilityForPlayer(player, true);

            // Feedback visuel de l'hologramme mis à jour
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.5f);
        }
    }

    /**
     * Protège les NPCs du Chapitre 2 contre TOUS les types de dégâts.
     * Même si setInvulnerable(true) est défini, certains plugins/explosions peuvent
     * bypass.
     * Ce listener est une protection supplémentaire.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChapter2NPCDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // Vérifier par tag scoreboard (plus rapide que PDC)
        if (entity.getScoreboardTags().contains("zombiez_injured_miner") ||
                entity.getScoreboardTags().contains("zombiez_igor_npc")) {
            event.setCancelled(true);
            return;
        }

        // Fallback: vérifier par PDC
        if (entity instanceof Villager villager) {
            PersistentDataContainer pdc = villager.getPersistentDataContainer();
            if (pdc.has(INJURED_MINER_KEY, PersistentDataType.BYTE) ||
                    pdc.has(IGOR_NPC_KEY, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Vérifie si un item est un bandage (utilise le système Consumable)
     */
    private boolean isBandage(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER)
            return false;
        if (!item.hasItemMeta())
            return false;

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

    // ==================== IGOR ET CAISSES DE RAVITAILLEMENT (ÉTAPE 7)
    // ====================

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
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5,
                    0.5, 0);
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

    // Hauteur maximum pour les caisses par rapport à Igor (évite les spawns dans
    // les arbres)
    private static final double CRATE_MAX_HEIGHT_ABOVE_IGOR = 5.0;
    private static final int CRATE_SPAWN_MAX_ATTEMPTS = 10; // Nombre max de tentatives pour trouver une position valide

    /**
     * Trouve une position valide pour spawner une caisse de ravitaillement.
     * Évite les positions trop hautes (arbres) en limitant à Y d'Igor + 5 blocs.
     *
     * @param world       Le monde
     * @param igorLoc     Position d'Igor
     * @param maxAllowedY Hauteur maximum autorisée
     * @return Une position valide pour la caisse
     */
    private Location findValidCrateSpawnLocation(World world, Location igorLoc, double maxAllowedY) {
        // Essayer plusieurs fois de trouver une bonne position
        for (int attempt = 0; attempt < CRATE_SPAWN_MAX_ATTEMPTS; attempt++) {
            // Position aléatoire autour d'Igor
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double distance = CRATE_SPAWN_RADIUS_MIN
                    + ThreadLocalRandom.current().nextDouble() * (CRATE_SPAWN_RADIUS_MAX - CRATE_SPAWN_RADIUS_MIN);
            double x = igorLoc.getX() + Math.cos(angle) * distance;
            double z = igorLoc.getZ() + Math.sin(angle) * distance;
            double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

            // Vérifier si la position est à une hauteur acceptable
            if (y <= maxAllowedY) {
                return new Location(world, x, y, z);
            }
        }

        // Si aucune position valide trouvée après toutes les tentatives,
        // forcer le spawn à la hauteur max autorisée (au sol près d'Igor)
        // Trouver une position au sol en cherchant vers le bas depuis maxAllowedY
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = CRATE_SPAWN_RADIUS_MIN
                + ThreadLocalRandom.current().nextDouble() * (CRATE_SPAWN_RADIUS_MAX - CRATE_SPAWN_RADIUS_MIN);
        double x = igorLoc.getX() + Math.cos(angle) * distance;
        double z = igorLoc.getZ() + Math.sin(angle) * distance;

        // Chercher le premier bloc solide en descendant depuis maxAllowedY
        Location searchLoc = new Location(world, x, maxAllowedY, z);
        while (searchLoc.getY() > igorLoc.getY() - 10 && !searchLoc.getBlock().getType().isSolid()) {
            searchLoc.subtract(0, 1, 0);
        }

        // Position juste au-dessus du bloc solide trouvé
        return new Location(world, x, searchLoc.getY() + 1, z);
    }

    /**
     * Spawn des caisses de ravitaillement autour d'Igor pour un joueur
     * Chaque joueur a ses propres caisses (INVISIBLES par défaut, visibles
     * uniquement par le propriétaire)
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

        // Hauteur max autorisée (Y d'Igor + 5 blocs)
        double maxAllowedY = igorLoc.getY() + CRATE_MAX_HEIGHT_ABOVE_IGOR;

        for (int i = 0; i < cratesToSpawn; i++) {
            Location spawnLoc = findValidCrateSpawnLocation(world, igorLoc, maxAllowedY);

            // 1. Créer le VISUEL (ItemDisplay) - ne peut pas être cliqué
            // INVISIBLE PAR DÉFAUT - visible uniquement par le propriétaire
            ItemDisplay visual = world.spawn(spawnLoc, ItemDisplay.class, display -> {
                // Item affiché: Chest
                display.setItemStack(new ItemStack(Material.CHEST));

                // Taille x1.5 pour visibilité
                display.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, 0.5f, 0), // Translation (légèrement au-dessus du sol)
                        new org.joml.AxisAngle4f(0, 0, 1, 0), // Left rotation
                        new org.joml.Vector3f(1.5f, 1.5f, 1.5f), // Scale x1.5
                        new org.joml.AxisAngle4f(0, 0, 1, 0) // Right rotation
                ));

                // FIXED pour que la caisse ne tourne pas (plus réaliste)
                display.setBillboard(Display.Billboard.FIXED);

                // Glow effect pour visibilité
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 200, 50)); // Jaune/Or

                // Distance de vue
                display.setViewRange(64f);

                // INVISIBLE PAR DÉFAUT - sera visible uniquement par le propriétaire
                display.setVisibleByDefault(false);
                display.setPersistent(false);
                display.addScoreboardTag("supply_crate_visual");
            });

            // 2. Créer l'entité INTERACTION (invisible mais cliquable)
            // INVISIBLE PAR DÉFAUT - cliquable uniquement par le propriétaire
            Interaction hitbox = world.spawn(spawnLoc.clone().add(0, 0.5, 0), Interaction.class, interaction -> {
                // Taille de la hitbox (largeur et hauteur)
                interaction.setInteractionWidth(1.5f);
                interaction.setInteractionHeight(1.5f);

                // Marquer comme caisse de ravitaillement
                PersistentDataContainer pdc = interaction.getPersistentDataContainer();
                pdc.set(SUPPLY_CRATE_KEY, PersistentDataType.BYTE, (byte) 1);
                pdc.set(CRATE_OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());

                // Lier au visuel pour le supprimer ensemble
                pdc.set(new NamespacedKey(plugin, "visual_uuid"), PersistentDataType.STRING,
                        visual.getUniqueId().toString());

                // INVISIBLE PAR DÉFAUT - sera visible uniquement par le propriétaire
                interaction.setVisibleByDefault(false);
                interaction.setPersistent(false);
                interaction.addScoreboardTag("supply_crate_hitbox");
            });

            // Montrer les entités UNIQUEMENT au propriétaire
            player.showEntity(plugin, visual);
            player.showEntity(plugin, hitbox);

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
            } catch (IllegalArgumentException ignored) {
            }
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
        player.sendActionBar(
                Component.text("§a+1 §eCaisse récupérée §7(" + newProgress + "/" + SUPPLY_CRATE_COUNT + ")"));

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

                if (!playersNearby)
                    return;

                // Compter les zombies incendiés existants
                long fireZombieCount = world.getEntitiesByClass(Zombie.class).stream()
                        .filter(z -> z.getPersistentDataContainer().has(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE))
                        .count();

                // Limiter à 15 zombies max
                if (fireZombieCount >= 15)
                    return;

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
        if (zombieManager == null)
            return;

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
        if (!(event.getEntity() instanceof Zombie zombie))
            return;
        if (!zombie.getPersistentDataContainer().has(FIRE_ZOMBIE_KEY, PersistentDataType.BYTE))
            return;

        Player killer = zombie.getKiller();
        if (killer == null)
            return;

        // Vérifier si le joueur est à l'étape 6
        JourneyStep currentStep = journeyManager.getCurrentStep(killer);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_6)
            return;

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
        // Protection anti-spawn multiple: si le boss existe déjà et est valide
        if (manorBossEntity != null && manorBossEntity.isValid() && !manorBossEntity.isDead()) {
            return;
        }

        Location loc = MANOR_BOSS_LOCATION.clone();
        loc.setWorld(world);

        // Ne spawn que si le chunk est chargé
        if (!loc.getChunk().isLoaded()) {
            return;
        }

        // Chercher un boss existant dans le monde (persisté après reboot)
        for (Entity entity : world.getNearbyEntities(loc, 50, 30, 50)) {
            if (entity instanceof Zombie z
                    && z.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE)) {
                manorBossEntity = z;
                return; // Réutiliser l'existant
            }
        }

        // Nettoyer les contributeurs
        bossContributors.clear();
        bossRespawnScheduled = false;

        ZombieManager zombieManager = plugin.getZombieManager();
        if (zombieManager == null) {
            plugin.log(Level.WARNING, "ZombieManager non disponible, spawn du boss annulé");
            return;
        }

        // Spawn via ZombieManager (avec IA JourneyBossAI, display name dynamique,
        // système de dégâts ZombieZ)
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
        boss.setPersistent(true); // IMPORTANT: survit au déchargement de chunk

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

        // Activer l'affichage du nom avec vie (système ZombieZ dynamique)
        boss.setCustomNameVisible(true);
    }

    /**
     * Tracker les joueurs qui attaquent le boss
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie boss))
            return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE))
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
     * Empêcher le boss de cibler autre chose que les joueurs
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie boss))
            return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE))
            return;

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
        if (!(event.getEntity() instanceof Zombie boss))
            return;
        if (!boss.getPersistentDataContainer().has(MANOR_BOSS_KEY, PersistentDataType.BYTE))
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
                    player.sendMessage(
                            "§8Le Seigneur du Manoir reviendra dans §c" + BOSS_RESPAWN_SECONDS + " secondes§8...");
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

    // ==================== TEXTDISPLAY PER-PLAYER VIA VISIBILITÉ (MINEUR)
    // ====================
    // Utilise 2 TextDisplays fixes avec Paper API showEntity/hideEntity pour la
    // visibilité per-player

    /**
     * Démarre le système de TextDisplay per-player et le respawn automatique des
     * NPCs.
     * Utilise deux TextDisplays fixes (Blessé / Soigné) et gère la visibilité
     * per-player via l'API Paper showEntity/hideEntity.
     *
     * SÉCURITÉ MAXMOBS=1: Respawne automatiquement les NPCs s'ils sont invalides,
     * avec nettoyage préalable pour garantir une seule instance.
     */
    private void startNPCNameUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorld("world");
                if (world == null)
                    return;

                Location minerLoc = MINER_LOCATION.clone();
                minerLoc.setWorld(world);
                Location igorLoc = IGOR_LOCATION.clone();
                igorLoc.setWorld(world);

                // === SÉCURITÉ MINEUR BLESSÉ (maxmobs=1) ===
                // Ne respawn QUE si le chunk est déjà chargé par un joueur (évite boucle
                // infinie)
                if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
                    if (minerLoc.getChunk().isLoaded()) {
                        cleanupOrphanedInjuredMiners(world);
                        spawnInjuredMiner(world);
                    }
                }

                // === SÉCURITÉ IGOR (maxmobs=1) ===
                // Ne respawn QUE si le chunk est déjà chargé par un joueur (évite boucle
                // infinie)
                if (igorEntity == null || !igorEntity.isValid()) {
                    if (igorLoc.getChunk().isLoaded()) {
                        cleanupOrphanedIgor(world);
                        spawnIgor(world);
                    }
                }

                // Si le mineur n'est toujours pas valide, on arrête ici
                if (injuredMinerEntity == null || !injuredMinerEntity.isValid()) {
                    return;
                }

                // Vérifier et recréer les displays si nécessaire (seulement si chunk chargé)
                if ((minerDisplayInjured == null || !minerDisplayInjured.isValid() ||
                        minerDisplayHealed == null || !minerDisplayHealed.isValid()) &&
                        minerLoc.getChunk().isLoaded()) {
                    spawnMinerTextDisplays(world, injuredMinerEntity.getLocation());
                }

                // Mettre à jour la position des displays (suivre le mineur)
                Location displayLoc = injuredMinerEntity.getLocation().clone().add(0, MINER_DISPLAY_HEIGHT, 0);
                if (minerDisplayInjured != null && minerDisplayInjured.isValid()) {
                    minerDisplayInjured.teleport(displayLoc);
                }
                if (minerDisplayHealed != null && minerDisplayHealed.isValid()) {
                    minerDisplayHealed.teleport(displayLoc);
                }

                Location entityMinerLoc = injuredMinerEntity.getLocation();

                // Mettre à jour la visibilité pour chaque joueur à portée
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean inRange = player.getWorld().equals(entityMinerLoc.getWorld()) &&
                            player.getLocation().distanceSquared(entityMinerLoc) <= MINER_VIEW_DISTANCE
                                    * MINER_VIEW_DISTANCE;

                    if (inRange) {
                        boolean hasHealed = hasPlayerHealedMiner(player);
                        updateMinerDisplayVisibilityForPlayer(player, hasHealed);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Augmenté à toutes les 20 ticks (1 sec) pour réduire le spam
    }

    /**
     * Vérifie si le joueur a déjà soigné le mineur (étape 4 du chapitre 2 complétée
     * ou dépassée)
     */
    private boolean hasPlayerHealedMiner(Player player) {
        // Vérifier d'abord dans le cache de session (pour mise à jour immédiate)
        if (playersWhoHealedMiner.contains(player.getUniqueId())) {
            return true;
        }

        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null)
            return false;

        // Si le joueur est au chapitre 2 étape 5 ou plus, ou dans un chapitre supérieur
        if (currentStep.getChapter().getId() > 2)
            return true;
        if (currentStep.getChapter().getId() == 2 && currentStep.getStepNumber() > 4)
            return true;

        // Si le joueur est exactement à l'étape 4, vérifier la progression
        if (currentStep == JourneyStep.STEP_2_4) {
            int progress = journeyManager.getStepProgress(player, currentStep);
            return progress >= 1; // 1 = a soigné le mineur
        }

        return false;
    }

    /**
     * Met à jour la visibilité des TextDisplays pour un joueur donné.
     * Montre le display "Blessé" ou "Soigné" selon l'état.
     *
     * @param player Le joueur
     * @param healed true si le joueur a soigné le mineur, false sinon
     */
    private void updateMinerDisplayVisibilityForPlayer(Player player, boolean healed) {
        if (minerDisplayInjured == null || !minerDisplayInjured.isValid() ||
                minerDisplayHealed == null || !minerDisplayHealed.isValid()) {
            return;
        }

        if (healed) {
            // Joueur a soigné: montrer "Soigné", cacher "Blessé"
            player.showEntity(plugin, minerDisplayHealed);
            player.hideEntity(plugin, minerDisplayInjured);
        } else {
            // Joueur n'a pas soigné: montrer "Blessé", cacher "Soigné"
            player.showEntity(plugin, minerDisplayInjured);
            player.hideEntity(plugin, minerDisplayHealed);
        }
    }

    /**
     * Cache les deux displays pour un joueur (quand il est hors de portée)
     */
    private void hideBothMinerDisplaysForPlayer(Player player) {
        if (minerDisplayInjured != null && minerDisplayInjured.isValid()) {
            player.hideEntity(plugin, minerDisplayInjured);
        }
        if (minerDisplayHealed != null && minerDisplayHealed.isValid()) {
            player.hideEntity(plugin, minerDisplayHealed);
        }
    }

    /**
     * Détruit les deux TextDisplays (nettoyage)
     */
    private void destroyAllMinerDisplays() {
        if (minerDisplayInjured != null && minerDisplayInjured.isValid()) {
            minerDisplayInjured.remove();
        }
        if (minerDisplayHealed != null && minerDisplayHealed.isValid()) {
            minerDisplayHealed.remove();
        }
        minerDisplayInjured = null;
        minerDisplayHealed = null;
        playersWhoHealedMiner.clear();
    }

    /**
     * Nettoie les données d'un joueur qui se déconnecte
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // Nettoyer le cache de soin du mineur (sera rechargé via progression au
        // reconnect)
        playersWhoHealedMiner.remove(playerId);

        // Nettoyer aussi les caisses de ravitaillement du joueur
        cleanupPlayerCrates(playerId);
    }

    /**
     * Gère la mort du joueur pendant la quête des caisses (étape 7)
     * Si le joueur meurt, l'étape est annulée et il doit recommencer à 0
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeathDuringSupplyCrateQuest(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Vérifier si le joueur est à l'étape 7 du chapitre 2
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_7) {
            return;
        }

        // Vérifier si le joueur avait des caisses actives (quête en cours)
        List<Entity> crates = playerSupplyCrates.get(player.getUniqueId());
        if (crates == null || crates.isEmpty()) {
            return; // Pas de quête de caisses en cours
        }

        // Nettoyer les caisses du joueur
        cleanupPlayerCrates(player.getUniqueId());

        // Réinitialiser la progression à 0
        journeyManager.setStepProgress(player, JourneyStep.STEP_2_7, 0);

        // Feedback au joueur après un délai (pour qu'il voit le message après le respawn)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage("");
                    player.sendMessage("§c§l✗ §eQuête échouée!");
                    player.sendMessage("§7Tu es mort pendant la recherche des caisses.");
                    player.sendMessage("§7Retourne parler à §6Igor §7pour recommencer.");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
                }
            }
        }.runTaskLater(plugin, 40L); // 2 secondes après la mort (temps de respawn)
    }

    /**
     * Nettoie les ressources lors de la désactivation du plugin
     */
    public void cleanup() {
        // Détruire les TextDisplays per-player
        destroyAllMinerDisplays();

        if (injuredMinerEntity != null)
            injuredMinerEntity.remove();
        if (igorEntity != null)
            igorEntity.remove();
        if (manorBossEntity != null)
            manorBossEntity.remove();
        if (bossSpawnDisplay != null)
            bossSpawnDisplay.remove();
        cleanupAllSupplyCrates(); // Nettoyer toutes les caisses de ravitaillement
    }
}