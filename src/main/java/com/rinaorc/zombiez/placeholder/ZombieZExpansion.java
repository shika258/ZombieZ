package com.rinaorc.zombiez.placeholder;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ItemManager;
import com.rinaorc.zombiez.zones.Zone;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * PlaceholderAPI Expansion for ZombieZ
 * Provides all player data as placeholders for TAB, scoreboards, etc.
 *
 * Usage: %zombiez_<placeholder>%
 *
 * @author Rinaorc Studio
 */
public class ZombieZExpansion extends PlaceholderExpansion {

    private final ZombieZPlugin plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.##");
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE);

    public ZombieZExpansion(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "zombiez";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Rinaorc Studio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return handleOfflinePlaceholder(params);
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return "N/A";
        }

        // Parse le placeholder
        String[] parts = params.toLowerCase().split("_");
        String mainParam = parts[0];

        return switch (mainParam) {
            // ==================== NIVEAU & PROGRESSION ====================
            case "level", "lvl" -> String.valueOf(data.getLevel().get());
            case "xp" -> formatNumber(data.getXp().get());
            case "xp_raw" -> String.valueOf(data.getXp().get());
            case "xp_required", "xpnext" -> formatNumber(data.getRequiredXpForNextLevel());
            case "xp_progress" -> decimalFormat.format(data.getLevelProgress()) + "%";
            case "xp_bar" -> createProgressBar(data.getLevelProgress(), 10);
            case "prestige" -> String.valueOf(data.getPrestige().get());
            case "prestige_star" -> getPrestigeStars(data.getPrestige().get());
            case "total_xp" -> formatNumber(data.getTotalXp().get());

            // ==================== ECONOMIE ====================
            case "points", "money", "coins" -> formatNumber(data.getPoints().get());
            case "points_raw" -> String.valueOf(data.getPoints().get());
            case "gems" -> formatNumber(data.getGems().get());
            case "gems_raw" -> String.valueOf(data.getGems().get());
            case "bank", "bankpoints" -> formatNumber(data.getBankPoints());
            case "bank_raw" -> String.valueOf(data.getBankPoints());
            case "total_wealth" -> formatNumber(data.getPoints().get() + data.getBankPoints());
            case "fragments" -> formatNumber(data.getFragments().get());

            // ==================== STATISTIQUES DE COMBAT ====================
            case "kills" -> formatNumber(data.getKills().get());
            case "kills_raw" -> String.valueOf(data.getKills().get());
            case "deaths" -> formatNumber(data.getDeaths().get());
            case "deaths_raw" -> String.valueOf(data.getDeaths().get());
            case "kd", "kdr", "ratio" -> decimalFormat.format(data.getKDRatio());
            case "killstreak", "streak" -> String.valueOf(data.getKillStreak().get());
            case "beststreak", "best_streak" -> String.valueOf(data.getBestKillStreak().get());

            // Kills par type
            case "zombie_kills" -> formatNumber(data.getZombieKills().get());
            case "elite_kills" -> formatNumber(data.getEliteKills().get());
            case "boss_kills" -> formatNumber(data.getBossKills().get());

            // Session
            case "session_kills" -> formatNumber(data.getSessionKills().get());
            case "session_time" -> formatTime(data.getSessionDuration());

            // ==================== ZONES ====================
            case "zone", "currentzone" -> String.valueOf(data.getCurrentZone().get());
            case "zone_name" -> getZoneName(data.getCurrentZone().get());
            case "zone_color" -> getZoneColor(data.getCurrentZone().get());
            case "zone_formatted" -> getZoneFormatted(data.getCurrentZone().get());
            case "maxzone", "highest_zone" -> String.valueOf(data.getMaxZone().get());
            case "checkpoint" -> String.valueOf(data.getCurrentCheckpoint().get());

            // ==================== ITEM LEVEL (ILVL) ====================
            case "ilvl", "itemlevel" -> String.valueOf(calculateAverageIlvl(player));
            case "iscore" -> String.valueOf(calculateTotalItemScore(player));
            case "iscore_formatted" -> formatNumber(calculateTotalItemScore(player));
            case "ilvl_weapon" -> String.valueOf(getItemIlvl(player.getInventory().getItemInMainHand()));
            case "ilvl_helmet" -> String.valueOf(getItemIlvl(player.getInventory().getHelmet()));
            case "ilvl_chest" -> String.valueOf(getItemIlvl(player.getInventory().getChestplate()));
            case "ilvl_legs" -> String.valueOf(getItemIlvl(player.getInventory().getLeggings()));
            case "ilvl_boots" -> String.valueOf(getItemIlvl(player.getInventory().getBoots()));

            // ==================== VIP ====================
            case "vip", "vip_rank" -> data.getVipRank();
            case "vip_formatted" -> formatVipRank(data.getVipRank());
            case "vip_color" -> getVipColor(data.getVipRank());
            case "vip_active" -> data.isVip() ? "&a&lVIP" : "";
            case "xp_multiplier" -> decimalFormat.format(data.getXpMultiplier()) + "x";
            case "points_multiplier" -> decimalFormat.format(data.getPointsMultiplier()) + "x";

            // ==================== TEMPS DE JEU ====================
            case "playtime" -> data.getFormattedPlaytime();
            case "playtime_hours" -> String.valueOf(data.getPlaytime().get() / 3600);
            case "playtime_raw" -> String.valueOf(data.getPlaytime().get());
            case "first_join" -> formatDate(data.getFirstJoin());

            // ==================== SKILLS ====================
            case "skill_points" -> String.valueOf(calculateAvailableSkillPoints(data));
            case "spent_skill_points" -> String.valueOf(data.getSpentSkillPoints());
            case "total_skill_points" -> String.valueOf(data.getLevel().get() / 5 + data.getBonusSkillPoints());
            case "skills_unlocked" -> String.valueOf(data.getUnlockedSkills().size());

            // ==================== ACHIEVEMENTS ====================
            case "achievements" -> String.valueOf(data.getUnlockedAchievements().size());
            case "achievements_total" -> String.valueOf(getTotalAchievements());

            // ==================== TITRES & COSMETICS ====================
            case "title" -> data.getActiveTitle().isEmpty() ? "" : data.getActiveTitle();
            case "cosmetic" -> data.getActiveCosmetic().isEmpty() ? "" : data.getActiveCosmetic();
            case "titles_count" -> String.valueOf(data.getUnlockedTitles().size());
            case "cosmetics_count" -> String.valueOf(data.getUnlockedCosmetics().size());

            // ==================== DAILY ====================
            case "daily_streak" -> String.valueOf(data.getDailyStreak());

            // ==================== BOOSTERS ====================
            case "booster_active" -> data.hasActiveBooster() ? "&a&lACTIF" : "&7Inactif";
            case "xp_boost" -> data.hasBooster("xp") ? decimalFormat.format(data.getXpBoostMultiplier()) + "x" : "-";
            case "loot_boost" -> data.hasBooster("loot") ? decimalFormat.format(data.getLootBoostMultiplier()) + "x" : "-";

            // ==================== MOMENTUM ====================
            case "combo" -> getCombo(player);
            case "fever" -> getFever(player);
            case "momentum_streak" -> getMomentumStreak(player);

            // ==================== PARTY ====================
            case "party_size" -> getPartySize(player);
            case "party_leader" -> getPartyLeader(player);

            // ==================== LEADERBOARD ====================
            case "rank_kills" -> getLeaderboardRank(player, "KILLS");
            case "rank_level" -> getLeaderboardRank(player, "LEVEL");
            case "rank_points" -> getLeaderboardRank(player, "POINTS");
            case "rank_zone" -> getLeaderboardRank(player, "MAX_ZONE");

            // ==================== CLASSES ====================
            case "class", "class_name" -> getClassName(player);
            case "class_id" -> getClassId(player);
            case "class_color" -> getClassColor(player);
            case "class_formatted" -> getClassFormatted(player);
            case "class_level", "class_lvl" -> getClassLevel(player);
            case "class_xp" -> getClassXp(player);
            case "class_xp_raw" -> getClassXpRaw(player);
            case "class_xp_required", "class_xpnext" -> getClassXpRequired(player);
            case "class_xp_progress" -> getClassXpProgress(player);
            case "class_xp_bar" -> getClassXpBar(player);
            case "class_kills" -> getClassKills(player);
            case "class_deaths" -> getClassDeaths(player);
            case "class_kd", "class_kdr" -> getClassKD(player);
            case "class_talents" -> getClassTalentsCount(player);
            case "class_tier" -> getClassCurrentTier(player);
            case "class_talent_tier1" -> getClassTalentForTier(player, TalentTier.TIER_1);
            case "class_talent_tier2" -> getClassTalentForTier(player, TalentTier.TIER_2);
            case "class_talent_tier3" -> getClassTalentForTier(player, TalentTier.TIER_3);
            case "class_talent_tier4" -> getClassTalentForTier(player, TalentTier.TIER_4);
            case "class_talent_tier5" -> getClassTalentForTier(player, TalentTier.TIER_5);
            case "class_talent_tier6" -> getClassTalentForTier(player, TalentTier.TIER_6);
            case "class_talent_tier7" -> getClassTalentForTier(player, TalentTier.TIER_7);
            case "class_talent_tier8" -> getClassTalentForTier(player, TalentTier.TIER_8);
            case "class_damage_dealt" -> getClassDamageDealt(player);
            case "class_damage_received" -> getClassDamageReceived(player);

            // ==================== SERVER STATS ====================
            case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "tps" -> getTPS();

            default -> null;
        };
    }

    /**
     * Gere les placeholders pour les joueurs hors ligne (stats serveur)
     */
    private String handleOfflinePlaceholder(String params) {
        return switch (params.toLowerCase()) {
            case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "tps" -> getTPS();
            default -> "";
        };
    }

    // ==================== HELPER METHODS ====================

    private String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return decimalFormat.format(number / 1_000_000_000.0) + "B";
        } else if (number >= 1_000_000) {
            return decimalFormat.format(number / 1_000_000.0) + "M";
        } else if (number >= 1_000) {
            return decimalFormat.format(number / 1_000.0) + "K";
        }
        return numberFormat.format(number);
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    private String formatDate(java.time.Instant instant) {
        if (instant == null) return "N/A";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy")
                .withZone(java.time.ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private String createProgressBar(double percentage, int length) {
        int filled = (int) (percentage / 100 * length);
        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("|");
            } else {
                bar.append("&7|");
            }
        }
        return bar.toString();
    }

    private String getPrestigeStars(int prestige) {
        if (prestige <= 0) return "";
        StringBuilder stars = new StringBuilder();
        String[] colors = {"&e", "&6", "&c", "&d", "&5", "&b", "&3", "&a", "&2", "&f"};
        int colorIndex = Math.min(prestige - 1, colors.length - 1);
        stars.append(colors[colorIndex]);
        for (int i = 0; i < Math.min(prestige, 10); i++) {
            stars.append("\u2605"); // Unicode star
        }
        return stars.toString();
    }

    private String getZoneName(int zoneId) {
        Zone zone = plugin.getZoneManager().getZoneById(zoneId);
        return zone != null ? zone.getName() : "Zone " + zoneId;
    }

    private String getZoneColor(int zoneId) {
        if (zoneId <= 5) return "&a";      // Vert - Facile
        if (zoneId <= 15) return "&e";     // Jaune - Normal
        if (zoneId <= 25) return "&6";     // Orange - Difficile
        if (zoneId <= 35) return "&c";     // Rouge - Tres difficile
        if (zoneId <= 45) return "&4";     // Rouge fonce - Extreme
        return "&5";                        // Violet - Legendaire
    }

    private String getZoneFormatted(int zoneId) {
        return getZoneColor(zoneId) + "Zone " + zoneId;
    }

    private int calculateAverageIlvl(Player player) {
        int total = 0;
        int count = 0;

        ItemStack[] items = {
            player.getInventory().getItemInMainHand(),
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };

        for (ItemStack item : items) {
            int ilvl = getItemIlvl(item);
            if (ilvl > 0) {
                total += ilvl;
                count++;
            }
        }

        return count > 0 ? total / count : 0;
    }

    /**
     * Calcule le score total d'items (somme de tous les iLvl de l'équipement)
     * Inclut: arme principale, casque, plastron, jambières, bottes, et arme secondaire
     */
    private int calculateTotalItemScore(Player player) {
        int total = 0;

        ItemStack[] items = {
            player.getInventory().getItemInMainHand(),
            player.getInventory().getItemInOffHand(),
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };

        for (ItemStack item : items) {
            int ilvl = getItemIlvl(item);
            if (ilvl > 0) {
                total += ilvl;
            }
        }

        return total;
    }

    private int getItemIlvl(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return 0;

        for (String line : meta.getLore()) {
            String stripped = line.replaceAll("§[0-9a-fk-or]", "");
            if (stripped.contains("Item Level:") || stripped.contains("iLvl:")) {
                try {
                    String numStr = stripped.replaceAll("[^0-9]", "");
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private String formatVipRank(String rank) {
        return switch (rank.toUpperCase()) {
            case "FREE" -> "&7Joueur";
            case "BRONZE" -> "&6Bronze";
            case "ARGENT" -> "&7&lArgent";
            case "OR" -> "&e&lOr";
            case "DIAMANT" -> "&b&lDiamant";
            case "ETERNEL" -> "&d&lEternel";
            default -> "&7" + rank;
        };
    }

    private String getVipColor(String rank) {
        return switch (rank.toUpperCase()) {
            case "BRONZE" -> "&6";
            case "ARGENT" -> "&7";
            case "OR" -> "&e";
            case "DIAMANT" -> "&b";
            case "ETERNEL" -> "&d";
            default -> "&7";
        };
    }

    private int calculateAvailableSkillPoints(PlayerData data) {
        int totalPoints = data.getLevel().get() / 5 + data.getBonusSkillPoints();
        return totalPoints - data.getSpentSkillPoints();
    }

    private int getTotalAchievements() {
        if (plugin.getAchievementManager() != null) {
            return plugin.getAchievementManager().getAchievements().size();
        }
        return 0;
    }

    private String getCombo(Player player) {
        if (plugin.getMomentumManager() == null) return "0";
        return String.valueOf(plugin.getMomentumManager().getCombo(player));
    }

    private String getFever(Player player) {
        if (plugin.getMomentumManager() == null) return "&7OFF";
        return plugin.getMomentumManager().isInFever(player) ? "&c&lFEVER!" : "&7OFF";
    }

    private String getMomentumStreak(Player player) {
        if (plugin.getMomentumManager() == null) return "0";
        return String.valueOf(plugin.getMomentumManager().getStreak(player));
    }

    private String getPartySize(Player player) {
        if (plugin.getPartyManager() == null) return "0";
        var party = plugin.getPartyManager().getParty(player);
        return party != null ? String.valueOf(party.getSize()) : "0";
    }

    private String getPartyLeader(Player player) {
        if (plugin.getPartyManager() == null) return "-";
        var party = plugin.getPartyManager().getParty(player);
        if (party == null) return "-";
        Player leader = Bukkit.getPlayer(party.getLeaderId());
        return leader != null ? leader.getName() : "-";
    }

    private String getLeaderboardRank(Player player, String type) {
        if (plugin.getLeaderboardManager() == null) return "-";
        try {
            var leaderboardType = com.rinaorc.zombiez.progression.LeaderboardManager.LeaderboardType.valueOf(type);
            int rank = plugin.getLeaderboardManager().getPlayerRank(player.getUniqueId(), leaderboardType);
            return rank > 0 ? "#" + rank : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private String getTPS() {
        double[] tps = Bukkit.getTPS();
        double currentTps = Math.min(tps[0], 20.0);
        String color;
        if (currentTps >= 18) color = "&a";
        else if (currentTps >= 15) color = "&e";
        else color = "&c";
        return color + String.format("%.1f", currentTps);
    }

    // ==================== CLASS HELPER METHODS ====================

    private ClassData getClassData(Player player) {
        if (plugin.getClassManager() == null) return null;
        return plugin.getClassManager().getClassData(player);
    }

    private String getClassName(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "Aucune";
        return data.getSelectedClass().getName();
    }

    private String getClassId(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "none";
        return data.getSelectedClass().getId();
    }

    private String getClassColor(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "&7";
        return data.getSelectedClass().getColor();
    }

    private String getClassFormatted(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "&7Aucune";
        return data.getSelectedClass().getColoredName();
    }

    private String getClassLevel(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return String.valueOf(data.getClassLevel().get());
    }

    private String getClassXp(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getClassXp().get());
    }

    private String getClassXpRaw(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return String.valueOf(data.getClassXp().get());
    }

    private String getClassXpRequired(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getRequiredXpForNextClassLevel());
    }

    private String getClassXpProgress(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0%";
        return decimalFormat.format(data.getClassLevelProgress()) + "%";
    }

    private String getClassXpBar(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return createProgressBar(0, 10);
        return createProgressBar(data.getClassLevelProgress(), 10);
    }

    private String getClassKills(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getClassKills().get());
    }

    private String getClassDeaths(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getClassDeaths().get());
    }

    private String getClassKD(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0.00";
        return decimalFormat.format(data.getClassKDRatio());
    }

    private String getClassTalentsCount(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        int count = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (data.getSelectedTalent(tier) != null) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getClassCurrentTier(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        int highestTier = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (data.isTalentTierUnlocked(tier)) {
                highestTier = tier.ordinal() + 1;
            }
        }
        return String.valueOf(highestTier);
    }

    private String getClassTalentForTier(Player player, TalentTier tier) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "-";
        String talentId = data.getSelectedTalent(tier);
        if (talentId == null || talentId.isEmpty()) return "-";

        if (plugin.getTalentManager() != null) {
            Talent talent = plugin.getTalentManager().getTalent(talentId);
            if (talent != null) {
                return talent.getName();
            }
        }
        return talentId;
    }

    private String getClassDamageDealt(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getDamageDealt().get());
    }

    private String getClassDamageReceived(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "0";
        return formatNumber(data.getDamageReceived().get());
    }
}
