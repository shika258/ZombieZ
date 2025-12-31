package com.rinaorc.zombiez.consumables;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.consumables.effects.ConsumableEffects;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Gestionnaire des consommables
 * Gère la génération, les drops et les effets des consommables
 */
public class ConsumableManager {

    private final ZombieZPlugin plugin;
    @Getter
    private final ConsumableEffects effects;

    // Configuration des drops
    private static final double BASE_DROP_CHANCE = 0.04; // 4% de base
    private static final double ZONE_BONUS_PER_ZONE = 0.002; // +0.2% par zone
    private static final double BOSS_DROP_MULTIPLIER = 3.0;
    private static final double ELITE_DROP_MULTIPLIER = 2.0;

    // Types de consommables par catégorie de zone
    private final Map<ZoneCategory, List<ConsumableType>> consumablesByZone;

    public ConsumableManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.effects = new ConsumableEffects(plugin);
        this.consumablesByZone = initializeConsumablesByZone();

        // Démarrer la tâche de mise à jour des jetpacks
        startJetpackUpdateTask();
    }

    /**
     * Initialise les consommables disponibles par catégorie de zone
     * Tous les consommables sont disponibles dès le début pour plus de variété
     */
    private Map<ZoneCategory, List<ConsumableType>> initializeConsumablesByZone() {
        Map<ZoneCategory, List<ConsumableType>> map = new EnumMap<>(ZoneCategory.class);

        // Tous les consommables disponibles dès le début
        List<ConsumableType> allConsumables = Arrays.asList(
            ConsumableType.TNT_GRENADE,
            ConsumableType.INCENDIARY_BOMB,
            ConsumableType.STICKY_CHARGE,
            ConsumableType.ACID_JAR,
            ConsumableType.JETPACK,
            ConsumableType.GRAPPLING_HOOK,
            ConsumableType.UNSTABLE_PEARL,
            ConsumableType.COBWEB_TRAP,
            ConsumableType.DECOY,
            ConsumableType.TURRET,
            ConsumableType.BANDAGE,
            ConsumableType.ANTIDOTE,
            ConsumableType.ADRENALINE_KIT
        );

        // Toutes les zones ont accès à tous les consommables
        map.put(ZoneCategory.BEGINNER, allConsumables);
        map.put(ZoneCategory.INTERMEDIATE, allConsumables);
        map.put(ZoneCategory.ADVANCED, allConsumables);
        map.put(ZoneCategory.EXPERT, allConsumables);

        return map;
    }

    /**
     * Démarre la tâche de mise à jour des jetpacks
     */
    private void startJetpackUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                effects.updateJetpacks();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Génère un consommable aléatoire pour une zone donnée
     */
    public Consumable generateConsumable(int zoneId, double luckBonus) {
        ZoneCategory category = ZoneCategory.fromZoneId(zoneId);
        List<ConsumableType> availableTypes = consumablesByZone.get(category);

        if (availableTypes == null || availableTypes.isEmpty()) {
            availableTypes = consumablesByZone.get(ZoneCategory.BEGINNER);
        }

        // Sélectionner un type aléatoire
        ConsumableType type = availableTypes.get(new Random().nextInt(availableTypes.size()));

        // Déterminer la rareté
        ConsumableRarity rarity = ConsumableRarity.rollRarity(luckBonus);

        // S'assurer que la rareté est au moins le minimum pour la zone
        ConsumableRarity minRarity = ConsumableRarity.getMinRarityForZone(zoneId);
        if (rarity.ordinal() < minRarity.ordinal()) {
            rarity = minRarity;
        }

        return new Consumable(type, rarity, zoneId);
    }

    /**
     * Génère un consommable spécifique
     */
    public Consumable generateConsumable(ConsumableType type, int zoneId, ConsumableRarity rarity) {
        return new Consumable(type, rarity, zoneId);
    }

    /**
     * Calcule la chance de drop d'un consommable pour un zombie
     */
    public double getDropChance(int zoneId, ZombieType zombieType, double luckBonus) {
        double chance = BASE_DROP_CHANCE + (zoneId * ZONE_BONUS_PER_ZONE);

        // Bonus selon le type de zombie
        if (zombieType.isBoss()) {
            chance *= BOSS_DROP_MULTIPLIER;
        } else if (zombieType.getCategory() == ZombieType.ZombieCategory.ELITE ||
                   zombieType.getCategory() == ZombieType.ZombieCategory.MINIBOSS) {
            chance *= ELITE_DROP_MULTIPLIER;
        }

        // Appliquer le bonus de luck
        chance *= (1 + luckBonus);

        // Cap à 50%
        return Math.min(0.5, chance);
    }

    /**
     * Tente de drop un consommable depuis un zombie mort
     * @return true si un consommable a été drop
     */
    public boolean tryDropConsumable(Location location, int zoneId, ZombieType zombieType, double luckBonus) {
        double chance = getDropChance(zoneId, zombieType, luckBonus);

        if (Math.random() < chance) {
            Consumable consumable = generateConsumable(zoneId, luckBonus);
            dropConsumable(location, consumable);
            return true;
        }

        return false;
    }

    /**
     * Drop un consommable au sol avec effets visuels
     */
    public void dropConsumable(Location location, Consumable consumable) {
        ItemStack item = consumable.createItemStack();

        // Créer l'entité item
        Item droppedItem = location.getWorld().dropItem(location, item);

        // Vélocité aléatoire
        Vector velocity = new Vector(
            (Math.random() - 0.5) * 0.3,
            0.3 + Math.random() * 0.2,
            (Math.random() - 0.5) * 0.3
        );
        droppedItem.setVelocity(velocity);

        // Effets visuels selon la rareté
        ConsumableRarity rarity = consumable.getRarity();

        // Toujours afficher le nom et le glow pour tous les consommables
        String displayName = consumable.getType().getDisplayName();
        ChatColor color = getConsumableRarityChatColor(rarity);
        plugin.getItemManager().applyDroppedItemEffects(droppedItem, displayName, color);

        // Effets supplémentaires selon la rareté
        switch (rarity) {
            case LEGENDARY -> {
                spawnRarityParticles(location, Particle.TOTEM_OF_UNDYING, 30);
                location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
            case EPIC -> {
                spawnRarityParticles(location, Particle.WITCH, 20);
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            }
            case RARE -> {
                spawnRarityParticles(location, Particle.ENCHANT, 15);
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            case UNCOMMON -> {
                spawnRarityParticles(location, Particle.HAPPY_VILLAGER, 10);
            }
            case COMMON -> {
                // Pas d'effet supplémentaire pour les communs
            }
        }
    }

    /**
     * Obtient la ChatColor correspondant à une ConsumableRarity
     */
    private ChatColor getConsumableRarityChatColor(ConsumableRarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatColor.WHITE;
            case UNCOMMON -> ChatColor.GREEN;
            case RARE -> ChatColor.BLUE;
            case EPIC -> ChatColor.DARK_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    /**
     * Donne un consommable à un joueur
     */
    public void giveConsumable(Player player, Consumable consumable) {
        ItemStack item = consumable.createItemStack();

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            // Inventaire plein, drop au sol
            dropConsumable(player.getLocation(), consumable);
            player.sendMessage("§e⚠ §7Inventaire plein! Le consommable a été drop au sol.");
        }

        // Message de notification
        ConsumableRarity rarity = consumable.getRarity();
        String message = switch (rarity) {
            case LEGENDARY -> "§6✦ LÉGENDAIRE! §6" + consumable.getType().getDisplayName() + " §6✦";
            case EPIC -> "§5★ Épique! §5" + consumable.getType().getDisplayName();
            case RARE -> "§9◆ Rare: §9" + consumable.getType().getDisplayName();
            default -> rarity.getColor() + consumable.getType().getDisplayName();
        };

        player.sendMessage("§a📦 §7Consommable obtenu: " + message);
    }

    /**
     * Spawn des particules pour les drops rares
     */
    private void spawnRarityParticles(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(
            particle,
            location.add(0, 0.5, 0),
            count,
            0.3, 0.3, 0.3,
            0.1
        );
    }

    /**
     * Utilise un consommable (appelé par le listener)
     * @return true si le consommable doit être retiré de l'inventaire
     */
    public boolean useConsumable(Player player, Consumable consumable, ItemStack item) {
        return effects.useConsumable(player, consumable, item);
    }

    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        effects.cleanup();
    }

    /**
     * Obtient les effets du consommable
     */
    public ConsumableEffects getConsumableEffects() {
        return effects;
    }

    /**
     * Catégories de zones pour la disponibilité des consommables
     */
    public enum ZoneCategory {
        BEGINNER(1, 10),
        INTERMEDIATE(11, 25),
        ADVANCED(26, 40),
        EXPERT(41, 50);

        private final int minZone;
        private final int maxZone;

        ZoneCategory(int minZone, int maxZone) {
            this.minZone = minZone;
            this.maxZone = maxZone;
        }

        public static ZoneCategory fromZoneId(int zoneId) {
            for (ZoneCategory cat : values()) {
                if (zoneId >= cat.minZone && zoneId <= cat.maxZone) {
                    return cat;
                }
            }
            return BEGINNER;
        }
    }
}
