package com.rinaorc.zombiez.momentum;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de momentum (Streaks, Combos, Fever)
 */
public class MomentumManager {

    private final ZombieZPlugin plugin;
    
    // Données par joueur
    private final Map<UUID, MomentumData> playerMomentum = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int COMBO_TIMEOUT = 5000; // 5 secondes
    private static final int STREAK_TIMEOUT = 30000; // 30 secondes sans kill
    private static final int FEVER_THRESHOLD = 50; // Kills pour activer Fever
    public static final int FEVER_DURATION = 30000; // 30 secondes de Fever (public pour accès externe)

    public MomentumManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startDecayTask();
    }

    /**
     * Enregistre un kill
     */
    public void registerKill(Player player) {
        MomentumData data = getOrCreate(player);
        long now = System.currentTimeMillis();

        // Vérifier le combo timeout
        if (now - data.lastKillTime > COMBO_TIMEOUT) {
            data.combo = 0;
        }

        // Vérifier le streak timeout
        if (now - data.lastKillTime > STREAK_TIMEOUT) {
            data.streak = 0;
        }

        // Incrémenter
        data.combo++;
        data.streak++;
        data.totalKills++;
        data.lastKillTime = now;

        // Vérifier Fever
        if (!data.inFever && data.streak >= FEVER_THRESHOLD) {
            activateFever(player, data);
        }

        // Notifications selon les milestones
        checkMilestones(player, data);

        // Notifier le système de Boss Bar Dynamique
        if (plugin.getDynamicBossBarManager() != null) {
            plugin.getDynamicBossBarManager().notifyKill(player);
        }
    }

    /**
     * Active le mode Fever
     */
    private void activateFever(Player player, MomentumData data) {
        data.inFever = true;
        data.feverStartTime = System.currentTimeMillis();
        
        MessageUtils.sendTitle(player, "", "§c§l🔥 FEVER MODE! §eBonus x2 pendant 30s!", 10, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        
        // Annoncer aux joueurs proches
        for (Player nearby : player.getWorld().getNearbyEntities(player.getLocation(), 50, 50, 50)
                .stream().filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
            if (nearby != player) {
                nearby.sendMessage("§c🔥 " + player.getName() + " §eest en FEVER MODE!");
            }
        }
    }

    /**
     * Vérifie les milestones de streak
     */
    private void checkMilestones(Player player, MomentumData data) {
        // Combo milestones
        if (data.combo == 10) {
            MessageUtils.sendActionBar(player, "§e⚡ COMBO x10!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        } else if (data.combo == 25) {
            MessageUtils.sendActionBar(player, "§6⚡ COMBO x25!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else if (data.combo == 50) {
            MessageUtils.sendActionBar(player, "§c⚡ MEGA COMBO x50!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1f);
        }
        
        // Streak milestones
        if (data.streak == 25) {
            player.sendMessage("§6🔥 Streak de 25 kills! §7Encore 25 pour le FEVER!");
        } else if (data.streak == 100) {
            plugin.getServer().broadcastMessage("§c§l🔥 " + player.getName() + " §ea atteint un streak de §c§l100 KILLS!");
        }
    }

    /**
     * Obtient le multiplicateur de dégâts
     */
    public double getDamageMultiplier(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        if (data == null) return 1.0;
        
        double mult = 1.0;
        
        // Bonus de combo (+1% par combo, max 50%)
        mult += Math.min(0.5, data.combo * 0.01);
        
        // Bonus de streak (+0.5% par streak, max 25%)
        mult += Math.min(0.25, data.streak * 0.005);
        
        // Bonus Fever (x2)
        if (data.inFever && !isFeverExpired(data)) {
            mult *= 2.0;
        }
        
        return mult;
    }

    /**
     * Obtient le multiplicateur de vitesse
     */
    public double getSpeedMultiplier(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        if (data == null) return 1.0;
        
        // Bonus de combo (+2% par combo, max 30%)
        return 1.0 + Math.min(0.3, data.combo * 0.02);
    }

    /**
     * Vérifie si Fever est expiré
     */
    private boolean isFeverExpired(MomentumData data) {
        return System.currentTimeMillis() - data.feverStartTime > FEVER_DURATION;
    }

    /**
     * Enregistre une mort (reset momentum)
     */
    public void registerDeath(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        if (data != null) {
            data.combo = 0;
            data.streak = 0;
            data.inFever = false;
        }
    }

    /**
     * Obtient les données de momentum
     */
    public MomentumData getOrCreate(Player player) {
        return playerMomentum.computeIfAbsent(player.getUniqueId(), k -> new MomentumData());
    }

    /**
     * Obtient le streak actuel
     */
    public int getStreak(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        return data != null ? data.streak : 0;
    }

    /**
     * Obtient le combo actuel
     */
    public int getCombo(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        return data != null ? data.combo : 0;
    }

    /**
     * Vérifie si en Fever
     */
    public boolean isInFever(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        return data != null && data.inFever && !isFeverExpired(data);
    }

    /**
     * Obtient le temps restant de Fever en millisecondes
     */
    public long getFeverTimeRemaining(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        if (data == null || !data.inFever) return 0;

        long elapsed = System.currentTimeMillis() - data.feverStartTime;
        long remaining = FEVER_DURATION - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Obtient la progression du Fever (1.0 = début, 0.0 = fin)
     */
    public double getFeverProgress(Player player) {
        MomentumData data = playerMomentum.get(player.getUniqueId());
        if (data == null || !data.inFever) return 0;

        long elapsed = System.currentTimeMillis() - data.feverStartTime;
        double progress = 1.0 - ((double) elapsed / FEVER_DURATION);
        return Math.max(0, Math.min(1.0, progress));
    }

    /**
     * Tâche de décroissance
     */
    private void startDecayTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            
            for (Map.Entry<UUID, MomentumData> entry : playerMomentum.entrySet()) {
                MomentumData data = entry.getValue();
                
                // Decay combo
                if (now - data.lastKillTime > COMBO_TIMEOUT && data.combo > 0) {
                    data.combo = 0;
                }
                
                // Decay streak
                if (now - data.lastKillTime > STREAK_TIMEOUT && data.streak > 0) {
                    data.streak = 0;
                }
                
                // End Fever
                if (data.inFever && isFeverExpired(data)) {
                    data.inFever = false;
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null) {
                        MessageUtils.sendTitle(player, "", "§7§l🔥 FEVER TERMINÉ! §8Streak: " + data.streak + " kills", 5, 30, 10);
                        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.0f);
                    }
                }
            }
        }, 20L, 20L);
    }

    /**
     * Données de momentum
     */
    @Getter
    public static class MomentumData {
        private int combo = 0;
        private int streak = 0;
        private int totalKills = 0;
        private long lastKillTime = 0;
        private boolean inFever = false;
        private long feverStartTime = 0;
        
        // Alias pour compatibilité
        public int getKillStreak() { return streak; }
        public int getCurrentCombo() { return combo; }
        public int getTotalKillsSession() { return totalKills; }
        public boolean isFeverActive() { return inFever; }
        
        public double getComboTimer() {
            if (combo == 0) return 0;
            long elapsed = System.currentTimeMillis() - lastKillTime;
            return Math.max(0, (COMBO_TIMEOUT - elapsed) / 1000.0);
        }
    }
    
    /**
     * Appelé quand un joueur se déconnecte
     */
    public void onPlayerQuit(Player player) {
        playerMomentum.remove(player.getUniqueId());
    }
    
    /**
     * Obtient les données de momentum d'un joueur
     */
    public MomentumData getMomentum(Player player) {
        return playerMomentum.get(player.getUniqueId());
    }
    
    /**
     * Obtient les données de momentum d'un joueur par UUID
     */
    public MomentumData getMomentum(UUID uuid) {
        return playerMomentum.get(uuid);
    }
}
