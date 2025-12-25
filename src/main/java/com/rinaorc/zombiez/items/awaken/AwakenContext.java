package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Contexte d'exécution d'un talent avec éveil
 *
 * Utilisé par TalentListener pour appliquer les modificateurs d'éveil
 * lors de l'exécution des talents.
 *
 * Pattern d'utilisation:
 * 1. Créer un contexte au début de l'exécution du talent
 * 2. Appliquer les modificateurs selon le type d'éveil
 * 3. Utiliser les valeurs modifiées pour l'effet
 */
@Getter
@Builder
public class AwakenContext {

    /**
     * Le joueur qui exécute le talent
     */
    private final Player player;

    /**
     * Le talent en cours d'exécution
     */
    private final Talent talent;

    /**
     * L'éveil actif (peut être null si pas d'éveil)
     */
    private final Awaken activeAwaken;

    /**
     * La cible (peut être null pour les AoE)
     */
    private final LivingEntity target;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifie si un éveil est actif
     */
    public boolean hasAwaken() {
        return activeAwaken != null;
    }

    /**
     * Vérifie si l'éveil correspond au talent en cours
     */
    public boolean isAwakenForCurrentTalent() {
        if (activeAwaken == null || talent == null) return false;
        return talent.getId().equals(activeAwaken.getTargetTalentId());
    }

    /**
     * Obtient le type de modificateur de l'éveil
     */
    public AwakenModifierType getModifierType() {
        return activeAwaken != null ? activeAwaken.getModifierType() : null;
    }

    /**
     * Vérifie si l'éveil a un type de modificateur spécifique
     */
    public boolean hasModifierType(AwakenModifierType type) {
        return activeAwaken != null && activeAwaken.getModifierType() == type;
    }

    // ==================== MODIFICATEURS DE VALEURS ====================

    /**
     * Applique le modificateur de dégâts
     *
     * @param baseDamage Dégâts de base
     * @return Dégâts modifiés
     */
    public double applyDamageModifier(double baseDamage) {
        if (!isAwakenForCurrentTalent()) return baseDamage;

        return switch (activeAwaken.getModifierType()) {
            case DAMAGE_BONUS ->
                baseDamage * (1.0 + activeAwaken.getModifierValue() / 100.0);
            default -> baseDamage;
        };
    }

    /**
     * Applique le modificateur de dégâts critiques
     *
     * @param baseCritDamage Dégâts critiques de base (multiplicateur)
     * @return Dégâts critiques modifiés
     */
    public double applyCritDamageModifier(double baseCritDamage) {
        if (!isAwakenForCurrentTalent()) return baseCritDamage;

        if (activeAwaken.getModifierType() == AwakenModifierType.CRIT_DAMAGE_BONUS) {
            return baseCritDamage + (activeAwaken.getModifierValue() / 100.0);
        }
        return baseCritDamage;
    }

    /**
     * Applique le modificateur de chance de critique
     *
     * @param baseCritChance Chance de base (0.0 à 1.0)
     * @return Chance modifiée
     */
    public double applyCritChanceModifier(double baseCritChance) {
        if (!isAwakenForCurrentTalent()) return baseCritChance;

        if (activeAwaken.getModifierType() == AwakenModifierType.CRIT_CHANCE_BONUS) {
            return Math.min(1.0, baseCritChance + (activeAwaken.getModifierValue() / 100.0));
        }
        return baseCritChance;
    }

    /**
     * Applique le modificateur de cooldown
     *
     * @param baseCooldown Cooldown de base en ms
     * @return Cooldown modifié
     */
    public long applyCooldownModifier(long baseCooldown) {
        if (!isAwakenForCurrentTalent()) return baseCooldown;

        if (activeAwaken.getModifierType() == AwakenModifierType.COOLDOWN_REDUCTION) {
            return (long) (baseCooldown * (1.0 - activeAwaken.getModifierValue() / 100.0));
        }
        return baseCooldown;
    }

    /**
     * Applique le modificateur de durée
     *
     * @param baseDuration Durée de base en ms
     * @return Durée modifiée
     */
    public long applyDurationModifier(long baseDuration) {
        if (!isAwakenForCurrentTalent()) return baseDuration;

        if (activeAwaken.getModifierType() == AwakenModifierType.DURATION_EXTENSION) {
            return (long) (baseDuration * (1.0 + activeAwaken.getModifierValue() / 100.0));
        }
        return baseDuration;
    }

    /**
     * Applique le modificateur de rayon
     *
     * @param baseRadius Rayon de base
     * @return Rayon modifié
     */
    public double applyRadiusModifier(double baseRadius) {
        if (!isAwakenForCurrentTalent()) return baseRadius;

        if (activeAwaken.getModifierType() == AwakenModifierType.RADIUS_BONUS) {
            return baseRadius * (1.0 + activeAwaken.getModifierValue() / 100.0);
        }
        return baseRadius;
    }

    /**
     * Obtient le nombre d'invocations/projectiles supplémentaires
     *
     * @return Nombre supplémentaire (0 si pas applicable)
     */
    public int getExtraCount() {
        if (!isAwakenForCurrentTalent()) return 0;

        return switch (activeAwaken.getModifierType()) {
            case EXTRA_SUMMON, EXTRA_PROJECTILE, EXTRA_BOUNCE, EXTRA_STACKS ->
                activeAwaken.getModifierValueAsInt();
            default -> 0;
        };
    }

    /**
     * Obtient la réduction de seuil (compteurs: stacks, kills)
     *
     * @return Réduction (0 si pas applicable)
     */
    public int getThresholdReduction() {
        if (!isAwakenForCurrentTalent()) return 0;

        if (activeAwaken.getModifierType() == AwakenModifierType.REDUCED_THRESHOLD) {
            return activeAwaken.getModifierValueAsInt();
        }
        return 0;
    }

    /**
     * Obtient le bonus de seuil en pourcentage (HP, Surchauffe, Virulence)
     * Ex: Execute <15% HP avec +5% bonus → <20% HP
     *
     * @return Bonus de seuil en % (0 si pas applicable)
     */
    public double getThresholdBonus() {
        if (!isAwakenForCurrentTalent()) return 0;

        if (activeAwaken.getModifierType() == AwakenModifierType.THRESHOLD_BONUS) {
            return activeAwaken.getModifierValue();
        }
        return 0;
    }

    /**
     * Applique le bonus de seuil à un seuil HP en pourcentage
     * Ex: applyThresholdBonus(15.0) avec +5% → 20.0
     *
     * @param baseThreshold Seuil de base (ex: 15.0 pour 15% HP)
     * @return Seuil modifié
     */
    public double applyThresholdBonus(double baseThreshold) {
        if (!isAwakenForCurrentTalent()) return baseThreshold;

        if (activeAwaken.getModifierType() == AwakenModifierType.THRESHOLD_BONUS) {
            return baseThreshold + activeAwaken.getModifierValue();
        }
        return baseThreshold;
    }

    /**
     * Applique le modificateur de chance de proc
     *
     * @param baseProcChance Chance de base (0.0 à 1.0)
     * @return Chance modifiée
     */
    public double applyProcChanceModifier(double baseProcChance) {
        if (!isAwakenForCurrentTalent()) return baseProcChance;

        if (activeAwaken.getModifierType() == AwakenModifierType.PROC_CHANCE_BONUS) {
            return Math.min(1.0, baseProcChance + (activeAwaken.getModifierValue() / 100.0));
        }
        return baseProcChance;
    }

    /**
     * Vérifie si un slow doit être appliqué
     */
    public boolean shouldApplySlow() {
        return isAwakenForCurrentTalent() &&
               activeAwaken.getModifierType() == AwakenModifierType.APPLY_SLOW;
    }

    /**
     * Obtient la durée du slow en ticks
     */
    public int getSlowDurationTicks() {
        if (!shouldApplySlow()) return 0;
        return (int) (activeAwaken.getModifierValue() * 20); // Secondes en ticks
    }

    /**
     * Vérifie si une vulnérabilité doit être appliquée
     */
    public boolean shouldApplyVulnerability() {
        return isAwakenForCurrentTalent() &&
               activeAwaken.getModifierType() == AwakenModifierType.APPLY_VULNERABILITY;
    }

    /**
     * Obtient le bonus de dégâts de vulnérabilité (0.0 à 1.0)
     */
    public double getVulnerabilityBonus() {
        if (!shouldApplyVulnerability()) return 0;
        return activeAwaken.getModifierValue() / 100.0;
    }

    /**
     * Vérifie si un buff de vitesse doit être appliqué
     */
    public boolean shouldApplySpeedBuff() {
        return isAwakenForCurrentTalent() &&
               activeAwaken.getModifierType() == AwakenModifierType.SPEED_BUFF;
    }

    /**
     * Obtient le bonus de vitesse (0.0 à 1.0)
     */
    public double getSpeedBonus() {
        if (!shouldApplySpeedBuff()) return 0;
        return activeAwaken.getModifierValue() / 100.0;
    }

    /**
     * Vérifie si un heal doit être appliqué
     */
    public boolean shouldHealOnProc() {
        return isAwakenForCurrentTalent() &&
               activeAwaken.getModifierType() == AwakenModifierType.HEAL_ON_PROC;
    }

    /**
     * Obtient le pourcentage de heal (basé sur max HP)
     */
    public double getHealPercent() {
        if (!shouldHealOnProc()) return 0;
        return activeAwaken.getModifierValue() / 100.0;
    }

    /**
     * Vérifie si un bouclier doit être appliqué
     */
    public boolean shouldApplyShield() {
        return isAwakenForCurrentTalent() &&
               activeAwaken.getModifierType() == AwakenModifierType.SHIELD_ON_PROC;
    }

    /**
     * Obtient le pourcentage de bouclier (basé sur max HP)
     */
    public double getShieldPercent() {
        if (!shouldApplyShield()) return 0;
        return activeAwaken.getModifierValue() / 100.0;
    }

    /**
     * Obtient le bonus d'XP (0.0 à 1.0)
     */
    public double getXpBonus() {
        if (!isAwakenForCurrentTalent()) return 0;
        if (activeAwaken.getModifierType() == AwakenModifierType.XP_BONUS) {
            return activeAwaken.getModifierValue() / 100.0;
        }
        return 0;
    }

    /**
     * Obtient le bonus de loot (0.0 à 1.0)
     */
    public double getLootBonus() {
        if (!isAwakenForCurrentTalent()) return 0;
        if (activeAwaken.getModifierType() == AwakenModifierType.LOOT_BONUS) {
            return activeAwaken.getModifierValue() / 100.0;
        }
        return 0;
    }

    // ==================== FACTORY METHOD ====================

    /**
     * Crée un contexte d'exécution pour un talent
     * Cherche l'éveil correspondant dans tout l'équipement (main hand, armures, off-hand)
     *
     * @param plugin Le plugin
     * @param player Le joueur
     * @param talent Le talent
     * @param target La cible (peut être null)
     * @return Le contexte
     */
    public static AwakenContext create(ZombieZPlugin plugin, Player player, Talent talent, LivingEntity target) {
        AwakenManager awakenManager = plugin.getAwakenManager();
        Awaken activeAwaken = null;

        if (awakenManager != null && talent != null) {
            // Cherche un éveil correspondant au talent dans tout l'équipement
            activeAwaken = awakenManager.getActiveAwakenForTalent(player, talent.getId());
        }

        return AwakenContext.builder()
            .player(player)
            .talent(talent)
            .activeAwaken(activeAwaken)
            .target(target)
            .build();
    }

    /**
     * Crée un contexte vide (pas d'éveil actif)
     */
    public static AwakenContext empty(Player player, Talent talent, LivingEntity target) {
        return AwakenContext.builder()
            .player(player)
            .talent(talent)
            .activeAwaken(null)
            .target(target)
            .build();
    }
}
