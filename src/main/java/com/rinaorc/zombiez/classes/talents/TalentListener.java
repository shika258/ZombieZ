package com.rinaorc.zombiez.classes.talents;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.classes.ClassData;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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

    // Cache des joueurs Guerriers actifs
    private final Set<UUID> activeGuerriers = ConcurrentHashMap.newKeySet();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 2000;

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

        ClassData data = plugin.getClassManager().getClassData(player);
        if (!data.hasClass()) return;

        double damage = event.getDamage();
        UUID uuid = player.getUniqueId();

        // Tracker dernier combat
        lastCombatTime.put(uuid, System.currentTimeMillis());
        lastDamageDealt.put(uuid, System.currentTimeMillis());

        // === TIER 1 ===

        // Frappe Sismique
        Talent seismicStrike = getActiveTalentIfHas(player, Talent.TalentEffectType.SEISMIC_STRIKE);
        if (seismicStrike != null && !isOnCooldown(uuid, "seismic_strike")) {
            double chance = seismicStrike.getValue(0);
            if (Math.random() < chance) {
                double aoeDamage = damage * seismicStrike.getValue(1);
                double radius = seismicStrike.getValue(2);
                procSeismicStrike(player, target.getLocation(), aoeDamage, radius);
                setCooldown(uuid, "seismic_strike", seismicStrike.getInternalCooldownMs());
            }
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

                // Effet visuel
                target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
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

        // Tourbillon de Lames
        Talent whirlwind = getActiveTalentIfHas(player, Talent.TalentEffectType.BLADE_WHIRLWIND);
        if (whirlwind != null && !isOnCooldown(uuid, "whirlwind")) {
            double chance = whirlwind.getValue(0);
            if (Math.random() < chance) {
                double aoeDamage = damage * whirlwind.getValue(1);
                double radius = whirlwind.getValue(2);
                procWhirlwind(player, aoeDamage, radius);
                setCooldown(uuid, "whirlwind", whirlwind.getInternalCooldownMs());
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

        // Extinction - first hit instakill (balance: seulement mobs < 50 HP)
        Talent extinction = getActiveTalentIfHas(player, Talent.TalentEffectType.EXTINCTION);
        if (extinction != null) {
            long lastCombat = lastCombatTime.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastCombat > extinction.getValue(0)) {
                // First hit!
                double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                boolean isBossOrElite = target.getScoreboardTags().contains("boss") ||
                                        target.getScoreboardTags().contains("elite") ||
                                        maxHp > 50;
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
        if (!data.hasClass()) return;

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
                    player.sendMessage("§6§l+ IMMORTEL! §7Vous avez triomphe de la mort!");
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
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHIELD_BLOCK, 1.0f, 2.0f);
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
                double dr = Math.min(bloodGod.getValue(1), 0.50); // Cap a 50% au lieu de 70%
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
        if (!data.hasClass()) return;

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
        if (!data.hasClass()) return;

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
                    player.sendMessage("§6§l+ CHARGE PRETE! §7Votre prochaine attaque sera devastatrice!");
                }
            }

            activeCyclones.remove(uuid);
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

        // SLOW TICK (40L = 2s) - Tremor
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGuerriers.isEmpty()) return;
                for (UUID uuid : activeGuerriers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent tremor = getActiveTalentIfHas(player, Talent.TalentEffectType.ETERNAL_TREMOR);
                    if (tremor != null) {
                        long lastCombat = lastCombatTime.getOrDefault(uuid, 0L);
                        if (System.currentTimeMillis() - lastCombat < 10000) {
                            procTremor(player, tremor.getValue(1), Math.min(tremor.getValue(2), 10.0));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);

        // RAGNAROK TICK (30s)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGuerriers.isEmpty()) return;
                for (UUID uuid : activeGuerriers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    Talent ragnarok = getActiveTalentIfHas(player, Talent.TalentEffectType.RAGNAROK);
                    if (ragnarok != null && !isOnCooldown(uuid, "ragnarok")) {
                        procRagnarok(player, ragnarok);
                        setCooldown(uuid, "ragnarok", (long) ragnarok.getValue(0));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30);
    }

    // ==================== PROCS ====================

    private void procSeismicStrike(Player player, Location center, double damage, double radius) {
        // Particules
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            for (double r = 0.5; r <= radius; r += 0.5) {
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                player.getWorld().spawnParticle(Particle.BLOCK, new Location(player.getWorld(), x, center.getY(), z),
                    3, Material.STONE.createBlockData());
            }
        }

        // Son
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.8f);

        // Degats
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
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
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, center, 1);
    }

    private void procWhirlwind(Player player, double damage, double radius) {
        Location center = player.getLocation();

        // Particules de rotation
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, new Location(player.getWorld(), x, center.getY() + 1, z), 1);
        }

        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }
    }

    private void procUnleash(Player player, double damageMultiplier, double radius) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1, 1, 1, 0);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        double baseDamage = 10; // Base damage
        double damage = baseDamage * damageMultiplier;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }

        player.sendMessage("§c§l+ DECHAINEMENT! §7Explosion devastatrice!");
    }

    private void procCataclysm(Player player, double damage, double radius) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        player.getWorld().spawnParticle(Particle.FLAME, center, 100, radius/2, 1, radius/2, 0.1);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }

        player.sendMessage("§c§l+ CATACLYSME! §7Une explosion massive ravage tout!");
    }

    private void procBloodAvatar(Player player, double damage, double radius, double selfHeal) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.DUST, center, 100, radius/2, 1, radius/2, 0.1,
            new Particle.DustOptions(Color.RED, 2));
        player.getWorld().playSound(center, Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }

        double heal = player.getAttribute(Attribute.MAX_HEALTH).getValue() * selfHeal;
        applyLifesteal(player, heal);

        player.sendMessage("§4§l+ AVATAR DE SANG! §7Le sang explose autour de vous!");
    }

    private void procEarthApocalypse(Player player, double damage, double radius, double stunMs) {
        Location center = player.getLocation();

        // Effet de tremblement de terre
        for (double r = 1; r <= radius; r += 1) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                player.getWorld().spawnParticle(Particle.BLOCK, new Location(player.getWorld(), x, center.getY(), z),
                    5, Material.DEEPSLATE.createBlockData());
            }
        }

        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
                if (target instanceof Mob mob) {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(stunMs / 50), 10, false, false));
                }
            }
        }

        player.sendMessage("§5§l+ APOCALYPSE! §7La terre tremble sous votre puissance!");
    }

    private void procTremor(Player player, double damageMultiplier, double radius) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.BLOCK, center, 30, radius/2, 0.5, radius/2, 0,
            Material.STONE.createBlockData());
        player.getWorld().playSound(center, Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);

        double baseDamage = 5;
        double damage = baseDamage * damageMultiplier;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }
    }

    private void procRagnarok(Player player, Talent talent) {
        Location center = player.getLocation();
        double damage = 10 * talent.getValue(1);
        double radius = talent.getValue(2);
        double stunMs = talent.getValue(3);

        // Effet apocalyptique
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5, radius/2, 2, radius/2, 0);
        player.getWorld().spawnParticle(Particle.LAVA, center, 200, radius/2, 2, radius/2, 0);
        player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.3f);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
                if (target instanceof Mob mob) {
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(stunMs / 50), 10, false, false));
                }
                // Knockback
                Vector direction = target.getLocation().toVector().subtract(center.toVector()).normalize();
                target.setVelocity(direction.multiply(1.5).setY(0.5));
            }
        }

        player.sendMessage("§6§l+ RAGNAROK! §7L'apocalypse s'abat sur vos ennemis!");
    }

    private void procVengeanceRelease(Player player, double damage, double radius) {
        Location center = player.getLocation();

        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, center, 50, radius/2, 1, radius/2, 0);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != player) {
                target.damage(damage, player);
            }
        }

        player.sendMessage("§5§l+ VENGEANCE! §7Vous liberez toute votre rage accumulee!");
    }

    private void procLivingCitadel(Player player, Talent talent) {
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(talent.getValue(0) / 50), 10, false, false));

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 50, 1, 1, 1, 0.5);
        player.sendMessage("§b§l+ CITADELLE! §7Vous etes invulnerable pendant 3 secondes!");

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);

                // Explosion
                Location center = player.getLocation();
                double damage = 10 * talent.getValue(1);
                double radius = talent.getValue(2);

                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof LivingEntity target && entity != player) {
                        target.damage(damage, player);
                    }
                }

                player.sendMessage("§b§l+ EXPLOSION! §7La citadelle libere sa puissance!");
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
                        target.damage(damage, player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, (long)(talent.getValue(0) / 50));
    }

    // ==================== UTILITAIRES ====================

    private Talent getActiveTalentIfHas(Player player, Talent.TalentEffectType effectType) {
        return talentManager.getActiveTalentWithEffect(player, effectType);
    }

    private boolean hasAnyAoETalent(Player player) {
        return talentManager.hasTalentEffect(player, Talent.TalentEffectType.SEISMIC_STRIKE) ||
               talentManager.hasTalentEffect(player, Talent.TalentEffectType.BLADE_WHIRLWIND) ||
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
}
