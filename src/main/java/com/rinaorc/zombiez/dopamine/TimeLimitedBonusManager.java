package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.zombies.types.ZombieType;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de bonus à durée limitée (refactorisé)
 *
 * Changements v2:
 * - Détection correcte des élites via ZombieType.getCategory()
 * - Messages chat concis (plus de box verbose)
 * - Utilisation de l'ActionBar au lieu des Titles pour réduire le spam
 * - Correction de la logique NO_DAMAGE (échec immédiat)
 * - Correction de HEADSHOT_STREAK (reset sur non-headshot)
 * - Optimisation de la fréquence de mise à jour
 */
public class TimeLimitedBonusManager implements Listener {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    // Défis actifs par joueur
    private final Map<UUID, ActiveChallenge> activeChallenges = new ConcurrentHashMap<>();

    // Tracking des headshots consécutifs
    private final Map<UUID, Integer> consecutiveHeadshots = new ConcurrentHashMap<>();

    // Configuration
    private static final int MIN_INTERVAL_SECONDS = 90; // Minimum entre deux défis
    private static final int MAX_INTERVAL_SECONDS = 240; // Maximum entre deux défis
    private static final double TRIGGER_CHANCE = 0.12; // 12% de chance par kill

    // Types de défis disponibles (rééquilibrés)
    private static final ChallengeType[] CHALLENGE_TYPES = {
            new ChallengeType("RAPID_KILLS", "Élimination Rapide", "Tue %d zombies", 6, 18, 1.4, 1.3),
            new ChallengeType("ELITE_HUNTER", "Chasseur d'Élite", "Tue %d élites", 2, 30, 2.0, 1.6),
            new ChallengeType("NO_DAMAGE", "Intouchable", "Tue %d zombies sans dégâts", 5, 20, 1.8, 1.5),
            new ChallengeType("COMBO_MASTER", "Maître du Combo", "Atteins un combo de %d", 12, 25, 1.6, 1.4),
            new ChallengeType("HEADSHOT_STREAK", "Tireur d'Élite", "Fais %d headshots", 4, 30, 1.8, 1.5),
            new ChallengeType("SPEED_DEMON", "Démon de Vitesse", "Tue %d zombies", 8, 10, 2.2, 1.8)
    };

    public TimeLimitedBonusManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        startChallengeProposer();
    }

    /**
     * Propose un défi à un joueur
     */
    public void proposeChallenge(Player player) {
        if (activeChallenges.containsKey(player.getUniqueId())) {
            return;
        }

        // Sélectionner un défi aléatoire
        ChallengeType type = CHALLENGE_TYPES[random.nextInt(CHALLENGE_TYPES.length)];

        // Créer le défi actif
        ActiveChallenge challenge = new ActiveChallenge(player, type);
        activeChallenges.put(player.getUniqueId(), challenge);

        // Reset les headshots consécutifs si c'est un défi headshot
        if (type.id.equals("HEADSHOT_STREAK")) {
            consecutiveHeadshots.put(player.getUniqueId(), 0);
        }

        // Annonce concise
        announceChallenge(player, challenge);

        // Démarrer le timer
        startChallengeTimer(player, challenge);
    }

    /**
     * Annonce un nouveau défi (version concise)
     */
    private void announceChallenge(Player player, ActiveChallenge challenge) {
        ChallengeType type = challenge.type;

        // Un seul Title court au démarrage
        String subtitle = String.format("§e" + type.description + " §7(%ds)", type.targetCount, type.durationSeconds);
        player.sendTitle("§6⚡ " + type.displayName, subtitle, 5, 30, 5);

        // Message chat simple avec valeurs fixes
        long bonusXp = (long) (100 * type.xpMultiplier);
        long bonusPoints = (long) (50 * type.pointsMultiplier);
        String bonusText = "§a+" + bonusXp + " XP§7, §e+" + bonusPoints + " Points";
        player.sendMessage("§6⚡ §eNouveau défi: §f" + type.displayName + " §7- " + bonusText);

        // Sons discrets
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);

        // Particules légères
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.05);
    }

    /**
     * Démarre le timer du défi
     */
    private void startChallengeTimer(Player player, ActiveChallenge challenge) {
        // Créer la boss bar
        BossBar bossBar = BossBar.bossBar(
                buildBossBarText(challenge),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS);

        challenge.bossBar = bossBar;
        player.showBossBar(bossBar);

        // Task de mise à jour (toutes les 5 ticks = 0.25s)
        challenge.timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeChallenges.containsKey(player.getUniqueId())) {
                    cleanup(player, challenge);
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - challenge.startTime;
                long remaining = (challenge.type.durationSeconds * 1000L) - elapsed;

                if (remaining <= 0) {
                    failChallenge(player, challenge);
                    cancel();
                    return;
                }

                // Mettre à jour la boss bar
                float progress = remaining / (float) (challenge.type.durationSeconds * 1000L);
                bossBar.progress(Math.max(0, Math.min(1, progress)));

                // Couleur selon le temps restant
                BossBar.Color color = remaining < 5000 ? BossBar.Color.RED
                        : remaining < 10000 ? BossBar.Color.YELLOW : BossBar.Color.GREEN;
                bossBar.color(color);

                // Mettre à jour le texte
                bossBar.name(buildBossBarText(challenge, remaining));

                // Son d'urgence seulement à 3 secondes (une seule fois)
                if (remaining <= 3000 && remaining > 2750) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /**
     * Construit le texte de la boss bar
     */
    private Component buildBossBarText(ActiveChallenge challenge) {
        return buildBossBarText(challenge, challenge.type.durationSeconds * 1000L);
    }

    private Component buildBossBarText(ActiveChallenge challenge, long remainingMs) {
        String text = String.format("⚡ %s: %d/%d [%.0fs]",
                challenge.type.displayName,
                challenge.currentProgress,
                challenge.type.targetCount,
                remainingMs / 1000.0);

        NamedTextColor color = remainingMs < 5000 ? NamedTextColor.RED : NamedTextColor.GOLD;
        return Component.text(text).color(color).decorate(TextDecoration.BOLD);
    }

    /**
     * Met à jour la progression d'un défi (appelé lors d'un kill)
     */
    public void onKill(Player player, LivingEntity killed, boolean isElite, boolean isHeadshot) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());

        // Pas de défi actif - chance d'en proposer un
        if (challenge == null) {
            if (random.nextDouble() < TRIGGER_CHANCE) {
                long lastChallenge = getLastChallengeTime(player);
                long elapsed = System.currentTimeMillis() - lastChallenge;

                if (elapsed > MIN_INTERVAL_SECONDS * 1000L) {
                    proposeChallenge(player);
                }
            }
            return;
        }

        // Traiter le défi headshot streak
        if (challenge.type.id.equals("HEADSHOT_STREAK")) {
            if (isHeadshot) {
                challenge.currentProgress++;
                showProgressFeedback(player, challenge);

                if (challenge.currentProgress >= challenge.type.targetCount) {
                    completeChallenge(player, challenge);
                }
            } else {
                // Non-headshot = reset du compteur
                if (challenge.currentProgress > 0) {
                    challenge.currentProgress = 0;
                    player.sendActionBar(Component.text("§c✗ Streak reset! §7Headshots requis", NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
                }
            }
            return;
        }

        // Vérifier si le kill compte pour les autres défis
        boolean counts = switch (challenge.type.id) {
            case "RAPID_KILLS", "SPEED_DEMON" -> true;
            case "ELITE_HUNTER" -> isElite;
            case "NO_DAMAGE" -> !challenge.tookDamage;
            case "COMBO_MASTER" -> false; // Géré par checkCombo
            default -> true;
        };

        if (counts) {
            challenge.currentProgress++;
            showProgressFeedback(player, challenge);

            if (challenge.currentProgress >= challenge.type.targetCount) {
                completeChallenge(player, challenge);
            }
        }
    }

    /**
     * Affiche un feedback de progression discret
     */
    private void showProgressFeedback(Player player, ActiveChallenge challenge) {
        // Son de progression (pitch monte avec la progression)
        float pitch = 1.0f + (challenge.currentProgress * 0.08f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, Math.min(2.0f, pitch));

        // ActionBar de progression
        int remaining = challenge.type.targetCount - challenge.currentProgress;
        if (remaining > 0) {
            String progressBar = buildProgressBar(challenge.currentProgress, challenge.type.targetCount);
            player.sendActionBar(Component
                    .text("§e⚡ " + progressBar + " §7(" + remaining + " restant" + (remaining > 1 ? "s" : "") + ")"));
        }
    }

    /**
     * Construit une barre de progression visuelle
     */
    private String buildProgressBar(int current, int total) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            bar.append(i < current ? "§a■" : "§8□");
        }
        return bar.toString();
    }

    /**
     * Vérifie si un combo atteint l'objectif du défi
     */
    public void checkCombo(Player player, int combo) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null || !challenge.type.id.equals("COMBO_MASTER"))
            return;

        challenge.currentProgress = Math.max(challenge.currentProgress, combo);

        if (combo >= challenge.type.targetCount) {
            completeChallenge(player, challenge);
        }
    }

    /**
     * Notifie que le joueur a pris des dégâts
     */
    public void onDamageTaken(Player player, double damage) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null)
            return;

        if (challenge.type.id.equals("NO_DAMAGE") && !challenge.tookDamage) {
            challenge.tookDamage = true;
            // Échec immédiat du défi
            failChallenge(player, challenge, "§cTouché! Défi échoué.");
        }
    }

    /**
     * Complète un défi avec succès
     */
    private void completeChallenge(Player player, ActiveChallenge challenge) {
        activeChallenges.remove(player.getUniqueId());
        cleanup(player, challenge);
        long bonusXp = 0;
        long bonusPoints = 0;
        // Calculer les récompenses
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            bonusXp = (long) (100 * challenge.type.xpMultiplier);
            bonusPoints = (long) (50 * challenge.type.pointsMultiplier);
            data.addXp(bonusXp);
            data.addPoints(bonusPoints);
            data.setStat("last_challenge_time", System.currentTimeMillis());
        }

        // Feedback de succès - ActionBar + son (pas de Title intrusif)
        player.sendActionBar(Component.text("§a§l✓ " + challenge.type.displayName + " réussi! §r§7(§a+" + bonusXp
                + " XP§7, §e+" + bonusPoints + " Points§7)"));

        // Message chat simple avec valeurs fixes
        player.sendMessage("§a✓ §fDéfi complété: §e" + challenge.type.displayName + " §7(§a+" + bonusXp + " XP§7, §e+"
                + bonusPoints + " Points§7)");

        // Sons satisfaisants
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);

        // Particules de célébration (légères)
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4,
                0.1);
    }

    /**
     * Échoue un défi (temps écoulé)
     */
    private void failChallenge(Player player, ActiveChallenge challenge) {
        failChallenge(player, challenge, null);
    }

    private void failChallenge(Player player, ActiveChallenge challenge, String customMessage) {
        activeChallenges.remove(player.getUniqueId());
        cleanup(player, challenge);

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data != null) {
            data.setStat("last_challenge_time", System.currentTimeMillis());
        }

        // Feedback d'échec discret - ActionBar seulement
        String message = customMessage != null ? customMessage
                : "§c✗ Temps écoulé! §7(" + challenge.currentProgress + "/" + challenge.type.targetCount + ")";
        player.sendActionBar(Component.text(message));

        // Son d'échec discret
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
    }

    /**
     * Nettoie les ressources d'un défi
     */
    private void cleanup(Player player, ActiveChallenge challenge) {
        if (challenge != null) {
            if (challenge.bossBar != null) {
                player.hideBossBar(challenge.bossBar);
            }
            if (challenge.timerTask != null && !challenge.timerTask.isCancelled()) {
                challenge.timerTask.cancel();
            }
        }
        consecutiveHeadshots.remove(player.getUniqueId());
    }

    /**
     * Obtient le temps du dernier défi d'un joueur
     */
    private long getLastChallengeTime(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null)
            return 0;
        return data.getStat("last_challenge_time");
    }

    /**
     * Démarre la tâche qui propose des défis aléatoirement
     */
    private void startChallengeProposer() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (activeChallenges.containsKey(player.getUniqueId()))
                    continue;
                if (plugin.getZoneManager().getPlayerZone(player) == null)
                    continue;

                long lastChallenge = getLastChallengeTime(player);
                long elapsed = System.currentTimeMillis() - lastChallenge;

                // 1.5% chance par check si délai passé
                if (elapsed > MIN_INTERVAL_SECONDS * 1000L && random.nextDouble() < 0.015) {
                    proposeChallenge(player);
                }
            }
        }, 20L * 45, 20L * 45); // Toutes les 45 secondes
    }

    /**
     * Obtient le multiplicateur de bonus actif pour un joueur
     */
    public double getActiveXpMultiplier(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null)
            return 1.0;
        return challenge.currentProgress > 0 ? challenge.type.xpMultiplier : 1.0;
    }

    public double getActivePointsMultiplier(Player player) {
        ActiveChallenge challenge = activeChallenges.get(player.getUniqueId());
        if (challenge == null)
            return 1.0;
        return challenge.currentProgress > 0 ? challenge.type.pointsMultiplier : 1.0;
    }

    /**
     * Vérifie si un joueur a un défi actif
     */
    public boolean hasActiveChallenge(Player player) {
        return activeChallenges.containsKey(player.getUniqueId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null)
            return;
        if (!plugin.getZombieManager().isZombieZMob(entity))
            return;

        // Détection correcte des élites via ZombieType
        boolean isElite = false;
        ZombieType zombieType = plugin.getZombieManager().getZombieType(entity);
        if (zombieType != null) {
            isElite = zombieType.getCategory() == ZombieType.ZombieCategory.ELITE ||
                    zombieType.getCategory() == ZombieType.ZombieCategory.MINIBOSS ||
                    zombieType.getCategory() == ZombieType.ZombieCategory.ZONE_BOSS;
        }

        // Vérification headshot via scoreboard tag (plus fiable que metadata à la mort)
        boolean isHeadshot = entity.getScoreboardTags().contains("zombiez_headshot_kill");

        onKill(killer, entity, isElite, isHeadshot);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        // Ignorer si pas de défi actif
        if (!activeChallenges.containsKey(player.getUniqueId()))
            return;

        // Ignorer les dégâts nuls ou très faibles
        if (event.getFinalDamage() < 0.5)
            return;

        onDamageTaken(player, event.getFinalDamage());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ActiveChallenge challenge = activeChallenges.remove(uuid);
        consecutiveHeadshots.remove(uuid);

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
                cleanup(player, entry.getValue());
            }
        }
        activeChallenges.clear();
        consecutiveHeadshots.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    private record ChallengeType(
            String id,
            String displayName,
            String description,
            int targetCount,
            int durationSeconds,
            double xpMultiplier,
            double pointsMultiplier) {
    }

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
