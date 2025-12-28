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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * IA pour le boss "Le Premier Mort" (Chapitre 4 Étape 2)
 * Wither Skeleton géant (scale x3) avec mécaniques uniques :
 * - Attaque au sol : onde de choc qui repousse les joueurs
 * - Invocation de squelettes depuis les tombes
 * - Phase 2 : Plus rapide et plus agressif
 *
 * Stratégie pour le tuer :
 * - Éviter ses attaques au sol (indiquées par des particules)
 * - L'attaquer après ses attaques spéciales (fenêtre de vulnérabilité)
 */
public class GravediggerBossAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private BossBar bossBar;
    private final Set<UUID> playersInFight = new HashSet<>();

    // Position de spawn pour le leash
    private final Location spawnLocation;
    private static final double LEASH_RANGE_SQUARED = 40 * 40;

    // Cooldowns
    private long lastGroundSlam = 0;
    private long lastSummon = 0;
    private static final long GROUND_SLAM_COOLDOWN = 8000; // 8 secondes
    private static final long SUMMON_COOLDOWN = 15000; // 15 secondes

    // État de vulnérabilité
    private boolean isVulnerable = false;
    private int vulnerableTicks = 0;
    private static final int VULNERABLE_DURATION = 60; // 3 secondes (20 ticks/s)

    // Charge attack
    private boolean isCharging = false;
    private int chargeTicks = 0;
    private Location chargeTarget = null;

    public GravediggerBossAI(ZombieZPlugin plugin, LivingEntity entity, ZombieType zombieType, int level) {
        super(plugin, entity, zombieType, level);
        this.abilityCooldown = 6000; // 6 secondes entre les attaques
        this.spawnLocation = entity.getLocation().clone();

        setupBossBar();
        applyBossBuffs();
    }

    private void setupBossBar() {
        String title = "§4§l☠ Le Premier Mort §7[Phase " + phase + "/2]";
        bossBar = plugin.getServer().createBossBar(title, BarColor.WHITE, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void applyBossBuffs() {
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.9); // Très résistant au knockback
        }

        // Immunité au feu et à la wither
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        tickCounter++;

        updateBossBar();
        updatePlayersInFight();
        checkPhaseTransition();
        checkLeash();
        handleVulnerability();
        handleCharge();

        // Particules d'aura constantes (os et âmes)
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 1.5, 0), 3, 0.5, 0.5, 0.5);
            playParticles(Particle.ASH, zombie.getLocation().add(0, 1, 0), 8, 1, 1.5, 1);
        }

        // Comportement de combat
        if (!isCharging && !isVulnerable) {
            performCombatBehavior();
        }
    }

    private void performCombatBehavior() {
        Player target = findNearestPlayer(30);
        if (target == null) return;

        // Ground Slam (priorité)
        if (canUseGroundSlam() && target.getLocation().distance(zombie.getLocation()) < 8) {
            prepareGroundSlam();
            return;
        }

        // Invocation de squelettes (phase 2 ou si vie < 70%)
        if (canUseSummon() && (phase == 2 || isHealthBelow(0.7))) {
            summonSkeletons();
            return;
        }

        // Charge vers le joueur s'il est loin
        if (canUseAbility() && target.getLocation().distance(zombie.getLocation()) > 10) {
            startCharge(target.getLocation());
        }
    }

    /**
     * Prépare l'attaque au sol (indiquée par des particules pour que le joueur puisse esquiver)
     */
    private void prepareGroundSlam() {
        playSound(Sound.ENTITY_WITHER_SKELETON_AMBIENT, 2f, 0.5f);

        // Particules d'avertissement au sol
        for (double angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            Location particleLoc = zombie.getLocation().add(Math.cos(rad) * 4, 0.1, Math.sin(rad) * 4);
            playParticles(Particle.DUST_PLUME, particleLoc, 5, 0.3, 0.1, 0.3);
        }

        // Exécuter le slam après un court délai
        plugin.getServer().getScheduler().runTaskLater(plugin, this::executeGroundSlam, 25L);
        lastGroundSlam = System.currentTimeMillis();
    }

    /**
     * Exécute l'attaque au sol - onde de choc
     */
    private void executeGroundSlam() {
        if (!zombie.isValid()) return;

        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        playSound(Sound.BLOCK_STONE_BREAK, 2f, 0.5f);

        // Particules d'impact
        playParticles(Particle.EXPLOSION, zombie.getLocation(), 3, 0, 0, 0);

        // Cercle de particules qui s'étend
        for (int radius = 1; radius <= 6; radius++) {
            final int r = radius;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;
                for (double angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = zombie.getLocation().add(Math.cos(rad) * r, 0.2, Math.sin(rad) * r);
                    playParticles(Particle.BLOCK, particleLoc, 8, 0.3, 0.1, 0.3,
                        org.bukkit.Material.COARSE_DIRT);
                }
            }, radius * 2L);
        }

        // Dégâts et knockback aux joueurs dans la zone
        double damage = phase == 1 ? 10 : 15;
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 6, 3, 6).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .filter(p -> !hasRespawnProtection(p))
                .forEach(p -> {
                    double dist = p.getLocation().distance(zombie.getLocation());
                    if (dist < 6) {
                        // Moins de dégâts si le joueur est loin
                        double actualDamage = damage * (1 - dist / 8);
                        p.damage(actualDamage, zombie);

                        // Knockback puissant vers l'extérieur
                        Vector knockback = p.getLocation().toVector()
                                .subtract(zombie.getLocation().toVector())
                                .normalize().multiply(1.8).setY(0.6);
                        p.setVelocity(knockback);
                    }
                });

        // Le boss devient vulnérable après le slam
        becomeVulnerable();
    }

    /**
     * Invoque des squelettes depuis le sol
     */
    private void summonSkeletons() {
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);

        var zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        int count = phase == 1 ? 2 : 3;
        int minionLevel = Math.max(1, level - 5);

        for (int i = 0; i < count; i++) {
            Location spawnLoc = getRandomNearbyLocation(8);
            if (spawnLoc != null) {
                // Effet visuel de spawn depuis le sol
                playParticles(Particle.SOUL, spawnLoc, 20, 0.3, 0.5, 0.3);
                playParticles(Particle.BLOCK, spawnLoc, 15, 0.5, 0.2, 0.5,
                    org.bukkit.Material.SOUL_SOIL);

                // Spawn le squelette via ZombieManager
                var minion = zombieManager.spawnZombie(ZombieType.SKELETON, spawnLoc, minionLevel);
                if (minion != null) {
                    Entity entity = plugin.getServer().getEntity(minion.getEntityId());
                    if (entity != null) {
                        entity.addScoreboardTag("gravedigger_minion_" + zombie.getUniqueId());
                    }
                }
            }
        }

        lastSummon = System.currentTimeMillis();

        // Message aux joueurs proches
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§8[§c☠§8] §7Le Premier Mort invoque ses serviteurs!");
            }
        }
    }

    /**
     * Démarre une charge vers une position
     */
    private void startCharge(Location target) {
        isCharging = true;
        chargeTicks = 0;
        chargeTarget = target.clone();

        playSound(Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);

        // Avertissement visuel
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§c⚠ Le Premier Mort se prépare à charger!");
            }
        }

        useAbility();
    }

    /**
     * Gère la charge en cours
     */
    private void handleCharge() {
        if (!isCharging) return;

        chargeTicks++;

        // Phase de préparation (1 seconde)
        if (chargeTicks < 20) {
            // Particules de préparation
            playParticles(Particle.SMOKE, zombie.getLocation().add(0, 0.5, 0), 10, 0.5, 0.3, 0.5);
            return;
        }

        // Phase de charge
        if (chargeTicks < 40) {
            Vector direction = chargeTarget.toVector().subtract(zombie.getLocation().toVector());
            if (direction.lengthSquared() > 4) { // Plus de 2 blocs
                direction.normalize().multiply(0.8);
                direction.setY(0.1);
                zombie.setVelocity(direction);

                // Particules de traînée
                playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation(), 5, 0.3, 0.3, 0.3);

                // Dégâts aux joueurs sur le chemin
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .filter(p -> !hasRespawnProtection(p))
                        .forEach(p -> {
                            p.damage(8, zombie);
                            Vector kb = p.getLocation().toVector()
                                    .subtract(zombie.getLocation().toVector())
                                    .normalize().multiply(1.2).setY(0.4);
                            p.setVelocity(kb);
                        });
            }
            return;
        }

        // Fin de charge
        isCharging = false;
        chargeTarget = null;

        // Vulnérabilité après la charge
        becomeVulnerable();
    }

    /**
     * Le boss devient vulnérable (moins de résistance aux dégâts)
     */
    private void becomeVulnerable() {
        isVulnerable = true;
        vulnerableTicks = 0;

        // Effet visuel
        playSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.5f);
        playParticles(Particle.ENCHANT, zombie.getLocation().add(0, 1, 0), 30, 1, 1, 1);

        // Message aux joueurs
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§a✦ Le Premier Mort est vulnérable! Attaque!");
            }
        }

        // Slowness pendant la vulnérabilité
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, VULNERABLE_DURATION, 1, false, true));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, VULNERABLE_DURATION, 0, false, true));
    }

    /**
     * Gère l'état de vulnérabilité
     */
    private void handleVulnerability() {
        if (!isVulnerable) return;

        vulnerableTicks++;

        // Particules pendant la vulnérabilité
        if (vulnerableTicks % 5 == 0) {
            playParticles(Particle.CRIT, zombie.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5);
        }

        // Fin de vulnérabilité
        if (vulnerableTicks >= VULNERABLE_DURATION) {
            isVulnerable = false;
            playSound(Sound.ENTITY_WITHER_AMBIENT, 1f, 0.7f);

            for (UUID uuid : playersInFight) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    p.sendMessage("§c⚠ Le Premier Mort reprend ses forces!");
                }
            }
        }
    }

    /**
     * Vérifie la transition de phase
     */
    private void checkPhaseTransition() {
        if (phase == 1 && isHealthBelow(0.5)) {
            phase = 2;
            onPhaseChange();
        }
    }

    private void onPhaseChange() {
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.8f, 1f);
        playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation(), 50, 2, 2, 2);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§4§l☠ PHASE 2", "§7Le Premier Mort s'enrage!", 10, 40, 10);
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

        // Régénération partielle
        heal(zombie.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.15);

        // Immunité temporaire
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 3, false, false));
    }

    /**
     * Vérifie si le boss est trop loin de son spawn
     */
    private void checkLeash() {
        if (tickCounter % 20 != 0) return;

        if (zombie.getLocation().distanceSquared(spawnLocation) > LEASH_RANGE_SQUARED) {
            zombie.teleport(spawnLocation);
            zombie.setTarget(null);
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            playParticles(Particle.PORTAL, spawnLocation.clone().add(0, 1, 0), 30, 0.5, 1, 0.5);

            // Reset les états
            isCharging = false;
            isVulnerable = false;
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;

        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double progress = zombie.getHealth() / maxHealth.getValue();
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));

            String vulnerableText = isVulnerable ? " §a[VULNÉRABLE]" : "";
            bossBar.setTitle("§4§l☠ Le Premier Mort §7[Phase " + phase + "/2]" + vulnerableText);

            // Couleur selon l'état
            if (isVulnerable) {
                bossBar.setColor(BarColor.GREEN);
            } else if (phase == 2) {
                bossBar.setColor(BarColor.RED);
            } else {
                bossBar.setColor(BarColor.WHITE);
            }
        }
    }

    private void updatePlayersInFight() {
        // Ajouter les joueurs proches
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 45, 20, 45).stream()
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
            if (p == null || !p.isOnline() || p.getLocation().distance(zombie.getLocation()) > 55) {
                if (p != null) bossBar.removePlayer(p);
                return true;
            }
            return false;
        });
    }

    // === COOLDOWN CHECKS ===

    private boolean canUseGroundSlam() {
        return System.currentTimeMillis() - lastGroundSlam >= GROUND_SLAM_COOLDOWN;
    }

    private boolean canUseSummon() {
        return System.currentTimeMillis() - lastSummon >= SUMMON_COOLDOWN;
    }

    private Location getRandomNearbyLocation(double range) {
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 3 + random.nextDouble() * range;
            Location loc = zombie.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            for (int y = (int) loc.getY() + 3; y > zombie.getWorld().getMinHeight(); y--) {
                Location checkLoc = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
                if (checkLoc.getBlock().getType().isSolid()) {
                    return checkLoc.add(0, 1, 0);
                }
            }
        }
        return null;
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Dégâts bonus pendant la vulnérabilité (déjà appliqués par le système de combat)

        // Contre-attaque occasionnelle hors vulnérabilité (10% de chance)
        if (!isVulnerable && attacker instanceof Player player && random.nextFloat() < 0.10f) {
            player.damage(4, zombie);
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

        // Tuer tous les minions
        zombie.getWorld().getEntities().stream()
                .filter(e -> e.getScoreboardTags().contains("gravedigger_minion_" + zombie.getUniqueId()))
                .forEach(Entity::remove);

        // Effets de mort épiques
        playSound(Sound.ENTITY_WITHER_DEATH, 1.5f, 0.7f);
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);

        // Explosion de particules
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 0, 0, 0);
        playParticles(Particle.SOUL, zombie.getLocation(), 100, 3, 3, 3);
        playParticles(Particle.ASH, zombie.getLocation(), 200, 5, 3, 5);
    }

    /**
     * Obtient les joueurs participant au combat (pour la quête)
     */
    public Set<UUID> getPlayersInFight() {
        return new HashSet<>(playersInFight);
    }
}
