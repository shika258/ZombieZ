package com.rinaorc.zombiez.zombies;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.LootTable;
import com.rinaorc.zombiez.zombies.affixes.ZombieAffix;
import com.rinaorc.zombiez.zombies.ai.ZombieAI;
import com.rinaorc.zombiez.zombies.ai.ZombieAIManager;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Giant;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    public ZombieManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.affixRegistry = ZombieAffix.ZombieAffixRegistry.getInstance();
        this.lootTableRegistry = LootTable.LootTableRegistry.getInstance();
        this.aiManager = new ZombieAIManager(plugin);
        this.activeZombies = new ConcurrentHashMap<>();
        this.zombieCountByZone = new ConcurrentHashMap<>();
        this.maxZombiesPerZone = new ConcurrentHashMap<>();
        this.lastSpawnByZone = new ConcurrentHashMap<>();

        initializeZoneLimits();
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
        int zoneId = plugin.getZoneManager().getZoneAt(location).getId();

        // Vérifier la limite
        int currentCount = zombieCountByZone.getOrDefault(zoneId, 0);
        int maxCount = maxZombiesPerZone.getOrDefault(zoneId, 50);

        if (currentCount >= maxCount) {
            return null;
        }

        // Vérifier le cooldown de spawn
        long lastSpawn = lastSpawnByZone.getOrDefault(zoneId, 0L);
        if (System.currentTimeMillis() - lastSpawn < 500) { // 500ms minimum entre spawns
            return null;
        }

        // Spawner l'entité personnalisée (peut être zombie ou autre)
        LivingEntity entity = spawnEntityByType(type, location, level);

        if (entity == null) {
            return null;
        }

        // Créer l'ActiveZombie
        ActiveZombie zombie = new ActiveZombie(entity.getUniqueId(), type, level, zoneId);

        // Appliquer les affixes si applicable
        if (shouldHaveAffix(zoneId, type)) {
            applyRandomAffix(zombie, entity, zoneId);
        }

        // Stocker les métadonnées
        entity.setMetadata("zombiez_type", new FixedMetadataValue(plugin, type.name()));
        entity.setMetadata("zombiez_level", new FixedMetadataValue(plugin, level));
        entity.setMetadata("zombiez_zone", new FixedMetadataValue(plugin, zoneId));

        if (zombie.hasAffix()) {
            entity.setMetadata("zombiez_affix", new FixedMetadataValue(plugin, zombie.getAffix().getId()));
        }

        // Créer l'IA pour ce zombie (seulement si c'est un Zombie)
        if (entity instanceof Zombie zombieEntity) {
            aiManager.createAI(zombieEntity, type, level);
        }

        // Enregistrer
        activeZombies.put(entity.getUniqueId(), zombie);
        zombieCountByZone.merge(zoneId, 1, Integer::sum);
        lastSpawnByZone.put(zoneId, System.currentTimeMillis());
        totalSpawned++;

        return zombie;
    }

    /**
     * Spawn l'entité appropriée en fonction du type de zombie
     */
    private LivingEntity spawnEntityByType(ZombieType type, Location location, int level) {
        // Calculer les stats
        double finalHealth = type.calculateHealth(level);
        double finalDamage = type.calculateDamage(level);
        double baseSpeed = type.getBaseSpeed();
        double speedMultiplier = 1.0 + (level * 0.005);
        double finalSpeed = Math.min(0.45, baseSpeed * speedMultiplier);

        String healthDisplay = formatHealthDisplay((int) finalHealth, (int) finalHealth);
        String tierColor = getTierColor(type.getTier());
        String customName = tierColor + type.getDisplayName() + " §7[Lv." + level + "] " + healthDisplay;

        // Spawn selon le type
        return switch (type) {
            case HUSK -> location.getWorld().spawn(location, Husk.class, entity -> {
                configureZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
            });
            case DROWNED, DROWNED_TRIDENT, DROWNER -> location.getWorld().spawn(location, Drowned.class, entity -> {
                configureZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Équiper un trident pour DROWNED_TRIDENT
                if (type == ZombieType.DROWNED_TRIDENT && entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });
            case ZOMBIE_VILLAGER -> location.getWorld().spawn(location, ZombieVillager.class, entity -> {
                configureZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
            });
            case ZOMBIFIED_PIGLIN -> location.getWorld().spawn(location, PigZombie.class, entity -> {
                configureZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                entity.setAngry(true);
                entity.setAnger(Integer.MAX_VALUE);
                // Équiper une épée d'or
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });
            case ZOGLIN -> location.getWorld().spawn(location, Zoglin.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
            });
            case RAVAGER_BEAST -> location.getWorld().spawn(location, Ravager.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
            });
            case PIGLIN_BRUTE -> location.getWorld().spawn(location, PiglinBrute.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
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
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Équiper un arc
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    // Armure légère selon le niveau
                    if (level > 10) {
                        entity.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                        entity.getEquipment().setHelmetDropChance(0);
                    }
                }
            });

            case STRAY -> location.getWorld().spawn(location, Stray.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Équiper un arc
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                    // Armure de glace (cuir bleu)
                    ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
                    entity.getEquipment().setHelmet(helmet);
                    entity.getEquipment().setHelmetDropChance(0);
                }
                // Résistance au froid
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            case RABID_WOLF -> location.getWorld().spawn(location, Wolf.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                entity.setAngry(true);
                entity.setTamed(false);
                // Le loup enragé a un collar rouge sang
                entity.setCollarColor(org.bukkit.DyeColor.RED);
                // Effets de rage
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            });

            case CREEPER -> location.getWorld().spawn(location, Creeper.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Le creeper ZombieZ est plus puissant
                entity.setExplosionRadius((int) (3 + level * 0.1));
                entity.setMaxFuseTicks(30); // 1.5 secondes
                // Chance d'être chargé selon le niveau
                if (level > 20 && Math.random() < 0.1) {
                    entity.setPowered(true);
                }
            });

            case EVOKER -> location.getWorld().spawn(location, Evoker.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // L'Evoker est un mage puissant
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            });

            case PILLAGER -> location.getWorld().spawn(location, Pillager.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Équiper une arbalète
                if (entity.getEquipment() != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(Material.CROSSBOW));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
            });

            case VINDICATOR -> location.getWorld().spawn(location, Vindicator.class, entity -> {
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
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
                configureNonZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
                // Le Giant est ÉNORME et lent mais dévastateur
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false));
            });

            default -> location.getWorld().spawn(location, Zombie.class, entity -> {
                configureZombieEntity(entity, type, level, finalHealth, finalDamage, finalSpeed, customName);
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

    /**
     * Applique l'équipement selon le type de zombie
     */
    private void applyZombieEquipment(Zombie zombie, ZombieType type, int level) {
        var equipment = zombie.getEquipment();
        if (equipment == null) return;

        switch (type.getCategory()) {
            case TANK -> {
                // Armure lourde pour les tanks
                equipment.setHelmet(new ItemStack(level > 5 ? Material.DIAMOND_HELMET : Material.IRON_HELMET));
                equipment.setChestplate(new ItemStack(level > 5 ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE));
                equipment.setLeggings(new ItemStack(level > 5 ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS));
                equipment.setBoots(new ItemStack(level > 5 ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS));
            }
            case MELEE -> {
                // Arme puissante pour les melee
                equipment.setItemInMainHand(new ItemStack(level > 7 ? Material.NETHERITE_AXE : Material.IRON_AXE));
                if (type == ZombieType.BERSERKER) {
                    equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                }
            }
            case ELITE -> {
                // Équipement complet pour les élites
                equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            }
            case MINIBOSS, ZONE_BOSS, FINAL_BOSS -> {
                // Boss ont un équipement unique
                equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
            }
            default -> {
                // Équipement basique basé sur le niveau
                if (level > 3) {
                    equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                }
                if (level > 6) {
                    equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
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
        
        // Calculer les récompenses finales
        int finalPoints = (int) (basePoints * levelMultiplier * affixMultiplier);
        int finalXP = (int) (baseXP * levelMultiplier * affixMultiplier);
        
        // Donner via EconomyManager
        plugin.getEconomyManager().addPoints(killer, finalPoints);
        plugin.getEconomyManager().addXP(killer, finalXP);
        
        // Traiter le loot
        processLoot(killer, zombie);
        
        // Statistiques
        var playerData = plugin.getPlayerDataManager().getPlayer(killer.getUniqueId());
        if (playerData != null) {
            playerData.incrementKills();
        }
    }

    /**
     * Traite le loot d'un zombie
     */
    private void processLoot(Player killer, ActiveZombie zombie) {
        ZombieType type = zombie.getType();
        int zoneId = zombie.getZoneId();

        // Obtenir la table de loot appropriée
        String tableId = type.isBoss() ?
            (type.getCategory() == ZombieType.ZombieCategory.FINAL_BOSS ? "final_boss" :
             type.getCategory() == ZombieType.ZombieCategory.ZONE_BOSS ? "zone_boss" : "mini_boss") :
            "zombie_tier" + type.getTier();

        LootTable table = lootTableRegistry.getTable(tableId);
        if (table == null) {
            table = lootTableRegistry.getTableForZombieTier(type.getTier());
        }

        if (table == null) return;

        // Calculer le bonus de luck
        double luckBonus = plugin.getItemManager().getPlayerStat(killer,
            com.rinaorc.zombiez.items.types.StatType.LUCK) / 100.0;

        // Bonus d'affix
        if (zombie.hasAffix()) {
            luckBonus += zombie.getAffix().getLootBonus();
        }

        // Générer et dropper le loot
        var items = table.generateLoot(zoneId, luckBonus);
        Location dropLoc = killer.getLocation();

        for (var item : items) {
            plugin.getItemManager().dropItem(dropLoc, item);
        }

        // Tenter de drop un consommable
        if (plugin.getConsumableManager() != null) {
            plugin.getConsumableManager().tryDropConsumable(dropLoc, zoneId, type, luckBonus);
        }
    }

    /**
     * Obtient un zombie actif par son UUID
     */
    public ActiveZombie getActiveZombie(UUID entityId) {
        return activeZombies.get(entityId);
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
     */
    public boolean isZombieZMob(Entity entity) {
        return entity.hasMetadata("zombiez_type");
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

        public ActiveZombie(UUID entityId, ZombieType type, int level, int zoneId) {
            this.entityId = entityId;
            this.type = type;
            this.level = level;
            this.zoneId = zoneId;
            this.spawnTime = System.currentTimeMillis();
        }

        public boolean hasAffix() {
            return affix != null;
        }
    }
}
