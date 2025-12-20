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
 * Listener pour la Voie de la Perforation du Chasseur.
 * Gère les événements de projectiles, dégâts et activation des talents.
 */
public class PerforationListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;
    private final PerforationManager perforationManager;

    private static final String PIERCE_COUNT_KEY = "perforation_pierce_count";
    private static final String PIERCE_DAMAGE_KEY = "perforation_pierce_damage";
    private static final String PIERCED_ENTITIES_KEY = "perforation_pierced_entities";

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

        // === T1: FLECHES PERCANTES - Calculer pierce max ===
        int maxPierce = 1;
        Talent piercingTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
        if (piercingTalent != null) {
            maxPierce = (int) piercingTalent.getValue(0); // 2
        }

        // === T2: CALIBRE - Bonus pierce sur Tir Lourd ===
        if (perforationManager.isHeavyShot(uuid)) {
            Talent caliberTalent = getActiveTalent(player, Talent.TalentEffectType.CALIBER);
            if (caliberTalent != null) {
                maxPierce += (int) caliberTalent.getValue(3); // +1 extra pierce
            }
        }

        // === T8: DÉVASTATION - Pierce infini ===
        if (perforationManager.isDevastationActive(uuid)) {
            maxPierce = 999; // Infini
        }

        // === T3: TRAJECTOIRE FATALE - Créer ligne si 2+ pierces ===
        Talent fatalTalent = getActiveTalent(player, Talent.TalentEffectType.FATAL_TRAJECTORY);
        if (fatalTalent != null && pierceCount >= fatalTalent.getValue(0)) {
            if (piercedEntities.size() >= 2) {
                // Créer ligne entre premier et dernier ennemi percé
                Location start = null;
                Location end = target.getLocation();

                // Le premier ennemi percé
                for (UUID entityUuid : piercedEntities) {
                    Entity entity = plugin.getServer().getEntity(entityUuid);
                    if (entity != null) {
                        start = entity.getLocation();
                        break;
                    }
                }

                if (start != null) {
                    perforationManager.createFatalLine(player, start, end);
                }
            }
        }

        // === T5: PERFORATION ABSOLUE - Réduction d'armure ===
        Talent absoluteTalent = getActiveTalent(player, Talent.TalentEffectType.ABSOLUTE_PERFORATION);
        if (absoluteTalent != null) {
            perforationManager.addArmorReduction(player, target, pierceCount);
        }

        // Incrémenter Calibre
        perforationManager.addCaliber(player, 1);

        // Ajouter Surchauffe
        perforationManager.addOverheat(player);

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

        // === T1: FLECHES PERCANTES - Bonus par ennemi traversé ===
        if (event.getDamager() instanceof Projectile projectile) {
            int pierceCount = projectile.hasMetadata(PIERCE_COUNT_KEY) ?
                projectile.getMetadata(PIERCE_COUNT_KEY).get(0).asInt() : 0;

            Talent piercingTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
            if (piercingTalent != null && pierceCount > 0) {
                double bonusPerPierce = piercingTalent.getValue(1); // 0.25 = 25%
                bonusMultiplier += pierceCount * bonusPerPierce;
            }
        }

        // === T2: CALIBRE - Bonus dégâts ===
        bonusMultiplier += perforationManager.getCaliberDamageBonus(player);

        // === T4: SURCHAUFFE - Bonus dégâts ===
        bonusMultiplier += perforationManager.getOverheatLevel(uuid);

        // Si Surchauffe à 100% et c'est un projectile, déclencher explosion
        if (perforationManager.isOverheatMax(uuid) && event.getDamager() instanceof Projectile) {
            perforationManager.triggerOverheatExplosion(player, target.getLocation());
        }

        // Consommer le Calibre si c'était un Tir Lourd
        if (perforationManager.isHeavyShot(uuid) && event.getDamager() instanceof Projectile) {
            perforationManager.consumeCaliber(uuid);
        }

        // === T5: PERFORATION ABSOLUE - Bonus dégâts sur cibles réduites ===
        bonusMultiplier += perforationManager.getArmorReductionDamageBonus(player, target);

        // === T3: TRAJECTOIRE FATALE - Bonus si dans ligne ===
        bonusMultiplier += perforationManager.getFatalLineDamageBonus(player, target);

        // === T8: DÉVASTATION - Bonus dégâts + slow ===
        if (perforationManager.isDevastationActive(uuid)) {
            bonusMultiplier += perforationManager.getDevastationDamageBonus(uuid);
            perforationManager.applyDevastationSlow(target);
        }

        // === T6: FRÉNÉSIE - Tirs enflammés ===
        if (perforationManager.isFrenzyActive(uuid)) {
            target.setFireTicks(60); // 3s de feu
        }

        // Appliquer les bonus
        event.setDamage(damage * bonusMultiplier);

        // === T7: PERFORATION EN CHAÎNE - Rebonds après dernier pierce ===
        if (event.getDamager() instanceof Projectile projectile) {
            Talent chainTalent = getActiveTalent(player, Talent.TalentEffectType.CHAIN_PERFORATION);
            if (chainTalent != null) {
                int pierceCount = projectile.hasMetadata(PIERCE_COUNT_KEY) ?
                    projectile.getMetadata(PIERCE_COUNT_KEY).get(0).asInt() : 0;

                int maxPierce = 2; // Par défaut
                Talent piercingTalent = getActiveTalent(player, Talent.TalentEffectType.PIERCING_ARROWS);
                if (piercingTalent != null) {
                    maxPierce = (int) piercingTalent.getValue(0);
                }

                // Si c'est le dernier pierce ou mode Dévastation, déclencher rebonds
                if (pierceCount >= maxPierce || perforationManager.isDevastationActive(uuid)) {
                    perforationManager.triggerChainBounce(player, target, damage * bonusMultiplier, 0);
                }
            }
        }
    }

    // ==================== MORT D'ENTITÉ ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        if (!perforationManager.isPerforationPlayer(killer)) return;

        // === T6: MOMENTUM - Kill pendant Surchauffe ===
        Talent momentumTalent = getActiveTalent(killer, Talent.TalentEffectType.HUNTER_MOMENTUM);
        if (momentumTalent != null) {
            perforationManager.onKillDuringOverheat(killer);
        }
    }

    // ==================== SNEAK (ACTIVATION ULTIMATES) ====================

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (!perforationManager.isPerforationPlayer(player)) return;

        // T8: DÉVASTATION - Double sneak
        Talent devTalent = getActiveTalent(player, Talent.TalentEffectType.DEVASTATION);
        if (devTalent != null) {
            perforationManager.handleSneakForJudgment(player); // Réutilise la logique double-sneak
            // Note: On vérifie d'abord Jugement car c'est T9
        }

        // T9: JUGEMENT - Double sneak puis immobile
        Talent judgmentTalent = getActiveTalent(player, Talent.TalentEffectType.JUDGMENT);
        if (judgmentTalent != null) {
            perforationManager.handleSneakForJudgment(player);
            return;
        }

        // Si pas Jugement, vérifier Dévastation
        if (devTalent != null && !perforationManager.isDevastationActive(player.getUniqueId())) {
            handleSneakForDevastation(player);
        }
    }

    private void handleSneakForDevastation(Player player) {
        UUID uuid = player.getUniqueId();

        // Simple double-sneak detection
        long now = System.currentTimeMillis();
        String key = "last_sneak_devastation";

        if (player.hasMetadata(key)) {
            long lastSneak = player.getMetadata(key).get(0).asLong();
            if (now - lastSneak < 500) {
                perforationManager.activateDevastation(player);
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
