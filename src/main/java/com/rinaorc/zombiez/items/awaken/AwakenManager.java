package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumMap;

/**
 * Gestionnaire central du système d'Éveil
 *
 * Responsabilités:
 * - Génération d'éveils lors de la création d'items
 * - Validation des éveils actifs
 * - Sérialisation/désérialisation des éveils
 * - Statistiques et debugging
 */
@Getter
public class AwakenManager {

    private final ZombieZPlugin plugin;
    private final AwakenRegistry registry;

    // ==================== CONFIGURATION ====================

    /**
     * Système activé/désactivé
     */
    private boolean enabled = true;

    /**
     * Chances de base d'obtenir un éveil par rareté
     * Ultra rare: ~1% sur Common à ~10% sur Exalted
     */
    private final Map<Rarity, Double> awakenChanceByRarity = new EnumMap<>(Rarity.class);

    /**
     * Bonus de chance par zone (zones hautes = légèrement plus de chances)
     */
    private double zoneChanceBonus = 0.001; // +0.1% par zone

    /**
     * Maximum de zone pour le bonus
     */
    private int maxZoneForBonus = 50;

    // ==================== CLÉS PDC ====================

    private final NamespacedKey keyAwakenId;
    private final NamespacedKey keyAwakenClass;
    private final NamespacedKey keyAwakenTalent;
    private final NamespacedKey keyAwakenModType;
    private final NamespacedKey keyAwakenModValue;
    private final NamespacedKey keyAwakenDesc;

    // ==================== CACHE ====================

    /**
     * Cache des éveils par UUID d'item
     */
    private final Map<UUID, Awaken> awakenCache = new ConcurrentHashMap<>();

    public AwakenManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.registry = AwakenRegistry.getInstance();

        // Initialiser les clés PDC
        this.keyAwakenId = new NamespacedKey(plugin, "awaken_id");
        this.keyAwakenClass = new NamespacedKey(plugin, "awaken_class");
        this.keyAwakenTalent = new NamespacedKey(plugin, "awaken_talent");
        this.keyAwakenModType = new NamespacedKey(plugin, "awaken_mod_type");
        this.keyAwakenModValue = new NamespacedKey(plugin, "awaken_mod_value");
        this.keyAwakenDesc = new NamespacedKey(plugin, "awaken_desc");

        initializeDefaultChances();
    }

    /**
     * Initialise les chances par défaut (ultra rares)
     */
    private void initializeDefaultChances() {
        awakenChanceByRarity.put(Rarity.COMMON, 0.005);      // 0.5%
        awakenChanceByRarity.put(Rarity.UNCOMMON, 0.01);     // 1%
        awakenChanceByRarity.put(Rarity.RARE, 0.02);         // 2%
        awakenChanceByRarity.put(Rarity.EPIC, 0.035);        // 3.5%
        awakenChanceByRarity.put(Rarity.LEGENDARY, 0.05);    // 5%
        awakenChanceByRarity.put(Rarity.MYTHIC, 0.075);      // 7.5%
        awakenChanceByRarity.put(Rarity.EXALTED, 0.10);      // 10%
    }

    // ==================== GÉNÉRATION ====================

    /**
     * Détermine si un item devrait avoir un éveil
     *
     * @param rarity Rareté de l'item
     * @param zoneId Zone de l'item
     * @param luckBonus Bonus de chance du joueur
     * @return true si l'item devrait avoir un éveil
     */
    public boolean shouldHaveAwaken(Rarity rarity, int zoneId, double luckBonus) {
        if (!enabled) return false;

        double baseChance = awakenChanceByRarity.getOrDefault(rarity, 0.01);

        // Bonus de zone (max +5% à zone 50)
        double zoneBonus = Math.min(zoneId, maxZoneForBonus) * zoneChanceBonus;

        // Bonus de luck
        double luckMultiplier = 1.0 + luckBonus;

        double finalChance = (baseChance + zoneBonus) * luckMultiplier;

        return Math.random() < finalChance;
    }

    /**
     * Génère un éveil aléatoire pour un item (arme)
     *
     * @param rarity Rareté de l'item (influence la qualité)
     * @param zoneId Zone de l'item
     * @return Un Awaken généré ou null si échec
     */
    public Awaken generateAwaken(Rarity rarity, int zoneId) {
        TalentManager talentManager = plugin.getTalentManager();
        if (talentManager == null) {
            plugin.getLogger().warning("[Awaken] TalentManager non disponible!");
            return null;
        }

        // Sélectionner une classe aléatoire
        ClassType randomClass = ClassType.values()[new Random().nextInt(ClassType.values().length)];

        // Récupérer tous les talents de cette classe
        List<Talent> talents = talentManager.getTalentsForClass(randomClass);
        if (talents.isEmpty()) {
            return null;
        }

        // Sélectionner un talent aléatoire
        Talent randomTalent = talents.get(new Random().nextInt(talents.size()));

        // Calculer le bonus de qualité basé sur la rareté
        double qualityBonus = rarity.rollQualityBonus();

        // Générer l'éveil
        return registry.generateAwaken(randomTalent, qualityBonus);
    }

    /**
     * Génère un éveil pour une armure
     *
     * @param armorType Type d'armure (HELMET, CHESTPLATE, LEGGINGS, BOOTS)
     * @param rarity Rareté de l'item (influence la qualité)
     * @return Un Awaken généré pour l'armure
     */
    public Awaken generateArmorAwaken(ItemType armorType, Rarity rarity) {
        if (!armorType.isArmor()) {
            plugin.getLogger().warning("[Awaken] generateArmorAwaken appelé avec un type non-armure: " + armorType);
            return null;
        }

        // Calculer le bonus de qualité basé sur la rareté
        double qualityBonus = rarity.rollQualityBonus();

        // Utiliser le template approprié pour le type d'armure
        AwakenTemplate template = registry.getArmorTemplate(armorType);
        return template.generateForArmor(armorType, qualityBonus);
    }

    /**
     * Génère un éveil pour un talent spécifique
     *
     * @param talent Le talent cible
     * @param qualityBonus Bonus de qualité
     * @return Un Awaken généré
     */
    public Awaken generateAwakenForTalent(Talent talent, double qualityBonus) {
        return registry.generateAwaken(talent, qualityBonus);
    }

    /**
     * Génère un éveil pour un talent par son ID
     *
     * @param talentId ID du talent
     * @param qualityBonus Bonus de qualité
     * @return Un Awaken généré ou null si talent non trouvé
     */
    public Awaken generateAwakenForTalentId(String talentId, double qualityBonus) {
        TalentManager talentManager = plugin.getTalentManager();
        if (talentManager == null) return null;

        Talent talent = talentManager.getTalent(talentId);
        if (talent == null) return null;

        return generateAwakenForTalent(talent, qualityBonus);
    }

    // ==================== VALIDATION ====================

    /**
     * Vérifie si un éveil est actif pour un joueur (pour les armes)
     *
     * @param player Le joueur
     * @param awaken L'éveil à vérifier
     * @return true si l'éveil est actif
     */
    public boolean isAwakenActive(Player player, Awaken awaken) {
        if (awaken == null || player == null) return false;

        // Les éveils d'armure sont toujours actifs s'ils n'ont pas de classe requise
        if (awaken.getRequiredClass() == null && awaken.getTargetTalentId() == null) {
            return true; // Éveil d'armure - toujours actif
        }

        ClassData classData = plugin.getClassManager().getClassData(player);
        if (!classData.hasClass()) return false;

        ClassType playerClass = classData.getSelectedClass();
        TalentBranch playerBranch = classData.getSelectedBranch();

        // Vérifier la compatibilité de classe
        if (!awaken.isClassCompatible(playerClass)) {
            return false;
        }

        // Vérifier si le joueur a le talent actif
        return classData.hasTalent(awaken.getTargetTalentId());
    }

    /**
     * Vérifie si un éveil d'armure est actif (toujours actif si l'armure est équipée)
     *
     * @param awaken L'éveil à vérifier
     * @return true si c'est un éveil d'armure (toujours actif)
     */
    public boolean isArmorAwakenActive(Awaken awaken) {
        if (awaken == null) return false;
        // Les éveils d'armure n'ont pas de classe/talent requis
        return awaken.getRequiredClass() == null && awaken.getTargetTalentId() == null;
    }

    /**
     * Vérifie si un éveil est actif pour un joueur via l'item en main (armes uniquement)
     *
     * @param player Le joueur
     * @return L'éveil actif ou null
     */
    public Awaken getActiveAwaken(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!ZombieZItem.isZombieZItem(mainHand)) return null;

        Awaken awaken = getAwakenFromItem(mainHand);
        if (awaken == null) return null;

        return isAwakenActive(player, awaken) ? awaken : null;
    }

    /**
     * Obtient tous les éveils actifs des armures équipées
     *
     * @param player Le joueur
     * @return Liste des éveils d'armures actifs
     */
    public List<Awaken> getActiveArmorAwakens(Player player) {
        List<Awaken> activeAwakens = new ArrayList<>();

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || !ZombieZItem.isZombieZItem(armor)) continue;

            Awaken awaken = getAwakenFromItem(armor);
            if (awaken != null && isArmorAwakenActive(awaken)) {
                activeAwakens.add(awaken);
            }
        }

        return activeAwakens;
    }

    /**
     * Obtient l'éveil d'une pièce d'armure spécifique équipée
     *
     * @param player Le joueur
     * @param slot Le slot d'armure (HEAD, CHEST, LEGS, FEET)
     * @return L'éveil de l'armure ou null
     */
    public Awaken getArmorAwakenForSlot(Player player, org.bukkit.inventory.EquipmentSlot slot) {
        ItemStack armor = switch (slot) {
            case HEAD -> player.getInventory().getHelmet();
            case CHEST -> player.getInventory().getChestplate();
            case LEGS -> player.getInventory().getLeggings();
            case FEET -> player.getInventory().getBoots();
            default -> null;
        };

        if (armor == null || !ZombieZItem.isZombieZItem(armor)) return null;

        Awaken awaken = getAwakenFromItem(armor);
        return (awaken != null && isArmorAwakenActive(awaken)) ? awaken : null;
    }

    /**
     * Calcule les bonus totaux des éveils d'armure pour un joueur
     *
     * @param player Le joueur
     * @return Map des types de modificateurs et leurs valeurs cumulées
     */
    public Map<AwakenModifierType, Double> getTotalArmorAwakenBonuses(Player player) {
        Map<AwakenModifierType, Double> bonuses = new EnumMap<>(AwakenModifierType.class);

        for (Awaken awaken : getActiveArmorAwakens(player)) {
            bonuses.merge(awaken.getModifierType(), awaken.getModifierValue(), Double::sum);
        }

        return bonuses;
    }

    /**
     * Obtient l'éveil actif pour un type d'effet de talent spécifique
     *
     * @param player Le joueur
     * @param effectType Le type d'effet du talent
     * @return L'éveil actif ou null
     */
    public Awaken getActiveAwakenForEffect(Player player, Talent.TalentEffectType effectType) {
        Awaken awaken = getActiveAwaken(player);
        if (awaken == null) return null;

        if (awaken.getTargetEffectType() == effectType) {
            return awaken;
        }

        return null;
    }

    // ==================== SÉRIALISATION ====================

    /**
     * Stocke un éveil dans le PDC d'un item
     *
     * @param item L'ItemStack
     * @param awaken L'éveil à stocker
     */
    public void storeAwakenInItem(ItemStack item, Awaken awaken) {
        if (item == null || awaken == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(keyAwakenId, PersistentDataType.STRING, awaken.getId());
        pdc.set(keyAwakenClass, PersistentDataType.STRING,
            awaken.getRequiredClass() != null ? awaken.getRequiredClass().name() : "");
        pdc.set(keyAwakenTalent, PersistentDataType.STRING,
            awaken.getTargetTalentId() != null ? awaken.getTargetTalentId() : "");
        pdc.set(keyAwakenModType, PersistentDataType.STRING, awaken.getModifierType().name());
        pdc.set(keyAwakenModValue, PersistentDataType.DOUBLE, awaken.getModifierValue());
        pdc.set(keyAwakenDesc, PersistentDataType.STRING,
            awaken.getEffectDescription() != null ? awaken.getEffectDescription() : "");

        item.setItemMeta(meta);
    }

    /**
     * Récupère un éveil depuis le PDC d'un item
     *
     * @param item L'ItemStack
     * @return L'éveil ou null
     */
    public Awaken getAwakenFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (!pdc.has(keyAwakenId, PersistentDataType.STRING)) {
            return null;
        }

        try {
            String id = pdc.get(keyAwakenId, PersistentDataType.STRING);
            String classStr = pdc.get(keyAwakenClass, PersistentDataType.STRING);
            String talentId = pdc.get(keyAwakenTalent, PersistentDataType.STRING);
            String modTypeStr = pdc.get(keyAwakenModType, PersistentDataType.STRING);
            Double modValue = pdc.get(keyAwakenModValue, PersistentDataType.DOUBLE);
            String desc = pdc.get(keyAwakenDesc, PersistentDataType.STRING);

            ClassType classType = null;
            if (classStr != null && !classStr.isEmpty()) {
                try {
                    classType = ClassType.valueOf(classStr);
                } catch (IllegalArgumentException ignored) {}
            }

            AwakenModifierType modType = AwakenModifierType.DAMAGE_BONUS;
            if (modTypeStr != null) {
                try {
                    modType = AwakenModifierType.valueOf(modTypeStr);
                } catch (IllegalArgumentException ignored) {}
            }

            // Récupérer le talent pour le type d'effet
            Talent.TalentEffectType effectType = null;
            if (talentId != null && !talentId.isEmpty() && plugin.getTalentManager() != null) {
                Talent talent = plugin.getTalentManager().getTalent(talentId);
                if (talent != null) {
                    effectType = talent.getEffectType();
                }
            }

            // Récupérer la branche depuis le talent
            TalentBranch branch = null;
            if (talentId != null && classType != null && plugin.getTalentManager() != null) {
                Talent talent = plugin.getTalentManager().getTalent(talentId);
                if (talent != null) {
                    TalentBranch[] branches = TalentBranch.getBranchesForClass(classType);
                    int slot = talent.getSlotIndex();
                    if (slot >= 0 && slot < branches.length) {
                        branch = branches[slot];
                    }
                }
            }

            // Générer le nom depuis le talent
            String displayName = "Éveil";
            if (talentId != null && plugin.getTalentManager() != null) {
                Talent talent = plugin.getTalentManager().getTalent(talentId);
                if (talent != null) {
                    displayName = talent.getName() + " " + getModifierSuffix(modType);
                }
            }

            return Awaken.builder()
                .id(id)
                .displayName(displayName)
                .requiredClass(classType)
                .requiredBranch(branch)
                .targetTalentId(talentId)
                .targetEffectType(effectType)
                .modifierType(modType)
                .modifierValue(modValue != null ? modValue : 0)
                .effectDescription(desc)
                .build();

        } catch (Exception e) {
            plugin.getLogger().warning("[Awaken] Erreur lors de la lecture de l'éveil: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si un item a un éveil
     */
    public boolean hasAwaken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyAwakenId, PersistentDataType.STRING);
    }

    /**
     * Obtient le suffixe pour le nom de l'éveil
     */
    private String getModifierSuffix(AwakenModifierType modType) {
        return switch (modType) {
            case EXTRA_SUMMON -> "Renforcé";
            case DAMAGE_BONUS -> "Dévastateur";
            case CRIT_DAMAGE_BONUS -> "Mortel";
            case CRIT_CHANCE_BONUS -> "Précis";
            case COOLDOWN_REDUCTION -> "Rapide";
            case DURATION_EXTENSION -> "Persistant";
            case RADIUS_BONUS -> "Étendu";
            case EXTRA_PROJECTILE -> "Multiple";
            case EXTRA_BOUNCE -> "Ricochant";
            case PROC_CHANCE_BONUS -> "Fréquent";
            case EXTRA_STACKS -> "Cumulatif";
            case REDUCED_THRESHOLD -> "Optimal";
            case THRESHOLD_BONUS -> "Optimisé";
            case APPLY_SLOW -> "Entravant";
            case APPLY_VULNERABILITY -> "Perçant";
            case SPEED_BUFF -> "Véloce";
            case HEAL_ON_PROC -> "Vital";
            case SHIELD_ON_PROC -> "Protecteur";
            case XP_BONUS -> "Sage";
            case LOOT_BONUS -> "Fortuné";
            case UNIQUE_EFFECT -> "Unique";
            // Types défensifs pour armures
            case DAMAGE_REDUCTION -> "Blindé";
            case ARMOR_BONUS -> "Cuirassé";
            case THORNS_DAMAGE -> "Épineux";
            case HEALTH_BONUS -> "Robuste";
            case BLOCK_CHANCE -> "Gardien";
            case HEALTH_REGEN -> "Régénérant";
            case CC_RESISTANCE -> "Inébranlable";
        };
    }

    // ==================== CONFIGURATION ====================

    /**
     * Charge la configuration depuis un ConfigurationSection
     */
    public void loadFromConfig(ConfigurationSection config) {
        if (config == null) return;

        enabled = config.getBoolean("enabled", true);
        zoneChanceBonus = config.getDouble("zone-chance-bonus", 0.001);
        maxZoneForBonus = config.getInt("max-zone-for-bonus", 50);

        // Charger les chances par rareté
        ConfigurationSection chancesSection = config.getConfigurationSection("awaken-chances");
        if (chancesSection != null) {
            for (Rarity rarity : Rarity.values()) {
                String key = rarity.name().toLowerCase();
                if (chancesSection.contains(key)) {
                    double chance = chancesSection.getDouble(key);
                    awakenChanceByRarity.put(rarity, Math.max(0.0, Math.min(1.0, chance)));
                }
            }
        }

        plugin.getLogger().info("[Awaken] Configuration chargée - Système: " +
            (enabled ? "Activé" : "Désactivé"));
    }

    // ==================== STATISTIQUES ====================

    /**
     * Obtient les statistiques du système
     */
    public String getStats() {
        return String.format(
            "Éveils: Templates=%d | Cache=%d | Système=%s",
            registry.getTemplateCount(),
            awakenCache.size(),
            enabled ? "§aActivé" : "§cDésactivé"
        );
    }

    /**
     * Nettoie le cache
     */
    public void clearCache() {
        awakenCache.clear();
    }

    /**
     * Obtient les chances d'éveil pour le debug
     */
    public Map<Rarity, Double> getAwakenChances() {
        return Collections.unmodifiableMap(awakenChanceByRarity);
    }
}
