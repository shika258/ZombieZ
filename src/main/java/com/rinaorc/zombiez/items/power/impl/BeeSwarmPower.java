package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Pouvoir: Essaim d'Abeilles
 *
 * Vos coups ont une chance d'invoquer un essaim d'abeilles qui pourchasse
 * les ennemis et inflige des dégâts sur la durée.
 *
 * Scaling ILVL:
 * - Dégâts par abeille: 2 * (ILVL / 10)
 * - Nombre d'abeilles: 3 + (ILVL / 25)
 * - Durée: 4s + (ILVL / 30)s
 */
public class BeeSwarmPower extends Power {

    // Paramètres de base (configurables)
    private double baseDamagePerBee = 2.0;
    private int baseBeeCount = 3;
    private int baseDurationTicks = 80; // 4 secondes

    // Paramètres de scaling
    private double damagePerILVL = 0.2;      // Dégâts par tranche de 10 ILVL
    private double beeCountPerILVL = 0.04;   // Nombre d'abeilles par ILVL (1 tous les 25 ILVL)
    private double durationPerILVL = 0.67;   // Durée en ticks par ILVL (1s tous les 30 ILVL)

    public BeeSwarmPower() {
        super("bee_swarm", "Essaim d'Abeilles",
            "Invoque des abeilles agressives qui pourchassent vos ennemis");

        this.baseProcChance = 0.12;  // 12% de chance de proc
        this.cooldownMs = 15000;     // 15 secondes de cooldown
        this.minimumRarity = Rarity.RARE;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double damagePerBee = calculateDamagePerBee(itemLevel);
        int beeCount = calculateBeeCount(itemLevel);
        int durationTicks = calculateDuration(itemLevel);

        // Position d'invocation (autour de la cible si présente, sinon du joueur)
        Location spawnCenter = target != null ? target.getLocation() : player.getLocation();

        // Effet visuel et sonore de spawn
        spawnCenter.getWorld().spawnParticle(
            Particle.FLAME,
            spawnCenter.clone().add(0, 1, 0),
            20,
            0.5, 0.5, 0.5,
            0.05
        );
        spawnCenter.getWorld().playSound(spawnCenter, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.5f, 1.2f);

        // Notification au joueur
        player.sendMessage("§e✦ §6Essaim d'Abeilles activé! §7(" + beeCount + " abeilles, §c" +
            String.format("%.1f", damagePerBee) + " dégâts§7)");

        // Liste pour tracker les abeilles
        List<Bee> bees = new ArrayList<>();

        // Invoquer les abeilles en cercle
        for (int i = 0; i < beeCount; i++) {
            double angle = (2 * Math.PI / beeCount) * i;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;

            Location beeLoc = spawnCenter.clone().add(x, 1, z);
            Bee bee = (Bee) player.getWorld().spawnEntity(beeLoc, EntityType.BEE);

            bee.setCustomName("§6✦ Abeille Colérique §6✦");
            bee.setCustomNameVisible(false);
            bee.setAnger(9999); // Très en colère
            bee.setHasStung(false);
            bee.setHasNectar(false);

            // Cibler la target initiale si présente
            if (target != null) {
                bee.setTarget(target);
            }

            bees.add(bee);

            // Effet de spawn pour chaque abeille
            beeLoc.getWorld().spawnParticle(
                Particle.FIREWORK,
                beeLoc,
                5,
                0.2, 0.2, 0.2,
                0.01
            );
        }

        // Task pour gérer le comportement des abeilles
        new BukkitRunnable() {
            int ticksElapsed = 0;
            final int maxTicks = durationTicks;

            @Override
            public void run() {
                if (ticksElapsed >= maxTicks) {
                    // Disparition des abeilles
                    for (Bee bee : bees) {
                        if (bee.isValid() && !bee.isDead()) {
                            Location loc = bee.getLocation();
                            loc.getWorld().spawnParticle(Particle.POOF, loc, 10, 0.3, 0.3, 0.3);
                            bee.remove();
                        }
                    }
                    cancel();
                    return;
                }

                ticksElapsed++;

                // Trouver une cible proche pour les abeilles sans cible
                if (ticksElapsed % 20 == 0) { // Toutes les secondes
                    Location searchCenter = target != null ? target.getLocation() : player.getLocation();

                    for (Bee bee : bees) {
                        if (!bee.isValid() || bee.isDead()) continue;

                        if (bee.getTarget() == null || !bee.getTarget().isValid()) {
                            // Chercher une nouvelle cible
                            LivingEntity newTarget = findNearestEnemy(bee.getLocation(), player, 15.0);
                            if (newTarget != null) {
                                bee.setTarget(newTarget);
                            }
                        }

                        // Infliger des dégâts périodiques à la cible
                        if (bee.getTarget() != null && bee.getTarget().isValid()) {
                            LivingEntity beeTarget = bee.getTarget();
                            double distance = bee.getLocation().distance(beeTarget.getLocation());

                            // Si proche de la cible, infliger des dégâts
                            if (distance < 2.0 && ticksElapsed % 10 == 0) { // Tous les 0.5s
                                beeTarget.damage(damagePerBee, player);

                                // Effet de poison léger
                                beeTarget.addPotionEffect(
                                    new PotionEffect(PotionEffectType.POISON, 40, 0, false, true)
                                );

                                // Effet visuel
                                beeTarget.getWorld().spawnParticle(
                                    Particle.DAMAGE_INDICATOR,
                                    beeTarget.getEyeLocation(),
                                    2,
                                    0.3, 0.3, 0.3
                                );

                                // Son de piqûre
                                bee.getWorld().playSound(
                                    bee.getLocation(),
                                    Sound.ENTITY_BEE_STING,
                                    0.5f,
                                    1.0f
                                );
                            }
                        }
                    }
                }

                // Particules visuelles autour des abeilles
                if (ticksElapsed % 5 == 0) {
                    for (Bee bee : bees) {
                        if (bee.isValid() && !bee.isDead()) {
                            bee.getWorld().spawnParticle(
                                Particle.FALLING_HONEY,
                                bee.getLocation(),
                                2,
                                0.1, 0.1, 0.1,
                                0
                            );
                        }
                    }
                }
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Trouve l'ennemi le plus proche
     */
    private LivingEntity findNearestEnemy(Location center, Player source, double range) {
        LivingEntity nearest = null;
        double nearestDistance = range;

        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof LivingEntity target && entity != source) {
                // Ne pas cibler les joueurs (sauf si PvP activé)
                if (target instanceof Player && !target.getWorld().getPVP()) {
                    continue;
                }

                // Ne pas cibler les abeilles elles-mêmes
                if (target instanceof Bee) {
                    continue;
                }

                double distance = center.distance(target.getLocation());
                if (distance < nearestDistance) {
                    nearest = target;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Calcule les dégâts par abeille selon l'ILVL
     */
    private double calculateDamagePerBee(int itemLevel) {
        return baseDamagePerBee + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le nombre d'abeilles selon l'ILVL
     */
    private int calculateBeeCount(int itemLevel) {
        return (int) (baseBeeCount + (itemLevel * beeCountPerILVL));
    }

    /**
     * Calcule la durée selon l'ILVL
     */
    private int calculateDuration(int itemLevel) {
        return (int) (baseDurationTicks + (itemLevel * durationPerILVL));
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts/abeille: §c" + String.format("%.1f", calculateDamagePerBee(itemLevel)));
        stats.add("§8Nombre d'abeilles: §e" + calculateBeeCount(itemLevel));
        stats.add("§8Durée: §e" + String.format("%.1f", calculateDuration(itemLevel) / 20.0) + "s");
        stats.add("§8Effet: §2Poison I");
        return stats;
    }
}
