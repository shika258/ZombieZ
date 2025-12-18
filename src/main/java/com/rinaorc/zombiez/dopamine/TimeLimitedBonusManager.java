package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de bonus à durée limitée
 *
 * Effet dopamine: Crée des moments d'urgence excitants avec des objectifs
 * à court terme et des récompenses immédiates, stimulant l'engagement continu.
 *
 * Fonctionnalités:
 * - Défis aléatoires proposés aux joueurs
 * - Timer visible avec boss bar
 * - Récompenses multiplicatives (XP, points)
 * - Différents types de défis (kills, combo, streak)
 *
 * @author ZombieZ Dopamine System
 */
public class TimeLimitedBonusManager implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // Défis actifs par joueur
    private final Map<UUID, ActiveChallenge> activeChallenges = new ConcurrentHashMap<>();

    // Configuration
    private static final int MIN_INTERVAL_SECONDS = 60;    // Minimum entre deux défis
    private static final int MAX_INTERVAL_SECONDS = 180;   // Maximum entre deux défis
    private static final double TRIGGER_CHANCE = 0.15;     // 15% de chance par kill

    // Types de défis disponibles
    private static final ChallengeType[] CHALLENGE_TYPES = {
        new ChallengeType("RAPID_KILLS", "Élimination Rapide", "Tue %d zombies en %ds!", 5, 15, 1.5, 1.3),
        new ChallengeType("ELITE_HUNTER", "Chasseur d'Élite", "Tue %d élites en %ds!", 2, 20, 2.0, 1.5),
        new ChallengeType("NO_DAMAGE", "Intouchable", "Tue %d zombies sans prendre de dégâts!", 8, 25, 2.0, 1.5),
        new ChallengeType("COMBO_MASTER", "Maître du Combo", "Atteins un combo de %d!", 15, 20, 1.8, 1.4),
        new ChallengeType("HEADSHOT_STREAK", "Tireur d'Élite", "Fais %d headshots consécutifs!", 3, 25, 2.0, 1.5),
        new ChallengeType("SPEED_DEMON", "Démon de Vitesse", "Tue %d zombies en %ds!", 10, 12, 2.5, 2.0)
    };

    public TimeLimitedBonusManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startChallengeProposer();
    }

    /**
     * Propose un défi à un joueur (appelé aléatoirement ou manuellement)
     */
    public void proposeChallenge(Player player) {
        if (activeChallenges.containsKey(player.getUniqueId())) {
            return; // Déjà un défi actif
        }

        // Sélectionner un défi aléatoire
        ChallengeType type = CHALLENGE_TYPES[random.nextInt(CHALLENGE_TYPES.length)];

        // Créer le défi actif
        ActiveChallenge challenge = new ActiveChallenge(player, type);
        activeChallenges.put(player.getUniqueId(), challenge);

        // Afficher l'annonce
        announceChallenge(player, challenge);

        // Démarrer le timer
        startChallengeTimer(player, challenge);
    }

    /**
     * Annonce un nouveau défi au joueur
     */
    private void announceChallenge(Player player, ActiveChallenge challenge) {
        ChallengeType type = challenge.type;

        // Titre spectaculaire
        String title = "§6§l⚡ DÉFI BONUS! ⚡";
        String subtitle = String.format(type.description, type.targetCount, type.durationSeconds);

        player.sendTitle(title, "§e" + subtitle, 5, 40, 10);

        // Message chat
        player.sendMessage("");
        player.sendMessage("§6§l╔══════════════════════════════════╗");
        player.sendMessage("§6§l║     §e§l⚡ DÉFI BONUS ACTIF! ⚡     §6§l║");
        player.sendMessage("§6§l╠══════════════════════════════════╣");
        player.sendMessage("§6§l║ §f" + type.displayName);
        player.sendMessage("§6§l║ §7" + String.format(type.description, type.targetCount, type.durationSeconds));
        player.sendMessage("§6§l║");
        player.sendMessage("§6§l║ §aRécompense: §f+" + (int)((type.xpMultiplier - 1) * 100) + "% XP §7& §f+" + (int)((type.pointsMultiplier - 1) * 100) + "% Points");
        player.sendMessage("§6§l╚══════════════════════════════════╝");
        player.sendMessage("");

        // Sons d'introduction
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Particules
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Démarre le timer du défi
     */
    private void startChallengeTimer(Player player, ActiveChallenge challenge) {
        // Créer la boss bar
        BossBar bossBar = BossBar.bossBar(
            Component.text("⚡ " + challenge.type.displayName + " ⚡")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD),
            1.0f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );

        challenge.bossBar = bossBar;
        player.showBossBar(bossBar);

        // Task de mise à jour
        challenge.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeChallenges.containsKey(player.getUniqueId())) {
                    cleanup(player);
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - challenge.startTime;
                long remaining = (challenge.type.durationSeconds * 1000L) - elapsed;

                if (remaining <= 0) {
                    // Temps écoulé - échec
                    failChallenge(player, challenge);
                    cancel();
                    return;
                }

                // Mettre à jour la boss bar
                float progress = remaining / (float)(challenge.type.durationSeconds * 1000L);
                bossBar.progress(Math.max(0, Math.min(1, progress)));

                // Mettre à jour le texte avec la progression
                String progressText = String.format("⚡ %s: %d/%d - %.1fs ⚡",
                    challenge.type.displayName,
                    challenge.currentProgress,
                    challenge.type.targetCount,
                    remaining / 1000.0);

                bossBar.name(Component.text(progressText)
                    .color(remaining < 5000 ? NamedTextColor.RED : NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD));

                // Son d'urgence dans les dernières secondes
                if (remaining < 5000 && remaining > 4900) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                } else if (remaining < 3000 && (remaining / 500) % 2 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 2f);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Toutes les 2 ticks (0.1s)
    }

    /**
     * Met à jour la progression d'un défi (appelé lors d'un kill)
     */
    public void onKill(Player player, LivingEntity killed, boolean isElite, boolean isHeadshot) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null) {
            // Pas de défi actif - chance d'en proposer un
            if (random.nextDouble() < TRIGGER_CHANCE) {
                long lastChallenge = getLastChallengeTime(player);
                long elapsed = System.currentTimeMillis() - lastChallenge;

                if (elapsed > MIN_INTERVAL_SECONDS * 1000L) {
                    proposeChallenge(player);
                }
            }
            return;
        }

        // Vérifier si le kill compte pour le défi
        boolean counts = switch (challenge.type.id) {
            case "RAPID_KILLS", "SPEED_DEMON" -> true;
            case "ELITE_HUNTER" -> isElite;
            case "NO_DAMAGE" -> !challenge.tookDamage;
            case "HEADSHOT_STREAK" -> isHeadshot;
            case "COMBO_MASTER" -> false; // Géré par checkCombo
            default -> true;
        };

        if (counts) {
            challenge.currentProgress++;

            // Son de progression
            float pitch = 1.0f + (challenge.currentProgress * 0.1f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, Math.min(2.0f, pitch));

            // Particules de progression
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, killed.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);

            // Vérifier si le défi est complété
            if (challenge.currentProgress >= challenge.type.targetCount) {
                completeChallenge(player, challenge);
            }
        }
    }

    /**
     * Vérifie si un combo atteint l'objectif du défi
     */
    public void checkCombo(Player player, int combo) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null || !challenge.type.id.equals("COMBO_MASTER")) return;

        challenge.currentProgress = combo;

        if (combo >= challenge.type.targetCount) {
            completeChallenge(player, challenge);
        }
    }

    /**
     * Notifie que le joueur a pris des dégâts (pour le défi "Intouchable")
     */
    public void onDamageTaken(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge != null && challenge.type.id.equals("NO_DAMAGE")) {
            challenge.tookDamage = true;
            // Le défi continue mais ne pourra plus être complété
            player.sendMessage("§c⚠ Tu as pris des dégâts! Le bonus \"Intouchable\" est annulé.");
        }
    }

    /**
     * Complète un défi avec succès
     */
    private void completeChallenge(Player player, ActiveChallenge challenge) {
        // Retirer le défi
        activeChallenges.remove(player.getUniqueId());
        cleanup(player);

        // Calculer les récompenses
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            // Bonus XP
            long bonusXp = (long) (100 * challenge.type.xpMultiplier);
            data.addXp(bonusXp);

            // Bonus Points
            long bonusPoints = (long) (50 * challenge.type.pointsMultiplier);
            data.addPoints(bonusPoints);

            // Enregistrer le temps du défi
            data.setStat("last_challenge_time", System.currentTimeMillis());
        }

        // Annonce de succès
        String title = "§a§l✓ DÉFI RÉUSSI! ✓";
        String subtitle = "§e+" + (int)((challenge.type.xpMultiplier - 1) * 100) + "% XP & Points!";

        player.sendTitle(title, subtitle, 5, 40, 10);

        // Message chat
        player.sendMessage("§a§l★ " + challenge.type.displayName + " COMPLÉTÉ! §r§7(+" + (int)((challenge.type.xpMultiplier - 1) * 100) + "% bonus appliqué)");

        // Sons de victoire
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        // Particules de célébration
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.2);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Échoue un défi (temps écoulé)
     */
    private void failChallenge(Player player, ActiveChallenge challenge) {
        // Retirer le défi
        activeChallenges.remove(player.getUniqueId());
        cleanup(player);

        // Enregistrer le temps
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            data.setStat("last_challenge_time", System.currentTimeMillis());
        }

        // Annonce d'échec
        String title = "§c§l✗ TEMPS ÉCOULÉ! ✗";
        String subtitle = "§7" + challenge.currentProgress + "/" + challenge.type.targetCount + " - Prochaine fois!";

        player.sendTitle(title, subtitle, 5, 30, 10);

        // Son d'échec
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);

        // Message d'encouragement
        player.sendMessage("§7Défi échoué - Reste attentif pour le prochain!");
    }

    /**
     * Nettoie les ressources d'un défi
     */
    private void cleanup(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge != null) {
            if (challenge.bossBar != null) {
                player.hideBossBar(challenge.bossBar);
            }
            if (challenge.timerTask != null && !challenge.timerTask.isCancelled()) {
                challenge.timerTask.cancel();
            }
        }
    }

    /**
     * Obtient le temps du dernier défi d'un joueur
     */
    private long getLastChallengeTime(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 0;
        return data.getStat("last_challenge_time");
    }

    /**
     * Démarre la tâche qui propose des défis aléatoirement
     */
    private void startChallengeProposer() {
        // Vérifier périodiquement si on peut proposer un défi
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (activeChallenges.containsKey(player.getUniqueId())) continue;

                // Vérifier si le joueur est en zone de combat
                if (plugin.getZoneManager().getPlayerZone(player) == null) continue;

                // Vérifier le délai depuis le dernier défi
                long lastChallenge = getLastChallengeTime(player);
                long elapsed = System.currentTimeMillis() - lastChallenge;

                // Proposer un défi aléatoirement si le délai est passé
                if (elapsed > MIN_INTERVAL_SECONDS * 1000L && random.nextDouble() < 0.02) { // 2% chance par check
                    proposeChallenge(player);
                }
            }
        }, 20L * 30, 20L * 30); // Toutes les 30 secondes
    }

    /**
     * Obtient le multiplicateur de bonus actif pour un joueur
     * (utilisé pour appliquer les bonus XP/points)
     */
    public double getActiveXpMultiplier(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null) return 1.0;
        // Le bonus s'applique pendant le défi si le joueur fait des progrès
        return challenge.currentProgress > 0 ? challenge.type.xpMultiplier : 1.0;
    }

    public double getActivePointsMultiplier(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null) return 1.0;
        return challenge.currentProgress > 0 ? challenge.type.pointsMultiplier : 1.0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // Vérifier si c'est un mob ZombieZ
        if (!plugin.getZombieManager().isZombieZMob(entity)) return;

        // Déterminer le type de mob (avec vérification de bounds)
        boolean isElite = false;
        if (entity.hasMetadata("zombiez_type")) {
            var metadata = entity.getMetadata("zombiez_type");
            if (!metadata.isEmpty()) {
                isElite = metadata.get(0).asString().contains("ELITE");
            }
        }

        boolean isHeadshot = false;
        if (entity.hasMetadata("zombiez_damage_headshot")) {
            var metadata = entity.getMetadata("zombiez_damage_headshot");
            if (!metadata.isEmpty()) {
                isHeadshot = metadata.get(0).asBoolean();
            }
        }

        onKill(killer, entity, isElite, isHeadshot);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ActiveChallenge challenge = activeChallenges.remove(uuid);
        if (challenge != null) {
            if (challenge.timerTask != null && !challenge.timerTask.isCancelled()) {
                challenge.timerTask.cancel();
            }
        }
    }

    public void shutdown() {
        for (Map.Entry<UUID, ActiveChallenge> entry : activeChallenges.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                cleanup(player);
            }
        }
        activeChallenges.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Type de défi
     */
    private record ChallengeType(
        String id,
        String displayName,
        String description,
        int targetCount,
        int durationSeconds,
        double xpMultiplier,
        double pointsMultiplier
    ) {}

    /**
     * Défi actif pour un joueur
     */
    private static class ActiveChallenge {
        final Player player;
        final ChallengeType type;
        final long startTime;
        int currentProgress = 0;
        boolean tookDamage = false;
        BossBar bossBar;
        BukkitTask timerTask;

        ActiveChallenge(Player player, ChallengeType type) {
            this.player = player;
            this.type = type;
            this.startTime = System.currentTimeMillis();
        }
    }
}
