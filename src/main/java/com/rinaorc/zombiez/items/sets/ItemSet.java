package com.rinaorc.zombiez.items.sets;

import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.StatType;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Représente un set d'équipement avec bonus
 */
@Getter
@Builder
public class ItemSet {

    private final String id;
    private final String displayName;
    private final String colorCode;
    private final String description;
    
    // Pièces du set (slot -> itemType requis)
    private final Map<ItemType, String> pieces; // ItemType -> piece_id
    
    // Bonus par nombre de pièces équipées
    private final Map<Integer, SetBonus> bonuses;
    
    // Zone minimum pour drop
    private final int minZone;

    /**
     * Obtient le nombre de pièces équipées par un joueur
     */
    public int getEquippedCount(Player player, java.util.function.Function<ItemStack, ZombieZItem> itemResolver) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) {
                ZombieZItem zItem = itemResolver.apply(item);
                if (zItem != null && id.equals(zItem.getSetId())) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * Obtient tous les bonus actifs pour un nombre de pièces
     */
    public List<SetBonus> getActiveBonuses(int pieceCount) {
        List<SetBonus> active = new ArrayList<>();
        
        for (var entry : bonuses.entrySet()) {
            if (pieceCount >= entry.getKey()) {
                active.add(entry.getValue());
            }
        }
        
        return active;
    }

    /**
     * Calcule les stats totales des bonus de set
     */
    public Map<StatType, Double> calculateBonusStats(int pieceCount) {
        Map<StatType, Double> stats = new EnumMap<>(StatType.class);
        
        for (SetBonus bonus : getActiveBonuses(pieceCount)) {
            for (var entry : bonus.getStatBonuses().entrySet()) {
                stats.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        
        return stats;
    }

    /**
     * Obtient les lignes de lore pour afficher les bonus du set
     */
    public List<String> getLoreLines(int equippedCount) {
        List<String> lore = new ArrayList<>();
        
        lore.add("");
        lore.add(colorCode + "§l" + displayName + " §7(" + equippedCount + "/" + pieces.size() + ")");
        
        for (var entry : bonuses.entrySet()) {
            int required = entry.getKey();
            SetBonus bonus = entry.getValue();
            
            String color = equippedCount >= required ? "§a" : "§8";
            String checkmark = equippedCount >= required ? "✓" : "○";
            
            lore.add(color + checkmark + " (" + required + ") " + bonus.getDescription());
        }
        
        return lore;
    }

    /**
     * Bonus d'un set
     */
    @Getter
    @Builder
    public static class SetBonus {
        private final String description;
        private final Map<StatType, Double> statBonuses;
        private final String specialAbility; // ID de l'ability spéciale (peut être null)
        private final String abilityDescription;
    }

    /**
     * Registre des sets disponibles
     */
    public static class SetRegistry {
        private static SetRegistry instance;
        private final Map<String, ItemSet> sets = new HashMap<>();

        private SetRegistry() {
            initializeSets();
        }

        public static SetRegistry getInstance() {
            if (instance == null) {
                synchronized (SetRegistry.class) {
                    if (instance == null) {
                        instance = new SetRegistry();
                    }
                }
            }
            return instance;
        }

        private void initializeSets() {
            // ==================== SET: CHASSEUR NOCTURNE ====================
            registerSet(ItemSet.builder()
                .id("night_hunter")
                .displayName("Chasseur Nocturne")
                .colorCode("§8")
                .description("Équipement des chasseurs de l'ombre")
                .pieces(Map.of(
                    ItemType.HELMET, "night_hunter_helmet",
                    ItemType.CHESTPLATE, "night_hunter_chest",
                    ItemType.LEGGINGS, "night_hunter_legs",
                    ItemType.BOOTS, "night_hunter_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+15% dégâts de nuit")
                        .statBonuses(Map.of(StatType.DAMAGE_PERCENT, 15.0))
                        .build(),
                    4, SetBonus.builder()
                        .description("Vision nocturne + 25% crit de nuit")
                        .statBonuses(Map.of(StatType.CRIT_CHANCE, 25.0))
                        .specialAbility("night_vision")
                        .abilityDescription("Vision nocturne permanente")
                        .build()
                ))
                .minZone(3)
                .build());

            // ==================== SET: GARDIEN DE FER ====================
            registerSet(ItemSet.builder()
                .id("iron_guardian")
                .displayName("Gardien de Fer")
                .colorCode("§7")
                .description("Armure des défenseurs inébranlables")
                .pieces(Map.of(
                    ItemType.HELMET, "iron_guardian_helmet",
                    ItemType.CHESTPLATE, "iron_guardian_chest",
                    ItemType.LEGGINGS, "iron_guardian_legs",
                    ItemType.BOOTS, "iron_guardian_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+20% armure")
                        .statBonuses(Map.of(StatType.ARMOR_PERCENT, 20.0))
                        .build(),
                    4, SetBonus.builder()
                        .description("-30% dégâts si immobile 2s")
                        .statBonuses(Map.of(StatType.DAMAGE_REDUCTION, 30.0))
                        .specialAbility("iron_stance")
                        .abilityDescription("Posture de fer (immobile)")
                        .build()
                ))
                .minZone(4)
                .build());

            // ==================== SET: MARCHEUR DU VENT ====================
            registerSet(ItemSet.builder()
                .id("wind_walker")
                .displayName("Marcheur du Vent")
                .colorCode("§f")
                .description("Équipement des voyageurs agiles")
                .pieces(Map.of(
                    ItemType.HELMET, "wind_walker_helmet",
                    ItemType.CHESTPLATE, "wind_walker_chest",
                    ItemType.LEGGINGS, "wind_walker_legs",
                    ItemType.BOOTS, "wind_walker_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+15% vitesse")
                        .statBonuses(Map.of(StatType.MOVEMENT_SPEED, 15.0))
                        .build(),
                    4, SetBonus.builder()
                        .description("Dash (double-tap direction)")
                        .statBonuses(Map.of(StatType.DODGE_CHANCE, 10.0))
                        .specialAbility("dash")
                        .abilityDescription("Esquive rapide tous les 3s")
                        .build()
                ))
                .minZone(5)
                .build());

            // ==================== SET: NÉCROMANCIEN ====================
            registerSet(ItemSet.builder()
                .id("necromancer")
                .displayName("Nécromancien")
                .colorCode("§5")
                .description("Reliques des maîtres de la mort")
                .pieces(Map.of(
                    ItemType.HELMET, "necromancer_helmet",
                    ItemType.CHESTPLATE, "necromancer_chest",
                    ItemType.LEGGINGS, "necromancer_legs",
                    ItemType.BOOTS, "necromancer_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("10% chance: zombie allié au kill")
                        .statBonuses(Map.of())
                        .specialAbility("raise_dead")
                        .abilityDescription("Réanimer un zombie")
                        .build(),
                    4, SetBonus.builder()
                        .description("Jusqu'à 3 zombies alliés")
                        .statBonuses(Map.of(StatType.LIFESTEAL, 5.0))
                        .specialAbility("undead_army")
                        .abilityDescription("Armée de morts-vivants")
                        .build()
                ))
                .minZone(6)
                .build());

            // ==================== SET: BERSERKER ====================
            registerSet(ItemSet.builder()
                .id("berserker")
                .displayName("Berserker")
                .colorCode("§4")
                .description("Armure des guerriers fous")
                .pieces(Map.of(
                    ItemType.HELMET, "berserker_helmet",
                    ItemType.CHESTPLATE, "berserker_chest",
                    ItemType.LEGGINGS, "berserker_legs",
                    ItemType.BOOTS, "berserker_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+2% dégâts par 1% HP manquant")
                        .statBonuses(Map.of())
                        .specialAbility("blood_rage")
                        .abilityDescription("Rage sanguinaire")
                        .build(),
                    4, SetBonus.builder()
                        .description("Immunité stagger sous 30% HP")
                        .statBonuses(Map.of(StatType.KNOCKBACK_RESISTANCE, 100.0))
                        .specialAbility("unstoppable")
                        .abilityDescription("Inarrêtable")
                        .build()
                ))
                .minZone(7)
                .build());

            // ==================== SET: PYROMANCIEN ====================
            registerSet(ItemSet.builder()
                .id("pyromancer")
                .displayName("Pyromancien")
                .colorCode("§6")
                .description("Vestiges du feu éternel")
                .pieces(Map.of(
                    ItemType.HELMET, "pyromancer_helmet",
                    ItemType.CHESTPLATE, "pyromancer_chest",
                    ItemType.LEGGINGS, "pyromancer_legs",
                    ItemType.BOOTS, "pyromancer_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+30% dégâts de feu")
                        .statBonuses(Map.of(StatType.FIRE_DAMAGE, 30.0))
                        .build(),
                    4, SetBonus.builder()
                        .description("Immunité feu + Nova de flammes")
                        .statBonuses(Map.of(StatType.FIRE_RESISTANCE, 100.0))
                        .specialAbility("flame_nova")
                        .abilityDescription("Explosion de feu tous les 10 kills")
                        .build()
                ))
                .minZone(8)
                .build());

            // ==================== SET: CRYOMANCIEN ====================
            registerSet(ItemSet.builder()
                .id("cryomancer")
                .displayName("Cryomancien")
                .colorCode("§b")
                .description("Reliques du givre éternel")
                .pieces(Map.of(
                    ItemType.HELMET, "cryomancer_helmet",
                    ItemType.CHESTPLATE, "cryomancer_chest",
                    ItemType.LEGGINGS, "cryomancer_legs",
                    ItemType.BOOTS, "cryomancer_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+30% dégâts de glace")
                        .statBonuses(Map.of(StatType.ICE_DAMAGE, 30.0))
                        .build(),
                    4, SetBonus.builder()
                        .description("Immunité froid + Aura de gel")
                        .statBonuses(Map.of(StatType.ICE_RESISTANCE, 100.0))
                        .specialAbility("frost_aura")
                        .abilityDescription("Ralentit les ennemis proches")
                        .build()
                ))
                .minZone(8)
                .build());

            // ==================== SET: IMMORTEL ====================
            registerSet(ItemSet.builder()
                .id("immortal")
                .displayName("Immortel")
                .colorCode("§d")
                .description("Armure des êtres éternels")
                .pieces(Map.of(
                    ItemType.HELMET, "immortal_helmet",
                    ItemType.CHESTPLATE, "immortal_chest",
                    ItemType.LEGGINGS, "immortal_legs",
                    ItemType.BOOTS, "immortal_boots"
                ))
                .bonuses(Map.of(
                    2, SetBonus.builder()
                        .description("+50 HP max, +2 HP/s regen")
                        .statBonuses(Map.of(
                            StatType.MAX_HEALTH, 50.0,
                            StatType.HEALTH_REGEN, 2.0
                        ))
                        .build(),
                    4, SetBonus.builder()
                        .description("Résurrection (1x par 5 min)")
                        .statBonuses(Map.of(StatType.DAMAGE_REDUCTION, 15.0))
                        .specialAbility("resurrection")
                        .abilityDescription("Revenir à 50% HP à la mort")
                        .build()
                ))
                .minZone(9)
                .build());
        }

        private void registerSet(ItemSet set) {
            sets.put(set.getId(), set);
        }

        public ItemSet getSet(String id) {
            return sets.get(id);
        }

        public Collection<ItemSet> getAllSets() {
            return Collections.unmodifiableCollection(sets.values());
        }

        public List<ItemSet> getSetsForZone(int zoneId) {
            return sets.values().stream()
                .filter(s -> s.getMinZone() <= zoneId)
                .toList();
        }
    }
}
