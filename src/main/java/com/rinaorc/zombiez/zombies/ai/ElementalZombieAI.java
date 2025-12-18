package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies ELEMENTAL
 * Comportement: Pouvoirs basés sur un élément (feu, glace, foudre)
 * Types: FROZEN, YETI, WENDIGO, DEMON, INFERNAL
 */
public class ElementalZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isChannelingElement = false;

    public ElementalZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 8000;
        applyElementalAura();
    }

    /**
     * Applique l'aura élémentaire de base
     */
    private void applyElementalAura() {
        switch (zombieType) {
            case FROZEN, YETI, WENDIGO -> {
                zombie.addPotionEffect(
                        new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
            case DEMON, INFERNAL -> {
                zombie.setFireTicks(0); // Immunité au feu
            }
        }
    }

    @Override
    public void tick() {
        tickCounter++;

        switch (zombieType) {
            case FROZEN -> tickFrozen();
            case YETI -> tickYeti();
            case WENDIGO -> tickWendigo();
            case DEMON -> tickDemon();
            case INFERNAL -> tickInfernal();
            default -> tickFrozen();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÉLÉMENTAIRES DE GLACE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Frozen: Zombie de glace, gèle les joueurs au contact
     */
    private void tickFrozen() {
        // Aura de froid
        if (tickCounter % 15 == 0) {
            playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);

            // Ralentir les joueurs proches
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 4, 3, 4).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false)));
        }

        // Souffle gelé
        if (canUseAbility()) {
            Player target = findNearestPlayer(10);
            if (target != null) {
                frostBreath(target);
                useAbility();
            }
        }
    }

    /**
     * Yeti: Grosse créature de glace, tempête de neige
     */
    private void tickYeti() {
        // Aura de tempête
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(0, 1.5, 0), 10, 1.5, 1, 1.5);
            playParticles(Particle.CLOUD, zombie.getLocation().add(0, 2, 0), 3, 1, 0.5, 1);
        }

        // Tempête de neige
        if (canUseAbility()) {
            Player target = findNearestPlayer(15);
            if (target != null) {
                snowStorm();
                useAbility();
            }
        }

        // Coup de griffe glacé au corps à corps
        Player nearTarget = findNearestPlayer(3);
        if (nearTarget != null && tickCounter % 30 == 0) {
            iceSlash(nearTarget);
        }
    }

    /**
     * Wendigo: Chasseur affamé du froid
     */
    private void tickWendigo() {
        // Aura de faim
        if (tickCounter % 20 == 0) {
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3);
        }

        Player target = findNearestPlayer(25);
        if (target == null)
            return;

        // Traque rapide
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));

        // Hurlement glaçant
        if (canUseAbility()) {
            chillingHowl(target);
            useAbility();
        }

        // Attaque vorace
        double distance = zombie.getLocation().distance(target.getLocation());
        if (distance < 4 && tickCounter % 40 == 0) {
            devouringBite(target);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ÉLÉMENTAIRES DE FEU
    // ═══════════════════════════════════════════════════════════════

    /**
     * Demon: Entité démoniaque de feu
     */
    private void tickDemon() {
        zombie.setFireTicks(0);

        // Aura de flammes
        if (tickCounter % 10 == 0) {
            playParticles(Particle.FLAME, zombie.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4);
            playParticles(Particle.SMOKE, zombie.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3);

            // Enflammer les joueurs proches
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2.5, 2, 2.5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> p.setFireTicks(Math.max(p.getFireTicks(), 40)));
        }

        // Boule de feu
        if (canUseAbility()) {
            Player target = findNearestPlayer(15);
            if (target != null) {
                fireballBarrage(target);
                useAbility();
            }
        }
    }

    /**
     * Infernal: Démon majeur de flammes
     */
    private void tickInfernal() {
        zombie.setFireTicks(0);

        // Aura infernale intense
        if (tickCounter % 8 == 0) {
            playParticles(Particle.FLAME, zombie.getLocation().add(0, 1, 0), 15, 0.6, 0.8, 0.6);
            playParticles(Particle.LAVA, zombie.getLocation(), 2, 0.5, 0.1, 0.5);

            // Zone de chaleur
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 4, 3, 4).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.setFireTicks(Math.max(p.getFireTicks(), 60));
                        p.damage(1, zombie);
                    });
        }

        // Nova infernale
        if (canUseAbility()) {
            Player target = findNearestPlayer(12);
            if (target != null) {
                infernalNova();
                useAbility();
            }
        }

        // Pilier de feu sous les joueurs
        if (tickCounter % 100 == 0) {
            Player target = findNearestPlayer(15);
            if (target != null) {
                flamePillar(target.getLocation());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ABILITIES DE GLACE
    // ═══════════════════════════════════════════════════════════════

    private void frostBreath(Player target) {
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2f);

        Vector direction = target.getLocation().toVector()
                .subtract(zombie.getLocation().toVector()).normalize();

        Location breathLoc = zombie.getEyeLocation().clone();
        for (int i = 0; i < 10; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location currentLoc = breathLoc.clone().add(direction.clone().multiply(step * 0.8));
                playParticles(Particle.SNOWFLAKE, currentLoc, 20, 0.5, 0.5, 0.5);
                playParticles(Particle.CLOUD, currentLoc, 5, 0.3, 0.3, 0.3);

                currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> {
                            p.damage(4 + level, zombie);
                            p.setFreezeTicks(p.getMaxFreezeTicks());
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                        });
            }, i * 2L);
        }
    }

    private void snowStorm() {
        playSound(Sound.WEATHER_RAIN, 2f, 0.5f);
        isChannelingElement = true;

        // Tempête de 5 secondes
        for (int second = 0; second < 5; second++) {
            final int tick = second;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid())
                    return;

                // Particules de tempête
                for (int i = 0; i < 30; i++) {
                    double x = (random.nextDouble() - 0.5) * 12;
                    double y = random.nextDouble() * 4;
                    double z = (random.nextDouble() - 0.5) * 12;
                    playParticles(Particle.SNOWFLAKE, zombie.getLocation().add(x, y, z), 1, 0, 0, 0);
                }

                // Effets aux joueurs dans la zone
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 8, 4, 8).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> {
                            p.damage(2, zombie);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                            p.setFreezeTicks(Math.min(p.getFreezeTicks() + 40, p.getMaxFreezeTicks()));
                        });
            }, tick * 20L);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isChannelingElement = false, 100L);
    }

    private void iceSlash(Player target) {
        playSound(Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
        playParticles(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);

        target.damage(8 + level, zombie);
        target.setFreezeTicks(target.getMaxFreezeTicks());
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
    }

    private void chillingHowl(Player target) {
        playSound(Sound.ENTITY_WOLF_HOWL, 1.5f, 0.5f);
        playSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 2f);

        playParticles(Particle.SOUL, zombie.getLocation().add(0, 1.5, 0), 30, 1, 1, 1);

        // Effet de peur
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 10, 5, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                    p.sendMessage("§b§l❄ Le hurlement vous glace le sang!");
                });
    }

    private void devouringBite(Player target) {
        playSound(Sound.ENTITY_WOLF_GROWL, 1.5f, 0.5f);

        double damage = 10 + level * 1.5;
        target.damage(damage, zombie);

        // Drain de vie important
        heal(damage * 0.6);

        playParticles(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
        target.sendMessage("§c§l✦ Le Wendigo vous dévore!");
    }

    // ═══════════════════════════════════════════════════════════════
    // ABILITIES DE FEU
    // ═══════════════════════════════════════════════════════════════

    private void fireballBarrage(Player target) {
        playSound(Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);

        for (int i = 0; i < 3; i++) {
            final int shot = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid())
                    return;

                Vector direction = target.getEyeLocation().toVector()
                        .subtract(zombie.getEyeLocation().toVector()).normalize();
                // Ajouter un peu de spread
                direction.add(new Vector(
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.2));

                Location projectileLoc = zombie.getEyeLocation().clone();

                plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
                    projectileLoc.add(direction.clone().multiply(1.5));
                    playParticles(Particle.FLAME, projectileLoc, 10, 0.1, 0.1, 0.1);

                    if (projectileLoc.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                        // Impact
                        playSound(Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                        playParticles(Particle.FLAME, projectileLoc, 20, 0.5, 0.5, 0.5);
                        target.damage(5 + level, zombie);
                        target.setFireTicks(100);
                        task.cancel();
                    }
                    if (projectileLoc.distance(zombie.getLocation()) > 30)
                        task.cancel();
                }, 0L, 1L);
            }, shot * 5L);
        }
    }

    private void infernalNova() {
        playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        playSound(Sound.ENTITY_BLAZE_SHOOT, 2f, 0.5f);

        // Onde de feu expansive
        for (int ring = 0; ring < 6; ring++) {
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid())
                    return;

                double radius = 2 + r * 2;
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = zombie.getLocation().add(Math.cos(rad) * radius, 0.5,
                            Math.sin(rad) * radius);
                    playParticles(Particle.FLAME, particleLoc, 5, 0.2, 0.2, 0.2);
                    playParticles(Particle.LAVA, particleLoc, 1, 0, 0, 0);
                }

                // Dégâts à cette distance
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), radius + 1, 3, radius + 1).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .filter(p -> {
                            double dist = p.getLocation().distance(zombie.getLocation());
                            return dist >= radius - 1 && dist <= radius + 1;
                        })
                        .forEach(p -> {
                            p.damage(8 + level, zombie);
                            p.setFireTicks(200);
                            Vector knockback = p.getLocation().toVector()
                                    .subtract(zombie.getLocation().toVector()).normalize()
                                    .multiply(1.5).setY(0.5);
                            p.setVelocity(knockback);
                        });
            }, r * 3L);
        }
    }

    private void flamePillar(Location target) {
        // Indicateur
        playSound(Sound.BLOCK_FIRE_AMBIENT, 1f, 0.5f);
        for (int i = 0; i < 20; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                playParticles(Particle.FLAME, target.clone().add(0, 0.1, 0), 3, 1, 0, 1);
            }, i);
        }

        // Explosion après délai
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            playSound(Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

            // Pilier de feu
            for (int y = 0; y < 5; y++) {
                Location pillarLoc = target.clone().add(0, y, 0);
                playParticles(Particle.FLAME, pillarLoc, 30, 0.5, 0.2, 0.5);
                playParticles(Particle.LAVA, pillarLoc, 5, 0.3, 0.1, 0.3);
            }

            target.getWorld().getNearbyEntities(target, 2, 5, 2).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(10 + level, zombie);
                        p.setFireTicks(200);
                        p.setVelocity(new Vector(0, 1, 0));
                    });
        }, 25L);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        switch (zombieType) {
            case FROZEN, YETI, WENDIGO -> {
                target.setFreezeTicks(Math.min(target.getFreezeTicks() + 60, target.getMaxFreezeTicks()));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                playParticles(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
            }
            case DEMON, INFERNAL -> {
                target.setFireTicks(Math.max(target.getFireTicks(), 100));
                playParticles(Particle.FLAME, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
            }
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Contre-attaque élémentaire
        if (attacker instanceof Player player && random.nextFloat() < 0.25f) {
            switch (zombieType) {
                case FROZEN, YETI, WENDIGO -> {
                    player.setFreezeTicks(player.getMaxFreezeTicks() / 2);
                    playParticles(Particle.SNOWFLAKE, player.getLocation(), 20, 0.5, 0.5, 0.5);
                }
                case DEMON, INFERNAL -> {
                    player.setFireTicks(80);
                    playParticles(Particle.FLAME, player.getLocation(), 20, 0.5, 0.5, 0.5);
                }
            }
        }

        // Enrage quand critique
        if (isHealthBelow(0.3) && !isEnraged) {
            enrage();
        }
    }

    @Override
    public void onDeath(Player killer) {
        // Explosion élémentaire à la mort
        switch (zombieType) {
            case FROZEN, YETI, WENDIGO -> {
                playSound(Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
                playParticles(Particle.SNOWFLAKE, zombie.getLocation(), 100, 3, 2, 3);
                applyAreaEffect(5, PotionEffectType.SLOWNESS, 100, 2);

                // Geler tous les joueurs proches (sauf s'ils sont déjà morts)
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 5, 3, 5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .filter(p -> !p.isDead()) // CRITIQUE: Ne pas geler les joueurs morts!
                        .forEach(p -> p.setFreezeTicks(p.getMaxFreezeTicks()));
            }
            case DEMON, INFERNAL -> {
                playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                playParticles(Particle.FLAME, zombie.getLocation(), 100, 3, 2, 3);
                playParticles(Particle.LAVA, zombie.getLocation(), 30, 2, 1, 2);

                // Enflammer et repousser
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 5, 3, 5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> {
                            p.damage(6, zombie);
                            p.setFireTicks(200);
                            Vector knockback = p.getLocation().toVector()
                                    .subtract(zombie.getLocation().toVector()).normalize()
                                    .multiply(1.5).setY(0.6);
                            p.setVelocity(knockback);
                        });
            }
        }
    }
}
