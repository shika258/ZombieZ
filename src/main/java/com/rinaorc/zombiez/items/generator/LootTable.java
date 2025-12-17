package com.rinaorc.zombiez.items.generator;

import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Système de tables de loot pour définir les drops
 * Utilisé pour les zombies, boss, coffres, etc.
 */
public class LootTable {

    @Getter
    private final String id;
    private final List<LootEntry> entries;
    private final double baseDropChance;
    private final int guaranteedDrops;

    @Builder
    public LootTable(String id, List<LootEntry> entries, double baseDropChance, int guaranteedDrops) {
        this.id = id;
        this.entries = entries != null ? entries : new ArrayList<>();
        this.baseDropChance = baseDropChance;
        this.guaranteedDrops = guaranteedDrops;
    }

    /**
     * Génère le loot basé sur cette table
     */
    public List<ZombieZItem> generateLoot(int zoneId, double luckBonus) {
        List<ZombieZItem> loot = new ArrayList<>();
        ItemGenerator generator = ItemGenerator.getInstance();
        Random random = new Random();

        // Drops garantis
        for (int i = 0; i < guaranteedDrops; i++) {
            LootEntry entry = rollEntry(random);
            if (entry != null) {
                ZombieZItem item = entry.generate(generator, zoneId, luckBonus);
                if (item != null) {
                    loot.add(item);
                }
            }
        }

        // Drops supplémentaires basés sur la chance
        double adjustedChance = baseDropChance * (1 + luckBonus);
        if (random.nextDouble() < adjustedChance) {
            LootEntry entry = rollEntry(random);
            if (entry != null) {
                ZombieZItem item = entry.generate(generator, zoneId, luckBonus);
                if (item != null) {
                    loot.add(item);
                }
            }
        }

        return loot;
    }

    /**
     * Tire une entrée au sort basée sur les poids
     */
    private LootEntry rollEntry(Random random) {
        if (entries.isEmpty()) return null;

        int totalWeight = entries.stream().mapToInt(LootEntry::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (LootEntry entry : entries) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                return entry;
            }
        }

        return entries.get(0);
    }

    /**
     * Entrée individuelle dans une table de loot
     */
    @Getter
    @Builder
    public static class LootEntry {
        private final int weight;           // Poids pour le tirage
        private final ItemType itemType;    // Type spécifique (null = aléatoire)
        private final Rarity minRarity;     // Rareté minimum garantie
        private final Rarity maxRarity;     // Rareté maximum possible
        private final double rarityBoost;   // Bonus de rareté pour cette entrée

        public ZombieZItem generate(ItemGenerator generator, int zoneId, double luckBonus) {
            double totalLuck = luckBonus + rarityBoost;
            
            // Déterminer la rareté
            Rarity rarity = Rarity.roll(totalLuck);
            
            // Appliquer les contraintes
            if (minRarity != null && rarity.ordinal() < minRarity.ordinal()) {
                rarity = minRarity;
            }
            if (maxRarity != null && rarity.ordinal() > maxRarity.ordinal()) {
                rarity = maxRarity;
            }

            // Générer l'item
            if (itemType != null) {
                return generator.generate(zoneId, rarity, itemType, totalLuck);
            } else {
                return generator.generate(zoneId, rarity, totalLuck);
            }
        }
    }

    /**
     * Registre des tables de loot prédéfinies
     */
    public static class LootTableRegistry {
        private static LootTableRegistry instance;
        private final Map<String, LootTable> tables = new HashMap<>();

        private LootTableRegistry() {
            initializeTables();
        }

        public static LootTableRegistry getInstance() {
            if (instance == null) {
                synchronized (LootTableRegistry.class) {
                    if (instance == null) {
                        instance = new LootTableRegistry();
                    }
                }
            }
            return instance;
        }

        private void initializeTables() {
            // ==================== ZOMBIES NORMAUX ====================
            
            // Zombie Tier 1 (Walker, Crawler, Runner)
            registerTable(LootTable.builder()
                .id("zombie_tier1")
                .baseDropChance(0.15) // 15% de drop
                .guaranteedDrops(0)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(80)
                        .maxRarity(Rarity.UNCOMMON)
                        .rarityBoost(0)
                        .build(),
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.RARE)
                        .rarityBoost(0.1)
                        .build()
                ))
                .build());

            // Zombie Tier 2 (Armored, Spitter, Screamer)
            registerTable(LootTable.builder()
                .id("zombie_tier2")
                .baseDropChance(0.25)
                .guaranteedDrops(0)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(60)
                        .maxRarity(Rarity.RARE)
                        .rarityBoost(0.05)
                        .build(),
                    LootEntry.builder()
                        .weight(30)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.1)
                        .build(),
                    LootEntry.builder()
                        .weight(10)
                        .minRarity(Rarity.RARE)
                        .rarityBoost(0.2)
                        .build()
                ))
                .build());

            // Zombie Tier 3 (Berserker, Shadow, Necromancer, Explosive)
            // Nerfé: 35% → 18% pour rendre le loot plus rare et satisfaisant
            registerTable(LootTable.builder()
                .id("zombie_tier3")
                .baseDropChance(0.18)
                .guaranteedDrops(0)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(55)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.RARE)
                        .rarityBoost(0.05)
                        .build(),
                    LootEntry.builder()
                        .weight(35)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.1)
                        .build(),
                    LootEntry.builder()
                        .weight(10)
                        .minRarity(Rarity.EPIC)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.15)
                        .build()
                ))
                .build());

            // Zombie Tier 4 (Colossus, Spectre, Ravager, Creaking)
            // Nerfé: 50% → 22% pour rendre le loot plus rare et satisfaisant
            registerTable(LootTable.builder()
                .id("zombie_tier4")
                .baseDropChance(0.22)
                .guaranteedDrops(0)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(50)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.1)
                        .build(),
                    LootEntry.builder()
                        .weight(35)
                        .minRarity(Rarity.EPIC)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.15)
                        .build(),
                    LootEntry.builder()
                        .weight(15)
                        .minRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.2)
                        .build()
                ))
                .build());

            // Zombie Tier 5 (Corrupted Warden, Archon)
            // Nerfé: 75% + 1 garanti → 30% sans garanti, rareté réduite
            registerTable(LootTable.builder()
                .id("zombie_tier5")
                .baseDropChance(0.30)
                .guaranteedDrops(0)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(45)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.1)
                        .build(),
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.EPIC)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.15)
                        .build(),
                    LootEntry.builder()
                        .weight(15)
                        .minRarity(Rarity.LEGENDARY)
                        .maxRarity(Rarity.MYTHIC)
                        .rarityBoost(0.25)
                        .build()
                ))
                .build());

            // ==================== MINI-BOSS ====================
            
            registerTable(LootTable.builder()
                .id("mini_boss")
                .baseDropChance(1.0) // 100% de drop
                .guaranteedDrops(2)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.2)
                        .build(),
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.EPIC)
                        .rarityBoost(0.3)
                        .build(),
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.4)
                        .build()
                ))
                .build());

            // ==================== BOSS DE ZONE ====================
            
            registerTable(LootTable.builder()
                .id("zone_boss")
                .baseDropChance(1.0)
                .guaranteedDrops(3)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(30)
                        .minRarity(Rarity.EPIC)
                        .maxRarity(Rarity.MYTHIC)
                        .rarityBoost(0.3)
                        .build(),
                    LootEntry.builder()
                        .weight(50)
                        .minRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.4)
                        .build(),
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.MYTHIC)
                        .rarityBoost(0.5)
                        .build()
                ))
                .build());

            // ==================== BOSS FINAL (Patient Zéro) ====================
            
            registerTable(LootTable.builder()
                .id("final_boss")
                .baseDropChance(1.0)
                .guaranteedDrops(5)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.LEGENDARY)
                        .maxRarity(Rarity.EXALTED)
                        .rarityBoost(0.5)
                        .build(),
                    LootEntry.builder()
                        .weight(50)
                        .minRarity(Rarity.MYTHIC)
                        .rarityBoost(0.6)
                        .build(),
                    LootEntry.builder()
                        .weight(30)
                        .minRarity(Rarity.EXALTED) // Chance d'EXALTED!
                        .rarityBoost(0.75)
                        .build()
                ))
                .build());

            // ==================== COFFRES ====================
            
            // Coffre commun (trouvé partout)
            registerTable(LootTable.builder()
                .id("chest_common")
                .baseDropChance(1.0)
                .guaranteedDrops(1)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(70)
                        .maxRarity(Rarity.UNCOMMON)
                        .build(),
                    LootEntry.builder()
                        .weight(25)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.RARE)
                        .build(),
                    LootEntry.builder()
                        .weight(5)
                        .minRarity(Rarity.RARE)
                        .build()
                ))
                .build());

            // Coffre rare (zones 4+)
            registerTable(LootTable.builder()
                .id("chest_rare")
                .baseDropChance(1.0)
                .guaranteedDrops(2)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(50)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.1)
                        .build(),
                    LootEntry.builder()
                        .weight(35)
                        .minRarity(Rarity.RARE)
                        .rarityBoost(0.2)
                        .build(),
                    LootEntry.builder()
                        .weight(15)
                        .minRarity(Rarity.EPIC)
                        .rarityBoost(0.3)
                        .build()
                ))
                .build());

            // Coffre légendaire (zones 7+)
            registerTable(LootTable.builder()
                .id("chest_legendary")
                .baseDropChance(1.0)
                .guaranteedDrops(3)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.2)
                        .build(),
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.EPIC)
                        .rarityBoost(0.3)
                        .build(),
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.4)
                        .build()
                ))
                .build());

            // ==================== ÉVÉNEMENTS ====================
            
            // Vague de zombies
            registerTable(LootTable.builder()
                .id("horde_event")
                .baseDropChance(0.5)
                .guaranteedDrops(1)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(60)
                        .minRarity(Rarity.UNCOMMON)
                        .maxRarity(Rarity.EPIC)
                        .rarityBoost(0.15)
                        .build(),
                    LootEntry.builder()
                        .weight(30)
                        .minRarity(Rarity.RARE)
                        .rarityBoost(0.25)
                        .build(),
                    LootEntry.builder()
                        .weight(10)
                        .minRarity(Rarity.EPIC)
                        .rarityBoost(0.35)
                        .build()
                ))
                .build());

            // Blood Moon
            registerTable(LootTable.builder()
                .id("blood_moon")
                .baseDropChance(0.6)
                .guaranteedDrops(1)
                .entries(List.of(
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.RARE)
                        .maxRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.25)
                        .build(),
                    LootEntry.builder()
                        .weight(40)
                        .minRarity(Rarity.EPIC)
                        .rarityBoost(0.35)
                        .build(),
                    LootEntry.builder()
                        .weight(20)
                        .minRarity(Rarity.LEGENDARY)
                        .rarityBoost(0.5)
                        .build()
                ))
                .build());
        }

        private void registerTable(LootTable table) {
            tables.put(table.getId(), table);
        }

        public LootTable getTable(String id) {
            return tables.get(id);
        }

        public Collection<LootTable> getAllTables() {
            return Collections.unmodifiableCollection(tables.values());
        }

        /**
         * Obtient la table appropriée pour un type de zombie
         */
        public LootTable getTableForZombieTier(int tier) {
            return switch (tier) {
                case 1 -> getTable("zombie_tier1");
                case 2 -> getTable("zombie_tier2");
                case 3 -> getTable("zombie_tier3");
                case 4 -> getTable("zombie_tier4");
                case 5 -> getTable("zombie_tier5");
                default -> getTable("zombie_tier1");
            };
        }
    }
}
