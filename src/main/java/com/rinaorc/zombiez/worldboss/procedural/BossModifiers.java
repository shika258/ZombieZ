package com.rinaorc.zombiez.worldboss.procedural;

import com.rinaorc.zombiez.worldboss.WorldBossType;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Modificateurs procéduraux pour un World Boss
 * Rend chaque boss unique avec des variations de stats, capacités et apparence
 */
@Getter
public class BossModifiers {

    // Identification unique
    private final long seed;

    // Traits (1-3 traits aléatoires)
    private final BossTrait[] traits;

    // Nom procédural
    private final BossNameGenerator.ProceduralName name;

    // Variations de stats (multiplicateurs)
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double scaleMultiplier;
    private final double abilityCooldownMultiplier;
    private final double abilityEffectMultiplier;

    // Variations visuelles
    private final Color primaryColor;
    private final Color secondaryColor;
    private final Particle ambientParticle;
    private final float particleSize;

    // Attributs spéciaux
    private final double knockbackResistance;
    private final double armorBonus;
    private final double regenerationRate; // HP par seconde

    // Capacités bonus
    private final boolean hasLifesteal;
    private final double lifestealPercent;
    private final boolean hasThorns;
    private final double thornsPercent;
    private final boolean hasExplosiveDeaths;

    private BossModifiers(Builder builder) {
        this.seed = builder.seed;
        this.traits = builder.traits;
        this.name = builder.name;
        this.healthMultiplier = builder.healthMultiplier;
        this.damageMultiplier = builder.damageMultiplier;
        this.speedMultiplier = builder.speedMultiplier;
        this.scaleMultiplier = builder.scaleMultiplier;
        this.abilityCooldownMultiplier = builder.abilityCooldownMultiplier;
        this.abilityEffectMultiplier = builder.abilityEffectMultiplier;
        this.primaryColor = builder.primaryColor;
        this.secondaryColor = builder.secondaryColor;
        this.ambientParticle = builder.ambientParticle;
        this.particleSize = builder.particleSize;
        this.knockbackResistance = builder.knockbackResistance;
        this.armorBonus = builder.armorBonus;
        this.regenerationRate = builder.regenerationRate;
        this.hasLifesteal = builder.hasLifesteal;
        this.lifestealPercent = builder.lifestealPercent;
        this.hasThorns = builder.hasThorns;
        this.thornsPercent = builder.thornsPercent;
        this.hasExplosiveDeaths = builder.hasExplosiveDeaths;
    }

    /**
     * Génère des modificateurs complètement aléatoires
     */
    public static BossModifiers generate(WorldBossType baseType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long seed = random.nextLong();

        return generate(baseType, seed);
    }

    /**
     * Génère des modificateurs à partir d'une seed (reproductible)
     */
    public static BossModifiers generate(WorldBossType baseType, long seed) {
        // Utiliser une seed pour la reproductibilité
        java.util.Random random = new java.util.Random(seed);

        // Nombre de traits (1-3)
        int traitCount = random.nextInt(3) + 1;
        BossTrait[] traits = BossTrait.randomUnique(traitCount, random);

        // Calculer les multiplicateurs de base depuis les traits
        double baseHealth = 1.0;
        double baseDamage = 1.0;
        double baseSpeed = 1.0;
        double baseAbility = 1.0;

        for (BossTrait trait : traits) {
            baseHealth *= trait.getHealthMultiplier();
            baseDamage *= trait.getDamageMultiplier();
            baseSpeed *= trait.getSpeedMultiplier();
            baseAbility *= trait.getAbilityMultiplier();
        }

        // Ajouter de la variance aléatoire (-15% à +15%)
        double variance = 0.15;
        double healthMult = baseHealth * (1.0 + (random.nextDouble() * 2 - 1) * variance);
        double damageMult = baseDamage * (1.0 + (random.nextDouble() * 2 - 1) * variance);
        double speedMult = baseSpeed * (1.0 + (random.nextDouble() * 2 - 1) * variance);
        double abilityMult = baseAbility * (1.0 + (random.nextDouble() * 2 - 1) * variance);

        // Scale variation procédurale (x1.0 à x2.0 multiplicateur)
        // Permet d'atteindre des scales finales de x2 (min) à x10 (max)
        double scaleMult = 1.0 + random.nextDouble();

        // Générer le nom (avec la même seed pour reproductibilité)
        String baseName = extractBaseName(baseType.getDisplayName());
        BossNameGenerator.ProceduralName name = BossNameGenerator.generate(baseName, traits, random);

        // Couleurs basées sur le trait principal + variance
        Color primaryColor = traits[0].getParticleColor();
        Color secondaryColor = generateVariantColor(primaryColor, random);

        // Particule ambiante aléatoire
        Particle[] ambientParticles = {
            Particle.SMOKE, Particle.SOUL, Particle.DUST, Particle.FLAME,
            Particle.SNOWFLAKE, Particle.WITCH, Particle.PORTAL, Particle.ASH,
            Particle.CRIMSON_SPORE, Particle.WARPED_SPORE
        };
        Particle ambientParticle = ambientParticles[random.nextInt(ambientParticles.length)];

        // Taille de particule
        float particleSize = 1.0f + random.nextFloat() * 1.5f;

        // Attributs spéciaux
        double knockbackRes = 0.5 + random.nextDouble() * 0.5; // 50-100%
        double armor = random.nextDouble() * 10; // 0-10 armor bonus

        // Régénération (certains boss seulement)
        double regenRate = 0.0;
        if (hasTrait(traits, BossTrait.REGENERATING)) {
            regenRate = 2.0 + random.nextDouble() * 4.0; // 2-6 HP/s
        }

        // Capacités spéciales basées sur les traits
        boolean lifesteal = hasTrait(traits, BossTrait.VAMPIRIC);
        double lifestealPct = lifesteal ? (0.15 + random.nextDouble() * 0.15) : 0; // 15-30%

        boolean thorns = hasTrait(traits, BossTrait.THORNS);
        double thornsPct = thorns ? (0.15 + random.nextDouble() * 0.15) : 0; // 15-30%

        boolean explosive = hasTrait(traits, BossTrait.EXPLOSIVE);

        return new Builder()
            .seed(seed)
            .traits(traits)
            .name(name)
            .healthMultiplier(healthMult)
            .damageMultiplier(damageMult)
            .speedMultiplier(speedMult)
            .scaleMultiplier(scaleMult)
            .abilityCooldownMultiplier(abilityMult)
            .abilityEffectMultiplier(1.0 + random.nextDouble() * 0.3) // 100-130%
            .primaryColor(primaryColor)
            .secondaryColor(secondaryColor)
            .ambientParticle(ambientParticle)
            .particleSize(particleSize)
            .knockbackResistance(knockbackRes)
            .armorBonus(armor)
            .regenerationRate(regenRate)
            .hasLifesteal(lifesteal)
            .lifestealPercent(lifestealPct)
            .hasThorns(thorns)
            .thornsPercent(thornsPct)
            .hasExplosiveDeaths(explosive)
            .build();
    }

    /**
     * Extrait le nom de base (sans "Le/La")
     */
    private static String extractBaseName(String fullName) {
        if (fullName.startsWith("Le ")) {
            return fullName.substring(3);
        } else if (fullName.startsWith("La ")) {
            return fullName.substring(3);
        } else if (fullName.startsWith("L'")) {
            return fullName.substring(2);
        }
        return fullName;
    }

    /**
     * Génère une couleur variante
     */
    private static Color generateVariantColor(Color base, java.util.Random random) {
        int r = Math.min(255, Math.max(0, base.getRed() + random.nextInt(61) - 30));
        int g = Math.min(255, Math.max(0, base.getGreen() + random.nextInt(61) - 30));
        int b = Math.min(255, Math.max(0, base.getBlue() + random.nextInt(61) - 30));
        return Color.fromRGB(r, g, b);
    }

    /**
     * Vérifie si un trait est présent
     */
    private static boolean hasTrait(BossTrait[] traits, BossTrait target) {
        for (BossTrait trait : traits) {
            if (trait == target) return true;
        }
        return false;
    }

    /**
     * Vérifie si le boss a un trait spécifique
     */
    public boolean hasTrait(BossTrait target) {
        return hasTrait(this.traits, target);
    }

    /**
     * Obtient une description des traits
     */
    public String getTraitsDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < traits.length; i++) {
            if (i > 0) sb.append(" §7| ");
            sb.append(traits[i].getColorCode()).append(traits[i].getDisplayName());
        }
        return sb.toString();
    }

    /**
     * Obtient le modificateur de difficulté global (pour le loot)
     */
    public double getDifficultyMultiplier() {
        return (healthMultiplier + damageMultiplier + (1.0 / abilityCooldownMultiplier)) / 3.0;
    }

    // Builder
    public static class Builder {
        private long seed;
        private BossTrait[] traits = new BossTrait[0];
        private BossNameGenerator.ProceduralName name;
        private double healthMultiplier = 1.0;
        private double damageMultiplier = 1.0;
        private double speedMultiplier = 1.0;
        private double scaleMultiplier = 1.0;
        private double abilityCooldownMultiplier = 1.0;
        private double abilityEffectMultiplier = 1.0;
        private Color primaryColor = Color.RED;
        private Color secondaryColor = Color.ORANGE;
        private Particle ambientParticle = Particle.SMOKE;
        private float particleSize = 1.5f;
        private double knockbackResistance = 0.8;
        private double armorBonus = 0.0;
        private double regenerationRate = 0.0;
        private boolean hasLifesteal = false;
        private double lifestealPercent = 0.0;
        private boolean hasThorns = false;
        private double thornsPercent = 0.0;
        private boolean hasExplosiveDeaths = false;

        public Builder seed(long seed) { this.seed = seed; return this; }
        public Builder traits(BossTrait[] traits) { this.traits = traits; return this; }
        public Builder name(BossNameGenerator.ProceduralName name) { this.name = name; return this; }
        public Builder healthMultiplier(double val) { this.healthMultiplier = val; return this; }
        public Builder damageMultiplier(double val) { this.damageMultiplier = val; return this; }
        public Builder speedMultiplier(double val) { this.speedMultiplier = val; return this; }
        public Builder scaleMultiplier(double val) { this.scaleMultiplier = val; return this; }
        public Builder abilityCooldownMultiplier(double val) { this.abilityCooldownMultiplier = val; return this; }
        public Builder abilityEffectMultiplier(double val) { this.abilityEffectMultiplier = val; return this; }
        public Builder primaryColor(Color color) { this.primaryColor = color; return this; }
        public Builder secondaryColor(Color color) { this.secondaryColor = color; return this; }
        public Builder ambientParticle(Particle particle) { this.ambientParticle = particle; return this; }
        public Builder particleSize(float size) { this.particleSize = size; return this; }
        public Builder knockbackResistance(double val) { this.knockbackResistance = val; return this; }
        public Builder armorBonus(double val) { this.armorBonus = val; return this; }
        public Builder regenerationRate(double val) { this.regenerationRate = val; return this; }
        public Builder hasLifesteal(boolean val) { this.hasLifesteal = val; return this; }
        public Builder lifestealPercent(double val) { this.lifestealPercent = val; return this; }
        public Builder hasThorns(boolean val) { this.hasThorns = val; return this; }
        public Builder thornsPercent(double val) { this.thornsPercent = val; return this; }
        public Builder hasExplosiveDeaths(boolean val) { this.hasExplosiveDeaths = val; return this; }

        public BossModifiers build() { return new BossModifiers(this); }
    }
}
