package com.rinaorc.zombiez.classes.skills;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.skills.ActiveSkill.SkillEffect;
import com.rinaorc.zombiez.classes.skills.ActiveSkill.SkillSlot;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

/**
 * Registre de toutes les compétences actives par classe
 * Chaque classe a 6 compétences: 2 primaires, 2 secondaires, 2 ultimes
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
        registerCommandoSkills();
        registerScoutSkills();
        registerMedicSkills();
        registerEngineerSkills();
        registerBerserkerSkills();
        registerSniperSkills();
    }

    // ==================== COMMANDO ====================
    private void registerCommandoSkills() {
        ClassType c = ClassType.COMMANDO;

        // === PRIMAIRES ===
        register(new ActiveSkill("cmd_burst", "Tir en Rafale",
            "Tire une rafale de 5 projectiles en succession rapide",
            c, SkillSlot.PRIMARY, Material.BLAZE_ROD, 8, 20,
            false, null,
            SkillEffect.LINE_DAMAGE, 15, 2, 0, 0, Sound.ENTITY_FIREWORK_ROCKET_BLAST));

        register(new ActiveSkill("cmd_grenade", "Grenade Frag",
            "Lance une grenade explosive qui inflige des dégâts en zone",
            c, SkillSlot.PRIMARY, Material.FIRE_CHARGE, 12, 30,
            false, null,
            SkillEffect.AOE_DAMAGE, 40, 4, 4, 0, Sound.ENTITY_GENERIC_EXPLODE));

        // === SECONDAIRES ===
        register(new ActiveSkill("cmd_suppression", "Tir de Suppression",
            "Tire en continu pendant 3s, ralentissant tous les ennemis touchés",
            c, SkillSlot.SECONDARY, Material.CHAIN, 20, 50,
            false, null,
            SkillEffect.SLOW, 25, 3, 6, 3, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE));

        register(new ActiveSkill("cmd_flashbang", "Grenade Flashbang",
            "Aveugle et étourdit les ennemis dans la zone pendant 2s",
            c, SkillSlot.SECONDARY, Material.GLOWSTONE_DUST, 25, 40,
            false, null,
            SkillEffect.STUN, 0, 0, 5, 2, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST));

        // === ULTIMES ===
        register(new ActiveSkill("cmd_airstrike", "Frappe Aérienne",
            "Appelle une frappe aérienne dévastatrice sur la zone ciblée",
            c, SkillSlot.ULTIMATE, Material.TNT, 60, 100,
            true, "cmd_barrage",
            SkillEffect.AOE_DAMAGE, 200, 15, 8, 0, Sound.ENTITY_LIGHTNING_BOLT_THUNDER));

        register(new ActiveSkill("cmd_siege", "Mode Siège",
            "S'ancre au sol, +50% dégâts et résistance, mais immobile pendant 10s",
            c, SkillSlot.ULTIMATE, Material.NETHERITE_CHESTPLATE, 90, 80,
            true, "cmd_fortress",
            SkillEffect.BUFF_SELF, 0, 0, 0, 10, Sound.BLOCK_ANVIL_LAND));
    }

    // ==================== ÉCLAIREUR (SCOUT) ====================
    private void registerScoutSkills() {
        ClassType c = ClassType.SCOUT;

        // === PRIMAIRES ===
        register(new ActiveSkill("sct_backstab", "Coup dans le Dos",
            "Dash derrière la cible et inflige des dégâts critiques garantis",
            c, SkillSlot.PRIMARY, Material.IRON_SWORD, 10, 25,
            false, null,
            SkillEffect.SINGLE_TARGET_DAMAGE, 30, 3, 0, 0, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        register(new ActiveSkill("sct_trap", "Piège à Loup",
            "Pose un piège invisible qui immobilise et endommage les ennemis",
            c, SkillSlot.PRIMARY, Material.TRIPWIRE_HOOK, 15, 20,
            false, null,
            SkillEffect.SUMMON_TRAP, 20, 2, 0, 5, Sound.BLOCK_TRIPWIRE_ATTACH));

        // === SECONDAIRES ===
        register(new ActiveSkill("sct_vanish", "Disparition",
            "Devient invisible pendant 5s, le prochain coup inflige +100% dégâts",
            c, SkillSlot.SECONDARY, Material.POTION, 25, 50,
            false, null,
            SkillEffect.INVISIBILITY, 0, 0, 0, 5, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE));

        register(new ActiveSkill("sct_smokebomb", "Bombe Fumigène",
            "Crée un nuage de fumée qui aveugle les ennemis et augmente l'esquive",
            c, SkillSlot.SECONDARY, Material.GRAY_DYE, 20, 35,
            false, null,
            SkillEffect.BUFF_SELF, 0, 0, 5, 4, Sound.ENTITY_SPLASH_POTION_BREAK));

        // === ULTIMES ===
        register(new ActiveSkill("sct_bladedance", "Danse des Lames",
            "Enchaîne 8 coups rapides sur les ennemis proches en 3s",
            c, SkillSlot.ULTIMATE, Material.DIAMOND_SWORD, 60, 100,
            true, "sct_shadow",
            SkillEffect.AOE_DAMAGE, 150, 12, 5, 3, Sound.ENTITY_PLAYER_ATTACK_CRIT));

        register(new ActiveSkill("sct_shadowcloak", "Voile d'Ombre",
            "Invisibilité totale 10s + 200% dégâts au premier coup + reset cooldowns",
            c, SkillSlot.ULTIMATE, Material.ENDER_EYE, 90, 80,
            true, "sct_phantom",
            SkillEffect.INVISIBILITY, 0, 0, 0, 10, Sound.ENTITY_ENDERMAN_TELEPORT));
    }

    // ==================== MÉDIC ====================
    private void registerMedicSkills() {
        ClassType c = ClassType.MEDIC;

        // === PRIMAIRES ===
        register(new ActiveSkill("med_heal", "Soin d'Urgence",
            "Soigne instantanément vous-même ou un allié ciblé",
            c, SkillSlot.PRIMARY, Material.GLISTERING_MELON_SLICE, 8, 30,
            false, null,
            SkillEffect.SELF_HEAL, 50, 5, 0, 0, Sound.ENTITY_PLAYER_LEVELUP));

        register(new ActiveSkill("med_injection", "Injection Stimulante",
            "Injecte un stim qui augmente la vitesse et les dégâts de 25% pendant 8s",
            c, SkillSlot.PRIMARY, Material.BLAZE_POWDER, 15, 35,
            false, null,
            SkillEffect.BUFF_SELF, 0, 0, 0, 8, Sound.ENTITY_WITCH_DRINK));

        // === SECONDAIRES ===
        register(new ActiveSkill("med_aura", "Aura de Régénération",
            "Crée une zone de soin qui régénère tous les alliés proches",
            c, SkillSlot.SECONDARY, Material.GOLDEN_APPLE, 25, 60,
            false, null,
            SkillEffect.AOE_HEAL, 10, 2, 6, 8, Sound.BLOCK_BEACON_AMBIENT));

        register(new ActiveSkill("med_toxin", "Toxine Paralysante",
            "Tire une seringue qui empoisonne et ralentit la cible",
            c, SkillSlot.SECONDARY, Material.SPIDER_EYE, 18, 40,
            false, null,
            SkillEffect.DOT_DAMAGE, 30, 3, 0, 6, Sound.ENTITY_SPIDER_AMBIENT));

        // === ULTIMES ===
        register(new ActiveSkill("med_revive", "Résurrection de Masse",
            "Ressuscite tous les alliés morts dans la zone avec 50% HP",
            c, SkillSlot.ULTIMATE, Material.TOTEM_OF_UNDYING, 120, 100,
            true, "med_immortal",
            SkillEffect.RESURRECT, 0, 0, 10, 0, Sound.ITEM_TOTEM_USE));

        register(new ActiveSkill("med_plague", "Nuage Toxique",
            "Libère un nuage de poison qui inflige des dégâts massifs sur la durée",
            c, SkillSlot.ULTIMATE, Material.DRAGON_BREATH, 60, 80,
            true, "med_pandemic",
            SkillEffect.DOT_DAMAGE, 200, 15, 6, 10, Sound.ENTITY_ENDER_DRAGON_GROWL));
    }

    // ==================== INGÉNIEUR ====================
    private void registerEngineerSkills() {
        ClassType c = ClassType.ENGINEER;

        // === PRIMAIRES ===
        register(new ActiveSkill("eng_turret", "Tourelle Automatique",
            "Déploie une tourelle qui tire sur les ennemis pendant 30s",
            c, SkillSlot.PRIMARY, Material.DISPENSER, 30, 50,
            false, null,
            SkillEffect.SUMMON_TURRET, 15, 2, 0, 30, Sound.BLOCK_PISTON_EXTEND));

        register(new ActiveSkill("eng_mine", "Mine de Proximité",
            "Pose une mine invisible qui explose au passage des ennemis",
            c, SkillSlot.PRIMARY, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 12, 25,
            false, null,
            SkillEffect.SUMMON_TRAP, 60, 5, 3, 60, Sound.BLOCK_STONE_BUTTON_CLICK_ON));

        // === SECONDAIRES ===
        register(new ActiveSkill("eng_shield", "Bouclier Énergétique",
            "Crée un bouclier qui absorbe les prochains dégâts",
            c, SkillSlot.SECONDARY, Material.END_CRYSTAL, 25, 50,
            false, null,
            SkillEffect.SHIELD, 100, 10, 0, 8, Sound.BLOCK_BEACON_ACTIVATE));

        register(new ActiveSkill("eng_drone", "Drone de Combat",
            "Déploie un drone qui suit et attaque les ennemis",
            c, SkillSlot.SECONDARY, Material.BAT_SPAWN_EGG, 35, 60,
            false, null,
            SkillEffect.SUMMON_MINION, 20, 3, 0, 20, Sound.ENTITY_BEE_LOOP));

        // === ULTIMES ===
        register(new ActiveSkill("eng_orbital", "Frappe Orbitale",
            "Appelle un bombardement orbital massif sur la zone ciblée",
            c, SkillSlot.ULTIMATE, Material.TNT_MINECART, 90, 100,
            true, "eng_artillery",
            SkillEffect.AOE_DAMAGE, 300, 20, 10, 3, Sound.ENTITY_WITHER_SPAWN));

        register(new ActiveSkill("eng_dome", "Dôme Protecteur",
            "Crée un dôme qui bloque tous les projectiles pendant 10s",
            c, SkillSlot.ULTIMATE, Material.BEACON, 60, 80,
            true, "eng_bunker",
            SkillEffect.SHIELD, 500, 30, 6, 10, Sound.BLOCK_BEACON_POWER_SELECT));
    }

    // ==================== BERSERKER ====================
    private void registerBerserkerSkills() {
        ClassType c = ClassType.BERSERKER;

        // === PRIMAIRES ===
        register(new ActiveSkill("ber_charge", "Charge Brutale",
            "Fonce vers l'ennemi, l'étourdit et inflige des dégâts",
            c, SkillSlot.PRIMARY, Material.NETHERITE_AXE, 10, 25,
            false, null,
            SkillEffect.DASH, 40, 4, 0, 1, Sound.ENTITY_RAVAGER_STEP));

        register(new ActiveSkill("ber_warcry", "Cri de Guerre",
            "Pousse un cri qui augmente vos dégâts de 30% et effraie les faibles",
            c, SkillSlot.PRIMARY, Material.GOAT_HORN, 20, 30,
            false, null,
            SkillEffect.BUFF_SELF, 0, 0, 8, 10, Sound.EVENT_RAID_HORN));

        // === SECONDAIRES ===
        register(new ActiveSkill("ber_slam", "Frappe Sismique",
            "Frappe le sol, infligeant des dégâts et projetant les ennemis",
            c, SkillSlot.SECONDARY, Material.MACE, 15, 40,
            false, null,
            SkillEffect.AOE_DAMAGE, 60, 6, 5, 0, Sound.ENTITY_IRON_GOLEM_ATTACK));

        register(new ActiveSkill("ber_frenzy", "Frénésie",
            "Entre en frénésie: +50% vitesse d'attaque, vol de vie doublé pendant 8s",
            c, SkillSlot.SECONDARY, Material.BLAZE_POWDER, 30, 50,
            false, null,
            SkillEffect.BUFF_SELF, 0, 0, 0, 8, Sound.ENTITY_VINDICATOR_AMBIENT));

        // === ULTIMES ===
        register(new ActiveSkill("ber_whirlwind", "Tourbillon Sanglant",
            "Tournoie sur place pendant 5s, infligeant des dégâts massifs autour",
            c, SkillSlot.ULTIMATE, Material.NETHERITE_AXE, 60, 100,
            true, "ber_rampage",
            SkillEffect.AOE_DAMAGE, 250, 20, 4, 5, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        register(new ActiveSkill("ber_immortal", "Rage Éternelle",
            "Devient immortel pendant 5s, puis récupère 50% des dégâts subis en HP",
            c, SkillSlot.ULTIMATE, Material.TOTEM_OF_UNDYING, 120, 80,
            true, "ber_undying",
            SkillEffect.INVULNERABILITY, 0, 0, 0, 5, Sound.ITEM_TOTEM_USE));
    }

    // ==================== SNIPER ====================
    private void registerSniperSkills() {
        ClassType c = ClassType.SNIPER;

        // === PRIMAIRES ===
        register(new ActiveSkill("snp_pierce", "Tir Perforant",
            "Tire un projectile qui traverse tous les ennemis en ligne",
            c, SkillSlot.PRIMARY, Material.SPECTRAL_ARROW, 12, 30,
            false, null,
            SkillEffect.LINE_DAMAGE, 50, 5, 0, 0, Sound.ENTITY_ARROW_HIT));

        register(new ActiveSkill("snp_mark", "Marque du Chasseur",
            "Marque un ennemi, révélant sa position et augmentant les dégâts reçus",
            c, SkillSlot.PRIMARY, Material.GLOW_INK_SAC, 15, 20,
            false, null,
            SkillEffect.MARK, 0, 0, 0, 15, Sound.ENTITY_EVOKER_PREPARE_ATTACK));

        // === SECONDAIRES ===
        register(new ActiveSkill("snp_camo", "Camouflage",
            "Devient invisible et gagne +50% dégâts au prochain tir",
            c, SkillSlot.SECONDARY, Material.GREEN_DYE, 20, 40,
            false, null,
            SkillEffect.INVISIBILITY, 0, 0, 0, 8, Sound.ENTITY_PHANTOM_AMBIENT));

        register(new ActiveSkill("snp_explosive", "Tir Explosif",
            "Tire un projectile qui explose à l'impact",
            c, SkillSlot.SECONDARY, Material.FIRE_CHARGE, 18, 45,
            false, null,
            SkillEffect.AOE_DAMAGE, 80, 8, 4, 0, Sound.ENTITY_GENERIC_EXPLODE));

        // === ULTIMES ===
        register(new ActiveSkill("snp_deadeye", "Tir Parfait",
            "Le prochain tir est un headshot garanti x3 dégâts, ignore armure",
            c, SkillSlot.ULTIMATE, Material.ENDER_EYE, 45, 80,
            true, "snp_deadeye",
            SkillEffect.SINGLE_TARGET_DAMAGE, 300, 25, 0, 0, Sound.ENTITY_ARROW_HIT_PLAYER));

        register(new ActiveSkill("snp_spectral", "Tir Spectral",
            "Tire un projectile qui ignore les murs et touche tous les ennemis marqués",
            c, SkillSlot.ULTIMATE, Material.SCULK_SENSOR, 90, 100,
            true, "snp_shadow_sniper",
            SkillEffect.AOE_DAMAGE, 150, 15, 50, 0, Sound.ENTITY_VEX_CHARGE));
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
     * Obtient les compétences d'un slot spécifique
     */
    public List<ActiveSkill> getSkillsBySlot(ClassType classType, SkillSlot slot) {
        return getSkillsForClass(classType).stream()
            .filter(s -> s.getSlot() == slot)
            .toList();
    }

    /**
     * Obtient les compétences débloquées (sans prérequis de talent)
     */
    public List<ActiveSkill> getUnlockedSkills(ClassType classType, Set<String> unlockedTalents) {
        return getSkillsForClass(classType).stream()
            .filter(s -> !s.isRequiresUnlock() || unlockedTalents.contains(s.getUnlockTalentId()))
            .toList();
    }

    /**
     * Obtient toutes les compétences de base (sans prérequis)
     */
    public List<ActiveSkill> getBaseSkills(ClassType classType) {
        return getSkillsForClass(classType).stream()
            .filter(s -> !s.isRequiresUnlock())
            .toList();
    }
}
