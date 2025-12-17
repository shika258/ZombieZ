package com.rinaorc.zombiez.data;

import com.rinaorc.zombiez.zones.Zone;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données d'un joueur - Structure optimisée pour accès concurrent
 * Utilise des types atomiques pour la thread-safety
 */
@Data
public class PlayerData {

    // Identification
    private final UUID uuid;
    @Setter private String name;

    // Progression (Atomiques pour thread-safety)
    private final AtomicInteger level = new AtomicInteger(1);
    private final AtomicLong xp = new AtomicLong(0);
    private final AtomicInteger prestige = new AtomicInteger(0);

    // Économie
    private final AtomicLong points = new AtomicLong(0);
    private final AtomicInteger gems = new AtomicInteger(0);
    
    // Banque
    private final AtomicLong bankPoints = new AtomicLong(0);
    private final AtomicInteger vaultSize = new AtomicInteger(27); // Taille du coffre

    // Statistiques
    private final AtomicLong kills = new AtomicLong(0);
    private final AtomicLong deaths = new AtomicLong(0);
    private final AtomicLong playtime = new AtomicLong(0); // En secondes

    // Zone
    private final AtomicInteger currentZone = new AtomicInteger(1);
    private final AtomicInteger maxZone = new AtomicInteger(1);
    private final AtomicInteger currentCheckpoint = new AtomicInteger(0);

    // VIP
    @Setter private String vipRank = "FREE";
    @Setter private Instant vipExpiry = null;

    // Timestamps
    @Setter private Instant firstJoin;
    @Setter private Instant lastLogin;
    @Setter private Instant lastLogout;

    // Session actuelle
    @Getter private transient Instant sessionStart;
    private final transient AtomicLong sessionKills = new AtomicLong(0);
    private final transient AtomicLong sessionDeaths = new AtomicLong(0);
    private final transient AtomicInteger killStreak = new AtomicInteger(0);
    private final transient AtomicInteger bestKillStreak = new AtomicInteger(0);

    // État de modification (pour savoir si on doit sauvegarder)
    private final transient AtomicBoolean dirty = new AtomicBoolean(false);

    // Cache des stats détaillées (lazy loaded)
    private transient Map<String, Long> detailedStats;

    // Cache de la zone actuelle
    @Setter private transient Zone cachedZone;
    
    // Titres et cosmétiques débloqués
    private final Set<String> unlockedTitles = ConcurrentHashMap.newKeySet();
    private final Set<String> unlockedCosmetics = ConcurrentHashMap.newKeySet();
    private final Set<String> exclusiveItems = ConcurrentHashMap.newKeySet();
    
    @Setter private String activeTitle = "";
    @Setter private String activeCosmetic = "";
    
    // Daily rewards tracking
    @Setter private int dailyStreak = 0;
    @Setter private Instant lastDailyReward = null;

    /**
     * Constructeur pour nouveau joueur
     */
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = Instant.now();
        this.lastLogin = Instant.now();
        this.sessionStart = Instant.now();
    }

    /**
     * Constructeur pour chargement depuis BDD
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.sessionStart = Instant.now();
    }

    // ==================== PROGRESSION ====================

    /**
     * Ajoute de l'XP et vérifie le level up
     * @return true si level up
     */
    public boolean addXp(long amount) {
        long newXp = xp.addAndGet(amount);
        markDirty();
        
        // Check level up
        long requiredXp = getRequiredXpForNextLevel();
        if (newXp >= requiredXp) {
            xp.set(newXp - requiredXp);
            level.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Calcule l'XP requis pour le prochain niveau
     */
    public long getRequiredXpForNextLevel() {
        int lvl = level.get();
        if (lvl <= 10) return 1000L * lvl;
        if (lvl <= 25) return 2500L * lvl;
        if (lvl <= 50) return 5000L * lvl;
        if (lvl <= 75) return 10000L * lvl;
        return 25000L * lvl;
    }

    /**
     * Obtient le pourcentage de progression vers le prochain niveau
     */
    public double getLevelProgress() {
        return (double) xp.get() / getRequiredXpForNextLevel() * 100;
    }

    // ==================== ÉCONOMIE ====================

    /**
     * Ajoute des points
     */
    public void addPoints(long amount) {
        points.addAndGet(amount);
        markDirty();
    }

    /**
     * Retire des points (thread-safe avec CAS)
     * @return true si succès (assez de points)
     */
    public boolean removePoints(long amount) {
        long current, next;
        do {
            current = points.get();
            if (current < amount) return false;
            next = current - amount;
        } while (!points.compareAndSet(current, next));
        markDirty();
        return true;
    }

    /**
     * Vérifie si le joueur a assez de points
     */
    public boolean hasPoints(long amount) {
        return points.get() >= amount;
    }

    /**
     * Ajoute des gems
     */
    public void addGems(int amount) {
        gems.addAndGet(amount);
        markDirty();
    }

    /**
     * Retire des gems (thread-safe avec CAS)
     * @return true si succès
     */
    public boolean removeGems(int amount) {
        int current, next;
        do {
            current = gems.get();
            if (current < amount) return false;
            next = current - amount;
        } while (!gems.compareAndSet(current, next));
        markDirty();
        return true;
    }

    // ==================== BANQUE ====================

    /**
     * Obtient les points en banque
     */
    public int getBankPoints() {
        return (int) bankPoints.get();
    }

    /**
     * Ajoute des points en banque
     */
    public void addBankPoints(int amount) {
        bankPoints.addAndGet(amount);
        markDirty();
    }

    /**
     * Retire des points de la banque (thread-safe avec CAS)
     */
    public boolean removeBankPoints(int amount) {
        long current, next;
        do {
            current = bankPoints.get();
            if (current < amount) return false;
            next = current - amount;
        } while (!bankPoints.compareAndSet(current, next));
        markDirty();
        return true;
    }

    /**
     * Obtient la taille du coffre
     */
    public int getVaultSize() {
        return vaultSize.get();
    }

    /**
     * Définit la taille du coffre
     */
    public void setVaultSize(int size) {
        vaultSize.set(size);
        markDirty();
    }

    // ==================== STATISTIQUES ====================

    /**
     * Incrémente les kills
     */
    public void addKill() {
        kills.incrementAndGet();
        sessionKills.incrementAndGet();
        int streak = killStreak.incrementAndGet();
        if (streak > bestKillStreak.get()) {
            bestKillStreak.set(streak);
        }
        markDirty();
    }

    /**
     * Incrémente les kills avec un montant
     */
    public void addKills(long amount) {
        kills.addAndGet(amount);
        sessionKills.addAndGet(amount);
        markDirty();
    }

    /**
     * Enregistre une mort
     */
    public void addDeath() {
        deaths.incrementAndGet();
        sessionDeaths.incrementAndGet();
        killStreak.set(0); // Reset streak
        markDirty();
    }

    /**
     * Obtient le ratio K/D
     */
    public double getKDRatio() {
        long d = deaths.get();
        if (d == 0) return kills.get();
        return (double) kills.get() / d;
    }

    /**
     * Met à jour le temps de jeu depuis le début de la session
     */
    public void updatePlaytime() {
        if (sessionStart != null) {
            long sessionSeconds = Instant.now().getEpochSecond() - sessionStart.getEpochSecond();
            // On ne met à jour que si ça a changé significativement (évite trop d'updates)
            playtime.addAndGet(sessionSeconds);
            sessionStart = Instant.now(); // Reset pour la prochaine update
            markDirty();
        }
    }

    /**
     * Obtient le temps de jeu formaté
     */
    public String getFormattedPlaytime() {
        long seconds = playtime.get();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %dm", hours, minutes);
    }

    // ==================== ZONES ====================

    /**
     * Met à jour la zone actuelle
     * @return true si c'est la première fois que le joueur atteint cette zone (nouvelle zone max)
     */
    public boolean updateZone(int zoneId) {
        int old = currentZone.getAndSet(zoneId);
        boolean isNewZone = zoneId > maxZone.get();
        if (isNewZone) {
            maxZone.set(zoneId);
        }
        if (old != zoneId) {
            markDirty();
        }
        return isNewZone;
    }

    /**
     * Définit le checkpoint actuel
     */
    public void setCheckpoint(int checkpointId) {
        currentCheckpoint.set(checkpointId);
        markDirty();
    }

    // ==================== VIP ====================

    /**
     * Vérifie si le joueur est VIP actif
     */
    public boolean isVip() {
        if (vipRank == null || vipRank.equals("FREE")) return false;
        if (vipExpiry == null) return true; // Permanent
        return Instant.now().isBefore(vipExpiry);
    }

    /**
     * Obtient le multiplicateur XP selon le rang VIP
     */
    public double getXpMultiplier() {
        if (!isVip()) return 1.0;
        return switch (vipRank.toUpperCase()) {
            case "BRONZE" -> 1.10;
            case "ARGENT" -> 1.20;
            case "OR" -> 1.30;
            case "DIAMANT" -> 1.50;
            case "ETERNEL" -> 1.50;
            default -> 1.0;
        };
    }

    /**
     * Obtient le multiplicateur de points selon le rang VIP
     */
    public double getPointsMultiplier() {
        if (!isVip()) return 1.0;
        return switch (vipRank.toUpperCase()) {
            case "BRONZE" -> 1.10;
            case "ARGENT" -> 1.20;
            case "OR" -> 1.30;
            case "DIAMANT" -> 1.50;
            case "ETERNEL" -> 1.50;
            default -> 1.0;
        };
    }

    /**
     * Obtient le bonus de loot luck selon le rang VIP
     */
    public double getLootLuckBonus() {
        if (!isVip()) return 0.0;
        return switch (vipRank.toUpperCase()) {
            case "OR" -> 0.10;
            case "DIAMANT" -> 0.20;
            case "ETERNEL" -> 0.20;
            default -> 0.0;
        };
    }

    // ==================== SESSION ====================

    /**
     * Démarre une nouvelle session
     */
    public void startSession() {
        sessionStart = Instant.now();
        sessionKills.set(0);
        sessionDeaths.set(0);
        killStreak.set(0);
        lastLogin = Instant.now();
        markDirty();
    }

    /**
     * Termine la session actuelle
     */
    public void endSession() {
        updatePlaytime();
        lastLogout = Instant.now();
        markDirty();
    }

    /**
     * Obtient la durée de la session actuelle en secondes
     */
    public long getSessionDuration() {
        if (sessionStart == null) return 0;
        return Instant.now().getEpochSecond() - sessionStart.getEpochSecond();
    }

    // ==================== STATS DÉTAILLÉES ====================

    /**
     * Obtient les stats détaillées (lazy load)
     */
    public Map<String, Long> getDetailedStats() {
        if (detailedStats == null) {
            detailedStats = new HashMap<>();
        }
        return detailedStats;
    }

    /**
     * Définit une stat détaillée
     */
    public void setStat(String key, long value) {
        getDetailedStats().put(key, value);
        markDirty();
    }

    /**
     * Incrémente une stat détaillée
     */
    public void incrementStat(String key) {
        getDetailedStats().merge(key, 1L, Long::sum);
        markDirty();
    }

    /**
     * Obtient une stat détaillée
     */
    public long getStat(String key) {
        return getDetailedStats().getOrDefault(key, 0L);
    }

    // ==================== DIRTY FLAG ====================

    /**
     * Marque les données comme modifiées
     */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Vérifie si les données ont été modifiées
     */
    public boolean isDirty() {
        return dirty.get();
    }

    /**
     * Réinitialise le flag dirty (après sauvegarde)
     */
    public void clearDirty() {
        dirty.set(false);
    }

    // ==================== UTILITAIRES ====================

    /**
     * Vérifie si le joueur est en ligne
     */
    public boolean isOnline() {
        return org.bukkit.Bukkit.getPlayer(uuid) != null;
    }

    /**
     * Obtient le joueur Bukkit associé
     */
    public Player getPlayer() {
        return org.bukkit.Bukkit.getPlayer(uuid);
    }

    /**
     * Copie les valeurs depuis un autre PlayerData (pour merge)
     */
    public void copyFrom(PlayerData other) {
        this.name = other.name;
        this.level.set(other.level.get());
        this.xp.set(other.xp.get());
        this.prestige.set(other.prestige.get());
        this.points.set(other.points.get());
        this.gems.set(other.gems.get());
        this.bankPoints.set(other.bankPoints.get());
        this.vaultSize.set(other.vaultSize.get());
        this.kills.set(other.kills.get());
        this.deaths.set(other.deaths.get());
        this.playtime.set(other.playtime.get());
        this.currentZone.set(other.currentZone.get());
        this.maxZone.set(other.maxZone.get());
        this.currentCheckpoint.set(other.currentCheckpoint.get());
        this.vipRank = other.vipRank;
        this.vipExpiry = other.vipExpiry;
        this.firstJoin = other.firstJoin;
        this.lastLogin = other.lastLogin;
    }

    @Override
    public String toString() {
        return String.format("PlayerData{uuid=%s, name=%s, level=%d, zone=%d}", 
            uuid, name, level.get(), currentZone.get());
    }
    
    // ==================== TITRES & COSMÉTIQUES ====================
    
    /**
     * Ajoute un titre débloqué
     */
    public void addTitle(String titleId) {
        unlockedTitles.add(titleId);
        markDirty();
    }
    
    /**
     * Vérifie si un titre est débloqué
     */
    public boolean hasTitle(String titleId) {
        return unlockedTitles.contains(titleId);
    }
    
    /**
     * Obtient tous les titres débloqués
     */
    public Set<String> getUnlockedTitles() {
        return Set.copyOf(unlockedTitles);
    }
    
    /**
     * Ajoute un cosmétique débloqué
     */
    public void addCosmetic(String cosmeticId) {
        unlockedCosmetics.add(cosmeticId);
        markDirty();
    }
    
    /**
     * Vérifie si un cosmétique est débloqué
     */
    public boolean hasCosmetic(String cosmeticId) {
        return unlockedCosmetics.contains(cosmeticId);
    }
    
    /**
     * Obtient tous les cosmétiques débloqués
     */
    public Set<String> getUnlockedCosmetics() {
        return Set.copyOf(unlockedCosmetics);
    }
    
    /**
     * Ajoute un item exclusif
     */
    public void addExclusive(String exclusiveId) {
        exclusiveItems.add(exclusiveId);
        markDirty();
    }
    
    /**
     * Vérifie si un item exclusif est possédé
     */
    public boolean hasExclusive(String exclusiveId) {
        return exclusiveItems.contains(exclusiveId);
    }
    
    /**
     * Obtient le meilleur kill streak
     */
    public AtomicInteger getBestKillStreak() {
        return bestKillStreak;
    }
    
    /**
     * Obtient le kill streak actuel
     */
    public AtomicInteger getKillStreak() {
        return killStreak;
    }
    
    /**
     * Obtient les kills de session
     */
    public AtomicLong getSessionKills() {
        return sessionKills;
    }
    
    /**
     * Définit le niveau
     */
    public void setLevel(int newLevel) {
        level.set(newLevel);
        markDirty();
    }
    
    /**
     * Définit l'XP
     */
    public void setXp(long newXp) {
        xp.set(newXp);
        markDirty();
    }
    
    /**
     * Définit le prestige
     */
    public void setPrestige(int newPrestige) {
        prestige.set(newPrestige);
        markDirty();
    }
    
    /**
     * Incrémente les kills d'un
     */
    public void incrementKills() {
        kills.incrementAndGet();
        sessionKills.incrementAndGet();
        killStreak.incrementAndGet();
        markDirty();
    }
    
    // ==================== STATS SUPPLÉMENTAIRES ====================
    
    // Fragments pour crafting
    private final AtomicInteger fragments = new AtomicInteger(0);
    
    // Stats détaillées de kills
    private final AtomicLong zombieKills = new AtomicLong(0);
    private final AtomicLong eliteKills = new AtomicLong(0);
    private final AtomicLong bossKills = new AtomicLong(0);
    
    // Stats de progression
    private final AtomicLong totalXp = new AtomicLong(0);
    private final AtomicLong totalPointsEarned = new AtomicLong(0);
    private final AtomicLong distanceTraveled = new AtomicLong(0);
    
    // Skills et achievements
    private final Set<String> unlockedSkills = ConcurrentHashMap.newKeySet();
    private final Set<String> unlockedAchievements = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> achievementProgress = new ConcurrentHashMap<>();
    private final AtomicInteger spentSkillPoints = new AtomicInteger(0);
    
    // Boosters
    @Setter private double xpBoostMultiplier = 1.0;
    @Setter private long xpBoostExpire = 0;
    @Setter private double lootBoostMultiplier = 1.0;
    @Setter private long lootBoostExpire = 0;
    private final Map<String, BoosterData> activeBoosters = new ConcurrentHashMap<>();
    
    public AtomicInteger getFragments() { return fragments; }
    
    public AtomicLong getZombieKills() { return zombieKills; }
    public AtomicLong getEliteKills() { return eliteKills; }
    public AtomicLong getBossKills() { return bossKills; }
    
    public void addZombieKill() { zombieKills.incrementAndGet(); markDirty(); }
    public void addEliteKill() { eliteKills.incrementAndGet(); markDirty(); }
    public void addBossKill() { bossKills.incrementAndGet(); markDirty(); }
    
    public AtomicLong getTotalXp() { return totalXp; }
    public void addTotalXp(long amount) { totalXp.addAndGet(amount); markDirty(); }
    
    public AtomicLong getTotalPointsEarned() { return totalPointsEarned; }
    public void addTotalPointsEarned(long amount) { totalPointsEarned.addAndGet(amount); markDirty(); }
    
    public AtomicLong getDistanceTraveled() { return distanceTraveled; }
    public void addDistanceTraveled(long amount) { distanceTraveled.addAndGet(amount); markDirty(); }
    
    public long getTotalKills() { return kills.get(); }
    public int getMaxZoneReached() { return maxZone.get(); }
    public int getHighestZone() { return maxZone.get(); }
    public void setHighestZone(int zone) { maxZone.set(zone); markDirty(); }
    
    public int getPrestigeLevel() { return prestige.get(); }
    
    // Skills
    public boolean hasSkill(String skillId) { return unlockedSkills.contains(skillId); }
    public void addSkill(String skillId) { unlockedSkills.add(skillId); markDirty(); }
    public Set<String> getUnlockedSkills() { return Set.copyOf(unlockedSkills); }
    public void clearSkills() { unlockedSkills.clear(); markDirty(); }

    public int getSpentSkillPoints() { return spentSkillPoints.get(); }
    public void setSpentSkillPoints(int points) { spentSkillPoints.set(points); markDirty(); }
    public void addSpentSkillPoints(int points) { spentSkillPoints.addAndGet(points); markDirty(); }

    // Bonus skill points permanents (gagnés via prestige)
    private final AtomicInteger bonusSkillPoints = new AtomicInteger(0);

    public int getBonusSkillPoints() { return bonusSkillPoints.get(); }
    public void setBonusSkillPoints(int points) { bonusSkillPoints.set(points); markDirty(); }
    public void addBonusSkillPoints(int points) { bonusSkillPoints.addAndGet(points); markDirty(); }
    
    // Achievements
    public boolean hasAchievement(String achievementId) { return unlockedAchievements.contains(achievementId); }
    public void unlockAchievement(String achievementId) { unlockedAchievements.add(achievementId); markDirty(); }
    public int getAchievementProgress(String achievementId) { return achievementProgress.getOrDefault(achievementId, 0); }
    public void setAchievementProgress(String achievementId, int progress) { achievementProgress.put(achievementId, progress); markDirty(); }
    
    // Cosmetics (alias)
    public Set<String> getCosmetics() { return getUnlockedCosmetics(); }
    public Set<String> getTitles() { return getUnlockedTitles(); }
    
    // Boosters
    public boolean hasActiveBooster() {
        return (xpBoostExpire > System.currentTimeMillis() && xpBoostMultiplier > 1.0) ||
               (lootBoostExpire > System.currentTimeMillis() && lootBoostMultiplier > 1.0) ||
               !activeBoosters.isEmpty();
    }
    
    public boolean hasBooster(String type) {
        if (type.equalsIgnoreCase("xp")) return xpBoostExpire > System.currentTimeMillis();
        if (type.equalsIgnoreCase("loot")) return lootBoostExpire > System.currentTimeMillis();
        return activeBoosters.containsKey(type) && activeBoosters.get(type).expireTime > System.currentTimeMillis();
    }
    
    public long getBoosterRemainingTime(String type) {
        if (type.equalsIgnoreCase("xp")) return Math.max(0, xpBoostExpire - System.currentTimeMillis());
        if (type.equalsIgnoreCase("loot")) return Math.max(0, lootBoostExpire - System.currentTimeMillis());
        BoosterData data = activeBoosters.get(type);
        return data != null ? Math.max(0, data.expireTime - System.currentTimeMillis()) : 0;
    }
    
    public void setBooster(String type, double multiplier, long durationMs) {
        if (type.equalsIgnoreCase("xp")) {
            xpBoostMultiplier = multiplier;
            xpBoostExpire = System.currentTimeMillis() + durationMs;
        } else if (type.equalsIgnoreCase("loot")) {
            lootBoostMultiplier = multiplier;
            lootBoostExpire = System.currentTimeMillis() + durationMs;
        } else {
            activeBoosters.put(type, new BoosterData(multiplier, System.currentTimeMillis() + durationMs));
        }
        markDirty();
    }
    
    public void addBankSlots(int slots) {
        vaultSize.addAndGet(slots);
        markDirty();
    }
    
    // Inner class for booster data
    @Getter
    public static class BoosterData {
        private final double multiplier;
        private final long expireTime;
        
        public BoosterData(double multiplier, long expireTime) {
            this.multiplier = multiplier;
            this.expireTime = expireTime;
        }
    }
}
