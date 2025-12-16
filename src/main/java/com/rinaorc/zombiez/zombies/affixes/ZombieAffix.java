package com.rinaorc.zombiez.zombies.affixes;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Affix pour les zombies - Modificateur qui rend un zombie plus dangereux
 */
@Getter
@Builder
public class ZombieAffix {

    private final String id;
    private final String prefix;         // Préfixe affiché devant le nom
    private final String colorCode;      // Code couleur
    private final int minZone;           // Zone minimum pour spawn
    private final int weight;            // Poids pour le tirage aléatoire
    
    // Modificateurs
    private final double healthMultiplier;      // Multiplicateur de vie
    private final double damageMultiplier;      // Multiplicateur de dégâts
    private final double speedMultiplier;       // Multiplicateur de vitesse
    private final double armorBonus;            // Bonus d'armure
    
    // Récompenses
    private final double rewardMultiplier;      // Multiplicateur de récompenses
    private final double lootBonus;             // Bonus de loot
    
    // Effets spéciaux
    private final List<PotionEffect> selfEffects;      // Effets sur le zombie
    private final List<PotionEffect> attackEffects;    // Effets sur les attaques
    private final String specialAbility;               // Ability spéciale

    /**
     * Applique l'affix à une entité
     */
    public void apply(LivingEntity entity) {
        // Modifier la vie
        if (healthMultiplier != 1.0) {
            AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) {
                double newMax = health.getBaseValue() * healthMultiplier;
                health.setBaseValue(newMax);
                entity.setHealth(newMax);
            }
        }
        
        // Modifier les dégâts
        if (damageMultiplier != 1.0) {
            AttributeInstance damage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(damage.getBaseValue() * damageMultiplier);
            }
        }
        
        // Modifier la vitesse
        if (speedMultiplier != 1.0) {
            AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(speed.getBaseValue() * speedMultiplier);
            }
        }
        
        // Ajouter l'armure
        if (armorBonus > 0) {
            AttributeInstance armor = entity.getAttribute(Attribute.ARMOR);
            if (armor != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("zombiez", "affix_armor");
                armor.addModifier(new AttributeModifier(
                    key,
                    armorBonus,
                    AttributeModifier.Operation.ADD_NUMBER
                ));
            }
        }
        
        // Appliquer les effets
        if (selfEffects != null) {
            for (PotionEffect effect : selfEffects) {
                entity.addPotionEffect(effect);
            }
        }
    }

    /**
     * Applique les effets d'attaque sur une cible
     */
    public void applyAttackEffects(LivingEntity target) {
        if (attackEffects != null) {
            for (PotionEffect effect : attackEffects) {
                target.addPotionEffect(effect);
            }
        }
    }

    /**
     * Registre des affixes de zombies
     */
    public static class ZombieAffixRegistry {
        private static ZombieAffixRegistry instance;
        private final Map<String, ZombieAffix> affixes = new HashMap<>();
        private final List<ZombieAffix> affixList = new ArrayList<>();

        private ZombieAffixRegistry() {
            initializeAffixes();
        }

        public static ZombieAffixRegistry getInstance() {
            if (instance == null) {
                synchronized (ZombieAffixRegistry.class) {
                    if (instance == null) {
                        instance = new ZombieAffixRegistry();
                    }
                }
            }
            return instance;
        }

        private void initializeAffixes() {
            // ==================== AFFIXES DE PUISSANCE ====================
            
            registerAffix(ZombieAffix.builder()
                .id("swift")
                .prefix("Rapide")
                .colorCode("§a")
                .minZone(4)
                .weight(100)
                .healthMultiplier(0.9)
                .damageMultiplier(1.0)
                .speedMultiplier(1.4)
                .armorBonus(0)
                .rewardMultiplier(1.2)
                .lootBonus(0.05)
                .build());

            registerAffix(ZombieAffix.builder()
                .id("tough")
                .prefix("Résistant")
                .colorCode("§7")
                .minZone(4)
                .weight(100)
                .healthMultiplier(1.5)
                .damageMultiplier(1.0)
                .speedMultiplier(0.9)
                .armorBonus(4)
                .rewardMultiplier(1.3)
                .lootBonus(0.1)
                .build());

            registerAffix(ZombieAffix.builder()
                .id("aggressive")
                .prefix("Agressif")
                .colorCode("§c")
                .minZone(5)
                .weight(80)
                .healthMultiplier(1.0)
                .damageMultiplier(1.5)
                .speedMultiplier(1.1)
                .armorBonus(0)
                .rewardMultiplier(1.4)
                .lootBonus(0.1)
                .build());

            registerAffix(ZombieAffix.builder()
                .id("regenerating")
                .prefix("Régénérant")
                .colorCode("§d")
                .minZone(6)
                .weight(60)
                .healthMultiplier(1.2)
                .damageMultiplier(1.0)
                .speedMultiplier(1.0)
                .armorBonus(0)
                .rewardMultiplier(1.5)
                .lootBonus(0.15)
                .selfEffects(List.of(
                    new PotionEffect(PotionEffectType.REGENERATION, 9999, 0, true, false)
                ))
                .build());

            // ==================== AFFIXES ÉLÉMENTAIRES ====================

            registerAffix(ZombieAffix.builder()
                .id("burning")
                .prefix("Brûlant")
                .colorCode("§6")
                .minZone(5)
                .weight(70)
                .healthMultiplier(1.1)
                .damageMultiplier(1.2)
                .speedMultiplier(1.0)
                .armorBonus(0)
                .rewardMultiplier(1.4)
                .lootBonus(0.1)
                .selfEffects(List.of(
                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 9999, 0, true, false)
                ))
                .attackEffects(List.of(
                    new PotionEffect(PotionEffectType.SLOWNESS, 40, 0)
                ))
                .specialAbility("ignite_on_hit")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("frozen")
                .prefix("Gelé")
                .colorCode("§b")
                .minZone(6)
                .weight(70)
                .healthMultiplier(1.2)
                .damageMultiplier(1.1)
                .speedMultiplier(0.8)
                .armorBonus(2)
                .rewardMultiplier(1.4)
                .lootBonus(0.1)
                .attackEffects(List.of(
                    new PotionEffect(PotionEffectType.SLOWNESS, 60, 1)
                ))
                .specialAbility("freeze_on_hit")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("electric")
                .prefix("Électrique")
                .colorCode("§e")
                .minZone(7)
                .weight(50)
                .healthMultiplier(1.0)
                .damageMultiplier(1.3)
                .speedMultiplier(1.2)
                .armorBonus(0)
                .rewardMultiplier(1.5)
                .lootBonus(0.15)
                .attackEffects(List.of(
                    new PotionEffect(PotionEffectType.SLOWNESS, 20, 2)
                ))
                .specialAbility("chain_lightning")
                .build());

            // ==================== AFFIXES RARES ====================

            registerAffix(ZombieAffix.builder()
                .id("vampiric")
                .prefix("Vampirique")
                .colorCode("§4")
                .minZone(7)
                .weight(40)
                .healthMultiplier(1.3)
                .damageMultiplier(1.4)
                .speedMultiplier(1.1)
                .armorBonus(2)
                .rewardMultiplier(1.8)
                .lootBonus(0.2)
                .specialAbility("lifesteal")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("mirror")
                .prefix("Miroir")
                .colorCode("§f")
                .minZone(8)
                .weight(30)
                .healthMultiplier(1.2)
                .damageMultiplier(1.0)
                .speedMultiplier(1.0)
                .armorBonus(6)
                .rewardMultiplier(2.0)
                .lootBonus(0.25)
                .selfEffects(List.of(
                    new PotionEffect(PotionEffectType.RESISTANCE, 9999, 0, true, false)
                ))
                .specialAbility("damage_reflect")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("immortal")
                .prefix("Immortel")
                .colorCode("§5")
                .minZone(9)
                .weight(20)
                .healthMultiplier(2.0)
                .damageMultiplier(1.2)
                .speedMultiplier(0.9)
                .armorBonus(8)
                .rewardMultiplier(2.5)
                .lootBonus(0.3)
                .selfEffects(List.of(
                    new PotionEffect(PotionEffectType.REGENERATION, 9999, 1, true, false),
                    new PotionEffect(PotionEffectType.RESISTANCE, 9999, 0, true, false)
                ))
                .specialAbility("death_prevention")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("arcane")
                .prefix("Arcanique")
                .colorCode("§d")
                .minZone(9)
                .weight(25)
                .healthMultiplier(1.4)
                .damageMultiplier(1.6)
                .speedMultiplier(1.0)
                .armorBonus(4)
                .rewardMultiplier(2.2)
                .lootBonus(0.25)
                .attackEffects(List.of(
                    new PotionEffect(PotionEffectType.SLOWNESS, 60, 0),
                    new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 0)
                ))
                .specialAbility("mana_drain")
                .build());

            registerAffix(ZombieAffix.builder()
                .id("corrupting")
                .prefix("Corrupteur")
                .colorCode("§0")
                .minZone(10)
                .weight(15)
                .healthMultiplier(1.5)
                .damageMultiplier(1.5)
                .speedMultiplier(1.1)
                .armorBonus(5)
                .rewardMultiplier(3.0)
                .lootBonus(0.35)
                .attackEffects(List.of(
                    new PotionEffect(PotionEffectType.WITHER, 60, 0),
                    new PotionEffect(PotionEffectType.DARKNESS, 80, 0)
                ))
                .specialAbility("corruption_spread")
                .build());
        }

        private void registerAffix(ZombieAffix affix) {
            affixes.put(affix.getId(), affix);
            affixList.add(affix);
        }

        public ZombieAffix getAffix(String id) {
            return affixes.get(id);
        }

        public Collection<ZombieAffix> getAllAffixes() {
            return Collections.unmodifiableCollection(affixes.values());
        }

        /**
         * Tire un affix au sort pour une zone donnée
         */
        public ZombieAffix rollAffix(int zoneId) {
            List<ZombieAffix> validAffixes = affixList.stream()
                .filter(a -> a.getMinZone() <= zoneId)
                .toList();
            
            if (validAffixes.isEmpty()) {
                return null;
            }
            
            int totalWeight = validAffixes.stream().mapToInt(ZombieAffix::getWeight).sum();
            int roll = (int) (Math.random() * totalWeight);
            int cumulative = 0;
            
            for (ZombieAffix affix : validAffixes) {
                cumulative += affix.getWeight();
                if (roll < cumulative) {
                    return affix;
                }
            }
            
            return validAffixes.get(0);
        }

        public int getAffixCount() {
            return affixes.size();
        }
    }
}
