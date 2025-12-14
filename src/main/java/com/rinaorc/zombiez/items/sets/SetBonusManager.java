package com.rinaorc.zombiez.items.sets;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les bonus de sets d'équipement
 * Applique les effets passifs et actifs des sets
 */
public class SetBonusManager {

    private final ZombieZPlugin plugin;
    private final ItemSet.SetRegistry setRegistry;
    
    // Cache des sets équipés par joueur
    private final Map<UUID, Map<String, Integer>> playerSets; // UUID -> SetID -> PieceCount
    
    // Cooldowns des abilities spéciales
    private final Map<UUID, Map<String, Long>> abilityCooldowns; // UUID -> AbilityID -> Timestamp
    
    // Compteurs pour les abilities (kills, hits, etc.)
    private final Map<UUID, Map<String, Integer>> abilityCounters;

    public SetBonusManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.setRegistry = ItemSet.SetRegistry.getInstance();
        this.playerSets = new ConcurrentHashMap<>();
        this.abilityCooldowns = new ConcurrentHashMap<>();
        this.abilityCounters = new ConcurrentHashMap<>();
        
        startPassiveEffectsTask();
    }

    /**
     * Met à jour les sets équipés d'un joueur
     */
    public void updatePlayerSets(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> sets = new HashMap<>();
        
        // Compter les pièces de chaque set
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && ZombieZItem.isZombieZItem(item)) {
                ZombieZItem zItem = plugin.getItemManager().getItem(item);
                if (zItem != null && zItem.getSetId() != null) {
                    sets.merge(zItem.getSetId(), 1, Integer::sum);
                }
            }
        }
        
        playerSets.put(playerId, sets);
        
        // Appliquer les effets passifs immédiatement
        applyPassiveEffects(player);
    }

    /**
     * Obtient les bonus de stats des sets pour un joueur
     */
    public Map<StatType, Double> getSetBonusStats(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> sets = playerSets.getOrDefault(playerId, Map.of());
        Map<StatType, Double> totalStats = new EnumMap<>(StatType.class);
        
        for (var entry : sets.entrySet()) {
            ItemSet set = setRegistry.getSet(entry.getKey());
            if (set != null) {
                Map<StatType, Double> setStats = set.calculateBonusStats(entry.getValue());
                for (var stat : setStats.entrySet()) {
                    totalStats.merge(stat.getKey(), stat.getValue(), Double::sum);
                }
            }
        }
        
        return totalStats;
    }

    /**
     * Vérifie si un joueur a une ability de set active
     */
    public boolean hasSetAbility(Player player, String abilityId) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> sets = playerSets.getOrDefault(playerId, Map.of());
        
        for (var entry : sets.entrySet()) {
            ItemSet set = setRegistry.getSet(entry.getKey());
            if (set != null) {
                for (ItemSet.SetBonus bonus : set.getActiveBonuses(entry.getValue())) {
                    if (abilityId.equals(bonus.getSpecialAbility())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Vérifie si une ability est en cooldown
     */
    public boolean isAbilityOnCooldown(Player player, String abilityId) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = abilityCooldowns.getOrDefault(playerId, Map.of());
        Long cooldownEnd = cooldowns.get(abilityId);
        
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Met une ability en cooldown
     */
    public void setAbilityCooldown(Player player, String abilityId, long durationMs) {
        UUID playerId = player.getUniqueId();
        abilityCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(abilityId, System.currentTimeMillis() + durationMs);
    }

    /**
     * Incrémente un compteur d'ability et retourne la nouvelle valeur
     */
    public int incrementAbilityCounter(Player player, String counterId) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> counters = abilityCounters.computeIfAbsent(playerId, 
            k -> new ConcurrentHashMap<>());
        return counters.merge(counterId, 1, Integer::sum);
    }

    /**
     * Réinitialise un compteur d'ability
     */
    public void resetAbilityCounter(Player player, String counterId) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> counters = abilityCounters.get(playerId);
        if (counters != null) {
            counters.remove(counterId);
        }
    }

    /**
     * Applique les effets passifs des sets
     */
    private void applyPassiveEffects(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> sets = playerSets.getOrDefault(playerId, Map.of());
        
        for (var entry : sets.entrySet()) {
            ItemSet set = setRegistry.getSet(entry.getKey());
            if (set == null) continue;
            
            for (ItemSet.SetBonus bonus : set.getActiveBonuses(entry.getValue())) {
                String ability = bonus.getSpecialAbility();
                if (ability == null) continue;
                
                switch (ability) {
                    case "night_vision" -> {
                        // Vision nocturne permanente
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.NIGHT_VISION, 400, 0, true, false, false
                        ));
                    }
                    case "frost_aura" -> {
                        // Ralentir les ennemis proches (géré dans le task)
                    }
                    // Les autres abilities sont gérées événementiellement
                }
            }
        }
    }

    /**
     * Traite un kill pour les abilities basées sur les kills
     */
    public void onPlayerKill(Player player, org.bukkit.entity.LivingEntity victim) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> sets = playerSets.getOrDefault(playerId, Map.of());
        
        for (var entry : sets.entrySet()) {
            ItemSet set = setRegistry.getSet(entry.getKey());
            if (set == null) continue;
            
            for (ItemSet.SetBonus bonus : set.getActiveBonuses(entry.getValue())) {
                String ability = bonus.getSpecialAbility();
                if (ability == null) continue;
                
                switch (ability) {
                    case "raise_dead" -> {
                        // 10% chance de réanimer un zombie allié
                        if (Math.random() < 0.10) {
                            spawnAllyZombie(player, victim.getLocation());
                        }
                    }
                    case "flame_nova" -> {
                        // Explosion de feu tous les 10 kills
                        int count = incrementAbilityCounter(player, "flame_nova_kills");
                        if (count >= 10) {
                            triggerFlameNova(player);
                            resetAbilityCounter(player, "flame_nova_kills");
                        }
                    }
                }
            }
        }
    }

    /**
     * Traite une mort pour les abilities de résurrection
     */
    public boolean onPlayerDeath(Player player) {
        if (hasSetAbility(player, "resurrection") && !isAbilityOnCooldown(player, "resurrection")) {
            // Résurrection!
            setAbilityCooldown(player, "resurrection", 5 * 60 * 1000); // 5 min cooldown
            
            // Le joueur revient à 50% HP (géré dans DeathListener)
            return true; // Annuler la mort
        }
        return false;
    }

    /**
     * Spawn un zombie allié (set Nécromancien)
     */
    private void spawnAllyZombie(Player player, org.bukkit.Location location) {
        // TODO: Implémenter les zombies alliés avec un système de marquage
        player.sendMessage("§5✦ Un zombie se relève pour vous servir!");
        
        org.bukkit.entity.Zombie zombie = location.getWorld().spawn(location, org.bukkit.entity.Zombie.class);
        zombie.setCustomName("§5Serviteur de " + player.getName());
        zombie.setCustomNameVisible(true);
        
        // Marquer comme allié (via metadata ou scoreboard tag)
        zombie.addScoreboardTag("zombiez_ally_" + player.getUniqueId());
        
        // Auto-despawn après 60 secondes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (zombie.isValid()) {
                    zombie.remove();
                }
            }
        }.runTaskLater(plugin, 20L * 60);
    }

    /**
     * Déclenche une nova de flammes (set Pyromancien)
     */
    private void triggerFlameNova(Player player) {
        player.sendMessage("§6✦ NOVA DE FLAMMES!");
        
        org.bukkit.Location loc = player.getLocation();
        
        // Effet visuel
        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 100, 3, 1, 3, 0.1);
        player.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        
        // Dégâts aux entités proches
        loc.getWorld().getNearbyEntities(loc, 5, 3, 5).stream()
            .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
            .filter(e -> e != player)
            .filter(e -> !e.getScoreboardTags().contains("zombiez_ally_" + player.getUniqueId()))
            .forEach(e -> {
                org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) e;
                living.damage(10, player);
                living.setFireTicks(100);
            });
    }

    /**
     * Task pour les effets passifs continus
     */
    private void startPassiveEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    Map<String, Integer> sets = playerSets.getOrDefault(playerId, Map.of());
                    
                    for (var entry : sets.entrySet()) {
                        ItemSet set = setRegistry.getSet(entry.getKey());
                        if (set == null) continue;
                        
                        for (ItemSet.SetBonus bonus : set.getActiveBonuses(entry.getValue())) {
                            String ability = bonus.getSpecialAbility();
                            if (ability == null) continue;
                            
                            switch (ability) {
                                case "night_vision" -> {
                                    // Renouveler la vision nocturne
                                    player.addPotionEffect(new PotionEffect(
                                        PotionEffectType.NIGHT_VISION, 400, 0, true, false, false
                                    ));
                                }
                                case "frost_aura" -> {
                                    // Ralentir les ennemis proches
                                    player.getWorld().getNearbyEntities(player.getLocation(), 4, 2, 4).stream()
                                        .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                                        .filter(e -> e != player)
                                        .forEach(e -> {
                                            ((org.bukkit.entity.LivingEntity) e).addPotionEffect(
                                                new PotionEffect(PotionEffectType.SLOWNESS, 40, 0)
                                            );
                                        });
                                }
                                case "iron_stance" -> {
                                    // Vérifier si le joueur est immobile depuis 2s
                                    // TODO: Implémenter le tracking de mouvement
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes
    }

    /**
     * Nettoie les données d'un joueur
     */
    public void cleanup(UUID playerId) {
        playerSets.remove(playerId);
        abilityCooldowns.remove(playerId);
        abilityCounters.remove(playerId);
    }

    /**
     * Obtient les sets équipés d'un joueur pour l'affichage
     */
    public Map<String, Integer> getEquippedSets(Player player) {
        return playerSets.getOrDefault(player.getUniqueId(), Map.of());
    }
}
