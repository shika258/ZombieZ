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
 * Gestionnaire principal du système de classes
 * Coordonne tous les sous-systèmes: talents, compétences, armes, buffs, mutations
 */
@Getter
public class ClassManager {

    private final ZombieZPlugin plugin;

    // Registres des systèmes
    private final ClassTalentTree talentTree;
    private final SkillRegistry skillRegistry;
    private final ClassWeaponRegistry weaponRegistry;
    private final ArcadeBuffRegistry buffRegistry;
    private final MutationManager mutationManager;

    // Cache des données de classe (même principe que PlayerDataManager)
    private final Cache<UUID, ClassData> classDataCache;
    private final Map<UUID, ClassData> pendingLevelUpChoice = new ConcurrentHashMap<>();

    // Configuration
    private static final int LEVELS_PER_TALENT_POINT = 1;
    private static final int ENERGY_REGEN_RATE = 5; // par seconde
    private static final int ENERGY_REGEN_INTERVAL = 20; // ticks (1 seconde)

    public ClassManager(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Initialiser les registres
        this.talentTree = new ClassTalentTree();
        this.skillRegistry = new SkillRegistry();
        this.weaponRegistry = new ClassWeaponRegistry();
        this.buffRegistry = new ArcadeBuffRegistry();
        this.mutationManager = new MutationManager(plugin);

        // Cache Caffeine
        this.classDataCache = Caffeine.newBuilder()
            .maximumSize(250)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((uuid, data, cause) -> {
                if (data != null && ((ClassData) data).isDirty()) {
                    saveClassDataAsync((UUID) uuid, (ClassData) data);
                }
            })
            .build();

        // Démarrer les tâches périodiques
        startPeriodicTasks();

        plugin.getLogger().info("[Classes] Système de classes initialisé avec " +
            ClassType.values().length + " classes, " +
            talentTree.getTalentsById().size() + " talents, " +
            skillRegistry.getSkillsById().size() + " compétences, " +
            weaponRegistry.getAllWeapons().size() + " armes");
    }

    /**
     * Démarre les tâches périodiques
     */
    private void startPeriodicTasks() {
        // Régénération d'énergie
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ClassData data : classDataCache.asMap().values()) {
                    if (data.hasClass()) {
                        int regenAmount = ENERGY_REGEN_RATE;
                        // Bonus de mutation si applicable
                        double mutationBonus = mutationManager.getMultiplier(
                            DailyMutation.MutationEffect.ENERGY_REGEN);
                        regenAmount = (int) (regenAmount * mutationBonus);
                        data.regenerateEnergy(regenAmount);
                    }
                }
            }
        }.runTaskTimer(plugin, ENERGY_REGEN_INTERVAL, ENERGY_REGEN_INTERVAL);

        // Vérification des mutations à minuit
        new BukkitRunnable() {
            @Override
            public void run() {
                mutationManager.refreshDailyMutations();
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Toutes les minutes
    }

    // ==================== GESTION DES DONNÉES ====================

    /**
     * Obtient les données de classe d'un joueur
     */
    public ClassData getClassData(Player player) {
        return getClassData(player.getUniqueId());
    }

    /**
     * Obtient les données de classe par UUID
     */
    public ClassData getClassData(UUID uuid) {
        return classDataCache.get(uuid, this::loadOrCreateClassData);
    }

    /**
     * Charge ou crée des données de classe
     */
    private ClassData loadOrCreateClassData(UUID uuid) {
        // Ici on pourrait charger depuis la BDD
        // Pour l'instant, on crée des nouvelles données
        return new ClassData(uuid);
    }

    /**
     * Sauvegarde async les données de classe
     */
    public CompletableFuture<Void> saveClassDataAsync(UUID uuid, ClassData data) {
        return CompletableFuture.runAsync(() -> {
            // TODO: Implémenter la sauvegarde en BDD
            data.clearDirty();
        });
    }

    /**
     * Décharge les données d'un joueur (déconnexion)
     */
    public void unloadPlayer(UUID uuid) {
        ClassData data = classDataCache.getIfPresent(uuid);
        if (data != null && data.isDirty()) {
            saveClassDataAsync(uuid, data);
        }
        classDataCache.invalidate(uuid);
        pendingLevelUpChoice.remove(uuid);
    }

    // ==================== SÉLECTION DE CLASSE ====================

    /**
     * Sélectionne une classe pour un joueur
     */
    public boolean selectClass(Player player, ClassType classType) {
        ClassData data = getClassData(player);

        // Vérifier si le joueur peut changer de classe
        if (data.hasClass()) {
            long timeSinceChange = System.currentTimeMillis() - data.getLastClassChange();
            long cooldown = 24 * 60 * 60 * 1000; // 24h cooldown

            if (timeSinceChange < cooldown) {
                long remainingHours = (cooldown - timeSinceChange) / (60 * 60 * 1000);
                player.sendMessage("§cVous devez attendre encore " + remainingHours + "h avant de changer de classe!");
                return false;
            }
        }

        data.changeClass(classType);

        // Équiper les compétences de base
        List<ActiveSkill> baseSkills = skillRegistry.getBaseSkills(classType);
        for (ActiveSkill skill : baseSkills) {
            if (skill.getSlot() == ActiveSkill.SkillSlot.PRIMARY &&
                data.getEquippedSkill("PRIMARY") == null) {
                data.equipSkill("PRIMARY", skill.getId());
            } else if (skill.getSlot() == ActiveSkill.SkillSlot.SECONDARY &&
                data.getEquippedSkill("SECONDARY") == null) {
                data.equipSkill("SECONDARY", skill.getId());
            }
        }

        player.sendMessage("§a✓ Vous êtes maintenant " + classType.getColoredName() + "§a!");
        player.sendMessage("§7" + classType.getDescription());

        // Afficher les bonus
        for (String bonus : classType.getBonusDescription()) {
            player.sendMessage(bonus);
        }

        return true;
    }

    // ==================== TALENTS ====================

    /**
     * Débloque un talent pour un joueur
     */
    public boolean unlockTalent(Player player, String talentId) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) {
            player.sendMessage("§cVous devez d'abord choisir une classe!");
            return false;
        }

        ClassTalent talent = talentTree.getTalent(talentId);
        if (talent == null) {
            player.sendMessage("§cTalent inconnu!");
            return false;
        }

        // Vérifier que c'est un talent de sa classe
        if (talent.getClassType() != data.getSelectedClass()) {
            player.sendMessage("§cCe talent n'est pas disponible pour votre classe!");
            return false;
        }

        // Vérifier les points disponibles
        if (data.getAvailableTalentPoints() < talent.getPointCost()) {
            player.sendMessage("§cPas assez de points de talents! (Requis: " +
                talent.getPointCost() + ", Disponible: " + data.getAvailableTalentPoints() + ")");
            return false;
        }

        // Vérifier le prérequis
        if (talent.getPrerequisiteId() != null) {
            ClassTalent prereq = talentTree.getTalent(talent.getPrerequisiteId());
            if (prereq != null && data.getTalentLevel(talent.getPrerequisiteId()) < prereq.getMaxLevel()) {
                player.sendMessage("§cVous devez d'abord maximiser: §e" + prereq.getName());
                return false;
            }
        }

        // Vérifier le niveau max
        if (data.getTalentLevel(talentId) >= talent.getMaxLevel()) {
            player.sendMessage("§cCe talent est déjà au niveau maximum!");
            return false;
        }

        // Débloquer
        if (data.unlockTalent(talentId, talent.getPointCost())) {
            int newLevel = data.getTalentLevel(talentId);
            player.sendMessage("§a✓ Talent amélioré: §e" + talent.getName() +
                " §7(Niveau " + newLevel + "/" + talent.getMaxLevel() + ")");
            player.sendMessage("§7" + talent.getDescriptionAtLevel(newLevel));

            // Vérifier si ça débloque une compétence ou arme
            checkTalentUnlocks(player, data, talent);

            return true;
        }

        return false;
    }

    /**
     * Vérifie si un talent débloque quelque chose de spécial
     */
    private void checkTalentUnlocks(Player player, ClassData data, ClassTalent talent) {
        if (talent.getEffectType() == ClassTalent.TalentEffect.UNLOCK_SKILL) {
            // Trouver la compétence débloquée
            List<ActiveSkill> skills = skillRegistry.getSkillsForClass(data.getSelectedClass());
            for (ActiveSkill skill : skills) {
                if (skill.isRequiresUnlock() && skill.getUnlockTalentId().equals(talent.getId())) {
                    player.sendMessage("§d✦ Nouvelle compétence débloquée: §f" + skill.getName());
                    break;
                }
            }
        } else if (talent.getEffectType() == ClassTalent.TalentEffect.UNLOCK_WEAPON) {
            // Trouver l'arme débloquée
            List<ClassWeapon> weapons = weaponRegistry.getWeaponsForClass(data.getSelectedClass());
            for (ClassWeapon weapon : weapons) {
                if (weapon.getUnlockTalentId().equals(talent.getId())) {
                    player.sendMessage("§6✦ Nouvelle arme débloquée: " +
                        weapon.getTier().getColor() + weapon.getName());
                    break;
                }
            }
        }
    }

    /**
     * Reset les talents d'un joueur
     */
    public boolean resetTalents(Player player, boolean free) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        if (!free) {
            // Coût en gemmes
            int cost = 100;
            if (!plugin.getEconomyManager().hasGems(player, cost)) {
                player.sendMessage("§cCoût du reset: " + cost + " Gemmes");
                return false;
            }
            plugin.getEconomyManager().removeGems(player, cost);
        }

        data.resetTalents();
        player.sendMessage("§a✓ Talents réinitialisés!");
        player.sendMessage("§7Vous avez " + data.getAvailableTalentPoints() + " points à redistribuer.");

        return true;
    }

    // ==================== COMPÉTENCES ====================

    /**
     * Équipe une compétence
     */
    public boolean equipSkill(Player player, String skillId) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        ActiveSkill skill = skillRegistry.getSkill(skillId);
        if (skill == null) return false;

        // Vérifier la classe
        if (skill.getClassType() != data.getSelectedClass()) {
            player.sendMessage("§cCette compétence n'est pas disponible pour votre classe!");
            return false;
        }

        // Vérifier le déverrouillage
        if (skill.isRequiresUnlock() && !data.hasTalent(skill.getUnlockTalentId())) {
            player.sendMessage("§cVous devez d'abord débloquer le talent requis!");
            return false;
        }

        String slot = skill.getSlot().name();
        data.equipSkill(slot, skillId);

        player.sendMessage("§a✓ Compétence équipée: §e" + skill.getName() +
            " §7(Slot " + skill.getSlot().getDisplayName() + ")");

        return true;
    }

    /**
     * Utilise une compétence
     */
    public boolean useSkill(Player player, String slot) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        String skillId = data.getEquippedSkill(slot);
        if (skillId == null) {
            player.sendMessage("§cAucune compétence équipée dans ce slot!");
            return false;
        }

        ActiveSkill skill = skillRegistry.getSkill(skillId);
        if (skill == null) return false;

        // Vérifier le cooldown
        if (data.isSkillOnCooldown(skillId)) {
            player.sendMessage("§cCompétence en recharge! (" + data.getRemainingCooldown(skillId) + "s)");
            return false;
        }

        // Vérifier l'énergie
        if (!data.consumeEnergy(skill.getManaCost())) {
            player.sendMessage("§cPas assez d'énergie! (Requis: " + skill.getManaCost() + ")");
            return false;
        }

        // Mettre en cooldown
        int cooldown = skill.getBaseCooldown();
        // Appliquer réduction de cooldown des talents/buffs
        double cdrBonus = getTotalCooldownReduction(data);
        cooldown = (int) (cooldown * (1 - cdrBonus / 100));
        data.putSkillOnCooldown(skillId, Math.max(1, cooldown));

        // Incrémenter les stats
        data.incrementSkillsUsed();

        // Jouer le son
        if (skill.getCastSound() != null) {
            player.playSound(player.getLocation(), skill.getCastSound(), 1.0f, 1.0f);
        }

        // L'effet de la compétence sera géré par un listener séparé
        return true;
    }

    // ==================== LEVEL UP & BUFFS ARCADE ====================

    /**
     * Gère un level up de classe
     */
    public void handleClassLevelUp(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return;

        int newLevel = data.getClassLevel().get();

        player.sendMessage("");
        player.sendMessage("§6§l✦ NIVEAU DE CLASSE " + newLevel + " ✦");
        player.sendMessage("§a+1 Point de Talent");
        player.sendMessage("");

        // Générer les choix de buffs
        List<ArcadeBuff> choices = buffRegistry.generateLevelUpChoices(
            data.getSelectedClass(), data.getArcadeBuffs());

        if (!choices.isEmpty()) {
            pendingLevelUpChoice.put(player.getUniqueId(), data);

            player.sendMessage("§e§lChoisissez un buff:");
            for (int i = 0; i < choices.size(); i++) {
                ArcadeBuff buff = choices.get(i);
                player.sendMessage("§7[" + (i + 1) + "] " + buff.getRarity().getColor() +
                    buff.getName() + " §7- " + buff.getFormattedDescription(data.getBuffStacks(buff.getId())));
            }
            player.sendMessage("");
            player.sendMessage("§8Utilisez /class buff <1-3> pour choisir");
        }
    }

    /**
     * Sélectionne un buff de level up
     */
    public boolean selectLevelUpBuff(Player player, int choice) {
        ClassData data = pendingLevelUpChoice.get(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cAucun choix de buff en attente!");
            return false;
        }

        List<ArcadeBuff> choices = buffRegistry.generateLevelUpChoices(
            data.getSelectedClass(), data.getArcadeBuffs());

        if (choice < 1 || choice > choices.size()) {
            player.sendMessage("§cChoix invalide! (1-" + choices.size() + ")");
            return false;
        }

        ArcadeBuff selected = choices.get(choice - 1);

        // Vérifier le max stacks
        if (data.getBuffStacks(selected.getId()) >= selected.getMaxStacks()) {
            player.sendMessage("§cCe buff est déjà au maximum!");
            return false;
        }

        data.addBuff(selected.getId());
        pendingLevelUpChoice.remove(player.getUniqueId());

        player.sendMessage("§a✓ Buff obtenu: " + selected.getRarity().getColor() + selected.getName());
        player.sendMessage("§7" + selected.getFormattedDescription(data.getBuffStacks(selected.getId())));

        return true;
    }

    // ==================== CALCULS DE STATS ====================

    /**
     * Calcule le multiplicateur de dégâts total
     */
    public double getTotalDamageMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        double multiplier = data.getSelectedClass().getDamageMultiplier();

        // Bonus des talents
        multiplier += getTalentBonus(data, ClassTalent.TalentEffect.DAMAGE_PERCENT) / 100;

        // Bonus des buffs arcade
        multiplier += buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.DAMAGE_PERCENT) / 100;

        // Mutations quotidiennes
        multiplier *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_DAMAGE);

        return multiplier;
    }

    /**
     * Calcule le multiplicateur de HP total
     */
    public double getTotalHealthMultiplier(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 1.0;

        double multiplier = data.getSelectedClass().getHealthMultiplier();

        // Talents
        multiplier += getTalentBonus(data, ClassTalent.TalentEffect.HEALTH_PERCENT) / 100;

        // Buffs
        multiplier += buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.HEALTH_PERCENT) / 100;

        // Mutations
        multiplier *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_HEALTH);

        return multiplier;
    }

    /**
     * Calcule le bonus de critique total
     */
    public double getTotalCritChance(Player player) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return 15; // Base crit

        double crit = 15 * data.getSelectedClass().getCritMultiplier();

        // Talents
        crit += getTalentBonus(data, ClassTalent.TalentEffect.CRIT_CHANCE);

        // Buffs
        crit += buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.CRIT_CHANCE);

        // Mutations
        crit *= mutationManager.getMultiplier(DailyMutation.MutationEffect.PLAYER_CRIT);

        return crit;
    }

    /**
     * Calcule la réduction de cooldown totale
     */
    public double getTotalCooldownReduction(ClassData data) {
        if (!data.hasClass()) return 0;

        double cdr = 0;

        // Talents
        cdr += getTalentBonus(data, ClassTalent.TalentEffect.COOLDOWN_REDUCTION);

        // Buffs
        cdr += buffRegistry.calculateTotalBonus(data.getArcadeBuffs(),
            ArcadeBuff.BuffEffect.COOLDOWN_RED);

        // Mutations
        cdr += mutationManager.getModifier(DailyMutation.MutationEffect.PLAYER_COOLDOWN);

        return Math.min(cdr, 50); // Cap à 50%
    }

    /**
     * Obtient le bonus total d'un type de talent
     */
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

    // ==================== VÉRIFICATION DES ARMES ====================

    /**
     * Vérifie si un joueur peut utiliser une arme de classe
     */
    public boolean canUseClassWeapon(Player player, String weaponId) {
        ClassData data = getClassData(player);
        if (!data.hasClass()) return false;

        ClassWeapon weapon = weaponRegistry.getWeapon(weaponId);
        if (weapon == null) return false;

        // Vérifier la classe
        if (weapon.getRequiredClass() != data.getSelectedClass()) {
            return false;
        }

        // Vérifier le talent de déblocage
        return data.hasTalent(weapon.getUnlockTalentId());
    }

    /**
     * Obtient le message d'erreur pour une arme non utilisable
     */
    public String getWeaponRestrictionMessage(Player player, String weaponId) {
        ClassData data = getClassData(player);
        ClassWeapon weapon = weaponRegistry.getWeapon(weaponId);

        if (weapon == null) return "§cArme inconnue!";

        if (!data.hasClass()) {
            return "§cVous devez choisir une classe pour utiliser cette arme!";
        }

        if (weapon.getRequiredClass() != data.getSelectedClass()) {
            return "§cCette arme est réservée aux " + weapon.getRequiredClass().getColoredName() + "§c!";
        }

        if (!data.hasTalent(weapon.getUnlockTalentId())) {
            ClassTalent talent = talentTree.getTalent(weapon.getUnlockTalentId());
            return "§cVous devez débloquer le talent §e" +
                (talent != null ? talent.getName() : weapon.getUnlockTalentId()) + "§c!";
        }

        return null; // Pas de restriction
    }

    // ==================== SHUTDOWN ====================

    /**
     * Sauvegarde toutes les données en attente
     */
    public void shutdown() {
        plugin.getLogger().info("[Classes] Sauvegarde des données de classe...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<UUID, ClassData> entry : classDataCache.asMap().entrySet()) {
            if (entry.getValue().isDirty()) {
                futures.add(saveClassDataAsync(entry.getKey(), entry.getValue()));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        classDataCache.invalidateAll();

        plugin.getLogger().info("[Classes] Données sauvegardées.");
    }
}
