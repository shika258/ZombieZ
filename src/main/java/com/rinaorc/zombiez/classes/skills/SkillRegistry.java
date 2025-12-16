package com.rinaorc.zombiez.classes.skills;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.skills.ActiveSkill.SkillEffect;
import com.rinaorc.zombiez.classes.skills.ActiveSkill.SkillType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

/**
 * Registre des compétences simplifié - 4 compétences par classe
 *
 * Structure par classe:
 * - 2 compétences de BASE (niveau 1)
 * - 1 compétence AVANCÉE (niveau 5)
 * - 1 compétence ULTIME (niveau 10)
 *
 * Le joueur peut équiper jusqu'à 3 compétences actives.
 */
@Getter
public class SkillRegistry {

    private final Map<ClassType, List<ActiveSkill>> skillsByClass;
    private final Map<String, ActiveSkill> skillsById;

    public SkillRegistry() {
        this.skillsByClass = new EnumMap<>(ClassType.class);
        this.skillsById = new LinkedHashMap<>();

        registerAllSkills();
    }

    private void register(ActiveSkill skill) {
        skillsById.put(skill.getId(), skill);
        skillsByClass.computeIfAbsent(skill.getClassType(), k -> new ArrayList<>()).add(skill);
    }

    private void registerAllSkills() {
        registerGuerrierSkills();
        registerChasseurSkills();
        registerOccultisteSkills();
    }

    // ==================== GUERRIER ====================
    // Focus: Mêlée, survie, dégâts bruts
    private void registerGuerrierSkills() {
        ClassType c = ClassType.GUERRIER;

        // COMPÉTENCE 1 - Charge (BASE)
        // Fonce vers l'avant et frappe les ennemis
        register(new ActiveSkill(
            "gue_charge", "Charge Brutale",
            "Foncez vers l'avant et frappez tous les ennemis sur votre passage",
            c, SkillType.BASE, Material.IRON_SWORD,
            8, 20, 0,
            SkillEffect.DASH, 50, 3, 0,
            Sound.ENTITY_RAVAGER_ATTACK
        ));

        // COMPÉTENCE 2 - Cri de guerre (BASE)
        // Buff de dégâts temporaire
        register(new ActiveSkill(
            "gue_warcry", "Cri de Guerre",
            "Poussez un cri terrifiant qui augmente vos dégâts de 30%",
            c, SkillType.BASE, Material.GOAT_HORN,
            20, 30, 0,
            SkillEffect.BUFF, 0, 0, 8,
            Sound.EVENT_RAID_HORN
        ));

        // COMPÉTENCE 3 - Frappe sismique (AVANCÉE)
        // Gros AoE au sol
        register(new ActiveSkill(
            "gue_slam", "Frappe Sismique",
            "Frappez le sol avec une force dévastatrice, infligeant des dégâts en zone",
            c, SkillType.ADVANCED, Material.MACE,
            15, 50, 5,
            SkillEffect.AOE_DAMAGE, 100, 5, 0,
            Sound.ENTITY_WARDEN_SONIC_BOOM
        ));

        // COMPÉTENCE 4 - Rage immortelle (ULTIME)
        // Invulnérable + boost de dégâts massif
        register(new ActiveSkill(
            "gue_rage", "Rage Immortelle",
            "Devenez invulnérable et gagnez +100% dégâts pendant 5 secondes",
            c, SkillType.ULTIMATE, Material.TOTEM_OF_UNDYING,
            90, 100, 10,
            SkillEffect.BUFF, 0, 0, 5,
            Sound.ITEM_TOTEM_USE
        ));
    }

    // ==================== CHASSEUR ====================
    // Focus: Distance, critiques, mobilité
    private void registerChasseurSkills() {
        ClassType c = ClassType.CHASSEUR;

        // COMPÉTENCE 1 - Tir rapide (BASE)
        // Tire plusieurs projectiles rapidement
        register(new ActiveSkill(
            "cha_multishot", "Tir Rapide",
            "Tirez une rafale de 3 flèches en succession rapide",
            c, SkillType.BASE, Material.ARROW,
            6, 15, 0,
            SkillEffect.DAMAGE, 30, 0, 0,
            Sound.ENTITY_ARROW_SHOOT
        ));

        // COMPÉTENCE 2 - Roulade (BASE)
        // Dash d'évasion
        register(new ActiveSkill(
            "cha_roll", "Roulade Tactique",
            "Effectuez une roulade rapide pour esquiver et repositionner",
            c, SkillType.BASE, Material.FEATHER,
            5, 10, 0,
            SkillEffect.DASH, 0, 0, 0,
            Sound.ENTITY_PHANTOM_FLAP
        ));

        // COMPÉTENCE 3 - Piège explosif (AVANCÉE)
        // Pose un piège qui explose
        register(new ActiveSkill(
            "cha_trap", "Piège Explosif",
            "Posez un piège invisible qui explose au passage des ennemis",
            c, SkillType.ADVANCED, Material.TRIPWIRE_HOOK,
            12, 40, 5,
            SkillEffect.AOE_DAMAGE, 80, 4, 30,
            Sound.BLOCK_TRIPWIRE_ATTACH
        ));

        // COMPÉTENCE 4 - Tir mortel (ULTIME)
        // Gros crit garanti
        register(new ActiveSkill(
            "cha_deadeye", "Tir Mortel",
            "Concentrez-vous pour un tir parfait: critique garanti x3 dégâts",
            c, SkillType.ULTIMATE, Material.ENDER_EYE,
            60, 80, 10,
            SkillEffect.EXECUTE, 200, 0, 0,
            Sound.ENTITY_ARROW_HIT_PLAYER
        ));
    }

    // ==================== OCCULTISTE ====================
    // Focus: AoE, sorts puissants, contrôle
    private void registerOccultisteSkills() {
        ClassType c = ClassType.OCCULTISTE;

        // COMPÉTENCE 1 - Orbe d'ombre (BASE)
        // Projectile magique
        register(new ActiveSkill(
            "occ_orb", "Orbe d'Ombre",
            "Lancez un orbe d'énergie sombre qui explose à l'impact",
            c, SkillType.BASE, Material.ENDER_PEARL,
            4, 15, 0,
            SkillEffect.AOE_DAMAGE, 40, 3, 0,
            Sound.ENTITY_ENDER_DRAGON_SHOOT
        ));

        // COMPÉTENCE 2 - Drain vital (BASE)
        // Dégâts + heal
        register(new ActiveSkill(
            "occ_drain", "Drain Vital",
            "Drainez la vie d'un ennemi pour vous soigner de 50% des dégâts",
            c, SkillType.BASE, Material.GHAST_TEAR,
            10, 25, 0,
            SkillEffect.DAMAGE, 60, 0, 0,
            Sound.ENTITY_ILLUSIONER_CAST_SPELL
        ));

        // COMPÉTENCE 3 - Nova de feu (AVANCÉE)
        // Grosse explosion AoE
        register(new ActiveSkill(
            "occ_nova", "Nova Infernale",
            "Déclenchez une explosion de flammes autour de vous",
            c, SkillType.ADVANCED, Material.FIRE_CHARGE,
            15, 60, 5,
            SkillEffect.AOE_DAMAGE, 120, 6, 0,
            Sound.ENTITY_BLAZE_SHOOT
        ));

        // COMPÉTENCE 4 - Apocalypse (ULTIME)
        // Dégâts AoE massifs sur la durée
        register(new ActiveSkill(
            "occ_apocalypse", "Apocalypse",
            "Invoquez une pluie de météores dévastateurs sur la zone ciblée",
            c, SkillType.ULTIMATE, Material.NETHER_STAR,
            90, 100, 10,
            SkillEffect.AOE_DAMAGE, 300, 10, 5,
            Sound.ENTITY_WITHER_SPAWN
        ));
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient une compétence par son ID
     */
    public ActiveSkill getSkill(String id) {
        return skillsById.get(id);
    }

    /**
     * Obtient toutes les compétences d'une classe
     */
    public List<ActiveSkill> getSkillsForClass(ClassType classType) {
        return skillsByClass.getOrDefault(classType, Collections.emptyList());
    }

    /**
     * Obtient les compétences débloquées pour un niveau de classe
     */
    public List<ActiveSkill> getUnlockedSkills(ClassType classType, int classLevel) {
        return getSkillsForClass(classType).stream()
            .filter(s -> s.isUnlocked(classLevel))
            .toList();
    }

    /**
     * Obtient les compétences par type
     */
    public List<ActiveSkill> getSkillsByType(ClassType classType, SkillType type) {
        return getSkillsForClass(classType).stream()
            .filter(s -> s.getType() == type)
            .toList();
    }

    /**
     * Obtient toutes les compétences de base (niveau 0)
     */
    public List<ActiveSkill> getBaseSkills(ClassType classType) {
        return getSkillsByType(classType, SkillType.BASE);
    }

    /**
     * Obtient la compétence ultime d'une classe
     */
    public ActiveSkill getUltimateSkill(ClassType classType) {
        return getSkillsByType(classType, SkillType.ULTIMATE).stream()
            .findFirst()
            .orElse(null);
    }

    /**
     * Vérifie si une compétence appartient à une classe
     */
    public boolean isSkillForClass(String skillId, ClassType classType) {
        ActiveSkill skill = getSkill(skillId);
        return skill != null && skill.getClassType() == classType;
    }
}
