package com.rinaorc.zombiez.ascension;

import com.rinaorc.zombiez.items.types.StatType;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Données d'Ascension d'un joueur
 * Stockage en RAM uniquement - reset au reboot serveur et à la mort
 */
@Getter
public class AscensionData {

    // ==================== DONNÉES PRINCIPALES ====================

    private final UUID playerId;
    private final List<Mutation> activeMutations = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger sessionKills = new AtomicInteger(0);
    private final AtomicLong lastKillTime = new AtomicLong(0);

    // Mutation assurée (conservée après mort si payée)
    private volatile Mutation insuredMutation = null;

    // ==================== CACHE DES STATS (recalculé à chaque mutation) ====================

    private volatile Map<StatType, Double> cachedStatBonuses = new EnumMap<>(StatType.class);
    private volatile int strainCarnageCount = 0;
    private volatile int strainSpectreCount = 0;
    private volatile int strainButinCount = 0;
    private final Object cacheLock = new Object(); // Lock pour thread-safety

    // ==================== DONNÉES D'EFFETS SPÉCIAUX ====================

    // Compteurs pour effets on-kill
    private final AtomicInteger hitCounter = new AtomicInteger(0);           // Pour Frappe Brutale (5ème hit)
    private final AtomicInteger novaKillCounter = new AtomicInteger(0);      // Pour Nova Mortelle (25 kills)
    private final AtomicInteger milestoneKillCounter = new AtomicInteger(0); // Pour Économiste (10 kills)
    private final AtomicInteger guaranteedRareCounter = new AtomicInteger(0);// Pour Favori de la Chance (50 kills)

    // Stacks temporaires (chaque type a son propre timer)
    private final AtomicInteger stackingLifesteal = new AtomicInteger(0);    // Soif Insatiable (max 5 stacks)
    private final AtomicInteger stackingSpeed = new AtomicInteger(0);        // Vélocité (max 10 stacks)
    private final AtomicInteger stackingCrit = new AtomicInteger(0);         // Danse Macabre (max 10 stacks)
    private final AtomicInteger cascadeStacks = new AtomicInteger(0);        // Cascade Sanglante (max 3 stacks)

    // Timers séparés pour chaque type de stack
    private final AtomicLong lastLifestealStackTime = new AtomicLong(0);     // Timer Soif Insatiable (10s)
    private final AtomicLong lastSpeedStackTime = new AtomicLong(0);         // Timer Vélocité (15s)
    private final AtomicLong lastCritStackTime = new AtomicLong(0);          // Timer Danse Macabre (20s)
    private final AtomicLong lastCascadeStackTime = new AtomicLong(0);       // Timer Cascade (5s)

    // Cooldowns des effets (pour éviter le spam)
    private final Map<String, Long> effectCooldowns = new ConcurrentHashMap<>();

    // Tracking pour effets conditionnels
    private final Set<UUID> hitByMobs = ConcurrentHashMap.newKeySet();       // Mobs qui nous ont touché (Traque)
    private final Set<UUID> firstHitMobs = ConcurrentHashMap.newKeySet();    // Mobs déjà frappés (Prédateur)

    // Heal per second tracking (anti-exploit)
    private final AtomicLong lastHealSecond = new AtomicLong(0);
    private volatile double healedThisSecond = 0.0;

    // ==================== STADE ET CHOIX ====================

    private volatile int currentStage = 0;                    // Stade actuel (0 = pas encore de mutation)
    private volatile boolean choicePending = false;           // Un choix est en attente
    private volatile long choiceStartTime = 0;                // Quand le choix a commencé
    private volatile List<Mutation> pendingChoices = null;    // Les 3 mutations proposées

    // ==================== CONSTRUCTEUR ====================

    public AscensionData(UUID playerId) {
        this.playerId = playerId;
    }

    // ==================== GESTION DES KILLS ====================

    /**
     * Enregistre un kill et vérifie les paliers
     * @return Le nouveau stade atteint, ou -1 si pas de nouveau stade
     */
    public int registerKill() {
        int kills = sessionKills.incrementAndGet();
        lastKillTime.set(System.currentTimeMillis());

        // Vérifier les stades
        int newStage = getStageForKills(kills);
        if (newStage > currentStage && !choicePending) {
            return newStage;
        }
        return -1;
    }

    /**
     * Obtient le stade correspondant à un nombre de kills
     */
    public static int getStageForKills(int kills) {
        if (kills >= 1250) return 10;
        if (kills >= 1000) return 9;
        if (kills >= 800) return 8;
        if (kills >= 650) return 7;
        if (kills >= 500) return 6;
        if (kills >= 400) return 5;
        if (kills >= 300) return 4;
        if (kills >= 200) return 3;
        if (kills >= 100) return 2;
        if (kills >= 50) return 1;
        return 0;
    }

    /**
     * Obtient le nombre de kills requis pour le prochain stade
     */
    public int getKillsForNextStage() {
        int nextStage = currentStage + 1;
        return switch (nextStage) {
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 200;
            case 4 -> 300;
            case 5 -> 400;
            case 6 -> 500;
            case 7 -> 650;
            case 8 -> 800;
            case 9 -> 1000;
            case 10 -> 1250;
            default -> Integer.MAX_VALUE;
        };
    }

    // ==================== GESTION DES MUTATIONS ====================

    /**
     * Ajoute une mutation et recalcule le cache
     */
    public void addMutation(Mutation mutation) {
        activeMutations.add(mutation);
        currentStage++;
        choicePending = false;
        pendingChoices = null;
        recalculateCache();
    }

    /**
     * Vérifie si le joueur a une mutation spécifique
     */
    public boolean hasMutation(Mutation mutation) {
        return activeMutations.contains(mutation);
    }

    /**
     * Vérifie si le joueur a un effet spécifique
     */
    public boolean hasEffect(Mutation.MutationEffect effect) {
        return activeMutations.stream().anyMatch(m -> m.getEffect() == effect);
    }

    /**
     * Recalcule le cache des stats (thread-safe)
     */
    public void recalculateCache() {
        Map<StatType, Double> newStats = new EnumMap<>(StatType.class);
        int carnage = 0, spectre = 0, butin = 0;

        // Copier la liste pour éviter ConcurrentModificationException
        List<Mutation> mutationsCopy;
        synchronized (activeMutations) {
            mutationsCopy = new ArrayList<>(activeMutations);
        }

        // Additionner les stats de toutes les mutations
        for (Mutation mutation : mutationsCopy) {
            // Stats directes
            mutation.getStatBonuses().forEach((stat, value) ->
                newStats.merge(stat, value, Double::sum)
            );

            // Compter les souches
            switch (mutation.getStrain()) {
                case CARNAGE -> carnage++;
                case SPECTRE -> spectre++;
                case BUTIN -> butin++;
            }
        }

        // Appliquer les bonus de collection
        if (carnage >= 7) newStats.merge(StatType.DAMAGE_PERCENT, 15.0, Double::sum);
        else if (carnage >= 5) newStats.merge(StatType.DAMAGE_PERCENT, 10.0, Double::sum);
        else if (carnage >= 3) newStats.merge(StatType.DAMAGE_PERCENT, 5.0, Double::sum);

        if (spectre >= 7) newStats.merge(StatType.MOVEMENT_SPEED, 15.0, Double::sum);
        else if (spectre >= 5) newStats.merge(StatType.MOVEMENT_SPEED, 10.0, Double::sum);
        else if (spectre >= 3) newStats.merge(StatType.MOVEMENT_SPEED, 5.0, Double::sum);

        // Bonus LUCK nerfé pour éviter le loot OP
        if (butin >= 7) newStats.merge(StatType.LUCK, 8.0, Double::sum);
        else if (butin >= 5) newStats.merge(StatType.LUCK, 5.0, Double::sum);
        else if (butin >= 3) newStats.merge(StatType.LUCK, 3.0, Double::sum);

        // Bonus équilibriste (au moins 2 de chaque)
        if (carnage >= 2 && spectre >= 2 && butin >= 2) {
            if (carnage >= 3 && spectre >= 3 && butin >= 3) {
                // Parfait 3-3-3 : +8% damage/speed, +4% luck (nerfé)
                newStats.merge(StatType.DAMAGE_PERCENT, 8.0, Double::sum);
                newStats.merge(StatType.MOVEMENT_SPEED, 8.0, Double::sum);
                newStats.merge(StatType.LUCK, 4.0, Double::sum);
            } else {
                // 2-2-2 minimum : +5% damage/speed, +2% luck (nerfé)
                newStats.merge(StatType.DAMAGE_PERCENT, 5.0, Double::sum);
                newStats.merge(StatType.MOVEMENT_SPEED, 5.0, Double::sum);
                newStats.merge(StatType.LUCK, 2.0, Double::sum);
            }
        }

        // Assignation atomique des valeurs
        synchronized (cacheLock) {
            this.cachedStatBonuses = newStats;
            this.strainCarnageCount = carnage;
            this.strainSpectreCount = spectre;
            this.strainButinCount = butin;
        }
    }

    // ==================== GESTION DES STACKS ====================

    /**
     * Met à jour les stacks temporaires (appelé régulièrement)
     * @return true si des stacks ont été reset
     */
    public boolean updateStacks() {
        long now = System.currentTimeMillis();
        boolean reset = false;

        // Soif Insatiable : reset après 10s d'inactivité
        if (stackingLifesteal.get() > 0 && now - lastLifestealStackTime.get() > 10000) {
            stackingLifesteal.set(0);
            reset = true;
        }

        // Vélocité : reset après 15s d'inactivité
        if (stackingSpeed.get() > 0 && now - lastSpeedStackTime.get() > 15000) {
            stackingSpeed.set(0);
            reset = true;
        }

        // Danse Macabre : reset après 20s d'inactivité
        if (stackingCrit.get() > 0 && now - lastCritStackTime.get() > 20000) {
            stackingCrit.set(0);
            reset = true;
        }

        // Cascade Sanglante : reset après 5s d'inactivité
        if (cascadeStacks.get() > 0 && now - lastCascadeStackTime.get() > 5000) {
            cascadeStacks.set(0);
            reset = true;
        }

        // Cleanup des Sets de tracking (évite memory leak)
        // On garde seulement les 50 derniers mobs pour éviter les Sets trop gros
        if (hitByMobs.size() > 50) {
            hitByMobs.clear();
        }
        if (firstHitMobs.size() > 50) {
            firstHitMobs.clear();
        }

        return reset;
    }

    /**
     * Ajoute un stack et met à jour le timer correspondant
     */
    public void addStack(StackType type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case LIFESTEAL -> {
                lastLifestealStackTime.set(now);
                stackingLifesteal.updateAndGet(v -> Math.min(v + 1, 5));
            }
            case SPEED -> {
                lastSpeedStackTime.set(now);
                stackingSpeed.updateAndGet(v -> Math.min(v + 1, 10));
            }
            case CRIT -> {
                lastCritStackTime.set(now);
                stackingCrit.updateAndGet(v -> Math.min(v + 1, 10));
            }
            case CASCADE -> {
                lastCascadeStackTime.set(now);
                cascadeStacks.updateAndGet(v -> Math.min(v + 1, 3));
            }
        }
    }

    /**
     * Consomme les stacks de cascade (après un hit)
     */
    public int consumeCascadeStacks() {
        return cascadeStacks.getAndSet(0);
    }

    public enum StackType {
        LIFESTEAL, SPEED, CRIT, CASCADE
    }

    // ==================== COOLDOWNS ====================

    /**
     * Vérifie si un effet peut être déclenché (cooldown)
     */
    public boolean canTriggerEffect(String effectId, long cooldownMs) {
        long now = System.currentTimeMillis();
        Long lastTrigger = effectCooldowns.get(effectId);
        if (lastTrigger != null && now - lastTrigger < cooldownMs) {
            return false;
        }
        effectCooldowns.put(effectId, now);
        return true;
    }

    // ==================== HEAL TRACKING (anti-exploit) ====================

    /**
     * Enregistre un heal et vérifie le cap par seconde
     * @return Le montant réellement soigné
     */
    public double registerHeal(double amount, double maxHp) {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000;
        long lastSecond = lastHealSecond.get();

        // Nouvelle seconde = reset
        if (currentSecond != lastSecond) {
            lastHealSecond.set(currentSecond);
            healedThisSecond = 0;
        }

        // Cap à 5% max HP par seconde
        double maxHealThisSecond = maxHp * 0.05;
        double canHeal = Math.max(0, maxHealThisSecond - healedThisSecond);
        double actualHeal = Math.min(amount, canHeal);

        healedThisSecond += actualHeal;
        return actualHeal;
    }

    // ==================== RESET ====================

    /**
     * Reset complet (mort ou nouveau)
     */
    public void reset() {
        activeMutations.clear();
        sessionKills.set(0);
        currentStage = 0;
        choicePending = false;
        pendingChoices = null;

        // Reset des compteurs
        hitCounter.set(0);
        novaKillCounter.set(0);
        milestoneKillCounter.set(0);
        guaranteedRareCounter.set(0);

        // Reset des stacks et leurs timers
        stackingLifesteal.set(0);
        stackingSpeed.set(0);
        stackingCrit.set(0);
        cascadeStacks.set(0);
        lastLifestealStackTime.set(0);
        lastSpeedStackTime.set(0);
        lastCritStackTime.set(0);
        lastCascadeStackTime.set(0);

        // Reset des cooldowns et tracking
        effectCooldowns.clear();
        hitByMobs.clear();
        firstHitMobs.clear();
        healedThisSecond = 0;

        // Reset du cache (thread-safe)
        synchronized (cacheLock) {
            cachedStatBonuses = new EnumMap<>(StatType.class);
            strainCarnageCount = 0;
            strainSpectreCount = 0;
            strainButinCount = 0;
        }

        // Si mutation assurée, la restaurer
        if (insuredMutation != null) {
            Mutation saved = insuredMutation;
            insuredMutation = null;
            addMutation(saved);
        }
    }

    // ==================== SETTERS ====================

    public void setChoicePending(boolean pending, List<Mutation> choices) {
        this.choicePending = pending;
        this.pendingChoices = choices;
        this.choiceStartTime = pending ? System.currentTimeMillis() : 0;
    }

    public void setInsuredMutation(Mutation mutation) {
        this.insuredMutation = mutation;
    }

    // ==================== GETTERS CALCULÉS ====================

    /**
     * Obtient le bonus de lifesteal actuel (avec stacks)
     */
    public double getCurrentLifestealBonus() {
        double base = cachedStatBonuses.getOrDefault(StatType.LIFESTEAL, 0.0);
        double stacks = stackingLifesteal.get() * 5.0; // +5% par stack
        return base + stacks;
    }

    /**
     * Obtient le bonus de speed actuel (avec stacks)
     */
    public double getCurrentSpeedBonus() {
        double base = cachedStatBonuses.getOrDefault(StatType.MOVEMENT_SPEED, 0.0);
        double stacks = stackingSpeed.get() * 3.0; // +3% par stack
        return base + stacks;
    }

    /**
     * Obtient le bonus de crit actuel (avec stacks)
     */
    public double getCurrentCritBonus() {
        double base = cachedStatBonuses.getOrDefault(StatType.CRIT_CHANCE, 0.0);
        double stacks = stackingCrit.get() * 2.0; // +2% par stack
        return base + stacks;
    }

    /**
     * Obtient les tags pour l'ActionBar
     */
    public String getStrainTags() {
        StringBuilder sb = new StringBuilder();
        for (Mutation m : activeMutations) {
            sb.append(m.getStrain().getTag());
        }
        return sb.toString();
    }

    /**
     * Obtient le temps restant pour le choix (en secondes)
     */
    public int getChoiceTimeRemaining() {
        if (!choicePending) return 0;
        long elapsed = System.currentTimeMillis() - choiceStartTime;
        int remaining = 30 - (int)(elapsed / 1000);
        return Math.max(0, remaining);
    }
}
