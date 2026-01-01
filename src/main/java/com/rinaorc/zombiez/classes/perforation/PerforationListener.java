package com.rinaorc.zombiez.classes.perforation;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener pour la Voie du GIVRE (TIR DE GIVRE) du Chasseur.
 * Gère les événements de projectiles, dégâts et activation des talents.
 *
 * Système de REBOND DE GIVRE:
 * - GIVRE (0-100%): Accumulation sur les ennemis
 * - 50% = RALENTI, 100% = GELÉ
 * - REBOND: Les projectiles rebondissent vers les mobs environnants
 * - ÉCLAT: Mort d'un gelé = explosion AoE + propagation
 */
public class PerforationListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;
    private final PerforationManager perforationManager;

    private static final String BOUNCE_COUNT_KEY = "frost_bounce_count";
    private static final String BOUNCED_ENTITIES_KEY = "frost_bounced_entities";
    private static final String FROST_APPLIED_KEY = "frost_applied_amount";
    private static final String BOUNCE_SOURCE_KEY = "frost_bounce_source";
    private static final String FIRST_TARGET_LOC_KEY = "frost_first_target_loc";
    private static final String ICE_LINE_CREATED_KEY = "frost_ice_line_created";

    // Givre de base par tir
    private static final double BASE_FROST_PER_HIT = 15.0; // 15% givre par tir
    private static final double BOUNCE_RANGE = 10.0; // Distance max pour trouver une cible de rebond
    private static final double BOUNCE_PROJECTILE_SPEED = 1.8; // Vitesse du projectile de rebond
    private static final int MAX_BOUNCES_CAP = 15; // Cap absolu pour éviter les boucles infinies

    public PerforationListener(ZombieZPlugin plugin, TalentManager talentManager, PerforationManager perforationManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        this.perforationManager = perforationManager;
    }

    // ==================== PROJECTILE HIT ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (!perforationManager.isPerforationPlayer(player)) return;

        Projectile projectile = event.getEntity();

        // Si on touche une entité
        if (event.getHitEntity() instanceof LivingEntity target && target instanceof Monster) {
            handleProjectileHitEntity(player, projectile, target);
        }
    }

    private void handleProjectileHitEntity(Player player, Projectile projectile, LivingEntity target) {
        UUID uuid = player.getUniqueId();

        // Récupérer ou initialiser le compteur de rebonds
        int bounceCount = projectile.hasMetadata(BOUNCE_COUNT_KEY) ?
            projectile.getMetadata(BOUNCE_COUNT_KEY).get(0).asInt() : 0;

        // Récupérer les entités déjà touchées par rebond
        @SuppressWarnings("unchecked")
        List<UUID> bouncedEntities = projectile.hasMetadata(BOUNCED_ENTITIES_KEY) ?
            (List<UUID>) projectile.getMetadata(BOUNCED_ENTITIES_KEY).get(0).value() : new ArrayList<>();

        // Récupérer la position de la première cible (pour Ligne de Glace)
        Location firstTargetLoc = projectile.hasMetadata(FIRST_TARGET_LOC_KEY) ?
            (Location) projectile.getMetadata(FIRST_TARGET_LOC_KEY).get(0).value() : null;

        // Vérifier si la Ligne de Glace a déjà été créée pour ce tir
        boolean iceLineCreated = projectile.hasMetadata(ICE_LINE_CREATED_KEY);

        // Ne pas toucher deux fois la même entité
        if (bouncedEntities.contains(target.getUniqueId())) return;

        bounceCount++;
        bouncedEntities.add(target.getUniqueId());

        // Sauvegarder la position de la première cible
        if (firstTargetLoc == null) {
            firstTargetLoc = target.getLocation().clone();
        }

        // === CALCUL DU GIVRE À APPLIQUER ===
        // T1: FLÈCHES REBONDISSANTES requis pour appliquer du givre
        Talent bounceTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
        if (bounceTalent == null) {
            // Pas de talent Givre T1 = pas de givre appliqué
            return;
        }

        double frostToApply = BASE_FROST_PER_HIT;

        // === T2: CHARGE GLACIALE - Bonus givre selon charge ===
        Talent chargeTalent = getActiveTalent(player, Talent.TalentEffectType.CALIBER);
        if (chargeTalent != null) {
            frostToApply += perforationManager.getFrostChargeBonus(player) * 100;
        }

        // Tir Glacial = gel instantané (nécessite T2 CALIBER)
        if (chargeTalent != null && perforationManager.isGlacialShot(uuid)) {
            frostToApply = 100.0;
        }

        // === T3: LIGNE DE GLACE - Bonus givre si dans la zone ===
        Talent iceTalent = getActiveTalent(player, Talent.TalentEffectType.FATAL_TRAJECTORY);
        if (iceTalent != null) {
            frostToApply += perforationManager.getIceLineFrostBonus(player, target) * 100;
        }

        // === T1: FLÈCHES REBONDISSANTES - Bonus givre par rebond (après le 1er hit) ===
        if (bounceTalent != null && bounceCount > 1) {
            double bonusPerBounce = bounceTalent.getValue(1) * 100; // 25% par rebond
            frostToApply += (bounceCount - 1) * bonusPerBounce * 0.5; // 12.5% givre bonus par rebond
        }

        // === T5: GIVRE PÉNÉTRANT - Bonus givre par rebond (cumulatif, max +80%) ===
        Talent penetratingTalent = getActiveTalent(player, Talent.TalentEffectType.ABSOLUTE_PERFORATION);
        if (penetratingTalent != null && bounceCount > 1) {
            double frostPerBounce = penetratingTalent.getValue(0) * 100; // 20% par rebond
            double maxBonus = penetratingTalent.getValue(1) * 100; // 80% max
            double bonusFrost = Math.min((bounceCount - 1) * frostPerBounce, maxBonus);
            frostToApply += bonusFrost;
        }

        // Appliquer le givre
        boolean justFrozen = perforationManager.addFrost(player, target, frostToApply);

        // === T1: FLÈCHES REBONDISSANTES - Calculer rebonds max ===
        int maxBounces = 1;
        if (bounceTalent != null) {
            maxBounces = (int) bounceTalent.getValue(0); // 2 rebonds
        }

        // === T2: CHARGE GLACIALE - Bonus rebond sur Tir Glacial ===
        if (perforationManager.isGlacialShot(uuid) && chargeTalent != null) {
            maxBounces += (int) chargeTalent.getValue(3); // +1 rebond supplémentaire
        }

        // === T8: HIVER ÉTERNEL - Rebonds étendus (avec cap pour éviter boucle infinie) ===
        if (perforationManager.isEternalWinterActive(uuid)) {
            maxBounces = MAX_BOUNCES_CAP;
        }

        // === T3: LIGNE DE GLACE - Créer ligne si 2+ rebonds (une seule fois par tir) ===
        if (iceTalent != null && !iceLineCreated && bounceCount >= (int) iceTalent.getValue(0)) {
            perforationManager.createIceLine(player, firstTargetLoc, target.getLocation());
            iceLineCreated = true;
        }

        // === SYSTÈME DE REBOND - Chercher et rebondir vers la prochaine cible ===
        if (bounceCount < maxBounces) {
            LivingEntity nextTarget = findNextBounceTarget(target, bouncedEntities);
            if (nextTarget != null) {
                launchBounceProjectile(player, target, nextTarget, bounceCount, bouncedEntities,
                    firstTargetLoc, frostToApply, iceLineCreated);
            } else {
                // Pas de cible pour rebondir, déclencher l'écho glacial
                triggerEchoIfNeeded(player, target, frostToApply);
            }
        } else {
            // Max rebonds atteint, déclencher l'écho glacial
            triggerEchoIfNeeded(player, target, frostToApply);
        }

        // Incrémenter Charge Glaciale (seulement sur le premier hit, pas les rebonds)
        // Nécessite les talents correspondants
        if (bounceCount == 1) {
            // T2: CHARGE GLACIALE
            if (chargeTalent != null) {
                perforationManager.addFrostCharge(player, 1);
            }
            // T4: HYPOTHERMIE
            Talent hypoTalent = getActiveTalent(player, Talent.TalentEffectType.OVERHEAT);
            if (hypoTalent != null) {
                perforationManager.addHypothermia(player);
            }
        }

        // Enregistrer le joueur si pas déjà fait
        perforationManager.registerPlayer(uuid);
    }

    /**
     * Trouve la prochaine cible de rebond la plus proche
     */
    private LivingEntity findNextBounceTarget(LivingEntity currentTarget, List<UUID> alreadyHit) {
        LivingEntity closest = null;
        double closestDistance = BOUNCE_RANGE;

        for (Entity entity : currentTarget.getNearbyEntities(BOUNCE_RANGE, BOUNCE_RANGE, BOUNCE_RANGE)) {
            if (entity instanceof Monster target && !target.isDead() && !alreadyHit.contains(target.getUniqueId())) {
                double distance = target.getLocation().distance(currentTarget.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = target;
                }
            }
        }

        return closest;
    }

    /**
     * Lance un projectile de rebond vers la prochaine cible
     */
    private void launchBounceProjectile(Player player, LivingEntity from, LivingEntity to,
                                         int currentBounceCount, List<UUID> bouncedEntities,
                                         Location firstTargetLoc, double previousFrost,
                                         boolean iceLineCreated) {
        Location start = from.getLocation().add(0, 1, 0);
        Location end = to.getLocation().add(0, 1, 0);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();

        // Effet visuel de traînée de glace
        spawnBounceTrail(start, end);

        // Son de rebond (pitch augmente avec les rebonds pour le feedback)
        float pitch = Math.min(1.5f + (currentBounceCount * 0.1f), 2.0f);
        start.getWorld().playSound(start, Sound.BLOCK_POWDER_SNOW_HIT, 0.8f, pitch);

        // Créer un nouveau projectile (flèche spectrale) vers la cible
        final Location finalFirstTargetLoc = firstTargetLoc;
        final boolean finalIceLineCreated = iceLineCreated;

        Arrow bounceArrow = player.getWorld().spawn(start, Arrow.class, arrow -> {
            arrow.setShooter(player);
            arrow.setVelocity(direction.multiply(BOUNCE_PROJECTILE_SPEED));
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setGlowing(true);

            // Transférer les métadonnées de rebond
            arrow.setMetadata(BOUNCE_COUNT_KEY, new FixedMetadataValue(plugin, currentBounceCount));
            arrow.setMetadata(BOUNCED_ENTITIES_KEY, new FixedMetadataValue(plugin, new ArrayList<>(bouncedEntities)));
            arrow.setMetadata(BOUNCE_SOURCE_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            arrow.setMetadata(FIRST_TARGET_LOC_KEY, new FixedMetadataValue(plugin, finalFirstTargetLoc));
            arrow.setMetadata(FROST_APPLIED_KEY, new FixedMetadataValue(plugin, previousFrost));

            // Marquer si la Ligne de Glace a déjà été créée
            if (finalIceLineCreated) {
                arrow.setMetadata(ICE_LINE_CREATED_KEY, new FixedMetadataValue(plugin, true));
            }
        });

        // Particules de givre sur la flèche pendant son vol
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (bounceArrow.isDead() || bounceArrow.isOnGround() || ticks++ > 40) {
                    cancel();
                    return;
                }
                bounceArrow.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    bounceArrow.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Crée une traînée visuelle de glace entre deux points
     */
    private void spawnBounceTrail(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = from.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.SNOWFLAKE, point, 1, 0.05, 0.05, 0.05, 0);
            if (d % 1 == 0) {
                point.getWorld().spawnParticle(Particle.BLOCK, point, 1, 0.1, 0.1, 0.1, 0,
                    Material.PACKED_ICE.createBlockData());
            }
        }
    }

    /**
     * Déclenche l'écho glacial si le talent est actif
     */
    private void triggerEchoIfNeeded(Player player, LivingEntity target, double frostApplied) {
        Talent echoTalent = getActiveTalent(player, Talent.TalentEffectType.CHAIN_PERFORATION);
        if (echoTalent != null) {
            perforationManager.triggerFrostEcho(player, target, frostApplied, 0);
        }
    }

    // ==================== DÉGÂTS ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return;

        if (!perforationManager.isPerforationPlayer(player)) return;

        UUID uuid = player.getUniqueId();
        double damage = event.getDamage();
        double bonusMultiplier = 1.0;

        // === BONUS DÉGÂTS SUR CIBLE GELÉE (+50%) ===
        bonusMultiplier += perforationManager.getFrozenDamageBonus(target);

        // === T1: FLÈCHES REBONDISSANTES - Bonus par rebond (après le 1er hit) ===
        if (event.getDamager() instanceof Projectile projectile) {
            int bounceCount = projectile.hasMetadata(BOUNCE_COUNT_KEY) ?
                projectile.getMetadata(BOUNCE_COUNT_KEY).get(0).asInt() : 0;

            Talent bounceTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
            if (bounceTalent != null && bounceCount > 1) {
                double bonusPerBounce = bounceTalent.getValue(1); // 0.25 = 25%
                bonusMultiplier += (bounceCount - 1) * bonusPerBounce; // Bonus seulement sur les rebonds
            }
        }

        // === T4: HYPOTHERMIE - Déclencher Vague de Froid à 100% ===
        if (perforationManager.isHypothermiaMax(uuid) && event.getDamager() instanceof Projectile) {
            perforationManager.triggerColdWave(player, target.getLocation());
        }

        // Consommer la Charge Glaciale si c'était un Tir Glacial
        if (perforationManager.isGlacialShot(uuid) && event.getDamager() instanceof Projectile) {
            perforationManager.consumeFrostCharge(uuid);
        }

        // === T8: HIVER ÉTERNEL - Bonus dégâts ===
        if (perforationManager.isEternalWinterActive(uuid)) {
            bonusMultiplier += 0.60; // +60% dégâts
        }

        // === T6: TEMPÊTE DE NEIGE - Bonus dégâts ===
        if (perforationManager.isBlizzardActive(uuid)) {
            bonusMultiplier += 0.30; // +30% dégâts
        }

        // Appliquer les bonus
        event.setDamage(damage * bonusMultiplier);
    }

    // ==================== MORT D'ENTITÉ (ÉCLAT) ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // Chercher le joueur source du givre
        Player frostSource = perforationManager.getFrostSource(victim.getUniqueId());
        Player killer = victim.getKiller();

        // Utiliser le killer ou la source du givre
        Player player = killer != null ? killer : frostSource;
        if (player == null) return;

        if (!perforationManager.isPerforationPlayer(player)) {
            // Nettoyer les données même si ce n'est pas un joueur Givre
            perforationManager.cleanupEntity(victim.getUniqueId());
            return;
        }

        // === ÉCLAT - Mort d'une entité avec 70%+ givre ===
        double frostLevel = perforationManager.getEntityFrost(victim.getUniqueId());
        if (frostLevel >= 70) {
            perforationManager.triggerShatter(player, victim, 0);
        } else {
            // Nettoyer les données de l'entité si pas d'éclat
            perforationManager.cleanupEntity(victim.getUniqueId());
        }
    }

    // ==================== SNEAK (ACTIVATION ULTIMATES) ====================

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (!perforationManager.isPerforationPlayer(player)) return;

        // T9: ZÉRO ABSOLU - Double sneak puis immobile
        Talent zeroTalent = getActiveTalent(player, Talent.TalentEffectType.JUDGMENT);
        if (zeroTalent != null) {
            perforationManager.handleSneakForAbsoluteZero(player);
            return;
        }

        // T8: HIVER ÉTERNEL - Double sneak
        Talent winterTalent = getActiveTalent(player, Talent.TalentEffectType.DEVASTATION);
        if (winterTalent != null && !perforationManager.isEternalWinterActive(player.getUniqueId())) {
            handleSneakForEternalWinter(player);
        }
    }

    private void handleSneakForEternalWinter(Player player) {
        // Simple double-sneak detection
        long now = System.currentTimeMillis();
        String key = "last_sneak_eternal_winter";

        if (player.hasMetadata(key)) {
            long lastSneak = player.getMetadata(key).get(0).asLong();
            if (now - lastSneak < 500) {
                perforationManager.activateEternalWinter(player);
                player.removeMetadata(key, plugin);
                return;
            }
        }
        player.setMetadata(key, new FixedMetadataValue(plugin, now));
    }

    // ==================== DÉCONNEXION ====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        perforationManager.cleanupPlayer(uuid);
    }

    // ==================== UTILITAIRES ====================

    private Player getPlayerFromDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private Talent getActiveTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType);
    }
}
