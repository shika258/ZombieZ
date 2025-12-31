package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Capacité active Marque du Prédateur - marque une cible pour des dégâts bonus
 */
@Getter
public class PredatorMarkActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int cooldown;
    private final int durationSeconds;            // 8 secondes
    private final double damageBonus;             // +50% dégâts reçus

    // Tracking des cibles marquées (targetUUID -> endTime)
    private final Map<UUID, Map<UUID, Long>> markedTargets = new HashMap<>();

    public PredatorMarkActive(String id, String name, String desc, int cd, int duration, double bonus) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.cooldown = cd;
        this.durationSeconds = duration;
        this.damageBonus = bonus;
        PassiveAbilityCleanup.registerForCleanup(markedTargets);
    }

    @Override
    public boolean isPassive() { return false; }

    @Override
    public int getCooldown() { return cooldown; }

    @Override
    public boolean activate(Player player, PetData petData) {
        World world = player.getWorld();
        UUID playerUUID = player.getUniqueId();

        // Trouver la cible (ennemi le plus proche dans la direction du regard)
        Monster target = findTarget(player);

        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucun ennemi ciblé!");
            return false;
        }

        // Calculer la durée ajustée
        int adjustedDuration = durationSeconds + (int)((petData.getStatMultiplier() - 1) * 2);
        double adjustedBonus = damageBonus + (petData.getStatMultiplier() - 1) * 0.10;
        long endTime = System.currentTimeMillis() + (adjustedDuration * 1000L);

        // Marquer la cible
        markedTargets.computeIfAbsent(playerUUID, k -> new HashMap<>());
        markedTargets.get(playerUUID).put(target.getUniqueId(), endTime);

        // Message et sons
        player.sendMessage("§a[Pet] §c§l MARQUE DU PRÉDATEUR! §r§7Cible marquée pendant §e" + adjustedDuration + "s");
        player.sendMessage("§7   > §c+" + (int)(adjustedBonus * 100) + "% §7dégâts reçus, §e100% §7chance de crit");

        world.playSound(target.getLocation(), Sound.ENTITY_CAT_HISS, 1.0f, 0.5f);
        world.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 2.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 1.5f);

        // Animation du chat pet
        animateCatMark(player, target);

        // Effets visuels sur la cible marquée
        startMarkVisuals(player, target, adjustedDuration);

        return true;
    }

    private Monster findTarget(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        Monster bestTarget = null;
        double bestScore = -1;

        for (Entity entity : player.getNearbyEntities(15, 8, 15)) {
            if (!(entity instanceof Monster m)) continue;
            if (!m.isValid() || m.isDead()) continue;

            Vector toEntity = entity.getLocation().toVector()
                .subtract(player.getLocation().toVector());
            double distance = toEntity.length();
            toEntity.normalize();

            // Score basé sur l'alignement avec le regard et la distance
            double dot = direction.dot(toEntity);
            if (dot < 0.5) continue; // Doit être à peu près devant

            double score = dot / (distance * 0.1 + 1); // Plus proche et aligné = meilleur score
            if (score > bestScore) {
                bestScore = score;
                bestTarget = m;
            }
        }

        return bestTarget;
    }

    private void animateCatMark(Player player, Monster target) {
        UUID uuid = player.getUniqueId();
        String ownerTag = "pet_owner_" + uuid;

        for (Entity entity : player.getNearbyEntities(30, 15, 30)) {
            if (entity instanceof Cat cat && entity.getScoreboardTags().contains(ownerTag)) {
                Location catLoc = cat.getLocation();
                World world = cat.getWorld();

                // Particules d'ombre autour du chat
                world.spawnParticle(Particle.SMOKE, catLoc.add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.02);
                world.spawnParticle(Particle.WITCH, catLoc, 10, 0.2, 0.2, 0.2, 0.01);

                // Ligne de particules du chat vers la cible
                Vector dir = target.getLocation().toVector().subtract(catLoc.toVector());
                double dist = dir.length();
                dir.normalize();

                for (double d = 0; d < dist; d += 0.5) {
                    Location point = catLoc.clone().add(dir.clone().multiply(d));
                    Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(50, 0, 50), 0.6f);
                    world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, dust);
                }

                break;
            }
        }
    }

    private void startMarkVisuals(Player player, Monster target, int durationSeconds) {
        World world = target.getWorld();
        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Vérifier si la marque est toujours active
                if (!isMarked(playerUUID, targetUUID) || !target.isValid() || target.isDead()) {
                    // Fin de la marque
                    if (target.isValid()) {
                        Location loc = target.getLocation().add(0, 1, 0);
                        world.spawnParticle(Particle.SMOKE, loc, 20, 0.4, 0.4, 0.4, 0.02);
                        world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.5f);
                    }

                    // Nettoyer
                    Map<UUID, Long> playerMarks = markedTargets.get(playerUUID);
                    if (playerMarks != null) {
                        playerMarks.remove(targetUUID);
                    }

                    if (player.isOnline()) {
                        player.sendMessage("§a[Pet] §7La marque s'estompe...");
                    }
                    cancel();
                    return;
                }

                Location loc = target.getLocation();

                // Particules de marque au-dessus de la tête
                if (ticks % 5 == 0) {
                    Location markLoc = loc.clone().add(0, target.getHeight() + 0.5, 0);

                    // Symbole de cible tournant
                    double angle = ticks * 0.15;
                    for (int i = 0; i < 4; i++) {
                        double a = angle + i * (Math.PI / 2);
                        double radius = 0.4;
                        Location pointLoc = markLoc.clone().add(
                            Math.cos(a) * radius,
                            0,
                            Math.sin(a) * radius
                        );
                        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f);
                        world.spawnParticle(Particle.DUST, pointLoc, 1, 0, 0, 0, dust);
                    }

                    // Point central
                    world.spawnParticle(Particle.CRIT, markLoc, 1, 0, 0, 0, 0);
                }

                // Aura d'ombre autour de la cible
                if (ticks % 10 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = (ticks * 0.1) + i * (Math.PI / 4);
                        Location auraLoc = loc.clone().add(
                            Math.cos(a) * 0.8,
                            0.1 + Math.sin(ticks * 0.2) * 0.2,
                            Math.sin(a) * 0.8
                        );
                        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(30, 0, 30), 0.5f);
                        world.spawnParticle(Particle.DUST, auraLoc, 1, 0, 0, 0, dust);
                    }
                }

                // Son périodique
                if (ticks % 40 == 0) {
                    world.playSound(loc, Sound.ENTITY_CAT_AMBIENT, 0.3f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Vérifie si une cible est marquée par un joueur
     */
    public boolean isMarked(UUID playerUUID, UUID targetUUID) {
        Map<UUID, Long> playerMarks = markedTargets.get(playerUUID);
        if (playerMarks == null) return false;

        Long endTime = playerMarks.get(targetUUID);
        if (endTime == null) return false;

        return System.currentTimeMillis() < endTime;
    }

    /**
     * Obtient le bonus de dégâts pour une cible marquée
     */
    public double getMarkBonus(UUID playerUUID, UUID targetUUID, double baseBonus) {
        if (isMarked(playerUUID, targetUUID)) {
            return baseBonus;
        }
        return 0;
    }

    /**
     * Vérifie si l'attaque sur une cible marquée doit être un crit
     */
    public boolean shouldForceCrit(UUID playerUUID, UUID targetUUID) {
        return isMarked(playerUUID, targetUUID);
    }
}
