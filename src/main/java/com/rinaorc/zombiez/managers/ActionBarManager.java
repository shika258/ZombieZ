package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.types.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Gestionnaire centralisé des ActionBars.
 *
 * Gère la priorité entre:
 * - ActionBar par défaut (stats du joueur)
 * - ActionBar de classe (Ombre, Poison, etc.)
 *
 * Logique:
 * - En combat (ou moins de 5s depuis le dernier combat) -> ActionBar de classe
 * - Hors combat depuis 5s+ -> ActionBar par défaut
 */
public class ActionBarManager {

    private final ZombieZPlugin plugin;

    // Temps de combat par joueur (dernière action de combat)
    private final Map<UUID, Long> lastCombatTime = new ConcurrentHashMap<>();

    // Fournisseurs d'ActionBar de classe par joueur
    private final Map<UUID, Function<Player, String>> classActionBarProviders = new ConcurrentHashMap<>();

    // Durée avant de revenir à l'ActionBar par défaut (5 secondes)
    private static final long COMBAT_TIMEOUT_MS = 5000;

    // Intervalle de mise à jour (4 ticks = 200ms pour fluidité)
    private static final long UPDATE_INTERVAL_TICKS = 4L;

    public ActionBarManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    /**
     * Démarre la tâche de mise à jour des ActionBars
     */
    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updateActionBar(player);
                }
            }
        }.runTaskTimer(plugin, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
    }

    /**
     * Met à jour l'ActionBar d'un joueur
     */
    private void updateActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        String actionBarContent;

        // Vérifier si le joueur est en combat ou a été récemment en combat
        if (isInCombat(uuid)) {
            // Utiliser l'ActionBar de classe si disponible
            Function<Player, String> provider = classActionBarProviders.get(uuid);
            if (provider != null) {
                actionBarContent = provider.apply(player);
            } else {
                // Pas de provider de classe, utiliser l'ActionBar par défaut
                actionBarContent = buildDefaultActionBar(player);
            }
        } else {
            // Hors combat, utiliser l'ActionBar par défaut
            actionBarContent = buildDefaultActionBar(player);
        }

        // Envoyer l'ActionBar
        player.sendActionBar(Component.text(actionBarContent));
    }

    /**
     * Vérifie si un joueur est en combat (ou récemment en combat)
     */
    public boolean isInCombat(UUID playerUuid) {
        Long lastCombat = lastCombatTime.get(playerUuid);
        if (lastCombat == null) return false;
        return System.currentTimeMillis() - lastCombat < COMBAT_TIMEOUT_MS;
    }

    /**
     * Marque un joueur comme étant en combat
     * Appelé par CombatListener quand le joueur attaque ou reçoit des dégâts
     */
    public void markInCombat(UUID playerUuid) {
        lastCombatTime.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * Enregistre un fournisseur d'ActionBar de classe pour un joueur
     * Le provider sera appelé quand le joueur est en combat
     */
    public void registerClassActionBar(UUID playerUuid, Function<Player, String> provider) {
        classActionBarProviders.put(playerUuid, provider);
    }

    /**
     * Retire le fournisseur d'ActionBar de classe d'un joueur
     */
    public void unregisterClassActionBar(UUID playerUuid) {
        classActionBarProviders.remove(playerUuid);
    }

    /**
     * Nettoie les données d'un joueur (déconnexion)
     */
    public void cleanupPlayer(UUID playerUuid) {
        lastCombatTime.remove(playerUuid);
        classActionBarProviders.remove(playerUuid);
    }

    /**
     * Construit l'ActionBar par défaut (stats du joueur)
     * Format: ❤ HP/Max │ 🛡 Défense │ ⚔ Dégâts │ Boussole
     */
    private String buildDefaultActionBar(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return "";

        // Mettre à jour la barre d'XP
        updateExpBar(player, data);

        // Health scaling
        if (!player.isHealthScaled()) {
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        }

        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);
        StringBuilder bar = new StringBuilder();

        // VIE
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        String healthColor = getHealthColor(currentHealth, maxHealth);
        bar.append(healthColor).append("❤ ").append((int) currentHealth).append("§7/§c").append((int) maxHealth);

        bar.append(" §8│ ");

        // DÉFENSE
        double armor = playerStats.getOrDefault(StatType.ARMOR, 0.0);
        double armorToughness = playerStats.getOrDefault(StatType.ARMOR_TOUGHNESS, 0.0);
        double damageReduction = playerStats.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0);
        double totalDefense = armor + (armorToughness * 2) + (damageReduction * 0.5);
        String defenseColor = getDefenseColor(totalDefense);
        bar.append(defenseColor).append("🛡 ").append((int) totalDefense);

        bar.append(" §8│ ");

        // DÉGÂTS
        double baseDamage = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        double totalDamage = baseDamage * (1 + damagePercent / 100);
        String damageColor = getDamageColor(totalDamage);
        bar.append(damageColor).append("⚔ ").append(formatStat(totalDamage));

        bar.append(" §8│ ");

        // BOUSSOLE
        bar.append(buildCompass(player));

        return bar.toString();
    }

    /**
     * Construit la boussole visuelle
     */
    private String buildCompass(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;

        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int directionIndex = (int) Math.round(yaw / 45) % 8;
        String mainDir = directions[directionIndex];

        if (mainDir.equals("N")) {
            return "§b§l⬆ N";
        } else if (mainDir.contains("N")) {
            return "§b↗ " + mainDir;
        } else if (mainDir.equals("S")) {
            return "§7⬇ S";
        } else if (mainDir.equals("E")) {
            return "§e→ E";
        } else if (mainDir.equals("W")) {
            return "§e← W";
        } else {
            return "§7↘ " + mainDir;
        }
    }

    private String formatStat(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }

    private String getHealthColor(double current, double max) {
        double percent = current / max;
        if (percent <= 0.25) return "§4§l";
        if (percent <= 0.5) return "§c";
        if (percent <= 0.75) return "§e";
        return "§a";
    }

    private String getDefenseColor(double defense) {
        if (defense >= 150) return "§b§l";
        if (defense >= 100) return "§9";
        if (defense >= 50) return "§3";
        if (defense >= 20) return "§7";
        return "§8";
    }

    private String getDamageColor(double damage) {
        if (damage >= 100) return "§c§l";
        if (damage >= 50) return "§c";
        if (damage >= 25) return "§6";
        if (damage >= 10) return "§e";
        return "§f";
    }

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
