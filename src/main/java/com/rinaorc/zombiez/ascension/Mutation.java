package com.rinaorc.zombiez.ascension;

import com.rinaorc.zombiez.items.types.StatType;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Catalogue complet des 45 mutations du système d'Ascension
 * Chaque mutation appartient à une souche et un tier
 */
@Getter
public enum Mutation {

    // ==================== SOUCHE CARNAGE (15 mutations) ====================

    // Tier 1 (Stades I-II)
    GRIFFES_ACEREES(
        "Griffes Acérées",
        "Tes coups de mêlée sont plus puissants",
        MutationStrain.CARNAGE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.DAMAGE_PERCENT, 12.0)
    ),

    MORSURE_VAMPIRE(
        "Morsure Vampire",
        "Récupère de la vie en infligeant des dégâts",
        MutationStrain.CARNAGE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.LIFESTEAL, 8.0)
    ),

    ADRENALINE(
        "Adrénaline",
        "Tes attaques sont plus rapides",
        MutationStrain.CARNAGE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.ATTACK_SPEED_PERCENT, 10.0)
    ),

    // Tier 2 (Stades III-IV)
    RAGE_INFECTEE(
        "Rage Infectée",
        "Plus tu es blessé, plus tu frappes fort",
        MutationStrain.CARNAGE,
        MutationTier.TIER_2,
        MutationEffect.CONDITIONAL_DAMAGE,
        Map.of() // +20% damage quand < 50% HP (géré dans le code)
    ),

    ECLATS_DOS(
        "Éclats d'Os",
        "Les ennemis tués peuvent exploser",
        MutationStrain.CARNAGE,
        MutationTier.TIER_2,
        MutationEffect.ON_KILL_EXPLOSION,
        Map.of() // 15% chance explosion AoE
    ),

    SOIF_INSATIABLE(
        "Soif Insatiable",
        "Le lifesteal augmente avec les kills",
        MutationStrain.CARNAGE,
        MutationTier.TIER_2,
        MutationEffect.STACKING_LIFESTEAL,
        Map.of() // +5% lifesteal/kill, max +25%, reset 10s
    ),

    FRAPPE_BRUTALE(
        "Frappe Brutale",
        "Chaque 5ème coup est dévastateur",
        MutationStrain.CARNAGE,
        MutationTier.TIER_2,
        MutationEffect.NTH_HIT_BONUS,
        Map.of() // Chaque 5ème hit = +50% dégâts
    ),

    // Tier 3 (Stades V-VI)
    EVISCERATION(
        "Éviscération",
        "Les critiques font saigner les ennemis",
        MutationStrain.CARNAGE,
        MutationTier.TIER_3,
        MutationEffect.CRIT_BLEED,
        Map.of() // Crits = saignement 3s, 2% HP/s
    ),

    FUREUR_DERNIER_SOUFFLE(
        "Fureur du Dernier Souffle",
        "Au bord de la mort, tu deviens mortel",
        MutationStrain.CARNAGE,
        MutationTier.TIER_3,
        MutationEffect.LOW_HP_BOOST,
        Map.of() // <20% HP: +40% Dmg, +20% Speed
    ),

    CASCADE_SANGLANTE(
        "Cascade Sanglante",
        "Chaque kill renforce le prochain coup",
        MutationStrain.CARNAGE,
        MutationTier.TIER_3,
        MutationEffect.KILL_STACK_DAMAGE,
        Map.of() // Kill = +25% next hit, stack 3x
    ),

    FLEAU_AMBULANT(
        "Fléau Ambulant",
        "Les ennemis proches souffrent",
        MutationStrain.CARNAGE,
        MutationTier.TIER_3,
        MutationEffect.DAMAGE_AURA,
        Map.of() // Mobs <5 blocs: 1% HP/s
    ),

    // Tier 4-5 (Stades VII-X)
    EXECUTEUR(
        "Exécuteur",
        "Achève les ennemis affaiblis",
        MutationStrain.CARNAGE,
        MutationTier.TIER_4,
        MutationEffect.EXECUTE,
        Map.of() // <10% HP = instakill (équilibré, cooldown 3s)
    ),

    RECOLTE_DE_SANG(
        "Récolte de Sang",
        "Chaque kill restaure ta santé",
        MutationStrain.CARNAGE,
        MutationTier.TIER_4,
        MutationEffect.HEAL_ON_KILL,
        Map.of() // Kill = +2% HP (cap 5%/s)
    ),

    BERSERKER_ULTIME(
        "Berserker Ultime",
        "Puissance maximale, défense minimale",
        MutationStrain.CARNAGE,
        MutationTier.TIER_5,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.DAMAGE_PERCENT, 30.0, StatType.DAMAGE_REDUCTION, -25.0) // Équilibré: +30% dmg, -25% DR
    ),

    NOVA_MORTELLE(
        "Nova Mortelle",
        "Une explosion dévastatrice tous les 25 kills",
        MutationStrain.CARNAGE,
        MutationTier.TIER_5,
        MutationEffect.KILL_COUNTER_EXPLOSION,
        Map.of() // Toutes les 25 kills = explosion 8 blocs
    ),

    // ==================== SOUCHE SPECTRE (15 mutations) ====================

    // Tier 1 (Stades I-II)
    PAS_LEGERS(
        "Pas Légers",
        "Tu te déplaces plus rapidement",
        MutationStrain.SPECTRE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.MOVEMENT_SPEED, 10.0)
    ),

    OEIL_DU_CHASSEUR(
        "Œil du Chasseur",
        "Tes coups critiques sont plus fréquents",
        MutationStrain.SPECTRE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.CRIT_CHANCE, 10.0)
    ),

    REFLEXES(
        "Réflexes",
        "Tu esquives parfois les attaques",
        MutationStrain.SPECTRE,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.DODGE_CHANCE, 8.0)
    ),

    // Tier 2 (Stades III-IV)
    FRAPPE_FANTOME(
        "Frappe Fantôme",
        "Tes critiques ignorent l'armure",
        MutationStrain.SPECTRE,
        MutationTier.TIER_2,
        MutationEffect.CRIT_ARMOR_PEN,
        Map.of() // Crits ignorent 25% armure
    ),

    TRAQUE(
        "Traque",
        "Plus de dégâts aux ennemis qui t'ont touché",
        MutationStrain.SPECTRE,
        MutationTier.TIER_2,
        MutationEffect.REVENGE_DAMAGE,
        Map.of() // +15% Dmg aux ennemis qui t'ont hit
    ),

    EVANESCENCE(
        "Évanescence",
        "Les kills réduisent l'attention des ennemis",
        MutationStrain.SPECTRE,
        MutationTier.TIER_2,
        MutationEffect.KILL_AGGRO_REDUCE,
        Map.of() // Kill = 3s aggro réduit
    ),

    VELOCITE(
        "Vélocité",
        "Chaque kill te rend plus rapide",
        MutationStrain.SPECTRE,
        MutationTier.TIER_2,
        MutationEffect.STACKING_SPEED,
        Map.of() // +3% speed/kill, stack 10x, reset 15s
    ),

    // Tier 3 (Stades V-VI)
    DOUBLE_FRAPPE(
        "Double Frappe",
        "Chance de frapper deux fois",
        MutationStrain.SPECTRE,
        MutationTier.TIER_3,
        MutationEffect.DOUBLE_HIT,
        Map.of() // 12% chance double hit
    ),

    PREDATEUR_SILENCIEUX(
        "Prédateur Silencieux",
        "Le premier coup est toujours plus fort",
        MutationStrain.SPECTRE,
        MutationTier.TIER_3,
        MutationEffect.FIRST_HIT_BONUS,
        Map.of() // Premier hit +30% dégâts
    ),

    PHASE_SPECTRALE(
        "Phase Spectrale",
        "Chance de te téléporter quand touché",
        MutationStrain.SPECTRE,
        MutationTier.TIER_3,
        MutationEffect.DAMAGE_TELEPORT,
        Map.of() // 10% chance TP 5 blocs
    ),

    DANSE_MACABRE(
        "Danse Macabre",
        "Les kills augmentent ta chance critique",
        MutationStrain.SPECTRE,
        MutationTier.TIER_3,
        MutationEffect.STACKING_CRIT,
        Map.of() // +2% Crit/kill, max +20%, reset 20s
    ),

    // Tier 4-5 (Stades VII-X)
    LAME_FANTOMATIQUE(
        "Lame Fantomatique",
        "Les critiques peuvent chaîner",
        MutationStrain.SPECTRE,
        MutationTier.TIER_4,
        MutationEffect.CRIT_CHAIN,
        Map.of() // 20% crit chain à 1 ennemi
    ),

    SPECTRE_DE_GUERRE(
        "Spectre de Guerre",
        "Au bord de la mort, tu deviens insaisissable",
        MutationStrain.SPECTRE,
        MutationTier.TIER_4,
        MutationEffect.LOW_HP_DEFENSE,
        Map.of() // <30% HP: +25% Esquive, +25% Speed
    ),

    MAITRE_DES_OMBRES(
        "Maître des Ombres",
        "Les attaques dans le dos sont toujours critiques",
        MutationStrain.SPECTRE,
        MutationTier.TIER_5,
        MutationEffect.BACKSTAB_CRIT,
        Map.of() // Dos = crit garanti
    ),

    VENT_DE_LA_MORT(
        "Vent de la Mort",
        "Tu peux traverser les ennemis en sprintant",
        MutationStrain.SPECTRE,
        MutationTier.TIER_5,
        MutationEffect.PHASE_THROUGH,
        Map.of() // Sprint = traverse mobs
    ),

    // ==================== SOUCHE BUTIN (15 mutations) ====================

    // Tier 1 (Stades I-II)
    OEIL_CUPIDE(
        "Œil Cupide",
        "Plus de chances de trouver du loot",
        MutationStrain.BUTIN,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.LUCK, 15.0)
    ),

    COLLECTEUR(
        "Collecteur",
        "Tu gagnes plus de points par kill",
        MutationStrain.BUTIN,
        MutationTier.TIER_1,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.POINTS_BONUS, 10.0)
    ),

    TOUCHER_DE_MIDAS(
        "Toucher de Midas",
        "Chance de gagner des points bonus",
        MutationStrain.BUTIN,
        MutationTier.TIER_1,
        MutationEffect.BONUS_POINTS_CHANCE,
        Map.of() // 5% kill = +25 pts bonus
    ),

    // Tier 2 (Stades III-IV)
    DETECTEUR_DE_TRESORS(
        "Détecteur de Trésors",
        "Les élites droppent mieux",
        MutationStrain.BUTIN,
        MutationTier.TIER_2,
        MutationEffect.ELITE_LOOT_BONUS,
        Map.of() // Élites +30% drop rare+
    ),

    ECONOMISTE(
        "Économiste",
        "Points bonus tous les 10 kills",
        MutationStrain.BUTIN,
        MutationTier.TIER_2,
        MutationEffect.KILL_MILESTONE_POINTS,
        Map.of() // Chaque 10 kills = +50 pts
    ),

    AIMANT_A_XP(
        "Aimant à XP",
        "Tu gagnes plus d'expérience",
        MutationStrain.BUTIN,
        MutationTier.TIER_2,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.XP_BONUS, 20.0)
    ),

    INSTINCT_DU_CHERCHEUR(
        "Instinct du Chercheur",
        "Le loot rare peut devenir encore meilleur",
        MutationStrain.BUTIN,
        MutationTier.TIER_2,
        MutationEffect.LOOT_TIER_UP,
        Map.of() // 10% rare+ → tier+1
    ),

    // Tier 3 (Stades V-VI)
    PLUIE_DOR(
        "Pluie d'Or",
        "Chance de gagner beaucoup de points",
        MutationStrain.BUTIN,
        MutationTier.TIER_3,
        MutationEffect.JACKPOT_POINTS,
        Map.of() // 3% kill = +100 pts
    ),

    CHANCE_DU_DIABLE(
        "Chance du Diable",
        "Plus de loot, moins de dégâts",
        MutationStrain.BUTIN,
        MutationTier.TIER_3,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.LUCK, 25.0, StatType.DAMAGE_PERCENT, -10.0)
    ),

    COMBO_PAYANT(
        "Combo Payant",
        "Les combos augmentent le loot",
        MutationStrain.BUTIN,
        MutationTier.TIER_3,
        MutationEffect.COMBO_LOOT_BONUS,
        Map.of() // Combo 10+ = +50% loot
    ),

    PROSPECTEUR(
        "Prospecteur",
        "Le loot est meilleur dans ta zone",
        MutationStrain.BUTIN,
        MutationTier.TIER_3,
        MutationEffect.ZONE_LOOT_AURA,
        Map.of() // Zone +10% loot (aura)
    ),

    // Tier 4-5 (Stades VII-X)
    JACKPOT_AMBULANT(
        "Jackpot Ambulant",
        "Chance de drop garanti Rare+",
        MutationStrain.BUTIN,
        MutationTier.TIER_4,
        MutationEffect.GUARANTEED_RARE_CHANCE,
        Map.of() // 1% kill = item Rare+ garanti
    ),

    FORTUNE_ULTIME(
        "Fortune Ultime",
        "Bonus massifs de loot et points",
        MutationStrain.BUTIN,
        MutationTier.TIER_4,
        MutationEffect.STAT_BONUS,
        Map.of(StatType.LUCK, 40.0, StatType.POINTS_BONUS, 30.0)
    ),

    MAITRE_DU_MARCHE(
        "Maître du Marché",
        "Le recyclage rapporte plus",
        MutationStrain.BUTIN,
        MutationTier.TIER_5,
        MutationEffect.RECYCLE_BONUS,
        Map.of() // Recycle value +50%
    ),

    FAVORI_DE_LA_CHANCE(
        "Favori de la Chance",
        "Loot Rare+ garanti tous les 50 kills",
        MutationStrain.BUTIN,
        MutationTier.TIER_5,
        MutationEffect.GUARANTEED_RARE_MILESTONE,
        Map.of() // 50 kills = 1 Rare+ garanti
    );

    private final String displayName;
    private final String description;
    private final MutationStrain strain;
    private final MutationTier tier;
    private final MutationEffect effect;
    private final Map<StatType, Double> statBonuses;

    Mutation(String displayName, String description, MutationStrain strain,
             MutationTier tier, MutationEffect effect, Map<StatType, Double> statBonuses) {
        this.displayName = displayName;
        this.description = description;
        this.strain = strain;
        this.tier = tier;
        this.effect = effect;
        this.statBonuses = statBonuses.isEmpty() ? Collections.emptyMap() : new EnumMap<>(statBonuses);
    }

    /**
     * Retourne le nom formaté avec couleur de souche
     */
    public String getFormattedName() {
        return strain.getColor() + strain.getIcon() + " " + displayName;
    }

    /**
     * Retourne le coût d'assurance en gemmes
     */
    public int getInsuranceCost() {
        return tier.getInsuranceCost();
    }

    /**
     * Vérifie si cette mutation donne des bonus de stats simples
     */
    public boolean hasStatBonuses() {
        return !statBonuses.isEmpty();
    }

    /**
     * Types d'effets de mutation
     */
    public enum MutationEffect {
        // Bonus de stats simples (additifs)
        STAT_BONUS,

        // Effets conditionnels
        CONDITIONAL_DAMAGE,      // +dmg sous X% HP
        LOW_HP_BOOST,           // Boost général sous X% HP
        LOW_HP_DEFENSE,         // Défense sous X% HP

        // Effets on-kill
        ON_KILL_EXPLOSION,      // Explosion à la mort
        KILL_COUNTER_EXPLOSION, // Explosion tous les X kills
        HEAL_ON_KILL,           // Soin au kill
        KILL_AGGRO_REDUCE,      // Réduit l'aggro au kill

        // Effets de stacking
        STACKING_LIFESTEAL,     // Lifesteal qui stack
        STACKING_SPEED,         // Speed qui stack
        STACKING_CRIT,          // Crit qui stack
        KILL_STACK_DAMAGE,      // Dégâts qui stack

        // Effets on-hit
        NTH_HIT_BONUS,          // Bonus au Nème hit
        DOUBLE_HIT,             // Chance de double hit
        FIRST_HIT_BONUS,        // Bonus au premier hit
        BACKSTAB_CRIT,          // Crit garanti dans le dos

        // Effets critiques
        CRIT_BLEED,             // Saignement sur crit
        CRIT_ARMOR_PEN,         // Pénétration armure sur crit
        CRIT_CHAIN,             // Chain sur crit

        // Effets défensifs
        DAMAGE_TELEPORT,        // TP quand touché
        PHASE_THROUGH,          // Traverse les mobs
        REVENGE_DAMAGE,         // +dmg aux attaquants

        // Effets d'exécution
        EXECUTE,                // Instakill sous X% HP

        // Effets de loot
        BONUS_POINTS_CHANCE,    // Chance pts bonus
        ELITE_LOOT_BONUS,       // Meilleur loot élites
        KILL_MILESTONE_POINTS,  // Points tous les X kills
        LOOT_TIER_UP,           // Upgrade tier loot
        JACKPOT_POINTS,         // Gros pts rare
        COMBO_LOOT_BONUS,       // Loot bonus en combo
        ZONE_LOOT_AURA,         // Aura loot zone
        GUARANTEED_RARE_CHANCE, // Chance rare garanti
        GUARANTEED_RARE_MILESTONE, // Rare garanti tous les X kills
        RECYCLE_BONUS,          // Bonus recyclage

        // Effets passifs
        DAMAGE_AURA             // Aura de dégâts
    }
}
