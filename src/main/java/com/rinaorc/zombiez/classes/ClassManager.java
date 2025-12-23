package com.rinaorc.zombiez.classes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.mutations.MutationManager;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
        ClassData data = new ClassData(uuid);

        // Charger depuis la BDD de manière synchrone (appelé depuis le cache)
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().table("class_data") + " WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Charger la classe
                        String classType = rs.getString("class_type");
                        if (classType != null && !classType.isEmpty()) {
                            try {
                                data.setSelectedClass(ClassType.valueOf(classType));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Classe invalide pour " + uuid + ": " + classType);
                            }
                        }

                        // Charger le niveau et XP
                        data.getClassLevel().set(rs.getInt("class_level"));
                        data.getClassXp().set(rs.getLong("class_xp"));

                        // Charger la branche
                        String branchId = rs.getString("selected_branch");
                        if (branchId != null && !branchId.isEmpty()) {
                            data.setSelectedBranchId(branchId);
                        }

                        // Charger les talents (format: "TIER1:talentId,TIER2:talentId,...")
                        String talentsStr = rs.getString("selected_talents");
                        if (talentsStr != null && !talentsStr.isEmpty()) {
                            for (String entry : talentsStr.split(",")) {
                                String[] parts = entry.split(":");
                                if (parts.length == 2) {
                                    try {
                                        TalentTier tier = TalentTier.valueOf(parts[0]);
                                        data.getSelectedTalentsInternal().put(tier, parts[1]);
                                    } catch (IllegalArgumentException ignored) {}
                                }
                            }
                        }

                        // Charger les statistiques
                        data.getClassKills().set(rs.getLong("class_kills"));
                        data.getClassDeaths().set(rs.getLong("class_deaths"));
                        data.getDamageDealt().set(rs.getLong("damage_dealt"));
                        data.getDamageReceived().set(rs.getLong("damage_received"));
                        data.setTotalPlaytimeAsClass(rs.getLong("total_playtime_as_class"));
                        data.setLastClassChange(rs.getLong("last_class_change"));
                        data.setLastBranchChange(rs.getLong("last_branch_change"));
                        data.setTalentMessagesEnabled(rs.getBoolean("talent_messages_enabled"));

                        data.clearDirty();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur chargement ClassData pour " + uuid, e);
        }

        return data;
    }

    public CompletableFuture<Void> saveClassDataAsync(UUID uuid, ClassData data) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = """
                    INSERT INTO %s (uuid, class_type, class_level, class_xp, selected_branch, selected_talents,
                        class_kills, class_deaths, damage_dealt, damage_received, total_playtime_as_class,
                        last_class_change, last_branch_change, talent_messages_enabled)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        class_type = excluded.class_type,
                        class_level = excluded.class_level,
                        class_xp = excluded.class_xp,
                        selected_branch = excluded.selected_branch,
                        selected_talents = excluded.selected_talents,
                        class_kills = excluded.class_kills,
                        class_deaths = excluded.class_deaths,
                        damage_dealt = excluded.damage_dealt,
                        damage_received = excluded.damage_received,
                        total_playtime_as_class = excluded.total_playtime_as_class,
                        last_class_change = excluded.last_class_change,
                        last_branch_change = excluded.last_branch_change,
                        talent_messages_enabled = excluded.talent_messages_enabled
                    """.formatted(plugin.getDatabaseManager().table("class_data"));

                // Pour MySQL, utiliser une syntaxe différente
                if (plugin.getDatabaseManager().getDatabaseType() ==
                    com.rinaorc.zombiez.data.DatabaseManager.DatabaseType.MYSQL) {
                    sql = """
                        INSERT INTO %s (uuid, class_type, class_level, class_xp, selected_branch, selected_talents,
                            class_kills, class_deaths, damage_dealt, damage_received, total_playtime_as_class,
                            last_class_change, last_branch_change, talent_messages_enabled)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            class_type = VALUES(class_type),
                            class_level = VALUES(class_level),
                            class_xp = VALUES(class_xp),
                            selected_branch = VALUES(selected_branch),
                            selected_talents = VALUES(selected_talents),
                            class_kills = VALUES(class_kills),
                            class_deaths = VALUES(class_deaths),
                            damage_dealt = VALUES(damage_dealt),
                            damage_received = VALUES(damage_received),
                            total_playtime_as_class = VALUES(total_playtime_as_class),
                            last_class_change = VALUES(last_class_change),
                            last_branch_change = VALUES(last_branch_change),
                            talent_messages_enabled = VALUES(talent_messages_enabled)
                        """.formatted(plugin.getDatabaseManager().table("class_data"));
                }

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, data.getSelectedClass() != null ? data.getSelectedClass().name() : null);
                    stmt.setInt(3, data.getClassLevel().get());
                    stmt.setLong(4, data.getClassXp().get());
                    stmt.setString(5, data.getSelectedBranchId());

                    // Sérialiser les talents
                    StringBuilder talentsStr = new StringBuilder();
                    for (Map.Entry<TalentTier, String> entry : data.getAllSelectedTalents().entrySet()) {
                        if (talentsStr.length() > 0) talentsStr.append(",");
                        talentsStr.append(entry.getKey().name()).append(":").append(entry.getValue());
                    }
                    stmt.setString(6, talentsStr.toString());

                    stmt.setLong(7, data.getClassKills().get());
                    stmt.setLong(8, data.getClassDeaths().get());
                    stmt.setLong(9, data.getDamageDealt().get());
                    stmt.setLong(10, data.getDamageReceived().get());
                    stmt.setLong(11, data.getTotalPlaytimeAsClass());
                    stmt.setLong(12, data.getLastClassChange());
                    stmt.setLong(13, data.getLastBranchChange());
                    stmt.setBoolean(14, data.isTalentMessagesEnabled());

                    stmt.executeUpdate();
                }

                data.clearDirty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Erreur sauvegarde ClassData pour " + uuid, e);
            }
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

            // Nettoyer les effets visuels de l'ancienne classe (ArmorStands, etc.)
            if (plugin.getTalentListener() != null) {
                plugin.getTalentListener().cleanupPlayer(player.getUniqueId());
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
