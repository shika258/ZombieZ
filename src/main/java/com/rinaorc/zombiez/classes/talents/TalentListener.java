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

    // Avatar de Sang - HP voles
    private final Map<UUID, Double> bloodStolenHp = new ConcurrentHashMap<>();

    // Represailles Infinies - stacks de riposte
    private final Map<UUID, Integer> retaliationStacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> retaliationLastProc = new ConcurrentHashMap<>();

    // Avatar de Vengeance - degats stockes
    private final Map<UUID, Double> storedDamage = new ConcurrentHashMap<>();

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

    // Onde de Fracture - tracking des cibles differentes frappees
    private final Map<UUID, Map<UUID, Long>> fractureWaveTargets = new ConcurrentHashMap<>();

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

        // Frappe Sismique - GARANTI: chaque attaque = onde de choc
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

        // Masse d'Armes - knockback on crit
        Talent maceImpact = getActiveTalentIfHas(player, Talent.TalentEffectType.MACE_IMPACT);
        if (maceImpact != null && event.isCritical()) {
            double knockbackPower = maceImpact.getValue(0);
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            target.setVelocity(direction.multiply(knockbackPower * 0.5).setY(0.3));
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 0.8f);
        }

        // === TIER 3 ===

        // Onde de Fracture - frapper 3 cibles differentes declenche une onde en cone
        Talent fractureWave = getActiveTalentIfHas(player, Talent.TalentEffectType.FRACTURE_WAVE);
        if (fractureWave != null && target instanceof LivingEntity) {
            UUID targetUUID = target.getUniqueId();
            long now = System.currentTimeMillis();
            long windowMs = (long) fractureWave.getValue(1);
            int targetsNeeded = (int) fractureWave.getValue(0);

            // Tracker les cibles frappees
            Map<UUID, Long> playerTargets = fractureWaveTargets.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

            // Nettoyer les cibles expirees
            playerTargets.entrySet().removeIf(e -> now - e.getValue() > windowMs);

            // Ajouter la nouvelle cible (ou update son timestamp)
            playerTargets.put(targetUUID, now);

            // Verifier si on a atteint le seuil
            if (playerTargets.size() >= targetsNeeded) {
                // Proc l'Onde de Fracture!
                double baseDamage = damage * fractureWave.getValue(2);  // 150%
                double bonusPerHit = fractureWave.getValue(3);           // +25% par ennemi
                double range = fractureWave.getValue(4);                 // 4 blocs
                double coneAngle = fractureWave.getValue(5);             // 60 degres
                double slowPercent = fractureWave.getValue(6);           // 30%
                long slowDurationMs = (long) fractureWave.getValue(7);   // 1500ms

                procFractureWave(player, baseDamage, bonusPerHit, range, coneAngle, slowPercent, slowDurationMs);

                // Reset le tracker
                playerTargets.clear();
            } else {
                // Feedback visuel de progression
                int currentCount = playerTargets.size();
                showFractureWaveProgress(player, currentCount, targetsNeeded);
            }
        }

        // Vampire de Guerre
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

        // === TIER 2 ===

        // Bastion
        Talent bastion = getActiveTalentIfHas(player, Talent.TalentEffectType.BASTION);
        if (bastion != null && player.isBlocking() && !isOnCooldown(uuid, "bastion")) {
            double shieldAmount = player.getAttribute(Attribute.MAX_HEALTH).getValue() * bastion.getValue(0);
            long duration = (long) bastion.getValue(1);
            applyTempShield(player, shieldAmount, duration);
            setCooldown(uuid, "bastion", (long) bastion.getValue(2));

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
        }

        // Frenetique
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

        // Nemesis - thorns
        Talent nemesis = getActiveTalentIfHas(player, Talent.TalentEffectType.NEMESIS);
        if (nemesis != null && event.getDamager() instanceof LivingEntity attacker) {
            double reflect = damage * nemesis.getValue(0);
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
     * Gere l'activation par double sneak - RAGNAROK UNIQUEMENT
     */
    private void handleDoubleSneak(Player player) {
        UUID uuid = player.getUniqueId();

        // Ragnarok - L'ULTIME du Guerrier Seisme
        Talent ragnarok = getActiveTalentIfHas(player, Talent.TalentEffectType.RAGNAROK);
        if (ragnarok != null) {
            if (!isOnCooldown(uuid, "ragnarok")) {
                procRagnarok(player, ragnarok);
                setCooldown(uuid, "ragnarok", (long) ragnarok.getValue(0));
            } else {
                // Feedback cooldown
                long remaining = getCooldownRemaining(uuid, "ragnarok");
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§c⏳ Ragnarok: " + (remaining / 1000) + "s"));
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
        int milestone = (int) ((current / APOCALYPSE_THRESHOLD) * 4); // 0, 1, 2, 3, 4
        int lastMilestone = lastApocalypseMilestone.getOrDefault(uuid, 0);

        if (milestone > lastMilestone && milestone < 4) {
            lastApocalypseMilestone.put(uuid, milestone);
            int percent = milestone * 25;
            String bar = "§8[§6" + "█".repeat(milestone) + "§8" + "░".repeat(4 - milestone) + "]";
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§6🌋 Apocalypse " + bar + " §e" + percent + "%"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 0.8f + milestone * 0.15f);
        }

        // Proc automatique!
        if (current >= APOCALYPSE_THRESHOLD && !isOnCooldown(uuid, "apocalypse")) {
            aoeDamageCounter.put(uuid, 0.0);
            lastApocalypseMilestone.put(uuid, 0); // Reset milestones

            // Message d'activation spectaculaire
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§6§l🌋 APOCALYPSE TERRESTRE! 🌋"));
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

        // Note: Ragnarok est active par double sneak (handleDoubleSneak)
    }

    // ==================== PROCS ====================

    private void procSeismicStrike(Player player, Location center, double damage, double radius) {
        // Effet visuel epure: cercle de particules au sol
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            player.getWorld().spawnParticle(Particle.DUST,
                new Location(player.getWorld(), x, center.getY() + 0.1, z),
                2, 0.1, 0, 0.1, 0, new Particle.DustOptions(Color.fromRGB(139, 119, 101), 1.5f));
        }
        // Impact central
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 5, 0.3, 0.1, 0.3, 0.01);

        // Son
        player.getWorld().playSound(center, Sound.BLOCK_DECORATED_POT_BREAK, 0.8f, 0.6f);

        // Degats avec indicateurs
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

        // Feedback ActionBar
        if (shouldSendTalentMessage(player)) {
            String msg = hitCount > 0
                ? "§7§l⚡ §6ONDE DE FRACTURE! §7" + hitCount + " cible(s) §c" + String.format("%.0f", totalDamage) + " dmg"
                : "§7§l⚡ §6Onde de Fracture §7(aucune cible)";
            player.sendActionBar(net.kyori.adventure.text.Component.text(msg));
        }

        // Contribution au systeme Apocalypse
        if (hitCount > 0) {
            trackAoeDamage(player.getUniqueId(), totalDamage * hitCount, player);
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
     * Affiche la progression du build-up de l'Onde de Fracture
     */
    private void showFractureWaveProgress(Player player, int current, int needed) {
        if (!shouldSendTalentMessage(player)) return;

        // Barre de progression visuelle
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 1; i <= needed; i++) {
            bar.append(i <= current ? "§6■" : "§7□");
        }
        bar.append("§8]");

        String msg = "§7Fracture " + bar + " §e" + current + "§7/" + needed;
        player.sendActionBar(net.kyori.adventure.text.Component.text(msg));

        // Petit son de progression
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f + (current * 0.2f));
    }

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
                player.getWorld().spawnParticle(Particle.DUST,
                    new Location(player.getWorld(), x, center.getY() + 0.2, z),
                    3, 0.2, 0.1, 0.2, 0, new Particle.DustOptions(Color.fromRGB(60, 60, 60), 2.0f));
            }
        }
        // Colonne centrale
        player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 8, 0.3, 0.5, 0.3, 0.02);

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

        // 3. Onde de choc concentrique au sol (orange → rouge dégradé)
        for (double r = 1.5; r <= radius; r += 2.0) {
            double progress = r / radius;
            int red = (int) (255 - progress * 75);
            int green = (int) (120 - progress * 80);
            int blue = (int) (30 - progress * 10);
            Color waveColor = Color.fromRGB(red, green, blue);

            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 5) {
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                player.getWorld().spawnParticle(Particle.DUST,
                    new Location(player.getWorld(), x, center.getY() + 0.15, z),
                    2, 0.15, 0.05, 0.15, 0, new Particle.DustOptions(waveColor, 2.2f));
            }
        }

        // 4. Braises/cendres qui s'élèvent
        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.8, 0), 8, 1.0, 0.6, 1.0, 0,
            new Particle.DustOptions(Color.fromRGB(255, 80, 20), 1.0f));
        player.getWorld().spawnParticle(Particle.LAVA, center, 3, 0.5, 0.2, 0.5, 0);

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
                    center.getWorld().spawnParticle(Particle.DUST,
                        new Location(center.getWorld(), x, center.getY() + 0.1, z),
                        1, 0.1, 0, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 80, 20), 1.5f));
                }

                // Fissures au sol (effet aleatoire)
                if (ticksElapsed % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double rx = center.getX() + (Math.random() - 0.5) * radius * 2;
                        double rz = center.getZ() + (Math.random() - 0.5) * radius * 2;
                        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                            new Location(center.getWorld(), rx, center.getY() + 0.2, rz),
                            1, 0.1, 0.05, 0.1, 0.01);
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
}
