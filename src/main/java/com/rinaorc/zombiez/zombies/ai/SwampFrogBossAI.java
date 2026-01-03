package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IA pour le boss Grenouille Géante du Marais (Chapitre 5 Étape 10)
 *
 * Capacités:
 * - Langue Venimeuse: Attire un joueur vers le boss et applique poison
 * - Saut Écrasant: Saute haut et atterrit sur les joueurs avec dégâts AoE
 * - Crachat Toxique: Lance un projectile de poison
 * - Invocation: Fait spawn des mini grenouilles mutantes
 */
public class SwampFrogBossAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private BossBar bossBar;
    private final Set<UUID> playersInFight = new HashSet<>();

    // Position de spawn pour le leash
    private final Location spawnLocation;
    private static final double LEASH_RANGE_SQUARED = 35 * 35;

    // Cooldowns
    private long lastTongueAttack = 0;
    private long lastLeapAttack = 0;
    private long lastToxicSpit = 0;
    private long lastMinionSpawn = 0;

    private static final long TONGUE_COOLDOWN = 8000;   // 8 secondes
    private static final long LEAP_COOLDOWN = 12000;    // 12 secondes
    private static final long SPIT_COOLDOWN = 5000;     // 5 secondes
    private static final long MINION_COOLDOWN = 15000;  // 15 secondes

    // État du saut
    private boolean isLeaping = false;
    private Location leapTarget = null;

    public SwampFrogBossAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 3000;
        this.spawnLocation = entity.getLocation().clone();

        setupBossBar();
        applyBossBuffs();
    }

    private void setupBossBar() {
        String title = "§2§l🐸 " + zombieType.getDisplayName() + " §7[Phase " + phase + "/2]";
        bossBar = plugin.getServer().createBossBar(title, BarColor.GREEN, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void applyBossBuffs() {
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.9);
        }

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        tickCounter++;

        updateBossBar();
        updatePlayersInFight();
        checkPhaseTransition();
        checkLeash();

        // Particules d'aura toxique
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SLIME, zombie.getLocation().add(0, 0.5, 0), 8, 1, 0.5, 1);
            if (phase == 2) {
                // Plus de particules toxiques en phase 2
                zombie.getWorld().spawnParticle(Particle.DUST, zombie.getLocation().add(0, 1, 0),
                    5, 0.8, 0.5, 0.8, 0, new Particle.DustOptions(Color.fromRGB(50, 150, 50), 1.5f));
            }
        }

        // Gestion du saut en cours
        if (isLeaping) {
            tickLeap();
            return;
        }

        Player target = findNearestPlayer(30);
        if (target == null) return;

        long now = System.currentTimeMillis();

        // Priorité des attaques

        // 1. Saut Écrasant (prioritaire, longue range)
        if (now - lastLeapAttack >= LEAP_COOLDOWN && target.getLocation().distance(zombie.getLocation()) > 8) {
            leapAttack(target);
            lastLeapAttack = now;
            return;
        }

        // 2. Langue Venimeuse (range moyenne)
        if (now - lastTongueAttack >= TONGUE_COOLDOWN && target.getLocation().distance(zombie.getLocation()) > 5) {
            tongueAttack(target);
            lastTongueAttack = now;
            return;
        }

        // 3. Crachat Toxique (constant)
        if (now - lastToxicSpit >= SPIT_COOLDOWN) {
            toxicSpit(target);
            lastToxicSpit = now;
        }

        // 4. Invocation de mini-grenouilles
        if (now - lastMinionSpawn >= MINION_COOLDOWN) {
            spawnMinions();
            lastMinionSpawn = now;
        }
    }

    /**
     * Langue Venimeuse - Attire le joueur vers le boss
     */
    private void tongueAttack(Player target) {
        playSound(Sound.ENTITY_FROG_TONGUE, 2f, 0.6f);
        playSound(Sound.ENTITY_SLIME_SQUISH, 1.5f, 0.8f);

        // Animation de la langue (particules en ligne)
        Location from = zombie.getLocation().add(0, 1.5, 0);
        Location to = target.getLocation().add(0, 1, 0);
        drawTongueLine(from, to);

        // Attirer le joueur
        Vector pull = zombie.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(2.0);
        pull.setY(0.5);
        target.setVelocity(pull);

        // Appliquer poison
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, phase == 1 ? 0 : 1, false, true));
        target.damage(6 + (phase * 2), zombie);

        target.sendMessage("§a§l🐸 §cLa langue de la grenouille t'a attrapé!");
    }

    /**
     * Dessine une ligne de particules pour la langue
     */
    private void drawTongueLine(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.4) {
            Location point = from.clone().add(direction.clone().multiply(d));
            zombie.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0,
                new Particle.DustOptions(Color.fromRGB(255, 100, 150), 1.2f));
        }
    }

    /**
     * Saut Écrasant - Saute vers le joueur et atterrit avec dégâts AoE
     */
    private void leapAttack(Player target) {
        isLeaping = true;
        leapTarget = target.getLocation().clone();

        playSound(Sound.ENTITY_FROG_LONG_JUMP, 2f, 0.5f);
        playSound(Sound.ENTITY_RAVAGER_ROAR, 1f, 1.5f);

        // Annoncer le saut
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§a§l🐸 §eLa grenouille prépare un saut massif!");
            }
        }

        // Calculer le vecteur de saut
        Vector jumpVec = leapTarget.toVector().subtract(zombie.getLocation().toVector());
        jumpVec.setY(2.0); // Hauteur du saut
        jumpVec.normalize().multiply(1.5);
        jumpVec.setY(1.2);

        zombie.setVelocity(jumpVec);

        // Particules de décollage
        playParticles(Particle.CLOUD, zombie.getLocation(), 30, 1, 0.5, 1);
    }

    /**
     * Gère l'atterrissage du saut
     */
    private void tickLeap() {
        // Particules pendant le saut
        playParticles(Particle.SLIME, zombie.getLocation(), 5, 0.5, 0.5, 0.5);

        // Vérifier si le boss est au sol
        if (zombie.isOnGround() && tickCounter > 5) {
            isLeaping = false;
            leapLanding();
        }
    }

    /**
     * Atterrissage avec dégâts AoE
     */
    private void leapLanding() {
        Location landLoc = zombie.getLocation();

        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        playSound(Sound.BLOCK_SLIME_BLOCK_FALL, 2f, 0.5f);

        // Particules d'impact
        playParticles(Particle.EXPLOSION, landLoc, 3, 1, 0.5, 1);
        zombie.getWorld().spawnParticle(Particle.BLOCK, landLoc, 50, 3, 0.5, 3, 0,
            org.bukkit.Material.SLIME_BLOCK.createBlockData());

        // Dégâts AoE
        double damage = 12 + (phase * 4);
        double radius = phase == 1 ? 5 : 6;

        zombie.getWorld().getNearbyEntities(landLoc, radius, 3, radius).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(damage, zombie);
                // Éjecter les joueurs
                Vector knockback = p.getLocation().toVector()
                    .subtract(landLoc.toVector())
                    .normalize().multiply(1.8).setY(0.6);
                p.setVelocity(knockback);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
            });
    }

    /**
     * Crachat Toxique - Projectile de poison
     */
    private void toxicSpit(Player target) {
        playSound(Sound.ENTITY_LLAMA_SPIT, 1.5f, 0.5f);

        Location from = zombie.getLocation().add(0, 1.5, 0);
        Vector direction = target.getLocation().add(0, 1, 0).toVector().subtract(from.toVector()).normalize();

        // Animation du crachat (particules qui voyagent)
        new BukkitRunnable() {
            Location current = from.clone();
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (ticks++ > maxTicks || !zombie.isValid()) {
                    cancel();
                    return;
                }

                current.add(direction.clone().multiply(1.5));

                // Particules
                zombie.getWorld().spawnParticle(Particle.DUST, current, 5, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 200, 50), 1.5f));
                zombie.getWorld().spawnParticle(Particle.SLIME, current, 3, 0.1, 0.1, 0.1, 0);

                // Vérifier collision avec joueurs
                for (Entity entity : zombie.getWorld().getNearbyEntities(current, 1, 1, 1)) {
                    if (entity instanceof Player p && playersInFight.contains(p.getUniqueId())) {
                        // Impact!
                        p.damage(5 + phase, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, phase - 1, false, true));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, false));

                        zombie.getWorld().playSound(current, Sound.ENTITY_SLIME_SQUISH_SMALL, 1f, 1f);
                        zombie.getWorld().spawnParticle(Particle.SLIME, current, 20, 0.5, 0.5, 0.5, 0);
                        cancel();
                        return;
                    }
                }

                // Vérifier si touche un bloc solide
                if (current.getBlock().getType().isSolid()) {
                    zombie.getWorld().spawnParticle(Particle.SLIME, current, 15, 0.3, 0.3, 0.3, 0);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Invocation de mini-grenouilles mutantes
     */
    private void spawnMinions() {
        playSound(Sound.ENTITY_FROG_AMBIENT, 2f, 0.5f);
        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1.2f);

        var zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        int count = phase == 1 ? 3 : 5;
        int minionLevel = Math.max(1, level - 2);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§a§l🐸 §eDes grenouilles mutantes surgissent du marais!");
            }
        }

        for (int i = 0; i < count; i++) {
            Location spawnLoc = getRandomNearbyLocation(6);
            if (spawnLoc != null) {
                var minion = zombieManager.spawnZombie(ZombieType.WALKER, spawnLoc, minionLevel);
                if (minion != null) {
                    Entity entity = plugin.getServer().getEntity(minion.getEntityId());
                    if (entity instanceof Zombie z) {
                        z.addScoreboardTag("swamp_frog_minion_" + zombie.getUniqueId());
                        // Rendre les minions plus petits et verts
                        var scale = z.getAttribute(Attribute.SCALE);
                        if (scale != null) {
                            scale.setBaseValue(0.6);
                        }
                        z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                    }
                }
                // Particules de spawn
                zombie.getWorld().spawnParticle(Particle.SLIME, spawnLoc, 20, 0.5, 0.5, 0.5, 0);
                zombie.getWorld().playSound(spawnLoc, Sound.ENTITY_FROG_STEP, 1f, 1.5f);
            }
        }
    }

    /**
     * Vérifie si le boss doit changer de phase
     */
    private void checkPhaseTransition() {
        if (phase == 1 && isHealthBelow(0.5)) {
            phase = 2;
            onPhaseChange();
        }
    }

    private void onPhaseChange() {
        playSound(Sound.ENTITY_FROG_HURT, 2f, 0.3f);
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 2, 0, 0, 0);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§2§lPHASE 2", "§aLa grenouille entre en rage!", 10, 40, 10);
            }
        }

        // Buffs de phase 2
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.25);
        }

        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.2);
        }

        // Grandir légèrement
        var scale = zombie.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(scale.getBaseValue() * 1.15);
        }

        // Regen partielle
        heal(zombie.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.1);

        // Immunité temporaire
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false));

        // Réduire les cooldowns en phase 2
        lastTongueAttack = 0;
        lastLeapAttack = 0;
    }

    /**
     * Vérifie si le boss est trop loin de son spawn
     */
    private void checkLeash() {
        if (tickCounter % 20 != 0) return;

        if (zombie.getLocation().distanceSquared(spawnLocation) > LEASH_RANGE_SQUARED) {
            zombie.teleport(spawnLocation);
            setZombieTarget(null);
            playSound(Sound.ENTITY_FROG_LONG_JUMP, 1.5f, 0.5f);
            playParticles(Particle.SLIME, spawnLocation.clone().add(0, 1, 0), 30, 1, 1, 1);

            // Heal complet si reset
            var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                zombie.setHealth(maxHealth.getValue());
            }
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;

        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double progress = zombie.getHealth() / maxHealth.getValue();
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));
            bossBar.setTitle("§2§l🐸 " + zombieType.getDisplayName() + " §7[Phase " + phase + "/2]");
            bossBar.setColor(phase == 1 ? BarColor.GREEN : BarColor.YELLOW);
        }
    }

    private void updatePlayersInFight() {
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 45, 25, 45).stream()
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
            if (p == null || !p.isOnline() || p.getLocation().distance(zombie.getLocation()) > 55) {
                if (p != null) bossBar.removePlayer(p);
                return true;
            }
            return false;
        });
    }

    // === UTILITY ===

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

    private Location getRandomNearbyLocation(double range) {
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 2 + random.nextDouble() * range;
            Location loc = zombie.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            for (int y = (int) loc.getY(); y > zombie.getWorld().getMinHeight(); y--) {
                Location checkLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
                if (checkLoc.getBlock().getType().isSolid()) {
                    return checkLoc.add(0, 1, 0);
                }
            }
        }
        return null;
    }

    protected void playParticles(Particle particle, Location loc, int count, double dx, double dy, double dz) {
        zombie.getWorld().spawnParticle(particle, loc, count, dx, dy, dz);
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
        // Contre-attaque occasionnelle (10% de chance) - crache du poison
        if (attacker instanceof Player player && random.nextFloat() < 0.10f) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0, false, false));
            playParticles(Particle.SLIME, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Nettoyer la bossbar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        // Effets de mort
        playSound(Sound.ENTITY_FROG_DEATH, 2f, 0.3f);
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 1, 1, 1);

        // Explosion de slime
        zombie.getWorld().spawnParticle(Particle.SLIME, zombie.getLocation(), 100, 3, 2, 3, 0);
        zombie.getWorld().spawnParticle(Particle.ITEM, zombie.getLocation(), 50, 2, 1, 2, 0.1,
            new org.bukkit.inventory.ItemStack(org.bukkit.Material.SLIME_BALL));
    }

    /**
     * Obtient les joueurs participant au combat (pour la quête)
     */
    public Set<UUID> getPlayersInFight() {
        return new HashSet<>(playersInFight);
    }
}
