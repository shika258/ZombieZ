package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;

/**
 * Système de gestion des records personnels
 *
 * Effet dopamine: Célèbre les accomplissements personnels du joueur
 * avec des popups satisfaisants et des effets visuels mémorables.
 *
 * Records trackés:
 * - Best Combo
 * - Best Kill Streak
 * - Best DPS (damage per second)
 * - Best Multi-Kill
 * - Best Session Kills
 * - Highest Single Hit Damage
 *
 * @author ZombieZ Dopamine System
 */
public class PersonalBestManager {

    private final ZombieZPlugin plugin;
    private final DecimalFormat dpsFormat = new DecimalFormat("#,##0.0");
    private final DecimalFormat damageFormat = new DecimalFormat("#,##0");

    // Préfixes pour les clés de stats
    private static final String STAT_BEST_COMBO = "best_combo";
    private static final String STAT_BEST_STREAK = "best_streak";
    private static final String STAT_BEST_DPS = "best_dps";
    private static final String STAT_BEST_MULTIKILL = "best_multikill";
    private static final String STAT_BEST_SESSION_KILLS = "best_session_kills";
    private static final String STAT_BEST_SINGLE_HIT = "best_single_hit";

    public PersonalBestManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTHODES DE CHECK ET MISE À JOUR DES RECORDS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Vérifie et met à jour le record de combo
     * @return true si nouveau record
     */
    public boolean checkBestCombo(Player player, int combo) {
        long currentBest = getPlayerStat(player, STAT_BEST_COMBO);

        if (combo > currentBest) {
            setPlayerStat(player, STAT_BEST_COMBO, combo);
            triggerNewRecordEffects(player, RecordType.COMBO, currentBest, combo);
            return true;
        }
        return false;
    }

    /**
     * Vérifie et met à jour le record de streak
     * @return true si nouveau record
     */
    public boolean checkBestStreak(Player player, int streak) {
        long currentBest = getPlayerStat(player, STAT_BEST_STREAK);

        if (streak > currentBest) {
            setPlayerStat(player, STAT_BEST_STREAK, streak);
            triggerNewRecordEffects(player, RecordType.STREAK, currentBest, streak);
            return true;
        }
        return false;
    }

    /**
     * Vérifie et met à jour le record de DPS
     * @return true si nouveau record
     */
    public boolean checkBestDPS(Player player, double dps) {
        long currentBest = getPlayerStat(player, STAT_BEST_DPS);
        long dpsLong = (long) (dps * 10); // Stocker avec 1 décimale

        if (dpsLong > currentBest) {
            setPlayerStat(player, STAT_BEST_DPS, dpsLong);
            triggerNewRecordEffects(player, RecordType.DPS, currentBest / 10.0, dps);
            return true;
        }
        return false;
    }

    /**
     * Vérifie et met à jour le record de multi-kill
     * @return true si nouveau record
     */
    public boolean checkBestMultiKill(Player player, int multiKill) {
        long currentBest = getPlayerStat(player, STAT_BEST_MULTIKILL);

        if (multiKill > currentBest) {
            setPlayerStat(player, STAT_BEST_MULTIKILL, multiKill);
            triggerNewRecordEffects(player, RecordType.MULTIKILL, currentBest, multiKill);
            return true;
        }
        return false;
    }

    /**
     * Vérifie et met à jour le record de kills en session
     * @return true si nouveau record
     */
    public boolean checkBestSessionKills(Player player, long sessionKills) {
        long currentBest = getPlayerStat(player, STAT_BEST_SESSION_KILLS);

        if (sessionKills > currentBest) {
            setPlayerStat(player, STAT_BEST_SESSION_KILLS, sessionKills);
            triggerNewRecordEffects(player, RecordType.SESSION_KILLS, currentBest, sessionKills);
            return true;
        }
        return false;
    }

    /**
     * Vérifie et met à jour le record de dégâts en un seul coup
     * @return true si nouveau record
     */
    public boolean checkBestSingleHit(Player player, double damage) {
        long currentBest = getPlayerStat(player, STAT_BEST_SINGLE_HIT);
        long damageLong = (long) damage;

        if (damageLong > currentBest) {
            setPlayerStat(player, STAT_BEST_SINGLE_HIT, damageLong);
            triggerNewRecordEffects(player, RecordType.SINGLE_HIT, currentBest, damageLong);
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS POUR LES RECORDS ACTUELS
    // ═══════════════════════════════════════════════════════════════════════

    public long getBestCombo(Player player) {
        return getPlayerStat(player, STAT_BEST_COMBO);
    }

    public long getBestStreak(Player player) {
        return getPlayerStat(player, STAT_BEST_STREAK);
    }

    public double getBestDPS(Player player) {
        return getPlayerStat(player, STAT_BEST_DPS) / 10.0;
    }

    public long getBestMultiKill(Player player) {
        return getPlayerStat(player, STAT_BEST_MULTIKILL);
    }

    public long getBestSessionKills(Player player) {
        return getPlayerStat(player, STAT_BEST_SESSION_KILLS);
    }

    public long getBestSingleHit(Player player) {
        return getPlayerStat(player, STAT_BEST_SINGLE_HIT);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EFFETS VISUELS ET SONORES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Déclenche les effets de nouveau record
     */
    private void triggerNewRecordEffects(Player player, RecordType type, double oldValue, double newValue) {
        // ═══════════════════════════════════════════════════════════════════
        // 1. TITRE SPECTACULAIRE
        // ═══════════════════════════════════════════════════════════════════
        String title = "§6§l★ NOUVEAU RECORD! ★";
        String subtitle = getRecordSubtitle(type, oldValue, newValue);

        player.sendTitle(title, subtitle, 10, 50, 15);

        // ═══════════════════════════════════════════════════════════════════
        // 2. MESSAGE CHAT DÉTAILLÉ
        // ═══════════════════════════════════════════════════════════════════
        player.sendMessage("");
        player.sendMessage("§6§l╔════════════════════════════════╗");
        player.sendMessage("§6§l║    §e§l★ RECORD PERSONNEL! ★    §6§l║");
        player.sendMessage("§6§l╠════════════════════════════════╣");
        player.sendMessage("§6§l║ §f" + type.getDisplayName() + ":");
        player.sendMessage("§6§l║ §7Ancien: §c" + formatValue(type, oldValue));
        player.sendMessage("§6§l║ §7Nouveau: §a§l" + formatValue(type, newValue) + " §6✦");
        player.sendMessage("§6§l╚════════════════════════════════╝");
        player.sendMessage("");

        // ═══════════════════════════════════════════════════════════════════
        // 3. SONS DE CÉLÉBRATION
        // ═══════════════════════════════════════════════════════════════════
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        // Son retardé pour effet de fanfare
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
        }, 5L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.8f);
        }, 8L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2.0f);
        }, 11L);

        // ═══════════════════════════════════════════════════════════════════
        // 4. EFFETS VISUELS
        // ═══════════════════════════════════════════════════════════════════
        spawnRecordParticles(player);

        // ═══════════════════════════════════════════════════════════════════
        // 5. ANNONCE POUR LES GROS RECORDS
        // ═══════════════════════════════════════════════════════════════════
        if (isSignificantRecord(type, newValue)) {
            announceRecord(player, type, newValue);
        }
    }

    /**
     * Spawn les particules de célébration de record
     */
    private void spawnRecordParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                org.bukkit.Location loc = player.getLocation().add(0, 1.5, 0);

                // Étoiles dorées montantes
                for (int i = 0; i < 3; i++) {
                    double angle = (ticks * 0.3) + (i * Math.PI * 2 / 3);
                    double radius = 1.0 + (ticks * 0.05);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = ticks * 0.1;

                    player.getWorld().spawnParticle(
                        Particle.TOTEM_OF_UNDYING,
                        loc.clone().add(x, y, z),
                        1, 0, 0, 0, 0
                    );
                }

                // Particules dorées
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f);
                player.getWorld().spawnParticle(
                    Particle.DUST,
                    loc,
                    5, 0.5, 0.5, 0.5, 0,
                    gold
                );

                // Fireworks à intervalles
                if (ticks % 10 == 0) {
                    player.getWorld().spawnParticle(
                        Particle.FIREWORK,
                        loc.clone().add(0, ticks * 0.1, 0),
                        15, 0.5, 0.5, 0.5, 0.1
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Annonce un record significatif au serveur
     */
    private void announceRecord(Player player, RecordType type, double value) {
        String message = "§6§l★ §e" + player.getName() + " §7a battu son record de §f" +
            type.getDisplayName() + "§7: §a§l" + formatValue(type, value) + "§7!";
        plugin.getServer().broadcastMessage(message);
    }

    /**
     * Vérifie si un record est significatif (mérite une annonce)
     */
    private boolean isSignificantRecord(RecordType type, double value) {
        return switch (type) {
            case COMBO -> value >= 100;
            case STREAK -> value >= 200;
            case DPS -> value >= 1000;
            case MULTIKILL -> value >= 7;
            case SESSION_KILLS -> value >= 1000;
            case SINGLE_HIT -> value >= 500;
        };
    }

    /**
     * Génère le sous-titre pour le type de record
     */
    private String getRecordSubtitle(RecordType type, double oldValue, double newValue) {
        String valueStr = formatValue(type, newValue);
        return type.getColor() + type.getDisplayName() + ": §f§l" + valueStr;
    }

    /**
     * Formate une valeur selon le type de record
     */
    private String formatValue(RecordType type, double value) {
        return switch (type) {
            case DPS -> dpsFormat.format(value) + " DPS";
            case SINGLE_HIT -> damageFormat.format(value) + " dégâts";
            case SESSION_KILLS -> damageFormat.format(value) + " kills";
            case COMBO -> (int) value + " combo";
            case STREAK -> (int) value + " streak";
            case MULTIKILL -> getMultiKillName((int) value);
        };
    }

    /**
     * Obtient le nom du multi-kill
     */
    private String getMultiKillName(int count) {
        return switch (count) {
            case 2 -> "Double Kill";
            case 3 -> "Triple Kill";
            case 4 -> "Quad Kill";
            case 5 -> "Penta Kill";
            case 6 -> "Hexa Kill";
            case 7 -> "Ultra Kill";
            case 8 -> "Monster Kill";
            default -> count >= 10 ? "GODLIKE" : count + " Kill";
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtient une stat du joueur depuis PlayerData
     */
    private long getPlayerStat(Player player, String key) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return data.getStat(key);
    }

    /**
     * Définit une stat du joueur dans PlayerData
     */
    private void setPlayerStat(Player player, String key, long value) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            data.setStat(key, value);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ENUMS ET CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Types de records
     */
    public enum RecordType {
        COMBO("Meilleur Combo", "§e"),
        STREAK("Meilleur Streak", "§c"),
        DPS("Meilleur DPS", "§b"),
        MULTIKILL("Meilleur Multi-Kill", "§d"),
        SESSION_KILLS("Kills en Session", "§a"),
        SINGLE_HIT("Plus Gros Coup", "§6");

        private final String displayName;
        private final String color;

        RecordType(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
}
