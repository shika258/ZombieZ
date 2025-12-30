package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Ultimate de la Luciole Errante - Guérisseuse de Combat
 * Soigne instantanément et applique Régénération
 */
@Getter
public class LifePulseActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double instantHeal; // En coeurs
    private final int regenDurationSeconds;

    public LifePulseActive(String id, String name, String desc, double heal, int regenDuration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.instantHeal = heal;
        this.regenDurationSeconds = regenDuration;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 30;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Calculer le soin avec le multiplicateur de stats
        double actualHeal = instantHeal * petData.getStatMultiplier();

        // Soigner le joueur instantanément
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(currentHealth + (actualHeal * 2), maxHealth); // *2 car les coeurs = 2 HP
        player.setHealth(newHealth);

        // Appliquer Régénération I
        int regenTicks = regenDurationSeconds * 20;
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION,
            regenTicks,
            0, // Niveau 0 = Régénération I
            true,  // Ambient (particules réduites)
            true,  // Particules
            true   // Icône
        ));

        // Effets visuels - onde de soin
        player.getWorld().spawnParticle(Particle.HEART,
            player.getLocation().add(0, 1, 0),
            10, 0.5, 0.5, 0.5, 0);

        player.getWorld().spawnParticle(Particle.END_ROD,
            player.getLocation().add(0, 0.5, 0),
            25, 1, 0.5, 1, 0.05);

        // Son de soin
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);

        // Message
        player.sendMessage("§a[Pet] §7Pulse de Vie! §c+" + String.format("%.1f", actualHeal) + "❤ §7+ Régénération " + regenDurationSeconds + "s");

        return true;
    }
}
