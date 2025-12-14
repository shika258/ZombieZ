package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.momentum.MomentumManager;
import com.rinaorc.zombiez.party.Party;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des scoreboards avec affichage dynamique du momentum
 * Optimisé pour ne mettre à jour que les lignes qui changent
 */
public class ScoreboardManager {

    private final ZombieZPlugin plugin;
    
    // Cache des scoreboards par joueur
    private final Map<UUID, PlayerScoreboard> playerScoreboards;
    
    // Cache des dernières valeurs pour update incrémentiel
    private final Map<UUID, ScoreboardCache> scoreboardCache;
    
    // Animation du titre
    private int titleAnimationFrame = 0;
    private static final String[] TITLE_FRAMES = {
        "§c§lZ§6§lO§e§lM§a§lB§b§lI§d§lE§f§lZ",
        "§f§lZ§c§lO§6§lM§e§lB§a§lI§b§lE§d§lZ",
        "§d§lZ§f§lO§c§lM§6§lB§e§lI§a§lE§b§lZ",
        "§b§lZ§d§lO§f§lM§c§lB§6§lI§e§lE§a§lZ",
        "§a§lZ§b§lO§d§lM§f§lB§c§lI§6§lE§e§lZ",
        "§e§lZ§a§lO§b§lM§d§lB§f§lI§c§lE§6§lZ",
        "§6§lZ§e§lO§a§lM§b§lB§d§lI§f§lE§c§lZ"
    };

    public ScoreboardManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new ConcurrentHashMap<>();
        this.scoreboardCache = new ConcurrentHashMap<>();
    }

    /**
     * Crée un scoreboard pour un joueur
     */
    public void createScoreboard(Player player) {
        UUID playerId = player.getUniqueId();
        
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("zombiez", Criteria.DUMMY, 
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .deserialize(TITLE_FRAMES[0]));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Initialiser les lignes (du bas vers le haut, scores inversés)
        PlayerScoreboard ps = new PlayerScoreboard(scoreboard, objective);
        playerScoreboards.put(playerId, ps);
        scoreboardCache.put(playerId, new ScoreboardCache());
        
        // Initialiser toutes les lignes
        initializeLines(ps);
        
        player.setScoreboard(scoreboard);
        
        // Update initial
        updateScoreboard(player);
    }

    /**
     * Initialise les lignes du scoreboard
     */
    private void initializeLines(PlayerScoreboard ps) {
        Objective obj = ps.objective;
        
        // Ligne 15: Header décoratif
        obj.getScore("§8§m                    ").setScore(15);
        
        // Ligne 14: Zone (dynamique)
        ps.zoneLine = createTeam(ps.scoreboard, "zone", "§7Zone: ", "");
        obj.getScore("§7Zone: ").setScore(14);
        
        // Ligne 13: Niveau joueur
        ps.levelLine = createTeam(ps.scoreboard, "level", "§7Niveau: ", "");
        obj.getScore("§7Niveau: ").setScore(13);
        
        // Ligne 12: Séparateur
        obj.getScore("§8§m                   ").setScore(12);
        
        // Ligne 11: Points
        ps.pointsLine = createTeam(ps.scoreboard, "points", "§e⚡ Points: ", "");
        obj.getScore("§e⚡ Points: ").setScore(11);
        
        // Ligne 10: Gems
        ps.gemsLine = createTeam(ps.scoreboard, "gems", "§d💎 Gems: ", "");
        obj.getScore("§d💎 Gems: ").setScore(10);
        
        // Ligne 9: XP
        ps.xpLine = createTeam(ps.scoreboard, "xp", "§a✦ XP: ", "");
        obj.getScore("§a✦ XP: ").setScore(9);
        
        // Ligne 8: Séparateur Momentum
        obj.getScore("§6§l⚔ MOMENTUM").setScore(8);
        
        // Ligne 7: Kill Streak
        ps.streakLine = createTeam(ps.scoreboard, "streak", "§c🔥 Streak: ", "");
        obj.getScore("§c🔥 Streak: ").setScore(7);
        
        // Ligne 6: Combo
        ps.comboLine = createTeam(ps.scoreboard, "combo", "§b⚡ Combo: ", "");
        obj.getScore("§b⚡ Combo: ").setScore(6);
        
        // Ligne 5: Multiplicateur
        ps.multiplierLine = createTeam(ps.scoreboard, "multi", "§a✧ Multi: ", "");
        obj.getScore("§a✧ Multi: ").setScore(5);
        
        // Ligne 4: Séparateur
        obj.getScore("§8§m                  ").setScore(4);
        
        // Ligne 3: Kills session
        ps.killsLine = createTeam(ps.scoreboard, "kills", "§7☠ Kills: ", "");
        obj.getScore("§7☠ Kills: ").setScore(3);
        
        // Ligne 2: Party (si applicable)
        ps.partyLine = createTeam(ps.scoreboard, "party", "§d♦ Groupe: ", "");
        obj.getScore("§d♦ Groupe: ").setScore(2);
        
        // Ligne 1: Online
        ps.onlineLine = createTeam(ps.scoreboard, "online", "§7⚑ Online: ", "");
        obj.getScore("§7⚑ Online: ").setScore(1);
        
        // Ligne 0: Footer
        obj.getScore("§8§m                 ").setScore(0);
    }

    /**
     * Crée une team pour une ligne modifiable
     */
    private Team createTeam(Scoreboard scoreboard, String name, String prefix, String suffix) {
        Team team = scoreboard.registerNewTeam(name);
        team.setPrefix(prefix);
        team.setSuffix(suffix);
        return team;
    }

    /**
     * Met à jour le scoreboard d'un joueur
     */
    public void updateScoreboard(Player player) {
        UUID playerId = player.getUniqueId();
        
        PlayerScoreboard ps = playerScoreboards.get(playerId);
        ScoreboardCache cache = scoreboardCache.get(playerId);
        
        if (ps == null || cache == null) {
            createScoreboard(player);
            return;
        }
        
        // Obtenir les données
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return;
        
        Zone zone = plugin.getZoneManager().getPlayerZone(player);
        MomentumManager.MomentumData momentum = plugin.getMomentumManager() != null ? 
            plugin.getMomentumManager().getMomentum(player) : null;
        Party party = plugin.getPartyManager() != null ? 
            plugin.getPartyManager().getParty(player) : null;
        
        // === Update Zone ===
        String zoneText = zone != null ? zone.getColor() + zone.getDisplayName() : "§7???";
        if (!zoneText.equals(cache.zone)) {
            ps.zoneLine.setSuffix(zoneText);
            cache.zone = zoneText;
        }
        
        // === Update Niveau ===
        String levelText = "§a" + data.getLevel().get() + " §8[§7" + String.format("%.0f", data.getLevelProgress()) + "%§8]";
        if (!levelText.equals(cache.level)) {
            ps.levelLine.setSuffix(levelText);
            cache.level = levelText;
        }
        
        // === Update Points ===
        String pointsText = "§e" + formatNumber(data.getPoints().get());
        if (!pointsText.equals(cache.points)) {
            ps.pointsLine.setSuffix(pointsText);
            cache.points = pointsText;
        }
        
        // === Update Gems ===
        String gemsText = "§d" + formatNumber(data.getGems().get());
        if (!gemsText.equals(cache.gems)) {
            ps.gemsLine.setSuffix(gemsText);
            cache.gems = gemsText;
        }
        
        // === Update XP ===
        String xpText = "§a" + formatNumber(data.getXp().get()) + "§7/" + formatNumber(data.getRequiredXpForNextLevel());
        if (!xpText.equals(cache.xp)) {
            ps.xpLine.setSuffix(xpText);
            cache.xp = xpText;
        }
        
        // === Update Streak ===
        String streakText;
        if (momentum != null) {
            String streakColor = momentum.getKillStreak() >= 50 ? "§c§l" : 
                                momentum.getKillStreak() >= 25 ? "§6" :
                                momentum.getKillStreak() >= 10 ? "§e" : "§f";
            streakText = streakColor + momentum.getKillStreak();
            if (momentum.isFeverActive()) {
                streakText += " §c§l🔥";
            }
        } else {
            streakText = "§f" + data.getKillStreak().get();
        }
        if (!streakText.equals(cache.streak)) {
            ps.streakLine.setSuffix(streakText);
            cache.streak = streakText;
        }
        
        // === Update Combo ===
        String comboText;
        if (momentum != null && momentum.getCurrentCombo() > 0 && momentum.getComboTimer() > 0) {
            String comboColor = momentum.getCurrentCombo() >= 20 ? "§d" :
                               momentum.getCurrentCombo() >= 10 ? "§b" :
                               momentum.getCurrentCombo() >= 5 ? "§a" : "§f";
            comboText = comboColor + momentum.getCurrentCombo() + "x §8[" + 
                       String.format("%.1f", momentum.getComboTimer()) + "s]";
        } else {
            comboText = "§7-";
        }
        if (!comboText.equals(cache.combo)) {
            ps.comboLine.setSuffix(comboText);
            cache.combo = comboText;
        }
        
        // === Update Multiplicateur ===
        double multi = calculateTotalMultiplier(player, momentum, party, data);
        String multiColor = multi >= 3.0 ? "§c§l" : multi >= 2.0 ? "§6" : multi >= 1.5 ? "§e" : "§a";
        String multiText = multiColor + String.format("%.2f", multi) + "x";
        if (!multiText.equals(cache.multiplier)) {
            ps.multiplierLine.setSuffix(multiText);
            cache.multiplier = multiText;
        }
        
        // === Update Kills Session ===
        String killsText = momentum != null ? "§f" + momentum.getTotalKillsSession() : "§f" + data.getSessionKills().get();
        if (!killsText.equals(cache.kills)) {
            ps.killsLine.setSuffix(killsText);
            cache.kills = killsText;
        }
        
        // === Update Party ===
        String partyText;
        if (party != null) {
            double bonus = party.getProximityBonus(player);
            partyText = "§d" + party.getSize() + "/" + Party.MAX_SIZE + " §8(§a+" + (int)(bonus * 100) + "%§8)";
        } else {
            partyText = "§7Aucun";
        }
        if (!partyText.equals(cache.party)) {
            ps.partyLine.setSuffix(partyText);
            cache.party = partyText;
        }
        
        // === Update Online ===
        String onlineText = "§f" + Bukkit.getOnlinePlayers().size();
        if (!onlineText.equals(cache.online)) {
            ps.onlineLine.setSuffix(onlineText);
            cache.online = onlineText;
        }
    }

    /**
     * Met à jour tous les scoreboards
     */
    public void updateAllScoreboards() {
        // Animation du titre
        titleAnimationFrame = (titleAnimationFrame + 1) % TITLE_FRAMES.length;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerScoreboard ps = playerScoreboards.get(player.getUniqueId());
            if (ps != null) {
                // Update titre animé
                ps.objective.displayName(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(TITLE_FRAMES[titleAnimationFrame])
                );
                
                // Update contenu
                updateScoreboard(player);
            }
        }
    }

    /**
     * Calcule le multiplicateur total pour affichage
     */
    private double calculateTotalMultiplier(Player player, MomentumManager.MomentumData momentum, Party party, PlayerData data) {
        double multi = 1.0;
        
        if (momentum != null) {
            // Streak
            if (momentum.getKillStreak() >= 5) {
                multi *= 1.0 + (momentum.getKillStreak() * 0.01);
            }
            
            // Combo
            if (momentum.getCurrentCombo() >= 3) {
                multi *= 1.0 + (momentum.getCurrentCombo() * 0.02);
            }
            
            // Fever
            if (momentum.isFeverActive()) {
                multi *= 1.5;
            }
        }
        
        // Party proximity
        if (party != null) {
            multi *= (1.0 + party.getProximityBonus(player));
        }
        
        // VIP
        if (data != null && data.isVip()) {
            multi *= data.getXpMultiplier();
        }
        
        return Math.min(10.0, multi); // Cap à 10x
    }

    /**
     * Supprime le scoreboard d'un joueur
     */
    public void removeScoreboard(Player player) {
        UUID playerId = player.getUniqueId();
        playerScoreboards.remove(playerId);
        scoreboardCache.remove(playerId);
        
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    /**
     * Vérifie si un joueur a un scoreboard
     */
    public boolean hasScoreboard(Player player) {
        return playerScoreboards.containsKey(player.getUniqueId());
    }

    /**
     * Obtient le nombre de scoreboards actifs
     */
    public int getActiveCount() {
        return playerScoreboards.size();
    }

    /**
     * Formate un nombre avec des suffixes K, M, B
     */
    private String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1_000_000) return String.format("%.1fK", number / 1000.0);
        if (number < 1_000_000_000) return String.format("%.1fM", number / 1_000_000.0);
        return String.format("%.1fB", number / 1_000_000_000.0);
    }

    private String formatNumber(int number) {
        return formatNumber((long) number);
    }

    /**
     * Structure de scoreboard par joueur
     */
    private static class PlayerScoreboard {
        final Scoreboard scoreboard;
        final Objective objective;
        
        Team zoneLine;
        Team levelLine;
        Team pointsLine;
        Team gemsLine;
        Team xpLine;
        Team streakLine;
        Team comboLine;
        Team multiplierLine;
        Team killsLine;
        Team partyLine;
        Team onlineLine;
        
        PlayerScoreboard(Scoreboard scoreboard, Objective objective) {
            this.scoreboard = scoreboard;
            this.objective = objective;
        }
    }

    /**
     * Cache pour éviter les updates inutiles
     */
    private static class ScoreboardCache {
        String zone = "";
        String level = "";
        String points = "";
        String gems = "";
        String xp = "";
        String streak = "";
        String combo = "";
        String multiplier = "";
        String kills = "";
        String party = "";
        String online = "";
    }
}
