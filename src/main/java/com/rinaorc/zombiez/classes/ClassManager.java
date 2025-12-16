package com.rinaorc.zombiez.classes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rinaorc.zombiez.ZombieZPlugin;
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
        int cooldown = skill.getCooldown();
        double cdrBonus = getTotalCooldownReduction(data);
        cooldown = (int) (cooldown * (1 - cdrBonus / 100));
        data.putSkillOnCooldown(skillId, Math.max(1, cooldown));

        data.incrementSkillsUsed();

        if (skill.getCastSound() != null) {
            player.playSound(player.getLocation(), skill.getCastSound(), 1.0f, 1.0f);
        }

        player.sendMessage("§a✦ " + skill.getName() + " §7activé!");
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

    public double getTotalDamageMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        double mult = data.getSelectedClass().getDamageMultiplier();
        mult += getTalentBonus(data, ClassTalent.TalentEffect.DAMAGE_PERCENT) / 100;
        mult += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.DAMAGE) / 100;
        mult *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_DAMAGE);

        return mult;
    }

    public double getTotalHealthMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        double mult = data.getSelectedClass().getHealthMultiplier();
        mult += getTalentBonus(data, ClassTalent.TalentEffect.HEALTH_PERCENT) / 100;
        mult += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.HEALTH) / 100;
        mult *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_HEALTH);

        return mult;
    }

    public double getTotalCritChance(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 15;

        double crit = 15 * data.getSelectedClass().getCritMultiplier();
        crit += getTalentBonus(data, ClassTalent.TalentEffect.CRIT_CHANCE);
        crit += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.CRIT_CHANCE);
        crit *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_CRIT);

        return crit;
    }

    public double getTotalCooldownReduction(ClassData data) {
        if (!data.hasClass()) return 0;

        double cdr = 0;
        cdr += getTalentBonus(data, ClassTalent.TalentEffect.COOLDOWN_REDUCTION);
        cdr += buffRegistry.getTotalBonus(data.getArcadeBuffs(), ArcadeBuff.BuffEffect.COOLDOWN);
        cdr += mutationManager.getModifier(DailyMutation.MutationEffect.PLAYER_COOLDOWN);

        return Math.min(cdr, 50);
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
