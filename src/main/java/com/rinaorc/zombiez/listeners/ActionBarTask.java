package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.momentum.MomentumManager;
import com.rinaorc.zombiez.zones.Zone;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche pour afficher l'ActionBar permanent aux joueurs
 * Affiche: Zone | Combo | Streak | Points | Fever Status
 */
public class ActionBarTask extends BukkitRunnable {

    private final ZombieZPlugin plugin;

    public ActionBarTask(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Démarre la tâche (appelée depuis ZombieZPlugin)
     */
    public void start() {
        // Toutes les 20 ticks (1 seconde)
        this.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendActionBar(player);
        }
    }

    /**
     * Construit et envoie l'ActionBar à un joueur
     */
    private void sendActionBar(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        StringBuilder bar = new StringBuilder();

        // ============ ZONE ============
        Zone zone = plugin.getZoneManager().getPlayerZone(player);
        if (zone != null) {
            String zoneColor = getZoneColor(zone.getId());
            bar.append(zoneColor).append("⬢ ").append(zone.getDisplayName()).append(" ");

            // Indicateur de difficulté
            bar.append("§8[");
            int stars = zone.getDifficulty();
            for (int i = 0; i < 5; i++) {
                bar.append(i < stars ? "§e★" : "§8☆");
            }
            bar.append("§8] ");
        } else {
            bar.append("§a⬢ Spawn ");
        }

        bar.append("§8| ");

        // ============ MOMENTUM ============
        MomentumManager.MomentumData momentum = plugin.getMomentumManager().getMomentum(player);

        if (momentum != null) {
            // Combo avec timer
            int combo = momentum.getCombo();
            if (combo > 0) {
                String comboColor = getComboColor(combo);
                double timer = momentum.getComboTimer();
                bar.append(comboColor).append("⚡x").append(combo);
                if (timer > 0) {
                    bar.append(" §7(").append(String.format("%.1f", timer)).append("s)");
                }
                bar.append(" ");
            }

            // Streak
            int streak = momentum.getKillStreak();
            if (streak > 0) {
                String streakColor = getStreakColor(streak);
                bar.append(streakColor).append("🔥").append(streak).append(" ");
            }

            // Fever Mode
            if (momentum.isFeverActive()) {
                bar.append("§c§l⚡FEVER§c ");
            }
        }

        // ============ STATS RAPIDES ============
        bar.append("§8| ");

        // Points
        long points = data.getPoints().get();
        bar.append("§6⚡").append(formatCompact(points)).append(" ");

        // Level
        int level = data.getLevel().get();
        int prestige = data.getPrestige().get();
        if (prestige > 0) {
            bar.append("§d✦").append(prestige).append("§f-");
        }
        bar.append("§bLv.").append(level);

        // ============ XP BAR MINI ============
        double xpPercent = calculateXpPercent(data);
        bar.append(" §8[");
        int barLength = 10;
        int filled = (int) (xpPercent / 100.0 * barLength);
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "§b█" : "§8░");
        }
        bar.append("§8]");

        // ============ ENVOYER ============
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    /**
     * Calcule le pourcentage d'XP vers le prochain niveau
     */
    private double calculateXpPercent(PlayerData data) {
        int level = data.getLevel().get();
        long currentXp = data.getXp().get();
        long requiredXp = calculateRequiredXp(level);
        return Math.min(100, (double) currentXp / requiredXp * 100);
    }

    /**
     * Calcule l'XP requis pour le niveau suivant
     */
    private long calculateRequiredXp(int level) {
        return (long) (100 * Math.pow(level, 1.5));
    }

    /**
     * Obtient la couleur basée sur la zone
     */
    private String getZoneColor(int zoneId) {
        return switch (zoneId) {
            case 0 -> "§a";      // Spawn - Vert
            case 1, 2 -> "§a";   // Zones faciles - Vert
            case 3, 4 -> "§e";   // Zones moyennes - Jaune
            case 5, 6 -> "§6";   // Zones difficiles - Orange
            case 7, 8 -> "§c";   // Zones très difficiles - Rouge
            case 9, 10, 11 -> "§4"; // Zones finales - Rouge foncé
            default -> "§7";
        };
    }

    /**
     * Obtient la couleur du combo basée sur le nombre
     */
    private String getComboColor(int combo) {
        if (combo >= 50) return "§c§l";
        if (combo >= 25) return "§6";
        if (combo >= 10) return "§e";
        return "§7";
    }

    /**
     * Obtient la couleur du streak basée sur le nombre
     */
    private String getStreakColor(int streak) {
        if (streak >= 100) return "§c§l";
        if (streak >= 50) return "§c";
        if (streak >= 25) return "§6";
        if (streak >= 10) return "§e";
        return "§7";
    }

    /**
     * Formate un nombre de manière compacte
     */
    private String formatCompact(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1_000_000) return String.format("%.1fK", amount / 1000.0);
        if (amount < 1_000_000_000) return String.format("%.1fM", amount / 1_000_000.0);
        return String.format("%.1fB", amount / 1_000_000_000.0);
    }
}
