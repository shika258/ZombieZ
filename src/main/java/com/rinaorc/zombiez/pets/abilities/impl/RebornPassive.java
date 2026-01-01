package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Capacité passive de renaissance - sauve le joueur de la mort
 */
@Getter
public class RebornPassive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double healthPercent;
    private final int cooldownSeconds;
    private final Map<UUID, Long> lastReborn = new ConcurrentHashMap<>();

    public RebornPassive(String id, String name, String desc, double health, int cooldown) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.healthPercent = health;
        this.cooldownSeconds = cooldown;
        PassiveAbilityCleanup.registerForCleanup(lastReborn);
    }

    @Override
    public boolean isPassive() {
        return true;
    }

    /**
     * Vérifie si la renaissance peut être déclenchée (pas en cooldown)
     */
    public boolean canTriggerReborn(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastReborn.getOrDefault(uuid, 0L);
        return now - last >= (cooldownSeconds * 1000L);
    }

    /**
     * Déclenche la renaissance et sauve le joueur de la mort
     */
    public void triggerReborn(Player player, PetData petData) {
        lastReborn.put(player.getUniqueId(), System.currentTimeMillis());

        // Calculer la santé restaurée
        double adjustedHealth = player.getMaxHealth() * healthPercent * petData.getStatMultiplier();
        player.setHealth(Math.max(1, Math.min(adjustedHealth, player.getMaxHealth())));

        // Effet visuel explosif de renaissance
        World world = player.getWorld();
        Location loc = player.getLocation();

        world.spawnParticle(Particle.FLAME, loc, 100, 1.5, 1.5, 1.5, 0.15);
        world.spawnParticle(Particle.LAVA, loc, 30, 1, 1, 1, 0.1);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.3);

        // Son de résurrection
        world.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);

        // Message
        int healthRestored = (int) (healthPercent * petData.getStatMultiplier() * 100);
        player.sendMessage("§a[Pet] §6§l RENAISSANCE! §7Ressuscité avec §c" + healthRestored + "% §7HP!");
        player.sendMessage("§a[Pet] §7Cooldown: §e" + cooldownSeconds + "s");
    }

    /**
     * Retourne le temps restant avant que la renaissance soit disponible (en secondes)
     */
    public int getCooldownRemaining(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastReborn.getOrDefault(uuid, 0L);
        long elapsed = (now - last) / 1000;
        return Math.max(0, cooldownSeconds - (int) elapsed);
    }

    @Override
    public void onDamageReceived(Player player, PetData petData, double damage) {
        // La logique est maintenant gérée dans le listener via canTriggerReborn() et triggerReborn()
    }
}
