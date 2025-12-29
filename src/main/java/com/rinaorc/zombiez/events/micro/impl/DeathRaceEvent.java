package com.rinaorc.zombiez.events.micro.impl;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.events.micro.MicroEvent;
import com.rinaorc.zombiez.events.micro.MicroEventManager;
import com.rinaorc.zombiez.events.micro.MicroEventType;
import com.rinaorc.zombiez.zones.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

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

    private int totalZombies;
    private int zombiesKilled = 0;
    private boolean raceStarted = false;
    private long raceStartTime = 0;
    private long raceEndTime = 0;
    private boolean recordBroken = false;

    // TextDisplay flottant pour l'affichage
    private TextDisplay titleDisplay;

    // Configuration
    private static final int MIN_ZOMBIES = 7;
    private static final int MAX_ZOMBIES = 11;
    private static final double ZOMBIE_HEALTH = 20.0;
    private static final double SPAWN_RADIUS = 48.0; // Rayon de spawn autour du joueur
    private static final float TEXT_SCALE = 1.5f;
    private static final int MAX_SPAWN_ATTEMPTS = 15; // Tentatives max par zombie

    public DeathRaceEvent(ZombieZPlugin plugin, Player player, Location location, Zone zone, MicroEventManager manager) {
        super(plugin, MicroEventType.DEATH_RACE, player, location, zone);
        this.manager = manager;

        // Nombre de zombies (plus dans les zones avancees)
        this.totalZombies = Math.min(MAX_ZOMBIES, MIN_ZOMBIES + (zone.getId() / 15));
    }

    @Override
    protected void onStart() {
        // Creer le TextDisplay flottant au-dessus du joueur
        Location displayLoc = player.getLocation().add(0, 3.5, 0);
        titleDisplay = displayLoc.getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(180, 0, 0, 0));
            display.setTransformation(new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                new org.joml.Quaternionf(),
                new Vector3f(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE),
                new org.joml.Quaternionf()
            ));
            display.text(Component.text("🏃 COURSE MORTELLE 🏃", NamedTextColor.AQUA, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text("Cibles: ", NamedTextColor.GRAY))
                .append(Component.text(totalZombies + " zombies", NamedTextColor.RED)));
        });
        registerEntity(titleDisplay);

        // Spawn les zombies aleatoirement dans un rayon de 48 blocs autour du joueur
        Location playerLoc = player.getLocation();
        java.util.Random random = new java.util.Random();
        int spawnedCount = 0;

        for (int i = 0; i < totalZombies; i++) {
            Location zombieLoc = findValidSpawnLocation(playerLoc, random);

            // Si aucune position valide trouvee, skip ce zombie
            if (zombieLoc == null) {
                continue;
            }

            // Spawn le zombie
            Zombie zombie = (Zombie) zombieLoc.getWorld().spawnEntity(zombieLoc, EntityType.ZOMBIE);
            raceZombies.add(zombie.getUniqueId());
            registerEntity(zombie);
            spawnedCount++;

            // Configuration - zombies cibles statiques avec GLOWING
            zombie.setCustomName("§b§l🎯 CIBLE");
            zombie.setCustomNameVisible(true);
            zombie.setBaby(false);
            zombie.setShouldBurnInDay(false);
            zombie.setAI(false); // Immobiles!
            zombie.setGlowing(true); // EFFET GLOWING pour visibilité

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
            final int index = spawnedCount - 1;
            final Location spawnLoc = zombieLoc.clone();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (zombie.isValid()) {
                    spawnLoc.getWorld().spawnParticle(Particle.FLASH, spawnLoc.clone().add(0, 1, 0), 1);
                    spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f + (index * 0.1f));
                }
            }, index * 2L);
        }

        // Mettre a jour le nombre reel de zombies spawnes
        this.totalZombies = spawnedCount;

        // Verifier qu'au moins quelques zombies ont spawn
        if (spawnedCount < 3) {
            player.sendMessage("§c§l✗ §7Impossible de lancer la course - terrain inadapte!");
            fail();
            return;
        }

        // Mettre a jour le TextDisplay avec le nombre reel
        if (titleDisplay != null && titleDisplay.isValid()) {
            titleDisplay.text(Component.text("🏃 COURSE MORTELLE 🏃", NamedTextColor.AQUA, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text("Cibles: ", NamedTextColor.GRAY))
                .append(Component.text(totalZombies + " zombies", NamedTextColor.RED)));
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

        // Mettre a jour le TextDisplay (suit le joueur et affiche le timer)
        if (titleDisplay != null && titleDisplay.isValid()) {
            // Suivre le joueur
            titleDisplay.teleport(player.getLocation().add(0, 3.5, 0));

            // Mettre a jour le texte
            int remaining = totalZombies - zombiesKilled;
            Component timerComponent;

            if (raceStarted) {
                double elapsed = (System.currentTimeMillis() - raceStartTime) / 1000.0;
                timerComponent = Component.text(String.format("%.2f", elapsed) + "s", NamedTextColor.YELLOW, TextDecoration.BOLD);
            } else {
                timerComponent = Component.text("En attente...", NamedTextColor.GRAY);
            }

            titleDisplay.text(
                Component.text("🏃 COURSE MORTELLE 🏃", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .appendNewline()
                    .append(Component.text("Cibles: ", NamedTextColor.GRAY))
                    .append(Component.text(remaining + "/" + totalZombies, NamedTextColor.RED))
                    .append(Component.text(" | Timer: ", NamedTextColor.DARK_GRAY))
                    .append(timerComponent)
            );
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

        // ActionBar (en complement du TextDisplay)
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
        // Nettoyer le TextDisplay
        if (titleDisplay != null && titleDisplay.isValid()) {
            titleDisplay.remove();
        }
        titleDisplay = null;

        // Supprime les zombies restants avec effet visuel
        for (UUID zombieId : raceZombies) {
            var entity = plugin.getServer().getEntity(zombieId);
            if (entity != null && !entity.isDead()) {
                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 10, 0.3, 0.5, 0.3, 0.02);
                entity.remove(); // Forcer la suppression
            }
        }
        raceZombies.clear(); // Nettoyer la liste pour eviter les fuites de memoire
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

        // Distribuer via EconomyManager pour inclure l'XP de classe (30%)
        int totalPoints = (int) ((basePoints + bonusPoints) * (1.0 + zone.getId() * 0.05));
        int xp = (int) (type.getBaseXpReward() * (1.0 + zone.getId() * 0.05));

        plugin.getEconomyManager().addPoints(player, totalPoints);
        plugin.getEconomyManager().addXp(player, xp);

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
            // Effet de hit visuel
            entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            // Retourner false pour laisser les dégâts vanilla s'appliquer
            // Les cibles de Death Race sont des zombies simples (20 HP)
            // et cela permet à entity.getKiller() de fonctionner pour handleDeath
            return false;
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

    /**
     * Trouve une position de spawn valide pour un zombie cible
     * Effectue plusieurs tentatives avec des positions aleatoires
     *
     * @param playerLoc Position du joueur (centre du rayon de spawn)
     * @param random Generateur aleatoire
     * @return Position valide ou null si aucune trouvee
     */
    private Location findValidSpawnLocation(Location playerLoc, java.util.Random random) {
        org.bukkit.World world = playerLoc.getWorld();
        if (world == null) return null;

        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            // Position aleatoire dans le rayon de spawn
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 10 + random.nextDouble() * (SPAWN_RADIUS - 10); // Min 10 blocs, max 48 blocs

            double x = playerLoc.getX() + Math.cos(angle) * distance;
            double z = playerLoc.getZ() + Math.sin(angle) * distance;

            // Trouver le bloc solide le plus haut
            int highestY = world.getHighestBlockYAt((int) x, (int) z);

            // Eviter les spawns trop hauts (toits, arbres) ou trop bas
            if (highestY > playerLoc.getY() + 10 || highestY < playerLoc.getY() - 20) {
                continue;
            }

            Location spawnLoc = new Location(world, x, highestY + 1, z);

            // Verifier que le sol est solide
            org.bukkit.block.Block groundBlock = spawnLoc.clone().add(0, -1, 0).getBlock();
            if (!groundBlock.getType().isSolid()) {
                continue;
            }

            // Verifier que l'espace au-dessus est libre (2 blocs pour le zombie)
            org.bukkit.block.Block feetBlock = spawnLoc.getBlock();
            org.bukkit.block.Block headBlock = spawnLoc.clone().add(0, 1, 0).getBlock();
            if (feetBlock.getType().isSolid() || headBlock.getType().isSolid()) {
                continue;
            }

            // Eviter l'eau et la lave
            if (groundBlock.isLiquid() || feetBlock.isLiquid()) {
                continue;
            }

            // Position valide trouvee
            return spawnLoc;
        }

        // Aucune position valide trouvee apres MAX_SPAWN_ATTEMPTS tentatives
        return null;
    }
}
