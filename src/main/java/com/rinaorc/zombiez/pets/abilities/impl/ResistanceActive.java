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
 * Ultimate du Scarabée Blindé
 * Applique l'effet Résistance II pendant une durée définie
 * Résistance II = -40% de dégâts reçus
 */
@Getter
public class ResistanceActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;

    public ResistanceActive(String id, String name, String desc, int duration) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.durationSeconds = duration;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 45;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Calculer la durée avec le multiplicateur de stats (bonus de durée)
        int actualDuration = (int) (durationSeconds * petData.getStatMultiplier());
        int durationTicks = actualDuration * 20;

        // Appliquer Résistance II (niveau 1 = Résistance II en jeu)
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE,
            durationTicks,
            1, // Niveau 1 = Résistance II (-40% dégâts)
            false, // Pas ambient
            true,  // Particules
            true   // Icône
        ));

        // Effets visuels - carapace
        player.getWorld().spawnParticle(Particle.CRIT,
            player.getLocation().add(0, 1, 0),
            20, 0.5, 0.5, 0.5, 0.1);

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0),
            10, 0.3, 0.3, 0.3, 0);

        // Son d'armure
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 0.8f);

        // Message clair
        player.sendMessage("§a[Pet] §7Carapace Blindée! §e-40% dégâts §7pendant §e" + actualDuration + "s");

        return true;
    }
}
