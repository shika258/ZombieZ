package com.rinaorc.zombiez.classes;

import lombok.Data;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données de classe d'un joueur
 * Stocke la progression de classe, les talents, les compétences équipées, et les buffs
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

    // Points de talents disponibles (1 par level up de classe)
    private final AtomicInteger talentPoints = new AtomicInteger(0);
    private final AtomicInteger spentTalentPoints = new AtomicInteger(0);

    // Talents débloqués: ID -> niveau actuel
    private final Map<String, Integer> unlockedTalents = new ConcurrentHashMap<>();

    // Compétences équipées (max 3: PRIMARY, SECONDARY, ULTIMATE)
    private final Map<String, String> equippedSkills = new ConcurrentHashMap<>();

    // Buffs arcade sélectionnés: ID -> nombre de stacks
    private final Map<String, Integer> arcadeBuffs = new ConcurrentHashMap<>();

    // Cooldowns des compétences actives (skillId -> timestamp de fin)
    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();

    // Énergie de classe (pour les compétences)
    private final AtomicInteger energy = new AtomicInteger(100);
    private final AtomicInteger maxEnergy = new AtomicInteger(100);

    // Stats dérivées des talents et buffs (cache)
    private transient Map<String, Double> cachedStats;
    private transient boolean statsNeedRefresh = true;

    // État de modification
    private final transient AtomicBoolean dirty = new AtomicBoolean(false);

    // Cache d'archétype (évite les recalculs constants)
    private transient String cachedArchetypeId = null;
    private transient long archetypeCacheTime = 0;
    private static final long ARCHETYPE_CACHE_DURATION_MS = 5000; // 5 secondes

    // Timestamps
    private long lastClassChange = 0;
    private long totalPlaytimeAsClass = 0;

    // Statistiques de classe
    private final AtomicLong classKills = new AtomicLong(0);
    private final AtomicLong classDeaths = new AtomicLong(0);
    private final AtomicLong damageDealt = new AtomicLong(0);
    private final AtomicLong damageReceived = new AtomicLong(0);
    private final AtomicInteger skillsUsed = new AtomicInteger(0);

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
            talentPoints.incrementAndGet();
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

    // ==================== TALENTS ====================

    /**
     * Débloque ou améliore un talent
     * @return true si succès
     */
    public boolean unlockTalent(String talentId, int cost) {
        if (getAvailableTalentPoints() < cost) return false;

        unlockedTalents.merge(talentId, 1, Integer::sum);
        spentTalentPoints.addAndGet(cost);
        statsNeedRefresh = true;
        invalidateArchetypeCache(); // L'archétype peut changer
        markDirty();
        return true;
    }

    /**
     * Points de talents disponibles
     */
    public int getAvailableTalentPoints() {
        return talentPoints.get() + (classLevel.get() - 1) - spentTalentPoints.get();
    }

    /**
     * Niveau d'un talent spécifique
     */
    public int getTalentLevel(String talentId) {
        return unlockedTalents.getOrDefault(talentId, 0);
    }

    /**
     * Vérifie si un talent est débloqué
     */
    public boolean hasTalent(String talentId) {
        return getTalentLevel(talentId) > 0;
    }

    /**
     * Reset les talents (coûte des gemmes)
     */
    public void resetTalents() {
        unlockedTalents.clear();
        spentTalentPoints.set(0);
        statsNeedRefresh = true;
        markDirty();
    }

    // ==================== COMPÉTENCES ====================

    /**
     * Équipe une compétence dans un slot
     */
    public void equipSkill(String slot, String skillId) {
        equippedSkills.put(slot, skillId);
        invalidateArchetypeCache(); // L'archétype peut changer
        markDirty();
    }

    /**
     * Retire une compétence d'un slot
     */
    public void unequipSkill(String slot) {
        equippedSkills.remove(slot);
        invalidateArchetypeCache(); // L'archétype peut changer
        markDirty();
    }

    /**
     * Obtient la compétence équipée dans un slot
     */
    public String getEquippedSkill(String slot) {
        return equippedSkills.get(slot);
    }

    /**
     * Obtient tous les IDs de compétences équipées
     */
    public Set<String> getEquippedSkillIds() {
        return new HashSet<>(equippedSkills.values());
    }

    /**
     * Obtient les IDs de tous les talents débloqués
     */
    public Set<String> getUnlockedTalentIds() {
        return unlockedTalents.keySet();
    }

    /**
     * Vérifie si une compétence est en cooldown
     */
    public boolean isSkillOnCooldown(String skillId) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Obtient le temps restant de cooldown en secondes
     */
    public int getRemainingCooldown(String skillId) {
        Long cooldownEnd = skillCooldowns.get(skillId);
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    /**
     * Met une compétence en cooldown
     */
    public void putSkillOnCooldown(String skillId, int cooldownSeconds) {
        skillCooldowns.put(skillId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    // ==================== ÉNERGIE ====================

    /**
     * Consomme de l'énergie
     * @return true si assez d'énergie
     */
    public boolean consumeEnergy(int amount) {
        int current = energy.get();
        if (current < amount) return false;
        energy.set(current - amount);
        return true;
    }

    /**
     * Régénère de l'énergie
     */
    public void regenerateEnergy(int amount) {
        int max = maxEnergy.get();
        energy.updateAndGet(e -> Math.min(e + amount, max));
    }

    /**
     * Reset l'énergie au maximum
     */
    public void resetEnergy() {
        energy.set(maxEnergy.get());
    }

    // ==================== BUFFS ARCADE ====================

    /**
     * Ajoute un stack de buff
     */
    public void addBuff(String buffId) {
        arcadeBuffs.merge(buffId, 1, Integer::sum);
        statsNeedRefresh = true;
        markDirty();
    }

    /**
     * Obtient le nombre de stacks d'un buff
     */
    public int getBuffStacks(String buffId) {
        return arcadeBuffs.getOrDefault(buffId, 0);
    }

    /**
     * Nombre total de buffs collectés
     */
    public int getTotalBuffCount() {
        return arcadeBuffs.values().stream().mapToInt(Integer::intValue).sum();
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

        // Reset les compétences équipées
        equippedSkills.clear();

        // Ne pas reset les talents et buffs - ils sont spécifiques à la classe
        // mais conservés si le joueur change de classe et revient

        statsNeedRefresh = true;
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

    public void incrementSkillsUsed() {
        skillsUsed.incrementAndGet();
    }

    public double getClassKDRatio() {
        long deaths = classDeaths.get();
        if (deaths == 0) return classKills.get();
        return (double) classKills.get() / deaths;
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

    public void invalidateStatsCache() {
        statsNeedRefresh = true;
    }

    /**
     * Invalide le cache d'archétype (appelé quand talents/skills changent)
     */
    public void invalidateArchetypeCache() {
        cachedArchetypeId = null;
        archetypeCacheTime = 0;
    }

    /**
     * Obtient l'archétype caché, ou null si expiré
     */
    public String getCachedArchetypeId() {
        if (System.currentTimeMillis() - archetypeCacheTime > ARCHETYPE_CACHE_DURATION_MS) {
            return null;
        }
        return cachedArchetypeId;
    }

    /**
     * Met à jour le cache d'archétype
     */
    public void setCachedArchetypeId(String archetypeId) {
        this.cachedArchetypeId = archetypeId;
        this.archetypeCacheTime = System.currentTimeMillis();
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
        this.talentPoints.set(other.talentPoints.get());
        this.spentTalentPoints.set(other.spentTalentPoints.get());
        this.unlockedTalents.clear();
        this.unlockedTalents.putAll(other.unlockedTalents);
        this.equippedSkills.clear();
        this.equippedSkills.putAll(other.equippedSkills);
        this.arcadeBuffs.clear();
        this.arcadeBuffs.putAll(other.arcadeBuffs);
        this.energy.set(other.energy.get());
        this.maxEnergy.set(other.maxEnergy.get());
        this.lastClassChange = other.lastClassChange;
        this.totalPlaytimeAsClass = other.totalPlaytimeAsClass;
        this.classKills.set(other.classKills.get());
        this.classDeaths.set(other.classDeaths.get());
        this.damageDealt.set(other.damageDealt.get());
        this.damageReceived.set(other.damageReceived.get());
        this.skillsUsed.set(other.skillsUsed.get());
        this.statsNeedRefresh = true;
    }

    @Override
    public String toString() {
        return String.format("ClassData{uuid=%s, class=%s, level=%d}",
            playerUuid, selectedClass != null ? selectedClass.name() : "NONE", classLevel.get());
    }
}
