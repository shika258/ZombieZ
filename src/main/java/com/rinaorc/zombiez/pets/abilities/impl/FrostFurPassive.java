package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Capacité passive Fourrure Glaciale - réduction de dégâts et retour de gel
 */
@Getter
public class FrostFurPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damageReduction;         // -20% = 0.20
    private final double reflectPercent;          // 5% des dégâts retournés = 0.05
    private final int slowDurationTicks;          // 1s = 20 ticks

    public FrostFurPassive(String id, String name, String desc, double reduction, double reflect, int slowTicks) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damageReduction = reduction;
        this.reflectPercent = reflect;
        this.slowDurationTicks = slowTicks;
    }

    @Override
    public boolean isPassive() { return true; }

    /**
     * Retourne le pourcentage de réduction de dégâts pour le listener
     */
    public double getDamageReduction(PetData petData) {
        return damageReduction + (petData.getStatMultiplier() - 1) * 0.05;
    }

    /**
     * Appelé quand le joueur reçoit des dégâts - applique le retour de gel
     */
    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // Trouver l'attaquant le plus proche (dans un rayon de 5 blocs)
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Monster monster) {
                // Calculer les dégâts de retour
                double adjustedReflect = reflectPercent + (petData.getStatMultiplier() - 1) * 0.03;
                double reflectDamage = damage * adjustedReflect;

                // Appliquer le ralentissement glacial
                int adjustedSlow = (int) (slowDurationTicks + (petData.getStatMultiplier() - 1) * 10);
                monster.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, adjustedSlow, 1, false, false));

                // Appliquer les dégâts de retour (gel)
                if (reflectDamage > 0) {
                    monster.damage(reflectDamage, player);

                    // Effet visuel de gel
                    world.spawnParticle(Particle.SNOWFLAKE, monster.getLocation().add(0, 1, 0),
                        10, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.BLOCK, monster.getLocation().add(0, 0.5, 0),
                        5, 0.2, 0.2, 0.2, 0, org.bukkit.Material.PACKED_ICE.createBlockData());
                }

                // On ne traite que le premier attaquant trouvé
                break;
            }
        }

        // Effet visuel de protection sur le joueur
        world.spawnParticle(Particle.SNOWFLAKE, playerLoc.add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.02);
        world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f);
    }
}
