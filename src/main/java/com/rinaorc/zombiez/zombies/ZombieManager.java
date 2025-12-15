package com.rinaorc.zombiez.zombies;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.LootTable;
import com.rinaorc.zombiez.utils.HealthBarUtils;
import com.rinaorc.zombiez.zombies.affixes.ZombieAffix;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager principal pour les zombies ZombieZ
 * Gère le spawn, les affixes, et l'intégration MythicMobs
 */
public class ZombieManager {

    private final ZombieZPlugin plugin;
    private final ZombieAffix.ZombieAffixRegistry affixRegistry;
    private final LootTable.LootTableRegistry lootTableRegistry;
    
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
        this.activeZombies = new ConcurrentHashMap<>();
        this.zombieCountByZone = new ConcurrentHashMap<>();
        this.maxZombiesPerZone = new ConcurrentHashMap<>();
        this.lastSpawnByZone = new ConcurrentHashMap<>();
        
        initializeZoneLimits();
    }

    /**
     * Initialise les limites de zombies par zone
     */
    private void initializeZoneLimits() {
        // Zone 0 (Spawn) - Pas de zombies
        maxZombiesPerZone.put(0, 0);
        
        // Zones progressives
        maxZombiesPerZone.put(1, 30);   // Village
        maxZombiesPerZone.put(2, 40);   // Plaines
        maxZombiesPerZone.put(3, 45);   // Désert
        maxZombiesPerZone.put(4, 40);   // Forêt Sombre
        maxZombiesPerZone.put(5, 50);   // Marécages
        maxZombiesPerZone.put(6, 60);   // PvP Arena
        maxZombiesPerZone.put(7, 45);   // Montagnes
        maxZombiesPerZone.put(8, 40);   // Toundra
        maxZombiesPerZone.put(9, 35);   // Terres Corrompues
        maxZombiesPerZone.put(10, 30);  // Enfer
        maxZombiesPerZone.put(11, 20);  // Citadelle Finale
    }

    /**
     * Spawn un zombie via MythicMobs
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
        
        // Spawner via MythicMobs (simulation - nécessite l'API MythicMobs)
        Entity entity = spawnMythicMob(type.getMythicMobId(), location, level);
        
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
        if (entity instanceof LivingEntity living) {
            living.setMetadata("zombiez_type", new FixedMetadataValue(plugin, type.name()));
            living.setMetadata("zombiez_level", new FixedMetadataValue(plugin, level));
            living.setMetadata("zombiez_zone", new FixedMetadataValue(plugin, zoneId));
            
            if (zombie.hasAffix()) {
                living.setMetadata("zombiez_affix", new FixedMetadataValue(plugin, zombie.getAffix().getId()));
            }
        }
        
        // Enregistrer
        activeZombies.put(entity.getUniqueId(), zombie);
        zombieCountByZone.merge(zoneId, 1, Integer::sum);
        lastSpawnByZone.put(zoneId, System.currentTimeMillis());
        totalSpawned++;
        
        return zombie;
    }

    /**
     * Spawner MythicMob (stub - nécessite l'API MythicMobs)
     */
    private Entity spawnMythicMob(String mobId, Location location, int level) {
        // TODO: Intégration avec l'API MythicMobs
        // io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager()
        //     .spawnMob(mobId, location, level);

        // Pour l'instant, on spawn un zombie vanilla comme placeholder
        if (location.getWorld() == null) return null;

        String baseName = mobId.replace("ZZ_", "");

        return location.getWorld().spawn(location, org.bukkit.entity.Zombie.class, zombie -> {
            // Ajuster la vie en fonction du niveau
            double baseHealth = 20 + (level * 2);
            zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            zombie.setHealth(baseHealth);

            // Ajuster la vitesse selon le type (simulation d'IA différente)
            double speedBonus = getSpeedBonusForType(baseName);
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.23 * (1 + speedBonus));

            // Ajuster les dégâts selon le niveau
            double attackDamage = 3 + (level * 0.5);
            zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(attackDamage);

            // DÉSACTIVER LA BRÛLURE AU SOLEIL
            zombie.setShouldBurnInDay(false);

            // Créer le nom avec barre de vie
            String nameWithHealth = HealthBarUtils.createZombieNameWithHealth(
                baseName, level, baseHealth, baseHealth, null, null
            );
            zombie.setCustomName(nameWithHealth);
            zombie.setCustomNameVisible(true);
            zombie.setRemoveWhenFarAway(true);

            // Appliquer des effets selon le type de zombie
            applyZombieTypeEffects(zombie, baseName, level);
        });
    }

    /**
     * Obtient le bonus de vitesse selon le type de zombie
     */
    private double getSpeedBonusForType(String typeName) {
        return switch (typeName.toUpperCase()) {
            case "RUNNER", "CRAWLER" -> 0.4;      // 40% plus rapide
            case "SHADOW", "LURKER" -> 0.2;       // 20% plus rapide
            case "BERSERKER" -> 0.3;              // 30% plus rapide
            case "GIANT", "BLOATER" -> -0.3;     // 30% plus lent
            case "YETI", "ABOMINATION" -> -0.2;  // 20% plus lent
            default -> 0.0;
        };
    }

    /**
     * Applique des effets spéciaux selon le type de zombie
     */
    private void applyZombieTypeEffects(org.bukkit.entity.Zombie zombie, String typeName, int level) {
        switch (typeName.toUpperCase()) {
            case "GIANT", "YETI", "ABOMINATION" -> {
                // Plus gros et plus de vie
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombie.getMaxHealth() * 2);
                zombie.setHealth(zombie.getMaxHealth());
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5);
            }
            case "RUNNER" -> {
                // Très rapide mais moins de vie
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombie.getMaxHealth() * 0.6);
                zombie.setHealth(zombie.getMaxHealth());
            }
            case "BERSERKER" -> {
                // Plus de dégâts
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2);
            }
            case "BLOATER" -> {
                // Beaucoup de vie, lent
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombie.getMaxHealth() * 3);
                zombie.setHealth(zombie.getMaxHealth());
            }
            case "DEMON", "INFERNAL", "ARCHON" -> {
                // Résistant au feu et plus dangereux
                zombie.setFireTicks(0);
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.8);
            }
            case "ELITE", "CHAMPION" -> {
                // Version améliorée
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(zombie.getMaxHealth() * 2.5);
                zombie.setHealth(zombie.getMaxHealth());
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                    zombie.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 2);
                zombie.getAttribute(Attribute.ARMOR).setBaseValue(10);
            }
            case "BOSS" -> {
                // Boss très puissant
                zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500 + (level * 50));
                zombie.setHealth(zombie.getMaxHealth());
                zombie.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(15 + (level * 2));
                zombie.getAttribute(Attribute.ARMOR).setBaseValue(20);
                zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            }
        }
    }

    /**
     * Détermine si un zombie doit avoir un affix
     */
    private boolean shouldHaveAffix(int zoneId, ZombieType type) {
        if (type.isBoss()) return false; // Les boss ont leurs propres mécaniques
        
        // Chance d'affix basée sur la zone
        double chance = switch (zoneId) {
            case 1, 2, 3 -> 0.0;      // Pas d'affixes zones 1-3
            case 4, 5 -> 0.05;        // 5% zones 4-5
            case 6, 7 -> 0.10;        // 10% zones 6-7
            case 8 -> 0.15;           // 15% zone 8
            case 9 -> 0.25;           // 25% zone 9
            case 10, 11 -> 0.35;      // 35% zones 10-11
            default -> 0.0;
        };
        
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
            
            // Mettre à jour le nom
            String currentName = living.getCustomName();
            if (currentName != null) {
                living.setCustomName(affix.getColorCode() + affix.getPrefix() + " " + currentName);
            }
        }
    }

    /**
     * Traite la mort d'un zombie
     */
    public void onZombieDeath(UUID entityId, Player killer) {
        ActiveZombie zombie = activeZombies.remove(entityId);
        if (zombie == null) return;
        
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

    /**
     * Nettoie les zombies trop loin des joueurs
     */
    public void cleanupDistantZombies() {
        List<UUID> toRemove = new ArrayList<>();
        
        for (var entry : activeZombies.entrySet()) {
            Entity entity = plugin.getServer().getEntity(entry.getKey());
            
            if (entity == null || !entity.isValid()) {
                toRemove.add(entry.getKey());
                continue;
            }
            
            // Vérifier si un joueur est proche
            boolean playerNearby = entity.getWorld().getNearbyEntities(
                entity.getLocation(), 64, 64, 64).stream()
                .anyMatch(e -> e instanceof Player);
            
            if (!playerNearby) {
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
