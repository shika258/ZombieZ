package com.rinaorc.zombiez.items.generator;

import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.affixes.Affix;
import com.rinaorc.zombiez.items.affixes.AffixRegistry;
import com.rinaorc.zombiez.items.scaling.ZoneScaling;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.Material;

import java.util.*;

/**
 * Générateur d'items procéduraux
 * Génère des items avec stats aléatoires, affixes, et noms générés
 *
 * SYSTÈME DE SCALING REVU:
 * - La ZONE est le facteur PRINCIPAL de puissance (via ZoneScaling)
 * - La RARETÉ définit la COMPLEXITÉ (nombre d'affixes, attributs, proc chances)
 *
 * Utilise le "Holder idiom" pour un singleton thread-safe et lazy
 */
public class ItemGenerator {

    // Holder idiom - thread-safe, lazy initialization sans synchronisation
    private static class Holder {
        static final ItemGenerator INSTANCE = new ItemGenerator();
    }
    
    private final AffixRegistry affixRegistry;
    private final NameGenerator nameGenerator;
    private final Random random;

    private ItemGenerator() {
        this.affixRegistry = AffixRegistry.getInstance();
        this.nameGenerator = new NameGenerator();
        this.random = new Random();
    }

    /**
     * Obtient l'instance unique du générateur (thread-safe)
     */
    public static ItemGenerator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Génère un item complètement aléatoire pour une zone
     */
    public ZombieZItem generate(int zoneId) {
        return generate(zoneId, 0.0);
    }

    /**
     * Génère un item avec bonus de luck
     */
    public ZombieZItem generate(int zoneId, double luckBonus) {
        // Déterminer la rareté
        Rarity rarity = Rarity.roll(luckBonus);
        
        // Déterminer le type d'item
        ItemType itemType = ItemType.random();
        
        return generate(zoneId, rarity, itemType, luckBonus);
    }

    /**
     * Génère un item avec rareté spécifique
     */
    public ZombieZItem generate(int zoneId, Rarity rarity, double luckBonus) {
        ItemType itemType = ItemType.random();
        return generate(zoneId, rarity, itemType, luckBonus);
    }

    /**
     * Génère un item avec type spécifique
     */
    public ZombieZItem generate(int zoneId, ItemType itemType, double luckBonus) {
        Rarity rarity = Rarity.roll(luckBonus);
        return generate(zoneId, rarity, itemType, luckBonus);
    }

    /**
     * Génère un item avec tous les paramètres spécifiés
     *
     * SYSTÈME DE SCALING:
     * - La ZONE est le facteur PRINCIPAL de puissance
     * - La RARETÉ définit la COMPLEXITÉ (nombre d'affixes, etc.)
     */
    public ZombieZItem generate(int zoneId, Rarity rarity, ItemType itemType, double luckBonus) {
        // Déterminer le tier du matériau
        int maxTier = itemType.getMaxTierForZone(zoneId);
        int tier = rollTier(maxTier, rarity);

        // Obtenir le matériau
        Material material = itemType.getMaterialForTier(tier);

        // Générer les stats de base avec ZONE SCALING
        Map<StatType, Double> baseStats = generateBaseStats(itemType, tier, zoneId, rarity);

        // Générer les affixes avec ZONE SCALING
        List<ZombieZItem.RolledAffix> affixes = generateAffixes(itemType, rarity, zoneId);

        // Calculer toutes les stats pour le score
        Map<StatType, Double> allStats = new HashMap<>(baseStats);
        for (ZombieZItem.RolledAffix ra : affixes) {
            for (var entry : ra.getRolledStats().entrySet()) {
                allStats.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        // Calculer le score avec ZONE comme facteur PRINCIPAL
        int itemScore = ZombieZItem.calculateItemScore(zoneId, rarity, allStats, affixes);

        // Calculer l'Item Level (intégration avec PowerManager si disponible)
        int itemLevel = calculateItemLevel(zoneId, rarity);

        // Générer le nom
        String baseName = nameGenerator.getBaseName(itemType);
        String generatedName = nameGenerator.generateFullName(baseName, affixes, rarity);

        // Déterminer si l'item a un pouvoir (sera appliqué plus tard par l'ItemManager)
        String powerId = null; // Sera assigné par l'ItemManager si nécessaire

        // Générer un armor trim pour les armures
        String trimPatternKey = null;
        String trimMaterialKey = null;
        if (itemType.isArmor()) {
            ArmorTrimGenerator.TrimResult trimResult = ArmorTrimGenerator.getInstance().generateTrim(rarity, zoneId);
            if (trimResult != null) {
                trimPatternKey = trimResult.getPatternKey();
                trimMaterialKey = trimResult.getMaterialKey();
            }
        }

        // Construire l'item
        return ZombieZItem.builder()
            .uuid(UUID.randomUUID())
            .itemType(itemType)
            .material(material)
            .rarity(rarity)
            .tier(tier)
            .zoneLevel(zoneId)
            .baseName(baseName)
            .generatedName(generatedName)
            .baseStats(baseStats)
            .affixes(affixes)
            .itemScore(itemScore)
            .createdAt(System.currentTimeMillis())
            .identified(true)
            .itemLevel(itemLevel)
            .powerId(powerId)
            .trimPatternKey(trimPatternKey)
            .trimMaterialKey(trimMaterialKey)
            .build();
    }

    /**
     * Calcule l'Item Level pour un item
     * Utilise une formule simple si le PowerManager n'est pas disponible
     */
    private int calculateItemLevel(int zoneId, Rarity rarity) {
        // Formule simple par défaut: base 10 par zone, avec influence de la rareté
        int baseILVL = Math.min(zoneId * 10, 100);

        // Ajuster selon la rareté
        int minForRarity = switch (rarity) {
            case COMMON -> 1;
            case UNCOMMON -> 10;
            case RARE -> 15;
            case EPIC -> 35;
            case LEGENDARY -> 50;
            case MYTHIC -> 70;
            case EXALTED -> 85;
        };

        int maxForRarity = switch (rarity) {
            case COMMON -> 20;
            case UNCOMMON -> 35;
            case RARE -> 40;
            case EPIC -> 70;
            case LEGENDARY -> 100;
            case MYTHIC -> 100;
            case EXALTED -> 100;
        };

        // Calculer l'ILVL dans la plage
        double zoneProgression = Math.min(1.0, zoneId / 10.0);
        int rangeSpan = maxForRarity - minForRarity;
        int progressionBonus = (int) (rangeSpan * zoneProgression);
        int finalILVL = minForRarity + progressionBonus;

        // Variation aléatoire
        int variation = (int) (finalILVL * 0.1);
        finalILVL += random.nextInt(variation * 2 + 1) - variation;

        // Clamp
        return Math.max(minForRarity, Math.min(maxForRarity, finalILVL));
    }

    /**
     * Génère un item avec rareté minimum garantie (pour boss, etc.)
     */
    public ZombieZItem generateWithMinRarity(int zoneId, Rarity minRarity, double luckBonus) {
        Rarity rarity = Rarity.rollWithMinimum(luckBonus, minRarity);
        return generate(zoneId, rarity, luckBonus);
    }

    /**
     * Génère plusieurs items
     */
    public List<ZombieZItem> generateMultiple(int zoneId, int count, double luckBonus) {
        List<ZombieZItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(generate(zoneId, luckBonus));
        }
        return items;
    }

    /**
     * Roule le tier du matériau
     */
    private int rollTier(int maxTier, Rarity rarity) {
        if (maxTier <= 0) return 0;
        
        // Les raretés plus hautes favorisent les tiers plus élevés
        double rarityBonus = rarity.ordinal() * 0.1;
        
        // Distribution pondérée vers les tiers supérieurs
        double roll = random.nextDouble();
        roll = Math.pow(roll, 1.0 - rarityBonus); // Bias vers 1.0
        
        return (int) (roll * (maxTier + 1));
    }

    /**
     * Génère les stats de base selon le type, tier et ZONE
     *
     * SYSTÈME DE SCALING:
     * - La ZONE multiplie les valeurs de base (facteur PRINCIPAL)
     * - Le qualityBonus ajoute une légère variation (PAS de puissance brute)
     */
    private Map<StatType, Double> generateBaseStats(ItemType itemType, int tier, int zoneId, Rarity rarity) {
        Map<StatType, Double> stats = new HashMap<>();

        // Obtenir le multiplicateur de zone (facteur PRINCIPAL de puissance)
        double zoneMultiplier = ZoneScaling.getBaseStatMultiplier(zoneId);

        // Léger bonus de qualité basé sur la rareté (max +30%)
        double qualityBonus = rarity.rollQualityBonus();

        if (itemType.isWeapon()) {
            // Arme: Dégâts + Vitesse d'attaque
            double baseDamage = itemType.getBaseStat1ForTier(tier);
            double baseSpeed = itemType.getBaseStat2ForTier(tier);

            // Appliquer variation aléatoire (-10% à +20%)
            double damageVariation = 0.9 + random.nextDouble() * 0.3;
            double speedVariation = 0.95 + random.nextDouble() * 0.1;

            // Appliquer ZONE SCALING (facteur PRINCIPAL) puis qualité (léger bonus)
            double finalDamage = baseDamage * damageVariation * zoneMultiplier * (1 + qualityBonus);
            double finalSpeed = baseSpeed * speedVariation;

            stats.put(StatType.DAMAGE, Math.round(finalDamage * 10) / 10.0);
            stats.put(StatType.ATTACK_SPEED, Math.round(finalSpeed * 100) / 100.0);

        } else if (itemType.isArmor()) {
            // Armure: Armure + Résistance
            double baseArmor = itemType.getBaseStat1ForTier(tier);
            double baseToughness = itemType.getBaseStat2ForTier(tier);

            double armorVariation = 0.9 + random.nextDouble() * 0.3;

            // Appliquer ZONE SCALING (facteur PRINCIPAL) puis qualité (léger bonus)
            double finalArmor = baseArmor * armorVariation * zoneMultiplier * (1 + qualityBonus);
            double finalToughness = baseToughness * zoneMultiplier * (1 + qualityBonus);

            stats.put(StatType.ARMOR, Math.round(finalArmor * 10) / 10.0);

            if (baseToughness > 0) {
                stats.put(StatType.ARMOR_TOUGHNESS, Math.round(finalToughness * 10) / 10.0);
            }
        }

        return stats;
    }

    /**
     * Génère les affixes pour un item avec ZONE SCALING
     *
     * SYSTÈME DE SCALING:
     * - La ZONE multiplie les valeurs des affixes (facteur PRINCIPAL)
     * - La RARETÉ détermine le nombre d'affixes et les tiers accessibles
     */
    private List<ZombieZItem.RolledAffix> generateAffixes(ItemType itemType, Rarity rarity, int zoneId) {
        List<ZombieZItem.RolledAffix> affixes = new ArrayList<>();
        Set<String> usedAffixIds = new HashSet<>();

        int affixCount = rarity.getAffixCount();

        // Bonus d'affixes selon la zone (zones hautes = plus de chance d'affixes bonus)
        if (zoneId >= 8 && random.nextDouble() < 0.2) {
            affixCount++;
        }

        // Distribution: généralement 1 prefix et le reste en suffix
        int prefixCount = Math.min(affixCount, 1 + (affixCount > 3 ? 1 : 0));
        int suffixCount = affixCount - prefixCount;

        // Léger bonus de qualité basé sur la rareté (PAS de puissance brute)
        double qualityBonus = rarity.rollQualityBonus();

        // Tier maximum accessible selon la rareté
        int maxTier = rarity.getMaxAffixTier();

        // Générer les préfixes avec ZONE SCALING
        for (int i = 0; i < prefixCount; i++) {
            Affix affix = affixRegistry.rollAffix(itemType, Affix.AffixType.PREFIX, zoneId, usedAffixIds, maxTier);
            if (affix != null) {
                // NOUVEAU: rollStats avec zone et qualité
                Map<StatType, Double> rolledStats = affix.rollStats(zoneId, qualityBonus);
                affixes.add(ZombieZItem.RolledAffix.builder()
                    .affix(affix)
                    .rolledStats(rolledStats)
                    .build());
                usedAffixIds.add(affix.getId());
            }
        }

        // Générer les suffixes avec ZONE SCALING
        for (int i = 0; i < suffixCount; i++) {
            Affix affix = affixRegistry.rollAffix(itemType, Affix.AffixType.SUFFIX, zoneId, usedAffixIds, maxTier);
            if (affix != null) {
                // NOUVEAU: rollStats avec zone et qualité
                Map<StatType, Double> rolledStats = affix.rollStats(zoneId, qualityBonus);
                affixes.add(ZombieZItem.RolledAffix.builder()
                    .affix(affix)
                    .rolledStats(rolledStats)
                    .build());
                usedAffixIds.add(affix.getId());
            }
        }

        return affixes;
    }

    /**
     * Générateur de noms pour les items
     * Thème: Survie post-apocalyptique / Zombie
     */
    private static class NameGenerator {

        private final Map<ItemType, List<String>> baseNames = new HashMap<>();
        private final Random random = new Random();

        // Titres thématiques par rareté
        private static final String[] LEGENDARY_TITLES = {
            "Relique", "Survivant", "Dernier", "Légendaire", "Ancien"
        };

        private static final String[] MYTHIC_TITLES = {
            "Fléau des Morts", "Purificateur", "Extinction", "Patient Zéro", "Héritage"
        };

        private static final String[] EXALTED_TITLES = {
            "Némésis", "Apocalypse", "Omega", "Alpha", "Jugement"
        };

        public NameGenerator() {
            initializeNames();
        }

        private void initializeNames() {
            // ==================== ARMES DE MÊLÉE ====================

            // Épées - Mix militaire/improvisé/survie
            baseNames.put(ItemType.SWORD, Arrays.asList(
                "Lame",              // Générique mais efficace
                "Machette",          // Classique survie zombie
                "Coupe-coupe",       // Outil de survie
                "Sabre",             // Militaire
                "Tranchoir",         // Improvisé/boucher
                "Katana",            // Iconique zombie fiction
                "Lame Brisée",       // Récupéré
                "Fendoir"            // Brutal/improvisé
            ));

            // Haches - Outils de survie convertis
            baseNames.put(ItemType.AXE, Arrays.asList(
                "Hache",             // Classique
                "Hachette",          // Survie compacte
                "Hache de Pompier",  // Urgence/sauvetage
                "Cognée",            // Bûcheron
                "Hache Tactique",    // Militaire moderne
                "Tomahawk",          // Survie/combat
                "Hache Rouillée"     // Récupérée
            ));

            // Masse - Armes contondantes improvisées
            baseNames.put(ItemType.MACE, Arrays.asList(
                "Masse",             // Base
                "Batte",             // Baseball classique
                "Matraque",          // Sécurité/police
                "Marteau",           // Outil
                "Pied-de-Biche",     // Outil polyvalent
                "Barre de Fer",      // Improvisé
                "Concasseur"         // Brutal
            ));

            // ==================== ARMES À DISTANCE ====================

            // Arcs - Armes silencieuses de survie
            baseNames.put(ItemType.BOW, Arrays.asList(
                "Arc",               // Base
                "Arc de Chasse",     // Survivaliste
                "Arc Composite",     // Moderne
                "Arc Improvisé",     // Récupéré
                "Arc Silencieux",    // Tactique
                "Arc de Survie"      // Thématique
            ));

            // Arbalètes - Puissance et précision
            baseNames.put(ItemType.CROSSBOW, Arrays.asList(
                "Arbalète",          // Base
                "Arbalète Tactique", // Militaire
                "Carreau Mortel",    // Agressif
                "Arbalète Lourde",   // Puissante
                "Perce-Crâne"        // Thématique zombie
            ));

            // Trident - Armes exotiques de survie
            baseNames.put(ItemType.TRIDENT, Arrays.asList(
                "Harpon",            // Maritime/survie
                "Fourche",           // Agricole réutilisé
                "Trident",           // Base
                "Pique",             // Défense
                "Lance Improvisée"   // Récupéré
            ));

            // ==================== ARMURES ====================

            // Casques - Protection de tête survie/tactique
            baseNames.put(ItemType.HELMET, Arrays.asList(
                "Casque",            // Base
                "Casque Tactique",   // Militaire
                "Casque Anti-Émeute",// Police
                "Masque de Survie",  // Post-apo
                "Casque de Chantier",// Improvisé
                "Heaume Blindé",     // Lourd
                "Capuche Renforcée"  // Léger
            ));

            // Plastrons - Protection du torse
            baseNames.put(ItemType.CHESTPLATE, Arrays.asList(
                "Plastron",          // Base
                "Gilet Pare-Balles", // Tactique
                "Gilet Tactique",    // Militaire
                "Armure de Fortune", // Improvisé
                "Cuirasse",          // Lourd
                "Veste Renforcée",   // Léger
                "Armure de Survie"   // Thématique
            ));

            // Jambières - Protection des jambes
            baseNames.put(ItemType.LEGGINGS, Arrays.asList(
                "Jambières",         // Base
                "Pantalon Tactique", // Militaire
                "Pantalon Cargo",    // Survie
                "Protèges-Jambes",   // Protection
                "Cuissards",         // Armure
                "Pantalon Renforcé"  // Improvisé
            ));

            // Bottes - Chaussures de survie
            baseNames.put(ItemType.BOOTS, Arrays.asList(
                "Bottes",            // Base
                "Bottes de Combat",  // Militaire
                "Rangers",           // Tactique
                "Bottes de Survie",  // Thématique
                "Chaussures Lourdes",// Protection
                "Bottes Renforcées"  // Improvisé
            ));

            // Bouclier - Protection défensive
            baseNames.put(ItemType.SHIELD, Arrays.asList(
                "Bouclier",          // Base
                "Bouclier Anti-Émeute", // Police
                "Plaque de Blindage",   // Improvisé
                "Bouclier Tactique",    // Militaire
                "Barricade Portative",  // Survie
                "Panneau Renforcé"      // Récupéré
            ));
        }

        /**
         * Obtient un nom de base aléatoire pour un type
         */
        public String getBaseName(ItemType type) {
            List<String> names = baseNames.getOrDefault(type, List.of(type.getDisplayName()));
            return names.get(random.nextInt(names.size()));
        }

        /**
         * Génère le nom complet avec préfixes et suffixes
         * Structure: [PRÉFIXE] [NOM DE BASE] [SUFFIXE]
         * Ex: "Infecté Machette du Survivant"
         */
        public String generateFullName(String baseName, List<ZombieZItem.RolledAffix> affixes, Rarity rarity) {
            StringBuilder name = new StringBuilder();

            // Ajouter le préfixe d'affix (si présent)
            Optional<ZombieZItem.RolledAffix> prefix = affixes.stream()
                .filter(a -> a.getAffix().getType() == Affix.AffixType.PREFIX)
                .findFirst();

            if (prefix.isPresent()) {
                name.append(prefix.get().getAffix().getDisplayName()).append(" ");
            }

            // Nom de base
            name.append(baseName);

            // Ajouter le suffixe d'affix (si présent)
            Optional<ZombieZItem.RolledAffix> suffix = affixes.stream()
                .filter(a -> a.getAffix().getType() == Affix.AffixType.SUFFIX)
                .findFirst();

            if (suffix.isPresent()) {
                name.append(" ").append(suffix.get().getAffix().getDisplayName());
            }

            // Pour les raretés élevées sans affixes, ajouter un titre thématique
            if (affixes.isEmpty()) {
                String title = getTitleForRarity(rarity);
                if (title != null) {
                    name.insert(0, title + " ");
                }
            }

            return name.toString();
        }

        /**
         * Obtient un titre thématique selon la rareté
         */
        private String getTitleForRarity(Rarity rarity) {
            return switch (rarity) {
                case LEGENDARY -> LEGENDARY_TITLES[random.nextInt(LEGENDARY_TITLES.length)];
                case MYTHIC -> MYTHIC_TITLES[random.nextInt(MYTHIC_TITLES.length)];
                case EXALTED -> EXALTED_TITLES[random.nextInt(EXALTED_TITLES.length)];
                default -> null;
            };
        }
    }
}
