package com.rinaorc.zombiez.economy;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Système de prestige amélioré
 * - Conserve les zones débloquées
 * - Bonus de départ selon le prestige
 * - Reset gratuit des skills
 * - Récompenses progressives
 */
public class PrestigeSystem {

    private final ZombieZPlugin plugin;

    @Getter
    private final Map<Integer, PrestigeRewards> prestigeRewards = new HashMap<>();

    public static final int MAX_PRESTIGE = 10;
    public static final int REQUIRED_LEVEL = 100;

    public PrestigeSystem(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initRewards();
    }

    /**
     * Initialise les récompenses de prestige
     * Format: points, gems, multiplier, startingZone, bonusSkillPoints
     */
    private void initRewards() {
        // Prestige 1-3: Early game boost
        prestigeRewards.put(1, new PrestigeRewards(500, 10, 1.05, 1, 2));
        prestigeRewards.put(2, new PrestigeRewards(1000, 25, 1.10, 2, 4));
        prestigeRewards.put(3, new PrestigeRewards(2000, 50, 1.15, 3, 6));

        // Prestige 4-6: Mid game advantage
        prestigeRewards.put(4, new PrestigeRewards(3500, 75, 1.20, 4, 8));
        prestigeRewards.put(5, new PrestigeRewards(5000, 100, 1.30, 5, 10));
        prestigeRewards.put(6, new PrestigeRewards(7500, 150, 1.40, 5, 12));

        // Prestige 7-9: Late game power
        prestigeRewards.put(7, new PrestigeRewards(10000, 200, 1.50, 6, 15));
        prestigeRewards.put(8, new PrestigeRewards(15000, 300, 1.65, 7, 18));
        prestigeRewards.put(9, new PrestigeRewards(20000, 400, 1.80, 8, 22));

        // Prestige 10: Ultimate power
        prestigeRewards.put(10, new PrestigeRewards(30000, 500, 2.00, 9, 30));
    }

    /**
     * Vérifie si un joueur peut prestige
     */
    public boolean canPrestige(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        return data.getLevel().get() >= REQUIRED_LEVEL &&
               data.getPrestige().get() < MAX_PRESTIGE;
    }

    /**
     * Effectue le prestige avec conservation des zones
     */
    public boolean prestige(Player player) {
        if (!canPrestige(player)) return false;

        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;

        int oldPrestige = data.getPrestige().get();
        int newPrestige = oldPrestige + 1;
        PrestigeRewards rewards = prestigeRewards.get(newPrestige);

        // Sauvegarder la zone max atteinte (CONSERVATION)
        int previousHighestZone = data.getHighestZone();

        // ============ RESET PARTIEL ============
        // Reset niveau et XP
        data.setLevel(1);
        data.getXp().set(0);

        // Reset skills (gratuit au prestige)
        plugin.getSkillTreeManager().resetSkills(player, true);

        // ============ APPLIQUER LE PRESTIGE ============
        data.getPrestige().set(newPrestige);

        // ============ AVANTAGES DU PRESTIGE ============
        if (rewards != null) {
            // Zone de départ minimum basée sur le prestige
            // Le joueur garde accès à max(previousHighestZone, startingZone)
            int newMinZone = Math.max(previousHighestZone, rewards.startingZone());
            data.setHighestZone(newMinZone);

            // Récompenses monétaires
            plugin.getEconomyManager().addPoints(player, rewards.points());
            plugin.getEconomyManager().addGems(player, rewards.gems());

            // Points de skill bonus permanents
            // Ces points sont additionnels au système normal
            data.addBonusSkillPoints(rewards.bonusSkillPoints());
        } else {
            // Fallback: conserver les zones
            data.setHighestZone(previousHighestZone);
        }

        // ============ FEEDBACK ============
        // Titre épique
        player.sendTitle(
            "§6§l✦ PRESTIGE " + newPrestige + " ✦",
            "§e" + getPrestigeTitle(newPrestige),
            20, 100, 30
        );

        // Message détaillé
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════════");
        player.sendMessage("§6§l    ✦ PRESTIGE " + newPrestige + " ATTEINT! ✦");
        player.sendMessage("§6§l═══════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§7  Tu as été récompensé:");
        if (rewards != null) {
            player.sendMessage("§6    ➤ §e" + rewards.points() + " Points");
            player.sendMessage("§d    ➤ §e" + rewards.gems() + " Gems");
            player.sendMessage("§a    ➤ §e+" + rewards.bonusSkillPoints() + " Points de Skill bonus");
            player.sendMessage("§b    ➤ §eMultiplicateur x" + rewards.multiplier());
            player.sendMessage("§c    ➤ §eZone de départ: " + rewards.startingZone());
        }
        player.sendMessage("");
        player.sendMessage("§7  Conservé:");
        player.sendMessage("§a    ✓ §7Zones débloquées (Zone max: " + data.getHighestZone() + ")");
        player.sendMessage("§a    ✓ §7Achievements");
        player.sendMessage("§a    ✓ §7Items et inventaire");
        player.sendMessage("");
        player.sendMessage("§7  Reset:");
        player.sendMessage("§c    ✗ §7Niveau → 1");
        player.sendMessage("§c    ✗ §7Skills (reset gratuit appliqué)");
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════════");

        // Sons et effets
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        // Particules
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
            player.getLocation().add(0, 1, 0), 100, 0.5, 1, 0.5, 0.3);
        player.getWorld().spawnParticle(Particle.END_ROD,
            player.getLocation().add(0, 2, 0), 50, 1, 1, 1, 0.1);

        // Annonce globale
        plugin.getServer().broadcastMessage("");
        plugin.getServer().broadcastMessage("§6§l  ✦ " + player.getName() + " §ea atteint le §6§lPrestige " + newPrestige + "§e!");
        plugin.getServer().broadcastMessage("§7     Titre: §e" + getPrestigeTitle(newPrestige));
        plugin.getServer().broadcastMessage("");

        // Jouer un son pour tous
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p != player) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            }
        }

        return true;
    }

    /**
     * Obtient le titre associé au prestige
     */
    public String getPrestigeTitle(int prestige) {
        return switch (prestige) {
            case 1 -> "Survivant";
            case 2 -> "Vétéran";
            case 3 -> "Élite";
            case 4 -> "Champion";
            case 5 -> "Légende";
            case 6 -> "Mythique";
            case 7 -> "Immortel";
            case 8 -> "Transcendant";
            case 9 -> "Divin";
            case 10 -> "Patient Zéro Slayer";
            default -> "Inconnu";
        };
    }

    /**
     * Obtient la couleur du prestige
     */
    public String getPrestigeColor(int prestige) {
        return switch (prestige) {
            case 1, 2 -> "§a";
            case 3, 4 -> "§e";
            case 5, 6 -> "§6";
            case 7, 8 -> "§c";
            case 9 -> "§d";
            case 10 -> "§4§l";
            default -> "§7";
        };
    }

    /**
     * Obtient le multiplicateur de prestige
     */
    public double getMultiplier(int prestige) {
        PrestigeRewards rewards = prestigeRewards.get(prestige);
        return rewards != null ? rewards.multiplier() : 1.0;
    }

    /**
     * Obtient la zone de départ pour un prestige
     */
    public int getStartingZone(int prestige) {
        PrestigeRewards rewards = prestigeRewards.get(prestige);
        return rewards != null ? rewards.startingZone() : 1;
    }

    /**
     * Récompenses de prestige
     * @param points Points donnés
     * @param gems Gems données
     * @param multiplier Multiplicateur XP/Points
     * @param startingZone Zone de départ minimum
     * @param bonusSkillPoints Points de skill bonus permanents
     */
    public record PrestigeRewards(long points, int gems, double multiplier, int startingZone, int bonusSkillPoints) {}
}
