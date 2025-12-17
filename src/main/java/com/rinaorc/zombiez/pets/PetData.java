package com.rinaorc.zombiez.pets;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données d'un Pet possédé par un joueur
 * Structure thread-safe pour accès concurrent
 */
@Data
public class PetData {

    // Type de pet
    @Getter
    private final PetType type;

    // Niveau actuel (1-9)
    private final AtomicInteger level = new AtomicInteger(1);

    // Copies accumulées
    private final AtomicInteger copies = new AtomicInteger(1);

    // Star Power débloqué (0-3)
    private final AtomicInteger starPower = new AtomicInteger(0);

    // Favori
    @Setter
    private volatile boolean favorite = false;

    // Statistiques
    private final AtomicLong totalDamageDealt = new AtomicLong(0);
    private final AtomicLong totalKills = new AtomicLong(0);
    private final AtomicInteger timesUsed = new AtomicInteger(0);
    private final AtomicLong timeEquipped = new AtomicLong(0);

    // Timestamps
    @Setter
    private long obtainedAt;
    @Setter
    private long lastEquippedAt;

    // Flag dirty
    private final transient AtomicBoolean dirty = new AtomicBoolean(false);

    public PetData(PetType type) {
        this.type = type;
        this.obtainedAt = System.currentTimeMillis();
    }

    /**
     * Constructeur pour chargement depuis BDD
     */
    public PetData(PetType type, int level, int copies, int starPower) {
        this.type = type;
        this.level.set(level);
        this.copies.set(copies);
        this.starPower.set(starPower);
    }

    // ==================== NIVEAU & COPIES ====================

    /**
     * Ajoute des copies et vérifie le level up
     * @return true si level up
     */
    public boolean addCopies(int amount) {
        int newCopies = copies.addAndGet(amount);
        markDirty();

        int currentLevel = level.get();
        if (currentLevel >= type.getRarity().getMaxLevel()) {
            // Déjà niveau max, check Star Power
            return checkStarPowerUnlock();
        }

        int requiredForNext = type.getRarity().getTotalCopiesForLevel(currentLevel + 1);
        if (newCopies >= requiredForNext && currentLevel < type.getRarity().getMaxLevel()) {
            level.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Vérifie si un Star Power peut être débloqué
     */
    private boolean checkStarPowerUnlock() {
        int currentSP = starPower.get();
        if (currentSP >= 3) return false;

        int copiesNeeded = type.getRarity().getCopiesForMax() +
                          type.getRarity().getCopiesForStarPower(currentSP + 1);

        if (copies.get() >= copiesNeeded) {
            starPower.incrementAndGet();
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Obtient les copies restantes pour le prochain niveau
     */
    public int getCopiesForNextLevel() {
        int currentLevel = level.get();
        if (currentLevel >= type.getRarity().getMaxLevel()) {
            // Check Star Power
            int currentSP = starPower.get();
            if (currentSP >= 3) return 0;

            int copiesNeeded = type.getRarity().getCopiesForMax() +
                              type.getRarity().getCopiesForStarPower(currentSP + 1);
            return Math.max(0, copiesNeeded - copies.get());
        }

        int requiredTotal = type.getRarity().getTotalCopiesForLevel(currentLevel + 1);
        return Math.max(0, requiredTotal - copies.get());
    }

    /**
     * Obtient le pourcentage de progression vers le prochain niveau
     */
    public double getProgressPercent() {
        int currentLevel = level.get();
        if (currentLevel >= type.getRarity().getMaxLevel()) {
            int currentSP = starPower.get();
            if (currentSP >= 3) return 100.0;

            int spCopiesNeeded = type.getRarity().getCopiesForStarPower(currentSP + 1);
            int copiesAfterMax = copies.get() - type.getRarity().getCopiesForMax();
            int copiesForPrevSP = currentSP > 0 ?
                type.getRarity().getCopiesForStarPower(currentSP) : 0;

            int progress = copiesAfterMax - copiesForPrevSP;
            int needed = type.getRarity().getCopiesForStarPower(currentSP + 1) - copiesForPrevSP;

            return Math.min(100.0, (progress * 100.0) / needed);
        }

        int prevTotal = currentLevel > 1 ?
            type.getRarity().getTotalCopiesForLevel(currentLevel) : 0;
        int nextTotal = type.getRarity().getTotalCopiesForLevel(currentLevel + 1);
        int copiesInLevel = copies.get() - prevTotal;
        int neededInLevel = nextTotal - prevTotal;

        return Math.min(100.0, (copiesInLevel * 100.0) / neededInLevel);
    }

    // ==================== BONUS DE NIVEAU ====================

    /**
     * Obtient le multiplicateur de stats basé sur le niveau
     * Chaque niveau = +10%
     */
    public double getStatMultiplier() {
        return 1.0 + ((level.get() - 1) * 0.10);
    }

    /**
     * Vérifie si le bonus niveau 5 est débloqué
     */
    public boolean hasLevel5Bonus() {
        return level.get() >= 5;
    }

    /**
     * Vérifie si l'évolution (skin) niveau 9 est débloquée
     */
    public boolean hasEvolution() {
        return level.get() >= 9;
    }

    /**
     * Vérifie si un Star Power spécifique est débloqué
     */
    public boolean hasStarPower(int sp) {
        return starPower.get() >= sp;
    }

    // ==================== STATISTIQUES ====================

    /**
     * Ajoute des dégâts au compteur
     */
    public void addDamage(long amount) {
        totalDamageDealt.addAndGet(amount);
        markDirty();
    }

    /**
     * Incrémente le compteur de kills
     */
    public void addKill() {
        totalKills.incrementAndGet();
        markDirty();
    }

    /**
     * Incrémente le compteur d'utilisation
     */
    public void incrementUsage() {
        timesUsed.incrementAndGet();
        lastEquippedAt = System.currentTimeMillis();
        markDirty();
    }

    /**
     * Ajoute du temps équipé
     */
    public void addEquippedTime(long millis) {
        timeEquipped.addAndGet(millis);
        markDirty();
    }

    // ==================== GETTERS ATOMIQUES ====================

    public int getLevel() {
        return level.get();
    }

    public int getCopies() {
        return copies.get();
    }

    public int getStarPower() {
        return starPower.get();
    }

    public long getTotalDamageDealt() {
        return totalDamageDealt.get();
    }

    public long getTotalKills() {
        return totalKills.get();
    }

    public int getTimesUsed() {
        return timesUsed.get();
    }

    public long getTimeEquipped() {
        return timeEquipped.get();
    }

    // ==================== SETTERS (pour chargement BDD) ====================

    public void setLevel(int lvl) {
        level.set(lvl);
        markDirty();
    }

    public void setCopies(int c) {
        copies.set(c);
        markDirty();
    }

    public void setStarPower(int sp) {
        starPower.set(sp);
        markDirty();
    }

    public void setTotalDamageDealt(long dmg) {
        totalDamageDealt.set(dmg);
    }

    public void setTotalKills(long kills) {
        totalKills.set(kills);
    }

    public void setTimesUsed(int times) {
        timesUsed.set(times);
    }

    public void setTimeEquipped(long time) {
        timeEquipped.set(time);
    }

    // ==================== DIRTY FLAG ====================

    public void markDirty() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void clearDirty() {
        dirty.set(false);
    }

    // ==================== AFFICHAGE ====================

    /**
     * Obtient le nom avec niveau et étoiles
     */
    public String getDisplayName() {
        String stars = "";
        int sp = starPower.get();
        if (sp > 0) {
            stars = " §e" + "★".repeat(sp);
        }
        return type.getColoredName() + " §7[Lv." + level.get() + "]" + stars;
    }

    /**
     * Obtient la barre de progression visuelle
     */
    public String getProgressBar() {
        double percent = getProgressPercent();
        int filled = (int) (percent / 10);
        int empty = 10 - filled;
        return "§a" + "█".repeat(filled) + "§7" + "░".repeat(empty);
    }
}
