package com.rinaorc.zombiez.items.awaken;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Helper pour l'application des effets d'éveil dans les talents
 *
 * Utilisation dans TalentListener:
 * 1. Créer un contexte: AwakenContext ctx = AwakenHelper.getContext(plugin, player, talent, target)
 * 2. Appliquer les modificateurs: damage = ctx.applyDamageModifier(damage)
 * 3. Appliquer les effets bonus: AwakenHelper.applyBonusEffects(ctx)
 */
public class AwakenHelper {

    /**
     * Obtient un contexte d'éveil pour un talent
     *
     * @param plugin Le plugin
     * @param player Le joueur
     * @param talent Le talent en cours d'exécution
     * @param target La cible (peut être null)
     * @return Le contexte d'éveil
     */
    public static AwakenContext getContext(ZombieZPlugin plugin, Player player,
                                           Talent talent, LivingEntity target) {
        return AwakenContext.create(plugin, player, talent, target);
    }

    /**
     * Obtient un contexte d'éveil par ID de talent
     *
     * @param plugin Le plugin
     * @param player Le joueur
     * @param talentId L'ID du talent
     * @param target La cible (peut être null)
     * @return Le contexte d'éveil
     */
    public static AwakenContext getContextByTalentId(ZombieZPlugin plugin, Player player,
                                                     String talentId, LivingEntity target) {
        Talent talent = plugin.getTalentManager().getTalent(talentId);
        return AwakenContext.create(plugin, player, talent, target);
    }

    /**
     * Obtient un contexte d'éveil par type d'effet
     *
     * @param plugin Le plugin
     * @param player Le joueur
     * @param effectType Le type d'effet du talent
     * @param target La cible (peut être null)
     * @return Le contexte d'éveil
     */
    public static AwakenContext getContextByEffectType(ZombieZPlugin plugin, Player player,
                                                       Talent.TalentEffectType effectType,
                                                       LivingEntity target) {
        // Chercher un éveil actif pour ce type d'effet
        var awakenManager = plugin.getAwakenManager();
        if (awakenManager == null) {
            return AwakenContext.empty(player, null, target);
        }

        Awaken awaken = awakenManager.getActiveAwakenForEffect(player, effectType);
        if (awaken == null) {
            return AwakenContext.empty(player, null, target);
        }

        // Trouver le talent correspondant
        Talent talent = plugin.getTalentManager().getTalent(awaken.getTargetTalentId());

        return AwakenContext.builder()
            .player(player)
            .talent(talent)
            .activeAwaken(awaken)
            .target(target)
            .build();
    }

    /**
     * Applique les effets bonus de l'éveil
     *
     * Cette méthode gère les effets secondaires comme:
     * - Slow sur la cible
     * - Vulnérabilité
     * - Speed buff sur le joueur
     * - Heal
     * - Bouclier
     *
     * @param ctx Le contexte d'éveil
     */
    public static void applyBonusEffects(AwakenContext ctx) {
        if (ctx == null || !ctx.hasAwaken() || !ctx.isAwakenForCurrentTalent()) {
            return;
        }

        Player player = ctx.getPlayer();
        LivingEntity target = ctx.getTarget();

        // Slow sur la cible
        if (ctx.shouldApplySlow() && target != null) {
            int durationTicks = ctx.getSlowDurationTicks();
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                durationTicks,
                1, // Slowness II
                false,
                true
            ));
        }

        // Speed buff sur le joueur
        if (ctx.shouldApplySpeedBuff()) {
            int speedLevel = (int) Math.round(ctx.getSpeedBonus() * 5); // 0.2 = Speed I
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                60, // 3 secondes
                Math.min(speedLevel, 2), // Max Speed III
                false,
                true
            ));
        }

        // Heal sur proc
        if (ctx.shouldHealOnProc()) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double healAmount = maxHealth * ctx.getHealPercent();
            double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
            player.setHealth(newHealth);

            // Effet visuel
            player.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                player.getLocation().add(0, 1, 0),
                3, 0.3, 0.3, 0.3, 0
            );
        }

        // Bouclier (absorption)
        if (ctx.shouldApplyShield()) {
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double shieldAmount = maxHealth * ctx.getShieldPercent();

            // Ajouter absorption via potion effect
            int absorptionLevel = (int) Math.ceil(shieldAmount / 4.0); // 4 HP par niveau
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                100, // 5 secondes
                Math.min(absorptionLevel - 1, 4), // Max Absorption V
                false,
                true
            ));

            // Effet visuel
            player.getWorld().spawnParticle(
                org.bukkit.Particle.END_ROD,
                player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.02
            );
        }
    }

    /**
     * Applique la vulnérabilité si l'éveil le requiert
     *
     * La vulnérabilité augmente les dégâts subis par la cible
     *
     * @param ctx Le contexte d'éveil
     * @param target La cible
     */
    public static void applyVulnerability(AwakenContext ctx, LivingEntity target) {
        if (ctx == null || !ctx.shouldApplyVulnerability() || target == null) {
            return;
        }

        // On utilise un tag scoreboard pour tracker la vulnérabilité
        String tag = "awaken_vulnerable_" + ctx.getPlayer().getUniqueId();
        target.addScoreboardTag(tag);

        // Programmer la suppression après 5 secondes
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            ctx.getPlayer().getServer().getPluginManager()
                .getPlugin("ZombieZ"),
            () -> target.removeScoreboardTag(tag),
            100 // 5 secondes
        );
    }

    /**
     * Vérifie si une cible est vulnérable
     *
     * @param target La cible
     * @param attacker L'attaquant (pour vérifier la source)
     * @return Le bonus de dégâts (0.0 si pas vulnérable)
     */
    public static double getVulnerabilityBonus(LivingEntity target, Player attacker) {
        if (target == null || attacker == null) return 0.0;

        String tag = "awaken_vulnerable_" + attacker.getUniqueId();
        if (target.getScoreboardTags().contains(tag)) {
            // Bonus par défaut de 10% si vulnérable
            return 0.10;
        }
        return 0.0;
    }

    /**
     * Obtient le nombre d'invocations supplémentaires
     *
     * @param ctx Le contexte d'éveil
     * @return Le nombre d'invocations bonus (0 si pas applicable)
     */
    public static int getExtraSummonCount(AwakenContext ctx) {
        if (ctx == null || !ctx.hasAwaken()) return 0;
        return ctx.getExtraCount();
    }

    /**
     * Obtient le nombre de projectiles supplémentaires
     *
     * @param ctx Le contexte d'éveil
     * @return Le nombre de projectiles bonus (0 si pas applicable)
     */
    public static int getExtraProjectileCount(AwakenContext ctx) {
        if (ctx == null || !ctx.hasAwaken()) return 0;

        if (ctx.hasModifierType(AwakenModifierType.EXTRA_PROJECTILE)) {
            return ctx.getExtraCount();
        }
        return 0;
    }

    /**
     * Obtient le nombre de rebonds supplémentaires
     *
     * @param ctx Le contexte d'éveil
     * @return Le nombre de rebonds bonus (0 si pas applicable)
     */
    public static int getExtraBounceCount(AwakenContext ctx) {
        if (ctx == null || !ctx.hasAwaken()) return 0;

        if (ctx.hasModifierType(AwakenModifierType.EXTRA_BOUNCE)) {
            return ctx.getExtraCount();
        }
        return 0;
    }

    /**
     * Calcule les dégâts avec tous les bonus d'éveil
     *
     * @param ctx Le contexte d'éveil
     * @param baseDamage Dégâts de base
     * @return Dégâts finaux
     */
    public static double calculateFinalDamage(AwakenContext ctx, double baseDamage) {
        if (ctx == null || !ctx.hasAwaken() || !ctx.isAwakenForCurrentTalent()) {
            return baseDamage;
        }

        double damage = ctx.applyDamageModifier(baseDamage);

        // Appliquer la vulnérabilité si présente
        if (ctx.getTarget() != null) {
            double vulnBonus = getVulnerabilityBonus(ctx.getTarget(), ctx.getPlayer());
            damage *= (1.0 + vulnBonus);
        }

        return damage;
    }

    /**
     * Calcule le cooldown avec le modificateur d'éveil
     *
     * @param ctx Le contexte d'éveil
     * @param baseCooldownMs Cooldown de base en millisecondes
     * @return Cooldown final
     */
    public static long calculateFinalCooldown(AwakenContext ctx, long baseCooldownMs) {
        if (ctx == null || !ctx.hasAwaken() || !ctx.isAwakenForCurrentTalent()) {
            return baseCooldownMs;
        }

        return ctx.applyCooldownModifier(baseCooldownMs);
    }

    /**
     * Calcule le rayon avec le modificateur d'éveil
     *
     * @param ctx Le contexte d'éveil
     * @param baseRadius Rayon de base
     * @return Rayon final
     */
    public static double calculateFinalRadius(AwakenContext ctx, double baseRadius) {
        if (ctx == null || !ctx.hasAwaken() || !ctx.isAwakenForCurrentTalent()) {
            return baseRadius;
        }

        return ctx.applyRadiusModifier(baseRadius);
    }

    /**
     * Calcule la durée avec le modificateur d'éveil
     *
     * @param ctx Le contexte d'éveil
     * @param baseDurationMs Durée de base en millisecondes
     * @return Durée finale
     */
    public static long calculateFinalDuration(AwakenContext ctx, long baseDurationMs) {
        if (ctx == null || !ctx.hasAwaken() || !ctx.isAwakenForCurrentTalent()) {
            return baseDurationMs;
        }

        return ctx.applyDurationModifier(baseDurationMs);
    }

    /**
     * Affiche une notification d'éveil au joueur
     *
     * @param player Le joueur
     * @param awakenName Le nom de l'éveil
     */
    public static void showAwakenNotification(Player player, String awakenName) {
        if (player == null || awakenName == null) return;

        player.sendActionBar(net.kyori.adventure.text.Component.text(
            "§d✦ " + awakenName + " §7activé!"
        ));

        // Son subtil
        player.playSound(player.getLocation(),
            org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_HIT,
            0.5f, 1.2f);
    }
}
