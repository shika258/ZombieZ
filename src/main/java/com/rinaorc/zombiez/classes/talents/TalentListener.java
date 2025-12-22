package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import com.rinaorc.zombiez.combat.PacketDamageIndicator;
import com.rinaorc.zombiez.items.types.StatType;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les effets passifs des talents du Guerrier
 * Gere tous les procs et effets des 40 talents
 */
public class TalentListener implements Listener {

    private final ZombieZPlugin plugin;
    private final TalentManager talentManager;

    // === Cooldowns internes ===
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // === Tracking pour talents specifiques ===

    // Fureur Croissante - stacks de damage
    private final Map<UUID, Integer> furyStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> furyLastHit = new ConcurrentHashMap<>();

    // Charge Devastatrice - tracking sprint
    private final Map<UUID, Long> sprintStartTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> chargeReady = new ConcurrentHashMap<>();

    // Dechainement - tracking multi-kills
    private final Map<UUID, List<Long>> recentKills = new ConcurrentHashMap<>();

    // Colere des Ancetres - riposte buff
    private final Map<UUID, Long> riposteBuffTime = new ConcurrentHashMap<>();

    // Cataclysme - compteur d'attaques
    private final Map<UUID, Integer> attackCounter = new ConcurrentHashMap<>();

    // Immortel - last proc time
    private final Map<UUID, Long> immortalLastProc = new ConcurrentHashMap<>();

    // Cyclone de Rage - active cyclone
    private final Set<UUID> activeCyclones = ConcurrentHashMap.newKeySet();

    // === REMPART - Tracking ===

    // Châtiment - stacks de coups
    private final Map<UUID, Integer> punishmentStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> punishmentLastHit = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> punishmentReady = new ConcurrentHashMap<>();

    // Bouclier Vengeur - compteur de coups pour disque
    private final Map<UUID, Integer> vengefulShieldCounter = new ConcurrentHashMap<>();

    // Fortification - stacks de HP bonus
    private final Map<UUID, Integer> fortifyStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fortifyExpireTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> fortifyBaseHealth = new ConcurrentHashMap<>(); // HP de base avant bonus

    // Marteau du Jugement - cooldown géré par le système standard

    // Écho de Fer - tracking dégâts stockés et stacks
    private final Map<UUID, Double> ironEchoStoredDamage = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ironEchoStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ironEchoFirstStack = new ConcurrentHashMap<>();

    // Avatar du Rempart - dégâts bloqués cumulés
    private final Map<UUID, Double> bulwarkDamageBlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bulwarkAvatarActiveUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bulwarkLastMilestone = new ConcurrentHashMap<>();

    // Represailles Infinies - stacks de riposte
    private final Map<UUID, Integer> retaliationStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> retaliationLastProc = new ConcurrentHashMap<>();

    // Avatar de Vengeance - degats stockes
    private final Map<UUID, Double> storedDamage = new ConcurrentHashMap<>();

    // Avatar de Sang - HP volés cumulés
    private final Map<UUID, Double> bloodStolenHp = new ConcurrentHashMap<>();

    // Seigneur de Guerre - chain execute buff
    private final Map<UUID, Long> chainExecuteBuff = new ConcurrentHashMap<>();

    // Dieu du Sang - last damage dealt time
    private final Map<UUID, Long> lastDamageDealt = new ConcurrentHashMap<>();

    // Extinction - out of combat tracker
    private final Map<UUID, Long> lastCombatTime = new ConcurrentHashMap<>();

    // Bouclier temporaire (Bastion, Seigneur Vampire)
    private final Map<UUID, Double> tempShield = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tempShieldExpiry = new ConcurrentHashMap<>();

    // Vengeance Ardente - burning stacks
    private final Map<UUID, Integer> burningStacks = new ConcurrentHashMap<>();

    // === SYSTEME SEISME SIMPLIFIE ===
    // Compteur de degats de zone pour proc Apocalypse
    private final Map<UUID, Double> aoeDamageCounter = new ConcurrentHashMap<>();
    private static final double APOCALYPSE_THRESHOLD = 500.0; // 500 degats AoE = proc

    // Resonance Sismique - entites marquees pour amplification AoE
    private final Map<UUID, Long> seismicResonanceTargets = new ConcurrentHashMap<>();
    private static final long SEISMIC_RESONANCE_DURATION_MS = 3000;

    // Secousses Residuelles - cooldown de stun par entite
    private final Map<UUID, Long> aftermathStunCooldowns = new ConcurrentHashMap<>();
    private static final long AFTERMATH_STUN_COOLDOWN_MS = 2000;

    // Double sneak detection pour Ragnarok UNIQUEMENT
    private final Map<UUID, Long> lastSneakTime = new ConcurrentHashMap<>();
    private static final long DOUBLE_SNEAK_WINDOW_MS = 400;

    // Tracker paliers Apocalypse (pour feedback sans spam)
    private final Map<UUID, Integer> lastApocalypseMilestone = new ConcurrentHashMap<>();

    // Cache des joueurs Guerriers actifs
    private final Set<UUID> activeGuerriers = ConcurrentHashMap.newKeySet();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 2000;

    // Cache des joueurs Guerrier avec ActionBar enregistrée
    private final Set<UUID> activeGuerrierActionBar = ConcurrentHashMap.newKeySet();

    // Onde de Fracture - compteur de coups
    private final Map<UUID, Integer> fractureWaveHitCounter = new ConcurrentHashMap<>();

    // === SYSTÈME DE MESSAGES TEMPORAIRES POUR ACTIONBAR ===
    // Messages d'événement ponctuels (affichés pendant quelques secondes dans l'ActionBar)
    private final Map<UUID, String> tempEventMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tempEventMessageExpiry = new ConcurrentHashMap<>();
    private static final long EVENT_MESSAGE_DURATION_MS = 2000; // 2 secondes

    public TalentListener(ZombieZPlugin plugin, TalentManager talentManager) {
        this.plugin = plugin;
        this.talentManager = talentManager;
        // Note: registration handled by ZombieZPlugin, not here

        // Taches periodiques
        startPeriodicTasks();
    }

    private void updateGuerriersCache() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate < CACHE_TTL) return;
        lastCacheUpdate = now;

        activeGuerriers.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassData data = plugin.getClassManager().getClassData(player);
            if (data.hasClass() && data.getSelectedClass() == com.rinaorc.zombiez.classes.ClassType.GUERRIER) {
                activeGuerriers.add(player.getUniqueId());
            }
        }
    }

    // ==================== DEGATS INFLIGES ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // IMPORTANT: Ignorer les dégâts secondaires (AoE des talents) pour éviter les cascades infinies
        // Ces dégâts sont marqués avant d'être appliqués via target.setMetadata("zombiez_secondary_damage", ...)
        if (target.hasMetadata("zombiez_secondary_damage")) {
            // Nettoyer la metadata après 1 tick pour ne pas affecter les futurs dégâts normaux
            Bukkit.getScheduler().runTaskLater(plugin, () -> target.removeMetadata("zombiez_secondary_damage", plugin), 1L);
            return;
        }

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != com.rinaorc.zombiez.classes.ClassType.GUERRIER) return;

        double damage = event.getDamage();
        UUID uuid = player.getUniqueId();

        // Tracker dernier combat
        lastCombatTime.put(uuid, System.currentTimeMillis());
        lastDamageDealt.put(uuid, System.currentTimeMillis());

        // === TIER 1 ===

        // Frappe Sismique - GARANTI: chaque attaque = onde de choc (cible incluse dans l'AoE)
        Talent seismicStrike = getActiveTalentIfHas(player, Talent.TalentEffectType.SEISMIC_STRIKE);
        if (seismicStrike != null && !isOnCooldown(uuid, "seismic_strike")) {
            double aoeDamage = damage * seismicStrike.getValue(0);
            double radius = seismicStrike.getValue(1);
            procSeismicStrike(player, target.getLocation(), aoeDamage, radius);
            setCooldown(uuid, "seismic_strike", seismicStrike.getInternalCooldownMs());

            // Accumuler pour Apocalypse Terrestre
            trackAoeDamage(player, aoeDamage);
        }

        // Fureur Croissante
        Talent risingFury = getActiveTalentIfHas(player, Talent.TalentEffectType.RISING_FURY);
        if (risingFury != null) {
            double stackPercent = risingFury.getValue(0);
            double maxBonus = risingFury.getValue(1);
            int maxStacks = (int) (maxBonus / stackPercent);

            furyLastHit.put(uuid, System.currentTimeMillis());
            int stacks = furyStacks.getOrDefault(uuid, 0);
            if (stacks < maxStacks) {
                furyStacks.put(uuid, stacks + 1);
            }

            // Appliquer bonus
            double bonus = furyStacks.get(uuid) * stackPercent;
            damage *= (1 + bonus);
        }

        // Charge Devastatrice
        if (chargeReady.getOrDefault(uuid, false)) {
            Talent charge = getActiveTalentIfHas(player, Talent.TalentEffectType.DEVASTATING_CHARGE);
            if (charge != null) {
                damage *= charge.getValue(1); // 200% multiplier
                chargeReady.put(uuid, false);

                // Stun
                if (target instanceof Mob mob) {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(charge.getValue(2) / 50), 10, false, false));
                }

                // Effet visuel - impact net sans explosion volumineuse
                target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 0.5, 0), 1);
                target.getWorld().playSound(target.getLocation(), Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 0.8f, 0.8f);
            }
        }

        // === TIER 2 ===

        // Châtiment (REMPART) - stacks → buff
        Talent punishment = getActiveTalentIfHas(player, Talent.TalentEffectType.PUNISHMENT);
        if (punishment != null) {
            int stacksNeeded = (int) punishment.getValue(0);
            long window = (long) punishment.getValue(1);
            long now = System.currentTimeMillis();

            // Vérifier si le buff est prêt
            if (punishmentReady.getOrDefault(uuid, false)) {
                // Consommer le buff
                damage *= (1 + punishment.getValue(2)); // +80% dégâts
                double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * punishment.getValue(3);
                applyLifesteal(player, heal);
                punishmentReady.put(uuid, false);
                punishmentStacks.put(uuid, 0);

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
                player.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

                // Effet visuel sacré/jaune (réduit)
                Location targetLoc = target.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 12, 0.3, 0.5, 0.3, 0.1);
                player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 6, 0.3, 0.5, 0.3, 0.03);
                player.getWorld().spawnParticle(Particle.DUST, targetLoc, 8, 0.4, 0.5, 0.4, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f));

                // Message d'activation via système centralisé
                showTempEventMessage(uuid, "§6§l⚔ CHÂTIMENT! §c+" + (int)(punishment.getValue(2)*100) + "% §7dégâts!");
            } else {
                // Accumuler les stacks
                Long lastHit = punishmentLastHit.get(uuid);
                if (lastHit == null || now - lastHit > window) {
                    punishmentStacks.put(uuid, 1);
                } else {
                    int stacks = punishmentStacks.merge(uuid, 1, Integer::sum);
                    if (stacks >= stacksNeeded) {
                        punishmentReady.put(uuid, true);
                        // Son de notification (l'affichage est dans l'ActionBar centralisé)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.5f);
                    } else {
                        // Son de progression (l'affichage est dans l'ActionBar centralisé)
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.8f + (stacks * 0.2f));
                    }
                }
                punishmentLastHit.put(uuid, now);
            }
        }

        // Masse d'Armes - knockback on crit
        Talent maceImpact = getActiveTalentIfHas(player, Talent.TalentEffectType.MACE_IMPACT);
        if (maceImpact != null && event.isCritical()) {
            double knockbackPower = maceImpact.getValue(0);
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            target.setVelocity(direction.multiply(knockbackPower * 0.5).setY(0.3));
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 0.8f);
        }

        // === TIER 3 ===

        // Onde de Fracture - tous les 4 coups = onde sismique en cone
        Talent fractureWave = getActiveTalentIfHas(player, Talent.TalentEffectType.FRACTURE_WAVE);
        if (fractureWave != null && target instanceof LivingEntity) {
            int hitsNeeded = (int) fractureWave.getValue(0);  // 4 coups

            // Incrementer le compteur de coups
            int currentHits = fractureWaveHitCounter.merge(uuid, 1, Integer::sum);

            // Verifier si on a atteint le seuil
            if (currentHits >= hitsNeeded) {
                // Proc l'Onde de Fracture!
                double baseDamage = damage * fractureWave.getValue(1);  // 150%
                double bonusPerHit = fractureWave.getValue(2);           // +25% par ennemi
                double range = fractureWave.getValue(3);                 // 4 blocs
                double coneAngle = fractureWave.getValue(4);             // 60 degres
                double slowPercent = fractureWave.getValue(5);           // 30%
                long slowDurationMs = (long) fractureWave.getValue(6);   // 1500ms

                procFractureWave(player, baseDamage, bonusPerHit, range, coneAngle, slowPercent, slowDurationMs);

                // Reset le compteur
                fractureWaveHitCounter.put(uuid, 0);
            } else {
                // Son de progression (l'affichage est géré par l'ActionBar centralisé)
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f + (currentHits * 0.2f));
            }
        }

        // Bouclier Vengeur (REMPART) - disque pulsant
        Talent vengefulShield = getActiveTalentIfHas(player, Talent.TalentEffectType.VENGEFUL_SHIELD);
        if (vengefulShield != null) {
            int hitsNeeded = (int) vengefulShield.getValue(0);

            // Vérifier si Avatar du Rempart est actif (double fréquence)
            boolean avatarActive = bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis();
            int effectiveHitsNeeded = avatarActive ? Math.max(2, hitsNeeded / 2) : hitsNeeded;

            int currentHits = vengefulShieldCounter.merge(uuid, 1, Integer::sum);

            if (currentHits >= effectiveHitsNeeded) {
                // Lancer le disque!
                vengefulShieldCounter.put(uuid, 0);

                // Utiliser les dégâts de base du joueur pour plus de consistance
                double baseDamage = getPlayerBaseDamage(player);
                double pulseDamage = baseDamage * vengefulShield.getValue(1);
                double pulseRadius = vengefulShield.getValue(2);
                int pulseCount = (int) vengefulShield.getValue(3);
                double explosionDamage = baseDamage * vengefulShield.getValue(4);
                double explosionRadius = vengefulShield.getValue(5);
                double travelDistance = vengefulShield.getValue(6);

                procVengefulShield(player, pulseDamage, pulseRadius, pulseCount, explosionDamage, explosionRadius, travelDistance);
            } else {
                // Son de progression (l'affichage est géré par l'ActionBar centralisé)
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.8f + (currentHits * 0.15f));
            }
        }

        // Écho de Fer ne s'active pas sur les attaques - voir onPlayerTakeDamage

        // Marteau du Jugement - si la cible est <15% HP
        Talent judgmentHammer = getActiveTalentIfHas(player, Talent.TalentEffectType.JUDGMENT_HAMMER);
        if (judgmentHammer != null && !isOnCooldown(uuid, "judgment_hammer")) {
            double hpThreshold = judgmentHammer.getValue(0); // 15%
            double targetMaxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            double targetCurrentHp = target.getHealth();

            if (targetCurrentHp / targetMaxHp <= hpThreshold) {
                // JUGEMENT!
                procJudgmentHammer(player, target, judgmentHammer, damage);
                setCooldown(uuid, "judgment_hammer", (long) judgmentHammer.getValue(4));
            }
        }

        // Avatar du Rempart bonus dégâts si actif (REMPART)
        if (bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
            Talent bulwarkAvatar = getActiveTalentIfHas(player, Talent.TalentEffectType.BULWARK_AVATAR);
            if (bulwarkAvatar != null) {
                damage *= (1 + bulwarkAvatar.getValue(3)); // +50% dégâts
            }
        }

        // Legacy: Vampire de Guerre
        Talent warVampire = getActiveTalentIfHas(player, Talent.TalentEffectType.WAR_VAMPIRE);
        if (warVampire != null) {
            double lifesteal = warVampire.getValue(0);
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (player.getHealth() / maxHp < warVampire.getValue(2)) {
                lifesteal = warVampire.getValue(1); // Boosted lifesteal
            }
            double heal = damage * lifesteal;
            applyLifesteal(player, heal);

            // Tracker pour Avatar de Sang
            bloodStolenHp.merge(uuid, heal, Double::sum);
        }

        // Colere des Ancetres - bonus de riposte
        if (riposteBuffTime.containsKey(uuid)) {
            long buffTime = riposteBuffTime.get(uuid);
            Talent ancestralWrath = getActiveTalentIfHas(player, Talent.TalentEffectType.ANCESTRAL_WRATH);
            if (ancestralWrath != null && System.currentTimeMillis() - buffTime < ancestralWrath.getValue(0)) {
                double bonus = ancestralWrath.getValue(1);

                // Represailles Infinies - stacks additionnels
                Talent infiniteRetaliation = getActiveTalentIfHas(player, Talent.TalentEffectType.INFINITE_RETALIATION);
                if (infiniteRetaliation != null) {
                    int stacks = retaliationStacks.getOrDefault(uuid, 0);
                    bonus += stacks * infiniteRetaliation.getValue(0);
                }

                damage *= (1 + bonus);
                riposteBuffTime.remove(uuid);

                // Vengeance Ardente
                Talent burningVengeance = getActiveTalentIfHas(player, Talent.TalentEffectType.BURNING_VENGEANCE);
                if (burningVengeance != null) {
                    burningStacks.put(uuid, (int) burningVengeance.getValue(0));
                }
            }
        }

        // Executeur
        Talent executioner = getActiveTalentIfHas(player, Talent.TalentEffectType.EXECUTIONER);
        if (executioner != null) {
            double threshold = executioner.getValue(0);
            double maxTargetHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (target.getHealth() / maxTargetHp < threshold) {
                damage *= (1 + executioner.getValue(1));
            }
        }

        // === TIER 4 ===

        // Vengeance Ardente - attaques brulantes
        if (burningStacks.getOrDefault(uuid, 0) > 0) {
            Talent burning = getActiveTalentIfHas(player, Talent.TalentEffectType.BURNING_VENGEANCE);
            if (burning != null) {
                target.setFireTicks((int)(burning.getValue(2) / 50));
                burningStacks.merge(uuid, -1, Integer::sum);
            }
        }

        // === TIER 5 ===

        // Cataclysme
        Talent cataclysm = getActiveTalentIfHas(player, Talent.TalentEffectType.CATACLYSM);
        if (cataclysm != null) {
            int counter = attackCounter.merge(uuid, 1, Integer::sum);
            if (counter >= cataclysm.getValue(0)) {
                attackCounter.put(uuid, 0);
                double aoeDamage = damage * cataclysm.getValue(1);
                double radius = cataclysm.getValue(2);
                procCataclysm(player, aoeDamage, radius);
            }
        }

        // Seigneur de Guerre - chain execute instakill
        if (chainExecuteBuff.containsKey(uuid)) {
            Talent warlord = getActiveTalentIfHas(player, Talent.TalentEffectType.WARLORD);
            if (warlord != null) {
                long buffStart = chainExecuteBuff.get(uuid);
                if (System.currentTimeMillis() - buffStart < warlord.getValue(1)) {
                    double threshold = warlord.getValue(0);
                    double maxTargetHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    if (target.getHealth() / maxTargetHp < threshold) {
                        // Instakill
                        damage = target.getHealth() + 1000;
                        chainExecuteBuff.remove(uuid);
                    }
                } else {
                    chainExecuteBuff.remove(uuid);
                }
            }
        }

        // === TIER 6 ===

        // Avatar de Sang
        Talent bloodAvatar = getActiveTalentIfHas(player, Talent.TalentEffectType.BLOOD_AVATAR);
        if (bloodAvatar != null) {
            double threshold = bloodAvatar.getValue(0);
            if (bloodStolenHp.getOrDefault(uuid, 0.0) >= threshold) {
                bloodStolenHp.put(uuid, 0.0);
                procBloodAvatar(player, damage * bloodAvatar.getValue(1), bloodAvatar.getValue(2), bloodAvatar.getValue(3));
            }
        }

        // Faucheur - auto execute
        Talent reaper = getActiveTalentIfHas(player, Talent.TalentEffectType.REAPER);
        if (reaper != null) {
            target.addScoreboardTag("zombiez_reaper_" + uuid);
        }

        // === TIER 7 ===

        // Apocalypse Terrestre
        Talent apocalypse = getActiveTalentIfHas(player, Talent.TalentEffectType.EARTH_APOCALYPSE);
        if (apocalypse != null && !isOnCooldown(uuid, "apocalypse")) {
            // Doit avoir un talent AoE actif pour proccer
            if (hasAnyAoETalent(player) && Math.random() < apocalypse.getValue(0)) {
                procEarthApocalypse(player, damage * apocalypse.getValue(1), apocalypse.getValue(2), apocalypse.getValue(3));
                setCooldown(uuid, "apocalypse", apocalypse.getInternalCooldownMs());
            }
        }

        // Seigneur Vampire - overheal shield
        Talent vampireLord = getActiveTalentIfHas(player, Talent.TalentEffectType.VAMPIRE_LORD);
        if (vampireLord != null && warVampire != null) {
            double lifesteal = warVampire.getValue(0);
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (player.getHealth() / maxHp < warVampire.getValue(2)) {
                lifesteal = warVampire.getValue(1);
            }
            double heal = damage * lifesteal;

            // Overheal vers shield
            if (player.getHealth() >= maxHp) {
                double maxShield = maxHp * vampireLord.getValue(0);
                double currentShield = tempShield.getOrDefault(uuid, 0.0);
                tempShield.put(uuid, Math.min(maxShield, currentShield + heal));
            }
        }

        // Colosse - bonus melee
        Talent colossus = getActiveTalentIfHas(player, Talent.TalentEffectType.COLOSSUS);
        if (colossus != null) {
            damage *= (1 + colossus.getValue(1)); // +30% melee damage
        }

        // === TIER 8 ===

        // Dieu du Sang - tracking (handled in damage received)

        // Avatar de Vengeance - crouch + attack release
        Talent vengeanceAvatar = getActiveTalentIfHas(player, Talent.TalentEffectType.VENGEANCE_AVATAR);
        if (vengeanceAvatar != null && player.isSneaking()) {
            double stored = storedDamage.getOrDefault(uuid, 0.0);
            if (stored > 0) {
                procVengeanceRelease(player, stored, vengeanceAvatar.getValue(2));
                storedDamage.put(uuid, 0.0);
            }
        }

        // Extinction - first hit instakill (balance: seulement mobs normaux, pas les boss/elite)
        Talent extinction = getActiveTalentIfHas(player, Talent.TalentEffectType.EXTINCTION);
        if (extinction != null) {
            long lastCombat = lastCombatTime.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastCombat > extinction.getValue(0)) {
                // First hit!
                double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();

                // Detecter si c'est un boss/elite via metadata ZombieZ ou tags
                boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                        target.getScoreboardTags().contains("elite") ||
                                        target.hasMetadata("zombiez_boss") ||
                                        target.hasMetadata("zombiez_elite");

                // Si pas de metadata, utiliser le seuil de HP (200+ HP = considéré comme elite)
                if (!isBossOrElite && maxHp >= 200) {
                    isBossOrElite = true;
                }

                if (isBossOrElite) {
                    // Boss/Elite = 25% HP (reduit de 30%)
                    damage = maxHp * Math.min(extinction.getValue(1), 0.25);
                } else {
                    // Normal = instakill
                    damage = target.getHealth() + 1000;
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
                player.getWorld().spawnParticle(Particle.FLASH, target.getLocation(), 1);
            }
        }

        event.setDamage(damage);
    }

    // ==================== DEGATS RECUS ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageReceived(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != com.rinaorc.zombiez.classes.ClassType.GUERRIER) return;

        double damage = event.getDamage();
        UUID uuid = player.getUniqueId();

        // Tracker dernier combat
        lastCombatTime.put(uuid, System.currentTimeMillis());

        // === TIER 1 ===

        // Peau de Fer
        Talent ironSkin = getActiveTalentIfHas(player, Talent.TalentEffectType.IRON_SKIN);
        if (ironSkin != null) {
            damage *= (1 - ironSkin.getValue(0)); // -15% DR
        }

        // Posture Défensive (REMPART) - blocage passif + riposte
        Talent defensiveStance = getActiveTalentIfHas(player, Talent.TalentEffectType.DEFENSIVE_STANCE);
        if (defensiveStance != null && event.getDamager() instanceof LivingEntity attacker) {
            double blockChance = defensiveStance.getValue(0);

            // Avatar du Rempart = 100% blocage
            if (bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
                blockChance = 1.0;
            }

            if (Math.random() < blockChance) {
                // Blocage réussi!
                double originalDamage = damage;

                // Réduction des dégâts (50% bloqué)
                damage *= 0.5;

                // Soin
                double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * defensiveStance.getValue(1);
                applyLifesteal(player, heal);

                // Riposte - dégâts à l'attaquant
                double riposteDamage = originalDamage * defensiveStance.getValue(2);
                attacker.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                attacker.damage(riposteDamage, player);

                // Effets visuels
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);

                // Tracker blocage pour les autres talents Rempart
                handleRempartBlock(player, uuid, originalDamage);

                // Écho de Fer - stocker les dégâts bloqués
                handleIronEcho(player, uuid, originalDamage);

                // Message d'événement via système centralisé
                showTempEventMessage(uuid, "§6🛡 BLOQUÉ! §a+" + String.format("%.1f", heal) + " PV §c→ " + String.format("%.1f", riposteDamage) + " riposte");
            }
        }

        // === TIER 2 ===

        // Bastion
        Talent bastion = getActiveTalentIfHas(player, Talent.TalentEffectType.BASTION);
        if (bastion != null && player.isBlocking() && !isOnCooldown(uuid, "bastion")) {
            double shieldAmount = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bastion.getValue(0);
            long duration = (long) bastion.getValue(1);
            applyTempShield(player, shieldAmount, duration);
            setCooldown(uuid, "bastion", (long) bastion.getValue(2));

            // Tracker blocage pour les talents Rempart
            handleRempartBlock(player, uuid, damage);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
        }

        // Legacy: Frenetique
        Talent frenetic = getActiveTalentIfHas(player, Talent.TalentEffectType.FRENETIC);
        if (frenetic != null) {
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (player.getHealth() / maxHp < frenetic.getValue(0)) {
                // Low HP mode - appliquer buffs
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false));
            }
        }

        // === TIER 3 ===

        // Colere des Ancetres - trigger riposte
        Talent ancestralWrath = getActiveTalentIfHas(player, Talent.TalentEffectType.ANCESTRAL_WRATH);
        if (ancestralWrath != null) {
            riposteBuffTime.put(uuid, System.currentTimeMillis());

            // Represailles Infinies - stack
            Talent infiniteRetaliation = getActiveTalentIfHas(player, Talent.TalentEffectType.INFINITE_RETALIATION);
            if (infiniteRetaliation != null) {
                retaliationLastProc.put(uuid, System.currentTimeMillis());
                int maxStacks = (int) (infiniteRetaliation.getValue(1) / infiniteRetaliation.getValue(0));
                int current = retaliationStacks.getOrDefault(uuid, 0);
                retaliationStacks.put(uuid, Math.min(current + 1, maxStacks));
            }
        }

        // Titan Immuable
        Talent titan = getActiveTalentIfHas(player, Talent.TalentEffectType.IMMOVABLE_TITAN);
        if (titan != null) {
            // No knockback is handled elsewhere
            // Check if stationary
            // For simplicity, we'll give DR bonus if sneaking
            if (player.isSneaking()) {
                damage *= (1 - titan.getValue(1));
            }
        }

        // === TIER 5 ===

        // Immortel
        Talent immortal = getActiveTalentIfHas(player, Talent.TalentEffectType.IMMORTAL);
        if (immortal != null) {
            long lastProc = immortalLastProc.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastProc > immortal.getValue(0)) {
                if (player.getHealth() - damage <= 0) {
                    // Cheat death!
                    event.setCancelled(true);
                    player.setHealth(1);
                    immortalLastProc.put(uuid, System.currentTimeMillis());

                    // Invulnerability
                    player.setInvulnerable(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.setInvulnerable(false), (long)(immortal.getValue(1) / 50));

                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 1, 1, 1, 0.1);
                    if (shouldSendTalentMessage(player)) {
                        player.sendMessage("§6§l+ IMMORTEL! §7Vous avez triomphe de la mort!");
                    }
                    return;
                }
            }
        }

        // Aegis Eternal
        Talent aegis = getActiveTalentIfHas(player, Talent.TalentEffectType.ETERNAL_AEGIS);
        if (aegis != null && player.isBlocking()) {
            // Perfect parry window - simplified check
            // In real implementation, would need more precise timing
            double reflect = damage * aegis.getValue(1);
            if (event.getDamager() instanceof LivingEntity attacker) {
                attacker.damage(reflect, player);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 2.0f);
            }
        }

        // === TIER 6 ===

        // Bastille Imprenable
        Talent impregnable = getActiveTalentIfHas(player, Talent.TalentEffectType.IMPREGNABLE_BASTION);
        if (impregnable != null) {
            // Double existing DR
            if (ironSkin != null) {
                damage *= (1 - ironSkin.getValue(0)); // Apply DR again
            }
        }

        // === TIER 7 ===

        // Écho de Fer - stocker les dégâts reçus (après réductions)
        handleIronEcho(player, uuid, damage);

        // === TIER 8 ===

        // Aura de Défi (REMPART) - réflexion + réduction de dégâts des ennemis
        Talent defianceAura = getActiveTalentIfHas(player, Talent.TalentEffectType.DEFIANCE_AURA);
        if (defianceAura != null && event.getDamager() instanceof LivingEntity attacker) {
            // Réflexion des dégâts mêlée
            double reflect = damage * defianceAura.getValue(2);
            attacker.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            attacker.damage(reflect, player);

            // Effet visuel de réflexion
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.3);
        }

        // Nemesis - thorns
        Talent nemesis = getActiveTalentIfHas(player, Talent.TalentEffectType.NEMESIS);
        if (nemesis != null && event.getDamager() instanceof LivingEntity attacker) {
            double reflect = damage * nemesis.getValue(0);
            attacker.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            attacker.damage(reflect, player);
        }

        // Colosse - HP bonus handled elsewhere
        Talent colossus = getActiveTalentIfHas(player, Talent.TalentEffectType.COLOSSUS);
        if (colossus != null) {
            // Speed malus handled in periodic task
        }

        // === TIER 8 ===

        // Dieu du Sang - DR while attacking (capped a 50%)
        Talent bloodGod = getActiveTalentIfHas(player, Talent.TalentEffectType.BLOOD_GOD);
        if (bloodGod != null) {
            long lastDamage = lastDamageDealt.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastDamage < bloodGod.getValue(0)) {
                double dr = Math.min(bloodGod.getValue(1), 0.50); // Cap standardise a 50% pour toutes les classes
                damage *= (1 - dr);
            }
        }

        // Avatar de Vengeance - stockage
        Talent vengeance = getActiveTalentIfHas(player, Talent.TalentEffectType.VENGEANCE_AVATAR);
        if (vengeance != null) {
            double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double maxStore = maxHp * vengeance.getValue(0);
            double current = storedDamage.getOrDefault(uuid, 0.0);
            storedDamage.put(uuid, Math.min(maxStore, current + damage));
        }

        // Citadelle Vivante
        Talent citadel = getActiveTalentIfHas(player, Talent.TalentEffectType.LIVING_CITADEL);
        if (citadel != null && player.isSneaking() && !isOnCooldown(uuid, "citadel")) {
            event.setCancelled(true);
            procLivingCitadel(player, citadel);
            setCooldown(uuid, "citadel", (long) citadel.getValue(3));
        }

        // Bouclier temporaire absorbe les degats
        double shield = tempShield.getOrDefault(uuid, 0.0);
        if (shield > 0) {
            if (shield >= damage) {
                tempShield.put(uuid, shield - damage);
                damage = 0;
            } else {
                damage -= shield;
                tempShield.put(uuid, 0.0);

                // Forteresse - explosion quand shield expire
                Talent fortress = getActiveTalentIfHas(player, Talent.TalentEffectType.FORTRESS);
                if (fortress != null) {
                    procFortressExplosion(player, shield * fortress.getValue(0), fortress.getValue(1));
                }
            }
        }

        event.setDamage(Math.max(0, damage));

        // === REMPART - Immunité knockback ===
        // Si le joueur est en Avatar du Rempart, annuler le knockback
        if (isBulwarkAvatar(player)) {
            // Annuler le velocity au prochain tick
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setVelocity(player.getVelocity().setX(0).setZ(0));
                }
            }, 1L);
        }
    }

    // ==================== KILLS ====================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!(target.getKiller() instanceof Player player)) return;

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != com.rinaorc.zombiez.classes.ClassType.GUERRIER) return;

        UUID uuid = player.getUniqueId();

        // === TIER 1 ===

        // Soif de Sang
        Talent bloodthirst = getActiveTalentIfHas(player, Talent.TalentEffectType.BLOODTHIRST);
        if (bloodthirst != null) {
            double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bloodthirst.getValue(0);
            applyLifesteal(player, heal);
        }

        // === TIER 2 ===

        // Dechainement
        Talent unleash = getActiveTalentIfHas(player, Talent.TalentEffectType.UNLEASH);
        if (unleash != null) {
            List<Long> kills = recentKills.computeIfAbsent(uuid, k -> new ArrayList<>());
            kills.add(System.currentTimeMillis());

            // Nettoyer les vieux kills
            long window = (long) unleash.getValue(1);
            kills.removeIf(t -> System.currentTimeMillis() - t > window);

            if (kills.size() >= unleash.getValue(0)) {
                kills.clear();
                procUnleash(player, unleash.getValue(2), unleash.getValue(3));
            }
        }

        // === TIER 4 ===

        // Moisson Sanglante
        Talent bloodyHarvest = getActiveTalentIfHas(player, Talent.TalentEffectType.BLOODY_HARVEST);
        if (bloodyHarvest != null) {
            double maxTargetHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (target.getHealth() / maxTargetHp < bloodyHarvest.getValue(0)) {
                // Execute kill
                double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bloodyHarvest.getValue(1);
                applyLifesteal(player, heal);
                // Reset sprint - just give speed boost
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
            }
        }

        // === TIER 5 ===

        // Seigneur de Guerre - chain execute
        Talent warlord = getActiveTalentIfHas(player, Talent.TalentEffectType.WARLORD);
        if (warlord != null) {
            double maxTargetHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (target.getHealth() / maxTargetHp < warlord.getValue(0)) {
                chainExecuteBuff.put(uuid, System.currentTimeMillis());
            }
        }

        // === TIER 6 ===

        // Faucheur - auto execute (checked in periodic task)
    }

    // ==================== SPRINT ====================

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != com.rinaorc.zombiez.classes.ClassType.GUERRIER) return;

        UUID uuid = player.getUniqueId();

        if (event.isSprinting()) {
            sprintStartTime.put(uuid, System.currentTimeMillis());

            // Cyclone de Rage
            Talent cyclone = getActiveTalentIfHas(player, Talent.TalentEffectType.RAGE_CYCLONE);
            if (cyclone != null && !activeCyclones.contains(uuid)) {
                activeCyclones.add(uuid);
                startRageCyclone(player, cyclone);
            }
        } else {
            // Check Charge Devastatrice
            Talent charge = getActiveTalentIfHas(player, Talent.TalentEffectType.DEVASTATING_CHARGE);
            if (charge != null) {
                long sprintStart = sprintStartTime.getOrDefault(uuid, System.currentTimeMillis());
                if (System.currentTimeMillis() - sprintStart >= charge.getValue(0)) {
                    chargeReady.put(uuid, true);
                    if (shouldSendTalentMessage(player)) {
                        player.sendMessage("§6§l+ CHARGE PRETE! §7Votre prochaine attaque sera devastatrice!");
                    }
                }
            }

            activeCyclones.remove(uuid);
        }
    }

    // ==================== DOUBLE SNEAK - ACTIVATIONS MANUELLES ====================

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass() || data.getSelectedClass() != com.rinaorc.zombiez.classes.ClassType.GUERRIER) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastSneak = lastSneakTime.getOrDefault(uuid, 0L);

        // Double sneak detecte?
        if (now - lastSneak < DOUBLE_SNEAK_WINDOW_MS) {
            // Reset pour eviter triple-sneak
            lastSneakTime.remove(uuid);
            handleDoubleSneak(player);
        } else {
            lastSneakTime.put(uuid, now);
        }
    }

    /**
     * Gere l'activation par double sneak - RAGNAROK et CHARGE DU BASTION
     */
    private void handleDoubleSneak(Player player) {
        UUID uuid = player.getUniqueId();

        // Charge du Bastion (REMPART) - priorité
        Talent bastionCharge = getActiveTalentIfHas(player, Talent.TalentEffectType.BASTION_CHARGE);
        if (bastionCharge != null) {
            if (!isOnCooldown(uuid, "bastion_charge")) {
                procBastionCharge(player, bastionCharge);
                setCooldown(uuid, "bastion_charge", (long) bastionCharge.getValue(4));
            } else {
                long remaining = getCooldownRemaining(uuid, "bastion_charge");
                showTempEventMessage(uuid, "§c⏳ Charge du Bastion: " + (remaining / 1000) + "s", 1500);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            return; // Ne pas activer Ragnarok si on a Charge du Bastion
        }

        // Ragnarok - L'ULTIME du Guerrier Seisme
        Talent ragnarok = getActiveTalentIfHas(player, Talent.TalentEffectType.RAGNAROK);
        if (ragnarok != null) {
            if (!isOnCooldown(uuid, "ragnarok")) {
                procRagnarok(player, ragnarok);
                setCooldown(uuid, "ragnarok", (long) ragnarok.getValue(0));
            } else {
                // Feedback cooldown via système centralisé
                long remaining = getCooldownRemaining(uuid, "ragnarok");
                showTempEventMessage(uuid, "§c⏳ Ragnarok: " + (remaining / 1000) + "s", 1500);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        }
    }

    /**
     * Retourne le temps restant d'un cooldown en ms
     */
    private long getCooldownRemaining(UUID uuid, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long cooldownEnd = playerCooldowns.get(ability);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    /**
     * Track les degats de zone pour proc Apocalypse Terrestre
     */
    private void trackAoeDamage(Player player, double damage) {
        UUID uuid = player.getUniqueId();

        Talent apocalypse = getActiveTalentIfHas(player, Talent.TalentEffectType.EARTH_APOCALYPSE);
        if (apocalypse == null) return;

        double current = aoeDamageCounter.merge(uuid, damage, Double::sum);

        // Feedback progression avec tracking de paliers (25%, 50%, 75%)
        // L'affichage de la barre de progression est géré par l'ActionBar centralisé
        int milestone = (int) ((current / APOCALYPSE_THRESHOLD) * 4); // 0, 1, 2, 3, 4
        int lastMilestone = lastApocalypseMilestone.getOrDefault(uuid, 0);

        if (milestone > lastMilestone && milestone < 4) {
            lastApocalypseMilestone.put(uuid, milestone);
            // Son de milestone (l'affichage est dans l'ActionBar centralisé)
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 0.8f + milestone * 0.15f);
        }

        // Proc automatique!
        if (current >= APOCALYPSE_THRESHOLD && !isOnCooldown(uuid, "apocalypse")) {
            aoeDamageCounter.put(uuid, 0.0);
            lastApocalypseMilestone.put(uuid, 0); // Reset milestones

            // Message d'activation spectaculaire via système centralisé
            showTempEventMessage(uuid, "§6§l🌋 APOCALYPSE TERRESTRE! 🌋", 2500);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);

            procEarthApocalypse(player, 10 * apocalypse.getValue(1), apocalypse.getValue(2), apocalypse.getValue(3));
            setCooldown(uuid, "apocalypse", apocalypse.getInternalCooldownMs());
        }
    }

    // ==================== TACHES PERIODIQUES OPTIMISEES ====================

    private void startPeriodicTasks() {
        // FAST TICK (10L = 0.5s) - Execute auras
        new BukkitRunnable() {
            @Override
            public void run() {
                updateGuerriersCache();
                if (activeGuerriers.isEmpty()) return;

                // Faucheur auto execute + Ange de la Mort
                for (UUID uuid : activeGuerriers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    // Faucheur
                    Talent reaper = getActiveTalentIfHas(player, Talent.TalentEffectType.REAPER);
                    if (reaper != null) {
                        double threshold = reaper.getValue(0);
                        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof LivingEntity target && entity.getScoreboardTags().contains("zombiez_reaper_" + uuid)) {
                                double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                                // Seulement instakill les mobs < 50 HP max
                                if (target.getHealth() / maxHp < threshold && maxHp <= 50) {
                                    // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                                    target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                    target.damage(target.getHealth() + 100, player);
                                }
                            }
                        }
                    }

                    // Ange de la Mort
                    Talent deathAngel = getActiveTalentIfHas(player, Talent.TalentEffectType.DEATH_ANGEL);
                    if (deathAngel != null) {
                        double radius = Math.min(deathAngel.getValue(0), 10.0);
                        double threshold = deathAngel.getValue(1);
                        double chance = deathAngel.getValue(2);

                        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                            if (entity instanceof LivingEntity target && !(entity instanceof Player)) {
                                double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                                // Seulement instakill les mobs < 50 HP max
                                if (target.getHealth() / maxHp < threshold && Math.random() < chance && maxHp <= 50) {
                                    // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                                    target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                                    target.damage(target.getHealth() + 100, player);
                                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 5, 0.3, 0.3, 0.3, 0.02);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);

        // NORMAL TICK (20L = 1s) - Regen, decay, stacks cleanup
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGuerriers.isEmpty()) return;
                long now = System.currentTimeMillis();

                // Cleanup fury stacks
                furyLastHit.forEach((uuid, time) -> {
                    if (now - time > 3000) furyStacks.remove(uuid);
                });

                // Cleanup retaliation stacks
                retaliationLastProc.forEach((uuid, time) -> {
                    if (now - time > 10000) retaliationStacks.remove(uuid);
                });

                for (UUID uuid : activeGuerriers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    // Bastille Imprenable regen
                    Talent impregnable = getActiveTalentIfHas(player, Talent.TalentEffectType.IMPREGNABLE_BASTION);
                    if (impregnable != null) {
                        long lastCombat = lastCombatTime.getOrDefault(uuid, 0L);
                        if (now - lastCombat < 10000) {
                            double regen = player.getAttribute(Attribute.MAX_HEALTH).getValue() * impregnable.getValue(1);
                            applyLifesteal(player, Math.min(regen, player.getMaxHealth() * 0.03)); // Cap 3%/s
                        }
                    }

                    // Dieu du Sang regen
                    Talent bloodGod = getActiveTalentIfHas(player, Talent.TalentEffectType.BLOOD_GOD);
                    if (bloodGod != null) {
                        long lastDamage = lastDamageDealt.getOrDefault(uuid, 0L);
                        if (now - lastDamage < bloodGod.getValue(0)) {
                            double regen = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bloodGod.getValue(2);
                            applyLifesteal(player, Math.min(regen, player.getMaxHealth() * 0.05)); // Cap 5%/s
                        }
                    }

                    // Vampire Lord shield decay
                    Talent vampireLord = getActiveTalentIfHas(player, Talent.TalentEffectType.VAMPIRE_LORD);
                    if (vampireLord != null) {
                        long lastCombat = lastCombatTime.getOrDefault(uuid, 0L);
                        if (now - lastCombat > 5000) {
                            double shield = tempShield.getOrDefault(uuid, 0.0);
                            if (shield > 0) {
                                double decay = player.getAttribute(Attribute.MAX_HEALTH).getValue() * vampireLord.getValue(1);
                                tempShield.put(uuid, Math.max(0, shield - decay));
                            }
                        }
                    }

                    // Colosse + Peau de Fer slowness
                    Talent colossus = getActiveTalentIfHas(player, Talent.TalentEffectType.COLOSSUS);
                    if (colossus != null) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                    }
                    Talent ironSkin = getActiveTalentIfHas(player, Talent.TalentEffectType.IRON_SKIN);
                    if (ironSkin != null) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                    }

                    // === REMPART - Expiration Fortification ===
                    Talent fortify = getActiveTalentIfHas(player, Talent.TalentEffectType.FORTIFY);
                    if (fortify != null) {
                        Long expireTime = fortifyExpireTime.get(uuid);
                        int stacks = fortifyStacks.getOrDefault(uuid, 0);

                        if (stacks > 0 && expireTime != null && now >= expireTime) {
                            // Expiration - retirer le bonus de HP
                            removeFortifyBonus(player, uuid);

                            // Message d'expiration via système centralisé
                            showTempEventMessage(uuid, "§8⚔ Fortification expirée...");
                            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                        }
                    }

                    // === REMPART - Aura de Défi (effets périodiques) ===
                    Talent defianceAura = getActiveTalentIfHas(player, Talent.TalentEffectType.DEFIANCE_AURA);
                    if (defianceAura != null) {
                        double radius = defianceAura.getValue(0);
                        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                            if (entity instanceof LivingEntity target && !(entity instanceof Player)) {
                                // Appliquer Glowing pour visibilité
                                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
                                // Appliquer Weakness pour -20% dégâts (approximation)
                                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));
                            }
                        }

                        // Animation améliorée de l'aura dorée
                        Location loc = player.getLocation();
                        World world = player.getWorld();
                        long tick = world.getGameTime();
                        double rotationOffset = (tick % 60) * (Math.PI * 2 / 60); // Rotation complète en 3s

                        // Anneau principal au sol (doré, rotatif) - réduit
                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                            double x = radius * Math.cos(angle + rotationOffset);
                            double z = radius * Math.sin(angle + rotationOffset);
                            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z),
                                1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.2f));
                        }

                        // Anneau secondaire (contre-rotation, orange) - réduit
                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                            double x = (radius - 0.5) * Math.cos(angle - rotationOffset * 0.5);
                            double z = (radius - 0.5) * Math.sin(angle - rotationOffset * 0.5);
                            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.15, z),
                                1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 140, 30), 0.8f));
                        }

                        // Piliers de lumière sacrée aux 2 points opposés (réduit)
                        for (int i = 0; i < 2; i++) {
                            double pillarAngle = Math.PI * i + rotationOffset * 0.3;
                            double px = (radius - 0.3) * Math.cos(pillarAngle);
                            double pz = (radius - 0.3) * Math.sin(pillarAngle);
                            for (double y = 0; y < 2.0; y += 0.6) {
                                world.spawnParticle(Particle.END_ROD, loc.clone().add(px, y, pz),
                                    1, 0.05, 0.1, 0.05, 0.01);
                            }
                        }

                        // Particules montantes centrales (effet sacré) - réduit
                        if (tick % 10 == 0) {
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI * 2 / 3) {
                                double sparkX = (radius * 0.5) * Math.cos(angle + rotationOffset * 2);
                                double sparkZ = (radius * 0.5) * Math.sin(angle + rotationOffset * 2);
                                world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(sparkX, 0.5, sparkZ),
                                    1, 0.1, 0.3, 0.1, 0.02);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // TREMOR TICK (20L = 1s) - Ondes sismiques EN COURANT (comme Whirlwind de D4)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGuerriers.isEmpty()) return;
                for (UUID uuid : activeGuerriers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent tremor = getActiveTalentIfHas(player, Talent.TalentEffectType.ETERNAL_TREMOR);
                    if (tremor != null && player.isSprinting()) {
                        // Genere des ondes sismiques EN COURANT
                        double damage = 5 * tremor.getValue(1);
                        double radius = tremor.getValue(2);
                        procTremor(player, tremor.getValue(1), radius);

                        // Contribue a Apocalypse
                        trackAoeDamage(player, damage);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes

        // ActionBar Registration Task (20L = 1s)
        new BukkitRunnable() {
            @Override
            public void run() {
                registerActionBarProviders();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Note: Ragnarok est active par double sneak (handleDoubleSneak)
    }

    // ==================== PROCS ====================

    private void procSeismicStrike(Player player, Location center, double damage, double radius) {
        // Effet visuel epure: cercle de particules au sol
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location particleLoc = new Location(player.getWorld(), x, center.getY() + 0.1, z);
            player.getWorld().spawnParticle(Particle.DUST, particleLoc,
                2, 0.1, 0, 0.1, 0, new Particle.DustOptions(Color.fromRGB(139, 119, 101), 1.5f));
            // Debris de roche
            player.getWorld().spawnParticle(Particle.BLOCK, particleLoc,
                2, 0.15, 0.1, 0.15, 0, Material.STONE.createBlockData());
        }
        // Impact central - debris de pierre
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 5, 0.3, 0.1, 0.3, 0.01);
        player.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.2, 0),
            8, 0.4, 0.15, 0.4, 0, Material.COBBLESTONE.createBlockData());

        // Son
        player.getWorld().playSound(center, Sound.BLOCK_DECORATED_POT_BREAK, 0.8f, 0.6f);

        // Degats a TOUS les ennemis dans la zone (cible principale incluse)
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }

        // Echo de Guerre
        Talent echo = getActiveTalentIfHas(player, Talent.TalentEffectType.WAR_ECHO);
        if (echo != null && Math.random() < echo.getValue(0)) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                procSeismicStrikeNoEcho(player, center, damage, radius), (long)(echo.getValue(1) / 50));
        }
    }

    private void procSeismicStrikeNoEcho(Player player, Location center, double damage, double radius) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1);
    }

    /**
     * Onde de Fracture - Cone AoE sismique devant le joueur
     */
    private void procFractureWave(Player player, double baseDamage, double bonusPerHit,
                                   double range, double coneAngle, double slowPercent, long slowDurationMs) {
        Location origin = player.getLocation();
        Vector direction = origin.getDirection().setY(0).normalize();
        double halfAngleRad = Math.toRadians(coneAngle / 2);

        // Collecter les cibles dans le cone
        List<LivingEntity> hitTargets = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity target) || entity instanceof Player) continue;

            Vector toEntity = entity.getLocation().toVector().subtract(origin.toVector()).setY(0);
            double distance = toEntity.length();
            if (distance > range || distance < 0.5) continue;

            // Verifier l'angle du cone
            double angle = Math.acos(toEntity.normalize().dot(direction));
            if (angle <= halfAngleRad) {
                hitTargets.add(target);
            }
        }

        // Calculer les degats (bonus par ennemi touche)
        int hitCount = hitTargets.size();
        double totalDamage = baseDamage * (1 + bonusPerHit * hitCount);

        // Appliquer les degats et le slow
        int slowTicks = (int) (slowDurationMs / 50);
        int slowLevel = (int) Math.round(slowPercent / 0.15) - 1; // ~30% = Slowness I
        slowLevel = Math.max(0, Math.min(slowLevel, 3));

        for (LivingEntity target : hitTargets) {
            target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            target.damage(totalDamage, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, slowLevel, false, true, true));
        }

        // === EFFETS VISUELS SPECTACULAIRES ===
        spawnFractureWaveVisuals(player, origin, direction, range, halfAngleRad, hitCount);

        // Feedback via système centralisé
        String msg = hitCount > 0
            ? "§7§l⚡ §6ONDE DE FRACTURE! §7" + hitCount + " cible(s) §c" + String.format("%.0f", totalDamage) + " dmg"
            : "§7§l⚡ §6Onde de Fracture §7(aucune cible)";
        showTempEventMessage(player.getUniqueId(), msg);

        // Contribution au systeme Apocalypse
        if (hitCount > 0) {
            trackAoeDamage(player, totalDamage);
        }
    }

    /**
     * Effets visuels de l'Onde de Fracture - fissure au sol + onde de choc
     */
    private void spawnFractureWaveVisuals(Player player, Location origin, Vector direction,
                                           double range, double halfAngleRad, int hitCount) {
        World world = player.getWorld();

        // Son d'impact sismique
        world.playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.5f);
        world.playSound(origin, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.6f);

        // Particules de fissure au sol (lignes qui partent du joueur)
        new BukkitRunnable() {
            double progress = 0;
            final double step = 0.5;

            @Override
            public void run() {
                if (progress > range) {
                    cancel();
                    return;
                }

                // Creer plusieurs lignes dans le cone
                for (double angleOffset = -halfAngleRad; angleOffset <= halfAngleRad; angleOffset += halfAngleRad / 2) {
                    double cos = Math.cos(angleOffset);
                    double sin = Math.sin(angleOffset);

                    // Rotation du vecteur direction
                    double rotX = direction.getX() * cos - direction.getZ() * sin;
                    double rotZ = direction.getX() * sin + direction.getZ() * cos;

                    Location particleLoc = origin.clone().add(rotX * progress, 0.1, rotZ * progress);

                    // Fissure au sol
                    world.spawnParticle(Particle.BLOCK, particleLoc, 3, 0.2, 0, 0.2, 0,
                        Material.CRACKED_DEEPSLATE_TILES.createBlockData());

                    // Poussiere sismique
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc.clone().add(0, 0.3, 0),
                        1, 0.1, 0.1, 0.1, 0.01);
                }

                // Onde de choc principale au front
                if (progress > 0.5) {
                    Location frontLoc = origin.clone().add(direction.clone().multiply(progress));
                    world.spawnParticle(Particle.SONIC_BOOM, frontLoc.add(0, 0.5, 0), 1);
                }

                progress += step;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Flash d'impact si on a touche des cibles
        if (hitCount > 0) {
            world.spawnParticle(Particle.FLASH, origin.clone().add(0, 1, 0), 1);
            world.spawnParticle(Particle.EXPLOSION, origin.clone().add(direction.multiply(range / 2)).add(0, 0.5, 0), 1);
        }
    }

    /**
     * NOTE: showFractureWaveProgress supprimée - progression affichée dans l'ActionBar centralisé
     */

    private void procUnleash(Player player, double damageMultiplier, double radius) {
        Location center = player.getLocation();

        // Onde de choc compacte au lieu d'explosions volumineuses
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center, 8, 1.5, 0.5, 1.5, 0);
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 1.2f);

        double baseDamage = 10; // Base damage
        double damage = baseDamage * damageMultiplier;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                target.damage(damage, player);
            }
        }

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§c§l+ DECHAINEMENT! §7Explosion devastatrice!");
        }
    }

    private void procCataclysm(Player player, double damage, double radius) {
        Location center = player.getLocation();

        // Impact puissant mais lisible - pas d'explosion volumineuse
        player.getWorld().spawnParticle(Particle.FLASH, center, 1);
        player.getWorld().spawnParticle(Particle.DUST, center, 15, radius / 2, 0.3, radius / 2, 0,
            new Particle.DustOptions(Color.fromRGB(255, 100, 50), 1.8f));
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 6, 0.5, 0.2, 0.5, 0.02);
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.8f);

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§c§l+ CATACLYSME! §7Une explosion massive ravage tout!");
        }
    }

    private void procBloodAvatar(Player player, double damage, double radius, double selfHeal) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.DUST, center, 100, radius/2, 1, radius/2, 0.1,
            new Particle.DustOptions(Color.RED, 2));
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                target.damage(damage, player);
            }
        }

        double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * selfHeal;
        applyLifesteal(player, heal);

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§4§l+ AVATAR DE SANG! §7Le sang explose autour de vous!");
        }
    }

    private void procEarthApocalypse(Player player, double damage, double radius, double stunMs) {
        Location center = player.getLocation();

        // Effet de tremblement de terre - onde concentrique elegante
        for (double r = 2; r <= radius; r += 2.5) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location particleLoc = new Location(player.getWorld(), x, center.getY() + 0.2, z);
                player.getWorld().spawnParticle(Particle.DUST, particleLoc,
                    3, 0.2, 0.1, 0.2, 0, new Particle.DustOptions(Color.fromRGB(60, 60, 60), 2.0f));
                // Debris de deepslate fissure
                player.getWorld().spawnParticle(Particle.BLOCK, particleLoc,
                    2, 0.2, 0.15, 0.2, 0, Material.CRACKED_DEEPSLATE_BRICKS.createBlockData());
            }
        }
        // Colonne centrale + explosion de debris
        player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 8, 0.3, 0.5, 0.3, 0.02);
        player.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.3, 0),
            15, 1.0, 0.3, 1.0, 0.1, Material.DEEPSLATE.createBlockData());

        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.5f);

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
                if (target instanceof Mob mob) {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(stunMs / 50), 10, false, false));
                }
            }
        }

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§5§l+ APOCALYPSE! §7La terre tremble sous votre puissance!");
        }
    }

    private void procTremor(Player player, double damageMultiplier, double radius) {
        Location center = player.getLocation();

        // Onde subtile au sol
        player.getWorld().spawnParticle(Particle.DUST, center, 8, radius / 2, 0.1, radius / 2, 0,
            new Particle.DustOptions(Color.fromRGB(100, 90, 80), 1.2f));
        // Debris de gravier/terre qui s'elevent
        player.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.1, 0),
            6, radius / 2, 0.1, radius / 2, 0, Material.GRAVEL.createBlockData());
        player.getWorld().playSound(center, Sound.BLOCK_GRAVEL_STEP, 0.6f, 0.4f);

        // Degats bases sur les stats du joueur
        double baseDamage = getPlayerBaseDamage(player);
        double damage = baseDamage * damageMultiplier;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }
    }

    private void procRagnarok(Player player, Talent talent) {
        Location center = player.getLocation().clone();
        double radius = 10.0; // Rayon de la zone persistante: 10 blocs
        double stunMs = talent.getValue(3);

        // Calcul des degats bases sur les stats du joueur
        double baseDamage = getPlayerBaseDamage(player);
        double initialDamage = baseDamage * talent.getValue(1); // Impact initial: 800% = 8.0
        double zoneDamagePerTick = baseDamage * 1.5; // Degats par seconde: 150% des degats de base

        // === RAGNAROK - Impact initial + Zone persistante ===

        // 1. Flash d'impact initial
        player.getWorld().spawnParticle(Particle.FLASH, center.clone().add(0, 0.3, 0), 1);

        // 2. Colonne de fumée centrale montante (style volcanique)
        player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, 0.5, 0), 6, 0.2, 0.1, 0.2, 0.04);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 0.02);

        // 3. Onde de choc concentrique au sol (orange → rouge dégradé) + debris
        for (double r = 1.5; r <= radius; r += 2.0) {
            double progress = r / radius;
            int red = (int) (255 - progress * 75);
            int green = (int) (120 - progress * 80);
            int blue = (int) (30 - progress * 10);
            Color waveColor = Color.fromRGB(red, green, blue);

            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 5) {
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location particleLoc = new Location(player.getWorld(), x, center.getY() + 0.15, z);
                player.getWorld().spawnParticle(Particle.DUST, particleLoc,
                    2, 0.15, 0.05, 0.15, 0, new Particle.DustOptions(waveColor, 2.2f));
                // Debris de roche qui eclatent
                player.getWorld().spawnParticle(Particle.BLOCK, particleLoc,
                    2, 0.2, 0.2, 0.2, 0.05, Material.NETHERRACK.createBlockData());
            }
        }

        // 4. Braises/cendres qui s'élèvent + debris centraux massifs
        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.8, 0), 8, 1.0, 0.6, 1.0, 0,
            new Particle.DustOptions(Color.fromRGB(255, 80, 20), 1.0f));
        player.getWorld().spawnParticle(Particle.LAVA, center, 3, 0.5, 0.2, 0.5, 0);
        player.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(0, 0.5, 0),
            20, 1.5, 0.4, 1.5, 0.15, Material.MAGMA_BLOCK.createBlockData());

        // Sons - grondement profond + impact
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.4f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
        player.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.6f);

        // === IMPACT INITIAL - Degats + Stun + Knockback ===
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                double distSq = Math.pow(target.getLocation().getX() - center.getX(), 2) +
                               Math.pow(target.getLocation().getZ() - center.getZ(), 2);
                if (distSq <= radius * radius) {
                    dealAoeDamage(player, target, initialDamage, true);
                    if (target instanceof Mob mob) {
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(stunMs / 50), 10, false, false));
                    }
                    // Knockback
                    Vector direction = target.getLocation().toVector().subtract(center.toVector()).normalize();
                    if (direction.lengthSquared() > 0) {
                        target.setVelocity(direction.multiply(1.5).setY(0.5));
                    }
                }
            }
        }

        // === ZONE PERSISTANTE - 5 secondes, degats toutes les secondes ===
        createSeismicZone(player, center, radius, zoneDamagePerTick, 100, 20); // 100 ticks = 5s, 20 ticks = 1s interval

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§6§l+ RAGNAROK! §7L'apocalypse s'abat sur vos ennemis! §8(Zone 5s)");
        }
    }

    private void procVengeanceRelease(Player player, double damage, double radius) {
        Location center = player.getLocation();

        // Liberation de rage - effet intense mais compact
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, center, 15, radius/3, 0.5, radius/3, 0);
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1);
        player.getWorld().spawnParticle(Particle.DUST, center, 20, radius/2, 0.8, radius/2, 0,
            new Particle.DustOptions(Color.fromRGB(180, 50, 50), 2.0f));
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.6f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                target.damage(damage, player);
            }
        }

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§5§l+ VENGEANCE! §7Vous liberez toute votre rage accumulee!");
        }
    }

    private void procLivingCitadel(Player player, Talent talent) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(talent.getValue(0) / 50), 10, false, false));

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 50, 1, 1, 1, 0.5);
        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§b§l+ CITADELLE! §7Vous etes invulnerable pendant 3 secondes!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);

                // Explosion de la citadelle - puissante mais lisible
                Location center = player.getLocation();
                double damage = 10 * talent.getValue(1);
                double radius = talent.getValue(2);

                player.getWorld().spawnParticle(Particle.FLASH, center, 1);
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1, 0), 1);
                player.getWorld().spawnParticle(Particle.DUST, center, 25, radius/2, 1, radius/2, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 200, 255), 2.0f));
                player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.0f);

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                        target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        target.damage(damage, player);
                    }
                }

                if (shouldSendTalentMessage(player)) {
                    player.sendMessage("§b§l+ EXPLOSION! §7La citadelle libere sa puissance!");
                }
            }
        }.runTaskLater(plugin, (long)(talent.getValue(0) / 50));
    }

    private void procFortressExplosion(Player player, double damage, double radius) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.DUST, center, 50, radius/2, 1, radius/2, 0,
            new Particle.DustOptions(Color.YELLOW, 2));
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                target.damage(damage, player);
            }
        }
    }

    private void startRageCyclone(Player player, Talent talent) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeCyclones.contains(player.getUniqueId()) || !player.isSprinting()) {
                    activeCyclones.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Location center = player.getLocation();
                double damage = 5 * talent.getValue(1);
                double radius = talent.getValue(2);

                // Particules tornado
                for (double y = 0; y < 2; y += 0.3) {
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                        double r = radius * (1 - y/3);
                        double x = center.getX() + r * Math.cos(angle + y);
                        double z = center.getZ() + r * Math.sin(angle + y);
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                            new Location(player.getWorld(), x, center.getY() + y, z), 1);
                    }
                }

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        // Marquer comme dégâts secondaires pour éviter les indicateurs multiples
                        target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        target.damage(damage, player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, (long)(talent.getValue(0) / 50));
    }

    // ==================== UTILITAIRES ====================

    private boolean shouldSendTalentMessage(Player player) {
        ClassData data = plugin.getClassManager().getClassData(player);
        return data != null && data.isTalentMessagesEnabled();
    }

    private Talent getActiveTalentIfHas(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentWithEffect(player, effectType);
    }

    private boolean hasAnyAoETalent(Player player) {
        return talentManager.hasTalentEffect(player, Talent.TalentEffectType.SEISMIC_STRIKE) ||
               talentManager.hasTalentEffect(player, Talent.TalentEffectType.FRACTURE_WAVE) ||
               talentManager.hasTalentEffect(player, Talent.TalentEffectType.CATACLYSM);
    }

    private boolean isOnCooldown(UUID uuid, String ability) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long cooldownEnd = playerCooldowns.get(ability);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    private void setCooldown(UUID uuid, String ability, long durationMs) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(ability, System.currentTimeMillis() + durationMs);
    }

    private void applyLifesteal(Player player, double amount) {
        double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHp = Math.min(maxHp, player.getHealth() + amount);
        player.setHealth(newHp);

        // Particules de heal
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
    }

    private void applyTempShield(Player player, double amount, long durationMs) {
        UUID uuid = player.getUniqueId();
        tempShield.put(uuid, tempShield.getOrDefault(uuid, 0.0) + amount);
        tempShieldExpiry.put(uuid, System.currentTimeMillis() + durationMs);

        // Schedule expiry
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Long expiry = tempShieldExpiry.get(uuid);
            if (expiry != null && System.currentTimeMillis() >= expiry) {
                double remaining = tempShield.getOrDefault(uuid, 0.0);
                tempShield.remove(uuid);
                tempShieldExpiry.remove(uuid);

                // Forteresse explosion si applicable
                Talent fortress = getActiveTalentIfHas(player, Talent.TalentEffectType.FORTRESS);
                if (fortress != null && remaining > 0) {
                    procFortressExplosion(player, remaining * fortress.getValue(0), fortress.getValue(1));
                }
            }
        }, durationMs / 50);
    }

    /**
     * Applique les effets passifs Seisme (Resonance Sismique + Secousses Residuelles)
     * a une cible touchee par une attaque AoE
     */
    private void applySeismicEffects(Player player, LivingEntity target, double baseDamage) {
        UUID targetUuid = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Resonance Sismique - marquer la cible pour amplification future
        Talent resonance = getActiveTalentIfHas(player, Talent.TalentEffectType.SEISMIC_RESONANCE);
        if (resonance != null) {
            seismicResonanceTargets.put(targetUuid, now);
        }

        // Secousses Residuelles - chance de stun
        Talent aftermath = getActiveTalentIfHas(player, Talent.TalentEffectType.SEISMIC_AFTERMATH);
        if (aftermath != null) {
            Long lastStun = aftermathStunCooldowns.get(targetUuid);
            if (lastStun == null || now - lastStun > AFTERMATH_STUN_COOLDOWN_MS) {
                // 25% de chance de stun
                if (Math.random() < 0.25) {
                    aftermathStunCooldowns.put(targetUuid, now);
                    if (target instanceof Mob mob) {
                        // Stun de 0.5s (10 ticks)
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 10, false, false));
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 128, false, false)); // Empeche de sauter
                    }
                    // Effet visuel subtil
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0),
                        3, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(Color.GRAY, 1.0f));
                }
            }
        }
    }

    /**
     * Calcule le multiplicateur de degats AoE en fonction de Resonance Sismique
     */
    private double getSeismicDamageMultiplier(Player player, LivingEntity target) {
        Talent resonance = getActiveTalentIfHas(player, Talent.TalentEffectType.SEISMIC_RESONANCE);
        if (resonance == null) return 1.0;

        UUID targetUuid = target.getUniqueId();
        Long markedTime = seismicResonanceTargets.get(targetUuid);
        if (markedTime != null && System.currentTimeMillis() - markedTime < SEISMIC_RESONANCE_DURATION_MS) {
            return 1.30; // +30% degats
        }
        return 1.0;
    }

    /**
     * Calcule les degats de base du joueur en fonction de ses stats d'equipement
     * Utilise pour les talents AoE afin que les degats scale avec la progression
     */
    private double getPlayerBaseDamage(Player player) {
        // Degats de base (attaque au poing = 1, epee diamant = ~7, netherite = ~8)
        double baseDamage = 8.0; // Base pour un guerrier equipe

        // Ajouter les bonus de stats d'items
        Map<StatType, Double> playerStats = plugin.getItemManager().calculatePlayerStats(player);

        // Bonus flat
        double flatDamage = playerStats.getOrDefault(StatType.DAMAGE, 0.0);
        baseDamage += flatDamage;

        // Bonus % degats
        double damagePercent = playerStats.getOrDefault(StatType.DAMAGE_PERCENT, 0.0);
        baseDamage *= (1 + damagePercent / 100.0);

        // Bonus de classe Guerrier (+20% degats de base)
        ClassData classData = plugin.getClassManager().getClassData(player);
        if (classData != null && classData.hasClass()) {
            baseDamage *= classData.getSelectedClass().getDamageMultiplier();
        }

        return baseDamage;
    }

    /**
     * Inflige des degats AoE et affiche les indicateurs de degats
     * Methode centrale pour tous les talents Seisme
     */
    private void dealAoeDamage(Player player, LivingEntity target, double damage, boolean showIndicator) {
        // Marquer comme degats secondaires pour eviter les cascades dans TalentListener
        target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        // Calculer le multiplicateur de Resonance Sismique
        double finalDamage = damage * getSeismicDamageMultiplier(player, target);

        // Infliger les degats
        target.damage(finalDamage, player);

        // Afficher l'indicateur de degats (hologramme)
        if (showIndicator && finalDamage > 0) {
            PacketDamageIndicator.display(plugin, target.getLocation().add(0, target.getHeight(), 0), finalDamage, false, player);
        }

        // Appliquer les effets passifs Seisme (Resonance + Secousses)
        applySeismicEffects(player, target, finalDamage);
    }

    /**
     * Cree une zone sismique persistante qui inflige des degats periodiquement
     * Utilisee par Ragnarok et potentiellement d'autres talents
     */
    private void createSeismicZone(Player player, Location center, double radius, double damagePerTick, int durationTicks, int tickInterval) {
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= durationTicks || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Effet visuel de la zone (cercle de particules)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location particleLoc = new Location(center.getWorld(), x, center.getY() + 0.1, z);
                    center.getWorld().spawnParticle(Particle.DUST, particleLoc,
                        1, 0.1, 0, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 80, 20), 1.5f));
                    // Debris de roche sur le contour
                    center.getWorld().spawnParticle(Particle.BLOCK, particleLoc,
                        1, 0.15, 0.1, 0.15, 0, Material.CRACKED_STONE_BRICKS.createBlockData());
                }

                // Fissures au sol (effet aleatoire) + debris
                if (ticksElapsed % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double rx = center.getX() + (Math.random() - 0.5) * radius * 2;
                        double rz = center.getZ() + (Math.random() - 0.5) * radius * 2;
                        Location fissureLoc = new Location(center.getWorld(), rx, center.getY() + 0.2, rz);
                        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, fissureLoc,
                            1, 0.1, 0.05, 0.1, 0.01);
                        // Debris de pierre qui jaillissent des fissures
                        center.getWorld().spawnParticle(Particle.BLOCK, fissureLoc,
                            3, 0.15, 0.2, 0.15, 0.02, Material.COBBLESTONE.createBlockData());
                    }
                }

                // Son periodique
                if (ticksElapsed % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_GRAVEL_STEP, 0.5f, 0.3f);
                }

                // Degats aux entites dans la zone
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                        // Verifier la distance horizontale (zone circulaire)
                        double distSq = Math.pow(target.getLocation().getX() - center.getX(), 2) +
                                       Math.pow(target.getLocation().getZ() - center.getZ(), 2);
                        if (distSq <= radius * radius) {
                            dealAoeDamage(player, target, damagePerTick, true);
                        }
                    }
                }

                ticksElapsed += tickInterval;
            }
        }.runTaskTimer(plugin, 0L, tickInterval);
    }

    // ==================== REMPART - UTILITAIRES ====================

    /**
     * Gère un blocage pour les talents Rempart
     * Met à jour Fortification et Avatar du Rempart
     */
    private void handleRempartBlock(Player player, UUID uuid, double blockedDamage) {
        long now = System.currentTimeMillis();

        // Fortification - bonus HP max par stack
        Talent fortify = getActiveTalentIfHas(player, Talent.TalentEffectType.FORTIFY);
        if (fortify != null) {
            double hpBonusPerStack = fortify.getValue(0); // 10%
            int maxStacks = (int) fortify.getValue(1); // 5
            long duration = (long) fortify.getValue(2); // 5000ms

            int currentStacks = fortifyStacks.getOrDefault(uuid, 0);

            // Premier stack - sauvegarder la HP de base
            if (currentStacks == 0) {
                fortifyBaseHealth.put(uuid, player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
            }

            // Ajouter un stack (max 5)
            int newStacks = Math.min(maxStacks, currentStacks + 1);
            fortifyStacks.put(uuid, newStacks);

            // Refresh le timer
            fortifyExpireTime.put(uuid, now + duration);

            // Appliquer le bonus de HP
            applyFortifyBonus(player, uuid, newStacks, hpBonusPerStack);

            // Effets visuels
            player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.9f + (newStacks * 0.15f));

            // Particules dorées (plus intenses avec les stacks)
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                5 + (newStacks * 3), 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.0f));

            if (newStacks == maxStacks) {
                // MAX STACKS! (particules réduites)
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.1);
                // Message d'événement via système centralisé
                int totalBonus = (int) (newStacks * hpBonusPerStack * 100);
                showTempEventMessage(uuid, "§6§l🛡 FORTIFICATION MAX! §c+" + totalBonus + "% §7PV max!");
            } else {
                // Son de progression (l'affichage est dans l'ActionBar centralisé)
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.8f + (newStacks * 0.1f));
            }
        }

        // Marteau du Jugement ne se déclenche pas sur blocage - voir onPlayerAttackZombie

        // Avatar du Rempart - accumulation dégâts bloqués
        Talent bulwarkAvatar = getActiveTalentIfHas(player, Talent.TalentEffectType.BULWARK_AVATAR);
        if (bulwarkAvatar != null && bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) <= now) {
            double current = bulwarkDamageBlocked.merge(uuid, blockedDamage, Double::sum);
            double threshold = bulwarkAvatar.getValue(0);

            // Feedback progression avec milestones
            // L'affichage de la barre est dans l'ActionBar centralisé
            int milestone = (int) ((current / threshold) * 4);
            int lastMilestone = bulwarkLastMilestone.getOrDefault(uuid, 0);

            if (milestone > lastMilestone && milestone < 4) {
                bulwarkLastMilestone.put(uuid, milestone);
                // Son de milestone (l'affichage est dans l'ActionBar centralisé)
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 0.8f + milestone * 0.15f);
            }

            // Déclencher l'Avatar!
            if (current >= threshold) {
                bulwarkDamageBlocked.put(uuid, 0.0);
                bulwarkLastMilestone.put(uuid, 0);
                activateBulwarkAvatar(player, bulwarkAvatar);
            }
        }
    }

    /**
     * Applique le bonus de HP max de Fortification
     */
    private void applyFortifyBonus(Player player, UUID uuid, int stacks, double bonusPerStack) {
        Double baseHealth = fortifyBaseHealth.get(uuid);
        if (baseHealth == null) return;

        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // Calculer le nouveau max HP
        double bonusMultiplier = 1.0 + (stacks * bonusPerStack);
        double newMaxHealth = baseHealth * bonusMultiplier;

        // Appliquer
        maxHealthAttr.setBaseValue(newMaxHealth);

        // Soigner proportionnellement si on gagne des HP
        double currentHealth = player.getHealth();
        double healthPercent = currentHealth / maxHealthAttr.getDefaultValue();
        double newHealth = Math.min(newMaxHealth, healthPercent * newMaxHealth + (newMaxHealth - maxHealthAttr.getDefaultValue()) * 0.5);
        player.setHealth(Math.max(1, newHealth));
    }

    /**
     * Retire le bonus de HP max de Fortification
     */
    private void removeFortifyBonus(Player player, UUID uuid) {
        Double baseHealth = fortifyBaseHealth.get(uuid);
        if (baseHealth == null) return;

        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // Calculer le ratio de vie actuel
        double healthRatio = player.getHealth() / maxHealthAttr.getValue();

        // Restaurer la HP de base
        maxHealthAttr.setBaseValue(baseHealth);

        // Ajuster la vie pour garder le même ratio (sans dépasser le max)
        double newHealth = Math.min(baseHealth, healthRatio * baseHealth);
        player.setHealth(Math.max(1, newHealth));

        // Nettoyer les données
        fortifyStacks.put(uuid, 0);
        fortifyExpireTime.remove(uuid);
        fortifyBaseHealth.remove(uuid);
    }

    /**
     * Active l'Avatar du Rempart
     */
    private void activateBulwarkAvatar(Player player, Talent talent) {
        UUID uuid = player.getUniqueId();
        long duration = (long) talent.getValue(1);
        bulwarkAvatarActiveUntil.put(uuid, System.currentTimeMillis() + duration);

        // Effets visuels d'activation (réduit)
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 1);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.8, 0.8, 0.8, 0.1);

        // Augmenter la taille du joueur (scale 1.25)
        if (player.getAttribute(Attribute.SCALE) != null) {
            player.getAttribute(Attribute.SCALE).setBaseValue(1.25);
        }

        // Immunité CC (résistance aux effets négatifs)
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int)(duration / 50), 0, false, false));

        if (shouldSendTalentMessage(player)) {
            player.sendMessage("§6§l✦ AVATAR DU REMPART! §7Transformation " + (duration/1000) + "s!");
            player.sendMessage("§7- §e100% blocage §7| §c+50% dégâts §7| §6Disques x2 §7| §eImmunité CC");
        }

        // Aura visuelle pendant la durée + maintien immunité CC
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = (int) (duration / 50);

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    // Restaurer la taille normale
                    if (player.getAttribute(Attribute.SCALE) != null) {
                        player.getAttribute(Attribute.SCALE).setBaseValue(1.0);
                    }
                    // Message d'expiration via système centralisé
                    showTempEventMessage(player.getUniqueId(), "§8Avatar du Rempart terminé");
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
                    cancel();
                    return;
                }

                // Maintenir immunité CC (annuler effets négatifs)
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.removePotionEffect(PotionEffectType.POISON);
                player.removePotionEffect(PotionEffectType.WITHER);

                // Particules dorées autour du joueur (réduites)
                Location loc = player.getLocation().add(0, 1.2, 0);
                if (ticks % 2 == 0) { // Une frame sur deux
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 2) { // 4 points au lieu de 8
                        double x = 1.5 * Math.cos(angle + ticks * 0.1);
                        double z = 1.5 * Math.sin(angle + ticks * 0.1);
                        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0, z),
                            1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.2f));
                    }
                }

                // Particules verticales pour effet "géant" (réduites)
                if (ticks % 8 == 0) {
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2.5, 0),
                        2, 0.2, 0.2, 0.2, 0.01);
                }

                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /**
     * Proc du Marteau du Jugement - fait tomber un marteau géant du ciel
     * Utilise ItemDisplay (1.19.4+) pour afficher une hache géante qui tombe
     */
    private void procJudgmentHammer(Player player, LivingEntity target, Talent talent, double baseDamage) {
        Location targetLoc = target.getLocation();
        Location spawnLoc = targetLoc.clone().add(0, 12, 0); // 12 blocs au-dessus

        double mainDamageMultiplier = talent.getValue(1); // 300%
        double aoeDamageMultiplier = talent.getValue(2); // 150%
        double aoeRadius = talent.getValue(3); // 6 blocs

        // Message d'activation via système centralisé
        showTempEventMessage(player.getUniqueId(), "§6§l⚒ MARTEAU DU JUGEMENT!");

        // Son de départ - tonnerre annonciateur
        player.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        player.getWorld().playSound(spawnLoc, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.5f);

        // Créer le marteau géant avec ItemDisplay (1.19.4+)
        org.bukkit.entity.ItemDisplay hammer = targetLoc.getWorld().spawn(spawnLoc, org.bukkit.entity.ItemDisplay.class, display -> {
            // Créer l'item de hache dorée avec enchantement pour l'effet brillant
            org.bukkit.inventory.ItemStack axeItem = new org.bukkit.inventory.ItemStack(Material.GOLDEN_AXE);
            org.bukkit.inventory.meta.ItemMeta meta = axeItem.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 1, true);
                axeItem.setItemMeta(meta);
            }
            display.setItemStack(axeItem);

            // Taille géante (3x plus grand)
            org.bukkit.util.Transformation transform = display.getTransformation();
            org.joml.Quaternionf leftRotation = new org.joml.Quaternionf().rotateX((float) Math.toRadians(180)); // Rotation pour pointer vers le bas
            display.setTransformation(new org.bukkit.util.Transformation(
                transform.getTranslation(),
                leftRotation,
                new org.joml.Vector3f(4.0f, 4.0f, 4.0f), // Scale 4x
                transform.getRightRotation()
            ));

            // Mode d'affichage
            display.setItemDisplayTransform(org.bukkit.entity.ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15)); // Pleine luminosité
            display.setGlowing(true); // Effet de brillance
            display.setGlowColorOverride(org.bukkit.Color.fromRGB(255, 200, 50)); // Lueur dorée
        });

        // Flash lumineux au spawn (réduit)
        player.getWorld().spawnParticle(Particle.FLASH, spawnLoc, 1);
        player.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 8, 0.4, 0.4, 0.4, 0.05);

        // Animation de chute
        new BukkitRunnable() {
            int ticks = 0;
            final int fallDuration = 12;
            final double startY = spawnLoc.getY();
            final double endY = targetLoc.getY() + 0.5;
            final double totalDrop = startY - endY;

            @Override
            public void run() {
                if (ticks >= fallDuration || !hammer.isValid()) {
                    hammer.remove();
                    triggerHammerImpact(player, targetLoc, target, baseDamage, mainDamageMultiplier, aoeDamageMultiplier, aoeRadius);
                    cancel();
                    return;
                }

                double progress = (double) ticks / fallDuration;
                double easedProgress = progress * progress;
                double currentY = startY - (totalDrop * easedProgress);

                Location newLoc = targetLoc.clone();
                newLoc.setY(currentY);
                hammer.teleport(newLoc);

                // Particules de traînée (réduites)
                if (ticks % 2 == 0) {
                    hammer.getWorld().spawnParticle(Particle.END_ROD, newLoc.clone().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
                    hammer.getWorld().spawnParticle(Particle.DUST, newLoc.clone().add(0, 1.5, 0), 3, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
                }

                // Traînée de feu vers la fin (réduite)
                if (ticks > fallDuration / 2 && ticks % 2 == 0) {
                    hammer.getWorld().spawnParticle(Particle.FLAME, newLoc.clone().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                }

                // Son de sifflement
                if (ticks % 3 == 0) {
                    float pitch = 1.5f + (float) (progress * 0.5);
                    hammer.getWorld().playSound(newLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, pitch);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Impact du Marteau du Jugement
     */
    private void triggerHammerImpact(Player player, Location impactLoc, LivingEntity mainTarget,
                                      double baseDamage, double mainMultiplier, double aoeMultiplier, double radius) {
        // === EFFETS VISUELS D'IMPACT (réduits) ===
        impactLoc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, impactLoc.clone().add(0, 0.5, 0), 30, 0.8, 0.8, 0.8, 0.3);
        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 2, 0.3, 0.3, 0.3, 0);
        impactLoc.getWorld().spawnParticle(Particle.END_ROD, impactLoc, 15, 1.5, 0.4, 1.5, 0.05);

        // Onde de choc au sol (réduite)
        for (int ring = 1; ring <= (int) radius; ring += 2) { // Un anneau sur deux
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 12; i++) { // 12 points au lieu de 20
                    double angle = (2 * Math.PI / 12) * i;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location particleLoc = impactLoc.clone().add(x, 0.2, z);
                    impactLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.2f));
                }
            }, ring);
        }

        // Sons d'impact
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.6f);
        impactLoc.getWorld().playSound(impactLoc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.0f, 0.8f);

        // === DÉGÂTS ===
        // Dégâts à la cible principale (300%)
        if (mainTarget.isValid() && !mainTarget.isDead()) {
            double mainDamage = baseDamage * mainMultiplier;
            mainTarget.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
            mainTarget.damage(mainDamage, player);

            // Knockback vers le haut
            mainTarget.setVelocity(new Vector(0, 0.8, 0));
        }

        // Dégâts AoE (150%)
        double aoeDamage = baseDamage * aoeMultiplier;
        int enemiesHit = 0;

        for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, radius, radius, radius)) {
            if (entity instanceof LivingEntity aoTarget && entity != mainTarget && !(entity instanceof Player)) {
                if (plugin.getZombieManager().isZombieZMob(aoTarget)) {
                    aoTarget.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    aoTarget.damage(aoeDamage, player);

                    // Knockback depuis le centre
                    Vector knockback = aoTarget.getLocation().toVector()
                        .subtract(impactLoc.toVector()).normalize().multiply(1.2).setY(0.5);
                    aoTarget.setVelocity(knockback);

                    enemiesHit++;
                }
            }
        }

        // Message de résultat via système centralisé
        String msg = "§6⚒ JUGEMENT! §c" + String.format("%.0f", baseDamage * mainMultiplier) + " §7dégâts";
        if (enemiesHit > 0) {
            msg += " | §e" + enemiesHit + " §7cibles AoE";
        }
        showTempEventMessage(player.getUniqueId(), msg);
    }

    /**
     * Proc du Bouclier Vengeur - lance un disque pulsant
     */
    private void procVengefulShield(Player player, double pulseDamage, double pulseRadius, int pulseCount,
                                     double explosionDamage, double explosionRadius, double travelDistance) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().setY(0).normalize();

        player.getWorld().playSound(start, Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.2f);

        // Message d'activation via système centralisé
        showTempEventMessage(player.getUniqueId(), "§6🛡 BOUCLIER VENGEUR!");

        new BukkitRunnable() {
            double traveled = 0;
            int pulsesDone = 0;
            final double speed = 0.4; // Vitesse lente
            final double pulseInterval = travelDistance / pulseCount;
            Location current = start.clone();

            @Override
            public void run() {
                if (traveled >= travelDistance || !player.isOnline()) {
                    // Explosion finale!
                    procVengefulShieldExplosion(player, current, explosionDamage, explosionRadius);
                    cancel();
                    return;
                }

                // Déplacer le disque
                current.add(direction.clone().multiply(speed));
                traveled += speed;

                // Particules du disque (forme circulaire tournante)
                spawnDiscParticles(current, traveled);

                // Pulse de dégâts à intervalles réguliers
                if (traveled >= (pulsesDone + 1) * pulseInterval) {
                    pulsesDone++;
                    procVengefulShieldPulse(player, current, pulseDamage, pulseRadius, pulsesDone);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Particules du disque tournant (réduit)
     */
    private void spawnDiscParticles(Location center, double rotation) {
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 2) { // 4 points au lieu de 8
            double x = 0.5 * Math.cos(angle + rotation);
            double z = 0.5 * Math.sin(angle + rotation);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z),
                1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.2f));
        }
    }

    /**
     * Pulse du disque (réduit)
     */
    private void procVengefulShieldPulse(Player player, Location center, double damage, double radius, int pulseNumber) {
        // Effet visuel de pulse (réduit)
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 3) { // 6 points au lieu de 12
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(x, 0, z), 2, 0.1, 0.1, 0.1, 0.05);
        }
        center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f + pulseNumber * 0.1f);

        // Dégâts aux ennemis
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }
    }

    /**
     * Explosion finale du disque (réduit)
     */
    private void procVengefulShieldExplosion(Player player, Location center, double damage, double radius) {
        // Effets visuels (réduits)
        center.getWorld().spawnParticle(Particle.FLASH, center, 1);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().spawnParticle(Particle.END_ROD, center, 12, radius / 2, 0.4, radius / 2, 0.05);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);

        // Dégâts aux ennemis
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player && !(entity instanceof Player)) {
                dealAoeDamage(player, target, damage, true);
            }
        }
    }

    /**
     * Proc de la Charge du Bastion
     */
    private void procBastionCharge(Player player, Talent talent) {
        Location start = player.getLocation();
        Vector direction = start.getDirection().setY(0).normalize();
        double distance = talent.getValue(0); // 12 blocs
        double damageMultiplier = talent.getValue(1); // 200%
        double hpPerEnemy = talent.getValue(2); // 8%
        long hpDuration = (long) talent.getValue(3); // 6000ms

        // Propulser le joueur vers l'avant
        player.setVelocity(direction.clone().multiply(3.0).setY(0.3));

        player.getWorld().playSound(start, Sound.ENTITY_BREEZE_CHARGE, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.CLOUD, start, 10, 0.4, 0.2, 0.4, 0.05); // Réduit

        // Message d'activation via système centralisé
        showTempEventMessage(player.getUniqueId(), "§6§l⚔ CHARGE DU BASTION!");

        // Sauvegarder la HP de base
        UUID uuid = player.getUniqueId();
        final double baseMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

        // Appliquer les dégâts et collecter les ennemis touchés
        final double baseDamage = getPlayerBaseDamage(player);
        new BukkitRunnable() {
            int ticks = 0;
            int enemiesHit = 0;
            final Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) { // ~1s de charge (plus long pour 12 blocs)
                    // Fin de la charge - appliquer le bonus HP
                    if (enemiesHit > 0) {
                        applyBastionChargeHpBonus(player, uuid, baseMaxHealth, enemiesHit, hpPerEnemy, hpDuration);
                    }
                    cancel();
                    return;
                }

                // Particules de traînée (réduit)
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                    2, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.5f));

                // Dégâts aux ennemis sur le chemin (rayon plus large pour 12 blocs)
                for (Entity entity : player.getNearbyEntities(2.5, 2.5, 2.5)) {
                    if (entity instanceof LivingEntity target && !(entity instanceof Player)) {
                        if (!hitEntities.contains(target.getUniqueId())) {
                            hitEntities.add(target.getUniqueId());
                            enemiesHit++;

                            // Dégâts
                            double damage = baseDamage * damageMultiplier;
                            target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                            target.damage(damage, player);

                            // Knockback
                            Vector knockback = direction.clone().multiply(1.5).setY(0.4);
                            target.setVelocity(knockback);

                            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.0f);

                            // Effet visuel par ennemi touché (réduit)
                            target.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.05);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Applique le bonus HP temporaire de la Charge du Bastion
     */
    private void applyBastionChargeHpBonus(Player player, UUID uuid, double baseMaxHealth, int enemiesHit, double hpPerEnemy, long duration) {
        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // Calculer le bonus total (8% par ennemi, sans limite)
        double bonusMultiplier = 1.0 + (enemiesHit * hpPerEnemy);
        double newMaxHealth = baseMaxHealth * bonusMultiplier;
        int bonusPercent = (int) (enemiesHit * hpPerEnemy * 100);

        // Appliquer le bonus
        maxHealthAttr.setBaseValue(newMaxHealth);

        // Soigner proportionnellement
        double currentHealth = player.getHealth();
        double bonusHealth = (newMaxHealth - baseMaxHealth) * 0.5; // Soigne 50% du bonus
        player.setHealth(Math.min(newMaxHealth, currentHealth + bonusHealth));

        // Effets visuels
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.2);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.1);

        // Message de résultat via système centralisé
        showTempEventMessage(uuid, "§6§l⚔ CHARGE! §e" + enemiesHit + " §7cibles | §c+" + bonusPercent + "% §7PV max §8(6s)");

        // Planifier la fin du bonus
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Restaurer la HP de base
                var attr = player.getAttribute(Attribute.MAX_HEALTH);
                if (attr != null) {
                    double healthRatio = player.getHealth() / attr.getValue();
                    attr.setBaseValue(baseMaxHealth);
                    player.setHealth(Math.max(1, Math.min(baseMaxHealth, healthRatio * baseMaxHealth)));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                // Message d'expiration via système centralisé
                showTempEventMessage(uuid, "§8⚔ Bonus Charge expiré...");
            }
        }, duration / 50L); // Convertir ms en ticks
    }

    /**
     * NOTE: showPunishmentProgress et showVengefulShieldProgress supprimées
     * Les progressions sont maintenant affichées dans l'ActionBar centralisé (buildRempartActionBar)
     */

    /**
     * Gère l'Écho de Fer - stocke les dégâts et déclenche l'onde de choc
     */
    private void handleIronEcho(Player player, UUID uuid, double damage) {
        Talent ironEcho = getActiveTalentIfHas(player, Talent.TalentEffectType.IRON_ECHO);
        if (ironEcho == null || damage <= 0) return;

        long now = System.currentTimeMillis();
        Long firstStack = ironEchoFirstStack.get(uuid);
        long windowMs = (long) ironEcho.getValue(2);

        // Reset si fenêtre expirée
        if (firstStack != null && now - firstStack > windowMs) {
            ironEchoStacks.put(uuid, 0);
            ironEchoStoredDamage.put(uuid, 0.0);
            ironEchoFirstStack.remove(uuid);
        }

        // Stocker les dégâts (15% des dégâts)
        double storagePercent = ironEcho.getValue(0);
        double storedAmount = damage * storagePercent;
        double totalStored = ironEchoStoredDamage.merge(uuid, storedAmount, Double::sum);

        // Incrémenter les stacks
        int stacks = ironEchoStacks.merge(uuid, 1, Integer::sum);
        int stacksNeeded = (int) ironEcho.getValue(1);

        // Premier stack - démarrer la fenêtre
        if (stacks == 1) {
            ironEchoFirstStack.put(uuid, now);
        }

        // Son de progression (l'affichage est géré par l'ActionBar centralisé)
        player.playSound(player.getLocation(), Sound.BLOCK_COPPER_BULB_TURN_ON, 0.5f, 0.8f + (stacks * 0.2f));

        // Vérifier si on déclenche l'onde de choc
        if (stacks >= stacksNeeded) {
            triggerIronEchoShockwave(player, uuid, totalStored, ironEcho);
        }
    }

    /**
     * NOTE: showIronEchoProgress supprimée - progression affichée dans l'ActionBar centralisé
     */

    /**
     * Déclenche l'onde de choc de l'Écho de Fer
     */
    private void triggerIronEchoShockwave(Player player, UUID uuid, double storedDamage, Talent talent) {
        double radius = talent.getValue(3);
        double healPercent = talent.getValue(4);

        Location center = player.getLocation();

        // === EFFETS VISUELS ÉPIQUES ===
        // Son de gong
        player.getWorld().playSound(center, Sound.BLOCK_BELL_USE, 1.5f, 0.6f);
        player.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);

        // Explosion dorée centrale (réduit)
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.3);
        player.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.15);

        // Onde de choc en cercle qui s'étend (réduit)
        for (int ring = 1; ring <= (int) radius; ring++) {
            final int r = ring;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    Location particleLoc = center.clone().add(x, 0.2, z);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.5f));
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }, ring * 2L);
        }

        // === DÉGÂTS AoE ===
        double totalDamageDealt = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && !(target instanceof Player)) {
                if (plugin.getZombieManager().isZombieZMob(target)) {
                    target.setMetadata("zombiez_secondary_damage", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    target.damage(storedDamage, player);
                    totalDamageDealt += storedDamage;

                    // Effet sur la cible (réduit)
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }

        // === SOIN ===
        double healAmount = totalDamageDealt * healPercent;
        if (healAmount > 0) {
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double newHealth = Math.min(player.getHealth() + healAmount, maxHealth);
            player.setHealth(newHealth);

            player.getWorld().spawnParticle(Particle.HEART, center.clone().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);
        }

        // === MESSAGE via système centralisé ===
        showTempEventMessage(uuid, "§6§l🔔 ÉCHO DE FER! §c" + String.format("%.0f", storedDamage) + " §7dégâts AoE! §a+" + String.format("%.0f", healAmount) + " PV");

        // === RESET ===
        ironEchoStacks.put(uuid, 0);
        ironEchoStoredDamage.put(uuid, 0.0);
        ironEchoFirstStack.remove(uuid);
    }

    /**
     * Vérifie si un joueur est en mode Avatar du Rempart
     */
    public boolean isBulwarkAvatar(Player player) {
        UUID uuid = player.getUniqueId();
        return bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    // ==================== ACTION BAR ====================

    /**
     * Enregistre/désenregistre les ActionBars pour les joueurs Guerrier
     */
    private void registerActionBarProviders() {
        if (plugin.getActionBarManager() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            ClassData data = plugin.getClassManager().getClassData(player);
            boolean isGuerrier = data.hasClass() && data.getSelectedClass() == com.rinaorc.zombiez.classes.ClassType.GUERRIER;

            if (isGuerrier) {
                if (!activeGuerrierActionBar.contains(uuid)) {
                    activeGuerrierActionBar.add(uuid);
                    plugin.getActionBarManager().registerClassActionBar(uuid, this::buildActionBar);
                }
            } else {
                if (activeGuerrierActionBar.contains(uuid)) {
                    activeGuerrierActionBar.remove(uuid);
                    plugin.getActionBarManager().unregisterClassActionBar(uuid);
                }
            }
        }
    }

    /**
     * Construit l'ActionBar spécifique au Guerrier et sa spécialisation
     * Centralise TOUS les affichages pour éviter le clignotement
     */
    public String buildActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        StringBuilder bar = new StringBuilder();

        // Vérifier si un message d'événement temporaire doit être affiché
        Long expiry = tempEventMessageExpiry.get(uuid);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            String tempMsg = tempEventMessage.get(uuid);
            if (tempMsg != null) {
                return tempMsg; // Afficher le message d'événement à la place
            }
        } else {
            // Nettoyer les messages expirés
            tempEventMessage.remove(uuid);
            tempEventMessageExpiry.remove(uuid);
        }

        // Détecter la spécialisation dominante (basée sur les talents actifs)
        int spec = detectDominantSpecialization(player);

        switch (spec) {
            case 0 -> buildBriseurActionBar(player, bar);
            case 1 -> buildRempartActionBar(player, bar);
            case 2 -> buildFureurActionBar(player, bar);
            case 3 -> buildTitanActionBar(player, bar);
            default -> buildGenericGuerrierActionBar(player, bar);
        }

        return bar.toString();
    }

    /**
     * Affiche un message d'événement temporaire dans l'ActionBar
     * Ce message remplace temporairement l'ActionBar normale pendant la durée spécifiée
     */
    private void showTempEventMessage(UUID uuid, String message) {
        showTempEventMessage(uuid, message, EVENT_MESSAGE_DURATION_MS);
    }

    /**
     * Affiche un message d'événement temporaire dans l'ActionBar avec durée personnalisée
     */
    private void showTempEventMessage(UUID uuid, String message, long durationMs) {
        tempEventMessage.put(uuid, message);
        tempEventMessageExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    /**
     * Détecte la spécialisation dominante du joueur
     * basée sur le nombre de talents de chaque slot
     */
    private int detectDominantSpecialization(Player player) {
        List<Talent> activeTalents = talentManager.getActiveTalents(player);
        int[] specCounts = new int[4]; // 0=Briseur, 1=Rempart, 2=Fureur, 3=Titan

        for (Talent talent : activeTalents) {
            int slot = talent.getSlotIndex();
            if (slot >= 0 && slot < 4) {
                specCounts[slot]++;
            }
        }

        // Trouver le slot avec le plus de talents
        int maxCount = 0;
        int dominantSpec = -1;
        for (int i = 0; i < 4; i++) {
            if (specCounts[i] > maxCount) {
                maxCount = specCounts[i];
                dominantSpec = i;
            }
        }

        return dominantSpec;
    }

    /**
     * ActionBar pour Briseur (Slot 0) - AoE/Séisme
     * Centralise: Apocalypse, Cataclysme, Onde de Fracture, Fureur, Charge
     */
    private void buildBriseurActionBar(Player player, StringBuilder bar) {
        UUID uuid = player.getUniqueId();
        bar.append("§7§l[§6⚡§7§l] ");

        // Apocalypse progress
        Talent apocalypse = getActiveTalentIfHas(player, Talent.TalentEffectType.EARTH_APOCALYPSE);
        if (apocalypse != null) {
            double current = aoeDamageCounter.getOrDefault(uuid, 0.0);
            int percent = (int) ((current / APOCALYPSE_THRESHOLD) * 100);
            percent = Math.min(percent, 100);
            String color = percent >= 100 ? "§c§l" : (percent >= 50 ? "§e" : "§7");
            bar.append(color).append("🌋 ").append(percent).append("%");
        }

        // Onde de Fracture - progression des coups
        Talent fractureWave = getActiveTalentIfHas(player, Talent.TalentEffectType.FRACTURE_WAVE);
        if (fractureWave != null) {
            int hitsNeeded = (int) fractureWave.getValue(0);
            int currentHits = fractureWaveHitCounter.getOrDefault(uuid, 0);
            if (currentHits > 0) {
                String waveColor = currentHits >= hitsNeeded - 1 ? "§a" : "§e";
                bar.append("  ").append(waveColor).append("⚡").append(currentHits).append("/").append(hitsNeeded);
            }
        }

        // Compteur d'attaques pour Cataclysme
        Talent cataclysm = getActiveTalentIfHas(player, Talent.TalentEffectType.CATACLYSM);
        if (cataclysm != null) {
            int count = attackCounter.getOrDefault(uuid, 0);
            int needed = (int) cataclysm.getValue(0);
            if (count > 0) {
                bar.append("  §8⚔§f").append(count).append("§7/§e").append(needed);
            }
        }

        // Stacks de Fureur Croissante (si actif même en Briseur)
        int fury = furyStacks.getOrDefault(uuid, 0);
        if (fury > 0) {
            bar.append("  §c🔥+").append(String.format("%.0f", fury * 2.0)).append("%");
        }

        // Charge Dévastatrice prête
        if (chargeReady.getOrDefault(uuid, false)) {
            bar.append("  §a§lCHARGE!");
        }
    }

    /**
     * ActionBar pour Rempart (Slot 1) - Tank/Défense
     * Centralise: Fortification, Écho de Fer, Châtiment, Bouclier Vengeur, Avatar
     */
    private void buildRempartActionBar(Player player, StringBuilder bar) {
        UUID uuid = player.getUniqueId();
        bar.append("§6§l[§e🛡§6§l] ");

        // Fortification stacks
        int fortStacks = fortifyStacks.getOrDefault(uuid, 0);
        if (fortStacks > 0) {
            Long expiry = fortifyExpireTime.get(uuid);
            String timeStr = "";
            if (expiry != null) {
                long remaining = (expiry - System.currentTimeMillis()) / 1000;
                if (remaining > 0) {
                    timeStr = " §7(" + remaining + "s)";
                }
            }
            bar.append("§e❤+").append(fortStacks * 10).append("%").append(timeStr);
        }

        // Écho de Fer stacks et dégâts stockés (intégré directement)
        Talent ironEcho = getActiveTalentIfHas(player, Talent.TalentEffectType.IRON_ECHO);
        if (ironEcho != null) {
            int echoStacks = ironEchoStacks.getOrDefault(uuid, 0);
            double storedDmg = ironEchoStoredDamage.getOrDefault(uuid, 0.0);
            int stacksNeeded = (int) ironEcho.getValue(1);
            if (echoStacks > 0 || storedDmg > 0) {
                String stackColor = echoStacks >= stacksNeeded ? "§a§l" : "§e";
                bar.append("  ").append(stackColor).append("🔔").append(echoStacks).append("/").append(stacksNeeded);
                if (storedDmg > 0) {
                    bar.append(" §c+").append(String.format("%.0f", storedDmg));
                }
            }
        }

        // Châtiment stacks (intégré avec progression)
        Talent punishment = getActiveTalentIfHas(player, Talent.TalentEffectType.PUNISHMENT);
        if (punishment != null) {
            int punishStacks = punishmentStacks.getOrDefault(uuid, 0);
            int stacksNeeded = (int) punishment.getValue(0);
            if (punishStacks > 0 || punishmentReady.getOrDefault(uuid, false)) {
                String stackColor = punishmentReady.getOrDefault(uuid, false) ? "§a§l" : "§d";
                bar.append("  ").append(stackColor).append("⚔").append(punishStacks).append("/").append(stacksNeeded);
                if (punishmentReady.getOrDefault(uuid, false)) {
                    bar.append("§a✓");
                }
            }
        }

        // Bouclier Vengeur - progression des coups
        Talent vengefulShield = getActiveTalentIfHas(player, Talent.TalentEffectType.VENGEFUL_SHIELD);
        if (vengefulShield != null) {
            int hitsNeeded = (int) vengefulShield.getValue(0);
            boolean avatarActive = bulwarkAvatarActiveUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis();
            int effectiveHitsNeeded = avatarActive ? Math.max(2, hitsNeeded / 2) : hitsNeeded;
            int currentHits = vengefulShieldCounter.getOrDefault(uuid, 0);
            if (currentHits > 0) {
                String shieldColor = currentHits >= effectiveHitsNeeded - 1 ? "§b" : "§3";
                bar.append("  ").append(shieldColor).append("◎").append(currentHits).append("/").append(effectiveHitsNeeded);
            }
        }

        // Avatar Rempart actif
        if (isBulwarkAvatar(player)) {
            long remaining = (bulwarkAvatarActiveUntil.get(uuid) - System.currentTimeMillis()) / 1000;
            bar.append("  §6§lAVATAR §e").append(remaining).append("s");
        } else {
            // Dégâts bloqués pour Avatar (si pas encore actif)
            double blocked = bulwarkDamageBlocked.getOrDefault(uuid, 0.0);
            if (blocked > 0) {
                bar.append("  §8Bloqué: §7").append(String.format("%.0f", blocked));
            }
        }
    }

    /**
     * ActionBar pour Fureur (Slot 2) - Rage/Dégâts
     */
    private void buildFureurActionBar(Player player, StringBuilder bar) {
        UUID uuid = player.getUniqueId();
        bar.append("§c§l[§4🔥§c§l] ");

        // Stacks de Fureur Croissante
        int fury = furyStacks.getOrDefault(uuid, 0);
        Talent risingFury = getActiveTalentIfHas(player, Talent.TalentEffectType.RISING_FURY);
        if (risingFury != null) {
            double maxBonus = risingFury.getValue(1) * 100;
            double currentBonus = fury * risingFury.getValue(0) * 100;
            String color = currentBonus >= maxBonus ? "§c§l" : (currentBonus >= maxBonus * 0.5 ? "§6" : "§e");
            bar.append(color).append("🔥 +").append(String.format("%.0f", currentBonus)).append("%");
        }

        // Déchaînement (multi-kills)
        List<Long> kills = recentKills.get(uuid);
        if (kills != null && !kills.isEmpty()) {
            long now = System.currentTimeMillis();
            int recentCount = (int) kills.stream().filter(t -> now - t < 3000).count();
            if (recentCount > 0) {
                bar.append("  §c⚔x").append(recentCount);
            }
        }

        // Riposte Buff actif
        if (riposteBuffTime.containsKey(uuid)) {
            long remaining = (riposteBuffTime.get(uuid) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                bar.append("  §6RIPOSTE §e").append(remaining).append("s");
            }
        }

        // Cyclone actif
        if (activeCyclones.contains(uuid)) {
            bar.append("  §c§lCYCLONE");
        }
    }

    /**
     * ActionBar pour Titan (Slot 3) - Tanky/Résistance
     */
    private void buildTitanActionBar(Player player, StringBuilder bar) {
        UUID uuid = player.getUniqueId();
        bar.append("§8§l[§7🗿§8§l] ");

        // Bouclier temporaire
        double shield = tempShield.getOrDefault(uuid, 0.0);
        if (shield > 0) {
            Long expiry = tempShieldExpiry.get(uuid);
            String timeStr = "";
            if (expiry != null) {
                long remaining = (expiry - System.currentTimeMillis()) / 1000;
                if (remaining > 0) timeStr = " §7(" + remaining + "s)";
            }
            bar.append("§b🛡 ").append(String.format("%.0f", shield)).append(timeStr);
        }

        // Représailles stacks
        int retStacks = retaliationStacks.getOrDefault(uuid, 0);
        if (retStacks > 0) {
            bar.append("  §c⚡").append(retStacks).append(" stacks");
        }

        // Dégâts stockés (Avatar Vengeance)
        double stored = storedDamage.getOrDefault(uuid, 0.0);
        if (stored > 0) {
            bar.append("  §4Stocké: §c").append(String.format("%.0f", stored));
        }

        // HP volés (Avatar de Sang)
        double bloodHp = bloodStolenHp.getOrDefault(uuid, 0.0);
        if (bloodHp > 0) {
            bar.append("  §4♥ ").append(String.format("%.0f", bloodHp));
        }

        // Immortel disponible
        Talent immortal = getActiveTalentIfHas(player, Talent.TalentEffectType.IMMORTAL);
        if (immortal != null) {
            long cooldownEnd = immortalLastProc.getOrDefault(uuid, 0L) + (long) immortal.getValue(0);
            if (System.currentTimeMillis() >= cooldownEnd) {
                bar.append("  §a§l☠ PRÊT");
            }
        }
    }

    /**
     * ActionBar générique si pas de spécialisation claire
     */
    private void buildGenericGuerrierActionBar(Player player, StringBuilder bar) {
        UUID uuid = player.getUniqueId();
        bar.append("§c§l[§6⚔§c§l] ");

        // Fureur stacks
        int fury = furyStacks.getOrDefault(uuid, 0);
        if (fury > 0) {
            bar.append("§c🔥+").append(fury * 2).append("%");
        }

        // Charge prête
        if (chargeReady.getOrDefault(uuid, false)) {
            bar.append("  §a§lCHARGE!");
        }

        // Combat récent indicator
        Long lastCombat = lastCombatTime.get(uuid);
        if (lastCombat != null && System.currentTimeMillis() - lastCombat < 5000) {
            bar.append("  §c⚔ COMBAT");
        }
    }

    /**
     * Nettoie les données d'un joueur (déconnexion)
     */
    public void cleanupPlayer(UUID playerUuid) {
        // Cleanup tracking data
        furyStacks.remove(playerUuid);
        furyLastHit.remove(playerUuid);
        sprintStartTime.remove(playerUuid);
        chargeReady.remove(playerUuid);
        recentKills.remove(playerUuid);
        riposteBuffTime.remove(playerUuid);
        attackCounter.remove(playerUuid);
        immortalLastProc.remove(playerUuid);
        activeCyclones.remove(playerUuid);
        punishmentStacks.remove(playerUuid);
        punishmentLastHit.remove(playerUuid);
        punishmentReady.remove(playerUuid);
        vengefulShieldCounter.remove(playerUuid);
        fortifyStacks.remove(playerUuid);
        fortifyExpireTime.remove(playerUuid);
        fortifyBaseHealth.remove(playerUuid);
        ironEchoStoredDamage.remove(playerUuid);
        ironEchoStacks.remove(playerUuid);
        ironEchoFirstStack.remove(playerUuid);
        bulwarkDamageBlocked.remove(playerUuid);
        bulwarkAvatarActiveUntil.remove(playerUuid);
        bulwarkLastMilestone.remove(playerUuid);
        retaliationStacks.remove(playerUuid);
        retaliationLastProc.remove(playerUuid);
        storedDamage.remove(playerUuid);
        bloodStolenHp.remove(playerUuid);
        chainExecuteBuff.remove(playerUuid);
        lastDamageDealt.remove(playerUuid);
        lastCombatTime.remove(playerUuid);
        tempShield.remove(playerUuid);
        tempShieldExpiry.remove(playerUuid);
        burningStacks.remove(playerUuid);
        aoeDamageCounter.remove(playerUuid);
        lastApocalypseMilestone.remove(playerUuid);
        lastSneakTime.remove(playerUuid);
        fractureWaveHitCounter.remove(playerUuid);
        tempEventMessage.remove(playerUuid);
        tempEventMessageExpiry.remove(playerUuid);

        // Unregister ActionBar
        activeGuerriers.remove(playerUuid);
        activeGuerrierActionBar.remove(playerUuid);
        if (plugin.getActionBarManager() != null) {
            plugin.getActionBarManager().unregisterClassActionBar(playerUuid);
        }
    }
}
