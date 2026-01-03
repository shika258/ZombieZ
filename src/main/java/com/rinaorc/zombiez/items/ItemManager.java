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
     * Applique le glow coloré à une entité item avec une couleur spécifique
     * Utilisé pour les items droppés (consommables, nourriture, etc.)
     */
    public void applyGlowWithColor(Item itemEntity, ChatColor color) {
        itemEntity.setGlowing(true);

        // Trouver la team correspondant à cette couleur
        for (Rarity rarity : Rarity.values()) {
            if (getRarityChatColor(rarity) == color) {
                applyGlowColor(itemEntity, rarity);
                return;
            }
        }

        // Si aucune rareté ne correspond, utiliser COMMON (blanc)
        applyGlowColor(itemEntity, Rarity.COMMON);
    }

    /**
     * Applique le glow coloré à une entité item basé sur une rareté ZombieZ
     */
    public void applyGlowForRarity(Item itemEntity, Rarity rarity) {
        itemEntity.setGlowing(true);
        applyGlowColor(itemEntity, rarity);
    }

    /**
     * Applique le glow coloré à un item droppé et affiche son nom
     * Peut être appelé depuis n'importe quel listener
     */
    public void applyDroppedItemEffects(Item itemEntity, String displayName, ChatColor glowColor) {
        // Afficher le nom
        itemEntity.setCustomName(glowColor + displayName);
        itemEntity.setCustomNameVisible(true);

        // Appliquer le glow
        applyGlowWithColor(itemEntity, glowColor);
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

        // Explosion de loot dopamine (système amélioré)
        if (plugin.getLootExplosionManager() != null && zItem.getRarity().isAtLeast(Rarity.UNCOMMON)) {
            plugin.getLootExplosionManager().triggerExplosion(location, zItem.getRarity(), null);
        } else {
            // Son de drop basique si le système dopamine n'est pas actif
            playDropSound(location, zItem.getRarity());
        }

        // Marquer pour le système de nettoyage (PerformanceManager)
        // Les drops sont automatiquement supprimés après le délai configuré
        if (plugin.getPerformanceManager() != null) {
            plugin.getPerformanceManager().markAsZombieZDrop(droppedItem);
        }
    }

    /**
     * Applique un éveil à un item si éligible (remplace l'ancien système de pouvoirs)
     */
    private ItemStack applyAwakenIfEligible(ZombieZItem zItem) {
        // Vérifier si le plugin a un AwakenManager
        var awakenManager = plugin.getAwakenManager();
        if (awakenManager == null || !awakenManager.isEnabled()) {
            return zItem.toItemStack();
        }

        // Les armes, armures et items off-hand peuvent avoir des éveils
        var category = zItem.getItemType().getCategory();
        if (category != com.rinaorc.zombiez.items.types.ItemType.ItemCategory.WEAPON &&
            category != com.rinaorc.zombiez.items.types.ItemType.ItemCategory.ARMOR &&
            category != com.rinaorc.zombiez.items.types.ItemType.ItemCategory.OFFHAND) {
            return zItem.toItemStack();
        }

        // Vérifier si l'item devrait avoir un éveil (ultra rare)
        if (!awakenManager.shouldHaveAwaken(zItem.getRarity(), zItem.getZoneLevel(), 0.0)) {
            return zItem.toItemStack();
        }

        // Générer un éveil aléatoire
        var awaken = awakenManager.generateAwaken(zItem.getRarity(), zItem.getZoneLevel());
        if (awaken == null) {
            return zItem.toItemStack();
        }

        // Appliquer l'éveil et ses données d'affichage
        zItem.setAwakenId(awaken.getId());

        // Remplir les données d'affichage de l'éveil
        if (awaken.getRequiredClass() != null) {
            zItem.setAwakenClassName(awaken.getRequiredClass().getColoredName());
        }
        if (awaken.getRequiredBranch() != null) {
            zItem.setAwakenBranchName(awaken.getRequiredBranch().getColoredName());
        }
        // Récupérer le nom du talent ciblé
        if (awaken.getTargetTalentId() != null && plugin.getTalentManager() != null) {
            var talent = plugin.getTalentManager().getTalent(awaken.getTargetTalentId());
            if (talent != null) {
                zItem.setAwakenTalentName(talent.getName());
            }
        }
        if (awaken.getEffectDescription() != null) {
            zItem.setAwakenEffectDesc(awaken.getEffectDescription());
        }

        // Créer l'ItemStack et stocker l'éveil dans le PDC
        ItemStack itemStack = zItem.toItemStack();
        awakenManager.storeAwakenInItem(itemStack, awaken);

        return itemStack;
    }

    /**
     * @deprecated Utilisez {@link #applyAwakenIfEligible} à la place
     */
    @Deprecated
    private ItemStack applyPowerIfEligible(ZombieZItem zItem) {
        return applyAwakenIfEligible(zItem);
    }

    /**
     * Génère et donne un item à un joueur
     */
    public ZombieZItem giveItem(Player player, int zoneId, double luckBonus) {
        ZombieZItem item = generator.generate(zoneId, luckBonus);

        // ============ INSTINCT DU CHERCHEUR (Ascension): 10% rare+ → tier+1 ============
        var ascensionManager = plugin.getAscensionManager();
        if (ascensionManager != null) {
            var ascData = ascensionManager.getData(player);
            if (ascData != null && ascData.hasMutation(com.rinaorc.zombiez.ascension.Mutation.INSTINCT_DU_CHERCHEUR)) {
                // Si l'item est Rare ou mieux, 10% chance d'upgrade rareté
                if (item.getRarity().ordinal() >= Rarity.RARE.ordinal()) {
                    if (Math.random() < 0.10) {
                        Rarity upgradedRarity = item.getRarity().getNext();
                        if (upgradedRarity != item.getRarity()) {
                            // Regénérer l'item avec la nouvelle rareté
                            item = generator.generate(zoneId, upgradedRarity, luckBonus);
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                            player.sendMessage("§d§l✨ §eInstinct du Chercheur! §7Item amélioré en §f" + upgradedRarity.getDisplayName() + "§7!");
                        }
                    }
                }
            }
        }

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
     * Inclut le bonus de forge sur chaque pièce d'équipement
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
                ZombieZItem zItem = getOrRestoreItem(item);
                if (zItem != null) {
                    double forgeMultiplier = getForgeMultiplier(item);
                    for (var entry : zItem.getTotalStats().entrySet()) {
                        double value = entry.getValue() * forgeMultiplier;
                        totalStats.merge(entry.getKey(), value, Double::sum);
                    }
                }
            }
        }

        // Arme en main
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (ZombieZItem.isZombieZItem(mainHand)) {
            ZombieZItem zItem = getOrRestoreItem(mainHand);
            if (zItem != null) {
                double forgeMultiplier = getForgeMultiplier(mainHand);
                for (var entry : zItem.getTotalStats().entrySet()) {
                    double value = entry.getValue() * forgeMultiplier;
                    totalStats.merge(entry.getKey(), value, Double::sum);
                }
            }
        }

        // Off-hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (ZombieZItem.isZombieZItem(offHand)) {
            ZombieZItem zItem = getOrRestoreItem(offHand);
            if (zItem != null) {
                double forgeMultiplier = getForgeMultiplier(offHand);
                for (var entry : zItem.getTotalStats().entrySet()) {
                    double value = entry.getValue() * forgeMultiplier;
                    totalStats.merge(entry.getKey(), value, Double::sum);
                }
            }
        }

        // Appliquer les caps sur les stats critiques pour éviter les valeurs absurdes
        applyStatCaps(totalStats);

        // Mettre en cache (expire après 5 secondes)
        playerStatsCache.put(playerUuid, new CachedPlayerStats(totalStats));

        return totalStats;
    }

    /**
     * Applique des caps (limites maximales) sur les stats critiques
     * pour éviter les builds complètement cassés
     */
    private void applyStatCaps(Map<StatType, Double> stats) {
        // CAPS DE SÉCURITÉ - Évite les valeurs complètement absurdes

        // Stats offensives
        capStat(stats, StatType.CRIT_CHANCE, 75.0);           // Max 75% crit chance
        capStat(stats, StatType.CRIT_DAMAGE, 250.0);          // Max +250% crit damage
        capStat(stats, StatType.LIFESTEAL, 25.0);             // Max 25% lifesteal
        capStat(stats, StatType.DAMAGE_PERCENT, 150.0);       // Max +150% damage bonus

        // Stats défensives
        capStat(stats, StatType.DAMAGE_REDUCTION, 60.0);      // Max 60% damage reduction
        capStat(stats, StatType.DODGE_CHANCE, 40.0);          // Max 40% dodge
        capStat(stats, StatType.CHEAT_DEATH_CHANCE, 10.0);    // Max 10% cheat death
        capStat(stats, StatType.ARMOR_PERCENT, 100.0);        // Max +100% armor bonus

        // Stats utilitaires
        capStat(stats, StatType.MOVEMENT_SPEED, 50.0);        // Max +50% movement speed
        capStat(stats, StatType.DRAW_SPEED, 75.0);            // Max +75% draw speed

        // Stats de momentum/fever
        capStat(stats, StatType.FEVER_DAMAGE_BONUS, 100.0);   // Max +100% fever damage
        capStat(stats, StatType.FEVER_DURATION_BONUS, 100.0); // Max +100% fever duration
        capStat(stats, StatType.STREAK_DAMAGE_BONUS, 5.0);    // Max +5% per streak kill

        // Stats d'exécution
        capStat(stats, StatType.EXECUTE_DAMAGE, 100.0);       // Max +100% execute damage
        capStat(stats, StatType.EXECUTE_THRESHOLD, 15.0);     // Max execute at <15% HP

        // Stats de chance/loot
        capStat(stats, StatType.LUCK, 50.0);                  // Max 50% luck (évite le loot à chaque kill)
        capStat(stats, StatType.DOUBLE_LOOT_CHANCE, 30.0);    // Max 30% double loot
        capStat(stats, StatType.LEGENDARY_DROP_BONUS, 100.0); // Max +100% legendary drop

        // Stats de vie
        capStat(stats, StatType.MAX_HEALTH, 150.0);           // Max +150 HP (évite les tanks immortels)

        // Dégâts élémentaires (évite les one-shots élémentaires)
        capStat(stats, StatType.FIRE_DAMAGE, 100.0);          // Max +100 fire damage
        capStat(stats, StatType.ICE_DAMAGE, 100.0);           // Max +100 ice damage
        capStat(stats, StatType.LIGHTNING_DAMAGE, 100.0);     // Max +100 lightning damage
        capStat(stats, StatType.POISON_DAMAGE, 50.0);         // Max +50 poison damage/s (DoT plus bas)
    }

    /**
     * Applique un cap à une stat si elle existe dans la map
     */
    private void capStat(Map<StatType, Double> stats, StatType stat, double maxValue) {
        Double current = stats.get(stat);
        if (current != null && current > maxValue) {
            stats.put(stat, maxValue);
        }
    }

    /**
     * Obtient un ZombieZItem depuis le cache, ou le restaure depuis le PDC si non trouvé
     */
    public ZombieZItem getOrRestoreItem(ItemStack itemStack) {
        UUID uuid = ZombieZItem.getItemUUID(itemStack);
        if (uuid == null) return null;

        // Vérifier le cache d'abord
        ZombieZItem cached = itemCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }

        // Si pas dans le cache, restaurer depuis le PDC
        ZombieZItem restored = ZombieZItem.fromItemStack(itemStack);
        if (restored != null) {
            // Mettre en cache pour les prochains accès
            itemCache.put(uuid, restored);
        }
        return restored;
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
     * Génère un item avec une rareté spécifique (pour les événements)
     */
    public ItemStack generateItem(int zoneId, Rarity rarity) {
        // Générer un item avec la rareté forcée
        ZombieZItem zItem = generator.generate(zoneId, rarity, 0.0);
        if (zItem == null) return null;

        // Cacher l'item
        itemCache.put(zItem.getUuid(), zItem);

        // Appliquer un pouvoir si éligible
        return applyPowerIfEligible(zItem);
    }

    /**
     * Génère un ZombieZItem avec une rareté spécifique (pour les événements)
     */
    public ZombieZItem generateZombieZItem(int zoneId, Rarity rarity) {
        return generator.generate(zoneId, rarity, 0.0);
    }

    /**
     * Obtient les données d'un item ZombieZ
     */
    public ZombieZItem getItemData(ItemStack itemStack) {
        return getItem(itemStack);
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
     * Obtient le multiplicateur de forge pour un item
     * Délègue au ForgeManager si disponible
     */
    private double getForgeMultiplier(ItemStack item) {
        var forgeManager = plugin.getForgeManager();
        if (forgeManager == null) return 1.0;
        return forgeManager.getForgeMultiplier(item);
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
