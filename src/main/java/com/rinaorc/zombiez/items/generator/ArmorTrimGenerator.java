package com.rinaorc.zombiez.items.generator;

import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

/**
 * Générateur de trims d'armure pour enrichir visuellement les items
 *
 * Les armor trims ajoutent des motifs décoratifs aux armures.
 * Ce générateur sélectionne des combinaisons pattern/matériau
 * en fonction de la rareté et de la zone de l'item.
 */
public class ArmorTrimGenerator {

    // Holder idiom - thread-safe, lazy initialization
    private static class Holder {
        static final ArmorTrimGenerator INSTANCE = new ArmorTrimGenerator();
    }

    private final Random random;

    // Patterns organisés par "prestige" (les plus rares en dernier)
    private final List<TrimPattern> commonPatterns;
    private final List<TrimPattern> rarePatterns;
    private final List<TrimPattern> epicPatterns;

    // Matériaux organisés par valeur visuelle
    private final List<TrimMaterial> basicMaterials;
    private final List<TrimMaterial> valuableMaterials;
    private final List<TrimMaterial> prestigeMaterials;

    private ArmorTrimGenerator() {
        this.random = new Random();

        // Initialiser les listes de patterns par tier
        this.commonPatterns = new ArrayList<>();
        this.rarePatterns = new ArrayList<>();
        this.epicPatterns = new ArrayList<>();

        // Initialiser les listes de matériaux par tier
        this.basicMaterials = new ArrayList<>();
        this.valuableMaterials = new ArrayList<>();
        this.prestigeMaterials = new ArrayList<>();

        initializePatterns();
        initializeMaterials();
    }

    public static ArmorTrimGenerator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Initialise les patterns de trim par catégorie de prestige
     */
    private void initializePatterns() {
        // Patterns communs (Smithing Templates faciles à obtenir)
        addPatternSafely(commonPatterns, "coast");
        addPatternSafely(commonPatterns, "dune");
        addPatternSafely(commonPatterns, "wild");
        addPatternSafely(commonPatterns, "sentry");

        // Patterns rares (Smithing Templates plus difficiles)
        addPatternSafely(rarePatterns, "vex");
        addPatternSafely(rarePatterns, "rib");
        addPatternSafely(rarePatterns, "snout");
        addPatternSafely(rarePatterns, "tide");
        addPatternSafely(rarePatterns, "ward");
        addPatternSafely(rarePatterns, "eye");
        addPatternSafely(rarePatterns, "shaper");
        addPatternSafely(rarePatterns, "raiser");
        addPatternSafely(rarePatterns, "host");
        addPatternSafely(rarePatterns, "wayfinder");

        // Patterns épiques (les plus prestigieux)
        addPatternSafely(epicPatterns, "spire");
        addPatternSafely(epicPatterns, "silence");
        addPatternSafely(epicPatterns, "flow");
        addPatternSafely(epicPatterns, "bolt");
    }

    /**
     * Initialise les matériaux de trim par catégorie de valeur
     */
    private void initializeMaterials() {
        // Matériaux basiques
        addMaterialSafely(basicMaterials, "copper");
        addMaterialSafely(basicMaterials, "iron");
        addMaterialSafely(basicMaterials, "redstone");
        addMaterialSafely(basicMaterials, "lapis");
        addMaterialSafely(basicMaterials, "quartz");

        // Matériaux de valeur moyenne
        addMaterialSafely(valuableMaterials, "gold");
        addMaterialSafely(valuableMaterials, "emerald");
        addMaterialSafely(valuableMaterials, "diamond");
        addMaterialSafely(valuableMaterials, "amethyst");

        // Matériaux prestigieux
        addMaterialSafely(prestigeMaterials, "netherite");
    }

    /**
     * Ajoute un pattern de manière sécurisée (vérifie l'existence)
     */
    private void addPatternSafely(List<TrimPattern> list, String key) {
        try {
            TrimPattern pattern = Registry.TRIM_PATTERN.get(org.bukkit.NamespacedKey.minecraft(key));
            if (pattern != null) {
                list.add(pattern);
            }
        } catch (Exception ignored) {
            // Pattern non disponible dans cette version
        }
    }

    /**
     * Ajoute un matériau de manière sécurisée (vérifie l'existence)
     */
    private void addMaterialSafely(List<TrimMaterial> list, String key) {
        try {
            TrimMaterial material = Registry.TRIM_MATERIAL.get(org.bukkit.NamespacedKey.minecraft(key));
            if (material != null) {
                list.add(material);
            }
        } catch (Exception ignored) {
            // Matériau non disponible dans cette version
        }
    }

    /**
     * Génère un trim aléatoire basé sur la rareté de l'item
     *
     * @param rarity La rareté de l'item
     * @param zoneId La zone où l'item a été droppé
     * @return Un TrimResult contenant le pattern et le matériau, ou null si pas de trim
     */
    public TrimResult generateTrim(Rarity rarity, int zoneId) {
        // Probabilité d'avoir un trim selon la rareté
        double trimChance = getTrimChance(rarity);

        if (random.nextDouble() > trimChance) {
            return null; // Pas de trim pour cet item
        }

        TrimPattern pattern = selectPattern(rarity);
        TrimMaterial material = selectMaterial(rarity, zoneId);

        if (pattern == null || material == null) {
            return null;
        }

        return new TrimResult(pattern, material);
    }

    /**
     * Génère un trim pour un mob (sans vérification de probabilité)
     * Utilisé par ZombieManager pour équiper les zombies
     *
     * @param rarity La rareté simulée basée sur le niveau du mob
     * @param zoneId La zone du mob
     * @return Un TrimResult contenant le pattern et le matériau, ou null si erreur
     */
    public TrimResult generateTrimForMob(Rarity rarity, int zoneId) {
        TrimPattern pattern = selectPattern(rarity);
        TrimMaterial material = selectMaterial(rarity, zoneId);

        if (pattern == null || material == null) {
            return null;
        }

        return new TrimResult(pattern, material);
    }

    /**
     * Probabilité d'avoir un trim selon la rareté
     */
    private double getTrimChance(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.10;      // 10% des commons ont un trim
            case UNCOMMON -> 0.25;    // 25% des uncommons
            case RARE -> 0.50;        // 50% des rares
            case EPIC -> 0.75;        // 75% des épiques
            case LEGENDARY -> 0.90;   // 90% des légendaires
            case MYTHIC -> 0.95;      // 95% des mythiques
            case EXALTED -> 1.0;      // 100% des exaltés
        };
    }

    /**
     * Sélectionne un pattern basé sur la rareté
     */
    private TrimPattern selectPattern(Rarity rarity) {
        List<TrimPattern> availablePatterns = getAvailablePatterns(rarity);

        if (availablePatterns.isEmpty()) {
            return null;
        }

        return availablePatterns.get(random.nextInt(availablePatterns.size()));
    }

    /**
     * Obtient les patterns disponibles selon la rareté
     */
    private List<TrimPattern> getAvailablePatterns(Rarity rarity) {
        List<TrimPattern> available = new ArrayList<>();

        // Tous ont accès aux patterns communs
        available.addAll(commonPatterns);

        // À partir de RARE, accès aux patterns rares
        if (rarity.isAtLeast(Rarity.RARE)) {
            available.addAll(rarePatterns);
        }

        // À partir de LEGENDARY, accès aux patterns épiques
        if (rarity.isAtLeast(Rarity.LEGENDARY)) {
            available.addAll(epicPatterns);
        }

        return available;
    }

    /**
     * Sélectionne un matériau basé sur la rareté et la zone
     */
    private TrimMaterial selectMaterial(Rarity rarity, int zoneId) {
        List<TrimMaterial> availableMaterials = getAvailableMaterials(rarity, zoneId);

        if (availableMaterials.isEmpty()) {
            return null;
        }

        // Pour les hautes raretés, favoriser les matériaux prestigieux
        if (rarity.isAtLeast(Rarity.LEGENDARY) && !prestigeMaterials.isEmpty() && random.nextDouble() < 0.5) {
            return prestigeMaterials.get(random.nextInt(prestigeMaterials.size()));
        }

        return availableMaterials.get(random.nextInt(availableMaterials.size()));
    }

    /**
     * Obtient les matériaux disponibles selon la rareté et la zone
     */
    private List<TrimMaterial> getAvailableMaterials(Rarity rarity, int zoneId) {
        List<TrimMaterial> available = new ArrayList<>();

        // Tous ont accès aux matériaux basiques
        available.addAll(basicMaterials);

        // À partir de UNCOMMON ou zone 3+, accès aux matériaux de valeur
        if (rarity.isAtLeast(Rarity.UNCOMMON) || zoneId >= 3) {
            available.addAll(valuableMaterials);
        }

        // À partir de EPIC ou zone 6+, accès aux matériaux prestigieux
        if (rarity.isAtLeast(Rarity.EPIC) || zoneId >= 6) {
            available.addAll(prestigeMaterials);
        }

        return available;
    }

    /**
     * Obtient un trim spécifique par ses clés (pour restauration depuis PDC)
     */
    public TrimResult getTrimByKeys(String patternKey, String materialKey) {
        try {
            TrimPattern pattern = Registry.TRIM_PATTERN.get(org.bukkit.NamespacedKey.minecraft(patternKey));
            TrimMaterial material = Registry.TRIM_MATERIAL.get(org.bukkit.NamespacedKey.minecraft(materialKey));

            if (pattern != null && material != null) {
                return new TrimResult(pattern, material);
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Résultat de génération de trim
     */
    public record TrimResult(TrimPattern pattern, TrimMaterial material) {

        /**
         * Obtient la clé du pattern pour sérialisation
         */
        public String getPatternKey() {
            return pattern.key().getKey();
        }

        /**
         * Obtient la clé du matériau pour sérialisation
         */
        public String getMaterialKey() {
            return material.key().getKey();
        }
    }
}
