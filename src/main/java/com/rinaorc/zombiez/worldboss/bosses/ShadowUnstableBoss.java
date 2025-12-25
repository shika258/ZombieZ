package com.rinaorc.zombiez.worldboss.bosses;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.WorldBoss;
import com.rinaorc.zombiez.worldboss.WorldBossType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * L'Ombre Instable (Vitesse - Taille x1.5)
 *
 * Capacité: Devient invisible pendant 3s toutes les 10s et réapparaît derrière un joueur
 * Stratégie: Utiliser des sons/particules pour anticiper son retour
 *
 * Mécaniques:
 * - Invisibilité temporaire avec particules de fumée
 * - Téléportation derrière un joueur aléatoire
 * - Attaque surprise après réapparition
 * - Vitesse augmentée
 */
public class ShadowUnstableBoss extends WorldBoss {

    // État d'invisibilité
    private boolean isInvisible = false;
    private Player targetPlayer = null;

    // Configuration
    private static final int INVISIBILITY_DURATION_TICKS = 60; // 3 secondes
    private static final double TELEPORT_DISTANCE = 2.5;

    public ShadowUnstableBoss(ZombieZPlugin plugin, int zoneId) {
        super(plugin, WorldBossType.SHADOW_UNSTABLE, zoneId);
    }

    @Override
    protected void useAbility() {
        if (entity == null || !entity.isValid()) return;
        if (isInvisible) return; // Déjà invisible

        Location bossLoc = entity.getLocation();
        World world = bossLoc.getWorld();
        if (world == null) return;

        // Choisir une cible
        List<Player> nearbyPlayers = getNearbyPlayers(30);
        if (nearbyPlayers.isEmpty()) return;

        targetPlayer = nearbyPlayers.get((int) (Math.random() * nearbyPlayers.size()));

        // Devenir invisible
        isInvisible = true;
        entity.setInvisible(true);

        // Effets de disparition
        world.playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        world.playSound(bossLoc, Sound.ENTITY_PHANTOM_AMBIENT, 2f, 0.3f);
        world.spawnParticle(Particle.SMOKE, bossLoc, 50, 1, 1.5, 1, 0.05);
        world.spawnParticle(Particle.LARGE_SMOKE, bossLoc, 20, 0.5, 1, 0.5, 0.02);

        // Avertissement aux joueurs
        for (Player player : getNearbyPlayers(40)) {
            player.sendMessage("§8" + type.getDisplayName() + " §7disparaît dans l'ombre...");
            player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 0.5f, 0.5f);
        }

        // Particules de traque pendant l'invisibilité
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!isInvisible || entity == null || !entity.isValid()) {
                    cancel();
                    return;
                }

                ticks++;

                // Particules subtiles autour du boss invisible
                Location currentLoc = entity.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.SMOKE, currentLoc, 3, 0.3, 0.5, 0.3, 0.01);

                // Son de menace croissante
                if (ticks % 10 == 0) {
                    for (Player player : getNearbyPlayers(30)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT,
                            0.3f + (ticks / 60f) * 0.5f, 0.5f);
                    }
                }

                // Fin de l'invisibilité
                if (ticks >= INVISIBILITY_DURATION_TICKS) {
                    reappear();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Réapparaît derrière la cible
     */
    private void reappear() {
        if (entity == null || !entity.isValid()) return;

        isInvisible = false;
        entity.setInvisible(false);

        World world = entity.getWorld();

        // Téléportation derrière la cible
        if (targetPlayer != null && targetPlayer.isOnline() &&
            targetPlayer.getWorld().equals(world)) {

            // Calculer la position derrière le joueur
            Location playerLoc = targetPlayer.getLocation();
            Vector behindDirection = playerLoc.getDirection().multiply(-1).normalize();
            Location teleportLoc = playerLoc.clone().add(behindDirection.multiply(TELEPORT_DISTANCE));

            // S'assurer que c'est sur un sol solide
            teleportLoc.setY(world.getHighestBlockYAt(teleportLoc) + 1);

            // Téléporter
            entity.teleport(teleportLoc);

            // Effets de réapparition
            world.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.7f);
            world.playSound(teleportLoc, Sound.ENTITY_WITHER_SKELETON_HURT, 1.5f, 0.5f);
            world.spawnParticle(Particle.SMOKE, teleportLoc, 30, 0.5, 1, 0.5, 0.1);
            world.spawnParticle(Particle.SOUL, teleportLoc, 20, 0.3, 0.5, 0.3, 0.05);

            // Avertissement à la cible
            targetPlayer.sendTitle("§8§l⚠ DERRIÈRE VOUS!", "", 5, 20, 10);
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);

            // Appliquer Darkness à la cible
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));

            // Dégâts bonus de surprise
            targetPlayer.damage(10 + zoneId * 0.3, entity);

        } else {
            // Pas de cible valide, réapparaître sur place
            Location loc = entity.getLocation();
            world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
            world.spawnParticle(Particle.SMOKE, loc, 30, 0.5, 1, 0.5, 0.1);
        }

        // Notification
        for (Player player : getNearbyPlayers(40)) {
            player.sendMessage("§8" + type.getDisplayName() + " §7réapparaît!");
        }

        targetPlayer = null;
    }

    @Override
    protected void tick() {
        // Appliquer les effets procéduraux des traits
        super.tick();

        if (entity == null || !entity.isValid()) return;

        // Effets spéciaux quand invisible
        if (isInvisible) {
            // Traînée de fumée subtile
            Location loc = entity.getLocation().add(0, 0.5, 0);
            World world = loc.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.SMOKE, loc, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    @Override
    protected void ambientParticles() {
        if (isInvisible) return; // Pas de particules ambiantes quand invisible
        super.ambientParticles();

        // Particules d'ombre supplémentaires
        if (entity != null && entity.isValid()) {
            Location loc = entity.getLocation();
            World world = loc.getWorld();
            if (world != null && Math.random() < 0.5) {
                // Ombres qui s'échappent
                double angle = Math.random() * Math.PI * 2;
                world.spawnParticle(Particle.SOUL,
                    loc.clone().add(Math.cos(angle), 0.5, Math.sin(angle)),
                    1, 0, 0, 0, 0.02);
            }
        }
    }

    @Override
    protected void updateBossBar() {
        super.updateBossBar();

        // Ajouter l'info d'invisibilité
        if (bossBar != null && isInvisible) {
            String bossName = modifiers != null ? modifiers.getName().titleName() : type.getTitleName();
            bossBar.setTitle(bossName + " §7[§8INVISIBLE§7] - §c" + getFormattedHealth());
        }
    }

    @Override
    public void cleanup() {
        // S'assurer que le boss redevient visible avant le cleanup
        if (entity != null && entity.isValid()) {
            entity.setInvisible(false);
        }
        isInvisible = false;
        targetPlayer = null;
        super.cleanup();
    }
}
