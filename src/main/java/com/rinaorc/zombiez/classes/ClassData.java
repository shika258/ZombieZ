package com.rinaorc.zombiez.classes;

import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentTier;
import lombok.Data;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données de classe d'un joueur
 * Stocke la progression de classe et les talents sélectionnés
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

    // Talents sélectionnés (TalentTier -> Talent ID)
    private final Map<TalentTier, String> selectedTalents = new ConcurrentHashMap<>();

    // Cooldown de changement de talent par tier (TalentTier -> timestamp)
    private final Map<TalentTier, Long> talentChangeCooldowns = new ConcurrentHashMap<>();

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

    // Préférences utilisateur
    private final AtomicBoolean talentMessagesEnabled = new AtomicBoolean(true);

    // Cooldown de changement de talent (1 heure)
    public static final long TALENT_CHANGE_COOLDOWN_MS = 60 * 60 * 1000L;

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

    // ==================== TALENTS ====================

    /**
     * Sélectionne un talent pour un palier
     * @return true si la sélection a réussi
     */
    public boolean selectTalent(TalentTier tier, String talentId) {
        if (!isTalentTierUnlocked(tier)) return false;
        if (isOnTalentChangeCooldown(tier)) return false;

        String current = selectedTalents.get(tier);
        if (current != null && !current.equals(talentId)) {
            // Changement de talent = cooldown
            talentChangeCooldowns.put(tier, System.currentTimeMillis());
        }

        selectedTalents.put(tier, talentId);
        markDirty();
        return true;
    }

    /**
     * Obtient le talent sélectionné pour un palier
     */
    public String getSelectedTalentId(TalentTier tier) {
        return selectedTalents.get(tier);
    }

    /**
     * Vérifie si un palier de talent est débloqué
     */
    public boolean isTalentTierUnlocked(TalentTier tier) {
        return classLevel.get() >= tier.getRequiredLevel();
    }

    /**
     * Vérifie si le joueur a un talent spécifique actif
     */
    public boolean hasTalent(String talentId) {
        return selectedTalents.containsValue(talentId);
    }

    /**
     * Vérifie si le joueur est en cooldown pour changer un talent
     */
    public boolean isOnTalentChangeCooldown(TalentTier tier) {
        Long lastChange = talentChangeCooldowns.get(tier);
        if (lastChange == null) return false;
        return System.currentTimeMillis() - lastChange < TALENT_CHANGE_COOLDOWN_MS;
    }

    /**
     * Obtient le temps restant avant de pouvoir changer un talent (en ms)
     */
    public long getTalentChangeCooldownRemaining(TalentTier tier) {
        Long lastChange = talentChangeCooldowns.get(tier);
        if (lastChange == null) return 0;
        long remaining = TALENT_CHANGE_COOLDOWN_MS - (System.currentTimeMillis() - lastChange);
        return Math.max(0, remaining);
    }

    /**
     * Obtient tous les talents sélectionnés
     */
    public Map<TalentTier, String> getAllSelectedTalents() {
        return Collections.unmodifiableMap(selectedTalents);
    }

    /**
     * Reset tous les talents (lors d'un changement de classe)
     */
    public void resetTalents() {
        selectedTalents.clear();
        talentChangeCooldowns.clear();
        markDirty();
    }

    /**
     * Reset tous les cooldowns de changement de talents
     */
    public void resetTalentCooldowns() {
        talentChangeCooldowns.clear();
        markDirty();
    }

    /**
     * Reset le cooldown de changement de classe
     */
    public void resetClassChangeCooldown() {
        lastClassChange = 0;
        markDirty();
    }

    /**
     * Reset tous les cooldowns (classe + talents)
     */
    public void resetAllCooldowns() {
        resetClassChangeCooldown();
        resetTalentCooldowns();
    }

    /**
     * Compte le nombre de talents sélectionnés
     */
    public int getSelectedTalentCount() {
        return selectedTalents.size();
    }

    /**
     * Obtient le nombre de paliers débloqués
     */
    public int getUnlockedTierCount() {
        int count = 0;
        for (TalentTier tier : TalentTier.values()) {
            if (isTalentTierUnlocked(tier)) count++;
        }
        return count;
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

    // ==================== PRÉFÉRENCES ====================

    /**
     * Vérifie si les messages de talents sont activés
     */
    public boolean isTalentMessagesEnabled() {
        return talentMessagesEnabled.get();
    }

    /**
     * Active ou désactive les messages de talents
     */
    public void setTalentMessagesEnabled(boolean enabled) {
        talentMessagesEnabled.set(enabled);
        markDirty();
    }

    /**
     * Inverse l'état des messages de talents
     * @return le nouvel état
     */
    public boolean toggleTalentMessages() {
        boolean newState = !talentMessagesEnabled.get();
        talentMessagesEnabled.set(newState);
        markDirty();
        return newState;
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
        // Copier les talents
        this.selectedTalents.clear();
        this.selectedTalents.putAll(other.selectedTalents);
        this.talentChangeCooldowns.clear();
        this.talentChangeCooldowns.putAll(other.talentChangeCooldowns);
        // Copier les préférences
        this.talentMessagesEnabled.set(other.talentMessagesEnabled.get());
    }

    @Override
    public String toString() {
        return String.format("ClassData{uuid=%s, class=%s, level=%d}",
            playerUuid, selectedClass != null ? selectedClass.name() : "NONE", classLevel.get());
    }
}
