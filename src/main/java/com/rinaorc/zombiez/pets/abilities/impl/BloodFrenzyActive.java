package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultimate "Frénésie Sanguinaire" du Loup Spectral
 * Pendant la durée, chaque kill augmente vitesse d'attaque et dégâts
 */
@Getter
public class BloodFrenzyActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;         // Durée de la frénésie (8s)
    private final double bonusPerKill;         // Bonus par kill (5% = 0.05)
    private final double maxBonus;             // Bonus maximum (25% = 0.25)

    // Tracking des joueurs en frénésie - ConcurrentHashMap pour thread safety
    private final Map<UUID, Long> frenzyEndTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> frenzyKills = new ConcurrentHashMap<>();

    public BloodFrenzyActive(String id, String name, String desc, int duration, double perKill, double max) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
        this.bonusPerKill = perKill;
        this.maxBonus = max;
        PassiveAbilityCleanup.registerForCleanup(frenzyEndTime);
        PassiveAbilityCleanup.registerForCleanup(frenzyKills);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 30;
    }

    /**
     * Vérifie si le joueur est en frénésie
     */
    public boolean isInFrenzy(UUID uuid) {
        Long end = frenzyEndTime.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }

    /**
     * Obtient le nombre de kills pendant la frénésie
     */
    public int getFrenzyKills(UUID uuid) {
        return frenzyKills.getOrDefault(uuid, 0);
    }

    /**
     * Calcule le bonus actuel de frénésie
     */
    public double getFrenzyBonus(UUID uuid, double statMultiplier) {
        if (!isInFrenzy(uuid)) return 0;
        int kills = getFrenzyKills(uuid);
        double bonus = kills * bonusPerKill * statMultiplier;
        return Math.min(bonus, maxBonus * statMultiplier);
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        UUID uuid = player.getUniqueId();
        long duration = (long) (durationSeconds * 1000 * petData.getStatMultiplier());

        // Activer la frénésie
        frenzyEndTime.put(uuid, System.currentTimeMillis() + duration);
        frenzyKills.put(uuid, 0);

        // Effet de Haste pour la vitesse d'attaque de base
        int effectDuration = (int) (durationSeconds * 20 * petData.getStatMultiplier());
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, effectDuration, 0, false, true));

        // Son de hurlement
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.5f, 1.2f);

        // Particules de sang
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30,
            new Particle.DustOptions(Color.RED, 1.2f));
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 10, 0.5, 0.3, 0.5, 0);

        player.sendMessage("§a[Pet] §c§l🐺 FRÉNÉSIE SANGUINAIRE! §7Chaque kill = +5% dégâts/vitesse!");

        // Animation continue pendant la frénésie
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = effectDuration;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline() || !isInFrenzy(uuid)) {
                    // Fin de la frénésie
                    if (player.isOnline()) {
                        int kills = getFrenzyKills(uuid);
                        double totalBonus = getFrenzyBonus(uuid, petData.getStatMultiplier()) * 100;
                        player.sendMessage("§a[Pet] §7Frénésie terminée! §e" + kills + " §7kills, bonus max: §c+" +
                            String.format("%.1f", totalBonus) + "%");
                        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.5f, 1.0f);
                    }
                    frenzyEndTime.remove(uuid);
                    frenzyKills.remove(uuid);
                    cancel();
                    return;
                }

                // Particules de frénésie
                if (ticks % 10 == 0) {
                    int kills = getFrenzyKills(uuid);
                    float intensity = Math.min(1.0f, 0.3f + (kills * 0.15f));
                    player.spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0),
                        3 + kills, new Particle.DustOptions(Color.fromRGB(200, 50, 50), intensity));

                    // Effet de rage croissante
                    if (kills >= 3) {
                        player.spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 2, 0), 1);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);

        return true;
    }

    @Override
    public void onKill(Player player, PetData petData, LivingEntity killed) {
        UUID uuid = player.getUniqueId();
        if (!isInFrenzy(uuid)) return;

        int currentKills = frenzyKills.getOrDefault(uuid, 0);
        int maxKills = (int) (maxBonus / bonusPerKill);

        if (currentKills < maxKills) {
            frenzyKills.put(uuid, currentKills + 1);
            int newKills = currentKills + 1;
            double currentBonus = newKills * bonusPerKill * 100;

            // Notification et effet
            player.sendMessage("§a[Pet] §c🔥 Frénésie x" + newKills + " §7(§e+" + String.format("%.0f", currentBonus) + "% §7dégâts/vitesse)");
            player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.5f, 1.0f + (newKills * 0.1f));

            // Mettre à jour le Haste selon les kills
            int remainingDuration = (int) ((frenzyEndTime.get(uuid) - System.currentTimeMillis()) / 50);
            if (remainingDuration > 0) {
                int hasteLevel = Math.min(2, newKills / 2); // Haste I à III
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, remainingDuration, hasteLevel, false, true));
            }

            // Particules de kill
            player.getWorld().spawnParticle(Particle.DUST, killed.getLocation().add(0, 1, 0), 15,
                new Particle.DustOptions(Color.RED, 1.0f));
        }
    }

    @Override
    public double onDamageDealt(Player player, PetData petData, double damage, LivingEntity target) {
        UUID uuid = player.getUniqueId();
        if (!isInFrenzy(uuid)) return damage;

        double bonus = getFrenzyBonus(uuid, petData.getStatMultiplier());
        return damage * (1 + bonus);
    }
}
