package com.rinaorc.zombiez.zombies;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.ArmorTrimGenerator;
import com.rinaorc.zombiez.items.generator.LootTable;
import com.rinaorc.zombiez.zombies.affixes.ZombieAffix;
import com.rinaorc.zombiez.zombies.ai.ZombieAI;
import com.rinaorc.zombiez.zombies.ai.ZombieAIManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Creaking;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Item;
import org.bukkit.ChatColor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.Particle;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.mobs.food.FoodItem;
import com.rinaorc.zombiez.consumables.Consumable;
import com.rinaorc.zombiez.consumables.ConsumableType;
import com.rinaorc.zombiez.consumables.ConsumableRarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager principal pour les zombies ZombieZ
 * Gère le spawn, les affixes et le système d'IA personnalisé
 */
public class ZombieManager {

    private final ZombieZPlugin plugin;
    private final ZombieAffix.ZombieAffixRegistry affixRegistry;
    private final LootTable.LootTableRegistry lootTableRegistry;

    // Gestionnaire d'IA personnalisé
    @Getter
    private final ZombieAIManager aiManager;

    // Tracking des zombies actifs
    private final Map<UUID, ActiveZombie> activeZombies;
    
    // Compteurs par zone
    private final Map<Integer, Integer> zombieCountByZone;
    
    // Limites de zombies par zone
    private final Map<Integer, Integer> maxZombiesPerZone;
    
    // Cache des derniers spawns pour éviter le spam
    private final Map<Integer, Long> lastSpawnByZone;
    
    // Stats
    private long totalSpawned = 0;
    private long totalKilled = 0;

    // === PERSISTENT DATA CONTAINER KEYS ===
    // Clé PDC pour identifier les mobs ZombieZ de manière ultra-performante
    @Getter
    private final NamespacedKey pdcMobKey;
    @Getter
    private final NamespacedKey pdcTypeKey;
    @Getter
    private final NamespacedKey pdcLevelKey;
    @Getter
    private final NamespacedKey pdcZoneKey;
    @Getter
    private final NamespacedKey pdcSpawnTimeKey;

    // === SYSTÈME DE COLLISION (DÉSACTIVATION) ===
    private static final String COLLISION_TEAM_NAME = "zombiez_nocollide";
    private Team noCollisionTeam;

    // === LIMITE GLOBALE DE SPAWN ===
    @Getter
    private int maxGlobalZombies = 500; // Valeur par défaut, modifiable via config

    // === SYSTÈME DE LOOT AMÉLIORÉ ===
    // Pity system: kills sans drop par joueur (reset quand drop)
    private final Map<UUID, Integer> killsWithoutDrop = new ConcurrentHashMap<>();
    private static final int PITY_THRESHOLD = 15; // Garanti un drop après 15 kills sans loot
    private static final double PITY_BONUS_PER_KILL = 0.02; // +2% par kill sans drop

    // Jackpot system: probabilité de bonus drop pour les hauts combos/streaks
    private static final int JACKPOT_COMBO_THRESHOLD = 20;
    private static final int JACKPOT_STREAK_THRESHOLD = 30;
    private static final double JACKPOT_CHANCE = 0.15; // 15% de chance de jackpot

    public ZombieManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.affixRegistry = ZombieAffix.ZombieAffixRegistry.getInstance();
        this.lootTableRegistry = LootTable.LootTableRegistry.getInstance();
        this.aiManager = new ZombieAIManager(plugin);
        this.activeZombies = new ConcurrentHashMap<>();
        this.zombieCountByZone = new ConcurrentHashMap<>();
        this.maxZombiesPerZone = new ConcurrentHashMap<>();
        this.lastSpawnByZone = new ConcurrentHashMap<>();

        // Initialisation des clés PDC pour marquage ultra-performant
        this.pdcMobKey = new NamespacedKey(plugin, "zombiez_mob");
        this.pdcTypeKey = new NamespacedKey(plugin, "zombiez_type");
        this.pdcLevelKey = new NamespacedKey(plugin, "zombiez_level");
        this.pdcZoneKey = new NamespacedKey(plugin, "zombiez_zone");
        this.pdcSpawnTimeKey = new NamespacedKey(plugin, "zombiez_spawn_time");

        // Chargement de la limite globale depuis la config
        loadConfigValues();

        // Initialisation du système de collision désactivée
        initializeCollisionTeam();

        initializeZoneLimits();
    }

    /**
     * Charge les valeurs de configuration pour les performances
     */
    public void loadConfigValues() {
        var config = plugin.getConfigManager().getMainConfig();
        if (config != null) {
            this.maxGlobalZombies = config.getInt("zombies.max-global", 500);
        }
    }

    /**
     * Initialise la team pour désactiver les collisions entre zombies
     * Réduit drastiquement les calculs physiques quand les zombies sont nombreux
     */
    private void initializeCollisionTeam() {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();

        // Supprimer l'ancienne team si elle existe
        Team existingTeam = scoreboard.getTeam(COLLISION_TEAM_NAME);
        if (existingTeam != null) {
            existingTeam.unregister();
        }

        // Créer la nouvelle team avec collisions désactivées
        noCollisionTeam = scoreboard.registerNewTeam(COLLISION_TEAM_NAME);
        noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        noCollisionTeam.setCanSeeFriendlyInvisibles(false);

        plugin.getLogger().info("§7[Performance] Team de collision désactivée initialisée: " + COLLISION_TEAM_NAME);
    }

    /**
     * Ajoute une entité à la team sans collision
     */
    private void addToNoCollisionTeam(Entity entity) {
        if (noCollisionTeam != null && entity instanceof LivingEntity) {
            try {
                noCollisionTeam.addEntry(entity.getUniqueId().toString());
            } catch (Exception e) {
                // Silencieux - la team peut avoir des limites
            }
        }
    }

    /**
     * Retire une entité de la team sans collision
     */
    private void removeFromNoCollisionTeam(Entity entity) {
        if (noCollisionTeam != null) {
            try {
                noCollisionTeam.removeEntry(entity.getUniqueId().toString());
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Initialise les limites de zombies par zone
     * 50 zones avec limites progressives
     */
    private void initializeZoneLimits() {
        // Zone 0 (Spawn) - Pas de zombies
        maxZombiesPerZone.put(0, 0);

        // 50 zones avec limites calculées dynamiquement
        for (int zone = 1; zone <= 50; zone++) {
            int limit;
            if (zone <= 10) {
                // ACTE I - LES DERNIERS JOURS: 60-100 zombies
                limit = 60 + (zone * 4);
            } else if (zone <= 20) {
                // ACTE II - LA CONTAMINATION: 80-120 zombies
                limit = 80 + ((zone - 10) * 4);
            } else if (zone <= 30) {
                // ACTE III - LE CHAOS: 100-140 zombies
                limit = 100 + ((zone - 20) * 4);
            } else if (zone <= 40) {
                // ACTE IV - L'EXTINCTION: 90-110 zombies
                limit = 90 + ((zone - 30) * 2);
            } else {
                // ACTE V - L'ORIGINE DU MAL: 70-80 zombies (plus difficile, moins nombreux)
                limit = 70 + ((zone - 40));
            }

            // Zone PVP (26) - Moins de zombies
            if (zone == 26) {
                limit = 50;
            }
            // Zone Boss finale (50) - Moins de zombies mais plus dangereux
            if (zone == 50) {
                limit = 40;
            }

            maxZombiesPerZone.put(zone, limit);
        }
    }

    /**
     * Spawn un zombie avec IA personnalisée
     */
    public ActiveZombie spawnZombie(ZombieType type, Location location, int level) {
        return spawnZombie(type, location, level, false);
    }

    /**
     * Spawn un zombie avec IA personnalisée
     * @param skipEliteConversion true pour empêcher la conversion en élite (utilisé par SUMMONER)
     */
    public ActiveZombie spawnZombie(ZombieType type, Location location, int level, boolean skipEliteConversion) {
        int zoneId = plugin.getZoneManager().getZoneAt(location).getId();

        // Les boss bypass toutes les limites (JOURNEY_BOSS, ZONE_BOSS, etc.)
        boolean isBoss = type.isBoss();

        // ═══════════════════════════════════════════════════════════════════
        // VÉRIFICATION 1: Limite globale (Global Cap) - Boss bypass
        // ═══════════════════════════════════════════════════════════════════
        if (!isBoss && activeZombies.size() >= maxGlobalZombies) {
            return null;
        }

        // Vérifier la limite de zone - Boss bypass
        if (!isBoss) {
            int currentCount = zombieCountByZone.getOrDefault(zoneId, 0);
            int maxCount = maxZombiesPerZone.getOrDefault(zoneId, 50);

            if (currentCount >= maxCount) {
                return null;
            }
        }

        // Vérifier le cooldown de spawn - Boss bypass
        if (!isBoss) {
            long lastSpawn = lastSpawnByZone.getOrDefault(zoneId, 0L);
            if (System.currentTimeMillis() - lastSpawn < 500) { // 500ms minimum entre spawns
                return null;
            }
        }

        // Spawner l'entité personnalisée (peut être zombie ou autre)
        LivingEntity entity = spawnEntityByType(type, location, level, zoneId);

        if (entity == null) {
            return null;
        }

        long spawnTime = System.currentTimeMillis();

        // Créer l'ActiveZombie
        ActiveZombie zombie = new ActiveZombie(entity.getUniqueId(), type, level, zoneId);

        // Appliquer les affixes si applicable
        if (shouldHaveAffix(zoneId, type)) {
            applyRandomAffix(zombie, entity, zoneId);
        }

        // ═══════════════════════════════════════════════════════════════════
        // MARQUAGE PDC (PersistentDataContainer) - Ultra-performant
        // ═══════════════════════════════════════════════════════════════════
        var pdc = entity.getPersistentDataContainer();
        pdc.set(pdcMobKey, PersistentDataType.BYTE, (byte) 1);           // Marqueur principal
        pdc.set(pdcTypeKey, PersistentDataType.STRING, type.name());     // Type de zombie
        pdc.set(pdcLevelKey, PersistentDataType.INTEGER, level);         // Niveau
        pdc.set(pdcZoneKey, PersistentDataType.INTEGER, zoneId);         // Zone de spawn
        pdc.set(pdcSpawnTimeKey, PersistentDataType.LONG, spawnTime);    // Temps de spawn

        // Stocker les métadonnées (conservé pour compatibilité)
        entity.setMetadata("zombiez_type", new FixedMetadataValue(plugin, type.name()));
        entity.setMetadata("zombiez_level", new FixedMetadataValue(plugin, level));
        entity.setMetadata("zombiez_zone", new FixedMetadataValue(plugin, zoneId));

        if (zombie.hasAffix()) {
            entity.setMetadata("zombiez_affix", new FixedMetadataValue(plugin, zombie.getAffix().getId()));
        }

        // ═══════════════════════════════════════════════════════════════════
        // DÉSACTIVATION DES COLLISIONS - Performance
        // ═══════════════════════════════════════════════════════════════════
        addToNoCollisionTeam(entity);

        // Créer l'IA pour cette entité (tous les types, pas seulement Zombie)
        // FIX: Les entités non-Zombie comme Creaking ont aussi besoin d'une IA
        aiManager.createAI(entity, type, level);

        // Enregistrer AVANT la conversion élite (pour que updateZombieHealthDisplay fonctionne)
        activeZombies.put(entity.getUniqueId(), zombie);
        zombieCountByZone.merge(zoneId, 1, Integer::sum);
        lastSpawnByZone.put(zoneId, System.currentTimeMillis());
        totalSpawned++;

        // ═══════════════════════════════════════════════════════════════════
        // CONVERSION EN ÉLITE - 1-3% de chance (sauf boss et spawns récursifs)
        // ═══════════════════════════════════════════════════════════════════
        if (!isBoss && !skipEliteConversion && plugin.getEliteManager() != null) {
            var eliteManager = plugin.getEliteManager();
            if (eliteManager.shouldBecomeElite(zoneId)) {
                eliteManager.convertToElite(entity, type.getDisplayName(), level, zoneId);
                zombie.setElite(true);
                // FIX: Mettre à jour le display name immédiatement après conversion
                updateZombieHealthDisplay(entity);
            }
        }

        return zombie;
    }

    /**
     * Spawn l'entité appropriée en fonction du type de zombie
     */
    private LivingEntity spawnEntityByType(ZombieType type, Location location, int level, int zoneId) {
        // Calculer les stats
        double finalHealth = type.calculateHealth(level);
        double finalDamage = type.calculateDamage(level);

        // ═══════════════════════════════════════════════════════════════════
        // NERF EARLY GAME - Réduction des dégâts pour les zones 1-10
        // Rend le début de partie plus accessible aux nouveaux joueurs
        // ═══════════════════════════════════════════════════════════════════
        if (zoneId >= 1 && zoneId <= 10 && !type.isBoss()) {
            // Réduction progressive: zone 1 = -30%, zone 10 = -5%
            // Formule: 30% - (zone - 1) * 2.78% ≈ 30% à 5%
            double earlyGameReduction = 0.30 - ((zoneId - 1) * 0.0278);
            finalDamage *= (1.0 - Math.max(0.05, earlyGameReduction));
        }

        double baseSpeed = type.getBaseSpeed();
        double speedMultiplier = 1.0 + (level * 0.005);
        double finalSpeed = Math.min(0.45, baseSpeed * speedMultiplier);

        String healthDisplay = formatHealthDisplay((int) finalHealth, (int) finalHealth);
        String tierColor = getTierColor(type.getTier());
        String customName = tierColor + type.getDisplayName() + " §7[Lv." + level + "] " + healthDisplay;

        // Variables finales pour les lambdas
        final double effectiveHealth = finalHealth;
        final double effectiveDamage = finalDamage;
        final double effectiveSpeed = finalSpeed;
        final String effectiveName = customName;

        // Spawn selon le type
        return switch (type) {
            case HUSK -> location.getWorld().spawn(location, Husk.class, entity -> {
                configureZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
            });
            case DROWNED, DROWNED_TRIDENT, DROWNER -> location.getWorld().spawn(location, Drowned.class, entity -> {
                configureZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Équiper un trident pour DROWNED_TRIDENT
                if (type == ZombieType.DROWNED_TRIDENT && entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });
            case ZOMBIE_VILLAGER -> location.getWorld().spawn(location, ZombieVillager.class, entity -> {
                configureZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
            });
            case ZOMBIFIED_PIGLIN -> location.getWorld().spawn(location, PigZombie.class, entity -> {
                configureZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                entity.setAngry(true);
                entity.setAnger(Integer.MAX_VALUE);
                // Équiper une épée d'or
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });
            case ZOGLIN -> location.getWorld().spawn(location, Zoglin.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
            });
            case RAVAGER_BEAST -> location.getWorld().spawn(location, Ravager.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
            });
            case PIGLIN_BRUTE -> location.getWorld().spawn(location, PiglinBrute.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                entity.setImmuneToZombification(true);
                // Équiper une hache d'or
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_AXE));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });

            // ═══════════════════════════════════════════════════════════════════
            // NOUVEAUX MOBS
            // ═══════════════════════════════════════════════════════════════════

            case SKELETON -> location.getWorld().spawn(location, Skeleton.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Équiper un arc
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    // Armure légère selon le niveau (avec trim)
                    if (level > 10) {
                        entity.getEquipment().setHelmet(createArmorWithTrim(Material.CHAINMAIL_HELMET, level));
                        entity.getEquipment().setHelmetDropChance(0);
                    }
                }
            });

            case STRAY -> location.getWorld().spawn(location, Stray.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Équiper un arc
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    // Armure de glace avec trim
                    entity.getEquipment().setHelmet(createArmorWithTrim(Material.LEATHER_HELMET, level));
                    entity.getEquipment().setHelmetDropChance(0);
                }
                // Résistance au froid
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            case RABID_WOLF -> location.getWorld().spawn(location, Wolf.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                entity.setAngry(true);
                entity.setTamed(false);
                // Le loup enragé a un collar rouge sang
                entity.setCollarColor(org.bukkit.DyeColor.RED);
                // Effets de rage
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            });

            case CREEPER -> location.getWorld().spawn(location, Creeper.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Le creeper ZombieZ est plus puissant
                entity.setExplosionRadius((int) (3 + level * 0.1));
                entity.setMaxFuseTicks(30); // 1.5 secondes
                // Chance d'être chargé selon le niveau
                if (level > 20 && Math.random() < 0.1) {
                    entity.setPowered(true);
                }
            });

            case EVOKER -> location.getWorld().spawn(location, Evoker.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // L'Evoker est un mage puissant
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            case PILLAGER -> location.getWorld().spawn(location, Pillager.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Équiper une arbalète
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.CROSSBOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });

            case VINDICATOR -> location.getWorld().spawn(location, Vindicator.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Équiper une hache de fer/diamant
                if (entity.getEquipment() != null) {
                    Material axeMaterial = level > 15 ? Material.DIAMOND_AXE : Material.IRON_AXE;
                    entity.getEquipment().setItemInMainHand(new ItemStack(axeMaterial));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
                // Le Vindicator est résistant
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            case GIANT_BOSS -> location.getWorld().spawn(location, Giant.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Le Giant est ÉNORME et lent mais dévastateur
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false));
            });

            // ═══════════════════════════════════════════════════════════════════
            // CREAKING BOSS (Chapitre 4 Étape 8 - Gardien de l'Arbre Maudit)
            // ═══════════════════════════════════════════════════════════════════
            case CREAKING_BOSS -> location.getWorld().spawn(location, Creaking.class, entity -> {
                configureNonZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
                // Le Creaking est une entité terrifiante du Pale Garden
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            default -> location.getWorld().spawn(location, Zombie.class, entity -> {
                configureZombieEntity(entity, type, level, effectiveHealth, effectiveDamage, effectiveSpeed, effectiveName);
            });
        };
    }

    /**
     * Configure une entité zombie avec les stats et équipement
     */
    private void configureZombieEntity(Zombie zombie, ZombieType type, int level,
                                        double finalHealth, double finalDamage, double finalSpeed, String customName) {
        zombie.setRemoveWhenFarAway(true);
        zombie.setShouldBurnInDay(false);

        // Réduire le temps d'invincibilité entre les coups (5 ticks = 0.25s au lieu de 10 = 0.5s)
        zombie.setMaximumNoDamageTicks(5);

        // Appliquer les attributs
        var maxHealthAttr = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(finalHealth);
            zombie.setHealth(finalHealth);
        }

        var damageAttr = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(finalDamage);
        }

        var speedAttr = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(finalSpeed);
        }

        var knockbackAttr = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(Math.min(0.8, type.getTier() * 0.1));
        }

        // ═══════════════════════════════════════════════════════════════════
        // VARIATION D'ÉCHELLE PROCÉDURALE (15% des mobs)
        // ═══════════════════════════════════════════════════════════════════
        applyProceduralScale(zombie, type);

        // Équipement basé sur le type
        applyZombieEquipment(zombie, type, level);

        // Effets de potion basés sur la catégorie
        applyZombieEffects(zombie, type);

        // Nom personnalisé
        zombie.setCustomName(customName);
        zombie.setCustomNameVisible(true);

        // Configuration additionnelle pour les boss
        if (type.isBoss()) {
            zombie.setRemoveWhenFarAway(false);
            zombie.setPersistent(true);
        }

        // Ajouter tag pour identification
        zombie.addScoreboardTag("zombiez_mob");
    }

    /**
     * Configure une entité non-zombie avec les stats
     */
    private void configureNonZombieEntity(LivingEntity entity, ZombieType type, int level,
                                           double finalHealth, double finalDamage, double finalSpeed, String customName) {
        entity.setRemoveWhenFarAway(true);

        // Réduire le temps d'invincibilité entre les coups (5 ticks = 0.25s au lieu de 10 = 0.5s)
        entity.setMaximumNoDamageTicks(5);

        // Empêcher les squelettes de brûler au soleil
        if (entity instanceof AbstractSkeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        }

        // Appliquer les attributs
        var maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(finalHealth);
            entity.setHealth(finalHealth);
        }

        var damageAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(finalDamage);
        }

        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(finalSpeed);
        }

        var knockbackAttr = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(Math.min(0.8, type.getTier() * 0.1));
        }

        // ═══════════════════════════════════════════════════════════════════
        // VARIATION D'ÉCHELLE PROCÉDURALE (15% des mobs)
        // ═══════════════════════════════════════════════════════════════════
        applyProceduralScale(entity, type);

        // Effets de potion basés sur la catégorie
        applyZombieEffectsToEntity(entity, type);

        // Nom personnalisé
        entity.setCustomName(customName);
        entity.setCustomNameVisible(true);

        // Configuration additionnelle pour les boss
        if (type.isBoss()) {
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
        }

        // Ajouter tag pour identification
        entity.addScoreboardTag("zombiez_mob");
    }

    /**
     * Applique des effets de potion à une entité non-zombie
     */
    private void applyZombieEffectsToEntity(LivingEntity entity, ZombieType type) {
        switch (type.getCategory()) {
            case TANK -> {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            case MELEE -> {
                if (type == ZombieType.ZOGLIN || type == ZombieType.PIGLIN_BRUTE) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE VARIATION D'ÉCHELLE PROCÉDURALE
    // ═══════════════════════════════════════════════════════════════════════════

    // Configuration de la variation d'échelle
    private static final double SCALE_VARIATION_CHANCE = 0.15; // 15% des mobs ont une taille modifiée
    private static final double SCALE_MIN = 0.6;  // -40% de la taille normale
    private static final double SCALE_MAX = 2.0;  // +100% de la taille normale
    private static final Random scaleRandom = new Random();

    /**
     * Applique une variation d'échelle procédurale à une entité
     * - 85% des mobs gardent leur taille normale (scale = 1.0)
     * - 15% des mobs ont une taille modifiée (0.6x à 2.0x)
     * - Les boss ne sont PAS affectés par cette variation (ils ont leurs propres échelles)
     *
     * @param entity L'entité à modifier
     * @param type Le type de zombie
     */
    private void applyProceduralScale(LivingEntity entity, ZombieType type) {
        // Les boss ont leurs propres échelles définies - ne pas modifier
        if (type.isBoss()) {
            return;
        }

        // 85% des mobs gardent leur taille normale
        if (scaleRandom.nextDouble() >= SCALE_VARIATION_CHANCE) {
            return;
        }

        // Générer une échelle aléatoire entre SCALE_MIN et SCALE_MAX
        // Distribution: légèrement biaisée vers les tailles normales
        // On utilise une distribution qui favorise les valeurs proches de 1.0
        double randomValue = scaleRandom.nextDouble();

        // Décider si on réduit ou agrandit (50/50)
        double scale;
        if (scaleRandom.nextBoolean()) {
            // Réduction: 0.6 à 1.0
            // Distribution linéaire pour les petits
            scale = SCALE_MIN + (randomValue * (1.0 - SCALE_MIN));
        } else {
            // Agrandissement: 1.0 à 2.0
            // Distribution légèrement biaisée vers les tailles modérées
            // On utilise une racine carrée pour favoriser les tailles moyennes
            scale = 1.0 + (Math.sqrt(randomValue) * (SCALE_MAX - 1.0));
        }

        // Appliquer l'échelle via l'attribut SCALE
        var scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(scale);
        }
    }

    /**
     * Applique l'équipement selon le type de zombie
     */
    private void applyZombieEquipment(Zombie zombie, ZombieType type, int level) {
        var equipment = zombie.getEquipment();
        if (equipment == null) return;

        switch (type.getCategory()) {
            case TANK -> {
                // Armure lourde pour les tanks
                equipment.setHelmet(createArmorWithTrim(level > 5 ? Material.DIAMOND_HELMET : Material.IRON_HELMET, level));
                equipment.setChestplate(createArmorWithTrim(level > 5 ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE, level));
                equipment.setLeggings(createArmorWithTrim(level > 5 ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS, level));
                equipment.setBoots(createArmorWithTrim(level > 5 ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS, level));
            }
            case MELEE -> {
                // Arme puissante pour les melee
                equipment.setItemInMainHand(new ItemStack(level > 7 ? Material.NETHERITE_AXE : Material.IRON_AXE));
                if (type == ZombieType.BERSERKER) {
                    equipment.setHelmet(createArmorWithTrim(Material.CHAINMAIL_HELMET, level));
                }
            }
            case ELITE -> {
                // Équipement complet pour les élites (toujours avec trim)
                equipment.setHelmet(createArmorWithTrim(Material.NETHERITE_HELMET, level, true));
                equipment.setChestplate(createArmorWithTrim(Material.NETHERITE_CHESTPLATE, level, true));
                equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            }
            case MINIBOSS, ZONE_BOSS, FINAL_BOSS -> {
                // Boss ont un équipement unique (toujours avec trim prestigieux)
                equipment.setHelmet(createArmorWithTrim(Material.NETHERITE_HELMET, level, true));
                equipment.setChestplate(createArmorWithTrim(Material.NETHERITE_CHESTPLATE, level, true));
                equipment.setLeggings(createArmorWithTrim(Material.NETHERITE_LEGGINGS, level, true));
                equipment.setBoots(createArmorWithTrim(Material.NETHERITE_BOOTS, level, true));
                equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
            }
            default -> {
                // Équipement basique basé sur le niveau
                if (level > 3) {
                    equipment.setHelmet(createArmorWithTrim(Material.LEATHER_HELMET, level));
                }
                if (level > 6) {
                    equipment.setChestplate(createArmorWithTrim(Material.LEATHER_CHESTPLATE, level));
                }
            }
        }

        // Empêcher de dropper l'équipement
        equipment.setHelmetDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setLeggingsDropChance(0);
        equipment.setBootsDropChance(0);
        equipment.setItemInMainHandDropChance(0);
    }

    /**
     * Crée une pièce d'armure avec un trim aléatoire
     * @param material Le matériau de l'armure
     * @param level Le niveau de la zone (affecte la probabilité et le type de trim)
     * @return L'ItemStack d'armure avec ou sans trim
     */
    private ItemStack createArmorWithTrim(Material material, int level) {
        return createArmorWithTrim(material, level, false);
    }

    /**
     * Crée une pièce d'armure avec un trim aléatoire
     * @param material Le matériau de l'armure
     * @param level Le niveau de la zone
     * @param guaranteedTrim Si true, le trim est garanti
     * @return L'ItemStack d'armure avec ou sans trim
     */
    private ItemStack createArmorWithTrim(Material material, int level, boolean guaranteedTrim) {
        ItemStack armor = new ItemStack(material);

        // Probabilité de trim basée sur le niveau (20% base + 5% par niveau, max 80%)
        double trimChance = guaranteedTrim ? 1.0 : Math.min(0.80, 0.20 + (level * 0.05));

        if (Math.random() > trimChance) {
            return armor;
        }

        // Générer un trim aléatoire pour les mobs
        ArmorTrimGenerator.TrimResult trimResult = generateMobTrim(level, guaranteedTrim);
        if (trimResult == null) {
            return armor;
        }

        // Appliquer le trim
        var meta = armor.getItemMeta();
        if (meta instanceof ArmorMeta armorMeta) {
            ArmorTrim trim = new ArmorTrim(trimResult.material(), trimResult.pattern());
            armorMeta.setTrim(trim);
            armor.setItemMeta(armorMeta);
        }

        return armor;
    }

    /**
     * Génère un trim adapté aux mobs basé sur le niveau
     */
    private ArmorTrimGenerator.TrimResult generateMobTrim(int level, boolean prestigious) {
        // Utiliser le générateur existant avec une rareté simulée basée sur le niveau
        com.rinaorc.zombiez.items.types.Rarity simulatedRarity;
        if (prestigious || level >= 8) {
            simulatedRarity = com.rinaorc.zombiez.items.types.Rarity.LEGENDARY;
        } else if (level >= 6) {
            simulatedRarity = com.rinaorc.zombiez.items.types.Rarity.EPIC;
        } else if (level >= 4) {
            simulatedRarity = com.rinaorc.zombiez.items.types.Rarity.RARE;
        } else if (level >= 2) {
            simulatedRarity = com.rinaorc.zombiez.items.types.Rarity.UNCOMMON;
        } else {
            simulatedRarity = com.rinaorc.zombiez.items.types.Rarity.COMMON;
        }

        // Forcer la génération d'un trim (on a déjà vérifié la probabilité)
        return ArmorTrimGenerator.getInstance().generateTrimForMob(simulatedRarity, level);
    }

    /**
     * Applique des effets de potion selon la catégorie et le type
     */
    private void applyZombieEffects(Zombie zombie, ZombieType type) {
        // Effets par catégorie
        switch (type.getCategory()) {
            case TANK -> {
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            case STEALTH -> {
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            }
            case ELEMENTAL -> {
                if (type == ZombieType.FROZEN || type == ZombieType.YETI || type == ZombieType.WENDIGO) {
                    zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }

        // Effets spécifiques par type de zombie
        switch (type) {
            case HUSK -> {
                // Husk: Résistant au soleil (déjà natif), inflige faim sur attaque
                // L'effet de faim est appliqué via le système de combat
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            case DROWNED, DROWNED_TRIDENT, DROWNER -> {
                // Noyés: Respiration aquatique, bonus en eau
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, false, false));
            }
            case ZOMBIE_VILLAGER -> {
                // Villageois zombie: Bonus de vitesse occasionnel
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 0, false, false));
            }
            case ZOMBIFIED_PIGLIN -> {
                // Piglin zombifié: Résistant au feu, force augmentée
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            }
        }
    }

    /**
     * Obtient la couleur selon le tier
     */
    private String getTierColor(int tier) {
        return switch (tier) {
            case 1 -> "§7"; // Gris
            case 2 -> "§a"; // Vert
            case 3 -> "§e"; // Jaune
            case 4 -> "§c"; // Rouge
            case 5 -> "§5"; // Violet
            default -> "§6"; // Or (boss)
        };
    }

    /**
     * Formate l'affichage de la vie: "❤ 100/100"
     */
    public static String formatHealthDisplay(int currentHealth, int maxHealth) {
        // Couleur basée sur le pourcentage de vie
        double healthPercent = (double) currentHealth / maxHealth;
        String healthColor;
        if (healthPercent > 0.6) {
            healthColor = "§a"; // Vert
        } else if (healthPercent > 0.3) {
            healthColor = "§e"; // Jaune
        } else {
            healthColor = "§c"; // Rouge
        }
        return healthColor + currentHealth + "§7/§a" + maxHealth + " §c❤";
    }

    /**
     * Met à jour le nom d'un zombie avec sa vie actuelle
     * Appelé par le ZombieListener quand un zombie prend des dégâts
     */
    public void updateZombieHealthDisplay(LivingEntity zombie) {
        if (!isZombieZMob(zombie)) return;

        ActiveZombie activeZombie = getActiveZombie(zombie.getUniqueId());
        if (activeZombie == null) return;

        // Récupérer les infos
        ZombieType type = activeZombie.getType();
        int level = activeZombie.getLevel();
        String displayName = type.getDisplayName();

        // Récupérer la vie actuelle
        var maxHealthAttr = zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        int currentHealth = (int) Math.ceil(zombie.getHealth());
        int maxHealth = maxHealthAttr != null ? (int) Math.ceil(maxHealthAttr.getValue()) : 20;

        // Si c'est un élite, utiliser le display name spécial doré
        if (activeZombie.isElite() && plugin.getEliteManager() != null) {
            var eliteDisplayName = plugin.getEliteManager().generateEliteDisplayName(
                displayName, level, currentHealth, maxHealth
            );
            zombie.customName(eliteDisplayName);
            return;
        }

        // Construire le nouveau nom
        String healthDisplay = formatHealthDisplay(currentHealth, maxHealth);
        String baseName = "§c" + displayName + " §7[Lv." + level + "] " + healthDisplay;

        // Si le zombie a un affix, l'ajouter au nom
        if (activeZombie.hasAffix()) {
            ZombieAffix affix = activeZombie.getAffix();
            zombie.setCustomName(affix.getColorCode() + affix.getPrefix() + " " + baseName);
        } else {
            zombie.setCustomName(baseName);
        }
    }

    /**
     * Met à jour le nom d'un zombie pour afficher 0 HP à sa mort
     * Appelé par le ZombieListener à la mort du zombie
     */
    public void updateZombieHealthDisplayOnDeath(LivingEntity zombie) {
        if (!isZombieZMob(zombie)) return;

        ActiveZombie activeZombie = getActiveZombie(zombie.getUniqueId());
        if (activeZombie == null) return;

        // Récupérer les infos
        ZombieType type = activeZombie.getType();
        int level = activeZombie.getLevel();
        String displayName = type.getDisplayName();

        // Récupérer la vie max (vie actuelle = 0 car mort)
        var maxHealthAttr = zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        int maxHealth = maxHealthAttr != null ? (int) Math.ceil(maxHealthAttr.getValue()) : 20;

        // Si c'est un élite, utiliser le display name spécial doré (0 HP)
        if (activeZombie.isElite() && plugin.getEliteManager() != null) {
            var eliteDisplayName = plugin.getEliteManager().generateEliteDisplayName(
                displayName, level, 0, maxHealth
            );
            zombie.customName(eliteDisplayName);
            return;
        }

        // Construire le nouveau nom avec 0 HP
        String healthDisplay = formatHealthDisplay(0, maxHealth);
        String baseName = "§c" + displayName + " §7[Lv." + level + "] " + healthDisplay;

        // Si le zombie a un affix, l'ajouter au nom
        if (activeZombie.hasAffix()) {
            ZombieAffix affix = activeZombie.getAffix();
            zombie.setCustomName(affix.getColorCode() + affix.getPrefix() + " " + baseName);
        } else {
            zombie.setCustomName(baseName);
        }
    }

    /**
     * Détermine si un zombie doit avoir un affix
     * Scaling progressif sur 50 zones
     */
    private boolean shouldHaveAffix(int zoneId, ZombieType type) {
        if (type.isBoss()) return false; // Les boss ont leurs propres mécaniques

        // Chance d'affix basée sur la zone (progressive sur 50 zones)
        double chance;
        if (zoneId <= 5) {
            // Zones 1-5: Pas d'affixes (tutorial)
            chance = 0.0;
        } else if (zoneId <= 10) {
            // Zones 6-10: 2-5%
            chance = 0.02 + ((zoneId - 5) * 0.006);
        } else if (zoneId <= 20) {
            // Zones 11-20: 5-15%
            chance = 0.05 + ((zoneId - 10) * 0.01);
        } else if (zoneId <= 30) {
            // Zones 21-30: 15-25%
            chance = 0.15 + ((zoneId - 20) * 0.01);
        } else if (zoneId <= 40) {
            // Zones 31-40: 25-35%
            chance = 0.25 + ((zoneId - 30) * 0.01);
        } else {
            // Zones 41-50: 35-45% (late game très dangereux)
            chance = 0.35 + ((zoneId - 40) * 0.01);
        }

        // Zone PVP (26) - Réduit les affixes
        if (zoneId == 26) {
            chance = 0.10;
        }

        return Math.random() < chance;
    }

    /**
     * Applique un affix aléatoire à un zombie
     */
    private void applyRandomAffix(ActiveZombie zombie, Entity entity, int zoneId) {
        ZombieAffix affix = affixRegistry.rollAffix(zoneId);
        if (affix == null) return;

        zombie.setAffix(affix);

        // Appliquer les effets de l'affix
        if (entity instanceof LivingEntity living) {
            affix.apply(living);

            // Mettre à jour le nom avec l'affix + vie
            updateZombieHealthDisplay(living);
        }
    }

    /**
     * Traite la mort d'un zombie
     */
    public void onZombieDeath(UUID entityId, Player killer) {
        ActiveZombie zombie = activeZombies.remove(entityId);
        if (zombie == null) return;

        // Nettoyer de la team de collision
        Entity entity = plugin.getServer().getEntity(entityId);
        if (entity != null) {
            removeFromNoCollisionTeam(entity);
        }

        // Notifier l'AIManager
        aiManager.onZombieDeath(entityId, killer);

        // Décrémenter le compteur de zone
        zombieCountByZone.merge(zombie.getZoneId(), -1, (a, b) -> Math.max(0, a + b));
        totalKilled++;

        // Donner les récompenses
        if (killer != null) {
            giveRewards(killer, zombie);
        }
    }

    /**
     * Donne les récompenses pour un kill
     */
    private void giveRewards(Player killer, ActiveZombie zombie) {
        ZombieType type = zombie.getType();
        int level = zombie.getLevel();

        // Points de base
        int basePoints = type.getBasePoints();
        int baseXP = type.getBaseXP();

        // Bonus de niveau
        double levelMultiplier = 1 + (level * 0.1);

        // Bonus d'affix
        double affixMultiplier = zombie.hasAffix() ? zombie.getAffix().getRewardMultiplier() : 1.0;

        // ═══════════════════════════════════════════════════════════════════
        // BONUS ÉLITE - x3 XP et Points
        // ═══════════════════════════════════════════════════════════════════
        double eliteMultiplier = 1.0;
        if (zombie.isElite()) {
            eliteMultiplier = com.rinaorc.zombiez.zombies.elite.EliteManager.XP_MULTIPLIER; // 3.0
        }

        // Calculer les récompenses finales
        int finalPoints = (int) (basePoints * levelMultiplier * affixMultiplier * eliteMultiplier);
        int finalXP = (int) (baseXP * levelMultiplier * affixMultiplier * eliteMultiplier);

        // Donner via EconomyManager
        plugin.getEconomyManager().addPoints(killer, finalPoints);
        plugin.getEconomyManager().addXP(killer, finalXP);

        // Traiter le loot
        processLoot(killer, zombie);

        // Statistiques
        var playerData = plugin.getPlayerDataManager().getPlayer(killer.getUniqueId());
        if (playerData != null) {
            playerData.incrementKills();
            playerData.addZombieKill();

            // Statistique élite
            if (zombie.isElite()) {
                playerData.addEliteKill();
            }

            // Statistique boss
            if (type.isBoss()) {
                playerData.addBossKill();
            }
        }
    }

    /**
     * Traite le loot d'un zombie avec système amélioré
     * - Intégration du Momentum (combo, streak, fever)
     * - Système de Pity (garantit drop après X kills sans loot)
     * - Jackpot drops (bonus pour hauts combos/streaks)
     */
    private void processLoot(Player killer, ActiveZombie zombie) {
        ZombieType type = zombie.getType();
        int zoneId = zombie.getZoneId();
        UUID playerId = killer.getUniqueId();

        // Obtenir la table de loot appropriée
        // NOTE: Les JOURNEY_BOSS (boss de quête réapparaissant rapidement) utilisent
        // le loot normal de leur zone pour éviter le farming excessif
        String tableId;
        if (type.isBoss()) {
            if (type.getCategory() == ZombieType.ZombieCategory.FINAL_BOSS) {
                tableId = "final_boss";
            } else if (type.getCategory() == ZombieType.ZombieCategory.ZONE_BOSS) {
                tableId = "zone_boss";
            } else if (type.getCategory() == ZombieType.ZombieCategory.JOURNEY_BOSS) {
                // Journey boss: loot normal de la zone (respawn rapide = pas de loot boss)
                tableId = "zombie_tier" + Math.max(1, zoneId);
            } else {
                tableId = "mini_boss";
            }
        } else {
            tableId = "zombie_tier" + type.getTier();
        }

        LootTable table = lootTableRegistry.getTable(tableId);
        if (table == null) {
            table = lootTableRegistry.getTableForZombieTier(type.getTier());
        }

        if (table == null) return;

        // === CALCUL DU BONUS DE LUCK TOTAL ===
        double luckBonus = plugin.getItemManager().getPlayerStat(killer,
            com.rinaorc.zombiez.items.types.StatType.LUCK) / 100.0;

        // Bonus d'affix du zombie
        if (zombie.hasAffix()) {
            luckBonus += zombie.getAffix().getLootBonus();
        }

        // === BONUS ÉLITE (+100% drop rate, +15% rare chance) ===
        if (zombie.isElite()) {
            luckBonus += com.rinaorc.zombiez.zombies.elite.EliteManager.DROP_RATE_MULTIPLIER - 1.0; // +100%
            luckBonus += com.rinaorc.zombiez.zombies.elite.EliteManager.RARE_CHANCE_BONUS; // +15%
        }

        // === BONUS MOMENTUM ===
        int combo = 0;
        int streak = 0;
        boolean inFever = false;
        if (plugin.getMomentumManager() != null) {
            combo = plugin.getMomentumManager().getCombo(killer);
            streak = plugin.getMomentumManager().getStreak(killer);
            inFever = plugin.getMomentumManager().isInFever(killer);

            // Combo bonus: +0.5% par combo (max +20%)
            luckBonus += Math.min(0.20, combo * 0.005);

            // Streak bonus: +0.25% par streak (max +12.5%)
            luckBonus += Math.min(0.125, streak * 0.0025);

            // FEVER bonus: +35% de luck
            if (inFever) {
                luckBonus += 0.35;
            }
        }

        // === SYSTÈME PITY ===
        int killsNoDrop = killsWithoutDrop.getOrDefault(playerId, 0);
        boolean forceDrop = killsNoDrop >= PITY_THRESHOLD;

        // Bonus progressif de pity (+2% par kill sans drop)
        luckBonus += killsNoDrop * PITY_BONUS_PER_KILL;

        // === GÉNÉRATION DU LOOT ===
        var items = table.generateLoot(zoneId, luckBonus);
        Location dropLoc = killer.getLocation();

        // Pity: forcer un drop si threshold atteint
        if (items.isEmpty() && forceDrop) {
            // Forcer un drop avec rareté garantie minimum UNCOMMON
            var forcedItem = com.rinaorc.zombiez.items.generator.ItemGenerator.getInstance()
                .generate(zoneId, com.rinaorc.zombiez.items.types.Rarity.UNCOMMON, luckBonus);
            if (forcedItem != null) {
                items.add(forcedItem);
            }
        }

        // === JACKPOT SYSTEM ===
        boolean jackpotTriggered = false;
        if ((combo >= JACKPOT_COMBO_THRESHOLD || streak >= JACKPOT_STREAK_THRESHOLD)
                && Math.random() < JACKPOT_CHANCE) {
            jackpotTriggered = true;

            // Générer 1-2 items bonus avec rareté augmentée
            int bonusItems = Math.random() < 0.3 ? 2 : 1;
            double jackpotLuck = luckBonus + 0.3; // +30% luck pour le jackpot

            for (int i = 0; i < bonusItems; i++) {
                var bonusItem = table.generateLoot(zoneId, jackpotLuck);
                items.addAll(bonusItem);
            }
        }

        // === DROP DES ITEMS ===
        if (!items.isEmpty()) {
            // Reset pity counter
            killsWithoutDrop.put(playerId, 0);

            for (var item : items) {
                plugin.getItemManager().dropItem(dropLoc, item);
            }

            // Effets visuels/sonores selon la quantité et qualité
            if (jackpotTriggered) {
                // Jackpot: effets spéciaux
                killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, dropLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            } else if (items.size() >= 2) {
                // Multiple drops: petit effet
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
        } else {
            // Incrémenter pity counter
            killsWithoutDrop.merge(playerId, 1, Integer::sum);
        }

        // Tenter de drop un consommable
        if (plugin.getConsumableManager() != null) {
            plugin.getConsumableManager().tryDropConsumable(dropLoc, zoneId, type, luckBonus);
        }

        // === DROP NOURRITURE ZOMBIE (4% de base, qualité inférieure aux mobs passifs) ===
        double foodDropChance = 0.04 + (luckBonus * 0.01); // 4% + bonus luck
        if (Math.random() < foodDropChance && plugin.getPassiveMobManager() != null) {
            FoodItem zombieFood = plugin.getPassiveMobManager().getFoodRegistry().getRandomZombieFood();
            if (zombieFood != null) {
                int amount = 1 + (Math.random() < 0.3 ? 1 : 0); // 30% de chance d'en avoir 2
                ItemStack foodItem = zombieFood.createItemStack(amount);
                Item droppedFood = dropLoc.getWorld().dropItemNaturally(dropLoc, foodItem);

                // Appliquer le glow et le nom visible comme les armes/armures
                org.bukkit.ChatColor glowColor = getFoodRarityChatColor(zombieFood.getRarity());
                plugin.getItemManager().applyDroppedItemEffects(droppedFood, zombieFood.getDisplayName(), glowColor);
            }
        }

        // === DROP BANDAGE RARE (1.25% de base) ===
        double bandageDropChance = 0.0125 + (luckBonus * 0.0025); // 1.25% + petit bonus luck
        if (Math.random() < bandageDropChance && plugin.getConsumableManager() != null) {
            // Créer un bandage de rareté aléatoire (principalement common/uncommon)
            ConsumableRarity bandageRarity = Math.random() < 0.8 ? ConsumableRarity.COMMON :
                                             (Math.random() < 0.7 ? ConsumableRarity.UNCOMMON : ConsumableRarity.RARE);
            Consumable bandage = new Consumable(ConsumableType.BANDAGE, bandageRarity, zoneId);
            plugin.getConsumableManager().dropConsumable(dropLoc, bandage);
        }
    }

    /**
     * Convertit une FoodRarity en ChatColor pour le glow
     */
    private ChatColor getFoodRarityChatColor(FoodItem.FoodRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Obtient un zombie actif par son UUID
     */
    public ActiveZombie getActiveZombie(UUID entityId) {
        return activeZombies.get(entityId);
    }

    /**
     * Inflige des dégâts à un zombie ZombieZ
     * @param attacker Le joueur qui inflige les dégâts
     * @param zombie Le zombie actif
     * @param damage Le montant de dégâts
     * @param damageType Le type de dégâts (pour effets spéciaux futurs)
     * @param isCritical Si c'est un coup critique
     */
    public void damageZombie(Player attacker, ActiveZombie zombie, double damage, DamageType damageType, boolean isCritical) {
        Entity entity = plugin.getServer().getEntity(zombie.getEntityId());
        if (entity instanceof LivingEntity living && !living.isDead()) {
            living.damage(damage, attacker);

            // Mettre à jour l'affichage de vie
            updateZombieHealthDisplay(living);
        }
    }

    /**
     * Obtient le type de zombie depuis une entité
     */
    public ZombieType getZombieType(Entity entity) {
        if (!entity.hasMetadata("zombiez_type")) {
            return null;
        }
        
        String typeName = entity.getMetadata("zombiez_type").get(0).asString();
        return ZombieType.valueOf(typeName);
    }

    /**
     * Vérifie si une entité est un zombie ZombieZ
     * Utilise le PDC en priorité (ultra-performant), puis fallback sur metadata
     */
    public boolean isZombieZMob(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        // Vérification PDC (prioritaire - O(1) ultra-rapide)
        if (living.getPersistentDataContainer().has(pdcMobKey, PersistentDataType.BYTE)) {
            return true;
        }

        // Fallback sur metadata (compatibilité avec anciens mobs)
        return entity.hasMetadata("zombiez_type");
    }

    /**
     * Vérifie si une entité est un zombie ZombieZ via PDC uniquement
     * Plus rapide que isZombieZMob() car pas de fallback
     */
    public boolean isZombieZMobPDC(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        return living.getPersistentDataContainer().has(pdcMobKey, PersistentDataType.BYTE);
    }

    /**
     * Obtient le nombre de zombies actifs dans une zone
     */
    public int getZombieCount(int zoneId) {
        return zombieCountByZone.getOrDefault(zoneId, 0);
    }

    /**
     * Obtient le nombre total de zombies actifs
     */
    public int getTotalZombieCount() {
        return activeZombies.size();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME CLEARLAG OPTIMISÉ
    // ═══════════════════════════════════════════════════════════════════════════

    // Configuration du clearlag
    private static final double PLAYER_NEARBY_RADIUS = 64.0;     // Rayon pour joueur proche
    private static final double ISOLATION_RADIUS = 20.0;         // Rayon pour vérifier isolation
    private static final int MIN_MOBS_FOR_GROUP = 2;             // Minimum pour considérer un groupe
    private static final long MOB_MAX_AGE_MS = 5 * 60 * 1000;    // 5 minutes max d'existence si isolé
    private static final int CLEANUP_BATCH_SIZE = 50;            // Nombre max de mobs à traiter par tick

    /**
     * Système de clearlag principal - appelé toutes les 30 secondes
     * Optimisé pour éviter les lags
     */
    public void cleanupDistantZombies() {
        List<UUID> toRemove = new ArrayList<>();
        int processed = 0;

        for (var entry : activeZombies.entrySet()) {
            if (processed >= CLEANUP_BATCH_SIZE) break; // Limiter par batch
            processed++;

            UUID entityId = entry.getKey();
            ActiveZombie zombie = entry.getValue();
            Entity entity = plugin.getServer().getEntity(entityId);

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 1: Entité invalide ou morte
            // ═══════════════════════════════════════════════════════════════════
            if (entity == null || !entity.isValid() || entity.isDead()) {
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 2: Chunk non chargé
            // ═══════════════════════════════════════════════════════════════════
            if (!entity.getLocation().getChunk().isLoaded()) {
                entity.remove();
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 3: Pas de joueur à proximité
            // ═══════════════════════════════════════════════════════════════════
            if (!hasPlayerNearby(entity, PLAYER_NEARBY_RADIUS)) {
                entity.remove();
                toRemove.add(entityId);
                continue;
            }

            // ═══════════════════════════════════════════════════════════════════
            // VÉRIFICATION 4: Mob isolé depuis trop longtemps (pas un boss)
            // ═══════════════════════════════════════════════════════════════════
            if (!zombie.getType().isBoss()) {
                long age = System.currentTimeMillis() - zombie.getSpawnTime();
                if (age > MOB_MAX_AGE_MS && isIsolated(entity)) {
                    entity.remove();
                    toRemove.add(entityId);
                }
            }
        }

        // Nettoyer les entrées
        for (UUID id : toRemove) {
            ActiveZombie zombie = activeZombies.remove(id);
            if (zombie != null) {
                zombieCountByZone.merge(zombie.getZoneId(), -1, (a, b) -> Math.max(0, a + b));
            }
        }

        // Log si beaucoup de mobs nettoyés
        if (toRemove.size() > 10) {
            plugin.getLogger().info("[Clearlag] " + toRemove.size() + " mobs nettoyés");
        }
    }

    /**
     * Vérifie si un joueur est à proximité d'une entité
     * Optimisé: utilise getNearbyEntities avec filtrage rapide
     */
    private boolean hasPlayerNearby(Entity entity, double radius) {
        // Vérification rapide des joueurs en ligne
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(entity.getWorld())) {
                double distSq = player.getLocation().distanceSquared(entity.getLocation());
                if (distSq <= radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vérifie si un mob est isolé (pas d'autres mobs proches)
     * Les mobs isolés sont candidats au nettoyage s'ils sont vieux
     */
    private boolean isIsolated(Entity entity) {
        int nearbyMobs = 0;

        for (Entity nearby : entity.getWorld().getNearbyEntities(
                entity.getLocation(), ISOLATION_RADIUS, ISOLATION_RADIUS, ISOLATION_RADIUS)) {
            if (nearby.getUniqueId().equals(entity.getUniqueId())) continue;
            if (isZombieZMob(nearby)) {
                nearbyMobs++;
                if (nearbyMobs >= MIN_MOBS_FOR_GROUP) {
                    return false; // Pas isolé, fait partie d'un groupe
                }
            }
        }
        return true; // Isolé
    }

    /**
     * Force un nettoyage complet de tous les mobs sans joueur proche
     * À appeler manuellement par un admin ou lors d'événements spéciaux
     */
    public int forceCleanupAllDistantMobs() {
        List<UUID> toRemove = new ArrayList<>();

        for (var entry : activeZombies.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());

            if (entity == null || !entity.isValid()) {
                toRemove.add(entry.getKey());
                continue;
            }

            if (!hasPlayerNearby(entity, PLAYER_NEARBY_RADIUS)) {
                entity.remove();
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            ActiveZombie zombie = activeZombies.remove(id);
            if (zombie != null) {
                zombieCountByZone.merge(zombie.getZoneId(), -1, (a, b) -> Math.max(0, a + b));
            }
        }

        return toRemove.size();
    }

    /**
     * Nettoie tous les mobs dans les chunks non chargés
     * Appelé périodiquement ou lors d'un cleanup manuel
     */
    public int cleanupUnloadedChunks() {
        List<UUID> toRemove = new ArrayList<>();

        for (var entry : activeZombies.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());

            if (entity == null) {
                toRemove.add(entry.getKey());
                continue;
            }

            if (!entity.getLocation().getChunk().isLoaded()) {
                entity.remove();
                toRemove.add(entry.getKey());
            }
        }

        for (UUID id : toRemove) {
            ActiveZombie zombie = activeZombies.remove(id);
            if (zombie != null) {
                zombieCountByZone.merge(zombie.getZoneId(), -1, (a, b) -> Math.max(0, a + b));
            }
        }

        return toRemove.size();
    }

    /**
     * Nettoie les mobs les plus vieux dans une zone si elle dépasse sa limite
     * Utile pour forcer la rotation des mobs
     */
    public int cleanupOldestMobsInZone(int zoneId, int targetCount) {
        int currentCount = zombieCountByZone.getOrDefault(zoneId, 0);
        if (currentCount <= targetCount) return 0;

        int toCleanup = currentCount - targetCount;
        List<Map.Entry<UUID, ActiveZombie>> zoneMobs = new ArrayList<>();

        // Collecter les mobs de cette zone
        for (var entry : activeZombies.entrySet()) {
            if (entry.getValue().getZoneId() == zoneId) {
                zoneMobs.add(entry);
            }
        }

        // Trier par temps de spawn (plus vieux en premier)
        zoneMobs.sort(Comparator.comparingLong(e -> e.getValue().getSpawnTime()));

        int removed = 0;
        for (var entry : zoneMobs) {
            if (removed >= toCleanup) break;

            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (entity != null && entity.isValid()) {
                // Ne pas supprimer les boss
                if (entry.getValue().getType().isBoss()) continue;

                entity.remove();
            }
            activeZombies.remove(entry.getKey());
            zombieCountByZone.merge(zoneId, -1, (a, b) -> Math.max(0, a + b));
            removed++;
        }

        return removed;
    }

    /**
     * Obtient les statistiques du manager
     */
    public String getStats() {
        return String.format("Active: %d | Spawned: %d | Killed: %d",
            activeZombies.size(), totalSpawned, totalKilled);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYNCHRONISATION DES COMPTEURS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Synchronise les compteurs internes avec les entités réelles
     * À appeler périodiquement pour corriger les désynchronisations
     * @return Le nombre d'entrées invalides corrigées
     */
    public int synchronizeCounters() {
        List<UUID> invalidEntries = new ArrayList<>();
        Map<Integer, Integer> actualCounts = new HashMap<>();

        // Vérifier chaque entrée
        for (var entry : activeZombies.entrySet()) {
            UUID entityId = entry.getKey();
            ActiveZombie zombie = entry.getValue();

            Entity entity = plugin.getServer().getEntity(entityId);

            // Entité invalide ou morte
            if (entity == null || !entity.isValid() || entity.isDead()) {
                invalidEntries.add(entityId);
            } else {
                // Compter par zone (entités valides uniquement)
                actualCounts.merge(zombie.getZoneId(), 1, Integer::sum);
            }
        }

        // Supprimer les entrées invalides
        for (UUID id : invalidEntries) {
            ActiveZombie zombie = activeZombies.remove(id);
            if (zombie != null) {
                // Ne pas décrémenter ici car on va resynchroniser
            }
        }

        // Resynchroniser les compteurs de zone avec les comptages réels
        for (int zoneId = 0; zoneId <= 50; zoneId++) {
            int actualCount = actualCounts.getOrDefault(zoneId, 0);
            int trackedCount = zombieCountByZone.getOrDefault(zoneId, 0);

            if (actualCount != trackedCount) {
                zombieCountByZone.put(zoneId, actualCount);
            }
        }

        return invalidEntries.size();
    }

    /**
     * Force la suppression de TOUS les mobs dans le monde
     * Utilisé en cas d'urgence (lag, crash imminent)
     * @return Le nombre de mobs supprimés
     */
    public int forceRemoveAllMobs() {
        int removed = 0;

        for (var entry : activeZombies.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            if (entity != null && entity.isValid()) {
                removeFromNoCollisionTeam(entity);
                entity.remove();
                removed++;
            }
        }

        // Clear toutes les maps
        activeZombies.clear();
        zombieCountByZone.clear();
        lastSpawnByZone.clear();

        plugin.getLogger().warning("[ZombieManager] Force remove: " + removed + " mobs supprimés");
        return removed;
    }

    /**
     * Obtient le nombre de mobs par zone (copie pour lecture)
     */
    public Map<Integer, Integer> getZoneCountsSnapshot() {
        return new HashMap<>(zombieCountByZone);
    }

    /**
     * Représente un zombie actif avec ses données
     */
    @lombok.Getter
    public static class ActiveZombie {
        private final UUID entityId;
        private final ZombieType type;
        private final int level;
        private final int zoneId;
        private final long spawnTime;

        @lombok.Setter
        private ZombieAffix affix;

        @lombok.Setter
        private boolean elite;

        public ActiveZombie(UUID entityId, ZombieType type, int level, int zoneId) {
            this.entityId = entityId;
            this.type = type;
            this.level = level;
            this.zoneId = zoneId;
            this.spawnTime = System.currentTimeMillis();
            this.elite = false;
        }

        public boolean hasAffix() {
            return affix != null;
        }
    }
}
