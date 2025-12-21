package com.rinaorc.zombiez.classes.shadow;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.classes.ClassType;
import com.rinaorc.zombiez.classes.talents.Talent;
import com.rinaorc.zombiez.classes.talents.TalentManager;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.progression.SkillTreeManager.SkillBonus;
import org.bukkit.*;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour la branche Ombre du Chasseur.
 * Gère tous les événements liés aux talents Ombre.
 *
 * Talents gérés:
 * - T1: SHADOW_BLADE (Points d'Ombre sur attaque)
 * - T2: INSIDIOUS_POISON (Poison stack sur attaque)
 * - T3: SHADOW_STEP (Téléportation derrière + 2 Points)
 * - T4: DEATH_MARK (Crits marquent la cible)
 * - T5: EXECUTION (5 Points = dégâts massifs sur marqué)
 * - T6: DANSE_MACABRE (Kill marqué = invis + reset + speed)
 * - T7: SHADOW_CLONE (5 Points = Lames Spectrales orbitales)
 * - T8: SHADOW_STORM (Exécution kill = AoE + marque tous)
 * - T9: SHADOW_AVATAR (Ultime transformation)
 */
public class ShadowListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;
    private final ShadowManager shadowManager;

    // Double-sneak pour Avatar activation
    private final Map<UUID, Long> lastAvatarSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW = 400; // 400ms

    // Tracking attaque (Shift+Attaque pour Shadow Step)
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private static final long SHIFT_ATTACK_WINDOW = 200; // 200ms

    // Cooldown interne ICD
    private final Map<UUID, Long> shadowBladeCooldown = new ConcurrentHashMap<>();
    private static final long SHADOW_BLADE_ICD = 300; // 300ms

    public ShadowListener(ZombieZPlugin plugin, TalentManager talentManager, ShadowManager shadowManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        this.shadowManager = shadowManager;
    }

    // ==================== ATTAQUE ====================

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Player player = getPlayerFromDamager(event.getDamager());
        if (player == null) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof Player) return; // Pas de PvP

        // Vérifier que c'est un joueur Ombre
        if (!shadowManager.isShadowPlayer(player)) return;

        UUID uuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        double damage = event.getDamage();
        boolean isMelee = !(event.getDamager() instanceof Projectile);

        // === T1: SHADOW_BLADE - Points d'Ombre sur attaque ===
        Talent shadowBlade = getActiveTalent(player, Talent.TalentEffectType.SHADOW_BLADE);
        if (shadowBlade != null && isMelee) {
            processShadowBlade(player, shadowBlade);
        }

        // === T2: SHADOW_SHOT - 15% chance tir de pistolet ===
        Talent shadowShot = getActiveTalent(player, Talent.TalentEffectType.SHADOW_SHOT);
        if (shadowShot != null && isMelee) {
            processShadowShot(player, target, shadowShot);
        }

        // === T3: SHADOW_STEP - Shift+Attaque = téléportation ===
        // Note: Ne pas déclencher si Execution peut être utilisée (priorité à Execution)
        Talent shadowStep = getActiveTalent(player, Talent.TalentEffectType.SHADOW_STEP);
        Talent executionCheck = getActiveTalent(player, Talent.TalentEffectType.EXECUTION);
        boolean canExecute = false;
        if (executionCheck != null) {
            int preparedCost = shadowManager.getPreparedExecutionCost(uuid);
            int requiredPoints = preparedCost > 0 ? preparedCost : 5;
            canExecute = shadowManager.hasEnoughPoints(uuid, requiredPoints);
        }

        if (shadowStep != null && isMelee && player.isSneaking() && !canExecute) {
            double shadowStepDamage = shadowManager.executeShadowStep(player, target, shadowStep);
            if (shadowStepDamage > 0) {
                // Annuler les dégâts de l'attaque normale (dégâts déjà appliqués dans executeShadowStep)
                event.setCancelled(true);
            }
        }

        // === T4: DEATH_MARK - Crits ZombieZ marquent ===
        Talent deathMark = getActiveTalent(player, Talent.TalentEffectType.DEATH_MARK);
        if (deathMark != null && isMelee) {
            // Utiliser le système de crit ZombieZ (CRIT_CHANCE stat + skill bonus)
            var playerStats = plugin.getItemManager().calculatePlayerStats(player);
            var skillManager = plugin.getSkillTreeManager();

            double baseCritChance = playerStats.getOrDefault(StatType.CRIT_CHANCE, 0.0);
            double skillCritChance = skillManager.getSkillBonus(player, SkillBonus.CRIT_CHANCE);
            double totalCritChance = baseCritChance + skillCritChance;

            // Roll de crit (même formule que CombatListener)
            boolean isCrit = Math.random() * 100 < totalCritChance;

            if (isCrit && !shadowManager.isMarked(targetUuid)) {
                shadowManager.applyDeathMark(player, target);
                // Feedback visuel et sonore
                player.sendMessage("§5§l[OMBRE] §7Cible §dmarquée§7 pendant §c8s§7!");
            }
        }

        // === T5: EXECUTION - 5 Points (ou 3 avec Exécution Préparée) ===
        Talent execution = getActiveTalent(player, Talent.TalentEffectType.EXECUTION);
        if (execution != null && isMelee) {
            // Vérifier si Exécution Préparée est active (coût réduit de Danse Macabre)
            int preparedCost = shadowManager.getPreparedExecutionCost(uuid);
            int requiredPoints = preparedCost > 0 ? preparedCost : 5;

            if (shadowManager.hasEnoughPoints(uuid, requiredPoints)) {
                // Exécution fonctionne sur toute cible, bonus si marquée
                boolean isMarked = shadowManager.isMarkedBy(targetUuid, uuid);
                double executionDamage = shadowManager.executeExecutionWithCost(player, target, isMarked, requiredPoints);
                if (executionDamage > 0) {
                    event.setCancelled(true); // On gère les dégâts nous-mêmes

                    // Consommer le buff d'Exécution Préparée si utilisé
                    if (preparedCost > 0) {
                        shadowManager.consumePreparedExecution(uuid);
                        player.sendMessage("§5§l[OMBRE] §eExécution Préparée §7utilisée! (§a" + requiredPoints + " pts§7)");
                    }

                    // === T8: SHADOW_STORM - Exécution kill = AoE ===
                    Talent shadowStorm = getActiveTalent(player, Talent.TalentEffectType.SHADOW_STORM);
                    if (shadowStorm != null && target.isDead()) {
                        // Utiliser les valeurs du talent: radius, damage_mult, points_per_enemy
                        double[] stormValues = shadowStorm.getValues();
                        double radius = stormValues.length > 0 ? stormValues[0] : 6.0;
                        double damageMult = stormValues.length > 1 ? stormValues[1] : 1.50;
                        shadowManager.triggerShadowStorm(player, target.getLocation(), executionDamage, radius, damageMult);
                    }
                }
            }
        }

        // === T7: LAMES SPECTRALES - Les lames frappent automatiquement ===
        // (Pas besoin d'appeler manuellement, updateSpectralBlades() gère la rotation et les hits)

        // === Bonus dégâts Avatar actif (+40% équilibré) ===
        // Note: Ce bonus ne s'applique pas aux Exécutions (event annulé plus haut)
        // Les Exécutions ont leur propre système de dégâts
        if (shadowManager.isAvatarActive(uuid) && !event.isCancelled()) {
            event.setDamage(event.getDamage() * 1.4); // 40% bonus
        }

        // Bonus dégâts sur cible marquée (+25%)
        if (shadowManager.isMarkedBy(targetUuid, uuid) && !event.isCancelled()) {
            double originalDamage = event.getDamage();
            double markedDamage = originalDamage * 1.25;
            event.setDamage(markedDamage);

            // Feedback visuel pour le bonus de marque
            target.getWorld().spawnParticle(Particle.WITCH,
                target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
        }

        // Tracker l'attaque
        lastAttackTime.put(uuid, System.currentTimeMillis());
    }

    /**
     * Traite le talent Shadow Blade (T1)
     */
    private void processShadowBlade(Player player, Talent talent) {
        UUID uuid = player.getUniqueId();

        // ICD check
        Long lastProc = shadowBladeCooldown.get(uuid);
        if (lastProc != null && System.currentTimeMillis() - lastProc < SHADOW_BLADE_ICD) {
            return;
        }
        shadowBladeCooldown.put(uuid, System.currentTimeMillis());

        // +1 Point d'Ombre par attaque
        shadowManager.addShadowPoints(uuid, 1);

        // Bonus Attack Speed + Speed 2 à 3+ Points
        int currentPoints = shadowManager.getShadowPoints(uuid);
        int threshold = (int) talent.getValue(1); // Index 1 = threshold (3)
        double attackSpeedBonus = talent.getValue(2); // Index 2 = attack_speed_bonus (0.30)

        if (currentPoints >= threshold) {
            applyAttackSpeedBonus(player, attackSpeedBonus);
            // Appliquer Speed II (amplifier 1) pendant 3 secondes, refresh à chaque attaque
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 60, 1, false, true, true));
        } else {
            removeAttackSpeedBonus(player);
        }
    }

    // Cooldown ICD pour Shadow Shot (évite spam)
    private final Map<UUID, Long> shadowShotCooldown = new ConcurrentHashMap<>();
    private static final long SHADOW_SHOT_ICD = 500; // 500ms minimum entre les procs

    /**
     * Traite le talent Shadow Shot (T2) - Tir de pistolet
     * 15% chance sur attaque, 150% dégâts, stun 1s, portée 20 blocs
     */
    private void processShadowShot(Player player, LivingEntity target, Talent talent) {
        UUID uuid = player.getUniqueId();

        // ICD check
        Long lastProc = shadowShotCooldown.get(uuid);
        if (lastProc != null && System.currentTimeMillis() - lastProc < SHADOW_SHOT_ICD) {
            return;
        }

        // Récupérer les valeurs du talent
        double[] values = talent.getValues();
        double procChance = values != null && values.length > 0 ? values[0] : 0.15;
        double damageMult = values != null && values.length > 1 ? values[1] : 1.50;
        double maxRange = values != null && values.length > 2 ? values[2] : 20.0;
        long stunDuration = values != null && values.length > 3 ? (long) values[3] : 1000L;

        // Fallback: s'assurer que le multiplicateur est au moins 1.50
        if (damageMult < 1.0) {
            damageMult = 1.50;
        }

        // Roll de chance
        if (Math.random() > procChance) {
            return; // Pas de proc
        }

        // Vérifier la portée
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
        double distance = playerLoc.distance(targetLoc);

        if (distance > maxRange) {
            return; // Trop loin
        }

        // Marquer le cooldown
        shadowShotCooldown.put(uuid, System.currentTimeMillis());

        // Calculer les dégâts (150% des dégâts d'attaque du joueur)
        double baseDamage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double finalDamage = baseDamage * damageMult;

        // === EFFETS VISUELS DU TIR ===

        // Son de tir de pistolet
        player.getWorld().playSound(playerLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.8f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 2.0f);

        // Particules de traînée (du joueur vers la cible)
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
        Location particleLoc = playerLoc.clone();

        for (double d = 0; d < distance; d += 0.5) {
            particleLoc.add(direction.clone().multiply(0.5));
            player.getWorld().spawnParticle(Particle.DUST,
                particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 120), 0.8f));
            player.getWorld().spawnParticle(Particle.SMOKE,
                particleLoc, 1, 0.05, 0.05, 0.05, 0);
        }

        // Impact sur la cible
        target.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 0.3, 0.3, 0.3, 0.1);
        target.getWorld().spawnParticle(Particle.WITCH, targetLoc, 10, 0.2, 0.2, 0.2, 0);
        target.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);

        // === APPLIQUER LES DÉGÂTS ===
        target.damage(finalDamage, player);

        // === APPLIQUER L'ÉTOURDISSEMENT ===
        // Utiliser Slowness IV + Blindness pour simuler un stun
        int stunTicks = (int) (stunDuration / 50); // Convertir ms en ticks
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS, stunTicks, 4, false, false, false));
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS, stunTicks, 0, false, false, false));
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, stunTicks, 2, false, false, false));

        // Feedback visuel au joueur
        player.sendMessage("§5§l[OMBRE] §7Tir d'Ombre! §c" + String.format("%.0f", finalDamage) + " §7dégâts + §9Stun§7!");

        // +1 Point d'Ombre sur proc
        shadowManager.addShadowPoints(uuid, 1);
    }

    // Clé pour l'AttributeModifier de vitesse d'attaque Shadow
    private static final NamespacedKey SHADOW_ATTACK_SPEED_KEY =
        NamespacedKey.fromString("zombiez:shadow_attack_speed");

    /**
     * Applique un bonus de vitesse d'attaque via AttributeModifier
     * Cela fonctionne avec le nouveau système de cooldown vanilla
     */
    private void applyAttackSpeedBonus(Player player, double bonus) {
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        // Supprimer l'ancien modifier s'il existe
        for (var modifier : attackSpeed.getModifiers()) {
            if (modifier.getKey().equals(SHADOW_ATTACK_SPEED_KEY)) {
                attackSpeed.removeModifier(modifier);
                break;
            }
        }

        // Ajouter le nouveau modifier (+30% = multiplier par 0.30)
        // ADD_SCALAR ajoute un pourcentage à la valeur finale
        org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
            SHADOW_ATTACK_SPEED_KEY,
            bonus, // 0.30 = +30%
            org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR
        );
        attackSpeed.addModifier(modifier);
    }

    /**
     * Retire le bonus de vitesse d'attaque
     */
    private void removeAttackSpeedBonus(Player player) {
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null) return;

        for (var modifier : attackSpeed.getModifiers()) {
            if (modifier.getKey().equals(SHADOW_ATTACK_SPEED_KEY)) {
                attackSpeed.removeModifier(modifier);
                break;
            }
        }
    }

    // ==================== MORT D'ENTITE ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        // Vérifier que c'est un joueur Ombre
        if (!shadowManager.isShadowPlayer(killer)) return;

        UUID killerUuid = killer.getUniqueId();
        UUID victimUuid = victim.getUniqueId();

        // === T5: EXECUTION - +2 Points si kill ===
        Talent execution = getActiveTalent(killer, Talent.TalentEffectType.EXECUTION);
        if (execution != null) {
            // Vérifié si c'était une exécution (détecté via metadata)
            if (victim.hasMetadata("shadow_executed")) {
                shadowManager.addShadowPoints(killerUuid, 2);
                victim.removeMetadata("shadow_executed", plugin);
            }
        }

        // === T6: DANSE_MACABRE - Kill marqué = Cascade + Frénésie ===
        Talent danseMacabre = getActiveTalent(killer, Talent.TalentEffectType.DANSE_MACABRE);
        if (danseMacabre != null && shadowManager.isMarkedBy(victimUuid, killerUuid)) {
            // Activer avec le talent et la position du kill pour la cascade
            shadowManager.activateDanseMacabre(killer, danseMacabre, victim.getLocation());
            // Note: les points sont maintenant ajoutés dans activateDanseMacabre
        }

        // Nettoyer la marque
        shadowManager.removeMark(victimUuid);

        // === T7: LAMES SPECTRALES - Invoquer les lames à 5 Points ===
        Talent spectralBlades = getActiveTalent(killer, Talent.TalentEffectType.SHADOW_CLONE);
        if (spectralBlades != null && shadowManager.getShadowPoints(killerUuid) >= 5) {
            // Ne pas réinvoquer si déjà actives
            if (!shadowManager.hasActiveBlades(killerUuid)) {
                shadowManager.summonSpectralBlades(killer, spectralBlades);
            }
            // Note: les points ne sont pas consommés, les lames apparaissent comme bonus
        }
    }

    // ==================== SNEAK (AVATAR ACTIVATION) ====================

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (!shadowManager.isShadowPlayer(player)) return;

        UUID uuid = player.getUniqueId();

        // === T9: SHADOW_AVATAR - Double sneak pour activer ===
        Talent shadowAvatar = getActiveTalent(player, Talent.TalentEffectType.SHADOW_AVATAR);
        if (shadowAvatar != null) {
            long now = System.currentTimeMillis();
            Long lastSneak = lastAvatarSneakTime.get(uuid);

            if (lastSneak != null && now - lastSneak < DOUBLE_SNEAK_WINDOW) {
                // Double sneak détecté!
                shadowManager.activateAvatar(player, shadowAvatar);
                lastAvatarSneakTime.remove(uuid);
            } else {
                lastAvatarSneakTime.put(uuid, now);
            }
        }
    }

    // ==================== DECONNEXION ====================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Nettoyer toutes les données
        shadowManager.cleanupPlayer(uuid);
        lastAvatarSneakTime.remove(uuid);
        lastAttackTime.remove(uuid);
        shadowBladeCooldown.remove(uuid);
        shadowShotCooldown.remove(uuid);

        // Retirer le bonus de vitesse d'attaque
        removeAttackSpeedBonus(player);
    }

    // ==================== UTILITAIRES ====================

    /**
     * Récupère le joueur depuis un damager (direct ou projectile)
     */
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

    /**
     * Récupère un talent actif par son type d'effet
     */
    private Talent getActiveTalent(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentByEffect(player, effectType);
    }

    /**
     * Vérifie si le joueur est un Chasseur
     */
    private boolean isChasseur(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data.hasClass() && data.getSelectedClass() == ClassType.CHASSEUR;
    }
}
