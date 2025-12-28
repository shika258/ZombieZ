package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creaking;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * IA originale pour le boss Creaking (Gardien de l'Arbre Maudit)
 * Utilise l'entité Creaking (Grinceur) de Minecraft 1.21+
 *
 * Mécaniques:
 * - ROOT SLAM: Frappe le sol, faisant surgir des racines qui endommagent les joueurs
 * - BRANCH SWEEP: Balayage en arc infligeant des dégâts et repoussant
 * - SPLINTER BARRAGE: Lance des éclats de bois vers les joueurs
 * - CREAKING PRESENCE: Se téléporte derrière le joueur s'il lui tourne le dos trop longtemps
 * - NATURE'S WRATH (Phase 2): Invocation de lianes restrictives + attaques plus rapides
 */
public class CreakingBossAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private BossBar bossBar;
    private final Set<UUID> playersInFight = new HashSet<>();
    private final Map<UUID, Integer> playerBackTurnedTicks = new HashMap<>();

    // Position de spawn pour le leash
    private final Location spawnLocation;
    private static final double LEASH_RANGE_SQUARED = 40 * 40;

    // Cooldowns des attaques
    private long lastRootSlam = 0;
    private long lastBranchSweep = 0;
    private long lastSplinterBarrage = 0;
    private long lastTeleport = 0;

    private static final long ROOT_SLAM_COOLDOWN = 8000;      // 8 secondes
    private static final long BRANCH_SWEEP_COOLDOWN = 6000;   // 6 secondes
    private static final long SPLINTER_COOLDOWN = 4000;       // 4 secondes
    private static final long TELEPORT_COOLDOWN = 12000;      // 12 secondes
    private static final int BACK_TURNED_THRESHOLD = 60;      // 3 secondes (60 ticks)

    // État d'attaque
    private boolean isAttacking = false;
    private String currentAttack = "";

    public CreakingBossAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 3000;
        this.spawnLocation = entity.getLocation().clone();

        setupBossBar();
        applyBossBuffs();
    }

    private void setupBossBar() {
        String title = "§4§l" + zombieType.getDisplayName() + " §7[Phase " + phase + "/2]";
        bossBar = plugin.getServer().createBossBar(title, BarColor.GREEN, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void applyBossBuffs() {
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.9);
        }

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        if (!zombie.isValid() || zombie.isDead()) return;

        tickCounter++;

        updateBossBar();
        updatePlayersInFight();
        checkPhaseTransition();
        checkLeash();

        // Particules d'aura constantes - aspect boisé/naturel corrompu
        if (tickCounter % 8 == 0) {
            playParticles(Particle.SPORE_BLOSSOM_AIR, zombie.getLocation().add(0, 1.5, 0), 3, 0.5, 0.8, 0.5);
            playParticles(Particle.ASH, zombie.getLocation().add(0, 1, 0), 5, 0.8, 1, 0.8);
        }

        // Ne pas attaquer pendant une animation
        if (isAttacking) return;

        Player target = findNearestPlayer(30);
        if (target == null) return;

        // Vérifier si le joueur tourne le dos
        checkPlayerBackTurned(target);

        // Choisir l'attaque selon les cooldowns et la distance
        double distance = zombie.getLocation().distance(target.getLocation());
        long now = System.currentTimeMillis();

        // Priorité aux attaques selon la distance et les cooldowns
        if (distance <= 4 && now - lastBranchSweep >= getBranchSweepCooldown()) {
            branchSweepAttack();
        } else if (distance <= 8 && now - lastRootSlam >= getRootSlamCooldown()) {
            rootSlamAttack();
        } else if (distance > 5 && distance <= 20 && now - lastSplinterBarrage >= getSplinterCooldown()) {
            splinterBarrageAttack(target);
        }
    }

    /**
     * Vérifie si le joueur tourne le dos au boss et déclenche une téléportation
     */
    private void checkPlayerBackTurned(Player player) {
        Vector toPlayer = player.getLocation().toVector().subtract(zombie.getLocation().toVector()).normalize();
        Vector playerLookDir = player.getLocation().getDirection().normalize();

        // Dot product < 0 signifie que le joueur tourne le dos
        double dot = toPlayer.dot(playerLookDir);

        if (dot > 0.3) { // Joueur tourne le dos
            int ticks = playerBackTurnedTicks.getOrDefault(player.getUniqueId(), 0) + 1;
            playerBackTurnedTicks.put(player.getUniqueId(), ticks);

            long now = System.currentTimeMillis();
            if (ticks >= BACK_TURNED_THRESHOLD && now - lastTeleport >= TELEPORT_COOLDOWN) {
                creakingTeleport(player);
                playerBackTurnedTicks.put(player.getUniqueId(), 0);
            }
        } else {
            playerBackTurnedTicks.put(player.getUniqueId(), 0);
        }
    }

    /**
     * ROOT SLAM - Frappe le sol, des racines surgissent en ligne
     */
    private void rootSlamAttack() {
        isAttacking = true;
        currentAttack = "ROOT_SLAM";
        lastRootSlam = System.currentTimeMillis();

        // Animation de charge
        playSound(Sound.ENTITY_RAVAGER_STUNNED, 1.5f, 0.5f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4, false, false));

        // Exécution de l'attaque après un délai
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!zombie.isValid() || zombie.isDead()) {
                    cancel();
                    isAttacking = false;
                    return;
                }

                tick++;

                if (tick == 15) { // Impact après 0.75s
                    playSound(Sound.ENTITY_IRON_GOLEM_DAMAGE, 2f, 0.3f);
                    playSound(Sound.BLOCK_GRASS_BREAK, 2f, 0.5f);

                    // Racines qui surgissent en cercle
                    Location center = zombie.getLocation();
                    for (int i = 0; i < 8; i++) {
                        double angle = (2 * Math.PI / 8) * i;
                        for (int dist = 2; dist <= 6; dist++) {
                            Location rootLoc = center.clone().add(
                                Math.cos(angle) * dist,
                                0,
                                Math.sin(angle) * dist
                            );

                            // Ajuster Y au sol
                            rootLoc = getGroundLocation(rootLoc);
                            if (rootLoc == null) continue;

                            final Location finalLoc = rootLoc;
                            final int delay = (dist - 2) * 2;

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // Particules de racine
                                    finalLoc.getWorld().spawnParticle(Particle.BLOCK, finalLoc.clone().add(0, 0.5, 0),
                                        15, 0.2, 0.5, 0.2, Material.DARK_OAK_LOG.createBlockData());
                                    finalLoc.getWorld().spawnParticle(Particle.BLOCK, finalLoc.clone().add(0, 0.5, 0),
                                        10, 0.2, 0.5, 0.2, Material.ROOTED_DIRT.createBlockData());

                                    // Dégâts aux joueurs
                                    for (Entity e : finalLoc.getWorld().getNearbyEntities(finalLoc, 1.5, 2, 1.5)) {
                                        if (e instanceof Player p) {
                                            double damage = phase == 1 ? 10 : 14;
                                            p.damage(damage, zombie);
                                            p.setVelocity(new Vector(0, 0.6, 0)); // Pop up
                                        }
                                    }
                                }
                            }.runTaskLater(plugin, delay);
                        }
                    }
                }

                if (tick >= 25) {
                    cancel();
                    isAttacking = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * BRANCH SWEEP - Balayage de branche en arc
     */
    private void branchSweepAttack() {
        isAttacking = true;
        currentAttack = "BRANCH_SWEEP";
        lastBranchSweep = System.currentTimeMillis();

        playSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);

        new BukkitRunnable() {
            int tick = 0;
            double startAngle = Math.atan2(
                zombie.getLocation().getDirection().getZ(),
                zombie.getLocation().getDirection().getX()
            );

            @Override
            public void run() {
                if (!zombie.isValid() || zombie.isDead()) {
                    cancel();
                    isAttacking = false;
                    return;
                }

                tick++;

                // Balayage sur 180 degrés
                if (tick <= 10) {
                    double sweepAngle = startAngle - Math.PI/2 + (Math.PI * tick / 10);

                    for (int dist = 2; dist <= 5; dist++) {
                        Location sweepLoc = zombie.getLocation().add(
                            Math.cos(sweepAngle) * dist,
                            1,
                            Math.sin(sweepAngle) * dist
                        );

                        zombie.getWorld().spawnParticle(Particle.SWEEP_ATTACK, sweepLoc, 1, 0, 0, 0);
                        zombie.getWorld().spawnParticle(Particle.BLOCK, sweepLoc, 5, 0.2, 0.2, 0.2,
                            Material.PALE_OAK_WOOD.createBlockData());

                        // Dégâts et knockback
                        for (Entity e : sweepLoc.getWorld().getNearbyEntities(sweepLoc, 1.5, 2, 1.5)) {
                            if (e instanceof Player p) {
                                double damage = phase == 1 ? 8 : 12;
                                p.damage(damage, zombie);

                                Vector knockback = p.getLocation().toVector()
                                    .subtract(zombie.getLocation().toVector())
                                    .normalize().multiply(1.2).setY(0.4);
                                p.setVelocity(knockback);
                            }
                        }
                    }

                    if (tick == 5) {
                        playSound(Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.7f);
                    }
                }

                if (tick >= 15) {
                    cancel();
                    isAttacking = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * SPLINTER BARRAGE - Lance des éclats de bois vers le joueur
     */
    private void splinterBarrageAttack(Player target) {
        isAttacking = true;
        currentAttack = "SPLINTER";
        lastSplinterBarrage = System.currentTimeMillis();

        playSound(Sound.ENTITY_SHULKER_SHOOT, 1.5f, 0.5f);

        int projectileCount = phase == 1 ? 5 : 8;

        new BukkitRunnable() {
            int shot = 0;

            @Override
            public void run() {
                if (!zombie.isValid() || zombie.isDead() || !target.isOnline()) {
                    cancel();
                    isAttacking = false;
                    return;
                }

                if (shot < projectileCount) {
                    // Tirer un éclat
                    Location start = zombie.getEyeLocation();
                    Vector direction = target.getEyeLocation().toVector()
                        .subtract(start.toVector()).normalize();

                    // Légère dispersion
                    direction.add(new Vector(
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.1,
                        (random.nextDouble() - 0.5) * 0.2
                    )).normalize();

                    launchSplinter(start, direction);
                    playSound(Sound.BLOCK_WOOD_BREAK, 1f, 1.5f);
                    shot++;
                } else {
                    cancel();
                    isAttacking = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    /**
     * Lance un éclat de bois
     */
    private void launchSplinter(Location start, Vector direction) {
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 40) { // Max 2 secondes
                    cancel();
                    return;
                }

                // Avancer le projectile
                current.add(direction.clone().multiply(1.2));

                // Particules
                current.getWorld().spawnParticle(Particle.BLOCK, current, 3, 0.1, 0.1, 0.1,
                    Material.PALE_OAK_PLANKS.createBlockData());

                // Vérifier collision avec bloc solide
                if (current.getBlock().getType().isSolid()) {
                    current.getWorld().spawnParticle(Particle.BLOCK, current, 10, 0.2, 0.2, 0.2,
                        Material.PALE_OAK_PLANKS.createBlockData());
                    cancel();
                    return;
                }

                // Vérifier collision avec joueur
                for (Entity e : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                    if (e instanceof Player p) {
                        double damage = phase == 1 ? 6 : 9;
                        p.damage(damage, zombie);
                        p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0, 1, 0),
                            15, 0.3, 0.3, 0.3, Material.PALE_OAK_PLANKS.createBlockData());
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * CREAKING TELEPORT - Se téléporte derrière le joueur qui lui tourne le dos
     */
    private void creakingTeleport(Player target) {
        lastTeleport = System.currentTimeMillis();

        // Effet de disparition
        playSound(Sound.BLOCK_WOOD_BREAK, 2f, 0.3f);
        playParticles(Particle.BLOCK, zombie.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5,
            Material.PALE_OAK_LOG.createBlockData());

        // Calculer position derrière le joueur
        Vector behind = target.getLocation().getDirection().normalize().multiply(-2);
        Location teleportLoc = target.getLocation().add(behind);
        teleportLoc = getGroundLocation(teleportLoc);

        if (teleportLoc != null && teleportLoc.distanceSquared(spawnLocation) <= LEASH_RANGE_SQUARED) {
            // Téléportation
            zombie.teleport(teleportLoc);

            // Faire face au joueur
            zombie.setRotation(
                (float) Math.toDegrees(Math.atan2(
                    target.getLocation().getZ() - teleportLoc.getZ(),
                    target.getLocation().getX() - teleportLoc.getX()
                )) - 90,
                0
            );

            // Effet d'apparition
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            playSound(Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.5f);
            playParticles(Particle.BLOCK, teleportLoc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5,
                Material.PALE_OAK_LOG.createBlockData());

            // Avertissement au joueur
            target.sendTitle("", "§c§l⚠ DERRIÈRE TOI ⚠", 0, 20, 10);
            target.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        }
    }

    /**
     * Transition de phase
     */
    private void checkPhaseTransition() {
        if (phase == 1 && isHealthBelow(0.5)) {
            phase = 2;
            onPhaseChange();
        }
    }

    private void onPhaseChange() {
        isAttacking = true;

        playSound(Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);

        // Animation de transformation
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;

                // Particules de transformation
                playParticles(Particle.BLOCK, zombie.getLocation().add(0, 1.5, 0), 20, 1, 1.5, 1,
                    Material.SCULK.createBlockData());
                playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation().add(0, 1, 0), 10, 0.8, 1, 0.8);

                if (tick == 40) {
                    // Explosion de phase
                    playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 0, 0, 0);

                    for (UUID uuid : playersInFight) {
                        Player p = plugin.getServer().getPlayer(uuid);
                        if (p != null) {
                            p.sendTitle("§4§lPHASE 2", "§7L'Arbre libère sa fureur!", 10, 40, 10);
                        }
                    }

                    // Buffs de phase 2
                    var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
                    if (damage != null) {
                        damage.setBaseValue(damage.getBaseValue() * 1.3);
                    }

                    var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (speed != null) {
                        speed.setBaseValue(speed.getBaseValue() * 1.2);
                    }

                    // Regen partielle
                    heal(zombie.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.15);

                    // Immunité temporaire
                    zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false));

                    // Changer couleur bossbar
                    bossBar.setColor(BarColor.PURPLE);

                    cancel();
                    isAttacking = false;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // === COOLDOWNS SELON PHASE ===

    private long getRootSlamCooldown() {
        return phase == 1 ? ROOT_SLAM_COOLDOWN : (long)(ROOT_SLAM_COOLDOWN * 0.7);
    }

    private long getBranchSweepCooldown() {
        return phase == 1 ? BRANCH_SWEEP_COOLDOWN : (long)(BRANCH_SWEEP_COOLDOWN * 0.7);
    }

    private long getSplinterCooldown() {
        return phase == 1 ? SPLINTER_COOLDOWN : (long)(SPLINTER_COOLDOWN * 0.6);
    }

    // === UTILITY ===

    private Location getGroundLocation(Location loc) {
        for (int y = (int) loc.getY() + 3; y > zombie.getWorld().getMinHeight(); y--) {
            Location checkLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
            if (checkLoc.getBlock().getType().isSolid()) {
                return checkLoc.add(0, 1, 0);
            }
        }
        return null;
    }

    private void checkLeash() {
        if (tickCounter % 20 != 0) return;

        if (zombie.getLocation().distanceSquared(spawnLocation) > LEASH_RANGE_SQUARED) {
            zombie.teleport(spawnLocation);
            zombie.setTarget(null);
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            playParticles(Particle.BLOCK, spawnLocation.clone().add(0, 1, 0), 30, 0.5, 1, 0.5,
                Material.PALE_OAK_LOG.createBlockData());
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;

        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double progress = zombie.getHealth() / maxHealth.getValue();
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));
            bossBar.setTitle("§4§l" + zombieType.getDisplayName() + " §7[Phase " + phase + "/2]");
        }
    }

    private void updatePlayersInFight() {
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 40, 20, 40).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    if (!playersInFight.contains(p.getUniqueId())) {
                        playersInFight.add(p.getUniqueId());
                        bossBar.addPlayer(p);
                    }
                });

        playersInFight.removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null || !p.isOnline() || p.getLocation().distance(zombie.getLocation()) > 50) {
                if (p != null) bossBar.removePlayer(p);
                playerBackTurnedTicks.remove(uuid);
                return true;
            }
            return false;
        });
    }

    protected boolean isHealthBelow(double percent) {
        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return false;
        return zombie.getHealth() / maxHealth.getValue() < percent;
    }

    protected void heal(double amount) {
        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            zombie.setHealth(Math.min(maxHealth.getValue(), zombie.getHealth() + amount));
        }
    }

    protected void playParticles(Particle particle, Location loc, int count, double dx, double dy, double dz) {
        zombie.getWorld().spawnParticle(particle, loc, count, dx, dy, dz);
    }

    protected void playParticles(Particle particle, Location loc, int count, double dx, double dy, double dz, Object data) {
        zombie.getWorld().spawnParticle(particle, loc, count, dx, dy, dz, data);
    }

    protected void playSound(Sound sound, float volume, float pitch) {
        zombie.getWorld().playSound(zombie.getLocation(), sound, volume, pitch);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Contre-attaque si touché en phase 2 (20% de chance)
        if (phase == 2 && attacker instanceof Player player && random.nextFloat() < 0.20f) {
            player.damage(4, zombie);
            playParticles(Particle.BLOCK, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3,
                Material.PALE_OAK_PLANKS.createBlockData());
            playSound(Sound.BLOCK_WOOD_BREAK, 1f, 1.2f);
        }
    }

    @Override
    public void onDeath(Player killer) {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        // Effets de mort spectaculaires
        playSound(Sound.ENTITY_WITHER_DEATH, 1.5f, 0.8f);
        playSound(Sound.ENTITY_IRON_GOLEM_DEATH, 2f, 0.5f);

        // Explosion de bois
        Location deathLoc = zombie.getLocation();
        deathLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 5, 1, 1, 1);
        deathLoc.getWorld().spawnParticle(Particle.BLOCK, deathLoc.add(0, 1.5, 0), 100, 2, 2, 2,
            Material.PALE_OAK_LOG.createBlockData());
        deathLoc.getWorld().spawnParticle(Particle.SOUL, deathLoc, 50, 2, 2, 2);
    }

    public Set<UUID> getPlayersInFight() {
        return new HashSet<>(playersInFight);
    }
}
