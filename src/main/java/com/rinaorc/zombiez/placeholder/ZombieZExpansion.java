package com.rinaorc.zombiez.placeholder;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.beasts.BeastManager;
import com.rinaorc.zombiez.classes.beasts.BeastType;
import com.rinaorc.zombiez.classes.mutations.DailyMutation;
import com.rinaorc.zombiez.classes.perforation.PerforationManager;
import com.rinaorc.zombiez.classes.poison.PoisonManager;
import com.rinaorc.zombiez.classes.seasons.SeasonManager;
import com.rinaorc.zombiez.classes.shadow.ShadowManager;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentBranch;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.items.ItemManager;
import com.rinaorc.zombiez.momentum.MomentumManager;
import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.PetType;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.progression.BattlePassManager;
import com.rinaorc.zombiez.progression.MissionManager;
import com.rinaorc.zombiez.recycling.RecycleMilestone;
import com.rinaorc.zombiez.recycling.RecycleSettings;
import com.rinaorc.zombiez.zones.Zone;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
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
            case "xp" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "raw" -> String.valueOf(data.getXp().get());
                        case "required" -> formatNumber(data.getRequiredXpForNextLevel());
                        case "progress" -> decimalFormat.format(data.getLevelProgress()) + "%";
                        case "bar" -> createProgressBar(data.getLevelProgress(), 10);
                        case "multiplier" -> decimalFormat.format(data.getXpMultiplier()) + "x";
                        case "boost" -> data.hasBooster("xp") ? decimalFormat.format(data.getXpBoostMultiplier()) + "x" : "-";
                        default -> formatNumber(data.getXp().get());
                    };
                }
                yield formatNumber(data.getXp().get());
            }
            case "xpnext" -> formatNumber(data.getRequiredXpForNextLevel());
            case "prestige" -> {
                if (parts.length > 1 && parts[1].equals("star")) {
                    yield getPrestigeStars(data.getPrestige().get());
                }
                yield String.valueOf(data.getPrestige().get());
            }
            case "total" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "xp" -> formatNumber(data.getTotalXp().get());
                        case "wealth" -> formatNumber(data.getPoints().get() + data.getBankPoints());
                        case "damage" -> {
                            if (parts.length > 2 && parts[2].equals("dealt")) {
                                yield formatNumber(getTotalDamageDealt(player));
                            } else if (parts.length > 2 && parts[2].equals("received")) {
                                yield formatNumber(getTotalDamageReceived(player));
                            }
                            yield formatNumber(getTotalDamageDealt(player));
                        }
                        case "points" -> {
                            if (parts.length > 2 && parts[2].equals("earned")) {
                                yield formatNumber(data.getTotalPointsEarned().get());
                            }
                            yield formatNumber(data.getPoints().get() + data.getBankPoints());
                        }
                        default -> "0";
                    };
                }
                yield formatNumber(data.getTotalXp().get());
            }

            // ==================== ECONOMIE ====================
            case "points", "money", "coins" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "raw" -> String.valueOf(data.getPoints().get());
                        case "multiplier" -> decimalFormat.format(data.getPointsMultiplier()) + "x";
                        default -> formatNumber(data.getPoints().get());
                    };
                }
                yield formatNumber(data.getPoints().get());
            }
            case "gems" -> {
                if (parts.length > 1 && parts[1].equals("raw")) {
                    yield String.valueOf(data.getGems().get());
                }
                yield formatNumber(data.getGems().get());
            }
            case "bank", "bankpoints" -> {
                if (parts.length > 1 && parts[1].equals("raw")) {
                    yield String.valueOf(data.getBankPoints());
                }
                yield formatNumber(data.getBankPoints());
            }
            case "fragments" -> formatNumber(data.getFragments().get());

            // ==================== STATISTIQUES DE COMBAT ====================
            case "kills" -> {
                if (parts.length > 1 && parts[1].equals("raw")) {
                    yield String.valueOf(data.getKills().get());
                }
                yield formatNumber(data.getKills().get());
            }
            case "deaths" -> {
                if (parts.length > 1 && parts[1].equals("raw")) {
                    yield String.valueOf(data.getDeaths().get());
                }
                yield formatNumber(data.getDeaths().get());
            }
            case "kd", "kdr", "ratio" -> decimalFormat.format(data.getKDRatio());
            case "killstreak", "streak" -> String.valueOf(data.getKillStreak().get());
            case "beststreak" -> String.valueOf(data.getBestKillStreak().get());
            case "headshots" -> formatNumber(data.getHeadshots().get());
            case "assists" -> getAssists(player);

            // Kills par type
            case "zombie" -> {
                if (parts.length > 1 && parts[1].equals("kills")) {
                    yield formatNumber(data.getZombieKills().get());
                }
                yield formatNumber(data.getZombieKills().get());
            }
            case "elite" -> {
                if (parts.length > 1 && parts[1].equals("kills")) {
                    yield formatNumber(data.getEliteKills().get());
                }
                yield formatNumber(data.getEliteKills().get());
            }
            case "boss" -> {
                if (parts.length > 1 && parts[1].equals("kills")) {
                    yield formatNumber(data.getBossKills().get());
                }
                yield formatNumber(data.getBossKills().get());
            }

            // Session
            case "session" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "kills" -> formatNumber(data.getSessionKills().get());
                        case "time" -> formatTime(data.getSessionDuration());
                        case "xp" -> formatNumber(data.getSessionXp().get());
                        case "points" -> formatNumber(data.getSessionPoints().get());
                        default -> "0";
                    };
                }
                yield formatNumber(data.getSessionKills().get());
            }

            // ==================== ZONES ====================
            case "zone", "currentzone" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "name" -> getZoneName(data.getCurrentZone().get());
                        case "color" -> getZoneColor(data.getCurrentZone().get());
                        case "formatted" -> getZoneFormatted(data.getCurrentZone().get());
                        default -> String.valueOf(data.getCurrentZone().get());
                    };
                }
                yield String.valueOf(data.getCurrentZone().get());
            }
            case "maxzone", "highest" -> {
                if (parts.length > 1 && parts[1].equals("zone")) {
                    yield String.valueOf(data.getMaxZone().get());
                }
                yield String.valueOf(data.getMaxZone().get());
            }
            case "checkpoint" -> String.valueOf(data.getCurrentCheckpoint().get());
            case "secret" -> {
                if (parts.length > 1 && parts[1].equals("zones")) {
                    yield String.valueOf(getSecretZonesDiscovered(player));
                }
                yield String.valueOf(getSecretZonesDiscovered(player));
            }

            // ==================== ITEM LEVEL (ILVL) ====================
            case "ilvl", "itemlevel" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "weapon" -> String.valueOf(getItemIlvl(player.getInventory().getItemInMainHand()));
                        case "helmet" -> String.valueOf(getItemIlvl(player.getInventory().getHelmet()));
                        case "chest" -> String.valueOf(getItemIlvl(player.getInventory().getChestplate()));
                        case "legs" -> String.valueOf(getItemIlvl(player.getInventory().getLeggings()));
                        case "boots" -> String.valueOf(getItemIlvl(player.getInventory().getBoots()));
                        default -> String.valueOf(calculateAverageIlvl(player));
                    };
                }
                yield String.valueOf(calculateAverageIlvl(player));
            }
            case "iscore" -> {
                if (parts.length > 1 && parts[1].equals("formatted")) {
                    yield formatNumber(calculateTotalItemScore(player));
                }
                yield String.valueOf(calculateTotalItemScore(player));
            }

            // ==================== VIP ====================
            case "vip" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "rank" -> data.getVipRank();
                        case "formatted" -> formatVipRank(data.getVipRank());
                        case "color" -> getVipColor(data.getVipRank());
                        case "active" -> data.isVip() ? "&a&lVIP" : "";
                        default -> data.getVipRank();
                    };
                }
                yield data.getVipRank();
            }

            // ==================== TEMPS DE JEU ====================
            case "playtime" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "hours" -> String.valueOf(data.getPlaytime().get() / 3600);
                        case "raw" -> String.valueOf(data.getPlaytime().get());
                        default -> data.getFormattedPlaytime();
                    };
                }
                yield data.getFormattedPlaytime();
            }
            case "first" -> {
                if (parts.length > 1 && parts[1].equals("join")) {
                    yield formatDate(data.getFirstJoin());
                }
                yield formatDate(data.getFirstJoin());
            }

            // ==================== SKILLS ====================
            case "skill" -> {
                if (parts.length > 1 && parts[1].equals("points")) {
                    yield String.valueOf(calculateAvailableSkillPoints(data));
                }
                yield String.valueOf(calculateAvailableSkillPoints(data));
            }
            case "spent" -> {
                if (parts.length > 2 && parts[1].equals("skill") && parts[2].equals("points")) {
                    yield String.valueOf(data.getSpentSkillPoints());
                }
                yield String.valueOf(data.getSpentSkillPoints());
            }
            case "skills" -> {
                if (parts.length > 1 && parts[1].equals("unlocked")) {
                    yield String.valueOf(data.getUnlockedSkills().size());
                }
                yield String.valueOf(data.getUnlockedSkills().size());
            }

            // ==================== ACHIEVEMENTS ====================
            case "achievements" -> {
                if (parts.length > 1 && parts[1].equals("total")) {
                    yield String.valueOf(getTotalAchievements());
                }
                yield String.valueOf(data.getUnlockedAchievements().size());
            }

            // ==================== TITRES & COSMETICS ====================
            case "title" -> {
                if (parts.length > 1 && parts[1].equals("count")) {
                    yield String.valueOf(data.getUnlockedTitles().size());
                }
                yield data.getActiveTitle().isEmpty() ? "" : data.getActiveTitle();
            }
            case "titles" -> {
                if (parts.length > 1 && parts[1].equals("count")) {
                    yield String.valueOf(data.getUnlockedTitles().size());
                }
                yield String.valueOf(data.getUnlockedTitles().size());
            }
            case "cosmetic" -> {
                if (parts.length > 1 && parts[1].equals("count")) {
                    yield String.valueOf(data.getUnlockedCosmetics().size());
                }
                yield data.getActiveCosmetic().isEmpty() ? "" : data.getActiveCosmetic();
            }
            case "cosmetics" -> {
                if (parts.length > 1 && parts[1].equals("count")) {
                    yield String.valueOf(data.getUnlockedCosmetics().size());
                }
                yield String.valueOf(data.getUnlockedCosmetics().size());
            }

            // ==================== DAILY ====================
            case "daily" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "streak" -> String.valueOf(data.getDailyStreak());
                        case "mission" -> getDailyMissionPlaceholder(player, parts);
                        case "reset" -> formatTime(getDailyResetTime());
                        default -> String.valueOf(data.getDailyStreak());
                    };
                }
                yield String.valueOf(data.getDailyStreak());
            }

            // ==================== WEEKLY ====================
            case "weekly" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "mission" -> getWeeklyMissionPlaceholder(player, parts);
                        case "reset" -> formatTime(getWeeklyResetTime());
                        default -> "0";
                    };
                }
                yield "0";
            }

            // ==================== MISSIONS ====================
            case "mission" -> getMissionPlaceholder(player, parts);

            // ==================== BOOSTERS ====================
            case "booster" -> {
                if (parts.length > 1 && parts[1].equals("active")) {
                    yield data.hasActiveBooster() ? "&a&lACTIF" : "&7Inactif";
                }
                yield data.hasActiveBooster() ? "&a&lACTIF" : "&7Inactif";
            }
            case "loot" -> {
                if (parts.length > 1 && parts[1].equals("boost")) {
                    yield data.hasBooster("loot") ? decimalFormat.format(data.getLootBoostMultiplier()) + "x" : "-";
                }
                yield "-";
            }

            // ==================== MOMENTUM ====================
            case "combo" -> getCombo(player);
            case "fever" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "time" -> getFeverTimeRemaining(player);
                        case "progress" -> getFeverProgress(player);
                        default -> getFever(player);
                    };
                }
                yield getFever(player);
            }
            case "momentum" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "streak" -> getMomentumStreak(player);
                        case "damage" -> getMomentumDamageBonus(player);
                        case "speed" -> getMomentumSpeedBonus(player);
                        default -> getMomentumStreak(player);
                    };
                }
                yield getMomentumStreak(player);
            }

            // ==================== PARTY ====================
            case "party" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "size" -> getPartySize(player);
                        case "leader" -> getPartyLeader(player);
                        default -> getPartySize(player);
                    };
                }
                yield getPartySize(player);
            }

            // ==================== LEADERBOARD ====================
            case "rank" -> {
                if (parts.length > 1) {
                    yield switch (parts[1]) {
                        case "kills" -> getLeaderboardRank(player, "KILLS");
                        case "level" -> getLeaderboardRank(player, "LEVEL");
                        case "points" -> getLeaderboardRank(player, "POINTS");
                        case "zone" -> getLeaderboardRank(player, "MAX_ZONE");
                        default -> "-";
                    };
                }
                yield "-";
            }

            // ==================== CLASSES ====================
            case "class" -> getClassPlaceholder(player, parts);

            // ==================== PETS ====================
            case "pet" -> getPetPlaceholder(player, parts);

            // ==================== BATTLE PASS ====================
            case "battlepass", "bp" -> getBattlePassPlaceholder(player, parts);

            // ==================== MUTATIONS ====================
            case "mutation" -> getMutationPlaceholder(parts);

            // ==================== RECYCLAGE ====================
            case "recycle" -> getRecyclePlaceholder(player, parts);

            // ==================== SAISONS ====================
            case "season" -> getSeasonPlaceholder(parts);

            // ==================== ÉVÉNEMENTS ====================
            case "event" -> getEventPlaceholder(player, parts);

            // ==================== AWAKENS ====================
            case "awaken" -> getAwakenPlaceholder(player, parts);

            // ==================== SERVER STATS ====================
            case "online" -> String.valueOf(Bukkit.getOnlinePlayers().size());
            case "max" -> {
                if (parts.length > 1 && parts[1].equals("players")) {
                    yield String.valueOf(Bukkit.getMaxPlayers());
                }
                yield String.valueOf(Bukkit.getMaxPlayers());
            }
            case "tps" -> getTPS();

            // ==================== LEGACY COMPATIBILITY ====================
            case "xp_raw" -> String.valueOf(data.getXp().get());
            case "xp_required" -> formatNumber(data.getRequiredXpForNextLevel());
            case "xp_progress" -> decimalFormat.format(data.getLevelProgress()) + "%";
            case "xp_bar" -> createProgressBar(data.getLevelProgress(), 10);
            case "prestige_star" -> getPrestigeStars(data.getPrestige().get());
            case "total_xp" -> formatNumber(data.getTotalXp().get());
            case "points_raw" -> String.valueOf(data.getPoints().get());
            case "gems_raw" -> String.valueOf(data.getGems().get());
            case "bank_raw" -> String.valueOf(data.getBankPoints());
            case "total_wealth" -> formatNumber(data.getPoints().get() + data.getBankPoints());
            case "kills_raw" -> String.valueOf(data.getKills().get());
            case "deaths_raw" -> String.valueOf(data.getDeaths().get());
            case "best_streak" -> String.valueOf(data.getBestKillStreak().get());
            case "zombie_kills" -> formatNumber(data.getZombieKills().get());
            case "elite_kills" -> formatNumber(data.getEliteKills().get());
            case "boss_kills" -> formatNumber(data.getBossKills().get());
            case "session_kills" -> formatNumber(data.getSessionKills().get());
            case "session_time" -> formatTime(data.getSessionDuration());
            case "zone_name" -> getZoneName(data.getCurrentZone().get());
            case "zone_color" -> getZoneColor(data.getCurrentZone().get());
            case "zone_formatted" -> getZoneFormatted(data.getCurrentZone().get());
            case "highest_zone" -> String.valueOf(data.getMaxZone().get());
            case "vip_rank" -> data.getVipRank();
            case "vip_formatted" -> formatVipRank(data.getVipRank());
            case "vip_color" -> getVipColor(data.getVipRank());
            case "vip_active" -> data.isVip() ? "&a&lVIP" : "";
            case "xp_multiplier" -> decimalFormat.format(data.getXpMultiplier()) + "x";
            case "points_multiplier" -> decimalFormat.format(data.getPointsMultiplier()) + "x";
            case "playtime_hours" -> String.valueOf(data.getPlaytime().get() / 3600);
            case "playtime_raw" -> String.valueOf(data.getPlaytime().get());
            case "first_join" -> formatDate(data.getFirstJoin());
            case "skill_points" -> String.valueOf(calculateAvailableSkillPoints(data));
            case "spent_skill_points" -> String.valueOf(data.getSpentSkillPoints());
            case "total_skill_points" -> String.valueOf(data.getLevel().get() / 5 + data.getBonusSkillPoints());
            case "skills_unlocked" -> String.valueOf(data.getUnlockedSkills().size());
            case "achievements_total" -> String.valueOf(getTotalAchievements());
            case "titles_count" -> String.valueOf(data.getUnlockedTitles().size());
            case "cosmetics_count" -> String.valueOf(data.getUnlockedCosmetics().size());
            case "daily_streak" -> String.valueOf(data.getDailyStreak());
            case "booster_active" -> data.hasActiveBooster() ? "&a&lACTIF" : "&7Inactif";
            case "xp_boost" -> data.hasBooster("xp") ? decimalFormat.format(data.getXpBoostMultiplier()) + "x" : "-";
            case "loot_boost" -> data.hasBooster("loot") ? decimalFormat.format(data.getLootBoostMultiplier()) + "x" : "-";
            case "momentum_streak" -> getMomentumStreak(player);
            case "party_size" -> getPartySize(player);
            case "party_leader" -> getPartyLeader(player);
            case "rank_kills" -> getLeaderboardRank(player, "KILLS");
            case "rank_level" -> getLeaderboardRank(player, "LEVEL");
            case "rank_points" -> getLeaderboardRank(player, "POINTS");
            case "rank_zone" -> getLeaderboardRank(player, "MAX_ZONE");
            case "max_players" -> String.valueOf(Bukkit.getMaxPlayers());
            case "class_name" -> getClassName(player);
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
            case "iscore_formatted" -> formatNumber(calculateTotalItemScore(player));
            case "ilvl_weapon" -> String.valueOf(getItemIlvl(player.getInventory().getItemInMainHand()));
            case "ilvl_helmet" -> String.valueOf(getItemIlvl(player.getInventory().getHelmet()));
            case "ilvl_chest" -> String.valueOf(getItemIlvl(player.getInventory().getChestplate()));
            case "ilvl_legs" -> String.valueOf(getItemIlvl(player.getInventory().getLeggings()));
            case "ilvl_boots" -> String.valueOf(getItemIlvl(player.getInventory().getBoots()));
            // Legacy class branch placeholders
            case "class_branch" -> getBranchPlaceholder(player, new String[]{"class", "branch"});
            case "class_branch_name" -> getBranchPlaceholder(player, new String[]{"class", "branch", "name"});
            case "class_branch_id" -> getBranchPlaceholder(player, new String[]{"class", "branch", "id"});
            case "class_branch_formatted" -> getBranchPlaceholder(player, new String[]{"class", "branch", "formatted"});
            case "class_branch_color" -> getBranchPlaceholder(player, new String[]{"class", "branch", "color"});
            // Legacy shadow placeholders
            case "class_shadow_points" -> getShadowPlaceholder(player, new String[]{"class", "shadow", "points"});
            case "class_shadow_points_bar" -> getShadowPlaceholder(player, new String[]{"class", "shadow", "points_bar"});
            case "class_shadow_avatar" -> getShadowPlaceholder(player, new String[]{"class", "shadow", "avatar"});
            case "class_shadow_blades" -> getShadowPlaceholder(player, new String[]{"class", "shadow", "blades"});
            // Legacy frost placeholders
            case "class_frost_charge" -> getFrostPlaceholder(player, new String[]{"class", "frost", "charge"});
            case "class_frost_charge_bar" -> getFrostPlaceholder(player, new String[]{"class", "frost", "charge_bar"});
            case "class_frost_hypothermia" -> getFrostPlaceholder(player, new String[]{"class", "frost", "hypothermia"});
            case "class_frost_blizzard" -> getFrostPlaceholder(player, new String[]{"class", "frost", "blizzard"});
            // Legacy beast placeholders
            case "class_beast_count" -> getBeastPlaceholder(player, new String[]{"class", "beast", "count"});
            case "class_beast_list" -> getBeastPlaceholder(player, new String[]{"class", "beast", "list"});
            // Legacy poison placeholders
            case "class_poison_virulence" -> getPoisonPlaceholder(player, new String[]{"class", "poison", "virulence"});
            case "class_poison_virulence_bar" -> getPoisonPlaceholder(player, new String[]{"class", "poison", "virulence_bar"});
            case "class_poison_avatar" -> getPoisonPlaceholder(player, new String[]{"class", "poison", "avatar"});

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
            // Mutations (globales)
            case "mutation_count" -> getMutationCount();
            case "mutation_1" -> getMutationName(0);
            case "mutation_2" -> getMutationName(1);
            case "mutation_3" -> getMutationName(2);
            case "mutation_special" -> isMutationSpecialDay();
            // Saison (globale)
            case "season_number" -> getSeasonNumber();
            case "season_name", "season_theme" -> getSeasonTheme();
            case "season_days" -> getSeasonDaysRemaining();
            case "season_progress" -> getSeasonProgress();
            default -> "";
        };
    }

    // ==================== PET PLACEHOLDERS ====================

    private String getPetPlaceholder(Player player, String[] parts) {
        if (plugin.getPetManager() == null) return "N/A";

        PlayerPetData petData = plugin.getPetManager().getPlayerData(player.getUniqueId());
        if (petData == null) return "0";

        if (parts.length < 2) {
            return petData.getEquippedPet() != null ? petData.getEquippedPet().getDisplayName() : "Aucun";
        }

        return switch (parts[1]) {
            case "equipped", "name" -> petData.getEquippedPet() != null ? petData.getEquippedPet().getDisplayName() : "Aucun";
            case "level" -> {
                if (petData.getEquippedPet() != null) {
                    PetData pd = petData.getPet(petData.getEquippedPet());
                    yield pd != null ? String.valueOf(pd.getLevel()) : "0";
                }
                yield "0";
            }
            case "star", "starpower" -> {
                if (petData.getEquippedPet() != null) {
                    PetData pd = petData.getPet(petData.getEquippedPet());
                    yield pd != null ? String.valueOf(pd.getStarPower()) : "0";
                }
                yield "0";
            }
            case "copies" -> {
                if (petData.getEquippedPet() != null) {
                    PetData pd = petData.getPet(petData.getEquippedPet());
                    yield pd != null ? String.valueOf(pd.getCopies()) : "0";
                }
                yield "0";
            }
            case "damage" -> {
                if (petData.getEquippedPet() != null) {
                    PetData pd = petData.getPet(petData.getEquippedPet());
                    yield pd != null ? formatNumber(pd.getTotalDamageDealt()) : "0";
                }
                yield "0";
            }
            case "kills" -> {
                if (petData.getEquippedPet() != null) {
                    PetData pd = petData.getPet(petData.getEquippedPet());
                    yield pd != null ? formatNumber(pd.getTotalKills()) : "0";
                }
                yield "0";
            }
            case "collection", "count" -> String.valueOf(petData.getOwnedPets().size());
            case "fragments" -> formatNumber(petData.getFragments());
            case "eggs" -> {
                if (parts.length > 2 && parts[2].equals("opened")) {
                    yield formatNumber(petData.getTotalEggsOpened());
                }
                yield formatNumber(petData.getTotalEggsOpened());
            }
            case "legendaries" -> String.valueOf(petData.getLegendariesObtained());
            case "mythics" -> String.valueOf(petData.getMythicsObtained());
            case "rarity" -> {
                if (petData.getEquippedPet() != null) {
                    yield petData.getEquippedPet().getRarity().getColoredName();
                }
                yield "-";
            }
            case "color" -> {
                if (petData.getEquippedPet() != null) {
                    yield petData.getEquippedPet().getRarity().getColor();
                }
                yield "&7";
            }
            case "formatted" -> {
                if (petData.getEquippedPet() != null) {
                    yield petData.getEquippedPet().getColoredName();
                }
                yield "&7Aucun";
            }
            default -> "0";
        };
    }

    // ==================== BATTLE PASS PLACEHOLDERS ====================

    private String getBattlePassPlaceholder(Player player, String[] parts) {
        if (plugin.getBattlePassManager() == null) return "0";

        BattlePassManager.PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerPass(player.getUniqueId());
        if (bp == null) return "0";

        if (parts.length < 2) {
            return String.valueOf(bp.getLevel());
        }

        return switch (parts[1]) {
            case "level", "lvl" -> String.valueOf(bp.getLevel());
            case "xp" -> {
                if (parts.length > 2 && parts[2].equals("raw")) {
                    yield String.valueOf(bp.getXp());
                }
                yield formatNumber(bp.getCurrentLevelXp());
            }
            case "xp_required", "xpnext" -> formatNumber(bp.getXpForNextLevel());
            case "progress" -> decimalFormat.format(bp.getProgressPercent()) + "%";
            case "bar" -> createProgressBar(bp.getProgressPercent(), 10);
            case "premium" -> bp.isPremium() ? "&d&lPREMIUM" : "&7Standard";
            case "is_premium" -> bp.isPremium() ? "true" : "false";
            case "days", "days_remaining" -> String.valueOf(plugin.getBattlePassManager().getDaysRemaining());
            case "season" -> String.valueOf(plugin.getBattlePassManager().getCurrentSeason().getId());
            case "season_name" -> plugin.getBattlePassManager().getCurrentSeason().getName();
            case "free_claimed" -> String.valueOf(bp.getClaimedFree().size());
            case "premium_claimed" -> String.valueOf(bp.getClaimedPremium().size());
            default -> "0";
        };
    }

    // ==================== MISSION PLACEHOLDERS ====================

    private String getMissionPlaceholder(Player player, String[] parts) {
        if (plugin.getMissionManager() == null) return "0";

        MissionManager.PlayerMissions missions = plugin.getMissionManager().getMissions(player.getUniqueId());
        if (missions == null) return "0";

        if (parts.length < 2) {
            return String.valueOf(missions.getDailyMissions().size() + missions.getWeeklyMissions().size());
        }

        return switch (parts[1]) {
            case "daily" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "count" -> String.valueOf(missions.getDailyMissions().size());
                        case "completed" -> String.valueOf(missions.getCompletedDailyCount());
                        case "remaining" -> String.valueOf(missions.getDailyMissions().size() - missions.getCompletedDailyCount());
                        default -> String.valueOf(missions.getDailyMissions().size());
                    };
                }
                yield String.valueOf(missions.getDailyMissions().size());
            }
            case "weekly" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "count" -> String.valueOf(missions.getWeeklyMissions().size());
                        case "completed" -> String.valueOf(missions.getCompletedWeeklyCount());
                        case "remaining" -> String.valueOf(missions.getWeeklyMissions().size() - missions.getCompletedWeeklyCount());
                        default -> String.valueOf(missions.getWeeklyMissions().size());
                    };
                }
                yield String.valueOf(missions.getWeeklyMissions().size());
            }
            case "total" -> {
                if (parts.length > 2 && parts[2].equals("completed")) {
                    yield String.valueOf(missions.getCompletedDailyCount() + missions.getCompletedWeeklyCount());
                }
                yield String.valueOf(missions.getDailyMissions().size() + missions.getWeeklyMissions().size());
            }
            default -> "0";
        };
    }

    private String getDailyMissionPlaceholder(Player player, String[] parts) {
        if (plugin.getMissionManager() == null) return "0";
        MissionManager.PlayerMissions missions = plugin.getMissionManager().getMissions(player.getUniqueId());
        if (missions == null) return "0";

        if (parts.length > 2) {
            return switch (parts[2]) {
                case "count" -> String.valueOf(missions.getDailyMissions().size());
                case "completed" -> String.valueOf(missions.getCompletedDailyCount());
                default -> String.valueOf(missions.getDailyMissions().size());
            };
        }
        return String.valueOf(missions.getDailyMissions().size());
    }

    private String getWeeklyMissionPlaceholder(Player player, String[] parts) {
        if (plugin.getMissionManager() == null) return "0";
        MissionManager.PlayerMissions missions = plugin.getMissionManager().getMissions(player.getUniqueId());
        if (missions == null) return "0";

        if (parts.length > 2) {
            return switch (parts[2]) {
                case "count" -> String.valueOf(missions.getWeeklyMissions().size());
                case "completed" -> String.valueOf(missions.getCompletedWeeklyCount());
                default -> String.valueOf(missions.getWeeklyMissions().size());
            };
        }
        return String.valueOf(missions.getWeeklyMissions().size());
    }

    private long getDailyResetTime() {
        if (plugin.getMissionManager() == null) return 0;
        return plugin.getMissionManager().getTimeUntilDailyReset();
    }

    private long getWeeklyResetTime() {
        if (plugin.getMissionManager() == null) return 0;
        return plugin.getMissionManager().getTimeUntilWeeklyReset();
    }

    // ==================== MUTATION PLACEHOLDERS ====================

    /**
     * Helper pour accéder au MutationManager via ClassManager
     */
    private com.rinaorc.zombiez.classes.mutations.MutationManager getMutationManager() {
        if (plugin.getClassManager() == null) return null;
        return plugin.getClassManager().getMutationManager();
    }

    private String getMutationPlaceholder(String[] parts) {
        if (getMutationManager() == null) return "0";

        if (parts.length < 2) {
            return getMutationCount();
        }

        return switch (parts[1]) {
            case "count" -> getMutationCount();
            case "1" -> getMutationName(0);
            case "2" -> getMutationName(1);
            case "3" -> getMutationName(2);
            case "special", "special_day" -> isMutationSpecialDay();
            case "list" -> getMutationList();
            default -> {
                // Tenter de parser comme index
                try {
                    int index = Integer.parseInt(parts[1]) - 1;
                    yield getMutationName(index);
                } catch (NumberFormatException e) {
                    yield "0";
                }
            }
        };
    }

    private String getMutationCount() {
        if (getMutationManager() == null) return "0";
        return String.valueOf(getMutationManager().getActiveMutationCount());
    }

    private String getMutationName(int index) {
        if (getMutationManager() == null) return "-";
        List<DailyMutation> mutations = getMutationManager().getActiveMutations();
        if (index < 0 || index >= mutations.size()) return "-";
        return mutations.get(index).getName();
    }

    private String isMutationSpecialDay() {
        if (getMutationManager() == null) return "false";
        return getMutationManager().isSpecialDay() ? "&6&lSPECIAL" : "&7Normal";
    }

    private String getMutationList() {
        if (getMutationManager() == null) return "";
        List<DailyMutation> mutations = getMutationManager().getActiveMutations();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mutations.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(mutations.get(i).getRarity().getColor()).append(mutations.get(i).getName());
        }
        return sb.toString();
    }

    // ==================== RECYCLE PLACEHOLDERS ====================

    private String getRecyclePlaceholder(Player player, String[] parts) {
        if (plugin.getRecycleManager() == null) return "0";

        RecycleSettings settings = plugin.getRecycleManager().getSettings(player.getUniqueId());
        if (settings == null) return "0";

        if (parts.length < 2) {
            return settings.isAutoRecycleEnabled() ? "&a&lACTIF" : "&7Inactif";
        }

        return switch (parts[1]) {
            case "enabled", "active" -> settings.isAutoRecycleEnabled() ? "&a&lACTIF" : "&7Inactif";
            case "session" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "items" -> formatNumber(settings.getSessionItemsRecycled().get());
                        case "points" -> formatNumber(settings.getSessionPointsEarned().get());
                        default -> "0";
                    };
                }
                yield formatNumber(settings.getSessionItemsRecycled().get());
            }
            case "total" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "items" -> formatNumber(settings.getTotalItemsRecycled().get());
                        case "points" -> formatNumber(settings.getTotalPointsEarned().get());
                        default -> "0";
                    };
                }
                yield formatNumber(settings.getTotalItemsRecycled().get());
            }
            case "milestones" -> {
                if (parts.length > 2 && parts[2].equals("total")) {
                    yield String.valueOf(RecycleMilestone.values().length);
                }
                yield String.valueOf(settings.getUnlockedMilestones().size());
            }
            case "best" -> formatNumber(settings.getBestSingleRecycle());
            case "rarities" -> String.valueOf(settings.getEnabledRaritiesCount());
            default -> "0";
        };
    }

    // ==================== SEASON PLACEHOLDERS ====================

    private String getSeasonPlaceholder(String[] parts) {
        if (parts.length < 2) {
            return getSeasonNumber();
        }

        return switch (parts[1]) {
            case "number", "id" -> getSeasonNumber();
            case "name", "theme" -> getSeasonTheme();
            case "days", "days_remaining" -> getSeasonDaysRemaining();
            case "progress" -> getSeasonProgress();
            default -> "0";
        };
    }

    private String getSeasonNumber() {
        if (plugin.getClassManager() == null) return "1";
        SeasonManager sm = plugin.getClassManager().getSeasonManager();
        return sm != null ? String.valueOf(sm.getCurrentSeasonNumber()) : "1";
    }

    private String getSeasonTheme() {
        if (plugin.getClassManager() == null) return "Saison 1";
        SeasonManager sm = plugin.getClassManager().getSeasonManager();
        return sm != null ? sm.getSeasonTheme() : "Saison 1";
    }

    private String getSeasonDaysRemaining() {
        if (plugin.getClassManager() == null) return "0";
        SeasonManager sm = plugin.getClassManager().getSeasonManager();
        return sm != null ? String.valueOf(sm.getDaysRemaining()) : "0";
    }

    private String getSeasonProgress() {
        if (plugin.getClassManager() == null) return "0%";
        SeasonManager sm = plugin.getClassManager().getSeasonManager();
        return sm != null ? decimalFormat.format(sm.getSeasonProgress()) + "%" : "0%";
    }

    // ==================== EVENT PLACEHOLDERS ====================

    private String getEventPlaceholder(Player player, String[] parts) {
        if (parts.length < 2) {
            return isEventActive() ? "&a&lACTIF" : "&7Aucun";
        }

        return switch (parts[1]) {
            case "active" -> isEventActive() ? "&a&lACTIF" : "&7Aucun";
            case "name" -> getActiveEventName();
            case "count" -> getActiveEventCount();
            case "micro" -> {
                if (parts.length > 2 && parts[2].equals("active")) {
                    yield isMicroEventActive() ? "&a&lACTIF" : "&7Aucun";
                }
                yield getActiveMicroEventName();
            }
            default -> "0";
        };
    }

    private String isEventActive() {
        if (plugin.getDynamicEventManager() == null) return "&7Aucun";
        return !plugin.getDynamicEventManager().getActiveEvents().isEmpty() ? "&a&lACTIF" : "&7Aucun";
    }

    private String getActiveEventName() {
        if (plugin.getDynamicEventManager() == null) return "-";
        var events = plugin.getDynamicEventManager().getActiveEvents();
        if (events.isEmpty()) return "-";
        return events.values().iterator().next().getType().getDisplayName();
    }

    private String getActiveEventCount() {
        if (plugin.getDynamicEventManager() == null) return "0";
        return String.valueOf(plugin.getDynamicEventManager().getActiveEvents().size());
    }

    private String isMicroEventActive() {
        if (plugin.getMicroEventManager() == null) return "&7Aucun";
        return !plugin.getMicroEventManager().getActiveEvents().isEmpty() ? "&a&lACTIF" : "&7Aucun";
    }

    private String getActiveMicroEventName() {
        if (plugin.getMicroEventManager() == null) return "-";
        var events = plugin.getMicroEventManager().getActiveEvents();
        if (events.isEmpty()) return "-";
        return events.values().iterator().next().getType().getDisplayName();
    }

    // ==================== AWAKEN PLACEHOLDERS ====================

    private String getAwakenPlaceholder(Player player, String[] parts) {
        if (plugin.getAwakenManager() == null) return "-";

        ClassData classData = getClassData(player);
        if (classData == null) return "-";

        if (parts.length < 2) {
            return classData.getEquippedAwaken() != null ? classData.getEquippedAwaken().getName() : "-";
        }

        return switch (parts[1]) {
            case "equipped", "name" -> classData.getEquippedAwaken() != null ? classData.getEquippedAwaken().getName() : "-";
            case "effect" -> classData.getEquippedAwaken() != null ? classData.getEquippedAwaken().getEffect().getDisplayName() : "-";
            case "rarity" -> classData.getEquippedAwaken() != null ? classData.getEquippedAwaken().getRarity().getColoredName() : "-";
            case "count" -> String.valueOf(classData.getUnlockedAwakens().size());
            case "formatted" -> {
                if (classData.getEquippedAwaken() != null) {
                    yield classData.getEquippedAwaken().getRarity().getColor() + classData.getEquippedAwaken().getName();
                }
                yield "&7-";
            }
            default -> "-";
        };
    }

    // ==================== MOMENTUM HELPER METHODS ====================

    private String getMomentumDamageBonus(Player player) {
        if (plugin.getMomentumManager() == null) return "1.0x";
        double mult = plugin.getMomentumManager().getDamageMultiplier(player);
        return decimalFormat.format(mult) + "x";
    }

    private String getMomentumSpeedBonus(Player player) {
        if (plugin.getMomentumManager() == null) return "1.0x";
        double mult = plugin.getMomentumManager().getSpeedMultiplier(player);
        return decimalFormat.format(mult) + "x";
    }

    private String getFeverTimeRemaining(Player player) {
        if (plugin.getMomentumManager() == null) return "0s";
        long remaining = plugin.getMomentumManager().getFeverTimeRemaining(player);
        return (remaining / 1000) + "s";
    }

    private String getFeverProgress(Player player) {
        if (plugin.getMomentumManager() == null) return "0%";
        double progress = plugin.getMomentumManager().getFeverProgress(player);
        return decimalFormat.format(progress * 100) + "%";
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
        if (seconds < 86400) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        return (seconds / 86400) + "j " + ((seconds % 86400) / 3600) + "h";
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

        org.bukkit.inventory.ItemStack[] items = {
            player.getInventory().getItemInMainHand(),
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };

        for (org.bukkit.inventory.ItemStack item : items) {
            int ilvl = getItemIlvl(item);
            if (ilvl > 0) {
                total += ilvl;
                count++;
            }
        }

        return count > 0 ? total / count : 0;
    }

    private int calculateTotalItemScore(Player player) {
        int total = 0;

        org.bukkit.inventory.ItemStack[] items = {
            player.getInventory().getItemInMainHand(),
            player.getInventory().getItemInOffHand(),
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };

        for (org.bukkit.inventory.ItemStack item : items) {
            int ilvl = getItemIlvl(item);
            if (ilvl > 0) {
                total += ilvl;
            }
        }

        return total;
    }

    private int getItemIlvl(org.bukkit.inventory.ItemStack item) {
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

    private String getAssists(Player player) {
        // AssistManager ne stocke pas de compteur persistant, retourne 0 par défaut
        // TODO: Ajouter le tracking d'assists dans PlayerData si nécessaire
        return "0";
    }

    private long getTotalDamageDealt(Player player) {
        ClassData classData = getClassData(player);
        if (classData != null) {
            return classData.getDamageDealt().get();
        }
        return 0;
    }

    private long getTotalDamageReceived(Player player) {
        ClassData classData = getClassData(player);
        if (classData != null) {
            return classData.getDamageReceived().get();
        }
        return 0;
    }

    private int getSecretZonesDiscovered(Player player) {
        if (plugin.getSecretZoneManager() == null) return 0;
        var discovered = plugin.getSecretZoneManager().getDiscoveredZones().get(player.getUniqueId());
        return discovered != null ? discovered.size() : 0;
    }

    // ==================== CLASS HELPER METHODS ====================

    private String getClassPlaceholder(Player player, String[] parts) {
        if (parts.length < 2) {
            return getClassName(player);
        }

        return switch (parts[1]) {
            case "name" -> getClassName(player);
            case "id" -> getClassId(player);
            case "color" -> getClassColor(player);
            case "formatted" -> getClassFormatted(player);
            case "level", "lvl" -> getClassLevel(player);
            case "xp" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "raw" -> getClassXpRaw(player);
                        case "required" -> getClassXpRequired(player);
                        case "progress" -> getClassXpProgress(player);
                        case "bar" -> getClassXpBar(player);
                        default -> getClassXp(player);
                    };
                }
                yield getClassXp(player);
            }
            case "kills" -> getClassKills(player);
            case "deaths" -> getClassDeaths(player);
            case "kd", "kdr" -> getClassKD(player);
            case "talents" -> getClassTalentsCount(player);
            case "tier" -> getClassCurrentTier(player);
            case "damage" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "dealt" -> getClassDamageDealt(player);
                        case "received" -> getClassDamageReceived(player);
                        default -> getClassDamageDealt(player);
                    };
                }
                yield getClassDamageDealt(player);
            }
            case "talent" -> {
                if (parts.length > 2) {
                    yield switch (parts[2]) {
                        case "tier1" -> getClassTalentForTier(player, TalentTier.TIER_1);
                        case "tier2" -> getClassTalentForTier(player, TalentTier.TIER_2);
                        case "tier3" -> getClassTalentForTier(player, TalentTier.TIER_3);
                        case "tier4" -> getClassTalentForTier(player, TalentTier.TIER_4);
                        case "tier5" -> getClassTalentForTier(player, TalentTier.TIER_5);
                        case "tier6" -> getClassTalentForTier(player, TalentTier.TIER_6);
                        case "tier7" -> getClassTalentForTier(player, TalentTier.TIER_7);
                        case "tier8" -> getClassTalentForTier(player, TalentTier.TIER_8);
                        default -> "-";
                    };
                }
                yield "-";
            }
            // ========== BRANCHE ==========
            case "branch" -> getBranchPlaceholder(player, parts);
            // ========== OMBRE (Shadow) ==========
            case "shadow", "ombre" -> getShadowPlaceholder(player, parts);
            // ========== GIVRE (Frost/Perforation) ==========
            case "frost", "givre" -> getFrostPlaceholder(player, parts);
            // ========== BÊTES (Beast) ==========
            case "beast", "bete" -> getBeastPlaceholder(player, parts);
            // ========== POISON (Virulence) ==========
            case "poison", "virulence" -> getPoisonPlaceholder(player, parts);
            default -> getClassName(player);
        };
    }

    // ==================== BRANCH PLACEHOLDERS ====================

    private String getBranchPlaceholder(Player player, String[] parts) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "-";

        if (parts.length < 3) {
            return getBranchName(data);
        }

        return switch (parts[2]) {
            case "name" -> getBranchName(data);
            case "id" -> data.getSelectedBranchId() != null ? data.getSelectedBranchId() : "none";
            case "formatted" -> getBranchFormatted(data);
            case "color" -> getBranchColor(data);
            case "has" -> data.hasBranch() ? "true" : "false";
            case "cooldown" -> {
                if (data.isOnBranchChangeCooldown()) {
                    long remaining = data.getBranchChangeCooldownRemaining() / 1000;
                    yield formatTime(remaining);
                }
                yield "0";
            }
            default -> getBranchName(data);
        };
    }

    private String getBranchName(ClassData data) {
        if (!data.hasBranch()) return "Aucune";
        TalentBranch branch = TalentBranch.fromId(data.getSelectedBranchId());
        return branch != null ? branch.getDisplayName() : data.getSelectedBranchId();
    }

    private String getBranchFormatted(ClassData data) {
        if (!data.hasBranch()) return "&7Aucune";
        TalentBranch branch = TalentBranch.fromId(data.getSelectedBranchId());
        return branch != null ? branch.getColoredName() : "&7" + data.getSelectedBranchId();
    }

    private String getBranchColor(ClassData data) {
        if (!data.hasBranch()) return "&7";
        TalentBranch branch = TalentBranch.fromId(data.getSelectedBranchId());
        return branch != null ? branch.getColor() : "&7";
    }

    // ==================== SHADOW PLACEHOLDERS ====================

    private String getShadowPlaceholder(Player player, String[] parts) {
        ShadowManager shadowManager = plugin.getShadowManager();
        if (shadowManager == null) return "0";

        if (parts.length < 3) {
            return String.valueOf(shadowManager.getShadowPoints(player.getUniqueId()));
        }

        return switch (parts[2]) {
            case "points" -> String.valueOf(shadowManager.getShadowPoints(player.getUniqueId()));
            case "points_max" -> String.valueOf(ShadowManager.MAX_SHADOW_POINTS);
            case "points_bar" -> getShadowPointsBar(shadowManager, player);
            case "avatar" -> {
                if (parts.length > 3 && parts[3].equals("active")) {
                    yield shadowManager.isAvatarActive(player.getUniqueId()) ? "&5&lACTIF" : "&7Inactif";
                }
                yield shadowManager.isAvatarActive(player.getUniqueId()) ? "true" : "false";
            }
            case "avatar_time" -> {
                if (shadowManager.isAvatarActive(player.getUniqueId())) {
                    // Avatar dure 15s de base
                    yield "Actif";
                }
                yield "0s";
            }
            case "blades", "lames" -> {
                if (parts.length > 3 && parts[3].equals("time")) {
                    long remaining = shadowManager.getBladesRemainingTime(player.getUniqueId());
                    yield (remaining / 1000) + "s";
                }
                yield shadowManager.hasActiveBlades(player.getUniqueId()) ? "&5&lACTIF" : "&7Inactif";
            }
            case "danse", "danse_macabre" -> shadowManager.isDanseMacabreActive(player.getUniqueId()) ? "&5&lACTIF" : "&7Inactif";
            case "execution_cost" -> {
                int cost = shadowManager.getPreparedExecutionCost(player.getUniqueId());
                yield cost > 0 ? String.valueOf(cost) : "5";
            }
            case "is_shadow" -> shadowManager.isShadowPlayer(player) ? "true" : "false";
            default -> String.valueOf(shadowManager.getShadowPoints(player.getUniqueId()));
        };
    }

    private String getShadowPointsBar(ShadowManager manager, Player player) {
        int points = manager.getShadowPoints(player.getUniqueId());
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < ShadowManager.MAX_SHADOW_POINTS; i++) {
            if (i < points) {
                bar.append("&5◆");
            } else {
                bar.append("&8◇");
            }
        }
        return bar.toString();
    }

    // ==================== FROST/GIVRE PLACEHOLDERS ====================

    private String getFrostPlaceholder(Player player, String[] parts) {
        PerforationManager perfoManager = plugin.getPerforationManager();
        if (perfoManager == null) return "0";

        if (parts.length < 3) {
            return String.valueOf(perfoManager.getFrostCharge(player.getUniqueId()));
        }

        return switch (parts[2]) {
            case "charge" -> String.valueOf(perfoManager.getFrostCharge(player.getUniqueId()));
            case "charge_max" -> "5";
            case "charge_bar" -> getFrostChargeBar(perfoManager, player);
            case "glacial" -> perfoManager.isGlacialShot(player.getUniqueId()) ? "&b&lPRÊT" : "&7Non";
            case "hypothermia", "hypo" -> {
                double hypo = perfoManager.getHypothermiaLevel(player.getUniqueId());
                yield decimalFormat.format(hypo * 100) + "%";
            }
            case "hypothermia_bar", "hypo_bar" -> getHypothermiaBar(perfoManager, player);
            case "hypothermia_max", "hypo_max" -> perfoManager.isHypothermiaMax(player.getUniqueId()) ? "&b&lMAX" : "&7Non";
            case "blizzard", "tempete" -> perfoManager.isBlizzardActive(player.getUniqueId()) ? "&9&lACTIF" : "&7Inactif";
            case "eternal", "hiver" -> perfoManager.isEternalWinterActive(player.getUniqueId()) ? "&b&lACTIF" : "&7Inactif";
            case "is_frost" -> perfoManager.isPerforationPlayer(player) ? "true" : "false";
            default -> String.valueOf(perfoManager.getFrostCharge(player.getUniqueId()));
        };
    }

    private String getFrostChargeBar(PerforationManager manager, Player player) {
        int charge = manager.getFrostCharge(player.getUniqueId());
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < charge) {
                bar.append("&b❄");
            } else {
                bar.append("&8○");
            }
        }
        return bar.toString();
    }

    private String getHypothermiaBar(PerforationManager manager, Player player) {
        double hypo = manager.getHypothermiaLevel(player.getUniqueId());
        int filled = (int) (hypo * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("&b█");
            } else {
                bar.append("&8░");
            }
        }
        return bar.toString();
    }

    // ==================== BEAST PLACEHOLDERS ====================

    private String getBeastPlaceholder(Player player, String[] parts) {
        BeastManager beastManager = plugin.getBeastManager();
        if (beastManager == null) return "0";

        if (parts.length < 3) {
            return String.valueOf(beastManager.getPlayerBeasts(player).size());
        }

        return switch (parts[2]) {
            case "count" -> String.valueOf(beastManager.getPlayerBeasts(player).size());
            case "max" -> "4"; // Max configurable via talents
            case "list" -> getBeastList(beastManager, player);
            case "wolf", "loup" -> beastManager.hasBeast(player, BeastType.WOLF) ? "&a&lACTIF" : "&7-";
            case "fox", "renard" -> beastManager.hasBeast(player, BeastType.FOX) ? "&6&lACTIF" : "&7-";
            case "bee", "abeille" -> beastManager.hasBeast(player, BeastType.BEE) ? "&e&lACTIF" : "&7-";
            case "axolotl" -> beastManager.hasBeast(player, BeastType.AXOLOTL) ? "&d&lACTIF" : "&7-";
            case "is_beast" -> isBeastPlayer(player) ? "true" : "false";
            case "focus" -> hasBeastFocusTarget(beastManager, player) ? "&c&lFOCUS" : "&7Aucun";
            default -> String.valueOf(beastManager.getPlayerBeasts(player).size());
        };
    }

    private boolean isBeastPlayer(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return false;
        if (data.getSelectedClass() != ClassType.CHASSEUR) return false;
        String branch = data.getSelectedBranchId();
        return branch != null && branch.toLowerCase().contains("bete");
    }

    private boolean hasBeastFocusTarget(BeastManager manager, Player player) {
        // Vérifier si le joueur a des bêtes actives et un ennemi ciblé
        return !manager.getPlayerBeasts(player).isEmpty();
    }

    private String getBeastList(BeastManager manager, Player player) {
        StringBuilder sb = new StringBuilder();
        for (BeastType type : BeastType.values()) {
            if (manager.hasBeast(player, type)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(type.getColoredName());
            }
        }
        return sb.length() > 0 ? sb.toString() : "&7Aucune";
    }

    // ==================== POISON PLACEHOLDERS ====================

    private String getPoisonPlaceholder(Player player, String[] parts) {
        PoisonManager poisonManager = plugin.getPoisonManager();
        if (poisonManager == null) return "0";

        if (parts.length < 3) {
            return String.valueOf(poisonManager.getVirulence(player.getUniqueId()));
        }

        return switch (parts[2]) {
            case "virulence" -> String.valueOf(poisonManager.getVirulence(player.getUniqueId()));
            case "virulence_bar" -> getVirulenceBar(poisonManager, player);
            case "virulence_percent" -> poisonManager.getVirulence(player.getUniqueId()) + "%";
            case "avatar", "plague" -> poisonManager.isPlagueAvatarActive(player.getUniqueId()) ? "&2&lACTIF" : "&7Inactif";
            case "targets" -> countPoisonedTargets(poisonManager, player);
            case "is_poison" -> poisonManager.isPoisonPlayer(player) ? "true" : "false";
            default -> String.valueOf(poisonManager.getVirulence(player.getUniqueId()));
        };
    }

    private String countPoisonedTargets(PoisonManager manager, Player player) {
        // Compter les entités empoisonnées par ce joueur dans un rayon de 20 blocs
        int count = 0;
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof LivingEntity living) {
                if (manager.isPoisoned(living.getUniqueId()) &&
                    player.getUniqueId().equals(manager.getPoisonOwner(living.getUniqueId()))) {
                    count++;
                }
            }
        }
        return String.valueOf(count);
    }

    private String getVirulenceBar(PoisonManager manager, Player player) {
        int virulence = manager.getVirulence(player.getUniqueId());
        int filled = virulence / 10;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("&2█");
            } else {
                bar.append("&8░");
            }
        }
        return bar.toString();
    }

    private ClassData getClassData(Player player) {
        if (plugin.getClassManager() == null) return null;
        return plugin.getClassManager().getClassData(player);
    }

    private String getClassName(Player player) {
        ClassData data = getClassData(player);
        if (data == null || !data.hasClass()) return "Aucune";
        return data.getSelectedClass().getDisplayName();
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
            if (data.getSelectedTalentId(tier) != null) {
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
        String talentId = data.getSelectedTalentId(tier);
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
