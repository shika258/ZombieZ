package com.rinaorc.zombiez.classes.weapons;

import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponEffect;
import com.rinaorc.zombiez.classes.weapons.ClassWeapon.WeaponTier;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.*;

/**
 * Registre des armes de classe simplifié - 2 armes par classe
 *
 * Structure par classe:
 * - 1 arme de BASE (débloquée niveau 1)
 * - 1 arme LÉGENDAIRE (débloquée niveau 10)
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
        registerGuerrierWeapons();
        registerChasseurWeapons();
        registerOccultisteWeapons();
    }

    // ==================== GUERRIER ====================
    private void registerGuerrierWeapons() {
        ClassType c = ClassType.GUERRIER;

        // Arme de BASE - Hache de Guerre
        register(new ClassWeapon(
            "gue_waraxe", "Hache de Guerre",
            "Une hache massive qui draine la vie de vos ennemis",
            c, WeaponTier.BASE, Material.NETHERITE_AXE, 0,
            25, 1.0, 10,
            WeaponEffect.LIFESTEAL, 15,
            Sound.ENTITY_PLAYER_ATTACK_CRIT
        ));

        // Arme LÉGENDAIRE - Lame du Titan
        register(new ClassWeapon(
            "gue_titan", "Lame du Titan",
            "L'épée légendaire d'un ancien titan, capable de trancher plusieurs ennemis",
            c, WeaponTier.LEGENDARY, Material.NETHERITE_SWORD, 10,
            40, 1.2, 15,
            WeaponEffect.CLEAVE, 3,
            Sound.ENTITY_IRON_GOLEM_ATTACK
        ));
    }

    // ==================== CHASSEUR ====================
    private void registerChasseurWeapons() {
        ClassType c = ClassType.CHASSEUR;

        // Arme de BASE - Arc du Prédateur
        register(new ClassWeapon(
            "cha_predator", "Arc du Prédateur",
            "Un arc de précision qui perfore ses cibles",
            c, WeaponTier.BASE, Material.BOW, 0,
            20, 1.5, 25,
            WeaponEffect.PIERCE, 2,
            Sound.ENTITY_ARROW_SHOOT
        ));

        // Arme LÉGENDAIRE - Arbalète de l'Ombre
        register(new ClassWeapon(
            "cha_shadow", "Arbalète de l'Ombre",
            "Une arbalète enchantée qui amplifie vos coups critiques",
            c, WeaponTier.LEGENDARY, Material.CROSSBOW, 10,
            35, 0.8, 40,
            WeaponEffect.CRIT_BOOST, 50,
            Sound.ITEM_CROSSBOW_SHOOT
        ));
    }

    // ==================== OCCULTISTE ====================
    private void registerOccultisteWeapons() {
        ClassType c = ClassType.OCCULTISTE;

        // Arme de BASE - Bâton Arcanique
        register(new ClassWeapon(
            "occ_staff", "Bâton Arcanique",
            "Un bâton magique qui crée des explosions à l'impact",
            c, WeaponTier.BASE, Material.BLAZE_ROD, 0,
            15, 1.4, 15,
            WeaponEffect.AOE_BLAST, 30,
            Sound.ENTITY_ILLUSIONER_CAST_SPELL
        ));

        // Arme LÉGENDAIRE - Faux de l'Âme
        register(new ClassWeapon(
            "occ_scythe", "Faux de l'Âme",
            "Une faux démoniaque qui restaure votre énergie avec chaque coup",
            c, WeaponTier.LEGENDARY, Material.NETHERITE_HOE, 10,
            30, 1.1, 20,
            WeaponEffect.MANA_DRAIN, 15,
            Sound.ENTITY_WITHER_SHOOT
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
     * Obtient les armes débloquées pour un niveau de classe
     */
    public List<ClassWeapon> getUnlockedWeapons(ClassType classType, int classLevel) {
        return getWeaponsForClass(classType).stream()
            .filter(w -> w.isUnlocked(classLevel))
            .toList();
    }

    /**
     * Obtient l'arme de base d'une classe
     */
    public ClassWeapon getBaseWeapon(ClassType classType) {
        return getWeaponsForClass(classType).stream()
            .filter(w -> w.getTier() == WeaponTier.BASE)
            .findFirst()
            .orElse(null);
    }

    /**
     * Obtient l'arme légendaire d'une classe
     */
    public ClassWeapon getLegendaryWeapon(ClassType classType) {
        return getWeaponsForClass(classType).stream()
            .filter(w -> w.getTier() == WeaponTier.LEGENDARY)
            .findFirst()
            .orElse(null);
    }

    /**
     * Vérifie si un joueur peut utiliser une arme
     */
    public boolean canUseWeapon(String weaponId, ClassType playerClass, int classLevel) {
        ClassWeapon weapon = getWeapon(weaponId);
        if (weapon == null) return false;
        if (weapon.getRequiredClass() != playerClass) return false;
        return weapon.isUnlocked(classLevel);
    }

    /**
     * Obtient toutes les armes
     */
    public Collection<ClassWeapon> getAllWeapons() {
        return weaponsById.values();
    }
}
