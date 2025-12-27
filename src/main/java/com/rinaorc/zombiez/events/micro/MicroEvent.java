package com.rinaorc.zombiez.events.micro;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Classe abstraite representant un micro-evenement
 * Les micro-evenements sont des evenements courts (20-60s) specifiques a un joueur
 *
 * Differences avec DynamicEvent:
 * - Specifique a UN joueur (pas global)
 * - Duree courte (20-60s vs 3-6min)
 * - Pas de boss bar (utilise ActionBar/Title)
 * - Spawn plus frequemment (3-5min vs 10-20min)
 * - Mecanique simple et immediate
 */
@Getter
public abstract class MicroEvent {

    protected final ZombieZPlugin plugin;
    protected final String id;
    protected final MicroEventType type;
    protected final Player player;
    protected final Location location;
    protected final Zone zone;
    protected final long startTime;

    @Setter
    protected boolean active = false;
    protected boolean completed = false;
    protected boolean failed = false;

    // Duree maximale (en ticks)
    protected int maxDuration;
    protected int elapsedTicks = 0;

    // Entites spawnees par l'evenement (pour cleanup)
    protected final Set<UUID> spawnedEntities = new HashSet<>();

    // Tache principale
    protected BukkitTask mainTask;

    // Système de countdown
    protected Set<Integer> announcedCountdowns = new HashSet<>();
    protected static final int[] MICRO_COUNTDOWN_THRESHOLDS = {10, 5, 3, 2, 1};

    public MicroEvent(ZombieZPlugin plugin, MicroEventType type, Player player, Location location, Zone zone) {
        this.plugin = plugin;
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.player = player;
        this.location = location.clone();
        this.zone = zone;
        this.startTime = System.currentTimeMillis();
        this.maxDuration = type.getDefaultDuration();
    }

    /**
     * Demarre le micro-evenement
     */
    public void start() {
        if (active) return;
        active = true;

        // Annoncer au joueur
        announceStart();

        // Demarrer la logique
        onStart();

        // Demarrer le tick
        mainTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                cleanup();
                return;
            }

            elapsedTicks++;

            // Verifier timeout
            if (elapsedTicks >= maxDuration) {
                fail();
                return;
            }

            // Verifier si le joueur est toujours valide
            if (!player.isOnline() || player.isDead()) {
                fail();
                return;
            }

            // Tick de l'evenement
            tick();

            // Vérifier countdown (uniquement au début de chaque seconde)
            if (elapsedTicks % 20 == 0) {
                checkCountdown();
            }
        }, 1L, 1L);
    }

    /**
     * Vérifie et annonce les alertes countdown
     */
    protected void checkCountdown() {
        int remaining = getRemainingTimeSeconds();

        for (int threshold : MICRO_COUNTDOWN_THRESHOLDS) {
            if (remaining == threshold && !announcedCountdowns.contains(threshold)) {
                announcedCountdowns.add(threshold);
                announceCountdown(threshold);
                break;
            }
        }
    }

    /**
     * Annonce le countdown au joueur
     */
    private void announceCountdown(int secondsRemaining) {
        String color;
        Sound sound;
        float pitch;

        if (secondsRemaining <= 3) {
            // Dernières secondes - URGENT!
            color = "§c§l";
            sound = Sound.BLOCK_NOTE_BLOCK_BELL;
            pitch = 2.0f - (secondsRemaining * 0.3f);
        } else if (secondsRemaining <= 5) {
            // Critique
            color = "§e§l";
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
            pitch = 1.5f;
        } else {
            // Première alerte (10s)
            color = "§6";
            sound = Sound.BLOCK_NOTE_BLOCK_CHIME;
            pitch = 1.2f;
        }

        // Son
        player.playSound(player.getLocation(), sound, 1.0f, pitch);

        // Titre pour les dernières secondes
        if (secondsRemaining <= 5) {
            String title;
            String subtitle;

            if (secondsRemaining <= 3) {
                title = color + "⏱ " + secondsRemaining;
                subtitle = "§7Dépêchez-vous!";
            } else {
                title = color + "⏱ 5 SECONDES!";
                subtitle = "§7" + type.getDisplayName() + " se termine!";
            }

            player.sendTitle(title, subtitle, 0, 20, 5);
        }

        // ActionBar pour toutes les alertes
        sendActionBar(type.getColor() + type.getIcon() + " " + type.getDisplayName() + " " + color + "⏱ " + secondsRemaining + "s!");
    }

    /**
     * Complete le micro-evenement (succes)
     */
    public void complete() {
        if (!active || completed || failed) return;

        completed = true;
        active = false;

        // Distribuer les recompenses
        distributeRewards();

        // Annoncer
        announceCompletion();

        // Cleanup
        cleanup();
    }

    /**
     * Echoue le micro-evenement
     */
    public void fail() {
        if (!active || completed || failed) return;

        failed = true;
        active = false;

        // Annoncer
        announceFailure();

        // Cleanup
        cleanup();
    }

    /**
     * Force l'arret
     */
    public void forceStop() {
        active = false;
        cleanup();
    }

    /**
     * Nettoyage des ressources
     */
    protected void cleanup() {
        // Annuler la tache
        if (mainTask != null && !mainTask.isCancelled()) {
            mainTask.cancel();
        }

        // Supprimer les entites spawnees
        for (UUID entityId : spawnedEntities) {
            Entity entity = plugin.getServer().getEntity(entityId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        spawnedEntities.clear();

        // Cleanup specifique
        onCleanup();
    }

    /**
     * Annonce le debut de l'evenement au joueur
     */
    protected void announceStart() {
        // Title
        player.sendTitle(
            type.getColor() + type.getIcon() + " " + type.getDisplayName().toUpperCase(),
            "§7" + type.getDescription(),
            5, 40, 10
        );

        // Message chat
        player.sendMessage("");
        player.sendMessage(type.getColor() + "§l" + type.getIcon() + " MICRO-EVENT: " + type.getDisplayName());
        player.sendMessage("§7" + type.getDescription());
        player.sendMessage("§7Temps limite: §e" + (maxDuration / 20) + "s");
        player.sendMessage("");

        // Son
        player.playSound(player.getLocation(), type.getStartSound(), 1f, 1f);

        // Particules a la location
        if (location.getWorld() != null) {
            location.getWorld().spawnParticle(Particle.FLASH, location, 1);
        }
    }

    /**
     * Distribue les recompenses
     */
    protected void distributeRewards() {
        // Calculer les recompenses avec multiplicateur de zone
        double zoneMultiplier = 1.0 + (zone.getId() * 0.05);
        int points = (int) (type.getBasePointsReward() * zoneMultiplier);
        int xp = (int) (type.getBaseXpReward() * zoneMultiplier);

        // Ajouter bonus specifique a l'evenement
        int bonusPoints = getBonusPoints();
        points += bonusPoints;

        // Distribuer
        plugin.getEconomyManager().addPoints(player, points);
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null) {
            playerData.addXp(xp);

            // ============ TRACKER MISSIONS: MICRO-ÉVÉNEMENTS ============
            plugin.getMissionManager().updateProgress(player,
                com.rinaorc.zombiez.progression.MissionManager.MissionTracker.MICRO_EVENTS_COMPLETED, 1);
        }

        // Message
        player.sendMessage("");
        player.sendMessage("§a§l✓ MICRO-EVENT COMPLETE!");
        player.sendMessage("§7Recompenses: §e+" + points + " Points §7| §b+" + xp + " XP");
        if (bonusPoints > 0) {
            player.sendMessage("§7Bonus: §a+" + bonusPoints + " Points");
        }
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    /**
     * Annonce la completion
     */
    protected void announceCompletion() {
        player.sendTitle(
            "§a§l✓ SUCCES!",
            "§7" + type.getDisplayName() + " termine!",
            5, 30, 10
        );

        // Particules de celebration
        if (player.getLocation().getWorld() != null) {
            player.getLocation().getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 1, 0.5, 0.1
            );
        }
    }

    /**
     * Annonce l'echec
     */
    protected void announceFailure() {
        player.sendTitle(
            "§c§l✗ ECHEC!",
            "§7" + type.getDisplayName() + " echoue...",
            5, 30, 10
        );

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
    }

    /**
     * Enregistre une entite spawnee pour cleanup
     */
    protected void registerEntity(Entity entity) {
        spawnedEntities.add(entity.getUniqueId());
    }

    /**
     * Verifie si l'evenement est valide
     */
    public boolean isValid() {
        return active && !completed && !failed;
    }

    /**
     * Temps restant en secondes
     */
    public int getRemainingTimeSeconds() {
        return Math.max(0, (maxDuration - elapsedTicks) / 20);
    }

    /**
     * Envoie une action bar au joueur
     */
    protected void sendActionBar(String message) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }

    // ==================== METHODES ABSTRAITES ====================

    /**
     * Appele au demarrage de l'evenement
     */
    protected abstract void onStart();

    /**
     * Appele a chaque tick (20 fois/seconde)
     */
    protected abstract void tick();

    /**
     * Appele lors du cleanup
     */
    protected abstract void onCleanup();

    /**
     * Retourne les points bonus specifiques a l'evenement
     */
    protected abstract int getBonusPoints();

    /**
     * Gere un evenement de degats sur une entite de l'evenement
     * @return true si l'evenement a ete gere
     */
    public abstract boolean handleDamage(LivingEntity entity, Player attacker, double damage);

    /**
     * Gere la mort d'une entite de l'evenement
     * @return true si l'evenement a ete gere
     */
    public abstract boolean handleDeath(LivingEntity entity, Player killer);
}
