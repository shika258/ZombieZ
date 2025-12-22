package com.rinaorc.zombiez.classes.poison;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
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
 *
 * REFONTE: Utilise le nouveau système de Virulence (0-100)
 * au lieu des stacks explosifs.
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return; // Pas de PvP

        // Vérifier que c'est un joueur Poison
        if (!poisonManager.isPoisonPlayer(player)) return;

        UUID targetUuid = target.getUniqueId();

        // === T1: VENOMOUS_STRIKE - Chance d'appliquer de la virulence ===
        Talent venomousStrike = getActiveTalent(player, Talent.TalentEffectType.VENOMOUS_STRIKE);
        if (venomousStrike != null) {
            double chance = venomousStrike.getValue(0); // 40%
            if (Math.random() < chance) {
                // Appliquer virulence de base
                poisonManager.addVirulence(player, target, PoisonManager.BASE_VIRULENCE_PER_HIT);
            }
        }

        // === T2: CORROSIVE_VENOM - Bonus dégâts sur cibles empoisonnées ===
        Talent corrosiveVenom = getActiveTalent(player, Talent.TalentEffectType.CORROSIVE_VENOM);
        if (corrosiveVenom != null && poisonManager.isPoisoned(targetUuid)) {
            double bonusPercent = corrosiveVenom.getValue(2); // 15%
            event.setDamage(event.getDamage() * (1 + bonusPercent));
        }

        // === Bonus Nécrose (+25% dégâts si 70%+ virulence) ===
        // Géré automatiquement dans les DoTs de PoisonManager

        // === T5: NECROSIS - Bonus dégâts sur cibles corrompues (+30%) ===
        if (poisonManager.isCorrupted(targetUuid)) {
            event.setDamage(event.getDamage() * 1.30);
        }

        // === T6: TOXIC_SYNERGY - Bonus dégâts basé sur virulence proche ===
        double synergyBonus = poisonManager.getToxicSynergyBonus(player);
        if (synergyBonus > 0) {
            event.setDamage(event.getDamage() * (1 + synergyBonus));
        }

        // === T8: BLIGHT - Bonus combo +20% dégâts si 200+ virulence totale ===
        if (poisonManager.isBlightComboActive(player)) {
            event.setDamage(event.getDamage() * 1.20);
        }

        // === Avatar actif - Application de virulence garantie ===
        if (poisonManager.isPlagueAvatarActive(player.getUniqueId())) {
            if (target instanceof Monster) {
                // Avatar garantit l'application (pas de chance)
                // addVirulence applique déjà le x3 d'Avatar
                if (venomousStrike == null) {
                    // Si pas le talent T1, appliquer quand même pendant Avatar
                    poisonManager.addVirulence(player, target, PoisonManager.BASE_VIRULENCE_PER_HIT);
                }
            }
        }
    }

    // ==================== MORT D'ENTITÉ ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();

        // Vérifier si la cible était empoisonnée
        if (!poisonManager.isPoisoned(victimUuid)) return;

        // Récupérer le propriétaire du poison
        UUID ownerUuid = poisonManager.getPoisonOwner(victimUuid);
        if (ownerUuid == null) return;

        Player owner = plugin.getServer().getPlayer(ownerUuid);
        if (owner == null) return;

        // Vérifier que c'est un joueur Poison
        if (!poisonManager.isPoisonPlayer(owner)) return;

        // === T4: PANDEMIC - Propagation à la mort ===
        Talent pandemic = getActiveTalent(owner, Talent.TalentEffectType.PANDEMIC);
        if (pandemic != null) {
            poisonManager.propagateOnDeath(owner, victim);
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

    // ==================== DÉCONNEXION ====================

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
