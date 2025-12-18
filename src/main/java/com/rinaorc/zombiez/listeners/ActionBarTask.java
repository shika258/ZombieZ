package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.StatType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Tâche pour afficher l'ActionBar permanent aux joueurs
 * Affiche: HP / Défense / Dégâts / IS (Item Score)
 *
 * Format épuré et lisible pour une meilleure expérience de jeu
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
        // Toutes les 10 ticks (0.5 seconde) pour une mise à jour réactive
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
     * Format: ❤ HP/Max │ 🛡 Défense │ ⚔ Dégâts │ IS: Score
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

        // ============ DÉFENSE TOTALE ============
        double armor = playerStats.getOrDefault(StatType.ARMOR, 0.0);
        double armorToughness = playerStats.getOrDefault(StatType.ARMOR_TOUGHNESS, 0.0);
        double damageReduction = playerStats.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0);

        // Calcul de la défense totale effective
        // Formule: armor + (toughness * 2) + (reduction en équivalent armor)
        double totalDefense = armor + (armorToughness * 2) + (damageReduction * 0.5);

        String defenseColor = getDefenseColor(totalDefense);
        bar.append(defenseColor).append("🛡 ").append((int) totalDefense);

        bar.append(" §8│ ");

        // ============ DÉGÂTS TOTAUX ============
        double baseDamage = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        double totalDamage = baseDamage * (1 + damagePercent / 100);

        String damageColor = getDamageColor(totalDamage);
        bar.append(damageColor).append("⚔ ").append(formatStat(totalDamage));

        bar.append(" §8│ ");

        // ============ ITEM SCORE ============
        int totalItemScore = plugin.getItemManager().calculateTotalItemScore(player);
        String isColor = getItemScoreColor(totalItemScore);
        bar.append(isColor).append("IS: ").append(formatNumber(totalItemScore));

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
        if (percent <= 0.25) return "§4§l";  // Rouge foncé (critique)
        if (percent <= 0.5) return "§c";     // Rouge
        if (percent <= 0.75) return "§e";    // Jaune
        return "§a";                          // Vert
    }

    /**
     * Obtient la couleur de la défense basée sur la valeur totale
     */
    private String getDefenseColor(double defense) {
        if (defense >= 150) return "§b§l";   // Cyan brillant (très haute)
        if (defense >= 100) return "§9";     // Bleu
        if (defense >= 50) return "§3";      // Cyan foncé
        if (defense >= 20) return "§7";      // Gris
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
     * Obtient la couleur de l'Item Score basée sur la valeur totale
     */
    private String getItemScoreColor(int score) {
        if (score >= 50000) return "§c§l";   // Rouge brillant (end-game)
        if (score >= 30000) return "§d";     // Violet (late-game)
        if (score >= 15000) return "§6";     // Orange (mid-late)
        if (score >= 7000) return "§e";      // Jaune (mid-game)
        if (score >= 3000) return "§a";      // Vert (early-mid)
        if (score >= 1000) return "§f";      // Blanc (early)
        return "§7";                          // Gris (débutant)
    }

    /**
     * Formate un nombre avec suffixes k, M
     * Ex: 10500 -> "10.5k", 1500000 -> "1.5M"
     */
    private String formatNumber(int value) {
        if (value >= 1_000_000) {
            double millions = value / 1_000_000.0;
            if (millions == (int) millions) {
                return (int) millions + "M";
            }
            return String.format("%.1fM", millions);
        }
        if (value >= 1_000) {
            double thousands = value / 1_000.0;
            if (thousands == (int) thousands) {
                return (int) thousands + "k";
            }
            return String.format("%.1fk", thousands);
        }
        return String.valueOf(value);
    }

    /**
     * Met à jour la barre d'XP native de Minecraft avec le niveau et la progression du plugin
     */
    private void updateExpBar(Player player, PlayerData data) {
        int pluginLevel = data.getLevel().get();
        if (player.getLevel() != pluginLevel) {
            player.setLevel(pluginLevel);
        }

        float progress = (float) (data.getLevelProgress() / 100.0);
        progress = Math.max(0f, Math.min(0.99999f, progress));

        if (Math.abs(player.getExp() - progress) > 0.01f) {
            player.setExp(progress);
        }
    }
}
