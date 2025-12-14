package com.rinaorc.zombiez.commands.player;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.progression.*;
import com.rinaorc.zombiez.progression.gui.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commandes de progression: achievements, skills, missions, battlepass, cosmétiques
 */
public class ProgressionCommand implements CommandExecutor, TabCompleter {

    private final ZombieZPlugin plugin;

    public ProgressionCommand(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande nécessite d'être un joueur.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            // === ACHIEVEMENTS ===
            case "achievements", "ach", "succes" -> {
                ProgressionGUI gui = new ProgressionGUI(plugin, player);
                gui.open(player);
            }
            
            // === SKILLS ===
            case "skills", "competences", "talent" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
                    plugin.getSkillTreeManager().resetSkills(player, false);
                } else {
                    // Ouvrir le GUI des skills
                    ProgressionGUI gui = new ProgressionGUI(plugin, player);
                    gui.openSkillTree(player);
                }
            }
            
            // === MISSIONS ===
            case "missions", "quests", "quetes" -> {
                MissionGUI gui = new MissionGUI(plugin, player);
                gui.open();
            }
            
            // === BATTLE PASS ===
            case "battlepass", "bp", "pass", "saison" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("buy")) {
                    plugin.getBattlePassManager().purchasePremium(player);
                } else {
                    BattlePassGUI gui = new BattlePassGUI(plugin, player);
                    gui.open();
                }
            }
            
            // === COSMÉTIQUES ===
            case "cosmetics", "cosmetiques", "title", "titre" -> {
                if (args.length < 2) {
                    showCosmeticsHelp(player);
                    return true;
                }
                
                String cosmeticSub = args[1].toLowerCase();
                switch (cosmeticSub) {
                    case "list", "liste" -> listCosmetics(player, args.length > 2 ? args[2] : "all");
                    case "equip", "set" -> {
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /progression cosmetics equip <id>");
                            return true;
                        }
                        equipCosmetic(player, args[2]);
                    }
                    case "unequip", "remove" -> {
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /progression cosmetics unequip <title|particle|aura>");
                            return true;
                        }
                        unequipCosmetic(player, args[2]);
                    }
                    default -> showCosmeticsHelp(player);
                }
            }
            
            // === LEADERBOARD ===
            case "leaderboard", "lb", "top" -> {
                String type = args.length > 1 ? args[1].toLowerCase() : "kills";
                showLeaderboard(player, type);
            }
            
            // === STATS ===
            case "stats", "statistiques" -> showDetailedStats(player);
            
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Progression ===");
        player.sendMessage("§e/progression achievements §7- Voir vos achievements");
        player.sendMessage("§e/progression skills §7- Arbre de compétences");
        player.sendMessage("§e/progression missions §7- Missions journalières/hebdo");
        player.sendMessage("§e/progression battlepass §7- Battle Pass de la saison");
        player.sendMessage("§e/progression cosmetics §7- Gérer vos cosmétiques");
        player.sendMessage("§e/progression leaderboard [type] §7- Classements");
        player.sendMessage("§e/progression stats §7- Vos statistiques détaillées");
    }

    private void showCosmeticsHelp(Player player) {
        player.sendMessage("§6=== Cosmétiques ===");
        player.sendMessage("§e/progression cosmetics list [titles|particles|auras] §7- Liste");
        player.sendMessage("§e/progression cosmetics equip <id> §7- Équiper");
        player.sendMessage("§e/progression cosmetics unequip <title|particle|aura> §7- Retirer");
    }

    private void listCosmetics(Player player, String type) {
        CosmeticManager cm = plugin.getCosmeticManager();
        
        player.sendMessage("§6=== Vos Cosmétiques ===");
        
        if (type.equals("all") || type.equals("titles")) {
            player.sendMessage("");
            player.sendMessage("§e§lTitres débloqués:");
            List<CosmeticManager.Title> titles = cm.getUnlockedTitles(player);
            if (titles.isEmpty()) {
                player.sendMessage("§7  Aucun titre débloqué");
            } else {
                for (CosmeticManager.Title title : titles) {
                    player.sendMessage("§7  - " + title.getDisplayFormat() + " §7(" + title.getId() + ")");
                }
            }
        }
        
        if (type.equals("all") || type.equals("particles")) {
            player.sendMessage("");
            player.sendMessage("§d§lParticules débloquées:");
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            boolean hasAny = false;
            if (data != null) {
                for (String cosId : data.getCosmetics()) {
                    CosmeticManager.ParticleEffect effect = cm.getParticleEffects().get(cosId);
                    if (effect != null) {
                        player.sendMessage("§7  - §e" + effect.getName() + " §7(" + cosId + ")");
                        hasAny = true;
                    }
                }
            }
            if (!hasAny) {
                player.sendMessage("§7  Aucune particule débloquée");
            }
        }
        
        if (type.equals("all") || type.equals("auras")) {
            player.sendMessage("");
            player.sendMessage("§5§lAuras débloquées:");
            PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            boolean hasAny = false;
            if (data != null) {
                for (String cosId : data.getCosmetics()) {
                    CosmeticManager.Aura aura = cm.getAuras().get(cosId);
                    if (aura != null) {
                        player.sendMessage("§7  - §e" + aura.getName() + " §7(" + cosId + ")");
                        hasAny = true;
                    }
                }
            }
            if (!hasAny) {
                player.sendMessage("§7  Aucune aura débloquée");
            }
        }
    }

    private void equipCosmetic(Player player, String id) {
        CosmeticManager cm = plugin.getCosmeticManager();
        
        // Essayer titre
        if (cm.getTitles().containsKey(id)) {
            cm.equipTitle(player, id);
            return;
        }
        
        // Essayer particule
        if (cm.getParticleEffects().containsKey(id)) {
            cm.equipParticle(player, id);
            return;
        }
        
        // Essayer aura
        if (cm.getAuras().containsKey(id)) {
            cm.equipAura(player, id);
            return;
        }
        
        player.sendMessage("§cCosmétique introuvable: " + id);
    }

    private void unequipCosmetic(Player player, String type) {
        CosmeticManager cm = plugin.getCosmeticManager();
        
        switch (type.toLowerCase()) {
            case "title", "titre" -> cm.unequip(player, CosmeticManager.CosmeticType.TITLE);
            case "particle", "particule" -> cm.unequip(player, CosmeticManager.CosmeticType.PARTICLE);
            case "aura" -> cm.unequip(player, CosmeticManager.CosmeticType.AURA);
            default -> player.sendMessage("§cType invalide. Utilisez: title, particle, ou aura");
        }
    }

    private void showLeaderboard(Player player, String type) {
        LeaderboardManager lb = plugin.getLeaderboardManager();
        
        String title = switch (type) {
            case "kills", "zombies" -> "§c☠ Top Kills Zombies";
            case "level", "niveau" -> "§e★ Top Niveaux";
            case "points" -> "§6$ Top Points";
            case "zones" -> "§a⚑ Top Exploration";
            case "bosses" -> "§d⚔ Top Boss Kills";
            default -> "§c☠ Top Kills Zombies";
        };
        
        player.sendMessage("");
        player.sendMessage(title);
        player.sendMessage("§7═══════════════════════");
        
        List<LeaderboardManager.LeaderboardEntry> entries = lb.getTopEntries(type, 10);
        
        if (entries.isEmpty()) {
            player.sendMessage("§7  Aucune donnée disponible");
        } else {
            int rank = 1;
            for (LeaderboardManager.LeaderboardEntry entry : entries) {
                String rankColor = switch (rank) {
                    case 1 -> "§6§l";
                    case 2 -> "§7§l";
                    case 3 -> "§c§l";
                    default -> "§f";
                };
                String medal = switch (rank) {
                    case 1 -> "§6🥇";
                    case 2 -> "§7🥈";
                    case 3 -> "§c🥉";
                    default -> "§f#" + rank;
                };
                
                player.sendMessage(medal + " " + rankColor + entry.getPlayerName() + " §7- §e" + formatNumber(entry.getValue()));
                rank++;
            }
        }
        
        // Position du joueur
        int playerRank = lb.getPlayerRank(player.getUniqueId(), type);
        if (playerRank > 0) {
            player.sendMessage("§7═══════════════════════");
            player.sendMessage("§eVotre position: §f#" + playerRank);
        }
        player.sendMessage("");
    }

    private void showDetailedStats(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cDonnées non chargées.");
            return;
        }
        
        player.sendMessage("");
        player.sendMessage("§6§l=== Vos Statistiques ===");
        player.sendMessage("");
        
        player.sendMessage("§e§lProgression:");
        player.sendMessage("§7  Niveau: §e" + data.getLevel().get());
        player.sendMessage("§7  Prestige: §d" + data.getPrestige().get());
        player.sendMessage("§7  XP Total: §b" + formatNumber(data.getTotalXp().get()));
        
        player.sendMessage("");
        player.sendMessage("§c§lCombat:");
        player.sendMessage("§7  Zombies tués: §c" + formatNumber(data.getZombieKills().get()));
        player.sendMessage("§7  Élites tués: §e" + formatNumber(data.getEliteKills().get()));
        player.sendMessage("§7  Boss tués: §d" + formatNumber(data.getBossKills().get()));
        player.sendMessage("§7  Morts: §8" + data.getDeaths().get());
        player.sendMessage("§7  Ratio K/D: §a" + String.format("%.2f", 
            data.getDeaths().get() > 0 ? (double) data.getZombieKills().get() / data.getDeaths().get() : data.getZombieKills().get()));
        
        player.sendMessage("");
        player.sendMessage("§a§lExploration:");
        player.sendMessage("§7  Zone max: §a" + data.getMaxZoneReached());
        player.sendMessage("§7  Distance parcourue: §b" + formatNumber(data.getDistanceTraveled().get()) + " blocs");
        player.sendMessage("§7  Temps de jeu: §e" + formatPlaytime(data.getPlaytime().get()));
        
        player.sendMessage("");
        player.sendMessage("§e§lÉconomie:");
        player.sendMessage("§7  Points actuels: §6" + formatNumber(data.getPoints().get()));
        player.sendMessage("§7  Gemmes: §d" + data.getGems().get());
        player.sendMessage("§7  Points gagnés (total): §6" + formatNumber(data.getTotalPointsEarned().get()));
        
        player.sendMessage("");
        player.sendMessage("§d§lAchievements:");
        int unlocked = plugin.getAchievementManager().getUnlockedCount(player);
        int total = plugin.getAchievementManager().getAchievements().size();
        player.sendMessage("§7  Débloqués: §e" + unlocked + "§7/" + total + 
            " §8(" + String.format("%.1f", (double) unlocked / total * 100) + "%)");
        
        player.sendMessage("");
    }

    private String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of(
                "achievements", "skills", "missions", "battlepass", 
                "cosmetics", "leaderboard", "stats"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "skills" -> completions.add("reset");
                case "battlepass", "bp" -> completions.add("buy");
                case "cosmetics" -> completions.addAll(List.of("list", "equip", "unequip"));
                case "leaderboard", "lb" -> completions.addAll(List.of("kills", "level", "points", "zones", "bosses"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("cosmetics")) {
                switch (args[1].toLowerCase()) {
                    case "list" -> completions.addAll(List.of("titles", "particles", "auras"));
                    case "equip" -> {
                        CosmeticManager cm = plugin.getCosmeticManager();
                        completions.addAll(cm.getTitles().keySet());
                        completions.addAll(cm.getParticleEffects().keySet());
                        completions.addAll(cm.getAuras().keySet());
                    }
                    case "unequip" -> completions.addAll(List.of("title", "particle", "aura"));
                }
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
