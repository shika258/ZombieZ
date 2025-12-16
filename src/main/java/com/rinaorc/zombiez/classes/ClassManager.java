package com.rinaorc.zombiez.classes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.archetypes.ArchetypeManager;
import com.rinaorc.zombiez.classes.archetypes.ArchetypeSkillModifier;
import com.rinaorc.zombiez.classes.archetypes.ArchetypeTalentBonus;
import com.rinaorc.zombiez.classes.archetypes.BuildArchetype;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuff;
import com.rinaorc.zombiez.classes.buffs.ArcadeBuffRegistry;
import com.rinaorc.zombiez.classes.mutations.DailyMutation;
import com.rinaorc.zombiez.classes.mutations.MutationManager;
import com.rinaorc.zombiez.classes.skills.ActiveSkill;
import com.rinaorc.zombiez.classes.skills.SkillRegistry;
import com.rinaorc.zombiez.classes.talents.ClassTalent;
import com.rinaorc.zombiez.classes.talents.ClassTalentTree;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon;
import com.rinaorc.zombiez.classes.weapons.ClassWeaponRegistry;
import lombok.Getter;
import org.bukkit.entity.Player;

import static com.rinaorc.zombiez.classes.StatCalculator.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire du système de classes simplifié
 * Coordonne: talents, compétences, armes, buffs, mutations
 */
@Getter
public class ClassManager {

    private final ZombieZPlugin plugin;

    // Registres
    private final ClassTalentTree talentTree;
    private final SkillRegistry skillRegistry;
    private final ClassWeaponRegistry weaponRegistry;
    private final ArcadeBuffRegistry buffRegistry;
    private final MutationManager mutationManager;
    private final ArchetypeManager archetypeManager;

    // Cache des données
    private final Cache<UUID, ClassData> classDataCache;
    private final Map<UUID, List<ArcadeBuff>> pendingBuffChoices = new ConcurrentHashMap<>();

    // Configuration
    private static final int ENERGY_REGEN_RATE = 5;
    private static final int ENERGY_REGEN_INTERVAL = 20;

    public ClassManager(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Initialiser les registres
        this.talentTree = new ClassTalentTree();
        this.skillRegistry = new SkillRegistry();
        this.weaponRegistry = new ClassWeaponRegistry();
        this.buffRegistry = new ArcadeBuffRegistry();
        this.mutationManager = new MutationManager(plugin);
        this.archetypeManager = new ArchetypeManager();

        // Cache
        this.classDataCache = Caffeine.newBuilder()
            .maximumSize(250)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((uuid, data, cause) -> {
                if (data != null && ((ClassData) data).isDirty()) {
                    saveClassDataAsync((UUID) uuid, (ClassData) data);
                }
            })
            .build();

        startPeriodicTasks();

        plugin.getLogger().info("[Classes] Système initialisé: " +
            ClassType.values().length + " classes, " +
            talentTree.getTalentsById().size() + " talents, " +
            skillRegistry.getSkillsById().size() + " compétences");
    }

    private void startPeriodicTasks() {
        // Régénération d'énergie
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ClassData data : classDataCache.asMap().values()) {
                    if (data.hasClass()) {
                        int regenAmount = ENERGY_REGEN_RATE;
                        double mutationBonus = mutationManager.getMultiplier(
                            DailyMutation.MutationEffect.ENERGY_REGEN);
                        regenAmount = (int) (regenAmount * mutationBonus);
                        data.regenerateEnergy(regenAmount);
                    }
                }
            }
        }.runTaskTimer(plugin, ENERGY_REGEN_INTERVAL, ENERGY_REGEN_INTERVAL);
    }

    // ==================== GESTION DES DONNÉES ====================

    public ClassData getClassData(Player player) {
        return getClassData(player.getUniqueId());
    }

    public ClassData getClassData(UUID uuid) {
        return classDataCache.get(uuid, this::loadOrCreateClassData);
    }

    private ClassData loadOrCreateClassData(UUID uuid) {
        return new ClassData(uuid);
    }

    public CompletableFuture<Void> saveClassDataAsync(UUID uuid, ClassData data) {
        return CompletableFuture.runAsync(() -> {
            // TODO: Sauvegarde BDD
            data.clearDirty();
        });
    }

    public void unloadPlayer(UUID uuid) {
        ClassData data = classDataCache.getIfPresent(uuid);
        if (data != null && data.isDirty()) {
            saveClassDataAsync(uuid, data);
        }
        classDataCache.invalidate(uuid);
        pendingBuffChoices.remove(uuid);
    }

    // ==================== SÉLECTION DE CLASSE ====================

    public boolean selectClass(Player player, ClassType classType) {
        ClassData data = getClassData(player);

        // Cooldown de changement (24h)
        if (data.hasClass()) {
            long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
            long cooldown = 24 * 60 * 60 * 1000;

            if (timeSinceChange < cooldown) {
                long remainingHours = (cooldown - timeSinceChange) / (60 * 60 * 1000);
                player.sendMessage("§cChangement possible dans " + remainingHours + "h!");
                return false;
            }
        }

        data.changeClass(classType);

        // Message de confirmation
        player.sendMessage("");
        player.sendMessage("§a§l✓ " + classType.getColoredName() + " §a§lsélectionné!");
        player.sendMessage(classType.getDescription());
        player.sendMessage("");
        player.sendMessage("§7Difficulté: " + classType.getDifficultyDisplay());
        player.sendMessage("");

        for (String bonus : classType.getBonusDescription()) {
            player.sendMessage(bonus);
        }

        player.sendMessage("");
        player.sendMessage("§8Utilisez /class talents pour voir vos talents");
        player.sendMessage("§8Utilisez /class skills pour voir vos compétences");

        return true;
    }

    // ==================== TALENTS ====================

    public boolean unlockTalent(Player player, String talentId) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) {
            player.sendMessage("§cChoisissez d'abord une classe!");
            return false;
        }

        ClassTalent talent = talentTree.getTalent(talentId);
        if (talent == null || talent.getClassType() != data.getSelectedClass()) {
            player.sendMessage("§cTalent invalide!");
            return false;
        }

        if (data.getAvailableTalentPoints() < talent.getPointCost()) {
            player.sendMessage("§cPas assez de points! (" + talent.getPointCost() + " requis)");
            return false;
        }

        // Prérequis
        if (talent.getPrerequisiteId() != null) {
            int prereqLevel = data.getTalentLevel(talent.getPrerequisiteId());
            if (prereqLevel < 1) {
                ClassTalent prereq = talentTree.getTalent(talent.getPrerequisiteId());
                player.sendMessage("§cDébloquez d'abord: §e" + (prereq != null ? prereq.getName() : "???"));
                return false;
            }
        }

        if (data.getTalentLevel(talentId) >= talent.getMaxLevel()) {
            player.sendMessage("§cNiveau maximum atteint!");
            return false;
        }

        if (data.unlockTalent(talentId, talent.getPointCost())) {
            int newLevel = data.getTalentLevel(talentId);
            player.sendMessage("§a✓ " + talent.getName() + " §7niveau " + newLevel + "/" + talent.getMaxLevel());
            player.sendMessage("§7" + talent.getDescriptionAtLevel(newLevel));
            return true;
        }

        return false;
    }

    public boolean resetTalents(Player player, boolean free) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        if (!free) {
            int cost = 100;
            if (!plugin.getEconomyManager().hasGems(player, cost)) {
                player.sendMessage("§cCoût: " + cost + " Gemmes");
                return false;
            }
            plugin.getEconomyManager().removeGems(player, cost);
        }

        data.resetTalents();
        player.sendMessage("§a✓ Talents réinitialisés! (" + data.getAvailableTalentPoints() + " points)");
        return true;
    }

    // ==================== COMPÉTENCES ====================

    public boolean useSkill(Player player, String slot) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        String skillId = data.getEquippedSkill(slot);
        if (skillId == null) {
            player.sendMessage("§cAucune compétence dans ce slot!");
            return false;
        }

        ActiveSkill skill = skillRegistry.getSkill(skillId);
        if (skill == null) return false;

        // Cooldown
        if (data.isSkillOnCooldown(skillId)) {
            player.sendMessage("§cRecharge: " + data.getRemainingCooldown(skillId) + "s");
            return false;
        }

        // Énergie
        if (!data.consumeEnergy(skill.getEnergyCost())) {
            player.sendMessage("§cÉnergie insuffisante! (" + skill.getEnergyCost() + " requis)");
            return false;
        }

        // Appliquer cooldown avec réduction
        // IMPORTANT: Les ultimes ont un cap de CDR séparé (15% max)
        int cooldown = skill.getCooldown();
        boolean isUltimate = skill.getType() == ActiveSkill.SkillType.ULTIMATE;
        double cdrBonus = getTotalCooldownReduction(data, isUltimate);
        cooldown = (int) (cooldown * (1 - cdrBonus / 100));
        data.putSkillOnCooldown(skillId, Math.max(1, cooldown));

        data.incrementSkillsUsed();

        if (skill.getCastSound() != null) {
            player.playSound(player.getLocation(), skill.getCastSound(), 1.0f, 1.0f);
        }

        // Message différent pour les ultimes
        if (isUltimate) {
            player.sendMessage("§c§l✦ " + skill.getName() + " §c§l✦");
        } else {
            player.sendMessage("§a✦ " + skill.getName() + " §7activé!");
        }
        return true;
    }

    public boolean equipSkill(Player player, String skillId, int slotNumber) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        ActiveSkill skill = skillRegistry.getSkill(skillId);
        if (skill == null || skill.getClassType() != data.getSelectedClass()) {
            player.sendMessage("§cCompétence invalide!");
            return false;
        }

        if (!skill.isUnlocked(data.getClassLevel().get())) {
            player.sendMessage("§cNiveau " + skill.getRequiredLevel() + " requis!");
            return false;
        }

        String slot = "SLOT_" + slotNumber;
        data.equipSkill(slot, skillId);
        player.sendMessage("§a✓ " + skill.getName() + " §7équipé (slot " + slotNumber + ")");
        return true;
    }

    // ==================== LEVEL UP & BUFFS ====================

    public void handleClassLevelUp(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return;

        int newLevel = data.getClassLevel().get();

        player.sendMessage("");
        player.sendMessage("§6§l✦ NIVEAU " + newLevel + " ✦");
        player.sendMessage("§a+1 Point de Talent");
        player.sendMessage("");

        // Générer les choix de buffs
        List<ArcadeBuff> choices = buffRegistry.generateChoices(
            data.getSelectedClass(), data.getArcadeBuffs());

        if (!choices.isEmpty()) {
            pendingBuffChoices.put(player.getUniqueId(), choices);

            player.sendMessage("§e§lChoisissez un buff:");
            for (int i = 0; i < choices.size(); i++) {
                ArcadeBuff buff = choices.get(i);
                player.sendMessage("§7[" + (i + 1) + "] " + buff.getRarity().getColor() +
                    buff.getName() + " §7- " + buff.getFormattedDescription());
            }
            player.sendMessage("");
            player.sendMessage("§8/class buff <1-3>");
        }
    }

    public boolean selectLevelUpBuff(Player player, int choice) {
        List<ArcadeBuff> choices = pendingBuffChoices.get(player.getUniqueId());
        if (choices == null) {
            player.sendMessage("§cAucun choix en attente!");
            return false;
        }

        if (choice < 1 || choice > choices.size()) {
            player.sendMessage("§cChoix invalide!");
            return false;
        }

        ArcadeBuff selected = choices.get(choice - 1);
        ClassData data = getClassData(player);

        if (data.getBuffStacks(selected.getId()) >= selected.getMaxStacks()) {
            player.sendMessage("§cBuff au maximum!");
            return false;
        }

        data.addBuff(selected.getId());
        pendingBuffChoices.remove(player.getUniqueId());

        player.sendMessage("§a✓ " + selected.getRarity().getColor() + selected.getName());
        return true;
    }

    // ==================== CALCULS DE STATS ====================
    // Tous les bonus % sont soumis aux diminishing returns via StatCalculator

    /**
     * Multiplicateur de dégâts avec soft cap à +50% et hard cap à +100%
     */
    public double getTotalDamageMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        // Base class multiplier (non capé - fait partie de l'identité de classe)
        double baseMult = data.getSelectedClass().getDamageMultiplier();

        // Bonus additifs (talents + buffs) - soumis aux caps
        double rawBonus = 0;
        rawBonus += getTalentBonus(data, ClassTalent.TalentEffect.DAMAGE_PERCENT);
        rawBonus += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DAMAGE);

        // Appliquer diminishing returns sur les bonus
        double effectiveBonus = calculateEffectiveDamageBonus(rawBonus);

        // Mutation (multiplicative, non capée - événement temporaire)
        double mutationMult = mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_DAMAGE);

        return (baseMult + effectiveBonus / 100) * mutationMult;
    }

    /**
     * Obtenir les valeurs raw et effective pour l'affichage
     */
    public double[] getDamageMultiplierDetails(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return new double[]{1.0, 1.0};

        double rawBonus = getTalentBonus(data, ClassTalent.TalentEffect.DAMAGE_PERCENT)
            + buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DAMAGE);
        double effectiveBonus = calculateEffectiveDamageBonus(rawBonus);

        return new double[]{rawBonus, effectiveBonus};
    }

    /**
     * Multiplicateur de vie (pas de cap - les tanks doivent pouvoir investir)
     */
    public double getTotalHealthMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        double mult = data.getSelectedClass().getHealthMultiplier();
        mult += getTalentBonus(data, ClassTalent.TalentEffect.HEALTH_PERCENT) / 100;
        mult += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.HEALTH) / 100;
        mult *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_HEALTH);

        return mult;
    }

    /**
     * Chance de critique avec soft cap à 40% et hard cap à 75%
     */
    public double getTotalCritChance(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 15;

        // Crit de base modifié par le multiplicateur de classe
        double baseCrit = 15 * data.getSelectedClass().getCritMultiplier();

        // Bonus additifs (soumis aux caps)
        double rawBonus = 0;
        rawBonus += getTalentBonus(data, ClassTalent.TalentEffect.CRIT_CHANCE);
        rawBonus += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.CRIT_CHANCE);

        // Appliquer diminishing returns
        double effectiveBonus = calculateEffectiveCritChance(rawBonus);

        // Résultat final avec mutation
        double total = baseCrit + effectiveBonus;
        total *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_CRIT);

        return Math.min(total, CRIT_CHANCE_HARD_CAP);
    }

    /**
     * Dégâts critiques avec soft cap à +100% et hard cap à +200%
     */
    public double getTotalCritDamage(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 50; // Base +50%

        double rawBonus = getTalentBonus(data, ClassTalent.TalentEffect.CRIT_DAMAGE);
        rawBonus += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.CRIT_DAMAGE);

        return 50 + calculateEffectiveCritDamage(rawBonus);
    }

    /**
     * Vol de vie avec soft cap à 15% et hard cap à 30%
     * CRITIQUE pour éviter les builds immortels
     */
    public double getTotalLifesteal(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;

        // Base de classe (Guerrier a 10%)
        double baseLifesteal = data.getSelectedClass().getLifesteal() * 100;

        // Bonus talents + buffs
        double rawBonus = getTalentBonus(data, ClassTalent.TalentEffect.LIFESTEAL);
        rawBonus += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.LIFESTEAL);

        // Total raw puis diminishing returns
        double rawTotal = baseLifesteal + rawBonus;
        return calculateEffectiveLifesteal(rawTotal);
    }

    /**
     * Réduction de dégâts avec soft cap à 20% et hard cap à 50%
     */
    public double getTotalDamageReduction(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;

        double rawReduction = getTalentBonus(data, ClassTalent.TalentEffect.DAMAGE_REDUCTION);
        rawReduction += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DAMAGE_REDUCTION);

        return calculateEffectiveDamageReduction(rawReduction);
    }

    /**
     * Vitesse avec soft cap à 20% et hard cap à 35%
     */
    public double getTotalSpeedBonus(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;

        // Base de classe
        double baseBonus = (data.getSelectedClass().getSpeedMultiplier() - 1.0) * 100;

        double rawBonus = baseBonus + getTalentBonus(data, ClassTalent.TalentEffect.MOVEMENT_SPEED);
        rawBonus += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.SPEED);

        return calculateEffectiveSpeed(rawBonus);
    }

    /**
     * Esquive avec soft cap à 15% et hard cap à 30%
     */
    public double getTotalDodgeChance(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;

        double rawDodge = getTalentBonus(data, ClassTalent.TalentEffect.DODGE_CHANCE);
        rawDodge += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DODGE);

        return calculateEffectiveDodge(rawDodge);
    }

    /**
     * Réduction de cooldown avec soft cap à 25% et hard cap à 40%
     * IMPORTANT: Les ultimes ont un cap séparé de 15%
     */
    public double getTotalCooldownReduction(ClassData data, boolean isUltimate) {
        if (!data.hasClass()) return 0;

        double rawCdr = 0;
        rawCdr += getTalentBonus(data, ClassTalent.TalentEffect.COOLDOWN_REDUCTION);
        rawCdr += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.COOLDOWN);
        rawCdr += mutationManager.getModifier(DailyMutation.MutationEffect.PLAYER_COOLDOWN);

        return calculateEffectiveCDR(rawCdr, isUltimate);
    }

    /**
     * Version legacy pour compatibilité - considère non-ultime
     */
    public double getTotalCooldownReduction(ClassData data) {
        return getTotalCooldownReduction(data, false);
    }

    /**
     * Régénération avec soft cap à 30% et hard cap à 60%
     */
    public double getTotalRegenBonus(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 0;

        double rawRegen = getTalentBonus(data, ClassTalent.TalentEffect.REGEN_PERCENT);
        rawRegen += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.REGEN);

        return calculateEffectiveRegen(rawRegen);
    }

    private double getTalentBonus(ClassData data, ClassTalent.TalentEffect effectType) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : data.getUnlockedTalents().entrySet()) {
            ClassTalent talent = talentTree.getTalent(entry.getKey());
            if (talent != null && talent.getEffectType() == effectType) {
                total += talent.getValueAtLevel(entry.getValue());
            }
        }
        return total;
    }

    // ==================== ARCHÉTYPES ====================

    /**
     * Obtient l'archétype actif d'un joueur basé sur ses choix
     * L'archétype est calculé dynamiquement, puis caché pour performance
     */
    public BuildArchetype getPlayerArchetype(Player player) {
        return getPlayerArchetype(player.getUniqueId());
    }

    /**
     * Obtient l'archétype actif d'un joueur par UUID
     * Utilise un cache pour éviter les recalculs constants
     */
    public BuildArchetype getPlayerArchetype(UUID uuid) {
        ClassData data = getClassData(uuid);

        // Vérifier le cache d'abord
        String cachedId = data.getCachedArchetypeId();
        if (cachedId != null) {
            try {
                return BuildArchetype.valueOf(cachedId);
            } catch (IllegalArgumentException ignored) {
                // Cache invalide, recalculer
            }
        }

        // Calculer et cacher
        BuildArchetype archetype = archetypeManager.calculateArchetype(data, data.getEquippedSkillIds());
        data.setCachedArchetypeId(archetype.name());
        return archetype;
    }

    /**
     * Obtient les scores détaillés d'archétypes pour debug/UI
     */
    public Map<BuildArchetype, Integer> getArchetypeScores(Player player) {
        ClassData data = getClassData(player);
        return archetypeManager.getDetailedScores(data, data.getEquippedSkillIds());
    }

    /**
     * Obtient le résumé textuel de l'archétype actif
     */
    public String getArchetypeSummary(Player player) {
        BuildArchetype archetype = getPlayerArchetype(player);
        return archetypeManager.getArchetypeSummary(archetype);
    }

    /**
     * Obtient les modificateurs de skill pour l'archétype actif
     */
    public ArchetypeSkillModifier.SkillModification getSkillModification(Player player, String skillId) {
        BuildArchetype archetype = getPlayerArchetype(player);
        return ArchetypeSkillModifier.getModification(skillId, archetype);
    }

    /**
     * Calcule les dégâts d'un skill avec les modificateurs d'archétype
     */
    public double calculateSkillDamage(Player player, ActiveSkill skill, boolean isAoe) {
        BuildArchetype archetype = getPlayerArchetype(player);
        ArchetypeSkillModifier.SkillModification mod = ArchetypeSkillModifier.getModification(skill.getId(), archetype);

        // Dégâts de base du skill
        double baseDamage = skill.getDamageValue();

        // Appliquer le modificateur de skill de l'archétype
        baseDamage = mod.applyDamageModifier(baseDamage);

        // Appliquer le multiplicateur AoE ou single-target de l'archétype
        if (isAoe) {
            baseDamage *= archetypeManager.getAoeDamageModifier(archetype);
        } else {
            baseDamage *= archetypeManager.getSingleTargetModifier(archetype);
        }

        // Appliquer le multiplicateur de dégâts global du joueur
        baseDamage *= getTotalDamageMultiplier(player);

        return baseDamage;
    }

    /**
     * Calcule le cooldown effectif d'un skill avec les modificateurs d'archétype
     */
    public int calculateSkillCooldown(Player player, ActiveSkill skill) {
        ClassData data = getClassData(player);
        BuildArchetype archetype = getPlayerArchetype(player);
        ArchetypeSkillModifier.SkillModification mod = ArchetypeSkillModifier.getModification(skill.getId(), archetype);

        // Cooldown de base
        double cooldown = skill.getCooldown();

        // Modificateur d'archétype
        cooldown = mod.applyCooldownModifier(cooldown);

        // CDR du joueur
        boolean isUltimate = skill.getType() == ActiveSkill.SkillType.ULTIMATE;
        double cdrBonus = getTotalCooldownReduction(data, isUltimate);
        cooldown = cooldown * (1 - cdrBonus / 100);

        return Math.max(1, (int) cooldown);
    }

    /**
     * Calcule le rayon AoE effectif d'un skill avec les modificateurs d'archétype
     */
    public double calculateSkillRadius(Player player, ActiveSkill skill) {
        BuildArchetype archetype = getPlayerArchetype(player);
        ArchetypeSkillModifier.SkillModification mod = ArchetypeSkillModifier.getModification(skill.getId(), archetype);

        // Rayon de base
        double radius = skill.getRadius();

        // Modificateur d'archétype
        radius = mod.applyRadiusModifier(radius);

        // Bonus de rayon AoE des talents
        ClassData data = getClassData(player);
        double aoeBonus = getTalentBonus(data, ClassTalent.TalentEffect.AOE_RADIUS);
        radius *= (1 + aoeBonus / 100);

        return radius;
    }

    /**
     * Vérifie si un skill a un effet bonus d'archétype actif
     */
    public boolean hasArchetypeSkillBonus(Player player, String skillId) {
        BuildArchetype archetype = getPlayerArchetype(player);
        ArchetypeSkillModifier.SkillModification mod = ArchetypeSkillModifier.getModification(skillId, archetype);
        return mod.hasModification();
    }

    /**
     * Obtient la valeur effective d'un talent avec les bonus d'archétype
     */
    public double getEffectiveTalentValue(Player player, String talentId) {
        ClassData data = getClassData(player);
        BuildArchetype archetype = getPlayerArchetype(player);

        ClassTalent talent = talentTree.getTalent(talentId);
        if (talent == null) return 0;

        int level = data.getTalentLevel(talentId);
        if (level <= 0) return 0;

        double baseValue = talent.getBaseValue();

        // Appliquer les bonus d'archétype
        return ArchetypeTalentBonus.calculateEffectiveValue(archetype, talentId, baseValue, level);
    }

    /**
     * Vérifie si un talent a un effet spécial avec l'archétype actif
     */
    public boolean hasTalentSpecialEffect(Player player, String talentId) {
        BuildArchetype archetype = getPlayerArchetype(player);
        return ArchetypeTalentBonus.hasSpecialEffect(archetype, talentId);
    }

    /**
     * Obtient le modificateur de réduction de dégâts basé sur l'archétype
     * (pour les archétypes tank comme Mur Vivant)
     */
    public double getArchetypeTankModifier(Player player) {
        BuildArchetype archetype = getPlayerArchetype(player);
        return archetypeManager.getTankModifier(archetype);
    }

    // ==================== ARMES ====================

    public boolean canUseClassWeapon(Player player, String weaponId) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        ClassWeapon weapon = weaponRegistry.getWeapon(weaponId);
        if (weapon == null) return false;

        return weapon.getRequiredClass() == data.getSelectedClass() &&
               weapon.isUnlocked(data.getClassLevel().get());
    }

    public String getWeaponRestrictionMessage(Player player, String weaponId) {
        ClassData data = getClassData(player);
        ClassWeapon weapon = weaponRegistry.getWeapon(weaponId);

        if (weapon == null) return "§cArme inconnue!";
        if (!data.hasClass()) return "§cChoisissez d'abord une classe!";
        if (weapon.getRequiredClass() != data.getSelectedClass()) {
            return "§cRéservé aux " + weapon.getRequiredClass().getColoredName() + "§c!";
        }
        if (!weapon.isUnlocked(data.getClassLevel().get())) {
            return "§cNiveau " + weapon.getRequiredLevel() + " requis!";
        }
        return null;
    }

    // ==================== SHUTDOWN ====================

    public void shutdown() {
        plugin.getLogger().info("[Classes] Sauvegarde...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<UUID, ClassData> entry : classDataCache.asMap().entrySet()) {
            if (entry.getValue().isDirty()) {
                futures.add(saveClassDataAsync(entry.getKey(), entry.getValue()));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        classDataCache.invalidateAll();

        plugin.getLogger().info("[Classes] Sauvegarde terminée.");
    }
}
