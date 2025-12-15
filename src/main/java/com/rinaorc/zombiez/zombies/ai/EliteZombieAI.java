package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * IA pour les zombies ELITE et SPECIAL
 * Comportement: Combinaison de plusieurs capacités, très dangereux
 * Types: ARMORED_ELITE, CREAKING, CORRUPTED_WARDEN, ARCHON
 */
public class EliteZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private int phase = 1;
    private boolean isExecutingSpecial = false;

    public EliteZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);
        this.abilityCooldown = 10000;
        applyEliteBuffs();
    }

    private void applyEliteBuffs() {
        // Tous les élites ont des buffs de base
        var knockback = zombie.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.6);
        }

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }

    @Override
    public void tick() {
        tickCounter++;

        // Vérifier les changements de phase
        checkPhaseTransition();

        switch (zombieType) {
            case ARMORED_ELITE -> tickArmoredElite();
            case CREAKING -> tickCreaking();
            case CORRUPTED_WARDEN -> tickCorruptedWarden();
            case ARCHON -> tickArchon();
            default -> tickArmoredElite();
        }
    }

    private void checkPhaseTransition() {
        if (phase == 1 && isHealthBelow(0.5)) {
            phase = 2;
            onPhaseChange(2);
        } else if (phase == 2 && isHealthBelow(0.25)) {
            phase = 3;
            onPhaseChange(3);
        }
    }

    private void onPhaseChange(int newPhase) {
        playSound(Sound.ENTITY_WARDEN_ROAR, 1f, 1f);
        playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5);

        // Boost de stats
        var damage = zombie.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * 1.2);
        }

        var speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.1);
        }

        // Notify players
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 30, 20, 30).stream()
            .filter(e -> e instanceof Player)
            .forEach(e -> ((Player) e).sendMessage("§c§l⚠ " + zombieType.getDisplayName() + " entre en phase " + newPhase + "!"));
    }

    // ═══════════════════════════════════════════════════════════════
    // ARMORED ELITE
    // ═══════════════════════════════════════════════════════════════

    private void tickArmoredElite() {
        // Aura d'acier
        if (tickCounter % 30 == 0) {
            playParticles(Particle.SCRAPE, zombie.getLocation().add(0, 1, 0), 5, 0.4, 0.5, 0.4);
        }

        Player target = findNearestPlayer(15);
        if (target == null) return;

        // Abilities selon la phase
        if (canUseAbility()) {
            switch (phase) {
                case 1 -> shieldBash(target);
                case 2 -> armorPierce(target);
                case 3 -> ironFortress();
            }
            useAbility();
        }
    }

    private void shieldBash(Player target) {
        playSound(Sound.ITEM_SHIELD_BLOCK, 2f, 0.8f);

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();
        zombie.setVelocity(direction.multiply(1.5).setY(0.2));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) return;
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 2, 2, 2).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.damage(10 + level, zombie);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    Vector knockback = direction.clone().multiply(2).setY(0.5);
                    p.setVelocity(knockback);
                    playSound(Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
                });
        }, 10L);
    }

    private void armorPierce(Player target) {
        playSound(Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);

        // Attaque qui ignore l'armure
        double trueDamage = 8 + level * 1.5;
        target.damage(trueDamage, zombie);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));

        playParticles(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3);
    }

    private void ironFortress() {
        playSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE, 2f, 0.5f);

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3, false, true));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, true));

        // Épines
        playParticles(Particle.END_ROD, zombie.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5);
    }

    // ═══════════════════════════════════════════════════════════════
    // CREAKING (Mob spécial de Minecraft)
    // ═══════════════════════════════════════════════════════════════

    private void tickCreaking() {
        // Se fige quand regardé
        Player watcher = findWatchingPlayer();
        if (watcher != null) {
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 10, false, false));

            if (tickCounter % 20 == 0) {
                playParticles(Particle.ENCHANTED_HIT, zombie.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);
            }
        } else {
            // Se déplace rapidement quand non regardé
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 2, false, false));
        }

        // Son de craquement
        if (tickCounter % 40 == 0) {
            playSound(Sound.ENTITY_SKELETON_STEP, 0.5f, 0.3f);
        }

        if (canUseAbility() && watcher == null) {
            Player target = findNearestPlayer(15);
            if (target != null) {
                creakingStrike(target);
                useAbility();
            }
        }
    }

    private Player findWatchingPlayer() {
        return zombie.getWorld().getNearbyEntities(zombie.getLocation(), 20, 10, 20).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .filter(p -> {
                Vector toZombie = zombie.getLocation().toVector().subtract(p.getEyeLocation().toVector());
                Vector playerLook = p.getEyeLocation().getDirection();
                return toZombie.normalize().dot(playerLook.normalize()) > 0.8;
            })
            .findFirst()
            .orElse(null);
    }

    private void creakingStrike(Player target) {
        // Téléportation silencieuse
        Vector behind = target.getLocation().getDirection().multiply(-2);
        Location teleportLoc = target.getLocation().add(behind);

        playParticles(Particle.SMOKE, zombie.getLocation(), 20, 0.3, 1, 0.3);
        zombie.teleport(teleportLoc);
        playParticles(Particle.SMOKE, teleportLoc, 20, 0.3, 1, 0.3);

        // Attaque surprise
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (zombie.isValid()) {
                playSound(Sound.ENTITY_SKELETON_HURT, 2f, 0.3f);
                target.damage(12 + level, zombie);
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
            }
        }, 5L);
    }

    // ═══════════════════════════════════════════════════════════════
    // CORRUPTED WARDEN
    // ═══════════════════════════════════════════════════════════════

    private void tickCorruptedWarden() {
        // Aura sombre
        if (tickCounter % 10 == 0) {
            playParticles(Particle.SCULK_SOUL, zombie.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
            playParticles(Particle.SOUL, zombie.getLocation().add(0, 0.5, 0), 3, 0.8, 0.3, 0.8);

            // Darkness aux joueurs proches
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 8, 5, 8).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false)));
        }

        // Heartbeat
        if (tickCounter % 40 == 0) {
            playSound(Sound.ENTITY_WARDEN_HEARTBEAT, 2f, 1f);
        }

        Player target = findNearestPlayer(20);
        if (target == null) return;

        if (canUseAbility()) {
            switch (phase) {
                case 1 -> sonicBoom(target);
                case 2 -> darknessSurge();
                case 3 -> wardenRoar();
            }
            useAbility();
        }
    }

    private void sonicBoom(Player target) {
        playSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        Location rayLoc = zombie.getEyeLocation().clone();
        for (int i = 0; i < 20; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location currentLoc = rayLoc.clone().add(direction.clone().multiply(step));
                playParticles(Particle.SONIC_BOOM, currentLoc, 1, 0, 0, 0);

                currentLoc.getWorld().getNearbyEntities(currentLoc, 1.5, 1.5, 1.5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(15 + level * 2, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                    });
            }, i);
        }
    }

    private void darknessSurge() {
        playSound(Sound.ENTITY_WARDEN_ROAR, 1.5f, 0.8f);

        // Onde d'obscurité
        for (int ring = 0; ring < 5; ring++) {
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                double radius = 3 + r * 3;
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location particleLoc = zombie.getLocation().add(Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius);
                    playParticles(Particle.SCULK_SOUL, particleLoc, 3, 0.2, 0.2, 0.2);
                }
            }, r * 3L);
        }

        // Effets
        applyAreaEffect(15, PotionEffectType.DARKNESS, 200, 0);
        applyAreaEffect(15, PotionEffectType.BLINDNESS, 60, 0);
    }

    private void wardenRoar() {
        playSound(Sound.ENTITY_WARDEN_ROAR, 3f, 0.5f);
        playParticles(Particle.SONIC_BOOM, zombie.getLocation().add(0, 1.5, 0), 10, 1, 1, 1);

        // Dégâts et knockback massifs
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 10, 5, 10).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                p.damage(20 + level * 2, zombie);
                Vector knockback = p.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(3).setY(1);
                p.setVelocity(knockback);
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
            });
    }

    // ═══════════════════════════════════════════════════════════════
    // ARCHON (Zombie légendaire)
    // ═══════════════════════════════════════════════════════════════

    private void tickArchon() {
        // Aura divine corrompue
        if (tickCounter % 8 == 0) {
            playParticles(Particle.END_ROD, zombie.getLocation().add(0, 1.5, 0), 5, 0.5, 0.8, 0.5);
            playParticles(Particle.SOUL_FIRE_FLAME, zombie.getLocation().add(0, 0.5, 0), 3, 0.5, 0.2, 0.5);
        }

        // Lévitation permanente
        zombie.setGravity(true);

        Player target = findNearestPlayer(25);
        if (target == null) return;

        if (canUseAbility()) {
            switch (phase) {
                case 1 -> divineBolt(target);
                case 2 -> holyNova();
                case 3 -> judgement(target);
            }
            useAbility();
        }

        // Attaque de base améliorée
        if (tickCounter % 60 == 0) {
            energyBurst(target);
        }
    }

    private void divineBolt(Player target) {
        playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);

        for (int i = 0; i < 3; i++) {
            final int bolt = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;

                Location strikeLoc = target.getLocation();
                playParticles(Particle.END_ROD, strikeLoc.clone().add(0, 10, 0), 50, 0.5, 5, 0.5);

                // Impact
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
                    playParticles(Particle.FLASH, strikeLoc, 1, 0, 0, 0);

                    strikeLoc.getWorld().getNearbyEntities(strikeLoc, 2, 2, 2).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> {
                            p.damage(10 + level, zombie);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 1));
                        });
                }, 10L);
            }, bolt * 15L);
        }
    }

    private void holyNova() {
        isExecutingSpecial = true;
        playSound(Sound.BLOCK_BEACON_ACTIVATE, 2f, 1.5f);

        // Préparation
        for (int i = 0; i < 30; i++) {
            final int tick = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;
                playParticles(Particle.END_ROD, zombie.getLocation().add(0, 1 + tick * 0.1, 0), 10, 0.3, 0.1, 0.3);
            }, i);
        }

        // Explosion
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) return;

            playSound(Sound.ENTITY_GENERIC_EXPLODE, 2f, 1.5f);
            playParticles(Particle.END_ROD, zombie.getLocation(), 100, 5, 2, 5);

            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 12, 6, 12).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    p.damage(18 + level * 2, zombie);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                });

            isExecutingSpecial = false;
        }, 30L);
    }

    private void judgement(Player target) {
        playSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1.5f);

        target.sendMessage("§6§l✦ JUGEMENT ✦");

        // Marqueur sur la cible
        for (int i = 0; i < 40; i++) {
            final int tick = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline()) return;
                playParticles(Particle.END_ROD, target.getLocation().add(0, 2.5, 0), 10, 0.3, 0.1, 0.3);
            }, i);
        }

        // Exécution
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid() || !target.isOnline()) return;

            playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.5f);
            playParticles(Particle.FLASH, target.getLocation(), 1, 0, 0, 0);
            playParticles(Particle.END_ROD, target.getLocation(), 100, 1, 2, 1);

            // Dégâts massifs basés sur la vie manquante
            var maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double missingHealth = maxHealth.getValue() - target.getHealth();
                double damage = 15 + level * 2 + missingHealth * 0.3;
                target.damage(damage, zombie);
            }
        }, 40L);
    }

    private void energyBurst(Player target) {
        playSound(Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getEyeLocation().toVector()).normalize();

        Location projectileLoc = zombie.getEyeLocation().clone();
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            projectileLoc.add(direction.clone().multiply(2));
            playParticles(Particle.END_ROD, projectileLoc, 5, 0.1, 0.1, 0.1);

            if (projectileLoc.distance(target.getLocation()) < 2) {
                target.damage(6 + level, zombie);
                playParticles(Particle.END_ROD, projectileLoc, 20, 0.5, 0.5, 0.5);
                task.cancel();
            }
            if (projectileLoc.distance(zombie.getLocation()) > 40) task.cancel();
        }, 0L, 1L);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;

        // Les élites font toujours des dégâts bonus
        target.damage(3 + level * 0.5, zombie);

        // Effet spécial selon le type
        switch (zombieType) {
            case CORRUPTED_WARDEN -> target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
            case ARCHON -> target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10, 0));
            case CREAKING -> target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0));
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        // Contre-attaque élite
        if (attacker instanceof Player player && random.nextFloat() < 0.2f) {
            player.damage(4, zombie);
            playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3);
        }
    }

    @Override
    public void onDeath(Player killer) {
        playSound(Sound.ENTITY_WARDEN_DEATH, 2f, 0.8f);
        playParticles(Particle.SOUL, zombie.getLocation(), 50, 2, 2, 2);

        // Explosion finale selon le type
        switch (zombieType) {
            case CORRUPTED_WARDEN -> {
                applyAreaEffect(8, PotionEffectType.DARKNESS, 200, 0);
                playParticles(Particle.SCULK_SOUL, zombie.getLocation(), 100, 3, 2, 3);
            }
            case ARCHON -> {
                playParticles(Particle.END_ROD, zombie.getLocation(), 100, 3, 3, 3);
                // Dernière bénédiction corrompue
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 10, 5, 10).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2));
                        p.damage(10, zombie);
                    });
            }
        }
    }
}
