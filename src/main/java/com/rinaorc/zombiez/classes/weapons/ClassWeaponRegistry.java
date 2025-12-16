package com.rinaorc.zombiez.classes.weapons;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponEffect;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponTier;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

/**
 * Registre de toutes les armes exclusives par classe
 * Chaque classe a 3 armes uniques: Basique, Avancée, Légendaire
 */
@Getter
public class ClassWeaponRegistry {

    private final Map<ClassType, List<ClassWeapon>> weaponsByClass;
    private final Map<String, ClassWeapon> weaponsById;

    public ClassWeaponRegistry() {
        this.weaponsByClass = new EnumMap<>(ClassType.class);
        this.weaponsById = new LinkedHashMap<>();

        registerAllWeapons();
    }

    private void register(ClassWeapon weapon) {
        weaponsById.put(weapon.getId(), weapon);
        weaponsByClass.computeIfAbsent(weapon.getRequiredClass(), k -> new ArrayList<>()).add(weapon);
    }

    private void registerAllWeapons() {
        registerCommandoWeapons();
        registerScoutWeapons();
        registerMedicWeapons();
        registerEngineerWeapons();
        registerBerserkerWeapons();
        registerSniperWeapons();
    }

    // ==================== COMMANDO ====================
    private void registerCommandoWeapons() {
        ClassType c = ClassType.COMMANDO;

        // Tier 1 - Basique: LMG Ravageur
        register(new ClassWeapon(
            "cmd_lmg", "LMG Ravageur",
            "Mitrailleuse lourde à haute cadence de tir, parfaite pour le contrôle de zone",
            c, WeaponTier.BASIC, Material.CROSSBOW, WeaponType.HEAVY, "cmd_lmg",
            12, 2.5, 8, 1.5,
            WeaponEffect.CLEAVE, 30, 2, Sound.ENTITY_FIREWORK_ROCKET_BLAST
        ));

        // Tier 2 - Avancée: Minigun Apocalypse
        register(new ClassWeapon(
            "cmd_minigun", "Minigun Apocalypse",
            "Minigun dévastatrice qui déchire tout sur son passage",
            c, WeaponTier.ADVANCED, Material.BLAZE_ROD, WeaponType.HEAVY, "cmd_minigun",
            8, 4.0, 5, 1.3,
            WeaponEffect.PIERCE, 40, 3, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE
        ));

        // Tier 3 - Légendaire: Lance-Roquettes Dévastateur
        register(new ClassWeapon(
            "cmd_rocket", "Lance-Roquettes Dévastateur",
            "Arme de destruction massive, chaque tir est une explosion dévastatrice",
            c, WeaponTier.LEGENDARY, Material.FIREWORK_ROCKET, WeaponType.HEAVY, "cmd_rocket",
            80, 0.5, 15, 2.0,
            WeaponEffect.EXPLOSIVE, 25, 6, Sound.ENTITY_GENERIC_EXPLODE
        ));
    }

    // ==================== ÉCLAIREUR (SCOUT) ====================
    private void registerScoutWeapons() {
        ClassType c = ClassType.SCOUT;

        // Tier 1 - Basique: Arbalète Silencieuse
        register(new ClassWeapon(
            "sct_crossbow", "Arbalète Silencieuse",
            "Arbalète modifiée pour des assassinats silencieux",
            c, WeaponTier.BASIC, Material.CROSSBOW, WeaponType.RANGED, "sct_crossbow",
            18, 1.2, 25, 2.0,
            WeaponEffect.STEALTH_BREAK, 100, 50, Sound.ITEM_CROSSBOW_SHOOT
        ));

        // Tier 2 - Avancée: Dagues Jumelles
        register(new ClassWeapon(
            "sct_daggers", "Dagues Jumelles",
            "Paire de dagues empoisonnées pour des combats rapprochés mortels",
            c, WeaponTier.ADVANCED, Material.IRON_SWORD, WeaponType.MELEE, "sct_daggers",
            10, 3.0, 35, 2.5,
            WeaponEffect.BLEED, 50, 8, Sound.ENTITY_PLAYER_ATTACK_SWEEP
        ));

        // Tier 3 - Légendaire: Arc du Prédateur
        register(new ClassWeapon(
            "sct_bow", "Arc du Prédateur",
            "Arc légendaire qui tire des flèches spectrales guidées",
            c, WeaponTier.LEGENDARY, Material.BOW, WeaponType.RANGED, "sct_bow",
            35, 1.5, 40, 3.0,
            WeaponEffect.MARK_TARGET, 100, 10, Sound.ENTITY_ARROW_SHOOT
        ));
    }

    // ==================== MÉDIC ====================
    private void registerMedicWeapons() {
        ClassType c = ClassType.MEDIC;

        // Tier 1 - Basique: Pistolet Seringue
        register(new ClassWeapon(
            "med_syringe", "Pistolet Seringue",
            "Tire des seringues qui soignent les alliés ou empoisonnent les ennemis",
            c, WeaponTier.BASIC, Material.BLAZE_ROD, WeaponType.RANGED, "med_syringe",
            8, 1.8, 10, 1.5,
            WeaponEffect.HEAL_PULSE, 30, 15, Sound.ENTITY_WITCH_THROW
        ));

        // Tier 2 - Avancée: Bâton de Vie
        register(new ClassWeapon(
            "med_staff", "Bâton de Vie",
            "Bâton médical qui canalise l'énergie vitale",
            c, WeaponTier.ADVANCED, Material.BLAZE_ROD, WeaponType.SPECIAL, "med_staff",
            15, 1.4, 15, 1.8,
            WeaponEffect.LIFESTEAL, 60, 30, Sound.BLOCK_BEACON_AMBIENT
        ));

        // Tier 3 - Légendaire: Défibrillateur
        register(new ClassWeapon(
            "med_defibrillator", "Défibrillateur",
            "Arme à choc électrique qui peut ressusciter ou électrocuter",
            c, WeaponTier.LEGENDARY, Material.LIGHTNING_ROD, WeaponType.SPECIAL, "med_defibrillator",
            45, 0.8, 20, 2.0,
            WeaponEffect.SHOCK, 35, 2, Sound.ENTITY_LIGHTNING_BOLT_IMPACT
        ));
    }

    // ==================== INGÉNIEUR ====================
    private void registerEngineerWeapons() {
        ClassType c = ClassType.ENGINEER;

        // Tier 1 - Basique: Pistolet Tesla
        register(new ClassWeapon(
            "eng_tesla", "Pistolet Tesla",
            "Pistolet à énergie qui enchaîne les éclairs entre les ennemis",
            c, WeaponTier.BASIC, Material.LIGHTNING_ROD, WeaponType.RANGED, "eng_tesla",
            12, 1.6, 12, 1.6,
            WeaponEffect.CHAIN, 45, 3, Sound.ENTITY_LIGHTNING_BOLT_THUNDER
        ));

        // Tier 2 - Avancée: Lance-Flammes
        register(new ClassWeapon(
            "eng_flamethrower", "Lance-Flammes",
            "Projette un cône de flammes dévastatrices",
            c, WeaponTier.ADVANCED, Material.FIRE_CHARGE, WeaponType.SPECIAL, "eng_flamethrower",
            6, 3.5, 5, 1.3,
            WeaponEffect.BURN, 80, 4, Sound.ENTITY_BLAZE_SHOOT
        ));

        // Tier 3 - Légendaire: Railgun
        register(new ClassWeapon(
            "eng_railgun", "Railgun",
            "Canon électromagnétique qui tire des projectiles à haute vélocité",
            c, WeaponTier.LEGENDARY, Material.SPYGLASS, WeaponType.HEAVY, "eng_railgun",
            90, 0.4, 30, 2.5,
            WeaponEffect.PIERCE, 100, 5, Sound.ENTITY_WITHER_SHOOT
        ));
    }

    // ==================== BERSERKER ====================
    private void registerBerserkerWeapons() {
        ClassType c = ClassType.BERSERKER;

        // Tier 1 - Basique: Masse Dévastatrice
        register(new ClassWeapon(
            "ber_mace", "Masse Dévastatrice",
            "Masse lourde qui écrase tout sur son passage",
            c, WeaponTier.BASIC, Material.MACE, WeaponType.MELEE, "ber_warhammer",
            25, 0.9, 15, 2.0,
            WeaponEffect.KNOCKBACK, 40, 3, Sound.ENTITY_IRON_GOLEM_ATTACK
        ));

        // Tier 2 - Avancée: Hache Démoniaque
        register(new ClassWeapon(
            "ber_axe", "Hache Démoniaque",
            "Hache maudite qui se nourrit du sang de ses victimes",
            c, WeaponTier.ADVANCED, Material.NETHERITE_AXE, WeaponType.MELEE, "ber_battleaxe",
            30, 1.1, 20, 2.2,
            WeaponEffect.LIFESTEAL, 100, 20, Sound.ENTITY_PLAYER_ATTACK_CRIT
        ));

        // Tier 3 - Légendaire: Poings du Titan
        register(new ClassWeapon(
            "ber_gauntlets", "Poings du Titan",
            "Gantelets antiques qui confèrent une force surhumaine",
            c, WeaponTier.LEGENDARY, Material.NETHERITE_INGOT, WeaponType.MELEE, "ber_gauntlets",
            20, 2.2, 25, 2.5,
            WeaponEffect.RAGE_BUILD, 100, 10, Sound.ENTITY_RAVAGER_ATTACK
        ));
    }

    // ==================== SNIPER ====================
    private void registerSniperWeapons() {
        ClassType c = ClassType.SNIPER;

        // Tier 1 - Basique: Carabine Silencieuse
        register(new ClassWeapon(
            "snp_rifle", "Carabine Silencieuse",
            "Fusil de précision avec silencieux intégré",
            c, WeaponTier.BASIC, Material.CROSSBOW, WeaponType.RANGED, "snp_silencer",
            25, 1.0, 30, 2.5,
            WeaponEffect.STEALTH_BREAK, 100, 75, Sound.ITEM_CROSSBOW_SHOOT
        ));

        // Tier 2 - Avancée: Fusil .50 Cal
        register(new ClassWeapon(
            "snp_50cal", "Fusil .50 Cal",
            "Fusil antimatériel capable de percer les blindages les plus lourds",
            c, WeaponTier.ADVANCED, Material.NETHERITE_INGOT, WeaponType.HEAVY, "snp_antimat",
            70, 0.5, 40, 3.0,
            WeaponEffect.PIERCE, 100, 2, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST
        ));

        // Tier 3 - Légendaire: Canon Gauss
        register(new ClassWeapon(
            "snp_gauss", "Canon Gauss",
            "Canon électromagnétique de précision qui désintègre ses cibles",
            c, WeaponTier.LEGENDARY, Material.END_CRYSTAL, WeaponType.HEAVY, "snp_railgun",
            150, 0.3, 50, 4.0,
            WeaponEffect.EXECUTE, 100, 100, Sound.ENTITY_ENDER_DRAGON_SHOOT
        ));
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient une arme par son ID
     */
    public ClassWeapon getWeapon(String id) {
        return weaponsById.get(id);
    }

    /**
     * Obtient toutes les armes d'une classe
     */
    public List<ClassWeapon> getWeaponsForClass(ClassType classType) {
        return weaponsByClass.getOrDefault(classType, Collections.emptyList());
    }

    /**
     * Obtient les armes d'un tier spécifique
     */
    public List<ClassWeapon> getWeaponsByTier(ClassType classType, WeaponTier tier) {
        return getWeaponsForClass(classType).stream()
            .filter(w -> w.getTier() == tier)
            .toList();
    }

    /**
     * Obtient les armes débloquées pour un joueur
     */
    public List<ClassWeapon> getUnlockedWeapons(ClassType classType, Set<String> unlockedTalents) {
        return getWeaponsForClass(classType).stream()
            .filter(w -> unlockedTalents.contains(w.getUnlockTalentId()))
            .toList();
    }

    /**
     * Vérifie si un joueur peut utiliser une arme
     */
    public boolean canUseWeapon(String weaponId, ClassType playerClass, Set<String> unlockedTalents) {
        ClassWeapon weapon = getWeapon(weaponId);
        if (weapon == null) return false;

        // Vérifier la classe
        if (weapon.getRequiredClass() != playerClass) return false;

        // Vérifier le talent
        return unlockedTalents.contains(weapon.getUnlockTalentId());
    }

    /**
     * Obtient toutes les armes
     */
    public Collection<ClassWeapon> getAllWeapons() {
        return weaponsById.values();
    }

    /**
     * Obtient le nombre total d'armes
     */
    public int getTotalWeaponCount() {
        return weaponsById.size();
    }
}
