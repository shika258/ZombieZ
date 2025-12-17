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
 * IA pour les BOSS (MINIBOSS, ZONE_BOSS, FINAL_BOSS)
 * Comportement: Multiphase, patterns complexes, très dangereux
 */
public class BossZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private int maxPhases = 3;
    private boolean isExecutingPattern = false;
    private BossBar bossBar;
    private final Set<UUID> playersInFight = new HashSet<>();

    // Cooldowns spéciaux pour les boss
    private long lastSpecialAttack = 0;
    private long specialCooldown = 15000;

    public BossZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 8000;

        setupBossBar();
        applyBossBuffs();
    }

    private void setupBossBar() {
        BarColor color = switch (zombieType.getCategory()) {
            case MINIBOSS -> BarColor.YELLOW;
            case ZONE_BOSS -> BarColor.RED;
            case FINAL_BOSS -> BarColor.PURPLE;
            default -> BarColor.WHITE;
        };

        String title = "§c§l" + zombieType.getDisplayName() + " §7[Phase " + phase + "]";
        bossBar = plugin.getServer().createBossBar(title, color, BarStyle.SEGMENTED_10);
        bossBar.setVisible(true);
    }

    private void applyBossBuffs() {
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.9);
        }

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        tickCounter++;

        updateBossBar();
        updatePlayersInFight();
        checkPhaseTransition();

        switch (zombieType.getCategory()) {
            case MINIBOSS -> tickMiniBoss();
            case ZONE_BOSS -> tickZoneBoss();
            case FINAL_BOSS -> tickFinalBoss();
            default -> tickMiniBoss();
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;

        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double progress = zombie.getHealth() / maxHealth.getValue();
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));
            bossBar.setTitle("§c§l" + zombieType.getDisplayName() + " §7[Phase " + phase + "/" + maxPhases + "]");
        }
    }

    private void updatePlayersInFight() {
        // Ajouter les joueurs proches à la bossbar
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 50, 30, 50).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                if (!playersInFight.contains(p.getUniqueId())) {
                    playersInFight.add(p.getUniqueId());
                    bossBar.addPlayer(p);
                    // Notifier le système de Boss Bar Dynamique
                    if (plugin.getDynamicBossBarManager() != null) {
                        var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);
                        double maxHp = maxHealth != null ? maxHealth.getValue() : 100;
                        plugin.getDynamicBossBarManager().registerBoss(p, zombie.getUniqueId(),
                                zombieType.getDisplayName(), maxHp);
                    }
                }
            });

        // Mettre à jour la santé du boss pour le système dynamique
        if (plugin.getDynamicBossBarManager() != null) {
            for (UUID uuid : playersInFight) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    plugin.getDynamicBossBarManager().updateBossHealth(p, zombie.getUniqueId(), zombie.getHealth());
                }
            }
        }

        // Retirer les joueurs trop loin
        playersInFight.removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null || !p.isOnline() || p.getLocation().distance(zombie.getLocation()) > 60) {
                if (p != null) {
                    bossBar.removePlayer(p);
                    // Notifier le système de Boss Bar Dynamique
                    if (plugin.getDynamicBossBarManager() != null) {
                        plugin.getDynamicBossBarManager().removeBoss(p, zombie.getUniqueId());
                    }
                }
                return true;
            }
            return false;
        });
    }

    private void checkPhaseTransition() {
        double[] thresholds = switch (zombieType.getCategory()) {
            case MINIBOSS -> new double[]{0.5, 0.2};
            case ZONE_BOSS -> new double[]{0.66, 0.33};
            case FINAL_BOSS -> new double[]{0.75, 0.50, 0.25};
            default -> new double[]{0.5};
        };

        maxPhases = thresholds.length + 1;

        for (int i = 0; i < thresholds.length; i++) {
            if (phase == i + 1 && isHealthBelow(thresholds[i])) {
                phase = i + 2;
                onPhaseChange(phase);
                break;
            }
        }
    }

    private void onPhaseChange(int newPhase) {
        isExecutingPattern = true;

        // Annonce
        playSound(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 3, 0, 0, 0);

        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§c§lPHASE " + newPhase, "§7" + zombieType.getDisplayName() + " s'enrage!", 10, 40, 10);
            }
        }

        // Buffs de phase
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.15);
        }

        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.1);
        }

        // Régénération partielle
        heal(zombie.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.1);

        // Immunité temporaire
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isExecutingPattern = false, 60L);
    }

    // ═══════════════════════════════════════════════════════════════
    // MINI-BOSS BEHAVIOR
    // ═══════════════════════════════════════════════════════════════

    private void tickMiniBoss() {
        switch (zombieType) {
            case BUTCHER -> tickButcher();
            case WIDOW -> tickWidow();
            case THE_GIANT -> tickTheGiant();
            case THE_PHANTOM -> tickThePhantom();
            default -> tickButcher();
        }
    }

    private void tickButcher() {
        // Effet de sang
        if (tickCounter % 20 == 0) {
            playParticles(Particle.BLOCK, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3);
        }

        Player target = findNearestPlayer(20);
        if (target == null || isExecutingPattern) return;

        if (canUseSpecialAttack()) {
            if (phase == 1) cleaveAttack(target);
            else if (phase == 2) bloodFrenzy();
            else executionStrike(target);
            useSpecialAttack();
        }
    }

    private void tickWidow() {
        // Toiles
        if (tickCounter % 30 == 0) {
            playParticles(Particle.ITEM_SNOWBALL, zombie.getLocation(), 5, 0.5, 0.5, 0.5);
        }

        Player target = findNearestPlayer(20);
        if (target == null || isExecutingPattern) return;

        if (canUseSpecialAttack()) {
            if (phase == 1) webShot(target);
            else if (phase == 2) venomBurst(target);
            else spiderSwarm();
            useSpecialAttack();
        }
    }

    private void tickTheGiant() {
        // Tremblement
        if (tickCounter % 40 == 0) {
            playSound(Sound.ENTITY_RAVAGER_STEP, 1f, 0.3f);
            playParticles(Particle.BLOCK, zombie.getLocation(), 20, 1, 0.1, 1);
        }

        Player target = findNearestPlayer(25);
        if (target == null || isExecutingPattern) return;

        if (canUseSpecialAttack()) {
            if (phase == 1) groundSlam();
            else if (phase == 2) boulderThrow(target);
            else earthquakeWave();
            useSpecialAttack();
        }
    }

    private void tickThePhantom() {
        // Effet fantomatique
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 5, 0.5, 0.8, 0.5);
            zombie.setInvisible(tickCounter % 40 < 10);
        }

        Player target = findNearestPlayer(25);
        if (target == null || isExecutingPattern) return;

        if (canUseSpecialAttack()) {
            if (phase == 1) phantomDash(target);
            else if (phase == 2) terrorScream();
            else soulHarvest();
            useSpecialAttack();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ZONE BOSS BEHAVIOR
    // ═══════════════════════════════════════════════════════════════

    private void tickZoneBoss() {
        // Aura de boss de zone
        if (tickCounter % 15 == 0) {
            playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation().add(0, 1, 0), 10, 1, 1, 1);
        }

        Player target = findNearestPlayer(30);
        if (target == null || isExecutingPattern) return;

        // Rotation d'abilities plus complexe
        if (canUseAbility()) {
            int ability = (tickCounter / 200) % 3;
            switch (ability) {
                case 0 -> zoneBossAbility1(target);
                case 1 -> zoneBossAbility2(target);
                case 2 -> zoneBossAbility3();
            }
            useAbility();
        }

        // Attaque spéciale
        if (canUseSpecialAttack()) {
            zoneBossUltimate(target);
            useSpecialAttack();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FINAL BOSS (PATIENT ZERO)
    // ═══════════════════════════════════════════════════════════════

    private void tickFinalBoss() {
        // Aura corrompue intense
        if (tickCounter % 8 == 0) {
            playParticles(Particle.DRAGON_BREATH, zombie.getLocation().add(0, 1, 0), 15, 1.5, 1, 1.5);
            playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation().add(0, 0.5, 0), 5, 1, 0.3, 1);

            // Darkness passive
            applyAreaEffect(15, PotionEffectType.DARKNESS, 40, 0);
        }

        // Musique de boss
        if (tickCounter % 100 == 0) {
            playSound(Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 0.8f);
        }

        Player target = findNearestPlayer(40);
        if (target == null || isExecutingPattern) return;

        // Phase 1: Attaques de base amplifiées
        // Phase 2: Invocation de minions
        // Phase 3: Patterns chaotiques
        // Phase 4: Désespoir (tout à la fois)

        if (canUseAbility()) {
            switch (phase) {
                case 1 -> patientZeroPhase1(target);
                case 2 -> patientZeroPhase2(target);
                case 3 -> patientZeroPhase3(target);
                case 4 -> patientZeroPhase4(target);
            }
            useAbility();
        }

        // Ultimate basé sur la phase
        if (canUseSpecialAttack() && phase >= 2) {
            patientZeroUltimate(target);
            useSpecialAttack();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ABILITIES DES MINI-BOSS
    // ═══════════════════════════════════════════════════════════════

    private void cleaveAttack(Player target) {
        playSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        playParticles(Particle.SWEEP_ATTACK, zombie.getLocation().add(0, 1, 0), 10, 2, 0.5, 2);

        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 4, 2, 4).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(15 + level * 2, zombie);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            });
    }

    private void bloodFrenzy() {
        playSound(Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2, false, true));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, true));
        playParticles(Particle.BLOCK, zombie.getLocation(), 50, 1, 1, 1);
    }

    private void executionStrike(Player target) {
        if (target.getHealth() < target.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.3) {
            playSound(Sound.ENTITY_PLAYER_ATTACK_CRIT, 2f, 0.5f);
            target.damage(30, zombie);
            playParticles(Particle.ENCHANTED_HIT, target.getLocation(), 50, 0.5, 0.5, 0.5);
            target.sendMessage("§4§l☠ EXÉCUTION! ☠");
        }
    }

    private void webShot(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
        playParticles(Particle.ITEM_SNOWBALL, target.getLocation(), 30, 0.5, 1, 0.5);
        playSound(Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.5f);
    }

    private void venomBurst(Player target) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 2));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        target.damage(10, zombie);
        playParticles(Particle.ITEM_SLIME, target.getLocation(), 30, 0.5, 0.5, 0.5);
    }

    private void spiderSwarm() {
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = getRandomNearbyLocation(5);
            if (spawnLoc != null) {
                var manager = plugin.getZombieManager();
                if (manager != null) {
                    manager.spawnZombie(ZombieType.LURKER, spawnLoc, Math.max(1, level - 5));
                }
            }
        }
        playSound(Sound.ENTITY_SPIDER_AMBIENT, 2f, 0.3f);
    }

    private void groundSlam() {
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        playParticles(Particle.BLOCK, zombie.getLocation(), 100, 3, 0.5, 3);

        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 8, 3, 8).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(20 + level, zombie);
                p.setVelocity(new Vector(0, 1.5, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            });
    }

    private void boulderThrow(Player target) {
        playSound(Sound.ENTITY_IRON_GOLEM_ATTACK, 2f, 0.5f);

        // Simuler le lancer de rocher
        Location impactLoc = target.getLocation();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            playParticles(Particle.BLOCK, impactLoc, 50, 2, 1, 2);
            impactLoc.getWorld().getNearbyEntities(impactLoc, 3, 3, 3).stream()
                .filter(e -> e instanceof Player)
                .forEach(e -> ((Player) e).damage(15, zombie));
        }, 20L);
    }

    private void earthquakeWave() {
        for (int wave = 0; wave < 5; wave++) {
            final int w = wave;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double radius = 3 + w * 3;
                playSound(Sound.ENTITY_RAVAGER_STEP, 1f, 0.5f);
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location loc = zombie.getLocation().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
                    playParticles(Particle.BLOCK, loc, 5, 0.2, 0.1, 0.2);
                }
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), radius + 1, 2, radius + 1).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .filter(p -> Math.abs(p.getLocation().distance(zombie.getLocation()) - radius) < 2)
                    .forEach(p -> {
                        p.damage(10, zombie);
                        p.setVelocity(new Vector(0, 0.8, 0));
                    });
            }, w * 10L);
        }
    }

    private void phantomDash(Player target) {
        playParticles(Particle.SOUL, zombie.getLocation(), 30, 0.5, 1, 0.5);
        zombie.teleport(target.getLocation().add(target.getLocation().getDirection().multiply(-2)));
        playParticles(Particle.SOUL, zombie.getLocation(), 30, 0.5, 1, 0.5);
        playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);

        target.damage(12, zombie);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
    }

    private void terrorScream() {
        playSound(Sound.ENTITY_GHAST_SCREAM, 2f, 0.5f);
        applyAreaEffect(15, PotionEffectType.DARKNESS, 100, 0);
        applyAreaEffect(15, PotionEffectType.SLOWNESS, 80, 1);
        playParticles(Particle.SONIC_BOOM, zombie.getLocation().add(0, 1, 0), 5, 2, 1, 2);
    }

    private void soulHarvest() {
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 12, 6, 12).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double damage = 8;
                p.damage(damage, zombie);
                heal(damage);
                playParticles(Particle.SOUL, p.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3);
            });
        playSound(Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
    }

    // ═══════════════════════════════════════════════════════════════
    // ZONE BOSS ABILITIES
    // ═══════════════════════════════════════════════════════════════

    private void zoneBossAbility1(Player target) {
        // Attaque chargée
        playSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 1f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid()) {
                target.damage(25 + level * 2, zombie);
                playParticles(Particle.EXPLOSION, target.getLocation(), 5, 0.5, 0.5, 0.5);
            }
        }, 30L);
    }

    private void zoneBossAbility2(Player target) {
        // Zone dangereuse
        Location center = target.getLocation();
        for (int i = 0; i < 50; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playParticles(Particle.DRAGON_BREATH, center, 10, 3, 0.1, 3);
                center.getWorld().getNearbyEntities(center, 3, 2, 3).stream()
                    .filter(e -> e instanceof Player)
                    .forEach(e -> ((Player) e).damage(3, zombie));
            }, i);
        }
    }

    private void zoneBossAbility3() {
        // Invocation
        for (int i = 0; i < 3; i++) {
            Location loc = getRandomNearbyLocation(8);
            if (loc != null && plugin.getZombieManager() != null) {
                plugin.getZombieManager().spawnZombie(ZombieType.WALKER, loc, level);
            }
        }
        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
    }

    private void zoneBossUltimate(Player target) {
        isExecutingPattern = true;

        playSound(Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§4§l⚠ ULTIMATE ⚠", "", 10, 30, 10);
            }
        }

        // Pattern complexe
        for (int step = 0; step < 10; step++) {
            final int s = step;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;
                applyAreaEffect(20, PotionEffectType.WITHER, 40, 1);
                playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation(), 50, 5, 2, 5);
            }, step * 10L);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isExecutingPattern = false, 100L);
    }

    // ═══════════════════════════════════════════════════════════════
    // PATIENT ZERO ABILITIES
    // ═══════════════════════════════════════════════════════════════

    private void patientZeroPhase1(Player target) {
        // Attaques de corruption
        playSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.8f);

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        Location rayLoc = zombie.getEyeLocation().clone();
        for (int i = 0; i < 25; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location currentLoc = rayLoc.clone().add(direction.clone().multiply(step));
                playParticles(Particle.DRAGON_BREATH, currentLoc, 10, 0.3, 0.3, 0.3);

                currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5).stream()
                    .filter(e -> e instanceof Player)
                    .forEach(e -> {
                        ((Player) e).damage(15 + level, zombie);
                        ((Player) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                    });
            }, i);
        }
    }

    private void patientZeroPhase2(Player target) {
        // Invocation massive
        playSound(Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location loc = getRandomNearbyLocation(10);
                if (loc != null && plugin.getZombieManager() != null) {
                    ZombieType summonType = random.nextFloat() < 0.3f ? ZombieType.BERSERKER : ZombieType.WALKER;
                    plugin.getZombieManager().spawnZombie(summonType, loc, level - 2);
                    playParticles(Particle.SOUL, loc, 20, 0.5, 0.5, 0.5);
                }
            }, idx * 5L);
        }
    }

    private void patientZeroPhase3(Player target) {
        // Patterns chaotiques
        isExecutingPattern = true;

        // Mur de corruption
        for (int angle = 0; angle < 360; angle += 30) {
            final int a = angle;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double rad = Math.toRadians(a);
                for (int dist = 0; dist < 15; dist++) {
                    Location loc = zombie.getLocation().add(Math.cos(rad) * dist, 0.5, Math.sin(rad) * dist);
                    playParticles(Particle.DRAGON_BREATH, loc, 5, 0.2, 0.2, 0.2);
                    loc.getWorld().getNearbyEntities(loc, 1, 1, 1).stream()
                        .filter(e -> e instanceof Player)
                        .forEach(e -> ((Player) e).damage(10, zombie));
                }
            }, (a / 30) * 5L);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isExecutingPattern = false, 70L);
    }

    private void patientZeroPhase4(Player target) {
        // Désespoir - tout à la fois
        patientZeroPhase1(target);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> patientZeroPhase2(target), 30L);
    }

    private void patientZeroUltimate(Player target) {
        isExecutingPattern = true;

        // L'Apocalypse
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§4§lL'APOCALYPSE", "§cPatient Zéro libère sa puissance", 10, 40, 10);
            }
        }

        playSound(Sound.ENTITY_WITHER_SPAWN, 2f, 0.3f);
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);

        // Onde de destruction
        for (int wave = 0; wave < 8; wave++) {
            final int w = wave;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;

                double radius = 5 + w * 4;
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    Location loc = zombie.getLocation().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
                    playParticles(Particle.DRAGON_BREATH, loc, 10, 0.5, 0.5, 0.5);
                    playParticles(Particle.SOUL_FIRE_FLAME, loc, 5, 0.2, 0.2, 0.2);
                }

                // Dégâts massifs
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), radius + 2, 5, radius + 2).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .filter(p -> Math.abs(p.getLocation().distance(zombie.getLocation()) - radius) < 3)
                    .forEach(p -> {
                        p.damage(30 + level * 2, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                        Vector knockback = p.getLocation().toVector()
                            .subtract(zombie.getLocation().toVector()).normalize()
                            .multiply(2).setY(0.8);
                        p.setVelocity(knockback);
                    });
            }, w * 15L);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isExecutingPattern = false, 150L);
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    private boolean canUseSpecialAttack() {
        return System.currentTimeMillis() - lastSpecialAttack >= specialCooldown && !isExecutingPattern;
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

    @Override
    public void onAttack(Player target) {
        currentTarget = target;
        // Dégâts bonus de boss
        double bonusDamage = switch (zombieType.getCategory()) {
            case MINIBOSS -> 5;
            case ZONE_BOSS -> 8;
            case FINAL_BOSS -> 12;
            default -> 3;
        };
        target.damage(bonusDamage, zombie);
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Contre-attaque de boss
        if (attacker instanceof Player player && random.nextFloat() < 0.15f) {
            player.damage(5, zombie);
            playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Nettoyer la bossbar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }

        // Nettoyer le système de Boss Bar Dynamique pour tous les joueurs
        if (plugin.getDynamicBossBarManager() != null) {
            for (UUID uuid : playersInFight) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    plugin.getDynamicBossBarManager().removeBoss(p, zombie.getUniqueId());
                }
            }
        }

        // Effets de mort épiques
        playSound(Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
        playSound(Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 1.2f);
        playParticles(Particle.EXPLOSION_EMITTER, zombie.getLocation(), 10, 2, 2, 2);
        playParticles(Particle.SOUL, zombie.getLocation(), 100, 3, 3, 3);

        // Message de victoire
        for (UUID uuid : playersInFight) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendTitle("§a§lVICTOIRE!", "§7" + zombieType.getDisplayName() + " vaincu!", 10, 60, 20);
                playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }
}
