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
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
    private final Map<UUID, Integer> woodGivenByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> bossContributors = ConcurrentHashMap.newKeySet();
    private boolean bossRespawnScheduled = false;

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

        // Initialiser les clés PDC
        INJURED_MINER_KEY = new NamespacedKey(plugin, "injured_miner");
        IGOR_NPC_KEY = new NamespacedKey(plugin, "igor_npc");
        FIRE_ZOMBIE_KEY = new NamespacedKey(plugin, "fire_zombie");
        MANOR_BOSS_KEY = new NamespacedKey(plugin, "manor_boss");
        BOSS_CONTRIBUTORS_KEY = new NamespacedKey(plugin, "boss_contributors");

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
                    spawnManorBoss(world);
                }
            }
        }.runTaskLater(plugin, 100L);
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
            npc.setCustomName("§c§l❤ §eMinerur Blessé §c§l❤");
            npc.setCustomNameVisible(true);
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

        // Changer l'apparence du NPC (soigné)
        if (entity instanceof Villager villager) {
            villager.setCustomName("§a§l✓ §7Mineur (Soigné)");
            villager.removePotionEffect(PotionEffectType.SLOWNESS);
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

    // ==================== IGOR ET RÉCOLTE DE BOIS (ÉTAPE 7) ====================

    /**
     * Gère l'interaction avec Igor pour donner du bois
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

        // Compter le bois de chêne dans l'inventaire
        ItemStack handItem = player.getInventory().getItemInMainHand();
        int woodCount = 0;

        // Accepter tous les types de bûches
        if (isLog(handItem)) {
            woodCount = handItem.getAmount();
        }

        if (woodCount == 0) {
            int currentProgress = journeyManager.getStepProgress(player, currentStep);
            player.sendMessage("");
            player.sendMessage("§6§lIgor: §f\"J'ai besoin de bûches de bois pour rebâtir mon village!\"");
            player.sendMessage("§7Progression: §e" + currentProgress + "§7/§e8 §7bûches");

            // Vérifier si le joueur a la hache, sinon lui en donner une nouvelle
            if (!journeyManager.hasWoodcutterAxe(player)) {
                player.sendMessage("");
                player.sendMessage("§6§lIgor: §f\"Tu n'as plus ma hache? Tiens, en voici une autre!\"");
                journeyManager.giveWoodcutterAxe(player, true);
            } else {
                player.sendMessage("§8(Utilise la Hache de Bûcheron pour couper du chêne)");
            }
            player.sendMessage("");
            return;
        }

        // Calculer combien prendre
        int currentProgress = journeyManager.getStepProgress(player, currentStep);
        int needed = 8 - currentProgress;
        int toTake = Math.min(woodCount, needed);

        // Retirer les bûches
        handItem.setAmount(handItem.getAmount() - toTake);

        // Mettre à jour la progression
        int newProgress = currentProgress + toTake;
        journeyManager.updateProgress(player, JourneyStep.StepType.GIVE_WOOD_NPC, newProgress);

        // Feedback
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        player.sendMessage("");
        player.sendMessage("§a§l✓ §6Igor: §f\"Excellent! " + toTake + " bûche(s) de plus!\"");
        player.sendMessage("§7Progression: §e" + newProgress + "§7/§e8 §7bûches");

        if (newProgress >= 8) {
            player.sendMessage("");
            player.sendMessage("§6§lIgor: §f\"C'est parfait! Grâce à toi, je peux reconstruire!\"");
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        }
        player.sendMessage("");
    }

    /**
     * Vérifie si un item est une bûche de bois
     */
    private boolean isLog(ItemStack item) {
        if (item == null) return false;
        return switch (item.getType()) {
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG,
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG,
                 CRIMSON_STEM, WARPED_STEM -> true;
            default -> false;
        };
    }

    /**
     * Gère la récolte de bois en frappant les arbres
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Vérifier si c'est une bûche
        if (!isLogBlock(blockType)) return;

        // Vérifier si le joueur est à l'étape 7
        JourneyStep currentStep = journeyManager.getCurrentStep(player);
        if (currentStep == null || currentStep != JourneyStep.STEP_2_7) return;

        // Ne pas casser le bloc, mais donner la ressource
        event.setCancelled(true);

        // Convertir le type de bloc en item
        Material logItem = blockToLogItem(blockType);
        if (logItem == null) return;

        // Donner la bûche au joueur (avec cooldown pour éviter le spam)
        UUID uuid = player.getUniqueId();
        if (isOnCooldown(uuid, "wood_harvest")) return;
        setCooldown(uuid, "wood_harvest", 500); // 500ms cooldown

        // Ajouter à l'inventaire ou drop
        ItemStack log = new ItemStack(logItem, 1);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(log);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), log);
        }

        // Effets visuels et sonores
        Location blockLoc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        player.getWorld().spawnParticle(Particle.BLOCK, blockLoc, 10, 0.3, 0.3, 0.3, 0, blockType.createBlockData());
        player.playSound(blockLoc, Sound.BLOCK_WOOD_HIT, 1f, 1f);

        // Message
        player.sendActionBar(Component.text("§a+1 §7Bûche récoltée"));
    }

    private boolean isLogBlock(Material type) {
        return switch (type) {
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG,
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG,
                 CRIMSON_STEM, WARPED_STEM,
                 STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG,
                 STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG,
                 STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM -> true;
            default -> false;
        };
    }

    private Material blockToLogItem(Material blockType) {
        return switch (blockType) {
            case OAK_LOG, STRIPPED_OAK_LOG -> Material.OAK_LOG;
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG -> Material.SPRUCE_LOG;
            case BIRCH_LOG, STRIPPED_BIRCH_LOG -> Material.BIRCH_LOG;
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG -> Material.JUNGLE_LOG;
            case ACACIA_LOG, STRIPPED_ACACIA_LOG -> Material.ACACIA_LOG;
            case DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG -> Material.DARK_OAK_LOG;
            case MANGROVE_LOG, STRIPPED_MANGROVE_LOG -> Material.MANGROVE_LOG;
            case CHERRY_LOG, STRIPPED_CHERRY_LOG -> Material.CHERRY_LOG;
            case CRIMSON_STEM, STRIPPED_CRIMSON_STEM -> Material.CRIMSON_STEM;
            case WARPED_STEM, STRIPPED_WARPED_STEM -> Material.WARPED_STEM;
            default -> null;
        };
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
        }.runTaskTimer(plugin, 200L, 100L); // Toutes les 5 secondes
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

                // Effet de spawn
                world.spawnParticle(Particle.FLAME, spawnLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                world.spawnParticle(Particle.LAVA, spawnLoc.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0);
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
     * Fait spawn le boss du manoir
     */
    private void spawnManorBoss(World world) {
        Location loc = MANOR_BOSS_LOCATION.clone();
        loc.setWorld(world);

        // Supprimer l'ancien boss si existant
        if (manorBossEntity != null && manorBossEntity.isValid()) {
            manorBossEntity.remove();
        }

        // Nettoyer les contributeurs
        bossContributors.clear();
        bossRespawnScheduled = false;

        // Créer un Zombie géant comme boss
        Zombie boss = world.spawn(loc, Zombie.class, zombie -> {
            zombie.setCustomName("§4§l☠ Seigneur du Manoir ☠");
            zombie.setCustomNameVisible(true);
            zombie.setBaby(false);
            zombie.setCanPickupItems(false);

            // Scale x3 via Paper API
            var scale = zombie.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(3.0);
            }

            var health = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(500); // 250 coeurs
                zombie.setHealth(500);
            }

            var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(15); // Dégâts importants
            }

            var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.28); // Plus rapide que normal
            }

            var knockback = zombie.getAttribute(Attribute.ATTACK_KNOCKBACK);
            if (knockback != null) {
                knockback.setBaseValue(2.0); // Fort knockback
            }

            // Équipement épique
            zombie.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            zombie.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            zombie.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            zombie.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));

            // Pas de drop
            zombie.getEquipment().setHelmetDropChance(0);
            zombie.getEquipment().setChestplateDropChance(0);
            zombie.getEquipment().setLeggingsDropChance(0);
            zombie.getEquipment().setBootsDropChance(0);
            zombie.getEquipment().setItemInMainHandDropChance(0);

            // Effets de boss
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, true));
            zombie.setGlowing(true);

            // Marquer comme boss du manoir
            zombie.getPersistentDataContainer().set(MANOR_BOSS_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        manorBossEntity = boss;

        // Démarrer l'IA du boss (attaques spéciales)
        startBossAI(boss);

        // Annoncer le spawn
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(loc) < 100) {
                player.sendMessage("");
                player.sendMessage("§4§l☠ Le Seigneur du Manoir a émergé des ténèbres!");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
            }
        }
    }

    /**
     * IA spéciale du boss - attaques périodiques
     */
    private void startBossAI(Zombie boss) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (boss == null || !boss.isValid() || boss.isDead()) {
                    cancel();
                    return;
                }

                tick++;
                World world = boss.getWorld();
                Location bossLoc = boss.getLocation();

                // Toutes les 5 secondes: attaque de zone
                if (tick % 100 == 0) {
                    // Onde de choc
                    world.playSound(bossLoc, Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
                    world.spawnParticle(Particle.EXPLOSION, bossLoc, 10, 2, 1, 2, 0);

                    // Repousser les joueurs proches
                    for (Entity entity : boss.getNearbyEntities(5, 3, 5)) {
                        if (entity instanceof Player player) {
                            player.damage(8, boss);
                            player.setVelocity(player.getLocation().toVector()
                                .subtract(bossLoc.toVector()).normalize().multiply(1.5).setY(0.5));
                        }
                    }
                }

                // Toutes les 8 secondes: invocation de renforts
                if (tick % 160 == 0) {
                    world.playSound(bossLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 0.8f);

                    // Spawn 2-3 zombies normaux
                    int reinforcements = ThreadLocalRandom.current().nextInt(2, 4);
                    for (int i = 0; i < reinforcements; i++) {
                        Location spawnLoc = bossLoc.clone().add(
                            ThreadLocalRandom.current().nextDouble(-3, 3),
                            0,
                            ThreadLocalRandom.current().nextDouble(-3, 3)
                        );
                        world.spawn(spawnLoc, Zombie.class, z -> {
                            z.setCustomName("§7Serviteur du Manoir");
                            z.setCustomNameVisible(true);
                        });
                    }
                }

                // Particules constantes
                if (tick % 10 == 0) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, bossLoc.add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnManorBoss(world);
                }
            }.runTaskLater(plugin, 20L * 60); // 60 secondes

            // Annoncer le respawn
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(deathLoc) < 100) {
                    player.sendMessage("§8Le Seigneur du Manoir reviendra dans §c1 minute§8...");
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

    /**
     * Nettoie les ressources lors de la désactivation du plugin
     */
    public void cleanup() {
        if (injuredMinerEntity != null) injuredMinerEntity.remove();
        if (igorEntity != null) igorEntity.remove();
        if (manorBossEntity != null) manorBossEntity.remove();
    }
}
