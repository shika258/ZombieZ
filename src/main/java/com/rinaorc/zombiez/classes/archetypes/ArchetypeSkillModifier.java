package com.rinaorc.zombiez.classes.archetypes;

import com.rinaorc.zombiez.classes.skills.ActiveSkill;
import lombok.Getter;

/**
 * Modificateurs de compétences basés sur l'archétype actif
 *
 * PHILOSOPHIE:
 * - Les skills de base restent les mêmes, mais leur comportement change
 * - Un même skill peut avoir des effets bonus différents selon l'archétype
 * - Encourage le joueur à synergiser archétype + skills
 *
 * EXEMPLES:
 * - Charge Brutale + Tornade = laisse une traînée de dégâts
 * - Charge Brutale + Boucher = applique une marque sur la cible finale
 * - Tir Rapide + Gatling = projectiles supplémentaires si en mouvement
 */
public class ArchetypeSkillModifier {

    /**
     * Obtient les modificateurs pour un skill selon l'archétype
     */
    public static SkillModification getModification(String skillId, BuildArchetype archetype) {
        if (archetype == null || archetype == BuildArchetype.NONE) {
            return SkillModification.NONE;
        }

        return switch (archetype) {
            // ==================== GUERRIER ====================
            case GUERRIER_TORNADE -> getTonadoModification(skillId);
            case GUERRIER_MUR -> getMurModification(skillId);
            case GUERRIER_BOUCHER -> getBoucherModification(skillId);

            // ==================== CHASSEUR ====================
            case CHASSEUR_GATLING -> getGatlingModification(skillId);
            case CHASSEUR_FANTOME -> getFantomeModification(skillId);
            case CHASSEUR_PIEGEUR -> getPiegeurModification(skillId);

            // ==================== OCCULTISTE ====================
            case OCCULTISTE_DEFLAGRATION -> getDeflagrationModification(skillId);
            case OCCULTISTE_SANG -> getSangModification(skillId);
            case OCCULTISTE_ARCHIMAGE -> getArchimageModification(skillId);

            default -> SkillModification.NONE;
        };
    }

    // ==================== GUERRIER: TORNADE ====================
    // Focus: AoE en mouvement, effets persistants
    private static SkillModification getTonadoModification(String skillId) {
        return switch (skillId) {
            case "gue_charge" -> new SkillModification(
                "Charge Tourbillonnante",
                "Laisse une traînée de dégâts (20% dégâts de base) sur 2s",
                1.0, 0.8, 1.2,  // damageMod, cooldownMod, aoeRadiusMod
                SkillBonusEffect.TRAIL_DAMAGE, 0.20
            );
            case "gue_warcry" -> new SkillModification(
                "Cri du Cyclone",
                "Affecte aussi les alliés proches (+15% dégâts)",
                1.0, 1.0, 2.0,  // Double le rayon d'effet
                SkillBonusEffect.ALLY_BUFF, 0.15
            );
            case "gue_slam" -> new SkillModification(
                "Impact Rotatif",
                "+30% rayon, peut être lancé en mouvement",
                0.9, 0.9, 1.3,  // Moins de dégâts, plus de rayon
                SkillBonusEffect.CAST_WHILE_MOVING, 0
            );
            case "gue_rage" -> new SkillModification(
                "Rage Tourbillonnante",
                "Pendant la rage, dégâts AoE passifs autour de vous",
                1.0, 1.0, 1.0,
                SkillBonusEffect.AURA_DAMAGE, 15  // 15 dégâts/tick
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== GUERRIER: MUR VIVANT ====================
    // Focus: Tank, contrôle, protection
    private static SkillModification getMurModification(String skillId) {
        return switch (skillId) {
            case "gue_charge" -> new SkillModification(
                "Charge du Bastion",
                "Repousse les ennemis au lieu de traverser, +50% knockback",
                0.7, 1.2, 1.0,  // Moins de dégâts, plus de CD
                SkillBonusEffect.KNOCKBACK, 1.5
            );
            case "gue_warcry" -> new SkillModification(
                "Cri Protecteur",
                "Donne un bouclier (20% HP max) au lieu du buff dégâts",
                1.0, 0.8, 1.5,
                SkillBonusEffect.SHIELD, 0.20
            );
            case "gue_slam" -> new SkillModification(
                "Tremblements",
                "Ralentit les ennemis de 40% pendant 3s, -20% dégâts",
                0.8, 1.0, 1.0,
                SkillBonusEffect.SLOW, 0.40
            );
            case "gue_rage" -> new SkillModification(
                "Rage du Rempart",
                "90% réduction de dégâts mais immobile, taunt tous les ennemis",
                1.0, 1.2, 1.0,
                SkillBonusEffect.TAUNT, 0.90  // DR augmentée
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== GUERRIER: BOUCHER ====================
    // Focus: Burst single-target, exécution
    private static SkillModification getBoucherModification(String skillId) {
        return switch (skillId) {
            case "gue_charge" -> new SkillModification(
                "Charge Marquante",
                "La cible finale est 'Marquée' (+30% dégâts reçus, 5s)",
                1.2, 1.1, 0.5,  // Plus de dégâts, rayon réduit (single target)
                SkillBonusEffect.MARK_TARGET, 0.30
            );
            case "gue_warcry" -> new SkillModification(
                "Cri du Prédateur",
                "Révèle les ennemis blessés (<50% HP), +40% dégâts contre eux",
                1.0, 1.0, 1.0,
                SkillBonusEffect.EXECUTE_BONUS, 0.40
            );
            case "gue_slam" -> new SkillModification(
                "Coup de Grâce",
                "Single-target: +80% dégâts, exécute si <20% HP",
                1.8, 1.3, 0.3,  // Énorme boost mais single target
                SkillBonusEffect.EXECUTE, 0.20  // Seuil d'exécution
            );
            case "gue_rage" -> new SkillModification(
                "Rage Sanglante",
                "Chaque kill pendant la rage prolonge de 1s (max +3s)",
                1.2, 1.0, 1.0,
                SkillBonusEffect.KILL_EXTEND, 3.0  // Max extension
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== CHASSEUR: GATLING ====================
    // Focus: DPS soutenu, tir rapide
    private static SkillModification getGatlingModification(String skillId) {
        return switch (skillId) {
            case "cha_multishot" -> new SkillModification(
                "Rafale Gatling",
                "5 flèches au lieu de 3, -20% dégâts par flèche",
                0.8, 0.7, 1.0,  // Plus de projectiles, moins de CD
                SkillBonusEffect.EXTRA_PROJECTILES, 2
            );
            case "cha_roll" -> new SkillModification(
                "Roulade Offensive",
                "Tire automatiquement 2 flèches pendant la roulade",
                1.0, 0.9, 1.0,
                SkillBonusEffect.AUTO_ATTACK, 2
            );
            case "cha_trap" -> new SkillModification(
                "Mine à Fragmentation",
                "L'explosion crée 3 mini-pièges qui durent 5s",
                0.7, 1.2, 0.8,
                SkillBonusEffect.SPAWN_TRAPS, 3
            );
            case "cha_deadeye" -> new SkillModification(
                "Tir en Rafale Précis",
                "Tire 3 tirs successifs rapides au lieu d'un gros",
                0.5, 0.8, 1.0,  // Chaque tir fait 50%
                SkillBonusEffect.MULTI_CAST, 3
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== CHASSEUR: FANTOME ====================
    // Focus: Burst, invisibilité, headshots
    private static SkillModification getFantomeModification(String skillId) {
        return switch (skillId) {
            case "cha_multishot" -> new SkillModification(
                "Tir Silencieux",
                "Tire 1 flèche mais ne brise pas l'invisibilité",
                1.5, 1.3, 1.0,
                SkillBonusEffect.STEALTH_ATTACK, 0
            );
            case "cha_roll" -> new SkillModification(
                "Pas de l'Ombre",
                "Devient invisible 2s après la roulade",
                1.0, 1.2, 1.0,
                SkillBonusEffect.GRANT_STEALTH, 2.0
            );
            case "cha_trap" -> new SkillModification(
                "Piège Fumigène",
                "Crée un nuage d'invisibilité au lieu de dégâts (5s)",
                0.3, 1.0, 1.5,
                SkillBonusEffect.SMOKE_CLOUD, 5.0
            );
            case "cha_deadeye" -> new SkillModification(
                "Tir de l'Assassin",
                "+100% dégâts depuis l'invisibilité, reset invisibilité si kill",
                2.0, 1.0, 1.0,
                SkillBonusEffect.STEALTH_BONUS, 1.0
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== CHASSEUR: PIEGEUR ====================
    // Focus: Contrôle de zone, pièges
    private static SkillModification getPiegeurModification(String skillId) {
        return switch (skillId) {
            case "cha_multishot" -> new SkillModification(
                "Tir Dispersant",
                "Les flèches repoussent les ennemis vers vos pièges",
                0.8, 1.0, 1.0,
                SkillBonusEffect.KNOCKBACK, 0.5
            );
            case "cha_roll" -> new SkillModification(
                "Roulade Piégée",
                "Pose un mini-piège au point de départ",
                1.0, 1.0, 1.0,
                SkillBonusEffect.DROP_TRAP, 0.5  // 50% dégâts d'un piège normal
            );
            case "cha_trap" -> new SkillModification(
                "Champ de Mines",
                "Pose 3 pièges en triangle au lieu d'un, -40% dégâts chacun",
                0.6, 0.8, 1.0,
                SkillBonusEffect.MULTI_TRAP, 3
            );
            case "cha_deadeye" -> new SkillModification(
                "Tir Détonnant",
                "Le tir fait exploser tous vos pièges instantanément (+50%)",
                1.0, 1.0, 1.0,
                SkillBonusEffect.DETONATE_TRAPS, 1.5
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== OCCULTISTE: DEFLAGRATION ====================
    // Focus: Stack & explode, AoE massif
    private static SkillModification getDeflagrationModification(String skillId) {
        return switch (skillId) {
            case "occ_orb" -> new SkillModification(
                "Orbe Instable",
                "Applique 'Corruption' (stack 3x), explose à 3 stacks (+100% dégâts)",
                0.6, 0.8, 1.0,
                SkillBonusEffect.STACK_DEBUFF, 3
            );
            case "occ_drain" -> new SkillModification(
                "Drain Explosif",
                "Si la cible a Corruption, fait exploser (+50% AoE)",
                1.0, 1.0, 1.0,
                SkillBonusEffect.DETONATE_DEBUFF, 1.5
            );
            case "occ_nova" -> new SkillModification(
                "Nova Cataclysmique",
                "+50% rayon, ennemis avec Corruption subissent +30% dégâts",
                1.0, 1.2, 1.5,
                SkillBonusEffect.CORRUPTION_BONUS, 0.30
            );
            case "occ_apocalypse" -> new SkillModification(
                "Apocalypse",
                "Applique Corruption à tous, explose en chaîne à la fin",
                1.3, 1.0, 1.2,
                SkillBonusEffect.CHAIN_EXPLOSION, 0.5  // 50% dégâts de base par explosion
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== OCCULTISTE: MAGE DE SANG ====================
    // Focus: Drain de vie, risque/récompense
    private static SkillModification getSangModification(String skillId) {
        return switch (skillId) {
            case "occ_orb" -> new SkillModification(
                "Orbe de Sang",
                "Coûte 10% HP au lieu de mana, +50% dégâts, soigne 20%",
                1.5, 1.0, 1.0,
                SkillBonusEffect.HEALTH_COST, 0.10
            );
            case "occ_drain" -> new SkillModification(
                "Transfusion",
                "Heal augmenté à 60%, crée un lien qui draine sur la durée",
                0.8, 0.9, 1.0,
                SkillBonusEffect.LIFESTEAL_BOOST, 0.60
            );
            case "occ_nova" -> new SkillModification(
                "Nova Vampirique",
                "Soigne de 5% HP max par ennemi touché (max 50%)",
                0.8, 1.0, 1.0,
                SkillBonusEffect.AOE_LIFESTEAL, 0.05
            );
            case "occ_apocalypse" -> new SkillModification(
                "Pact de Sang",
                "Sacrifie 30% HP, mais x2 dégâts et full heal si tue 5+ ennemis",
                2.0, 1.0, 1.0,
                SkillBonusEffect.BLOOD_PACT, 0.30
            );
            default -> SkillModification.NONE;
        };
    }

    // ==================== OCCULTISTE: ARCHIMAGE ====================
    // Focus: Combos, efficacité, polyvalence
    private static SkillModification getArchimageModification(String skillId) {
        return switch (skillId) {
            case "occ_orb" -> new SkillModification(
                "Orbe Primordial",
                "Après un Drain, le prochain Orbe fait +40% et coûte -50%",
                1.0, 1.0, 1.0,
                SkillBonusEffect.COMBO_BONUS, 0.40
            );
            case "occ_drain" -> new SkillModification(
                "Siphon Arcanique",
                "Restaure 30 énergie en plus du heal, reset CD Orbe",
                1.0, 1.0, 1.0,
                SkillBonusEffect.ENERGY_REFUND, 30
            );
            case "occ_nova" -> new SkillModification(
                "Nova Canalisée",
                "Peut être maintenue: +10% dégâts et rayon par seconde (max 3s)",
                1.0, 1.3, 1.0,
                SkillBonusEffect.CHANNEL, 3.0
            );
            case "occ_apocalypse" -> new SkillModification(
                "Maîtrise Arcanique",
                "Pendant la tempête, tous les autres skills -50% CD et coût",
                0.9, 0.9, 1.0,
                SkillBonusEffect.SKILL_AMPLIFY, 0.50
            );
            default -> SkillModification.NONE;
        };
    }

    /**
     * Types d'effets bonus que les modifications peuvent ajouter
     */
    @Getter
    public enum SkillBonusEffect {
        NONE("Aucun"),

        // Effets de dégâts
        TRAIL_DAMAGE("Traînée de dégâts"),
        AURA_DAMAGE("Dégâts d'aura"),
        EXECUTE("Exécution"),
        EXECUTE_BONUS("Bonus exécution"),
        CHAIN_EXPLOSION("Explosion en chaîne"),

        // Effets de contrôle
        KNOCKBACK("Recul"),
        SLOW("Ralentissement"),
        TAUNT("Provocation"),

        // Effets de buff/debuff
        ALLY_BUFF("Buff allié"),
        SHIELD("Bouclier"),
        MARK_TARGET("Marque la cible"),
        STACK_DEBUFF("Debuff empilable"),
        DETONATE_DEBUFF("Détone debuff"),
        CORRUPTION_BONUS("Bonus corruption"),

        // Effets de mobilité/utilité
        CAST_WHILE_MOVING("Lancer en mouvement"),
        KILL_EXTEND("Extension sur kill"),

        // Effets de projectiles
        EXTRA_PROJECTILES("Projectiles supplémentaires"),
        AUTO_ATTACK("Attaque automatique"),
        MULTI_CAST("Cast multiple"),

        // Effets de furtivité
        STEALTH_ATTACK("Attaque furtive"),
        GRANT_STEALTH("Donne invisibilité"),
        SMOKE_CLOUD("Nuage de fumée"),
        STEALTH_BONUS("Bonus furtivité"),

        // Effets de pièges
        SPAWN_TRAPS("Génère des pièges"),
        DROP_TRAP("Dépose un piège"),
        MULTI_TRAP("Pièges multiples"),
        DETONATE_TRAPS("Détone les pièges"),

        // Effets de vie
        HEALTH_COST("Coût en vie"),
        LIFESTEAL_BOOST("Boost vol de vie"),
        AOE_LIFESTEAL("Vol de vie en zone"),
        BLOOD_PACT("Pacte de sang"),

        // Effets de combo/énergie
        COMBO_BONUS("Bonus de combo"),
        ENERGY_REFUND("Remboursement énergie"),
        CHANNEL("Canalisation"),
        SKILL_AMPLIFY("Amplification de skills");

        private final String displayName;

        SkillBonusEffect(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Représente une modification de compétence par archétype
     */
    @Getter
    public static class SkillModification {
        public static final SkillModification NONE = new SkillModification(
            null, null, 1.0, 1.0, 1.0, SkillBonusEffect.NONE, 0
        );

        private final String modifiedName;        // Nouveau nom du skill
        private final String bonusDescription;    // Description du bonus
        private final double damageModifier;      // Multiplicateur de dégâts
        private final double cooldownModifier;    // Multiplicateur de CD
        private final double aoeRadiusModifier;   // Multiplicateur de rayon
        private final SkillBonusEffect bonusEffect;  // Effet spécial
        private final double bonusValue;          // Valeur de l'effet

        public SkillModification(String modifiedName, String bonusDescription,
                                double damageModifier, double cooldownModifier,
                                double aoeRadiusModifier,
                                SkillBonusEffect bonusEffect, double bonusValue) {
            this.modifiedName = modifiedName;
            this.bonusDescription = bonusDescription;
            this.damageModifier = damageModifier;
            this.cooldownModifier = cooldownModifier;
            this.aoeRadiusModifier = aoeRadiusModifier;
            this.bonusEffect = bonusEffect;
            this.bonusValue = bonusValue;
        }

        public boolean hasModification() {
            return this != NONE && modifiedName != null;
        }

        /**
         * Applique les modificateurs aux stats du skill
         */
        public double applyDamageModifier(double baseDamage) {
            return baseDamage * damageModifier;
        }

        public double applyCooldownModifier(double baseCooldown) {
            return baseCooldown * cooldownModifier;
        }

        public double applyRadiusModifier(double baseRadius) {
            return baseRadius * aoeRadiusModifier;
        }

        /**
         * Génère le lore de la modification pour l'affichage
         */
        public String[] getLoreLines(String archetypeColor) {
            if (!hasModification()) {
                return new String[0];
            }

            return new String[]{
                "",
                archetypeColor + "§l» " + modifiedName,
                "§7" + bonusDescription
            };
        }
    }
}
