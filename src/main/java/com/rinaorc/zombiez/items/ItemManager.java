package com.rinaorc.zombiez.items;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.generator.ItemGenerator;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manager pour les items ZombieZ
 * Gère le cache, les effets visuels, et les applications de stats
 */
public class ItemManager {

    private final ZombieZPlugin plugin;
    private final ItemGenerator generator;
    
    // Cache des items générés (UUID -> ZombieZItem)
    private final Cache<UUID, ZombieZItem> itemCache;
    
    // Cache des stats calculées par joueur (pour éviter les recalculs)
    private final Map<UUID, CachedPlayerStats> playerStatsCache;
    
    // Items droppés avec effets visuels actifs
    private final Map<UUID, DroppedItemEffect> activeDropEffects;

    // Scoreboard pour les teams de glow coloré
    private final Scoreboard scoreboard;

    // Préfixe pour les noms de teams de rareté
    private static final String TEAM_PREFIX = "zz_rarity_";

    public ItemManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.generator = ItemGenerator.getInstance();

        // Cache de 1000 items, expire après 30 min d'inactivité
        this.itemCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

        this.playerStatsCache = new ConcurrentHashMap<>();
        this.activeDropEffects = new ConcurrentHashMap<>();

        // Initialiser le scoreboard pour le glow coloré
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeRarityTeams();

        // Démarrer le task des effets de drop
        startDropEffectsTask();
    }

    /**
     * Initialise les teams pour chaque rareté avec la couleur appropriée
     * Ces teams permettent de colorer le glow des items droppés
     */
    private void initializeRarityTeams() {
        for (Rarity rarity : Rarity.values()) {
            String teamName = TEAM_PREFIX + rarity.name().toLowerCase();
            Team team = scoreboard.getTeam(teamName);

            // Créer la team si elle n'existe pas
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            // Définir la couleur de la team (détermine la couleur du glow)
            team.setColor(getRarityChatColor(rarity));
        }
    }

    /**
     * Obtient la couleur ChatColor correspondant à une rareté
     * Ces couleurs déterminent la couleur du glow effect des items
     */
    private ChatColor getRarityChatColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
            case MYTHIC -> ChatColor.LIGHT_PURPLE;
            case EXALTED -> ChatColor.RED;
        };
    }

    /**
     * Ajoute une entité item à la team de sa rareté pour le glow coloré
     */
    private void applyGlowColor(Item itemEntity, Rarity rarity) {
        String teamName = TEAM_PREFIX + rarity.name().toLowerCase();
        Team team = scoreboard.getTeam(teamName);

        if (team != null) {
            // Ajouter l'UUID de l'entité à la team
            team.addEntry(itemEntity.getUniqueId().toString());
        }
    }

    /**
     * Génère et drop un item au sol
     */
    public void dropItem(Location location, int zoneId, double luckBonus) {
        ZombieZItem item = generator.generate(zoneId, luckBonus);
        dropItem(location, item);
    }

    /**
     * Drop un item spécifique au sol
     */
    public void dropItem(Location location, ZombieZItem zItem) {
        // Appliquer un pouvoir si le système est activé
        ItemStack itemStack = applyPowerIfEligible(zItem);

        // Cacher l'item
        itemCache.put(zItem.getUuid(), zItem);

        // Drop l'item avec vélocité aléatoire
        Item droppedItem = location.getWorld().dropItem(location, itemStack);
        droppedItem.setVelocity(new Vector(
            (Math.random() - 0.5) * 0.2,
            0.3,
            (Math.random() - 0.5) * 0.2
        ));

        // Afficher le nom de l'item au sol avec la couleur de rareté
        droppedItem.setCustomName(zItem.getRarity().getChatColor() + zItem.getGeneratedName());
        droppedItem.setCustomNameVisible(true);

        // Faire briller l'item avec la couleur de rareté
        droppedItem.setGlowing(true);
        applyGlowColor(droppedItem, zItem.getRarity());

        // Effets visuels selon la rareté
        if (zItem.getRarity().isAtLeast(Rarity.RARE)) {
            startDropEffect(droppedItem, zItem);
        }

        // Son de drop
        playDropSound(location, zItem.getRarity());
    }

    /**
     * Applique un pouvoir à un item si éligible
     */
    private ItemStack applyPowerIfEligible(ZombieZItem zItem) {
        // Vérifier si le plugin a un PowerManager
        var powerManager = plugin.getPowerManager();
        if (powerManager == null || !powerManager.isEnabled()) {
            return zItem.toItemStack();
        }

        // Vérifier si l'item devrait avoir un pouvoir
        if (!powerManager.shouldHavePower(zItem.getRarity(), 0.0)) {
            return zItem.toItemStack();
        }

        // Sélectionner un pouvoir aléatoire
        var power = powerManager.selectRandomPower(zItem.getRarity());
        if (power == null) {
            return zItem.toItemStack();
        }

        // Appliquer le pouvoir
        zItem.setPowerId(power.getId());

        // Créer l'ItemStack avec le pouvoir
        var powerListener = plugin.getPowerTriggerListener();
        if (powerListener != null) {
            return zItem.toItemStackWithPower(powerListener, power);
        }

        return zItem.toItemStack();
    }

    /**
     * Génère et donne un item à un joueur
     */
    public ZombieZItem giveItem(Player player, int zoneId, double luckBonus) {
        ZombieZItem item = generator.generate(zoneId, luckBonus);
        giveItem(player, item);
        return item;
    }

    /**
     * Donne un item spécifique à un joueur
     */
    public void giveItem(Player player, ZombieZItem zItem) {
        // Appliquer un pouvoir si le système est activé
        ItemStack itemStack = applyPowerIfEligible(zItem);

        // Cacher l'item
        itemCache.put(zItem.getUuid(), zItem);

        // Donner au joueur
        var remaining = player.getInventory().addItem(itemStack);

        // Si inventaire plein, drop au sol
        if (!remaining.isEmpty()) {
            for (ItemStack leftover : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            MessageUtils.sendRaw(player, "§c⚠ Inventaire plein! Item droppé au sol.");
        }

        // Notification selon rareté
        sendItemNotification(player, zItem);
    }

    /**
     * Obtient un ZombieZItem depuis le cache par UUID
     */
    public ZombieZItem getItem(UUID uuid) {
        return itemCache.getIfPresent(uuid);
    }

    /**
     * Obtient un ZombieZItem depuis un ItemStack
     */
    public ZombieZItem getItem(ItemStack itemStack) {
        UUID uuid = ZombieZItem.getItemUUID(itemStack);
        if (uuid == null) return null;
        return getItem(uuid);
    }

    /**
     * Calcule les stats totales d'un joueur basées sur son équipement
     */
    public Map<StatType, Double> calculatePlayerStats(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Vérifier le cache
        CachedPlayerStats cached = playerStatsCache.get(playerUuid);
        if (cached != null && !cached.isExpired()) {
            return cached.stats;
        }
        
        Map<StatType, Double> totalStats = new EnumMap<>(StatType.class);
        
        // Parcourir l'équipement
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && ZombieZItem.isZombieZItem(item)) {
                ZombieZItem zItem = getItem(item);
                if (zItem != null) {
                    for (var entry : zItem.getTotalStats().entrySet()) {
                        totalStats.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                }
            }
        }
        
        // Arme en main
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (ZombieZItem.isZombieZItem(mainHand)) {
            ZombieZItem zItem = getItem(mainHand);
            if (zItem != null) {
                for (var entry : zItem.getTotalStats().entrySet()) {
                    totalStats.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }
        
        // Off-hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (ZombieZItem.isZombieZItem(offHand)) {
            ZombieZItem zItem = getItem(offHand);
            if (zItem != null) {
                for (var entry : zItem.getTotalStats().entrySet()) {
                    totalStats.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }
        
        // Mettre en cache (expire après 5 secondes)
        playerStatsCache.put(playerUuid, new CachedPlayerStats(totalStats));
        
        return totalStats;
    }

    /**
     * Obtient une stat spécifique d'un joueur
     */
    public double getPlayerStat(Player player, StatType stat) {
        return calculatePlayerStats(player).getOrDefault(stat, 0.0);
    }

    /**
     * Invalide le cache de stats d'un joueur (après changement d'équipement)
     */
    public void invalidatePlayerStats(UUID playerUuid) {
        playerStatsCache.remove(playerUuid);
    }

    /**
     * Calcule l'Item Score total de l'équipement d'un joueur
     */
    public int calculateTotalItemScore(Player player) {
        int total = 0;
        
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) {
                total += ZombieZItem.getItemScore(item);
            }
        }
        
        total += ZombieZItem.getItemScore(player.getInventory().getItemInMainHand());
        total += ZombieZItem.getItemScore(player.getInventory().getItemInOffHand());
        
        return total;
    }

    /**
     * Démarre l'effet visuel pour un item droppé
     */
    private void startDropEffect(Item droppedItem, ZombieZItem zItem) {
        UUID itemEntityId = droppedItem.getUniqueId();
        Rarity rarity = zItem.getRarity();
        
        DroppedItemEffect effect = new DroppedItemEffect(droppedItem, rarity);
        activeDropEffects.put(itemEntityId, effect);
    }

    /**
     * Task pour les effets visuels des items droppés
     */
    private void startDropEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, DroppedItemEffect>> iterator = activeDropEffects.entrySet().iterator();
                
                while (iterator.hasNext()) {
                    Map.Entry<UUID, DroppedItemEffect> entry = iterator.next();
                    DroppedItemEffect effect = entry.getValue();
                    
                    if (!effect.item.isValid() || effect.item.isDead()) {
                        iterator.remove();
                        continue;
                    }
                    
                    effect.tick();
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    /**
     * Joue le son de drop selon la rareté
     */
    private void playDropSound(Location location, Rarity rarity) {
        Sound sound = switch (rarity) {
            case EXALTED -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case MYTHIC -> Sound.ENTITY_PLAYER_LEVELUP;
            case LEGENDARY -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case EPIC -> Sound.BLOCK_NOTE_BLOCK_CHIME;
            case RARE -> Sound.BLOCK_NOTE_BLOCK_PLING;
            default -> Sound.ENTITY_ITEM_PICKUP;
        };
        
        float pitch = switch (rarity) {
            case EXALTED -> 0.5f;
            case MYTHIC -> 0.7f;
            case LEGENDARY -> 1.0f;
            case EPIC -> 1.2f;
            default -> 1.5f;
        };
        
        location.getWorld().playSound(location, sound, 1.0f, pitch);
    }

    /**
     * Envoie une notification pour un item obtenu
     */
    private void sendItemNotification(Player player, ZombieZItem zItem) {
        Rarity rarity = zItem.getRarity();
        
        if (rarity.isAtLeast(Rarity.LEGENDARY)) {
            // Annonce serveur pour légendaire+
            String message = rarity.getChatColor() + "★ " + player.getName() + 
                " §7a obtenu " + rarity.getChatColor() + zItem.getGeneratedName() + 
                " §8[" + zItem.getItemScore() + " IS]";
            plugin.getServer().broadcastMessage(message);
            
            MessageUtils.sendTitle(player, 
                rarity.getChatColor() + "§l" + rarity.getDisplayName().toUpperCase() + "!",
                "§f" + zItem.getGeneratedName(),
                10, 40, 10);
                
        } else if (rarity.isAtLeast(Rarity.EPIC)) {
            MessageUtils.sendRaw(player, rarity.getChatColor() + "★ Vous avez obtenu: " + 
                rarity.getChatColor() + zItem.getGeneratedName() + 
                " §8[" + zItem.getItemScore() + " IS]");
        }
    }

    /**
     * Obtient les statistiques du cache
     */
    public String getCacheStats() {
        return "Items: " + itemCache.estimatedSize() + 
            " | PlayerStats: " + playerStatsCache.size() +
            " | DropEffects: " + activeDropEffects.size();
    }

    /**
     * Nettoie les caches
     */
    public void cleanup() {
        itemCache.invalidateAll();
        playerStatsCache.clear();
        activeDropEffects.clear();
    }
    
    /**
     * Obtient le générateur d'items
     */
    public ItemGenerator getGenerator() {
        return generator;
    }
    
    /**
     * Obtient le système de loot drop
     */
    public LootDropSystem getLootDropSystem() {
        return new LootDropSystem(plugin);
    }
    
    /**
     * Obtient le SetBonusManager depuis le plugin
     */
    public com.rinaorc.zombiez.items.sets.SetBonusManager getSetBonusManager() {
        return plugin.getSetBonusManager();
    }
    
    /**
     * Construit un ItemStack depuis un ZombieZItem
     */
    public ItemStack buildItemStack(ZombieZItem zItem) {
        return zItem.toItemStack();
    }

    /**
     * Cache des stats joueur avec expiration
     */
    private static class CachedPlayerStats {
        final Map<StatType, Double> stats;
        final long timestamp;
        
        CachedPlayerStats(Map<StatType, Double> stats) {
            this.stats = stats;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5000; // 5 secondes
        }
    }

    /**
     * Effet visuel pour un item droppé
     */
    private class DroppedItemEffect {
        final Item item;
        final Rarity rarity;
        int tickCount = 0;
        
        DroppedItemEffect(Item item, Rarity rarity) {
            this.item = item;
            this.rarity = rarity;
        }
        
        void tick() {
            tickCount++;
            Location loc = item.getLocation();
            
            // Particules selon la rareté
            Particle particle = switch (rarity) {
                case EXALTED -> Particle.END_ROD;
                case MYTHIC -> Particle.DRAGON_BREATH;
                case LEGENDARY -> Particle.FLAME;
                case EPIC -> Particle.ENCHANT;
                case RARE -> Particle.HAPPY_VILLAGER;
                default -> Particle.CRIT;
            };
            
            // Beam de lumière pour rare+
            if (rarity.isHasLightBeam()) {
                for (double y = 0; y < 3; y += 0.3) {
                    loc.getWorld().spawnParticle(particle, 
                        loc.clone().add(0, y, 0), 
                        1, 0.1, 0, 0.1, 0);
                }
            }
            
            // Cercle au sol
            double angle = tickCount * 0.2;
            double radius = 0.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            
            loc.getWorld().spawnParticle(particle, 
                loc.clone().add(x, 0.2, z), 
                1, 0, 0, 0, 0);
        }
    }
}
