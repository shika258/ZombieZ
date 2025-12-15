package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.momentum.MomentumManager;
import com.rinaorc.zombiez.zones.Zone;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Tâche pour afficher l'ActionBar permanent aux joueurs
 * Affiche les stats du joueur en temps réel
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
        // Toutes les 10 ticks (0.5 seconde) pour une mise à jour plus réactive
        this.runTaskTimer(plugin, 10L, 10L);
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

        // Mettre à jour la barre d'XP du plugin
        updateExpBar(player, data);

        // S'assurer que le health scaling est actif (10 cœurs fixes)
        if (!player.isHealthScaled()) {
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        }

        // Calculer les stats du joueur
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        StringBuilder bar = new StringBuilder();

        // ============ VIE ============
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();

        String healthColor = getHealthColor(currentHealth, maxHealth);
        bar.append(healthColor).append("❤ ").append((int) currentHealth).append("§7/§c").append((int) maxHealth);

        bar.append(" §8│ ");

        // ============ DÉFENSE ============
        double armor = playerStats.getOrDefault(StatType.ARMOR, 0.0);
        double damageReduction = playerStats.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0);

        String defenseColor = getDefenseColor(armor);
        bar.append(defenseColor).append("🛡 ").append((int) armor);

        if (damageReduction > 0) {
            bar.append(" §9(-").append((int) damageReduction).append("%)");
        }

        bar.append(" §8│ ");

        // ============ DÉGÂTS ============
        double baseDamage = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        double totalDamage = baseDamage * (1 + damagePercent / 100);

        String damageColor = getDamageColor(totalDamage);
        bar.append(damageColor).append("⚔ ").append(formatStat(totalDamage));

        // ============ STATS SECONDAIRES ============
        double critChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double critDamage = playerStats.getOrDefault(StatType.CRIT_DAMAGE, 0.0);
        double attackSpeed = playerStats.getOrDefault(StatType.ATTACK_SPEED, 0.0);
        double lifesteal = playerStats.getOrDefault(StatType.LIFESTEAL, 0.0);

        // Afficher crit si présent
        if (critChance > 0) {
            bar.append(" §8│ §6✦ ").append((int) critChance).append("%");
            if (critDamage > 0) {
                bar.append(" §8(§6+").append((int) critDamage).append("%§8)");
            }
        }

        // Afficher vitesse d'attaque si présent
        if (attackSpeed > 0) {
            bar.append(" §8│ §e⚡ +").append(String.format("%.1f", attackSpeed));
        }

        // Afficher vol de vie si présent
        if (lifesteal > 0) {
            bar.append(" §8│ §4❤ ").append((int) lifesteal).append("%");
        }

        // ============ MOMENTUM (compact) ============
        MomentumManager.MomentumData momentum = plugin.getMomentumManager().getMomentum(player);
        if (momentum != null) {
            int combo = momentum.getCombo();
            int streak = momentum.getKillStreak();

            if (combo > 0 || streak > 0 || momentum.isFeverActive()) {
                bar.append(" §8│ ");

                if (momentum.isFeverActive()) {
                    bar.append("§c§l⚡FEVER ");
                }

                if (combo > 0) {
                    String comboColor = getComboColor(combo);
                    bar.append(comboColor).append("x").append(combo).append(" ");
                }

                if (streak >= 5) {
                    String streakColor = getStreakColor(streak);
                    bar.append(streakColor).append("🔥").append(streak);
                }
            }
        }

        // ============ ENVOYER ============
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    /**
     * Formate une stat avec un décimal si nécessaire
     */
    private String formatStat(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    /**
     * Obtient la couleur de la vie basée sur le pourcentage
     */
    private String getHealthColor(double current, double max) {
        double percent = current / max;
        if (percent <= 0.25) return "§4§l";  // Rouge foncé clignotant
        if (percent <= 0.5) return "§c";     // Rouge
        if (percent <= 0.75) return "§e";    // Jaune
        return "§a";                          // Vert
    }

    /**
     * Obtient la couleur de la défense basée sur la valeur
     */
    private String getDefenseColor(double defense) {
        if (defense >= 100) return "§b§l";   // Cyan brillant
        if (defense >= 60) return "§9";      // Bleu
        if (defense >= 30) return "§3";      // Cyan foncé
        if (defense >= 10) return "§7";      // Gris
        return "§8";                          // Gris foncé
    }

    /**
     * Obtient la couleur des dégâts basée sur la valeur
     */
    private String getDamageColor(double damage) {
        if (damage >= 100) return "§c§l";    // Rouge brillant
        if (damage >= 50) return "§c";       // Rouge
        if (damage >= 25) return "§6";       // Orange
        if (damage >= 10) return "§e";       // Jaune
        return "§f";                          // Blanc
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
     * Met à jour la barre d'XP native de Minecraft avec le niveau et la progression du plugin
     * Le niveau affiché correspond au niveau du plugin ZombieZ
     * La barre d'XP montre la progression vers le prochain niveau
     */
    private void updateExpBar(Player player, PlayerData data) {
        // Définir le niveau affiché (niveau du plugin)
        int pluginLevel = data.getLevel().get();
        if (player.getLevel() != pluginLevel) {
            player.setLevel(pluginLevel);
        }

        // Définir la progression vers le prochain niveau (0.0 à 1.0)
        float progress = (float) (data.getLevelProgress() / 100.0);
        // Clamp entre 0 et 0.99999 (1.0 cause parfois des bugs visuels)
        progress = Math.max(0f, Math.min(0.99999f, progress));

        // Seulement mettre à jour si changement significatif (évite spam réseau)
        if (Math.abs(player.getExp() - progress) > 0.01f) {
            player.setExp(progress);
        }
    }
}
