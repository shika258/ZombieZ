package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.api.events.PlayerZoneChangeEvent;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.utils.MessageUtils;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener pour les changements de zone
 * Gère les messages d'entrée, effets environnementaux, etc.
 */
public class ZoneChangeListener implements Listener {

    private final ZombieZPlugin plugin;

    public ZoneChangeListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onZoneChange(PlayerZoneChangeEvent event) {
        Player player = event.getPlayer();
        Zone fromZone = event.getFromZone();
        Zone toZone = event.getToZone();

        // Message d'entrée dans la zone
        sendZoneEntryMessage(player, toZone, event.isFirstTime());

        // Gérer les effets spéciaux selon la zone
        handleZoneEffects(player, fromZone, toZone);

        // Récompense si première visite
        if (event.isFirstTime() && !toZone.isSafeZone()) {
            rewardFirstVisit(player, toZone);
        }

        // Avertissement PvP
        if (event.isEnteringPvP()) {
            sendPvPWarning(player);
        } else if (event.isLeavingPvP()) {
            sendPvPExitMessage(player);
        }

        // Avertissement zone dangereuse
        if (toZone.isDangerous()) {
            sendDangerWarning(player, toZone);
        }

        // Zone de boss
        if (event.isEnteringBossZone()) {
            sendBossZoneMessage(player, toZone);
        }
    }

    /**
     * Envoie le message d'entrée dans la zone
     */
    private void sendZoneEntryMessage(Player player, Zone zone, boolean firstTime) {
        // Titre principal
        String title = zone.getColor() + zone.getDisplayName().toUpperCase();
        String subtitle = zone.getStarsDisplay();
        
        if (firstTime) {
            subtitle = "§6✦ §eNouvelle zone découverte! §6✦";
        }

        MessageUtils.sendTitle(player, title, subtitle, 10, 40, 20);

        // Son d'entrée
        if (firstTime) {
            MessageUtils.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        } else {
            MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        }

        // Message dans le chat avec description
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§8§m                              ");
            MessageUtils.sendRaw(player, "  " + zone.getColor() + "§l" + zone.getDisplayName());
            MessageUtils.sendRaw(player, "  §7" + zone.getDescription());
            MessageUtils.sendRaw(player, "  §7Difficulté: " + zone.getStarsDisplay());
            MessageUtils.sendRaw(player, "§8§m                              ");
            MessageUtils.sendRaw(player, "");
        }, 5L);
    }

    /**
     * Gère les effets de potion selon la zone
     */
    private void handleZoneEffects(Player player, Zone fromZone, Zone toZone) {
        // Retirer les effets de l'ancienne zone
        if (fromZone != null) {
            removeZoneEffects(player, fromZone);
        }

        // Appliquer les effets de la nouvelle zone
        applyZoneEffects(player, toZone);
    }

    /**
     * Retire les effets d'une zone
     */
    private void removeZoneEffects(Player player, Zone zone) {
        String effect = zone.getEnvironmentalEffect();
        
        switch (effect.toUpperCase()) {
            case "COLD" -> player.removePotionEffect(PotionEffectType.SLOWNESS);
            case "TOXIC" -> player.removePotionEffect(PotionEffectType.POISON);
            case "RADIATION" -> {
                player.removePotionEffect(PotionEffectType.WITHER);
                player.removePotionEffect(PotionEffectType.NAUSEA);
            }
            case "DARKNESS" -> player.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }

    /**
     * Applique les effets d'une zone
     */
    private void applyZoneEffects(Player player, Zone zone) {
        String effect = zone.getEnvironmentalEffect();
        if (effect.equals("NONE")) return;

        // Les effets sont gérés par un task périodique, pas ici
        // On affiche juste un avertissement

        // Mais on peut appliquer des effets visuels permanents
        switch (effect.toUpperCase()) {
            case "COLD" -> {
                // Léger effet de lenteur dans le froid
                // player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 0, true, false, false));
            }
            case "DARKNESS" -> {
                // Effet de darkness dans les zones sombres
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 999999, 0, true, false, true));
            }
        }
    }

    /**
     * Récompense pour première visite d'une zone
     */
    private void rewardFirstVisit(Player player, Zone zone) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;

        // Points bonus pour découverte
        int zoneId = zone.getId();
        long bonus = zoneId * 100L;
        
        plugin.getEconomyManager().addPoints(player, bonus, "Découverte " + zone.getDisplayName());
        
        // XP bonus
        long xpBonus = zoneId * 50L;
        plugin.getEconomyManager().addXp(player, xpBonus, "Exploration");

        // Message spécial
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.send(player, "§6§l★ ZONE DÉCOUVERTE! §e+" + bonus + " Points §7| §b+" + xpBonus + " XP");
        }, 60L);

        // Statistique
        data.incrementStat("zones_discovered");
    }

    /**
     * Avertissement d'entrée en zone PvP
     */
    private void sendPvPWarning(Player player) {
        MessageUtils.sendTitle(player, "§c§l⚔ ZONE PVP ⚔", "§7Le PvP est activé ici!", 10, 60, 20);
        MessageUtils.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§c§l⚠ ATTENTION ⚠");
            MessageUtils.sendRaw(player, "§7Vous entrez dans une §czone PvP§7!");
            MessageUtils.sendRaw(player, "§7Les autres joueurs peuvent vous attaquer.");
            MessageUtils.sendRaw(player, "§7Meilleur loot mais plus de danger!");
            MessageUtils.sendRaw(player, "");
        }, 20L);
    }

    /**
     * Message de sortie de zone PvP
     */
    private void sendPvPExitMessage(Player player) {
        MessageUtils.sendActionBar(player, "§a✓ Vous avez quitté la zone PvP");
        MessageUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
    }

    /**
     * Avertissement zone dangereuse (effets environnementaux)
     */
    private void sendDangerWarning(Player player, Zone zone) {
        String effect = zone.getEnvironmentalEffect();
        String warning = switch (effect.toUpperCase()) {
            case "HEAT" -> "§6§l☀ CHALEUR EXTRÊME §7- Hydratez-vous!";
            case "COLD" -> "§b§l❄ FROID GLACIAL §7- Restez en mouvement!";
            case "TOXIC" -> "§2§l☠ ZONE TOXIQUE §7- Masque à gaz recommandé!";
            case "RADIATION" -> "§a§l☢ RADIATION §7- Exposition dangereuse!";
            case "FIRE" -> "§c§l🔥 ZONE INFERNALE §7- Protection feu requise!";
            default -> null;
        };

        if (warning != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                MessageUtils.sendRaw(player, "");
                MessageUtils.sendRaw(player, warning);
                MessageUtils.sendRaw(player, "§7Dégâts: §c" + zone.getEnvironmentalDamage() + "/s");
                MessageUtils.sendRaw(player, "");
            }, 80L);
        }
    }

    /**
     * Message d'entrée en zone de boss
     */
    private void sendBossZoneMessage(Player player, Zone zone) {
        MessageUtils.sendTitle(player, "§5§l☠ ZONE DE BOSS ☠", "§7Préparez-vous au combat...", 10, 60, 20);
        MessageUtils.playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.8f);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§5§l═══════════════════════════");
            MessageUtils.sendRaw(player, "§d§l  ZONE DE BOSS DÉTECTÉE");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§7  Un boss puissant rôde dans cette zone.");
            MessageUtils.sendRaw(player, "§7  Équipez-vous correctement avant");
            MessageUtils.sendRaw(player, "§7  de l'affronter!");
            MessageUtils.sendRaw(player, "");
            MessageUtils.sendRaw(player, "§5§l═══════════════════════════");
            MessageUtils.sendRaw(player, "");
        }, 40L);
    }
}
