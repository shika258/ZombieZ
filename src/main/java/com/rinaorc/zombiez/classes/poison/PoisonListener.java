package com.rinaorc.zombiez.classes.poison;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.UUID;

/**
 * Listener pour la Voie du Poison du Chasseur.
 * Gère les événements d'attaque, de mort et d'activation des talents.
 */
public class PoisonListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;
    private final PoisonManager poisonManager;

    public PoisonListener(ZombieZPlugin plugin, TalentManager talentManager, PoisonManager poisonManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        this.poisonManager = poisonManager;
    }

    // ==================== ATTAQUE ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return; // Pas de PvP

        // Vérifier que c'est un joueur Poison
        if (!poisonManager.isPoisonPlayer(player)) return;

        UUID targetUuid = target.getUniqueId();
        double damage = event.getDamage();

        // === T1: VENOMOUS_STRIKE - Chance de poison ===
        Talent venomousStrike = getActiveTalent(player, Talent.TalentEffectType.VENOMOUS_STRIKE);
        if (venomousStrike != null) {
            double chance = venomousStrike.getValue(0); // 40%
            if (Math.random() < chance) {
                poisonManager.addPoisonStacks(player, target, 1);
            }
        }

        // === T2: CORROSIVE_VENOM - Réduction d'armure ===
        Talent corrosiveVenom = getActiveTalent(player, Talent.TalentEffectType.CORROSIVE_VENOM);
        if (corrosiveVenom != null && poisonManager.isPoisoned(targetUuid)) {
            // Simuler -10% armure via bonus dégâts
            double armorReduction = corrosiveVenom.getValue(2); // 10%
            event.setDamage(damage * (1 + armorReduction));
        }

        // === Bonus Nécrose (+25% dégâts poison) ===
        if (poisonManager.hasNecrosis(targetUuid)) {
            // Les dégâts de poison sont gérés par PoisonManager
        }

        // === T3: DEADLY_TOXINS - Burst à 5 stacks + crit ===
        Talent deadlyToxins = getActiveTalent(player, Talent.TalentEffectType.DEADLY_TOXINS);
        if (deadlyToxins != null && poisonManager.getPoisonStacks(targetUuid) >= 5) {
            // Si c'est un crit, déclencher l'explosion
            double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
            if (damage > baseDamage * 1.4) { // Crit détecté
                // Explosion burst
                poisonManager.triggerEpidemicExplosion(player, target);
            }
        }

        // === T6: TOXIC_SYNERGY - Bonus Attack Speed ===
        // Géré par des effets de potion dans PoisonManager

        // === T8: BLIGHT - Bonus combo +25% dégâts ===
        if (poisonManager.isBlightComboActive(player)) {
            event.setDamage(event.getDamage() * 1.25);
        }

        // === Avatar actif - Tous les stacks instantanément ===
        if (poisonManager.isPlagueAvatarActive(player.getUniqueId())) {
            if (target instanceof Monster) {
                // Max stacks appliqués automatiquement par PoisonManager
            }
        }
    }

    // ==================== MORT D'ENTITE ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // Vérifier que c'est un joueur Poison
        if (!poisonManager.isPoisonPlayer(killer)) return;

        UUID victimUuid = victim.getUniqueId();

        // === T4: PANDEMIC - Explosion à la mort d'un empoisonné ===
        Talent pandemic = getActiveTalent(killer, Talent.TalentEffectType.PANDEMIC);
        if (pandemic != null && poisonManager.isPoisoned(victimUuid)) {
            int chainCount = poisonManager.getPandemicChainCount(victimUuid);
            double victimMaxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
            poisonManager.triggerPandemicExplosion(killer, victim.getLocation(), victimMaxHealth, chainCount);
        }

        // === T7: BLACK_PLAGUE - Nuage mortel à la mort par poison ===
        Talent blackPlague = getActiveTalent(killer, Talent.TalentEffectType.BLACK_PLAGUE);
        if (blackPlague != null && poisonManager.isPoisoned(victimUuid)) {
            // Vérifier si mort par poison (dernière source de dégâts)
            if (victim.getLastDamageCause() != null &&
                victim.getLastDamageCause().getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.POISON) {
                poisonManager.createDeathCloud(killer, victim.getLocation());
            }
        }
    }

    // ==================== SNEAK (AVATAR ACTIVATION) ====================

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (!poisonManager.isPoisonPlayer(player)) return;

        // T9: PLAGUE_AVATAR - Double sneak pour activer
        poisonManager.handleSneakForAvatar(player);
    }

    // ==================== DECONNEXION ====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        poisonManager.cleanupPlayer(uuid);
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
