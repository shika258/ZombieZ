package com.rinaorc.zombiez.classes.perforation;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import org.bukkit.Location;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener pour la Voie du GIVRE (TIR DE GIVRE) du Chasseur.
 * Gère les événements de projectiles, dégâts et activation des talents.
 *
 * Système inspiré de l'Ice Shot Amazon de PoE2:
 * - GIVRE (0-100%): Accumulation sur les ennemis
 * - 50% = RALENTI, 100% = GELÉ
 * - ÉCLAT: Mort d'un gelé = explosion AoE + propagation
 */
public class PerforationListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;
    private final PerforationManager perforationManager;

    private static final String PIERCE_COUNT_KEY = "frost_pierce_count";
    private static final String PIERCED_ENTITIES_KEY = "frost_pierced_entities";
    private static final String FROST_APPLIED_KEY = "frost_applied_amount";

    // Givre de base par tir
    private static final double BASE_FROST_PER_HIT = 15.0; // 15% givre par tir

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

        // Récupérer ou initialiser le compteur de pierce
        int pierceCount = projectile.hasMetadata(PIERCE_COUNT_KEY) ?
            projectile.getMetadata(PIERCE_COUNT_KEY).get(0).asInt() : 0;

        // Récupérer les entités déjà percées
        @SuppressWarnings("unchecked")
        List<UUID> piercedEntities = projectile.hasMetadata(PIERCED_ENTITIES_KEY) ?
            (List<UUID>) projectile.getMetadata(PIERCED_ENTITIES_KEY).get(0).value() : new ArrayList<>();

        // Ne pas percer deux fois la même entité
        if (piercedEntities.contains(target.getUniqueId())) return;

        pierceCount++;
        piercedEntities.add(target.getUniqueId());

        // Mettre à jour les métadonnées
        projectile.setMetadata(PIERCE_COUNT_KEY, new FixedMetadataValue(plugin, pierceCount));
        projectile.setMetadata(PIERCED_ENTITIES_KEY, new FixedMetadataValue(plugin, piercedEntities));

        // === CALCUL DU GIVRE À APPLIQUER ===
        double frostToApply = BASE_FROST_PER_HIT;

        // === T2: CHARGE GLACIALE - Bonus givre selon charge ===
        frostToApply += perforationManager.getFrostChargeBonus(player) * 100; // Convertir en %

        // Tir Glacial = gel instantané
        if (perforationManager.isGlacialShot(uuid)) {
            frostToApply = 100.0; // Gel instantané
        }

        // === T3: LIGNE DE GLACE - Bonus givre si dans la zone ===
        frostToApply += perforationManager.getIceLineFrostBonus(player, target) * 100;

        // === T1: TIRS PERÇANTS - Bonus givre par ennemi traversé ===
        Talent piercingTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
        if (piercingTalent != null && pierceCount > 1) {
            double bonusPerPierce = piercingTalent.getValue(1) * 100; // 25% par traversée
            frostToApply += (pierceCount - 1) * bonusPerPierce * 0.5; // 12.5% givre bonus par traversée
        }

        // Appliquer le givre
        boolean justFrozen = perforationManager.addFrost(player, target, frostToApply);

        // Stocker le givre appliqué pour l'écho
        projectile.setMetadata(FROST_APPLIED_KEY, new FixedMetadataValue(plugin, frostToApply));

        // === T1: TIRS PERÇANTS - Calculer pierce max ===
        int maxPierce = 1;
        if (piercingTalent != null) {
            maxPierce = (int) piercingTalent.getValue(0); // 2
        }

        // === T2: CHARGE GLACIALE - Bonus pierce sur Tir Glacial ===
        if (perforationManager.isGlacialShot(uuid)) {
            Talent chargeTalent = getActiveTalent(player, Talent.TalentEffectType.CALIBER);
            if (chargeTalent != null) {
                maxPierce += (int) chargeTalent.getValue(3); // +1 extra pierce
            }
        }

        // === T8: HIVER ÉTERNEL - Pierce infini ===
        if (perforationManager.isEternalWinterActive(uuid)) {
            maxPierce = 999; // Infini
        }

        // === T3: LIGNE DE GLACE - Créer ligne si 2+ pierces ===
        Talent iceTalent = getActiveTalent(player, Talent.TalentEffectType.FATAL_TRAJECTORY);
        if (iceTalent != null && pierceCount >= iceTalent.getValue(0)) {
            if (piercedEntities.size() >= 2) {
                Location start = null;
                Location end = target.getLocation();

                for (UUID entityUuid : piercedEntities) {
                    Entity entity = plugin.getServer().getEntity(entityUuid);
                    if (entity != null) {
                        start = entity.getLocation();
                        break;
                    }
                }

                if (start != null) {
                    perforationManager.createIceLine(player, start, end);
                }
            }
        }

        // === T7: ÉCHO GLACIAL - Propager givre après dernier pierce ===
        Talent echoTalent = getActiveTalent(player, Talent.TalentEffectType.CHAIN_PERFORATION);
        if (echoTalent != null) {
            if (pierceCount >= maxPierce || perforationManager.isEternalWinterActive(uuid)) {
                perforationManager.triggerFrostEcho(player, target, frostToApply, 0);
            }
        }

        // Incrémenter Charge Glaciale
        perforationManager.addFrostCharge(player, 1);

        // Ajouter Hypothermie
        perforationManager.addHypothermia(player);

        // Enregistrer le joueur si pas déjà fait
        perforationManager.registerPlayer(uuid);
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

        // === T1: TIRS PERÇANTS - Bonus par ennemi traversé ===
        if (event.getDamager() instanceof Projectile projectile) {
            int pierceCount = projectile.hasMetadata(PIERCE_COUNT_KEY) ?
                projectile.getMetadata(PIERCE_COUNT_KEY).get(0).asInt() : 0;

            Talent piercingTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
            if (piercingTalent != null && pierceCount > 0) {
                double bonusPerPierce = piercingTalent.getValue(1); // 0.25 = 25%
                bonusMultiplier += pierceCount * bonusPerPierce;
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
