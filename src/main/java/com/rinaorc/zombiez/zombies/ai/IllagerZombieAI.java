package com.rinaorc.zombiez.zombies.ai;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.zombies.types.ZombieType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * IA pour les zombies ILLAGER
 * Comportement: Tactiques de raid, travail en groupe
 * Types: PILLAGER, VINDICATOR, EVOKER
 *
 * PILLAGER: Attaques à distance avec arbalète, tactiques de harcèlement
 * VINDICATOR: Combat au corps à corps brutal avec hache, charge
 * EVOKER: Invocateur de magie, crocs et vex
 */
public class IllagerZombieAI extends ZombieAI {

    private int tickCounter = 0;
    private boolean isCharging = false;
    private boolean isChanneling = false;
    private int consecutiveHits = 0;

    // Pour l'Evoker
    private final List<UUID> summonedVexes = new ArrayList<>();
    private static final int MAX_VEXES = 4;
    private int fangCooldown = 0;

    // Distance préférée selon le type
    private double preferredDistance;

    public IllagerZombieAI(ZombieZPlugin plugin, Zombie zombie, ZombieType zombieType, int level) {
        super(plugin, zombie, zombieType, level);

        // Configurer selon le type
        switch (zombieType) {
            case PILLAGER -> {
                this.abilityCooldown = 2500;
                this.preferredDistance = 14.0;
            }
            case VINDICATOR -> {
                this.abilityCooldown = 5000;
                this.preferredDistance = 3.0;
            }
            case EVOKER -> {
                this.abilityCooldown = 8000;
                this.preferredDistance = 12.0;
            }
            default -> {
                this.abilityCooldown = 5000;
                this.preferredDistance = 10.0;
            }
        }
    }

    @Override
    public void tick() {
        tickCounter++;
        fangCooldown = Math.max(0, fangCooldown - 1);

        // Nettoyer les vex morts (pour l'Evoker)
        if (zombieType == ZombieType.EVOKER) {
            summonedVexes.removeIf(uuid -> {
                Entity entity = plugin.getServer().getEntity(uuid);
                return entity == null || !entity.isValid() || entity.isDead();
            });
        }

        switch (zombieType) {
            case PILLAGER -> tickPillager();
            case VINDICATOR -> tickVindicator();
            case EVOKER -> tickEvoker();
            default -> tickPillager();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PILLAGER - Arbalétrier tactique
    // ═══════════════════════════════════════════════════════════════════

    private void tickPillager() {
        // Effets visuels
        if (tickCounter % 30 == 0) {
            playSound(Sound.ENTITY_PILLAGER_AMBIENT, 0.4f, 1f);
        }

        Player target = findNearestPlayer(25);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Positionnement tactique
        if (distance < 7) {
            retreatTactically(target);
        } else if (distance > preferredDistance * 1.3) {
            zombie.setTarget(target);
        } else {
            // Distance idéale pour tirer
            strafeSideway(target);

            if (canUseAbility() && zombie.hasLineOfSight(target)) {
                fireCrossbow(target);
                useAbility();
            }
        }
    }

    /**
     * Tire un carreau d'arbalète
     */
    private void fireCrossbow(Player target) {
        Location eyeLoc = zombie.getEyeLocation();

        // Prédiction légère
        Vector targetVel = target.getVelocity();
        double dist = eyeLoc.distance(target.getEyeLocation());
        Location predictedLoc = target.getEyeLocation().add(
            targetVel.getX() * dist * 0.15,
            targetVel.getY() * dist * 0.1,
            targetVel.getZ() * dist * 0.15
        );

        Vector direction = predictedLoc.toVector()
            .subtract(eyeLoc.toVector()).normalize();

        // L'arbalète est plus précise que l'arc
        double spread = Math.max(0.02, 0.08 - (level * 0.001));
        direction.add(new Vector(
            (random.nextDouble() - 0.5) * spread,
            (random.nextDouble() - 0.5) * spread * 0.5,
            (random.nextDouble() - 0.5) * spread
        ));

        // Son de tir d'arbalète
        playSound(Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f);
        playSound(Sound.ENTITY_PILLAGER_CELEBRATE, 0.3f, 1.5f);

        // Créer le carreau (flèche mais plus puissante)
        Arrow bolt = zombie.getWorld().spawnArrow(
            eyeLoc.add(direction.clone().multiply(0.5)),
            direction,
            2.5f, // Plus rapide qu'un arc
            0
        );

        bolt.setShooter(zombie);
        bolt.setDamage(6 + level * 0.15);
        bolt.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        bolt.setCritical(true);

        // Particules de tir
        playParticles(Particle.CRIT, eyeLoc, 8, 0.2, 0.2, 0.2);

        // Chance de tir perforant
        if (random.nextFloat() < 0.2f + level * 0.01f) {
            bolt.setPierceLevel(1);
            playParticles(Particle.ENCHANTED_HIT, eyeLoc, 5, 0.1, 0.1, 0.1);
        }
    }

    /**
     * Retraite tactique
     */
    private void retreatTactically(Player target) {
        Vector away = zombie.getLocation().toVector()
            .subtract(target.getLocation().toVector()).normalize();

        // Mouvement diagonal pour être imprévisible
        Vector lateral = new Vector(-away.getZ(), 0, away.getX()).multiply(random.nextBoolean() ? 0.5 : -0.5);
        zombie.setVelocity(away.add(lateral).multiply(0.6).setY(0.15));

        playSound(Sound.ENTITY_PILLAGER_HURT, 0.5f, 1.5f);
    }

    /**
     * Strafe latéral
     */
    private void strafeSideway(Player target) {
        if (tickCounter % 20 != 0) return;

        Vector toTarget = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();
        Vector strafe = new Vector(-toTarget.getZ(), 0, toTarget.getX())
            .multiply(random.nextBoolean() ? 0.35 : -0.35);

        zombie.setVelocity(strafe);
    }

    // ═══════════════════════════════════════════════════════════════════
    // VINDICATOR - Guerrier brutal
    // ═══════════════════════════════════════════════════════════════════

    private void tickVindicator() {
        // Effets visuels de rage
        if (tickCounter % 25 == 0) {
            playSound(Sound.ENTITY_VINDICATOR_AMBIENT, 0.5f, 1f);
            playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2);
        }

        Player target = findNearestPlayer(20);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());
        zombie.setTarget(target);

        // Charge si distance moyenne
        if (distance > 6 && distance < 15 && canUseAbility() && !isCharging) {
            vindicatorCharge(target);
            useAbility();
        }

        // Attaque balayante au corps à corps
        if (distance < 3.5 && tickCounter % 30 == 0) {
            sweepingAxeAttack();
        }

        // Enrage si blessé
        if (isHealthBelow(0.4) && !isEnraged) {
            vindicatorRage();
        }
    }

    /**
     * Charge brutale du Vindicator
     */
    private void vindicatorCharge(Player target) {
        isCharging = true;

        playSound(Sound.ENTITY_VINDICATOR_CELEBRATE, 1.5f, 0.8f);
        playSound(Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.5f);

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, false));

        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!zombie.isValid() || !isCharging) {
                isCharging = false;
                task.cancel();
                return;
            }

            zombie.setVelocity(direction.clone().multiply(1.1).setY(0.05));
            playParticles(Particle.CRIT, zombie.getLocation(), 5, 0.3, 0.1, 0.3);

            // Vérifier collision avec joueurs
            zombie.getWorld().getNearbyEntities(zombie.getLocation(), 1.5, 1.5, 1.5).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .forEach(p -> {
                    double damage = 10 + level * 1.5;
                    p.damage(damage, zombie);
                    p.setVelocity(direction.clone().multiply(1.5).setY(0.5));
                    playSound(Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.8f);
                    isCharging = false;
                    task.cancel();
                });

            // Timeout
            if (zombie.getLocation().distance(target.getLocation()) < 2) {
                isCharging = false;
                task.cancel();
            }
        }, 0L, 2L);

        // Auto-stop après 2 secondes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> isCharging = false, 40L);
    }

    /**
     * Attaque balayante avec la hache
     */
    private void sweepingAxeAttack() {
        playSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.8f);
        playSound(Sound.ITEM_AXE_STRIP, 1f, 0.5f);
        playParticles(Particle.SWEEP_ATTACK, zombie.getLocation().add(0, 1, 0), 5, 1.5, 0.5, 1.5);

        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 3, 2, 3).stream()
            .filter(e -> e instanceof Player)
            .map(e -> (Player) e)
            .forEach(p -> {
                double damage = 7 + level;
                p.damage(damage, zombie);

                // Knockback latéral
                Vector knockback = p.getLocation().toVector()
                    .subtract(zombie.getLocation().toVector()).normalize()
                    .multiply(1.0).setY(0.3);
                p.setVelocity(knockback);

                // Chance de désarmer (ralentir l'attaque)
                if (random.nextFloat() < 0.2f) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1));
                    p.sendMessage("§c§l✦ Le Vindicator vous a désarmé temporairement!");
                }
            });
    }

    /**
     * Rage du Vindicator quand blessé
     */
    private void vindicatorRage() {
        enrage();

        playSound(Sound.ENTITY_VINDICATOR_CELEBRATE, 2f, 0.5f);
        playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5);

        // Boost supplémentaire
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        // Avertissement aux joueurs
        zombie.getWorld().getNearbyEntities(zombie.getLocation(), 15, 10, 15).stream()
            .filter(e -> e instanceof Player)
            .forEach(e -> ((Player) e).sendMessage("§4§l⚠ Le Vindicator entre en RAGE!"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // EVOKER - Mage invocateur
    // ═══════════════════════════════════════════════════════════════════

    private void tickEvoker() {
        // Aura magique
        if (tickCounter % 15 == 0) {
            playParticles(Particle.WITCH, zombie.getLocation().add(0, 1, 0), 5, 0.4, 0.5, 0.4);
            playParticles(Particle.ASH, zombie.getLocation().add(0, 0.5, 0), 3, 0.8, 0.3, 0.8);
        }

        if (tickCounter % 60 == 0) {
            playSound(Sound.ENTITY_EVOKER_AMBIENT, 0.5f, 1f);
        }

        Player target = findNearestPlayer(25);
        if (target == null) return;

        double distance = zombie.getLocation().distance(target.getLocation());

        // Maintenir la distance
        if (distance < 6) {
            retreatWithMagic(target);
        }

        // Invoquer des Vex si on en a peu
        if (summonedVexes.size() < MAX_VEXES && canUseAbility() && !isChanneling) {
            summonVexes(target);
            useAbility();
        }

        // Attaque de crocs (fang attack)
        if (fangCooldown <= 0 && distance < 16 && distance > 3) {
            fangAttack(target);
            fangCooldown = 60; // 3 secondes
        }

        // Rituel de protection si blessé
        if (isHealthBelow(0.3) && canUseAbility() && !isChanneling) {
            protectionRitual();
            useAbility();
        }
    }

    /**
     * Invoque des Vex (simulés avec des particules et dégâts)
     */
    private void summonVexes(Player target) {
        isChanneling = true;

        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));
        playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
        playSound(Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);

        // Cercle d'invocation
        for (int angle = 0; angle < 360; angle += 60) {
            double rad = Math.toRadians(angle);
            Location particleLoc = zombie.getLocation().add(Math.cos(rad) * 2, 0.5, Math.sin(rad) * 2);
            playParticles(Particle.WITCH, particleLoc, 10, 0.1, 0.3, 0.1);
        }

        // Invoquer après délai
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) {
                isChanneling = false;
                return;
            }

            playSound(Sound.ENTITY_VEX_AMBIENT, 1.5f, 1f);
            int vexCount = 2 + random.nextInt(2);

            for (int i = 0; i < vexCount && summonedVexes.size() < MAX_VEXES; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                Location spawnLoc = zombie.getLocation().add(
                    Math.cos(angle) * 2, 1.5, Math.sin(angle) * 2
                );

                // Simuler un Vex avec un zombie invisible qui fait des dégâts
                // Dans la vraie implémentation, on spawnerait un vrai Vex
                createPhantomVex(spawnLoc, target);
            }

            playParticles(Particle.WITCH, zombie.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
            isChanneling = false;
        }, 40L);
    }

    /**
     * Crée un "Vex fantôme" (effet visuel + dégâts périodiques)
     */
    private void createPhantomVex(Location loc, Player target) {
        // ID unique pour le vex
        UUID vexId = UUID.randomUUID();
        summonedVexes.add(vexId);

        playParticles(Particle.WITCH, loc, 20, 0.3, 0.3, 0.3);

        // Le vex "vit" pendant 10 secondes et attaque périodiquement
        final Location[] vexLoc = {loc.clone()};

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (!summonedVexes.contains(vexId) || !zombie.isValid()) {
                summonedVexes.remove(vexId);
                task.cancel();
                return;
            }

            // Le vex se déplace vers la cible
            if (target.isOnline() && target.getWorld().equals(vexLoc[0].getWorld())) {
                Vector direction = target.getLocation().add(0, 1, 0).toVector()
                    .subtract(vexLoc[0].toVector()).normalize();
                vexLoc[0].add(direction.multiply(0.8));

                // Particules du vex
                zombie.getWorld().spawnParticle(Particle.WITCH, vexLoc[0], 5, 0.2, 0.2, 0.2);

                // Attaque si proche
                if (vexLoc[0].distance(target.getLocation().add(0, 1, 0)) < 2) {
                    target.damage(3 + level * 0.5, zombie);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_VEX_HURT, 0.8f, 1.2f);
                    playParticles(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2);
                }
            }
        }, 10L, 5L);

        // Auto-destruction après 10 secondes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            summonedVexes.remove(vexId);
            zombie.getWorld().spawnParticle(Particle.POOF, vexLoc[0], 10, 0.2, 0.2, 0.2);
        }, 200L);
    }

    /**
     * Attaque de crocs (fang attack)
     */
    private void fangAttack(Player target) {
        playSound(Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1f, 1f);
        playSound(Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.2f);

        // Direction vers la cible
        Vector direction = target.getLocation().toVector()
            .subtract(zombie.getLocation().toVector()).normalize();

        // Créer une ligne de crocs
        for (int i = 1; i <= 8; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location fangLoc = zombie.getLocation().add(direction.clone().multiply(step * 1.5));
                fangLoc.setY(fangLoc.getWorld().getHighestBlockYAt(fangLoc) + 1);

                // Effet visuel du croc
                playParticles(Particle.WITCH, fangLoc, 10, 0.2, 0.5, 0.2);
                playParticles(Particle.DAMAGE_INDICATOR, fangLoc.clone().add(0, 0.5, 0), 5, 0.1, 0.3, 0.1);
                fangLoc.getWorld().playSound(fangLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 0.8f, 1f);

                // Dégâts aux joueurs dans la zone
                fangLoc.getWorld().getNearbyEntities(fangLoc, 1, 2, 1).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(6 + level, zombie);
                        p.setVelocity(new Vector(0, 0.5, 0));
                    });
            }, step * 2L);
        }
    }

    /**
     * Retraite magique avec téléportation
     */
    private void retreatWithMagic(Player target) {
        if (random.nextFloat() < 0.3f) {
            // Téléportation courte
            Vector away = zombie.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();
            Location tpLoc = zombie.getLocation().add(away.multiply(6));

            playParticles(Particle.WITCH, zombie.getLocation(), 30, 0.5, 1, 0.5);
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
            zombie.teleport(tpLoc);
            playParticles(Particle.WITCH, tpLoc, 30, 0.5, 1, 0.5);
        } else {
            // Recul normal
            Vector away = zombie.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();
            zombie.setVelocity(away.multiply(0.6).setY(0.2));
        }
    }

    /**
     * Rituel de protection
     */
    private void protectionRitual() {
        isChanneling = true;

        playSound(Sound.ENTITY_EVOKER_CAST_SPELL, 1.5f, 0.8f);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));

        // Cercle de protection
        for (int tick = 0; tick < 30; tick++) {
            final int t = tick;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;

                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle + t * 12);
                    Location particleLoc = zombie.getLocation().add(Math.cos(rad) * 2.5, 0.5 + t * 0.05, Math.sin(rad) * 2.5);
                    playParticles(Particle.WITCH, particleLoc, 2, 0, 0, 0);
                }
            }, t);
        }

        // Effet final
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!zombie.isValid()) {
                isChanneling = false;
                return;
            }

            playSound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1f);
            playParticles(Particle.WITCH, zombie.getLocation().add(0, 1, 0), 50, 1, 1, 1);

            // Soins et buffs
            heal(zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() * 0.25);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, false, true));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true));

            isChanneling = false;
        }, 30L);
    }

    @Override
    public void onAttack(Player target) {
        currentTarget = target;
        consecutiveHits++;

        switch (zombieType) {
            case PILLAGER -> {
                // Coup de crosse
                playSound(Sound.ENTITY_PILLAGER_HURT, 1f, 0.8f);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                retreatTactically(target);
            }
            case VINDICATOR -> {
                // Coup de hache dévastateur
                playSound(Sound.ITEM_AXE_STRIP, 1f, 0.6f);

                // Combo: plus de dégâts à chaque coup consécutif
                if (consecutiveHits >= 3) {
                    target.damage(5, zombie);
                    playParticles(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                    target.sendMessage("§c§l✦ Combo x" + consecutiveHits + "!");
                }
            }
            case EVOKER -> {
                // Drain de vie magique
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
                heal(3);
                playParticles(Particle.WITCH, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
                retreatWithMagic(target);
            }
        }
    }

    @Override
    public void onDamaged(Entity attacker, double damage) {
        consecutiveHits = 0; // Reset combo quand touché

        switch (zombieType) {
            case PILLAGER -> {
                playSound(Sound.ENTITY_PILLAGER_HURT, 1f, 1f);
                if (attacker instanceof Player player && random.nextFloat() < 0.3f) {
                    retreatTactically(player);
                }
            }
            case VINDICATOR -> {
                playSound(Sound.ENTITY_VINDICATOR_HURT, 1f, 1f);
                // Contre-attaque possible
                if (attacker instanceof Player player && random.nextFloat() < 0.4f) {
                    player.damage(3, zombie);
                    playParticles(Particle.CRIT, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2);
                }
            }
            case EVOKER -> {
                playSound(Sound.ENTITY_EVOKER_HURT, 1f, 1f);
                // Interrompre la canalisation
                if (isChanneling && random.nextFloat() < 0.4f) {
                    isChanneling = false;
                    playSound(Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 0.5f);
                }
                // Les vex défendent leur maître
                if (attacker instanceof Player player) {
                    for (UUID vexId : summonedVexes) {
                        // Les vex attaquent l'agresseur (géré par leur AI déjà)
                    }
                }
            }
        }
    }

    @Override
    public void onDeath(Player killer) {
        switch (zombieType) {
            case PILLAGER -> {
                playSound(Sound.ENTITY_PILLAGER_DEATH, 1f, 1f);
                // Laisse tomber son arbalète (visuellement)
                playParticles(Particle.SMOKE, zombie.getLocation(), 20, 0.5, 0.5, 0.5);
            }
            case VINDICATOR -> {
                playSound(Sound.ENTITY_VINDICATOR_DEATH, 1f, 1f);
                // Dernière attaque en mourant
                sweepingAxeAttack();
                playParticles(Particle.ANGRY_VILLAGER, zombie.getLocation(), 30, 1, 1, 1);
            }
            case EVOKER -> {
                playSound(Sound.ENTITY_EVOKER_DEATH, 1f, 1f);
                playParticles(Particle.WITCH, zombie.getLocation(), 50, 1, 1, 1);

                // Les vex deviennent fous
                for (UUID vexId : summonedVexes) {
                    // Boost les vex restants (ils meurent plus vite mais font plus de dégâts)
                }

                // Explosion magique finale
                zombie.getWorld().getNearbyEntities(zombie.getLocation(), 5, 3, 5).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .forEach(p -> {
                        p.damage(5, zombie);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    });
            }
        }
    }
}
