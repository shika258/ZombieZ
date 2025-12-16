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
    // Équilibré pour travailler avec soft caps et CDR ultime 15% max
    private void registerGuerrierSkills() {
        ClassType c = ClassType.GUERRIER;

        // COMPÉTENCE 1 - Charge (BASE)
        // Fonce vers l'avant et frappe les ennemis
        register(new ActiveSkill(
            "gue_charge", "Charge Brutale",
            "Foncez vers l'avant et frappez tous les ennemis sur votre passage",
            c, SkillType.BASE, Material.IRON_SWORD,
            10, 25, 0,  // CD 8->10, énergie 20->25
            SkillEffect.DASH, 40, 3, 0,  // dégâts 50->40
            Sound.ENTITY_RAVAGER_ATTACK
        ));

        // COMPÉTENCE 2 - Cri de guerre (BASE)
        // Buff de dégâts temporaire (réduit)
        register(new ActiveSkill(
            "gue_warcry", "Cri de Guerre",
            "Poussez un cri terrifiant qui augmente vos dégâts de 25%",
            c, SkillType.BASE, Material.GOAT_HORN,
            25, 35, 0,  // CD 20->25, énergie 30->35
            SkillEffect.BUFF, 0, 0, 6,  // durée 8->6
            Sound.EVENT_RAID_HORN
        ));

        // COMPÉTENCE 3 - Frappe sismique (AVANCÉE)
        // AoE équilibré
        register(new ActiveSkill(
            "gue_slam", "Frappe Sismique",
            "Frappez le sol avec une force dévastatrice, infligeant des dégâts en zone",
            c, SkillType.ADVANCED, Material.MACE,
            18, 55, 5,  // CD 15->18
            SkillEffect.AOE_DAMAGE, 80, 4, 0,  // dégâts 100->80, rayon 5->4
            Sound.ENTITY_WARDEN_SONIC_BOOM
        ));

        // COMPÉTENCE 4 - Rage du Berserker (ULTIME)
        // Changé: Plus invulnérable, mais réduction de dégâts massive + boost
        // CDR max 15% = CD minimum ~102s
        register(new ActiveSkill(
            "gue_rage", "Rage du Berserker",
            "Réduction de dégâts 70% et +50% dégâts pendant 4s. Prend 10% HP en fin.",
            c, SkillType.ULTIMATE, Material.BLAZE_POWDER,
            120, 80, 10,  // CD 90->120 (avec 15% CDR = 102s), énergie 100->80
            SkillEffect.BUFF, 0, 0, 4,  // durée 5->4
            Sound.ENTITY_RAVAGER_ROAR
        ));
    }

    // ==================== CHASSEUR ====================
    // Focus: Distance, critiques, mobilité
    // Équilibré: dégâts conditionnels, pas de burst gratuit
    private void registerChasseurSkills() {
        ClassType c = ClassType.CHASSEUR;

        // COMPÉTENCE 1 - Tir rapide (BASE)
        // Tire plusieurs projectiles - dégâts moyens, bon pour le farming
        register(new ActiveSkill(
            "cha_multishot", "Tir Rapide",
            "Tirez une rafale de 3 flèches (25 dégâts chacune)",
            c, SkillType.BASE, Material.ARROW,
            8, 20, 0,  // CD 6->8, énergie 15->20
            SkillEffect.DAMAGE, 25, 0, 0,  // dégâts 30->25 (x3 = 75 total)
            Sound.ENTITY_ARROW_SHOOT
        ));

        // COMPÉTENCE 2 - Roulade (BASE)
        // Dash d'évasion - utilité pure
        register(new ActiveSkill(
            "cha_roll", "Roulade Tactique",
            "Effectuez une roulade rapide pour esquiver et repositionner",
            c, SkillType.BASE, Material.FEATHER,
            6, 15, 0,  // CD 5->6, énergie 10->15
            SkillEffect.DASH, 0, 0, 0,
            Sound.ENTITY_PHANTOM_FLAP
        ));

        // COMPÉTENCE 3 - Piège explosif (AVANCÉE)
        // Contrôle de zone, pas juste des dégâts
        register(new ActiveSkill(
            "cha_trap", "Piège Explosif",
            "Posez un piège qui explose et ralentit les ennemis 3s",
            c, SkillType.ADVANCED, Material.TRIPWIRE_HOOK,
            15, 45, 5,  // CD 12->15
            SkillEffect.AOE_DAMAGE, 60, 3, 20,  // dégâts 80->60, rayon 4->3
            Sound.BLOCK_TRIPWIRE_ATTACH
        ));

        // COMPÉTENCE 4 - Tir de Précision (ULTIME)
        // Changé: Plus x3 dégâts auto. Crit garanti mais dégâts normaux.
        // L'intérêt c'est le crit garanti + headshot si bien visé
        // CDR max 15% = CD minimum ~68s
        register(new ActiveSkill(
            "cha_deadeye", "Tir de Précision",
            "Tir à charge: critique garanti, +100% dégâts si headshot",
            c, SkillType.ULTIMATE, Material.ENDER_EYE,
            80, 60, 10,  // CD 60->80
            SkillEffect.EXECUTE, 120, 0, 0,  // dégâts 200->120
            Sound.ENTITY_ARROW_HIT_PLAYER
        ));
    }

    // ==================== OCCULTISTE ====================
    // Focus: AoE, sorts puissants, contrôle
    // Équilibré: Fragile, dépendant de l'énergie, pas de spam
    private void registerOccultisteSkills() {
        ClassType c = ClassType.OCCULTISTE;

        // COMPÉTENCE 1 - Orbe d'ombre (BASE)
        // Projectile magique - bon dégât single target avec petit AoE
        register(new ActiveSkill(
            "occ_orb", "Orbe d'Ombre",
            "Lancez un orbe d'énergie sombre qui explose à l'impact",
            c, SkillType.BASE, Material.ENDER_PEARL,
            5, 20, 0,  // CD 4->5, énergie 15->20
            SkillEffect.AOE_DAMAGE, 35, 2, 0,  // dégâts 40->35, rayon 3->2
            Sound.ENTITY_ENDER_DRAGON_SHOOT
        ));

        // COMPÉTENCE 2 - Drain vital (BASE)
        // Dégâts + heal réduit (travaille avec spell lifesteal cap)
        register(new ActiveSkill(
            "occ_drain", "Drain Vital",
            "Drainez la vie d'un ennemi pour vous soigner de 30% des dégâts",
            c, SkillType.BASE, Material.GHAST_TEAR,
            12, 30, 0,  // CD 10->12, énergie 25->30
            SkillEffect.DAMAGE, 50, 0, 0,  // dégâts 60->50
            Sound.ENTITY_ILLUSIONER_CAST_SPELL
        ));

        // COMPÉTENCE 3 - Nova de feu (AVANCÉE)
        // AoE équilibré autour du joueur
        register(new ActiveSkill(
            "occ_nova", "Nova Infernale",
            "Déclenchez une explosion de flammes autour de vous",
            c, SkillType.ADVANCED, Material.FIRE_CHARGE,
            18, 65, 5,  // CD 15->18
            SkillEffect.AOE_DAMAGE, 90, 5, 0,  // dégâts 120->90, rayon 6->5
            Sound.ENTITY_BLAZE_SHOOT
        ));

        // COMPÉTENCE 4 - Tempête Arcanique (ULTIME)
        // Changé: Plus de 300 dégâts. Zone plus petite, effet de ralentissement
        // CDR max 15% = CD minimum ~102s
        register(new ActiveSkill(
            "occ_apocalypse", "Tempête Arcanique",
            "Invoquez une tempête de magie pendant 4s. Ralentit de 50%.",
            c, SkillType.ULTIMATE, Material.NETHER_STAR,
            120, 90, 10,  // CD 90->120
            SkillEffect.AOE_DAMAGE, 150, 7, 4,  // dégâts 300->150, rayon 10->7
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
