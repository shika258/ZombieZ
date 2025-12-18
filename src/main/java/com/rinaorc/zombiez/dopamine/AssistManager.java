package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de gestion des assists (assistances au kill)
 *
 * Effet dopamine: Récompense la coopération en donnant des XP/points aux joueurs
 * qui ont contribué à un kill, même s'ils n'ont pas porté le coup final.
 *
 * Fonctionnalités:
 * - Track les dégâts infligés par chaque joueur
 * - Répartit les récompenses proportionnellement
 * - Notifications d'assist avec statistiques
 * - Bonus pour le travail d'équipe
 *
 * @author ZombieZ Dopamine System
 */
public class AssistManager implements Listener {

    private final ZombieZPlugin plugin;

    // Dégâts trackés par mob (UUID mob -> Map<UUID joueur, dégâts>)
    private final Map<UUID, DamageTracker> damageTrackers = new ConcurrentHashMap<>();

    // Configuration
    private static final long ASSIST_TIMEOUT_MS = 10000; // 10 secondes pour que les dégâts comptent
    private static final double MIN_DAMAGE_PERCENT_FOR_ASSIST = 0.10; // Minimum 10% des dégâts pour un assist
    private static final double ASSIST_XP_RATIO = 0.5; // 50% de l'XP du kill
    private static final double ASSIST_POINTS_RATIO = 0.5; // 50% des points du kill

    public AssistManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    /**
     * Enregistre les dégâts infligés par un joueur sur un mob
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Vérifier que c'est un joueur qui inflige des dégâts
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;

        // Vérifier que c'est un mob ZombieZ
        if (!plugin.getZombieManager().isZombieZMob(mob)) return;

        // Enregistrer les dégâts
        UUID mobUuid = mob.getUniqueId();
        DamageTracker tracker = damageTrackers.computeIfAbsent(mobUuid,
            k -> new DamageTracker(mob.getMaxHealth()));

        tracker.recordDamage(player.getUniqueId(), event.getFinalDamage());
    }

    /**
     * Traite les assists quand un mob meurt
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        Player killer = mob.getKiller();

        // Vérifier que c'est un mob ZombieZ
        if (!plugin.getZombieManager().isZombieZMob(mob)) return;

        // Récupérer le tracker de dégâts
        DamageTracker tracker = damageTrackers.remove(mob.getUniqueId());
        if (tracker == null) return;

        // Calculer les contributions
        Map<UUID, Double> contributions = tracker.calculateContributions();
        if (contributions.isEmpty()) return;

        // Identifier le killer et les assistants
        UUID killerUuid = killer != null ? killer.getUniqueId() : null;

        // Calculer les récompenses de base (depuis le type de mob)
        int baseXp = getBaseXpReward(mob);
        int basePoints = getBasePointsReward(mob);

        // Traiter les assistants
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            UUID playerUuid = entry.getKey();
            double contribution = entry.getValue();

            // Ignorer le killer (il reçoit sa récompense normale)
            if (playerUuid.equals(killerUuid)) continue;

            // Ignorer les contributions trop faibles
            if (contribution < MIN_DAMAGE_PERCENT_FOR_ASSIST) continue;

            // Obtenir le joueur
            Player assistant = plugin.getServer().getPlayer(playerUuid);
            if (assistant == null || !assistant.isOnline()) continue;

            // Calculer et donner les récompenses
            rewardAssist(assistant, mob, contribution, baseXp, basePoints, killer);
        }

        // Bonus de teamwork si plusieurs joueurs ont contribué
        int assistCount = (int) contributions.entrySet().stream()
            .filter(e -> !e.getKey().equals(killerUuid))
            .filter(e -> e.getValue() >= MIN_DAMAGE_PERCENT_FOR_ASSIST)
            .count();

        if (assistCount >= 2 && killer != null) {
            giveTeamworkBonus(killer, assistCount);
        }
    }

    /**
     * Donne les récompenses d'assist à un joueur
     */
    private void rewardAssist(Player assistant, LivingEntity mob, double contribution,
                              int baseXp, int basePoints, Player killer) {
        // Calculer les récompenses proportionnelles
        int assistXp = (int) (baseXp * ASSIST_XP_RATIO * contribution);
        int assistPoints = (int) (basePoints * ASSIST_POINTS_RATIO * contribution);

        // S'assurer d'un minimum de récompenses
        assistXp = Math.max(1, assistXp);
        assistPoints = Math.max(1, assistPoints);

        // Donner les récompenses
        PlayerData data = plugin.getPlayerDataManager().getPlayer(assistant);
        long totalXp = 0;

        if (data != null) {
            data.addXp(assistXp);
            data.addPoints(assistPoints);
            data.incrementStat("total_assists");
            totalXp = data.getTotalXp().get();
        }

        // Notification
        String killerName = killer != null ? killer.getName() : "un joueur";
        int contributionPercent = (int) (contribution * 100);

        assistant.sendMessage("§e⚔ ASSIST §7(" + contributionPercent + "%) §8- §f" + killerName +
            " §7a achevé le mob §8| §a+" + assistXp + " XP §7& §e+" + assistPoints + " Points");

        // Son subtil
        assistant.playSound(assistant.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.3f);

        // Mise à jour du leaderboard (seulement si data existe)
        if (data != null && plugin.getLiveLeaderboardManager() != null) {
            plugin.getLiveLeaderboardManager().notifyStatChange(assistant,
                LiveLeaderboardManager.LeaderboardType.XP, totalXp);
        }
    }

    /**
     * Donne un bonus de teamwork au killer
     */
    private void giveTeamworkBonus(Player killer, int assistCount) {
        int bonusXp = 10 * assistCount;
        int bonusPoints = 5 * assistCount;

        PlayerData data = plugin.getPlayerDataManager().getPlayer(killer);
        if (data != null) {
            data.addXp(bonusXp);
            data.addPoints(bonusPoints);
        }

        killer.sendMessage("§d✦ TEAMWORK BONUS §7(" + assistCount + " assists) §8- §a+" + bonusXp + " XP §7& §e+" + bonusPoints + " Points");
        killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
    }

    /**
     * Obtient le type de mob de manière sécurisée
     */
    private String getMobType(LivingEntity mob) {
        if (!mob.hasMetadata("zombiez_type")) return "";
        var metadata = mob.getMetadata("zombiez_type");
        if (metadata.isEmpty()) return "";
        return metadata.get(0).asString();
    }

    /**
     * Obtient la récompense XP de base pour un mob
     */
    private int getBaseXpReward(LivingEntity mob) {
        String type = getMobType(mob);
        if (type.contains("BOSS")) return 500;
        if (type.contains("ELITE")) return 50;
        if (type.contains("SPECIAL")) return 30;
        return 10;
    }

    /**
     * Obtient la récompense points de base pour un mob
     */
    private int getBasePointsReward(LivingEntity mob) {
        String type = getMobType(mob);
        if (type.contains("BOSS")) return 1000;
        if (type.contains("ELITE")) return 100;
        if (type.contains("SPECIAL")) return 50;
        return 20;
    }

    /**
     * Démarre la tâche de nettoyage des trackers expirés
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            damageTrackers.entrySet().removeIf(entry -> {
                DamageTracker tracker = entry.getValue();
                return now - tracker.lastDamageTime > ASSIST_TIMEOUT_MS * 3;
            });
        }, 20L * 30, 20L * 30); // Toutes les 30 secondes
    }

    /**
     * Obtient le nombre total d'assists d'un joueur
     */
    public long getTotalAssists(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null ? data.getStat("total_assists") : 0;
    }

    public void shutdown() {
        damageTrackers.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tracker de dégâts pour un mob spécifique
     * Thread-safe pour gérer les accès concurrents
     */
    private static class DamageTracker {
        private final double maxHealth;
        private final Map<UUID, DamageEntry> damageByPlayer = new ConcurrentHashMap<>();
        private volatile long lastDamageTime = System.currentTimeMillis();

        DamageTracker(double maxHealth) {
            this.maxHealth = maxHealth;
        }

        void recordDamage(UUID playerUuid, double damage) {
            long now = System.currentTimeMillis();
            lastDamageTime = now;

            DamageEntry entry = damageByPlayer.computeIfAbsent(playerUuid, k -> new DamageEntry());
            entry.totalDamage += damage;
            entry.lastDamageTime = now;
        }

        /**
         * Calcule les contributions en pourcentage
         * Ne compte que les dégâts récents (dans le timeout)
         */
        Map<UUID, Double> calculateContributions() {
            long now = System.currentTimeMillis();
            Map<UUID, Double> contributions = new HashMap<>();

            double totalRecentDamage = 0;

            // Calculer le total des dégâts récents
            for (DamageEntry entry : damageByPlayer.values()) {
                if (now - entry.lastDamageTime <= ASSIST_TIMEOUT_MS) {
                    totalRecentDamage += entry.totalDamage;
                }
            }

            if (totalRecentDamage <= 0) return contributions;

            // Calculer les contributions
            for (Map.Entry<UUID, DamageEntry> entry : damageByPlayer.entrySet()) {
                DamageEntry damage = entry.getValue();
                if (now - damage.lastDamageTime <= ASSIST_TIMEOUT_MS) {
                    double contribution = damage.totalDamage / totalRecentDamage;
                    contributions.put(entry.getKey(), contribution);
                }
            }

            return contributions;
        }
    }

    /**
     * Entrée de dégâts pour un joueur
     */
    private static class DamageEntry {
        double totalDamage = 0;
        long lastDamageTime = System.currentTimeMillis();
    }
}
