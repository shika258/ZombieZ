package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Système de récompenses de connexion quotidienne
 *
 * Effet dopamine: Récompense les joueurs fidèles avec des bonus croissants,
 * créant une habitude de connexion et un sentiment de progression continue.
 *
 * Fonctionnalités:
 * - Bonus croissant pour connexions consécutives
 * - Récompenses spéciales aux milestones (7, 14, 30 jours)
 * - Animation de célébration à chaque connexion
 * - Protection contre la perte du streak (1 jour de grâce)
 *
 * @author ZombieZ Dopamine System
 */
public class DailyLoginStreakManager implements Listener {

    private final ZombieZPlugin plugin;

    // Récompenses par jour de streak
    private static final int[] DAILY_POINTS = {100, 150, 200, 250, 300, 350, 400}; // Jours 1-7
    private static final int[] DAILY_XP = {50, 75, 100, 125, 150, 175, 200};
    private static final int[] DAILY_GEMS = {0, 0, 1, 0, 0, 2, 5}; // Gems aux jours 3, 6, 7

    // Milestones spéciaux
    private static final int[] MILESTONE_DAYS = {7, 14, 21, 30, 60, 90, 180, 365};
    private static final int[] MILESTONE_GEMS = {10, 25, 40, 75, 150, 250, 500, 1000};

    public DailyLoginStreakManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Vérifier le streak avec un léger délai (données chargées)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkAndRewardStreak(player);
        }, 40L); // 2 secondes après la connexion
    }

    /**
     * Vérifie et récompense le streak de connexion
     */
    private void checkAndRewardStreak(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        Instant lastReward = data.getLastDailyReward();
        int currentStreak = data.getDailyStreak();
        LocalDate today = LocalDate.now();

        // Vérifier si la récompense a déjà été réclamée aujourd'hui
        if (lastReward != null) {
            LocalDate lastRewardDate = lastReward.atZone(ZoneId.systemDefault()).toLocalDate();

            if (lastRewardDate.equals(today)) {
                // Déjà réclamé aujourd'hui
                return;
            }

            // Calculer les jours depuis la dernière récompense
            long daysSinceLastReward = ChronoUnit.DAYS.between(lastRewardDate, today);

            if (daysSinceLastReward == 1) {
                // Connexion consécutive - incrémenter le streak
                currentStreak++;
            } else if (daysSinceLastReward == 2) {
                // 1 jour de grâce - garder le streak mais avertir
                player.sendMessage("§6⚠ §eTu as manqué un jour! Ton streak est sauvé cette fois.");
            } else {
                // Streak perdu
                if (currentStreak > 0) {
                    player.sendMessage("§c⚠ Ton streak de §f" + currentStreak + " jours §ca été réinitialisé...");
                }
                currentStreak = 1;
            }
        } else {
            // Premier login avec le système
            currentStreak = 1;
        }

        // Mettre à jour les données
        data.setDailyStreak(currentStreak);
        data.setLastDailyReward(Instant.now());

        // Donner les récompenses
        giveStreakRewards(player, data, currentStreak);
    }

    /**
     * Donne les récompenses de streak
     */
    private void giveStreakRewards(Player player, PlayerData data, int streak) {
        // Calculer les récompenses de base (cycle de 7 jours)
        int dayIndex = ((streak - 1) % 7);
        int basePoints = DAILY_POINTS[dayIndex];
        int baseXp = DAILY_XP[dayIndex];
        int gems = DAILY_GEMS[dayIndex];

        // Multiplicateur basé sur le streak total
        double streakMultiplier = 1.0 + (Math.min(streak, 30) * 0.02); // +2% par jour, max +60%

        int finalPoints = (int) (basePoints * streakMultiplier);
        int finalXp = (int) (baseXp * streakMultiplier);

        // Appliquer les récompenses
        data.addPoints(finalPoints);
        data.addXp(finalXp);
        if (gems > 0) {
            data.addGems(gems);
        }

        // Vérifier les milestones
        int milestoneGems = checkMilestone(streak);
        if (milestoneGems > 0) {
            data.addGems(milestoneGems);
            gems += milestoneGems;
        }

        // Afficher la récompense
        showRewardAnimation(player, streak, finalPoints, finalXp, gems, milestoneGems > 0);
    }

    /**
     * Vérifie si un milestone est atteint
     */
    private int checkMilestone(int streak) {
        for (int i = 0; i < MILESTONE_DAYS.length; i++) {
            if (streak == MILESTONE_DAYS[i]) {
                return MILESTONE_GEMS[i];
            }
        }
        return 0;
    }

    /**
     * Affiche l'animation de récompense
     */
    private void showRewardAnimation(Player player, int streak, int points, int xp, int gems, boolean isMilestone) {
        // Titre
        String title;
        String subtitle;

        if (isMilestone) {
            title = "§6§l✦ MILESTONE ATTEINT! ✦";
            subtitle = "§e" + streak + " jours consécutifs!";
        } else if (streak >= 7) {
            title = "§e§l★ STREAK: " + streak + " JOURS! ★";
            subtitle = "§7Continue comme ça!";
        } else {
            title = "§a§l✓ CONNEXION QUOTIDIENNE";
            subtitle = "§7Jour " + streak + " - Continue!";
        }

        player.sendTitle(title, subtitle, 10, 60, 15);

        // Message chat détaillé
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("");

                // En-tête compact avec emoji
                if (isMilestone) {
                    player.sendMessage("§6§l⭐ MILESTONE " + streak + " JOURS! §e— Félicitations!");
                } else {
                    player.sendMessage("§e§l☀ Récompense Quotidienne §8— §7Jour §a" + streak);
                }

                player.sendMessage("§8" + "─".repeat(40));

                // Récompenses sur une ligne compacte avec icônes
                StringBuilder rewardsLine = new StringBuilder("§f  ");
                rewardsLine.append("§a+").append(points).append(" §2Points  ");
                rewardsLine.append("§b+").append(xp).append(" §3XP");
                if (gems > 0) {
                    rewardsLine.append("  §d+").append(gems).append(" §5Gemmes ✦");
                }
                player.sendMessage(rewardsLine.toString());

                // Aperçu de demain et milestone sur une ligne
                int nextDayIndex = (streak % 7);
                int nextPoints = (int) (DAILY_POINTS[nextDayIndex] * (1.0 + (Math.min(streak + 1, 30) * 0.02)));
                int nextMilestone = getNextMilestone(streak);

                StringBuilder infoLine = new StringBuilder("§7  ");
                infoLine.append("§7Demain: §e+").append(nextPoints);
                if (nextMilestone > 0) {
                    int daysLeft = nextMilestone - streak;
                    infoLine.append(" §8| §7Milestone §e").append(nextMilestone).append("j §8(").append(daysLeft).append("j)");
                }
                player.sendMessage(infoLine.toString());

                player.sendMessage("");
            }
        }.runTaskLater(plugin, 20L);

        // Sons
        if (isMilestone) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 0.8f);
        } else if (streak >= 7) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
        }

        // Particules
        spawnStreakParticles(player, streak, isMilestone);
    }

    /**
     * Spawn les particules de célébration
     */
    private void spawnStreakParticles(Player player, int streak, boolean isMilestone) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = isMilestone ? 40 : 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                org.bukkit.Location loc = player.getLocation().add(0, 1, 0);

                if (isMilestone) {
                    // Effet spectaculaire pour les milestones
                    for (int i = 0; i < 5; i++) {
                        double angle = (ticks * 0.3) + (i * Math.PI * 2 / 5);
                        double radius = 1.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        double y = ticks * 0.1;

                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                    }

                    if (ticks % 10 == 0) {
                        player.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                } else {
                    // Effet normal
                    Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.5, 0.5, 0.5, 0, gold);

                    if (streak >= 7) {
                        player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0.02);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Obtient le prochain milestone
     */
    private int getNextMilestone(int currentStreak) {
        for (int milestone : MILESTONE_DAYS) {
            if (milestone > currentStreak) {
                return milestone;
            }
        }
        return 0;
    }

    /**
     * Obtient le streak actuel d'un joueur
     */
    public int getStreak(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        return data != null ? data.getDailyStreak() : 0;
    }

    /**
     * Vérifie si le joueur a réclamé sa récompense aujourd'hui
     */
    public boolean hasClaimedToday(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null || data.getLastDailyReward() == null) return false;

        LocalDate lastRewardDate = data.getLastDailyReward().atZone(ZoneId.systemDefault()).toLocalDate();
        return lastRewardDate.equals(LocalDate.now());
    }
}
