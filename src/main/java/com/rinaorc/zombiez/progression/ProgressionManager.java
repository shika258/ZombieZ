package com.rinaorc.zombiez.progression;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Système de progression des joueurs
 * Niveaux 1-100, Prestiges 0-10, Skills passifs
 */
public class ProgressionManager {

    private final ZombieZPlugin plugin;
    
    // Configuration des niveaux
    public static final int MAX_LEVEL = 100;
    public static final int MAX_PRESTIGE = 10;
    
    // XP requis par niveau (formule exponentielle)
    @Getter
    private final long[] xpRequirements;
    
    // Bonus par prestige
    private final Map<Integer, PrestigeBonus> prestigeBonuses;

    public ProgressionManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.xpRequirements = calculateXpRequirements();
        this.prestigeBonuses = initPrestigeBonuses();
    }

    /**
     * Calcule les XP requis pour chaque niveau
     * Formule: 100 * level^1.8
     */
    private long[] calculateXpRequirements() {
        long[] requirements = new long[MAX_LEVEL + 1];
        requirements[0] = 0;
        
        for (int level = 1; level <= MAX_LEVEL; level++) {
            requirements[level] = (long) (100 * Math.pow(level, 1.8));
        }
        
        return requirements;
    }

    /**
     * Initialise les bonus de prestige
     */
    private Map<Integer, PrestigeBonus> initPrestigeBonuses() {
        Map<Integer, PrestigeBonus> bonuses = new HashMap<>();
        
        bonuses.put(1, new PrestigeBonus("§6✦ Prestige I", 
            1.05, 1.05, 1.10, "§eAccès au /prestige shop"));
        bonuses.put(2, new PrestigeBonus("§6✦✦ Prestige II", 
            1.10, 1.10, 1.20, "§eSlot d'item supplémentaire"));
        bonuses.put(3, new PrestigeBonus("§c✦✦✦ Prestige III", 
            1.15, 1.15, 1.35, "§eAura de prestige visible"));
        bonuses.put(4, new PrestigeBonus("§c✦✦✦✦ Prestige IV", 
            1.20, 1.20, 1.50, "§eRéduction prix shop -10%"));
        bonuses.put(5, new PrestigeBonus("§5✦✦✦✦✦ Prestige V", 
            1.30, 1.25, 1.75, "§dTitre §5[Vétéran]"));
        bonuses.put(6, new PrestigeBonus("§5✦✦✦✦✦✦ Prestige VI", 
            1.40, 1.30, 2.00, "§dDouble loot chance +5%"));
        bonuses.put(7, new PrestigeBonus("§d✦✦✦✦✦✦✦ Prestige VII", 
            1.50, 1.35, 2.50, "§dGemmes bonus +10%"));
        bonuses.put(8, new PrestigeBonus("§d✦✦✦✦✦✦✦✦ Prestige VIII", 
            1.65, 1.40, 3.00, "§dRespawn instantané"));
        bonuses.put(9, new PrestigeBonus("§b✦✦✦✦✦✦✦✦✦ Prestige IX", 
            1.80, 1.50, 4.00, "§bTitre §b[Légende]"));
        bonuses.put(10, new PrestigeBonus("§4✦✦✦✦✦✦✦✦✦✦ Prestige X §4[MAX]", 
            2.00, 1.75, 5.00, "§4Titre §4[Immortel] §c+ Aura de feu"));
        
        return bonuses;
    }

    /**
     * Ajoute de l'XP à un joueur
     */
    public void addXP(Player player, long xp) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        int currentLevel = data.getLevel().get();
        int prestige = data.getPrestige().get();
        
        // Bonus de prestige sur l'XP
        double xpMultiplier = getPrestigeBonus(prestige).xpMultiplier();
        long bonusXp = (long) (xp * xpMultiplier);
        
        data.addXp(bonusXp);
        
        // Vérifier le level up
        checkLevelUp(player, data);
    }

    /**
     * Vérifie et applique les level ups
     */
    private void checkLevelUp(Player player, PlayerData data) {
        int currentLevel = data.getLevel().get();
        
        while (currentLevel < MAX_LEVEL && data.getXp().get() >= getXpForLevel(currentLevel + 1)) {
            currentLevel++;
            data.setLevel(currentLevel);
            
            // Notification
            onLevelUp(player, currentLevel);
        }
    }

    /**
     * Appelé quand un joueur monte de niveau
     */
    private void onLevelUp(Player player, int newLevel) {
        // Titre et message
        player.sendTitle(
            "§a§lNIVEAU " + newLevel + "!",
            "§7Félicitations!",
            10, 40, 10
        );
        
        // Son
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        
        // Récompenses par palier
        if (newLevel % 10 == 0) {
            // Milestone tous les 10 niveaux
            int gems = newLevel / 2;
            plugin.getEconomyManager().addGems(player, gems);
            player.sendMessage("§d+" + gems + " Gemmes §7pour avoir atteint le niveau " + newLevel + "!");
            
            // Effet visuel
            player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0),
                50, 1, 1, 1, 0.3
            );
        }
        
        // Bonus temporaire
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 0));
        
        // Broadcast aux joueurs proches
        for (Player nearby : player.getWorld().getNearbyEntities(player.getLocation(), 50, 50, 50)
                .stream().filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
            if (nearby != player) {
                nearby.sendMessage("§7" + player.getName() + " est passé niveau §a" + newLevel + "§7!");
            }
        }

        // Notifier le système de Parcours (Journey)
        if (plugin.getJourneyListener() != null) {
            plugin.getJourneyListener().onLevelUp(player, newLevel);
        }
    }

    /**
     * Effectue un prestige
     */
    public boolean prestige(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return false;
        
        int currentPrestige = data.getPrestige().get();
        int currentLevel = data.getLevel().get();
        
        // Vérifications
        if (currentPrestige >= MAX_PRESTIGE) {
            player.sendMessage("§cTu as déjà atteint le prestige maximum!");
            return false;
        }
        
        if (currentLevel < MAX_LEVEL) {
            player.sendMessage("§cTu dois être niveau " + MAX_LEVEL + " pour prestige!");
            return false;
        }
        
        // Effectuer le prestige
        int newPrestige = currentPrestige + 1;
        data.setPrestige(newPrestige);
        data.setLevel(1);
        data.setXp(0);
        
        // Annoncer
        onPrestige(player, newPrestige);
        
        return true;
    }

    /**
     * Appelé quand un joueur prestige
     */
    private void onPrestige(Player player, int newPrestige) {
        PrestigeBonus bonus = getPrestigeBonus(newPrestige);
        
        // Annonce globale
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle("", "", 0, 1, 0); // Clear
            p.sendMessage("");
            p.sendMessage("§6§l★ PRESTIGE! ★");
            p.sendMessage("§e" + player.getName() + " §7a atteint " + bonus.displayName());
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        
        // Titre pour le joueur
        player.sendTitle(
            bonus.displayName(),
            "§7" + bonus.specialReward(),
            20, 100, 20
        );
        
        // Récompenses
        int gemReward = newPrestige * 50;
        plugin.getEconomyManager().addGems(player, gemReward);
        player.sendMessage("§d+" + gemReward + " Gemmes §7de récompense de prestige!");
        
        // Effets
        player.getWorld().spawnParticle(
            org.bukkit.Particle.END_ROD,
            player.getLocation().add(0, 1, 0),
            200, 2, 2, 2, 0.5
        );

        // Notifier le système de Parcours (Journey)
        if (plugin.getJourneyListener() != null) {
            plugin.getJourneyListener().onPrestige(player, newPrestige);
        }
    }

    /**
     * Obtient l'XP requis pour un niveau
     */
    public long getXpForLevel(int level) {
        if (level < 0) return 0;
        if (level > MAX_LEVEL) return Long.MAX_VALUE;
        return xpRequirements[level];
    }

    /**
     * Obtient l'XP total cumulé jusqu'à un niveau
     */
    public long getTotalXpForLevel(int level) {
        long total = 0;
        for (int i = 1; i <= level && i <= MAX_LEVEL; i++) {
            total += xpRequirements[i];
        }
        return total;
    }

    /**
     * Obtient le bonus de prestige
     */
    public PrestigeBonus getPrestigeBonus(int prestige) {
        if (prestige <= 0) {
            return new PrestigeBonus("§7Aucun prestige", 1.0, 1.0, 1.0, "");
        }
        return prestigeBonuses.getOrDefault(prestige, 
            new PrestigeBonus("§7Prestige " + prestige, 1.0, 1.0, 1.0, ""));
    }

    /**
     * Calcule le multiplicateur de dégâts total
     */
    public double getDamageMultiplier(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 1.0;
        
        double base = 1.0;
        
        // Bonus de niveau (+0.5% par niveau)
        base += data.getLevel().get() * 0.005;
        
        // Bonus de prestige
        base *= getPrestigeBonus(data.getPrestige().get()).damageMultiplier();
        
        return base;
    }

    /**
     * Calcule le multiplicateur de défense total
     */
    public double getDefenseMultiplier(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 1.0;
        
        double base = 1.0;
        
        // Bonus de niveau (+0.3% par niveau)
        base += data.getLevel().get() * 0.003;
        
        // Bonus de prestige
        base *= getPrestigeBonus(data.getPrestige().get()).defenseMultiplier();
        
        return base;
    }

    /**
     * Obtient le titre affiché du joueur
     */
    public String getPlayerTitle(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return "";
        
        int prestige = data.getPrestige().get();
        int level = data.getLevel().get();
        
        StringBuilder title = new StringBuilder();
        
        // Prestige
        if (prestige >= 10) {
            title.append("§4[Immortel] ");
        } else if (prestige >= 9) {
            title.append("§b[Légende] ");
        } else if (prestige >= 5) {
            title.append("§5[Vétéran] ");
        } else if (prestige > 0) {
            title.append("§6[P").append(prestige).append("] ");
        }
        
        // Niveau
        title.append("§7[").append(level).append("] ");
        
        return title.toString();
    }

    /**
     * Représente les bonus d'un prestige
     */
    public record PrestigeBonus(
        String displayName,
        double xpMultiplier,
        double damageMultiplier,
        double defenseMultiplier,
        String specialReward
    ) {}
}
