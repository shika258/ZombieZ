package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventManager;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Course Mortelle - Zombies alignes a eliminer rapidement
 *
 * Mecanique:
 * - 10-12 zombies spawn EN LIGNE devant le joueur
 * - Timer demarre au PREMIER kill
 * - Timer s'arrete au DERNIER kill
 * - Record par zone (leaderboard)
 * - Recompenses basees sur le temps
 */
public class DeathRaceEvent extends MicroEvent {

    private final MicroEventManager manager;
    private final List<UUID> raceZombies = new ArrayList<>();
    private final List<Location> zombiePositions = new ArrayList<>();

    private int totalZombies;
    private int zombiesKilled = 0;
    private boolean raceStarted = false;
    private long raceStartTime = 0;
    private long raceEndTime = 0;
    private boolean recordBroken = false;

    // Configuration
    private static final int MIN_ZOMBIES = 8;
    private static final int MAX_ZOMBIES = 12;
    private static final double ZOMBIE_HEALTH = 20.0;
    private static final double ZOMBIE_SPACING = 3.5; // Blocs entre chaque zombie

    public DeathRaceEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone, MicroEventManager manager) {
        super(plugin, MicroEventType.DEATH_RACE, player, location, zone);
        this.manager = manager;

        // Nombre de zombies (plus dans les zones avancees)
        this.totalZombies = Math.min(MAX_ZOMBIES, MIN_ZOMBIES + (zone.getId() / 15));
    }

    @Override
    protected void onStart() {
        // Calculer la direction vers laquelle le joueur regarde
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

        // Spawn les zombies en ligne devant le joueur
        Location startLoc = player.getLocation().add(direction.multiply(8)); // 8 blocs devant

        for (int i = 0; i < totalZombies; i++) {
            // Position le long de la ligne
            Location zombieLoc = startLoc.clone().add(direction.clone().multiply(i * ZOMBIE_SPACING));

            // Ajuster au sol
            zombieLoc.setY(zombieLoc.getWorld().getHighestBlockYAt(zombieLoc) + 1);

            zombiePositions.add(zombieLoc.clone());

            // Spawn le zombie
            Zombie zombie = (Zombie) zombieLoc.getWorld().spawnEntity(zombieLoc, EntityType.ZOMBIE);
            raceZombies.add(zombie.getUniqueId());
            registerEntity(zombie);

            // Configuration - zombies cibles statiques
            zombie.setCustomName("§b§l" + (i + 1) + " §7/ " + totalZombies);
            zombie.setCustomNameVisible(true);
            zombie.setBaby(false);
            zombie.setShouldBurnInDay(false);
            zombie.setAI(false); // Immobiles!

            // Stats faibles (cibles faciles)
            zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(ZOMBIE_HEALTH);
            zombie.setHealth(ZOMBIE_HEALTH);
            zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);

            // Equipement distinctif (cible)
            zombie.getEquipment().setHelmet(new ItemStack(Material.TARGET));
            zombie.getEquipment().setHelmetDropChance(0f);

            // Tags
            zombie.addScoreboardTag("micro_event_entity");
            zombie.addScoreboardTag("death_race");
            zombie.addScoreboardTag("event_" + id);

            // Effet de spawn en sequence
            final int index = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid()) {
                    zombieLoc.getWorld().spawnParticle(Particle.FLASH, zombieLoc.clone().add(0, 1, 0), 1);
                    zombieLoc.getWorld().playSound(zombieLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f + (index * 0.1f));
                }
            }, i * 2L);
        }

        // Afficher le record actuel
        MicroEventManager.DeathRaceRecord record = manager.getDeathRaceRecord(zone.getId());
        if (record != null) {
            player.sendMessage("§b🏃 §7Record actuel: §e" + String.format("%.2f", record.getTimeSeconds()) + "s §7par §b" + record.getPlayerName());
        } else {
            player.sendMessage("§b🏃 §7Aucun record pour cette zone - soyez le premier!");
        }

        // Son de depart
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    @Override
    protected void tick() {
        // Verifier si tous les zombies sont tues
        if (zombiesKilled >= totalZombies) {
            complete();
            return;
        }

        // Afficher les cibles restantes avec particules
        if (elapsedTicks % 10 == 0) {
            for (int i = zombiesKilled; i < raceZombies.size(); i++) {
                UUID zombieId = raceZombies.get(i);
                var entity = plugin.getServer().getEntity(zombieId);
                if (entity != null && !entity.isDead()) {
                    // Particule de cible
                    Location loc = entity.getLocation().add(0, 2, 0);
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 5,
                        0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.AQUA, 1.0f));
                }
            }
        }

        // ActionBar
        String timerText;
        if (raceStarted) {
            double elapsed = (System.currentTimeMillis() - raceStartTime) / 1000.0;
            timerText = "§e" + String.format("%.2f", elapsed) + "s";
        } else {
            timerText = "§7En attente...";
        }

        int remaining = totalZombies - zombiesKilled;
        sendActionBar("§b🏃 Course Mortelle §7| Cibles: §c" + remaining + "/" + totalZombies +
            " §7| Timer: " + timerText + " §7| Limite: §e" + getRemainingTimeSeconds() + "s");
    }

    @Override
    protected void onCleanup() {
        // Supprime les zombies restants avec effet
        for (UUID zombieId : raceZombies) {
            var entity = plugin.getServer().getEntity(zombieId);
            if (entity != null && !entity.isDead()) {
                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    @Override
    protected void distributeRewards() {
        if (!raceStarted || raceEndTime == 0) {
            super.distributeRewards();
            return;
        }

        double raceTime = (raceEndTime - raceStartTime) / 1000.0;

        // Calculer les recompenses basees sur le temps
        int basePoints = type.getBasePointsReward();
        int bonusPoints = 0;

        if (raceTime < 3.0) {
            bonusPoints = 500;
            player.sendMessage("§6§l⚡ INCROYABLE! §e" + String.format("%.2f", raceTime) + "s!");
        } else if (raceTime < 5.0) {
            bonusPoints = 300;
            player.sendMessage("§a§l✓ EXCELLENT! §e" + String.format("%.2f", raceTime) + "s!");
        } else if (raceTime < 8.0) {
            bonusPoints = 150;
            player.sendMessage("§a§l✓ BIEN JOUE! §e" + String.format("%.2f", raceTime) + "s!");
        } else if (raceTime < 12.0) {
            bonusPoints = 50;
            player.sendMessage("§7Pas mal! §e" + String.format("%.2f", raceTime) + "s");
        }

        // Verifier record
        boolean newRecord = manager.updateDeathRaceRecord(zone.getId(), player.getName(), raceTime);
        if (newRecord) {
            recordBroken = true;
            bonusPoints += 500;

            // Annonce serveur
            plugin.getServer().broadcast(
                net.kyori.adventure.text.Component.text(
                    "§b§l🏆 NOUVEAU RECORD! §e" + player.getName() + " §7a battu le record de Course Mortelle " +
                    "en Zone " + zone.getId() + ": §b" + String.format("%.2f", raceTime) + "s!"
                )
            );

            player.sendTitle(
                "§6§l🏆 NOUVEAU RECORD!",
                "§e" + String.format("%.2f", raceTime) + "s §7en Zone " + zone.getId(),
                10, 60, 20
            );

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // Distribuer
        int totalPoints = (int) ((basePoints + bonusPoints) * (1.0 + zone.getId() * 0.05));
        int xp = (int) (type.getBaseXpReward() * (1.0 + zone.getId() * 0.05));

        plugin.getEconomyManager().addPoints(player, totalPoints);
        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (playerData != null) {
            playerData.addXp(xp);
        }

        // Message
        player.sendMessage("");
        player.sendMessage("§a§l✓ COURSE TERMINEE!");
        player.sendMessage("§7Temps: §e" + String.format("%.2f", raceTime) + "s");
        player.sendMessage("§7Recompenses: §e+" + totalPoints + " Points §7| §b+" + xp + " XP");
        if (recordBroken) {
            player.sendMessage("§6§lBonus nouveau record: +500 Points!");
        }
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    @Override
    protected int getBonusPoints() {
        // Gere dans distributeRewards
        return 0;
    }

    @Override
    public boolean handleDamage(LivingEntity entity, Player attacker, double damage) {
        if (raceZombies.contains(entity.getUniqueId())) {
            // Effet de hit
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleDeath(LivingEntity entity, Player killer) {
        int index = -1;
        for (int i = 0; i < raceZombies.size(); i++) {
            if (raceZombies.get(i).equals(entity.getUniqueId())) {
                index = i;
                break;
            }
        }

        if (index == -1) return false;

        // Premier kill = demarrer le timer
        if (!raceStarted) {
            raceStarted = true;
            raceStartTime = System.currentTimeMillis();
            player.sendMessage("§b🏃 §a§lGO! §7Timer demarre!");
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        }

        zombiesKilled++;

        // Effet de mort
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0, 1, 0), 1);

        // Son progressif
        float pitch = 0.5f + (zombiesKilled / (float) totalZombies) * 1.5f;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, pitch);

        // Dernier kill = fin du timer
        if (zombiesKilled >= totalZombies) {
            raceEndTime = System.currentTimeMillis();

            double raceTime = (raceEndTime - raceStartTime) / 1000.0;
            player.sendMessage("§b🏃 §7Temps final: §e§l" + String.format("%.2f", raceTime) + "s");

            // Effet de fin
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        }

        return true;
    }
}
