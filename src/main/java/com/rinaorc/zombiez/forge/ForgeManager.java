package com.rinaorc.zombiez.forge;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire du système de Forge (Enhancement)
 *
 * Permet aux joueurs d'améliorer leurs équipements de +0 à +10
 * avec un système de risque/récompense progressif.
 *
 * Phases:
 * - Safe (+1 à +3): 100% succès
 * - Risque (+4 à +6): 70-50% succès, -1 niveau en cas d'échec
 * - Danger (+7 à +9): 40-20% succès, -2 niveaux en cas d'échec
 * - Légendaire (+10): 10% succès, retour à +7 en cas d'échec
 */
public class ForgeManager {

    private final ZombieZPlugin plugin;

    // Clés PDC
    public static final NamespacedKey KEY_FORGE_LEVEL = new NamespacedKey("zombiez", "forge_level");

    // Configuration des niveaux
    public static final int MAX_FORGE_LEVEL = 10;

    // Statistiques des joueurs (en mémoire, optionnel pour persistence)
    @Getter
    private final Map<UUID, ForgeStats> playerStats = new ConcurrentHashMap<>();

    // Configuration des niveaux de forge
    private static final ForgeLevel[] FORGE_LEVELS = {
        // Niveau, Coût base, Chance succès, Perte en cas d'échec, Bonus stats %
        new ForgeLevel(1, 500, 1.00, 0, 5),      // +1: 100%, pas de perte
        new ForgeLevel(2, 800, 1.00, 0, 10),     // +2: 100%, pas de perte
        new ForgeLevel(3, 1200, 1.00, 0, 15),    // +3: 100%, pas de perte
        new ForgeLevel(4, 2000, 0.70, 1, 22),    // +4: 70%, -1 niveau
        new ForgeLevel(5, 3500, 0.60, 1, 30),    // +5: 60%, -1 niveau
        new ForgeLevel(6, 6000, 0.50, 1, 40),    // +6: 50%, -1 niveau
        new ForgeLevel(7, 10000, 0.40, 2, 52),   // +7: 40%, -2 niveaux
        new ForgeLevel(8, 18000, 0.30, 2, 66),   // +8: 30%, -2 niveaux
        new ForgeLevel(9, 30000, 0.20, 2, 82),   // +9: 20%, -2 niveaux
        new ForgeLevel(10, 50000, 0.10, 3, 100), // +10: 10%, retour à +7
    };

    // Multiplicateurs de coût par rareté
    private static final Map<Rarity, Double> RARITY_COST_MULTIPLIERS = Map.of(
        Rarity.COMMON, 0.5,
        Rarity.UNCOMMON, 0.75,
        Rarity.RARE, 1.0,
        Rarity.EPIC, 1.5,
        Rarity.LEGENDARY, 2.0,
        Rarity.MYTHIC, 3.0,
        Rarity.EXALTED, 5.0
    );

    public ForgeManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise le système de forge
     */
    public void init() {
        plugin.log(Level.INFO, "§a✓ Système de Forge initialisé");
    }

    /**
     * Arrête le système de forge
     */
    public void shutdown() {
        playerStats.clear();
    }

    // ==================== API PRINCIPALE ====================

    /**
     * Vérifie si un item peut être forgé
     */
    public boolean canBeForged(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        // Vérifier que c'est un item ZombieZ
        if (!ZombieZItem.isZombieZItem(item)) return false;

        // Récupérer le type d'item
        ZombieZItem zItem = ZombieZItem.fromItemStack(item);
        if (zItem == null) return false;

        ItemType type = zItem.getItemType();

        // Seuls les équipements peuvent être forgés
        return type.isWeapon() || type.isArmor();
    }

    /**
     * Obtient le niveau de forge actuel d'un item
     */
    public int getForgeLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        return pdc.getOrDefault(KEY_FORGE_LEVEL, PersistentDataType.INTEGER, 0);
    }

    /**
     * Définit le niveau de forge d'un item
     */
    public void setForgeLevel(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) return;

        level = Math.max(0, Math.min(MAX_FORGE_LEVEL, level));

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (level == 0) {
            pdc.remove(KEY_FORGE_LEVEL);
        } else {
            pdc.set(KEY_FORGE_LEVEL, PersistentDataType.INTEGER, level);
        }

        item.setItemMeta(meta);

        // Mettre à jour le lore/nom de l'item
        updateItemDisplay(item, level);
    }

    /**
     * Calcule le coût de forge pour passer au niveau suivant
     */
    public int getForgeCost(ItemStack item) {
        int currentLevel = getForgeLevel(item);
        if (currentLevel >= MAX_FORGE_LEVEL) return -1;

        ForgeLevel nextLevel = FORGE_LEVELS[currentLevel];
        int baseCost = nextLevel.baseCost;

        // Appliquer le multiplicateur de rareté
        ZombieZItem zItem = ZombieZItem.fromItemStack(item);
        if (zItem != null) {
            double multiplier = RARITY_COST_MULTIPLIERS.getOrDefault(zItem.getRarity(), 1.0);
            baseCost = (int) Math.ceil(baseCost * multiplier);
        }

        return baseCost;
    }

    /**
     * Obtient la chance de succès pour le prochain niveau
     */
    public double getSuccessChance(ItemStack item) {
        int currentLevel = getForgeLevel(item);
        if (currentLevel >= MAX_FORGE_LEVEL) return 0;

        return FORGE_LEVELS[currentLevel].successChance;
    }

    /**
     * Obtient le nombre de niveaux perdus en cas d'échec
     */
    public int getFailurePenalty(ItemStack item) {
        int currentLevel = getForgeLevel(item);
        if (currentLevel >= MAX_FORGE_LEVEL) return 0;

        return FORGE_LEVELS[currentLevel].levelLoss;
    }

    /**
     * Obtient le bonus de stats pour un niveau donné
     */
    public int getStatBonus(int level) {
        if (level <= 0) return 0;
        if (level > MAX_FORGE_LEVEL) level = MAX_FORGE_LEVEL;

        return FORGE_LEVELS[level - 1].statBonus;
    }

    /**
     * Tente de forger un item
     *
     * @param player Le joueur qui forge
     * @param item L'item à forger
     * @param useProtection Utiliser une pierre de protection
     * @param useChance Utiliser une pierre de chance
     * @return Le résultat de la forge
     */
    public ForgeResult attemptForge(Player player, ItemStack item, boolean useProtection, boolean useChance) {
        // Vérifications
        if (!canBeForged(item)) {
            return new ForgeResult(false, ForgeResultType.INVALID_ITEM, 0, 0, "Cet item ne peut pas être forgé!");
        }

        int currentLevel = getForgeLevel(item);
        if (currentLevel >= MAX_FORGE_LEVEL) {
            return new ForgeResult(false, ForgeResultType.MAX_LEVEL, currentLevel, currentLevel, "Cet item est déjà au niveau maximum!");
        }

        int cost = getForgeCost(item);

        // Vérifier les points
        var playerData = plugin.getPlayerDataManager().getPlayer(player);
        if (playerData == null || playerData.getPoints().get() < cost) {
            return new ForgeResult(false, ForgeResultType.NOT_ENOUGH_POINTS, currentLevel, currentLevel,
                "Points insuffisants! (Besoin: " + cost + ")");
        }

        // Déduire les points
        playerData.removePoints(cost);

        // Calculer la chance de succès
        double successChance = getSuccessChance(item);
        if (useChance) {
            successChance = Math.min(1.0, successChance + 0.15); // +15% avec pierre de chance
        }

        // Tenter la forge
        boolean success = Math.random() < successChance;

        int newLevel;
        ForgeResultType resultType;
        String message;

        // Enregistrer les points dépensés
        ForgeStats stats = getOrCreateStats(player.getUniqueId());
        stats.recordPointsSpent(cost);

        if (success) {
            // Succès!
            newLevel = currentLevel + 1;
            setForgeLevel(item, newLevel);
            resultType = ForgeResultType.SUCCESS;
            message = "Forge réussie! +" + newLevel;

            // Effets
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f + (newLevel * 0.1f));

            // Stats
            stats.recordSuccess();
            stats.updateHighestLevel(newLevel);

            // Annonce serveur pour +10
            if (newLevel == MAX_FORGE_LEVEL) {
                stats.recordMaxLevel();
                announceMaxForge(player, item);
            }

        } else {
            // Échec
            int penalty = useProtection ? 0 : getFailurePenalty(item);
            newLevel = Math.max(0, currentLevel - penalty);

            if (useProtection) {
                resultType = ForgeResultType.PROTECTED;
                message = "Échec... mais votre Pierre de Protection a sauvé l'item!";
            } else {
                setForgeLevel(item, newLevel);
                resultType = ForgeResultType.FAILURE;
                message = "Échec! L'item est retombé à +" + newLevel;
            }

            // Effets
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.8f);

            // Stats
            stats.recordFailure();
        }

        // Mettre à jour la mission si existante
        updateForgeMission(player, success, currentLevel + 1);

        return new ForgeResult(success, resultType, currentLevel, newLevel, message);
    }

    // ==================== MISE À JOUR AFFICHAGE ====================

    /**
     * Met à jour l'affichage de l'item (nom avec niveau de forge)
     */
    private void updateItemDisplay(ItemStack item, int forgeLevel) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        String currentName = meta.getDisplayName();

        // Retirer l'ancien niveau de forge du nom s'il existe
        currentName = currentName.replaceAll(" §e\\[\\+\\d+\\]$", "");
        currentName = currentName.replaceAll(" §6§l\\[\\+10\\]$", "");

        // Ajouter le nouveau niveau si > 0
        if (forgeLevel > 0) {
            if (forgeLevel == MAX_FORGE_LEVEL) {
                currentName += " §6§l[+10]"; // Style spécial pour +10
            } else {
                currentName += " §e[+" + forgeLevel + "]";
            }
        }

        meta.setDisplayName(currentName);
        item.setItemMeta(meta);

        // Mettre à jour le lore avec les stats boostées (recalcul depuis les valeurs de base PDC)
        updateItemLore(item, forgeLevel);
    }

    /**
     * Met à jour le lore de l'item avec le bonus de forge et les stats boostées
     * Recalcule TOUJOURS à partir des stats de base stockées dans le PDC
     */
    private void updateItemLore(ItemStack item, int forgeLevel) {
        // Reconstruire le ZombieZItem pour avoir les stats de base originales
        ZombieZItem zItem = ZombieZItem.fromItemStack(item);
        if (zItem == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Calculer le multiplicateur
        double multiplier = forgeLevel > 0 ? 1.0 + (getStatBonus(forgeLevel) / 100.0) : 1.0;

        // Obtenir les stats de base et d'affixes originales
        Map<com.rinaorc.zombiez.items.types.StatType, Double> baseStats = zItem.getBaseStats();
        List<ZombieZItem.RolledAffix> affixes = zItem.getAffixes();

        // Créer un map de toutes les stats avec leurs valeurs boostées
        Map<String, Double> boostedStats = new HashMap<>();

        // Stats de base
        for (var entry : baseStats.entrySet()) {
            if (entry.getKey().isBaseStat()) {
                boostedStats.put(entry.getKey().getDisplayName(), entry.getValue() * multiplier);
            }
        }

        // Stats d'affixes (stocker par "affixName:statName" pour éviter les conflits)
        for (ZombieZItem.RolledAffix affix : affixes) {
            for (var entry : affix.getRolledStats().entrySet()) {
                String key = affix.getAffix().getDisplayName() + ":" + entry.getKey().getDisplayName();
                boostedStats.put(key, entry.getValue() * multiplier);
            }
        }

        // Mettre à jour les lignes de stats dans le lore
        updateStatLinesInLore(lore, baseStats, affixes, multiplier);

        // Chercher et remplacer la ligne de forge existante
        int forgeLineIndex = -1;
        int zoneLineIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("§7Forge:") || line.contains("⚒ FORGE") || line.contains("✧ FORGE")) {
                forgeLineIndex = i;
            }
            if (line.contains("Requiert:")) {
                zoneLineIndex = i;
            }
        }

        if (forgeLevel > 0) {
            int bonus = getStatBonus(forgeLevel);
            // FORGE en jaune (§e) pour différencier de STATS DE BASE (orange)
            String forgeLine = "§e⚒ FORGE §f+" + forgeLevel + " §7(+" + bonus + "% stats)";

            if (forgeLineIndex >= 0) {
                lore.set(forgeLineIndex, forgeLine);
            } else {
                // Ajouter après la ligne "Requiert: Zone X" avec un spacer
                if (zoneLineIndex >= 0) {
                    lore.add(zoneLineIndex + 1, ""); // Spacer vide
                    lore.add(zoneLineIndex + 2, forgeLine);
                } else if (lore.size() > 2) {
                    lore.add(3, forgeLine); // Fallback: après Item Score et Zone
                } else {
                    lore.add(forgeLine);
                }
            }
        } else if (forgeLineIndex >= 0) {
            lore.remove(forgeLineIndex);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Met à jour les lignes de stats dans le lore avec les valeurs boostées
     * Utilise les stats de base du PDC pour recalculer les valeurs correctes
     */
    private void updateStatLinesInLore(List<String> lore,
                                       Map<com.rinaorc.zombiez.items.types.StatType, Double> baseStats,
                                       List<ZombieZItem.RolledAffix> affixes,
                                       double multiplier) {

        // Créer une map avec clé unique (displayName + format) pour les stats de base
        // Cela évite les conflits entre DAMAGE/DAMAGE_PERCENT, ATTACK_SPEED/ATTACK_SPEED_PERCENT, etc.
        Map<String, Map.Entry<Double, com.rinaorc.zombiez.items.types.StatType>> baseStatsMap = new HashMap<>();
        for (var entry : baseStats.entrySet()) {
            if (entry.getKey().isBaseStat()) {
                String uniqueKey = getUniqueStatKey(entry.getKey());
                baseStatsMap.put(uniqueKey,
                    new AbstractMap.SimpleEntry<>(entry.getValue(), entry.getKey()));
            }
        }

        // Tracker quel affix on traite (pour les stats d'affixes)
        int currentAffixIndex = -1;
        Map<String, Map.Entry<Double, com.rinaorc.zombiez.items.types.StatType>> currentAffixStats = null;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);

            // Détecter quand on entre dans un nouvel affix
            if (line.startsWith("§") && line.contains("▸ ")) {
                currentAffixIndex++;
                if (currentAffixIndex < affixes.size()) {
                    currentAffixStats = new HashMap<>();
                    ZombieZItem.RolledAffix affix = affixes.get(currentAffixIndex);
                    for (var entry : affix.getRolledStats().entrySet()) {
                        String uniqueKey = getUniqueStatKey(entry.getKey());
                        currentAffixStats.put(uniqueKey,
                            new AbstractMap.SimpleEntry<>(entry.getValue(), entry.getKey()));
                    }
                }
                continue;
            }

            // Chercher les lignes de stats (format: "  §7StatName: §aValue" ou "  §7StatName: §cValue")
            if (line.startsWith("  §7") && line.contains(": §")) {
                // Extraire le nom de la stat
                int colonIndex = line.indexOf(": §");
                if (colonIndex > 4) {
                    String statName = line.substring(4, colonIndex); // Après "  §7"

                    // Déterminer si la valeur dans le lore est un pourcentage
                    // en regardant si la ligne contient un % après le ":"
                    String valueSection = line.substring(colonIndex);
                    boolean isPercentInLore = valueSection.contains("%");

                    // Construire la même clé unique utilisée pour indexer
                    String lookupKey = statName + (isPercentInLore ? "_PCT" : "_FLAT");

                    // Chercher d'abord dans l'affix courant, puis dans les stats de base
                    Map.Entry<Double, com.rinaorc.zombiez.items.types.StatType> statEntry = null;
                    if (currentAffixStats != null) {
                        statEntry = currentAffixStats.get(lookupKey);
                    }
                    if (statEntry == null) {
                        statEntry = baseStatsMap.get(lookupKey);
                    }

                    if (statEntry != null) {
                        double baseValue = statEntry.getKey();
                        com.rinaorc.zombiez.items.types.StatType statType = statEntry.getValue();
                        double boostedValue = baseValue * multiplier;

                        // Reconstruire la ligne avec la valeur boostée
                        String valueColor = boostedValue >= 0 ? "§a" : "§c";
                        String formattedValue = statType.formatValue(boostedValue);

                        // Préserver l'indicateur god roll s'il existe
                        String godRollSuffix = line.contains("§6✦") ? " §6✦" : "";

                        lore.set(i, "  §7" + statName + ": " + valueColor + formattedValue + godRollSuffix);
                    }
                }
            }
        }
    }

    /**
     * Génère une clé unique pour un StatType en combinant displayName et format
     * Évite les conflits entre DAMAGE/DAMAGE_PERCENT, ATTACK_SPEED/ATTACK_SPEED_PERCENT, etc.
     */
    private String getUniqueStatKey(com.rinaorc.zombiez.items.types.StatType statType) {
        // Si le format contient %, c'est un pourcentage
        boolean isPercentFormat = statType.getDisplayFormat().contains("%");
        return statType.getDisplayName() + (isPercentFormat ? "_PCT" : "_FLAT");
    }

    // ==================== PIERRES DE PROTECTION ====================

    /**
     * Vérifie si le joueur possède une pierre de protection
     */
    public boolean hasProtectionStone(Player player) {
        return countProtectionStones(player) > 0;
    }

    /**
     * Compte les pierres de protection du joueur
     */
    public int countProtectionStones(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isProtectionStone(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Vérifie si un item est une pierre de protection
     */
    public boolean isProtectionStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(new NamespacedKey("zombiez", "protection_stone"), PersistentDataType.BYTE);
    }

    /**
     * Vérifie si un item est une pierre de chance
     */
    public boolean isChanceStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(new NamespacedKey("zombiez", "chance_stone"), PersistentDataType.BYTE);
    }

    /**
     * Consomme une pierre de protection
     */
    public boolean consumeProtectionStone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isProtectionStone(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Consomme une pierre de chance
     */
    public boolean consumeChanceStone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isChanceStone(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Crée une pierre de protection
     */
    public ItemStack createProtectionStone(int amount) {
        ItemStack stone = new ItemStack(org.bukkit.Material.AMETHYST_SHARD, amount);
        ItemMeta meta = stone.getItemMeta();

        meta.setDisplayName("§d§lPierre de Protection");
        meta.setLore(List.of(
            "",
            "§7Protège votre item en cas",
            "§7d'échec de forge.",
            "",
            "§8Consommée à l'utilisation",
            "",
            "§dClic droit dans la forge"
        ));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey("zombiez", "protection_stone"), PersistentDataType.BYTE, (byte) 1);

        stone.setItemMeta(meta);
        return stone;
    }

    /**
     * Crée une pierre de chance
     */
    public ItemStack createChanceStone(int amount) {
        ItemStack stone = new ItemStack(org.bukkit.Material.PRISMARINE_CRYSTALS, amount);
        ItemMeta meta = stone.getItemMeta();

        meta.setDisplayName("§b§lPierre de Chance");
        meta.setLore(List.of(
            "",
            "§7Augmente les chances de",
            "§7succès de §a+15%§7.",
            "",
            "§8Consommée à l'utilisation",
            "",
            "§bClic droit dans la forge"
        ));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey("zombiez", "chance_stone"), PersistentDataType.BYTE, (byte) 1);

        stone.setItemMeta(meta);
        return stone;
    }

    /**
     * Crée une pierre bénie (protection + chance)
     */
    public ItemStack createBlessedStone(int amount) {
        ItemStack stone = new ItemStack(org.bukkit.Material.NETHER_STAR, amount);
        ItemMeta meta = stone.getItemMeta();

        meta.setDisplayName("§6§lPierre Bénie");
        meta.setLore(List.of(
            "",
            "§7Combine les effets de",
            "§7protection §det chance§7.",
            "",
            "§a+15% §7chance de succès",
            "§d+Protection §7en cas d'échec",
            "",
            "§8Consommée à l'utilisation",
            "",
            "§6Clic droit dans la forge"
        ));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey("zombiez", "blessed_stone"), PersistentDataType.BYTE, (byte) 1);

        stone.setItemMeta(meta);
        return stone;
    }

    /**
     * Vérifie si un item est une pierre bénie
     */
    public boolean isBlessedStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(new NamespacedKey("zombiez", "blessed_stone"), PersistentDataType.BYTE);
    }

    /**
     * Compte les pierres bénies du joueur
     */
    public int countBlessedStones(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBlessedStone(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Consomme une pierre bénie
     */
    public boolean consumeBlessedStone(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBlessedStone(item)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    // ==================== STATISTIQUES ====================

    private ForgeStats getOrCreateStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, k -> new ForgeStats());
    }

    public ForgeStats getStats(UUID playerId) {
        return playerStats.get(playerId);
    }

    // ==================== UTILITAIRES ====================

    private void announceMaxForge(Player player, ItemStack item) {
        String itemName = item.getItemMeta().getDisplayName();
        plugin.getServer().broadcastMessage("");
        plugin.getServer().broadcastMessage("§6§l★ §e§lFORGE LÉGENDAIRE §6§l★");
        plugin.getServer().broadcastMessage("§7" + player.getName() + " §fa forgé un item §6§l+10§f!");
        plugin.getServer().broadcastMessage("§7Item: " + itemName);
        plugin.getServer().broadcastMessage("");

        // Son pour tout le serveur
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1f);
        }
    }

    private void updateForgeMission(Player player, boolean success, int targetLevel) {
        // Intégration avec le système de missions
        if (success) {
            plugin.getMissionManager().updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.FORGE_SUCCESS, 1);

            if (targetLevel == MAX_FORGE_LEVEL) {
                plugin.getMissionManager().updateProgress(player,
                    com.rinaorc.zombiez.progression.MissionManager.MissionTracker.FORGE_MAX_LEVEL, 1);
            }
        }
    }

    /**
     * Obtient les informations du prochain niveau de forge
     */
    public ForgeLevel getNextLevelInfo(int currentLevel) {
        if (currentLevel >= MAX_FORGE_LEVEL) return null;
        return FORGE_LEVELS[currentLevel];
    }

    /**
     * Calcule le multiplicateur de stats d'un item forgé
     */
    public double getForgeMultiplier(ItemStack item) {
        int level = getForgeLevel(item);
        if (level <= 0) return 1.0;
        return 1.0 + (getStatBonus(level) / 100.0);
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * Configuration d'un niveau de forge
     */
    public record ForgeLevel(int level, int baseCost, double successChance, int levelLoss, int statBonus) {}

    /**
     * Résultat d'une tentative de forge
     */
    public record ForgeResult(boolean success, ForgeResultType type, int previousLevel, int newLevel, String message) {}

    /**
     * Types de résultats de forge
     */
    public enum ForgeResultType {
        SUCCESS,
        FAILURE,
        PROTECTED,
        MAX_LEVEL,
        NOT_ENOUGH_POINTS,
        INVALID_ITEM
    }

    /**
     * Statistiques de forge d'un joueur
     */
    @Getter
    public static class ForgeStats {
        private int totalAttempts = 0;
        private int totalSuccess = 0;
        private int totalFailures = 0;
        private int highestLevel = 0;
        private long pointsSpent = 0;
        private int itemsAtMax = 0;

        public void recordSuccess() {
            totalAttempts++;
            totalSuccess++;
        }

        public void recordFailure() {
            totalAttempts++;
            totalFailures++;
        }

        public void recordPointsSpent(int points) {
            pointsSpent += points;
        }

        public void recordMaxLevel() {
            itemsAtMax++;
        }

        public void updateHighestLevel(int level) {
            if (level > highestLevel) {
                highestLevel = level;
            }
        }

        public double getSuccessRate() {
            if (totalAttempts == 0) return 0;
            return (double) totalSuccess / totalAttempts * 100;
        }
    }
}
