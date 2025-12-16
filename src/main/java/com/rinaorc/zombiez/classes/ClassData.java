package com.rinaorc.zombiez.classes;

import lombok.Data;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données de classe d'un joueur
 * Stocke la progression de classe simplifiée
 */
@Data
public class ClassData {

    // Identification
    private final UUID playerUuid;

    // Classe actuelle
    @Getter
    private ClassType selectedClass;

    // Niveau de classe (séparé du niveau global)
    private final AtomicInteger classLevel = new AtomicInteger(1);
    private final AtomicLong classXp = new AtomicLong(0);

    // État de modification
    private final transient AtomicBoolean dirty = new AtomicBoolean(false);

    // Timestamps
    private long lastClassChange = 0;
    private long totalPlaytimeAsClass = 0;

    // Statistiques de classe
    private final AtomicLong classKills = new AtomicLong(0);
    private final AtomicLong classDeaths = new AtomicLong(0);
    private final AtomicLong damageDealt = new AtomicLong(0);
    private final AtomicLong damageReceived = new AtomicLong(0);

    public ClassData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.selectedClass = null; // Pas de classe par défaut
    }

    // ==================== PROGRESSION ====================

    /**
     * Ajoute de l'XP de classe et vérifie le level up
     * @return true si level up
     */
    public boolean addClassXp(long amount) {
        if (selectedClass == null) return false;

        long newXp = classXp.addAndGet(amount);
        markDirty();

        long required = getRequiredXpForNextClassLevel();
        if (newXp >= required) {
            classXp.set(newXp - required);
            classLevel.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Calcule l'XP requis pour le prochain niveau de classe
     */
    public long getRequiredXpForNextClassLevel() {
        int lvl = classLevel.get();
        // Progression plus rapide que le niveau global
        if (lvl <= 10) return 500L * lvl;
        if (lvl <= 25) return 1000L * lvl;
        if (lvl <= 50) return 2000L * lvl;
        return 5000L * lvl;
    }

    /**
     * Pourcentage vers le prochain niveau
     */
    public double getClassLevelProgress() {
        return (double) classXp.get() / getRequiredXpForNextClassLevel() * 100;
    }

    // ==================== CHANGEMENT DE CLASSE ====================

    /**
     * Change la classe du joueur
     */
    public void changeClass(ClassType newClass) {
        if (selectedClass == newClass) return;

        // Sauvegarder le temps joué avec l'ancienne classe
        if (selectedClass != null && lastClassChange > 0) {
            totalPlaytimeAsClass += System.currentTimeMillis() - lastClassChange;
        }

        this.selectedClass = newClass;
        this.lastClassChange = System.currentTimeMillis();
        markDirty();
    }

    // ==================== STATISTIQUES ====================

    public void addClassKill() {
        classKills.incrementAndGet();
        markDirty();
    }

    public void addClassDeath() {
        classDeaths.incrementAndGet();
        markDirty();
    }

    public void addDamageDealt(long amount) {
        damageDealt.addAndGet(amount);
    }

    public void addDamageReceived(long amount) {
        damageReceived.addAndGet(amount);
    }

    public double getClassKDRatio() {
        long deaths = classDeaths.get();
        if (deaths == 0) return classKills.get();
        return (double) classKills.get() / deaths;
    }

    /**
     * Reset les statistiques de session (kills, deaths, damage)
     */
    public void resetSessionStats() {
        classKills.set(0);
        classDeaths.set(0);
        damageDealt.set(0);
        damageReceived.set(0);
        markDirty();
    }

    // ==================== CACHE & DIRTY ====================

    public void markDirty() {
        dirty.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void clearDirty() {
        dirty.set(false);
    }

    /**
     * Vérifie si le joueur a une classe sélectionnée
     */
    public boolean hasClass() {
        return selectedClass != null;
    }

    /**
     * Obtient l'ID de la classe pour la base de données
     */
    public String getClassId() {
        return selectedClass != null ? selectedClass.getId() : null;
    }

    /**
     * Copie les données depuis un autre ClassData
     */
    public void copyFrom(ClassData other) {
        this.selectedClass = other.selectedClass;
        this.classLevel.set(other.classLevel.get());
        this.classXp.set(other.classXp.get());
        this.lastClassChange = other.lastClassChange;
        this.totalPlaytimeAsClass = other.totalPlaytimeAsClass;
        this.classKills.set(other.classKills.get());
        this.classDeaths.set(other.classDeaths.get());
        this.damageDealt.set(other.damageDealt.get());
        this.damageReceived.set(other.damageReceived.get());
    }

    @Override
    public String toString() {
        return String.format("ClassData{uuid=%s, class=%s, level=%d}",
            playerUuid, selectedClass != null ? selectedClass.name() : "NONE", classLevel.get());
    }
}
