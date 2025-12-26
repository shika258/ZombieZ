package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IA pour les boss du Journey (chapitres de progression)
 * Comportement: Plus simple que les ZONE_BOSS, adapté à la progression des joueurs
 */
public class JourneyBossAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private BossBar bossBar;
    private final Set<UUID> playersInFight = new HashSet<>();

    // Position de spawn pour le leash
    private final Location spawnLocation;
    private static final double LEASH_RANGE_SQUARED = 32 * 32;

    // Cooldowns
    private long lastSpecialAttack = 0;
    private static final long SPECIAL_COOLDOWN = 10000; // 10 secondes

    public JourneyBossAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 5000; // 5 secondes entre les attaques
        this.spawnLocation = zombie.getLocation().clone();

        setupBossBar();
        applyBossBuffs();
    }

    private void setupBossBar() {
        String title = "§4§l" + zombieType.getDisplayName() + " §7[Phase " + phase + "/2]";
        bossBar = plugin.getServer().createBossBar(title, BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void applyBossBuffs() {
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.8);
        }

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        tickCounter++;

        updateBossBar();
        updatePlayersInFight();
        checkPhaseTransition();
        checkLeash();

        // Comportement selon le type de boss
        switch (zombieType) {
            case MANOR_LORD -> tickManorLord();
            default -> tickManorLord(); // Default fallback
        }
    }

    /**
     * IA du Seigneur du Manoir
     */
    private void tickManorLord() {
        // Particules d'aura constantes
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
        }

        Player target = findNearestPlayer(25);
        if (target == null) return;

        // Attaque principale: Onde de choc (toutes les 5 secondes)
        if (canUseAbility()) {
            shockwaveAttack();
            useAbility();
        }

        // Attaque spéciale: Invocation de renforts (toutes les 10 secondes)
        if (canUseSpecialAttack()) {
            summonReinforcements();
            useSpecialAttack();
        }
    }

    /**
     * Onde de choc - repousse les joueurs et inflige des dégâts
     */
    private void shockwaveAttack() {
        playSound(Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
        playParticles(Particle.EXPLOSION, zombie.getLocation(), 10, 2, 1, 2);

        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 5, 3, 5).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    double damage = 8 + (phase * 2); // Plus de dégâts en phase 2
                    p.damage(damage, zombie);
                    Vector knockback = p.getLocation().toVector()
                            .subtract(zombie.getLocation().toVector())
                            .normalize().multiply(1.5).setY(0.5);
                    p.setVelocity(knockback);
                });
    }

    /**
     * Invocation de serviteurs
     */
    private void summonReinforcements() {
        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 2f, 0.8f);

        int count = phase == 1 ? 2 : 3; // Plus de renforts en phase 2
        for (int i = 0; i < count; i++) {
            Location spawnLoc = getRandomNearbyLocation(5);
            if (spawnLoc != null) {
                zombie.getWorld().spawn(spawnLoc, Zombie.class, z -> {
                    z.setCustomName("§7Serviteur du Manoir");
                    z.setCustomNameVisible(true);
                    z.setBaby(false);

                    var health = z.getAttribute(Attribute.MAX_HEALTH);
                    if (health != null) {
                        health.setBaseValue(30);
                        z.setHealth(30);
                    }
                });
                playParticles(Particle.SOUL, spawnLoc, 15, 0.3, 0.5, 0.3);
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
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 0, 0, 0);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§c§lPHASE 2", "§7Le Seigneur du Manoir s'enrage!", 10, 40, 10);
            }
        }

        // Buffs de phase 2
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.2);
        }

        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.15);
        }

        // Regen partielle
        heal(zombie.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.1);

        // Immunité temporaire
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false));
    }

    /**
     * Vérifie si le boss est trop loin de son spawn et le ramène
     */
    private void checkLeash() {
        if (tickCounter % 20 != 0) return; // Check toutes les secondes

        if (zombie.getLocation().distanceSquared(spawnLocation) > LEASH_RANGE_SQUARED) {
            zombie.teleport(spawnLocation);
            zombie.setTarget(null);
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            playParticles(Particle.PORTAL, spawnLocation.clone().add(0, 1, 0), 30, 0.5, 1, 0.5);
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
        // Ajouter les joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 40, 20, 40).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    if (!playersInFight.contains(p.getUniqueId())) {
                        playersInFight.add(p.getUniqueId());
                        bossBar.addPlayer(p);
                    }
                });

        // Retirer les joueurs éloignés
        playersInFight.removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null || !p.isOnline() || p.getLocation().distance(zombie.getLocation()) > 50) {
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

    private boolean canUseSpecialAttack() {
        return System.currentTimeMillis() - lastSpecialAttack >= SPECIAL_COOLDOWN;
    }

    private void useSpecialAttack() {
        lastSpecialAttack = System.currentTimeMillis();
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
        // Contre-attaque occasionnelle (15% de chance)
        if (attacker instanceof Player player && random.nextFloat() < 0.15f) {
            player.damage(3, zombie);
            playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Nettoyer la bossbar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        // Effets de mort (les messages sont gérés par le système Journey)
        playSound(Sound.ENTITY_WITHER_DEATH, 1.5f, 0.8f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 5, 1, 1, 1);
        playParticles(Particle.SOUL, zombie.getLocation(), 50, 2, 2, 2);
    }

    /**
     * Obtient les joueurs participant au combat (pour la quête)
     */
    public Set<UUID> getPlayersInFight() {
        return new HashSet<>(playersInFight);
    }
}
