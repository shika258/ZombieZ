package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

/**
 * Passif "Instinct de Meute" du Loup Spectral
 * Plus il y a d'ennemis proches, plus les dégâts augmentent
 */
@Getter
public class PackHunterPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePerEnemy;  // Bonus par ennemi (3% = 0.03)
    private final double maxBonus;         // Bonus maximum (15% = 0.15)
    private final int detectionRadius;     // Rayon de détection (8 blocs)

    public PackHunterPassive(String id, String name, String desc, double perEnemy, double max, int radius) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePerEnemy = perEnemy;
        this.maxBonus = max;
        this.detectionRadius = radius;
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    /**
     * Compte les ennemis dans le rayon de détection
     */
    private int countNearbyEnemies(Player player) {
        int count = 0;
        for (var entity : player.getNearbyEntities(detectionRadius, detectionRadius, detectionRadius)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Calcule le bonus de dégâts actuel
     */
    public double getCurrentBonus(Player player, PetData petData) {
        int enemies = countNearbyEnemies(player);
        double bonus = enemies * damagePerEnemy * petData.getStatMultiplier();
        return Math.min(bonus, maxBonus * petData.getStatMultiplier());
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        int enemies = countNearbyEnemies(player);
        if (enemies == 0) {
            return damage;
        }

        double bonus = enemies * damagePerEnemy * petData.getStatMultiplier();
        double cappedBonus = Math.min(bonus, maxBonus * petData.getStatMultiplier());

        // Effet visuel quand le bonus est actif
        if (cappedBonus > 0.05) {
            // Particules de loup spectral autour du joueur
            player.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 3,
                new Particle.DustOptions(Color.fromRGB(100, 200, 255), 0.8f));
        }

        return damage * (1 + cappedBonus);
    }

    @Override
    public void applyPassive(Player player, PetData petData) {
        int enemies = countNearbyEnemies(player);
        if (enemies >= 3) {
            // Effet visuel de meute active (yeux de loup)
            player.spawnParticle(Particle.DUST, player.getLocation().add(0, 1.8, 0), 2,
                new Particle.DustOptions(Color.fromRGB(50, 150, 255), 0.5f));
        }
    }
}
