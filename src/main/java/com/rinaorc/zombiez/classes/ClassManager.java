package com.rinaorc.zombiez.classes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.mutations.MutationManager;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire du système de classes simplifié
 * Gère la sélection de classe et la progression de niveau
 */
@Getter
public class ClassManager {

    private final ZombieZPlugin plugin;

    // Mutations quotidiennes (système indépendant)
    private final MutationManager mutationManager;

    // Cache des données
    private final Cache<UUID, ClassData> classDataCache;

    public ClassManager(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Initialiser le gestionnaire de mutations
        this.mutationManager = new MutationManager(plugin);

        // Cache
        this.classDataCache = Caffeine.newBuilder()
            .maximumSize(250)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((uuid, data, cause) -> {
                if (data != null && ((ClassData) data).isDirty()) {
                    saveClassDataAsync((UUID) uuid, (ClassData) data);
                }
            })
            .build();

        plugin.getLogger().info("[Classes] Systeme initialise: " +
            ClassType.values().length + " classes");
    }

    // ==================== GESTION DES DONNÉES ====================

    public ClassData getClassData(Player player) {
        return getClassData(player.getUniqueId());
    }

    public ClassData getClassData(UUID uuid) {
        return classDataCache.get(uuid, this::loadOrCreateClassData);
    }

    private ClassData loadOrCreateClassData(UUID uuid) {
        return new ClassData(uuid);
    }

    public CompletableFuture<Void> saveClassDataAsync(UUID uuid, ClassData data) {
        return CompletableFuture.runAsync(() -> {
            // TODO: Sauvegarde BDD
            data.clearDirty();
        });
    }

    public void unloadPlayer(UUID uuid) {
        ClassData data = classDataCache.getIfPresent(uuid);
        if (data != null && data.isDirty()) {
            saveClassDataAsync(uuid, data);
        }
        classDataCache.invalidate(uuid);
    }

    // ==================== SÉLECTION DE CLASSE ====================

    public boolean selectClass(Player player, ClassType classType) {
        ClassData data = getClassData(player);

        // Cooldown de changement (24h)
        if (data.hasClass()) {
            long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
            long cooldown = 24 * 60 * 60 * 1000;

            if (timeSinceChange < cooldown) {
                long remainingHours = (cooldown - timeSinceChange) / (60 * 60 * 1000);
                player.sendMessage("§cChangement possible dans " + remainingHours + "h!");
                return false;
            }
        }

        data.changeClass(classType);

        // Message de confirmation
        player.sendMessage("");
        player.sendMessage("§a§l+ " + classType.getColoredName() + " §a§lselectionne!");
        player.sendMessage(classType.getDescription());
        player.sendMessage("");
        player.sendMessage("§7Difficulte: " + classType.getDifficultyDisplay());
        player.sendMessage("");

        for (String bonus : classType.getBonusDescription()) {
            player.sendMessage(bonus);
        }

        player.sendMessage("");

        return true;
    }

    // ==================== LEVEL UP ====================

    public void handleClassLevelUp(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return;

        int newLevel = data.getClassLevel().get();

        player.sendMessage("");
        player.sendMessage("§6§l+ NIVEAU " + newLevel + " +");
        player.sendMessage("§7Votre classe " + data.getSelectedClass().getColoredName() + " §7a gagne un niveau!");
        player.sendMessage("");
    }

    // ==================== CALCULS DE STATS DE BASE ====================

    /**
     * Multiplicateur de dégâts de la classe
     */
    public double getClassDamageMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;
        return data.getSelectedClass().getDamageMultiplier();
    }

    /**
     * Multiplicateur de vie de la classe
     */
    public double getClassHealthMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;
        return data.getSelectedClass().getHealthMultiplier();
    }

    /**
     * Multiplicateur de vitesse de la classe
     */
    public double getClassSpeedMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;
        return data.getSelectedClass().getSpeedMultiplier();
    }

    /**
     * Multiplicateur de critique de la classe
     */
    public double getClassCritMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;
        return data.getSelectedClass().getCritMultiplier();
    }

    /**
     * Lifesteal de base de la classe
     */
    public double getClassLifesteal(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;
        return data.getSelectedClass().getLifesteal();
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        plugin.getLogger().info("[Classes] Sauvegarde...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<UUID, ClassData> entry : classDataCache.asMap().entrySet()) {
            if (entry.getValue().isDirty()) {
                futures.add(saveClassDataAsync(entry.getKey(), entry.getValue()));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        classDataCache.invalidateAll();

        plugin.getLogger().info("[Classes] Sauvegarde terminee.");
    }
}
